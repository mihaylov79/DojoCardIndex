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
import java.util.List;
import java.util.Set;
import java.util.UUID;
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

        switch (place) {
            case 1 -> updateWinner(event, event.getFirstPlaceWinner(), user,
                    User::getAchievedFirstPlaces, User::setAchievedFirstPlaces);
            case 2 -> updateWinner(event, event.getSecondPlaceWinner(), user,
                    User::getAchievedSecondPlaces, User::setAchievedSecondPlaces);
            case 3 -> updateWinner(event, event.getThirdPlaceWinner(), user,
                    User::getAchievedThirdPlaces, User::setAchievedThirdPlaces);
            default -> throw new IllegalArgumentException("Невалидна позиция. Позицията може да бъде 1, 2, или 3.");
        }

        eventRepository.save(event);
    }

    private void updateWinner(Event event, User oldWinner, User newWinner,
                              Function<User, Integer> getPlaceCount,
                              BiConsumer<User, Integer> setPlaceCount) {

        if (oldWinner != null) {
            int newCount = Math.max(0, getPlaceCount.apply(oldWinner) - 1);
            setPlaceCount.accept(oldWinner, newCount);
        }

        if (newWinner != null) {
            setPlaceCount.accept(newWinner, getPlaceCount.apply(newWinner) + 1);
        }

        int placeCount = getPlaceCount.apply(newWinner);
        assert newWinner != null;
        if (placeCount == newWinner.getAchievedFirstPlaces()) {
            event.setFirstPlaceWinner(newWinner);
        } else if (placeCount == newWinner.getAchievedSecondPlaces()) {
            event.setSecondPlaceWinner(newWinner);
        } else if (placeCount == newWinner.getAchievedThirdPlaces()) {
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

            users.stream().forEach(u -> writer.printf("%s,%s,%s,%s,%d,%d,%s,%s%n",
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

        try {
            Image background = Image.getInstance("src/main/resources/static/images/KAN_PDF_BACKGROUND.jpg");
            PdfWriter  writer = PdfWriter.getInstance(document,response.getOutputStream());
            writer.setPageEvent(new BackgroundPageEvent(background,0.3f));
        } catch (IOException e) {
            log.error("Генерирането на PDF файл за събитие: {} беще неуспешно!",eventId,e);
            throw new ExportIOException("Генерирането на PDF файл беще неуспешно!");
        }
        document.open();

//        // Зареждане на шрифт с кирилица
        try {

//            InputStream fontStream = getClass().getClassLoader().getResourceAsStream("fonts/Ubuntu-Regular.ttf");
//            BaseFont baseFont = BaseFont.createFont("Ubuntu-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontStream.readAllBytes(), null);
//
//        Font font = new Font(baseFont, 12);
//        Font titleFont = new Font(baseFont, 18, Font.BOLD);
//        Font subtitleFont = new Font(baseFont, 14, Font.BOLD);

        BaseFont baseFont = BaseFont.createFont("src/main/resources/fonts/Ubuntu-Regular.ttf", BaseFont.IDENTITY_H,BaseFont.EMBEDDED);
        Font font = new Font(baseFont,10,Font.NORMAL);
        Font headerFont = new Font(baseFont, 8, Font.BOLD);
        Font logoFont = new Font(baseFont, 22,Font.BOLDITALIC);
        Font titleFont = new Font(baseFont, 16, Font.BOLD);
        Font subtitleFont = new Font(baseFont, 10, Font.BOLD);
            addDojoName(logoFont, document);

            document.add(Chunk.NEWLINE);
        document.add(new Paragraph("Събитие: " + event.getEventDescription(),titleFont));
        document.add(new Paragraph(" Начало: " + event.getStartDate().format(DateTimeFormatter.ofPattern("dd-MM-yyy ' г.'")),subtitleFont));
        document.add(new Paragraph("Място: " + event.getLocation(),subtitleFont));
        document.add(Chunk.NEWLINE);
        document.add(new Paragraph("Списък с участниците от Драгон Доджо ДСД:",font));

        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

            PdfPCell header1 = createHeaderCell("Име", headerFont);
            PdfPCell header2 = createHeaderCell("Фамилия", headerFont);
            PdfPCell header3 = createHeaderCell("Дата на раждане", headerFont);
            PdfPCell header4 = createHeaderCell("Група", headerFont);
            PdfPCell header5 = createHeaderCell("Тегло", headerFont);
            PdfPCell header6 = createHeaderCell("Години", headerFont);
            PdfPCell header7 = createHeaderCell("Степен", headerFont);
            PdfPCell header8 = createHeaderCell("Мед. преглед", headerFont);

            addCellsToTable(table,header1,header2,header3,header4,header5,header6,header7,header8);
// Добавяне на заглавията към таблицата

            users.forEach(u -> {

                String ageGroupDescription = u.getAgeGroup() != null ? u.getAgeGroup().getDescription() : " - ";
                LocalDate medicalExam = u.getMedicalExamsPassed();
                String medicalExamFormatter = medicalExam != null ? medicalExam.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : " - ";
                String birthdateFormatter = u.getBirthDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

                PdfPCell cell1 = createCell(u.getFirstName(), font);
                PdfPCell cell2 = createCell(u.getLastName(), font);
                PdfPCell cell3 = createCell(birthdateFormatter, font);
                PdfPCell cell4 = createCell(ageGroupDescription, font);
                PdfPCell cell5 = createCell((u.getWeight() + " кг."), font);
                PdfPCell cell6 = createCell((userService.calculateAge(u.getBirthDate()) + " г."), font);
                PdfPCell cell7 = createCell(u.getReachedDegree().getDescription(), font);
                PdfPCell cell8 = createCell(medicalExamFormatter, font);

                addCellsToTable(table, cell1,
                                        cell2,
                                        cell3,
                                        cell4,
                                        cell5,
                                        cell6,
                                        cell7,
                                        cell8);
            });

        document.add(table);
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
        try {

        Image imageBackground = Image.getInstance("src/main/resources/static/images/Kan_PDF_background.jpg");
            PdfWriter writer = PdfWriter.getInstance(document,response.getOutputStream());
            writer.setPageEvent(new BackgroundPageEvent(imageBackground,0.3f));

        document.open();

            BaseFont baseFont = BaseFont.createFont("src/main/resources/fonts/Ubuntu-Regular.ttf",BaseFont.IDENTITY_H,BaseFont.EMBEDDED);
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

        return cell;
    }

    private PdfPCell createCell(String content, Font font){

        PdfPCell cell = new PdfPCell(new Phrase(content,font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        return cell;
    }

}
