package LangForU_DevTeam.LangForU.appuser;

import LangForU_DevTeam.LangForU.courses.Course;
import LangForU_DevTeam.LangForU.courses.CourseService;
import LangForU_DevTeam.LangForU.lections.Lection;
import lombok.AllArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Контролер, който обработва основните HTTP заявки, свързани с потребителския интерфейс,
 * като начална страница, страница "за нас", потребителски профил и други общи страници.
 */
@Controller
@RequestMapping
@AllArgsConstructor
public class UserController {

    private final AppUserService userService;
    private final CourseService courseService;

    /**
     * Пренасочва заявките от основния път ("/") към началната страница ("/index").
     * Достъпен за всички.
     * @return String за пренасочване.
     */
    @GetMapping("/")
    @PreAuthorize("permitAll()")
    public String root() {
        return "redirect:/index";
    }

    /**
     * Обработва заявки за началната страница.
     * Добавя списък с активни курсове и флаг за автентикация към модела.
     * Достъпен за всички.
     * @param model Модел за подаване на данни към изгледа (view).
     * @param principal Обект, представляващ текущо логнатия потребител (може да бъде null).
     * @return Името на шаблона "index".
     */
    @GetMapping("/index")
    @PreAuthorize("permitAll()")
    public String index(Model model, Principal principal) {
        // Проверява дали потребителят е автентикиран по наличието на Principal обекта.
        model.addAttribute("isAuthenticated", principal != null);

        List<Course> courses = courseService.getAllActiveCourses();
        if (courses.isEmpty()) {
            model.addAttribute("message", "В момента няма налични курсове.");
        }
        model.addAttribute("courses", courses);

        return "index";
    }

    /**
     * Обработва заявки за страницата "За нас".
     * Достъпен за всички.
     * @return Името на шаблона "about".
     */
    @GetMapping("/about")
    @PreAuthorize("permitAll()")
    public String about(Model model, Principal principal) {
        model.addAttribute("isAuthenticated", principal != null);

        List<Course> courses = courseService.getAllActiveCourses();
        model.addAttribute("courses", courses);

        return "about";
    }

    /**
     * Обработва заявки за страницата с общи условия.
     * Достъпен за всички.
     * @return Името на шаблона "terms".
     */
    @GetMapping("/terms")
    @PreAuthorize("permitAll()")
    public String terms() {
        return "terms";
    }

    /**
     * Обработва заявки за информационната страница "Sevi".
     * Достъпен за всички.
     * @return Името на шаблона "seviInfo".
     */
    @GetMapping("/Sevi")
    @PreAuthorize("permitAll()")
    public String seviInfo() {
        return "seviInfo";
    }

    /**
     * Обработва заявки за потребителския профил.
     * Изисква потребителят да бъде автентикиран.
     * @param model Модел за подаване на данни към изгледа.
     * @param principal Обект, представляващ текущо логнатия потребител.
     * @return Името на шаблона "user/profile".
     */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public String profile(Model model, Principal principal) {
        String email = principal.getName(); // Взимаме имейла на логнатия потребител.
        AppUser user = (AppUser) userService.loadUserByUsername(email);

        // Събира всички лекции от курсовете, за които потребителят е записан.
        List<Lection> userLections = user.getCourses().stream()
                .flatMap(course -> course.getLections().stream())
                .toList();

        // Подготвя данните за лекциите в удобен формат за календара в изгледа.
        List<Map<String, String>> lectionData = userLections.stream()
                .map(lection -> Map.of(
                        "title", lection.getName(),
                        "date", lection.getReleaseDate().toString()
                ))
                .collect(Collectors.toList());

        model.addAttribute("user", user);
        model.addAttribute("lectionEvents", lectionData);

        return "user/profile";
    }

    /**
     * REST ендпойнт за взимане на списък с налични аватари според пола.
     * Изисква потребителят да бъде автентикиран.
     * @param gender Полът ('male' или 'female'), за който се търсят аватари.
     * @return ResponseEntity, съдържащ списък с имената на файловете или JSON обект с грешка.
     */
    @GetMapping("/avatar/{gender}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAvatars(@PathVariable String gender) {
        if (!"male".equalsIgnoreCase(gender) && !"female".equalsIgnoreCase(gender)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Невалиден пол. Моля, използвайте 'male' или 'female'."));
        }
        try {
            // Аватарите се четат от classpath, за да работи и когато приложението е пуснато като JAR
            // (напр. на хостинг), а не само от изходния код.
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:static/img/avatars/" + gender.toLowerCase() + "/*");

            List<String> avatars = Arrays.stream(resources)
                    .map(Resource::getFilename)
                    .filter(Objects::nonNull)
                    .filter(name -> !name.isBlank())
                    .sorted()
                    .collect(Collectors.toList());

            if (avatars.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Няма налични аватари за този пол."));
            }
            return ResponseEntity.ok(avatars);
        } catch (IOException e) {
            // Забележка: В реално приложение е добре да се логва грешката (e.getMessage())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Възникна вътрешна грешка при четене на аватарите."));
        }
    }

    /**
     * REST ендпойнт за записване на избрания от потребителя аватар.
     * Изисква потребителят да бъде автентикиран.
     * @param avatarRequest Обект, съдържащ пътя до новата профилна снимка.
     * @param principal Обект, представляващ текущо логнатия потребител.
     * @return ResponseEntity със съобщение за успех или грешка.
     */
    @PostMapping("/avatar/save-avatar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> saveAvatar(@RequestBody AvatarRequest avatarRequest, Principal principal) {
        if (avatarRequest == null || !avatarRequest.isValid()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Грешка: Не е избран валиден аватар."));
        }
        try {
            AppUser currentUser = (AppUser) userService.loadUserByUsername(principal.getName());
            currentUser.setProfilePicture(avatarRequest.getProfilePicture());
            userService.save(currentUser);
            return ResponseEntity.ok(Map.of("message", "Аватарът беше записан успешно!"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Грешка при записване на аватара: " + e.getMessage()));
        }
    }
}