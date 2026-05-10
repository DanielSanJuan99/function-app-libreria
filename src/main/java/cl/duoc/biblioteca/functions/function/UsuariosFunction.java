package cl.duoc.biblioteca.functions.function;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

import com.azure.messaging.eventgrid.EventGridEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.EventGridTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import cl.duoc.biblioteca.functions.domain.Notificacion;
import cl.duoc.biblioteca.functions.domain.Prestamo;
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

        List<Prestamo> prestamos = OracleStore.getPrestamosByUsuario(id);

        return request.createResponseBuilder(HttpStatus.ACCEPTED)
                .body(Map.of(
                        "mensaje", "Eliminacion solicitada; sera procesada en cascada por evento Usuario.EliminacionSolicitada",
                        "idUsuario", id,
                        "usuario", usuario,
                        "prestamos", prestamos,
                        "totalPrestamos", prestamos.size()
                ))
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

    // =================================================================
    // Consumer Event Grid: cascada de baja de usuario.
    //   Usuario.EliminacionSolicitada -> por cada prestamo del usuario:
    //     1) incrementa COPIAS_DISPONIBLE del libro (devuelve copia)
    //     2) marca el prestamo como CANCELADO y lo elimina
    //   Finalmente: elimina al usuario y registra notificacion de auditoria.
    // Idempotente via EVENTO_PROCESADO.
    // =================================================================

    private static final String EVENT_USUARIO_ELIMINACION = "Usuario.EliminacionSolicitada";

    @FunctionName("usuarioEliminadoConsumer")
    public void onUsuarioEliminado(
            @EventGridTrigger(name = "event") String content,
            final ExecutionContext context) {

        context.getLogger().info("[usuarioEliminadoConsumer] Evento recibido: " + content);

        try {
            List<EventGridEvent> events = EventGridEvent.fromString(content);
            if (events == null || events.isEmpty()) {
                context.getLogger().warning("[usuarioEliminadoConsumer] Payload sin eventos validos");
                return;
            }

            for (EventGridEvent event : events) {
                procesarEventoBaja(event, context);
            }
        } catch (Exception ex) {
            context.getLogger().log(Level.SEVERE, "[usuarioEliminadoConsumer] Error en cascada de baja", ex);
            throw new RuntimeException(ex);
        }
    }

    private void procesarEventoBaja(EventGridEvent event, ExecutionContext context) {
        String eventType = event.getEventType();
        String eventId = event.getId();

        if (!EVENT_USUARIO_ELIMINACION.equals(eventType)) {
            context.getLogger().info(
                    "[usuarioEliminadoConsumer] Evento ignorado (fuera de dominio): " + eventType);
            return;
        }

        if (eventId != null && OracleStore.isEventoProcesado(eventId)) {
            context.getLogger().info("[usuarioEliminadoConsumer] Evento ya procesado: id=" + eventId);
            return;
        }

        Map<String, Object> data = parseEventData(event);
        aplicarCascadaBaja(data, eventId, context);
    }

    private void aplicarCascadaBaja(Map<String, Object> data, String eventId, ExecutionContext context) {
        String idUsuario = stringOrNullEvent(data.get("idUsuario"));
        if (idUsuario == null) {
            idUsuario = stringOrNullEvent(data.get("id"));
        }

        if (idUsuario == null) {
            context.getLogger().warning("[usuarioEliminadoConsumer] Sin idUsuario, abortando cascada");
            return;
        }

        // Snapshot de los prestamos del usuario antes de borrar
        List<Prestamo> prestamos = OracleStore.getPrestamosByUsuario(idUsuario);
        int copiasDevueltas = 0;
        int prestamosBorrados = 0;

        for (Prestamo p : prestamos) {
            String estado = p.getEstado() == null ? "" : p.getEstado().toUpperCase();
            // Solo se devuelve copia al inventario si el prestamo no estaba ya cerrado
            if (!"DEVUELTO".equals(estado) && !"CANCELADO".equals(estado)) {
                int incFilas = OracleStore.incrementarCopiasDisponibles(p.getIdLibro());
                if (incFilas > 0) {
                    copiasDevueltas++;
                }
            }
            int delFilas = OracleStore.cancelarYBorrarPrestamo(p.getId());
            if (delFilas > 0) {
                prestamosBorrados++;
            }
        }

        // Tras la cascada, elimina fisicamente al usuario
        boolean usuarioBorrado = OracleStore.deleteUsuario(idUsuario) != null;

        // Notificacion de auditoria
        Notificacion auditoria = new Notificacion(
                null,
                idUsuario,
                "USUARIO_ELIMINADO",
                "Baja de usuario procesada en cascada",
                String.format(
                        "Usuario %s eliminado. Prestamos cerrados: %d. Copias devueltas al inventario: %d. Eliminacion fisica: %s.",
                        idUsuario, prestamosBorrados, copiasDevueltas, usuarioBorrado ? "OK" : "ya no existia"),
                "PENDIENTE",
                null,
                null);
        OracleStore.saveNotificacion(auditoria);

        context.getLogger().info(String.format(
                "[usuarioEliminadoConsumer] Cascada finalizada idUsuario=%s prestamos=%d copias=%d usuarioBorrado=%s",
                idUsuario, prestamosBorrados, copiasDevueltas, usuarioBorrado));

        OracleStore.marcarEventoProcesado(eventId, EVENT_USUARIO_ELIMINACION,
                "idUsuario=" + idUsuario + " prestamos=" + prestamosBorrados + " copias=" + copiasDevueltas);
    }

    private Map<String, Object> parseEventData(EventGridEvent event) {
        if (event.getData() == null) {
            return Map.of();
        }
        try {
            return OBJECT_MAPPER.readValue(event.getData().toBytes(), new TypeReference<>() {
            });
        } catch (Exception ex) {
            return Map.of("rawData", event.getData().toString());
        }
    }

    private String stringOrNullEvent(Object value) {
        if (value == null) return null;
        String text = value.toString();
        return text.isBlank() ? null : text;
    }
}
