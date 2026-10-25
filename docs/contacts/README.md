# Converses i els contactes del dispositiu — Pla

Actualitzat el 25 d'oct. de 2026 (fase 0 tancada) · @Bernat · **disseny pendent (C8).** Aquest document recull l'abast, l'arquitectura, les decisions i els riscos; les fases són a `docs/contacts/fases.md`. És una funció **Pro**: viu darrere de `ContactLinkFeature` (apagat), com la resta de `docs/pro/README.md`.

## Objectiu

Vincular una persona de Converses a un contacte del dispositiu i que aquest contacte, a l'app Contactes, mostri l'app **Converses** amb dues files: **«Veure notes»** (obre la persona) i **«Crear nota»** (obre la persona amb el full de nova entrada obert), com fan WhatsApp, Telegram o Signal.

## Premisses

1. **Funció Pro, darrere de flag.** `ContactLinkFeature` (`pro = true`), apagat per defecte; es prova amb el «Pro mode» del menú de debug. Amb el flag apagat no es veu res de la funció ni es demana cap permís.
2. **La font de veritat és Room.** El proveïdor de contactes només en rep una còpia mínima i reconstruïble; mai no s'hi llegeixen dades de Converses de tornada.
3. **Sense núvol.** La compte de Converses és local: no sincronitza res fora del mòbil.
4. **Res del que s'escriu a Contactes és privat:** qualsevol app amb `READ_CONTACTS` ho pot llegir. Per això només hi va un text fix i l'id intern de la persona, mai nom, notes ni comptadors.

## Abast

### Dins

- **Vincular** una persona a un contacte des de l'editor (Editar persona / Nova persona) amb el selector del sistema, i **desvincular-la**.
- **Al detall de la persona:** indicació «Vinculada a {nom del contacte}» i acció «Obre a Contactes». Res més del contacte (C2).
- **A l'app Contactes:** l'app «Converses» amb dues files fixes, «Veure notes» i «Crear nota» (C3, C9).
- Textos en en/ca/es.

### Fora

- **Persones protegides a Contactes** (C1): el vincle es desa, però no tenen files.
- Foto, telèfons, correus o accions de trucar o enviar missatges del contacte (C2).
- **People Directory** (`PeopleContract`, API 37.2): descartat de moment (C10).
- Avís de trucada entrant (`CallScreeningService`) i importació de diversos contactes alhora: són a «Més endavant» (fase 7).
- Enviar el vincle al rellotge, a Android Auto o a l'exportació de notes.

## Què va sortir de la fase 0

La prova de concepte és a la branca `poc/contacts-row` (no es fusiona mai), amb el detall a `docs/contacts/handoff.md` d'aquella branca. Es va provar als emuladors API 37.1 i 37.2 amb Google Contacts 4.85 i Telèfon 234 (l'app Contactes de l'AOSP no es va provar: els emuladors porten la de Google).

- **La via antiga funciona a la 37.1 i a la 37.2.** Google Contacts mostra una secció «Connected apps» amb la icona i el nom de l'app; desplegant-la, les nostres files. Cada fila té el seu mimetype i en tocar-la el sistema obre la nostra activitat amb el `content://com.android.contacts/data/N` de la fila.
- **Les files són només text:** Google Contacts no mostra l'`android:icon` de cada `ContactsDataKind` (només la icona de l'app a la capçalera). Les icones de Maps a la mateixa secció són una integració interna de Google, no una API.
- **El selector** (`PickContact`) obre el nou Contact Picker del sistema i dona id, lookup key i nom del contacte triat sense cap permís.
- **Lookup key:** canvia en agregar el nostre raw contact (`…7a7d` → `…7a7d.381r74-`), però l'antic continua resolent el mateix contacte amb `Contacts.lookupContact`.
- **Esborrar la compte** a Ajustos › Comptes: Ajustos avisa que se n'esborraran els contactes. El nostre raw contact no desapareix a l'instant, però sí més tard (després de reiniciar ja no hi era).
- **People Directory (37.2):** el sistema registra el directori i, després de `requestRefresh`, ens envia tots els telèfons i correus de l'agenda per omplir la seva caché; un client amb `MANAGE_CONTACTS` ens troba. Però **cap app del sistema el fa servir** (ni Google Contacts ni Telèfon, ni en una trucada entrant), i l'API és per a apps de comunicació (capacitats veu, vídeo i text).

### Política (verificada)

