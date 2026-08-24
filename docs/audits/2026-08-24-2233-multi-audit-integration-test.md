# 多维审计报告 — mission `integration-test`（重执行，当前 HEAD 复核）

> Audit Status: triaged
> Audit Type: multi-dimensional
> Mission: integration-test
> Remediation: 0 P0 / 0 P1 / 4 P2——四项 P2 均为 2026-08-24-2233 首轮双审计已发现项的**当前状态复核**（全部仍未修复、均已登记 `docs/backlog/integration-test-roadmap.md § Follow-up Backlog` 带触发条件，该登记为工作树未提交变更）。P2-only ⇒ 按 Mission Driver 分级规则归后续 backlog，无需修复计划（triage 已完成，勿重复分派；本轮已终态标记 `triaged`）。

- **审计对象**：`integration-test` mission 整件工作（M0.1-M0.3 设计/基建/分批 + B1-B10 共 22 用例 + V.1 全量回归 + V.2 收尾对齐 + 窗口内同前缀关联计划 0900-1/1147-1/2115-1），焦点 `./`（代码、配置、测试、公开契约），对照架构/设计文档核验契约漂移。
- **审计者**：独立多维度审计（fresh session，无执行者上下文；mission-driver 2026-08-24-223318 派发，2026-08-25 重执行）。
- **与首轮报告的关系**：本文件覆盖首轮同路径报告。首轮结论（0 P0 / 0 P1 / 4 P2）经本次**全量独立复核确认**，并新增：① 当前 HEAD 活体复跑证据（测试套件 + compliance checker）；② 姊妹开放式审计 P1 发现（OA-01/02/03）的路由状态核验（三份 0330-* remediation 计划 `active` 在盘、未执行）。
- **方法**：按 `docs/skills/multi-dimensional-audit-prompt.md` 7 默认维度 + 3 项目特定维度（ORM 完整性 / 代码生成纪律 / view.xml gen-control 契约）+ 已知失败模式 9-13 加权执行；本轮活体验证：
  - `mvn test -pl app-erp-all`（2026-08-25 04:20 实跑，前置 `lsof` 8011/8080 无 live server）：**54/0/0/1，BUILD SUCCESS，墙钟 01:35**——与 V.1 基线（54/0/0/1）逐项零漂移。
  - `bash docs/audits/nop-compliance-checker.sh`（实跑）：R1d=14/R2a=34/R2b=237/R2c=1505/R2d=38/R3=5/R6=2/R10=12/R12a/b/c=70/66/41——与 `compliance-baseline.md` §BASELINE 机器可读块（:471-495）**逐项相等，零漂移**（lesson 07 复核通过）。
- **实仓结构核验**：`app-erp-all/src/test/java/io/nop/app/all/it/` = 22 用例类 + 试点 `TestErpP2pPilot` + 基类 `ErpIntegrationTestCase` + 6 冻结时钟扩展 = 30 文件；`_cases/io/nop/app/all/it/` = **23 用例目录 1:1**（1562 文件，6.0M）；`db/`、`_tmp/`、`_dump/` gitignore 正确（`git check-ignore` 实证）。覆盖矩阵 Σ=88 逐域复算通过（6+15+8+7+18+3+3+5+2+2+3+2+2+2+2+2+2+2+2=88，19/19 域 ≥2）。
- **边界抽验**：B4-B10 六个 feat 提交 `git show --name-only` 复核，文件全部落在 docs/ + app-erp-all 测试域（`_cases`/`src/test`/pom），零生产代码零 ORM 零 seed。基类源码通读：机制 (c)（`setLocalDb(false)`+`setTableInit(false)` 双模抑制 + `initBeans` 显式触发惰性 `DataInitInitializer` + 每类 1 次删库重建）与设计文档 §3.2/§3.3/§3.4 逐点一致；`it/` 测试代码零裸时钟调用（grep `System.currentTimeMillis|LocalDateTime.now` = 0）。

---

## 发现（按严重性排序）

> 本审计 **0 × P0 / 0 × P1 / 4 × P2**。四项 P2 全部为首轮已发现、本轮复核**仍未修复但已完成 backlog 登记**的存量项——按分级规则不驱动修复计划，维持 follow-up backlog 触发条件处置。

