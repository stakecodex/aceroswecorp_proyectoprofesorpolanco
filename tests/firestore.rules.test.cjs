const { readFileSync } = require("node:fs");
const { after, before, beforeEach, test } = require("node:test");
const { equal } = require("node:assert/strict");
const {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} = require("@firebase/rules-unit-testing");
const {
  Timestamp,
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  query,
  setDoc,
  updateDoc,
  where,
} = require("firebase/firestore");

const projectId = "demo-aceroswecorp";
const sellerUid = "seller-1";
const otherSellerUid = "seller-2";
const adminUid = "admin-1";
let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId,
    firestore: {
      rules: readFileSync("firestore.rules", "utf8"),
    },
  });
});

beforeEach(async () => {
  await testEnv.clearFirestore();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await Promise.all([
      setDoc(doc(db, "perfiles", sellerUid), {
        uid: sellerUid,
        rol: "vendedor",
        activo: true,
      }),
      setDoc(doc(db, "perfiles", otherSellerUid), {
        uid: otherSellerUid,
        rol: "vendedor",
        activo: true,
      }),
      setDoc(doc(db, "perfiles", adminUid), {
        uid: adminUid,
        rol: "administrador",
        activo: true,
      }),
      setDoc(doc(db, "productos", "varilla-038"), { nombre: "Varilla 3/8" }),
      setDoc(doc(db, "cotizaciones", "quote-1"), {
        vendedorUid: sellerUid,
        total: 2500,
      }),
      setDoc(doc(db, "cotizaciones", "quote-2"), {
        vendedorUid: otherSellerUid,
        total: 1800,
      }),
    ]);
  });
});

after(async () => {
  await testEnv.cleanup();
});

function sellerDb(uid = sellerUid) {
  return testEnv.authenticatedContext(uid).firestore();
}

function adminDb() {
  return testEnv.authenticatedContext(adminUid).firestore();
}

function newSellerProfile(uid) {
  return {
    uid,
    nombre: "Vendedor de prueba",
    correo: `${uid}@example.test`,
    rol: "vendedor",
    activo: true,
    creadoEn: Timestamp.fromMillis(0),
  };
}

test("FW-01 vendedor lee su perfil, pero no el perfil de otro vendedor", async () => {
  const db = sellerDb();

  await assertSucceeds(getDoc(doc(db, "perfiles", sellerUid)));
  await assertFails(getDoc(doc(db, "perfiles", otherSellerUid)));
});

test("FW-02 registro solo permite crear el perfil propio con rol vendedor", async () => {
  const db = sellerDb("seller-new");

  await assertSucceeds(setDoc(
    doc(db, "perfiles", "seller-new"),
    newSellerProfile("seller-new"),
  ));
  await assertFails(setDoc(
    doc(sellerDb("seller-escalated"), "perfiles", "seller-escalated"),
    { ...newSellerProfile("seller-escalated"), rol: "administrador" },
  ));
});

test("FW-03 vendedor no puede cambiar su rol ni eliminar su perfil", async () => {
  const db = sellerDb();

  await assertFails(updateDoc(doc(db, "perfiles", sellerUid), { rol: "administrador" }));
  await assertFails(deleteDoc(doc(db, "perfiles", sellerUid)));
});

test("FW-04 administrador puede leer y actualizar perfiles", async () => {
  const db = adminDb();

  await assertSucceeds(getDoc(doc(db, "perfiles", otherSellerUid)));
  await assertSucceeds(updateDoc(doc(db, "perfiles", otherSellerUid), { activo: false }));
});

test("FW-05 vendedor y administrador pueden leer productos; solo administrador los modifica", async () => {
  const seller = sellerDb();
  const admin = adminDb();

  await assertSucceeds(getDoc(doc(seller, "productos", "varilla-038")));
  await assertFails(setDoc(doc(seller, "productos", "nuevo"), { nombre: "Perfil" }));
  await assertFails(updateDoc(doc(seller, "productos", "varilla-038"), { nombre: "Cambiar" }));
  await assertFails(deleteDoc(doc(seller, "productos", "varilla-038")));
  await assertSucceeds(getDoc(doc(admin, "productos", "varilla-038")));
  await assertSucceeds(setDoc(doc(admin, "productos", "nuevo"), { nombre: "Perfil" }));
  await assertSucceeds(updateDoc(doc(admin, "productos", "nuevo"), { nombre: "Perfil nuevo" }));
  await assertSucceeds(deleteDoc(doc(admin, "productos", "nuevo")));
});

test("FW-06 vendedor solo lee cotizaciones propias y no puede consultar toda la colección", async () => {
  const db = sellerDb();

  await assertSucceeds(getDoc(doc(db, "cotizaciones", "quote-1")));
  await assertFails(getDoc(doc(db, "cotizaciones", "quote-2")));
  await assertFails(getDocs(collection(db, "cotizaciones")));
  const ownQuotes = await assertSucceeds(getDocs(query(
    collection(db, "cotizaciones"),
    where("vendedorUid", "==", sellerUid),
  )));
  equal(ownQuotes.size, 1);
});

test("FW-07 vendedor crea cotizaciones propias y no puede suplantar otro vendedor", async () => {
  const db = sellerDb();

  await assertSucceeds(setDoc(doc(db, "cotizaciones", "quote-new"), {
    vendedorUid: sellerUid,
    total: 3200,
  }));
  await assertFails(setDoc(doc(db, "cotizaciones", "quote-forged"), {
    vendedorUid: otherSellerUid,
    total: 3200,
  }));
});

test("FW-08 vendedor actualiza su cotización sin cambiar vendedorUid", async () => {
  const db = sellerDb();

  await assertSucceeds(updateDoc(doc(db, "cotizaciones", "quote-1"), { total: 2600 }));
  await assertFails(updateDoc(doc(db, "cotizaciones", "quote-1"), {
    vendedorUid: otherSellerUid,
  }));
});

test("FW-09 administrador consulta y elimina cotizaciones de cualquier vendedor", async () => {
  const db = adminDb();

  await assertSucceeds(getDoc(doc(db, "cotizaciones", "quote-2")));
  await assertSucceeds(getDocs(collection(db, "cotizaciones")));
  await assertSucceeds(setDoc(doc(db, "cotizaciones", "quote-new"), {
    vendedorUid: otherSellerUid,
    total: 2200,
  }));
  await assertSucceeds(updateDoc(doc(db, "cotizaciones", "quote-2"), { total: 1900 }));
  await assertSucceeds(deleteDoc(doc(db, "cotizaciones", "quote-2")));
});

test("FW-10 usuario sin autenticar no lee datos protegidos", async () => {
  const db = testEnv.unauthenticatedContext().firestore();

  await assertFails(getDoc(doc(db, "perfiles", sellerUid)));
  await assertFails(getDoc(doc(db, "productos", "varilla-038")));
  await assertFails(getDoc(doc(db, "cotizaciones", "quote-1")));
});
