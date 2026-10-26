# Converses Pro — Fases

Què és gratis i què és Pro, i la regla, són a `docs/pro/README.md`. Disseny de la pantalla de Pro i dels punts d'entrada: lliurament de Claude Design del 26-10-2026 (`converses-pro-2026-10-26.zip`, resumit a «Disseny» del `README.md`). Cada fase acaba amb alguna cosa que es pot provar; no es comença la següent sense validar l'anterior.

| Fase | Títol | Depèn de | Estat |
| --- | --- | --- | --- |
| 1 | Infraestructura: datasource, `ProRepository`, runner de flags Pro, debug drawer i etiqueta PRO | — | Feta i validada (26-10-2026) |
| 2 | RevenueCat: SDK, entitlement `converses_pro`, preu, compra i restauració | 1 | En curs |
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

`isEnabled()` d'un flag Pro vol dir `Unlocked`. L'estat Pro surt d'un `ProDataSource`: a release, RevenueCat (o «mai Pro» si no hi ha clau); a debug, RevenueCat amb el Test Store i un override des del menú de debug (fase 2).

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

### Configuració de RevenueCat (26-10-2026)

- **Projecte Converses**, dues apps: Test Store (debug) i Converses (Play Store, `com.alorma.contactnotes`, amb les credencials del service account).
- **Entitlement** `converses_pro`, amb els dos productes `lifetime` lligats (no consumibles): Test Store `prode32ea36f83` i Play Store `prod0874b2bb70`. El codi mira sempre l'entitlement (`PRO_ENTITLEMENT`), mai el producte.
- **Offering** `default` (actual), amb un sol paquet: l'app compra el primer paquet de l'offering actual.
- **Claus:** a `local.properties` i als secrets de GitHub del mateix nom. `REVENUECAT_TEST_API_KEY` (debug i l'APK de `build-apk.yml`) i `REVENUECAT_GOOGLE_API_KEY` (release a `release.yml`). Sense clau, l'app no configura RevenueCat i ningú no és Pro.
- **Sandbox Testing Access ha d'estar a «Anybody»** (Project settings › General). Si és restringit (per exemple, al correu de Bernat), les compres de sandbox i del Test Store queden com a «Unattached products» i no donen l'entitlement: l'app no té comptes i RevenueCat no pot identificar ningú per correu. A producció no hi influeix.
- El producte de Play Console no es podia crear fins que hi hagués una build amb `BILLING`: es va pujar la 1.7.9 al track intern.
- **Notificacions de Google (RTDN):** topic `projects/converses/topics/Play-Store-Notifications` (Google Cloud, projecte `converses`), posat a Play Console › Monetization setup. Google Play hi ha de poder publicar: `google-play-developer-notifications@system.gserviceaccount.com` amb el rol *Pub/Sub Publisher* al topic (sense, la notificació de prova falla).
- L'SDK també va a la release: la fitxa de Play ja no diu «sense accés a internet».

### Debug

Al menú de debug, l'apartat **Pro** té un sol interruptor, «Pro user», que ensenya l'estat real: encès i bloquejat si RevenueCat (Test Store) diu que s'ha comprat; si no, encendre'l fa override (Pro sense comprar, desat a DataStore). A sota, «Buy Pro (Test Store)» i «Restore purchases» (al Test Store la restauració no existeix: torna el `CustomerInfo` desat). Per tornar a provar una compra des de zero, cal esborrar les dades de l'app (nou identificador anònim).

### Tasques

- [x] SDK 10.29.0 i claus a `BuildConfig`; configurat anònim quan el fil principal queda lliure (al `onCreate` afegia ~400 ms a l'arrencada), i sense clau no es configura; manifest, builds i emulador — `c8a59ce`
- [x] `RevenueCatProDataSource`: listener de `CustomerInfo`, entitlement `converses_pro`, memòria cau sense connexió; ha de suportar que l'SDK encara no estigui configurat; tests de JVM — `e9cdda4`
- [x] Preu de l'offering, compra i restauració a `ProRepository`, amb els estats (pendent, error, cancel·lació, sense connexió, restaurar amb i sense compra); tests de JVM — `79a8000`
- [x] Reemborsament o revocació: es perd Pro i no s'esborren dades; test de JVM — `e2e16af`
- [x] Debug: apartat Pro amb l'interruptor «Pro user» (estat de RevenueCat o override) i compra i restauració de prova; títols dels apartats en negreta; emulador amb el Test Store (compra vàlida, fallida i cancel·lada, override, sense connexió) — `5ebad34`, `4c95246`, `445104e`
- [x] Fitxa de Play (en, ca, es): treure «sense accés a internet» — `a07ad48`
- [x] `docs/pro/` al dia (entitlement `converses_pro`, configuració de RevenueCat, debug, secrets)
- [x] RevenueCat: el `lifetime` de Play Store també al paquet de l'offering `default` (`$rc_lifetime`, amb els dos productes)
- [x] RevenueCat: notificacions de Google (RTDN) connectades i provades (notificació de prova rebuda)
- [ ] Compra real a Play des del track intern amb un *license tester*

## Fase 3 · Paywall

- [ ] Pantalla «Converses Pro» (8a), des d'una funció (8b, paràmetre d'origen) i «Ja tens Pro» (8c)
- [ ] Targeta d'Ajustos i punts d'entrada `Locked` que porten al paywall, amb el seu origen. Ara no fan res: Ajustos › Contactes i la fila de Contactes de l'editor (`contactes`), el full «Avatar» (`avatar`) i Ajustos › «Color de la persona» (`avatar`, o el que digui el disseny)
- [ ] Textos ca, en i es (tauler 8d)

## Fase 4 · Publicació

- [ ] Política de privacitat (converses.app i alorma.github.io): RevenueCat rep un identificador anònim i les dades de compra, mai notes ni persones
- [ ] Formulari de seguretat de dades de Play
- [ ] Termes
