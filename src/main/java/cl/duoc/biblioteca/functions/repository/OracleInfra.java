package cl.duoc.biblioteca.functions.repository;

import java.sql.Connection;
import java.sql.SQLException;

import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;

final class OracleInfra {

    private static final String DEFAULT_TNS_ALIAS = "bdlibreriacn2_high";
    private static final String DEFAULT_WALLET_PATH = "./Wallet_BDLIBRERIACN2";
    private static final int POOL_INITIAL_SIZE = 2;
    private static final int POOL_MIN_SIZE = 2;
    private static final int POOL_MAX_SIZE = 8;
    private static final int POOL_TIMEOUT_CHECK_INTERVAL = 30;
    private static final int POOL_INACTIVE_TIMEOUT = 120;
    private static final boolean POOL_VALIDATE_ON_BORROW = true;

    private static volatile PoolDataSource poolDataSource;

    private OracleInfra() {}

    /**
     * Obtiene una conexión desde el pool Oracle.
    * @return {@link Connection} conexión activa
     * @throws SQLException si falla la conexión
     */
    static Connection getConnection() throws SQLException {
        return getPoolDataSource().getConnection();
    }

    /**
     * Devuelve el pool Oracle inicializado.
    * @return {@link PoolDataSource} pool de conexiones
     * @throws SQLException si falla la inicialización
     */
    private static synchronized PoolDataSource getPoolDataSource() throws SQLException {
        if (poolDataSource == null) {
            poolDataSource = buildPoolDataSource();
        }
        return poolDataSource;
    }

    /**
     * Construye el pool de conexiones Oracle.
    * @return {@link PoolDataSource} pool configurado
     * @throws SQLException si falla la configuración
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
     * Resuelve la URL JDBC a usar.
    * @return {@link String} URL JDBC Oracle
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
     * Obtiene una variable de entorno obligatoria.
     * @param key nombre de la variable
    * @return {@link String} valor de la variable
     */
    private static String getenvRequired(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Falta variable de entorno requerida: " + key);
        }
        return value;
    }

    /**
     * Obtiene la primera variable de entorno disponible.
     * @param keys variables candidatas
    * @return {@link String} primer valor no vacío
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
     * Obtiene una variable de entorno o un valor por defecto.
     * @param key nombre de la variable
     * @param defaultValue valor por defecto
    * @return {@link String} valor encontrado o por defecto
     */
    private static String getenvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
