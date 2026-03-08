package mg.miniframework.core.dispatch;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mg.miniframework.core.routing.RouteMap;
import mg.miniframework.logging.LogManager;
import mg.miniframework.logging.LogManager.LogStatus;
import mg.miniframework.metrics.MetricsManager;

public class NotFoundResponseRenderer {

    public void renderNotFound(HttpServletRequest req, HttpServletResponse resp,
            String urlPath, String httpMethod, RouteMap routeMap,
            MetricsManager metricsManager, LogManager logManager) throws IOException {

        metricsManager.incrementErrorCount();

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        resp.setContentType("text/html;charset=UTF-8");

        java.io.PrintWriter out = resp.getWriter();
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

        java.util.List<java.util.Map.Entry<mg.miniframework.core.routing.Url, mg.miniframework.core.routing.CachedMethodInfo>> sortedEntries = new java.util.ArrayList<>(
            routeMap.getUrlMethodsMap().entrySet()
        );

        sortedEntries.sort(java.util.Comparator.comparing(
            e -> e.getKey().getUrlPath(),
            String.CASE_INSENSITIVE_ORDER
        ));

        for (java.util.Map.Entry<mg.miniframework.core.routing.Url, mg.miniframework.core.routing.CachedMethodInfo> entry : sortedEntries) {
            out.println("<li>" + entry.getKey().getMethod() + " " + entry.getKey().getUrlPath() + "</li>");
        }

        out.println("</ul>");
        out.println("</body></html>");
        out.flush();
    }
}
