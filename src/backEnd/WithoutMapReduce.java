package backEnd;

import java.io.IOException;
import java.net.MalformedURLException;
import de.l3s.boilerpipe.BoilerpipeProcessingException;

public class WithoutMapReduce {
	public static final String TAGGER_MODEL_PATH = "simsum/stanford-postagger-2013-06-20/models/english-left3words-distsim.tagger";
	
	public static void main(String[] args) throws MalformedURLException, IOException, BoilerpipeProcessingException, InterruptedException {
		
		String[] webpageToTxtArgs = new String[2];
		webpageToTxtArgs[0] = "dummyText/Who was the first person to walk on the moon.txt";
		webpageToTxtArgs[1] = "dummyText/webpages.txt";
		WebpageToTxt.main(webpageToTxtArgs);
		
		Thread.sleep(4000); // Delay to allow time to finish writing intermediate result to txt file
		
		String[] parserArgs = new String[2];
		parserArgs[0] = "dummyText/webpages.txt";
		parserArgs[1] = "dummyText/keywords.txt";
		Parser.main(parserArgs);
		
		Thread.sleep(4000); // Delay to allow time to finish writing intermediate result to txt file
		
		String[] decomposerArgs = new String[2];
		decomposerArgs[0] = "dummyText/keywords.txt";
		decomposerArgs[1] = "dummyText/features.txt";
		Decomposer.main(decomposerArgs);
		
		Thread.sleep(4000); // Delay to allow time to finish writing intermediate result to txt file
		
		String[] distanceArgs = new String[2];
		distanceArgs[0] = "dummyText/features.txt";
		distanceArgs[1] = "dummyText/distance.txt";
		SemanticDistance.main(distanceArgs);
	}
}