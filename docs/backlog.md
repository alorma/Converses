# Backlog: bugs i features pendents

Actualitzat el 4 de nov. de 2026. Només hi ha el que encara no està fet.

| # | Tipus | Tema | Issue |
| --- | --- | --- | --- |
| 6 | Feature (Iteració 2, Pro) | Pendents (follow-ups) | [#5](https://github.com/alorma/Converses/issues/5) |
| 18 | Bug (Pro, abans de producció) | El paywall es penja si RevenueCat no pot enviar la compra | — |

El punt 6 és de l'**Iteració 2**, és Pro i el seu pla és a `docs/pro/` (fase 7 de `docs/pro/fases.md`, disseny del 02-11-2026 resumit al `README.md`). El #45 s'ha descartat i la cerca (#41) queda per més endavant. **Qualsevol canvi de la base de dades ha d'incloure una migració i el seu test** (la v1 ja és a Google Play). La base de dades és a la v5 (taules del 6 i del 8, ja fetes a la fase 5 del Pro). La numeració és la del backlog original, perquè els issues i les referències no canviïn; els punts resolts (1–5, 7, 8, 9, 10, 11, 12, 13, 15, 16, 17) i el 14 (descartat: es queda el split button del detall) són a l'historial de git.

---

## 6 · Feature (Pro): pendents

Issue: [#5](https://github.com/alorma/Converses/issues/5)

Poder marcar coses per fer-ne seguiment amb una persona ("li he de preguntar com li ha anat l'entrevista", "m'ha de tornar el llibre"...).

- Redissenyat el 03-11-2026 com a **tasques**: taula pròpia `task`, formulari propi i fora de la cronologia; «Qui» (Jo · {nom} · Apunt) no canvia.
- Pla, disseny i decisions: `docs/pro/` (fases 5 i 7). Fet a la fase 7 (formulari, detall i llistat), validada el 03-11-2026.
- Més endavant: «Avisa'm» (notificació a les 9:00; la columna `remind` ja hi és).

## 18 · Bug (Pro): el paywall es penja si RevenueCat no pot enviar la compra

Vist el 04-11-2026 amb la compra real de la 1.8.2 (track intern, Pixel 8a). Play cobra, però RevenueCat no pot enviar el token al seu servidor (`Unable to start a network connection due to a network configuration issue: timeout`; Android tenia la xarxa de l'app a `blocked=APP_BACKGROUND` mentre el full de Play era a sobre). `awaitPurchase` no torna, el paywall es queda amb els botons desactivats i els intents següents (també «Restaura la compra») fallen amb `OperationAlreadyInProgressError`. La compra no es perd: en tornar a obrir l'app, RevenueCat l'envia i dona Pro.

- Cal que el paywall no es quedi esperant per sempre: per exemple, un temps màxim que passi a «Compra pendent» (es pot tancar) i que Pro s'encengui sol quan arribi el `CustomerInfo`.
- S'ha d'arreglar abans de la release de producció que obri Pro.

**Estat (04-11-2026): fet, pendent de la validació de Bernat.**

- [x] `OperationAlreadyInProgressError` és «pendent» (compra i restauració), no «error» — `a5f7213`
- [x] El paywall espera com a molt 15 s des que torna a primer pla (en sortir del full de Play): després reactiva els botons i diu «La compra està pendent…»; si Pro ja ha arribat pel listener, no diu res. La compra no es cancel·la: si RevenueCat respon tard, Pro s'encén o surt l'error — `6a9a4cb`
- Validat amb tests de JVM (temps virtual) i a l'emulador amb el Test Store: restaurar sense compra, cancel·lació, compra fallida, compra vàlida, i el temporitzador (sortir i tornar amb el full obert). El full del Test Store és un diàleg dins l'activitat i no la pausa; el de Play sí, i és el cas real. El penjament real no s'ha pogut reproduir (sense xarxa, RevenueCat falla de seguida amb `NetworkError`).

