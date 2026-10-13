package com.alorma.contactnotes.shared

/** Un dispositiu de l'altre costat de la Data Layer, sense dependre de Play Services. */
data class CompanionNode(
  val id: String,
  val displayName: String?,
  val isNearby: Boolean,
)

/** Si l'app és a l'altre dispositiu (mòbil per al rellotge, rellotge per al mòbil). */
sealed interface CompanionState {
  /** Algun node anuncia la capacitat de l'altre costat. */
  data object Present : CompanionState

  /** Hi ha un dispositiu emparellat i connectat, però sense l'app. */
  data class Missing(
    val node: CompanionNode,
  ) : CompanionState

  /** No hi ha cap dispositiu emparellat. */
  data object NoDevice : CompanionState
}

/**
 * Decideix l'estat des del que veu la Data Layer: els nodes amb la capacitat de l'altre costat i els nodes connectats.
 * Una capacitat que no es propaga (emuladors) surt com a [CompanionState.Missing]: no es pot distingir d'una app que no hi
 * és, i això és el que es vol detectar.
 */
fun companionStateOf(
  withApp: Collection<CompanionNode>,
  connected: Collection<CompanionNode>,
): CompanionState {
  if (withApp.isNotEmpty()) return CompanionState.Present
  val node = connected.firstOrNull { it.isNearby } ?: connected.firstOrNull()
  return if (node != null) CompanionState.Missing(node) else CompanionState.NoDevice
}

/** Sufix dels builds de debug; no existeix a Google Play. */
private const val DEBUG_ID_SUFFIX = ".dev"

/** Fitxa de l'app a Google Play, per obrir-la a l'altre dispositiu. Els builds de debug apunten a la fitxa real. */
fun playStoreUri(applicationId: String): String = "market://details?id=${applicationId.removeSuffix(DEBUG_ID_SUFFIX)}"
