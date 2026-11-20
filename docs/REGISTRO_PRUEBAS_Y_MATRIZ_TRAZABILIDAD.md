# Registro de pruebas y matriz de trazabilidad

**Proyecto:** Aceros We-Corp

**Revisión:** 19-11-2026

**Alcance:** autenticación, persistencia, vistas móviles, Firebase Storage e integración del dispositivo
**Estado:** build Android y pruebas de reglas locales aprobados; quedan pendientes las pruebas manuales y la publicación de reglas en Firebase.

## Ejecución reproducible

Las reglas se prueban contra el emulador local, con perfiles y datos ficticios. No se conecta al proyecto Firebase de producción.
Se necesita Node.js 20 o superior y Firebase CLI 15.x (`npm install -g firebase-tools`).

```powershell
npm ci
npm run test:rules
npm run test:storage
```

Para validar los recursos Android se requiere el `google-services.json` del equipo, excluido del repositorio:

```powershell
.\gradlew.bat :app:assembleDebug
```

No basta con añadir `firestore.rules` al repositorio para cambiar la configuración de Firebase. Una persona con acceso al proyecto debe publicar las reglas antes de que venza el modo de prueba y guardar la evidencia de esa publicación.

## Registro de ejecución

| Ejecución | Fecha | Entorno | Resultado | Evidencia |
| --- | --- | --- | --- | --- |
| RUN-FW-01 | 19-11-2026 | Firestore Emulator; Node 24.19.0; Firebase CLI 15.36.0; Firebase JS 13.4.0 | Aprobado: 10/10 casos | Salida de `npm run test:rules` en esta rama |
| RUN-ST-01 | 19-11-2026 | Firestore y Firebase Storage Emulators; Firebase CLI 15.36.0 | Aprobado: 4/4 casos | Salida de `npm run test:storage` en esta rama |
| RUN-ANDROID-01 | 19-11-2026 | Android SDK local; Java 25.0.3; `google-services.json` local | Build debug y pruebas unitarias aprobados | Salida de `:app:assembleDebug` y `:app:testDebugUnitTest` |
| RUN-UI-01 | 19-11-2026 | Emulador Pixel 6a; Android 17 | Aprobado: pantalla de login muestra las acciones principales | Salida de `:app:connectedDebugAndroidTest` |
| RUN-APP-01 | Pendiente | Emulador/dispositivo Android con cuenta Firebase de prueba | Pendiente; requiere cuenta de prueba y reglas publicadas | Capturas y/o video de los casos APP |
| RUN-FIREBASE-01 | Pendiente | Proyecto AcerosWecorp | Pendiente; publicar reglas y comprobar consola | Captura de Firestore > Reglas |

Al completar una ejecución, registrar fecha, versión/entorno, resultado por caso y adjuntar la evidencia en la entrega. No marcar como aprobados los casos de aplicación con datos de ejemplo estáticos.

## Matriz de trazabilidad

| Requisito | Criterio verificable | Casos | Estado actual |
| --- | --- | --- | --- |
| AUTH-01 | Registro crea cuenta y perfil propio con rol `vendedor`; el rol no se elige desde la app. | APP-01, FW-02 | Código existente; prueba manual pendiente; regla automatizada |
| AUTH-02 | Login lleva al menú correspondiente al rol guardado; sesión persiste y se puede cerrar. | APP-02 a APP-04 | Prueba manual pendiente |
| UI-01 | Pantallas distintas de login/registro usan el encabezado, logo, franja, campos y tarjetas del estilo de autenticación. | UI-01 | Implementado; comparación con el canvas y capturas pendientes |
| DB-01 | Un vendedor solo lee su perfil; no puede crear otro rol ni modificar/eliminar su perfil. Administrador puede consultar y actualizar perfiles. | FW-01 a FW-04 | Reglas automatizadas; falta publicar |
| DB-02 | Vendedor y administrador leen productos; solo administrador crea, actualiza o elimina productos. | FW-05 | CRUD implementado; reglas automatizadas; falta publicar |
| DB-03 | Vendedor consulta y crea cotizaciones propias; no puede consultar las de otros ni cambiar el `vendedorUid`. Administrador accede a todas y puede eliminarlas. | FW-06 a FW-09 | CRUD implementado respetando reglas; reglas automatizadas; falta publicar |
| DB-04 | Sin autenticación no se leen perfiles, productos ni cotizaciones protegidos. | FW-10 | Reglas automatizadas; falta publicar |
| H3-DATA | CRUD de productos/cotizaciones, modelos y consultas dinámicas de Firestore. | APP-05 | Implementado; prueba manual con cuenta Firebase pendiente |
| H4-CAM | Captura de cámara y almacenamiento en Firebase Storage. | APP-06 | Implementado; prueba manual y habilitación del bucket pendientes |
| H4-GEO | Permisos y estados de geolocalización (aceptado, denegado y ubicación desactivada). | APP-07 | Implementado; prueba manual pendiente |
| H4-REST | Consulta REST del tipo de cambio sin bloquear el hilo principal. | APP-08 | Implementado; prueba manual pendiente |
| H4-INTENT | Apertura de URI HTTPS con `ACTION_VIEW`. | APP-09 | Implementado; prueba manual pendiente |
| H5-DEPLOY | Publicación de reglas de Firestore en AcerosWecorp antes de vencer el modo de prueba. | FIREBASE-01 | Pendiente de acceso/publicación |
| H5-STORAGE | Reglas de Firebase Storage por UID y rol, para fotos de usuario y acceso administrador. | FIREBASE-02 | Reglas implementadas y 4/4 pruebas aprobadas en emulador; falta publicar |

