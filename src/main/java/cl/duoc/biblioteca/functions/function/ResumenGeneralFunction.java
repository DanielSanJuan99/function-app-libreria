package cl.duoc.biblioteca.functions.function;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import cl.duoc.biblioteca.functions.domain.Prestamo;
import cl.duoc.biblioteca.functions.repository.OracleStore;

// Función Azure para manejar consultas HTTP GET sobre usuarios y préstamos.
// Permite consultar usuarios, préstamos, y realizar búsquedas por ID.
public class ResumenGeneralFunction {

    // Función Azure para manejar consultas HTTP GET en el endpoint /resumen/general
    @FunctionName("resumenGeneral")
    // Maneja solicitudes GET para obtener un resumen general de préstamos y usuarios
    public HttpResponseMessage run(
        @HttpTrigger(
            name = "req",
            methods = { HttpMethod.GET },
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "resumen/general"
        ) HttpRequestMessage<String> request,
        final ExecutionContext context) {

        try {
            List<Prestamo> prestamos = OracleStore.getPrestamos();

            long activos = prestamos.stream()
                .filter(p -> "ACTIVO".equalsIgnoreCase(normalizeEstado(p.getEstado())))
                .count();
            long devueltos = prestamos.stream()
                .filter(p -> "DEVUELTO".equalsIgnoreCase(normalizeEstado(p.getEstado())))
                .count();
            long atrasados = prestamos.stream()
                .filter(p -> "ATRASADO".equalsIgnoreCase(normalizeEstado(p.getEstado())))
                .count();

            long usuariosActivos = OracleStore.getUsuarios().stream()
                .filter(u -> Boolean.TRUE.equals(u.isActivo()))
                .count();
            long usuariosInactivos = OracleStore.getUsuarios().stream()
                .filter(u -> !Boolean.TRUE.equals(u.isActivo()))
                .count();

            // Construcción del cuerpo de la respuesta con el resumen de préstamos y usuarios
            Map<String, Object> body = Map.of(
                "prestamos", Map.of(
                    "total", prestamos.size(),
                    "prestados", activos,
                    "devueltos", devueltos,
                    "atrasados", atrasados),
                "usuarios", Map.of(
                    "activos", usuariosActivos,
                    "inactivos", usuariosInactivos));

            return request.createResponseBuilder(HttpStatus.OK)
                .body(body)
                .build();
        } catch (Exception ex) {
            context.getLogger().log(Level.SEVERE, "Error en resumen general", ex);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "Error interno",
                    "detalle", ex.getMessage()))
                .build();
        }
    }

    // Método auxiliar para normalizar el estado de un préstamo, tratando null como "ACTIVO" y convirtiendo a mayúsculas
    private String normalizeEstado(String estado) {
        if (estado == null) {
            return "ACTIVO";
        }
        return estado.trim().toUpperCase(Locale.ROOT);
    }
}
