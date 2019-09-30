//Name: 
//Section: 
//ID: 

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

public class SearcherEvaluator {
    private List<Document> queries = null;                //List of test queries. Each query can be treated as a Document object.
    private Map<Integer, Set<Integer>> answers = null;    //Mapping between query ID and a set of relevant document IDs

    public List<Document> getQueries() {
        return queries;
    }

    public Map<Integer, Set<Integer>> getAnswers() {
        return answers;
    }

    /**
     * Load queries into "queries"
     * Load corresponding documents into "answers"
     * Other initialization, depending on your design.
     *
     * @param corpus
     */
    public SearcherEvaluator(String corpus) {
        String queryFilename = corpus + "/queries.txt";
        String answerFilename = corpus + "/relevance.txt";

        //load queries. Treat each query as a document.
        this.queries = Searcher.parseDocumentFromFile(queryFilename);
        this.answers = new HashMap<Integer, Set<Integer>>();
        //load answers
        try {
            List<String> lines = FileUtils.readLines(new File(answerFilename), "UTF-8");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\t");
                Integer qid = Integer.parseInt(parts[0]);
                String[] docIDs = parts[1].trim().split("\\s+");
                Set<Integer> relDocIDs = new HashSet<Integer>();
                for (String docID : docIDs) {
                    relDocIDs.add(Integer.parseInt(docID));
                }
                this.answers.put(qid, relDocIDs);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Returns an array of 3 numbers: precision, recall, F1, computed from the top *k* search results
     * returned from *searcher* for *query*
     *
     * @param query
     * @param searcher
     * @param k
     * @return
     */
    public double[] getQueryPRF(Document query, Searcher searcher, int k) {
        // TODO: YOUR CODE HERE
        List<SearchResult> searchResults = searcher.search(query.getRawText(), k);
        int retrievedItem = searchResults.size();

        Set<Integer> realRelevantDocIdSet = answers.get(query.getId());

        HashSet<Integer> searchResultDocIdSet = new HashSet<>();
        for (SearchResult searchResultDocId : searchResults) {
            searchResultDocIdSet.add(searchResultDocId.getDocument().getId());
        }

        double precision = EvaluatorMathUtil.calculatePrecision(searchResultDocIdSet, realRelevantDocIdSet);
        double recall = EvaluatorMathUtil.calculateRecall(searchResultDocIdSet, realRelevantDocIdSet);
        double f1 = EvaluatorMathUtil.calculateBigF1(precision, recall);

        return new double[]{precision, recall, f1};
    }

    /**
     * Test all the queries in *queries*, from the top *k* search results returned by *searcher*
     * and take the average of the precision, recall, and F1.
     *
     * @param searcher
     * @param k
     * @return
     */
    public double[] getAveragePRF(Searcher searcher, int k) {

        double sumPrecision = 0, sumRecall = 0, sumF = 0;
        for (Document query: queries){
            double[] precisionRecallF = getQueryPRF(query, searcher, k);
            sumPrecision += precisionRecallF[0];
            sumRecall += precisionRecallF[1];
            sumF += precisionRecallF[2];
        }

        // System.out.println("QuerySize = " + queries.size());

        double averagePrecision = sumPrecision / queries.size();
        double averageRecall = sumRecall / queries.size();
        double averageF = sumF / queries.size();

        return new double[]{averagePrecision, averageRecall, averageF};
    }
}

class EvaluatorMathUtil{
    static double calculatePrecision(Set<Integer> searchResultDocIds, Set<Integer> realRelevantDocIdSet) {
        Set<Integer> actualRelevant = new HashSet<>(searchResultDocIds);
        actualRelevant.retainAll(realRelevantDocIdSet);

        return (double) actualRelevant.size() / (double) searchResultDocIds.size();
    }

    static double calculateRecall(Set<Integer> searchResultDocIds, Set<Integer> realRelevantDocIdSet) {
        Set<Integer> actualRelevant = new HashSet<>(searchResultDocIds);
        actualRelevant.retainAll(realRelevantDocIdSet);

        return (double) actualRelevant.size() / (double) realRelevantDocIdSet.size();
    }

    static double calculateBigF1(double precision, double recall) {
        if (precision + recall == 0){
            return 0;
        }
        return (2.0 * precision * recall) / (precision + recall);
    }
}
