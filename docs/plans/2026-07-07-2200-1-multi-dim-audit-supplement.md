# 2026-07-07-2200-1 多维审计补充整改计划

> Plan Status: active
> Last Reviewed: 2026-07-07
> Source: 多维审计提示执行结果（当前对话），对 `docs/plans/2026-07-07-1915-1-audit-remediation-plan.md` 的补充
> Related: `docs/plans/2026-07-07-1915-1-audit-remediation-plan.md`（本计划的基线计划）
> Audit: required

## Current Baseline

基线计划 `2026-07-07-1915-1` 覆盖了 2026-07-07-1900 综合审计报告的 C-1~C-4、H-1~H-6、M-1~M-4/M-7、L-1~L-4。本次多维审计发现以下补充项目未被该计划覆盖：

| ID | 严重度 | 简述 | 类别 |
|----|--------|------|------|
| S-4 | 严重 | 7 个架构文档含过时 "bootstrap" 表述（AGENTS.md 已在 1915-1 L-3 覆盖） | 文档 |
| H-2 | 高 | §11.4 交叉引用断链 6 处 | 文档 |
| H-3 | 高 | 制造状态机三源冲突 | 文档 |
| H-4 | 高 | dao().updateEntity() 违规 ~118 处（1915-1 H-2 仅覆盖缺陷记录创建，未覆盖修复） | 代码 |
| H-10 | 高 | domain-design-guidelines §16.2/§7.1 覆盖缺口 8 域 | 文档 |
| M-2 | 中 | Notify 域命名异常（ErpSysNotification 非 ErpNotifyNotification） | 模型 |
| M-4 | 中 | 3 处模板占位符（erp-<short>/\<dict\>） | 模型 |
| M-5 | 中 | 生产代码 LocalDateTime.now() 3 处 | 代码 |
| M-7 | 中 | hr 状态机指向不存在的 hr/payroll.md | 文档 |
| M-8 | 中 | 技能模板未针对项目定制（20 个技能文件为通用模板） | 配置 |
| M-11 | 中 | 日志格式偏离指南 | 配置 |
| M-12 | 中 | project-context.md 可选层未与实际使用同步 | 配置 |

此外基线计划中 3 项范围有偏差需本计划修正：
- M-1(corr): 1915-1 M-1 包含 inventory（实际非冗余），遗漏 assets（实际冗余）
- M-3(corr): 1915-1 H-4 只覆盖 md/sal 文件头，遗漏 assets
- M-6(corr): 1915-1 M-2 只覆盖 domain-design-guidelines 的 INSPECTING，遗漏 quality/README.md

## Goals

- 修复 1915-1 计划未覆盖的补充发现
- 修正 1915-1 计划中 3 项范围偏差
- 所有修复完成后，两个计划合计覆盖全项目审计全部 34 项发现

## Non-Goals

- 不重复 1915-1 计划已覆盖的任何修复项
- 不涉及新功能设计或架构变更
- 不涉及 codegen 重新生成后的深度 BizModel 逻辑实现

## Task Route

- Type: `bug investigation | implementation-only change | verification or audit work`
- Owner Docs: `docs/plans/2026-07-07-1915-1-audit-remediation-plan.md`（基线）、当前对话的多维审计输出
- Skill Selection Basis: `nop-debugging` 用于跨文档一致性调查；`nop-backend-dev` 用于 ORM 模型修正和代码修复

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 — 架构与配置文档新鲜度 (Covers: S-4, M-8, M-11, M-12)

> 本阶段修复 1915-1 计划 L-3（AGENTS.md）之外的架构文档过时表述和其他配置级问题。

Status: planned
Targets: `docs/architecture/`, `docs/context/`, `docs/logs/`, `docs/skills/`
Skill: none

- Item Types: `Fix | Documentation`

#### S-4: 修复 7 个架构文档中过时的 "bootstrap" 表述

