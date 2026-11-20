# Estado del proyecto y pendientes de entrega

**Revisión:** 19-11-2026  
**Rama:** `hito3-layouts-firestore-tests`  
**Pull request:** [#2 — Diseño móvil y reglas de Firestore con pruebas](https://github.com/stakecodex/aceroswecorp_proyectoprofesorpolanco/pull/2)  
**Último commit:** `7ad5415` — `Completa persistencia y funciones de Hito 4`

Este documento resume lo implementado y lo que aún requiere publicación, comprobación manual o evidencia. La matriz detallada está en [REGISTRO_PRUEBAS_Y_MATRIZ_TRAZABILIDAD.md](./REGISTRO_PRUEBAS_Y_MATRIZ_TRAZABILIDAD.md).

## Hecho

### Diseño y pantallas

- Se aplicó el estilo compartido a las pantallas pendientes: encabezado, logo, franja y tarjetas.
- Inventario y cotizaciones usan listas dinámicas con adaptadores; se retiraron las tarjetas de datos estáticos de esas vistas.
- Los menús muestran el nombre y el rol leídos de `perfiles`.

### Persistencia y permisos

- Se agregaron los modelos `PerfilUsuario`, `Producto`, `ItemCotizacion` y `Cotizacion`.
- Productos: consulta para usuarios autorizados; crear, editar y eliminar desde el inventario de administrador.
- Cotizaciones: el vendedor crea, consulta y actualiza las propias; el administrador consulta y puede eliminarlas. El vendedor no elimina cotizaciones, de acuerdo con las reglas actuales.
- Se conservan las reglas de Firestore por UID y rol y sus **10 pruebas** con emulador.
- Se añadieron reglas de Storage para `usuarios/{uid}/fotos/{fileName}`. El propietario administra sus fotos y el administrador activo puede consultarlas. Sus **4 pruebas** con emuladores están aprobadas.

### Integración Android

- Cámara con Activity Result API y carga de fotos a Firebase Storage; no se guardan imágenes base64 en Firestore.
- Solicitud de ubicación con manejo de permisos, servicio desactivado y ausencia de ubicación reciente.
- Consulta de `rates.CLP` en un hilo de trabajo, diálogo con tres acciones y apertura HTTPS con `ACTION_VIEW`.
- Se generó el APK de depuración: `app/build/outputs/apk/debug/app-debug.apk`.

### Validación automatizada

- `npm run test:rules`: **10/10** aprobadas.
- `npm run test:storage`: **4/4** aprobadas.
- `:app:assembleDebug`, `:app:testDebugUnitTest`, `:app:connectedDebugAndroidTest` y `:app:lintDebug`: completados correctamente.
- La prueba instrumentada comprueba que la pantalla de login muestra las acciones principales.
- `app/google-services.json` y `local.properties` son configuraciones locales ignoradas por Git; no se incluyeron en el commit.

## Pendiente

### 1. Publicar las reglas de Firestore — pendiente a propósito

Las reglas **no están publicadas** en el proyecto `aceroswecorp`; el modo de prueba puede seguir activo. La Firebase CLI requiere autenticación. Antes de publicar:

1. Iniciar sesión con `firebase login`.
2. Confirmar que el proyecto seleccionado es `aceroswecorp`.
3. Publicar `firestore.rules` y comprobar el resultado en Firebase Console.
4. Guardar una captura de Firestore > Reglas como evidencia.

La publicación se debe completar antes del vencimiento del modo de prueba indicado en el requerimiento original: **antes del 10-12-2026**. Las pruebas locales no publican reglas.

### 2. Publicar reglas de Firebase Storage

- Las reglas están implementadas y las pruebas locales pasan, pero **tampoco se han publicado** en el proyecto real.
- Confirmar que Firebase Storage está habilitado y que el bucket del proyecto está configurado.
- Luego publicar `storage.rules` y guardar evidencia. La publicación cambia permisos de acceso y debe verificarse en Firebase Console.

### 3. Completar pruebas manuales de la aplicación

Usar cuentas de prueba autorizadas y registrar capturas o video:

- Registro, perfil asociado al UID, login según rol, errores de autenticación y cierre de sesión.
- CRUD de productos como administrador; consulta del inventario como vendedor.
- Crear, listar y actualizar notas de cotizaciones propias; revisar acceso de administrador.
- Subir una foto a Storage y verificar que queda en la ruta del usuario.
- Probar ubicación con permiso concedido, denegado y servicio desactivado.
- Probar tipo de cambio con conexión y sin conexión, acciones del diálogo y apertura del enlace HTTPS.

No usar cuentas ni datos de producción como datos de prueba. Registrar resultados en la matriz de trazabilidad.

### 4. Revisar visualmente las pantallas

- Abrir las pantallas en un dispositivo o emulador y compararlas con el canvas **“Layouts App Móvil Aceros We-Corp”**.
- Guardar capturas de cada pantalla y registrar cualquier diferencia. El código aplica el estilo compartido, pero esta comparación visual todavía no se ha realizado.

### 5. Cerrar la entrega

- Preparar y registrar la demostración final.
- Adjuntar evidencia de las pruebas manuales y publicaciones.
- Revisar y fusionar el PR #2 después de la revisión del equipo.

