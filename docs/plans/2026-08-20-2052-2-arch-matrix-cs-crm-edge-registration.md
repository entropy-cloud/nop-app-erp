# 2026-08-20-2052-2-arch-matrix-cs-crm-edge-registration 架构矩阵补登 cs→crm Java 层边（RC-R1.65 契约漂移修复）

> Plan Status: completed
> Last Reviewed: 2026-08-21（closed）
> Source: docs/audits/2026-08-17-2125-multi-audit-requirement-compliance.md F2（P1）+ docs/audits/2026-08-17-2125-open-audit-requirement-compliance.md F2（P1，复用 multi F2 同根因同控制点）
> Related: docs/plans/2026-08-17-2125-1-rc-mr1-r1-65-cs-ticket-create-enrichment.md（RC-R1.65 实现计划，收尾清单补记对象）
> Audit: required

## Current Baseline

- **实仓依赖已落地**：`module-cs/erp-cs-service/pom.xml:58-61` — `app-erp-crm-dao` compile scope，pom 注释自述「工单创建自动分配候选池（RC-R1.65，P1-RC-054，UC-CS-01 ④）经 IErpCrmTeamBiz/IErpCrmTeamMemberBiz 跨域只读查询（cs team code -> crm 同码团队成员 userId）。cs→crm 单向（R），DAG 无环」。**消费侧容错机制实况**：`TicketAssignResolver.java:34-38` 以普通 `@Inject` 注入两个 crm I*Biz（非 `@Nullable`），失败隔离经 try/catch（:48/:67）+ 池空/解析失败 → 留 NEW + ⑧ 客服主管升级通知（见 roadmap RC-R1.65 done 行 D3/D4）。
- **架构权威文档缺失**：`docs/architecture/data-dependency-matrix.md` grep `crm-dao` = 0 命中；§2.4 表（:127-140）已登记 multi 审计 F2 计数的全部 8 条 MR1 新增 Java 层边（cs→qa、pur→drp、pur→ct、assets→mnt、mfg→mnt、mnt→mfg、mnt→qa、aps→notify；表内另含 R1.61/85 等先例行），cs→crm 是 MR1 边中唯一漏登项。
- **`module-boundaries.md` 无需同步**：该文件不枚举 Java 层 pom 边（grep 证实；其内容为 owner-doc 路由表 + 共享内核类型节 `:85-90` + ORM 跨模块规则，均非 §2.4 同构登记面）。
- **§2.3 三类依赖计数为 ORM 层口径**（R/S/P 表层计数），Java 层 §2.4 为文字注记，计数不受影响。
- **闭合声明缺口**：roadmap RC-R1.65 done 行与 plan `2026-08-17-2125-1` 收尾同步清单均未列 matrix 登记（对比 R1.68/76/77/78/81/85/86 行内显式「matrix §2.4 登记」）。

## Goals

- `data-dependency-matrix.md §2.4` 补 cs-service → crm-dao 登记行（镜像 :127 cs→qa 行格式），使 MR1 全部 9 条 Java 层跨域边在架构权威文档可见。
- RC-R1.65 计划收尾记录补记 matrix 登记完成（追溯闭环）。

## Non-Goals

- 不改动 `module-cs` 代码与 pom（依赖本身正确，仅文档登记缺失）。
- 不复核其余 8 条已登记边的语义（超出本 finding 范围）。
- 不处理 multi 审计其余 finding（F1 归 plan `2026-08-20-2052-1`，F3/F4/F5 归 Follow-up Backlog）。

## Task Route

- Type: `implementation-only change`（架构 owner doc 契约漂移修复，纯文档）
- Owner Docs: `docs/architecture/data-dependency-matrix.md`（§2.4）
- Skill Selection Basis: 纯文档登记，无匹配技能（`Skill: none`）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 - 矩阵登记 + 收尾补记

Status: completed
Targets: `docs/architecture/data-dependency-matrix.md`（§2.4 表）、`docs/plans/2026-08-17-2125-1-rc-mr1-r1-65-cs-ticket-create-enrichment.md`（Closure 补记）
Skill: none

- Item Types: `Fix`
- Prereqs: 无（与 plan `2026-08-20-2052-1` 无执行依赖，可并行）

- [x] Fix: §2.4 表追加行（镜像 :127 cs→qa 行格式与克制注记，措辞以实仓机制为准）：`| cs-service → crm-dao | IErpCrmTeamBiz/IErpCrmTeamMemberBiz（工单创建自动分配候选池：cs team code → crm 同码团队成员 userId 池，只读） | RC-R1.65（P1-RC-054） | 单向叶依赖：crm 对 cs 零反向（ORM + Java 双向均无），DAG 无环；crm 查询失败经 try/catch 失败隔离（池空/解析失败 → 留 NEW + 客服主管升级通知，非 @Nullable 容错路径——节前言 :123 约定的既有偏离先例见 :137-138） |`——表格列序与文字对齐既有行风格，pom 注释为语义真相源
  - Skill: none
