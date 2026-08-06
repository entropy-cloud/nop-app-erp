# RC MA4 A4.2.14/15/16 — HR 员工/组织域运行时配置覆盖与未到岗逃生路径验证（A1.12 §7-3/§7-4/§7-5）验证报告

> Audit Status: closed
> 里程碑：MA4（代码与前端质量层 / 运行时行为验证）
> 工作项：A4.2.14 / A4.2.15 / A4.2.16（MA4 运行时行为验证 — A1.12 §7-3/§7-4/§7-5：UC-HR-12 评估聚合权重运行时配置覆盖 + UC-HR-08 handleContract 三态运行时行为 + UC-HR-05 未到岗回退运行时处理）
> 验证 plan：`docs/plans/2026-08-07-0530-1-rc-ma4-a4-2-14-16-hr-employee-org-runtime-config-escape.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§2 分级判据 / §4 Q1 真相源层级与冲突裁决 / §7 arm-index 衔接 / §8 过程纪律自检 / §9 真相源冻结 / §去重协议）
> 输入存疑点：A1.12 §7-3/§7-4/§7-5（`docs/audits/2026-08-02-2328-rc-ma1-a1-12-hr-f1-employee-organization.md:322-326`）
> 关联既有裁决：A1.12 §5 UC-HR-08 = **接受**（合同三态 AUTO/YES/NO config-gated）/ UC-HR-12 = **接受**（权重 config 驱动默认 15%/50%/25/10%）/ UC-HR-05 = **P2**（P2-RC-010 未到岗回退异常路径未实现）
> 关联同型范式：A4.2.12（`2026-08-06-2247-rc-ma4-a4-2-12-hr-contract-expiry-cron-wiring.md`，done — config-gate 部署普查范式，本验证对齐其「config-gate default-off = 部署启用决策非契约缺失」判据框架）+ A4.2.13（`2026-08-06-2247-rc-ma4-a4-2-13-hr-contract-expiry-multi-tier-alert.md`，done — config 部署普查同型范式）+ A4.1.4（budget config 默认关闭 → 维持接受 0 finding 先例）+ A4.1.7（commitment-release-on-return 默认关闭 → 维持接受 0 finding）
> 关联 finding：`P2-RC-010`（UC-HR-05 ⑱未到岗回退异常路径未实现，watch-only successor MR1）+ `P2-MA2-048`（招聘 close 无守卫，watch-only）+ `P1-MA2-039`（员工 RESIGNED 死状态，resolved R1.15）
> 验证性质：**只读运行时配置覆盖普查 + 逃生路径可达性探查**（grep 全 20 生产 application.yaml config override + config getter 默认值 census + 逃生路径代码可达性分析；不改代码/ORM/api.xml/config 默认值/真相源；方法论 §5 保护区域，roadmap 预授权类目「只读评估」）
> 验证日期：2026-08-07
> 验证者：主代理（独立结束审计由独立子代理执行，见 plan §Closure）

---

## 0. 验证结论（TL;DR）

| 项 | 结果 | 处置 |
|---|---|---|
| **A1.12 §7-3（A4.2.14）存疑点裁决** | **维持 UC-HR-12 接受**（评估聚合权重 config-gate 完整 + 默认值与 L1 一致 + 全 20 生产 application.yaml 零非默认权重 override → config-gate = 部署启用决策非契约缺失，对齐 A4.1.4/A4.2.12 范式） | 不升级 finding |
| **A1.12 §7-4（A4.2.15）存疑点裁决** | **维持 UC-HR-08 接受**（handleContract 三态 AUTO/YES/NO config-gate 完整 + AUTO 分支依赖 `transferAutoHandleContract()` 默认 true + 全 20 生产 application.yaml 零 false override → 主路径满足） | 不升级 finding |
| **A1.12 §7-5（A4.2.16）存疑点裁决** | **维持 P2-RC-010 watch-only**（`hire`→HIRED 终态无 rollbackHire grep 零业务命中 CONFIRMED；逃生路径 close 无守卫[任意态→CLOSED] + useLogicalDelete[平台标准逻辑删除] + 重新申请新 ErpHrRecruitment 三步代码可达且可操作；但 owner doc `recruitment.md §关键业务规则 #3` 仅覆盖 REJECTED 重申不覆盖 HIRED 未到岗 → 逃生路径可达但文档引导缺口 = P2-RC-010 方案 B 修复义务归 MR1） | 不撤销 watch-only / 不升级 P1 |
| 全 application.yaml 部署 override 普查 | **三项 config 键全部零命中**（全 20 生产 application.yaml + README/seed/部署运维文档 — 无任何站点设 `erp-hr.assessment-*-weight` / `erp-hr.transfer-auto-handle-contract` override） | 生产环境默认值 = 代码默认值 |
| 新 finding | **0**（对齐 A4.2.12/A4.2.13/A4.1.4 = 0 新 finding；P2-RC-010 运行时现状确认追加注记不撤销） | 无新控制点 |
| MR0 触发 | **无** | — |

**整体裁决**：A1.12 §7-3/§7-4/§7-5 三个静态存疑点经全 20 生产 application.yaml config override 普查 + config getter 默认值 census + 逃生路径代码可达性分析 **CONFIRMED**：

