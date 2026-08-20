# Lesson 15: xbiz XScript 无法 try/catch——编排逻辑必须下沉 Java Bean

> **来源**：2026-08 hr 域两案。M4.64（2026-08-14，plan `2026-08-14-0456-2`，落地 36 个手写 `ErpHr*.xbiz` delta）在 xbiz 机制注记（`ErpHrSalary.xbiz` 审批轴 source 注释）首立：**XScript try/catch 在 XLang 引擎不可行，逻辑须下沉 Java Bean**（Guard/StateMachine Bean 范式）；RC-R1.89（2026-08-20，plan `2026-08-20-0518-3`）D2 裁决再次消费该机制约束——过账编排（含失败隔离 try/catch）落 `ErpHrSalaryPostApprovalProcessor` Java Bean，xbiz `approve` mutation 状态写回后仅一行 inject 委托调用。
> **适用场景**：任何要在 `*.xbiz.xml` 手写 action source 中编写**多步编排、异常隔离（try/catch）、事务边界控制、复杂守卫链**的时刻。
> **失败模式**：在 xbiz XScript source 里直接写 try/catch 或多步编排——`TryStatement` 不被 XLang 编译支持，**写不出来/编不过**；绕路写法（脚本内多 action 顺序调用无失败隔离）导致部分失败悬挂（吞异常反面，见 lesson 09）或事务语义失控。

## 核心论点

xbiz 的 XScript source 层与 Java Bean 层有明确的能力分工：

| 层 | 适合 | 不适合 |
|---|---|---|
| **xbiz XScript source** | 薄委托：状态字段写回（`approveStatus=APPROVED` + 审计字段）+ 一行 inject 调用 Java Bean | try/catch、多步失败隔离编排、跨实体事务组合、复杂守卫链 |
| **Java Bean**（Guard / StateMachine / 编排 Processor） | 守卫链（`assertCan<X>`）、状态迁移断言、多路过账编排 + 各路独立失败隔离（非 short-circuit 聚合）、幂等防御 | —— |

机制根源：XLang 编译器不支持 XScript 的 `TryStatement`。这不是编码风格偏好，是**平台能力边界**——凡是"任何一步失败不得中断其余步骤 / 失败要告警但主流程继续"的语义（RC-R1.89 的 270→290→300 三路计提各自 G3 失败隔离是典型），在 xbiz 层**无表达载体**，必须下沉。

下沉后的标准形态（RC-R1.89 D2 定稿，可作为模板）：

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
2. 尝试在 xbiz XScript source 写 try { ... } catch { ... }
   → TryStatement 不被 XLang 支持，写不出/编不过
3. 绕路 A：脚本内顺序调用三个 action，无 try/catch
   → 首路失败中断全部（过严）或异常上抛回滚全部（无隔离）→ 部分悬挂或全量失败
4. 绕路 B：改用 .xwf listener 回调
   → 时点耦合 wf 回调链；Debug/直批路径绕过 wf → 漏触发（RC-R1.89 D2 否决理由 c）
5. 正解：编排下沉 Java Bean（守卫/状态机 Bean 同范式），xbiz 只做薄委托
```

## 真实案例

| 案例 | 形态 |
|---|---|
| M4.64（2026-08-14） | 36 个手写 `ErpHr*.xbiz` delta 全族采用 Guard/StateMachine Bean 范式；`ErpHrSalary.xbiz:12-17` 机制注记首立「XScript try/catch 不可行，逻辑须下沉 Java Bean」 |
| RC-R1.89 D2（2026-08-20，plan `2026-08-20-0518-3`） | 薪酬 approve 过账编排：xbiz `approve` source 状态写回后委托 `ErpHrSalaryPostApprovalProcessor.postAccruals`（270→290→300 顺序 + 非 short-circuit `&` 聚合保三路各自失败隔离 + posted writer + 幂等防御）；否决 XScript 内联（机制不可行）与 .xwf listener（直批绕过风险）；beans.xml 注册；wf 回调与直批共用单一接线点 |

## 自检清单（写 xbiz action source 时）

- [ ] source 里是否出现了（或想要写）try/catch？→ 立即下沉 Java Bean，xbiz 只留薄委托。
- [ ] 多步编排中是否存在"单步失败不应中断其余"的语义？→ 下沉 + 各步独立 try/catch（对齐 lesson 09：失败要显式告警回退，不得静默吞）。
- [ ] 守卫链是否已在 Guard/StateMachine Bean（`assertCan<X>` 范式）而非脚本内联？
- [ ] 编排 Bean 是否注册 `app-service.beans.xml`？xbiz 委托是否经 `inject(...)`？
- [ ] 触发点是否有 wf/直批双路径？是否覆写同一 action 使单一委托点全覆盖（先例 `TestErpHrSalaryWorkflowApproval` 证法）？

## 何时复发

- 从 Java BizModel 迁移逻辑到 xbiz delta（Model→Delta 决策链）时，把 Java 侧 try/catch 编排原样搬进 XScript。
- 新写"审批触发多路过账/多路通知"类编排时先写脚本再发现编不过。
- 用 .xwf listener 兼做业务编排触发点（直批路径绕过风险）。

## 关联

- 机制注记原文：`module-hr/erp-hr-service/.../ErpHrSalary.xbiz`（:12-17 机制注记；行号写时实测）
- 案例计划：M4.64 手写 xbiz delta 族（plan `2026-08-14-0456-2`）+ RC-R1.89（`docs/plans/2026-08-20-0518-3-rc-mr1-r1-89-hr-payroll-posting-wiring.md` D2 裁决 + 否决替代记录）
- 平台文档：`../nop-entropy/docs-for-ai/`（xbiz / XLang 相关章节）
- 划界：与 lesson 06（codegen 产物编辑必被覆盖）同属**平台机制边界**课但对象不同——06 是"生成产物不可手编"（改模型源或 Delta），本课是"XScript 表达能力边界"（编排下沉 Java）；与 lesson 09（吞异常悬挂）衔接——下沉后的 Java 编排必须做显式失败隔离 + 告警，不得复制吞异常反模式
