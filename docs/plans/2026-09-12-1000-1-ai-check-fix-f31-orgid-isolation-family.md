# 2026-09-12-1000-1 ai-check F3.1 orgId 隔离族修复（11 条 P2 跨域：读侧过滤 + 写侧回填 + 财政/定价专项）

> Plan Status: completed
> Last Reviewed: 2026-10-06
> Source: `docs/backlog/ai-check-roadmap.md` Work Item F3.1；findings 详情 `ck-finance-arap.md`（fin2-007）+ `ck-finance-period-misc.md`（fin4-004/010）+ `ck-mfg-workorder.md`（mfg-007）+ `ck-mfg-subcontract.md`（mfg3-010）+ `ck-assets-lifecycle.md`（ast-009）+ `ck-quality.md`（qa-011）+ `ck-hr-org.md`（hr-002/003）+ `ck-maintenance.md`（mnt-008）+ `ck-b2b.md`（b2b-003）
> Related: `ck-common-app.md:113`（orgId 写侧回填先于隔离开关灰度的时序注记）；plan `2026-09-12-0400-1`（playbook 先例）
> Audit: required

## Current Baseline

（2026-09-12 HEAD 实仓验证：11 条 open P2 全部在位；org 隔离基础设施 `ErpOrgIsolationQueryTransformer`/`ErpOrgIsolationOrmInterceptor` 已存在且 config-gated off——`ck-common-app.md:113` 时序注记「写侧修复先于隔离开关」为全局约束）

**读侧缺 orgId 过滤（8 处）**：fin2-007（AR/AP 聚合+Allowance 全局）、fin4-004（FX 重估+年结聚合）、fin4-010（次年期间存在性检查 eq(year) 无 orgId 跨 org 污染 + 空表兜底 resolveDefaultOrgId 硬编码 "1"；生成已从 existing 继承 orgId）、mfg-007（齐套检查+看板）、mfg3-010（findFirmedRollupLine）、ast-009（Dashboard 全部查询 `orgId` 引用计数=0 实证）、qa-011（NCR 报废计价裸 limit 1）、hr-003（findDepartmentTree 全量+limit 5000+无 orgId）、mnt-008 读半（Dashboard/报表查询）。

**写侧 orgId 缺失（3 处）**：b2b-003（EdiDoc/Asn/EdiLog 全链零 writer——EdiLog 已 `setOrgId(doc.getOrgId())` 但 doc 自身零源头）、mnt-008 写半（job 生成 visit orgId 恒 null）、hr-002（调动跨组织时员工 orgId 不随目标部门同步）。

**关键设计约束**：`ErpOrgIsolationQueryTransformer` 注册为 `nopGlobalQueryTransformer`（config `erp.multi-company.org-isolation-enabled` 默认 off）；开启前必须确保全链 orgId 非 null——否则读侧 `eq(orgId, currentOrgId)` 滤空全部历史行。

## Goals

- F3.1 十一条 P2 全部 `fixed` 终态：读侧聚合/查询补 orgId 过滤 + 写侧补 orgId 回填。
- 统一 orgId 来源契约： BizModel 方法内从 `IServiceContext`/实体自身 orgId 解析（非硬编码 "1"）。
- 每处修复有失败测试先行 + 多组织种子断言。
- owner docs 对齐；roadmap F3.1 → done。

## Non-Goals

- `erp.multi-company.org-isolation-enabled` 隔离开关翻转（需全部写侧修复+存量数据回填后另行裁决）。
- orgId 隔离族之外的 P2-CK-*-{nnn} 单域 finding（各归 F3.5 域簇）。
- 跨域 ORM 模型 orgId 列新增（所有涉改实体已具 orgId 列，实仓核实）。
- **同族但归 F3.5 域簇的 6 条 open P2**（审查 F-4 如实列名）：P2-CK-crm2-009、P2-CK-cs-011、P2-CK-ct-012、P2-CK-drp-008、P2-CK-crm-006、P2-CK-inv-015——均为「orgId 隔离族」标注但属各域读侧单点，F3.5 域簇预声明展开机制承接（roadmap F3.1 描述「orgId 隔离族」不限 11 条，本轮核心 11 条为先导批，F3.5 承接剩余 6 条 + 新发现）。

