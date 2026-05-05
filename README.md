# function-app-libreria

Function App Java para gestión de biblioteca, con 10 funciones HTTP / GraphQL / Event Grid:

- `usuarios`
- `libros`
- `autores`
- `prestamos`
- `resumen/catalogo`
- `resumen/general`
- `graphql/catalogo`
- `graphql/general`
- `notificacionConsumer` *(Event Grid Trigger)*
- `notificaciones`

El proyecto se conecta a una **Base de Datos Autónoma de Oracle Cloud** usando wallet y variables de entorno.

---

## ¿Qué incluye este proyecto?

- Azure Functions HTTP en Java 21.
- Consultas REST de resumen para catálogo y estado general.
- Endpoints GraphQL formales con `graphql-java`.
- **Consumer Event Grid (`@EventGridTrigger`) que materializa notificaciones de dominio.**
- **Endpoint REST de consulta de notificaciones (lista global y por usuario).**
- Persistencia Oracle con repositorios separados por entidad.
- Refactor de `OracleStore` como fachada para mantener compatibilidad.
- Documentación JavaDoc en métodos clave.
- `Dockerfile` para desplegar la app como contenedor.

---

## Arquitectura rápida

- `src/main/java/cl/duoc/biblioteca/functions/function/`
	- handlers HTTP (`UsuariosFunction`, `LibrosFunction`, `AutoresFunction`, `PrestamosFunction`),
	- handlers REST de resumen (`ResumenCatalogoFunction`, `ResumenGeneralFunction`),
	- handlers GraphQL (`CatalogoGraphqlFunction`, `ResumenGeneralGraphqlFunction`),
	- consumer Event Grid (`NotificacionConsumerFunction`),
	- handler HTTP de consulta de notificaciones (`NotificacionesFunction`).
- `src/main/java/cl/duoc/biblioteca/functions/domain/`
	- entidades de dominio (`Notificacion`, ...).
- `src/main/java/cl/duoc/biblioteca/functions/repository/`
	- acceso a datos (`UsuarioRepository`, `LibroRepository`, `AutorRepository`, `PrestamoRepository`, `NotificacionRepository`),
	- infraestructura Oracle (`OracleInfra`),
	- utilidades (`RepositoryUtils`),
	- fachada (`OracleStore`).
- `src/main/java/cl/duoc/biblioteca/functions/exception/`
	- traducción de errores SQL (`RepositoryExceptionHandler`).

---

## Function App en Azure

- **Nombre de Function App de despliegue:** `functionsbiblioteca`
- **Funciones desplegadas:** `usuarios`, `libros`, `autores`, `prestamos`, `resumen/catalogo`, `resumen/general`, `graphql/catalogo`, `graphql/general`, `notificacionConsumer` *(EventGrid)*, `notificaciones`
- **Topic Event Grid asociado al consumer:** `biblioteca-topics`
- **Event Subscription:** apunta al recurso `functionsbiblioteca` → función `notificacionConsumer`

> Nota: En `pom.xml` existe `functionAppName=biblioteca-function-app` para el empaquetado local.
> Si vas a desplegar con `mvn azure-functions:deploy`, ajusta ese nombre al recurso real (`functionsbiblioteca`) o despliega desde la extensión de VS Code seleccionando el recurso correcto.

---

## Configuración obligatoria de VS Code (`.vscode/settings.json`)

Para que la extensión de Azure Functions funcione correctamente en este repo, usa al menos:

```json
{
	"azureFunctions.javaBuildTool": "maven",
	"azureFunctions.deploySubpath": "target/azure-functions/biblioteca-function-app",
	"azureFunctions.projectLanguage": "Java",
	"azureFunctions.projectRuntime": "~4",
	"azureFunctions.preDeployTask": "package (functions)"
}
```

### Error común de despliegue

Si aparece:

`Failed to deploy path that does not exist`

verifica que `azureFunctions.deploySubpath` sea exactamente:

`target/azure-functions/biblioteca-function-app`

y no una ruta con prefijo extra (`function-app/...`).

---

## Variables de entorno en Azure Functions

En el recurso `functionsbiblioteca`, configura estas variables de entorno:

- `ORACLE_USER`
- `ORACLE_PASSWORD`
- `ORACLE_TNS_ALIAS`
- `ORACLE_WALLET_PATH`

También deben existir las estándar de Functions:

- `FUNCTIONS_WORKER_RUNTIME=java`
- `FUNCTIONS_EXTENSION_VERSION=~4`