- **§7-3（A4.2.14）**：`AssessmentAggregator.java:75-78` 四权重经 `ErpHrConfigs.assessment*Weight()` config 驱动，默认值 0.15/0.50/0.25/0.10（`ErpHrConfigs.java:33-39` 常量 + `:129-151` getter）与 L1 `use-cases.md:143`「默认 SELF 15%/MANAGER 50%/PEER 25%/SUBORDINATE 10%」+ L2 `competency-management.md:164` + `:191-195` 配置点表 **逐字一致**；全 20 生产 application.yaml 零 override → config-gate = 部署启用决策，**维持 UC-HR-12 接受**。
- **§7-4（A4.2.15）**：`ErpHrEmployeeBizModel.resolveHandleContract:212-233` 三态 AUTO/YES/NO，AUTO 分支 `:220-222` `shouldHandle = ErpHrConfigs.transferAutoHandleContract()` 默认 true（`ErpHrConfigs.java:43` 常量 + `:159-163` getter）；全 20 生产 application.yaml 零 false override → 调动时自动处理合同主路径默认满足，**维持 UC-HR-08 接受**。
- **§7-5（A4.2.16）**：`ErpHrRecruitmentHireProcessor.hire:37-49` 将 status 设 HIRED（终态，OFFERED 守卫 `:39`），grep `rollbackHire|undoHire|revertHire|cancelHire|revokeHire` 跨全 module-hr **零业务命中** CONFIRMED P2-RC-010 静态判定；逃生路径三步代码可达（`close:130-135` 无守卫任意态→CLOSED [P2-MA2-048 watch-only 证据] + CrudBizModel 标准 `delete` mutation → `dao().deleteEntity()` 逻辑删除[`useLogicalDelete="true"` `app-erp-hr.orm.xml:793`] + 标准 CRUD save 新建 ErpHrRecruitment[`defaultPrepareSave:57-66`→OPEN]）；但 owner doc `recruitment.md §关键业务规则 #3` 逐字「候选人状态不可逆：REJECTED 不可回退（误操作通过黑名单恢复模式处理）」**仅覆盖 REJECTED 重申，未覆盖 HIRED 未到岗** → 逃生路径代码可达但文档引导缺口 + 已创建的 Employee/Contract 成孤儿需独立清理 → **维持 P2-RC-010 watch-only 不撤销**（修复义务归 MR1 R1.0 展开器）。

按 §2 分级判据三源复核三项均无 P0/P1 升级：①**§2 接受判据满足**（A4.2.14/A4.2.15 config-gate 机制完整 + 默认值与 L1 一致，L1 未要求「开箱不可配置」）；②**§2 P1②「异常路径未实现」对 A4.2.16 不成立**——存在 close+重开+逻辑删除三步可达逃生路径（非完全缺失），属"边界场景弱"§2 P2①；③**§2 P0④ 会计过账正确性破坏**对三项均不适用（权重/合同处理/招聘状态均不直接破坏 GL）。

**不触发 MR0，无新 finding，维持 A1.12 §5 三项既有裁决（UC-HR-12 接受 / UC-HR-08 接受 / UC-HR-05 P2）。** A4.2.14/A4.2.15 登记 config-gate watch-only residual（部署启用决策，记于本报告非 arm-index finding 行，对齐 A4.2.12 范式）；A4.2.16 P2-RC-010 运行时现状确认注记追加（不撤销 watch-only）。本验证**不实施 config 默认值变更或 rollbackHire 实现**（plan Non-Goals），仅本报告落盘 + roadmap/log/arm-index 注记同步。

> **与 A4.2.12/A4.2.13 方向一致性声明**：三者同 A1.12 §7 族（HR 域 config-gate 部署普查 + 运行时行为确认），均裁决「维持 A1.12 §5 既有结论 + 0 新 finding + config-gate watch-only residual 记录」。A4.2.12（cron 接线，config-default-off 双层关闭）+ A4.2.13（30/60/90 多档预警，单阈值 default-30）+ 本验证 A4.2.14（权重 default-L1 一致）+ A4.2.15（合同处理 default-true 主路径满足）四项 config-gate 范式一致；A4.2.16（逃生路径可达性）属代码可达性探查非 config 普查但同收口 A1.12 §7 族。

---

## 1. 输入存疑点原文 + L1/L2/L3 锚点

### 1.1 输入存疑点原文（A1.12 §7-3/§7-4/§7-5，逐字引用）

> **§7-3 UC-HR-12 评估聚合权重运行时配置覆盖**：`ErpHrConfigs.assessment*Weight()` 默认 15%/50%/25%/10% + `AppConfig.var` 可覆盖。运行时是否有非默认配置覆盖需 MA4 确认（静态确认 config 驱动非硬编码）。
>
> **§7-4 UC-HR-08 handleContract 三态运行时行为**：AUTO 模式依赖 `ErpHrConfigs.transferAutoHandleContract()` 默认 true。运行时 config 是否被覆盖需 MA4 确认。
>
> **§7-5 UC-HR-05 未到岗回退运行时处理**：P2-RC-010 未实现——运行时 HR 是否经 close+重开处理未到岗场景需 MA4 探查（静态确认无 rollbackHire mutation）。
> — `docs/audits/2026-08-02-2328-rc-ma1-a1-12-hr-f1-employee-organization.md:322-326`

