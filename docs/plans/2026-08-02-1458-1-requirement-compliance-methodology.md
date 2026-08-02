# 2026-08-02-1458-1 requirement-compliance-methodology 需求-实现符合性审计方法论文档

> Plan Status: completed
> Last Reviewed: 2026-08-02
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item 0.1（M0 审计编排基线）
> Related: `docs/discussions/2026-08-02-1700-requirement-implementation-compliance-audit.md`（Q1/Q4 已裁决）、`docs/backlog/audit-remediation-roadmap.md`（MQ 文档先行范式先例）
> Audit: required

## Current Baseline

- **目标产物不存在**：`docs/audits/requirement-compliance-methodology.md` 尚未创建（已用 glob 确认）。本计划创建它。
- **roadmap 已就绪**：`docs/backlog/requirement-compliance-roadmap.md` v1.3 已是可执行版本——Q1/Q4 已裁决（2026-08-02），M0 工作项全部 `ready`。本计划是 mission `requirement-compliance` 的**首个** plan（DRAFT_PLANS 首轮，无前置 plan）。
- **裁决输入已定案**（讨论文档 `2026-08-02-1700`）：
  - **Q1=(c)**：需求真相源 = `product-scope.md`（顶层范围）+ 各域 `use-cases.md`（192 UC 功能契约，权威）；owner doc 为设计参考；三者逐项对照，冲突时**一律以需求真相源为准**，推定 owner doc 已向实现妥协（除非有显式人工批准登记）。
  - **Q4=(a) 无例外**：P0/P1 需求分歧**必须实现**，**禁止方案 B（documented simplification / Deferred）关闭**；无"技术不可行"例外通道（技术不可行项须更深设计变更而非退缩到方案 B）；唯一出口 = 审计发现需求本身不合理时经人工批准修改 product-scope（需求变更非降级）。
- **方法论文据先例**（本仓已落地的同类范式）：
  - `docs/skills/audit-remediation-roadmap-authoring-prompt.md` —— MQ 文档先行范式（步骤化方法论 + 自检清单 + 产物清单 + 命名规范 + 归档纪律）。0.1 的方法论文档**沿用其骨架风格**（用途/前置阅读/方法论步骤/分级判据/归档规范/自检清单）。
  - `docs/skills/multi-dimensional-audit-prompt.md` / `open-ended-audit-prompt.md` / `closure-audit-prompt.md` —— MA1/MA2-MA3/V.3 将复用的审计提示；方法论文档的"报告输出格式"与"过程纪律自检段"须与这些提示对齐（不重复其正文，仅引用 + 衔接）。
  - `docs/audits/00-audit-execution-guide.md` —— 审计执行通用规范，方法论文档引用而不重写。
- **既有审计产物（去重输入，方法论须定义衔接规则）**：
  - `docs/audits/arm-index.md`（292 条 finding，方案 B 关闭项 + successor 声明内嵌于此）—— 方法论须定义 **P1-RC-xxx 命名** + **"复用 or 新增"裁决规则** + **MA1 报告开头声明"与 MA2 报告差异增量"** 的格式。
  - `docs/audits/2026-07-2*-arm-ma2-*.md`（既有 MA2 状态机/业财链路行为审计报告，多份）—— 已证实的状态机/链路行为作为**既有证据输入**，MA1 只补"需求契约↔实际行为"差异，不重新核实行为本身。方法论须显式定义此去重协议。
  - `docs/audits/compliance-baseline.md` + `docs/audits/nop-compliance-checker.sh`（19 规则 checker）—— 方法论的"过程纪律自检段"须定义 **checker 退出码门控核查**（区分门控退出码 vs 纯 reporter 退出码，吸取 R6.9 误判教训）。
- **保护区域**：本工作项为**纯文档**（方法论 markdown），不触及 ORM/会计过账/数据删除代码。属 roadmap 预授权类目（"文档更新类修复：预授权自动执行"）。无需 ask-first。
- **剩余差距**：方法论文档缺失 = M0.2/M0.3/MA1-MA4 全部阻塞（它们 Deps 含 0.1）。本计划解除 0.2/0.3 的 DRAFT_PLANS 阻塞。

