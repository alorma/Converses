# Converses · Canals en 5 grups · instruccions de disseny

Els 11 canals passen a **5 grups fixos**: En persona · Telèfon · Xat · Social · Altres. Aquest document recull el disseny de les quatre peces que faltaven (icones, avís, badge «beta», filtres i selectors) i quines pantalles canvien. Les captures són a `converses-canals-5-grups.html`.

## 1. Icones dels grups

Material Symbols Rounded, contorn (no farcit). 24 dp a les files grans, 18 dp dins de xips i pastilles.

| Grup | Icona | Notes |
|---|---|---|
| En persona | `group` | Es manté |
| Telèfon | `call` | Es manté |
| Xat | `chat` | Nova |
| Social | `public` | Nova |
| Altres | `more_horiz` | Nova. Contenidor genèric: els futurs sub-canals Pro (feature Pro) penjaran d'aquí sense canviar d'icona |

Les tres noves tenen silueta pròpia a 18 dp (bafarada, globus, tres punts) i no es confonen entre elles ni amb les altres dues. Ordre fix a tot arreu: **En persona, Telèfon, Xat, Social, Altres**.

On es veuen amb icona: pastilles de canal del detall (44 dp d'alt, radi complet), xips «On» de les entrades, xips de «On hi parles» (36 dp, radi 10, icona 18). El filtre del llistat és **només text** (vegeu 2).

## 2. Filtre de canal del llistat (1b)

- Fila **amb scroll horitzontal** (`LazyRow`), no `FlowRow`: xips de 32 dp, radi 8, separació 8 dp, padding horitzontal 12 dp, `flex: none` (no s'encongeixen).
- **Només text**. Amb icona + text els 5 xips ocuparien uns 510 dp i no caben en els 358 dp d'un mòbil petit; només text són uns 370 dp, així que l'últim pot quedar tallat a la dreta (indica que hi ha més).
- Sense seleccionar: vora `outline-variant` 1 dp, text `on-surface-variant`. Seleccionat: fons `primary-container`, text `on-primary-container`, ✓ de 18 dp a l'esquerra (padding esquerre 8 dp), pes 700.
- Selecció múltiple. Cap seleccionat = tots. No hi ha xip «Tots».
- Etiquetes de la persona al llistat (majúscules, 12 sp): grups separats per « · » en l'ordre fix, p. ex. `EN PERSONA · XAT`.

## 3. «On hi parles» (editor, 3b) i selector «On» (entrades, 2c/3c/3e)

- Editor: targeta `surface-card` (radi 24, padding 14) amb els 5 xips en **`FlowRow`** (separació 8): cap scroll. Amb 5 opcions ocupa dues files netes; no cal simplificar-la més. Xip 36 dp, radi 10, icona 18 (✓ quan és seleccionat). Comptador a la dreta de la capçalera: «Cap» / «1 triat» / «N triats».
- Selector «On» d'una entrada: només els grups de la persona (xips de 36 dp, radi 12, ✓ en seleccionar). Ordre fix. A «Triar persona» ja no hi ha el botó «+ Altre» (els canals propis són Pro i queden fora d'aquesta fase).

## 4. Avís de la primera obertura (1d)

**Component:** banner no bloquejant, primer element del llistat, entre la capçalera i els xips de filtre. Es descarta només amb el botó «Entesos». No es tanca sol (s'ha de veure) i no interromp, com faria un diàleg.

**Aspecte (invers):**
- Contenidor: `primary` (en clar és una targeta fosca; en fosc, clara). Contingut: `on-primary`. Radi 24, padding 16 dp (12 dp a la dreta i a baix), marges laterals 16, separació inferior 14, ombra suau (`shadow-float`, 0 4 14 / 18 %).
- Icona `info` farcida dins un cercle de 40 dp (fons `on-primary`, icona `primary`).
- Títol 16 sp / 700. Cos 14 sp, interlineat 1.4, opacitat 0.82.
- Botó «Entesos»: 40 dp d'alt, radi complet, padding 20 dp, fons `on-primary`, text `primary`, 14 sp / 700. Alineat a la dreta.
- Semàntica: `role = status` (`liveRegion = Polite`).

**Comportament:**
- Només es mostra als usuaris que actualitzen des de la beta i han tingut canals fusionats. No als que instal·len de nou.
- En tocar «Entesos» es desa un flag (p. ex. DataStore) i no torna a sortir. Es descarta amb animació (`AnimatedVisibility`, fade + col·lapse de l'alçada).
- Text genèric, **sense dades de cap persona**: no revela res de les protegides.

**Textos:**

| | Títol | Cos | Botó |
|---|---|---|---|
| ca | Hem simplificat els canals | Ara hi ha cinc grups: En persona, Telèfon, Xat, Social i Altres. Els canals que tenies s'han fusionat dins del grup que els correspon. | Entesos |
| en | We've simplified channels | There are now five groups: In person, Phone, Chat, Social and Other. Your existing channels were merged into the matching group. | Got it |
| es | Hemos simplificado los canales | Ahora hay cinco grupos: En persona, Teléfono, Chat, Social y Otros. Tus canales anteriores se han fusionado en el grupo que les corresponde. | Entendido |

## 5. Badge «beta» (1a, 1b)

- Píndola tonal grafit: contenidor `primary-container`, text `on-primary-container`. Funciona en clar, fosc i amb tema dinàmic perquè només fa servir tokens.
- 22 dp d'alt, radi 8, padding horitzontal 8 dp. Text «BETA» en majúscules, 11 sp, pes 800, `letterSpacing` 0,8 sp.
- Posició: a la dreta del títol «Converses», 8 dp de separació, alineada a la part alta de les lletres (marge superior ≈ 7 dp respecte al títol de 44 sp). `contentDescription`: «Versió beta».
- «beta» no es tradueix.

## 6. Nom del grup nou

**Altres** (ca) · **Other** (en) · **Otros** (es). Confirmat.

Noms dels altres grups: En persona / In person / En persona · Telèfon / Phone / Teléfono · Xat / Chat / Chat · Social / Social / Social.

## 7. Pantalles que canvien al canvas

| Pantalla | Canvi |
|---|---|
| 1a Llistat buit | Badge «beta» |
| 1b Llistat | Badge, 5 xips en scroll row, etiquetes de grups a les persones |
| 1d Llistat amb avís | **Nova.** Banner invers (clar i fosc) |
| 1e Canals en 5 grups | **Nova.** Icones 24/18, clar i fosc, xips en ca/en/es |
| 2a Detall | Pastilles: Xat · En persona; cronologia amb «XAT» |
| 2b / 2d / 2e | Mateixes dades d'exemple amb grups |
| 2c Nova entrada | «On»: Xat ✓ · En persona |
| 3a Nova persona | «On hi parles» amb els 5 grups |
| 3b Editar persona | «On hi parles» amb els 5 grups |
| 3c / 3d Nota ràpida | «On»: En persona · Telèfon · Xat ✓ · Altres |
| 3e Triar persona | «On»: Xat ✓ · En persona (sense «+ Altre») |
| 0a–0c Onboarding | Files d'exemple amb «XAT» |
| P3–P7 | Dades d'exemple amb grups. P7: el filtre vell amb «Tots» passa als 5 xips |

## 8. Fora d'abast d'aquesta entrega

Tauleta, Wear OS i design system (ChannelChips) encara no s'han actualitzat. P8 i P9 (canals propis i valors, aparcats) segueixen amb el model antic.

## 9. Cal validar al dispositiu

- Que el filtre del llistat es llegeix bé amb scroll horitzontal i text gran / castellà.
- Les icones `chat`, `public` i `more_horiz` a 18 dp, en clar i fosc.
- L'avís en tema dinàmic (el contrast `primary` / `on-primary` el garanteix el tema).
