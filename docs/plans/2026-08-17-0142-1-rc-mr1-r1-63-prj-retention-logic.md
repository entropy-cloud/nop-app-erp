# 2026-08-17-0142-1-rc-mr1-r1-63-prj-retention-logic RC-R1.63 — projects 质保金逻辑（P1-RC-052：retentionAmount 留存填充 + 到期返还 mutation + 质保金凭证）

> Plan Status: completed
> Last Reviewed: 2026-08-17
> Mission: requirement-compliance
> Work Item: RC-R1.63（P1-RC-052，UC-PRJ-07 AC-④⑤ 质保金留存 + 到期返还）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.63 行（:455）+ `docs/audits/arm-index.md` P1-RC-052 行（:225）+ 展开器 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`（RC-R1.63 原标注「Provider/VoucherFact（A4.2.122 须 ask-first）」——**2026-08-12 B 类批量裁决用户级覆盖该审计级 ask-first 备注**，降级为预授权自动执行，见 Current Baseline）
> Related: `docs/design/projects/use-cases.md`（L1 UC-PRJ-07 :115-127）；`docs/design/projects/profitability.md`（§实体清单 ErpPrjProjectSettlement :43-65 + §关键流程 2/3）；`docs/audits/2026-08-05-2330-1-rc-ma1-a1-36-projects-f3-settlement-dashboard.md`（A1.36 §5/§6 P1-RC-052）；`docs/audits/2026-08-07-2359-rc-ma4-a4-2-113-123-projects-f1-f2-f3-runtime.md`（A4.2.122 :153-163）
> Audit: required

## Current Baseline

- **finding P1-RC-052（arm-index:225，UC-PRJ-07 ④⑤）**：L1（`use-cases.md:124`）逐字「质保金(retentionAmount)留存,到期返还」——要求**留存 + 到期返还**两段行为。L3 实仓（HEAD 核查）：
  - **schema 存在**：`retentionAmount`（propId 16，domain amount DECIMAL 20,4，defaultValue="0"）+ `retentionDueDate`（propId 17，DATE）——`module-projects/model/app-erp-projects.orm.xml:826-827`，`_ErpPrjProjectSettlement` 生成 getter/setter + api InputBean/OutputBean。
  - **service 层零 writer**：grep `setRetention|getRetention|Retention` 跨 `module-projects/erp-prj-service/src/main` 零业务命中（仅 `_gen` + api bean）。
  - **createSettlement 不填**：`ErpPrjProjectSettlementCreateSettlementProcessor.createSettlement:26-56` 仅填 code/projectId/orgId/customerId/businessDate/settlementType/pnlSnapshotId/currencyId/exchangeRate/finalRevenue/finalCost/finalProfit/transferToAsset/docStatus/approveStatus/posted，**零 retention 填充**。
  - **结算行生成器无质保金分支**：`ErpPrjProjectSettlementProcessor.buildLines:174-197` 仅 INCOME/COST 两类行（BILLING/COST_COLLECTION 来源）。
  - **无到期返还 mutation**：grep `returnRetention|retentionDue|到期返还` 全 module-projects 零命中。
  - **Provider 无质保金分录**：`ProjectSettlementAcctDocProvider.createFacts:56-102`（`.../posting/ProjectSettlementAcctDocProvider.java`）仅 CLOSE 转固分支（Dr 1601/Cr 1603）+ FINAL/INTERIM 分支（Dr 5101 + 本年利润 4103 / Cr 6001），**全文零 retentionAmount 读取**；`readDecimal/readString/readLong/readBoolean:117-151` 不读 retentionAmount/retentionDueDate——经 delta 填充后 Provider 不感知。
- **owner doc 立场（A1.36 §5/§6 复核）**：`profitability.md:57-58` schema 列设计意图含质保金**未声明 Deferred**——L2 与 L1 一致，实现未达属实现未达标非设计妥协 → §4 三判据均不成立 → **Q4=(a) 强制实现禁止方案 B**（roadmap 头 :24-25 裁决）。
- **2026-08-12 批量裁决（roadmap 头 :49 B 类清单）**：**RC-R1.63 在 B 类**——「retention 字段已存在，Processor 填充 + 返还 mutation」经核实不需要 ORM 结构变更，纯代码逻辑/跨域契约可解决，从"越界项 ask-first"**降级为预授权自动执行**。roadmap 行旧「越界项…双独立子 agent 批准 checkbox」字样按 B 类裁决执行期改写消除歧义（对齐 RC-R1.41/42/48/50/52-54/59/61/62 先例）。**A4.2.122 ask-first 备注冲突消解**：A4.2.122 运行时报告称「修复归 MR1 触 Provider/VoucherFact 须 ask-first」——**2026-08-12 用户批量裁决 B 类清单显式收录 RC-R1.63**，裁决（用户级）覆盖 A4.2 备注（审计级），按 B 类预授权执行（对齐 RC-R1.50/52-54 会计类同型先例 + R1.62 计划 :19-20 同型消解注记）；**2026-08-15 用户裁决升级**（roadmap 头 :13）仅升级未降级项（保护区域允许 AI 自动修改但须双独立子 agent 批准）——**B 类降级项不适用该升级**（以 2026-08-12 批量裁决为准，对齐 R1.61/62 执行先例零 checkbox）；若 Phase 1 Explore 证实必须 ORM 结构变更 → 暂停回落越界流程（双独立子 agent 批准）。
- **A4.2.122 运行时确认（2026-08-07，维持 P1）**：orm.xml:826-827 列存在 + grep service 零 [Rr]etention 业务命中 + `ProjectSettlementAcctDocProvider:56-102` 无质保金分录 + readDecimal 等不读 retention（经 delta 填充不感知）。
- **既有编排上下文**：结算三轴状态机（docStatus/approveStatus/posted）已落地——`ErpPrjProjectSettlementSubmitForApprovalProcessor`/`ApproveProcessor`/`RejectProcessor`/`CancelProcessor`/`ReverseSettlementProcessor` 齐全（`reverseSettlement` 红冲凭证 + 回退卡片 + posted=false，**不触碰 retention 字段**——与留存/返还的交互见 Phase 1 Explore ⑥）；`ProjectSettlementPostingDispatcher` 组装 `PostingEvent`（businessType=PROJECT_SETTLEMENT，:77-78 setCurrencyId/setExchangeRate 既有）；`ErpPrjProjectSettlementBizModel` 已暴露 createSettlement/submit/approve/reject/cancel/reverseSettlement 等 @BizMutation（:53-58 起）。
- **测试基线**：erp-prj-service **158 tests 全绿**（R1.60 基线）；`TestErpPrjProjectSettlement` 4 @Test（CLOSE 转固 + 红冲凭证 + 资产卡片）+ E2E `projects-settlement-posting` 为既有结算测试范本；**零 retention 测试**。
- **compliance 基线**：R2b=235 / R2c=1433 / R2d=35（R1.60 登记后基线）；本计划预期经既有 daoProvider 站点/IBiz 注入实现 → 零漂移或 +N baseline-raise 登记（per-site 证据）。

## Goals

- **UC-PRJ-07 ④⑤ 运行时成立（P1-RC-052 核心）**：「留存」——`createSettlement` 填充 `retentionAmount`/`retentionDueDate`（载体 Phase 1 裁决：config 比例规则 vs 手工录入 + 适用结算类型范围；**禁止静默零填充**）；「到期返还」——新增 `returnRetention` mutation（扫描到期结算 + 资金面返还 + 幂等防重）。
- **质保金凭证**（P1-RC-052 finding 修复规范「质保金凭证[经 finance Provider 注册]」）：Phase 1 裁决载体——A. 扩展 `ProjectSettlementAcctDocProvider` 增留存/返还分录腿（保留 PROJECT_SETTLEMENT businessType）；B. 注册新 businessType（如 PROJECT_SETTLEMENT_RETENTION）+ 独立 Provider。**reverseSettlement/cancel 与留存凭证生命周期一致性（Explore ⑥）纳入判据**。须显式裁决并记录理由与残留风险（GL 平衡/科目选择/悬挂风险）。
- **幂等与守卫**：返还幂等（不重复返还）载体 Phase 1 裁决（billHeadCode 后缀标记 + 既有已过账凭证反查，镜像 R1.52 catchUp `#CATCHUP` 范式 / 或 remark 标记）；非 FINAL/非 APPROVED/未过账结算不返还守卫。
- **测试**：新增 retention 测试组（createSettlement 填充断言/返还 mutation/幂等/守卫/凭证行级断言/零回归）；既有 158 tests 零回归。
- **零回归**：erp-prj-service 全量测试全绿 + 全量 `mvn test` + `mvn clean install -DskipTests` + compliance checker 零漂移或基线登记。
- **owner doc 收敛**：`profitability.md` §实体清单/§关键流程 补质保金实现注记（留存/返还/凭证裁决记录）；arm-index P1-RC-052 → done (RC-R1.63)（修复记录 + 历史保留）；roadmap RC-R1.63 → done ✅（行标签按 B 类裁决改写）+ 本日志条目。

