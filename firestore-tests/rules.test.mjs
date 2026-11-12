import { readFileSync } from 'node:fs'
import { assertFails, assertSucceeds, initializeTestEnvironment } from '@firebase/rules-unit-testing'
import {
  Bytes,
  deleteField,
  doc,
  getDoc,
  serverTimestamp,
  setDoc,
  Timestamp,
  updateDoc,
  writeBatch,
} from 'firebase/firestore'

const OWNER = 'owner'
const OTHER = 'other'
const DEVICE = 'device-1'

let env

const db = (uid) => env.authenticatedContext(uid).firestore()
const account = (extra = {}) => ({
  wrappedKey: Bytes.fromUint8Array(new Uint8Array(60)),
  format: 1,
  createdAt: serverTimestamp(),
  ...extra,
})
const record = (extra = {}) => ({
  c: Bytes.fromUint8Array(new Uint8Array(100)),
  deleted: false,
  by: DEVICE,
  updatedAt: serverTimestamp(),
  ...extra,
})
const device = () => ({
  name: Bytes.fromUint8Array(new Uint8Array(30)),
  addedAt: serverTimestamp(),
  lastSyncAt: serverTimestamp(),
})
const recordRef = (firestore, id = 'r1', uid = OWNER) => doc(firestore, `users/${uid}/records/${id}`)
const accountRef = (firestore, uid = OWNER) => doc(firestore, `users/${uid}`)

// Un compte amb un dispositiu apuntat, escrit sense passar per les regles
const seedAccount = () =>
  env.withSecurityRulesDisabled(async (context) => {
    await setDoc(accountRef(context.firestore()), account({ plan: 'free', devices: { [DEVICE]: device() } }))
  })

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-converses',
    firestore: { rules: readFileSync('../firestore.rules', 'utf8') },
  })
})

after(async () => {
  await env.cleanup()
})

beforeEach(async () => {
  await env.clearFirestore()
})

describe('compte', () => {
  it('el propietari el crea i el llegeix', async () => {
    const firestore = db(OWNER)
    await assertSucceeds(setDoc(accountRef(firestore), account()))
    await assertSucceeds(getDoc(accountRef(firestore)))
  })

  it('un altre usuari ni el llegeix ni l\'escriu', async () => {
    await seedAccount()
    const firestore = db(OTHER)
    await assertFails(getDoc(accountRef(firestore)))
    await assertFails(setDoc(accountRef(firestore), account()))
    await assertFails(updateDoc(accountRef(firestore), { plan: 'pro' }))
  })

  it('sense sessió no hi ha accés', async () => {
    await seedAccount()
    const firestore = env.unauthenticatedContext().firestore()
    await assertFails(getDoc(accountRef(firestore)))
  })

  it('rebutja un camp que no és de la llista', async () => {
    await assertFails(setDoc(accountRef(db(OWNER)), account({ email: 'a@b.c' })))
  })

  it('rebutja un pla que no és free ni pro', async () => {
    await assertFails(setDoc(accountRef(db(OWNER)), account({ plan: 'gold' })))
    await assertSucceeds(setDoc(accountRef(db(OWNER)), account({ plan: 'pro' })))
  })

  it('el propietari pot actualitzar devices, plan i lastSyncAt', async () => {
    await seedAccount()
    const firestore = db(OWNER)
    await assertSucceeds(
      updateDoc(accountRef(firestore), {
        'devices.device-2': device(),
        plan: 'pro',
        lastSyncAt: serverTimestamp(),
      }),
    )
    await assertSucceeds(updateDoc(accountRef(firestore), { 'devices.device-1.lastSyncAt': serverTimestamp() }))
    // Treure un dispositiu (la fase 5 ho farà des d'un altre)
    await assertSucceeds(updateDoc(accountRef(firestore), { 'devices.device-2': deleteField() }))
  })

  it('rebutja afegir un camp nou en actualitzar', async () => {
    await seedAccount()
    await assertFails(updateDoc(accountRef(db(OWNER)), { extra: 1 }))
  })
})

