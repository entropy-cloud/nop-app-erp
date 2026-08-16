# 2026-08-16-1634-2-rc-mr1-r1-58-qa-critical-item-veto RC-R1.58 — quality 关键项否决（P1-RC-040 越界项：ORM isCritical 列 + 评估器否决分支）

> Plan Status: active
> Last Reviewed: 2026-08-16
> Mission: requirement-compliance
> Work Item: RC-R1.58（P1-RC-040，UC-QA-06 关键项否决 + UC-QA-03 关键项断言复用）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.58 行 + `docs/audits/arm-index.md` P1-RC-040 行（:207）+ 展开器 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`（RC-R1.58 = ORM isCritical 列）
> Related: `docs/design/quality/use-cases.md`（L1 UC-QA-06 :101-111 + UC-QA-03 :50-64）；`docs/design/quality/inspection-integration.md`（§五 :191-224 + §十 :333）；`docs/audits/2026-08-05-1830-2-rc-ma1-a1-31-quality-f1-inspection-gating.md`（A1.31 §5 UC-QA-06 :196-198 + §6 P1-RC-040 :238）；`docs/audits/2026-08-07-2359-rc-ma4-a4-2-101-112-quality-f1-f2-f3-runtime.md`（A4.2.101 :32-39）
> Audit: required

## Current Baseline

- **finding P1-RC-040（arm-index:207，UC-QA-06）**：L1（`use-cases.md:107-109`）逐字「质检模板行.是否关键项 == true 且 该行不合格 → 整体质检单 = REJECTED（关键项否决，无论其他项）」「非关键项不合格 → 可让步（CONDITIONAL）」；UC-QA-03（:60）「关键项不合格 → 不可让步（直接 REJECTED）」。L3 实仓（HEAD 核查）：
  - ORM `ErpQaInspectionTemplateLine`（`app-erp-quality.orm.xml:315-350`）：isRequired（:326 propId 8，INTEGER mandatory defaultValue=1，"是否必检"语义≠关键项）、inspectionMethod（propId 9）、sortNum（propId 10）——**无 isCritical 列**；`ErpQaInspectionLine`（:244-293）：id/inspectionId/lineNo/parameterId/parameterName/specMin/specMax/measuredValue/unit/result/remark（propId 1-11）——**无 isCritical 列**；grep `isCritical|criticalItem|关键项` 跨 `module-quality/{erp-qa-service,erp-qa-dao}/src/main` + orm.xml **零业务命中**（仅 SPC SEVERITY_CRITICAL 无关）；**isCritical 新列 propId 分配基准（propId 连续校验）**：TemplateLine 现有 propId 1-16（updateTime=16，orm.xml:334）→ 新列 propId **17**；Line 现有 propId 1-17（updateTime=17，orm.xml:264）→ 新列 propId **18**；**禁止中段插入/系统列重编号**（重编号破坏纯加性边界，Phase 1 D1 显式排除）；
  - `InspectionResultEvaluator.aggregate:73-92` **无关键项否决分支**——`:87-92` 仅 `anyRejected → allowConcession ? CONDITIONAL : REJECTED`，**关键项不合格 + allowConcession=true 错误产出 CONDITIONAL，直接违反 L1 :108-109**；
  - `evaluateLine:28-52` 是通用 out-of-spec REJECTED 探测，无关键项维度（行级评测正确，缺否决语义）；
  - `ErpQaInspectionRecordResultProcessor.recordResult:40-81`：applyLineResults（显式行结果）→ evaluateLine（缺省行结果）→ `aggregate(lines, concession, inspection.getCode())` :67 → setResult :68 → posted=true + 让步 APPROVED（:70-74）→ REJECTED 时 autoCreateNcrFromInspection（:78-81）；
  - **模板行→质检单行复制**：`InspectionTemplateMatcher.match`（TemplateLineSpec 仅 parameterName/specMin/specMax/unit 四字段）+ `ErpQaInspectionCreateForBusinessBillProcessor.copyTemplateLinesToInspection`（复制四字段）——**isCritical 复制链缺失**；
  - L4 `testRejectedCriticalGoesRejected:84` **命名误导**——实为 `allowConcession=false` 通用拒绝断言（无关键项标记可 seed），未覆盖「关键项否决覆盖让步」路径（Q1 根因：测试名误导致基线误判）。
- **Q4 判据**：§2 P1①（功能完全缺失——数据模型 + 评估器逻辑双缺）+ P1⑤（断言误导）。**product-scope 确认义务**：arm-index:207 状态列标注「先须人工确认 product-scope」——inspection-integration.md §十:333 + §5.3:219-222 + L1 均显式声明关键项否决，且 A4.2.101 运行时确认（:39）已按 Q4=(a) 裁决维持 P1 强制实现；2026-08-15 升级（ai-autonomy-policy.md:83）使越界项不再暂停等待人工确认 → **本计划显式裁决：product-scope 未裁剪 → Q4=(a) P1 强制实现，确认义务由 A4.2.101 Q4=(a) 裁决 + 本裁决声明闭合，审计不复开**。A4.2.101 运行时确认维持 P1（:39）。
- **2026-08-12 批量裁决 A 类**（roadmap 头 :31-38）：quality RC-R1.58（ErpQaInspectionTemplateLine + ErpQaInspectionLine 加 isCritical 列）ORM 修改授权已批量批准（对齐 Q3 纯加性类自动执行范围：加列/加 UK/新增实体）；越界回落双独立子 agent 批准（2026-08-15 升级：保护区域 ORM 修改须双独立子 agent 批准，批准记录落盘计划文件）。
- **测试基线**：`TestErpQaInspectionStateMachine`（13 @Test，含 testRejectedCriticalGoesRejected:84 误导名）；`TestErpQaInspectionTrigger`（强制质检门控）；`TestErpQaInspectionTemplateCrudSmoke`（模板头-行 CRUD，:137 lineData.put("isRequired",1)）；`statemachine/TestErpQaInspectionResultStateMachineMatrix`（4 态矩阵）。erp-qa-service **172 tests 全绿**（R1.26 基线）。
- **compliance 基线**：R2c=1420 / R2b=233 / R2d=35；isCritical 读取经 ORM getter（to-one/同实体列）零新增 daoFor 面预期。

## Goals

- **UC-QA-06/03 关键项否决运行时成立（P1-RC-040 核心）**：关键项行不合格 → 整体 REJECTED（**覆盖 allowConcession，跳过让步**）——`InspectionResultEvaluator.aggregate` 增否决分支 + `ErpQaInspectionLine.isCritical` 消费。
- **ORM 双实体加 isCritical 列**（Q3 纯加性，2026-08-12 A 类已授权）：`ErpQaInspectionTemplateLine` + `ErpQaInspectionLine` 各加 1 列（INTEGER 0/1 或 BOOLEAN，Phase 1 D1 定稿），无 NOT NULL/默认/索引/UK（纯加性边界）。
- **模板→行复制链扩展**：TemplateLineSpec + InspectionTemplateMatcher.match + copyTemplateLinesToInspection 复制 isCritical（模板定义关键项 → 质检单行继承）；手工建行（无模板）可经 CRUD 直设。
- **测试补强（P1⑤ 断言误导修复）**：`testRejectedCriticalGoesRejected` 更名 + 新增关键项否决覆盖让步路径测试（关键项 REJECTED + allowConcession=true → REJECTED）——Q1 根因测试名误导致基线误判的修复。
- **零回归**：erp-qa-service 全量测试（172 基线）全绿 + 全仓 `mvn test` + 全量构建 + compliance checker 零漂移。
- **owner doc 收敛**：inspection-integration.md §五 补关键项否决实现注记（D1-D3 裁决）；arm-index P1-RC-040 → done (RC-R1.58) + roadmap 行 done + logs 条目。

## Non-Goals

- **不实现关键项行级 UI 标记样式/AMIS 定制**（isCritical 字段经 XMeta 生成展示，无页面定制——产品化打磨归 successor）。
- **不实现多关键项权重/关键项优先级排序**（L1 无此语义：任一关键项不合格即否决）。
- **不改 evaluateLine 行级评测逻辑**（行级评测正确，仅 aggregate 层加否决语义）。
- **不实现"关键项必检"联动**（isRequired 与 isCritical 正交：必检≠关键项，L1 无联动要求）。
- **不改真相源契约段落**（use-cases L1 不动；inspection-integration.md 契约段不动，仅补实现注记）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧修复：ORM 结构变更[Q3 纯加性加列，2026-08-12 A 类已授权] + 评估器否决分支 + 复制链扩展；Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/quality/use-cases.md`（L1 UC-QA-06/03）+ `docs/design/quality/inspection-integration.md`（§五/§十）
- Skill Selection Basis: ORM 模型变更（加列）→ 增量重生成 `mvn clean install -DskipTests`；BizModel/评估器实现（`nop-backend-dev`：静态工具类 + per-mutation Processor 模式 + IBiz 注入）；测试（`nop-testing`：JunitBaseTestCase + GraphQL 引擎 + `_cases/` 快照）。

