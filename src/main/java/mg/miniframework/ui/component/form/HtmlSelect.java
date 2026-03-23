package mg.miniframework.ui.component.form;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;
import mg.miniframework.ui.base.HtmlComponent;
import mg.miniframework.ui.base.XmlLoader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.net.URI;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@XmlRootElement(name = "select")
@XmlAccessorType(XmlAccessType.FIELD)
public class HtmlSelect extends HtmlComponent {

    @XmlElement(name = "api-source")
    private String apiSource;

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
        this.apiSource = sel.apiSource;
        this.label = sel.label;
        this.name = sel.name;
        this.cssClass = sel.cssClass;
        this.value = sel.value;
        this.options = sel.options;
    }

    public String getApiSource() { return apiSource; }
    public void setApiSource(String apiSource) { this.apiSource = apiSource; }

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
        List<Option> effectiveOptions = resolveOptions();
        if (effectiveOptions == null || effectiveOptions.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (Option opt : effectiveOptions) {
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

    private List<Option> resolveOptions() {
        if (apiSource != null && !apiSource.isBlank()) {
            List<Option> apiOptions = fetchOptionsFromApi(apiSource.trim());
            if (apiOptions != null && !apiOptions.isEmpty()) {
                return apiOptions;
            }
        }
        return options;
    }

    private List<Option> fetchOptionsFromApi(String endpoint) {
        List<Option> fetched = new ArrayList<>();
        HttpURLConnection connection = null;
        try {
            URL url = URI.create(endpoint).toURL();
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("Accept", "application/json");

            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                System.err.println("HtmlSelect API error status " + status + " for " + endpoint);
                return fetched;
            }

            try (InputStream inputStream = connection.getInputStream()) {
                String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                fetched.addAll(parseOptionsFromJson(json));
            }
        } catch (Exception e) {
            System.err.println("HtmlSelect API fetch failed for " + endpoint + ": " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return fetched;
    }

    private List<Option> parseOptionsFromJson(String json) throws Exception {
        List<Option> parsed = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);

        JsonNode nodeToRead = root;
        if (root != null && root.isObject()) {
            if (root.has("options") && root.get("options").isArray()) {
                nodeToRead = root.get("options");
            } else if (root.has("data") && root.get("data").isArray()) {
                nodeToRead = root.get("data");
            }
        }

        if (nodeToRead != null && nodeToRead.isArray()) {
            for (JsonNode item : nodeToRead) {
                Option option = toOption(item);
                if (option != null) {
                    parsed.add(option);
                }
            }
            return parsed;
        }

        if (nodeToRead != null && nodeToRead.isObject()) {
            nodeToRead.fields().forEachRemaining(entry -> {
                JsonNode valueNode = entry.getValue();
                if (valueNode != null && valueNode.isValueNode()) {
                    Option option = new Option();
                    option.value = entry.getKey();
                    option.label = valueNode.asText(entry.getKey());
                    parsed.add(option);
                }
            });
        }

        return parsed;
    }

    private Option toOption(JsonNode item) {
        if (item == null || item.isNull()) {
            return null;
        }

        Option option = new Option();
        if (item.isObject()) {
            option.value = textFrom(item, "value", "id", "code", "key");
            option.label = textFrom(item, "label", "name", "text", "title");

            if (option.label == null || option.label.isBlank()) {
                option.label = option.value;
            }
            if (item.has("disabled") && !item.get("disabled").isNull()) {
                option.disabled = item.get("disabled").asBoolean();
            }
            if (item.has("selected") && !item.get("selected").isNull()) {
                option.selected = item.get("selected").asBoolean();
            }
            return option;
        }

        if (item.isValueNode()) {
            String text = item.asText();
            option.value = text;
            option.label = text;
            return option;
        }

        return null;
    }

    private String textFrom(JsonNode node, String... keys) {
        for (String key : keys) {
            if (node.has(key) && !node.get(key).isNull()) {
                return node.get(key).asText();
            }
        }
        return null;
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

