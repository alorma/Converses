package com.alorma.contactnotes.shared

/**
 * Inicials de l'avatar: la primera lletra de les dues primeres paraules del nom («ana bea carla» →
 * «AB»). Només compten lletres i números: «Pepe (pis)» → «PP», no «P(». Sense cap, «?».
 */
fun initialsOf(name: String): String =
  name
    .split(' ', '_', '-', '.')
    .map { word -> word.filter(Char::isLetterOrDigit) }
    .filter { it.isNotEmpty() }
    .take(2)
    .joinToString("") { it.first().uppercase() }
    .ifEmpty { "?" }
