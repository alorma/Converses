# Backlog: bugs i features pendents

Actualitzat el 2 de nov. de 2026. Només hi ha el que encara no està fet.

| # | Tipus | Tema | Issue |
| --- | --- | --- | --- |
| 6 | Feature (Iteració 2, Pro) | Pendents (follow-ups) | [#5](https://github.com/alorma/Converses/issues/5) |
| 7 | Feature (Iteració 2) | Nou canal "Empresa" (queda dins el 8, com a canal propi d'Altres) | — |
| 8 | Feature (Iteració 2, Pro) | Canals propis (dins dels 5 grups de canals) | — |

Els punts 6, 7 i 8 són de l'**Iteració 2**; el 6 i el 8 són Pro i el seu pla és a `docs/pro/` (fases 5–7 de `docs/pro/fases.md`, disseny del 02-11-2026 resumit al `README.md`). El #45 s'ha descartat i la cerca (#41) queda per més endavant. **Qualsevol canvi de la base de dades ha d'incloure una migració i el seu test** (la v1 ja és a Google Play). La base de dades és a la v4; el 6 i el 8 van junts a la v5. La numeració és la del backlog original, perquè els issues i les referències no canviïn; els punts resolts (1–5, 9, 10, 11, 12, 13, 15, 16, 17) i el 14 (descartat: es queda el split button del detall) són a l'historial de git.

---

## 6 · Feature (Pro): pendents

Issue: [#5](https://github.com/alorma/Converses/issues/5)

Poder marcar coses per fer-ne seguiment amb una persona ("li he de preguntar com li ha anat l'entrevista", "m'ha de tornar el llibre"...).

- És un estat d'una entrada (`isPending`, `doneAt`), no un quart tipus: «Qui» (Jo · {nom} · Apunt) no canvia.
- Pla, disseny i decisions: `docs/pro/` (fases 5 i 7).
- Decidit (03-11-2026): també es marca i desmarca des del full d'accions de l'entrada (fase 7).
- Més endavant, fora de les fases 6 i 7: recordatoris o notificacions en una data.

## 7 · Feature: nou canal "Empresa" (entrevistes de feina)

**Absorbit pel 8.** Ja no és un valor de l'enum: serà un canal propi d'«Altres» creat per l'usuari (Pro). No es fa apart.

## 8 · Feature (Pro): canals propis

Es construeix sobre els 5 grups de canals (ja fets). Dins de qualsevol grup, inclòs «Altres», l'usuari crea **canals propis**: globals (no per persona), amb **nom únic dins del grup** (sense distingir majúscules) i **sense valor**. A l'entrada són opcionals.

- **Gratis:** els 5 grups. **Pro:** els canals propis, de qualsevol grup.
- Sense icones pròpies: cada canal usa la del seu grup.
- Si s'esborra un canal, les seves entrades queden al grup (no es perd res); si canvia de grup, les entrades s'hi mouen.
- Els canals de la beta (WhatsApp, Discord…) no es poden recuperar: la migració 1→2 els va esborrar.
- Pla, disseny i decisions: `docs/pro/` (fases 5 i 6).
- **Estat (03-11-2026):** fet a la fase 6, darrere de `CustomChannelsFeature`; pendent de la validació de Bernat per tancar la fase.
