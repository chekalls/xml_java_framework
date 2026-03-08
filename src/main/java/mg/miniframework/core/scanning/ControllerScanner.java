package mg.miniframework.core.scanning;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletContext;
import mg.miniframework.logging.LogManager;
import mg.miniframework.logging.LogManager.LogStatus;

public class ControllerScanner {

    private final LogManager logManager;

    public ControllerScanner() {
        this.logManager = new LogManager();
    }

    public List<Class<?>> findControllers(ServletContext servletContext, Class<?> annotationClass) throws Exception {
        List<Class<?>> result = new ArrayList<>();

        List<Class<?>> servletContextScanResult = scanPackagesForAnnotation(servletContext, annotationClass,
                "Controlleur", "controller", "controllers");
        if (!servletContextScanResult.isEmpty()) {
            try {
                logManager.insertLog("Controllers found via ServletContext scan: " + servletContextScanResult.size(), LogStatus.INFO);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return servletContextScanResult;
        }

        try {
            URL resource = getClass().getClassLoader().getResource("");

            if (resource == null) {
                logManager.insertLog("ClassLoader.getResource returned null - fallback to servlet scan", LogStatus.WARN);
                return scanPackagesForAnnotation(servletContext, annotationClass, "Controlleur", "controller", "controllers");
            }

            String protocol = resource.getProtocol();
            logManager.insertLog("Resource protocol: " + protocol + ", path: " + resource.getPath(), LogStatus.INFO);

            if ("vfs".equals(protocol)) {
                logManager.insertLog("VFS detected - using package scanning", LogStatus.INFO);
                return scanPackagesForAnnotation(servletContext, annotationClass, "Controlleur", "controller", "controllers");
            }

            Path basePath = Paths.get(resource.toURI());
            logManager.insertLog("Scanning base path: " + basePath.toString(), LogStatus.INFO);

            Files.walkFileTree(basePath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toString().endsWith(".class")) {
                        String className = basePath.relativize(file).toString().replace(File.separatorChar, '.')
                                .replace(".class", "");
                        try {
                            Class<?> clazz = loadClassSafely(className);
                            if (clazz != null && hasAnnotationByName(clazz, annotationClass.getName())) {
                                result.add(clazz);
                                logManager.insertLog("Controller found: " + className, LogStatus.INFO);
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            if (result.isEmpty()) {
                List<Class<?>> fallback = scanPackagesForAnnotation(servletContext, annotationClass, "Controlleur", "controller", "controllers");
                if (!fallback.isEmpty()) {
                    return fallback;
                }
            }
        } catch (Exception e) {
            logManager.insertLog("Error scanning for controllers: " + e.getMessage(), LogStatus.ERROR);
            e.printStackTrace();
            return scanPackagesForAnnotation(servletContext, annotationClass, "Controlleur", "controller", "controllers");
        }

        return result;
    }

    private List<Class<?>> scanPackagesForAnnotation(ServletContext ctx, Class<?> annotationClass, String... packageNames) {
        List<Class<?>> result = new ArrayList<>();

        try {
            for (String pkg : packageNames) {
                String candidate = "/WEB-INF/classes/" + pkg + "/";
                if (ctx.getResource(candidate) != null) {
                    scanServletContextRecursively(ctx, candidate, annotationClass, result);
                }
            }

            // default scan of WEB-INF/classes
            scanServletContextRecursively(ctx, "/WEB-INF/classes/", annotationClass, result);

        } catch (Exception e) {
            try {
                logManager.insertLog("Error scanning servlet context for controllers: " + e.getMessage(), LogStatus.ERROR);
            } catch (IOException ioe) {
                // ignore
            }
        }

        return result;
    }

    private void scanServletContextRecursively(ServletContext ctx, String path, Class<?> annotationClass, List<Class<?>> result) {
        try {
            @SuppressWarnings("unchecked")
            java.util.Set<String> resources = ctx.getResourcePaths(path);
            if (resources == null) {
                return;
            }
            for (String resourcePath : resources) {
                if (resourcePath.endsWith("/")) {
                    scanServletContextRecursively(ctx, resourcePath, annotationClass, result);
                    continue;
                }

                if (!resourcePath.endsWith(".class")) {
                    continue;
                }

                String className = resourcePath.replace("/WEB-INF/classes/", "").replace('/', '.').replace(".class", "");
                Class<?> clazz = loadClassSafely(className);
                if (clazz != null && hasAnnotationByName(clazz, annotationClass.getName())) {
                    result.add(clazz);
                }
            }
        } catch (Exception e) {
            // ignore individual failures
        }
    }

    private Class<?> loadClassSafely(String className) {
        try {
            if (className == null || className.isBlank()) {
                return null;
            }
            Class<?> clazz = Class.forName(className);
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
                return null;
            }
            return clazz;
        } catch (Throwable t) {
            return null;
        }
    }

    private boolean hasAnnotationByName(Class<?> clazz, String annotationClassName) {
        try {
            return java.util.Arrays.stream(clazz.getDeclaredAnnotations())
                    .anyMatch(a -> a.annotationType().getName().equals(annotationClassName));
        } catch (Throwable t) {
            return false;
        }
    }
}
