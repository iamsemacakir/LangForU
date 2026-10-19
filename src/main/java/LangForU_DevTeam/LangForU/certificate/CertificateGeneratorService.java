package LangForU_DevTeam.LangForU.certificate;

import LangForU_DevTeam.LangForU.security.encryption.EncryptionService;
import LangForU_DevTeam.LangForU.singUpForCourse.UserCourseRequest;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Сервизен клас, отговорен за генерирането на PDF сертификати за участници,
 * завършили успешно курсове. Използва библиотеката iTextPDF.
 * Кодът е преработен за по-добра структура, сигурност и производителност.
 */
@Service
public class CertificateGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(CertificateGeneratorService.class);

    private final EncryptionService encryptionService;
    private final BaseFont baseFont;
    private final BaseFont boldFont;
    private final byte[] logoBytes;

    /**
     * Конструктор за инжектиране на зависимости и инициализация на ресурси (шрифтове и лого).
     *
     * @param encryptionService Сервиз за криптиране/декриптиране.
     */
    public CertificateGeneratorService(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
        try {
            // Зареждаме шрифтовете веднъж при създаването на бийна, за да избегнем повторно четене.
            // Четат се от classpath, за да работят и когато приложението е пуснато като JAR (на хостинг).
            // IDENTITY_H позволява Unicode (кирилица), а EMBEDDED вгражда шрифта в PDF файла.
            this.baseFont = BaseFont.createFont("FreeSerif.otf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED,
                    BaseFont.CACHED, readClasspathBytes("static/fonts/FreeSerif.otf"), null);
            this.boldFont = BaseFont.createFont("FreeSerifBold.otf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED,
                    BaseFont.CACHED, readClasspathBytes("static/fonts/FreeSerifBold.otf"), null);
            this.logoBytes = readClasspathBytes("static/img/logo/logo.png");
        } catch (DocumentException | IOException e) {
            logger.error("Критична грешка: Неуспешно зареждане на PDF шрифтове. Сертификати няма да могат да бъдат генерирани.", e);
            throw new RuntimeException("Грешка при зареждане на шрифтове", e);
        }
    }

    /**
     * Главен метод, който приема заявка за курс и генерира сертификат.
     * Той извлича необходимите данни, декриптира ЕГН и извиква метода за създаване на PDF.
     *
     * @param request        Обект {@link UserCourseRequest}, съдържащ данни за потребителя и курса.
     * @param finalExamScore Резултат от финалния изпит (в момента не се използва в дизайна на сертификата).
     * @return Масив от байтове (byte[]), представляващ генерирания PDF файл, или null при грешка.
     */
    /**
     * Главен метод, който приема заявка за курс и генерира сертификат.
     * Той извлича необходимите данни, декриптира ЕГН и извиква метода за създаване на PDF.
     *
     * @param request        Обект {@link UserCourseRequest}, съдържащ данни за потребителя и курса.
     * @param finalExamScore Резултат от финалния изпит (в момента не се използва в дизайна на сертификата).
     * @return Масив от байтове (byte[]), представляващ генерирания PDF файл, или null при грешка.
     */
    public byte[] generateCertificateAsBytes(UserCourseRequest request, int finalExamScore) {
        try {
            String fullName = request.getUser().getName();
            // Декриптираме ЕГН-то точно преди да го използваме.
            String decryptedEgn = encryptionService.decrypt(request.getPIN());
            String courseName = request.getCourse().getLanguage();
            LocalDate startDate = request.getCourse().getStartDate();
            LocalDate endDate = request.getCourse().getEndDate();
            int totalHours = request.getCourse().getLections().size();

            // Извикване на основния метод за генериране с извлечените и декриптирани данни.
            return buildPdf(fullName, decryptedEgn, courseName, startDate, endDate, totalHours);
        } catch (Exception e) {
            // КОРИГИРАН КОД: Безопасна проверка преди достъп до данните на потребителя
            String userIdentifier = (request != null && request.getUser() != null) ? request.getUser().getEmail() : "unknown user";
            logger.error("Грешка при генериране на сертификат за потребител: {}", userIdentifier, e);
            return null; // Връщаме null, за да може контролерът да обработи грешката.
        }
    }

    /**
     * Създава и форматира PDF сертификат с подадените данни.
     *
     * @param fullName     Пълно име на участника.
     * @param decryptedEgn Декриптиран ЕГН на участника.
     * @param courseName   Име на курса.
     * @param startDate    Начална дата на курса.
     * @param endDate      Крайна дата на курса.
     * @param totalHours   Обща продължителност в учебни часове.
     * @return Масив от байтове (byte[]), представляващ генерирания PDF файл.
     * @throws DocumentException при грешки по време на създаването на PDF.
     * @throws IOException при грешки при зареждане на изображението за лого.
     */
    private byte[] buildPdf(String fullName, String decryptedEgn, String courseName,
                            LocalDate startDate, LocalDate endDate, int totalHours) throws DocumentException, IOException {

        Document document = new Document(PageSize.A4, 50, 50, 60, 60); // Създаване на А4 документ с полета.
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); // Поток за запис на PDF в паметта.
        PdfWriter writer = PdfWriter.getInstance(document, outputStream);
        document.open();

        // --- Дефиниране на шрифтове ---
        Font titleFont = new Font(boldFont, 24, Font.BOLD, new BaseColor(40, 40, 90));
        Font subFont = new Font(baseFont, 12, Font.NORMAL, new BaseColor(40, 40, 90));
        Font nameFont = new Font(boldFont, 28, Font.BOLD, new BaseColor(40, 40, 90));
        Font labelFont = new Font(baseFont, 10, Font.BOLD, BaseColor.DARK_GRAY);
        Font footerFont = new Font(baseFont, 10, Font.NORMAL, BaseColor.WHITE);

        // --- Лого ---
        Image logo = Image.getInstance(logoBytes);
        logo.scaleToFit(100, 100);
        logo.setAlignment(Image.ALIGN_CENTER);
        document.add(logo);

        // --- Основно съдържание ---
        document.add(createParagraph("СЕРТИФИКАТ ЗА УЧАСТИЕ", titleFont, Element.ALIGN_CENTER, 10));
        document.add(createParagraph("С настоящото се удостоверява, че", subFont, Element.ALIGN_CENTER, 15));
        document.add(createParagraph(fullName, nameFont, Element.ALIGN_CENTER, 15));

        String courseDetailsText = String.format(
                "е участвал(а) активно в курса по тема \"%s\", проведен в периода от %s до %s, с обща продължителност от %d учебни часа.",
                courseName,
                startDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                endDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                totalHours
        );
        Paragraph details = createParagraph(courseDetailsText, subFont, Element.ALIGN_CENTER, 25);
        details.setSpacingBefore(10);
        document.add(details);

        document.add(createParagraph("ЕГН: " + decryptedEgn, labelFont, Element.ALIGN_CENTER, 30));

        // --- Таблица за подписи и дата ---
        document.add(createFooterTable(labelFont));

        // --- Цветен футър в най-долната част ---
        drawPageFooter(writer, footerFont);

        document.close();
        return outputStream.toByteArray();
    }

    /**
     * Прочита файл от classpath (src/main/resources) като масив от байтове.
     */
    private static byte[] readClasspathBytes(String path) throws IOException {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return in.readAllBytes();
        }
    }

    /**
     * Помощен метод за създаване на параграф с определени атрибути.
     */
    private Paragraph createParagraph(String text, Font font, int alignment, float spacingAfter) {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(alignment);
        p.setSpacingAfter(spacingAfter);
        return p;
    }

    /**
     * Помощен метод за създаване на таблицата с подписи.
     */
    private PdfPTable createFooterTable(Font font) {
        PdfPTable footerTable = new PdfPTable(3);
        footerTable.setWidthPercentage(100);
        try {
            footerTable.setWidths(new float[]{1.5f, 1f, 1.5f});
        } catch (DocumentException e) {
            // Това няма да се случи с валидни стойности
            logger.error("Грешка при задаване на ширини на колони в таблица за подпис.", e);
        }

        // Лява колона
        PdfPCell leftCell = new PdfPCell(new Phrase("Подпис:\nД-р Иван Иванов\nРъководител", font));
        leftCell.setBorder(Rectangle.NO_BORDER);
        footerTable.addCell(leftCell);

        // Централна колона
        PdfPCell centerCell = new PdfPCell(new Phrase("УЧАСТНИК\n2025", font));
        centerCell.setBorder(Rectangle.NO_BORDER);
        centerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        footerTable.addCell(centerCell);

        // Дясна колона
        PdfPCell rightCell = new PdfPCell(new Phrase("Дата на издаване: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), font));
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        footerTable.addCell(rightCell);

        return footerTable;
    }

    /**
     * Помощен метод за изрисуване на цветния футър на страницата.
     */
    private void drawPageFooter(PdfWriter writer, Font font) {
        PdfContentByte canvas = writer.getDirectContentUnder();
        Rectangle rect = new Rectangle(0, 0, PageSize.A4.getWidth(), 50);
        rect.setBackgroundColor(new BaseColor(40, 40, 90));
        canvas.rectangle(rect);

        Phrase footerText = new Phrase("© 2025 Академия за Езици. Всички права запазени.", font);
        ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
                footerText,
                PageSize.A4.getWidth() / 2, 20, 0);
    }
}
