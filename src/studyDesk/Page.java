package studyDesk;

import java.util.Map;
import javax.swing.*;

/**
 * 主页面契约。
 * onShow / onHide 处理页面进出；saveState / restoreState 支持"返回时恢复原页面筛选状态"。
 */
public interface Page {
    PageId getId();

    String getTitle();

    JComponent getView();

    /** 页面被展示时调用，params 携带页面内跳转参数（筛选条件、对象 id 等）。 */
    default void onShow(Map<String, Object> params) {
    }

    default void onHide() {
    }

    /** 离开页面前保存当前筛选 / 展开状态。 */
    default Map<String, Object> saveState() {
        return new java.util.HashMap<>();
    }

    /** 返回或带参跳转时恢复状态。 */
    default void restoreState(Map<String, Object> state) {
    }
}
