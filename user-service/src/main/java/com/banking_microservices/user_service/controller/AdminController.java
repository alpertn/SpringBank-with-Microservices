package com.banking_microservices.user_service.controller;

import com.banking_microservices.user_service.dto.RoleEnum.RoleEnum;
import com.banking_microservices.user_service.dto.admin.AdminPasswordResetDto;
import com.banking_microservices.user_service.models.Users;
import com.banking_microservices.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/user-service/v1/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    // DUZELTME: @PostMapping ve body'den String almak yanlis - ID icin GET + @PathVariable kullanilmali.
    @GetMapping("/finduserbyid/{id}")
    public ResponseEntity<Users> findUserById(@PathVariable String id) {
        log.info("AdminController findUserById Modulu Istegi aldi. id : {}", id);
        return ResponseEntity.ok(userService.findUserById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Users>> searchUsers(@RequestParam String query) {
        return ResponseEntity.ok(userService.searchUsersByName(query));
    }

    @PatchMapping("/updaterole/{id}")
    public ResponseEntity<?> changeRole(@PathVariable String id, @RequestParam String role) {
        userService.updateUserRole(id, RoleEnum.Role.valueOf(role.toUpperCase()));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats/total")
    public ResponseEntity<Long> getTotalUsers() {
        return ResponseEntity.ok(userService.getTotalUserCount());
    }

    @GetMapping("/findbyemail")
    public ResponseEntity<List<Users>> searchByEmail(@RequestParam String email) {
        log.info("Admin search by email request. Email: {}", email);
        return ResponseEntity.ok(userService.searchUsersByEmail(email));
    }

    // DUZELTME: resetPassword ve activate/deactivate endpointleri yoruma alinmisti.
    // UserService'de tam implemente var. Aktif edildi.
    @PostMapping("/users/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable String id,
                                           @Valid @RequestBody AdminPasswordResetDto passwordDto) {
        log.warn("Admin password reset request for user ID: {}", id);
        userService.resetUserPassword(id, passwordDto.getNewPassword());
        return ResponseEntity.ok().build();
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

    // DUZELTME: deleteUser ve updateUser endpointleri yoruma alinmisti ama UserService'de implemente var.
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
}