### [P2] P2-IT-01 设计文档 §6 用例编号标签「C01-C22」失实——实无 C22，实集 = C01-C21（含 C20a/C20b）= 22 用例
- **一行理由**：纯文档标签不一致（`docs/design/integration-testing.md:191` 写「C01-C22」，全文无 C22 用例；§7 矩阵 / seed-data.md 面 2 盘点 / backlog README P8 均正确写「C01-C21 含 C20a/C20b」），计数语义（22 用例、Σ=88、19/19 域 ≥2）无一处受影响。
- **本轮复核**：`grep -n "C22" docs/design/integration-testing.md` 仍仅命中 :191 标签自身——**未修复**；follow-up backlog 行在盘（触发条件：下次触碰该文档时顺手修正）。
- **维度**：owner-doc 对齐。

### [P2] P2-IT-02 e2e-runbook「集成测试」运行方式注释计数陈旧——「既有 12 基建类」vs 实仓当前非 it 测试类 14 个
- **一行理由**：`docs/testing/e2e-runbook.md:1013` 注释「全量（含既有 12 基建类…）」为设计基线时点计数；mission 窗口内同前缀 flux 会话计划（1147-1 等）向 app-erp-all 落了 `ErpPickerSchemaContractTest`/`ErpFluxDebugInvPickerTest`/`ErpFluxDiffDemoTest` 等非 it 测试类；54/0/0/1 权威计数显式指向 known-good-baselines V.1 行，不构成双真相源，故仅注释性漂移。
- **本轮复核**：`find app-erp-all/src/test/java -name "*.java" -not -path "*/it/*"` = **14 文件**（本轮实数）；runbook 注释仍写 12——**未修复**；follow-up backlog 行在盘（触发条件：下次 runbook 变更时对齐）。
- **维度**：owner-doc 对齐 / 回归风险（watch）。

### [P2] P2-IT-03 mission 命名空间混装——用户直发兄弟计划以 `integration-test` 提交前缀落地，未在 roadmap 状态块登记工作项行
- **一行理由**：`2026-08-24-0900-1`（nop-entropy delVersion 回退 + 双仓重录）、`2026-08-24-1147-1`（flux picker delta 修复）、`2026-08-24-2115-1`（flux queryForm 语义 + G-001 视觉）及其落盘提交（`63781668c`/`b2396d59d`/`9df4862cc`/`10c7190d9`/`ff5bfe9f3`/`e031113f8` 等）均带 integration-test 前缀但非 roadmap 工作项——溯源噪音已实际造成 V.1 边界审计需对 `750577323`/`63781668c` 两提交作「归属他会话/他计划」预先裁决；各计划自身均有独立 plan-audit/closure 与用户来源声明（非无声扩权），故降为登记性/命名一致性问题。
- **本轮复核**：`git log --oneline -30` 前缀混装仍在（历史事实，不可改写）；follow-up backlog 行在盘（触发条件：下一 mission 启动时立约）。
- **维度**：待办或自主权策略漂移。

### [P2] P2-IT-04 V.1「E2E 零回归」为构造性满足（提交枚举证明），Playwright 套件未在 V.1 实跑——watch-only 残余风险
- **一行理由**：V.1 对 M0.x+B1-B10 共 27 提交逐提交 `git show --name-only` 证明零生产零 seed（本轮 B4-B10 六提交抽验佐证成立），但窗口内他计划确有前端生产面变更（flux picker/queryForm/视觉修复），其 E2E 层验证义务委托给各计划自身闭包，全量 Playwright 260+ spec 未在 mission 收口时点整体跑一轮——边界已披露且经 V.1 独立结束审计 PASS，属登记性残余风险，非缺陷。
- **本轮复核**：follow-up backlog 行在盘（触发条件：frontend-ui roadmap 既有节奏或前端面变更再积累时）。
- **维度**：验证充分性。

---

## 跨审计处置（不重复立项，路由状态核验）

姊妹开放式审计（`docs/audits/2026-08-24-2233-open-audit-integration-test.md`，0 P0 / 3 P1 / 3 P2）的三项 P1 为**生产侧缺陷**（发现于本 mission 执行期、被「勘误」口径吸收），不属本 mission 测试交付物缺陷，首轮双审计已作分工归属开放审计。本轮核验其路由状态：

