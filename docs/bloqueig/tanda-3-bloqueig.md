# Tanda 3 · Pantalla de bloqueig d'una persona protegida

Referència visual: llenç «Converses», secció **«Backlog 2 · Estats del bloqueig»**:

- **2f** · Esperant l'empremta.
- **2g** · Cancel·lat (als controls hi ha la variant «massa intents»).
- **2h** · Sense biometria.

Resol el **bug 2** del backlog. És independent de les tandes 1 i 2. Fa servir el color de la persona (`color-persona.md`).

**Fitxers:** `ui/person/PersonLockedScreen.kt`, `ui/person/BiometricUnlock.kt` i els textos a `strings.xml` (en, ca, es).

## Problema

Quan s'obre una persona protegida, la pantalla de bloqueig ensenya el layout de la persona mentre es demana l'empremta.

## Regla de privacitat

Fins que el desbloqueig no ha anat bé, **no es compon ni es carrega res de la persona**: ni dades fixades, ni canals, ni entrades. La pantalla de bloqueig només rep el **nom**, la **forma** i el **color** de l'avatar, que ja es veuen al llistat. El `ViewModel` del detall no ha de demanar les entrades fins que la ruta passi a `PersonRoute`.

## Estats

```kotlin
sealed interface LockState {
    data object Waiting : LockState        // BiometricPrompt obert
    data object Canceled : LockState       // l'usuari ha cancel·lat o error recuperable
    data class LockedOut(val seconds: Int) : LockState   // massa intents
    data object NoCredentials : LockState  // el dispositiu no té biometria ni bloqueig de pantalla
}
```

Totes les variants tenen la mateixa base:

- botó `arrow_back` a dalt;
- avatar de 112 dp amb la forma i el pastel de la persona i `lock` farcit;
- el nom (`person-name` 30/800), centrat.

| Estat | Sota el nom | Botó principal | Botó secundari |
| --- | --- | --- | --- |
| **Waiting** | Indicador de càrrega Expressive (`ContainedLoadingIndicator`, 56 dp) + «Esperant l'empremta…» | — | — |
| **Canceled** | `info` + **«No s'ha desbloquejat»** · «Has cancel·lat l'empremta. Les dades de {nom} continuen amagades.» | `fingerprint` **Torna-ho a provar** (`primary`) | **Torna enrere** (outlined) |
| **LockedOut** | `error` + **«Massa intents»** (en `error`) · «L'empremta no s'ha reconegut. Torna-ho a provar d'aquí a 30 segons o fes servir el PIN.» | `timer` **Torna-ho a provar (30 s)**: desactivat, amb compte enrere; s'activa en arribar a 0 | **Torna enrere** |
| **NoCredentials** | `no_encryption` + **«Sense bloqueig al dispositiu»** · «Aquest mòbil no té empremta ni bloqueig de pantalla. Configura'n un per poder obrir les persones protegides.» | `open_in_new` **Configura la seguretat** (`primary`) | **Torna enrere** |

- Botons a tot l'ample (32 dp de marge lateral), separats 8 dp: el principal fa 56 dp i el secundari 52 dp.
- **Torna enrere** fa `popBackStack()` i torna al llistat.

## Flux

1. En entrar a `PersonLockedRoute`, primer es comprova `BiometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)`:
   - si dona `BIOMETRIC_SUCCESS`, `LockState.Waiting` i s'obre el `BiometricPrompt` automàticament;
   - si dona `BIOMETRIC_ERROR_NONE_ENROLLED`, `BIOMETRIC_ERROR_NO_HARDWARE` o `BIOMETRIC_ERROR_HW_UNAVAILABLE`, `LockState.NoCredentials`.
2. Resultat del `BiometricPrompt`:
   - `onAuthenticationSucceeded`: substituir la ruta per `PersonRoute(id)`, com ara;
   - `ERROR_USER_CANCELED`, `ERROR_NEGATIVE_BUTTON`, `ERROR_CANCELED` o `ERROR_TIMEOUT` → `Canceled`;
   - `ERROR_LOCKOUT` → `LockedOut(30)`, amb el compte enrere al `ViewModel`;
   - `ERROR_LOCKOUT_PERMANENT` → `Canceled`, amb el text «Fes servir el PIN del dispositiu per desbloquejar-la». Amb `DEVICE_CREDENTIAL` permès, el prompt ja ofereix el PIN.
   - `ERROR_NO_BIOMETRICS` o `ERROR_NO_DEVICE_CREDENTIAL` → `NoCredentials`.
   - `onAuthenticationFailed` (un dit que no és): no fa res, el prompt continua obert.
