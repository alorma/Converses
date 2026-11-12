package com.alorma.contactnotes.data.sync.engine

import com.alorma.contactnotes.data.sync.LocalState

/**
 * Qui guanya quan el mateix registre ha canviat aquí i al núvol. Es resol per registre sencer: la data de modificació
 * del client (la que va dins del xifrat) decideix, i en cas d'empat el `deviceId` més gran, perquè tots els
 * dispositius arribin al mateix resultat sense parlar-se. Aplicar dues vegades el mateix registre no fa res.
 */
internal object ConflictRules {
  /** Si el registre baixat ha de sobreescriure l'estat local. */
  fun remoteEditWins(
    local: LocalState,
    modifiedAt: Long,
    by: String,
    ownDeviceId: String,
  ): Boolean =
    when (local) {
      LocalState.Absent -> true
      // Un esborrat local només el desfà una edició posterior
      is LocalState.Deleted -> modifiedAt > local.deletedAt
      is LocalState.Live ->
        when {
          // El nostre propi registre que torna: si el local no és més antic, no hi ha res a aprendre
          by == ownDeviceId && local.modifiedAt >= modifiedAt -> false
          !local.dirty -> modifiedAt >= local.modifiedAt
          modifiedAt != local.modifiedAt -> modifiedAt > local.modifiedAt
          else -> by > ownDeviceId
        }
    }

  /**
   * Quan un registre baixat perd contra [local], si la versió d'aquí ha de tornar al núvol: només si ja s'havia pujat
   * (no és `dirty`) i el registre és d'un altre dispositiu, que l'ha trepitjada amb una escriptura més antiga.
   */
  fun mustReassert(
    local: LocalState.Live,
    by: String,
    ownDeviceId: String,
  ): Boolean = !local.dirty && by != ownDeviceId

  /** Si un esborrat baixat ha d'esborrar la fila local: guanya a una edició anterior o simultània, no a una posterior. */
  fun remoteDeletionWins(
    local: LocalState,
    deletedAt: Long,
  ): Boolean = local is LocalState.Live && deletedAt >= local.modifiedAt
}
