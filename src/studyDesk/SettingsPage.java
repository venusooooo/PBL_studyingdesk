package studyDesk;

import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.*;

/** 系统设置：数据位置、学期周次、导出与清空。 */
public final class SettingsPage extends BasePage {

    @Override
    public PageId getId() {
        return PageId.SETTINGS;
    }

    @Override
    protected void buildContent() {
        Store store = Store.get();
        setHeaderHint("数据全部保存在本机，不联网、不上传；换电脑时把 data.json 拷走即可迁移。");

        // ---------- 数据位置 ----------
        JPanel pathRow = new JPanel(new BorderLayout(AppTheme.GRID, 0));
        pathRow.setOpaque(false);
        JLabel path = Ui.label(store.dataFile().toString(), AppTheme.FONT_SMALL, AppTheme.TEXT);
        path.setToolTipText(store.dataFile().toString());
        JPanel pathActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        pathActions.setOpaque(false);

        JButton openDir = Ui.ghost("打开数据目录");
        openDir.addActionListener(e -> openFolder(store.dataDir()));
        JButton saveNow = Ui.ghost("立即保存");
        saveNow.addActionListener(e -> {
            store.save();
            context().setStatus("已保存：" + store.dataFile());
        });
        pathActions.add(saveNow);
        pathActions.add(openDir);

        pathRow.add(path, BorderLayout.CENTER);
        pathRow.add(pathActions, BorderLayout.EAST);

        JPanel pathBody = new JPanel(new BorderLayout(0, 8));
        pathBody.setOpaque(false);
        pathBody.add(pathRow, BorderLayout.NORTH);
        JLabel pathState = Ui.muted("当前状态：" + store.saveState()
                + " · 上一版自动备份为 data.bak.json");
        pathBody.add(pathState, BorderLayout.SOUTH);
        addCard(Ui.card("数据存储", null, pathBody), 0, 0);

        // ---------- 学期 ----------
        JPanel termRow = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.GRID, 0));
        termRow.setOpaque(false);
        JTextField termStart = Ui.field(10);
        termStart.setText(store.termStart());
        termStart.setToolTipText("格式：2026-09-01，填学期第一周的周一");
        JButton saveTerm = Ui.primary("保存");
        saveTerm.addActionListener(e -> {
            String text = termStart.getText().trim();
            if (!text.isEmpty() && Dates.parse(text) == null) {
                FormDialog.toast(getView(), "日期格式应为 2026-09-01");
                return;
            }
            store.setTermStart(text);
            context().setStatus(text.isEmpty() ? "已清除学期起始日" : "学期起始日已设为 " + text);
            refresh();
        });
        termRow.add(termStart);
        termRow.add(saveTerm);
        termRow.add(Ui.muted("当前第 " + store.currentWeek() + " 周"));

        JPanel termBody = new JPanel(new BorderLayout(0, 8));
        termBody.setOpaque(false);
        termBody.add(termRow, BorderLayout.NORTH);
        termBody.add(Ui.muted("顶部状态栏的「第 N 周」按这个日期推算；留空则始终显示第 1 周。"), BorderLayout.SOUTH);
        addCard(Ui.card("学期设置", null, termBody), 1, 0);

        // ---------- 导出 ----------
        JButton export = Ui.primary("导出 CSV 到桌面");
        export.addActionListener(e -> exportCsv());
        JPanel exportRow = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.GRID, 0));
        exportRow.setOpaque(false);
        exportRow.add(export);
        exportRow.add(Ui.muted("导出任务、错题、笔记三张表，方便写进课程报告"));
        addCard(Ui.card("数据导出", null, exportRow), 2, 0);

        // ---------- 维护 ----------
        JButton restore = Ui.ghost("恢复示例数据");
        restore.addActionListener(e -> {
            if (FormDialog.confirm(getView(), "恢复示例数据",
                    "会清空当前全部数据并重新写入示例课程 / 任务 / 错题，确定继续吗？")) {
                store.restoreSamples();
                context().setStatus("已恢复示例数据");
                refresh();
            }
        });
        JButton clear = Ui.danger("清空全部数据");
        clear.addActionListener(e -> {
            if (FormDialog.confirm(getView(), "清空全部数据",
                    "将删除所有课程、课表、错题、笔记与任务，且无法撤销。\n建议先导出 CSV。确定清空吗？")) {
                store.clearAll();
                context().setStatus("数据已清空");
                refresh();
            }
        });
        JPanel dangerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.GRID, 0));
        dangerRow.setOpaque(false);
        dangerRow.add(clear);
        dangerRow.add(restore);
        addCard(Ui.card("数据维护", null, dangerRow), 3, 0);

        // ---------- 关于 ----------
        JLabel about = Ui.wrap("StudyDesk v0.3（PBL 课程实践）\n"
                + "技术栈：JDK 11+ / Java Swing / AWT，零第三方依赖，javac 直接编译。\n"
                + "架构：Page 契约 + BasePage 卡片页面 + PageContext 跳转 + Store 单例数据层。\n"
                + "已实现：课表（单双周 / 导出 ics）、课程、错题复习、笔记、计划任务、番茄钟、\n"
                + "学习统计、AI 助手、全局搜索与本地持久化。", 540);
        addCard(Ui.card("关于", null, about), 4, 0);
    }

    private void openFolder(Path dir) {
        try {
            if (!java.nio.file.Files.exists(dir)) {
                java.nio.file.Files.createDirectories(dir);
            }
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(new File(dir.toString()));
            } else {
                FormDialog.toast(getView(), "当前系统不支持直接打开文件夹，路径：\n" + dir);
            }
        } catch (Exception ex) {
            FormDialog.toast(getView(), "打开目录失败：" + ex.getMessage());
        }
    }

    private void exportCsv() {
        try {
            Path desktop = Paths.get(System.getProperty("user.home"), "Desktop");
            if (!java.nio.file.Files.exists(desktop)) {
                desktop = Paths.get(System.getProperty("user.home"));
            }
            Path target = desktop.resolve("StudyDesk导出_" + Dates.today().replace("-", ""));
            Store.get().exportCsv(target);
            context().setStatus("已导出到 " + target);
            FormDialog.toast(getView(), "导出完成：\n" + target);
        } catch (Exception ex) {
            FormDialog.toast(getView(), "导出失败：" + ex.getMessage());
        }
    }
}
