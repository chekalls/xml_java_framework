package mg.miniframework.persistence.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

import mg.miniframework.persistence.annotation.Column;
import mg.miniframework.persistence.annotation.Generated;
import mg.miniframework.persistence.annotation.Loader;
import mg.miniframework.persistence.annotation.PrimaryKey;
import mg.miniframework.persistence.base.BaseEntity;
import mg.miniframework.persistence.base.Vue;

import com.fasterxml.jackson.databind.JsonNode;

import org.apache.poi.ss.formula.functions.T;
import org.postgresql.util.PGobject;



public class CGenericUtils {

    // ==================== HELPER METHODS ====================

    /**
     * Récupère tous les champs d'une classe, y compris ceux hérités des classes
     * parentes
     */
    public static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && clazz != Object.class && clazz != BaseEntity.class) {
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                fields.add(field);
            }
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    /**
     * Convertit une valeur SQL vers le type Java attendu
     * Gère notamment la conversion java.sql.Timestamp -> java.time.LocalDateTime
     */
    public static Object convertValue(Object value, Class<?> fieldType) {
        if (value == null) {
            return null;
        }

        // PostgreSQL json/jsonb -> convert via ObjectMapper
        if (value instanceof PGobject) {
            PGobject pg = (PGobject) value;
            String type = pg.getType();
            if ("json".equalsIgnoreCase(type) || "jsonb".equalsIgnoreCase(type)) {
                String json = pg.getValue();
                if (json == null) return null;
                try {
                    if (fieldType == String.class) {
                        return json;
                    }
                    return OBJECT_MAPPER.readValue(json, fieldType);
                } catch (Exception e) {
                    if (fieldType == Map.class) {
                        try {
                            return OBJECT_MAPPER.readValue(json, Map.class);
                        } catch (Exception ex) {
                            return json;
                        }
                    }
                    return json;
                }
            }
        }

        // Enum handling: PostgreSQL may return PGobject or String; some schemas use ordinal (int)
        if (fieldType != null && fieldType.isEnum()) {
            // If value is a PGobject (enum type), extract its textual value
            String enumText = null;
            if (value instanceof PGobject) {
                enumText = ((PGobject) value).getValue();
            } else if (value instanceof String) {
                enumText = (String) value;
            } else if (value instanceof Number) {
                // ordinal mapping
                int ord = ((Number) value).intValue();
                Object[] constants = fieldType.getEnumConstants();
                if (constants != null && ord >= 0 && ord < constants.length) {
                    return constants[ord];
                }
            }

            if (enumText != null) {
                try {
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    Object enumVal = Enum.valueOf((Class) fieldType, enumText);
                    return enumVal;
                } catch (Exception ignore) {
                    // try case-insensitive match on name
                    try {
                        @SuppressWarnings({"unchecked", "rawtypes"})
                        Object[] constants = fieldType.getEnumConstants();
                        if (constants != null) {
                            for (Object c : constants) {
                                if (((Enum<?>) c).name().equalsIgnoreCase(enumText)) {
                                    return c;
                                }
                            }
                        }
                    } catch (Exception ignore2) {
                        // ignore and fall through
                    }
                }
            }
        }

        // Si le type est déjà correct, retourner la valeur
        if (fieldType.isInstance(value)) {
            return value;
        }

        // Conversion Timestamp -> LocalDateTime
        if (fieldType == LocalDateTime.class && value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        }

        // Conversion Date -> LocalDate
        if (fieldType == LocalDate.class && value instanceof Date) {
            return ((Date) value).toLocalDate();
        }

        // Conversion Timestamp -> java.util.Date
        if (fieldType == java.util.Date.class && value instanceof Timestamp) {
            return new java.util.Date(((Timestamp) value).getTime());
        }

        // BigDecimal handling
        if (fieldType == BigDecimal.class) {
            if (value instanceof BigDecimal) {
                return value;
            }
            if (value instanceof Number) {
                try {
                    return new BigDecimal(value.toString());
                } catch (Exception ex) {
                    return null;
                }
            }
            if (value instanceof String) {
                try {
                    return new BigDecimal((String) value);
                } catch (Exception ex) {
                    return null;
                }
            }
        }

        // Autres conversions numériques
        if (fieldType == Integer.class || fieldType == int.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        }
        if (fieldType == Long.class || fieldType == long.class) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
        }
        if (fieldType == Double.class || fieldType == double.class) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
        }
        if (fieldType == Float.class || fieldType == float.class) {
            if (value instanceof Number) {
                return ((Number) value).floatValue();
            }
        }
        if (fieldType == Boolean.class || fieldType == boolean.class) {
            if (value instanceof Boolean) {
                return value;
            }
            if (value instanceof String) {
                return Boolean.parseBoolean((String) value);
            }
        }

        return value;
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static void setPreparedStatementValue(PreparedStatement pstmt, int index, Object value) throws Exception {
        if (value == null) {
            pstmt.setObject(index, null);
            return;
        }

        // Enum handling: prefer sending enum name as Types.OTHER so PostgreSQL driver maps it to enum
        if (value instanceof Enum) {
            String name = ((Enum<?>) value).name();
            pstmt.setObject(index, name, java.sql.Types.OTHER);
            return;
        }

        if (value instanceof BigDecimal) {
            pstmt.setBigDecimal(index, (BigDecimal) value);
            return;
        }

        if (value instanceof Map || value instanceof List || value instanceof JsonNode) {
            PGobject pg = new PGobject();
            pg.setType("jsonb");
            pg.setValue(OBJECT_MAPPER.writeValueAsString(value));
            pstmt.setObject(index, pg);
            return;
        }

        pstmt.setObject(index, value);
    }

    /**
     * Vérifie si un champ doit être ignoré selon l'annotation @Column
     */
    public static boolean isFieldIgnored(Field field) {
        Column columnAnnotation = field.getAnnotation(Column.class);
        if (columnAnnotation != null) {
            return columnAnnotation.ignore();
        }
        return false;
    }

    /**
     * Vérifie si un champ est une clé primaire auto-générée
     */
    public static boolean isAutoGeneratedPrimaryKey(Field field) {
        PrimaryKey pkAnnotation = field.getAnnotation(PrimaryKey.class);
        if (pkAnnotation != null) {
            return pkAnnotation.autGenerated();
        }
        return false;
    }

    /**
     * Vérifie si un champ est auto-généré par la base de données
     */
    public static boolean isGeneratedField(Field field) {
        return field.getAnnotation(Generated.class) != null;
    }

    /**
     * Récupère les champs à inclure dans une requête INSERT
     * Exclut les champs ignorés, les clés primaires auto-générées et les champs
     * auto-générés
     */
    public static List<Field> getFieldsForInsert(Class<?> clazz) {
        List<Field> fieldsForInsert = new ArrayList<>();
        for (Field field : getAllFields(clazz)) {
            if (isFieldIgnored(field)) {
                continue;
            }
            if (isAutoGeneratedPrimaryKey(field)) {
                continue;
            }
            if (isGeneratedField(field)) {
                continue;
            }
            fieldsForInsert.add(field);
        }
        return fieldsForInsert;
    }

    /**
     * Construit une requête INSERT en prenant en compte les clés primaires
     * auto-générées
     */
    public static String buildInsertQuery(Class<?> clazz) throws Exception {
        String tableName = ClassUtils.getTableName(clazz);
        List<Field> fields = getFieldsForInsert(clazz);

        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();

        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                columns.append(", ");
                values.append(", ");
            }
            columns.append(ClassUtils.getFieldName(fields.get(i)));
            values.append("?");
        }

        return "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + values + ")";
    }

    /**
     * Charge les attributs avec la méthode annotée @Loader
     * 
     * @param entity     L'entité sur laquelle appeler les méthodes @Loader
     * @param connection La connexion à la base de données
     */
    public static void loadAttributes(BaseEntity entity, Connection connection) {
        if (entity == null) {
            return;
        }
        Class<?> clazz = entity.getClass();
        try {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Loader.class)) {
                    method.setAccessible(true);
                    method.invoke(entity, connection);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error loading attributes for " + clazz.getSimpleName(), e);
        }
    }

    private static Map<String, Field> buildFieldLookup(Class<?> clazz) {
        Map<String, Field> lookup = new HashMap<>();
        for (Field field : getAllFields(clazz)) {
            if (isFieldIgnored(field)) {
                continue;
            }
            field.setAccessible(true);
            lookup.put(field.getName().toLowerCase(), field);
            lookup.put(ClassUtils.getFieldName(field).toLowerCase(), field);
        }
        return lookup;
    }

    private static Field resolveField(Map<String, Field> fieldLookup, String key) {
        Field field = fieldLookup.get(key.toLowerCase());
        if (field == null) {
            throw new IllegalArgumentException("Unknown field or column: " + key);
        }
        return field;
    }


    public static <T extends BaseEntity> List<T> find(Connection conn,Class<T> clazz,String customWhere,Object... params){
        if (conn == null) {
            throw new IllegalArgumentException("Connection must not be null");
        }
        if (clazz == null) {
            throw new IllegalArgumentException("Class must not be null");
        }

        try {
            StringBuilder query = new StringBuilder(ClassUtils.buildQueryFromClass(clazz));

            if (customWhere != null && !customWhere.trim().isEmpty()) {
                String trimmed = customWhere.trim();
                // If caller provided a clause starting with WHERE/ORDER/LIMIT, append as-is, otherwise prefix with WHERE
                String upper = trimmed.length() >= 6 ? trimmed.substring(0, 6).toUpperCase() : trimmed.toUpperCase();
                if (upper.startsWith("WHERE") || upper.startsWith("ORDER") || upper.startsWith("LIMIT")) {
                    query.append(" ").append(trimmed);
                } else {
                    query.append(" WHERE ").append(trimmed);
                }
            }

            return executeQuery(conn, clazz, query.toString(), params);
        } catch (Exception e) {
            throw new RuntimeException("Error executing find for " + clazz.getSimpleName(), e);
        }
    }

    public static <T extends BaseEntity> List<T> search(Connection conn, Class<T> clazz, String search) {
        if (conn == null) {
            throw new IllegalArgumentException("Connection must not be null");
        }

        List<T> results = new ArrayList<>();

        try {
            // Champs String utilisables pour la recherche
            List<Field> searchableFields = new ArrayList<>();
            for (Field field : getAllFields(clazz)) {
                if (isFieldIgnored(field)) {
                    continue;
                }
                if (field.getType().equals(String.class)) {
                    searchableFields.add(field);
                }
            }

            StringBuilder query = new StringBuilder(ClassUtils.buildQueryFromClass(clazz));
            List<Object> params = new ArrayList<>();

            // if (Referentiel.class.isAssignableFrom(clazz) && clazz != Referentiel.class) {
            //     String categorie = ((Referentiel) clazz.getDeclaredConstructor().newInstance()).getCategorie();
            //     if (categorie != null && !categorie.isBlank()) {
            //         query.append(" WHERE categorie = ?");
            //         params.add(categorie);
            //     }
            // }

            if (search != null && !search.isBlank() && !searchableFields.isEmpty()) {
                query.append(query.indexOf(" WHERE ") >= 0 ? " AND " : " WHERE ");
                for (int i = 0; i < searchableFields.size(); i++) {
                    if (i > 0) {
                        query.append(" OR ");
                    }
                    String columnName = ClassUtils.getFieldName(searchableFields.get(i));
                    query.append(columnName).append(" ILIKE ?");
                }
            }

            try (PreparedStatement pstmt = conn.prepareStatement(query.toString())) {
                int paramIndex = 1;
                for (Object p : params) {
                    setPreparedStatementValue(pstmt, paramIndex++, p);
                }
                if (search != null && !search.isBlank()) {
                    String pattern = "%" + search.trim() + "%";
                    for (int i = 0; i < searchableFields.size(); i++) {
                        pstmt.setString(paramIndex++, pattern);
                    }
                }

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        T instance = clazz.getDeclaredConstructor().newInstance();
                        for (Field field : getAllFields(clazz)) {
                            if (isFieldIgnored(field)) {
                                continue;
                            }
                            field.setAccessible(true);
                            String columnName = ClassUtils.getFieldName(field);
                            Object value = rs.getObject(columnName);
                            Object convertedValue = convertValue(value, field.getType());
                            field.set(instance, convertedValue);
                        }
                        results.add(instance);
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Error executing search for " + clazz.getSimpleName(), e);
        }

        return results;
    }

    /**
     * Recherche d'entités par critères avec LIKE sur les String
     */
    public static <T extends BaseEntity> List<T> search(Connection conn, Class<T> clazz,
            Map<String, Object> searchCriteria) {
        if (conn == null) {
            throw new IllegalArgumentException("Connection must not be null");
        }

        List<T> results = new ArrayList<>();

        try {
            Map<String, Field> fieldsByName = buildFieldLookup(clazz);
            Map<String, Object> effectiveCriteria = (searchCriteria != null) ? new HashMap<>(searchCriteria)
                    : new HashMap<>();

            List<Map.Entry<String, Object>> orderedCriteria = (!effectiveCriteria.isEmpty())
                    ? new ArrayList<>(effectiveCriteria.entrySet())
                    : Collections.emptyList();

            List<String> columnNames = new ArrayList<>(orderedCriteria.size());
            for (Map.Entry<String, Object> entry : orderedCriteria) {
                Field field = resolveField(fieldsByName, entry.getKey());
                columnNames.add(ClassUtils.getFieldName(field));
            }

            // Construction de la requête
            StringBuilder query = new StringBuilder(ClassUtils.buildQueryFromClass(clazz));
            if (!columnNames.isEmpty()) {
                query.append(" WHERE ");
                for (int i = 0; i < columnNames.size(); i++) {
                    if (i > 0)
                        query.append(" AND ");
                    Object value = orderedCriteria.get(i).getValue();
                    if (value instanceof String) {
                        query.append(columnNames.get(i)).append(" LIKE ?");
                    } else {
                        query.append(columnNames.get(i)).append(" = ?");
                    }
                }
            }

            try (PreparedStatement pstmt = conn.prepareStatement(query.toString())) {
                for (int i = 0; i < orderedCriteria.size(); i++) {
                    Object value = orderedCriteria.get(i).getValue();
                    if (value instanceof String) {
                        setPreparedStatementValue(pstmt, i + 1, "%" + value + "%"); // LIKE avec wildcards
                    } else {
                        setPreparedStatementValue(pstmt, i + 1, value);
                    }
                }

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        T instance = clazz.getDeclaredConstructor().newInstance();
                        for (Field field : getAllFields(clazz)) {
                            if (isFieldIgnored(field))
                                continue;
                            field.setAccessible(true);
                            String columnName = ClassUtils.getFieldName(field);
                            Object value = rs.getObject(columnName);
                            Object convertedValue = convertValue(value, field.getType());
                            field.set(instance, convertedValue);
                        }
                        results.add(instance);
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Error executing search for " + clazz.getSimpleName(), e);
        }

        return results;
    }

    /**
     * Recherche d'entités par critères exacts
     */
    public static <T extends BaseEntity> List<T> find(Connection connection, Class<T> clazz,
            Map<String, Object> criteria) {
        return find(connection, clazz, criteria, false);
    }

    /**
     * Recherche d'entités par critères exacts avec option de chargement des
     * attributs
     */
    public static <T extends BaseEntity> List<T> find(Connection connection, Class<T> clazz,
            Map<String, Object> criteria, boolean loadAttributes) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection must not be null");
        }

        List<T> results = new ArrayList<>();

        try {
            Map<String, Field> fieldsByName = buildFieldLookup(clazz);
            Map<String, Object> effectiveCriteria = (criteria != null) ? new HashMap<>(criteria) : new HashMap<>();

            // // Inject categorie for Referentiel subclasses if absent
            // if (Referentiel.class.isAssignableFrom(clazz) && clazz != Referentiel.class
            //         && !effectiveCriteria.containsKey("categorie")) {
            //     String categorie = ((Referentiel) clazz.getDeclaredConstructor().newInstance()).getCategorie();
            //     if (categorie != null && !categorie.isBlank()) {
            //         effectiveCriteria.put("categorie", categorie);
            //     }
            // }

            List<Map.Entry<String, Object>> orderedCriteria = (!effectiveCriteria.isEmpty())
                    ? new ArrayList<>(effectiveCriteria.entrySet())
                    : Collections.emptyList();

            List<Field> resolvedFields = new ArrayList<>(orderedCriteria.size());
            List<String> columnNames = new ArrayList<>(orderedCriteria.size());
            for (Map.Entry<String, Object> entry : orderedCriteria) {
                Field field = resolveField(fieldsByName, entry.getKey());
                resolvedFields.add(field);
                columnNames.add(ClassUtils.getFieldName(field));
            }

            StringBuilder query = new StringBuilder(ClassUtils.buildQueryFromClass(clazz));
            if (!columnNames.isEmpty()) {
                query.append(" WHERE ");
                for (int i = 0; i < columnNames.size(); i++) {
                    if (i > 0) {
                        query.append(" AND ");
                    }
                    query.append(columnNames.get(i)).append(" = ?");
                }
            }

            try (PreparedStatement pstmt = connection.prepareStatement(query.toString())) {
                for (int i = 0; i < orderedCriteria.size(); i++) {
                    setPreparedStatementValue(pstmt, i + 1, orderedCriteria.get(i).getValue());
                }

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        T instance = clazz.getDeclaredConstructor().newInstance();
                        for (Field field : getAllFields(clazz)) {
                            if (isFieldIgnored(field)) {
                                continue;
                            }
                            field.setAccessible(true);
                            String columnName = ClassUtils.getFieldName(field);
                            Object value = rs.getObject(columnName);
                            Object convertedValue = convertValue(value, field.getType());
                            field.set(instance, convertedValue);
                        }
                        results.add(instance);
                    }
                }
            }

            if (loadAttributes) {
                for (T entity : results) {
                    loadAttributes(entity, connection);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error executing find for " + clazz.getSimpleName(), e);
        }
        return results;
    }

    /**
     * Recherche d'une seule entité par critères
     */
    public static <T extends BaseEntity> T findOne(Connection connection, Class<T> clazz, Map<String, Object> criteria) {
        return findOne(connection, clazz, criteria, false);
    }

    /**
     * Recherche d'une seule entité par critères avec option de chargement des
     * attributs
     */
    public static <T extends BaseEntity> T findOne(Connection connection, Class<T> clazz, Map<String, Object> criteria,
            boolean loadAttributes) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection must not be null");
        }

        try {
            Map<String, Field> fieldsByName = buildFieldLookup(clazz);
            Map<String, Object> effectiveCriteria = (criteria != null) ? new HashMap<>(criteria) : new HashMap<>();

            // // Inject categorie for Referentiel subclasses if absent
            // if (Referentiel.class.isAssignableFrom(clazz) && clazz != Referentiel.class
            //         && !effectiveCriteria.containsKey("categorie")) {
            //     String categorie = ((Referentiel) clazz.getDeclaredConstructor().newInstance()).getCategorie();
            //     if (categorie != null && !categorie.isBlank()) {
            //         effectiveCriteria.put("categorie", categorie);
            //     }
            // }

            List<Map.Entry<String, Object>> orderedCriteria = (!effectiveCriteria.isEmpty())
                    ? new ArrayList<>(effectiveCriteria.entrySet())
                    : Collections.emptyList();

            List<Field> resolvedFields = new ArrayList<>(orderedCriteria.size());
            List<String> columnNames = new ArrayList<>(orderedCriteria.size());
            for (Map.Entry<String, Object> entry : orderedCriteria) {
                Field field = resolveField(fieldsByName, entry.getKey());
                resolvedFields.add(field);
                columnNames.add(ClassUtils.getFieldName(field));
            }

            StringBuilder query = new StringBuilder(ClassUtils.buildQueryFromClass(clazz));
            if (!columnNames.isEmpty()) {
                query.append(" WHERE ");
                for (int i = 0; i < columnNames.size(); i++) {
                    if (i > 0) {
                        query.append(" AND ");
                    }
                    query.append(columnNames.get(i)).append(" = ?");
                }
            }
            query.append(" LIMIT 1");

            try (PreparedStatement pstmt = connection.prepareStatement(query.toString())) {
                for (int i = 0; i < orderedCriteria.size(); i++) {
                    setPreparedStatementValue(pstmt, i + 1, orderedCriteria.get(i).getValue());
                }

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }

                    T instance = clazz.getDeclaredConstructor().newInstance();
                    for (Field field : getAllFields(clazz)) {
                        if (isFieldIgnored(field)) {
                            continue;
                        }
                        field.setAccessible(true);
                        String columnName = ClassUtils.getFieldName(field);
                        try {
                            Object value = rs.getObject(columnName);
                            Object convertedValue = convertValue(value, field.getType());
                            field.set(instance, convertedValue);
                        } catch (Exception e) {
                            // Colonne non présente dans le ResultSet, on ignore
                        }
                    }

                    if (loadAttributes) {
                        loadAttributes(instance, connection);
                    }

                    return instance;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error executing findOne for " + clazz.getSimpleName(), e);
        }
    }

    /**
     * Sauvegarde une entité (INSERT ou UPDATE selon la présence de l'ID)
     */
    public static <T extends BaseEntity> T save(Connection connection, T entity) {
        Class<?> clazz = entity.getClass();

        // Prevent accidental save on read-only DB views
        if (Vue.class.isAssignableFrom(clazz) || ClassUtils.getTableName(clazz).toLowerCase().startsWith("v_")) {
            throw new IllegalArgumentException("Cannot save entity mapped to a database view: " + clazz.getSimpleName());
        }

        try {
            // Récupérer tous les champs clés primaires
            List<Field> primaryKeyFields = new ArrayList<>();
            for (Field field : getAllFields(clazz)) {
                PrimaryKey pkAnnotation = field.getAnnotation(PrimaryKey.class);
                if (pkAnnotation != null) {
                    primaryKeyFields.add(field);
                }
            }

            // Si clé primaire simple avec ID auto-généré, utiliser la logique insert/update
            boolean hasSingleAutoGeneratedId = primaryKeyFields.size() == 1
                    && primaryKeyFields.get(0).getAnnotation(PrimaryKey.class).autGenerated();

            if (hasSingleAutoGeneratedId) {
                Integer id = ClassUtils.getClassIdValue(entity);
                if (id == null) {
                    // INSERT
                    String insertSql = buildInsertQuery(clazz);
                    try (var pstmt = connection.prepareStatement(insertSql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                        List<Field> fields = getFieldsForInsert(clazz);
                        int paramIndex = 1;
                        for (Field field : fields) {
                            field.setAccessible(true);
                            Column columnAnnotation = field.getAnnotation(Column.class);
                            Object fieldValue = field.get(entity);
                            if (columnAnnotation != null && columnAnnotation.jsonb()) {
                                if (fieldValue instanceof String) {
                                    PGobject pg = new PGobject();
                                    pg.setType("jsonb");
                                    pg.setValue((String) fieldValue);
                                    setPreparedStatementValue(pstmt, paramIndex++, pg);
                                } else {
                                    setPreparedStatementValue(pstmt, paramIndex++, fieldValue);
                                }
                            } else {
                                setPreparedStatementValue(pstmt, paramIndex++, fieldValue);
                            }
                        }
                        pstmt.executeUpdate();
                        try (var generatedKeys = pstmt.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                Method setIdMethod = clazz.getMethod("setId", Integer.class);
                                setIdMethod.invoke(entity, generatedKeys.getInt(1));
                            }
                        }
                    }
                } else {
                    // UPDATE
                    String updateSql = ClassUtils.buildUpdateQueryFromClass(clazz);
                    try (var pstmt = connection.prepareStatement(updateSql)) {
                        List<Field> fields = getAllFields(clazz);
                        int paramIndex = 1;
                        for (Field field : fields) {
                            if (isFieldIgnored(field)) {
                                continue;
                            }
                            if (isAutoGeneratedPrimaryKey(field)) {
                                continue;
                            }
                            if (isGeneratedField(field)) {
                                continue;
                            }
                            // Also skip the primary key field itself
                            if (field.getAnnotation(PrimaryKey.class) != null) {
                                continue;
                            }
                            field.setAccessible(true);
                            Column columnAnnotation = field.getAnnotation(Column.class);
                            Object fieldValue = field.get(entity);
                            if (columnAnnotation != null && columnAnnotation.jsonb()) {
                                if (fieldValue instanceof String) {
                                    PGobject pg = new PGobject();
                                    pg.setType("jsonb");
                                    pg.setValue((String) fieldValue);
                                    setPreparedStatementValue(pstmt, paramIndex++, pg);
                                } else {
                                    setPreparedStatementValue(pstmt, paramIndex++, fieldValue);
                                }
                            } else {
                                setPreparedStatementValue(pstmt, paramIndex++, fieldValue);
                            }
                        }
                        setPreparedStatementValue(pstmt, paramIndex, ClassUtils.getClassIdValue(entity));
                        pstmt.executeUpdate();
                    }
                }
            } else {
                // Clé primaire composite ou sans ID auto-généré : INSERT simple
                String insertSql = buildInsertQuery(clazz);
                try (var pstmt = connection.prepareStatement(insertSql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                    List<Field> fields = getFieldsForInsert(clazz);
                    int paramIndex = 1;
                    for (Field field : fields) {
                        field.setAccessible(true);
                        Column columnAnnotation = field.getAnnotation(Column.class);
                        Object fieldValue = field.get(entity);
                        if (columnAnnotation != null && columnAnnotation.jsonb()) {
                            if (fieldValue instanceof String) {
                                PGobject pg = new PGobject();
                                pg.setType("jsonb");
                                pg.setValue((String) fieldValue);
                                setPreparedStatementValue(pstmt, paramIndex++, pg);
                            } else {
                                setPreparedStatementValue(pstmt, paramIndex++, fieldValue);
                            }
                        } else {
                            setPreparedStatementValue(pstmt, paramIndex++, fieldValue);
                        }
                    }
                    pstmt.executeUpdate();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error saving " + clazz.getSimpleName()+" : " + e.getMessage(), e);
        }
        return entity;
    }

    /**
     * Supprime une entité de la base de données
     * Gère les clés primaires simples et composites
     */
    public static void delete(Connection connection, BaseEntity entity) throws Exception {
        Class<?> clazz = entity.getClass();
        String tableName = ClassUtils.getTableName(clazz);

        // Récupérer tous les champs annotés comme clés primaires
        List<Field> primaryKeyFields = new ArrayList<>();
        List<Object> primaryKeyValues = new ArrayList<>();

        for (Field field : getAllFields(clazz)) {
            PrimaryKey pkAnnotation = field.getAnnotation(PrimaryKey.class);
            if (pkAnnotation != null) {
                field.setAccessible(true);
                primaryKeyFields.add(field);
                primaryKeyValues.add(field.get(entity));
            }
        }

        if (primaryKeyFields.isEmpty()) {
            throw new IllegalStateException("No primary key fields found in " + clazz.getSimpleName());
        }

        // Construire la clause WHERE avec tous les champs de clé primaire
        StringBuilder deleteSql = new StringBuilder("DELETE FROM ").append(tableName).append(" WHERE ");
        for (int i = 0; i < primaryKeyFields.size(); i++) {
            if (i > 0) {
                deleteSql.append(" AND ");
            }
            deleteSql.append(ClassUtils.getFieldName(primaryKeyFields.get(i))).append(" = ?");
        }

        try (PreparedStatement pstmt = connection.prepareStatement(deleteSql.toString())) {
            for (int i = 0; i < primaryKeyValues.size(); i++) {
                setPreparedStatementValue(pstmt, i + 1, primaryKeyValues.get(i));
            }
            pstmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error deleting " + clazz.getSimpleName(), e);
        }
    }

    // ==================== CUSTOM QUERIES ====================

    /**
     * Exécute une requête SQL personnalisée et mappe les résultats vers l'entité
     */
    public static <T extends BaseEntity> List<T> executeQuery(Connection connection, Class<T> clazz, String sql,
            Object... parameters) {
        return executeQuery(connection, clazz, sql, false, parameters);
    }

    /**
     * Exécute une requête SQL personnalisée et mappe les résultats vers l'entité
     */
    public static <T extends BaseEntity> List<T> executeQuery(Connection connection, Class<T> clazz, String sql,
            boolean loadAttributes, Object... parameters) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection must not be null");
        }
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL query must not be null or empty");
        }

        List<T> results = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            // Bind les paramètres
            if (parameters != null) {
                for (int i = 0; i < parameters.length; i++) {
                    setPreparedStatementValue(pstmt, i + 1, parameters[i]);
                }
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    T instance = clazz.getDeclaredConstructor().newInstance();
                    for (Field field : getAllFields(clazz)) {
                        if (isFieldIgnored(field)) {
                            continue;
                        }
                        field.setAccessible(true);
                        String columnName = ClassUtils.getFieldName(field);
                        try {
                            Object value = rs.getObject(columnName);
                            Object convertedValue = convertValue(value, field.getType());
                            field.set(instance, convertedValue);
                        } catch (Exception e) {
                            // Colonne non présente dans le ResultSet, on ignore
                        }
                    }
                    results.add(instance);
                }

                if (loadAttributes) {
                    for (T entity : results) {
                        loadAttributes(entity, connection);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error executing custom query: " + sql, e);
        }
        return results;
    }

    /**
     * Exécute une requête SQL personnalisée et retourne un seul résultat
     */
    public static <T extends BaseEntity> T executeQueryOne(Connection connection, Class<T> clazz, String sql,
            Object... parameters) {
        return executeQueryOne(connection, clazz, sql, false, parameters);
    }

    /**
     * Exécute une requête SQL personnalisée et retourne un seul résultat
     */
    public static <T extends BaseEntity> T executeQueryOne(Connection connection, Class<T> clazz, String sql,
            boolean loadAttributes, Object... parameters) {
        List<T> results = executeQuery(connection, clazz, sql, loadAttributes, parameters);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Exécute une requête SQL personnalisée et retourne les résultats sous forme de
     * Map
     */
    public static List<Map<String, Object>> executeRawQuery(Connection connection, String sql, Object... parameters) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection must not be null");
        }
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL query must not be null or empty");
        }

        List<Map<String, Object>> results = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            // Bind les paramètres
            if (parameters != null) {
                for (int i = 0; i < parameters.length; i++) {
                    setPreparedStatementValue(pstmt, i + 1, parameters[i]);
                }
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                var metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);
                        row.put(columnName, value);
                    }
                    results.add(row);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error executing raw query: " + sql, e);
        }
        return results;
    }

    /**
     * Exécute une requête SQL personnalisée et retourne une seule ligne sous forme
     * de Map
     */
    public static Map<String, Object> executeRawQueryOne(Connection connection, String sql, Object... parameters) {
        List<Map<String, Object>> results = executeRawQuery(connection, sql, parameters);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Exécute une requête de modification (INSERT, UPDATE, DELETE)
     */
    public static int executeUpdate(Connection connection, String sql, Object... parameters) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection must not be null");
        }
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL query must not be null or empty");
        }

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            // Bind les paramètres
            if (parameters != null) {
                for (int i = 0; i < parameters.length; i++) {
                    setPreparedStatementValue(pstmt, i + 1, parameters[i]);
                }
            }

            return pstmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error executing update query: " + sql, e);
        }
    }

    /**
     * Exécute une requête qui retourne une seule valeur (COUNT, SUM, MAX, etc.)
     */
    public static Object executeScalar(Connection connection, String sql, Object... parameters) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection must not be null");
        }
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL query must not be null or empty");
        }

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            // Bind les paramètres
            if (parameters != null) {
                for (int i = 0; i < parameters.length; i++) {
                    setPreparedStatementValue(pstmt, i + 1, parameters[i]);
                }
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getObject(1);
                }
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error executing scalar query: " + sql, e);
        }
    }

    /**
     * Compte le nombre d'enregistrements selon des critères
     */
    public static <T extends BaseEntity> long count(Connection connection, Class<T> clazz,
            Map<String, Object> criteria) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection must not be null");
        }

        try {
            String tableName = ClassUtils.getTableName(clazz);
            StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ").append(tableName);

            Map<String, Object> effectiveCriteria = (criteria != null) ? new HashMap<>(criteria) : new HashMap<>();
            // // Inject categorie for Referentiel subclasses if absent
            // if (Referentiel.class.isAssignableFrom(clazz) && clazz != Referentiel.class
            //         && !effectiveCriteria.containsKey("categorie")) {
            //     String categorie = ((Referentiel) clazz.getDeclaredConstructor().newInstance()).getCategorie();
            //     if (categorie != null && !categorie.isBlank()) {
            //         effectiveCriteria.put("categorie", categorie);
            //     }
            // }

            List<Object> parameters = new ArrayList<>();
            if (!effectiveCriteria.isEmpty()) {
                sql.append(" WHERE ");
                Map<String, Field> fieldsByName = buildFieldLookup(clazz);
                int count = 0;
                for (Map.Entry<String, Object> entry : effectiveCriteria.entrySet()) {
                    if (count > 0) {
                        sql.append(" AND ");
                    }
                    Field field = resolveField(fieldsByName, entry.getKey());
                    sql.append(ClassUtils.getFieldName(field)).append(" = ?");
                
                    parameters.add(entry.getValue());
                    count++;
                }
            }

            Object result = executeScalar(connection, sql.toString(), parameters.toArray());
            return result != null ? ((Number) result).longValue() : 0;
        } catch (Exception e) {
            throw new RuntimeException("Error executing count for " + clazz.getSimpleName(), e);
        }
    }

    /**
     * Vérifie si une entité existe avec une clé primaire simple
     */
    public static <T extends BaseEntity> boolean exist(Connection connection, Class<T> clazz, Object id) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection must not be null");
        }
        if (id == null) {
            return false;
        }

        try {
            // Trouver le champ clé primaire
            Field primaryKeyField = null;
            for (Field field : getAllFields(clazz)) {
                PrimaryKey pkAnnotation = field.getAnnotation(PrimaryKey.class);
                if (pkAnnotation != null) {
                    if (primaryKeyField != null) {
                        throw new IllegalStateException(
                                "Multiple primary keys found. Use exist(Connection, Class, Map) for composite keys.");
                    }
                    primaryKeyField = field;
                }
            }

            if (primaryKeyField == null) {
                throw new IllegalStateException("No primary key field found in " + clazz.getSimpleName());
            }

            String tableName = ClassUtils.getTableName(clazz);
            String columnName = ClassUtils.getFieldName(primaryKeyField);
            String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE " + columnName + " = ?";

            Object result = executeScalar(connection, sql, id);
            return result != null && ((Number) result).longValue() > 0;
        } catch (Exception e) {
            throw new RuntimeException("Error checking existence for " + clazz.getSimpleName(), e);
        }
    }

    /**
     * Vérifie si une entité existe selon des critères
     */
    public static <T extends BaseEntity> boolean exist(Connection connection, Class<T> clazz,
            Map<String, Object> criteria) {
        return count(connection, clazz, criteria) > 0;
    }

    /**
     * Exécute une requête paginée personnalisée avec comptage automatique
     * Utilise la requête fournie pour compter et pour récupérer les données
     */
    public static <T extends BaseEntity> Page<T> executeQueryWithPagination(
            Connection connection, Class<T> clazz, String countSql, String dataSql,
            int pageNumber, int pageSize, boolean loadAttributes, Object... parameters) {
        
        if (pageNumber < 1) pageNumber = 1;
        if (pageSize < 1) pageSize = 10;

        try {
            // Récupérer le nombre total d'éléments
            Object countResult = executeScalar(connection, countSql, parameters);
            long total = countResult != null ? ((Number) countResult).longValue() : 0L;

            // Calculer le nombre de pages
            int totalPages = (int) Math.max(1, Math.ceil((double) total / pageSize));
            if (pageNumber > totalPages) {
                pageNumber = totalPages;
            }

            // Exécuter la requête de données avec LIMIT et OFFSET
            List<T> content = executeQuery(connection, clazz, dataSql, loadAttributes, parameters);

            return new Page<>(content, pageNumber, pageSize, total);
        } catch (Exception e) {
            throw new RuntimeException("Error executing paginated query for " + clazz.getSimpleName(), e);
        }
    }

    /**
     * Recherche paginée par critères (avec WHERE clause supportant ILIKE)
     */
    public static <T extends BaseEntity> Page<T> findPaginated(
            Connection connection, Class<T> clazz, Map<String, Object> criteria,
            int pageNumber, int pageSize, boolean loadAttributes) {
        
        if (connection == null) {
            throw new IllegalArgumentException("Connection must not be null");
        }
        if (pageNumber < 1) pageNumber = 1;
        if (pageSize < 1) pageSize = 10;

        try {
            String table = ClassUtils.getTableName(clazz);
            Map<String, Field> fieldsByName = buildFieldLookup(clazz);
            Map<String, Object> effectiveCriteria = (criteria != null) ? new HashMap<>(criteria) : new HashMap<>();

            // Inject categorie for Referentiel subclasses if absent
            // if (Referentiel.class.isAssignableFrom(clazz) && clazz != Referentiel.class
            //         && !effectiveCriteria.containsKey("categorie")) {
            //     String categorie = ((Referentiel) clazz.getDeclaredConstructor().newInstance()).getCategorie();
            //     if (categorie != null && !categorie.isBlank()) {
            //         effectiveCriteria.put("categorie", categorie);
            //     }
            // }

            List<Map.Entry<String, Object>> orderedCriteria = (!effectiveCriteria.isEmpty())
                    ? new ArrayList<>(effectiveCriteria.entrySet())
                    : Collections.emptyList();

            List<String> columnNames = new ArrayList<>();
            for (Map.Entry<String, Object> entry : orderedCriteria) {
                Field field = resolveField(fieldsByName, entry.getKey());
                columnNames.add(ClassUtils.getFieldName(field));
            }

            // Construire la clause WHERE
            StringBuilder whereClause = new StringBuilder();
            if (!columnNames.isEmpty()) {
                whereClause.append(" WHERE ");
                for (int i = 0; i < columnNames.size(); i++) {
                    if (i > 0) whereClause.append(" AND ");
                    Object value = orderedCriteria.get(i).getValue();
                    if (value instanceof String) {
                        whereClause.append(columnNames.get(i)).append(" LIKE ?");
                    } else {
                        whereClause.append(columnNames.get(i)).append(" = ?");
                    }
                }
            }

            // Préparer les paramètres
            List<Object> params = new ArrayList<>();
            for (Map.Entry<String, Object> entry : orderedCriteria) {
                Object value = entry.getValue();
                if (value instanceof String) {
                    params.add("%" + value + "%");
                } else {
                    params.add(value);
                }
            }

            // Requête de comptage
            String countSql = "SELECT COUNT(*) FROM " + table + whereClause.toString();
            Object countResult = executeScalar(connection, countSql, params.toArray());
            long total = countResult != null ? ((Number) countResult).longValue() : 0L;

            // Calculer les pages
            int totalPages = (int) Math.max(1, Math.ceil((double) total / pageSize));
            if (pageNumber > totalPages) {
                pageNumber = totalPages;
            }

            int offset = (pageNumber - 1) * pageSize;

            // Requête de données
                String dataSql = "SELECT * FROM " + table + whereClause.toString() 
                    + " ORDER BY id DESC LIMIT " + pageSize + " OFFSET " + offset;
            List<T> content = executeQuery(connection, clazz, dataSql, loadAttributes, params.toArray());

            return new Page<>(content, pageNumber, pageSize, total);
        } catch (Exception e) {
            throw new RuntimeException("Error executing paginated find for " + clazz.getSimpleName(), e);
        }
    }

    /**
     * Recherche paginée avec recherche textuelle (ILIKE sur champs String) et filtres exacts
     */
    public static <T extends BaseEntity> Page<T> searchPaginated(
            Connection connection, Class<T> clazz, String search, Map<String, Object> filters,
            int pageNumber, int pageSize, boolean loadAttributes) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection must not be null");
        }
        if (pageNumber < 1) pageNumber = 1;
        if (pageSize < 1) pageSize = 10;

        try {
            String table = ClassUtils.getTableName(clazz);

            // Champs String utilisables pour la recherche
            List<Field> searchableFields = new ArrayList<>();
            for (Field field : getAllFields(clazz)) {
                if (isFieldIgnored(field)) continue;
                if (field.getType().equals(String.class)) {
                    searchableFields.add(field);
                }
            }

            StringBuilder whereClause = new StringBuilder();
            List<Object> params = new ArrayList<>();

            // Filtres exacts (criteria)
            if (filters != null && !filters.isEmpty()) {
                int idx = 0;
                for (Map.Entry<String, Object> entry : filters.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    Field field = buildFieldLookup(clazz).get(key.toLowerCase());
                    if (field == null) {
                        throw new IllegalArgumentException("Unknown filter field: " + key);
                    }
                    String columnName = ClassUtils.getFieldName(field);
                    if (idx == 0) {
                        whereClause.append(" WHERE ");
                    } else {
                        whereClause.append(" AND ");
                    }
                    if (value instanceof String) {
                        whereClause.append(columnName).append(" LIKE ?");
                        params.add("%" + value + "%");
                    } else {
                        whereClause.append(columnName).append(" = ?");
                        params.add(value);
                    }
                    idx++;
                }
            }

            // Recherche textuelle sur champs string
            if (search != null && !search.trim().isEmpty() && !searchableFields.isEmpty()) {
                if (whereClause.length() == 0) whereClause.append(" WHERE "); else whereClause.append(" AND ");
                String pattern = "%" + search.trim() + "%";
                whereClause.append("(");
                for (int i = 0; i < searchableFields.size(); i++) {
                    if (i > 0) whereClause.append(" OR ");
                    whereClause.append(ClassUtils.getFieldName(searchableFields.get(i))).append(" ILIKE ?");
                    params.add(pattern);
                }
                whereClause.append(")");
            }

            // Requête de comptage
            String countSql = "SELECT COUNT(*) FROM " + table + whereClause.toString();
            Object countResult = executeScalar(connection, countSql, params.toArray());
            long total = countResult != null ? ((Number) countResult).longValue() : 0L;

            // Calculer les pages
            int totalPages = (int) Math.max(1, Math.ceil((double) total / pageSize));
            if (pageNumber > totalPages) {
                pageNumber = totalPages;
            }

            int offset = (pageNumber - 1) * pageSize;

            // Requête de données
            String dataSql = "SELECT * FROM " + table + whereClause.toString()
                    + " ORDER BY id DESC LIMIT " + pageSize + " OFFSET " + offset;
            List<T> content = executeQuery(connection, clazz, dataSql, loadAttributes, params.toArray());

            return new Page<>(content, pageNumber, pageSize, total);
        } catch (Exception e) {
            throw new RuntimeException("Error executing paginated search for " + clazz.getSimpleName(), e);
        }
    }
}
