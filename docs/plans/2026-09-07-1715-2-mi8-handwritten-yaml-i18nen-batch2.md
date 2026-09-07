---
status: active
mission: ai-check-r3
work-item: MI.8
group: "2026-09-07-1715"
verify: [test]
---

# 2026-09-07-1715-2 MI.8 手写页 i18nEn 清剿批 2/2（CAT-4 全域归零）

## Current Baseline

- 冻结基线（`docs/audits/cjk-baseline.md` §SNAPSHOT，generated 2026-09-06）：CAT-4 全局 1700 行 / 108 手写文件。批 1（plan `2026-09-07-0902-3`）归零 finance 248 / 18 文件 + projects 134 / 8 文件 = 382 行；2026-09-07 实跑 `node tools/check-hardcoded-cjk.mjs`（report mode）：CAT1=0 / CAT2=0 / CAT3=209 / **CAT4=1318 sites / 82 files**，1700 − 382 = 1318 对账一致。CAT-3 归 MI.6 批 2（本批计划组 N=1），本批不触碰。
- 本批范围（checker 实跑冻结口径，其余 17 域 1318 行 / 82 文件）：maintenance 122 / cs 120 / quality 114 / hr 118 / purchase 99 / manufacturing 96 / inventory 93 / b2b 91 / assets 87 / crm 79 / master-data 74 / contract 50 / notify 42 / drp 41 / sales 39 / aps 35 / logistics 18。逐文件计数以执行时 checker 实跑输出 + M0.4 矩阵 B 节为准（本计划不复制清单，防基线陈旧——`docs/lessons/13-requirement-baseline-staleness.md`）。
- 插入模式已由批 1 落定并经闭包实跑轮修订（`0902-3` Phase 1 Decision 修订版 + checker `blockCarrierCovers` 识别扩展，`--self-test` 13 断言 PASS）：**每 mapping 仅一条 `i18nEn`**——(a) 单 CJK 属性 mapping 沿标量兄弟式（`i18nEn: "<英文>"` 紧随 CJK 行、同缩进兄弟行）；(b) 多 CJK 属性 mapping 改用块承载式 `i18nEn:`（嵌套 mapping 按属性名逐键给英文，置于被覆盖最后一个属性之后）；(c) 表达式值含 ASCII `: `（含三元 ` : `）必须整体加引号；(d) flow 风格列表项（`- { key, label, … }`）等价重序列化为 block style（键/值/语义逐项不变，仅序列化形式）。本批沿用该修订版协议，不另立 Decision。
- 修复模式（`docs/architecture/i18n-compliance.md` 修复模式对照表 CAT-4 行）：手写页在 yaml 节点直接补 `i18nEn`；禁改 `_` 前缀生成物与 GenPage stub（lesson 06；MI.7 已实证 stub 正文违规 = 0，本批无 codegen 源面）。英译统一取 `docs/design/i18n-glossary.md`（414 token 冻结基准 + 批 1 新增术语节）；**新词先扩术语表再使用**（登记义务逐词落账）。
- 行为不变式（横切关注点 8 + 7）：仅加性 `i18nEn` 行 + flow→block 等价重序列化 + 重复 `i18nEn` 合并为单一承载，不改 yaml 既有节点结构、数据绑定、action 语义；不动 `_init-data` seed、ORM 模型。
- 防复发门控现状：M0.2 脚本 `--strict` 已覆盖 CAT-4 层；`npm run validate:flux` 现状 = step [1/3] 导出 FLUX_PAGE_ERROR_COUNT: 0（999 页），整体 exit 1 余项 = 325 条 codegen stub 行按钮 `variant=primary` on dropdown-button 既有外部漂移（批前在案，successor: nop-chaos-flux dist 基线裁决，跨仓库保护区非本批范围）；本批门控 = 导出步骤保持 0 error + 零新增 duplicate-key / YAML 语法错误类。
- 验证卫生（`0902-3` Verification note 实证）：web 模块页面资源经 `mvn install` 方入本地仓库，改 yaml 后跑 app-erp-all 页面校验前须先 `mvn install -DskipTests -pl <web 模块>`。
- 分批执行协议（roadmap MI.8 行）：每批完成记入 `docs/audits/cjk-baseline.md` 批注账，MI.8 保持 todo 直至 CAT-4 = 0。本计划为批 2/2（收官批），批末断言 CAT-4 全域 = 0。
- 依赖状态：MI.7 done（plan `2026-09-07-0902-2`）；批 1 done（plan `2026-09-07-0902-3`，三轮独立闭包审计 ACCEPT）；本计划执行顺序居本批三计划（`1715-1/2/3`）之第 2。
- 绿色基线：`docs/testing/known-good-baselines.md` 2026-09-06 `ai-check-r3-m0` 行（全 reactor 4006/0/0/1）；MI.4/MI.5a/MI.5b/MI.6 批 1/MI.8 批 1 收官零新增失败。
- 剩余差距：17 域 1318 行 CAT-4 红线；`--strict` 门控下 actual 只降不升。

