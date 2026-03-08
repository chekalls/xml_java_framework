package mg.miniframework.ui.component.sidebar;

import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletContext;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;
import mg.miniframework.ui.base.HtmlComponent;
import mg.miniframework.ui.base.XmlLoader;

@XmlRootElement(name = "sidebar-item")
@XmlAccessorType(XmlAccessType.FIELD)
public class HtmlSidebarItem extends HtmlComponent {
@XmlElement private String href;
    @XmlElement private String cssClass;
    @XmlElement private String icon;
    @XmlElement private String label;
    @XmlElement private String id;

    public HtmlSidebarItem(){
        this.setTemplateFile(HtmlComponent.getBasePath()+"/html-sidebar-item.xml");
    }

    // JAXB va chercher <children><sidebar-item>...</sidebar-item></children>
    @XmlElementWrapper(name = "children")
    @XmlElement(name = "sidebar-item")
    private List<HtmlSidebarItem> children = new ArrayList<>();

    @Override
    public void loadContent() {
        if (this.children != null && !this.children.isEmpty()) {
            loadCollapseContent();
        } else {
            loadSimpleContent();
        }
    }

    private void loadCollapseContent() {
        try {
            String template = XmlLoader.getTemplateByType("collapse", this.getTemplateFile());
            StringBuilder childrenHtml = new StringBuilder();
            
            for (HtmlSidebarItem subItem : children) {
                subItem.loadContent();
                childrenHtml.append(subItem.getHtmlContent());
            }

            this.htmlContent = template
                .replace("${cssClass}", nullSafeEscape(this.cssClass))
                .replace("${id}", nullSafeEscape(this.id))
                .replace("${icon}", nullSafeEscape(this.icon))
                .replace("${label}", nullSafeEscape(this.label))
                .replace("${children}", childrenHtml.toString());
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadSimpleContent() {
        try {
            String template = XmlLoader.getTemplateByType("simple", this.getTemplateFile());
            this.htmlContent = template
                .replace("${href}", nullSafeEscape(this.getRealHrefPath()))
                .replace("${cssClass}", nullSafeEscape(this.cssClass))
                .replace("${icon}", nullSafeEscape(this.icon))
                .replace("${label}", nullSafeEscape(this.label));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String getRealHrefPath(){
        if(this.href == null || this.href.isEmpty()){
            return "#";
        }
        String realHref = HtmlComponent.getServletContext().getContextPath()+this.href;
        return realHref;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getCssClass() {
        return cssClass;
    }

    public void setCssClass(String cssClass) {
        this.cssClass = cssClass;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @XmlType(name = "SidebarItemComponentWrapper")
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlSeeAlso({ HtmlSidebarItem.class })
    public static class ComponentWrapper {

        public List<HtmlSidebarItem> childrens = new ArrayList<>();
        private HtmlSidebarItem children;

        public List<HtmlSidebarItem> getChildrens() {
            return childrens;
        }

        public void setChildrens(List<HtmlSidebarItem> childrens) {
            this.childrens = childrens;
        }

        public HtmlSidebarItem getChildren() {
            return children;
        }

        public void setChildren(HtmlSidebarItem children) {
            this.children = children;
        }
    }
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<HtmlSidebarItem> getChildren() {
        return children;
    }

    public void setChildren(List<HtmlSidebarItem> children) {
        this.children = children;
    }

}
