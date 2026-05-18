package com.banking_microservices.admin_service_query.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MongoConfig {

    @Bean
    @Primary
    public MongoClient mongoClient(
            @Value("${SPRING_DATA_MONGODB_URI:mongodb://mongodb:27017/banking_admin_query}") String mongoUri) {
        return MongoClients.create(mongoUri);
    }
}
