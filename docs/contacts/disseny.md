# Contactes — Disseny (C8)

Lliurament de Claude Design del 25 d'oct. de 2026 (secció 6 i taulers 5a–5h de `pantalles.html`; la resta del lliurament no és d'aquest pla). Mides en dp; colors amb els tokens del tema (`bg`, `card`, `card-hi`, `on`, `on-var`, `outline`, `outline-var`, `pri`, `on-pri`, `pri-c`, `on-pri-c`).

**Tot va darrere de `ContactLinkFeature`.** Amb el flag apagat no es veu res; amb el flag encès es veuen les pantalles d'aquest document. **No hi ha estat «Gratuït» ni etiqueta «PRO»** (decisió de Bernat del 25 d'oct.): el lliurament els dibuixa, però no s'implementen fins que existeixi el Pro.

## Editor de persona (5a, 5c, 5d)

Secció «Contacte» entre el nom i les «Dades», amb tres estats:

- **Sense vincle:** fila de 80 dp, icona `person_add` dins d'un cercle de 44 dp, títol «Vincula un contacte», subtítol «Opcional · surt a la fitxa de Contactes» i `chevron_right`. Si és el primer vincle, obre l'explicació (5b); si no, el selector de contactes del sistema.
- **Vinculat:** fila de 80 dp, icona `contacts` (plena), etiqueta «VINCULAT A» i el nom del contacte. A la dreta, botó d'icona `link_off` de 48 dp («Treu el vincle» per a l'accessibilitat). Treure el vincle no demana confirmació i no toca ni les notes ni la persona.
- **Sense permís:** targeta a dues línies. A dalt, icona `lock`, «Sense permís de Contactes» i «Cal per vincular un contacte». A sota, alineat a la dreta, botó tonal (`pri-c`) «Dona permís», de 40 dp. Si el sistema ja no deixa tornar a preguntar, el botó obre els ajustos de l'app.

Sense permís **no es pot vincular** (substitueix la idea de la C7 de desar el vincle igualment).

La fila a **Nova persona** (3a) ha de ser la mateixa que a l'editor (no està dibuixada).

## Explicació del permís (5b)

