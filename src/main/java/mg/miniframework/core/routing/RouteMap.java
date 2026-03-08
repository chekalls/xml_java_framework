package mg.miniframework.core.routing;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mg.miniframework.annotation.Controller;
import mg.miniframework.annotation.GetMapping;
import mg.miniframework.annotation.PostMapping;
import mg.miniframework.annotation.Service;
import mg.miniframework.annotation.UrlMap;
import mg.miniframework.service.api.FrameworkService;

public class RouteMap {

    private Map<Class<?>, List<CachedMethodInfo>> methodMaps;
    private Map<Url, CachedMethodInfo> urlMethodsMap;


    public RouteMap() {
        methodMaps = new HashMap<>();
        urlMethodsMap = new HashMap<>();
    }

    public void addController(Class<?> controller) throws Exception {
        Controller controllerAnn = controller.getAnnotation(Controller.class);

        if (controllerAnn == null) {
            throw new IllegalArgumentException(
                    "La classe " + controller.getName() + " n'est pas annotée avec @Controller.");
        }

        String baseUrl = "/";
        try {
            baseUrl = controllerAnn.mapping();
        } catch (Exception ignored) {
        }

        List<CachedMethodInfo> annotatedMethods = new ArrayList<>();

        for (Method m : controller.getDeclaredMethods()) {
            Annotation urlMapAnnotation = findAnnotationByName(m, UrlMap.class.getName());
            if (urlMapAnnotation != null) {
                CachedMethodInfo cachedInfo = new CachedMethodInfo(m);
                annotatedMethods.add(cachedInfo);

                String urlValue = null;
                try {
                    Object value = urlMapAnnotation.annotationType()
                            .getMethod("value")
                            .invoke(urlMapAnnotation);
                    if (value != null) {
                        urlValue = value.toString();
                    }
                } catch (Exception ignored) {
                }

                if (urlValue == null || urlValue.isBlank()) {
                    continue;
                }

                String fullUrl = normalizeUrl(baseUrl, urlValue);

                Url newUrl = new Url();
                newUrl.setUrlPath(fullUrl);
                if (isAMapping(m, newUrl)) {
                    urlMethodsMap.put(newUrl, cachedInfo);
                }
            }
        }
        // loadControllerService(controller);
        methodMaps.put(controller, annotatedMethods);
    }

    public static List<Class<? extends FrameworkService>> getControllerServices(Class<?> controllerClass){
        List<Class<? extends FrameworkService>> fields = new ArrayList<>();
        Field[] controllerFields = controllerClass.getDeclaredFields();
        for (Field field : controllerFields) {
            if(field.getType().getSuperclass().equals(FrameworkService.class)){
                fields.add(field.getType().asSubclass(FrameworkService.class));
            }
        }
        return fields;
    }  

    // private void loadControllerService(Class<?> controller) {
    //     Field[] fields = controller.getDeclaredFields();
    //     for (Field field : fields) {
    //         field.setAccessible(true);
    //         if (field.isAnnotationPresent(Service.class)) {
    //             Class<?> serviceType = field.getType();
    //             if (serviceType.getSuperclass().equals(FrameworkService.class)) {
    //                 try {
    //                     Object serviceInstance = serviceType.newInstance();
    //                     serviceType.getMethod("init").invoke(serviceInstance);
    //                     field.set(controller, serviceInstance);
    //                 } catch (Exception e) {
    //                     e.printStackTrace();
    //                 }
    //             }
    //         }
    //     }
    // }

    private boolean isAMapping(Method method, Url newUrl) throws Exception {
        if (findAnnotationByName(method, PostMapping.class.getName()) != null) {
            newUrl.setMethod(Url.Method.POST);
            return true;
        }

        if (findAnnotationByName(method, GetMapping.class.getName()) != null) {
            newUrl.setMethod(Url.Method.GET);
            return true;
        }

        return false;
    }

    private Annotation findAnnotationByName(Class<?> clazz, String annotationName) {
        for (Annotation annotation : clazz.getAnnotations()) {
            if (annotation.annotationType().getName().equals(annotationName)) {
                return annotation;
            }
        }
        return null;
    }

    private Annotation findAnnotationByName(Method method, String annotationName) {
        for (Annotation annotation : method.getAnnotations()) {
            if (annotation.annotationType().getName().equals(annotationName)) {
                return annotation;
            }
        }
        return null;
    }

    private String normalizeUrl(String baseUrl, String path) {
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        String fullUrl = baseUrl + path;
        fullUrl = fullUrl.replaceAll("/+", "/");

        return fullUrl;
    }

    public Map<Class<?>, List<CachedMethodInfo>> getMethodMaps() {
        return methodMaps;
    }

    public void setMethodMaps(Map<Class<?>, List<CachedMethodInfo>> methodMaps) {
        this.methodMaps = methodMaps;
    }

    public Map<Url, CachedMethodInfo> getUrlMethodsMap() {
        return urlMethodsMap;
    }

    public void setUrlMethodsMap(Map<Url, CachedMethodInfo> urlMethodsMap) {
        this.urlMethodsMap = urlMethodsMap;
    }
}
