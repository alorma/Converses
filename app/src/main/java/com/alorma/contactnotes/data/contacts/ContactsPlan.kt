package com.alorma.contactnotes.data.contacts

/** Persona amb un contacte vinculat, tal com la veu el reconciliador. */
data class LinkedPerson(
  val id: Long,
  val contactId: Long,
  val lookupKey: String,
  val isProtected: Boolean,
)

/**
 * Què cal canviar a Contactes perquè les files coincideixin amb Room (`docs/contacts`, «Reconciliació»). És pur: el
 * calcula [of] i l'aplica `ContactsReconciler`. Aplicar-lo i tornar-lo a calcular dona un pla buit.
 */
data class ContactsPlan(
  val ensureAccount: Boolean = false,
  val removeAccount: Boolean = false,
  /** Persones a qui cal crear o refer les files (refer-les també esborra les velles). */
  val create: List<LinkedPerson> = emptyList(),
  /** Persones de qui cal esborrar les files. */
  val remove: List<Long> = emptyList(),
) {
  val isEmpty: Boolean get() = this == ContactsPlan()

  companion object {
    /**
     * [people] són totes les persones vinculades, també protegides i arxivades. Sense permís no es poden llegir ni
     * esborrar files ([rows] arriba buit): només es treu la compte, i el sistema n'esborra les files; els vincles es
     * conserven. Les protegides mai tenen files (C1); les arxivades sí (C4). Amb el flag apagat no queda res.
     */
    fun of(
      people: List<LinkedPerson>,
      enabled: Boolean,
      granted: Boolean,
      hasAccount: Boolean,
      rows: List<ContactRows>,
      labels: List<String>,
    ): ContactsPlan {
      if (!granted) return ContactsPlan(removeAccount = hasAccount)

      val wanted = if (enabled) people.filterNot { it.isProtected } else emptyList()
      val wantedIds = wanted.map { it.id }.toSet()
      val rowsByPerson = rows.groupBy { it.personId }
      val accountWanted = enabled && people.isNotEmpty()

      val create =
        wanted.filter { person ->
          val existing = rowsByPerson[person.id].orEmpty()
          // Sense compte, les files que hi quedin són de la compte esborrada i el sistema les traurà
          !hasAccount ||
            existing.size != 1 ||
            existing.single().contactId != person.contactId ||
            existing.single().labels != labels
        }
      val remove = rowsByPerson.keys.filterNot { it in wantedIds }.sorted()

      return ContactsPlan(
        ensureAccount = accountWanted && !hasAccount,
        removeAccount = !accountWanted && hasAccount,
        create = create,
        remove = remove,
      )
    }
  }
}
