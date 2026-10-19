package LangForU_DevTeam.LangForU.appuser;

import LangForU_DevTeam.LangForU.config.AppLinks;
import LangForU_DevTeam.LangForU.courses.Course;
import LangForU_DevTeam.LangForU.email.EmailService;
import LangForU_DevTeam.LangForU.email.EmailTemplateService;
import LangForU_DevTeam.LangForU.registration.admin.AdminConfirmationToken;
import LangForU_DevTeam.LangForU.registration.admin.AdminConfirmationTokenService;
import LangForU_DevTeam.LangForU.registration.token.ConfirmationToken;
import LangForU_DevTeam.LangForU.registration.token.ConfirmationTokenService;
import LangForU_DevTeam.LangForU.singUpForCourse.UserCourseRequestService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

/**
 * Сервизен клас, който управлява бизнес логиката, свързана с потребителите (AppUser).
 * Имплементира UserDetailsService на Spring Security за интеграция със системата за сигурност.
 * Отговаря за операции като регистрация, автентикация, управление на потребители и роли.
 */
@Service
@AllArgsConstructor // Lombok: Автоматично генерира конструктор, който инжектира всички финални полета (dependency injection).
public class AppUserService implements UserDetailsService {

    //<editor-fold desc="Dependencies">
    private final AppUserRepository appUserRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final ConfirmationTokenService confirmationTokenService;
    private final AdminConfirmationTokenService adminConfirmationTokenService;
    private final EmailService emailService;
    private final EmailTemplateService emailTemplateService;
    private UserCourseRequestService userCourseRequestService;
    //</editor-fold>

    /**
     * Автентикира потребител чрез имейл и парола.
     * @param email Имейлът на потребителя.
     * @param rawPassword Суровата (некриптирана) парола.
     * @return Обект AppUser при успешен вход, в противен случай null.
     */
    public AppUser authenticate(String email, String rawPassword) {
        Optional<AppUser> appUserOptional = appUserRepository.findByEmail(email);

        if (appUserOptional.isPresent()) {
            AppUser appUser = appUserOptional.get();
            // Проверява дали подадената парола съвпада с хешираната парола в базата данни.
            if (bCryptPasswordEncoder.matches(rawPassword, appUser.getPassword())) {
                return appUser;
            }
        }
        return null;
    }

    /**
     * Потвърждава заявка за администраторска роля чрез токен.
     * @param token Токенът за потвърждение, изпратен на имейла.
     * @return 1 при успех, 0 при неуспех (напр. изтекъл или невалиден токен).
     */
    public int confirmAdmin(String token) {
        Optional<AdminConfirmationToken> optionalToken = adminConfirmationTokenService.getToken(token);

        if (optionalToken.isPresent()) {
            AdminConfirmationToken adminToken = optionalToken.get();

            // Проверява дали токенът не е вече потвърден и не е изтекъл.
            if (adminToken.getConfirmedAt() == null && adminToken.getExpiresAt().isAfter(LocalDateTime.now())) {
                try {
                    adminConfirmationTokenService.setConfirmedAt(token); // Маркира токена като потвърден.
                    appUserRepository.updateUserRole(adminToken.getAppUser().getId(), AppUserRole.ADMIN); // Променя ролята на потребителя.
                    return 1;
                } catch (Exception e) {
                    throw new RuntimeException("Възникна грешка при потвърждаване на администраторската роля: " + e.getMessage());
                }
            }
        }
        return 0;
    }

    /**
     * Изтрива потребител и всички свързани с него записи (заявки за курсове, токени).
     * @param userId ID на потребителя за изтриване.
     */
    @Transactional
    public void deleteUserAndRelatedRecords(Long userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Потребителят не беше намерен с ID: " + userId));

        try {
            // Изтрива свързаните записи, за да се избегнат проблеми с външни ключове (foreign key constraints).
            userCourseRequestService.deleteRequestsByUserId(userId);
            confirmationTokenService.deleteTokensByUserId(userId);

            if (user.getCourses() != null) {
                user.getCourses().clear(); // Премахва връзките към курсове.
            }

            appUserRepository.deleteById(userId); // Изтрива самия потребител.
            sendUserDeleteAccountEmail(user); // Изпраща уведомителен имейл.
        } catch (Exception e) {
            throw new RuntimeException("Възникна грешка при изтриването на потребителя с ID: " + userId + ". Причина: " + e.getMessage());
        }
    }

