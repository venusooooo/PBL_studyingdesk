package studyDesk;

import javax.swing.*;
import java.awt.*;

/** 轻量弹窗基类，供后续任务编辑 / CSV 导入等复用。 */
public class LightDialog extends JDialog {
    public LightDialog(Frame owner, String title, boolean modal) {
        super(owner, title, modal);
        setLayout(new BorderLayout());
        getContentPane().setBackground(AppTheme.SURFACE);
        setResizable(false);
    }

    public void centerOn(Component parent) {
        pack();
        setLocationRelativeTo(parent);
    }
}