## Goals

- 创建 `docs/audits/requirement-compliance-methodology.md`，作为 `requirement-compliance` mission 的 **Phase 1 契约**（仿 MQ Q0-Q7 文档先行范式），覆盖：
  1. **五级追踪矩阵模板**：use-case → owner doc 契约 → 代码路径 → 测试断言 → 运行时行为（含每级证据格式与判读标准）。
  2. **P0/P1/P2/接受 分级判据**（绑定 Q4：P0/P1 强制实现禁方案 B；P2 登记不强制）。
  3. **完整枚举纪律**：工作项 = 功能切片 × 显式 UC 清单，**禁止合并/抽样**（明确写出反例与判定）。
  4. **Q1 真相源层级与冲突裁决规则**（含"显式人工批准记录"三判据证据标准）。
  5. **Q4 修复义务与保护区域暂停协议**（ORM/会计/删除三类 ask-first 门控 + 触及行暂停机制 + 预授权类目清单声明）。
  6. **报告输出格式**：MA 报告必含段落（需求契约原文/实现证据/测试证据/运行时行为证据/符合性结论/与 arm-index 衔接/静态存疑点清单/过程纪律自检段/与 MA2 报告差异增量声明）。
  7. **与 arm-index 命名衔接**（P1-RC-xxx 命名规则 + "复用 or 新增"裁决规则 + 双向可追溯要求）。
  8. **审计过程纪律自检段**（checker 退出码门控核查 + closure-audit 独立性声明 + 与 arm-index 交叉去重声明）。
  9. **真相源冻结条款**：审计期间 product-scope/use-cases/owner doc 需求契约修订须经人工批准并登记；审计发现的 doc 分歧记入报告，不直接改真相源。
  10. **MR0 即时通道 + MR1 展开器机制声明**（P0 发现即止血；R1.0 自动展开为 RC-R1.n，对齐 audit-remediation R*.0 范式）。
- 方法论文档经**独立子代理 ≥2 轮审查**收敛（双视角：① 规范合规 ② 覆盖面/可执行性），记录于本计划 `## Draft Review Record`。

## Non-Goals

- **不执行任何 MA1-MA4 审计工作项**（它们 Deps 含 0.2/0.3，本轮不触及）。
- **不修改 product-scope / use-cases / owner doc**（真相源冻结条款本身禁止；产品需求真相源的修订属 0.2 范围，且须人工批准）。
- **不导出 arm-index 方案 B 清单或 successor 清单**（属 0.3 范围；方法论仅定义导出口径与分区约束规则，不执行导出）。
- **不创建 mission.json**（mission `requirement-compliance` 的 json 配置不在 0.1 范围；如尚未存在由 mission driver 侧处理）。
- **不修改任何 ORM/api.xml/BizModel/Processor/view.xml**（纯文档工作项）。

## Task Route

- Type: `app-layer design change`（方法论是 mission 级审计契约的设计文档；属应用层方法学设计变更，非实现变更）
- Owner Docs: `docs/backlog/requirement-compliance-roadmap.md`（M0.1 工作项定义与 Work Item Details）+ `docs/discussions/2026-08-02-1700-requirement-implementation-compliance-audit.md`（Q1/Q4 裁决原文）+ `docs/skills/audit-remediation-roadmap-authoring-prompt.md`（文档先行范式先例）+ `docs/audits/00-audit-execution-guide.md`（审计执行通用规范）
- Skill Selection Basis: `Skill: none`（无 opencode 技能匹配"编写审计方法论文档"。内容输入引用 `docs/skills/multi-dimensional-audit-prompt.md`/`open-ended-audit-prompt.md`/`closure-audit-prompt.md` 作为方法论须对齐的审计提示，但这些是**内容参照**而非工作方法选择器——本工作的方法是"参照 MQ 文档先行范式编写"，由 roadmap 工作项明确指定，无需加载 opencode 技能）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 纯 markdown 文档，无端口/环境变量/密钥/外部服务依赖。

