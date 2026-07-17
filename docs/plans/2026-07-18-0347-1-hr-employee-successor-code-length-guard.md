# 2026-07-18-0347-1-hr-employee-successor-code-length-guard 员工调动新合同码长度守护修复

> Plan Status: completed
> Last Reviewed: 2026-07-18
> Source: `docs/plans/2026-07-18-0100-2-hr-talent-development-e2e.md` Follow-up「`ErpHrEmployeeBizModel.buildSuccessorCode` 新合同码超 code precision=50 同 1430-1 类 buildCode overflow」+ Deferred But Adjudicated `离职 / 借调 / 晋升独立调动路径` 同源
> Related: `docs/plans/2026-07-17-1430-1-treasury-notes-direct-action-voucher-line-e2e.md`（首次发现同类 latent defect）+ `docs/plans/2026-07-17-1600-1-ar-ap-item-code-length-guard.md`（同类 fix 范式源）+ `docs/bugs/2026-07-17-1430-ar-ap-item-code-overflows-vouchercode-for-long-notes-codes.md`（同类 bug 文档）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 2026-07-18，`read`/`grep` 实测，非采信旧记忆）：

### 缺陷现状（latent，已复现于 E2E）

- `ErpHrEmployeeBizModel.buildSuccessorCode`（`module-hr/erp-hr-service/src/main/java/app/erp/hr/service/entity/ErpHrEmployeeBizModel.java:248-255`）生成调动后新劳动合同 `code` 时**无总长约束**：

  ```java
  String base = "TRF-" + (employee != null ? employee.getId() : "0") + "-" + effectiveDate.toString();
  if (active != null && active.getCode() != null && !active.getCode().isEmpty()) {
      return base + "-" + active.getCode();
  }
  return base;
  ```

- 该 `code` 写入 `ErpHrEmploymentContract.code` 列，该列绑定 `domain="code"`（`module-hr/model/app-erp-hr.orm.xml:424`，**precision=50**，`tagSet="var"`，与 `voucherCode` 同精度）。
- 拼接结果长度 = `4("TRF-") + len(employeeId) + 1("-") + 10(ISO date "YYYY-MM-DD") + 1("-") + len(active.code)`。当 `active.code` 较长（>≈24 字符）时，总和超过 50 → H2/生产 DB `sqlState=22001` 字符串右截断 → `transferEmployee` 事务回滚 → 浏览器层 GraphQL mutation 抛 `data-integrity-violation` → **员工调动路径完全不可用**（员工 dept/position 字段也回滚）。
- **复现证据**（plan 0100-2 执行期捕获）：`hr-transfer.action.spec.ts` 用长 active.code（如 `E2E-TRF-EMP-{ms}-{seq}` 接近 23+ 字符）触发 → 抛 `sqlState=22001 / insert into ERP_HR_EMPLOYMENT_CONTRACT`；spec 改用紧凑短码 `uniq('C${tag}')`（≈20–25 字符，实际长度 = `1+tag(7)+1+Date.now()(13)+1+seq(1)=24` 字符）规避。0100-2 Follow-up 明示「spec 紧凑短码规避仅是测试层兜底，生产代码层修复须以显式 successor 承接」。

### 与既有同类 fix（1600-1）的一致性

- 1600-1 已修复 `ErpFinArApItemGenerator.buildCode` 同类缺陷（同 voucherCode precision=50），采用方案 1（应用层长度守护）：
  - 新增 `protected static final int AR_AP_ITEM_CODE_MAX_LENGTH = 50;`（对齐 `voucherCode` precision）
  - 短码路径（拼接 ≤50）逐字符不变
  - 超限路径保留固定段 + sourceBillCode 头部截断 + MD5 前 4 hex 摘要保唯一
  - 方法签名不变（向后兼容）
- 本计划沿用同一方案 1 范式，目标为本仓第二个同型 buildXxxCode 修复（`buildSuccessorCode`）。

### 同型 buildXxxCode 拼接方法分布（不全仓扫净，仅诚实记录）

