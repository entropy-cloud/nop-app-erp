# 2026-08-02-1530-1 requirement-baseline-extraction 需求基线提取与清单化

> Plan Status: completed
> Last Reviewed: 2026-08-02
> Mission: requirement-compliance
> Work Item: 0.2（M0 审计编排基线 — 需求基线提取与清单化）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item 0.2
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 已 done，解除 0.2 阻塞）
> Audit: required

## Current Baseline

- **方法论契约已就绪**：`docs/audits/requirement-compliance-methodology.md` 已落盘（M0.1 done）。§1 定义五级追踪矩阵模板；§4 定义 Q1=(c) 真相源层级（product-scope + use-cases 权威 > owner doc 参考）；§4 notify 特例要求"0.2 必须补写完整 use-cases（不标 N/A）"；§9 真相源冻结条款。本计划是方法论之后的**首个基线建立**工作项。
- **product-scope 陈旧段已大幅修复**：`docs/requirements/product-scope.md:50-73` "当前里程碑" 段曾由 P1-MA3-011（dim 4）标记陈旧，**已于 2026-07-31 由 R2.2 修复**（plan `2026-07-31-0010-2`，milestone/status/成功指标对齐 AGENTS.md，见 product-scope.md:52 对齐说明）。剩余陈旧为**残余事实性漂移**（如成功指标行 `1902 单元测试` vs roadmap 基线 1903、域级能力描述可能与 use-cases 错位）——非里程碑框架陈旧。0.2 仅做残余核实与事实性修正，**不触碰需求契约内容**（§9 冻结条款纪律，分歧记入报告不直改真相源）。
- **18 域 use-cases 已存在，格式基本统一**：18 个 `docs/design/<domain>/use-cases.md` 均存在，多数采用 `## UC-XXX-NN 标题` heading 格式（如 finance/use-cases.md:16 `## UC-FIN-01 业财自动过账`）。roadmap 统计 192 UC 拆为 50 个 UC 切片 + 1 个 notify 切片 = 51 行（MA1 表）。
- **logistics 存在 3 个非标准 heading**：`docs/design/logistics/use-cases.md:41/53/79` 为 `## 用例四：…`/`## 用例五：…`/`## 用例六：…`（对应 UC-LOG-04/05/06），与其余 `## UC-LOG-NN` heading 不一致。0.2 须归一化（方法论文档 §3 要求行内 UC 编号与 use-cases.md 标题一一对应，不可跳号；非标准 heading 会破坏 MA1 逐 UC 核对）。
- **notify 无 use-cases.md**：`docs/design/notify/` 仅有 `README.md` + `inbox-patterns.md`，**无 use-cases.md**。Q1 裁决（讨论文档 2026-08-02-1700）：notify 是已实现跨域子系统，**必须补写完整 use-cases**（不标 N/A），补写后纳入 MA1 A1.51 切片，可能新增切片行。notify 需求契约来源：`notify/README.md`（定位/边界/核心业务对象）、`notify/inbox-patterns.md`（收件箱前端范式）、`docs/architecture/notification-strategy.md`（通知类型/频控/通道策略权威）。
- **工具与技能就绪**：`tools/use-case-map.cjs`（解析 action-auth.xml 菜单→UC 对照 + 完备性检查 + 域级概览）存在；`docs/skills/design-completeness-scan-prompt.md`（roadmap 为 0.2 指定的技能）存在。
- **五级追踪矩阵骨架不存在**：MA1 各切片报告需填充五级矩阵（§1），但作为统一入口的"矩阵骨架 + UC 清单权威表"尚未建立。0.2 初始化它。
- **保护区域**：本工作项以**纯文档/清单**为主（use-cases 归一化、notify use-cases 创建、UC 清单产出、矩阵骨架初始化）。product-scope 仅残余事实性修正。**不触及 ORM/api.xml/BizModel/Processor**。属 roadmap 预授权类目（"文档更新类修复：预授权自动执行"）；但 product-scope 是顶层需求真相源，0.2 对其仅做事实性核实，**任何需求契约分歧记入产出的"基线分歧登记"段交 MA1，不直改真相源**（§9 冻结条款）。
- **剩余差距**：UC 权威清单 + 切片覆盖复校 + notify use-cases + logistics 归一化 + 矩阵骨架全部缺失 = MA1 A1.1-A1.51 全部阻塞（Deps 含 0.2）。本计划解除 MA1（及 MA2 的 0.2 依赖）的 DRAFT_PLANS 阻塞。

