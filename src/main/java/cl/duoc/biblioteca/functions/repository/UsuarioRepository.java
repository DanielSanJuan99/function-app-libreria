package cl.duoc.biblioteca.functions.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import cl.duoc.biblioteca.functions.domain.Usuario;
import cl.duoc.biblioteca.functions.exception.RepositoryExceptionHandler;

final class UsuarioRepository {

    private UsuarioRepository() {}

    /**
     * Lista todos los usuarios.
    * @return {@link List<Usuario>} lista de usuarios
     */
    static List<Usuario> getUsuarios() {
        String sql = "SELECT ID_USUARIO, NOMBRE, APELLIDO_P, APELLIDO_M, CORREO, ACTIVO FROM USUARIO ORDER BY ID_USUARIO";
        try (Connection cn = OracleInfra.getConnection();
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
            throw RepositoryExceptionHandler.sqlException("Error consultando usuarios", e);
        }
    }

    /**
     * Busca un usuario por ID.
     * @param id identificador del usuario
    * @return {@link Usuario} usuario encontrado o {@code null}
     */
    static Usuario getUsuario(String id) {
        Long idNum = RepositoryUtils.parseLong(id);
        if (idNum == null) {
            return null;
        }

        String sql = "SELECT ID_USUARIO, NOMBRE, APELLIDO_P, APELLIDO_M, CORREO, ACTIVO FROM USUARIO WHERE ID_USUARIO = ?";
        try (Connection cn = OracleInfra.getConnection();
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
            throw RepositoryExceptionHandler.sqlException("Error consultando usuario por id", e);
        }
    }

    /**
     * Crea un nuevo usuario.
     * @param usuario datos del usuario
    * @return {@link Usuario} usuario creado
     */
    static Usuario saveUsuario(Usuario usuario) {
        if (existsUsuarioByIdentity(
                usuario.getNombre(),
                usuario.getApellidoPaterno(),
                usuario.getApellidoMaterno(),
                usuario.getEmail())) {
            throw new IllegalStateException("Ya existe un usuario con el mismo nombre, apellidoPaterno, apellidoMaterno y email");
        }

        long id = RepositoryUtils.nextId("USUARIO", "ID_USUARIO");
        int rut = RepositoryUtils.buildRut(id);

        String sql = """
                INSERT INTO USUARIO
                (ID_USUARIO, RUT, DV, NOMBRE, APELLIDO_P, APELLIDO_M, CORREO, FECHA_REGISTRO, ACTIVO, ID_TIPO_USUARIO)
                VALUES (?, ?, ?, ?, ?, ?, ?, SYSDATE, ?, ?)
                """;

        try (Connection cn = OracleInfra.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.setInt(2, rut);
            ps.setString(3, "9");
            ps.setString(4, usuario.getNombre());
            ps.setString(5, usuario.getApellidoPaterno());
            ps.setString(6, usuario.getApellidoMaterno());
            ps.setString(7, usuario.getEmail());
            ps.setInt(8, usuario.isActivo() != null && usuario.isActivo() ? 1 : 0);
            ps.setInt(9, 2);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error creando usuario", e);
        }

        return getUsuario(String.valueOf(id));
    }

    /**
     * Actualiza un usuario existente.
     * @param id identificador del usuario
     * @param usuario datos actualizados
    * @return {@link Usuario} usuario actualizado o {@code null}
     */
    static Usuario updateUsuario(String id, Usuario usuario) {
        Long idNum = RepositoryUtils.parseLong(id);
        if (idNum == null) {
            return null;
        }

        String sql = "UPDATE USUARIO SET NOMBRE = ?, APELLIDO_P = ?, APELLIDO_M = ?, CORREO = ?, ACTIVO = ? WHERE ID_USUARIO = ?";
        try (Connection cn = OracleInfra.getConnection();
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
            throw RepositoryExceptionHandler.sqlException("Error actualizando usuario", e);
        }

        return getUsuario(id);
    }

    /**
     * Elimina un usuario por ID.
     * @param id identificador del usuario
    * @return {@link Usuario} usuario eliminado o {@code null}
     */
    static Usuario deleteUsuario(String id) {
        Usuario usuarioExistente = getUsuario(id);
        if (usuarioExistente == null) {
            return null;
        }

        Long idNum = RepositoryUtils.parseLong(id);
        String sql = "DELETE FROM USUARIO WHERE ID_USUARIO = ?";
        try (Connection cn = OracleInfra.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, idNum);
            ps.executeUpdate();
            return usuarioExistente;
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error eliminando usuario", e);
        }
    }

    /**
     * Verifica existencia de usuario por ID.
     * @param idUsuario identificador del usuario
    * @return {@code boolean} {@code true} si existe
     */
    static boolean usuarioExiste(String idUsuario) {
        Long idNum = RepositoryUtils.parseLong(idUsuario);
        if (idNum == null) {
            return false;
        }

        String sql = "SELECT 1 FROM USUARIO WHERE ID_USUARIO = ?";
        try (Connection cn = OracleInfra.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, idNum);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error validando existencia de usuario", e);
        }
    }

    /**
     * Cuenta préstamos activos de un usuario.
     * @param usuarioId identificador del usuario
    * @return {@code long} cantidad de préstamos activos
     */
    static long getPrestamosActivos(String usuarioId) {
        Long idNum = RepositoryUtils.parseLong(usuarioId);
        if (idNum == null) {
            return 0;
        }
        String sql = "SELECT COUNT(*) FROM PRESTAMO WHERE ID_USUARIO = ? AND ID_ESTADO != 2";
        try (Connection cn = OracleInfra.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, idNum);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error contando préstamos activos", e);
        }
    }

    /**
     * Elimina un usuario si está inactivo y sin préstamos activos.
     * @param usuarioId identificador del usuario
    * @return {@code boolean} {@code true} si se eliminó
     */
    static boolean deleteUsuarioIfInactive(String usuarioId) {
        Long idNum = RepositoryUtils.parseLong(usuarioId);
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
        try (Connection cn = OracleInfra.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, idNum);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error eliminando usuario inactivo", e);
        }
    }

    /**
     * Verifica duplicidad de usuario por identidad y correo.
     * @param nombre nombre
     * @param apellidoPaterno apellido paterno
     * @param apellidoMaterno apellido materno
     * @param email correo
    * @return {@code boolean} {@code true} si ya existe
     */
    static boolean existsUsuarioByIdentity(String nombre, String apellidoPaterno, String apellidoMaterno, String email) {
        String sql = """
                SELECT 1
                FROM USUARIO
                WHERE UPPER(TRIM(NOMBRE)) = UPPER(TRIM(?))
                  AND UPPER(TRIM(APELLIDO_P)) = UPPER(TRIM(?))
                  AND UPPER(TRIM(APELLIDO_M)) = UPPER(TRIM(?))
                  AND UPPER(TRIM(CORREO)) = UPPER(TRIM(?))
                """;

        try (Connection cn = OracleInfra.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, nombre);
            ps.setString(2, apellidoPaterno);
            ps.setString(3, apellidoMaterno);
            ps.setString(4, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw RepositoryExceptionHandler.sqlException("Error validando duplicidad de usuario", e);
        }
    }
}