| 发现 | 缺陷 | 路由 | 当前状态 |
|------|------|------|---------|
| OA-01 | 聚合 app 种子缺 notify 模板族（27 行）+ cs-ticket-code 规则 → fresh-DB 下通知子系统静默失活；owner doc 根因误判 | `docs/plans/2026-08-25-0330-1-aggregate-notify-cs-seeds-into-app-init-data.md` | **active，未执行**（未跟踪文件） |
| OA-02 | closePeriod 同事务 FX 凭证未 flush → 损益结转永久缺 FX 腿；fin-service 单测与 C13 集成测钉死**相反**语义 | `docs/plans/2026-08-25-0330-2-fix-closeperiod-fx-flush-profit-loss.md` | **active，未执行**（未跟踪文件） |
| OA-03 | ASN→收货链零 orgId 写入 → 账套解析 null → 过账零凭证 posted 悬挂（已知失败模式 #11 家族「缺 writer」新站点）；C19 以 DAO fixture 掩蔽 | `docs/plans/2026-08-25-0330-3-adjudicate-integration-test-defect-routing.md`（含 C18/C20b 同型分诊 + 勘误→缺陷路由通道立法） | **active，未执行**（未跟踪文件） |

**残余风险声明**：三计划执行前，上述缺陷仍存在于生产代码/种子资产，且黄金路径套件按现状看护（C13 `PL_TOTAL=1130` 不含 FX 腿、C19 fixture 补 orgId）——修复落地时须同步翻转对应断言与快照（该义务已在 0330-2/0330-3 计划内登记）。本审计不将其重复立为 P1 finding 以避免重复 remediation 起草；其 P1 严重性与修复义务以开放审计报告及三份 active 计划为准。OA-04/05/06（P2）与 P2-IT-01..04 同处 follow-up backlog。

**流程注记（非 finding）**：当前工作树中，双审计报告（2 文件）、三份 remediation 计划、roadmap Follow-up Backlog 节均为未提交变更——审计路由证据尚无 git 持久性，依赖 mission-driver 步骤收尾提交；若本步骤提交仅含本审计文件，需确认路由工件不遗失。

---

## 分维度裁决（反窄化：每维度至少一句裁决）

| # | 维度 | 裁决 | 要点 |
|---|------|------|------|
| 1 | **需求正确性** | PASS | mission 目标（app-erp-all 单模块、19 子系统、15-25 用例、每用例 3+ 域 + 审批/过账/状态机、三层全比对、不接 CI）逐项兑现：23 集成类 1:1 落地（本轮实数）、19/19 域 ≥2（Σ=88 本轮复算）、机制 (c) 按选定落地且 M0.2 五风险逐项实证落盘（§3.4）；**本轮活体复跑全绿**（54/0/0/1 @01:35，2026-08-25 04:20）。唯一发现 = P2-IT-01 编号标签。 |
| 2 | **owner-doc 对齐** | PASS（2×P2） | 五处文本计数一致（54/0/0/1、23 类、94 CSV+1SQL、3834/0/0/1/642）：roadmap ↔ 设计文档 ↔ e2e-runbook ↔ seed-data.md（§快照重录义务 :67 本轮确认在位）↔ known-good-baselines V.1 行 ↔ backlog README P8 ↔ index.md:53 双路由（本轮确认在位）；实施期漂移全部以 §6 勘误块登记闭环。发现 = P2-IT-01/02。OA-01 的 owner-doc 根因误判修正已入 0330-1 计划 Goals。 |
| 3 | **架构或边界影响** | PASS | 无新增生产依赖/DAG 边：app-erp-all pom 仅测试态消费；surefire 模块级串行化在位（本轮 pom 通读：`forkCount=1`/`reuseForks=true`/`parallel=none` + 注释援引已知约束 1）；mission 自有提交零 ORM/API/seed 变更（首轮 27 提交全列 + 本轮 6 提交抽验）；跨域实体访问发生在聚合 app 测试层 = module-boundaries 认可的聚合面；保护区域未被本 mission 触碰（0900-1 外部仓库改动双独立子 agent 批准在案、2115-1 用户豁免留痕）。发现 = P2-IT-03（登记性）。 |
| 4 | **验证充分性** | PASS（1×P2 watch + 跨审计在途项） | 每验收标准有独立证明策略且**本轮活体复现**：套件 54/0/0/1 复跑全绿 + compliance 与 §BASELINE 机器可读块逐项零漂移——「如果声称是假的，现在就会红」，它没红。E2E 层构造性边界 = P2-IT-04。OA-01/02/03 的「测试遮蔽生产缺陷」形态已由开放审计立项路由（三计划 active），修复翻转断言义务在案。 |
| 5 | **回归风险** | PASS | 已知脆弱点均有既判处置：日期/时间戳漂移 `*` 通配有先例纪律 + delVersion 重录副作用在 runbook 登记；6 冻结时钟扩展复用 common-test 基建、`it/` 零裸时钟（本轮 grep 实证）；1 类 1 方法 + fresh-DB 每类 1 次 + 串行单 fork 消除竞态；基类 `freshDbForClass` 静态键控在非 it 类穿插执行下仍正确触发。发现 = P2-IT-02（计数注释陈旧，watch）。 |
| 6 | **路由和技能选择正确性** | PASS | 任务路由（测试开发 → `nop-testing`）与 AGENTS.md §任务路由匹配；全部 M0.x/B1-B10/V.x 行内登记 `Skill: nop-testing`；每批「独立 plan-audit → 执行 → 独立 closure audit → 写回 done」闭环在案（本轮抽验 V.1/B5/V.2 计划 Closure 段全在）；0900-1 `Skill: none` 附理由。若换路由（如 nop-backend-dev）无增益——本 mission 零生产业务代码。无发现。 |
| 7 | **待办或自主权策略漂移** | PASS（1×P2） | 声明范围 vs 实际边界：roadmap 声明收口 = M0.x+B1-B10+V.1+V.2，实际全部 done 且独立闭包在案，无未完成项被静默关闭；Deferred 三项（CI 接线/未实现功能用例/monitor 解析限制）分类清晰带 successor 触发条件；seed 修正授权全程未触发（各批自包含建数，基线行逐批登记）。发现 = P2-IT-03（兄弟计划前缀混装，非无声扩权——来源均为用户直发且各有闭包）。 |
| 8 | **ORM 完整性**（项目特定） | N/A 无发现 | mission 零 ORM 变更（首轮全列 + 本轮抽验）；测试经 GraphQL RPC + 实体类只读访问，不触碰 `*.orm.xml`。 |
| 9 | **代码生成纪律**（项目特定） | N/A 无发现 | 无 `_gen/`/`_` 前缀产物手改；构建产物变更（`9df4862cc`）为 0900-1 重录后的再生成追赶，属生成器输出。 |
| 10 | **view.xml gen-control 契约**（项目特定） | N/A（对 roadmap 工作项） | 集成测试 roadmap 工作项不含 delta view 改动；窗口内 gen-control 清理归 1147-1（已过自身双批准与 146 站点契约测试闭包），不属本 mission roadmap 对象。 |

