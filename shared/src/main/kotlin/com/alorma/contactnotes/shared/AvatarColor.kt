package com.alorma.contactnotes.shared

import java.text.Normalizer

/**
 * Pastel fix de l'avatar i llavor de l'esquema de color de la persona, com a ARGB.
 * Cada plataforma el converteix al seu `Color`.
 */
enum class AvatarColor(
  val argb: Long,
  val seedArgb: Long,
) {
  Lavender(0xFFDCD3F5, 0xFF6B55B5),
  Mint(0xFFC9EBD6, 0xFF2F7A52),
  Peach(0xFFFFD6BF, 0xFFB5602E),
  Sky(0xFFCBE3F7, 0xFF2D6FA3),
  Rose(0xFFF7CFDD, 0xFFB04670),
  Lemon(0xFFF1E9A8, 0xFF8A7A12),
  Sand(0xFFE8DDCB, 0xFF86704C),
  ;

  companion object {
    /** Color automàtic: el mateix nom (sense majúscules, accents ni espais als extrems) dona sempre el mateix color. */
    fun forName(name: String): AvatarColor {
      val normalized = Normalizer.normalize(name.trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
      // FNV-1a: estable entre execucions i reparteix millor els noms curts que `String.hashCode`
      var hash = -0x7ee3623b
      for (char in normalized) hash = (hash xor char.code) * 16777619
      return entries[Math.floorMod(hash, entries.size)]
    }
  }
}
