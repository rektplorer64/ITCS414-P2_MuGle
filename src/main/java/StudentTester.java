/*
This Code is modified by Section 1 Students of Mahidol University, the Faculty of ICT, 2019
as part of the second project of ITCS414 - Information Retrieval and Storage.

The group consists of
    1. Krittin      Chatrinan       ID 6088022
    2. Anon         Kangpanich      ID 6088053
    3. Tanawin      Wichit          ID 6088221
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Class containing a set of test-cases.
 *
 * @author Dr. Suppawong Tuarob (copyrighted)
 */
public class StudentTester {

    //*********************** DO NOT MODIFY THESE VARIABLES *****************************//
    public static final String testCorpus = "./data/lisa";
    public static final int k = 10;
    public static final String[] testQueries = new String[]{
            ""
            , "Information Retrieval"
            , "Machine Learning"
            , "Deep Learning"
            , "I AM INTERESTED IN INFORMATION ON THE PROVISION OF CURRENT AWARENESS BULLETINS, ESPECIALLY SDI SERVICES IN ANY INSTITUTION, E.G. ACADEMIC LIBRARIES, INDUSTRY, AND IN ANY SUBJECT FIELD. SDI, SELECTIVE DISSEMINATION OF INFORMATION, CURRENT AWARENESS BULLETINS, INFORMATION BULLETINS."
            , "THE WHITE HOUSE CONFERENCE ON LIBRARY AND INFORMATION SERVICES, 1979. SUMMARY, MARCH 1980. FOR AN ABSTRACT OF THIS REPORT SEE 81/795. REPORT NOT AVAILABLE FROM NTIS."
    };

    //*********************** DO NOT MODIFY THIS METHOD *****************************//
    public static void testJaccardSearcher(String corpus) {
        System.out.println("@@@ Testing Jaccard-based documents searcher on " + corpus);
        String documentFilename = corpus + "/documents.txt";
        long startTime = System.currentTimeMillis();
        //initialize search engine
        Searcher searcher = new JaccardSearcher(documentFilename);
        for (String query : testQueries) {
            List<SearchResult> results = searcher.search(query, k);
            System.out.println("@@@ Results: " + (query.length() > 50 ? query.substring(0, 50) + "..." : query));
            Searcher.displaySearchResults(results);
            System.out.println();
        }

        long endTime = System.currentTimeMillis();
        System.out.println("@@@ Total time used: " + (endTime - startTime) + " milliseconds.");
    }

    //*********************** DO NOT MODIFY THIS METHOD *****************************//
    public static void testTFIDFSearcher(String corpus) {
        System.out.println("@@@ Testing TFIDF-based documents searcher on " + corpus);
        String documentFilename = corpus + "/documents.txt";
        long startTime = System.currentTimeMillis();
        //initialize search engine
        Searcher searcher = new TFIDFSearcher(documentFilename);
        for (String query : testQueries) {
            List<SearchResult> results = searcher.search(query, k);
            System.out.println("@@@ Results: " + (query.length() > 50 ? query.substring(0, 50) + "..." : query));
            Searcher.displaySearchResults(results);
            System.out.println();
        }

        long endTime = System.currentTimeMillis();
        System.out.println("@@@ Total time used: " + (endTime - startTime) + " milliseconds.");
    }

    //*********************** DO NOT MODIFY THIS METHOD *****************************//
    public static void testCompareTwoSearchersOnSomeQueries(String corpus) {
        System.out.println("@@@ Comparing two searchers on some queries in " + corpus);
        long startTime = System.currentTimeMillis();
        SearcherEvaluator eval = new SearcherEvaluator(testCorpus);
        Searcher jSearcher = new JaccardSearcher(testCorpus + "/documents.txt");
        Searcher tSearcher = new TFIDFSearcher(testCorpus + "/documents.txt");

        int[] qIndexes = new int[3];
        qIndexes[0] = 0;
        qIndexes[1] = eval.getQueries().size() / 2;
        qIndexes[2] = eval.getQueries().size() - 1;

        for (int qIndex : qIndexes) {
            System.out.println("@@@ Query: " + eval.getQueries().get(qIndex));
            double[] jResults = eval.getQueryPRF(eval.getQueries().get(qIndex), jSearcher, k);
            double[] tResults = eval.getQueryPRF(eval.getQueries().get(qIndex), tSearcher, k);
            System.out.println("\tJaccard (P,R,F): " + Arrays.toString(jResults));
            System.out.println("\tTFIDF (P,R,F): " + Arrays.toString(tResults));
        }

        long endTime = System.currentTimeMillis();
        System.out.println("@@@ Total time used: " + (endTime - startTime) + " milliseconds.");
    }

    //*********************** DO NOT MODIFY THIS METHOD *****************************//
    public static void testCompareTwoSearchersOnAllQueries(String corpus) {
        System.out.println("@@@ Comparing two searchers on all the queries in " + corpus);
        long startTime = System.currentTimeMillis();
        SearcherEvaluator s = new SearcherEvaluator(corpus);
        Searcher jSearcher = new JaccardSearcher(testCorpus + "/documents.txt");
        Searcher tSearcher = new TFIDFSearcher(testCorpus + "/documents.txt");

        double[] jResults = s.getAveragePRF(jSearcher, k);
        double[] tResults = s.getAveragePRF(tSearcher, k);
        System.out.println("@@@ Jaccard: " + Arrays.toString(jResults));
        System.out.println("@@@ TFIDF: " + Arrays.toString(tResults));
        long endTime = System.currentTimeMillis();
        System.out.println("@@@ Total time used: " + (endTime - startTime) + " milliseconds.");
    }

