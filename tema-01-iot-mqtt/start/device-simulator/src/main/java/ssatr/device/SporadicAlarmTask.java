package ssatr.device;

import java.util.concurrent.BlockingQueue;

/**
 * TASK SPORADIC: tratează alarmele de temperatură.
 *
 * Evenimentele sosesc în coada alarmEvents (puse de taskul periodic).
 * Intervalul minim între sosiri este MIN_INTERARRIVAL_MS: între două eliberări
 * ale taskului (două alarme publicate) trebuie să treacă cel puțin atât.
 * Intervalul este impus de cod: evenimentele care sosesc mai des sunt
 * respinse, ca să nu fie inundat brokerul.
 */
public class SporadicAlarmTask implements Runnable {

    static final long MIN_INTERARRIVAL_MS = 5000;

    private final String deviceId;
    private final MqttConnection mqtt;
    private final BlockingQueue<Double> alarmEvents;
    private final PeriodicTelemetryTask periodic;
    private long lastAlarmTimeMs = 0;

    public SporadicAlarmTask(String deviceId, MqttConnection mqtt, BlockingQueue<Double> alarmEvents, PeriodicTelemetryTask periodic) {
        this.deviceId = deviceId;
        this.mqtt = mqtt;
        this.alarmEvents = alarmEvents;
        this.periodic = periodic;
    }
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                double temp = alarmEvents.take();
                long now = System.currentTimeMillis();
                if (now - lastAlarmTimeMs < MIN_INTERARRIVAL_MS) {
                    System.out.println("[" + deviceId + "] ALARMĂ RESPINSĂ (prea curând): "
                            + String.format(java.util.Locale.US, "%.2f", temp) + " °C");
                } else {
                    lastAlarmTimeMs = now;
                    String tempFormatted = String.format(java.util.Locale.US, "%.2f", temp);
                    double currentThreshold = (periodic != null) ? periodic.getThreshold() : 28.0;
                    String thresholdFormatted = String.format(java.util.Locale.US, "%.2f", currentThreshold);
                    String payload = deviceId + ";" + now + ";" + tempFormatted + ";" + thresholdFormatted;
                    mqtt.publish(Config.alarmTopic(deviceId), payload);
                    System.out.println("[" + deviceId + "] ALARMĂ TRIMISĂ PE REȚEA: " + payload);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // ieșim din buclă dacă se închide aplicația
                break;
            }
        }
    }

        // TODO 2: implementați taskul sporadic:
        //   - așteptați un eveniment: alarmEvents.take()
        //   - dacă de la ultima eliberare au trecut mai puțin de
        //     MIN_INTERARRIVAL_MS, respingeți evenimentul (afișați un mesaj)
        //   - altfel publicați pe Config.alarmTopic(deviceId) mesajul
        //     "deviceId;timestamp;temperatura;prag"
        //     (pragul curent poate fi transmis împreună cu temperatura sau
        //      citit din PeriodicTelemetryTask; alegeți și justificați în raport)
        //
        // Vezi exemplul exemple/01-taskuri/.../SporadicTaskExample.java
    }

