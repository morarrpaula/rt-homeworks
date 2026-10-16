package ssatr.service;

import java.util.concurrent.BlockingQueue;

/**
 * Preia mesajele de un anumit tip ("telemetry", "alarm" sau "status") din coada
 * inbox, le salvează și actualizează dashboard-ul.
 *
 * Formatul mesajelor (câmpuri separate prin ";"):
 *   telemetry:  deviceId;timestamp;temperatura
 *   alarm:      deviceId;timestamp;temperatura;prag
 *   status:     deviceId;timestamp;text
 */
public class MessageProcessor implements Runnable {

    private final String kind;
    private final BlockingQueue<String> inbox;
    private final CsvStorage storage;
    private final Dashboard dashboard;

    public MessageProcessor(String kind, BlockingQueue<String> inbox, CsvStorage storage, Dashboard dashboard) {
        this.kind = kind;
        this.inbox = inbox;
        this.storage = storage;
        this.dashboard = dashboard;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // 1. Preluăm un mesaj sosit pe MQTT (blochează thread-ul până sosște mesajul)
                String message = inbox.take();
                // 2. Salvăm mesajul în fișierul CSV (ex: data/telemetry.csv, data/alarm.csv)
                storage.append(kind, message);
                // 3. Parsăm mesajul pentru a actualiza Dashboard-ul
                try {
                    String[] parts = message.split(";");
                    if (parts.length >= 3) {
                        String deviceId = parts[0];
                        long timestamp = Long.parseLong(parts[1]);
                        switch (kind) {
                            case "telemetry":
                                double temp = Double.parseDouble(parts[2]);
                                dashboard.updateTelemetry(deviceId, timestamp, temp);
                                break;
                            case "alarm":
                                double alarmTemp = Double.parseDouble(parts[2]);
                                double threshold = parts.length > 3 ? Double.parseDouble(parts[3]) : 0.0;
                                dashboard.updateAlarm(deviceId, timestamp, alarmTemp, threshold);

                                // Alarmele se afișează imediat în consolă!
                                System.out.println("\n [ALARMĂ NOUĂ] Dispozitiv: " + deviceId
                                        + " | Temp: " + alarmTemp + " °C | Prag: " + threshold);
                                break;
                            case "status":
                                String text = parts[2];
                                dashboard.updateStatus(deviceId, timestamp, text);
                                // Confirmările de status se afișează imediat în consolă!
                                System.out.println("\n [CONFIRMARE STATUS] Dispozitiv: " + deviceId
                                        + " | Răspuns: " + text);
                                break;
                        }
                    }
                } catch (Exception e) {
                    // Mesajele formatate greșit sunt ignorate cu un avertisment
                    System.out.println("Eroare la parsarea mesajului (" + kind + "): " + message);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // ieșim curat la oprire
                break;
            }
        }

        // TODO 4: într-o buclă:
        //   - preluați un mesaj: inbox.take()
        //   - salvați mesajul în fișierul CSV: storage.append(kind, mesaj)
        //     (CsvStorage este gata implementată)
        //   - actualizați dashboard-ul, în funcție de kind
        //   - alarmele și confirmările se afișează imediat în consolă
        //   - un mesaj greșit (format invalid) nu trebuie să oprească thread-ul
    }
}
