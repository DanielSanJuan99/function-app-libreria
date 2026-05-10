package cl.duoc.biblioteca.functions.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import cl.duoc.biblioteca.functions.exception.RepositoryExceptionHandler;

/**
 * Persistencia de la tabla EVENTO_PROCESADO usada para garantizar
 * idempotencia frente a reintentos de Event Grid.
 */
final class EventoProcesadoRepository {

    private EventoProcesadoRepository() {}

    /**
     * Indica si un evento ya fue procesado anteriormente.
     * @param eventId identificador unico del evento (event.id de Event Grid)
     * @return {@code true} si existe registro previo
     */
    static boolean isProcesado(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return false;
        }
        String sql = "SELECT 1 FROM EVENTO_PROCESADO WHERE EVENT_ID = ?";
        try (Connection cn = OracleInfra.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error consultando evento procesado", e);
        }
    }

    /**
     * Marca un evento como procesado. Si ya existe (PK violation), se ignora.
     * @param eventId identificador unico del evento
     * @param eventType tipo del evento (Prestamo.Creado, Usuario.EliminacionSolicitada, etc.)
     * @param detalle detalle opcional (idUsuario, idPrestamo, etc.) max 2000 caracteres
     */
    static void marcarProcesado(String eventId, String eventType, String detalle) {
        if (eventId == null || eventId.isBlank()) {
            return;
        }
        String sql = """
                INSERT INTO EVENTO_PROCESADO (EVENT_ID, EVENT_TYPE, DETALLE)
                SELECT ?, ?, ? FROM DUAL
                 WHERE NOT EXISTS (SELECT 1 FROM EVENTO_PROCESADO WHERE EVENT_ID = ?)
                """;
        try (Connection cn = OracleInfra.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            ps.setString(2, eventType == null ? "UNKNOWN" : eventType);
            ps.setString(3, truncate(detalle));
            ps.setString(4, eventId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error marcando evento como procesado", e);
        }
    }

    private static String truncate(String value) {
        if (value == null) return null;
        return value.length() > 2000 ? value.substring(0, 2000) : value;
    }
}
