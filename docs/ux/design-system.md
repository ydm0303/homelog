# 家记 - 设计系统规范

## 1. 设计 Token (Design Tokens)

### 1.1 色板系统

#### 主色调
```css
/* Primary - 品牌主色：温暖可信的蓝绿色 */
--color-primary-50:  #E6F4F1;
--color-primary-100: #CCE8E3;
--color-primary-200: #99D1C7;
--color-primary-300: #66BBAB;
--color-primary-400: #33A58F;
--color-primary-500: #008F73;  /* 主色标准 */
--color-primary-600: #00735C;
--color-primary-700: #005645;
--color-primary-800: #003A2E;
--color-primary-900: #001D17;

/* Primary 语义变量 */
--color-primary: var(--color-primary-500);
--color-primary-hover: var(--color-primary-600);
--color-primary-active: var(--color-primary-700);
--color-primary-light: var(--color-primary-100);
--color-primary-text: var(--color-primary-700);
--color-primary-border: var(--color-primary-300);
```

#### 语义色
```css
/* Success - 成功/正常/完成 */
--color-success-50:  #F6FFED;
--color-success-500: #52C41A;
--color-success-text: #389E0D;

/* Warning - 警告/即将到期/注意 */
--color-warning-50:  #FFFBE6;
--color-warning-500: #FAAD14;
--color-warning-text: #D48806;

/* Error/Danger - 错误/危险/已过期/删除 */
--color-error-50:  #FFF1F0;
--color-error-500: #F5222D;
--color-error-text: #CF1322;

/* Info - 信息/提示/进行中 */
--color-info-50:  #E6F7FF;
--color-info-500: #1890FF;
--color-info-text: #096DD9;

/* 优先级色标 */
--color-priority-high:    #F5222D;  /* 高 - 红 */
--color-priority-medium:  #FAAD14;  /* 中 - 黄 */
--color-priority-low:     #52C41A;  /* 低 - 绿 */
--color-priority-none:    #999999;  /* 无 - 灰 */
```

#### 中性色
```css
/* 文本色 */
--color-text-primary:   #1A1A1A;   /* 主标题/正文 */
--color-text-secondary: #434343;   /* 次要文本/描述 */
--color-text-tertiary:  #8C8C8C;   /* 辅助文本/占位符 */
--color-text-quaternary: #BFBFBF;  /* 禁用/分隔线 */
--color-text-inverse:   #FFFFFF;   /* 深色背景上 */

/* 背景色 */
--color-bg-base:        #FFFFFF;   /* 页面/卡片背景 */
--color-bg-page:        #F5F5F5;   /* 页面底色 */
--color-bg-elevated:    #FFFFFF;   /* 悬浮层/弹窗 */
--color-bg-hover:       #F5F5F5;   /* Hover 态 */
--color-bg-active:      #E8E8E8;   /* Active/按下态 */
--color-bg-disabled:    #F5F5F5;   /* 禁用态 */
--color-bg-mask:        rgba(0,0,0,0.45);  /* 遮罩层 */

/* 边框色 */
--color-border-base:    #D9D9D9;   /* 标准边框 */
--color-border-light:   #F0F0F0;   /* 浅分割线 */
--color-border-focus:   var(--color-primary-500);  /* 聚焦态 */
--color-border-error:   var(--color-error-500);    /* 错误态 */
```

#### 长辈/高对比度模式色板 (CSS 变量覆盖)
```css
[data-elderly="true"] {
  --color-text-primary:   #000000;
  --color-text-secondary: #1A1A1A;
  --color-text-tertiary:  #434343;
  --color-border-base:    #999999;
  --color-primary-500:    #006644;  /* 更深的主色，对比度更高 */
  --color-success-500:    #2E8B00;
  --color-warning-500:    #CC8800;
  --color-error-500:      #CC0000;
}
```

---

### 1.2 字阶系统

