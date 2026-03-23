package mg.miniframework.core.invocation;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import mg.miniframework.service.injection.ServiceInjector;
import mg.miniframework.core.routing.CachedMethodInfo;
import mg.miniframework.core.routing.RouteRegistryBuilder;
import mg.miniframework.logging.LogManager;
import mg.miniframework.service.registry.CachedService;

public class ControllerMethodInvoker {

    private LogManager logManager;
    private String fileSavePath;

    public ControllerMethodInvoker() {
        logManager = new LogManager();
        fileSavePath = new String();
    }

    public Object invokeCorrespondingMethod(CachedMethodInfo cachedInfo, Class<?> clazz, Map<String, String> params,
            CachedService cachedService, HttpServletRequest request, HttpServletResponse resp)
            throws Exception {

        Method method = cachedInfo.getMethod();
        Object instance = clazz.getDeclaredConstructor().newInstance();
        RouteRegistryBuilder.loadControllerSidebar(request, clazz);
        new ServiceInjector(logManager).injectServices(instance, clazz, cachedService);

        List<CachedMethodInfo.ParameterInfo> paramInfos = cachedInfo.getParamInfos();

        Map<String, Object> mapParameters = new HashMap<>();
        Map<Path, byte[]> fileMap = new HashMap<>();
        Map<Path, mg.miniframework.modules.File> fileMap2 = new HashMap<>();

        boolean isMultipart = request.getContentType() != null
                && request.getContentType().toLowerCase().startsWith("multipart/");
        boolean hasSessionVar = false;
        HttpSession session = request.getSession();

        if (isMultipart) {
            mg.miniframework.web.multipart.MultipartRequestData mdata =
                    new mg.miniframework.web.multipart.MultipartRequestReader(logManager, fileSavePath)
                            .read(request);
            mapParameters.putAll(mdata.getFormParameters());
            fileMap.putAll(mdata.getFileBytes());
            fileMap2.putAll(mdata.getFileObjects());
        } else {
            Enumeration<String> parameterNames = request.getParameterNames();
            while (parameterNames.hasMoreElements()) {
                String param = parameterNames.nextElement();
                mapParameters.put(param, request.getParameter(param));
            }
        }

        MethodArgumentResolver resolver = new MethodArgumentResolver(logManager);
        MethodArgumentResolver.ResolveResult res = resolver.resolve(paramInfos, params, mapParameters, fileMap,
                fileMap2, request);

        Object result = method.invoke(instance, res.getArgs());
        if (res.hasSessionVar()) {
            for (Map.Entry<String, Object> sessionVar : res.getSessionVariables().entrySet()) {
                session.setAttribute(sessionVar.getKey(), sessionVar.getValue());
            }
        }
        return result;
    }

    private Charset getRequestEncoding(HttpServletRequest request) {
        String encoding = request.getCharacterEncoding();
        if (encoding == null || encoding.isBlank()) {
            return StandardCharsets.UTF_8;
        }

        try {
            return Charset.forName(encoding);
        } catch (Exception ex) {
            return StandardCharsets.UTF_8;
        }
    }

    private String readPartValue(Part part, Charset encoding) throws IOException {
        try (InputStream input = part.getInputStream()) {
            byte[] bytes = input.readAllBytes();
            return new String(bytes, encoding);
        }
    }

    private byte[] readPartBytes(Part part) throws IOException {
        try (InputStream input = part.getInputStream()) {
            return input.readAllBytes();
        }
    }

    public LogManager getLogManager() {
        return logManager;
    }

    public void setLogManager(LogManager logManager) {
        this.logManager = logManager;
    }

    public String getFileSavePath() {
        return fileSavePath;
    }

    public void setFileSavePath(String fileSavePath) {
        this.fileSavePath = fileSavePath;
    }
}
