package net.calvuz.qstore.app.presentation.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qstore.app.domain.model.RecognitionSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecognitionSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecognitionSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentSettings by viewModel.currentSettings.collectAsStateWithLifecycle()
    val currentPreset by viewModel.currentPreset.collectAsStateWithLifecycle()

    // Gestione messaggi di stato
    LaunchedEffect(uiState.error, uiState.message) {
        if (uiState.error != null || uiState.message != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Impostazioni Riconoscimento") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBackIosNew, "Indietro")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.resetToDefault() }) {
                        Icon(Icons.Default.RestartAlt, "Reset")
                    }
                }
            )
        }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Messaggi di stato
            if (uiState.error != null) {
                item {
                    ErrorCard(message = uiState.error!!)
                }
            }

            if (uiState.message != null) {
                item {
                    SuccessCard(message = uiState.message!!)
                }
            }

            // Preset rapidi
            item {
                PresetSection(
                    currentPreset = currentPreset,
                    onPresetSelected = viewModel::applyPreset,
                    isLoading = uiState.isLoading
                )
            }

            // Validazione parametri
            if (uiState.validationErrors.isNotEmpty()) {
                item {
                    ValidationErrorsCard(errors = uiState.validationErrors)
                }
            }

            // Parametri Matching
            item {
                MatchingParametersSection(
                    settings = currentSettings,
                    onUpdateParameter = viewModel::updateParameter,
                    isEnabled = !uiState.isLoading
                )
            }

            // Pesi Similarity
            item {
                SimilarityWeightsSection(
                    settings = currentSettings,
                    onUpdateParameter = viewModel::updateParameter,
                    isEnabled = !uiState.isLoading
                )
            }

            // Parametri Validazione
            item {
                ValidationParametersSection(
                    settings = currentSettings,
                    onUpdateParameter = viewModel::updateParameter,
                    isEnabled = !uiState.isLoading
                )
            }

            // Test Impostazioni
            item {
                TestSection(
                    uiState = uiState,
                    onTestSettings = viewModel::testCurrentSettings
                )
            }
        }
    }
}

