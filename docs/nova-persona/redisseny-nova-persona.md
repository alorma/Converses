# Redisseny de Nova persona / Editar persona: capçalera amb l'avatar

Referència visual: llenç «Converses», secció **«Avatar a Nova persona»**:

- **4c** · versió gratuïta;
- **4e** · Pro, formulari;
- **4f** · Pro, full «Avatar».

Captures: `docs/nova-persona/img/`:

- `avatar-gratuit.png`
- `avatar-pro.png`
- `avatar-pro-full.png`

Es fa servir a Nova persona i a Editar persona. Context de què és gratuït i què és Pro: `docs/pro.md`.

## Problema

Sense el selector de forma i color (versió gratuïta), la targeta d'avatar queda gran i buida, amb només l'avatar a dins.

## Solució

S'**elimina la targeta d'avatar**. L'avatar passa a la **primera fila del grup «Nom»**, a l'esquerra del camp.

---

## 1. Versió gratuïta (4c)

### Layout (ordre complet del formulari)

1. **Omple des d'un contacte** (només a Nova persona): fila de 64 dp en `surfaceContainerHigh`, radi 24, amb un cercle de 40 dp (`surfaceContainerLowest`) i `person_add`, el títol «Omple des d'un contacte», el subtítol «Nom i telèfon de l'agenda» i `chevron_right`. Obre el selector de contactes del sistema (vegeu `docs/pro.md`).
2. **Nom + Àlies**, amb l'avatar (detall a sota).
3. **Dades**, com ara (abans «Dades fixades»; vegeu la secció 3).
4. **On hi parles**, com ara.
5. **Persona protegida**, com ara.
6. **Esborrar persona**, només a Editar persona, com ara.

Separació de 14 dp entre blocs.

### Nom + Àlies

- Grup connectat Nom + Àlies (radis 24/6, 2 dp entre files, `surfaceContainerLowest`).
- **Fila 1:**
  - padding de 12 dp a dalt, a baix i a l'esquerra, i de 16 dp a la dreta;
  - avatar de 56 dp;
  - 14 dp de separació;
  - columna amb l'etiqueta «Nom» (12/700, `onSurfaceVariant`) i el camp (17 sp).
- **Fila 2:** Àlies, igual que ara.
- A sota continua la secció «Dades», com fins ara.

### Avatar estàndard

- **Forma:** sempre **Galeta** (`MaterialShapes.Cookie9Sided`).
- **Color:** automàtic, un dels 7 pastels.
  - `AvatarColor.forName(name)` el tria amb un hash estable del nom normalitzat (minúscules, sense accents ni espais als extrems).
  - El mateix nom dona sempre el mateix color. Si es reanomena la persona, el color pot canviar.
- **Inicials:**
  - primera lletra del primer mot + primera lletra de l'últim mot (si n'hi ha més d'un), en majúscules;
  - inicials en `#1E1D24`, 20 sp / 800.
- Les inicials i el color s'actualitzen **mentre s'escriu** el nom, amb una transició de color de 300 ms.
- Amb el nom buit, l'avatar és `surfaceContainerHigh` amb «?».
- L'avatar no és clicable. No hi ha cap reclam del Pro.

### Model de dades

- L'app encara no està publicada, així que no cal migració.
- `avatarShape` i `avatarColor` són **nul·lables**: `null` vol dir «estàndard» (Galeta + color automàtic).
- A la versió gratuïta sempre es desen amb `null`.
- Una sola funció resol l'avatar a tot arreu (llistat, detall, selector de persona, formulari): `resolveAvatar(person) → (shape, color)`.

## 2. Versió Pro (4e i 4f)

Per quan existeixi el Pro, darrere de `FeatureGate.isPro`.

### Formulari (4e)

- **Mateix layout** que la versió gratuïta, però l'avatar és un botó.
- **Insígnia:** 24 dp a baix a la dreta (desplaçada −4 dp), cercle `primary` amb `edit` de 13 dp en `onPrimary` i vora de 2 dp del color de la targeta.
- `contentDescription`: «Canvia la forma i el color de l'avatar».
- En tocar-lo s'obre el full «Avatar».

### Full «Avatar» (4f)

`ModalBottomSheet`, amb padding de 10/20/28 dp i 18 dp entre seccions:

