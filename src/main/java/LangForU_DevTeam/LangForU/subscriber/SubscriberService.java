package LangForU_DevTeam.LangForU.subscriber;

import LangForU_DevTeam.LangForU.appuser.AppUser;
import LangForU_DevTeam.LangForU.appuser.AppUserService;
import LangForU_DevTeam.LangForU.blog.Blog;
import LangForU_DevTeam.LangForU.config.AppLinks;
import LangForU_DevTeam.LangForU.email.EmailService;
import LangForU_DevTeam.LangForU.email.EmailTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Сервизен клас, който капсулира бизнес логиката за управление на абонати.
 * Отговаря за абонирането на нови потребители и за изпращането на известия
 * до тях (напр. за нови блог публикации).
 */
@Service
public class SubscriberService {

    //<editor-fold desc="Dependencies">
    @Autowired
    private final SubscriberRepository subscriberRepository;
    private final AppUserService appUserService;
    private final EmailService emailService;
    private final EmailTemplateService emailTemplateService;
    //</editor-fold>

    /**
     * Конструктор за инжектиране на зависимости.
     */
    public SubscriberService(SubscriberRepository subscriberRepository, AppUserService appUserService, EmailService emailService, EmailTemplateService emailTemplateService) {
        this.subscriberRepository = subscriberRepository;
        this.appUserService = appUserService;
        this.emailService = emailService;
        this.emailTemplateService = emailTemplateService;
    }

    /**
     * Абонира потребител с даден имейл адрес.
     * Преди да създаде нов абонамент, проверява дали такъв вече не съществува,
     * за да се предотвратят дубликати.
     *
     * @param email Имейл адресът за абониране.
     * @return {@code true} ако абонаментът е създаден успешно,
     * {@code false} ако потребителят вече е бил абониран.
     */
    public boolean subscribe(String email) {
        Optional<Subscriber> existingSubscriber = subscriberRepository.findByEmail(email);

        // Ако не съществува абонат с този имейл, създаваме нов.
        if (existingSubscriber.isEmpty()) {
            Subscriber newSubscriber = new Subscriber();
            newSubscriber.setEmail(email);
            subscriberRepository.save(newSubscriber);
            return true;
        }

        // Ако абонатът вече съществува, не правим нищо.
        return false;
    }

    /**
     * Изпраща имейл известие до конкретен абонат за нова блог публикация.
     *
     * @param subscriber Обектът {@link Subscriber}, който да бъде уведомен.
     * @param blog       Обектът {@link Blog}, за който се изпраща известие.
     */
    public void notifySubscriber(Subscriber subscriber, Blog blog) {
        // Проверява дали абонатът все още съществува в базата данни.
        Optional<Subscriber> existingSubscriber = subscriberRepository.findByEmail(subscriber.getEmail());
        if (existingSubscriber.isEmpty()) {
            // Ако не съществува (може да е отписан междувременно), го изтриваме за всеки случай.
            subscriberRepository.delete(subscriber);
            return;
        }

        // Опитва да намери регистриран потребител с имейла на абоната.
        AppUser appUser = appUserService.findByEmail(subscriber.getEmail());
        if (appUser == null) {
            // Ако няма такъв потребител, изтриваме абонамента (ако логиката го изисква).
            subscriberRepository.delete(subscriber);
            return;
        }

        // Генерира линк към новата публикация и създава съдържанието на имейла чрез шаблон.
        String link = AppLinks.to("/blog/detail/" + blog.getId());
        String emailContent = emailTemplateService.buildEmail_NewBlogNotification(appUser.getName(), blog.getName(), link);

        // Изпраща имейла.
        emailService.send(subscriber.getEmail(), emailContent);
    }

}