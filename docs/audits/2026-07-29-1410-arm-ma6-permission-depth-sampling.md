# ARM-MA6 权限深度抽样审计报告（A6.2）

> Audit Status: closed
> 里程碑：MA6（安全与权限层审计）
> 维度：权限深度抽样（finance / manufacturing / purchase / sales 4 个 S/A 级域）
> Skill：`multi-dimensional-audit-prompt.md`（跨多维度挑战——职责分离/高危守卫/状态机角色一致性/被动防护风险）
> Owner Docs：`docs/design/roles-and-permissions.md`（高危操作表 + 职责分离）+ 各域 `docs/design/<domain>/state-machine.md`（迁移执行角色）
> Plan：`docs/plans/2026-07-29-1410-1-ma6-permission-and-data-auth-audit.md`（Phase 2）
> 审计日期：2026-07-29
> 审计锚点：HEAD 0e963531d（M0 锚点）

## 1. 审计对象与范围

按 `multi-dimensional-audit-prompt.md` 对 finance / manufacturing / purchase / sales 4 个 S/A 级域做**权限深度抽样**——职责分离（创建人≠审核人）程序级可强制 vs 仅角色配置隐含 + 高危操作（反审核/作废/反结账/处置/红冲）程序级守卫 + 状态机迁移执行角色与代码一致性 + 「高危操作仅靠状态不可逆被动防护」的风险面。

**审计对象是 4 域权限落地的多维证据**。输入包括 owner doc（高危操作表 + 职责分离建议 + 角色→权限点映射）、4 域 BizModel/Processor Java 源码、各域 state-machine.md、既有审计结论（P1-MA3-046 / P1-MA2-020/021 等）。

## 2. 维度裁决

### 维度 1：职责分离（创建人≠审核人）程序级可强制性

owner doc `roles-and-permissions.md §职责分离（建议配置）` 声明「单据创建人与审核人不可为同一人」+ §审核与管理角色「审核人……与单据创建人职责分离」。

**核实**：4 域 submit→approve 路径抽样——

| 域 | approve 路径 | createdBy 比对 | 裁决 |
|----|------------|--------------|------|
| finance | `ErpFinVoucherBizModel.postVoucher` + 各 Processor `doApprove` | grep `getCreatedBy\|createdBy.*userId\|sameAsCreator` 全域 = **0** | **无程序级 SoD 守卫** |
| manufacturing | `ErpMfgWorkOrderProcessor` approve 链 | 同上 = 0 | **无程序级 SoD 守卫** |
| purchase | `ErpPurOrderProcessor.doApprove:335-337` `order.setApproveStatus(APPROVED) + setApprovedBy(currentUserId())` | **无 `createdBy` 比对**（`currentUserId()` 仅写入 approvedBy，不校验 ≠ createdBy） | **无程序级 SoD 守卫** |
| sales | `ErpSalOrderProcessor` approve 链 | 同上 = 0 | **无程序级 SoD 守卫** |

**VERDICT: FAIL → P1-MA6-001**。全域 4 域 approve 路径**零** `createdBy`/`getCreatedBy` 比对——创建人可自审。owner doc 标注「建议配置」（软门控），但代码层**完全不提供**程序级强制入口（无 `assertApproverNotCreator` 守卫方法、无 nop-wf step-owner≠submitter 配置实证）。即使 P1-MA3-046 修复（仅审核人角色可调 approve），同一人若同时持有「创建角色 + 审核角色」仍可自审——SoD 是**独立于 action-level RBAC 的工作流级不变量**，须单独守卫。详见 §3 P1-MA6-001。

### 维度 2：高危操作程序级守卫抽样

owner doc §高危操作权限 表列出 10 类高危操作「需管理员权限或额外审批」。逐项核实是否有 BizModel/Processor 内**显式角色/状态守卫**：

