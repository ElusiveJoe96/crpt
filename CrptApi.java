import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CrptApi {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;
    private final AtomicInteger requestCounter;
    private final int requestLimit;
    private final long timeIntervalMillis;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.requestCounter = new AtomicInteger(0);
        this.requestLimit = requestLimit;
        this.timeIntervalMillis = timeUnit.toMillis(1);

        this.scheduler.scheduleAtFixedRate(() -> requestCounter.set(0),
                0, timeIntervalMillis, TimeUnit.MILLISECONDS);
    }

    public String createDocument(Document document, String signature) throws InterruptedException, IOException {

        synchronized (this) {
            while (requestCounter.get() >= requestLimit) {
                wait(timeIntervalMillis);
            }
            requestCounter.incrementAndGet();
        }

        String jsonRequest = objectMapper.writeValueAsString(document);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                .header("Content-Type", "application/json")
                .header("Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    public static class Document {
        public String description;
        public String docId;
        public String docStatus;
        public String docType;
        public boolean importRequest;
        public String ownerInn;
        public String participantInn;
        public String producerInn;
        public String productionDate;
        public String productionType;
        public String regDate;
        public String regNumber;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 2);

        Document document = new Document();
        document.description = "Description";
        document.docId = "doc_id";
        document.docStatus = "status";
        document.docType = "LP_INTRODUCE_GOODS";
        document.importRequest = true;
        document.ownerInn = "1234567890";
        document.participantInn = "0987654321";
        document.producerInn = "1122334455";
        document.productionDate = "2024-09-02";
        document.productionType = "production_type";
        document.regDate = "2024-09-02";
        document.regNumber = "reg_number";

        String signature = "signature";
        String response = crptApi.createDocument(document, signature);

        System.out.println(response);
    }
}