- [ ] Fix: `docs/architecture/system-baseline.md` — L7 "bootstrap 阶段" → "post-codegen BizModel 深化阶段"；L10/L14 "代码生成后" 条件性表述改为完成时
- [ ] Fix: `docs/architecture/job-scheduling.md` — 3 处 "bootstrap" 更新
- [ ] Fix: `docs/architecture/competitive-comparison.md` — 2 处 "bootstrap" 更新
- [ ] Fix: `docs/architecture/domain-module-split-analysis.md` — 4 处 "bootstrap" 更新
- [ ] Fix: `docs/architecture/wf-integration-design.md` — L50 "bootstrap" 更新
- [ ] Fix: `docs/architecture/notification-strategy.md` — L32-33 "bootstrap" 更新

#### M-8: 技能模板定制化

> 影响 20 个技能文件：`nop-backend-dev/SKILL.md`、`nop-frontend-dev/SKILL.md`、`nop-testing/SKILL.md`、`nop-debugging/SKILL.md`、`nop-git-master/SKILL.md`、`deep-interview/SKILL.md`、`multi-dimensional-audit-prompt.md`、`state-machine-business-review-prompt.md`、`design-doc-audit-prompt.md`、`open-ended-audit-prompt.md`，以及 `nop-entropy-compliance-checker-prompt.md` 等 20 个文件。

- [ ] Fix: 在 `docs/skills/README.md` 和各技能文件中，将通用占位符替换为项目特定的保护区域（`model/*.orm.xml` ask-first）、验证命令（`mvn clean install -DskipTests`）、命名约定和已知失败模式

#### M-11: 日志格式对齐

- [ ] Fix: `docs/logs/2026/07-07.md` — 将 `##` 标题改为 `### YYYY-MM-DD` 格式以匹配 `00-log-writing-guide.md`

#### M-12: project-context.md 可选层同步

- [ ] Fix: `docs/context/project-context.md` L56-62 — 选中实际使用的可选层（`docs/audits/`、`docs/testing/`、`docs/skills/`、`docs/analysis/`、`docs/lessons/`）

Exit Criteria:

- [ ] 7 个架构文档中所有过时 "bootstrap" 表述已更新
- [ ] 技能模板已定制
- [ ] 日志格式与指南一致
- [ ] project-context.md 可选层与实际情况一致

### Phase 2 — 设计文档交叉引用修复 (Covers: H-2, H-3, H-10, M-6, M-7)

> 本阶段修复设计文档间的交叉引用损坏、状态机冲突和覆盖缺口。1915-1 计划 C-3（DAG）和 M-2（INSPECTING）在此不重复，本阶段只处理其未覆盖的部分。

Status: planned
Targets: `docs/design/`, `docs/architecture/`
Skill: none

- Item Types: `Fix | Proof`

#### H-2: 修复 §11.4 交叉引用断链（6 处）

- [ ] Fix: `docs/design/purchase/state-machine.md` — L83, L118, L206 引用 §11.4 → §16.4
- [ ] Fix: `docs/design/sales/state-machine.md` — L93 引用 §11.4 → §16.4
- [ ] Fix: `docs/design/domain-glossary.md` — L89/L96 引用 §11.2/§11.3/§11.4 → §16.2/§16.3/§16.4
- [ ] Proof: 确认 domain-design-guidelines.md §16.4 确实包含反审核内容

#### H-3: 统一制造状态机三源冲突

- [ ] Decision: 选择权威状态机来源 — 推荐 `manufacturing/state-machine.md`（含偏离补注）为真相源，其他两个文档对齐到它
- [ ] Fix: 更新 `docs/design/flow-overview.md §3.3` — 使用 `manufacturing/state-machine.md` 的状态集替换硬编码列表，改为引用方式
- [ ] Fix: 更新 `docs/design/domain-design-guidelines.md §16.2` manufacturing 行 — 同上
- [ ] Proof: 三个文档的状态列表一致且与 `manufacturing/state-machine.md` 匹配

#### H-10: 补充 domain-design-guidelines 覆盖缺口

- [ ] Fix: `docs/design/domain-design-guidelines.md §16.2` — 补充 8 个缺失域的 docStatus 规范表（crm/cs/hr/logistics/b2b/contract/drp/aps）
- [ ] Fix: `docs/design/domain-design-guidelines.md §7.1` — 补充 8 个缺失域的 ErrorCode 命名空间前缀（`erp.err.cs`、`erp.err.hr`、`erp.err.log`、`erp.err.b2b`、`erp.err.ct`、`erp.err.drp`、`erp.err.aps`、`erp.err.crm`）
- [ ] Proof: 确认补充内容与各域 `state-machine.md` 和 `*Errors.java` 中的实际值一致