## Non-Goals

- **不触 ORM 结构变更**（B 类裁决：retentionAmount/retentionDueDate 列已存在；若 Phase 1 Explore 证实必须加列/加实体（如返还记录实体/返还标记列）→ 暂停回落越界流程（双独立子 agent 批准 + 独立 plan-audit），本计划不预授权）。
- **不重写结算主链/转固链**（`createSettlement`/`buildLines`/`createAndActivateAsset`/`ProjectSettlementAcctDocProvider` 既有 FINAL/INTERIM/CLOSE 分支语义不改变，仅增质保金分支）。
- **不实现合同域联动**（结算 ORM 无 contractId 维度——留存比例若需合同驱动属 successor 触发条件登记，不本计划实现）。
- **不改真相源契约段落**（use-cases L1 不动；profitability.md 契约段不动，仅补实现注记）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧修复：纯 BizModel/Processor/Provider 代码逻辑，2026-08-12 B 类裁决降级预授权；Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/projects/use-cases.md`（L1 UC-PRJ-07）+ `docs/design/projects/profitability.md`（§结算）+ `docs/design/finance/posting.md`（Provider 注册范式）
- Skill Selection Basis: per-mutation Processor + protected step + 跨实体 Facade + 业财 Provider/VoucherFact（`nop-backend-dev`：决策门 + 幂等语义 + 错误码 + 事务边界）；测试（`nop-testing`：JunitBaseTestCase + GraphQL 引擎集成 + 凭证行级断言范式对齐 R1.52/53 先例）。

## Infrastructure And Config Prereqs

- 无新 config key（除非 Phase 1 裁决引入留存比例/到期月数门控键——若引入按 `erp-prj.*` 命名 + ErpPrjConfigs reader + 默认值裁决；对齐 `settlement-require-approval` 先例）。
- 无 ORM 变更（B 类裁决）。
- 分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-projects/erp-prj-service`。