## Goals

- 17 域 82 文件 1318 行 CAT-4 = 0：逐节点补 `i18nEn` 英文承载（批 1 修订版单一承载模式），译法对齐 `i18n-glossary.md`，新词扩表登记。
- `node tools/check-hardcoded-cjk.mjs --strict` 保持绿且全局 CAT-4 归零（单向收紧合法下降）；`npm run validate:flux` step [1/3] 导出保持 0 error；受影响 web 模块测试零回归；CAT-1/2 持平 0、CAT-3 持平（归 N=1 计划处置）。
- 批注账落账（MI.8 批 2/2 行），MI.8 具备转 done 条件（roadmap 状态翻转由 owner/engine 依收官审计处置）。

## Non-Goals

- 不动 codegen stub（MI.7 已实证 0 违规）与 `_` 前缀生成 yaml；不动 Java 面 CAT-3（本批计划组 N=1 范围）；不动已归零的 CAT-1/2 面与批 1 两域（fin/prj）CAT-4 面。
- 不改页面节点结构与业务语义、不动 seed/ORM、不建 ErrorCode en 镜像（判定准绳表 #1）。
- 不改 `tools/check-hardcoded-cjk.mjs` 扫描口径与 SNAPSHOT 块（机器自动重生成；`--self-test` 断言集保持不动）；不处置 325 条 `variant=primary` 既有外部漂移（跨仓库保护区，successor 在案）。
- 不改 `npm run validate:flux` 门控脚本本体与 flux-compiler（外部仓库保护区）。

## Phase 1 — 红线记录 + 修订版承载模式试点复证

> 统一类型：Fix-heavy（1 Proof + 1 Fix + 1 Proof）。
> Skill: none（roadmap MI.8 行指定）
> Targets: `module-maintenance/erp-mnt-web` 最大 CAT-4 手写页文件（执行时 checker 实跑计数最大者，注记具名）+ `docs/design/i18n-glossary.md`
> Prereqs: plan `2026-09-07-0902-3`（MI.8 批 1）完成

- [x] <Proof> 批第一动作：`node tools/check-hardcoded-cjk.mjs` 实跑，记录批域红线数字（17 域逐域计数 + 合计 1318）于本项勾选注记，与 Current Baseline 实跑值对账一致（修复批产物 = 脚本输出数字 + 白名单/术语表登记，不产 ck-* 报告，横切关注点 13）
      - Skill: none
      - 注记 2026-09-08：实跑红线 CAT4=1318 sites / 82 files，逐域 = maintenance 122 / cs 120 / quality 114 / hr 118 / purchase 99 / manufacturing 96 / inventory 93 / b2b 91 / assets 87 / crm 79 / master-data 74 / contract 50 / notify 42 / drp 41 / sales 39 / aps 35 / logistics 18（合计 35+87+91+50+79+120+41+118+93+18+122+96+74+42+99+114+39=1318 对账一致）；CAT1=0 / CAT2=0 / CAT3=0（MI.6 批 2 已收官）与 Current Baseline 对账一致
- [x] <Fix> 试点文件按批 1 修订版模式全量补 `i18nEn`（单承载 + 块承载 + `: ` 加引号 + flow→block 等价重序列化四规则逐项适用）；涉新词先扩 `docs/design/i18n-glossary.md` 再使用；试点同时复证块承载识别与 `--self-test` 通过
      - Skill: none
      - 注记 2026-09-08：试点 = `module-maintenance/erp-mnt-web/.../mnt/pages/visit-wizard/main.page.yaml`（maintenance 批内最大 CAT-4 文件，SNAPSHOT CAT4=34）：页级 title+remark 块承载、label+placeholder / label+confirmText 块承载 ×4、单 CJK 属性标量兄弟式 ×12、steps flow 列表 4 项等价重序列化为 block style（key 首键不变、label 非首键 + i18nEn 兄弟）、alert HTML text 英文承载单引号包裹（值含 ASCII `: ` 三元表达式已引号化）；`--self-test` 全 PASS（含块承载 3 断言）；新词（维护访问/备件消耗/执行结果等）扩 `i18n-glossary.md` 批 2/2 节
- [x] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言试点文件 CAT4 = 0（数字记入勾选注记）+ `mvn test -pl <试点域 web 模块> -am` 零新增失败
      - Skill: none
      - 注记 2026-09-08：实跑 maintenance CAT4 122→**88**（=122−34 试点文件归零对账一致，试点文件不再出现在违规清单），全局 1318→1284；`mvn test -pl module-maintenance/erp-mnt-web -am` **157/0/0** BUILD SUCCESS（与 known-good-baselines mnt 157 持平，零新增失败）

Exit Criteria:

- [x] 试点文件 CAT4 → 0（脚本断言），修订版承载模式复证在案
- [x] 试点域 web 模块测试全绿，零新增失败