describe('registres', () => {
  beforeEach(seedAccount)

  it('el propietari en crea, en llegeix i en actualitza', async () => {
    const firestore = db(OWNER)
    await assertSucceeds(setDoc(recordRef(firestore), record()))
    await assertSucceeds(getDoc(recordRef(firestore)))
    await assertSucceeds(setDoc(recordRef(firestore), record({ deleted: true })))
  })

  it('un lot de registres s\'accepta', async () => {
    const firestore = db(OWNER)
    const batch = writeBatch(firestore)
    for (let i = 0; i < 10; i++) batch.set(recordRef(firestore, `b${i}`), record())
    await assertSucceeds(batch.commit())
  })

  it('un altre usuari ni els llegeix ni els escriu', async () => {
    await env.withSecurityRulesDisabled(async (context) => {
      await setDoc(recordRef(context.firestore()), { ...record(), updatedAt: Timestamp.now() })
    })
    const firestore = db(OTHER)
    await assertFails(getDoc(recordRef(firestore)))
    await assertFails(setDoc(recordRef(firestore), record()))
    await assertFails(setDoc(recordRef(firestore, 'r2', OWNER), record()))
  })

  it('rebutja un camp de més', async () => {
    await assertFails(setDoc(recordRef(db(OWNER)), record({ name: 'Anna' })))
  })

  for (const missing of ['c', 'deleted', 'by', 'updatedAt']) {
    it(`rebutja un registre sense ${missing}`, async () => {
      const data = record()
      delete data[missing]
      await assertFails(setDoc(recordRef(db(OWNER)), data))
    })
  }

  it('rebutja tipus incorrectes', async () => {
    const firestore = db(OWNER)
    await assertFails(setDoc(recordRef(firestore), record({ c: 'text' })))
    await assertFails(setDoc(recordRef(firestore), record({ deleted: 'no' })))
    await assertFails(setDoc(recordRef(firestore), record({ by: 7 })))
  })

  it('c: 64 KB hi passa, un byte més no', async () => {
    const firestore = db(OWNER)
    await assertSucceeds(setDoc(recordRef(firestore), record({ c: Bytes.fromUint8Array(new Uint8Array(65536)) })))
    await assertFails(setDoc(recordRef(firestore), record({ c: Bytes.fromUint8Array(new Uint8Array(65537)) })))
  })

  it('rebutja un by que no és a devices', async () => {
    await assertFails(setDoc(recordRef(db(OWNER)), record({ by: 'device-9' })))
  })

  it('rebutja un by si el dispositiu s\'ha tret', async () => {
    const firestore = db(OWNER)
    await updateDoc(accountRef(firestore), { 'devices.device-1': deleteField() })
    await assertFails(setDoc(recordRef(firestore), record()))
  })

  it('rebutja un by si el compte encara no té devices', async () => {
    await env.clearFirestore()
    await env.withSecurityRulesDisabled(async (context) => {
      await setDoc(accountRef(context.firestore()), account())
    })
    await assertFails(setDoc(recordRef(db(OWNER)), record()))
  })

  it('rebutja un updatedAt que no és l\'hora del servidor', async () => {
    const firestore = db(OWNER)
    await assertFails(setDoc(recordRef(firestore), record({ updatedAt: Timestamp.fromMillis(1000) })))
    await assertFails(setDoc(recordRef(firestore), record({ updatedAt: Timestamp.now() })))
  })

  it('un lot amb un sol registre dolent falla sencer', async () => {
    const firestore = db(OWNER)
    const batch = writeBatch(firestore)
    batch.set(recordRef(firestore, 'ok'), record())
    batch.set(recordRef(firestore, 'bad'), record({ by: 'device-9' }))
    await assertFails(batch.commit())
  })

  // Quants registres admet un lot: el límit de crides d'accés de les regles (20 per lot) hi pot dir la seva
  it('un lot de 200 registres', async () => {
    const firestore = db(OWNER)
    const batch = writeBatch(firestore)
    for (let i = 0; i < 200; i++) batch.set(recordRef(firestore, `big${i}`), record())
    await assertSucceeds(batch.commit())
  })
})