## Execution Plan

### Phase 1 - 编写方法论文档

Status: completed
Targets: `docs/audits/requirement-compliance-methodology.md`（新建）
Skill: none

- Item Types: `Add`
- Prereqs: 无（首个 plan，前置阅读已完成于本计划 Current Baseline）

- [x] 创建 `docs/audits/requirement-compliance-methodology.md`，沿用 `audit-remediation-roadmap-authoring-prompt.md` 的骨架风格（用途/前置阅读/方法论步骤/分级判据/归档规范/自检清单），纳入 Goals 列出的全部 10 项必备内容：
      - 五级追踪矩阵模板（含每级证据格式：需求契约原文引用格式、代码路径引用格式 `file:line`、测试断言引用、运行时行为证据来源 `tests/e2e/`+探针）
      - P0/P1/P2/接受 四级分级判据表（绑定 Q4：P0/P1 强制实现禁方案 B 无例外通道；唯一出口=需求不合理经人工批准改 product-scope）
      - 完整枚举纪律段（工作项=功能切片×显式UC清单；反例：合并切片/代表性抽样均视为未完成）
      - Q1 真相源层级与冲突裁决规则（product-scope+use-cases 权威 > owner doc 参考；冲突一律真相源为准；"显式人工批准记录"三判据：(i)plan含独立plan-audit通过 (ii)owner doc显式documented simplification标注经人工批准 (iii)product-scope范围裁剪登记）
      - Q4 修复义务 + 保护区域暂停协议（ORM/会计/删除三类 ask-first 门控；触及行 mission driver 暂停等待人工批准；非触及行继续；预授权类目清单声明）
      - 报告输出格式模板（MA报告必含9段落骨架）
      - 与 arm-index 命名衔接（P1-RC-xxx 命名 + "复用or新增"裁决规则：同根因同控制点→复用既有ID追加RC交叉引用；新根因/新功能点/新维度→新建P1-RC-xxx；每报告须grep arm-index同域同控制点后给裁决）
      - 审计过程纪律自检段模板（checker退出码门控核查——区分门控退出码vs纯reporter退出码，吸取R6.9教训；closure-audit独立性声明；与arm-index交叉去重声明）
      - 真相源冻结条款（审计期间product-scope/use-cases/owner doc需求契约修订须人工批准并登记；doc分歧记入报告不直接改真相源）
      - MR0即时通道+MR1展开器机制声明（P0发现即止血走独立fix plan；R1.0自动展开为RC-R1.n，对齐audit-remediation R*.0范式）
      - Skill: none

Exit Criteria:

> 本阶段交付单一可观察结果：方法论文档落盘且含全部必备段落。下游 0.2/0.3/MA1 依赖此文档存在，故验证 = 段落完整性自查（非完整仓库构建——本计划无代码变更，完整验证属 Closure Gates）。

- [x] `docs/audits/requirement-compliance-methodology.md` 存在，且包含 Goals 列出的 10 项必备内容各自成段（段落标题可逐项定位）—— 实测 §1-§10 共 10 段落标题可定位
- [x] 文档内嵌五级追踪矩阵模板表（5 列）与 P0/P1/P2/接受 分级判据表（4 行，含 Q4 修复义务绑定列）—— 实测 §1 矩阵 5 列（列/名称/内容/证据格式/判读标准）+ §2 判据表 4 行（P0/P1/P2/接受）含 Q4 修复义务绑定列

### Phase 2 - 独立子代理审查（≥2 轮，双视角）

Status: completed
Targets: `docs/audits/requirement-compliance-methodology.md`（按审查反馈修订）
Skill: none

- Item Types: `Proof`
- Prereqs: Phase 1 完成（文档已落盘）

