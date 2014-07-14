package backEnd;

import java.io.*;
import java.util.*;

import edu.mit.jwi.*;
import edu.mit.jwi.data.ILoadPolicy;
import edu.mit.jwi.item.*;

// The input to this class are lists of keywords. Each line of the input corresponds to either the list of all nouns or verbs from the corresponding website
// The output is in the same form as the input except each list is expanded to include other words that are related to the original list
public class Decomposer {
	private static final String SPLIT_SYMBOL = "`";
	
	// this path needs to point to the dict folder in Wordnet directory
	public static final File DICTIONARY_PATH = new File("WordNet/2.1/dict");

	// this is a constant representing the knowledge limit of the average human for any topic in general, you should not change this
	private static final Double KNOWLEDGE_LIMIT = Math.exp(4.2);
	
	// this stores the path to the document
	public String path;
	
	// this stores the type, are they nouns or verbs?
	public String type;
	
	// this stores the feature extracted from the document
	public List<String> chain;
	private List<Integer> chainWeight;
	private int chainMarker;
	
	// this stores the full history of the visited nodes during decomposition to ensure nodes are not revisited
	private List<ISynsetID> chainSynsetID;
	private List<Integer> weightSynsetID;
	private int synsetIDMarker;
	
	private IRAMDictionary dictionary;
	
	// decomposer object to store intermediate result
	public Decomposer(IRAMDictionary dict) {
		this.dictionary = dict;
		this.chain = new ArrayList<String>();
		this.chainWeight = new ArrayList<Integer>();
		this.chainSynsetID = new ArrayList<ISynsetID>();
		this.weightSynsetID = new ArrayList<Integer>();
	}
	
	// resets a decomposer object
	public void resetDecomposer() {
		this.chain = new ArrayList<String>();
		this.chainWeight = new ArrayList<Integer>();
		this.chainSynsetID = new ArrayList<ISynsetID>();
		this.weightSynsetID = new ArrayList<Integer>();
	}
	
	// starts the decomposer object
	public String beginDecomposing(String inputPath) throws IOException {
		String output = new String();
		if (!inputPath.isEmpty()) {
			String url, type, input;
			List<String> inputList;
			
			if (inputPath.contains(".txt")) { // Split the input into lines depending on whether the input is a string or txt file
				inputList = readTxt(inputPath);
			} else {
				inputList = Arrays.asList(inputPath.split("\n"));
			}
			
			Iterator<String> inputListIterator = inputList.iterator();
			while (inputListIterator.hasNext()) { // repeat as long as there are more lists
				resetDecomposer();
				input = inputListIterator.next();
				this.chain = new ArrayList<String>(Arrays.asList(input.split(SPLIT_SYMBOL)));
				url = this.chain.get(0); // This is the url where the current list of keywords came from
				this.chain.remove(0);
				type = this.chain.get(0); // This is the type of words in the current list, either nouns or verbs
				this.chain.remove(0);
				if (type.equals("VERB")) { // If this is a list of verbs, decompose the words as verbs
					decomposeChain(POS.VERB);
				}
				if (type.equals("NOUN")) { // If this is a list of nouns, decompose the words as nouns
					decomposeChain(POS.NOUN);
				}
				output = output + "\n" + url + SPLIT_SYMBOL + type + SPLIT_SYMBOL + writeString(); // construct the result for the current list
			}
			output = output.substring(1); // removes the excess newline character from the beginning
		}
		return output;
	}
	
	public static void main(String[] args) throws IOException {
		if (args.length != 2) { // See if the number of command line input is correct
			System.out.println("java Decomposer <keywords textfile path> <output textfile path>");
			return;
		}
		IRAMDictionary dictionary = new RAMDictionary(DICTIONARY_PATH, ILoadPolicy.NO_LOAD);
		dictionary.open();
		
		// construct a decomposer
		Decomposer d = new Decomposer(dictionary);
		String output = d.beginDecomposing(args[0]); // Decomposition is done here
		Decomposer.writeTxt(args[1], output); // Saves the result into a text file
		
		dictionary.close();
		return;
	}
	
