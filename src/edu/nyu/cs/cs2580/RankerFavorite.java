package edu.nyu.cs.cs2580;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Scanner;
import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2 based on a refactoring of your favorite
 * Ranker (except RankerPhrase) from HW1. The new Ranker should no longer rely
 * on the instructors' {@link IndexerFullScan}, instead it should use one of
 * your more efficient implementations.
 */

public class RankerFavorite extends Ranker {

    public RankerFavorite(Options options,
        CgiArguments arguments, Indexer indexer) {
      super(options, arguments, indexer);
      System.out.println("Using Ranker: " + this.getClass().getSimpleName());
    }

    @Override
    public Vector<ScoredDocument> runQuery(QueryPhrase query, int numResults) {
    	Queue<ScoredDocument> rankQueue = new PriorityQueue<ScoredDocument>();

		int docid = -1;
		Document doc = _indexer.nextDoc(query, docid);
		while(doc != null){
			rankQueue.add(runquery(query, doc));
			if(rankQueue.size() > numResults){
				rankQueue.poll();
			}
			docid = doc._docid;
			doc = _indexer.nextDoc(query, docid);
		}
		
        Vector<ScoredDocument> results = new Vector<ScoredDocument>();
        ScoredDocument scoredDoc = null;
        while ((scoredDoc = rankQueue.poll()) != null) {
          results.add(scoredDoc);
        }
        Collections.sort(results, Collections.reverseOrder());
        System.out.println("results size:" + results.size());
		return results;
    }
    
	public ScoredDocument runquery(Query query, Document doc){
		// you will be implementing score fuctions here - Q1 
		// Build query vector
		
		//System.out.println("title: " + doc.getTitle());
		List < String > qv = query._tokens;

		// Score the document. 
		double score = 0.0;
        double lambda = 0.5;
	    for (int j = 0; j < qv.size(); ++j){
            // pqd: probability of jth term in qv occurs in document did
            // pqc: probability of jth term in qv occurs in collection of all document
            double pqd = (double)_indexer.documentTermFrequency(qv.get(j), doc._docid)/(double)_indexer.documentTotalTermFrequency(doc._docid);
            double pqc = (double)_indexer.corpusTermFrequency(qv.get(j))/(double)_indexer.totalTermFrequency();
            //System.out.println("pqd: " + pqd + " pqc: " + pqc);
            score += Math.log((1-lambda)*pqd + lambda*pqc);
		}

        if(qv.size() > 0){
            score = Math.pow(Math.E, score);
        }

		return new ScoredDocument(doc, score);
	}


}

