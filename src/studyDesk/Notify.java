package studyDesk;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * 系统托盘通知 + 提示音。
 * 只用 JDK 自带的 SystemTray / TrayIcon / Toolkit，不引第三方库。
 * 托盘不可用（部分 Linux 桌面、无桌面环境）时静默退化为仅提示音。
 */
final class Notify {

    private static TrayIcon trayIcon;

    private Notify() {
    }

    /** 弹一条系统通知并响一声，用于番茄钟结束提醒。 */
    static void toast(String title, String body) {
        beep();
        if (!SystemTray.isSupported()) {
            return;
        }
        try {
            if (trayIcon == null) {
                trayIcon = new TrayIcon(createIcon(), "StudyDesk");
                trayIcon.setImageAutoSize(true);
                SystemTray.getSystemTray().add(trayIcon);
            }
            trayIcon.displayMessage(title, body, TrayIcon.MessageType.INFO);
        } catch (Exception e) {
            System.err.println("托盘通知不可用：" + e.getMessage());
        }
    }

    static void beep() {
        try {
            Toolkit.getDefaultToolkit().beep();
        } catch (Exception ignored) {
            // 没有声卡也不影响主流程
        }
    }

    /** 画一个 32×32 的圆角方块当作托盘图标，避免依赖外部图片文件。 */
    private static Image createIcon() {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(AppTheme.PRIMARY);
        g.fillRoundRect(2, 2, 28, 28, 10, 10);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Microsoft YaHei", Font.BOLD, 18));
        FontMetrics fm = g.getFontMetrics();
        String text = "S";
        g.drawString(text, (32 - fm.stringWidth(text)) / 2,
                (32 - fm.getHeight()) / 2 + fm.getAscent());
        g.dispose();
        return image;
    }
}
