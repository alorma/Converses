# Backlog: bugs i features pendents

Actualitzat el 7 d'oct. de 2026, reconciliat amb `docs/plan.md`. Només hi ha el que encara no està fet; el que ja s'ha resolt està resumit a `docs/plan.md` («Formulari d'entrada i "Quan"», «Qui» i «Bloqueig d'una persona protegida»).

| # | Tipus | Tema | Issue |
| --- | --- | --- | --- |
| 1 | Bug | L'àrea clicable surt fora de la superfície | [#6](https://github.com/alorma/Contacts-Notes/issues/6) |
| 3 | Bug | Canal "Other" ha de mostrar "Other - {valor}" | — |
| 5 | Bug | Activar/desactivar biometria sense desar el perfil | [#4](https://github.com/alorma/Contacts-Notes/issues/4) |
| 6 | Feature (Iteració 2) | Follow-ups / entrades pendents | [#5](https://github.com/alorma/Contacts-Notes/issues/5) |
| 7 | Feature (Iteració 2) | Nou canal "Empresa" (queda dins el 8) | — |
| 8 | Feature (Iteració 2) | Gestor de canals propis | — |

Els punts 6, 7 i 8 són features no prioritàries: es faran a l'**Iteració 2**, després de publicar l'app (vegeu `docs/plan.md`). La numeració és la del backlog original, perquè els issues i les referències no canviïn.

**Resolts** (ja són a l'app):

- **2** · La pantalla de bloqueig d'una persona ja no mostra el layout: només el nom, la forma i el color de l'avatar, amb els estats de càrrega, cancel·lat, massa intents i sense biometria.
- **4** · «Ha passat» ja no es talla: el selector és «Qui» (Jo · {nom} · Apunt), amb textos curts i la icona sempre visible. Queda per comprovar a M4 amb noms de 12 caràcters, en ca i es i amb la font gran.
- **9** · «Qui» i «Quan»: un sol `EntryForm`, fila «Quan» amb el full «Quan va ser?», cronologia, filtres, marcadors i exportació.

---

## 1 · Bug: l'àrea clicable surt fora de la superfície dels elements

Issue: [#6](https://github.com/alorma/Contacts-Notes/issues/6)

L'efecte de clic (ripple) i l'àrea clicable surten per fora de la superfície visible de l'element. S'ha vist a **Ajustos**, però cal revisar totes les pantalles.

**Què cal fer**
- Revisar `ui/settings/SettingsScreen.kt` i la resta de pantalles (llista, detall, edició, fulls inferiors).
- Normalment passa quan `Modifier.clickable` va abans del `clip`/`background`: l'ordre ha de ser `clip(shape)`, després `clickable`, després `padding`. O fer servir les variants de `Surface`/`ListItem` amb `onClick`, que ja retallen.
- Si hi ha un component compartit al `designsystem`, arreglar-ho allà.

## 3 · Bug: el canal "Other" hauria de mostrar "Other - {valor}"

`ChannelType.OTHER` es mostra només com a "Other"/"Altre", sense el valor introduït (p. ex. "Signal").

**Esperat:** `Other - Signal` (`Altre - Signal`, `Otro - Signal`). Si el valor és buit, només "Other".

**Què cal fer**
- String amb format, p. ex. `channel_other_with_value` = `"Other - %1$s"` a `values/`, `values-ca/` i `values-es/`.
- Fer-lo servir on es pinta el nom del canal: xips del detall, selector «On» d'`EntryForm`, etiqueta de cada entrada.
- Una entrada només desa `channelType`, no el valor de «Other». Per a l'etiqueta `qui · hora · canal` (`EntryCard`, `EntryActionsSheet` i `PersonExporter`) cal buscar el `handle` del canal `OTHER` de la persona: `PersonDetailUiState.channels` ja el té, i `EntryCard` només hauria de rebre'l.

**Fitxers:** `ui/people/PeopleFormatting.kt`, `ui/person/PersonFormatting.kt`, `ui/person/PersonDetailScreen.kt`, `ui/person/EntryActions.kt`, `ui/person/PersonExporter.kt`, `ui/entry/EntryForm.kt`.

## 5 · Bug: activar/desactivar la biometria d'una persona no hauria de requerir desar el perfil

Issue: [#4](https://github.com/alorma/Contacts-Notes/issues/4)

Ara mateix, per activar o desactivar el bloqueig biomètric d'una persona cal canviar l'interruptor i després **desar el perfil**.

**Esperat:** el canvi s'aplica directament en canviar l'interruptor, sense haver de desar la resta del formulari.

**Notes**
- Separar aquest camp de l'estat del formulari d'edició i desar-lo amb una crida pròpia al repositori/DAO.
- Valorar demanar la biometria abans de **desactivar**-la, perquè ningú la pugui treure sense autenticar-se (`promptUnlock` ja ho fa en activar-la).
- Si s'està creant una persona nova (encara sense id), mantenir el comportament actual.

**Fitxers:** `ui/edit/EditPersonScreen.kt`, `ui/edit/EditPersonViewModel.kt`.

## 6 · Feature: follow-ups / entrades pendents a la cronologia

Issue: [#5](https://github.com/alorma/Contacts-Notes/issues/5)

Poder marcar coses per fer-ne seguiment amb una persona ("li he de preguntar com li ha anat l'entrevista", "m'ha de tornar el llibre"...).

**Decisió recomanada:** un flag `pending` a `EntryEntity` (i, opcionalment, una data de recordatori), no un quart tipus. «Qui» (Jo · {nom} · Apunt) és un grup de 3 botons que respon a «qui»; un «Follow-up» no hi encaixa, i amb el flag qualsevol entrada pot quedar pendent.

**Què cal fer**
- Camp `pending` a `EntryEntity` i **migració de Room** (versió 2 → 3).
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
