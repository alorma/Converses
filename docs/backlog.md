# Backlog: bugs i features pendents

Actualitzat el 8 d'oct. de 2026. Només hi ha el que encara no està fet.

| # | Tipus | Tema | Issue |
| --- | --- | --- | --- |
| 6 | Feature (Iteració 2) | Follow-ups / entrades pendents | [#5](https://github.com/alorma/Converses/issues/5) |
| 7 | Feature (Iteració 2) | Nou canal "Empresa" (queda dins el 8) | — |
| 8 | Feature (Iteració 2) | Gestor de canals propis | — |
| 11 | Feature (Iteració 2) | Importar text a la nota ràpida (compartir, seleccionar text i enganxar) | [#30](https://github.com/alorma/Converses/issues/30) |
| 12 | Bug | Nota ràpida: el xip «Persona» desactivat confon | [#40](https://github.com/alorma/Converses/issues/40) |
| 13 | Millora | Llista: ordenar els filtres de canal pels més usats | [#42](https://github.com/alorma/Converses/issues/42) |
| 14 | Millora (a avaluar) | Detall de persona: les 3 accions visibles, sense el split FAB | [#44](https://github.com/alorma/Converses/issues/44) |
| 15 | Millora | Ajustos: secció «Quant a» | [#43](https://github.com/alorma/Converses/issues/43) |
| 16 | Millora | Idiomes: mostrar els disponibles a Ajustos i valorar gallec i italià | [#45](https://github.com/alorma/Converses/issues/45) |
| 17 | Feature | Cerca d'entrades per paraula clau | [#41](https://github.com/alorma/Converses/issues/41) |

Els punts 6, 7, 8 i 11 són de l'**Iteració 2**; el 6 i el 8 són Pro i l'11 és gratis (vegeu `docs/pro.md`). Els punts 12–17 vénen del feedback de la beta (issues #40–#45) i es treballen en aquest ordre: primer els petits (12, 13, 14), després el 15 i el 16, i la cerca (17) al final. **Qualsevol canvi de la base de dades ha d'incloure una migració i el seu test** (la v1 ja és a Google Play); l'11 no toca Room. Dels nous, només el 17 pot tocar Room (si s'opta per FTS). La numeració és la del backlog original, perquè els issues i les referències no canviïn; els punts resolts (1–5, 9, 10) són a l'historial de git.

---

## 6 · Feature: follow-ups / entrades pendents a la cronologia

Issue: [#5](https://github.com/alorma/Converses/issues/5)

Poder marcar coses per fer-ne seguiment amb una persona ("li he de preguntar com li ha anat l'entrevista", "m'ha de tornar el llibre"...).

**Decisió recomanada:** un flag `pending` a `EntryEntity` (i, opcionalment, una data de recordatori), no un quart tipus. «Qui» (Jo · {nom} · Apunt) és un grup de 3 botons que respon a «qui»; un «Follow-up» no hi encaixa, i amb el flag qualsevol entrada pot quedar pendent.

**Què cal fer**
- Camp `pending` a `EntryEntity` i **migració de Room** (versió 1 → 2) amb el seu test (`MigrationTestHelper`).
- Un interruptor o xip «Pendent» a `EntryForm`, a sota de «Quan», que és el formulari comú de la nota ràpida, la nova entrada i l'edició.
- Indicador visual a la cronologia, i potser un comptador o filtre de pendents al detall i a la llista.
- Marcar i desmarcar des del full d'accions de l'entrada.
- Opcional: recordatori o notificació en una data.
- Textos a `strings.xml` (en, ca, es) i actualitzar `docs/plan.md`.

## 7 · Feature: nou canal "Empresa" (entrevistes de feina)

- Afegir el valor a `enum class ChannelType` (`data/db/Entities.kt`); comprovar el converter de Room i, si l'esquema canvia, fer la migració.
- Icona a `ChannelType.icon()` (p. ex. `Icons.Rounded.Business` o `Work`).
- String a `ui/people/PeopleFormatting.kt` + `strings.xml` (en: "Company", ca/es: "Empresa").
- Decidir a quin `ChannelFilter` (`ui/people/PeopleListViewModel.kt`) pertany.

**Recomanació:** no fer-lo apart. Si es fa el 8, «Empresa» és un canal creat per l'usuari i no cal tocar l'enum.

## 8 · Feature: gestor de canals (crear canals globals propis)

Permetre crear canals propis (Signal, LinkedIn, Empresa...) en lloc de dependre només de l'enum `ChannelType`.

**Idea**
- Pantalla de gestió de canals (des d'Ajustos): llistar, crear, reanomenar, triar icona, reordenar, esborrar/arxivar.
- Els canals actuals es mantenen com a canals de sistema.

**Impacte tècnic**
- Passar de l'enum a una taula de canals a Room (id, nom, icona, sistema/usuari, ordre) amb **migració i test**: els canals i les entrades existents han d'apuntar als canals de sistema equivalents.
- Afecta: detall de persona, `EntryForm` (xips d'«On»), `ChannelsCard` de l'editor, `ChannelFilter` i `PersonExporter.kt`.
- Els noms creats per l'usuari no es tradueixen.

---

## 11 · Feature: importar text a la nota ràpida

Issue: [#30](https://github.com/alorma/Converses/issues/30) (el detall complet, el comportament i els criteris d'acceptació són a la issue).

Portar text d'altres apps a la nota ràpida amb el mínim de passos: el text ja hi és posat, l'usuari tria la persona i desa. WhatsApp, Telegram i companyia no comparteixen els missatges com a text (només deixen copiar), per això hi ha tres vies:

- **A. Compartir:** `ACTION_SEND` + `text/plain` (navegador, correu, notes, X, Reddit…).
- **B. Seleccionar text:** `ACTION_PROCESS_TEXT` (Converses surt al menú de selecció).
- **C. Enganxar:** botó «Enganxa» a la nota ràpida, visible quan el porta-retalls té text; el contingut només es llegeix quan es prem.

**Què cal fer**
- `intent-filter`s `SEND` i `PROCESS_TEXT` a `MainActivity`; `Intent.toBackStack()` com a deep link de Navigation 3 i `QuickNoteRoute(sharedText)`.
- Sense persones, «Crea la primera persona» obre l'editor i el text es manté en tornar.
- «Qui» per defecte a la persona (`HEARD`); provisionalment, un enllaç sol va a `HAPPENED` (pregunta oberta de la issue).
- **Privacitat:** el bloqueig de l'app surt abans de la nota ràpida; una persona protegida demana l'empremta i la pantalla té `FLAG_SECURE`.
- Fora d'abast: imatges i fitxers (`SEND_MULTIPLE`), Direct Share i xats exportats.
- Sense canvis a Room, **no cal migració**. Textos nous a `values/`, `values-ca/` i `values-es/`; en acabar, afegir-ho a «Gratis per sempre» de `docs/pro.md` i actualitzar `docs/plan.md`.

---

## 12 · Bug: xip «Persona» desactivat a la nota ràpida

Issue: [#40](https://github.com/alorma/Converses/issues/40)

A la nota ràpida, sense persona triada, el xip «Persona» de «Qui» surt gris i desactivat (i truncat, «Pers…»). Sembla el lloc on afegir la persona; els usuaris el toquen i no passa res, perquè la persona es tria a la targeta «Amb qui» de dalt.

**Decisió recomanada:** tocar el xip sense persona obre el selector de persones (el mateix full que la targeta «Amb qui»); en triar-ne una, el xip queda seleccionat.

**Què cal fer**
- `WhoSelector` d'`EntryForm` (`ui/entry/EntryForm.kt`): el xip deixa d'estar desactivat quan no hi ha persona i demana obrir el selector. L'etiqueta continua sortint de `whoLabel(...)` (`ui/person/WhoLabels.kt`).
- La nota ràpida és qui obre el full; el nou entry sheet de la persona no canvia.
- Etiqueta llegible: treure la icona del xip buit perquè «Persona» càpiga sense tallar-se.
- Protegides: el selector no ha de mostrar res de les entrades abans de desbloquejar.
- Sense Room, sense migració. Captura de la nota ràpida sense persona.

## 13 · Millora: ordre dels filtres de canal

Issue: [#42](https://github.com/alorma/Converses/issues/42)

Els filtres de la llista surten **Xat · Social · En persona · Telèfon**, i els dos més usats queden a la dreta, mig tallats.

- Ordre fix nou: **En persona, Telèfon, Xat, Social** (`ChannelGroup.entries` a `ui/people/PeopleListViewModel.kt`, recorregut a `FilterRow` de `ui/people/PeopleListScreen.kt`).
- Que els primers xips es vegin sencers sense fer scroll.
- Decidit de moment: ordre fix. Opcional més endavant: que l'usuari triï l'ordre, o ordenar-los per ús real.
- Sense Room. Actualitzar les captures.

## 14 · Millora (a avaluar): accions del detall de persona

Issue: [#44](https://github.com/alorma/Converses/issues/44)

Ara hi ha un split FAB amb **Nota** i un menú amb **Exportar notes** i **Arxivar**. Només són tres accions.

**Opcions**
- A. FAB només per a **Nota**; Exportar i Arxivar a la `topBar` (accions o menú «⋮»). Recomanada: escala millor quan `docs/pro.md` hi afegeixi accions.
- B. Barra flotant amb les tres accions visibles, al slot `floatingActionButton`.

Pendent de decidir. Seguir les regles del `Scaffold` del `CLAUDE.md`. Sense Room.

## 15 · Millora: «Quant a» als Ajustos

Issue: [#43](https://github.com/alorma/Converses/issues/43)

Entrada «Quant a» als Ajustos, amb subpantalla (`ConversesTopBar`, mateixes regles de `Scaffold`):
- Autor i una manera de contactar o seguir el projecte.
- Nom de l'app i versió.
- Enllaç al repositori (si és públic) i llicències de codi obert.
- Política de privadesa / fitxa de Play, i enviar feedback.

Pendent de decidir: dades i enllaços concrets. Textos a `strings.xml` (en, ca, es). Sense Room.

## 16 · Millora: idiomes disponibles

Issue: [#45](https://github.com/alorma/Converses/issues/45)

L'app només està en ca/es/en (`res/xml/locales_config.xml`); un idioma no suportat cau en silenci al següent de la llista.

**Dues parts independents**
- A. A Ajustos, «Idioma de l'app»: llistar els suportats i marcar l'actiu (i si segueix el del sistema). Esmentar-los a la fitxa de Play.
- B. Valorar afegir **gallec** i **italià** (suggerits per l'usuari). Cada idioma nou és un `values-xx/` complet i una entrada a `locales_config.xml`; es poden acceptar traduccions de la comunitat.

Pendent de decidir: només la A ara i la B segons qui tradueixi. Sense Room.

## 17 · Feature: cerca

Issue: [#41](https://github.com/alorma/Converses/issues/41)

Cerca pel nom de la persona i dins el text de les entrades, amb els resultats etiquetats per persona; tocar-ne un obre el detall a aquella entrada.

- **Protegides:** només es troben pel nom; el text de les seves entrades no es cerca ni es mostra abans de desbloquejar.
- **Tècnica:** consultes `LIKE` (sense canvi d'esquema, la recomanada per començar) o Room FTS (més ràpid amb molts registres, però exigeix taula nova, versió, migració i test). Si es tria FTS, coordinar-ho amb la migració del punt 6.
- Surt de «Després de la v1» del pla: ara és a la iteració 2.
