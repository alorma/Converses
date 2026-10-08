# Backlog: bugs i features pendents

Actualitzat el 7 d'oct. de 2026. Només hi ha el que encara no està fet.

| # | Tipus | Tema | Issue |
| --- | --- | --- | --- |
| 6 | Feature (Iteració 2) | Follow-ups / entrades pendents | [#5](https://github.com/alorma/Converses/issues/5) |
| 7 | Feature (Iteració 2) | Nou canal "Empresa" (queda dins el 8) | — |
| 8 | Feature (Iteració 2) | Gestor de canals propis | — |
| 11 | Feature (Iteració 2) | Importar text a la nota ràpida (compartir, seleccionar text i enganxar) | [#30](https://github.com/alorma/Converses/issues/30) |

Tots són de l'**Iteració 2**, després de publicar l'app; el 6 i el 8 són Pro i l'11 és gratis (vegeu `docs/pro.md`). **Qualsevol canvi de la base de dades ha d'incloure una migració i el seu test** (la v1 ja és a Google Play); l'11 no toca Room. La numeració és la del backlog original, perquè els issues i les referències no canviïn; els punts resolts (1–5, 9, 10) són a l'historial de git.

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
