package cl.duoc.biblioteca.functions.exception;

import java.sql.SQLException;

public final class RepositoryExceptionHandler {

    private RepositoryExceptionHandler() {}

    /**
     * Traduce un error SQL a excepción de runtime con detalle.
     * @param message contexto del error
     * @param e excepción SQL original
    * @return {@link RuntimeException} excepción runtime con detalle
     */
    public static RuntimeException sqlException(String message, SQLException e) {
        String detail = String.format(
                "%s (SQLState=%s, ErrorCode=%d): %s",
                message,
                e.getSQLState(),
                e.getErrorCode(),
                e.getMessage()
        );
        return new RuntimeException(detail, e);
    }
}