```css
/* 字体族 */
--font-family-base:  "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Helvetica, Arial, sans-serif;
--font-family-mono:  "SF Mono", "Fira Code", "Consolas", monospace;

/* 字号阶梯 (基准 14px) */
--font-size-xs:   10px;  /* 标签/徽标 */
--font-size-sm:   12px;  /* 辅助文本/时间戳 */
--font-size-base: 14px;  /* 标准正文 */
--font-size-md:   16px;  /* 强调文本/输入框 */
--font-size-lg:   18px;  /* 副标题/卡片标题 */
--font-size-xl:   20px;  /* 页面标题 */
--font-size-2xl:  24px;  /* 一级标题 */
--font-size-3xl:  32px;  /* 超大标题/空状态 */

/* 字重 */
--font-weight-normal: 400;
--font-weight-medium: 500;
--font-weight-semibold: 600;
--font-weight-bold: 700;

/* 行高 */
--line-height-tight: 1.3;   /* 标题 */
--line-height-base: 1.5;    /* 正文 */
--line-height-loose: 1.7;   /* 长文本/长辈模式 */

/* 长辈模式字号缩放 (基准 14px → 18px = 1.286x) */
[data-elderly="true"] {
  --font-size-xs:   13px;
  --font-size-sm:   15px;
  --font-size-base: 18px;
  --font-size-md:   20px;
  --font-size-lg:   22px;
  --font-size-xl:   24px;
  --font-size-2xl:  28px;
  --font-size-3xl:  36px;
  --line-height-base: 1.6;
  --line-height-loose: 1.8;
}
```

---

### 1.3 间距系统

```css
/* 基础单位：8px */
--spacing-0: 0;
--spacing-1: 4px;   /* 0.5x */
--spacing-2: 8px;   /* 1x - 基础单位 */
--spacing-3: 12px;  /* 1.5x */
--spacing-4: 16px;  /* 2x - 卡片内边距标准 */
--spacing-5: 20px;  /* 2.5x */
--spacing-6: 24px;  /* 3x - 区块间距 */
--spacing-8: 32px;  /* 4x - 大区块间距 */
--spacing-10: 40px; /* 5x */
--spacing-12: 48px; /* 6x */
--spacing-16: 64px; /* 8x */

/* 语义化间距 */
--spacing-card-padding: var(--spacing-4);      /* 16px */
--spacing-card-gap: var(--spacing-4);          /* 16px */
--spacing-section-gap: var(--spacing-6);       /* 24px */
--spacing-page-margin: var(--spacing-6);       /* 24px */
--spacing-inline-gap: var(--spacing-3);        /* 12px */
--spacing-form-item-gap: var(--spacing-4);     /* 16px */

/* 长辈模式间距放大 (基础单位 8px → 12px = 1.5x) */
[data-elderly="true"] {
  --spacing-1: 6px;
  --spacing-2: 12px;
  --spacing-3: 18px;
  --spacing-4: 24px;
  --spacing-5: 30px;
  --spacing-6: 36px;
  --spacing-8: 48px;
}
```

---

### 1.4 圆角系统

```css
--radius-none: 0;
--radius-xs: 2px;    /* 标签/徽标 */
--radius-sm: 4px;    /* 输入框/按钮/小卡片 */
--radius-base: 8px;  /* 标准卡片/弹窗/下拉 */
--radius-md: 12px;   /* 大卡片/页面区块 */
--radius-lg: 16px;   /* 模态框/抽屉/画廊 */
--radius-xl: 24px;   /* 特大容器 */
--radius-full: 9999px; /* 圆形/胶囊按钮 */

/* 长辈模式圆角放大 */
[data-elderly="true"] {
  --radius-sm: 8px;
  --radius-base: 12px;
  --radius-md: 16px;
  --radius-lg: 20px;
  --radius-xl: 28px;
}
```

---

### 1.5 阴影系统

