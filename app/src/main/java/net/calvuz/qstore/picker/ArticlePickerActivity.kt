package net.calvuz.qstore.picker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import net.calvuz.qstore.app.presentation.ui.theme.QuickStoreTheme
import net.calvuz.qstore.provider.ArticleContract

/**
 * Activity che espone il selettore articoli a QReport.
 *
 * Lancio da QReport:
 *   Intent(ArticleContract.ACTION_PICK).apply {
 *       setPackage("net.calvuz.quickstore")
 *       putStringArrayListExtra(PRESELECTED_UUIDS, ArrayList(existingUuids))
 *   }
 *
 * Risultato (RESULT_OK):
 *   data.getStringArrayListExtra(SELECTED_UUIDS) → ArrayList<String> di UUID scelti
 */
@AndroidEntryPoint
class ArticlePickerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            QuickStoreTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ArticlePickerScreen(
                        onConfirm = { selectedUuids ->
                            val result = Intent().apply {
                                putStringArrayListExtra(
                                    ArticleContract.PickerExtras.SELECTED_UUIDS,
                                    ArrayList(selectedUuids)
                                )
                            }
                            setResult(RESULT_OK, result)
                            finish()
                        },
                        onCancel = {
                            setResult(RESULT_CANCELED)
                            finish()
                        }
                    )
                }
            }
        }
    }
}