	// Decompose the current list/chain of words either as nouns or verbs
	// Input is a word type specifying the type for the current list of word
	private void decomposeChain(POS pos) {
		
		// initialize weights for the targeted chain
		this.chainWeight = new ArrayList<Integer>();
		this.chainWeight.addAll(Collections.nCopies(this.chain.size(), 1));
		
		// this stores the full history of the visited nodes during decomposition of the current chain to ensure nodes are not revisited
		this.chainSynsetID = new ArrayList<ISynsetID>();
		this.weightSynsetID = new ArrayList<Integer>();
		
		// these are  reusable markers for marking the progress of decomposition for the current chain
		this.chainMarker = 0;
		this.synsetIDMarker = 0;
		
		boolean done;
		int current;
		int history = 0;
		// keep decomposing the chain while chainMarker have not reach the last feature AND the minimum semantic distance so far does not exceed the defined knowledge limit
		while ((this.chainMarker < this.chain.size()) && (history < KNOWLEDGE_LIMIT)) {
			done = true;
			while (this.chainMarker < this.chain.size()) {
				decomposeTerm(this.chain.get(this.chainMarker), this.chainWeight.get(this.chainMarker), pos);
				this.chainMarker++;
			}
//			System.out.println(this.chainMarker + " " + this.synsetIDMarker);
			convertSynsetsToWords();
			
			
			// remove features that are beyond the defined knowledge limit
			filterFeature();
			
			if ((this.chain.size() - this.chainMarker) > 2) {
				current = Collections.min(this.chainWeight.subList(this.chainMarker, (this.chainWeight.size() - 1)));
				if (current != history) {
					done = false;
					history = current;
				}
			}
			
			// if the result is not improving or if the current result already contains all features of the query then stop even if decomposition is not complete
			if (done) {
				break;
			}
		}
	}
	
	// Decompose the current word into a list of related features
	// Input is the current word, the weight of its parents and its word type
	private void decomposeTerm(String term, Integer weight, POS pos) {
		
		// retrieve term from dictionary
		IIndexWord idxWord = this.dictionary.getIndexWord(term, pos);
		
		// proceed if the term is in dictionary
		if (idxWord != null) {
			
			// get the synset of the current term
			IWordID wordID = idxWord.getWordIDs().get(0);
			IWord word = this.dictionary.getWord(wordID);
			ISynset synset = word.getSynset();
			
			// append SynsetID of related words for the current term
			List<ISynsetID> tempSynsetID = new ArrayList<ISynsetID>();
			if (!this.chainSynsetID.contains(synset.getID())) {
				tempSynsetID.add(synset.getID());
			}
			for (ISynsetID sid : synset.getRelatedSynsets(Pointer.HYPERNYM)) {
				if (!this.chainSynsetID.contains(sid) && !tempSynsetID.contains(sid)) {
					tempSynsetID.add(sid);
				}
			}
			for (ISynsetID sid :synset.getRelatedSynsets(Pointer.HYPONYM)) {
				if (!this.chainSynsetID.contains(sid) && !tempSynsetID.contains(sid)) {
					tempSynsetID.add(sid);
				}
			}
			for (ISynsetID sid : synset.getRelatedSynsets(Pointer.HOLONYM_MEMBER)) {
				if (!this.chainSynsetID.contains(sid) && !tempSynsetID.contains(sid)) {
					tempSynsetID.add(sid);
				}
			}
			for (ISynsetID sid : synset.getRelatedSynsets(Pointer.HOLONYM_PART)) {
				if (!this.chainSynsetID.contains(sid) && !tempSynsetID.contains(sid)) {
					tempSynsetID.add(sid);
				}
			}
			for (ISynsetID sid : synset.getRelatedSynsets(Pointer.HOLONYM_SUBSTANCE)) {
				if (!this.chainSynsetID.contains(sid) && !tempSynsetID.contains(sid)) {
					tempSynsetID.add(sid);
				}
			}
			for (ISynsetID sid : synset.getRelatedSynsets(Pointer.MERONYM_MEMBER)) {
				if (!this.chainSynsetID.contains(sid) && !tempSynsetID.contains(sid)) {
					tempSynsetID.add(sid);
				}
			}
			for (ISynsetID sid : synset.getRelatedSynsets(Pointer.MERONYM_PART)) {
				if (!this.chainSynsetID.contains(sid) && !tempSynsetID.contains(sid)) {
					tempSynsetID.add(sid);
				}
			}
			for (ISynsetID sid : synset.getRelatedSynsets(Pointer.MERONYM_SUBSTANCE)) {
				if (!this.chainSynsetID.contains(sid) && !tempSynsetID.contains(sid)) {
					tempSynsetID.add(sid);
				}
			}
			// Calculate the weight of each feature based on the weight of the current word's parents 
			this.weightSynsetID.addAll(Collections.nCopies(tempSynsetID.size(), weight * tempSynsetID.size()));
			this.chainSynsetID.addAll(tempSynsetID); // Collect all the related synset IDs
		}
	}
	
