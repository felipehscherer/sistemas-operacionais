package server;

/***
 * Permite modelar as necessidades do servidor de maneira fácil e rodar abordagens diferentes com facilidade. Exemplo:
 * ServerConfig.getInstance().setCriticalSectionAccessControlEnabled(true); // Habilita a sessão crítica
 */
public class ServerConfig {
    private static ServerConfig instance;

    private boolean printLogEnabled;
    private boolean criticalSectionAccessControlEnabled;
    private Enum serverType;
    private Enum databaseType;
    private int port;

    public static enum SERVER_TYPES{
        THREADED, SELECTOR, PROCESS;
    }

    public static enum DB_TYPES{
        MEMORY, DISK, CACHE;
    }

    // Construtor privado para evitar instância fora da classe
    private ServerConfig() {
        // Configurações padrão
        this.printLogEnabled = false;
        this.criticalSectionAccessControlEnabled = true;
        this.serverType = SERVER_TYPES.THREADED; // Exemplo de servidor padrão
        this.databaseType = DB_TYPES.DISK;
        this.port = 8080;
    }


    // Método para obter a instância do Singleton
    public static ServerConfig getInstance() {
        if (instance == null) {
            instance = new ServerConfig();
        }
        return instance;
    }

    // Getters e Setters para as variáveis de configuração
    public boolean isPrintLogEnabled() {
        return printLogEnabled;
    }

    public void setPrintLogEnabled(boolean printLogEnabled) {
        this.printLogEnabled = printLogEnabled;
    }

    public boolean isCriticalSectionAccessControlEnabled() {
        return criticalSectionAccessControlEnabled;
    }

    public void setCriticalSectionAccessControlEnabled(boolean criticalSectionAccessControlEnabled) {
        this.criticalSectionAccessControlEnabled = criticalSectionAccessControlEnabled;
    }

    public Enum getServerType() {
        return serverType;
    }

    public void setServerType(Enum serverType) {
        this.serverType = serverType;
    }

    public Enum getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(Enum databaseType) {
        this.databaseType = databaseType;
    }
}
