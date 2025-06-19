package api.exception;

import api.SerferStorage;

public class StorageAlreadyExistsException extends RuntimeException {
  public StorageAlreadyExistsException() {}

    public StorageAlreadyExistsException(String message) {
        super(message);
    }

    public StorageAlreadyExistsException(Exception e) {
      super(e);
  }
}
