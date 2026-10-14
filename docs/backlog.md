# Backlog: bugs i features pendents

Actualitzat el 14 d'oct. de 2026. Només hi ha el que encara no està fet.

| # | Tipus | Tema | Issue |
| --- | --- | --- | --- |
| 6 | Feature (Iteració 2) | Follow-ups / entrades pendents | [#5](https://github.com/alorma/Converses/issues/5) |
| 7 | Feature (Iteració 2) | Nou canal "Empresa" (queda dins el 8, com a sub-canal d'Altres) | — |
| 8 | Feature (Iteració 2, Pro) | Sub-canals amb nom i valor (dins dels 5 grups de canals) | — |
| 11 | Feature (Iteració 2) | Importar text a la nota ràpida (compartir, seleccionar text i enganxar) | [#30](https://github.com/alorma/Converses/issues/30) |
| 16 | Neteja | Treure l'avís dels canals quan la v1 sigui residual | — |

Els punts 6, 7, 8 i 11 són de l'**Iteració 2**; el 6 i el 8 són Pro, i l'11 és gratis (vegeu `docs/pro/`). El #45 s'ha descartat i la cerca (#41) queda per més endavant. **Qualsevol canvi de la base de dades ha d'incloure una migració i el seu test** (la v1 ja és a Google Play); l'11 i el 16 no toquen Room. La v2 (canals en 5 grups) ja està feta; el 6 i el 8 necessitaran la v3. La numeració és la del backlog original, perquè els issues i les referències no canviïn; els punts resolts (1–5, 9, 10, 12, 13, 15, 17) i el 14 (descartat: es queda el split button del detall) són a l'historial de git.

---

## 6 · Feature: follow-ups / entrades pendents a la cronologia

Issue: [#5](https://github.com/alorma/Converses/issues/5)

Poder marcar coses per fer-ne seguiment amb una persona ("li he de preguntar com li ha anat l'entrevista", "m'ha de tornar el llibre"...).

**Decisió recomanada:** un flag `pending` a `EntryEntity` (i, opcionalment, una data de recordatori), no un quart tipus. «Qui» (Jo · {nom} · Apunt) és un grup de 3 botons que respon a «qui»; un «Follow-up» no hi encaixa, i amb el flag qualsevol entrada pot quedar pendent.

**Què cal fer**
- Camp `pending` a `EntryEntity` i **migració de Room** (versió 1 → 2) amb el seu test (`MigrationTestHelper`).
- Un interruptor o xip «Pendent» a `EntryForm`, a sota de «Quan», que és el formulari comú de la nota ràpida, la nova entrada i l'edició.
- Indicador visual a la cronologia, i potser un comptador o filtre de pendents al detall i a la llista.
- Marcar i desmarcar des del full d'accions de l'entrada.
- Opcional: recordatori o notificació en una data.
- Textos a `strings.xml` (en, ca, es).

## 7 · Feature: nou canal "Empresa" (entrevistes de feina)

**Absorbit pel 8.** Ja no és un valor de l'enum: serà un sub-canal d'«Altres» creat per l'usuari (Pro). No es fa apart.

## 8 · Feature (Pro): sub-canals

Es construeix sobre els 5 grups de canals (ja fets). Dins de qualsevol grup, inclòs «Altres», l'usuari pot crear **sub-canals** amb un **nom lliure** i un **valor opcional** (número, usuari, enllaç).

- **Gratis:** els 5 grups. **Pro:** els sub-canals, de qualsevol grup.
- **Nom únic per persona**, sense distingir majúscules: «WhatsApp» dues vegades és un error; «WhatsApp personal» i «WhatsApp feina» són vàlids. Validació a l'editor, sense índex a Room.
- Taula nova a Room (persona, grup, nom, valor opcional) amb **migració i test**.
- Sense icones pròpies de moment: cada sub-canal usa la del seu grup.
- Si s'esborra un sub-canal, les entrades que hi apuntaven passen al grup (no es perd res).
- Afecta: detall de persona, `EntryForm` («On»), editor de persona (ara sense selecció de canals), filtres (sub-xips en seleccionar un grup) i `PersonExporter.kt`.
- Pregunta oberta: valor només informatiu o accionable (obrir WhatsApp, trucar, copiar). Si és accionable, mirar privadesa i permisos.
- Les persones protegides no han de mostrar sub-canals ni valors abans de desbloquejar.

---

## 11 · Feature: importar text a la nota ràpida

Issue: [#30](https://github.com/alorma/Converses/issues/30) (el detall complet, el comportament i els criteris d'acceptació són a la issue).

Portar text d'altres apps a la nota ràpida amb el mínim de passos: el text ja hi és posat, l'usuari tria la persona i desa. WhatsApp, Telegram i companyia no comparteixen els missatges com a text (només deixen copiar), per això hi ha tres vies:

- **A. Compartir:** `ACTION_SEND` + `text/plain` (navegador, correu, notes, X, Reddit…).
- **B. Seleccionar text:** `ACTION_PROCESS_TEXT` (Converses surt al menú de selecció).
- **C. Enganxar:** botó «Enganxa» a la nota ràpida, visible quan el porta-retalls té text; el contingut només es llegeix quan es prem.

**Què cal fer**
- `intent-filter`s `SEND` i `PROCESS_TEXT` a `MainActivity`; `Intent.toBackStack()` com a deep link de Navigation 3 i `QuickNoteRoute(sharedText)`.
- Sense persones, «Crea la primera persona» obre l'editor i el text es manté en tornar.
- «Qui» per defecte a la persona (`HEARD`); provisionalment, un enllaç sol va a `HAPPENED` (pregunta oberta de la issue).
- **Privacitat:** el bloqueig de l'app surt abans de la nota ràpida; una persona protegida demana l'empremta i la pantalla té `FLAG_SECURE`.
- Fora d'abast: imatges i fitxers (`SEND_MULTIPLE`), Direct Share i xats exportats.
- Sense canvis a Room, **no cal migració**. Textos nous a `values/`, `values-ca/` i `values-es/`; en acabar, afegir-ho a «Gratis per sempre» de `docs/pro/README.md`.

---

## 16 · Neteja: treure l'avís dels canals

Quan la v1 sigui residual a la Play Console, treure l'avís de la migració dels canals: el banner (`NoticeBanner` i el seu ús a `PeopleListScreen`), `showChannelsNotice` i `onChannelsNoticeDismissed` al `PeopleListViewModel`, el flag `channelsNoticePending` d'`AppSettings`/`SettingsRepository`, el callback de `Migration1To2` i les strings `channels_notice_*`.

**La migració i el seu test no es treuen mai:** qualsevol usuari amb la BBDD a la v1 que obri una versió nova ha de passar per ella.

## 18 · Wear OS: pendent després de la 1.ª versió

El pla de Wear (fases 0–11) es va tancar el 13 d'oct. de 2026 i `docs/wear/` s'ha esborrat (queda a l'historial de git). Pendent:

- **Beta en revisió:** la versió amb el rellotge és a `beta` esperant la revisió de Play. Quan s'aprovi, passar-la a producció.
- **Notes de versió:** proposar els punts visibles de les fases 10 i 11 (rellotge, instal·lació creuada); no tocar els fitxers sense acord.
- **Rellotge real** (Xiaomi Watch 2, quan s'engegui):
  - Clau Keystore: la còpia és il·legible amb el rellotge bloquejat i la clau desapareix en treure el PIN.
  - Biometria real a l'aprovació d'una protegida, i dictat + nota a una protegida de punta a punta.
  - Català al reconeixedor de veu; «Sense mòbil» real; desconnexió i reconnexió amb notes a la cua.
  - Release amb R8: dictat, entrega d'una nota i aprovació.
  - TalkBack: la fila d'una persona protegida diu «Persona protegida» i no repeteix les inicials.
  - Bateria i latència de la sincronització; ambient (l'app hauria de desaparèixer sense deixar contingut).
  - Pantalla de Play al rellotge i instal·lació completa (instal·lació creuada); el botó «Obre al mòbil» es parteix en dues línies a Large Round i al Xiaomi (límit de l'`EdgeButton`).
  - Referències de captura de la fila protegida i de `approvalConfirm` (etiqueta `update-screenshots` a la PR).
- **Més endavant:** tile i complicació de nota ràpida (sense dissenyar), respostes predefinides o triar el canal des del rellotge, notificacions des del mòbil (sense contingut sensible) i rellotge autònom amb base de dades pròpia.

**Decisions que cal recordar** (eren al pla):
- Les protegides no es desen al rellotge: es mostren només en memòria i només amb una aprovació biomètrica al mòbil; les notes a protegides no passen per la cua.
- La còpia local del rellotge és xifrada amb Keystore i s'esborra si es treu el bloqueig; la cua de notes s'esborra igual (les notes es perden, no es xifren).
- El rellotge publica a la pista `wear:<track>` i `:wear` té el `versionCode` amb offset 2 (`FormFactor.WEAR`).
