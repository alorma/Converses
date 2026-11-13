# Converses · Sincronització al núvol: disseny (10 de novembre de 2026)

Resposta a l'encàrrec «Sincronització al núvol». La referència visual és `pantalles.html`: obre-la al navegador amb connexió, perquè les icones i la tipografia es carreguen de Google Fonts. Les mides són en dp i els colors fan servir els tokens del tema grafit (`bg`, `card`, `card-hi`, `s-high`, `s-highest`, `on`, `on-var`, `outline`, `outline-var`, `pri`, `on-pri`, `pri-c`, `on-pri-c`, `err`, `on-err`, `err-c`, `on-err-c`, `scrim`).

Totes les pantalles són un `Scaffold` amb `ConversesTopBar`, i fan servir el tema de l'app, no el color de cap persona. Els números de tauler (13a…13t, 0c) corresponen al canvas.

---

## 1. Preguntes obertes: proposta

1. **Onboarding:** sí. Al pas 3/3 (0c) hi ha un botó de text «Ja tinc dades al núvol», amb la icona `cloud_download`, 104 dp per sobre del peu. Obre el flux de dispositiu nou (13i) abans de crear cap dada.
   - També cal actualitzar el pas 3: els xips passen de «Sense núvol» i «Sense comptes» a «Xifrat» i «Compte opcional».
   - El text passa a ser «Les notes es queden al mòbil i, si les sincronitzes, viatgen xifrades. Si vols, protegeix una persona amb l'empremta i ningú més la podrà obrir.».
2. **Paraules:**
   - Decisió de Bernat d'11 de novembre de 2026, que substitueix la graella de `pantalles.html`: les 12 paraules es mostren juntes, una darrere l'altra, com un sol bloc de text dins una targeta, sense números.
   - Les paraules van en anglès; la interfície que les envolta, traduïda.
3. **Indicador i `more_vert`:** conviuen. Primer va `more_vert` i, a la dreta de tot, l'indicador (àrea tàctil de 48 dp, avatar de 40).
4. **Recordatori de les paraules:** no hi ha avisos.
   - La fila «Paraules de seguretat» de Sincronització diu quan es van revisar («Les vas revisar fa 4 mesos»).
   - Obrir-les reinicia el comptador.
5. **Nom dels dispositius:**
   - Per defecte, el model: `Settings.Global.DEVICE_NAME`, o `Build.MODEL` si no n'hi ha.
   - Es pot canviar des del menú de la fila, amb «Canvia el nom» i «Treu».

## 2. Indicador de compte (13a)

Va a la capçalera del llistat, a la dreta de `more_vert`. Té una àrea tàctil de 48 dp i la contentDescription «Compte i sincronització» seguida de l'estat. En tocar-lo s'obre Sincronització.

| Estat | Aspecte |
| --- | --- |
| Sense sessió | Cercle de 40 dp amb una vora d'1,5 dp en `outline-var` i la icona `cloud` (22 dp) en `on-var` |
| Sincronitzat | Avatar de Google de 40 dp, sense cap afegit |
| Sincronitzant | Avatar amb un arc de 2,5 dp en `pri` que gira sobre una pista en `outline-var`. No es mostra si la sincronització dura menys de 600 ms |
| Sense connexió o error | Insígnia de 18 dp a baix a la dreta, amb una vora de 2 dp en `bg`: `cloud_off` sobre `s-highest`, en `on-var`. **No és vermella** |
| Falten les paraules | Insígnia amb la icona `key`, sobre `pri` |
| Dispositiu tret | Insígnia amb la icona `priority_high`, sobre `err`, i l'avís fix (13c) |

## 3. Ajustos (13b)

- Secció nova «Compte» a dalt de tot, just sota la targeta de Converses Pro.
- Té una fila de 72 dp amb el títol «Sincronització», un subtítol que depèn de l'estat i `chevron_right`.
- Icones segons l'estat: `cloud`, `cloud_done`, `sync`, `cloud_off`, `key` o `sync_problem`.
- Si el dispositiu s'ha tret, la icona i el subtítol van en `err`.

## 4. Sense sessió (13d)

