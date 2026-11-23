package studyDesk;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;

/**
 * 主窗口：固定四区结构 + 底部状态栏。
 * 左：一级导航；顶：状态栏；中：独立滚动的内容区；右：可折叠侧栏（AI 占位）；底：状态栏。
 */
public final class MainFrame extends JFrame implements PageContext {

    private static final long serialVersionUID = 1L;

    private final TopBar topBar = new TopBar();
    private final BottomBar bottomBar = new BottomBar();
    private final JPanel navPanel = new JPanel();
    private final Map<PageId, JButton> navButtons = new EnumMap<>(PageId.class);
    private final PageHost contentCards = new PageHost(new CardLayout());
    private final JScrollPane contentScroll;
    private final JPanel sidePanel = new JPanel();
    private final Map<PageId, Page> pages = new EnumMap<>(PageId.class);
    private final Deque<NavRecord> history = new ArrayDeque<>();

    private PageId current;

    private static final class NavRecord {
        final PageId id;
        final Map<String, Object> state;

        NavRecord(PageId id, Map<String, Object> state) {
            this.id = id;
            this.state = state;
        }
    }

    /**
     * 内容区宿主。CardLayout 的偏好宽度取所有页面最大值，
     * 直接放进滚动面板会让每个页面都多出一条横向滚动条，
     * 这里让内容宽度始终跟随视口，横向只保留垂直滚动。
     */
    private static final class PageHost extends JPanel implements Scrollable {

        private static final long serialVersionUID = 1L;

        PageHost(java.awt.LayoutManager layout) {
            super(layout);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(visibleRect.height - 60, 60);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    public MainFrame() {
        super("StudyDesk 个人学习平台");
        setSize(1280, 800);
        setMinimumSize(new Dimension(1180, 720));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        initPages();
        initNav();
        initSide();

        contentScroll = new JScrollPane(contentCards);
        contentScroll.setBorder(null);
        contentScroll.getVerticalScrollBar().setUnitIncrement(16);
        contentScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        add(topBar, BorderLayout.NORTH);
        add(navPanel, BorderLayout.WEST);
        add(contentScroll, BorderLayout.CENTER);
        add(sidePanel, BorderLayout.EAST);
        add(bottomBar, BorderLayout.SOUTH);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                Store.get().save();
            }
        });