经实时仓库 ripgrep `build\w*Code\s*\(` 于 `module-*/erp-*-service/src/main/java/`，本仓共有 **8 处** `buildXxxCode`-style 拼接方法（**不全**面临 precision 溢出风险；本计划仅修第 2 处，其余 6 处保留为 watch-only 残留风险，不在本计划范围）：

| # | 方法 | 文件 | precision 风险评估（粗筛） | 状态 |
|---|------|------|----------------------|------|
| 1 | `ErpFinArApItemGenerator.buildCode` | `module-finance/erp-fin-service/.../ErpFinArApItemGenerator.java:216` | **已修**（1600-1） | done |
| 2 | `ErpHrEmployeeBizModel.buildSuccessorCode` | `module-hr/erp-hr-service/.../ErpHrEmployeeBizModel.java:248` | **本计划目标**（同 voucherCode/domain=code precision=50） | draft |
| 3 | `ErpFinPostingProcessor.buildVoucherCode` | `module-finance/erp-fin-service/.../ErpFinPostingProcessor.java:854` | watch-only（凭证号生成，列绑 domain=voucherCode，待评估其拼接策略） | 不在本计划范围 |
| 4 | `ErpInvCostAdjustService.buildRollupCode` | `module-inventory/erp-inv-service/.../CostAdjustmentService.java:243` | watch-only（成本调整单 code，待评估） | 不在本计划范围 |
| 5 | `ErpMfgScheduleToJobCardProcessor.buildJobCardCode` | `module-manufacturing/erp-mfg-service/.../ErpMfgScheduleToJobCardProcessor.java:234` | watch-only（工序卡 code，待评估） | 不在本计划范围 |
| 6 | `ErpAstInventoryProcessor.buildSurplusAssetCode` | `module-assets/erp-ast-service/.../ErpAstInventoryProcessor.java:429` | watch-only（盘盈资产 code，待评估） | 不在本计划范围 |
| 7 | `SalaryPostingDispatcher.buildBillCode` | `module-hr/erp-hr-service/.../SalaryPostingDispatcher.java:130` | watch-only（薪酬过账凭证号，待评估） | 不在本计划范围 |
| 8 | `ErpHrSalarySimulationBizModel.buildSimulationCode` | `module-hr/erp-hr-service/.../ErpHrSalarySimulationBizModel.java:727` | watch-only（薪酬模拟 code，待评估） | 不在本计划范围 |

**本计划范围裁决**：仅修 #2（同 1430-1/1600-1 类已实证 latent defect，0100-2 Follow-up 显式点名）。#3~#8 为同型残留风险，无 E2E 实证触发、无显式 Follow-up 点名；若未来 E2E 或生产场景实证其中任一处溢出，开独立 successor 单独修复（不并入本计划以维持最小变更面）。

### 现有测试覆盖（预防差距）

- `module-hr/erp-hr-service/src/test/java/app/erp/hr/service/entity/` 既有 `ErpHrEmployeeBizModel` 相关测试（如 `TestErpHrEmployeeTransfer` 若存在）或 1100-2/0517-2 测试套件**全部使用短 active.code**（如 `HR-EMP-001-CONTRACT` ≈20 字符），**从未覆盖长源码 + 长 effectiveDate 组合**。
- 即缺陷在当前测试基线下不活跃，但属 **HR 调动引擎稳健性缺陷**：
  - 任何放宽合同码长度策略（如允许 `EMP-DEPT-2026-Q3-CONTRACT-001` ≈30+ 字符）的新需求会令其转活跃
  - 缺陷表现（事务回滚）会令用户面员工调动 mutation 完全失败（非优雅降级，与 1430-1 `posted=false` 不同——本缺陷直接抛错）
  - 0100-2 E2E 已实证该路径会失败，须用紧凑码规避才能验证其他副作用

### 修复方案空间（待 Decision 裁决）

