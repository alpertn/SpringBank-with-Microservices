package com.banking_microservices.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * cmdye komut yazarak gelen logları controller vasıtasıyla ulastırmak.
 * Admin Log Streaming Controller
 *
 * Kubernetes loglarini SSE (Server-Sent Events) olarak frontend'e iletir.
 * Kullanim: GET /api/gateway/admin/logs/{service}
 *
 * Kullanilan kubectl komutu:
 * kubectl logs -n banking-microservices -l app={service} --tail=200 -f
 * --max-log-requests=10
 *
 * Bu endpoint Spring WebFlux reactive stack uzerinde calisir.
 * Admin panelinde EventSource API ile baglanilir.
 */
@Slf4j
@RestController
@RequestMapping("/api/gateway/admin")
public class LogStreamController {

    /** Desteklenen servisler — sadece bunlara izin verilir */
    private static final List<String> ALLOWED_SERVICES = List.of(
            "gateway", "auth-service", "user-service",
            "money-service", "transaction-service", "fraud-service");

    private static final String NAMESPACE = "banking-microservices";

    /**
     * Belirtilen servise ait Kubernetes loglarini SSE stream olarak dondurur.
     *
     * @param service Servis adi (auth-service, money-service vb.)
     * @param tail    Kac satirlik log gecmisi alinacak (varsayilan 150)
     * @return SSE stream — her satir bir event olarak gelir
     */
    @GetMapping(value = "/logs/{service}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamLogs(
            @PathVariable String service,
            @RequestParam(defaultValue = "150") int tail) {

        log.info(" > LogStreamController | streamLogs -> Istek alindi. Service: {}, Tail: {}", service, tail);

        // Tum servisler isteniyor mu?
        if ("all".equals(service)) {
            return buildStream(
                "all",
                List.of("kubectl", "logs", "-n", NAMESPACE,
                    "-l", "app in (gateway,auth-service,user-service,money-service,transaction-service,fraud-service)",
                    "--tail=" + tail, "-f", "--max-log-requests=50", "--prefix=true")
            );
        }

        // Guvenlik: sadece izin verilen servislere sorgu yapilir
        if (!ALLOWED_SERVICES.contains(service)) {
            log.warn(" > LogStreamController | streamLogs -> Izinsiz servis istegi: {}", service);
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data("[HATA] Gecersiz servis: " + service)
                    .build());
        }

        return buildStream(
            service,
            List.of("kubectl", "logs", "-n", NAMESPACE,
                "-l", "app=" + service,
                "--tail=" + tail, "-f", "--max-log-requests=10")
        );
    }

    /**
     * Verilen kubectl komutu ile SSE log stream olusturur.
     * subscribeOn(Schedulers.boundedElastic()) ile blocking I/O reactive thread pool'a tasiniyor.
     */
    private Flux<ServerSentEvent<String>> buildStream(String label, List<String> cmd) {
        return Flux.<ServerSentEvent<String>>create(emitter -> {
            Process process = null;
            try {
                ProcessBuilder pb = new ProcessBuilder(new ArrayList<>(cmd));
                pb.redirectErrorStream(true);
                process = pb.start();

                emitter.next(ServerSentEvent.<String>builder()
                        .event("connected")
                        .data("[SYSTEM] " + label + " log stream basladi...")
                        .build());

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (emitter.isCancelled()) break;
                        emitter.next(ServerSentEvent.<String>builder()
                                .event("log")
                                .data(line)
                                .build());
                    }
                }

                emitter.next(ServerSentEvent.<String>builder()
                        .event("disconnected")
                        .data("[SYSTEM] Log stream sonlandi.")
                        .build());
                emitter.complete();

            } catch (Exception e) {
                log.error(" > LogStreamController | buildStream -> Hata: {}", e.getMessage());
                emitter.next(ServerSentEvent.<String>builder()
                        .event("error")
                        .data("[HATA] kubectl baglanti hatasi: " + e.getMessage()
                                + " | kubectl PATH'de mi? Namespace dogru mu?")
                        .build());
                emitter.complete();
            } finally {
                if (process != null) process.destroyForcibly();
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .timeout(Duration.ofMinutes(30), Flux.empty());
    }

    /**
     * Tum servislerin son loglarini tek seferde toplar (polling icin).
     * SSE degil — HTTP GET ile JSON doner.
     *
     * @param service Servis adi
     * @param lines   Kac satir (varsayilan 50)
     */
    @GetMapping("/logs/{service}/recent")
    public Flux<String> getRecentLogs(
            @PathVariable String service,
            @RequestParam(defaultValue = "50") int lines) {

        log.info(" > LogStreamController | getRecentLogs -> Istek alindi. Service: {}, Lines: {}", service, lines);

        if (!ALLOWED_SERVICES.contains(service) && !"all".equals(service)) {
            log.warn(" > LogStreamController | getRecentLogs -> Izinsiz servis: {}", service);
            return Flux.just("[HATA] Gecersiz servis: " + service);
        }

        List<String> cmd = new ArrayList<>();
        cmd.add("kubectl"); cmd.add("logs");
        cmd.add("-n"); cmd.add(NAMESPACE);
        if ("all".equals(service)) {
            cmd.add("-l"); cmd.add("app in (gateway,auth-service,user-service,money-service,transaction-service,fraud-service)");
            cmd.add("--max-log-requests=50");
            cmd.add("--prefix=true");
        } else {
            cmd.add("-l"); cmd.add("app=" + service);
            cmd.add("--max-log-requests=10");
        }
        cmd.add("--tail=" + lines);

        return Flux.<String>create(emitter -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        emitter.next(line);
                    }
                }
                process.waitFor();
                emitter.complete();
            } catch (Exception e) {
                log.error(" > LogStreamController | getRecentLogs -> Hata: {}", e.getMessage());
                emitter.next("[HATA] " + e.getMessage());
                emitter.complete();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