## Execution Plan

### Phase 1 - Explore + Decisions（质保金载体裁决）

Status: completed
Targets: `module-projects/erp-prj-service/src/main/java/app/erp/prj/service/`（processor/posting 包）、`module-projects/model/app-erp-projects.orm.xml`（只读核查）、`docs/design/projects/profitability.md`（只读复核）
Skill: `nop-backend-dev`
Item Types: `Decision`
Prereqs: 无

- [x] Explore：结算域现状复核（只读）——① grep `retention` 全 erp-prj-service 复核零 writer 基线；② `ErpPrjProjectSettlementBizModel` 全部 @BizMutation 清单（returnRetention 落点）；③ `ProjectSettlementPostingDispatcher` buildEvent billData 键集（BILL_DATA_FINAL_REVENUE/FINAL_COST/FINAL_PROFIT/SETTLEMENT_TYPE/TRANSFER_TO_ASSET 既有——retention 键是否需新增）；④ finance `ErpFinBusinessType` 注册表现状（PROJECT_SETTLEMENT 单值 vs 新增枚举）；⑤ 既有返还幂等先例核查（R1.52 catchUp `#CATCHUP` billHeadCode 范式 / 已过账凭证反查可达性）；⑥ **reverseSettlement/cancel 交互核查**——`reverseSettlement`（红冲凭证 + posted=false + 回退卡片）不触碰 retention 字段：若留存凭证采用 D2 独立 businessType（选项 B），主结算红冲/取消后留存凭证是否悬挂（dangling voucher）？裁决是否需在 reverseSettlement/cancel 时同步冲销留存凭证（归 D2 判据）。
      - Skill: `nop-backend-dev`
