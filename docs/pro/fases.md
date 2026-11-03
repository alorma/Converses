# Converses Pro — Fases

Què és gratis i què és Pro, i la regla, són a `docs/pro/README.md`. Disseny de la pantalla de Pro i dels punts d'entrada: lliurament de Claude Design del 26-10-2026 (`converses-pro-2026-10-26.zip`, resumit a «Disseny» del `README.md`). Cada fase acaba amb alguna cosa que es pot provar; no es comença la següent sense validar l'anterior.

| Fase | Títol | Depèn de | Estat |
| --- | --- | --- | --- |
| 1 | Infraestructura: datasource, `ProRepository`, runner de flags Pro, debug drawer i etiqueta PRO | — | Feta i validada (26-10-2026) |
| 2 | RevenueCat: SDK, entitlement `converses_pro`, preu, compra i restauració | 1 | Feta i validada (26-10-2026) |
| 3 | Paywall: pantalla «Converses Pro» (8a, 8b, 8c) i punts d'entrada | 2 | Pendent |
| 4 | Publicació: política de privacitat, seguretat de dades de Play, termes | 3 | Pendent |
| 5 | Room v5 i flags de Canals propis i Pendents | — | Feta i validada (03-11-2026) |
| 6 | Canals propis: camp «On» (7a, 7c), full «Nou canal» (7b), cronologia, exportar, Ajustos › Canals propis (7d) | 5 | Feta i validada (03-11-2026) |
| 7 | Pendents: fila «Pendent» (P5, 7e), secció al detall (P6), marcar com a fet (7f), cronologia i comptador al llistat (P7) | 5 | Pendent |

Canals propis i Pendents: lliurament de Claude Design del 02-11-2026 (`converses-canals-pendents.zip`, resumit a «Disseny · Canals propis i Pendents» del `README.md`). La personalització de l'avatar tindrà el seu lliurament i la seva fase.

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
- [ ] Passar a Claude Design els estats `Locked` provisionals (full «Avatar», «Color de la persona») per afinar-los

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
- **SDK 11.0.1 (03-11-2026, Dependabot):** l'API no canvia; porta Play Billing 9.1.0 i demana Kotlin 2.1+ i `compileSdk` 35+ (ja els complim). Quan Play està bloquejat, l'error passa de `StoreProblemError` a `PurchaseNotAllowedError`, i tots dos van a `PurchaseResult.Failed`. Provat amb el Test Store a l'emulador (offering, compra fallida i vàlida); Billing 9 es valida amb la compra real del track intern (fase 3).

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

## Fase 3 · Paywall

### Decisions (26-10-2026)

- **Visible a la release:** la targeta d'Ajustos i el paywall no van darrere de cap flag. Quan es prepari la release que obri Pro, es trauran els flags Pro i només quedarà la comprovació del Pro; fins aleshores no es publica cap release amb el paywall.
- **Els quatre beneficis**, com el disseny, encara que Canals propis i Pendents no existeixin.
- **Peu sense «Termes»:** només «Privacitat» (la política actual). «Termes» s'afegeix a la fase 4, quan hi hagi la pàgina.
- **Origen:** `PaywallRoute(highlight: ProBenefit?)`; `People` cobreix `avatar` i «Color de la persona», `null` és la targeta d'Ajustos. `Channels` i `FollowUps` esperen les seves funcions.
- **Full «Avatar» `Locked`:** no hi havia disseny per a l'accés; tocar «Forma» o «Color» tanca el full i obre el paywall, sense cap canvi visual.

### Tasques

