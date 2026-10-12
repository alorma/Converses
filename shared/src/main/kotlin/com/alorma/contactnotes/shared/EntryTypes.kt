package com.alorma.contactnotes.shared

/**
 * Els 5 grups fixos de canals. L'ordre de declaració és l'ordre fix amb què es mostren, del més habitual al menys.
 * Els valors es desen pel nom: la migració 1 → 2 va fusionar els antics (WhatsApp, X…) al seu grup.
 */
enum class ChannelType { IN_PERSON, PHONE, CHAT, SOCIAL, OTHER }

enum class EntryKind { SAID, HEARD, HAPPENED }