> Compatibilidad: el código también acepta `ORACLE_ADMIN_PASSWORD` como fallback, pero se recomienda estandarizar en `ORACLE_PASSWORD`.

---

## Configuración de `local.settings.json` (desarrollo local)

Ejemplo recomendado:

```json
{
	"IsEncrypted": false,
	"Values": {
		"AzureWebJobsStorage": "UseDevelopmentStorage=true",
		"FUNCTIONS_WORKER_RUNTIME": "java",
		"ORACLE_USER": "<REEMPLAZAR_USUARIO_ORACLE>",
		"ORACLE_PASSWORD": "<REEMPLAZAR_PASSWORD_ORACLE>",
		"ORACLE_TNS_ALIAS": "<REEMPLAZAR_TNS_ALIAS>",
		"ORACLE_WALLET_PATH": "<REEMPLAZAR_RUTA_WALLET>"
	}
}
```

### Recomendaciones

- Mantener wallet Oracle en una ruta local válida.
- Verificar que `ORACLE_TNS_ALIAS` exista en `tnsnames.ora` del wallet.

---

## Ejecutar localmente

1. Compilar y empaquetar:
	 - `mvn clean package`
2. Levantar Functions host:
	 - tarea VS Code: `func: host start`

Base URL local:

- `http://localhost:7071/api`

---

## Despliegue en Azure desde VS Code

1. Ejecuta `mvn clean package`.
2. En panel Azure, selecciona el proyecto y **Deploy to Azure...**.
3. Elige la Function App `functionsbiblioteca`.
4. Si la app está detenida, inicia primero el recurso (Start) y vuelve a desplegar.

---

## Docker (despliegue en contenedor)

Se creó `Dockerfile` con build multi-stage:

- Etapa 1: compila y empaqueta con Maven.
- Etapa 2: usa imagen oficial `mcr.microsoft.com/azure-functions/java:4-java21`.

Esto permite desplegar la Function App en entornos containerizados manteniendo el runtime oficial de Azure Functions.

---

## Endpoints y ejemplos de uso

Base URL en Azure (ejemplo):

- `https://functionsbiblioteca.azurewebsites.net/api`

## 1) `usuarios`

Gestiona usuarios y su estado (activo/inactivo según préstamos).

- `GET /usuarios` → lista usuarios
- `GET /usuarios/{id}` → detalle por ID
- `POST /usuarios` → crea usuario
- `PUT /usuarios/{id}` → actualiza usuario
- `DELETE /usuarios/{id}` → elimina o marca inactivo si tiene préstamos activos

Ejemplo `POST`:

```json
{
	"nombre": "Ana",
	"apellidoPaterno": "Pérez",
	"apellidoMaterno": "Gómez",
	"email": "ana.perez@correo.cl",
	"activo": true
}
```

## 2) `autores`

Gestiona autores de libros.

- `GET /autores`
- `GET /autores/{id}`
- `POST /autores`
- `PUT /autores/{id}`
- `DELETE /autores/{id}` (bloquea eliminación si tiene libros asociados)

Ejemplo `POST`:

```json
{
	"nombreAutor": "Gabriel García Márquez"
}
```

## 3) `libros`

Gestiona catálogo de libros.

- `GET /libros`
- `GET /libros/{id}`
- `POST /libros`
- `PUT /libros/{id}`
- `DELETE /libros/{id}` (bloquea eliminación si tiene préstamos asociados)

Ejemplo `POST`:

```json
{
	"isbn": "9788497592208",
	"titulo": "Cien años de soledad",
	"anioPublicacion": 1967,
	"copiasTotales": 5,
	"copiasDisponible": 5,
	"idAutor": "1"
}
```

## 4) `prestamos`

Gestiona préstamos entre usuarios y libros.

- `GET /prestamos`
- `GET /prestamos/{id}`
- `POST /prestamos`
- `PUT /prestamos/{id}`
- `DELETE /prestamos/{id}`

Ejemplo `POST`:

```json
{
	"idUsuario": "1",
	"idLibro": "1",
	"fechaPrestamo": "2026-03-30",
	"fechaDevolucion": "2026-04-13",
	"estado": "PRESTADO"
}
```

## 5) `notificacionConsumer` *(Event Grid Trigger)*

Función con `@EventGridTrigger` que **consume** los eventos de dominio publicados al **Event Grid Topic** `biblioteca-topics` y persiste un registro en la tabla `NOTIFICACIONES` por cada uno.

- **No es invocable por HTTP.** Se gatilla automáticamente por la Event Subscription configurada en Azure Portal.
- **Eventos que procesa (switch por `eventType`):**
	- `Prestamo.Creado`
	- `Prestamo.Devuelto`
	- `Usuario.Inactivo`
