package cl.duoc.biblioteca.functions.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import cl.duoc.biblioteca.functions.domain.Notificacion;
import cl.duoc.biblioteca.functions.exception.RepositoryExceptionHandler;

final class NotificacionRepository {

    private NotificacionRepository() {}

    /**
     * Persiste una notificación generada por el consumer de Event Grid.
     * @param notificacion datos de la notificación
     * @return {@link Notificacion} notificación guardada con ID asignado
     */
    static Notificacion saveNotificacion(Notificacion notificacion) {
        String sql = """
                INSERT INTO NOTIFICACIONES
                (ID_USUARIO, TIPO, ASUNTO, CUERPO, ESTADO)
                VALUES (?, ?, ?, ?, ?)
                """;

        String estado = (notificacion.getEstado() == null || notificacion.getEstado().isBlank())
                ? "PENDIENTE" : notificacion.getEstado();

        try (Connection cn = OracleInfra.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, new String[] {"ID"})) {

            ps.setString(1, notificacion.getIdUsuario());
            ps.setString(2, notificacion.getTipo());
            ps.setString(3, notificacion.getAsunto());
            ps.setString(4, notificacion.getCuerpo());
            ps.setString(5, estado);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return getNotificacion(String.valueOf(keys.getLong(1)));
                }
            }
            return notificacion;
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error guardando notificación", e);
        }
    }

    /**
     * Lista todas las notificaciones ordenadas por fecha descendente.
     * @return {@link List<Notificacion>} listado completo
     */
    static List<Notificacion> getNotificaciones() {
        String sql = """
                SELECT ID, ID_USUARIO, TIPO, ASUNTO, CUERPO, ESTADO, FECHA_CREACION, FECHA_ENVIO
                FROM NOTIFICACIONES
                ORDER BY FECHA_CREACION DESC, ID DESC
                """;

        try (Connection cn = OracleInfra.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Notificacion> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error consultando notificaciones", e);
        }
    }

    /**
     * Lista notificaciones de un usuario específico.
     * @param idUsuario identificador del usuario
     * @return {@link List<Notificacion>} notificaciones del usuario
     */
    static List<Notificacion> getNotificacionesByUsuario(String idUsuario) {
        String sql = """
                SELECT ID, ID_USUARIO, TIPO, ASUNTO, CUERPO, ESTADO, FECHA_CREACION, FECHA_ENVIO
                FROM NOTIFICACIONES
                WHERE ID_USUARIO = ?
                ORDER BY FECHA_CREACION DESC, ID DESC
                """;

        try (Connection cn = OracleInfra.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                List<Notificacion> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error consultando notificaciones del usuario", e);
        }
    }

    /**
     * Recupera una notificación por su ID.
     * @param id identificador de la notificación
     * @return {@link Notificacion} encontrada o {@code null}
     */
    static Notificacion getNotificacion(String id) {
        Long idNum = RepositoryUtils.parseLong(id);
        if (idNum == null) {
            return null;
        }

        String sql = """
                SELECT ID, ID_USUARIO, TIPO, ASUNTO, CUERPO, ESTADO, FECHA_CREACION, FECHA_ENVIO
                FROM NOTIFICACIONES
                WHERE ID = ?
                """;

        try (Connection cn = OracleInfra.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, idNum);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapRow(rs);
            }
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error consultando notificación por id", e);
        }
    }

    private static Notificacion mapRow(ResultSet rs) throws SQLException {
        Timestamp fechaCreacion = rs.getTimestamp("FECHA_CREACION");
        Timestamp fechaEnvio = rs.getTimestamp("FECHA_ENVIO");

        return new Notificacion(
                String.valueOf(rs.getLong("ID")),
                rs.getString("ID_USUARIO"),
                rs.getString("TIPO"),
                rs.getString("ASUNTO"),
                rs.getString("CUERPO"),
                rs.getString("ESTADO"),
                fechaCreacion == null ? null : fechaCreacion.toLocalDateTime().toString(),
                fechaEnvio == null ? null : fechaEnvio.toLocalDateTime().toString()
        );
    }
}
