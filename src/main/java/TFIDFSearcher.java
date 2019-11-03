/*
This Code is modified by Section 1 Students of Mahidol University, the Faculty of ICT, 2019
as part of the second project of ITCS414 - Information Retrieval and Storage.

The group consists of
    1. Krittin      Chatrinan       ID 6088022
    2. Anon         Kangpanich      ID 6088053
    3. Tanawin      Wichit          ID 6088221
 */

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * This class facilitates an algorithm to retrieve documents based on its TF-IDF weight with cosine similarity score.
 */
public class TFIDFSearcher extends Searcher {

    /**
     * An instance of indexer which contains all indexed docs and terms.
     */
    private VectorSpaceModelIndexer indexer;

    /**
     * Main constructor for the Searcher
     *
     * @param docFilename the name of a file that contains documents in it
     */
    public TFIDFSearcher(String docFilename) {
        // TODO: Your Code Here
        // Please take a look at the other constructor
        this(docFilename, null);
    }

    /**
     * Constructor for Unit testing
     *
     * @param docFilename the name of a file that contains documents in it
     * @param w           debugging interface
     */
    public TFIDFSearcher(String docFilename, WeightCalculationListener w) {
        super(docFilename);
        // Instantiate the indexer by using Builder class
        // NOTE:
        // Please read the code, we know it is over-engineered but still.
        // We just only want to create a reusable code.
        VectorSpaceModelIndexer.Builder indexerBuilder = new VectorSpaceModelIndexer.Builder(documents, stopWords);
        indexerBuilder.setDebuggerInterface(w);
        indexer = indexerBuilder.build();
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
        // TODO: Your Code Here

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
         * CONSTRUCT A NEW DOCUMENT VECTOR for the Given Query
         */

        // Create a new Document Vector for the Query
        DocumentVector queryDv = new DocumentVector(-1);
        for (int termId : queryTermFreq.keySet()) {     // Iterates thru all elements inside the queryTermFreq map
            queryDv.getVector().put(termId, Double.valueOf(queryTermFreq.get(termId)));     // Add each item to the Query Vector
        }

        // Calculate TF-IDF Weight; the outcome is present in queryDv (queryDv got modified)
        VectorSpaceModelIndexer.calculateTfIdfWeight(queryDv,
                indexer.getDocumentVectors().size(),
                indexer.getTermDocFrequency());

        // Calculate the norm of the Query vector
        queryDv.setNorm(TfIdfMathUtil.calculateNorm(queryDv.getVector()));

        /*
         * Section 3:
         * Calculate COSINE SIMILARITY between the Query and all POTENTIAL DOCUMENTS
         * Manipulate irrelevant documents so that the program behave correctly
         * when there is totally no matched result.
         */

        // ArrayList for the Final Result
        ArrayList<SearchResult> searchResults = new ArrayList<>();

        // Iterate thru all Document
        for (Document document : documents) {
            if (potentialDocIds.contains(document.getId())) {        // If the Id of the current Document is relevant
                DocumentVector docVector = indexer.getDocumentVectors().get(document.getId());

                // Calculate the Cosine Similarity Score using Query and Vector
                double score = TfIdfMathUtil.calculateCosineSimilarity(queryDv, docVector);

                // Add that to the Final Result ArrayList
                searchResults.add(new SearchResult(document, score));
            } else {
                // Add the irrelevant ones to the result and give it NaN (Not-a-Number) score
                searchResults.add(new SearchResult(document, Double.NaN));
            }
        }

        return finalizeSearchResult(searchResults, k);
    }

