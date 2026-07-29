# 2026-07-29-0749-3-audit-remediation-ma4-i18n-coverage MA4 全域 i18n 完整性审计（A4.9）

> Plan Status: active
> Mission: audit-remediation
> Work Item: A4.9（全域 i18n 完整性，MA4 代码与前端质量层 i18n 项）
> Last Reviewed: 2026-07-29
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA4（工作项 A4.9，i18n 完整性，全域合并跑 checker）
> Related: `docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.4「i18n 完整性（MA4）」行；`docs/audits/arm-index.md`（P1 索引）；`docs/audits/i18n-coverage-checker.sh`（审计工具——F15 i18n regression gate）；`docs/plans/2026-07-29-0430-3-audit-remediation-ma4-finance-mfg-view-xml-drift.md`（A4.6）+ `2026-07-29-0749-1-...-pur-sal-inv-...`（A4.7）+ `2026-07-29-0749-2-...-crm-hr-...`（A4.8）（view.xml drift 三批——本审计与其同属前端层但关注 i18n 覆盖维度，互补）
> Audit: required

## Current Baseline

全域 19 业务域 i18n 完整性审计（代码与前端质量层 MA4 i18n 项 A4.9）。roadmap 工作项 A4.9 声明"i18n 完整性（全域合并跑 checker）"，owner doc 标注 `docs/audits/i18n-coverage-checker.sh`，skill 列为 `i18n-checker`（即 checker 脚本本身，非 docs/skills/ 下的提示词技能）。MA4 的 view.xml drift 三批（A4.6/A4.7/A4.8）关注契约一致性，本审计关注**前端 i18n 覆盖与质量**——是互补的前端层维度。本项完成后 i18n 维度收口；MA4（代码与前端质量层，9 工作项 A4.1a-A4.9）全域收口**仅当 A4.7 + A4.8 亦完成时**（当前仍 todo）。

**关键基线事实（实时仓库核实）**：

- **审计工具**：`docs/audits/i18n-coverage-checker.sh`（F15 i18n regression gate，164 行 python3 脚本，无 xmllint 依赖）。扫描 19 业务域**手写**（排除 `/target/`、`/_dump/`、`/_gen/`）的 `*.view.xml` + `*.action-auth.xml`。两种模式：
  - **quality 模式**（默认）：对每个声明了 `i18n-en:*` 属性的元素，断言值 (a) 非空、(b) 非裸键（无残留 CJK、非纯 `${...}`）、(c) 非与源中文 `label=/title=/displayName=` 完全相同。报告缺陷 + 覆盖缺口摘要。
  - **strict 模式**（`--strict`）：任何覆盖缺口（有中文 `label=/title=/displayName=` 但无对应 `i18n-en:*`）= 失败。
  - 退出码：0 = clean，1 = defects found。
**审计对象规模**（实测运行 checker，权威值来自工具自报告）：全域 19 业务域手写（排除 `/target/`、`/_dump/`、`/_gen/`）`*.view.xml` **354 个** + `erp-*.action-auth.xml` **19 个** = **合计 373 个文件**（每域一份 action-auth.xml）。注：`find module-<domain> -name "*.view.xml" -not -path "*/target/*"`（仅排除 target）会得到 ~704 个含 `_gen/`/`_dump/` 生成文件的计数——**非** checker 审计范围；本审计以 checker 实际扫描的 373 手写文件为准。
- **i18n 维度（适配 checker 语义）**：(1) **i18n-en 缺陷**——已声明 `i18n-en:*` 但值有问题（空值 / 残留 CJK / 纯 `${...}` / 与源中文雷同），属已尝试英文化但质量不合格的 major 缺陷；(2) **覆盖缺口**——有中文 `label=/title=/displayName=` 但无 `i18n-en:*` 的元素，属未英文化的覆盖缺口（strict 模式判失败，quality 模式 informational）；(3) **域级覆盖分布**——哪些域已系统英文化、哪些域零覆盖，识别系统性缺口域。
- **已知 i18n 历史与基线**：
  - frontend-ui-roadmap（`2026-07-23-1408-3`）系统性为 19 域 view.xml + action-auth.xml 补充 i18n-en 标签，接入 CI F15 gate。本审计是对该批英文化工作的**首次系统性完整性审计**。
  - **当前 checker 实测结果（2026-07-29 实时运行）**：`bash docs/audits/i18n-coverage-checker.sh`（quality 模式）= 扫描 373 文件（354 view.xml + 19 action-auth.xml），**0 defects，0 coverage gaps，EXIT 0（PASS）**；`--strict` 模式 = **同样 0 defects，0 gaps，EXIT 0（PASS）**。即 frontend-ui-roadmap 英文化批次当前已使全域 i18n 覆盖与质量**完全干净**。本审计的主要价值因此从"发现缺陷"转为：**确认并登记零基线** + 复核 frontend-ui-roadmap 完整性 + 为 A7.4 CI guard 提供权威 i18n 基线锚点。若执行时复跑仍为 0，则无新 P1/P2 产生（计划仍有效——它登记了零基线并完成完整性确认）。
  - `nop-compliance-checker.sh` R 列不含 i18n 规则（i18n 由独立的 `i18n-coverage-checker.sh` F15 承载），compliance-baseline.md 不含 F15/i18n 基线行。本审计产出后登记 i18n 基线（0 defects / 0 gaps）供 A7.4 CI guard 持续激活验证复核。
  - 多份 E2E plan 修复 page.yaml `$var` 模板 + reportContainer 接线时可能引入/遗漏 i18n 标签——本审计复核（当前 0 缺陷表明未引入回归）。
- **MA4 已审计的 view.xml drift finding（i18n 审计交叉输入）**：A4.6/A4.7/A4.8 view.xml drift 三批若发现 view.xml 有新增/修改，其 i18n 覆盖一致性是本审计的交叉复核项。

**审计张力**：frontend-ui-roadmap 已系统性英文化 19 域前端，且**当前 checker 实测已完全干净**（0 defects / 0 gaps / strict PASS）。但从未做一次**全域合并跑 checker 并登记基线**的完整性审计（roadmap §其他纪律明确"i18n 完整性归 A4.9 全域合并跑 checker"）。本审计的核心价值是：(1) 独立复跑确认零基线（非依赖 frontend-ui-roadmap 自述）；(2) 登记 i18n 基线锚点（0 defects / 0 gaps）供 A7.4 CI guard 持续激活验证；(3) 复核 frontend-ui-roadmap 完整性 + E2E plan 修改 view.xml 的 i18n 一致性；(4) 将 i18n 覆盖维度正式纳入审计-修复闭环（即使当前干净，基线锚点 + 维度收口是 roadmap 的显式工作项）。本审计用机械化 checker 全域扫描 + 人工复核，区分"已英文化但有质量缺陷"（major）与"未英文化覆盖缺口"（strict 失败）——当前两者均为 0。

剩余差距：需要一次全域 19 域合并跑 `i18n-coverage-checker.sh`（quality + strict 双模式）确认零基线 + 完整性审计 + 基线登记。预期产出：0 defects / 0 gaps 的权威基线行 + frontend-ui-roadmap 完整性确认 + scope matrix i18n 行收口。若复跑发现非零缺陷（执行时与起草时基线可能漂移），则按缺陷类型分类：i18n-en 质量缺陷（major）登记 P1 / 覆盖缺口（strict 失败）登记 P2 watch-only。i18n 缺陷修改 view.xml action-auth.xml 标签（非 ORM 保护区域），但触及用户可见前端国际化行为。

## Goals

- 全域 19 业务域合并跑 `docs/audits/i18n-coverage-checker.sh`（quality 模式 + strict 模式），**确认零基线**（起草时实测 0 defects / 0 gaps / 双模式 EXIT 0）并产出机械扫描结果 + 完整性确认审计报告。
- 审计覆盖 3 i18n 维度：i18n-en 质量缺陷（空值/残留 CJK/纯变量/与源雷同）/ 覆盖缺口（有中文 label 无 i18n-en）/ 域级覆盖分布（系统性缺口域识别）——当前三者均预期为 0/干净。
- 复核 frontend-ui-roadmap 英文化批次完整性 + 多份 E2E plan 修改 view.xml 的 i18n 一致性 + A4.6/A4.7/A4.8 view.xml drift finding 中新增/修改 view.xml 的 i18n 覆盖。
- 登记 i18n 基线（0 defects / 0 gaps，或执行时实测值）至 `docs/audits/compliance-baseline.md`，供 A7.4 CI guard 持续激活验证复核。
- scope matrix §2.4「i18n 完整性（MA4）」行推进至完成。**注**：A4.9 完成后 i18n 维度收口；MA4（代码与前端质量层）仅当 A4.7 + A4.8 亦完成时全域收口（当前 A4.7/A4.8 仍 todo）。
- 若复跑发现非零缺陷：i18n-en 质量缺陷登记为 P1 汇总至 `arm-index.md` §P1 发现汇总（起始编号 = MA4 已分配最大 P1-MA4-N（023）+ 1 = P1-MA4-024，避免命名空间碰撞）；覆盖缺口登记为 P2 watch-only。若复跑仍为 0（预期），则无新 P1/P2 产生，计划以零基线 + 维度收口完满收尾。roadmap A4.9 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**做 view.xml 契约 drift（字段名/动作名/枚举/参数类型）— 归 A4.6/A4.7/A4.8（view.xml drift 三批）。本审计只关注 i18n 覆盖与质量。
- **不**做后端 BizModel/xbiz 代码实现质量 — 归 A4.1a-A4.5（已 done）。
- **不**做报表/看板 page.yaml 渲染层契约问题 — 前端 UI-roadmap + 多份 E2E plan 已修复。本审计审 view.xml + action-auth.xml 的 i18n 标签。
- **不**做像素级视觉回归 — 归前端 UI-roadmap Deferred。
- **不**做测试覆盖深度 — 归 MA5。
- **不**在本计划内批量修复 i18n 缺陷 — P1 经 R2.0 展开机制进入 MR2（i18n 标签修复属代码变更）。本审计只识别 + 分类 + 登记基线。
- **不**手改 view.xml 或 action-auth.xml。
- **不**重写或扩展 `i18n-coverage-checker.sh` 本身（仅作为审计工具运行；如发现 checker 缺陷，登记为 Follow-up 而非就地修改）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/i18n-coverage-checker.sh`（审计工具 + 语义定义——quality/strict 双模式断言规则）；`module-*/erp-*-web/src/main/resources/_vfs/*.view.xml` + `*.action-auth.xml`（全域 19 域审计对象）；`docs/audits/compliance-baseline.md`（i18n 基线登记目标）
- Skill Selection Basis: roadmap A4.9 skill 列为 `i18n-checker`，即 `docs/audits/i18n-coverage-checker.sh` 脚本本身（非 docs/skills/ 下的提示词技能）。审计方法是机械化 checker 全域扫描 + 人工分类，不适用多维度审计提示词。故 Phase 记 `Skill: none`，审计方法由 checker 脚本语义定义 + 本计划描述的 3 维度分类。
- Verification: 审计不改代码/文档，故无单测回归；报告产出即更新 `arm-index.md` + 登记 i18n 基线。i18n 缺陷修复在 MR2 批量进行。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。本审计依赖 `python3`（checker 脚本唯一依赖，无 xmllint），不运行应用。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码/文档，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。checker 扫描无回归风险，build/test 门控为同型审计 plan 的标准 Closure 实践。

