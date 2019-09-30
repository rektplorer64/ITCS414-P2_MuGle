/*
This Code is modified by Section 1 Students of Mahidol University, the Faculty of ICT, 2019
as part of the second project of ITCS414 - Information Retrieval and Storage.

The group consists of
    1. Krittin      Chatrinan       ID 6088022
    2. Anon         Kangpanich      ID 6088053
    3. Tanawin      Wichit          ID 6088221
 */

import java.util.*;

/**
 * This class facilitates an algorithm to retrieve documents based on its Jaccard Coefficient.
 */
public class JaccardSearcher extends Searcher {

    /**
     * An instance of indexer which contains all indexed docs and terms.
     */
    private Indexer indexer;

    /**
     * Main constructor for the Searcher
     *
     * @param docFilename the name of a file that contains documents in it
     */
    public JaccardSearcher(String docFilename) {
        super(docFilename);
        // TODO: YOUR CODE HERE

        // Instantiate the indexer
        indexer = new Indexer(documents, stopWords);
    }

    /**
     * Search the specified dataset by given query string with top k items.
     *
     * @param queryString keyword to be searched
     * @param k           number of top ranking results to be returned
     * @return k top most relevant results
     */
    @Override
    public List<SearchResult> search(String queryString, int k) {
        // TODO: YOUR CODE HERE

        // Tokenize the query and put it into a Set
        HashSet<String> tokenSet = new HashSet<>(Searcher.tokenize(queryString));

        /*
         * Section 1: FETCHING termId, termFreq and relevant docId from the Query
         */

        // Init a set to store Relevant Document Id
        HashSet<Integer> relevantDocIdSet = new HashSet<>();
        for (String token : tokenSet) {         // Iterates thru all query tokens
            int termId;
            try {
                termId = indexer.getTermDict().get(token);
            } catch (NullPointerException e) {      // In case current token is not in the termDict
                continue;                           // Skip this one
            }
            // Get the Posting associate to the termId
            HashSet<Integer> posting = indexer.getPostingLists().get(termId);
            relevantDocIdSet.addAll(posting);   // Add them all to the Relevant Document Id set
        }

        /*
         * Section 2: Calculate Jaccard Coefficient between the Query and all POTENTIAL DOCUMENTS
         */

        // ArrayList for the Final Result
        ArrayList<SearchResult> searchResults = new ArrayList<>();
        for (Document doc : documents) {        // Iterates thru all documents
            if (!relevantDocIdSet.contains(doc.getId())) {                  // If the document is relevant
                searchResults.add(new SearchResult(doc, 0));          // Add the document as a SearchResult with zero score
            } else {
                HashSet<String> termIdSet = new HashSet<>(doc.getTokens()); // Get the token set from the document

                // Calculate Jaccard Coefficient of the document
                double jaccardScore = JaccardMathHelper.calculateJaccardSimilarity(tokenSet, termIdSet);

                // Add the SearchResult with the computed Jaccard Score
                searchResults.add(new SearchResult(doc, jaccardScore));
            }
        }

        return TFIDFSearcher.finalizeSearchResult(searchResults, k);
    }

}

/**
 * This class contains helper methods that facilitates Jaccard Coefficient
 */
class JaccardMathHelper {

    /**
     * Calculate the Jaccard Similarity Score for two given termId Sets
     *
     * @param queryTermIdSet    set for termId of the query
     * @param documentTermIdSet set for termId of the document
     * @return Jaccard Score
     */
    static double calculateJaccardSimilarity(Set<String> queryTermIdSet, Set<String> documentTermIdSet) {
        if (queryTermIdSet.isEmpty() || documentTermIdSet.isEmpty()) {
            return 0;
        }

        // Intersection between QUERY termId set and DOCUMENT termId set
        Set<String> intersect = new HashSet<>(documentTermIdSet);
        intersect.retainAll(queryTermIdSet);

        // Union of DOCUMENT termId set and QUERY term Id set
        Set<String> union = new HashSet<>(documentTermIdSet);
        union.addAll(queryTermIdSet);

        // System.out.println();
        // System.out.println("Query:" + queryTermIdSet);
        // System.out.println("Doc:" + documentTermIdSet);
        // System.out.println("Intersect: " + intersect.size() + "\tUnion: " + union.size());
        return (double) intersect.size() / (double) union.size();
    }
}
