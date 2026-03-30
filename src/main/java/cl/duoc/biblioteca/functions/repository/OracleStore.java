package cl.duoc.biblioteca.functions.repository;

import java.util.List;

import cl.duoc.biblioteca.functions.domain.Autor;
import cl.duoc.biblioteca.functions.domain.Libro;
import cl.duoc.biblioteca.functions.domain.Prestamo;
import cl.duoc.biblioteca.functions.domain.Usuario;

public final class OracleStore {

    private OracleStore() {}

    /**
     * Obtiene todos los usuarios.
    * @return {@link List<Usuario>} lista de usuarios
     */
    public static List<Usuario> getUsuarios() {
        return UsuarioRepository.getUsuarios();
    }

    /**
     * Obtiene un usuario por ID.
     * @param id identificador del usuario
    * @return {@link Usuario} usuario encontrado o {@code null}
     */
    public static Usuario getUsuario(String id) {
        return UsuarioRepository.getUsuario(id);
    }

    /**
     * Crea un usuario.
     * @param usuario datos del usuario
    * @return {@link Usuario} usuario creado
     */
    public static Usuario saveUsuario(Usuario usuario) {
        return UsuarioRepository.saveUsuario(usuario);
    }

    /**
     * Actualiza un usuario.
     * @param id identificador del usuario
     * @param usuario datos actualizados
    * @return {@link Usuario} usuario actualizado o {@code null}
     */
    public static Usuario updateUsuario(String id, Usuario usuario) {
        return UsuarioRepository.updateUsuario(id, usuario);
    }

    /**
     * Elimina un usuario.
     * @param id identificador del usuario
    * @return {@link Usuario} usuario eliminado o {@code null}
     */
    public static Usuario deleteUsuario(String id) {
        return UsuarioRepository.deleteUsuario(id);
    }

    /**
     * Obtiene todos los autores.
    * @return {@link List<Autor>} lista de autores
     */
    public static List<Autor> getAutores() {
        return AutorRepository.getAutores();
    }

    /**
     * Obtiene un autor por ID.
     * @param id identificador del autor
    * @return {@link Autor} autor encontrado o {@code null}
     */
    public static Autor getAutor(String id) {
        return AutorRepository.getAutor(id);
    }

    /**
     * Crea un autor.
     * @param autor datos del autor
    * @return {@link Autor} autor creado
     */
    public static Autor saveAutor(Autor autor) {
        return AutorRepository.saveAutor(autor);
    }

    /**
     * Actualiza un autor.
     * @param id identificador del autor
     * @param autor datos actualizados
    * @return {@link Autor} autor actualizado o {@code null}
     */
    public static Autor updateAutor(String id, Autor autor) {
        return AutorRepository.updateAutor(id, autor);
    }

    /**
     * Elimina un autor.
     * @param id identificador del autor
    * @return {@link Autor} autor eliminado o {@code null}
     */
    public static Autor deleteAutor(String id) {
        return AutorRepository.deleteAutor(id);
    }

    /**
     * Cuenta libros asociados a un autor.
     * @param idAutor identificador del autor
    * @return {@code long} cantidad de libros
     */
    public static long getLibrosPorAutor(String idAutor) {
        return AutorRepository.getLibrosPorAutor(idAutor);
    }

    /**
     * Obtiene todos los préstamos.
    * @return {@link List<Prestamo>} lista de préstamos
     */
    public static List<Prestamo> getPrestamos() {
        return PrestamoRepository.getPrestamos();
    }

    /**
     * Obtiene todos los libros.
    * @return {@link List<Libro>} lista de libros
     */
    public static List<Libro> getLibros() {
        return LibroRepository.getLibros();
    }

    /**
     * Obtiene un libro por ID.
     * @param id identificador del libro
    * @return {@link Libro} libro encontrado o {@code null}
     */
    public static Libro getLibro(String id) {
        return LibroRepository.getLibro(id);
    }

    /**
     * Crea un libro.
     * @param libro datos del libro
    * @return {@link Libro} libro creado o {@code null}
     */
    public static Libro saveLibro(Libro libro) {
        return LibroRepository.saveLibro(libro);
    }

    /**
     * Actualiza un libro.
     * @param id identificador del libro
     * @param libro datos actualizados
    * @return {@link Libro} libro actualizado o {@code null}
     */
    public static Libro updateLibro(String id, Libro libro) {
        return LibroRepository.updateLibro(id, libro);
    }

    /**
     * Elimina un libro.
     * @param id identificador del libro
    * @return {@link Libro} libro eliminado o {@code null}
     */
    public static Libro deleteLibro(String id) {
        return LibroRepository.deleteLibro(id);
    }

