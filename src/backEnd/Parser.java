package backEnd;

import java.io.*;
import java.util.*;

import edu.stanford.nlp.process.Morphology;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

// The input to this class is a list with each line containing the list of sentences in the corresponding website URL
// The output is same as the input except the words for each URL are separated into two groups, noun and verb
public class Parser {
	// This is the list of stop words that are ignored in the computation
	private static final String IGNORE_LIST = "i me my myself we us our ours ourselves you your yours yourself yourselves he him his himself she her hers herself it its itself they them their theirs themselves what which who whom this that these those am is are was were be been being have has had having do does did doing will would shall should can could may might must ought im youre hes shes its were theyre ive youve weve theyve id youd hed shed wed theyd ill youll hell shell well theyll isnt arent wasnt werent hasnt havent hadnt doesnt dont didnt wont wouldnt shant shouldnt cant cannot couldnt mustnt lets thats whos whats heres theres whens wheres whys hows darent neednt oughtnt mightnt a an the and but if or because as until while of at by for with about against between into through during before after above below to from up down in out on off over under again further then once here there when where why how all any both each few more most other some such no nor not only own same so than too very every least less many now ever never also just put whether since another however one two three four five first second new old high long";
	
	private static final String NOUN = "NN";
	private static final String VERB = "VB";
	
	private static final String SPLIT_SYMBOL = "`"; // Default split token
	
	// this path needs to point to the model file used for the POS tagger
	public static final String TAGGER_MODEL_PATH = "stanford-postagger-2013-06-20/models/english-left3words-distsim.tagger";
	
	// this stores the feature extracted from the document
	public List<List<String>> nounFeature;
	public List<List<String>> verbFeature;
	
	private List<String> keyWords;
	private int longestChainIndex;
	
	private MaxentTagger tagger;
	
	// Parser object for storing intermediate results
	public Parser() throws IOException {
		this.nounFeature = new ArrayList<List<String>>(); // This stores the noun features
		this.verbFeature = new ArrayList<List<String>>(); // This stores the verb features
		this.keyWords = new ArrayList<String>(); // All keywords noun + verbs are stored here
		this.longestChainIndex = 0; // The index number of the longest chain represents the main topic
		this.tagger = new MaxentTagger(TAGGER_MODEL_PATH);
	}
	
	// The actual parsing is done here
	public String beginParsing(String inputPath) throws IOException {
		String output = new String();
		if (!inputPath.isEmpty()) { // Only proceed if there is input
			String url, input;
			List<String> inputList, sentences;
			if (inputPath.contains(".txt")) {
				// If the input is a txt file path then store each line of the file into the inputList list structure
				inputList = readTxt(inputPath);
			} else {
				// If the input is a string then break it into lines according to the newline character and store each line into the inputList list structure
				inputList = Arrays.asList(inputPath.split("\n"));
			}
			Iterator<String> inputListIterator = inputList.iterator();
			while (inputListIterator.hasNext()) { // Iterate over each line where each line represents all sentences from a particular webpage URL
				resetParser(); // Initialize the parser object
				input = inputListIterator.next(); // Grabs a line
				sentences = Arrays.asList(input.split(SPLIT_SYMBOL)); // Split the line into sentences according to the default split token
				Iterator<String> sentencesIterator = sentences.iterator(); // Iterate over the sentences
				url = sentencesIterator.next(); // The URL is always at the first position in a line
				while (sentencesIterator.hasNext()) { // While there are still unprocessed sentences
					extractPOS(sentencesIterator.next()); // Extract the part-of-speech of each word in the current sentence
				}
				chainFeature(); // Group related words together based on their part-of-speech to identify the main topic and save the intermediate result into the parser object
				output = output + "\n" + url + SPLIT_SYMBOL + "VERB" + SPLIT_SYMBOL + verbString() + "\n" + url + SPLIT_SYMBOL + "NOUN" + SPLIT_SYMBOL + nounString(); // Saves the noun group and verb group results
			}
			output = output.substring(1); // Need to remove the extra newline character at the start
		}
		return output;
	}
	
	// Initialize variables
	public void resetParser() {
		this.nounFeature = new ArrayList<List<String>>();
		this.verbFeature = new ArrayList<List<String>>();
		this.keyWords = new ArrayList<String>();
		this.longestChainIndex = 0;
	}
	
	public static void main(String[] args) throws IOException {
		if (args.length != 2) { // See if the number of command line input is correct
			System.out.println("java Parser <webpage textfile path> <output textfile path>");
			return;
		}
		Parser p = new Parser();
		String output = p.beginParsing(args[0]); // Parsing is done here
		Parser.writeTxt(args[1], output); // Saves the result into a text file
		return;
	}
	
