package net.calvuz.qstore.settings.domain.model

/**
 * Stili di visualizzazione per le card degli articoli nella lista.
 *
 * Ogni stile offre un diverso livello di dettaglio e densità di informazioni.
 */
enum class ArticleCardStyle {
    /**
     * Visualizzazione completa con immagine grande e tutti i dettagli.
     * - Immagine: 80dp
     * - Info: nome, categoria, codici OEM/ERP, descrizione (2 righe)
     * - Azioni: delete, freccia dettagli
     */
    FULL,

    /**
     * Visualizzazione bilanciata con immagine media e info essenziali.
     * - Immagine: 56dp
     * - Info: nome, categoria, codici OEM/ERP
     * - Azioni: delete, freccia dettagli
     *
     * Questo è lo stile di default.
     */
    COMPACT,

    /**
     * Visualizzazione minima per liste dense.
     * - Immagine: 40dp
     * - Info: solo nome
     * - Azioni: freccia dettagli (delete su swipe o long-press)
     */
    MINIMAL;

    companion object {
        val DEFAULT = COMPACT

        /**
         * Ottiene lo stile dal nome, con fallback al default.
         */
        fun fromName(name: String?): ArticleCardStyle {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: DEFAULT
        }
    }
}

/**
 * Extension per ottenere il nome localizzato dello stile.
 */
fun ArticleCardStyle.getDisplayName(): String = when (this) {
    ArticleCardStyle.FULL -> "Completo"
    ArticleCardStyle.COMPACT -> "Compatto"
    ArticleCardStyle.MINIMAL -> "Minimo"
}

/**
 * Extension per ottenere la descrizione dello stile.
 */
fun ArticleCardStyle.getDescription(): String = when (this) {
    ArticleCardStyle.FULL -> "Immagine grande, tutti i dettagli"
    ArticleCardStyle.COMPACT -> "Immagine media, info essenziali"
    ArticleCardStyle.MINIMAL -> "Lista densa, solo nome"
}
