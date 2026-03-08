package mg.miniframework.controller;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Pattern;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import mg.miniframework.annotation.Controller;
import mg.miniframework.annotation.JsonUrl;
import mg.miniframework.annotation.Route;
import mg.miniframework.modules.*;
import mg.miniframework.modules.LogManager.LogStatus;
import mg.miniframework.modules.security.SecurityManager;
import mg.miniframework.modules.service.CachedService;
import mg.miniframework.modules.service.FrameworkService;
import mg.miniframework.ui.base.HtmlComponent;
import mg.miniframework.modules.security.AuthenticationProvider;
import mg.miniframework.utils.RoutePatternUtils;

@WebServlet(name = "FrontControllerServlet", urlPatterns = "/")
@MultipartConfig
public class FrontControllerServlet extends HttpServlet {
    private String baseFile;
    private MethodManager methodeManager;
    private LogManager logManager;
    private ConfigLoader configLoader;
    private ContentRenderManager contentRenderManager;
    private MetricsManager metricsManager;
    private SecurityManager securityManager;
    private CachedService cachedService;

    @Override
    public void init() throws ServletException {
        this.methodeManager = new MethodManager();
        this.logManager = new LogManager();
        this.configLoader = new ConfigLoader();
        this.contentRenderManager = new ContentRenderManager();
        this.metricsManager = new MetricsManager();
        this.securityManager = new SecurityManager();
        this.cachedService = new CachedService();

        ServletContext ctx = getServletContext();
        String userAttName = ctx.getInitParameter("security.user.attributeName");
        String rolesAttNAme = ctx.getInitParameter("security.role.attributeName");

        try {
            logManager.insertLog("user att name :"+userAttName, LogStatus.INFO);
            logManager.insertLog("roles att name :"+rolesAttNAme, LogStatus.INFO);
        } catch (IOException e) {
            e.printStackTrace();
        }

        securityManager.setConnectedUserVarName(userAttName);
        securityManager.setUserRolesVarName(rolesAttNAme);

        String securityEnabled = ctx.getInitParameter("security.enabled");
        if (securityEnabled != null) {
            securityManager.setEnabled(Boolean.parseBoolean(securityEnabled));
        }
        
        String loginUrl = ctx.getInitParameter("security.loginUrl");
        if (loginUrl != null && !loginUrl.isEmpty()) {
            securityManager.setLoginUrl(loginUrl);
        }
        
        String accessDeniedUrl =ctx.getInitParameter("security.accessDeniedUrl");
        if (accessDeniedUrl != null && !accessDeniedUrl.isEmpty()) {
            securityManager.setAccessDeniedUrl(accessDeniedUrl);
        }
        
        String authProviderClass = ctx.getInitParameter("security.authenticationProvider");
        if (authProviderClass != null && !authProviderClass.isEmpty()) {
            try {
                Class<?> providerClass = Class.forName(authProviderClass);
                AuthenticationProvider provider = (AuthenticationProvider) providerClass.getDeclaredConstructor().newInstance();
                securityManager.setAuthenticationProvider(provider);
                logManager.insertLog("Authentication provider loaded: " + authProviderClass, LogStatus.INFO);
            } catch (Exception e) {
                try {
                    logManager.insertLog("Failed to load authentication provider: " + e.getMessage(), LogStatus.ERROR);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        metricsManager.incrementRequestCount();

        ServletContext servletContext = req.getServletContext();
        String requestURI = req.getRequestURI();
        String contextPath = req.getContextPath();
        String relativePath = requestURI.substring(contextPath.length());
        String realPath = servletContext.getRealPath(relativePath);

        if (relativePath.equals("/metrics")) {
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().print(metricsManager.exportMetrics());
            long duration = System.currentTimeMillis() - startTime;
            metricsManager.addRequestDuration(duration);
            return;
        }

        Map<String, String> requestData = Map.of("requestURI", requestURI, "contextPath", contextPath, "relativePath",
                relativePath, "realPath", realPath);

        logManager.insertLog("request data :" + contentRenderManager.convertToJson(requestData), LogStatus.DEBUG);

        try {
            if (relativePath.endsWith(".jsp")) {
                RequestDispatcher jspDispatcher = req.getRequestDispatcher(relativePath);
                if (jspDispatcher == null) {
                    throw new ServletException("Impossible de forward vers la JSP : " + relativePath);
                }
                jspDispatcher.forward(req, resp);
                return;
            }

            if (relativePath.matches("(?i).*\\.(css|js|png|jpg|jpeg|gif|svg)$")) {
                File resource = realPath == null ? null : new File(realPath);
                if (resource != null && resource.exists() && !resource.isDirectory()) {
                    RequestDispatcher defaultDispatcher = servletContext.getNamedDispatcher("default");
                    if (defaultDispatcher != null) {
                        req.setAttribute("jakarta.servlet.include.request_uri", relativePath);
                        req.setAttribute("jakarta.servlet.include.servlet_path", relativePath);
                        defaultDispatcher.forward(req, resp);
                    } else {
                        servletContext.getRequestDispatcher(relativePath).forward(req, resp);
                    }
                    return;
                }
            }
            if (servletContext.getAttribute("settingMap") == null) {
                Map<String, String> settings = configLoader.getAllProperties(servletContext);
                settings.forEach((k, v) -> {
                    try {
                        logManager.insertLog("config found [" + k + ":" + v + "]", LogStatus.INFO);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                servletContext.setAttribute("settingMap", settings);
            }

            Map<String, String> mapSetting = (Map<String, String>) servletContext.getAttribute("settingMap");

            if (mapSetting.containsKey("jsp_base_path")) {
                baseFile = mapSetting.get("jsp_base_path");
                contentRenderManager.setBaseJspPath(baseFile);
            }

            if(mapSetting.containsKey("xml_views_template_path")){
                HtmlComponent.setBasePath(mapSetting.get("xml_views_template_path"),servletContext);
                HtmlComponent.setServletContext(servletContext);
            }

            if (mapSetting.containsKey("upload_path")) {
                methodeManager.setFileSavePath(mapSetting.get("upload_path"));
            }
            
            if (servletContext.getAttribute("rolePermissionLoader") == null) {
                mg.miniframework.modules.security.RolePermissionLoader loader = 
                    new mg.miniframework.modules.security.RolePermissionLoader();
                loader.loadFromConfig(mapSetting);
                servletContext.setAttribute("rolePermissionLoader", loader);
                securityManager.setRolePermissionLoader(loader);
                
                injectRolePermissionLoader(loader);
                
                try {
                    logManager.insertLog("Security roles and permissions loaded from config", LogStatus.INFO);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                mg.miniframework.modules.security.RolePermissionLoader loader = 
                    (mg.miniframework.modules.security.RolePermissionLoader) servletContext.getAttribute("rolePermissionLoader");
                securityManager.setRolePermissionLoader(loader);
                
                injectRolePermissionLoader(loader);
            }

            if (servletContext.getAttribute("routeMap") == null) {
                RouteMap routeMap = new RouteMap();
                logManager.insertLog("Scanning for controllers...", LogStatus.INFO);
                List<Class<?>> controllers = trouverClassesAvecAnnotation(Controller.class);
                logManager.insertLog("Controllers found: " + controllers.size(), LogStatus.INFO);
                for (Class<?> c : controllers) {
                    routeMap.addController(c);
                    List<Class<? extends FrameworkService>> controllerServices = RouteMap.getControllerServices(c);
                    for (Class<? extends FrameworkService> s : controllerServices) {
                        cachedService.registerControllerService(c, s);
                    }
                }
                logManager.insertLog("Total routes registered: " + routeMap.getUrlMethodsMap().size(), LogStatus.INFO);
                servletContext.setAttribute("routeMap", routeMap);
            }

            RouteMap routeMap = (RouteMap) servletContext.getAttribute("routeMap");

            Url url = new Url();
            url.setMethod(Url.Method.valueOf(req.getMethod()));
            url.setUrlPath(relativePath);

            Map<Url, Pattern> routePatterns = new HashMap<>();

            for (Map.Entry<Url, CachedMethodInfo> entry : routeMap.getUrlMethodsMap().entrySet()) {
                routePatterns.put(
                        entry.getKey(),
                        RoutePatternUtils.convertRouteToPattern(entry.getKey().getUrlPath()));
            }

            Integer status = gererRoutes(
                    url,
                    routePatterns,
                    routeMap.getUrlMethodsMap(),
                    req,
                    resp);

            if (status.equals(RouteStatus.NOT_FOUND.getCode())) {
                print404(req, resp, relativePath, req.getMethod(), routeMap);
            }

            if (status.equals(RouteStatus.RETURN_TYPE_UNKNOWN.getCode())) {
                metricsManager.incrementErrorCount();
            }

            long duration = System.currentTimeMillis() - startTime;
            metricsManager.addRequestDuration(duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            metricsManager.addRequestDuration(duration);
            metricsManager.incrementErrorCount();
            try {
                logManager.insertLog("Erreur interne: " + e.toString(), LogStatus.ERROR);
                java.io.StringWriter sw = new java.io.StringWriter();
                e.printStackTrace(new java.io.PrintWriter(sw));
                logManager.insertLog(sw.toString(), LogStatus.ERROR);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("text/plain;charset=UTF-8");
            try {
                resp.getWriter().println("Erreur interne : " + e.toString());
                java.io.StringWriter sw2 = new java.io.StringWriter();
                e.printStackTrace(new java.io.PrintWriter(sw2));
                resp.getWriter().println(sw2.toString());
            } catch (java.io.IOException ioex) {
                ioex.printStackTrace();
            }
        }
    }

    private Integer gererRoutes(
            Url requestURL,
            Map<Url, Pattern> routes,
            Map<Url, CachedMethodInfo> methodsMap,
            HttpServletRequest req,
            HttpServletResponse resp) throws IOException {

        PrintWriter out = resp.getWriter();

        for (Map.Entry<Url, Pattern> entry : routes.entrySet()) {
            Url routeURL = entry.getKey();

            if (entry.getValue().matcher(requestURL.getUrlPath()).matches()
                    && requestURL.getMethod() == routeURL.getMethod()) {

                try {
                    try {
                        logManager.insertLog("Route matched: " + routeURL.getUrlPath() + " (http: " + routeURL.getMethod() + ")", LogStatus.DEBUG);
                    } catch (IOException ioex) {
                        ioex.printStackTrace();
                    }
                    CachedMethodInfo cachedInfo = methodsMap.get(routeURL);
                    Method method = cachedInfo.getMethod();
                    System.out.println("""
                            testing roles =====================================================
                            """);
                    if (!securityManager.isAccessAllowed(method, req, resp)) {
                        return RouteStatus.RETURN_TYPE_UNKNOWN.getCode();
                    }

                    Map<String, String> pathParams = RoutePatternUtils.extractPathParams(
                            routeURL.getUrlPath(),
                            requestURL.getUrlPath());

                    try {
                        logManager.insertLog("Invoking method: " + method.getDeclaringClass().getName() + "#" + method.getName(), LogStatus.DEBUG);
                    } catch (IOException ioex) {
                        ioex.printStackTrace();
                    }

                    Object result = methodeManager.invokeCorrespondingMethod(
                            cachedInfo,
                            method.getDeclaringClass(),
                            pathParams,cachedService,
                            req,
                            resp);

                    try {
                        logManager.insertLog("Method invoked successfully: " + method.getDeclaringClass().getName() + "#" + method.getName(), LogStatus.DEBUG);
                    } catch (IOException ioex) {
                        ioex.printStackTrace();
                    }

                    if (method.isAnnotationPresent(JsonUrl.class)) {
                        resp.setContentType("application/json;charset=UTF-8");
                        out.print(contentRenderManager.convertToJson(result));
                        out.flush();
                        return RouteStatus.RETURN_JSON.getCode();
                    }

                    return contentRenderManager.renderContent(result, req, resp);
                } catch (Exception e) {
                    try {
                        logManager.insertLog("Erreur interne: " + e.toString(), LogStatus.ERROR);
                        java.io.StringWriter sw = new java.io.StringWriter();
                        e.printStackTrace(new java.io.PrintWriter(sw));
                        logManager.insertLog(sw.toString(), LogStatus.ERROR);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    out.print("Erreur interne : " + e.toString());
                    out.print("<pre>");
                    java.io.StringWriter sw2 = new java.io.StringWriter();
                    e.printStackTrace(new java.io.PrintWriter(sw2));
                    out.print(sw2.toString().replaceAll("<","&lt;"));
                    out.print("</pre>");
                    return RouteStatus.RETURN_TYPE_UNKNOWN.getCode();
                }
            }
        }

        return RouteStatus.NOT_FOUND.getCode();
    }

    private void print404(HttpServletRequest req, HttpServletResponse resp,
            String urlPath, String httpMethod,
            RouteMap routeMap) throws IOException {

        metricsManager.incrementErrorCount();

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        resp.setContentType("text/html;charset=UTF-8");

        PrintWriter out = resp.getWriter();
        out.println("<html><body>");
        out.println("<h1>404 - Page non trouvée</h1>");
        out.println("<p><b>" + httpMethod + "</b> " + urlPath + "</p>");
        
        try {
            logManager.insertLog("Printing 404 page. Available routes count: " + 
                routeMap.getUrlMethodsMap().size(), LogStatus.DEBUG);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        out.println("<h3>Routes disponibles (" + routeMap.getUrlMethodsMap().size() + "):</h3>");
        out.println("<ul>");

        List<Map.Entry<Url, CachedMethodInfo>> sortedEntries = new ArrayList<>(
            routeMap.getUrlMethodsMap().entrySet()
        );
        
        sortedEntries.sort(Comparator.comparing(
            e -> e.getKey().getUrlPath(),
            String.CASE_INSENSITIVE_ORDER
        ));
        
        for (Map.Entry<Url, CachedMethodInfo> entry : sortedEntries) {
            out.println("<li>"
                    + entry.getKey().getMethod()
                    + " "
                    + entry.getKey().getUrlPath()
                    + "</li>");
        }
        
        out.println("</ul>");
        out.println("</body></html>");
        out.flush();
    }

    private List<Class<?>> trouverClassesAvecAnnotation(Class<?> annotationClass)
            throws Exception {

        List<Class<?>> result = new ArrayList<>();

        List<Class<?>> servletContextScanResult = scanPackagesForAnnotation(
                annotationClass,
                "Controlleur",
                "controller",
                "controllers");
        if (!servletContextScanResult.isEmpty()) {
            try {
                logManager.insertLog(
                        "Controllers found via ServletContext scan: " + servletContextScanResult.size(),
                        LogStatus.INFO);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return servletContextScanResult;
        }
        
        try {
            java.net.URL resource = getClass().getClassLoader().getResource("");
            
            if (resource == null) {
                logManager.insertLog("ClassLoader.getResource returned null - trying alternate method", LogStatus.WARN);
                return scanPackagesForAnnotation(annotationClass, "Controlleur", "controller", "controllers");
            }
            
            String protocol = resource.getProtocol();
            logManager.insertLog("Resource protocol: " + protocol + ", path: " + resource.getPath(), LogStatus.INFO);
            
            if ("vfs".equals(protocol)) {
                logManager.insertLog("VFS detected (WildFly) - using package scanning", LogStatus.INFO);
                return scanPackagesForAnnotation(annotationClass, "Controlleur", "controller", "controllers");
            }
            
            Path basePath = Paths.get(resource.toURI());
            logManager.insertLog("Scanning base path: " + basePath.toString(), LogStatus.INFO);

            Files.walkFileTree(basePath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {

                    if (file.toString().endsWith(".class")) {
                        String className = basePath.relativize(file)
                                .toString()
                                .replace(File.separatorChar, '.')
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
                List<Class<?>> fallbackResult = scanPackagesForAnnotation(
                        annotationClass,
                        "Controlleur",
                        "controller",
                        "controllers");
                if (!fallbackResult.isEmpty()) {
                    return fallbackResult;
                }
            }
        } catch (Exception e) {
            logManager.insertLog("Error scanning for controllers: " + e.getMessage(), LogStatus.ERROR);
            e.printStackTrace();
            return scanPackagesForAnnotation(annotationClass, "Controlleur", "controller", "controllers");
        }

        return result;
    }
    
    private List<Class<?>> scanPackagesForAnnotation(Class<?> annotationClass, String... packageNames) {
        List<Class<?>> result = new ArrayList<>();
        ServletContext ctx = getServletContext();
        
        try {
            logManager.insertLog("Scanning packages: " + String.join(", ", packageNames), LogStatus.INFO);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        
        try {
            logManager.insertLog("Starting recursive scan from /WEB-INF/classes/", LogStatus.INFO);
            scanServletContextRecursively(ctx, "/WEB-INF/classes/", annotationClass, result);
        } catch (Exception e) {
            try {
                logManager.insertLog("Error during recursive scan: " + e.getMessage(), LogStatus.ERROR);
                e.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        
        if (result.isEmpty()) {
            for (String packageName : packageNames) {
                try {
                    @SuppressWarnings("unchecked")
                    java.util.Set<String> resourcePaths = ctx.getResourcePaths("/WEB-INF/classes/" + packageName.replace('.', '/') + "/");
                    
                    if (resourcePaths != null) {
                        logManager.insertLog("Found resources in package " + packageName + ": " + resourcePaths.size(), LogStatus.INFO);
                        for (String resourcePath : resourcePaths) {
                            if (resourcePath.endsWith(".class")) {
                                String className = resourcePath
                                    .replace("/WEB-INF/classes/", "")
                                    .replace('/', '.')
                                    .replace(".class", "");
                                
                                try {
                                    Class<?> clazz = loadClassSafely(className);
                                    if (clazz != null && hasAnnotationByName(clazz, annotationClass.getName())) {
                                        result.add(clazz);
                                        logManager.insertLog("Controller found (via ServletContext): " + className, LogStatus.INFO);
                                    }
                                } catch (Throwable ignored) {
                                }
                            }
                        }
                    } else {
                        logManager.insertLog("No resources found in package: " + packageName, LogStatus.WARN);
                    }
                } catch (Exception e) {
                    try {
                        logManager.insertLog("Error scanning package " + packageName + ": " + e.getMessage(), LogStatus.WARN);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
        
        return result;
    }
    
    private void scanServletContextRecursively(ServletContext ctx, String path, 
            Class<?> annotationClass, List<Class<?>> result) throws IOException {
        
        @SuppressWarnings("unchecked")
        java.util.Set<String> resourcePaths = ctx.getResourcePaths(path);
        
        if (resourcePaths == null) {
            return;
        }
        
        logManager.insertLog("Scanning path: " + path + " (found " + resourcePaths.size() + " resources)", LogStatus.DEBUG);
        
        for (String resourcePath : resourcePaths) {
            if (resourcePath.endsWith("/")) {
                scanServletContextRecursively(ctx, resourcePath, annotationClass, result);
            } else if (resourcePath.endsWith(".class")) {
                String className = resourcePath
                    .replace("/WEB-INF/classes/", "")
                    .replace('/', '.')
                    .replace(".class", "");
                
                try {
                    Class<?> clazz = loadClassSafely(className);
                    if (clazz != null && hasAnnotationByName(clazz, annotationClass.getName())) {
                        result.add(clazz);
                        logManager.insertLog("Controller found: " + className, LogStatus.INFO);
                    }
                } catch (Throwable e) {
                    logManager.insertLog("Could not load class " + className + ": " + e.getMessage(), LogStatus.DEBUG);
                }
            }
        }
    }

    private Class<?> loadClassSafely(String className) {
        try {
            ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
            if (contextLoader != null) {
                return contextLoader.loadClass(className);
            }
        } catch (Throwable ignored) {
        }

        try {
            return getClass().getClassLoader().loadClass(className);
        } catch (Throwable ignored) {
        }

        try {
            return Class.forName(className);
        } catch (Throwable ignored) {
        }

        return null;
    }

    private boolean hasAnnotationByName(Class<?> clazz, String annotationClassName) {
        for (java.lang.annotation.Annotation annotation : clazz.getAnnotations()) {
            if (annotation.annotationType().getName().equals(annotationClassName)) {
                return true;
            }
        }
        return false;
    }
    
    private void injectRolePermissionLoader(mg.miniframework.modules.security.RolePermissionLoader loader) {
        AuthenticationProvider provider = securityManager.getAuthenticationProvider();
        if (provider != null) {
            try {
                java.lang.reflect.Method setter = provider.getClass()
                    .getMethod("setRolePermissionLoader", mg.miniframework.modules.security.RolePermissionLoader.class);
                setter.invoke(provider, loader);
                logManager.insertLog("RolePermissionLoader injected into AuthenticationProvider", LogStatus.DEBUG);
            } catch (NoSuchMethodException e) {
                try {
                    logManager.insertLog("AuthenticationProvider does not have setRolePermissionLoader method", LogStatus.DEBUG);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } catch (Exception e) {
                try {
                    logManager.insertLog("Failed to inject RolePermissionLoader: " + e.getMessage(), LogStatus.WARN);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