**已知失败模式加权核查**：#9 compliance 基线漂移 → 本轮活跑与 §BASELINE 逐项相等 ✓；#10 closure-pending → roadmap 全部计划闭包证据在案（本轮 3 份抽验）✓；#11 业财过账吞异常悬挂 → 套件以 `posted=true` + 凭证存在性 + 借贷平衡断言跨 12+ 链主动覆盖，其「缺 writer」新控制点（OA-03）已路由 0330-3 ✓；#12 dict 死状态 → 22 用例实走状态机迁移（含负路径守卫断言）✓；#13 arm-index 回填 → 本 mission 无 arm-index finding 所有权，N/A（反向回填通道缺失已由 0330-3 立法承接）。

---

## 结论

**passes multi-dimensional audit**（0 BLOCKER / 0 P0 / 0 P1 / 4 P2）。mission 交付物与声明在代码、配置、测试、公开契约四个面上实质一致；本轮在当前 HEAD 独立复现了两项活体证据（套件 54/0/0/1 @01:35 + compliance 零漂移），并确认姊妹审计 P1 项的路由（三份 0330-* 计划 active）在位。

剩余风险（均 P2，已在 follow-up backlog 登记，勿重复 triage）：
1. P2-IT-01 设计文档 §6 编号标签勘误——下次触碰该文档时顺手修正。
2. P2-IT-02 runbook 基建类计数注释刷新——下次 runbook 变更时对齐实仓 14。
3. P2-IT-03 mission 提交前缀归一约定——下一 mission 启动时立约。
4. P2-IT-04 E2E 全量套件回收窗口——前端生产面变更积累后择机整体跑一轮 Playwright 260+ spec。

在途依赖（非本审计发现，跟踪至其自有计划闭包）：OA-01/02/03 三份 active 计划执行前，对应生产缺陷（种子聚合缺口 / closePeriod FX flush / ASN orgId）持续存在，黄金路径套件按现状看护——修复时须按计划内义务同步翻转 C13/C16/C19 断言与快照并履行双面快照重录。