```css
--shadow-none: none;
--shadow-xs: 0 1px 2px rgba(0,0,0,0.03), 0 1px 1px rgba(0,0,0,0.02);
--shadow-sm: 0 1px 3px rgba(0,0,0,0.05), 0 1px 2px rgba(0,0,0,0.03);
--shadow-base: 0 2px 8px rgba(0,0,0,0.08), 0 1px 3px rgba(0,0,0,0.04);
--shadow-md: 0 4px 16px rgba(0,0,0,0.10), 0 2px 6px rgba(0,0,0,0.05);
--shadow-lg: 0 8px 24px rgba(0,0,0,0.12), 0 4px 12px rgba(0,0,0,0.06);
--shadow-xl: 0 16px 48px rgba(0,0,0,0.15), 0 8px 16px rgba(0,0,0,0.08);

/* 语义化阴影 */
--shadow-card: var(--shadow-base);
--shadow-card-hover: var(--shadow-md);
--shadow-dropdown: var(--shadow-md);
--shadow-modal: var(--shadow-lg);
--shadow-drawer: var(--shadow-xl);
--shadow-fab: var(--shadow-lg);
```

---

### 1.6 动效时长

```css
--duration-instant: 0ms;
--duration-fast: 100ms;      /* Hover/Active/Tooltip */
--duration-base: 200ms;      /* 展开/折叠/Tab切换/Modal */
--duration-slow: 300ms;      /* 页面转场/抽屉/复杂展开 */
--duration-slower: 500ms;    /* 全屏转场/启动动画 */

/* 缓动函数 */
--ease-linear: linear;
--ease-in: cubic-bezier(0.4, 0, 1, 1);
--ease-out: cubic-bezier(0, 0, 0.2, 1);
--ease-in-out: cubic-bezier(0.4, 0, 0.2, 1);
--ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1);  /* 弹性 */

/* 长辈/减弱动效模式 */
[data-reduced-motion="true"],
[data-elderly="true"] {
  --duration-fast: 0ms;
  --duration-base: 0ms;
  --duration-slow: 0ms;
  --duration-slower: 0ms;
}
```

---

### 1.7 Z-Index 层级

```css
--z-base: 0;
--z-dropdown: 1000;      /* 下拉菜单/选择器面板 */
--z-sticky: 1100;        /* 粘性头部/TabBar */
--z-fixed: 1200;         /* 固定按钮/FAB */
--z-drawer: 1300;        /* 抽屉/侧边栏 */
--z-modal: 1400;         /* 模态框 */
--z-popover: 1500;       /* 气泡确认框/Tooltip */
--z-toast: 1600;         /* 全局提示 */
--z-loading: 1700;       /* 全屏加载 */
```

---

### 1.8 断点系统

```css
--breakpoint-xs: 480px;   /* 小手机 */
--breakpoint-sm: 576px;   /* 手机 */
--breakpoint-md: 768px;   /* 平板竖屏 */
--breakpoint-lg: 992px;   /* 平板横屏/小笔记本 */
--breakpoint-xl: 1200px;  /* 桌面 */
--breakpoint-xxl: 1600px; /* 大桌面 */

/* 命名语义 */
--breakpoint-mobile: 767px;    /* ≤ Mobile */
--breakpoint-tablet: 1023px;   /* ≤ Tablet */
--breakpoint-desktop: 1024px;  /* ≥ Desktop */
```

---

## 2. 基础组件状态规范

### 2.1 Button (按钮)

