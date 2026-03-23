package mg.miniframework.core.bootstrap;

import java.io.IOException;
import jakarta.servlet.ServletContext;
import mg.miniframework.logging.LogManager;
import mg.miniframework.logging.LogManager.LogStatus;
import mg.miniframework.core.config.ConfigLoader;
import mg.miniframework.web.response.ContentRenderManager;
import mg.miniframework.metrics.MetricsManager;
import mg.miniframework.security.SecurityManager;
import mg.miniframework.service.registry.CachedService;
import mg.miniframework.core.invocation.ControllerMethodInvoker;

public class FrameworkBootstrap {

    public static class BootstrapContext {
        public final ControllerMethodInvoker methodInvoker;
        public final LogManager logManager;
        public final ConfigLoader configLoader;
        public final ContentRenderManager contentRenderManager;
        public final MetricsManager metricsManager;
        public final SecurityManager securityManager;
        public final CachedService cachedService;

        public BootstrapContext(ControllerMethodInvoker methodInvoker, LogManager logManager,
                ConfigLoader configLoader, ContentRenderManager contentRenderManager, MetricsManager metricsManager,
                SecurityManager securityManager, CachedService cachedService) {
            this.methodInvoker = methodInvoker;
            this.logManager = logManager;
            this.configLoader = configLoader;
            this.contentRenderManager = contentRenderManager;
            this.metricsManager = metricsManager;
            this.securityManager = securityManager;
            this.cachedService = cachedService;
        }
    }

    public BootstrapContext bootstrap(ServletContext ctx) {
        ControllerMethodInvoker methodInvoker = new ControllerMethodInvoker();
        LogManager logManager = new LogManager();
        ConfigLoader configLoader = new ConfigLoader();
        ContentRenderManager contentRenderManager = new ContentRenderManager();
        MetricsManager metricsManager = new MetricsManager();
        SecurityManager securityManager = new SecurityManager();
        CachedService cachedService = new CachedService();

        String userAttName = ctx.getInitParameter("security.user.attributeName");
        String rolesAttNAme = ctx.getInitParameter("security.role.attributeName");

        try {
            logManager.insertLog("user att name :" + userAttName, LogStatus.INFO);
            logManager.insertLog("roles att name :" + rolesAttNAme, LogStatus.INFO);
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

        String accessDeniedUrl = ctx.getInitParameter("security.accessDeniedUrl");
        if (accessDeniedUrl != null && !accessDeniedUrl.isEmpty()) {
            securityManager.setAccessDeniedUrl(accessDeniedUrl);
        }

        String authProviderClass = ctx.getInitParameter("security.authenticationProvider");
        if (authProviderClass != null && !authProviderClass.isEmpty()) {
            try {
                Class<?> providerClass = Class.forName(authProviderClass);
                mg.miniframework.security.AuthenticationProvider provider = (mg.miniframework.security.AuthenticationProvider) providerClass
                        .getDeclaredConstructor().newInstance();
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

        return new BootstrapContext(methodInvoker, logManager, configLoader, contentRenderManager, metricsManager,
                securityManager, cachedService);
    }
}
