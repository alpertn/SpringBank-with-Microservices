package com.banking_microservices.money_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MoneyServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MoneyServiceApplication.class, args);
	}

	@org.springframework.context.annotation.Bean
	public com.google.gson.Gson gson() {
		return new com.google.gson.GsonBuilder()
				.serializeNulls()
				.registerTypeAdapter(java.time.LocalDateTime.class,
						(com.google.gson.JsonSerializer<java.time.LocalDateTime>) (src, type, ctx) ->
								new com.google.gson.JsonPrimitive(src.toString()))
				.registerTypeAdapter(java.time.LocalDateTime.class,
						(com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, type, ctx) ->
								java.time.LocalDateTime.parse(json.getAsString()))
				.create();
	}
}
