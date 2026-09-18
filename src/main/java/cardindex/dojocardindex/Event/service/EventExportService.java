package cardindex.dojocardindex.Event.service;

import cardindex.dojocardindex.Event.models.Event;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.Utils.BackgroundPageEvent;
import cardindex.dojocardindex.exceptions.ExportIOException;
import cardindex.dojocardindex.exceptions.IllegalEventOperationException;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class EventExportService {

    private final EventService eventService;
    private final UserService userService;

    @Autowired
    public EventExportService(EventService eventService, UserService userService) {
        this.eventService = eventService;
        this.userService = userService;
    }

    public void exportEventDetailsAsCsv(UUID eventId, HttpServletResponse response) {

        Event event = eventService.getEventById(eventId);
        Set<User> users = event.getUsers();

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition","attachment; filename=event_" + eventId + ".csv");

        try {
            OutputStream out = response.getOutputStream();
            out.write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF});


            PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));

            writer.println("Име,Фамилия,Дата на раждане,Категория,Тегло,Възраст,Степен,Мед. преглед");

            users.stream().forEach(u -> writer.printf("%s,%s,%s,%s,%f,%d,%s,%s%n",
                    u.getFirstName(),
                    u.getLastName(),
                    u.getBirthDate(),
                    u.getAgeGroup(),
                    u.getWeight(),
                    userService.calculateAge(u.getBirthDate()),
                    u.getReachedDegree(),
                    u.getMedicalExamsPassed()));

            writer.flush();
            writer.close();

        } catch (IOException e) {
            log.error("Генерирането на CSV файл за събитие: {} беше неуспешно!", eventId, e);
            throw new ExportIOException("Генерирането на PDF файл беще неуспешно!");
        }

    }

    public void exportEventDetailsAsPDF(UUID eventId, HttpServletResponse response) {

        Event event = eventService.getEventById(eventId);
        Set<User> users = event.getUsers();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=event_" + eventId + ".pdf");

        Document document = new Document(PageSize.A4);

        setPageBackground(response, document);
        document.open();

//        // Зареждане на шрифт с кирилица
        try (InputStream fontStream = getClass().getClassLoader().getResourceAsStream("fonts/Ubuntu-Regular.ttf")) {
            if (fontStream == null) {
                throw new ExportIOException("Шрифтът Ubuntu-Regular.ttf не е намерен!");
            }
            BaseFont baseFont = BaseFont.createFont("Ubuntu-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontStream.readAllBytes(), null);

            Font font = new Font(baseFont,10,Font.NORMAL);
            Font headerFont = new Font(baseFont, 10, Font.BOLD);
            Font logoFont = new Font(baseFont, 22,Font.BOLDITALIC);
            Font titleFont = new Font(baseFont, 16, Font.BOLD);
            Font subtitleFont = new Font(baseFont, 11, Font.BOLD);
            Font disclaimerFont = new Font(baseFont, 10, Font.NORMAL, Color.red);
            Font footerFont = new Font(baseFont, 8, Font.ITALIC);
//            addDojoName(logoFont, document);

            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setSpacingBefore(10f);
            headerTable.setSpacingAfter(10f);
            headerTable.setWidths(new float[]{1f, 3f});

            PdfPCell kanLogo =  new PdfPCell();
            kanLogo.setColspan(2);
//            kanLogo.setBorder(Rectangle.NO_BORDER);

            try (InputStream logoStream = getClass().getClassLoader().getResourceAsStream("static/images/KanLogo.jpg")) {
                if (logoStream == null) {
                    throw new ExportIOException("KAN лого не е намерено!");
                }
                com.lowagie.text.Image logo = com.lowagie.text.Image.getInstance(logoStream.readAllBytes());
                logo.scaleToFit(100, 100);
                logo.setAlignment(Element.ALIGN_CENTER);
                kanLogo.addElement(logo);
            }

            Paragraph logoText = new Paragraph("БЪЛГАРСКА ФЕДЕРАЦИЯ\n КИОКУШИН-КАН\n",titleFont);
            logoText.setAlignment(Element.ALIGN_CENTER);
            logoText.setSpacingAfter(10f);
            kanLogo.addElement(logoText);

            headerTable.addCell(kanLogo);

            PdfPCell emptyRow = new PdfPCell(new Phrase(" "));
            emptyRow.setColspan(2);
//            emptyRow.setBorder(Rectangle.NO_BORDER);
            emptyRow.setFixedHeight(18f);
            headerTable.addCell(emptyRow);

            headerTable.addCell(createCell("СЪСТЕЗАНИЕ:",subtitleFont));
            headerTable.addCell(createCell(event.getEventDescription(),subtitleFont));

            headerTable.addCell(createCell("МЯСТО:",subtitleFont));
            headerTable.addCell(createCell(event.getLocation(),subtitleFont));

            headerTable.addCell(createCell("ДАТА:",subtitleFont));
            headerTable.addCell(createCell(event.getStartDate().format(DateTimeFormatter.ofPattern("dd-MM-yyy ' г.'")),subtitleFont));

            PdfPCell titleRow = new PdfPCell(new Phrase("ЗАЯВКА ЗА УЧАСТИЕ",logoFont));
            titleRow.setColspan(2);
            titleRow.setPaddingBottom(10f);
            titleRow.setHorizontalAlignment(Element.ALIGN_CENTER);
            titleRow.setVerticalAlignment(Element.ALIGN_MIDDLE);
            headerTable.addCell(titleRow);

            headerTable.addCell(createCell("КЛУБ:",subtitleFont));
            headerTable.addCell(createCell("ДРАГОН ДОДЖО ДСД",subtitleFont));

            document.add(headerTable);


//            document.add(Chunk.NEWLINE);
//        document.add(new Paragraph("Събитие: " + event.getEventDescription(),titleFont));
//        document.add(new Paragraph(" Начало: " + event.getStartDate().format(DateTimeFormatter.ofPattern("dd-MM-yyy ' г.'")),subtitleFont));
//        document.add(new Paragraph("Място: " + event.getLocation(),subtitleFont));
            document.add(Chunk.NEWLINE);
//        document.add(new Paragraph("Списък с участниците от Драгон Доджо ДСД:",font));

            PdfPTable disclaimer = new PdfPTable(1);
            disclaimer.setWidthPercentage(100);
            disclaimer.setSpacingBefore(10f);
            disclaimer.setSpacingAfter(10f);

            String disclaimerContent = "*Заявката се попълва с подредени състезатели според годината на раждане !\n Започвате с родените през 2018 година и завършвате с мъже и жени.\n Пишете с еднакъв шрифт!";


            PdfPCell disclaimerCell = new PdfPCell(new Phrase(disclaimerContent,disclaimerFont));
            disclaimerCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            disclaimerCell.setPaddingBottom(5f);
            disclaimer.addCell(disclaimerCell);

            document.add(disclaimer);


            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);
            float[] columnWidths = new float[]{1f, 3f, 2f, 1f, 2f};
            table.setWidths(columnWidths);

            PdfPCell cellNum = createHeaderCell("№", headerFont);
            PdfPCell header1 = createHeaderCell("Име и Фамилия", headerFont);
            PdfPCell header3 = createHeaderCell("Град", headerFont);
            PdfPCell header4 = createHeaderCell("Тегло", headerFont);
            PdfPCell header5 = createHeaderCell("Дата на раждане", headerFont);

            addCellsToTable(table,cellNum,header1,header3,header4,header5);
// Добавяне на заглавията към таблицата
            List<User> sortedUsers = new ArrayList<>(users);
            sortedUsers.sort(Comparator.comparing(User::getBirthDate));


            AtomicInteger counter = new AtomicInteger(1);


            sortedUsers.forEach(u -> {

                int number = counter.getAndIncrement();

                String ageGroupDescription = u.getAgeGroup() != null ? u.getAgeGroup().getDescription() : " - ";
                LocalDate medicalExam = u.getMedicalExamsPassed();
                String medicalExamFormatter = medicalExam != null ? medicalExam.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : " - ";
                String birthdateFormatter = u.getBirthDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy ' г.'"));

                PdfPCell cellN = createCell(String.valueOf(number), font);
                PdfPCell cell1 = createCell(u.getFirstName() + ' ' + u.getLastName(), font);
                PdfPCell cell3 = createCell("Асеновград", font);
                PdfPCell cell5 = createCell(birthdateFormatter, font);
                PdfPCell cell4 = createCell((u.getWeight() + " кг."), font);

                addCellsToTable(table, cellN,
                        cell1,
                        cell3,
                        cell4,
                        cell5);
            });

            document.add(table);
            document.add(Chunk.NEWLINE);
            Paragraph spacer = new Paragraph();
            spacer.setSpacingBefore(10f);
            spacer.setSpacingAfter(10f);
            document.add(spacer);

            String footerContent = "С подписването на настоящата заявка, деклараирам, че всички участници ще спазват състезателния правилник на БФК, както и всички други нормативни документи, по които се провежда състезанието.";

            document.add(new Paragraph(footerContent,footerFont));

            PdfPTable footerTable = new PdfPTable(3);
            footerTable.setWidthPercentage(100);
            footerTable.setSpacingBefore(10f);
            footerTable.setSpacingAfter(10f);

            PdfPCell footerCell1 = new PdfPCell(new Phrase("Дата:",subtitleFont));
            footerCell1.setHorizontalAlignment(Element.ALIGN_LEFT);
            footerCell1.setPaddingBottom(5f);
            footerTable.addCell(footerCell1);

            PdfPCell footerCell2 = new PdfPCell(new Phrase("Град:",subtitleFont));
            footerCell2.setHorizontalAlignment(Element.ALIGN_LEFT);
            footerTable.addCell(footerCell2);

            PdfPCell footerCell3 = new PdfPCell(new Phrase("Подпис:",subtitleFont));
            footerCell3.setHorizontalAlignment(Element.ALIGN_LEFT);
            footerTable.addCell(footerCell3);

            document.add(footerTable);

            document.close();

        } catch (Exception e) {
            log.error("Генерирането на PDF файл за събитие: {} беще неуспешно!",eventId,e);
            throw new ExportIOException("Генерирането на PDF файл беще неуспешно!");
        }

    }

    public void exportPDFExamProtocolForUser(UUID eventId, UUID userId, HttpServletResponse response) {

        Event event = eventService.getEventById(eventId);
        User user = userService.getUserById(userId);

        response.setContentType("application/pdf");
        String fileName = URLEncoder.encode(user.getFirstName() + "_" + user.getLastName() + ".pdf", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);

        Document document = new Document(PageSize.A4);

        setPageBackground(response, document);

        document.open();

        try (InputStream fontStream = getClass().getClassLoader().getResourceAsStream("fonts/Ubuntu-Regular.ttf")) {
            if (fontStream == null) {
                throw new ExportIOException("Шрифтът Ubuntu-Regular.ttf не е намерен!");
            }

            BaseFont baseFont = BaseFont.createFont("Ubuntu-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontStream.readAllBytes(), null);
            Font defaultFont = new Font(baseFont,14,Font.NORMAL);
            Font nameFont = new Font(baseFont,20,Font.NORMAL);
            Font logoFont = new Font(baseFont,22,Font.BOLDITALIC);
            Font titleFont = new Font(baseFont,20,Font.BOLD);
            Font headerFont = new Font(baseFont,12,Font.BOLD);

            addDojoName(logoFont, document);

            document.add(Chunk.NEWLINE);

            Paragraph protocol = new Paragraph("ИЗПИТЕН ПРОТОКОЛ",titleFont);
            protocol.setAlignment(Element.ALIGN_CENTER);
            document.add(protocol);
            document.add(Chunk.NEWLINE);

            Paragraph name = new Paragraph(user.getFirstName() + " " + user.getLastName(),nameFont);
            name.setAlignment(Element.ALIGN_CENTER);
            document.add(name);
            document.add(new Paragraph("Дата на раждане: " + user.getBirthDate()
                    .format(DateTimeFormatter.ofPattern("dd-MM-yyyy ' г.'" + "              " + "Клуб: Драгон Доджо ДСД")),defaultFont));
            document.add(new Paragraph("Защитена степен: " + user.getReachedDegree().getDescription() + "                              " + "Дата на последен изпит:...............................",defaultFont));
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(3);
            PdfPCell header1 = createHeaderCell("KИХОН",headerFont);
            PdfPCell header2 = createHeaderCell("KATA",headerFont);
            PdfPCell header3 = createHeaderCell("KУМИТЕ",headerFont);

            addCellsToTable(table,header1,
                    header2,
                    header3);

            PdfPCell cell1 = createCell("",defaultFont);
            PdfPCell cell2 = createCell("",defaultFont);
            PdfPCell cell3 = createCell("",defaultFont);

            cell1.setFixedHeight(250f);
            cell2.setFixedHeight(250f);
            cell3.setFixedHeight(250f);

            addCellsToTable(table,cell1,cell2,cell3);

            document.add(table);

            document.add(Chunk.NEWLINE);

            document.add(new Paragraph("дата на изпита: "+ event.getStartDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy ' г.'")) + "                                        " + "Изпитен резултат:...............",defaultFont));

            document.close();


        } catch (IOException e) {
            log.error("Генерирането на PDF файл за събитие: {} беще неуспешно!",eventId,e);
            throw new ExportIOException("Генерирането на PDF файл беще неуспешно!");
        }

    }

    public void exportParentConsentConfirmNote(UUID eventId, UUID userId, HttpServletResponse response) {
        Event event = eventService.getEventById(eventId);
        User user = userService.getUserById(userId);

        if (userService.calculateAge(user.getBirthDate()) >= 18) {
            throw new IllegalEventOperationException("Потребителят е навършил 18 години и не се изисква родителско съгласие.");
        }

        response.setContentType("application/pdf");
        String fileName = URLEncoder.encode(user.getFirstName() + "_" + user.getLastName() + "_" + eventId + ".pdf", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);

        Document document = new Document(PageSize.A4);
        setPageBackground(response, document);
        document.open();

        try (InputStream fontStream = getClass().getClassLoader().getResourceAsStream("fonts/Ubuntu-Regular.ttf")) {
            if (fontStream == null) {
                throw new ExportIOException("Шрифтът Ubuntu-Regular.ttf не е намерен!");
            }

            BaseFont baseFont = BaseFont.createFont("Ubuntu-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontStream.readAllBytes(), null);
            Font defaultFont = new Font(baseFont, 12, Font.NORMAL);
            Font titleFont = new Font(baseFont, 16, Font.BOLD);
            Font logoFont = new Font(baseFont, 22, Font.BOLDITALIC);

            addDojoName(logoFont, document);
            document.add(Chunk.NEWLINE);

            Paragraph title = new Paragraph("ДЕКЛАРАЦИЯ ЗА РОДИТЕЛСКО СЪГЛАСИЕ", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(Chunk.NEWLINE);

            String parentName = (user.getContactPerson() != null && !user.getContactPerson().isBlank())
                    ? user.getContactPerson()
                    : "..................................................";

            String birthDateStr = user.getBirthDate() != null
                    ? user.getBirthDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy ' г.'"))
                    : "................";

            String content = "Аз " + parentName + ", родител/настойник на " + user.getFirstName() + " " + user.getLastName() +
                    ", роден/a на " + birthDateStr +
                    ", давам своето съгласие за участието му/и в: \"" + event.getEventDescription() +
                    "\", което ще се проведе на " + event.getStartDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy ' г.'")) +
                    " в " + event.getLocation() + ".\n\n" +
                    "С подписването на тази декларация потвърждавам, че съм запознат с правилата и условията на посоченото по-горе събитие, както и с евентуални рискове свързани с участието на детето ми в него.\n\n" +
                    "Дата: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy ' г.'")) + "\n\n" +
                    "Подпис на родител/настойник: ....................................................";

            // Задължително подаваме defaultFont, за да се изпише правилно кирилицата
            Paragraph mainText = new Paragraph(content, defaultFont);
            mainText.setAlignment(Element.ALIGN_JUSTIFIED);
            document.add(mainText);

            document.close();
        } catch (Exception e) {
            log.error("Грешка при генериране на родителско съгласие за потребител {} и събитие {}", userId, eventId, e);
            throw new ExportIOException("Генерирането на PDF файл беше неуспешно!");
        }
    }

    private static void addDojoName(Font logoFont, Document document) {
        Paragraph clubName = (new Paragraph("\"ДРАГОН ДОДЖО ДСД - Асеновград\"", logoFont));
        clubName.setAlignment(Element.ALIGN_CENTER);
        document.add(clubName);
        LineSeparator line = new LineSeparator();
        line.setPercentage(75f);
        line.setAlignment(Element.ALIGN_CENTER);
        line.setOffset(-9f);
        line.setLineWidth(3f);
        document.add(line);
    }

    private void addCellsToTable(PdfPTable table, PdfPCell... cells) {
        for (PdfPCell cell : cells) {
            table.addCell(cell);
        }
    }

    private PdfPCell createHeaderCell(String content , Font font) {

        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setBackgroundColor(new Color(186,210,232));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingBottom(5f);

        return cell;
    }

    private PdfPCell createCell(String content, Font font){

        PdfPCell cell = new PdfPCell(new Phrase(content,font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingBottom(5f);

        return cell;
    }

    private void setPageBackground(HttpServletResponse response, Document document) {
        try (InputStream backgroundStream = getClass().getClassLoader().getResourceAsStream("static/images/KAN_PDF_BACKGROUND.jpg")) {
            if (backgroundStream == null) {
                throw new ExportIOException("Фоновото изображение KAN_PDF_BACKGROUND.jpg не е намерено!");
            }
            com.lowagie.text.Image background = Image.getInstance(backgroundStream.readAllBytes());
            PdfWriter writer = PdfWriter.getInstance(document, response.getOutputStream());
            writer.setPageEvent(new BackgroundPageEvent(background,0.3f));
        } catch (IOException e) {
            log.error("Грешка при зареждане или задаване на фоново изображение за PDF!", e);
            throw new ExportIOException("Неуспешно инициализиране на фоновото изображение на PDF документа!");
        }
    }
}
