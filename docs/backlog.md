# Backlog: bugs i features pendents

Actualitzat el 14 d'oct. de 2026. Només hi ha el que encara no està fet.

| # | Tipus | Tema | Issue |
| --- | --- | --- | --- |
| 6 | Feature (Iteració 2) | Follow-ups / entrades pendents | [#5](https://github.com/alorma/Converses/issues/5) |
| 7 | Feature (Iteració 2) | Nou canal "Empresa" (queda dins el 8, com a sub-canal d'Altres) | — |
| 8 | Feature (Iteració 2, Pro) | Sub-canals amb nom i valor (dins dels 5 grups de canals) | — |

Els punts 6, 7 i 8 són de l'**Iteració 2**; el 6 i el 8 són Pro (vegeu `docs/pro/`). El #45 s'ha descartat i la cerca (#41) queda per més endavant. **Qualsevol canvi de la base de dades ha d'incloure una migració i el seu test** (la v1 ja és a Google Play). La v2 (canals en 5 grups) ja està feta; el 6 i el 8 necessitaran la v3. La numeració és la del backlog original, perquè els issues i les referències no canviïn; els punts resolts (1–5, 9, 10, 11, 12, 13, 15, 16, 17) i el 14 (descartat: es queda el split button del detall) són a l'historial de git.

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
