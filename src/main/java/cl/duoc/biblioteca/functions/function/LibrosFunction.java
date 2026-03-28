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

import cl.duoc.biblioteca.functions.domain.Libro;
import cl.duoc.biblioteca.functions.repository.OracleStore;

public class LibrosFunction {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @FunctionName("libros")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE},
                    authLevel = AuthorizationLevel.ANONYMOUS,
                    route = "libros/{id?}")
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context
    ) {
        try {
            String id = extractId(request, "libros");
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
            context.getLogger().log(Level.SEVERE, "Error en libros function", ex);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno", "detalle", ex.getMessage()))
                    .build();
        }
    }

    private HttpResponseMessage handleGet(HttpRequestMessage<Optional<String>> request, String id) {
        if (id == null || id.isBlank()) {
            return request.createResponseBuilder(HttpStatus.OK)
                    .body(OracleStore.getLibros())
                    .build();
        }

        Libro libro = OracleStore.getLibro(id);
        if (libro == null) {
            return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Libro no encontrado", "id", id))
                    .build();
        }

        return request.createResponseBuilder(HttpStatus.OK)
                .body(libro)
                .build();
    }

    private HttpResponseMessage handlePost(HttpRequestMessage<Optional<String>> request) throws Exception {
        if (request.getBody().isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Body requerido"))
                    .build();
        }

        Libro libro = OBJECT_MAPPER.readValue(request.getBody().get(), Libro.class);
        String validationError = validateLibro(libro);
        if (validationError != null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", validationError))
                    .build();
        }

        if (!OracleStore.autorExiste(libro.getIdAutor())) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "No existe autor para el libro", "idAutor", libro.getIdAutor()))
                    .build();
        }

        if (OracleStore.existsLibroByIsbnTitulo(libro.getIsbn(), libro.getTitulo())) {
            return request.createResponseBuilder(HttpStatus.CONFLICT)
                .body(Map.of("error", "Ya existe un libro con el mismo isbn y titulo"))
                .build();
        }

        Libro creado = OracleStore.saveLibro(libro);
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

        if (OracleStore.getLibro(id) == null) {
            return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Libro no encontrado", "id", id))
                    .build();
        }

        if (request.getBody().isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Body requerido"))
                    .build();
        }

        Libro libro = OBJECT_MAPPER.readValue(request.getBody().get(), Libro.class);
        String validationError = validateLibro(libro);
        if (validationError != null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", validationError))
                    .build();
        }

        if (!OracleStore.autorExiste(libro.getIdAutor())) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "No existe autor para el libro", "idAutor", libro.getIdAutor()))
                    .build();
        }

        Libro actualizado = OracleStore.updateLibro(id, libro);
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

        long prestamosAsociados = OracleStore.getPrestamosPorLibro(id);
        if (prestamosAsociados > 0) {
            return request.createResponseBuilder(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "error", "No se puede eliminar el libro porque tiene préstamos asociados",
                            "id", id,
                            "prestamosAsociados", prestamosAsociados
                    ))
                    .build();
        }

        Libro eliminado;
        try {
            eliminado = OracleStore.deleteLibro(id);
        } catch (RuntimeException ex) {
            String detalle = ex.getMessage() == null ? "" : ex.getMessage();
            if (detalle.contains("ORA-02292")) {
                return request.createResponseBuilder(HttpStatus.CONFLICT)
                        .body(Map.of(
                                "error", "No se puede eliminar el libro porque tiene préstamos asociados",
                                "id", id
                        ))
                        .build();
            }
            throw ex;
        }

        if (eliminado == null) {
            return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Libro no encontrado", "id", id))
                    .build();
        }

        return request.createResponseBuilder(HttpStatus.OK)
                .body(Map.of("mensaje", "Libro eliminado", "id", id))
                .build();
    }

    private String validateLibro(Libro libro) {
        if (libro == null) {
            return "Body inválido";
        }
        if (libro.getIsbn() == null || libro.getIsbn().isBlank()) {
            return "isbn es obligatorio";
        }
        if (libro.getTitulo() == null || libro.getTitulo().isBlank()) {
            return "titulo es obligatorio";
        }
        if (libro.getAnioPublicacion() == null) {
            return "anioPublicacion es obligatorio";
        }
        if (libro.getCopiasTotales() == null) {
            return "copiasTotales es obligatorio";
        }
        if (libro.getCopiasDisponible() == null) {
            return "copiasDisponible es obligatorio";
        }
        if (libro.getIdAutor() == null || libro.getIdAutor().isBlank()) {
            return "idAutor es obligatorio";
        }
        if (libro.getCopiasTotales() < 0 || libro.getCopiasDisponible() < 0) {
            return "copiasTotales y copiasDisponible deben ser >= 0";
        }
        if (libro.getCopiasDisponible() > libro.getCopiasTotales()) {
            return "copiasDisponible no puede ser mayor que copiasTotales";
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
