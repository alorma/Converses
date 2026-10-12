# Converses per a Wear OS — Pla

Actualitzat el 12 d'oct. de 2026 (2.ª versió del brief) · @Bernat · **ja hi ha disseny.** Les captures de referència són a `docs/wear/screenshots/` (per fer comparatives amb la implementació). La resta del brief (instruccions, tokens i fonts `.dc.html`, que manen en mides, radis i colors) és a `~/Downloads/converses-wear-handoff/`. Aquest document en recull l'abast, l'arquitectura, les decisions i els riscos; les fases són a `docs/wear/fases.md`.

## Referència visual

Captures a 720×720, fons transparent. Són una aproximació: la tipografia hi surt amb Inter en lloc de Roboto Flex.

| Fitxer | Pantalla |
| --- | --- |
| `screenshots/00-overview.png` | Visió general |
| `screenshots/01-persones.png` | Persones |
| `screenshots/02-cronologia.png` | Cronologia |
| `screenshots/03-dictat.png` | Dictat |
| `screenshots/04-qui.png` | Qui |
| `screenshots/05-desat.png` | Desat |
| `screenshots/06-desat-sense-mobil.png` | Desat sense mòbil |
| `screenshots/07-persona-protegida.png` | Persona protegida (desbloqueig al rellotge) |
| `screenshots/08-persones-sense-mobil.png` | Persones sense mòbil |
| `screenshots/09-cal-bloqueig-rellotge.png` | Cal un bloqueig al rellotge |

## Objectiu

Un mirall del mòbil al rellotge: veure les persones i les últimes entrades, i **afegir una nota per veu** a una persona existent.

## Premisses (del disseny)

1. **No és una app autònoma.** El mòbil (Room) és l'única font de veritat; el rellotge en té una còpia reduïda.
2. **Sense núvol.** Sincronització només amb la Wearable Data Layer; cap servidor propi ni Firebase. `allowBackup` desactivat.
3. **Només lectura + una acció.** L'única escriptura és una nota per veu a una persona existent.
4. **Persones protegides:** exigeixen **desbloqueig al rellotge** (PIN, patró o contrasenya del rellotge) cada cop que s'obren. El rellotge no n'emmagatzema les entrades.
5. **Bloqueig de l'app:** si és actiu al mòbil, el rellotge ha de tenir bloqueig de pantalla propi; si no, no mostra ni desa res.
6. **Sempre fosc**, fons negre pur (OLED), sense color de tema ni Pro. El color només és identitat (avatars).

## Abast

### Dins

