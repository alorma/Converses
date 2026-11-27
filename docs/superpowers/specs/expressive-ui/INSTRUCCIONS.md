# Converses · Prova de UI «Expressive» (27 de novembre de 2026)

Això és una **prova**: una direcció visual nova per a les pantalles principals, que conviu amb la UI actual darrere d'un flag. No substitueix res fins que decidim.

La referència visual és `pantalles.html`, amb els taulers 14a–14f del canvas. Obre-la al navegador amb connexió, perquè la tipografia i les icones es carreguen de Google Fonts. Les mides són en dp.

---

## 1. Abast i flag

- **Feature flag:** `expressive_ui`, desactivat per defecte.
  - Es pot activar des del menú de debug.
  - Afegeix també una fila oculta a Ajustos, «UI experimental», només a les builds `debug` i `internal`.
- **Pantalles de la prova:**

  | Tauler | Pantalla |
  | --- | --- |
  | 14a, 14e | Llistat de persones (clar i fosc) |
  | 14b | Detall de la persona |
  | 14c | Entrada (pantalla nova) |
  | 14d | Nota ràpida |

- **La resta de pantalles es queda igual:** Ajustos, Pro, sincronització, formularis, onboarding…
- **Sense canvis de dades:** cap ViewModel, cap use case i cap canvi de base de dades. Només capa de UI. Si una pantalla necessita una dada nova (per exemple, la propera tasca de 14a), surt d'un use case que ja existeix.
- **Organització:** fes la UI nova en un paquet a part (`ui.expressive`) i tria les pantalles al `NavHost` segons el flag. Les dues versions consumeixen el mateix `UiState`.

## 2. Tema

- Fes-lo amb `MaterialExpressiveTheme` i `MotionScheme.expressive()`.
- **Fons de l'app:** crema `#FBF8F1` (fosc `#14130F`). **Tinta:** `#1C1B17` (fosc `#F1EDE2`). **Text secundari:** `#5A574D` (fosc `#B8B2A3`).
- **Superfície neutra** (xips, botons secundaris): `#EFEBE0` (fosc `#2A2822`).
- **Accent** (recomptes i element actiu de la barra): `#F2E8A6` (fosc `#E4D77A`), amb el text sempre `#1C1B17`.
- El color dinàmic del sistema no s'aplica a aquesta prova. El color el posa cada persona (secció 4).

## 3. Tipografia

La família és **Roboto Flex** (variable), carregada com a font descarregable o inclosa a l'APK, amb `FontVariation`:

| Rol | Eixos | Mida | On |
| --- | --- | --- | --- |
| Display ample | `width(151f)`, `weight(900)`, opsz 144, tracking −0,6 | 32–44 sp | «Converses», «Avui», el nom al detall, «Nota ràpida» |
| Dada destacada | Igual que el display | 20–44 sp | Hores del llistat, «fa 2 h», «Xat», recomptes |
| Cita | `weight(600)`, tracking −0,3 | 22–32 sp | Text de la targeta destacada i de l'entrada |
| Overline | 12 sp, `weight(700)`, majúscules, tracking +1,1 | 12 sp | «JORDI PUIG · XAT», «QUAN», «TASCA · DIVENDRES» |
| Cos | Normal | 15–17 sp | La resta |

Comprova que el display ample no es talla amb noms llargs. Fes servir `maxLines = 2` i `autoSize` (`TextAutoSize.StepBased`) entre 28 i 44 sp.

## 4. Color de cada persona

Cada persona té un dels 6 pastels de l'avatar. A la prova se'n fan servir 4 tons:

| Color | Superfície | Avatar | Text fosc | Fons del detall |
| --- | --- | --- | --- | --- |
| Menta | `#E2F2E8` | `#C2E8D0` | `#245C3F` | `#F1F8F3` |
| Lavanda | `#ECE7F8` | `#D9CFF5` | `#4F3E92` | `#F6F3FC` |
| Préssec | `#FBE7DC` | `#FFD3BA` | `#8A3E22` | `#FDF4EE` |
| Cel | `#E0EDF8` | `#C6DFF5` | `#24557F` | `#F2F7FC` |
| Rosa | `#F9E3EA` | `#F5CADA` | `#8C3456` | `#FCF2F5` |
| Llimona | `#F5F0CF` | `#EEE39C` | `#5E5410` | `#FBF9EA` |