- **Salida:** un `Notificacion` con `tipo`, `asunto`, `cuerpo`, `estado=PENDIENTE`, `fechaCreacion=now`, `fechaEnvio=null`.

Para verificar el flujo manualmente, basta con publicar un evento desde el publisher (`event-rounting-libreria/eventPublisher`) o desde el BFF (que ya orquesta la publicación).

## 6) `notificaciones`

Endpoint HTTP que expone las notificaciones generadas por el consumer.

- `GET /notificaciones` → lista todas las notificaciones (más recientes primero).
- `GET /notificaciones/{idUsuario}` → notificaciones de un usuario específico.

Respuesta de ejemplo:

```json
[
	{
		"id": "12",
		"idUsuario": "1",
		"tipo": "PRESTAMO_CREADO",
		"asunto": "Préstamo registrado",
		"cuerpo": "Tu préstamo del libro 5 ha sido registrado.",
		"estado": "PENDIENTE",
		"fechaCreacion": "2026-05-03T10:30:00Z",
		"fechaEnvio": null
	}
]
```

> El campo `fechaEnvio` se mantiene `null` en este alcance — queda reservado para una futura etapa de despacho real (mailer/SMS) que marque la notificación como enviada.

---

## Flujo event-driven (Notificaciones)

```
[BFF / cliente HTTP]
		↓ POST con {eventType, subject, data}
[event-rounting-libreria → eventPublisher]
		↓ sendEvent()
[Event Grid Topic: biblioteca-topics]
		↓ entrega async
[Event Subscription → notificacionConsumer]
		↓ NotificacionRepository.saveNotificacion()
[Tabla NOTIFICACIONES en Oracle ATP]
		↑ consultable vía
[GET /api/notificaciones]
```

### Pre-requisito SQL

Antes de procesar eventos, debe existir la tabla `NOTIFICACIONES` en Oracle ATP. El DDL está en `db/notificaciones.sql`.

### Configuración del consumer en Azure

`notificacionConsumer` no requiere variables de entorno adicionales: el binding `@EventGridTrigger` se conecta automáticamente a la Event Subscription configurada en el portal.

Pasos para crear la Event Subscription (una sola vez):
1. Azure Portal → Event Grid Topic `biblioteca-topics` → **+ Event Subscription**
2. **Endpoint Type:** Azure Function
3. **Endpoint:** Subscription → `rg_functions_bliblioteca` → Function App `functionsbiblioteca` → función `notificacionConsumer`
4. (Opcional) Filtrar por **Event Types**: `Prestamo.Creado`, `Prestamo.Devuelto`, `Usuario.Inactivo`

---

## Ejemplos rápidos con cURL

```bash
# Listar usuarios
curl -X GET http://localhost:7071/api/usuarios

# Crear autor
curl -X POST http://localhost:7071/api/autores \
	-H "Content-Type: application/json" \
	-d '{"nombreAutor":"Isabel Allende"}'

# Crear libro
curl -X POST http://localhost:7071/api/libros \
	-H "Content-Type: application/json" \
	-d '{"isbn":"9788401337208","titulo":"La casa de los espíritus","anioPublicacion":1982,"copiasTotales":3,"copiasDisponible":3,"idAutor":"1"}'

# Crear préstamo
curl -X POST http://localhost:7071/api/prestamos \
	-H "Content-Type: application/json" \
	-d '{"idUsuario":"1","idLibro":"1","estado":"PRESTADO"}'

# Resumen catálogo
curl -X GET http://localhost:7071/api/resumen/catalogo

# Resumen general
curl -X GET http://localhost:7071/api/resumen/general

# GraphQL catálogo
curl -X POST http://localhost:7071/api/graphql/catalogo \
	-H "Content-Type: application/json" \
	-d '{"query":"query { libros { id titulo } }"}'

# GraphQL general
curl -X POST http://localhost:7071/api/graphql/general \
	-H "Content-Type: application/json" \
	-d '{"query":"query { usuarios { id nombre } }"}'

# Listar notificaciones (todas)
curl -X GET http://localhost:7071/api/notificaciones

# Listar notificaciones de un usuario específico
curl -X GET http://localhost:7071/api/notificaciones/1
```

> El consumer `notificacionConsumer` no se prueba con cURL: se gatilla cuando llega un evento al Topic. Para forzar uno, publicar desde `event-rounting-libreria/eventPublisher` y luego consultar `/api/notificaciones`.
