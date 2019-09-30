import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

internal class SearchResultTest {

    private val searchResult: ArrayList<SearchResult> =
        arrayListOf(SearchResult(null, Double.NaN), SearchResult(null, 0.1), SearchResult(null, 0.2))

//    @BeforeEach
//    fun setUp() {
//    }

//    @AfterEach
//    fun tearDown() {
//    }

    @Test
    fun `Test SearchResult Sort Interactions`() {
        searchResult.sortWith(kotlin.Comparator { o1, o2 ->
            val a = if(o1.score.isNaN()) {
                Double.MIN_VALUE
            } else {
                o1.score
            }

            val b = if(o2.score.isNaN()) {
                Double.MIN_VALUE
            } else {
                o2.score
            }

            return@Comparator when {
                a > b -> 1
                a == b -> 0
                else -> -1
            }
        });
        searchResult.reverse()
        println("Actual Result: $searchResult")

        val expected = arrayListOf(SearchResult(null, 0.2), SearchResult(null, 0.1), SearchResult(null, Double.NaN))
        println("Expected Result: $expected")
        for (i in 0 until 3) {
            assertEquals(expected[i].score, searchResult[i].score)
        }
    }
}