package edu.nyu.cs.cs2580;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.nyu.cs.cs2580.SearchEngine.Options;

public abstract class Indexer2 extends Indexer implements Serializable{
	  /**
	 * 
	 */
	private static final long serialVersionUID = 5380744807563119350L;

	// Data structure to maintain unique terms with id
	Map<String, Integer> _dictionary = new HashMap<String, Integer>();

	// Data structure to store number of times a term occurs in Document
	// term id --> frequency
	ArrayList<Integer> _documentTermFrequency = new ArrayList<Integer>();

	// Data structure to store number of times a term occurs in the complete Corpus
	// term id --> frequency
	ArrayList<Integer> _corpusTermFrequency = new ArrayList<Integer>();

	ArrayList<Integer> _termLineNum = new ArrayList<Integer>();

	// Data structure to store unique terms in the document
	//private Vector<String> _terms = new Vector<String>();

	// Stores all Document in memory.
	List<DocumentIndexed> _documents = new ArrayList<DocumentIndexed>();

	public Indexer2() { }
	  public Indexer2(Options options) {
	    super(options);
	  }
}
