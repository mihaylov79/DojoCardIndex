package cardindex.dojocardindex.UserConsentHistory.service;

import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.UserConsentHistory.model.UserConsentHistory;
import cardindex.dojocardindex.exceptions.ExportIOException;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ConsentHistoryExportService {

    private static final Logger log = LoggerFactory.getLogger(ConsentHistoryExportService.class);
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private static final DateTimeFormatter SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final UserConsentHistoryService userConsentHistoryService;
    private final UserService userService;

    public ConsentHistoryExportService(UserConsentHistoryService userConsentHistoryService,
                                       UserService userService) {
        this.userConsentHistoryService = userConsentHistoryService;
        this.userService = userService;
    }

    public void exportConsentHistoryPdf(UUID userId, HttpServletResponse response) {
        User user = userService.getUserById(userId);
        Map<String, List<UserConsentHistory>> historyByAgreement = userConsentHistoryService.getHistoryForUserGroupedByAgreementTitle(user);
        int eventCount = historyByAgreement.values().stream().mapToInt(List::size).sum();

        response.setContentType("application/pdf");
        String exportTimestampForFile = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "consent-history-" + userId + "-" + exportTimestampForFile + ".pdf";
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        try (InputStream fontStream = getClass().getClassLoader().getResourceAsStream("fonts/Ubuntu-Regular.ttf")) {
            if (fontStream == null) {
                throw new ExportIOException("Шрифтът Ubuntu-Regular.ttf не е намерен!");
            }

            BaseFont baseFont = BaseFont.createFont(
                    "Ubuntu-Regular.ttf",
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED,
                    true,
                    fontStream.readAllBytes(),
                    null
            );

            Font titleFont = new Font(baseFont, 18, Font.BOLD);
            Font headerFont = new Font(baseFont, 11, Font.BOLD);
            Font bodyFont = new Font(baseFont, 10, Font.NORMAL);
            Font smallFont = new Font(baseFont, 9, Font.NORMAL);

            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            Paragraph title = new Paragraph("История на съгласията", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(6f);
            document.add(title);

            // Export timestamp (visible on the document)
            Paragraph exportInfo = new Paragraph("Експортиран на: " + formatDateTime(LocalDateTime.now()), smallFont);
            exportInfo.setAlignment(Element.ALIGN_RIGHT);
            exportInfo.setSpacingAfter(12f);
            document.add(exportInfo);

            PdfPTable userTable = new PdfPTable(2);
            userTable.setWidthPercentage(100);
            userTable.setSpacingAfter(12f);
            userTable.setWidths(new float[]{1.3f, 3.7f});
            addRow(userTable, "Потребител", safeUserName(user), headerFont, bodyFont);
            addRow(userTable, "Имейл", safeText(user.getEmail()), headerFont, bodyFont);
            addRow(userTable, "Общо събития", String.valueOf(eventCount), headerFont, bodyFont);
            document.add(userTable);

            if (historyByAgreement.isEmpty()) {
                Paragraph empty = new Paragraph("Няма история на съгласия за този потребител.", bodyFont);
                document.add(empty);
            } else {
                for (Map.Entry<String, List<UserConsentHistory>> entry : historyByAgreement.entrySet()) {
                    Paragraph agreementTitle = new Paragraph(entry.getKey(), headerFont);
                    agreementTitle.setSpacingBefore(8f);
                    agreementTitle.setSpacingAfter(6f);
                    document.add(agreementTitle);

                    for (UserConsentHistory history : entry.getValue()) {
                        PdfPTable historyTable = new PdfPTable(2);
                        historyTable.setWidthPercentage(100);
                        historyTable.setSpacingAfter(8f);
                        historyTable.setWidths(new float[]{1.6f, 3.4f});

                        addRow(historyTable, "Действие", safeEnum(history.getAction()), headerFont, bodyFont);
                        addRow(historyTable, "Време", formatDateTime(history.getActionAt()), headerFont, bodyFont);
                        addRow(historyTable, "Consent ID", safeConsentId(history), headerFont, bodyFont);
                        addRow(historyTable, "Съгласие от", formatDateTime(history.getConsent() != null ? history.getConsent().getCreatedAt() : null, SHORT_DATE_FORMAT), headerFont, bodyFont);
                        addRow(historyTable, "Извършено от", safeActionBy(history), headerFont, bodyFont);
                        addRow(historyTable, "Причина", safeText(history.getReason()), headerFont, bodyFont);
                        addRow(historyTable, "Бележки", safeText(history.getNotes()), headerFont, bodyFont);

                        document.add(historyTable);
                    }
                }
            }

            Paragraph footer = new Paragraph("Документът е генериран автоматично.", smallFont);
            footer.setSpacingBefore(10f);
            document.add(footer);
        } catch (IOException e) {
            log.error("Генерирането на PDF файл за история на съгласията за потребител {} беше неуспешно!", userId, e);
            throw new ExportIOException("Генерирането на PDF файл беше неуспешно!");
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    private void addRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        table.addCell(createCell(label, labelFont, true));
        table.addCell(createCell(value, valueFont, false));
    }

    private PdfPCell createCell(String content, Font font, boolean headerCell) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5f);
        cell.setBorderColor(Color.LIGHT_GRAY);
        if (headerCell) {
            cell.setBackgroundColor(new Color(240, 240, 240));
        }
        return cell;
    }

    private String safeUserName(User user) {
        return safeText((user.getFirstName() != null ? user.getFirstName() : "") + " " + (user.getLastName() != null ? user.getLastName() : ""));
    }

    private String safeActionBy(UserConsentHistory history) {
        if (history.getActionBy() == null) {
            return "Система";
        }
        return safeText(history.getActionBy().getFirstName() + " " + history.getActionBy().getLastName());
    }

    private String safeConsentId(UserConsentHistory history) {
        if (history.getConsent() == null || history.getConsent().getId() == null) {
            return "Без ID";
        }
        return history.getConsent().getId().toString();
    }

    private String safeEnum(Enum<?> value) {
        return value == null ? "-" : value.name();
    }

    private String formatDateTime(java.time.LocalDateTime dateTime) {
        return formatDateTime(dateTime, DATE_TIME_FORMAT);
    }

    private String formatDateTime(java.time.LocalDateTime dateTime, DateTimeFormatter formatter) {
        return dateTime == null ? "-" : dateTime.format(formatter);
    }

    private String safeText(String value) {
        return (value == null || value.trim().isEmpty()) ? "-" : value;
    }
}

