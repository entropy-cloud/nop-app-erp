# Nop 合规性检查基线（Compliance Baseline）

> Owner: `docs/audits/nop-compliance-checker.sh`（checker）+ `.github/workflows/compliance.yml`（CI 回归门控）
> 基线落盘日期: 2026-07-24
> 基线来源: 计划 `docs/plans/2026-07-24-0930-1-compliance-guard-activation-ci-baseline.md` Phase 1 实测（`bash docs/audits/nop-compliance-checker.sh` 汇总表）

## 用途

本文件是 F8 治理 finding（`docs/audits/2026-07-23-0000-architecture-governance-review.md`）的**回归门控基线**：CI 每次运行 checker 后将汇总表命中数与本基线比对，**任何规则命中数超过基线即判 CI 失败**。

这激活了既有但此前未接入 CI 的 checker（dead armor → live guard，见治理审查 §Guards 槽与闭包前必须项 #4）。

## 基线表（16 行可计数规则）

| 规则 | 描述 | 严重度 | 基线命中 |
|------|------|--------|----------|
| R1a | dao().saveEntity (BizModel) | 🔴 高 | 0 |
| R1b | dao().updateEntity (BizModel) | 🔴 高 | 0 |
| R1c | dao().getEntityById (BizModel) | 🔴 高 | 0 |
| R1d | dao().findAllByQuery (BizModel) | 🔴 高 | 23 |
| R2a | BizModel daoFor(ErpMd*) | 🔴 高 | 37 |
| R2b | BizModel daoFor(Erp*) 跨域 | 🔴 高 | 319 |
| R2c | 全生产代码 daoFor() 总量 | 🔴 高 | 1108 |
| R2d | Processor daoFor(ErpMd*) | 🔴 高 | 34 |
| R3 | new Erp*() 构造实体 | 🟡 中 | 19 |
| R4 | extends RuntimeException | 🟢 低 | 0 |
| R5 | @Inject private | 🟡 中 | 0 |
| R6 | @Transactional in BizModel | 🟢 低 | 7 |
| R7 | System.currentTimeMillis() | 🟢 低 | 2 |
| R8 | Processor 无 xbiz 接线 | 🔴 高 | 42 |
| R10 | REQUIRES_NEW 事务 | 🟡 中 | 51 |
| R11 | Processor 重复状态判断方法 | 🟡 中 | 0 |

> R9（doReverseApprove 一致性）为**定性校验**（输出 ✓/✗ 清单，无数值计数），故不在上表参与数值门控；其输出仍由 checker 打印供人工查阅，CI 不对其做数值断言。

## R2c 增量注记

R2c=1108 较 `docs/plans/2026-07-16-2134-1-ddd-entity-methods-daofor-convergence.md` 历史快照（965）增长 **+143**，因后续 A2/A3/B1 等深化工作新增生产代码（含跨域写豁免登记的合法新增）。该 delta 已被本次基线吸收，后续以 1108 为回归门控起点。

## 回归门控规则

- **门控方向：单向收紧**。新增命中数（实际 > 基线）→ CI 失败（regression）。命中数下降（实际 < 基线）→ CI 通过，且**鼓励**（不强制）更新本基线以反映改善。
- **调高基线的唯一途径**：开独立计划，在该计划中逐项人工确认新增命中的合理性（合理偏离 / 已登记豁免 / 需重构），并显式更新本文件的基线表与下方机器可读块。**禁止在功能 PR 中直接调高基线**。
- **门控实现方式**：CI workflow（`.github/workflows/compliance.yml`）解析 checker 汇总表，比对下方 `## BASELINE (machine-readable)` 块。checker 脚本本身保持纯报告工具，不侵入其核心逻辑（Phase 1 Decision 裁决方案 b）。

## BASELINE (machine-readable)

> CI gate 解析本块。格式：`RULE=value`，每行一条。仅含可计数规则（R9 除外）。修改本块须经独立计划裁决（见上文"调高基线的唯一途径"）。

```yaml
R1a: 0
R1b: 0
R1c: 0
R1d: 23
R2a: 37
R2b: 319
R2c: 1108
R2d: 34
R3: 19
R4: 0
R5: 0
R6: 7
R7: 2
R8: 42
R10: 51
R11: 0
```

## 关联

- Checker: `docs/audits/nop-compliance-checker.sh`
- CI 门控: `.github/workflows/compliance.yml`
- 源 finding: `docs/audits/2026-07-23-0000-architecture-governance-review.md` F8 + 闭包前必须项 #4
- 计划: `docs/plans/2026-07-24-0930-1-compliance-guard-activation-ci-baseline.md`
- daoFor 分类（为何 R2c 基线合理）: `docs/plans/2026-07-16-2134-1-ddd-entity-methods-daofor-convergence.md`