## Infrastructure And Config Prereqs

- 无新 config 键（否决语义为 L1 硬契约非配置门控；Phase 1 D4 裁决是否加 `erp-qua.critical-item-veto-enabled` 门控——倾向不加，L1 无 config 语义，零 config 零回归）。
- ORM 变更（双实体加列）→ 须 `mvn clean install -DskipTests` 增量重新生成（**不要重跑 nop-cli gen**），生成产物核对含 propId 分配 + DDL 三方言。
- 双独立子 agent 批准 checkbox（2026-08-15 升级：ORM 保护区域修改须两个独立子 agent 分别检查批准，批准记录落盘本计划 Phase 2）。

## Execution Plan

### Phase 1 - 列定义/否决语义/复制链/门控裁决（Decision）

Status: planned
Targets: `module-quality/model/app-erp-quality.orm.xml`（isCritical 列草案）；`docs/design/quality/inspection-integration.md`（§五注记草案）
Skill: `nop-backend-dev`

- Item Types: `Decision`
- Prereqs: none

- [ ] `Decision` D1 列定义：isCritical 类型/默认值裁决——选项 A（推荐）INTEGER defaultValue=0 mandatory（对齐 isRequired 同实体先例 :326）；选项 B BOOLEAN defaultValue=false；选项 C 可空无默认（null=非关键项，Q3 纯加性最保守）——记录选择 + propId 分配（**TemplateLine propId 17 / Line propId 18**，顺延 updateTime 之后；**禁止中段插入/系统列重编号**——重编号破坏纯加性边界，平台 propId 连续校验仅约束新增连续不约束重排）。
      - Skill: `nop-backend-dev`
