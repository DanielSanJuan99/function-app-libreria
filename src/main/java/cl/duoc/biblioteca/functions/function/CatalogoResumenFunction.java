package cl.duoc.biblioteca.functions.function;

import java.util.List;
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

import cl.duoc.biblioteca.functions.domain.Libro;
import cl.duoc.biblioteca.functions.repository.OracleStore;

public class CatalogoResumenFunction {

    @FunctionName("resumenCatalogo")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET},
                    authLevel = AuthorizationLevel.ANONYMOUS,
                    route = "resumen/catalogo")
            HttpRequestMessage<String> request,
            final ExecutionContext context) {

        try {
            List<Libro> libros = OracleStore.getLibros();
            int totalLibros = libros.size();
            int totalCopias = libros.stream()
                    .map(Libro::getCopiasTotales)
                    .filter(v -> v != null)
                    .mapToInt(Integer::intValue)
                    .sum();
            int copiasDisponibles = libros.stream()
                    .map(Libro::getCopiasDisponible)
                    .filter(v -> v != null)
                    .mapToInt(Integer::intValue)
                    .sum();

            Map<String, Object> body = Map.of(
                    "autores", OracleStore.getAutores().size(),
                    "libros", totalLibros,
                    "copiasTotales", totalCopias,
                    "copiasDisponibles", copiasDisponibles
            );

            return request.createResponseBuilder(HttpStatus.OK)
                    .body(body)
                    .build();
        } catch (Exception ex) {
            context.getLogger().log(Level.SEVERE, "Error en resumen catalogo", ex);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno", "detalle", ex.getMessage()))
                    .build();
        }
    }
}
