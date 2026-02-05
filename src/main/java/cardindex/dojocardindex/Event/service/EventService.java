package cardindex.dojocardindex.Event.service;


import cardindex.dojocardindex.Event.models.Event;
import cardindex.dojocardindex.Event.models.EventType;
import cardindex.dojocardindex.Event.repository.EventRepository;
import cardindex.dojocardindex.User.models.Degree;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.Utils.BackgroundPageEvent;
import cardindex.dojocardindex.exceptions.EventNotFoundException;
import cardindex.dojocardindex.exceptions.ExportIOException;
import cardindex.dojocardindex.exceptions.IllegalEventOperationException;
import cardindex.dojocardindex.web.dto.CreateEventRequest;
import cardindex.dojocardindex.web.dto.EditEventRequest;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Slf4j
@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserService userService;


    @Autowired
    public EventService(EventRepository eventRepository, UserService userService) {
        this.eventRepository = eventRepository;
        this.userService = userService;

    }

    public void addNewEvent(CreateEventRequest createEventRequest){

        Event event = Event.builder()
                .type(createEventRequest.getEventType())
                .EventDescription(createEventRequest.getEventDescription())
                .startDate(createEventRequest.getStartDate())
                .endDate(createEventRequest.getEndDate())
                .location(createEventRequest.getLocation())
                .requirements(createEventRequest.getRequirements())
                .closed(false)
                .result(false)
                .build();

        eventRepository.save(event);

    }

    public void closeEvent(UUID eventId){
        Event event = getEventById(eventId);

        event = event.toBuilder().closed(true).build();

        eventRepository.save(event);

    }

    public void showResultOnUpdateDetailsPage(UUID eventId){
        Event event = getEventById(eventId);

        if (event.getUsers().isEmpty()){

            throw new IllegalEventOperationException("Списъкът с участници за това събитие е празен. Няма резултати за визуализация.");
        }

        if (event.isResult()){
            throw new IllegalEventOperationException("Събитията вече са публикувани.");
        }

        event = event.toBuilder()
                .result(true)
                .build();

        eventRepository.save(event);
    }

    public void hideResultOnUpdateDetailsPage(UUID eventId){
        Event event = getEventById(eventId);

        if (!event.isResult()){
            throw new IllegalEventOperationException("Резултатите от това събитие все още не са публикувани.");
        }

        event = event.toBuilder()
                .result(false)
                .build();

        eventRepository.save(event);
    }

    //TODO Да добавя автоматично изпращане на мейл след обновявне на защитената степен!
    public void setExamResult(UUID eventId,UUID userId, Degree updatedDegree){

        Event event = getEventById(eventId);
        if (event.isResult()){
            throw new IllegalEventOperationException("Резултатите за това събитие вече са публикувани. " +
                    "Необходимо е да свалите резултатите преди да направите тази промяна.");
        }
        User user = userService.getUserById(userId);
        user = user.toBuilder()
                .reachedDegree(updatedDegree)
                .build();
        userService.saveUser(user);
    }

    //TODO Да добавя възможност за задаване на победителите в събитието или в едит или в отделен метод
    public void editEvent(UUID eventId, EditEventRequest editEventRequest){

        Event event = getEventById(eventId);

        event = event.toBuilder()
                .type(editEventRequest.getEventType())
                .EventDescription(editEventRequest.getEventDescription())
                .startDate(editEventRequest.getStartDate())
                .location(editEventRequest.getLocation())
                .endDate(editEventRequest.getEndDate())
                .requirements(editEventRequest.getRequirements())
                .build();

        eventRepository.save(event);
    }



    public Event getEventById(UUID eventId){
        return eventRepository.findById(eventId).orElseThrow(() ->new EventNotFoundException("Събитие с идентификация [%s] не съществува".formatted(eventId)));
    }

    public List<Event> getAllActiveEvents(){

        return eventRepository.findAllByClosedOrderByStartDate(false);

    }

    public List<Event> getUpcomingEvents(){

        return eventRepository.findAllByStartDateAfterAndClosed(LocalDate.now(),false, Limit.of(3),Sort.by(Sort.Order.by("startDate")));
    }


    @Transactional
    public void setWinner(UUID eventId, UUID userId, int place) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Събитието не е открито"));

        if (event.getType() != EventType.TOURNAMENT) {
            throw new IllegalStateException("Победители могат да бъдат задавани само в ТУРНИР");
        }

        User user = userService.getUserById(userId);

        if (!event.getUsers().contains(user)) {
            throw new IllegalArgumentException("Победителят трябва да бъде участник в събитието!");
        }