### 1.2 A1.12 §5 既有裁决（输入，本验证复核分层一致性）

| UC | A1.12 §5 裁决 | 关键断言 | 本验证复核对象 |
|----|--------------|---------|---------------|
| UC-HR-12 | **接受** | ㊴权重聚合 SELF 15%/MANAGER 50%/PEER 25%/SUBORDINATE 10%——`AssessmentAggregator.aggregate:38-104` + 权重经 `ErpHrConfigs` **config 驱动**（默认值与 L1 一致）✓ | §7-3：运行时是否有非默认权重 override（部署普查） |
| UC-HR-08 | **接受** | ㉚原合同 TERMINATED+新合同——`resolveHandleContract:212-233` 三态 AUTO/YES/NO + config-gated ✓ | §7-4：运行时 config 是否被 false 覆盖（部署普查） |
| UC-HR-05 | **P2**（P2-RC-010） | ⑱「候选人接受 Offer 后未到岗需状态回退」——**未实现**。`hire` 设 HIRED 终态无回退 mutation，有 close+重开逃生路径 | §7-5：逃生路径运行时可达性 + owner doc 覆盖面（代码探查） |

本验证**不重复核实**断言代码逻辑（A1.12 §5 已证 + A2.7a 状态机复用 pass），只评 config 部署普查 + 逃生路径运行时可达性差异。

### 1.3 L1 需求契约（逐字）

**UC-HR-12 ㊴**（`use-cases.md:143`）：「全部提交后系统按权重聚合（默认 SELF 15%/MANAGER 50%/PEER 25%/SUBORDINATE 10%）」——L1 显式标注「默认」，**未要求固定不可配置**。

**UC-HR-08 ㉚**（`use-cases.md:95`）：「如有劳动合同，标记原合同→TERMINATED，创建新合同」+ `:99` 注记「合同处理三态 `handleContract` AUTO/YES/NO + config-gated」。

**UC-HR-05 ⑱**（`use-cases.md:60`）：「异常：候选人接受 Offer 后未到岗需状态回退」——L1 显式声明异常路径。

### 1.4 L2 owner doc 契约（设计参考，非真相源）

- `competency-management.md:164`「默认权重：SELF=15%、MANAGER=50%、PEER=25%、SUBORDINATE=10%（可配置）」+ `:191-195` 配置点表登记 `erp-hr.assessment-*-weight` 默认值 → 与 L1 + L3 默认值三方一致。
- `recruitment.md §十 关键业务规则 #3:396`「候选人状态不可逆：REJECTED 不可回退（误操作通过黑名单恢复模式处理）」——**仅覆盖 REJECTED 重申，未覆盖 HIRED 未到岗**（本验证 A4.2.16 关键发现）。

### 1.5 L3 实现锚点（live repo 实测，本验证复核）

| 组件 | file:line（写时实测） | 行为断言 |
|------|----------------------|----------|
| 权重聚合引擎 | `module-hr/erp-hr-service/.../competency/AssessmentAggregator.java:75-78` | `selfW=ErpHrConfigs.assessmentSelfWeight()` / `mgrW=...ManagerWeight()` / `peerW=...PeerWeight()` / `subW=...SubordinateWeight()` 四权重 config 驱动 |
| 权重 config getter | `module-hr/erp-hr-service/.../ErpHrConfigs.java:129-151` | 四 getter 经 `AppConfig.var(CONFIG_*, DEFAULT_*)` 读取，默认常量 `:33-39` = 0.15/0.50/0.25/0.10 |
| 权重 config 键 | `module-hr/erp-hr-service/.../ErpHrConstants.java:156-162` | `erp-hr.assessment-self-weight` / `-manager-weight` / `-peer-weight` / `-subordinate-weight` |
| 合同三态处理 | `module-hr/erp-hr-service/.../entity/ErpHrEmployeeBizModel.java:212-233` | `resolveHandleContract`：YES→handle / NO→skip / AUTO→`shouldHandle=ErpHrConfigs.transferAutoHandleContract()`（`:220-222`） |
| 合同 config getter | `ErpHrConfigs.java:159-163` | `transferAutoHandleContract()` 经 `AppConfig.var(CONFIG_TRANSFER_AUTO_HANDLE_CONTRACT, DEFAULT_TRANSFER_AUTO_HANDLE_CONTRACT=true)`（`:43`） |
| 合同 config 键 | `ErpHrConstants.java:202` | `erp-hr.transfer-auto-handle-contract` |
| hire 终态 | `module-hr/erp-hr-service/.../processor/ErpHrRecruitmentHireProcessor.java:37-49` | `hire`：OFFERED 守卫 `:39`→`setStatus(HIRED):41` + 跨实体创建 Employee `:43` + Contract `:47` + employeeId 回写 `:44` |
| close 无守卫 | `module-hr/erp-hr-service/.../entity/ErpHrRecruitmentBizModel.java:130-135` | `close`：`setStatus(CLOSED)` 无 status 守卫（任意态可 CLOSED，[P2-MA2-048 watch-only 证据]） |
| 招聘 ORM 逻辑删除 | `module-hr/model/app-erp-hr.orm.xml:793` | `<entity ... useLogicalDelete="true" deleteFlagProp="delVersion" ...>`（`delete` mutation → `dao().deleteEntity()` 逻辑删除） |
| 新建招聘入口 | `ErpHrRecruitmentBizModel.defaultPrepareSave:57-66` | 标准 CRUD save → status==null→`setStatus(OPEN):64`（重新申请新 ErpHrRecruitment 可达） |

