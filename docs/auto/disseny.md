# Converses per a Android Auto — Què cal dissenyar

Llista per demanar a Claude Design (A7 de `docs/auto/README.md`). Les pantalles es pinten amb plantilles de la Car App Library: el disseny no defineix píxels, colors ni tipografia (els posa Android Auto), sinó **contingut, ordre, textos, icones i estats**. No s'ha de demanar res fins que la fase 0 confirmi que Play admet l'app (A1).

## Què NO es dissenya

- Tema, color de persona, Pro, Roboto Flex: no s'apliquen a Auto.
- Persones protegides: no hi apareixen (A2), per tant no hi ha cadenat ni aprovació.
- Teclat ni cap entrada de text: tot és per veu o per tocs grans.

## Pantalles

| # | Pantalla | Què cal definir |
| --- | --- | --- |
| 1 | Persones | Títol, què hi ha a cada fila (nom, hora de l'última entrada, avatar o només una icona), ordre, què passa amb moltes persones (límit de files de la plantilla), acció de dictar directa des de la fila o només entrar |
| 2 | Últimes entrades | Quantes (≈ 5), com es mostra qui/hora/canal, longitud màxima del text, acció «Nota» sempre visible |
| 3 | Dictat | Estat d'escolta, text parcial o no, acció «Acaba» i «Cancel·la», com es mostra que el micròfon és actiu |
| 4 | Qui | Jo / {persona} / Apunt: tres opcions grans, quina és la per defecte, «Desa» |
| 5 | Desat | Confirmació breu, si es tanca sola, a on torna |
| 6 | Bloqueig de l'app actiu | Missatge «Desbloqueja al mòbil» (A3), sense dades |
| 7 | Mòbil bloquejat o sense sessió | Missatge i acció (A6) |

## Estats i errors

| Estat | Què cal definir |
| --- | --- |
| Sense persones | Text i si hi ha cap acció |
| Persona sense entrades | Només l'acció «Nota» |
| Dictat sense veu | «No s'ha sentit res» i «Reintenta» |
| Micròfon sense permís | Text i com es demana (al cotxe no es pot concedir: dir que cal fer-ho al mòbil) |
| Sense reconeixedor o error genèric | Text i «Reintenta» |
| Persona esborrada o feta protegida durant l'ús | Es tanca sense mostrar res; cal el text mínim |
| Error en desar | Text i si la nota es conserva per reintentar |

## Textos nous (en/ca/es)

Reutilitzar els del mòbil (Jo, Apunt, Desa, grups de canal, `whoLabel(...)`). Per redactar: títols de pantalla, «Nota», «Acaba», «Cancel·la», «Reintenta», «Desat», el missatge de bloqueig (A3), el de micròfon sense permís i el d'error en desar. Tot breu: es llegeix de reüll.

## Icones i identitat

- Icona de l'app per a la llista d'Android Auto (adaptativa, monocroma si cal).
- Avatar: la plantilla permet una imatge per fila; decidir si es fa servir la Galeta amb el color de la persona o només una icona neutra.
- Icones d'acció: micròfon, desa, tanca, torna.

## Restriccions que el disseny ha de respectar

- Màxim d'elements per llista i de nivells de navegació amb el cotxe en marxa (cal tancar-ho a la fase 0).
- Text curt i sense desplaçaments llargs; accions amb poc toc.
- Sense animacions pròpies ni contingut que s'actualitzi sol.

## Què es dona al disseny

README i fases d'aquest pla, la llista d'abast, les restriccions de plantilles (un cop investigades a la fase 0) i captures de la DHU amb una plantilla de prova.

## Sortida esperada

Una captura per pantalla i estat de la taula, en en/ca/es si hi ha text diferent, amb les icones i els textos finals; sense especificar colors.
