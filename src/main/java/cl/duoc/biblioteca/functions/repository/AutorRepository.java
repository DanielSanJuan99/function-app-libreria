package cl.duoc.biblioteca.functions.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import cl.duoc.biblioteca.functions.domain.Autor;
import cl.duoc.biblioteca.functions.exception.RepositoryExceptionHandler;

final class AutorRepository {

    private AutorRepository() {}

    /**
     * Lista todos los autores.
    * @return {@link List<Autor>} lista de autores
     */
    static List<Autor> getAutores() {
        String sql = """
                SELECT ID_AUTOR, NOMBRE_AUTOR
                FROM AUTOR
                ORDER BY ID_AUTOR
                """;
        try (Connection cn = OracleInfra.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            java.util.ArrayList<Autor> result = new java.util.ArrayList<>();
            while (rs.next()) {
                result.add(new Autor(
                        String.valueOf(rs.getLong("ID_AUTOR")),
                        rs.getString("NOMBRE_AUTOR")
                ));
            }
            return result;
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error consultando autores", e);
        }
    }

    /**
     * Busca un autor por ID.
     * @param id identificador del autor
    * @return {@link Autor} autor encontrado o {@code null}
     */
    static Autor getAutor(String id) {
        Long idNum = RepositoryUtils.parseLong(id);
        if (idNum == null) {
            return null;
        }

        String sql = """
                SELECT ID_AUTOR, NOMBRE_AUTOR
                FROM AUTOR
                WHERE ID_AUTOR = ?
                """;
        try (Connection cn = OracleInfra.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, idNum);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                return new Autor(
                        String.valueOf(rs.getLong("ID_AUTOR")),
                        rs.getString("NOMBRE_AUTOR")
                );
            }
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error consultando autor por id", e);
        }
    }

    /**
     * Crea un nuevo autor.
     * @param autor datos del autor
    * @return {@link Autor} autor creado
     */
    static Autor saveAutor(Autor autor) {
        if (existsAutorByNombre(autor.getNombreAutor())) {
            throw new IllegalStateException("Ya existe un autor con el mismo nombreAutor");
        }

        long id = RepositoryUtils.nextId("AUTOR", "ID_AUTOR");

        String sql = """
                INSERT INTO AUTOR
                (ID_AUTOR, NOMBRE_AUTOR)
                VALUES (?, ?)
                """;

        try (Connection cn = OracleInfra.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.setString(2, autor.getNombreAutor());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error creando autor", e);
        }

        return getAutor(String.valueOf(id));
    }

    /**
     * Actualiza un autor existente.
     * @param id identificador del autor
     * @param autor datos actualizados
    * @return {@link Autor} autor actualizado o {@code null}
     */
    static Autor updateAutor(String id, Autor autor) {
        Long idNum = RepositoryUtils.parseLong(id);
        if (idNum == null) {
            return null;
        }

        String sql = "UPDATE AUTOR SET NOMBRE_AUTOR = ? WHERE ID_AUTOR = ?";
        try (Connection cn = OracleInfra.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, autor.getNombreAutor());
            ps.setLong(2, idNum);

            int updated = ps.executeUpdate();
            if (updated == 0) {
                return null;
            }
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error actualizando autor", e);
        }

        return getAutor(id);
    }

    /**
     * Elimina un autor por ID.
     * @param id identificador del autor
    * @return {@link Autor} autor eliminado o {@code null}
     */
    static Autor deleteAutor(String id) {
        Autor autorExistente = getAutor(id);
        if (autorExistente == null) {
            return null;
        }

        Long idNum = RepositoryUtils.parseLong(id);
        String sql = "DELETE FROM AUTOR WHERE ID_AUTOR = ?";
        try (Connection cn = OracleInfra.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, idNum);
            ps.executeUpdate();
            return autorExistente;
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error eliminando autor", e);
        }
    }

    /**
     * Cuenta libros asociados a un autor.
     * @param idAutor identificador del autor
    * @return {@code long} cantidad de libros
     */
    static long getLibrosPorAutor(String idAutor) {
        Long idNum = RepositoryUtils.parseLong(idAutor);
        if (idNum == null) {
            return 0;
        }

        String sql = "SELECT COUNT(*) FROM LIBRO WHERE ID_AUTOR = ?";
        try (Connection cn = OracleInfra.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, idNum);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error contando libros por autor", e);
        }
    }

    /**
     * Verifica existencia de autor por ID.
     * @param idAutor identificador del autor
    * @return {@code boolean} {@code true} si existe
     */
    static boolean autorExiste(String idAutor) {
        Long idNum = RepositoryUtils.parseLong(idAutor);
        if (idNum == null) {
            return false;
        }

        String sql = "SELECT 1 FROM AUTOR WHERE ID_AUTOR = ?";
        try (Connection cn = OracleInfra.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, idNum);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error validando existencia de autor", e);
        }
    }

    /**
     * Verifica duplicidad de autor por nombre.
     * @param nombreAutor nombre del autor
    * @return {@code boolean} {@code true} si ya existe
     */
    static boolean existsAutorByNombre(String nombreAutor) {
        String sql = """
                SELECT 1
                FROM AUTOR
                WHERE UPPER(TRIM(NOMBRE_AUTOR)) = UPPER(TRIM(?))
                """;

        try (Connection cn = OracleInfra.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, nombreAutor);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error validando duplicidad de autor", e);
        }
    }
}
