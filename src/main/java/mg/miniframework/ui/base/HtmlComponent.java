package mg.miniframework.ui.base;

import org.apache.commons.text.StringEscapeUtils;

import jakarta.servlet.ServletContext;

public abstract class HtmlComponent {
    private static String basePath;
    private static ServletContext servletContext;
    private String templateFile;
    private String contentFileXml;
    protected String htmlContent;

    public abstract void loadContent();

    public String escapeHtml(String str) {
        return StringEscapeUtils.escapeHtml4(str);
    }

    public String getTemplateFile() {
        return templateFile;
    }

    public void setTemplateFile(String templateFile) {
        this.templateFile = templateFile;
    }

    public String getContentFileXml() {
        return contentFileXml;
    }

    public void setContentFileXml(String contentFileXml) {
        this.contentFileXml = contentFileXml;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }

    public static String getBasePath() {
        return basePath;
    }

    protected String nullSafeEscape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return escapeHtml(value);
    }

    public static void setBasePath(String basePath, ServletContext context) {
        String normalized = basePath.startsWith("/") ? basePath : "/" + basePath;
        String realRoot = context.getRealPath("/");
        HtmlComponent.basePath = (realRoot != null)
                ? realRoot + normalized
                : normalized;
        HtmlComponent.servletContext = context;
    }

    public static void setBasePath(String basePath) {
        HtmlComponent.basePath = basePath;
    }

    public static ServletContext getServletContext() {
        return servletContext;
    }

    public static void setServletContext(ServletContext servletContext) {
        HtmlComponent.servletContext = servletContext;
    }
}
