# 开放式审计报告：id-string-migration mission

> Audit Status: closed（P1 ×3 修复义务已由 plan `docs/plans/2026-08-23-1752-1-id-string-mission-closure-backfill.md` 兑付；P2 ×4 已登记 roadmap `## Follow-up Backlog` 节）
> Audit Type: open-ended
> Mission: id-string-migration

- 审计日期：2026-08-23（mission 驱动时间戳 2026-08-22-0702，落盘于 mission 收官后；与同驱动 `2026-08-22-0702-multi-audit-id-string-migration.md` 为**独立并审**——本报告全部证据为本会话独立重采，未转录多维修计结论）
- 审计对象：`id-string-migration` mission 整件工作——19 域 1662 列（PK 477 + BIGINT FK 1185）`stdDataType long→string` 迁移 M0-M4 全链（代码、配置、测试、E2E、公共契约、文档终态、流程合规）
- 审计方法：按 `docs/skills/open-ended-audit-prompt.md`（已注入 `docs/skills/README.md §项目定制化层`：保护区域 / 验证命令 / 命名约定 / 已知失败模式 13 项）执行项目级对抗搜索，重点覆盖标准清单外维度：语义陷阱 grep 全量重扫、E2E 修复形态完备性、契约模板复活面、状态回填链、承诺-证据一致性
- 输入：`AGENTS.md`、`docs/backlog/id-string-migration-roadmap.md`（规则 1-8 + Draft Review Record）、M0/M0.2 审计工件、登记册 `tools/id-migration-registry.json5`、19 份域 plan + 3 份 M4.1 批内 plan、`docs/testing/known-good-baselines.md`、`docs/audits/compliance-baseline.md`、`docs/testing/e2e-runbook.md`、`docs/logs/2026/08-23.md`、live 代码/测试/E2E/config

## 0. 独立复跑证据（本审计会话 live 执行）

| 验证项 | 命令/方法 | 结果 |
| --- | --- | --- |
| fail-closed 扫描 | `node tools/check-bigint-id-types.mjs scan` | PK 477 / FK 1227 / BIGINT 主外键 1662；实际修改 0、DEFERRED 0、告警 0、BLOCKER 0（与 roadmap 终态一致） |
| orm 残留 long 列 | scan 输出逐域核对 | 残留仅 delVersion / 孤儿操作人列 / fileSize / durationMs 等非 PK/FK 列——与规则 4 排除面吻合 |
| 登记册终态 | JSON5 解析 + grep | 259 条全 `retired`、0 active；头部终态注记在案 |
| compliance 门控 | `bash docs/audits/nop-compliance-checker.sh` | **exit=0**（live 复跑）；R2c 基线表 = 1505 与回写后一致 |
| Java 契约抽查 | `_ErpPurOrder.java:1246` | `public final java.lang.String getId()` —— 实体 id String 落地 |
| stub 与权威源一致性 | `module-finance/model/app-erp-finance.orm.xml:2245` | ErpMdSubject stub id = `stdDataType="string"`（与 md 权威源一致，对称胶水失败模式已消灭） |
| M1.3 兑付 | `TestErpOrgIsolation.java:72` | `setCurrentOrgId(ctx, "2")` —— M2.1 successor 承诺已兑付；common-service org 三文件 `Long` 仅存 2 处 Javadoc 历史注记（计划 sanctioned 例外） |
| 语义陷阱 grep（main，全量非抽样） | `ConvertHelper.toLong` / `Long.parseLong` / `.longValue()` / `Map<Long` / `String.format("%d` / `getId()==` / `compareTo` / `comparing(::getId)` | idOrder 数值序族（`Long.compare(toLong,…)` + `Long.MAX_VALUE` 哨兵）合法；`parseLong`/`longValue` 残留全为 epoch 日期 parse、计数聚合、`Number` 拆箱（合法非 id）；`Map<Long` main 零命中（1 处 Javadoc）；`getId()==` 全为 null 检查；`%d` 仅 fiscalYear/seq；`comparing(::getId)` 裸字典序**零命中**；`eq("…Id", toLong(…))` 过滤值桥**零残留** |
| 测试层 | `_cases` json5 数字 id（308 文件） | `"id": <数字>` 零命中 |
| page.yaml | `:Long` 全仓 grep | 零命中（module web + app-erp-all） |
| E2E 残留 | `Number(<id>)` / eqFilter Number 包装 / parseInt(id) | id 族零残留；剩余 `Number(` 抽查全为金额/计数/时长（totalCostAmount、settledAmount、leadCount 等）；`eqFilter('id'` 存留位点值侧全 String 直传 |
| `_tmp` 终态 | `ls _tmp` | `bigint-id-string-fix/` 不存在；e2e-id-repair / m41-compliance 证据存档在位 |
| 流程合规 | 19 份域 plan 批准记录 grep + M2.5 抽查 | 每份 plan 含保护区域双独立子 agent 批准记录（M2.5 治理 ses_fd7013b03ffe + 技术 ses_fd70168fcffe 双 `approve` 落盘 Draft Review Record，:156-158）；M4.1 三计划独立结束审计证据在案 |
| 文档/日志收尾 | §16A / bug docs / logs / runbook / baselines | §16A.1/16A.3/16A.4 修正 + 历史化清零在案；8 份 bug doc 在位；`docs/logs/2026/08-23.md` 三条目含验证状态；runbook :220 计数 2026-08-23 刷新；known-good-baselines 四源证据链条目在案 |
| git 状态 | `git status --porcelain` | 工作树仅 1 个未跟踪审计文件（同驱动多维修计），零生产代码漂移 |

