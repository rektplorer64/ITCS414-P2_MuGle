import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * This class facilitates an algorithm to retrieve documents based on its Probabilistic (BM25).
 */
public class MyCoolSearcher extends Searcher {

    /**
     * An instance of Probabilistic indexer which contains all indexed docs and terms.
     */
    private final ProbabilisticIndexer indexer;

    /**
     * Default constructor. Load raw documents into Document objects in memory
     *
     * @param docFilename the name of a file that contains documents in it
     */
    public MyCoolSearcher(String docFilename) {
        super(docFilename);
        ProbabilisticIndexer.Builder indexerBuilder = new ProbabilisticIndexer.Builder(documents, stopWords, 1.2, 0.75, 2.0);
        indexer = indexerBuilder.build();
    }

    @Override
    public List<SearchResult> search(String queryString, int k) {

        // Tokenize the Query string to a List of String
        List<String> tokens = Searcher.tokenize(queryString);

        /*
         * Section 1:
         * FETCHING termId, termFreq and relevant docId from the Query
         */

        // HashMap for Storing Query's (termId: Int) maps to (termFreqInsideQuery: Int)
        HashMap<Integer, Integer> queryTermFreq = new HashMap<>();

        // HashMap for Storing potentially relevant Document Ids
        HashSet<Integer> potentialDocIds = new HashSet<>();

        // For every token inside Query
        for (String token : tokens) {

            // If the token is not exist in the Term Dictionary
            if (!indexer.getTermDict().containsKey(token)) {
                continue;   // Dismiss it
            }

            // If it exists, then get the TermId of this token
            int termId = indexer.getTermDict().get(token);

            // If the the Map that stores docFreq does not meet this termId before
            if (!queryTermFreq.containsKey(termId)) {
                // Memorize it with initial frequency = 0
                queryTermFreq.put(termId, 0);
            }
            // Count the frequency of the query token up by 1
            queryTermFreq.put(termId, queryTermFreq.get(termId) + 1);

            // Add all docIds from the acc
            potentialDocIds.addAll(indexer.getPostingLists().get(termId));
        }

        /*
         * Section 2:
         * CALCULATE BM25 Probabilistic score foreach document and query
         */

        // New Map to store search result
        HashMap<Integer, ProbabilisticResult> results = new HashMap<>();

        // Instantiate new tuning variables
        double k1 = indexer.getTuningK1();
        double b = indexer.getTuningB();
        double k3 = indexer.getTuningK3();

        for (int docId : potentialDocIds) {

            // Initialize Retrieve Status Value for the document relative to query
            double rsv = 0.0;
            int documentLength = indexer.getDocumentLengthMap().get(docId);

            for (int termId : queryTermFreq.keySet()) {
                // Term Frequency of the term in the document
                Integer termFreqDoc = indexer.getTermIncidenceMatrix().get(docId).get(termId);

                // Error Handling for invalid termId
                if (termFreqDoc == null) {
                    continue;
                }

                // Term Frequency of the term in the query
                Integer termFreqQuery = queryTermFreq.get(termId);

                double rsvDocTerm = indexer.getTermIdfScore().get(termId) * ((k1 + 1) * termFreqDoc) / (k1 * ((1 - b) + (b * (documentLength / indexer.getAverageDocumentLength()) + termFreqDoc)));
                double rsvQueryTerm = ((k3 + 1) * termFreqQuery) / (k3 + termFreqQuery);
                rsv += rsvDocTerm * rsvQueryTerm;
            }
            results.put(docId, new ProbabilisticResult(docId, rsv));
        }

        /*
         * Section 3:
         * FILTER the search result list
         */

        ArrayList<SearchResult> searchResults = new ArrayList<>();
        for (Document document : documents) {
            if (potentialDocIds.contains(document.getId())) {        // If the Id of the current Document is relevant
                // Get the result from the map
                ProbabilisticResult pr = results.get(document.getId());

                // Add that to the Final Result ArrayList
                searchResults.add(new SearchResult(document, pr.rsv));
            } else {
                // Add the irrelevant ones to the result and give it NaN (Not-a-Number) score
                searchResults.add(new SearchResult(document, Double.NaN));
            }
        }
        return TFIDFSearcher.finalizeSearchResult(searchResults, k);
    }

    public static class ProbabilisticResult implements Comparable {

        private int docId;
        private double rsv;

        public ProbabilisticResult(int docId, double rsv) {
            this.docId = docId;
            this.rsv = rsv;
        }

        public int getDocId() {
            return docId;
        }

        public void setDocId(int docId) {
            this.docId = docId;
        }

        public double getRsv() {
            return rsv;
        }

        public void setRsv(double rsv) {
            this.rsv = rsv;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProbabilisticResult that = (ProbabilisticResult) o;
            return docId == that.docId &&
                    Double.compare(that.rsv, rsv) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(docId, rsv);
        }

        @Override
        public int compareTo(@NotNull Object o) {
            if (!(o instanceof ProbabilisticResult)) {
                return 0;
            }
            return Double.compare(rsv, ((ProbabilisticResult) o).rsv);
        }
    }
}