## Goals

- 产出 **UC 权威清单表**（域 × UC 编号 × 标题 × 功能点 × use-cases.md 定位行），覆盖 18 域 + notify，作为 MA1 五级追踪矩阵 L1 的唯一入口与逐切片核对的锚点。
- **归一化 logistics use-cases.md** 3 个非标准 heading（`## 用例四/五/六：…` → `## UC-LOG-04/05/06 …`），使其与其余域/本域 `## UC-LOG-NN` 格式一致。
- **创建 notify/use-cases.md**（完整需求契约），按 `docs/design/use-case-authoring-guide.md` 与既有域 use-cases 格式，覆盖 notify 子系统的功能点（通知模板管理 / 通知实例派发链：模板渲染→接收人解析→频控合并→站内落库→外发通道 / 已读状态 / 用户面收件箱后端），**每条 UC 含验收标准原文**。**纪律**：use-cases 描述的是**期望行为/需求契约**（来源 notify README + inbox-patterns + notification-strategy 权威），**不是**"文档化当前实现"——避免 Q1 根因（真相源向实现妥协）重演。
- **复校切片覆盖统计**：UC 权威清单 vs roadmap MA1 表 51 行（50 UC 切片 + 1 notify 切片）逐一核对；notify 补写后若 UC 数变化，更新切片行映射（如 A1.51 从"按功能点核验"细化为显式 UC 编号清单）。
- **初始化五级追踪矩阵骨架**（空表模板 + 51 切片索引 + 每切片 UC 编号清单），供 MA1 各切片报告直接填充（方法论 §1 列定义）。
- 核实 product-scope 残余事实性陈旧（成功指标计数、域级能力描述与 use-cases 错位），仅修正**事实性陈述**；**需求契约分歧**记入产出文件的"基线分歧登记"段交 MA1，不直改真相源。

## Non-Goals

- **不执行 MA1-MA4 审计**（它们 Deps 含 0.2，本轮不触及）。
- **不导出 arm-index 方案 B / successor 清单**（属 0.3 范围；本计划不解析 arm-index）。
- **不修改 product-scope 的需求契约内容**（§9 冻结条款；只做事实性 status/count 核实；契约分歧登记交 MA1）。
- **不修改 owner doc 的需求契约段落**（机制/状态机/跨域契约定义）。
- **不修改 ORM/api.xml/BizModel/Processor/view.xml**（纯文档/清单工作项）。
- **不评判 notify 实现是否符合 use-cases**（那是 MA1 A1.51 的审计范围；0.2 只产出需求契约真相源）。
- **不创建 mission.json**（同 M0.1 Deferred：mission driver 侧兜底）。

## Task Route

- Type: `requirement clarification`（需求基线澄清 + 清单化；产出 MA1 审计锚点）—— 含少量 `app-layer design change`（notify use-cases 是新增需求契约文档），但主体是基线澄清。
- Owner Docs: `docs/backlog/requirement-compliance-roadmap.md`（M0.2 工作项 + Work Item Details）+ `docs/audits/requirement-compliance-methodology.md`（§1 矩阵模板 / §4 Q1 层级 + notify 特例 / §9 冻结条款）+ `docs/discussions/2026-08-02-1700-requirement-implementation-compliance-audit.md`（Q1 notify 裁决）+ `docs/design/use-case-authoring-guide.md`（UC 编写规范）+ `docs/design/notify/README.md` + `docs/design/notify/inbox-patterns.md` + `docs/architecture/notification-strategy.md`（notify 需求契约来源）
- Skill Selection Basis: `Skill: docs/skills/design-completeness-scan-prompt.md`（roadmap 为 0.2 明确指定）。该技能定义 UC 完备性扫描方法（含维度 0 开源对标、菜单→UC 对照），0.2 复用其"UC 清单化 + 完备性检查"方法做基线提取，并配合 `tools/use-case-map.cjs` 做菜单↔UC 交叉核对。notify use-cases 编写参照 `use-case-authoring-guide.md` 格式（该方法非 opencode 技能，是项目内编写规范）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 纯 markdown 文档与清单产出，无端口/环境变量/密钥/外部服务依赖。`tools/use-case-map.cjs` 为纯 Node.js（无第三方依赖，正则解析 action-auth.xml）。