```
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│ 尺寸规范                                                                                        │
├──────────────┬──────────────┬──────────────┬──────────────┬──────────────┬─────────────────────┤
│ 尺寸         │ 高度         │ 内边距(水平)  │ 字号         │ 圆角         │ 适用场景             │
├──────────────┼──────────────┼──────────────┼──────────────┼──────────────┼─────────────────────┤
│ Large (lg)   │ 40px         │ 24px         │ 16px         │ 8px          │ 页面主操作/表单提交  │
│ Medium (md)  │ 32px         │ 16px         │ 14px         │ 6px          │ 标准/工具栏/列表操作 │
│ Small (sm)   │ 28px         │ 12px         │ 12px         │ 4px          │ 紧凑/表格行/标签页   │
│ Mini (xs)    │ 22px         │ 8px          │ 10px         │ 4px          │ 极度紧凑/徽标按钮    │
├──────────────┼──────────────┼──────────────┼──────────────┼──────────────┼─────────────────────┤
│ 长辈模式     │ +12px        │ +8px         │ +4px         │ +4px         │ 全尺寸统一放大       │
│ (Large)      │ 52px         │ 32px         │ 20px         │ 12px         │                      │
└──────────────┴──────────────┴──────────────┴──────────────┴──────────────┴─────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│ 类型与状态                                                                                      │
├──────────────┬──────────────────────────────────────────────────────────────────────────────────┤
│ Primary      │ 主操作：品牌色填充，白字                                                          │
│ Secondary    │ 次操作：白底品牌色边框，品牌色字                                                  │
│ Success      │ 成功态：绿色填充/边框                                                              │
│ Warning      │ 警告态：黄色填充/边框                                                              │
│ Danger       │ 危险态：红色填充/边框，白字/红字                                                  │
│ Info         │ 信息态：蓝色填充/边框                                                              │
│ Text/Link    │ 文字按钮：无背景无边框，品牌色字，Hover 下划线                                     │
├──────────────┼──────────────────────────────────────────────────────────────────────────────────┤
│ Default      │ 标准状态                                                                          │
│ Hover        │ 背景/边框色加深 10%，鼠标指针                                                    │
│ Active       │ 背景/边框色再加深 10%，缩放 0.98                                                 │
│ Loading      │ 禁用 + 内部 Spinner(16px)，文字改为"加载中..."/保持原文                          │
│ Disabled     │ 透明度 0.4，灰色背景/边框，禁用指针，不触发事件                                   │
└──────────────┴──────────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Input / Form Controls (输入框/表单控件)

```
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│ 尺寸：Medium 32px / Large 40px (长辈 52px)                                                      │
├──────────────┬──────────────────────────────────────────────────────────────────────────────────┤
│ 状态         │ 视觉规范                                                                          │
├──────────────┼──────────────────────────────────────────────────────────────────────────────────┤
│ Default      │ 边框 #D9D9D9，背景白，文字 #1A1A1A，占位符 #BFBFBF                               │
│ Hover        │ 边框 #999999                                                                      │
│ Focus        │ 边框 #008F73 + 阴影 0 0 0 2px rgba(0,143,115,0.2)                               │
│ Typing       │ 同 Focus，清除按钮显示                                                            │
│ Error        │ 边框 #F5222D，右侧错误图标 ❌，下方红色错误文本                                  │
│ Warning      │ 边框 #FAAD14，右侧警告图标 ⚠️                                                   │
│ Success      │ 边框 #52C41A，右侧成功图标 ✅ (校验通过反馈)                                      │
│ Disabled     │ 背景 #F5F5F5，边框 #D9D9D9，文字 #8C8C8C，禁用指针                               │
│ ReadOnly     │ 背景白，边框 #D9D9D9，文字正常，无光标，可选中复制                                │
├──────────────┼──────────────────────────────────────────────────────────────────────────────────┤
│ 前缀/后缀    │ 图标/文字颜色 #8C8C8C，Hover #1A1A1A，背景透明                                   │
│ 字符计数     │ 右下角 #BFBFBF，超限 #F5222D                                                     │
│ 前缀标签     │ 标签在左侧，宽度自适应，冒号对齐                                                  │
└──────────────┴──────────────────────────────────────────────────────────────────────────────────┘
```

### 2.3 Select / Dropdown (下拉选择)

```
触发器：同 Input 状态
下拉面板：
- 最大高度 280px，超出滚动
- 选项高度 32px (长辈 44px)
- 悬停项背景 #F5F5F5，选中项背景 #E6F4F1 + 主色文字
- 分组标题：12px #8C8C8C，上边距 12px 下边距 4px
- 空状态：居中"暂无数据" 14px #BFBFBF
- 可搜索：顶部搜索框 32px
```

### 2.4 DatePicker / TimePicker (日期时间选择)

```
面板：280px × 自适应
- 头部：月份/年份切换，当前月高亮主色
- 日期单元格：28px × 28px (长辈 40px)，圆角 50%
- 今天：描边主色 2px
- 选中：主色填充圆形
- 范围选择：首尾圆形，中间主色 10% 背景矩形
- 禁用日期：透明度 0.4，禁止指针
- 时间面板：列表滚动，步进 30min 可配置
- 确认/清空/此刻：底部操作栏
```

### 2.5 Tag / Badge (标签/徽标)

```
尺寸：高度 20px (长辈 28px)，内边距 0 8px (长辈 0 12px)，圆角 10px
类型：
- Default: 灰底灰字 #F5F5F5 / #8C8C8C
- Primary: 主色浅底主色字 #E6F4F1 / #008F73
- Success: 绿色浅底绿字 #F6FFED / #52C41A
- Warning: 黄色浅底黄字 #FFFBE6 / #FAAD14
- Danger: 红色浅底红字 #FFF1F0 / #F5222D
- Info: 蓝色浅底蓝字 #E6F7FF / #1890FF
可关闭：右侧 ✕ 16px，Hover 显示背景
圆点标记：前缀 6px 圆点 + 4px 间距
```

### 2.6 Avatar (头像)

```
尺寸：XS 20px / SM 28px / MD 36px / LG 48px / XL 64px (长辈 +8px)
形状：圆形 / 方形(圆角 8px)
状态：
- 图片加载中：骨架层动画
- 无图片：背景色由名称哈希生成，首字母大写白字
- 状态标记：右下角 8px 圆点 (在线绿/离线灰/忙碌红)
- 叠加组：最多 5 个，重叠 -8px，第 6 个显示 "+N"
```

### 2.7 Card (卡片)

```
结构：
┌─────────────────────────────────────────────┐
│ Header (可选)  标题 + 操作按钮               │ ← 内边距 16px，底部分割线
├─────────────────────────────────────────────┤
│ Content                                     │ ← 内边距 16px
├─────────────────────────────────────────────┤
│ Footer (可选)  操作按钮组/分页               │ ← 内边距 16px，顶部分割线
└─────────────────────────────────────────────┘