- **Llista de persones** (agrupada, avatar Galeta, nom, hora de l'última entrada). Les protegides només mostren nom i cadenat.
- **Cronologia** (només lectura; qui, hora, canal i text) amb l'`EdgeButton` «Nota» sempre visible. Mostra les 5 últimes de la còpia local i **en carrega més sota demanda** (pàgines de 20) en entrar i en arribar al final.
- **Nota per veu:** Dictat (transcripció en directe) → «Qui» (Jo / {persona} / Apunt; per defecte, la persona) → Desat.
- **Cua fora de línia** amb reintents i estat «S'enviarà al mòbil quan hi hagi connexió».
- **Persona protegida:** pantalla 7 → «Desbloqueja» → credencial de dispositiu del sistema → Cronologia amb entrades demanades al mòbil **només en memòria**.
- **Bloqueig de l'app:** pantalla 9 («Cal un bloqueig») si el rellotge no és segur.
- **Mòbil no connectat:** avís i dades de la còpia local («Dades de fa N min»).
- Textos en ca/es/en; els nous són `Nota`, `Acaba`, `Qui ho ha dit?`, `Nota per a {nom}`, `Desat`, `Enviada al mòbil`, `S'enviarà al mòbil quan hi hagi connexió`, `Mòbil no connectat`, `Dades de fa {n} min`, `Persona protegida`, `Desbloqueja-la amb el PIN del rellotge.`, `Desbloqueja`, `Cal un bloqueig`, `Activa un PIN al rellotge.`, `Configura`.

### Fora

Crear, editar, arxivar o esborrar res; Ajustos; color de tema; Pro; tile i complicacions (no dissenyats); analítica de contingut.

## Pantalles i flux

Persones → Cronologia → Dictat → Qui → Desat, més els estats d'excepció. Rutes: `people`, `person/{id}`, `dictate/{personId}`, `who/{personId}`, `saved/{pending}`, `protected`.

| # | Pantalla | Notes |
| --- | --- | --- |
| 1 | Persones | Toc → Cronologia; protegida → pantalla 7 |
| 2 | Cronologia | `EdgeButton` «Nota» fix; càrrega de més entrades sota demanda |
| 3 | Dictat | Text parcial en directe, ones decoratives, «Acaba» |
| 4 | Qui | Grup connectat de 3; «Desa» |
| 5 | Desat | Confirmació; es tanca sola i torna a la Cronologia |
| 6 | Desat sense mòbil | Mateix, amb icona de rellotge |
| 7 | Persona protegida | `EdgeButton` «Desbloqueja» → credencial del sistema; en èxit → Cronologia |
| 8 | Persones sense mòbil | Avís «Mòbil no connectat · Dades de fa N min» |
| 9 | Cal un bloqueig | Substitueix qualsevol altra pantalla; sense dades; «Configura» |

Amb el mòbil connectat no es mostra cap indicador de sincronització; només a les excepcions (6 i 8).

## Arquitectura

- **Mòduls:** nou `:wear` (aplicació Wear OS) amb **el mateix `applicationId`** que el mòbil. Models de presentació i contracte de missatges en un mòdul comú petit (`:shared`), sense Room. `:designsystem` és per a telèfon: el rellotge usa **Wear Compose Material 3** i un `ConversesWearTheme` propi amb els tokens del brief.
- **Manifest:** `uses-feature` `android.hardware.type.watch` i `com.google.android.wearable.standalone = false`; `allowBackup=false` i regles de backup al mòdul.
- **UI:** Wear Compose Material 3 (`AppScaffold`, `ScreenScaffold`, `TransformingLazyColumn`, `EdgeButton`, `TimeText`) i `compose-navigation` per a Wear. Versions: les estables vigents, comprovades a la documentació oficial (regla de `CLAUDE.md`).
- **Mòbil → rellotge (instantani):** `DataClient`. Persona: `id`, nom, color i forma d'avatar, `protected`, hora de l'última entrada. Entrades: **només les últimes 5 per persona**. Protegides: només `id`, nom, color i `protected = true`. A més, l'**estat del Bloqueig de l'app** (actiu/inactiu). Es republica quan canvien les dades i en connectar-se.
- **Més entrades (sota demanda):** petició/resposta per `MessageClient`, pàgines de 20; viuen **només en memòria** (ViewModel) i s'obliden en sortir. Si el mòbil no és accessible, es veuen només les 5 i la llista s'acaba sense error destacat.
- **Persones protegides:** doble desbloqueig (vegeu P0). Primer, la credencial del dispositiu al rellotge (`KeyguardManager.createConfirmDeviceCredentialIntent` o l'equivalent de Wear OS; cal verificar-lo a l'API mínima) cada cop que s'obre la persona; després, el rellotge demana les entrades al mòbil, que les serveix només després d'aprovar-ho amb `BiometricPrompt`; el rellotge les mostra només en memòria; en sortir o passar a segon pla, s'esborren. Sense bloqueig al rellotge: pantalla 9. Sense mòbil després del desbloqueig: «No disponible sense el mòbil» (no dissenyat). Gravar una nota a una protegida només és possible després del desbloqueig.
- **Bloqueig de l'app:** si és actiu, el rellotge ha de ser segur (`KeyguardManager.isDeviceSecure`). Si no, pantalla 9 i ni es mostra ni es desa cap entrada. La còpia local (les 5 últimes) es xifra amb una clau d'Android Keystore lligada a l'autenticació del dispositiu, si l'API ho permet; si es treu el bloqueig del rellotge, **s'esborra la còpia** en obrir l'app i a cada sincronització. Si no es pot verificar l'estat, s'assumeix **actiu**. La cua de pendents i «més entrades» segueixen les mateixes regles.
- **Rellotge → mòbil (nota):** `MessageClient`, camí `/converses/note`, payload `{ id (UUID), personId, who, text, timestamp }`. El mòbil la rep amb un `WearableListenerService`, valida que la persona existeixi i la desa com una entrada nova. Idempotent per UUID. Cua al rellotge (WorkManager o pròpia) amb reintents; s'esborra en entregar-se.
- **Còpia local al rellotge:** DataStore o Room petita pròpia (preferible no tocar l'esquema del mòbil), amb l'hora de l'última sincronització, fora del backup.
- **«Mòbil no connectat»:** `NodeClient`/`CapabilityClient` sense el node del mòbil; capacitat `converses_phone` anunciada al mòbil.
- **Dictat:** `SpeechRecognizer` amb `EXTRA_PARTIAL_RESULTS` o `ACTION_RECOGNIZE_SPEECH` (més simple, sense text en directe). Permís `RECORD_AUDIO`.

## Decisions

### Preses pel disseny

- Només s'apunta a una persona **existent**; sense notes «sense persona».
- Sense tile ni complicacions a la v1.
- Sempre fosc; sense color de tema ni Pro.
- Còpia local reduïda a **5 entrades** per persona; la resta, sota demanda i en memòria.
- Protegides: desbloqueig al rellotge cada cop; res de contingut persistit.

### P0 · Qui autoritza veure una protegida al rellotge

El brief posa el desbloqueig al rellotge i diu que el mòbil serveix les entrades «després del desbloqueig». Però **el mòbil no pot comprovar que el rellotge s'hagi desbloquejat**: només rep una petició. Acceptar-ho sense més canviaria la regla de `CLAUDE.md` (una protegida no filtra res abans de desbloquejar).

**Decidit:** el mòbil demana confirmació (opció 2). Les altres opcions (acceptar el brief tal com és, o deixar les protegides fora del rellotge) es descarten. No cal cap emparellament nou: la Data Layer ja només parla amb el rellotge emparellat, i el mòbil només ha de comprovar que el node és el del parell. El que falta és **la presència de l'usuari al mòbil**.

**Decidit el 12 d'oct. de 2026:** vigència de l'aprovació de **5 minuts** per persona, i la **notificació** com a camí.

**Mecanisme:**

1. Rellotge: toc a la protegida → pantalla 7 → «Desbloqueja» → credencial del rellotge (primer desbloqueig).
2. Rellotge: envia al mòbil una petició amb un identificador d'un sol ús que caduca en ~60 s, i mostra «Confirma-ho al mòbil».
3. Mòbil: notificació **genèrica, sense nom** («El rellotge demana obrir una persona protegida»). En tocar-la s'obre el `BiometricPrompt` de sempre (segon desbloqueig).
4. Mòbil: si va bé, respon només a aquella petició amb les entrades d'aquella persona; si falla o caduca, no envia res.
5. Rellotge: mostra les entrades només en memòria.
6. **Notes a una protegida:** l'aprovació dona un testimoni de vigència curta que el rellotge adjunta a la nota; el mòbil rebutja les notes a protegides sense testimoni vàlid. Cobreix la cua fora de línia.
7. Cal limitar la freqüència de peticions (sense spam de notificacions) i no registrar cap contingut.

**Vigència configurable?** Idea oberta: un ajust a Ajustos → Seguretat del mòbil, amb 5 min per defecte. Aniria a DataStore (sense migració de Room). Cal decidir si es fa a la v1 del rellotge o es deixa fixa a 5 min i es mou a un ajust més endavant; i, si es fa, quines opcions hi ha.

**Caldrà disseny** (es demana quan faci falta, a la fase 5): pantalles «Confirma-ho al mòbil», «Denegat» i «Ha caducat» al rellotge; la notificació i el flux d'aprovació al mòbil; i, si hi ha l'ajust, la fila d'Ajustos.

### Decidides a la fase 0 (12 d'oct. de 2026)

| # | Decisió |
| --- | --- |
| P1 | Les protegides mostren **nom i cadenat** a la llista; cap entrada, canal ni hora. |
| P6 | Haptic `Confirm` en desar, respectant el sistema i l'interruptor de l'app. |
| P7 | Pàgines de «més entrades» de 20; el límit de memòria es fixa a la fase 4. |
| P9 | La nota des del rellotge té **el canal per defecte de la nota ràpida**. |
| P11 | Vigència de l'aprovació **fixa a 5 min** a la v1; ajust més endavant si cal. |
| P14 | Les notes pendents es queden al rellotge fins entregar-se (fora del backup; xifrat/esborrat segons la fase 5). |
| P15 | Bloqueig de l'app actiu: **no es demana el PIN en obrir**; es confia en el bloqueig del rellotge (pantalla 9 si no és segur). |
| Navegació | **Navigation 3 per a Wear** (`NavDisplay` + `rememberSwipeDismissableSceneStrategy`), coherent amb el mòbil. Cost: `navigation3-runtime/ui` **1.3.0-alpha02** (alpha) al rellotge; el mòbil continua amb la 1.2.0 estable (claus separades al catàleg). |

### Versions fixades (Google Maven, 12 d'oct. de 2026)

`androidx.wear.compose:compose-material3`, `-foundation`, `-navigation3` i `-ui-tooling`: **1.7.1** (estable; la 1.8.0-alpha01 no s'usa). `androidx.wear:wear-tooling-preview` **1.0.0**. `com.google.android.gms:play-services-wearable` **20.0.1**. `navigation3-runtime/ui` del rellotge **1.3.0-alpha02**. `EdgeButton`, `TransformingLazyColumn`, `ScreenScaffold` i `AppScaffold` són estables; Horologist no cal. `compileSdk`/`targetSdk` 37 (Wear OS 7). **`minSdk` del rellotge: pendent** (els docs no en recomanen cap). Ja són al `gradle/libs.versions.toml` (claus `wear*`).

### Versionat (`alorma/VersionPlugin`)

El plugin calcula `versionCode` només de `version.properties` (`major·minor·patch` + `1` release / `0` snapshot), així que `:wear` tindria **el mateix `versionCode` que `:app`**, i Play exigeix que sigui únic entre form factors. **Pendent de decidir (fase 1/10):** opció al plugin (`versionCodeOffset` o similar), o compensar-ho al `:wear` (p. ex. el del plugin + 2). El `versionName` pot ser el mateix.

### Publicació a Play (investigat, no tot verificat)

Mateix package i mateixa clau de signatura (WO-G7). Form factor Wear OS a Advanced settings de la Play Console, amb captures 1:1 sense marc (WO-G5), icona (WO-G3) i descripció que esmenti Wear OS (WO-G2). **No verificat:** si mòbil i rellotge poden anar en la mateixa release, si Gradle Play Publisher pot pujar els dos AAB sense que l'un esborri l'altre (`retain`), i si la Data Layer canvia el formulari de seguretat de les dades. Es comprova a la fase 10 amb una prova real.

### Dictat i català (P5) — sense confirmar

La documentació oficial no confirma el català (ca-ES) al reconeixedor del rellotge, ni en línia ni fora de línia, ni el comportament de `EXTRA_PARTIAL_RESULTS` a Wear. L'única via documentada és `ACTION_RECOGNIZE_SPEECH` (sense text parcial en directe). Cal una **prova en un rellotge real** amb ca/es/en, amb i sense connexió (`checkRecognitionSupport`). Si el català no hi és, s'avisa abans de buscar alternatives.

### Obertes — la resta

| # | Decisió | Inclinació |
| --- | --- | --- |
| P2 | **`EdgeButton` i scroll:** ha de ser sempre visible; comprovar si `ScreenScaffold` el plega | Verificar a la fase 3 |
| P3 | **Cronologia buida:** només el botó «Nota» gran al centre (no dissenyat) | Acceptar |
| P4 | **Micròfon denegat i error del reconeixedor:** no dissenyats | Estat simple |
| P5 | **Català al reconeixedor:** si no el suporta, avisar abans de buscar alternativa | Sense confirmar; cal rellotge real (vegeu a dalt) |
| P8 | **Política de privacitat i «Sense núvol»:** verificar com viatgen les dades a la Data Layer i ajustar-ne el text (esborrany al brief) abans de dir res a la fitxa de Play | Fase 8 |
| P10 | **Versions mínimes de Wear OS**, i que `isDeviceSecure` i el xifratge amb Keystore lligat a autenticació hi funcionin; si no, esborrar la còpia en lloc de xifrar-la | Comprovat a l'emulador (API 37); `minSdk` 30; falta rellotge real (fase 9) |
| P12 | **Pantalles no dissenyades de protegides:** «No disponible sense el mòbil», «Confirma-ho al mòbil», «Denegat» i «Ha caducat» | Caldrà disseny; es demana quan faci falta |
| P13 | **Mòbil inaccessible a «més entrades»:** la llista s'acaba sense error (suposició del brief, la frase original es va tallar) | Confirmar |
| P16 | **Indicador de càrrega** de «més entrades» (no dissenyat) | Coherent amb Wear M3 |

## Riscos

- **R1 · Fuga de protegides.** Cal el filtre al mòbil abans de publicar l'instantani (cap entrada de protegides), amb test de JVM, i que el mòbil només serveixi una protegida amb l'aprovació biomètrica de P0. La notificació de l'aprovació no pot dur el nom.
- **R2 · Dades de text al rellotge.** Les últimes 5 entrades de cada persona no protegida es desen al rellotge, i fins a 20 més en memòria. Reduït respecte de la 1.ª versió del brief, però és una novetat de privacitat: recollir-ho a la política (P8).
- **R3 · Persona que passa a protegida** amb la còpia al rellotge o amb notes encuades: el rellotge esborra les entrades i les notes pendents no s'apliquen sense decisió.
- **R4 · Persona esborrada al mòbil amb notes pendents:** rebuig o descart amb avís.
- **R5 · Duplicats** per reintents: UUID idempotent.
- **R6 · Backup:** cua i còpia local fora de la còpia d'Android.
- **R7 · Bloqueig no verificable:** si no es pot saber l'estat del Bloqueig de l'app, s'assumeix actiu.
- **R8 · Dependència nova** `play-services-wearable`; revisar com afecta la fitxa de Play.
- **R9 · CI:** un mòdul més a `pr-build.yml`, `release.yml` i captures.
- **R10 · Complexitat de seguretat al rellotge** (Keystore, credencial de dispositiu, API mínima): pot no ser viable a tots els rellotges (P10).

## Observacions del brief

- Parla de «versió alpha a Google Play»: la pista actual és **beta**.
- Cita el repo `alorma/Contacts-Notes`; el repo és `alorma/Converses`.
- La premissa 2 diu que la sincronització «va de rellotge a mòbil», però també hi ha mòbil → rellotge (instantani, entrades).
- Els pendents del brief estan numerats 1–9, 11, 10 (desordenats); el pla els renumera a P1–P16.
- El «Bloqueig de l'app» és el que ja existeix al mòbil (`LockState`, biometria/credencial); cal confirmar que l'estat és fàcil d'exposar al publicador.

## Proves

Tests de JVM per a la lògica (cua, filtratge, idempotència, mapatge, esborrat de la còpia si es perd el bloqueig); captures de preview de les 9 pantalles comparades amb `docs/wear/screenshots/`; res d'instrumentat al CI. Validació manual amb parell emulador Wear (Large i Small Round) + telèfon i, després, rellotge real. Casos obligats: mòbil absent, rellotge reiniciat, nota duplicada, persona que passa a protegida, persona esborrada amb notes pendents, bloqueig de l'app actiu amb rellotge sense bloqueig, i bloqueig tret del rellotge.
