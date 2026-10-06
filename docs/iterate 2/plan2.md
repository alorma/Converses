# Converses — Traspàs de disseny a Compose (v2)

Oct 6, 2026 · @Bernat

## Canvis respecte a la v1

La v2 afegeix la persona protegida amb empremta i fixa que cada avatar porti el seu color pastel a tota l'app.

- **Persona protegida (nou):** interruptor a Editar persona; al llistat només es veu el nom; el detall es desbloqueja amb `BiometricPrompt`. Pantalla nova 2d.
- **Avatars:** el color pastel triat a Editar persona surt al llistat i a totes les pantalles. Desapareix l'estat de "novetats": l'activitat l'entra l'usuari, no n'hi ha de nova.
- **Model:** `person` guanya `isProtected`.
- **Navegació:** el llistat porta a 2d (desbloqueig) quan la persona està protegida.
- **Decisió tancada:** avatars amb color pastel al llistat i al detall. El vincle amb l'agenda queda fora de la v1 i es treu del disseny.

## Resum

Converses és una app Android per recordar què has dit, què t'han dit i què ha passat amb cada persona, sigui per xat, xarxes o en persona. Es construeix amb Jetpack Compose i Material 3 Expressive, en monocrom amb color dinàmic; el color només apareix als avatars (pastel) i als errors (vermell).

- **Referència visual:** el llenç "Converses", files *Material 3 Expressive* (clar i fosc), 10 destinacions enllaçades i provables amb Play.
- **Abast v1:** llistat, detall amb cronologia unificada, nova entrada (des del llistat i des del detall), editar persona amb dades fixades, persona protegida, accions d'entrada i esborrar persona.
- **Fora de la v1:** vincle amb l'agenda, cerca dins el text de les entrades, exportació real, còpies de seguretat i widgets.

## Stack i dependències

Una sola activitat en Compose, amb Navigation Compose, Room, Koin i Biometric. Les versions s'han de confirmar en crear el projecte: les API d'Expressive encara canvien entre versions.

| Peça | Llibreria | Per a què |
| --- | --- | --- |
| UI | `androidx.compose.material3:material3` (1.4 o superior) | `MaterialExpressiveTheme`, `SplitButtonLayout`, `ButtonGroup`, `MaterialShapes`, `HorizontalFloatingToolbar` |
| Formes | `androidx.graphics:graphics-shapes` | `RoundedPolygon` i `.toShape()` dels avatars |
| Color | `com.materialkolor:material-kolor` | Esquemes `Monochrome` i `Neutral` a partir d'una llavor |
| Biometria | `androidx.biometric:biometric` | `BiometricPrompt` per a les persones protegides |
| Navegació | `androidx.navigation:navigation-compose` | Rutes tipades i bottom sheets |
| Dades | `androidx.room` (amb KSP) | Persones, canals, entrades i dades fixades |
| DI | Hilt | ViewModels i repositoris |
| Icones | Material Symbols Rounded com a vectors | Plena quan està seleccionada, contorn quan no |

Tots els components Expressive necessiten `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`; convé concentrar-lo al mòdul `designsystem`.

## Tema

La interfície és monocrom amb color dinàmic; només els avatars (pastel, un per persona) i els errors (vermell) porten color propi.

**Color dinàmic**

- Llavor: el `primary` de `dynamicLightColorScheme(context)` a Android 12+; llavor fixa `#5B6EAE` a versions anteriors.
- Esquema amb MaterialKolor: `PaletteStyle.Neutral` ("tenyit") per defecte, `PaletteStyle.Monochrome` ("pur") com a opció d'Ajustos.
- Clar, fosc i "segueix el sistema".

**Colors fixos**

| Ús | Clar | Fosc |
| --- | --- | --- |
| Error, accions destructives | `#BA1A1A` sobre `#FFDAD6` | `#FFB4AB` sobre `#93000A` |
| Avatars (7 pastels) | Lavanda `#DCD3F5`, Menta `#C9EBD6`, Préssec `#FFD6BF`, Cel `#CBE3F7`, Rosa `#F7CFDD`, Llimona `#F1E9A8`, Sorra `#E8DDCB` | Els mateixos |
| Inicials sobre l'avatar | `#1E1D24` | `#1E1D24` |

L'avatar és la identitat de la persona: el mateix color i la mateixa forma al llistat, al detall, als fulls i als diàlegs. No codifica cap estat.

**Rols de Material**

