# Propuesta del proyecto: Aceroswecorp

## Hito 1 — Propuesta y diseño preliminar

**Asignatura:** Taller de Nuevas Tecnologías
**Integrantes:** Equipo de Bryan Vergara, Aníbal Rodríguez y José Rodríguez
**Plataforma:** Android, Kotlin
**Estado:** propuesta aprobada como base de trabajo; las integraciones técnicas descritas son planificadas, no se consideran implementadas hasta que cada hito se complete y pruebe.

## 1. Identificación y descripción

**Aceroswecorp** será una aplicación móvil para apoyar la gestión de productos de acero y cotizaciones de una distribuidora. Permitirá al personal consultar inventario, registrar y dar seguimiento a cotizaciones, y entregar una referencia de tipo de cambio USD/CLP para apoyar la evaluación de precios de productos expuestos a costos en dólares.

La propuesta mantiene la temática de cotizaciones e inventario reflejada en los layouts existentes. La aplicación tendrá perfiles de vendedor y administrador; las funciones disponibles y los permisos sobre los datos dependerán del usuario autenticado.

## 2. Problemática y usuarios beneficiarios

La consulta de existencias y la preparación o seguimiento de cotizaciones puede depender de registros separados y procesos manuales. Esto dificulta mantener información consistente, saber quién creó o modificó un registro y atender oportunamente solicitudes de clientes.

Los usuarios beneficiarios directos serán:

- **Vendedores:** consultan productos y existencias, preparan cotizaciones y hacen seguimiento.
- **Administradores:** mantienen el catálogo, supervisan cotizaciones e inventario y revisan la actividad.
- **Clientes de la distribuidora:** reciben cotizaciones preparadas con información más oportuna y consistente.

## 3. Objetivos

### Objetivo general

Desarrollar una aplicación Android en Kotlin para apoyar la gestión de inventario y cotizaciones de Aceroswecorp, con autenticación, persistencia segura en Firebase, navegación coherente e integración de funcionalidades nativas y una API REST útil para el proceso de cotización.

### Objetivos específicos

1. Permitir que los usuarios se registren e inicien sesión, y que puedan recuperar su contraseña y cerrar sesión.
2. Administrar productos y cotizaciones mediante operaciones CRUD asociadas a usuarios y roles.
3. Mostrar los registros de Firestore en listas personalizadas y permitir seleccionar elementos para consultarlos o gestionarlos.
4. Integrar cámara y ubicación a una cotización, gestionando permisos y conservando la fotografía de manera persistente.
5. Consultar y mostrar el tipo de cambio USD/CLP mediante una API REST, con estados de carga y tratamiento de errores.

## 4. Funcionalidades y recorrido esperado

1. El usuario abre la aplicación e inicia sesión o crea una cuenta.
2. Según su rol, llega al menú de vendedor o administrador.
3. Consulta productos y existencias en el inventario.
4. Crea una cotización para un cliente, agrega productos y cantidades, revisa el total y puede consultar la referencia USD/CLP.
5. Si es pertinente, registra la ubicación asociada a la entrega y toma una fotografía de respaldo; la imagen se guarda en Firebase Storage y la cotización conserva su referencia.
6. Guarda la cotización, la consulta en sus listas y actualiza sus datos o la elimina cuando sus permisos y estado lo permitan.
7. El administrador revisa las cotizaciones y mantiene el catálogo de productos.
8. El usuario puede consultar el historial, abrir información externa mediante un enlace y cerrar sesión.

Las pantallas de referencia existentes incluyen acceso, menú de ventas, nueva cotización, consulta de inventario, mis cotizaciones, menú de administrador e historiales. Los XML están creados, pero todavía deben conectarse a Activities y a funciones reales en hitos posteriores.

## 5. Integración técnica prevista

