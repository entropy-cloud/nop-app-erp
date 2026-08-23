# 2026-08-23 ct 两段式终止审批在 enforcement 下无可用账号（身份守卫 × FNPT 声明缺口死锁）

## 现象

`ct-contract-lifecycle.action.spec.ts` 两段式终止链（RC-R1.34，terminate → PENDING 法务审批记录 → approveTermination → TERMINATED）在 %test enforcement 栈（action-auth ON）下无任何 E2E 账号可走通：

- **admin（userId 1）**：`approveTermination` 被 `guardTerminationRecord` 身份守卫拒绝——`审批记录 {id} 审批人为 19，当前用户 1 无权操作`（ErpCtContractBizModel，守卫要求 caller == record.approverId）。
- **role-ct-approver（userId 19，即 resolveApproverId 解析的法定审批人）**：`approveTermination` 被 action-auth 拒绝——`nop.err.auth.no-permission`。`erp-ct.action-auth.xml` 的 FNPT 声明仅覆盖 activate/finalizeVersion/signVersion 等，**无 `FNPT:ErpCtContract:approveTermination`（及 rejectTermination/terminate）条目**，非 admin 账号对该自定义 mutation 无角色授权。

## 根因

RC-R1.34 两段式终止为 plan 2026-08-15 后新增流程，落地时补了身份守卫但未同步 R2.7 per-action FNPT 声明层 → 身份要求与授权矩阵互斥。该 spec 段落在 2026-08-23 flux 全量回归首次执行（08-11 基线后新增，E2E 空白期未验证）。

## 影响

- 两段式终止流程在 enforcement ON 下功能不可达（产品缺陷，非测试缺陷）。
- E2E `ct-contract-lifecycle` 2 用例红（happy path + illegal guards，两用例共用 terminateAndApprove）。

## 修复建议（successor）

产品侧（二选一，均出本计划范围）：
1. `erp-ct.action-auth.xml` 补 `FNPT:ErpCtContract:approveTermination`/`rejectTermination` `roles="合同审批人"` + role-resource 种子（R2.7 范式）；
2. 或 guardTerminationRecord 放宽为「admin 代理审批」白名单（不推荐，弱化法务门控语义）。

## 复现

fresh-DB + enforcement 栈：`npx playwright test tests/e2e/business-actions/ct-contract-lifecycle.action.spec.ts --workers=1`（确定性红）。

## 状态

open（登记日 2026-08-23，plan 2026-08-23-0434-2 Phase 2/3 裁决落盘；successor = 产品 FNPT 声明补齐）
