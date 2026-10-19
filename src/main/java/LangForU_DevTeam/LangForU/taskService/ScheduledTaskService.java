package LangForU_DevTeam.LangForU.taskService;

import LangForU_DevTeam.LangForU.appuser.AppUserService;
import LangForU_DevTeam.LangForU.config.AppLinks;
import LangForU_DevTeam.LangForU.email.EmailService;
import LangForU_DevTeam.LangForU.email.EmailTemplateService;
import LangForU_DevTeam.LangForU.lections.Lection;
import LangForU_DevTeam.LangForU.lections.LectionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Сервизен клас, който съдържа планирани задачи (cron jobs), изпълнявани автоматично.
 * В момента съдържа задача за изпращане на известия по имейл, когато нови лекции станат налични.
 */
@Service
public class ScheduledTaskService {

    private final LectionService lectionService;
    private final EmailService emailService;
    private final AppUserService appUserService;
    private final EmailTemplateService emailTemplateService;

    /**
     * Конструктор за внедряване на зависимостите чрез DI.
     *
     * @param lectionService         сервиз за лекции
     * @param emailService           сервиз за изпращане на имейли
     * @param appUserService         сервиз за потребителски данни
     * @param emailTemplateService   сервиз за изграждане на шаблони за имейли
     */
    public ScheduledTaskService(LectionService lectionService, EmailService emailService,
                                AppUserService appUserService, EmailTemplateService emailTemplateService) {
        this.lectionService = lectionService;
        this.emailService = emailService;
        this.appUserService = appUserService;
        this.emailTemplateService = emailTemplateService;
    }

    /**
     * Планирана задача, която се изпълнява всяка сутрин в 08:00.
     * Изпраща имейл известие на потребителите, когато нова лекция за техния курс е налична за деня.
     */
    @Scheduled(cron = "0 0 8 * * ?")  // изпълнява се ежедневно в 08:00 ч.
    @Transactional
    public void notifyAvailableLections() {
        LocalDate today = LocalDate.now();

        // Извличане на лекции, които стават достъпни днес
        List<Lection> availableLections = lectionService.findByReleaseDate(today);

        for (Lection lection : availableLections) {
            // Получаване на имейл адресите на всички потребители, записани в курса
            List<String> userEmails = appUserService.getUserEmailsByCourse(lection.getCourse());

            for (String email : userEmails) {
                // Извличане на името на потребителя
                String userName = appUserService.getUserNameByEmail(email);
                String courseName = lection.getCourse().getLanguage();
                String lectionTitle = lection.getName();

                // Генериране на линк към новата лекция
                String link = AppLinks.to("/lections/view/" + lection.getId());

                // Създаване на съдържание за имейл чрез шаблон
                String emailContent = emailTemplateService.buildEmail_NewLectionNotification(
                        userName, courseName, lectionTitle, link);

                // Изпращане на имейла
                emailService.send(email, emailContent);
            }
        }
    }
}
