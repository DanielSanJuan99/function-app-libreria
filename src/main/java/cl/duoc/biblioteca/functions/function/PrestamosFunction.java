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

import cl.duoc.biblioteca.functions.domain.Prestamo;
import cl.duoc.biblioteca.functions.repository.OracleStore;

public class PrestamosFunction {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @FunctionName("prestamos")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE},
                    authLevel = AuthorizationLevel.ANONYMOUS,
                    route = "prestamos/{id?}")
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context
    ) {
        try {
            String id = extractId(request, "prestamos");
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
            context.getLogger().log(Level.SEVERE, "Error en prestamos function", ex);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno", "detalle", ex.getMessage()))
                    .build();
        }
    }

    private HttpResponseMessage handleGet(HttpRequestMessage<Optional<String>> request, String id) {
        if (id == null || id.isBlank()) {
            return request.createResponseBuilder(HttpStatus.OK)
                    .body(OracleStore.getPrestamos())
                    .build();
        }

        Prestamo prestamo = OracleStore.getPrestamo(id);
        if (prestamo == null) {
            return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Préstamo no encontrado", "id", id))
                    .build();
        }
        return request.createResponseBuilder(HttpStatus.OK)
                .body(prestamo)
                .build();
    }

    private HttpResponseMessage handlePost(HttpRequestMessage<Optional<String>> request) throws Exception {
        if (request.getBody().isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Body requerido"))
                    .build();
        }

        Prestamo prestamo = OBJECT_MAPPER.readValue(request.getBody().get(), Prestamo.class);
        if (prestamo.getIdUsuario() == null || prestamo.getIdUsuario().isBlank() ||
                prestamo.getIdLibro() == null || prestamo.getIdLibro().isBlank()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "idUsuario e idLibro son obligatorios"))
                    .build();
        }

        if (!OracleStore.usuarioExiste(prestamo.getIdUsuario())) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "No existe usuario para el préstamo", "idUsuario", prestamo.getIdUsuario()))
                    .build();
        }

        if (!OracleStore.libroExiste(prestamo.getIdLibro())) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "No existe libro para el préstamo", "idLibro", prestamo.getIdLibro()))
                .build();
        }

        if (OracleStore.existsPrestamoByUsuarioLibro(prestamo.getIdUsuario(), prestamo.getIdLibro())) {
            return request.createResponseBuilder(HttpStatus.CONFLICT)
                .body(Map.of("error", "Ya existe un préstamo para el mismo idUsuario e idLibro"))
                .build();
        }

        Prestamo creado = OracleStore.savePrestamo(prestamo);
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

        Prestamo prestamoActual = OracleStore.getPrestamo(id);
        if (prestamoActual == null) {
            return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Préstamo no encontrado", "id", id))
                    .build();
        }

        if (request.getBody().isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Body requerido"))
                    .build();
        }

        Prestamo prestamo = OBJECT_MAPPER.readValue(request.getBody().get(), Prestamo.class);
        if (prestamo.getIdUsuario() == null || prestamo.getIdUsuario().isBlank() ||
                prestamo.getIdLibro() == null || prestamo.getIdLibro().isBlank()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "idUsuario e idLibro son obligatorios"))
                    .build();
        }

        if (!OracleStore.usuarioExiste(prestamo.getIdUsuario())) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "No existe usuario para el préstamo", "idUsuario", prestamo.getIdUsuario()))
                    .build();
        }

        if (!OracleStore.libroExiste(prestamo.getIdLibro())) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "No existe libro para el préstamo", "idLibro", prestamo.getIdLibro()))
                .build();
        }

        Prestamo actualizado = OracleStore.updatePrestamo(id, prestamo);
        
        // Limpieza automática: si el préstamo se marcó como devuelto (estado=2) y el usuario está inactivo sin préstamos, eliminarlo
        if ("DEVUELTO".equalsIgnoreCase(prestamo.getEstado())) {
            OracleStore.deleteUsuarioIfInactive(prestamo.getIdUsuario());
        }
        
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

        Prestamo eliminado = OracleStore.deletePrestamo(id);
        if (eliminado == null) {
            return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Préstamo no encontrado", "id", id))
                    .build();
        }

        return request.createResponseBuilder(HttpStatus.OK)
                .body(Map.of("mensaje", "Préstamo eliminado", "id", id))
                .build();
    }

    private String extractId(HttpRequestMessage<Optional<String>> request, String routeBase) {
        String path = request.getUri().getPath();
        String marker = "/" + routeBase;

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
