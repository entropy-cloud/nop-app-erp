# 2026-07-08-0056-1-extended-domains-posted-businessdate-std-fields 7 扩展域 posted/businessDate 标准字段补充

> Plan Status: draft
> Mission: erp
> Work Item: 7 扩展域 posted/businessDate 标准字段补充
> Last Reviewed: 2026-07-08（迭代 2 修订后）
> Source: deferred 项承接 `docs/plans/2026-07-07-1915-1-audit-remediation-plan.md` Deferred「7 个扩展域补充 posted/businessDate 标准字段」+ `docs/plans/2026-07-07-2143-1-roadmap-backlog-status-reconciliation.md` 显化的 backlog P5 行；`docs/backlog/README.md` P5（触发条件「1915-1 关闭后」**已满足**——1915-1 Plan Status=completed）
> Related: `docs/plans/2026-07-07-1915-1-audit-remediation-plan.md`（completed，其 Deferred 本计划承接）、`docs/plans/2026-07-07-2143-1-roadmap-backlog-status-reconciliation.md`（completed，显化本后继）
> Audit: required

## Current Baseline

实时仓库逐项核实（`grep`/`read`，非采信旧记忆）：

- **缺口确认**：7 个扩展域源 `orm.xml` 中 `posted`/`businessDate` 列计数均为 **0**：
  - `module-{cs,hr,logistics,b2b,contract,drp,aps}/model/app-erp-*.orm.xml` → `posted=0 businessDate=0`
- **核心域范式（镜像基准）**：
  - finance：`posted=5 businessDate=10`；purchase：`posted=5 businessDate=16`；sales：`posted=5 businessDate=14`；assets：`posted=10 businessDate=22`
  - `posted`：`<column name="posted" displayName="已过账" stdSqlType="BOOLEAN" defaultValue="false" code="POSTED" .../>`，仅出现在**过账绑定**的单据头（如 `erp_pur_order`/`erp_pur_receive`/`erp_pur_invoice`/`erp_pur_payment`/`erp_pur_return`、finance 各过账实体）。
  - `businessDate`：`<column name="businessDate" displayName="..." stdSqlType="DATE" mandatory="true" code="BUSINESS_DATE" .../>`，出现在**事务型单据头**（含未过账的请购/询价/报价头）。
  - master-data：`posted=0 businessDate=0`（主数据域，非事务单据，正确无此字段——**不在本计划范围**）。
- **7 域事务型单据头候选**（粗扫，精确分类留 Phase 1 Decision）：
  - cs：`ErpCsTicket`/`ErpCsSurvey`/`ErpCsEntitlement`/`ErpCsContract`/`ErpCsTimeEntry`（config/master 实体如 `ErpCsSlaPolicy`/`ErpCsKnowledgeBase`/`ErpCsCannedResponse` 等不纳入）
  - hr：`ErpHrSalary`(过账绑定 SALARY 凭证)/`ErpHrLeaveRequest`/`ErpHrAttendance`/`ErpHrTimesheet`/`ErpHrEmployeeAssessment`/`ErpHrSurvey`/`ErpHrDevelopmentPlan`/`ErpHrRecruitment`/`ErpHrEmploymentContract`/`ErpHrShiftAssignment`/`ErpHrShiftSwapRequest`/`ErpHrSalarySimulation`
  - logistics：`ErpLogShipment`(过账绑定 TMS 运费过账 plan 1115-3)/`ErpLogDeliveryWindow`?
  - b2b：`ErpB2bEdiDoc`/`ErpB2bAsn`
  - contract：`ErpCtContract`/`ErpCtInvoicePlan`(触发发票)/`ErpCtRebateAgreement`/`ErpCtRebateSettlement`(返利结算过账)/`ErpCtRebateAccrual`(返利计提，结算经 ErpCtRebateSettlement 过账——A/B 档由 Phase 1 裁决)
  - drp：`ErpDrpPlan`（drp 模块另含 `ErpInvDrpCrossDock`/`ErpInvDrpDockAppointment`/`ErpInvDrpLeadTimeRecord`/`ErpInvDrpSafetyStockCalc` 计算/记录中间表，预期 C 档，Phase 1 复核）
  - aps：`ErpApsOperationOrder`/`ErpApsSchedule`
