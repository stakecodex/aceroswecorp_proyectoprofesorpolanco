# Hito 2 — Navegación y autenticación: avance y próximos pasos

**Rama:** `hito2-autenticacion` · **Responsable:** Bryan Vergara · **Fecha:** 11-11-2026

El Hito 2 quedó funcionando en el emulador: se puede crear una cuenta, iniciar sesión, recuperar la contraseña, entrar al menú según el rol y cerrar sesión, todo con Firebase Authentication y Firestore.

## Lo que quedó hecho

| Funcionalidad | Dónde está | Cómo funciona |
| --- | --- | --- |
| Conexión a Firebase | `gradle/libs.versions.toml`, `build.gradle.kts` (raíz y app) | Plugin `google-services` 4.5.0 y Firebase BoM 34.19.0 con `firebase-auth` y `firebase-firestore` |
| Inicio de sesión | `MainActivity.kt` + `activity_control_acceso.xml` | Valida campos vacíos y formato de correo; luego `signInWithEmailAndPassword` |
| Registro | `RegistroActivity.kt` + `activity_registro.xml` | Valida nombre, correo, contraseña (6+) y confirmación; crea el usuario y el documento `perfiles/{uid}` |
| Recuperación de contraseña | `MainActivity.kt` | AlertDialog con campo de correo y `sendPasswordResetEmail` |
| Inicio según rol | `MainActivity.kt` → `abrirMenuSegunRol()` | Lee `perfiles/{uid}.rol`: `administrador` abre el menú de administrador; cualquier otro, el de ventas |
| Sesión persistente | `MainActivity.kt` → `onStart()` | Si ya hay sesión, entra directo al menú |
| Cierre de sesión | `NavegacionActivities.kt` → `configurarCerrarSesion()` | La flecha de los dos menús pregunta y ejecuta `signOut()`; vuelve al login sin historial |
| Diseño de login y registro | `res/layout` y `res/drawable` (`bg_campo`, `bg_tarjeta`, `franja_seguridad`, `ic_logo_wecorp`) | Igual al documento "Layouts App Móvil Aceros We-Corp" (H4 del ramo de Metodología y Calidad) |

Perfil que se guarda al registrarse, en la colección `perfiles` (id del documento = `uid`):

| Campo | Valor |
| --- | --- |
| `uid` | UID de Firebase Authentication |
| `nombre` | Nombre ingresado |
| `correo` | Correo ingresado |
| `rol` | `vendedor` (siempre al registrarse) |
| `activo` | `true` |
| `creadoEn` | Fecha y hora del servidor |

## Cómo configurar Firebase en tu equipo

Sin esto la app no compila.

1. Pídele a Bryan el archivo `google-services.json`. No está en el repositorio a propósito: quedó en `.gitignore`, como dice la propuesta.
2. Cópialo en la carpeta `app/`, al lado de `app/build.gradle.kts`.
3. Pídele a Bryan que te agregue como miembro del proyecto Firebase **AcerosWecorp** para ver usuarios y datos en la consola.
4. En Android Studio: **Sync Project with Gradle Files** y ▶.

## Decisiones técnicas que hay que conocer

- **Firebase BoM 34.19.0, no 35.x.** La serie 35 está compilada con Kotlin 2.4 y el proyecto usa el Kotlin 2.2 que trae AGP 9.4.1. Con la 35 falla `compileDebugKotlin` ("Module was compiled with an incompatible version of Kotlin"). No subir la BoM sin subir Kotlin.
- **No aceptar el "Upgrade Assistant" (AGP 9.5.1) ni el cambio a Gradle 9.9** sin acordarlo en el equipo: un Android Studio más antiguo deja de abrir el proyecto.
- **Cómo crear un administrador:** registra la cuenta normal en la app y, en **Firestore > perfiles > {uid}**, cambia `rol` de `vendedor` a `administrador`. El rol no se puede elegir desde la app.
- **Se retiraron los botones "Explorar como vendedor / administrador".** Ahora se entra con una cuenta real.
- **Firestore está en modo de prueba** (creado el 10-11-2026): permite leer y escribir a cualquiera por 30 días, hasta alrededor del 10-12-2026 (revisar la fecha exacta en **Firestore > Reglas**). Las reglas por `uid` y rol tienen que estar antes de esa fecha.

## Pruebas del Hito 2 (evidencia)

- [ ] Registro correcto → llega al Menú Ventas y aparece en Authentication y en `perfiles`
- [ ] Registro con correo ya usado → "Ese correo ya está registrado."
- [ ] Contraseña de menos de 6 caracteres y contraseñas distintas → mensaje de error
- [ ] Login correcto → menú según rol
- [ ] Login con contraseña incorrecta → "Correo o contraseña incorrectos."
- [ ] Recuperar contraseña → llega el correo
- [ ] Usuario con `rol: administrador` → abre el Menú Administrador
- [ ] Cerrar sesión → vuelve al login y el botón atrás no regresa al menú
- [ ] Cerrar y abrir la app con sesión iniciada → entra directo al menú

## Próximos pasos

Responsables sugeridos según la distribución preliminar de la propuesta (Bryan: navegación y Activities; Aníbal: Firebase, modelo de datos y reglas; José: layouts, API y pruebas). Se ajusta en la revisión de cada hito.

### Hito 3 — Persistencia y vistas (semana 13)

- [ ] Modelos Kotlin `PerfilUsuario`, `Producto`, `ItemCotizacion` y `Cotizacion` — Aníbal
- [ ] CRUD de `productos` en Firestore: crear, listar, editar y eliminar — Aníbal y Bryan
- [ ] CRUD de `cotizaciones` asociado al `uid` del vendedor — Bryan
- [ ] ListView con adaptador propio y layout de fila para Consulta de inventario y Mis cotizaciones — José y Bryan
- [ ] Mostrar nombre y rol del usuario en el encabezado de los menús, leídos de `perfiles` — Bryan
- [ ] Igualar el resto de las pantallas al diseño "Layouts App Móvil Aceros We-Corp" — José

### Hito 4 — Integración (semana 14)

- [ ] Cámara con la API de resultados de actividad y foto guardada en Firebase Storage
- [ ] Geolocalización con permisos en tiempo de ejecución (aceptado, denegado y sin ubicación)
- [ ] AlertDialog con acciones positiva, negativa y neutra con resultados distintos; Toast de confirmación
- [ ] Cliente REST `GET https://open.er-api.com/v6/latest/USD` mostrando `rates.CLP`, sin bloquear el hilo principal
- [ ] Intent implícito `ACTION_VIEW` con una URI HTTPS

### Hito 5 — Cierre y entrega (semana 15)

- [ ] Reglas de Firestore y Storage por `uid` y rol, probadas con casos permitidos y denegados (antes de que venza el modo de prueba)
- [ ] Matriz de trazabilidad y registro de pruebas
- [ ] APK y demostración