阴影：var(--shadow-card)
圆角：var(--radius-md) 12px
Hover：阴影 var(--shadow-card-hover) + 上浮 -2px
分割线：var(--color-border-light)
长辈模式：内边距 24px，圆角 16px，阴影加深
```

### 2.8 Table (表格)

```
行高：44px (紧凑 36px，长辈 52px)
表头：背景 #FAFAFA，字重 500，底部 1px 分割线
单元格：内边距 12px 16px，垂直居中
斑马纹：隔行背景 #FAFAFA (可关闭)
悬停行：背景 #F5F5F5
选中行：背景 #E6F4F1
边框：垂直 #F0F0F0，水平 #F0F0F0
固定列：左侧阴影 var(--shadow-sm)，右侧阴影镜像
展开行：展开动画 200ms slide-down
空状态：居中插画 + 文案 + 操作按钮，垂直居中占满高度
```

### 2.9 Modal / Drawer (模态框/抽屉)

```
Modal：
- 宽度：SM 400px / MD 520px / LG 720px / XL 960px / Full 100%
- 最大高度：视口 90%，超出内容区滚动
- 遮罩：var(--color-bg-mask)，点击关闭可配置
- 动画：Fade + Scale(0.95→1) 200ms
- 头部：标题 + 关闭按钮，内边距 16px 24px
- 内容：内边距 24px
- 底部：右对齐按钮组，内边距 16px 24px，顶部分割线

Drawer：
- 位置：Right (默认) / Left / Top / Bottom
- 宽度：Right/Left 320px / 720px / 100% (Mobile)
- 高度：Top/Bottom 50% / 70% / 100%
- 遮罩：同 Modal
- 动画：Slide 250ms ease-out
```

### 2.10 Toast / Message (轻量反馈)

```
Toast (顶部/中部居中)：
- 成功：绿色图标 + 文案，3s 自动关闭
- 错误：红色图标 + 文案，5s 或手动关闭
- 警告：黄色图标 + 文案，4s
- 信息：蓝色图标 + 文案，3s
- 加载：旋转 Spinner + 文案，不自动关闭
- 最大同时：3 个，堆叠间距 8px
- 动画：Slide-down + Fade 200ms