## Execution Plan

### Phase 1 - 全域 19 域 i18n-coverage-checker 双模式扫描 + 3 维度分类审计

Status: planned
Targets: 全域 19 业务域手写 `*.view.xml` + `*.action-auth.xml`（`module-*/erp-*-web/src/main/resources/_vfs/`，排除 `/target/` `/​_dump/` `/​_gen/`）；审计工具 `docs/audits/i18n-coverage-checker.sh`（quality + strict 双模式）
Skill: none

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）；frontend-ui-roadmap done（19 域系统性英文化已落地，本审计审其完整性）；A4.6/A4.7/A4.8 done 或并行（view.xml drift 三批提供新增/修改 view.xml 清单作为 i18n 交叉输入——若 A4.7/A4.8 未完成，本审计仍可独立执行，交叉复核项标注"待 A4.7/A4.8 完成后复核"）。

- [ ] 运行 `bash docs/audits/i18n-coverage-checker.sh`（quality 模式）全域扫描，记录退出码 + 缺陷清单（i18n-en 质量缺陷：空值/残留 CJK/纯 `${...}`/与源中文雷同）+ 覆盖缺口摘要。**起草时实测 = 0 defects / 0 gaps / EXIT 0（PASS）**；执行时复跑确认零基线或捕获任何漂移。
      - Skill: none
