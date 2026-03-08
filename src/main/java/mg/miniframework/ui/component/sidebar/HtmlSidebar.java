package mg.miniframework.ui.component.sidebar;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import mg.miniframework.ui.base.HtmlComponent;
import mg.miniframework.ui.base.XmlLoader;

@XmlRootElement(name = "sidebar")
@XmlAccessorType(XmlAccessType.FIELD)
public class HtmlSidebar extends HtmlComponent {

    @XmlElement(name = "cssClass")
    private String cssClass;

    @XmlElementWrapper(name = "components")
    @XmlElement(name = "sidebar-item")
    private List<HtmlSidebarItem> items = new ArrayList<>();

    public HtmlSidebar(){
        this.setTemplateFile(HtmlComponent.getBasePath()+"/html-sidebar.xml");
    }

    @Override
    public void loadContent() {
        try {
            String template = XmlLoader.getXMLFileContentAsString(this.getTemplateFile());
            StringBuilder childrenHtml = new StringBuilder();

            for (HtmlSidebarItem item : items) {
                if (item != null) {
                    item.loadContent();
                    childrenHtml.append(item.getHtmlContent());
                }
            }

            this.htmlContent = template
                    .replace("${cssClass}", nullSafeEscape(this.cssClass))
                    .replace("${item}", childrenHtml.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getCssClass() {
        return cssClass;
    }

    public void setCssClass(String cssClass) {
        this.cssClass = cssClass;
    }

    public List<HtmlSidebarItem> getItems() {
        return items;
    }

    public void setItems(List<HtmlSidebarItem> items) {
        this.items = items;
    }
}
