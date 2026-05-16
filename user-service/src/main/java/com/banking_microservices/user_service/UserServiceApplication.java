package com.banking_microservices.user_service;

import com.banking_microservices.user_service.config.KeycloakBootstrapSetup;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class UserServiceApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(UserServiceApplication.class, args);
		context.getBean(KeycloakBootstrapSetup.class).setupOnce();
	}

}
