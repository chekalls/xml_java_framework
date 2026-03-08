package mg.miniframework.service.registry;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import mg.miniframework.service.api.FrameworkService;

public class CachedService {
    private Map<Class<?>, Map<Class<? extends FrameworkService>, FrameworkService>> controllerServices;

    public CachedService() {
        this.controllerServices = new HashMap<>();
    }

    public FrameworkService getControllerServiceInstance(Class<?> controllerClass,
            Class<? extends FrameworkService> serviceClass) throws Exception {
        return controllerServices.get(controllerClass).get(serviceClass);
    }

    public void registerControllerService(Class<?> controllerClass, Class<? extends FrameworkService> serviceClass) {
        try {
            Object serviceInstance = serviceClass.getDeclaredConstructor().newInstance();
            serviceClass.getMethod("init").invoke(serviceInstance);
            initialiseControllerServiceMap(controllerClass);
            this.controllerServices.get(controllerClass).put(serviceClass, (FrameworkService) serviceInstance);

        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void initialiseControllerServiceMap(Class<?> controllerClass) {
        if (this.controllerServices.get(controllerClass) == null) {
            this.controllerServices.put(controllerClass, new HashMap<>());
        }
    }

    public Map<Class<?>, Map<Class<? extends FrameworkService>, FrameworkService>> getControllerServices() {
        return controllerServices;
    }

    public void setControllerServices(
            Map<Class<?>, Map<Class<? extends FrameworkService>, FrameworkService>> controllerServices) {
        this.controllerServices = controllerServices;
    }

}
