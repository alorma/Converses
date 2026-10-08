# Converses — Versió gratuïta (v1) i Pro

Què queda fora de la versió gratuïta, perquè passarà a ser Pro, i què es queda gratis per sempre. **La v1 (publicada) no té Pro:** les funcions Pro existeixen al codi però són darrere de feature flags apagats. Document de treball per al Pro.

Model previst per al Pro: **pagament únic**, sense subscripció i sense anuncis. La compra (Play Billing) encara no està feta.

## Regla

Mai es cobra per **accedir, protegir o treure** les dades pròpies. Al Pro hi va el que fa l'app més còmoda o més potent.

## Principis d'implementació

- **No s'esborra codi.** El que passa a Pro s'amaga darrere d'un sistema de feature flag (els feature flags), amb un interruptor de debug per provar els dos modes.
- **A la primera release no es mostra res de Pro:** ni selectors, ni reclams, ni pantalles de compra.

## Gratis per sempre

- Persones, entrades, cronologia, «Qui» i «Quan».
- Persones protegides i bloqueig de l'app.
- Exportar notes.
- Els 5 grups de canals: En persona, Telèfon, Xat, Social i Altres (backlog 13).

## Pro (fora de la v1 gratuïta)

| Funció | A la v1 gratuïta | Amb Pro |
| --- | --- | --- |
| **Avatar personalitzat** (color i forma) | Forma fixa **Galeta** (`Cookie9Sided`) i color automàtic a partir del nom; no es poden canviar | Selector de color i forma a Editar persona / Nova persona |
| **Sub-canals** (backlog 8) | No existeix: només els 5 grups | Crear sub-canals dins de qualsevol grup, amb nom lliure (únic per persona) i valor opcional |
| **Follow-ups / pendents** (backlog 6) | No existeix | Marcar entrades com a pendents, secció «Pendents» al detall, comptador al llistat i recordatoris |
| **Color de la persona** (tema del detall, `PersonColorFeature`) | No hi és: les pantalles de la persona usen el tema de l'app i l'avatar només té el color automàtic pel nom | Activar el tema que pren el color de l'avatar (i l'ajust «Color de la persona» a Ajustos) |
| **Contact integration** | No existeix | Vincular una persona a un contacte (`LOOKUP_KEY`), enllaç a «Connected apps» de Contactes, avís de trucada entrant (`CallScreeningService`), importació de diversos contactes de cop (`READ_CONTACTS`) |

## Fet a la v1 gratuïta

- **Feature flags** (`data/flags/`): `CustomAvatarFeature` i `PersonColorFeature`, tots dos apagats; es provan amb el «Pro mode» del menú de debug. Els flags de sub-canals, follow-ups i contactes s'afegiran quan existeixin aquestes funcions. Encara no es comprova cap pagament.
- **Avatar estàndard:** Galeta i color automàtic pel nom (`AvatarColor.forName`); `avatarShape` i `avatarColor` nul·lables (`null` = estàndard).
- **Editor:** sense targeta d'avatar; l'avatar és a la fila del nom i, amb `CustomAvatarFeature`, obre el full «Avatar».

## Pendent per al Pro

- Play Billing i la comprovació de pagament al runner de flags.
- Sub-canals (backlog 8), follow-ups (backlog 6) i integració amb contactes, cadascun darrere del seu flag. Tots tres toquen la base de dades: **cal migració i test** (la v1 ja és publicada).

## Decidit

- **Canal «Empresa»** (backlog 7): ja no és un canal del sistema; és un sub-canal d'«Altres» (Pro).
- Els canals que els usuaris de la beta tenien (WhatsApp, Telegram…) es migren al seu grup en una sola entrada; no es mantenen gratis (backlog 13).

## Proves

- [ ] Amb totes les funcions apagades: no hi ha selector d'avatar, tots els avatars són Galeta, i el mateix nom dona sempre el mateix color.
- [x] Els 7 colors surten repartits amb noms diferents (`AvatarColorTest`, 20 noms).
- [x] Amb `CustomAvatarFeature` activada (debug): el selector torna a sortir i desa els valors.