- [x] `PaywallRoute(origin)`, `PaywallViewModel` (preu, compra, restauració, sense connexió amb `NetworkMonitor`, snackbars) i tests de JVM — `ce7df4e`
- [x] Pantalla «Converses Pro» (8a), des d'una funció (8b) i «Ja tens Pro» (8c), amb els textos ca, en i es del tauler 8d; captures — `5406eaa`
- [x] Targeta d'Ajustos (destacada sense Pro, «Activat» amb Pro); captures i emulador amb el Test Store (restaurar sense compra, cancel·lació, error, sense connexió, compra vàlida i «Som-hi») — `f1e2686`
- [x] Punts d'entrada `Locked`: Ajustos › Contactes i la fila de Contactes de l'editor (`Contacts`), Ajustos › «Color de la persona» i el full «Avatar» (`People`); captures i emulador — `c5bd808`
- [ ] Compra real a Play des del track intern amb un *license tester* (Play Console › Settings › License testing; el compte de Bernat ja hi és des del 26-10-2026): a la fase 2 la release no tenia cap botó per comprar; el Test Store sí que es va validar

## Fase 4 · Publicació

- [x] Política de privacitat a converses.app (ConversesLanding `da12eb6`, 26-10-2026): RevenueCat rep un identificador anònim i les dades de compra, mai notes ni persones
- [ ] La mateixa política a alorma.github.io
- [ ] Formulari de seguretat de dades de Play
- [ ] Termes

## Fases 5–7 · Canals propis i Pendents

**Totes dues són funcions Pro.** Cadascuna va darrere del seu flag Pro (`CustomChannelsFeature`, `FollowUpsFeature`; `pro = true`, apagats): `Hidden` no mostra res, `Locked` (sense Pro) mostra l'etiqueta PRO i porta al paywall (`ProBenefit.Channels`, `ProBenefit.FollowUps`), `Unlocked` funciona. La base de dades no depèn del Pro: el que es va crear amb Pro es conserva i es continua veient si es perd («Baixada de Pro», `README.md`).

### Decisions (02-11-2026)

- **Room v5, una sola migració per a les dues funcions** (fase 5). Taula `custom_channel(id, channelType, name, createdAt)` i, a `entry`, `customChannelId` (nul·lable, `SET NULL` en esborrar el canal), `isPending` i `doneAt`.
- **Canviar un canal de grup:** les seves entrades s'hi mouen (el `channelType` de l'entrada passa al grup nou), perquè el filtre per grups quadri.
- **Nom únic dins del grup, sense distingir majúscules** («WhatsApp» i «whatsapp» són el mateix nom); validat al repositori.
- **Canals antics (beta):** no es poden recuperar. La migració 1→2 els va convertir al grup i va esborrar la taula `channel`; no en queda cap rastre.

### Decisions per a les fases 6 i 7 (03-11-2026)

- **«Reanomena» i «Canvia de grup»:** es reaprofita el full 7b («Nou canal», nom i grup) amb el títol «Edita el canal» i els camps omplerts. No es demana disseny nou.
- **Marcar i desmarcar un pendent** també des del full d'accions de l'entrada (backlog 6), a més de la fila «Pendent» de l'`EntryForm`.
- **Textos en anglès i castellà** dels pendents («Pendent», els dos subtítols, «Pendents», «PENDENT», «Marca com a fet»): els proposa Claude a partir del català i es revisen a la proposta de la fase 7.
- **Recordatoris dels pendents:** fora de les fases 6 i 7; queden al backlog.

## Fase 5 · Room v5 i flags

