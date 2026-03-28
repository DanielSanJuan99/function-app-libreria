package cl.duoc.biblioteca.functions.function;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import cl.duoc.biblioteca.functions.domain.Usuario;
import cl.duoc.biblioteca.functions.repository.OracleStore;

public class UsuariosFunction {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @FunctionName("usuarios")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE},
                    authLevel = AuthorizationLevel.ANONYMOUS,
                    route = "usuarios/{id?}")
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context
    ) {
        try {
            String id = extractId(request, "usuarios");
            HttpMethod method = request.getHttpMethod();
            return switch (method) {
                case GET -> handleGet(request, id);
                case POST -> handlePost(request);
                case PUT -> handlePut(request, id);
                case DELETE -> handleDelete(request, id);
                default -> request.createResponseBuilder(HttpStatus.METHOD_NOT_ALLOWED)
                        .body(Map.of("error", "Método no soportado"))
                        .build();
            };
        } catch (Exception ex) {
            context.getLogger().log(Level.SEVERE, "Error en usuarios function", ex);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno", "detalle", ex.getMessage()))
                    .build();
        }
    }

    private HttpResponseMessage handleGet(HttpRequestMessage<Optional<String>> request, String id) {
        if (id == null || id.isBlank()) {
            return request.createResponseBuilder(HttpStatus.OK)
                    .body(OracleStore.getUsuarios())
                    .build();
        }
        Usuario usuario = OracleStore.getUsuario(id);
        if (usuario == null) {
            return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Usuario no encontrado", "id", id))
                    .build();
        }
        return request.createResponseBuilder(HttpStatus.OK)
                .body(usuario)
                .build();
    }

    private HttpResponseMessage handlePost(HttpRequestMessage<Optional<String>> request) throws Exception {
        if (request.getBody().isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Body requerido"))
                    .build();
        }

        Usuario usuario = OBJECT_MAPPER.readValue(request.getBody().get(), Usuario.class);
        if (usuario.getNombre() == null || usuario.getNombre().isBlank() ||
                usuario.getApellidoPaterno() == null || usuario.getApellidoPaterno().isBlank() ||
                usuario.getApellidoMaterno() == null || usuario.getApellidoMaterno().isBlank() ||
                usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "nombre, apellidoPaterno, apellidoMaterno y email son obligatorios"))
                    .build();
        }

        if (usuario.isActivo() == null || !usuario.isActivo()) {
            usuario.setActivo(true);
        }

        if (OracleStore.existsUsuarioByIdentity(
            usuario.getNombre(),
            usuario.getApellidoPaterno(),
            usuario.getApellidoMaterno(),
            usuario.getEmail())) {
            return request.createResponseBuilder(HttpStatus.CONFLICT)
                .body(Map.of("error", "Ya existe un usuario con el mismo nombre, apellidoPaterno, apellidoMaterno y email"))
                .build();
        }

        Usuario creado = OracleStore.saveUsuario(usuario);
        return request.createResponseBuilder(HttpStatus.CREATED)
                .body(creado)
                .build();
    }

    private HttpResponseMessage handlePut(HttpRequestMessage<Optional<String>> request, String id) throws Exception {
        if (id == null || id.isBlank()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "El id es obligatorio en la ruta"))
                    .build();
        }

        if (OracleStore.getUsuario(id) == null) {
            return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Usuario no encontrado", "id", id))
                    .build();
        }

        if (request.getBody().isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Body requerido"))
                    .build();
        }

        Usuario usuario = OBJECT_MAPPER.readValue(request.getBody().get(), Usuario.class);
        if (usuario.getNombre() == null || usuario.getNombre().isBlank() ||
                usuario.getApellidoPaterno() == null || usuario.getApellidoPaterno().isBlank() ||
                usuario.getApellidoMaterno() == null || usuario.getApellidoMaterno().isBlank() ||
                usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "nombre, apellidoPaterno, apellidoMaterno y email son obligatorios"))
                    .build();
        }

        Usuario actualizado = OracleStore.updateUsuario(id, usuario);
        return request.createResponseBuilder(HttpStatus.OK)
                .body(actualizado)
                .build();
    }

    private HttpResponseMessage handleDelete(HttpRequestMessage<Optional<String>> request, String id) {
        if (id == null || id.isBlank()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "El id es obligatorio en la ruta"))
                    .build();
        }

        Usuario usuario = OracleStore.getUsuario(id);
        if (usuario == null) {
            return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Usuario no encontrado", "id", id))
                    .build();
        }

        long prestamosActivos = OracleStore.getPrestamosActivos(id);
        if (prestamosActivos > 0) {
            // Usuario tiene préstamos pendientes, marcar como inactivo
            Usuario usuarioActual = OracleStore.getUsuario(id);
            Usuario actualizado = OracleStore.updateUsuario(id, new Usuario(id, usuarioActual.getNombre(), usuarioActual.getApellidoPaterno(), usuarioActual.getApellidoMaterno(), usuarioActual.getEmail(), false));
            return request.createResponseBuilder(HttpStatus.OK)
                    .body(Map.of(
                            "mensaje", "Usuario marcado como inactivo debido a préstamos pendientes",
                            "usuario", actualizado,
                            "prestamosActivos", prestamosActivos
                    ))
                    .build();
        }

        // Sin préstamos activos, proceder a eliminar
        OracleStore.deleteUsuario(id);
        return request.createResponseBuilder(HttpStatus.OK)
                .body(Map.of("mensaje", "Usuario eliminado exitosamente", "id", id))
                .build();
    }

    private String extractId(HttpRequestMessage<Optional<String>> request, String routeBase) {
        String path = request.getUri().getPath();
        String marker = "/api/" + routeBase;

        int idx = path.indexOf(marker);
        if (idx < 0) {
            return null;
        }

        String tail = path.substring(idx + marker.length());
        if (tail.isBlank() || "/".equals(tail)) {
            return null;
        }

        if (tail.startsWith("/")) {
            tail = tail.substring(1);
        }

        int slash = tail.indexOf('/');
        if (slash >= 0) {
            tail = tail.substring(0, slash);
        }

        return tail.isBlank() ? null : tail;
    }
}