- [x] Decision D1（留存比例载体）：A. config 驱动——`erp-prj.settlement-retention-ratio`（默认 0）+ `erp-prj.settlement-retention-due-months`（默认 12），createSettlement 自动填 `retentionAmount = finalRevenue × ratio`、`retentionDueDate = businessDate + N月`；B. 纯手工录入——createSettlement 不填、经 CRUD update 维护。**与「禁止静默零填充」张力消解（reviewer iter-2 折叠）**：Goal「禁止静默零填充」指**不得因实现缺失而静默留零**；若选 B（设计性零自动留存），须同时裁决**手工填入门控**（approve/过账前 retentionAmount 必填校验或显式「零质保金」确认），使「留存」语义由手工路径达成而非静默缺失——裁决记录该消解。**留存适用结算类型范围裁决**（结合 Explore ⑥）：L1 UC-PRJ-07 场景=「项目竣工结算」→ 留存仅 FINAL（竣工结算）填充（INTERIM/CLOSE 不填），或全类型填充——按 D1 记录理由。若选 A 引入 config key 按 ErpPrjConstants+ErpPrjConfigs 既有范式落地。
  - Skill: `nop-backend-dev`
- [x] Decision D2（质保金凭证载体）：A. 扩展 `ProjectSettlementAcctDocProvider` createFacts——留存时增「借 应收账款/其他应收（质保金） / 贷 其他应付款-质保金」平衡腿（金额=retentionAmount，标 projectId 辅助核算）；B. 新 businessType（如 PROJECT_SETTLEMENT_RETENTION）+ 独立 Provider 注册——留存与返还各生成独立凭证。**判据扩展（Explore ⑥ 结论纳入）**：GL 平衡性 + 与既有 PROJECT_SETTLEMENT 凭证的解耦 + 返还时红冲可达性 + **reverseSettlement/cancel 时留存凭证生命周期一致性**（选项 B 须裁决是否同步冲销，避免悬挂）；记录理由与残留风险（科目选择经 `ErpMdSubject` 解析，subjectCode 默认回退 + billData 传递）。
  - Skill: `nop-backend-dev`
- [x] Decision D3（返还幂等与守卫载体）：返还标记载体——A. 经既有已过账凭证反查（billHeadCode=settlement.code + 返还后缀，镜像 R1.52 `#CATCHUP` 范式，零 ORM 变更）；B. remark 文本标记（弱，不推荐）。守卫链：docStatus=APPROVED（非 CANCELLED）+ approveStatus=APPROVED + posted=true（留存凭证已过账）+ retentionAmount>0 + retentionDueDate<=today + 未返还幂等。裁决并记录理由。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] D1/D2/D3 三裁决落地（理由 + 替代方案 + 残留风险记录于计划/引用文档）；Explore ⑥ reverseSettlement/cancel 交互结论纳入 D2 判据；零 ORM 变更确认（或触越界暂停声明）
- [x] returnRetention 落点（BizModel mutation + per-mutation Processor 拆分）与凭证接线方向明确，解除 Phase 2 阻塞

### Phase 2 - 质保金实现（Add）

Status: completed
Targets: `module-projects/erp-prj-service/.../processor/`（createSettlement 扩展 + 新 returnRetention Processor + Facade protected step）、`.../posting/`（Provider/Dispatcher 按 D2 裁决）、`.../entity/ErpPrjProjectSettlementBizModel.java`（mutation 注册）、`ErpPrjConstants`/`ErpPrjConfigs`/`ErpPrjErrors`（按需）
Skill: `nop-backend-dev`
Item Types: `Add`
Prereqs: Phase 1