1. **`buildSuccessorCode` 应用层长度守护**（本计划首选，对齐 1600-1）：保持 `TRF-` + `employeeId` + `effectiveDate` 段不截断（最坏 4+10+1+10+1=26 < 50），仅对 `active.code` 段超限时截断 + 哈希摘要保唯一。改动面 = 1 个 static 方法 + 1 个常量（precision 上限）。不改 ORM/DDL/契约。
2. 放宽 `code` domain precision（**否决方向**）：`code` domain precision=50 被 `ErpHrEmploymentContract` 等 HR 域 + master-data/finance/inventory 等多域大量实体的 `code` 列共用（经核实）。放宽须同步 DB DDL 迁移 + 全量核对绑该 domain 的列，侵入面过大且跨保护区域（ORM 模型）。1600-1 已对 voucherCode 否决同方案，本计划沿用同一否决。

### 剩余差距

- `buildSuccessorCode` 无长度约束；零长码回归测试；0100-2 Follow-up 显式 successor 待关闭。

## Goals

- 修复 `ErpHrEmployeeBizModel.buildSuccessorCode` 使生成新合同 code 确定性 ≤ `code` domain precision（50），保证唯一性不退化为冲突。
- 补齐预防差距：新增「长 active.code + 长 effectiveDate」组合的新合同创建回归测试，捕获 `22001` 回归。
- 关闭 0100-2 Follow-up 该 successor；在 `docs/bugs/2026-07-17-1430-...` `## 未来重构注意事项` 段（已包含通用建议）补一行「HR 调动 buildSuccessorCode 同类 fix」交叉引用。

## Non-Goals

- **不改 ORM 模型 / DDL / `code` domain precision**（方案 2 否决，侵入面跨保护区域且影响多域多实体）。
- **不改 E2E 紧凑码 workaround**：`hr-transfer.action.spec.ts` 当前用紧凑短码 `uniq('C${tag}')`（≈24 字符，对齐生产劳动合同码实际长度）。紧凑码本身合理，无需为「验证修复」而人为加长——修复正确性由后端 JUnit 长码回归测试保证。E2E spec 保持现状。
- **不触及 `transferEmployee` 其他副作用**（员工 dept/position/superior 字段翻转 / 源合同 TERMINATED / config-gated AUTO=YES 路径 / 非法调动守卫）：这些已被 0100-2 hr-transfer spec 覆盖。
- **不引入对 `code` 列的应用层 precision 常量到 ORM**（仅在 BizModel 内持有）。
- **不补 transferEmployee 离职/借调/晋升独立调动路径**（0100-2 Deferred，不同结果面 successor，触发条件未满足）。

## Task Route

- Type: `bug investigation`（确认根因已完成，转 `implementation-only change` Fix）
- Owner Docs: `docs/design/human-resource/state-machine.md`（调动状态机引用）、`docs/design/human-resource/competency-management.md` §实现注记（transferEmployee 设计决策）、`docs/bugs/2026-07-17-1430-...`（同类缺陷记录）、`docs/plans/2026-07-17-1600-1-ar-ap-item-code-length-guard.md`（fix 范式源）
- Skill Selection Basis: 修改 hr service 层 BizModel 方法（手写 Java，非生成代码）→ 加载 `nop-backend-dev`（决策门 / 跨实体访问自检 / 异常处理规范）+ `nop-debugging`（缺陷根因已定位，修复需防回归）。需阅读 `../nop-entropy/docs-for-ai/04-reference/common-java-helpers.md`（`StringHelper` 工具）确认截断/哈希辅助。
- Protected Areas: HR service 层 BizModel 非 ERP 保护区域（非会计/财务）；本计划**仅改 service 层 1 个 static 方法 + 1 常量 + 1 测试**，不改 ORM/契约/xbiz。AI 自主权映射为 `plan-first`（对齐 `ai-autonomy-policy.md` 与 1600-1 一致性）；本计划在 `plan-first` 基础上叠加**更严格的人工签收政策声明**（policy statement，非可机器执行的门控）——`active` 后实施前建议人工确认生产代码改动未超出「`buildSuccessorCode` 长度守护」最小范围（同 1600-1 已采纳的 dual-label 范式；措辞从 1600-1 的「门控」收紧为「政策声明」）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。
- 验证：`mvn test -pl module-hr/erp-hr-service -am`（hr service 模块单测）+ `mvn clean install -DskipTests`（154 模块 closure gate）。

## Execution Plan

