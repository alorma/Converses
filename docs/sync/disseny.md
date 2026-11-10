# Sincronització al núvol: disseny

Estat: **proposta, res construït**. Escrit el 10 de novembre de 2026 a partir de la conversa amb Bernat. Quan la funció existeixi, aquest fitxer es condensa en un `README.md` que descrigui el que hi ha, com la resta de `docs/`.

Les decisions marcades **(B)** són de Bernat. Les marcades **(C)** les ha pres Claude en escriure el disseny i es poden canviar. Les pantalles les fa disseny a partir de `converses-sincronitzacio-brief-disseny.md`.

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

Resum; el detall de pantalles i estats és al brief de disseny.

- **(B)** Un indicador de compte a la capçalera del llistat de persones (anònim o avatar) i una fila a Ajustos porten a la pantalla «Sincronització».
- **(B)** L'onboarding ofereix iniciar sessió, com a opció i sense obligar: qui ve d'un reset recupera les dades abans de crear res.
- **Primer dispositiu:** iniciar sessió amb Google, veure les paraules de seguretat, confirmar que s'han guardat, pujada inicial.
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
- **Al dispositiu:** el secret i la clau de dades es guarden en un fitxer xifrat amb una clau AES de l'Android Keystore, no exportable. El fitxer queda fora del backup d'Android, com la base de dades. Consultar les paraules o mostrar el QR demana `BiometricPrompt` i `FLAG_SECURE`.
- **Registres:** AES-256-GCM, nonce aleatori de 12 bytes per xifratge. Les dades associades (AAD) inclouen l'UID, l'identificador del registre i la versió del format, perquè un registre no es pugui moure a un altre lloc ni a un altre compte.
- **Biblioteca:** primitives de la plataforma (`javax.crypto`) o Tink; ho decideix el pla després de mirar la versió estable actual **(C)**. Cap criptografia inventada.

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

- Només `request.auth.uid == uid` llegeix i escriu sota `users/{uid}`.
- `devices` no pot **créixer** per sobre de 2 entrades amb `plan == "free"` ni de 5 amb `"pro"`. El límit només es comprova quan s'afegeix un dispositiu: un compte que baixa de Pro amb 5 dispositius ha de poder continuar actualitzant el document.
- Qualsevol sessió del propietari pot editar `devices`, encara que el seu dispositiu no hi sigui: és el que permet al que entra treure'n un altre.
- Un registre només es pot escriure si `by` és a `devices`. Això fa efectiva l'expulsió al servidor, no només a l'app. Costa una lectura addicional per escriptura; cal confirmar-ne l'impacte en el cost.
- Forma fixa dels documents i mida màxima de `c` (proposta: 64 KB **(C)**).
- **App Check** amb Play Integrity obligatori a Firestore.
- No hi ha límit de registres per compte: les regles no saben comptar. El control és App Check, la mida per registre i les alertes de pressupost **(C)**.

### Canvis a Room (v5 → v6)

Avui les files tenen `id` autoincremental local i només `person` té `updatedAt`. Cal:

- `syncId` (text, únic) a `person`, `entry`, `task` i `custom_channel`. La migració n'omple un per a cada fila existent.
- `modifiedAt` i `dirty` a les mateixes taules (`person` ja té `updatedAt`, que es pot reaprofitar).
- Una taula `sync_tombstone` (`syncId`, `deletedAt`, `dirty`) per recordar els esborrats fins que s'hagin pujat. Esborrar una persona n'hi afegeix també per a les seves entrades i tasques.
- Una taula o DataStore petit per a l'estat del motor (cursor de l'última baixada).

Els `id` locals no canvien: la UI, les rutes i les claus foranes continuen igual. Com mana `CLAUDE.md`: pujar la versió, esquema exportat, `Migration` registrada a `di/AppModules.kt` i test amb `MigrationTestHelper`.

Aquesta migració es pot publicar abans que la resta, perquè sense sessió no té cap efecte visible.

On es marca `dirty`: al `ConversesRepository` o amb triggers de SQLite. Ho decideix el pla; el criteri és que cap camí d'escriptura (incloses les cascades i `changeGroup`) se'n pugui escapar.

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

Cal, abans de publicar:

- **Política de privacitat** a ConversesLanding (`privacy/`, `ca/privacitat/`, `es/privacidad/`): compte opcional, què es puja i què no, xifratge d'extrem a extrem, retenció («es poden esborrar després de 24 mesos sense activitat»), com esborrar el compte. Text a proposar i aprovar per Bernat.
- **Formulari de seguretat de dades de Play** (només Bernat): adreça de correu i identificadors d'usuari, recollits, opcionals, per a la funcionalitat de l'app i la gestió del compte; esborrat de dades disponible. Cal comprovar com es declara el contingut xifrat d'extrem a extrem.
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

- Biblioteques i versions exactes (Auth, Firestore, App Check, Credential Manager, Tink o plataforma).
- On es marca `dirty` (repositori o triggers).
- Format exacte del JSON de cada tipus de registre i com evoluciona (`format`).
- Retard d'agrupació de pujades i política de reintents.
- Com es mostra el progrés de la pujada i la baixada inicials.

## Queda per a Bernat

- **Regió de Firestore:** el projecte `converses-72251` encara no té Firestore activat, o sigui que no hi ha cap regió heretada: es tria en crear la base de dades a la consola i no es pot canviar. Proposta: una regió europea (**B:** «la del projecte»; cal mirar a la consola si el projecte en té una per defecte).
