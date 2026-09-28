# 性能/UX 收尾技术债与门禁 Roadmap（台账合并 / 门禁脚本 / doc-only 清理）

> 最后更新：2026-09-28
> 来源：性能与 UI/UX 深度优化 mission 收官（分析报告 `docs/analysis/2026-09-27-perf-ux-deep-optimization-analysis.md` + 6 个已完成计划 2026-09-27-0318-1 / 2026-09-27-0325-2 / 2026-09-28-0835-1 / 2026-09-28-0852-2 / 2026-09-28-0906-3 / 2026-09-28-1418-4，提交 f11c3d5ae→f3955fc16）+ 收官增量重扫描结论（无新可优化项，残留全部为触发条件驱动的已登记债）
> 规范：`docs/backlog/00-roadmap-authoring-guide.md`
> Skill: none（roadmap 起草无匹配技能；GATE-xx 脚本实施时参照 `tools/check-hardcoded-cjk.mjs` 先例）
> 关联：`docs/backlog/optimization-audit-roadmap.md`（工作项全部 ready 的深度调研框架，**不被本 roadmap 吞并**；本 roadmap 只收编 perf-ux mission 产生/暴露的债与防复发门禁）

## 1. 目的

mission 收官时遗留三类收尾资产，本 roadmap 将其转为三个可交付工作项：

1. **触发条件驱动的技术债登记表**（§3）——将散落在 6 个计划文件 Deferred But Adjudicated + 分析报告 §6 的 22 项候选 follow-up（20 项已登记，另有 2 项按 §3 排除标准不登记） 合并为单一监控表面。**登记表项有意无状态、不是工作项**（不进 AI 队列）：每项带触发条件与升格形态，触发命中时经 plan-first 升格为 `docs/plans/` 独立计划。
2. **门禁脚本批（GATE-01~04）**——将 mission 反复抓到问题的机制从"审计层人肉发现"转为"常驻脚本拒绝"（lesson 20 先例：机械违规须脚本基线门控而非抽样审计）。覆盖四类已实证的失败模式：XDSL 结构损坏、计划门控勾选失真、验证链 stale（stale jar）、A9 式枚举漏盘。
3. **doc-only 清理批（DOC-01/02）**——mission 期间登记的事实性文档漂移，一个轻量批次出清。

**本 roadmap 不是**一次新的更全面审计：mission 已完成三通道全量分析→6 批次修复→逐批独立审计→收官增量重扫描（结论：无新可优化项）。再开全量审计的边际产出为重发现已裁决债，故本 roadmap 的防复发手段是**脚本门禁 + 触发监控**，替代周期性人工全量审计。

硬约束：不实施登记表中的任何触发驱动项（升格须独立计划）；GATE 脚本不改动既有 validate:flux / checker 的语义，只做叠加断言；DOC 批仅事实性校正，不改业务语义。

## 2. Work Item Status

状态计数（唯一动态状态块）：

| 状态 | 数量 |
| --- | --- |
| todo | 5 |
| ready | 0 |
| done | 1 |

### M1 门禁脚本批（防复发）

