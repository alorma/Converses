# Converses — Color de la persona com a tema

Referència visual: llenç «Converses», secció **«Color de la persona com a llavor del tema»** (Luna · Lavanda, Jordi · Menta, Laia · Préssec, en clar i en fosc, intensitat *tonal*).

## Idea

Cada persona ja té un color d'avatar. A partir d'ara, aquest color també és la **llavor del tema** de totes les pantalles d'aquella persona. El llistat i els Ajustos continuen en monocrom; quan entres a una persona, l'app pren el seu color.

- Els fons i les targetes queden **gairebé neutres**, amb un tint molt lleuger.
- El color fort va als **accents**: el split button «Nota», els filtres de la cronologia, els marcadors de la cronologia i el selector de tipus.
- L'**avatar** manté el pastel fix i les inicials en `#1E1D24`; no surt de l'esquema generat.
- L'**error** continua fix: clar `#BA1A1A` sobre `#FFDAD6`, fosc `#FFB4AB` sobre `#93000A`.

## Abast

| Pantalla o element | Tema de la persona |
| --- | --- |
| Detall (`PersonRoute`) | Sí |
| Desbloqueig (`PersonLockedRoute`) | Sí (el color ja es veu al llistat, no és un secret) |
| Full de nova entrada, accions d'entrada, menú del split button, diàleg d'esborrar | Sí: hereten el tema perquè s'obren des del detall |
| Editar persona (`EditPersonRoute`, persona existent) | Sí, i en directe: si canvies el color, el tema canvia amb animació |
| Nova persona (`EditPersonRoute(0)`) | Sí, amb el color triat (Lavanda per defecte); canvia en directe |
| Nota ràpida (`QuickNoteRoute`) | Només quan ja hi ha una persona triada; sense persona, el tema de l'app |
| Llistat, Arxivades, Ajustos, Triar persona | No: tema de l'app |

## Generació de l'esquema

- Llibreria: MaterialKolor 5.0.1 (la que ja fem servir).
- Llavor: el **color de llavor** de l'avatar, no el pastel. Amb el pastel, l'esquema surt pàl·lid i amb poc contrast.
- Estil: `PaletteStyle.TonalSpot`. Dona el que volem: superfícies amb croma baixa (neutral ≈ 6) i `primary` saturat (to 40 en clar, to 80 en fosc).
- `isDark`: el mateix que el tema de l'app (Clar · Fosc · Sistema).
- El color dinàmic i el color del tema dels Ajustos **no hi intervenen**: dins de la persona mana el seu color.

| Color | Pastel (avatar) | Llavor del tema |
| --- | --- | --- |
| Lavanda | `#DCD3F5` | `#6B55B5` |
| Menta | `#C9EBD6` | `#2F7A52` |
| Préssec | `#FFD6BF` | `#B5602E` |
| Cel | `#CBE3F7` | `#2D6FA3` |
| Rosa | `#F7CFDD` | `#B04670` |
| Llimona | `#F1E9A8` | `#8A7A12` |
| Sorra | `#E8DDCB` | `#86704C` |

Si l'enum `AvatarColor` encara no té la llavor, s'hi afegeix: són els mateixos valors que el `ThemeColor` dels Ajustos.

## Rols que fa servir cada element

No cal tocar els components: ja llegeixen el `MaterialTheme.colorScheme`. Aquesta taula serveix per comprovar que no n'hi ha cap amb colors fixos.

