# 2026-10-06-2400-1 open P2 首批修复（WI-06：高危/同域聚簇 12 条）

> Plan Status: active
> Last Reviewed: 2026-10-06
> Source: `docs/backlog/successor-followup-closure-roadmap.md` WI-06 + 分诊报告 `docs/audits/check/2026-10-06-2300-successor-wi06/wi06-open-p2-triage.md`（113 条四档矩阵，本批取其 §3 首批建议 12 条）
> Related: `docs/audits/check/ai-check-index.md`（分诊后 106 open P2 余量，续行批次由分诊报告排期）
> Audit: required

## Current Baseline（分诊报告为基线真相源，2026-10-06 HEAD 实证）

分诊报告 §1 矩阵 113/113 全量覆盖，本批 12 条的分诊判定均为**仍存在**，锚点与证据要点见报告 §1 对应行。修复域分布：mnt-008（orgId 族首）+ crm2-009 + cs-011 + ct-012 + drp-008（orgId 隔离族，一套守卫范式 ×5 域）+ hr2-012/hr2-011（薪酬正确性同文件簇）+ inv-006/inv-007（VMI 同文件簇）+ md-004（汇率窗口覆盖+绕 MUTEX）+ ct-006（DRAFT 绕签署）+ drp-005（0 数量释放）。

- **兼收口簿记**：md-004 修复同时收口分诊 §4 披露的计划虚报实例（plan 2026-09-16-1000-1 对 md-004 勾 [x] 虚报——修复落定后该虚报在案自证）。
- **F-R20 家族延续注记**：收件箱页四源链数据面缺陷（plan 2026-10-06-1836-1 Deferred）不在本批（页面级，独立 successor）。

## Goals

- 12 条 open P2 → fixed（红绿测试先行，载体随各域既有测试设施）。
- ai-check-index 对应 12 行回填 fixed（红绿测试指针）。
- 各域模块测试绿 + 全 reactor 零新增失败 + 四门禁矩阵。

## Non-Goals

- 余量 94 条 open P2（分诊报告 §2）续行批次按分诊报告排期（D9 性能族/cron 键漂移族独立续行）。
- hr2-010 UK 根治（分诊判定转注册面——ORM 保护区双批准阻塞，非本批）。
- 降级 2 条（hr2-007/cs-006 → P3）随本批索引回填降级注记（非修复项）。

## Task Route

- Type: `implementation-only change`
- Owner Docs: 各域 owner docs（mnt/crm/cs/ct/drp/hr/inv/md 菜单域对应文档，实施期逐条对位）
- Skill Selection Basis: `Skill: none`（沿 f311 既有批次纪律：红绿先行 + 快照/回归矩阵）

## Infrastructure And Config Prereqs

- 既有各域测试设施（各域 service 测试类 + `_cases` 快照）。orgId 族双组织种子为**新建基建**（Phase 1 显式 `Add`：5 域逐域种子，范式 = f311 红灯映射表载体形态 + f31 Infrastructure 节种子设计〔`_cases`/ormTemplate + `erp_md_organization` org1/org2 两行〕——**诚实注记：f31 只设计未落地且其证明测试零落地系已审计事实，本批即其 Deferred 证明债的清偿载体**）。
- 开工义务：各条锚点 HEAD 复验一次（分诊锚点已现漂移实例：inv-006 :140→:133），漂移行内订正。
- 回滚策略：单提交 revert。

## Execution Plan

> **红灯映射表（12 行，M1；绿灯断言吸收 M2 裁决结果）**：

| # | Finding | 红灯载体 | 绿灯断言 |
|---|---|---|---|
| 1 | mnt-008 | `TestErpMntDashboard`（扩，新建双组织种子：equipment 挂接 org1/org2） | org1 上下文 KPI 仅聚合 org1 装备关联行（request/downtime 缺列侧经 equipment 间接解析，空挂接行按裁决剔除/计入） |
| 2 | crm2-009 | `TestErpCrm*` 既有类扩（双组织种子） | orgId 过滤后跨组织数据不可见 |
| 3 | cs-011 | `TestErpCs*` 扩 | 同上 |
| 4 | ct-012 | `TestErpCt*` 扩 | 同上 |
| 5 | drp-008 | `TestErpDrp*` 扩 | 同上 |
| 6 | hr2-012 | `TestErpHr*` 扩（调津贴用例） | ALLOWANCE 落对应津贴列（非 basicSalary）+ recordAdjustment salaryItemCode 跟随 |
| 7 | hr2-011 | 同上（核算基数用例） | 核算基数取合同约定行（非任取行） |
| 8 | inv-006 | `TestErpInvOwnershipTransfer` 扩 | 非整除除法按记账尺度正常完成（:133 锚点订正） |
| 9 | inv-007 | 同文件簇扩 | 层式计价零成本转移按语义断言 |
| 10 | md-004 | `TestErpMd*` 扩（汇率窗口用例） | 窗口语义裁决落地：次日无重叠双匹配 + 经 BizModel MUTEX |
| 11 | ct-006 | `TestErpCt*` 扩（amend 拒绝用例） | 按裁决分支：无前任 current/变更谱系时 rejectAmend 拒绝 |
| 12 | drp-005 | `TestErpDrp*` 扩 | 0 数量行释放被拒/跳过（P2-RC-069 族） |

红灯失败输出转录进本计划（f311 stash 法先例：先 stash 修复码跑红，pop 后转绿）。

### Phase 0 — 双组织种子基建（orgId 族 5 域）

Status: planned
Targets: 各域 service 测试种子设施
Skill: none

- Item Types: `Add`
- [ ] 5 域逐域双组织种子设施（erp_md_organization org1/org2 + 各域业务行挂接）——f31 证明债清偿载体（M3）
- [ ] Proof：种子装载自证（org1/org2 行计数断言）