1. Galeta de 72 dp (`pri-c`) amb la icona `cloud_sync`.
2. Títol, en display de 26 sp, i text.
3. Tres files agrupades de 76 dp, cadascuna amb una icona dins d'un cercle de 40 dp: `lock`, `key` i `devices`.
4. Peu fix:
   - el botó de Google, de 56 dp (vegeu més avall);
   - una nota de 13 sp: «El compte només serveix per saber de qui són les dades. No pot desxifrar res.».

**Botó de Google:** fes servir el botó oficial de Credential Manager o Sign in with Google, amb els recursos de marca de Google. El disseny només en fixa la posició i la mida.

## 5. Activació al primer dispositiu (13e → 13h)

### Empremta (13e)
- Abans d'obrir les paraules i el QR apareix `BiometricPrompt`, amb `BIOMETRIC_STRONG` o la credencial del dispositiu.
- Títol: «Confirma que ets tu». Subtítol: «Per veure les paraules de seguretat» o «Per mostrar el codi QR».
- Si es cancel·la, l'usuari torna a la pantalla anterior.
- Es demana sempre, també al primer dispositiu just després d'iniciar sessió.

### Paraules (13f, 13f2)
- **Pantalla:** TopAppBar amb «Tancar». Té `FLAG_SECURE`.
- **Contingut:**
  - text;
  - les paraules en una sola targeta `card` (radi 20), separades per espais i amb salt de línia natural, en 19 sp, pes 600 i interlineat de 30 sp, sense números ni guionet (decisió de Bernat d'11 de novembre de 2026, no pas la graella 2×6 de `pantalles.html`);
  - botó tonal «Copia» de 40 dp;
  - nota `card` amb la icona `info` i el text sobre què passa si es perden.
- **Copiar:** el porta-retalls es marca com a sensible (`ClipDescription.EXTRA_IS_SENSITIVE`) i s'esborra al cap d'1 minut. Snackbar: «Copiades. S'esborraran del porta-retalls en 1 minut.».
- **Peu fix:**
  - casella «Les he guardat en un lloc segur»;
  - «Continua», desactivat fins que la casella està marcada;
  - «Fins que no continuïs, no es puja res.».
- **Tancar abans de continuar:** tanca la sessió i no deixa res al núvol.

### Pujada (13g) i fet (13h)
- **Pujada:**
  - Galeta de 88 dp amb la icona `cloud_upload`.
  - Barra de progrés determinada de 8 dp, segons el nombre d'entrades.
  - «Amaga» deixa la feina en segon pla amb WorkManager, amb una notificació de progrés.
- **Fet:**
  - Galeta `pri` amb la icona `check` i «Aquest dispositiu: Pixel 8 · 1 de 2 places».
  - Botó «Fet» i botó de text «Afegeix un altre dispositiu», que obre 13p.

## 6. Dispositiu nou o després d'un reset (13i → 13n)

**Ordre:** clau → places (si cal) → fusió (si hi ha notes locals) → baixada → fet.

### Clau (13i)
- Dues targetes iguals de 164 dp, una al costat de l'altra:
  - «Llegeix el QR» (`qr_code_scanner`);
  - «Escriu les paraules» (`password`).
- A sota, una fila amb el compte i «Canvia».
- Al peu, «No tinc les paraules».

### Escriure (13j, 13j2)
- **Pantalla:** té `FLAG_SECURE`. Al costat del títol, a la dreta, hi ha un botó «Enganxa» amb vora, que substitueix el text pel del porta-retalls.
- **Camp:** decisió de Bernat d'11 de novembre de 2026, que substitueix els 12 camps de `pantalles.html`: un sol camp multilínia (mínim 4 línies, radi 16) on s'escriuen o s'enganxen les paraules, separades per espais o salts de línia. Es veu el que l'usuari ha escrit; `Bip39.split` tolera números, comes i majúscules. El camp porta una vora de 2 dp en `pri` amb el focus. Té descripció per a lectors de pantalla.
- **Mentre s'escriu:**
  - sota el camp, un comptador «7 / 12»;
  - xips de suggeriment (fins a 4) per a l'última paraula, a partir de 2 lletres i només mentre s'escriu; tocar-ne un la completa i hi afegeix un espai;
  - teclat sense suggeriments ni aprenentatge (`KeyboardOptions(autoCorrect = false)`, `IME_FLAG_NO_PERSONALIZED_LEARNING`).
- **Paraula que no és a la llista:** es pinta en `err` dins el camp un cop completa (seguida d'un espai o amb el camp sense focus), i la vora del camp passa a `err`.
- **«Continua»:** s'activa amb exactament 12 paraules i totes de la llista.
- **Paraules incorrectes:** nota `err-c` a dalt amb el text «Aquestes paraules no obren les dades d'aquest compte…».

### QR (13k, 13k2)
- **Visor:** pantalla negra de càmera amb un marc de 260 dp i cantonades de 4 dp.
- **Text:** «Apunta al QR de l'altre dispositiu» i la ruta per arribar-hi.
- **Permís:** `CAMERA` es demana en tocar «Llegeix el QR».
- **QR no vàlid o caducat:** avís fosc dins del visor.
- **Alternativa:** a baix, «Escriu les paraules».

### Places plenes (13l)
- **Text:** «Aquest compte ja sincronitza en 2 de 2 dispositius…».
- **Llista:** un radio per dispositiu, amb la icona, el nom i l'última sincronització. **Cap dispositiu triat per defecte.**
- **Pro:** fila amb vora discontínua «Fins a 5 dispositius amb Pro», amb l'etiqueta PRO. Obre el paywall amb l'origen `sync`.
- **Botons:** «Treu-lo i continua» (s'activa en triar-ne un) i «Ara no».

### Fusió (13m) i baixada (13m2)
- Diàleg «Aquest dispositiu ja té notes» amb el recompte d'entrades, mai noms.
- Baixada: la mateixa pantalla de progrés que la pujada, amb `cloud_download`.

### «No tinc les paraules» (13n)
Tres targetes numerades, en aquest ordre:
1. Un altre dispositiu → botó tonal «Llegeix el QR».
2. Aquest dispositiu té les dades → botó amb vora «Comença de nou», que porta a la confirmació. **Només surt si hi ha dades locals.**
3. Cap de les dues → explicació, sense cap botó.

## 7. Amb sessió (13o, 13o2)

De dalt a baix:

1. **Compte:** targeta amb l'avatar de 48 dp, el nom i el correu.
2. **Estat:** `cloud_done` i «Al dia · fa 2 minuts». En error, el motiu.
3. **Dispositius:**
   - A la capçalera, «2 de 2».
   - Una fila per dispositiu amb la icona, el nom i l'etiqueta «Aquest dispositiu». A sota, l'última sincronització. A la dreta, `more_vert` amb «Canvia el nom» i «Treu».
   - Fila «Afegeix un dispositiu» (`qr_code_2`).
   - Fila discontínua de Pro (5 places).
4. **Seguretat:** fila «Paraules de seguretat», amb el subtítol de l'última revisió i la icona `fingerprint` a la dreta.
5. **Compte:** «Tanca la sessió» i «Esborra el compte» (en `err`).
6. **Peu:** `lock` i «Xifrat d'extrem a extrem…».

**Dispositiu tret (13o2):**
- A dalt hi ha l'avís `err-c`, que no es pot tancar.
- No surten l'estat ni «Afegeix un dispositiu».
- La llista mostra els dispositius que sí que sincronitzen.
- «Torna a entrar» porta a 13l.

## 8. Afegir un dispositiu: QR (13p)

- **Accés:** demana l'empremta abans. La pantalla té `FLAG_SECURE`.
- **QR:** de 232 dp, sobre fons blanc, amb 20 dp de marge interior i radi 28.
- **Caducitat:** el QR caduca en 5 minuts, amb un compte enrere (`timer`). Quan caduca, se'n genera un de nou en tocar-lo.
- **Avís:** nota amb la icona `visibility_off` i el text «Qui vegi aquest codi pot obrir les teves dades…».

## 9. Confirmacions (13q)

Són diàlegs de M3, amb el títol en 22 sp.

| Confirmació | Botó principal |
| --- | --- |
| Treure un dispositiu | «Treu», en `pri` |
| Tancar sessió | «Tanca la sessió», en `pri` |
| Esborrar el compte | «Esborra», en `err` |
| Començar de nou | «Comença de nou», en `err` |

Esborrar el compte necessita connexió. Sense connexió surt el snackbar «Cal connexió per esborrar el compte.».

## 10. Dispositiu tret al llistat (13c)

- És una targeta `err-c` a dalt del llistat, sota la capçalera: icona `sync_problem`, títol, text i el botó «Torna a entrar» (en `on-err-c`).
- No es pot tancar i desapareix quan el dispositiu torna a sincronitzar.
- És compatible amb l'avís de canals: si n'hi ha dos, el de canals va a sota.

## 11. Pantalles grans (13r)

- **Tauleta:**
  - L'indicador va a baix de tot del rail, amb l'etiqueta «Compte». Quan Sincronització està oberta, porta un indicador de 56 dp en `pri-c`.
  - La capçalera del llistat no porta indicador.
- **Sincronització:** una columna de 640 dp dins de l'àrea de contingut, sense el llistat al costat.
- **Diàlegs de 560 dp:** paraules, QR, clau, places, «No tinc les paraules» i confirmacions.
- **Pantalla completa:** el lector de QR.
- **Plegable obert:** com al mòbil, dins del panell del llistat.

## 12. Restriccions que cal complir

- **`FLAG_SECURE`:** 13f, 13j i 13p. Comprova que surten en negre a les recents.
- **Persones protegides:** cap pantalla del flux mostra noms ni contingut de persones; només recomptes.
- **Pro:** el paywall només s'obre des de les files de 5 places, amb l'origen `sync`.
- **Sense connexió:** l'indicador i la fila canvien d'estat, i els canvis pugen sols. No hi ha cap diàleg.

## 13. Textos

Són al tauler 13t i a la taula següent. Les claus són una proposta per a `strings.xml`.

| Clau | ca | en | es |
| --- | --- | --- | --- |
| `title` | Sincronització | Sync | Sincronización |
| `off_h` | Les teves converses, a tots els teus dispositius | Your conversations, on all your devices | Tus conversaciones, en todos tus dispositivos |
| `off_b` | És opcional i gratuït. Si no l’actives, Converses funciona igual que ara. | It’s optional and free. If you don’t turn it on, Converses works just as it does now. | Es opcional y gratuito. Si no la activas, Converses funciona igual que ahora. |
| `p1_t` | Xifrat al teu mòbil | Encrypted on your phone | Cifrado en tu móvil |
| `p1_s` | Les dades es xifren abans de sortir. Ningú més les pot llegir: ni nosaltres ni Google. | Your data is encrypted before it leaves. Nobody else can read it: not us, not Google. | Los datos se cifran antes de salir. Nadie más puede leerlos: ni nosotros ni Google. |
| `p2_t` | La clau és teva | The key is yours | La clave es tuya |
| `p2_s` | Et donarem unes paraules de seguretat. Guarda-les bé: les necessitaràs per afegir un dispositiu. | We’ll give you some security words. Keep them safe: you’ll need them to add a device. | Te daremos unas palabras de seguridad. Guárdalas bien: las necesitarás para añadir un dispositivo. |
| `p3_t` | 2 dispositius de franc | 2 devices for free | 2 dispositivos gratis |
| `p3_s` | Fins a 5 amb Converses Pro. | Up to 5 with Converses Pro. | Hasta 5 con Converses Pro. |
| `google` | Continua amb Google | Continue with Google | Continuar con Google |
| `acc_note` | El compte només serveix per saber de qui són les dades. No pot desxifrar res. | The account only tells us whose data it is. It can’t decrypt anything. | La cuenta solo sirve para saber de quién son los datos. No puede descifrar nada. |
| `bio_t` | Confirma que ets tu | Confirm it’s you | Confirma que eres tú |
| `bio_s` | Per veure les paraules de seguretat | To see your security words | Para ver las palabras de seguridad |
| `bio_qr` | Per mostrar el codi QR | To show the QR code | Para mostrar el código QR |
| `words_t` | Paraules de seguretat | Security words | Palabras de seguridad |
| `words_b` | Aquestes 12 paraules són la clau de les teves dades. Guarda-les en un gestor de contrasenyes, com 1Password, o apunta-les en paper. | These 12 words are the key to your data. Keep them in a password manager, like 1Password, or write them down on paper. | Estas 12 palabras son la clave de tus datos. Guárdalas en un gestor de contraseñas, como 1Password, o apúntalas en papel. |
| `copy` | Copia | Copy | Copiar |
| `copied` | Copiades. S’esborraran del porta-retalls en 1 minut. | Copied. They’ll be cleared from the clipboard in 1 minute. | Copiadas. Se borrarán del portapapeles en 1 minuto. |
| `lose` | Si perds les paraules i tots els dispositius alhora, les dades del núvol no es podran recuperar. Les que tinguis al mòbil es queden igualment. | If you lose the words and all your devices at once, the data in the cloud can’t be recovered. Whatever is on your phone stays there anyway. | Si pierdes las palabras y todos los dispositivos a la vez, los datos de la nube no se podrán recuperar. Los que tengas en el móvil se quedan igualmente. |
| `saved_cb` | Les he guardat en un lloc segur | I’ve stored them somewhere safe | Las he guardado en un lugar seguro |
| `continue` | Continua | Continue | Continuar |
| `nothing_up` | Fins que no continuïs, no es puja res. | Nothing is uploaded until you continue. | Hasta que no continúes, no se sube nada. |
| `up_t` | Pujant les teves dades | Uploading your data | Subiendo tus datos |
| `down_t` | Baixant les teves dades | Downloading your data | Descargando tus datos |
| `prog` | %1$d de %2$d entrades · xifrades al mòbil | %1$d of %2$d entries · encrypted on your phone | %1$d de %2$d entradas · cifradas en el móvil |
| `prog_bg` | Pots continuar fent servir l’app. Acabarà sol. | You can keep using the app. It’ll finish on its own. | Puedes seguir usando la app. Terminará sola. |
| `hide` | Amaga | Hide | Ocultar |
| `done_t` | Sincronització activada | Sync is on | Sincronización activada |
| `done_s` | Aquest dispositiu: %1$s · %2$d de %3$d places | This device: %1$s · %2$d of %3$d slots | Este dispositivo: %1$s · %2$d de %3$d plazas |
| `done` | Fet | Done | Hecho |
| `add_other` | Afegeix un altre dispositiu | Add another device | Añadir otro dispositivo |
| `key_t` | Aquest compte ja té dades | This account already has data | Esta cuenta ya tiene datos |
| `key_b` | Per obrir-les en aquest dispositiu cal la clau. Tria com la vols donar: | To open it on this device you need the key. Choose how to provide it: | Para abrirlos en este dispositivo hace falta la clave. Elige cómo darla: |
| `qr_opt` | Llegeix el QR | Scan the QR code | Escanear el QR |
| `qr_opt_s` | D’un dispositiu que ja sincronitza | From a device that already syncs | De un dispositivo que ya sincroniza |
| `w_opt` | Escriu les paraules | Enter the words | Escribir las palabras |
| `w_opt_s` | Les 12 paraules de seguretat | The 12 security words | Las 12 palabras de seguridad |
| `no_words` | No tinc les paraules | I don’t have the words | No tengo las palabras |
| `paste12` | Enganxa | Paste | Pegar |
| `w_err` | Aquestes paraules no obren les dades d’aquest compte. Revisa l’ordre i com estan escrites. | These words don’t open this account’s data. Check the order and the spelling. | Estas palabras no abren los datos de esta cuenta. Revisa el orden y cómo están escritas. |
| `qr_aim` | Apunta al QR de l’altre dispositiu | Point at the QR code on the other device | Apunta al QR del otro dispositivo |
| `qr_where` | A l’altre dispositiu: Converses › Sincronització › Afegeix un dispositiu | On the other device: Converses › Sync › Add a device | En el otro dispositivo: Converses › Sincronización › Añadir un dispositivo |
| `qr_err` | Aquest QR no és de Converses o ha caducat. Genera’n un de nou a l’altre dispositiu. | This QR code isn’t from Converses or has expired. Generate a new one on the other device. | Este QR no es de Converses o ha caducado. Genera uno nuevo en el otro dispositivo. |
| `full_t` | No queden places | No slots left | No quedan plazas |
| `full_b` | Aquest compte ja sincronitza en %1$d de %2$d dispositius. Per entrar amb aquest, treu-ne un. El que treguis conserva les seves dades. | This account already syncs on %1$d of %2$d devices. To add this one, remove one. The one you remove keeps its data. | Esta cuenta ya sincroniza en %1$d de %2$d dispositivos. Para entrar con este, quita uno. El que quites conserva sus datos. |
| `pro5` | Fins a 5 dispositius amb Pro | Up to 5 devices with Pro | Hasta 5 dispositivos con Pro |
| `remove_go` | Treu-lo i continua | Remove it and continue | Quitarlo y continuar |
| `notnow` | Ara no | Not now | Ahora no |
| `merge_t` | Aquest dispositiu ja té notes | This device already has notes | Este dispositivo ya tiene notas |
| `merge_b` | Les %d entrades que tens aquí s’afegiran a les del núvol. No se’n perd cap. | The %d entries you have here will be added to the ones in the cloud. None will be lost. | Las %d entradas que tienes aquí se añadirán a las de la nube. No se pierde ninguna. |
| `cancel` | Cancel·la | Cancel | Cancelar |
| `nw_1t` | Tens un altre dispositiu que sincronitza? | Do you have another device that syncs? | ¿Tienes otro dispositivo que sincroniza? |
| `nw_1s` | Obre-hi Converses › Sincronització. Pots llegir-ne el QR o veure-hi les paraules. | Open Converses › Sync on it. You can scan its QR code or see the words there. | Abre allí Converses › Sincronización. Puedes escanear su QR o ver allí las palabras. |
| `nw_2t` | Aquest dispositiu té les teves dades? | Does this device have your data? | ¿Este dispositivo tiene tus datos? |
| `nw_2s` | Pots començar de nou amb paraules noves. El que hi ha al núvol se substituirà pel que hi ha en aquest dispositiu. | You can start over with new words. What’s in the cloud will be replaced with what’s on this device. | Puedes empezar de nuevo con palabras nuevas. Lo que hay en la nube se sustituirá por lo que hay en este dispositivo. |
| `restart` | Comença de nou | Start over | Empezar de nuevo |
| `nw_3t` | Si no tens cap de les dues coses | If you have neither | Si no tienes ninguna de las dos cosas |
| `nw_3s` | Les dades del núvol no es poden recuperar. Estan xifrades amb les teves paraules i ningú més les pot obrir, tampoc nosaltres. | The data in the cloud can’t be recovered. It’s encrypted with your words and nobody else can open it, including us. | Los datos de la nube no se pueden recuperar. Están cifrados con tus palabras y nadie más puede abrirlos, tampoco nosotros. |
| `ok_now` | Al dia · fa 2 minuts | Up to date · 2 minutes ago | Al día · hace 2 minutos |
| `devices` | Dispositius | Devices | Dispositivos |
| `slots` | %1$d de %2$d | %1$d of %2$d | %1$d de %2$d |
| `this_dev` | Aquest dispositiu | This device | Este dispositivo |
| `add_dev` | Afegeix un dispositiu | Add a device | Añadir un dispositivo |
| `add_dev_s` | Mostra un codi QR per llegir-lo des del nou | Shows a QR code to scan from the new one | Muestra un código QR para leerlo desde el nuevo |
| `rename` | Canvia el nom | Rename | Cambiar el nombre |
| `remove` | Treu | Remove | Quitar |
| `security` | Seguretat | Security | Seguridad |
| `see_words` | Paraules de seguretat | Security words | Palabras de seguridad |
| `see_words_s` | Les vas revisar fa 4 mesos | You last checked them 4 months ago | Las revisaste hace 4 meses |
| `account` | Compte | Account | Cuenta |
| `signout` | Tanca la sessió | Sign out | Cerrar sesión |
| `signout_s` | Les dades es queden en aquest dispositiu | Your data stays on this device | Los datos se quedan en este dispositivo |
| `delete` | Esborra el compte | Delete account | Borrar la cuenta |
| `delete_s` | I les dades del núvol. Les d’aquí es queden. | And the data in the cloud. The data here stays. | Y los datos de la nube. Los de aquí se quedan. |
| `e2e` | Xifrat d’extrem a extrem. Ni nosaltres ni Google podem llegir les teves dades. | End-to-end encrypted. Neither we nor Google can read your data. | Cifrado de extremo a extremo. Ni nosotros ni Google podemos leer tus datos. |
| `addqr_t` | Afegeix un dispositiu | Add a device | Añadir un dispositivo |
| `addqr_b` | Al dispositiu nou, inicia sessió amb el mateix compte i tria «Llegeix el QR». | On the new device, sign in with the same account and choose “Scan the QR code”. | En el dispositivo nuevo, inicia sesión con la misma cuenta y elige «Escanear el QR». |
| `expires` | El codi caduca en %s | The code expires in %s | El código caduca en %s |
| `qr_warn` | Qui vegi aquest codi pot obrir les teves dades. No el comparteixis ni en facis captures. | Anyone who sees this code can open your data. Don’t share it or take screenshots. | Quien vea este código puede abrir tus datos. No lo compartas ni hagas capturas. |
| `out_t` | Aquest dispositiu ja no sincronitza | This device no longer syncs | Este dispositivo ya no sincroniza |
| `out_b` | L’han tret des d’un altre dispositiu. Les dades d’aquí es queden. | It was removed from another device. The data here stays. | Lo han quitado desde otro dispositivo. Los datos de aquí se quedan. |
| `rejoin` | Torna a entrar | Rejoin | Volver a entrar |
| `c1_t` | Treure el %s? | Remove %s? | ¿Quitar el %s? |
| `c1_b` | Deixarà de sincronitzar, però conservarà les seves dades. Hi podràs tornar a entrar més endavant. | It will stop syncing but keep its data. You can add it again later. | Dejará de sincronizar, pero conservará sus datos. Podrás volver a entrar más adelante. |
| `c2_t` | Tancar la sessió? | Sign out? | ¿Cerrar sesión? |
| `c2_b` | Les dades es queden en aquest dispositiu. Només s’atura la sincronització i s’allibera la plaça. | Your data stays on this device. Sync just stops and the slot is freed. | Los datos se quedan en este dispositivo. Solo se detiene la sincronización y se libera la plaza. |
| `c3_t` | Esborrar el compte? | Delete account? | ¿Borrar la cuenta? |
| `c3_b` | S’esborraran les dades del núvol i el compte, i els altres dispositius deixaran de sincronitzar. Les dades d’aquest dispositiu es queden. No es pot desfer. | The cloud data and the account will be deleted, and your other devices will stop syncing. The data on this device stays. This can’t be undone. | Se borrarán los datos de la nube y la cuenta, y los demás dispositivos dejarán de sincronizar. Los datos de este dispositivo se quedan. No se puede deshacer. |
| `c3_go` | Esborra | Delete | Borrar |
| `c4_t` | Començar de nou? | Start over? | ¿Empezar de nuevo? |
| `c4_b` | Es generaran paraules noves i el núvol es substituirà pel que hi ha en aquest dispositiu. Els altres dispositius hauran d’entrar amb les paraules noves. No es pot desfer. | New words will be generated and the cloud will be replaced with what’s on this device. Your other devices will need the new words. This can’t be undone. | Se generarán palabras nuevas y la nube se sustituirá por lo que hay en este dispositivo. Los demás dispositivos tendrán que entrar con las palabras nuevas. No se puede deshacer. |
| `st_off` | Desactivada · opcional i xifrada | Off · optional and encrypted | Desactivada · opcional y cifrada |
| `st_ok` | Al dia · fa 2 minuts | Up to date · 2 minutes ago | Al día · hace 2 minutos |
| `st_sync` | Sincronitzant… | Syncing… | Sincronizando… |
| `st_pend` | Sense connexió · %d canvis pendents | Offline · %d changes pending | Sin conexión · %d cambios pendientes |
| `st_key` | Falten les paraules de seguretat | Security words needed | Faltan las palabras de seguridad |
| `st_out` | Aquest dispositiu ja no sincronitza | This device no longer syncs | Este dispositivo ya no sincroniza |
| `ind_off` | Sincronització: desactivada | Sync: off | Sincronización: desactivada |
| `ind_ok` | Compte: al dia | Account: up to date | Cuenta: al día |
| `ob_link` | Ja tinc dades al núvol | I already have data in the cloud | Ya tengo datos en la nube |
| `ob_body` | Les notes es queden al mòbil i, si les sincronitzes, viatgen xifrades. Si vols, protegeix una persona amb l’empremta i ningú més la podrà obrir. | Your notes stay on your phone and, if you sync them, they travel encrypted. If you want, protect a person with your fingerprint and nobody else can open them. | Las notas se quedan en el móvil y, si las sincronizas, viajan cifradas. Si quieres, protege a una persona con la huella y nadie más podrá abrirla. |
| `ob_c1` | Xifrat | Encrypted | Cifrado |
| `ob_c2` | Compte opcional | Account optional | Cuenta opcional |
| `del_offline` | Cal connexió per esborrar el compte. | You need a connection to delete your account. | Hace falta conexión para borrar la cuenta. |
| `rail_acc` | Compte | Account | Cuenta |