| Requisito | Implementación prevista | Estado al cierre de este hito |
|---|---|---|
| Kotlin y mínimo 7 Activity | Actividades con responsabilidades separadas para autenticación, menú, productos, cotizaciones e historial; navegación con Intent explícitos. | Por implementar |
| Inicio y Manifest | Actividad de acceso como punto de entrada; registro de las demás Activities en `AndroidManifest.xml`. | Por implementar |
| Intent explícito e implícito | Navegación entre pantallas con Intent explícitos; apertura de una fuente externa con `ACTION_VIEW` y URI HTTPS. | Por implementar |
| Firebase Authentication | Registro, inicio, recuperación de contraseña y cierre de sesión. | Por implementar |
| Cloud Firestore | Perfil de usuario y CRUD de las colecciones funcionales `productos` y `cotizaciones`, con validación y manejo visible de errores. | Por implementar |
| Modelo y ListView | Modelos Kotlin, `ArrayList`, adaptadores propios con layouts personalizados, carga desde Firestore y selección de elementos. | Por implementar |
| Toast y AlertDialog | Toast para confirmar resultados; AlertDialog con acciones positiva, negativa y neutra con comportamientos diferenciados. | Por implementar |
| Cámara | Captura asociada a una cotización mediante la API de resultados de actividad; persistencia en Firebase Storage, no en un documento Firestore como imagen codificada. | Por implementar |
| Geolocalización | Solicitud de ubicación asociada al contexto de una entrega/cotización, con permisos en tiempo de ejecución y tratamiento de denegación o ubicación no disponible. | Por implementar |
| API REST | Solicitud HTTP GET a la API de tipo de cambio USD, parseo JSON, visualización del valor CLP y estados de carga, desconexión y error. | Endpoint validado; cliente Android por implementar |
| Seguridad | Reglas de Firestore y Storage basadas en UID y rol; las operaciones de cada usuario se validan también en la interfaz, sin sustituir las reglas del servidor. | Por implementar |

### API REST seleccionada