- [x] Add：`createSettlement` 填充 retentionAmount/retentionDueDate——按 D1 裁决载体（config 比例 × finalRevenue / 到期月数），填充时保留手工覆盖路径（update 可改），零静默零填充。
      - Skill: `nop-backend-dev`
- [x] Add：`returnRetention` mutation——按 D3 守卫链扫描到期结算（retentionAmount>0 + retentionDueDate<=today + 状态/过账守卫）+ 幂等防重 + 资金面返还语义（状态/标记回写按 D3 载体）+ 失败异常显式抛出（NopException + 新 ErrorCode 或复用既有，描述中文）。
      - Skill: `nop-backend-dev`
- [x] Add：质保金凭证接线——按 D2 裁决（Provider createFacts 扩展或新 businessType + Provider 注册），留存/返还凭证行级正确性（借贷平衡 + 金额=retentionAmount + projectId 辅助核算）+ billData 增 retention 键（BILL_DATA_RETENTION_AMOUNT 等，ErpPrjConstants 常量）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 留存填充 + 返还 mutation + 凭证接线行为落地（成功与失败模式可验证：超期返还/幂等跳过/状态守卫拒绝/凭证平衡）
- [x] `mvn compile -DskipTests -pl module-projects/erp-prj-service` 通过（本地化类型检查，解除 Phase 3 阻塞）

### Phase 3 - 测试 + 文档 + 回填（Add | Proof）

Status: completed
Targets: `module-projects/erp-prj-service/src/test/`、`docs/design/projects/profitability.md`、`docs/audits/arm-index.md`、`docs/backlog/requirement-compliance-roadmap.md`、`docs/logs/2026/08-17.md`
Skill: `nop-testing`
Item Types: `Add | Proof`
Prereqs: Phase 2

- [x] Proof：新增 retention 测试组——①createSettlement 填充断言（ratio×finalRevenue + dueDate 推演，D1 载体）；②returnRetention 到期返还成功（含凭证生成 D2 载体）；③幂等（重复调用零副作用）；④守卫（非 APPROVED/未过账/未到期/retentionAmount=0 拒绝）；⑤凭证行级断言（借贷平衡 + projectId + 金额）；⑥既有 158 tests 零回归。
      - Skill: `nop-testing`
      - 落地：`TestErpPrjProjectSettlementRetention` 10 组全绿（①FINAL 填充[500.0000 + 2027-07-17]②INTERIM/CLOSE 零填充③返还成功[回链+镜像腿+projectId]④幂等⑤非 APPROVED 拒绝⑥未过账拒绝⑦未到期拒绝⑧零质保金拒绝⑨主凭证留存腿+GL 平衡⑩已返还后红冲拒绝）+ `_cases` 快照；既有 158 零回归（168 全绿）。执行期测试基建修复：`_vfs/_delta/test-prj-delta/.../app-service.beans.xml` 补 `x:extends="super"`（delta 层缺省整文件替换致同 JVM 后续测试类 prj bean not-find 顺序依赖污染，本计划 baseline beans 增 bean 触发暴露，见日志 08-17）。
- [x] Proof：`mvn test -pl module-projects/erp-prj-service` 全绿 + 全量 `mvn test` + `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh`（零漂移或 baseline-raise 登记 per-site 证据落 compliance-baseline.md）。
      - Skill: `nop-testing`
      - 结果：分域 168/0/0；全量 `mvn test` BUILD SUCCESS **3549 tests / 0 failures / 0 errors**；全量 `mvn clean install -DskipTests` BUILD SUCCESS；checker exit 0——R2c 1433→1434 + R12a 69→70 baseline-raise（`ErpPrjProjectSettlementProcessor.isRetentionReturned:311` `daoFor(ErpFinVoucherBillR)` 跨域回链 1 站点 + 其共生 `import ErpFinBusinessType`（:5/:314 businessType 枚举 name() 过滤）per-site 证据已落 compliance-baseline.md，计划预期 +1 daoFor 站点一致 + 结束审计发现 R12a 漏登记已补；R2b=235/R2d=35 零变化）。