## Casos manuales de aplicación

| ID | Preparación y pasos | Resultado esperado | Estado / evidencia |
| --- | --- | --- | --- |
| APP-01 | Registrar una cuenta nueva con datos válidos. Revisar Authentication y `perfiles/{uid}`. | Usuario autenticado; perfil con el mismo UID, rol `vendedor` y `activo: true`; abre Menú Ventas. | Pendiente |
| APP-02 | Iniciar sesión con una cuenta vendedora y otra administradora. | Cada cuenta abre el menú que corresponde al campo `rol` de su perfil. | Pendiente |
| APP-03 | Iniciar sesión con contraseña incorrecta y probar correo inválido. | Se muestra el error esperado y no se abre ningún menú. | Pendiente |
| APP-04 | Cerrar sesión y volver a abrir la app con una sesión válida. | Cierre vuelve al login y limpia el historial; sesión activa abre el menú al iniciar. | Pendiente |
| UI-01 | Abrir cada pantalla de la app y comparar con el canvas de diseño del equipo. | No hay pantallas fuera de la guía visual; navegación y controles siguen operativos. | Pendiente; guardar capturas |
| APP-05 | Probar consulta y escritura de productos/cotizaciones desde la app cuando estén implementadas. | Listados y cambios persisten; vendedor solo ve sus cotizaciones. | Pendiente Hito 3 |
| APP-06 | Tomar una foto desde la app y revisar el archivo en Storage. | Permiso aceptado permite guardar; denegado muestra alternativa/error claro. | Pendiente Hito 4 |
| APP-07 | Probar ubicación con permiso aceptado, denegado y servicio sin ubicación. | Cada estado se maneja sin bloqueo ni cierre inesperado. | Pendiente Hito 4 |
| APP-08 | Consultar `rates.CLP` en línea y sin conexión. | Se muestra el valor recibido; la interfaz no se bloquea y el fallo se informa. | Pendiente Hito 4 |
| APP-09 | Abrir el enlace HTTPS configurado. | Android ofrece o abre una aplicación compatible mediante `ACTION_VIEW`. | Pendiente Hito 4 |

## Casos automatizados de reglas

Los casos `FW-01` a `FW-10` se encuentran en `tests/firestore.rules.test.cjs`. Los casos `ST-01` a `ST-03` se encuentran en `tests/storage.rules.test.cjs`. Ambos grupos verifican accesos permitidos y denegados usando emuladores locales; no publican reglas en el proyecto real.

| ID | Permitido | Denegado |
| --- | --- | --- |
| FW-01 | Vendedor lee su perfil | Lee perfil de otro vendedor |
| FW-02 | Vendedor crea su propio perfil con rol `vendedor` | Crea perfil propio con rol `administrador` |
| FW-03 | — | Vendedor cambia su rol o elimina su perfil |
| FW-04 | Administrador lee y actualiza perfiles | — |
| FW-05 | Vendedor/administrador leen productos; administrador crea, actualiza y elimina | Vendedor modifica productos |
| FW-06 | Vendedor lee su cotización | Lee cotización ajena o lista la colección sin filtro de propietario |
| FW-07 | Vendedor crea cotización con su UID | Crea cotización atribuyéndola a otro vendedor |
| FW-08 | Vendedor actualiza su cotización conservando `vendedorUid` | Cambia `vendedorUid` |
| FW-09 | Administrador consulta, crea, actualiza y elimina cotizaciones | — |
| FW-10 | — | Usuario anónimo lee perfiles, productos o cotizaciones |

| ID | Permitido | Denegado |
| --- | --- | --- |
| ST-01 | Usuario autenticado sube, lee y elimina sus propias fotos JPEG | — |
| ST-02 | — | Vendedor accede a la ruta de fotos de otro usuario |
| ST-03 | — | Usuario anónimo sube fotos; usuario autenticado sube contenido que no es imagen |
| ST-04 | Administrador activo consulta fotos de vendedores | — |
