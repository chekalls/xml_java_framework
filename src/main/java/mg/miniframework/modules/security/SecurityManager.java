package mg.miniframework.modules.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.lang.reflect.Method;

import mg.miniframework.annotation.AllowAnonymous;
import mg.miniframework.annotation.RequirePermission;
import mg.miniframework.annotation.RequireRole;
import mg.miniframework.annotation.Secured;
import mg.miniframework.modules.LogManager;
import mg.miniframework.modules.LogManager.LogStatus;

public class SecurityManager {
    
    private static final String DEFAULT_SESSION_USER_KEY = "mg.miniframework.security.SessionUser";
    
    private AuthenticationProvider authenticationProvider;
    private RolePermissionLoader rolePermissionLoader;
    private LogManager logManager;
    private String loginUrl = "/login";
    private String accessDeniedUrl = "/access-denied";
    private boolean enabled = true;
    // private String 
    private String connectedUserVarName;
    private String userRolesVarName;

    public SecurityManager() {
        this.logManager = new LogManager();
    }

    public User authenticate(String username, String password, HttpServletRequest request) 
            throws IOException {

        ensureAuthenticationProviderInitialized(request);
        
        if (authenticationProvider == null) {
            logManager.insertLog("No authentication provider configured", LogStatus.WARN);
            return null;
        }

        HttpSession session = request.getSession();

        User user = authenticationProvider.authenticate(username, password);

        if (user != null) {
            user.setAuthenticated(true);
            session.setAttribute(getSessionUserKey(), user);
            logManager.insertLog("User authenticated: " + username, LogStatus.INFO);
            return user;
        }
        
        logManager.insertLog("Authentication failed for user: " + username, LogStatus.WARN);
        return null;
    }

    /**
     * Reads the authenticated user object from session.
     * previously code manufactured a User every time which always
     * returned authenticated=true, effectively bypassing security.
     */
    public User getUserFromSession(HttpServletRequest request) {
        ensureAuthenticationProviderInitialized(request);

        HttpSession session = request.getSession(false);

        if (session == null) {
            return null;
        }
        Object obj = session.getAttribute(getSessionUserKey());
        if (obj instanceof User user) {
            return user;
        }

        if (obj instanceof String username && authenticationProvider != null) {
            User loaded = authenticationProvider.loadUserByUsername(username);
            if (loaded != null) {
                loaded.setAuthenticated(true);
                return loaded;
            }
        }

        return null;
    }

