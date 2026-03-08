package mg.miniframework.core.config;


import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import jakarta.servlet.ServletContext;

public class ConfigLoader {

    public Map<String, String> getAllProperties(ServletContext context) {
        Map<String, String> map = new HashMap<>();

        Set<String> propertyFiles = context.getResourcePaths("/WEB-INF/config/");

        if (propertyFiles != null) {
            for (String path : propertyFiles) {
                if (path.endsWith(".properties")) {
                    loadPropertiesFromServletPath(context, path, map);
                }
            }
        }

        loadPropertiesFromServletPath(context, "/WEB-INF/config/framework.properties", map);
        loadPropertiesFromServletPath(context, "/WEB-INF/classes/framework.properties", map);

        loadPropertiesFromClasspath("framework.properties", map);

        return map;
    }

    private void loadPropertiesFromServletPath(
            ServletContext context,
            String path,
            Map<String, String> target) {
        try (InputStream in = context.getResourceAsStream(path)) {
            if (in == null) {
                return;
            }

            Properties props = new Properties();
            props.load(in);
            props.forEach((k, v) -> target.put(k.toString(), v.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPropertiesFromClasspath(String resourceName, Map<String, String> target) {
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();

        if (contextLoader != null) {
            try (InputStream in = contextLoader.getResourceAsStream(resourceName)) {
                if (in != null) {
                    Properties props = new Properties();
                    props.load(in);
                    props.forEach((k, v) -> target.put(k.toString(), v.toString()));
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (InputStream in = ConfigLoader.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) {
                return;
            }

            Properties props = new Properties();
            props.load(in);
            props.forEach((k, v) -> target.put(k.toString(), v.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
