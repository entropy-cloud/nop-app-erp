# 2026-08-23 E3.1 金额掩码下 E2E 无可观察账号（FNPT/读授权缺口）

## 现象

E3.1 后端响应层脱敏（@BizLoader + MaskHelper，plan 2026-08-10-2059-2 / E4.2）生效后，薪酬（ErpHrSalary 13 DECIMAL 金额字段）与合同返利（ErpCtRebateAccrual.accruedRebate 等）的**明文金额对任意 E2E 账号均不可观察**：

- **admin/nop**：@BizLoader 掩码 → 金额字段返回 `null`（`Number(null)=0` 造成断言假失败）；
- **掩码白名单角色**（薪酬审批人 role-hr-salary / 合同专员 role-ct-clerk / 合同审批人 role-ct-approver）：对相关实体的 `__get`/`__findPage` **读路径无授权**（`nop.err.auth.no-permission`）——FNPT/资源声明层未授予这些角色对应查询权限；
- 相关自定义 mutation（`ErpHrSalary__calculateSalary`、`ErpHrSalarySimulation__adjustItem/createSimulation/convertToFormal`、`ErpCtRebateAgreement__runAccrual`）同样无 FNPT 角色授权 → 角色账号无法以「跑动作 + 读明文」组合观察金额。

## 根因

E3.1 掩码落地（08-10/08-11）时以 JUnit 为验证载体（in-process 无 enforcement），E2E 角色账号的授权矩阵（R2.7 FNPT 声明 + e2e-shared 账号种子）未同步覆盖「掩码白名单角色的读授权 + 自定义 mutation 授权」，形成掩码生效但无人可读的观察空洞。

## 影响

- E2E 层无法断言薪酬/返利金额数值（2026-08-23 flux 全量回归 4 spec 受影响：hr-payroll / hr-salary-simulation / ct-rebate-accrual /（hr-shift-rotation 关联 spec 已按产品契约对齐另行处理））。
- 已按 plan 2026-08-23-0434-2 裁决将 E2E 断言降为可观察面（状态机翻转 + 掩码 fail-closed 生效实证 `masked null`），**金额数值正确性由 JUnit 承载**（TestErpHrSalary* / TestErpHrSalarySimulation* / TestErpCtRebate* 全绿，08-23 基线 3808/0/0/1）。

## 修复建议（successor）

授权矩阵补齐（产品/种子层，二组）：
1. `erp-hr.action-auth.xml` / `erp-ct.action-auth.xml` 补掩码白名单角色的实体查询资源授权（或 SUBM 菜单资源 role-resource 种子覆盖 `__get/__findPage`）；
2. 补自定义 mutation FNPT（calculateSalary/adjustItem/runAccrual 等，roles=对应业务角色）——与 `2026-08-23-ct-terminate-approval-fnpt-deadlock.md` 同族（R2.7 声明层覆盖缺口）。

补齐后 E2E 可恢复「角色账号跑动作 + 读明文断言」全价值形态（spec 注释已留恢复锚点）。

## 状态

open（登记日 2026-08-23，plan 2026-08-23-0434-2 Phase 2/3 裁决落盘；successor = 授权矩阵补齐后恢复金额断言）