---

## 1. 发现（按严重性排序）

### [P1] 1. backlog 登记册行未回填：mission 已完成但 `docs/backlog/README.md` 行仍 `todo` + 废止计数 1605

**理由：状态回填是闭合义务的一部分（已知失败模式 11 同型），`todo` 状态可直接诱导后续会话对已完成 mission 重复立项。**

`docs/backlog/README.md:70`：

> `| P0 | 主键/外键 stdDataType string 化迁移（全 19 域 1605 列 PK/FK long→string…） | id-string-migration-roadmap.md | todo | plan-first（ORM 保护区域…） |`

- **本会话独立验证**：roadmap 头部「MISSION 完成 🎉 M0-M4 全 done」+ 依赖图全 done + 259/259 retired；README 同表内已完成 mission 惯例标 `✅ done`（30+ 行）；行内 1605 为 08-16 废止口径（权威 1662 = M0 审计 §1 + 本审计 live scan）。
- **影响**：`AGENTS.md` 快速路由「选择下一个工作项 → `docs/backlog/README.md`」——一个 P0 优先级、`todo` 状态的 19 域迁移行会误导后续会话重新规划整个 mission。
- **修复方向**：状态改 `✅ done` + 计数对齐 1662（或注明权威口径见 roadmap）。
- **注**：同驱动多维修计（未跟踪文件）finding 1 已报告同一问题，截至本审计落盘时点**仍未修复**——两次审计之间零兑付，佐证该义务需要落进待办而非仅存于审计文本。

### [P1] 2. 架构 owner-doc 契约模板漂移：`data-dependency-matrix.md` §5.6.4(b) 仍教 `stdDataType="long"` 外部实体 stub id 声明

**理由：该节是新增跨域引用的复制粘贴模板，按模板执行会重新引入 mission 刚消灭的 stub↔权威源类型错配（`_gen` to-one 胶水对称编译破坏）失败模式。**

`docs/architecture/data-dependency-matrix.md:632`：

```xml
<column name="id" code="ID" stdSqlType="BIGINT" primary="true" stdDataType="long"/>
```

