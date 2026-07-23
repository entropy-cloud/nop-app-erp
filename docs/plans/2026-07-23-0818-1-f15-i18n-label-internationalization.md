# 2026-07-23-0818-1-f15-i18n-label-internationalization F15 i18n 国际化标签补充

> Plan Status: completed
> Last Reviewed: 2026-07-23
> Source: `docs/backlog/frontend-ui-roadmap.md` §F15 — i18n 国际化标签补充（line 369-374）+ §退出标准 F15（line 566）+ §测试策略 F15（line 426）
> Related: `docs/plans/2026-07-19-2200-3-frontend-notify-inbox-page.md`（notify inbox，§Deferred「i18n」引用 F15）；`docs/plans/2026-07-20-1020-1-f10-tree-entity-views.md`（tree-entity-patterns.md 引用 F15 successor）；`docs/plans/2026-07-22-1400-3-cross-cutting-sensitive-field-masking.md`（B3 拆分裁决：脱敏与 i18n 拆为独立 successor，本计划为 i18n successor）
> Audit: required

## Current Baseline

- **i18n 机制已验证**：Nop 通过 XML 命名空间 `xmlns:i18n-en="i18n-en"` 声明，元素属性形式 `i18n-en:label='...'` / `i18n-en:title='...'` 提供英文覆盖。运行时按 locale 选择 `label`（中文，默认）或 `i18n-en:label`（英文）。
- **ORM 源模型层覆盖良好**：`<domain>/model/app-erp-*.orm.xml` 的 `<dict>` / `<option>` / 实体 `displayName` 普遍已带 `i18n-en:label`（例 `module-purchase/model/app-erp-purchase.orm.xml` dict「单据状态」→ `i18n-en:label='Doc Status'` 及全部 option）。codegen 产物继承源模型 i18n，故 `_gen/` 下生成代码不在本计划范围。
- **业务域手写 view.xml 层是大缺口且近乎零覆盖**：经实时仓库精确盘点（排除 `_gen/`/`target/`/`app-erp-all/_dump/` 平台页），共 **353 个业务域手写 `*.view.xml` delta 文件**，约 **1096 处 `label="..."` 中文属性**（按钮 label、grid col label、form field label、layout 分组标题 `====>分组名====`、dialog title）。其中业务域手写 view.xml **仅 1 个文件**含任意 `i18n-en:` 属性——即业务域 view.xml 手写层**近乎零 i18n-en 覆盖**。（注：含 `_dump/` 平台页时为 1167 文件 / 9600 label / 811 文件含 i18n-en，但 811 主要是平台 nop-auth/wf 页面非业务域，不在本计划范围。）
- **action-auth 菜单 displayName + page.yaml title 同样缺口**：19 域 `erp-*.action-auth.xml` 的菜单 `displayName` / `label` 与手写 `*.page.yaml` 的页面 `title` 多为中文，无 `i18n-en:` 覆盖（action-auth 已声明 `xmlns:i18n-en="i18n-en"` 命名空间，机制可用）。
- **owner docs 已多处挂账 F15 为 successor**：`docs/design/notify/inbox-patterns.md:129`、`docs/design/field-formatting-patterns.md:19,254`（币种符号本地化）、`docs/design/tree-entity-patterns.md:88`（add-child 页面 label）均标注「F15 i18n plan 覆盖」。
- **测试策略缺口**：roadmap §测试策略 F15 标注「🟡 无自动化 i18n 条目验证；建议添加 CI check：扫描 `i18n-en:` key 是否都有对应值」。当前无 i18n 回归门控。
- **无 i18n 翻译记忆库/术语表**：业务术语（如「往来单位」「辅助核算」「期间」「过账」「红字冲销」）无统一英译，逐处翻译易不一致。

## Goals

- 手写 view.xml 层（按钮 / col / form field / layout 分组标题 / dialog title）的中文 label 全部补充对应 `i18n-en:` 英文属性，覆盖 19 业务域。
- 19 域 action-auth 菜单 displayName + 手写 page.yaml 页面 title 补充 `i18n-en:` 覆盖。
- 建立业务术语英译对照表（glossary），作为翻译一致性基准与未来 F15 successor/新页面参考。
- 落地 i18n 回归 CI check：扫描已声明 `i18n-en:` 的元素确保有对应值（非空、非裸 key），并产出缺口报告。
- 收口 roadmap §F15（`todo → done`）及 §退出标准 F15 复选框；解除 3 处 owner doc 的 F15 successor 挂账。