- [ ] 运行 `bash docs/audits/i18n-coverage-checker.sh --strict`（strict 模式）全域扫描，记录退出码 + 全部覆盖缺口（有中文 `label=/title=/displayName=` 无 `i18n-en:*` 的元素）。**起草时实测 = 0 gaps / EXIT 0（PASS）**；执行时复跑确认。
      - Skill: none
- [ ] 维度「i18n-en 质量缺陷」分类：对 quality 模式报告的每个缺陷（若复跑为 0 则确认干净）定位文件路径+行号+元素，分类为 空值 / 残留 CJK / 纯 `${...}` 变量 / 与源中文雷同 四类，评估严重性（major——英文用户仍看到错误/中文内容）。
      - Skill: none
- [ ] 维度「覆盖缺口」分类：对 strict 模式报告的缺口（若复跑为 0 则确认干净）按域 + 按元素类型（grid 列 / form 字段 / page 标题 / 按钮 / tab）聚合，识别系统性缺口域（零覆盖或低覆盖域）vs 偶发缺口。
      - Skill: none
- [ ] 维度「域级覆盖分布」：统计 19 域每域的 i18n-en 覆盖率（有 i18n-en 元素数 / 有中文 label 元素数），产出覆盖热力分布（当前预期全域 ~100% 覆盖——确认 frontend-ui-roadmap 无遗漏域；无需过度投入）。
      - Skill: none
