package net.calvuz.qstore.app.presentation.ui.theme

import androidx.compose.ui.graphics.Color

// "Opzione 3 — Arancio e Technical Design" (vedi /design/mockup-orange-technical.html)
// Due soli hue: arancio (brand/attenzione) e graphite (testo). Il resto è scala di grigi
// neutra. `error` resta un rosso vero e proprio, eccezione deliberata: le azioni distruttive
// (elimina magazzino/categoria) non devono confondersi con "arancio = attenzione scorta".
//
// Regola d'inchiostro (allineata a QReport, stessa direzione di design — vedi
// ../../QuickReport/design/design-system.md, sezione "La regola d'inchiostro"): l'arancio come
// inchiostro (testo/icona) è ammesso solo su sfondo grafite (tema scuro); il grafite come
// inchiostro è ammesso su bianco o sopra un riempimento arancione pieno. I riempimenti pieni
// (pulsanti, chip, badge, mire d'angolo) restano arancio invariati in entrambi i temi — la
// regola vincola solo il colore del testo/icona sopra quei riempimenti, non i riempimenti
// stessi. Eccezione deliberata, verificata su device: `onPrimary`/`onTertiary` in chiaro
// restano bianco (il grafite appesantiva troppo i due pulsanti a massima enfasi) — stesso
// compromesso già scelto per QReport.

// Light Theme Colors
val md_theme_light_primary = Color(0xFFE88706) // Arancio brand
val md_theme_light_onPrimary = Color(0xFFFFFFFF) // Eccezione deliberata, vedi sopra — non grafite
val md_theme_light_primaryContainer = Color(0xFFFCE3C4)
val md_theme_light_onPrimaryContainer = Color(0xFF333333) // Grafite, non più arancio scuro
val md_theme_light_secondary = Color(0xFF5B5B5B) // Ruolo neutro (graphite mid-tone)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFE7E7E7)
val md_theme_light_onSecondaryContainer = Color(0xFF333333)
val md_theme_light_tertiary = Color(0xFFB96A05) // Sfumatura più scura dell'arancio, non un nuovo hue
val md_theme_light_onTertiary = Color(0xFFFFFFFF) // Eccezione deliberata, vedi sopra — non grafite
val md_theme_light_tertiaryContainer = Color(0xFFF5D9B8)
val md_theme_light_onTertiaryContainer = Color(0xFF333333) // Grafite, non più arancio scuro
val md_theme_light_error = Color(0xFFC4432E) // Rosso vero, solo azioni distruttive
val md_theme_light_errorContainer = Color(0xFFF8D4CD)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_onErrorContainer = Color(0xFF410E06)
val md_theme_light_background = Color(0xFFF4F4F4) // Paper
val md_theme_light_onBackground = Color(0xFF333333) // Graphite
val md_theme_light_surface = Color(0xFFFFFFFF)
val md_theme_light_onSurface = Color(0xFF333333)
val md_theme_light_surfaceVariant = Color(0xFFECECEC)
val md_theme_light_onSurfaceVariant = Color(0xFF565656) // Più scuro del grigio Polytec originale (#707070) per leggibilità
val md_theme_light_outline = Color(0xFFBDBDBD)
val md_theme_light_inverseOnSurface = Color(0xFFF4F4F4)
val md_theme_light_inverseSurface = Color(0xFF333333)
val md_theme_light_inversePrimary = Color(0xFFFFB86B)

// Dark Theme Colors
val md_theme_dark_primary = Color(0xFFF0993A) // Arancio, alzato di luminosità per contrasto
val md_theme_dark_onPrimary = Color(0xFF333333) // Grafite: qui il riempimento arancio è pieno, mai bianco (contrasto insufficiente)
val md_theme_dark_primaryContainer = Color(0xFF5A3D14)
val md_theme_dark_onPrimaryContainer = Color(0xFFF2F2F2) // Testo neutro chiaro, non più arancio tenue
val md_theme_dark_secondary = Color(0xFFB0B0B0)
val md_theme_dark_onSecondary = Color(0xFF262626)
val md_theme_dark_secondaryContainer = Color(0xFF3A3A3A)
val md_theme_dark_onSecondaryContainer = Color(0xFFECECEC)
val md_theme_dark_tertiary = Color(0xFFD68A3E)
val md_theme_dark_onTertiary = Color(0xFF333333) // Grafite, stesso motivo di onPrimary
val md_theme_dark_tertiaryContainer = Color(0xFF4A3218)
val md_theme_dark_onTertiaryContainer = Color(0xFFF2F2F2) // Testo neutro chiaro, non più arancio tenue
val md_theme_dark_error = Color(0xFFE06A50)
val md_theme_dark_errorContainer = Color(0xFF5C2318)
val md_theme_dark_onError = Color(0xFF2A0B04)
val md_theme_dark_onErrorContainer = Color(0xFFF8D4CD)
val md_theme_dark_background = Color(0xFF1B1B1B)
val md_theme_dark_onBackground = Color(0xFFF2F2F2)
val md_theme_dark_surface = Color(0xFF262626)
val md_theme_dark_onSurface = Color(0xFFF2F2F2)
val md_theme_dark_surfaceVariant = Color(0xFF2F2F2F)
val md_theme_dark_onSurfaceVariant = Color(0xFFB8B8B8) // Coerente con l'aumento di contrasto lato light
val md_theme_dark_outline = Color(0xFF6E6E6E)
val md_theme_dark_inverseOnSurface = Color(0xFF333333)
val md_theme_dark_inverseSurface = Color(0xFFF2F2F2)
val md_theme_dark_inversePrimary = Color(0xFFB96A05)
