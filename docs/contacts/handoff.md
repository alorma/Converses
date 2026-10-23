# Contactes — handoff de la fase 0 (23 d'oct. de 2026)

Estat de la investigació per continuar en una sessió nova. El pla és a `docs/contacts/README.md` i `fases.md` (a `main`, commit `587db00`); **aquest document el contradiu en diversos punts**: quan es tanqui la fase 0, cal reescriure el `README.md` amb el que hi ha aquí.

## On és cada cosa

- **Branca `poc/contacts-row`** (worktree `../Converses-poc-contacts`): prova de concepte només a `app/src/debug`. **No es fusiona mai a `main`.**
  - `debug/contacts/ContactsPocActivity.kt`: accions `legacy`, `directory` i `clear` (vegeu la capçalera del fitxer per a les ordres `adb`).
  - `debug/contacts/PocAccount.kt`: authenticator i sync adapter buit (via antiga).
  - `debug/contacts/PocPeopleDirectoryProvider.kt`: People Directory (via nova), amb les constants de `PeopleContract` copiades (el `compileSdk` continua sent 37).
  - `res/xml/poc_*.xml` i `AndroidManifest.xml` de debug.
- **`ConversesLanding`** (repo apart, GitHub Pages, `converses.app`): `/.well-known/assetlinks.json` publicat amb `com.alorma.contactnotes` (clau de signatura de Play `64:24:A7…` i clau de pujada `DA:96:3E…`) i `com.alorma.contactnotes.dev` (debug `ED:05:68…`). Comprovat amb l'API de Digital Asset Links i `pm get-app-links` (`verified`). El push per SSH no funciona des d'aquesta xarxa: es fa per HTTPS amb `gh auth git-credential`.
- **Emuladors:** `37_Pixel_Phone` (API 37.1, `emulator-5554`, compartit amb Caducity: demanar-lo a la seva sessió) i `converses_37_2` (API 37.2, nou, 8 GB, `emulator-5556`). Tots dos amb el compte de Google de Bernat.
- **Contactes de prova** (al compte de Google): Marta Soler, Jordi Puig, Laia Ferrer, Pau Vidal (persones 2–5 de l'app de debug a la 37.1). Luna Roja (persona 1) és protegida i no té contacte. **Hi he afegit telèfons `+3460000000N` i correus `nom.cognom@example.com`** (per al People Directory): cal treure'ls en acabar.

## Política (verificat a les fonts oficials)

- `WRITE_CONTACTS` deixa de funcionar per a apps que apuntin a la «upcoming Android release» (2027; probablement API 38): [blog](https://developer.android.com/blog/posts/reliable-way-to-add-and-edit-contacts-android). `MANAGE_CONTACTS` no ens toca («adding call or message actions: No»). Es pot mantenir `WRITE_CONTACTS` amb `maxSdkVersion`.
- `READ_CONTACTS` amb targetSdk 37: declaració obligatòria a Play Console a partir del 27 de gener de 2027: [Play Help](https://support.google.com/googleplay/android-developer/answer/16935362). No s'ha verificat si `maxSdkVersion` n'eximeix.
- Data safety: tot queda al mòbil, no compta com a «recollit».

## Què s'ha provat i què ha sortit

| Prova | Emulador | Resultat |
| --- | --- | --- |
| `PickContact` (`ACTION_PICK`) sense permís | 37.1 | Obre el nou Contact Picker del sistema. Dona id, lookup key i nom; **no** telèfons ni correus (`SecurityException`). |
| `ACTION_PICK_CONTACTS` amb `EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS` (telèfon, correu) | 37.2 | **Funciona sense permís:** retorna telèfon, correu i lookup key del contacte triat. |
| Via B: `ACTION_EDIT` + `Insert.DATA` amb un lloc web | 37.1, Google Contacts | Funciona i l'enllaç `https://converses.app/person/{id}` obre l'app (App Link verificat). L'esquema propi obre Chrome. **Descartat per Bernat: no volem una URL visible al contacte.** Les dues files de prova ja s'han esborrat. |
| People Directory | 37.2 | El sistema **registra** el directori (`dumpsys activity provider …PeopleProvider`: «Total registered directories: 1»), però Google Contacts **no fa cap consulta**: forçant `cmd jobscheduler run -f com.android.providers.contacts 201` surt «0 targeted queries». El nostre provider no rep cap `query`. |
| Via antiga (compte + sync adapter + mimetype propi) | 37.1 | **Pendent:** el codi està escrit i compila; no s'ha arribat a provar (el picker es va quedar obert). Ordre: `--el personId 2 --es action legacy`, després de `pm grant` de `READ_CONTACTS` i `WRITE_CONTACTS`. |

## Decisió de Bernat (vigent)

- **Res d'URL** al contacte.
- **Fins a l'API 37.1:** via antiga (compte pròpia, sync adapter, `contacts.xml`, `RawContact` amb mimetype propi, `AggregationExceptions` pel lookup key), amb `READ_CONTACTS` i `WRITE_CONTACTS` amb `maxSdkVersion="37"`.
- **API 37.2 o superior:** People Directory, o una alternativa si no surt enlloc.
- Decisions anteriors que es mantenen: protegides mai a Contactes (C1), text fix a la fila (C3), arxivades mantenen la fila (C4), sense biblioteca de tercers (C5).
- **C2 canvia:** per al People Directory cal desar a Room telèfons i correus del contacte (els dona el picker nou).

## Pendent per tancar la fase 0

1. Provar la via antiga a la 37.1 (fila «Converses · Veure notes», obrir-la, lookup key abans i després d'agregar, què passa si s'esborra la compte a Ajustos › Comptes: C6).
2. People Directory: esbrinar quin client el fa servir (Google Contacts, Telèfon, trucada entrant?) i com es veu. Opció: un client de prova amb `MANAGE_CONTACTS` (accés especial) que consulti `Persons.CONTENT_URI` per telèfon, per veure si el sistema ens pregunta i si `createPeopleInteractionRequest` obre `ContactsPocInteractionActivity`. Si cap app del sistema el mostra, decidir amb Bernat què es fa a la 37.2 o superior (la via antiga encara funciona amb targetSdk 37).
3. Decidir C6, C7 (quan es demanen els permisos) i què es desa a Room.
4. Encàrrec de disseny a Claude Design (C8).
5. Reescriure `docs/contacts/README.md` i `fases.md` amb tot això i tancar la fase amb Bernat.
6. Netejar: telèfons i correus de prova dels contactes, `clear` de la prova de concepte, i esborrar el worktree quan ja no calgui.

## Notes de procés

- Bernat no vol que s'editin fitxers amb scripts (`perl`, `sed`): sempre Edit/Write.
- Abans de fer servir `emulator-5554`, avisar la sessió de Caducity.