---

## 2. Phase 1 — 运行时证据采集

### 2.1 A4.2.14 评估聚合权重 config 部署普查（§7-3）

**普查方法**：`grep -rn "assessment-self-weight|assessment-manager-weight|assessment-peer-weight|assessment-subordinate-weight"` 跨全 20 生产 application.yaml + README/seed/部署运维文档（排除 `/target/`）。

**普查结果**：**零命中**（exit code 1）。全 20 生产 application.yaml 无任何站点设评估权重 override。

**config getter 默认值 census**：

| 权重 | config key（`ErpHrConstants`） | 默认常量（`ErpHrConfigs`） | getter | L1/L2 一致性 |
|------|-------------------------------|---------------------------|--------|-------------|
| SELF | `erp-hr.assessment-self-weight`（`:156`） | `DEFAULT_ASSESSMENT_SELF_WEIGHT=0.15`（`:33`） | `assessmentSelfWeight():129-133` | ✓ L1 `use-cases.md:143`「SELF 15%」+ L2 `competency-management.md:191` |
| MANAGER | `erp-hr.assessment-manager-weight`（`:158`） | `DEFAULT_ASSESSMENT_MANAGER_WEIGHT=0.50`（`:35`） | `assessmentManagerWeight():135-139` | ✓ L1「MANAGER 50%」+ L2 `:192` |
| PEER | `erp-hr.assessment-peer-weight`（`:160`） | `DEFAULT_ASSESSMENT_PEER_WEIGHT=0.25`（`:37`） | `assessmentPeerWeight():141-145` | ✓ L1「PEER 25%」+ L2 `:193` |
| SUBORDINATE | `erp-hr.assessment-subordinate-weight`（`:162`） | `DEFAULT_ASSESSMENT_SUBORDINATE_WEIGHT=0.10`（`:39`） | `assessmentSubordinateWeight():147-151` | ✓ L1「SUBORDINATE 10%」+ L2 `:194` |

**部署活跃性裁决**：**生产环境默认值 = 代码默认值 = L1 契约默认值**（三方逐字一致）。全仓无任何站点覆盖评估权重 → 权重运行时恒为 15%/50%/25%/10%。config-gate = 部署启用决策（站点可按需覆盖，如基层员工无下级场景下调整 SUBORDINATE 权重），非契约缺失。L1 措辞「默认」明示可配置性，config-gate 完全满足 L1 语义。

**结论**：**维持 A1.12 §5 UC-HR-12 接受**（§2 接受判据满足：config 驱动非硬编码 + 默认值与 L1 一致 + L4 `TestAssessmentAggregator` 强测覆盖加权/重归一化/clamp）。登记 config-gate watch-only residual（部署启用决策，记本报告 §0/§5 非 arm-index finding 行，对齐 A4.2.12 范式）。

### 2.2 A4.2.15 handleContract 三态 config 部署普查（§7-4）

**普查方法**：`grep -rn "transfer-auto-handle-contract"` 跨全 20 生产 application.yaml + README/seed/部署运维文档。

**普查结果**：**零命中**（exit code 1）。全 20 生产 application.yaml 无任何站点设 `transfer-auto-handle-contract` override。

**config getter 默认值 + AUTO 分支可达性 census**：

| 项 | 证据 | 默认值/状态 |
|----|------|-----------|
| config 键 | `ErpHrConstants.java:202` `erp-hr.transfer-auto-handle-contract` | — |
| 默认常量 | `ErpHrConfigs.java:43` `DEFAULT_TRANSFER_AUTO_HANDLE_CONTRACT=true` | **true** |
| getter | `ErpHrConfigs.transferAutoHandleContract():159-163` `AppConfig.var(..., true)` | **true** |
| AUTO 分支 | `ErpHrEmployeeBizModel.resolveHandleContract:220-222` `else { shouldHandle = ErpHrConfigs.transferAutoHandleContract(); }` | AUTO（默认/空值/非 YES-NO）→ true → 处理合同 |
| 三态归一化 | `normalizeHandleContract:235-245` null/空/非 YES-NO → AUTO；YES/NO 显式 | AUTO 是默认态 |
| 处理副作用 | `:226-232` ACTIVE 合同→TERMINATED + `newContractFrom` 新建 ACTIVE | 调动时自动终止旧合同+建新合同 |

**部署活跃性裁决**：**生产环境默认值 = 代码默认值 = true**（主路径满足）。全仓无任何站点覆盖为 false → 调动时 `transferEmployee` AUTO 模式恒自动处理合同（终止旧+建新）。config-gate = 部署启用决策（站点可在调动不希望自动处理合同时设 false，此时须手工处理），非契约缺失。