- **本会话独立验证**：实况权威源已全量翻转——`module-finance/model/app-erp-finance.orm.xml:2245` 同名 stub（ErpMdSubject）现为 `stdDataType="string"`；19 orm 残留 long 列全为非 PK/FK（§0 表）。M0 审计 §10.1 证明「stub 声明与权威源 stdDataType 不一致 → 对称编译级破坏、唯两端同时 String 才自愈」是本 mission 的核心成本来源，而 §5.6.4(b) 正是未来新跨域引用的**规范入口文档**。
- **影响**：未来任何域按此模板新增外部实体引用 → Long-id stub ↔ String-id 权威实体错配 → 编译破坏回归。M4.1 文档收尾清单（§16A / orm-model-design / 登记册 / 基线 / 依赖图）**漏了本架构 owner doc**。
- **修复方向**：示例 id 列改 `stdDataType="string"` + 一行注记（Java 层 String / DB 层 BIGINT，方案 B）。
- **注**：同驱动多维修计 finding 2 同题，仍未修复。

### [P1] 3. 承诺的 follow-up 无登记实体：「孤儿操作人列建模问题已登记 follow-up，另案裁决」在全 docs 树无对应工作项

**理由：「已登记」声明不被证据支持，建模债实际无主、无触发条件、不可路由——「承诺但没有证据」型缺口。**

`docs/backlog/id-string-migration-roadmap.md:15`：

> 未分类 BIGINT 列 368（…孤儿操作人列等）非主键非外键，不在本 mission 范围、保持 long（**孤儿操作人列建模问题已登记 follow-up，另案裁决**）。

- **本会话独立验证**：全 `docs/` 树 grep「孤儿操作人」——命中仅 roadmap 自身 + 各域 plan 规则 4 执行记录 + 日志 + 审计文件；`docs/backlog/README.md`、其他 roadmap、`docs/discussions/`、`docs/analysis/` **零登记**。约 15 个操作人列（completedBy/assignedTo/verificationPerson/signedBy/responsiblePerson/resolvedBy/requestedBy/reconciledBy/acceptedBy）在 String-id 世界保持 Long，且已产生运行时桥产物（如 `ErpMntVisitReportAdditionalFaultProcessor.java:79-88` `toLongUserId` try-catch 桥 + `ErpMntVisitReportAdditionalFaultProcessor:68-71` assignedTo 回退）。范围排除本身合法（规则 4 + 工具防御性限定，执行正确）。
- **修复方向**：在 `docs/backlog/README.md` 补一行 watch/follow-up 工作项（含触发条件：操作人列需 FK 化/审计实体化时），或将 roadmap 措辞降级为「建议未来登记」。
- **注**：同驱动多维修计 finding 3 同题，仍未修复。

### [P2] 4. E2E 测试层 TS 类型声明未随迁移 String 化：49 处 `id: number` / `xxxId: number` 陈旧注解散布 22 文件（**本审计新发现，超出同驱动多维修计发现集**）

**理由：类型注解为编译期文档、运行时被擦除（playwright 转译不类型检查），当前 E2E 全绿零行为影响；但声明的契约与 String API 实态相悖，会误导后续测试作者写数值断言/算术。**

- **证据**（本会话 grep `id: number` / `xxxId: number`，tests/e2e）：**49 处 / 22 文件**。样例：
  - `tests/e2e/business-actions/f13-kanban-drag.action.spec.ts:23` `interface Task { id: number; … }`——同文件 :41-74 GraphQL 已是 `$id:String`，运行时 `todo.id` 为 String 直传，注解与实态矛盾；
  - `tests/e2e/business-actions/sal-return-exchange.action.spec.ts:90,113,129` `findItems<{ id: number; … }>` / `exchangeReturnId: number`；
  - `tests/e2e/business-actions/notify-inbox.action.spec.ts:95,119,235` 过渡形态 `id: number | string`；
  - 其余分布 orchestration/_helper.ts、negative/e2-2、e2-3、mfg-genealogy、fin/pur/drp/ct/b2b/hr 等 19 文件。
- **根因**：M4.1 计划 2 的「三形态」修复面（`Number()` 解包 / 字面量数字 id / eqFilter+`:Long` 变量声明）**未枚举 TS interface 字段注解形态**——880 处修复后该形态成为系统性残留。
- **修复方向**：批量 `id: number` → `id: string`（`xxxId: number` 同理，保留 assignedTo 等规则 4 Long 保留列例外），与既有 `DUMMY_ID` 类常量清理同批顺带完成。