- [ ] 交叉复核：frontend-ui-roadmap 英文化批次完整性（是否有遗漏域）+ 多份 E2E plan 修改 view.xml 的 i18n 一致性 + A4.6/A4.7/A4.8 view.xml drift finding 中新增/修改 view.xml 的 i18n 覆盖（若 A4.7/A4.8 未完成，标注"待复核"）。
      - Skill: none
- [ ] checker 工具健全性复核：确认 checker 扫描范围（19 域 view.xml + action-auth.xml）、排除规则（`/target/` `/​_dump/` `/​_gen/`）、断言规则正确执行；如发现 checker 自身缺陷，登记为 Follow-up（不在本计划修改 checker）。
      - Skill: none
- [ ] 产出审计报告 `docs/audits/2026-07-29-0749-arm-ma4-i18n-coverage.md`（含：quality + strict 双模式原始结果 / 3 维度分类（质量缺陷/覆盖缺口/域级分布）/ 19 域覆盖热力 / 交叉复核结果 / P0-P3 finding 清单按严重性排序（每项含文件路径+行号+元素+缺陷描述+严重性+目标 MR）/ checker 健全性裁决 / 剩余风险）。
      - Skill: none

Exit Criteria:

> 审计报告是唯一可观察产物。完整仓库 `mvn test` 属 Closure Gates（见执行时规则 7）。

- [ ] quality + strict 双模式扫描结果产出（含退出码 + 完整缺陷/缺口清单）
- [ ] 3 维度分类结果产出（i18n-en 质量缺陷 4 类 / 覆盖缺口按域+类型聚合 / 19 域覆盖热力分布）
- [ ] frontend-ui-roadmap + E2E plan + A4.6/A4.7/A4.8 交叉复核产出（每项标记"无 i18n 缺口"或"发现 i18n 缺陷"）
- [ ] P0-P3 finding 清单产出按严重性排序，每个含文件路径+行号+元素+缺陷描述+严重性+目标 MR
- [ ] checker 健全性裁决产出

### Phase 2 - finding 汇总交接 MR2 + 基线登记 + 索引/矩阵更新

Status: planned
Targets: i18n finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.4「i18n 完整性（MA4）」行；`docs/audits/compliance-baseline.md`（i18n 基线登记）
Skill: none

- Item Types: `Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [ ] finding 汇总：**若复跑发现非零缺陷**——i18n-en 质量缺陷登记为 P1 至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA4-NNN`，起始编号 = MA4 已分配最大 P1-MA4-N（023）+ 1 = P1-MA4-024，避免命名空间碰撞）；覆盖缺口登记为 P2 watch-only 至 §P2 发现汇总。**若复跑仍为 0（预期，起草时实测干净）**——无新 P1/P2 产生，报告注明"零缺陷基线确认"。与 A4.6/A4.7/A4.8 已登记 P1 经交叉去重无冲突。
      - Skill: none
- [ ] 分类裁决：i18n-en 质量缺陷目标 MR2（i18n 标签修复属代码变更）；覆盖缺口 P2 watch-only。当前预期两者均为 0（基线干净）。
      - Skill: none
- [ ] 登记 i18n 基线：在 `docs/audits/compliance-baseline.md` 新增 i18n 基线行（quality 模式 defects 数 + strict 模式 gaps 数——起草时实测 0/0），供 A7.4 CI guard 持续激活验证复核。
      - Skill: none
- [ ] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §2.4「i18n 完整性（MA4）」行（标记 i18n 维度收口）。**注**：MA4 全域收口仅在 A4.7 + A4.8 亦完成时——本计划不单独声明 MA4 收口。
      - Skill: none

Exit Criteria:

- [ ] 所有 i18n-en 质量缺陷已登记 arm-index §P1 汇总（i18n 代码类 MR2），覆盖缺口登记 §P2 watch-only，待展开
- [ ] 与 A4.6/A4.7/A4.8 已登记 P1 经交叉去重无重复登记
- [ ] i18n 基线已登记 compliance-baseline.md（quality 缺陷数 + strict 缺口数）
- [ ] arm-index 报告清单 + scope matrix 已反映审计结论（i18n 维度收口；MA4 全域收口仅在 A4.7 + A4.8 亦完成时）

