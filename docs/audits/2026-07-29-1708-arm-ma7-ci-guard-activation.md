# A7.4 CI/guard 持续激活验证报告

> 审计 ID：A7.4
> 里程碑：MA7（运维与性能层审计）
> 维度：CI/guard 持续激活验证（compliance checker 19 规则基线漂移 + CI 工作流激活性 + checker↔基线块同步 + 19 模块 web 测试 @Tag 持续覆盖 + F15 i18n checker CI 接入裁决）
> 域范围：全仓 + `.github/workflows/compliance.yml` + `docs/audits/nop-compliance-checker.sh` + `docs/audits/i18n-coverage-checker.sh` + 19 模块 `erp-*-web/src/test/java`
> Owner Doc：`docs/audits/compliance-baseline.md`（基线表 + machine-readable YAML 块 + §M0.3 锚点注记 + §F15 i18n 基线注记 + §回归门控规则）
> Skill：`none`（roadmap `compliance-checker` 简写无对应 skill 文件——`docs/skills/` 23 文件 + `.opencode/skills/` 均无匹配；方法源 = checker 脚本 + compliance-baseline.md 回归门控规则 + CI 工作流）
> 审计日期：2026-07-29
> 关联 plan：`docs/plans/2026-07-29-1708-2-ma7-ci-guard-activation-verification.md` Phase 1
> Source Audits: `docs/audits/2026-07-29-1708-arm-ma7-ci-guard-activation.md`
> Audit Status: closed

## Verdict: PASS（CI guard 持续激活、基线零漂移；零 P0 + 1 项 P1 + 零 P2）

A7.4 是 MV V.2（compliance 基线对比验证里程碑）的**前置事实确证**。本审计核实四项事实：

1. **A7.4-a compliance checker 基线漂移验证**：`bash docs/audits/nop-compliance-checker.sh` 实测全 19 可计数规则 actual **精确等于** `## BASELINE (machine-readable)` 块 baseline 值，**0 漂移**（0 regression + 0 improvement）。M0.3 锚点（HEAD=`0e963531d`，2026-07-27 实测落锚，记录 0 漂移）之后至当前 HEAD（`b933e2403`）共 62 commits（含 4 个 P0 即时通道 fix 触及 16 个生产 Java/ORM 文件）**未引入任何 daoFor/反模式回归**——CI 单向收紧门控持续生效。
2. **A7.4-b CI 工作流持续激活核实**：`.github/workflows/compliance.yml` 触发条件（push/PR `master` + `workflow_dispatch`）正确激活；checker 执行 + machine-readable 块解析（正则 `^\s*(R\d+[a-d]?)\b.*?(\d+)\s*$` 捕获汇总行尾数）+ 单向收紧门控判定（`actual > baseline => FAIL`）与 `compliance-baseline.md §回归门控规则` 声明**逐项一致**；checker 规则集（19 可计数）与基线块（19 行）**精确同步**，无 checker 新增/移除规则未登记基线块的不一致。
3. **A7.4-c web 测试 @Tag 持续覆盖验证**：19 模块 web 测试（`erp-*-web/src/test/java/.../Erp*WebPagesTest.java`）各含 `@Tag("full-app")`，**计数=19（精确匹配 19 业务域）+ 命名 100% 一致**（单一值 `full-app`，无拼写分歧）。CI 经 `web-pages-validation` job 复用 `ErpAllWebPagesTest`（非 @Disabled 聚合测试，`PAGE_ERROR_COUNT=0` 断言）执行；19 域级测试 un-@Disabled（tag-gated）清除 suppression 计数。A4.6-A4.8 view.xml drift 三批审计（388 view.xml）后 web 测试基线维持全绿（`known-good-baselines.md` 多条记录证实 `validateAllPages` 0 errors + E2E 全绿）。
4. **A7.4-d F15 i18n checker CI 接入裁决**（§F15 line 226 委托）：F15 `i18n-coverage-checker.sh` 当前**未接入 CI workflow**（实仓 grep `.github/workflows/` 无引用），属「可手动运行的回归门」。本审计裁决=**接入 CI**（对齐 F8 `nop-compliance-checker.sh` 接入模式），登记 **P1-MA7-007**（目标 MR3）。基线干净（quality 0 defects / strict 0 gaps，与 §F15 基线注记一致），接入不会触发 CI red；接入关闭 F15 「dead armor → live guard」最后一公里（同 F8 经 plan `2026-07-24-0930-1` 激活的范式）。

