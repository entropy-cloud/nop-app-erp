# nop-erp-loop-engineering PPT 选择合并记录

> 从多个版本的 PPT 中选择特定页，合并组织成新的 PPT。

## 源文件一览

| 简称 | 文件 | 总页数 |
|------|------|--------|
| loop-v1 | `nop-erp-loop-engineering.ppt.html` | 32 |
| loop-v2 | `nop-erp-loop-engineering-v2.ppt.html` | 20 |
| journey | `nop-erp-dev-journey.ppt.html` | ~34 |
| journey-v2 | `nop-erp-dev-journey-v2.ppt.html` | ~30 |
| journey-v3 | `nop-erp-dev-journey-v3.ppt.html` | 27 |
| comprehensive | `nop-erp-dev-journey-comprehensive.ppt.html` | ~48 |
| synthesis | `nop-erp-dev-journey-synthesis.ppt.html` | ~48 |
| v4 | `nop-erp-dev-journey-ppt-outline-v4.ppt.html` | ~23 |

## 设计原则

1. **去重**：知识转移曲线仅保留 journey#slide-14（含三阶段总结，信息密度最高）；A/B/C 干预从 8 页压缩到 2 页
2. **叙事弧线**：开场 → 问题+实验 → Harness 层 → Attractor 层 → 证据 → 收尾
3. **高潮位置**：知识转移曲线紧接 Attractor 概念之后，保持 momentum
4. **收尾顺序**：先 Takeaway（带走什么）→ 最后全场架构总图（视觉定格）

## 合并后 PPT 逐页来源

| 页码 | Act | 源文件 | 原标题/备注 |
|------|-----|--------|------------|
| 1 | Act 1 开场 | `loop-v2#slide-1` | S01 标题页 |
| 2 | Act 2 问题 | `v4#slide-2` | 原题"一个反直觉的起点"。**改标题为变革点主题**：传统软件工程需要人全程参与；智能时代人如何逐步退出，从 In-the-Loop 到 On-the-Loop；nop-app-erp 作为一个实验性项目，验证这一过程 |
| 3 | Act 2 实验 | `journey-v2#slide-3` | "nop-app-erp 是什么"——三圈交叠 + 一句话定位 + 关键指标速览 |
| 4 | Act 2 实验 | `journey-v2#slide-4` | "起点：06-22 上午的真实状态"——AGE 模板空骨架 |
| 5 | Act 2 实验 | `synthesis#slide-6` | "22 天后：经审计校准的关键规模指标"——8 项 stat |
| 6 | Act 3 Harness 趋势 | `loop-v1#slide-3` | S03 从 Prompt → Loop → AGE。业内发展方向 + AGE 补充增强 |
| 7 | Act 3 Plan Loop | `loop-v1#slide-8` | 原题"没有 Loop 会怎样"。**改标题为"Vibe Coding Loop"** |
| 8 | Act 3 Plan Loop | `loop-v1#slide-9`（主体）+ `loop-v2#slide-5`（标题+总结） | **标题替换**：v1 原题 → v2 标题"从 In 到 On 的关键：Plan"。**结尾替换**：v1 highlight box → v2 Harness 定义总结 |
| 9 | Act 3 Plan Loop | `loop-v2#slide-6` | S06 实例：Plan 1100-1 销售定价引擎 |
| 10 | Act 3 Mission Driver | `loop-v1#slide-14` | S14 "为什么需要更大一层的 Loop"——Plan Loop 只管理单次变更 |
| 11 | Act 3 Mission Driver | `loop-v1#slide-16` | S16 "Mission Driver 的 5 步闭环"。**概念重构**：三层 → 一个主流程 + 两个展开子流程 |
| 12 | Act 3 Mission Driver | `loop-v1#slide-17` | S17 "Mission Driver 的实现架构"。**移除**顶部 speaker note。**新增**"对人介入的支持"小节 |
| 13 | Act 3 Mission Driver | `loop-v1#slide-15` | S15 "真实运行记录：07-10 一次完整循环" |
| 14 | Act 4 Attractor | `loop-v1#slide-19` | S19 "两层 Harness 的关系——以及它们不能做的事"——过渡到 Attractor |
| 15 | Act 4 Attractor | `loop-v1#slide-21` | S21 "动力系统语言——为什么需要 Attractor"——四层本体 + 三种常见混淆 |
| 16 | Act 4 Attractor | `comprehensive#slide-18` | "用户其实在做什么——定义和演进吸引子"——A/B/C 三类介入 |
| 17 | Act 4 高潮 | `journey#slide-14` | "知识转移曲线"——柱状图+折线图组合，底部三阶段总结（早期/中期/后期）。**选此版本原因**：信息密度最高，含三阶段文字释意 |
| 18 | Act 5 证据 | `journey-v2#slide-6` | "8 阶段演进"——Gantt 图 |
| 19 | Act 5 证据 | `journey-v2#slide-14` | "六种自驱动机制"——计划-审计闭环等 |
| 20 | Act 5 反思 | `v4#slide-14` | "对 AI 自主开发研究的含义"——四条件 + 总结反思 |
| 21 | Act 5 展望 | `v4#slide-16` | "从 28 条干预到全自动的最后一公里"——三个基础设施 |
| 22 | Act 6 收尾 | `loop-v2` line 1269 (S19) | "三个 Takeaway"——Takeaway 1：把"变更"从对话变成契约 / Takeaway 2：把"编排"从记忆变成自动 / Takeaway 3：把"方向"从个人变成文件 |
| 23 | Act 6 收尾 | `loop-v2` line 1294 (S20) | "落地路径——三种项目模式"——模式 A 已有项目（最小改动）/ 模式 B 新项目（全自动）/ 模式 C 平台框架（方向决策） |
| 24 | 结尾 | 仿 nop-chaos-flux 样式 | Q&A 页——标题 + 两个二维码占位（公众号/交流群）+ 项目信息 |

### 去重说明

| 删除项 | 原因 | 替代保留 |
|--------|------|---------|
| `synthesis#slide-31` 知识转移曲线 | 与 journey#14 重复 | journey#14（信息密度更高） |
| `journey-v2#slide-12` 知识转移曲线 | 与 journey#14 重复 | journey#14（含三阶段总结） |
| `v4#slide-4` 28 条干预饼图 | 与 comprehensive#18 重叠 | comprehensive#18 已含 A/B/C 分类 |
| `v4#slide-5` 典型实例表格 | 与 comprehensive#18 重叠 | — |
| `v4#slide-6` 80设计决策 92.5% | 单页数据，非叙事必须 | — |
| `synthesis#slide-28` 外部注入三层模型 | 与 comprehensive#18 重叠 | — |
| `synthesis#slide-29` A 类速览表 | 过度细节，非叙事必须 | — |
| `synthesis#slide-30` B 类案例 | 过度细节，非叙事必须 | — |
| `journey#slide-10` A 类全局租户开关 | 过度细节，非叙事必须 | — |
| `comprehensive#slide-13` DEEP_AUDIT | 与 MD 运行记录重叠 | loop-v1#slide-15 已含 |
| `journey#slide-16` 注入来源全景图 | 非核心叙事 | — |
| `journey-v3#slide-22` 审计方法论演进 | 过度细节 | — |
