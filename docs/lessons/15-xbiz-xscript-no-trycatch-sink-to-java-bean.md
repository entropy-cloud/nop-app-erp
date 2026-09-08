# Lesson 15: xbiz XScript 编排下沉 Java Bean（try/catch 已支持——勘误版）

> **来源**：2026-08 hr 域两案。M4.64（2026-08-14，plan `2026-08-14-0456-2`，落地 36 个手写 `ErpHr*.xbiz` delta）首立 Guard/StateMachine Bean 范式；RC-R1.89（2026-08-20，plan `2026-08-20-0518-3`）D2 裁决——过账编排（含失败隔离 try/catch）落 `ErpHrSalaryPostApprovalProcessor` Java Bean，xbiz `approve` mutation 状态写回后仅一行 inject 委托调用。
>
> **⚠️ 勘误（2026-09-07）**：本课原述「XLang 引擎不支持 XScript 的 `TryStatement`，无法 try/catch」——该结论在 2026-08 时成立（`TryStatement` 无 AST→Executable 构造点，`BuildExecutableProcessor` 抛 `not-supported-node`），但 **nop-entropy 已随 plan 2258 落地 XScript try/catch/finally 支持（master `c6d8f66a12`，JS 语义：catch 绑定异常、catch 体正常完成即吞异常、显式 `throw` 重抛、finally 必执行）**。因此「无法 try/catch」不再是平台能力边界；**本课保留的结论是架构偏好而非能力限制**：多步编排 + 各自失败隔离、跨实体事务组合、幂等防御仍建议下沉 Java Bean（事务边界、类型安全、可测试性）。

## 适用场景

任何要在 `*.xbiz.xml` 手写 action source 中编写**多步编排、异常隔离（try/catch）、事务边界控制、复杂守卫链**的时刻。

## 核心论点（勘误后）

xbiz 的 XScript source 层与 Java Bean 层的能力分工：

| 层 | 适合 | 不适合 |
|---|---|---|
| **xbiz XScript source** | 薄委托：状态字段写回（`approveStatus=APPROVED` + 审计字段）+ 一行 inject 调用 Java Bean；简单 try/catch（单步失败隔离 + 告警） | 跨实体事务组合、多路过账编排 + 各路独立失败隔离（非 short-circuit 聚合）、复杂守卫链 |
| **Java Bean**（Guard / StateMachine / 编排 Processor） | 守卫链（`assertCan<X>`）、状态迁移断言、多路过账编排 + 各路独立失败隔离、幂等防御、事务边界 | —— |

**为什么仍下沉（架构偏好，非能力边界）**：RC-R1.89 的 270→290→300 三路计提各自 G3 失败隔离（"任何一步失败不得中断其余步骤 / 失败要告警但主流程继续"）是典型的**编排 + 事务 + 幂等**组合——XScript 的 try/catch 能表达单点隔离，但把三路计提 + 各自独立回滚 + 幂等 writer + posted 收敛全塞进脚本既难测试也难维护。Java Bean 提供事务边界、类型安全、单元可测与幂等防御的成熟载体。

## 下沉后的标准形态（RC-R1.89 D2 定稿，可作为模板）

```
xbiz approve mutation（source 层）:
  1. 守卫：ErpXxApprovalGuard.assertCanApprove(...)
  2. 状态写回：approveStatus=APPROVED / approvedBy / approvedAt
  3. 一行委托：inject('xxBean').postApprove(entity, svcCtx)

Java 编排 Bean（app-service.beans.xml 注册）:
  - 多步编排 + 各步 try/catch 失败隔离 + 告警派发 + 幂等防御 + 终态回写
```

附带收益：wf 工作流回调与直批路径若覆写**同一 action**（`TestErpHrSalaryWorkflowApproval` 证实 wf 结束经 listener 回调同一 `approve`），单一委托点即可双路径覆盖，无需二次接线。

## 失败模式（典型路径）

