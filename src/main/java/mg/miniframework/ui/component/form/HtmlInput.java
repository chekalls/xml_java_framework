package mg.miniframework.ui.component.form;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import mg.miniframework.ui.base.HtmlComponent;
import mg.miniframework.ui.base.XmlLoader;
import mg.miniframework.ui.type.HtmlInputType;

@XmlRootElement(name = "input")
@XmlAccessorType(XmlAccessType.FIELD)
public class HtmlInput extends HtmlComponent {

    @XmlElement(name = "label")
    private String label;

    @XmlElement(name = "name")
    private String name;

    @XmlElement(name = "type")
    private HtmlInputType type;

    @XmlElement(name = "value")
    private String value;

    @XmlElement(name = "cssClass")
    private String cssClass;

    @XmlElement(name = "placeholder")
    private String placeholder;

    public HtmlInput(String resource) {
        HtmlInput field = (HtmlInput) XmlLoader.loadFrom(resource, HtmlInput.class);
        this.label = field.getLabel();
        this.name = field.getName();
        this.type = field.getType();
        this.value = field.getValue();
        this.cssClass = field.getCssClass();
        this.placeholder = field.getPlaceholder();
    }

    public HtmlInput() {
        this.setTemplateFile(HtmlComponent.getBasePath() + "/html-input.xml");
    }

    public HtmlInput(String label, String name, HtmlInputType type) {
        this.label = label;
        this.name = name;
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HtmlInputType getType() {
        return type;
    }

    public void setType(HtmlInputType type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getCssClass() {
        return cssClass;
    }

    public void setCssClass(String cssClass) {
        this.cssClass = cssClass;
    }

    public String getValueAsString() {
        return (value != null) ? value : "";
    }

    @Override
    public String toString() {
        return "" + escapeHtml(this.getLabel()) + " " + this.getName() + " " + this.getValue() + " " + this.getPlaceholder();
    }

    @Override
    public void loadContent() {
        try {
            String fieldType = this.type != null ? this.type.getValue() : "text";
            String template = XmlLoader.getTemplateByType(fieldType, this.getTemplateFile());

            if (template == null || template.isEmpty()) {
                System.out.println("Template spécifique pour type '" + fieldType + "' non trouvé, utilisation du template générique.");
                template = XmlLoader.getTemplateByType("text", this.getTemplateFile());
            }

            if (template == null || template.isEmpty()) {
                System.out.println("Template générique pour type 'text' non trouvé, chargement direct du fichier.");
                template = XmlLoader.getXMLFileContentAsString(this.getTemplateFile());
            }

            if (template == null || template.isEmpty()) {
                System.err.println("template vide pour " + getTemplateFile());
                return;
            }

            this.htmlContent = template
                    .replace("${label}", nullSafeEscape(this.label))
                    .replace("${name}", nullSafeEscape(this.name))
                    .replace("${type}", fieldType)
                    .replace("${icon}", resolveIconClass(fieldType))
                    .replace("${value}", nullSafeEscape(getValueAsString()))
                    .replace("${customClass}", nullSafeEscape(this.cssClass))
                    .replace("${placeholder}", nullSafeEscape(this.placeholder))
                    .replace("${checked}", "")
                    .replace("${options}", "")
                    .replace("${fields}", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String resolveIconClass(String fieldType) {
        if (fieldType == null || fieldType.isEmpty()) {
            return "bi bi-input-cursor-text";
        }

        return switch (fieldType.toLowerCase()) {
            case "email" -> "bi bi-envelope";
            case "password" -> "bi bi-lock";
            case "number" -> "bi bi-123";
            case "date", "datetime-local" -> "bi bi-calendar-event";
            case "checkbox" -> "bi bi-check2-square";
            case "tel" -> "bi bi-telephone";
            case "url" -> "bi bi-link-45deg";
            default -> "bi bi-person";
        };
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }
}