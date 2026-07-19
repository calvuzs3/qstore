# QuickStore — Design System

Due direzioni visive alternative, stessi contenuti reali (parti ABB, magazzini
GN368SE/Sede/Magazzino principale), stesso impianto tecnico (token, componenti,
spacing) — cambia la pelle. Scegline una come base per l'implementazione
Android, l'altra resta di riserva.

## Opzione 3 — Arancio e Technical Design

Anteprima: `design/mockup-orange-technical.html` · Token: `design/tokens-orange-technical.json`

Riprende la struttura/tipografia dell'opzione 2 (targhetta, mire d'angolo,
Plex Sans/Plex Mono) ma **ricolorata con solo due colori** — arancio `#E88706`
e graphite `#333333`, nient'altro. Niente teal/verde/rosso come accento (il
rosso resta solo per le azioni distruttive, vedi sotto). **Questa è l'opzione
attualmente implementata nell'app** (vedi `Theme.kt`/`Type.kt`/`Color.kt`).

**Aggiornamento — regola d'inchiostro e font Inter** (allineato a QReport, che ha
adattato questa stessa direzione — vedi `../../QuickReport/design/design-system.md`,
sezione "La regola d'inchiostro"): l'arancio come colore di testo/icona diretto su
pagina chiara (eyebrow di sezione, icone di scorciatoia, valori numerici in
evidenza, badge "on container") è stato sostituito da grafite —
`MaterialTheme.colorScheme.accentInk`/`accentInkAlt` in `Theme.kt`, che restano
arancio solo in tema scuro. I riempimenti pieni (pulsanti, chip, badge, mire
d'angolo) non sono toccati, restano arancio in entrambi i temi — eccezione
deliberata mantenuta per `onPrimary`/`onTertiary` in chiaro (restano bianco sui due
ruoli a massima enfasi, stesso compromesso scelto per QReport). `TextButton`/
`OutlinedButton` passano dal default M3 (`primary`) a `onSurface` tramite i wrapper
`QsTextButton`/`QsOutlinedButton` (`app/presentation/ui/common/QsButtons.kt`),
agganciati ovunque via import alias, zero modifiche ai call site — stesso pattern di
`QrButtons.kt` in QReport. **Title/body/label ora su Inter** (Regular 400, SemiBold
600, licenza SIL OFL) al posto di IBM Plex Sans, per lo stesso motivo già isolato in
QReport: Plex Sans era bundlato come variabile ma senza istanziare l'asse "wght",
quindi renderizzava tutto al peso di default. **Display/headline ora su Space
Grotesk** (Regular 400, SemiBold 600, Bold 700, licenza SIL OFL, istanziati come
statici dal variabile in `google/fonts` via `fonttools`/`woff2_decompress` — nessuna
build statica ufficiale disponibile): il font di sistema (Roboto) provato come
ripiego dopo lo scarto di Big Shoulders Display (troppo condensato) non si abbinava
bene a Inter sul corpo — proporzioni/x-height diverse, il salto si sentiva. Space
Grotesk è il pairing standard per Inter, coerente con l'estetica "nameplate
industriale". Nessun peso ExtraBold statico disponibile per questo font, quindi
`displayLarge`/`displayMedium` scendono da `ExtraBold` a `Bold` (vedi `Type.kt`).

**Rinfrescata 2026-07 — Archivo e JetBrains Mono al posto di Space Grotesk e IBM
Plex Mono.** Richiesta esplicita dell'utente ("non mi piacciono"): Space Grotesk e
Plex Mono sono tra le scelte più ricorrenti nel design generato via AI, poco
caratterizzate per uno strumento professionale. Confronto fatto con un artifact di
anteprima (token, componenti Home/Articoli con dati reali, toggle chiaro/scuro) più
un secondo giro con un selettore dal vivo per confrontare due alternative per ruolo
in tutta la pagina prima di scegliere. Palette e Inter per title/body/label
invariati — cambia solo il pairing display/dati.
- **Display/headline → Archivo** (Regular 400, SemiBold 600, Bold 700, licenza SIL
  OFL, stesso percorso `fonttools`-free già usato per Space Grotesk — vedi memoria
  di progetto "static Google Fonts"): grottesco robusto, tiene meglio il peso alle
  taglie grandi (numeri statistica) senza il tono trendy-SaaS di Space Grotesk.
  Alternativa scartata: Barlow Semi Condensed (nato per segnaletica stradale/
  industriale, coerente con la metafora "targhetta" ma meno leggibile su titoli
  lunghi per via della condensazione).
- **Dati (codici/quantità/timestamp) → JetBrains Mono** (Regular 400, Medium 500,
  licenza SIL OFL, via `@fontsource/jetbrains-mono`): pensato per leggibilità di
  codici a taglie piccole, pairing consolidato con Inter. Alternativa scartata:
  Roboto Mono (più neutro/di sistema, meno carattere).

Con un solo accento disponibile, la severità (normale / attenzione / critico)
si legge per **intensità dell'arancio** invece che per tinta diversa: striscia
grigia neutra = normale, striscia arancio piena = attenzione (sotto scorta),
badge arancio pieno con testo = critico (esaurito). Coerente col soggetto
(strumenti di misura/allarme spesso usano un solo colore di segnale a
intensità variabile, non un semaforo a tre colori).

Spaziatura interna dei componenti (card, liste, bottoni, chip, campi) ridotta
di un gradino rispetto alle altre due opzioni (xs/sm invece di sm/md/lg) — su
richiesta, per vedere l'effetto di una UI più densa. Titoli e ritmo tra
sezioni **non toccati**, restano su lg/xl/xxl come nell'opzione 2.

## Opzione 2 — Nameplate industriale (di riserva)

Anteprima: `design/mockup.html` · Token: `design/tokens.json`

## Perché questa direzione (opzione 2)

QuickStore gestisce ricambi elettromeccanici reali (controller ABB, servomotori,
cuscinetti, cavi) in più magazzini, usato da tecnici da telefono spesso a mani
impegnate. Non è un catalogo consumer: è uno strumento professionale. Il sistema
tratta ogni riga di magazzino come la **targhetta identificativa di uno strumento**
(nameplate) invece che come una card e-commerce: codice in monospazio, stato
leggibile a colpo d'occhio via striscia colorata, piccole mire d'angolo che
richiamano i segni di registrazione dei disegni tecnici.

Deliberatamente evitato: il blu/viola generico da SaaS, il crema+terracotta e il
nero+verde acido che sono i default con cui capita qualunque design generato senza
una direzione — nessuno dei due avrebbe detto qualcosa di specifico su *questo*
prodotto.

## Palette

| Nome | Hex | Ruolo |
|---|---|---|
| Graphite | `#12181F` | Testo primario (light) / sfondo (dark) |
| Paper | `#F3F5F6` | Sfondo (light), neutro freddo non crema |
| Signal Teal | `#0E6E66` | Brand, azione primaria |
| Calibration Amber | `#E98A2E` | Semantico: sotto scorta / attenzione |
| Circuit Green | `#3A8F5C` | Semantico: carico / disponibile |
| Alert Red | `#C4432E` | Semantico: esaurito / scarico / errore |

Regola: ambra/verde/rosso si usano come **sfondo di chip, striscia laterale o
icona** — mai come colore di testo corrente su sfondo chiaro (contrasto
insufficiente). Il teal è l'unico colore usato anche come testo/bordo interattivo.

Dark theme: stessi ruoli, luminosità alzata per contrasto su sfondo scuro (vedi
`tokens.json`, set `dark`). Non è un'inversione automatica — i valori sono scelti
a mano per restare leggibili.

## Tipografia

| Ruolo | Font | Pesi | Uso |
|---|---|---|---|
| Display | **Big Shoulders Display** | 700/800 | Titoli schermata, numeri statistica grandi. Con moderazione. |
| Corpo | **IBM Plex Sans** | 400/500/600 | Tutto il testo UI |
| Dati | **IBM Plex Mono** | 400/500 | Codici articolo, quantità, timestamp — allineamento a colonna dei numeri |

Tutti e tre gratuiti su Google Fonts, disponibili nativamente nel menu font di
Figma (nessun upload). L'anteprima HTML usa font di sistema per compatibilità
dell'artifact — vedi nota in `mockup.html`.

## Spaziatura

Riusa esattamente la scala già presente in `app/presentation/ui/theme/Spacing.kt`:
`xs=4 · sm=8 · md=12 · lg=16 · xl=24 · xxl=32`. Nessuna nuova unità introdotta.

## Componenti — mappatura sul codice esistente

| Componente design | File Kotlin corrispondente |
|---|---|
| Nameplate / ListItemCard | `presentation/ui/common/ListItemCard.kt` |
| Empty state | `presentation/ui/common/EmptyState.kt` |
| Error state | `presentation/ui/common/ErrorState.kt` |
| Stat readout (3-up) | `HomeScreen.kt`, blocco "Statistiche Magazzino" |
| Chip filtro magazzino | `LocationFilterMenu` in `ArticleListScreen.kt` |
| Segmented Carico/Scarico/Trasferimento | `AddMovementScreen.kt` |

Ogni componente disegnato ha già un equivalente funzionante in Kotlin: questo non
è un redesign da costruire da zero, è una pelle visiva da applicare a componenti
che esistono già e sono già centralizzati (vedi commit "Consolidamento design
system" precedente).

## Motivo ricorrente: mire d'angolo

Piccola "L" da 9×9px in due angoli opposti di card e stat, colore = colore
semantico della riga (teal di default, ambra se sotto scorta, ecc.). Richiama i
segni di registrazione di un disegno tecnico/tavola di calibrazione — coerente col
soggetto (ricambi industriali), non decorativo. Vedi classe `.plate` in
`mockup.html`.

## Come portarlo in Figma

Vale per entrambe le opzioni, cambia solo quale file token/mockup usi come fonte.

1. Installa il plugin gratuito **Tokens Studio for Figma**.
2. Plugin → Import → incolla il contenuto di `design/tokens-orange-technical.json`
   (opzione 3) o `design/tokens.json` (opzione 2).
3. "Create Styles/Variables" per generare Color Styles e Variables da quei token.
4. Applica i font (già nativi in Figma, nessun upload): IBM Plex Sans/IBM Plex
   Mono per l'opzione 3 (il titolo/display usa il Roboto di sistema), IBM Plex
   Sans/Big Shoulders Display/IBM Plex Mono per l'opzione 2.
5. Ricostruisci i componenti della sezione "Componenti" del mockup scelto come
   componenti Figma (Auto Layout), usando gli Styles appena creati — il mockup
   HTML è la mappa 1:1 di cosa costruire.

## Riuso per la futura web app

Le custom property CSS in cima a ciascun mockup (`:root { --sp-lg: 16px; ... }`)
sono **la stessa fonte di verità** del rispettivo token JSON, stessi nomi, stessi
valori — copiabili direttamente come punto di partenza del CSS della web app,
senza dover tradurre nulla tra Figma e codice. Se la web app userà React/Vue con
un design system a componenti, i blocchi in ".comp-grid"/".comp-card" del mockup
sono già markup+CSS funzionante da cui partire, non solo immagini statiche.

## Deliberatamente fuori scope

- Nessuna modifica al codice Android in questo giro — questo è materiale di
  design, non un altro passo di refactor.
- Non tutte le schermate dell'app sono mockate, solo le tre più rappresentative
  (Home, Articoli, Movimento) — le altre seguono lo stesso linguaggio.
- `dynamicColor` in `Theme.kt` (Material You) va disattivato solo quando si
  inizia l'implementazione Android vera di questa palette, non prima.
