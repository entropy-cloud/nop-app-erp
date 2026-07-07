# Lesson 03: 计划状态文本一致性

> **来源**：2026-07-07 多维审计补充整改（plan 2026-07-07-2200-1，Draft Review B3/N1）
> **适用场景**：计划编写、阶段状态更新、关闭门控
> **失败模式**：计划 `Status:` 行与 `[x]/[ ]` 复选框不一致，导致执行驱动器误判阶段完成状态

## 问题

计划文件中存在两层状态表达：

1. **阶段级** `Status: completed|planned|active`
2. **条目级** `- [x]` / `- [ ]` 复选框

当只更新其中一层（如将 `Status` 改为 `completed` 但未勾选所有 `[ ]`），或两层表达冲突（`Status: completed` 但有未勾选的 `[ ]`），执行驱动器（mission-driver）会重新触发该阶段，造成 `EXECUTE ↔ CLOSURE_VERIFY` 无限循环。

## 根因

1. 手动更新状态时只改了 `Status:` 行，忘记同步勾选复选框
2. 阶段完成时只勾选了 Fix 项，遗漏了 Proof 或 Exit Criteria 项
3. 前序阶段完成后，后续阶段的状态未同步更新

## 正确做法

### 阶段完成时的强制同步规则

完成一个阶段时，**必须同时**：

1. 勾选该阶段内**所有** `- [ ]` 为 `- [x]`（包括 Fix、Proof、Decision、Exit Criteria）
2. 将该阶段的 `Status: planned` 改为 `Status: completed`
3. 两者在同一编辑中完成，不可分开

### 计划完成时的全量验证

```bash
# 检查是否有 Status: completed 但仍有未勾选项的阶段
grep -B5 "\- \[ \]" docs/plans/<plan-file>.md | grep "Status: completed"
# 如果有输出，说明存在不一致
```

## 反模式

```
Status: completed          ← 改了状态
- [ ] Fix: xxx             ← 但没勾选
- [ ] Proof: yyy           ← 也没勾选
```

## 正例

```
Status: completed          ← 状态和复选框同步
- [x] Fix: xxx             ← 全部勾选
- [x] Proof: yyy
```

## 检查清单

- [ ] `Status: completed` 的阶段内无 `- [ ]` 残留
- [ ] `Status: planned` 的阶段内 `- [ ]` 数量与预期未完成项一致
- [ ] 关闭门控（Closure Gates）的 `- [ ]` 全部勾选后才标 `Plan Status: completed`
