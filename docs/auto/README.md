# Converses per a Android Auto — Pla

Esborrany del 12 d'oct. de 2026 · @Bernat · **encara no hi ha disseny.** Aquest document recull l'abast, l'arquitectura, les decisions i els riscos; les fases són a `docs/auto/fases.md`. Es va inspirar en el pla de Wear (ja esborrat, és a l'historial de git), però **és una plataforma diferent**: Android Auto és una projecció del mòbil, no un dispositiu amb la seva pròpia còpia de les dades.

## Objectiu

Al cotxe, sense mirar el mòbil: **dictar una nota per veu** a una persona existent i **llegir-ne les últimes entrades**, amb les restriccions de distracció del conductor.

## Premisses

1. **L'app corre al mòbil.** Android Auto només n'és la pantalla: no hi ha mòdul nou amb dades pròpies, ni còpia local, ni `DataClient`/`MessageClient`. La font de veritat continua sent Room, llegida directament.
2. **Sense sincronització de persones protegides** (decidit amb Bernat): les protegides **no es toquen mai** des d'Auto. No hi ha aprovació biomètrica, ni testimoni, ni pantalla de bloqueig per a elles.
3. **Sense núvol**, igual que a la resta de l'app. Cap permís nou llevat del micròfon si cal.
4. **Només lectura curta + una acció** (nota per veu). Res de crear, editar, arxivar ni esborrar persones; res d'Ajustos.
5. **Conducció primer:** poca informació per pantalla, textos curts, cap teclat mentre es condueix. Les plantilles i els límits els marca la Car App Library, no el disseny propi.

## Abast

### Dins

- **Llista de persones** no arxivades i **no protegides** (nom i avatar si la plantilla ho permet; hora de l'última entrada).
- **Últimes entrades** d'una persona (llista curta; text retallat; sense paginació infinita).
- **Nota per veu:** persona → dictat → «Qui» (Jo / {persona} / Apunt, per defecte la persona) → desat. Mateix canal per defecte que la nota ràpida (P9 de Wear) i mateixes etiquetes `whoLabel(...)`.
- **Bloqueig de l'app actiu:** vegeu A3.
- Textos en ca/es/en, reutilitzant els del mòbil.

### Fora

- **Persones protegides** (ni nom ni cadenat: vegeu A2). Persones arxivades.
- Crear, editar o esborrar res; Ajustos; Pro; color de persona; notificacions.
- Qualsevol cosa que obligui a teclejar o a llegir text llarg amb el cotxe en marxa.

## Arquitectura (proposta)

- **Mòdul:** cap de nou a priori; el codi d'Auto viu a `:app` (`CarAppService`, `Session`, `Screen` a `auto/`). Si el volum ho demana, es treu a un mòdul `:auto` com a biblioteca. **Això es decideix a la fase 0.**
- **Manifest:** servei `androidx.car.app.CarAppService` amb la categoria que Play accepti (A1) i `automotive_app_desc.xml` amb `<uses name="template"/>`; versió mínima de l'API de Car App.
- **UI:** plantilles de la Car App Library (`ListTemplate`, `PaneTemplate`, `MessageTemplate`…), **no Compose**: no hi ha disseny propi de pantalla; el tema el posa Android Auto. El `ConversesTheme` no s'hi aplica.
- **Dades:** `ConversesRepository` directament, amb consultes que **ja exclouen les protegides a SQL** (com el filtre de Wear, però en l'altre sentit: ni nom). Cap canvi d'esquema de Room, per tant cap migració. Si cal una consulta nova de DAO, no toca l'esquema.
- **Dictat:** per verificar (A4). Opcions: `CarAudioRecord` de la Car App Library + `SpeechRecognizer`, o l'entrada de veu del sistema. El text no s'ha d'escriure amb teclat.
- **Desar:** el mateix cas d'ús que la nota ràpida del mòbil; no un camí nou.
- **Privacitat:** l'app no ha de mostrar mai una protegida a la pantalla del cotxe ni al log; consulta SQL, test de JVM i `PrivacyAuditTest` equivalent al de Wear.

## Decisions

### Preses

| # | Decisió |
| --- | --- |
| A0 | Plataforma: **Android Auto** (projecció). Android Automotive OS queda fora d'aquest pla (`FormFactor.AUTOMOTIVE` del VersionPlugin no s'usa). |
| A0b | Abast: **nota per veu + llegir les últimes entrades**. |
| A2 | Protegides: **no hi són**, ni a la llista ni com a cadenat (a diferència de Wear). Inclinació; es confirma amb el disseny. |

### Obertes

| # | Decisió | Inclinació |
| --- | --- | --- |
| A1 | **Categoria a Play.** Android Auto només admet certes categories d'app (navegació, punts d'interès, IoT, meteorologia, càrrega i aparcament; missatgeria va per notificacions, no per plantilles). Una app de notes/converses **no encaixa clarament**. Cal comprovar-ho a la documentació oficial i a la política de Play abans d'escriure codi. | Fase 0, **bloquejant**; si no encaixa, plantejar la via de missatgeria (notificacions amb `MessagingStyle` i resposta per veu) o abandonar |
| A3 | **Bloqueig de l'app actiu:** la pantalla del cotxe no pot demanar biometria. | No mostrar cap dada (missatge «Desbloqueja al mòbil») |
| A4 | **Dictat al cotxe:** quin camí és permès (micròfon del cotxe, permís, categoria). | Fase 0 |
| A5 | **Entrades:** quantes, text retallat a quin límit, llegides en veu alta o només en pantalla. | Poques (≈ 5), sense TTS a la v1 |
| A6 | **Mòbil bloquejat o sense sessió:** què es veu. | Només el missatge d'A3 |
| A7 | **Disseny:** és les plantilles de la biblioteca, però cal acordar l'ordre de pantalles i els textos. | Es demana a Claude Design només si hi ha pantalles pròpies |
| A8 | **Política de privacitat i fitxa de Play:** si Auto canvia el formulari de seguretat de les dades. | Fase 6 |

## Riscos

- **R1 · Rebuig de Play (A1).** És el risc principal: pot invalidar tot el pla. Per això la fase 0 és abans de qualsevol codi.
- **R2 · Fuga de protegides.** Qualsevol camí que les llegeixi (llista, cerca, nota nova) és un error de privacitat; filtre a SQL, no a la UI.
- **R3 · Pantalla compartida.** Passatgers i cotxes de lloguer o d'altri veuen el contingut; per això A3 és restrictiu per defecte.
- **R4 · Distracció del conductor.** Límits de plantilles (nombre d'elements, text) i possible rebuig en revisió; es respecten, no se salten.
- **R5 · Dictat no permès o no fiable** (A4); sense dictat la funció principal cau.
- **R6 · Testabilitat.** Sense Desktop Head Unit (DHU) no es pot validar res visual; la DHU s'ha de poder fer servir amb l'emulador, si no, només hi ha tests de JVM.
- **R7 · Dependència nova** (`androidx.car.app`); revisar com afecta la fitxa i el tamany de l'APK.

## Proves

Tests de JVM per a la lògica (consulta que exclou protegides, mapatge a files, límit d'entrades, bloqueig de l'app, desat de la nota); els `Screen` es proven com a Wear prova els seus ViewModels, separant la lògica de la plantilla. Res d'instrumentat al CI. Validació manual amb **Desktop Head Unit** + emulador de mòbil i, després, un cotxe real o una unitat compatible. Casos obligats: persona protegida (no apareix), persona arxivada, bloqueig de l'app actiu, mòbil bloquejat, sense entrades, sense micròfon o permís, nota duplicada i canvi d'idioma.
