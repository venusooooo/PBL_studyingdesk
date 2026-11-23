package studyDesk;

import java.util.LinkedHashMap;
import java.util.Map;

/** 学习笔记，纯文本存储，支持课程归类、标签、置顶。 */
public final class Note {
    public long id;
    public long courseId;
    public String title = "";
    public String content = "";
    /** 逗号分隔的标签。 */
    public String tags = "";
    public boolean pinned;
    public String createdAt = "";
    public String updatedAt = "";

    public Note() {
    }

    public Note(String title, String content, long courseId) {
        this.title = title;
        this.content = content;
        this.courseId = courseId;
        this.createdAt = Dates.today();
        this.updatedAt = Dates.today();
    }

    /** 列表里展示的摘要，去掉换行并截断。 */
    public String preview(int max) {
        String flat = content == null ? "" : content.replace("\r", " ").replace("\n", " ").trim();
        if (flat.length() <= max) {
            return flat;
        }
        return flat.substring(0, max) + "…";
    }

    Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("courseId", courseId);
        m.put("title", title);
        m.put("content", content);
        m.put("tags", tags);
        m.put("pinned", pinned);
        m.put("createdAt", createdAt);
        m.put("updatedAt", updatedAt);
        return m;
    }

    static Note fromMap(Map<String, Object> m) {
        Note n = new Note();
        n.id = Json.lng(m, "id", 0);
        n.courseId = Json.lng(m, "courseId", 0);
        n.title = Json.str(m, "title", "未命名笔记");
        n.content = Json.str(m, "content", "");
        n.tags = Json.str(m, "tags", "");
        n.pinned = Json.bool(m, "pinned", false);
        n.createdAt = Json.str(m, "createdAt", "");
        n.updatedAt = Json.str(m, "updatedAt", "");
        return n;
    }
}
