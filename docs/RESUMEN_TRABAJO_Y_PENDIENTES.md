# Resumen del trabajo y pendientes

**Proyecto:** Aceros We-Corp

**Fecha de revisión:** 19-11-2026

**Rama:** `hito3-layouts-firestore-tests`

**Revisión de cambios:** [PR #2](https://github.com/stakecodex/aceroswecorp_proyectoprofesorpolanco/pull/2)

Este resumen distingue lo implementado de lo probado y publicado. La implementación no significa que todas las funciones hayan sido validadas en un dispositivo o en Firebase real.

## Trabajo realizado

### Diseño y navegación

- Se actualizaron los layouts de los menús, inventario, cotizaciones y ventas/documentos siguiendo el estilo de login y registro.
- Se agregó un encabezado compartido y un fondo para tarjetas interactivas.
- Se conectó la navegación existente entre pantallas y se añadió la lectura del nombre y rol del perfil para el encabezado.
- En las vistas de inventario y cotizaciones se reemplazaron los listados de ejemplo por `ListView` con adaptadores y filas propias.

### Datos y Firestore

- Se incorporaron los modelos `PerfilUsuario`, `Producto`, `ItemCotizacion` y `Cotizacion`, y un repositorio para acceder a Firestore.
- Inventario: consulta, búsqueda y filtro por categoría para vendedores; creación, edición y eliminación de productos desde la vista de administrador.
- Cotizaciones: creación asociada al UID del vendedor, consulta de las propias, filtro por estado y edición de notas. La vista de administrador permite consultar, cambiar el estado y eliminar cotizaciones.
- Se prepararon [reglas de Firestore](../firestore.rules) para perfiles, productos y cotizaciones, con permisos por UID y rol.
- Se escribieron diez pruebas automatizadas de accesos permitidos y denegados en [firestore.rules.test.cjs](../tests/firestore.rules.test.cjs).

### Funciones del dispositivo e integración

- Se agregó captura de una vista previa de la cámara y subida como JPEG a Firebase Storage, en `usuarios/{uid}/fotos/{fileName}`.
- Se prepararon [reglas de Storage](../storage.rules) y cuatro pruebas automatizadas de acceso.
- Se agregó consulta de ubicación con permisos en ejecución, manejo de denegación, proveedor desactivado y espera de una ubicación reciente.
- Se implementó la consulta de `rates.CLP` desde `https://open.er-api.com/v6/latest/USD` en un hilo de trabajo.
- Se agregaron un diálogo con acciones positiva, negativa y neutra y la apertura de una URI HTTPS mediante `ACTION_VIEW`.

### Documentación, entorno y entrega

- Se crearon el [registro de pruebas y matriz de trazabilidad](./REGISTRO_PRUEBAS_Y_MATRIZ_TRAZABILIDAD.md) y el [estado y pendientes](./ESTADO_Y_PENDIENTES.md); se actualizó el documento de hitos.
- Se configuró Java a partir del JBR de Android Studio y la ruta local del Android SDK para compilar en este equipo.
- Se generó un APK de depuración en `app/build/outputs/apk/debug/app-debug.apk`.
- Se subieron los cambios a la rama del PR. El archivo local `app/google-services.json` y el APK no forman parte del repositorio.

## Validaciones realizadas

Las siguientes ejecuciones se realizaron el 19-11-2026:

| Validación | Resultado | Alcance |
| --- | --- | --- |
| `npm run test:rules` | 10/10 aprobadas | Reglas de Firestore en emulador, con datos ficticios. |
| `npm run test:storage` | 4/4 aprobadas | Reglas de Storage con emuladores locales. |
| `:app:assembleDebug` | Aprobado | Compilación y generación del APK debug con la configuración local de Firebase. |
| `:app:testDebugUnitTest` | Aprobado | Pruebas unitarias existentes; no demuestra los flujos completos. |
| `:app:connectedDebugAndroidTest` | Aprobado en Pixel 6a emulado | Comprueba que se muestran las acciones principales del login. |
| `:app:lintDebug` | Aprobado con advertencias | Análisis de Android lint; el éxito no implica ausencia de advertencias. |

Las pruebas con emuladores no publican reglas ni demuestran el comportamiento del proyecto Firebase real.

## Pendiente por hacer

### Firebase y seguridad

- [ ] Autenticar Firebase CLI y, cuando se decida hacerlo, publicar las reglas de Firestore en `aceroswecorp`. El intento anterior falló por falta de autenticación; no se publicaron. Verificar la fecha de vencimiento del modo de prueba en la consola y publicar antes del **10-12-2026** indicado en el requerimiento.
- [ ] Confirmar que Firebase Storage y el bucket están habilitados; revisar y publicar sus reglas por separado, con autorización, y comprobarlas en la consola.
- [ ] Guardar evidencia de ambas publicaciones y probar accesos reales permitidos y denegados con cuentas autorizadas.

### Pruebas y revisión visual

- [ ] Probar registro, login, recuperación de contraseña, sesión persistente y cierre de sesión con cuentas de prueba de ambos roles.
- [ ] Probar en la app las operaciones de productos y cotizaciones, incluyendo restricciones por rol y persistencia.
- [ ] Probar cámara y subida real a Storage; ubicación concedida, denegada y desactivada; consulta de tipo de cambio con y sin conexión; diálogo y apertura del enlace.
- [ ] Comparar todas las pantallas con el canvas **“Layouts App Móvil Aceros We-Corp”**, verificar navegación y guardar capturas.
- [ ] Registrar los resultados y adjuntar capturas o video a la matriz y a la entrega; no marcar como aprobadas pruebas no ejecutadas.

### Alcance funcional que aún requiere revisión

- [ ] Revisar el encabezado en dispositivo: actualmente el identificador que recibe nombre y rol está en el texto del nombre de la aplicación.
- [ ] Sustituir o identificar como demostración los indicadores y registros estáticos que permanecen en los menús y en ventas/documentos; no representan datos reales.
- [ ] La gestión de clientes y usuarios sigue indicada como pendiente en la navegación; definir su alcance antes de implementarla.
- [ ] Revisar con el equipo si la cotización debe admitir varios productos o editar campos adicionales: el formulario actual guarda un producto por cotización y el vendedor solo edita notas. El borrado está reservado al administrador en las reglas actuales.
- [ ] Confirmar si la foto debe quedar asociada a una cotización o producto: actualmente se sube a una ruta del usuario, sin vincularla a un documento.

### Cierre

- [ ] Preparar la demostración final y adjuntar sus evidencias.
- [ ] Revisar el [PR #2](https://github.com/stakecodex/aceroswecorp_proyectoprofesorpolanco/pull/2) y fusionarlo en `main` cuando el equipo lo apruebe.

El APK de depuración es local. Cada integrante debe obtener `google-services.json` por un canal autorizado para compilar y probar la app; el archivo está excluido de Git.
