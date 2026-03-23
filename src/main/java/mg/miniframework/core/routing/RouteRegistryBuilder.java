package mg.miniframework.core.routing;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequest;
import mg.miniframework.annotation.Sidebar;
import mg.miniframework.core.scanning.ControllerScanner;
import mg.miniframework.logging.LogManager;
import mg.miniframework.logging.LogManager.LogStatus;
import mg.miniframework.service.api.FrameworkService;
import mg.miniframework.service.registry.CachedService;

public class RouteRegistryBuilder {

    // public static 

    public RouteMap build(ServletContext servletContext, LogManager logManager, CachedService cachedService)
            throws Exception {

        RouteMap routeMap = new RouteMap();

        logManager.insertLog("Scanning for controllers...", LogStatus.INFO);
        List<Class<?>> controllers = new ControllerScanner().findControllers(servletContext,
                mg.miniframework.annotation.Controller.class);
        logManager.insertLog("Controllers found: " + controllers.size(), LogStatus.INFO);
        for (Class<?> c : controllers) {
            routeMap.addController(c);
            List<Class<? extends FrameworkService>> controllerServices = RouteMap.getControllerServices(c);
            for (Class<? extends FrameworkService> s : controllerServices) {
                cachedService.registerControllerService(c, s);
            }
        }
        logManager.insertLog("Total routes registered: " + routeMap.getUrlMethodsMap().size(), LogStatus.INFO);
        servletContext.setAttribute("routeMap", routeMap);
        return routeMap;
    }

    public static void loadControllerSidebar(ServletRequest req,Class<?> controller) throws Exception{
        if(controller.isAnnotationPresent(Sidebar.class)){
            Sidebar sidebarAnnotation = controller.getAnnotation(Sidebar.class);
            String module = sidebarAnnotation.prefix();
            String sidebarFile = "/WEB-INF/views/partials/sidebar-"+module+".xml";
            try {
                req.setAttribute("sidebar", sidebarFile);
            } catch (Exception e) {
                throw e;
            }   
        }
    }
}
