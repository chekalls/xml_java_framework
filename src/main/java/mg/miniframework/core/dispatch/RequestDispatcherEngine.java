package mg.miniframework.core.dispatch;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mg.miniframework.core.invocation.ControllerMethodInvoker;
import mg.miniframework.core.routing.CachedMethodInfo;
import mg.miniframework.core.routing.Url;
import mg.miniframework.logging.LogManager;
import mg.miniframework.logging.LogManager.LogStatus;
import mg.miniframework.modules.RouteStatus;
import mg.miniframework.security.SecurityManager;
import mg.miniframework.service.registry.CachedService;
import mg.miniframework.web.response.ContentRenderManager;

public class RequestDispatcherEngine {

    public int dispatch(
            Url requestURL,
            Map<Url, Pattern> routes,
            Map<Url, CachedMethodInfo> methodsMap,
            HttpServletRequest req,
            HttpServletResponse resp,
            ControllerMethodInvoker methodInvoker,
            CachedService cachedService,
            ContentRenderManager contentRenderManager,
            LogManager logManager,
            SecurityManager securityManager) throws IOException {

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
                    java.lang.reflect.Method method = cachedInfo.getMethod();

                    if (!securityManager.isAccessAllowed(method, req, resp)) {
                        return RouteStatus.RETURN_TYPE_UNKNOWN.getCode();
                    }

                    Map<String, String> pathParams = mg.miniframework.core.routing.RoutePatternUtils.extractPathParams(
                            routeURL.getUrlPath(),
                            requestURL.getUrlPath());

                    try {
                        logManager.insertLog("Invoking method: " + method.getDeclaringClass().getName() + "#" + method.getName(), LogStatus.DEBUG);
                    } catch (IOException ioex) {
                        ioex.printStackTrace();
                    }

                    Object result = methodInvoker.invokeCorrespondingMethod(
                            cachedInfo,
                            method.getDeclaringClass(),
                            pathParams, cachedService,
                            req,
                            resp);

                    try {
                        logManager.insertLog("Method invoked successfully: " + method.getDeclaringClass().getName() + "#" + method.getName(), LogStatus.DEBUG);
                    } catch (IOException ioex) {
                        ioex.printStackTrace();
                    }

                    if (method.isAnnotationPresent(mg.miniframework.annotation.JsonUrl.class)) {
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
}
