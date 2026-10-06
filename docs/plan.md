# Converses — Traspàs de disseny a Compose

Oct 6, 2026 · @Bernat

## Resum

Converses és una app Android per recordar què has dit, què t'han dit i què ha passat amb cada persona amb qui parles, sigui per xat, xarxes o en persona. La v1 es construeix amb Jetpack Compose i Material 3 Expressive, en monocrom amb color dinàmic i el vermell reservat per a errors i accions destructives.

- **Referència visual:** el llenç de disseny "Converses", files *Material 3 Expressive* (clar i fosc). Té 8 pantalles enllaçades i es pot provar amb Play.
- **Abast v1:** llistat de persones, detall amb cronologia unificada, nova entrada (des del llistat i des del detall), editar persona amb dades fixades, accions d'entrada, esborrar persona i vincle amb l'agenda.
- **Fora de la v1:** cerca dins el text de les entrades, exportació real, còpies de seguretat i widgets.

## Stack i dependències

Una sola activitat en Compose, amb Navigation Compose, Room i Hilt. Les versions s'han de confirmar en crear el projecte: les API d'Expressive encara canvien entre versions.

| Peça | Llibreria | Per a què |
| --- | --- | --- |
| UI | `androidx.compose.material3:material3` (1.4 o superior) | `MaterialExpressiveTheme`, `SplitButtonLayout`, `ButtonGroup`, `MaterialShapes`, `HorizontalFloatingToolbar` |
| Formes | `androidx.graphics:graphics-shapes` | `RoundedPolygon` i `.toShape()` dels avatars |
| Color | `com.materialkolor:material-kolor` | Esquemes `Monochrome` i `Neutral` a partir d'una llavor |
| Navegació | `androidx.navigation:navigation-compose` | Rutes tipades i bottom sheets |
| Dades | `androidx.room` (amb KSP) | Persones, canals, entrades i dades fixades |
| DI | Hilt | ViewModels i repositoris |
| Icones | `material-icons-extended` o Material Symbols com a vectors | Icones arrodonides; plena quan està seleccionada |

Tots els components Expressive necessiten `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`. Convé concentrar-lo en un mòdul `designsystem` perquè les pantalles no el repeteixin.

## Tema

El color ve del sistema però es mostra en monocrom: la llavor és el `primary` de `dynamicLightColorScheme(context)` (Android 12+) i MaterialKolor en genera l'esquema. Per sota d'Android 12 s'usa una llavor fixa.

**Color**

- Estil per defecte `PaletteStyle.Neutral` ("tenyit"); opció a Ajustos per passar a `PaletteStyle.Monochrome` ("pur").
- Clar, fosc i "segueix el sistema".
- Error fix, fora de l'esquema dinàmic: clar `#BA1A1A` sobre `#FFDAD6`, fosc `#FFB4AB` sobre `#93000A`. Només per a errors i accions destructives.
- Colors pastel dels avatars: 7 valors fixos guardats a la persona (Lavanda `#DCD3F5`, Menta `#C9EBD6`, Préssec `#FFD6BF`, Cel `#CBE3F7`, Rosa `#F7CFDD`, Llimona `#F1E9A8`, Sorra `#E8DDCB`). Les inicials sempre en fosc, també en mode fosc.

| Rol al disseny | Rol de Material |
| --- | --- |
| Fons de pantalla | `surface` |
| Targetes i files de llista | `surfaceContainerLowest` (clar) / `surfaceContainer` (fosc) |
| Xips i botons no seleccionats | `surfaceContainerHigh` |
| Botó ple, opció seleccionada | `primary` / `onPrimary` |
| Xips de filtre actius, avatars | `primaryContainer` / `onPrimaryContainer` |
| Text secundari | `onSurfaceVariant` |
| Vores i línia de la cronologia | `outline` / `outlineVariant` |

**Tipografia:** Roboto Flex com a família variable. Els títols de pantalla (40 sp) i el nom al detall (30 sp) van en pes 800 i amplada 118; la resta segueix l'escala de Material.

**Formes:** cantonades de 24 dp als extrems de les llistes agrupades i 6 dp entre files, separades per 2 dp. Botons en píndola; dins d'un grup connectat, 8 dp a les juntes.

## Components

Gairebé tot el disseny té component natiu; només la cronologia i la llista agrupada són composables propis.

| Element del disseny | On surt | Compose |
| --- | --- | --- |
| Split button "Nota ▾" | Detall | `SplitButtonLayout` amb `SplitButtonDefaults.LeadingButton` i `TrailingButton`; el trailing obre un `DropdownMenu` |
| Filtres de canal i de cronologia | Llistat, Detall | `ButtonGroup` amb `ToggleButton` (connectat) o `FilterChip` |
| Tipus d'entrada (Li he dit / M'ha dit / Ha passat) | Nova entrada | `ButtonGroup` connectat, selecció única |
| Avatar amb forma | Llistat, Detall, Editar | `Box` amb `clip(MaterialShapes.Cookie9Sided.toShape())`; formes: `Cookie9Sided`, `Clover4Leaf`, `Flower`, `Sunny` |
| Selector de forma i de color | Editar persona | `Row` de `IconToggleButton` amb `selectableGroup()` |
| Barra d'eines flotant | Llistat | `HorizontalFloatingToolbar` amb el FAB integrat |
| FAB "Nota ràpida" | Llistat | `MediumFloatingActionButton` |
| Llista agrupada | Llistat, Detall, Editar | Composable propi `GroupedList` que calcula les cantonades per posició |
| Cronologia | Detall | Composable propi `TimelineItem`: marcador per tipus, línia vertical, targeta |
| Avisos d'error | Llistat (sincronització), Editar (àlies duplicat) | `Card` amb `errorContainer`; camps amb `isError` i `supportingText` |
| Full d'accions i nova entrada des del detall | Detall | `ModalBottomSheet` |
| Confirmació d'esborrar | Detall | `AlertDialog` amb icona en forma |
| Interruptor | Editar | `Switch` amb icona al polze |
| Camps de text | Nova entrada, Editar | `TextField` (filled) amb cantonades superiors de 20 dp |