**YES/NO 手工路径可达性**：YES（`handleContract=YES`）→ 始终处理（含无 ACTIVE 合同时仍新建，A1.12 §3 `TestErpHrEmployeeTransfer#testTransferContractYesCreatesEvenWithoutActive` 强测）；NO（`handleContract=NO`）→ 不触及合同（`TestErpHrEmployeeTransfer#testTransferContractNoDoesNotTouchContracts` 强测）。两路径经 `transferEmployee` 入参 `handleContract` 显式传入，运行时可达且文档引导齐全（L1 `use-cases.md:99` 注记 + L2 `state-machine.md`）。

**结论**：**维持 A1.12 §5 UC-HR-08 接受**（§2 接受判据满足：三态 AUTO/YES/NO 完整 + AUTO 默认 true 主路径满足 + config-gate 部署启用决策 + L4 9 方法强测）。登记 config-gate watch-only residual（部署启用决策）。

### 2.3 A4.2.16 未到岗回退逃生路径运行时可达性探查（§7-5）

#### 2.3.1 rollbackHire 缺失确认

**普查方法**：`grep -rni "rollbackHire|undoHire|revertHire|cancelHire|revokeHire"` 跨全 `module-hr`（`*.java` + `*.xbiz.xml`）。

**普查结果**：**零业务命中**（exit code 1）。CONFIRMED A1.12 §5 静态判定：`hire`→HIRED 终态后**无 rollbackHire mutation**。

**hire 终态性确认**（`ErpHrRecruitmentHireProcessor.hire:37-49`）：
- `:39` `requireStatus(rec, OFFERED, HIRED)`——仅 OFFERED 可 hire
- `:41` `rec.setStatus(RECRUITMENT_STATUS_HIRED)`——HIRED 终态
- `:43-47` 跨实体副作用：创建 Employee + Contract + employeeId 回写
- `reject:116-126` 守卫**拒 HIRED/CLOSED/REJECTED**（`:118-121`）——HIRED 后不可 reject，证实终态不可恢复

#### 2.3.2 逃生路径三步代码可达性

| 步骤 | mutation | file:line | 可达性 | 守卫 |
|------|----------|-----------|--------|------|
| ① close 行政关闭 | `ErpHrRecruitmentBizModel.close:130-135` | `setStatus(CLOSED)` + `updateEntity` | **可达**——`@BizMutation` 标准 mutation，HIRED→CLOSED 合法（无 status 守卫） | **无守卫**（P2-MA2-048 watch-only：任意态可 CLOSED） |
| ② useLogicalDelete 逻辑删除 | CrudBizModel 标准 `delete`（继承） | `dao().deleteEntity()` → `delVersion` 软删除 | **可达**——`ErpHrRecruitmentBizModel extends CrudBizModel`，标准 `delete(id)` mutation 可用；`app-erp-hr.orm.xml:793` `useLogicalDelete="true"` → 逻辑删除非物理删除 | 平台 `getDefaultRefNamesToCheckExists` 引用检查（标准） |
| ③ 重新申请新 ErpHrRecruitment | 标准 CRUD `save` | `defaultPrepareSave:57-66`→`setStatus(OPEN):64` | **可达**——标准 CRUD save mutation，HR 可为新候选人/同一候选人新建招聘记录 | 无（新建入口） |

**逃生路径代码可达性裁决**：三步**全部代码可达且可操作**。HR 处理「候选人接受 Offer 后未到岗」场景的运行时操作路径 = ① close 原 HIRED 记录（行政关闭，标记 CLOSED）+ ②（可选）逻辑删除原记录保留审计痕迹/或保留 CLOSED 记录作审计 + ③ 为重新招聘新建 ErpHrRecruitment 走完整 7 态流程。

#### 2.3.3 owner doc 覆盖面核验

`recruitment.md §十 关键业务规则 #3:396` 逐字：「**候选人状态不可逆：REJECTED 不可回退（误操作通过黑名单恢复模式处理）**」。

**覆盖面裁决**：owner doc **仅覆盖 REJECTED 重申**（经黑名单恢复模式），**未覆盖 HIRED 未到岗**场景的逃生路径文档引导。L1 `use-cases.md:60` ⑱「候选人接受 Offer 后未到岗需状态回退」要求显式状态回退，但实现采用 close+重开管理逃生路径（非显式 rollbackHire），且 owner doc 未显式标注该逃生路径适用于 HIRED 未到岗——**文档引导缺口**。

#### 2.3.4 逃生路径运营场景满足度评估

**满足维度**：
- 招聘记录可达终态清理（CLOSED）+ 审计痕迹保留（逻辑删除/_CLOSED 记录）+ 重新招聘入口可达——**招聘记录层面逃生路径满足**"重新发起招聘"运营需求。

