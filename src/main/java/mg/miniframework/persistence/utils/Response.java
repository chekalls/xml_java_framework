package mg.miniframework.persistence.utils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Response<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success = true;
    private int status = 200;
    private String message;
    private T data;
    private Object errors;
    private Map<String, Object> meta;
    private long timestamp = System.currentTimeMillis();

    public Response() {
    }

    public static <T> Response<T> ok() {
        return new Response<>();
    }

    public static <T> Response<T> ok(T data) {
        Response<T> r = new Response<>();
        r.setData(data);
        return r;
    }

    public static <T> Response<T> ok(T data, String message) {
        Response<T> r = ok(data);
        r.setMessage(message);
        return r;
    }

    public static <T> Response<T> error(String message) {
        return error(message, 500);
    }

    public static <T> Response<T> error(String message, int status) {
        Response<T> r = new Response<>();
        r.setSuccess(false);
        r.setStatus(status);
        r.setMessage(message);
        return r;
    }

    // --- Fluent setters ---
    public Response<T> withSuccess(boolean success) {
        this.success = success;
        return this;
    }

    public Response<T> withStatus(int status) {
        this.status = status;
        return this;
    }

    public Response<T> withMessage(String message) {
        this.message = message;
        return this;
    }

    public Response<T> withData(T data) {
        this.data = data;
        return this;
    }

    public Response<T> withErrors(Object errors) {
        this.errors = errors;
        return this;
    }

    public Response<T> withMeta(String key, Object value) {
        if (this.meta == null) this.meta = new HashMap<>();
        this.meta.put(key, value);
        return this;
    }

    public Response<T> withMeta(Map<String, Object> meta) {
        this.meta = meta;
        return this;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Object getErrors() {
        return errors;
    }

    public void setErrors(Object errors) {
        this.errors = errors;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public void setMeta(Map<String, Object> meta) {
        this.meta = meta;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Response{" +
                "success=" + success +
                ", status=" + status +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", errors=" + errors +
                ", meta=" + meta +
                ", timestamp=" + timestamp +
                '}';
    }
}
