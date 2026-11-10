# Converses · Pantalles grans: tauleta i plegable (10 de novembre de 2026)

Aquest lliurament adapta l'app a tauletes i plegables. La referència visual és `pantalles.html`: obre-la al navegador amb connexió, perquè les icones i la tipografia es carreguen de Google Fonts. Les mides són en dp i els colors fan servir els tokens del tema grafit.

**El mòbil no canvia.** Tot el que hi ha aquí s'aplica només a partir de l'amplada mitjana (600 dp o més).

---

## 1. Base tècnica

- Calcula la mida de la finestra amb `currentWindowAdaptiveInfo()` i fes servir les classes d'amplada `COMPACT`, `MEDIUM` i `EXPANDED`. No ho decideixis per model de dispositiu.
- Fes servir aquestes dependències:
  - `androidx.compose.material3.adaptive:adaptive`
  - `adaptive-layout`
  - `adaptive-navigation`
  - `material3-adaptive-navigation-suite`
- Llistat i detall: `NavigableListDetailPaneScaffold`, amb la clau de la persona com a contingut del panell de detall.
- Navegació principal: `NavigationSuiteScaffold`.
  - Compacta: la barra flotant actual.
  - Mitjana i expandida en tauleta: un rail.
- Plegables: `WindowInfoTracker` i `FoldingFeature`. Vegeu la secció 5.
- Al manifest:
  - `android:resizeableActivity="true"`;
  - cap orientació bloquejada;
  - comprova que funciona en multifinestra i en finestres lliures (ChromeOS i escriptori de tauleta).

## 2. Punts de tall

| Classe | Amplada (dp) | Dispositius | Disposició |
| --- | --- | --- | --- |
| Compacta | < 600 | Mòbil, plegable tancat | Un panell, igual que ara |
| Mitjana | 600–839 | Tauleta vertical, plegable obert | Dos panells. Tauleta: rail + llistat de 300 + detall d'una columna. Plegable: secció 5 |
| Expandida | ≥ 840 | Tauleta horitzontal, escriptori | Rail de 96 + llistat de 380 + detall a dues columnes |

## 3. Tauleta horitzontal (12a)

### Rail (96 dp)
- A dalt hi ha el botó «Nota ràpida»: 56 dp, radi 16, color `pri`, icona `edit_square`. Substitueix el FAB.
- Té 28 dp d'espai fins al primer element.
- Els elements són Persones (`group`), Arxivades (`inventory_2`) i Ajustos (`settings`):
  - Cada element té un indicador de 56×32 i radi 16, amb l'etiqueta de 12 sp a sota.
  - L'element actiu fa servir `pri` / `on-pri` i la icona plena.

### Llistat (380 dp, 24 dp de marge a la dreta)
- La capçalera porta «Converses» en display de 36 sp, el comptador de persones i `more_vert`.
- Els xips de filtre passen a dues línies si no caben (`FlowRow`), en lloc de fer scroll horitzontal.
- Files de 48 dp d'avatar, amb el mateix estil agrupat que al mòbil.
- **Persona oberta:** fons `pri-c`, text `on-pri-c`, radi 24 i nom en pes 800. Porta `aria-current` (a Compose, `selected` a la semàntica).