## Execution Plan

### Phase 1 - UC 清单提取与归一化（18 域 + logistics 归一化）

Status: completed
Targets: `docs/design/logistics/use-cases.md`（归一化 3 heading）；产出文件 `docs/audits/rc-requirement-baseline-inventory.md`（新建，UC 权威清单 + 切片索引 + 矩阵骨架）
Skill: `docs/skills/design-completeness-scan-prompt.md`

- Item Types: `Add | Fix | Proof`
- Prereqs: M0.1 done（方法论契约可用）

- [x] `Proof` 运行 `node tools/use-case-map.cjs`（overview + coverage 模式）获取菜单↔UC 对照基线，作为完备性交叉核对输入
      - Skill: `docs/skills/design-completeness-scan-prompt.md`
- [x] `Fix` 归一化 `docs/design/logistics/use-cases.md` 3 个非标准 heading（`## 用例四：运费过账（UC-LOG-04）`/`## 用例五：承运商集成（UC-LOG-05）`/`## 用例六：签收确认（UC-LOG-06）`）为 `## UC-LOG-04 …`/`## UC-LOG-05 …`/`## UC-LOG-06 …` 标准格式，标题文案与验收标准正文不变
      - Skill: none
- [x] `Add` 提取 18 域 UC 权威清单（域 × UC 编号 × 标题 × 功能点摘要 × use-cases.md:line），落盘 `docs/audits/rc-requirement-baseline-inventory.md` 的"§UC 权威清单"段；逐域核对 UC 编号无跳号、无重复、heading 格式统一（归一化后 logistics 一并核对）
      - Skill: `docs/skills/design-completeness-scan-prompt.md`
- [x] `Add` 在产出文件"§切片覆盖复校"段，将 UC 权威清单与 roadmap MA1 表 51 行逐一对照（50 UC 切片 + 1 notify 切片），记录覆盖统计（实测 UC 总数 vs roadmap 192）与任何映射偏差
      - Skill: none

Exit Criteria:

- [x] logistics/use-cases.md 3 个非标准 heading 已归一化为 `## UC-LOG-NN` 格式（实测 `rg -n "^## 用例" docs/design/logistics/use-cases.md` 无命中）
- [x] 产出文件 `docs/audits/rc-requirement-baseline-inventory.md` 存在，含"§UC 权威清单"段（18 域逐域 UC 行，每行含域/UC编号/标题/功能点/use-cases.md:line）与"§切片覆盖复校"段（51 行映射 + 覆盖统计）

### Phase 2 - notify use-cases 补写（需求契约真相源）

Status: completed
Targets: `docs/design/notify/use-cases.md`（新建）
Skill: none（参照 `docs/design/use-case-authoring-guide.md` 编写规范 + notify 设计来源文档）

- Item Types: `Add`
- Prereqs: Phase 1 完成（UC 清单框架已建立，notify 行占位待填）

- [x] `Add` 创建 `docs/design/notify/use-cases.md`，按既有域 use-cases 格式（参照 finance/use-cases.md 结构：状态轴速查 + `## UC-SYS-NN 标题` + 验收标准原文块），覆盖 notify 子系统功能点（来源 notify/README.md §定位/§边界/§核心业务对象 + inbox-patterns.md + notification-strategy.md）：通知模板管理 / 通知实例派发链（模板渲染→接收人解析→频控合并→站内落库→config-gated 外发通道）/ 已读状态管理 / 用户面收件箱后端查询（+ best-effort 语义边界：派发失败不回滚调用方业务事实）
      - Skill: none
