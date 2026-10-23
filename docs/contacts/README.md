# Converses i els contactes del dispositiu — Pla

Esborrany del 23 d'oct. de 2026 · @Bernat · **encara no hi ha disseny.** Aquest document recull l'abast, l'arquitectura, les decisions i els riscos; les fases són a `docs/contacts/fases.md`. És una funció **Pro**: viu darrere de `ContactLinkFeature` (apagat), com la resta de `docs/pro/README.md`.

## Objectiu

Vincular una persona de Converses a un contacte del dispositiu i que aquest contacte, a l'app Contactes (AOSP o Google Contacts), mostri una fila **«Converses · Veure notes»** que obre la persona, com fan WhatsApp, Telegram o Signal.

## Premisses

1. **Funció Pro, darrere de flag.** `ContactLinkFeature` (`pro = true`), apagat per defecte; es prova amb el «Pro mode» del menú de debug. Amb el flag apagat no es veu res de la funció ni es demana cap permís.
2. **La font de veritat és Room.** El proveïdor de contactes només en rep una còpia mínima i reconstruïble; mai no s'hi llegeixen dades de Converses de tornada.
3. **Sense núvol.** La compte de Converses és local: no sincronitza res fora del mòbil.
4. **Res del que s'escriu a Contactes és privat:** qualsevol app amb `READ_CONTACTS` ho pot llegir. Per això només hi va un text fix i l'id intern de la persona, mai nom, notes ni comptadors.

## Abast

### Dins

- **Vincular** una persona a un contacte des de l'editor (Editar persona / Nova persona) amb el selector del sistema, i **desvincular-la**.
- **Al detall de la persona:** indicació «Vinculada a {nom del contacte}» i acció «Obre a Contactes». Res més del contacte (decisió C2).
- **A l'app Contactes:** fila fixa «Converses» / «Veure notes» al contacte vinculat; en tocar-la s'obre la persona a Converses.
- Textos en en/ca/es.

### Fora

- **Persones protegides a Contactes** (decisió C1): el vincle es desa, però no tenen fila.
- Foto, telèfons, correus o accions de trucar o enviar missatges del contacte (C2).
- Avís de trucada entrant (`CallScreeningService`) i importació de diversos contactes alhora: són a «Més endavant» (fase 7), cadascun amb la seva decisió.
- Enviar el vincle al rellotge, a Android Auto o a l'exportació de notes.

## Decisions

| # | Decisió | Estat |
| --- | --- | --- |
| C1 | **Protegides:** no tenen fila a Contactes. Protegir una persona vinculada n'esborra la fila; desprotegir-la la torna a crear. | Decidit |
| C2 | **A Converses només es veu el vincle** (nom del contacte i «Obre a Contactes»); ni foto ni telèfons. | Decidit |
| C3 | **Text de la fila fix:** «Converses» / «Veure notes», sense cap dada de la persona ni de les notes. | Decidit |
| C4 | **Arxivades mantenen la fila;** en tocar-la s'obre la persona arxivada. | Decidit |
| C5 | **Sense biblioteca de tercers.** S'ha descartat [contacts-android](https://github.com/vestrel00/contacts-android): només es publica a JitPack, té un sol mantenidor i no aporta el sync adapter ni `contacts.xml`, que són la part difícil. El poc de `ContactsContract` que cal queda tancat darrere de `ContactsGateway`. Es pot tornar a valorar per a la importació de diversos contactes. | Decidit |
| C6 | **Si l'usuari esborra la compte «Converses»** a Ajustos › Comptes: es torna a crear sola o s'entén que ja no vol les files (i es desactiva la integració fins que la torni a activar)? | Obert (fase 0) |
| C7 | **Quan es demanen els permisos** `READ_CONTACTS` i `WRITE_CONTACTS`: en vincular el primer contacte (proposta) o en un interruptor a Ajustos. I què es mostra si es deneguen. | Obert (fase 0) |
| C8 | **Disseny** de la fila de l'editor, de la indicació al detall i de l'explicació del permís (Claude Design). | Obert (fase 0) |

## Arquitectura (proposta)

### Vincle (Room)

