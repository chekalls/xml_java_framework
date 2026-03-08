package mg.miniframework.core.invocation;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import mg.miniframework.core.binding.RequestObjectBinder;
import mg.miniframework.core.routing.CachedMethodInfo;
import mg.miniframework.logging.LogManager;
import mg.miniframework.logging.LogManager.LogStatus;
import mg.miniframework.utils.DataTypeUtils;

public class MethodArgumentResolver {

    private final LogManager logManager;

    public MethodArgumentResolver(LogManager logManager) {
        this.logManager = logManager;
    }

    public static class ResolveResult {
        private final Object[] args;
        private final Map<String, Object> sessionVariables;
        private final boolean hasSessionVar;

        public ResolveResult(Object[] args, Map<String, Object> sessionVariables, boolean hasSessionVar) {
            this.args = args;
            this.sessionVariables = sessionVariables;
            this.hasSessionVar = hasSessionVar;
        }

        public Object[] getArgs() {
            return args;
        }

        public Map<String, Object> getSessionVariables() {
            return sessionVariables;
        }

        public boolean hasSessionVar() {
            return hasSessionVar;
        }
    }

    public ResolveResult resolve(List<CachedMethodInfo.ParameterInfo> paramInfos, Map<String, String> params,
            Map<String, Object> mapParameters, Map<Path, byte[]> fileMap,
            Map<Path, mg.miniframework.modules.File> fileMap2, HttpServletRequest request) throws Exception {

        Object[] args = new Object[paramInfos.size()];
        Map<String, Object> sessionVariables = new HashMap<>();
        boolean hasSessionVar = false;
        HttpSession session = request.getSession();

        for (int i = 0; i < paramInfos.size(); i++) {
            CachedMethodInfo.ParameterInfo info = paramInfos.get(i);
            Object argValue = null;

            if (info.getFormParamName() != null) {
                argValue = request.getParameter(info.getFormParamName());
            } else if (info.getUrlParamName() != null) {
                argValue = params.getOrDefault(info.getUrlParamName(), null);
            } else if (info.getRequestParamName() != null) {
                argValue = request.getParameter(info.getRequestParamName());
                if (argValue == null || argValue.toString().isEmpty()) {
                    argValue = info.getDefaultValue();
                }
            } else {
                if (Map.class.isAssignableFrom(info.getType())) {
                    Type paramType = info.getGenericType();
                    if (DataTypeUtils.isMapOfType(mapParameters, String.class, Object.class, paramType)) {
                        if (info.isSessionVariables()) {
                            Enumeration<String> sessionVars = session.getAttributeNames();
                            while (sessionVars.hasMoreElements()) {
                                String var = sessionVars.nextElement();
                                sessionVariables.put(var, session.getAttribute(var));
                            }
                            logManager.insertLog("map is assignable for String,Object and for session",
                                    LogStatus.DEBUG);
                            hasSessionVar = true;
                            argValue = sessionVariables;
                        } else {
                            logManager.insertLog("map is assignable for String,Object", LogStatus.DEBUG);
                            argValue = mapParameters;
                        }

                    } else if (DataTypeUtils.isMapOfType(fileMap, Path.class, byte[].class, paramType)) {
                        argValue = fileMap;
                        logManager.insertLog("map is assignable for Path,byte[]", LogStatus.DEBUG);

                    } else if (DataTypeUtils.isMapOfType(fileMap2, Path.class, mg.miniframework.modules.File.class,
                            paramType)) {
                        argValue = fileMap2;
                        logManager.insertLog("map is assignable for Path,File", LogStatus.DEBUG);

                    } else {
                        logManager.insertLog("empty map assigned", LogStatus.DEBUG);

                        argValue = Collections.emptyMap();
                    }
                } else {
                    if (DataTypeUtils.isPrimitiveOrWrapper(info.getType()) || info.getType().equals(String.class)) {
                        String paramName = null;
                        try {
                            paramName = info.getParameterName();
                        } catch (Throwable t) {
                            // ignore
                        }

                        String rawValue = null;
                        if (paramName != null && !paramName.isEmpty()) {
                            rawValue = request.getParameter(paramName);
                            if (rawValue == null && params != null) {
                                rawValue = params.get(paramName);
                            }
                        }

                        if (rawValue != null) {
                            argValue = DataTypeUtils.convertParam(rawValue, info.getType());
                        } else {
                            argValue = null;
                        }
                    } else {
                        argValue = new RequestObjectBinder(logManager).bind(info.getType(), request, "");
                    }
                }
            }

            if (argValue != null && !info.getType().isInstance(argValue)) {
                logManager.insertLog("convertion of type required ", LogStatus.DEBUG);
                argValue = DataTypeUtils.convertParam(argValue.toString(), info.getType());
            }

            args[i] = argValue;
        }

        return new ResolveResult(args, sessionVariables, hasSessionVar);
    }
}