- [x] 独立子代理审查第 1 轮 —— **规范合规视角**：新生成子代理会话（不重用执行者上下文），核对方法论文档是否符合 roadmap M0.1 工作项定义 + Work Item Details + 讨论文档 Q1/Q4 裁决原文 + plan-authoring-guide 规范；输出 `needs revision`（列确切的缺失段落/裁决偏离）或 `acceptable`。执行者按反馈修订文档。
      - Skill: none
- [x] 独立子代理审查第 2 轮 —— **覆盖面/可执行性视角**：新生成子代理会话，核对方法论是否可被 MA1-MA4 工作项直接消费（五级矩阵模板是否可填、分级判据是否无歧义、归档衔接规则是否可执行、保护区域暂停协议是否在无人值守 driver 下可操作、checker 门控核查是否区分了门控vs reporter退出码）；输出 `needs revision` 或 `acceptable`。执行者按反馈修订文档。
      - Skill: none
- [x] 两轮审查均收敛为 `acceptable`（若任一轮 `needs revision` 则修订后重跑该视角直到收敛；审查证据记入 `## Draft Review Record`）
      - Skill: none

Exit Criteria:

- [x] 第 1 轮（规范合规）审查证据已记录且结论为 acceptable —— 见 `## Draft Review Record` Round 1（ses_03eb13c45ffeqC1LkJGCujr3Z9，10 项检查 A-J 全 PASS：roadmap M0.1 合规 / Q1=(c) 忠实 / Q4=(a) 无例外通道无后门 / 10 必备项齐 / Non-Goals 守约 / 9 段落格式 / P1-RC-xxx 命名 / checker 退出码区分 / 保护区域暂停协议 / MR0+MR1 机制）
- [x] 第 2 轮（覆盖面/可执行性）审查证据已记录且结论为 acceptable —— 见 `## Draft Review Record` Round 2（ses_03eb0f2aeffeFTrWxin0FNOds7，10 项检查 A-J 全 PASS：五级矩阵可填 / 分级判据无歧义 / arm-index 复用or新增可执行 / 保护区域暂停协议无人值守可操作 / checker 门控区分正确且实证 / 9 段落可消费 / 去重协议可操作 / MR0+MR1 与 roadmap 一致 / 真相源冻结可执行 / 引用文件实测有效；5 项 minor non-blocking 已由安全方向默认吸收或经记录理由审计）
- [x] 方法论文档已按审查反馈完成修订（无未处理的阻塞意见）—— Round 2 标记的 finding 计数陈旧（§前置阅读 "292 条" 实测 313）已软化措辞为"以 0.3 导出实测为准"，消除陈旧数字断言

## Draft Review Record

> 区分两类审查：**本段** = plan 级草案审查（审查 plan 契约本身，收敛后 `Plan Status` 转 `active`）；**Phase 2** = roadmap M0.1 要求的方法论文档**内容**审查（≥2 轮独立审查，审查**已落盘的文档**，证据回填 Closure Gates）。两者不是同一活动，无循环依赖：先审查 plan 契约 → 转 active → 执行 Phase 1 落盘文档 → 执行 Phase 2 审查文档内容。

- Independent draft review iteration 1: `acceptable`（独立子代理 ses_03eb839efffeAeak9lyodjtEQJ，fresh session）。审查 plan 契约：Deps 正确性（仅 draft 0.1，0.2/0.3 正确排除）、Baseline 准确性（实时核实方法论文档不存在 + arm-index/MA2报告/compliance基线/审计提示均存在）、10 项必备内容完整性、退出标准与结束门控（doc-only 正确删除 build/test 门控）、单结果表面、状态一致性、反模式——全 PASS。两项非阻塞残留：(1) 原 Draft Review Record 措辞将 plan 契约审查与文档内容审查混为一谈（本次修订已通过区分两类审查消除）；(2) Phase 2 统一标 `Proof` 但修订子步实为 `Fix`（保留：阶段为审查主导，统一 `Proof` 可辩护）。

### Phase 2 文档内容审查证据（roadmap M0.1 要求的 ≥2 轮独立审查）

