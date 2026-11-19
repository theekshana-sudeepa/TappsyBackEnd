package util;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {

    private static final SessionFactory SESSION_FACTORY;

    static {
        try {
            Configuration configuration = new Configuration();
            configuration.configure("hibernate.cfg.xml");

            applyEnvOverride(configuration, "DB_URL", "hibernate.connection.url");
            applyEnvOverride(configuration, "DB_USER", "hibernate.connection.username");
            applyEnvOverride(configuration, "DB_PASSWORD", "hibernate.connection.password");

            StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                    .applySettings(configuration.getProperties())
                    .build();
            SESSION_FACTORY = configuration.buildSessionFactory(registry);
        } catch (Throwable ex) {
            System.err.println("Initial SessionFactory creation failed: " + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static void applyEnvOverride(Configuration configuration, String envKey, String hibernateKey) {
        String value = System.getProperty(envKey);
        if (value == null) {
            value = System.getenv(envKey);
        }
        if (value != null && !value.trim().isEmpty()) {
            configuration.setProperty(hibernateKey, value.trim());
        }
    }

    public static SessionFactory getSessionFactory() {
        return SESSION_FACTORY;
    }

    public static void shutdown() {
        getSessionFactory().close();
    }
}
