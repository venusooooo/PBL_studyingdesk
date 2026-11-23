package studyDesk;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/** 日期 / 周次相关的轻量工具，避免各页面重复处理字符串与 LocalDate 的转换。 */
final class Dates {
    static final String[] WEEK_LABELS = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    private Dates() {
    }

    static String today() {
        return LocalDate.now().toString();
    }

    static LocalDate parse(String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(iso.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    static String iso(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    /** 1 = 周一 ... 7 = 周日。 */
    static int weekdayOf(LocalDate date) {
        return date.getDayOfWeek().getValue();
    }

    static String weekdayLabel(int weekday) {
        if (weekday < 1 || weekday > 7) {
            return "—";
        }
        return WEEK_LABELS[weekday - 1];
    }

    /** 把 ISO 日期变成"今天 / 明天 / 昨天 / 3 天后 / 5 月 12 日"这类人话。 */
    static String friendly(String iso) {
        LocalDate date = parse(iso);
        if (date == null) {
            return "未设置";
        }
        long delta = ChronoUnit.DAYS.between(LocalDate.now(), date);
        if (delta == 0) {
            return "今天";
        }
        if (delta == 1) {
            return "明天";
        }
        if (delta == -1) {
            return "昨天";
        }
        if (delta > 1 && delta <= 7) {
            return delta + " 天后";
        }
        if (delta < -1 && delta >= -7) {
            return (-delta) + " 天前";
        }
        return date.getMonthValue() + " 月 " + date.getDayOfMonth() + " 日";
    }

    /** 距离今天还有多少天，负数代表已过期。 */
    static long daysFromToday(String iso) {
        LocalDate date = parse(iso);
        if (date == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), date);
    }

    static boolean isToday(String iso) {
        LocalDate date = parse(iso);
        return date != null && date.equals(LocalDate.now());
    }

    static boolean isOverdue(String iso) {
        LocalDate date = parse(iso);
        return date != null && date.isBefore(LocalDate.now());
    }

    /** 本周一。 */
    static LocalDate weekStart() {
        LocalDate today = LocalDate.now();
        return today.minusDays(today.getDayOfWeek().getValue() - 1L);
    }

    /** 是否落在本周（含今天之后的剩余天数）。 */
    static boolean isThisWeek(String iso) {
        LocalDate date = parse(iso);
        if (date == null) {
            return false;
        }
        LocalDate start = weekStart();
        return !date.isBefore(start) && date.isBefore(start.plusDays(7));
    }

    static String shortDate(String iso) {
        LocalDate date = parse(iso);
        if (date == null) {
            return "—";
        }
        return String.format("%02d-%02d", date.getMonthValue(), date.getDayOfMonth());
    }
}