    public void logout(HttpServletRequest request) throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            User user = (User) session.getAttribute(getSessionUserKey());
            if (user != null) {
                logManager.insertLog("User logged out: " + user.getUsername(), LogStatus.INFO);
            }
            session.removeAttribute(getSessionUserKey());
            session.invalidate();
        }
    }

    public boolean isAccessAllowed(Method method, HttpServletRequest request, 
                                   HttpServletResponse response) 
            throws IOException {
        if (!enabled) {
            return true;
        }

        if (method.isAnnotationPresent(AllowAnonymous.class)) {
            logManager.insertLog("Access allowed (anonymous): " + method.getName(), LogStatus.DEBUG);
            return true;
        }

        Class<?> controllerClass = method.getDeclaringClass();
        if (controllerClass.isAnnotationPresent(AllowAnonymous.class)) {
            logManager.insertLog("Access allowed (anonymous class): " + method.getName(), LogStatus.DEBUG);
            return true;
        }

        boolean methodSecured = method.isAnnotationPresent(Secured.class);
        boolean classSecured = controllerClass.isAnnotationPresent(Secured.class);
        
        boolean hasRoleAnnotation = method.isAnnotationPresent(RequireRole.class) ||
                                   controllerClass.isAnnotationPresent(RequireRole.class);
        
        boolean hasPermissionAnnotation = method.isAnnotationPresent(RequirePermission.class) ||
                                         controllerClass.isAnnotationPresent(RequirePermission.class);

        if (!methodSecured && !classSecured && !hasRoleAnnotation && !hasPermissionAnnotation) {
            return true;
        }
        // if(!methodSecured && !hasRoleAnnotation ){
        //     return true;
        // }

        User user = getUserFromSession(request);

        // logManager.insertLog("user in session : "+user.getUsername(), LogStatus.INFO);
        SecurityContext securityContext = new SecurityContext(request);
        securityContext.setUser(user);

        if (!securityContext.isAuthenticated()) {
            logManager.insertLog("Access denied (not authenticated): " + method.getName(), 
                               LogStatus.WARN);
            handleUnauthorized(request, response);
            return false;
        }

        if (!checkRoles(method, controllerClass, securityContext)) {
            logManager.insertLog("Access denied (insufficient roles): " + method.getName() + 
                               " for user: " + user.getUsername(), LogStatus.WARN);
            handleAccessDenied(request, response);
            return false;
        }

        if (!checkPermissions(method, controllerClass, securityContext)) {
            logManager.insertLog("Access denied (insufficient permissions): " + method.getName() +
                               " for user: " + user.getUsername(), LogStatus.WARN);
            handleAccessDenied(request, response);
            return false;
        }

        logManager.insertLog("Access granted: " + method.getName() + 
                           " for user: " + user.getUsername(), LogStatus.DEBUG);
        return true;
    }

    private boolean checkRoles(Method method, Class<?> controllerClass, SecurityContext context) {
        RequireRole methodRole = method.getAnnotation(RequireRole.class);
        // RequireRole classRole = controllerClass.getAnnotation(RequireRole.class);

        // RequireRole roleAnnotation = methodRole != null ? methodRole : classRole;
        RequireRole roleAnnotation = methodRole;

        if (roleAnnotation == null) {
            return true; 
        }

        String[] requiredRoles = roleAnnotation.value();
        boolean requireAll = roleAnnotation.requireAll();

        if (requireAll) {
            return context.hasAllRoles(requiredRoles);
        } else {
            return context.hasAnyRole(requiredRoles);
        }
    }

    private boolean checkPermissions(Method method, Class<?> controllerClass, 
                                    SecurityContext context) {
        RequirePermission methodPermission = method.getAnnotation(RequirePermission.class);
        RequirePermission classPermission = controllerClass.getAnnotation(RequirePermission.class);

        RequirePermission permissionAnnotation = methodPermission != null ? 
                                                 methodPermission : classPermission;

        if (permissionAnnotation == null) {
            return true;
        }

        String[] requiredPermissions = permissionAnnotation.value();
        boolean requireAll = permissionAnnotation.requireAll();

        if (requireAll) {
            return context.hasAllPermissions(requiredPermissions);
        } else {
            return context.hasAnyPermission(requiredPermissions);
        }
    }

    private void handleUnauthorized(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        if (loginUrl != null && !loginUrl.isEmpty()) {
            response.sendRedirect(request.getContextPath() + loginUrl);
        } else {
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().println("<html><body>");
            response.getWriter().println("<h1>401 - Non authentifié</h1>");
            response.getWriter().println("<p>Vous devez être authentifié pour accéder à cette ressource.</p>");
            response.getWriter().println("</body></html>");
        }
    }

    private void handleAccessDenied(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        
        if (accessDeniedUrl != null && !accessDeniedUrl.isEmpty()) {
            response.sendRedirect(request.getContextPath() + accessDeniedUrl);
        } else {
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().println("<html><body>");
            response.getWriter().println("<h1>403 - Accès refusé</h1>");
            response.getWriter().println("<p>Vous n'avez pas les droits nécessaires pour accéder à cette ressource.</p>");
            response.getWriter().println("</body></html>");
        }
    }

    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    public void setAuthenticationProvider(AuthenticationProvider authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
    }

    public LogManager getLogManager() {
        return logManager;
    }

    public void setLogManager(LogManager logManager) {
        this.logManager = logManager;
    }

    public String getLoginUrl() {
        return loginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public String getAccessDeniedUrl() {
        return accessDeniedUrl;
    }

    public void setAccessDeniedUrl(String accessDeniedUrl) {
        this.accessDeniedUrl = accessDeniedUrl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public RolePermissionLoader getRolePermissionLoader() {
        return rolePermissionLoader;
    }

    public void setRolePermissionLoader(RolePermissionLoader rolePermissionLoader) {
        this.rolePermissionLoader = rolePermissionLoader;
    }

    // public static String getSessionUserKey() {
    //     return SESSION_USER_KEY;
    // }

    public String getConnectedUserVarName() {
        return connectedUserVarName;
    }

    public void setConnectedUserVarName(String connectedUserVarName) {
        this.connectedUserVarName = connectedUserVarName;
    }

    public String getUserRolesVarName() {
        return userRolesVarName;
    }

    public void setUserRolesVarName(String userRolesVarName) {
        this.userRolesVarName = userRolesVarName;
    }

    private String getSessionUserKey() {
        if (connectedUserVarName == null || connectedUserVarName.isBlank()) {
            return DEFAULT_SESSION_USER_KEY;
        }
        return connectedUserVarName;
    }

    private void ensureAuthenticationProviderInitialized(HttpServletRequest request) {
        if (authenticationProvider != null || request == null || request.getServletContext() == null) {
            return;
        }

        String authProviderClass = request.getServletContext().getInitParameter("security.authenticationProvider");
        if (authProviderClass == null || authProviderClass.isBlank()) {
            return;
        }

        try {
            Class<?> providerClass = Class.forName(authProviderClass);
            if (!AuthenticationProvider.class.isAssignableFrom(providerClass)) {
                logManager.insertLog("Configured security.authenticationProvider does not implement AuthenticationProvider: " + authProviderClass,
                        LogStatus.ERROR);
                return;
            }

            AuthenticationProvider provider = (AuthenticationProvider) providerClass.getDeclaredConstructor().newInstance();
            this.authenticationProvider = provider;

            if (rolePermissionLoader != null) {
                try {
                    Method setter = provider.getClass().getMethod("setRolePermissionLoader", RolePermissionLoader.class);
                    setter.invoke(provider, rolePermissionLoader);
                } catch (NoSuchMethodException ignored) {
                }
            }

            logManager.insertLog("Authentication provider auto-loaded from init-param: " + authProviderClass, LogStatus.INFO);
        } catch (Exception e) {
            try {
                logManager.insertLog("Failed to auto-load authentication provider: " + e.getMessage(), LogStatus.ERROR);
            } catch (IOException ignored) {
            }
        }
    }
}
