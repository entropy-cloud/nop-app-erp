# 2026-07-07 审计 1300 评分偏差的质量事件分析

> 关联计划：`docs/plans/2026-07-07-1915-1-audit-remediation-plan.md` H-3
> 触发审计：`docs/audits/2026-07-05-1300-code-vs-design-vs-best-practices-audit.md`
> 修正审计：`docs/audits/2026-07-07-1900-comprehensive-design-and-implementation-audit.md` §2.1

## 原始源输入

`docs/audits/2026-07-05-1300-code-vs-design-vs-best-practices-audit.md` 把"flow-overview.md 描述 REQUIRES_NEW 独立事务 vs data-dependency-matrix.md 描述 SYNC 默认"判定为"事务语义矛盾"，并建议修正 flow-overview.md 对齐到 SYNC。

## 缺少或误导的内容

- 1300 审计未核对实际代码（`ErpFinVoucherBizModel.post()` 上是否真的有 `@Transactional(REQUIRES_NEW)` 注解）
- 1300 审计未引用 `docs/analysis/2026-07-05-1300-platform-best-practices-extended-audit.md:231`（同日扩展审计已确认 REQUIRES_NEW 是有意架构决策）
- 1300 审计把"data-dependency-matrix.md §4.x 描述 SYNC"理解为"业务+库存+凭证三者同事务"，但实际语义是"同步调用 + REQUIRES_NEW 独立事务隔离"（文档表述歧义被放大为矛盾）

## 仅在实施过程中发现的内容

- 实际代码（`ErpFinVoucherBizModel.post()`）确实叠加 `@Transactional(REQUIRES_NEW)`，且所有 posting executor JavaDoc 都引用独立事务
- `flow-overview.md §6.1` 的描述与代码一致，无需修改；`data-dependency-matrix.md §4.1/§4.4` 的描述需要澄清为"同步调用 + REQUIRES_NEW"
- 这是审计 1300 的"假阳性"——基于文档字面差异推断出"矛盾"，未与代码交叉验证

## 下次应提前在流程中移动的内容

- "代码 vs 文档一致性审计"必须先读代码再读文档，不能仅基于文档对比就下结论
- 涉及"事务边界/并发/事务传播"等技术性强的判断，必须引用源代码或平台文档作为决定性证据
- 同日多份审计报告之间应互相引用（1300 主报告与 1300 扩展审计之间缺少交叉链接）

## 应创建哪些新技能、审计提示或工作流规则

- 审计提示：审计事务/并发/事务传播类问题时，必须包含至少一条源代码引用（class:method 或文件:行号）
- 审计提示：当多份审计报告同日产出时，主报告必须列出"扩展审计/补充审计"的交叉链接
- 技能候选：考虑创建 `transaction-semantics-audit.md` 技能模板，强制要求审计者验证实际事务注解

## 行动项（已落地）

- 已修正 `docs/audits/2026-07-07-1900-comprehensive-design-and-implementation-audit.md §2.1`，标注 1300 审计原判断为误判
- 已修正 `docs/architecture/data-dependency-matrix.md §4.1/§4.4`，澄清 REQUIRES_NEW 语义
- 本回顾作为后续审计质量改进的参考
