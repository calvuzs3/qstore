package net.calvuz.qstore.settings.domain.model

/**
 * Impostazioni di visualizzazione dell'applicazione.
 *
 * Questo model racchiude tutte le preferenze relative all'aspetto
 * e al layout dell'interfaccia utente.
 *
 * Le impostazioni sono immutabili: usa copy() per creare varianti.
 */
data class DisplaySettings(
    /**
     * Stile di visualizzazione delle card articoli nella lista.
     */
    val articleCardStyle: ArticleCardStyle = ArticleCardStyle.DEFAULT,

    /**
     * Mostra indicatori di stock (icone colorate) nelle card.
     * Verde = disponibile, Giallo = sotto soglia, Rosso = esaurito
     */
    val showStockIndicators: Boolean = true,

    /**
     * Mostra l'immagine thumbnail nelle card articoli.
     * Se false, mostra solo l'icona placeholder.
     */
    val showArticleImages: Boolean = true,

    /**
     * Numero di colonne nella griglia articoli (se si usa grid layout).
     * Valori supportati: 1 (lista), 2, 3
     * Nota: per ora usiamo sempre lista, questo è per future implementazioni.
     */
    val gridColumns: Int = 1

    // === Future espansioni ===
    // val themeMode: ThemeMode = ThemeMode.SYSTEM,
    // val accentColor: AccentColor = AccentColor.DEFAULT,
    // val fontSize: FontSize = FontSize.MEDIUM,
    // val animationsEnabled: Boolean = true,
) {
    companion object {
        /**
         * Impostazioni di default.
         */
        fun getDefault() = DisplaySettings()

        /**
         * Validazione delle impostazioni.
         * @return Lista di errori (vuota se valido)
         */
        fun DisplaySettings.validate(): List<String> {
            val errors = mutableListOf<String>()

            if (gridColumns !in 1..3) {
                errors.add("Numero colonne deve essere tra 1 e 3")
            }

            return errors
        }
    }
}