## 1. 19 规则漂移矩阵（baseline vs 实测 + M0.3 锚点后新漂移核实）

**审计方法**：`bash docs/audits/nop-compliance-checker.sh` 取汇总表实测值 × `compliance-baseline.md ## BASELINE (machine-readable)` YAML 块 baseline 值逐规则比对。

| 规则 | 描述 | Baseline | Actual（HEAD=`b933e2403`） | 漂移方向 | 裁决 |
|------|------|----------|----------------------------|---------|------|
| R1a | dao().saveEntity (BizModel) | 0 | 0 | 一致 | ✅ |
| R1b | dao().updateEntity (BizModel) | 0 | 0 | 一致 | ✅ |
| R1c | dao().getEntityById (BizModel) | 0 | 0 | 一致 | ✅ |
| R1d | dao().findAllByQuery (BizModel) | 17 | 17 | 一致 | ✅ |
| R2a | BizModel daoFor(ErpMd*) | 37 | 37 | 一致 | ✅ |
| R2b | BizModel daoFor(Erp*) 跨域 | 315 | 315 | 一致 | ✅ |
| R2c | 全生产代码 daoFor() 总量 | 1228 | 1228 | 一致 | ✅（合法裁决性上调，plan 2026-07-25-1057-2，非漂移） |
| R2d | Processor daoFor(ErpMd*) | 28 | 28 | 一致 | ✅ |
| R3 | new Erp*() 构造实体 | 5 | 5 | 一致 | ✅ |
| R4 | extends RuntimeException | 0 | 0 | 一致 | ✅ |
| R5 | @Inject private | 0 | 0 | 一致 | ✅ |
| R6 | @Transactional in BizModel | 2 | 2 | 一致 | ✅ |
| R7 | System.currentTimeMillis() | 0 | 0 | 一致 | ✅ |
| R8 | Processor 无 xbiz 接线 | 42 | 42 | 一致 | ✅ |
| R10 | REQUIRES_NEW 事务 | 6 | 6 | 一致 | ✅ |
| R11 | Processor 重复状态判断方法 | 0 | 0 | 一致 | ✅ |
| R12a | 共享内核 import ErpFinBusinessType | 69 | 69 | 一致 | ✅ |
| R12b | 共享内核 import PostingEvent | 66 | 66 | 一致 | ✅ |
| R12c | 共享内核 import AcctSchemaResolver | 38 | 38 | 一致 | ✅ |

**R9（doReverseApprove 一致性）**：定性校验（输出 ✓/✗ 清单无数值计数），不参与数值门控，CI 不对其做数值断言（与基线块设计一致）。

**汇总**：19 规则 **0 regression（actual > baseline）+ 0 improvement（actual < baseline）**——精确匹配，0 漂移。

### M0.3 锚点后新漂移核实

| 字段 | 值 |
|------|----|
| M0.3 锚点 HEAD | `0e963531d4b07d44b593828a7aab048ea0c9d3db`（2026-07-27 实测落锚，记录 0 漂移） |
| 当前 HEAD | `b933e2403a0658510d73c2cca343f6af8756e201` |
| commits 数 | 62 |
| 生产 Java/ORM 变更文件数 | 16（4 个 P0 即时通道 fix：P0-MA2-016 fin ProfitLossClosingService / P0-MA2-017 qa InspectionBizModel / P0-MA2-019 aps SchedulingProcessor+ErpApsCapacityReservation 新实体 / P0-MA2-020 inv StockMoveBookkeeper+OwnershipTransferProcessor+CostAdjustmentPostingDispatcher） |
| 锚点后 daoFor/反模式新增回归 | **0**——checker 实测全 19 规则 actual 精确等于 baseline |

**裁决**：M0.3 锚点之后（MA1-MA7 审计期间 + 4 个 P0 fix 落地）**无新引入漂移**。4 个 P0 fix 的生产代码变更（含 aps 新增 `ErpApsCapacityReservation` 实体 + inv 余额表 UK + qa 状态守卫 + fin 汇兑结转修复）均未新增 daoFor 跨域写/反模式——纪律强化（`docs/analysis/governed-path-cost-evaluation.md §基线漂移复发防护`）生效，closure audit 核实 checker 基线的纪律在 P0 fix plan 中被遵循。**结论：CI guard 持续激活、基线零漂移**，MV V.2 的 compliance 基线对比前置事实确证 PASS。