- [ ] `Decision` D2 aggregate 否决分支语义：任一 `line.result==REJECTED` 且 `line.isCritical`（null/0=非关键项）→ 强制返回 REJECTED（跳过 allowConcession）；关键项 ACCEPTED 或非关键项 REJECTED → 维持既有三分支；否决判定在 aggregate 循环内（evaluateLine 后/显式结果后统一读 line.getResult()）。
      - Skill: `nop-backend-dev`
- [ ] `Decision` D3 复制链扩展：TemplateLineSpec 增 isCritical 字段（构造器/Getter 扩展）→ InspectionTemplateMatcher.match 传值 → copyTemplateLinesToInspection 复制；手工建质检单行（无模板）isCritical 经 CRUD 直设；recordResult 不传 isCritical（否决仅读行实体字段）。
      - Skill: `nop-backend-dev`
- [ ] `Decision` D4 门控：不加 config 门控（L1 无 config 语义）vs 加 `erp-qua.critical-item-veto-enabled` 默认 TRUE——记录理由（倾向不加：否决是验收标准硬契约，门控化会引入部署开关掩盖契约缺失；A4.1.4 config-gate 范式仅适用可配置行为）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] D1-D4 各记录选择 + 替代方案 + 残留风险（写入计划或 inspection-integration.md 注记）
- [ ] orm.xml 双实体 isCritical 列草案 well-formed（`xmllint --noout` 通过）

