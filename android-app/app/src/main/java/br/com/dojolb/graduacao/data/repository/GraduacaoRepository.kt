package br.com.dojolb.graduacao.data.repository

import br.com.dojolb.graduacao.data.dao.PositionDao
import br.com.dojolb.graduacao.data.dao.StudentDao
import br.com.dojolb.graduacao.data.entity.Position
import br.com.dojolb.graduacao.data.entity.Student
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class GraduacaoRepository(
    private val studentDao: StudentDao,
    private val positionDao: PositionDao
) {
    // Student operations
    suspend fun insertStudent(student: Student): Long = studentDao.insert(student)

    suspend fun updateStudent(student: Student): Int = studentDao.update(student)

    suspend fun deleteStudent(studentId: Long) {
        positionDao.deleteByStudentId(studentId)
        studentDao.deleteById(studentId)
    }

    fun getAllStudents(): Flow<List<Student>> = studentDao.getAllStudents()

    suspend fun getStudentById(studentId: Long): Student? = studentDao.getStudentById(studentId)

    fun observeStudentById(studentId: Long): Flow<Student?> = studentDao.observeStudentById(studentId)

    // Position operations
    suspend fun insertPositions(positions: List<Position>): List<Long> = positionDao.insertAll(positions)

    suspend fun updatePosition(position: Position): Int = positionDao.update(position)

    suspend fun updatePositions(positions: List<Position>): Int = positionDao.updateAll(positions)

    fun getPositionsByStudentId(studentId: Long): Flow<List<Position>> = positionDao.getPositionsByStudentId(studentId)

    suspend fun getPositionsByStudentIdSync(studentId: Long): List<Position> = positionDao.getPositionsByStudentIdSync(studentId)

    suspend fun getEvaluatedCount(studentId: Long): Int = positionDao.getEvaluatedCount(studentId)

    suspend fun getTotalCount(studentId: Long): Int = positionDao.getTotalCount(studentId)

    suspend fun getProvisionalAverage(studentId: Long): Double? = positionDao.getProvisionalAverage(studentId)

    suspend fun getPosition(studentId: Long, positionNumber: Int): Position? = positionDao.getPosition(studentId, positionNumber)

    // Calculations
    suspend fun calculateFinalAverage(studentId: Long): Double {
        val positions = positionDao.getPositionsByStudentIdSync(studentId)
        if (positions.isEmpty()) return 0.0
        val sum = positions.sumOf { it.grade.toDouble() }
        return sum / positions.size
    }

    suspend fun isEvaluationComplete(studentId: Long): Boolean {
        val evaluated = positionDao.getEvaluatedCount(studentId)
        val total = positionDao.getTotalCount(studentId)
        return evaluated == total && total > 0
    }

    suspend fun determineResult(average: Double): String {
        return if (average > 75.0) Student.RESULT_APPROVED else Student.RESULT_FAILED
    }

    // Initialize positions for a new student
    suspend fun initializePositions(studentId: Long, count: Int) {
        val positions = (1..count).map { index ->
            Position(
                studentId = studentId,
                positionNumber = index,
                name = "Posição $index",
                grade = Position.GRADE_NOT_EVALUATED
            )
        }
        positionDao.insertAll(positions)
    }

    // Add a new position
    suspend fun addPosition(studentId: Long): Position? {
        val total = positionDao.getTotalCount(studentId)
        val newPosition = Position(
            studentId = studentId,
            positionNumber = total + 1,
            name = "Posição ${total + 1}",
            grade = Position.GRADE_NOT_EVALUATED
        )
        val id = positionDao.insert(newPosition)
        return newPosition.copy(id = id)
    }

    // Remove last position
    suspend fun removeLastPosition(studentId: Long): Boolean {
        val positions = positionDao.getPositionsByStudentIdSync(studentId)
        if (positions.isEmpty()) return false
        val lastPosition = positions.last()
        positionDao.deleteById(lastPosition.id)
        return true
    }

    // Update student positions count
    suspend fun updateStudentPositionsCount(studentId: Long, newCount: Int): Boolean {
        val student = studentDao.getStudentById(studentId) ?: return false
        val currentCount = positionDao.getTotalCount(studentId)

        if (newCount > currentCount) {
            // Add positions
            repeat(newCount - currentCount) {
                addPosition(studentId)
            }
        } else if (newCount < currentCount) {
            // Remove positions (from the end)
            repeat(currentCount - newCount) {
                removeLastPosition(studentId)
            }
        }

        val updatedStudent = student.copy(
            positionsCount = newCount,
            updatedAt = System.currentTimeMillis()
        )
        studentDao.update(updatedStudent)
        return true
    }
}