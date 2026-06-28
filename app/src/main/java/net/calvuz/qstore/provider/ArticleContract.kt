@file:Suppress("HardCodedStringLiteral", "unused")
package net.calvuz.qstore.provider

import android.net.Uri
import androidx.core.net.toUri

/**
 * Costanti IPC condivise tra QuickStore e QReport.
 * Fonte di verità per il contratto ContentProvider + Picker.
 */
object ArticleContract {

    const val AUTHORITY       = "net.calvuz.qstore.provider"
    const val PERMISSION_READ = "net.calvuz.qstore.permission.READ_ARTICLES"
    const val ACTION_PICK     = "net.calvuz.qstore.action.PICK_ARTICLES"

    val ARTICLES_URI: Uri = "content://$AUTHORITY/articles".toUri()

    object Articles {
        const val UUID            = "uuid"
        const val NAME            = "name"
        const val DESCRIPTION     = "description"
        const val CATEGORY_ID     = "category_id"
        const val UNIT_OF_MEASURE = "unit_of_measure"
        const val CODE_OEM        = "code_oem"
        const val CODE_ERP        = "code_erp"
        const val CODE_BM         = "code_bm"
        const val NOTES           = "notes"
        const val UPDATED_AT      = "updated_at"
    }

    object PickerExtras {
        /** Inviato a QuickStore: UUID già presenti nel checkup (pre-spuntati nel picker). */
        const val PRESELECTED_UUIDS = "preselected_uuids"

        /** Ricevuto da QuickStore: ArrayList<String> degli UUID selezionati dall'utente. */
        const val SELECTED_UUIDS    = "selected_uuids"
    }
}
