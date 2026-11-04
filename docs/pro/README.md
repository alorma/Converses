# Converses — Versió gratuïta (v1) i Pro

Què queda fora de la versió gratuïta, perquè passarà a ser Pro, i què es queda gratis per sempre. **La v1 (publicada) no té Pro:** les funcions Pro existeixen al codi, darrere de feature flags Pro (encesos per defecte: sense Pro es veuen amb l'etiqueta PRO). Document de treball per al Pro.

Model previst per al Pro: **pagament únic**, sense subscripció i sense anuncis. La compra es farà amb **RevenueCat**: un producte no consumible (`lifetime`) i un entitlement `converses_pro`; el codi consulta l'entitlement, mai el producte. Sense compte: RevenueCat fa servir el seu identificador anònim i no rep mai notes ni persones. Fases a `docs/pro/fases.md`.

## Regla

Mai es cobra per **accedir, protegir o treure** les dades pròpies. Al Pro hi va el que fa l'app més còmoda o més potent.

## Principis d'implementació

- **No s'esborra codi.** El que passa a Pro s'amaga darrere d'un sistema de feature flag (els feature flags), amb un interruptor de debug per provar els dos modes.
- **A la primera release no es mostra res de Pro:** ni selectors, ni reclams, ni pantalles de compra.

## Gratis per sempre

- Persones, entrades, cronologia, «Qui» i «Quan».
- Persones protegides i bloqueig de l'app.
- Exportar notes.
- Importar text a la nota ràpida: compartir, seleccionar text i enganxar.
- Els 5 grups de canals: En persona, Telèfon, Xat, Social i Altres.

## Pro (fora de la v1 gratuïta)

| Funció | A la v1 gratuïta | Amb Pro |
| --- | --- | --- |
| **Avatar personalitzat** (color i forma) | Galeta (`Cookie9Sided`) i color automàtic a partir del nom. La píndola PRO a l'avatar de l'editor obre el full «Avatar», on les opcions Pro porten cadenat i es poden **provar** abans de comprar (11a–11c) | Selector de color i forma a Editar persona / Nova persona |
| **Canals propis** (backlog 8) | Només els 5 grups; el camp «On» mostra «Canal propi» amb PRO | Canals globals dins de qualsevol grup, amb nom (únic dins del grup) i sense valor; es trien a l'entrada i es gestionen a Ajustos › Canals propis |
| **Pendents** (backlog 6) | «Nova tasca» al menú del detall mostra PRO | Tasques per persona (formulari propi), secció «Pendents» al detall, marcar com a fet i comptador al llistat |
| **Color de la persona** (tema del detall) | Les pantalles de la persona usen el tema de l'app. A Ajustos, la fila «Color de la persona» porta PRO i un `chevron_right` en lloc de l'interruptor, i obre el paywall (11e) | L'interruptor «Color de la persona» a Ajustos (apagat per defecte) activa el tema que pren el color de l'avatar |
| ~~**Contact integration**~~ | Retirada el novembre de 2026 (canvien els permisos de Contactes): s'ha tret tot el codi (vincle, compte local, sync adapter, files a Contactes) i les columnes de Room (v4) | — |

## Fet a la v1 gratuïta

- **Les funcions Pro només depenen del Pro:** la UI llegeix `ProRepository.isPro` (`rememberIsPro()`); sense Pro surten amb PRO i porten al paywall, amb Pro funcionen. Els quatre flags Pro (`custom_avatar`, `person_color`, `custom_channels`, `follow_ups`) es van treure el 4 de novembre de 2026, perquè eren funcions acabades i encesos per defecte.
- **Feature flags** (`data/flags/`): la infraestructura es manté, buida, per a funcions noves o a mitges (neixen amb el flag apagat i s'encenen des del menú de debug). Un flag Pro és `Hidden`, `Locked` o `Unlocked` segons el flag i el `ProRepository` (`docs/pro/fases.md`, «Model»). Al menú de debug, l'apartat Pro ensenya l'estat de RevenueCat (Test Store) i permet fer override. A release, Pro surt de RevenueCat (fase 2).
- **Avatar estàndard:** Galeta i color automàtic pel nom (`AvatarColor.forName`); `avatarShape` i `avatarColor` nul·lables (`null` = estàndard).
- **Editor:** sense targeta d'avatar; l'avatar és a la fila del nom i obre el full «Avatar» (amb PRO si no és Pro).
- **Personalització sense Pro (fase 8):** píndola PRO a l'editor, full «Avatar» amb prova i baixada de Pro, i fila «Color de la persona» amb PRO a Ajustos. Resum a «Disseny · Personalització sense Pro».
- **Canals propis (fase 6):** targeta sota «On» (7a, 7c) amb el full «Nou canal» / «Edita el canal» (7b), nom del canal a la cronologia, al full d'accions i a l'exportació, i Ajustos › Canals propis (7d). La nota ràpida i la nova entrada prenen per defecte el canal de l'última entrada (només amb Pro).

## Pendent per al Pro

- Infraestructura (`ProRepository` i runner de flags Pro), RevenueCat, paywall i publicació: `docs/pro/fases.md`.
- Canals propis (backlog 8) i pendents (backlog 6), cadascun darrere del seu flag Pro: fases 5–7 de `docs/pro/fases.md`. Tots dos toquen la base de dades: **migració i test** a Room v5 (fase 5).

## Decidit

- **Baixada de Pro** (reemborsament, revocació, o flag obert sense Pro): el que es va fer amb Pro es conserva i es continua veient (per exemple, la forma i el color de l'avatar). Només no es pot canviar ni fer-ne de nou. Mai s'esborren dades.

- **Canal «Empresa»** (backlog 7): ja no és un canal del sistema; és un canal propi d'«Altres» (Pro).
- Els canals de les entrades de la beta (WhatsApp, Telegram…) passen al seu grup i les persones deixen de tenir canals propis; no es mantenen gratis. **No es poden recuperar com a canals propis:** la migració 1→2 els va convertir al grup i va esborrar la taula `channel`.

## Proves

- [ ] Sense Pro: els avatars nous són Galeta amb el color automàtic, i el mateix nom dona sempre el mateix color.
- [x] Els 7 colors surten repartits amb noms diferents (`AvatarColorTest`, 20 noms).
- [x] Amb Pro: el selector surt i desa els valors.

## Disseny

Lliurament de Claude Design del 26-10-2026 (pantalla de Pro i punts d'entrada). Resum:

- **Pantalla «Converses Pro» (8a):** pantalla completa amb tancar; capçalera (galeta de 64 dp en `primary` amb `workspace_premium`, «Converses Pro», «Un sol pagament. Per sempre.»), tres beneficis en llista agrupada (`palette` Persones a la teva manera, `forum` Canals propis, `flag` Pendents; el quart, `contacts` Vincle amb Contactes, es va treure amb la funció), nota amb `lock` i peu fix amb «Aconsegueix Pro · {preu}», «Restaura la compra» i «Termes · Privacitat».
- **Des d'una funció (8b):** paràmetre d'origen (`ajustos`, `canals`, `pendents`, `avatar`); el benefici d'origen va primer amb una vora de 2 dp.
- **Ja tens Pro (8c):** `check`, «Ja tens Pro», beneficis amb `check_circle` i «Som-hi», que torna on era l'usuari.
- **Punts d'entrada:** mai s'obre sola. Targeta a dalt d'Ajustos, Ajustos › Canals propis, Ajustos › Color de la persona, xip «Canal propi» al camp «On», «Nova tasca» al detall i «Desbloqueja amb Pro» del full «Avatar».
- **Etiqueta PRO:** 20 dp d'alçada, radi 7, fons `primaryContainer`, text de 10,5 sp en pes 800.
- **Estats de la compra:** pendent, error, cancel·lació (sense missatge), restaurar amb i sense compra, sense connexió (botó desactivat), reemborsament (es perd Pro, no s'esborren dades). Textos ca, en i es al tauler 8d.
- **Sincronització:** per definir; no surt a la pantalla de Pro.

## Disseny · Canals propis i Pendents

Lliurament de Claude Design del 02-11-2026 (`converses-canals-pendents.zip`: `pantalles.html` i `INSTRUCCIONS.md`). Totes dues funcions són Pro. Resum:

### Canals propis

- **Model:** globals i sense valor (ni @usuari ni número); només nom i grup. Nom únic dins del grup. A l'entrada són opcionals: pot quedar-se només amb el grup.
- **Camp «On» (7a):** fila dels 5 grups (xips de 36 dp, radi 12, scroll horitzontal). En triar un grup surt una targeta (`surface-card`, radi 20) amb el títol overline del grup («QUIN XAT?», «QUIN TELÈFON?», «QUINA XARXA?», «ON EN PERSONA?», «QUIN CANAL?»), els seus canals (el triat amb `check` i `primary-container`; tocar-lo el desmarca) i el xip discontinu «+ Nou». Canviar de grup desmarca el canal.
- **Full «Nou canal» (7b):** «El podràs triar a qualsevol entrada, dins del seu grup.», camp «Nom», selector «GRUP» (preseleccionat), «Crea» i «Cancel·la». En crear-lo queda triat.
- **Gratuït (7c):** targeta «CANALS PROPIS» amb un xip discontinu «Canal propi» i PRO → paywall `canals`.
- **On es veu:** a la cronologia, el nom del canal en lloc del grup («JO · 19:20 · WHATSAPP»); filtre del llistat i pastilles del detall continuen per grups; exportar inclou el nom.
- **Ajustos › Canals (7d):** secció «Canals» amb PRO entre Seguretat i Quant a, fila «Canals propis» (`forum`, «N canals dins dels grups»); gratuït → paywall. Pantalla amb TopAppBar («Canals propis» + PRO), text d'explicació, llista agrupada per grup (capçalera amb la icona del grup; fila amb nom, «N entrades» i `more_vert`: Reanomena, Canvia de grup, Esborra amb confirmació). Grups buits no surten. FAB estès «Nou canal» i nota al peu.
- **Baixada de Pro:** les entrades conserven i mostren el canal; no se'n creen ni se'n trien de nous.

### Pendents (tasques, redisseny del 03-11-2026)

- **Model:** taula pròpia `task` (`personId`, `text`, `dueDate?`, `remind`, `createdAt`, `doneAt?`), independent de les entrades; no surten a la cronologia.
- **Crear (9a, 9b, 9c):** «Nova tasca» és la primera opció del menú «Nota ▾» del detall (gratuït: PRO → paywall `pendents`) i el botó «+ Nova tasca» de la secció. Formulari a pantalla completa: «Amb qui» (ve triada), «Què has de fer?» amb enganxar, «Quan» cap al futur (Sense data, Avui, Demà, el proper dia rellevant, La setmana que ve, Tria dia…) i el botó fix «Guarda a pendents». «Avisa'm» queda per més endavant.
- **Detall (9d, 9h):** secció «Pendents · N» entre «Dades» i «Cronologia»; amb Pro surt sempre (targeta buida si no n'hi ha). Fila: cercle «Marca com a fet» de 24 dp en 48 dp, etiqueta de data («DEMÀ», «DIVENDRES», «SENSE DATA», «FA 3 DIES», sense vermell) i text. Tocar la fila obre l'editor, amb l'opció d'esborrar.
- **Marcar com a fet (9e, 9f):** snackbar «Marcat com a fet» amb «Desfés» i haptic `Confirm`; passa a «Fets · N», plegable, amb text ratllat i «FET AHIR». El check d'una feta la torna a pendents.
- **Llistat (9g):** pastilla `primary-container` amb `flag` i el nombre, al costat dels canals. Mai a les persones protegides.
- **Baixada de Pro:** les tasques es veuen i es marquen com a fetes; no se'n creen de noves (sense botó ni targeta buida).

Textos ca, en i es al tauler 7g, més els del menú i el diàleg d'esborrar a `INSTRUCCIONS.md`.

## Disseny · Personalització sense Pro

Lliurament de Claude Design del 03-11-2026 (`converses-analitica-avatar.zip`, `INSTRUCCIONS.md` §2, taulers 11a–11f). Decisions completes a la fase 8 de `docs/pro/fases.md`. Resum:

- **Editor (11a):** sense Pro, la píndola PRO (`ProPill`, al designsystem) substitueix el llapis de l'avatar i obre el full. Val per a persona nova i existent.
- **Full «Avatar» (11b):** PRO al títol, targeta «Formes i colors amb Pro»; les formes Pro porten `lock` de 16 dp i els colors Pro un `lock` en un cercle de 18 dp. Galeta i Automàtic són lliures (`AvatarSheetLock`).
- **Prova (11c):** tocar una opció Pro la previsualitza al full, sense obrir el paywall. «Fet» passa a «Desbloqueja amb Pro» (paywall `ProBenefit.People`) i «Descarta la prova». Tancar el full descarta; si compra, torna al full amb la prova aplicada i la pot desar. L'avatar de l'editor no canvia mentre es prova.
- **Baixada de Pro (11d):** la tria desada surt triada i sense cadenat, amb la targeta «Triat amb Pro». Triar Galeta o Automàtic i desar-ho la fa perdre; no s'esborra res més.
- **Color de la persona (11e):** sense Pro, PRO al títol i `chevron_right`; tota la fila obre el paywall. A la baixada, el tema de la persona s'apaga però la preferència queda desada.
