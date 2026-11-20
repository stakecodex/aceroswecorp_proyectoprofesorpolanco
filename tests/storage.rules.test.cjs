const { readFileSync } = require("node:fs");
const { after, before, test } = require("node:test");
const { assertFails, assertSucceeds, initializeTestEnvironment } = require("@firebase/rules-unit-testing");
const { deleteObject, getBytes, ref, uploadBytes } = require("firebase/storage");
const { doc, setDoc } = require("firebase/firestore");

const projectId = "demo-aceroswecorp";
const ownerUid = "seller-1";
const otherSellerUid = "seller-2";
const adminUid = "admin-1";
let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId,
    firestore: {
      rules: readFileSync("firestore.rules", "utf8"),
    },
    storage: {
      rules: readFileSync("storage.rules", "utf8"),
    },
  });
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await Promise.all([
      setDoc(doc(db, "perfiles", ownerUid), {
        uid: ownerUid,
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
    ]);
  });
});

after(async () => {
  await testEnv.cleanup();
});

test("ST-01 vendedor solo sube, lee y elimina sus fotos", async () => {
  const storage = testEnv.authenticatedContext(ownerUid).storage();
  const ownPhoto = ref(storage, `usuarios/${ownerUid}/fotos/foto.jpg`);
  const image = new Uint8Array([0xff, 0xd8, 0xff, 0xd9]);

  await assertSucceeds(uploadBytes(ownPhoto, image, { contentType: "image/jpeg" }));
  await assertSucceeds(getBytes(ownPhoto));
  await assertSucceeds(deleteObject(ownPhoto));
});

test("ST-02 usuario no puede acceder a la ruta de otro vendedor", async () => {
  const storage = testEnv.authenticatedContext(ownerUid).storage();
  const otherPhoto = ref(storage, "usuarios/seller-2/fotos/foto.jpg");

  await assertFails(uploadBytes(otherPhoto, new Uint8Array([1]), { contentType: "image/jpeg" }));
  await assertFails(getBytes(otherPhoto));
});

test("ST-03 acceso anónimo y tipos que no son imágenes son denegados", async () => {
  const anonymousPhoto = ref(
    testEnv.unauthenticatedContext().storage(),
    `usuarios/${ownerUid}/fotos/anonima.jpg`,
  );
  const ownStorage = testEnv.authenticatedContext(ownerUid).storage();
  const nonImage = ref(ownStorage, `usuarios/${ownerUid}/fotos/documento.txt`);

  await assertFails(uploadBytes(anonymousPhoto, new Uint8Array([1]), { contentType: "image/jpeg" }));
  await assertFails(uploadBytes(nonImage, new Uint8Array([1]), { contentType: "text/plain" }));
});

test("ST-04 administrador activo puede consultar fotos de vendedores", async () => {
  const image = new Uint8Array([0xff, 0xd8, 0xff, 0xd9]);
  const storage = testEnv.authenticatedContext(ownerUid).storage();
  const ownPhoto = ref(storage, `usuarios/${ownerUid}/fotos/foto-admin.jpg`);
  await assertSucceeds(uploadBytes(ownPhoto, image, { contentType: "image/jpeg" }));

  const adminStorage = testEnv.authenticatedContext(adminUid).storage();
  await assertSucceeds(getBytes(ref(adminStorage, `usuarios/${ownerUid}/fotos/foto-admin.jpg`)));
});
