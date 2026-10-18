package br.com.dojolb.graduacao.ui.report

import android.content.Intent
import android.net.Uri
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
import br.com.dojolb.graduacao.ui.components.CommonComponents.LoadingOverlay
import br.com.dojolb.graduacao.ui.components.CommonComponents.SectionHeader
import br.com.dojolb.graduacao.ui.components.CommonComponents.StatusChip
import br.com.dojolb.graduacao.ui.theme.DojoLBGraduacaoTheme
import br.com.dojolb.graduacao.ui.theme.RedPrimary
import br.com.dojolb.graduacao.ui.theme.White
import br.com.dojolb.graduacao.ui.viewmodel.ReportViewModel
import kotlinx.coroutines.launch

class ReportActivity : ComponentActivity() {
    private val viewModel: ReportViewModel by viewModels(
        factory = ReportViewModelFactory(
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
                    ReportScreen(viewModel = viewModel)
                }
            }
        }
    }
}

class ReportViewModelFactory(
    private val repository: br.com.dojolb.graduacao.data.repository.GraduacaoRepository,
    private val studentId: Long
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return ReportViewModel(repository, studentId) as T
    }
}

@Composable
fun ReportScreen(viewModel: ReportViewModel) {
    val reportData by remember { mutableStateOf<ReportViewModel.ReportData?>(null) }
    val isLoading by viewModel.isLoading.observeAsState(initialValue = false)
    val error by viewModel.error.observeAsState(initialValue = null)
    val exportSuccess by viewModel.exportSuccess.observeAsState(initialValue = null)

    var observations by remember { mutableStateOf("") }
    var showExportOptions by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf<Uri?>(null) }

    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.loadData()
    }

    androidx.compose.runtime.LaunchedEffect(viewModel.student, viewModel.positions) {
        val student = viewModel.student.value
        val positions = viewModel.positions.value
        if (student != null) {
            val average = student.finalAverage
            val result = student.result
            reportData = ReportViewModel.ReportData(student, positions, average, result)
        }
    }

    if (isLoading && reportData == null) {
        LoadingOverlay("Carregando relatório…")
        return
    }

    val data = reportData ?: return

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top
    ) {
        TopAppBar(
            modifier = Modifier.fillMaxWidth(),
            title = {
                Text(
                    text = "Relatório: ${data.student.fullName}",
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

        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            DojoSurface {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DOJO LB",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = RedPrimary
                            )
                            Text(
                                text = "Relatório de Exame de Graduação",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusChip(data.result)
                    }
                }
            }

            // Student Info
            DojoSurface {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    SectionHeader("Dados do Aluno")
                    br.com.dojolb.graduacao.ui.components.CommonComponents.InfoRow("Aluno", data.student.fullName)
                    br.com.dojolb.graduacao.ui.components.CommonComponents.InfoRow("Faixa atual", data.student.currentBelt)
                    br.com.dojolb.graduacao.ui.components.CommonComponents.InfoRow("Faixa desejada", data.student.desiredBelt)
                    br.com.dojolb.graduacao.ui.components.CommonComponents.InfoRow("Data do exame", data.formattedDate)
                    br.com.dojolb.graduacao.ui.components.CommonComponents.InfoRow("Total de posições", data.positions.size.toString())
                }
            }

            // Positions Table
            DojoSurface {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    SectionHeader("Posições Avaliadas")
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("#", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(0.1f))
                            Text("Posição", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(0.7f).padding(start = 8.dp))
                            Text("Nota", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(0.2f).padding(end = 8.dp))
                        }

                        data.positions.forEachIndexed { index, position ->
                            val bgColor = if (index % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                                    .background(bgColor),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${position.positionNumber}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(0.1f)
                                )
                                Text(
                                    position.name.ifEmpty { "Posição ${position.positionNumber}" },
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(0.7f).padding(start = 8.dp),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.TextOverflow.Ellipsis
                                )
                                Text(
                                    if (position.grade == Position.GRADE_NOT_EVALUATED) "—" else "${position.grade}%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (position.grade == Position.GRADE_NOT_EVALUATED) MaterialTheme.colorScheme.onSurfaceVariant else RedPrimary,
                                    modifier = Modifier.weight(0.2f).padding(end = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Final Result
            DojoSurface {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    SectionHeader("Resultado Final")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Média final", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(data.formattedAverage, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = RedPrimary)
                    }
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Resultado", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (data.isApproved) "APROVADO" else "REPROVADO",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (data.isApproved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))
                    Text(
                        text = "Regra: média > 75% = Aprovado | média ≤ 75% = Reprovado",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.TextAlign.Center
                    )
                }
            }

            // Observations
            DojoSurface {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    SectionHeader("Observações do Professor")
                    androidx.compose.material3.TextField(
                        value = observations,
                        onValueChange = { observations = it },
                        label = { Text("Digite observações opcionais…") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        colors = androidx.compose.material3.TextFieldDefaults.textFieldColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = RedPrimary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }

            // Export Buttons
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DojoButton(
                    text = "Exportar PDF",
                    onClick = {
                        viewModel.viewModelScope.launch {
                            val result = viewModel.exportPdf(observations)
                            if (result is Result.Success) {
                                shareFile(result.getOrNull())
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                DojoOutlinedButton(
                    text = "Exportar CSV",
                    onClick = {
                        viewModel.viewModelScope.launch {
                            val result = viewModel.exportCsv()
                            if (result is Result.Success) {
                                shareFile(result.getOrNull())
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Error dialog
    error?.let { err ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { /* error handled */ },
            title = { Text("Erro", fontWeight = FontWeight.Bold) },
            text = { Text(err) },
            confirmButton = {
                androidx.compose.material3.Button(onClick = { /* dismiss */ }) {
                    Text("OK")
                }
            }
        )
    }

    // Success toast would go here
}

private fun shareFile(file: java.io.File?) {
    file?.let { f ->
        val uri = androidx.core.content.FileProvider.getUriForFile(
            LocalContext.current,
            "${LocalContext.current.packageName}.fileprovider",
            f
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = when {
                f.name.endsWith(".pdf") -> "application/pdf"
                f.name.endsWith(".csv") -> "text/csv"
                else -> "*/*"
            }
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Compartilhar relatório")
        LocalContext.current.startActivity(chooser)
    }
}

import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity.finish
import androidx.core.content.FileProvider