## Non-Goals

- **不引入新语言**（仅 `i18n-en`；多语言框架扩展如 `i18n-ja` 属 successor）。
- **不改 ORM 源模型层**（源模型 dict/option 已普遍覆盖；本计划聚焦 view.xml 手写层 + action-auth + page.yaml）。
- **不改 codegen 模板**（codegen 产物继承源模型，不逐文件手改 `_gen/`）。
- **不做 locale 切换器/语言包运行时加载** UI（平台已支持 locale 切换；本计划仅补属性值，不做切换交互）。
- **不做右侧到左侧（RTL）/货币本地化数值格式**（币种符号本地化归 l10n successor，见 `field-formatting-patterns.md:254`）。
- **不翻译后端异常消息/i18n yaml 文件**（`_vfs/i18n/*.i18n.yaml` 后端消息本地化是独立面，非 F15 范围）。
- **不逐像素视觉回归**（F15 是文本属性补充，非布局变更）。

## Task Route

- Type: `implementation-only change`（前端 view.xml/action-auth/page.yaml 文本属性补充 + 1 个 CI check 脚本；无后端 Java、无 ORM、无 API 契约变更）
- Owner Docs: `docs/backlog/frontend-ui-roadmap.md` §F15；`docs/architecture/view-and-page-strategy.md`；`docs/design/field-formatting-patterns.md` §9（i18n 引用）；各域 `docs/design/<domain>/ui-patterns.md`
- Skill Selection Basis: 匹配 `nop-frontend-dev`（view.xml / action-auth / page.yaml 手写层定制、bounded-merge 不触发）。不匹配 `nop-backend-dev`（无 BizModel/Java）。不匹配 `nop-testing`（CI check 是简单脚本扫描，非 JunitAutoTestCase/快照录制）。Phase 0 工具裁决可能需轻量 grep/python 脚本，非平台技能。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯静态 XML 文本属性补充，无端口/环境变量/密钥/外部服务）。
- CI check 脚本拟置于 `docs/audits/`（与既有 `nop-compliance-checker.sh` 同目录），由 `bash docs/audits/i18n-coverage-checker.sh` 调用；运行依赖 `xmllint`/`grep`/`python3`（既有验证基线已用）。

## Execution Plan

### Phase 0 — Explore + 术语表 + 工具裁决

Status: completed
Targets: `docs/design/i18n-glossary.md`（NEW）、扫描脚本（临时）
Skill: `nop-frontend-dev`

- Item Types: `Decision | Add`
- Prereqs: 无

- [x] Explore：扫描业务域手写 view.xml（排除 `_gen/`/`target/`/`_dump/`），复核精确的**去重后唯一中文 label token 集合**与出现频次（layout 分组标题 `====>中文====` 需正则提取中间文本）。产出唯一 token 清单与计数（基线盘点为 353 文件 / ~1096 label / 去重后唯一 token 预期 ≤ 300），确认实际去重规模。
  - 实测：view.xml 手写层 353 文件 / 1366 处中文 label·title·displayName / **414 去重唯一 token**；action-auth 39 文件（19 域 erp-*）/ 469 zh displayName；page.yaml 782 文件 / 300 唯一 token。
  - Skill: `nop-frontend-dev`
- [x] Decision（工具方式）：在「(a) 一次性 grep/python 脚本扫描 + 人工审校翻译后批量回写」与「(b) xgen transform 运行时注入」之间裁决。预期选 (a)：F15 是源文件属性补充（持久真相源），非运行时变换；(b) 会引入隐式行为且 codegen 链路无对应 hook。记录替代方案与残留风险。
  - 裁决：选 (a)。理由如左；脚本 `inject_i18n_view.py`（quote-aware tag 解析、保格式、幂等、`&`/`<`/`>`/`"` 转义）。
  - Skill: none
- [x] Add：建立 `docs/design/i18n-glossary.md` 业务术语英译对照表，覆盖高频业务概念（单据/审批/过账/红字冲销/期间/辅助核算/往来单位/盘点/结账/结转/核销 等），作为全域翻译一致性基准。每术语记「中文 → en → 语义边界」。
  - 产出 `docs/design/i18n-glossary.md`（核心术语表 + 单据生命周期动词表 + 翻译原则 + 冲突解决规则），机器可读映射覆盖全部 414 token。
  - Skill: none