### Phase 2 - ORM 落地 + 评估器/复制链实现（Add|Fix）

Status: planned
Targets: `module-quality/model/app-erp-quality.orm.xml`（isCritical 列）；`module-quality/erp-qa-service/.../entity/InspectionResultEvaluator.java`（aggregate 否决分支）；`module-quality/erp-qa-service/.../entity/TemplateLineSpec.java` + `InspectionTemplateMatcher.java` + `processor/ErpQaInspectionCreateForBusinessBillProcessor.java`（复制链）；生成产物
Skill: `nop-backend-dev`

- Item Types: `Add | Fix`
- Prereqs: Phase 1

- [ ] `Add` orm.xml 双实体加 isCritical 列（D1 定稿）+ `mvn clean install -DskipTests` 增量重生成，生成产物核对（实体 getter/setter + XMeta + DDL 三方言）
      - Skill: `nop-backend-dev`
- [ ] **双独立子 agent 批准 checkbox（ORM 保护区域，2026-08-15 升级）**：两个独立子 agent（fresh session）分别检查批准本次 ORM 变更（纯加性加列，无既有语义变更/无删除/无迁移），批准记录落盘本计划（ses id + 结论）
      - Skill: `nop-backend-dev`
- [ ] `Fix` InspectionResultEvaluator.aggregate 增否决分支（D2）：循环内 collect criticalRejected——任一关键项行 REJECTED → 强制 REJECTED；javadoc 更新三态语义
      - Skill: `nop-backend-dev`
- [ ] `Add` 复制链扩展（D3）：TemplateLineSpec + InspectionTemplateMatcher.match + copyTemplateLinesToInspection 增 isCritical 字段复制
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] orm.xml 加列 + 生成产物一致（实体 getter/setter + DDL）；propId 无冲突
- [ ] 双独立子 agent 批准记录落盘（两个 APPROVE 结论 + 检查范围）
- [ ] `mvn compile -pl module-quality/erp-qa-service -am` 通过 + aggregate javadoc 与实现一致

### Phase 3 - 测试 + 文档回填（Proof）

Status: planned
Targets: `module-quality/erp-qa-service/src/test/java/app/erp/qa/service/TestErpQaInspectionStateMachine.java`（测试更名 + 新增）；`docs/design/quality/inspection-integration.md`（§五注记）；`docs/audits/arm-index.md`（P1-RC-040 行）；`docs/backlog/requirement-compliance-roadmap.md`（RC-R1.58 行）
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 2

- [ ] `Proof` `testRejectedCriticalGoesRejected:84` 更名（如 testRejectedNoConcessionGoesRejected）修正误导 + 新增 `TestErpQaCriticalItemVeto` 或同文件新增组：①关键项 REJECTED + allowConcession=true → REJECTED（否决覆盖让步，P1-RC-040 核心断言）；②关键项 ACCEPTED + 非关键项 REJECTED + 让步 → CONDITIONAL（不否决）；③关键项 REJECTED + 无让步 → REJECTED（既有语义保持）；④模板行 isCritical=1 → 质检单行 isCritical=1（复制链断言，createForBusinessBill 路径）；⑤手工建行 isCritical 直设 + aggregate 否决；⑥`_cases/` 快照录制（对齐既有测试范式）
      - Skill: `nop-testing`
- [ ] `Proof` 零回归验证：`mvn test -pl module-quality/erp-qa-service` 全绿（172 基线 + 新增）+ 快照重录核验 + `mvn clean install -DskipTests` 全量构建 BUILD SUCCESS
      - Skill: `nop-testing`
