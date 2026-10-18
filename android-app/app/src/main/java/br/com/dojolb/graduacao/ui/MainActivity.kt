package br.com.dojolb.graduacao.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.dojolb.graduacao.R
import br.com.dojolb.graduacao.data.entity.Student
import br.com.dojolb.graduacao.ui.components.CommonComponents.DojoButton
import br.com.dojolb.graduacao.ui.components.CommonComponents.DojoCard
import br.com.dojolb.graduacao.ui.components.CommonComponents.DojoSurface
import br.com.dojolb.graduacao.ui.components.CommonComponents.EmptyState
import br.com.dojolb.graduacao.ui.components.CommonComponents.InfoRow
import br.com.dojolb.graduacao.ui.components.CommonComponents.ProgressBar
import br.com.dojolb.graduacao.ui.components.CommonComponents.SectionHeader
import br.com.dojolb.graduacao.ui.components.CommonComponents.StatusChip
import br.com.dojolb.graduacao.ui.theme.DojoLBGraduacaoTheme
import br.com.dojolb.graduacao.ui.theme.DojoSurface
import br.com.dojolb.graduacao.ui.theme.RedPrimary
import br.com.dojolb.graduacao.ui.theme.White
import br.com.dojolb.graduacao.ui.viewmodel.StudentViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
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
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: StudentViewModel) {
    val students by viewModel.observeStudents().observeAsState(initialValue = emptyList())
    val isLoading by viewModel.isLoading.observeAsState(initialValue = false)
    var showAddStudent by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top
    ) {
        // Top App Bar
        TopAppBar(
            modifier = Modifier.fillMaxWidth(),
            title = {
                Text(
                    text = "Dojo LB Graduação",
                    fontSize = 20.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = White
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = White
            ),
            actions = {
                IconButton(onClick = { showAddStudent = true }) {
                    Icon(
                        painter = androidx.compose.material.icons.Icons.Default.Add,
                        contentDescription = "Novo Aluno",
                        tint = White
                    )
                }
            }
        )

        // Content
        if (isLoading) {
            br.com.dojolb.graduacao.ui.components.CommonComponents.LoadingOverlay("Carregando alunos…")
        } else if (students.isEmpty()) {
            EmptyState(
                title = "Nenhum aluno cadastrado",
                subtitle = "Toque no botão + para adicionar o primeiro aluno",
                actionText = "Novo Aluno",
                onAction = { showAddStudent = true },
                modifier = Modifier.fillMaxSize().padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 100.dp)
            ) {
                items(students) { student ->
                    StudentCard(student = student, onClick = {
                        val intent = Intent(LocalContext.current, StudentDetailActivity::class.java)
                        intent.putExtra("STUDENT_ID", student.id)
                        LocalContext.current.startActivity(intent)
                    })
                }
            }
        }
    }

    if (showAddStudent) {
        StudentFormDialog(
            onDismiss = { showAddStudent = false },
            onSave = { name, currentBelt, desiredBelt, examDate, positionsCount ->
                viewModel.viewModelScope.launch {
                    val result = viewModel.createStudent(name, currentBelt, desiredBelt, examDate, positionsCount)
                    if (result is Result.Success) {
                        showAddStudent = false
                    }
                }
            }
        )
    }
}

@Composable
fun StudentCard(
    student: Student,
    onClick: () -> Unit
) {
    val evaluatedCount = student.positionsCount // This would come from position count
    val progress = if (student.positionsCount > 0) evaluatedCount.toFloat() / student.positionsCount else 0f

    DojoCard(onClick = onClick) {
        Column {
            // Header with name and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = student.fullName,
                        fontSize = 18.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.unit.dp.toPx(4))
                    Row(
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "${student.currentBelt} → ${student.desiredBelt}",
                            fontSize = 14.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = RedPrimary
                        )
                        Text(
                            text = student.examDate,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                StatusChip(student.result)
            }

            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.unit.dp.toPx(12))

            // Progress info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Posições: ${student.positionsCount}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Média: ${String.format("%.2f%%", student.finalAverage)}",
                    fontSize = 14.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.unit.dp.toPx(8))

            ProgressBar(
                progress = progress,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

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
                // Full Name
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

                // Current Belt
                androidx.compose.material3.ExposedDropdownMenuBox(
                    modifier = Modifier.fillMaxWidth()
                ) { expanded, onExpandedChange ->
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
                    androidx.compose.material3.ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { onExpandedChange(false) }
                    ) {
                        belts.forEach { belt ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(text = belt) },
                                onClick = {
                                    currentBelt = belt
                                    onExpandedChange(false)
                                }
                            )
                        }
                    }
                }
                errors["belt"]?.let { error ->
                    Text(text = error, fontSize = 12.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
                }

                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))

                // Desired Belt
                androidx.compose.material3.ExposedDropdownMenuBox(
                    modifier = Modifier.fillMaxWidth()
                ) { expanded, onExpandedChange ->
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
                    androidx.compose.material3.ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { onExpandedChange(false) }
                    ) {
                        belts.forEach { belt ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(text = belt) },
                                onClick = {
                                    desiredBelt = belt
                                    onExpandedChange(false)
                                }
                            )
                        }
                    }
                }

                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))

                // Exam Date
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

                // Positions Count
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
                    // Validate
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

// Need to import LocalContext
import androidx.compose.ui.platform.LocalContext