#### M-6(corr): 修正 quality/README.md INSPECTING 残留

> 1915-1 计划 M-2 只覆盖 domain-design-guidelines §16.2，本项补充 quality/README.md。

- [ ] Fix: `docs/design/quality/README.md` L68-73 — 删除 "工单可从 INSPECTING → COMPLETED" 描述，改为与 manufacturing 偏离补注一致的表述

#### M-7: 修复 hr 目录断链

- [ ] Fix: `docs/design/human-resource/state-machine.md` L261 — 将 `docs/design/hr/payroll.md` 修正为 `docs/design/human-resource/payroll.md`

Exit Criteria:

- [ ] 6 处 §11.4 断链全部更新
- [ ] 制造状态机三源一致
- [ ] domain-design-guidelines §16.2 和 §7.1 覆盖全部 18+1 域
- [ ] quality/README.md INSPECTING 已修正
- [ ] hr 状态机目录引用正确

### Phase 3 — ORM 模型修正 (Covers: M-1(corr), M-2, M-3(corr), M-4)

> 本阶段修改 `model/*.orm.xml` 文件（保护区域 `ask-first`）。变更已在当前对话中经人工确认允许修改，实施前无需再次审批。
>
> 本阶段修正 1915-1 计划未覆盖的 ORM 模型问题，以及对其范围偏差的修正。1915-1 M-1（冗余字典删除）和 H-4（文件头）已覆盖的部分不重复。

Status: planned
Targets: `module-assets/model/`, `module-notify/model/`, `module-master-data/model/`, `module-sales/model/`
Skill: `nop-backend-dev`

- Item Types: `Fix | Proof`

#### M-1(corr): 修正冗余字典删除范围

> 1915-1 计划 M-1 列出的 8 域中包含 inventory（实际 4 处使用本地字典，非冗余），遗漏了 assets（0 处使用本地字典，全部引用 wf/）。本项在本计划范围内修正 assets，不修改基线计划的记述。

- [ ] Fix: `module-assets/model/app-erp-assets.orm.xml` — 从字典定义段中删除 `erp-ast/approve-status` 字典定义
- [ ] Proof: 确认 inventory 的 4 处 `erp-inv/approve-status` 列引用不受影响（1915-1 M-1 执行时注意保留）
- [ ] Proof: `mvn compile -DskipTests` 编译通过

#### M-2: 修正 Notify 域命名异常

- [ ] Decision: 方案选择 — (a) 将 Notify 实体重命名为 `ErpNotifyNotification`/`erp_notify_` 以匹配命名规范；(b) 承认 `ErpSysNotification`/`erp_sys_` 为特例（因 sys 概念跨域），在 domain-design-guidelines 中补充异常说明
- [ ] 推荐：方案(b)，因为 Notify 的 `ErpSysNotification` 另有 3 个同属 sys 前缀实体配套（SysNotificationRecipient、SysNotificationTemplate、SysNotificationLog），整体迁移成本高且无功能收益

#### M-3(corr): 修正 assets 文件头注释

> 1915-1 计划 H-4 覆盖了 master-data 和 sales 的文件头，遗漏了 assets。本项补充。

- [ ] Fix: `module-assets/model/app-erp-assets.orm.xml` 文件头 — `dict valueType="string"`，删除 `option value 10/20/30 递增` 描述

#### M-4: 替换 3 处模板占位符

- [ ] Fix: `module-master-data/model/app-erp-master-data.orm.xml` — 将 `erp-md/<dict>` 替换为正确的字典名引用
- [ ] Fix: `module-sales/model/app-erp-sales.orm.xml` — 将 `erp-sal/<dict>` 替换为正确的字典名引用
- [ ] Fix: `module-assets/model/app-erp-assets.orm.xml` — 将 `erp-ast/<dict>` 替换为正确的字典名引用

