package studyDesk;

import java.util.LinkedHashMap;
import java.util.Map;

/** 一次专注记录（番茄钟完成一次记一条），用于今日面板统计。 */
public final class FocusRecord {
    public String date = "";
    public int minutes;
    public String label = "";
    /** 是否由番茄钟完成产生，用于统计"今日完成 N 个番茄"。 */
    public boolean pomodoro;

    public FocusRecord() {
    }

    public FocusRecord(String date, int minutes, String label) {
        this.date = date;
        this.minutes = minutes;
        this.label = label;
    }

    Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("date", date);
        m.put("minutes", minutes);
        m.put("label", label);
        m.put("pomodoro", pomodoro);
        return m;
    }

    static FocusRecord fromMap(Map<String, Object> m) {
        FocusRecord f = new FocusRecord();
        f.date = Json.str(m, "date", "");
        f.minutes = Json.integer(m, "minutes", 0);
        f.label = Json.str(m, "label", "");
        f.pomodoro = Json.bool(m, "pomodoro", false);
        return f;
    }
}
