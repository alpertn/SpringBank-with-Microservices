package com.banking_microservices.user_service.service;


import com.banking_microservices.user_service.dto.UsersDto;
import com.banking_microservices.user_service.exception.CreateUserException;
import com.banking_microservices.user_service.exception.MailNotFoundException;
import com.banking_microservices.user_service.exception.UserAlreadyExistsException;
import com.banking_microservices.user_service.exception.UserSaveDatabaseException;
import com.banking_microservices.user_service.models.Users;
import com.banking_microservices.user_service.repository.repository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.google.gson.Gson;

import java.util.List;

@Service
@Slf4j
public class service {

    private Gson gson = new Gson();
    @Autowired
    private repository repository;

    public service(repository repository) {
        this.repository = repository;
    }

    @Transactional
    public List<Users> getAllUsers(){
        return repository.findAll();
    }

    @Transactional
    public Users findUserByMail(String mail) {
        log.info("findUserByMail sorgusu icin parametre {}", gson.toJson(mail));
        return repository.findUsersByMail(mail)
                .orElseThrow(() -> new MailNotFoundException("Mail Not Found: {}" + mail));
    }

    public UsersDto saveUser(UsersDto usersDto) {

        if (repository.existsBymail(usersDto.getMail())) {
            log.info("Mail already exists In Service.saveUser and Values =  {}", gson.toJson(usersDto));
            throw new UserAlreadyExistsException("Mail Already Exists " + usersDto.getMail());
        }

        Users newUsers = Users
                .builder()
                .mail(usersDto.getMail())
                .password(usersDto.getPassword())
                .name(usersDto.getName())
                .surname(usersDto.getSurname())
                .role("USER")
                .build();

        try {
            Users user = repository.save(newUsers);
            log.info("User Olusturuldu! {}", gson.toJson(user));
            return usersDto;

        } catch (Exception e) {
            log.warn("repository.save(newUsers); Sorgusunda Hata olustu.  {} --- {}", gson.toJson(newUsers), e.getMessage());
            throw new UserSaveDatabaseException("User veritabanina kaydedilirken bir sorun olustu " + e.getMessage() + gson.toJson(newUsers));
        }
    }


}


//@Transactional // Veritabanı işlemi - Hata olursa rollback yapar. Başarısız olursa: Veri tutarsızlığı
//public UserDTO createUser(UserDTO request) { // Yeni kullanıcı oluştur. AuthController'dan çağrılır. UserDTO döner
//    log.info("Creating user: {}", request.getEmail()); // Email loglama - Kim kaydoluyor?
//
//    if (userRepository.existsByEmail(request.getEmail())) { // Email kontrolü - Daha önce kayıtlı mı?
//        throw new UserAlreadyExistsException("Email already exists: " + request.getEmail()); // Hata fırlat - 409 Conflict döner
//    }
//
//    User newUser = User.builder() // Yeni User entity oluştur - DB'ye kaydedilecek
//            .email(request.getEmail()) // Email - Kullanıcı kimliği
//            .password(request.getPassword()) // Şifre - BCrypt hash'li gelir
//            .fullName(request.getFullName()) // Ad Soyad - Görünen isim
//            .role("USER") // Rol - Varsayılan USER
//            .active(true) // Durum - Aktif kullanıcı
//            .build();
//
//    User savedUser = userRepository.save(newUser); // DB'ye kaydet - ID otomatik atanır
//    log.info("User created with ID: {}", savedUser.getId()); // Başarı logu
//
//    // HESAP OLUŞTURMA - TOLERANSLI MOD
//    try { // Try-catch ile Money Service hatasını yakala - Kullanıcı kaydı başarısız olmasın
//        moneyServiceClient.createAccount( // Money Service'e hesap aç - HTTP çağrısı
//                new MoneyServiceClient.CreateAccountRequest(savedUser.getId(), new java.math.BigDecimal("1000"))); // 1000 TL başlangıç
//        log.info("Account created for user {} with 1000 TL", savedUser.getId()); // Başarı logu
//    } catch (Exception e) { // Money Service hatası yakala - Ağ hatası, service down, timeout vs
//        log.warn("Account creation failed for user {}. Will be created on first login. Error: {}",
//                savedUser.getId(), e.getMessage()); // Uyarı logu - Kullanıcı kaydı başarılı ama hesap oluşmadı
//        // Kullanıcı kaydını iptal etme - Hesap sonra oluşturulabilir
//    }
//
//    return convertToDTO(savedUser); // DTO dönüştür ve döndür
//}
//
//@Transactional(readOnly = true)
//public UserDTO getUserById(UUID id) {
//    User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User not found: " + id));
//    return convertToDTO(user);
//}
//
//@Transactional(readOnly = true)
//public UserDTO getUserByEmail(String email) {
//    User user = userRepository.findByEmail(email)
//            .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
//    return convertToDTO(user);
//}
//
//@Transactional(readOnly = true)
//public List<UserDTO> getAllUsers() {
//    return userRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
//}
//
//@Transactional
//public UserDTO makeAdmin(UUID userId) {
//    log.info("Making user {} an admin", userId);
//    User user = userRepository.findById(userId)
//            .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
//    user.setRole("ADMIN");
//    userRepository.save(user);
//    log.info("User {} is now an admin", userId);
//    return convertToDTO(user);
//}
//
//@Transactional
//public void deleteUser(UUID id) {
//    log.info("Deleting user with id: {}", id);
//    if (!userRepository.existsById(id)) {
//        throw new UserNotFoundException("User not found: " + id);
//    }
//    userRepository.deleteById(id);
//    log.info("User deleted successfully with id: {}", id);
//}
//
//private UserDTO convertToDTO(User user) {
//    UserDTO dto = new UserDTO();
//    dto.setId(user.getId());
//    dto.setEmail(user.getEmail());
//    dto.setPassword(user.getPassword());
//    dto.setFullName(user.getFullName());
//    dto.setRole(user.getRole());
//    dto.setActive(user.getActive());
//    dto.setCreatedAt(user.getCreatedAt());
//    return dto;
//}