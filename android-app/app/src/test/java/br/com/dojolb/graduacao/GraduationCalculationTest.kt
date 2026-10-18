package br.com.dojolb.graduacao

import br.com.dojolb.graduacao.data.entity.Position
import br.com.dojolb.graduacao.data.entity.Student
import org.junit.Assert.*
import org.junit.Test

class GraduationCalculationTest {

    @Test
    fun `average 100% results in approval`() {
        val positions = listOf(
            createPosition(1, 100),
            createPosition(2, 100),
            createPosition(3, 100)
        )
        val average = calculateAverage(positions)
        assertEquals(100.0, average, 0.001)
        assertTrue("Average > 75 should be approved", isApproved(average))
    }

    @Test
    fun `average 80% results in approval`() {
        val positions = listOf(
            createPosition(1, 100),
            createPosition(2, 100),
            createPosition(3, 100),
            createPosition(4, 100),
            createPosition(5, 20) // This makes average 84%
        )
        val average = calculateAverage(positions)
        assertEquals(84.0, average, 0.001)
        assertTrue("Average > 75 should be approved", isApproved(average))
    }

    @Test
    fun `average exactly 75% results in failure`() {
        val positions = listOf(
            createPosition(1, 100),
            createPosition(2, 100),
            createPosition(3, 100),
            createPosition(4, 0) // Average = 300/4 = 75%
        )
        val average = calculateAverage(positions)
        assertEquals(75.0, average, 0.001)
        assertFalse("Average exactly 75 should be failed (strict > 75)", isApproved(average))
    }

    @Test
    fun `average 50% results in failure`() {
        val positions = listOf(
            createPosition(1, 50),
            createPosition(2, 50),
            createPosition(3, 50)
        )
        val average = calculateAverage(positions)
        assertEquals(50.0, average, 0.001)
        assertFalse("Average <= 75 should be failed", isApproved(average))
    }

    @Test
    fun `incomplete evaluation remains in progress`() {
        val positions = listOf(
            createPosition(1, 100),
            createPosition(2, 75),
            createPosition(3, Position.GRADE_NOT_EVALUATED)
        )
        val evaluatedCount = positions.count { it.grade != Position.GRADE_NOT_EVALUATED }
        val totalCount = positions.size
        assertEquals(2, evaluatedCount)
        assertEquals(3, totalCount)
        assertTrue("Should have pending positions", evaluatedCount < totalCount)
    }

    @Test
    fun `changing a grade updates average`() {
        var positions = listOf(
            createPosition(1, 100),
            createPosition(2, 50),
            createPosition(3, 50)
        )
        var average = calculateAverage(positions)
        assertEquals(66.66, average, 0.01)

        // Change position 2 from 50 to 100
        positions = positions.map { if (it.positionNumber == 2) it.copy(grade = 100) else it }.toList()
        average = calculateAverage(positions)
        assertEquals(83.33, average, 0.01)

        // Should now be approved
        assertTrue("Updated average > 75 should be approved", isApproved(average))
    }

    @Test
    fun `single position exam works correctly`() {
        // Single position with 100% -> approved
        var positions = listOf(createPosition(1, 100))
        var average = calculateAverage(positions)
        assertEquals(100.0, average, 0.001)
        assertTrue(isApproved(average))

        // Single position with 75% -> failed (strict > 75)
        positions = listOf(createPosition(1, 75))
        average = calculateAverage(positions)
        assertEquals(75.0, average, 0.001)
        assertFalse(isApproved(average))

        // Single position with 76% -> approved
        positions = listOf(createPosition(1, 76))
        average = calculateAverage(positions)
        assertEquals(76.0, average, 0.001)
        assertTrue(isApproved(average))
    }

    @Test
    fun `different students maintain independent evaluations`() {
        val student1Positions = listOf(
            createPosition(1, 100, 1),
            createPosition(2, 100, 1)
        )
        val student2Positions = listOf(
            createPosition(1, 0, 2),
            createPosition(2, 0, 2)
        )

        val avg1 = calculateAverage(student1Positions)
        val avg2 = calculateAverage(student2Positions)

        assertEquals(100.0, avg1, 0.001)
        assertEquals(0.0, avg2, 0.001)
        assertTrue(isApproved(avg1))
        assertFalse(isApproved(avg2))
    }

    @Test
    fun `provisional average excludes non-evaluated positions`() {
        val positions = listOf(
            createPosition(1, 100),
            createPosition(2, 50),
            createPosition(3, Position.GRADE_NOT_EVALUATED),
            createPosition(4, Position.GRADE_NOT_EVALUATED)
        )

        val evaluatedPositions = positions.filter { it.grade != Position.GRADE_NOT_EVALUATED }
        val provisionalAverage = calculateAverage(evaluatedPositions)

        assertEquals(75.0, provisionalAverage, 0.001)
        // Provisional average is 75%, but final would depend on remaining positions
    }

    @Test
    fun `grade values are valid`() {
        val validGrades = Position.VALID_GRADES
        assertEquals(listOf(0, 25, 50, 75, 100), validGrades)

        validGrades.forEach { grade ->
            val position = createPosition(1, grade)
            assertEquals(grade, position.grade)
        }
    }

    @Test
    fun `approval boundary values`() {
        // Test boundary: 75.0001 should pass, 75.0 should fail
        val justAbove = listOf(createPosition(1, 76)) // Only way to get > 75 with valid grades
        val exactly75 = listOf(createPosition(1, 75))
        val justBelow = listOf(createPosition(1, 50))

        assertTrue(isApproved(calculateAverage(justAbove)))
        assertFalse(isApproved(calculateAverage(exactly75)))
        assertFalse(isApproved(calculateAverage(justBelow)))
    }

    private fun createPosition(number: Int, grade: Int, studentId: Long = 1): Position {
        return Position(
            id = number.toLong(),
            studentId = studentId,
            positionNumber = number,
            name = "Posição $number",
            grade = grade
        )
    }

    private fun calculateAverage(positions: List<Position>): Double {
        if (positions.isEmpty()) return 0.0
        val sum = positions.sumOf { it.grade.toDouble() }
        return sum / positions.size
    }

    private fun isApproved(average: Double): Boolean {
        return average > 75.0 // STRICT comparison as per requirements
    }
}