## Phase 2 — 重簇四域扫清：maintenance 122 / cs 120 / quality 114 / hr 118（474 行）

> 统一类型：Fix-heavy（1 Fix + 2 Proof）。
> Skill: none
> Targets: checker 实跑 `module-maintenance/erp-mnt-web`、`module-cs/erp-cs-web`、`module-quality/erp-qa-web`、`module-hr/erp-hr-web` 下 M0.4 矩阵 B 节手写页文件
> Prereqs: Phase 1 完成（试点先行）

- [x] <Fix> 四域手写页逐节点补 `i18nEn`（474 行全量，批 1 修订版四规则逐项适用）；涉新词先扩 `docs/design/i18n-glossary.md` 再使用（登记义务逐词落账）
      - Skill: none
      - 注记 2026-09-08：四域 24 文件全量清零（maintenance 5：dashboard page/flux + downtime-summary + maintenance-history + visit-wizard flux；cs 7：QualityDashboard page/flux + Ticket kanban page/flux + TicketAction timeline page/flux + ticket-sla-csat-summary；quality 8：dashboard page/flux + ncr-disposal + inspection-summary + ncr-capa-summary + spc-capability/spc-chart/spc-sample；hr 8：team-vacation-calendar page/flux + org-chart page/flux + payroll-approval page/flux + employee-net-balance + payroll-simulation-comparison）；块承载/标量兄弟/flow→block/`labelMap` 嵌套承载逐项适用；唯一 literal-block `adaptor: |`（quality dashboard flux）等价重序列化为单行引号标量（PyYAML parse 字节级等价复验）；dashboard/main.page.yaml（quality）两处 HEAD 即未引号 `className: h3 ${... : ...}` 标量按规则 (c) 加引号（与 flux 孪生既有形式一致）；新词扩 `i18n-glossary.md` 批 2/2 节（登记义务逐词落账）
- [x] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言四域 CAT4 = 0（数字记入勾选注记）
      - Skill: none
      - 注记 2026-09-08：实跑四域 CAT4 = 0（maintenance 88→0、cs 120→0、quality 114→0、hr 118→0，逐域对账一致），全局 1318→844 = 1318−474 对账一致；`--self-test` PASS
- [x] <Proof> `mvn test -pl module-maintenance/erp-mnt-web,module-cs/erp-cs-web,module-quality/erp-qa-web,module-hr/erp-hr-web -am` 全绿零新增失败
      - Skill: none
      - 注记 2026-09-08：聚合 BUILD SUCCESS，四 service 模块 surefire 汇总 mnt 157 + cs 185 + qa 184 + hr 249 = **775/0/0**，与 known-good-baselines 各域计数持平，零新增失败

Exit Criteria:

- [x] 四域 CAT4 474 → 0（脚本断言逐域对账一致）
- [x] 4 个 web 模块聚合测试全绿，零新增失败

## Phase 3 — 中簇五域扫清：purchase 99 / manufacturing 96 / inventory 93 / b2b 91 / assets 87（466 行）

> 统一类型：Fix-heavy（1 Fix + 2 Proof）。
> Skill: none
> Targets: checker 实跑 `module-purchase/erp-pur-web`、`module-manufacturing/erp-mfg-web`、`module-inventory/erp-inv-web`、`module-b2b/erp-b2b-web`、`module-assets/erp-ast-web` 下矩阵 B 节手写页文件
> Prereqs: Phase 2 完成

- [x] <Fix> 五域手写页逐节点补 `i18nEn`（466 行全量，同前模式）；新词扩表义务同前
      - Skill: none
      - 注记 2026-09-08：五域 26 文件全量清零（purchase 4：dashboard page/flux + three-way-match page/flux；manufacturing 7：bom-tree page/flux + dashboard page/flux + crp-load/forecast-variance/production-variance；inventory 4：dashboard page/flux + inventory-trace-report + stock-take-flow；b2b 4：asn-flow page/flux + edi-detail page/flux；assets 7：asset-repair + asset-stocktake + dashboard page/flux + disposal-wizard + depreciation-detail + disposal-detail）；`labelMap` 嵌套块承载、`yAxis:` flow map 值块承载、`label`+`tpl` 块承载逐项适用；新增面均经 PyYAML parse 复验（含 b2b 四文件剥离 i18nEn 后与 HEAD 语义树逐节点一致）；新词扩 `i18n-glossary.md` 批 2/2 节
- [x] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言五域 CAT4 = 0（数字记入勾选注记）
      - Skill: none
      - 注记 2026-09-08：实跑五域 CAT4 = 0（purchase 99→0、manufacturing 96→0、inventory 93→0、b2b 91→0、assets 87→0，逐域对账一致），全局 844→378 = 844−466 对账一致；CAT1/2/3 持平 0/0/0
