package cl.duoc.biblioteca.functions.function;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import cl.duoc.biblioteca.functions.repository.OracleStore;

public class NotificacionesFunction {

    @FunctionName("notificaciones")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET},
                    authLevel = AuthorizationLevel.ANONYMOUS,
                    route = "notificaciones/{idUsuario?}")
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context
    ) {
        try {
            String idUsuario = extractIdUsuario(request);
            if (idUsuario == null || idUsuario.isBlank()) {
                return request.createResponseBuilder(HttpStatus.OK)
                        .body(OracleStore.getNotificaciones())
                        .build();
            }
            return request.createResponseBuilder(HttpStatus.OK)
                    .body(OracleStore.getNotificacionesByUsuario(idUsuario))
                    .build();
        } catch (Exception ex) {
            context.getLogger().log(Level.SEVERE, "Error en notificaciones function", ex);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno", "detalle", ex.getMessage()))
                    .build();
        }
    }

    private String extractIdUsuario(HttpRequestMessage<Optional<String>> request) {
        String path = request.getUri().getPath();
        String marker = "/notificaciones";

        int idx = path.indexOf(marker);
        if (idx < 0) {
            return null;
        }

        String tail = path.substring(idx + marker.length());
        if (tail.isBlank() || "/".equals(tail)) {
            return null;
        }

        if (tail.startsWith("/")) {
            tail = tail.substring(1);
        }

        int slash = tail.indexOf('/');
        if (slash >= 0) {
            tail = tail.substring(0, slash);
        }

        return tail.isBlank() ? null : tail;
    }
}
