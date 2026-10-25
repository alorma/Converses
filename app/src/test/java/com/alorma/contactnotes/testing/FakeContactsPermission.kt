package com.alorma.contactnotes.testing

import com.alorma.contactnotes.data.contacts.ContactsPermission

class FakeContactsPermission(
  var granted: Boolean = true,
) : ContactsPermission {
  override fun isGranted(): Boolean = granted
}
