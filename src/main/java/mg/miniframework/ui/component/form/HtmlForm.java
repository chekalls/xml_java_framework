package mg.miniframework.ui.component.form;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlAnyElement;
import mg.miniframework.ui.base.HtmlComponent;
import mg.miniframework.ui.base.XmlLoader;

@XmlRootElement(name = "form")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso({HtmlInput.class, HtmlSelect.class})
public class HtmlForm extends HtmlComponent{

    public HtmlForm() {
        this.setTemplateFile(HtmlComponent.getBasePath() + "/html-form.xml");
    }

    public HtmlForm(String resource) {
        HtmlForm f = (HtmlForm) XmlLoader.loadFrom(resource, HtmlForm.class);
        this.id = f.id;
        this.cssClass = f.cssClass;
        this.submitText = f.submitText;
        this.metadata = f.metadata;
        this.components = f.components;
        this.setTemplateFile(f.getTemplateFile());
    }

    @XmlElement(name = "id")
    private String id;

    @XmlElement(name = "cssClass")
    private String cssClass;

    @XmlElement(name = "submitText")
    private String submitText;

    @XmlElement(name="metadata")
    private Metadata metadata;

    @XmlElement(name = "component")
    private java.util.List<ComponentWrapper> components = new java.util.ArrayList<>();

    @Override
    public void loadContent() {
        try {
            String template = XmlLoader.getXMLFileContentAsString(this.getTemplateFile());
            if (template == null || template.isEmpty()) {
                System.err.println("template vide pour " + getTemplateFile());
                return;
            }

            StringBuilder childrenHtml = new StringBuilder();
            if (components != null) {
                for (ComponentWrapper wrapper : components) {
                    if (wrapper != null && wrapper.components != null && !wrapper.components.isEmpty()) {
                        // wrapper contient multiple HtmlComponent
                        for (HtmlComponent comp : wrapper.components) {
                            if (comp != null) {
                                comp.loadContent();
                                childrenHtml.append(comp.getHtmlContent());
                            }
                        }
                    } else if (wrapper != null && wrapper.component != null) {
                        // fallback: single component (backwards compatibility)
                        wrapper.component.loadContent();
                        childrenHtml.append(wrapper.component.getHtmlContent());
                    }
                }
            }

            String action = metadata != null ? metadata.getAction() : "";
            if (action != null && action.startsWith("/") && HtmlComponent.getServletContext() != null) {
                String cp = HtmlComponent.getServletContext().getContextPath();
                if (cp != null && !cp.isEmpty() && !action.startsWith(cp)) {
                    action = cp + action;
                }
            }
            String method = metadata != null ? metadata.getMethod() : "";
            String enctype = metadata != null ? metadata.getEnctype() : "";
            String idValue = this.getId();
            String cssValue = this.getCssClass();

            this.htmlContent = template
                    .replace("${id}", nullSafeEscape(idValue))
                    .replace("${cssClass}", nullSafeEscape(cssValue))
                    .replace("${action}", nullSafeEscape(action))
                    .replace("${method}", nullSafeEscape(method))
                    .replace("${enctype}", nullSafeEscape(enctype))
                    .replace("${submitText}", nullSafeEscape(this.submitText))
                    .replace("${children}", childrenHtml.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCssClass() { return cssClass; }
    public void setCssClass(String cssClass) { this.cssClass = cssClass; }

    public String getSubmitText() { return submitText; }
    public void setSubmitText(String submitText) { this.submitText = submitText; }

    public Metadata getMetadata() { return metadata; }
    public void setMetadata(Metadata metadata) { this.metadata = metadata; }

    public java.util.List<ComponentWrapper> getComponents() { return components; }
    public void setComponents(java.util.List<ComponentWrapper> components) { this.components = components; }

    public static class Metadata {
        private String action;
        private String method;
        private String enctype;

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }

        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }

        public String getEnctype() { return enctype; }
        public void setEnctype(String enctype) { this.enctype = enctype; }

    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlSeeAlso({HtmlInput.class, HtmlSelect.class})
    public static class ComponentWrapper {
        @XmlAnyElement(lax = true)
        private java.util.List<HtmlComponent> components = new java.util.ArrayList<>();
        
        // keep single component for backwards compatibility
        private HtmlComponent component;

        public java.util.List<HtmlComponent> getComponents() {
            return components;
        }

        public void setComponents(java.util.List<HtmlComponent> components) {
            this.components = components;
        }

        public HtmlComponent getComponent() {
            return component;
        }

        public void setComponent(HtmlComponent component) {
            this.component = component;
        }
    }
}