    /**
     * Finalize the Search Result ArrayList by sorting correctly and trim to k.
     *
     * @param searchResults ArrayList of Search Result
     * @param k             number of results to be shown
     * @return the Final Search Result ArrayList
     */
    static List<SearchResult> finalizeSearchResult(ArrayList<SearchResult> searchResults, int k) {

        // Collections.sort(searchResults);
        // Sort the ArrayList
        searchResults.sort(new Comparator<SearchResult>() {
            /**
             * Compare both Documents by the given logic flow below
             *
             * @param o1    a SearchResult
             * @param o2    another SearchResult
             * @return an integer indicate the equality or value difference
             */
            @Override
            public int compare(SearchResult o1, SearchResult o2) {
                // If either or both of them are NaN, mark it as the Lowest Value (Lower than 0)
                double a = (Double.isNaN(o1.getScore())) ? Double.MIN_VALUE : o1.getScore();
                double b = (Double.isNaN(o2.getScore())) ? Double.MIN_VALUE : o2.getScore();

                // Compare both together
                int compare = Double.compare(b, a);
                // If the comparison is zero / both are equal, then compare these by its Document Id
                if (compare == 0) {
                    return o1.getDocument().getId() - o2.getDocument().getId();
                }
                return compare;
            }
        });

        // Collections.reverse(searchResults);

        // Returns only a Sub list of the First k elements
        return searchResults.subList(0, Math.min(searchResults.size(), k));
    }

    /**
     * Fetch Document objects which relevant to the given set of docId from the super class's List of Documents.
     *
     * @param docIds a Set of document Id
     * @return a Mapping between document Id and an actual Document object
     */
    private Map<Integer, Document> fetchDocumentsByIds(Set<Integer> docIds) {
        HashMap<Integer, Document> documentObjMap = new HashMap<>();

        // Iterates thru every single Document in the super's List.
        for (Document doc : super.documents) {
            // If it is relevant, put it in the map.
            if (docIds.contains(doc.getId())) {
                documentObjMap.put(doc.getId(), doc);
            }
        }
        return documentObjMap;
    }

    /**
     * This Data class represents a Document Vector.
     */
    public static class DocumentVector implements Comparable<Integer> {
        /**
         * an integer for Document Id
         */
        private int docId;

        /**
         * a double precision floating point number for the Norm/Size of the Vector
         */
        private double norm;

        /**
         * Mapping between (termId: Int) and (weightScore: Double)
         */
        private HashMap<Integer, Double> vector;

        DocumentVector(int docId) {
            this(docId, new HashMap<>());
        }

        DocumentVector(int docId, HashMap<Integer, Double> vector) {
            this.docId = docId;
            this.vector = vector;
        }

        public int getDocId() {
            return docId;
        }

        public void setDocId(int docId) {
            this.docId = docId;
        }

        public HashMap<Integer, Double> getVector() {
            return vector;
        }

        public void setVector(HashMap<Integer, Double> vector) {
            this.vector = vector;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DocumentVector vector = (DocumentVector) o;
            return docId == vector.docId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(docId);
        }

        @Override
        public int compareTo(@NotNull Integer o) {
            return this.docId - o;
        }

        public double getNorm() {
            return norm;
        }

        public void setNorm(double norm) {
            this.norm = norm;
        }
    }

    /**
     * An interface for debugging TF-IDF weight calculation
     */
    public static interface WeightCalculationListener {
        boolean onLoopIterationCheckCondition(int docId);

        void onCalculation(TFIDFSearcher.DocumentVector dv, int totalDocument, Map<Integer, Integer> termDocFrequency);

        void onCalculated(TFIDFSearcher.DocumentVector dv);
    }

    /**
     * A class that responsible for the document indexing.
     * Getters are allowed only as we do not allow any reassignments from external classes.
     */
    abstract static class Indexer {
        /**
         * Mapping between (term: String) and (termId: Int)
         */
        protected HashMap<String, Integer> termDict = new HashMap<>();
        /**
         * Mapping between (termId: Int) and (termFreq: Int)
         */
        protected HashMap<Integer, Integer> termDocFrequency = new HashMap<>();
        /**
         * Mapping between (termId: Int) and (docIdSet: HashSet{@literal <Int>})
         */
        protected HashMap<Integer, HashSet<Integer>> postingLists = new HashMap<>();
        /**
         * Integer that counts all term frequency
         */
        protected int totalTermFrequency = 0;

