package examples.smarthub;

import api.SEntity;
import api.Serfer;
import api.SerferStorage;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SmartHomeHub {

    private static final String DB_FILE = "smart-home.db";

    private Serfer storage;
    private final SensorSimulator simulator = new SensorSimulator();

    public SmartHomeHub() throws IOException {
        storage = SerferStorage.openOrCreate(DB_FILE);
    }

    public void simulateSensorUpdate() throws IOException {
        Map<String, Integer> newStates = Map.of(
                "sensor:temp:livingroom", simulator.readTemperature(),
                "sensor:temp:kitchen", simulator.readTemperature(),
                "sensor:co2:bedroom", simulator.readCO2(),
                "sensor:water:kitchen", simulator.readWaterUsage()
        );

        for (var entry : newStates.entrySet()) {
            String key = entry.getKey();
            int value = entry.getValue();
            System.out.printf("Updating %s → %d%n", key, value);
            storage.insert(key, SEntity.of(value));
        }

    }

    public void printLastKnownState() {
        System.out.println("Last known sensor states:");
        for (String key : new String[]{
                "sensor:temp:livingroom",
                "sensor:temp:kitchen",
                "sensor:co2:bedroom",
                "sensor:water:kitchen"
        }) {
            storage.tryGet(key)
                    .flatMap(SEntity::asInt)
                    .ifPresentOrElse(
                            val -> System.out.printf(" %s = %d%n", key, val),
                            () -> System.out.printf(" %s = N/A%n", key));
        }
    }

    public void shutdown() throws IOException {
        storage.flush();
    }

    public void clean() throws IOException {
        storage.freeStorage();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        SmartHomeHub hub = new SmartHomeHub();


        for (int i = 0; i < 3; i++) {
            hub.printLastKnownState();
            System.out.println("\nSimulating sensor update...");
            hub.simulateSensorUpdate();
            TimeUnit.SECONDS.sleep(1);
        }
        hub.shutdown();

        System.out.println("\nSimulation finished. Restart to see persistent values.");
    }
}
