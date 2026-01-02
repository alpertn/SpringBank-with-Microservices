package com.springbank.user_service;

import com.springbank.user_service.model.User;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UserServiceApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(UserServiceApplication.class, args);
	}

    @Override // CommandLinerRunner kullanmasi icin
    public void run(String... args) throws Exception{

        User user1 = new User();
        // tamamlaancak repo yapilinca

    }

}
