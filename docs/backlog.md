# Backlog: bugs i features pendents

Actualitzat el 8 d'oct. de 2026. Només hi ha el que encara no està fet.

| # | Tipus | Tema | Issue |
| --- | --- | --- | --- |
| 6 | Feature (Iteració 2) | Follow-ups / entrades pendents | [#5](https://github.com/alorma/Converses/issues/5) |
| 7 | Feature (Iteració 2) | Nou canal "Empresa" (queda dins el 8, com a sub-canal d'Altres) | — |
| 8 | Feature (Iteració 2, Pro) | Sub-canals amb nom i valor (dins dels 5 grups de canals) | — |
| 11 | Feature (Iteració 2) | Importar text a la nota ràpida (compartir, seleccionar text i enganxar) | [#30](https://github.com/alorma/Converses/issues/30) |
| 16 | Neteja | Treure l'avís dels canals quan la v1 sigui residual | — |

Els punts 6, 7, 8 i 11 són de l'**Iteració 2**; el 6 i el 8 són Pro, i l'11 és gratis (vegeu `docs/pro.md`). El #45 s'ha descartat i la cerca (#41) queda per més endavant (vegeu `docs/plan.md`). **Qualsevol canvi de la base de dades ha d'incloure una migració i el seu test** (la v1 ja és a Google Play); l'11 i el 16 no toquen Room. La v2 (canals en 5 grups) ja està feta; el 6 i el 8 necessitaran la v3. La numeració és la del backlog original, perquè els issues i les referències no canviïn; els punts resolts (1–5, 9, 10, 12, 13, 15) i el 14 (descartat: es queda el split button del detall) són a l'historial de git.

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
- Textos a `strings.xml` (en, ca, es) i actualitzar `docs/plan.md`.

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
- Sense canvis a Room, **no cal migració**. Textos nous a `values/`, `values-ca/` i `values-es/`; en acabar, afegir-ho a «Gratis per sempre» de `docs/pro.md` i actualitzar `docs/plan.md`.

---

## 16 · Neteja: treure l'avís dels canals

Quan la v1 sigui residual a la Play Console, treure l'avís de la migració dels canals: el banner (`NoticeBanner` i el seu ús a `PeopleListScreen`), `showChannelsNotice` i `onChannelsNoticeDismissed` al `PeopleListViewModel`, el flag `channelsNoticePending` d'`AppSettings`/`SettingsRepository`, el callback de `Migration1To2` i les strings `channels_notice_*`.

**La migració i el seu test no es treuen mai:** qualsevol usuari amb la BBDD a la v1 que obri una versió nova ha de passar per ella.
