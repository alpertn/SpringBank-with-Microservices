package com.banking_microservices.admin_service.controller;

import com.banking_microservices.admin_service.grpc.AdminHistoryQueryGrpcClient;
import com.banking_microservices.admin_service.service.AdminHistoryDispatchService;
import com.banking_microservices.admin_service.service.AdminUserContextExtractor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin-service/v1")
public class AdminOpsController {

    private static final String NAMESPACE = "banking-microservices";
    private static final Set<String> DEPLOYMENTS = Set.of(
            "gateway", "user-service", "money-service", "money-service-command", "money-service-query",
            "transaction-service", "fraud-service", "admin-service", "admin-service-command", "admin-service-query",
            "postgres", "redis", "mongodb", "elasticsearch", "zookeeper", "kafka", "keycloak");
    private static final List<String> APP_DEPLOYMENTS = List.of(
            "gateway", "user-service", "money-service", "money-service-command", "money-service-query",
            "transaction-service", "fraud-service", "admin-service", "admin-service-command", "admin-service-query");
    private static final Set<String> POSTGRES_DATABASES = Set.of(
            "banking", "banking_fraud", "banking_keycloak", "banking_money", "banking_money_command",
            "banking_transactions", "banking_users", "banking_admin_command");
    private static final Set<String> MONGO_DATABASES = Set.of("banking_money_query", "banking_admin_query");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AdminHistoryDispatchService historyDispatchService;
    private final AdminHistoryQueryGrpcClient historyQueryGrpcClient;
    private final AdminUserContextExtractor adminUserContextExtractor;

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        Map<String, Object> result = baseResult("overview");
        result.put("deployments", run(List.of("kubectl", "-n", NAMESPACE, "get", "deploy", "-o", "wide"), 30));
        result.put("pods", run(List.of("kubectl", "-n", NAMESPACE, "get", "pods", "-o", "wide"), 30));
        result.put("hpa", run(List.of("kubectl", "-n", NAMESPACE, "get", "hpa"), 30));
        result.put("events", run(List.of("kubectl", "-n", NAMESPACE, "get", "events", "--sort-by=.lastTimestamp"), 30));
        result.put("topPods", runAllowFail(List.of("kubectl", "-n", NAMESPACE, "top", "pods"), 20));
        audit("Kubernetes overview opened", Map.of("namespace", NAMESPACE));
        return result;
    }

    @GetMapping("/config")
    public Map<String, Object> config() {
        Map<String, Object> result = baseResult("config");
        Map<String, String> runtime = new LinkedHashMap<>();
        runtime.put("kubernetes.namespace", NAMESPACE);
        runtime.put("admin.logRoot", testLogRoot().toString());
        runtime.put("admin.auditPath", auditPath().toString());
        runtime.put("admin.backupRoot", backupRoot().toString());

        Map<String, String> topics = new LinkedHashMap<>();
        topics.put("admin-command", "banking-microservices.admin.history.command.v1");
        topics.put("admin-projection-sync", "banking-microservices.admin.history.projection-sync.v1");
        topics.put("money-projection-sync", "banking-microservices.money.projection-sync.v1");
        topics.put("transaction-created", "banking-microservices.transaction.created.v1");

        result.put("runtime", runtime);
        result.put("topics", topics);
        result.put("deploymentEnv", runAllowFail(List.of("kubectl", "-n", NAMESPACE, "get", "deploy", "-o", "jsonpath={range .items[*]}{.metadata.name}{': '}{range .spec.template.spec.containers[0].env[*]}{.name}{'='}{.value}{' | '}{end}{'\\n'}{end}"), 30));
        audit("System config viewed", Map.of("topics", topics.size()));
        return result;
    }

    @GetMapping("/kafka")
    public Map<String, Object> kafka() {
        Map<String, Object> result = baseResult("kafka");
        String script = String.join(" ",
                "kafka-topics --bootstrap-server kafka:9092 --list;",
                "echo '--- consumer groups ---';",
                "kafka-consumer-groups --bootstrap-server kafka:9092 --list || true;",
                "echo '--- offsets ---';",
                "for g in $(kafka-consumer-groups --bootstrap-server kafka:9092 --list 2>/dev/null); do",
                "echo \"## $g\";",
                "kafka-consumer-groups --bootstrap-server kafka:9092 --describe --group \"$g\" 2>/dev/null || true;",
                "done");
        result.put("raw", execInApp("kafka", List.of("sh", "-lc", script), 45));
        audit("Kafka monitor opened", Map.of());
        return result;
    }

    @GetMapping("/cqrs")
    public Map<String, Object> cqrs() {
        Map<String, Object> result = baseResult("cqrs");
        result.put("moneyQueryHealth", httpInsideCluster("http://money-service-query:8093/api/money-service-query/v1/accounts/health"));
        result.put("adminQueryHealth", httpInsideCluster("http://admin-service-query:8098/api/admin-service-query/v1/history/health"));
        result.put("mongoMoneyCount", execInApp("mongodb", List.of("mongosh", "banking_money_query", "--quiet", "--eval", "db.money_accounts.countDocuments()"), 20));
        result.put("mongoAdminCount", execInApp("mongodb", List.of("mongosh", "banking_admin_query", "--quiet", "--eval", "db.admin_query_history.countDocuments()"), 20));
        result.put("elasticIndices", httpInsideCluster("http://elasticsearch:9200/_cat/indices?v"));
        audit("CQRS monitor opened", Map.of());
        return result;
    }

    @GetMapping("/reconcile")
    public Map<String, Object> reconcile() {
        Map<String, Object> result = baseResult("reconcile");
        result.put("moneyTable", psql("banking_money", "select count(*), coalesce(sum(money),0), coalesce(sum(blocked_money),0) from money;"));
        result.put("transactionExpectedBalance", psql("banking_transactions", "select coalesce(sum(case when status='COMPLETED' and transaction_type='DEPOSIT' then money when status='COMPLETED' and transaction_type='WITHDRAW' then -money else 0 end),0) from transactions;"));
        result.put("mongoProjectionCount", execInApp("mongodb", List.of("mongosh", "banking_money_query", "--quiet", "--eval", "db.money_accounts.countDocuments()"), 20));
        result.put("elasticIndexCount", httpInsideCluster("http://elasticsearch:9200/money-accounts/_count"));
        audit("Reconciliation run", Map.of());
        return result;
    }

    @GetMapping("/saga")
    public Map<String, Object> saga() {
        Map<String, Object> result = baseResult("saga");
        result.put("all", httpInsideCluster("http://transaction-service:8083/api/transaction-service/v1/saga/all"));
        result.put("stuckTransactions", httpInsideCluster("http://transaction-service:8083/api/transaction-service/v1/admin/stuck?olderThanMinutes=0"));
        audit("Saga monitor opened", Map.of());
        return result;
    }

    @PostMapping("/kubernetes/restart")
    public Map<String, Object> restart(@RequestBody Map<String, Object> body) {
        String deployment = stringValue(body.get("deployment"));
        if ("all".equalsIgnoreCase(deployment)) {
            List<CommandResult> outputs = new ArrayList<>();
            for (String app : APP_DEPLOYMENTS) {
                outputs.add(run(List.of("kubectl", "-n", NAMESPACE, "rollout", "restart", "deployment/" + app), 30));
            }
            audit("All app deployments restarted", Map.of("count", outputs.size()));
            return Map.of("status", "started", "outputs", outputs);
        }
        requireDeployment(deployment);
        CommandResult out = run(List.of("kubectl", "-n", NAMESPACE, "rollout", "restart", "deployment/" + deployment), 30);
        audit("Deployment restarted", Map.of("deployment", deployment, "exit", out.exitCode()));
        return Map.of("status", out.exitCode() == 0 ? "ok" : "failed", "output", out);
    }

    @PostMapping("/kubernetes/scale")
    public Map<String, Object> scale(@RequestBody Map<String, Object> body) {
        String deployment = stringValue(body.get("deployment"));
        requireDeployment(deployment);
        int replicas = intValue(body.get("replicas"), 1);
        CommandResult out = run(List.of("kubectl", "-n", NAMESPACE, "scale", "deployment/" + deployment, "--replicas=" + replicas), 30);
        audit("Deployment scaled", Map.of("deployment", deployment, "replicas", replicas, "exit", out.exitCode()));
        return Map.of("status", out.exitCode() == 0 ? "ok" : "failed", "output", out);
    }

    @GetMapping("/kubernetes/history")
    public Map<String, Object> rolloutHistory(@RequestParam String deployment) {
        requireDeployment(deployment);
        return Map.of("deployment", deployment, "history", run(List.of("kubectl", "-n", NAMESPACE, "rollout", "history", "deployment/" + deployment), 30));
    }

    @PostMapping("/run")
    public Map<String, Object> runOperation(@RequestBody Map<String, Object> body) throws IOException {
        String action = stringValue(body.get("action"));
        Map<String, Object> result = baseResult("run-" + action);
        switch (action) {
            case "restart-project" -> result.put("result", restart(Map.of("deployment", "all")));
            case "k8s-status" -> result.put("result", overview());
            case "test-everything" -> result.put("result", runAdminSmokeTest());
            case "host-test-bat" -> result.put("result", tryRunHostBatch(".scriptsandhelpers\\test-everything.bat", "dev"));
            case "host-full-run" -> result.put("result", tryRunHostBatch("springbankcalistirma.bat", "1"));
            case "host-build" -> result.put("result", tryRunHostBatch("springbankcalistirma.bat", "2"));
            default -> throw new IllegalArgumentException("Bilinmeyen operasyon: " + action);
        }
        audit("Admin run operation", Map.of("action", action));
        return result;
    }

    @PostMapping("/backup/export")
    public Map<String, Object> exportBackup(@RequestBody Map<String, Object> body) throws IOException {
        String db = stringValue(body.getOrDefault("database", "banking_transactions"));
        if (!POSTGRES_DATABASES.contains(db)) {
            throw new IllegalArgumentException("Gecersiz database: " + db);
        }
        Path dir = backupRoot();
        Files.createDirectories(dir);
        String name = db + "-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()) + ".sql";
        CommandResult dump = execInApp("postgres", List.of("pg_dump", "-U", "banking_admin", "-d", db), 120);
        Path target = dir.resolve(name).normalize();
        Files.writeString(target, dump.output(), StandardCharsets.UTF_8);
        audit("Database backup exported", Map.of("database", db, "file", name, "exit", dump.exitCode()));
        return Map.of("database", db, "file", name, "bytes", Files.size(target), "command", dump);
    }

    @GetMapping("/backup/list")
    public List<Map<String, Object>> listBackups() throws IOException {
        Path root = backupRoot();
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.list(root)) {
            return paths.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(this::lastModified).reversed())
                    .map(path -> Map.<String, Object>of(
                            "name", path.getFileName().toString(),
                            "bytes", size(path),
                            "modifiedAt", Instant.ofEpochMilli(lastModified(path)).toString()))
                    .toList();
        }
    }

    @GetMapping(value = "/backup/{name}", produces = MediaType.TEXT_PLAIN_VALUE)
    public String readBackup(@PathVariable String name, @RequestParam(defaultValue = "400") int lines) throws IOException {
        if (!name.matches("[a-zA-Z0-9_.-]+\\.sql")) {
            return "[HATA] Gecersiz dosya adi";
        }
        Path root = backupRoot();
        Path file = root.resolve(name).normalize();
        if (!file.startsWith(root) || !Files.exists(file)) {
            return "[HATA] Backup bulunamadi";
        }
        List<String> all = Files.readAllLines(file, StandardCharsets.UTF_8);
        int from = Math.max(0, all.size() - Math.max(1, Math.min(lines, 5000)));
        return String.join(System.lineSeparator(), all.subList(from, all.size()));
    }

    @PostMapping("/audit")
    public Map<String, Object> appendAudit(@RequestBody Map<String, Object> body) {
        audit(stringValue(body.getOrDefault("action", "ui-event")), body.getOrDefault("detail", Map.of()));
        return Map.of("status", "ok");
    }

    @GetMapping("/audit")
    public List<String> readAudit(@RequestParam(defaultValue = "200") int lines) throws IOException {
        Path path = auditPath();
        if (!Files.exists(path)) {
            return List.of();
        }
        List<String> all = Files.readAllLines(path, StandardCharsets.UTF_8);
        int from = Math.max(0, all.size() - Math.max(1, Math.min(lines, 2000)));
        return all.subList(from, all.size());
    }

    @GetMapping("/query/catalog")
    public Map<String, Object> queryCatalog() {
        Map<String, Object> result = baseResult("query-catalog");
        result.put("databases", queryDatabases());
        result.put("templates", queryTemplates());
        result.put("kafkaTopics", kafkaTopicNames());
        result.put("transports", List.of("grpc", "kafka"));
        audit("Query catalog opened", Map.of("targets", queryDatabases().size()));
        return result;
    }

    @GetMapping("/query/kafka/topics")
    public Map<String, Object> queryKafkaTopics() {
        return Map.of(
                "type", "query-kafka-topics",
                "topics", kafkaTopicNames(),
                "consumerGroups", execInApp("kafka", List.of("sh", "-lc", "kafka-consumer-groups --bootstrap-server kafka:9092 --list || true"), 30)
        );
    }

    @GetMapping("/query/kafka/topic")
    public Map<String, Object> queryKafkaTopic(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String topic,
            @RequestParam(defaultValue = "20") int maxMessages,
            @RequestParam(defaultValue = "true") boolean fromBeginning,
            @RequestParam(defaultValue = "grpc") String transport) {
        String requestId = historyDispatchService.newRequestId();
        Map<String, String> adminUser = adminUserContextExtractor.extract(authorization);
        Map<String, Object> requestPayload = Map.of("topic", topic, "maxMessages", maxMessages, "fromBeginning", fromBeginning);
        if ("kafka".equalsIgnoreCase(transport)) {
            historyDispatchService.recordKafkaPending(requestId, adminUser, "KAFKA_TOPIC_BROWSE", "kafka", topic, topic, topic, requestPayload);
            historyDispatchService.runAsyncCompletion(() -> {
                try {
                    Map<String, Object> response = executeKafkaTopicQuery(topic, maxMessages, fromBeginning);
                    historyDispatchService.recordKafkaCompletion(requestId, adminUser, "KAFKA_TOPIC_BROWSE", "kafka", topic, topic, topic, requestPayload, response, "json", "COMPLETED", "");
                } catch (Exception exception) {
                    historyDispatchService.recordKafkaCompletion(requestId, adminUser, "KAFKA_TOPIC_BROWSE", "kafka", topic, topic, topic, requestPayload, Map.of(), "error", "FAILED", exception.getMessage());
                }
            });
            return Map.of("requestId", requestId, "transport", "kafka", "status", "accepted", "message", "Kafka uzerinden asenkron isleme alindi.");
        }
        Map<String, Object> result = executeKafkaTopicQuery(topic, maxMessages, fromBeginning);
        result.put("requestId", requestId);
        result.put("transport", "grpc");
        historyDispatchService.recordSync(requestId, adminUser, "KAFKA_TOPIC_BROWSE", "kafka", topic, topic, topic, requestPayload, result, "json", "COMPLETED", "");
        return result;
    }

    @PostMapping("/query/database")
    public Map<String, Object> queryDatabase(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        String engine = stringValue(body.get("engine")).toLowerCase(Locale.ROOT);
        String database = stringValue(body.get("database"));
        String transport = stringValue(body.getOrDefault("transport", "grpc"));
        String query = String.valueOf(body.getOrDefault("query", ""));
        String requestId = historyDispatchService.newRequestId();
        Map<String, String> adminUser = adminUserContextExtractor.extract(authorization);
        if ("kafka".equalsIgnoreCase(transport)) {
            historyDispatchService.recordKafkaPending(requestId, adminUser, "DATABASE_QUERY", engine, database, "", query, body);
            historyDispatchService.runAsyncCompletion(() -> {
                try {
                    Map<String, Object> response = executeDatabaseQuery(engine, database, body);
                    historyDispatchService.recordKafkaCompletion(requestId, adminUser, "DATABASE_QUERY", engine, database, "", query, body, response, "json", "COMPLETED", "");
                } catch (Exception exception) {
                    historyDispatchService.recordKafkaCompletion(requestId, adminUser, "DATABASE_QUERY", engine, database, "", query, body, Map.of(), "error", "FAILED", exception.getMessage());
                }
            });
            return Map.of("requestId", requestId, "transport", "kafka", "status", "accepted", "message", "Sorgu Kafka transport ile kuyruga alindi.");
        }

        Map<String, Object> result = executeDatabaseQuery(engine, database, body);
        result.put("requestId", requestId);
        result.put("transport", "grpc");
        historyDispatchService.recordSync(requestId, adminUser, "DATABASE_QUERY", engine, database, "", query, body, result, "json", "COMPLETED", "");
        return result;
    }

    @GetMapping("/history")
    public List<Map<String, Object>> history(
            @RequestParam(defaultValue = "40") int limit,
            @RequestParam(defaultValue = "") String keyword) {
        return historyQueryGrpcClient.list(limit, keyword);
    }

    @GetMapping("/history/{requestId}")
    public Map<String, Object> historyByRequestId(@PathVariable String requestId) {
        return historyQueryGrpcClient.get(requestId);
    }

    private Map<String, Object> executeKafkaTopicQuery(String topic, int maxMessages, boolean fromBeginning) {
        String safeTopic = stringValue(topic);
        if (safeTopic.isBlank() || safeTopic.contains(" ")) {
            throw new IllegalArgumentException("Gecersiz topic");
        }
        int cappedMessages = Math.max(1, Math.min(maxMessages, 100));
        Map<String, Object> result = baseResult("query-kafka-topic");
        result.put("topic", safeTopic);
        result.put("describe", execInApp("kafka", List.of("sh", "-lc", "kafka-topics --bootstrap-server kafka:9092 --describe --topic " + shellToken(safeTopic) + " || true"), 30));
        String consumeScript = "kafka-console-consumer --bootstrap-server kafka:9092 --topic " + shellToken(safeTopic)
                + (fromBeginning ? " --from-beginning" : "")
                + " --max-messages " + cappedMessages
                + " --timeout-ms 5000"
                + " --property print.partition=true"
                + " --property print.offset=true"
                + " --property print.timestamp=true"
                + " --property print.key=true"
                + " --property key.separator=@@KEY@@";
        CommandResult consume = execInApp("kafka", List.of("sh", "-lc", consumeScript), 45);
        result.put("messages", parseKafkaMessages(consume.output()));
        result.put("raw", consume);
        audit("Kafka topic queried", Map.of("topic", safeTopic, "messages", cappedMessages));
        return result;
    }

    private Map<String, Object> executeDatabaseQuery(String engine, String database, Map<String, Object> body) {
        return switch (engine) {
            case "postgres" -> queryPostgres(database, String.valueOf(body.getOrDefault("query", "")));
            case "mongo" -> queryMongo(database, String.valueOf(body.getOrDefault("query", "")));
            case "elasticsearch" -> queryElasticsearch(database, body);
            default -> throw new IllegalArgumentException("Desteklenmeyen engine: " + engine);
        };
    }

    private Map<String, Object> runAdminSmokeTest() throws IOException {
        Path root = testLogRoot();
        Files.createDirectories(root);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("testScript", tryRunHostBatch(".scriptsandhelpers\\test-everything.bat", "dev"));
        result.put("availableRuns", Files.isDirectory(root)
                ? Files.list(root).filter(Files::isDirectory).map(path -> path.getFileName().toString()).sorted().toList()
                : List.of());
        return result;
    }

    private Map<String, Object> tryRunHostBatch(String script, String argument) {
        Path repo = repoRoot();
        Path target = repo.resolve(script).normalize();
        if (!target.startsWith(repo) || !Files.exists(target)) {
            return Map.of("status", "unavailable", "message", "Script bulunamadi: " + target);
        }
        return Map.of("status", "unavailable", "message", "Container icinden host batch dogrudan calistirilamiyor.", "script", target.toString(), "argument", argument);
    }

    private CommandResult httpInsideCluster(String url) {
        return runAllowFail(List.of("sh", "-lc", "wget -qO- " + shellToken(url) + " || true"), 20);
    }

    private CommandResult psql(String db, String sql) {
        return execInApp("postgres", List.of("psql", "-U", "banking_admin", "-d", db, "-c", sql), 30);
    }

    private Map<String, Object> queryPostgres(String database, String query) {
        if (!POSTGRES_DATABASES.contains(database)) {
            throw new IllegalArgumentException("Gecersiz Postgres database: " + database);
        }
        String trimmedQuery = query == null ? "" : query.trim();
        if (trimmedQuery.isBlank()) {
            throw new IllegalArgumentException("Postgres sorgusu bos olamaz");
        }
        CommandResult commandResult = looksLikeTableQuery(trimmedQuery)
                ? execInApp("postgres", List.of("psql", "-U", "banking_admin", "-d", database, "--csv", "-c", trimmedQuery), 90)
                : execInApp("postgres", List.of("psql", "-U", "banking_admin", "-d", database, "-c", trimmedQuery), 90);
        Map<String, Object> result = baseResult("query-postgres");
        result.put("engine", "postgres");
        result.put("database", database);
        result.put("query", trimmedQuery);
        result.put("raw", commandResult);
        if (looksLikeTableQuery(trimmedQuery) && commandResult.exitCode() == 0) {
            result.putAll(tablePayloadFromCsv(commandResult.output()));
        } else {
            result.put("pretty", commandResult.output());
        }
        return result;
    }

    private Map<String, Object> queryMongo(String database, String query) {
        if (!MONGO_DATABASES.contains(database)) {
            throw new IllegalArgumentException("Gecersiz Mongo database: " + database);
        }
        String trimmedQuery = query == null ? "" : query.trim();
        if (trimmedQuery.isBlank()) {
            throw new IllegalArgumentException("Mongo sorgusu bos olamaz");
        }
        String eval = buildMongoEval(trimmedQuery);
        CommandResult commandResult = execInApp("mongodb", List.of("mongosh", database, "--quiet", "--eval", eval), 90);
        Map<String, Object> result = baseResult("query-mongo");
        result.put("engine", "mongo");
        result.put("database", database);
        result.put("query", trimmedQuery);
        result.put("raw", commandResult);
        result.putAll(jsonPayload(commandResult.output()));
        return result;
    }

    private Map<String, Object> queryElasticsearch(String database, Map<String, Object> body) {
        String path = stringValue(body.getOrDefault("path", ""));
        String method = stringValue(body.getOrDefault("method", "GET")).toUpperCase(Locale.ROOT);
        String requestBody = String.valueOf(body.getOrDefault("requestBody", ""));
        if (database.isBlank()) {
            throw new IllegalArgumentException("Elasticsearch hedefi bos olamaz");
        }
        if (path.isBlank()) {
            path = "_cluster".equals(database) ? "/_cluster/health" : "/" + database + "/_search";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        String curl = "wget -qO- --method=" + shellToken(method).replace("'", "") + " --header='Content-Type: application/json' "
                + (requestBody.isBlank() ? "" : ("--body-data=" + shellToken(requestBody) + " "))
                + shellToken("http://elasticsearch:9200" + path) + " || true";
        CommandResult commandResult = runAllowFail(List.of("sh", "-lc", curl), 90);
        Map<String, Object> result = baseResult("query-elasticsearch");
        result.put("engine", "elasticsearch");
        result.put("database", database);
        result.put("path", path);
        result.put("method", method);
        result.put("raw", commandResult);
        result.putAll(jsonPayload(commandResult.output()));
        return result;
    }

    private List<Map<String, Object>> queryDatabases() {
        List<Map<String, Object>> targets = new ArrayList<>();
        POSTGRES_DATABASES.stream().sorted().forEach(db -> targets.add(Map.of("engine", "postgres", "database", db, "label", "Postgres / " + db, "language", "sql")));
        MONGO_DATABASES.stream().sorted().forEach(db -> targets.add(Map.of("engine", "mongo", "database", db, "label", "MongoDB / " + db, "language", "mongodb-js")));
        for (String target : List.of("money-accounts", "admin-query-history", "_cluster", "_cat")) {
            targets.add(Map.of("engine", "elasticsearch", "database", target, "label", "Elasticsearch / " + target, "language", "json"));
        }
        return targets;
    }

    private List<Map<String, Object>> queryTemplates() {
        List<Map<String, Object>> templates = new ArrayList<>();
        templates.add(template("postgres", "banking_users", "Son 50 kullanici", "select id, mail, role, active, created_at from users order by created_at desc nulls last limit 50;"));
        templates.add(template("postgres", "banking_transactions", "Son 100 islem", "select id, sender_iban, receiver_iban, transaction_type, money, status, created_at from transactions order by created_at desc limit 100;"));
        templates.add(template("postgres", "banking_admin_command", "Son admin sorgulari", "select request_id, admin_email, transport, request_type, target_name, status, requested_at from admin_query_history order by requested_at desc limit 50;"));
        templates.add(template("mongo", "banking_money_query", "Ilk 20 projection kaydi", "db.money_accounts.find().limit(20).toArray()"));
        templates.add(template("mongo", "banking_admin_query", "Son admin history kayitlari", "db.admin_query_history.find().sort({ requestedAt: -1 }).limit(20).toArray()"));
        templates.add(template("elasticsearch", "money-accounts", "Ilk 20 dokuman", "{\"size\":20,\"query\":{\"match_all\":{}}}"));
        templates.add(template("elasticsearch", "admin-query-history", "Admin history arama", "{\"size\":20,\"sort\":[{\"requestedAt\":{\"order\":\"desc\"}}],\"query\":{\"match_all\":{}}}"));
        templates.add(template("elasticsearch", "_cluster", "Cluster health", ""));
        templates.add(template("elasticsearch", "_cat", "Index listesi", ""));
        return templates;
    }

    private Map<String, Object> template(String engine, String database, String label, String query) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("engine", engine);
        item.put("database", database);
        item.put("label", label);
        item.put("query", query);
        return item;
    }

    private List<String> kafkaTopicNames() {
        return nonEmptyLines(execInApp("kafka", List.of("sh", "-lc", "kafka-topics --bootstrap-server kafka:9092 --list | sort"), 30).output());
    }

    private CommandResult execInApp(String app, List<String> innerCommand, int timeoutSeconds) {
        String podName = resolvePodName(app);
        if (podName.isBlank()) {
            return new CommandResult(List.of("kubectl", "-n", NAMESPACE, "get", "pods", "-l", "app=" + app), 997, false, "Pod bulunamadi: " + app, 0);
        }
        List<String> command = new ArrayList<>(List.of("kubectl", "-n", NAMESPACE, "exec", podName, "--"));
        command.addAll(innerCommand);
        return runAllowFail(command, timeoutSeconds);
    }

    private String resolvePodName(String app) {
        return stringValue(runAllowFail(List.of("kubectl", "-n", NAMESPACE, "get", "pods", "-l", "app=" + app, "-o", "jsonpath={.items[0].metadata.name}"), 20).output());
    }

    private Map<String, Object> tablePayloadFromCsv(String csvText) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<List<String>> parsed = parseCsv(csvText);
        if (parsed.isEmpty()) {
            result.put("columns", List.of());
            result.put("rows", List.of());
            result.put("pretty", csvText);
            return result;
        }
        List<String> columns = parsed.getFirst();
        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = 1; i < parsed.size(); i++) {
            List<String> values = parsed.get(i);
            Map<String, String> row = new LinkedHashMap<>();
            for (int j = 0; j < columns.size(); j++) {
                row.put(columns.get(j), j < values.size() ? values.get(j) : "");
            }
            rows.add(row);
        }
        result.put("columns", columns);
        result.put("rows", rows);
        result.put("pretty", csvText);
        return result;
    }

    private Map<String, Object> jsonPayload(String raw) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pretty", raw == null ? "" : raw.trim());
        if (raw == null || raw.isBlank()) {
            return result;
        }
        try {
            JsonNode jsonNode = objectMapper.readTree(raw);
            result.put("json", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode));
            if (jsonNode.isArray()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                Set<String> columns = new LinkedHashSet<>();
                jsonNode.forEach(item -> {
                    if (item.isObject()) {
                        item.fieldNames().forEachRemaining(columns::add);
                    }
                });
                if (!columns.isEmpty()) {
                    jsonNode.forEach(item -> {
                        if (item.isObject()) {
                            Map<String, Object> row = new LinkedHashMap<>();
                            columns.forEach(column -> row.put(column, jsonValue(item.get(column))));
                            rows.add(row);
                        }
                    });
                    result.put("columns", new ArrayList<>(columns));
                    result.put("rows", rows);
                }
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    private Object jsonValue(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isNumber()) return node.numberValue();
        if (node.isBoolean()) return node.booleanValue();
        if (node.isTextual()) return node.textValue();
        return node.toString();
    }

    private List<List<String>> parseCsv(String csvText) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < csvText.length(); i++) {
            char current = csvText.charAt(i);
            if (inQuotes) {
                if (current == '"') {
                    if (i + 1 < csvText.length() && csvText.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cell.append(current);
                }
                continue;
            }
            if (current == '"') inQuotes = true;
            else if (current == ',') {
                row.add(cell.toString());
                cell.setLength(0);
            } else if (current == '\n') {
                row.add(cell.toString());
                cell.setLength(0);
                if (!row.stream().allMatch(String::isBlank)) rows.add(new ArrayList<>(row));
                row.clear();
            } else if (current != '\r') {
                cell.append(current);
            }
        }
        if (cell.length() > 0 || !row.isEmpty()) {
            row.add(cell.toString());
            if (!row.stream().allMatch(String::isBlank)) rows.add(new ArrayList<>(row));
        }
        return rows;
    }

    private List<Map<String, Object>> parseKafkaMessages(String rawOutput) {
        List<Map<String, Object>> messages = new ArrayList<>();
        for (String line : nonEmptyLines(rawOutput)) {
            String[] parts = line.split("\\t");
            Map<String, Object> item = new LinkedHashMap<>();
            String payload = parts.length == 0 ? line : parts[parts.length - 1];
            for (String part : parts) {
                if (part.startsWith("Partition:")) item.put("partition", part.substring("Partition:".length()));
                else if (part.startsWith("Offset:")) item.put("offset", part.substring("Offset:".length()));
                else if (part.startsWith("CreateTime:")) item.put("timestamp", part.substring("CreateTime:".length()));
                else if (part.startsWith("LogAppendTime:")) item.put("timestamp", part.substring("LogAppendTime:".length()));
            }
            String[] keyAndValue = payload.split("@@KEY@@", 2);
            item.put("key", keyAndValue.length > 1 ? keyAndValue[0] : "");
            item.put("value", keyAndValue.length > 1 ? keyAndValue[1] : payload);
            try {
                item.put("json", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree(String.valueOf(item.get("value")))));
            } catch (Exception ignored) {
            }
            messages.add(item);
        }
        return messages;
    }

    private List<String> nonEmptyLines(String output) {
        if (output == null || output.isBlank()) return List.of();
        return Arrays.stream(output.split("\\R")).map(String::trim).filter(line -> !line.isBlank()).toList();
    }

    private boolean looksLikeTableQuery(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("select") || normalized.startsWith("with") || normalized.startsWith("show") || normalized.startsWith("values") || normalized.startsWith("explain");
    }

    private String buildMongoEval(String query) {
        return query.contains("return ")
                ? "const __sb = (function(){ " + query + " })(); print(EJSON.stringify(__sb));"
                : "const __sb = " + query + "; print(EJSON.stringify(__sb));";
    }

    private String shellToken(String value) {
        return "'" + String.valueOf(value).replace("'", "'\"'\"'") + "'";
    }

    private CommandResult runAllowFail(List<String> command, int timeoutSeconds) {
        try {
            return run(command, timeoutSeconds);
        } catch (Exception exception) {
            return new CommandResult(command, 999, false, exception.getMessage(), 0);
        }
    }

    private CommandResult run(List<String> command, int timeoutSeconds) {
        long start = System.nanoTime();
        Process process = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();
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
                return new CommandResult(command, 124, true, output.toString(), elapsedMillis(start));
            }
            reader.join(1000);
            return new CommandResult(command, process.exitValue(), false, output.toString(), elapsedMillis(start));
        } catch (Exception exception) {
            if (process != null) process.destroyForcibly();
            return new CommandResult(command, 998, false, exception.getMessage(), elapsedMillis(start));
        }
    }

    private long elapsedMillis(long start) {
        return Duration.ofNanos(System.nanoTime() - start).toMillis();
    }

    private void requireDeployment(String deployment) {
        if (deployment == null || !DEPLOYMENTS.contains(deployment)) {
            throw new IllegalArgumentException("Gecersiz deployment: " + deployment);
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private int intValue(Object value, int fallback) {
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private Map<String, Object> baseResult(String type) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", type);
        result.put("namespace", NAMESPACE);
        result.put("generatedAt", Instant.now().toString());
        return result;
    }

    private Path testLogRoot() {
        return absolutePath(System.getenv().getOrDefault("TEST_LOG_ROOT", ".scriptsandhelpers/logs"));
    }

    private Path backupRoot() {
        return absolutePath(System.getenv().getOrDefault("ADMIN_BACKUP_ROOT", "/tmp/springbank-admin-backups"));
    }

    private Path auditPath() {
        return absolutePath(System.getenv().getOrDefault("ADMIN_AUDIT_LOG", "/tmp/springbank-admin-audit.jsonl"));
    }

    private Path repoRoot() {
        return absolutePath(System.getenv().getOrDefault("SPRINGBANK_REPO_ROOT", "."));
    }

    private Path absolutePath(String value) {
        Path path = Paths.get(value);
        if (!path.isAbsolute()) {
            path = Paths.get("").toAbsolutePath().resolve(path);
        }
        return path.normalize();
    }

    private void audit(String action, Object detail) {
        try {
            Path path = auditPath();
            Files.createDirectories(path.getParent());
            String line = "{\"at\":\"" + Instant.now() + "\",\"action\":\"" + jsonEscape(action) + "\",\"detail\":\"" + jsonEscape(String.valueOf(detail)) + "\"}";
            Files.writeString(path, line + System.lineSeparator(), StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception exception) {
            log.warn("Admin audit yazilamadi: {}", exception.getMessage());
        }
    }

    private String jsonEscape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ignored) {
            return 0L;
        }
    }

    private long size(Path path) {
        try {
            return Files.size(path);
        } catch (IOException ignored) {
            return 0L;
        }
    }

    public record CommandResult(List<String> command, int exitCode, boolean timedOut, String output, long durationMs) {
    }
}
