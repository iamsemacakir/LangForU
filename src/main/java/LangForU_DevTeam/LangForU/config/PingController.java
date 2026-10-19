package LangForU_DevTeam.LangForU.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Лека проверка дали сайтът работи.
 * Външна услуга (напр. cron-job.org) отваря /ping на всеки няколко минути,
 * за да не "заспива" безплатният сървър в Render.
 * Не използва базата данни, за да не изразходва безплатните часове на Neon.
 */
@RestController
public class PingController {

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("ok");
    }
}