**未满足维度（P2-RC-010 修复义务）**：
- **已创建的 Employee/Contract 成孤儿**：`hire:43-47` 在 HIRED 时已跨实体创建 ErpHrEmployee（ACTIVE）+ ErpHrEmploymentContract（ACTIVE）。close 仅设 recruitment.status=CLOSED，**不清理已创建的 Employee/Contract**——若候选人未到岗，已创建的 Employee（ACTIVE 状态）+ Contract（ACTIVE 状态）成孤儿活跃记录，需 HR 独立手工清理（员工离职/合同终止迁移——但 P1-MA2-039 确认员工 RESIGNED/TERMINATED writer 未实现，故清理路径本身受限）。
- **无显式 rollbackHire**：L1 ⑱要求「状态回退」，实现采用行政关闭（非状态回退语义）——形式偏离 L1 字面。
- **owner doc 未引导**：HIRED 未到岗无显式逃生路径文档（§7-5 关键发现）。

**结论**：**维持 P2-RC-010 watch-only 不撤销**（§2 P2① 边界场景弱：主路径 7 态完整 + 招聘记录层面逃生路径可达，但已创建 Employee/Contract 成孤儿 + 无显式 rollbackHire + owner doc 未引导 → 边界场景弱非完全缺失）。逃生路径代码可达性 CONFIRMED 但运营完整性部分满足（招聘记录层面满足，Employee/Contract 层面需独立清理）。修复义务归 MR1 R1.0 展开器（方案 A 实现 rollbackHire 清 Employee/Contract + 方案 B owner doc 标注逃生路径），本审计不实施（plan Non-Goals）。

---

## 3. Phase 1 Exit Criteria 复核

- [x] **验证报告落盘 `docs/audits/2026-08-07-0530-rc-ma4-a4-2-14-16-hr-employee-org-runtime-config-escape.md`，含三项存疑点各自裁决 + file:line 证据 + §2 判据命中分支**：本文件 §0/§1/§2 含三项裁决（UC-HR-12 接受 / UC-HR-08 接受 / UC-HR-05 P2 维持）+ 全 file:line 证据（§1.5 表 + §2.1-2.3 逐项）+ §2 判据命中分支（§5.1）。
- [x] **每项裁决明确：维持现有结论（接受/P2）或升级；config-gate watch-only residual（若有）已按 §去重协议裁决是否新建 arm-index 行**：三项均维持现有结论（§5.1）；config-gate watch-only residual 经 §5.2 裁决**不新建 arm-index 行**（与 A4.1.4/A4.2.12/P1-MA2-086 同范式 = 部署启用决策非契约缺失，对齐 §去重协议）；P2-RC-010 运行时现状确认注记追加（不撤销 watch-only）。

---

## 4. 多维度审计（`docs/skills/multi-dimensional-audit-prompt.md`）

按多维审计提示要求，对每个维度至少给出一句裁决：