	// Forward lookup of synset IDs to find the corresponding words
	private void convertSynsetsToWords() {
		Integer newWeight;
		List<IWord> temp = new ArrayList<IWord>();
		while (this.synsetIDMarker < this.chainSynsetID.size()) {
			temp = this.dictionary.getSynset(this.chainSynsetID.get(this.synsetIDMarker)).getWords();
			newWeight = this.weightSynsetID.get(this.synsetIDMarker) * temp.size();
			for (int t = 0; t < temp.size(); t++) {
				if(!this.chain.contains(temp.get(t).getLemma())) {
					this.chain.add(temp.get(t).getLemma());
					this.chainWeight.add(newWeight);
				}
			}
			this.synsetIDMarker++;
		}
	}
	
	// Filter out features that are beyond the specified knowledge limit
	private void filterFeature() {
		int t = this.chainWeight.size() - 1;
		while (t >= 0) {
			if (this.chainWeight.get(t) > KNOWLEDGE_LIMIT) {
				this.chain.remove(t);
				this.chainWeight.remove(t);
				if (t < this.chainMarker) {
					this.chainMarker--;
				}
			}
			t--;
		}
	}
	
	// Opens and reads text from a txt file
	// Input is the txt file path
	// Output is a list structure containing each line in the text file
	private static List<String> readTxt(String path) throws IOException {
		List<String> input = new ArrayList<String>();
		String line;
		
		InputStream is = null;
		InputStreamReader isr = null;
		BufferedReader br = null;
		
		try {
			is = new FileInputStream(path);
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
			
			while ((line = br.readLine()) != null) {
				input.add(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (is != null) {
				is.close();
			}
			if (isr != null) {
				isr.close();
			}
			if (br != null) {
				br.close();
			}
		}
		return input;
	}
	
	// Writes a string to a txt file
	// Input is the destination path and the output string you want to write to the txt file
	// Output is the txt file at the destination
	public static void writeTxt(String path, String output) {
		// write to text file
		OutputStream os = null;
		OutputStreamWriter osw = null;
		BufferedWriter bw = null;
		try {
			os = new FileOutputStream(path);
			osw = new OutputStreamWriter(os);
			bw = new BufferedWriter(osw);
			
			bw.write(output);
			
			bw.close();
			osw.close();
			os.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// Converts the intermediate results from a chain/list of feature into a single string with items separated by the split token
	public String writeString() {
		String temp = new String(this.chain.toString().replace(", ", " "));
		return temp.substring(1, temp.length() - 1).replace(" ", SPLIT_SYMBOL);
	}
}
