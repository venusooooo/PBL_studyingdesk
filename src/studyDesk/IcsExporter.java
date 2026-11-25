package studyDesk;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 把课表导出成 iCalendar（.ics），可以直接导入手机日历 / Outlook / Google Calendar。
 * 内容按 RFC 5545 手拼，不依赖任何第三方库。
 */
final class IcsExporter {

    /** 与课表页面保持一致的节次开始时间。 */
    private static final String[] SLOT_TIME = {
            "08:00", "08:55", "10:00", "10:55", "14:00",
            "14:55", "16:00", "16:55", "19:00", "19:55"};

    /** 一节课 45 分钟，连续两节之间休息 10 分钟。 */
    private static final int CLASS_MINUTES = 45;
    private static final int BREAK_MINUTES = 10;

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final DateTimeFormatter CLOCK =
            DateTimeFormatter.ofPattern("HHmm");

    private IcsExporter() {
    }

    /** 导出全部课时，返回写入的事件条数。 */
    static int export(Path target) throws IOException {
        Store store = Store.get();
        LocalDate monday = termMonday(store);
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("PRODID:-//StudyDesk//Course Schedule//CN\r\n");
        sb.append("CALSCALE:GREGORIAN\r\n");
        sb.append("METHOD:PUBLISH\r\n");
        sb.append("X-WR-CALNAME:").append(escape("StudyDesk 课表")).append("\r\n");

        int events = 0;
        for (Lesson lesson : store.lessons()) {
            String course = store.courseName(lesson.courseId);
            int startSlot = Math.max(1, Math.min(SLOT_TIME.length, lesson.startSlot));
            int endSlot = Math.max(startSlot, Math.min(SLOT_TIME.length, lesson.endSlot));
            int blocks = endSlot - startSlot + 1;
            int minutes = blocks * CLASS_MINUTES + (blocks - 1) * BREAK_MINUTES;

            // 单周从第 1 周开始、双周从第 2 周开始，保证第一次出现就符合周次
            boolean odd = "ODD".equals(lesson.weeks);
            boolean even = "EVEN".equals(lesson.weeks);
            int firstWeek = even ? 2 : 1;
            LocalDate firstDay = monday.plusWeeks(firstWeek - 1L)
                    .plusDays(Math.max(1, Math.min(7, lesson.weekday)) - 1L);
            LocalTime startTime = LocalTime.parse(SLOT_TIME[startSlot - 1]);

            sb.append("BEGIN:VEVENT\r\n");
            sb.append("UID:studydesk-").append(lesson.id).append("@local\r\n");
            sb.append("DTSTAMP:").append(ZonedDateTime.now(ZoneOffset.UTC).format(STAMP)).append("\r\n");
            sb.append("DTSTART:").append(stamp(firstDay, startTime)).append("\r\n");
            sb.append("DTEND:").append(stamp(firstDay, startTime.plusMinutes(minutes))).append("\r\n");
            sb.append("SUMMARY:").append(escape(course + weeksSuffix(lesson))).append("\r\n");
            if (lesson.room != null && !lesson.room.isBlank()) {
                sb.append("LOCATION:").append(escape(lesson.room)).append("\r\n");
            }
            sb.append("RRULE:FREQ=WEEKLY;INTERVAL=").append(odd || even ? 2 : 1)
                    .append(";COUNT=").append(odd || even ? 10 : 20).append("\r\n");
            sb.append("END:VEVENT\r\n");
            events++;
        }

        sb.append("END:VCALENDAR\r\n");
        Files.writeString(target, sb.toString(), StandardCharsets.UTF_8);
        return events;
    }

    private static String weeksSuffix(Lesson lesson) {
        if ("ODD".equals(lesson.weeks)) {
            return "（单周）";
        }
        if ("EVEN".equals(lesson.weeks)) {
            return "（双周）";
        }
        return "";
    }

    /** 生成本地时间形式的 DTSTART/DTEND，例如 20260907T080000。 */
    private static String stamp(LocalDate date, LocalTime time) {
        return date.format(DateTimeFormatter.BASIC_ISO_DATE) + "T"
                + time.format(CLOCK) + "00";
    }

    private static LocalDate termMonday(Store store) {
        LocalDate start = Dates.parse(store.termStart());
        return start == null ? Dates.weekStart() : start;
    }

    /** RFC 5545 文本转义：反斜杠、分号、逗号、换行。 */
    private static String escape(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\n", "\\n");
    }
}