| Rol al disseny | Rol de Material |
| --- | --- |
| Fons de pantalla | `surface` |
| Targetes i files de llista | `surfaceContainerLowest` (clar) / `surfaceContainer` (fosc) |
| Xips i botons no seleccionats | `surfaceContainerHigh` |
| Botó ple, opció seleccionada | `primary` / `onPrimary` |
| Xips de filtre actius | `primaryContainer` / `onPrimaryContainer` |
| Text secundari | `onSurfaceVariant` |
| Vores i línia de la cronologia | `outline` / `outlineVariant` |

**Tipografia:** Roboto Flex variable. Títols de pantalla (40–44 sp), nom al detall (30 sp) i encapçalaments (22–28 sp) en pes 800, amplada 118 i mida òptica 72; la resta segueix l'escala de Material.

**Formes:** llistes agrupades amb 24 dp als extrems i 6 dp entre files, separades 2 dp. Botons en píndola; dins d'un grup connectat, 8 dp a les juntes.

## Components

Gairebé tot el disseny té component natiu; la cronologia, la llista agrupada i l'avatar són composables propis del mòdul `designsystem`.

| Element del disseny | On surt | Compose |
| --- | --- | --- |
| `PersonAvatar` (forma + color + inicials o cadenat) | Totes | `Box` amb `clip(MaterialShapes.X.toShape())`; formes `Cookie9Sided`, `Clover4Leaf`, `Flower`, `Sunny`; mostra un cadenat si la persona està protegida |
| Split button "Nota ▾" | Detall | `SplitButtonLayout`; la part principal obre la nova entrada, el trailing un `DropdownMenu` amb les accions de persona |
| Filtres de canal i de cronologia | Llistat, Detall | `ButtonGroup` amb `ToggleButton` o `FilterChip`, multiselecció a la cronologia |
| Tipus d'entrada (Li he dit / M'ha dit / Ha passat) | Nova entrada | `ButtonGroup` connectat, selecció única |
| Selector de forma i de color de l'avatar | Editar persona | `Row` de `IconToggleButton` amb `selectableGroup()`; vista prèvia en directe |
| Dades fixades editables | Editar persona | `GroupedList` amb dos `BasicTextField` per fila, nansa de reordenar i botó de treure |
| Barra d'eines flotant | Llistat | `HorizontalFloatingToolbar` amb FAB "Nota ràpida" |
| Llista agrupada | Llistat, Detall, Editar | `GroupedList`: cantonades segons la posició |
| Cronologia | Detall | `TimelineItem`: marcador per tipus, línia vertical, targeta amb tipus · hora · canal |
| Avisos d'error | Editar (àlies duplicat) | `Card` amb `errorContainer`; camps amb `isError` i `supportingText` |
| Fulls inferiors | Nova entrada des del detall, Accions d'entrada | `ModalBottomSheet` |
| Confirmació d'esborrar | Editar persona | `AlertDialog` amb icona dins una forma |
| Desbloqueig | Persona protegida | `BiometricPrompt` del sistema (no es dibuixa a l'app) |
| Interruptors | Editar persona | `Switch` amb icona al polze |

## Pantalles i navegació

Hi ha 5 pantalles i 4 superposicions; el detall de persona és el nucli i, si la persona està protegida, s'hi arriba passant pel desbloqueig.

&#91;embedded content: navegació · 9 destinacions\]

El llistat porta al detall (o al desbloqueig si és protegida) i a la nota ràpida; del detall surten la nova entrada, el menú de persona i les accions de cada entrada.

| Destinació | Tipus | Ruta | Llenç |
| --- | --- | --- | --- |
| Llistat | Pantalla | `people` | 1 |
| Detall | Pantalla | `person/{id}` | 2 |
| Menú de persona | `DropdownMenu` del split button | dins de `person/{id}` | 2b |
| Nova entrada | `ModalBottomSheet` | dins de `person/{id}` | 2c |
| Desbloqueig | Pantalla + `BiometricPrompt` | `person/{id}/locked` | 2d |
| Nota ràpida | Pantalla | `entry/new?personId=` | 3 |
| Editar persona | Pantalla | `person/{id}/edit` (i `person/new`) | 4 |
| Accions d'entrada | `ModalBottomSheet` | dins de `person/{id}` | 5 |
| Esborrar persona | `AlertDialog` | dins de `person/{id}/edit` | 6 |

El menú de persona té 3 accions: Editar persona, Exportar notes i Arxivar. Esborrar es fa des d'Editar persona.

## Persona protegida

Una persona protegida només mostra el nom fins que l'usuari s'identifica amb l'empremta (o el PIN del dispositiu). S'activa amb l'interruptor "Persona protegida" d'Editar persona.

**Al llistat**

- Es veu: nom, avatar amb un cadenat en lloc de les inicials, cadenat petit al costat del nom i la línia "Protegida · toca per desbloquejar".
- No es veu: hora, canals, ni última entrada.
- La cerca només la troba pel nom, mai per dades fixades ni per text d'entrades.

**En obrir-la**

1. Es navega a `person/{id}/locked` (pantalla 2d): avatar amb cadenat, nom i botó "Desbloqueja".
2. Es llança `BiometricPrompt` automàticament, amb `BIOMETRIC_STRONG or DEVICE_CREDENTIAL`.
3. Si va bé, es substitueix la ruta per `person/{id}` (sense deixar 2d a la pila).
4. Si es cancel·la, es queda a 2d; el botó "Desbloqueja" el torna a llançar.

**Regles**

- El desbloqueig dura fins que se surt del detall o l'app va a segon pla (decisió oberta, vegeu el final).
- Si el dispositiu no té biometria ni bloqueig de pantalla, l'interruptor es desactiva amb una explicació.
- Dins el detall desbloquejat, `FLAG_SECURE` per evitar captures i la vista prèvia a Recents.
- Desactivar la protecció també demana l'empremta.

## Model de dades

Quatre taules a Room; la cronologia és una sola consulta sobre `entry`, ordenada per data i sense separar per canal.

| Taula | Camps | Notes |
| --- | --- | --- |
| `person` | `id`, `name`, `alias?`, `avatarShape`, `avatarColor`, `isProtected`, `archived`, `notes?`, `createdAt`, `updatedAt` | `avatarShape` i `avatarColor` són enums; `isProtected` activa el desbloqueig amb empremta |
| `channel` | `id`, `personId`, `type`, `handle?` | `type`: `DISCORD`, `TELEGRAM`, `WHATSAPP`, `REDDIT`, `PHONE`, `IN_PERSON`, `OTHER`; `IN_PERSON` sense `handle` |
| `entry` | `id`, `personId`, `kind`, `channelType`, `text`, `occurredAt` | `kind`: `SAID`, `HEARD`, `HAPPENED`; `channelType` en lloc d'una clau a `channel`, perquè l'entrada sobrevisqui si es treu el canal |
| `pinned_fact` | `id`, `personId`, `label`, `value`, `position` | Només s'editen des d'Editar persona; `position` per reordenar |

Totes les claus `personId` amb `onDelete = CASCADE`. Índex a `entry(personId, occurredAt)`.

**Consultes clau**

- Llistat: persones no arxivades amb la data de l'última entrada, els canals i l'últim text; per a les protegides, el repositori no retorna ni el text ni els canals.
- Comptadors del detall: `COUNT` per `kind`.
- Cronologia: `entry` per persona, filtrable per un conjunt de `kind`, agrupada per dia a la UI.
- Cerca: per nom i àlies a totes; per dades fixades i text només a les no protegides.
- Validació: àlies únic (sense distingir majúscules) entre persones no arxivades.

## Ordre d'implementació

Cada pas deixa l'app executable; el disseny es comprova contra el llenç en acabar cada pantalla.

1. Projecte, mòdul `designsystem` i tema (color dinàmic monocrom, Roboto Flex, `GroupedList`, `PersonAvatar` amb forma i color).
2. Room: entitats, DAO, repositori i dades de prova.
3. Llistat de persones amb filtres i barra d'eines flotant.
4. Detall: capçalera, dades fixades i cronologia amb filtres.
5. Nova entrada: full des del detall i pantalla des del llistat.
6. Editar persona: forma, color, àlies amb validació, dades fixades, canals.
7. Split button amb menú, accions d'entrada i esborrar persona.
8. Persona protegida: interruptor, pantalla 2d, `BiometricPrompt` i filtratge al llistat i la cerca.

**Decisions obertes**

- [ ] Quan es torna a bloquejar una persona protegida: en sortir del detall o quan l'app va a segon pla?
- [ ] Ordre de la capçalera del llistat: cercador a dalt i filtres a sota (clar) o filtres a dalt i cercador a sota (fosc)? Al llenç difereixen.
- [ ] "Esborrar tota la conversa" al menú d'una entrada: es canvia per "Esborrar les entrades d'aquest dia" o es treu?
- [ ] Què fan exactament "Exportar notes" i "Arxivar" (format d'exportació, on es veuen les persones arxivades)?