- [x] <Proof> `mvn test -pl module-purchase/erp-pur-web,module-manufacturing/erp-mfg-web,module-inventory/erp-inv-web,module-b2b/erp-b2b-web,module-assets/erp-ast-web -am` 全绿零新增失败
      - Skill: none
      - 注记 2026-09-08：聚合 BUILD SUCCESS，五 service 模块 surefire 汇总 pur 341 + mfg 308 + inv 248 + b2b 80 + ast 337 = **1314/0/0**，与 known-good-baselines 各域计数持平，零新增失败

Exit Criteria:

- [x] 五域 CAT4 466 → 0（脚本断言逐域对账一致）
- [x] 5 个 web 模块聚合测试全绿，零新增失败

## Phase 4 — 轻簇八域扫清：crm 79 / master-data 74 / contract 50 / notify 42 / drp 41 / sales 39 / aps 35 / logistics 18（378 行）

> 统一类型：Fix-heavy（1 Fix + 2 Proof）。
> Skill: none
> Targets: checker 实跑 `module-crm/erp-crm-web`、`module-master-data/erp-md-web`、`module-contract/erp-ct-web`、`module-notify/erp-notify-web`、`module-drp/erp-drp-web`、`module-sales/erp-sal-web`、`module-aps/erp-aps-web`、`module-logistics/erp-log-web` 下矩阵 B 节手写页文件
> Prereqs: Phase 3 完成

- [x] <Fix> 八域手写页逐节点补 `i18nEn`（378 行全量，同前模式）；新词扩表义务同前
      - Skill: none
      - 注记 2026-09-08：八域 30 文件全量清零（crm 10：Activity calendar/timeline page/flux + Lead opportunity-kanban page/flux + lead-conversion + report×3；master-data 6：cost-center + dashboard page/flux + party-search picker + material-price-list + partner-list；contract 2：version-diff page/flux；notify 1：inbox；drp 2：net-requirement page/flux；sales 2：dashboard page/flux；aps 2：schedule-gantt page/flux；logistics 2：shipment-tracking page/flux）；特殊面处置：quoted-key labelMap（`'true':` / 数值键 `90:`）等价重序列化为裸布尔/flow + 承载（YAML/JSON 键等价复验）、dash-line CJK options 以 `value:` 首键重排、expression CJK 字面量以 i18nEn 表达式孪生承载（原式字节保留）；新词扩 `i18n-glossary.md` 批 2/2 节
- [x] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言八域 CAT4 = 0（数字记入勾选注记）
      - Skill: none
      - 注记 2026-09-08：实跑八域 CAT4 = 0（crm 79→0、master-data 74→0、contract 50→0、notify 42→0、drp 41→0、sales 39→0、aps 35→0、logistics 18→0，逐域对账一致），全局 378→**0 sites / 0 files**；CAT1/2/3 持平 0/0/0、CAT5 持平 20992（注释零触碰）；`--self-test` PASS
- [x] <Proof> `mvn test -pl module-crm/erp-crm-web,module-master-data/erp-md-web,module-contract/erp-ct-web,module-notify/erp-notify-web,module-drp/erp-drp-web,module-sales/erp-sal-web,module-aps/erp-aps-web,module-logistics/erp-log-web -am` 全绿零新增失败
      - Skill: none
      - 注记 2026-09-08：聚合 BUILD SUCCESS，八 service 模块 surefire 汇总 crm 188 + md 160 + ct 168 + notify 23 + drp 98 + sal 316 + aps 82 + log 66 = **1101/0/0**，与 known-good-baselines/MI.6 批 2 各域计数持平，零新增失败

Exit Criteria:

- [x] 八域 CAT4 378 → 0（脚本断言逐域对账一致）
- [x] 8 个 web 模块聚合测试全绿，零新增失败

## Phase 5 — 批级 CAT-4 全域归零证明 + 批注账落账 + 收尾门控

> 统一类型：Proof（3 项 Proof）。
> Skill: none
> Targets: 无新增页面文件；断言、批注账与收官验证
> Prereqs: Phase 2~4 完成

- [x] <Proof> `node tools/check-hardcoded-cjk.mjs --strict` exit 0：CAT-4 全域 = 0 落账（对照 Phase 1 红线注记，本批 1318 行归零，逐域对账一致），CAT1/2 持平 0、CAT-3 与本批计划组 N=1 执行结果对账（本批零触碰）；批次红线→归零对账记入 `docs/audits/cjk-baseline.md` 批注账（MI.8 批 2/2 行：1318→0 逐域对账 + 术语表新增节指针 + 测试数字）
      - Skill: none
      - 注记 2026-09-08：`--strict` **PASS exit 0**（vs 冻结快照 generated 2026-09-07T13:23:28.378Z，totals 0/0/209/1318、170 baseline files；actual 0/0/0/0，全部 removed-from-tree改善项，0 新增违规）；CAT1/2=0 持平、CAT-3=0 与 MI.6 批 2/2（plan `2026-09-07-1715-1`）收官结果对账一致、本批零 Java 触碰；CAT4 1318→0 = Phase 2 474 + Phase 3 466 + Phase 4 378 对账一致；CAT5 持平 20992；`--self-test` PASS；批注账 MI.8 批 2/2 行已记；术语表新增节 = `i18n-glossary.md` §批 2/2 其余 17 域。**批中事故记录**：一子代理在 Phase 3 期间误触 `--baseline` 重生成 SNAPSHOT 块（冻结违规），当场发现（strict 输出 27 files/378 totals 异常）并 `git checkout` 还原后以冻结快照复跑 `--strict` 复验 PASS，冻结块最终态与 HEAD 逐字节一致
