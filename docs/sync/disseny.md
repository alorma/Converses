# Sincronització al núvol: disseny

Estat: **proposta; només hi ha construïdes les fases 1, 2 i 3** (identitat de sincronització a Room v6, vegeu «Canvis a Room»; criptografia, vegeu «Claus»; compte i clau darrere del flag `sync`, vegeu «Construït (fase 3)»). Escrit el 10 de novembre de 2026 a partir de la conversa amb Bernat. Quan la funció existeixi, aquest fitxer es condensa en un `README.md` que descrigui el que hi ha, com la resta de `docs/`.

Les decisions marcades **(B)** són de Bernat. Les marcades **(C)** les ha pres Claude en escriure el disseny i es poden canviar. Les pantalles són a `disseny/`: `INSTRUCCIONS.md` (mides, estats, textos en tres idiomes) i `pantalles.html` (referència visual, taulers 13a…13t i 0c). Les decisions que venen d'allà van marcades **(D)**.

## Per què

- Bernat fa factory resets i vol recuperar les dades sense haver-hi pensat abans.
- Hi haurà una app de tauleta (una altra feina, `docs/pantalles-grans`): s'escriu al mòbil pel carrer o a l'oficina i es consulta a la tauleta a casa, per exemple en rebre una trucada.
- Bernat, com a administrador de Firebase, **no ha de poder llegir el que escriu la gent**.

## Objectius

1. **(B)** L'app continua funcionant sense compte, exactament igual que ara. Iniciar sessió activa la sincronització; no hi ha cap altre interruptor.
2. **(B)** Sincronització completa i **gratuïta per a tothom**, coherent amb la regla de Pro: mai es cobra per accedir, protegir o treure les dades pròpies.
3. **(B)** Xifratge d'extrem a extrem. La clau només és als dispositius de l'usuari i a les seves paraules de seguretat, que guarda on vulgui (1Password o similar). Cap còpia de la clau al servidor.
4. **(B)** Room continua sent la font de veritat. Tot funciona sense connexió i la sincronització passa per darrere.
5. **(B)** Límit de dispositius: 2 gratuït, 5 amb Pro. Canviar-los ha de ser possible però tediós.

## Fora d'abast

- Lligar RevenueCat amb el compte de Firebase **(B: es mirarà a part)**.
- Còpia i restauració amb un fitxer local.
- Block Store (restaurar la clau sola després d'un reset). Es pot afegir després sense canviar el disseny.
- Notificacions push per sincronitzar amb l'app tancada **(C)**.
- Sincronitzar els ajustos (tema, idioma, bloqueig de l'app, consentiment d'estadístiques): són de cada dispositiu **(C)**.
- Esborrat automàtic de comptes inactius i avisos per correu.
- L'app de tauleta en si.

## El que no ha de passar mai

- Que text, noms de persones o el tipus d'un registre arribin al servidor sense xifrar.
- Que una sincronització esborri o trepitgi dades locals sense que l'usuari ho hagi confirmat.
- Que un dispositiu deixi de sincronitzar en silenci.
- Que Firebase Analytics o Crashlytics recullin res perquè l'usuari ha iniciat sessió per sincronitzar. El consentiment d'estadístiques continua sent independent.
- Que una notificació, un error o un esdeveniment d'analítica de la sincronització porti noms o text.
- Que l'app proposi la sincronització amb finestres que s'obren soles.

## Com es veu per a l'usuari

Resum; el detall de pantalles i estats és a `disseny/INSTRUCCIONS.md`.

- **(B)** Un indicador de compte a la capçalera del llistat de persones (anònim o avatar) i una fila a Ajustos porten a la pantalla «Sincronització». **(D)** L'indicador va a la dreta de `more_vert`; a la tauleta va a baix del rail i la capçalera no en porta. La fila d'Ajustos va en una secció nova «Compte», sota la targeta de Pro.
- **(B)** L'onboarding ofereix iniciar sessió, com a opció i sense obligar: qui ve d'un reset recupera les dades abans de crear res. **(D)** És un botó de text «Ja tinc dades al núvol» al pas 3/3. El mateix pas canvia els xips «Sense núvol» i «Sense comptes» per «Xifrat» i «Compte opcional», i el text.
- **Primer dispositiu:** iniciar sessió amb Google, veure les paraules de seguretat, confirmar que s'han guardat, pujada inicial. **(D)** Fins que no es marca «Les he guardat en un lloc segur» i es continua, no es puja res; tancar abans tanca la sessió i no deixa res al núvol.
- **(D)** Pujada i baixada inicials: barra de progrés determinada pel nombre d'entrades. «Amaga» deixa la feina en segon pla amb WorkManager i una notificació de progrés, sense noms ni text.
- **(D)** Nom del dispositiu: per defecte el model (`Settings.Global.DEVICE_NAME`, o `Build.MODEL`); es pot canviar des de la llista de dispositius.
- **(D)** No hi ha avisos per recordar les paraules. La fila «Paraules de seguretat» diu quan es van revisar per última vegada, i obrir-les reinicia el comptador.
- **(D)** Sense connexió no hi ha cap diàleg: l'indicador i la fila canvien d'estat, i els canvis pugen sols. Esborrar el compte sí que necessita connexió.
- **Dispositiu addicional o després d'un reset:** iniciar sessió, donar la clau (paraules o QR d'un dispositiu que ja sincronitza), triar quin dispositiu surt si les places són plenes, fusionar amb el que hi hagi en local, baixar.
- **(B)** Places plenes: tria el dispositiu que entra. Mai s'expulsa ningú automàticament. Sense límit de canvis d'entrada.
- **Dispositiu tret:** conserva les dades, deixa de sincronitzar i mostra un avís fix. En tornar, es fusiona.
- **(C)** Tancar sessió allibera la plaça i deixa les dades al dispositiu. Esborrar el compte esborra el núvol i el compte, no el dispositiu.
- **(B)** Sense paraules i sense cap dispositiu que tingui la clau, les dades del núvol són irrecuperables. **(C)** Si el dispositiu encara té les dades en local, pot començar de nou amb paraules noves, substituint el núvol.