| 维度 | 裁决 |
|------|------|
| **需求正确性** | L1 UC-HR-12:143「默认」明示权重可配置（config-gate 满足）；L1 UC-HR-08:99 注记明示三态 config-gated（满足）；L1 UC-HR-05:60 ⑱要求「状态回退」——实现采用 close+重开管理逃生路径（招聘记录层面可达），但无显式 rollbackHire（P2-RC-010 维持）。无「承诺但没有证据」项（三项 config/逃生路径均有 file:line 证据）。 |
| **owner-doc 对齐** | L2 `competency-management.md:164,191-195` 权重配置点表与 L1+L3 三方一致 ✓；L2 `recruitment.md:396 §关键业务规则 #3` 仅覆盖 REJECTED 重申**未覆盖 HIRED 未到岗**——文档引导缺口（P2-RC-010 方案 B 修复义务）。 |
| **架构或边界影响** | 无新跨模块依赖 / API 契约变更 / 保护区域触碰。本验证零代码变更，config-gate + 逃生路径均既有机制。 |
| **验证充分性** | config 覆盖假设可证伪：若有 application.yaml override，§2.1/2.2 grep 会命中（实测零命中）。逃生路径不可达假设可证伪：若 close 有守卫拒 HIRED 或 delete 不可用，A2.7a + P2-MA2-048 + 本验证 §2.3.2 会发现（实测三步可达）。 |
| **回归风险** | 本验证零代码变更（只读评估 + 文档更新），无脆弱路径引入。config-gate 默认值与 L1 一致是稳定基线。 |
| **路由和技能选择正确性** | 任务路由 = verification or audit work（只读评估），Skill = `multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。匹配。换路由无遗漏。 |
| **待办或自主权策略漂移** | 范围未无声扩大；三项既有裁决维持（UC-HR-12/UC-HR-08 接受不降级，UC-HR-05 P2 不撤销不升级）；config-gate watch-only residual 是验证**输出**非范围内项目降级（plan Deferred But Adjudicated 正确分类 rollbackHire 修复义务归 MR1）。 |
| **view.xml gen-control 契约**（项目特定维度） | 不适用——本验证对象是后端 config/逃生路径，不触及 delta view 前端层。本维度无发现。 |

**反窄化自检**：本验证覆盖 7 维度（需求/owner-doc/架构/验证充分性/回归风险/路由/待办漂移）+ 1 项目特定维度（view.xml，不适用），非单维深挖。每个维度已给出裁决。

---

## 5. Phase 2 — 三项存疑点裁决（§2 判据 + finding 衔接）

### 5.1 §2 判据复核（三项）

| 存疑点 | §2 接受 | §2 P0①④ | §2 P1①② | §2 P2① | 最终裁决 |
|--------|--------|---------|---------|--------|---------|
| A4.2.14（权重） | **✓**（config 驱动 + 默认值与 L1 一致 + L4 强测） | ✗（权重不破坏 GL/活跃数据） | ✗（功能完整非缺失/异常路径完整） | ✗（主路径完整非边界弱） | **维持 UC-HR-12 接受** |
| A4.2.15（合同三态） | **✓**（三态完整 + AUTO 默认 true 主路径满足 + L4 9 方法强测） | ✗（合同处理不破坏 GL/活跃数据） | ✗（功能完整非缺失/异常路径完整） | ✗（主路径完整非边界弱） | **维持 UC-HR-08 接受** |
| A4.2.16（未到岗） | ✗（⑭主路径 OK 但⑱异常路径弱） | ✗（不破坏 GL/活跃数据——Employee/Contract 孤儿需独立清理但不自动破坏） | ✗（逃生路径可达非完全缺失） | **✓**（⑱边界场景弱：主路径 7 态完整 + 招聘记录逃生路径可达，但 Employee/Contract 孤儿 + 无显式 rollbackHire + owner doc 未引导） | **维持 P2-RC-010 watch-only** |

**取最高原则**：A4.2.14/A4.2.15 仅 §2 接受成立 → 维持接受；A4.2.16 仅 §2 P2① 成立 → 维持 P2。

### 5.2 finding 衔接裁决（§7 复用 or 新增）

| 既有 arm-index 行 | 控制点 | 本验证关系 | 裁决 |
|-------------------|--------|-----------|------|
| `P2-RC-010`（UC-HR-05 ⑱未到岗回退，watch-only successor MR1，`:149`） | 未到岗回退异常路径 | 本验证 A4.2.16 CONFIRMED 逃生路径代码可达但 Employee/Contract 孤儿 + owner doc 未引导 → 运行时现状注记 | **不新建**——追加 A4.2.16 运行时现状确认注记于既有 P2-RC-010 行（不撤销 watch-only，修复义务仍归 MR1） |
| `P2-MA2-048`（招聘 close 无守卫，watch-only，`:741`） | close 守卫 | 本验证 A4.2.16 引用 close 无守卫作逃生路径可达性证据（HIRED→CLOSED 合法） | **不新建**——控制点不同（close 守卫 vs 未到岗回退），仅作逃生路径可达性证据引用 |
| `P1-MA2-039`（员工 RESIGNED 死状态，resolved R1.15，`:501`） | 员工离职/终止迁移 | 本验证 A4.2.16 发现 Employee/Contract 孤儿清理受限（无 RESIGNED writer）——继承 P1-MA2-039 successor | **不新建**——Employee/Contract 孤儿清理缺口同根因（无员工离职 writer），继承 P1-MA2-039 successor Deferred |
| config-gate 范式（A4.1.4/A4.2.12/A4.1.7 同型 = 部署启用决策非契约缺失） | config-gate default | 本验证 A4.2.14/A4.2.15 config-gate watch-only residual 同范式 | **不新建 arm-index 行**——对齐 A4.2.12 范式（config-gate watch-only residual 记报告 §0/§5 非 arm-index finding 行，因 §2 裁决为接受非 P2，且 config-gate 事实属部署启用决策） |
| A1.12 §5 UC-HR-12/UC-HR-08 接受（无独立 finding 行） | 需求契约 | 本验证维持该接受裁决 | **不新建**——接受类无 finding |

**新 finding 数 = 0**（对齐 A4.2.12/A4.2.13/A4.1.4 = 0 新 finding）。

### 5.3 不触发 MR0 / 不归 MR1（本审计）

- **不触发 MR0**：无 P0（§2 P0①④对三项均不成立）。
- **不归 MR1（本审计）**：A4.2.14/A4.2.15 无 P1 finding（config-gate 部署启用决策）；A4.2.16 维持 P2-RC-010 watch-only（修复义务归 MR1 R1.0 展开器——方案 A rollbackHire / 方案 B owner doc 标注，本审计不实施，plan Deferred But Adjudicated 正确分类）。

---

## 6. 文档更新（预授权）

### 6.1 本验证报告落盘

`docs/audits/2026-08-07-0530-rc-ma4-a4-2-14-16-hr-employee-org-runtime-config-escape.md`（本文件）。

### 6.2 P2-RC-010 arm-index 运行时现状注记追加（§去重协议）

`docs/audits/arm-index.md:149` P2-RC-010 行 `修复` 列追加 A4.2.16 运行时现状确认注记（逃生路径代码可达但 Employee/Contract 孤儿 + owner doc `recruitment.md §关键业务规则 #3` 仅覆盖 REJECTED 不覆盖 HIRED 未到岗 → 维持 watch-only 不撤销，修复义务仍归 MR1）。不新建 finding 行。

### 6.3 roadmap / log 同步

- `docs/backlog/requirement-compliance-roadmap.md` A4.2.14/A4.2.15/A4.2.16 `todo → done ✅`。
- `docs/logs/2026/08-07.md` 追加完成条目（裁决摘要 + 报告路径）。

---

## 7. 与 arm-index / 既有审计去重声明（§去重协议）