- **Phase 2 Round 1（规范合规视角）**：`acceptable`（独立子代理 ses_03eb13c45ffeqC1LkJGCujr3Z9，fresh session，不重用执行者上下文）。10 项检查 A-J 全 PASS：(A) roadmap M0.1 工作项合规——§1/§2/§3/§5/§6/§7/§8/§9 八项 M0.1 内容义务全覆盖；(B) Q1=(c) 忠实——§4 层级表 product-scope+use-cases 权威 / owner doc 设计参考，冲突一律真相源为准，三判据 i/ii/iii 与 roadmap MA2 详情逐字一致含"AI 自写标注不算"+兜底顺序，notify 补写 use-cases 不标 N/A；(C) Q4=(a) 无例外通道无后门——§2/§5 枚举禁止关闭方式（方案B/技术不可行降级/representative enough/owner doc 自圆），技术不可行→更深设计变更（billR 判别列例），唯一出口=需求变更非降级，会计/数据安全无例外，取最高原则关闭 P0/P1→P2 降级后门，既有 arm-index P0 deferred 边界为范围声明非方案B通道；(D) 10 必备项 1:1 映射 §1-§10；(E) Non-Goals 守约（只定义方法论不执行）；(F) 9 段落格式与 plan item 6 逐字一致；(G) P1-RC-xxx 命名 + 复用or新增规则完整；(H) checker 退出码区分正确（脚本恒0纯reporter / CI workflow sys.exit(1)真门控）；(I) 保护区域暂停协议可操作；(J) MR0+MR1 机制对齐 R*.0 范式不预注册占位行。无裁决偏离、无例外通道后门。
- **Phase 2 Round 2（覆盖面/可执行性视角）**：`acceptable`（独立子代理 ses_03eb0f2aeffeFTrWxin0FNOds7，fresh session）。10 项检查 A-J 全 PASS（5 项含 minor non-blocking 备注，均由安全方向默认吸收或经记录理由审计，不阻塞执行代理）：(A) 五级矩阵可填——证据格式具体（file:line/TestFile#method/e2e spec#describe），L5 三源（MA2报告/tests+helper/探针）无歧义；(B) 分级判据无歧义——4 行表+取最高+示例 MECE，Q4 修复义务绑定列无例外通道；(C) arm-index 复用or新增可执行——grep-before-create 强制 + 双向可追溯完全规定（minor: grep 字面词未给算法，代理按域+实体/方法词推断，可审计）；(D) 保护区域暂停协议无人值守可操作——4 步机制 + ask-first 默认吸收误分类（minor: 触及检测启发式未给算法，类别→文件类型可推断）；(E) checker 门控区分正确且**实证**——实测 `nop-compliance-checker.sh` 无 exit 调用恒 0、`compliance.yml:96` sys.exit(1)、`compliance-baseline.md:61/286` 证实 option b + R6.9 误判记录（最强段落）；(F) 9 段落可消费；(G) 去重协议可操作（minor: 无切片→MA2报告映射表，代理 glob 域名匹配，20 份报告实测可行）；(H) MR0+MR1 与 roadmap 逐行一致，done=展开完成跨 R1.0/A4.1/A4.2 三处一致；(I) 真相源冻结可执行——不编辑默认使其可执行（minor: owner doc 内冻结/非冻结段落边界语义模糊，但不操作危险）；(J) 引用文件实测有效——arm-index(587行)/compliance-baseline/M0锚点/R6.9误判记录/检查器纯reporter/审计提示全存在（minor staleness: §前置阅读 "292 条" 实测 313——**已修订**：软化措辞为"以 0.3 导出实测为准"消除陈旧数字断言）。
- **收敛结论**：两轮均首轮 `acceptable`，无 `needs revision`，无需重跑。Round 2 唯一被采纳的修订 = finding 计数陈旧软化（已落地）。其余 4 项 minor non-blocking 经评估均为安全方向默认吸收或记录理由审计，不构成阻塞。

## Closure Gates