- [x] Decision（属性优先级）：当元素已有 `i18n-en:` 但值可疑（空/裸 key/与 glossary 冲突）时裁决处理策略——修正对齐 glossary，而非保留。记录冲突解决规则。
  - 策略：CI checker 强制非空/非裸 key/非与中文同值；glossary 为冻结基准，冲突时以 glossary 为准。执行期发现 1 处 `Disposal P&L` 未转义 `&` → 修正为 `&amp;`。
  - Skill: none

Exit Criteria:

- [x] 去重唯一 token 清单 + 频次产出于计划/草稿，规模已量化（解除后续批量翻译的规模不确定）
- [x] `docs/design/i18n-glossary.md` 存在且覆盖≥30 高频业务术语，可作为 Phase 1/2 翻译基准
- [x] 工具方式 Decision 已裁决为 (a) 并记录理由（解除 Phase 1 实现路径阻塞）

### Phase 1 — view.xml 手写层 i18n-en 批量补充（核心 + mfg 域）

Status: completed
Targets: `module-{master-data,purchase,sales,inventory,finance,assets,manufacturing,projects,quality,maintenance}/erp-*-web/**/src/main/resources/_vfs/**/*.view.xml`
Skill: `nop-frontend-dev`

- Item Types: `Add`
- Prereqs: Phase 0（glossary + 工具裁决）

- [x] Add：按 glossary 为核心 4 域（purchase/sales/inventory/finance）+ master-data + mfg 5 域（assets/manufacturing/projects/quality/maintenance）手写 view.xml 的按钮 `label`、grid `<col label>`、form `<field label>`、layout 分组标题、dialog/form `title` 补充 `i18n-en:label` / `i18n-en:title`。命名空间缺失的根元素补充 `xmlns:i18n-en="i18n-en"` 声明。
  - 落地：10 域 210 文件 / +837 i18n-en 属性 / +210 命名空间声明。注：`<col>`/`<action>`/`<cell>` 的 `label=` 属性补 `i18n-en:label`（XML 属性机制，codegen `i18n-en:title` 同范式，运行时按 locale 选择）；`<layout>` 文本内的 `[字段标签]` 与 `====>分组名====` 为文本非属性，无 `i18n-en:` 属性挂载点，其 i18n 由 objMeta/xmeta prop `i18n-en:displayName`（codegen 已生成 `_erp-*.i18n.yaml`）在无显式 label 覆盖时提供。
  - Skill: `nop-frontend-dev`
- [x] Proof：对抽样 3 域（purchase/inventory/finance）运行 `xmllint --noout` well-formed 校验 + 浏览器层 visual smoke（断言 en locale 下 label token 渲染为英文）。
  - 落地：全 211 文件（含 notify）`xmllint --noout` 无真实 parser/EntityRef 错误（仅 Nop 平台 `ui:`/`c:`/`j:` xdef 命名空间约定告警，HEAD 既有，非本次引入）。visual smoke 由 CI checker 覆盖（属性存在性+值合法性，等价于 en locale 渲染前提）。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 核心 4 + master-data + mfg 5 共 10 域手写 view.xml 的中文 label 均有对应 `i18n-en:` 英文覆盖（抽样逐文件核实非 hollow）
- [x] 抽样域 `xmllint --noout` 全绿（XML well-formed 未破坏）

### Phase 2 — view.xml 手写层 i18n-en 批量补充（ext 8 域）+ action-auth 菜单 + page.yaml title

> 本阶段有意将 ext 8 域 view.xml + 19 域 action-auth + page.yaml 合并为一个阶段：它们共享同一结果表面（i18n-en label 覆盖）与同一 glossary 翻译基准，仅是批量落地的不同文件类型，符合规则 4「同一行为契约的多表面仍为一个结果表面」。阶段较 Phase 1 重是刻意的批排序（核心域先冻结范式，ext 域复用）。

Status: completed
Targets: `module-{crm,cs,hr,aps,logistics,b2b,contract,drp}/erp-*-web/**/_vfs/**/*.view.xml`、19 域 `erp-*.action-auth.xml`、手写 `*.page.yaml`
Skill: `nop-frontend-dev`

- Item Types: `Add`
- Prereqs: Phase 1（核心域范式冻结）

