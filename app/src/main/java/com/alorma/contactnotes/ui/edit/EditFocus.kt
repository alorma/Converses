package com.alorma.contactnotes.ui.edit

import kotlinx.serialization.Serializable

/** On ha de posar el focus en obrir l'editor des d'un espai buit del detall. */
@Serializable
enum class EditFocus { CHANNELS, NEW_FACT }