    public static void testYourSearcher(String corpus) {
        // TODO: YOUR CODE HERE (BONUS)

        SearcherEvaluator evaluator = new SearcherEvaluator(corpus);

        System.out.println("\n\nTEST PHASE 1:");
        System.out.println("@@@ Testing Probabilistic BM25 documents searcher on " + corpus);
        String documentFilename = corpus + "/documents.txt";

        long startTimeBM25 = System.currentTimeMillis();

        // initialize search engine
        Searcher myCoolSearcher = new MyCoolSearcher(documentFilename);
        for (String query : testQueries) {
            List<SearchResult> results = myCoolSearcher.search(query, k);

            System.out.println("@@@ Results: " + (query.length() > 50 ? query.substring(0, 50) + "..." : query));
            Searcher.displaySearchResults(results);
            System.out.println();
        }
        System.out.println("@@@ Total time used: " + (System.currentTimeMillis() - startTimeBM25) + " milliseconds.");

        double[] myCoolSearcherResults = evaluator.getAveragePRF(myCoolSearcher, k);
        System.out.println("@@@ Evaluation of MyCoolSearcher: " + Arrays.toString(myCoolSearcherResults));

        /*
         * INITIALIZING COMPARISON TEST
         */

        System.out.println("\n\nTEST PHASE 2:");
        System.out.println("Comparing with other two searchers on all the queries in " + corpus);

        TreeMap<Double, String> indexingMethodRankings = new TreeMap<>();

        System.out.println("\n*** MyCoolSearcher: BM25 ***");
        Searcher myCoolSearcher2 = new MyCoolSearcher(testCorpus + "/documents.txt");
        final double[] myCoolSearcherEvalResult = evaluator.getAveragePRF(myCoolSearcher2, k);
        System.out.println("@@ Evaluation of MyCoolSearcher: " + Arrays.toString(myCoolSearcherEvalResult));
        indexingMethodRankings.put(myCoolSearcherEvalResult[2], "MyCoolSearcher: BM25");

        System.out.println("\n*** Jaccard Coefficient ***");
        Searcher jSearcher = new JaccardSearcher(testCorpus + "/documents.txt");
        final double[] jSearcherEvalResult = evaluator.getAveragePRF(jSearcher, k);
        System.out.println("@@ Evaluation of Jaccard: " + Arrays.toString(jSearcherEvalResult));
        indexingMethodRankings.put(jSearcherEvalResult[2], "Jaccard Coefficient   ");

        System.out.println("\n*** TF-IDF w/ Cosine Similarity ***");
        Searcher tSearcher = new TFIDFSearcher(testCorpus + "/documents.txt");
        final double[] tSearcherEvalResult = evaluator.getAveragePRF(tSearcher, k);
        System.out.println("@@ Evaluation of TF-IDF: " + Arrays.toString(tSearcherEvalResult));
        indexingMethodRankings.put(tSearcherEvalResult[2], "TF-IDF w/ Cosine    ");

        NavigableMap<Double, String> nvMap = indexingMethodRankings.descendingMap();
        System.out.println("\n--- TEST SUMMARY ---");
        System.out.println("Search Systems ranked by F1");

        int rankCount = 1;
        for (Map.Entry<Double, String> e : nvMap.entrySet()) {
            System.out.println("Rank " + (rankCount++) + ":\t" + e.getValue() + "\t\t\t@ F1 = " + e.getKey());
        }
    }

    public static void iterativelySearchTopK(String corpus, int maxK) {

        PrintWriter jaccardWriter = null, tfIdfWriter = null, bm25Writer = null;
        try {
            jaccardWriter = new PrintWriter("evaluationTo" + maxK + "-JaccardCoeff.csv", StandardCharsets.UTF_8);
            tfIdfWriter = new PrintWriter("evaluationTo" + maxK + "-TfIdf.csv", StandardCharsets.UTF_8);
            bm25Writer = new PrintWriter("evaluationTo" + maxK + "-BM25.csv", StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        SearcherEvaluator s = new SearcherEvaluator(corpus);

        Searcher jSearcher = new JaccardSearcher(testCorpus + "/documents.txt");
        Searcher tSearcher = new TFIDFSearcher(testCorpus + "/documents.txt");
        Searcher bm25Searcher = new MyCoolSearcher(testCorpus + "/documents.txt");

        for (int k = 1; k <= maxK; k++) {
            double[] jResults = s.getAveragePRF(jSearcher, k);
            assert jaccardWriter != null;
            jaccardWriter.println(k + ", " + jResults[0] + ", " + jResults[1] + ", " + jResults[2]);

            double[] tResults = s.getAveragePRF(tSearcher, k);
            assert tfIdfWriter != null;
            tfIdfWriter.println(k + ", " + tResults[0] + ", " + tResults[1] + ", " + tResults[2]);

            double[] bm25Results = s.getAveragePRF(bm25Searcher, k);
            assert bm25Writer != null;
            bm25Writer.println(k + ", " + bm25Results[0] + ", " + bm25Results[1] + ", " + bm25Results[2]);
        }
        jaccardWriter.close();
        tfIdfWriter.close();
        bm25Writer.close();
    }

    public static void main(String[] args) {
        /********************* Uncomment test cases you want to test ***************/
        testJaccardSearcher(testCorpus);
        // testTFIDFSearcher(testCorpus);
        // testCompareTwoSearchersOnSomeQueries(testCorpus);
        // testCompareTwoSearchersOnAllQueries(testCorpus);

        // iterativelySearchTopK(testCorpus, 50);

        //********** BONUS **************//
        // testYourSearcher(testCorpus);
        //*******************************//
    }

}
