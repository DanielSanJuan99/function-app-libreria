package cl.duoc.biblioteca.functions.repository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import cl.duoc.biblioteca.functions.exception.RepositoryExceptionHandler;

final class RepositoryUtils {

    private RepositoryUtils() {}

    /**
     * Calcula el siguiente ID disponible de una tabla.
     * @param tableName nombre de la tabla
     * @param idColumn nombre de la columna ID
    * @return {@code long} siguiente ID
     */
    static long nextId(String tableName, String idColumn) {
        String sql = "SELECT NVL(MAX(" + idColumn + "), 0) + 1 FROM " + tableName;
        try (Connection cn = OracleInfra.getConnection();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error obteniendo siguiente id para " + tableName, e);
        }
    }

    /**
     * Genera un RUT numérico desde el ID de usuario.
     * @param idUsuario ID del usuario
    * @return {@code int} RUT calculado
     */
    static int buildRut(long idUsuario) {
        long rut = 10_000_000L + idUsuario;
        if (rut > 99_999_999L) {
            rut = 99_999_999L - (idUsuario % 10_000_000L);
        }
        return (int) rut;
    }

    /**
     * Convierte una cadena a {@link Long}.
     * @param value valor a convertir
    * @return {@link Long} número convertido o {@code null}
     */
    static Long parseLong(String value) {
        try {
            if (value == null) {
                return null;
            }
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * Convierte texto de fecha a {@link Date} SQL.
     * @param value fecha en texto
     * @param defaultDate fecha por defecto
    * @return {@link Date} fecha SQL convertida
     */
    static Date toSqlDate(String value, LocalDate defaultDate) {
        if (value == null || value.isBlank()) {
            return Date.valueOf(defaultDate);
        }
        try {
            return Date.valueOf(LocalDate.parse(value));
        } catch (DateTimeParseException ignored) {
        }
        try {
            return Date.valueOf(LocalDateTime.parse(value).toLocalDate());
        } catch (DateTimeParseException ignored) {
        }
        try {
            return Date.valueOf(OffsetDateTime.parse(value).toLocalDate());
        } catch (DateTimeParseException ignored) {
        }
        return Date.valueOf(defaultDate);
    }

    /**
     * Formatea una fecha SQL en formato ISO.
     * @param date fecha SQL
    * @return {@link String} fecha formateada o {@code null}
     */
    static String formatDate(Date date) {
        return date == null ? null : date.toLocalDate().toString();
    }

    /**
     * Convierte el estado del préstamo a su ID.
     * @param estado estado textual
    * @return {@code int} ID de estado
     */
    static int estadoToId(String estado) {
        if (estado == null || estado.isBlank()) {
            return 1;
        }
        return switch (estado.trim().toUpperCase(Locale.ROOT)) {
            case "DEVUELTO" -> 2;
            case "ATRASADO" -> 3;
            default -> 1;
        };
    }
}
