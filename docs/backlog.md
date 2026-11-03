# Backlog: bugs i features pendents

Actualitzat el 3 de nov. de 2026. Només hi ha el que encara no està fet.

| # | Tipus | Tema | Issue |
| --- | --- | --- | --- |
| 6 | Feature (Iteració 2, Pro) | Pendents (follow-ups) | [#5](https://github.com/alorma/Converses/issues/5) |

El punt 6 és de l'**Iteració 2**, és Pro i el seu pla és a `docs/pro/` (fase 7 de `docs/pro/fases.md`, disseny del 02-11-2026 resumit al `README.md`). El #45 s'ha descartat i la cerca (#41) queda per més endavant. **Qualsevol canvi de la base de dades ha d'incloure una migració i el seu test** (la v1 ja és a Google Play). La base de dades és a la v5 (taules del 6 i del 8, ja fetes a la fase 5 del Pro). La numeració és la del backlog original, perquè els issues i les referències no canviïn; els punts resolts (1–5, 7, 8, 9, 10, 11, 12, 13, 15, 16, 17) i el 14 (descartat: es queda el split button del detall) són a l'historial de git.

---

## 6 · Feature (Pro): pendents

Issue: [#5](https://github.com/alorma/Converses/issues/5)

Poder marcar coses per fer-ne seguiment amb una persona ("li he de preguntar com li ha anat l'entrevista", "m'ha de tornar el llibre"...).

- És un estat d'una entrada (`isPending`, `doneAt`), no un quart tipus: «Qui» (Jo · {nom} · Apunt) no canvia.
- Pla, disseny i decisions: `docs/pro/` (fases 5 i 7).
- Decidit (03-11-2026): també es marca i desmarca des del full d'accions de l'entrada (fase 7).
- Més endavant, fora de les fases 6 i 7: recordatoris o notificacions en una data.