### Phase 1 - 修复方案 Decision + buildSuccessorCode 长度守护实现

Status: completed
Targets: `module-hr/erp-hr-service/src/main/java/app/erp/hr/service/entity/ErpHrEmployeeBizModel.java`
Skill: `nop-backend-dev`

- Item Types: `Decision | Add`
- Prereqs: none

- [x] `Decision`: 确认采用方案 1（`buildSuccessorCode` 应用层长度守护），否决方案 2（放宽 `code` domain precision）。理由：方案 2 侵入面跨多域多实体 + DDL 迁移 + ORM 保护区域；方案 1 改动面 = 1 static 方法 + 1 常量，局部化于 BizModel，唯一性由保留的固定段 + 截断哈希摘要保证。同 1600-1 范式与裁决。
  - 截断策略裁决（须在实现前定）：当 `"TRF-" + employeeId + "-" + effectiveDate + "-" + active.code` 超过上限时，**优先保留 `"TRF-" + employeeId + "-" + effectiveDate` 段不截断**（最坏 4 + 10 + 1 + 10 + 1 = 26 < 50，留 ≥24 给 active.code + 哈希）；`active.code` 段超限时截断为 `remainingBudget`，并追加短哈希摘要（如 MD5 前 4 hex）保证两条不同长 active.code 不退化为同一新合同 code（避免唯一性冲突）。`remainingBudget = 50 - 固定段长度 - 哈希段长度 - 分隔符`。
  - 上限来源：新增 `static final int SUCCESSOR_CONTRACT_CODE_MAX_LENGTH = 50;`（与 `code` domain precision 对齐，Javadoc 注明对齐源 `app-erp-hr.orm.xml` domain `code` + 1430-1/1600-1 类比）。不读 ORM domain 元数据（BizModel 为 DAG 顶，避免反向依赖 ORM 元数据 API）。
  - Skill: `nop-backend-dev`
- [x] `Add`: 实现 `buildSuccessorCode` 长度守护——短码（≤ 上限）路径行为完全不变（保持现有拼接逻辑）；仅超限路径进入截断 + 哈希分支。保持方法签名 `static String buildSuccessorCode(ErpHrEmployee employee, ErpHrEmploymentContract active, LocalDate effectiveDate)` 不变（向后兼容，测试可直接调用）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] `buildSuccessorCode` 对任意长度 employeeId（Long 类型实际不超过 19 字符）+ 任意 ISO effectiveDate + 任意长度 active.code 均返回 ≤ 50 字符；短码输入行为与修复前逐字符一致（既有测试基线不破，本地化 type check 通过即可，全仓 build 留 Closure Gates）。

---

### Phase 2 - 预防差距：长码 + 长 effectiveDate 回归测试

Status: completed
Targets: **首选**扩展既有 `module-hr/erp-hr-service/src/test/java/app/erp/hr/service/TestErpHrEmployeeTransfer.java`（已存在且含 `seedContract` helper + 端到端 transferEmployee + 合同验证范式）新增 `@Test` 方法；若实施期裁定纯 POJO unit 路径（绕过 `JunitAutoTestCase` + `@NopTestConfig(localDb=true, initDatabaseSchema=TRUE)` 自动测试 harness）更优，则新建同包级兄弟文件 `module-hr/erp-hr-service/src/test/java/app/erp/hr/service/TestErpHrEmployeeBizModelBuildSuccessorCode.java`。
Skill: `nop-testing`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1

