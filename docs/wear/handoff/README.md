# Converses · versió Wear OS — instruccions per a Claude Code

Disseny de la versió de rellotge de **Converses** (i del flux de confirmació al mòbil) (`com.alorma.contactnotes`, repo `alorma/Contacts-Notes`). Aquest document és el brief: què construir, com es veu i quines decisions ja estan preses.

## Què hi ha al zip

| Carpeta | Contingut |
|---|---|
| `screenshots/` | 16 pantalles en PNG (rellotge a 720×720 amb fons transparent; mòbil a 2×) + `00-overview-rellotge.png` i `00-overview-mobil.png`. |
| `design/` | Fonts `.dc.html` de cada pantalla i `canvas.json`. Són la **referència exacta** de mides, radis i colors (estils inline). |
| `tokens/converses-tokens.json` | Tokens del sistema de disseny Converses (clar/fosc, tipografia, espaiats, radis). |

Les captures són una aproximació: la tipografia hi surt amb una font de reserva (Inter) en lloc de Roboto Flex, i les icones són SVG equivalents a Material Symbols Rounded. Per a valors exactes, mana el HTML de `design/`.

## Premisses (importants)

1. **No és una app standalone.** El rellotge és un mirall del mòbil. El mòbil (Room) és l'única font de veritat.
2. **Sense núvol.** La sincronització va de rellotge a mòbil amb la Wearable Data Layer (`play-services-wearable`). No s'afegeix cap servidor propi ni Firebase. Mantén `allowBackup` desactivat.
3. **Només lectura + una acció.** Al rellotge no es creen, editen ni esborren persones. L'única escriptura és **una nota per veu** a una persona existent.
4. **Persones protegides:** s'obren al rellotge només amb **aprovació biomètrica al mòbil** (BiometricPrompt), cada cop. El rellotge no n'emmagatzema les entrades (vegeu «Persones protegides: aprovació al mòbil»).
5. **Bloqueig de l'app:** si està activat al mòbil, el rellotge exigeix que **tingui un bloqueig de pantalla propi (PIN, patró o contrasenya)**. No és biomètric, però és protecció (vegeu «Bloqueig de l'app»).

## Pantalles i flux

Flux: Persones → Cronologia → Dictat → Qui → Desat. Més dos estats d'excepció.

