package mg.miniframework.ui.component.form;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;
import mg.miniframework.ui.base.HtmlComponent;
import mg.miniframework.ui.base.XmlLoader;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "select")
@XmlAccessorType(XmlAccessType.FIELD)
public class HtmlSelect extends HtmlComponent {

    @XmlElement(name = "label")
    private String label;

    @XmlElement(name = "name")
    private String name;

    @XmlElement(name = "cssClass")
    private String cssClass;

    @XmlElement(name = "value")
    private String value;

    @XmlElementWrapper(name = "options")
    @XmlElement(name = "option")
    private List<Option> options = new ArrayList<>();

    public HtmlSelect() {
        this.setTemplateFile(HtmlComponent.getBasePath() + "/html-select.xml");
    }

    public HtmlSelect(String resource) {
        HtmlSelect sel = (HtmlSelect) XmlLoader.loadFrom(resource, HtmlSelect.class);
        this.label = sel.label;
        this.name = sel.name;
        this.cssClass = sel.cssClass;
        this.value = sel.value;
        this.options = sel.options;
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCssClass() { return cssClass; }
    public void setCssClass(String cssClass) { this.cssClass = cssClass; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public List<Option> getOptions() { return options; }
    public void setOptions(List<Option> options) { this.options = options; }

    @Override
    public void loadContent() {
        try {
            String template = XmlLoader.getXMLFileContentAsString(this.getTemplateFile());
            if (template == null || template.isEmpty()) {
                System.err.println("template vide pour " + getTemplateFile());
                return;
            }

            String optionsHtml = buildOptionsHtml();

            this.htmlContent = template
                    .replace("${label}", nullSafeEscape(this.label))
                    .replace("${name}", nullSafeEscape(this.name))
                    .replace("${customClass}", nullSafeEscape(this.cssClass))
                    .replace("${options}", optionsHtml);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String buildOptionsHtml() {
        if (options == null || options.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Option opt : options) {
            sb.append("<option");
            if (opt.value != null) {
                sb.append(" value=\"").append(escapeHtml(opt.value)).append("\"");
            }
            if (Boolean.TRUE.equals(opt.disabled)) {
                sb.append(" disabled");
            }
            boolean selected = Boolean.TRUE.equals(opt.selected) ||
                    (opt.value != null && opt.value.equals(this.value));
            if (selected) {
                sb.append(" selected");
            }
            sb.append(">");
            sb.append(escapeHtml(opt.label != null ? opt.label : ""));
            sb.append("</option>");
        }
        return sb.toString();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Option {
        @XmlAttribute(name = "value")
        private String value;

        @XmlAttribute(name = "disabled")
        private Boolean disabled;

        @XmlAttribute(name = "selected")
        private Boolean selected;

        @XmlValue
        private String label;

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        public Boolean getDisabled() { return disabled; }
        public void setDisabled(Boolean disabled) { this.disabled = disabled; }

        public Boolean getSelected() { return selected; }
        public void setSelected(Boolean selected) { this.selected = selected; }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }
}