- [x] `Add` 每条 UC 编写**验收标准原文**（逐条可核验，非转述），作为 MA1 A1.51 切片 L1 证据
      - Skill: none
- [x] `Decision` notify UC 编号前缀与数量：采用 `UC-SYS-NN`（对齐 notify appName `erp-notify` / 二级简称 `sys`，与 arm-index/roadmap notify 工程命名一致）；UC 数量由 README/inbox-patterns/strategy 功能点决定（预估 4-7 条），在产出文件记录裁决依据
      - Skill: none

Exit Criteria:

- [x] `docs/design/notify/use-cases.md` 存在，采用 `## UC-SYS-NN 标题` 标准格式，每条 UC 含验收标准原文块
- [x] notify UC 描述的是**期望行为/需求契约**（来源可追溯到 notify README/inbox-patterns/notification-strategy），非"当前实现文档化"——产出文件"§notify 补写纪律自检"段声明来源映射 + 未参照运行时代码

### Phase 3 - 切片索引 + 矩阵骨架 + product-scope 残余核实 + 基线分歧登记

Status: completed
Targets: `docs/audits/rc-requirement-baseline-inventory.md`（补 §切片索引 + §矩阵骨架 + §product-scope 核实 + §基线分歧登记）；`docs/requirements/product-scope.md`（仅残余事实性修正，若有）
Skill: none

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 2 完成（notify UC 已纳入清单）

- [x] `Add` 在产出文件"§切片索引 + 五级矩阵骨架"段，输出方法论 §1 矩阵列定义的空表模板（L1-L5 五列）+ 51 切片索引（含 notify A1.51 补写后的显式 UC 编号清单），供 MA1 各切片报告填充
      - Skill: none
- [x] `Add` 更新产出文件"§切片覆盖复校"段：notify 补写后复算 UC 总数与切片行数（若 notify UC 使总切片数偏离 51，记录偏差与 A1.51 行调整）
      - Skill: none
- [x] `Proof` 核实 `docs/requirements/product-scope.md` 残余事实性陈旧：成功指标计数（如 `1902 单元测试` vs 实测/roadmap 1903）、域级能力描述与 use-cases 错位、跨域子系统计数等；**仅修正事实性 status/count 陈述**（P1-MA3-011 里程碑框架已由 R2.2 修复，不重做）
      - Skill: none
- [x] `Decision` **product-scope 需求契约分歧处理**：核实中发现任何**需求契约内容**（非事实性 status/count）与 use-cases/owner doc 分歧时，**记入产出文件"§基线分歧登记"段**（含分歧点/三源对照/影响切片），交 MA1 审计，**不直改 product-scope**（§9 冻结条款；唯一例外 = 经人工批准的需求变更，本工作项不触发）
      - Skill: none

Exit Criteria:

- [x] 产出文件含"§切片索引 + 五级矩阵骨架"段（空表模板 L1-L5 五列 + 51 切片索引含 notify UC 清单）
- [x] product-scope.md 仅事实性修正（若有），无需求契约内容被改；"§基线分歧登记"段记录所有发现的契约分歧（无则注明"无契约分歧"）

## Draft Review Record

- Independent draft review iteration 1: `acceptable`（独立子代理 ses_03ea43774ffeomPFKW233UItuw，fresh session）。审查 plan 契约，11 项检查 A-K 全 PASS：Deps 正确（仅 draft 0.2，0.1 done）、单结果表面、Baseline 准确（实时核实 P1-MA3-011 已由 R2.2 fixed + notify 无 use-cases.md + logistics 3 非标准 heading + tools/skills 存在 + 残余漂移"1902 单元测试"vs 1903）、roadmap 0.2 六项交付全覆盖、方法论 §4/§9 对齐、notify use-cases Q1 根因守卫到位（描述期望行为非文档化实现）、反松弛合规、Closure Gates 正确删除 build/test（doc-only）、无范围蔓延、item typing 合规、Plan Status=draft。3 项 non-blocking 已吸收 1 项（Phase 1 item 1 补 `Proof` 内联类型）；2 项执行期处理（roadmap"四个 heading"笔误实测 3 个——已在 plan 正确用 3；notify UC 数量 Decision 执行时记录最终计数+备选前缀）。无阻塞意见，达到共识。

