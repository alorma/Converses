# Converses — Versió gratuïta (v1) i Pro

Què queda fora de la versió gratuïta, perquè passarà a ser Pro, i què es queda gratis per sempre. **La v1 (publicada) no té Pro:** les funcions Pro existeixen al codi però són darrere de feature flags apagats. Document de treball per al Pro.

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
| **Avatar personalitzat** (color i forma) | Forma fixa **Galeta** (`Cookie9Sided`) i color automàtic a partir del nom; no es poden canviar | Selector de color i forma a Editar persona / Nova persona |
| **Sub-canals** (backlog 8) | No existeix: només els 5 grups | Crear sub-canals dins de qualsevol grup, amb nom lliure (únic per persona) i valor opcional |
| **Follow-ups / pendents** (backlog 6) | No existeix | Marcar entrades com a pendents, secció «Pendents» al detall, comptador al llistat i recordatoris |
| **Color de la persona** (tema del detall, `PersonColorFeature`) | No hi és: les pantalles de la persona usen el tema de l'app i l'avatar només té el color automàtic pel nom | Activar el tema que pren el color de l'avatar (i l'ajust «Color de la persona» a Ajustos) |
| ~~**Contact integration**~~ | Retirada el novembre de 2026 (canvien els permisos de Contactes): s'ha tret tot el codi (vincle, compte local, sync adapter, files a Contactes) i les columnes de Room (v4) | — |

## Fet a la v1 gratuïta

- **Feature flags** (`data/flags/`): `CustomAvatarFeature` i `PersonColorFeature`, tots dos apagats. Els flags de sub-canals i follow-ups s'afegiran quan existeixin aquestes funcions. Cada flag Pro és `Hidden`, `Locked` o `Unlocked` segons el flag i el `ProRepository` (`docs/pro/fases.md`, «Model»); al menú de debug, l'apartat Pro ensenya l'estat de RevenueCat (Test Store) i permet fer override, i «Turn on every Pro flag» obre tots els flags. A release, Pro surt de RevenueCat (fase 2).
- **Avatar estàndard:** Galeta i color automàtic pel nom (`AvatarColor.forName`); `avatarShape` i `avatarColor` nul·lables (`null` = estàndard).
- **Editor:** sense targeta d'avatar; l'avatar és a la fila del nom i, amb `CustomAvatarFeature`, obre el full «Avatar».

## Pendent per al Pro

- Infraestructura (`ProRepository` i runner de flags Pro), RevenueCat, paywall i publicació: `docs/pro/fases.md`.
- Sub-canals (backlog 8) i follow-ups (backlog 6), cadascun darrere del seu flag. Tots dos toquen la base de dades: **cal migració i test** (la v1 ja és publicada).

## Decidit

- **Baixada de Pro** (reemborsament, revocació, o flag obert sense Pro): el que es va fer amb Pro es conserva i es continua veient (per exemple, la forma i el color de l'avatar). Només no es pot canviar ni fer-ne de nou. Mai s'esborren dades.

- **Canal «Empresa»** (backlog 7): ja no és un canal del sistema; és un sub-canal d'«Altres» (Pro).
- Els canals de les entrades de la beta (WhatsApp, Telegram…) passen al seu grup i les persones deixen de tenir canals propis; no es mantenen gratis.

## Proves

- [ ] Amb totes les funcions apagades: no hi ha selector d'avatar, tots els avatars són Galeta, i el mateix nom dona sempre el mateix color.
- [x] Els 7 colors surten repartits amb noms diferents (`AvatarColorTest`, 20 noms).
- [x] Amb `CustomAvatarFeature` activada (debug): el selector torna a sortir i desa els valors.

## Disseny

Lliurament de Claude Design del 26-10-2026 (pantalla de Pro i punts d'entrada). Resum:

- **Pantalla «Converses Pro» (8a):** pantalla completa amb tancar; capçalera (galeta de 64 dp en `primary` amb `workspace_premium`, «Converses Pro», «Un sol pagament. Per sempre.»), tres beneficis en llista agrupada (`palette` Persones a la teva manera, `forum` Canals propis, `flag` Pendents; el quart, `contacts` Vincle amb Contactes, es va treure amb la funció), nota amb `lock` i peu fix amb «Aconsegueix Pro · {preu}», «Restaura la compra» i «Termes · Privacitat».
- **Des d'una funció (8b):** paràmetre d'origen (`ajustos`, `canals`, `pendents`, `avatar`); el benefici d'origen va primer amb una vora de 2 dp.
- **Ja tens Pro (8c):** `check`, «Ja tens Pro», beneficis amb `check_circle` i «Som-hi», que torna on era l'usuari.
- **Punts d'entrada:** mai s'obre sola. Targeta a dalt d'Ajustos, Ajustos › Canals, xip «Canal propi» al camp «On», fila «Pendent» de la nota ràpida i, quan es llanci, l'editor d'avatar.
- **Etiqueta PRO:** 20 dp d'alçada, radi 7, fons `primaryContainer`, text de 10,5 sp en pes 800.
- **Estats de la compra:** pendent, error, cancel·lació (sense missatge), restaurar amb i sense compra, sense connexió (botó desactivat), reemborsament (es perd Pro, no s'esborren dades). Textos ca, en i es al tauler 8d.
- **Sincronització:** per definir; no surt a la pantalla de Pro.