- [x] Add：owner doc 回填——`profitability.md` §实体清单/§关键流程 补质保金实现注记 + D1/D2/D3 裁决记录；arm-index P1-RC-052 → done (RC-R1.63)（修复记录 + 历史保留）；roadmap RC-R1.63 → done ✅（行标签按 B 类裁决改写，对齐 R1.61/62 先例）；本日志条目。
      - Skill: `none`
      - 落地：profitability.md §关键流程 2 实现注记（D1/D2/D3 + 部署残留风险 + successor）；arm-index P1-RC-052 状态 todo → done (RC-R1.63)（原审计登记历史保留）；roadmap 行标签 B 类改写 + done ✅（含修复记录）；`docs/logs/2026/08-17.md` 新增条目。

Exit Criteria:

- [x] 测试组全绿 + 既有 158 tests 零回归（分域 `mvn test -pl module-projects/erp-prj-service`；全量验证属 Closure Gates）
- [x] owner doc/arm-index/roadmap 回填完成，D1/D2/D3 裁决留痕

## Draft Review Record

- Independent draft review iteration 1: **accept** (ses_ff452373cffeo7lMFdeEFk5cyG) because B 类预授权声明经 roadmap 头 :49 + R1.61/62 执行先例（零 checkbox 零双 agent）证实可辩护；baseline 逐项实仓核对通过。5 Minor 已就地折叠：①Source 字段展开器引用改为「Provider/VoucherFact（A4.2.122 须 ask-first）+ B 类裁决覆盖」；②orm.xml 列引用 803-804→826-827（+defaultValue="0"）；③B 类处理补「用户级裁决覆盖审计级 ask-first 备注」引用（对齐 R1.62 计划 :19-20）；④新增 Explore ⑥（reverseSettlement/cancel 交互）+ D2 判据扩展 + D1 结算类型范围考量；⑤Phase 3 Exit 本地化。
- Independent draft review iteration 2: **accept** (ses_ff4465f52ffethHUdURHOiWUhV) because 5 Minor 折叠全部实仓核验落地 + 无新 Blocker/Major；reverseSettlement 交互相干（Explore ⑥ → D2 判据 → Deferred 一致）。2 余 Minor 已就地折叠：①Draft Review Record 填充；②D1 选项 B 与「禁止静默零填充」张力消解（见 D1 注记）。
- Independent draft review iteration 3: **accept** (ses_ff42e5512ffe59amAypHKT4PAq) because 2 余 Minor 全部落地核验通过（D1 张力消解 + Draft Review Record）+ spot-checks 全过（orm.xml:826-827 列/ B 类裁决先例 / reverseSettlement 交互相干 / Phase 1 可执行性）+ 无新 Blocker/Major。3 项非阻塞措辞观察（可选折叠）：D1 选项 A ratio=0 为设计性 opt-in 默认需显式记录；Deferred #2 标题措辞；D3-A 凭证后缀即返还标记需显式陈述——均在执行时裁决记录中覆盖。

## Execution Record（Phase 1 Explore + D1/D2/D3 裁决）

**Explore ①-⑥ 实测结论（live HEAD，2026-08-17）：**

