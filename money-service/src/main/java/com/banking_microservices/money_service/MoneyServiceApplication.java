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
		return new com.google.gson.GsonBuilder().serializeNulls().create();
	}
}
