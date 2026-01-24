package net.calvuz.qstore.export.presentation.ui.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.calvuz.qstore.export.domain.model.ExportFormat
import net.calvuz.qstore.export.domain.model.ExportOptions
import net.calvuz.qstore.export.domain.model.ExportResult
import net.calvuz.qstore.export.domain.usecase.export.ExportInventoryUseCase
import javax.inject.Inject

data class ExportUiState(
    val selectedFormat: ExportFormat = ExportFormat.CSV,
    val includePhotos: Boolean = false,
    val isExporting: Boolean = false,
    val result: ExportResult? = null
)

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val exportInventoryUseCase: ExportInventoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    fun setFormat(format: ExportFormat) {
        _uiState.update { it.copy(selectedFormat = format) }
    }

    fun setIncludePhotos(include: Boolean) {
        _uiState.update { it.copy(includePhotos = include) }
    }

    fun export() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, result = null) }

            val options = ExportOptions(
                format = _uiState.value.selectedFormat,
                includePhotos = _uiState.value.includePhotos
            )

            val result = exportInventoryUseCase(options)

            _uiState.update { it.copy(isExporting = false, result = result) }
        }
    }

    fun clearResult() {
        _uiState.update { it.copy(result = null) }
    }
}
