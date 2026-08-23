# 多维审计报告：id-string-migration mission（全链终态）

> Audit Status: closed（P1 ×3 修复义务已由 plan `docs/plans/2026-08-23-1752-1-id-string-mission-closure-backfill.md` 兑付；P2 ×3 已登记 roadmap `## Follow-up Backlog` 节）
> Audit Type: multi-dimensional
> Mission: id-string-migration

- 审计日期：2026-08-23（mission 驱动时间戳 2026-08-22-0702，落盘于 mission 收官后）
- 审计对象：`id-string-migration` mission **整件工作**——19 域 1662 列（PK 477 + BIGINT FK 1185）`stdDataType long→string` 迁移 + M0 冻结/登记册机制 + M4.1 全量收尾（代码、配置、测试、公共契约/导出面、文档终态）
- 审计方法：按 `docs/skills/multi-dimensional-audit-prompt.md`（已注入 `docs/skills/README.md §项目定制化层`：保护区域/验证命令/命名约定/已知失败模式 13 项）执行 7 默认维度 + 3 项目特定维度，含 live 独立复跑
- 输入：`docs/backlog/id-string-migration-roadmap.md`（含规则 1-8 + Draft Review Record）、M0 审计 `2026-08-21-1045`、登记册 `tools/id-migration-registry.json5`、M4.1 三计划（`2026-08-23-0434-1/2/3`）、`docs/testing/known-good-baselines.md`、`docs/audits/compliance-baseline.md`、`docs/design/domain-design-guidelines.md §16A`、`../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md`

## 0. 独立复跑证据（审计会话 live 执行，非转录）

| 验证项 | 命令/方法 | 结果 |
| --- | --- | --- |
| fail-closed 扫描 | `node tools/check-bigint-id-types.mjs scan` | PK 477 / FK 1227 / BIGINT 主外键 1662；**实际修改 0、DEFERRED 0、告警 0、BLOCKER 0**（与 roadmap 终态声明一致） |
| 登记册终态 | JSON5 解析 `tools/id-migration-registry.json5` | **259 entries 全 `retired`、0 active**（orm-deferral 8 + service-bridge 128 + backward-pointer 123，与 M4.1 计划 3 声明一致） |
| compliance 门控 | `bash docs/audits/nop-compliance-checker.sh` | **exit=0**；R2c actual=1505 = 更新后基线，其余规则 0 命中或达标（零漂移复现） |
| seq-string Proof | `mvn test -pl module-common-test` | `TestSeqStringIdProof` **4/4 绿**（独立复现 M0.1 Proof） |
| 已迁移域抽查 | `mvn test -pl module-notify/erp-notify-service` | **23/23 绿**（6 测试类全过） |
| 公共契约抽查 | `_ErpPurOrder.java` / `_ErpPurOrder.xmeta` | 生成实体 `getId()` 返回 `java.lang.String`；xmeta `id` prop `<schema type="java.lang.String"/>`（GraphQL/前端面 String） |
| 残留 grep | page.yaml `:Long`、`Map<Long`（main）、`getId() ==`（main）、`_cases` json5 `"id": <数字>`、E2E `Number(lnk.voucherId)` | **全部 0**；E2E 剩余 `Number(` 265 处抽样全为金额/时长/计数等数值字段断言（非 id 强转） |
| orm 残留 long 列 | 全 19 orm `stdDataType="long"` 枚举 | 380 列 = delVersion ×363 + 孤儿操作人列（completedBy/assignedTo/verificationPerson/signedBy/responsiblePerson/resolvedBy/requestedBy/reconciledBy/acceptedBy）+ fileSize/durationMs——与规则 4 排除面精确吻合 |
| git 状态 | `git status --porcelain` = 0；基线运行后仅 2 个 docs/tools-only 提交（`91d6b5c1d`/`c388cd25c`） | 工作树干净，**记录基线后零生产代码漂移** |
| `_tmp` 终态 | `_tmp/bigint-id-string-fix/` 不存在（已清除）；`_tmp/m41-compliance/` 证据存档 4 文件在位 | 与 M4.1 计划 3 声明一致 |
| 跨仓注记 | `../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md:208` | 方案 B 落地实证注记（19 域 1662 列）在案 |

