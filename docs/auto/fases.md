# Converses per a Android Auto — Fases

Context, abast, arquitectura, decisions (A0–A8) i riscos són a `docs/auto/README.md`. Cada fase acaba amb alguna cosa que es pot provar; no es comença la següent sense validar l'anterior.

| Fase | Títol | Depèn de | Estat |
| --- | --- | --- | --- |
| 0 | Decisions i investigació (categoria de Play, dictat, DHU) | — | Pendent |
| 1 | Esquelet: dependència, servei i primera pantalla | 0 | Pendent |
| 2 | Lectura: Persones i Últimes entrades | 1 | Pendent |
| 3 | Dictat | 0 (A4), 1 | Pendent |
| 4 | Qui i Desat | 2, 3 | Pendent |
| 5 | Bloqueig de l'app i estats d'error | 2, 4 | Pendent |
| 6 | Qualitat, privacitat i accessibilitat | 5 | Pendent |
| 7 | CI i publicació | 6 | Pendent |
| 8 | Més endavant | 7 | Idees |

---

Com es treballa cada fase (tasques, commits, validació, proposta d'inici): `.claude/rules/phased-work.md`.

---

## Fase 0 · Decisions i investigació

Sense codi de producte. **Bloquejant:** si Play no admet l'app, el pla s'atura aquí.

- [ ] **A1:** categoria de Play per a Android Auto. Llegir la documentació oficial (Car App Library, categories admeses, política de Play per a apps d'Android Auto) i dir si una app de notes per persona hi encaixa; si no, avaluar la via de missatgeria (`MessagingStyle` amb resposta per veu) i el seu cost. Decidir amb Bernat si es continua.
- [ ] **A4:** mecanisme de dictat permès al cotxe (`CarAudioRecord`, permís `RECORD_AUDIO`, restriccions mentre es condueix).
- [ ] Límits de plantilles amb el cotxe en marxa (nombre d'elements, text, nivell d'API mínim) i versions estables de `androidx.car.app`; anotar-ho al `README.md` i afegir-ho al version catalog.
- [ ] Preparar la **Desktop Head Unit** amb l'emulador de mòbil (o un mòbil real) i comprovar que s'hi veu una app de prova.
- [ ] Decidir si el codi viu a `:app` o a un mòdul `:auto` (vegeu l'arquitectura).
- [ ] Tancar A3, A5 i A6 (bloqueig, entrades i mòbil bloquejat) i demanar disseny només si cal (A7).
- [ ] Confirmar amb Bernat A2: les protegides no apareixen enlloc.

**Sortida:** decisions escrites, versions fixades i una DHU que funciona.

## Fase 1 · Esquelet

- [ ] Dependència `androidx.car.app` al catàleg de versions; `CarAppService`, `Session` i una `Screen` d'exemple; manifest amb la categoria acordada.
- [ ] Textos a `strings.xml` en en/ca/es (reutilitzant els del mòbil).
- [ ] ktlint; `testDebugUnitTest` i els APK actuals sense trencar-se.

**Sortida:** l'app apareix a la DHU amb una pantalla de prova.

## Fase 2 · Lectura

- [ ] Consulta de persones **no arxivades i no protegides** a SQL (sense canvi d'esquema) i test de JVM que ho garanteix.
- [ ] Pantalla Persones (`ListTemplate`) amb l'hora de l'última entrada.
- [ ] Pantalla Últimes entrades (≈ 5, text retallat; sense protegides).
- [ ] Tests de JVM del model de files (mapatge, límit, ordre, sense entrades).

**Sortida:** navegar Persones → Últimes entrades amb dades reals a la DHU.

## Fase 3 · Dictat

- [ ] Captura de veu pel camí validat a la fase 0 (A4) i permís de micròfon.
- [ ] Estats: sense veu, sense permís, error del reconeixedor.
- [ ] Idioma el de l'app (ca/es/en).

**Sortida:** dictar una frase i veure'n la transcripció al cotxe.

## Fase 4 · Qui i Desat

- [ ] «Qui»: Jo / {persona} / Apunt amb `whoLabel(...)`; per defecte la persona.
- [ ] Desar com la nota ràpida del mòbil (mateix cas d'ús, canal per defecte), amb confirmació breu i tornada a la persona.
- [ ] Tests de JVM; **cap canvi d'esquema de Room: sense migració**.

**Sortida:** apuntar una nota des del cotxe i veure-la al mòbil.

## Fase 5 · Bloqueig de l'app i estats d'error

- [ ] Bloqueig de l'app actiu: no es mostra cap dada (A3); test de JVM.
- [ ] Mòbil bloquejat, persona esborrada o feta protegida mentre s'usa, sense persones, sense entrades.
- [ ] Persona que passa a protegida amb la pantalla oberta: es tanca sense mostrar res.

**Sortida:** amb el bloqueig actiu només es veu el missatge; una protegida no apareix en cap cas.

## Fase 6 · Qualitat, privacitat i accessibilitat

- [ ] Repàs de privacitat: una protegida no arriba mai a la pantalla del cotxe ni als logs; test equivalent a `PrivacyAuditTest`.
- [ ] Mida de lletra, mode fosc/clar del cotxe, ca/es/en.
- [ ] Release amb R8: servei, plantilles i dictat.
- [ ] Cotxe real o unitat compatible: dictat, latència i permisos. **Pendent, no es pot fer sense cotxe.**

## Fase 7 · CI i publicació

- [ ] `pr-build.yml`: tests, ktlint i APK; `release.yml`: comprovar que el mateix AAB del mòbil serveix (A1 i versió mínima de l'API).
- [ ] Fitxa de Play: text per a Android Auto en en/ca/es i formulari de seguretat de les dades (A8). El text de la landing el canvia Bernat.
- [ ] Pista inicial `internal`, després `beta`.
- [ ] Notes de versió: proposar el punt; no es toquen sense acord.

## Fase 8 · Més endavant

- Lectura en veu alta de les entrades.
- Resposta per veu des d'una notificació de missatge.
- Android Automotive OS (APK propi, sincronització i bloqueig propis).
