package cl.duoc.biblioteca.functions.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import cl.duoc.biblioteca.functions.domain.Libro;
import cl.duoc.biblioteca.functions.exception.RepositoryExceptionHandler;

final class LibroRepository {

    private LibroRepository() {}

    /**
     * Lista todos los libros.
    * @return {@link List<Libro>} lista de libros
     */
    static List<Libro> getLibros() {
        String sql = """
                SELECT ID_LIBRO, ISBN, TITULO, ANIO_PUBLICACION, COPIAS_TOTALES, COPIAS_DISPONIBLE, ID_AUTOR
                FROM LIBRO
                ORDER BY ID_LIBRO
                """;
        try (Connection cn = OracleInfra.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            java.util.ArrayList<Libro> result = new java.util.ArrayList<>();
            while (rs.next()) {
                result.add(new Libro(
                        String.valueOf(rs.getLong("ID_LIBRO")),
                        rs.getString("ISBN"),
                        rs.getString("TITULO"),
                        rs.getInt("ANIO_PUBLICACION"),
                        rs.getInt("COPIAS_TOTALES"),
                        rs.getInt("COPIAS_DISPONIBLE"),
                        String.valueOf(rs.getLong("ID_AUTOR"))
                ));
            }
            return result;
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error consultando libros", e);
        }
    }

    /**
     * Busca un libro por ID.
     * @param id identificador del libro
    * @return {@link Libro} libro encontrado o {@code null}
     */
    static Libro getLibro(String id) {
        Long idNum = RepositoryUtils.parseLong(id);
        if (idNum == null) {
            return null;
        }

        String sql = """
                SELECT ID_LIBRO, ISBN, TITULO, ANIO_PUBLICACION, COPIAS_TOTALES, COPIAS_DISPONIBLE, ID_AUTOR
                FROM LIBRO
                WHERE ID_LIBRO = ?
                """;
        try (Connection cn = OracleInfra.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, idNum);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                return new Libro(
                        String.valueOf(rs.getLong("ID_LIBRO")),
                        rs.getString("ISBN"),
                        rs.getString("TITULO"),
                        rs.getInt("ANIO_PUBLICACION"),
                        rs.getInt("COPIAS_TOTALES"),
                        rs.getInt("COPIAS_DISPONIBLE"),
                        String.valueOf(rs.getLong("ID_AUTOR"))
                );
            }
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error consultando libro por id", e);
        }
    }

    /**
     * Crea un nuevo libro.
     * @param libro datos del libro
    * @return {@link Libro} libro creado o {@code null}
     */
    static Libro saveLibro(Libro libro) {
        if (existsLibroByIsbnTitulo(libro.getIsbn(), libro.getTitulo())) {
            throw new IllegalStateException("Ya existe un libro con el mismo isbn y titulo");
        }

        long id = RepositoryUtils.nextId("LIBRO", "ID_LIBRO");
        Long idAutor = RepositoryUtils.parseLong(libro.getIdAutor());

        if (idAutor == null) {
            return null;
        }

        String sql = """
                INSERT INTO LIBRO
                (ID_LIBRO, ISBN, TITULO, ANIO_PUBLICACION, COPIAS_TOTALES, COPIAS_DISPONIBLE, ID_AUTOR)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection cn = OracleInfra.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.setString(2, libro.getIsbn());
            ps.setString(3, libro.getTitulo());
            ps.setInt(4, libro.getAnioPublicacion());
            ps.setInt(5, libro.getCopiasTotales());
            ps.setInt(6, libro.getCopiasDisponible());
            ps.setLong(7, idAutor);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error creando libro", e);
        }

        return getLibro(String.valueOf(id));
    }

    /**
     * Actualiza un libro existente.
     * @param id identificador del libro
     * @param libro datos actualizados
    * @return {@link Libro} libro actualizado o {@code null}
     */
    static Libro updateLibro(String id, Libro libro) {
        Long idNum = RepositoryUtils.parseLong(id);
        Long idAutor = RepositoryUtils.parseLong(libro.getIdAutor());

        if (idNum == null || idAutor == null) {
            return null;
        }

        String sql = """
                UPDATE LIBRO
                   SET ISBN = ?,
                       TITULO = ?,
                       ANIO_PUBLICACION = ?,
                       COPIAS_TOTALES = ?,
                       COPIAS_DISPONIBLE = ?,
                       ID_AUTOR = ?
                 WHERE ID_LIBRO = ?
                """;
        try (Connection cn = OracleInfra.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, libro.getIsbn());
            ps.setString(2, libro.getTitulo());
            ps.setInt(3, libro.getAnioPublicacion());
            ps.setInt(4, libro.getCopiasTotales());
            ps.setInt(5, libro.getCopiasDisponible());
            ps.setLong(6, idAutor);
            ps.setLong(7, idNum);

            int updated = ps.executeUpdate();
            if (updated == 0) {
                return null;
            }
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error actualizando libro", e);
        }

        return getLibro(id);
    }

    /**
     * Elimina un libro por ID.
     * @param id identificador del libro
    * @return {@link Libro} libro eliminado o {@code null}
     */
    static Libro deleteLibro(String id) {
        Libro libroExistente = getLibro(id);
        if (libroExistente == null) {
            return null;
        }

        Long idNum = RepositoryUtils.parseLong(id);
        String sql = "DELETE FROM LIBRO WHERE ID_LIBRO = ?";
        try (Connection cn = OracleInfra.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, idNum);
            ps.executeUpdate();
            return libroExistente;
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error eliminando libro", e);
        }
    }

    /**
     * Verifica existencia de libro por ID.
     * @param idLibro identificador del libro
    * @return {@code boolean} {@code true} si existe
     */
    static boolean libroExiste(String idLibro) {
        Long idNum = RepositoryUtils.parseLong(idLibro);
        if (idNum == null) {
            return false;
        }

        String sql = "SELECT 1 FROM LIBRO WHERE ID_LIBRO = ?";
        try (Connection cn = OracleInfra.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, idNum);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error validando existencia de libro", e);
        }
    }

    /**
     * Verifica duplicidad de libro por ISBN y título.
     * @param isbn ISBN del libro
     * @param titulo título del libro
    * @return {@code boolean} {@code true} si ya existe
     */
    static boolean existsLibroByIsbnTitulo(String isbn, String titulo) {
        String sql = """
                SELECT 1
                FROM LIBRO
                WHERE UPPER(TRIM(ISBN)) = UPPER(TRIM(?))
                  AND UPPER(TRIM(TITULO)) = UPPER(TRIM(?))
                """;

        try (Connection cn = OracleInfra.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, isbn);
            ps.setString(2, titulo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error validando duplicidad de libro", e);
        }
    }
}
