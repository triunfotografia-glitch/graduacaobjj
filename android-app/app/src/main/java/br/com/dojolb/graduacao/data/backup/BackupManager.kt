package br.com.dojolb.graduacao.data.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import br.com.dojolb.graduacao.data.database.AppDatabase
import br.com.dojolb.graduacao.data.entity.Position
import br.com.dojolb.graduacao.data.entity.Student
import br.com.dojolb.graduacao.data.repository.GraduacaoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManager(
    private val context: Context,
    private val repository: GraduacaoRepository
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    // Export backup to a zip file
    suspend fun exportBackup(): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val students = repository.getAllStudents().first()
            val allPositions = mutableListOf<Position>()
            students.forEach { student ->
                val positions = repository.getPositionsByStudentIdSync(student.id)
                allPositions.addAll(positions)
            }

            val backupData = BackupData(
                version = 1,
                exportedAt = System.currentTimeMillis(),
                students = students,
                positions = allPositions
            )

            val jsonString = json.encodeToString(backupData)

            // Create zip file in cache
            val zipFile = File(context.cacheDir, "dojo_lb_backup_${System.currentTimeMillis()}.zip")
            val zipOut = ZipOutputStream(FileOutputStream(zipFile))
            zipOut.putNextEntry(ZipEntry("backup.json"))
            zipOut.write(jsonString.toByteArray(StandardCharsets.UTF_8))
            zipOut.closeEntry()
            zipOut.close()

            // Use MediaStore or FileProvider to share
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                zipFile
            )

            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Import backup from a zip file
    suspend fun importBackup(uri: Uri, overwriteExisting: Boolean = false): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return Result.failure(IllegalStateException("Não foi possível abrir o arquivo"))
            val zipIn = ZipInputStream(inputStream)

            var entry: ZipEntry?
            var jsonContent: String? = null

            while (zipIn.nextEntry(entry).also { entry = it } != null) {
                if (entry.name == "backup.json") {
                    val buffer = StringBuilder()
                    val charBuffer = CharArray(1024)
                    var len: Int
                    val reader = java.io.InputStreamReader(zipIn, StandardCharsets.UTF_8)
                    while (reader.read(charBuffer).also { len = it } != -1) {
                        buffer.append(charBuffer, 0, len)
                    }
                    jsonContent = buffer.toString()
                    break
                }
            }
            zipIn.close()

            jsonContent ?: return Result.failure(IllegalStateException("Arquivo de backup inválido"))

            val backupData = json.decodeFromString<BackupData>(jsonContent)

            var importedCount = 0
            var skippedCount = 0

            for (student in backupData.students) {
                val existingStudent = repository.getStudentById(student.id)
                if (existingStudent != null && !overwriteExisting) {
                    skippedCount++
                    continue
                }

                // Insert or update student
                val studentId = if (existingStudent != null) {
                    repository.updateStudent(student.copy(id = existingStudent.id))
                    existingStudent.id
                } else {
                    repository.insertStudent(student)
                }

                // Delete existing positions for this student
                repository.deleteStudent(studentId) // This deletes positions too, need to re-insert
                // Re-insert student
                val newStudentId = repository.insertStudent(student.copy(id = 0))

                // Insert positions
                val studentPositions = backupData.positions.filter { it.studentId == student.id }
                studentPositions.forEach { pos ->
                    repository.insertPositions(listOf(pos.copy(id = 0, studentId = newStudentId)))
                }

                importedCount++
            }

            Result.success(importedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Export all data as CSV (for spreadsheet compatibility)
    suspend fun exportAllCsv(): Result<File> = withContext(Dispatchers.IO) {
        try {
            val students = repository.getAllStudents().first()
            val file = File(context.cacheDir, "dojo_lb_todos_alunos_${System.currentTimeMillis()}.csv")

            val writer = OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8)
            writer.write('\uFEFF') // BOM
            writer.write("Nome do Aluno;Faixa Atual;Faixa Desejada;Data do Exame;Nome da Posição;Nota Percentual;Média Final;Situação\n")

            for (student in students) {
                val positions = repository.getPositionsByStudentIdSync(student.id)
                val resultText = when (student.result) {
                    Student.RESULT_APPROVED -> "Aprovado"
                    Student.RESULT_FAILED -> "Reprovado"
                    else -> "Em Andamento"
                }
                val formattedAverage = String.format(java.util.Locale.getDefault(), "%.2f", student.finalAverage)

                positions.forEach { position ->
                    val grade = if (position.grade == Position.GRADE_NOT_EVALUATED) "—" else "${position.grade}%"
                    val positionName = position.name.ifEmpty { "Posição ${position.positionNumber}" }.replace("\n", " ").replace("\r", " ")
                    writer.write(
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
            }

            writer.flush()
            writer.close()

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun escapeCsv(value: String): String {
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"${value.replace("\"", "\"\"")}\""
        }
        return value
    }
}

@kotlinx.serialization.Serializable
data class BackupData(
    val version: Int,
    val exportedAt: Long,
    val students: List<Student>,
    val positions: List<Position>
)