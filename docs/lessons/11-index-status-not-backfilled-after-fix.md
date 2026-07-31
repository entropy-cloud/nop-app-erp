# Lesson 11: 修复完成未回填追踪索引——arm-index `todo (R*.x)` 陈旧标签

> **来源**：2026-07 审计-修复任务。V.5（plan `2026-07-31-1705-3`，审计报告索引完整性校验）发现 **102 条** arm-index 中状态为 `todo (R*.x)` 但 roadmap 对应工作项早已 `done` 的陈旧标签。75 条 MR1（R1.12–R1.29）+ 27 条 MR2（R2.6/R2.8–R2.15）。
> **适用场景**：任何「真相源有多个投影面」的项目——本项目有 roadmap（工作项状态）、arm-index（finding 真相源 + 修复状态列）、计划文件（执行记录）三面。修复在 roadmap/计划闭合后，**必须回填** arm-index 的 `修复状态` 列。
> **失败模式**：修复工作项在 roadmap 标 `done`、计划标 `completed`，但 arm-index 对应 finding 行的 `修复状态` 列仍停留在 `todo (R1.x)` / `todo (R2.x)`。索引与真相漂移，后续审计（V.5 全量可追溯复核）发现「裸 todo」（看似未修实则已修），被迫批量回填。

## 核心论点

当 finding 的真相分散在多个投影面时，**状态回填是闭合的一部分**，不是事后清理。一个 finding 的完整闭合要求：

1. **roadmap 工作项** `todo` → `done`。
2. **计划文件** `Plan Status: completed` + closure 证据。
3. **arm-index finding 行** `修复状态` 列从 `todo (R*.x)` → `done (R*.x)`（含闭合回填写）。

漏掉第 3 步 = 索引与真相漂移。回填不是可选——V.5 的「全量可追溯」门控会把它揪出来。晚回填的成本远高于即时回填：V.5 一次性回填 102 条，远比每条修复时顺手回填一行费力且易漏。

## 失败模式（典型路径）

```
1. 审计产出 finding F，登记 arm-index `修复状态: todo (R1.12)`
2. roadmap 工作项 R1.12 实现 → 标 done
3. 计划闭合 → Plan Status: completed
4. ❌ 忘记回填 arm-index F 行的 `修复状态: todo (R1.12)` → `done (R1.12, plan ...)`
5. 下一个 finding 同样漏回填 ... 累积 102 条
6. V.5 全量可追溯复核：grep arm-index 裸 todo → 102 条「已修但未回填」
7. 批量回填 102 行（远比即时回填费力，且需逐条核对 finding↔工作项映射）
```

## 真实案例

### Case: V.5 发现 102 条陈旧 `todo (R*.x)`

- **构成**：MR1 批次 75 条（R1.12–R1.29，覆盖 finance/mfg/hr/inv/qa/prj/contract/aps/logistics 等域）+ MR2 批次 27 条（R2.6 mfg owner-doc drift / R2.8–R2.15 文档+代码+view.xml drift）。
- **状态**：roadmap 这些工作项**全部早已 done**，但 arm-index finding 行的 `修复状态` 列仍 `todo (R*.x)`。
- **后果**：arm-index 作为「finding 真相源」对外失真——任何按 arm-index 统计未修 finding 数的人会得到严重虚高的数字。
- **修复（plan `2026-07-31-1705-3` V.5）**：全表 191 P1 + 6 P0 + 3 跨维度发现逐条核对 finding↔工作项映射，102 条全部回填 `done (R*.x)` + 闭合回填写，达成「零裸 todo 全可追溯」。

## 自检清单（修复工作项闭合时）

- [ ] roadmap 工作项已 `done`？
- [ ] 计划已 `Plan Status: completed` + closure 证据？
- [ ] arm-index 中该工作项覆盖的**每个 finding 行**的 `修复状态` 列已从 `todo` 回填为 `done (R*.x)` + 闭合回填写？
- [ ] 回填写含计划指针（如 `done (R1.12, plan 2026-07-29-...)`）？
- [ ] 若 finding 被 deferred：`修复状态` 标 `deferred` + Deferred But Adjudicated 段含 adjudication？

## 何时复发

- 多域批量修复时，关注点在代码/owner doc，索引回填被遗忘。
- finding ID 与工作项编号非一一对应（一个工作项覆盖多个 finding）时，漏回填部分 finding。
- deferred finding 未在 arm-index 显式标 deferred，停留在 todo。

## 关联

- 真相源：`docs/audits/arm-index.md`（finding 真相源 + 修复状态列）
- 工作项状态：`docs/backlog/audit-remediation-roadmap.md`
- 修复证据：plan `2026-07-31-1705-3` V.5（102 条回填）
- 关联 lesson：lesson 08（closure-pending——同属「闭合未完成」家族，08 缺外部审计，11 缺索引回填）