## Pantalles i navegació

Hi ha 4 pantalles i 4 superposicions (fulls, menú i diàleg); el detall de persona és el nucli.

&#91;embedded content: navegació · 8 destinacions\]

El llistat porta al detall o a la nota ràpida; des del detall surten la nova entrada, el menú de persona i les accions de cada entrada.

| Destinació | Tipus | Ruta | Llenç |
| --- | --- | --- | --- |
| Llistat | Pantalla | `people` | 1 |
| Detall | Pantalla | `person/{id}` | 2 |
| Menú de persona | `DropdownMenu` del split button | dins de `person/{id}` | 2b |
| Nova entrada | `ModalBottomSheet` | dins de `person/{id}` | 2c |
| Nota ràpida | Pantalla | `entry/new?personId=` | 3 |
| Editar persona | Pantalla | `person/{id}/edit` (i `person/new`) | 4 |
| Accions d'entrada | `ModalBottomSheet` | dins de `person/{id}` | 5 |
| Esborrar persona | `AlertDialog` | dins de `person/{id}/edit` | 6 |

## Model de dades

Quatre taules a Room; la cronologia és una sola consulta sobre `entry`, ordenada per data i sense separar per canal.

| Taula | Camps | Notes |
| --- | --- | --- |
| `person` | `id`, `name`, `alias?`, `avatarShape`, `avatarColor`, `contactLookupKey?`, `archived`, `createdAt`, `updatedAt` | `avatarShape` i `avatarColor` són enums; `contactLookupKey` només si està vinculada |
| `channel` | `id`, `personId`, `type`, `handle?` | `type`: `DISCORD`, `TELEGRAM`, `WHATSAPP`, `REDDIT`, `PHONE`, `IN_PERSON`, `OTHER`; `IN_PERSON` sense `handle` |
| `entry` | `id`, `personId`, `kind`, `channelType`, `text`, `occurredAt` | `kind`: `SAID`, `HEARD`, `HAPPENED`; `channelType` en lloc d'una clau a `channel`, perquè l'entrada sobrevisqui si es treu el canal |
| `pinned_fact` | `id`, `personId`, `label`, `value`, `position` | Només s'editen des d'Editar persona; `position` per reordenar |

Totes les claus `personId` amb `onDelete = CASCADE`. Índex a `entry(personId, occurredAt)`.

**Consultes clau**

- Llistat: persones no arxivades amb l'última entrada i els seus canals, ordenades per `occurredAt` de l'última entrada.
- Comptadors del detall: `COUNT` per `kind`.
- Cronologia: `entry` per persona, filtrable per un conjunt de `kind`, agrupada per dia a la UI.
- Validació: àlies únic (sense distingir majúscules) entre persones no arxivades.

## Sincronització amb l'agenda

La v1 només vincula, no escriu a l'agenda: la persona guarda la *lookup key* del contacte i l'app en llegeix nom i foto quan cal.

1. L'usuari toca "Vincula a l'agenda" (detall, menú del split button o Editar persona).
2. Es demana `READ_CONTACTS` en aquell moment, no en obrir l'app.
3. S'obre el selector del sistema amb `ActivityResultContracts.PickContact()`.
4. Es guarda `ContactsContract.Contacts.LOOKUP_KEY` a `person.contactLookupKey`.
5. En llegir, es resol amb `Contacts.lookupContact()`; si el contacte ja no hi és, es mostra l'avís d'error del llistat amb "Reintenta" i l'opció de desvincular.

**Esborrar també de l'agenda** (diàleg d'esborrar persona) necessita `WRITE_CONTACTS`. Es pot deixar fora de la v1 i amagar la casella si no hi ha permís.

## Ordre d'implementació

Cada pas deixa l'app executable; el disseny es comprova contra el llenç en acabar cada pantalla.

1. Projecte, mòdul `designsystem` i tema (color dinàmic monocrom, Roboto Flex, `GroupedList`, avatar amb forma).
2. Room: entitats, DAO, repositori i dades de prova.
3. Llistat de persones amb filtres i barra d'eines flotant.
4. Detall: capçalera, dades fixades i cronologia amb filtres.
5. Nova entrada: full des del detall i pantalla des del llistat.
6. Editar persona: forma, color, àlies amb validació, dades fixades, canals.
7. Accions d'entrada, menú del split button i esborrar persona.
8. Vincle amb l'agenda.

**Decisions obertes**

- [ ] Els avatars del llistat i del detall surten amb el color pastel de cada persona o en monocrom?
- [ ] "Esborrar tota la conversa" al menú d'una entrada: es canvia per "Esborrar les entrades d'aquest dia" o es treu?
- [ ] Esborrar també de l'agenda entra a la v1 (`WRITE_CONTACTS`) o es deixa per després?
- [ ] Què fan "Exportar notes" i "Arxivar" exactament (format d'exportació, on es veuen les persones arxivades)?