        navigateTo(PageId.TODAY, new HashMap<>());
    }

    private void initPages() {
        BasePage[] all = {
                new TodayPage(),
                new SchedulePage(),
                new CoursesPage(),
                new ExercisesPage(),
                new NotesPage(),
                new PlansPage(),
                new SettingsPage()
        };
        for (BasePage page : all) {
            page.bind(this);
            pages.put(page.getId(), page);
            contentCards.add(page.getView(), page.getId().name());
        }
        // 任何数据变更后同步顶部周次与底部保存状态
        Store.get().addListener(() -> {
            topBar.setWeek(Store.get().currentWeek());
            bottomBar.setSaveState(Store.get().saveState());
        });
        topBar.setWeek(Store.get().currentWeek());
        bottomBar.setSaveState(Store.get().saveState());
    }

    private void initNav() {
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setBackground(AppTheme.SURFACE);
        navPanel.setBorder(BorderFactory.createEmptyBorder(
                AppTheme.PAGE_PADDING, AppTheme.GRID * 2,
                AppTheme.PAGE_PADDING, AppTheme.GRID * 2));

        JLabel caption = new JLabel("导航");
        caption.setFont(AppTheme.FONT_SMALL);
        caption.setForeground(AppTheme.TEXT_SECONDARY);
        caption.setAlignmentX(Component.LEFT_ALIGNMENT);
        navPanel.add(caption);
        navPanel.add(Box.createVerticalStrut(AppTheme.GRID * 2));

        for (PageId id : PageId.values()) {
            JButton button = new JButton(id.getLabel());
            button.setFont(AppTheme.FONT_BODY);
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setOpaque(true);
            button.setBackground(AppTheme.SURFACE);
            button.setForeground(AppTheme.TEXT);
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
            button.setMaximumSize(new Dimension(200, 40));
            button.addActionListener(e -> navigateTo(id, new HashMap<>()));
            navButtons.put(id, button);
            navPanel.add(button);
            navPanel.add(Box.createVerticalStrut(AppTheme.GRID / 2));
        }
    }

    private void initSide() {
        sidePanel.setLayout(new BorderLayout());
        sidePanel.setBackground(AppTheme.SURFACE);
        sidePanel.setBorder(BorderFactory.createEmptyBorder(
                AppTheme.PAGE_PADDING, AppTheme.GRID,
                AppTheme.PAGE_PADDING, AppTheme.GRID));
        sidePanel.setPreferredSize(new Dimension(260, 0));

        JButton toggle = new JButton("收起侧栏");
        toggle.setFont(AppTheme.FONT_SMALL);
        toggle.addActionListener(e -> {
            boolean showing = sidePanel.isVisible();
            sidePanel.setVisible(!showing);
            toggle.setText(showing ? "展开侧栏" : "收起侧栏");
            revalidate();
            repaint();
        });

        JLabel placeholder = new JLabel("<html><body style='width:200px'>"
                + "AI 侧栏（界面占位）<br>AI 对话等功能不在本阶段范围内。"
                + "</body></html>");
        placeholder.setVerticalAlignment(SwingConstants.TOP);
        placeholder.setFont(AppTheme.FONT_SMALL);
        placeholder.setForeground(AppTheme.TEXT_SECONDARY);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        top.setOpaque(false);
        top.add(toggle);
        sidePanel.add(top, BorderLayout.NORTH);
        sidePanel.add(placeholder, BorderLayout.CENTER);
    }

    @Override
    public void navigateTo(PageId id, Map<String, Object> params) {
        if (id == null) {
            return;
        }
        if (current == id) {
            applyParams(id, params);
            return;
        }
        if (current != null) {
            Page old = pages.get(current);
            old.onHide();
            history.push(new NavRecord(current, old.saveState()));
        }
        applyParams(id, params);
        current = id;
        bottomBar.setStatus("已打开 " + id.getLabel());
    }

    /** 统一执行"恢复状态 -> 显示页面 -> 更新标题/导航/滚动位置"。 */
    private void applyParams(PageId id, Map<String, Object> params) {
        Map<String, Object> safe = params == null ? new HashMap<>() : params;
        Page page = pages.get(id);
        page.restoreState(safe);
        page.onShow(safe);
        ((CardLayout) contentCards.getLayout()).show(contentCards, id.name());
        current = id;
        topBar.setPageTitle(page.getTitle());
        for (Map.Entry<PageId, JButton> entry : navButtons.entrySet()) {
            boolean active = entry.getKey() == id;
            entry.getValue().setBackground(active ? AppTheme.NAV_ACTIVE : AppTheme.SURFACE);
        }
        contentScroll.getViewport().setViewPosition(new Point(0, 0));
    }

    @Override
    public void back() {
        if (history.isEmpty()) {
            bottomBar.setStatus("没有可返回的页面");
            return;
        }
        NavRecord record = history.pop();
        Page page = pages.get(record.id);
        page.restoreState(record.state);
        page.onShow(record.state);
        ((CardLayout) contentCards.getLayout()).show(contentCards, record.id.name());
        current = record.id;
        topBar.setPageTitle(page.getTitle());
        for (Map.Entry<PageId, JButton> entry : navButtons.entrySet()) {
            boolean active = entry.getKey() == record.id;
            entry.getValue().setBackground(active ? AppTheme.NAV_ACTIVE : AppTheme.SURFACE);
        }
        contentScroll.getViewport().setViewPosition(new Point(0, 0));
        bottomBar.setStatus("已返回 " + record.id.getLabel());
    }

    @Override
    public void openDialog(JDialog dialog) {
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    @Override
    public void setStatus(String text) {
        bottomBar.setStatus(text);
    }
}
