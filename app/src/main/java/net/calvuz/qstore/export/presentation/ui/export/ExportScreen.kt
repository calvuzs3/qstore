package net.calvuz.qstore.export.presentation.ui.export

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import net.calvuz.qstore.export.domain.model.ExportFormat
import net.calvuz.qstore.export.domain.model.ExportResult
import java.io.File

@Composable
fun ExportScreen(
    viewModel: ExportViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.result) {
        when (val result = uiState.result) {
            is ExportResult.Success -> {
                snackbarHostState.showSnackbar("Esportati ${result.itemCount} articoli")
            }
            is ExportResult.Error -> {
                snackbarHostState.showSnackbar(result.message)
            }
            null -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Esporta Inventario",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Formato
            Text(
                text = "Formato",
                style = MaterialTheme.typography.titleMedium
            )

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = uiState.selectedFormat == ExportFormat.CSV,
                        onClick = { viewModel.setFormat(ExportFormat.CSV) }
                    )
                    Text(
                        text = "CSV (separatore ;)",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = uiState.selectedFormat == ExportFormat.EXCEL,
                        onClick = { viewModel.setFormat(ExportFormat.EXCEL) }
                    )
                    Text(
                        text = "Excel (.xlsx)",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Opzione foto
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = uiState.includePhotos,
                    onCheckedChange = { viewModel.setIncludePhotos(it) }
                )
                Text(
                    text = "Includi foto (crea ZIP)",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Risultato
            when (val result = uiState.result) {
                is ExportResult.Success -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "File salvato in:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = result.filePath,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { shareFile(context, result.filePath) }
                        ) {
                            Text("Condividi")
                        }
                    }
                }
                else -> {}
            }

            // Pulsante esporta
            Button(
                onClick = { viewModel.export() },
                enabled = !uiState.isExporting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                }
                Text(if (uiState.isExporting) "Esportazione..." else "Esporta")
            }
        }
    }
}

private fun shareFile(context: android.content.Context, filePath: String) {
    val file = File(filePath)
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = when {
            filePath.endsWith(".csv") -> "text/csv"
            filePath.endsWith(".xlsx") -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            filePath.endsWith(".zip") -> "application/zip"
            else -> "*/*"
        }
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(intent, "Condividi export"))
}