---

## 1. 发现（按严重性排序）

### [P1] 1. backlog 登记册行未回填：mission 完成但 README 行仍 `todo` + 废止计数 —— 违反登记惯例与「让状态过时」反模式（lesson 11 同型）

`docs/backlog/README.md:70`：

> `| P0 | 主键/外键 stdDataType string 化迁移（全 19 域 1605 列 PK/FK long→string…） | id-string-migration-roadmap.md | todo | plan-first |`

- **证据**：roadmap 头部「MISSION 完成 🎉 M0-M4 全 done」+ 依赖图全 done；README 同表内全部已完成 mission 惯例标 `✅ done`（crud/core-business/extended 等 30+ 行）；行内计数 1605 为 08-16 废止口径（权威 1662，见 M0 审计 §1）。
- **影响**：`AGENTS.md` 快速路由把「选择下一个工作项」指向 `docs/backlog/README.md`——一个 P0 优先级、`todo` 状态的 19 域迁移行会诱导后续会话重复立项/重复规划整个 mission。`00-roadmap-authoring-guide.md §Anti-Patterns` 明列「让状态过时」。
- **修复方向**：该行状态改 `✅ done`，描述计数对齐 1662（或直接注明权威口径见 roadmap）。

### [P1] 2. 架构 owner-doc 契约漂移：`data-dependency-matrix.md` §5.6.4(b) 标准代码模式仍教 `stdDataType="long"` 外部实体 id 声明

`docs/architecture/data-dependency-matrix.md:632`（§5.6.4 标准代码模式 (b)，notGenCode 外部实体声明模板）：

```xml
<column name="id" code="ID" stdSqlType="BIGINT" primary="true" stdDataType="long"/>
```

- **证据**：实况权威源已全量翻转——如 `module-finance/model/app-erp-finance.orm.xml:2245` 同一 stub（ErpMdSubject）现为 `stdDataType="string"`；19 orm 剩余 long 列 380 全为非 PK/FK（见 §0 表）。M0 审计 §10.1 已证明「stub 声明与权威源 stdDataType 不一致 → `_gen` to-one 关系胶水对称编译级破坏」是本 mission 消灭的核心失败模式，而本节文档正是新跨域引用的**复制粘贴模板**。
- **影响**：未来任何域按此模板新增外部实体引用，会重新引入 Long-id stub ↔ String-id 权威实体的类型错配（mission 消灭的模式经文档复活）。M4.1 文档收尾清单覆盖了 §16A/orm-model-design/登记册/基线，**漏了本架构 owner doc**。
- **修复方向**：§5.6.4(b) 示例 id 列改 `stdDataType="string"` 并加一行注记（Java 层 String / DB 层 BIGINT，方案 B）。

### [P1] 3. 承诺的 follow-up 无登记实体：「孤儿操作人列建模问题已登记 follow-up，另案裁决」无对应工作项

`docs/backlog/id-string-migration-roadmap.md:15`：

> 未分类 BIGINT 列 368（…孤儿操作人列等）非主键非外键，不在本 mission 范围、保持 long（**孤儿操作人列建模问题已登记 follow-up，另案裁决**）。

- **证据**：全 `docs/` 树 grep「孤儿操作人」——命中仅 roadmap 自身 + 各域 plan 的规则 4 执行记录 + 日志；`docs/backlog/README.md`、其他 roadmap、`docs/discussions/`、`docs/analysis/` **零登记**。约 15 个操作人列（completedBy ×4 / assignedTo ×3 / verificationPerson / signedBy / responsiblePerson / resolvedBy / requestedBy / reconciledBy / acceptedBy）在 String-id 世界里保持 Long（如 `ErpMntVisitReportAdditionalFaultProcessor:84` `toLongUserId` 桥即为其产物）。
- **影响**：范围排除本身合法（规则 4 + 工具防御性限定，执行正确）；但「已登记」是不被证据支持的声明——该建模债实际处于**无主、无触发条件、不可路由**状态，属「承诺但没有证据」型缺口。
- **修复方向**：在 `docs/backlog/README.md`（或合适 roadmap）补一行 watch/follow-up 工作项（含触发条件：如操作人列需要 FK 化/审计实体化时），或将 roadmap 措辞降级为「建议未来登记」。