        /**
         * Responsible for the initialization of an Indexing process
         * Be careful for this initialization, as it can be expensive for a large dataset.
         *
         * @param documents List of Document Objects
         * @param stopWords Set of the Stop word
         */
        protected void start(List<Document> documents, Set<String> stopWords) {
            // Temporary Mapping between (docId: Int) and (Mapping between (termId: Int) and (scoreWeight: Double))
            HashMap<Integer, HashMap<Integer, Double>> tempDocVector = new HashMap<>();

            // Mapping between (term: String) and (termId: Int)
            HashMap<String, Integer> invertedTermDict = new HashMap<>();

            // Initialize the term Id counter
            int termIdCounter = 0;

            // Iterates thru all Documents in the param List
            for (final Document document : documents) {
                // Create an empty Vector for the Document
                tempDocVector.put(document.getId(), new HashMap<>());

                onIndexingDocument(document.getId(), document.getRawText().length());

                // Initialize the set that memorizes the term in for the document. (We want to track document frequency of the term)
                HashSet<Integer> exploredTermSet = new HashSet<>();
                for (final String token : document.getTokens()) {            // We iterate thru all tokens in the document
                    totalTermFrequency++;                                    // Add up total frequency

                    int currentTermId;                                       // Variable for storing termId
                    if (!invertedTermDict.containsKey(token)) {              // If we never seen this token String before,
                        // Add that token String to the termDict;
                        // also increment termIdCounter by 1.
                        invertedTermDict.put(token, ++termIdCounter);

                        // Assign the counter Id as the Current termId
                        currentTermId = termIdCounter;

                        // Prepare the PostingList for this termId
                        postingLists.put(currentTermId, new HashSet<>());
                    } else {                                                // Otherwise
                        currentTermId = invertedTermDict.get(token);         // We get the termId by token String.
                    }

                    // We add docId to the PostingList of termId.
                    postingLists.get(currentTermId).add(document.getId());

                    // Mark the Term as explored
                    exploredTermSet.add(currentTermId);

                    // Get the Document Vector from the docId
                    HashMap<Integer, Double> docTermScoreMap = tempDocVector.get(document.getId());
                    if (!docTermScoreMap.containsKey(currentTermId)) {      // If the current termId is not present in the Document Vector
                        // Put it as 0; because we want to treat it as frequency for now.
                        docTermScoreMap.put(currentTermId, 0.0);
                    }
                    // Crank the Score up by 1; We treat score as token frequency for now!
                    docTermScoreMap.put(currentTermId, docTermScoreMap.get(currentTermId) + 1.0);
                }

                // For every explored terms in this document
                for (int termId : exploredTermSet) {
                    if (!termDocFrequency.containsKey(termId)) {        // If the termId is not exists in the map
                        termDocFrequency.put(termId, 0);                // We initialize document Frequency as 0.
                    }
                    termDocFrequency.put(termId, termDocFrequency.get(termId) + 1);     // We add up document Frequency by 1.
                }
            }

            populateTermDict(invertedTermDict);
            onPostIndexing(invertedTermDict, tempDocVector);
        }

        /**
         * Populate field termDict by simply assign it.
         *
         * @param invertedTermDict Mapping between (term: String) and (termId: Int)
         */
        private void populateTermDict(HashMap<String, Integer> invertedTermDict) {
            // for (String term : invertedTermDict.keySet()) {
            //     termDict.put(invertedTermDict.get(term), term);
            // }
            termDict = invertedTermDict;
            invertedTermDict = null;
        }

        /**
         * The method will invoked when the indexer start indexing a document
         *
         * @param docId     document Id
         * @param docLength the length of the actual document (Duplications and stop-words included)
         */
        abstract void onIndexingDocument(int docId, int docLength);

        /**
         * The method will be invoked after the indexing process is entirely completed.
         *
         * @param invertedTermDict mapping between (term string and its termId number)
         * @param tempDocVector    mapping between (documentId and mapping between (termId and value such as TF-IDF or raw termFreq))
         */
        abstract void onPostIndexing(HashMap<String, Integer> invertedTermDict, HashMap<Integer, HashMap<Integer, Double>> tempDocVector);

        HashMap<String, Integer> getTermDict() {
            return termDict;
        }

        HashMap<Integer, Integer> getTermDocFrequency() {
            return termDocFrequency;
        }

        HashMap<Integer, HashSet<Integer>> getPostingLists() {
            return postingLists;
        }

        public int getTotalTermFrequency() {
            return totalTermFrequency;
        }

    }
}

/**
 * A class that responsible for the document indexing using TF-IDF Weight with Cosine Similarity.
 * Getters are allowed only as we do not allow any reassignments from external classes.
 */
