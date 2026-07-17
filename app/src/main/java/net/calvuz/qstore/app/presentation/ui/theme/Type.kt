package net.calvuz.qstore.app.presentation.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import net.calvuz.qstore.R

// "Opzione 3 — Arancio e Technical Design" (vedi /design/mockup-orange-technical.html).
// Big Shoulders Display (provato per display/headline) è stato scartato dopo verifica visiva:
// troppo condensato/stretto per restare leggibile nei titoli reali (codici articolo, storico
// movimenti). Roboto di sistema come ripiego per display/headline è stato a sua volta scartato:
// abbinato a Inter sul corpo, il salto tra le due famiglie (x-height/proporzioni diverse) si
// sentiva. Display/headline sono ora su Space Grotesk, grottesco geometrico pensato apposta
// per fare da titolo sopra un corpo Inter (pairing standard tra le due famiglie), coerente con
// l'estetica "nameplate industriale" del sistema.
//
// Title/body/label su Inter invece di IBM Plex Sans (allineato a QReport, stessa direzione
// di design — vedi ../../QuickReport/design/design-system.md): Plex Sans era bundlato come
// font variabile ma caricato senza istanziare l'asse "wght" per i pesi richiesti, quindi
// renderizzava tutto al peso di default, troppo sottile alle dimensioni piccole. Inter è
// bundlato qui come due pesi statici reali (Regular 400, SemiBold 600, licenza SIL OFL) — nessun
// peso Bold/ExtraBold reale disponibile, quindi i ruoli sotto non richiedono mai più di SemiBold
// su questo font (richiederlo produrrebbe un fake-bold sintetico invece di una faccia vera).
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_semibold, FontWeight.SemiBold)
)

// Bundlato come tre pesi statici reali (Regular 400, SemiBold 600, Bold 700, licenza SIL OFL) —
// Space Grotesk non ha un peso ExtraBold/Black statico, quindi display/headline restano al
// massimo su Bold (vedi sotto: displayLarge/Medium usavano FontWeight.ExtraBold su Roboto, qui
// scendono a Bold per lo stesso motivo per cui Inter non supera SemiBold sopra).
val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.space_grotesk_bold, FontWeight.Bold)
)

// Nessuno slot "mono" nativo in Typography M3: esposto a parte, usato esplicitamente nei
// punti che mostrano codici articolo/quantità/timestamp (es. ArticleCard, MovementCard).
val PlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium)
)

/**
 * Tipografia Material 3 per QuickStore
 */
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