| 域 | 高危操作 | 方法 | 守卫类型 | 裁决 |
|----|---------|------|---------|------|
| finance | 反结账 | `ErpFinAccountingPeriodProcessor.reverseClose:274-307` | **config kill-switch**（`isReverseCloseApprovalRequired()→throw`）+ 状态守卫（须 CLOSED_FINAL）；**无角色断言、无审批流** | **被动 + kill-switch**（与 P1-MA2-020 同——kill-switch 非审批流） |
| finance | 红冲凭证 | `ErpFinVoucherBizModel.reverseVoucher:104-114` | **仅状态守卫**（须 POSTED）；**无角色断言、无期间 CLOSED_FINAL 守卫** | **被动防护**（与 P1-MA2-021 同——CLOSED_FINAL 锁定未实现） |
| finance | 过账凭证 | `ErpFinVoucherBizModel.postVoucher:88-100` | **仅状态守卫**（须 DRAFT）；**无角色断言、无期间状态守卫** | **被动防护**（P1-MA2-021） |
| finance | 期末批量折旧 | `ErpAstDepreciationScheduleProcessor.executeBatchDepreciation` | **仅业务校验**（IN_SERVICE 筛选）；**无角色断言、无二次确认** | **被动防护** |
| manufacturing | 工单关闭（部分完工） | `ErpMfgWorkOrderProcessor.close:141-154` | **仅状态守卫**（须 STOPPED/IN_PROCESS）；**无角色断言** | **被动防护** |
| manufacturing | 强制部分齐套开工 | `ErpMfgWorkOrderProcessor.start` | **仅业务校验**（齐套检查 config-gated）；**无角色断言** | **被动防护** |
| purchase | 反审核 | `ErpPurOrderProcessor.doReverseApprove:347-352` | **仅 approveStatus 守卫**（须 APPROVED）+ 红冲闭环；**无角色断言** | **被动防护** |
| sales | 反审核/作废 | `ErpSalOrderProcessor` reverseApprove/cancel | **仅状态守卫**；**无角色断言** | **被动防护** |

**VERDICT: 确认 P1-MA3-046 范围（无新 finding）**。4 域高危操作**全部**仅靠状态机不可逆性「被动」防护 + 个别 config kill-switch（反结账），**零**显式角色/权限程序级守卫（grep `isAdmin\|hasRole\|checkRole\|requireRole\|currentUserRole` 4 域 = 0）。此结论与 P1-MA3-046「全域敏感动作零运行时权限保护」完全一致——P1-MA3-046 已登记此缺陷为 P1（action-level 注解 + enforcement 全缺失），本维度复核确认无升级、无新独立 finding。

### 维度 3：状态机迁移执行角色与代码一致性

owner doc 各域 `state-machine.md` 声明迁移执行角色（如「生产主管：工单审核/开工/停工/关闭」「管理员：反结账」），`roles-and-permissions.md` 声明「角色名与状态机迁移执行角色同源」。

**核实**：4 域 BizModel/Processor 方法**零**角色断言（维度 2 已确认 `isAdmin`/`hasRole`/`requireRole` = 0）。状态机文档声明的迁移角色在代码层**完全不落地**——角色名仅作为 owner doc 文字描述 + 粗粒度 FNPT `mutation` 点的隐含归属，运行时无任何代码路径校验「执行者是否持有声明角色」。

**VERDICT: 确认 P1-MA3-046 范围（无新 finding）**。state-machine.md 迁移角色 vs 代码零角色断言的差距，根因同 P1-MA3-046（action-level 权限基础设施全缺失）——角色名是「文档层归属」，运行时无 enforcement。不分裂独立 finding。

### 维度 4：「高危操作仅靠状态不可逆被动防护」的风险面

评估被动防护的可绕过路径：

- **状态不可逆提供主路径安全**：反审核后 approveStatus=REJECTED（PROC 路径）+ 红冲凭证 posted=false——主终态持有，不可重复 approve（须重新 submit）。被动防护对「正常状态机流转」有效。
- **可绕过路径**：(a) INLINE reject/withdrawApproval 路径缺 isCancelled 守卫（P1-MA2-050/057——CANCELLED 单据 approveStatus 副轴漂移，危害有限）；(b) reverseClose kill-switch 置 false 时无条件放行（P1-MA2-020——无审批流）；(c) CLOSED_FINAL 凭证锁定未实现（P1-MA2-021——可修改/红冲已结账期间凭证）。三者均**已登记为独立 P1**（MR1），本审计不重复登记。
- **SoD 缺口是新增绕过面**：自审（创建人=审核人）不受状态机约束——approve 状态迁移合法，但破坏职责分离不变量。此为维度 1 的新 finding P1-MA6-001。

**VERDICT: 被动防护主路径安全，已知绕过面均已登记（P1-MA2-020/021/050/057）；SoD 缺口为新 finding P1-MA6-001**。

### 维度 5：owner-doc 对齐

owner doc §职责分离 标注「（建议配置）」——owner doc 已软化此要求为「建议」而非「硬约束」。但代码层连「可配置」入口都不提供（无守卫方法、无 config 开关、无 nop-wf SoD 配置实证）——**声明「建议配置」与「零程序级入口」之间存在落地差距**。owner doc §高危操作 表未标注「当前仅状态机被动防护，无角色守卫」——声明 vs 实现的差距未在 owner doc 显式标注。

