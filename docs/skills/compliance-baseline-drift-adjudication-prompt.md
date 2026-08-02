# Compliance 基线漂移裁决提示

> **项目定制化层（nop-app-erp）**：使用本提示前必须先读 `docs/skills/README.md §项目定制化层（nop-app-erp）`，将本仓库的保护区域（`module-<domain>/model/*.orm.xml` ask-first、会计/财务/数据删除）、验证命令（`bash docs/audits/nop-compliance-checker.sh`、`mvn clean install -DskipTests`）、已知失败模式（见 `docs/lessons/07-compliance-baseline-drift-on-plan-changes.md`）注入上下文。本提示的通用默认值在本仓库不充分。

在裁决 compliance checker 报出的 `actual > baseline` 漂移时使用此提示——区分「合法新增需 baseline-raise」与「真违规需 Fix」，避免整批 raise 掩盖违规（假阴性）或把合法增长误报回归（假阳性）。

## 使用场景

- 计划闭合（closure）或 MV 验证时，`bash docs/audits/nop-compliance-checker.sh` 报出 `actual > baseline`。
- 加深 / fix / 业财一体计划在模型/代码层新增了 checker 可计数的站点（`daoFor(...)` / `import io.nop...` / 状态字面量 / `@Inject` / 跨实体访问）后。
- codegen 增量重生成（`mvn clean install -DskipTests`）后核对基线是否漂移。
- 对照 M0 锚点（`docs/audits/compliance-baseline.md §M0 锚点注记`）复跑时发现 post-M0 漂移。

## 不使用场景

- 任务是单文件低风险编辑（文案/小样式），不触及 checker 可计数站点 → 跑一次 checker 确认即可，无需裁决流程。
- 业务设计审计 / ORM 字段规范审计（用 `design-doc-audit-prompt.md` / `orm-model-audit-prompt.md`）。
- 平台最佳实践合规审计（12 维度，用 `nop-platform-conformance-audit-prompt.md`）——本提示只管 checker 计数与基线，不管平台规范定性。

## 必需输入

- `docs/audits/compliance-baseline.md`（§基线表 + §BASELINE 机器可读块 + §M0 锚点注记 HEAD 哈希）
- `docs/audits/nop-compliance-checker.sh`（checker 脚本，19 可计数规则）
- M0 锚点 / 上次基线提交的 HEAD 哈希（用于 `git diff <锚点>..HEAD` per-site 分类）
- owner docs 背书：`docs/architecture/processor-extension-pattern.md` / `data-dependency-matrix.md` / `docs/analysis/shared-kernel-extraction-decision.md`（+ owner-doc 登记面 `docs/architecture/module-boundaries.md §共享内核`）（裁决跨域只读访问是否合法）
- 当前 working tree（可跑 checker + git diff）

## 预期输出

- `docs/audits/compliance-baseline.md` 更新：§基线表 baseline 值 + §BASELINE 机器可读块 baseline 值（**两处同步**）
- 每条漂移规则的裁决记录：规则名 / actual / baseline / 裁决（baseline-raise 至 N / Fix）/ per-site 分类证据（git diff 站点 + 分类理由 + owner doc 背书）
- 若有 Fix 类：代码修复 + 重跑 checker 确认 actual 回落（**不 raise 基线掩盖**）

## 步骤

```text
1. 跑 `bash docs/audits/nop-compliance-checker.sh`，记录每条规则 actual vs baseline。
   → actual == baseline 全规则：无漂移，结束。
   → 任一 actual > baseline：进入步骤 2。

2. 对每条漂移规则，逐站点 git diff <锚点>..HEAD 定位新增站点：
   - R5（@Inject private）：grep `@Inject` + `private` 同行。
   - R2a/R2b/R2c（daoFor）：grep `daoFor(Erp...`。
   - R12c（共享内核 import）：grep 新增 import io.nop... 跨工程。
   - 状态字面量：grep 新增 setStatus("...") 硬编码。

3. 对每个新增站点分类裁决（二选一）：
   a. baseline-raise（合法新增）：
      - 同域内部访问（domain 内部实体）。
      - owner doc 背书的跨域只读（如期间结账清理 / dashboard 聚合 / 共享内核 AcctSchemaResolver）。
      - 经 posting-exemptions.md 登记的写豁免。
      → 更新 §基线表 + §BASELINE 块 baseline 值至 actual。
      → 记录分类理由 + owner doc 指针。
   b. Fix（真违规）：
      - @Inject private（Nop IoC 硬规则，见 skills README §已知失败模式 #6）。
      - 字符串比较 == / !=（应用 Objects.equals）。
      - 绕过 I*Biz 越权写（应经 I*Biz 接口）。
      - System.currentTimeMillis() / LocalDateTime.now()（应用 CoreMetrics）。
      → 改代码使 actual 回落，不 raise 基线。
      → 重跑 checker 确认 actual < baseline。

4. 严禁整批 raise：必须 per-site 分类。整批 raise 会把真违规（如 Case B @Inject private）当成合法增长默默放过（假阴性）。

5. §基线表 与 §BASELINE 机器可读块两处必须同步更新。CI（`.github/workflows/compliance.yml`）读 §BASELINE 块判定门控。

6. 裁决后重跑 checker 确认全 19 规则 actual ≤ baseline（0 裸漂移）。
```

## 自检反模式清单

- [ ] 是否对**每个**漂移规则做了 per-site git diff 分类（而非整批 raise）？
- [ ] baseline-raise 是否同步更新了 §基线表**和** §BASELINE 机器可读块（两处）？
- [ ] Fix 类站点是否真的改了代码使 actual 回落（而非 raise 基线掩盖）？
- [ ] 裁决理由是否引用了 owner doc 背书（processor-extension-pattern / data-dependency-matrix / docs/analysis/shared-kernel-extraction-decision / posting-exemptions）？
- [ ] 是否重跑 checker 确认 0 裸漂移？
- [ ] @Inject private / 字符串 == / System.currentTimeMillis 是否被识别为 Fix 而非 raise？（这三类永远是 Fix，不可 raise）
- [ ] 是否对照 M0 锚点而非任意基线（确保 post-M0 漂移都被捕获）？

## 关联

- lesson：`docs/lessons/07-compliance-baseline-drift-on-plan-changes.md`（失败模式 + case + 决策树）
- 真相源：`docs/audits/compliance-baseline.md`
- 裁决先例：plan `2026-07-25-1057-1`（Round 1）/ `2026-07-28-0823-1`（Round 2）/ `2026-07-31-1705-2` V.2（Round 3，5 项 post-M0 漂移逐项裁决）/ `2026-07-31-1330-1` G.1（0 漂移收敛）
- CI 门控：`.github/workflows/compliance.yml`