- **MA1 ↔ MA2 去重**：本验证复用 A2.7a（`2026-07-28-0230-arm-ma2-hr-employee-organization-state-machine.md`）状态机行为 pass 结论（招聘 7 态 + hire 跨实体副作用 + close 无守卫 + 合同三态 + 考核聚合）+ A1.12 §3/§5 实现证据，不重新核实行为本身（§去重协议 1-2）。
- **MA4 ↔ A5.6 边界**：本验证审「行为是否符合需求」（config 覆盖 + 逃生路径可达性），不重做 A5.6 E2E 断言强度审计（§去重协议 MA4↔A5.6）。
- **MA4 §7 族去重**：本验证与 A4.2.12（§7-1 cron 接线）+ A4.2.13（§7-2 多档预警）同 A1.12 §7 族，覆盖 §7-3/§7-4/§7-5 剩余三项，无范围重叠（每项独立存疑点）。
- **arm-index 交叉去重**：本报告 0 新 finding（§5.2），全部经 grep arm-index 同域同控制点后裁决（P2-RC-010 追加注记 / P2-MA2-048 证据引用 / P1-MA2-039 successor 继承 / config-gate 范式对齐），无未经比对直接新建的 finding。
- **config-gate 范式对比**：本验证 A4.2.14（权重 default-L1 一致）/ A4.2.15（合同处理 default-true 主路径满足）对齐 A4.1.4（budget default-off）+ A4.2.12（cron default-off 双层）+ A4.1.7（commitment default-off）——「config-gate = 部署启用决策非契约缺失，维持接受 0 finding」范式一致；A4.2.16（逃生路径可达性）属代码可达性探查但同收口 A1.12 §7 族。

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`。**本报告无生产代码变更**（纯审计报告 + arm-index 注记 + roadmap/log 同步，零 Java/ORM/契约变更），checker 无回归风险。actual vs baseline 汇总表如下。**不以 checker 脚本退出码 0 作为门控通过依据**（区分 reporter vs CI 门控）。

  | 规则 | baseline | actual（本次实测） | 变化 |
  |------|----------|-------------------|------|
  | R1a dao().saveEntity (BizModel) | 0 | 0 | — |
  | R1b dao().updateEntity (BizModel) | 0 | 0 | — |
  | R1d dao().findAllByQuery (BizModel) | 14 | 14 | — |
  | R2a BizModel daoFor(ErpMd*) | 34 | 34 | — |
  | R2b BizModel daoFor(Erp*) 跨域 | 229 | 229 | — |
  | R2c 全生产代码 daoFor() 总量 | 1382 | 1382 | — |
  | R3 new Erp*() 构造实体 | 5 | 5 | — |
  | R8 Processor 无 xbiz 接线 | 0 | 0 | — |

  本审计无生产代码变更，actual == baseline，无回归风险。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告 0 新 finding（§5.2），全部经 grep arm-index 同域同控制点后给出「复用 or 新增」裁决（P2-RC-010 追加注记 / P2-MA2-048 证据引用 / P1-MA2-039 successor 继承 / config-gate 范式对齐不新建行），无未经比对直接新建的 finding。

---

## 9. 结论

A1.12 §7-3/§7-4/§7-5 三个静态存疑点经全 20 生产 application.yaml config override 普查 + config getter 默认值 census + 逃生路径代码可达性分析：

- **§7-3（A4.2.14 评估聚合权重）**：四权重 config 驱动（`AssessmentAggregator:75-78`）+ 默认值 0.15/0.50/0.25/0.10（`ErpHrConfigs:33-39,129-151`）与 L1/L2 三方逐字一致 + 全 20 生产 application.yaml 零 override → **维持 UC-HR-12 接受**（config-gate = 部署启用决策，对齐 A4.1.4/A4.2.12 范式）。
- **§7-4（A4.2.15 handleContract 三态）**：三态 AUTO/YES/NO 完整（`resolveHandleContract:212-233`）+ AUTO 分支默认 true（`transferAutoHandleContract():159-163`）主路径满足 + 全 20 生产 application.yaml 零 false override → **维持 UC-HR-08 接受**。
- **§7-5（A4.2.16 未到岗回退）**：`hire`→HIRED 终态无 rollbackHire（grep 零业务命中）CONFIRMED P2-RC-010 静态判定；逃生路径三步代码可达（close 无守卫 + useLogicalDelete + 重开）但已创建 Employee/Contract 成孤儿 + owner doc `recruitment.md §关键业务规则 #3` 仅覆盖 REJECTED 不覆盖 HIRED 未到岗 → **维持 P2-RC-010 watch-only 不撤销**（修复义务归 MR1 R1.0 展开器）。
- **不触发 MR0，无新 finding，无 successor（本审计）**。三项 config-gate watch-only residual 记录于本报告 §0/§5（非 arm-index finding 行，对齐 A4.2.12 范式）；P2-RC-010 运行时现状注记追加于 arm-index 既有行。

§7-3/§7-4/§7-5 存疑点**闭合**。A1.12 §7 族五项存疑点（§7-1 A4.2.12 + §7-2 A4.2.13 + §7-3 A4.2.14 + §7-4 A4.2.15 + §7-5 A4.2.16）**全数收口**。
