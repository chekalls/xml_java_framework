package mg.miniframework.persistence.base;

import java.io.Serializable;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

import mg.miniframework.persistence.annotation.Table;
import mg.miniframework.persistence.utils.CGenericUtils;


public abstract class BaseEntity implements Serializable {

    public Object getId() {
        return null;
    }

    public String getTableName(){
        if(this.getClass().isAnnotationPresent(Table.class)){
            return this.getClass().getAnnotation(Table.class).name();
        }
        return null;
    }

    public List<BaseEntity> find(Connection connection, Map<String, Object> criteria) {
        return find(connection, criteria, false);
    }

    @SuppressWarnings("unchecked")
    public List<BaseEntity> search(Connection conn, String search) {
        return (List<BaseEntity>) (List<?>) CGenericUtils.search(conn, this.getClass(), search);
    }

    @SuppressWarnings("unchecked")
    public List<BaseEntity> search(Connection conn, Map<String, Object> searchCriteria) {
        return (List<BaseEntity>) (List<?>) CGenericUtils.search(conn, this.getClass(), searchCriteria);
    }

    @SuppressWarnings("unchecked")
    public List<BaseEntity> find(Connection connection, Map<String, Object> criteria, boolean loadAttributes) {
        return (List<BaseEntity>) (List<?>) CGenericUtils.find(connection, this.getClass(), criteria, loadAttributes);
    }

    public BaseEntity findOne(Connection connection, Map<String, Object> criteria) {
        return findOne(connection, criteria, false);
    }

    public BaseEntity findOne(Connection connection, Map<String, Object> criteria, boolean loadAttributes) {
        return CGenericUtils.findOne(connection, this.getClass(), criteria, loadAttributes);
    }

    public BaseEntity save(Connection connection) {
        return CGenericUtils.save(connection, this);
    }

    public void delete(Connection connection) throws Exception {
        CGenericUtils.delete(connection, this);
    }


    @SuppressWarnings("unchecked")
    public List<BaseEntity> executeQuery(Connection connection, String sql, Object... parameters) {
        return (List<BaseEntity>) (List<?>) CGenericUtils.executeQuery(connection, this.getClass(), sql, parameters);
    }

    @SuppressWarnings("unchecked")
    public List<BaseEntity> executeQuery(Connection connection, String sql, boolean loadAttributes,
            Object... parameters) {
        return (List<BaseEntity>) (List<?>) CGenericUtils.executeQuery(connection, this.getClass(), sql,
                loadAttributes, parameters);
    }

    public BaseEntity executeQueryOne(Connection connection, String sql, Object... parameters) {
        return CGenericUtils.executeQueryOne(connection, this.getClass(), sql, parameters);
    }

    public BaseEntity executeQueryOne(Connection connection, String sql, boolean loadAttributes, Object... parameters) {
        return CGenericUtils.executeQueryOne(connection, this.getClass(), sql, loadAttributes, parameters);
    }

    public static List<Map<String, Object>> executeRawQuery(Connection connection, String sql, Object... parameters) {
        return CGenericUtils.executeRawQuery(connection, sql, parameters);
    }

    public static Map<String, Object> executeRawQueryOne(Connection connection, String sql, Object... parameters) {
        return CGenericUtils.executeRawQueryOne(connection, sql, parameters);
    }

    public static int executeUpdate(Connection connection, String sql, Object... parameters) {
        return CGenericUtils.executeUpdate(connection, sql, parameters);
    }

    public static Object executeScalar(Connection connection, String sql, Object... parameters) {
        return CGenericUtils.executeScalar(connection, sql, parameters);
    }

    public long count(Connection connection, Map<String, Object> criteria) {
        return CGenericUtils.count(connection, this.getClass(), criteria);
    }

    public static boolean exist(Connection connection, BaseEntity entity, Object id) {
        return entity.exist(connection, id);
    }

    public boolean exist(Connection connection, Object id) {
        return CGenericUtils.exist(connection, this.getClass(), id);
    }

    public boolean exist(Connection connection, Map<String, Object> criteria) {
        return CGenericUtils.exist(connection, this.getClass(), criteria);
    }
}