| # | Captura | Què fa |
|---|---|---|
| 1 | `01-persones` | Llista agrupada de persones (avatar Galeta, nom, hora de l'última entrada). Toc → Cronologia. Si la persona és protegida: cadenat i toc → pantalla 7. |
| 2 | `02-cronologia` | Capçalera (avatar + nom) i entrades: qui (Jo / persona / Apunt), hora, canal, text. **EdgeButton «Nota»** fix a baix, acció principal → Dictat. |
| 3 | `03-dictat` | Transcripció en directe, ones de veu i EdgeButton «Acaba» → Qui. |
| 4 | `04-qui` | Grup connectat de 3 opcions: Jo · {persona} · Apunt. **Per defecte, la persona** (l'altra). EdgeButton «Desa». |
| 5 | `05-desat` | Confirmació: Galeta gran amb check, «Desat», «Enviada al mòbil». Es tanca sola i torna a la Cronologia. |
| 6 | `06-desat-sense-mobil` | Mateix, si el mòbil no és accessible: icona de rellotge, «S'enviarà al mòbil quan hi hagi connexió». |
| 7 | `07-confirma-ho-al-mobil` | Espera d'aprovació d'una persona protegida: anell de compte enrere (≈ 60 s), «Queden N s» i «Cancel·la». |
| 10 | `10-denegat` | «Denegat · S'ha rebutjat al mòbil.» + «Tanca». Neutre, sense vermell (no és un error). |
| 11 | `11-ha-caducat` | «Ha caducat · No s'ha confirmat a temps.» + «Reintenta». |
| 12 | `12-no-disponible-sense-mobil` | «No disponible sense el mòbil · El rellotge no troba el mòbil.» + «Tanca». |
| 13 | `13-mobil-notificacio` | Notificació del mòbil, genèrica i sense nom. |
| 14 | `14-mobil-confirma` | Pantalla en tocar la notificació, abans de la biometria: «Confirma» / «Denega». |
| 15 | `15-mobil-empremta` | BiometricPrompt (referència; el dibuixa el sistema, reutilitza el diàleg existent). |
| 16 | `16-mobil-fet` | «Fet · Ja pots veure la persona al rellotge.» + «Tanca». |
| 8 | `08-persones-sense-mobil` | Llista amb la còpia local i un avís: «Mòbil no connectat · Dades de fa N min». |
| 9 | `09-cal-bloqueig-rellotge` | Si el Bloqueig de l'app és actiu al mòbil i el rellotge **no té bloqueig**: «Cal un bloqueig · Activa un PIN al rellotge.» + EdgeButton «Configura». Substitueix qualsevol altra pantalla; no mostra cap dada. |

Amb el mòbil connectat **no es mostra cap indicador de sincronització**; només apareix a les excepcions (6 i 8).

## Arquitectura proposada

- Nou mòdul `:wear` (aplicació Wear OS) amb **el mateix `applicationId`** que el mòbil, perquè Play els empari com a parella.
- Manifest: `<uses-feature android:name="android.hardware.type.watch" />` i
  `<meta-data android:name="com.google.android.wearable.standalone" android:value="false" />`.
- UI: **Wear Compose Material 3** (`androidx.wear.compose:compose-material3`): `AppScaffold`, `ScreenScaffold`, `TransformingLazyColumn`, `EdgeButton`, `TimeText`, `Button`/`Card`. Navegació amb `androidx.wear.compose:compose-navigation` (`SwipeDismissableNavHost`). Mira les versions estables al version catalog; no fixis cap versió de memòria.
- Rutes: `people`, `person/{id}`, `dictate/{personId}`, `who/{personId}`, `saved/{pending}`, `approval/{personId}`, `locked`.
- Tema: crea `ConversesWearTheme` sobre Wear M3 amb els tokens de sota. Si ja hi ha mòdul de design system compartit, reutilitza'l; si no, deixa els tokens en un fitxer del mòdul `:wear`.
- Models de presentació compartits (persona, entrada) en un mòdul comú, perquè mòbil i rellotge parlin el mateix format.

## Sincronització (mòbil → rellotge)

- El mòbil publica un **instantani** amb `DataClient` (un `DataItem` per persona o un únic item amb la llista; decideix-ho per mida) amb:
  - persona: `id`, nom, color d'avatar, forma d'avatar, `protected`, hora de l'última entrada;
  - entrades recents: **només les últimes 5 per persona** (decisió: còpia reduïda): `who` (jo / persona / apunt), hora, canal (grup), text. Cap més s'emmagatzema al rellotge de forma persistent.
- **Persones protegides:** a la còpia local només `id`, nom, color i `protected = true`. **Cap entrada, cap canal, cap hora.** Les seves entrades es demanen al mòbil només després de l'aprovació biomètrica al mòbil (vegeu «Persones protegides»). (Supòsit meu: confirma si el nom també s'ha d'ocultar a la llista.)
- El rellotge desa l'última còpia rebuda en emmagatzematge privat (DataStore o una Room petita pròpia) i hi guarda l'**hora de l'última sincronització** per al «Dades de fa N min». Aquest emmagatzematge ha de quedar **exclòs de la còpia de seguretat** (`allowBackup="false"` i regles de backup al mòdul `:wear`).
- Refresca l'instantani quan canvien dades al mòbil (escoltar canvis a Room i republicar) i en connectar-se.
- «Mòbil no connectat» = `NodeClient`/`CapabilityClient` no troba el node del mòbil. Anuncia la capacitat `converses_phone` al mòbil (`res/values/wear.xml`, `android_wear_capabilities`).

## Més entrades sota demanda

- En obrir la Cronologia d'una persona, el rellotge **demana al mòbil més entrades automàticament** (en entrar i en arribar al final de la llista), amb petició/resposta per `MessageClient`. Pàgines de 20 (ajustable).
- Les entrades extra viuen **només en memòria** (per exemple al ViewModel) i s'obliden en sortir de la pantalla. L'emmagatzematge persistent continua sent només les 5 últimes.
- UI: un indicador de càrrega petit al final de la llista mentre arriba la pàgina (no dissenyat; fes-lo coherent amb Wear M3). Per a persones protegides, vegeu «Persones protegides».
- Si el mòbil **no és accessible** (rellotge no emparellat o desconnectat), no es pot carregar res més: es veuen només les 5 de la còpia i la llista s'acaba sense error destacat. *(Suposició meva: la frase original es va tallar; confirma-ho.)*

## Persones protegides: aprovació al mòbil

Disseny: pantalles 7, 10, 11 i 12 (rellotge) i 13–16 (mòbil).

Flux:
1. Toc a una persona protegida (cadenat) al rellotge → pantalla 7 «Confirma-ho al mòbil», amb anell de compte enrere, «Queden N s» (≈ 60 s) i «Cancel·la».
2. El rellotge envia una petició per `MessageClient` (`requestId` únic + `personId`). Si el mòbil no és accessible → pantalla 12.
3. El mòbil publica una **notificació genèrica, sense nom** («El rellotge demana obrir una persona protegida», pantalla 13), amb `PendingIntent` immutable cap a una activity pròpia que porta el `requestId`.
4. En tocar-la, pantalla 14 («El rellotge vol obrir una persona protegida», «Confirma» / «Denega»). Tampoc hi surt el nom. «Confirma» llança el **BiometricPrompt que ja existeix** (pantalla 15: títol «Confirma l'accés», subtítol «Obre una persona protegida al rellotge.»). Les pantalles 14 i 15 són de referència; el prompt el pinta el sistema.
5. Èxit → pantalla 16 «Fet». El mòbil respon al rellotge amb l'aprovació **i les entrades de la persona**; el rellotge obre la Cronologia amb les dades només en memòria.
6. «Denega», cancel·lar el prompt o fallar la biometria → pantalla 10 «Denegat» al rellotge.
7. Sense aprovació en ≈ 60 s → pantalla 11 «Ha caducat» amb «Reintenta» (nova petició amb un `requestId` nou). El mòbil invalida la petició en caducar.

Regles:
- Cada petició és d'un sol ús i caduca; no es reutilitza mai una aprovació.
- Les entrades d'una persona protegida **no s'envien al rellotge fins a l'aprovació** i no es persisteixen. La sessió acaba en sortir de la persona o amb l'app en segon pla (confirma-ho amb mi).
- La notificació és genèrica perquè no filtri res a la pantalla de bloqueig: cap text pot portar el nom.
- Si l'usuari cancel·la al rellotge, retira la notificació i tanca el prompt si és possible.
- Gravar una nota a una persona protegida només és possible dins una sessió aprovada.
- La llista no ensenya res d'una persona protegida: només nom i cadenat (sense hora, canals ni últim comentari).
- El rellotge dibuixa el compte enrere, però el temps el valida el mòbil.

## Bloqueig de l'app

- El mòbil envia l'estat de **Bloqueig de l'app** (actiu / inactiu) amb la resta de l'instantani.
- **Inactiu:** el rellotge funciona normalment.
- **Actiu:** el rellotge només mostra dades si és **segur**: té PIN, patró o contrasenya (`KeyguardManager.isDeviceSecure`). No és biomètric, però és una protecció real: el rellotge es bloqueja quan se'l treu del canell.
- Si el rellotge **no té bloqueig**, mostra la pantalla 9 («Cal un bloqueig · Activa un PIN al rellotge.» + EdgeButton «Configura», que obre els ajustos de seguretat del rellotge si el sistema ho permet) i **no mostra ni desa cap entrada**.
- Mentre el bloqueig és actiu, la còpia local (les 5 últimes) es guarda **xifrada amb una clau d'Android Keystore** lligada a l'autenticació del dispositiu, si l'API ho permet al rellotge. Si l'usuari treu el bloqueig del rellotge, **esborra la còpia** a la primera comprovació (en obrir l'app i a cada sincronització).
- Si no pots verificar l'estat del bloqueig de l'app (mòbil absent i cap valor conegut), assumeix **actiu**.
- No es demana el PIN dins l'app a cada obertura; es confia en el bloqueig de sistema del rellotge. *(Variant possible: demanar la credencial del dispositiu en obrir. Confirma-ho.)*
- Les notes ja a la cua de pendents segueixen les mateixes regles: no es mostren i s'entreguen quan es pugui.
- Les «més entrades sota demanda» també queden aturades si el rellotge no és segur.

## Sincronització (rellotge → mòbil): la nota

- Al desar, el rellotge envia un missatge amb `MessageClient` al node del mòbil: `{ personId, who, text, timestamp }`. Via de canal fixa (`/converses/note`).
- El mòbil rep amb un `WearableListenerService`, valida (la persona existeix; si és protegida, la nota només ha pogut gravar-se amb una aprovació vigent al mòbil) i fa l'alta com qualsevol entrada nova. Si el canal no s'ha triat, deixa'l buit o el valor per defecte que ja usi la nota ràpida.
- **Cua de pendents al rellotge** (WorkManager o cua pròpia) amb reintents: si el mòbil no és accessible, la nota queda guardada i es mostra la pantalla 6. En entregar-se, s'esborra de la cua.
- **Idempotència:** cada nota porta un UUID generat al rellotge; el mòbil ignora duplicats.
- **Room:** si cal una columna nova (per exemple `source = watch`), escriu la **migració** (hi ha una versió alpha a Google Play; res de destructive fallback). Preferiblement, no toquis l'esquema.

## Dictat

- Pantalla 3: mostra el text parcial a mesura que arriba. Opcions: `SpeechRecognizer` amb `EXTRA_PARTIAL_RESULTS` per tenir transcripció en directe, o `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` del sistema (més simple, però sense text en directe). Tria i explica el motiu.
- Idioma: el de l'app (ca, es, en). Si el reconeixedor del rellotge no suporta català, avisa'm abans d'inventar una alternativa.
- Permís `RECORD_AUDIO` al rellotge; la demanda i l'estat de «denegat» **no estan dissenyats** (vegeu pendents).
- Les ones de la captura són decoratives; ancora-les a l'amplitud del micròfon si és fàcil, o deixa-les com a animació simple (respecta «reduir animacions»).

## Tokens (tema fosc; el rellotge és sempre fosc)

| Token | Valor | Ús al rellotge |
|---|---|---|
| fons de pantalla | `#000000` | Negre pur (OLED); **no** és el `surface` `#0F0F0F` del mòbil |
| `surface-card` | `#1C1C1C` | Files de llista, entrades de la cronologia |
| `surface-card-high` | `#252525` | Opcions no seleccionades del «Qui», avís sense mòbil, botó «Tanca», cercle d'icona |
| `primary-container` | `#404040` | Opció seleccionada del «Qui» |
| `primary` / `on-primary` | `#E8E8E8` / `#161616` | EdgeButton principal, marcador «Jo», Galeta de «Desat» |
| `on-surface` | `#EBEBEB` | Text principal |
| `on-surface-variant` | `#B7B7B7` | Metadades, subtítols |
| `outline` | `#808080` | Barres d'ones atenuades |
| avatars | lavender `#dcd3f5`, mint `#c9ebd6`, peach `#ffd6bf`, sky `#cbe3f7`, rose `#f7cfdd`, lemon `#f1e9a8`, sand `#e8ddcb` | Fons d'avatar; text/icona sobre avatar sempre `#1e1d24` |

Regla de color del sistema: el color només és **identitat** (avatars) o **error**. No hi ha error dissenyat al rellotge.

- **Tipografia:** Roboto Flex. Títols a pes 800 amb `wdth 118`, `opsz 72`, interlletratge lleugerament negatiu; la resta, amplada normal. Mides del disseny (px ≈ dp): títol de llista 22, nom a la capçalera 20, títol del «Qui» 18, «Desat» 28, text de fila 15/600, subtítol 12, text d'entrada 14/19, text del dictat 17/24, overline 12/700 en majúscules, botó 15/700.
- **Formes:** Galeta (`MaterialShapes.Cookie9Sided`) per als avatars i per a la confirmació. Llistes connectades: separació 2, radi 24 a fora i 6 a dins. Entrades de la cronologia: radi 18. EdgeButton: 160×44, cantonades superiors 20, la inferior segueix la pantalla.
- **Icones:** Material Symbols Rounded: `mic`, `check`, `person` (Jo), mini avatar (persona), `edit_note` (Apunt), `lock` (farcit), `schedule`, `phonelink_off`.
- **Mides de referència:** el disseny fa 240×240 (≈ dp). Comprova-ho amb les previsualitzacions de **Wear OS Large Round** i **Small Round**. Usa les mides del disseny com a proporció i deixa que `TransformingLazyColumn` faci l'escalat de vora; no codifiquis px absoluts de pantalla.
- **Marges a la pantalla rodona:** respecta les zones de seguretat; els elements propers a la vora (llista, EdgeButton) són estrets a dalt i a baix pel cercle.

## Textos (català, veu de l'app)

Segona persona, frases curtes, verbs en imperatiu, sense exclamacions ni emojis. Reutilitza els recursos existents del mòbil (Jo, Apunt, Desa, grups de canal) i afegeix només els nous:

`Nota` · `Acaba` · `Qui ho ha dit?` · `Nota per a {nom}` · `Desat` · `Enviada al mòbil` · `S'enviarà al mòbil quan hi hagi connexió` · `Mòbil no connectat` · `Dades de fa {n} min` · `Persona protegida` · `Confirma-ho al mòbil` · `Queden {n} s` · `Cancel·la` · `Denegat` · `S'ha rebutjat al mòbil.` · `Ha caducat` · `No s'ha confirmat a temps.` · `Reintenta` · `No disponible sense el mòbil` · `El rellotge no troba el mòbil.` · `El rellotge demana obrir una persona protegida` · `El rellotge vol obrir una persona protegida` · `Confirma que ets tu amb l'empremta.` · `Confirma` · `Denega` · `Confirma l'accés` · `Obre una persona protegida al rellotge.` · `Fet` · `Ja pots veure la persona al rellotge.` · `Cal un bloqueig` · `Activa un PIN al rellotge.` · `Configura`

Tradueix-los a castellà i anglès amb el mateix criteri que la resta de l'app.

## Fora d'abast / no fer

- Cap pantalla de creació, edició, arxiu o ajustos al rellotge.
- Cap selecció de color de tema, ni funcions Pro.
- No obrir mai una persona protegida al rellotge sense aprovació biomètrica al mòbil, ni persistir-ne el contingut.
- Cap analítica de contingut. Només dades dins el parell mòbil–rellotge.

## Pendents i punts a verificar (no inventis, pregunta'm)

1. **EdgeButton i scroll.** Al `ScreenScaffold` de Wear M3, comprova si l'`edgeButton` es plega o s'amaga mentre es fa scroll. A la Cronologia ha de ser **sempre visible** (és l'acció principal); si el comportament per defecte no ho permet, troba la manera d'aconseguir-ho.
2. **Cronologia buida:** proposta, mostrar només el botó «Nota» gran al centre. No dissenyat.
3. **Permís de micròfon denegat** i **error del reconeixedor**: no dissenyats.
4. **Nom de persones protegides al rellotge:** el disseny el mostra amb cadenat; confirma si s'ha d'ocultar.
5. **Haptic:** el sistema diu que Wear és fora d'abast. Proposta: `Confirm` en desar, respectant l'ajust del sistema i l'interruptor de l'app. Pendent d'aprovar.
6. **Política de privacitat i «Sense núvol»:** abans de dir res del rellotge a la ficha, revisa com viatgen realment les dades a la Data Layer i ajusta el text.
7. **Mida de la pàgina de «més entrades»** (proposta 20) i límit de memòria.
8. **Notes pendents amb el bloqueig actiu:** es queden al rellotge fins que s'entreguen (és el que l'usuari ha dictat). Confirma que és acceptable.
9. **Política de privacitat:** actualitza el text (esborrany al final).
11. **Persones protegides:** falta dissenyar una petició **ja caducada** oberta des de la notificació al mòbil, i què passa si es cancel·la al rellotge amb el prompt biomètric obert. Confirma que la sessió d'aprovació acaba en sortir de la persona.
10. **Wear OS i seguretat:** verifica que `isDeviceSecure` i la xifratge amb Keystore lligat a autenticació funcionen a l'API mínima que cobreixis. Si no, esborra la còpia local en comptes de xifrar-la i digue'm-ho.

## Ordre de feina suggerit

1. Mòdul `:wear`, manifest, tema i previsualitzacions amb dades fictícies de les 8 pantalles.
2. Protocol de sincronització mòbil → rellotge (instantani) i pantalles 1, 2, 7, 8 amb dades reals.
3. Dictat + Qui + Desat, i nota rellotge → mòbil amb cua, UUID i pantalles 5 i 6.
4. Proves: mòbil absent, rellotge reiniciat, nota duplicada, persona que passa a protegida, esborrada al mòbil amb notes pendents.
5. Bloqueig de l'app: comprovació de rellotge segur, esborrat de la còpia si es perd el bloqueig i pantalla 9.
6. Revisa les captures contra la teva versió real (Large i Small Round) i digue'm les diferències abans de tancar.

## Esborrany per a la política de privacitat (anglès; traduir a ca/es)

> Your data is stored only in the app's private storage on your devices: your phone and, if you use the Wear OS companion, your paired watch. The watch keeps only a small copy of the latest notes of each person (never of protected people, whose notes are shown only after you confirm with biometrics on your phone and are not stored), excluded from backups, and, when the app lock is enabled, requires the watch to be protected by its own screen lock. We do not collect, receive or share any of your data.

Abans de publicar-lo, verifica com viatja la Data Layer i ajusta la redacció si cal.
