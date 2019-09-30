//Name: 
//Section: 
//ID: 

import java.util.*;

public class JaccardSearcher extends Searcher {

    private Indexer indexer;

    public JaccardSearcher(String docFilename) {
        super(docFilename);
        // TODO: YOUR CODE HERE
        indexer = new Indexer(documents, stopWords);
    }

    @Override
    public List<SearchResult> search(String queryString, int k) {
        // TODO: YOUR CODE HERE
        HashSet<String> tokenSet = new HashSet<>(Searcher.tokenize(queryString));

        // HashSet<Integer> queryTermIdSet = new HashSet<>();
        HashSet<Integer> relevantDocIdSet = new HashSet<>();
        for (String token : tokenSet) {
            int termId = 0;
            try {
                termId = indexer.getTermDict().get(token);
            } catch (NullPointerException e) {
                continue;
            }
            // queryTermIdSet.add(termId);

            HashSet<Integer> docId = indexer.getPostingLists().get(termId);
            relevantDocIdSet.addAll(docId);
        }

        ArrayList<SearchResult> searchResults = new ArrayList<>();
        for (Document doc : documents) {
            if (!relevantDocIdSet.contains(doc.getId())) {
                searchResults.add(new SearchResult(doc, 0));
            } else {
                HashSet<String> termIdSet = new HashSet<>(doc.getTokens());
                // for (String term : new HashSet<>(doc.getTokens())) {
                //     if (indexer.getTermDict().get(term) == null) {
                //         continue;
                //     }
                //     int termId = indexer.getTermDict().get(term);
                //     termIdSet.add(termId);
                // }
                double jaccardScore = JaccardMathHelper.calculateJaccardSimilarity(tokenSet, termIdSet);
                searchResults.add(new SearchResult(doc, jaccardScore));
            }
        }

        return TFIDFSearcher.finalizeSearchResult(searchResults, k);
    }

}

class JaccardMathHelper {
    static double calculateJaccardSimilarity(Set<String> queryTermIdSet, Set<String> documentTermIdSet) {
        if (queryTermIdSet.isEmpty() || documentTermIdSet.isEmpty()) {
            return 0;
        }

        Set<String> intersect = new HashSet<>(documentTermIdSet);
        intersect.retainAll(queryTermIdSet);

        Set<String> union = new HashSet<>(documentTermIdSet);
        union.addAll(queryTermIdSet);

        // System.out.println();
        // System.out.println("Query:" + queryTermIdSet);
        // System.out.println("Doc:" + documentTermIdSet);
        // System.out.println("Intersect: " + intersect.size() + "\tUnion: " + union.size());
        return (double) intersect.size() / (double) union.size();
    }
}