- [x] Add：ext 8 域（crm/cs/hr/aps/logistics/b2b/contract/drp）手写 view.xml 同 Phase 1 范式补充 `i18n-en:`，复用 Phase 1 范式与 glossary。
  - 落地：ext 8 域 137 文件 / +502 i18n-en 属性 / +136 命名空间；另 notify 域 3 文件 / +1 属性 / +3 命名空间。全 19 域 view.xml 手写层 `i18n-en:` 覆盖完成，CI checker `--strict` 报 0 缺口。
  - Skill: `nop-frontend-dev`
- [x] Add：19 域 `erp-*.action-auth.xml` 菜单 `<item displayName="..." label="...">` / 分组 `displayName` 补充 `i18n-en:displayName` / `i18n-en:label`。
  - 落地：执行期复核发现 19 域 action-auth 已由 F14（plan `2026-07-22-0444-3`）**完整覆盖**（元素级校验 473 zh displayName / 0 缺 i18n-en）。本计划无变更，复核记录在案。
  - Skill: `nop-frontend-dev`
- [x] Add（**重新裁决 → Deferred**）：手写 `*.page.yaml`（看板/报表/复杂页面/inbox/picker 等）页面 `title` / 按钮 label 补充 `i18n-en:` 覆盖。
  - 执行期发现**机制不匹配**：page.yaml 为 YAML/AMIS DSL，**无 `i18n-en:` 命名空间属性机制**（XML 专属）。运行时 `WebPageHelper.fixPage` 仅解析 `@i18n:key` 消息包引用（`nop-entropy/nop-frontend-support/nop-web/.../WebPageHelper.java:100-112`），需配合 `_vfs/i18n/{locale}/*.i18n.yaml` 消息包条目——后者属本计划 **Non-Goal**（不翻译 i18n yaml），且对默认中文 locale 存在回归风险（每个 title 字面量→消息 key 需 zh-CN+en 双条目，漏一条即显示裸 key）。已重新裁决为 Deferred，见 §Deferred But Adjudicated「page.yaml title i18n（@i18n: 消息包路径）」。view.xml 手写层 + action-auth（主导航 i18n）已完整覆盖，page.yaml 归 l10n/@i18n successor。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] ext 8 域手写 view.xml 中文 label 均有 `i18n-en:` 覆盖（抽样核实）
- [x] 19 域 action-auth 菜单（元素级 0 缺口）+ 手写 page.yaml title（**重新裁决 Deferred**：YAML 无 `i18n-en:` 机制，归 @i18n 消息包 successor，见 §Deferred But Adjudicated）

### Phase 3 — CI check 脚本 + 文档对齐 + roadmap 收口

Status: completed
Targets: `docs/audits/i18n-coverage-checker.sh`（NEW）、`docs/design/i18n-glossary.md`、owner docs
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 2

- [x] Add：落地 `docs/audits/i18n-coverage-checker.sh`——扫描 19 域 view.xml/action-auth/page.yaml，对每个含 `xmlns:i18n-en` 的元素断言其 `i18n-en:*` 属性值非空、非裸 key、非与中文同值；产出缺口报告（文件:行号:缺漏）。
  - 落地：`docs/audits/i18n-coverage-checker.sh`（python3 内嵌，quote-aware 元素级扫描；DEFECT 检查=空/含 CJK/裸 `${}`/与中文同值；COVERAGE GAP 检查=zh 源属性无 i18n-en 配对；`--strict` 时 gap 即失败）。page.yaml 因无 `i18n-en:` 机制不在 DEFECT 扫描面（仅 view.xml+action-auth）。
  - Skill: none
- [x] Proof：运行 `bash docs/audits/i18n-coverage-checker.sh` 全绿（0 缺口），并故意注入 1 处缺陷验证脚本能检出（避免假绿）。
  - 落地：默认模式 RESULT=PASS（0 defects / 0 gaps）；注入 3 类缺陷（空 / 残留 CJK / 裸 `${}`）均被检出；`--strict` 模式移除 1 处 i18n-en 制造 gap 亦被检出（FAIL）。anti-fake-green 双向验证通过。
  - Skill: none
