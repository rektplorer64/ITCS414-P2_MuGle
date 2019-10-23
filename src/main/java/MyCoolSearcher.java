import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MyCoolSearcher extends Searcher {

    private final ProbabilisticIndexer indexer;

    /**
     * Default constructor. Load raw documents into Document objects in memory
     *
     * @param docFilename
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

        HashMap<Integer, ProbabilisticResult> results = new HashMap<>();
        double k1 = indexer.getTuningK1();
        double b = indexer.getTuningB();
        double k3 = indexer.getTuningK3();

        for (int docId : potentialDocIds) {
            Double rsv = 0.0;
            int documentLength = indexer.getDocumentLengthMap().get(docId);

            for (int termId: queryTermFreq.keySet()) {
                Integer termFreqDoc = indexer.getTermIncidenceMatrix().get(docId).get(termId);
                Integer termFreqQuery = queryTermFreq.get(termId);

                if (termFreqDoc == null){
                    continue;
                }

                double rsvDocTerm = indexer.getTermIdfScore().get(termId) * ((k1 + 1) * termFreqDoc) / (k1 * ((1 - b) + (b * (documentLength / indexer.getAverageDocumentLength()) + termFreqDoc)));
                double rsvQueryTerm = ((k3 + 1) * termFreqQuery) / (k3 + termFreqQuery);
                rsv += rsvDocTerm * rsvQueryTerm;
            }
            results.put(docId, new ProbabilisticResult(docId, rsv));
        }

        ArrayList<SearchResult> searchResults = new ArrayList<>();
        for (Document document : documents) {
            if (potentialDocIds.contains(document.getId())) {        // If the Id of the current Document is relevant
                ProbabilisticResult pr = results.get(document.getId());

                // System.out.println("RSV of #" + document.getId() + ": " + pr.rsv);

                // Add that to the Final Result ArrayList
                searchResults.add(new SearchResult(document, pr.rsv));
            } else {
                // Add the irrelevant ones to the result and give it NaN (Not-a-Number) score
                searchResults.add(new SearchResult(document, Double.NaN));
            }
        }
        return TFIDFSearcher.finalizeSearchResult(searchResults, k);
    }

    public static class ProbabilisticResult implements Comparable{

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
            if (!(o instanceof ProbabilisticResult)){
                return 0;
            }
            return Double.compare(rsv, ((ProbabilisticResult) o).rsv);
        }
    }
}

class ProbabilisticIndexer extends Indexer{
    private HashMap<Integer, Integer> documentLengthMap = new HashMap<>();
    private HashMap<Integer, Double> termIdfScore = new HashMap<>();
    private HashMap<Integer, HashMap<Integer, Integer>> termIncidenceMatrix = new HashMap<>();

    private final double tuningK1;
    private final double tuningB;
    private final double tuningK3;
    private double averageDocumentLength = 0;

    public ProbabilisticIndexer(double tuningK1, double tuningB, double tuningK3) {
        this.tuningK1 = tuningK1;
        this.tuningB = tuningB;
        this.tuningK3 = tuningK3;
    }

    @Override
    void onIndexingDocument(int docId, int docLength) {
        documentLengthMap.put(docId, docLength);
        averageDocumentLength += docLength;
    }

    @Override
    void onPostIndexing(HashMap<String, Integer> invertedTermDict, HashMap<Integer, HashMap<Integer, Double>> tempDocVector) {

        int totalDocument = tempDocVector.size();

        for(int docId : tempDocVector.keySet()) {
            HashMap<Integer, Integer> termFreqMap = new HashMap<>();
            for(int termId : tempDocVector.get(docId).keySet()){
                termFreqMap.put(termId,tempDocVector.get(docId).get(termId).intValue());
            }
            termIncidenceMatrix.put(docId, termFreqMap);
        }

        for (int termId : super.termDocFrequency.keySet()) {
            termIdfScore.put(termId, TfIdfMathUtil.calculateInvertedDocFrequency(totalDocument, termDocFrequency.get(termId)));
        }
        averageDocumentLength = averageDocumentLength / (double) totalDocument;
    }

    public static class Builder {
        private final List<Document> documents;
        private final Set<String> stopWords;
        private final double k1;
        private final double b;
        private final double k3;
        // private TFIDFSearcher.WeightCalculationListener listener;

        public Builder(List<Document> documents, Set<String> stopWords, double k1, double b, double k3){
            this.documents = documents;
            this.stopWords = stopWords;
            this.k1 = k1;
            this.b = b;
            this.k3 = k3;
        }

        // public MyCoolSearcher.Builder setDebuggerInterface(TFIDFSearcher.WeightCalculationListener listener) {
        //     this.listener = listener;
        //     return this;
        // }

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