    /**
     * Деактивира потребителски акаунт по ID.
     * @param id ID на потребителя.
     */
    @Transactional
    public void disableUserById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID-то трябва да бъде положително число.");
        }
        if (!appUserRepository.existsById(id)) {
            throw new RuntimeException("Потребителят не е намерен с ID: " + id);
        }

        int updatedRows = appUserRepository.disableAppUser(id);
        if (updatedRows == 0) {
            throw new RuntimeException("Неуспешно деактивиране на потребителя с ID: " + id);
        }

        AppUser user = findUserById(id);
        sendUserDisableAccountEmail(user); // Изпраща уведомителен имейл.
    }

    /**
     * Активира потребителски акаунт (използва се след потвърждение на имейл).
     * @param email Имейл на потребителя.
     * @return Броят на обновените редове (обикновено 1).
     */
    public int enableAppUser(String email) {
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Имейлът не може да бъде празен или null.");
        }
        try {
            int result = appUserRepository.enableAppUser(email);
            if (result == 0) {
                throw new IllegalStateException("Неуспешно активиране на потребителя.");
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Възникна грешка при активирането на потребителя: " + e.getMessage());
        }
    }

    /**
     * Активира потребителски акаунт по ID (използва се от администратор).
     * @param id ID на потребителя.
     */
    @Transactional
    public void enableUserById(Long id) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Потребителят не беше намерен с ID: " + id));

        try {
            user.setEnabled(true);
            appUserRepository.save(user);
            sendUserEnabledAccountEmail(user); // Изпраща уведомителен имейл.
        } catch (Exception e) {
            throw new RuntimeException("Възникна грешка при активирането на акаунта на потребителя с ID: " + id + ". Причина: " + e.getMessage());
        }
    }

    /**
     * Намира потребител по имейл.
     * @param email Имейл за търсене.
     * @return Намереният AppUser.
     * @throws UsernameNotFoundException ако потребителят не е намерен.
     */
    public AppUser findByEmail(String email) {
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Имейлът не може да бъде празен.");
        }
        try {
            return appUserRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException(
                            String.format("Потребителят с имейл %s не беше намерен.", email)));
        } catch (Exception e) {
            throw new RuntimeException("Възникна грешка при търсене на потребителя с имейл: " + email + ". Причина: " + e.getMessage());
        }
    }

    /**
     * Намира потребител по неговото ID.
     * @param id ID за търсене.
     * @return Намереният AppUser.
     * @throws NoSuchElementException ако потребител с такова ID не съществува.
     */
    public AppUser findUserById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID-то трябва да бъде положително число.");
        }
        return appUserRepository.findById(id).orElseThrow(() ->
                new NoSuchElementException("Потребител с ID " + id + " не беше намерен."));
    }

    /**
     * Връща списък с всички потребители с определена роля.
     * @param role Ролята за филтриране (ADMIN или USER).
     * @return Списък с потребители.
     */
    public List<AppUser> getAllUsersByRole(AppUserRole role) {
        if (role == null) {
            throw new IllegalArgumentException("Ролята не може да бъде null.");
        }
        try {
            return appUserRepository.findByAppUserRole(role);
        } catch (Exception e) {
            throw new RuntimeException("Възникна грешка при извличането на потребители с роля: " + role + ". Причина: " + e.getMessage());
        }
    }

    /**
     * Връща текущо влезлия (автентикиран) потребител.
     * @return Обект AppUser на текущия потребител.
     * @throws IllegalStateException ако няма логнат потребител.
     */
    public AppUser getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("Няма намерен аутентифициран потребител.");
        }
        try {
            return (AppUser) loadUserByUsername(auth.getName());
        } catch (Exception e) {
            throw new RuntimeException("Възникна грешка при извличането на текущия потребител. Причина: " + e.getMessage());
        }
    }

    /**
     * Връща имейлите на всички потребители, записани за даден курс.
     * @param course Обект на курса.
     * @return Списък с имейли.
     */
    public List<String> getUserEmailsByCourse(Course course) {
        if (course == null || course.getId() == null) {
            throw new IllegalArgumentException("Курсът или неговото ID не могат да бъдат null.");
        }
        try {
            return appUserRepository.findUserEmailsByCourseId(course.getId());
        } catch (Exception e) {
            throw new RuntimeException("Възникна грешка при извличането на имейлите на потребителите за курса с ID: " + course.getId() + ". Причина: " + e.getMessage());
        }
    }

    /**
     * Връща името на потребител по неговия имейл.
     * @param email Имейл на потребителя.
     * @return Името (name) на потребителя или null, ако не е намерен.
     */
    public String getUserNameByEmail(String email) {
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Имейлът не може да бъде празен.");
        }
        try {
            AppUser appUser = findByEmail(email);
            return appUser != null ? appUser.getName() : null;
        } catch (Exception e) {
            throw new RuntimeException("Възникна грешка при получаването на името на потребителя с имейл: " + email + ". Причина: " + e.getMessage());
        }
    }

    /**
     * Връща списък с всички активирани потребители, които имат роля 'USER'.
     * @return Списък с потребители.
     * @throws NoSuchElementException ако няма такива потребители.
     */
    public List<AppUser> getEnabledUsersWithRoleUser() {
        try {
            List<AppUser> enabledUsers = appUserRepository.findByAppUserRoleAndEnabled(AppUserRole.USER, true);
            if (enabledUsers.isEmpty()) {
                throw new NoSuchElementException("Няма активирани потребители с роля 'USER'.");
            }
            return enabledUsers;
        } catch (Exception e) {
            throw new RuntimeException("Възникна грешка при извличането на потребителите: " + e.getMessage());
        }
    }

    /**
     * Метод от интерфейса UserDetailsService.
     * Зарежда потребител по неговото потребителско име (в случая имейл) за нуждите на Spring Security.
     * @param email Имейлът, който се използва като потребителско име.
     * @return Обект UserDetails (имплементиран от AppUser).
     * @throws UsernameNotFoundException ако потребителят не е намерен.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        String.format("Потребителят с имейл %s не беше намерен.", email)));
    }

    /**
     * Записва (създава или обновява) потребителски обект в базата данни.
     * @param currentUser Потребителят за запис.
     */
    public void save(AppUser currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Потребителят не може да бъде null.");
        }
        try {
            appUserRepository.save(currentUser);
        } catch (Exception e) {
            throw new RuntimeException("Възникна грешка при записване на потребителя. Причина: " + e.getMessage());
        }
    }

    /**
     * Изпраща заявка за администраторска роля. Генерира токен и изпраща имейл.
     * @param appUser Потребителят, който изпраща заявката.
     * @return Генерираният токен.
     */
    public String sendAdminRequest(AppUser appUser) {
        if (appUser == null) {
            throw new IllegalArgumentException("Потребителят не може да бъде null.");
        }
        if (appUser.getEmail() == null || appUser.getEmail().isEmpty()) {
            throw new IllegalArgumentException("Имейлът на потребителя не може да бъде празен.");
        }

        // Генерира уникален токен за потвърждение.
        String token = UUID.randomUUID().toString();
        AdminConfirmationToken adminToken = new AdminConfirmationToken(
                token,
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(15), // Токенът е валиден 15 минути.
                appUser
        );

        try {
            adminConfirmationTokenService.saveAdminConfirmationToken(adminToken);
        } catch (Exception e) {
            throw new RuntimeException("Възникна грешка при запазването на токена за администратора: " + e.getMessage());
        }

        String link = AppLinks.to("/admin/confirmAdmin?token=" + token);

        try {
            // Генерира и изпраща имейл с линк за активация.
            String emailBody = emailTemplateService.buildEmail_AdminActivation(appUser.getName(), link);
            emailService.send(appUser.getEmail(), emailBody);
        } catch (Exception e) {
            throw new RuntimeException("Възникна грешка при изпращането на имейла: " + e.getMessage());
        }

        return token;
    }

    /**
     * Изпраща имейл за уведомяване при изтриване на акаунт.
     * @param appUser Потребителят, чийто акаунт е изтрит.
     */
    public void sendUserDeleteAccountEmail(AppUser appUser) {
        if (appUser == null) {
            throw new IllegalArgumentException("Потребителят не може да бъде null.");
        }
        if (appUser.getEmail() == null || appUser.getEmail().isEmpty()) {
            throw new IllegalArgumentException("Имейлът на потребителя не може да бъде празен.");
        }
        try {
            String emailBody = emailTemplateService.buildEmail_AccountDeletion(appUser.getName(), appUser.getEmail());
            emailService.send(appUser.getEmail(), emailBody);
        } catch (Exception e) {
            throw new RuntimeException("Възникна грешка при изпращането на имейла за изтриване на акаунта: " + e.getMessage());
        }
    }

    /**
     * Изпраща имейл за уведомяване при деактивиране на акаунт.
     * @param appUser Потребителят, чийто акаунт е деактивиран.
     */
    public void sendUserDisableAccountEmail(AppUser appUser) {
        if (appUser == null) {
            throw new IllegalArgumentException("Потребителят не може да бъде null.");
        }
        if (appUser.getEmail() == null || appUser.getEmail().isEmpty()) {
            throw new IllegalArgumentException("Имейлът на потребителя не може да бъде празен.");
        }
        try {
            String emailBody = emailTemplateService.buildEmail_AccountDisable(appUser.getName(), appUser.getEmail());
            emailService.send(appUser.getEmail(), emailBody);
        } catch (Exception e) {
            throw new RuntimeException("Възникна грешка при изпращането на имейла за деактивиране на акаунта: " + e.getMessage());
        }
    }

    /**
     * Изпраща имейл за уведомяване при активиране на акаунт.
     * @param appUser Потребителят, чийто акаунт е активиран.
     */
    private void sendUserEnabledAccountEmail(AppUser appUser) {
        if (appUser == null) {
            throw new IllegalArgumentException("Потребителят не може да бъде null.");
        }
        if (appUser.getEmail() == null || appUser.getEmail().isEmpty()) {
            throw new IllegalArgumentException("Имейлът на потребителя не може да бъде празен.");
        }
        try {
            String emailBody = emailTemplateService.buildEmail_AccountEnabled(appUser.getName(), appUser.getEmail());
            emailService.send(appUser.getEmail(), emailBody);
        } catch (Exception e) {
            throw new RuntimeException("Възникна грешка при изпращането на имейла за активиране на акаунта: " + e.getMessage());
        }
    }

    /**
     * Регистрира нов потребител в системата.
     * @param appUser Обект AppUser с данните за регистрация.
     * @return Токен за потвърждение, който се изпраща на имейла.
     */
    public String signUpUser(AppUser appUser) {
        // Проверява дали имейлът вече не е зает.
        boolean userExists = appUserRepository.findByEmail(appUser.getEmail()).isPresent();
        if (userExists) {
            throw new IllegalStateException("Имейлът вече е зает.");
        }
        if (appUser.getPassword() == null || appUser.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Паролата не може да бъде празна.");
        }

        // Хешира паролата преди запис в базата данни.
        String encodedPassword = bCryptPasswordEncoder.encode(appUser.getPassword());
        appUser.setPassword(encodedPassword);

        try {
            appUserRepository.save(appUser);
        } catch (Exception e) {
            throw new RuntimeException("Възникна грешка при записването на потребителя: " + e.getMessage());
        }

        // Създава токен за потвърждение на имейла.
        String token = UUID.randomUUID().toString();
        ConfirmationToken confirmationToken = new ConfirmationToken(
                token,
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(15), // Токенът е валиден 15 минути.
                appUser
        );

        try {
            confirmationTokenService.saveConfirmationToken(confirmationToken);
        } catch (Exception e) {
            throw new RuntimeException("Възникна грешка при създаването на токена за потвърждение: " + e.getMessage());
        }

        return token;
    }
}