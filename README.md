# StudyDesk 个人学习平台

基于 **Java Swing** 的桌面端个人学习管理工具，面向 PBL（项目式学习）课程实践。当前版本完成的是**界面框架与导航体系**，业务功能以占位页形式预留，后续按模块逐个迭代。

技术栈只有 JDK 自带的 Swing / AWT，**不依赖任何第三方库**，不需要 Maven 或 Gradle，下载即编译。

---

## 界面结构

主窗口采用固定的四区布局，各区域职责单一：

```
┌──────────────────────────────────────────────────────┐
│  顶部状态栏 TopBar                                    │
├──────────┬──────────────────────────────┬────────────┤
│          │                              │            │
│  一级    │      内容区（可独立滚动）      │  右侧栏    │
│  导航    │      CardLayout 切换页面       │  AI 占位   │
│  Nav     │                              │  Side      │
│          │                              │            │
├──────────┴──────────────────────────────┴────────────┤
│  底部状态栏 BottomBar                                 │
└──────────────────────────────────────────────────────┘
```

- **左侧一级导航**：七个主模块，点击即切换，当前页高亮。
- **中间内容区**：每个页面独立滚动，切换页面不影响导航栏与状态栏位置。
- **右侧栏**：预留 AI 助手位置，可折叠。
- **顶部 / 底部状态栏**：前者显示标题与面包屑，后者显示操作反馈。

## 七个功能模块

| 模块 | 标识 | 当前状态 |
| --- | --- | --- |
| 今日面板 | `TODAY` | 占位页 |
| 课表管理 | `SCHEDULE` | 占位页 |
| 课程与课时 | `COURSES` | 占位页 |
| 习题与错题 | `EXERCISES` | 占位页 |
| 学习笔记 | `NOTES` | 占位页 |
| 学习计划与任务 | `PLANS` | 占位页 |
| 系统设置 | `SETTINGS` | 占位页 |

## 目录结构

```
PBL_studyingdesk/
├── README.md
├── .gitignore
└── src/
    └── studyDesk/
        ├── StudyDeskApp.java      程序入口，设置系统外观并启动主窗口
        ├── MainFrame.java         主窗口：四区布局、导航、页面栈、历史返回
        ├── PageId.java            七个一级页面的枚举
        ├── Page.java              页面契约接口
        ├── BasePage.java          页面基类：页头 + 卡片网格
        ├── PlaceholderPage.java   占位页实现
        ├── PageContext.java       页面向主窗口发起跳转的统一入口
        ├── TopBar.java            顶部状态栏
        ├── BottomBar.java         底部状态栏
        ├── AppTheme.java          全局配色、字体、间距常量
        └── LightDialog.java       轻量弹窗基类
```

## 运行方式

要求 JDK 17 及以上（代码只用到标准 Swing，低版本 JDK 理论也可编译）。

```bash
git clone https://github.com/venusooooo/PBL_studyingdesk.git
cd PBL_studyingdesk

# 编译，产物输出到 out/
javac -encoding UTF-8 -d out src/studyDesk/*.java

# 运行
java -cp out studyDesk.StudyDeskApp
```

Windows 下如果终端编码不是 UTF-8，源码里的中文注释可能导致编译警告，加 `-encoding UTF-8` 即可。

## 架构说明

框架的核心是**页面与主窗口解耦**，新增业务功能时不必改动主窗口。

**页面契约 `Page`**

```java
public interface Page {
    PageId getId();
    String getTitle();
    JComponent getView();
    default void onShow(Map<String, Object> params) { }
    default void onHide() { }
    default Map<String, Object> saveState() { return new HashMap<>(); }
    default void restoreState(Map<String, Object> state) { }
}
```

`onShow` / `onHide` 处理页面进出，`saveState` / `restoreState` 支持「返回上一页时恢复原来的筛选条件、展开状态」，这是多级页面跳转能用的前提。

**页面基类 `BasePage`**

固定「页头 + 卡片网格」结构，内容区交给 `JScrollPane` 独立滚动。子类只需实现 `buildContent()`，用 `addCard(卡片, 行, 列)` 摆放卡片即可，间距与配色全部取自 `AppTheme`。

**页面上下文 `PageContext`**

页面不直接操作 `MainFrame`，而是通过注入的 `PageContext` 发起动作：

```java
void navigateTo(PageId id, Map<String, Object> params);
void back();
void openDialog(JDialog dialog);
void setStatus(String text);
```

**新增一个业务页面的步骤**

1. 继承 `BasePage`，实现 `getId()` 与 `buildContent()`；
2. 在 `MainFrame.initPages()` 里把该 `PageId` 的实例从 `PlaceholderPage` 换成新类；
3. 页面内部通过 `context().navigateTo(...)` 跳转，不要直接引用 `MainFrame`。

**样式统一**

所有颜色、字号、间距集中在 `AppTheme`，不在页面里写魔法数字。改主题色只需改一个文件。

## 当前进度

- [x] 主窗口四区布局与尺寸规范
- [x] 七个模块的一级导航与页面切换
- [x] 页面栈与返回、状态保存恢复机制
- [x] 全局主题常量
- [ ] 七个模块的具体业务功能
- [ ] 数据持久化
- [ ] 右侧 AI 助手接入

## 说明

本项目为课程 PBL 实践作品，界面框架阶段成果。占位页上的「功能待实现」是预期状态，不是缺陷。