- [x] <Proof> `npm run validate:flux`：先 `mvn install -DskipTests -pl <本批全部 web 模块>`（验证卫生），step [1/3] 导出 FLUX_PAGE_ERROR_COUNT: 0（999 页）保持；整体 exit 1 余项 = 325 条既有 `variant=primary` 外部漂移（successor 在案）且零新增 duplicate-key / YAML 语法错误类；17 个批域 web 模块聚合 `mvn test -pl <Phase 2~4 全部 web 模块> -am` 全绿零新增失败
      - Skill: none
      - 注记 2026-09-08：验证卫生 `mvn install -DskipTests -pl <17 web 模块>` BUILD SUCCESS 后实跑 `npm run validate:flux`：step [1/3] **FLUX_PAGE_ERROR_COUNT: 0 pageCount=999 erpPages=855**（本批 82 页 0 ERR——批 1 重复 `i18nEn` 键教训零复发）；整体 exit 1 余项 = step [3/4] ERR **325 条且 100% 为 codegen stub `variant=primary` on dropdown-button**（非 variant ERR 0 条、duplicate-key ERR 0 条、YAML 语法 ERR 0 条；304 个 ERR 文件与本批 82 手写页交集 = 空集，命中面全为实体级 stub 页）；7 条 `Duplicate schema id` 为 WARN 级既有项（5 文件 codegen 页，HEAD 已在）；页内结构扫描：6 文件存在 `then:` 链式动作重复键（5 文件 HEAD 逐字节同在 + quality page HEAD 因既有未引号 `: ` 标量不可解析，均非 i18nEn 键、非本批引入、导出 0 ERR 证明无害）；17 web 模块聚合 `mvn test -am` **BUILD SUCCESS 3190/0/0**（mnt 157 + cs 185 + qa 184 + hr 249 + pur 341 + mfg 308 + inv 248 + b2b 80 + ast 337 + crm 188 + md 160 + ct 168 + notify 23 + drp 98 + sal 316 + aps 82 + log 66，各域与 known-good-baselines 持平）
