package ssatr.service;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Ultima stare cunoscută a fiecărui device: ultima temperatură, momentul
 * ultimului mesaj, numărul de alarme etc.
 *
 * Este actualizată de thread-urile MessageProcessor și citită de CommandConsole,
 * deci accesul trebuie sincronizat.
 */
public class Dashboard {

    public static class DeviceState {
        private final String deviceId;
        private double lastTemperature;
        private long lastMessageTimeMs;
        private int alarmCount;
        private String lastStatusText = "-";
        public DeviceState(String deviceId) {
            this.deviceId = deviceId;
        }
        public synchronized void updateTelemetry(long timestamp, double temp) {
            this.lastTemperature = temp;
            this.lastMessageTimeMs = Math.max(this.lastMessageTimeMs, timestamp);
        }
        public synchronized void updateAlarm(long timestamp, double temp, double threshold) {
            this.lastTemperature = temp;
            this.lastMessageTimeMs = Math.max(this.lastMessageTimeMs, timestamp);
            this.alarmCount++;
        }
        public synchronized void updateStatus(long timestamp, String text) {
            this.lastMessageTimeMs = Math.max(this.lastMessageTimeMs, timestamp);
            this.lastStatusText = text;
        }
        public synchronized String getDeviceId() { return deviceId; }
        public synchronized double getLastTemperature() { return lastTemperature; }
        public synchronized long getLastMessageTimeMs() { return lastMessageTimeMs; }
        public synchronized int getAlarmCount() { return alarmCount; }
        public synchronized String getLastStatusText() { return lastStatusText; }
    }

    private final Map<String, DeviceState> devices = new ConcurrentHashMap<>();

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    public void updateTelemetry(String deviceId, long timestamp, double temp) {
        devices.computeIfAbsent(deviceId, DeviceState::new).updateTelemetry(timestamp, temp);
    }
    public void updateAlarm(String deviceId, long timestamp, double temp, double threshold) {
        devices.computeIfAbsent(deviceId, DeviceState::new).updateAlarm(timestamp, temp, threshold);
    }
    public void updateStatus(String deviceId, long timestamp, String text) {
        devices.computeIfAbsent(deviceId, DeviceState::new).updateStatus(timestamp, text);
    }

    // TODO 5: alegeți structura de date pentru starea device-urilor
    //   (de exemplu un Map<String, ...> cu cheia deviceId)
    //   și adăugați metodele de actualizare apelate din MessageProcessor.

    /** Afișează un tabel cu starea tuturor device-urilor. */
    public void print() {
        if (devices.isEmpty()) {
            System.out.println("Nu există niciun dispozitiv înregistrat în dashboard.");
            return;
        }
        System.out.println("\n=========================================================================================");
        System.out.printf("%-15s | %-12s | %-22s | %-8s | %-25s%n",
                "DEVICE ID", "ULTIMA TEMP", "ULTIMUL MESAJ", "ALARME", "ULTIMUL STATUS");
        System.out.println("=========================================================================================");
        long now = System.currentTimeMillis();
        for (DeviceState state : devices.values()) {
            long lastTime = state.getLastMessageTimeMs();
            long diffSec = (now - lastTime) / 1000;
            String timeFormatted = TIME_FORMATTER.format(Instant.ofEpochMilli(lastTime));
            String ageStr = "acum " + diffSec + "s (" + timeFormatted + ")";
            System.out.printf("%-15s | %-12s | %-22s | %-8d | %-25s%n",
                    state.getDeviceId(),
                    String.format("%.2f °C", state.getLastTemperature()),
                    ageStr,
                    state.getAlarmCount(),
                    state.getLastStatusText());
        }
        System.out.println("=========================================================================================\n");
    }
        // TODO 5: afișați câte un rând pentru fiecare device, de exemplu:
        //   DEVICE     TEMP    ULTIMUL MESAJ   ALARME
        //   device-1   23.41   acum 1 s        2
    }