### Detall
- És un panell amb fons `card-hi`, radi 28 i 16 dp de marge a dalt, a baix i a la dreta.
- **Sense fletxa enrere.**
- **Capçalera:**
  - avatar de 96 dp, nom en display de 32 sp, subtítol i estadístiques;
  - a la dreta, `edit` i el split button «Nota ▾» (48 dp d'alçada). El split button ja no flota quan hi ha dues columnes.
- Pastilles de canal en `card`.
- **Dues columnes:** a l'esquerra, Dades i Pendents (només Pro) en 280 dp; a la dreta, la Cronologia, que ocupa la resta. Hi ha 24 dp de separació.

### Sense cap persona oberta
Es veu amb el selector `seleccio = cap` al tauler 12a.
- El detall mostra una galeta de 96 dp (`s-highest`) amb la icona `forum`, el títol «Tria una persona» i el text «La seva cronologia, les dades i els pendents sortiran aquí.».
- En arrencar no s'obre cap persona automàticament.

## 4. Tauleta vertical (12c)

- La disposició és: rail, llistat de 300 dp i detall d'una sola columna.
- El detall és el del mòbil sense la fletxa enrere.
- L'avatar fa 80 dp, la icona `edit` va a dalt a la dreta i el split button «Nota ▾» flota a baix a la dreta del panell.

## 5. Plegable

### Obert (12d)
- Quan hi ha un `FoldingFeature` vertical (`isSeparating` o una frontissa en postura plana), la divisió entre panells s'alinea amb `bounds` de la frontissa.
- El llistat ocupa la meitat esquerra i el detall, la dreta.
- No hi ha rail: la barra flotant (Persones, Arxivades, Ajustos) i el FAB de nota ràpida van a baix, dins del panell del llistat, com al mòbil.
- No hi pot haver res interactiu a menys de 24 dp de la frontissa.
- Si no hi ha `FoldingFeature`, s'aplica la disposició de la tauleta vertical.

### Plegar i desplegar
- Es conserva la persona oberta, l'scroll de cada panell i el text que s'estigui escrivint.
- **Obert → tancat:** si hi havia una persona oberta, es veu el detall amb la fletxa enrere. Si no, es veu el llistat.
- **Tancat → obert:** dos panells amb la mateixa selecció.
- Fes servir `rememberSaveable` i l'estat del navegador del scaffold. No recreïs els ViewModels.

### Mode taula (12e) · opcional
- S'activa amb una frontissa horitzontal i `state == HALF_OPENED`.
- **Meitat de dalt:** la persona (fletxa enrere, avatar de 72 dp, nom) i la cronologia.
- **Meitat de baix:** una entrada nova amb:
  - el segmentat Qui (Jo / {persona} / Apunt);
  - el selector On;
  - el camp de text amb el botó d'enganxar;
  - el botó «Desa» de 120 dp d'amplada.
- Si es decideix no fer-lo, el plegable mig obert es comporta com quan està obert del tot.

## 6. Diàlegs i fulls

- **A partir de l'amplada mitjana**, aquestes pantalles completes passen a ser **diàlegs de 560 dp** (radi 28, scrim, Esc per tancar):
  - Nova persona i Editar persona;
  - Nota ràpida (12b);
  - Nova tasca;
  - Converses Pro.
- **Nota ràpida:** si hi ha una persona oberta al detall, ja ve triada. L'etiqueta del camp diu «AMB QUI · LA PERSONA OBERTA»; per a aquesta etiqueta n'hi ha prou amb «AMB QUI».
- **Fulls inferiors** («Quan va ser?», Avatar, menús, estadístiques d'ús):
  - en amplada mitjana, es queden com a bottom sheet;
  - en amplada expandida, es mantenen com a bottom sheet amb una amplada màxima de 640 dp, centrat.
- **Ajustos i Pro:** columna centrada d'amplada màxima 640 dp dins de l'àrea de contingut, sense el llistat al costat.

## 7. Altres regles

- **Persones protegides:** si se'n selecciona una al llistat, s'obre la biometria i el detall no es mostra fins que es valida. Si es cancel·la, el detall torna a «Tria una persona».
- **Teclat i ratolí:**
  - Les fletxes amunt i avall es mouen pel llistat i Intro obre la persona.
  - Ctrl+N obre la nota ràpida i Esc tanca els diàlegs.
  - Tots els elements tocables tenen estat de hover.
- **Valoració dins l'app:** quan hi ha dos panells, «tornar al llistat» vol dir canviar de persona. La consulta vàlida compta quan es tria una altra persona després d'haver-ne mirat una, i no hi ha cap navegació enrere.
- **Etiqueta BETA:** no surt enlloc, perquè l'app ja és a producció.

## 8. Textos nous

| ca | en | es |
| --- | --- | --- |
| Tria una persona | Choose a person | Elige una persona |
| La seva cronologia, les dades i els pendents sortiran aquí. | Their timeline, details and to-dos will show up here. | Su cronología, los datos y los pendientes aparecerán aquí. |

## 9. Com provar-ho

- Emuladors:
  - Pixel Tablet, en vertical i en horitzontal;
  - Pixel Fold o 9 Pro Fold, obert, tancat i en mode taula;
  - Resizable Emulator.
- Fes previews de Compose amb `@PreviewScreenSizes` per a cada pantalla principal.
- Prova que en plegar i desplegar amb text escrit a la nota ràpida no es perd res.
