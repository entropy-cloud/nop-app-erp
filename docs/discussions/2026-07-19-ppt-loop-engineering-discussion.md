# PPT 叙事结构与人机协作模式讨论

> 源文件：`docs/ppts/nop-erp-loop-engineering-outline.md`、`docs/ppts/nop-erp-loop-engineering.ppt.html`
>
> 本文件按时间顺序记录讨论过程，每轮包含：问题 → 候选解释 → 结论。

---

## 第一轮：AGE 与 Loop Engineering 的关系

**日期**：2026-07-19

**问题**：当前 PPT 大纲把 Attractor Evolution Cycle 定位为"第三层反馈控制 Loop"，是否正确？

**候选解释**：
- A（原始大纲）：三层嵌套反馈控制 Loop——Agentic Coding Loop / Mission Driver Loop / Attractor Evolution Cycle
- B（AGE 文章原文）：Attractor 不是 Loop，是结构不变量，是为控制 Loop 提供方向的前提。"state space → attractor → trajectory → control" 是一个逻辑依赖链，不是三层 Loop

**结论**：采纳 B。Attractor 不是第三层 Loop。大纲中所有"三环嵌套"视觉和"External Feedback Loop → Attractor Evolution Cycle"的 Andrew Ng 映射均为范畴错误，已全部修正为"双环+轴心"模型。

**未解决问题**：Mission Driver 与业内 Loop Engineering 的关系仍需精确表述。

---

## 第二轮：Mission Driver 的定位

**日期**：2026-07-19

**问题**：Mission Driver 是"Developer Feedback Loop 的自动化版本"吗？

**候选解释**：
- A（原始大纲）：Mission Driver = Developer Feedback Loop 的自动化实现
- B（用户指正）：Mission Driver 是 AGE 的自然发展，只是实践层面接近业内的 Loop。AGE 与 Harness 有本质性区别

**结论**：采纳 B。Mission Driver 是 AGE harness 理念的自然发展，不是从 Loop Engineering 借来的概念。修正了 S14 和附录 B 的表述，将"Developer Feedback Loop 的自动化"改为"AGE harness 层的自然发展，天/周级自动编排"。

---

## 第三轮：时间尺度修正

**日期**：2026-07-19

**问题**：大纲中 Plan Loop（分钟级）、Mission Driver（小时级）的时间标注是否准确？

**数据来源**：logs/2026/ 下 25 个日志文件、docs/plans/ 下约 250 个计划文件

**发现**：
- Plan 生命周期：4-12 小时（同一天），最短 ~4h（07-10 的 4 个计划同日完成），最长 ~2 天
- Mission Driver 单次循环：4-12 小时（处理 1-7 个计划）
- Mission Driver 全路线图遍历：06-22 到 07-13，22 天
- 没有以分钟计的计划——每条都必须经过 draft → 审查 → execute → build 验证 → closure audit

**结论**：Plan Loop = 小时级，Mission Driver = 天/周级。已修正。

---

## 第四轮：Mission Driver 实现架构页

**日期**：2026-07-19

**问题**：是否需要一页介绍 Mission Driver 的实现架构？核心特征是什么？

**结论**：新增 S17。三个关键设计：
1. **注入机制**：AI step 的 prompt 模板含 {{planGuide}} 等占位符，引擎从 mission.json 读取注入
2. **隔离的顶级步骤**：CHECK/REVIEW_PLANS/EXEC_PLANS/DRAFT_PLANS/DEEP_AUDIT 相互不传参数，可断点恢复
3. **零 IPC，全文件**：没有 web server，状态通过 Plan 文件和 Audit 文件持久化

---

## 第五轮：三个 Attractor 的提法是否准确

**日期**：2026-07-19

**问题**："三个吸引子"的提法正确吗？原有"模型驱动/状态一致性/过账契约"是否过于狭窄？

**分析**：AGE 文章对 nop-chaos-flux 的描述是"由少数高价值不变量共同定义"——attractor 是一个，不是三个。原有的"模型驱动/状态一致性/过账契约"是 feature 级别的设计决策，不是结构不变量。

**结论**：改为"一个 Attractor 的三个结构维度"：① ORM XML 唯一真相源（数据空间）② DAG 单向依赖 + R/S/P 跨域契约（模块空间）③ 三轴正交状态分离（业务状态空间）。三者覆盖系统的三个正交空间，共同定义同一个 Attractor。

---

## 第六轮：Human In The Loop vs Human On The Loop

**日期**：2026-07-19

