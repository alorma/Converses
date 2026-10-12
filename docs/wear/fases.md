# Converses per a Wear OS — Fases

Context, abast, arquitectura, decisions (P0–P16) i riscos són a `docs/wear/README.md`; les captures de referència, a `docs/wear/screenshots/`. Cada fase acaba amb alguna cosa que es pot provar; no es comença la següent sense validar l'anterior.

| Fase | Títol | Depèn de | Estat |
| --- | --- | --- | --- |
| 0 | Decisions i investigació | — | Feta, amb 3 pendents que no bloquegen (català i mecanisme de dictat: abans de la fase 6; AVD i Data Layer: fases 1–2) |
| 1 | Mòdul `:wear`, tema i previews amb dades fictícies | 0 | Pendent |
| 2 | Contracte i sincronització mòbil → rellotge | 0 | Pendent |
| 3 | Pantalles de lectura: Persones, Cronologia, Sense mòbil | 1, 2 | Pendent |
| 4 | Més entrades sota demanda | 3 | Pendent |
| 5 | Bloqueig de l'app i persones protegides | 0 (P0, P10), 3, 4 | Pendent |
| 6 | Dictat | 1 | Pendent |
| 7 | Qui, Desat i nota rellotge → mòbil amb cua | 2, 3, 6 | Pendent |
| 8 | Estats d'error i casos límit | 5, 7 | Pendent |
| 9 | Qualitat, privacitat i accessibilitat | 8 | Pendent |
| 10 | CI i publicació | 9 | Pendent |
| 11 | Més endavant | 10 | Idees |

---

## Com es treballa cada fase

Regles d'execució, per a Claude i per a Bernat. Valen per a totes les fases.

