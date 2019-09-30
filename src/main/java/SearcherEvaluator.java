/*
This Code is modified by Section 1 Students of Mahidol University, the Faculty of ICT, 2019
as part of the second project of ITCS414 - Information Retrieval and Storage.

The group consists of
    1. Krittin      Chatrinan       ID 6088022
    2. Anon         Kangpanich      ID 6088053
    3. Tanawin      Wichit          ID 6088221
 */

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

/**
 * This class facilitates evaluation process for the Search Engine
 */
public class SearcherEvaluator {

    /**
     * List of test queries. Each query can be treated as a Document object.
     */
    private List<Document> queries = null;

    /**
     * Mapping between query ID and a set of relevant document IDs
     */
    private Map<Integer, Set<Integer>> answers = null;

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
     * @param corpus the String directory of corpus
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
     * @param query    Document Object for a query
     * @param searcher Searcher Instance to be search
     * @param k        top k SearchResult with highest relevant
     * @return an array consists of precision, recall and F1
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
     * @param searcher Searcher Instance to be search
     * @param k        top k SearchResult with highest relevant
     * @return an array consists of precision, recall and F1
     */
    public double[] getAveragePRF(Searcher searcher, int k) {

        // Instantiates the summation variables with precision, recall, and F
        double sumPrecision = 0, sumRecall = 0, sumF = 0;

        for (Document query : queries) {        // For every Document in query field map
            double[] precisionRecallF = getQueryPRF(query, searcher, k);    // Calculate precision, recall, and F
            sumPrecision += precisionRecallF[0];       // Accumulates Precision
            sumRecall += precisionRecallF[1];          // Accumulates Recall
            sumF += precisionRecallF[2];               // Accumulates F
        }

        // System.out.println("QuerySize = " + queries.size());

        // Find the averages of each variables
        double averagePrecision = sumPrecision / queries.size();
        double averageRecall = sumRecall / queries.size();
        double averageF = sumF / queries.size();

        // Pack them into an double array
        return new double[]{averagePrecision, averageRecall, averageF};
    }
}

/**
 * This class contains helper methods that facilitates Search Engine evaluation process
 */
class EvaluatorMathUtil {

    /**
     * Calculates the Precision value from given result DocId sets
     * Formula: Precision = {Set of Actual Retrieved Relevant elements} / {Set of Search Result elements}
     *
     * @param searchResultDocIds   the set of Ids of Document in the Search Result
     * @param realRelevantDocIdSet the set of Ids of real relevant Document
     * @return precision value
     */
    static double calculatePrecision(Set<Integer> searchResultDocIds, Set<Integer> realRelevantDocIdSet) {
        // Intersection between Search Result set and real Relevant set
        Set<Integer> actualRetrievedRelevant = new HashSet<>(searchResultDocIds);
        actualRetrievedRelevant.retainAll(realRelevantDocIdSet);

        return (double) actualRetrievedRelevant.size() / (double) searchResultDocIds.size();
    }

    /**
     * Calculates the Recall value from given result DocId sets
     * Formula: Recall = {Set of Actual Retrieved Relevant elements} / {Set of Real Relevant elements}
     *
     * @param searchResultDocIds   the set of Ids of Document in the Search Result
     * @param realRelevantDocIdSet the set of Ids of real relevant Document
     * @return precision value
     */
    static double calculateRecall(Set<Integer> searchResultDocIds, Set<Integer> realRelevantDocIdSet) {
        // Intersection between Search Result set and real Relevant set
        Set<Integer> actualRetrievedRelevant = new HashSet<>(searchResultDocIds);
        actualRetrievedRelevant.retainAll(realRelevantDocIdSet);

        return (double) actualRetrievedRelevant.size() / (double) realRelevantDocIdSet.size();
    }

    /**
     * Find the value of F1; aka bridging between precision and recall values.
     *
     * @param precision precision value
     * @param recall    recall value
     * @return F1 value
     */
    static double calculateBigF1(double precision, double recall) {
        if (precision + recall == 0) {
            return 0;
        }
        return (2.0 * precision * recall) / (precision + recall);
    }
}