- ① grep `retention` 全 `erp-prj-service/src/main`（业务源码）零 writer 命中，仅 `_gen/_ErpPrjProjectSettlement` + api InputBean/OutputBean —— 零 writer 基线确认（orm.xml:826-827 列名 = **retentionAmount/retentionDueDate**，code=RETENTION_AMOUNT/RETENTION_DUE_DATE；**命名漂移核查**：实仓 Java 属性/ORM 列名即 plan 所称 retentionAmount/retentionDueDate，无 ln 前缀，按 plan 命名执行）。
- ② `ErpPrjProjectSettlementBizModel` @BizMutation 清单：createSettlement/submit/approve/reject/cancel/reverseSettlement（:53-89）——returnRetention 落点 = BizModel mutation + per-mutation Processor（镜像 reverseSettlement 拆分范式）。
- ③ `ProjectSettlementPostingDispatcher.buildEvent` billData 键集：PROJECT_ID/FINAL_REVENUE/FINAL_COST/FINAL_PROFIT/SETTLEMENT_TYPE/TRANSFER_TO_ASSET/ASSET_CARD_CODE（:83-93）——**需新增 retention 键**（BILL_DATA_RETENTION_AMOUNT/RETENTION_DUE_DATE/RETENTION_RETURN 标志，Provider 经 readDecimal 感知）。
- ④ finance `ErpFinBusinessType`：PROJECT_SETTLEMENT(430) 单值存在；新增枚举须同步 `erp-fin/business-type` 字典（orm.xml dict 变更，超 B 类边界）→ D2 选 A 零字典变更。
- ⑤ 幂等先例：R1.52 `#CATCHUP` billHeadCode 后缀范式（`ErpAstConstants.CATCHUP_BILL_SUFFIX`）；引擎 `ErpFinPostingProcessor.findBillLinks:947-952` 按 `billCode=billHeadCode + businessType` 查询 `ErpFinVoucherBillR`——**已过账凭证反查可达**（projects-service 已依赖 fin-dao，跨域 daoFor 1 站点，R2c +1 baseline-raise 登记）。
- ⑥ reverseSettlement/cancel 交互：D2 选 A 时留存腿在主结算凭证内（billHeadCode=结算单号），主结算红冲（`reverse(settlement.code, PROJECT_SETTLEMENT)`）**自动覆盖留存腿**——生命周期一致无悬挂；返还凭证（billHeadCode=结算单号#RETURN）独立存在 → **reverseSettlement/cancel 前须守卫「未返还」**（已返还拒绝红冲/取消，避免返还凭证悬挂）。

**D1 裁决=选项 A（config 驱动自动留存，仅 FINAL）**：新增 config 键 `erp-prj.settlement-retention-ratio`（默认 `0`=设计性 opt-in——留存逻辑存在且配置驱动，零为显式 opt-in 默认非静默缺失，满足「禁止静默零填充」）+ `erp-prj.settlement-retention-due-months`（默认 12）；createSettlement 仅 `FINAL`（竣工结算，L1 UC-PRJ-07 场景）自动填 `retentionAmount = finalRevenue × ratio`（scale 4 HALF_UP）+ `retentionDueDate = businessDate + N月`；INTERIM（阶段结算无尾款留存语义）/CLOSE（自建转固非应收）不填；手工覆盖路径保留（CRUD update 可改）。否决 B（纯手工录入——留存语义依赖操作员自觉，违反 L1「留存」自动行为）。ErpPrjConstants+ErpPrjConfigs 既有范式落地（镜像 settlement-require-approval）。

**D2 裁决=选项 A（扩展 ProjectSettlementAcctDocProvider，保留 PROJECT_SETTLEMENT businessType）**：createFacts 留存时增平衡腿「借 1122 应收账款-质保金 / 贷 2241 其他应付款-质保金」（金额=retentionAmount，标 projectId 辅助核算；ACCOUNT_KEY_RETENTION_RECEIVABLE/RETENTION_PAYABLE 供 GL mapping 规则覆盖，subjectCode 默认回退）；返还经同 Provider 同 businessType，billData `RETENTION_RETURN=true` 标志 + billHeadCode=结算单号#RETURN 独立凭证，镜像腿「借 2241 / 贷 1122」对冲清零。判据：GL 平衡（留存腿恒等）+ 与既有 PROJECT_SETTLEMENT 凭证解耦（返还独立红冲可达）+ **Explore ⑥ 生命周期一致性经 reverseSettlement/cancel 守卫闭合**。否决 B（新枚举须 orm.xml 字典变更超 B 类边界 + 双 Provider 冗余）。残留风险：1122/2241 科目为 Provider 默认编码，未配置 ErpMdSubject 时过账抛 ERR_SUBJECT_NOT_FOUND（部署须预置科目或配置 GL mapping 规则）。