```
1. 需要"审批后触发多路过账、各自失败隔离"
2. XScript 现可写 try/catch（plan 2258 后），但把三路计提 + 独立回滚 + 幂等 + posted 收敛全塞进脚本
   → 事务边界、可测试性、幂等防御俱差（历史版本则直接编不过）
3. 绕路 A：脚本内顺序调用三个 action，无 try/catch
   → 首路失败中断全部（过严）或异常上抛回滚全部（无隔离）→ 部分悬挂或全量失败
4. 绕路 B：改用 .xwf listener 回调
   → 时点耦合 wf 回调链；Debug/直批路径绕过 wf → 漏触发（RC-R1.89 D2 否决理由 c）
5. 正解：编排下沉 Java Bean（守卫/状态机 Bean 同范式），xbiz 只做薄委托
```

## 真实案例

| 案例 | 形态 |
|---|---|
| M4.64（2026-08-14） | 36 个手写 `ErpHr*.xbiz` delta 全族采用 Guard/StateMachine Bean 范式；`ErpHrSalary.xbiz:12-17` 机制注记首立「编排下沉 Java Bean」 |
| RC-R1.89 D2（2026-08-20，plan `2026-08-20-0518-3`） | 薪酬 approve 过账编排：xbiz `approve` source 状态写回后委托 `ErpHrSalaryPostApprovalProcessor.postAccruals`（270→290→300 顺序 + 非 short-circuit `&` 聚合保三路各自失败隔离 + posted writer + 幂等防御）；否决 .xwf listener（直批绕过风险）；beans.xml 注册；wf 回调与直批共用单一接线点 |

## 自检清单（写 xbiz action source 时）

- [ ] 跨实体事务组合 / 多路编排 + 各自失败隔离 / 幂等防御？→ 下沉 Java Bean（编排 Processor），xbiz 只留薄委托。
- [ ] 只是单点 try/catch（如记录 + 告警后继续）？→ XScript 可表达（plan 2258 后），按 `09` 反模式要求不得静默吞。
- [ ] 守卫链是否已在 Guard/StateMachine Bean（`assertCan<X>` 范式）而非脚本内联？
- [ ] 编排 Bean 是否注册 `app-service.beans.xml`？xbiz 委托是否经 `inject(...)`？
- [ ] 触发点是否有 wf/直批双路径？是否覆写同一 action 使单一委托点全覆盖（先例 `TestErpHrSalaryWorkflowApproval` 证法）？

## 何时复发

- 从 Java BizModel 迁移逻辑到 xbiz delta（Model→Delta 决策链）时，把 Java 侧重编排原样搬进 XScript（XScript 现可写 try/catch，但复杂编排仍应留 Java）。
- 新写"审批触发多路过账/多路通知"类编排时，把三路计提 + 独立回滚 + 幂等全塞进脚本。
- 用 .xwf listener 兼做业务编排触发点（直批路径绕过风险）。

## 关联

- 机制注记原文：`module-hr/erp-hr-service/.../ErpHrSalary.xbiz`（:12-17 机制注记；2026-09-07 已勘误）
- 案例计划：M4.64 手写 xbiz delta 族（plan `2026-08-14-0456-2`）+ RC-R1.89（`docs/plans/2026-08-20-0518-3-rc-mr1-r1-89-hr-payroll-posting-wiring.md` D2 裁决 + 否决替代记录）
- 平台：nop-entropy plan 2258（XScript try/catch/finally 支持，master `c6d8f66a12`）；平台文档 `../nop-entropy/docs-for-ai/02-core-guides/xlang-and-xpl-basics.md`「XScript 的 try/catch/finally」
- 全局直抛迁移：`docs/plans/2026-09-07-2200-1-statemachine-domain-error-code-direct-throw.md`（2026-09-08 落地——StateMachine 直抛领域码后，历史上因本课「转码断链」被迫落在 Java Guard/Processor 的 catch-and-remap 转码层全仓退役为同码补参，见 lesson 19）
- 划界：与 lesson 06（codegen 产物编辑必被覆盖）同属平台机制课但对象不同；与 lesson 09（吞异常悬挂）衔接——无论 XScript 还是 Java，失败都要显式隔离 + 告警，不得复制吞异常反模式
