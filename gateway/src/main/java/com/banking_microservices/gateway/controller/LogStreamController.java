package com.banking_microservices.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RestController
@RequestMapping("/api/gateway/admin")
public class LogStreamController {

    private static final List<String> ALLOWED_SERVICES = List.of(
            "gateway", "user-service",
            "money-service", "transaction-service", "fraud-service");

    private static final String NAMESPACE = "banking-microservices";
    private static final List<String> TEST_LOG_FILES = List.of(
            "all-logs", "http", "kafka", "db", "pods", "kubectl", "console", "audit");
    private static final String TEST_RUN_ID_PATTERN = "full-e2e-[a-zA-Z0-9_.-]+";

    private Path testLogRoot() {
        String configured = System.getenv().getOrDefault("TEST_LOG_ROOT", ".scriptsandhelpers/logs");
        Path path = Paths.get(configured);
        if (!path.isAbsolute()) {
            path = Paths.get("").toAbsolutePath().resolve(path).normalize();
        }
        return path;
    }

    @GetMapping(value = "/logs/{service}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamLogs(
            @PathVariable String service,
            @RequestParam(defaultValue = "150") int tail) {

        log.info(" > LogStreamController | streamLogs -> Istek alindi. Service: {}, Tail: {}", service, tail);

        if ("all".equals(service)) {
            return buildStream(
                "all",
                List.of("kubectl", "logs", "-n", NAMESPACE,
                    "-l", "app in (gateway,user-service,money-service,transaction-service,fraud-service)",
                    "--tail=" + tail, "-f", "--max-log-requests=50", "--prefix=true")
            );
        }

        if (!ALLOWED_SERVICES.contains(service)) {
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("error").data("[HATA] Gecersiz servis: " + service).build());
        }

        return buildStream(
            service,
            List.of("kubectl", "logs", "-n", NAMESPACE,
                "-l", "app=" + service,
                "--tail=" + tail, "-f", "--max-log-requests=10")
        );
    }

    @GetMapping(value = "/logs/mock", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> mockStream() {
        return Flux.interval(Duration.ofSeconds(1))
                .map(seq -> ServerSentEvent.<String>builder()
                        .event("log")
                        .data("[MOCK] Sistem log " + seq)
                        .build())
                .take(10);
    }

    private Flux<ServerSentEvent<String>> buildStream(String label, List<String> cmd) {
        return Flux.<ServerSentEvent<String>>create(emitter -> {
            Process process = null;
            Thread readerThread = null;
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.redirectErrorStream(true);
                process = pb.start();
                final Process p = process; // lambda icin

                emitter.next(ServerSentEvent.<String>builder()
                        .event("connected")
                        .data("[SYSTEM] " + label + " log stream basladi...")
                        .build());

                // Read in a separate manual thread to avoid blocking WebFlux Schedulers
                // and correctly handle client cancellation
                readerThread = new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                        String line;
                        // isCancelled() was insufficient in readLine() blocking wait. 
                        // Now if process is destroyed, readLine will break with IOException.
                        while (!emitter.isCancelled() && (line = reader.readLine()) != null) {
                            emitter.next(ServerSentEvent.<String>builder().event("log").data(line).build());
                        }
                    } catch (Exception e) {
                        if (!emitter.isCancelled()) {
                            log.error(" > LogStreamController stream kesildi: {}", e.getMessage());
                        }
                    } finally {
                        emitter.complete();
                    }
                });
                readerThread.setDaemon(true);
                readerThread.start();

                // Handle client cancellation (e.g. browser closes connection)
                emitter.onDispose(() -> {
                    log.info(" > LogStreamController | stream iptal edildi. Process sonlandiriliyor.");
                    p.destroyForcibly();
                });

            } catch (Exception e) {
                log.error(" > LogStreamController | buildStream Hata: {}", e.getMessage());
                emitter.next(ServerSentEvent.<String>builder()
                        .event("error")
                        .data("[HATA] kubectl baslatilamadi: " + e.getMessage())
                        .build());
                emitter.complete();
            }
        });
    }

    @GetMapping("/logs/{service}/recent")
    public Flux<String> getRecentLogs(
            @PathVariable String service,
            @RequestParam(defaultValue = "50") int lines) {

        log.info(" > LogStreamController | getRecentLogs -> Istek alindi. Service: {}", service);

        if (!ALLOWED_SERVICES.contains(service) && !"all".equals(service)) {
            return Flux.just("[HATA] Gecersiz servis: " + service);
        }

        List<String> cmd = new ArrayList<>();
        cmd.add("kubectl"); cmd.add("logs");
        cmd.add("-n"); cmd.add(NAMESPACE);
        if ("all".equals(service)) {
            cmd.add("-l"); cmd.add("app in (gateway,user-service,money-service,transaction-service,fraud-service)");
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

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        emitter.next(line);
                    }
                }
                process.waitFor();
                emitter.complete();
            } catch (Exception e) {
                emitter.next("[HATA] " + e.getMessage());
                emitter.complete();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/test-logs/runs")
    public List<Map<String, Object>> listTestLogRuns() throws IOException {
        Path root = testLogRoot();
        if (!Files.isDirectory(root)) {
            return List.of();
        }

        try (Stream<Path> paths = Files.list(root)) {
            return paths
                    .filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().matches(TEST_RUN_ID_PATTERN))
                    .sorted(Comparator.comparing(this::lastModified).reversed())
                    .limit(50)
                    .map(path -> {
                        Map<String, Object> run = new LinkedHashMap<>();
                        run.put("runId", path.getFileName().toString());
                        run.put("modifiedAt", Instant.ofEpochMilli(lastModified(path)).toString());
                        run.put("files", TEST_LOG_FILES.stream()
                                .filter(file -> Files.exists(resolveTestLogFile(path, file)))
                                .collect(Collectors.toList()));
                        return run;
                    })
                    .collect(Collectors.toList());
        }
    }

    @GetMapping(value = "/test-logs/{runId}/{file}", produces = MediaType.TEXT_PLAIN_VALUE)
    public String readTestLog(
            @PathVariable String runId,
            @PathVariable String file,
            @RequestParam(defaultValue = "1200") int lines) throws IOException {

        Path root = testLogRoot();
        if (!runId.matches(TEST_RUN_ID_PATTERN) || !TEST_LOG_FILES.contains(file)) {
            return "[HATA] Gecersiz test log istegi.";
        }

        Path runDir = root.resolve(runId).normalize();
        if (!runDir.startsWith(root) || !Files.isDirectory(runDir)) {
            return "[HATA] Test log run bulunamadi: " + runId;
        }

        Path target = resolveTestLogFile(runDir, file);
        if (!target.startsWith(runDir) || !Files.exists(target)) {
            return "[HATA] Dosya bulunamadi: " + file;
        }

        byte[] bytes = Files.readAllBytes(target);
        String content = bytes.length >= 2 && (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE)
                ? new String(bytes, StandardCharsets.UTF_16LE)
                : new String(bytes, StandardCharsets.UTF_8);
        List<String> allLines = content.lines().collect(Collectors.toList());
        int safeLines = Math.max(1, Math.min(lines, 10000));
        int from = Math.max(0, allLines.size() - safeLines);
        return String.join(System.lineSeparator(), allLines.subList(from, allLines.size()));
    }

    private Path resolveTestLogFile(Path runDir, String file) {
        String name = switch (file) {
            case "console" -> "console.log";
            case "audit" -> "audit.jsonl";
            default -> file + ".txt";
        };
        return runDir.resolve(name).normalize();
    }

    private long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }
}