	// Extracts the part-of-speech from a sentence
	private void extractPOS(String str) {
		// Runs the Stanford Part-of-Speech Tagger, the result is a single string containing the tags in this format: Word1_TT Word2_TT where TT equals NN (noun) or VB (verb)
		String tagged = this.tagger.tagString(str);
		
		String[] splitSpace = tagged.split(" "); // Separate the part-of-speech result into individual tags
		String[] splitUnderscore;
		String tempWord;
		List<String> tempNoun = new ArrayList<String>();
		List<String> tempVerb = new ArrayList<String>();
		for (int t = 0; t < splitSpace.length; t++) {
			if (splitSpace[t].contains("_")) {
				splitUnderscore = splitSpace[t].split("_"); // Split each tag into word and type
				
				if (!IGNORE_LIST.contains(splitUnderscore[0].toLowerCase())) { // Test if each word is on the ignore list
					tempWord = Morphology.stemStatic(splitUnderscore[0], splitUnderscore[1]).word(); // Stem the word to reduce it to its base form
					if (!tempNoun.contains(splitUnderscore[0]) && splitUnderscore[1].contains(NOUN)) { // If the current word is a noun
						tempNoun.add(tempWord); // Add the word to the noun buffer
						if (!this.keyWords.contains(tempWord)) {
							this.keyWords.add(tempWord); // Add the word to the list of keyword
						}
					}
					if (!tempVerb.contains(splitUnderscore[0]) && splitUnderscore[1].contains(VERB)) { // If the current word is a verb
						tempVerb.add(tempWord); // Add the word to the verb buffer
						if (!this.keyWords.contains(tempWord)) {
							this.keyWords.add(tempWord); // Add the word to the list of keyword
						}
					}
				}
			}
		}
		
		// Add the noun buffer and verb buffer to the list of noun feature and the list of verb feature
		if (!tempNoun.isEmpty() || !tempVerb.isEmpty()) {
			this.nounFeature.add(tempNoun);
			this.verbFeature.add(tempVerb);
		}
	}
	
	// Construct lexical chains using indirect association, the purpose is to identify the main topic when a webpage does not only focus on one topic
	// The logic of this method is difficult to explain here, you need to read my thesis: K. Y. Tam, "Video Summarization based on Speaker Unit", University of Sydney, 2011
	private void chainFeature() {
		int f = 0;
		int t = 0;
		int len = 0;
		boolean found;
		while (f < this.nounFeature.size()) { // Keep merging the list of nouns and the list of verbs as long as we are not over the boundaries
			found = false;
			for (int o = 0; o < this.nounFeature.size(); o++) {
				
				// If two sentences from the same URL have the same noun or the same verb then those sentences are probably related
				if ((o != f) && (!Collections.disjoint(this.nounFeature.get(f), this.nounFeature.get(o)) || !Collections.disjoint(this.verbFeature.get(f), this.verbFeature.get(o)))) {
					t = 0;
					len = Math.max(this.nounFeature.get(o).size(), this.verbFeature.get(o).size());
					while (t < len) {
						if (t < this.nounFeature.get(o).size() && !this.nounFeature.get(f).contains(this.nounFeature.get(o).get(t))) {
							this.nounFeature.get(f).add(this.nounFeature.get(o).get(t));
						}
						if (t < this.verbFeature.get(o).size() && !this.verbFeature.get(f).contains(this.verbFeature.get(o).get(t))) {
							this.verbFeature.get(f).add(this.verbFeature.get(o).get(t));
						}
						t++;
					}
					this.nounFeature.remove(o);
					this.verbFeature.remove(o);
					found = true;
					break;
				}
			
			}
			if (found == false) {
				f++;
			}
		}
		
		// Find the length of each chain in order to identify the longest one corresponding to the main topic
		List<Integer> chainLength = new ArrayList<Integer>();
		for (int i = 0; i < nounFeature.size(); i++) {
			chainLength.add(nounFeature.get(i).size());
		}
		this.longestChainIndex = chainLength.indexOf(Collections.max(chainLength));
	}
	
	// Reformats verb features into a string for output
	public String verbString() {
		String temp = this.verbFeature.get(this.longestChainIndex).toString().replace(", ", " ");
		return new String(temp.substring(1, temp.length() - 1).replace(" ", SPLIT_SYMBOL));
	}
	
	// Reformats noun features into a string for output
	public String nounString() {
		String temp = this.nounFeature.get(this.longestChainIndex).toString().replace(", ", " ");
		return new String(temp.substring(1, temp.length() - 1).replace(" ", SPLIT_SYMBOL));
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
		OutputStream os = null;
		OutputStreamWriter osw = null;
		BufferedWriter bw = null;
		try {
			// Open streams for writing
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
}