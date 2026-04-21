package tn.rouhfan.tools;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class to load application configurations from config.properties
 */
public class Config {
    private static final String CONFIG_FILE = "config.properties";
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = Config.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                System.err.println("⚠️ Warning: " + CONFIG_FILE + " not found in resources. Using default/placeholder values.");
            } else {
                properties.load(input);
            }
        } catch (IOException ex) {
            System.err.println("❌ Error loading " + CONFIG_FILE + ": " + ex.getMessage());
        }
    }

    /**
     * Get a property value by key
     * @param key The key to look up
     * @return The value, or a placeholder if not found
     */
    public static String get(String key) {
        return properties.getProperty(key, "YOUR_STRIPE_SECRET_KEY");
    }

    /**
     * Get a property with a specific default value
     */
    public static String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}