//        switch (place) {
//            case 1 -> updateWinner(event, event.getFirstPlaceWinner(), user,
//                    User::getAchievedFirstPlaces, User::setAchievedFirstPlaces);
//            case 2 -> updateWinner(event, event.getSecondPlaceWinner(), user,
//                    User::getAchievedSecondPlaces, User::setAchievedSecondPlaces);
//            case 3 -> updateWinner(event, event.getThirdPlaceWinner(), user,
//                    User::getAchievedThirdPlaces, User::setAchievedThirdPlaces);
//            default -> throw new IllegalArgumentException("Невалидна позиция. Позицията може да бъде 1, 2, или 3.");
//        }
        //Добавяме параметър place - който да използваме в updateWinner за по ясен код
        switch (place) {
            case 1 -> updateWinner(event, event.getFirstPlaceWinner(), user,
                    User::getAchievedFirstPlaces, User::setAchievedFirstPlaces, 1);
            case 2 -> updateWinner(event, event.getSecondPlaceWinner(), user,
                    User::getAchievedSecondPlaces, User::setAchievedSecondPlaces, 2);
            case 3 -> updateWinner(event, event.getThirdPlaceWinner(), user,
                    User::getAchievedThirdPlaces, User::setAchievedThirdPlaces, 3);
            default -> throw new IllegalArgumentException("Невалидна позиция. Позицията може да бъде 1, 2, или 3.");
        }


        eventRepository.save(event);
    }

    private void updateWinner(Event event, User oldWinner, User newWinner,
                              Function<User, Integer> getPlaceCount,
                              BiConsumer<User, Integer> setPlaceCount, int place) {

        if (oldWinner != null) {
            int newCount = Math.max(0, getPlaceCount.apply(oldWinner) - 1);
            setPlaceCount.accept(oldWinner, newCount);
        }

        if (newWinner != null) {
            setPlaceCount.accept(newWinner, getPlaceCount.apply(newWinner) + 1);
        }

//        int placeCount = getPlaceCount.apply(newWinner);
//        assert newWinner != null;
//        if (placeCount == newWinner.getAchievedFirstPlaces()) {
//            event.setFirstPlaceWinner(newWinner);
//        } else if (placeCount == newWinner.getAchievedSecondPlaces()) {
//            event.setSecondPlaceWinner(newWinner);
//        } else if (placeCount == newWinner.getAchievedThirdPlaces()) {
//            event.setThirdPlaceWinner(newWinner);
//        }

        // Актуализираме съответната позиция въз основа на параметъра 'place' - place се добавя като параметър на метода
        if (place == 1) {
            event.setFirstPlaceWinner(newWinner);
        } else if (place == 2) {
            event.setSecondPlaceWinner(newWinner);
        } else if (place == 3) {
            event.setThirdPlaceWinner(newWinner);
        }

    }


    @Transactional
    public void resetWinners(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Събитието не е открито"));

        if (event.getType() != EventType.TOURNAMENT) {
            throw new IllegalStateException("Победители могат да бъдат задавани само в ТУРНИР");
        }

        resetWinner(event, event.getFirstPlaceWinner(), User::getAchievedFirstPlaces, User::setAchievedFirstPlaces);
        resetWinner(event, event.getSecondPlaceWinner(), User::getAchievedSecondPlaces, User::setAchievedSecondPlaces);
        resetWinner(event, event.getThirdPlaceWinner(), User::getAchievedThirdPlaces, User::setAchievedThirdPlaces);

        event = event.toBuilder()
                .firstPlaceWinner(null)
                .secondPlaceWinner(null)
                .thirdPlaceWinner(null)
                .build();

        eventRepository.save(event);
    }

    private void resetWinner(Event event, User winner,
                             Function<User, Integer> getPlaceCount,
                             BiConsumer<User, Integer> setPlaceCount) {

        if (winner != null) {
            int newCount = Math.max(0, getPlaceCount.apply(winner) - 1);
            setPlaceCount.accept(winner, newCount);
        }
    }


    public void saveEvent(Event event){
        eventRepository.save(event);
    }

    public void exportEventDetailsAsCsv(UUID eventId, HttpServletResponse response) {

        Event event = getEventById(eventId);
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

        Event event = getEventById(eventId);
        Set<User> users = event.getUsers();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=event_" + eventId + ".pdf");

        Document document = new Document(PageSize.A4);

        try (InputStream backgroundStream = getClass().getClassLoader().getResourceAsStream("static/images/KAN_PDF_BACKGROUND.jpg")) {
            if (backgroundStream == null) {
                throw new ExportIOException("Background image не е намерено!");
            }
            Image background = Image.getInstance(backgroundStream.readAllBytes());
            PdfWriter  writer = PdfWriter.getInstance(document,response.getOutputStream());
            writer.setPageEvent(new BackgroundPageEvent(background,0.3f));
        } catch (IOException e) {
            log.error("Генерирането на PDF файл за събитие: {} беще неуспешно!",eventId,e);
            throw new ExportIOException("Генерирането на PDF файл беще неуспешно!");
        }
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
        Font disclaimerFont = new Font(baseFont, 10, Font.NORMAL,Color.red);
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
                Image logo = Image.getInstance(logoStream.readAllBytes());
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

        Event event = getEventById(eventId);
        User user = userService.getUserById(userId);

        response.setContentType("application/pdf");
        String fileName = URLEncoder.encode(user.getFirstName() + "_" + user.getLastName() + ".pdf", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);

        Document document = new Document(PageSize.A4);
        try (InputStream backgroundStream = getClass().getClassLoader().getResourceAsStream("static/images/Kan_PDF_background.jpg");
             InputStream fontStream = getClass().getClassLoader().getResourceAsStream("fonts/Ubuntu-Regular.ttf")) {

            if (backgroundStream == null) {
                throw new ExportIOException("Background image не е намерено!");
            }
            if (fontStream == null) {
                throw new ExportIOException("Шрифтът Ubuntu-Regular.ttf не е намерен!");
            }

            Image imageBackground = Image.getInstance(backgroundStream.readAllBytes());
            PdfWriter writer = PdfWriter.getInstance(document,response.getOutputStream());
            writer.setPageEvent(new BackgroundPageEvent(imageBackground,0.3f));

            document.open();

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

}
