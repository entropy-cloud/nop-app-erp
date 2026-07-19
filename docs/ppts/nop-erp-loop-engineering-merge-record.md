# nop-erp-loop-engineering PPT 选择合并记录

> 从多个版本的 PPT 中选择特定页，合并组织成新的 PPT。

## 源文件一览

| 版本 | 文件 | 总页数 |
|------|------|--------|
| v1 | `nop-erp-loop-engineering.ppt.html` | 32 |
| v2 | `nop-erp-loop-engineering-v2.ppt.html` | 20 |
| v3 | `.gen-variants/nop-erp-loop-engineering-v3.html` | — |

## 合并后 PPT 逐页来源

| 合并后页码 | 源文件 | 原标题/备注 |
|-----------|--------|------------|
| 1 | `nop-erp-loop-engineering-v2.ppt.html#slide-1` | S01 标题页 |
| 2 | `nop-erp-dev-journey-ppt-outline-v4.ppt.html#slide-2` | 原题"一个反直觉的起点"。**改标题为变革点主题**：传统软件工程需要人全程参与；智能时代人如何逐步退出，从 In-the-Loop 到 On-the-Loop；nop-app-erp 作为一个实验性项目，验证这一过程。保留双栏对比结构（左=传统信念，右=本项目的证据），内容按新主题调整 |
| 3 | `nop-erp-dev-journey-v2.ppt.html#slide-3` | "nop-app-erp 是什么"——三圈交叠（Nop Platform / AGE 工作流 / ERP 应用层），一句话定位，关键指标速览（18+1 域 / 154 模块 / 352 实体 / 22 天），直接使用 |
| 4 | `nop-erp-dev-journey-v2.ppt.html#slide-4` | "起点：06-22 上午的真实状态"——AGE 模板空骨架，10 域多数实体为 0，构建未闭环，测试 = 0，直接使用 |
| 5 | `nop-erp-dev-journey-synthesis.ppt.html#slide-6` | "22 天后：经审计校准的关键规模指标"——8 项 stat 展示产物（自有实体 352+110、154 模块、~2890 测试、260+ E2E、24+10 报表看板等），直接使用 |
| 6 | `nop-erp-loop-engineering.ppt.html#slide-3` | S03 从 Prompt → Loop → AGE。业内发展方向：Cherny/Osmani/Andrew Ng 的 Loop Engineering 共识；AGE 在 Harness 层之上的补充——方向定义层（Attractor）。内容本身已吻合"发展方向 + AGE 补充增强"主题，可直接使用 |
| 7 | `nop-erp-loop-engineering.ppt.html#slide-8` | 原题"没有 Loop 会怎样"。**改标题为"Vibe Coding Loop"**：传统无 Loop 开发的四大缺失（无基线/无范围/无退出标准/无审计），对应 Vibe Coding 模式下人类 In-the-Loop 的不可持续问题 |
| 8 | `nop-erp-loop-engineering.ppt.html#slide-9`（主体）+ `nop-erp-loop-engineering-v2.ppt.html#slide-5`（标题+结尾总结） | **标题替换**：v1 原题"Plan 作为关闭契约" → 使用 v2#slide-5 标题"从 In 到 On 的关键：Plan"。**结尾总结替换**：v1 的 final highlight box 替换为 v2#slide-5 底部 Harness 定义总结（"生成与验收分离" + "Harness——控制轨迹的执行层"）。保留 v1 slide-9 的 Plan Loop 流程图主体 |
| 9 | `nop-erp-loop-engineering-v2.ppt.html#slide-6` | S06 实例：Plan 1100-1 销售定价引擎。真实 Plan 执行实录 + 4 个 P0 缺陷在编码前拦截，直接使用 |
| 10 | `nop-erp-loop-engineering.ppt.html#slide-14` | S14 "为什么需要更大一层的 Loop"——Plan Loop 只管理单次变更，需要 Mission Driver 作为编排层实现天/周级自动循环，直接使用 |
| 11 | `nop-erp-loop-engineering.ppt.html#slide-16` | S16 "Mission Driver 的 5 步闭环"。**概念重构**：原图分三层（第一层 5 步闭环、第二层 EXEC 展开、第三层 DRAFT 展开），改为"一个主流程 + 两个展开子流程"——主流程为 5 步闭环（CHECK→REVIEW→EXEC→DRAFT→DEEP_AUDIT），EXEC 展开和 DRAFT 展开作为主流程中对应步骤的下钻展开图，而非并列的"层" |
| 12 | `nop-erp-loop-engineering.ppt.html#slide-17` | S17 "Mission Driver 的实现架构——通用 Flow DSL 引擎"。**移除**顶部 italic speaker note 段落。**保留**四层架构图 + 三个关键设计卡片。**新增**"对人介入的支持"小节：DRAFT_PLANS 持续从路线图起草新 Plan（不限批次）；REVIEW_PLANS 自动拾取任何 Status=draft 的 Plan（不区分来源——手工创建或自动起草都一样）；人可随时修改 Plan 状态/路线图，下轮循环自动适配 |
| 13 | `nop-erp-loop-engineering.ppt.html#slide-15` | S15 "真实运行记录：07-10 一次完整循环"——Mission Driver 运行时间线（CHECK→REVIEW→EXEC→DRAFT→DEEP_AUDIT，08:00→16:05），直接使用 |
| 14 | `nop-erp-dev-journey-comprehensive.ppt.html#slide-13` | "DEEP_AUDIT——系统不自满"——审计流程（MULTI_AUDIT→OPEN_AUDIT→DRAFT_FROM_AUDITS），07-07 实例（4 严重+6 高，1-3 天全部闭合），核心："全 done 不是停止信号，是升级审计的信号" |
| 15 | `nop-erp-loop-engineering.ppt.html#slide-19` | S19 "两层 Harness 的关系——以及它们不能做的事"——Plan Loop vs Mission Driver Loop 的职责边界，引出 Attractor 的必要性（方向定义层），直接使用 |
| 15 | `nop-erp-loop-engineering.ppt.html#slide-21` | S21 "动力系统语言——为什么需要 Attractor"——状态空间→吸引子→轨迹→控制四层本体，三种常见混淆（≠边界/≠护栏/≠控制目标），直接使用 |
| 16 | `nop-erp-dev-journey-comprehensive.ppt.html#slide-18` | "用户其实在做什么——定义和演进吸引子"——A/B/C 三类介入映射到吸引子不同层面，知识转移曲线，用户后期介入归零不是因为 AI 学会写代码而是因为吸引子已定义好，直接使用 |
| 17 | `nop-erp-dev-journey-ppt-outline-v4.ppt.html#slide-4` | "分类框架：28 条干预的构成"——饼图分解 28 条人类干预：质量门控 43%/平台机制 36%/架构质疑 10.5%/战略方向 3.5%/其他 7%，核心主张：约 90% 为通用可编码类型 |
| 18 | `nop-erp-dev-journey-ppt-outline-v4.ppt.html#slide-5` | "典型实例速览——两类最密集的纠正"——平台机制纠正与质量门控纠正的具体实例表格，每例可知来源为公开文档/源码/通用工程原则 |
| 19 | `nop-erp-dev-journey-ppt-outline-v4.ppt.html#slide-6` | "grill-with-docs 80 个设计决策——92.5% 接受 AI 推荐"——柱状图：74 次选 A (92.5%) / 3 次选 B / 2 次选 C / 1 次选 D（AI 未覆盖），关键案例 Q71 人类提出新方向 |
| 20 | `nop-erp-dev-journey-synthesis.ppt.html#slide-28` | "外部注入三层模型"——A/B/C 金字塔三层模型及典型实例（A 类修正技术基底 / B 类指明方向 / C 类在吸引子内扩张） |
| 21 | `nop-erp-dev-journey-synthesis.ppt.html#slide-29` | "A 类全部 9 次速览表"——完整列表：日期/纠正对象/平台机制/AI 犯错原因，共同模式：全部是 docs-for-ai 未覆盖的平台细节，06-22~07-03 后 A 类消失 |
| 22 | `nop-erp-dev-journey-synthesis.ppt.html#slide-30` | "B 类案例：两次关键 B 类（07-01）"——过账引擎 DDD 聚合根原则 + 架构文档元规则，核心："用户不告诉答案，只告诉方向" |
| 23 | `nop-erp-dev-journey.ppt.html#slide-10` | "A 类案例（一）：全局租户开关"——AI 写法（逐域声明）vs 用户校正（全局配置），模式提炼：配置层隐藏机制是 docs 经常遗漏的知识 |
| 24 | `nop-erp-dev-journey-synthesis.ppt.html#slide-31` | "知识转移曲线"——另一版本的双 Y 轴知识转移曲线，标注"全 PPT 最核心的一页" |
| 25 | `nop-erp-dev-journey-v2.ppt.html#slide-6` | "8 阶段演进 (06-22 → 07-13 / 22 天)"——Gantt 图展示 8 阶段开发节奏：S1 设计期→S2 深化→S3 核心逻辑→S4 扩展域→S5 业财+运营→S6 审计→S7 E2E→S8 收尾，直接使用 |
| 25 | `nop-erp-dev-journey-v2.ppt.html#slide-12` | "【全场高潮】知识转移曲线"——双 Y 轴折线图（A 类频次↓ + AI 自主度↑），关键事件标注（grill 83 问、最后 A 类等），核心句："用户不是在写代码，而是在转移平台知识"，直接使用 |
| 26 | `nop-erp-dev-journey.ppt.html#slide-14` | "知识转移曲线"——另一版本，柱状图+折线图组合，底部三阶段总结（早期 A 类密集 / 中期 B 类 / 后期全 C），直接使用 |
| 27 | `nop-erp-dev-journey-v2.ppt.html#slide-14` | "六种自驱动机制"——内部驱动机制总结：计划-审计闭环 / 路线图驱动 / 审计-整改驱动 / 测试回归防护 / 竞品对标驱动 / 文档一致性驱动，直接使用 |
| 28 | `nop-erp-dev-journey-v3.ppt.html#slide-22` | "审计方法论 8 阶段演进"——从单代理→多路并行→对抗性子代理→工具化合规检查→UC 断言对照→自省的演进，控制 Loop 自身的元进化，直接使用 |
| 29 | `nop-erp-dev-journey.ppt.html#slide-16` | "注入来源全景图"——外部知识注入四来源（开源 ERP 对标 / 独立子代理审计 / mission-driver / 竞品调研背书），设计来源明确标注（凭证←赤龙、库存←Odoo 等） |
| 30 | `nop-erp-dev-journey-ppt-outline-v4.ppt.html#slide-14` | "对 AI 自主开发研究的含义"——AI 可自主完成从 ORM 到 E2E 的全栈开发实证；四条件（业务规则通用/不依赖企业偏好/参照系公开/正确性可由内在一致性论证），总结反思 |
| 31 | `nop-erp-dev-journey-ppt-outline-v4.ppt.html#slide-16` | "从 28 条干预到全自动的最后一公里"——三个基础设施：平台知识注入 agent / 质量深度门控 agent / 领域知识注入 agent，展望方向 |
| 32 | `nop-erp-loop-engineering-v2.ppt.html#slide-18` | S18 "全场架构总图"——作为 Takeaway 总结：Attractor 方向层 + Harness 控制层全貌，与 Andrew Ng 三层 Loop 对比，核心句"Loop 让 AI 跑得更快，Attractor 让 Loop 知道往哪里跑" |
| 33 | `nop-erp-loop-engineering-v2.ppt.html#slide-19` | S19 "三个 Takeaway"——Takeaway 1：把"变更"从对话变成契约（Plan Loop）/ Takeaway 2：把"编排"从记忆变成自动（Mission Driver）/ Takeaway 3：把"方向"从个人变成文件（Attractor），直接使用 |