## Draft Review Record

- Independent draft review iteration 1: **needs-revision**（`ses_054da2331ffeab4VlDLYzeir5v`，独立 general 子代理 fresh-context）——VERDICT: needs-revision，3 项 BLOCKER：(B1) 审计对象规模错误——按域 view.xml 计数（72/72/.../6）合 704 非 656，且 `-not -path "*/target/*"` 含 `_gen/`/`_dump/` 生成文件却误标"手写"；checker 实际扫描 354 view.xml + 19 action-auth.xml = 373 手写文件。(B2) Current Baseline 缺当前 checker 实测结果——实测 quality+strict 双模式均 0 defects/0 gaps/EXIT 0（已干净），计划却暗示将发现缺陷。(B3) "MA4 全域收口" 闭包主张不准确——A4.7/A4.8 仍 todo，A4.9 单独完成不闭包 MA4。LIVE-REPO 复核 checker 语义/工具存在/scope matrix i18n 行/roadmap A4.9 行/skill:none 论证 全 ✓；Guide R2/3/4/7/8/12 + anti-slack 全通过。修订：B1 改 373 手写文件规模 + 标注 704 为生成文件非 checker 范围；B2 加实测干净基线 + Goals/Phase 重构为"确认+登记零基线"；B3 全部 MA4 收口主张改条件式（仅当 A4.7+A4.8 亦完成）。
- Independent draft review iteration 2: **accept**（`ses_054d5975affeQiUBpevw1ZAdmM`，独立 general 子代理 fresh-context）——VERDICT: accept，3 项 BLOCKER 全解决。复核：B1 规模现 354+19=373 ✓（grep 656/合计约 零残留，704 已限定为生成文件计数）；B2 Current Baseline 含实测 0/0/EXIT 0 双模式，独立重跑 checker 双模式 RESULT: PASS/EXIT 0 确认主张成立，Goals/Phase 重构为"确认+登记零基线" ✓；B3 全文"收口"grep：MA4 级主张全改条件式"仅当 A4.7+A4.8 亦完成时"，残留仅为"i18n 维度收口"（有效）；roadmap A4.7/A4.8=todo 确认 ✓；P1 编号 023+1=024 正确；无新问题。Plan Status 转 active。

## Closure Gates

> 本计划主体是机械化 checker 全域扫描 + 人工分类（不改代码；产出为审计报告 + arm-index/scope-matrix/compliance-baseline 更新）。完整仓库验证在此处运行一次（同型审计 plan 的标准 Closure 实践）。i18n 缺陷修复在 MR2 批量进行。本审计只识别 + 分类 + 登记基线。

- [ ] 范围内行为完成（A4.9 全域 i18n 完整性审计报告产出 + arm-index 更新 + i18n 基线登记[0/0 或实测值] + scope matrix i18n 行收口）
- [ ] 相关文档对齐（审计报告、arm-index、scope matrix、compliance-baseline 结论已反映）
- [ ] 已运行验证：checker 扫描无代码变更，build/test 门控仅作回归基线确认（同型审计 plan 的相同 Closure 实践）
- [ ] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR2；checker 自身缺陷若有则登记 Follow-up 并命名触发条件）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证（状态、阶段、门控、日志都一致）
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留空作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### view.xml 契约 drift（A4.6/A4.7/A4.8）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计关注 i18n 覆盖与质量；view.xml 契约 drift（字段名/动作名/枚举/参数类型）归 A4.6（已 done）/A4.7/A4.8。
- Successor Required: `yes`——A4.7/A4.8 执行时复核 pur/sal/inv + crm/hr view 契约 drift（i18n 交叉输入）。

### i18n 缺陷批量修复

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本审计只识别 + 分类 + 登记基线；i18n-en 质量缺陷修复目标 MR2（i18n 标签修复属代码变更），覆盖缺口 P2 watch-only 修复优先级由 MR2 裁决。
- Successor Required: `yes`——R2.0 展开机制将 P1 finding 展开为 MR2 具体修复工作项行。

## Closure

Status Note: <待执行与独立结束审计填充>

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计填充>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- 无非阻塞跟进项目（i18n 缺陷修复按设计进入 MR2 批量进行[若复跑发现非零]；本批次完成后 i18n 维度收口——MA4 全域收口仅在 A4.7 + A4.8 亦完成时）。