- [x] Fix: plan `2026-08-17-2125-1` 的 `## Closure` 节追加补记行：matrix §2.4 cs→crm 边登记由本 plan 完成（来源 multi 审计 F2，注明原收尾清单漏项）
  - Skill: none
- [x] Fix: roadmap `docs/backlog/requirement-compliance-roadmap.md:457` RC-R1.65 done 行末追加短语「matrix §2.4 cs→crm 边登记补齐（本 plan，2026-08-20）」——对齐 R1.68/76/77/78/81/85/86 兄弟行内显式「matrix §2.4 登记」的回写对称性
  - Skill: none

Exit Criteria:

- [x] `grep -n "crm-dao" docs/architecture/data-dependency-matrix.md` ≥ 1 命中且位于 §2.4 表内；表格渲染完整（列数与表头一致）
- [x] RC-R1.65 plan Closure 补记在盘；roadmap :457 行末登记短语在盘

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（`ses_fe0c255fbffemE3L17PqF6Na6a`，1 MAJOR / 3 MINOR）——MAJOR：拟登记行的「@Nullable 注入容错」与实仓不符（`TicketAssignResolver.java:34-38` 普通 `@Inject` + try/catch 失败隔离）；MINOR：module-boundaries.md 表述不精确、日志条目误列为阶段项、「8 条」计数歧义。已全部修订：行文改为实仓机制（try/catch + 池空留 NEW + 升级通知，镜像 :127 克制注记）、baseline 补共享内核节说明与 MR1 边/先例行计数区分、日志项移至 Closure Gates 计划级步骤。
- Independent draft review iteration 2: `acceptable as-is`（`ses_fe0bcccbbffe2EP2hy9oOIUz77`，0 MAJOR / 2 MINOR）——MAJOR 全解（行文与实仓机制逐字核对一致，端到端核至 `ErpCsTicketBizModel:161-163`）；F2 双修复义务覆盖 + 指南合规通过。2 残留 MINOR 已采纳：①行注记补「非 @Nullable 容错路径」半句（预防结束审计对 §2.4 前言 :123 约定的偏离质询，偏离先例 :137-138 在盘）；②roadmap :457 RC-R1.65 done 行补登记短语（对齐兄弟行回写对称性，新增第三个 Fix 项）。共识达成，转 active。

## Closure Gates

> 本计划无代码/配置变更（纯文档），删除构建/测试验证命令门控并说明原因：改动面仅 markdown 文档，无可执行产物受影响；一致性以 grep 复核代替。`docs/logs/2026/08-20.md` 条目为计划级结束步骤（时间倒序），在此完成。

- [x] 范围内行为完成（矩阵行登记 + 收尾补记）
- [x] 相关文档对齐（§2.4 与 pom/roadmap 声明一致）
- [x] 一致性复核完成（grep 证据 + 表格完整性）
- [x] `docs/logs/2026/08-20.md` 条目在盘（计划级结束步骤）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

（无）

## Closure

Status Note: Phase 1 全勾选 + 三交付物在盘（matrix §2.4 :141 登记行 / RC-R1.65 plan Post-closure 补记 :272-274 / roadmap :457 行末登记短语）+ grep 一致性复核通过（crm-dao 唯一命中 :141 位于 §2.4 表内 4 列完整）+ 独立结束审计 PASS——completed。纯文档计划（零代码/配置/ORM 变更），构建/测试门控按 Closure Gates 声明以 grep 复核替代。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（新会话，task `ses_fdff1737affeH9YCUSe1TnaBRm`，2026-08-21）
- Evidence: VERDICT **PASS**（0 MAJOR / 0 MINOR / 4 INFO）——①计划一致性（Phase 1 + Exit Criteria 全 [x]，Closure Gates 仅剩审计门待本证据闭合，Plan Status active 期间审计）②matrix :141 行位于 §2.4 表内（header :125-126，4 列与表头一致，内容与 Fix spec 逐项吻合；crm-dao 全文件唯一命中）③Post-closure 补记 :272-274 定位/来源/漏项说明核验④roadmap :457 短语逐字在盘 + 行结构完整（6 cells 与兄弟行一致，done ✅ 保持）⑤语义准确性对实仓核验（pom :60-64 app-erp-crm-dao compile + TicketAssignResolver :34-38 普通 @Inject 非 @Nullable + :48/:67 try/catch + :68 池空留 NEW 升级通知；module-crm 对 cs 零反向 grep 0 命中）⑥日志条目 :5 与盘上状态一致⑦git 脏集 = 5 个预期文件，module-cs 代码/pom 零触及，Non-Goals 遵守。4 INFO：§5.6.2 无 crm-dao 先前提及（退出判据 ≥1 命中仍满足）；pom 引用行号 :58-61 vs 实际 :60-64 偏移（可忽略）；matrix 行类名带反引号（镜像 :127 格式的计划自身要求，语义等价）；兄弟计划 2052-1 脏文件属其自身预期范围。

Follow-up:

- （无——已确认的契约漂移属不可降级 Fix，不出现在此处）