class VectorSpaceModelIndexer extends TFIDFSearcher.Indexer {

    /**
     * Debugging Interface for MyCoolSearcher.Indexer
     */
    protected TFIDFSearcher.WeightCalculationListener debuggerInterface;

    /**
     * Mapping between (docId: Int) and (documentVector: DocumentVector)
     */
    protected HashMap<Integer, TFIDFSearcher.DocumentVector> documentVectors = new HashMap<>();

    /**
     * Constructor for the MyCoolSearcher.Indexer class; must be instantiate via {@link Builder}
     */
    private VectorSpaceModelIndexer() {
    }

    /**
     * It is not necessary to do anything on indexing a new document.
     *
     * @param docId     document Id
     * @param docLength the length of the actual document (Duplications and stop-words included)
     */
    @Override
    void onIndexingDocument(int docId, int docLength) {
    }

    /**
     * Populate DocumentVectors and Calculate Norm and TF-IDF of all elements in each vectors.
     *
     * @param invertedTermDict mapping between (term string and its termId number)
     * @param tempDocVector    mapping between (documentId and mapping between (termId and value such as TF-IDF or raw termFreq))
     */
    @Override
    void onPostIndexing(HashMap<String, Integer> invertedTermDict, HashMap<Integer, HashMap<Integer, Double>> tempDocVector) {
        populateDocumentVectors(tempDocVector);
        calculateWeightsAndNormForAllVectors();
    }

    /**
     * Calculate IF-IDF Weight Score and Norm for every vector
     */
    private void calculateWeightsAndNormForAllVectors() {
        // For every Vector in the HashMap
        for (TFIDFSearcher.DocumentVector vector : documentVectors.values()) {

            boolean isDebuggingTarget = (debuggerInterface != null) && debuggerInterface.onLoopIterationCheckCondition(vector.getDocId());

            if (isDebuggingTarget) {
                debuggerInterface.onCalculation(vector, documentVectors.size(), termDocFrequency);
            }

            // We calculate TF-IDF Weight and Norm of each vector.
            calculateTfIdfWeight(vector, documentVectors.size(), termDocFrequency);

            // Calculate the norm and set it to the Document Vector
            vector.setNorm(TfIdfMathUtil.calculateNorm(vector.getVector()));

            if (isDebuggingTarget) {
                debuggerInterface.onCalculated(vector);
            }

            // System.out.println("Norm of Doc #" + vector.getDocId() + "\t= " + norm);
            // System.out.println();
        }
    }

    /**
     * Calculate TF-IDF Weight of the given Document Vector.
     *
     * @param dv               a Document Vector which had its Score Map populated by term frequencies
     * @param totalDocument    total number of documents in the dataset
     * @param termDocFrequency mapping between term Id and document Frequency
     */
    static void calculateTfIdfWeight(TFIDFSearcher.DocumentVector dv, int totalDocument, Map<Integer, Integer> termDocFrequency) {
        // Iterates every Vector in the Map
        for (Map.Entry<Integer, Double> termIdFreqEntry : dv.getVector().entrySet()) {
            // We get term frequency from the entry (Convert it to an Int)
            final int termFrequency = termIdFreqEntry.getValue().intValue();

            // Calculate Term Frequency (TF) Weight
            double tfWeight = TfIdfMathUtil.calculateTermFrequency(termFrequency);

            // Calculate Inverted Document Frequency (IDF) Weight
            double idfWeight = TfIdfMathUtil
                    .calculateInvertedDocFrequency(totalDocument
                            , termDocFrequency.get(termIdFreqEntry.getKey()));

            // Actual final score
            double tfIdfWeight = tfWeight * idfWeight;

            // Set the actual score to the Map
            termIdFreqEntry.setValue(tfIdfWeight);
        }
    }

    public void setDebuggerInterface(TFIDFSearcher.WeightCalculationListener debuggerInterface) {
        this.debuggerInterface = debuggerInterface;
    }

    HashMap<Integer, TFIDFSearcher.DocumentVector> getDocumentVectors() {
        return documentVectors;
    }