- **Servicio:** [ExchangeRate-API, endpoint de tasas abiertas](https://open.er-api.com/v6/latest/USD).
- **Fuente y documentación:** [open.er-api.com](https://www.exchangerate-api.com/docs/free).
- **Solicitud:** `GET https://open.er-api.com/v6/latest/USD`.
- **Autenticación para este endpoint:** no requiere clave.
- **Dato utilizado:** `rates.CLP`, la cantidad de pesos chilenos por dólar estadounidense; también se mostrará `time_last_update_utc`.
- **Utilidad:** presentar una referencia para que el vendedor contextualice cotizaciones de productos cuyo costo de adquisición pueda depender del dólar. No reemplaza la lista de precios aprobada por la empresa ni recalcula automáticamente una cotización.
- **Ejemplo de estructura JSON relevante:**

```json
{
  "result": "success",
  "base_code": "USD",
  "time_last_update_utc": "Wed, 04 Nov 2026 00:02:31 +0000",
  "rates": {
    "CLP": 978.112665
  }
}
```

El endpoint se consultó durante la preparación de esta propuesta y devolvió `result: "success"`, `base_code: "USD"` y un valor para CLP. El valor y la hora del ejemplo son ilustrativos de esa consulta y cambian con el tiempo. La aplicación deberá ejecutar la solicitud en segundo plano, convertir el JSON a un modelo Kotlin, indicar la última actualización y mostrar estados de carga, error HTTP, error de conexión y respuesta inválida. Se conservará solo el último resultado correcto si la solución de caché se incorpora; un dato almacenado deberá identificarse como potencialmente desactualizado. No se enviarán datos personales ni cotizaciones al servicio.

## 6. Diseño preliminar y navegación

```text
Acceso
├── Registro
├── Recuperar contraseña
└── Inicio según rol
    ├── Vendedor
    │   ├── Consulta de inventario → Detalle de producto
    │   ├── Nueva cotización → Guardar → Detalle de cotización
    │   ├── Mis cotizaciones → Detalle / editar / eliminar
    │   └── Referencia USD/CLP
    └── Administrador
        ├── Gestión de productos → crear / consultar / editar / eliminar
        ├── Gestión e historial de cotizaciones
        └── Cerrar sesión
```

Cada pantalla deberá cumplir una función verificable; no se crearán Activities vacías para alcanzar el mínimo. Los layouts existentes son el prototipo visual de partida. Durante el hito de navegación se consolidará el mapeo final de pantallas a Activities y se ajustará el `AndroidManifest.xml`.

## 7. Modelo de datos preliminar

### Colección `perfiles` (estructura de perfil)

Documento identificado por el UID de Firebase Authentication:

| Campo | Tipo | Descripción |
|---|---|---|
| `uid` | String | UID del usuario autenticado |
| `nombre` | String | Nombre para mostrar |
| `correo` | String | Correo asociado a la cuenta |
| `rol` | String | `vendedor` o `administrador` |
| `activo` | Boolean | Indica si el perfil está habilitado |
| `creadoEn` | Timestamp | Fecha de creación |

### Colección funcional `productos`

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | String | ID del documento Firestore |
| `codigo` | String | Código único del producto |
| `nombre` | String | Nombre o descripción |
| `categoria` | String | Familia de producto |
| `unidad` | String | Unidad de venta |
| `stock` | Number | Existencia actual, no negativa |
| `precioClp` | Number | Precio de referencia en CLP |
| `actualizadoEn` | Timestamp | Última actualización |
| `actualizadoPor` | String | UID del usuario que actualizó |

### Colección funcional `cotizaciones`

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | String | ID del documento Firestore |
| `propietarioUid` | String | UID creador y propietario de la cotización |
| `clienteNombre` | String | Nombre del cliente |
| `items` | Array de objetos | Producto, código, cantidad y precio unitario acordado |
| `subtotalClp` / `ivaClp` / `totalClp` | Number | Totales calculados y validados |
| `estado` | String | Estado controlado de la cotización |
| `creadaEn` / `actualizadaEn` | Timestamp | Fechas de auditoría |
| `ubicacion` | Map opcional | Latitud, longitud y fecha de captura para la entrega |
| `fotografiaUrl` | String opcional | URL/ruta de la imagen persistida en Firebase Storage |

Los modelos Kotlin iniciales serán `PerfilUsuario`, `Producto`, `ItemCotizacion` y `Cotizacion`. El alcance CRUD de las dos colecciones funcionales deberá cubrir crear, leer/listar, actualizar y eliminar. Se aplicarán validaciones de campos, cantidades, precios y estados antes de persistir.

### Acceso previsto

- Un usuario autenticado puede leer el catálogo activo.
- Solo administradores pueden crear, actualizar o eliminar productos.
- Un vendedor puede crear y gestionar sus propias cotizaciones; el UID del propietario debe coincidir con `request.auth.uid`.
- Un administrador puede revisar las cotizaciones conforme a su rol.
- Cada usuario puede leer y actualizar su propio perfil; el rol no podrá elevarse desde el cliente.
- Firebase Storage aplicará reglas equivalentes para las fotografías y validará usuario y ruta.
- Las reglas definitivas se probarán con los casos de acceso permitido y denegado; ocultar botones en Android no se considerará control de seguridad suficiente.

## 8. Plan por hitos y semanas

Cada hito se revisará, probará y publicará por separado en el repositorio. El trabajo no se considerará completo por solo crear pantallas o clases sin comportamiento funcional.

| Hito | Semana | Alcance | Evidencia |
|---|---:|---|---|
| 1. Propuesta y diseño | 11 | Problema, objetivos, flujo, modelo, selección de API y planificación. | Este documento y revisión de la propuesta. |
| 2. Navegación y autenticación | 12 | Activities funcionales, Manifest, Intents y flujos Firebase Authentication. | Recorrido de pantallas y pruebas de registro, inicio, error, recuperación y cierre. |
| 3. Persistencia y vistas | 13 | Perfiles, dos CRUD Firestore, modelos, ArrayList, adaptadores y ListView. | Pruebas de altas, consultas, cambios, eliminaciones y selección en listas. |
| 4. Integración | 14 | Cámara/Storage, ubicación, diálogos/Toast y cliente REST USD/CLP. | Captura persistida, permisos, JSON mostrado y pruebas de carga/errores. |
| 5. Cierre y entrega | 15 | Correcciones, reglas de seguridad, pruebas, instrucciones y empaquetado. | Matriz de trazabilidad, registro de pruebas, APK si el entorno lo permite y demostración. |

## 9. Distribución preliminar de tareas

La asignación se coordinará entre Bryan Vergara, Aníbal Rodríguez y José Rodríguez en cada revisión del hito. Como punto de partida:

- **Bryan Vergara:** navegación y Activities.
- **Aníbal Rodríguez:** Firebase, modelo de datos y reglas.
- **José Rodríguez:** layouts, integración de API y apoyo a pruebas.
- **Trabajo conjunto:** definición funcional, revisión de seguridad, validación cruzada, pruebas de aceptación y defensa técnica.

La distribución es preliminar y podrá ajustarse según disponibilidad y avances; cada integrante deberá comprender la solución completa.

## 10. Criterios de aceptación para los siguientes hitos

- No integrar credenciales administrativas, claves privadas ni archivos locales sensibles al repositorio.
- Mantener `google-services.json` fuera del control de versiones si contiene configuración sensible; documentar cómo configurar Firebase en un entorno autorizado.
- Implementar operaciones de red y persistencia sin bloquear el hilo principal y presentar errores comprensibles.
- Verificar permisos aceptados, denegados y ubicación no disponible.
- Probar las tres acciones de AlertDialog y comprobar que cada una produce un resultado distinto.
- Ejecutar la API desde Android y mostrar datos procesados, no solo registrar la respuesta en consola.
- Mantener una matriz de trazabilidad y un registro real de pruebas a medida que se completen funcionalidades.
- Actualizar esta propuesta si una decisión del equipo o del docente cambia el alcance.