| # | Work Item | Status | Owner Doc | Deps | Skill |
| --- | --- | --- | --- | --- | --- |
| GATE-01 | XDSL 结构不变量脚本 `tools/check-xdsl-invariants.mjs`：手写 view.xml/page.xml 断言——form 位于 `<forms>` 段内、`<form id="edit|add">` 每文件 ≤1、无重复 cell id、无 `@[a-zA-Z]+\[\[` 双括号、无 `[=>^]@` 组头误标；接入门禁链（npm script）+ 故障注入自测（对人为损坏 fixture 断言报错）（计划 `2026-09-28-1710-1` completed：草案审查 2 轮收敛 + 独立结束审计 1 轮收敛[Blocker B1 门控预勾回退整改后 ACCEPT]） | done | `docs/architecture/flux-page-export-and-validation.md` | — | none |
| GATE-02 | 计划门控状态检查 `tools/check-plan-gates.mjs`：`docs/plans/*.md` 断言——机器可检不变量取 lessons/25 防御规则 3（结束审计门控 `[x]` ⟹ Closure Audit Evidence 非占位文本）+ `completed` 计划零 `[ ]`；**存量计划按 lesson 20 基线快照豁免**（历史 completed 计划含遗留 `[ ]`，首日全仓断言将大面积误报），细化留 GATE-02 计划。接入 mission 收尾自检 | todo | `docs/plans/00-plan-authoring-and-execution-guide.md` 规则 12 | — | none |
| GATE-03 | 验证链新鲜度包装 `tools/validate-flux-fresh.sh`：串联「触及模块 `mvn install` → `npm run validate:flux`」并断言 `_tmp/flux-page-validation-report.json` mtime 晚于最新触及源文件 mtime + `flux-pages/manifest.json` failedPages=0 与 totals 同时出具（stale jar 三案防线，lessons/26） | todo | 同 GATE-01 | GATE-01 可并行 | none |
| GATE-04 | 无界查询可重放枚举 `tools/enum-unbounded-queries.mjs`：全仓 src/main 枚举 `findAllByQuery`/`findList`/`findAll` 无 `setLimit` 站点 → 输出 `file:line` 清单与内置豁免台账（批次 2 Phase 5 留痕区 + 1418-4 豁免）diff；新增未登记站点即非零退出。使 A9 式枚举漏盘成为一条命令可证伪的命题 | todo | 分析报告 §2.2 A9 行 | — | none |

### M2 doc-only 清理批

| # | Work Item | Status | Owner Doc | Deps | Skill |
| --- | --- | --- | --- | --- | --- |
| DOC-01 | `docs/design/dashboards.md` 两处事实漂移校正：①:76/:266「orderLine.deliveryDate」实为订单头级 `ErpPurOrder.deliveryDate`（`ErpPurOrderLine` 无此字段）；②:263 purchaseAmount 状态轴补全 `approveStatus=APPROVED AND docStatus≠CANCELLED`（现文仅 docStatus='ACTIVE'，外延等价但文本失准） | todo | `docs/design/dashboards.md` | — | none |
| DOC-02 | 「GROUP BY chartId」措辞归一至执行形态「单列投影+内存去重」（草案审查 F-3 实核重定靶点：分析报告 §2.4 **无**该失准表述无需改动；失准措辞位于 1418-4 计划文件 :16 机制先例行/:23 Goals F-4 孪生行/:58 Phase 1 Fix F-4 条目/:62——该计划已 completed，属闭包后化妆品级文本归一；lessons/26 引用系 stale jar 主题与此无关不动） | todo | `docs/plans/2026-09-28-1418-4-a9-addendum-unbounded-queries.md` | — | none |

## 3. 触发条件驱动的技术债登记表（无状态监控表面，非工作项）

> 登记表项**不是工作项**：无状态字段、不进 AI 队列。触发命中时经 plan-first 升格为独立计划（升格后在本节标注日期并移入对应计划的 Deferred 记录）。复核节奏：每次新 mission 启动选工作项前，先扫本表触发条件。
>
> **登记范围排除标准**（草案审查 F-5）：已耗尽的条件分支（如 0906-3 自闭合按钮 drawer 尺寸——机制实证可用后残余分支归零）与非本 mission 性质的常设债（如 0852-2 生产 DDL 迁移——部署面）不登记；0318-1 的 reportCompletion 计划剩余量候选登记为 D-20。