## Closure Gates

> 本计划为**纯文档/清单**工作项（无代码/ORM/api.xml/view.xml 变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控——UC 清单与 use-cases 不触发编译或测试。验证 = 产出文件完整性 + logistics 归一化可验证 + notify use-cases 来源可追溯 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：UC 权威清单（18 域 + notify）+ logistics 归一化 + notify/use-cases.md + 切片索引 + 矩阵骨架 + product-scope 残余核实全部落地
- [x] 相关文档对齐：产出文件与方法论 §1/§4（Q1 层级 + notify 特例）/§9（冻结条款）一致；notify use-cases 与 notify README/inbox-patterns/notification-strategy 一致
- [x] 已运行验证：logistics 归一化（`rg "^## 用例" docs/design/logistics/use-cases.md` 无命中）+ UC 清单完备性（`node tools/use-case-map.cjs --overview` 交叉核对）+ notify use-cases 来源映射自检（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### product-scope 需求契约修订

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 0.2 是基线**建立**而非审计期修订；§9 冻结条款要求审计期需求契约修订须经人工批准。0.2 发现的契约分歧记入"§基线分歧登记"交 MA1，不直改真相源。product-scope 里程碑框架陈旧（P1-MA3-011）已由 R2.2 修复，0.2 仅做残余事实性核实。
- Successor Required: yes（契约分歧由 MA1 审计时按 §4 真相源层级裁决；若裁决为需求本身不合理，按 §5 唯一合法出口经人工批准改 product-scope）

## Closure

Status Note: 全部 3 Phase 已执行并标记 completed；独立结束审计（fresh-context 子代理）判定所有交付物 PASS（logistics 归一化 0 非标准 heading、UC 清单 192+7=199 行号无伪造、51 切片映射与 roadmap 一致、notify use-cases Q1 根因守卫到位、§9 冻结条款遵守——仅事实性计数修正与 heading 格式归一化，无需求契约内容被改、无代码/ORM/api.xml/view.xml 变更）。初始审计唯一的阻塞项 = 计划自身的收尾记账（Plan Status/Closure Gates/Closure 段），已于本次补齐。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 ses_03e984596ffeRknFAEGaaHHXui（fresh session，未执行本计划）
- Verdict: pass（初始 needs revision 仅因计划收尾记账未补齐；所有交付物核实 PASS：Phase 状态一致、line 号实测命中、Q1 守卫、§9 冻结、git status 仅 docs/ 变更）
- Evidence: 13 处 UC `:line` 引用实测全部命中（UC-FIN-08:147 / UC-FIN-14:269 / UC-FIN-04:76 / UC-FIN-15:298 / UC-LOG-04:41 / UC-LOG-05:53 / UC-LOG-06:79 / UC-LOG-07:65 / UC-B2B-001:3 / UC-DRP-01:3 / UC-CT-01:3 / UC-CS-01:3 / UC-APS-01:3）；`rg "^## 用例" docs/design/logistics/use-cases.md` = 0 命中；`node tools/use-case-map.cjs --overview` 合计 192；notify UC-SYS-04 窗口 300s/60s 与 notification-strategy.md:21-22 逐字一致；UC-SYS-07 email/sms-enabled 默认 false 与 README.md:130-131 逐字一致；git diff 仅 logistics/use-cases.md（heading 格式）+ product-scope.md（1902→1903 计数）+ 新建 docs/audits/rc-requirement-baseline-inventory.md + 新建 docs/design/notify/use-cases.md。

Follow-up:

- 0.3（存量清单导出）与本计划无 Deps 互赖（均仅依赖 0.1），可并行；MA1 A1.1-A1.51（Deps=0.2）与 MA2 A2.1-A2.9（Deps=0.2+0.3）在本计划 done 后进入 DRAFT_PLANS 可起草范围（MA2 仍须等 0.3）。
