package mg.miniframework.modules;

import java.util.HashMap;
import java.util.Map;

public class ModelView {
    private String view;
    private Map<String,Object> dataMap;
    private boolean isXml = false;
    private String template;
    private boolean viewOnly = false;  // when true, treat `view` as final JSP even if template is provided
    private String redirect;


    public ModelView(){
        this.dataMap = new HashMap<>();
    }

    public void setData(String dataKey,Object objectValue){
        dataMap.put(dataKey, objectValue);
    }

    public Map<String, Object> getDataMap() {
        return dataMap;
    }

    public void setDataMap(Map<String, Object> dataMap) {
        this.dataMap = dataMap;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    public boolean isXml() {
        return isXml;
    }

    public void setXml(boolean isXml) {
        this.isXml = isXml;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public boolean isViewOnly() {
        return viewOnly;
    }

    public void setViewOnly(boolean viewOnly) {
        this.viewOnly = viewOnly;
    }

    // alias methods using the term 'view' as mentioned by caller
    public boolean isView() {
        return viewOnly;
    }

    public void setView(boolean viewOnly) {
        this.viewOnly = viewOnly;
    }

    public String getRedirect() {
        return redirect;
    }

    public void setRedirect(String redirect) {
        this.redirect = redirect;
    }
}