Message (全局单例，顶部下拉)：
- 同 Toast，但单例模式，新消息替换旧消息
- 可配置显示关闭按钮
```

### 2.11 Progress / Steps (进度/步骤条)

```
Line Progress：
- 高度 6px (长辈 8px)，圆角 3px
- 背景 #F0F0F0，前景主色渐变
- 显示百分比标签 (可选)

Circle Progress：
- 尺寸 48px / 64px / 80px
- 描边宽度 4px / 6px
- 顺时针动画

Steps：
- 竖向/横向
- 图标：待做 圆环 / 进行 旋转 / 完成 ✅ / 错误 ❌
- 连接线：主色 (已完成) / 灰色 (待做)
- 标题 + 描述双行
```

### 2.12 Skeleton / Empty / Error / Loading (状态组件)

```
Skeleton (骨架层)：
- 动画：脉冲/波浪 #E8E8E8 → #F5F5F5 → #E8E8E8 1.5s 无限循环
- 圆角跟随目标组件
- 常用模式：卡片列表、表格行、详情页、表单页

Empty (空状态)：
┌─────────────────────────────────────────────┐
│                                             │
│        📭 (插画 120px / 长辈 180px)          │
│                                             │
│        暂无数据                             │ ← 18px #434343
│        调整筛选条件或新增第一条记录           │ ← 14px #8C8C8C
│                                             │
│        [主操作按钮: 新增/重置/去逛逛]         │
│                                             │
└─────────────────────────────────────────────┘

Error (错误状态)：
- 同 Empty，插画 📡/⚠️/😢，主按钮"重试"/"返回首页"

Loading (全屏/局部)：
- 全屏：居中 Spinner 32px + "加载中..." 14px #8C8C8C
- 局部：容器相对定位，绝对居中 Spinner 24px，半透明遮罩
```

---

## 3. 反馈组件详细规范

### 3.1 Toast (轻提示)

```typescript
interface ToastOptions {
  type: 'success' | 'error' | 'warning' | 'info' | 'loading';
  message: string;
  duration?: number;        // ms，默认 success/info: 3000, warning: 4000, error: 5000, loading: 0
  showClose?: boolean;      // 默认 false
  position?: 'top' | 'center' | 'bottom';  // 默认 top
  onClose?: () => void;
  id?: string;              // 去重用
  // 长辈模式
  large?: boolean;          // 字号+2px，内边距+4px，图标+4px
}

// 视觉
// 容器：固定定位，z-index 1600
// 单条：最小宽 240px，最大宽 400px，内边距 12px 16px
// 圆角 8px，阴影 var(--shadow-lg)
// 图标 20px，文案 14px，间距 8px
// 入场：slide-down 200ms + fade
// 离场：slide-up 200ms + fade
```

### 3.2 Modal (模态对话框)

```typescript
interface ModalProps {
  title?: string;
  visible: boolean;
  width?: number | string;  // 400/520/720/960/full
  fullscreen?: boolean;
  maskClosable?: boolean;   // 点击遮罩关闭
  closable?: boolean;       // 显示关闭按钮
  confirmLoading?: boolean;
  okText?: string;          // 默认"确定"
  cancelText?: string;      // 默认"取消"
  destroyOnClose?: boolean;
  // 长辈模式
  large?: boolean;          // 宽度+20%，字号+2px，按钮高度 52px
}

// 结构
// .modal-mask (遮罩)
//   .modal-wrap (居中容器)
//     .modal (内容盒)
//       .modal-header: 标题 + 关闭按钮
//       .modal-body: 内容区 (可滚动)
//       .modal-footer: 底部按钮组 (右对齐)
```

### 3.3 Drawer (抽屉)

```typescript
interface DrawerProps {
  placement: 'left' | 'right' | 'top' | 'bottom';
  visible: boolean;
  size?: number | string;   // width/height
  maskClosable?: boolean;
  keyboard?: boolean;       // ESC 关闭
  header?: ReactNode;       // 自定义头部
  footer?: ReactNode;       // 自定义底部
  // 长辈模式
  large?: boolean;          // 尺寸+20%，内边距+8px
}