**问题**：S08 用"vibe coding"作为反面案例是否准确？

**候选解释**：
- A（原始大纲）：Vibe coding = 四种缺失（无基线/无范围/无退出标准/无审计）
- B（讨论结论）：Vibe Coding 不是反面案例，它是 Human In The Loop 的一种极端形式。真正要区分的是两种控制模式——人在循环内部 vs 人在循环之上

**进一步问题**：从 Log 看，人随时发起额外审计/计划后，Mission Driver 是否真的会自动拾取？

**日志实证**（25 个日志文件 + 258 份计划）：
- 07-12 竞争审计链条：用户要求"核实 8 个超越点"→ 系统自动审计 → 自动起草 Plan → Mission Driver 自动拾取 → 自动审查执行 → 自动关闭。**用户唯一手动的步骤是第一次提出要求**
- 07-14 闭包审计：自动扫描 206 份 completed Plan → 发现 24 份 deficient → 自动 spawn 24 个子代理重审计

**结论**：
1. 采纳 B：S08 改为 Human In The Loop vs Human On The Loop
2. Plan 是 AI Loop 的基本单位（有边界/基线/退出标准/可审计/可恢复/可编排）
3. Mission Driver 是从 In 到 On 的跨越器，不是 Plan Loop 的锦上添花
4. 已根据此结论修改 S08/S09/S14 的 outline

---

## 第七轮：标题页精简

**日期**：2026-07-19

**问题**：标题页太啰嗦，副标题"从 Harness 到 Attractor——22 天 · 154 模块 · 0 regression 的 ERP 全自动开发"太长，钩子文字"所有 Loop 都在回答'怎么做'"等一大堆。怎么改？

**讨论过程**：
1. 先改为"从 Harness 到 Attractor"→ 删去数字
2. 用户指出副标题改为"中型 ERP 产品的 AI 全自动开发"或类似
3. 尝试"Attractor Guided Loop：中型 ERP 产品的全自动开发"
4. 用户说"Attractor Guided Loop 是 Loop 的一种做法"
5. 提出 5 个方案，用户选"Loop Engineering × Attractor——Attractor Guided Loop 实战"
6. 为底部信息（0 regression 等）提出 5 个子方案，用户问"0 regression 是什么意思？"→ 告知后用户说删掉
7. 标题太长折行，用户提议把"Attractor Guided Loop 实战"放到副标题
8. 最终确定为：

```
Loop Engineering × Attractor
Attractor Guided Loop 实战——中型 ERP 产品的 AI 全自动开发
Nop Platform · AGE 工作流 · 22 天 · 154 模块
```

**结论**：标题为主概念交叉，副标题为"方法——实证"结构，底栏为技术栈和规模数据。去除所有钩子文字和多余数字。

---

## 第八轮：PPT 叙事结构重构

**日期**：2026-07-19

**问题**：当前 PPT 的论证结构有问题——概念先于问题，观众不觉得痛就开始给药。

**当前结构的问题分析**：
1. Act 1 集中引入"Harness""Attractor""Plan Loop""Mission Driver"一堆术语，观众没有"需要这个"的痛点
2. Attractor 在 Act 4，听众已经听了 30 分钟具体内容突然被拉回抽象层，认知切换成本高
3. 22 天全景在 Act 1，数字在概念框架建立前没有意义
4. Plan Loop 的问题（"没有 Loop 会怎样"）只占 1.5 分钟，问题还没痛够就给药

**建议的新结构**（问题驱动，逐层展开）：

```
S01-S02  开场 + 悬念                    ← "谁回答往哪做"
S03-S04  Vibe = Human In The Loop       ← 问题：Infinite Loop
S05-S07  Plan 作为基本单位              ← 方案 1：Plan Loop
S08-S10  Plan 够吗？不够 → MD           ← 方案 2：编排层
S11-S13  22 天数据                      ← 实证
S14-S17  人在做什么 → Attractor         ← 回扣悬念 + 统一解释框架
S18-S20  带走                           ← 收束
```

**关键设计决策**：
1. Vibe Coding 不做反面案例，做"问题起点"
2. Plan Loop 后必须加"Plan 够了吗？不够"的过渡——Mission Driver 的价值不成立
3. Attractor 必须晚于实证——"22 天发生了什么 → 人在其中做了什么 → 因为人在定义 Attractor"
4. 知识转移曲线是全场高潮，不是中间某一页

**状态**：待采纳。当前 outline 仍为旧结构，需根据此结论重写。