- **保护区域约束**：`module-<domain>/model/*.orm.xml` 是 `ask-first` 保护区域（`docs/context/ai-autonomy-policy.md` 保护区域表：`model/*.orm.xml` 模式 = ask first = 规划或实施前需要人工批准）。本计划起草经 mission-driver `draft-from-roadmap` 指令 + 2143-1 显式「下次 mission-driver 起草」触发条件授权；**但 Phase 2（ORM 修改）与计划激活（→ active）须待人工批准**。
- **codegen 后阶段规则**（AGENTS.md）：ORM 变更后用 `mvn clean install -DskipTests` 触发增量重新生成（不重跑 `nop-cli gen`）。

剩余差距：(1) 7 域事务单据头缺 posted/businessDate 标准字段，与核心域范式不一致；(2) 哪些实体得 businessDate、哪些额外得 posted（仅过账绑定头）需分类裁决；(3) 加列后 codegen 重生成 + 下游（XMeta/view/BizModel 默认赋值）对齐。

## Goals

- 为 7 个扩展域（cs/hr/logistics/b2b/contract/drp/aps）的**事务型单据头**实体按核心域范式补齐 `businessDate`（DATE mandatory）标准字段。
- 为其中**过账绑定**的单据头（已有或规划业财过账的实体，如 `ErpHrSalary`/`ErpLogShipment`/`ErpCtRebateAccrual` 等）额外补齐 `posted`（BOOLEAN defaultValue=false）标准字段。
- 触发 codegen 增量重生成并验证 154 reactor 模块全绿。
- 对齐下游（XMeta 显示名、view 列表/筛选、BizModel 默认赋值）与 owner docs。

## Non-Goals

- **不**新增实体、不重命名实体、不改既有列语义/类型——仅向既有事务头**加性新增** `posted`/`businessDate` 列。
- **不**实现新的过账业务逻辑——`posted` 字段仅作为状态标志位落库；各域是否在现有动作中置位 `posted=true` 属各自既有/后继业务逻辑，不在本计划强行接线（仅补字段 + 默认值 false）。
- **不**纳入 master-data 域（主数据非事务单据，核心域 master-data 亦无此字段）。
- **不**纳入 config/master/明细行/审计日志类实体（如 `ErpCsSlaPolicy`/`ErpHrDepartment`/各 `*Line`/`*Log`）。
- **不**做 DRP 实体命名统一（`ErpDrp*` vs `ErpInvDrp*`，经 `2026-07-05-1500-2` 裁定为 out-of-scope improvement，触发条件「DRP 域业务深化」）。
- **不**做 7 域既有 BizModel 业务逻辑深化（各域 UC successor 自行承接）。

## Task Route

- Type: `architecture change`（ORM 模式变更，跨 7 域保护区域）+ `implementation-only change`（codegen 重生成 + 下游对齐）
- Owner Docs: `docs/design/domain-design-guidelines.md`（标准字段范式）、`docs/architecture/data-dependency-matrix.md`（域依赖）、各域 `docs/design/<domain>/` use-cases/state-machine（判定哪些头过账绑定）
- Skill Selection Basis: `nop-backend-dev`（ORM 列声明 + codegen 增量重生成 + BizModel 默认赋值钩子；标准字段补齐属既验证范式的跨域复制，非新业务逻辑）。`nop-frontend-dev` 仅当 view 列表/筛选需补 businessDate 列时（Phase 4 局部）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline
- ORM 变更后需 `mvn clean install -DskipTests` 触发增量 codegen 重生成
- **保护区域门控**：Phase 2 ORM 修改实施前须获人工批准（ask-first）；本计划保持 `draft` 直至批准

## Execution Plan

### Phase 1 - 实体清单盘点与分类裁决（Decision）

