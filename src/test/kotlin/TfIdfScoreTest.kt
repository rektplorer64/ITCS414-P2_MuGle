import org.junit.jupiter.api.*

import org.junit.jupiter.api.Assertions.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

internal class TfIdfScoreTest {

    private lateinit var tfIdfSearcher: TFIDFSearcher

    @BeforeEach
    fun setUp() {
        tfIdfSearcher = TFIDFSearcher("./data/lisa/documents.txt", object :
            TFIDFSearcher.WeightCalculationListener {
            override fun onLoopIterationCheckCondition(docId: Int): Boolean {
                return docId == 1789
            }

            override fun onCalculation(
                dv: TFIDFSearcher.DocumentVector,
                totalDocument: Int,
                termDocFrequency: MutableMap<Int, Int>
            ) {
                println("N (Total Document) = $totalDocument")

                println()
                println("Printing Vector...")
                for (entry in dv.vector){
                    println("\tTermId = ${String.format("%4d", entry.key)}\thas the docFreq of ${termDocFrequency[entry.key]}\tand the termFreq of ${entry.value.toInt()}")
                }
            }

            override fun onCalculated(dv: TFIDFSearcher.DocumentVector) {
                println("\nResult of TfIdf Weight Calculation of Vector #${dv.docId}")
                println("\tNorm = ${dv.norm}")

                for (entry in dv.vector){
                    println("\tTermId = ${String.format("%4d", entry.key)}\thas the score of ${entry.value}")
                }

                println()
            }
        })
    }

    @AfterEach
    fun tearDown() {
    }

    @Test
    fun `Debug TF-IDF of Doc #2790`() {
        val result = tfIdfSearcher.search("Information Retrieval", 10)
        Searcher.displaySearchResults(result)
    }
}