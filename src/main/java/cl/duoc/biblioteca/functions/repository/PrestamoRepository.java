package cl.duoc.biblioteca.functions.repository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import cl.duoc.biblioteca.functions.domain.Prestamo;
import cl.duoc.biblioteca.functions.exception.RepositoryExceptionHandler;

final class PrestamoRepository {

    private PrestamoRepository() {}

    /**
     * Lista todos los préstamos.
    * @return {@link List<Prestamo>} lista de préstamos
     */
    static List<Prestamo> getPrestamos() {
        String sql = """
                SELECT p.ID_PRESTAMO, p.ID_USUARIO, p.LIBRO_ID_LIBRO,
                       p.FECHA_PRESTAMO, p.FECHA_DEVOLUCION_ESPERADA, te.NOMBRE_ESTADO
                FROM PRESTAMO p
                JOIN TIPO_ESTADO te ON te.ID_ESTADO = p.ID_ESTADO
                ORDER BY p.ID_PRESTAMO
                """;
        try (Connection cn = OracleInfra.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            java.util.ArrayList<Prestamo> result = new java.util.ArrayList<>();
            while (rs.next()) {
                result.add(new Prestamo(
                        String.valueOf(rs.getLong("ID_PRESTAMO")),
                        String.valueOf(rs.getLong("ID_USUARIO")),
                        String.valueOf(rs.getLong("LIBRO_ID_LIBRO")),
                        RepositoryUtils.formatDate(rs.getDate("FECHA_PRESTAMO")),
                        RepositoryUtils.formatDate(rs.getDate("FECHA_DEVOLUCION_ESPERADA")),
                        rs.getString("NOMBRE_ESTADO")
                ));
            }
            return result;
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error consultando préstamos", e);
        }
    }

