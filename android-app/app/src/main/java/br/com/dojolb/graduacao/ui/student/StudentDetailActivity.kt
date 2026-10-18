package br.com.dojolb.graduacao.ui.student

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.dojolb.graduacao.R
import br.com.dojolb.graduacao.data.entity.Student
import br.com.dojolb.graduacao.ui.components.CommonComponents.DojoButton
import br.com.dojolb.graduacao.ui.components.CommonComponents.DojoOutlinedButton
import br.com.dojolb.graduacao.ui.components.CommonComponents.DojoSurface
import br.com.dojolb.graduacao.ui.components.CommonComponents.EmptyState
import br.com.dojolb.graduacao.ui.components.CommonComponents.InfoRow
import br.com.dojolb.graduacao.ui.components.CommonComponents.SectionHeader
import br.com.dojolb.graduacao.ui.components.CommonComponents.StatusChip
import br.com.dojolb.graduacao.ui.theme.DojoLBGraduacaoTheme
import br.com.dojolb.graduacao.ui.theme.RedPrimary
import br.com.dojolb.graduacao.ui.theme.White
import br.com.dojolb.graduacao.ui.viewmodel.StudentViewModel

class StudentDetailActivity : ComponentActivity() {
    private val viewModel: StudentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupDojoWindow()
        setContent {
            DojoLBGraduacaoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StudentDetailScreen(
                        viewModel = viewModel,
                        studentId = intent.getLongExtra("STUDENT_ID", -1)
                    )
                }
            }
        }
    }
}