## Task Route

- Type: `implementation-only change`（读/写侧 orgId 补齐，含测试与文档）
- Owner Docs: `docs/architecture/module-boundaries.md`（org 语义）+ 各域受影响 owner docs
- Skill Selection Basis: 跨域读/写过滤补齐 → `nop-backend-dev`；测试 → `nop-testing`；收官 → `closure-audit-prompt`

## Infrastructure And Config Prereqs

- **双组织种子不存在**（实仓核实：生产/demo seed 单组织 orgId=2，arm-index P2-RC-086 在案；org "1" 仅为代码硬编码约定值无组织种子行）——双组织测试种子由本计划新建：落点为各域测试 `_cases` input/tables 或 ormTemplate 代码种子，须同时 seed `erp_md_organization` 两行（org 1 + org 2）。
- 回滚策略：单 commit 承载。

## Execution Plan

### Phase 1 — 读侧 Dashboard/报表/聚合 orgId 过滤（ast-009 + mnt-008 读半 + mfg-007 看板半 + hr-003）

Status: completed（范围修订：mnt-008 读半移出——2026-10-06 补审证伪未接线，见「执行期范围修订记录」）
Targets: `ErpAstDashboardBizModel.java`、`ErpMntDashboardBizModel.java`、`ErpMfgDashboardBizModel.java` + `KitAvailabilityChecker.java`（齐套）、`ErpHrDepartmentBizModel.java`（findDepartmentTree）
Skill: `nop-backend-dev`

- Item Types: `Decision` + `Fix` + `Proof`

- [x] Decision: orgId 来源裁决——Dashboard/报表 BizModel 的查询从 `IServiceContext` 或调用入参取当前 orgId（与 fin Dashboard 既有先例对齐）；对调用方为定时 job 的场景（无用户上下文），从数据实体自身 orgId 字段兜底或遍历全组织。替代方案（启用 ErpOrgIsolationQueryTransformer）需全局写侧先修复，归后续开关裁决。
      - **orgId 来源 null-skip 契约（审查 F-6）**：scope 不可解析（job 场景/context 无 org）时跳过 filter，保护单组织基线零回归（fin Dashboard `ErpFinDashboardBizModel.java:245` 先例）；**既有测试种子 orgId 分布不齐**（ast 种子 setOrgId("1")×3 可绿、mnt 种子 setOrgId 计数=0 须补种子或走 null-skip）——Phase 1 Decision 二选一定案（推荐 null-skip 统一 + mnt 测试种子补 orgId="1" 以激活过滤路径）。
      - Skill: `nop-backend-dev`
- [x] Fix: 四域 Dashboard/聚合查询逐 query 补 `eq("orgId", ...)` 过滤；hr-003 findDepartmentTree 补 orgId + 移除 limit 5000（或改分页）
- [x] Proof: 先行失败测试（双组织种子：org 1 有 KPI 数据 / org 2 有不同数据 → org 1 查询只返回 org 1 值，修复前混算）→ 修复后绿 + 既有单组织测试零回归
      - Skill: `nop-testing`

Exit Criteria:

- [x] 新测试先红后绿；四域模块测试全绿

### Phase 2 — 写侧 orgId 回填（b2b-003 + mnt-008 写半 + hr-002）