Exit Criteria:

- [ ] 5 域种子设施就绪且自证绿

### Phase 1 — orgId 隔离族（mnt-008/crm2-009/cs-011/ct-012/drp-008）

Status: planned
Targets: 各域 Dashboard/聚合查询站点（分诊报告 §1 锚点，开工时 HEAD 复验）
Skill: none

- Item Types: `Fix | Decision | Proof`
- [x] Decision（mnt-008，裁决先于红灯表 #1）：①替代方案 ORM 加列 = 保护区阻塞（规则 9 记录：非本批，随 mnt 实体演进立项）；②request/downtime 缺列侧经 equipment 间接解析；③**空挂接（equipmentId 为空）行裁决 = 剔除出 KPI 分母并在看板 remark 登记**（计入会稀释「未分配」语义，剔除可运营感知——实施期如发现既有 KPI 消费方依赖全量口径则翻转为 null-skip 计入，落实施记录）
- [ ] 五域查询站点补 orgId 过滤（ErpOrgContext null-skip 契约统一，f31/F3.1 范式）；mnt-008：equipment/visit 侧直滤 + request/downtime 侧经 equipment 间接解析
- [ ] Proof：红灯映射表 #1-5 红→绿（双组织种子）

Exit Criteria:

- [ ] 五域红绿测试绿 + 各域模块测试零回归

### Phase 2 — 薪酬/VMI/md/ct/drp 聚簇（hr2-012/hr2-011/inv-006/inv-007/md-004/ct-006/drp-005）

Status: planned
Targets: 分诊报告 §1 锚点（开工时 HEAD 复验；inv-006 :140→:133 已订正〔m6〕）
Skill: none

- Item Types: `Fix | Decision | Proof`
- [ ] （Decision）ct-006 门控位置裁决：**约束为守卫条件变更而非状态图变更**（避免 owner-doc 义务滑入）——候选分支：(a) amend 谱系前置（存在前任 current/变更谱系方可 rejectAmend，恢复语义 javadoc 自洽）/(b) 恢复路径保留签署门 /(c) 源状态收窄。实码核验 rejectAmendTargetStatus()=ACTIVE 且恢复语义依赖前任版本谱系——倾向 (a)；裁决 + 替代 + 残险落盘
- [ ] （Decision）md-004 窗口语义一行式裁决：validTo=validFrom 同日闭区间 或 排他上界+查询侧对齐（二选一 + 一句话理由；MUTEX 半边被既有设施强制 = 约束行：dao 直写改经 BizModel `enforceNoOverlap`→`enforceMutex`）
- [ ] （Decision）hr2-012 落列一行式裁决：ALLOWANCE 落对应津贴列（ORM 4 列实证）+ `recordAdjustment` salaryItemCode 跟随
- [ ] 七条修复 + 红灯映射表 #6-12 红→绿（既有测试类扩展，断言面按映射表）
- [ ] md-004 落定后同步分诊 §4 计划虚报实例的收口注记

Exit Criteria:

- [ ] 七条红绿绿 + md/inv/ct/drp/hr 模块测试零回归

### Phase 3 — 回归 + 回填

Status: planned
Targets: ai-check-index、roadmap、docs/logs
Skill: none

- Item Types: `Proof | Add`
- [ ] 全 reactor install + test（零新增失败对照）+ 四门禁矩阵 + compliance per-site（如新增站点）
- [ ] 索引回填：12 行 fixed + 降级 2 行注记 + **分诊 §4 七行缺口回填收尾**（md-001/002/003/005、ast2-010、common-001、aps-006——WI-06 确认阶段产出，m4 归入本计划闭合）+ roadmap WI-06 收口（余量 94 条续行批次指针）+ 日志

Exit Criteria:

- [ ] 全 reactor + 四门禁零新增失败
- [ ] 索引/roadmap/日志回写在案

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（独立草案审查子代理，fresh session，2026-10-06）——4M+3m：M1 红灯义务未逐条落位（f311 M1 同型）/M2 四个真实裁决面零 Decision 项（ct-006 门控位置/mnt-008 间接解析+空挂接语义/md-004 窗口语义/hr2-012 落列）/M3「f31 红灯表同款范式」失实引用（f31 只设计未落地系已审计事实）+ m4-m6。抽查 6/12 锚点全实证（inv-006 :140→:133 漂移订正）。
- Independent draft review iteration 2: `accept`（同审查者增量确认，2026-10-06）——六项修订全部落盘（12 行红灯映射表/Phase 0 种子基建/mnt-008+ct-006 Decision/md-004+hr2-012 一行式裁决/Infra 诚实注记+锚点复验义务/§4 七行归入 Phase 3）。**实施期义务**：①hr2-012 落列裁决具名具体津贴列+理由；②红灯表通配载体 Phase 0 收尾时具体化；③mnt-008 翻转条款落实施记录；④本两条审查记录即 Draft Review Record 回填。计划转为可执行契约。

## Closure Gates

- [ ] 范围内行为完成（12 条修复 + 红绿载体）
- [ ] 相关文档对齐（索引/roadmap/日志 + owner doc 对位如涉语义）
- [ ] 已运行验证（全 reactor + 四门禁 + 各域模块）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

- 余量 94 条 open P2 + 2 降级 P3：分诊报告 §2/§4 台账承载，续行批次按其排期（非本计划 deferred——单一台账原则）。

## Closure

Status Note: <closure 时填写>

Closure Audit Evidence:

- Auditor / Agent: <closure 时填写>
- Evidence: <closure 时填写>

Follow-up:

- （仅非阻塞跟进；已确认缺陷不得在此）
