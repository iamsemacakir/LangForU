package LangForU_DevTeam.LangForU.config;

import LangForU_DevTeam.LangForU.appuser.AppUser;
import LangForU_DevTeam.LangForU.appuser.AppUserRepository;
import LangForU_DevTeam.LangForU.appuser.AppUserRole;
import LangForU_DevTeam.LangForU.appuser.AppUserService;
import LangForU_DevTeam.LangForU.blog.Blog;
import LangForU_DevTeam.LangForU.blog.BlogRepository;
import LangForU_DevTeam.LangForU.courses.Course;
import LangForU_DevTeam.LangForU.courses.CourseRepository;
import LangForU_DevTeam.LangForU.courses.Level;
import LangForU_DevTeam.LangForU.finalexam.FinalExam;
import LangForU_DevTeam.LangForU.finalexam.FinalExamRepository;
import LangForU_DevTeam.LangForU.lections.Lection;
import LangForU_DevTeam.LangForU.lections.LectionRepository;
import LangForU_DevTeam.LangForU.question.Question;
import LangForU_DevTeam.LangForU.registration.token.ConfirmationToken;
import LangForU_DevTeam.LangForU.registration.token.ConfirmationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Configuration
public class DatabaseInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Autowired
    private AppUserService appUserService;

    // Паролите на началните акаунти се задават чрез променливи на средата (ADMIN_PASSWORD,
    // DEMO_USER_PASSWORD), а не в кода, защото кодът е публичен в GitHub.
    // Ако паролата не е зададена, съответният акаунт не се създава.
    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.demo-user.password:}")
    private String demoUserPassword;

    @Bean
    public CommandLineRunner initDatabase(AppUserRepository appUserRepository,
                                          CourseRepository courseRepository,
                                          BlogRepository blogRepository,
                                          FinalExamRepository finalExamRepository,
                                          LectionRepository lectionRepository,
                                          ConfirmationTokenRepository confirmationTokenRepository,
                                          PasswordEncoder passwordEncoder) {
        return args -> {
            try {
                createAdminProfile(appUserRepository, confirmationTokenRepository, passwordEncoder);
                createCourses(courseRepository, lectionRepository, finalExamRepository);
                createBlogs(blogRepository);
            } catch (Exception e) {
                logger.error("Error initializing database: {}", e.getMessage(), e);
            }
        };
    }

    private void createAdminProfile(AppUserRepository appUserRepository,
                                    ConfirmationTokenRepository confirmationTokenRepository,
                                    PasswordEncoder passwordEncoder) {

        appUserRepository.findByEmail("langforu.softdev@gmail.com").ifPresentOrElse(
                admin -> logger.info("Admin user already exists. Skipping initialization."),
                () -> {
                    if (adminPassword == null || adminPassword.isBlank()) {
                        logger.warn("ADMIN_PASSWORD не е зададена – администраторският акаунт не е създаден.");
                        return;
                    }
                    try {
                        AppUser adminUser = new AppUser(
                                "langforu.softdev@gmail.com",
                                passwordEncoder.encode(adminPassword),
                                "Админ",
                                LocalDate.of(2002, 3, 6),
                                "female",
                                AppUserRole.ADMIN
                        );
                        appUserRepository.save(adminUser);

                        ConfirmationToken confirmationToken = new ConfirmationToken(
                                UUID.randomUUID().toString(),
                                LocalDateTime.now(),
                                LocalDateTime.now().plusMinutes(15),
                                adminUser
                        );
                        confirmationTokenRepository.save(confirmationToken);

                        logger.info("Admin user and confirmation token created successfully.");
                    } catch (Exception e) {
                        logger.error("Failed to create admin user: {}", e.getMessage(), e);
                    }
                }
        );

        appUserRepository.findByEmail("semayasharova@gmail.com").ifPresentOrElse(
                user -> logger.info("Ordinary user already exists. Skipping initialization."),
                () -> {
                    if (demoUserPassword == null || demoUserPassword.isBlank()) {
                        logger.info("DEMO_USER_PASSWORD не е зададена – демо потребителят не е създаден.");
                        return;
                    }
                    try {
                        AppUser ordinaryUser = new AppUser(
                                "semayasharova@gmail.com",
                                passwordEncoder.encode(demoUserPassword),
                                "Сема Яшарова",
                                LocalDate.of(2002, 3, 6),
                                "female",
                                AppUserRole.USER
                        );
                        appUserRepository.save(ordinaryUser);

                        ConfirmationToken confirmationToken = new ConfirmationToken(
                                UUID.randomUUID().toString(),
                                LocalDateTime.now(),
                                LocalDateTime.now().plusMinutes(15),
                                ordinaryUser
                        );
                        confirmationTokenRepository.save(confirmationToken);

                        logger.info("Ordinary user and confirmation token created successfully.");
                    } catch (Exception e) {
                        logger.error("Failed to create ordinary user: {}", e.getMessage(), e);
                    }
                }
        );
    }

    private void createBlogs(BlogRepository blogRepository) {
        if (blogRepository.count() > 0) {
            logger.info("Блоговете вече съществуват. Пропускане на инициализацията.");
            return;
        }


        AppUser adminUser = appUserService.findByEmail("langforu.softdev@gmail.com");

        List<Blog> blogs = List.of(
                new Blog(
                        null,
                        "Защо е важно да учим английски език?",
                        "Открийте защо английският език е критично умение в съвременния свят.",
                        "Английският език е един от най-говорените езици в света и играе важна роля в глобалната комуникация, образование и бизнес. Много компании изискват владеене на английски, а университетите често предлагат програми на този език. Освен това, английският е ключът към култури и ресурси, които иначе биха били недостъпни. Учейки английски, вие се отваряте към нови възможности и перспективи. \n\n"
                                + "Процесът на учене на английски може да бъде предизвикателство, особено за начинаещите. За да направите първите стъпки, започнете с изучаване на основна граматика и базов речник. Редовното четене на книги и гледане на филми на английски ще ви помогне да усвоите езика по-бързо. Практикувайте говорене с приятели или в онлайн групи, за да развиете увереността си. \n\n"
                                + "Английският е езикът на интернет и технологиите. Много от най-големите уебсайтове, включително социалните медии, използват английски като основен език. Затова изучаването му ще ви позволи да се възползвате от информация, която иначе би останала скрита. Учейки английски, вие не само развивате езикови умения, но и ставате по-конкурентоспособни на пазара на труда. \n\n"
                                + "Английският език е и мост между различните култури. Чрез него можете да се запознаете с обичаи, литература и изкуство от различни страни. Учейки английски, вие разширявате хоризонтите си и се свързвате с хора от цял свят. Това не е просто език, а инструмент за комуникация, културно разбиране и личностно развитие.",
                        LocalDate.now(),
                        "https://biornacademy.com/wp-content/uploads/2022/06/Learning-The-English-Language0.jpeg",
                        List.of("Езиково обучение", "Английски"),
                        List.of("Английски", "Умения", "Образование"),
                        null,
                        new HashSet<>(),
                        adminUser
                ),
                new Blog(
                        null,
                        "Как да започнем да учим немски език?",
                        "Съвети и трикове за начинаещи, които учат немски език.",
                        "Немският език е предизвикателен, но също така и много полезен за изучаване. В Германия има много възможности за работа и образование, които изискват познания по немски език. За да започнете успешно, е важно да имате план и да се придържате към него. \n\n"
                                + "Започнете с основния речник и научете често използвани фрази. Учете граматика, но не се плашете от сложните правила – те ще станат по-ясни с практиката. Гледайте видеоклипове и слушайте подкасти на немски, за да свикнете с произношението. \n\n"
                                + "Практикувайте редовно с носители на езика – това е най-добрият начин да развиете уменията си. Можете да използвате онлайн платформи за обмен на езици, за да намерите партньори за разговори. Ученето на немски изисква време и постоянство, но с правилния подход ще усетите значителен напредък.",
                        LocalDate.now(),
                        "https://images.pexels.com/photos/109629/pexels-photo-109629.jpeg",
                        List.of("Езиково обучение", "Немски"),
                        List.of("Немски", "Съвети", "Начинаещи"),
                        null,
                        new HashSet<>(),
                        adminUser
                ),
                new Blog(
                        null,
                        "Съвети за напреднали учащи италиански език",
                        "Техники за усъвършенстване на италиански език за напреднали.",
                        "Ако вече сте усвоили основите на италианския език, е време да се съсредоточите върху по-сложни аспекти като идиоми и литературен изказ. Италианският език е богат на културни нюанси и изрази, които могат да бъдат предизвикателни дори за напреднали учащи. \n\n"
                                + "За да усъвършенствате уменията си, четете италианска литература и гледайте филми без субтитри. Това ще ви помогне да разберете по-добре езика и да усетите ритъма му. Опитайте да разговаряте с носители на езика, за да се адаптирате към различни акценти и диалекти. \n\n"
                                + "Редовните упражнения и анализът на текстове ще ви помогнат да развиете по-задълбочени познания. Идиомите и разговорните фрази са ключови за по-естественото използване на езика. С практика и постоянство ще успеете да достигнете ниво на владеене, което ще ви позволи да общувате свободно и уверено.",
                        LocalDate.now(),
                        "https://languagesconnect.ie/wp-content/uploads/2021/08/Say-Yes-to-Italian-2-1024x626.png",
                        List.of("Езиково обучение", "Италиански"),
                        List.of("Италиански", "Напреднали съвети", "Флуентност"),
                        null,
                        new HashSet<>(),
                        adminUser
                )
        );

        try {
            blogRepository.saveAll(blogs);
            logger.info("Блоговете са инициализирани успешно.");
        } catch (Exception e) {
            logger.error("Грешка при инициализацията на блоговете: {}", e.getMessage(), e);
        }
    }


    private void createCourses(CourseRepository courseRepository, LectionRepository lectionRepository, FinalExamRepository finalExamRepository) {
        if (courseRepository.count() > 0) {
            logger.info("Courses already exist. Skipping initialization.");
            return;
        }


        Course englishA1Course = new Course(
                null,
                "Безплатен курс по Английски език  A1",
                Level.A1,
                199.99f,
                LocalDate.now(),
                LocalDate.now().plusMonths(3),
                "Този начален курс ще ви научи на основните принципи на английския език. Ще овладеете базова граматика, основен речников запас и ключови фрази за ежедневни разговори. След завършване ще получите сертификат за ниво A1 и ще можете да се изразявате в прости ситуации. Севи ще бъде на разположение за допълнителна помощ и практика.",
                "Джон Смит",
                "Мария Иванова",
                "Иван Димитров",
                "https://media.istockphoto.com/id/1047570732/vector/english.jpg?s=612x612&w=is&k=20&c=oFIS0kUTvJe-uTXcDTTbgQaevFtGNaV1ngPFRqxcLvw=",
                5,
                new ArrayList<>(),
                null
        );


        Course germanA1Course = new Course(
                null,
                "Безплатен курс по Немски език  A1",
                Level.A1,
                199.99f,
                LocalDate.now(),
                LocalDate.now().plusMonths(3),
                "Този курс по немски език ниво A1 ще ви въведе в основите на езика – азбуката, представянето, числата, часовете и ежедневни разговори. Ще изградите основен речников запас и ще научите граматични конструкции, необходими за общуване в ежедневието.",
                "Франк Майер",
                "Лора Шулц",
                "Иван Димитров",
                "https://media.istockphoto.com/id/1090094168/vector/german-german-language-hand-drawn-doodles-and-lettering.jpg?s=1024x1024&w=is&k=20&c=CtjHiKC-NHl-Hvf6n3lIHzztdiGlYIxhRXpGrd06HHA=",
                5,
                new ArrayList<>(),
                null
        );


        Course italianA1Course = new Course(
                null,
                "Безплатен курс по Италиански език  A1",
                Level.A1,
                199.99f,
                LocalDate.now(),
                LocalDate.now().plusMonths(3),
                "Научете основите на италианския език с този начален курс. Ще усвоите ключови изрази за пътуване, ежедневие и общуване, както и базова граматика и произношение. Подходящ за начинаещи с нулево ниво.",
                "Марко Роси",
                "Анна Белучи",
                "Иван Димитров",
                "https://media.istockphoto.com/id/1131178156/vector/italian.jpg?s=612x612&w=0&k=20&c=F5y5GYwyWKHtkIaBkp1I96S-lGjOEybhQibfS82jRGM=",
                5,
                new ArrayList<>(),
                null
        );


        Course spanishA1Course = new Course(
                null,
                "Безплатен курс по Испански език  A1",
                Level.A1,
                199.99f,
                LocalDate.now(),
                LocalDate.now().plusMonths(3),
                "Този курс е за пълни начинаещи, които искат да започнат да говорят испански. Ще покрием основни теми като представяне, семейство, числа, дати, времето и как да общувате в различни ежедневни ситуации.",
                "Карлос Гомес",
                "Мария Лопес",
                "Иван Димитров",
                "https://media.istockphoto.com/id/1055903384/vector/espanol.jpg?s=612x612&w=0&k=20&c=C-ECjXQxLQmFj7rczqvqpWlmaPOljpFAVPfJ3Nvxxp8=",
                5,
                new ArrayList<>(),
                null
        );


        Course japaneseA1Course = new Course(
                null,
                "Безплатен курс по Японски език  A1",
                Level.A1,
                199.99f,
                LocalDate.now(),
                LocalDate.now().plusMonths(3),
                "Запознайте се с японския език чрез този начален курс – ще научите хирагана, катакана, основни канджи и как да водите елементарен разговор. Курсът покрива основни фрази, граматика и културни особености.",
                "Юки Танака",
                "Харука Сато",
                "Иван Димитров",
                "https://a.storyblok.com/f/55469/1176x732/3070577851/jp_-_2022.png",
                5,
                new ArrayList<>(),
                null
        );


        Course russianA1Course = new Course(
                null,
                "Безплатен курс по Руски език  A1",
                Level.A1,
                199.99f,
                LocalDate.now(),
                LocalDate.now().plusMonths(3),
                "Този курс по руски език е създаден за абсолютно начинаещи. Ще научите кирилицата, основно произношение, речник и граматика. Подходящ за хора, които искат да започнат да четат, пишат и говорят на руски в ежедневни ситуации.",
                "Алексей Петров",
                "Оксана Смирнова",
                "Иван Димитров",
                "https://media.istockphoto.com/id/1365806793/vector/people-learning-russian-language-or-talk-banner.jpg?s=612x612&w=0&k=20&c=dHuAaHxjczN8yPXEFlIjYu4BB_Mlq9BLStK8bqekZmw=",
                5,
                new ArrayList<>(),
                null
        );


        courseRepository.save(englishA1Course);
        courseRepository.save(germanA1Course);
        courseRepository.save(italianA1Course);
        courseRepository.save(spanishA1Course);
        courseRepository.save(japaneseA1Course);
        courseRepository.save(russianA1Course);

        List<Lection> englishA1Lections = List.of(
                new Lection(
                        null,
                        "Hello and Welcome!",
                        "Научете основните поздрави, как да се представяте и как да питате за името и произхода на някого. Практикувайте прости диалози за първи срещи.",
                        "https://www.youtube.com/watch?v=EFo74b6b7Lw",
                        "Начинаещи (A1)",
                        LocalDate.now(),
                        "Джон Смит",
                        List.of(
                                new Question(
                                        "Какъв е английският еквивалент на 'Привет'?",
                                        List.of("Hi", "Goodbye", "Please", "Thank you"),
                                        "Hi"
                                ),
                                new Question(
                                        "Представете се на английски: 'My name is [име]. I am from [държава].'",
                                        "My name is"
                                )
                        ),
                        // Допълнителни ресурси с активни линкове
                        """
                        <p><strong>ESL eSchool – Greetings and Introductions:</strong> Интерактивни уроци и упражнения за начинаещи, които обхващат основните поздрави и представяне.<br/>
                        <a href="https://esleschool.com" target="_blank" rel="noopener noreferrer">https://esleschool.com</a></p>
                    
                        <p><strong>Learn English Online – Basic Introductions:</strong> Подробен урок за основни представяния на английски език, включително граматически обяснения и упражнения.<br/>
                        <a href="https://learn-english-online.org" target="_blank" rel="noopener noreferrer">https://learn-english-online.org</a></p>
                    
                        <p><strong>USA Learns – Free Online English Courses:</strong> Безплатни онлайн курсове по английски език за начинаещи и междинно ниво, които включват видео уроци и интерактивни упражнения.<br/>
                        <a href="https://usalearns.org" target="_blank" rel="noopener noreferrer">https://usalearns.org</a></p>
                    
                        <p><strong>ESOL Courses – Free English Lessons Online:</strong> Разнообразие от безплатни уроци по английски език, подходящи за самостоятелно обучение.<br/>
                        <a href="https://esolcourses.com" target="_blank" rel="noopener noreferrer">https://esolcourses.com</a></p>
                    
                        <p><strong>Busuu – How to Introduce Yourself in English:</strong> Ръководство с полезни фрази и съвети за представяне на английски език, подходящо за начинаещи.<br/>
                        <a href="https://www.busuu.com" target="_blank" rel="noopener noreferrer">https://www.busuu.com</a></p>
                        """,
                        // Подробно текстово изложение (summary)
                        """
                        <h2>Подробно изложение на урока "Hello and Welcome!"</h2>
                    
                        <p>Този урок е предназначен за начинаещи, които тепърва започват да учат английски език и искат да овладеят основните умения за поздрави и представяне.</p>
                    
                        <h3>1. Основни поздрави</h3>
                        <p>Започваме с най-често използваните поздрави в ежедневието:</p>
                        <ul>
                            <li><strong>Hello</strong> – официално и универсално поздравление, подходящо за всяка ситуация.</li>
                            <li><strong>Hi</strong> – по-неформален поздрав, използван сред приятели и познати.</li>
                            <li><strong>Good morning</strong> – поздрав за сутринта, използва се до около 12 часа на обяд.</li>
                            <li><strong>Good afternoon</strong> – поздрав за следобеда, използва се от 12:00 до около 18:00 часа.</li>
                            <li><strong>Good evening</strong> – поздрав за вечерта, използва се след 18:00 часа.</li>
                            <li><strong>Goodbye</strong> или <strong>Bye</strong> – думи за сбогуване, като "Goodbye" е по-официално, а "Bye" – по-неформално.</li>
                        </ul>
                        <p>Тези фрази се използват, когато се срещате или разделяте с хора.</p>
                    
                        <h3>2. Как да се представите</h3>
                        <p>След като поздравите някого, следва да се представите. Най-важната фраза за това е:</p>
                        <p><em>My name is...</em> – "Казвам се...".</p>
                        <p>Например: <strong>My name is Maria.</strong></p>
                        <p>Можете да добавите и информация откъде сте, като използвате израза:</p>
                        <p><em>I am from...</em> – "Аз съм от...".</p>
                        <p>Например: <strong>I am from Bulgaria.</strong></p>
                        <p>Така цялото представяне може да звучи така:</p>
                        <p><em>Hello! My name is Maria. I am from Bulgaria.</em></p>
                    
                        <h3>3. Как да зададете въпроси за запознанство</h3>
                        <p>За да започнете разговор, трябва да можете да питате другия човек за неговото име и произход. Това става с въпросите:</p>
                        <ul>
                            <li><strong>What’s your name?</strong> – Как се казваш?</li>
                            <li><strong>Where are you from?</strong> – От къде си?</li>
                        </ul>
                        <p>Отговорите на тези въпроси са с вече научените изрази:</p>
                        <ul>
                            <li><strong>My name is John.</strong></li>
                            <li><strong>I am from England.</strong></li>
                        </ul>
                    
                        <h3>4. Допълнителни полезни фрази</h3>
                        <p>В урока се учи и как да кажете възрастта си, което е полезно в ежедневните разговори. Това става с израза:</p>
                        <p><em>I am [възраст] years old.</em> Например: <strong>I am 25 years old.</strong></p>
                    
                        <h3>5. Практика с диалози</h3>
                        <p>Видео урокът включва прости диалози, които пресъздават типична ситуация на първа среща или запознанство, където се използват всички научени изрази за поздрави, представяне и задаване на въпроси.</p>
                        <p>Това помага да се развият умения за слушане, разбиране и говорене.</p>
                    
                        <h3>6. Как да използвате урока</h3>
                        <p>За да усвоите материала, е добре да гледате видеото няколко пъти, да повтаряте след говорителя и да практикувате диалозите с приятел или преподавател.</p>
                        <p>Можете също да използвате допълнителните ресурси, за да направите упражнения, да прочетете допълнителни обяснения и да се упражнявате по различни начини.</p>
                    
                        <h3>7. Обобщение</h3>
                        <p>Този урок дава основите на комуникацията на английски език за начинаещи: как да поздравявате, как да се представяте, как да питате и да отговаряте за името и произхода, както и как да изразявате възрастта си.</p>
                        <p>Практиката на тези умения ще ви помогне да започнете разговори на английски и да се чувствате по-уверени при първи срещи.</p>
                        """,
                        englishA1Course
                ),

                new Lection(
                        null,
                        "The English Alphabet and Pronunciation",
                        "Get familiar with the English alphabet, common pronunciation rules, and how to spell your name and other basic words.",
                        "https://example.com/video2.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(2),
                        "John Smith",
                        List.of(
                                new Question("What letter comes after 'B' in the English alphabet?",
                                        List.of("C", "D", "A", "E"), "C"),
                                new Question("Spell your name in English.", "Some name")
                        ),
                        "https://example.com/extra_resource2",
                        "In this lesson, you will learn the English alphabet and how to pronounce the letters. You will also practice spelling your name and other common words. Attention is given to pronunciation tips and tricks.",
                        englishA1Course
                ),

                new Lection(
                        null,
                        "Where Are You From?",
                        "Talk about countries and nationalities. Practice asking and answering questions like “Where are you from?” and “What’s your nationality?”",
                        "https://example.com/video3.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(3),
                        "John Smith",
                        List.of(
                                new Question("What is the nationality of a person from France?",
                                        List.of("French", "Spanish", "German", "Italian"), "French"),
                                new Question("Where are you from?", "Some name")
                        ),
                        "https://example.com/extra_resource3",
                        "In this lesson, you'll learn how to ask about someone's nationality and country of origin. You'll practice using 'Where are you from?' and answering the question correctly. Vocabulary related to countries and nationalities will be introduced.",
                        englishA1Course
                ),

                new Lection(
                        null,
                        "Numbers and Ages",
                        "Learn numbers from 0 to 100, how to ask about age, phone numbers, and basic math expressions.",
                        "https://example.com/video4.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(4),
                        "John Smith",
                        List.of(
                                new Question("How do you say '45' in English?",
                                        List.of("Fortyfive", "Fiftyfour", "Fourfive", "Fourtyfive"), "Fortyfive"),
                                new Question("How old are you?", "Some name")
                        ),
                        "https://example.com/extra_resource4",
                        "In this lesson, we will cover numbers in English, from 0 to 100. You'll learn how to ask someone’s age, how to express your own age, and how to talk about phone numbers. Basic math operations will also be practiced.",
                        englishA1Course
                ),

                new Lection(
                        null,
                        "Days, Months, and Dates",
                        "Master the days of the week, months of the year, and how to say and ask about dates and birthdays.",
                        "https://example.com/video5.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(5),
                        "John Smith",
                        List.of(
                                new Question("Which day of the week is after Friday?",
                                        List.of("Monday", "Tuesday", "Saturday", "Sunday"), "Saturday"),
                                new Question("When is your birthday?", "Some name")
                        ),
                        "https://example.com/extra_resource5",
                        "This lesson will teach you how to say the days of the week and the months of the year. You’ll learn how to ask and answer questions about dates, birthdays, and important events.",
                        englishA1Course
                ),


                new Lection(
                        null,
                        "What Time Is It?",
                        "Tell and ask the time in English, learn timerelated vocabulary, and practice talking about daily schedules.",
                        "https://example.com/video6.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(6),
                        "John Smith",
                        List.of(
                                new Question("How do you ask for the time in English?",
                                        List.of("What is the time?", "What time is it?", "How long is it?", "What time do you have?"), "What time is it?"),
                                new Question("What time do you wake up?", "Some name")
                        ),
                        "https://example.com/extra_resource6",
                        "Learn how to tell time in English and how to ask for the time. We will also introduce expressions for different parts of the day and practice talking about your daily routine.",
                        englishA1Course
                ),

                new Lection(
                        null,
                        "My Family and Friends",
                        "Talk about family members and relationships. Practice vocabulary for describing people’s roles and appearances.",
                        "https://example.com/video7.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(7),
                        "John Smith",
                        List.of(
                                new Question("Who is your mother’s sister?",
                                        List.of("Aunt", "Uncle", "Cousin", "Brother"), "Aunt"),
                                new Question("Describe your best friend.", "Some name")
                        ),
                        "https://example.com/extra_resource7",
                        "This lesson covers vocabulary for family members (mother, father, etc.) and friends. Learn how to describe people’s appearances and relationships, and practice using possessive pronouns.",
                        englishA1Course
                ),

                new Lection(
                        null,
                        "A Day in My Life",
                        "Describe your daily routine using the present simple tense. Learn common verbs and time expressions for daily activities.",
                        "https://example.com/video8.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(8),
                        "John Smith",
                        List.of(
                                new Question("How do you say 'I eat lunch at 12 PM' in English?",
                                        List.of("I eat lunch at 12 PM.", "I eats lunch at 12 PM.", "I eating lunch at 12 PM.", "I had lunch at 12 PM."), "I eat lunch at 12 PM."),
                                new Question("Describe your daily routine.", "Some name")
                        ),
                        "https://example.com/extra_resource8",
                        "Learn how to describe your daily activities using the present simple tense. You'll practice talking about your routine using time expressions like 'in the morning,' 'at night,' and 'every day.'",
                        englishA1Course
                ),


                new Lection(
                        null,
                        "My House and Home",
                        "Learn the vocabulary for rooms and furniture. Describe where you live and talk about your home environment.",
                        "https://example.com/video9.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(9),
                        "John Smith",
                        List.of(
                                new Question("Where do you sleep?",
                                        List.of("In the kitchen", "In the living room", "In the bedroom", "In the bathroom"), "In the bedroom"),
                                new Question("Describe your living room.", "Some name")
                        ),
                        "https://example.com/extra_resource9",
                        "This lesson will introduce vocabulary for different rooms and furniture in the house. You’ll learn how to describe your living environment and talk about where you live.",
                        englishA1Course
                ),

                new Lection(
                        null,
                        "Let’s Eat!",
                        "Explore food and drink vocabulary, practice ordering in a café or restaurant, and talk about your favorite meals.",
                        "https://example.com/video10.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(10),
                        "John Smith",
                        List.of(
                                new Question("What is the word for 'спагети' in English?",
                                        List.of("Spaghetti", "Pasta", "Burger", "Pizza"), "Spaghetti"),
                                new Question("Order a coffee in a café.", "Some name")
                        ),
                        "https://example.com/extra_resource10",
                        "In this lesson, you'll learn food-related vocabulary, how to order food in a restaurant, and how to talk about your favorite meals. We'll also discuss polite expressions when dining out.",
                        englishA1Course
                ),

                new Lection(
                        null,
                        "Getting Around Town",
                        "Learn to describe places in a city, ask for and give directions, and understand transportation-related vocabulary.",
                        "https://example.com/video11.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(11),
                        "John Smith",
                        List.of(
                                new Question("Where is the post office?",
                                        List.of("On Main Street", "At the park", "Behind the store", "In the square"), "On Main Street"),
                                new Question("How do you get to the nearest bus stop?", "Some name")
                        ),
                        "https://example.com/extra_resource11",
                        "In this lesson, you'll learn how to ask for and give directions in English. We'll cover vocabulary related to transportation, places in a city, and practice using prepositions for locations.",
                        englishA1Course
                ),


                new Lection(
                        null,
                        "My House and Home",
                        "Learn the vocabulary for rooms and furniture. Describe where you live and talk about your home environment.",
                        "https://example.com/video9.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(9),
                        "John Smith",
                        List.of(
                                new Question("Where do you sleep?",
                                        List.of("In the kitchen", "In the living room", "In the bedroom", "In the bathroom"), "In the bedroom"),
                                new Question("Describe your living room.", "Some name")
                        ),
                        "https://example.com/extra_resource9",
                        "This lesson will introduce vocabulary for different rooms and furniture in the house. You’ll learn how to describe your living environment and talk about where you live.",
                        englishA1Course
                ),

                new Lection(
                        null,
                        "Let’s Eat!",
                        "Explore food and drink vocabulary, practice ordering in a café or restaurant, and talk about your favorite meals.",
                        "https://example.com/video10.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(10),
                        "John Smith",
                        List.of(
                                new Question("What is the word for 'спагети' in English?",
                                        List.of("Spaghetti", "Pasta", "Burger", "Pizza"), "Spaghetti"),
                                new Question("Order a coffee in a café.", "Some name")
                        ),
                        "https://example.com/extra_resource10",
                        "In this lesson, you'll learn food-related vocabulary, how to order food in a restaurant, and how to talk about your favorite meals. We'll also discuss polite expressions when dining out.",
                        englishA1Course
                ),

                new Lection(
                        null,
                        "Getting Around Town",
                        "Learn to describe places in a city, ask for and give directions, and understand transportation-related vocabulary.",
                        "https://example.com/video11.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(11),
                        "John Smith",
                        List.of(
                                new Question("Where is the post office?",
                                        List.of("On Main Street", "At the park", "Behind the store", "In the square"), "On Main Street"),
                                new Question("How do you get to the nearest bus stop?", "Some name")
                        ),
                        "https://example.com/extra_resource11",
                        "In this lesson, you'll learn how to ask for and give directions in English. We'll cover vocabulary related to transportation, places in a city, and practice using prepositions for locations.",
                        englishA1Course
                ),

                new Lection(
                        null,
                        "What Do You Do?",
                        "Talk about jobs, professions, and what you or others do for a living. Practice using “to be” and “do” in basic questions.",
                        "https://example.com/video12.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(12),
                        "John Smith",
                        List.of(
                                new Question("What is the job of someone who teaches students?",
                                        List.of("Doctor", "Teacher", "Engineer", "Artist"), "Teacher"),
                                new Question("What do you do?", "Some name")
                        ),
                        "https://example.com/extra_resource12",
                        "Learn how to talk about jobs and professions in this lesson. You'll practice asking and answering questions like 'What do you do?' and use 'to be' and 'do' in context.",
                        englishA1Course
                ),

                new Lection(
                        null,
                        "School Life",
                        "Discuss school subjects, classroom objects, and typical school routines. Learn to describe your learning environment.",
                        "https://example.com/video13.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(13),
                        "John Smith",
                        List.of(
                                new Question("Which object do you use to write?",
                                        List.of("Pencil", "Eraser", "Book", "Paper"), "Pencil"),
                                new Question("What is your favorite school subject?", "Some name")
                        ),
                        "https://example.com/extra_resource13",
                        "In this lesson, you’ll discuss your school life, talk about different subjects, and describe your school routine. You will also learn classroom-related vocabulary and common phrases.",
                        englishA1Course
                ),

                new Lection(
                        null,
                        "Hobbies and Free Time",
                        "Talk about what you like to do in your free time, express preferences, and make simple invitations and responses.",
                        "https://example.com/video14.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(14),
                        "John Smith",
                        List.of(
                                new Question("What is your favorite hobby?",
                                        List.of("Reading", "Sleeping", "Watching TV", "Exercising"), "Reading"),
                                new Question("Invite someone to go to the cinema with you.", "Some name")
                        ),
                        "https://example.com/extra_resource14",
                        "Learn how to talk about your hobbies and free time activities. This lesson will help you express preferences and make simple invitations or responses for different activities.",
                        englishA1Course
                ),

                new Lection(
                        null,
                        "At the Store",
                        "Learn vocabulary for clothes and shopping, practice asking for sizes, prices, and making basic purchases.",
                        "https://example.com/video15.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(15),
                        "John Smith",
                        List.of(
                                new Question("How do you ask for the price of an item?",
                                        List.of("How much is this?", "What is the price?", "Is this expensive?", "How much it costs?"), "How much is this?"),
                                new Question("What clothes do you need to buy?", "Some name")
                        ),
                        "https://example.com/extra_resource15",
                        "This lesson will teach you vocabulary for clothes and shopping. You will practice asking for sizes, prices, and making basic purchases at a store.",
                        englishA1Course
                )
        );


        List<Lection> germanA1Lections = List.of(
                new Lection(
                        null,
                        "Hallo und Willkommen!",
                        "Lerne grundlegende Begrüßungen, wie man sich vorstellt und wie man nach dem Namen und Herkunft einer Person fragt. Übe einfache Dialoge für erste Begegnungen.",
                        "https://example.com/video1.mp4",
                        "Beginner",
                        LocalDate.now(),
                        "Max Müller",
                        List.of(
                                new Question("Wie sagt man 'Hello' auf Deutsch?",
                                        List.of("Hallo", "Goodbye", "Bitte", "Danke"), "Hallo"),
                                new Question("Stelle dich auf Deutsch vor: 'Ich heiße [Name]. Ich komme aus [Land].'", "Some name")
                        ),
                        "https://example.com/extra_resource1",
                        "In dieser Lektion lernst du, wie man sich begrüßt und vorstellt. Du wirst auch üben, wie du nach dem Namen und der Herkunft einer Person fragst.",
                        germanA1Course
                ),
                new Lection(
                        null,
                        "Das deutsche Alphabet und die Aussprache",
                        "Mache dich mit dem deutschen Alphabet und den häufigsten Ausspracheregeln vertraut. Lerne, deinen Namen und andere einfache Wörter zu buchstabieren.",
                        "https://example.com/video2.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(2),
                        "Max Müller",
                        List.of(
                                new Question("Welcher Buchstabe kommt nach 'B' im deutschen Alphabet?",
                                        List.of("C", "D", "A", "E"), "C"),
                                new Question("Buchstabiere deinen Namen auf Deutsch.", "Some name")
                        ),
                        "https://example.com/extra_resource2",
                        "In dieser Lektion lernst du das deutsche Alphabet und die korrekte Aussprache der Buchstaben. Du wirst auch üben, deinen Namen und andere gängige Wörter zu buchstabieren.",
                        germanA1Course
                ),
                new Lection(
                        null,
                        "Woher kommst du?",
                        "Sprich über Länder und Nationalitäten. Übe Fragen wie 'Woher kommst du?' und 'Was ist deine Nationalität?'",
                        "https://example.com/video3.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(3),
                        "Max Müller",
                        List.of(
                                new Question("Welche Nationalität hat eine Person aus Frankreich?",
                                        List.of("Französisch", "Spanisch", "Deutsch", "Italienisch"), "Französisch"),
                                new Question("Woher kommst du?", "Some name")
                        ),
                        "https://example.com/extra_resource3",
                        "In dieser Lektion lernst du, wie man nach der Herkunft einer Person fragt und wie man antwortet. Du wirst Vokabular zu Ländern und Nationalitäten kennenlernen.",
                        germanA1Course
                ),
                new Lection(
                        null,
                        "Zahlen und Alter",
                        "Lerne die Zahlen von 0 bis 100, wie man nach dem Alter fragt und wie man Telefonnummern und einfache mathematische Ausdrücke verwendet.",
                        "https://example.com/video4.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(4),
                        "Max Müller",
                        List.of(
                                new Question("Wie sagt man '45' auf Deutsch?",
                                        List.of("Fünfundvierzig", "Fünf vier", "Vierzig fünf", "Vier fünf"), "Fünfundvierzig"),
                                new Question("Wie alt bist du?", "Some name")
                        ),
                        "https://example.com/extra_resource4",
                        "In dieser Lektion lernst du, wie man Zahlen von 0 bis 100 auf Deutsch sagt. Du wirst lernen, nach dem Alter zu fragen und Telefonnummern zu nennen.",
                        germanA1Course
                ),
                new Lection(
                        null,
                        "Tage, Monate und Daten",
                        "Lerne die Wochentage, die Monate und wie man nach dem Datum und dem Geburtstag fragt.",
                        "https://example.com/video5.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(5),
                        "Max Müller",
                        List.of(
                                new Question("Welcher Wochentag folgt auf Freitag?",
                                        List.of("Montag", "Dienstag", "Samstag", "Sonntag"), "Samstag"),
                                new Question("Wann hast du Geburtstag?", "Some name")
                        ),
                        "https://example.com/extra_resource5",
                        "In dieser Lektion lernst du, wie man die Wochentage und Monate auf Deutsch sagt. Du wirst auch lernen, nach dem Datum und Geburtstag zu fragen.",
                        germanA1Course
                ),
                new Lection(
                        null,
                        "Wie spät ist es?",
                        "Lerne, wie man nach der Uhrzeit fragt und wie man sie auf Deutsch sagt. Übe die Zeitangaben und tägliche Aktivitäten.",
                        "https://example.com/video6.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(6),
                        "Max Müller",
                        List.of(
                                new Question("Wie fragt man auf Deutsch nach der Uhrzeit?",
                                        List.of("Wie spät ist es?", "Was ist die Zeit?", "Wie lange ist es?", "Was ist die Stunde?"), "Wie spät ist es?"),
                                new Question("Wann stehst du normalerweise auf?", "Some name")
                        ),
                        "https://example.com/extra_resource6",
                        "In dieser Lektion lernst du, wie man nach der Uhrzeit fragt und wie man sie auf Deutsch sagt. Du wirst Zeitangaben und einfache Tagesabläufe üben.",
                        germanA1Course
                ),
                new Lection(
                        null,
                        "Meine Familie und Freunde",
                        "Sprich über Familienmitglieder und Beziehungen. Übe, wie du das Aussehen und die Rollen der Personen beschreiben kannst.",
                        "https://example.com/video7.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(7),
                        "Max Müller",
                        List.of(
                                new Question("Wie heißt die Schwester deiner Mutter?",
                                        List.of("Tante", "Onkel", "Cousine", "Bruder"), "Tante"),
                                new Question("Beschreibe deinen besten Freund.", "Some name")
                        ),
                        "https://example.com/extra_resource7",
                        "In dieser Lektion lernst du, wie man Familienmitglieder und Freunde beschreibt. Du wirst üben, das Aussehen von Menschen zu beschreiben und Beziehungen zu erklären.",
                        germanA1Course
                ),
                new Lection(
                        null,
                        "Mein Tagesablauf",
                        "Beschreibe deinen Tagesablauf im Präsens. Lerne häufige Verben und Zeitangaben, um alltägliche Aktivitäten zu beschreiben.",
                        "https://example.com/video8.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(8),
                        "Max Müller",
                        List.of(
                                new Question("Wie sagt man auf Deutsch: 'Ich esse um 12 Uhr Mittag'?",
                                        List.of("Ich esse um 12 Uhr Mittag.", "Ich esse Mittag um 12 Uhr.", "Ich esse bei 12 Mittag.", "Mittag esse ich um 12 Uhr."), "Ich esse um 12 Uhr Mittag."),
                                new Question("Beschreibe deinen Tagesablauf.", "Some name")
                        ),
                        "https://example.com/extra_resource8",
                        "In dieser Lektion lernst du, wie du deinen Tagesablauf beschreiben kannst. Du wirst das Präsens benutzen und Zeitangaben wie 'am Morgen', 'nachts' und 'jeden Tag' üben.",
                        germanA1Course
                ),
                new Lection(
                        null,
                        "Mein Zuhause",
                        "Lerne Vokabular zu Zimmern und Möbeln. Beschreibe, wo du wohnst, und erkläre dein Zuhause.",
                        "https://example.com/video9.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(9),
                        "Max Müller",
                        List.of(
                                new Question("Wo schläfst du?",
                                        List.of("In der Küche", "Im Wohnzimmer", "Im Schlafzimmer", "Im Badezimmer"), "Im Schlafzimmer"),
                                new Question("Beschreibe dein Wohnzimmer.", "Some name")
                        ),
                        "https://example.com/extra_resource9",
                        "In dieser Lektion lernst du, wie man verschiedene Zimmer und Möbel im Haus beschreibt. Du wirst auch üben, wie du dein Zuhause und deine Umgebung erklärst.",
                        germanA1Course
                )

        );


        List<Lection> italianA1Lections = List.of(
                new Lection(
                        null,
                        "Ciao! Mi chiamo...",
                        "Impara a presentarti in italiano. Dì il tuo nome, chiedi il nome degli altri e saluta in modo formale e informale.",
                        "https://example.com/video1.mp4",
                        "Beginner",
                        LocalDate.now(),
                        "Giulia Rossi",
                        List.of(
                                new Question("Come ti chiami?",
                                        List.of("Mi chiamo Maria", "Ho fame", "Buongiorno", "Sto bene"), "Mi chiamo Maria"),
                                new Question("Presentati in due frasi.", "Some name")
                        ),
                        "https://example.com/extra_resource1",
                        "In questa lezione imparerai a presentarti in italiano, a dire il tuo nome e a chiedere il nome degli altri. Scoprirai le differenze tra saluti formali e informali e quando usarli.",
                        italianA1Course
                ),
                new Lection(
                        null,
                        "Numeri e colori",
                        "Apprendi i numeri da 1 a 20 e i colori principali. Usa i numeri per contare e descrivi oggetti con i colori.",
                        "https://example.com/video2.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(2),
                        "Giulia Rossi",
                        List.of(
                                new Question("Qual è il numero dopo dieci?",
                                        List.of("Dodici", "Otto", "Undici", "Tredici"), "Undici"),
                                new Question("Qual è il tuo colore preferito e perché?", "Some name")
                        ),
                        "https://example.com/extra_resource2",
                        "In questa lezione imparerai i numeri da 1 a 20 e i principali colori in italiano. Userai i numeri per contare oggetti e i colori per descriverli.",
                        italianA1Course
                ),
                new Lection(
                        null,
                        "Famiglia e amici",
                        "Parla della tua famiglia e dei tuoi amici. Impara i vocaboli familiari e come descrivere le relazioni.",
                        "https://example.com/video3.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(3),
                        "Giulia Rossi",
                        List.of(
                                new Question("Chi è il fratello di tua madre?",
                                        List.of("Zio", "Nonno", "Padre", "Cugino"), "Zio"),
                                new Question("Descrivi un membro della tua famiglia.", "Some name")
                        ),
                        "https://example.com/extra_resource3",
                        "In questa lezione scoprirai il vocabolario per parlare della tua famiglia e dei tuoi amici. Imparerai a descrivere le persone e le relazioni familiari.",
                        italianA1Course
                ),
                new Lection(
                        null,
                        "Giorni e mesi",
                        "Impara i giorni della settimana e i mesi dell’anno. Parla delle date e pianifica eventi.",
                        "https://example.com/video4.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(4),
                        "Giulia Rossi",
                        List.of(
                                new Question("Qual è il primo mese dell’anno?",
                                        List.of("Gennaio", "Dicembre", "Febbraio", "Marzo"), "Gennaio"),
                                new Question("Qual è il tuo giorno preferito e perché?", "Some name")
                        ),
                        "https://example.com/extra_resource4",
                        "In questa lezione imparerai i giorni della settimana e i mesi dell’anno. Ti eserciterai a parlare delle date, a usare il calendario e a pianificare eventi semplici.",
                        italianA1Course
                ),
                new Lection(
                        null,
                        "A casa",
                        "Descrivi la tua casa e impara i nomi delle stanze e dei mobili principali.",
                        "https://example.com/video5.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(5),
                        "Giulia Rossi",
                        List.of(
                                new Question("Dove dormi?",
                                        List.of("In bagno", "In salotto", "In cucina", "In camera da letto"), "In camera da letto"),
                                new Question("Descrivi la tua stanza preferita.", "Some name")
                        ),
                        "https://example.com/extra_resource5",
                        "Questa lezione ti insegnerà a parlare della tua casa, nominare le stanze e descrivere l’arredamento di base. Potrai descrivere dove vivi e come è fatta la tua abitazione.",
                        italianA1Course
                ),
                new Lection(
                        null,
                        "Mangiare e bere",
                        "Impara a parlare di cibi e bevande. Ordina al ristorante e parla dei tuoi piatti preferiti.",
                        "https://example.com/video6.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(6),
                        "Giulia Rossi",
                        List.of(
                                new Question("Quale tra questi è una bevanda?",
                                        List.of("Pizza", "Pasta", "Acqua", "Pane"), "Acqua"),
                                new Question("Cosa mangi a colazione?", "Some name")
                        ),
                        "https://example.com/extra_resource6",
                        "In questa lezione imparerai i nomi dei cibi e delle bevande, come ordinare in un ristorante e come parlare delle tue preferenze alimentari.",
                        italianA1Course
                ),
                new Lection(
                        null,
                        "Andare in giro",
                        "Parla di mezzi di trasporto e chiedi indicazioni. Impara a muoverti in città.",
                        "https://example.com/video7.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(7),
                        "Giulia Rossi",
                        List.of(
                                new Question("Qual è un mezzo di trasporto?",
                                        List.of("Treno", "Albero", "Casa", "Sedia"), "Treno"),
                                new Question("Come vai a scuola o al lavoro?", "Some name")
                        ),
                        "https://example.com/extra_resource7",
                        "Questa lezione ti aiuta a parlare dei mezzi di trasporto, a chiedere e dare indicazioni e a comprendere i segnali in città.",
                        italianA1Course
                ),
                new Lection(
                        null,
                        "Tempo libero",
                        "Parla dei tuoi hobby e di cosa fai nel tempo libero. Impara verbi comuni e attività quotidiane.",
                        "https://example.com/video8.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(8),
                        "Giulia Rossi",
                        List.of(
                                new Question("Quale tra questi è un passatempo?",
                                        List.of("Leggere", "Mangiare", "Dormire", "Correre"), "Leggere"),
                                new Question("Cosa fai nel tempo libero?", "Some name")
                        ),
                        "https://example.com/extra_resource8",
                        "In questa lezione parlerai delle tue attività preferite, scoprirai nuovi hobby e userai verbi comuni per descrivere le tue abitudini.",
                        italianA1Course
                ),
                new Lection(
                        null,
                        "Fare la spesa",
                        "Impara a fare acquisti nei negozi. Chiedi prezzi e parla di quantità.",
                        "https://example.com/video9.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(9),
                        "Giulia Rossi",
                        List.of(
                                new Question("Cosa compri al supermercato?",
                                        List.of("Libro", "Pane", "Telefono", "Divano"), "Pane"),
                                new Question("Descrivi cosa compri ogni settimana.", "Some name")
                        ),
                        "https://example.com/extra_resource9",
                        "In questa lezione imparerai a chiedere il prezzo degli oggetti, parlare delle quantità e interagire in modo semplice nei negozi o al mercato.",
                        italianA1Course
                ),
                new Lection(
                        null,
                        "Al bar",
                        "Ordina cibi e bevande al bar. Impara frasi utili per interagire con il personale.",
                        "https://example.com/video10.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(10),
                        "Giulia Rossi",
                        List.of(
                                new Question("Cosa ordini al bar per colazione?",
                                        List.of("Un cappuccino", "Una pizza", "Una pasta", "Un vino"), "Un cappuccino"),
                                new Question("Simula un dialogo al bar.", "Some name")
                        ),
                        "https://example.com/extra_resource10",
                        "In questa lezione ti eserciterai a ordinare qualcosa da mangiare o bere in un bar italiano e a rispondere in modo cortese.",
                        italianA1Course
                ),
                new Lection(
                        null,
                        "La routine quotidiana",
                        "Parla della tua giornata tipo. Usa verbi riflessivi e indicazioni temporali.",
                        "https://example.com/video11.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(11),
                        "Giulia Rossi",
                        List.of(
                                new Question("Cosa fai la mattina?",
                                        List.of("Mi sveglio", "Mangio la cena", "Vado a dormire", "Lavoro di notte"), "Mi sveglio"),
                                new Question("Descrivi la tua routine quotidiana.", "Some name")
                        ),
                        "https://example.com/extra_resource11",
                        "Questa lezione ti guiderà nel parlare delle azioni quotidiane, usando verbi riflessivi e parole che indicano il tempo.",
                        italianA1Course
                ),
                new Lection(
                        null,
                        "Che lavoro fai?",
                        "Parla di professioni e descrivi che lavoro fai. Impara a fare e rispondere a domande sul lavoro.",
                        "https://example.com/video12.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(12),
                        "Giulia Rossi",
                        List.of(
                                new Question("Chi lavora in una scuola?",
                                        List.of("Cuoco", "Insegnante", "Medico", "Pescatore"), "Insegnante"),
                                new Question("Che lavoro fai o vorresti fare?", "Some name")
                        ),
                        "https://example.com/extra_resource12",
                        "In questa lezione imparerai a parlare della tua professione o aspirazione lavorativa, usando il verbo 'fare' e descrivendo diverse occupazioni.",
                        italianA1Course
                )

        );

        List<Lection> spanishA1Lections = List.of(
                new Lection(
                        null,
                        "¡Hola y bienvenido!",
                        "Learn basic Spanish greetings, how to introduce yourself, and ask about someone’s name and origin. Practice simple conversational exchanges.",
                        "https://example.com/video1.mp4",
                        "Beginner",
                        LocalDate.now(),
                        "Juan Pérez",
                        List.of(
                                new Question("How do you say 'Hello' in Spanish?",
                                        List.of("Hola", "Adiós", "Buenos días", "Buenas noches"), "Hola"),
                                new Question("¿Cómo te llamas?", "Some name")
                        ),
                        "https://example.com/extra_resource1",
                        "In this lesson, you’ll learn basic greetings in Spanish, how to ask someone’s name and where they’re from, and introduce yourself.",
                        spanishA1Course
                ),

                new Lection(null,
                        "El alfabeto y la pronunciación",
                        "Familiarize yourself with the Spanish alphabet, pronunciation tips (including special letters like ñ), and how to spell simple words.",
                        "https://example.com/video2.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(2),
                        "Juan Pérez",
                        List.of(
                                new Question("What is the Spanish letter 'ñ' called?",
                                        List.of("Eñe", "Ni", "Ñeta", "Niño"), "Eñe"),
                                new Question("Cómo se dice 'cat' en español?", "Some name")
                        ),
                        "https://example.com/extra_resource2",
                        "In this lesson, you’ll learn the Spanish alphabet and get tips on pronunciation, including how to pronounce special letters like 'ñ'.",
                        spanishA1Course
                ),

                new Lection(null,
                        "¿De dónde eres?",
                        "Talk about countries and nationalities. Practice asking and answering '¿De dónde eres?' and stating your origin.",
                        "https://example.com/video3.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(3),
                        "Juan Pérez",
                        List.of(
                                new Question("How do you ask 'Where are you from?' in Spanish?",
                                        List.of("¿De dónde eres?", "¿Dónde está?", "¿De dónde vives?", "¿De dónde vienes?"), "¿De dónde eres?"),
                                new Question("¿De dónde eres?", "Some name")
                        ),
                        "https://example.com/extra_resource3",
                        "This lesson will teach you how to ask and answer questions about where you’re from, and discuss countries and nationalities.",
                        spanishA1Course
                ),

                new Lection(null,
                        "Números y la edad",
                        "Learn numbers from 0 to 100, talk about age, phone numbers, and use numbers in daily contexts.",
                        "https://example.com/video4.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(4),
                        "Juan Pérez",
                        List.of(
                                new Question("How do you say 'twentyfive' in Spanish?",
                                        List.of("Veinticinco", "Cinco veint", "Veinte y cinco", "Cinco veinte"), "Veinticinco"),
                                new Question("¿Cuántos años tienes?", "Some name")
                        ),
                        "https://example.com/extra_resource4",
                        "In this lesson, you’ll learn the numbers from 0 to 100 and how to ask about age and phone numbers in Spanish.",
                        spanishA1Course
                ),

                new Lection(null,
                        "Días, meses y fechas",
                        "Master the days of the week, months of the year, and how to say and ask about dates and birthdays.",
                        "https://example.com/video5.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(5),
                        "Juan Pérez",
                        List.of(
                                new Question("What is the Spanish word for 'Monday'?",
                                        List.of("Lunes", "Domingo", "Martes", "Miércoles"), "Lunes"),
                                new Question("¿Cuándo es tu cumpleaños?", "Some name")
                        ),
                        "https://example.com/extra_resource5",
                        "In this lesson, you’ll learn the days of the week and months of the year, and how to ask about dates and birthdays in Spanish.",
                        spanishA1Course
                ),

                new Lection(null,
                        "¿Qué hora es?",
                        "Tell and ask the time in Spanish, use daily time expressions, and describe your typical daily routine.",
                        "https://example.com/video6.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(6),
                        "Juan Pérez",
                        List.of(
                                new Question("How do you say 'What time is it?' in Spanish?",
                                        List.of("¿Qué hora es?", "¿Cuál es la hora?", "¿Qué tiempo es?", "¿A qué hora?"), "¿Qué hora es?"),
                                new Question("¿A qué hora te levantas?", "Some name")
                        ),
                        "https://example.com/extra_resource6",
                        "Learn how to tell and ask the time in Spanish and practice describing your daily routine with timerelated expressions.",
                        spanishA1Course
                ),

                new Lection(null,
                        "Mi familia y mis amigos",
                        "Talk about your family and friends. Describe relationships, appearances, and basic personality traits.",
                        "https://example.com/video7.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(7),
                        "Juan Pérez",
                        List.of(
                                new Question("How do you say 'My brother is tall' in Spanish?",
                                        List.of("Mi hermano es alto", "Mi hermano es bajo", "Mi hermano está alto", "Mi hermano alto es"), "Mi hermano es alto"),
                                new Question("¿Tienes hermanos?", "Some name")
                        ),
                        "https://example.com/extra_resource7",
                        "In this lesson, you’ll learn how to talk about your family, describe relationships, and discuss basic personality traits.",
                        spanishA1Course
                ),

                new Lection(null,
                        "El clima y las estaciones",
                        "Talk about the weather and learn expressions for different seasons in Spanish.",
                        "https://example.com/video21.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(21),
                        "Juan Pérez",
                        List.of(
                                new Question("How do you say 'It is raining' in Spanish?",
                                        List.of("Está lloviendo", "Hace lluvia", "Hay lluvia", "Llueve ahora"), "Está lloviendo"),
                                new Question("¿Qué tiempo hace hoy?", "Some name")
                        ),
                        "https://example.com/extra_resource21",
                        "In this lesson, you’ll learn vocabulary and phrases to describe the weather and talk about the seasons.",
                        spanishA1Course
                ),

                new Lection(null,
                        "Ropa y estilo",
                        "Learn vocabulary related to clothing and personal style, and practice describing what people are wearing.",
                        "https://example.com/video22.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(22),
                        "Juan Pérez",
                        List.of(
                                new Question("How do you say 'I am wearing a red shirt' in Spanish?",
                                        List.of("Llevo una camisa roja", "Uso una camisa roja", "Estoy con camisa roja", "Tengo camisa roja"), "Llevo una camisa roja"),
                                new Question("¿Qué ropa te gusta llevar?", "Some name")
                        ),
                        "https://example.com/extra_resource22",
                        "This lesson teaches vocabulary for clothing and how to describe outfits and personal style in Spanish.",
                        spanishA1Course
                ),

                new Lection(null,
                        "La tecnología y la vida diaria",
                        "Talk about technology, devices you use, and digital communication in everyday life.",
                        "https://example.com/video23.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(23),
                        "Juan Pérez",
                        List.of(
                                new Question("How do you say 'I use my phone every day' in Spanish?",
                                        List.of("Uso mi teléfono todos los días", "Tengo mi teléfono cada día", "Llamo mi teléfono cada día", "Uso el teléfono siempre"), "Uso mi teléfono todos los días"),
                                new Question("¿Con qué frecuencia usas tu computadora?", "Some name")
                        ),
                        "https://example.com/extra_resource23",
                        "In this lesson, you’ll learn how to discuss technology and digital communication in Spanish.",
                        spanishA1Course
                )
        );

        List<Lection> japaneseA1Lections = List.of(
                new Lection(null,
                        "はじめまして (Hajimemashite – Nice to Meet You)",
                        "Learn basic greetings, polite introductions, and how to say your name and nationality using formal Japanese.",
                        "https://example.com/video1.mp4",
                        "Beginner",
                        LocalDate.now(),
                        "Yuki Tanaka",
                        List.of(
                                new Question("What is the Japanese word for 'Hello'?",
                                        List.of("こんにちは", "ありがとう", "さようなら", "おはよう"), "こんにちは"),
                                new Question("Introduce yourself in Japanese: 'My name is [name]. I am from [country].'", "Some name")
                        ),
                        "https://example.com/extra_resource1",
                        "In this lesson, you'll learn how to greet people, introduce yourself, and ask about others' origins. This lesson will help you make polite introductions in Japanese.",
                        japaneseA1Course
                ),

                new Lection(null,
                        "ひらがな・カタカナ入門 (Introduction to Hiragana & Katakana)",
                        "Get introduced to the two main Japanese writing systems, hiragana and katakana. Practice reading and writing simple words.",
                        "https://example.com/video2.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(2),
                        "Yuki Tanaka",
                        List.of(
                                new Question("Which is the correct hiragana for 'a'?",
                                        List.of("あ", "か", "さ", "た"), "あ"),
                                new Question("Write 'apple' in katakana.", "Some name")
                        ),
                        "https://example.com/extra_resource2",
                        "In this lesson, you'll learn about hiragana and katakana, the two essential Japanese writing systems. Practice reading and writing basic words using these characters.",
                        japaneseA1Course
                ),

                new Lection(null,
                        "あなたはどこの出身ですか？ (Where Are You From?)",
                        "Learn how to ask and say where someone is from using basic Japanese structures for countries and nationalities.",
                        "https://example.com/video3.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(3),
                        "Yuki Tanaka",
                        List.of(
                                new Question("What is the Japanese word for 'France'?",
                                        List.of("フランス", "ドイツ", "アメリカ", "イギリス"), "フランス"),
                                new Question("Where are you from?", "Some name")
                        ),
                        "https://example.com/extra_resource3",
                        "This lesson teaches you how to ask where someone is from and how to answer. You will learn country names and nationalities in Japanese.",
                        japaneseA1Course
                ),

                new Lection(null,
                        "数字と年齢 (Numbers and Age)",
                        "Learn numbers from 0 to 100, how to ask about age, phone numbers, and basic math expressions.",
                        "https://example.com/video4.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(4),
                        "Yuki Tanaka",
                        List.of(
                                new Question("How do you say 'twentythree' in Japanese?",
                                        List.of("にじゅうさん", "さんじゅう", "いちじゅう", "ごじゅう"), "にじゅうさん"),
                                new Question("How old are you?", "Some name")
                        ),
                        "https://example.com/extra_resource4",
                        "In this lesson, you will learn how to say and write numbers in Japanese, how to ask someone’s age, and how to express your own age.",
                        japaneseA1Course
                ),

                new Lection(null,
                        "曜日と日付 (Days of the Week and Dates)",
                        "Master the days of the week, months, and how to express dates, birthdays, and other calendarrelated vocabulary.",
                        "https://example.com/video5.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(5),
                        "Yuki Tanaka",
                        List.of(
                                new Question("What is the Japanese word for 'Sunday'?",
                                        List.of("日曜日", "月曜日", "火曜日", "水曜日"), "日曜日"),
                                new Question("When is your birthday?", "Some name")
                        ),
                        "https://example.com/extra_resource5",
                        "In this lesson, you'll learn the days of the week, months, and how to say and ask about dates and birthdays in Japanese.",
                        japaneseA1Course
                ),

                new Lection(null,
                        "いま何時ですか？ (What Time Is It Now?)",
                        "Learn how to ask and tell the time in Japanese, and practice talking about daily activities related to time.",
                        "https://example.com/video6.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(6),
                        "Yuki Tanaka",
                        List.of(
                                new Question("How do you ask for the time in Japanese?",
                                        List.of("今何時ですか？", "今日は何日ですか？", "どこですか？", "何時に行きますか？"), "今何時ですか？"),
                                new Question("What time do you wake up?", "Some name")
                        ),
                        "https://example.com/extra_resource6",
                        "This lesson will help you learn how to tell the time in Japanese and discuss daily schedules using timerelated expressions.",
                        japaneseA1Course
                ),

                new Lection(null,
                        "わたしの家族 (My Family)",
                        "Talk about family members, describe relationships, and practice using possessive pronouns like 'my' and 'your'.",
                        "https://example.com/video7.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(7),
                        "Yuki Tanaka",
                        List.of(
                                new Question("What is the Japanese word for 'mother'?",
                                        List.of("お母さん", "お父さん", "姉", "兄"), "お母さん"),
                                new Question("Describe your family.", "Some name")
                        ),
                        "https://example.com/extra_resource7",
                        "In this lesson, you'll learn how to describe your family and relationships using vocabulary like mother, father, sister, etc.",
                        japaneseA1Course
                ),

                new Lection(null,
                        "わたしの一日 (My Daily Routine)",
                        "Describe your daily routine using basic verbs and time expressions.",
                        "https://example.com/video8.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(8),
                        "Yuki Tanaka",
                        List.of(
                                new Question("How do you say 'I eat breakfast at 7 AM' in Japanese?",
                                        List.of("朝7時に朝食を食べます", "朝食を食べます7時に", "7時に朝食を食べます", "食べます朝7時"), "朝7時に朝食を食べます"),
                                new Question("Describe your daily routine.", "Some name")
                        ),
                        "https://example.com/extra_resource8",
                        "This lesson will help you describe your daily activities using simple verbs and time expressions like 'in the morning' and 'at night'.",
                        japaneseA1Course
                ),

                new Lection(null,
                        "わたしの家 (My Home)",
                        "Learn vocabulary for different rooms and furniture. Practice describing where things are in your house.",
                        "https://example.com/video9.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(9),
                        "Yuki Tanaka",
                        List.of(
                                new Question("Where do you sleep?",
                                        List.of("寝室", "リビング", "台所", "浴室"), "寝室"),
                                new Question("Describe your living room.", "Some name")
                        ),
                        "https://example.com/extra_resource9",
                        "This lesson covers vocabulary for rooms and furniture in your house. You will also practice describing the location of things.",
                        japaneseA1Course
                ),

                new Lection(null,
                        "たべものと飲みもの (Food and Drinks)",
                        "Learn foodrelated vocabulary, how to order at a restaurant, and express preferences.",
                        "https://example.com/video10.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(10),
                        "Yuki Tanaka",
                        List.of(
                                new Question("What is the Japanese word for 'coffee'?",
                                        List.of("コーヒー", "お茶", "ジュース", "水"), "コーヒー"),
                                new Question("Order a drink at a café.", "Some name")
                        ),
                        "https://example.com/extra_resource10",
                        "In this lesson, you will learn food and drink vocabulary and practice ordering in a restaurant. You will also learn to express likes and dislikes.",
                        japaneseA1Course
                )

        );


        List<Lection> russianA1Lections = List.of(
                new Lection(
                        null,
                        "Привет и добро пожаловать!",
                        "Изучите основные приветствия, как представиться и как спросить имя и страну происхождения. Попрактикуйтесь в простых диалогах для первых встреч.",
                        "https://example.com/video1.mp4",
                        "Beginner",
                        LocalDate.now(),
                        "Иван Иванов",
                        List.of(
                                new Question("Как порусски сказать 'Hi'?",
                                        List.of("Привет", "Здравствуй", "До свидания", "Спасибо"), "Привет"),
                                new Question("Представьтесь на русском языке: 'Меня зовут [имя]. Я из [страна].'", "Some name")
                        ),
                        "https://example.com/extra_resource1",
                        "Этот урок посвящен приветствиям и представлениям. Вы научитесь говорить 'привет', спрашивать имя и представляться с использованием простых выражений. Мы также рассмотрим, как спросить, откуда человек, и рассказать о себе.",
                        russianA1Course
                ),

                new Lection(
                        null,
                        "Русский алфавит и произношение",
                        "Познакомьтесь с русским алфавитом, основными правилами произношения и как произносить ваше имя и другие базовые слова.",
                        "https://example.com/video2.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(2),
                        "Иван Иванов",
                        List.of(
                                new Question("Какая буква идет после 'Б' в русском алфавите?",
                                        List.of("В", "Г", "А", "Е"), "В"),
                                new Question("Произнесите ваше имя порусски.", "Some name")
                        ),
                        "https://example.com/extra_resource2",
                        "В этом уроке вы узнаете русский алфавит и как правильно произносить буквы. Вы также научитесь произносить ваше имя и другие распространенные слова. Особое внимание уделено произношению.",
                        russianA1Course
                ),
                new Lection(
                        null,
                        "Откуда ты?",
                        "Говорите о странах и национальностях. Учитесь задавать и отвечать на вопросы типа 'Откуда ты?' и 'Какая у тебя национальность?'",
                        "https://example.com/video3.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(3),
                        "Иван Иванов",
                        List.of(
                                new Question("Какая национальность у человека из Франции?",
                                        List.of("Француз", "Испанец", "Немец", "Итальянец"), "Француз"),
                                new Question("Откуда ты?", "Some name")
                        ),
                        "https://example.com/extra_resource3",
                        "В этом уроке вы научитесь спрашивать о национальности и стране происхождения. Вы будете практиковать использование выражения 'Откуда ты?' и правильно отвечать на этот вопрос. Будет представлена лексика, связанная с национальностями и странами.",
                        russianA1Course
                ),

                new Lection(
                        null,
                        "Числа и возраст",
                        "Изучите числа от 0 до 100, как спрашивать возраст, номера телефонов и основные математические выражения.",
                        "https://example.com/video4.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(4),
                        "Иван Иванов",
                        List.of(
                                new Question("Как порусски сказать '45'?",
                                        List.of("Сорок пять", "Пять сорок", "Сорокпять", "Пять сорок"), "Сорок пять"),
                                new Question("Сколько тебе лет?", "Some name")
                        ),
                        "https://example.com/extra_resource4",
                        "В этом уроке мы рассмотрим числа на русском языке от 0 до 100. Вы научитесь спрашивать чейто возраст, говорить о своем возрасте и обсуждать номера телефонов. Также будут рассмотрены основные математические выражения.",
                        russianA1Course
                ),

                new Lection(
                        null,
                        "Дни недели, месяцы и даты",
                        "Выучите дни недели, месяцы года, как говорить и спрашивать о датах и днях рождения.",
                        "https://example.com/video5.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(5),
                        "Иван Иванов",
                        List.of(
                                new Question("Какой день недели идет после пятницы?",
                                        List.of("Понедельник", "Вторник", "Суббота", "Воскресенье"), "Суббота"),
                                new Question("Когда твой день рождения?", "Some name")
                        ),
                        "https://example.com/extra_resource5",
                        "В этом уроке вы научитесь называть дни недели и месяцы года. Вы научитесь задавать и отвечать на вопросы о датах, днях рождения и важных событиях.",
                        russianA1Course
                ),

                new Lection(
                        null,
                        "Какое время?",
                        "Научитесь говорить и спрашивать время на русском, узнаете слова, связанные с временем, и потренируетесь рассказывать о своем ежедневном расписании.",
                        "https://example.com/video6.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(6),
                        "Иван Иванов",
                        List.of(
                                new Question("Как порусски спросить время?",
                                        List.of("Который час?", "Что по времени?", "Какое время?", "Во сколько?"), "Который час?"),
                                new Question("Во сколько ты просыпаешься?", "Some name")
                        ),
                        "https://example.com/extra_resource6",
                        "Научитесь говорить время порусски и спрашивать, который час. Также будут представлены выражения, связанные с различными частями дня, и вы будете практиковаться в обсуждении своего ежедневного распорядка.",
                        russianA1Course
                ),

                new Lection(
                        null,
                        "Моя семья и друзья",
                        "Говорите о членах семьи и отношениях. Изучите лексику для описания ролей и внешности людей.",
                        "https://example.com/video7.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(7),
                        "Иван Иванов",
                        List.of(
                                new Question("Как называется сестра твоей матери?",
                                        List.of("Тётя", "Дядя", "Кузина", "Брат"), "Тётя"),
                                new Question("Опишите вашего лучшего друга.", "Some name")
                        ),
                        "https://example.com/extra_resource7",
                        "Этот урок посвящен лексике для членов семьи (мать, отец и т. д.) и друзей. Вы научитесь описывать внешность людей и их отношения, а также попрактикуетесь в использовании притяжательных местоимений.",
                        russianA1Course
                ),

                new Lection(
                        null,
                        "Один день из моей жизни",
                        "Опишите свой повседневный распорядок с использованием настоящего простого времени. Изучите обычные глаголы и выражения времени для повседневных действий.",
                        "https://example.com/video8.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(8),
                        "Иван Иванов",
                        List.of(
                                new Question("Как сказать 'Я обедаю в 12 часов' на русском?",
                                        List.of("Я обедаю в 12 часов.", "Я обедаю в 12", "Я обедал в 12 часов.", "Я буду обедать в 12 часов."), "Я обедаю в 12 часов."),
                                new Question("Опишите свой ежедневный распорядок.", "Some name")
                        ),
                        "https://example.com/extra_resource8",
                        "Научитесь описывать свои повседневные действия с использованием настоящего простого времени. Вы будете практиковаться в рассказе о своем расписании с использованием временных выражений, таких как 'утром', 'вечером' и 'каждый день'.",
                        russianA1Course
                ),

                new Lection(
                        null,
                        "Мой дом и квартира",
                        "Изучите лексику для комнат и мебели. Опишите, где вы живете, и расскажите о своем доме.",
                        "https://example.com/video9.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(9),
                        "Иван Иванов",
                        List.of(
                                new Question("Где вы спите?",
                                        List.of("На кухне", "В гостиной", "В спальне", "В ванной"), "В спальне"),
                                new Question("Опишите вашу гостиную.", "Some name")
                        ),
                        "https://example.com/extra_resource9",
                        "Этот урок посвящен лексике для различных комнат и мебели в доме. Вы научитесь описывать свою жилую среду и рассказывать, где вы живете.",
                        russianA1Course
                ),

                new Lection(
                        null,
                        "Что мы будем есть?",
                        "Изучите лексику для еды и напитков, попрактикуйтесь в заказе пищи в кафе или ресторане и поговорите о своих любимых блюдах.",
                        "https://example.com/video10.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(10),
                        "Иван Иванов",
                        List.of(
                                new Question("Как порусски сказать 'спагетти'?",
                                        List.of("Спагетти", "Паста", "Бургер", "Пицца"), "Спагетти"),
                                new Question("Закажите кофе в кафе.", "Some name")
                        ),
                        "https://example.com/extra_resource10",
                        "В этом уроке вы узнаете лексику, связанную с едой, как заказать еду в ресторане, а также как рассказать о своих любимых блюдах. Мы также обсудим вежливые выражения при ужине.",
                        russianA1Course
                ),

                new Lection(
                        null,
                        "Как добраться до города?",
                        "Учите описывать места в городе, спрашивать и давать направления, а также понимать лексику, связанную с транспортом.",
                        "https://example.com/video11.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(11),
                        "Иван Иванов",
                        List.of(
                                new Question("Где находится почта?",
                                        List.of("На главной улице", "В парке", "За магазином", "На площади"), "На главной улице"),
                                new Question("Как добраться до ближайшей автобусной остановки?", "Some name")
                        ),
                        "https://example.com/extra_resource11",
                        "В этом уроке вы научитесь спрашивать и давать направления порусски. Мы будем использовать лексику, связанную с транспортом, местами в городе, и практиковаться в использовании предлогов для местоположения.",
                        russianA1Course
                ),

                new Lection(
                        null,
                        "Чем ты занимаешься?",
                        "Говорите о профессиях и о том, что вы или другие люди делаете для жизни. Практикуйте использование 'быть' и 'делать' в простых вопросах.",
                        "https://example.com/video12.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(12),
                        "Иван Иванов",
                        List.of(
                                new Question("Какая профессия у человека, который учит студентов?",
                                        List.of("Доктор", "Учитель", "Инженер", "Художник"), "Учитель"),
                                new Question("Чем ты занимаешься?", "Some name")
                        ),
                        "https://example.com/extra_resource12",
                        "В этом уроке вы научитесь говорить о профессиях. Вы будете практиковаться в вопросах типа 'Чем ты занимаешься?' и использовать глаголы 'быть' и 'делать' в контексте.",
                        russianA1Course
                ),

                new Lection(
                        null,
                        "Школьная жизнь",
                        "Обсуждайте школьные предметы, школьные принадлежности и типичные школьные рутины. Учитесь описывать ваше учебное окружение.",
                        "https://example.com/video13.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(13),
                        "Иван Иванов",
                        List.of(
                                new Question("Какой предмет используется для письма?",
                                        List.of("Карандаш", "Ластик", "Книга", "Бумага"), "Карандаш"),
                                new Question("Какой твой любимый школьный предмет?", "Some name")
                        ),
                        "https://example.com/extra_resource13",
                        "В этом уроке вы будете обсуждать вашу школьную жизнь, говорить о различных предметах и описывать школьную рутину. Вы также узнаете лексику, связанную с классом, и общие фразы.",
                        russianA1Course
                ),

                new Lection(
                        null,
                        "Хобби и свободное время",
                        "Говорите о том, что вам нравится делать в свободное время, выражайте предпочтения, делайте простые приглашения и ответы.",
                        "https://example.com/video14.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(14),
                        "Иван Иванов",
                        List.of(
                                new Question("Какое ваше любимое хобби?",
                                        List.of("Чтение", "Сон", "Просмотр телевизора", "Спортивные занятия"), "Чтение"),
                                new Question("Пригласите когото пойти в кино.", "Some name")
                        ),
                        "https://example.com/extra_resource14",
                        "Этот урок поможет вам говорить о ваших хобби и свободных времени. Вы научитесь выражать предпочтения и делать простые приглашения или ответы для разных занятий.",
                        russianA1Course
                ),

                new Lection(
                        null,
                        "В магазине",
                        "Изучите лексику для одежды и покупок, практикуйтесь в запросах на размеры, цены и совершении покупок.",
                        "https://example.com/video15.mp4",
                        "Beginner",
                        LocalDate.now().plusDays(15),
                        "Иван Иванов",
                        List.of(
                                new Question("Как спросить, сколько стоит рубашка?",
                                        List.of("Сколько стоит эта рубашка?", "Какая цена этой рубашки?", "Рубашка сколько стоит?", "Сколько эта рубашка?"), "Сколько стоит эта рубашка?"),
                                new Question("Какую одежду вы предпочитаете носить?", "Some name")
                        ),
                        "https://example.com/extra_resource15",
                        "В этом уроке вы будете учить лексику для покупок в магазине, включая одежду, размеры и цены. Вы также научитесь спрашивать стоимость вещей и предлагать покупки.",
                        russianA1Course
                )
        );


        for (Lection lection : englishA1Lections) {
            lection.setCourse(englishA1Course);
            for (Question q : lection.getQuestions()) {
                q.setLection(lection);
            }
        }
        for (Lection lection : germanA1Lections) {
            lection.setCourse(germanA1Course);
            for (Question q : lection.getQuestions()) {
                q.setLection(lection);
            }
        }
        for (Lection lection : italianA1Lections) {
            lection.setCourse(italianA1Course);
            for (Question q : lection.getQuestions()) {
                q.setLection(lection);
            }
        }
        for (Lection lection : spanishA1Lections) {
            lection.setCourse(spanishA1Course);
            for (Question q : lection.getQuestions()) {
                q.setLection(lection);
            }
        }
        for (Lection lection : japaneseA1Lections) {
            lection.setCourse(japaneseA1Course);
            for (Question q : lection.getQuestions()) {
                q.setLection(lection);
            }
        }
        for (Lection lection : russianA1Lections) {
            lection.setCourse(russianA1Course);
            for (Question q : lection.getQuestions()) {
                q.setLection(lection);
            }
        }

        lectionRepository.saveAll(englishA1Lections);
        lectionRepository.saveAll(germanA1Lections);
        lectionRepository.saveAll(italianA1Lections);
        lectionRepository.saveAll(spanishA1Lections);
        lectionRepository.saveAll(japaneseA1Lections);
        lectionRepository.saveAll(russianA1Lections);

        FinalExam englishA1Exam = new FinalExam(
                englishA1Course,
                List.of(
                        new Question("Преведете на английски: 'Аз се казвам Мария и уча английски език.'", "My name is Maria and I study English."),
                        new Question("Съставете изречение с тези думи: 'I / go / to / school / every / day'", "I go to school every day."),
                        new Question("Напишете кратко представяне на себе си на английски.", "My name is [Name]. I am [Age] years old. I am from [Country]."),
                        new Question("Опишете вашия дом на английски език.", "My home is small. It has two bedrooms and a kitchen."),
                        new Question("Разкажете за вашето хоби на английски.", "I like reading books and playing football."),
                        new Question("Как прекарвате свободното си време? Отговорете на английски.", "I spend my free time watching movies or walking in the park."),
                        new Question("Опишете ваш приятел на английски.", "My friend is kind. He is tall and has black hair."),
                        new Question("Какво обичате да ядете за закуска? Напишете на английски.", "I like to eat eggs and toast for breakfast."),
                        new Question("Опишете вашата училищна стая на английски.", "There are 20 desks and a whiteboard."),
                        new Question("Какъв е вашият любим ден от седмицата и защо? Отговорете на английски.", "My favorite day is Saturday because I can relax."),
                        new Question("Опишете времето днес на английски.", "It is sunny and warm today."),
                        new Question("Какво правите след училище? Отговорете на английски.", "I do my homework and then play outside."),
                        new Question("Разкажете за пътуване, което сте правили. Напишете на английски.", "I went to the mountains last summer."),
                        new Question("Какви животни харесвате? Отговорете на английски.", "I like cats and dogs."),
                        new Question("Какъв е вашият любим предмет в училище и защо? Напишете на английски.", "My favorite subject is English because it is fun."),
                        new Question("Попълнете пропуските: 'My name _____ John. I _____ 25 years old.'", List.of("is, am", "am, is", "are, is"), "is, am"),
                        new Question("Изберете правилния отговор: 'What is your name?'", List.of("I am fine", "My name is John", "I am 25"), "My name is John"),
                        new Question("Попълнете: 'I _____ a student.'", List.of("am", "is", "are"), "am"),
                        new Question("Изберете: 'She _____ a teacher.'", List.of("are", "is", "am"), "is"),
                        new Question("Кое е правилно: 'I ___ coffee.'", List.of("drink", "drinks", "drank"), "drink"),
                        new Question("Попълнете: 'He _____ to school every day.'", List.of("go", "goes", "gone"), "goes"),
                        new Question("Изберете правилното местоимение: '_____ is my sister.'", List.of("He", "She", "It"), "She"),
                        new Question("Попълнете: 'We _____ happy today.'", List.of("is", "are", "am"), "are"),
                        new Question("Изберете правилния отговор: 'How old are you?'", List.of("I am 20", "I like dogs", "I live in Sofia"), "I am 20"),
                        new Question("Попълнете: 'They _____ at home now.'", List.of("is", "are", "am"), "are"),
                        new Question("Кое е правилно: 'You _____ late.'", List.of("is", "are", "am"), "are"),
                        new Question("Попълнете: 'The cat _____ on the sofa.'", List.of("is", "are", "am"), "is"),
                        new Question("Изберете: 'It is _____ apple.'", List.of("a", "an", "the"), "an"),
                        new Question("Попълнете: 'This is _____ book.'", List.of("my", "mine", "me"), "my"),
                        new Question("Изберете правилното време: 'I _____ breakfast at 8.'", List.of("eat", "ate", "eats"), "eat")
                ),
                "Напишете кратко есе за това как прекарвате типичен ден в училище на английски език."
        );


        for (Question q : englishA1Exam.getExamQuestions()) {
            q.setFinalExam(englishA1Exam);
        }


        englishA1Course.setFinalExam(englishA1Exam);


        finalExamRepository.save(englishA1Exam);


        courseRepository.save(englishA1Course);


        FinalExam germanA1Exam = new FinalExam(
                germanA1Course,
                List.of(
                        new Question("Übersetzen Sie ins Deutsche: 'Ich heiße Maria und lerne Deutsch.'", "Mein Name ist Maria und ich lerne Deutsch."),
                        new Question("Bilden Sie einen Satz mit diesen Wörtern: 'Ich / gehe / zur / Schule / jeden / Tag'", "Ich gehe zur Schule jeden Tag"),
                        new Question("Schreiben Sie eine kurze Selbstvorstellung auf Deutsch.", "Ich heiße [Name], bin [Alter] Jahre alt und komme aus [Land]."),
                        new Question("Beschreiben Sie Ihr Zuhause auf Deutsch.", "Meine Wohnung ist klein. Sie hat zwei Schlafzimmer und eine Küche."),
                        new Question("Erzählen Sie über Ihr Hobby auf Deutsch.", "Ich lese gerne Bücher und spiele Fußball."),
                        new Question("Was machen Sie in Ihrer Freizeit? Schreiben Sie auf Deutsch.", "In meiner Freizeit schaue ich Filme oder gehe spazieren."),
                        new Question("Beschreiben Sie einen Freund oder eine Freundin auf Deutsch.", "Mein Freund ist nett. Er ist groß und hat schwarze Haare."),
                        new Question("Was essen Sie gerne zum Frühstück? Schreiben Sie auf Deutsch.", "Ich esse gerne Eier und Toast zum Frühstück."),
                        new Question("Beschreiben Sie Ihr Klassenzimmer auf Deutsch.", "Es gibt 20 Tische und ein Whiteboard."),
                        new Question("Was ist Ihr Lieblingstag der Woche und warum? Schreiben Sie auf Deutsch.", "Mein Lieblingstag ist Samstag, weil ich mich entspannen kann."),
                        new Question("Wie ist das Wetter heute? Schreiben Sie auf Deutsch.", "Heute ist es sonnig und warm."),
                        new Question("Was machen Sie nach der Schule? Schreiben Sie auf Deutsch.", "Ich mache meine Hausaufgaben und spiele dann draußen."),
                        new Question("Erzählen Sie von einer Reise, die Sie gemacht haben. Schreiben Sie auf Deutsch.", "Ich war letzten Sommer in den Bergen."),
                        new Question("Welche Tiere mögen Sie? Schreiben Sie auf Deutsch.", "Ich mag Katzen und Hunde."),
                        new Question("Was ist Ihr Lieblingsfach in der Schule und warum? Schreiben Sie auf Deutsch.", "Mein Lieblingsfach ist Deutsch, weil es Spaß macht."),
                        new Question("Ergänzen Sie die Lücken: 'Ich heiße ____ und ____ 25 Jahre alt.'", "Maria, bin"),
                        new Question("Wählen Sie die richtige Antwort: 'Wie heißt du?' -> a) Ich bin gut b) Ich heiße Maria c) Ich bin 25", "b"),
                        new Question("Ergänzen Sie: 'Ich _____ Schüler.'", "bin"),
                        new Question("Wählen Sie: 'Sie _____ Lehrerin.' -> a) sind b) ist c) bin", "b"),
                        new Question("Was ist richtig: 'Ich ___ Kaffee.' -> a) trinke b) trinkt c) trank", "a"),
                        new Question("Ergänzen Sie: 'Er _____ jeden Tag zur Schule.'", "geht"),
                        new Question("Wählen Sie das richtige Pronomen: '_____ ist meine Schwester.' -> a) Er b) Sie c) Es", "b"),
                        new Question("Ergänzen Sie: 'Wir _____ heute glücklich.'", "sind"),
                        new Question("Wählen Sie die richtige Antwort: 'Wie alt bist du?' -> a) Ich bin 20 b) Ich mag Hunde c) Ich wohne in Berlin", "a"),
                        new Question("Ergänzen Sie: 'Sie _____ jetzt zu Hause.'", "sind"),
                        new Question("Was ist richtig: 'Du _____ spät.' -> a) ist b) bist c) bin", "b"),
                        new Question("Ergänzen Sie: 'Die Katze _____ auf dem Sofa.'", "ist"),
                        new Question("Wählen Sie: 'Es ist _____ Apfel.' -> a) ein b) eine c) der", "a"),
                        new Question("Ergänzen Sie: 'Das ist _____ Buch.'", "mein"),
                        new Question("Wählen Sie das richtige Verb: 'Ich _____ um 8 Uhr Frühstück.' -> a) esse b) aß c) isst", "a")
                ),
                "Напишете кратко есе за това как прекарвате типичен ден в училище на немски език."
        );


        for (Question q : germanA1Exam.getExamQuestions()) {
            q.setFinalExam(germanA1Exam);
        }


        germanA1Course.setFinalExam(germanA1Exam);


        finalExamRepository.save(germanA1Exam);


        courseRepository.save(germanA1Course);

        FinalExam italianA1Exam = new FinalExam(
                italianA1Course,
                List.of(
                        new Question("Traduci in italiano: 'Mi chiamo Maria e studio italiano.'", "Mi chiamo Maria e studio italiano."),
                        new Question("Forma una frase con queste parole: 'Io / vado / a / scuola / ogni / giorno'", "Io vado a scuola ogni giorno"),
                        new Question("Scrivi una breve presentazione di te stesso in italiano.", "Mi chiamo [Nome], ho [età] anni e vengo da [Paese]."),
                        new Question("Descrivi la tua casa in italiano.", "La mia casa è piccola. Ha due camere da letto e una cucina."),
                        new Question("Parla del tuo hobby in italiano.", "Mi piace leggere libri e giocare a calcio."),
                        new Question("Come passi il tuo tempo libero? Scrivi in italiano.", "Nel mio tempo libero guardo film o faccio passeggiate."),
                        new Question("Descrivi un tuo amico o una tua amica in italiano.", "Il mio amico è gentile. È alto e ha i capelli neri."),
                        new Question("Cosa ti piace mangiare a colazione? Scrivi in italiano.", "Mi piace mangiare uova e pane tostato."),
                        new Question("Descrivi la tua aula in italiano.", "Ci sono 20 banchi e una lavagna."),
                        new Question("Qual è il tuo giorno preferito della settimana e perché? Scrivi in italiano.", "Il mio giorno preferito è il sabato perché posso rilassarmi."),
                        new Question("Com'è il tempo oggi? Scrivi in italiano.", "Oggi è soleggiato e caldo."),
                        new Question("Cosa fai dopo scuola? Scrivi in italiano.", "Faccio i compiti e poi gioco fuori."),
                        new Question("Parla di un viaggio che hai fatto. Scrivi in italiano.", "Sono andato in montagna l'estate scorsa."),
                        new Question("Quali animali ti piacciono? Scrivi in italiano.", "Mi piacciono i gatti e i cani."),
                        new Question("Qual è la tua materia preferita a scuola e perché? Scrivi in italiano.", "La mia materia preferita è italiano perché è divertente."),
                        new Question("Completa gli spazi: 'Mi chiamo ____ e ____ 25 anni.'", "Maria, ho"),
                        new Question("Scegli la risposta corretta: 'Come ti chiami?' -> a) Sto bene b) Mi chiamo Maria c) Ho 25 anni", "b"),
                        new Question("Completa: 'Io _____ uno studente.'", "sono"),
                        new Question("Scegli: 'Lei _____ un'insegnante.' -> a) sei b) è c) sono", "b"),
                        new Question("Qual è corretto: 'Io ___ il caffè.' -> a) bevo b) bevi c) beve", "a"),
                        new Question("Completa: 'Lui _____ a scuola ogni giorno.'", "va"),
                        new Question("Scegli il pronome corretto: '_____ è mia sorella.' -> a) Lui b) Lei c) Esso", "b"),
                        new Question("Completa: 'Noi _____ felici oggi.'", "siamo"),
                        new Question("Scegli la risposta corretta: 'Quanti anni hai?' -> a) Mi chiamo Anna b) Ho 20 anni c) Mi piace il calcio", "b"),
                        new Question("Completa: 'Loro _____ a casa adesso.'", "sono"),
                        new Question("Qual è corretto: 'Tu _____ in ritardo.' -> a) è b) sei c) sono", "b"),
                        new Question("Completa: 'Il gatto _____ sul divano.'", "è"),
                        new Question("Scegli: 'È _____ arancia.' -> a) un b) una c) il", "b"),
                        new Question("Completa: 'Questo è _____ libro.'", "il mio"),
                        new Question("Scegli il tempo corretto: 'Io _____ colazione alle 8.' -> a) faccio b) facevo c) fai", "a")
                ),
                "Напишете кратко есе за това как прекарвате типичен ден в училище на италиански език."
        );


        for (Question q : italianA1Exam.getExamQuestions()) {
            q.setFinalExam(italianA1Exam);
        }


        italianA1Course.setFinalExam(italianA1Exam);


        finalExamRepository.save(italianA1Exam);


        courseRepository.save(italianA1Course);


        FinalExam spanishA1Exam = new FinalExam(
                spanishA1Course,
                List.of(
                        new Question("Traduce al español: 'Me llamo María y estudio español.'", "Me llamo María y estudio español."),
                        new Question("Forma una oración con estas palabras: 'Yo / voy / a / la / escuela / todos / los / días'", "Yo voy a la escuela todos los días"),
                        new Question("Escribe una breve presentación sobre ti en español.", "Me llamo [Nombre], tengo [edad] años y soy de [país]."),
                        new Question("Describe tu casa en español.", "Mi casa es pequeña. Tiene dos habitaciones y una cocina."),
                        new Question("Habla de tu pasatiempo en español.", "Me gusta leer libros y jugar al fútbol."),
                        new Question("¿Qué haces en tu tiempo libre? Escríbelo en español.", "En mi tiempo libre veo películas o doy paseos."),
                        new Question("Describe a un amigo o una amiga en español.", "Mi amigo es simpático. Es alto y tiene el pelo negro."),
                        new Question("¿Qué te gusta desayunar? Escríbelo en español.", "Me gusta desayunar huevos y pan tostado."),
                        new Question("Describe tu salón de clases en español.", "Hay 20 pupitres y una pizarra."),
                        new Question("¿Cuál es tu día favorito de la semana y por qué? Escríbelo en español.", "Mi día favorito es el sábado porque puedo descansar."),
                        new Question("¿Cómo está el clima hoy? Escríbelo en español.", "Hoy hace sol y calor."),
                        new Question("¿Qué haces después de la escuela? Escríbelo en español.", "Hago la tarea y luego juego afuera."),
                        new Question("Habla de un viaje que hayas hecho. Escríbelo en español.", "Fui a la montaña el verano pasado."),
                        new Question("¿Qué animales te gustan? Escríbelo en español.", "Me gustan los gatos y los perros."),
                        new Question("¿Cuál es tu materia favorita y por qué? Escríbelo en español.", "Mi materia favorita es español porque es divertida."),
                        new Question("Completa los espacios: 'Me llamo ____ y ____ 25 años.'", "María, tengo"),
                        new Question("Elige la respuesta correcta: '¿Cómo te llamas?' -> a) Estoy bien b) Me llamo María c) Tengo 25 años", "b"),
                        new Question("Completa: 'Yo _____ estudiante.'", "soy"),
                        new Question("Elige: 'Ella _____ profesora.' -> a) eres b) es c) soy", "b"),
                        new Question("¿Cuál es correcto?: 'Yo ___ café.' -> a) bebo b) bebe c) bebes", "a"),
                        new Question("Completa: 'Él _____ a la escuela todos los días.'", "va"),
                        new Question("Elige el pronombre correcto: '_____ es mi hermana.' -> a) Él b) Ella c) Eso", "b"),
                        new Question("Completa: 'Nosotros _____ contentos hoy.'", "estamos"),
                        new Question("Elige la respuesta correcta: '¿Cuántos años tienes?' -> a) Tengo 20 b) Me llamo Ana c) Me gusta leer", "a"),
                        new Question("Completa: 'Ellos _____ en casa ahora.'", "están"),
                        new Question("¿Cuál es correcto?: 'Tú _____ tarde.' -> a) eres b) estás c) soy", "b"),
                        new Question("Completa: 'El gato _____ en el sofá.'", "está"),
                        new Question("Elige: 'Es _____ manzana.' -> a) un b) una c) el", "b"),
                        new Question("Completa: 'Este es _____ libro.'", "mi"),
                        new Question("Elige el verbo correcto: 'Yo _____ desayuno a las 8.' -> a) tomo b) tomas c) toma", "a")
                ),
                "Напишете кратко есе за това как прекарвате типичен ден в училище на испански език."
        );


        for (Question q : spanishA1Exam.getExamQuestions()) {
            q.setFinalExam(spanishA1Exam);
        }


        spanishA1Course.setFinalExam(spanishA1Exam);


        finalExamRepository.save(spanishA1Exam);


        courseRepository.save(spanishA1Course);


        FinalExam japaneseA1Exam = new FinalExam(
                japaneseA1Course,
                List.of(
                        new Question("ロシア語で「私の名前はマリアです。ロシア語を勉強しています。」を訳してください。", "Меня зовут Мария и я учу русский язык."),
                        new Question("次の単語を使って文を作ってください: 'わたし / は / まいにち / がっこう / に / いきます'", "わたしはまいにちがっこうにいきます"),
                        new Question("じこしょうかいを日本語で書いてください。", "わたしのなまえは[名前]です。[国]からきました。"),
                        new Question("あなたのへやについて日本語で書いてください。", "わたしのへやはちいさいです。ベッドとつくえがあります。"),
                        new Question("あなたのしゅみはなんですか？日本語で書いてください。", "わたしのしゅみはほんをよむことです。"),
                        new Question("ひまなとき、なにをしますか？日本語で書いてください。", "えいがをみたり、さんぽしたりします。"),
                        new Question("ともだちについて日本語で書いてください。", "ともだちはしんせつです。せがたかいです。"),
                        new Question("あさごはんに なにを たべますか？日本語で書いてください。", "たまごとトーストをたべます。"),
                        new Question("あなたのきょうしつについて日本語で書いてください。", "きょうしつにはつくえがにじゅうこあります。"),
                        new Question("すきなひはいつですか？なぜですか？日本語で書いてください。", "すきなひはどようびです。やすめるからです。"),
                        new Question("きょうのてんきはどうですか？日本語で書いてください。", "きょうははれです。あついです。"),
                        new Question("がっこうのあと、なにをしますか？日本語で書いてください。", "しゅくだいをして、そとであそびます。"),
                        new Question("りょこうについて日本語で書いてください。", "なつやすみにやまへいきました。"),
                        new Question("すきなどうぶつはなんですか？日本語で書いてください。", "ねこといぬがすきです。"),
                        new Question("すきな かもくは なんですか？なぜですか？日本語で書いてください。", "にほんごがすきです。たのしいからです。"),
                        new Question("空欄をうめてください:『わたしのなまえは____、わたしは____さいです。』", "マリア, にじゅうご"),
                        new Question("『おなまえはなんですか？』に正しくこたえるのはどれ？ a) げんきです b) わたしはマリアです c) 25さいです", "b"),
                        new Question("『わたしは がくせい ____。』 -> a) です b) ます c) に", "a"),
                        new Question("『せんせいは にほんじん ____。』 -> a) に b) です c) で", "b"),
                        new Question("『わたしは みずを ____。』 -> a) のみます b) のみ c) のむ", "a"),
                        new Question("『かれは まいにち がっこうに ____。』 -> a) いきます b) いきません c) いきたい", "a"),
                        new Question("『これは わたしの ともだちです。_____ は やさしいです。』 -> a) かれ b) かのじょ c) それ", "a"),
                        new Question("『わたしたちは いま とても _____。』 -> a) たのしい b) たのしみ c) たのしさ", "a"),
                        new Question("『なんさいですか？』の ただしいこたえは？ a) わたしはマリアです b) にじゅうごさいです c) ほんがすきです", "b"),
                        new Question("『かれらは いま うちに ____。』 -> a) います b) いきます c) みます", "a"),
                        new Question("『あなたは ちこくです。』に ただしい ひょうげんは？ -> a) わたしはちこくです b) あなたはちこくでしょ c) あなたはちこくです", "c"),
                        new Question("『ねこは ソファーのうえに ____。』 -> a) いる b) ある c) です", "a"),
                        new Question("『これは ____ りんごです。』 -> a) ひとつ b) いちご c) ひとつの", "c"),
                        new Question("『このほんは ____ です。』 -> a) わたしの b) わたし c) わたしが", "a"),
                        new Question("『あさ、ごはんを ____。』 -> a) たべます b) たべる c) たべた", "a")
                ),
                "Напишете кратко есе за това как прекарвате типичен ден в училище на японски език."
        );


        for (Question q : japaneseA1Exam.getExamQuestions()) {
            q.setFinalExam(japaneseA1Exam);
        }

        japaneseA1Course.setFinalExam(japaneseA1Exam);
        finalExamRepository.save(japaneseA1Exam);
        courseRepository.save(japaneseA1Course);

        FinalExam russianA1Exam = new FinalExam(
                russianA1Course,
                List.of(
                        new Question("Переведите на русский язык: 'Меня зовут Мария и я учу русский язык.'", "Меня зовут Мария и я учу русский язык."),
                        new Question("Составьте предложение из слов: 'Я / иду / в / школу / каждый / день'", "Я иду в школу каждый день"),
                        new Question("Напишите краткое представление о себе на русском языке.", "Меня зовут [Имя], мне [возраст] лет, я из [страна]."),
                        new Question("Опишите свою комнату на русском языке.", "Моя комната маленькая. В ней есть кровать и стол."),
                        new Question("Расскажите о своём хобби на русском языке.", "Я люблю читать книги и гулять."),
                        new Question("Что вы делаете в свободное время? Напишите на русском языке.", "Я смотрю фильмы или гуляю."),
                        new Question("Опишите своего друга или подругу на русском языке.", "Моя подруга добрая и весёлая. У неё светлые волосы."),
                        new Question("Что вы обычно едите на завтрак? Напишите на русском языке.", "Я ем яйца и хлеб."),
                        new Question("Опишите свой класс на русском языке.", "В классе 20 парт и доска."),
                        new Question("Какой ваш любимый день недели и почему? Напишите на русском языке.", "Мой любимый день — суббота, потому что можно отдохнуть."),
                        new Question("Какая сегодня погода? Напишите на русском языке.", "Сегодня солнечно и тепло."),
                        new Question("Что вы делаете после школы? Напишите на русском языке.", "Я делаю домашнее задание и играю."),
                        new Question("Напишите о путешествии, которое вы совершали.", "Летом я ездил в горы."),
                        new Question("Какие животные вам нравятся? Напишите на русском языке.", "Мне нравятся кошки и собаки."),
                        new Question("Какой ваш любимый школьный предмет и почему? Напишите на русском языке.", "Мне нравится русский язык, потому что он интересный."),
                        new Question("Заполните пропуски: 'Меня зовут ____ и мне ____ лет.'", "Мария, двадцать пять"),
                        new Question("Как правильно ответить на вопрос: 'Как тебя зовут?' -> a) Мне 25 лет b) Меня зовут Мария c) Я из Испании", "b"),
                        new Question("Дополните: 'Я _____ студент.'", "являюсь"),
                        new Question("Выберите правильный вариант: 'Он _____ учитель.' -> a) есть b) быть c) —", "—"),
                        new Question("Выберите правильное слово: 'Я _____ воду.' -> a) пью b) пьёт c) пьёшь", "a"),
                        new Question("Дополните: 'Он _____ в школу каждый день.'", "ходит"),
                        new Question("Выберите правильное местоимение: '_____ моя сестра.' -> a) Он b) Она c) Это", "b"),
                        new Question("Дополните: 'Мы сейчас _____ дома.'", "находимся"),
                        new Question("Выберите правильный ответ на вопрос: 'Сколько тебе лет?' -> a) Мне 25 b) Меня зовут Анна c) Я читаю книгу", "a"),
                        new Question("Дополните: 'Они сейчас _____ дома.'", "дома"),
                        new Question("Выберите правильный глагол: 'Ты _____ устал.' -> a) есть b) был c) выглядишь", "c"),
                        new Question("Дополните: 'Кошка _____ на диване.'", "сидит"),
                        new Question("Выберите: 'Это _____ яблоко.' -> a) одна b) одно c) однажды", "b"),
                        new Question("Дополните: 'Это _____ книга.'", "моя"),
                        new Question("Выберите правильный глагол: 'Я _____ завтрак в 8 утра.' -> a) ем b) ешь c) ест", "a")
                ),
                "Напишете кратко есе за това как прекарвате типичен ден в училище на руски език."
        );


        for (Question q : russianA1Exam.getExamQuestions()) {
            q.setFinalExam(russianA1Exam);
        }


        russianA1Course.setFinalExam(russianA1Exam);


        finalExamRepository.save(russianA1Exam);


        courseRepository.save(russianA1Course);

    }
}
