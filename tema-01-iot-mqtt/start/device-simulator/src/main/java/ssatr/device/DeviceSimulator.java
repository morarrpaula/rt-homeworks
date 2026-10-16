package ssatr.device;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Punctul de pornire al simulatorului de device.
 *
 * Rulare:  mvn -q compile exec:java -Dexec.args="device-1"
 * Pentru mai multe device-uri, porniți programul de mai multe ori, cu ID-uri diferite.
 *
 * Structura aplicației:
 *
 *   PeriodicTelemetryTask --(alarmEvents)--> SporadicAlarmTask
 *          |                                        |
 *          +---- telemetry ---> MQTT <--- alarm ----+
 *                                |
 *                            commands ---(commands)--> AperiodicCommandTask ---> status
 */
public class DeviceSimulator {

    public static void main(String[] args) throws Exception {
        String deviceId = args.length > 0 ? args[0] : "device-1";
        System.out.println("Pornire simulator pentru " + deviceId);

        TemperatureSensor sensor = new TemperatureSensor();

        MqttConnection mqtt = new MqttConnection(Config.STUDENT + "-" + deviceId);
        mqtt.connect();

        BlockingQueue<Double> alarmEvents = new LinkedBlockingQueue<>();
        BlockingQueue<String> commands = new LinkedBlockingQueue<>();

        mqtt.subscribe(Config.commandTopic(deviceId), commands);

        PeriodicTelemetryTask periodic = new PeriodicTelemetryTask(deviceId, sensor, mqtt, alarmEvents);
        SporadicAlarmTask sporadic = new SporadicAlarmTask(deviceId, mqtt, alarmEvents, periodic);
        AperiodicCommandTask aperiodic = new AperiodicCommandTask(deviceId, sensor, mqtt, commands, periodic);

        new Thread(periodic, "periodic").start();
        new Thread(sporadic, "sporadic").start();
        new Thread(aperiodic, "aperiodic").start();
    }
}
