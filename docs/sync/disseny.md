# Sincronització al núvol: disseny

Estat: **proposta; només hi ha construïdes les fases 1 i 2** (identitat de sincronització a Room v6, vegeu «Canvis a Room», i criptografia, vegeu «Claus»). Escrit el 10 de novembre de 2026 a partir de la conversa amb Bernat. Quan la funció existeixi, aquest fitxer es condensa en un `README.md` que descrigui el que hi ha, com la resta de `docs/`.

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

**(B)** Firebase es pot inicialitzar sense el consentiment d'estadístiques; el que depèn del consentiment és **enviar** dades d'ús. Per tant hi ha una sola instància de Firebase, la per defecte:

- S'inicialitza quan hi ha sessió de sincronització o consentiment d'estadístiques, el que arribi primer **(C: no a cada arrencada per a qui no fa servir cap de les dues coses)**.
- La recollida d'Analytics i de Crashlytics continua apagada per defecte i només s'encén amb el «Sí». `FirebaseController` deixa de decidir si Firebase existeix i passa a decidir només si es recull.
- En passar de «Sí» a «No» amb sessió iniciada, s'apaga la recollida i es descarten els pendents com ara, però Firebase continua viu per a la sincronització.

El spike de la fase 0 ho ha de comprovar mirant el trànsit de xarxa: amb sessió iniciada i estadístiques en «No», no ha de sortir res cap a Analytics ni Crashlytics. Si en sortís, l'alternativa és una segona instància de `FirebaseApp` amb nom només per a Auth, Firestore i App Check.

La regla de `CLAUDE.md` i de `docs/analytics` passa a dir: «Analytics i Crashlytics no recullen ni envien res sense consentiment. Firebase només s'inicialitza amb consentiment o amb sessió de sincronització».

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
  c               bytes    registre xifrat (buit si està esborrat)
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

### Persones protegides

Es sincronitzen com qualsevol altra: `isProtected` va dins del xifrat i el dispositiu que les rep les manté protegides. El motor treballa a la capa de dades sense desbloquejar res a la UI. Cap text de la sincronització (progrés, errors, llista de dispositius) mostra noms de persones.

### Compte

- **Inici de sessió:** Credential Manager amb «Sign in with Google», i el token cap a Firebase Auth. Versions a comprovar a la documentació oficial en el moment d'afegir-les.
- **Pla:** l'app escriu `plan` segons `ProRepository.isPro`. En baixar de Pro amb més de 2 dispositius no s'expulsa ningú; simplement no se'n poden afegir més **(C)**, igual que la resta de la baixada de Pro.
- **Tancar sessió:** treu el dispositiu de `devices`, esborra la clau local, atura el motor. Les dades i els `syncId` es queden.
- **Esborrar el compte:** esborra els registres per lots, el document del compte i l'usuari d'Auth (pot demanar tornar a iniciar sessió). Play també exigeix una **pàgina web** per demanar l'esborrat: va a ConversesLanding, i Bernat l'executa a mà.
- **Comptes inactius (B):** 24 mesos sense sincronitzar; es poden esborrar, a mà. `lastSyncAt` ho fa possible.

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

0. **Spike** (es llença): inicialitzar Firebase sense consentiment d'estadístiques, inici de sessió amb Google, App Check, i comprovar que no surt res cap a Analytics ni Crashlytics.
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
