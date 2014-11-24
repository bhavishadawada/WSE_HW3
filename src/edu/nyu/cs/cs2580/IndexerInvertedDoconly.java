package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.io.FileUtils;

import java.io.File;

import org.apache.commons.lang.StringUtils;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */

// check if this should implement serializable
public class IndexerInvertedDoconly extends Indexer implements Serializable{
	private static final long serialVersionUID = 3361289105007800861L;

	final int BULK_DOC_PROCESSING_SIZE = 1000;

	// Data structure to maintain unique terms with id
	private Map<String, Integer> _dictionary = new HashMap<String, Integer>();

	// Data structure to store number of times a term occurs in Document
	// term id --> frequency
	private ArrayList<Integer> _documentTermFrequency = new ArrayList<Integer>();

	// Data structure to store number of times a term occurs in the complete Corpus
	// term id --> frequency
	private ArrayList<Integer> _corpusTermFrequency = new ArrayList<Integer>();

	private ArrayList<Integer> _termLineNum = new ArrayList<Integer>();

	// Data structure to store unique terms in the document
	//private Vector<String> _terms = new Vector<String>();

	// Stores all Document in memory.
	private List<DocumentIndexed> _documents = new ArrayList<DocumentIndexed>();
	private Map<Character, Map<String, List<Integer>>> _characterMap = 
			new HashMap<Character, Map<String, List<Integer>>>();

	private HashMap<String, ArrayList<Integer>> _postListBuf = 
			new HashMap<String, ArrayList<Integer>>();
	int _postListBufSize = 1000;

	double[] pageRankLs;

	int[] numViewLs;

	// Provided for serialization
	public IndexerInvertedDoconly(){ }

	public IndexerInvertedDoconly(Options options) {
		super(options);
		System.out.println("Using Indexer: " + this.getClass().getSimpleName());
	}