3. **Torna-ho a provar** torna a obrir el prompt i passa a `Waiting`.
4. **Configura la seguretat** obre `Settings.ACTION_BIOMETRIC_ENROLL` amb `EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED = BIOMETRIC_STRONG or DEVICE_CREDENTIAL`. Si no es pot resoldre, obre `Settings.ACTION_SECURITY_SETTINGS`. En tornar a l'app (`ON_RESUME`) es fa de nou el pas 1.
5. Si l'app passa a segon pla durant `Waiting`, el prompt es cancel·la sol. En tornar, s'ha de quedar a `Canceled`, sense reobrir el prompt en bucle.

## Altres

- `FLAG_SECURE` també a aquesta pantalla, perquè no surti el nom a Recents amb dades a mig carregar.
- El `ContainedLoadingIndicator` és d'Expressive: si no és al `designsystem`, cal afegir-lo allà (on viu l'`@OptIn`).
- Animació entre estats: `AnimatedContent` amb fade de la zona de sota el nom. L'avatar i el nom no es mouen.

## Textos (`strings.xml`)

| Clau | ca | es | en |
| --- | --- | --- | --- |
| `lock_waiting` | Esperant l'empremta… | Esperando la huella… | Waiting for fingerprint… |
| `lock_canceled_title` | No s'ha desbloquejat | No se ha desbloqueado | Not unlocked |
| `lock_canceled_body` | Has cancel·lat l'empremta. Les dades de %1$s continuen amagades. | Has cancelado la huella. Los datos de %1$s siguen ocultos. | You canceled the fingerprint. %1$s's details stay hidden. |
| `lock_retry` | Torna-ho a provar | Vuelve a intentarlo | Try again |
| `lock_retry_in` | Torna-ho a provar (%1$d s) | Vuelve a intentarlo (%1$d s) | Try again (%1$d s) |
| `lock_lockout_title` | Massa intents | Demasiados intentos | Too many attempts |
| `lock_lockout_body` | L'empremta no s'ha reconegut. Torna-ho a provar d'aquí a 30 segons o fes servir el PIN. | No se ha reconocido la huella. Vuelve a intentarlo en 30 segundos o usa el PIN. | Fingerprint not recognized. Try again in 30 seconds or use your PIN. |
| `lock_lockout_permanent` | Fes servir el PIN del dispositiu per desbloquejar-la. | Usa el PIN del dispositivo para desbloquearla. | Use your device PIN to unlock. |
| `lock_none_title` | Sense bloqueig al dispositiu | Sin bloqueo en el dispositivo | No screen lock on this device |
| `lock_none_body` | Aquest mòbil no té empremta ni bloqueig de pantalla. Configura'n un per poder obrir les persones protegides. | Este móvil no tiene huella ni bloqueo de pantalla. Configura uno para poder abrir las personas protegidas. | This phone has no fingerprint or screen lock. Set one up to open protected people. |
| `lock_setup` | Configura la seguretat | Configurar la seguridad | Set up security |
| `lock_back` | Torna enrere | Volver | Go back |

## Pla en commits

1. `LockState` i la lògica de `BiometricUnlock.kt`: mapeig d'errors, comprovació inicial, compte enrere i `ON_RESUME`, amb tests unitaris.
2. `PersonLockedScreen` amb els quatre estats i el `ContainedLoadingIndicator`.
3. Strings (en, ca, es).
4. Previews (quatre estats × clar i fosc) i captures de referència.

## Proves

- [ ] Mentre el prompt està obert no es veu cap dada de la persona, i el `ViewModel` del detall encara no s'ha creat.
- [ ] Cancel·lar → «No s'ha desbloquejat»; **Torna-ho a provar** torna a obrir el prompt.
- [ ] Massa intents → el botó es desactiva amb compte enrere de 30 s i després s'activa.
- [ ] Sense biometria ni PIN → «Sense bloqueig al dispositiu»; configurar-ne un i tornar a l'app obre el prompt.
- [ ] Passar a segon pla amb el prompt obert no fa que es reobri en bucle.
- [ ] **Torna enrere** porta al llistat en tots els estats.
- [ ] Provat en un dispositiu real amb empremta (a l'emulador, només amb el PIN).