### [P2] 5. roadmap 内部计数不一致：`目的` 节 368 vs `当前基线` 节 380（工具权威 380）

**理由：同文件两口径并存属行级文书漂移，不影响行为与路由。**

`id-string-migration-roadmap.md:15`「未分类 BIGINT 列 368」与同文件 `:87`「未分类 BIGINT 共 380」并存；M0 审计 §1 与本审计 live 工具输出均为 380。

### [P2] 6. roadmap `现状` 行未注记 `_tmp/bigint-id-string-fix/` 已清除

**理由：历史快照行缺终态注记，可能误导检索者寻找已不存在的时点副本；单行文书抛光。**

`id-string-migration-roadmap.md:5` 仍陈述「副本在 `_tmp/bigint-id-string-fix/`（08-21 全量刷新…）」，本审计 live 验证该目录已按 M4.1 计划 3 清除（`ls _tmp` 无此目录）。

### [P2] 7. mission JSON description 保留废止口径与被 D3 修订超越的 verify 措辞

**理由：驱动器元数据为历史文本、全部 mission JSON 均无 status 字段（仓库惯例），无路由后果；纯文字陈旧。**

`missions/id-string-migration.json:3` 含 08-16 废止计数（1605/463/1142/1182/40/368）及「域级 `-am` build verify」表述（已被 M0 裁决 D3 修订为「7 模块显式列表 no-am」口径，roadmap 规则 3 为权威）。

---

## 2. 反窄化自检与维度扫描

本审计未止于核实 roadmap/plan 声明（那是窄结构化审计的对象），而是把视野拉到项目级：跨工件的虚假关闭、owner-doc 整体差距、契约复活面、E2E 修复形态完备性、保护区域渗漏、状态回填链。

| 维度 | 裁决 | 要点 |
| --- | --- | --- |
| 需求正确性 | **通过** | 「主键和外键数据类型全部改成 string」→ Java/GraphQL/前端层全 String（live scan 0 残留 + 实体 getId String + stub 一致性抽查）+ DB 层 BIGINT 零改动 + 非 BIGINT FK 42 列零误改 + 380 非 PK/FK 列合法排除（规则 4）。 |
| 语义陷阱横切（编译器盲区） | **通过** | §0 全量 grep：装箱 == / parseLong / longValue / Map<Long / %d / 字典序比较 / eq 过滤值桥在 main 全零或合法非 id；idOrder 数值序范式统一收口。 |
| E2E 修复完备性 | **通过（P2-4 为形态枚举缺口）** | 主门 564/30/8 全数裁决 + examples 16/2/0；30 失败 = 13 白名单维持 + 14 08-11 漏记同源（2 bug doc）+ 3 产品缺陷（2 bug doc）；「三形态」外存在第 4 形态（TS 类型注解，发现 4）。 |
| owner-doc 对齐 | **needs fix（发现 2）** | §16A 三处修正、orm-model-design 方案 B 注记（nop-entropy :208 live 验证）、各域 owner doc 注记在案；唯 `data-dependency-matrix.md` 模板漂移（发现 2）。 |
| 状态回填链 | **needs fix（发现 1/3）** | roadmap 依赖图/工作项表/登记册/基线互证一致；但 backlog README 行（发现 1）与孤儿列 follow-up 实体（发现 3）缺位——失败模式 11 在 backlog 载体的变体复发。 |
| 保护区域纪律 | **通过** | 19 域 plan 双独立子 agent 批准记录全部在案（M2.5 全文抽查 + 其余 grep 计数）；`_gen` 零手改（全量构建 156 模块绿为机制证明）；会计/财务域变更均附 owner doc + tests + 双批准。 |
| 验证充分性 | **通过** | 四源证据链（build 156 / test 3808-0-0-1 / E2E 564-30-8 / compliance exit=0）落盘且本审计 live 复现 scan + checker + 关键 grep；基线后零生产代码漂移（git log + status）。 |
| 回归风险 | **通过（含 watch）** | 快照 String 形态重录全量（`_cases` 数字 id 零残留）；FIFO 负行哨兵 `"0"`/`negativeIdOf` 语义等价收口；watch 项见 §4。 |