    /**
     * Populate field DocumentVector Map.
     *
     * @param tempDocVector Mapping between (docId: Int) and (Mapping between (termId: Int) and (weightScore: Double))
     */
    private void populateDocumentVectors(HashMap<Integer, HashMap<Integer, Double>> tempDocVector) {
        for (Map.Entry<Integer, HashMap<Integer, Double>> entry : tempDocVector.entrySet()) {
            // Convert each entry to a DocumentVector and put it to the field map
            documentVectors.put(entry.getKey(), new TFIDFSearcher.DocumentVector(entry.getKey(), entry.getValue()));
        }

        // Clear the reference and hope that GC will clean it up
        tempDocVector = null;
    }

    /**
     * Builder for the VSM MyCoolSearcher.Indexer
     */
    public static class Builder {
        private final List<Document> documents;
        private final Set<String> stopWords;
        private TFIDFSearcher.WeightCalculationListener listener;

        public Builder(List<Document> documents, Set<String> stopWords) {
            this.documents = documents;
            this.stopWords = stopWords;
        }

        public Builder setDebuggerInterface(TFIDFSearcher.WeightCalculationListener listener) {
            this.listener = listener;
            return this;
        }

        public VectorSpaceModelIndexer build() {
            VectorSpaceModelIndexer indexer = new VectorSpaceModelIndexer();
            indexer.setDebuggerInterface(listener);
            indexer.start(documents, stopWords);
            return indexer;
        }
    }
}

/**
 * This class contains helper methods that facilitates TF-IDF scoring
 */
class TfIdfMathUtil {

    /**
     * Calculates Term Frequency (TF) Weight of the given Term Frequency
     *
     * @param frequency term frequency in a document or query
     * @return TF weight
     */
    static double calculateTermFrequency(int frequency) {
        if (frequency == 0) {    // If it is ZERO, don't bother calculating it.
            return 0d;
        }
        return 1d + Math.log10(frequency);
    }

    /**
     * Calculates Inverted Document Frequency (IDF) weight of the Document Frequency of the The given term.
     *
     * @param totalDocument actual number of the documents in the dataset
     * @param docFrequency  actual number of the documents that contain a term
     * @return IDF weight
     */
    static double calculateInvertedDocFrequency(int totalDocument, int docFrequency) {
        return Math.log10(1d + ((double) totalDocument / (double) docFrequency));
    }

    /**
     * Calculates Norm of the given Mapping of Integer and Score/Weight
     *
     * @param scoreVector Map of termId Int and Score/Weight Float
     * @return calculated score matrix
     */
    static Double calculateNorm(Map<Integer, Double> scoreVector) {
        double sum = 0;
        for (int termId : scoreVector.keySet()) {
            double weight = scoreVector.get(termId);
            sum += Math.pow(weight, 2);
        }
        return Math.sqrt(sum);
    }

    /**
     * Calculate a matrix to get the norm of the vector for each entry inside the key set of scoreMatrix
     *
     * @param scoreMatrix Mapping between docId and Map of termId and score/weight
     * @return calculated vector norm for each docId
     */
    static Map<Integer, Double> calculateNormMatrix(Map<Integer, Map<Integer, Double>> scoreMatrix) {
        Map<Integer, Double> normMap = new TreeMap<>();
        for (Map.Entry<Integer, Map<Integer, Double>> entry : scoreMatrix.entrySet()) {
            normMap.put(entry.getKey(), calculateNorm(entry.getValue()));
        }
        return normMap;
    }

    /**
     * Calculate the cosine similarity between two Document Vectors
     *
     * @param query vector of Query
     * @param doc   vector of Document
     * @return the cosine similarity
     */
    static double calculateCosineSimilarity(TFIDFSearcher.DocumentVector query, TFIDFSearcher.DocumentVector doc) {
        double sum = 0;
        for (int termId : query.getVector().keySet()) {
            if (doc.getVector().get(termId) == null) {
                continue;
            }
            double queryScore = query.getVector().get(termId);
            double docScore = doc.getVector().get(termId);
            // System.out.println("q = " + queryScore + "\t d = " + docScore);
            sum += queryScore * docScore;
        }

        // System.out.println("Sum = " + sum + "\tQuery Norm = " + query.getNorm() + "\tDoc Norm = " + doc.getNorm() + "\n");
        return sum / (doc.getNorm() * query.getNorm());
    }
}
