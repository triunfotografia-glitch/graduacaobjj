package br.com.dojolb.graduacao.ui.evaluation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.dojolb.graduacao.R
import br.com.dojolb.graduacao.data.entity.Position
import br.com.dojolb.graduacao.data.entity.Student
import br.com.dojolb.graduacao.ui.components.CommonComponents.DojoButton
import br.com.dojolb.graduacao.ui.components.CommonComponents.DojoOutlinedButton
import br.com.dojolb.graduacao.ui.components.CommonComponents.DojoSurface
import br.com.dojolb.graduacao.ui.components.CommonComponents.GradeButton
import br.com.dojolb.graduacao.ui.components.CommonComponents.LoadingOverlay
import br.com.dojolb.graduacao.ui.components.CommonComponents.ProgressBar
import br.com.dojolb.graduacao.ui.components.CommonComponents.SectionHeader
import br.com.dojolb.graduacao.ui.components.CommonComponents.StatusChip
import br.com.dojolb.graduacao.ui.theme.DojoLBGraduacaoTheme
import br.com.dojolb.graduacao.ui.theme.RedPrimary
import br.com.dojolb.graduacao.ui.theme.White
import br.com.dojolb.graduacao.ui.viewmodel.EvaluationViewModel
import kotlinx.coroutines.launch

class EvaluationActivity : ComponentActivity() {
    private val viewModel: EvaluationViewModel by viewModels(
        factory = EvaluationViewModelFactory(
            repository = (application as br.com.dojolb.graduacao.DojoApplication).repository,
            studentId = intent.getLongExtra("STUDENT_ID", -1)
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupDojoWindow()
        setContent {
            DojoLBGraduacaoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EvaluationScreen(
                        viewModel = viewModel,
                        studentId = intent.getLongExtra("STUDENT_ID", -1),
                        shouldReopen = intent.getBooleanExtra("REOPEN", false)
                    )
                }
            }
        }
    }
}

class EvaluationViewModelFactory(
    private val repository: br.com.dojolb.graduacao.data.repository.GraduacaoRepository,
    private val studentId: Long
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return EvaluationViewModel(repository, studentId) as T
    }
}