Status: planned
Targets: 7 域 `module-<domain>/model/app-erp-*.orm.xml` 中所有实体（盘点），产出分类表
Skill: `nop-backend-dev`

- Item Types: `Decision | Proof`
- Prereqs: 无

- [ ] `Proof`：逐域读取 7 域源 `orm.xml`，列出全部实体，按核心域范式分类为三档：
      - **A 档（posted + businessDate）**：过账绑定单据头——已有业财过账接线的实体（如 hr `ErpHrSalary`(SALARY 凭证)、logistics `ErpLogShipment`(运费过账)、contract `ErpCtRebateSettlement`(返利结算过账)），对照 `docs/design/finance/posting.md` 业务类型清单 + 各域已落地过账计划核对。
      - **B 档（仅 businessDate）**：事务型单据头但非过账绑定（如 cs `ErpCsTicket`/`ErpCsSurvey`、hr `ErpHrLeaveRequest`/`ErpHrAttendance`/`ErpHrTimesheet`、aps `ErpApsSchedule`）。
      - **C 档（不加）**：config/master/明细行/审计日志/计算中间表（如各 `*Config`/`*Category`/`*Line`/`*Log`/`*Calc`/`*Record`，含 drp 模块 `ErpInvDrp*`）。
      - **下游展示基线核查**：读取核心域（purchase/finance）既有 XMeta/view，记录 `businessDate`/`posted` 是否进默认列表/筛选——产出确定性的「Phase 4 view 是否补 businessDate 列」结论（是/否 + 范式指针），消除后续条件性判断。
      - Skill: `nop-backend-dev`
- [ ] `Decision`：分类口径裁决——记录选择、边界判定依据（参照核心域 finance/purchase 哪些头有/无 posted 的实证）、以及「字段加性新增不强制既有动作置位 posted」的残留风险（既有过账动作是否回填 posted=true 属各域既有逻辑/后继，本计划仅补字段）。
      - 替代方案考虑：(a) 仅给已过账绑定的头补字段（最小变更，rejected——B 档非过账头缺 businessDate 仍与核心域请购/询价/报价头范式不一致）；(b) 全实体补两字段（rejected——config/master/明细行加 posted 无语义，污染模型）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 7 域实体分类表落盘（写入本计划或引用的 `docs/analysis/` 文件），每实体标注 A/B/C 档 + 一行理由，propId 连续性方案确定（加性追加到各实体现有最大 propId 之后，注意既有 `postedAt`/`postedBy` 子串列非 `posted` 布尔标志，用引号精确匹配 `"posted"`）。
- [ ] 「下游展示基线核查」结论落盘（核心域 XMeta/view 是否在列表/筛选展示 businessDate → 产出确定性的 Phase 4 view 是否补列 是/否 + 范式指针），消除后续条件性判断。

### Phase 2 - 向 7 域源 orm.xml 加性新增列（ORM·ask-first，须人工批准）

Status: planned
Targets: `module-{cs,hr,logistics,b2b,contract,drp,aps}/model/app-erp-*.orm.xml`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1 分类表裁决完成 + **人工批准**（ask-first 保护区域）

- [ ] `Add`：按 Phase 1 A/B 档分类，向各实体 `<columns>` 末尾加性新增列声明（`propId` 取该实体现有最大 propId +1，`code` 取 `BUSINESS_DATE`/`POSTED`，`stdSqlType`/`stdDataType`/`defaultValue`/`mandatory` 对齐核心域范式），并向 `<indexes>` 既有块新增非唯一索引 `<index name="IDX_<ENTITY>_ORG_BUSINESS_DATE" unique="false"><column name="orgId"/><column name="businessDate"/></index>`（参照 purchase `erp_pur_order` :598-601 / finance 各过账实体范式——**核心域无 `<composite-keys>` 块，businessDate 经 orgId+businessDate 非唯一索引加速区间查询**）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 7 域源 `orm.xml` 中 A/B 档实体均含 `businessDate`（B 档）/ `businessDate`+`posted`（A 档）列声明，`grep '"posted"'/"businessDate"'` 计数 > 0（与 Phase 1 表一致）；`xmllint --noout` well-formed。