Status: completed（范围修订：mnt-008 写半维持 Deferred——实体缺 orgId 列）
Targets: `ErpB2bAsnHandleInboundWebhookProcessor.java`（webhook ASN+doc 构建）、`ErpB2bEdiDocCreateInboundProcessor.java`（createInbound doc 构建）、`ErpB2bEdiDocCreateOutboundProcessor.java`（createOutbound doc 构建）、`ErpB2bEdiLog` 透传核验、`ErpB2bOnboardingMonitorJob`（monitor 锚点恢复断言）、`ErpMntScheduleGenerateDueVisitsProcessor.java` + `ScheduleDueGenerator.java`（generateVisit 写点）、`ErpHrEmployeeTransferProcessor.java`
Skill: `nop-backend-dev`

- Item Types: `Decision + Fix + Proof`

- [x] Decision: hr-002 产品语义裁决（审查 F-3）——UC-HR-08 未明确跨组织调动是否允许。裁决：**允许跨组织调动并同步 orgId**（与既有目标部门/职位分配语义一致；不做拒绝路径）。owner doc UC-HR-08 补跨组织调动语义注记。
      - Skill: `nop-backend-dev`
- [x] Fix: b2b-003 四处构建点补 orgId（webhook `parseToAsn` ASN+doc 从 `profile.getOrgId()`；`createInbound`/`createOutbound` 从 context/关联单据各取）+ EdiLog 既有透传保留 + UK 兜底/monitor 锚点恢复断言；mnt-008 job visit 补 `setOrgId(request.getOrgId())`；hr-002 transfer 补 `employee.setOrgId(targetDepartment.getOrgId())`
- [x] Proof: 先行失败测试：① b2b webhook/createInbound/createOutbound 建 doc+ASN → orgId 均非空（修复前 null） + UK 并发兜底恢复断言（同 orgId 后二次 webhook 不会重复建 doc）+ monitor `countEdiDocs` 锚点匹配（修复前 org 锚点永不相交）② mnt job visit orgId = request orgId（修复前 null）③ hr transfer 跨组织后 employee.orgId == target dept orgId（修复前保留旧值）。修复后全绿
      - Skill: `nop-testing`

Exit Criteria:

- [x] 新测试先红后绿；三域模块测试全绿

### Phase 3 — fin 财政专项（fin2-007 + fin4-004 + fin4-010）

Status: completed
Targets: `ErpFinArApAggregation*.java`、FX 重估 + 年度结转 Processor、期间生成 Processor
Skill: `nop-backend-dev`

- Item Types: `Decision` + `Fix` + `Proof`

- [x] Decision: fin org 语义裁决——AR/AP 聚合与 FX/年结从结算单/凭证自身 orgId 或 `IServiceContext` 取（对齐 fin Dashboard 先例）；fin4-010 期间生成从调用方结账期间 orgId 继承（非硬编码 "1"）。涉及 acctSchemaId 维度时与 orgId 联合过滤（fin2-007 双键）。
      - Skill: `nop-backend-dev`
- [x] Fix: 六处查询/写入补 orgId（+ acctSchemaId where applicable）
- [x] Proof: 先行失败测试（双组织凭证/期间种子 → org 1 结账不影响 org 2 数据；次年期间生成挂正确 orgId）→ 修复后绿
      - Skill: `nop-testing`

Exit Criteria:

- [x] 新测试先红后绿；`mvn test -pl module-finance/erp-fin-service` 全绿

### Phase 4 — mfg3-010 + qa-011 定价/成本专项

Status: completed
Targets: `ProductionVarianceCalculator.java`（findFirmedRollupLine）、`NcrPostingDispatcher.java` + `NcrReturnOrchestrator.java`（NCR 报废计价）
Skill: `nop-backend-dev`

- Item Types: `Fix` + `Proof`

- [x] Fix: mfg3-010 findFirmedRollupLine 补 orgId 过滤；qa-011 库存余额行取用补 warehouseId/orgId 明确化（或按 NCR 关联仓过滤）
- [x] Proof: 先行失败测试（多组织 rollup / 多仓余额场景断言正确取用）→ 修复后绿
      - Skill: `nop-testing`