@Composable
fun EvaluationScreen(
    viewModel: EvaluationViewModel,
    studentId: Long,
    shouldReopen: Boolean
) {
    val student by viewModel.student.observeAsState(initialValue = null)
    val positions by viewModel.positions.observeAsState(initialValue = emptyList())
    val isLoading by viewModel.isLoading.observeAsState(initialValue = false)
    val evaluatedCount by viewModel.evaluatedCount.observeAsState(initialValue = 0)
    val totalCount by viewModel.totalCount.observeAsState(initialValue = 0)
    val provisionalAverage by viewModel.provisionalAverage.observeAsState(initialValue = null)
    val isComplete by viewModel.isComplete.observeAsState(initialValue = false)
    val error by viewModel.error.observeAsState(initialValue = null)

    var showFinalizeDialog by remember { mutableStateOf(false) }
    var showRemoveConfirm by remember { mutableStateOf(false) }
    var positionToRemove by remember { mutableStateOf<Position?>(null) }
    var positionNames by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }

    // Initialize position names
    androidx.compose.runtime.LaunchedEffect(positions) {
        positionNames = positions.associateBy({ it.id }, { it.name })
    }

    if (shouldReopen) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            viewModel.viewModelScope.launch {
                viewModel.reopenEvaluation()
            }
        }
    }

    if (student == null) {
        LoadingOverlay("Carregando aluno…")
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
                    text = "Avaliação: ${student.fullName}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
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
            )
        )

        if (isLoading) {
            LoadingOverlay("Carregando avaliação…")
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Info
                DojoSurface {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${student.currentBelt} → ${student.desiredBelt}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RedPrimary
                                )
                                Text(
                                    text = "Data: ${student.examDate}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusChip(student.result)
                        }
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Avaliadas: $evaluatedCount de $totalCount",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            provisionalAverage?.let { avg ->
                                Text(
                                    text = "Média provisória: ${String.format("%.2f%%", avg)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = RedPrimary
                                )
                            }
                        }
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))
                        ProgressBar(
                            progress = if (totalCount > 0) evaluatedCount.toFloat() / totalCount else 0f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Instructions
                DojoSurface {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Text(
                            text = "Toque na nota para avaliar cada posição",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = androidx.compose.ui.text.TextStyle.Italic
                        )
                    }
                }

                // Positions List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(positions) { index, position ->
                        PositionItem(
                            position = position,
                            positionNumber = index + 1,
                            positionName = positionNames[position.id] ?: position.name,
                            onGradeClick = { grade ->
                                viewModel.viewModelScope.launch {
                                    viewModel.updatePositionGrade(position, grade)
                                }
                            },
                            onNameChange = { newName ->
                                positionNames = positionNames + (position.id to newName)
                                viewModel.viewModelScope.launch {
                                    viewModel.updatePositionName(position, newName)
                                }
                            },
                            onRemoveClick = {
                                positionToRemove = position
                                showRemoveConfirm = true
                            },
                            showRemove = positions.size > 1
                        )
                    }
                }

                // Bottom Actions
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DojoOutlinedButton(
                            text = "+ Adicionar Posição",
                            onClick = {
                                viewModel.viewModelScope.launch {
                                    viewModel.addPosition()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        DojoOutlinedButton(
                            text = "Remover última",
                            onClick = {
                                positionToRemove = positions.lastOrNull()
                                showRemoveConfirm = true
                            },
                            borderColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))
                    DojoButton(
                        text = if (isComplete) "Ver Relatório" else "Finalizar Exame",
                        onClick = {
                            if (isComplete) {
                                val intent = Intent(LocalContext.current, ReportActivity::class.java)
                                intent.putExtra("STUDENT_ID", studentId)
                                LocalContext.current.startActivity(intent)
                            } else {
                                showFinalizeDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // Finalize Dialog
    if (showFinalizeDialog) {
        FinalizeEvaluationDialog(
            student = student,
            positions = positions,
            evaluatedCount = evaluatedCount,
            totalCount = totalCount,
            provisionalAverage = provisionalAverage,
            onConfirm = {
                viewModel.viewModelScope.launch {
                    val result = viewModel.finalizeEvaluation()
                    if (result is Result.Success) {
                        showFinalizeDialog = false
                        val intent = Intent(LocalContext.current, ReportActivity::class.java)
                        intent.putExtra("STUDENT_ID", studentId)
                        LocalContext.current.startActivity(intent)
                    }
                }
            },
            onDismiss = { showFinalizeDialog = false }
        )
    }

    // Remove Position Confirm
    if (showRemoveConfirm && positionToRemove != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRemoveConfirm = false; positionToRemove = null },
            title = { Text("Remover posição", fontWeight = FontWeight.Bold) },
            text = {
                Text("A última posição será removida. Se já tiver nota, ela será perdida. Continuar?")
            },
            confirmButton = {
                DojoButton(
                    text = "Remover",
                    onClick = {
                        viewModel.viewModelScope.launch {
                            viewModel.removeLastPosition()
                        }
                        showRemoveConfirm = false
                        positionToRemove = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                )
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showRemoveConfirm = false; positionToRemove = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun PositionItem(
    position: Position,
    positionNumber: Int,
    positionName: String,
    onGradeClick: (Int) -> Unit,
    onNameChange: (String) -> Unit,
    onRemoveClick: () -> Unit,
    showRemove: Boolean
) {
    var name by remember { mutableStateOf(positionName) }

    DojoSurface {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            // Header with position number and name field
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Posição $positionNumber",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (showRemove) {
                    IconButton(onClick = onRemoveClick) {
                        Icon(
                            painter = androidx.compose.material.icons.Icons.Default.Delete,
                            contentDescription = "Remover posição",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))

            // Name field
            androidx.compose.material3.TextField(
                value = name,
                onValueChange = { name = it; onNameChange(it) },
                label = { Text("Nome da posição (opcional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.TextFieldDefaults.textFieldColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = RedPrimary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                )
            )

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))

            // Grade buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(0, 25, 50, 75, 100).forEach { grade ->
                    GradeButton(
                        grade = grade,
                        isSelected = position.grade == grade,
                        onClick = { onGradeClick(grade) },
                        modifier = Modifier.weight(1f),
                        enabled = true
                    )
                }
            }

            // Current grade indicator
            if (position.grade != Position.GRADE_NOT_EVALUATED) {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Nota selecionada: ${position.grade}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = RedPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun FinalizeEvaluationDialog(
    student: Student,
    positions: List<Position>,
    evaluatedCount: Int,
    totalCount: Int,
    provisionalAverage: Double?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val pendingPositions = positions.filter { it.grade == Position.GRADE_NOT_EVALUATED }
    val hasPending = pendingPositions.isNotEmpty()
    val average = provisionalAverage ?: 0.0
    val result = if (average > 75.0) "APROVADO" else "REPROVADO"
    val isApproved = average > 75.0

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Finalização do Exame", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column(modifier = Modifier.padding(16.dp).width(300.dp)) {
                Text(
                    text = "Confira todas as notas antes de concluir",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))

                if (hasPending) {
                    Text(
                        text = "⚠ Existem ${pendingPositions.size} posições sem avaliação:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        pendingPositions.forEach { pos ->
                            Text(
                                text = "• Posição ${pos.positionNumber}: ${pos.name.ifEmpty { "Posição ${pos.positionNumber}" }}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Text(
                        text = "✓ Todas as posições foram avaliadas",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 16.dp))

                // Summary
                DojoSurface {
                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Aluno", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(student.fullName, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Faixa atual", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(student.currentBelt, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Faixa desejada", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(student.desiredBelt, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Data do exame", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(student.examDate, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total de posições", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(totalCount.toString(), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Posições avaliadas", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(evaluatedCount.toString(), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Média final", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(String.format("%.2f%%", average), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RedPrimary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Resultado", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(result, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (isApproved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        }
                    }
                }

                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))
                Text(
                    text = "Regra: média > 75% = Aprovado | média ≤ 75% = Reprovado",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.TextAlign.Center
                )
            }
        },
        confirmButton = {
            DojoButton(
                text = "Confirmar e Finalizar",
                onClick = { onConfirm(); onDismiss() },
                enabled = !hasPending,
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Voltar à Avaliação")
            }
        }
    )
}