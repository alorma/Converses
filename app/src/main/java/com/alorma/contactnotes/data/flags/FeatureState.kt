package com.alorma.contactnotes.data.flags

/** En quin estat és una funció darrere d'un flag (`docs/pro/fases.md`, «Model»). */
enum class FeatureState {
  /** Flag apagat: no surt res. */
  Hidden,

  /** Flag Pro encès però l'usuari no és Pro: surt amb l'etiqueta PRO i no funciona. */
  Locked,

  /** Funciona. */
  Unlocked,
}
