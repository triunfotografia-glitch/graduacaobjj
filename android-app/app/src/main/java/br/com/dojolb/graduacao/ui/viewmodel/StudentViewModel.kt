package br.com.dojolb.graduacao.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.dojolb.graduacao.data.entity.Student
import br.com.dojolb.graduacao.data.repository.GraduacaoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudentViewModel(
    private val repository: GraduacaoRepository
) : ViewModel() {

    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students = _students.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun loadStudents() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            // The flow will emit automatically when data changes
        }
    }

    fun observeStudents(): androidx.lifecycle.LiveData<List<Student>> {
        return repository.getAllStudents().toLiveData()
    }

    suspend fun createStudent(
        fullName: String,
        currentBelt: String,
        desiredBelt: String,
        examDate: String,
        positionsCount: Int
    ): Result<Long> {
        return try {
            _isLoading.value = true
            val student = Student(
                fullName = fullName.trim(),
                currentBelt = currentBelt,
                desiredBelt = desiredBelt,
                examDate = examDate,
                positionsCount = positionsCount
            )
            val id = repository.insertStudent(student)
            // Initialize positions
            repository.initializePositions(id, positionsCount)
            Result.success(id)
        } catch (e: Exception) {
            _error.value = "Erro ao criar aluno: ${e.message}"
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun updateStudent(student: Student): Result<Unit> {
        return try {
            _isLoading.value = true
            val updated = student.copy(updatedAt = System.currentTimeMillis())
            repository.updateStudent(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            _error.value = "Erro ao atualizar aluno: ${e.message}"
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun deleteStudent(studentId: Long): Result<Unit> {
        return try {
            _isLoading.value = true
            repository.deleteStudent(studentId)
            Result.success(Unit)
        } catch (e: Exception) {
            _error.value = "Erro ao excluir aluno: ${e.message}"
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun updateStudentPositionsCount(studentId: Long, newCount: Int): Result<Unit> {
        return try {
            _isLoading.value = true
            val success = repository.updateStudentPositionsCount(studentId, newCount)
            if (success) Result.success(Unit) else Result.failure(IllegalStateException("Failed to update positions count"))
        } catch (e: Exception) {
            _error.value = "Erro ao atualizar posições: ${e.message}"
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    fun validateStudentForm(
        fullName: String,
        currentBelt: String,
        desiredBelt: String,
        examDate: String,
        positionsCount: String
    ): ValidationResult {
        val errors = mutableMapOf<String, String>()

        if (fullName.trim().isEmpty()) {
            errors["fullName"] = "Nome é obrigatório"
        }

        if (currentBelt.isEmpty() || desiredBelt.isEmpty()) {
            errors["belt"] = "Selecione as faixas"
        }

        if (currentBelt == desiredBelt) {
            errors["belt"] = "Faixa atual e desejada não podem ser iguais"
        }

        if (examDate.trim().isEmpty()) {
            errors["examDate"] = "Data é obrigatória"
        } else if (!isValidDateFormat(examDate)) {
            errors["examDate"] = "Formato inválido. Use DD/MM/AAAA"
        }

        val positionsCountInt = positionsCount.trim().toIntOrNull()
        if (positionsCountInt == null) {
            errors["positionsCount"] = "Quantidade de posições é obrigatória"
        } else if (positionsCountInt < 1) {
            errors["positionsCount"] = "Mínimo 1 posição"
        } else if (positionsCountInt > 50) {
            errors["positionsCount"] = "Máximo 50 posições"
        }

        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }

    private fun isValidDateFormat(date: String): Boolean {
        return try {
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            format.isLenient = false
            format.parse(date)
            true
        } catch (e: Exception) {
            false
        }
    }

    sealed interface ValidationResult {
        data class Valid : ValidationResult
        data class Invalid(val errors: Map<String, String>) : ValidationResult
    }
}

private val GraduacaoRepository.getAllStudents: () -> kotlinx.coroutines.flow.Flow<List<Student>>
    get() = { studentDao().getAllStudents() }

// Extension to convert Flow to LiveData
import androidx.lifecycle.LiveData
import androidx.lifecycle.toLiveData

@Suppress("UNUSED_PARAMETER")
inline fun <T> kotlinx.coroutines.flow.Flow<T>.toLiveData(): LiveData<T> = androidx.lifecycle.liveData {
    collect { value -> emit(value) }
}