### [P2] 4. roadmap 内部计数不一致：`目的` 节 368 vs `当前基线` 节 380（工具权威 380）

`id-string-migration-roadmap.md:15`「未分类 BIGINT 列 368」与同文件 `:87`「未分类 BIGINT 共 380」并存；M0 审计 §1 与 live 工具输出均为 380。行级文书漂移，不影响行为。

### [P2] 5. roadmap `现状` 行未注记 `_tmp/bigint-id-string-fix/` 已清除

`id-string-migration-roadmap.md:5` 仍陈述「副本在 `_tmp/bigint-id-string-fix/`（08-21 全量刷新…）」，而 M4.1 计划 3 已清除该目录（live 验证不存在）。历史快照行缺终态注记，可能误导检索者寻找已不存在的副本。

### [P2] 6. mission JSON description 保留废止口径与被 D3 修订超越的 verify 措辞

`missions/id-string-migration.json` description 含 08-16 废止计数（1605/463/1142/1182/40/368）及「域级 `-am` build verify」表述（已被 M0 裁决 D3 修订为 7 模块显式列表 no-am 口径）。驱动器元数据为历史文本，且全部 mission JSON 均无 status 字段（惯例如此），故仅文字陈旧，无路由后果。

---

## 2. 分维度裁决（反窄化自检：每维度一句裁决）

| 维度 | 裁决 | 要点 |
| --- | --- | --- |
| **需求正确性** | **通过（发现 3/4/6 为边界文书）** | 用户请求「主键和外键数据类型全部改成 string」→ 交付 = Java/GraphQL/前端层全 String（1662 列 live 复扫 0 残留 + 实体/xmeta 抽查 String）+ DB 层 BIGINT 零改动（stdSqlType 保全）+ 非 BIGINT FK 42 列本就 string 零误改 + 380 非 PK/FK 列合法排除。需求-交付无偏移。 |
| **owner-doc 对齐** | **needs fix（发现 2）** | §16A.1/16A.3/16A.4 三处修正落盘 ✓、nop-entropy 方案 B 注记 ✓、各域 owner doc 注记（crm/cs/mnt/logistics）✓；但 `data-dependency-matrix.md` 契约模板漂移（发现 2）。 |
| **架构或边界影响** | **通过（发现 2 属文档面）** | 零新增跨模块依赖边/模块数（156 不变）；DAG 与 module-boundaries 未被突破；api 模块保持全生成件零手写；登记册 259/259 retired 无悬挂桥。类型迁移未引入边界变化，唯一漂移是文档模板（发现 2）。 |
| **验证充分性** | **通过** | 四源证据链（build 156 模块 / test 3808-0-0-1 / E2E 564-30-8 全数裁决 / compliance 零漂移）落盘 `known-good-baselines.md`；审计会话独立复现 scan/checker/Proof/notify 域测试全绿；基线后仅 docs-only 提交，无代码漂移。E2E 30 失败全数裁决（08-11 白名单 13 + reports 14 已立 bug doc 含 successor + 3 复核），零未决。 |
| **回归风险** | **通过（含 watch 注记）** | 字典序陷阱已按 idOrder 数值序范式统一收口（`Long.compare(ConvertHelper.toLong(id),…)` 族，null 经 `Long.MAX_VALUE` 哨兵）；FIFO 负行 ID `"-"` 前缀哨兵语义等价；快照全量重录 String 形态。Watch：idOrder 范式隐含「id 恒为可解析数字串」假设——seq-default 生成与 CSV 种子数字 id 下成立；若未来引入非数字显式 id（如 UUID），`ConvertHelper.toLong` 返回 null 将在拆箱处 NPE。属边界假设而非当前缺陷，登记观感即可。 |
| **路由和技能选择正确性** | **通过** | 各域 plan 记录 `Skill:` 行（抽查 M2.5/M4.1 计划 1/2 均在）；roadmap §预期技能映射兑现（M0 双审计技能、域迁移 nop-backend-dev/nop-testing、M4.1 nop-testing + compliance 裁决）；ORM 保护区域逐域双独立子 agent 批准记录落盘 Draft Review Record（M3.4/M3.7/M3.10 抽查在案）。 |
| **待办或自主权策略漂移** | **needs fix（发现 1/3）** | 范围无声扩大：无（380 列排除面与登记一致）；阻塞降级：无（E2E 失败逐条裁决 + bug successor）；但 backlog 登记状态未回填（发现 1）+ follow-up 声明无实体（发现 3）。M4.1 Deferred 两项均为起草期裁定 watch-only（触发条件登记），诚实移交。 |
| **ORM 完整性**（项目特定） | **通过** | propId 连续性经平台构建 + 工具校验；表前缀零改动（tableName 未动，lesson 01 不适用）；dict 绑定面未触碰；380 long 残留列枚举与排除清单精确吻合。 |
| **代码生成纪律**（项目特定） | **通过** | `_gen` 全经 codegen 重生成（mission 中间态自愈机制依赖此）；api 模块零手写（compliance R 规则 + 抽查）；`_tmp` 副本机制按 Decision A 时点 dry-run + 新鲜度门控执行，终态清除。 |
| **view.xml gen-control 契约**（项目特定） | **本维度无发现** | mission 未触碰手写 view gen-control `<c:script>`（各域「手写 view 零被动变更」声明 + 变更面仅 page.yaml 主动 Fix）；badge 调色板/dict 真值面不在本 mission 变更集内。 |

