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

import cl.duoc.biblioteca.functions.domain.Autor;
import cl.duoc.biblioteca.functions.repository.OracleStore;

public class AutoresFunction {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @FunctionName("autores")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE},
                    authLevel = AuthorizationLevel.ANONYMOUS,
                    route = "autores/{id?}")
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context
    ) {
        try {
            String id = extractId(request, "autores");
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
            context.getLogger().log(Level.SEVERE, "Error en autores function", ex);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno", "detalle", ex.getMessage()))
                    .build();
        }
    }

    private HttpResponseMessage handleGet(HttpRequestMessage<Optional<String>> request, String id) {
        if (id == null || id.isBlank()) {
            return request.createResponseBuilder(HttpStatus.OK)
                    .body(OracleStore.getAutores())
                    .build();
        }

        Autor autor = OracleStore.getAutor(id);
        if (autor == null) {
            return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Autor no encontrado", "id", id))
                    .build();
        }

        return request.createResponseBuilder(HttpStatus.OK)
                .body(autor)
                .build();
    }

    private HttpResponseMessage handlePost(HttpRequestMessage<Optional<String>> request) throws Exception {
        if (request.getBody().isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Body requerido"))
                    .build();
        }

        Autor autor = OBJECT_MAPPER.readValue(request.getBody().get(), Autor.class);
        String validationError = validateAutor(autor);
        if (validationError != null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", validationError))
                    .build();
        }

        if (OracleStore.existsAutorByNombre(autor.getNombreAutor())) {
            return request.createResponseBuilder(HttpStatus.CONFLICT)
                .body(Map.of("error", "Ya existe un autor con el mismo nombreAutor"))
                .build();
        }

        Autor creado = OracleStore.saveAutor(autor);
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

        if (OracleStore.getAutor(id) == null) {
            return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Autor no encontrado", "id", id))
                    .build();
        }

        if (request.getBody().isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Body requerido"))
                    .build();
        }

        Autor autor = OBJECT_MAPPER.readValue(request.getBody().get(), Autor.class);
        String validationError = validateAutor(autor);
        if (validationError != null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", validationError))
                    .build();
        }

        Autor actualizado = OracleStore.updateAutor(id, autor);
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

        long librosAsociados = OracleStore.getLibrosPorAutor(id);
        if (librosAsociados > 0) {
            return request.createResponseBuilder(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "error", "No se puede eliminar el autor porque tiene libros asociados",
                            "id", id,
                            "librosAsociados", librosAsociados
                    ))
                    .build();
        }

        Autor eliminado;
        try {
            eliminado = OracleStore.deleteAutor(id);
        } catch (RuntimeException ex) {
            String detalle = ex.getMessage() == null ? "" : ex.getMessage();
            if (detalle.contains("ORA-02292")) {
                return request.createResponseBuilder(HttpStatus.CONFLICT)
                        .body(Map.of(
                                "error", "No se puede eliminar el autor porque tiene libros asociados",
                                "id", id
                        ))
                        .build();
            }
            throw ex;
        }

        if (eliminado == null) {
            return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Autor no encontrado", "id", id))
                    .build();
        }

        return request.createResponseBuilder(HttpStatus.OK)
                .body(Map.of("mensaje", "Autor eliminado", "id", id))
                .build();
    }

    private String validateAutor(Autor autor) {
        if (autor == null) {
            return "Body inválido";
        }

        if (autor.getNombreAutor() == null || autor.getNombreAutor().isBlank()) {
            return "nombreAutor es obligatorio";
        }

        return null;
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
