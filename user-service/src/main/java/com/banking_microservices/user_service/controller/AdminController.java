package com.banking_microservices.user_service.controller;

import com.banking_microservices.user_service.dto.admin.*;
import com.banking_microservices.user_service.models.Users;
import com.banking_microservices.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/user/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/finduserbyid/{id}")
    public ResponseEntity<Users> findUserById(@PathVariable String id) {
        log.info("Admin findUserById istegi alindi. ID: {}", id);
        return ResponseEntity.ok(userService.findUserById(id));
    }

    @PutMapping("/updateuser")
    public ResponseEntity<?> updateUserWithId(@RequestBody Users user) {
        log.info("Admin updateUser istegi alindi.");
        userService.updateUser(user);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/deleteuser/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        log.info("Admin deleteUser istegi alindi. Silinecek ID: {}", id);
        userService.deleteUserById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<Users>> searchUsers(@RequestParam String query) {
        return ResponseEntity.ok(userService.searchUsersByName(query));
    }

    @PatchMapping("/updaterole/{id}")
    public ResponseEntity<?> changeRole(@PathVariable String id, @RequestParam String role) {
        userService.updateUserRole(id, role);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats/total")
    public ResponseEntity<Long> getTotalUsers() {
        return ResponseEntity.ok(userService.getTotalUserCount());
    }

    // Rol Bazlı İstatistikler
    @GetMapping("/stats/by-role")
    public ResponseEntity<RoleStatsResponseDto> getRoleStatistics() {
        log.info("Admin role statistics request received");
        Map<String, Long> roleStats = userService.getRoleStatistics();
        Long totalUsers = userService.getTotalUserCount();

        RoleStatsResponseDto response = RoleStatsResponseDto.builder()
                .roleDistribution(roleStats)
                .totalUsers(totalUsers)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/users/{id}/activate")
    public ResponseEntity<?> activateUser(@PathVariable String id) {
        log.info("Admin activate user request. ID: {}", id);
        userService.updateUserStatus(id, true);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{id}/deactivate")
    public ResponseEntity<?> deactivateUser(@PathVariable String id) {
        log.info("Admin deactivate user request. ID: {}", id);
        userService.updateUserStatus(id, false);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search-by-email")
    public ResponseEntity<List<Users>> searchByEmail(@RequestParam String email) {
        log.info("Admin search by email request. Email: {}", email);
        return ResponseEntity.ok(userService.searchUsersByEmail(email));
    }

    @PostMapping("/users/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable String id,
            @Valid @RequestBody AdminPasswordResetDto passwordDto) {
        log.warn("Admin password reset request for user ID: {}", id);
        userService.resetUserPassword(id, passwordDto.getNewPassword());
        return ResponseEntity.ok().build();
    }

}
