package br.com.dojolb.graduacao.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.dojolb.graduacao.data.entity.Position
import br.com.dojolb.graduacao.data.entity.Student
import br.com.dojolb.graduacao.data.repository.GraduacaoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class EvaluationViewModel(
    private val repository: GraduacaoRepository,
    private val studentId: Long
) : ViewModel() {

    private val _student = MutableStateFlow<Student?>(null)
    val student = _student.asStateFlow()

    private val _positions = MutableStateFlow<List<Position>>(emptyList())
    val positions = _positions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _provisionalAverage = MutableStateFlow<Double?>(null)
    val provisionalAverage = _provisionalAverage.asStateFlow()

    private val _evaluatedCount = MutableStateFlow(0)
    val evaluatedCount = _evaluatedCount.asStateFlow()

    private val _totalCount = MutableStateFlow(0)
    val totalCount = _totalCount.asStateFlow()

    private val _isComplete = MutableStateFlow(false)
    val isComplete = _isComplete.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val student = repository.getStudentById(studentId)
                _student.value = student

                val positions = repository.getPositionsByStudentIdSync(studentId)
                _positions.value = positions
                _totalCount.value = positions.size

                val evaluated = repository.getEvaluatedCount(studentId)
                _evaluatedCount.value = evaluated

                val average = repository.getProvisionalAverage(studentId)
                _provisionalAverage.value = average

                val complete = repository.isEvaluationComplete(studentId)
                _isComplete.value = complete
            } catch (e: Exception) {
                _error.value = "Erro ao carregar dados: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun observePositions(): androidx.lifecycle.LiveData<List<Position>> {
        return repository.getPositionsByStudentId(studentId).toLiveData()
    }

    suspend fun updatePositionGrade(position: Position, grade: Int): Result<Unit> {
        return try {
            val updated = position.copy(
                grade = grade,
                updatedAt = System.currentTimeMillis()
            )
            repository.updatePosition(updated)
            // Refresh local state
            loadData()
            Result.success(Unit)
        } catch (e: Exception) {
            _error.value = "Erro ao salvar nota: ${e.message}"
            Result.failure(e)
        }
    }

    suspend fun updatePositionName(position: Position, name: String): Result<Unit> {
        return try {
            val updated = position.copy(
                name = name.trim(),
                updatedAt = System.currentTimeMillis()
            )
            repository.updatePosition(updated)
            loadData()
            Result.success(Unit)
        } catch (e: Exception) {
            _error.value = "Erro ao salvar nome: ${e.message}"
            Result.failure(e)
        }
    }

    suspend fun addPosition(): Result<Position?> {
        return try {
            val newPosition = repository.addPosition(studentId)
            loadData()
            Result.success(newPosition)
        } catch (e: Exception) {
            _error.value = "Erro ao adicionar posição: ${e.message}"
            Result.failure(e)
        }
    }

    suspend fun removeLastPosition(): Result<Unit> {
        return try {
            val success = repository.removeLastPosition(studentId)
            if (success) {
                loadData()
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Nenhuma posição para remover"))
            }
        } catch (e: Exception) {
            _error.value = "Erro ao remover posição: ${e.message}"
            Result.failure(e)
        }
    }

    suspend fun finalizeEvaluation(): Result<EvaluationResult> {
        return try {
            _isLoading.value = true
            val student = repository.getStudentById(studentId) ?: return Result.failure(IllegalStateException("Aluno não encontrado"))
            val positions = repository.getPositionsByStudentIdSync(studentId)

            // Check for pending positions
            val pending = positions.filter { it.grade == Position.GRADE_NOT_EVALUATED }
            if (pending.isNotEmpty()) {
                return Result.failure(IllegalStateException("Existem ${pending.size} posições pendentes"))
            }

            // Calculate final average
            val average = repository.calculateFinalAverage(studentId)
            val result = repository.determineResult(average)

            // Update student
            val updatedStudent = student.copy(
                isCompleted = true,
                finalAverage = average,
                result = result,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateStudent(updatedStudent)

            Result.success(EvaluationResult(
                student = updatedStudent,
                positions = positions,
                average = average,
                result = result
            ))
        } catch (e: Exception) {
            _error.value = "Erro ao finalizar: ${e.message}"
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun reopenEvaluation(): Result<Unit> {
        return try {
            val student = repository.getStudentById(studentId) ?: return Result.failure(IllegalStateException("Aluno não encontrado"))
            val updatedStudent = student.copy(
                isCompleted = false,
                result = Student.RESULT_IN_PROGRESS,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateStudent(updatedStudent)
            loadData()
            Result.success(Unit)
        } catch (e: Exception) {
            _error.value = "Erro ao reabrir: ${e.message}"
            Result.failure(e)
        }
    }

    data class EvaluationResult(
        val student: Student,
        val positions: List<Position>,
        val average: Double,
        val result: String
    ) {
        val isApproved: Boolean
            get() = result == Student.RESULT_APPROVED

        val formattedAverage: String
            get() = String.format(Locale.getDefault(), "%.2f%%", average)

        val formattedDate: String
            get() {
                return try {
                    val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val date = inputFormat.parse(student.examDate)
                    outputFormat.format(date)
                } catch (e: Exception) {
                    student.examDate
                }
            }
    }
}