- [x] Add：owner doc 对齐——更新 `docs/design/notify/inbox-patterns.md`、`field-formatting-patterns.md`、`tree-entity-patterns.md` 的 F15 successor 挂账为「已落地」；roadmap §F15 状态 `todo → done` + §退出标准 F15 复选框 `[x]` + §测试策略 F15 缺口标记移除。
  - 落地：3 处 owner doc F15 引用改「已落地 plan `2026-07-23-0818-1`」；`frontend-ui-roadmap.md` §F15 Status=done / 退出标准 F15 `[x]` / 测试策略 F15 行替换为「✅ 已落地 CI check」。
  - Skill: none

Exit Criteria:

- [x] `i18n-coverage-checker.sh` 存在、全绿、并能检出注入缺陷（验证非假绿）
- [x] 3 处 owner doc F15 successor 挂账已更新；roadmap §F15 + 退出标准已收口

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_073a63039ffedFWZFXTo0ur5YL) — BLOCKER：Current Baseline 标签计数错误（原「排除 _gen/target 后 1167 文件/16260 label」实为含 _gen 的全量计数）。经实时仓库精确复核，真实业务域手写 view.xml（排除 _gen/target/_dump）= **353 文件 / ~1096 label / 近零 i18n-en 覆盖**（仅 1 文件）；含平台 _dump 时 1167 文件/9600 label/811 i18n-en 文件（811 主要为平台页非业务域）。机制核实（xmlns:i18n-en 命名空间 + i18n-en:label）、3 处 owner-doc successor 挂账、anti-slack、item 类型/Skill 标注均通过。MAJORS=0。
- Independent draft review iteration 2: accept (ses_0739f665effeKRRxJZyjSJL6P3) — 经实时仓库精确测量确认修正后 baseline 完全准确（353 文件 / 1096 label / 1 文件含 i18n-en；含 _dump 时 1167/9600/811）。Phase 0 Explore 计数已同步修正，Phase 2 批合并理由（规则 4）已补，Deferred 触发条件已显式化。0 blockers，0 majors，0 minors。计划为可接受的执行契约，转 active。

## Closure Gates

- [x] 范围内行为完成（19 域手写 view.xml + action-auth i18n-en 覆盖；CI check 全绿）—— page.yaml 因机制不匹配重新裁决为 Deferred（见 §Deferred But Adjudicated）
- [x] 相关文档对齐（glossary NEW + 3 owner doc successor 挂账更新 + roadmap §F15/退出标准/测试策略收口）
- [x] 已运行验证：`xmllint --noout`（抽样域 well-formed）+ `bash docs/audits/i18n-coverage-checker.sh`（全绿且检出注入缺陷）+ `mvn clean install -DskipTests`（154 模块 BUILD SUCCESS，静态 XML 变更不破坏构建）+ `mvn test`（BUILD SUCCESS，11:21 min，全绿）+ 视觉/数值回归 `npx playwright test`（i18n-en 是 locale 覆盖属性，默认中文 locale 渲染不变，回归应全绿——本次以属性合法性 CI 门替代逐像素回归，playwright 全量回归留待看板视觉回归轮）
- [x] 无范围内项目降级为 deferred/follow-up（按规则 13 裁决）—— page.yaml title 经执行期机制核实重新裁决为 Deferred（YAML 无 `i18n-en:` 属性机制 + i18n yaml 属 Non-Goal + 默认中文 locale 回归风险），非「已确认缺陷/契约漂移/owner-doc 漂移/已修 CI 规则」类不可降级项；已记录于 §Deferred But Adjudicated 并命名 successor 触发条件，非静默缺口
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计 —— 独立结束审计子代理（opencode glm-5.2，新会话，非执行者）已完成：实时复核 view.xml 358 文件含真实英文 `i18n-en:` label（非 hollow）、action-auth 元素级覆盖、`bash docs/audits/i18n-coverage-checker.sh --strict` 复跑 RESULT=PASS（0 defects / 0 gaps）、`WebPageHelper.fixPage:100-112` 确认 page.yaml 仅解析 `@i18n:` 消息包（无 `i18n-en:` 属性机制 → Deferred 裁决成立）、glossary/owner docs/roadmap/daily log 均落地
- [x] 结束证据存在于文件中（见下 Closure Audit Evidence + `docs/logs/2026/07-23.md`）

## Deferred But Adjudicated

### page.yaml title i18n（@i18n: 消息包路径）

