package br.com.dojolb.graduacao.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.dojolb.graduacao.data.entity.Position
import br.com.dojolb.graduacao.data.entity.Student
import br.com.dojolb.graduacao.data.repository.GraduacaoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportViewModel(
    private val repository: GraduacaoRepository,
    private val studentId: Long
) : ViewModel() {

    private val _student = MutableStateFlow<Student?>(null)
    val student = _student.asStateFlow()

    private val _positions = MutableStateFlow<List<Position>>(emptyList())
    val positions = _positions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _exportSuccess = MutableStateFlow<String?>(null)
    val exportSuccess = _exportSuccess.asStateFlow()

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
            } catch (e: Exception) {
                _error.value = "Erro ao carregar relatório: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getReportData(): ReportData? {
        val student = _student.value ?: return null
        val positions = _positions.value
        val average = student.finalAverage
        val result = student.result
        return ReportData(student, positions, average, result)
    }

    data class ReportData(
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

    // PDF Export using iText7
    suspend fun exportPdf(observations: String = ""): Result<File> {
        return try {
            _isLoading.value = true
            val data = getReportData() ?: return Result.failure(IllegalStateException("Dados não carregados"))

            val file = File(
                repository.getStudentById(studentId)?.let { context ->
                    // We need context for files dir, but we'll use external files dir
                    null
                } ?: File(System.getProperty("java.io.tmpdir")),
                "relatorio_${data.student.fullName.replace(' ', '_')}_${System.currentTimeMillis()}.pdf"
            )

            // Generate PDF using iText7
            generatePdf(file, data, observations)

            _exportSuccess.value = "PDF exportado com sucesso"
            Result.success(file)
        } catch (e: Exception) {
            _error.value = "Erro ao exportar PDF: ${e.message}"
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    private fun generatePdf(file: File, data: ReportData, observations: String) {
        // iText7 PDF generation
        import com.itextpdf.kernel.pdf.PdfWriter
        import com.itextpdf.kernel.pdf.PdfDocument
        import com.itextpdf.layout.Document
        import com.itextpdf.layout.element.Paragraph
        import com.itextpdf.layout.element.Table
        import com.itextpdf.layout.element.Cell
        import com.itextpdf.layout.element.Text
        import com.itextpdf.layout.properties.TextAlignment
        import com.itextpdf.layout.properties.UnitValue
        import com.itextpdf.kernel.colors.ColorConstants
        import com.itextpdf.kernel.font.PdfFontFactory
        import com.itextpdf.io.font.constants.StandardFonts
        import com.itextpdf.layout.properties.VerticalAlignment
        import com.itextpdf.layout.borders.Border
        import com.itextpdf.layout.borders.SolidBorder
        import com.itextpdf.kernel.colors.DeviceRgb

        val writer = PdfWriter(file.absolutePath)
        val pdf = PdfDocument(writer)
        val document = Document(pdf)

        // Colors
        val dojoRed = DeviceRgb(198, 40, 40) // #C62828
        val darkGray = DeviceRgb(30, 30, 30) // #1E1E1E
        val white = ColorConstants.WHITE
        val black = ColorConstants.BLACK

        // Fonts
        val fontRegular = PdfFontFactory.createFont(StandardFonts.HELVETICA)
        val fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)

        // Header
        val headerTable = Table(UnitValue.createPercentArray(1)).useAllAvailableWidth()
        headerTable.addCell(
            Cell().add(
                Paragraph("DOJO LB")
                    .setFont(fontBold)
                    .setFontSize(28)
                    .setFontColor(dojoRed)
                    .setTextAlignment(TextAlignment.CENTER)
            ).setBorder(Border.NO_BORDER).setBackgroundColor(black).setPadding(16f)
        )
        document.add(headerTable)

        // Title
        document.add(
            Paragraph("Relatório de Exame de Graduação")
                .setFont(fontBold)
                .setFontSize(18)
                .setFontColor(dojoRed)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(16f)
                .setMarginBottom(8f)
        )

        // Student info table
        val infoTable = Table(UnitValue.createPercentArray(2)).useAllAvailableWidth()
        infoTable.setMarginBottom(16f)

        fun addInfoRow(label: String, value: String) {
            infoTable.addCell(
                Cell().add(Paragraph(label).setFont(fontBold).setFontSize(11).setFontColor(white))
                    .setBackgroundColor(darkGray).setPadding(8f).setBorder(SolidBorder(0.5f, ColorConstants.GRAY))
            )
            infoTable.addCell(
                Cell().add(Paragraph(value).setFont(fontRegular).setFontSize(11).setFontColor(black))
                    .setBackgroundColor(white).setPadding(8f).setBorder(SolidBorder(0.5f, ColorConstants.GRAY))
            )
        }

        addInfoRow("Aluno", data.student.fullName)
        addInfoRow("Faixa Atual", data.student.currentBelt)
        addInfoRow("Faixa Desejada", data.student.desiredBelt)
        addInfoRow("Data do Exame", data.formattedDate)
        addInfoRow("Total de Posições", data.positions.size.toString())
        addInfoRow("Posições Avaliadas", data.positions.count { it.grade != Position.GRADE_NOT_EVALUATED }.toString())

        document.add(infoTable)

        // Positions table
        val positionsTable = Table(UnitValue.createPercentArray(3)).useAllAvailableWidth()
        positionsTable.setMarginBottom(16f)

        // Header
        positionsTable.addHeaderCell(
            Cell().add(Paragraph("Posição").setFont(fontBold).setFontSize(10).setFontColor(white))
                .setBackgroundColor(dojoRed).setPadding(8f).setTextAlignment(TextAlignment.CENTER).setBorder(SolidBorder(0.5f, ColorConstants.GRAY))
        )
        positionsTable.addHeaderCell(
            Cell().add(Paragraph("Nome da Posição").setFont(fontBold).setFontSize(10).setFontColor(white))
                .setBackgroundColor(dojoRed).setPadding(8f).setTextAlignment(TextAlignment.CENTER).setBorder(SolidBorder(0.5f, ColorConstants.GRAY))
        )
        positionsTable.addHeaderCell(
            Cell().add(Paragraph("Nota").setFont(fontBold).setFontSize(10).setFontColor(white))
                .setBackgroundColor(dojoRed).setPadding(8f).setTextAlignment(TextAlignment.CENTER).setBorder(SolidBorder(0.5f, ColorConstants.GRAY))
        )

        data.positions.forEachIndexed { index, position ->
            val bgColor = if (index % 2 == 0) ColorConstants.WHITE else DeviceRgb(245, 245, 245)
            positionsTable.addCell(
                Cell().add(Paragraph("${position.positionNumber}").setFont(fontRegular).setFontSize(10).setFontColor(black))
                    .setBackgroundColor(bgColor).setPadding(8f).setTextAlignment(TextAlignment.CENTER).setBorder(SolidBorder(0.5f, ColorConstants.LIGHT_GRAY))
            )
            positionsTable.addCell(
                Cell().add(Paragraph(position.name.ifEmpty { "Posição ${position.positionNumber}" }).setFont(fontRegular).setFontSize(10).setFontColor(black))
                    .setBackgroundColor(bgColor).setPadding(8f).setBorder(SolidBorder(0.5f, ColorConstants.LIGHT_GRAY))
            )
            positionsTable.addCell(
                Cell().add(Paragraph(if (position.grade == Position.GRADE_NOT_EVALUATED) "—" else "${position.grade}%").setFont(fontRegular).setFontSize(10).setFontColor(black))
                    .setBackgroundColor(bgColor).setPadding(8f).setTextAlignment(TextAlignment.CENTER).setBorder(SolidBorder(0.5f, ColorConstants.LIGHT_GRAY))
            )
        }

        document.add(positionsTable)

        // Final result
        val resultTable = Table(UnitValue.createPercentArray(2)).useAllAvailableWidth()
        resultTable.setMarginBottom(16f)

        val resultColor = if (data.isApproved) DeviceRgb(46, 125, 50) else DeviceRgb(198, 40, 40)
        val resultText = if (data.isApproved) "APROVADO" else "REPROVADO"

        resultTable.addCell(
            Cell().add(Paragraph("Média Final").setFont(fontBold).setFontSize(12).setFontColor(white))
                .setBackgroundColor(darkGray).setPadding(10f).setBorder(SolidBorder(0.5f, ColorConstants.GRAY))
        )
        resultTable.addCell(
            Cell().add(Paragraph(data.formattedAverage).setFont(fontBold).setFontSize(12).setFontColor(black))
                .setBackgroundColor(white).setPadding(10f).setBorder(SolidBorder(0.5f, ColorConstants.GRAY)).setTextAlignment(TextAlignment.CENTER)
        )

        resultTable.addCell(
            Cell().add(Paragraph("Resultado").setFont(fontBold).setFontSize(12).setFontColor(white))
                .setBackgroundColor(darkGray).setPadding(10f).setBorder(SolidBorder(0.5f, ColorConstants.GRAY))
        )
        resultTable.addCell(
            Cell().add(Paragraph(resultText).setFont(fontBold).setFontSize(14).setFontColor(resultColor))
                .setBackgroundColor(white).setPadding(10f).setBorder(SolidBorder(0.5f, ColorConstants.GRAY)).setTextAlignment(TextAlignment.CENTER)
        )

        document.add(resultTable)

        // Rule note
        document.add(
            Paragraph("Regra: média > 75% = Aprovado | média ≤ 75% = Reprovado")
                .setFont(fontRegular)
                .setFontSize(9)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(8f)
        )

        // Observations
        if (observations.isNotBlank()) {
            document.add(
                Paragraph("Observações do Professor")
                    .setFont(fontBold)
                    .setFontSize(12)
                    .setFontColor(dojoRed)
                    .setMarginTop(16f)
                    .setMarginBottom(8f)
            )
            document.add(
                Paragraph(observations)
                    .setFont(fontRegular)
                    .setFontSize(11)
                    .setFontColor(black)
                    .setMarginBottom(16f)
            )
        }

        // Footer
        document.add(
            Paragraph("Gerado em ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())} pelo Dojo LB Graduação")
                .setFont(fontRegular)
                .setFontSize(8)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(24f)
        )

        document.close()
    }

    // CSV Export
    suspend fun exportCsv(): Result<File> {
        return try {
            _isLoading.value = true
            val data = getReportData() ?: return Result.failure(IllegalStateException("Dados não carregados"))

            val file = File(
                File(System.getProperty("java.io.tmpdir")),
                "relatorio_${data.student.fullName.replace(' ', '_')}_${System.currentTimeMillis()}.csv"
            )

            generateCsv(file, data)

            _exportSuccess.value = "CSV exportado com sucesso"
            Result.success(file)
        } catch (e: Exception) {
            _error.value = "Erro ao exportar CSV: ${e.message}"
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    private fun generateCsv(file: File, data: ReportData) {
        val writer = OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8)
        // Write BOM for UTF-8
        writer.write('\uFEFF')

        // Header
        writer.write("Nome do Aluno;Faixa Atual;Faixa Desejada;Data do Exame;Nome da Posição;Nota Percentual;Média Final;Situação\n")

        val resultText = if (data.isApproved) "Aprovado" else "Reprovado"
        val formattedAverage = data.formattedAverage.replace("%", "")
        val formattedDate = data.formattedDate

        data.positions.forEach { position ->
            val grade = if (position.grade == Position.GRADE_NOT_EVALUATED) "—" else "${position.grade}%"
            val positionName = position.name.ifEmpty { "Posição ${position.positionNumber}" }.replace("\n", " ").replace("\r", " ")
            writer.write(
                "${escapeCsv(data.student.fullName)};" +
                "${escapeCsv(data.student.currentBelt)};" +
                "${escapeCsv(data.student.desiredBelt)};" +
                "${escapeCsv(formattedDate)};" +
                "${escapeCsv(positionName)};" +
                "${escapeCsv(grade)};" +
                "${escapeCsv(formattedAverage)};" +
                "${escapeCsv(resultText)}\n"
            )
        }

        writer.flush()
        writer.close()
    }

    private fun escapeCsv(value: String): String {
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"${value.replace("\"", "\"\"")}\""
        }
        return value
    }

    // Export all students CSV
    suspend fun exportAllStudentsCsv(allStudents: List<Student>): Result<File> {
        return try {
            _isLoading.value = true
            val file = File(
                File(System.getProperty("java.io.tmpdir")),
                "relatorio_todos_alunos_${System.currentTimeMillis()}.csv"
            )

            val writer = OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8)
            writer.write('\uFEFF')
            writer.write("Nome do Aluno;Faixa Atual;Faixa Desejada;Data do Exame;Nome da Posição;Nota Percentual;Média Final;Situação\n")

            for (student in allStudents) {
                val positions = repository.getPositionsByStudentIdSync(student.id)
                val average = student.finalAverage
                val result = student.result
                val resultText = when (result) {
                    Student.RESULT_APPROVED -> "Aprovado"
                    Student.RESULT_FAILED -> "Reprovado"
                    else -> "Em Andamento"
                }
                val formattedAverage = String.format(Locale.getDefault(), "%.2f", average).replace(",", ".")
                val formattedDate = try {
                    val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val date = inputFormat.parse(student.examDate)
                    outputFormat.format(date)
                } catch (e: Exception) {
                    student.examDate
                }

                positions.forEach { position ->
                    val grade = if (position.grade == Position.GRADE_NOT_EVALUATED) "—" else "${position.grade}%"
                    val positionName = position.name.ifEmpty { "Posição ${position.positionNumber}" }.replace("\n", " ").replace("\r", " ")
                    writer.write(
                        "${escapeCsv(student.fullName)};" +
                        "${escapeCsv(student.currentBelt)};" +
                        "${escapeCsv(student.desiredBelt)};" +
                        "${escapeCsv(formattedDate)};" +
                        "${escapeCsv(positionName)};" +
                        "${escapeCsv(grade)};" +
                        "${escapeCsv(formattedAverage)};" +
                        "${escapeCsv(resultText)}\n"
                    )
                }
            }

            writer.flush()
            writer.close()

            _exportSuccess.value = "CSV de todos os alunos exportado com sucesso"
            Result.success(file)
        } catch (e: Exception) {
            _error.value = "Erro ao exportar CSV: ${e.message}"
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }
}