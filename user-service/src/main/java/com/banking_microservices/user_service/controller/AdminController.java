package com.banking_microservices.user_service.controller;

import com.banking_microservices.user_service.dto.RoleEnum.RoleEnum;
import com.banking_microservices.user_service.dto.admin.AdminPasswordResetDto;
import com.banking_microservices.user_service.models.Users;
import com.banking_microservices.user_service.service.UserService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/user-service/v1/admin")
public class AdminController {

    private final Gson gson = new GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonSerializer<java.time.LocalDateTime>) (src, type, ctx) ->
                            new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, type, ctx) ->
                            java.time.LocalDateTime.parse(json.getAsString()))
            .create();
    private final UserService userService;
    private final java.util.function.Supplier<String> currentTime;

    public AdminController(UserService userService, java.util.function.Supplier<String> currentTime) {
        this.userService = userService;
        this.currentTime = currentTime;
    }

    @GetMapping("/finduserbyid/{id}")
    public ResponseEntity<Users> findUserById(@PathVariable String id) {
        log.info(" ({}) > AdminController | findUserById -> Istek alindi. Id : {}", currentTime.get(), id);
        return ResponseEntity.ok(userService.findUserById(id));
    }

    @GetMapping("/findbykeycloakuuid/{uuid}")
    public ResponseEntity<Users> findByKeycloakUUID(@PathVariable String uuid) {
        log.info(" ({}) > AdminController | findByKeycloakUUID -> Istek alindi. UUID : {}", currentTime.get(), uuid);
        return ResponseEntity.ok(userService.findByKeycloakUUID(uuid));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Users>> searchUsers(@RequestParam String query) {
        log.info(" ({}) > AdminController | searchUsers -> Istek alindi. Query : {}", currentTime.get(), query);
        return ResponseEntity.ok(userService.searchUsersByName(query));
    }

    @PatchMapping("/updaterole/{id}")
    public ResponseEntity<?> changeRole(@PathVariable String id, @RequestParam String role) {
        log.info(" ({}) > AdminController | changeRole -> Istek alindi. Id: {}, Role: {}", currentTime.get(), id, role);
        userService.updateUserRole(id, RoleEnum.Role.valueOf(role.toUpperCase()));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats/total")
    public ResponseEntity<Long> getTotalUsers() {
        log.info(" ({}) > AdminController | getTotalUsers -> Istek alindi.", currentTime.get());
        return ResponseEntity.ok(userService.getTotalUserCount());
    }

    @GetMapping("/allusers")
    public ResponseEntity<List<Users>> getAllUsers() {
        log.info(" ({}) > AdminController | getAllUsers -> Istek alindi.", currentTime.get());
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/stats/roles")
    public ResponseEntity<java.util.Map<String, Long>> getRoleStats() {
        log.info(" ({}) > AdminController | getRoleStats -> Istek alindi.", currentTime.get());
        java.util.Map<String, Long> stats = new java.util.HashMap<>();
        stats.put("USER", userService.countByRole(RoleEnum.Role.USER));
        stats.put("ADMIN", userService.countByRole(RoleEnum.Role.ADMIN));
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/stats/active")
    public ResponseEntity<java.util.Map<String, Long>> getActiveStats() {
        log.info(" ({}) > AdminController | getActiveStats -> Istek alindi.", currentTime.get());
        java.util.Map<String, Long> stats = new java.util.HashMap<>();
        stats.put("active", userService.countByActive(true));
        stats.put("inactive", userService.countByActive(false));
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/findbyemail")
    public ResponseEntity<List<Users>> searchByEmail(@RequestParam String email) {
        log.info(" ({}) > AdminController | searchByEmail -> Istek alindi. Email: {}", currentTime.get(), email);
        return ResponseEntity.ok(userService.searchUsersByEmail(email));
    }

    @PostMapping("/users/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable String id,
                                           @Valid @RequestBody AdminPasswordResetDto passwordDto) {
        log.warn(" ({}) > AdminController | resetPassword -> Istek alindi. ID: {}, Dto: {}", currentTime.get(), id, gson.toJson(passwordDto));
        userService.resetUserPassword(id, passwordDto.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{id}/activate")
    public ResponseEntity<?> activateUser(@PathVariable String id) {
        log.info(" ({}) > AdminController | activateUser -> Istek alindi. ID: {}", currentTime.get(), id);
        userService.updateUserStatus(id, true);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{id}/deactivate")
    public ResponseEntity<?> deactivateUser(@PathVariable String id) {
        log.info(" ({}) > AdminController | deactivateUser -> Istek alindi. ID: {}", currentTime.get(), id);
        userService.updateUserStatus(id, false);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/updateuser")
    public ResponseEntity<?> updateUserWithId(@RequestBody Users user) {
        log.info(" ({}) > AdminController | updateUserWithId -> Istek alindi. UserDto: {}", currentTime.get(), gson.toJson(user));
        userService.updateUser(user);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/deleteuser/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        log.info(" ({}) > AdminController | deleteUser -> Istek alindi. ID: {}", currentTime.get(), id);
        userService.deleteUserById(id);
        return ResponseEntity.ok().build();
    }
}