- **Superfície:** les targetes de la persona al llistat, les seves bombolles i les rajoles.
- **Avatar:** el fons de l'avatar.
- **Text fosc:** el nom, les overlines de la persona, la targeta destacada i el botó «Nova entrada».
- **Fons del detall:** el fons de 14b, 14c i, si la nota ràpida porta una persona triada, 14d.
- **Mode fosc (14e):** la superfície passa a un to fosc del mateix color (`#1F3328` en menta, `#2A2540` en lavanda…), la targeta destacada fa servir el color de l'avatar com a fons, i les overlines van en el color de l'avatar.
- Posa aquests valors en una `PersonPalette` (enum o data class) amb les variants clara i fosca. Si «Color de la persona» ja calcula un esquema, pots reaprofitar-lo, però a la prova mana aquesta taula.

## 5. Formes

Fes servir `MaterialShapes` de `material3` amb `toShape()`:

| Forma | On |
| --- | --- |
| `Cookie9Sided` | Botons d'acció: nota ràpida (FAB de 80 dp), «Desa» (96 dp), afegir persona (52 dp), afegir entrada a la targeta destacada (84 dp), «Nova tasca» (64 dp) |
| `Clover4Leaf` | Avatar gran del detall (200 dp) i botó «Jordi» del selector Qui |
| `Flower` | Decoració de la targeta destacada: 250 dp, blanc al 12 %, retallada per la cantonada |
| La forma de l'avatar de cada persona | Avatars del llistat (56 dp). Sense Pro, és `Cookie9Sided` |

**Pressió:** en prémer, els botons amb forma fan `Morph` cap a un cercle, amb l'spring ràpid del `MotionScheme`.

## 6. Pantalles

### 14a · Llistat

**Capçalera**
- «Converses» en display de 34 sp.
- A la dreta, dos botons de 52 dp:
  - afegir persona: `Cookie9Sided` en accent, amb la icona `person_add`;
  - compte i sincronització: cercle neutre amb l'avatar de l'usuari de 36 dp.

**Targeta destacada**
- 268 dp d'alçada i radi 40.
- El fons és el color fosc de la persona amb l'**entrada més recent** de totes.
- Contingut:
  - a dalt, l'avatar de 30 dp i l'overline «NOM · CANAL · FA 20 MIN»;
  - la cita en 27 sp entre cometes «»;
  - la decoració `Flower`.
- **A baix a l'esquerra, només si la persona té alguna tasca pendent (Pro):**
  - l'overline «TASCA · {dia}»;
  - una pastilla de 40 dp (blanc al 16 %) amb `flag` i el text de la tasca més propera;
  - tocar la pastilla obre el detall a la secció Pendents.
  - Sense tasques o sense Pro, aquesta zona queda buida.
- **A baix a la dreta:** galeta blanca de 84 dp amb `add` (contentDescription «Afegeix una entrada a {nom}»), que obre la nota ràpida amb la persona ja triada.
- Tocar la resta de la targeta obre el detall.
- Si la persona és protegida, la targeta mostra la següent persona no protegida. No hi ha cap targeta si no hi ha entrades.

**Xips de filtre**
- 52 dp.
- «Totes» va en tinta amb la icona `apps`.
- Els canals van en neutre, amb la icona dins d'un quadrat blanc de 36 dp i radi 12.
- Fan scroll horitzontal.

**Grups per dates**
- Grups: Avui, Ahir, Aquesta setmana, i després els mesos.
- Capçalera de cada grup: una insígnia de recompte (34 dp, radi 11, accent) i el títol en display de 34 sp.

**Fila de persona**
- Targeta de 92 dp, radi 32, en la superfície de la persona.
- Avatar de 56 dp.
- Overline amb el nom, en el seu color fosc, i a sota l'última línia (16 sp, una línia).
- A la dreta, l'hora o el dia en display de 20 sp.
- **Protegides:** l'icona `lock` i «Protegida» en lloc del text.
- Entre targetes hi ha 6 dp.

**Navegació**
- Barra flotant de 68 dp, radi 34, en tinta: Persones (activa, en accent, amb text), Arxivades i Ajustos.
- A la dreta, el FAB `Cookie9Sided` de 80 dp, en préssec fosc (`#8A3E22`), amb `edit`.

### 14b · Detall de la persona

**Capçalera**
- El fons és el fons de detall de la persona.
- Top bar: enrere, `edit` i `more_vert`.
- Avatar `Clover4Leaf` de 200 dp amb les inicials en display de 64 sp.
- L'overline «@ÀLIES · DES DE {mes}», el nom en display de 44 sp en el color fosc i, a sota, les pastilles de canal (blanques, 36 dp).

**Accions**
- Botó «Nova entrada» de 64 dp, en el color fosc.
- Al costat, una galeta de 64 dp per a «Nova tasca».
- Sense Pro, la galeta no surt i el botó ocupa tota l'amplada.

**Recomptes**
- Tres rajoles blanques agrupades: Jo, {persona} i Apunts.
- El número en display de 30 sp.

