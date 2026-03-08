package mg.miniframework.core.binding;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Enumeration;

import jakarta.servlet.http.HttpServletRequest;
import mg.miniframework.logging.LogManager;
import mg.miniframework.logging.LogManager.LogStatus;
import mg.miniframework.utils.DataTypeUtils;

public class RequestObjectBinder {

    private final LogManager logManager;

    public RequestObjectBinder(LogManager logManager) {
        this.logManager = logManager;
    }

    public Object bind(Class<?> clazz, HttpServletRequest request, String prefix) throws Exception {

        if (DataTypeUtils.isArrayType(clazz)) {
            logManager.insertLog("array type parameter found : " + clazz.getSimpleName(), LogStatus.DEBUG);
            logManager.insertLog("unsupported function parameter : " + clazz.getName(), LogStatus.ERROR);
            return null;
        }

        Field[] classFields = clazz.getDeclaredFields();
        String className = clazz.getSimpleName().toLowerCase();

        logManager.insertLog("class name : " + className, LogStatus.DEBUG);
        logManager.insertLog("==== found " + classFields.length + " fields", LogStatus.DEBUG);

        String basePrefix = (prefix == null || prefix.isEmpty()) ? className : prefix;

        if (DataTypeUtils.isPrimitiveOrWrapper(clazz) || clazz.equals(String.class)) {
            logManager.insertLog("primitive or String detected for object creation: " + clazz.getName(),
                    LogStatus.DEBUG);
            return null;
        }

        Constructor<?> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object instance = constructor.newInstance();

        for (int n = 0; n < classFields.length; n++) {
            Field field = classFields[n];
            if (Modifier.isStatic(field.getModifiers())) {
                logManager.insertLog("skipping static field: " + field.getName(), LogStatus.DEBUG);
                continue;
            }
            field.setAccessible(true);

            if (!DataTypeUtils.isArrayType(field.getType())) {
                String attributeName = (basePrefix + "." + field.getName()).strip();
                logManager.insertLog(
                        "---- object parameter found :[" + field.getName() + " ::'" + field.getType().getName()
                                + "'] => " + attributeName,
                        LogStatus.DEBUG);

                String fieldValue = request.getParameter(attributeName);

                if (fieldValue != null && !fieldValue.isEmpty()) {
                    Object converted = DataTypeUtils.convertParam(fieldValue, field.getType());
                    field.set(instance, converted);
                } else if (!DataTypeUtils.isPrimitiveOrWrapper(field.getType())
                        && !field.getType().equals(String.class)) {
                    Object subObject = bind(field.getType(), request, attributeName);
                    field.set(instance, subObject);
                }
            } else {
                logManager.insertLog("class foundd ===>" + DataTypeUtils.getContentType(field).getSimpleName(),
                        LogStatus.DEBUG);
                if (DataTypeUtils.isPrimitiveOrWrapper(DataTypeUtils.getContentType(field))) {
                    logManager.insertLog("is primitive", LogStatus.DEBUG);

                    ArrayList<Object> valueList = new ArrayList<>();
                    logManager.insertLog("array attributes found : " + field.getName() + " :: " + field.getType(),
                            LogStatus.DEBUG);
                    for (Enumeration<String> paramEnumeration = request.getParameterNames(); paramEnumeration
                            .hasMoreElements();) {
                        String paramName = paramEnumeration.nextElement();

                        String attributeBaseName = (basePrefix + "." + field.getName() + "[").strip();
                        if (paramName.startsWith(attributeBaseName)) {
                            int indexStart = paramName.indexOf('[') + 1;
                            int indexEnd = paramName.indexOf(']');
                            int index = Integer.parseInt(paramName.substring(indexStart, indexEnd));
                            String attributeName = basePrefix + "." + field.getName() + "[" + index + "]";
                            logManager.insertLog("===== param name : " + attributeName, LogStatus.DEBUG);
                            String fieldValue = request.getParameter(attributeName);
                            Object converted = DataTypeUtils.convertParam(fieldValue,
                                    DataTypeUtils.getContentType(field));
                            valueList.add(index, converted);
                            logManager.insertLog("===== values " + index + " : " + fieldValue + " :: "
                                    + DataTypeUtils.getContentType(field).getSimpleName(), LogStatus.DEBUG);
                        }
                    }
                    field.set(instance, DataTypeUtils.convertListToTargetType(valueList, field.getType(),
                            DataTypeUtils.getContentType(field)));
                } else {
                    ArrayList<Object> valueList = new ArrayList<>();
                    String attributeBaseName = (basePrefix + "." + field.getName() + "[").strip();

                    ArrayList<Integer> indices = new ArrayList<>();
                    for (Enumeration<String> paramEnumeration = request.getParameterNames(); paramEnumeration
                            .hasMoreElements();) {
                        String paramName = paramEnumeration.nextElement();
                        if (paramName.startsWith(attributeBaseName)) {
                            int indexStart = paramName.indexOf('[') + 1;
                            int indexEnd = paramName.indexOf(']');
                            if (indexStart > 0 && indexEnd > indexStart) {
                                try {
                                    int index = Integer.parseInt(paramName.substring(indexStart, indexEnd));
                                    if (!indices.contains(index)) {
                                        indices.add(index);
                                    }
                                } catch (NumberFormatException ex) {
                                    // ignore malformed index
                                }
                            }
                        }
                    }

                    Class<?> contentType = DataTypeUtils.getContentType(field);
                    for (Integer idx : indices) {
                        String elementPrefix = basePrefix + "." + field.getName() + "[" + idx + "]";
                        Object elementInstance = bind(contentType, request, elementPrefix);
                        while (valueList.size() <= idx) {
                            valueList.add(null);
                        }
                        valueList.set(idx, elementInstance);
                    }

                    field.set(instance, DataTypeUtils.convertListToTargetType(valueList, field.getType(),
                            DataTypeUtils.getContentType(field)));
                }
            }
        }

        return instance;
    }
}