	@Override
	public void constructIndex() throws IOException {
		String corpusFile = _options._corpusPrefix;
		System.out.println("Construct index from: " + corpusFile);
		long startTime = System.nanoTime();
		//delete everything in index before constructIndex

		CorpusAnalyzer analyzer = new CorpusAnalyzerPagerank(_options);
		pageRankLs = (double[]) analyzer.load();

		LogMiner miner = new LogMinerNumviews(_options);
		numViewLs = (int[]) miner.load();

		deleteFile();
		DocProcessor dp = new DocProcessor(_options._corpusPrefix);
		while (dp.hasNextDoc()) {
			// The problem is this will include num_views also
			dp.nextDoc();
			processDocument(dp.title, dp.body);

			if(_numDocs % BULK_DOC_PROCESSING_SIZE == 0){
				writeFile(_characterMap, false);
				_characterMap.clear();
				//writeFrequency(_corpusTermFrequency);
				//_corpusTermFrequency.clear();
			}
		}

		// if the documents are  < BULK_DOC_PROCESSING_SIZE
		if (!_characterMap.isEmpty()) {
			writeFile(_characterMap, false);
			_characterMap.clear();
		}
		mergeAll();

		System.out.println("_dictionary size: " + _dictionary.size());
		String indexFile = _options._indexPrefix + "/corpus.idx";
		System.out.println("Write Indexer to " + indexFile);

		ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(indexFile));
		writer.writeObject(this);
		writer.close();
		long endTime = System.nanoTime();
		System.out.println("Took ConstructIndex"+(endTime - startTime)/1000000000.0 + " s");
	}

	private void processDocument(String title, String body) throws IOException {
		int docId = _numDocs;
		++ _numDocs;
		Set<String> uniqueTermSetTitle = Utility.tokenize(title);
		buildMapFromTokens(uniqueTermSetTitle,docId);
		Set<String> uniqueTermSetBody = Utility.tokenize(body);
		buildMapFromTokens(uniqueTermSetBody,docId);

		DocumentIndexed doc = new DocumentIndexed(docId);
		
		Map<Integer,Integer> termFrequencyMap = new HashMap<Integer,Integer>();
		//build _dictionary
		for(String token:uniqueTermSetBody){
			if(!_dictionary.containsKey(token)){
				_dictionary.put(token, _corpusTermFrequency.size());
				_corpusTermFrequency.add(0);
				_documentTermFrequency.add(0);
				_termLineNum.add(0);
			}
			int id = _dictionary.get(token);
			_documentTermFrequency.set(id, _documentTermFrequency.get(id) + 1);
		}

		List<String> bodyTermVector = Utility.tokenize2(body);
		for(String token : bodyTermVector){
			int id = _dictionary.get(token);
			_corpusTermFrequency.set(id, _corpusTermFrequency.get(id) + 1);
			
			if(termFrequencyMap.containsKey(id)){
				termFrequencyMap.put(id, termFrequencyMap.get(id) + 1);
			}
			else{
				termFrequencyMap.put(id, 1);
			}
			
		}
		
		// Convert the hashMap to arrayList for Sorting
		List<Map.Entry<Integer, Integer>> entries = new ArrayList<Map.Entry<Integer, Integer>>(termFrequencyMap.entrySet());
		Collections.sort(entries, new Comparator<Map.Entry<Integer, Integer>>() {
			  public int compare(
			      Map.Entry<Integer, Integer> entry1, Map.Entry<Integer, Integer> entry2) {
				  if(entry1.getValue() > entry2.getValue()){
					  return -1;
				  }
				  else if(entry1.getValue() < entry2.getValue()){
					  return 1;
				  }
				  else{
					  return 0;
				  }
			  }
			});
		
		int indexNum = 0;
		for(Entry<Integer,Integer> entry : entries){
			doc.termId[indexNum] = entry.getKey();
			doc.termFrequency[indexNum] = entry.getValue();
			indexNum = indexNum + 1;
		}	

		doc._pageRank = (float)pageRankLs[docId];
		doc._numViews = numViewLs[docId];
		doc.setTitle(title);
		doc._termNum = bodyTermVector.size();
		doc.setUrl(Integer.toString(docId));
		_documents.add(doc);

	}

	private void buildMapFromTokens(Set<String> uniqueTermSet, int docId){

		for(String token: uniqueTermSet){
			// check how to do document frequency here
			char start = token.charAt(0);
			if (_characterMap.containsKey(start)) {
				Map<String, List<Integer>> wordMap = _characterMap.get(start);
				if (wordMap.containsKey(token)) {
					List<Integer> docList = wordMap.get(token);
					if(!docList.contains(docId)){
						docList.add(docId);
					}
				}
				else{
					List<Integer> tempDocList = new ArrayList<Integer>();
					tempDocList.add(docId);
					wordMap.put(token, tempDocList);
				}
			}else{
				// else for if not characterMap
				Map<String, List<Integer>> tempMap = new HashMap<String, List<Integer>>();
				List<Integer> tempList = new ArrayList<Integer>();
				tempList.add(docId);
				tempMap.put(token,tempList);
				_characterMap.put(start,tempMap);		
			}
		}
	}

	private void writeFile( Map<Character, Map<String, List<Integer>>> _characterMap, Boolean record) throws IOException{
		int lineNum = 0;
		// assign id to file names
		for(Entry<Character, Map<String, List<Integer>>> entry : _characterMap.entrySet()){
			String path = _options._indexPrefix + "/" + entry.getKey() + ".idx";
			System.out.println("The path is" + path);
			File file = new File(path);
			BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
			Map<String, List<Integer>> docMap = entry.getValue();
			for(Entry<String, List<Integer>> entry1 : docMap.entrySet()){
				String wordName = entry1.getKey();
				List<Integer> docList = entry1.getValue();
				writer.write(wordName + ":");
				//StringBuffer sb = new StringBuffer();
				String tempString = StringUtils.join(docList, " ");
				writer.write(tempString + "\n");
				lineNum++;
				if(record){
					if(_dictionary.containsKey(wordName)){
						int id = _dictionary.get(wordName);
						_termLineNum.set(id, lineNum);
					}
					else{
						System.out.println(wordName + " is not in _dictionary");
					}
				}
			} 

			writer.close();
		}

	}

	private void deleteFile(){
		String path = _options._indexPrefix + "/";
		File dir = new File(path);
		File[] fileLs = dir.listFiles();
		for(File file : fileLs){
			if(file.getName().endsWith(".idx")){
			file.delete();
			}
		}
	}

	private void writeFrequency(Map<String, Integer> frequency) throws IOException{
		String path = _options._indexPrefix + "/" + _numDocs + ".freq";
		File file = new File(path);
		OutputStream out = new FileOutputStream(file, true);
		for (Map.Entry<String, Integer> entry : frequency.entrySet()) {
			out.write(entry.getKey().getBytes());
			out.write(" ".getBytes());
			out.write(entry.getValue().toString().getBytes());
			out.write("\n".getBytes());
		}
		out.close();
	}

	private void mergeAll() throws IOException{
		List<String> files = Utility.getFilesInDirectory(_options._indexPrefix);
		for (String file : files) {
			if (file.endsWith(".idx")) {
				System.out.println("merging files " + file);
				Map<Character, Map<String,List<Integer>>> charMap = readAll(file);
				String fileName = _options._indexPrefix + "/" + file;
				File charFile = new File(fileName);
				charFile.delete();
				writeFile(charMap, true);
			}
		}
	}

	private Map<Character, Map<String, List<Integer>>> readAll(String filename) throws FileNotFoundException{
		Map<Character, Map<String, List<Integer>>> CharacterMap = new HashMap<Character, Map<String, List<Integer>>>();
		Map<String, List<Integer>> tempMap = new HashMap<String, List<Integer>>();

		String file = _options._indexPrefix + "/" + filename;
		BufferedReader reader = new BufferedReader(new FileReader(file));
		try{
			String line = null;
			while ((line = reader.readLine()) != null) {
				String lineArray[] = line.split(":");
				if(lineArray.length == 2){
					String word = lineArray[0];
					String[] docIDList = lineArray[1].split(" ");
					List<Integer> docList = new ArrayList<Integer>();
					for(int i = 0; i < docIDList.length; i++){
						Integer docId = Integer.parseInt(docIDList[i].trim());
						docList.add(docId);	
					}
					if(tempMap.containsKey(word)){
						List<Integer> tempList = tempMap.get(word);
						tempList.addAll(docList);
						tempMap.put(word,tempList);
					}
					else{
						tempMap.put(word, docList);
					}
				}
			}
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
			try {
				reader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		CharacterMap.put(filename.charAt(0),tempMap);
		return CharacterMap;

	}


	// This is used when the SearchEngine is called with the serve option
	@Override
	public void loadIndex() throws IOException, ClassNotFoundException {
		String indexFile = _options._indexPrefix + "/corpus.idx";
		System.out.println("Load index from: " + indexFile);

		ObjectInputStream reader = new ObjectInputStream(new FileInputStream(indexFile));

		IndexerInvertedDoconly loaded = (IndexerInvertedDoconly) reader.readObject();

		this._documents = loaded._documents;
		this._dictionary = loaded._dictionary;
		this._numDocs = _documents.size();
		this._corpusTermFrequency = loaded._corpusTermFrequency;
		this._documentTermFrequency = loaded._documentTermFrequency;
		this._termLineNum = loaded._termLineNum;
		for (Integer freq : loaded._corpusTermFrequency) {
			this._totalTermFrequency += freq;
		}
		System.out.println(Integer.toString(_numDocs) + " documents loaded " +
				"with " + Long.toString(_totalTermFrequency) + " terms!");
		reader.close();

		/*
		    Query query = new Query("Alfred Matthew");
		    Document doc = nextDoc(query, -1);
		    System.out.println(doc.getTitle());
		    doc = nextDoc(query, doc._docid);
		    System.out.println(doc.getTitle());
		 */
	}

	@Override
	public DocumentIndexed getDoc(int docid) {
		if(docid < _documents.size()){
			return _documents.get(docid);
		}
		else{
			return null;
		}
	}

	/**
	 * In HW2, you should be using {@link DocumentIndexed}
	 */

	//TODO: This is to be implemented as discussed in class
	@Override
	public DocumentIndexed nextDoc(QueryPhrase query, int docid) {
		List<ArrayList<Integer>> postLsArr = new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> cache = new ArrayList<Integer>();

		//query.processQuery();
		List<String> queryVector = query._tokens;
		for (String search : queryVector) {
			ArrayList<Integer> postLs = getPostList(search);
			if(postLs == null){
				return null;
			}
			postLsArr.add(postLs);
			cache.add(0);
		}


		if(postLsArr.size() > 0){
			Boolean hasNextDocId = true;
			while(hasNextDocId){
				int nextDocId = -1;
				int cnt = 0;
				for(int i = 0; i < postLsArr.size(); i++){
					int c = cache.get(i);
					ArrayList<Integer> postLs = postLsArr.get(i);
					c = postLsNext(postLs, c, docid);
					cache.set(i, c);
					if(c == -1){
						hasNextDocId = false;
						break;
					}
					else{
						int currDocId = postLs.get(c);
						if(nextDocId == -1){
							nextDocId = currDocId;
							cnt++;
						}
						else{
							if(nextDocId == currDocId){
								cnt++;
							}
							else{
								nextDocId = Math.max(nextDocId, currDocId);
							}
						}
					}
				}
				if(cnt == postLsArr.size()){
					System.out.println("document found " + nextDocId);
					return _documents.get(nextDocId);
				}
				else{
					docid = nextDocId - 1;
				}
			}
			return null;
		}
		else{
			return null;
		}
	}

	// return a pos such that posLs.get(pos)> docid
	public int postLsNext(ArrayList<Integer> postLs, int cache, int docid){
		int last = postLs.size() - 1;
		if(cache < 0){
			return -1;
		}
		else if(cache > last){
			return -1;
		}
		else if(postLs.get(last) <= docid){
			return -1;
		}
		while(cache < postLs.size() && postLs.get(cache) <= docid){
			cache++;
		}

		if(postLs.get(cache) > docid){
			return cache;
		}
		else{
			return -1;
		}
	}

	public ArrayList<Integer> getPostList(String term){
		if(_postListBuf.size() > _postListBufSize){
			_postListBuf.clear();
		}
		if(_postListBuf.containsKey(term)){
			return _postListBuf.get(term);
		}
		if(_dictionary.containsKey(term)){

			int lineNum = _termLineNum.get(_dictionary.get(term));
			String fileName = _options._indexPrefix + "/"+ term.charAt(0) + ".idx";


			System.out.println("queryTerm " + term);
			System.out.println("Search in " + fileName);
			System.out.println("lineNum " + lineNum);

			// build post list
			BufferedReader br;
			try {
				br = new BufferedReader(new FileReader(fileName));
				String line = "";
				int li = 0;
				while(line != null && li < lineNum){
					li++;
					line = br.readLine();
				}
				if(li == lineNum){
					ArrayList<Integer> postLs = buildPostLs(line);

					//buffer the post list to reduce file IO
					_postListBuf.put(term, postLs);

					return postLs;
				}
				else{
					System.out.println("error lineNum: " + li);
					return null;
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}

	public ArrayList<Integer> buildPostLs(String line){
		String lineArray[] = line.split(":");
		ArrayList<Integer> postLs= new ArrayList<Integer>();
		if(lineArray.length == 2){
			String word = lineArray[0];
			String[] docIDList = lineArray[1].split(" ");
			for(int i = 0; i < docIDList.length; i++){
				Integer docId = Integer.parseInt(docIDList[i].trim());
				postLs.add(docId);
			}
		}
		return postLs;
	}

	// number of documents the term occurs in
	@Override
	public int corpusDocFrequencyByTerm(String term) {
		return _dictionary.containsKey(term) ?
				_documentTermFrequency.get(_dictionary.get(term)) : 0;
	}

	//number of times a term appears in corpus
	@Override
	public int corpusTermFrequency(String term) {
		return _dictionary.containsKey(term) ?
				_corpusTermFrequency.get(_dictionary.get(term)) : 0;
	}

	// number of times a term occurs in document
	@Override
	public int documentTermFrequency(String term, int docid) {
		//System.out.println("get docid: " + docid);
		if(_dictionary.containsKey(term)){
			int cache = -1;
			ArrayList<Integer> postLs = getPostList(term);
			cache = postLsNext(postLs, cache, docid-1);
			if(cache >= 0 && postLs.get(cache) == docid){
				return 1;
			}
			else{
				return 0;
			}
		}
		else{
			return 0;
		}
	}

	@Override
	public int documentTotalTermFrequency(int docid) {
		if(docid < _documents.size()){
			return 1;
		}
		else{
			return 0;
		}
	}
}
