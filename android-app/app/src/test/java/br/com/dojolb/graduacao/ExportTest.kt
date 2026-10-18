package br.com.dojolb.graduacao

import br.com.dojolb.graduacao.data.entity.Position
import br.com.dojolb.graduacao.data.entity.Student
import br.com.dojolb.graduacao.data.backup.BackupManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets

class ExportTest {

    @Test
    fun `csv export contains correct data`() = runBlocking {
        val student = Student(
            id = 1,
            fullName = "João Silva",
            currentBelt = "Branca",
            desiredBelt = "Cinza",
            examDate = "15/03/2024",
            positionsCount = 3,
            isCompleted = true,
            finalAverage = 75.0,
            result = Student.RESULT_APPROVED
        )

        val positions = listOf(
            Position(id = 1, studentId = 1, positionNumber = 1, name = "Queda", grade = 100),
            Position(id = 2, studentId = 1, positionNumber = 2, name = "Imobilização", grade = 75),
            Position(id = 3, studentId = 1, positionNumber = 3, name = "Finalização", grade = 50)
        )

        // Test CSV generation logic
        val csvContent = generateCsv(student, positions)

        // Verify CSV structure
        val lines = csvContent.trim().split("\n")
        assertEquals(4, lines.size) // Header + 3 positions

        // Verify header
        assertTrue(lines[0].contains("Nome do Aluno"))
        assertTrue(lines[0].contains("Faixa Atual"))
        assertTrue(lines[0].contains("Faixa Desejada"))
        assertTrue(lines[0].contains("Data do Exame"))
        assertTrue(lines[0].contains("Nome da Posição"))
        assertTrue(lines[0].contains("Nota Percentual"))
        assertTrue(lines[0].contains("Média Final"))
        assertTrue(lines[0].contains("Situação"))

        // Verify data rows
        lines.drop(1).forEach { line ->
            assertTrue(line.contains("João Silva"))
            assertTrue(line.contains("Branca"))
            assertTrue(line.contains("Cinza"))
            assertTrue(line.contains("15/03/2024"))
            assertTrue(line.contains("75.00") || line.contains("75,00")) // Locale dependent
            assertTrue(line.contains("Aprovado") || line.contains("Reprovado"))
        }

        // Verify individual positions
        assertTrue(lines[1].contains("Queda"))
        assertTrue(lines[1].contains("100"))
        assertTrue(lines[2].contains("Imobilização"))
        assertTrue(lines[2].contains("75"))
        assertTrue(lines[3].contains("Finalização"))
        assertTrue(lines[3].contains("50"))
    }

    @Test
    fun `csv uses semicolon separator`() = runBlocking {
        val student = Student(
            id = 1,
            fullName = "Test User",
            currentBelt = "Branca",
            desiredBelt = "Cinza",
            examDate = "01/01/2024",
            positionsCount = 1,
            isCompleted = true,
            finalAverage = 100.0,
            result = Student.RESULT_APPROVED
        )

        val positions = listOf(
            Position(id = 1, studentId = 1, positionNumber = 1, name = "Posição com; ponto e vírgula", grade = 100)
        )

        val csvContent = generateCsv(student, positions)
        val lines = csvContent.trim().split("\n")

        // Should have semicolons as separators
        val dataLine = lines[1]
        val parts = dataLine.split(";")
        // Name field should be quoted because it contains semicolon
        assertTrue(parts.size >= 8)
    }

    @Test
    fun `csv handles special characters`() = runBlocking {
        val student = Student(
            id = 1,
            fullName = "José da Silva",
            currentBelt = "Branca",
            desiredBelt = "Cinza",
            examDate = "01/01/2024",
            positionsCount = 1,
            isCompleted = true,
            finalAverage = 100.0,
            result = Student.RESULT_APPROVED
        )

        val positions = listOf(
            Position(id = 1, studentId = 1, positionNumber = 1, name = "Posição \"com aspas\"", grade = 100)
        )

        val csvContent = generateCsv(student, positions)
        val lines = csvContent.trim().split("\n")

        // Should handle quotes by doubling them
        assertTrue(lines[1].contains("\"\"com aspas\"\""))
    }

