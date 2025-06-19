package api.exception;

public class StorageNotFoundException extends RuntimeException {
    public StorageNotFoundException() {}

    public StorageNotFoundException(String message) {
        super(message);
    }

    public StorageNotFoundException(Exception e) {
        super(e);
    }
}
