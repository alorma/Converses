# Backlog: bugs i features pendents

Actualitzat el 8 d'oct. de 2026. Només hi ha el que encara no està fet.

| # | Tipus | Tema | Issue |
| --- | --- | --- | --- |
| 6 | Feature (Iteració 2) | Follow-ups / entrades pendents | [#5](https://github.com/alorma/Converses/issues/5) |
| 7 | Feature (Iteració 2) | Nou canal "Empresa" (queda dins el 8, com a sub-canal d'Altres) | — |
| 8 | Feature (Iteració 2, Pro) | Sub-canals amb nom i valor (dins dels 5 grups del 13) | — |
| 11 | Feature (Iteració 2) | Importar text a la nota ràpida (compartir, seleccionar text i enganxar) | [#30](https://github.com/alorma/Converses/issues/30) |
| 13 | Tasca gran (gratis, la primera) | Refactor dels canals a 5 grups (migració v1 → v2) | [#42](https://github.com/alorma/Converses/issues/42) |
| 12 | Bug | Nota ràpida: el xip «Persona» desactivat confon | [#40](https://github.com/alorma/Converses/issues/40) |
| 14 | Millora | Detall de persona: les 3 accions visibles, sense el split FAB | [#44](https://github.com/alorma/Converses/issues/44) |
| 15 | Millora | Ajustos: secció «Quant a» | [#43](https://github.com/alorma/Converses/issues/43) |

Els punts 6, 7, 8 i 11 són de l'**Iteració 2**; el 6 i el 8 són Pro, i l'11 i el 13 són gratis (vegeu `docs/pro.md`). El punt 13 (refactor dels canals) és la tasca gran i la primera a fer. Els punts 12, 14 i 15 vénen del feedback de la beta (issues #40, #43 i #44; el #45 s'ha descartat i la cerca, #41, queda per més endavant (vegeu `docs/plan.md`)) i són tasques independents, després del 13. **Qualsevol canvi de la base de dades ha d'incloure una migració i el seu test** (la v1 ja és a Google Play); l'11 no toca Room. El 13 requereix la migració v1 → v2; la resta no toca Room. La numeració és la del backlog original, perquè els issues i les referències no canviïn; els punts resolts (1–5, 9, 10) són a l'historial de git.

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

**Absorbit pel 8.** Ja no és un valor de l'enum: serà un sub-canal d'«Altres» creat per l'usuari (Pro). No es fa apart.

## 8 · Feature (Pro): sub-canals

Depèn del 13 (els 5 grups). Dins de qualsevol grup, inclòs «Altres», l'usuari pot crear **sub-canals** amb un **nom lliure** i un **valor opcional** (número, usuari, enllaç).

- **Gratis:** els 5 grups. **Pro:** els sub-canals, de qualsevol grup.
- **Nom únic per persona**, sense distingir majúscules: «WhatsApp» dues vegades és un error; «WhatsApp personal» i «WhatsApp feina» són vàlids. Validació a l'editor, sense índex a Room.
- Taula nova a Room (persona, grup, nom, valor opcional) amb **migració i test**.
- Sense icones pròpies de moment: cada sub-canal usa la del seu grup.
- Si s'esborra un sub-canal, les entrades que hi apuntaven passen al grup (no es perd res).
- Afecta: detall de persona, `EntryForm` («On»), `ChannelsCard` de l'editor, filtres (sub-xips en seleccionar un grup) i `PersonExporter.kt`.
- Pregunta oberta: valor només informatiu o accionable (obrir WhatsApp, trucar, copiar). Si és accionable, mirar privadesa i permisos.
- Les persones protegides no han de mostrar sub-canals ni valors abans de desbloquejar.

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
- `WhoSelector` d'`EntryForm` (`ui/entry/EntryForm.kt`): el xip deixa d'estar desactivat sense persona i demana obrir el selector. L'etiqueta continua sortint de `whoLabel(...)` (`ui/person/WhoLabels.kt`).
- La nota ràpida és qui obre el full; el full de nova entrada de la persona no canvia.
- Etiqueta llegible: treure la icona del xip buit perquè «Persona» càpiga sense tallar-se.
- Protegides: el selector no ha de mostrar res de les entrades abans de desbloquejar.
- Sense Room, sense migració. Captura de la nota ràpida sense persona.

## 13 · Tasca gran: refactor dels canals a 5 grups

Issue: [#42](https://github.com/alorma/Converses/issues/42) (els filtres surten amb els més usats tallats a la dreta)

Els 11 valors de `ChannelType` passen a **5 grups fixos**: **En persona, Telèfon, Xat, Social, Altres**. Els filtres tornen a ser un a un amb el grup i caben sense scroll. És la primera tasca; els sub-canals (8) i «Empresa» (7) en depenen i són Pro.

**Decisions**
- Els canals que els usuaris de la beta ja tenen (WhatsApp, Telegram…) **es migren al seu grup com un de sol** i els antics s'eliminen. Mantenir-los gratis seria massa complex i aniria contra el Pro. Són uns 20 usuaris.
- Cada grup usa la seva icona; sense icones d'apps concretes.
- **Equivalències (decidides):** WhatsApp, Telegram, Signal, Discord i Correu → Xat; X, Instagram, LinkedIn i Reddit → Social; En persona i Telèfon es mantenen; «Altres» no rep cap canal existent.

**Estat:** fet el refactor de `ChannelType` i la migració (amb test instrumentat, pendent d'executar a l'emulador). Pendent de disseny: icones de Xat, Social i Altres (ara provisionals), avís genèric i badge «beta». Pendent: captures (etiqueta `update-screenshots` al PR).

**Què cal fer**
- **Migració de Room v1 → v2** amb el seu test (`MigrationTestHelper`), sense columnes noves: canvia el valor desat de `channel.type` i d'`entry.channelType`. Cal deduplicar els canals d'una persona que acabin al mateix grup. Test amb una persona amb diversos canals del mateix grup i entrades de cada tipus.
- `ChannelType` es redueix a 5 valors; `ChannelGroup` (`ui/people/PeopleListViewModel.kt`) gairebé desapareix.
- Afecta: detall de persona, `EntryForm` («On»), `ChannelsCard` de l'editor, `PeopleFormatting.kt`, `SampleData` i `PersonExporter.kt`.
- **Avís genèric** a la primera obertura després de l'actualització («Hem simplificat els canals…»). Sense detall per persona.
- **Badge «beta»** al títol de la primera pantalla (la llista de persones).
- Textos a `strings.xml` (en, ca, es), captures i actualitzar `docs/plan.md` i `docs/pro.md`.
- Pot compartir la migració v1 → v2 amb el 6 (follow-ups) si van junts.

## 14 · Millora: accions del detall de persona

Issue: [#44](https://github.com/alorma/Converses/issues/44)

Ara hi ha un split FAB amb **Nota** i un menú amb **Exportar notes** i **Arxivar**; l'usuari proposa tenir-les visibles.

**Decidit**
- **FAB estès només per a «Nota»** (adéu al split button).
- **`topBar`:** Enrere a l'esquerra; a la dreta, icones **Editar** i **Compartir** (exportar notes), i un «⋮» amb **Arxivar / Desarxivar**. Arxivar és l'acció menys freqüent i el menú deixa lloc a accions Pro futures.

**Què cal fer**
- `PersonDetailScreen.kt`: treure `ConversesSplitButton` del slot `floatingActionButton` i posar-hi un FAB estès; afegir les accions i el menú a la `topBar`.
- `contentDescription` i tooltip a les icones (la de compartir diu «Exportar notes»).
- Quan la persona és arxivada, l'entrada del menú passa a «Desarxivar».
- `ConversesSplitButton` es queda a `:designsystem`, encara que l'app no el faci servir.
- Persones protegides: l'exportació no canvia. Sense Room. Actualitzar les captures del detall.

## 15 · Millora: ampliar «Quant a» als Ajustos

Issue: [#43](https://github.com/alorma/Converses/issues/43)

Ajustos **ja té** una secció «Quant a» (`settings_about`) amb la política de privadesa i, al peu, «Converses · versió X». Cal ampliar-la perquè es vegi qui hi ha darrere de l'app.

**Decidit**
- **Opció A:** més files a la secció actual (`SettingsScreen.kt`), sense subpantalla.
- **Sense llicències de codi obert de moment** (quedaria per més endavant, amb una pantalla pròpia o AboutLibraries, mirant abans la versió estable).

**Files a afegir**, a sota de la privadesa
- **Qui ho fa:** el nom de l'autor amb un enllaç.
- **Codi font:** enllaç al repositori, si és públic.
- **Enviar feedback:** correu o formulari.
- Opcional: **Valorar a Google Play**.

**Dades a decidir:** nom i enllaç de l'autor, si el repositori és públic, i el canal del feedback (`mailto:` o issues de GitHub; el correu quedaria exposat).

Textos a `strings.xml` (en, ca, es) i captures d'Ajustos. Sense Room.
