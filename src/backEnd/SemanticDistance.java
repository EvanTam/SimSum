package backEnd;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

// The SemanticDistance class compares the query features against the webpage features of the URLs and saves the result in a txt file
public class SemanticDistance {
	private static final String SPLIT_SYMBOL = "`"; // Default split token
	
	public static void main(String[] args) throws IOException {
		List<Double> result = new ArrayList<Double>();
		if (args.length != 2) { // Test if the number of command line arguments is correct
			System.out.println("java SemanticDistance <features textfile path>");
			return;
		}
		result = SemanticDistance.semanticDistance(args[0]); // The actual comparison is done here
		String temp = new String(result.toString().replace(", ", "\n"));
		writeTxt(args[1], temp.substring(1, temp.length() - 1)); // Saves the result to a txt file
		return;
	}
	
	// Reads the features and compare the ones from the query against the ones from webpages
	// Input is either a string of features separated using the newline character or it is a txt file in the same format
	// Output is a list structure containing the results
	public static List<Double> semanticDistance(String inputPath) throws IOException {
		String input, type, key;
		List<String> inputList, features;
		
		List<String> queryVerbList = new ArrayList<String>(); // Stores all verbs from the query into a list
		List<String> queryNounList = new ArrayList<String>(); // Stores all nouns from the query into a list
		List<String> verbKeyList = new ArrayList<String>(); // Stores the webpage URL (key) where each verb came from
		List<List<String>> verbValueList = new ArrayList<List<String>>(); // Stores the verbs (value) corresponding to the verbKeyList
		List<String> nounKeyList = new ArrayList<String>(); // Stores the webpage URL (key) where each noun came from
		List<List<String>> nounValueList = new ArrayList<List<String>>(); // Stores the nouns (value) corresponding to the nounKeyList
		
		if (inputPath.contains(".txt")) { // Split the input into lines depending on whether the input is a string or txt file
			inputList = readTxt(inputPath);
		} else {
			inputList = Arrays.asList(inputPath.split("\n"));
		}
		
		Iterator<String> inputListIterator = inputList.iterator();
		while (inputListIterator.hasNext()) { // Examine each line of the input
			input = inputListIterator.next().trim();
			features = new ArrayList<String>(Arrays.asList(input.split(SPLIT_SYMBOL))); // Convert the line into a list of words
			key = features.get(0); // The first word represents the source, if the current line was produced from the query then the it is "QUERY" otherwise it is the URL of the webpage
			features.remove(0); // Pop off the source we just read
			type = features.get(0); // The type is either "NOUN" or "VERB"
			features.remove(0); // Pop off the type we just read
			key = key + " " + type; // The concatenation of the source and the type is the key
			if (key.contains("QUERY")) { // Only the first two lines at the input corresponds to the query
				if (type.equals("VERB")) { // If the current line contains the string of verb features from the query then save it into queryVerbList
					queryVerbList = listUnion(queryVerbList, features);
				}
				if (type.equals("NOUN")) { // If the current line contains the string of noun features from the query then save it into queryNounList
					queryNounList = listUnion(queryNounList, features);
				}
			} else { // If we reach here it means the current line was produced from a URL in the search result
				if (type.equals("VERB")) { // If the current line contains the string of verb features from a URL then save its key into VerbKeyList and the corresponding verb into VerbValueList
					verbKeyList.add(key);
					verbValueList.add(features);
				}
				if (type.equals("NOUN")) { // If the current line contains the string of noun features from a URL then save its key into NounKeyList and the corresponding noun into NounValueList
					nounKeyList.add(key);
					nounValueList.add(features);
				}
			}
		}
		
		List<String> verbTwo, nounTwo;
		List<Double> result = new ArrayList<Double>();
		for (int i = 0; i < verbValueList.size(); i++) { // Grab the verb and noun list for each URL
			verbTwo = verbValueList.get(i);
			nounTwo = nounValueList.get(i);
			
			// Compare the verb and noun list for each URL against the ones from the query
			result.add(Math.max(((double) listIntersect(queryNounList, nounTwo).size()) / Math.min(queryNounList.size(), nounTwo.size()), ((double) listIntersect(queryVerbList, verbTwo).size()) / listUnion(queryVerbList, verbTwo).size()));
		}
		return result;
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
	
	// Takes the intersection between two lists
	// Input are two lists
	// Output is a single list containing the intersection
	private static List<String> listIntersect(List<String> listOne, List<String> listTwo) {
		List<String> temp = new ArrayList<String>();
		for (int o = 0; o < listOne.size(); o++) {
			if (!temp.contains(listOne.get(o)) && listTwo.contains(listOne.get(o))) {
				temp.add(listOne.get(o));
			}
		}
		return temp;
	}
	
	// Takes the union between two lists
	// Input are two lists
	// Output is a single list containing the union
	private static List<String> listUnion(List<String> listOne, List<String> listTwo) {
		List<String> temp = new ArrayList<String>();
		listOne.addAll(listTwo);
		for (int o = 0; o < listOne.size(); o++) {
			if (!temp.contains(listOne.get(o))) {
				temp.add(listOne.get(o));
			}
		}
		return temp;
	}
}