## Arquitectura

### Peces

| Peça | On | Què fa |
| --- | --- | --- |
| Identitat de sincronització | Room, migració | Dona a cada fila un identificador global, una data de modificació i una marca de «pendent de pujar»; registra els esborrats |
| Criptografia | `data/sync/crypto`, JVM pur | Paraules, derivació de claus, xifrar i desxifrar registres |
| Magatzem de la clau | `data/sync` | Guarda la clau en un fitxer xifrat amb una clau de l'Android Keystore |
| Compte | `data/sync` | Inici de sessió amb Google, Firebase Auth, document del compte, dispositius |
| Motor | `data/sync` | Puja els pendents, baixa els canvis, fusiona |
| UI | `ui/sync` | Pantalla «Sincronització» i indicador; segons disseny |

La resta de l'app no sap res de Firebase: parla amb un `SyncRepository` que exposa l'estat (`SignedOut`, `NeedsKey`, `Syncing`, `Synced`, `Offline`, `Removed`, `Error`) i les accions. Darrere hi ha interfícies (`SyncRemote`, `KeyStore`, `AuthGateway`) amb implementacions falses per als tests JVM, com `FirebaseGateway` i `RevenueCatGateway`.

### Firebase sense trencar la regla de l'analítica

Avui Firebase **no s'inicialitza** sense el «Sí» a les estadístiques (`docs/analytics/README.md`). La sincronització necessita Auth i Firestore encara que l'usuari hagi dit «No».

**(B, 11 de novembre de 2026)** Hi ha dues instàncies de Firebase, i cadascuna té el seu motiu per existir:

| Instància | Què hi viu | Quan s'inicialitza |
| --- | --- | --- |
| La per defecte | Analytics i Crashlytics | Només amb el «Sí» a les estadístiques, com fins ara (`FirebaseGateway`) |
| `sync`, amb nom | Auth, Firestore i App Check | Només quan hi ha sessió de sincronització o l'usuari la comença **(C: no a cada arrencada per a qui no sincronitza)** |

- La regla de `CLAUDE.md` i de `docs/analytics` no canvia: Firebase Analytics i Crashlytics no s'inicialitzen mai sense consentiment. Iniciar sessió per sincronitzar no els engega.
- Les dues són independents: dir «Sí» o «No» a les estadístiques no toca la sincronització, i tancar la sessió no toca les estadístiques.
- `AndroidFirebaseGateway` ha de mirar si existeix la instància per defecte (`FirebaseApp.DEFAULT_APP_NAME`), no si n'hi ha alguna.

La primera idea era una sola instància amb la recollida apagada. El spike la va descartar: vegeu a sota què passa amb el que Crashlytics i Analytics recullen durant el «No».

#### Resultat del spike (10 i 11 de novembre de 2026)

Fet en un emulador amb Play Store (`37_Pixel_Phone`), amb la variant de debug i el compte de Google de Bernat. El codi del spike és a la branca local `spike/sync-firebase` i no es fusiona. Les proves es van llegir als registres (`log.tag.FA`, `FA-SVC` i `FirebaseCrashlytics` en detall), amb una prova de control en «Sí» per confirmar que el mètode veu les pujades quan n'hi ha.

**Amb una sola instància (la idea inicial), durant el «No» no surt res, però el que es recull llavors surt després.**

- Analytics diu «App measurement disabled via the manifest» i «Event not sent since app measurement is disabled», i el servei de mesura de Google Play no registra cap esdeveniment ni cap pujada de l'app. Crashlytics diu «automatic data collection DISABLED».
- Tot i així, Crashlytics **captura els crashes i els desa al disc** («Waiting for send/deleteUnsentReports to be called»). Quan l'usuari passa a «Sí», els envia: «Sending cached crash reports». Són crashes de quan havia dit «No», i sense passar pel `SanitizingExceptionHandler`, que només s'instal·la des de `FirebaseGateway.enable()`.
- Analytics desa l'hora de la primera obertura en inicialitzar-se i, amb el «Sí», envia `first_open` amb aquella hora i un `app_exception` amb l'hora del crash antic.

**Amb una segona instància amb nom (`FirebaseApp.initializeApp(context, FirebaseOptions.fromResource(context), "sync")`), Analytics i Crashlytics no arrenquen.**

- Zero línies d'Analytics i de Crashlytics als registres, cap fitxer de Crashlytics al disc després d'un crash provocat.
- Auth (`FirebaseAuth.getInstance(app)`), Firestore i App Check funcionen igual sobre la instància amb nom, i la sessió es conserva en reiniciar l'app.
- Les dues instàncies conviuen: amb el «Sí» s'inicialitza la per defecte al costat de `sync`; en tornar a «No» s'apaga la recollida i la sincronització continua escrivint.
- Cal un canvi a `AndroidFirebaseGateway`: avui mira si hi ha *alguna* instància (`FirebaseApp.getApps(context).isEmpty()`). Amb `sync` present creu que Firebase ja està inicialitzat i l'app peta en dir «Sí» («Default FirebaseApp is not initialized»). Ha de mirar la instància per defecte (`FirebaseApp.DEFAULT_APP_NAME`), tant a `enable()` com a `disable()`. Comprovat al spike.

**(B)** Es fa servir la segona instància. Així la regla d'avui es queda tal com és i no cal refiar-se d'esborrar a temps el que s'hagi recollit durant el «No».

**La resta de preguntes del spike:**