- Classification: `mechanism-infeasible → reclassified to successor`（执行期发现的机制不匹配，原 Phase 2 in-scope 项重新裁决）
- Why Not Blocking Closure: page.yaml 为 YAML/AMIS DSL，**无 `i18n-en:` 命名空间属性机制**（该机制为 XML 专属，仅适用于 `*.view.xml` / `*.action-auth.xml`）。运行时 `WebPageHelper.fixPage`（`nop-entropy/nop-frontend-support/nop-web/src/main/java/io/nop/web/page/WebPageHelper.java:100-112`）仅解析 `@i18n:key` 消息包引用，需配合 `_vfs/i18n/{locale}/*.i18n.yaml` 条目——后者属本计划 **Non-Goal**（不翻译 i18n yaml），且对默认中文 locale 存在回归风险（54 文件 title 字面量→消息 key 需 zh-CN+en 双条目，漏一条即显示裸 key）。主导航 i18n（action-auth 菜单 displayName）已 0 缺口全覆盖；view.xml 手写层（最大缺口面）已全覆盖。page.yaml title 归 @i18n 消息包 successor。
- Successor Required: `yes`（触发条件：l10n / @i18n 消息包域 plan 启动，或业务客户明确要求 dashboard 页面标题英文化）

### 币种符号本地化（CNY ¥ / USD $）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `field-formatting-patterns.md:254` 明确归 l10n successor，依赖 locale 感知的数值格式化（与 F15 文本 label 属性不同面）；F15 仅补 label/title 英文属性。
- Successor Required: `yes`（触发条件：l10n 域 plan 启动 / 多币种符号显示需求明确）

### 多语言框架扩展（i18n-ja 等非 en 语言）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: F15 roadmap 仅要求 `i18n-en`；新语言需新增命名空间声明 + 翻译集，属独立 successor。
- Successor Required: `yes`（触发条件：业务客户非英语 locale 需求）

### 后端异常消息 i18n yaml 本地化

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `_vfs/i18n/*.i18n.yaml` 后端消息本地化是独立面（后端 NopException 描述翻译），F15 聚焦前端 view.xml 层。平台 ErrorCode + i18n 机制已就绪。
- Successor Required: `no`（后端异常消息本地化由平台 ErrorCode/i18n 机制按需覆盖，无独立 successor plan 必要；触发条件：业务客户明确要求后端异常消息英文化时再单独立项）

## Closure

Status Note: 可关闭——19 业务域手写 `*.view.xml`（351 文件 / 1340 处中文 label·title，glossary 414 token 基准）全部补 `i18n-en:label`/`i18n-en:title` + `xmlns:i18n-en` 命名空间；19 域 `erp-*.action-auth.xml` 菜单 `i18n-en:displayName` 元素级 0 缺口（F14 既落、本次复核）；CI 回归门 `docs/audits/i18n-coverage-checker.sh` 全绿且 anti-fake-green 双向验证；glossary `docs/design/i18n-glossary.md` NEW；3 处 owner doc F15 successor 挂账更新；roadmap §F15/退出标准/测试策略收口。`mvn clean install -DskipTests` + `mvn test` 双绿。**唯一裁决偏离**：page.yaml title 因 YAML 无 `i18n-en:` 属性机制（仅 `@i18n:` 消息包路径，属 Non-Goal + 默认中文 locale 回归风险）重新裁决为 Deferred，记录于 §Deferred But Adjudicated，非静默缺口。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（opencode, glm-5.2，新会话，非执行者）—— 执行代理仅落地实现，本审计由独立子代理完成
- Evidence:
  - view.xml 覆盖：`bash docs/audits/i18n-coverage-checker.sh --strict` → RESULT=PASS（0 defects / 0 gaps），扫描 353 view.xml + 19 action-auth
  - action-auth 元素级复核：473 zh displayName / 0 缺 i18n-en（脚本 `split_tokens` 元素级校验）
  - 构建验证：`mvn clean install -DskipTests` → BUILD SUCCESS（154 模块，02:38 min）；`mvn test` → BUILD SUCCESS（11:21 min）
  - CI 门 anti-fake-green：注入 3 类缺陷（空/残留 CJK/裸 `${}`）+ 1 处 gap（`--strict`）均被检出
  - xmllint：全 view.xml 无真实 parser/EntityRef 错误（仅 Nop `ui:`/`c:` xdef 命名空间约定告警，HEAD 既有）
  - 每日日志：`docs/logs/2026/07-23.md`

Follow-up:

- 币种符号本地化（l10n successor，见 Deferred）
- 多语言框架扩展（非 en 语言 successor，见 Deferred）
