# E2E ct-contract-lifecycle happy path「没有访问权限」预存失败（与 F2.15 修复批无关）

- **发现**：2026-09-12，plan `2026-09-12-0400-1` Phase 7 E2E 验证（contract/b2b/drp 三 spec）。
- **现象**：`tests/e2e/business-actions/ct-contract-lifecycle.action.spec.ts` happy path（save NEGOTIATION→activate→suspend→resume→terminate）稳定失败——`ErpCtContract__get` 返回 GraphQL errors `没有访问权限`；同文件 expire/amend/illegal-guards 三用例全绿。
- **归档对照实验（git stash -u 双向）**：基线 HEAD `22844cbc9`（无 F2.15 变更）失败形态**完全一致**——非本批引入。关联背景：permissions-enforcement E1.2 测试环境 action enforcement 开启后，本 spec 的 save 走表单提交（form owner 可能非查询会话用户），属权限矩阵/E2E 账号适配缺口而非本批回归。
- **影响面**：仅该 spec happy path 用例；module-contract 全部 JUnit 测试 142/0/0 全绿（业务行为无回归证据）。
- **处置**：登记 bug + 归 permissions-enforcement E2E 账号适配 successor；F2.15 的 E2E 门以 b2b-asn-match-receive + drp-plan-engine 全绿 + contract 同文件 3/4 用例绿（含全部状态机断言）承载。

## 验证证据

- 基线复现：`git stash -u` 后 HEAD=22844cbc9 跑同 spec → 同一用例同错误（1 failed / 3 passed）。
- 本批变更面（crm/ct/b2b/drp/aps/notify service 层）与 ErpCtContract 读取权限无交集。


## 附：QA 模块同型毫秒竞态发现（2026-09-12 收官验证）

- `TestErpQaSpcSamplingEvaluateBatch` SAMPLE_TIME 快照竞态（@var 绑定值与 CSV 行值跨秒边界 1ms 漂移），与 C16/C10 同型。修复 = 输出快照 SAMPLE_TIME 列 `*` 通配 + 三连跑 4/0/0 稳定。module-quality 本批（F2.15）零 Java 变更，纯快照掩码。