### 已知失败模式加权检查（skills README §已知失败模式 1-13）

| # | 模式 | 结果 |
| --- | --- | --- |
| 1-8 | ORM/Java 微模式 | compliance R 规则 exit=0；`@Inject private`/异常基类/`==` 比较/propId 断续全清 —— **全清** |
| 9 | compliance 基线漂移 | 本审计 live 复跑 exit=0、R2c=1505 与基线双处一致 —— **无漂移** |
| 10 | closure-pending | 19 域 + M0.1/M0.2/M4.1 计划独立结束审计证据抽查在案 —— **无欠账** |
| 11 | 状态不回填 | **命中变体**（发现 1：backlog README 行；发现 3 为其孪生——承诺登记未落实体） |
| 12/13 | dict 死状态 / arm-index | 本 mission 未触碰 —— 不适用 |

---

## 3. 结论

**passes open-ended audit（无 P0 阻塞发现）。** mission 交付本体——19 域 1662 列 Java 层全 String / DB 层 BIGINT 保持、契约（实体/IBiz/xmeta/GraphQL/E2E）端到端 String、登记册 259/259 retired、语义陷阱横切清零、四源验证证据链——经本审计会话独立复跑全部成立；保护区域双批准与结束审计纪律完整。

**修复义务（P1 ×3，须修复；与同驱动多维修计的 P1 集合相同——两次审计间零兑付，本审计独立复核确认仍然成立）：**

1. `docs/backlog/README.md:70` 行回填 `✅ done` + 计数对齐 1662（发现 1）；
2. `docs/architecture/data-dependency-matrix.md:632` §5.6.4(b) 模板 String 化 + 方案 B 注记（发现 2，防失败模式经文档复活）；
3. 孤儿操作人列 follow-up 实体化登记（backlog 行 + 触发条件）或 roadmap 措辞降级（发现 3）。

**P2 ×4（发现 4/5/6/7）**为文书/类型注解抛光，归后续 backlog 顺带清理，不单独构成修复计划（发现 4 建议与下一轮 E2E 维护同批）。

### 剩余未知数（watch，非缺陷）

- **id 非数字串假设**：idOrder/negativeIdOf 族依赖 `ConvertHelper.toLong(id)`（`stringToLong` 对非数字串**直接抛 NopException** 而非返回 null）——seq-default 数字序列 + 数字 CSV 种子下恒成立；若未来引入非数字显式 id（UUID 等），排序/取负/Map 键路径将运行时异常。引入前需先废数值序范式（属设计变更，应走 plan）。
- **visual/ 像素套件**：M4.1 计划 2 修复了 visual 5 文件的 id 形态但按既有基线口径未运行（adjudicated Deferred，所有权 plan `2026-07-17-2010-2`）；String id 在 AMIS 文本渲染与数字同形，像素基线预期不受影响，但未经本次回归证明。
- **E2E 白名单/bug successor**：cross-repo flux 对话框渲染缺口（crud 3 + examples 2）、reports 下载按钮/ar-ap-aging、ct FNPT 死锁、drp 双段推进等产品缺陷均已有 bug doc + successor，属 mission 外义务，等待各自 successor 消化。

> Audit Status: closed（P1 ×3 修复义务已由 plan `docs/plans/2026-08-23-1752-1-id-string-mission-closure-backfill.md` 兑付——发现 1 → Fix 1（README:70 `✅ done` + 计数 1662）、发现 2 → Fix 2（§5.6.4(b) `stdDataType="string"` + 方案 B 注记）、发现 3 → Fix 3（孤儿操作人列 backlog 登记行含触发条件）；P2 ×4 维持 roadmap `## Follow-up Backlog` 登记）
