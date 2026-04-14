# function-app-libreria

Function App Java para gestión de biblioteca, con 8 funciones HTTP/GraphQL:

- `usuarios`
- `libros`
- `autores`
- `prestamos`
- `resumen/catalogo`
- `resumen/general`
- `graphql/catalogo`
- `graphql/general`

El proyecto se conecta a una **Base de Datos Autónoma de Oracle Cloud** usando wallet y variables de entorno.

---

## ¿Qué incluye este proyecto?

- Azure Functions HTTP en Java 21.
- Consultas REST de resumen para catálogo y estado general.
- Endpoints GraphQL formales con `graphql-java`.
- Persistencia Oracle con repositorios separados por entidad.
- Refactor de `OracleStore` como fachada para mantener compatibilidad.
- Documentación JavaDoc en métodos clave.
- `Dockerfile` para desplegar la app como contenedor.

---

## Arquitectura rápida

- `src/main/java/cl/duoc/biblioteca/functions/function/`
	- handlers HTTP (`UsuariosFunction`, `LibrosFunction`, `AutoresFunction`, `PrestamosFunction`),
	- handlers REST de resumen (`ResumenCatalogoFunction`, `ResumenGeneralFunction`),
	- handlers GraphQL (`CatalogoGraphqlFunction`, `ResumenGeneralGraphqlFunction`).
- `src/main/java/cl/duoc/biblioteca/functions/repository/`
	- acceso a datos (`UsuarioRepository`, `LibroRepository`, `AutorRepository`, `PrestamoRepository`),
	- infraestructura Oracle (`OracleInfra`),
	- utilidades (`RepositoryUtils`),
	- fachada (`OracleStore`).
- `src/main/java/cl/duoc/biblioteca/functions/exception/`
	- traducción de errores SQL (`RepositoryExceptionHandler`).

---

## Function App en Azure

- **Nombre de Function App de despliegue:** `functionsbiblioteca`
- **Funciones desplegadas:** `usuarios`, `libros`, `autores`, `prestamos`, `resumen/catalogo`, `resumen/general`, `graphql/catalogo`, `graphql/general`

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
```