**Dades**
- Una línia de text, amb el primer valor en negreta i els altres separats per «·».
- Toca-la per desplegar-la.

**Cronologia en bombolles**
- Agrupada per dies, amb la mateixa capçalera que el llistat.
- **El que diu la persona:** superfície de la persona, alineat a l'esquerra (36 dp de marge a la dreta), radi 32/32/32/10.
- **El que dius tu:** fons de tinta i text clar, alineat a la dreta (36 dp de marge a l'esquerra), radi 32/32/10/32.
- **Apunts:** sense fons, amb una vora de 2 dp en el color de l'avatar, radi 24.
- Cada bombolla porta l'overline «QUI · HORA · CANAL» i el text en 17 sp.
- Tocar una bombolla obre 14c.

### 14c · Entrada (pantalla nova)

Substitueix el full d'accions de l'entrada (2d) quan el flag està actiu.

- **Capçalera:**
  - el fons de detall de la persona i el botó enrere;
  - l'enllaç «NOM ›», que porta al detall de la persona;
  - qui ho va dir: l'avatar de 40 dp i «Ho va dir en {nom}», «Ho vas dir tu» o «Apunt».
- **Text:** 32 sp, `weight(600)`, en el color fosc de la persona.
- **Dues rajoles** amb la superfície de la persona i radi 32, que comparteixen estructura (overline, fila de 48 dp i línia de sota):
  - **QUAN** (3/5 de l'amplada): el temps relatiu en display de 44 sp («fa 2 h») i, a sota, «Avui, a les 19:12».
  - **ON** (2/5 de l'amplada): el grup en display de 44 sp («Xat») i, a sota, el canal propi si n'hi ha («WhatsApp»). Si no n'hi ha, aquesta línia no surt.
- **Graella d'accions** de 236 dp d'alçada, amb 10 dp entre peces:
  - «Edita», un rectangle de radi 44 en tinta, que ocupa dues files;
  - quatre botons rodons en la superfície de la persona: `content_copy` (Copia), `swap_horiz` (Canvia el canal), `flag` (Fes-ne una tasca, només Pro) i `delete` (Esborra, amb la confirmació actual).
  - Sense Pro, la bandera porta el punt PRO i obre el paywall.
  - Tots porten contentDescription.

### 14d · Nota ràpida

1. **Capçalera:** el botó de tancar (cercle neutre de 48 dp) i el títol «Nota ràpida» en display de 38 sp.
2. **Fila «AMB QUI»:** 64 dp, radi 32, en la superfície de la persona, amb l'avatar de 48 dp i `unfold_more`.
3. **Selector Qui:** tres formes de 92 dp.
   - Jo: cercle en tinta amb `person`.
   - La persona: `Clover4Leaf` amb les inicials.
   - Apunt: `Cookie9Sided` en neutre amb `edit_note`.
   - L'opció triada porta l'etiqueta en negreta i una barreta de 24×4 a sota.
   - En canviar d'opció, la forma triada fa morph.
4. **Text:** camp blanc de radi 32 i text de 22 sp, amb el botó d'enganxar de 44 dp.
5. **On:** xips de 44 dp; el triat va en tinta amb `check`.
6. **A baix, fixos:**
   - la píndola «Quan» de 72 dp en neutre;
   - al costat, «Desa», una galeta de 96 dp en el color fosc de la persona, amb `check` i el text.

## 7. Moviment

- Fes servir `MotionScheme.expressive()` a tot arreu.
- La targeta destacada entra amb una molla en escala de 0,96 a 1.
- Les files de la llista es reordenen amb `Modifier.animateItem()`.
- Les formes fan `Morph` en prémer i en seleccionar.
- Respecta l'opció «Elimina animacions» del sistema: sense morph i amb transicions curtes.

## 8. Accessibilitat

- El text fosc de la persona sobre la seva superfície compleix AA, però comprova-ho amb el contrast real de cada parella.
- Totes les icones soles porten contentDescription.
- Els objectius tàctils fan com a mínim 48 dp.
- Amb text gran (200 %), els títols ample han de passar a dues línies, sense tallar-se.

## 9. Com avaluar la prova

- **Previews:** una per pantalla amb `@PreviewLightDark`, i una amb noms llargs i text gran.
- **Comparació:** captures de les dues UI amb les mateixes dades (llistat, detall, entrada i nota).
- **Rendiment:** comprova que el llistat no perd frames amb les formes retallades (`graphicsLayer { clip = true; shape = … }`) i 200 persones.
- **Abans de decidir, queda obert:**
  - Ajustos, Pro i els formularis amb aquest estil;
  - les pantalles grans;
  - si 14c substitueix definitivament el full d'accions.