@Composable
private fun PresetSection(
    currentPreset: String?,
    onPresetSelected: (String) -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Preset Predefiniti",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Configurazioni ottimizzate per diversi scenari d'uso",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PresetButton(
                    name = "Preciso",
                    description = "Più accurato, più lento",
                    isSelected = currentPreset == "Preciso",
                    onSelect = onPresetSelected,
                    isEnabled = !isLoading,
                    modifier = Modifier.weight(1f)
                )

                PresetButton(
                    name = "Bilanciato",
                    description = "Equilibrio qualità/velocità",
                    isSelected = currentPreset == "Bilanciato",
                    onSelect = onPresetSelected,
                    isEnabled = !isLoading,
                    modifier = Modifier.weight(1f)
                )

                PresetButton(
                    name = "Veloce",
                    description = "Più rapido, meno preciso",
                    isSelected = currentPreset == "Veloce",
                    onSelect = onPresetSelected,
                    isEnabled = !isLoading,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PresetButton(
    name: String,
    description: String,
    isSelected: Boolean,
    onSelect: (String) -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        onClick = { onSelect(name) },
        modifier = modifier,
        enabled = isEnabled,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MatchingParametersSection(
    settings: RecognitionSettings,
    onUpdateParameter: ((RecognitionSettings) -> RecognitionSettings) -> Unit,
    isEnabled: Boolean
) {
    ParameterSection(
        title = "Parametri Matching",
        description = "Controlli principali per il riconoscimento"
    ) {
        // Lowe Ratio Threshold
        SliderParameter(
            label = "Lowe Ratio Threshold",
            value = settings.loweRatioThreshold,
            valueRange = 0.3f..0.9f,
            steps = 59,
            onValueChange = { newValue ->
                onUpdateParameter { it.copy(loweRatioThreshold = newValue) }
            },
            isEnabled = isEnabled,
            formatValue = { "%.2f".format(it) },
            description = "Soglia per filtro qualità features (più basso = più rigoroso)"
        )

        // Absolute Distance Threshold
        SliderParameter(
            label = "Distance Threshold",
            value = settings.absoluteDistanceThreshold,
            valueRange = 100f..500f,
            steps = 79,
            onValueChange = { newValue ->
                onUpdateParameter { it.copy(absoluteDistanceThreshold = newValue) }
            },
            isEnabled = isEnabled,
            formatValue = { "%.0f".format(it) },
            description = "Distanza massima accettabile per match"
        )

        // Min Features
        SliderParameter(
            label = "Min Features",
            value = settings.minFeatures.toFloat(),
            valueRange = 10f..100f,
            steps = 17,
            onValueChange = { newValue ->
                onUpdateParameter { it.copy(minFeatures = newValue.toInt()) }
            },
            isEnabled = isEnabled,
            formatValue = { "%.0f".format(it) },
            description = "Numero minimo di features richieste per il matching"
        )

        // Default Matching Threshold
        SliderParameter(
            label = "Soglia Matching Generale",
            value = settings.defaultMatchingThreshold.toFloat(),
            valueRange = 0.1f..1.0f,
            steps = 89,
            onValueChange = { newValue ->
                onUpdateParameter { it.copy(defaultMatchingThreshold = newValue.toDouble()) }
            },
            isEnabled = isEnabled,
            formatValue = { "%.2f".format(it) },
            description = "Soglia globale per considerare un match valido"
        )
    }
}

@Composable
private fun SimilarityWeightsSection(
    settings: RecognitionSettings,
    onUpdateParameter: ((RecognitionSettings) -> RecognitionSettings) -> Unit,
    isEnabled: Boolean
) {
    ParameterSection(
        title = "Pesi Calcolo Similarità",
        description = "Importanza relativa dei fattori nel calcolo similarità (somma = 1.0)"
    ) {
        // Match Ratio Weight
        SliderParameter(
            label = "Peso Match Ratio",
            value = settings.matchRatioWeight.toFloat(),
            valueRange = 0.0f..1.0f,
            steps = 99,
            onValueChange = { newValue ->
                onUpdateParameter { it.copy(matchRatioWeight = newValue.toDouble()) }
            },
            isEnabled = isEnabled,
            formatValue = { "%.2f".format(it) },
            description = "Importanza del numero di match trovati"
        )

        // Density Weight
        SliderParameter(
            label = "Peso Densità",
            value = settings.densityWeight.toFloat(),
            valueRange = 0.0f..1.0f,
            steps = 99,
            onValueChange = { newValue ->
                onUpdateParameter { it.copy(densityWeight = newValue.toDouble()) }
            },
            isEnabled = isEnabled,
            formatValue = { "%.2f".format(it) },
            description = "Importanza della densità di features"
        )

        // Distance Quality Weight
        SliderParameter(
            label = "Peso Qualità Distanza",
            value = settings.distanceQualityWeight.toFloat(),
            valueRange = 0.0f..1.0f,
            steps = 99,
            onValueChange = { newValue ->
                onUpdateParameter { it.copy(distanceQualityWeight = newValue.toDouble()) }
            },
            isEnabled = isEnabled,
            formatValue = { "%.2f".format(it) },
            description = "Importanza della qualità delle distanze"
        )

        // Consistency Weight
        SliderParameter(
            label = "Peso Consistenza",
            value = settings.consistencyWeight.toFloat(),
            valueRange = 0.0f..1.0f,
            steps = 99,
            onValueChange = { newValue ->
                onUpdateParameter { it.copy(consistencyWeight = newValue.toDouble()) }
            },
            isEnabled = isEnabled,
            formatValue = { "%.2f".format(it) },
            description = "Importanza della consistenza delle distanze"
        )

        // Visualizza somma pesi
        val totalWeight = settings.matchRatioWeight + settings.densityWeight +
                settings.distanceQualityWeight + settings.consistencyWeight

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Somma pesi:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "%.3f".format(totalWeight),
                style = MaterialTheme.typography.bodyMedium,
                color = if (totalWeight in 0.95..1.05) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }
    }
}

@Composable
private fun ValidationParametersSection(
    settings: RecognitionSettings,
    onUpdateParameter: ((RecognitionSettings) -> RecognitionSettings) -> Unit,
    isEnabled: Boolean
) {
    ParameterSection(
        title = "Parametri Validazione",
        description = "Soglie per la validazione qualità immagini"
    ) {
        // Min Features for Validation
        SliderParameter(
            label = "Min Features Validazione",
            value = settings.minFeaturesForValidation.toFloat(),
            valueRange = 10f..100f,
            steps = 17,
            onValueChange = { newValue ->
                onUpdateParameter { it.copy(minFeaturesForValidation = newValue.toInt()) }
            },
            isEnabled = isEnabled,
            formatValue = { "%.0f".format(it) },
            description = "Numero minimo di features per considerare un'immagine valida"
        )

        // Ideal Features for Validation
        SliderParameter(
            label = "Features Ideali Validazione",
            value = settings.idealFeaturesForValidation.toFloat(),
            valueRange = 50f..500f,
            steps = 89,
            onValueChange = { newValue ->
                onUpdateParameter { it.copy(idealFeaturesForValidation = newValue.toInt()) }
            },
            isEnabled = isEnabled,
            formatValue = { "%.0f".format(it) },
            description = "Numero ideale di features per qualità ottimale"
        )
    }
}

@Composable
private fun TestSection(
    uiState: RecognitionSettingsUiState,
    onTestSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Test Impostazioni",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = onTestSettings,
                enabled = !uiState.isTestingSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isTestingSettings) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Analizza Database")
            }

            // Risultati test
            uiState.testResult?.let { result ->
                HorizontalDivider()

                Text(
                    "Risultati Analisi Database",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                val stats = result.databaseStats

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem("Immagini totali", stats.totalImages.toString())
                    StatItem("Immagini valide", stats.validImages.toString())
                    StatItem("Corrotte", stats.corruptedImages.toString())
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem("Qualità media", "%.1f%%".format(stats.averageQuality * 100))
                    StatItem("Features medie", stats.averageFeatures.toString())
                }

                if (stats.issues.isNotEmpty()) {
                    Text(
                        "Problemi rilevati:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    stats.issues.take(3).forEach { issue ->
                        Text(
                            "• $issue",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParameterSection(
    title: String,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            description?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            content()
        }
    }
}

@Composable
private fun SliderParameter(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    isEnabled: Boolean,
    formatValue: (Float) -> String,
    description: String? = null
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                formatValue(value),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            enabled = isEnabled
        )

        description?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SuccessCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ValidationErrorsCard(errors: List<String>) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    "Errori Validazione",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            errors.forEach { error ->
                Text(
                    "• $error",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}