- [x] Fases 5–7 i disseny a `docs/pro/`; backlog 6 i 8 actualitzats
- [x] Room v5: `CustomChannelEntity`, `customChannelId`, `isPending` i `doneAt` a `EntryEntity`; `AutoMigration` 4→5 i test (4→5, `SET NULL` i 1→5 sense perdre dades); `connectedAndroidTest` a l'emulador (8/8) — `c726ce9`
- [x] DAO, `ConversesRepository` i `FakeDb`: canals (crear, reanomenar, canviar de grup amb les entrades, esborrar, llistar amb el nombre d'entrades, nom únic) i pendents (llistar, fet, desfer, `openFollowUps` al llistat, 0 per a les protegides); tests de JVM (`ProFeaturesRepositoryTest`) — `c8e9c29`
- [x] Test instrumentat dels DAO sobre Room en memòria (`CustomChannelDaoTest` a `androidTest`): l'SQL real de `changeGroup`, `SET NULL`, `NOCASE`, `setDoneAt`, `observeOpenFollowUps` i `openFollowUps` al llistat; emulador (7/7) — `6c829a2`
- [x] Flags `CustomChannelsFeature` (`custom_channels_enabled`) i `FollowUpsFeature` (`follow_ups_enabled`), Pro i apagats, al menú de debug i a «Turn on every Pro flag»; tests de JVM (Hidden → Locked → Unlocked) i emulador — `3fbee1a`

## Fase 6 · Canals propis (Pro)

### Decisions (03-11-2026)

- **Canal per defecte:** la nota ràpida i la nova entrada del detall prenen el grup i el canal propi de l'última entrada de la persona (el canal només si està `Unlocked`).
- **Baixada de Pro a l'editor:** una entrada amb canal propi el mostra triat (es pot desmarcar), sense els altres canals ni «+ Nou», amb PRO → paywall.
- **Textos que no eren al tauler 7g** (proposats per Claude, per revisar): «Edita el canal» / «Edit channel» / «Editar canal»; nom repetit: «Ja hi ha un canal amb aquest nom a %s.» / «There's already a channel with this name in %s.» / «Ya hay un canal con este nombre en %s.»; el botó d'editar fa servir «Desa».

### Tasques

- [x] Estat i desament: `EntryChannel` (grup + canal propi) a `EntryFormState`; canviar de grup desmarca el canal; el desen el detall i la nota ràpida; tests de JVM — `906ca3f`
- [x] Camp «On» (7a, 7c): targeta del grup amb els canals i «+ Nou»; `Locked` amb «Canal propi» i PRO → paywall `Channels`; captures i emulador — `4e1a78f`
- [x] Full «Nou canal» / «Edita el canal» (7b); en crear-lo queda triat; tests de JVM, captures i emulador — `7b6a037`
- [x] Cronologia, full d'accions i exportar amb el nom del canal; tests de JVM, captures i emulador — `ef06f32`
- [x] Ajustos › Canals propis (7d): fila i pantalla (llista per grups, reanomena, canvia de grup, esborra, FAB); tests de JVM, captures i emulador — `a792e5d`
- [x] `docs/pro/` i backlog 8 al dia

## Fase 7 · Pendents (Pro): tasques

### Redisseny (03-11-2026)

El primer plantejament (estat `isPending` d'una entrada, amb la fila «Pendent» a l'`EntryForm`) es va descartar a mitja fase: no quadrava amb una cosa que mira cap al futur. Claude Design ho va redissenyar com a **tasques** (taules 9a–9h de `converses-canals-pendents.zip`, a `~/Downloads`; resum a «Disseny · Canals propis i Pendents» del `README.md`). Els commits `be85bd8`, `0108a61` i `1f9a0f9` són d'aquell primer plantejament i es desfan o s'adapten a les tasques següents.

### Decisions (03-11-2026)

- **Taula pròpia, independent de les notes:** `task(id, personId → CASCADE, text, dueDate?, remind, createdAt, doneAt?)`. Les tasques no surten a la cronologia ni tenen vincle amb cap entrada.
- **Es refà la Room v5, sense v6:** Play té la v3 (1.7.10, 26-10-2026) i la v4 i la v5 no han sortit mai. La v5 queda amb `custom_channel` i `customChannelId`, sense `isPending` ni `doneAt` a `entry`, i amb la taula `task`. Es regenera el `5.json` (excepció a «mai editar un esquema exportat», perquè no és a cap dispositiu d'usuari) i no cal convertir entrades pendents en tasques. Les instal·lacions de debug amb la v5 vella han d'esborrar les dades.
- **«Avisa'm» (notificació a les 9:00) queda per més endavant:** la columna `remind` ja hi és, però la fila no surt al formulari.
- **Tocar una tasca** (pendent o feta, fora del cercle) obre el formulari en mode edició, amb l'opció d'esborrar-la.
- **Exportar:** sense canvis en aquesta fase.
- **Decidit per Claude mentre Bernat era fora (per revisar):**
  - **Proper dia rellevant** (el xip amb el nom del dia): el divendres o el dilluns més proper que caigui d'aquí a 2 dies o més (dl–dc → divendres; dj–ds → dilluns; dg → divendres), perquè no coincideixi mai amb «Avui», «Demà» ni «La setmana que ve». «La setmana que ve» és d'aquí a 7 dies. «Tria dia…» deixa triar des d'avui.
  - **Etiquetes de data:** d'aquí a 2–6 dies, el nom del dia; d'aquí a 7 o més, «DC. 18 DE NOV.»; passades, «AHIR» i «FA N DIES». Les fetes avui diuen «FET AVUI».
  - **Textos nous que no eren al tauler 7g:** «Edita la tasca» / «Edit task» / «Editar tarea»; «Esborra la tasca» / «Delete task» / «Borrar tarea»; «Vols esborrar aquesta tasca?» / «Delete this task?» / «¿Borrar esta tarea?»; «Fa %d dies» / «%d days ago» / «Hace %d días»; «Fet avui» / «Done today» / «Hecho hoy». L'edició desa amb «Desa els canvis».
  - **Sense Pro:** la secció només surt si hi ha tasques (pendents o fetes), sense «+ Nova tasca» ni la targeta buida.
  - **Formulari obert des del detall:** la persona inicial (o la de la tasca) compta com a desbloquejada; triar-ne una altra de protegida demana desbloquejar-la, com a la nota ràpida.

### Handoff (03-11-2026)

- **Estat (03-11-2026, tarda):** les quatre tasques de la fase 7 estan fetes i validades (JVM, ktlint, captures i emulador); falta que Bernat la validi i la tanqui. No s'ha pujat res des de `ed075af`.
- **Per revisar:** les decisions «Decidit per Claude mentre Bernat era fora» de més amunt (dia rellevant, etiquetes de data i textos nous).
- **Pendent fora de la fase:** «Avisa'm» (backlog 6); el botó flotant de debug tapa l'acció «Desfés» del snackbar (només a debug).
- **Disseny:** `~/Downloads/converses-canals-pendents.zip`, `INSTRUCCIONS.md` §2 i `pantalles.html` (taulers 9a–9h, textos al 7g). Textos ja decidits que es poden reaprofitar de l'historial (`git show 0108a61 -- app/src/main/res`): «Pendents», «Marca com a fet», «Marcat com a fet». La secció antiga del detall és a `git show 1f9a0f9:app/src/main/java/com/alorma/contactnotes/ui/person/FollowUpsSection.kt`.

### Tasques

- [x] Room v5 refeta i dades (unides): `TaskEntity` + `TaskDao`, sense `isPending`/`doneAt` a `entry`, `5.json` regenerat, repositori (`observeOpenTasks`, `observeDoneTasks`, `getTask`, `saveTask`, `deleteTask`, `markTaskDone`, `reopenTask`), `openTasks` al llistat, `FakeDb` amb `FakeTaskDao`, i desfets `be85bd8`, `0108a61` i `1f9a0f9` (revert). Tests de JVM i ktlint verds; `connectedDebugAndroidTest` a l'emulador (17/17: `MigrationTest`, `CustomChannelDaoTest`, `TaskDaoTest`) — `ed075af`
- [x] Formulari «Nova tasca» / edició (9b, 9c): persona, text amb enganxar, «Quan» en futur, «Guarda a pendents», esborrar; tests de JVM (`TaskFormViewModelTest`, `TaskDueTest`), captures i emulador — `3c267a0` (un sol commit amb la tasca següent, perquè comparteixen textos, navegació i captures)
- [x] Detall: «Nova tasca» al menú amb PRO (9a), secció «Pendents» amb «+ Nova tasca» i estat buit (9d, 9h), marcar com a fet amb «Desfés» (9e), «Fets · N» (9f), editar i esborrar; tests de JVM (`PersonDetailViewModelTest`, `TaskLabelsTest`), captures i emulador (gratuït, Pro i baixada de Pro) — `3c267a0`
- [x] Llistat (9g): pastilla amb `flag` i el nombre, mai a les protegides; captures i emulador — `c63c64d`
- [x] `docs/pro/` i backlog 6 al dia — `c63c64d`
