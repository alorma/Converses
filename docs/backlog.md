# Backlog: bugs i features trobats provant l'app (2026-10-06)

Alguns ja són issues a GitHub; la resta són aquí perquè l'API de GitHub fallava en crear-los.

| # | Tipus | Tema | On |
| --- | --- | --- | --- |
| 1 | Bug | L'àrea clicable surt fora de la superfície | [#6](https://github.com/alorma/Contacts-Notes/issues/6) |
| 2 | Bug | La pantalla de bloqueig de persona mostra el layout | aquí |
| 3 | Bug | Canal "Other" ha de mostrar "Other - {valor}" | aquí |
| 4 | Bug | A "Nova entrada", "Ha passat" es talla | aquí |
| 5 | Bug | Activar/desactivar biometria sense desar el perfil | [#4](https://github.com/alorma/Contacts-Notes/issues/4) |
| 6 | Feature | Follow-ups / entrades pendents | [#5](https://github.com/alorma/Contacts-Notes/issues/5) |
| 7 | Feature | Nou canal "Empresa" | aquí |
| 8 | Feature | Gestor de canals propis | aquí |

---

## 2 · Bug: la pantalla de bloqueig d'una persona mostra el layout

Quan s'obre una persona amb bloqueig biomètric, la pantalla de bloqueig mostra el layout mentre es demana la biometria.

**Comportament proposat**
- Mentre es demana la biometria: només un estat de **càrrega** (sense layout de la persona).
- Si l'usuari **cancel·la** o falla: estat d'**error** amb un botó per tornar-ho a intentar (i/o tornar enrere).
- Si no hi ha biometria disponible: missatge clar i un botó d'acció (desbloquejar amb credencial del dispositiu o tornar).

**Fitxers:** `ui/person/PersonLockedScreen.kt`, `ui/person/BiometricUnlock.kt`. Textos nous a `strings.xml` (en, ca, es).

## 3 · Bug: el canal "Other" hauria de mostrar "Other - {valor}"

`ChannelType.OTHER` es mostra només com a "Other"/"Altre", sense el valor introduït (p. ex. "Signal").

**Esperat:** `Other - Signal` (`Altre - Signal`, `Otro - Signal`). Si el valor és buit, només "Other".

**Què cal fer**
- String amb format, p. ex. `channel_other_with_value` = `"Other - %1$s"` a `values/`, `values-ca/` i `values-es/`.
- Fer-lo servir on es pinta el nom del canal: xips del detall, selector "On" del formulari d'entrada, cronologia.

**Fitxers:** `ui/people/PeopleFormatting.kt`, `ui/person/PersonFormatting.kt`, `ui/entry/EntryForm.kt`.

## 4 · Bug: a "Nova entrada", l'opció "Ha passat" es talla

El grup de botons de tipus (Li he dit / M'ha dit / Ha passat) es talla: amb "Ha passat" seleccionat i la icona, només es veu "Ha".

**Opcions**
- Fila amb scroll horitzontal (com el selector "On").
- No afegir la icona a l'element seleccionat o redistribuir l'amplada perquè el text càpiga.
- Permetre 2 línies o reduir el padding.

Comprovar-ho en català i castellà (textos més llargs) i amb mida de font gran.

**Fitxers:** `ui/entry/EntryForm.kt`, `ui/entry/NewEntrySheet.kt`.

## 7 · Feature: nou canal "Empresa" (entrevistes de feina)

- Afegir el valor a `enum class ChannelType` (`data/db/Entities.kt`); comprovar el converter de Room i si cal migració.
- Icona a `ui/person/PersonFormatting.kt` (p. ex. `Icons.Rounded.Business` o `Work`).
- String a `ui/people/PeopleFormatting.kt` + `strings.xml` (en: "Company", ca/es: "Empresa").
- Decidir a quin `ChannelFilter` (`ui/people/PeopleListViewModel.kt`) pertany.

Si es fa abans el gestor de canals (8), potser passa a ser un canal creat per l'usuari.

## 8 · Feature: gestor de canals (crear canals globals propis)

Permetre crear canals propis (Signal, LinkedIn, Empresa...) en lloc de dependre només de l'enum `ChannelType` + "Other".

**Idea**
- Pantalla de gestió de canals (des d'Ajustos): llistar, crear, reanomenar, triar icona, reordenar, esborrar/arxivar.
- Els canals actuals es mantenen com a canals de sistema.

**Impacte tècnic**
- Passar de l'enum a una taula `Channel` a Room (id, nom, icona, sistema/usuari, ordre) amb migració, incloent convertir els `OTHER` amb valor en canals propis.
- Afecta: detall de persona, formulari d'entrada, edició de persona, `ChannelFilter`, `ui/person/ChannelLinks.kt` i `PersonExporter.kt`.
- Els noms creats per l'usuari no es tradueixen.