> 本计划为**纯文档**工作项（无代码/ORM/api.xml/view.xml 变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控——方法论文档不触发编译或测试。验证 = 文档段落完整性 + ≥2 轮独立审查收敛 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：`docs/audits/requirement-compliance-methodology.md` 存在且含 Goals 全部 10 项必备内容（§1-§10 共 10 段落标题可定位，closure audit 实测 446 行）
- [x] 相关文档对齐：方法论与 roadmap M0.1 工作项定义、讨论文档 Q1/Q4 裁决、audit-remediation 文档先行范式先例一致（closure audit check 3 PASS：Q1=(c) 三层级+冲突规则+三判据+notify；Q4=(a) 无例外通道无后门）
- [x] 已运行验证：文档段落完整性自查（五级矩阵模板表 5 列 + 分级判据表 4 行含 Q4 绑定列存在；10 项必备段落标题可定位）——本计划无代码变更故不跑 build/test（closure audit check 2 PASS）
- [x] 无范围内项目降级为 deferred/follow-up（mission.json 经 `## Deferred But Adjudicated` 显式分类为 out-of-scope improvement + Successor Required，非静默降级）
- [x] 独立草案审查已完成并记录（Draft Review Record 两轮收敛：Round 1 ses_03eb13c45ffeqC1LkJGCujr3Z9 规范合规 acceptable / Round 2 ses_03eb0f2aeffeFTrWxin0FNOds7 覆盖面可执行性 acceptable，均首轮收敛无 needs revision）
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致（closure audit check 9 PASS：Phase 1/2 completed 全 [x]，Draft Review Record 收敛，Closure Gates/Plan Status/Closure 互洽）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（closure audit ses_03eada4abffeRLd41z5fuOCltf，fresh session，11 项检查全 PASS）
- [x] 结束证据存在于文件中（见 `## Closure` `Closure Audit Evidence` 段）

## Deferred But Adjudicated

### mission.json 创建/校验

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: mission `requirement-compliance` 的 json 配置不属于 roadmap 工作项 0.1 范围（0.1 产物是方法论文档）；mission.json 由 mission driver 侧或独立工作项处理。
- Successor Required: yes（若 mission driver 启动前发现 mission.json 缺失，由 mission driver 流程兜底；不阻塞方法论文档的交付）

## Closure

Status Note: 方法论文档 `docs/audits/requirement-compliance-methodology.md` 已落盘（446 行，§1-§10 全 10 项必备内容）；Phase 2 双视角独立审查首轮均收敛 acceptable（规范合规 + 覆盖面/可执行性）；独立结束审计 11 项检查全 PASS（CLOSURE VERDICT: passes closure audit）；解除 0.2/0.3 的 DRAFT_PLANS 阻塞。本计划为纯文档工作项，无代码/ORM/api.xml/view.xml 变更，故无 build/test 验证（adjudicated gate set）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 ses_03eada4abffeRLd41z5fuOCltf（fresh session，不重用执行者上下文）
- Walkthrough: 11 项检查逐项核对实时仓库 —— (1) 交付物存在含 §1-§10 全 10 段落；(2) 五级矩阵 5 列表 + 分级判据 4 行含 Q4 绑定列；(3) Q1=(c) §4 忠实（三层级+冲突规则+三判据+notify）+ Q4=(a) §2/§5 忠实无例外通道后门；(4) Non-Goals 守约（只定义方法论不执行）；(5) Phase 1/2 全 [x]（grep 实测剩余 [ ] 仅在 Closure Gates，已本审计回填）；(6) Phase Status 均 completed；(7) Draft Review Record 含双视角审查证据 + session ID；(8) Plan Status 本审计前=active（符合 closure 时序）；(9) 文本一致性 PASS；(10) Deferred mission.json 显式分类 + Successor；(11) roadmap M0.1=ready（closure 后由执行者翻 done）
- Verdict: `passes closure audit`

Follow-up:

- 0.2（需求基线提取与清单化）与 0.3（存量清单导出）在本计划 done 后进入 DRAFT_PLANS 可起草范围（其 Deps=0.1 满足）—— 由 mission driver 下一轮 DRAFT_PLANS 处理，不在本计划范围内。