Bottom sheet abans de demanar `READ_CONTACTS` i `WRITE_CONTACTS`, només la primera vegada que es vincula un contacte (mai en obrir l'app):

- Títol i text breu.
- Tres punts: el compte «Converses» que apareixerà a Ajustos › Comptes; que no es copia res a Contactes; què passa si l'usuari ho rebutja.
- «Continua» llança el diàleg del sistema; «Ara no» torna a l'editor sense vincle.

## Detall (5e, 5f)

Entre els pills i les «Dades», una fila en un d'aquests estats:

- **Vinculada:** fila de 76 dp amb «Vinculada a {nom}» (dues línies si el nom és llarg) i «Obre a Contactes», amb `open_in_new`. Tota la fila obre el contacte.
- **Contacte esborrat:** es detecta en obrir el detall. Targeta a dues línies amb vora d'1,5 dp `outline-var`: «El contacte ja no existeix» i «S'ha esborrat de Contactes. Les notes es queden aquí.». A sota, alineat a la dreta, botó «Desvincula». Si no es toca, la persona funciona igual.
- **Sense permís:** cap fila de contacte.
- **Persona protegida bloquejada:** no mostra el nom del contacte ni l'accés (ja ho garanteix `PersonLockedScreen`).

## Ajustos › Contactes (1c, 5h)

Secció «Contactes» entre Seguretat i Quant a, **només amb el flag encès** i sense etiqueta PRO. Tres estats:

1. **Mai activat:** «Vincle amb Contactes», «Encara no l'has activat. Et demanarem permís per veure els contactes.» i el botó «Activa», que passa per l'explicació (5b) i després pel diàleg del sistema.
2. **Activat:** una sola fila, «Activat · %d persones vinculades» (plurals). Les files «Compte «Converses»» i «Gestiona el permís» del disseny es van treure a la fase 5 (Bernat, 25-10-2026): quan tot va bé no cal explicar com treure la compte ni el permís.
3. **Denegat:** «Sense permís de Contactes» amb el botó «Dona permís» i «Les persones vinculades no mostren l'accés a Contactes. Les notes no es perden.».

A sota, sempre: «Les notes no es copien a Contactes: només s'hi afegeixen «Veure notes» i «Crear nota».».

## Textos (tauler 5g)

| Català | English | Español |
| --- | --- | --- |
| Contacte | Contact | Contacto |
| Vincula un contacte | Link a contact | Vincula un contacto |
| Opcional · surt a la fitxa de Contactes | Optional · shows up in the Contacts card | Opcional · aparece en la ficha de Contactos |
| VINCULAT A | LINKED TO | VINCULADO A |
| Treu el vincle (accessibilitat) | Remove link | Quitar vínculo |
| Sense permís de Contactes | No Contacts permission | Sin permiso de Contactos |
| Cal per vincular un contacte | Needed to link a contact | Necesario para vincular un contacto |
| Dona permís | Allow | Dar permiso |
| Vinculada a %s | Linked to %s | Vinculada a %s |
| Obre a Contactes | Open in Contacts | Abrir en Contactos |
| El contacte ja no existeix | This contact no longer exists | El contacto ya no existe |
| S'ha esborrat de Contactes. Les notes es queden aquí. | It was deleted from Contacts. Your notes stay here. | Se ha borrado de Contactos. Las notas se quedan aquí. |
| Desvincula | Unlink | Desvincular |
| Vincula persones amb Contactes | Link people to Contacts | Vincula personas con Contactos |
| Per vincular, Converses necessita veure els teus contactes. Així les teves notes surten a la fitxa del contacte. | To link, Converses needs to see your contacts. That way your notes show up on the contact's card. | Para vincular, Converses necesita ver tus contactos. Así tus notas aparecen en la ficha del contacto. |
| Apareixerà un compte «Converses» | A “Converses” account will appear | Aparecerá una cuenta «Converses» |
| Es veurà a Ajustos › Comptes. És normal: l'Android el necessita per mostrar «Veure notes» i «Crear nota» dins del contacte. | You'll see it in Settings › Accounts. That's normal: Android needs it to show “View notes” and “Create note” inside the contact. | Se verá en Ajustes › Cuentas. Es normal: Android lo necesita para mostrar «Ver notas» y «Crear nota» dentro del contacto. |
| No es copia res a Contactes | Nothing is copied to Contacts | No se copia nada a Contactos |
| Només s'hi afegeixen aquests dos accessos. Les notes es queden a Converses. | Only those two shortcuts are added. Your notes stay in Converses. | Solo se añaden esos dos accesos. Las notas se quedan en Converses. |
| Si no ho permets | If you don't allow it | Si no lo permites |
| Converses funciona igual. Només no podràs vincular contactes ni veure l'accés a Contactes al detall. | Converses works the same. You just can't link contacts or see the Contacts shortcut on a person. | Converses funciona igual. Solo no podrás vincular contactos ni ver el acceso a Contactos en el detalle. |
| Continua | Continue | Continuar |
| Ara no | Not now | Ahora no |
| Contactes (secció d'Ajustos) | Contacts | Contactos |
| Vincle amb Contactes | Contacts link | Vínculo con Contactos |
| Mostra les teves notes dins de la fitxa de cada contacte | Show your notes inside each contact's card | Muestra tus notas dentro de la ficha de cada contacto |
| Encara no l'has activat. Et demanarem permís per veure els contactes. | Not turned on yet. We'll ask for permission to see your contacts. | Aún no lo has activado. Te pediremos permiso para ver tus contactos. |
| Activa | Turn on | Activar |
| Activat · %d persones vinculades | On · %d people linked | Activado · %d personas vinculadas |
| Les persones vinculades no mostren l'accés a Contactes. Les notes no es perden. | Linked people won't show the Contacts shortcut. No notes are lost. | Las personas vinculadas no muestran el acceso a Contactos. No se pierden notas. |
| Les notes no es copien a Contactes: només s'hi afegeixen «Veure notes» i «Crear nota». | Notes aren't copied to Contacts: only “View notes” and “Create note” are added. | Las notas no se copian a Contactos: solo se añaden «Ver notas» y «Crear nota». |

Les files a Contactes («Veure notes», «Crear nota») no surten a la taula: són del `contacts.xml` i també es tradueixen (en: «View notes», «Create note»; es: «Ver notas», «Crear nota»).

## Confirmat amb el codi (25 d'oct.)

- El compte «Converses» es veu a Ajustos › Comptes (provat a la 37.1).
- Només s'hi afegeixen les dues files; el raw contact no porta nom, notes ni telèfons.
- Persona protegida bloquejada: no es veu res del vincle.
