package cl.duoc.biblioteca.functions.function;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.azure.messaging.eventgrid.EventGridEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.EventGridTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;

import cl.duoc.biblioteca.functions.domain.Notificacion;
import cl.duoc.biblioteca.functions.repository.OracleStore;

public class NotificacionConsumerFunction {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String EVENT_PRESTAMO_CREADO = "Prestamo.Creado";
    private static final String EVENT_PRESTAMO_DEVUELTO = "Prestamo.Devuelto";
    private static final String EVENT_USUARIO_ELIMINACION = "Usuario.EliminacionSolicitada";

    @FunctionName("notificacionConsumer")
    public void run(
            @EventGridTrigger(name = "event") String content,
            final ExecutionContext context) {

        context.getLogger().info("Evento Event Grid recibido: " + content);

        try {
            List<EventGridEvent> events = EventGridEvent.fromString(content);
            if (events == null || events.isEmpty()) {
                context.getLogger().warning("Payload sin eventos válidos");
                return;
            }

            for (EventGridEvent event : events) {
                procesarEvento(event, context);
            }
        } catch (Exception ex) {
            context.getLogger().log(Level.SEVERE, "Error procesando evento Event Grid", ex);
            throw new RuntimeException(ex);
        }
    }

    private void procesarEvento(EventGridEvent event, ExecutionContext context) {
        String eventType = event.getEventType();
        Map<String, Object> data = parseData(event);

        Notificacion notificacion = switch (eventType) {
            case EVENT_PRESTAMO_CREADO -> buildNotificacionPrestamoCreado(data);
            case EVENT_PRESTAMO_DEVUELTO -> buildNotificacionPrestamoDevuelto(data);
            case EVENT_USUARIO_ELIMINACION -> buildNotificacionUsuarioEliminacionSolicitada(data);
            default -> null;
        };

        if (notificacion == null) {
            context.getLogger().info("Event Type sin handler asociado: " + eventType);
            return;
        }

        Notificacion guardada = OracleStore.saveNotificacion(notificacion);
        context.getLogger().info(String.format(
                "Notificación creada id=%s tipo=%s idUsuario=%s",
                guardada.getId(), guardada.getTipo(), guardada.getIdUsuario()));
    }

    private Map<String, Object> parseData(EventGridEvent event) {
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

    private Notificacion buildNotificacionPrestamoCreado(Map<String, Object> data) {
        String idUsuario = stringOrNull(data.get("idUsuario"));
        String idLibro = stringOrNull(data.get("idLibro"));
        String idPrestamo = stringOrNull(data.get("id"));
        String fechaDevolucion = stringOrNull(data.get("fechaDevolucion"));

        String asunto = "Préstamo registrado";
        String cuerpo = String.format(
                "Hola, su préstamo #%s del libro %s ha sido registrado correctamente. " +
                "Recuerde devolverlo antes del %s.",
                idPrestamo == null ? "(sin id)" : idPrestamo,
                idLibro == null ? "(sin id)" : idLibro,
                fechaDevolucion == null ? "(sin fecha)" : fechaDevolucion);

        return new Notificacion(null, idUsuario, "PRESTAMO_CREADO", asunto, cuerpo, "PENDIENTE", null, null);
    }

    private Notificacion buildNotificacionPrestamoDevuelto(Map<String, Object> data) {
        String idUsuario = stringOrNull(data.get("idUsuario"));
        String idLibro = stringOrNull(data.get("idLibro"));
        String idPrestamo = stringOrNull(data.get("id"));

        String asunto = "Devolución confirmada";
        String cuerpo = String.format(
                "Gracias por devolver el libro %s. Su préstamo #%s ha sido cerrado.",
                idLibro == null ? "(sin id)" : idLibro,
                idPrestamo == null ? "(sin id)" : idPrestamo);

        return new Notificacion(null, idUsuario, "PRESTAMO_DEVUELTO", asunto, cuerpo, "PENDIENTE", null, null);
    }

    private Notificacion buildNotificacionUsuarioEliminacionSolicitada(Map<String, Object> data) {
        String idUsuario = stringOrNull(data.get("idUsuario"));
        if (idUsuario == null) {
            idUsuario = stringOrNull(data.get("id"));
        }
        Object totalPrestamos = data.get("totalPrestamos");

        String asunto = "Solicitud de baja de usuario recibida";
        String cuerpo = String.format(
                "Aviso al administrador: se recibio solicitud de baja para el usuario %s. " +
                "Prestamos asociados: %s. La cascada (devolucion de copias al inventario y " +
                "eliminacion de prestamos) sera ejecutada por el consumer de negocio.",
                idUsuario == null ? "(sin id)" : idUsuario,
                totalPrestamos == null ? "?" : totalPrestamos.toString());

        return new Notificacion(null, idUsuario, "USUARIO_ELIMINACION_SOLICITADA", asunto, cuerpo, "PENDIENTE", null, null);
    }

    private String stringOrNull(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString();
        return text.isBlank() ? null : text;
    }
}
