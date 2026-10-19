package LangForU_DevTeam.LangForU.registration;

import LangForU_DevTeam.LangForU.appuser.AppUser;
import LangForU_DevTeam.LangForU.appuser.AppUserRole;
import LangForU_DevTeam.LangForU.appuser.AppUserService;
import LangForU_DevTeam.LangForU.config.AppLinks;
import LangForU_DevTeam.LangForU.email.EmailSender;
import LangForU_DevTeam.LangForU.email.EmailTemplateService;
import LangForU_DevTeam.LangForU.registration.token.ConfirmationToken;
import LangForU_DevTeam.LangForU.registration.token.ConfirmationTokenService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Сервизен клас, който оркестрира процеса по регистрация на нови потребители.
 * Той координира работата на няколко други сервиза, за да изпълни всички стъпки:
 * валидация на данни, създаване на потребител, генериране на токен и изпращане на имейл за потвърждение.
 */
@Service
@AllArgsConstructor // Lombok: Генерира конструктор с всички полета за dependency injection.
public class RegistrationService {

    //<editor-fold desc="Dependencies">
    private final AppUserService appUserService;
    private final EmailValidator emailValidator;
    private final ConfirmationTokenService confirmationTokenService;
    private final EmailSender emailSender;
    private final EmailTemplateService emailTemplateService;
    //</editor-fold>

    /**
     * Основен метод, който обработва заявка за регистрация.
     *
     * @param request Обект {@link RegistrationRequest}, съдържащ данните от формата за регистрация.
     * @return Генерираният токен за потвърждение.
     * @throws IllegalStateException ако имейлът не е валиден.
     */
    public String register(RegistrationRequest request) {
        // 1. Валидира синтаксиса на имейла.
        boolean isValidEmail = emailValidator.test(request.getEmail());
        if (!isValidEmail) {
            throw new IllegalStateException("Невалиден имейл адрес");
        }

        // 2. Делегира създаването на потребител и токен към AppUserService.
        String token = appUserService.signUpUser(
                new AppUser(
                        request.getEmail(),
                        request.getPassword(),
                        request.getName(),
                        request.getDateOfBirth(),
                        request.getGender(),
                        AppUserRole.USER // Всички нови регистрации са с роля USER.
                )
        );

        // 3. Генерира линк за потвърждение и изпраща имейл.
        String link = AppLinks.to("/registration/confirm?token=" + token);
        emailSender.send(
                request.getEmail(),
                emailTemplateService.buildEmail_Registration(request.getName(), link)
        );

        return token;
    }

    /**
     * Потвърждава потребителски акаунт чрез подаден токен.
     *
     * @param token Низът на токена, получен от линка в имейла.
     * @return Низ "confirmed" при успех.
     * @throws IllegalStateException ако токенът е невалиден, вече използван или изтекъл.
     */
    @Transactional
    public String confirmToken(String token) {
        // 1. Намира токена в базата данни.
        ConfirmationToken confirmationToken = confirmationTokenService
                .getToken(token)
                .orElseThrow(() -> new IllegalStateException("Невалиден или изтекъл токен. Моля, уверете се, че връзката не е изтекла, и опитайте отново."));

        // 2. Проверява дали токенът вече не е потвърден.
        if (confirmationToken.getConfirmedAt() != null) {
            throw new IllegalStateException("Имейлът вече е потвърден");
        }

        // 3. Проверява дали токенът не е изтекъл.
        if (confirmationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Токенът е изтекъл. Моля, опитайте отново с нов токен.");
        }

        // 4. Маркира токена като потвърден.
        confirmationTokenService.setConfirmedAt(token);

        // 5. Активира потребителския акаунт.
        appUserService.enableAppUser(
                confirmationToken.getAppUser().getEmail());

        return "confirmed";
    }
}