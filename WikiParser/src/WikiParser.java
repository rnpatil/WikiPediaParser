import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.StringReader;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import edu.jhu.nlp.wikipedia.PageCallbackHandler;
import edu.jhu.nlp.wikipedia.WikiPage;
import edu.jhu.nlp.wikipedia.WikiXMLParser;
import edu.jhu.nlp.wikipedia.WikiXMLParserFactory;


public class WikiParser {


	private static String text = null;
	private static String[] textArray = null;
	private static final String RedirectString ="#REDIRECT";
	private  static Integer idCounter =0;
	private final static LinkedHashMap<String,TreeMap<String,Integer>> documentWordListMap = new LinkedHashMap<String,TreeMap<String,Integer>>();
	private final static LinkedHashMap<String,Integer> wordIDMap = new LinkedHashMap<String,Integer>();
	private static  TreeMap<String,Integer> wordIdMapforDocument;
	private static Map<String, Integer> sortedwordIDMap= new HashMap<String,Integer>();
	private static final String regexAlpha="[^a-zA-Z\\s]";
	private static final String regexMultispace="\\s{2,}";

	public static void main(String[] args) throws Exception {

		parse(args);
	}


	public static void  parse(String[] args)
	{
		WikiXMLParser wxsp = WikiXMLParserFactory.getSAXParser(args[0]);

		try {
			wxsp.setPageCallback(new PageCallbackHandler() { 

				public void process(WikiPage page)  {
					try {
						if(!page.getText().startsWith(RedirectString))
						{
							text =removeStopWordsandStem(page.getText().replaceAll(regexMultispace, " ").trim().replaceAll(regexAlpha, "").trim().toLowerCase());

							textArray= text.split("\\s");
							wordIdMapforDocument= new TreeMap<String,Integer>();

							for(String str: textArray)
							{
								if(!wordIDMap.containsKey(str))
								{
									wordIDMap.put(str, idCounter);
									idCounter++;
								}
								if(wordIdMapforDocument.containsKey(str))
								{
									int wordCount = wordIdMapforDocument.get(str);
									wordIdMapforDocument.put(str,wordCount+1);
								}
								else
								{
									wordIdMapforDocument.put(str,1);
								}
							}
							documentWordListMap.put(page.getID(), wordIdMapforDocument);
						}


					} catch (Exception e) {

						e.printStackTrace();
					}
				}
			});

			wxsp.parse();

			sortedwordIDMap = sortRankMapByValues(wordIDMap);


			BufferedWriter bw = new BufferedWriter(new FileWriter(args[1]));
			BufferedWriter bw2 = new BufferedWriter(new FileWriter(args[2]));

			
			bw2.write(sortedwordIDMap.size()+"----------------vocabularysize");
			
			System.out.println(sortedwordIDMap.size()+"----------------vocabularysize");

			BigInteger sum = BigInteger.valueOf(0);

			bw2.write(documentWordListMap.size()+"--------------------docsize");
			System.out.println(documentWordListMap.size()+"--------------------docsize");
			for(String docID: documentWordListMap.keySet())
			{
				TreeMap<String, Integer> wordListMap =documentWordListMap.get(docID);
				sum = sum.add(BigInteger.valueOf(wordListMap.size()));
				for(String wordID:sortedwordIDMap.keySet())
				{

					if(wordListMap.containsKey(wordID))
					{
						bw.write(sortedwordIDMap.get(wordID)+":"+wordListMap.get(wordID)+ " ");
					}
				}
				bw.write("\n");
				bw.flush();

			}
			
			bw.close();
			bw2.write(sum+"--------------------totalTokens");
			bw2.close();
			System.out.println(sum+"--------------------totalTokens");
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static String removeStopWordsandStem(String text) throws Exception {

		StringBuilder sb = new StringBuilder();
		Tokenizer whitespaceTokenizer = new WhitespaceTokenizer();
		whitespaceTokenizer.setReader(new StringReader(text));
		TokenStream tokenStream = new StopFilter(whitespaceTokenizer, StopAnalyzer.ENGLISH_STOP_WORDS_SET);
		tokenStream = new PorterStemFilter(tokenStream);
		try
		{
			CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
			tokenStream.reset();
			while (tokenStream.incrementToken()) {
				String term = charTermAttribute.toString();
				sb.append(term + " ");
			}
			return sb.toString();
		}
		catch(Exception e)
		{
			return null;
		}
		finally {
			tokenStream.close();
		}
	}

	public static <K extends Comparable,V extends Comparable> Map<K,V> sortRankMapByValues(Map<K,V> map){
		List<Map.Entry<K,V>> entries = new LinkedList<Map.Entry<K,V>>(map.entrySet());

		Collections.sort(entries, new Comparator<Map.Entry<K,V>>() {
			public int compare(Entry<K, V> o1, Entry<K, V> o2) {
				return o1.getValue().compareTo(o2.getValue());
			}
		});
		Map<K,V> descendingSortedMap = new LinkedHashMap<K,V>();

		for(Map.Entry<K,V> entry: entries){
			descendingSortedMap.put(entry.getKey(), entry.getValue());
		}
		return descendingSortedMap;
	}
}