    /**
     * Busca un préstamo por ID.
     * @param id identificador del préstamo
    * @return {@link Prestamo} préstamo encontrado o {@code null}
     */
    static Prestamo getPrestamo(String id) {
        Long idNum = RepositoryUtils.parseLong(id);
        if (idNum == null) {
            return null;
        }

        String sql = """
                SELECT p.ID_PRESTAMO, p.ID_USUARIO, p.LIBRO_ID_LIBRO,
                       p.FECHA_PRESTAMO, p.FECHA_DEVOLUCION_ESPERADA, te.NOMBRE_ESTADO
                FROM PRESTAMO p
                JOIN TIPO_ESTADO te ON te.ID_ESTADO = p.ID_ESTADO
                WHERE p.ID_PRESTAMO = ?
                """;
        try (Connection cn = OracleInfra.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, idNum);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new Prestamo(
                        String.valueOf(rs.getLong("ID_PRESTAMO")),
                        String.valueOf(rs.getLong("ID_USUARIO")),
                        String.valueOf(rs.getLong("LIBRO_ID_LIBRO")),
                        RepositoryUtils.formatDate(rs.getDate("FECHA_PRESTAMO")),
                        RepositoryUtils.formatDate(rs.getDate("FECHA_DEVOLUCION_ESPERADA")),
                        rs.getString("NOMBRE_ESTADO")
                );
            }
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error consultando préstamo por id", e);
        }
    }

    /**
     * Crea un nuevo préstamo.
     * @param prestamo datos del préstamo
    * @return {@link Prestamo} préstamo creado o {@code null}
     */
    static Prestamo savePrestamo(Prestamo prestamo) {
        Long idUsuario = RepositoryUtils.parseLong(prestamo.getIdUsuario());
        Long idLibro = RepositoryUtils.parseLong(prestamo.getIdLibro());
        if (idUsuario == null || idLibro == null) {
            return null;
        }

        if (existsPrestamoByUsuarioLibro(prestamo.getIdUsuario(), prestamo.getIdLibro())) {
            throw new IllegalStateException("Ya existe un préstamo para el mismo idUsuario e idLibro");
        }

        long id = RepositoryUtils.nextId("PRESTAMO", "ID_PRESTAMO");
        Date fechaPrestamo = RepositoryUtils.toSqlDate(prestamo.getFechaPrestamo(), LocalDate.now());
        Date fechaEsperada = RepositoryUtils.toSqlDate(prestamo.getFechaDevolucion(), LocalDate.now().plusDays(14));

        int estadoId = RepositoryUtils.estadoToId(prestamo.getEstado());

        String sql = """
                INSERT INTO PRESTAMO
                (ID_PRESTAMO, ID_USUARIO, LIBRO_ID_LIBRO, FECHA_PRESTAMO, FECHA_DEVOLUCION_ESPERADA, FECHA_DEVOLUCION_REAL, ID_ESTADO)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection cn = OracleInfra.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.setLong(2, idUsuario);
            ps.setLong(3, idLibro);
            ps.setDate(4, fechaPrestamo);
            ps.setDate(5, fechaEsperada);

            if (estadoId == 2 && prestamo.getFechaDevolucion() != null && !prestamo.getFechaDevolucion().isBlank()) {
                ps.setDate(6, RepositoryUtils.toSqlDate(prestamo.getFechaDevolucion(), LocalDate.now()));
            } else {
                ps.setNull(6, java.sql.Types.DATE);
            }
            ps.setInt(7, estadoId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error creando préstamo", e);
        }

        return getPrestamo(String.valueOf(id));
    }

    /**
     * Actualiza un préstamo existente.
     * @param id identificador del préstamo
     * @param prestamo datos actualizados
    * @return {@link Prestamo} préstamo actualizado o {@code null}
     */
    static Prestamo updatePrestamo(String id, Prestamo prestamo) {
        Long idNum = RepositoryUtils.parseLong(id);
        Long idUsuario = RepositoryUtils.parseLong(prestamo.getIdUsuario());
        Long idLibro = RepositoryUtils.parseLong(prestamo.getIdLibro());
        if (idNum == null || idUsuario == null || idLibro == null) {
            return null;
        }

        Date fechaPrestamo = RepositoryUtils.toSqlDate(prestamo.getFechaPrestamo(), LocalDate.now());
        Date fechaEsperada = RepositoryUtils.toSqlDate(prestamo.getFechaDevolucion(), LocalDate.now().plusDays(14));
        int estadoId = RepositoryUtils.estadoToId(prestamo.getEstado());

        String sql = """
                UPDATE PRESTAMO
                   SET ID_USUARIO = ?,
                       LIBRO_ID_LIBRO = ?,
                       FECHA_PRESTAMO = ?,
                       FECHA_DEVOLUCION_ESPERADA = ?,
                       FECHA_DEVOLUCION_REAL = ?,
                       ID_ESTADO = ?
                 WHERE ID_PRESTAMO = ?
                """;
        try (Connection cn = OracleInfra.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, idUsuario);
            ps.setLong(2, idLibro);
            ps.setDate(3, fechaPrestamo);
            ps.setDate(4, fechaEsperada);

            if (estadoId == 2 && prestamo.getFechaDevolucion() != null && !prestamo.getFechaDevolucion().isBlank()) {
                ps.setDate(5, RepositoryUtils.toSqlDate(prestamo.getFechaDevolucion(), LocalDate.now()));
            } else {
                ps.setNull(5, java.sql.Types.DATE);
            }
            ps.setInt(6, estadoId);
            ps.setLong(7, idNum);

            int updated = ps.executeUpdate();
            if (updated == 0) {
                return null;
            }
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error actualizando préstamo", e);
        }

        return getPrestamo(id);
    }

    /**
     * Elimina un préstamo por ID.
     * @param id identificador del préstamo
    * @return {@link Prestamo} préstamo eliminado o {@code null}
     */
    static Prestamo deletePrestamo(String id) {
        Prestamo previo = getPrestamo(id);
        if (previo == null) {
            return null;
        }

        Long idNum = RepositoryUtils.parseLong(id);
        String sql = "DELETE FROM PRESTAMO WHERE ID_PRESTAMO = ?";
        try (Connection cn = OracleInfra.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, idNum);
            ps.executeUpdate();
            return previo;
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error eliminando préstamo", e);
        }
    }

    /**
     * Cuenta préstamos asociados a un libro.
     * @param idLibro identificador del libro
    * @return {@code long} cantidad de préstamos
     */
    static long getPrestamosPorLibro(String idLibro) {
        Long idNum = RepositoryUtils.parseLong(idLibro);
        if (idNum == null) {
            return 0;
        }

        String sql = "SELECT COUNT(*) FROM PRESTAMO WHERE LIBRO_ID_LIBRO = ?";
        try (Connection cn = OracleInfra.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, idNum);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error contando préstamos por libro", e);
        }
    }

    /**
     * Verifica duplicidad de préstamo por usuario y libro.
     * @param idUsuario identificador del usuario
     * @param idLibro identificador del libro
    * @return {@code boolean} {@code true} si ya existe
     */
    static boolean existsPrestamoByUsuarioLibro(String idUsuario, String idLibro) {
        Long idUsuarioNum = RepositoryUtils.parseLong(idUsuario);
        Long idLibroNum = RepositoryUtils.parseLong(idLibro);
        if (idUsuarioNum == null || idLibroNum == null) {
            return false;
        }

        String sql = """
                SELECT 1
                FROM PRESTAMO
                WHERE ID_USUARIO = ?
                  AND LIBRO_ID_LIBRO = ?
                """;

        try (Connection cn = OracleInfra.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, idUsuarioNum);
            ps.setLong(2, idLibroNum);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error validando duplicidad de préstamo", e);
        }
    }
}
