# Backlog: bugs i features pendents

Actualitzat el 7 d'oct. de 2026, reconciliat amb `docs/plan.md`. Només hi ha el que encara no està fet; el que ja s'ha resolt està resumit a `docs/plan.md` («Formulari d'entrada i "Quan"», «Qui» i «Bloqueig d'una persona protegida»).

| # | Tipus | Tema | Issue |
| --- | --- | --- | --- |
| 3 | Bug | Canal "Other" ha de mostrar "Other - {valor}" | — |
| 5 | Bug | Activar/desactivar biometria sense desar el perfil | [#4](https://github.com/alorma/Contacts-Notes/issues/4) |
| 10 | Seguretat | Demanar l'empremta en triar una persona protegida a la nota ràpida | — |
| 6 | Feature (Iteració 2) | Follow-ups / entrades pendents | [#5](https://github.com/alorma/Contacts-Notes/issues/5) |
| 7 | Feature (Iteració 2) | Nou canal "Empresa" (queda dins el 8) | — |
| 8 | Feature (Iteració 2) | Gestor de canals propis | — |

Els punts 6, 7 i 8 són features no prioritàries: es faran a l'**Iteració 2**, després de publicar l'app (vegeu `docs/plan.md`). La numeració és la del backlog original, perquè els issues i les referències no canviïn.

**Resolts** (ja són a l'app):

- **1** · L'àrea clicable sortia de la superfície: la fila d'interruptor d'Ajustos posava el `toggleable` fora del retall de la forma; ara va dins (`SettingRow.contentModifier`). La resta de components ja retallaven abans de fer-se clicables. (Issue #6.)
- **2** · La pantalla de bloqueig d'una persona ja no mostra el layout: només el nom, la forma i el color de l'avatar, amb els estats de càrrega, cancel·lat, massa intents i sense biometria.
- **4** · «Ha passat» ja no es talla: el selector és «Qui» (Jo · {nom} · Apunt), amb textos curts i la icona sempre visible. Queda per comprovar a M4 amb noms de 12 caràcters, en ca i es i amb la font gran.
- **9** · «Qui» i «Quan»: un sol `EntryForm`, fila «Quan» amb el full «Quan va ser?», cronologia, filtres, marcadors i exportació.

---

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

## 10 · Seguretat: demanar l'empremta en triar una persona protegida a la nota ràpida

**Problema.** Des del detall, per arribar a «Nota» cal desbloquejar la persona protegida. A la nota ràpida, en canvi, es pot triar una persona protegida (amb el nom i el cadenat) i escriure-hi sense demanar res. Escriure no filtra cap dada, però és un camí que se salta el bloqueig. Per això `QuickNoteViewModel` no carrega ni els canals ni l'última entrada de les protegides, i el selector «On» ensenya tots els canals.

**Què cal fer**
- En triar una persona protegida al full de triar persona (`PersonPickerSheet`), demanar l'empremta amb `promptUnlock` (el mateix que fan Ajustos i l'editor). Un sol prompt en triar-la; en desar no se'n torna a demanar.
- Si es cancel·la o falla: la persona **no** es tria i la nota ràpida es queda com estava (decisió a confirmar amb Bernat).
- Si va bé: ja es poden carregar els canals de la persona i el canal de l'última entrada (a `QuickNoteViewModel`, treure l'excepció per a les protegides), perquè «On» mostri el mateix que a les altres persones.
- Cas sense bloqueig al dispositiu: igual que a la resta de l'app, no es pot triar (no hi ha manera d'autenticar-se); mostrar el mateix missatge que la pantalla de bloqueig.
- Si la nota ràpida s'obre ja amb una persona protegida (`QuickNoteRoute(personId)`), també cal demanar-la en entrar.
- Pantalla de nota ràpida amb `FLAG_SECURE` mentre hi hagi una persona protegida triada (com l'editor), perquè no surti a Recents.

**Fitxers:** `ui/entry/QuickNote.kt`, `ui/entry/PersonPickerSheet.kt`, `ui/person/BiometricUnlock.kt`.

**Proves:** triar una protegida demana l'empremta; cancel·lar deixa la selecció com estava; amb l'empremta bona, «On» mostra els canals de la persona; no es demana de nou en desar; no hi ha captura a Recents.