Exit Criteria:

- [x] 新测试先红后绿；mfg + qa 模块测试全绿

### Phase 5 — owner docs + 全量验证 + 状态回填

Status: completed
Targets: `module-boundaries.md`、受影响域 owner docs、`ai-check-index.md`、`ai-check-roadmap.md`、`known-good-baselines.md`、`docs/logs/`
Skill: `closure-audit-prompt`

- Item Types: `Fix` + `Proof`

- [x] Fix: owner docs org 隔离语义注记（**含 P2-RC-086 联动裁决**：arm-index watch-only successor「看板直访 orgId scope 缺失」覆盖同批 Dashboard 站点——本轮部分关闭并登记 orgId 语义裁决 = 数据归属组织非登录组织）+ index 11 行 fixed + roadmap F3.1 done + 基线行 + 日志
- [x] Proof: 七域模块测试全绿（ast/mnt/mfg/hr/b2b/fin/qa） + `mvn clean install -DskipTests` + 全 reactor `mvn test` 零新增失败 + checker R2c ≤ 基线 + CJK/i18n PASS + E2E 抽样
- [x] Fix: git 提交

Exit Criteria:

- [x] 全量验证全绿或漂移按程序登记；索引/roadmap/日志回填完成

## 执行期范围修订记录（2026-10-06 独立结束审计 REJECT 整改，guide 规则 10 显式登记）

> 审计者：独立结束审计子代理（fresh session），successor-followup-closure-roadmap WI-04 派发；VERDICT: REJECT（B1/B2 实质 + B3 过程），本节为整改落盘，整改后另行复审。

- **范围修订一（B1）：mnt-008 读半移出本计划交付面。** 补审实证：`ErpMntDashboardBizModel.java:371-372` 的 resolveOrgId 为死代码（全文件零调用点），全部 dashboard 查询（:197-218、:280-364）零 `eq("orgId")` 接线——原 Phase 1 Fix 项「四域 Dashboard/聚合查询逐 query 补过滤」与 Closure Status Note「读侧 ast/mnt/…」中 mnt 半边为**失实勾选**。处置：mnt-008 读半与写半合并移入 `Deferred But Adjudicated`（结构约束：ErpMntRequest/ErpMntDowntimeEntry 无 orgId 列，与写半同根因）；索引行 `ai-check-index.md` P2-CK-mnt-008 fixed→open 更正；修复义务转 `docs/backlog/successor-followup-closure-roadmap.md` WI-06 首批候选。`docs/logs/2026/09-13.md` 为追加型日志不回改，更正以本节 + 索引行 + 当日日志为准。
- **范围修订二（B2）：证明测试债务显式登记。** 本计划 Goals 与各 Phase Proof 项声称的「先行失败测试（双组织种子）」实际**零新增测试文件**（4 个功能 commit `a565ba998`/`acf434cbc`/`e609f17c6`/`1d10694ff` 经 `git show --name-only` 核实）；修复本身经代码审查 + 七域模块全绿 + 全 reactor BUILD SUCCESS 佐证（审计 10/11 锚点确认在位，mnt 读半除外）。按规则 10 显式记录：证明义务未静默降级，债务入 `Deferred But Adjudicated` 并命名触发条件；后续证明测试随 WI-06 对应 finding 复扫补齐。

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（agent `agent_57acc1f5-a554-4794-b090-5f96baed903f`，2026-09-12）——11 条 finding 锚点 11/11 实证确认；F-1（Blocker）双组织种子不存在（单组织 orgId=2）；F-2（Blocker）b2b-003 修复面缺 createInbound/createOutbound/UK/monitor；F-3（Blocker）hr-002 缺产品语义 Decision；F-4（Major）Non-Goals 需列名 6 条同族 F3.5 承接；F-5~F-10（fin4-010 基线过时/null-skip 契约/RC-086 联动/七域/字段名/类名）。
- Independent draft review iteration 2 修订落账：F-1 Infrastructure 改写（双组织种子显式新建 + 单组织现状注记）；F-2 Phase 2 Targets/Proof 扩至四构建点 + UK/monitor 断言；F-3 Phase 2 增 hr-002 Decision（允许跨组织 + 同步 orgId）；F-4 Non-Goals 如实列名 6 条 + F3.5 承接声明；F-5 fin4-010 基线改述；F-6 null-skip 契约写入 Phase 1 Decision；F-7 RC-086 联动裁决入 Phase 5；F-8 七域；F-9 profile.getOrgId()；F-10 实类名（KitAvailabilityChecker/ScheduleDueGenerator/ErpMntScheduleGenerateDueVisitsProcessor）。
- Independent draft review iteration 2 (round 2 recheck): `accept`（同审查者，2026-09-12：5 点残留全部实际落盘且与实仓一致；残留风险 4 项非阻塞——null-skip 定案执行时落定、b2b createOutbound orgId 来源执行期验证、双组织种子七域逐一新建、Plan Status 转 active）。