## 2. CI 工作流激活性核实（A7.4-b）

### 2.1 触发条件

`.github/workflows/compliance.yml` 声明：

```yaml
on:
  push:
    branches: [master]
  pull_request:
    branches: [master]
  workflow_dispatch:
```

**核实**：任何推送 master + 任何 PR 指向 master + 手动 dispatch 均触发 `compliance` job。CI 在 PR 流程中**实际运行**（非 dead armor）。

### 2.2 checker 执行 + 解析 + 门控判定

CI `compliance` job 三步：

1. **Run nop-compliance-checker**：`bash docs/audits/nop-compliance-checker.sh > checker-output.txt` 并 `cat` 打印。
2. **Enforce baseline gate**（Phase 1 Decision option b——门控逻辑在 CI，checker 保持纯报告工具）：
   - 解析 checker 汇总表：正则 `^\s*(R\d+[a-d]?)\b.*?(\d+)\s*$` 捕获汇总段内每行 `规则ID → 尾部数字`（自动跳过分隔线 `------` 与表头 `规则 描述...命中`——无尾数不匹配）。
   - 解析基线块：正则 ```` ```yaml\n(.*?)\n``` ```` 抽取 `compliance-baseline.md` 机器可读块，逐行 `RULE: value` 入 dict。
   - 门控判定：`actual > baseline => regressions（FAIL, sys.exit(1)）`；`actual < baseline => improvements（PASS，鼓励更新非阻塞）`；`actual == baseline => PASS`。
3. **Upload checker output**：`always()` 上传 `checker-output.txt` artifact（30 天保留）。

**与 `compliance-baseline.md §回归门控规则` 一致性核实**：

