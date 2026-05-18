package com.banking_microservices.admin_service.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
@RestController
@RequestMapping("/api/admin-service")
public class LogStreamController {

    private static final List<String> ALLOWED_SERVICES = List.of(
            "gateway", "user-service", "money-service", "money-service-command", "money-service-query",
            "transaction-service", "fraud-service", "admin-service", "admin-service-command", "admin-service-query",
            "postgres", "redis", "mongodb", "elasticsearch", "zookeeper", "kafka", "keycloak");
    private static final String ALL_LOG_SELECTOR =
            "app in (gateway,user-service,money-service,money-service-command,money-service-query,transaction-service,fraud-service,admin-service,admin-service-command,admin-service-query,postgres,redis,mongodb,elasticsearch,zookeeper,kafka,keycloak)";
    private static final String NAMESPACE = "banking-microservices";
    private static final List<String> TEST_LOG_FILES = List.of("all-logs", "http", "kafka", "db", "pods", "kubectl", "console", "audit");
    private static final String TEST_RUN_ID_PATTERN = "full-e2e-[a-zA-Z0-9_.-]+";

    @GetMapping(value = "/logs/{service}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLogs(@PathVariable String service, @RequestParam(defaultValue = "150") int tail) {
        SseEmitter emitter = new SseEmitter(0L);
        List<String> command = buildLogCommand(service, tail, true);
        new Thread(() -> streamProcess(service, command, emitter)).start();
        return emitter;
    }

    @GetMapping("/logs/{service}/recent")
    public String getRecentLogs(@PathVariable String service, @RequestParam(defaultValue = "50") int lines) {
        List<String> command = buildLogCommand(service, lines, false);
        return run(command, 20).output();
    }

    @GetMapping("/test-logs/runs")
    public List<Map<String, Object>> listTestLogRuns() throws IOException {
        Path root = testLogRoot();
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.list(root)) {
            return paths.filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().matches(TEST_RUN_ID_PATTERN))
                    .sorted(Comparator.comparing(this::lastModified).reversed())
                    .limit(50)
                    .map(path -> {
                        Map<String, Object> run = new LinkedHashMap<>();
                        run.put("runId", path.getFileName().toString());
                        run.put("modifiedAt", Instant.ofEpochMilli(lastModified(path)).toString());
                        run.put("files", TEST_LOG_FILES.stream().filter(file -> Files.exists(resolveTestLogFile(path, file))).toList());
                        return run;
                    }).toList();
        }
    }

    @GetMapping(value = "/test-logs/{runId}/{file}", produces = MediaType.TEXT_PLAIN_VALUE)
    public String readTestLog(@PathVariable String runId, @PathVariable String file, @RequestParam(defaultValue = "1200") int lines) throws IOException {
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
        List<String> allLines = Files.readAllLines(target, StandardCharsets.UTF_8);
        int safeLines = Math.max(1, Math.min(lines, 10000));
        int from = Math.max(0, allLines.size() - safeLines);
        return String.join(System.lineSeparator(), allLines.subList(from, allLines.size()));
    }

    private void streamProcess(String service, List<String> command, SseEmitter emitter) {
        Process process = null;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
            emitter.send(SseEmitter.event().name("connected").data("[SYSTEM] " + service + " log stream basladi..."));
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    emitter.send(SseEmitter.event().name("log").data(line));
                }
            }
            emitter.send(SseEmitter.event().name("disconnected").data("[SYSTEM] log stream bitti."));
            emitter.complete();
        } catch (Exception exception) {
            try {
                emitter.send(SseEmitter.event().name("error").data("[HATA] " + exception.getMessage()));
            } catch (IOException ignored) {
            }
            emitter.completeWithError(exception);
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
        }
    }

    private List<String> buildLogCommand(String service, int tail, boolean follow) {
        if (!ALLOWED_SERVICES.contains(service) && !"all".equals(service)) {
            throw new IllegalArgumentException("Gecersiz servis: " + service);
        }
        List<String> command = new ArrayList<>(List.of("kubectl", "logs", "-n", NAMESPACE));
        if ("all".equals(service)) {
            command.addAll(List.of("-l", ALL_LOG_SELECTOR, "--max-log-requests=50", "--prefix=true"));
        } else {
            command.addAll(List.of("-l", "app=" + service, "--max-log-requests=10"));
        }
        command.add("--tail=" + tail);
        if (follow) {
            command.add("-f");
        }
        return command;
    }

    private CommandResult run(List<String> command, int timeoutSeconds) {
        long start = System.nanoTime();
        Process process = null;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
            StringBuilder output = new StringBuilder();
            Process startedProcess = process;
            Thread reader = new Thread(() -> {
                try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(startedProcess.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        output.append(line).append(System.lineSeparator());
                    }
                } catch (IOException ignored) {
                }
            });
            reader.setDaemon(true);
            reader.start();
            boolean finished = process.waitFor(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new CommandResult(command, 124, true, output.toString(), 0L);
            }
            reader.join(1000);
            return new CommandResult(command, process.exitValue(), false, output.toString(), 0L);
        } catch (Exception exception) {
            if (process != null) process.destroyForcibly();
            return new CommandResult(command, 998, false, exception.getMessage(), 0L);
        }
    }

    private Path testLogRoot() {
        String configured = System.getenv().getOrDefault("TEST_LOG_ROOT", ".scriptsandhelpers/logs");
        Path path = Paths.get(configured);
        if (!path.isAbsolute()) {
            path = Paths.get("").toAbsolutePath().resolve(path).normalize();
        }
        return path;
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
        } catch (IOException ignored) {
            return 0L;
        }
    }

    public record CommandResult(List<String> command, int exitCode, boolean timedOut, String output, long durationMs) {
    }
}
