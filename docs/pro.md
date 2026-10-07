# Converses — Versió gratuïta (v1) i Pro

Què queda fora de la primera versió gratuïta, perquè passarà a ser Pro, i què es queda gratis per sempre. **La v1 no té Pro. Els canvis de «Canvis a fer per a la v1 gratuïta» s'han de fer abans de la release 1.** Document de treball: s'hi continuarà treballant (vegeu «Versió gratuïta i Pro» a `docs/plan.md`).

Model previst per al Pro: **pagament únic**, sense subscripció i sense anuncis. La compra (Play Billing) no forma part d'aquests canvis.

## Regla

Mai es cobra per **accedir, protegir o treure** les dades pròpies. Al Pro hi va el que fa l'app més còmoda o més potent.

## Principis d'implementació

- **No s'esborra codi.** El que passa a Pro s'amaga darrere d'un sistema de feature flag (els feature flags), amb un interruptor de debug per provar els dos modes.
- **A la primera release no es mostra res de Pro:** ni selectors, ni reclams, ni pantalles de compra.

## Gratis per sempre

- Persones, entrades, cronologia, «Qui» i «Quan».
- Persones protegides i bloqueig de l'app.
- Exportar notes.
- Canals del sistema + «Altres – {valor}».

## Pro (fora de la v1 gratuïta)

| Funció | A la v1 gratuïta | Amb Pro |
| --- | --- | --- |
| **Avatar personalitzat** (color i forma) | Forma fixa **Galeta** (`Cookie9Sided`) i color automàtic a partir del nom; no es poden canviar | Selector de color i forma a Editar persona / Nova persona |
| **Canals propis** (backlog 8) | No existeix: canals del sistema + «Altres – {valor}» | Crear canals amb nom i icona. Els «Altres – {valor}» es converteixen sols en canals propis (agrupant els valors repetits sense distingir majúscules ni espais) |
| **Follow-ups / pendents** (backlog 6) | No existeix | Marcar entrades com a pendents, secció «Pendents» al detall, comptador al llistat i recordatoris |
| **Color de la persona** (tema del detall, `PersonColorFeature`) | No hi és: les pantalles de la persona usen el tema de l'app i l'avatar només té el color automàtic pel nom | Activar el tema que pren el color de l'avatar (i l'ajust «Color de la persona» a Ajustos) |
| **Contact integration** | No existeix | Vincular una persona a un contacte (`LOOKUP_KEY`), enllaç a «Connected apps» de Contactes, avís de trucada entrant (`CallScreeningService`), importació de diversos contactes de cop (`READ_CONTACTS`) |

## Canvis a fer per a la v1 gratuïta

### 1. Interruptor de funcions Pro

- Feature flags a `data/flags/` amb l'estil de Caducity i FireAndForget: una classe per funció (de moment `CustomAvatarFeature` i `PersonColorFeature`; els flags de canals propis, follow-ups i integració amb contactes s'afegiran quan aquestes funcions existeixin), cadascuna activable per separat; ara tot és local (`LocalFeatureFlagRunner`). A la v1 totes estan apagades i no es comprova cap pagament (el Pro encara no existeix); més endavant el runner mirarà si s'ha pagat. La UI no mostra mai res de gratuït ni de Pro.
- L'interruptor per provar els dos modes viu al **menú de debug** (mòdul Pro), que crida `LocalFeatureFlagRunner.setOverride(key, value)`; Ajustos no en porta cap.

### 2. Avatar estàndard

- **Forma:** sempre **Galeta** (`MaterialShapes.Cookie9Sided`), la mateixa per a tothom.
- **Color:** automàtic, un dels 7 pastels. `AvatarColor.forName(name)` el tria amb un hash estable del nom normalitzat (minúscules, sense accents ni espais als extrems): el mateix nom dona sempre el mateix color. Si la persona es reanomena, el color pot canviar.
- **Model de dades:** l'app encara no és publicada, així que no cal migració. `avatarShape` i `avatarColor` passen a ser **nul·lables**: `null` vol dir «estàndard» (Galeta + color automàtic). A la v1 sempre es desen amb `null`. Amb Pro, s'hi desarà el que triï l'usuari.
- **Editar persona / Nova persona:** s'**amaga** la targeta de forma i color, i queda només l'avatar de previsualització a dalt del formulari.
- No es mostra cap reclam del Pro a la v1.

### 3. Fora del codi de la v1

No s'hi ha de començar res, o queda darrere de els feature flags:

- backlog 6 (pendents);
- backlog 8 (gestor de canals);
- vincle amb contactes, Connected apps, avís de trucada i importació múltiple.

## Pendent de decidir

- **Canal «Empresa»** (backlog 7): si es fa abans dels canals propis, és un canal del sistema gratuït; si no, serà un canal propi (Pro).

## Proves

- [ ] Amb totes les funcions apagades: no hi ha selector d'avatar, tots els avatars són Galeta, i el mateix nom dona sempre el mateix color.
- [ ] Els 7 colors surten repartits amb noms diferents (prova amb una llista de 20 noms).
- [ ] Amb `CustomAvatar` activada (debug): el selector torna a sortir i desa els valors.
