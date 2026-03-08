package mg.miniframework.modules;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import org.apache.commons.text.StringEscapeUtils;
import org.w3c.dom.Element;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mg.miniframework.ui.base.HtmlComponent;
import mg.miniframework.ui.base.XmlLoader;
import mg.miniframework.utils.JsonUtils;

public class ContentRenderManager {

    private LogManager logManager;
    private String baseJspPath = "";

    public ContentRenderManager() {
        this.logManager = new LogManager();
    }

    /*
     * =======================
     * JSON
     * =======================
     */
    public String convertToJson(Object object) {
        if (object instanceof ModelView mv) {
            return JsonUtils.mapToJson(mv.getDataMap());
        }
        return JsonUtils.objectToJson(object);
    }

    public int renderContent(
            Object result,
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        if (result == null) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return RouteStatus.NOT_FOUND.getCode();
        }


        if (result instanceof String str) {
            return handleStringResult(str, request, response);
        }

        if (result instanceof ModelView mv) {
            mv.getDataMap().forEach(request::setAttribute);

            if(mv.getRedirect()!=null && !mv.getRedirect().isBlank()){
                response.sendRedirect(request.getContextPath() + mv.getRedirect());
                return RouteStatus.REDIRECT.getCode();

            }
            // if controller explicitly marked this model-view as a plain view
            if (mv.isViewOnly()) {
                String forwardPath = resolveJspForwardPath(mv.getView());
                forwardToJsp(request, response, forwardPath);
                return RouteStatus.RETURN_MODEL_VIEW.getCode();
            }

            // priorité au traitement XML (qui peut avoir un template)
            if (mv.isXml()) {
                String xmlHtml = renderXmlComponentToString(mv.getView(), request);
                if (xmlHtml == null || xmlHtml.isEmpty()) {
                    throw new IOException("Le contenu HTML du composant est vide après loadContent()");
                }
                // if template exists, render template with xmlHtml as content
                if (mv.getTemplate() != null && !mv.getTemplate().isEmpty()) {
                    request.setAttribute("contentHtml", xmlHtml);
                    String forwardPath = resolveJspForwardPath(mv.getTemplate());
                    forwardToJsp(request, response, forwardPath);
                    return RouteStatus.RETURN_MODEL_VIEW.getCode();
                }
                // otherwise write directly
                response.setContentType("text/html;charset=UTF-8");
                PrintWriter out = response.getWriter();
                out.print(xmlHtml);
                out.flush();
                return RouteStatus.RETURN_MODEL_VIEW.getCode();
            }

            // template pour les JSP standard (non-XML)
            if (mv.getTemplate() != null && !mv.getTemplate().isEmpty()) {
                String forwardPath = resolveJspForwardPath(mv.getTemplate());
                request.setAttribute("contentPage", resolveJspForwardPath(mv.getView()));
                forwardToJsp(request, response, forwardPath);
                return RouteStatus.RETURN_MODEL_VIEW.getCode();
            }

            // fallback: simple JSP view
            String forwardPath = resolveJspForwardPath(mv.getView());
            forwardToJsp(request, response, forwardPath);
            return RouteStatus.RETURN_MODEL_VIEW.getCode();
        }

        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().println(
                "Return type inconnu : " + result.getClass().getName());

        return RouteStatus.RETURN_TYPE_UNKNOWN.getCode();
    }

    // --- helper utilities ------------------------------------------------
    private int handleStringResult(String str, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (looksLikeJspPath(str)) {
            String forwardPath = resolveJspForwardPath(str);
            forwardToJsp(request, response, forwardPath);
            return RouteStatus.RETURN_MODEL_VIEW.getCode();
        }
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().print(str);
        return RouteStatus.RETURN_STRING.getCode();
    }

    /**
     * Charge un composant XML et écrit le HTML dans la réponse.
     * @return true si un contenu non vide a été rendu, false sinon
     */
    private boolean renderXmlComponent(String xmlView, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        try {
            HtmlComponent component = (HtmlComponent) XmlLoader.loadFrom(
                    resolveXmlForwardPath(xmlView), request.getServletContext());
            component.loadContent();
            String html = component.getHtmlContent();
            if (html != null && !html.isEmpty()) {
                response.setContentType("text/html;charset=UTF-8");
                PrintWriter out = response.getWriter();
                out.print(html);
                out.flush();
                return true;
            }
            return false;
        } catch (Exception e) {
            throw new IOException("Erreur lors du chargement du XML : " + e.getMessage(), e);
        }
    }

    /**
     * Rend le composant XML sous forme de chaîne, sans écrire dans la réponse.
     */
    private String renderXmlComponentToString(String xmlView, HttpServletRequest request) throws IOException {
        try {
            HtmlComponent component = (HtmlComponent) XmlLoader.loadFrom(
                    resolveXmlForwardPath(xmlView), request.getServletContext());
            component.loadContent();
            return component.getHtmlContent();
        } catch (Exception e) {
            throw new IOException("Erreur lors du chargement du XML : " + e.getMessage(), e);
        }
    }

    /*
     * =======================
     * UTILS
     * =======================
     */
    private String normalizeJspPath(String jsp) {
        if (jsp == null || jsp.isBlank()) {
            throw new IllegalArgumentException("Le nom de la vue JSP est vide");
        }
        String trimmed = jsp.trim();
        String normalized = trimmed.startsWith("/") ? trimmed : "/" + trimmed;
        return normalized.replaceAll("/{2,}", "/");
    }

    private boolean looksLikeJspPath(String value) {
        if (value == null) {
            return false;
        }

        String trimmed = value.trim().toLowerCase();
        return trimmed.endsWith(".jsp");
    }

    private String resolveXmlForwardPath(String xmlView) {
        String xml = normalizeJspPath(xmlView);

        if (xml.startsWith("/WEB-INF/")) {
            return xml;
        }

        return baseJspPath + xml;
    }

    private String resolveJspForwardPath(String jspView) {
        String jsp = normalizeJspPath(jspView);

        if (jsp.startsWith("/WEB-INF/")) {
            return jsp;
        }

        if (baseJspPath == null || baseJspPath.isBlank()) {
            if (jsp.startsWith("/pages/")) {
                return "/WEB-INF/views" + jsp;
            }
            return jsp;
        }

        return baseJspPath + jsp;
    }

    private void forwardToJsp(
            HttpServletRequest request,
            HttpServletResponse response,
            String forwardPath) throws ServletException, IOException {
        RequestDispatcher jspDispatcher = request.getRequestDispatcher(forwardPath);
        if (jspDispatcher == null) {
            throw new ServletException("Impossible de forward vers la JSP : " + forwardPath);
        }
        jspDispatcher.forward(request, response);
    }

    public LogManager getLogManager() {
        return logManager;
    }

    public void setLogManager(LogManager logManager) {
        this.logManager = logManager;
    }

    public String getBaseJspPath() {
        return baseJspPath;
    }

    public void setBaseJspPath(String baseJspPath) {
        if (baseJspPath == null) {
            this.baseJspPath = "";
        } else {
            this.baseJspPath = baseJspPath.endsWith("/")
                    ? baseJspPath.substring(0, baseJspPath.length() - 1)
                    : baseJspPath;
        }
    }
}
