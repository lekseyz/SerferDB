package examples;

import api.SEntity;
import api.Serfer;
import api.SerferStorage;

import java.io.IOException;

public class SimpleSerferUseExample {
    public static void main(String[] args) throws IOException, SerferStorage.StorageAlreadyExistsException, SerferStorage.StorageNotFoundException {
        Serfer db = SerferStorage.create("simple_serfer_use.dump");
        db.insert("somekey", SEntity.of("somevalue"));
        var result = db.get("somekey");
        System.out.println(result.asString().orElse("not found"));
        SerferStorage.deleteStorage("simple_serfer_use.dump");
    }
}