| owner doc 声明 | CI 实现 | 一致性 |
|---------------|---------|--------|
| 门控方向：单向收紧（actual > baseline → CI 失败） | `if act > base: regressions.append; sys.exit(1)` | ✅ 一致 |
| 命中数下降（actual < baseline）→ CI 通过，鼓励（不强制）更新基线 | `elif act < base: improvements.append`（仅打印，不 exit） | ✅ 一致 |
| 调高基线唯一途径：开独立计划裁决 | CI 失败时打印「open an independent plan and adjudicate each new hit」指引 | ✅ 一致 |
| 门控实现：CI 解析 machine-readable 块 | Python 解析 ```yaml 块 | ✅ 一致 |

### 2.3 web-pages-validation job

CI 第二 job `web-pages-validation`：`mvn -B -pl app-erp-all -am test -Dtest=ErpAllWebPagesTest`（Phase 2 Decision option a——复用非 @Disabled 聚合测试 `ErpAllWebPagesTest`，`PAGE_ERROR_COUNT=0` 断言，替代 19 域 full-app tag 测试以控 CI 时长）。19 域级测试 un-@Disabled（tag-gated）清除 suppression 计数。

**门控模拟**（当前 actual 全等于 baseline）：`regressions=[]` / `improvements=[]` → `OK: no rule exceeds baseline. Gate PASSED.` ✅

## 3. checker 规则集 ↔ 基线块同步性核实

| 项 | checker 汇总表 | 基线 YAML 块 | 同步 |
|----|---------------|-------------|------|
| 可计数规则数 | 19（R1a-d / R2a-d / R3-R8 / R10-R11 / R12a-c） | 19（同集合） | ✅ |
| R9 处理 | 定性校验，汇总表无 R9 行（仅 §R9 段打印 ✓/✗ 清单） | 基线块无 R9 行（§基线表注记明示 R9 不参与数值门控） | ✅ |
| 规则 ID 集合 | {R1a,R1b,R1c,R1d,R2a,R2b,R2c,R2d,R3,R4,R5,R6,R7,R8,R10,R11,R12a,R12b,R12c} | 同集合 | ✅ |

**裁决**：checker 与基线块**精确同步**——无 checker 新增规则但基线块未登记 / 基线块有规则但 checker 已移除的不一致。checker 校准历史（R3 orm.xml 白名单 / R8 module-common-service + per-mutation 排除 / R1d·R6·R10 注释排除）均经独立计划裁决并同步更新基线块（见 compliance-baseline.md 各 §注记）。

## 4. 19 模块 web 测试 @Tag 持续覆盖验证（A7.4-c）

**审计方法**：`grep -rl '@Tag' module-*/erp-*-web/src/test/` 计数 + `@Tag` 值分布。

| 项 | 实测 | 裁决 |
|----|------|------|
| 含 @Tag 的 web 测试文件数 | **19**（精确匹配 19 业务域） | ✅ 全覆盖 |
| @Tag 值分布 | 19 × `@Tag("full-app")`（单一值，100% 命名一致） | ✅ 无拼写分歧 |
| profile/tag 过滤逻辑 | CI 经 `ErpAllWebPagesTest`（非 @Disabled 聚合）执行 `PAGE_ERROR_COUNT=0` 断言；19 域级 tag-gated 测试 un-@Disabled | ✅ |

**19 模块清单**（逐域 `Erp*WebPagesTest.java`）：aps / assets / b2b / contract / crm / cs / drp / finance / hr / inventory / logistics / maintenance / manufacturing / master-data / notify / projects / purchase / quality / sales。

**A4.6-A4.8 view.xml drift 审计后 web 测试状态**：A4.6（fin+mfg 134 view.xml）+ A4.7（pur+sal+inv 114 view.xml）+ A4.8（crm+hr 140 view.xml）三批共 388 view.xml drift 修复后，`known-good-baselines.md` 多条记录证实 `validateAllPages` 0 errors + E2E 全绿（如 2026-07-16 基线 `mvn test -pl app-erp-all -Dtest=ErpAllWebPagesTest` validateAllPages 0 errors；2026-07-25 全 E2E 490 passed）。web 测试 @Tag 持续有效。

**已知 skipped**：`ErpAllWebPagesCollectTest` `@Disabled`（JDK 26/ANTLR 兼容性致 PAGE_ERROR_COUNT 0↔203 跳变，见 `docs/bugs/2026-07-20-2200-page-error-count-instability.md`，M0.3 锚点记录 1 skipped）——非 @Tag 覆盖回归，非本审计范围。

## 5. F15 i18n checker CI 接入裁决（A7.4-d）

### 5.1 F15 现状核实

| 项 | 实测 | 裁决 |
|----|------|------|
| F15 checker 存在 | `docs/audits/i18n-coverage-checker.sh`（python3，quality + `--strict` 双模式） | ✅ |
| quality 模式基线 | DEFECTS=0 / COVERAGE GAPS=0 / EXIT 0（扫描 373 文件：354 view.xml + 19 action-auth.xml） | ✅ 与 §F15 基线注记一致 |
| strict 模式基线 | DEFECTS=0 / COVERAGE GAPS=0 / EXIT 0 | ✅ 与 §F15 基线注记一致 |
| CI 接入状态 | `.github/workflows/` grep `i18n-coverage-checker` **零引用** | ⚠️ 未接入 CI（「可手动运行的回归门」） |

### 5.2 裁决：接入 CI（对齐 F8 模式）

**裁决依据**：

1. **§F15 line 226 显式委托**：`compliance-baseline.md §F15 i18n 基线注记` 明示「接入 CI 由 A7.4（CI/guard 激活审计）裁决（对齐 F8 `nop-compliance-checker.sh` 的 CI 接入模式）」。本审计即该裁决点。
2. **F8 先例**：F8 `nop-compliance-checker.sh` 经 plan `2026-07-24-0930-1` 从「dead armor」（首审 F8 finding：guard 可能是 dead armor）激活为「live guard」。F15 当前状态与 F8 激活前同型（脚本存在 + 基线落锚但未接入 CI），裁决=对齐 F8 接入。
3. **成本极低**：F15 checker 纯 python3（无 maven 依赖），373 文件扫描秒级完成，接入 CI 不显著增加时长。
4. **基线干净**：quality 0 defects / strict 0 gaps——接入不会触发 CI red（无活跃缺陷）。
5. **回归风险真实**：view.xml 修改频繁（A4.6-A4.8 三批 388 view.xml + 多份 E2E plan 持续修改）——开发者新增中文 `label=`/`title=` 而未补 `i18n-en:*` 对应项时，strict 模式可捕获，但当前仅人工运行 checker 时才暴露。接入 CI 后自动阻断 i18n 覆盖回归。

**登记 P1-MA7-007**（目标 MR3）：F15 `i18n-coverage-checker.sh` 接入 `.github/workflows/compliance.yml`（或新建 job），对齐 F8 模式——checker 执行 + 解析双模式基线（quality defects=0 / strict gaps=0）+ 单向收紧门控。接入实施须独立 plan（触及 CI 工作流，本审计 Non-Goal 排除直接变更）。

## 6. Finding 汇总

### P0（即时通道）

无。CI guard 持续激活、基线零漂移、web @Tag 全覆盖、F15 基线干净——无活跃数据破坏或回归。

### P1（目标 MR3）

| Finding ID | 描述 | 目标 MR | 修复状态 |
|-----------|------|--------|---------|
| `P1-MA7-007` | **F15 `i18n-coverage-checker.sh` 未接入 CI workflow**——`docs/audits/i18n-coverage-checker.sh`（F15 i18n regression gate，quality + `--strict` 双模式）当前仅「可手动运行的回归门」，`.github/workflows/` 零引用。基线干净（quality 0 defects / strict 0 gaps，与 `compliance-baseline.md §F15 i18n 基线注记` 一致），接入不会触发 CI red。**A7.4-d 裁决=接入 CI**（对齐 F8 `nop-compliance-checker.sh` 经 plan `2026-07-24-0930-1` 激活的范式，关闭 F15「dead armor → live guard」最后一公里）。view.xml 修改频繁（A4.6-A4.8 三批 388 view.xml + 多份 E2E plan），未接入期间 i18n 覆盖回归仅人工运行 checker 才暴露。修复方式：MR3——`.github/workflows/compliance.yml` 新增 job（或扩展现有 compliance job）执行 `bash docs/audits/i18n-coverage-checker.sh --strict`，解析双模式基线（defects=0 / gaps=0）做单向收紧门控（actual > baseline => FAIL）。触及 CI 工作流，修复须独立 plan。 | MR3 | todo |

### P2（watch-only）

无。checker↔基线块同步（19=19）+ CI 门控逻辑与 owner doc 一致 + web @Tag 19/19 命名一致 + F15 基线干净——四维度均 PASS，无 watch-only 残留。

## 7. 与 MA1-MA6 已登记 finding 交叉去重

| 本审计观察 | 已登记 finding | 关系 |
|-----------|---------------|------|
| F15 i18n checker 未接入 CI | A4.9（`2026-07-29-0749-arm-ma4-i18n-coverage.md` line 165 已登记「F15 checker 未接入 CI ... 登记为 A7.4 输入」） | **委托闭合**：A4.9 委托 A7.4 裁决，本审计（A7.4）裁决=接入 CI 并登记 P1-MA7-007。A4.9 不重复登记 P1（其报告仅记录观察 + 委托），P1-MA7-007 是该委托的唯一裁决产出，无重复 |
| compliance checker CI 激活 | F8（首审 `2026-07-23-0000` finding）+ 闭包前必须项 #4（plan `2026-07-24-0930-1` 已激活） | **已闭包确认**：本审计核实 F8 已从 dead armor 激活为 live guard（CI 实际运行 + 门控逻辑正确 + 基线零漂移持续生效），不重复登记 |
| checker 校准残留风险（R3/R8/R1d/R6/R10 content-based 排除） | compliance-baseline.md 各 §注记 + checker 脚本内联注释 | **已登记**：每项校准的残留风险（如未来 per-mutation Processor 不继承抽象基类则 R8 漏排除）已在 checker 注释 + 基线注记显式声明 successor 触发条件，本审计不重复登记 |

## 8. Exit Criteria 核实

- [x] 19 规则漂移矩阵产出（§1 baseline vs 实测 + M0.3 锚点后新漂移核实 + 逐条裁决）+ CI 激活性核实结论（§2）+ web @Tag 覆盖结论（§4）+ F15 CI 接入裁决（§5）
- [x] A7.4 P0/P1/P2 已登记 arm-index.md（零 P0 + 1 P1 + 零 P2），且与 MA1-MA6 既有 P1 交叉去重无重复（§7：P1-MA7-007 是 A4.9 委托的唯一裁决产出）
