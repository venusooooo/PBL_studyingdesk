package studyDesk;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.border.Border;

/**
 * 界面零件工具箱：卡片、按钮、标签、进度条、空状态等。
 * 页面里不允许再出现孤立的颜色 / 字号数字，统一走 AppTheme + Ui。
 */
final class Ui {

    private Ui() {
    }

    // ========================= 颜色 =========================

    static Color color(String hex) {
        try {
            return Color.decode(hex);
        } catch (Exception e) {
            return AppTheme.PRIMARY;
        }
    }

    static Color courseColor(long courseId) {
        Course c = Store.get().course(courseId);
        return c == null ? AppTheme.TEXT_SECONDARY : color(c.color);
    }

    /** 把颜色调浅，用于标签底色。 */
    static Color tint(Color c, float ratio) {
        int r = (int) (c.getRed() + (255 - c.getRed()) * ratio);
        int g = (int) (c.getGreen() + (255 - c.getGreen()) * ratio);
        int b = (int) (c.getBlue() + (255 - c.getBlue()) * ratio);
        return new Color(clamp(r), clamp(g), clamp(b));
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    // ========================= 容器 =========================

    /** 白底 + 1px 描边 + 16px 内边距的标准卡片。 */
    static JPanel card() {
        JPanel p = new JPanel(new BorderLayout(AppTheme.GRID, AppTheme.GRID));
        p.setBackground(AppTheme.SURFACE);
        p.setBorder(AppTheme.cardBorder());
        return p;
    }

    /** 带标题行的卡片：标题 + 右侧操作区 + 主体。 */
    static JPanel card(String title, JComponent right, JComponent body) {
        JPanel p = card();
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setFont(AppTheme.FONT_H2);
        t.setForeground(AppTheme.TEXT);
        head.add(t, BorderLayout.WEST);
        if (right != null) {
            head.add(right, BorderLayout.EAST);
        }
        p.add(head, BorderLayout.NORTH);
        if (body != null) {
            p.add(body, BorderLayout.CENTER);
        }
        return p;
    }

    static JPanel hbox(int gap, Component... items) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, gap, 0));
        p.setOpaque(false);
        for (Component c : items) {
            p.add(c);
        }
        return p;
    }

    static JPanel vbox(int gap) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        p.putClientProperty("gap", gap);
        return p;
    }

    /** 竖直堆叠一组组件，统一间距。 */
    static JPanel stack(int gap, Component... items) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        boolean first = true;
        for (Component c : items) {
            if (!first && gap > 0) {
                p.add(Box.createVerticalStrut(gap));
            }
            if (c instanceof JComponent) {
                ((JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);
            }
            p.add(c);
            first = false;
        }
        return p;
    }

    // ========================= 文本 =========================

    static JLabel title(String text) {
        return label(text, AppTheme.FONT_TITLE, AppTheme.TEXT);
    }

    static JLabel h2(String text) {
        return label(text, AppTheme.FONT_H2, AppTheme.TEXT);
    }

    static JLabel body(String text) {
        return label(text, AppTheme.FONT_BODY, AppTheme.TEXT);
    }

    static JLabel muted(String text) {
        return label(text, AppTheme.FONT_SMALL, AppTheme.TEXT_SECONDARY);
    }

    static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text == null ? "" : text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    /** 自动换行的多行文本（HTML 渲染），px 为内容宽度。 */
    static JLabel wrap(String text, int px, Font font, Color color) {
        String safe = escape(text == null ? "" : text).replace("\n", "<br>");
        JLabel l = new JLabel("<html><body style='width:" + px + "px;font-family:Microsoft YaHei'>"
                + safe + "</body></html>");
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    static JLabel wrap(String text, int px) {
        return wrap(text, px, AppTheme.FONT_SMALL, AppTheme.TEXT_SECONDARY);
    }

    static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ========================= 按钮 =========================

    static JButton filled(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setFont(AppTheme.FONT_SMALL);
        b.setForeground(fg);
        b.setBackground(bg);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setContentAreaFilled(true);
        b.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    static JButton primary(String text) {
        return filled(text, AppTheme.PRIMARY, Color.WHITE);
    }

    static JButton ghost(String text) {
        JButton b = new JButton(text);
        b.setFont(AppTheme.FONT_SMALL);
        b.setForeground(AppTheme.TEXT_SECONDARY);
        b.setBackground(AppTheme.SURFACE);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    static JButton danger(String text) {
        JButton b = new JButton(text);
        b.setFont(AppTheme.FONT_SMALL);
        b.setForeground(AppTheme.DANGER);
        b.setBackground(AppTheme.SURFACE);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.DANGER, 1),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** 纯文字按钮，用于列表里的次要操作。 */
    static JButton link(String text) {
        JButton b = new JButton(text);
        b.setFont(AppTheme.FONT_SMALL);
        b.setForeground(AppTheme.PRIMARY);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setMargin(new Insets(0, 4, 0, 4));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ========================= 标签 / 统计 =========================

    /** 圆角色块标签。 */
    static JLabel chip(String text, Color c) {
        Chip chip = new Chip(text, tint(c, 0.86f), c);
        return chip;
    }

    private static final class Chip extends JLabel {
        private final Color bg;

        Chip(String text, Color bg, Color fg) {
            super(text);
            this.bg = bg;
            setForeground(fg);
            setFont(AppTheme.FONT_SMALL);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** 数字 + 说明的统计块。 */
    static JPanel stat(String value, String label, Color accent) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        JLabel v = label(value, AppTheme.FONT_NUM, accent);
        JLabel l = muted(label);
        v.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(v);
        p.add(Box.createVerticalStrut(2));
        p.add(l);
        return p;
    }

    /** 自绘进度条：去掉系统 L&F 的渐变与动画，保持和卡片一致的圆角细条。 */
    static JProgressBar progress(int value, final Color color) {
        JProgressBar bar = new JProgressBar(0, 100) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                g2.setColor(new Color(0xE9, 0xED, 0xF4));
                g2.fillRoundRect(0, 0, w, h, h, h);
                int fill = (int) (w * getValue() / 100.0);
                if (fill > 0) {
                    g2.setColor(color);
                    g2.fillRoundRect(0, 0, Math.max(fill, h), h, h, h);
                }
                g2.dispose();
            }
        };
        bar.setBorderPainted(false);
        bar.setOpaque(false);
        bar.setValue(Math.max(0, Math.min(100, value)));
        bar.setPreferredSize(new Dimension(120, 8));
        bar.setBackground(AppTheme.SOFT);
        bar.setForeground(color);
        return bar;
    }

    /** 把组件靠右放（配合 BorderLayout.EAST 使用，避免控件被纵向拉伸）。 */
    static JPanel alignRight(JComponent c) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        p.setOpaque(false);
        p.add(c);
        return p;
    }

    /** 把组件靠左放并保持自然高度。 */
    static JPanel alignLeft(JComponent c) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.setOpaque(false);
        p.add(c);
        return p;
    }

    // ========================= 输入 =========================

    static JTextField field(int columns) {
        JTextField f = new JTextField(columns);
        f.setFont(AppTheme.FONT_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        return f;
    }

    static JTextField search(int columns, String placeholder) {
        final String hint = placeholder;
        JTextField f = new JTextField(columns) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && hint != null && !hint.isEmpty()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(new Color(0xA9, 0xB0, 0xBC));
                    g2.setFont(getFont());
                    Insets ins = getInsets();
                    int y = (getHeight() + g2.getFontMetrics().getAscent()) / 2 - 2;
                    g2.drawString(hint, ins.left + 2, y);
                    g2.dispose();
                }
            }
        };
        f.setFont(AppTheme.FONT_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        f.setToolTipText(placeholder);
        return f;
    }

    /** 固定宽度（高度保持自然高度），用于双列卡片避免把整页撑宽。 */
    static <T extends JComponent> T width(T c, int w) {
        c.setPreferredSize(new Dimension(w, c.getPreferredSize().height));
        return c;
    }

    static JComboBox<String> combo(String[] items, int selected) {
        JComboBox<String> box = new JComboBox<>(items);
        box.setFont(AppTheme.FONT_SMALL);
        box.setSelectedIndex(Math.max(0, Math.min(selected, items.length - 1)));
        box.setBackground(AppTheme.SURFACE);
        return box;
    }

    // ========================= 空状态 / 分隔 =========================

    static JPanel empty(String message) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(18, 4, 18, 4));
        JLabel l = new JLabel(message);
        l.setFont(AppTheme.FONT_SMALL);
        l.setForeground(new Color(0x9A, 0xA1, 0xAE));
        p.add(l, BorderLayout.WEST);
        return p;
    }

    static JSeparator divider() {
        JSeparator s = new JSeparator();
        s.setForeground(AppTheme.BORDER);
        s.setBackground(AppTheme.BORDER);
        return s;
    }

    /** 卡片内的一条列表行：白底、底部细线、鼠标悬停变浅灰。 */
    static JPanel row() {
        JPanel p = new JPanel(new BorderLayout(AppTheme.GRID, 0));
        p.setBackground(AppTheme.SURFACE);
        p.setBorder(BorderFactory.createEmptyBorder(10, 4, 10, 4));
        p.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                p.setBackground(new Color(0xFA, 0xFB, 0xFD));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                p.setBackground(AppTheme.SURFACE);
            }
        });
        return p;
    }

    static Border pad(int top, int left, int bottom, int right) {
        return BorderFactory.createEmptyBorder(top, left, bottom, right);
    }

    /** 让组件在 BoxLayout 中占满宽度。 */
    static <T extends JComponent> T wide(T c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        return c;
    }

    // ========================= 撤销提示 =========================

    private static JWindow toastWindow;

    /**
     * 在窗口底部浮出一条深色提示，附带"撤销"，5 秒后自动消失。
     * 比每次删除都弹确认框更顺手：删错了还能补救，没删错也不用多点一次。
     */
    static void undoToast(Component parent, String message, Runnable onUndo) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        if (owner == null || !owner.isShowing()) {
            return;
        }
        if (toastWindow != null) {
            toastWindow.dispose();
        }

        JWindow window = new JWindow(owner);
        JPanel panel = new JPanel(new BorderLayout(AppTheme.GRID, 0));
        panel.setBackground(new Color(0x1F, 0x29, 0x37));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 8));

        JLabel label = new JLabel(message);
        label.setFont(AppTheme.FONT_SMALL);
        label.setForeground(Color.WHITE);

        JButton undo = new JButton("撤销");
        undo.setFont(AppTheme.FONT_SMALL);
        undo.setForeground(new Color(0x9E, 0xC1, 0xFF));
        undo.setBorderPainted(false);
        undo.setContentAreaFilled(false);
        undo.setFocusPainted(false);
        undo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        undo.addActionListener(e -> {
            window.dispose();
            onUndo.run();
        });

        panel.add(label, BorderLayout.CENTER);
        panel.add(undo, BorderLayout.EAST);
        window.setContentPane(panel);
        window.pack();
        Point origin = owner.getLocationOnScreen();
        window.setLocation(origin.x + (owner.getWidth() - window.getWidth()) / 2,
                origin.y + owner.getHeight() - window.getHeight() - 48);
        window.setAlwaysOnTop(true);
        window.setVisible(true);

        toastWindow = window;
        javax.swing.Timer closer = new javax.swing.Timer(5000, e -> window.dispose());
        closer.setRepeats(false);
        closer.start();
    }
}
