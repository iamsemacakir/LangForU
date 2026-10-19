package LangForU_DevTeam.LangForU.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Изгражда публични линкове към сайта (за имейли за потвърждение, известия и др.).
 * Базовият адрес се задава чрез настройката {@code app.base-url}
 * (променлива на средата {@code APP_BASE_URL}), напр. https://langforu.onrender.com.
 * Ако не е зададен, се използва http://localhost:8080 за локална разработка.
 */
@Component
public class AppLinks {

    private static final String DEFAULT_BASE_URL = "http://localhost:8080";

    // Статично поле, за да могат съществуващите сервизи (и unit тестовете им)
    // да ползват линковете без промяна в конструкторите си.
    private static volatile String baseUrl = DEFAULT_BASE_URL;

    public AppLinks(@Value("${app.base-url:" + DEFAULT_BASE_URL + "}") String configuredBaseUrl) {
        baseUrl = normalize(configuredBaseUrl);
    }

    /**
     * Връща пълен адрес към дадения път, напр. {@code to("/blog/detail/5")}.
     *
     * @param path Път, започващ с "/".
     * @return Пълният публичен адрес.
     */
    public static String to(String path) {
        return baseUrl + path;
    }

    private static String normalize(String url) {
        if (url == null || url.isBlank()) {
            return DEFAULT_BASE_URL;
        }
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
