package LangForU_DevTeam.LangForU.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Сервизен клас, отговорен за изпращането на имейли от приложението.
 * Имплементира интерфейса {@link EmailSender} и използва {@link JavaMailSender} на Spring
 * за същинското изпращане.
 */
@Service
@RequiredArgsConstructor // Lombok: Генерира конструктор с всички final полета за dependency injection.
public class EmailService implements EmailSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender emailSender; // Компонент на Spring за изпращане на имейли.
    private final EmailTemplateService emailTemplateService; // Персонализиран сервиз за генериране на HTML съдържание за имейли.

    // Имейл на изпращача. Задава се с променливата на средата MAIL_FROM
    // (трябва да е потвърден подател при доставчика на имейли, напр. Brevo).
    @Value("${app.mail.from:langforu.softdev@gmail.com}")
    private String fromAddress = "langforu.softdev@gmail.com";

    /**
     * Основен метод за изпращане на имейл.
     * Операцията се изпълнява асинхронно (@Async), за да не блокира основната нишка на приложението.
     *
     * @param to    Имейл адресът на получателя.
     * @param email Съдържанието на имейла (очаква се да бъде HTML).
     */
    @Override
    @Async // Указва на Spring да изпълни този метод в отделна нишка.
    public void send(String to, String email) {
        try {
            // Създаване на MimeMessage, който поддържа HTML съдържание, прикачени файлове и др.
            MimeMessage mimeMessage = emailSender.createMimeMessage();
            // Помощен клас за лесно попълване на MimeMessage.
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setText(email, true); // Вторият параметър 'true' указва, че съдържанието е HTML.
            helper.setTo(to);
            helper.setSubject("LangForU"); // Тема на имейла.
            helper.setFrom(fromAddress); // Имейл на изпращача.
            emailSender.send(mimeMessage);
        } catch (MessagingException e) {
            // При грешка, записваме в логовете и хвърляме изключение, за да сигнализираме за проблема.
            LOGGER.error("Неуспешно изпращане на имейл към {}", to, e);
            throw new IllegalStateException("Изпращането на имейла се провали.", e);
        }
    }

    /**
     * Специфичен метод за изпращане на отговор на заявка за контакт.
     * Той използва {@link EmailTemplateService} за да генерира тялото на имейла,
     * след което вика основния 'send' метод за изпращане.
     *
     * @param to            Имейл адресът на получателя (потребителя, изпратил заявката).
     * @param middleContent Основното съдържание на отговора, написано от администратора.
     * @param adminName     Името на администратора, който отговаря.
     * @param userName      Името на потребителя, на когото се отговаря.
     */
    public void sendEmail(String to, String middleContent, String adminName, String userName) {
        try {
            // Генериране на HTML тялото на имейла чрез шаблонен сервиз.
            String body = emailTemplateService.buildEmail_contactAnswer(middleContent, adminName, userName);
            // Извикване на основния метод за изпращане.
            send(to, body);
        } catch (Exception e) {
            LOGGER.error("Грешка при подготовка или изпращане на имейл към {}", to, e);
            throw new IllegalStateException("Възникна грешка при изпращането на имейла.", e);
        }
    }
}