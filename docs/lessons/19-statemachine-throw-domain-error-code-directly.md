# Lesson 19: 组件直接抛领域错误码——"先抛通用码再外层转码"是被迫转码反模式

> **来源**：2026-09-07 用户裁定点评"为什么底层不直接把错误码搞好，而是先抛通用码再捕获转码，多此一举"。全仓核实坐实：约 90 个 StateMachine（14+ 模块）经私有 `illegal()` helper 抛 common 码 `nop.err.erp.common.illegal-status-transition`，转码层分布在 hr（Guard）、sal/pur/inv（Processor try/catch）等处，其余实体（如 ErpAstMovement）common 码裸奔。
>
> **适用场景**：任何"共享/底层组件抛通用错误码 + 外层（Guard/Processor/xbiz）捕获后转码为领域码"的设计时刻；新建 StateMachine/领域组件定义错误码契约的时刻。
>
> **失败模式**：底层组件图"统一分类/复用"抛通用码 → 通用码全仓无通用消费方（唯一消费方是转码层自己）→ 每个实体多出一层纯 boilerplate 转码；没建转码层的实体则通用码裸奔，跨域错误码风格不一致。

## 核心论点

**错误码契约应该在"拥有领域语境的组件"里一步到位，而不是底层抛通用码、外层转码。**

判据（三条全占即反模式）：

1. **通用码没有通用消费方**：除转码层自己，没有任何前端/拦截器/wf 引擎按它做机器分类——它是纯中间产物，转码是纯 boilerplate。
2. **"底层"其实是每实体独立的类**：StateMachine 无共享基类，`illegal()` 是各类私有 helper——"统一声明"的复用论据不成立，完全可以直接抛各域码。
3. **底层 API 拿不到实体上下文**：assert 只收 status 字符串，抛领域码也填不了 entity id——转码层的存在本质是在补"Bean API 不带实体"这个自己挖的洞。

正确形态：

```
StateMachine.illegal() 直接构造领域错误码（各域 ErpXxErrors 已有
erp.err.<domain>.illegal-status-transition 或实体专属码），
params 保持 action/currentStatus/expectedStatus；
→ 外层 Guard/Processor 的 try/catch 转码整层退役（直接调 assert）；
→ xbiz 直接调 assert（XScript try/catch 支持后如需内联增强也无需中转层）。
```

**什么时候允许 common 码**：真正跨域、与实体无关且有直接消费方的通用机制（如 `AbstractProcessor` 兜底 `ERR_ENTITY_NOT_FOUND`，由框架层统一渲染）——判据是"存在与实体无关的调用方直接按该码分支"。

## 真实案例

| 案例 | 形态 |
|---|---|
| hr 薪酬（M4.64/RC-R1.89） | `ErpHrSalaryApprovalStateMachine` 抛 common 码 → `ErpHrSalaryApprovalGuard.map()` try/catch 转码 `ERR_SALARY_ILLEGAL_STATUS_TRANSITION` + salaryId/currentStatus/expectedStatus——转码层是纯 boilerplate + 元数据增强 |
| assets Movement（plan 2026-08-13） | StateMachine javadoc 原意"领域码映射归 xbiz source（契约 §7）"，当年 XScript 无 try/catch 断链 → common 码裸奔至今，跨域错误码风格不一致 |
| sal/pur/inv Processor 族 | 每实体 5+ 个 Processor 各写一遍 catch-and-remap，域通用码 `erp.err.{sal,pur,inv}.illegal-status-transition` 其实早已存在——Bean 直接抛它即可删掉全部映射 |

## 自检清单（定义组件错误码契约时）

- [ ] 这个"通用码"有没有**与实体无关的直接消费方**？没有 → 不要造通用码。
- [ ] 抛码的组件是否每实体一个独立类？是 → 直接抛各域码，不要私有 helper 统一抛 common。
- [ ] 是否正在写"catch 通用码 → throw 领域码"的转码层？是 → 停下来，把底层改成直接抛领域码，删掉这层。
- [ ] 领域码是否已存在（`erp.err.<domain>.illegal-status-transition` / 实体专属码）？优先复用，端到端错误码值不变。

## 何时复发

- 新建共享组件/基类时顺手定义"通用错误码"。
- 以"以后前端好统一处理"为由抛通用码（真要做统一处理时按 params 约定或再加分类码，YAGNI）。
- 从旧模式迁移时为"保持行为一致"保留转码层（端到端码值不变的迁移不需要转码层，见 plan 2026-09-07-2200-1）。

## 关联

- 全局修复计划：`docs/plans/2026-09-07-2200-1-statemachine-domain-error-code-direct-throw.md`（~90 StateMachine 直抛领域码 + 转码层退役）
- 契约同步：`docs/design/assets/state-machine.md` §错误码迁移说明（2026-09-07 勘误后随本计划再收敛）
- 历史脉络：lesson 15（xbiz XScript try/catch 勘误——转码断链的技术根源）+ `nop.err.erp.common.illegal-status-transition`（plan 2026-07-24-2200-1 引入）