// Mobile 底部抽屉 (Action Sheet 风格)
// - 从底部滑入，最大高度 70%vh
// - 顶部圆角 16px，带拖拽指示条
// - 底部安全区适配
```

### 3.4 Message (全局消息)

```typescript
// 单例模式，API:
message.success(content, duration?)
message.error(content, duration?)
message.warning(content, duration?)
message.info(content, duration?)
message.loading(content, duration?)
message.destroy()

// 视觉同 Toast，但固定顶部，单例替换
```

### 3.5 Progress (进度条)

```typescript
interface ProgressProps {
  type: 'line' | 'circle' | 'dashboard';
  percent: number;          // 0-100
  status?: 'success' | 'exception' | 'active';
  strokeWidth?: number;     // line: 6-8, circle: 4-6
  showInfo?: boolean;       // 显示百分比/自定义内容
  format?: (percent) => ReactNode;
  // 长辈模式
  large?: boolean;          // line高度 10px, circle尺寸+16px
}
```

### 3.6 Popconfirm (气泡确认框)

```typescript
// 点击触发元素弹出，用于删除/归档等二次确认
// 宽度 280px，箭头指向触发元素
// 内容：标题 + 描述 + 取消/确认按钮
// 确认按钮危险色 (删除场景)
```

### 3.7 Tooltip (文字提示)

```typescript
// 悬停/聚焦显示
// 延迟：显示 150ms，隐藏 100ms
// 位置：top/right/bottom/left + start/center/end
// 最大宽度 280px，自动换行
// 长辈模式：字号 14px，内边距 10px 12px，显示延迟 300ms
```

---

## 4. 图标规范

```css
/* 图标库：Element Plus Icons / @icon-park/vue-next */
/* 统一尺寸变量 */
--icon-size-xs: 12px;   /* 徽标内/紧凑 */
--icon-size-sm: 14px;   /* 按钮内/输入框前缀 */
--icon-size-base: 16px; /* 标准/菜单项 */
--icon-size-md: 20px;   /* 卡片/列表/工具栏 */
--icon-size-lg: 24px;   /* 页面标题/空状态 */
--icon-size-xl: 32px;   /* 大插画/引导页 */
--icon-size-2xl: 48px;  /* 超大状态插画 */

/* 长辈模式 +4px */
[data-elderly="true"] {
  --icon-size-base: 20px;
  --icon-size-md: 24px;
  --icon-size-lg: 28px;
}

/* 常用语义图标映射 */
--icon-home: HomeFilled;
--icon-menu: Menu;
--icon-close: Close;
--icon-search: Search;
--icon-add: Plus;
--icon-edit: Edit;
--icon-delete: Delete;
--icon-view: View;
--icon-copy: CopyDocument;
--icon-archive: Box;
--icon-download: Download;
--icon-upload: Upload;
--icon-camera: Camera;
--icon-photo: Picture;
--icon-scan: Qrcode;
--icon-member: User;
--icon-members: Avatar;
--icon-setting: Setting;
--icon-bell: Bell;
--icon-clock: Clock;
--icon-calendar: Calendar;
--icon-tag: Tag;
--icon-star: StarFilled;
--icon-warning: WarningFilled;
--icon-success: CheckCircleFilled;
--icon-error: CloseCircleFilled;
--icon-info: InfoFilled;
--icon-loading: Loading;
--icon-chevron-right: ArrowRight;
--icon-chevron-left: ArrowLeft;
--icon-chevron-up: ArrowUp;
--icon-chevron-down: ArrowDown;
--icon-more: More;
--icon-drag: GripHorizontal;
--icon-eye: View;
--icon-eye-off: Hide;
--icon-lock: Lock;
--icon-unlock: Unlock;
--icon-share: Share;
--icon-print: Printer;
```

---

## 5. 组件库适配说明 (Element Plus / Naive UI)

```typescript
// Element Plus 主题定制 (SCSS)
@forward 'element-plus/theme-chalk/src/common/var.scss' with (
  $colors: (
    'primary': (
      'base': var(--color-primary-500),
      'light-3': var(--color-primary-100),
      'light-5': var(--color-primary-50),
      'dark-2': var(--color-primary-700),
    ),
    'success': ( 'base': var(--color-success-500) ),
    'warning': ( 'base': var(--color-warning-500) ),
    'danger': ( 'base': var(--color-error-500) ),
    'info': ( 'base': var(--color-info-500) ),
  ),
  $font-sizes: (
    'base': var(--font-size-base),
    'sm': var(--font-size-sm),
    'xs': var(--font-size-xs),
    'lg': var(--font-size-lg),
    'xl': var(--font-size-xl),
  ),
  $border-radius: (
    'base': var(--radius-base),
    'small': var(--radius-sm),
    'round': var(--radius-full),
  ),
  $spacing: (
    'base': var(--spacing-4),
    'sm': var(--spacing-2),
    'lg': var(--spacing-6),
  ),
  $shadows: (
    'base': var(--shadow-base),
    'light': var(--shadow-sm),
    'dark': var(--shadow-md),
  ),
  $transitions: (
    'fast': var(--duration-fast),
    'base': var(--duration-base),
    'slow': var(--duration-slow),
  )
);