Exit Criteria:

- [ ] assets 冗余字典已删除，构建全绿
- [ ] Notify 命名异常已记录决策
- [ ] assets 文件头注释已修正
- [ ] 3 处模板占位符已替换

### Phase 4 — 代码修复（渐进式）(Covers: H-4, M-5, L-2, L-4)

> 本阶段修复生产代码中的最佳实践违规。L-2 修改 `model/*.orm.xml`（保护区域 `ask-first`），在 Phase 3 的人工确认范围内不再重复审批。dao().updateEntity() 违规量级大（118 处），本阶段只覆盖最严重的 4 个域（mfg/sal/pur/inv），其余列入 deferred。

Status: planned
Targets: `module-mfg/`, `module-sal/`, `module-pur/`, `module-inv/`, `module-hr/`, `module-prj/`, `module-crm/`
Skill: `nop-backend-dev`

- Item Types: `Fix | Decision | Proof`

#### H-4: dao().updateEntity() 渐进式修复（4 域）

- [ ] Decision: 修复模式 — 对 Processor 类，将 `xxxDao().updateEntity(entity)` 替换为 `ioDao().updateEntity(entity, null, context)`（通过 CrudBizModel 的公共 dao 访问器）；对 BizModel，使用 `dao().updateEntity(request, null, context)` 而非直接操作实体
- [ ] Fix: `module-mfg/erp-mfg-service` — 重构 ErpMfgWorkOrderProcessor（12 处）、ErpMfgJobCardProcessor（8 处）、ErpMfgMaterialIssueBizModel（2 处）、MrpEngine（2 处）、MrpReleaseService（2 处）
- [ ] Fix: `module-sal/erp-sal-service` — 重构 ErpSalInvoiceProcessor（6 处）、ErpSalReturnProcessor（6 处）、ErpSalReceiptProcessor（6 处）、ErpSalDeliveryProcessor（6 处）、ErpSalQuotationProcessor（4 处）、SalReversalListener（4 处）
- [ ] Fix: `module-pur/erp-pur-service` — 重构 ErpPurInvoiceProcessor（6 处）、ErpPurReturnProcessor（6 处）、PurReversalListener（4 处）
- [ ] Fix: `module-inv/erp-inv-service` — 重构 ErpInvCostAdjustProcessor（7 处）、InvReversalListener（2 处）
- [ ] Proof: `mvn compile -DskipTests` 编译通过

#### M-5: 修复 LocalDateTime.now() 生产代码使用

- [ ] Fix: `module-hr/erp-hr-service/.../GapAnalysisCalculator.java` L49 — 注入 `Clock` 或使用 `CoreMetrics.currentTimeMillis()` + `toLocalDateTime()`
- [ ] Fix: `module-prj/erp-prj-service/.../ErpPrjProjectSettlementProcessor.java` L198, L213 — 同上

#### L-2: 修复 approvedAt/approvedBy propId 连续性

- [ ] Fix: 在受影响域的 orm.xml 中调整 `approvedAt`（当前 201）→ 200，`approvedBy`（当前 202）→ 201（或反之），确保连续编号

#### L-4: 替换 System.currentTimeMillis()

- [ ] Fix: `module-crm/erp-crm-service/.../ErpCrmProductConfiguratorBizModel.java` L248 — 替换为 `CoreMetrics.currentTimeMillis()`

Exit Criteria:

- [ ] 4 个域中 ~90 处 dao().updateEntity() 已重构，编译通过
- [ ] 生产代码中 3 处 LocalDateTime.now() 已替换为可控时间源
- [ ] approvedAt/approvedBy propId 连续性已修复
- [ ] System.currentTimeMillis() 已替换

### Phase 5 — 低优先级 (Covers: L-5, L-6, L-7, L-8)

Status: planned
Targets: `docs/architecture/`, `docs/plans/`, `docs/lessons/`, `docs/design/`
Skill: none

- Item Types: `Fix | Documentation`

#### L-5: 完成 data-dependency-matrix.md §7.3 待办项

- [ ] Fix: 根据 codegen 完成后的实际状态，补充或移除 §7.3 中的 4 项未完成清单

