package LangForU_DevTeam.LangForU.security.config;

import LangForU_DevTeam.LangForU.appuser.AppUserService;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Централен конфигурационен клас за Spring Security.
 * Този клас дефинира правилата за сигурност на приложението, включително
 * кои URL адреси са защитени, как се извършва автентикация и оторизация,
 * и как работи процесът на вход и изход от системата.
 */
@Configuration
@AllArgsConstructor
public class WebSecurityConfig {

    private final AppUserService appUserService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    /**
     * Дефинира основния филтър за сигурност (SecurityFilterChain), който се прилага към HTTP заявките.
     * Конфигурира CSRF защита, правила за достъп до URL адреси, формата за вход и функционалността за изход.
     *
     * @param http Обект {@link HttpSecurity}, който се използва за изграждане на конфигурацията.
     * @return Изграденият {@link SecurityFilterChain} обект.
     * @throws Exception при грешка в конфигурацията.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Конфигурация на CSRF (Cross-Site Request Forgery) защита.
                .csrf(csrf -> csrf
                        // Изключва CSRF защитата за определени пътища, които може да се извикват от външни клиенти или AJAX.
                        .ignoringRequestMatchers("/blog/subscribe", "/chat", "/chat/**", "/avatar/save-avatar", "/lections/submit")
                )
                // Конфигурация на правилата за оторизация (достъп до URL).
                .authorizeHttpRequests(authz -> authz
                        // --- Позволява публичен достъп (без нужда от логване) до изброените пътища. ---
                        .requestMatchers(
                                "/", "/index", "/courses", "/about", "/Sevi", "/terms", "/blog", "/blog/**", "/search", "/tag",
                                "/tag/**", "/blog/subscribe", "/category/**", "/contact", "/login", "/register",
                                "/courses/view/**", "/registration/confirm", "/confirmedRegistration",
                                "/registrationSuccess", "/admin/confirmAdmin/**", // <-- Преместено тук
                                "/chat", "/chat/send",
                                "/ping") // Проверка за работа на сайта (keep-alive при хостинга).
                        .permitAll()

                        // --- Изисква потребителят да има роля 'ADMIN' за достъп до всички пътища, които започват с /admin/. ---
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // Добавени специфични правила за изтриване/модифициране на курсове и лекции,
                        // които трябва да бъдат достъпни само за администратори и да използват POST заявки.
                        .requestMatchers(HttpMethod.POST, "/courses/delete/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/lections/delete/**").hasRole("ADMIN")

                        // --- Достъп за логнати потребители (USER или ADMIN) ---
                        .requestMatchers(
                                "/profile", "/avatar/**",
                                "/blog/*/like", "/blog/*/comment",
                                "/courses/signup/**", "/courses/requests",
                                "/lections/view/**", // view lection is for all authenticated users
                                "/final-exams/view/**", "/final-exams/submit/**", "/final-exams/*/certificate",
                                "/Sevi/chat" // chat access is for authenticated users
                        ).authenticated()

                        // Позволява публичен достъп до статични ресурси (CSS, JS, изображения и др.).
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/fonts/**", "/scss/**").permitAll()
                        // Всички останали заявки, които не са описани по-горе, изискват автентикация.
                        .anyRequest().authenticated()
                )

                // Конфигурация на формата за вход (login).
                .formLogin(form -> form
                        .loginPage("/login") // Указва пътя към персонализираната страница за вход.
                        .usernameParameter("email") // Указва, че полето за потребителско име в HTML формата е с name="email".
                        .defaultSuccessUrl("/profile", true) // Пренасочва към /profile след успешен вход.
                        .failureUrl("/login?error=true") // Пренасочва към тази страница при неуспешен вход.
                        .permitAll() // Позволява достъп до страницата за вход за всички.
                )

                // Конфигурация на функционалността за изход (logout).
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout")) // URL, който задейства изход.
                        .logoutSuccessUrl("/login?logout=true") // Страница, към която се пренасочва след успешен изход.
                        .invalidateHttpSession(true) // Прави HTTP сесията невалидна.
                        .deleteCookies("JSESSIONID") // Изтрива бисквитката за сесията.
                        .permitAll() // Позволява достъп до URL-а за изход за всички.
                );

        return http.build();
    }

    /**
     * Създава и конфигурира бийн за {@link DaoAuthenticationProvider}.
     * Това е основният компонент, който свързва Spring Security с потребителските данни.
     *
     * @return Конфигуриран DaoAuthenticationProvider.
     */
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        // Задава енкодера за пароли, който ще се използва за сравняване на паролите.
        provider.setPasswordEncoder(bCryptPasswordEncoder);
        // Задава нашия персонализиран сервиз, който знае как да зареди потребител по имейл.
        provider.setUserDetailsService(appUserService);
        return provider;
    }

    /**
     * Създава бийн за {@link AuthenticationManager}.
     * Този мениджър обработва автентикационните заявки.
     * Извлича се от стандартната конфигурация на Spring Security.
     *
     * @param authConfig Конфигурацията за автентикация.
     * @return Инстанция на AuthenticationManager.
     * @throws Exception при грешка.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