- **Inici de sessió:** Credential Manager 1.6.0 amb `googleid` 1.2.1 i `R.string.default_web_client_id` (el genera el plugin a partir del `google-services.json` nou) cap a Firebase Auth. Funciona amb l'empremta de debug. La primera vegada Google mostra el seu propi full: «Google will share your name, email address, and profile picture». No s'ha provat amb la clau de signatura de Play.
- **App Check:** el proveïdor de debug escriu un token al logcat a cada instal·lació. Es registra amb `firebase appcheck:debugtokens:create <token> --app <appId>`. Sense registrar, el servidor torna 403 («App attestation failed»), l'SDK fa servir un token de substitució i les crides passen mentre App Check no es forci.
  - **Forçat (11 de novembre de 2026):** amb el token registrat, l'app inicia sessió i escriu. Amb un token sense registrar (dades de l'app esborrades), ja falla l'inici de sessió: «Firebase App Check token is invalid». Per tant App Check també es força a Authentication, no només a Firestore, i una app sense atestació no arriba ni a tenir sessió.
  - **Conseqüència que cal decidir (B):** amb App Check forçat, només poden sincronitzar les instal·lacions que Play Integrity reconeix. Un APK baixat de GitHub Releases (signat amb la clau de pujada) o un dispositiu sense serveis de Google previsiblement no passarien l'atestació. No s'ha provat; cal fer-ho amb una build de Play i una de GitHub abans de publicar.
  - Queda per provar: Play Integrity en una build distribuïda per Play, i cada emulador o dispositiu de proves necessita el seu token de debug registrat.
- **Regles:** l'escriptura i la lectura a `users/{uid}/spike/ping` funcionen; l'escriptura al document d'un altre usuari és rebutjada (`PERMISSION_DENIED`).
- **Versions:** el `firebase-bom` 35.1.0 del projecte ja és l'últim estable i porta Auth 25.0.0, Firestore 27.1.0 i App Check 20.0.0. No cal pujar-lo.
- **Manifest fusionat:** un permís nou, `com.google.android.providers.gsf.permission.READ_GSERVICES`, que ve de `com.google.android.recaptcha` (dependència de Firebase Auth). Activitats noves: les ocultes de Credential Manager, `SignInHubActivity`, `GenericIdpActivity` i `RecaptchaActivity` de Firebase Auth, i `PlayCoreDialogWrapperActivity`. Serveis nous: `CredentialProviderMetadataHolder` i `RevocationBoundService`. Cap permís de càmera ni de notificacions.
- **Servidors contactats i identificadors nous: no mesurat.** La captura de xarxa de l'emulador no va recollir res d'útil i no hi havia cap proxy instal·lat. Cal fer-ho abans de tocar la política de privacitat.

**Per netejar quan s'acabi el spike:** l'usuari de Firebase Auth creat amb el compte de Bernat, el document `users/{uid}/spike/ping` i el token de debug «Emulador 37_Pixel_Phone (spike)».

### Claus

- **Secret de recuperació:** 128 bits aleatoris, generats al primer dispositiu. Les **paraules de seguretat** en són la forma llegible: 12 paraules de la llista anglesa de BIP39, amb la seva suma de control **(C)**. El QR porta el mateix secret.
- **Clau de dades:** 256 bits aleatoris. Xifra tots els registres. Es puja al document del compte **embolcallada** amb una clau derivada del secret (HKDF-SHA256). Paraules incorrectes fan fallar el desembolcallat, i així es detecten. Canviar de paraules en el futur no obligaria a tornar a xifrar-ho tot.
- **Al dispositiu:** el secret i la clau de dades es guarden en un fitxer xifrat amb una clau AES de l'Android Keystore, no exportable. El fitxer queda fora del backup d'Android, com la base de dades. Consultar les paraules o mostrar el QR demana `BiometricPrompt` (`BIOMETRIC_STRONG` o la credencial del dispositiu) i `FLAG_SECURE`, sempre, també al primer dispositiu just després d'iniciar sessió.
- **(D) Pantalles amb la clau a la vista:** `FLAG_SECURE` a les paraules (13f), a escriure-les (13j) i al QR (13p). El QR caduca en 5 minuts i se'n genera un de nou en tocar-lo. En copiar les paraules, el porta-retalls es marca com a sensible (`ClipDescription.EXTRA_IS_SENSITIVE`) i s'esborra al cap d'1 minut. Els camps per escriure-les no tenen autocorrecció ni aprenentatge del teclat (`IME_FLAG_NO_PERSONALIZED_LEARNING`). Les paraules es mostren sempre en anglès; la interfície, traduïda.
- **Registres:** AES-256-GCM, nonce aleatori de 12 bytes per xifratge. Les dades associades (AAD) inclouen l'UID, l'identificador del registre i la versió del format, perquè un registre no es pugui moure a un altre lloc ni a un altre compte.
- **Biblioteca:** primitives de la plataforma (`javax.crypto`), sense Tink **(C)**. Cap criptografia inventada.

**Construït (fase 2), a `data/sync/crypto`.** És Kotlin pur, sense Android ni Firebase, i encara no el crida ningú:

| Peça | Què fa | Comprovat amb |
| --- | --- | --- |
| `Bip39` | Entropia de 128 bits ↔ 12 paraules. En llegir-les accepta majúscules i espais, i diu quines posicions no són a la llista o si falla la suma de control | Vectors de referència de BIP39 i el SHA-256 de la llista oficial |
| `Hkdf` | HKDF-SHA256 | RFC 5869, casos 1 i 3 |
| `AesGcm` | AES-256-GCM; el blob és `nonce (12 bytes) ‖ text xifrat i etiqueta (16 bytes)` | Vector del NIST |
| `RecoverySecret`, `DataKey` | El secret i la clau de dades; no surten mai en un `toString()` | |
| `KeyWrap` | Embolcalla la clau de dades. Unes paraules equivocades o un altre compte tornen `null` | |
| `RecordCipher` | Xifra i desxifra un registre per al seu compte i el seu identificador | |

Decisions preses en construir-ho **(C)**:

- Plataforma i no Tink: tot el que cal ja hi és, i HKDF són quinze línies sobre HMAC comprovades amb els vectors de l'RFC. Tink afegiria una dependència i pes a l'APK.
- La clau d'embolcall és `HKDF-SHA256(secret, sal buida, info = "converses/sync/v1/wrap")`. El secret ja té 128 bits d'entropia, i per això no cal cap derivació lenta com les de les contrasenyes.
- Els registres no es xifren amb la clau de dades directament, sinó amb una clau derivada només per a això: `HKDF-SHA256(clau de dades, sal buida, info = "converses/sync/v1/record")`. Així la clau de dades pot servir més endavant per a una altra cosa (fitxers adjunts, per exemple) sense que una mateixa clau faci dues feines ni calgui tornar-ho a xifrar tot.
- La versió del format (`SYNC_FORMAT = 1`) no va dins del blob: va a les dades associades i al camp `format` del compte. `KeyWrap` i `RecordCipher` la reben com a paràmetre, per poder llegir un format antic el dia que n'hi hagi un de nou.
- El format està fixat per dos tests amb valors coneguts (`theWrappedFormatIsFrozen`, `theRecordFormatIsFrozen`): si algú canvia la derivació, l'ordre dels camps o la versió, fallen. Un cop hi hagi dades al núvol, no es toquen sense una versió nova i un camí per llegir l'antiga.
- `Bip39.split` és l'única manera de partir una frase enganxada (espais, salts de línia, comes o numerada). Les paraules es comparen només per les seves lletres, i així no hi fan res les majúscules ni els caràcters invisibles que s'enganxen en copiar.
- Amb un nonce aleatori de 96 bits i una sola clau, el límit recomanat pel NIST és de 2³² xifratges; a 10.000 escriptures diàries, més de mil anys. «Començar de nou amb paraules noves» canvia la clau.
- Les dades associades són camps prefixats per la seva llargada: `("wrap", format, uid)` per a la clau embolcallada i `("record", format, uid, recordId)` per als registres. Dos conjunts de camps diferents no poden donar mai els mateixos bytes.
- La llista de paraules és un fitxer Kotlin (`Bip39Words.kt`), no un recurs, perquè el paquet continuï sent JVM pur.

- **Començar de nou amb paraules noves:** genera un secret i una clau de dades nous, esborra els registres del núvol, puja-ho tot de nou des del local i substitueix `wrappedKey`. Els altres dispositius, en no poder desembolcallar la clau nova, passen a l'estat `NeedsKey` i demanen les paraules noves; no perden res en local.

El Keystore no sobreviu a un factory reset, a una desinstal·lació ni a un «esborra dades». En tots tres casos el dispositiu torna com a nou i necessita les paraules o el QR.

### Què es guarda a Firestore

```
users/{uid}
  wrappedKey      bytes    clau de dades embolcallada
  format          int      versió del format
  plan            string   "free" | "pro", el que declara l'app
  devices         map      { deviceId: { name: bytes xifrats, addedAt, lastSyncAt } }
  createdAt, lastSyncAt    timestamps

users/{uid}/records/{recordId}
  c               bytes    registre xifrat (si està esborrat, només la data de l'esborrat, xifrada)
  deleted         bool
  by              string   deviceId que l'ha escrit
  updatedAt       timestamp del servidor
```

- `recordId` és un UUID aleatori. El **tipus** (persona, entrada, tasca, canal propi) va dins del xifrat: tots els registres són al mateix sac.
- El contingut xifrat és JSON (`kotlinx.serialization`, ja al projecte) amb el tipus, els camps de l'entitat, la data de modificació del client i les referències a altres registres per `recordId`, no per l'`id` local.
- Els **fets fixats** (`pinned_fact`) viatgen dins del registre de la seva persona, perquè l'app ja els desa en bloc **(C)**.
- **(B)** El que Bernat veu: correu, UID, pla, nombre de dispositius, nombre de registres, mides i dates. Res més.
- `deviceId` és un UUID aleatori de la instal·lació, no cap identificador del maquinari **(C)**.

### Regles de Firestore

Les regles viuen al repositori, a `firestore.rules` (amb `firebase.json` i `.firebaserc`), i es despleguen amb la CLI de Firebase: `firebase deploy --only firestore:rules`. No s'editen a la consola: el fitxer és la font de veritat i els canvis passen per git. Ara hi ha només la regla del spike (cadascú, el que hi ha sota el seu usuari); el que segueix és el que hi ha d'arribar amb les fases 4 i 5.

- Només `request.auth.uid == uid` llegeix i escriu sota `users/{uid}`.
- `devices` no pot **créixer** per sobre de 2 entrades amb `plan == "free"` ni de 5 amb `"pro"`. El límit només es comprova quan s'afegeix un dispositiu: un compte que baixa de Pro amb 5 dispositius ha de poder continuar actualitzant el document.
- Qualsevol sessió del propietari pot editar `devices`, encara que el seu dispositiu no hi sigui: és el que permet al que entra treure'n un altre.
- Un registre només es pot escriure si `by` és a `devices`. Això fa efectiva l'expulsió al servidor, no només a l'app. Costa una lectura addicional per escriptura; cal confirmar-ne l'impacte en el cost.
- Forma fixa dels documents i mida màxima de `c` (proposta: 64 KB **(C)**).
- **App Check** amb Play Integrity obligatori a Firestore.
- No hi ha límit de registres per compte: les regles no saben comptar. El control és App Check, la mida per registre i les alertes de pressupost **(C)**.

### Canvis a Room (v5 → v6)

**Construït (fase 1).** És l'única part de la sincronització que ja existeix:

- `syncId` (un UUID, únic) a `person`, `entry`, `task` i `custom_channel`. `Migration5To6` n'omple un per a cada fila que ja hi era.
- `dirty` («pendent de pujar») a les mateixes taules, i `modifiedAt` a `entry`, `task` i `custom_channel`. La data de modificació de la persona és el seu `updatedAt`. Les files que ja hi eren queden pendents, amb `occurredAt` (entrades), `COALESCE(doneAt, createdAt)` (tasques) i `createdAt` (canals) com a data **(C)**.
- Una taula `sync_tombstone` (`syncId`, `deletedAt`, `dirty`) que recorda els esborrats fins que s'hagin pujat. Esborrar una persona n'hi afegeix també per a les seves entrades i tasques; els fets fixats no en tenen, perquè viatgen dins de la persona.

Els `id` locals no canvien: la UI, les rutes i les claus foranes continuen igual. Sense sessió res d'això es veu, i per això es pot publicar abans que la resta.

**(B)** `dirty` es marca als DAOs i al repositori, no amb triggers de SQLite: els triggers no es poden provar a la JVM, que és el que executa el CI, i una `AutoMigration` que recreï una taula els esborraria en silenci.

- Tota escriptura d'una entitat surt de `ConversesRepository` segellada (`dirty` i la data), encara que la pantalla l'hagi carregat neta.
- Els canvis en bloc (marcar una tasca com a feta, moure les entrades d'un canal a un altre grup) segellen a l'SQL.
- L'únic esborrat públic de cada DAO és una transacció que deixa el tombstone. Esborrar un canal propi també segella les entrades que el feien servir, perquè passen a no tenir canal.
- Tornar a inserir una entrada esborrada («Desfés») treu el seu tombstone i la segella amb l'hora actual, perquè una edició posterior guanyi a l'esborrat **(C)**.
- `SyncIdentityTest` recorre els mètodes públics del repositori i falla si n'hi ha cap d'escriptura sense classificar.

Queda per a la fase 4:

- L'estat del motor (cursor de l'última baixada) anirà a un DataStore, no a Room **(C)**.
- Sense sessió els tombstones s'acumulen, una fila petita per esborrat. El motor els neteja en pujar-los, i la pujada inicial del primer dispositiu els pot llençar tots, perquè al núvol encara no hi ha res per esborrar **(C)**.
- `SampleData` i «esborra-ho tot» del menú de debug escriuen pels DAOs, sense passar pel repositori; només existeixen en debug.

### Motor

- **Pujar:** després d'una escriptura local, amb un retard curt per agrupar canvis, i amb WorkManager si l'app es tanca abans. Lots de Firestore.
- **Baixar:** en obrir l'app i en tornar al primer pla, amb un listener sobre els registres posteriors al cursor mentre l'app és visible. Amb l'app tancada no es baixa res **(C)**. A la tauleta, les dades arriben en obrir-la, no abans.
- **Cursor:** `updatedAt` del servidor. Cada baixada torna a demanar un petit marge anterior al cursor, perquè dues escriptures simultànies poden quedar desordenades; aplicar un registre dues vegades no fa res.
- **Conflictes (C):** guanya la modificació més recent segons la data del client que va dins del xifrat; en cas d'empat, el `deviceId` més gran. Un esborrat guanya a una edició anterior. Es resol per registre sencer, no per camp. El risc acceptat és un rellotge mal posat en un dispositiu.
- **Referències:** una entrada que arriba abans que la seva persona espera en una cua local fins que la persona hi és.
- **Duplicats amb regla d'unicitat:** dos canals propis amb el mateix nom i grup creats en dos dispositius es fusionen en un i les entrades s'hi remapen. Dos àlies iguals en persones diferents es toleren **(C)**.
- **Primera sincronització d'un dispositiu amb dades:** unió del local i del núvol. No es dedupliquen persones amb el mateix nom **(C)**: si l'usuari les havia creat a tots dos llocs, en veurà dues.
- **Dispositiu tret:** el motor ho descobreix en llegir `devices` o en rebre un rebuig de les regles, s'atura i passa a l'estat `Removed`.
- **Errors:** reintents amb espera creixent. `trackError` amb llocs nous a `ErrorSites.kt`, sense `message`, com la resta.

**Construït (fase 4), branca `sync-fase-4`.** Pla a `plans/fase-4-motor.md`. Tot continua darrere de `SyncFlag`; Room no canvia.

| Peça | Què fa |
| --- | --- |
| `record/` (`SyncRecord`, `RecordCodec`) | El JSON de cada tipus (persona amb els seus fets fixats, entrada, tasca, canal propi) i l'esborrat (`{ deletedAt }`). Referències per `syncId`. Format fixat per un test |
| `RoomLocalSyncSource` | El costat de Room: pendents, marcar nets només si no han canviat, aplicar el que baixa sense segellar |
| `engine/` (`RecordPusher`, `RecordPuller`, `ConflictRules`) | Baixar abans de pujar, pujar per lots de 200 i baixar per pàgines, amb els conflictes, els esborrats, les referències que esperen i els canals duplicats |
| `FirestoreSyncRemote` | `users/{uid}/records`, el listener i el dispositiu a `devices`. Una pujada que el servidor no confirma en 20 s és «sense connexió» |
| `SyncScheduler`, `SyncLoop`, `SyncWorker` | Baixar en obrir i amb el listener, pujar 2 s després de l'últim canvi, reintents (5 s, 15 s, 1 min, 5 min), i WorkManager en passar a segon pla amb canvis pendents |
| `SyncRepository` | `status` (al dia, sincronitzant, sense connexió amb pendents, error) i `initialSync` (pujada, baixada, fusió) |
| `ui/sync` | Pujada i baixada inicials (13g, 13m2), «Sincronització activada» (13h, només el dispositiu), fusió (13m) i l'estat a 13o i a l'indicador |

Decisions preses en construir-ho **(C)**:

- Un esborrat no deixa `c` buit: hi va xifrat `{ deletedAt }`, perquè sense la data del client no es pot decidir si guanya a una edició.
- Esborrar una persona en un dispositiu esborra en cascada les seves entrades i tasques als altres; una entrada nova feta en un altre dispositiu abans de baixar l'esborrat es perd.
- «Amaga» deixa la feina en marxa sense notificació **(B)**; en acabar no torna a ensenyar el resultat, que ja diu l'estat de 13o.
- WorkManager no s'inicialitza sol (ConversesApp és `Configuration.Provider`) i se li treuen `RECEIVE_BOOT_COMPLETED` i `FOREGROUND_SERVICE`: el que quedi pendent després de reiniciar el mòbil puja en obrir l'app. Cap permís nou.
- L'analítica de la pujada i la baixada inicials (`sync_upload_finished`, `sync_download_finished`, `sync_merge_shown`) només compta mentre la pantalla de Sincronització existeix.
- El nom del dispositiu és `Settings.Global.DEVICE_NAME` o, si no n'hi ha, `Build.MODEL` (a l'emulador, `sdk_gphone16k_arm64`).

Comprovat amb tests JVM (motor amb dos dispositius simulats, repositori, bucle, accions), 23 tests de les regles a l'emulador de Firestore, i a l'emulador d'Android (12 de novembre de 2026): activar amb dades (tot pujat, res pendent), tornar a entrar amb les paraules en un dispositiu amb les mateixes dades (fusió sense duplicats, la persona protegida continua protegida), entrar en un dispositiu buit (persones, entrades i fets idèntics als originals), una persona nova que puja sola, i sense connexió («Sense connexió · 1 canvi pendent» i, en tornar la xarxa, pujat).

Comprovat després amb el servidor real i dos emuladors (12 de novembre de 2026):

- **Regles desplegades** a `converses-72251`. Una pujada de 250 registres (un lot de 200 i un de 50) passa amb les regles, així que el `get()` de `users/{uid}` per registre no topa amb el límit d'accessos: els lots es queden a 200.
- **Dos dispositius** (telèfon i tauleta): la tauleta entra amb les paraules i baixa una base de dades idèntica; una persona creada en un apareix a l'altre en uns segons i esborrar-la en un la treu de l'altre.
- **Una persona esborrada des d'un altre dispositiu** deixava la fitxa oberta en blanc. Ara la fitxa, en veure que la persona ja no hi és, treu de la navegació tot el que en depèn (`TopLevelBackStack.forgetPerson`), i l'esborrat local fa servir el mateix.
- **Edicions simultànies:** la mateixa persona reanomenada a tots dos dispositius, un sense xarxa. Si edita més tard el que no té xarxa, guanya ell en tornar la connexió. En el cas contrari, abans quedava cadascú amb la seva, perquè Firestore escriu sense mirar què hi ha i el dispositiu que tornava trepitjava al núvol l'edició més nova amb la seva. Ara hi ha dues correccions. `pushPending` baixa abans de pujar, perquè el conflicte es resolgui al dispositiu i només pugi el que guanya. A més, si una baixada porta un registre d'un altre dispositiu que perd contra una fila local ja pujada, la fila torna a quedar pendent (`ConflictRules.mustReassert`, `markDirty`) i el guanyador torna al núvol. Una divergència que havia quedat d'abans es va arreglar sola en obrir l'app amb la versió nova. Tots dos casos convergeixen a la versió més nova, i passa el mateix amb un esborrat que trepitja una edició posterior.

Queda per a la fase 5: tancar la sessió buida el DataStore `sync` i genera un `deviceId` nou cada vegada, i a `devices` s'hi acumulen entrades velles. És el primer punt de la fase 5, amb el límit de dispositius i la pantalla de dispositius.

### Persones protegides

Es sincronitzen com qualsevol altra: `isProtected` va dins del xifrat i el dispositiu que les rep les manté protegides. El motor treballa a la capa de dades sense desbloquejar res a la UI. Cap text de la sincronització (progrés, errors, llista de dispositius) mostra noms de persones.

### Compte

- **Inici de sessió:** Credential Manager amb «Sign in with Google», i el token cap a Firebase Auth. Versions a comprovar a la documentació oficial en el moment d'afegir-les.
- **Pla:** l'app escriu `plan` segons `ProRepository.isPro`. En baixar de Pro amb més de 2 dispositius no s'expulsa ningú; simplement no se'n poden afegir més **(C)**, igual que la resta de la baixada de Pro.
- **Tancar sessió:** treu el dispositiu de `devices`, esborra la clau local, atura el motor. Les dades i els `syncId` es queden.
- **Esborrar el compte:** esborra els registres per lots, el document del compte i l'usuari d'Auth (pot demanar tornar a iniciar sessió). Play també exigeix una **pàgina web** per demanar l'esborrat: va a ConversesLanding, i Bernat l'executa a mà.
- **Comptes inactius (B):** 24 mesos sense sincronitzar; es poden esborrar, a mà. `lastSyncAt` ho fa possible.

**Construït (fase 3), a `data/sync` i `ui/sync`.** Compte i clau, tot darrere de `SyncFlag` (apagat per defecte). Encara no es puja ni es baixa cap registre: això és la fase 4. Amb el flag apagat l'app és la d'abans: sense indicador, sense fila a Ajustos, l'onboarding amb els textos de sempre i Firebase `sync` sense inicialitzar.