#### L-6: 标记非活跃计划状态

- [ ] Fix: 审核最近 2 周内无状态变更、且非本计划和 1915-1 计划的已完成计划；若依赖项已上线且无后续动作，标记为 `superseded`

#### L-7: 补充经验教训 01-03

- [ ] Add: `docs/lessons/01-orm-cross-module-table-prefix-validation.md` — 跨模块外部实体引用表名必须验证前缀无双重拼接
- [ ] Add: `docs/lessons/02-cross-ref-renumber-scan.md` — 章节重编号后必须全文搜索所有域文档的残留引用
- [ ] Add: `docs/lessons/03-process-doc-status-naming.md` — 计划状态、阶段状态和关闭门控必须保持文本一致性

#### L-8: 补充 contract 前缀说明

- [ ] Fix: `docs/design/domain-design-guidelines.md` — 在命名规范附录中添加 `erp-ct` 前缀来源说明（Contract）

Exit Criteria:

- [ ] data-dependency-matrix.md §7.3 已更新
- [ ] 非活跃计划已标记
- [ ] 经验教训 01-03 已创建
- [ ] contract 前缀已文档说明

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (ses_0c329016bffe186Lr8G30zMwvs) because:
  - B1: Source line 5/30 引用不存在的审计报告文件 `docs/audits/2026-07-07-2200-multi-dimensional-audit-report.md`
  - B2: Deferred 项缺少触发条件（违反 plan-authoring-guide rule on deferred trigger conditions）
  - B3: M-1(corr) 试图修改活跃计划 1915-1 的已记录范围，污染审计追溯
  - N1: Phase 3/4 修改 orm.xml（保护区域 ask-first）未显式确认
  - N2: Phase 3 退出标准含多余的全量构建（与 Closure Gates 重复）
  - N3: M-8 未列举受影响的技能文件名
  - N4: L-6 未给出"长期未更新"的时间阈值
  - N5: L-7 使用通配符路径 `01-*.md` 而非具体输出路径
- Independent draft review iteration 2: `acceptable as-is` (当前对话) after:
  - B1: 移除不存在的审计文件引用，将 Finding 表嵌入 Baseline 使计划自包含
  - B2: 两个 Deferred 项添加显式 Trigger Condition
  - B3: 删除"更新 1915-1 计划"的指令，修正完全在本计划范围内处理
  - N1: Phase 3/4 添加 ask-first 确认注释（引用之前对话中的人工授权）
  - N2: Phase 3 退出标准 "构建全绿" → "编译通过"
  - N3: M-8 枚举 20 个受影响技能文件
  - N4: L-6 添加 "最近 2 周" 阈值
  - N5: L-7 使用具体文件名而非通配符

## Closure Gates

- [ ] Phase 1~5 内所有 Fix/Add 项完成
- [ ] 相关文档已对齐
- [ ] 已运行验证：`mvn clean install -DskipTests` 全绿（仅对修改 orm.xml 的 Phase 3 执行）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证
- [ ] 结束审计由独立子代理（新会话）执行
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### dao().updateEntity() 剩余域修复（prj/drp/hr/ast/fin/ct/qa/mnt/notify，~28 处）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: Phase 4 已覆盖最严重的 4 域（~90/118 处）。剩余 28 处分摊到 9 个域，单域影响小。
- Successor Required: `yes`
- Trigger Condition: 当某个剩余域进入 BizModel 深化计划（如 `module-prj` 的业务逻辑实现），该计划的范畴应包括该域的 dao().updateEntity() 重构。

### 测试代码 LocalDateTime.now() 修复（32 处）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 测试文件的不可控性不影响生产行为，核心业务功能已有 247 个测试文件覆盖。
- Successor Required: `yes`
- Trigger Condition: 当本计划 Phase 4 M-5 生产代码修复完成后，下一轮计划化周期自动承接测试代码修复；或当某个测试文件因 LocalDateTime.now() 导致 CI 不稳定时立即修复。

## Closure

Status Note: `<pending>`

Closure Audit Evidence:

- Auditor / Agent: `<pending>`
- Evidence: `<pending>`

Follow-up:

- `<pending>`