@Composable
fun StudentDetailScreen(
    viewModel: StudentViewModel,
    studentId: Long
) {
    val student by viewModel.observeStudentById(studentId).observeAsState(initialValue = null)
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var navigateToEvaluation by remember { mutableStateOf(false) }
    var navigateToReport by remember { mutableStateOf(false) }

    if (student == null) {
        EmptyState(
            title = "Aluno não encontrado",
            subtitle = "Este aluno pode ter sido excluído",
            actionText = "Voltar",
            onAction = { finish() }
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top
    ) {
        TopAppBar(
            modifier = Modifier.fillMaxWidth(),
            title = {
                Text(
                    text = student.fullName,
                    fontSize = 18.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = White,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = { finish() }) {
                    Icon(
                        painter = androidx.compose.material.icons.Icons.Default.ArrowBack,
                        contentDescription = "Voltar",
                        tint = White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = White
            ),
            actions = {
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(
                        painter = androidx.compose.material.icons.Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = White
                    )
                }
            }
        )

        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Student Info Card
            DojoSurface {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    SectionHeader("Informações do Aluno")
                    InfoRow("Faixa atual", student.currentBelt)
                    InfoRow("Faixa desejada", student.desiredBelt)
                    InfoRow("Data do exame", student.examDate)
                    InfoRow("Total de posições", student.positionsCount.toString())
                    InfoRow("Situação", when (student.result) {
                        Student.RESULT_APPROVED -> "APROVADO"
                        Student.RESULT_FAILED -> "REPROVADO"
                        else -> "EM ANDAMENTO"
                    })
                }
            }

            // Progress Card
            DojoSurface {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    SectionHeader("Progresso do Exame")
                    // We'd need to observe positions count here
                    // For now, show basic info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                    ) {
                        Text("Média atual", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            String.format("%.2f%%", student.finalAverage),
                            fontSize = 20.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = RedPrimary
                        )
                    }
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.unit.dp.toPx(8))
                    br.com.dojolb.graduacao.ui.components.CommonComponents.ProgressBar(
                        progress = if (student.positionsCount > 0) 1f else 0f, // placeholder
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Status Chip
            StatusChip(student.result)

            // Actions
            if (student.result == Student.RESULT_IN_PROGRESS) {
                DojoButton(
                    text = "Continuar Avaliação",
                    onClick = { navigateToEvaluation = true },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                DojoButton(
                    text = "Ver Relatório",
                    onClick = { navigateToReport = true },
                    modifier = Modifier.fillMaxWidth()
                )
                DojoOutlinedButton(
                    text = "Reabrir Avaliação",
                    onClick = {
                        // Reopen evaluation
                        val intent = Intent(LocalContext.current, EvaluationActivity::class.java)
                        intent.putExtra("STUDENT_ID", studentId)
                        intent.putExtra("REOPEN", true)
                        LocalContext.current.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            DojoOutlinedButton(
                text = "Editar Cadastro",
                onClick = { showEditDialog = true },
                modifier = Modifier.fillMaxWidth()
            )

            DojoOutlinedButton(
                text = "Excluir Aluno",
                onClick = { showDeleteConfirm = true },
                borderColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showEditDialog) {
        StudentFormDialog(
            onDismiss = { showEditDialog = false },
            onSave = { name, currentBelt, desiredBelt, examDate, positionsCount ->
                val updatedStudent = student.copy(
                    fullName = name,
                    currentBelt = currentBelt,
                    desiredBelt = desiredBelt,
                    examDate = examDate,
                    positionsCount = positionsCount,
                    updatedAt = System.currentTimeMillis()
                )
                viewModel.viewModelScope.launch {
                    val result = viewModel.updateStudent(updatedStudent)
                    if (result is Result.Success) {
                        showEditDialog = false
                    }
                }
            },
            student = student
        )
    }

    if (showDeleteConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Excluir aluno", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
            text = { Text("Tem certeza que deseja excluir ${student.fullName}? Todos os dados da avaliação serão perdidos.") },
            confirmButton = {
                DojoButton(
                    text = "Excluir",
                    onClick = {
                        viewModel.viewModelScope.launch {
                            val result = viewModel.deleteStudent(studentId)
                            if (result is Result.Success) {
                                showDeleteConfirm = false
                                finish()
                            }
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                )
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (navigateToEvaluation) {
        val intent = Intent(LocalContext.current, EvaluationActivity::class.java)
        intent.putExtra("STUDENT_ID", studentId)
        LocalContext.current.startActivity(intent)
        navigateToEvaluation = false
    }

    if (navigateToReport) {
        val intent = Intent(LocalContext.current, ReportActivity::class.java)
        intent.putExtra("STUDENT_ID", studentId)
        LocalContext.current.startActivity(intent)
        navigateToReport = false
    }
}

// Reuse StudentFormDialog from MainActivity - need to make it accessible
// For now, duplicate or create a shared component
@Composable
fun StudentFormDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Int) -> Unit,
    student: Student? = null
) {
    var fullName by remember { mutableStateOf(student?.fullName ?? "") }
    var currentBelt by remember { mutableStateOf(student?.currentBelt ?? "") }
    var desiredBelt by remember { mutableStateOf(student?.desiredBelt ?? "") }
    var examDate by remember { mutableStateOf(student?.examDate ?? "") }
    var positionsCount by remember { mutableStateOf(student?.positionsCount.toString() ?? "") }
    var errors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val isEditing = student != null

    val belts = listOf("Branca", "Cinza", "Amarela", "Laranja", "Verde", "Azul", "Roxa", "Marrom", "Preta")

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Editar Aluno" else "Novo Aluno",
                fontSize = 20.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.padding(16.dp).width(300.dp)) {
                androidx.compose.material3.TextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Nome completo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.TextFieldDefaults.textFieldColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = RedPrimary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                    )
                )
                errors["fullName"]?.let { error ->
                    Text(text = error, fontSize = 12.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
                }

                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))

                androidx.compose.material3.ExposedDropdownMenuBox(modifier = Modifier.fillMaxWidth()) { expanded, onExpandedChange ->
                    val selectedText = if (currentBelt.isEmpty()) "Faixa atual" else currentBelt
                    androidx.compose.material3.TextField(
                        value = selectedText,
                        onValueChange = {},
                        label = { Text("Faixa atual") },
                        readOnly = true,
                        trailingIcon = {
                            androidx.compose.material3.Icon(
                                imageVector = if (expanded) androidx.compose.material.icons.Icons.Default.KeyboardArrowUp else androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.TextFieldDefaults.textFieldColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = RedPrimary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    androidx.compose.material3.ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
                        belts.forEach { belt ->
                            androidx.compose.material3.DropdownMenuItem(text = { Text(text = belt) }, onClick = { currentBelt = belt; onExpandedChange(false) })
                        }
                    }
                }
                errors["belt"]?.let { error ->
                    Text(text = error, fontSize = 12.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
                }

                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))

                androidx.compose.material3.ExposedDropdownMenuBox(modifier = Modifier.fillMaxWidth()) { expanded, onExpandedChange ->
                    val selectedText = if (desiredBelt.isEmpty()) "Faixa desejada" else desiredBelt
                    androidx.compose.material3.TextField(
                        value = selectedText,
                        onValueChange = {},
                        label = { Text("Faixa desejada") },
                        readOnly = true,
                        trailingIcon = {
                            androidx.compose.material3.Icon(
                                imageVector = if (expanded) androidx.compose.material.icons.Icons.Default.KeyboardArrowUp else androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.TextFieldDefaults.textFieldColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = RedPrimary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    androidx.compose.material3.ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
                        belts.forEach { belt ->
                            androidx.compose.material3.DropdownMenuItem(text = { Text(text = belt) }, onClick = { desiredBelt = belt; onExpandedChange(false) })
                        }
                    }
                }

                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))

                androidx.compose.material3.TextField(
                    value = examDate,
                    onValueChange = { examDate = it },
                    label = { Text("Data do exame (DD/MM/AAAA)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.TextFieldDefaults.textFieldColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = RedPrimary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                    )
                )
                errors["examDate"]?.let { error ->
                    Text(text = error, fontSize = 12.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
                }

                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))

                androidx.compose.material3.TextField(
                    value = positionsCount,
                    onValueChange = { positionsCount = it },
                    label = { Text("Quantidade de posições") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.TextFieldDefaults.textFieldColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = RedPrimary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                    )
                )
                errors["positionsCount"]?.let { error ->
                    Text(text = error, fontSize = 12.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
                }
            }
        },
        confirmButton = {
            DojoButton(
                text = if (isEditing) "Salvar alterações" else "Salvar",
                onClick = {
                    val validationErrors = mutableMapOf<String, String>()
                    if (fullName.trim().isEmpty()) validationErrors["fullName"] = "Nome é obrigatório"
                    if (currentBelt.isEmpty() || desiredBelt.isEmpty()) validationErrors["belt"] = "Selecione as faixas"
                    if (currentBelt == desiredBelt) validationErrors["belt"] = "Faixa atual e desejada não podem ser iguais"
                    if (examDate.trim().isEmpty()) validationErrors["examDate"] = "Data é obrigatória"
                    val positionsInt = positionsCount.trim().toIntOrNull()
                    if (positionsInt == null || positionsInt < 1) validationErrors["positionsCount"] = "Mínimo 1 posição"
                    if (positionsInt != null && positionsInt > 50) validationErrors["positionsCount"] = "Máximo 50 posições"

                    if (validationErrors.isEmpty()) {
                        onSave(fullName.trim(), currentBelt, desiredBelt, examDate.trim(), positionsInt!!)
                    } else {
                        errors = validationErrors
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity.finish