# Converses — Versió gratuïta (v1) i Pro

Què queda fora de la versió gratuïta, perquè passarà a ser Pro, i què es queda gratis per sempre. **La v1 (publicada) no té Pro:** les funcions Pro existeixen al codi però són darrere de feature flags apagats. Document de treball per al Pro.

Model previst per al Pro: **pagament únic**, sense subscripció i sense anuncis. La compra es farà amb **RevenueCat**: un producte no consumible i un entitlement `pro`; el codi consulta l'entitlement, mai el producte. Sense compte: RevenueCat fa servir el seu identificador anònim i no rep mai notes ni persones. Fases a `docs/pro/fases.md`.

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
| **Avatar personalitzat** (color i forma) | Forma fixa **Galeta** (`Cookie9Sided`) i color automàtic a partir del nom; no es poden canviar | Selector de color i forma a Editar persona / Nova persona |
| **Sub-canals** (backlog 8) | No existeix: només els 5 grups | Crear sub-canals dins de qualsevol grup, amb nom lliure (únic per persona) i valor opcional |
| **Follow-ups / pendents** (backlog 6) | No existeix | Marcar entrades com a pendents, secció «Pendents» al detall, comptador al llistat i recordatoris |
| **Color de la persona** (tema del detall, `PersonColorFeature`) | No hi és: les pantalles de la persona usen el tema de l'app i l'avatar només té el color automàtic pel nom | Activar el tema que pren el color de l'avatar (i l'ajust «Color de la persona» a Ajustos) |
| **Contact integration** (`ContactLinkFeature`) | No es veu: el flag és apagat i la release no porta els permisos de Contactes (C12) | Vincular una persona a un contacte des de l'editor, «Vinculada a…» i «Obre a Contactes» al detall, «Veure notes» i «Crear nota» a «Connected apps» de Contactes (mai per a protegides) i Ajustos › Contactes. Fet (fases 1–6 de `docs/contacts/`); més endavant (fase 7): avís de trucada entrant (`CallScreeningService`) i importació de diversos contactes de cop |

## Fet a la v1 gratuïta

- **Feature flags** (`data/flags/`): `CustomAvatarFeature` i `PersonColorFeature`, tots dos apagats. `ContactLinkFeature` (contactes) també hi és, apagat. Els flags de sub-canals i follow-ups s'afegiran quan existeixin aquestes funcions. Cada flag Pro és `Hidden`, `Locked` o `Unlocked` segons el flag i el `ProRepository` (`docs/pro/fases.md`, «Model»); al menú de debug, «Pro user» fa de compra i «Turn on every Pro flag» obre tots els flags. A release encara ningú no és Pro.
- **Avatar estàndard:** Galeta i color automàtic pel nom (`AvatarColor.forName`); `avatarShape` i `avatarColor` nul·lables (`null` = estàndard).
- **Editor:** sense targeta d'avatar; l'avatar és a la fila del nom i, amb `CustomAvatarFeature`, obre el full «Avatar».

## Pendent per al Pro

- Infraestructura (`ProRepository` i runner de flags Pro), RevenueCat, paywall i publicació: `docs/pro/fases.md`.
- Sub-canals (backlog 8) i follow-ups (backlog 6), cadascun darrere del seu flag. Tots dos toquen la base de dades: **cal migració i test** (la v1 ja és publicada).
- Contactes, per obrir-ho al Pro: passar `READ_CONTACTS` i `WRITE_CONTACTS` al manifest principal (C12), enviar la declaració de `READ_CONTACTS` a Play Console (esborrany a `docs/contacts/README.md`, «Política») i regenerar la Baseline Profile. Fitxa de Play (`app/src/main/play/listings`, en/ca/es): afegir la funció a la descripció (vincular un contacte, «Veure notes» i «Crear nota» des de Contactes) i repassar «ni comptes» de «Privada per disseny», perquè amb la funció apareix la compte local «Converses» a Ajustos › Comptes (per exemple «ni registre», o explicar que és local i només serveix perquè Contactes mostri les files); captures noves si es vol ensenyar. Abans d'obrir-ho, provar amb TalkBack la fila «Vinculada a…» de l'editor, la fila del detall i el full 5b (codi a `c71f6de`).

## Decidit

- **Baixada de Pro** (reemborsament, revocació, o flag obert sense Pro): el que es va fer amb Pro es conserva i es continua veient (per exemple, la forma i el color de l'avatar). Només no es pot canviar ni fer-ne de nou. Mai s'esborren dades.

- **Canal «Empresa»** (backlog 7): ja no és un canal del sistema; és un sub-canal d'«Altres» (Pro).
- Els canals de les entrades de la beta (WhatsApp, Telegram…) passen al seu grup i les persones deixen de tenir canals propis; no es mantenen gratis.

## Proves

- [ ] Amb totes les funcions apagades: no hi ha selector d'avatar, tots els avatars són Galeta, i el mateix nom dona sempre el mateix color.
- [x] Els 7 colors surten repartits amb noms diferents (`AvatarColorTest`, 20 noms).
- [x] Amb `CustomAvatarFeature` activada (debug): el selector torna a sortir i desa els valors.
- [x] Amb `ContactLinkFeature` apagat no surt res de Contactes (editor, detall, Ajustos) i no queda ni la compte ni les files; el vincle es conserva (emulador, fases 2 i 4).
- [x] Amb `ContactLinkFeature` activada (debug): vincular, obrir a Contactes, files a «Connected apps», protegides sense files, contacte esborrat i fusionat (emulador, fases 2–5).
- [x] La release no porta els permisos de Contactes (APK de release, fase 3).

## Disseny

Lliurament de Claude Design del 26-10-2026 (pantalla de Pro i punts d'entrada). Resum:

- **Pantalla «Converses Pro» (8a):** pantalla completa amb tancar; capçalera (galeta de 64 dp en `primary` amb `workspace_premium`, «Converses Pro», «Un sol pagament. Per sempre.»), quatre beneficis en llista agrupada (`palette` Persones a la teva manera, `forum` Canals propis, `flag` Pendents, `contacts` Vincle amb Contactes), nota amb `lock` i peu fix amb «Aconsegueix Pro · {preu}», «Restaura la compra» i «Termes · Privacitat».
- **Des d'una funció (8b):** paràmetre d'origen (`ajustos`, `contactes`, `canals`, `pendents`, `avatar`); el benefici d'origen va primer amb una vora de 2 dp.
- **Ja tens Pro (8c):** `check`, «Ja tens Pro», beneficis amb `check_circle` i «Som-hi», que torna on era l'usuari.
- **Punts d'entrada:** mai s'obre sola. Targeta a dalt d'Ajustos, Ajustos › Canals, Ajustos › Contactes, xip «Canal propi» al camp «On», fila «Pendent» de la nota ràpida i, quan es llanci, l'editor d'avatar.
- **Etiqueta PRO:** 20 dp d'alçada, radi 7, fons `primaryContainer`, text de 10,5 sp en pes 800.
- **Estats de la compra:** pendent, error, cancel·lació (sense missatge), restaurar amb i sense compra, sense connexió (botó desactivat), reemborsament (es perd Pro, no s'esborren dades). Textos ca, en i es al tauler 8d.
- **Sincronització:** per definir; no surt a la pantalla de Pro.