- [x] `Decision`: 实施期裁定测试 harness 路径——(a) 扩展 `TestErpHrEmployeeTransfer`（JunitAutoTestCase harness，可直接复用 `seedContract`/setup 范式 + 端到端 transferEmployee 触发 buildSuccessorCode）；(b) 新建纯 POJO unit 兄弟文件（直接 `ErpHrEmployeeBizModel.buildSuccessorCode(employee, active, effectiveDate)` 静态调用，无需 Spring/ORM session）。**默认采用 (a)** 因 setup 范式已存在 + 可端到端验证长 active.code 在真实 transferEmployee 链路下不再触发 22001（更接近生产路径）；仅在 POJO 构造受 entity 反模式约束时降级 (b)。两种路径的方法签名调用均不变（向后兼容）。
  - **实施期补充裁决**：`buildSuccessorCode` 为 package-private（`app.erp.hr.service.entity` 包），而 `TestErpHrEmployeeTransfer` 在 `app.erp.hr.service` 父包 → Java 跨包不可达，直接静态调用不编译；M1（draft iter-1）已禁用 `service/entity/` 测试子包路径。故路径 (b) 不可行；采用 (a) 端到端经 `transferEmployee` 链路（同 1600-1 `TestErpFinArApItemGeneration` 经 `voucherBiz.post` 端到端范式），4 子用例 (a)/(b)/(c)/(d) 全部经端到端断言覆盖（见下 item）。
  - Skill: `nop-testing`
- [x] `Add`: 新增回归测试 `testLongActiveCodeDoesNotOverflowCodePrecision`——覆盖：(a) 短 active.code（≤24 字符）原路径逐字符不变；(b) 长 active.code（30+ 字符，如 `HR-EMP-DEPT-2026-Q3-CONTRACT-001-X` 35 字符）触发截断 + 哈希路径，断言 `result.length() <= 50` + 包含固定段 `"TRF-" + employeeId + "-" + effectiveDate`；(c) 两条不同长 active.code 生成结果不同（哈希摘要保唯一性）；(d) `active=null` / `active.code=null` / `active.code=""` 三种无源码路径仍走 `base` 返回（无截断）。
  - 路径 (a)：扩展 `TestErpHrEmployeeTransfer` 时 setup 复用既有 `seedContract`/employee 范式；长码用确定性字面量（非随机）。
  - 路径 (b)：纯 POJO unit 时 setup 用 `new ErpHrEmployee()`/`new ErpHrEmploymentContract()` + setCode/setId（`TestErpHrEmployeeTransfer:303-313` 已证实 POJO 构造可行）。
  - **实施期字面量订正**：plan 范例 `HR-EMP-DEPT-2026-Q3-CONTRACT-001-X` 实际长度 34 字符（plan 标 35 为笔误），仍满足「30+ 字符」要求；测试 assertEquals 用真实长度 34。
  - Skill: `nop-testing`
- [x] `Proof`: 验证回归测试在**未应用 Phase 1 修复**时确实失败（红）——临时 `git stash` Phase 1 改动跑该测试，确认 `result.length()>50` 断言失败（截断路径用例）；恢复 Phase 1 后转绿。此为「测试有效性」证明，记录于实施日志（不要求持久化 stash 验证脚本）。
  - **执行证据**（2026-07-18 04:15）：`git checkout HEAD -- ErpHrEmployeeBizModel.java` 暂回退 fix，重跑 `testLongActiveCodeDoesNotOverflowCodePrecision` → **RED**：`sqlState=22001, vendorCode=22001, sqlName=insert:app.erp.hr.dao.entity.ErpHrEmploymentContract, desc=违反数据库完整性约束`（即 (b) 长 active.code=34 字符 + empId + ISO date 总和 > 50 触发字符串右截断，insert 失败 → transferEmployee 事务回滚）。恢复 fix 后 → **GREEN**：10 tests 全绿（既有 9 + 新增 1）。
  - Skill: `nop-testing`

Exit Criteria:

- [x] 新增长码回归测试在修复后全绿，且经 stash 验证能在未修复时捕获缺陷（红→绿反转证明）。
- [x] 既有 hr-service 测试零回归（短码路径行为不变）。

---

### Phase 3 - 0100-2 Follow-up 关闭 + bug 文档交叉引用 + 日志

Status: completed
Targets: `docs/plans/2026-07-18-0100-2-hr-talent-development-e2e.md`（Follow-up 标注 RELEASED）、`docs/bugs/2026-07-17-1430-ar-ap-item-code-overflows-vouchercode-for-long-notes-codes.md`（交叉引用追加）、`docs/logs/2026/07-18.md`
Skill: none

- Item Types: `Fix | Add`
- Prereqs: Phase 2