    /**
     * Obtiene un préstamo por ID.
     * @param id identificador del préstamo
    * @return {@link Prestamo} préstamo encontrado o {@code null}
     */
    public static Prestamo getPrestamo(String id) {
        return PrestamoRepository.getPrestamo(id);
    }

    /**
     * Crea un préstamo.
     * @param prestamo datos del préstamo
    * @return {@link Prestamo} préstamo creado o {@code null}
     */
    public static Prestamo savePrestamo(Prestamo prestamo) {
        return PrestamoRepository.savePrestamo(prestamo);
    }

    /**
     * Actualiza un préstamo.
     * @param id identificador del préstamo
     * @param prestamo datos actualizados
    * @return {@link Prestamo} préstamo actualizado o {@code null}
     */
    public static Prestamo updatePrestamo(String id, Prestamo prestamo) {
        return PrestamoRepository.updatePrestamo(id, prestamo);
    }

    /**
     * Elimina un préstamo.
     * @param id identificador del préstamo
    * @return {@link Prestamo} préstamo eliminado o {@code null}
     */
    public static Prestamo deletePrestamo(String id) {
        return PrestamoRepository.deletePrestamo(id);
    }

    /**
     * Verifica si existe un usuario.
     * @param idUsuario identificador del usuario
    * @return {@code boolean} {@code true} si existe
     */
    public static boolean usuarioExiste(String idUsuario) {
        return UsuarioRepository.usuarioExiste(idUsuario);
    }

    /**
     * Verifica si existe un libro.
     * @param idLibro identificador del libro
    * @return {@code boolean} {@code true} si existe
     */
    public static boolean libroExiste(String idLibro) {
        return LibroRepository.libroExiste(idLibro);
    }

    /**
     * Cuenta préstamos asociados a un libro.
     * @param idLibro identificador del libro
    * @return {@code long} cantidad de préstamos
     */
    public static long getPrestamosPorLibro(String idLibro) {
        return PrestamoRepository.getPrestamosPorLibro(idLibro);
    }

    /**
     * Verifica si existe un autor.
     * @param idAutor identificador del autor
    * @return {@code boolean} {@code true} si existe
     */
    public static boolean autorExiste(String idAutor) {
        return AutorRepository.autorExiste(idAutor);
    }

    /**
     * Cuenta préstamos activos de un usuario.
     * @param usuarioId identificador del usuario
    * @return {@code long} cantidad de préstamos activos
     */
    public static long getPrestamosActivos(String usuarioId) {
        return UsuarioRepository.getPrestamosActivos(usuarioId);
    }

    /**
     * Elimina un usuario inactivo sin préstamos activos.
     * @param usuarioId identificador del usuario
    * @return {@code boolean} {@code true} si se eliminó
     */
    public static boolean deleteUsuarioIfInactive(String usuarioId) {
        return UsuarioRepository.deleteUsuarioIfInactive(usuarioId);
    }

    /**
     * Verifica duplicidad de usuario por identidad y correo.
     * @param nombre nombre
     * @param apellidoPaterno apellido paterno
     * @param apellidoMaterno apellido materno
     * @param email correo
    * @return {@code boolean} {@code true} si ya existe
     */
    public static boolean existsUsuarioByIdentity(String nombre, String apellidoPaterno, String apellidoMaterno, String email) {
        return UsuarioRepository.existsUsuarioByIdentity(nombre, apellidoPaterno, apellidoMaterno, email);
    }

    /**
     * Verifica duplicidad de autor por nombre.
     * @param nombreAutor nombre del autor
    * @return {@code boolean} {@code true} si ya existe
     */
    public static boolean existsAutorByNombre(String nombreAutor) {
        return AutorRepository.existsAutorByNombre(nombreAutor);
    }

    /**
     * Verifica duplicidad de libro por ISBN y título.
     * @param isbn ISBN del libro
     * @param titulo título del libro
    * @return {@code boolean} {@code true} si ya existe
     */
    public static boolean existsLibroByIsbnTitulo(String isbn, String titulo) {
        return LibroRepository.existsLibroByIsbnTitulo(isbn, titulo);
    }

    /**
     * Verifica duplicidad de préstamo por usuario y libro.
     * @param idUsuario identificador del usuario
     * @param idLibro identificador del libro
    * @return {@code boolean} {@code true} si ya existe
     */
    public static boolean existsPrestamoByUsuarioLibro(String idUsuario, String idLibro) {
        return PrestamoRepository.existsPrestamoByUsuarioLibro(idUsuario, idLibro);
    }
}