- `WRITE_CONTACTS` deixa de funcionar per a apps que apuntin a la «upcoming Android release» (2027, probablement API 38): [blog](https://developer.android.com/blog/posts/reliable-way-to-add-and-edit-contacts-android). **Depèn del targetSdk, no del mòbil:** «Apps targeting older versions keep their current behavior». Amb targetSdk 37 funciona també als mòbils amb l'API 38; quan es pugi el targetSdk, les escriptures fallen amb `SecurityException` (el blog no diu què passa amb les files ja escrites). El límit real el posa Play en obligar a pujar el targetSdk (data a verificar a la fase 6). El mateix blog recomana el People Directory per a les files «Call with…/Message with…» d'altres apps. `MANAGE_CONTACTS` no ens toca.
- `maxSdkVersion="37"` cobreix la 37.2 (`ro.build.version.sdk` hi continua sent 37).
- `READ_CONTACTS` amb targetSdk 37: declaració obligatòria a Play Console a partir del 27 de gener de 2027: [Play Help](https://support.google.com/googleplay/android-developer/answer/16935362). Cal verificar si `maxSdkVersion` n'eximeix abans de publicar (fase 6).
- Data safety: tot queda al mòbil, no compta com a «recollit».

## Decisions

| # | Decisió | Estat |
| --- | --- | --- |
| C1 | **Protegides:** no tenen files a Contactes. Protegir una persona vinculada n'esborra les files; desprotegir-la les torna a crear. | Decidit |
| C2 | **A Converses només es veu el vincle** (nom del contacte i «Obre a Contactes»); ni foto ni telèfons. | Decidit |
| C3 | **Text de les files fix:** «Veure notes» i «Crear nota» sota «Converses», sense cap dada de la persona ni de les notes. | Decidit |
| C4 | **Arxivades mantenen les files;** en tocar-les s'obre la persona arxivada. | Decidit |
| C5 | **Sense biblioteca de tercers.** S'ha descartat [contacts-android](https://github.com/vestrel00/contacts-android): només es publica a JitPack, té un sol mantenidor i no aporta el sync adapter ni `contacts.xml`, que són la part difícil. El poc de `ContactsContract` que cal queda tancat darrere de `ContactsGateway`. | Decidit |
| C6 | **Si l'usuari esborra la compte «Converses»:** en obrir l'app, si hi ha persones vinculades i la compte no hi és, es torna a crear i es refan les files. | Decidit |
| C7 | **Permisos** `READ_CONTACTS` i `WRITE_CONTACTS` (tots dos amb `maxSdkVersion="37"`): es demanen en vincular el primer contacte, després de l'explicació (C8). Si es deneguen, el vincle es desa igualment i no hi ha files. | Decidit |
| C8 | **Disseny** de la fila de l'editor, de la indicació al detall, de l'estat «contacte esborrat» i de l'explicació del permís (Claude Design). Encàrrec a la secció següent. | Obert |
| C9 | **Destí de les files:** «Veure notes» obre la fitxa de la persona; «Crear nota» obre la fitxa amb el full de nova entrada (`NewEntrySheet`) ja obert. Si la persona és protegida, primer el desbloqueig i després el mateix destí. | Decidit |
| C10 | **Via antiga per a tothom, sense People Directory.** Es torna a mirar quan surti l'API 38 i calgui pujar el targetSdk (aleshores `WRITE_CONTACTS` deixarà de funcionar). Descartada també la via d'una URL al contacte (Bernat no vol una URL visible). | Decidit |
| C11 | **Què es desa a Room:** `contactLookupKey` i l'id del contacte (per a `Contacts.getLookupUri(id, lookupKey)`); telèfons i correus no. Si es desa també `contactName` (per mostrar el vincle sense `READ_CONTACTS`) es decideix a l'inici de la fase 1. | Parcial |

## Encàrrec de disseny (C8)

Per a Claude Design. Context: Converses (Material 3 Expressive, color de la persona via `PersonTheme`). Una persona es pot vincular a un contacte del mòbil (funció Pro); el contacte mostra «Converses › Veure notes / Crear nota» a l'app Contactes.

1. **Editor de persona** (Editar / Nova persona): fila «Contacte» en tres estats: sense vincle (acció per triar-ne un amb el selector del sistema), vinculada (nom del contacte i acció per treure el vincle) i contacte esborrat.
2. **Detall de la persona:** indicació discreta «Vinculada a {nom}» amb l'acció «Obre a Contactes».
3. **Estat «contacte esborrat»** al detall: el contacte ja no existeix; opció de desvincular.
4. **Explicació del permís**, abans del diàleg del sistema, en vincular el primer contacte: per què es demana, que apareixerà la compte «Converses» a Ajustos › Comptes i que mai s'hi escriu el nom ni les notes. I què es veu si l'usuari diu que no (el vincle es desa, sense files a Contactes).

## Arquitectura

### Vincle (Room)

- Columnes noves nul·lables a `person`: `contactLookupKey`, l'id del contacte i, segons C11, `contactName`. **Room v2 → v3:** migració, esquema `3.json` exportat i test amb `MigrationTestHelper`.
- `null` vol dir sense vincle. Apagar el flag **no esborra el vincle** (mai no es perden dades de l'usuari); només s'amaga.
- Les columnes no surten a l'snapshot de Wear, a Auto ni a l'exportació.

### Triar el contacte

- `ActivityResultContracts.PickContact()`: el sistema dona accés temporal **només al contacte triat**, sense permís. En tornar, es llegeixen id, `LOOKUP_KEY` i `DISPLAY_NAME`.
- Per resoldre'l més tard: `Contacts.getLookupUri(id, lookupKey)` i `Contacts.lookupContact(...)`, que el troba encara que l'id canviï. Si ja no existeix, el detall diu que el contacte s'ha esborrat i ofereix desvincular-lo.

### Files a Contactes

1. **Compte pròpia:** `AbstractAccountAuthenticator` i un servei amb `android.accounts.AccountAuthenticator`; nom «Converses». El tipus de compte ha de ser diferent a debug i a release (deriva de l'`applicationId`, com a la prova: `com.alorma.contactnotes.dev.poc`), perquè les dues apps poden estar instal·lades alhora.
2. **Sync adapter** (`AbstractThreadedSyncAdapter`) amb `res/xml/syncadapter.xml` i `res/xml/contacts.xml`. No sincronitza res, però el sistema només llegeix `contacts.xml` d'una compte que en tingui.
3. **`contacts.xml`:** dos `ContactsDataKind`, un per fila: `vnd.android.cursor.item/vnd.com.alorma.converses.person` («Veure notes») i `…converses.note` («Crear nota»), amb `summaryColumn` i `detailColumn` per al text fix.
4. **Per cada persona vinculada i visible** (no protegida), un `RawContact` de la nostra compte amb **les dues files** (sense `StructuredName`, perquè el nom no arribi al proveïdor), l'id de la persona a `data1` de cada fila i a `RawContacts.SYNC1`, i una `AggregationExceptions` `TYPE_KEEP_TOGETHER` amb un raw contact del contacte triat.
5. **Obrir:** una activitat amb `ACTION_VIEW` per als dos mimetypes; llegeix el `personId` de la fila, comprova el flag i navega segons C9. Si la persona s'ha protegit entremig, passa per `PersonLockedRoute`. Si no existeix o el flag està apagat, obre l'app normal.

Després de crear les files es torna a llegir el lookup key (canvia en agregar) i s'actualitza a Room.

### Reconciliació

- `ContactsGateway` (interfície): llistar els raw contacts de la nostra compte, crear-ne un enganxat a un contacte, esborrar-ne, resoldre un lookup key i assegurar o treure la compte. Una implementació Android amb `ContentProviderOperation` i `CALLER_IS_SYNC_ADAPTER`; un `FakeContactsGateway` als tests.
- `ContactsReconciler.reconcile()`, **idempotent**: compara l'estat desitjat (persones amb vincle, no protegides, flag encès i permisos donats) amb el que hi ha al proveïdor, i en treu un pla de crear i esborrar. El pla és pur i es prova a la JVM.
- **Disparadors:** arrencada de l'app (inclou tornar a crear la compte, C6), desar una persona, protegir o desprotegir, esborrar, canviar el flag i tornar de concedir permisos. Amb el flag apagat, el pla ho esborra tot i treu la compte.
- Desinstal·lar l'app esborra la compte i les seves files.

### Manifest i permisos

- `READ_CONTACTS` i `WRITE_CONTACTS` amb `maxSdkVersion="37"` (C7, C10). `GET_ACCOUNTS` no cal per a la compte pròpia.
- Serveis de l'authenticator i del sync adapter, exportats amb els intent filters del sistema.
- Activitat de visualització amb `ACTION_VIEW` i els dos mimetypes.

## Riscos

| Risc | Com es detecta o s'evita |
| --- | --- |
| **Fuga de protegides** (C1) | Test de JVM del pla: una protegida mai no genera files; protegir-ne una amb files genera un esborrat. Comprovació a l'emulador. |
| **Fuga de contingut** a apps amb `READ_CONTACTS` | Només text fix i l'id de la persona; revisió del codi de `ContactsGateway` a la fase 5. |
| **Política de Play** per a permisos de contactes i Data safety | Verificar a la fase 6 si `maxSdkVersion` eximeix de la declaració; cap canvi a Play sense permís. |
| **`WRITE_CONTACTS` deixa de funcionar** amb el targetSdk de 2027 | C10: es revisa quan surti l'API 38, abans de pujar el targetSdk. |
| **Migració v2 → v3** trenca dades publicades | Test amb `MigrationTestHelper`; mai `fallbackToDestructiveMigration` a release. |
| **Lookup key caducat** o contacte esborrat | `lookupContact` i estat «contacte esborrat» al detall; test del pla amb contactes que ja no hi són. |
| La compte «Converses» a Ajustos sorprèn l'usuari | Explicació del permís (C8); si l'esborra, es torna a crear (C6). |

## Release notes

Darrere de flag: **cap línia** a les notes fins que el Pro existeixi.