- Nova columna nul·lable `contactLookupKey` a `person` (i, si la fase 0 ho confirma, `contactName` com a còpia del nom triat, per mostrar-lo sense `READ_CONTACTS`). **Room v2 → v3:** migració, esquema `3.json` exportat i test amb `MigrationTestHelper`.
- `null` vol dir sense vincle. Apagar el flag **no esborra el vincle** (mai no es perden dades de l'usuari); només s'amaga.
- La columna no surt a l'snapshot de Wear, a Auto ni a l'exportació.

### Triar el contacte

- `ActivityResultContracts.PickContact()`: el sistema dona accés temporal **només al contacte triat**, sense permís. En tornar, es llegeixen `LOOKUP_KEY` i `DISPLAY_NAME` i es desen.
- Per resoldre'l més tard: `Contacts.getLookupUri(id, lookupKey)` i `Contacts.lookupContact(...)`, que el troba encara que l'id canviï. Si ja no existeix, el detall diu que el contacte s'ha esborrat i ofereix desvincular-lo.

### Fila a Contactes

Perquè l'app Contactes mostri una fila d'una altra app calen les mateixes peces que fa servir WhatsApp:

1. **Compte pròpia:** `AbstractAccountAuthenticator` i un servei amb `android.accounts.AccountAuthenticator`; tipus de compte `com.alorma.converses`, nom «Converses».
2. **Sync adapter** (`AbstractThreadedSyncAdapter`) amb `res/xml/syncadapter.xml` i `res/xml/contacts.xml`. No sincronitza res (no hi ha servidor), però el sistema només llegeix `contacts.xml` d'una compte que en tingui.
3. **`contacts.xml`:** un `ContactsDataKind` amb mimetype `vnd.android.cursor.item/vnd.com.alorma.converses.person`, `summaryColumn` i `detailColumn` per al text fix i la icona de l'app.
4. **Per cada persona vinculada i visible** (no protegida), un `RawContact` de la nostra compte amb **una sola fila** d'aquest mimetype (sense `StructuredName`, perquè el nom no arribi al proveïdor), i una `AggregationExceptions` `TYPE_KEEP_TOGETHER` amb un raw contact del contacte triat. L'id de la persona va a `RawContacts.SYNC1`, no a `SOURCE_ID` (que canviaria el lookup key del contacte).
5. **Obrir:** una activitat amb `ACTION_VIEW` i el mimetype propi; llegeix `SYNC1`, comprova el flag i navega a `PersonRoute(id)`. Si la persona s'ha protegit entremig, `PersonLockedRoute(id)`. Si no existeix o el flag està apagat, obre l'app normal.

Agregar el nostre raw contact canvia el lookup key del contacte. `lookupContact` ho tolera, però després de crear la fila es torna a llegir el lookup key i s'actualitza a Room.

### Reconciliació

- `ContactsGateway` (interfície): llistar els raw contacts de la nostra compte, crear-ne un enganxat a un contacte, esborrar-ne, resoldre un lookup key i assegurar o treure la compte. Una implementació Android amb `ContentProviderOperation` i `CALLER_IS_SYNC_ADAPTER`; un `FakeContactsGateway` als tests.
- `ContactsReconciler.reconcile()`, **idempotent**: compara l'estat desitjat (persones amb vincle, no protegides, flag encès i permisos donats) amb el que hi ha al proveïdor, i en treu un pla de crear i esborrar. El pla és pur i es prova a la JVM.
- **Disparadors:** arrencada de l'app, desar una persona (vincle o desvincle), protegir o desprotegir, esborrar, canviar el flag i tornar de concedir permisos. Amb el flag apagat, el pla ho esborra tot i treu la compte, perquè no quedi «Converses» a Ajustos › Comptes.
- Desinstal·lar l'app esborra la compte i les seves files: no cal fer res.

### Manifest i permisos

- `READ_CONTACTS` i `WRITE_CONTACTS` (C7). `GET_ACCOUNTS` no cal per a la compte pròpia.
- Serveis de l'authenticator i del sync adapter, exportats amb els intent filters del sistema (`android.accounts.AccountAuthenticator`, `android.content.SyncAdapter`); a la fase 0 es revisa com protegir-los.
- Activitat de visualització amb `ACTION_VIEW` i el mimetype propi.

## Riscos

| Risc | Com es detecta o s'evita |
| --- | --- |
| **Fuga de protegides** (C1) | Test de JVM del pla: una protegida mai no genera una fila; protegir-ne una amb fila genera un esborrat. Comprovació a l'emulador. |
| **Fuga de contingut** a apps amb `READ_CONTACTS` | Només text fix i `SYNC1`; revisió del codi de `ContactsGateway` a la fase 6. |
| **Política de Play** per a permisos de contactes i Data safety | Fase 0: llegir la política vigent i decidir què es declara (les dades no surten del mòbil). Cap canvi a Play sense permís. |
| **Google Contacts** no mostra la fila com l'AOSP | Fase 0: prova a l'emulador amb les dues apps abans d'escriure codi de producte. |
| **Migració v2 → v3** trenca dades publicades | Test amb `MigrationTestHelper`; mai `fallbackToDestructiveMigration` a release. |
| **Lookup key caducat** o contacte esborrat | `lookupContact` i estat «contacte esborrat» al detall; test del pla amb contactes que ja no hi són. |
| La compte «Converses» a Ajustos sorprèn l'usuari | Decisió C6 i text de l'explicació del permís (C8). |

## Release notes

Darrere de flag: **cap línia** a les notes fins que el Pro existeixi.