1. **Una fase, una sessió.** Cada fase es fa de manera individual. Un cop executada, validada i aprovada, **no es reutilitza la sessió de Claude**: la fase següent comença en una sessió nova o després d'un `/clear`. El que cal recordar va al pla (aquest document i el `README.md`), no a la conversa.
2. **Abans d'iniciar, plantejar les tasques i esperar aprovació.** Claude llegeix el pla i la fase, proposa la llista de tasques concreta (adaptant la de la fase si cal) i **no comença fins que Bernat l'aprova**.
3. **Les tasques es fan en sèrie i de manera autònoma**, d'una en una, validant cada una abans de passar a la següent (regla de `CLAUDE.md`: passos petits).
4. **Cada tasca declara com es valida**, sense dependre de Bernat quan es pot:
   - **Tests** (JVM): `./gradlew testDebugUnitTest` i els tests nous de la tasca.
   - **ktlint:** `./gradlew ktlintCheck`.
   - **Captures de pantalla:** previews i captures comparades amb `docs/wear/screenshots/`; Claude les valida ell mateix.
   - **Emulador:** Claude valida els canvis de UI al parell d'emuladors Wear + telèfon.
   - **Humana**, només quan no es pot fer sol (rellotge real, desbloqueig biomètric, TalkBack, sensació d'ús).
5. **Plantejar com s'executa cada fase**, i deixar-ho dit en proposar les tasques (Bernat ho aprova amb la resta):
   - **Model:** si val la pena un de més petit (p. ex. Haiku) per a tasques mecàniques, o cal un de més capaç per a les delicades (seguretat, sincronització).
   - **Agents:** si algun tros es beneficia d'un subagent (exploració, investigació de documentació) o es fa tot en la sessió principal.
   - **Paral·lelisme:** les tasques que es poden fer en paral·lel (no toquen els mateixos fitxers ni depenen l'una de l'altra) **es fan en paral·lel amb agents**, cadascun en el seu worktree aïllat per evitar conflictes. Claude marca a la proposta quines tasques són paral·lelitzables i quines van en sèrie, i integra i valida el resultat de cada agent abans de seguir. Les tasques dependents continuen en sèrie.
   - **Model dels agents:** per a cada agent d'implementació, Claude **valora si val la pena fer-lo amb Haiku** i ho diu a la proposta. Haiku és candidat quan la tasca és mecànica i ben acotada (components i previews a partir d'un disseny, strings en tres idiomes, plomeria de Gradle o CI, tests a partir d'un patró clar). Es manté un model més capaç (Sonnet o superior) per a tot el que és delicat: privacitat i protegides, sincronització, Keystore i desbloqueig, migracions. Si Haiku no ho resol bé a la primera, es reassigna a un model més capaç en lloc d'insistir. En qualsevol cas, **la validació del resultat la fa el model de la sessió principal**, no l'agent que ha implementat.
6. **Marcar el progrés al pla.** Quan una tasca està feta i validada, es marca a la fase (`- [x]`) amb el hash del seu commit al costat, per exemple `- [x] Tasca — \`abc1234\``. Així el pla diu què hi ha fet i on és.
7. **Commits.** A la proposta d'inici, Claude diu quina estratègia de commits proposa: **un commit per tasca** (per defecte, quan la tasca té sentit sola) o **un commit de tancament de la fase**. L'aprovació de la proposta per part de Bernat autoritza aquests commits, i només aquests. Missatges en anglès, petits, amb l'atribució habitual.
8. **Al acabar totes les tasques de la fase, demanar la validació de Bernat.** Claude presenta què s'ha fet, com s'ha validat i què queda pendent, i **no dona la fase per tancada** fins que Bernat l'aprova. Només llavors s'actualitza l'estat de la taula.
9. **Push només amb la fase validada.** Quan Bernat valida la fase, es fa el push. **Mai en mig d'una tasca, de manera autònoma**, tret que Bernat ho demani expressament (per exemple, per validar en un dispositiu real). Cap canvi a Google Play sense permís.
10. **Les decisions obertes es pregunten, no s'inventen.** Si una fase depèn d'una decisió (P0–P16) o d'un disseny que falta, es demana abans de començar.
11. **Quan una fase ho necessita, es demana disseny** a Claude Design; no es dissenya sobre la marxa.

### Plantilla de proposta per a l'inici de cada fase

Claude presenta, abans de tocar res:

| Camp | Contingut |
| --- | --- |
| Fase | Número i títol |
| Decisions i disseny que cal tenir | Quines P# i quin disseny falten |
| Tasques (en ordre) | Llista numerada; cadascuna amb la seva **validació** (tests / captures / emulador / humana) |
| Execució | Model, agents i quines tasques van en paral·lel o en sèrie, i per què |
| Commits | Un per tasca o un de tancament de la fase |
| Riscos | Què pot anar malament i com es detecta |
| Sortida | Què es pot provar en acabar |

---

## Fase 0 · Decisions i investigació

Sense codi de producte.

- [x] **P0:** aprovació al mòbil amb empremta, vigència de 5 min i notificació com a camí (decidit).
- [x] Decidir si la vigència és un ajust (Ajustos → Seguretat) o queda fixa a 5 min (P11): fixa a 5 min.
- [ ] Demanar el disseny de les pantalles noves (P12) **quan s'arribi a la fase 5**, no abans.
- [x] Tancar P1, P6, P7, P9, P11, P14 i P15 (vegeu `README.md`).
- [x] Consultar la documentació oficial: Wear Compose Material 3 i navegació, Data Layer (`play-services-wearable`), `EdgeButton`, `TransformingLazyColumn`, reconeixement de veu i requisits de Play per a apps de Wear. Versions anotades al `README.md` i afegides al version catalog.
- [x] **Versionat:** s'amplia `alorma/VersionPlugin` (1.1.0, encara no publicada) amb `appVersion { formFactor = FormFactor.WEAR }`: enum PHONE=0 (per defecte), WEAR=2, TV=4, AUTOMOTIVE=6, i `versionCode` = base + offset (1.2.3 → 10020031; WEAR → 10020033). `:app` no canvia. Document inicial: `~/projects/personal/VersionPlugin/docs/version-code-offset.md` (parlava de `versionCodeOffset`; el disseny final és `formFactor`). **Resolt a la fase 1** (1.1.0 publicada i integrada).
- [x] `minSdk` de `:wear`: **30** (Wear OS 3+).
- [x] **P10:** comprovat amb una app de prova descartable a l'emulador Wear OS 7 (API 37), `minSdk` 30 acordat. Resultats: sense bloqueig `isDeviceSecure=false`, el credencial intent és `null` i **no es pot crear una clau que exigeixi autenticació** (cal el bloqueig de pantalla); amb PIN, `isDeviceSecure=true`, el credencial intent apunta als ajustos del rellotge, la clau es crea, i el xifratge queda **bloquejat** (`UserNotAuthenticatedException`) un cop caducada la finestra d'autenticació (provat a 40 s amb una finestra de 30 s); en treure el PIN, **la clau desapareix sola**. Limitacions: és un emulador (`insideSecureHardware=false`, Keystore per programari) i no hi ha prova a l'API 30; cal repetir-ho al rellotge real a la fase 9. Conclusió: s'adopta el xifratge amb Keystore lligat a autenticació.
- [ ] Comprovar si el reconeixedor de veu del rellotge suporta **català** (P5); si no, avisar abans de cercar alternatives. **Pendent:** la documentació no ho confirma; cal rellotge real (abans de la fase 6).
- [ ] Triar el mecanisme de dictat (`SpeechRecognizer` amb parcials o `ACTION_RECOGNIZE_SPEECH`) i explicar-ne el motiu. **Pendent**, depèn de la prova anterior; plantejament: `SpeechRecognizer` amb parcials, pla B `ACTION_RECOGNIZE_SPEECH`.
- [ ] Preparar emuladors: parell Wear + telèfon amb Google Play. Fet: telèfon (Android 17) i `Wear_OS_XL_Round` ja **emparellats**. Pendent: AVD Large Round i Small Round (fase 1) i comprovar que la Data Layer hi funciona (fase 2).
- [x] Confirmar com es publica una app de Wear emparellada a la mateixa fitxa de Play: investigat; els punts no verificats (mateixa release, GPP, formulari de dades) passen a la fase 10.

**Sortida:** decisions escrites i versions fixades.

## Fase 1 · Mòdul `:wear`, tema i previews

- [x] Afegir `:wear` (i `:shared` si es confirma) a `settings.gradle.kts` i al catàleg de versions. — `5708737` (`:shared` queda per a la fase 2)
- [x] Mateix `applicationId` que `:app`, amb sufix `.dev` i signatura de debug com al mòbil; manifest amb `type.watch` i `standalone = false`; `allowBackup=false` i regles de backup. — `5708737`
- [x] **Versionat:** `VersionPlugin` 1.1.0 (ja publicada) aplicat a `:wear` amb `appVersion { formFactor = FormFactor.WEAR }`, sense import. Comprovat: `:wear` 10060012 i `:app` 10060010 (1.6.1 snapshot).
- [x] `ConversesWearTheme` amb els tokens del brief (negre pur, `surface-card`, `surface-card-high`, `primary`, `on-surface`…, avatars pastel i tinta `#1E1D24`) i Roboto Flex (subconjunt llatí, com al mòbil). — `f5bcfcc`
- [x] Components: llista agrupada (separació 2, radi 24 fora i 6 dins), fila de persona, entrada de cronologia (radi 18), grup connectat del «Qui», Galeta d'avatar. — `components/` (previews a la tasca 5)
- [ ] Les 9 pantalles com a previews amb dades fictícies, a Large i Small Round, i captures de preview comparades amb `docs/wear/screenshots/`.
- [ ] Textos a `strings.xml` en en/ca/es; reutilitzar els del mòbil (Jo, Apunt, Desa, grups de canal) i afegir només els nous.
- [ ] ktlint al mòdul; `testDebugUnitTest`, `ktlintCheck` i els APK actuals sense trencar-se.

**Sortida:** les 9 pantalles es veuen a les previews i s'instal·la un APK de debug a l'emulador.

## Fase 2 · Contracte i sincronització mòbil → rellotge

La part més delicada: privacitat.

- [ ] Contracte (a `:shared`): models d'instantani amb **versió de protocol**, serialització kotlinx.serialization.
- [ ] `WearSnapshotPublisher` al mòbil: construeix l'instantani des de Room i el publica amb `DataClient`; republica en canviar les dades i en connectar-se.
- [ ] Contingut: persones (`id`, nom, color i forma d'avatar, `protected`, hora de l'última entrada), **5 últimes entrades** per persona no protegida, i l'**estat del Bloqueig de l'app**.
- [ ] **Filtre de protegides al mòbil:** només `id`, nom, color i `protected = true`; cap entrada, canal ni hora.
- [ ] Capacitat `converses_phone` anunciada al mòbil.
- [ ] Còpia local al rellotge (DataStore o Room petita pròpia) fora del backup, amb l'hora de l'última sincronització.
- [ ] Tests de JVM: una protegida no porta mai contingut; una persona que passa a protegida perd les entrades a la següent publicació; límit de 5; ordre; l'estat del bloqueig s'inclou.
- [ ] Si alguna cosa toca Room → migració, schema i test (regla de `CLAUDE.md`).

**Sortida:** amb dades reals del mòbil, el rellotge rep i desa un instantani correcte; els tests de privacitat passen.

## Fase 3 · Pantalles de lectura

- [ ] Persones (1) i Persones sense mòbil (8), amb «Dades de fa N min». Les protegides només mostren nom i cadenat.
- [ ] Cronologia (2) amb l'`EdgeButton` «Nota»: comprovar P2 i, si cal, forçar que sigui sempre visible.
- [ ] Cronologia buida (P3).
- [ ] Detecció de «mòbil no connectat» amb `NodeClient`/`CapabilityClient`.
- [ ] Navegació amb `SwipeDismissableNavHost`.
- [ ] ViewModels amb tests de JVM; captures de preview.

**Sortida:** navegar Persones → Cronologia amb dades reals; estat sense mòbil correcte.

## Fase 4 · Més entrades sota demanda

- [ ] Petició/resposta per `MessageClient` al mòbil (pàgines de 20) en entrar a la Cronologia i en arribar al final.
- [ ] Les entrades extra viuen **només en memòria** (ViewModel) i s'obliden en sortir.
- [ ] Indicador de càrrega al final de la llista (P16).
- [ ] Mòbil inaccessible: només les 5 de la còpia, la llista s'acaba sense error destacat (P13).
- [ ] Límit de memòria (P7).
- [ ] Tests de JVM: paginació, final de llista, mòbil absent, res al disc.

**Sortida:** una persona amb moltes entrades les va carregant mentre es fa scroll.

## Fase 5 · Bloqueig de l'app i persones protegides

Depèn de **P0** i **P10**.

**Bloqueig de l'app**
- [ ] Llegir l'estat del bloqueig de l'instantani; si no es pot verificar, assumir **actiu**.
- [ ] Si és actiu i el rellotge no és segur (`isDeviceSecure`): pantalla 9 («Cal un bloqueig», «Configura», que obre els ajustos de seguretat si el sistema ho permet). No mostra ni desa cap entrada.
- [ ] Còpia local xifrada amb Keystore lligada a l'autenticació del dispositiu (o esborrada, segons P10).
- [ ] Esborrar la còpia si es treu el bloqueig del rellotge, en obrir l'app i a cada sincronització.
- [ ] La cua de pendents i «més entrades» segueixen les mateixes regles.

**Persones protegides**
- [ ] **Disseny (P12):** abans de començar, demanar les pantalles noves de protegides i la notificació/flux al mòbil.
- [ ] Pantalla 7 amb «Desbloqueja» → credencial de dispositiu; vigència de 5 min (P11).
- [ ] **Aprovació al mòbil (P0):** petició amb identificador d'un sol ús i caducitat (~60 s) → pantalla «Confirma-ho al mòbil» → notificació genèrica al mòbil → `BiometricPrompt` → resposta amb les entrades només d'aquella persona. Denegat i caducat gestionats.
- [ ] Limitar la freqüència de peticions i no registrar contingut.
- [ ] Testimoni de vigència curta per a notes a protegides; el mòbil rebutja les que no en tenen un de vàlid.
- [ ] Mostrar les entrades **només en memòria**; esborrar-les en sortir o amb l'app en segon pla.
- [ ] Sense mòbil després del desbloqueig: «No disponible sense el mòbil» (P12).
- [ ] Gravar una nota a una protegida només després del desbloqueig.
- [ ] Al mòbil: servir les entrades d'una protegida **només amb una aprovació biomètrica vàlida** (P0).
- [ ] Tests de JVM: res d'una protegida al disc ni a la còpia, esborrat en sortir, estat del bloqueig, assumir actiu; una petició sense aprovació, caducada o repetida no serveix res; una nota a protegida sense testimoni vàlid es rebutja.

**Sortida:** amb el bloqueig actiu i rellotge sense PIN, només es veu la pantalla 9; amb PIN, una protegida s'obre i no queda rastre en sortir.

## Fase 6 · Dictat

- [ ] Pantalla 3 amb el mecanisme triat a la fase 0, text parcial en directe i «Acaba».
- [ ] Permís `RECORD_AUDIO` (flux de petició).
- [ ] Ones: animació simple o lligada a l'amplitud del micròfon; respectar «reduir animacions».
- [ ] Idioma el de l'app (ca/es/en).
- [ ] Estats d'error i permís denegat (P4).

**Sortida:** dictar una frase i veure'n la transcripció al rellotge.

## Fase 7 · Qui, Desat i nota rellotge → mòbil

- [ ] «Qui» (4): Jo / {persona} / Apunt amb els mateixos textos que `whoLabel(...)`; per defecte la persona; «Desa».
- [ ] Cua de pendents amb reintents (WorkManager o pròpia), amb UUID per nota; les notes queden a la cua fins entregar-se (P14).
- [ ] Enviament amb `MessageClient` a `/converses/note`.
- [ ] `WearableListenerService` al mòbil: valida que la persona existeixi, desa com una entrada nova i ignora duplicats per UUID. Decidir el canal (P9). Si cal una columna (p. ex. origen) → migració, schema i test; preferible no tocar l'esquema.
- [ ] Desat (5) i Desat sense mòbil (6): es tanca sola i torna a la Cronologia; en entregar, s'esborra de la cua.
- [ ] Haptic `Confirm` en desar (P6).
- [ ] Tests de JVM: cua, reintents, idempotència.

**Sortida:** apuntar una nota des del rellotge i veure-la al mòbil, amb i sense connexió.

## Fase 8 · Estats d'error i casos límit

- [ ] Mòbil absent durant tota la sessió; rellotge reiniciat amb notes a la cua.
- [ ] Nota duplicada.
- [ ] Persona que passa a protegida (amb còpia al rellotge i amb notes encuades): el rellotge esborra les entrades i la nota no s'aplica sense decisió (R3).
- [ ] Persona esborrada al mòbil amb notes pendents (R4).
- [ ] Reconeixedor sense resultat o sense català (P5).
- [ ] Bloqueig de l'app activat o desactivat mentre el rellotge és obert.

## Fase 9 · Qualitat, privacitat i accessibilitat

- [ ] Repàs de privacitat de punta a punta: res de protegides al disc del rellotge (còpia, logs, backup, notificacions). Test que ho comprova.
- [ ] Còpia i cua fora del backup d'Android.
- [ ] Large i Small Round, mida de lletra gran, ambient, ca/es/en.
- [ ] TalkBack al rellotge.
- [ ] Rellotge real: bateria, latència, desconnexions, bloqueig i desbloqueig reals.
- [ ] Release amb R8: Koin, serialització i Data Layer.
- [ ] Permisos: només els imprescindibles (`RECORD_AUDIO`).
- [ ] Comparar la implementació amb `docs/wear/screenshots/` i anotar les diferències.

## Fase 10 · CI i publicació

- [ ] `pr-build.yml`: tests, ktlint i APK de `:wear`.
- [ ] `screenshot-tests.yml`: referències de `:wear` (etiqueta `update-screenshots`).
- [ ] `release.yml`: AAB del rellotge i Gradle Play Publisher; confirmar el flux de la Play Console.
- [ ] **Política de privacitat i «Sense núvol» (P8):** verificar com viatgen les dades a la Data Layer i ajustar l'esborrany del brief abans de publicar-lo; traduir a ca/es.
- [ ] Fitxa de Play: captures i text per a Wear OS en en/ca/es (`app/src/main/play`); formulari de seguretat de les dades si cal.
- [ ] Pista inicial `internal`, després `beta`.
- [ ] Notes de versió: proposar el punt; no es toquen sense acord.

## Fase 11 · Més endavant

- Tile i complicació de nota ràpida (no dissenyats).
- Respostes predefinides, o triar el canal des del rellotge.
- Notificacions des del mòbil (sense contingut sensible).
- Rellotge autònom amb base de dades pròpia.
