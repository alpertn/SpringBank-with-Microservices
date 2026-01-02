package com.springbank.user_service;

import com.springbank.user_service.model.Users;
import com.springbank.user_service.repository.repository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@SpringBootApplication
public class UserServiceApplication implements CommandLineRunner {

    private final repository repository;

    public UserServiceApplication(repository repository) {
        this.repository = repository;
    }

    public static void main(String[] args) {
		SpringApplication.run(UserServiceApplication.class, args);
	}

    @Override // CommandLinerRunner kullanmasi icin
    public void run(String... args) throws Exception{

        Users user1 = Users.builder()
                .tckimlikNo("51121943355")
                .password("alper123A")
                .customerName("Alper")
                .customerSurname("Karakus").build();

        Users user2 = Users.builder()
                .tckimlikNo("58894823344")
                .password("mertefebaran123A")
                .customerName("Mert Efe")
                .customerSurname("Baran").build();

        Users user3 = Users.builder()
                .tckimlikNo("59940234422")
                .password("ardacky3A")
                .customerName("Arda")
                .customerSurname("Karatakli").build();

//        repository.save(user1);
//        repository.saveAll(Arrays.asList(user1, user2, user3)); // saveAll arraylist ile kullanilir

        List<Users> userList = repository.saveAll(Arrays.asList(user1, user2, user3));

        System.out.println(userList);
    }

}
