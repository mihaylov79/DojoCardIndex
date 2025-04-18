package cardindex.dojocardindex.Event.service;


import cardindex.dojocardindex.Event.models.Event;
import cardindex.dojocardindex.Event.models.EventType;
import cardindex.dojocardindex.Event.repository.EventRepository;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.Utils.BackgroundPageEvent;
import cardindex.dojocardindex.exceptions.EventNotFoundException;
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
                .build();

        eventRepository.save(event);

    }

    public void closeEvent(UUID eventId){
        Event event = getEventById(eventId);

        event = event.toBuilder().closed(true).build();

        eventRepository.save(event);

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
            //TODO Да създаам ExportIOException - който да улавям когато възникне проблем
            log.error("Генерирането на CSV файл за събитие: {} беше неуспешно!", eventId, e);
            throw new RuntimeException("Генерирането на PDF файл беще неуспешно!");
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
            PdfWriter  writer =PdfWriter.getInstance(document,response.getOutputStream());
            writer.setPageEvent(new BackgroundPageEvent(background,0.3f));
        } catch (IOException e) {
            log.error("Генерирането на PDF файл за събитие: {} беще неуспешно!",eventId,e);
            //TODO Да създаам ExportIOException - който да улавям когато възникне проблем
            throw new RuntimeException("Генерирането на PDF файл беще неуспешно!");
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
        Font logoFont = new Font(baseFont, 22,Font.BOLDITALIC,new Color(61,133,198));
        Font titleFont = new Font(baseFont, 16, Font.BOLD);
        Font subtitleFont = new Font(baseFont, 10, Font.BOLD);
        Paragraph paragraph = (new Paragraph("\"ДРАГОН ДОДЖО ДСД - Асеновград\"",logoFont));
        paragraph.setAlignment(Element.ALIGN_CENTER);
        document.add(paragraph);

        LineSeparator lineSeparator = new LineSeparator();
        lineSeparator.setPercentage(75f);
        lineSeparator.setLineColor(new Color(61,133,198));
        lineSeparator.setAlignment(Element.ALIGN_CENTER);
        lineSeparator.setOffset(-6f);
        lineSeparator.setLineWidth(4f);
        document.add(lineSeparator);
            
        document.add(Chunk.NEWLINE);
        document.add(new Paragraph("Събитие: " + event.getEventDescription(),titleFont));
        document.add(new Paragraph("Начало: " + event.getStartDate(),subtitleFont));
        document.add(new Paragraph("Място: " + event.getLocation(),subtitleFont));
        document.add(Chunk.NEWLINE);
        document.add(new Paragraph("Списък с участниците от Драгон Доджо ДСД:",font));

        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);




//        Stream.of("Име", "Фамилия", "Дата на раждане", "Категория", "Тегло", "Възраст", "Степен", "Мед. преглед")
//                .forEach(header -> {
//                    PdfPCell cell = new PdfPCell(new Phrase(header));
//                    cell.setBackgroundColor(new Color(220, 220, 220));
//                    cell.setPadding(5);
//                    table.addCell(cell);
//                });


//        users.forEach(u-> {
//            table.addCell(u.getFirstName());
//            table.addCell(u.getLastName());
//            table.addCell(u.getBirthDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
//            String ageGroupDesc = u.getAgeGroup() != null
//                    ? u.getAgeGroup().getDescription()
//                    : "Няма възрастова група";
//            table.addCell(ageGroupDesc);
//            table.addCell(String.valueOf(u.getWeight()));
//            table.addCell(String.valueOf(userService.calculateAge(u.getBirthDate())));
//            table.addCell(u.getReachedDegree().getDescription());
//            LocalDate medicalExam = u.getMedicalExamsPassed();
//            String medicalExamDate = medicalExam != null
//                    ? medicalExam.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
//                    : "няма информация";
//            table.addCell(medicalExamDate);
//
//        });

//            BaseFont baseFont = BaseFont.createFont("src/main/resources/fonts/Ubuntu-Regular.ttf", BaseFont.IDENTITY_H,BaseFont.EMBEDDED);
//            Font font = new Font(baseFont,10,Font.NORMAL);
//            Font headerFont = new Font(baseFont, 8, Font.BOLD);