- [ ] `Proof` compliance checker 复跑：`bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline（预期零新增 daoFor 面——isCritical 经同实体 getter/模板行复制，无跨域站点）；若漂移分类 per-site 证据
      - Skill: none
- [ ] `Add` 文档回填：inspection-integration.md §五 补关键项否决实现注记（D1-D4 裁决 + isCritical 列 + 复制链 + aggregate 否决语义）；arm-index P1-RC-040 → done (RC-R1.58)；roadmap RC-R1.58 行 done ✅；`docs/logs/2026/08-16.md` 日志条目
      - Skill: none

Exit Criteria:

- [ ] 关键项否决 ①-⑥ 全绿（指定成功/失败模式：否决覆盖让步断言逐项 + 复制链断言 + 快照落盘）
- [ ] erp-qa-service 全量测试全绿（172 基线零回归，失败模式=任何既有测试翻红）
- [ ] compliance checker actual ≤ baseline（或 baseline-raise 带 per-site 证据）

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_ff63ad59fffewjX2X9Z7swE8uR`) — 1 MAJOR + 7 MINOR。M1 已修订：propId 分配修正为 TemplateLine propId 17（updateTime=16 之后）/ Line propId 18（updateTime=17 之后）+ baseline 补 next-free-propId 基准 + 显式排除中段插入/系统列重编号；m1 已修订：isRequired 引用 :332→:326；m2 已修订：inspection-integration.md §五 范围 :185-186→:191-224；m3 已修订：UC-QA-03 范围 :39-60→:50-64；m4 已修订：recordResult :40-90→:40-81；m5 已修订：Infra prereqs D3→D4 交叉引用修正；m6 已修订：Phase 1 Item Types `Decision | Add`→`Decision` + 文档回填项补 `Add` 类型标签；m7 已修订：baseline 补 product-scope 确认义务显式裁决声明（A4.2.101 Q4=(a) 裁决 + 2026-08-15 升级闭合，审计不复开）。
- Independent draft review iteration 2: `acceptable as-is` (`ses_ff631fd12ffe3mJ8T9DwwEPuEf`) — 迭代 1 全部 8 项修复逐项核实 FIXED（M1 propId 17/18 + next-free baseline + 排除重编号 :14/:65；m1 :326；m2 §五 :191-224；m3 :50-64；m4 :40-81；m5 D4；m6 `Decision` + `Add` 标签；m7 product-scope 裁决 :20）；独立基线全 PASS（aggregate 无否决分支 :87-92 / 双实体无 isCritical / recordResult:67 aggregate / TemplateLineSpec 4 字段复制链 / testRejectedCriticalGoesRejected:84 误导 / R2c=1420 / 172 tests）；格式合规 PASS 无反松弛词；ORM 双 agent checkbox 在位（:52/:90）。2 项非阻塞 MINOR（TestErpQaInspectionStateMachine @Test 计数 9→13 已就地修正；roadmap 头行号 :34→:31 已就地修正）。草案审查收敛 → `Plan Status: draft → active`。

## Closure Gates

- [ ] 范围内行为完成（isCritical 列 + aggregate 否决 + 复制链 + 测试补强）
- [ ] 相关文档对齐（inspection-integration.md/arm-index/roadmap/logs）
- [ ] 已运行验证（`mvn test -pl module-quality/erp-qa-service` + `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh`）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 关键项行级 UI 标记样式（AMIS badge/高亮）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: isCritical 字段经 XMeta 生成展示；视觉定制属产品化打磨非 L1 契约
- Successor Required: `no`

### 关键项必检联动（isRequired × isCritical）

- Classification: `watch-only residual`
- Why Not Blocking Closure: L1 未要求联动语义（必检≠关键项）；当前正交实现
- Successor Required: `no`

## Closure

Status Note: <待执行完成>

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计>
- Evidence: <待执行>

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