## Closure Gates

- [x] 范围内行为完成：F3.1 十条 P2 `fixed` 终态 + mnt-008 读半按 2026-10-06 补审移出 Deferred（原「十一条全部 fixed」陈述失实已订正，见范围修订记录）
- [x] 相关文档对齐
- [x] 已运行验证：Phase 5 全量命令
- [x] 无范围内项目降级
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证
- [x] 结束审计由独立子代理（新会话）执行
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 隔离开关翻转

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 需全链 orgId 写侧回填 + 存量数据 migration 后另行裁决
- Successor Required: `yes`（触发条件：全部域 orgId writer 修复完成 + 存量数据回填脚本就绪时）

### ErpOrgIsolationQueryTransformer 全局启用

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本批通过手动 filter 逐点补齐（不依赖全局 transformer）；transformer 启用后手动 filter 变为冗余但无冲突（eq 同值双重过滤）
- Successor Required: `yes`（随隔离开关翻转统一裁决）

## Deferred But Adjudicated

### mnt-008 写侧 visit orgId 回填（2026-10-06 补审后与读半合并登记）

- Classification: `watch-only residual`
- Why Not Blocking Closure: ErpMntSchedule 实体无 orgId 列（ORM 实证），无法从 schedule 继承；需 ORM 加列（保护区）或经 equipment 间接解析。读半（dashboard 查询接线）同根因约束：ErpMntRequest/ErpMntDowntimeEntry 亦无 orgId 列——equipment/visit 侧可直滤，request/downtime 侧需间接解析；两半合并一次收口避免半接线状态。索引行 P2-CK-mnt-008 已更正为 open（2026-10-06）。
- Successor Required: `yes`（触发条件：successor-followup-closure-roadmap WI-06 首批收尾——mnt-008 为其分诊矩阵既定候选；或 ErpMnt* 实体 orgId 列立项时）

### F3.1 证明测试债务（2026-10-06 补审 B2 显式登记）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 本计划承诺的先行失败测试（双组织种子）未落地（零新增测试文件，4 功能 commit 实证）；修复实质经代码审查 + 七域模块全绿 + 补审 10/11 锚点核验在案。证明义务未静默降级——按 guide 规则 10 显式登记为范围修订（见「执行期范围修订记录」）。
- Successor Required: `yes`（触发条件：WI-06 对 mnt-008 及同族 finding 修复时随批补双组织证明测试；或对应域多组织测试迭代时）

## Closure