//            PdfPCell header1 = new PdfPCell(new Phrase("Име", headerFont));
//            header1.setBackgroundColor(new Color(200, 200, 255));
//            PdfPCell header2 = new PdfPCell(new Phrase("Фамилия", headerFont));
//            header2.setBackgroundColor(new Color(200,200,255));
//            PdfPCell header3 = new PdfPCell(new Phrase("Дата на раждане", headerFont));
//            header3.setBackgroundColor(new Color(200,200,255));
//            PdfPCell header4 = new PdfPCell(new Phrase("Група", headerFont));
//            header4.setBackgroundColor(new Color(200,200,255));
//            PdfPCell header5 = new PdfPCell(new Phrase("Тегло", headerFont));
//            header5.setBackgroundColor(new Color(200,200,255));
//            PdfPCell header6 = new PdfPCell(new Phrase("Години", headerFont));
//            header6.setBackgroundColor(new Color(200,200,255));
//            PdfPCell header7 = new PdfPCell(new Phrase("Степен", headerFont));
//            header7.setBackgroundColor(new Color(200,200,255));
//            PdfPCell header8 = new PdfPCell(new Phrase("Мед. преглед", headerFont));
//            header8.setBackgroundColor(new Color(200,200,255));

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
//            table.addCell(header1);
//            table.addCell(header2);
//            table.addCell(header3);
//            table.addCell(header4);
//            table.addCell(header5);
//            table.addCell(header6);
//            table.addCell(header7);
//            table.addCell(header8);

//            users.forEach(u -> {
//                PdfPCell cell1 = new PdfPCell(new Phrase(u.getFirstName(), font));
//                PdfPCell cell2 = new PdfPCell(new Phrase(u.getLastName(), font));
//                PdfPCell cell3 = new PdfPCell(new Phrase(u.getBirthDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")), font));
//                String ageGroupDesc = u.getAgeGroup() != null ? u.getAgeGroup().getDescription() : " - ";
//                PdfPCell cell4 = new PdfPCell(new Phrase(ageGroupDesc, font));
//                PdfPCell cell5 = new PdfPCell(new Phrase(String.valueOf(u.getWeight()), font));
//                PdfPCell cell6 = new PdfPCell(new Phrase(String.valueOf(userService.calculateAge(u.getBirthDate())), font));
//                PdfPCell cell7 = new PdfPCell(new Phrase(u.getReachedDegree().getDescription(), font));
//                LocalDate medicalExam = u.getMedicalExamsPassed();
//                String medicalExamDate = medicalExam != null ? medicalExam.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : " - ";
//                PdfPCell cell8 = new PdfPCell(new Phrase(medicalExamDate, font));
//
//                addCellsToTable(table,cell1,cell2,cell3,cell4,cell5,cell6,cell7,cell8);
////                table.addCell(cell1);
////                table.addCell(cell2);
////                table.addCell(cell3);
////                table.addCell(cell4);
////                table.addCell(cell5);
////                table.addCell(cell6);
////                table.addCell(cell7);
////                table.addCell(cell8);
//            });

            users.forEach(u -> {

                String ageGroupDescription = u.getAgeGroup() != null ? u.getAgeGroup().getDescription() : " - ";
                LocalDate medicalExam = u.getMedicalExamsPassed();
                String medicalExamFormatter = medicalExam != null ? medicalExam.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : " - ";
                String birthdateFormatter = u.getBirthDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

                PdfPCell cell1 = createCell(u.getFirstName(), font);
                PdfPCell cell2 = createCell(u.getLastName(), font);
                PdfPCell cell3 = createCell(birthdateFormatter, font);
                PdfPCell cell4 = createCell(ageGroupDescription, font);
                PdfPCell cell5 = createCell(String.valueOf(u.getWeight()), font);
                PdfPCell cell6 = createCell(String.valueOf(userService.calculateAge(u.getBirthDate())), font);
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
            throw new RuntimeException(e);
        }

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
