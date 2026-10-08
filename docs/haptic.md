# Haptic a Converses

Estat actual del feedback haptic de l'app, per passar-lo al Converses Design System.

## Criteri

El haptic confirma accions amb efecte i marca errors, igual que el vermell només s'usa per a errors. Sobri i coherent.

- **Mai** al scroll, a la navegació (tornar enrere, obrir pantalles), al plegat del títol de la TopAppBar, en obrir menús, en exportar ni en desfer.
- Respecta l'ajust del sistema (feedback tàctil i intensitat): amb el haptic desactivat, silenci.
- L'app té el seu propi interruptor a Ajustos → «Feedback tàctil» (`hapticFeedback`, actiu per defecte), que ho apaga tot.
- El `BiometricPrompt` del sistema ja vibra en èxit i error: l'app no hi afegeix res. Només vibra quan el canvi s'aplica després d'un èxit.

## Implementació

Única font: `ConversesHaptics` (`designsystem/components/Haptics.kt`), obtinguda amb `rememberConversesHaptics()`. Embolcalla `LocalHapticFeedback` i respecta l'interruptor de l'app (`LocalHapticsEnabled`).

| Mètode | `HapticFeedbackType` |
| --- | --- |
| `confirm()` | `Confirm` |
| `reject()` | `Reject` |
| `toggle(on)` | `ToggleOn` / `ToggleOff` |
| `segmentTick()` | `SegmentTick` |
| `longPress()` | `LongPress` |

## Components compartits (`:designsystem`)

| Component | Haptic | Detall |
| --- | --- | --- |
| `ConversesSaveButton` | `Confirm` / `Reject` | `Confirm` en tocar-lo. `Reject` si està desactivat i es toca (el toc es detecta al contenidor, sense consumir-lo). |
| `SettingSwitchRow` | `ToggleOn` / `ToggleOff` | Paràmetre `haptic = false` per als canvis no immediats (bloqueig de l'app), on vibra qui crida després de l'èxit. |
| `ConversesToggleGroup` | `SegmentTick` | Només si la selecció canvia (selector Qui, Tema). |
| `ConversesFilterChip` | `SegmentTick` | Filtres Jo · {nom} · Apunts del detall. |
| `ConversesChannelChip` | `LongPress` | Només en el toc llarg, que ara no s'usa enlloc. |

## Pantalles (`:app`)

| Interacció | Haptic | On |
| --- | --- | --- |
| Desa persona (nova o editar) | `Confirm` / `Reject` | `EditPersonScreen` |
| Desa nota ràpida | `Confirm` / `Reject` | `QuickNote` |
| Desa entrada (nova o editar) | `Confirm` / `Reject` | `NewEntrySheet` |
| Persona protegida | `ToggleOn` / `ToggleOff` | `EditPersonScreen`, després que l'empremta vagi bé |
| Bloqueig de l'app | `ToggleOn` / `ToggleOff` | `SettingsScreen`, després de l'empremta |
| Xips de canal | `SegmentTick` | `EntryForm` (només si canvia), `ChannelsCard` de l'editor i `FilterRow` del llistat |
| Reordenar dades | `LongPress` en agafar, `SegmentTick` a cada posició | `FactsEditor` |
| Esborra persona i esborra dia | `Confirm` | `DeletePersonDialog`, `ConfirmDeleteDialog` |
| Esborrar una entrada | `Confirm` | `PersonDetailScreen`, ⋮ → Esborra |
| Copiar, moure, arxivar o desarxivar | `Confirm` | `PersonDetailScreen` |

## Fora d'abast

- **Wear OS:** no aplicat, perquè no hi ha cap mòdul Wear al projecte.
- **`LongPress` a les accions d'una entrada i al menú del split button:** s'obren amb un toc, no hi ha pulsació llarga.
- **Error de validació:** l'únic cas és «àlies repetit», que desactiva Desa. Només vibra (`Reject`) quan es toca Desa desactivat, no cada cop que l'àlies passa a ser invàlid.
