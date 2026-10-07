# Backlog: bugs i features pendents

Actualitzat el 7 d'oct. de 2026, reconciliat amb `docs/plan.md`. Només hi ha el que encara no està fet; el que ja s'ha resolt està resumit a `docs/plan.md` («Formulari d'entrada i "Quan"», «Qui» i «Bloqueig d'una persona protegida»).

| # | Tipus | Tema | Issue |
| --- | --- | --- | --- |
| 6 | Feature (Iteració 2) | Follow-ups / entrades pendents | [#5](https://github.com/alorma/Converses/issues/5) |
| 7 | Feature (Iteració 2) | Nou canal "Empresa" (queda dins el 8) | — |
| 8 | Feature (Iteració 2) | Gestor de canals propis | — |

Els punts 6, 7 i 8 són features no prioritàries: es faran a l'**Iteració 2**, després de publicar l'app (vegeu `docs/plan.md`). La numeració és la del backlog original, perquè els issues i les referències no canviïn.

**Resolts** (ja són a l'app):

- **1** · L'àrea clicable sortia de la superfície: la fila d'interruptor d'Ajustos posava el `toggleable` fora del retall de la forma; ara va dins (`SettingRow.contentModifier`). La resta de components ja retallaven abans de fer-se clicables. (Issue #6.)
- **2** · La pantalla de bloqueig d'una persona ja no mostra el layout: només el nom, la forma i el color de l'avatar, amb els estats de càrrega, cancel·lat, massa intents i sense biometria.
- **4** · «Ha passat» ja no es talla: el selector és «Qui» (Jo · {nom} · Apunt), amb textos curts i la icona sempre visible. Queda per comprovar a M4 amb noms de 12 caràcters, en ca i es i amb la font gran.
- **3** · El canal «Other» mostra el valor: «Other - Signal» (`channelName` a `PersonFormatting.kt`, amb `channel_with_value`) als xips del detall, al selector «On», a l'etiqueta de cada entrada, al full d'accions, a l'exportació i a la fila del llistat. Fa servir el nom ja existent del canal: «Altres - Signal» en català i «Otros - Signal» en castellà. **Substituït (#20):** els canals ja no porten valor ni hi ha «Altres»; vegeu `docs/plan.md` › «On hi parles».
- **5** · Activar o desactivar la biometria d'una persona es desa a l'acte (`EditPersonViewModel.onProtected` i `ConversesRepository.setProtected`); en una persona nova es continua desant amb el perfil. Ja demanava l'empremta tant en activar com en desactivar. (Issue #4.)
- **10** · Triar una persona protegida a la nota ràpida demana l'empremta (`QuickNoteViewModel`: `pending`, `unlock` i `cancelPending`). Si es cancel·la, la selecció es queda com estava; sense bloqueig al dispositiu surt el diàleg «Sense bloqueig» amb «Configura la seguretat». Només un cop desbloquejada es carreguen els seus canals i el de l'última entrada, i la pantalla té `FLAG_SECURE` mentre hi ha una protegida triada o pendent.
- **9** · «Qui» i «Quan»: un sol `EntryForm`, fila «Quan» amb el full «Quan va ser?», cronologia, filtres, marcadors i exportació.

---

## 6 · Feature: follow-ups / entrades pendents a la cronologia

Issue: [#5](https://github.com/alorma/Converses/issues/5)

Poder marcar coses per fer-ne seguiment amb una persona ("li he de preguntar com li ha anat l'entrevista", "m'ha de tornar el llibre"...).

**Decisió recomanada:** un flag `pending` a `EntryEntity` (i, opcionalment, una data de recordatori), no un quart tipus. «Qui» (Jo · {nom} · Apunt) és un grup de 3 botons que respon a «qui»; un «Follow-up» no hi encaixa, i amb el flag qualsevol entrada pot quedar pendent.

**Què cal fer**
- Camp `pending` a `EntryEntity` i **migració de Room** (versió 1 → 2).
- Un interruptor o xip «Pendent» a `EntryForm`, a sota de «Quan», que és el formulari comú de la nota ràpida, la nova entrada i l'edició.
- Indicador visual a la cronologia, i potser un comptador o filtre de pendents al detall i a la llista.
- Marcar i desmarcar des del full d'accions de l'entrada.
- Opcional: recordatori o notificació en una data.
- Textos a `strings.xml` (en, ca, es) i actualitzar `docs/plan.md`.

## 7 · Feature: nou canal "Empresa" (entrevistes de feina)

- Afegir el valor a `enum class ChannelType` (`data/db/Entities.kt`); comprovar el converter de Room i si cal migració.
- Icona a `ui/person/PersonFormatting.kt` (p. ex. `Icons.Rounded.Business` o `Work`).
- String a `ui/people/PeopleFormatting.kt` + `strings.xml` (en: "Company", ca/es: "Empresa").
- Decidir a quin `ChannelFilter` (`ui/people/PeopleListViewModel.kt`) pertany.

**Recomanació:** no fer-lo apart. Si es fa el 8, «Empresa» és un canal creat per l'usuari i no cal tocar l'enum.

## 8 · Feature: gestor de canals (crear canals globals propis)

Permetre crear canals propis (Signal, LinkedIn, Empresa...) en lloc de dependre només de l'enum `ChannelType` + "Other".

**Idea**
- Pantalla de gestió de canals (des d'Ajustos): llistar, crear, reanomenar, triar icona, reordenar, esborrar/arxivar.
- Els canals actuals es mantenen com a canals de sistema.

**Impacte tècnic**
- Passar de l'enum a una taula `Channel` a Room (id, nom, icona, sistema/usuari, ordre) amb migració, incloent convertir els `OTHER` amb valor en canals propis. Això també resol el 3.
- Afecta: detall de persona, `EntryForm` (xips d'«On»), edició de persona, `ChannelFilter`, `ui/person/ChannelLinks.kt` i `PersonExporter.kt`.
- Els noms creats per l'usuari no es tradueixen.