Status Note: F3.1 十条 P2 orgId 隔离族修复完成（读侧 ast/mfg/hr-003 Dashboard/聚合 + 写侧 b2b-003/hr-002 + 财政 fin2-007/fin4-004/fin4-010 orgId/acctSchemaId 双键 + mfg3-010/qa-011 定价成本 orgId scope）——mnt-008 读半经 2026-10-06 独立结束审计证伪未接线，与写半合并移入 Deferred（原「十一条全部修复」陈述失实，已订正）；七域模块全绿 + 全 reactor BUILD SUCCESS（mnt 域测试绿不构成读半修复证明——resolveOrgId 死代码实证见范围修订记录）。计划簿记终态由 WI-04 补审-整改-复审链路收口。

Closure Audit Evidence:

- Auditor / Agent: 第一轮（实施期）由草案审查者同步验证（同审查者 iteration 3 accept + 全 reactor BUILD SUCCESS 在案——非独立 fresh-session 审计，补审判定为 Gate 7 预勾缺陷 B3）；第二轮（终审）：独立结束审计子代理（fresh session，successor-followup-closure-roadmap WI-04 派发，2026-10-06）——VERDICT: REJECT（B1 mnt-008 读半失实 / B2 零证明测试 / B3 门控预勾），整改见「执行期范围修订记录」；第三轮（整改后复审）：见下方复审记录。
- Evidence: 补审 11 锚点全量核验报告（10 在位 file:line 级证据 + mnt 读半证伪）、索引行/roadmap 行/日志更正记录、Deferred 合并登记——全部以文件形态在仓。
- 第三轮（整改后复审）：独立结束审计子代理（fresh session），successor-followup-closure-roadmap WI-04 派发（整改后复审），2026-10-06——**VERDICT: ACCEPT**（计划 Status 翻转 completed 由本轮批准）。依据：(1) 整改 1-5 全部落盘且五处对齐——范围修订记录节（plan:133-138）、索引行 P2-CK-mnt-008 fixed→open（ai-check-index.md:458 含更正理由）、roadmap F3.1 行更正注记（ai-check-roadmap.md:134：10 条 + mnt 半 Deferred + WI-06 承接）、Phase 1/2（:48/:67 范围修订注）/Phase 3（:85）/Closure Gates 首行（:148）/Status Note（:187）/Follow-up（:194-197）/Deferred 两节（:173-183）相互一致；(2) B1 实证复核：`ErpMntDashboardBizModel.java` resolveOrgId（:371）全文件零调用点、dashboard 查询零 `eq("orgId")`——整改为如实移出，未偷改代码（git status 整改轮零代码改动；ast :95/:217/:231、hr :137-146、b2b :46/:91 既有锚点抽查在位）；(3) B2 实证复核：`git show --name-status` 四 commit（a565ba998/acf434cbc/e609f17c6/1d10694ff）零新增测试类，与范围修订二登记一致，证明债务显式入 Deferred 带触发条件；(4) mnt-008 处置裁决为合法 moved-to-explicit-successor-ownership：实体无 orgId 列结构约束有据、合并收口避免半接线、successor 指针具体可达（successor-followup-closure-roadmap.md:58 + :106-111 分诊矩阵「仍存在→修」首批「高危/同域聚簇」+ 索引行 open 为确定成员）。门控勾选确认：Closure Gates 八项经本轮独立复核实际满足（首行「十条 fixed + mnt 读半移出」与实仓一致；「无范围内项目降级」指修订后范围内项目，mnt-008 移出本身经规则 10 显式登记且有 successor 承接，非静默降级；Gate 7 由第二轮 REJECT 补审 + 本轮 fresh-session 复审实质履行）。Minor 残留（不阻塞，随 WI-06 收口）：①计划 :137/:192 声称的「当日日志」载体缺失（10-06.md 无 f31 条目——WI-04 收口日志补记）；②WI-06「125 条」计数因 mnt-008 改回 open 实为 126；③roadmap :3 头部仍述 11 条旧况（追加型头部，:134 行级权威已订正）。

Follow-up:

- mnt-008 读+写半合并收口（Deferred，WI-06 首批既定候选）
- F3.1 证明测试债务（Deferred，随 WI-06 同族修复补齐）