// Naive UI 主题定制 (TypeScript)
import { createTheme } from 'naive-ui'
const theme = createTheme({
  common: {
    primaryColor: 'var(--color-primary-500)',
    primaryColorHover: 'var(--color-primary-400)',
    primaryColorPressed: 'var(--color-primary-600)',
    baseColor: 'var(--color-text-primary)',
    bodyColor: 'var(--color-bg-page)',
    fontSize: 'var(--font-size-base)',
    fontSizeSmall: 'var(--font-size-sm)',
    fontSizeMedium: 'var(--font-size-md)',
    fontSizeLarge: 'var(--font-size-lg)',
    borderRadius: 'var(--radius-base)',
    borderRadiusSmall: 'var(--radius-sm)',
    boxShadow1: 'var(--shadow-sm)',
    boxShadow2: 'var(--shadow-base)',
    boxShadow3: 'var(--shadow-md)',
    transitionDuration: 'var(--duration-base)',
  },
  Button: {
    heightSmall: '28px',
    heightMedium: '32px',
    heightLarge: '40px',
    fontSizeSmall: 'var(--font-size-sm)',
    fontSizeMedium: 'var(--font-size-base)',
    fontSizeLarge: 'var(--font-size-md)',
  },
  Input: {
    heightSmall: '28px',
    heightMedium: '32px',
    heightLarge: '40px',
  },
  // ... 其他组件同理
})
```

---

## 6. 响应式栅格系统

```css
/* 24 列栅格 */
.container {
  width: 100%;
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 var(--spacing-4);
}

.row {
  display: flex;
  flex-wrap: wrap;
  margin: 0 calc(var(--spacing-3) * -1);
}

.col {
  padding: 0 var(--spacing-3);
  flex: 1;
}

/* 响应式类 */
.col-xs-{1-24}   { flex: 0 0 calc(100% * n / 24); max-width: calc(100% * n / 24); }
.col-sm-{1-24}   { @media (min-width: 576px) { ... } }
.col-md-{1-24}   { @media (min-width: 768px) { ... } }
.col-lg-{1-24}   { @media (min-width: 992px) { ... } }
.col-xl-{1-24}   { @media (min-width: 1200px) { ... } }

/* 常用布局模式 */
.layout-sidebar {
  /* Desktop: 侧边栏 260px + 内容 1fr */
  /* Tablet: 侧边栏可折叠 64px/260px */
  /* Mobile: 抽屉菜单 320px */
}

.layout-tabbar {
  /* Mobile: 底部固定 4-5 项 TabBar */
  /* Desktop/Tablet: 隐藏 */
}
```

---

*文件：docs/ux/design-system.md | 版本：v1.0*