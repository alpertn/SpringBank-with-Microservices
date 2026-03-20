package com.banking_microservices.auth_service.kafka;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.security.web.SecurityFilterChain;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Icin Producer Config. Bu olmazsa kafkaya veriler gonderilemez. kafkaya veirler raw olarak gonderilemedigi icin
 * serializer yazmak zorundayiz. Bu defaulttur degisebilir.
 * mesela ben verileri gson ile donusturdugum gibin gsonserializer ekledim.
 * Javanin kendi kutuphanesini kullanacak olsayudiniz oraya da onu yazmaniz gerekti.
 *
 * Kafka icin {@link ProducerFactory} ve {@link KafkaTemplate} Beanlarını tanımlar.
 *
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, GsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "none");

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Gson Serializerin Amaci Kafka verileri alirken ve gonderirken Gson kullanirsak gson localdatatime verisini ceviremez. oyuzden Confige ekliyoruz.
     */
    public static class GsonSerializer implements Serializer<Object> {
        private final Gson gson = new GsonBuilder()
                .serializeNulls()
                .registerTypeAdapter(java.time.LocalDateTime.class,
                        (com.google.gson.JsonSerializer<java.time.LocalDateTime>) (src, type, ctx) ->
                                new com.google.gson.JsonPrimitive(src.toString()))
                .registerTypeAdapter(java.time.LocalDateTime.class,
                        (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, type, ctx) ->
                                java.time.LocalDateTime.parse(json.getAsString()))
                .create();

        @Override
        public byte[] serialize(String topic, Object data) {
            if (data == null) {
                return null;
            }
            return gson.toJson(data).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