**D3 裁决=选项 A（已过账凭证反查幂等 + 守卫链）**：返还标记 = `ErpFinVoucherBillR`（billCode=结算单号#RETURN + businessType=PROJECT_SETTLEMENT）存在性反查，零 ORM 变更，镜像 R1.52 #CATCHUP 范式；引擎 `post()` 按 (billHeadCode, businessType) 幂等为第二层防线。守卫链：docStatus=APPROVED + approveStatus=APPROVED + posted=true + retentionAmount>0 + retentionDueDate<=today + 未返还；守卫失败抛新 ErrorCode `ERR_RETENTION_RETURN_NOT_ALLOWED`（中文描述 + reason 参数）；已返还重复调用=幂等 no-op 零副作用；返还凭证过账失败显式抛 `ERR_RETENTION_RETURN_POSTING_FAILED`（与主结算过账失败隔离语义区分——返还是用户显式操作）。否决 B（remark 弱标记不可反查不可红冲）。**命名对齐**：实仓列名即 retentionAmount/retentionDueDate（审计反馈所称 lnAmount/lnDueDate 与 orm.xml:826-827 实仓不符，不采纳）。

## Closure Gates

- [x] 范围内行为完成（留存填充 + 到期返还 + 质保金凭证全落地）
- [x] 相关文档对齐（profitability.md 注记 + arm-index + roadmap + logs）
- [x] 已运行验证（`mvn test -pl module-projects/erp-prj-service` 168/0/0 + 全量 `mvn test` BUILD SUCCESS 3549/0/0 + `mvn clean install -DskipTests` BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh` actual == updated baseline[R2c=1434/R12a=70，per-site 证据落 compliance-baseline.md]）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（回合 1 FAIL[R12a 漏登记]→修复→回合 2 PASS，见 Closure Audit Evidence）
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 合同域驱动的留存比例（contract 联动）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 结算 ORM 无 contractId 维度；L1 UC-PRJ-07 未要求合同联动；留存比例按 config/结算规则（D1）已满足「留存」语义
- Successor Required: `yes`（触发条件：结算实体增加合同引用列/合同域联动立项时）

### 返还记录独立实体（若 D3 选 A 凭证反查载体）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 幂等经已过账凭证反查（billHeadCode 后缀）可达，零 ORM 变更符合 B 类边界
- Successor Required: `yes`（触发条件：返还审计追溯需求升级/ORM 变更获批时）

## Closure

Status Note: 三 Phase 全部完成（Phase 1 Explore+D1/D2/D3 裁决、Phase 2 实现、Phase 3 测试+文档+回填）；UC-PRJ-07 ④⑤ 运行时成立（FINAL 留存填充 + returnRetention 到期返还 + 质保金凭证留存/镜像腿 + 红冲/取消未返还守卫）；验证全绿（分域 168/0/0 + 全量 3549/0/0 + 构建 + checker actual == updated baseline）；两 Deferred 项预裁定带 successor 触发条件；执行期发现并修复 test-prj-delta beans.xml x:extends="super" 缺失（测试基建反模式，日志留痕）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理回合 1（ses_fefd9f1b2ffeQOLUcLtnE1pD0k）FAIL——发现 R12a 69→70 未登记漂移（本计划 `ErpPrjProjectSettlementProcessor` import ErpFinBusinessType）+ 其余 14 项 PASS；执行者修复（compliance-baseline.md R12a=70 登记 + per-site 证据 + 计划/日志同步）；独立子代理回合 2（ses_fefd1fa40ffeiDYK42BhYjQpOF）**PASS**——R12a/R2c actual == baseline 复核 + 实现点检 + 分域 168/0/0 复跑 + 文档一致性 + 零范围降级，全数验证通过
- Evidence: 回合 1/2 完整审计证据（file:line + 命令输出）见上述 task id 记录；验证命令输出存档：分域 168/0/0、全量 `mvn test` BUILD SUCCESS 3549/0/0（13:43 min）、`mvn clean install -DskipTests` BUILD SUCCESS、checker R2c=1434/R12a=70 == baseline

Follow-up:

- 合同域驱动的留存比例（Deferred But Adjudicated，successor 触发=结算实体增加合同引用/合同域联动立项）
- 返还记录独立实体（Deferred But Adjudicated，successor 触发=返还审计追溯需求升级/ORM 变更获批）
- test-prj-delta 跨 JVM 泄漏的框架级机理（本计划仅按 delta-customization.md 规则 3 修复应用层症状；launcher 探针下可见合并视图跨类残留，surefire destroy 周期内不可复现，watch-only）
