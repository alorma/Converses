# Converses — Versió gratuïta (v1) i Pro

Què queda fora de la primera versió gratuïta, perquè passarà a ser Pro, i què es queda gratis per sempre. Document de treball: s'hi continuarà treballant (vegeu «Pro i versió gratuïta» a `docs/plan.md`).

Model previst per al Pro: **pagament únic**, sense subscripció i sense anuncis. La compra (Play Billing) no forma part d'aquests canvis.

## Regla

Mai es cobra per **accedir, protegir o treure** les dades pròpies. Al Pro hi va el que fa l'app més còmoda o més potent.

## Principis d'implementació

- **No s'esborra codi.** El que passa a Pro s'amaga darrere d'un sistema de feature flag (`FeatureGate`), amb un interruptor de debug per provar els dos modes.
- **A la primera release no es mostra res de Pro:** ni selectors, ni reclams, ni pantalles de compra.

## Gratis per sempre

- Persones, entrades, cronologia, «Qui» i «Quan».
- Persones protegides i bloqueig de l'app.
- Exportar notes.
- Canals del sistema + «Altres – {valor}».
- Importar **una** persona des del selector de contactes del sistema (`ACTION_PICK`, sense `READ_CONTACTS`). Pre-omple Nova persona.
- Color de la persona com a tema del detall (`color-persona.md`), amb el color automàtic de l'avatar.

## Pro (fora de la v1 gratuïta)

| Funció | A la v1 gratuïta | Amb Pro |
| --- | --- | --- |
| **Avatar personalitzat** (color i forma) | Forma fixa **Galeta** (`Cookie9Sided`) i color automàtic a partir del nom; no es poden canviar | Selector de color i forma a Editar persona / Nova persona |
| **Canals propis** (backlog 8) | No existeix: canals del sistema + «Altres – {valor}» | Crear canals amb nom i icona. Els «Altres – {valor}» es converteixen sols en canals propis (agrupant els valors repetits sense distingir majúscules ni espais) |
| **Follow-ups / pendents** (backlog 6) | No existeix | Marcar entrades com a pendents, secció «Pendents» al detall, comptador al llistat i recordatoris |
| **Contact integration** | No existeix | Vincular una persona a un contacte (`LOOKUP_KEY`), enllaç a «Connected apps» de Contactes, avís de trucada entrant (`CallScreeningService`), importació de diversos contactes de cop (`READ_CONTACTS`) |

## Canvis a fer per a la v1 gratuïta

### 1. Interruptor de funcions Pro

- Un sol punt de consulta, p. ex. `FeatureGate.isPro: StateFlow<Boolean>`. A la v1 sempre és `false`; més endavant el connectarà Play Billing.
- En debug, un interruptor a Ajustos (només en builds de debug) per provar els dos modes.

### 2. Avatar estàndard

- **Forma:** sempre **Galeta** (`MaterialShapes.Cookie9Sided`), la mateixa per a tothom.
- **Color:** automàtic, un dels 7 pastels. `AvatarColor.forName(name)` el tria amb un hash estable del nom normalitzat (minúscules, sense accents ni espais als extrems): el mateix nom dona sempre el mateix color. Si la persona es reanomena, el color pot canviar.
- **Model de dades:** l'app encara no és publicada, així que no cal migració. `avatarShape` i `avatarColor` passen a ser **nul·lables**: `null` vol dir «estàndard» (Galeta + color automàtic). A la v1 sempre es desen amb `null`. Amb Pro, s'hi desarà el que triï l'usuari.
- **Editar persona / Nova persona:** s'**amaga** la targeta de forma i color, i queda només l'avatar de previsualització a dalt del formulari.
- No es mostra cap reclam del Pro a la v1.

### 3. Fora del codi de la v1

No s'hi ha de començar res, o queda darrere de `FeatureGate.isPro`:

- backlog 6 (pendents);
- backlog 8 (gestor de canals);
- vincle amb contactes, Connected apps, avís de trucada i importació múltiple.

### 4. Afegit a la v1 gratuïta

- **Importar una persona des dels contactes:**
  - **a Nova persona:** una fila «Omple des d'un contacte»;
  - **al llistat buit (1b):** un segon botó «Importa dels contactes»;
  - **pre-omple:** nom, i el telèfon com a canal «Telèfon» (i «WhatsApp» si l'usuari el marca);
  - **duplicats:** si ja hi ha una persona amb el mateix número, avisa «Ja tens en {nom} · Obre-la».

## Pendent de decidir

- **Canal «Empresa»** (backlog 7): si es fa abans dels canals propis, és un canal del sistema gratuït; si no, serà un canal propi (Pro).

## Proves

- [ ] Amb `isPro = false`: no hi ha selector d'avatar, tots els avatars són Galeta, i el mateix nom dona sempre el mateix color.
- [ ] Els 7 colors surten repartits amb noms diferents (prova amb una llista de 20 noms).
- [ ] Amb `isPro = true` (debug): el selector torna a sortir i desa els valors.
- [ ] Importar un contacte sense haver concedit `READ_CONTACTS`.
- [ ] Importar un contacte amb un número que ja existeix avisa del duplicat.
