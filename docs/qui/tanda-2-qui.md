# Tanda 2 · Selector «Qui» i cronologia

Referència visual: llenç «Converses», secció **«"Qui" en lloc de "Li he dit / M'ha dit"»**:

- **3e** · Nota ràpida amb el selector «Qui».
- **2e** · Detall amb la cronologia Jo / persona / Apunts, en clar i en fosc.

**Requisit:** la tanda 1 ja feta (`EntryForm` comú amb l'ordre nou).

## Objectiu

El tipus d'entrada «Li he dit · M'ha dit · Ha passat» passa a ser **«Qui»: Jo · {persona} · Apunt**, al formulari, a la cronologia, als filtres, als comptadors, al full d'accions i a l'exportació.

**Model de dades: sense canvis.** `EntryKind` es manté (`SAID` / `HEARD` / `HAPPENED`) i no cal migració.

| `EntryKind` | Abans | Ara (selector / filtre / etiqueta) | Icona / marcador |
| --- | --- | --- | --- |
| `SAID` | Li he dit | **Jo** / Jo / `JO` | `person` (farcida en seleccionar) |
| `HEARD` | M'ha dit | **{nom}** / {nom} / `{NOM}` | avatar de la persona en petit |
| `HAPPENED` | Ha passat | **Apunt** / Apunts / `APUNT` | `edit_note` |

`{nom}` és el **primer mot del nom** de la persona («Jordi Puig» → «Jordi»). Si passa de 12 caràcters, es talla amb «…».

Aquesta tanda resol el bug 4 del backlog («Ha passat» es talla).

---

## 1. Selector «Qui» (formulari)

- Substitueix el `KindSelector` dins d'`EntryForm`, amb el títol de secció `Qui`.
- `ButtonGroup` connectat de 3 opcions a parts iguals, 52 dp d'alt i 2 dp entre botons.
- Seleccionat: pastilla `primary` / `onPrimary`, pes 700, icona farcida. No seleccionat: `surfaceContainerHigh`, pes 500, juntes de 8 dp.
- Contingut de cada botó:
  - **Jo:** icona `person` + «Jo».
  - **{nom}:** l'`Avatar` de la persona a 26 dp + el nom.
  - **Apunt:** icona `edit_note` + «Apunt».
- Per defecte, **Jo**.
- A la nota ràpida sense persona triada, el botó del mig diu «Persona», amb `person_outline`, i està desactivat.
- El text d'ajuda canvia segons «Qui»:
  - Jo: «Què has dit?»;
  - {nom}: «Què ha dit {nom}?»;
  - Apunt: «Què vols apuntar?».

**Al `designsystem`:**

- Ara `ConversesToggleGroup` només mostra la icona a l'opció seleccionada i només accepta `ImageVector`.
- «Qui» necessita:
  - la icona sempre visible;
  - contingut propi (l'`Avatar`);
  - 52 dp d'alt i pes 700/500;
  - una opció desactivada;
  - `role = RadioButton`.
- Es pot ampliar `ConversesToggleGroup` (amb un slot `leading` per opció) o crear-ne un de nou. El tema d'Ajustos el fa servir: no s'hi pot trencar res.

## 2. Detall i cronologia

### Comptadors de la capçalera

`Jo · {nom} · Apunts`, en lloc de `Li he dit · M'ha dit · Converses`.

### Filtres

- Tres `FilterChip` de multiselecció (ja hi ha `selectedKinds`):
  - **Jo**, amb `person`;
  - **{nom}**, amb l'avatar de 22 dp;
  - **Apunts**, amb `edit_note`.
- Els que no tenen entrades s'amaguen (amb `saidCount` / `heardCount` / `happenedCount`).
- `ConversesFilterChip` només accepta `ImageVector`: cal un slot per a l'avatar.

### Marcadors (`TimelineMarker`)

Passen de 40 a 32 dp. `TimelineRow` fa servir `MarkerSize` per a l'amplada de la columna i el centre de la línia.

| Tipus | Marcador |
| --- | --- |
| Jo | Cercle `primary` amb `person` farcida en `onPrimary` |
| {nom} | **L'avatar de la persona** (forma, pastel i inicial, 32 dp), sense fons |
| Apunt | Quadrat de radi 10 amb vora de 2 dp `outline` i `edit_note` en `onSurface` |

Cal un estil de marcador amb contingut propi, a més dels tres fixos d'ara.

### Etiqueta de cada entrada

- Sempre **qui · hora · canal**: `JO · 19:20 · DISCORD`, `JORDI · 19:12 · DISCORD`, `APUNT · 22:00 · EN PERSONA`.
- Avui s'etiqueta en tres llocs amb `EntryKind.label()`: `EntryCard`, la capçalera d'`EntryActionsSheet` i `PersonExporter`. Cal una sola funció, `whoLabel(kind, firstName)`. L'exportador no és composable i necessita `Context`.
- **Bug 3 (canal «Other»):** si es fa abans o al mateix temps, l'etiqueta ha de mostrar `OTHER - SIGNAL`, amb el `handle` del canal `OTHER` de la persona.

### Full d'accions d'entrada

La capçalera té el mateix format i ordre que la cronologia.

## 3. Exportar notes

`PersonExporter` fa servir `Jo:`, `{Nom}:` i `Apunt:`.

## 4. Textos (`strings.xml`)

| Clau | ca | es | en |
| --- | --- | --- | --- |
| `entry_who_title` | Qui | Quién | Who |
| `entry_who_me` | Jo | Yo | Me |
| `entry_who_note` | Apunt | Apunte | Note |
| `entry_who_notes` | Apunts | Apuntes | Notes |
| `entry_hint_me` | Què has dit? | ¿Qué has dicho? | What did you say? |
| `entry_hint_person` | Què ha dit %1$s? | ¿Qué ha dicho %1$s? | What did %1$s say? |
| `entry_hint_note` | Què vols apuntar? | ¿Qué quieres apuntar? | What do you want to note? |

- S'esborren `kind_said`, `kind_heard`, `kind_happened` i `entry_label_*`.
- Cal revisar `timeline_filtered_empty` («No hi ha entrades de %1$s»), que ha de rebre els noms nous.

## 5. Accessibilitat

- Botons de «Qui»: `role = RadioButton`, i el `contentDescription` del botó de la persona és el nom complet.
- Marcador d'avatar: decoratiu. L'entrada es llegeix sencera: «Jordi, avui a les 19:12, per Discord: …».

## Pla en commits

1. Strings (en, ca, es) i `whoLabel()` + `firstName()`, amb tests.
2. Component «Qui» al `designsystem` i el seu ús dins d'`EntryForm`.
3. `TimelineMarker` de 32 dp, amb el marcador d'avatar.
4. Etiquetes, filtres (slot d'avatar i amagar els buits), comptadors i full d'accions.
5. `PersonExporter`.
6. Previews, tests i regenerar les captures de referència (`personDetail*` i components) amb l'etiqueta `update-screenshots` a la PR.

## Proves

- [ ] El text d'ajuda canvia en canviar «Qui», i el nom és correcte (primer mot, tallat a 12).
- [ ] No es talla res amb un nom de 12 caràcters, en ca i es i amb la font gran (bug 4).
- [ ] Sense persona a la nota ràpida: el botó del mig i Desa estan desactivats.
- [ ] Cronologia: marcadors, etiquetes, filtres i comptadors nous, en clar i en fosc, amb persones de colors diferents.
- [ ] Els filtres sense entrades no surten.
- [ ] Exportar notes fa servir les etiquetes noves.
- [ ] El tema d'Ajustos (`ConversesToggleGroup`) es veu igual que abans.
- [ ] Previews: `EntryForm` (tres valors de «Qui»), `TimelineRow` (tres tipus) i el selector amb l'opció desactivada.
