package cl.duoc.biblioteca.functions.repository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

import cl.duoc.biblioteca.functions.domain.Autor;
import cl.duoc.biblioteca.functions.domain.Libro;
import cl.duoc.biblioteca.functions.domain.Prestamo;
import cl.duoc.biblioteca.functions.domain.Usuario;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;

public final class OracleStore {

    private static final String DEFAULT_TNS_ALIAS = "bdlibreriacn2_high";
    private static final String DEFAULT_WALLET_PATH = "./Wallet_BDLIBRERIACN2";
    private static final int POOL_INITIAL_SIZE = 2;
    private static final int POOL_MIN_SIZE = 2;
    private static final int POOL_MAX_SIZE = 8;
    private static final int POOL_TIMEOUT_CHECK_INTERVAL = 30;
    private static final int POOL_INACTIVE_TIMEOUT = 120;
    private static final boolean POOL_VALIDATE_ON_BORROW = true;
    private static volatile PoolDataSource poolDataSource;

    private OracleStore() {}

    /**
     * Consulta todos los usuarios registrados en la base de datos, ordenados por su ID de forma ascendente.
     */
    public static List<Usuario> getUsuarios() {
        String sql = "SELECT ID_USUARIO, NOMBRE, APELLIDO_P, APELLIDO_M, CORREO, ACTIVO FROM USUARIO ORDER BY ID_USUARIO";
        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            java.util.ArrayList<Usuario> result = new java.util.ArrayList<>();
            while (rs.next()) {
                result.add(new Usuario(
                        String.valueOf(rs.getLong("ID_USUARIO")),
                        rs.getString("NOMBRE"),
                        rs.getString("APELLIDO_P"),
                        rs.getString("APELLIDO_M"),
                        rs.getString("CORREO"),
                        rs.getInt("ACTIVO") == 1
                ));
            }
            return result;
        } catch (SQLException e) {
            throw sqlException("Error consultando usuarios", e);
        }
    }

    /**
     * Consulta un usuario específico por su ID. 
     * Si el ID no es un número válido o no se encuentra ningún usuario con ese ID, se devuelve null.
     * @param id
     * @return {@link Usuario} o {@code null}
     */
    public static Usuario getUsuario(String id) {
        Long idNum = parseLong(id);
        if (idNum == null) {
            return null;
        }

        String sql = "SELECT ID_USUARIO, NOMBRE, APELLIDO_P, APELLIDO_M, CORREO, ACTIVO FROM USUARIO WHERE ID_USUARIO = ?";
        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, idNum);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new Usuario(
                        String.valueOf(rs.getLong("ID_USUARIO")),
                        rs.getString("NOMBRE"),
                        rs.getString("APELLIDO_P"),
                        rs.getString("APELLIDO_M"),
                        rs.getString("CORREO"),
                        rs.getInt("ACTIVO") == 1
                );
            }
        } catch (SQLException e) {
            throw sqlException("Error consultando usuario por id", e);
        }
    }

    /**
     * Creación de nuevo usuario. 
     * Antes de crear el usuario, se valida que no exista otro usuario con el mismo nombre, apellido paterno, apellido materno y email. 
     * Si ya existe un usuario con la misma combinación de estos campos, se lanza una excepción IllegalStateException. 
     * Si la creación es exitosa, se devuelve el usuario creado con su ID asignado.
     * @param usuario
     * @return {@link Usuario}
     */
    public static Usuario saveUsuario(Usuario usuario) {
        if (existsUsuarioByIdentity(
                usuario.getNombre(),
                usuario.getApellidoPaterno(),
                usuario.getApellidoMaterno(),
                usuario.getEmail())) {
            throw new IllegalStateException("Ya existe un usuario con el mismo nombre, apellidoPaterno, apellidoMaterno y email");
        }

        long id = nextId("USUARIO", "ID_USUARIO");
        int rut = buildRut(id);

        String sql = """
                INSERT INTO USUARIO
                (ID_USUARIO, RUT, DV, NOMBRE, APELLIDO_P, APELLIDO_M, CORREO, FECHA_REGISTRO, ACTIVO, ID_TIPO_USUARIO)
                VALUES (?, ?, ?, ?, ?, ?, ?, SYSDATE, ?, ?)
                """;

        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.setInt(2, rut);
            ps.setString(3, "9");
            ps.setString(4, usuario.getNombre());
            ps.setString(5, usuario.getApellidoPaterno());
            ps.setString(6, usuario.getApellidoMaterno());
            ps.setString(7, usuario.getEmail());
            ps.setInt(8, usuario.isActivo() != null && usuario.isActivo() ? 1 : 0);
            ps.setInt(9, 2); // cliente
            ps.executeUpdate();
        } catch (SQLException e) {
            throw sqlException("Error creando usuario", e);
        }

        return getUsuario(String.valueOf(id));
    }

    /**
     * Actualiza un usuario existente en la base de datos.
     * @param id
     * @param usuario
     * @return {@link Usuario} o {@code null}
     */
    public static Usuario updateUsuario(String id, Usuario usuario) {
        Long idNum = parseLong(id);
        if (idNum == null) {
            return null;
        }

        String sql = "UPDATE USUARIO SET NOMBRE = ?, APELLIDO_P = ?, APELLIDO_M = ?, CORREO = ?, ACTIVO = ? WHERE ID_USUARIO = ?";
        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, usuario.getNombre());
            ps.setString(2, usuario.getApellidoPaterno());
            ps.setString(3, usuario.getApellidoMaterno());
            ps.setString(4, usuario.getEmail());
            ps.setInt(5, usuario.isActivo() != null && usuario.isActivo() ? 1 : 0);
            ps.setLong(6, idNum);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                return null;
            }
        } catch (SQLException e) {
            throw sqlException("Error actualizando usuario", e);
        }

        return getUsuario(id);
    }

    /**
     * Elimina un usuario de la base de datos.
     * @param id
     * @return {@link Usuario} o {@code null}
     */
    public static Usuario deleteUsuario(String id) {
        Usuario usuarioExistente = getUsuario(id);
        if (usuarioExistente == null) {
            return null;
        }

        Long idNum = parseLong(id);
        String sql = "DELETE FROM USUARIO WHERE ID_USUARIO = ?";
        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, idNum);
            ps.executeUpdate();
            return usuarioExistente;
        } catch (SQLException e) {
            throw sqlException("Error eliminando usuario", e);
        }
    }
    
    /**
     * Consulta todos los autores registrados en la base de datos, ordenados por su ID de forma ascendente.
     * @return Lista de {@link Autor}
     */
    public static List<Autor> getAutores() {
        String sql = """
                SELECT ID_AUTOR, NOMBRE_AUTOR
                FROM AUTOR
                ORDER BY ID_AUTOR
                """;
        try (Connection cn = getConnection();
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
            throw sqlException("Error consultando autores", e);
        }
    }

    /**
     * Consulta un autor específico por su ID. 
     * Si el ID no es un número válido o no se encuentra ningún autor con ese ID, se devuelve null.
     * @param id
     * @return {@link Autor} o {@code null}
     */
    public static Autor getAutor(String id) {
        Long idNum = parseLong(id);
        if (idNum == null) {
            return null;
        }

        String sql = """
                SELECT ID_AUTOR, NOMBRE_AUTOR
                FROM AUTOR
                WHERE ID_AUTOR = ?
                """;
        try (Connection cn = getConnection();
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
            throw sqlException("Error consultando autor por id", e);
        }
    }

    /**
     * Crea un nuevo autor en la base de datos. 
     * Antes de crear el autor, se valida que no exista otro autor con el mismo nombre (ignorando mayúsculas, minúsculas y espacios). Si ya existe un autor con el mismo nombre, se lanza una excepción IllegalStateException. Si la creación es exitosa, se devuelve el autor creado con su ID asignado.
     * @param autor
     * @return El autor creado con su ID asignado
     */
    public static Autor saveAutor(Autor autor) {
        if (existsAutorByNombre(autor.getNombreAutor())) {
            throw new IllegalStateException("Ya existe un autor con el mismo nombreAutor");
        }

        long id = nextId("AUTOR", "ID_AUTOR");

        String sql = """
                INSERT INTO AUTOR
                (ID_AUTOR, NOMBRE_AUTOR)
                VALUES (?, ?)
                """;

        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.setString(2, autor.getNombreAutor());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw sqlException("Error creando autor", e);
        }

        return getAutor(String.valueOf(id));
    }

    /**
     * Actualiza un autor existente en la base de datos. 
     * Se valida que el ID proporcionado sea un número válido y que exista un autor con ese ID. Si no se cumple alguna de estas condiciones, se devuelve null. Si el autor a actualizar tiene un nombre que ya existe para otro autor (ignorando mayúsculas, minúsculas y espacios), se lanza una excepción IllegalStateException. Si la actualización es exitosa, se devuelve el autor actualizado.
     * @param id
     * @param autor
     * @return {@link Autor} o {@code null}
     */
    public static Autor updateAutor(String id, Autor autor) {
        Long idNum = parseLong(id);
        if (idNum == null) {
            return null;
        }

        String sql = "UPDATE AUTOR SET NOMBRE_AUTOR = ? WHERE ID_AUTOR = ?";
        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, autor.getNombreAutor());
            ps.setLong(2, idNum);

            int updated = ps.executeUpdate();
            if (updated == 0) {
                return null;
            }
        } catch (SQLException e) {
            throw sqlException("Error actualizando autor", e);
        }

        return getAutor(id);
    }

    /**
     * Elimina un autor de la base de datos. 
     * Se valida que el ID proporcionado sea un número válido y que exista un autor con ese ID. 
     * Si no se cumple alguna de estas condiciones, se devuelve null. 
     * Si la eliminación es exitosa, se devuelve el autor que fue eliminado.
     * @param id
     * @return {@link Autor} o {@code null}
     */
    public static Autor deleteAutor(String id) {
        Autor autorExistente = getAutor(id);
        if (autorExistente == null) {
            return null;
        }

        Long idNum = parseLong(id);
        String sql = "DELETE FROM AUTOR WHERE ID_AUTOR = ?";
        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, idNum);
            ps.executeUpdate();
            return autorExistente;
        } catch (SQLException e) {
            throw sqlException("Error eliminando autor", e);
        }
    }

    /**
     * Cuenta la cantidad de libros asociados a un autor específico.
     * @param idAutor
     * @return cantidad de libros asociados al autor, o 0 si el ID no es válido o no se encuentra el autor.
     */
    public static long getLibrosPorAutor(String idAutor) {
        Long idNum = parseLong(idAutor);
        if (idNum == null) {
            return 0;
        }

        String sql = "SELECT COUNT(*) FROM LIBRO WHERE ID_AUTOR = ?";
        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, idNum);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw sqlException("Error contando libros por autor", e);
        }
    }

    /**
     * Consulta todos los préstamos registrados en la base de datos, ordenados por su ID de forma ascendente.
     * @return {@link List<Prestamo>}
     */
    public static List<Prestamo> getPrestamos() {
        String sql = """
                SELECT p.ID_PRESTAMO, p.ID_USUARIO, p.LIBRO_ID_LIBRO,
                       p.FECHA_PRESTAMO, p.FECHA_DEVOLUCION_ESPERADA, te.NOMBRE_ESTADO
                FROM PRESTAMO p
                JOIN TIPO_ESTADO te ON te.ID_ESTADO = p.ID_ESTADO
                ORDER BY p.ID_PRESTAMO
                """;
        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            java.util.ArrayList<Prestamo> result = new java.util.ArrayList<>();
            while (rs.next()) {
                result.add(new Prestamo(
                        String.valueOf(rs.getLong("ID_PRESTAMO")),
                        String.valueOf(rs.getLong("ID_USUARIO")),
                        String.valueOf(rs.getLong("LIBRO_ID_LIBRO")),
                        formatDate(rs.getDate("FECHA_PRESTAMO")),
                        formatDate(rs.getDate("FECHA_DEVOLUCION_ESPERADA")),
                        rs.getString("NOMBRE_ESTADO")
                ));
            }
            return result;
        } catch (SQLException e) {
            throw sqlException("Error consultando préstamos", e);
        }
    }

    /**
     * Consulta todos los libros registrados en la base de datos, ordenados por su ID de forma ascendente.
     * @return {@link List<Libro>}
     */
    public static List<Libro> getLibros() {
        String sql = """
                SELECT ID_LIBRO, ISBN, TITULO, ANIO_PUBLICACION, COPIAS_TOTALES, COPIAS_DISPONIBLE, ID_AUTOR
                FROM LIBRO
                ORDER BY ID_LIBRO
                """;
        try (Connection cn = getConnection();
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
            throw sqlException("Error consultando libros", e);
        }
    }

    /**
     * Consulta un libro específico por su ID.
     * @param id
     * @return {@link Libro} o {@code null}
     */
    public static Libro getLibro(String id) {
        Long idNum = parseLong(id);
        if (idNum == null) {
            return null;
        }

        String sql = """
                SELECT ID_LIBRO, ISBN, TITULO, ANIO_PUBLICACION, COPIAS_TOTALES, COPIAS_DISPONIBLE, ID_AUTOR
                FROM LIBRO
                WHERE ID_LIBRO = ?
                """;
        try (Connection cn = getConnection();
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
            throw sqlException("Error consultando libro por id", e);
        }
    }

    /**
     * Crea un nuevo libro en la base de datos.
     * @param libro
     * @return {@link Libro} o {@code null}
     */
    public static Libro saveLibro(Libro libro) {
        if (existsLibroByIsbnTitulo(libro.getIsbn(), libro.getTitulo())) {
            throw new IllegalStateException("Ya existe un libro con el mismo isbn y titulo");
        }

        long id = nextId("LIBRO", "ID_LIBRO");
        Long idAutor = parseLong(libro.getIdAutor());

        if (idAutor == null) {
            return null;
        }

        String sql = """
                INSERT INTO LIBRO
                (ID_LIBRO, ISBN, TITULO, ANIO_PUBLICACION, COPIAS_TOTALES, COPIAS_DISPONIBLE, ID_AUTOR)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection cn = getConnection();
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
            throw sqlException("Error creando libro", e);
        }

        return getLibro(String.valueOf(id));
    }

    /**
     * Actualiza un libro existente en la base de datos.
     * @param id
     * @param libro
     * @return {@link Libro} o {@code null}
     */
    public static Libro updateLibro(String id, Libro libro) {
        Long idNum = parseLong(id);
        Long idAutor = parseLong(libro.getIdAutor());

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
        try (Connection cn = getConnection();
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
            throw sqlException("Error actualizando libro", e);
        }

        return getLibro(id);
    }

    /**
     * Elimina un libro de la base de datos.
     * @param id
     * @return {@link Libro} o {@code null}
     */
    public static Libro deleteLibro(String id) {
        Libro libroExistente = getLibro(id);
        if (libroExistente == null) {
            return null;
        }

        Long idNum = parseLong(id);
        String sql = "DELETE FROM LIBRO WHERE ID_LIBRO = ?";
        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, idNum);
            ps.executeUpdate();
            return libroExistente;
        } catch (SQLException e) {
            throw sqlException("Error eliminando libro", e);
        }
    }

    /**
     * Elimina un préstamo de la base de datos.
     * @param id
     * @return {@link Prestamo} o {@code null}
     */
    public static Prestamo getPrestamo(String id) {
        Long idNum = parseLong(id);
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
        try (Connection cn = getConnection();
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
                        formatDate(rs.getDate("FECHA_PRESTAMO")),
                        formatDate(rs.getDate("FECHA_DEVOLUCION_ESPERADA")),
                        rs.getString("NOMBRE_ESTADO")
                );
            }
        } catch (SQLException e) {
            throw sqlException("Error consultando préstamo por id", e);
        }
    }

    /**
     * Crea un nuevo préstamo y devuelve el registro creado.
     * Retorna {@code null} si el ID de usuario o libro no es válido,
     * y lanza {@link IllegalStateException} si ya existe un préstamo
     * para la misma combinación de usuario y libro.
     * @param prestamo datos del préstamo a crear
     * @return {@link Prestamo} o {@code null}
     */
    public static Prestamo savePrestamo(Prestamo prestamo) {
        Long idUsuario = parseLong(prestamo.getIdUsuario());
        Long idLibro = parseLong(prestamo.getIdLibro());
        if (idUsuario == null || idLibro == null) {
            return null;
        }

        if (existsPrestamoByUsuarioLibro(prestamo.getIdUsuario(), prestamo.getIdLibro())) {
            throw new IllegalStateException("Ya existe un préstamo para el mismo idUsuario e idLibro");
        }

        long id = nextId("PRESTAMO", "ID_PRESTAMO");
        Date fechaPrestamo = toSqlDate(prestamo.getFechaPrestamo(), LocalDate.now());
        Date fechaEsperada = toSqlDate(prestamo.getFechaDevolucion(), LocalDate.now().plusDays(14));

        int estadoId = estadoToId(prestamo.getEstado());

        String sql = """
                INSERT INTO PRESTAMO
                (ID_PRESTAMO, ID_USUARIO, LIBRO_ID_LIBRO, FECHA_PRESTAMO, FECHA_DEVOLUCION_ESPERADA, FECHA_DEVOLUCION_REAL, ID_ESTADO)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.setLong(2, idUsuario);
            ps.setLong(3, idLibro);
            ps.setDate(4, fechaPrestamo);
            ps.setDate(5, fechaEsperada);

            if (estadoId == 2 && prestamo.getFechaDevolucion() != null && !prestamo.getFechaDevolucion().isBlank()) {
                ps.setDate(6, toSqlDate(prestamo.getFechaDevolucion(), LocalDate.now()));
            } else {
                ps.setNull(6, java.sql.Types.DATE);
            }
            ps.setInt(7, estadoId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw sqlException("Error creando préstamo", e);
        }

        return getPrestamo(String.valueOf(id));
    }

    /**
     * Actualiza un préstamo existente en la base de datos.
     * @param id
     * @param prestamo
     * @return {@link Prestamo} o {@code null}
     */
    public static Prestamo updatePrestamo(String id, Prestamo prestamo) {
        Long idNum = parseLong(id);
        Long idUsuario = parseLong(prestamo.getIdUsuario());
        Long idLibro = parseLong(prestamo.getIdLibro());
        if (idNum == null || idUsuario == null || idLibro == null) {
            return null;
        }

        Date fechaPrestamo = toSqlDate(prestamo.getFechaPrestamo(), LocalDate.now());
        Date fechaEsperada = toSqlDate(prestamo.getFechaDevolucion(), LocalDate.now().plusDays(14));
        int estadoId = estadoToId(prestamo.getEstado());

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
        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, idUsuario);
            ps.setLong(2, idLibro);
            ps.setDate(3, fechaPrestamo);
            ps.setDate(4, fechaEsperada);

            if (estadoId == 2 && prestamo.getFechaDevolucion() != null && !prestamo.getFechaDevolucion().isBlank()) {
                ps.setDate(5, toSqlDate(prestamo.getFechaDevolucion(), LocalDate.now()));
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
            throw sqlException("Error actualizando préstamo", e);
        }

        return getPrestamo(id);
    }

    /**
     * Elimina un préstamo de la base de datos. 
     * Se valida que el ID proporcionado sea un número válido y que exista un préstamo con ese ID. 
     * Si no se cumple alguna de estas condiciones, se devuelve null. 
     * Si la eliminación es exitosa, se devuelve el préstamo que fue eliminado.
     * @param id
     * @return {@link Prestamo} o {@code null}
     */
    public static Prestamo deletePrestamo(String id) {
        Prestamo previo = getPrestamo(id);
        if (previo == null) {
            return null;
        }

        Long idNum = parseLong(id);
        String sql = "DELETE FROM PRESTAMO WHERE ID_PRESTAMO = ?";
        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, idNum);
            ps.executeUpdate();
            return previo;
        } catch (SQLException e) {
            throw sqlException("Error eliminando préstamo", e);
        }
    }

    /**
     * Valida si existe un usuario con el ID proporcionado.
     * @param idUsuario
     * @return {@code true} o {@code false}
     */
    public static boolean usuarioExiste(String idUsuario) {
        Long idNum = parseLong(idUsuario);
        if (idNum == null) {
            return false;
        }

        String sql = "SELECT 1 FROM USUARIO WHERE ID_USUARIO = ?";
        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, idNum);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw sqlException("Error validando existencia de usuario", e);
        }
    }

    /**
     * Valida si existe un libro con el ID proporcionado.
     * @param idLibro
     * @return {@code true} o {@code false}
     */
    public static boolean libroExiste(String idLibro) {
        Long idNum = parseLong(idLibro);
        if (idNum == null) {
            return false;
        }

        String sql = "SELECT 1 FROM LIBRO WHERE ID_LIBRO = ?";
        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, idNum);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw sqlException("Error validando existencia de libro", e);
        }
    }

    /**
     * Cuenta la cantidad de préstamos asociados a un libro específico.
     * @param idLibro
     * @return {@code long} o {@code 0}
     */
    public static long getPrestamosPorLibro(String idLibro) {
        Long idNum = parseLong(idLibro);
        if (idNum == null) {
            return 0;
        }

        String sql = "SELECT COUNT(*) FROM PRESTAMO WHERE LIBRO_ID_LIBRO = ?";
        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, idNum);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw sqlException("Error contando préstamos por libro", e);
        }
    }

    /**
     * Valida si existe un autor con el ID proporcionado.
     * @param idAutor
     * @return {@code true} o {@code false}
     */
    public static boolean autorExiste(String idAutor) {
        Long idNum = parseLong(idAutor);
        if (idNum == null) {
            return false;
        }

        String sql = "SELECT 1 FROM AUTOR WHERE ID_AUTOR = ?";
        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, idNum);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw sqlException("Error validando existencia de autor", e);
        }
    }

    /**
     * Cuenta la cantidad de préstamos activos asociados a un usuario específico. 
     * Un préstamo se considera activo si su estado es diferente a "DEVUELTO" (ID_ESTADO != 2).
     * @param usuarioId
     * @return {@code long} o {@code 0}
     */
    public static long getPrestamosActivos(String usuarioId) {
        Long idNum = parseLong(usuarioId);
        if (idNum == null) {
            return 0;
        }
        String sql = "SELECT COUNT(*) FROM PRESTAMO WHERE ID_USUARIO = ? AND ID_ESTADO != 2";
        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, idNum);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw sqlException("Error contando préstamos activos", e);
        }
    }

    /**
     * Elimina un usuario de la base de datos solo si no tiene préstamos activos asociados.
     * @param usuarioId
     * @return {@code true} o {@code false}
     */
    public static boolean deleteUsuarioIfInactive(String usuarioId) {
        Long idNum = parseLong(usuarioId);
        if (idNum == null) {
            return false;
        }

        Usuario usuario = getUsuario(usuarioId);
        if (usuario == null || usuario.isActivo()) {
            return false;
        }

        long prestamosActivos = getPrestamosActivos(usuarioId);
        if (prestamosActivos > 0) {
            return false;
        }

        String sql = "DELETE FROM USUARIO WHERE ID_USUARIO = ?";
        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, idNum);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw sqlException("Error eliminando usuario inactivo", e);
        }
    }

    /**
     * Valida si ya existe un usuario con la misma combinación de nombre, 
     * apellido paterno, apellido materno y email (ignorando mayúsculas, minúsculas y espacios).
     * @param nombre
     * @param apellidoPaterno
     * @param apellidoMaterno
     * @param email
     * @return {@code true} o {@code false}
     */
    public static boolean existsUsuarioByIdentity(String nombre, String apellidoPaterno, String apellidoMaterno, String email) {
        String sql = """
                SELECT 1
                FROM USUARIO
                WHERE UPPER(TRIM(NOMBRE)) = UPPER(TRIM(?))
                  AND UPPER(TRIM(APELLIDO_P)) = UPPER(TRIM(?))
                  AND UPPER(TRIM(APELLIDO_M)) = UPPER(TRIM(?))
                  AND UPPER(TRIM(CORREO)) = UPPER(TRIM(?))
                """;

        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, nombre);
            ps.setString(2, apellidoPaterno);
            ps.setString(3, apellidoMaterno);
            ps.setString(4, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw sqlException("Error validando duplicidad de usuario", e);
        }
    }

    /**
     * Valida si ya existe un autor con el mismo nombre (ignorando mayúsculas, minúsculas y espacios).
     * @param nombreAutor
     * @return {@code true} o {@code false}
     */
    public static boolean existsAutorByNombre(String nombreAutor) {
        String sql = """
                SELECT 1
                FROM AUTOR
                WHERE UPPER(TRIM(NOMBRE_AUTOR)) = UPPER(TRIM(?))
                """;

        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, nombreAutor);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw sqlException("Error validando duplicidad de autor", e);
        }
    }

    /**
     * Valida si ya existe un libro con la misma combinación de ISBN y título (ignorando mayúsculas, 
     * minúsculas y espacios).
     * @param isbn
     * @param titulo
     * @return {@code true} o {@code false}
     */
    public static boolean existsLibroByIsbnTitulo(String isbn, String titulo) {
        String sql = """
                SELECT 1
                FROM LIBRO
                WHERE UPPER(TRIM(ISBN)) = UPPER(TRIM(?))
                  AND UPPER(TRIM(TITULO)) = UPPER(TRIM(?))
                """;

        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, isbn);
            ps.setString(2, titulo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw sqlException("Error validando duplicidad de libro", e);
        }
    }

    /**
     * Valida si ya existe un préstamo para la misma combinación de usuario y libro.
     * @param idUsuario
     * @param idLibro
     * @return {@code true} o {@code false}
     */
    public static boolean existsPrestamoByUsuarioLibro(String idUsuario, String idLibro) {
        Long idUsuarioNum = parseLong(idUsuario);
        Long idLibroNum = parseLong(idLibro);
        if (idUsuarioNum == null || idLibroNum == null) {
            return false;
        }

        String sql = """
                SELECT 1
                FROM PRESTAMO
                WHERE ID_USUARIO = ?
                  AND LIBRO_ID_LIBRO = ?
                """;

        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, idUsuarioNum);
            ps.setLong(2, idLibroNum);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw sqlException("Error validando duplicidad de préstamo", e);
        }
    }

    /**
     * Obtiene una conexión a la base de datos desde el pool de conexiones configurado.
     * @return {@code Connection} a la base de datos
     * @throws SQLException
     */
    private static Connection getConnection() throws SQLException {
        return getPoolDataSource().getConnection();
    }

    /**
     * Obtiene una instancia de {@code PoolDataSource} configurada para conectarse a la base de datos Oracle.
     * @return {@code PoolDataSource} configurado para Oracle
     * @throws SQLException
     */
    private static synchronized PoolDataSource getPoolDataSource() throws SQLException {
        if (poolDataSource == null) {
            poolDataSource = buildPoolDataSource();
        }
        return poolDataSource;
    }
    /**
     * Construye un PoolDataSource configurado para conectarse a una base de datos Oracle 
     * utilizando las credenciales y parámetros definidos en las variables de entorno.
     * @return {@code PoolDataSource} configurado para Oracle
     * @throws SQLException
     */
    private static PoolDataSource buildPoolDataSource() throws SQLException {
        String user = getenvRequired("ORACLE_USER");
        String pwd = getenvAnyRequired("ORACLE_PASSWORD", "ORACLE_ADMIN_PASSWORD");
        String jdbcUrl = resolveJdbcUrl();

        PoolDataSource pds = PoolDataSourceFactory.getPoolDataSource();
        pds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
        pds.setConnectionPoolName("biblioteca-oracle-pool");
        pds.setURL(jdbcUrl);
        pds.setUser(user);
        pds.setPassword(pwd);
        pds.setInitialPoolSize(POOL_INITIAL_SIZE);
        pds.setMinPoolSize(POOL_MIN_SIZE);
        pds.setMaxPoolSize(POOL_MAX_SIZE);
        pds.setTimeoutCheckInterval(POOL_TIMEOUT_CHECK_INTERVAL);
        pds.setInactiveConnectionTimeout(POOL_INACTIVE_TIMEOUT);
        pds.setValidateConnectionOnBorrow(POOL_VALIDATE_ON_BORROW);
        return pds;
    }

    /**
     * Resuelve la URL de conexión JDBC para Oracle.
     * @return {@code String} URL de conexión JDBC
     */
    private static String resolveJdbcUrl() {
        String jdbcUrl = System.getenv("ORACLE_JDBC_URL");
        if (jdbcUrl != null && !jdbcUrl.isBlank()) {
            return jdbcUrl;
        }

        String alias = getenvOrDefault("ORACLE_TNS_ALIAS", DEFAULT_TNS_ALIAS);
        String walletPath = getenvOrDefault("ORACLE_WALLET_PATH", DEFAULT_WALLET_PATH);
        return "jdbc:oracle:thin:@" + alias + "?TNS_ADMIN=" + walletPath;
    }

    /**
     * Obtiene el próximo ID para una tabla (MAX(idColumn) + 1).
     * Si no hay registros, retorna 1.
     * @param tableName
     * @param idColumn
     * @return {@code long} siguiente ID disponible
     */
    private static long nextId(String tableName, String idColumn) {
        String sql = "SELECT NVL(MAX(" + idColumn + "), 0) + 1 FROM " + tableName;
        try (Connection cn = getConnection();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw sqlException("Error obteniendo siguiente id para " + tableName, e);
        }
    }

    /**
     * Construye un RUT único para un usuario a partir de su ID numérico.
     * @param idUsuario
     * @return {@code int}
     */
    private static int buildRut(long idUsuario) {
        long rut = 10_000_000L + idUsuario;
        if (rut > 99_999_999L) {
            rut = 99_999_999L - (idUsuario % 10_000_000L);
        }
        return (int) rut;
    }

    /**
     * Intenta convertir una cadena a un número Long. 
     * Si la cadena es null, vacía o no es un número válido, se devuelve null.
     * @param value
     * @return {@code Long} o {@code null}
     */
    private static Long parseLong(String value) {
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
     * Convierte una cadena a un objeto Date de SQL, intentando parsear en varios formatos de fecha y hora.
     * @param value
     * @param defaultDate
     * @return {@code Date} o {@code defaultDate}
     */
    private static Date toSqlDate(String value, LocalDate defaultDate) {
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
     * Convierte un objeto Date a su representación de cadena en formato ISO (YYYY-MM-DD).
     * @param date
     * @return {@code String} o {@code null}
     */
    private static String formatDate(Date date) {
        return date == null ? null : date.toLocalDate().toString();
    }

    /**
     * Convierte un estado de préstamo representado como cadena a su correspondiente ID numérico en la base de datos.
     * @param estado
     * @return {@code int} o {@code 1}
     */
    private static int estadoToId(String estado) {
        if (estado == null || estado.isBlank()) {
            return 1;
        }
        return switch (estado.trim().toUpperCase(Locale.ROOT)) {
            case "DEVUELTO" -> 2;
            case "ATRASADO" -> 3;
            default -> 1;
        };
    }

    /**
     * Obtiene el valor de una variable de entorno requerida. 
     * Si la variable no está definida o está vacía, 
     * se lanza una excepción IllegalStateException indicando que falta la variable requerida.
     * @param key
     * @return {@code String}
     */
    private static String getenvRequired(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Falta variable de entorno requerida: " + key);
        }
        return value;
    }

    /**
     * Obtiene el valor de la primera variable de entorno definida y no vacía entre las proporcionadas.
     * @param keys
     * @return {@code String}
     */
    private static String getenvAnyRequired(String... keys) {
        for (String key : keys) {
            String value = System.getenv(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        throw new IllegalStateException("Falta variable de entorno requerida: " + String.join(" o ", keys));
    }

    /**
     * Construye un mensaje de error detallado a partir de una excepción SQLException, 
     * incluyendo el mensaje original, el SQLState y el código de error.
     * @param message
     * @param e
     * @return {@link RuntimeException}
     */
    private static RuntimeException sqlException(String message, SQLException e) {
        String detail = String.format(
                "%s (SQLState=%s, ErrorCode=%d): %s",
                message,
                e.getSQLState(),
                e.getErrorCode(),
                e.getMessage()
        );
        return new RuntimeException(detail, e);
    }

    /**
     * Obtiene el valor de una variable de entorno o devuelve un valor 
     * predeterminado si la variable no está definida o está vacía.
     * @param key
     * @param defaultValue
     * @return {@code String}
     */
    private static String getenvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

}