- **Capçalera:**
  - previsualització de 88 dp (forma i color triats, inicials de 30 sp);
  - títol «Avatar» (24/800);
  - resum en 13 sp `onSurfaceVariant`: «Trèvol · Cel», o «Galeta · color automàtic (Cel)».
- **Forma:**
  - títol de secció 14/700;
  - `ButtonGroup` connectat de 4 opcions a parts iguals, de 72 dp d'alt: Galeta, Trèvol, Flor i Sol;
  - cada opció porta la forma a 34 dp i el nom a sota (12 sp);
  - la seleccionada va en `primaryContainer` (forma en `onPrimaryContainer`, text 700), amb radi 20; les altres, `surfaceContainerLowest` amb la forma en `outline`, juntes de 8 dp i extrems de 20 dp.
- **Color:**
  - 8 mostres de 30 dp dins d'anells de 40 dp, repartides a tot l'ample;
  - la primera és **Automàtic**: icona `autorenew` sobre `surfaceContainerHigh`. Torna al color calculat pel nom;
  - després, els 7 pastels;
  - la seleccionada té anell de 2 dp `primary` i `check` de 18 dp en `#1E1D24`. Automàtic no porta `check`: només l'anell.
- **Fet:** pastilla `primary` de 56 dp a tot l'ample. Tanca el full.
- **Comportament:**
  - els canvis es veuen en directe a la previsualització i a la fila del formulari;
  - tancar el full lliscant també els manté;
  - es desen quan es desa la persona.
- **Dades:**
  - forma triada → `avatarShape`;
  - color triat → `avatarColor`;
  - «Automàtic» → `avatarColor = null`.

## 3. «Dades fixades» passa a ser «Dades» (a tota l'app)

Les dades sempre surten a dalt del perfil, així que no cal dir que estan «fixades».

- **Títol de la secció:**
  - ca: «Dades fixades» → **«Dades»**;
  - es: → **«Datos»**;
  - en: «Pinned facts» → **«Info»**.
- **Icona:** es treu la `push_pin` del títol, al detall i a l'editor, perquè quedi com la resta de títols de secció («On hi parles», etc.).
- **Subtítol:** es treu «Surten a dalt del detall» de l'editor.
- **On canvia:**
  - detall;
  - Editar persona i Nova persona;
  - full d'accions d'entrada i menú del split button (que mostren el detall de fons);
  - descripció del diàleg d'esborrar;
  - exportar notes: capçalera de la secció a l'exportació de text.
- «Afegeix una dada» es manté.
- **Codi:** és només un canvi de textos de la UI. La taula `pinned_fact` i els noms interns del codi es poden quedar igual.

## 4. Textos (`strings.xml`)

| Clau | ca | es | en |
| --- | --- | --- | --- |
| `avatar_edit_cd` | Canvia la forma i el color de l'avatar | Cambia la forma y el color del avatar | Change avatar shape and colour |
| `avatar_sheet_title` | Avatar | Avatar | Avatar |
| `avatar_shape` | Forma | Forma | Shape |
| `avatar_color` | Color | Color | Colour |
| `avatar_color_auto` | Automàtic | Automático | Automatic |
| `avatar_summary_auto` | %1$s · color automàtic (%2$s) | %1$s · color automático (%2$s) | %1$s · automatic colour (%2$s) |
| `avatar_done` | Fet | Hecho | Done |
| `facts_title` (substitueix el de «Dades fixades») | Dades | Datos | Info |
| `shape_cookie` · `shape_clover` · `shape_flower` · `shape_sunny` | Galeta · Trèvol · Flor · Sol | Galleta · Trébol · Flor · Sol | Cookie · Clover · Flower · Sun |

## Proves

- [ ] **Gratuïta:** no hi ha targeta d'avatar; l'avatar és a la fila del nom i no es pot tocar.
- [ ] **Gratuïta:** totes les persones són Galeta i el mateix nom dona sempre el mateix color.
- [ ] **Gratuïta:** amb 20 noms diferents, els 7 colors surten repartits.
- [ ] **Gratuïta:** les inicials i el color canvien mentre s'escriu; amb el nom buit, «?».
- [ ] **Pro** (`isPro = true` en debug): la insígnia obre el full, els canvis es veuen en directe i es desen amb la persona.
- [ ] **Pro:** «Automàtic» torna al color del nom i desa `avatarColor = null`.
- [ ] Previews: fila del nom (nom buit i ple), full «Avatar» (Automàtic i amb color triat), en clar i en fosc.
