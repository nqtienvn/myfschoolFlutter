package vn.edu.fpt.myfschool.common.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, String field, Object value) {
        super(String.format("%s không tìm thấy với %s: '%s'", resource, field, value));
    }
}
