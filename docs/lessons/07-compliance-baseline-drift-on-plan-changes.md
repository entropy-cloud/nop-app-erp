# Lesson 07: Compliance checker 基线漂移——加深计划新增站点后未同步基线 / closure 未核对

> **来源**：2026-07 审计-修复任务（MA1–MA7 + MR1–MR3 + MV + MG）。compliance checker 基线漂移经 **3 轮独立裁决**（plan `2026-07-25-1057-1` Round 1 / plan `2026-07-28-0823-1` Round 2 / plan `2026-07-31-1705-2` V.2 Round 3）才收敛；MG G.1（plan `2026-07-31-1330-1`）复核确认 0 漂移。
> **适用场景**：任何使用「checker 计数 + BASELINE 机器可读块单向收紧门控」机制的项目（如本项目的 `docs/audits/nop-compliance-checker.sh` + `docs/audits/compliance-baseline.md §BASELINE`）。每当代码生成、新增跨实体访问、新增 daoFor/import、新增硬编码状态字面量等会**改变 checker 计数**的工作发生时。
> **失败模式**：加深计划或 fix 计划在模型/代码层新增了 checker 可计数的站点（新 `daoFor(...)`、新 `import io.nop...`、新状态字面量），但**既未在 closure 前重跑 checker 核对基线，也未把合理新增站点提升为新的基线值**。下一次 CI 或 MV 验证发现 `actual > baseline` → 被误判为回归（假阳性 red），或本应裁决为 Fix 的站点被默默放过（假阴性）。

## 核心论点

Compliance checker 是**单调基线 + 单向收紧门控**：`actual > baseline => FAIL`。这意味着基线**必须随合理新增同步上移**，否则门控会把一切合法增长都报为回归。漂移的根因不是「写了违规代码」，而是「写了合法代码但没同步基线块」——这是一个**过程纪律失败**，不是代码质量失败。

## 失败模式（典型路径）

```
1. 加深计划新增 N 个跨域只读 daoFor / import / 状态字段（均合法，经 owner doc 背书）
2. mvn test 通过 → 提交 → 标 done
3. closure audit 只核对 mvn / plan 项，没跑 checker
4. 下一次 CI（或 MV V.2）跑 checker → actual > baseline → red
5. 裁决者逐站点 git diff 锚点..HEAD 分类（baseline-raise vs Fix）→ 回填 BASELINE 块
6. 下一轮计划又新增站点 → 回到步骤 2 → 反复多轮才收敛
```

## 真实案例

### Case A: 3 轮裁决才收敛（1057-1 / 0823-1 / V.2）

- **Round 1（plan 2026-07-25-1057-1）**：MR1 ORM 模型变更 + 业财过账深化引入大量跨域只读访问与共享内核 import。checker 多规则 `actual > baseline`。逐站点 git diff 分类后，裁决为 baseline-raise（同域内部访问 + 文档化跨域只读），回填基线块。
- **Round 2（plan 2026-07-28-0823-1）**：MR2 继续深化（dashboard/report 聚合、期间结账清理、AcctSchemaResolver 共享内核），又一波站点漂移。再次 per-site 分类 + baseline-raise。
- **Round 3（plan 2026-07-31-1705-2 V.2）**：对照 M0 锚点（HEAD=`0e963531d`）复跑，5 项 post-M0 漂移逐项裁决：R5（`@Inject private`，0→1）**Fix**；R2a/R2b/R2c/R12c **baseline-raise** 至实测值。此轮还裁决了「口径分歧」（154 vs 156 模块）落定。
- **MG G.1（plan 2026-07-31-1330-1）**：复核确认 19 规则 actual ≤ baseline，**0 漂移**才正式收敛。

### Case B: R5 `@Inject private` 被基线漂移掩盖一轮

V.2 裁决中 R5 规则（`@Inject private`）从 0→1。这不是合法新增而是**真违规**（`ErpRoleDataAuthChecker.java:30-31`，违反 Nop IoC 硬规则，见 skills README §已知失败模式 #6）。因为它被混在「一波 baseline-raise」里，若不逐站点 git diff 分类，极易被当成合法增长默默放过（假阴性）。**教训：漂移必须 per-site 分类，不能整批 raise。**

## 决策树：计划触及模型/代码后，问「这会不会改 checker 计数？」

```
1. 本计划是否新增/修改了 daoFor(...)、import、硬编码状态字面量、@Inject、跨实体访问？
   → 否：checker 不会漂移，跳过本清单。
   → 是：进入步骤 2。

2. 跑 `bash docs/audits/nop-compliance-checker.sh`，对照 compliance-baseline.md §BASELINE 块。
   → actual == baseline：无漂移，正常收尾。
   → actual > baseline：进入步骤 3。

3. 逐站点 git diff <M0 锚点或上次基线提交>..HEAD，按以下分类裁决：
   a. 合法新增（同域内部访问 / owner doc 背书的跨域只读 / 共享内核）→ baseline-raise（更新 BASELINE 块 baseline 值）。共享内核裁决依据见 `docs/analysis/shared-kernel-extraction-decision.md` + `docs/architecture/module-boundaries.md §共享内核`。
   b. 真违规（@Inject private / 字符串比较 == / 绕过 I*Biz 越权写）→ Fix（改代码使 actual 回落，不 raise 基线）。
   → 严禁整批 raise；真违规会被掩盖。

4. 更新 compliance-baseline.md：§基线表 + §BASELINE 机器可读块（两处必须同步）。
```

## 自检清单（计划 closure 前必跑）

- [ ] 跑了 `bash docs/audits/nop-compliance-checker.sh` 并对照 `## BASELINE` 块？
- [ ] 若 actual > baseline：是否对**每个**新增站点做了 git diff per-site 分类（baseline-raise vs Fix）？
- [ ] baseline-raise 是否同步更新了 §基线表**和** §BASELINE 机器可读块（两处）？
- [ ] Fix 类站点是否真的改了代码使 actual 回落，而非 raise 基线掩盖？
- [ ] 裁决理由是否引用了 owner doc（如 `processor-extension-pattern.md` / `data-dependency-matrix.md` / `docs/analysis/shared-kernel-extraction-decision.md`）背书？

## 何时复发

- 任何 codegen 增量重生成（`mvn clean install -DskipTests` 触发）后未跑 checker。
- MR / 加深 / 业财一体计划批量新增跨域访问后，closure 只跑 mvn 不跑 checker。
- 模型源（`*.orm.xml`）变更后只验证 ORM 层，忽略下游生成代码引入的新 import / daoFor。

## 关联

- 真相源：`docs/audits/compliance-baseline.md`（§基线表 + §BASELINE 机器可读块 + §M0 锚点注记）
- checker 脚本：`docs/audits/nop-compliance-checker.sh`
- CI 门控：`.github/workflows/compliance.yml`
- 裁决证据：plan `2026-07-31-1705-2` V.2（5 项 post-M0 漂移逐项裁决）+ plan `2026-07-31-1330-1` G.1（0 漂移收敛）
- 关联 lesson：本失败的「裁决法」已提升为 skill `docs/skills/compliance-baseline-drift-adjudication-prompt.md`
- 关联速查：`docs/context/project-context.md` §已知失败模式（plan `-1330-1` G.4 内联摘要）