### 已知失败模式加权检查（skills README §已知失败模式 1-13）

| # | 模式 | 结果 |
| --- | --- | --- |
| 1-8（ORM/Java 微模式） | 前缀双拼/updateEntity 越权/now()/异常基类/@Inject private/字符串 ==/propId 断续 | compliance checker R1b/R4/R5/R7 = 0；`getId() ==` main 0；scan 0 告警 —— **全清** |
| 9 | compliance 基线漂移 | M4.1 计划 3 复跑 + 本审计复跑 exit=0、R2c=1505 与基线双处一致 —— **无漂移** |
| 10 | closure-pending | M0.1/M0.2/M4.1 三计划独立结束审计在案（含 live 复核记录）；各域 plan 状态记录含审计指针 —— **无欠账**（域级 plan 抽查） |
| 11 | 状态不回填 | **命中变体**：backlog README 行未回填（发现 1）——本次发生在 backlog 登记册而非 arm-index |
| 12/13 | dict 死状态 / arm-index | 本 mission 未触碰 dict setStatus 与 arm-index 行 —— 不适用 |

---

## 3. 结论

**passes multi-dimensional audit**（无 P0 阻塞发现）。mission 交付本体——19 域 1662 列 Java 层全 String / DB 层 BIGINT 保持、公共契约（实体/IBiz/xmeta/GraphQL/前端）端到端 String、登记册 259/259 retired、四源验证证据链——经本审计会话独立复跑全部成立。

**剩余风险与修复义务（P1 ×3，均非运行时阻塞，属登记/文档收尾缺口）**：

1. backlog README 行回填 `✅ done` + 计数对齐（发现 1）；
2. `data-dependency-matrix.md` §5.6.4(b) 模板 String 化（发现 2，防 mission 消灭的失败模式经文档复活）；
3. 孤儿操作人列 follow-up 实体化登记或措辞降级（发现 3）。

P2 ×3（发现 4/5/6）为文书抛光，归后续 backlog 顺带清理，不单独构成修复计划。

> Audit Status: closed（P1 ×3 修复义务已由 plan `docs/plans/2026-08-23-1752-1-id-string-mission-closure-backfill.md` 兑付——发现 1 → Fix 1（README:70 `✅ done` + 计数 1662）、发现 2 → Fix 2（§5.6.4(b) `stdDataType="string"` + 方案 B 注记）、发现 3 → Fix 3（孤儿操作人列 backlog 登记行含触发条件）；P2 ×3 维持 roadmap `## Follow-up Backlog` 登记）
