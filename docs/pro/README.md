# Converses Pro

Pagament únic (`lifetime`), sense subscripció, sense anuncis i sense compte. Obert a tothom des de la 1.8.x.

## Regla

Mai es cobra per **accedir, protegir o treure** les dades pròpies. Al Pro hi va el que fa l'app més còmoda o més potent.

## Gratis per sempre

- Persones, entrades, cronologia, «Qui» i «Quan».
- Persones protegides i bloqueig de l'app.
- Exportar notes.
- Importar text a la nota ràpida (compartir, seleccionar text, enganxar).
- Els 5 grups de canals: En persona, Telèfon, Xat, Social i Altres.

## Pro

| Funció | Sense Pro | Amb Pro |
| --- | --- | --- |
| **Persones a la teva manera:** forma i color de l'avatar | Galeta i color automàtic pel nom. La píndola PRO de l'editor obre el full «Avatar», on les opcions Pro porten cadenat i **es poden provar** abans de comprar | Selector de forma i color |
| **Persones a la teva manera:** color de la persona | La fila d'Ajustos porta PRO i obre el paywall | Interruptor a Ajustos (apagat per defecte): les pantalles de la persona prenen el color de l'avatar |
| **Canals propis** | El camp «On» mostra «Canal propi» amb PRO | Canals amb nom dins de cada grup (nom únic dins del grup, sense distingir majúscules). Es trien a l'entrada, surten a la cronologia i a l'exportació, i es gestionen a Ajustos › Canals propis. Canviar un canal de grup hi mou les seves entrades |
| **Pendents** | «Nova tasca» al detall porta PRO | Tasques per persona (taula `task`, fora de la cronologia) amb data opcional, secció «Pendents» al detall, marcar com a fet amb «Desfés» i comptador al llistat (mai a les protegides) |
| **Fins a 5 dispositius** (darrere de `SyncFlag`, no públic) | Sincronització gratuïta amb 2 dispositius; l'accés a Pro des de Sincronització obre el paywall amb el benefici ressaltat (origen d'analítica `sync`) | Fins a 5 dispositius al mateix compte. El benefici només surt a la llista del paywall amb el flag encès (`ProBenefit.Devices`); apagat, el paywall no canvia |

**Baixada de Pro** (reemborsament o revocació): el que es va fer amb Pro es conserva i es continua veient (avatar, canals de les entrades, tasques, que es poden marcar com a fetes). Només no se'n pot canviar ni crear de nou. El tema de la persona s'apaga però la preferència queda desada. Mai s'esborren dades.

## Com funciona

- **RevenueCat**, configurat anònim quan el fil principal queda lliure (no a l'arrencada). Projecte Converses amb dues apps: Test Store (debug) i Play Store (`com.alorma.contactnotes`).
- **Entitlement `converses_pro`**, amb el producte `lifetime` de cada botiga. El codi mira sempre l'entitlement, mai el producte. L'offering `default` té un sol paquet i l'app compra el primer.
- **Claus:** `REVENUECAT_TEST_API_KEY` (debug) i `REVENUECAT_GOOGLE_API_KEY` (release), a `local.properties` i als secrets de GitHub. Sense clau no es configura RevenueCat i ningú no és Pro; `release.yml` falla si la clau és buida.
- **Sandbox Testing Access a «Anybody»** (RevenueCat › Project settings): si no, les compres de prova queden «Unattached» perquè l'app no té comptes.
- **Notificacions de Google (RTDN)** connectades al topic `Play-Store-Notifications` del projecte de Google Cloud `converses`.
- **Codi:** `ProRepository.isPro` és l'única font; la UI el llegeix amb `rememberIsPro()`. Sense Pro les funcions surten amb l'etiqueta PRO i porten al paywall (`PaywallRoute(highlight: ProBenefit?)`), que mai s'obre sol. La infraestructura de feature flags (`data/flags/`) es manté, buida, per a funcions noves.
- **Paywall:** preu, compra i restauració, estats de pendent, error, cancel·lació i sense connexió. Si RevenueCat no confirma la compra, el paywall deixa d'esperar 15 s després de tornar del full de Play.

## Debug

- Menú de debug › **Pro**: l'interruptor «Pro user» mostra l'estat real del Test Store o en fa override; també hi ha «Buy Pro (Test Store)» i «Restore purchases».
- Per tornar a gratuït amb Pro comprat al Test Store: copiar i esborrar `shared_prefs/*revenuecat*` de `com.alorma.contactnotes.dev` (nou usuari anònim) i restaurar-los després.
- Per comprovar que un AAB de Play porta la clau: App bundle explorer › Download, i buscar `goog_` al `classes.dex`.

## Pendent

A `docs/backlog.md` («Avisa'm» als pendents i «Termes» al paywall).
