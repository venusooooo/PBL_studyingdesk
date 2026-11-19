package studyDesk;

import javax.swing.*;
import java.util.Map;

/** 页面向主窗口发起跳转 / 弹窗 / 状态更新的统一入口。 */
public interface PageContext {
    /** 一级导航直达，或页面内带参跳转。 */
    void navigateTo(PageId id, Map<String, Object> params);

    /** 返回上一页面，并恢复其离开前的状态。 */
    void back();

    /** 轻量弹窗操作。 */
    void openDialog(JDialog dialog);

    void setStatus(String text);
}
