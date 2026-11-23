package studyDesk;

import java.util.LinkedHashMap;
import java.util.Map;

/** 课程。课时、错题、笔记、任务都通过 courseId 关联到课程。 */
public final class Course {
    public long id;
    public String name = "";
    public String teacher = "";
    /** 课表与标签统一使用这个颜色，存 #RRGGBB。 */
    public String color = "#2F6FED";
    public String note = "";

    public Course() {
    }

    public Course(String name, String teacher, String color) {
        this.name = name;
        this.teacher = teacher;
        this.color = color;
    }

    Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("teacher", teacher);
        m.put("color", color);
        m.put("note", note);
        return m;
    }

    static Course fromMap(Map<String, Object> m) {
        Course c = new Course();
        c.id = Json.lng(m, "id", 0);
        c.name = Json.str(m, "name", "未命名课程");
        c.teacher = Json.str(m, "teacher", "");
        c.color = Json.str(m, "color", "#2F6FED");
        c.note = Json.str(m, "note", "");
        return c;
    }
}