| Peça | Què fa |
| --- | --- |
| `SyncFirebase` | La instància `sync` de Firebase (Auth, Firestore, App Check), a part de la per defecte. No es crea fins que algú la demana, i amb App Check instal·lat abans de la primera crida: Play Integrity en release, el proveïdor de debug en debug |
| `FirebaseAuthGateway` | Credential Manager amb «Continua amb Google» i el token cap a Firebase Auth. Tancar el full no és cap error; sense connexió es distingeix d'un error. Tancar la sessió no peta mai |
| `FirestoreAccountRemote` | El document `users/{uid}` amb `wrappedKey`, `format` i `createdAt`. Es llegeix sempre del servidor, no de la memòria cau. Es crea en una transacció que no escriu si ja existeix |
| `AndroidSyncKeyStore`, `StoredKeysCodec` | El secret i la clau de dades en un fitxer a `noBackupFilesDir`, xifrat amb una clau AES-GCM de l'Android Keystore. Un fitxer que no es pot llegir és com si no hi fos |
| `DataStoreSyncLocalStore` | La marca de sessió i la data de l'última revisió de les paraules (DataStore `sync`, de cada dispositiu) |
| `SyncRepository` | L'estat i les accions; la resta de l'app no sap res de Firebase |

Estats de `SyncRepository`:

- `SignedOut`: sense sessió; res surt del dispositiu.
- `Checking`: recuperant la sessió en arrencar o iniciant-la.
- `Activating`: primer dispositiu, amb les paraules generades i res pujat.
- `NeedsKey`: el compte ja té dades i aquest dispositiu no en té la clau.
- `Ready`: sessió i clau al dispositiu.

Pantalles, totes amb el tema de l'app i sense mostrar mai noms ni contingut de persones:

- **Sense sessió:** galeta, tres files, el botó de Google i una nota al peu. Si falla, un snackbar diu si és per falta de connexió o un error.
- **Activació (primer dispositiu):** demana l'empremta (`BiometricPrompt`) i ensenya les 12 paraules **juntes, en un sol bloc de text**, amb «Copia», una nota i, al peu, la casella «Les he guardat» i «Continua». Només amb la casella marcada es puja la clau embolcallada i es desa al dispositiu. Cancel·lar l'empremta, tancar la pantalla o morir el procés abans de continuar tanca la sessió i no deixa res al núvol.
- **Clau en un dispositiu nou (`NeedsKey`):** la targeta «Escriu les paraules» i el compte amb «Canvia», que tanca la sessió. Les paraules s'**escriuen en un sol camp**, no en dotze: el teclat no té autocorrecció ni aprenentatge, hi ha suggeriments de `Bip39`, les paraules desconegudes es marquen i es pot enganxar tot d'una (`Bip39.split`). Unes paraules vàlides que no obren el compte, o d'un altre compte, donen «no obren les dades d'aquest compte» i el dispositiu es queda a `NeedsKey`.
- **Amb sessió:** el compte, «Paraules de seguretat» amb la data de la revisió i «Tanca la sessió» amb confirmació. Veure les paraules demana l'empremta de nou.
- **Indicador i entrades:** un indicador a la dreta del menú de la llista de persones (sense sessió, amb sessió, «falten les paraules»), que a pantalla gran és al peu del rail amb l'etiqueta «Compte»; la secció «Compte» d'Ajustos amb la fila «Sincronització»; i, a l'onboarding, l'enllaç «Ja tinc dades al núvol» amb el text del pas 3 canviat.
- Les rutes són `SyncRoute`, `SecurityWordsRoute(activation)` i `EnterWordsRoute`; les dues últimes són formularis (`isFormRoute()`), diàlegs de 560 dp en pantalla gran.

Decisions preses en construir-ho **(C)**:

- Firebase `sync` només s'inicialitza si el dispositiu té una sessió marcada o quan l'usuari toca «Continua amb Google». Iniciar sessió no engega Analytics ni Crashlytics: `AndroidFirebaseGateway` només mira la instància per defecte, no la `sync`.
- La clau del Keystore que xifra el fitxer no demana autenticació, perquè el motor l'ha de poder llegir sol. L'empremta la demana la UI, i el repositori no dona les paraules si abans no s'han desbloquejat (`unlockWords`), tampoc després de restaurar el procés; en sortir de la pantalla tornen a quedar bloquejades.
- Les paraules van amb `FLAG_SECURE`, el porta-retalls es marca com a sensible i s'esborra al cap d'1 minut, i el camp d'escriure-les no té aprenentatge del teclat.
- Un procés mort a mitja activació deixa una sessió sense clau ni document: en arrencar, es tanca. Si en arrencar no hi ha connexió per saber-ho, es queda a `NeedsKey` i es resol en donar les paraules.
- Dos dispositius que s'activen alhora no es trepitgen la clau: el segon troba el document, passa a `NeedsKey` i demana les paraules del primer.
- Un dispositiu que havia tancat la sessió sempre demana les paraules en tornar a entrar, encara que el Keystore en conservés la clau.
- Tancar la sessió és, ara, la forma mínima: tanca Auth, esborra la clau local i la marca de sessió, i les dades es queden. Alliberar la plaça arriba amb la fase 5.
- L'avatar de l'indicador és la inicial del nom (o del correu) i no la foto de Google: carregar-la demanaria una biblioteca d'imatges i una connexió més.
- Permís nou al manifest fusionat: `READ_GSERVICES`, de reCAPTCHA (dependència de Firebase Auth). Surt de l'spike.

**Sense QR a la fase 3 (B, 11 de novembre de 2026).** Les targetes i files que hi porten no es dibuixen; el QR passa a una fase 3b. Tampoc hi són «No tinc les paraules» («Comença de nou», que necessita el motor), l'estat «Al dia», els dispositius, esborrar el compte ni el dispositiu tret.

**Analítica:** les pantalles `sync`, `sync_words` i `sync_enter_words`, set accions `sync_*` amb valors tancats (catàleg a `docs/analytics/README.md`) i els errors `sync_sign_in`, `sync_account` i `sync_key_store`. Mai el compte, el correu ni les paraules. Com la resta, només amb el «Sí» a les estadístiques: no envia cap dada nova.

Comprovat amb tests JVM (`SyncRepositoryTest`, `StoredKeysCodecTest`, `SyncViewModelTest`, `SecurityWordsViewModelTest`, `EnterWordsViewModelTest`, `SyncIndicatorTest`) amb falsos de les quatre portes (`FakeSync`), i amb captures de pantalla de cada pantalla.