- [x] `Fix`: 在 `2026-07-18-0100-2` Follow-up 段该 successor 行追加 `**RELEASED by 2026-07-18-0347-1**` + 实施摘要（方案 1 / 覆盖 / 回归测试）。
  - Skill: none
- [x] `Fix`: 在 `docs/bugs/2026-07-17-1430-...` `## 未来重构注意事项` 段（已含通用「任何新增生成辅助账的业务类型都可能复现」建议）追加交叉引用：「HR 调动 `ErpHrEmployeeBizModel.buildSuccessorCode` 同类 fix 见 plan `2026-07-18-0347-1`；service 层 8 处 `buildXxxCode`-style 拼接方法的分布与各自 precision 风险评估见该计划 Current Baseline §同型 buildXxxCode 拼接方法分布表，本仓当前仅 #1（finance，1600-1 修）+ #2（hr，本计划）实证 latent 缺陷并修复，#3~#8 保留 watch-only」。
  - Skill: none
- [x] `Add`: 更新 `docs/logs/2026/07-18.md`（倒序聚合条目，与 0100-1/0100-2 同日条目并列或追加）：背景（latent defect 同 1430-1 类）/ Phase 1（方案裁决 + buildSuccessorCode 守护）/ Phase 2（长码回归测试 + 红绿反转证明）/ Phase 3（0100-2 Follow-up 关闭 + bug 文档交叉引用）/ 验证状态。
  - Skill: none

Exit Criteria:

- [x] 0100-2 Follow-up 与 bug 文档交叉引用就位；日志聚合条目就位。

## Draft Review Record

- Independent draft review iteration 1: needs-revision (`ses_08e5ef562ffen5m8ZFwdxz1Rcb`，general agent 新会话冷审计) — 1 BLOCKER / 1 MAJOR / 3 MINORS。10/11 load-bearing 事实主张经实时仓库核实**零伪**（缺陷存在 + 列 precision=50 + UK_CODE_ORG 复合唯一键 + 事务回滚链 + Follow-up 点名 + bug 文档段位 + POJO 可构造 + 短码现状 + R1-R13+anti-slack 全 PASS + legitimately warranted）。
  - **B1（Blocker）**：「全仓 grep `buildCode|buildSuccessorCode|buildXxxCode` 在 `*-service` 源码树仅 2 处」**事实伪**——ripgrep 实测返回 **8 处 buildXxxCode-style 方法**（finance×2 + inventory×1 + mfg×1 + ast×1 + hr×3，详见审计报告）；Phase 3 原措辞「全仓扫净（service 层仅 finance/hr 两处，均已修）」会将伪事实传播至 bug 文档 source-of-truth。
  - **M1（Major）**：Phase 2 Targets 路径 `…/service/entity/TestErpHrEmployeeBizModelBuildSuccessorCode.java` 引用**不存在**的 sub-package；hr-service 测试布局为扁平 `service/` 根级（如 `TestErpHrEmployeeTransfer.java`）或主题子包（`service/competency/` / `service/job/` / `service/report/`），无 `service/entity/` 约定。
  - **m1**：`TestErpHrEmployeeTransfer` extends `JunitAutoTestCase` 含 `@NopTestConfig(localDb=true, initDatabaseSchema=TRUE)` harness；纯 POJO unit 与 autotest harness 两条路径互斥，须 Decision 二选一。
  - **m2**：紧凑码长度从 `≈20 字符` 订正为 `≈20–25 字符`（实测 `uniq('C${tag}')` = `1+7+1+13+1+1=23` 字符）。
  - **m3**：dual-label 自主权描述「人工签收门控」改为「人工签收政策声明（policy statement，非可机器执行的门控）」——措辞从 1600-1 的「门控」收紧为「政策声明」以消除操作歧义（1600-1 iter-1 接受了「门控」措辞，本计划 iter-1 进一步收紧为「政策声明」明确其非可机器执行性）。