### 维度 6：架构边界 / 回归风险

SoD 守卫若在 BizModel/Processor 层添加（`assertApproverNotCreator(entity, context)`），属单域内变更，无跨模块依赖、无契约变更、不触及保护区域（仅加只读校验 + 抛 NopException）。回归风险低——新增守卫仅在 approve 路径加一个前置断言，不影响主路径状态迁移。

## 3. 新增 P1 清单

### P1-MA6-001 职责分离（创建人≠审核人）程序级零强制（4 域 approve 路径）

- **域**：finance + manufacturing + purchase + sales（4 S/A 级域）
- **根因**：owner doc `roles-and-permissions.md §职责分离` 声明「单据创建人与审核人不可为同一人」，但 4 域 approve 路径（`ErpPurOrderProcessor.doApprove:335-337` `setApprovedBy(currentUserId())` 无 `createdBy` 比对 / finance+mfg+sal 同型）**零** `getCreatedBy`/`createdBy` 比对——创建人可自审。即使 P1-MA3-046 修复（仅审核人角色可调 approve），同一人持「创建角色+审核角色」仍可自审——SoD 是独立于 action-level RBAC 的工作流级不变量。
- **影响**：自审破坏 ERP 职责分离核心控制目标（owner doc §职责分离）；单用户/admin 种子下风险不显现，多角色用户部署即触发。
- **P1 非 P0**：(1) owner doc 标注「建议配置」（软门控）；(2) 单组织种子 + admin skip-check 下无活跃数据破坏；(3) SoD 亦可在 nop-wf 审批流层配置（step-owner≠submitter），属补能力非活跃缺陷。
- **修复方式**：MR3 裁决——方案 A（推荐）在 4 域大 Processor `doApprove` 前置 `assertApproverNotCreator(entity, context)`（比对 `entity.createdBy != context.userId`，相等抛 `NopException(ERR_*_APPROVER_IS_CREATOR)`）+ owner doc §职责分离 标注「程序级强制已落地」；方案 B 经 nop-wf 审批流配置 step-owner≠submitter（依赖审批流启用，非直接 approve 路径）；方案 C owner doc 标注「SoD 为运营配置约定，程序级 successor」。
- **目标 MR**：MR3。

## 4. 与 P1-MA3-046 / MA2 协同去重

- **P1-MA6-001 ≠ P1-MA3-046**：P1-MA3-046 是 action-level 权限注解 + enforcement 全缺失（谁可调 approve）；P1-MA6-001 是工作流级 SoD 不变量（approver ≠ creator）——不同控制层。即使 P1-MA3-046 修复，P1-MA6-001 仍独立存在。
- **维度 2/3/4（被动防护/角色一致性/绕过面）→ 确认 P1-MA3-046 范围**：4 域高危操作零角色断言 + state-machine 角色不落地，根因同 P1-MA3-046，不分裂独立 finding。
- **维度 4 已知绕过面 → 已登记 MA2 P1**：reverseClose kill-switch（P1-MA2-020）/ CLOSED_FINAL 锁定（P1-MA2-021）/ INLINE 守卫缺失（P1-MA2-050/057）均已独立登记 MR1，本审计复核无升级。

## 5. Verdict

**passes multi-dimensional audit（⚠️ P1，零 P0）**。4 域权限深度抽样产出 **1 项新 P1**（P1-MA6-001 职责分离程序级零强制）+ 复核确认 P1-MA3-046（高危操作零角色守卫）+ P1-MA2-020/021/050/057（已知绕过面）范围一致无升级。SoD 缺口是 4 域抽样唯一独立于既有 finding 的新发现。

## 6. 剩余未知数（watch-only）

1. **nop-wf 审批流 SoD 配置能力未实证**：owner doc §审批与审计要求 声明「单据审核支持配置审批流（nop-wf）」，SoD 理论上可在审批流层配置，但本审计未实证 nop-wf 是否提供 step-owner≠submitter 原生支持——若 nop-wf 已支持，P1-MA6-001 修复方式 B 更轻量。MR3 裁决时须先核实 nop-wf 能力。
2. **SoD 仅抽样 4 S/A 域**：扩展域（crm/cs/hr/...）approve 路径未抽样——按同型根因推测一致（零 `createdBy` 比对），但未实证。MR3 修复时全域铺开。

## 7. audit 关闭条件

本报告产出 + arm-index §P1 详细清单登记 P1-MA6-001 + arm-index §报告清单登记本报告 + roadmap A6.2 推进 `todo → ready`。
