package mg.miniframework.ui.base;

public class Sidebar {
    private static String defaultSidebar;
    private String currentSidebar;

    public Sidebar(){}

    public static String getDefaultSidebar() {
        return defaultSidebar;
    }

    public static void setDefaultSidebar(String defaultSidebar) {
        Sidebar.defaultSidebar = defaultSidebar;
    }

    public String getCurrentSidebar() {
        return currentSidebar;
    }

    public void setCurrentSidebar(String currentSidebar) {
        this.currentSidebar = currentSidebar;
    }

}
