# Closure-pending 检测与批量 closure-audit 提示

> **项目定制化层（nop-app-erp）**：使用本提示前必须先读 `docs/skills/README.md §项目定制化层（nop-app-erp）`，将本仓库的保护区域（`module-<domain>/model/*.orm.xml` ask-first、会计/财务/数据删除、auth/permissions）、验证命令（`mvn clean install -DskipTests`）、已知失败模式（见 `docs/lessons/08-plan-closure-without-independent-audit.md`）注入上下文。本提示的通用默认值在本仓库不充分。

在检测「声称 completed 却缺独立 closure audit 证据」的计划，并批量编排独立子代理 fresh session 补 closure 时使用此提示。这是**检测 + 批量编排**方法，与单计划 closure 审计（`closure-audit-prompt.md`）互补。

## 使用场景

- 大批计划（如 audit-remediation roadmap 多里程碑）闭合后，系统性回溯哪些 `Status: completed` / `Plan Status: completed` 的计划**缺独立子代理 closure 证据**。
- mission-driver 自动推进多计划后，确认每份都有独立 fresh-session closure（非执行者自填）。
- 过程纪律审计（如 A6.4 保护区域纪律）发现系统性 closure-pending 时，形式化清理批次。

## 不使用场景

- 审计**单个**已完成计划是否真闭合 → 用 `closure-audit-prompt.md`（本提示编排的每个独立子代理内部就用它）。
- 计划实施前的拦截审计 → 用 `plan-audit-prompt.md`。
- 任务是全新起草计划（尚无 closure 需求）。

## 必需输入

- 计划集合（如 `docs/plans/` 下某批次 / 某里程碑覆盖的计划）
- closure 证据判定标准：`## Closure` 段是否含 `Independent Closure Audit` + 独立子代理 task `ses_` 指针（非执行者自填 `Status Note`）
- 保护区域清单（`docs/context/ai-autonomy-policy.md` §保护区域）——保护区域计划需额外 plan-audit 证据
- `docs/skills/closure-audit-prompt.md`（每个独立子代理运行的单计划审计提示）
- 可调度独立子代理 fresh session 的能力

## 预期输出

- **closure-pending 候选清单**：计划文件 + closure 缺口分类（缺独立审计 / 假勾选 Gate / 缺 plan-audit 等）+ 处置策略（方案 A 补 closure / 方案 B 其它）
- 每份候选回填的 `## Closure` 段：`Independent Closure Audit (Round N)` Auditor 指针（task `ses_` id）+ 五点一致性复核 + anti-hollow（核对生成产物/测试真实存在）+ deferred honesty（deferred 项显式 adjudication）+ 实时仓库复核要点
- 统计：候选数 / 方案 A 数 / 方案 B 数 / PASS 数

## 步骤

```text
1. 生成候选清单：
   a. 枚举范围内所有 `Status: completed` / `Plan Status: completed` 的计划。
   b. 对每份 grep `## Closure` 段：
      - 含 `Independent Closure Audit` + task `ses_` 指针 → 已闭合，剔除。
      - 仅执行者自填 `Status Note` / 无独立指针 → closure-pending 候选。
      - Closure Gate 复选框勾选但无审计证据 → 「假勾选」高危候选。
   c. 减去已被前序轮次清理的（去重，避免重复审计）。

2. 对每份候选分类 closure 缺口 + 选处置策略：
   - 保护区域计划（ORM ask-first / deployment / auth）：方案 A（补独立 closure）+ 确认是否也缺实施前 plan-audit。
   - 非保护区域计划：方案 A（补独立 closure）。
   - 假勾选 Gate：方案 A，但审计时重点核查 Gate 是否实际满足。

3. 批量编排独立子代理：
   - 每份候选分配一个独立子代理（fresh session / cold context，非执行者本人）。
   - 子代理运行 `closure-audit-prompt.md` 对该单计划做 closure 审计。
   - 禁止执行者自我审计自己的产出（AGENTS.md §技能组合使用方式反模式）。

4. 子代理产出回填：
   - 在计划 `## Closure` 段补 `Independent Closure Audit (Round N)` 段：
     * Auditor / Agent + task `ses_` 指针
     * 五点一致性复核（Status 行 vs [x]/[ ] / Plan Status / roadmap 工作项 / arm-index finding）
     * anti-hollow（生成产物 / 测试真实存在，非空壳）
     * deferred honesty（deferred 项显式 adjudication，非沉默降级）
     * 实时仓库复核要点（非仅复述计划文案）

5. 统计 + 收尾：
   - 候选数 / PASS 数 / needs-revision 数。
   - needs-revision 的回到执行者修复后重审。
   - 保护区域计划额外确认 plan-audit 证据（实施前）存在。
```

## 自检反模式清单

- [ ] 候选清单是否去重了前序轮次已清理的计划（避免重复审计）？
- [ ] 每份候选是否由**独立子代理 fresh session**审计（非执行者本人 / 非 cold replay）？
- [ ] 回填证据是否含 task `ses_` 指针（可追溯）而非仅文字声明？
- [ ] 回填证据是否含 anti-hollow（核对生成产物/测试真实存在）+ deferred honesty？
- [ ] 回填证据是否含**实时仓库复核**（非仅复述计划文案）？
- [ ] 保护区域计划是否额外核查了实施前 plan-audit 证据？
- [ ] 假勾选 Gate 是否被识别并重点核查（非放过）？
- [ ] 冷重播是否未被当作第二位审查者批准保护区域（见 `docs/context/ai-autonomy-policy.md`）？

## 关联

- lesson：`docs/lessons/08-plan-closure-without-independent-audit.md`（失败模式 + 与 lesson 03 区分 + 反模式表）
- 单计划审计：`docs/skills/closure-audit-prompt.md`（本提示编排的每个子代理内部使用）
- 计划前审计：`docs/skills/plan-audit-prompt.md`
- 真相源：`docs/audits/arm-index.md`（P1-MA6-003/004/005）
- 清理先例：plan `2026-07-31-1439-1` R3.5（Round 3，14 份全 PASS 回填）
- 过程纪律审计：`docs/audits/2026-07-29-1410-arm-ma6-protected-area-discipline.md`（A6.4）