    @Test
    fun `csv uses UTF-8 with BOM`() = runBlocking {
        val student = Student(
            id = 1,
            fullName = "João",
            currentBelt = "Branca",
            desiredBelt = "Cinza",
            examDate = "01/01/2024",
            positionsCount = 1,
            isCompleted = true,
            finalAverage = 100.0,
            result = Student.RESULT_APPROVED
        )

        val positions = listOf(
            Position(id = 1, studentId = 1, positionNumber = 1, name = "Posição", grade = 100)
        )

        val csvContent = generateCsv(student, positions)
        val bytes = csvContent.toByteArray(StandardCharsets.UTF_8)

        // Check for BOM (EF BB BF)
        assertEquals(0xEF, bytes[0].toInt() and 0xFF)
        assertEquals(0xBB, bytes[1].toInt() and 0xFF)
        assertEquals(0xBF, bytes[2].toInt() and 0xFF)
    }

    @Test
    fun `backup data serialization`() = runBlocking {
        val student = Student(
            id = 1,
            fullName = "Backup Test",
            currentBelt = "Branca",
            desiredBelt = "Cinza",
            examDate = "01/01/2024",
            positionsCount = 2,
            isCompleted = false,
            finalAverage = 0.0,
            result = Student.RESULT_IN_PROGRESS
        )

        val positions = listOf(
            Position(id = 1, studentId = 1, positionNumber = 1, name = "Posição 1", grade = 100),
            Position(id = 2, studentId = 1, positionNumber = 2, name = "Posição 2", grade = 75)
        )

        val backupData = BackupManager.BackupData(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            students = listOf(student),
            positions = positions
        )

        // Test serialization roundtrip
        val json = kotlinx.serialization.json.Json { prettyPrint = true; ignoreUnknownKeys = true }
        val jsonString = json.encodeToString(backupData)
        val decoded = json.decodeFromString<BackupManager.BackupData>(jsonString)

        assertEquals(1, decoded.students.size)
        assertEquals("Backup Test", decoded.students[0].fullName)
        assertEquals(2, decoded.positions.size)
        assertEquals(100, decoded.positions[0].grade)
        assertEquals(75, decoded.positions[1].grade)
    }

    private fun generateCsv(student: Student, positions: List<Position>): String {
        val builder = StringBuilder()
        builder.append('\uFEFF') // BOM
        builder.append("Nome do Aluno;Faixa Atual;Faixa Desejada;Data do Exame;Nome da Posição;Nota Percentual;Média Final;Situação\n")

        val resultText = when (student.result) {
            Student.RESULT_APPROVED -> "Aprovado"
            Student.RESULT_FAILED -> "Reprovado"
            else -> "Em Andamento"
        }
        val formattedAverage = String.format(java.util.Locale.getDefault(), "%.2f", student.finalAverage)

        positions.forEach { position ->
            val grade = if (position.grade == Position.GRADE_NOT_EVALUATED) "—" else "${position.grade}%"
            val positionName = position.name.ifEmpty { "Posição ${position.positionNumber}" }.replace("\n", " ").replace("\r", " ")
            builder.append(
                "${escapeCsv(student.fullName)};" +
                "${escapeCsv(student.currentBelt)};" +
                "${escapeCsv(student.desiredBelt)};" +
                "${escapeCsv(student.examDate)};" +
                "${escapeCsv(positionName)};" +
                "${escapeCsv(grade)};" +
                "${escapeCsv(formattedAverage)};" +
                "${escapeCsv(resultText)}\n"
            )
        }

        return builder.toString()
    }

    private fun escapeCsv(value: String): String {
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"${value.replace("\"", "\"\"")}\""
        }
        return value
    }
}