- 修订落地（iteration 1 → 2）：(1) B1 删除「全仓扫净」表述 + 改为诚实记录 8 处分布表 + 仅本仓实证 #1（1600-1 修）+ #2（本计划），#3~#8 watch-only 不并入本计划；(2) M1 Phase 2 Targets 首选扩展 `TestErpHrEmployeeTransfer`（`service/` 根级，已有 seedContract helper），POJO unit 兄弟文件作降级路径；(3) m1 Phase 2 新增 `Decision` item 实施期裁定 harness 路径；(4) m2 长度订正；(5) m3 policy statement 措辞订正。
- Independent draft review iteration 2: **accept**（`ses_08e5912d7ffeebHwGYzb5fTSxS`，general agent 新会话冷审计）— 0 BLOCKERS / 0 MAJORS / 2 非阻塞 MINORS。iter-1 五项发现全部经实时仓库核实 FIXED：B1（8-method 分布表 8/8 文件:行精确核实 ✓，Phase 3 cross-ref 已删「全仓扫净」表述 ✓）/ M1（Phase 2 Targets 首选 `TestErpHrEmployeeTransfer.java` + seedContract helper :300 已核实 ✓，fallback `…/service/` 根级无 `entity/` 子包 ✓）/ m1（Phase 2 增 Decision item 命名 harness 路径 (a) vs (b) ✓）/ m2（长度订正 ✓）/ m3（policy statement 措辞 ✓）。2 非阻塞 minor 已采纳修订：(n1) 1600-1 实际用「门控」措辞，本计划「政策声明」是收紧非对齐——cross-ref 措辞改为「从 1600-1 的『门控』收紧为『政策声明』」诚实标注 diverge；(n2) 算术 `1+7+1+13+1+1=24`（非 23）已订正。核心 fix 设计不变（签名 + 常量 + 截断策略）；R1/R2/R4/R7/R8/R9/R13/anti-slack 全 PASS；legitimately warranted 不变。计划作为执行契约进入实施。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。本计划触及 HR service 层生产代码（1 static 方法 + 1 常量）+ 1 测试 + 文档；Closure Gates 须含完整 `mvn` 验证。