**Provat a l'emulador** amb el flag encès (11 i 12 de novembre de 2026): activar, tancar la sessió, tornar a entrar amb les paraules i amb paraules equivocades, i tancar les paraules abans de «Continua» (la sessió es tanca i, en tornar a entrar, el compte torna a l'activació: al núvol no hi ha quedat res). La prova va trobar que un segon inici de sessió des de la mateixa pantalla es quedava esperant l'empremta, i que sortir de «Sincronització» a mitja activació la deixava oberta. Ara l'empremta es demana una vegada per inici de sessió (`SyncViewModel.takeActivationPrompt`) i sortir de la pantalla cancel·la l'activació. El proveïdor de debug d'App Check escriu un token al logcat que cal registrar a la consola de Firebase.

Queda per a la fase 7:

- **APK de GitHub:** amb App Check forçat, només passen les instal·lacions que Play Integrity reconeix, i previsiblement un APK de GitHub no podrà iniciar sessió. Es provarà més endavant. **(B, 12 de novembre de 2026)** Els dispositius sense serveis de Google no importen: s'accepta que no puguin sincronitzar.
- La política de privacitat i el formulari de Play (fase 7, abans de treure el flag) i mesurar els servidors contactats.

## Privacitat externa

Surt del telèfon, només si l'usuari inicia sessió (opcional):

| Dada | A qui | Per a què |
| --- | --- | --- |
| Correu, nom i foto del compte de Google, UID | Firebase Auth | Identificar de qui són les dades |
| Registres xifrats, nombre, mides i dates | Firestore | Sincronitzar |
| Nom del dispositiu (xifrat), nombre de dispositius | Firestore | Places |
| Si és Pro | Firestore | Límit de places; associat al correu per primera vegada |
| Atestació de Play Integrity | App Check | Evitar abús |
| IP | Google | Inherent a la connexió |

Permisos nous, que no fan sortir cap dada del telèfon **(D)**:

- `CAMERA`, per llegir el QR d'un altre dispositiu. Es demana en tocar «Llegeix el QR», no abans; la imatge es processa al dispositiu i no es guarda. Sense el permís, queda l'opció d'escriure les paraules.
- `POST_NOTIFICATIONS`, per a la notificació de progrés de la pujada o la baixada en segon pla. Cal mirar al manifest fusionat si alguna biblioteca ja l'afegeix.

Cal, abans de publicar:

- **Política de privacitat** a ConversesLanding (`privacy/`, `ca/privacitat/`, `es/privacidad/`): compte opcional, què es puja i què no, xifratge d'extrem a extrem, retenció («es poden esborrar després de 24 mesos sense activitat»), com esborrar el compte. Text a proposar i aprovar per Bernat.
- **Formulari de seguretat de dades de Play** (només Bernat): adreça de correu i identificadors d'usuari, recollits, opcionals, per a la funcionalitat de l'app i la gestió del compte; esborrat de dades disponible. Cal comprovar com es declara el contingut xifrat d'extrem a extrem, i si el permís de càmera (ús només local) hi canvia alguna resposta.
- `docs/pro/README.md`: Pro deixa de ser «sense compte» a seques; el compte és opcional i només per sincronitzar.
- Catàleg d'analítica: pantalla `sync` i accions amb valors tancats (activar, tancar sessió, treure dispositiu, esborrar compte), només amb consentiment.

## Fases

Cada fase es valida abans de la següent. De la 1 a la 6 van darrere d'un feature flag (`data/flags`).

0. **Spike** (es llença): inicialitzar Firebase sense consentiment d'estadístiques, inici de sessió amb Google, App Check, i comprovar que no surt res cap a Analytics ni Crashlytics. **Fet**, tret de mesurar els servidors contactats; vegeu «Resultat del spike».
1. **Room v6:** identitat de sincronització, migració i test. Publicable sola.
2. **Criptografia:** paraules, claus, registres. JVM pur, amb vectors de prova.
3. **Compte i clau:** iniciar sessió, generar i guardar la clau, paraules, QR, magatzem al Keystore.
4. **Motor:** pujar, baixar, fusionar; regles de Firestore.
5. **Dispositius:** places, triar qui surt, dispositiu tret, pla.
6. **Compte:** tancar sessió, esborrar el compte, pàgina web d'esborrat.
7. **Publicar:** política de privacitat, formulari de Play, treure el flag, notes de versió, valorar el Baseline Profile.

## Tests

- JVM, com la resta: criptografia amb vectors coneguts; motor i fusió amb `FakeDb` i un `SyncRemote` fals (conflictes, esborrats, referències que arriben desordenades, canals duplicats, dispositiu tret).
- Migració v5 → v6 amb `MigrationTestHelper`.
- Regles de Firestore amb l'emulador. Afegeix una eina nova al CI; el pla decideix si val la pena o es proven a mà **(C: val la pena, són la frontera de seguretat)**.
- Dos emuladors per a la prova d'extrem a extrem de cada fase.

## Queda per al pla

- Biblioteques i versions exactes (Auth, Firestore, App Check, Credential Manager).
- Format exacte del JSON de cada tipus de registre i com evoluciona (`format`).
- Retard d'agrupació de pujades i política de reintents.
- Biblioteca per llegir i generar el QR, i què porta exactament a dins (secret i caducitat).
- On es guarda la data de l'última revisió de les paraules (és de cada dispositiu o del compte).
- Rebase sobre `main` abans de tocar codi: el rail i `docs/pantalles-grans` hi són, i la secció 11 de `disseny/INSTRUCCIONS.md` en depèn.

## Queda per a Bernat

- Res pendent. **(B)** Firestore està creat a la regió `eur3` (multiregió europea), que ja no es pot canviar, amb Authentication (Google) i App Check activats.
