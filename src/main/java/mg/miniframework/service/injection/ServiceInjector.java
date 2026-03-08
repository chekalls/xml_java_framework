package mg.miniframework.service.injection;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

import mg.miniframework.logging.LogManager;
import mg.miniframework.logging.LogManager.LogStatus;
import mg.miniframework.service.api.FrameworkService;
import mg.miniframework.service.registry.CachedService;

public class ServiceInjector {

    private final LogManager logManager;

    public ServiceInjector(LogManager logManager) {
        this.logManager = logManager;
    }

    public void injectServices(Object controllerInstance, Class<?> controllerClass, CachedService cachedService) {
        Field[] controllerFields = controllerClass.getDeclaredFields();
        for (Field field : controllerFields) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(mg.miniframework.annotation.Service.class)) {
                Object serviceInstance = null;
                Class<?> fieldType = field.getType();
                try {
                    if (FrameworkService.class.isAssignableFrom(fieldType)) {
                        serviceInstance = cachedService.getControllerServiceInstance(controllerClass,
                                fieldType.asSubclass(FrameworkService.class));
                    } else {
                        @SuppressWarnings({ "unchecked", "rawtypes" })
                        Map raw = (Map) cachedService.getControllerServices().get(controllerClass);
                        if (raw != null) {
                            serviceInstance = raw.get(fieldType);
                        }
                        if (serviceInstance == null) {
                            serviceInstance = fieldType.getDeclaredConstructor().newInstance();
                        }
                    }
                } catch (Exception e) {
                    try {
                        logManager.insertLog("Failed to obtain service instance for field " + field.getName() + ": "
                                + e.getMessage(), LogStatus.ERROR);
                    } catch (IOException ioex) {
                        // ignore logging failure
                    }
                }

                if (serviceInstance != null) {
                    try {
                        field.set(controllerInstance, serviceInstance);
                    } catch (IllegalAccessException iae) {
                        // ignore
                    }
                }
            }
        }
    }
}