| Element | Rol |
| --- | --- |
| Fons de pantalla | `surface` |
| Targetes, dades fixades, entrades de la cronologia | `surfaceContainerLowest` (o el que ja faci servir `GroupedList`) |
| Xips de canal, botó circular de tancar | `surfaceContainerHigh` |
| Split button «Nota ▾» | `primary` / `onPrimary` |
| Filtres de la cronologia actius, selector de tipus | `primaryContainer` / `onPrimaryContainer` |
| Marcador «Li he dit» | `primary` / `onPrimary` |
| Marcador «M'ha dit» | `primaryContainer` / `onPrimaryContainer` |
| Marcador «Ha passat» | vora `outline`, fons `surface` |
| Línia de la cronologia | `outlineVariant` |
| Metadades, etiquetes | `onSurfaceVariant` |
| Avatar | pastel fix + `#1E1D24` (fora de l'esquema) |
| Accions destructives | `error` fix |

## Implementació

### 1. `PersonTheme` (al mòdul `designsystem`)

```kotlin
@Composable
fun PersonTheme(
    color: AvatarColor,
    content: @Composable () -> Unit,
) {
    val settings = LocalAppSettings.current          // o com s'exposin avui els AppSettings
    if (!settings.personColorTheme) {
        content()
        return
    }
    val isDark = LocalIsDarkTheme.current            // el mateix valor que fa servir ConversesTheme
    val scheme = rememberDynamicColorScheme(
        seedColor = color.seed,
        isDark = isDark,
        style = PaletteStyle.TonalSpot,
    ).withConversesError(isDark)                     // reutilitzar l'override d'error de ConversesTheme

    val animated = scheme.animated()                 // animació de ~300 ms entre esquemes (vegeu el pas 4)

    MaterialExpressiveTheme(
        colorScheme = animated,
        typography = MaterialTheme.typography,       // conservar Roboto Flex i l'escala pròpia
        shapes = MaterialTheme.shapes,
        motionScheme = MaterialTheme.motionScheme,
        content = content,
    )
}
```

Notes:

- Comprova els noms exactes de l'API de MaterialKolor 5.0.1 (`rememberDynamicColorScheme`, els paràmetres `style` i `specVersion`). No facis servir `DynamicMaterialTheme` si perd la tipografia o les formes pròpies: és millor generar l'esquema i passar-lo a `MaterialExpressiveTheme`.
- Si `ConversesTheme` exposa `isDark` d'una altra manera, fes-ho servir. Si no s'exposa, afegeix-hi un `CompositionLocal`.
- Si l'override d'error viu dins de `ConversesTheme`, extreu-lo a una funció `ColorScheme.withConversesError(isDark)` i fes-la servir als dos llocs.

### 2. On s'aplica

- `PersonScreen` (detall): embolcalla tot el contingut **i** els `ModalBottomSheet`, el `DropdownMenu` i l'`AlertDialog` que s'hi obren. Han de quedar dins del `PersonTheme`, perquè agafin els locals de composició del punt on es criden.
- `PersonLockedScreen`: igual, amb el color de la persona. El repositori ja retorna el color per a les protegides.
- `EditPersonScreen`: fes servir el color de l'**estat del formulari** (no el desat), perquè el tema canviï en directe quan es tria un altre pastel.
- `QuickNoteScreen`: `PersonTheme` només si `selectedPerson != null`; si no, el contingut va directament.

### 3. Barres del sistema

Amb l'edge-to-edge actual, les icones de la barra d'estat i de navegació depenen només d'`isDark`, que no canvia. No cal fer-hi res, però comprova-ho amb Llimona i Préssec en clar.

### 4. Transicions

- **Dins d'una pantalla** (canviar el color a l'editor): anima cada rol de l'esquema amb `animateColorAsState` (`tween(300)`), o amb el mecanisme d'animació de MaterialKolor si el té.
- **En entrar al detall** des del llistat: n'hi ha prou amb la transició de navegació actual. És opcional començar amb l'esquema de l'app i animar cap al de la persona en el primer frame; només s'ha de fer si el canvi es veu brusc.

### 5. Ajust nou

A Ajustos → Aparença, sota «Color dinàmic»:

- Fila amb interruptor: **«Color de la persona» · «Les pantalles de cada persona prenen el color del seu avatar»**.
- Clau de DataStore: `person_color_theme` = `true` | `false`. Per defecte `true`.
- `AppSettings`: `val personColorTheme: Boolean = true`.
- Amb l'interruptor apagat, `PersonTheme` no fa res i tot es veu com ara (monocrom amb el tema de l'app).

Actualitza també `settings.md` amb aquesta fila.

## Proves

- [x] Previews de Compose del detall amb els **7 colors × clar i fosc** (`@PreviewParameter` amb `AvatarColor.entries`; test `personDetailThemed`).
- [x] **Contrast:** `onPrimary` sobre `primary` i `onPrimaryContainer` sobre `primaryContainer` ≥ 4,5:1 per als 7 colors (test `PersonThemeContrastTest`, també `onSurface` i `onSurfaceVariant` sobre `surface`).
- [ ] L'avatar pastel es distingeix dels filtres `primaryContainer` (sobretot Lavanda i Cel en clar).
- [ ] Error: «Esborrar persona», «Esborrar aquesta entrada» i l'àlies invàlid continuen en el vermell fix amb qualsevol color.
- [ ] Els fulls i el menú del detall surten amb el color de la persona, no amb el de l'app.
- [ ] Editor: canviar el color anima el tema; si es tanca sense desar, el detall torna al color desat.
- [ ] Amb «Color de la persona» apagat, res canvia respecte d'ara.
- [ ] Rendiment: obrir i tancar el detall diverses vegades sense *jank* (l'esquema es recorda per llavor i `isDark`).

## Fora d'abast

- Tenyir les files del llistat amb el color de cada persona.
- Colors personalitzats fora dels 7 pastels.
- Canviar la intensitat (subtil o tonal) des dels Ajustos: queda fixada a *tonal*.
