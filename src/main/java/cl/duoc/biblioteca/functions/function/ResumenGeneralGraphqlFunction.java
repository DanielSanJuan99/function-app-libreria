package cl.duoc.biblioteca.functions.function;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import cl.duoc.biblioteca.functions.repository.OracleStore;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

/**
 * Función Azure para manejar consultas GraphQL sobre usuarios y préstamos.
 * Permite consultar usuarios, préstamos, y realizar búsquedas por ID.
 */
public class ResumenGeneralGraphqlFunction {

    // Se utiliza un ObjectMapper para parsear el body JSON de las solicitudes
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    // Definición del esquema GraphQL para usuarios y préstamos
    private static final String SCHEMA = """
        type Query {
            usuarios: [Usuario!]!
            prestamos: [Prestamo!]!
            usuario(id: ID!): Usuario
            prestamo(id: ID!): Prestamo
        }

        type Usuario {
            id: ID
            nombre: String
            apellidoPaterno: String
            apellidoMaterno: String
            email: String
            activo: Boolean
        }

        type Prestamo {
            id: ID
            idUsuario: ID
            idLibro: ID
            fechaPrestamo: String
            fechaDevolucion: String
            estado: String
        }
        """;
    // Construcción del objeto GraphQL a partir del esquema y los dataFetcher que consultan el OracleStore
    private static final GraphQL GRAPHQL = buildGraphQL();

    // Función Azure para manejar consultas GraphQL sobre usuarios y préstamos
    @FunctionName("graphqlGeneral")
    // Maneja solicitudes POST con consultas GraphQL en el endpoint /graphql/general
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    authLevel = AuthorizationLevel.ANONYMOUS,
                    route = "graphql/general")
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context
    ) {
        try {
            if (request.getBody().isEmpty()) {
                return badRequest(request, "Body requerido con query GraphQL");
            }

            // Parseo del body JSON para extraer la consulta, el nombre de la operación y las variables
            Map<String, Object> payload = OBJECT_MAPPER.readValue(request.getBody().get(), new TypeReference<>() {});
            String query = Objects.toString(payload.get("query"), "");
            String operationName = payload.get("operationName") == null ? null : payload.get("operationName").toString();
            Map<String, Object> variables = asMap(payload.get("variables"));

            // Construcción del ExecutionInput para GraphQL con la consulta, operación y variables
            ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                    .query(query)
                    .operationName(operationName)
                    .variables(variables)
                    .build();

            // Ejecución de la consulta GraphQL y obtención del resultado
            ExecutionResult result = GRAPHQL.execute(executionInput);
            Map<String, Object> body = result.toSpecification();

            return request.createResponseBuilder(HttpStatus.OK)
                    .body(body)
                    .build();
        } catch (Exception ex) {
            context.getLogger().log(Level.SEVERE, "Error en graphql general", ex);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "data", Map.of(),
                            "errors", List.of(Map.of("message", "Error interno", "detalle", ex.getMessage()))
                    ))
                    .build();
        }
    }

    // Método auxiliar para construir una respuesta de error 400 Bad Request con formato GraphQL
    private HttpResponseMessage badRequest(HttpRequestMessage<Optional<String>> request, String message) {
        return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "data", Map.of(),
                        "errors", List.of(Map.of("message", message))
                ))
                .build();
    }

    // Método para construir el objeto GraphQL a partir del esquema y los dataFetcher que consultan el OracleStore
    private static GraphQL buildGraphQL() {
        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(SCHEMA);

        RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring()
                .type("Query", builder -> builder
                        .dataFetcher("usuarios", env -> OracleStore.getUsuarios())
                        .dataFetcher("prestamos", env -> OracleStore.getPrestamos())
                        .dataFetcher("usuario", env -> OracleStore.getUsuario(env.getArgument("id")))
                        .dataFetcher("prestamo", env -> OracleStore.getPrestamo(env.getArgument("id")))
                )
                .build();

        GraphQLSchema graphQLSchema = new SchemaGenerator().makeExecutableSchema(typeRegistry, wiring);
        return GraphQL.newGraphQL(graphQLSchema).build();
    }

    // Método auxiliar para convertir un objeto a Map<String, Object> si es posible, o retornar un Map vacío
    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> raw) {
            return (Map<String, Object>) raw;
        }
        return Collections.emptyMap();
    }
}