| # | 项 | 来源 | 触发条件 | 升格形态 |
| --- | --- | --- | --- | --- |
| D-01 | fin 现金流量表/账龄报表完整 SQL 聚合重写（A4/A5） | 0325-2 Deferred | 月度凭证量实测 >1 万 或 报表 RT 超标 | plan-first |
| D-02 | 物流/B2B 网关重试移出事务（G2/G3） | 分析报告 §6 | 承运商 5xx 阻塞事务/连接池耗尽暴露 或 owner doc 增补异步重试语义 | plan-first |
| D-03 | fin-posting 热路径 getEntityById（B6） | 分析报告 §6 | 过账 RT profiling 证据成为瓶颈 | plan-first |
| D-04 | 循环 save 批量化（C1，约 60 处） | 分析报告 §6 | 平台批量 API spike 结论落地 或 HR payroll/notify 广播实测超标 | plan-first |
| D-05 | notify 外发通道批量化/异步队列（I2） | 0835-1 Deferred | 广播量级实测 >500 人 或 nop-message 总线接入 | plan-first |
| D-06 | 看板/报表取数合并（qa/mfg/kanban 重页） | 分析报告 §6 | 看板首屏 RT 实测超标 | plan-first（含 api 契约面审批） |
| D-07 | 65 页列裁剪全面收敛/行按钮「更多」归并/列宽配置 | 分析报告 §6 | 逐域 UX 迭代排期 或 flux 列虚拟化行为实证 | plan-first |
| D-08 | 币种→汇率/物料→单位联动、批量驳回、43 页双查询入口收敛 | 分析报告 §6 | `docs/design/<domain>/ui-patterns.md` 增补对应范式 | plan-first |
| D-09 | flux warning 18k 模板级收敛 + duplicate-schema-id | 分析报告 §6 | nop-entropy/nop-chaos-flux 模板迭代 | 跨仓库 plan |
| D-10 | G1 汇率刷新事务外重排/异步化 | 0835-1 Deferred | 真实汇率 provider（exchangerate-host/fixed-fetch）立项 | plan-first |
| D-11 | IErpFinArApItemBiz 聚合接口（sumOpenAmount 类） | 0325-2 Deferred | api.xml 加法变更立项（含保护区双批准） | plan-first |
| D-12 | onTimeRate 聚合重写（消除 5000 截断） | 0325-2 Deferred | 企业量级截断失真暴露 或 owner doc 对截断语义裁决 | plan-first |
| D-13 | 泛型 per-entity status 字段只读化（约 56 文件） | 0906-3 Deferred | ui-patterns 定表单状态字段范式并逐域字典裁决 | plan-first |
| D-14 | batch 按钮命名统一（qa batch-pass vs approve） | 0906-3 watch-only | ui-patterns 定命名范式并排期 E2E 选择器迁移 | plan-first |
| D-15 | html 组件空态支持 | 0906-3 Deferred | flux html 组件支持 empty 配置 | plan-first |
| D-16 | F-1 CS 质量看板三入口 SQL 聚合重写 | 1418-4 Deferred | CS 质量看板 RT 实测超标 或 owner doc 对全量语义裁决 | plan-first |
| D-17 | mnt `loadScheduleIdsWithVisit` 无排序截断（setLimit 5000） | 1418-4 watch-only | visits 实测接近 5000（改 GROUP BY scheduleId 投影，零成本等价修法） | plan-first |
| D-18 | 平台 auth 页渲染失败 2 处（NopAuthLoginAttempt/NopAuthRateLimitCounter，flux-web.xlib 层） | 批次 1 验证期发现 | nop-entropy 侧修复排期 | 跨仓库 |
| D-19 | compliance 基线记账同步（R3/R10/R2c 残余归属 f3.10 预存遗留） | 批次 2 结束审计登记 | 基线裁决 successor 启动 | plan-first |
| D-20 | reportCompletion 默认带出计划剩余量（UX 跟进候选） | 0318-1 Deferred（Successor Required: no） | 确认 findPage gql:selection 含计划/完工数量字段（源裁决触发措辞） | plan-first |


## 4. 执行顺序与完成口径

- **执行顺序**：GATE-01/02/03/04 互不依赖可并行，编号仅为引用锚点；DOC-01/02 独立，可随时插入。
- **完成口径**：全部工作项 done + §3 登记表在案 + 本 roadmap 状态块归零（todo=0）。登记表本身永久存续（触发驱动，无"完成"概念）。
- 每个工作项实施前须形成 `docs/plans/` 独立计划并通过独立草案审查（`todo → ready`）；独立结束审计通过后方可 `ready → done`。

## 5. 非目标

- 不做新的全量审计（mission 已完成三通道分析+收官重扫描，边际产出为重发现已裁决债）。
- 不实施 §3 登记表中的任何项（升格须独立计划）。
- 不吞并/关闭 `optimization-audit-roadmap.md`。
- GATE 脚本不修改既有 validate:flux / checker 语义（只叠加断言与包装）。
