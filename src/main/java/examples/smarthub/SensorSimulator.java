package examples.smarthub;

import java.util.Random;

public class SensorSimulator {
    private final Random random = new Random();

    public int readTemperature() {
        return 18 + random.nextInt(7); // 18–24°C
    }

    public int readCO2() {
        return 400 + random.nextInt(600); // 400–1000 ppm
    }

    public int readWaterUsage() {
        return 1 + random.nextInt(10); // 1–10 литров за тик
    }
}
