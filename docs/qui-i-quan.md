# Converses — «Qui» i «Quan» a les entrades

Referència visual: llenç «Converses», secció **«Nota ràpida · selector "Quan"»**. Captures a `docs/img/qui-i-quan/`:

| | |
| --- | --- |
| **3c** · Nota ràpida, amb «Quan» com a fila | ![3c](img/qui-i-quan/3c-nota-rapida-quan.png) |
| **3d** · Full «Quan va ser?» | ![3d](img/qui-i-quan/3d-full-quan-va-ser.png) |
| **3e** · Nota ràpida amb el selector «Qui» i el camp de text buit | ![3e](img/qui-i-quan/3e-nota-rapida-qui.png) |
| **2e** · Detall amb la cronologia Jo / persona / Apunts | ![2e](img/qui-i-quan/2e-detall-cronologia.png) |

Es fa sobre el color de la persona (`docs/plan.md` → «Color de la persona»; el `color-persona.md` original ja no existeix): totes aquestes pantalles ja porten el tema de la persona.

Pla de treball i estat: `docs/backlog.md`, punt 9.

## Resum dels canvis

1. El tipus d'entrada deixa de ser «Li he dit · M'ha dit · Ha passat». Ara és **«Qui»: Jo · {persona} · Apunt**.
2. La data i l'hora es trien amb una **fila «Quan»**, que obre un full amb opcions ràpides. Substitueix el xip «Ara».
3. El formulari d'entrada queda amb un **ordre i un estil únics**, tant a la nota ràpida com al full des del detall i a l'edició d'una entrada.
4. La **cronologia, els filtres i els comptadors** del detall fan servir la mateixa terminologia.

**Model de dades: sense canvis.** L'enum `EntryKind` es manté i només canvien els textos, les icones i els marcadors:

| `EntryKind` | Abans | Ara (selector / filtre / etiqueta) | Icona / marcador |
| --- | --- | --- | --- |
| `SAID` | Li he dit | **Jo** / Jo / `JO` | `person` (farcida en seleccionar) |
| `HEARD` | M'ha dit | **{nom}** / {nom} / `{NOM}` | avatar de la persona en petit |
| `HAPPENED` | Ha passat | **Apunt** / Apunts / `APUNT` | `edit_note` |

`{nom}` és el **primer mot del nom** de la persona («Jordi Puig» → «Jordi»). Si és més llarg de 12 caràcters, es talla amb «…».

---

## 1. Formulari d'entrada

S'aplica a tres llocs:

- la nota ràpida (`QuickNoteRoute`);
- el full «Nova entrada» del detall;
- l'edició d'una entrada (Accions d'entrada → Editar).

Si avui són composables diferents, val la pena extreure'n un de comú, `EntryForm`.

### Ordre dels camps

1. **Amb qui:** només a la nota ràpida. El full del detall ja sap la persona.
2. **Qui**
3. **Text**
4. **On**
5. **Quan**

Cada secció porta un títol curt (`Qui`, `On`, `Quan`) en `labelLarge` 14/700, `onSurfaceVariant` i 6 dp de marge lateral. Les seccions van separades 14 dp.

### Qui

- `ButtonGroup` connectat, de 3 opcions a parts iguals, 52 dp d'alt i 2 dp entre botons.
- Seleccionat: pastilla `primary` / `onPrimary`, pes 700, icona farcida. No seleccionat: `surfaceContainerHigh`, pes 500, juntes de 8 dp.
- Contingut de cada botó:
  - **Jo:** icona `person` + «Jo».
  - **{nom}:** l'`Avatar` de la persona a 26 dp (forma, pastel i inicial) + el nom.
  - **Apunt:** icona `edit_note` + «Apunt».
- Per defecte, **Jo**.
- A la nota ràpida sense persona triada, el botó del mig diu «Persona», amb la icona `person_outline`, i està desactivat.

### Text

- Camp sense vora ni indicador: `surfaceContainerHigh`, radi 24, alçada mínima de 120 dp, text de 17 sp.
- El text d'ajuda canvia segons «Qui»:

| Qui | Text d'ajuda |
| --- | --- |
| Jo | Què has dit? |
| {nom} | Què ha dit {nom}? |
| Apunt | Què vols apuntar? |

### On

- Xips de selecció única (`FilterChip`), radi 12 i 36 dp d'alt.
- Seleccionat: `primaryContainer` amb `check`. No seleccionat: vora d'1 dp `outlineVariant`.
- Primer, els canals de la persona; al final, «Altres». Per defecte, el canal de la seva última entrada.

### Quan

- Fila de 72 dp, `surfaceContainerLowest` i radi 24, amb el mateix patró que la targeta «Amb qui»:
  - **cercle de 44 dp** (`surfaceContainerHigh`) amb la icona de l'opció;
  - **títol** de l'opció (17/700);
  - **data i hora completes** a sota (13, `onSurfaceVariant`);
  - `unfold_more` a la dreta.
- Tocar-la obre el full «Quan va ser?».

### Desa

- Desactivat si el text és buit, o si a la nota ràpida no hi ha persona.
- A la nota ràpida va a dalt a la dreta (pastilla). Al full del detall continua com a botó ample a baix («Desa a la cronologia»).

---

## 2. Full «Quan va ser?»

`ModalBottomSheet` amb el títol «Quan va ser?» (`sheet-title` 24/800) i una llista agrupada (24/6, 2 dp entre files) de 64 dp per fila:

| Opció | Icona | Subtítol (exemple) | Valor |
| --- | --- | --- | --- |
| Ara | `schedule` | Dilluns 6 d'oct. · 21:46 | Es resol **en desar** |
| Fa una hora | `history` | Dilluns 6 d'oct. · 20:46 | `now − 1 h`, calculat en triar-la |
| Ahir a aquesta hora | `event_repeat` | Diumenge 5 d'oct. · 21:46 | `now − 1 dia` |
| Tria dia i hora… | `edit_calendar` | Calendari i rellotge | Obre els selectors (vegeu més avall) |

- L'opció activa es pinta en `primaryContainer` amb `check` a la dreta. «Tria dia i hora…» porta `chevron_right`.
- Triar una opció tanca el full i actualitza la fila.
- **«Ara» no és un instant fix:** el valor es pren en prémer Desa, i el subtítol de la fila s'actualitza cada minut. Les altres opcions sí que fixen l'instant en triar-les.
- **Tria dia i hora…:**
  1. `DatePickerDialog` de M3, amb `selectableDates` fins a avui (no es permet el futur).
  2. Després, `TimePicker` dins d'un diàleg, amb l'hora actual per defecte en 24 h.
  3. En acceptar, la fila mostra la data com a títol («Dissabte 4 d'oct.») i l'hora a sota («18:30»), amb la icona `edit_calendar`. Al full, «Tria dia i hora…» queda marcada amb aquest valor com a subtítol.
- **Editar una entrada existent:** la fila mostra la data i l'hora desades, en el format personalitzat de dalt. Cap opció ràpida queda marcada.

Format de data: `EEEE d MMM` amb el locale de l'app («Dilluns 6 d'oct.» / «Lunes 6 oct.» / «Monday 6 Oct.»), primera lletra en majúscula, i hora `HH:mm`.

---

## 3. Detall i cronologia

### Comptadors de la capçalera

`Jo · {nom} · Apunts`, en lloc de `Li he dit · M'ha dit · Converses`. Ara el tercer número compta només els apunts.

### Filtres de la cronologia

- Tres `FilterChip` de multiselecció (tots actius per defecte):
  - **Jo**, amb `person`;
  - **{nom}**, amb l'avatar de la persona a 22 dp;
  - **Apunts**, amb `edit_note`.
- Si un filtre no té cap entrada, s'amaga.

### Marcadors (32 dp, sobre la línia `outlineVariant`)

| Tipus | Marcador |
| --- | --- |
| Jo | Cercle `primary` amb `person` farcida en `onPrimary` |
| {nom} | **L'avatar de la persona** (forma, pastel i inicial, 32 dp), sense fons |
| Apunt | Quadrat arrodonit (10) amb vora de 2 dp `outline` i `edit_note` en `onSurface` |

### Etiqueta de cada entrada

`JO · 19:20 · DISCORD`, `JORDI · 19:12 · DISCORD`, `APUNT · 22:00 · EN PERSONA`: sempre **qui · hora · canal**.

### Accions d'entrada (full)

La capçalera fa servir el mateix format i ordre que la cronologia (`JO · 17:06 · WHATSAPP`).

---

## 4. Textos (`strings.xml`)

| Clau proposada | ca | es | en |
| --- | --- | --- | --- |
| `entry_who_title` | Qui | Quién | Who |
| `entry_who_me` | Jo | Yo | Me |
| `entry_who_note` | Apunt | Apunte | Note |
| `entry_who_notes` | Apunts | Apuntes | Notes |
| `entry_hint_me` | Què has dit? | ¿Qué has dicho? | What did you say? |
| `entry_hint_person` | Què ha dit %1$s? | ¿Qué ha dicho %1$s? | What did %1$s say? |
| `entry_hint_note` | Què vols apuntar? | ¿Qué quieres apuntar? | What do you want to note? |
| `entry_where_title` | On | Dónde | Where |
| `entry_when_title` | Quan | Cuándo | When |
| `when_sheet_title` | Quan va ser? | ¿Cuándo fue? | When was it? |
| `when_now` | Ara | Ahora | Now |
| `when_hour_ago` | Fa una hora | Hace una hora | An hour ago |
| `when_yesterday` | Ahir a aquesta hora | Ayer a esta hora | Yesterday, same time |
| `when_pick` | Tria dia i hora… | Elegir día y hora… | Pick date and time… |
| `when_pick_sub` | Calendari i rellotge | Calendario y reloj | Calendar and clock |

Cal esborrar les claus antigues («Li he dit», «M'ha dit», «Ha passat», «Converses» del comptador) quan ja no es facin servir. Revisa també el **text d'exportar notes**: ha de fer servir les etiquetes noves (`Jo:`, `{Nom}:`, `Apunt:`).

## 5. Accessibilitat

- Botons de «Qui»: `role = RadioButton`, i el `contentDescription` del botó de la persona és el nom complet.
- Marcador d'avatar a la cronologia: decoratiu. L'entrada es llegeix sencera: «Jordi, avui a les 19:12, per Discord: …».
- Fila «Quan»: «Quan: Ara, dilluns 6 d'octubre a les 21:46. Toca per canviar-ho».

## Proves

- [ ] Nota ràpida, full del detall i edició mostren el mateix formulari en el mateix ordre.
- [ ] El text d'ajuda canvia en canviar «Qui», i el nom de la persona és correcte (primer mot, tallat si és llarg).
- [ ] Sense persona triada a la nota ràpida: el botó del mig està desactivat i Desa també.
- [ ] «Ara» desa l'hora del moment de desar, no la d'obrir el formulari.
- [ ] «Tria dia i hora…» no deixa triar dies futurs, i el valor triat es manté si es torna a obrir el full.
- [ ] Editar una entrada antiga mostra la seva data i, en desar, no la canvia si no s'ha tocat.
- [ ] Cronologia: marcadors, etiquetes, filtres i comptadors amb la terminologia nova, en clar i en fosc, i amb dues o tres persones de colors diferents.
- [ ] Exportar notes fa servir les etiquetes noves.
- [ ] Previews de Compose: `EntryForm` (tres valors de «Qui» × text buit o ple), `WhenSheet`, `TimelineRow` (tres tipus).
