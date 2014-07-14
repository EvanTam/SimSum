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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.ArticleExtractor;

// The WebpageToTxt class extracts the sentences from each URL in the input file and concatenates them into a single txt file
public class WebpageToTxt {
	private static final String END_OF_SENTENCE = "!?."; // The list of punctuation that separates sentences
	private static final String SPLIT_SYMBOL = "`"; // Default split token
	
	public static void main(String[] args) throws MalformedURLException, IOException, BoilerpipeProcessingException {
		if (args.length != 2) { // Test if the number of command line arguments is correct
			System.out.println("java WebPageToTxt <url textfile path> <output textfile path>");
			return;
		}
		String output = WebpageToTxt.webPageToTxt(args[0]); // Text extraction is done here 
		WebpageToTxt.writeTxt(args[1], output); // Write result to txt file
		return;
	}
	
	// Text extraction
	// Input is either a list of URLs separated using newline characters or a txt file in the same format
	// Output consisting of a list of strings corresponding to each URL, each string is a concatenation of website text
	public static String webPageToTxt(String url) throws IOException, BoilerpipeProcessingException {
		List<String> urlList;
		if (url.contains(".txt")) { // Load URLS depending on the input format
			urlList = readTxt(url);
		} else {
			urlList = Arrays.asList(url.split("\n"));
		}
		return multiWebPageString(urlList);
	}
	
	// Opens and reads text from a txt file
	// Input is the txt file path
	// Output is a list structure containing the URLs
	private static List<String> readTxt(String path) throws IOException {
		List<String> input = new ArrayList<String>();
		String line = new String();
		
		InputStream is = null;
		InputStreamReader isr = null;
		BufferedReader br = null;
		
		try {
			
			// Open the streams for reading
			is = new FileInputStream(path);
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
			
			while ((line = br.readLine()) != null) { // Read in one line at a time where each line is an URL
				input.add(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			
			// When we are done close the streams
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
	
	// Grabs the text on a webpage based on its URL
	// Input is the webpage URL
	// Output is a string of sentences separated by the default split token
	private static String webPageString(String url) throws MalformedURLException, BoilerpipeProcessingException {
		String lineByLine = new String(url);
		String sentence = new String();
		Character letter;
		
		// Extract text from webpage using the Boilerpipe library, the result is a string of characters from the webpage
		String text = ArticleExtractor.INSTANCE.getText(new URL(url));
		
		Integer textLength = text.length();
		for (int idx = 0; idx < textLength; idx++) { // Parse the string one character at a time
			letter = text.charAt(idx);
			if (END_OF_SENTENCE.contains(letter.toString())) { // If we encounter a end of sentence punctuation then add a split token and then the sentence buffer content to the result
				sentence = sentence.trim() + letter;
				lineByLine = lineByLine + SPLIT_SYMBOL + sentence;
				sentence = new String();
			} else if (Character.isLetterOrDigit(letter)) { // If it is a letter or digit then we accumulate the character in a sentence buffer
				sentence = sentence + letter;
			} else if (Character.isWhitespace(letter)) { // If it is any whitespace then we add a space in a sentence buffer
				sentence = sentence + " ";
			} else {
				sentence = sentence.trim() + letter; // Otherwise we just cut off any leading/trailing whitespace and add the character to the sentence buffer
			}
		}
		return lineByLine;
	}
	
	// Writes a string to a txt file
	// Input is the destination path and the output string you want to write to the txt file
	// Output is the txt file at the destination
	private static void writeTxt(String path, String output) throws BoilerpipeProcessingException, MalformedURLException {
		OutputStream os = null;
		OutputStreamWriter osw = null;
		BufferedWriter bw = null;
		try {
			
			// Open streams for writing
			os = new FileOutputStream(path);
			osw = new OutputStreamWriter(os);
			bw = new BufferedWriter(osw);
			
			bw.write(output); // Writes the file
			
			// Close the streams when we are done
			bw.close();
			osw.close();
			os.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// Loops the webPageString method to process a list of URLs
	// Input is a list structure containing URLs
	// Output is the corresponding webpage strings separate by newline characters
	private static String multiWebPageString(List<String> urls) throws MalformedURLException, BoilerpipeProcessingException {
		String url, multiWebPage = new String();
		ListIterator<String> urlsIterator = urls.listIterator();
		while (urlsIterator.hasNext()) {
			url = urlsIterator.next();
			if (url.contains("QUERY")) { // If the current line is the original query then we just copy it to the result
				multiWebPage = multiWebPage + "\n" + url;
			} else { // Otherwise if it is a URL then we invoke the webPageString method and save the result
				multiWebPage = multiWebPage + "\n" + webPageString(url);
			}
		}
		return multiWebPage.substring(1);
	}
}