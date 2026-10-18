package br.com.dojolb.graduacao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.dojolb.graduacao.data.database.AppDatabase
import br.com.dojolb.graduacao.data.entity.Position
import br.com.dojolb.graduacao.data.entity.Student
import br.com.dojolb.graduacao.data.repository.GraduacaoRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class RepositoryTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: AppDatabase
    private lateinit var repository: GraduacaoRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = GraduacaoRepository(db.studentDao(), db.positionDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `insert and retrieve student`() = runBlocking {
        val student = Student(
            fullName = "João Silva",
            currentBelt = "Branca",
            desiredBelt = "Cinza",
            examDate = "15/03/2024",
            positionsCount = 5
        )

        val id = repository.insertStudent(student)
        assertTrue(id > 0)

        val retrieved = repository.getStudentById(id)
        assertNotNull(retrieved)
        assertEquals("João Silva", retrieved?.fullName)
        assertEquals("Branca", retrieved?.currentBelt)
        assertEquals("Cinza", retrieved?.desiredBelt)
        assertEquals("15/03/2024", retrieved?.examDate)
        assertEquals(5, retrieved?.positionsCount)
    }

    @Test
    fun `initialize positions for student`() = runBlocking {
        val student = Student(
            fullName = "Maria Santos",
            currentBelt = "Amarela",
            desiredBelt = "Laranja",
            examDate = "20/04/2024",
            positionsCount = 3
        )

        val id = repository.insertStudent(student)
        repository.initializePositions(id, 3)

        val positions = repository.getPositionsByStudentIdSync(id)
        assertEquals(3, positions.size)
        assertEquals(1, positions[0].positionNumber)
        assertEquals(2, positions[1].positionNumber)
        assertEquals(3, positions[2].positionNumber)
        positions.forEach { assertEquals(Position.GRADE_NOT_EVALUATED, it.grade) }
    }

    @Test
    fun `update position grade`() = runBlocking {
        val student = Student(
            fullName = "Test User",
            currentBelt = "Branca",
            desiredBelt = "Cinza",
            examDate = "01/01/2024",
            positionsCount = 2
        )

        val id = repository.insertStudent(student)
        repository.initializePositions(id, 2)

        val position = repository.getPosition(id, 1)!!
        val updated = position.copy(grade = 75)
        repository.updatePosition(updated)

        val retrieved = repository.getPosition(id, 1)
        assertEquals(75, retrieved?.grade)
    }

    @Test
    fun `calculate final average`() = runBlocking {
        val student = Student(
            fullName = "Average Test",
            currentBelt = "Branca",
            desiredBelt = "Cinza",
            examDate = "01/01/2024",
            positionsCount = 4
        )

        val id = repository.insertStudent(student)
        repository.initializePositions(id, 4)

        // Set grades: 100, 75, 50, 100 -> average = 81.25
        repository.updatePosition(repository.getPosition(id, 1)!!.copy(grade = 100))
        repository.updatePosition(repository.getPosition(id, 2)!!.copy(grade = 75))
        repository.updatePosition(repository.getPosition(id, 3)!!.copy(grade = 50))
        repository.updatePosition(repository.getPosition(id, 4)!!.copy(grade = 100))

        val average = repository.calculateFinalAverage(id)
        assertEquals(81.25, average, 0.001)
    }

    @Test
    fun `determine result - approved`() = runBlocking {
        val result = repository.determineResult(81.25)
        assertEquals(Student.RESULT_APPROVED, result)
    }

    @Test
    fun `determine result - failed at 75`() = runBlocking {
        val result = repository.determineResult(75.0)
        assertEquals(Student.RESULT_FAILED, result)
    }

    @Test
    fun `determine result - failed below 75`() = runBlocking {
        val result = repository.determineResult(50.0)
        assertEquals(Student.RESULT_FAILED, result)
    }

    @Test
    fun `is evaluation complete`() = runBlocking {
        val student = Student(
            fullName = "Complete Test",
            currentBelt = "Branca",
            desiredBelt = "Cinza",
            examDate = "01/01/2024",
            positionsCount = 2
        )

        val id = repository.insertStudent(student)
        repository.initializePositions(id, 2)

        // Not complete initially
        var complete = repository.isEvaluationComplete(id)
        assertFalse(complete)

        // Complete after all graded
        repository.updatePosition(repository.getPosition(id, 1)!!.copy(grade = 100))
        repository.updatePosition(repository.getPosition(id, 2)!!.copy(grade = 75))

        complete = repository.isEvaluationComplete(id)
        assertTrue(complete)
    }

    @Test
    fun `add and remove positions`() = runBlocking {
        val student = Student(
            fullName = "Position Test",
            currentBelt = "Branca",
            desiredBelt = "Cinza",
            examDate = "01/01/2024",
            positionsCount = 2
        )

        val id = repository.insertStudent(student)
        repository.initializePositions(id, 2)

        var count = repository.getTotalCount(id)
        assertEquals(2, count)

        // Add position
        val newPos = repository.addPosition(id)
        assertNotNull(newPos)
        assertEquals(3, newPos?.positionNumber)

        count = repository.getTotalCount(id)
        assertEquals(3, count)

        // Remove last position
        val removed = repository.removeLastPosition(id)
        assertTrue(removed)

        count = repository.getTotalCount(id)
        assertEquals(2, count)
    }

    @Test
    fun `update student positions count`() = runBlocking {
        val student = Student(
            fullName = "Count Test",
            currentBelt = "Branca",
            desiredBelt = "Cinza",
            examDate = "01/01/2024",
            positionsCount = 3
        )

        val id = repository.insertStudent(student)
        repository.initializePositions(id, 3)

        // Increase count
        var success = repository.updateStudentPositionsCount(id, 5)
        assertTrue(success)

        var count = repository.getTotalCount(id)
        assertEquals(5, count)

        // Decrease count
        success = repository.updateStudentPositionsCount(id, 2)
        assertTrue(success)

        count = repository.getTotalCount(id)
        assertEquals(2, count)
    }

    @Test
    fun `delete student cascades to positions`() = runBlocking {
        val student = Student(
            fullName = "Delete Test",
            currentBelt = "Branca",
            desiredBelt = "Cinza",
            examDate = "01/01/2024",
            positionsCount = 3
        )

        val id = repository.insertStudent(student)
        repository.initializePositions(id, 3)

        var positions = repository.getPositionsByStudentIdSync(id)
        assertEquals(3, positions.size)

        repository.deleteStudent(id)

        val deletedStudent = repository.getStudentById(id)
        assertNull(deletedStudent)

        positions = repository.getPositionsByStudentIdSync(id)
        assertTrue(positions.isEmpty())
    }
}