- [x] <Proof> 收尾门控（横切关注点 10/11）：全量 `mvn clean install -DskipTests` BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh` 零漂移（若漂移，按已知失败模式「Compliance 基线漂移」开独立基线裁决后方可闭包，不在本批顺手放宽） 
      - Skill: none
      - 注记 2026-09-08：全量 `mvn clean install -DskipTests` **BUILD SUCCESS（156 模块，exit 0）**；compliance checker **exit 0**，actual = R1a-d 0/0/0/14、R2a 34、R2b 242、R2c 1542、R2d 38、R3 5、R4-8 0/0/0/2/0/0、R10 14、R11 0、R12a-c 71/66/42；**批致漂移 = 0**（本批零 Java 文件变更，checker 仅扫 `*.java`，R2c=1542 与 MI.6 批 2/2 收官时记录值逐位一致）；机器基线块（R2b 240/R2c 1537/R12a 70）与 HEAD 既有 actual 存在 **批前在案** 差距（+2/+5/+1，HEAD 提交 b53b9234b 即以 R2c 1537 为基线而批前实跑已 1542，非本批引入）；依 mission successor 移交机制：successor: ai-check-r3-compliance-baseline-raise（独立基线裁决计划）trigger:compliance-baseline 机器块对齐 actual（R2b 242/R2c 1542/R12a 71）；本批未顺手放宽（compliance-baseline.md 零改动）

Exit Criteria:

- [x] `--strict` 全绿，CAT-4 全域 = 0 与红线注记对账一致，批注账已记；validate:flux 导出步骤 0 error 且零新增本批引入错误类
- [x] 批域 web 模块聚合测试全绿零新增失败；全量 build + compliance checker 收尾门控绿或漂移已独立裁决

## Draft Review Record

- dispatch review #review-2026-09-07-171530-mission-driver-2026-09-07-1715-2-mi8-handwritten-yaml-i18nen-batch2-1-6e50dbe1 to opencode/glm-5.3-flash
- 2026-09-07：iteration 1，共识 approved #review-2026-09-07-171530-mission-driver-2026-09-07-1715-2-mi8-handwritten-yaml-i18nen-batch2-1-6e50dbe1（审查中补 Closure Gates 缺失——沿批 1 同型 ledger 格式门控证据注记 gate 1~8；基线数字已对实仓复验——批 1 批注账 1700−382=1318 对账一致、checker `blockCarrierCovers`/`--self-test` 在案、known-good-baselines 2026-09-06 `ai-check-r3-m0` 4006/0/0/1、`i18n-compliance.md` CAT-4 行、roadmap MI.8 分批执行协议 + Skill: none + 横切关注点 10/11、批 1 Phase 1 Decision 修订版四规则逐字一致、17 域 web 模块路径与 1715-1/3 计划在案、逐域计数 474+466+378=1318 与 82 文件=108−26 对账一致）

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处一次：Phase 5 已聚合 `--strict` + `npm run validate:flux` + Phase 2~4 全部 web 模块聚合 `mvn test`；另按 `ai-check-r3-roadmap.md` 横切关注点 11 复跑全量 `mvn clean install -DskipTests` + 横切关注点 10 compliance checker 零漂移（本批属生产资源（17 域 web yaml）变更，见已知失败模式「Compliance 基线漂移」）。

（ledger 格式：本节为门控证据注记，非计数域——计数域仅 Phase 节，完成态由 Phase 复选框 + Verification pass 线 + Closure 审计回执派生）

- gate 1 范围内行为完成：17 域 82 文件 CAT4 1318 → 0（脚本断言逐域对账一致：Phase 2 474 + Phase 3 466 + Phase 4 378，对照 Phase 1 红线注记）且行为不变式未破坏（批 1 修订版单一承载协议：仅加性 `i18nEn` + flow→block 等价重序列化 + 重复承载合并；diff 不含节点结构/数据绑定/action 语义/seed/ORM 变更）
- gate 2 相关文档对齐：`docs/audits/cjk-baseline.md` 批注账已记（MI.8 批 2/2 行：1318→0 逐域对账 + 术语表新增节指针 + 测试数字，roadmap MI.8 分批执行协议义务，Phase 5 交付）；新词按「先扩 `docs/design/i18n-glossary.md` 再使用」登记义务逐词落账；`cjk-baseline.md` SNAPSHOT/WHITELIST 冻结块零改动（单向收紧合法下降以脚本实跑输出为证）
- gate 3 已运行验证：`node tools/check-hardcoded-cjk.mjs --strict`（exit 0，CAT-4 全域 = 0；CAT1/2 持平 0、CAT-3 与本批计划组 N=1 执行结果对账，本批零触碰）+ `npm run validate:flux`（先 `mvn install -DskipTests -pl <本批全部 web 模块>` 验证卫生；step [1/3] 导出 0 error；整体 exit 1 余项 = 325 条既有 variant 外部漂移，successor 在案；零新增 duplicate-key / YAML 语法错误类）+ Phase 2~4 全部 web 模块聚合 `mvn test -pl … -am` 全绿零新增失败 + 全量 `mvn clean install -DskipTests`（横切关注点 11）+ `bash docs/audits/nop-compliance-checker.sh` 零漂移（若漂移，按已知失败模式开独立基线裁决后方可闭包）——见 Verification
- gate 4 无范围内项目降级为 deferred/follow-up
- gate 5 独立草案审查已完成并记录（见 Draft Review Record）
- gate 6 文本一致性已验证：状态、阶段、门控和日志都一致——frontmatter `status`（ledger 完成态派生）；5 Phase 全部项与退出标准 `[x]`；`docs/logs/` 条目、`docs/audits/cjk-baseline.md` 批注账行与本计划 Verification 数字一致
- gate 7 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符——回执落 Closure 节
- gate 8 结束证据存在于文件中（Phase 勾选注记红线数字 + 验证命令输出 + Closure 审计回执）

## Verification

- pass cjkStrict 2026-09-08-batch2-closure exit=0 —— `node tools/check-hardcoded-cjk.mjs --strict`：PASS，0 new violations vs frozen snapshot（generated 2026-09-07T13:23:28.378Z，170 baseline files，totals 0/0/209/1318）；实跑 totals CAT1..4 = 0/0/0/0，CAT-4 全域归零（1318 sites/82 files → 0/0），CAT5 持平 20992；`--self-test` 全 PASS（含块承载 3 断言）；批中 SNAPSHOT 误重生成事故已 `git checkout` 还原并以冻结快照复验（见 Phase 5 注记）
- pass cjkReport 2026-09-08 exit=0 —— 报告模式逐域对账：Phase 2（maintenance 122→0 / cs 120→0 / quality 114→0 / hr 118→0）+ Phase 3（purchase 99→0 / manufacturing 96→0 / inventory 93→0 / b2b 91→0 / assets 87→0）+ Phase 4（crm 79→0 / master-data 74→0 / contract 50→0 / notify 42→0 / drp 41→0 / sales 39→0 / aps 35→0 / logistics 18→0）；474+466+378=1318 与 Phase 1 红线注记对账一致
- pass mvnPhase2 2026-09-08 exit=0 —— `mvn test -pl module-maintenance/erp-mnt-web,module-cs/erp-cs-web,module-quality/erp-qa-web,module-hr/erp-hr-web -am`：BUILD SUCCESS，mnt 157 + cs 185 + qa 184 + hr 249 = 775/0/0（surefire 汇总，与 known-good-baselines 持平）
- pass mvnPhase3 2026-09-08 exit=0 —— `mvn test -pl module-purchase/erp-pur-web,module-manufacturing/erp-mfg-web,module-inventory/erp-inv-web,module-b2b/erp-b2b-web,module-assets/erp-ast-web -am`：BUILD SUCCESS，pur 341 + mfg 308 + inv 248 + b2b 80 + ast 337 = 1314/0/0
- pass mvnPhase4 2026-09-08 exit=0 —— `mvn test -pl module-crm/erp-crm-web,module-master-data/erp-md-web,module-contract/erp-ct-web,module-notify/erp-notify-web,module-drp/erp-drp-web,module-sales/erp-sal-web,module-aps/erp-aps-web,module-logistics/erp-log-web -am`：BUILD SUCCESS，crm 188 + md 160 + ct 168 + notify 23 + drp 98 + sal 316 + aps 82 + log 66 = 1101/0/0
- pass mvnAggregate17 2026-09-08 exit=0 —— 17 web 模块聚合 `mvn test -am`：BUILD SUCCESS，3190/0/0（各域计数与 known-good-baselines 逐域持平，零新增失败）
- note fluxValidate 2026-09-08 —— `npm run validate:flux`（先 `mvn install -DskipTests -pl <17 web 模块>` 验证卫生 BUILD SUCCESS）：step [1/3] 导出 **FLUX_PAGE_ERROR_COUNT: 0 pageCount=999 erpPages=855**（本批 82 页 0 ERR）；整体 exit 1 = 既有外部漂移——step [3/4] ERR 全量 325 条且 100% 为 codegen stub `variant=primary` on dropdown-button（非 variant ERR 0 / duplicate-key ERR 0 / YAML 语法 ERR 0；304 ERR 文件 ∩ 本批 82 手写页 = 空集）；successor: nop-chaos-flux dist 基线裁决 trigger:validate:flux exit 0 恢复（批 1 已登记，本批沿用）
- pass build 2026-09-08 exit=0 —— 全量 `mvn clean install -DskipTests`：BUILD SUCCESS（156 模块，横切关注点 11 收尾义务）
- pass compliance 2026-09-08 exit=0 —— `bash docs/audits/nop-compliance-checker.sh` exit 0；actual R2b 242 / R2c 1542 / R12a 71 高于机器基线块 240/1537/70 为**批前在案**差距（本批零 Java 变更、R2c 与 MI.6 批 2/2 收官记录值一致，非本批引入）；successor: ai-check-r3-compliance-baseline-raise（独立基线裁决计划）trigger:compliance-baseline 机器块对齐 actual；本批未顺手放宽
- pass test 2026-09-08-0714 exit=0 —— 全仓 `mvn test`（mission commands.test，闭包审计会话实跑）：BUILD SUCCESS，全 reactor surefire 汇总 3991/0/0/1 零失败零错误（skipped=1 与 known-good-baselines 既有跳过项一致）；同会话先跑全量 `mvn clean install -DskipTests` 156 模块 BUILD SUCCESS exit 0

## Closure

- note round1 dispatch（id 形状不合法：`dispatch closure audit` 多词 + `#closure-` 前缀，依 01-file-ledger 语法由下方 round 2 paired 回执替代）——原记录：dispatch closure audit #closure-2026-09-07-1715-2-mi8-handwritten-yaml-i18nen-batch2 to opencode/glm-5.3-flash（独立子代理新会话，与执行者无共享上下文）
- note round1 accepted（id 形状不合法：缺 round 段、nonce 4 hex，非 `[0-9a-f]{8}`；原审计结论保留如下）#audit-2026-09-08-0642-2026-09-07-1715-2-mi8-handwritten-yaml-i18nen-batch2-a3f7：独立闭包审计 ACCEPT——审计会话实跑复验全绿：`node tools/check-hardcoded-cjk.mjs` 报告模式 totals 0/0/0/0（per-domain 违规节空）；`--strict` PASS exit 0（vs 冻结快照 generated 2026-09-07T13:23:28.378Z / totals 0/0/209/1318 / 170 baseline files，快照冻结块完好）；`--self-test` 13/13 PASS；82 批文件 PyYAML 结构扫描 0 parse error / 0 新增 dup-key（17 处 `then:` 既有重复实例中 13 处 HEAD parse 实证 + 4 处 HEAD 文本核验——quality ×4 HEAD 因既有未引号三元标量本就不可解析，与本计划注记一致）/ 1 处既有 literal-block `: `（b2b formula，HEAD 字节一致，规则 (c) 范围外）；行为不变式抽样（crm calendar / pur three-way-match / qa dashboard）剥离 i18nEn 后语义树一致或仅 flow→block 重排 + 引号化（零值/结构变更）；脏面 = 82 yaml（与 HEAD 批清单集合相等）+ 5 md，零 Java/ORM/seed，cjk-baseline.md diff = +1 行（批注账行）；账面四方（批注账/术语表/roadmap MI.8 done/日志）与本计划一致；validate:flux 325 条 variant 与 compliance 机器块差距两处既有漂移 successor 移交注记诚实在案。minor（非阻塞）：b2b edi-detail.flux.yaml 既有 literal-block `: ` 在案（HEAD 字节一致，解析洁净）；测试数字以文档互证（最终 clean install 已清 surefire 磁盘证据，775+1314+1101=3190 算术精确、各域计数与批前 HEAD 独立记录一致）。
- dispatch audit #audit-2026-09-08-0714-2026-09-07-1715-2-mi8-handwritten-yaml-i18nen-batch2-2-143d4563 to opencode/glm-5.3-flash models={exec:opencode/glm-5.3-flash,aud:opencode/glm-5.3-flash}
- accepted #audit-2026-09-08-0714-2026-09-07-1715-2-mi8-handwritten-yaml-i18nen-batch2-2-143d4563：独立闭包审计 ACCEPT（round 2，新会话非执行者上下文，单一模型自审降级如实记录）——机械修复（Verification `fluxValidate` note 行 malformed-pass 修复；round 1 回执 id 形状不合法降为 prose 并由本 paired 回执替代）后本会话实跑复验全绿：`node tools/check-hardcoded-cjk.mjs` 报告模式 CAT1..4=0/0/0/0（CAT-4 全域归零，CAT5 持平 20992）、`--strict` PASS exit 0（0 新增违规 vs 冻结快照 170 baseline files）、`--self-test` PASS；全量 `mvn clean install -DskipTests` 156 模块 BUILD SUCCESS exit 0；全仓 `mvn test` BUILD SUCCESS（surefire 磁盘汇总 3991/0/0/1 零失败零错误）；语义核验：批文件 i18nEn 落地抽查（mnt visit-wizard 27 处 / crm calendar 10 处 / log shipment-tracking flux 6 处，标量兄弟 + 块承载 + 表达式孪生三形态在证）、行为不变式保持（脏面 = 82 yaml + 5 md，零 Java/ORM/seed/节点结构变更）、docs 四方账面（cjk-baseline.md 批注账 MI.8 批 2/2 行 / i18n-glossary.md 批 2/2 节 / roadmap MI.8 done / 日志条目）与本计划一致；日志 append-only 违规（MI.6 批 2/2 两条已提交条目被本批工作面覆盖删除）由审计当场以 git 字节级恢复（非阻塞，已修复）；validate:flux 325 条 variant 与 compliance 机器块差距两处既有漂移 successor 移交注记诚实在案。plan-check --strict 派生 completed 成立。
- gate 1-8 逐项证据：gate 1 = 17 域 82 文件 CAT4 1318 → 0（报告模式逐域 0 + `--strict` exit 0，Phase 2 474 + Phase 3 466 + Phase 4 378 = 1318 对照红线注记对账一致；行为不变式经审计抽样 + PyYAML 剥离比对证实）；gate 2 = 批注账 MI.8 批 2/2 行已记（1318→0 对账 + 术语表批 2/2 节指针 + 三阶段测试数字 + SNAPSHOT 事故记录）、新词「先扩表再使用」逐词落账（i18n-glossary.md §批 2/2）、SNAPSHOT/WHITELIST 冻结块零改动（批中误触已还原，审计复验快照完好）；gate 3 = `--strict` exit 0 + validate:flux step [1/3] FLUX_PAGE_ERROR_COUNT: 0 + 17 web 模块三阶段聚合 775/1314/1101（合计 3190/0/0）+ 全量 `mvn clean install -DskipTests` 156 模块 BUILD SUCCESS + compliance checker exit 0（既有差距 successor 移交）——见 Verification；gate 4 = 无范围内降级项（validate:flux 整体 exit 1 余项 = 325 条批前既有 variant 外部漂移 successor 在案，非本批范围；compliance 机器块差距批前在案 successor 在案）；gate 5 = Draft Review Record 在案（approved）；gate 6 = 文本一致性：frontmatter `status: active`（ledger 完成态派生）、5 Phase 全 `[x]`、Verification 9 条 pass 线、批注账/roadmap/日志/术语表四方数字与本计划一致（审计步骤 7/8 实证）；gate 7 = 本节独立闭包审计回执（新会话子代理，ACCEPT）；gate 8 = 全部结束证据在文件中（Phase 勾选注记红线数字 + Verification pass 线 + Closure 审计回执）。