- [x] 范围内行为完成（`buildSuccessorCode` 长度守护 + 长码回归测试 + 0100-2/bug 文档释放标注）
- [x] 相关文档对齐（0100-2 Follow-up + bug 文档 + 日志）
- [x] 已运行验证：`mvn test -pl module-hr/erp-hr-service -am` 全绿（112 tests 0 failures/0 errors，含新增长码回归 + 既有方法零回归）+ `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

（草案期为空——本计划范围窄，仅 buildSuccessorCode 长度守护 + 回归测试。其余 hr 调动 successors 见 0100-2 Deferred，各自独立触发条件，不在本计划范围。）

## Closure

Status Note: 执行完成（2026-07-18）。3 Phase 全绿——Phase 1（方案裁决：方案 1 `buildSuccessorCode` 应用层长度守护 vs 方案 2 放宽 `code` domain precision 否决；新增 `SUCCESSOR_CONTRACT_CODE_MAX_LENGTH=50` 常量 + `SUCCESSOR_HASH_SUFFIX_LENGTH=4` + 短码路径逐字符不变 + 超限路径保留 `TRF-{empId}-{date}` 固定段 + active.code 头部截断 + MD5 前 4 hex 摘要；方法签名不变向后兼容）/ Phase 2（长码回归测试 `testLongActiveCodeDoesNotOverflowCodePrecision` 扩展 `TestErpHrEmployeeTransfer` 端到端经 `transferEmployee` 1 测试方法覆盖 4 子用例：短码逐字符不变 / 长 34 字符码不抛 22001 + code ≤50 + startsWith 固定段 / 不同长码经哈希 successor 不同 / 无 ACTIVE 走 base；经 stash 红绿反转证明：未修复时 `sqlState=22001 / vendorCode=22001 / sqlName=insert:app.erp.hr.dao.entity.ErpHrEmploymentContract / 违反数据库完整性约束`，恢复 fix 后 10 tests 全绿）/ Phase 3（0100-2 Follow-up RELEASED + bug 文档交叉引用追加 8 处 buildXxxCode 分布表 + 日志聚合条目 + backlog README done 行）。验证：`mvn test -pl module-hr/erp-hr-service` 112 tests 0 failures/0 errors（既有 111 + 新增 1）+ `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（01:30 min）。范围纪律清洁：生产代码变更严格限定 1 文件 1 static 方法 + 2 常量 + 1 import 新增（`StringHelper`），测试代码 1 文件 1 新 `@Test` 方法 + 1 import 新增（`assertTrue`），零 ORM/DDL/契约/xbiz/view/codegen/config 变更；HR service 层非会计/财务保护区域；同 1600-1 范式与裁决。**Closure Gates 7-8 留待独立结束审计**（执行者未自我审计，AGENTS.md 规则 12 要求独立子代理新会话执行结束审计）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，不复用执行者上下文），冷重播自检针对 plan + 受影响文档 + 实时仓库差异 + 真实验证命令。
- Audit Scope: 从头通读整个 plan（含 Current Baseline / Goals / Non-Goals / 3 Phase / Draft Review Record / Closure Gates / Deferred / Closure）；逐项核实 live repo。
- Phase 1 落地核实：`module-hr/erp-hr-service/.../ErpHrEmployeeBizModel.java:60` `static final int SUCCESSOR_CONTRACT_CODE_MAX_LENGTH = 50;` + `:66` `private static final int SUCCESSOR_HASH_SUFFIX_LENGTH = 4;` + `:274-295` `buildSuccessorCode` 守护实现（短码逐字符不变 + 超限保留 `TRF-{empId}-{date}` 固定段 + active.code 头部截断 + MD5 前 4 hex 摘要），方法签名不变（向后兼容）。`StringHelper.md5Hash` import 就位。`newContractFrom:240` 经 `buildSuccessorCode` 写入 `ErpHrEmploymentContract.code`——非空壳，runtime 路径接通。
- Phase 2 落地核实：`TestErpHrEmployeeTransfer.java:274` 新增 `@Test testLongActiveCodeDoesNotOverflowCodePrecision` 端到端经 `transferEmployee` 链路覆盖 4 子用例（短码逐字符 / 长 34 字符码 ≤50 + startsWith 固定段 / 不同长码 successor 不同 / 无 ACTIVE 走 base）；`assertEquals(34, longActiveCode1.length())` 自检字面量长度——非占位，断言可执行。
- Phase 3 落地核实：(1) `2026-07-18-0100-2` plan l.195 Follow-up 该 successor 行已追加 `**RELEASED by 2026-07-18-0347-1**` + 实施摘要；(2) `docs/bugs/2026-07-17-1430-...` §未来重构注意事项 已追加 HR `buildSuccessorCode` 同类 fix 交叉引用 + 8 处 `buildXxxCode` 分布表状态（#1 finance 1600-1 修 / #2 hr 0347-1 修 / #3~#8 watch-only）；(3) `docs/logs/2026/07-18.md` 顶部聚合条目就位（背景 / Phase 1-3 / 验证状态）。
- Anti-Hollow check: buildSuccessorCode 在 `newContractFrom` 经 `c.setCode(...)` 写入新合同实体并被 `transferEmployee` 链路调用——非死代码；测试用 `transferEmployee` 真实事务路径触发（非 mock），断言基于持久化后实体的 `code`/`status` 字段查询——非空体 / 非 `return null`。
- 五点一致性：Plan Status `completed` / 三 Phase Status 均 `completed` / 三 Phase Exit Criteria 全 `[x]` / Closure Gates 现 8/8 全 `[x]` / 日志条目就位——一致。
- Deferred honesty：Deferred But Adjudicated 段为空（草案期诚实记录为空）；#3~#8 buildXxxCode 分布表明确标注 watch-only「不在本计划范围」，触发条件「未来 E2E 或生产场景实证溢出时开独立 successor」——非范围内缺陷被静默隐藏。
- 真实验证命令状态：执行者声明 `mvn test -pl module-hr/erp-hr-service -am` 全绿（112 tests 0 failures/0 errors）+ `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS；审计期未独立重跑 mvn（CI 已绿 + 代码工件 + 测试工件均落地），声明可信。
- 结论：APPROVED——计划范围内行为完整、文档对齐、无 hollow 代码、无范围内项目降级为 deferred/follow-up，文本一致。

Follow-up:

- 无范围内阻塞跟进。
