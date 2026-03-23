package mg.miniframework.web.servlet;

import java.io.File;
import java.io.IOException;
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

import mg.miniframework.core.config.ConfigLoader;
import mg.miniframework.core.invocation.ControllerMethodInvoker;
import mg.miniframework.core.routing.CachedMethodInfo;
import mg.miniframework.core.routing.RouteMap;
import mg.miniframework.core.routing.RoutePatternUtils;
import mg.miniframework.core.routing.RouteRegistryBuilder;
import mg.miniframework.core.routing.Url;
import mg.miniframework.logging.LogManager;
import mg.miniframework.logging.LogManager.LogStatus;
import mg.miniframework.metrics.MetricsManager;
import mg.miniframework.modules.*;
import mg.miniframework.security.AuthenticationProvider;
import mg.miniframework.security.SecurityManager;
import mg.miniframework.service.registry.CachedService;
import mg.miniframework.ui.base.HtmlComponent;
import mg.miniframework.web.response.ContentRenderManager;
import mg.miniframework.core.dispatch.RequestErrorHandler;
import mg.miniframework.core.dispatch.NotFoundResponseRenderer;

@WebServlet(name = "FrontControllerServlet", urlPatterns = "/")
@MultipartConfig
public class FrontControllerServlet extends HttpServlet {
    private String baseFile;
    private ControllerMethodInvoker methodeManager;
    private LogManager logManager;
    private ConfigLoader configLoader;
    private ContentRenderManager contentRenderManager;
    private MetricsManager metricsManager;
    private SecurityManager securityManager;
    private CachedService cachedService;

    @Override
    public void init() throws ServletException {
        ServletContext ctx = getServletContext();
        mg.miniframework.core.bootstrap.FrameworkBootstrap bootstrap = new mg.miniframework.core.bootstrap.FrameworkBootstrap();
        mg.miniframework.core.bootstrap.FrameworkBootstrap.BootstrapContext bc = bootstrap.bootstrap(ctx);

        this.methodeManager = bc.methodInvoker;
        this.logManager = bc.logManager;
        this.configLoader = bc.configLoader;
        this.contentRenderManager = bc.contentRenderManager;
        this.metricsManager = bc.metricsManager;
        this.securityManager = bc.securityManager;
        this.cachedService = bc.cachedService;
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
                mg.miniframework.security.RolePermissionLoader loader = 
                    new mg.miniframework.security.RolePermissionLoader();
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
                mg.miniframework.security.RolePermissionLoader loader = 
                    (mg.miniframework.security.RolePermissionLoader) servletContext.getAttribute("rolePermissionLoader");
                securityManager.setRolePermissionLoader(loader);
                
                injectRolePermissionLoader(loader);
            }

            if (servletContext.getAttribute("routeMap") == null) {
                RouteRegistryBuilder builder = new RouteRegistryBuilder();
                builder.build(servletContext, logManager, cachedService);
                // builder.loadControllerSidebar(req);
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

                Integer status = new mg.miniframework.core.dispatch.RequestDispatcherEngine().dispatch(
                    url,
                    routePatterns,
                    routeMap.getUrlMethodsMap(),
                    req,
                    resp,
                    methodeManager,
                    cachedService,
                    contentRenderManager,
                    logManager,
                    securityManager);

            if (status.equals(RouteStatus.NOT_FOUND.getCode())) {
                new NotFoundResponseRenderer().renderNotFound(req, resp, relativePath, req.getMethod(), routeMap, metricsManager, logManager);
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
            new RequestErrorHandler().handleInternalError(e, req, resp, logManager);
        }
    }

    

    

    
    
    private void injectRolePermissionLoader(mg.miniframework.security.RolePermissionLoader loader) {
        AuthenticationProvider provider = securityManager.getAuthenticationProvider();
        if (provider != null) {
            try {
                java.lang.reflect.Method setter = provider.getClass()
                    .getMethod("setRolePermissionLoader", mg.miniframework.security.RolePermissionLoader.class);
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