/**
 * A class that responsible for the document indexing using BestMatch25 (BM25), a probability-based similarity matching
 * Getters are allowed only as we do not allow any reassignments from external classes.
 */
class ProbabilisticIndexer extends Indexer {
    /**
     * Mapping between documentId and Raw Document Length
     */
    private HashMap<Integer, Integer> documentLengthMap = new HashMap<>();

    /**
     * Mapping between termId and IDF score
     */
    private HashMap<Integer, Double> termIdfScore = new HashMap<>();

    /**
     * Mapping between docId and (Mapping between termId and termFrequency)
     */
    private HashMap<Integer, HashMap<Integer, Integer>> termIncidenceMatrix = new HashMap<>();

    /**
     * Tuning variable K1
     */
    private final double tuningK1;

    /**
     * Tuning variable B
     */
    private final double tuningB;

    /**
     * Tuning variable K3
     */
    private final double tuningK3;

    /**
     * Average Document Length in the corpus
     */
    private double averageDocumentLength = 0;

    /**
     * Instantiate the Probabilistic Indexer
     *
     * @param tuningK1 tuning variable K1
     * @param tuningB  tuning variable B
     * @param tuningK3 tuning variable K3
     */
    public ProbabilisticIndexer(double tuningK1, double tuningB, double tuningK3) {
        this.tuningK1 = tuningK1;
        this.tuningB = tuningB;
        this.tuningK3 = tuningK3;
    }

    /**
     * This method will be called when a new document start indexing.
     * Accumulating Document Length into the map and Sum them up to calculate average.
     *
     * @param docId     document Id
     * @param docLength the length of the actual document (Duplications and stop-words included)
     */
    @Override
    void onIndexingDocument(int docId, int docLength) {
        documentLengthMap.put(docId, docLength);
        averageDocumentLength += docLength;
    }

    /**
     * This method will be called when the indexing process is finished.
     * Convert floating raw term frequency in {@param tempDocVector} into integer into {@link ProbabilisticIndexer#termIncidenceMatrix}.
     * Then, calculate IDF and store it into {@link ProbabilisticIndexer#termIdfScore}.
     * Lastly, calculate average document length in the corpus {@link ProbabilisticIndexer#averageDocumentLength}.
     *
     * @param invertedTermDict mapping between (term string and its termId number)
     * @param tempDocVector    mapping between (documentId and mapping between (termId and value such as TF-IDF or raw termFreq))
     */
    @Override
    void onPostIndexing(HashMap<String, Integer> invertedTermDict, HashMap<Integer, HashMap<Integer, Double>> tempDocVector) {
        // Total document size
        int totalDocument = tempDocVector.size();

        // Iterates thru every document
        for (int docId : tempDocVector.keySet()) {
            // Temporary Map for term frequency
            HashMap<Integer, Integer> termFreqMap = new HashMap<>();
            for (int termId : tempDocVector.get(docId).keySet()) {
                // Convert the raw termFreq into Integer and put it into the temporary map.
                termFreqMap.put(termId, tempDocVector.get(docId).get(termId).intValue());
            }
            // Put both docId and termFreqMap into the new termIncidenceMatrix
            termIncidenceMatrix.put(docId, termFreqMap);
        }

        // Calculate all IDF into termIdfScore map
        for (int termId : super.termDocFrequency.keySet()) {
            termIdfScore.put(termId, TfIdfMathUtil.calculateInvertedDocFrequency(totalDocument, termDocFrequency.get(termId)));
        }

        // Calculate the average document length
        averageDocumentLength = averageDocumentLength / (double) totalDocument;
    }

    /**
     * Builder class for Probabilistic Indexer
     */
    public static class Builder {
        private final List<Document> documents;
        private final Set<String> stopWords;
        private final double k1;
        private final double b;
        private final double k3;

        public Builder(List<Document> documents, Set<String> stopWords, double k1, double b, double k3) {
            this.documents = documents;
            this.stopWords = stopWords;
            this.k1 = k1;
            this.b = b;
            this.k3 = k3;
        }

        public ProbabilisticIndexer build() {
            ProbabilisticIndexer indexer = new ProbabilisticIndexer(k1, b, k3);
            // indexer.setDebuggerInterface(listener);
            indexer.start(documents, stopWords);
            return indexer;
        }
    }

    public HashMap<Integer, Integer> getDocumentLengthMap() {
        return documentLengthMap;
    }

    public double getTuningK1() {
        return tuningK1;
    }

    public double getTuningB() {
        return tuningB;
    }

    public double getTuningK3() {
        return tuningK3;
    }

    public double getAverageDocumentLength() {
        return averageDocumentLength;
    }

    public HashMap<Integer, Double> getTermIdfScore() {
        return termIdfScore;
    }

    public HashMap<Integer, HashMap<Integer, Integer>> getTermIncidenceMatrix() {
        return termIncidenceMatrix;
    }
}