### Phase 3 - codegen 增量重生成 + 构建验证

Status: planned
Targets: 7 域 dao/meta/service/web 全链 + `app-erp-all` 聚合
Skill: `nop-backend-dev`

- Item Types: `Proof`
- Prereqs: Phase 2 ORM 落地

- [ ] `Proof`：执行 `mvn clean install -DskipTests -pl :app-erp-{cs,hr,logistics,b2b,contract,drp,aps}-dao -am`（7 受影响域 dao 链 + 其依赖），验证 codegen 增量重生成 Entity/XMeta/DAO 无报错、BUILD SUCCESS（全工作区 154 模块构建留 Closure Gates，避免冗余长构建）。
      - Skill: `nop-backend-dev`
- [ ] `Proof`：抽样核对生成产物——A/B 档实体的 `_app.orm.xml`、Entity Java、XMeta 含新增 `posted`/`businessDate` 字段（`grep` 抽样 7 域各 1 实体）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 7 受影响域 dao 链 `mvn clean install -DskipTests` BUILD SUCCESS，0 编译错误；抽样生成的 Entity/XMeta 含新字段（解除 Phase 4 阻塞的本地化检查）。

### Phase 4 - 下游对齐与 owner-doc 同步

Status: planned
Targets: 各域 XMeta/view/BizModel、`docs/design/<domain>/`、`docs/design/domain-design-guidelines.md`
Skill: `nop-backend-dev | nop-frontend-dev`

- Item Types: `Fix | Add`
- Prereqs: Phase 3 重生成通过

- [ ] `Fix`：检查 7 域既有 BizModel `defaultPrepareSave`/`save` 钩子——事务头创建时 `businessDate` 若 mandatory 且未由前端传入，补默认赋值（`LocalDate.now()` 经 `CoreDateProvider`/平台时间工具，非 `LocalDate.now()` 直调——见 bug `2026-07-07-1915-localdatetime-now-in-12-domains`）；`posted` 默认 false 由列 `defaultValue` 承载，无需代码赋值。
      - Skill: `nop-backend-dev`
- [ ] `Add`：为 A/B 档实体 view 列表页补 `businessDate` 列与日期区间筛选——按 Phase 1「下游展示基线核查」产出结论执行（核心域若在列表/筛选展示 businessDate 则镜像；若核心域未展示则此 item 转为明确移出范围并在此记录理由，不留模糊）。XMeta 显示名/列同步对齐。
      - Skill: `nop-frontend-dev`
- [ ] `Add`：`docs/design/domain-design-guidelines.md` 标准字段段补「事务型单据头必须含 businessDate；过账绑定头额外含 posted」的统一规约（若已存在则核对一致，不重复）。
      - Skill: none
- [ ] `Fix`：各域 owner doc（use-cases/state-machine）若描述单据生命周期涉及 posted/businessDate 语义，核对一致（仅当文档与新增字段语义冲突时修正；无冲突则不动——不写样板填充）。
      - Skill: none

Exit Criteria:

- [ ] 7 域事务头 `businessDate` mandatory 字段有默认赋值兜底（前端未传时不报错）；`posted` 默认 false 生效。
- [ ] view 列表/筛选与核心域范式对齐（或实证核心域无此展示则标注省略理由）。
- [ ] `domain-design-guidelines.md` 标准字段规约与实现一致。

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (ses_0c274ad36ffeDB25XKIrBE0KBV) because 1 BLOCKER + 5 NOTES：
  - B1：Phase 2 结构契约事实错误——`<composite-keys>`/`_tenant_composite_key` 在核心域源 `orm.xml` 中 **0 命中**（`rg 'composite-keys' module-{purchase,finance,sales}/model/*.orm.xml`=0）；purchase `erp_pur_order:598-601` 实际是 `<index name="IDX_PUR_ORDER_ORG_BUSINESS_DATE" unique="false">`（orgId+businessDate 非唯一索引），非 composite-key。已更正为索引范式 + 指针。
  - N1：orm.xml 无 `biz:grid`（显示元数据在 XMeta meta 层）——已删除该误导项，XMeta 展示并入 Phase 4。
  - N2：Phase 3 Exit 重复全工作区 154 模块构建（执行规则 7）——已收敛为 7 受影响域 dao 链 `-pl ...-am`，全工作区构建留 Closure Gates。
  - N3：条件性「若涉及/仅当」致退出标准不可判定——biz:grid 项已删；view 项改为按 Phase 1「下游展示基线核查」结论执行（是→镜像/否→明确移出范围记理由），消除模糊。
  - N4：`ErpCtRebateAccrual` A 档标注不准（计提实体无独立 posted 迁移，结算经 ErpCtRebateSettlement 过账）——已软化，A/B 档交 Phase 1 裁决。
  - N5：drp 模块 `ErpInvDrp*` 计算/记录实体未枚举——baseline 已补注（预期 C 档，Phase 1 复核）。
  - 正面确认（无需变更）：ORM ask-first 门控正确（保持 draft，Phase 2 Prereqs 须人工批准，未自我激活）；Phase 1 A/B/C 分类框架可执行非藏范围；propId 连续性正确；posted 置位接线 Deferred 诚实（无活体缺陷藏匿）；命名/单结果面/技能标注合规。
- Independent draft review iteration 2: `acceptable-as-is` (ses_0c26a2690ffeBcPJa0DvLwR9ZC) — 全部 B1/N1~N5 经实时仓库复核确认已修复，无新增 BLOCKER（`composite-keys` 0 命中核实、`IDX_*_ORG_BUSINESS_DATE` 索引范式 :598-601 属实、Phase 3 收敛为 7 dao 链、biz:grid 项已删、条件性项转确定式、drp ErpInvDrp* 实体核实存在 :214/259/323/369）。迭代 2 残留非阻塞 NOTE 已采纳：Phase 1 Exit 增「下游展示基线核查结论落盘」+ postedAt/postedBy 子串匹配提示。**草案审查已收敛**——但本计划触及 ORM `ask-first` 保护区域，依 `ai-autonomy-policy.md`（`model/*.orm.xml` = ask first = 规划或实施前需人工批准），**激活（→ active）须待人工批准**；故 `Plan Status` 保持 `draft`，Phase 2 ORM 修改实施前须获人工批准。

## Closure Gates

> 本计划涉及 ORM 保护区域变更，结束前除下方门控外须确认 Phase 2 已获人工批准（ask-first）。

- [ ] 范围内行为完成（7 域事务头 posted/businessDate 落地 + codegen 重生成 + 下游对齐）
- [ ] 相关文档对齐（`domain-design-guidelines.md` 标准字段规约 + 各域 owner doc 一致）
- [ ] 已运行验证：`mvn clean install -DskipTests`（154 模块）+ `mvn test`（7 域既有测试无回归，0 failures/0 errors）+ 7 域源 `orm.xml` `xmllint --noout`
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 既有过账动作回填 posted=true（置位接线）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划仅补 `posted` 字段（默认 false）作为状态标志位落库；各域既有过账动作（如 hr `ErpHrSalary` PAID、logistics 运费 DELIVERED 过账）是否在过账成功后置位 `posted=true`，属各域既有业务逻辑/后继，非本「标准字段补齐」结果面。补字段不依赖置位接线即可交付（字段存在即满足范式一致性）。
- Successor Required: `yes`
- Trigger Condition: 各域过账动作需以 `posted=true` 作为幂等/重复过账防护门控时（如期末结账扫描 posted 标志），由各域后继计划接线。

### Phase 2 触发的 codegen 后业务逻辑深化

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 标准字段补齐不改变既有业务行为；7 域 UC successor（如 cs 知识库搜索、hr 调动等）各自独立推进，不依赖本字段补齐。
- Successor Required: `no`

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计填写>
- Evidence: <待填写>

Follow-up:

- <仅非阻塞跟进项；已确认缺陷不得出现于此>
