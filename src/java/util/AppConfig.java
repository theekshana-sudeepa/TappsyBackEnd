package util;

/**
 * Centralized runtime configuration loader reading from JVM system properties
 * or OS environment variables with safe local defaults.
 */
public final class AppConfig {

    private static final String DEFAULT_BASE_URL = "http://localhost:8080";
    private static final String DEFAULT_CONTEXT_PATH = "/TappsyBackEnd";

    private AppConfig() {
    }

    public static String get(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            value = System.getenv(key);
        }
        return (value != null && !value.trim().isEmpty()) ? value.trim() : defaultValue;
    }

    public static String getPublicBaseUrl() {
        return stripTrailingSlash(get("PUBLIC_BASE_URL", DEFAULT_BASE_URL));
    }

    public static String getLocalBaseUrl() {
        return stripTrailingSlash(get("LOCAL_BASE_URL", DEFAULT_BASE_URL));
    }

    public static String getContextPath() {
        String path = get("APP_CONTEXT_PATH", DEFAULT_CONTEXT_PATH);
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return stripTrailingSlash(path);
    }

    private static String stripTrailingSlash(String value) {
        if (value != null && value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
