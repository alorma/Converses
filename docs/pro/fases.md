# Converses Pro — Fases

Què és gratis i què és Pro, i la regla, són a `docs/pro/README.md`. Disseny de la pantalla de Pro i dels punts d'entrada: lliurament de Claude Design del 26-10-2026 (`converses-pro-2026-10-26.zip`, resumit a «Disseny» del `README.md`). Cada fase acaba amb alguna cosa que es pot provar; no es comença la següent sense validar l'anterior.

| Fase | Títol | Depèn de | Estat |
| --- | --- | --- | --- |
| 1 | Infraestructura: datasource, `ProRepository`, runner de flags Pro, debug drawer i etiqueta PRO | — | Feta i validada (26-10-2026) |
| 2 | RevenueCat: SDK, entitlement `pro`, preu, compra i restauració | 1 | Pendent |
| 3 | Paywall: pantalla «Converses Pro» (8a, 8b, 8c) i punts d'entrada | 2 | Pendent |
| 4 | Publicació: política de privacitat, seguretat de dades de Play, termes | 3 | Pendent |

Canals propis, Pendents i la personalització de l'avatar tindran els seus lliuraments de disseny i les seves fases.

---

Com es treballa cada fase (tasques, commits, validació, proposta d'inici): `.claude/rules/phased-work.md`.

---

## Model

Dues dimensions independents per a cada funció Pro:

| Flag (local; més endavant remot) | És Pro (`ProRepository`) | `FeatureState` |
| --- | --- | --- |
| Apagat | — | `Hidden`: no surt res |
| Encès | No | `Locked`: surt amb l'etiqueta PRO (més endavant porta al paywall) |
| Encès | Sí | `Unlocked`: funciona |

`isEnabled()` d'un flag Pro vol dir `Unlocked`. L'estat Pro surt d'un `ProDataSource`: a debug, un local que es commuta des del debug drawer; a release, fins a la fase 2, un que sempre diu que no.

## Fase 1 · Infraestructura

- [x] `docs/pro/fases.md` i `README.md` actualitzat
- [x] `ProDataSource` (local a DataStore a debug, «mai Pro» a release) i `ProRepository`; tests de JVM — `e681ae9`
- [x] `ProFeatureFlagRunner` i `FeatureState` (`Hidden`, `Locked`, `Unlocked`), `rememberFeatureState`; tests amb les quatre combinacions i el canvi en calent — `40e7a87`
- [x] Debug drawer: «Pro user» i «Turn on every Pro flag» (abans «Pro mode»), amb l'estat de cada flag; emulador — `fdd61f9`
- [x] Etiqueta PRO al `:designsystem` i Ajustos › Contactes en estat `Locked` (sense acció fins al paywall); captures i emulador — `4d8a82c`
- [x] L'editor i el detall segueixen els canvis del flag i del Pro amb la pantalla oberta (abans es llegien un cop); tests de JVM — `9deed39`
- [x] Editor, fila «Vincula un contacte» `Locked` (PRO, sense acció; el vincle desat es conserva); al detall no surt res; tests, captures i emulador — `7825f7a`
- [x] Editor, full «Avatar» `Locked`: el llapis es manté, el full porta PRO, cadenat a «Forma» i «Color» i opcions desactivades (es veu l'avatar desat); captures i emulador — `fedf1aa`
- [x] Ajustos, «Color de la persona» `Locked`: PRO i interruptor desactivat; captures i emulador — `c1837d2`
- [ ] Passar a Claude Design els estats `Locked` provisionals (fila de Contactes de l'editor, full «Avatar», «Color de la persona») per afinar-los

## Fase 2 · RevenueCat

RevenueCat (26-10-2026): entitlement `converses_pro`, producte `lifetime` (Test Store fet; Play Store pendent de pujar una build amb `BILLING` per poder-lo crear a Play Console), offering `default`. Claus a `local.properties` i secrets de GitHub: `REVENUECAT_TEST_API_KEY` (debug, Test Store) i `REVENUECAT_GOOGLE_API_KEY` (release). L'SDK va també a la release: la fitxa de Play deixa de poder dir «sense internet».

- [ ] SDK 10.29.0 i claus a `BuildConfig`; configurat anònim quan el fil principal queda lliure (al `onCreate` afegia ~400 ms a l'arrencada), i sense clau no es configura; manifest, builds i emulador
- [ ] `RevenueCatProDataSource`: listener de `CustomerInfo`, entitlement `converses_pro`, memòria cau sense connexió; ha de suportar que l'SDK encara no estigui configurat; tests de JVM
- [ ] Preu de l'offering, compra i restauració a `ProRepository`, amb els estats (pendent, error, cancel·lació, sense connexió, restaurar amb i sense compra); tests de JVM
- [ ] Reemborsament o revocació: es perd Pro i no s'esborren dades; test de JVM
- [ ] Debug: «Override Pro» (encès, mana «Pro user»; apagat, RevenueCat), estat de RevenueCat i compra i restauració de prova; emulador amb el Test Store
- [ ] Fitxa de Play (en, ca, es): treure «sense accés a internet» (text revisat per Bernat)
- [ ] `docs/pro/` al dia (entitlement `converses_pro`, override, secrets)

## Fase 3 · Paywall

- [ ] Pantalla «Converses Pro» (8a), des d'una funció (8b, paràmetre d'origen) i «Ja tens Pro» (8c)
- [ ] Targeta d'Ajustos i punts d'entrada `Locked` que porten al paywall, amb el seu origen. Ara no fan res: Ajustos › Contactes i la fila de Contactes de l'editor (`contactes`), el full «Avatar» (`avatar`) i Ajustos › «Color de la persona» (`avatar`, o el que digui el disseny)
- [ ] Textos ca, en i es (tauler 8d)

## Fase 4 · Publicació

- [ ] Política de privacitat (converses.app i alorma.github.io): RevenueCat rep un identificador anònim i les dades de compra, mai notes ni persones
- [ ] Formulari de seguretat de dades de Play
- [ ] Termes
