# 2026-08-15-1605-3-rc-mr1-r1-40-md-price-validation-level-default-convergence RC-R1.40 — master-data priceValidationLevel 默认值收敛（MR1 第一批纯预授权）

> Plan Status: completed
> Last Reviewed: 2026-08-15
> Mission: requirement-compliance
> Work Item: RC-R1.40（P2-RC-057：ErpMdMaterialCategory.priceValidationLevel defaultValue "20" → "WARN"，不改表结构/既有数据/行为）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.40 行 + `docs/audits/arm-index.md` P2-RC-057 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md` + 人工裁决 `docs/discussions/2026-08-07-1140-rc-approval-inventory-analysis.md` §7 A5（2026-08-08 生效：P2-RC-057 ORM defaultValue 改 "WARN" 纯收敛修复，按 Q3 纯加性类自动执行）
> Related: `docs/audits/2026-08-08-0015-rc-ma4-a4-2-143-146-master-data-runtime.md`（A4.2.145 运行时证据）；`module-master-data/model/app-erp-master-data.orm.xml`（:344 priceValidationLevel 列）；`docs/design/master-data/sku-multi-unit.md`（价格优先级）+ `use-cases.md`
> Audit: required

## Current Baseline

- **finding P2-RC-057（arm-index 行，A4.2.145）**：ORM `ErpMdMaterialCategory.priceValidationLevel`（`app-erp-master-data.orm.xml:344`，propId=6，VARCHAR(20)，ext:dict=`erp-md/price-validation`）**`defaultValue="20"`——孤儿非字典值**：dict `erp-md/price-validation`（orm.xml:72-76）仅 `OFF`/`WARN`/`HARD` 三值 + `ErpMdConstants.java:31-35` 常量集同；**种子输入侧普查**（A4.2.145）：全仓 csv/sql/json **种子输入**零 priceValidationLevel 显式填充（`erp-md-service/_cases/**/erp_md_material_category.csv` 仅表头无值 + `erp-md-meta/_templates/_ErpMdMaterialCategory.json` 空值模板；`_cases` output 快照含显式校验级别值属测试产物，非种子输入）→ "20" 仅经 ORM defaultValue 在插入未显式赋值时物化；`resolvePriceValidationLevel`（`ErpMdMaterialSkuBizModel.java:356-373`，非字典值兜底 `:371-372`——行号继承自 A4.2.145 报告，执行期按实仓校正）非字典合法值（含 "20"）→ WARN + category null/不存在 → WARN；`validatePrice`（`ErpMdMaterialSkuBizModel.java:182-207`，WARN 返回 `:205-206`——同上继承注记）WARN 分支 → passed=true + warning=true **警告但放行**。
- **影响面结论（A4.2.145）**：新创建分类未显式设置 → 静默得 WARN 语义（与 dict WARN 行为对齐、无阻断副作用）；孤儿值仅违反字典契约（可维护性/语义漂移风险），运行时行为正确。**登记 watch-only 部署配置决策：defaultValue 修订归 MR1 人工裁决方案 A[ORM defaultValue 改 "WARN" 须 ask-first] / B[文档预授权]**。
- **人工裁决（2026-08-08 §7 A5，批准人：用户，生效即日）**：**A5：P2-RC-057 ORM defaultValue 改 "WARN"（纯收敛修复，按 Q3 纯加性类自动执行）**——ask-first 义务由该显式人工裁决满足（**非类推**：A5 是针对本具体项的显式裁决，不扩大 Q3 枚举「加列/加 UK/新增实体」范围；skills README「批量预授权枚举不可类推」条款适用边界以显式裁决为准）。roadmap RC-R1.40 行标注「第一批（纯预授权）」。
- **变更面**：仅 `defaultValue` 属性文本 `"20"` → `"WARN"`（orm.xml:344 单行）；不改表结构/索引/UK/实体/既有数据（defaultValue 仅影响**新插入未显式赋值**行的物化值，既有行不受影响）；行为不变（"20"→WARN 与 "WARN"→WARN 经 `resolvePriceValidationLevel` 同归 WARN 分支）。
- **重生成链路**：ORM 模型变更后用 `mvn clean install -DskipTests` 触发增量重新生成（**不要**重跑 `nop-cli gen`，见 project-context.md）；生成产物（XMeta/DDL/i18n/codegen Java）中 defaultValue 同步传播（XMeta `_ErpMdMaterialCategory.xmeta` 默认值注记），须核对不引入漂移。
- **涉及文件**：`module-master-data/model/app-erp-master-data.orm.xml`（:344 单行修改）+ 重生成产物核对 + 测试（erp-md-service 既有 143 tests 基线，R1.29 注记）+ owner doc（master-data 价格校验描述处补收敛注记）+ arm-index + roadmap + `docs/logs/2026/08-15.md`（回填）。

## Goals

- **defaultValue 收敛（P2-RC-057 方案 A 落地）**：`ErpMdMaterialCategory.priceValidationLevel` defaultValue `"20"` → `"WARN"`——新创建分类未显式赋值时物化字典合法值 WARN，孤儿非字典值从模型消除（字典契约收敛）。
- **行为不变性证明**：新分类默认校验级别 WARN 语义（修复前后一致——"20" 经 resolvePriceValidationLevel 亦归 WARN）；既有行/既有测试零影响。
- **重生成干净**：`mvn clean install -DskipTests` 增量重生成通过，生成产物（XMeta/DDL）defaultValue 同步且零意外漂移。
- **验证**：erp-md-service 143 tests 零回归 + 新增 3 个收敛/兜底断言测试（defaultValue 物化 WARN + 显式 OFF/WARN/HARD 不受影响 + 非字典值兜底回归）+ 全量构建 + compliance checker 零漂移。
- **回填**：arm-index P2-RC-057 → `done (RC-R1.40)` + roadmap 行 → done ✅ + owner doc 收敛注记 + `docs/logs/2026/08-15.md` 日志条目。

## Non-Goals

- **不改表结构/索引/UK/实体**（仅 defaultValue 属性文本；Q3 纯加性枚举内「加列/加 UK/新增实体」均不适用本行）。
- **不做既有数据迁移**（defaultValue 仅影响新插入；既有 "20" 值行保持现状——行为即 WARN，无迁移必要；数据卫生迁移属 successor）。
- **不实现 P2-RC-058**（barcode 并发 TOCTOU DB UK——A4.2.144 登记 watch-only，未映射 RC-R1.n 行，登记不强制；触 ORM UK 须 ask-first）。
- **不实现 P1-RC-062**（SKU 独立停用 + 引用检查——独立越界行 RC-R1.72 须 ask-first）。
- **不改 `resolvePriceValidationLevel` / `validatePrice` 行为**（非字典值→WARN 兜底保留，防历史遗留 "20" 值行行为翻转）。
- **不做前端 AMIS 接线**。
- **不改真相源契约段落**（use-cases L1 不动）。

## Task Route

- Type: `implementation-only change`（P2 finding 的纯收敛修复，2026-08-08 §7 A5 人工裁决预授权；ORM defaultValue 单行修改 + 重生成）
- Owner Docs: `docs/design/master-data/sku-multi-unit.md`（价格优先级）+ `docs/design/master-data/use-cases.md`（UC-MD 价格校验契约）+ `docs/audits/2026-08-08-0015-rc-ma4-a4-2-143-146-master-data-runtime.md`（A4.2.145 运行时证据）
- Skill Selection Basis: 实现面 = ORM 模型单属性收敛 + 增量重生成（`nop-backend-dev`——模型优先 + 重生成纪律 `mvn clean install -DskipTests` 不重跑 gen）；测试（`nop-testing`——既有 erp-md-service 测试范式 + 收敛断言）。无 view.xml/xbiz/会计/删除变更。

## Infrastructure And Config Prereqs

- 无新外部服务/环境变量/config key（defaultValue 是模型元数据，非运行配置）。
- ORM 变更后须 `mvn clean install -DskipTests` 全量重生成（增量 gen-orm.xgen 链），**不要**重跑 `nop-cli gen`（project-context.md 规则）。
- 分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-master-data/erp-md-service`。

## Execution Plan

### Phase 1 - ORM defaultValue 收敛 + 重生成

Status: completed
Targets: `module-master-data/model/app-erp-master-data.orm.xml`（:344 单行）+ 全量重生成
Item Types: `Fix | Proof`
Skill: `nop-backend-dev`

- [x] `priceValidationLevel` 列 defaultValue `"20"` → `"WARN"`（仅属性文本修改，其余列定义不动）；`xmllint --noout` well-formed 校验
  - Skill: `nop-backend-dev`
- [x] Proof: `mvn clean install -DskipTests` 增量重生成通过 + 生成产物核对——XMeta `_ErpMdMaterialCategory.xmeta` defaultValue 同步为 "WARN"（若模板传播）+ DDL default 子句更新为 `default 'WARN'`（**defaultValue 令牌同步属预期变化，仅核对无结构性漂移**——DDL 结构/索引/UK 零变更）+ 全仓 grep `priceValidationLevel` 生成侧无 "20" 残留（`_gen` 注释/i18n 核对）
  - Skill: `nop-testing`

Exit Criteria:

- [x] orm.xml 单行收敛 + well-formed + 全量构建通过（重生成干净，无编译错误/无生成产物意外漂移）
- [x] 生成侧（XMeta/DDL/i18n）defaultValue "WARN" 同步确认或「不传播」事实记录

### Phase 2 - 测试与验证

Status: completed
Targets: `module-master-data/erp-md-service/src/test`（新增收敛断言）+ 全量验证
Item Types: `Add | Proof`
Skill: `nop-testing`

- [x] 新增收敛断言测试（对齐既有 TestErpMd* 范式）：不显式赋值创建 ErpMdMaterialCategory → 持久化 `priceValidationLevel == "WARN"`（defaultValue 物化断言）+ 显式赋值 OFF/WARN/HARD 路径不受影响
- [x] 新增非字典值兜底回归断言（无条件项）：既有 "20" 值行 resolve 行为——`resolvePriceValidationLevel` 非字典值→WARN 兜底保留断言（既有 4 个校验测试仅覆盖 OFF/WARN/HARD/高于底线，非字典兜底路径确实零覆盖——本项补缺口，修复后行为不变性证明）
- [x] Proof: `mvn test -pl module-master-data/erp-md-service` 143 基线 + 新增全绿 + 全量 `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh` actual==baseline 零漂移（零新增 daoFor/import 面）
  - Skill: `nop-testing`

Exit Criteria:

- [x] 收敛断言 + 兜底回归断言测试 GREEN + erp-md-service 既有 143 tests 零回归
- [x] 全量构建通过 + checker 零漂移（R2c=1399 / R10=9 不变）

### Phase 3 - 回填

Status: completed
Targets: arm-index + roadmap + owner doc + docs/logs
Item Types: `Follow-up`
Skill: `none`

- [x] arm-index P2-RC-057 → `done (RC-R1.40)`（含修复落地摘要：A5 裁决引用 + defaultValue 收敛 + 行为不变性证明 + 数据卫生 successor）
- [x] roadmap RC-R1.40 行 → done ✅（含落地摘要）
- [x] owner doc 注记：master-data 价格校验描述处（sku-multi-unit.md 或对应价格校验节）补收敛实现注记（defaultValue "WARN" + 既有 "20" 值行语义不变 + 数据卫生迁移 successor）
- [x] `docs/logs/2026/08-15.md` 顶部追加本计划落地日志条目（格式见 `docs/logs/00-log-writing-guide.md`）

Exit Criteria:

- [x] 回填完成且与 roadmap/arm-index/owner doc/logs 四源一致

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is with 6 non-blocking suggestions (`ses_ffb8a886fffeRTkt2IlThNEp06`) because 实仓全过（orm.xml:344 defaultValue="20" / dict 三值 / 143 tests 基线 / A5 裁决引用非类推 / 重生成命令正确 / 行为中立论证成立），无阻塞问题——P2-RC-058 映射引用修正 / 行号漂移注记 / Item Types / DDL 预期变化措辞 / 无条件兜底断言 / 种子输入侧措辞
- Independent draft review iteration 2: accept (`ses_ffb812540ffednCm2KT7man3tv`) because 6 项建议全部修订核实（含实仓精确行号 :356-373/:371-372/:182-207 确认 + TestErpMdSkuPriceValidation 4 测试零非字典路径确认），无阻塞问题

## Closure Gates

- [x] 范围内行为完成（defaultValue 收敛 + 行为不变 + 重生成干净）
- [x] 相关文档对齐（owner doc 注记 + arm-index + roadmap + logs）
- [x] 已运行验证（`mvn test -pl module-master-data/erp-md-service` + 全量 `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh` 零漂移）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 既有 "20" 值行数据卫生迁移

- Classification: `watch-only residual`
- Why Not Blocking Closure: 既有 "20" 值行运行时行为即 WARN（resolvePriceValidationLevel 非字典值→WARN 兜底保留），无行为偏离；defaultValue 收敛只影响新插入；数据迁移（UPDATE 存量 "20"→"WARN"）属数据变更须 ask-first，移出本行
- Successor Required: `yes`（存量数据卫生迁移需求立项后按 ask-first 流程实施）

## Closure

Status Note: completed（2026-08-15 独立结束审计 ACCEPT 后关闭）

Closure Audit Evidence:

- Auditor / Agent: independent subagent `ses_ffb15c809ffejkBg5UveGPIlPy`（新会话，无执行者上下文）
- Evidence: 三 Phase 全绿 + 实仓核对（orm.xml:344 defaultValue="WARN" 仅属性单行变更 + XMeta/DDL/_app.orm.xml 同步 + BizModel 注释-only + 3 新测试断言三语义 + 4 快照 CSV 仅 PRICE_VALIDATION_LEVEL 20→WARN）+ 验证复跑（`mvn test -pl module-master-data/erp-md-service` 146/146 + checker R2c=1399/R10=9 零漂移 + sales TestErpSalPricingCompliance 10/10）+ 四源回填一致（arm-index 9-pipe 行结构完好 / roadmap done ✅ / sku-multi-unit.md 注记 / logs 顶部条目）+ 1 项非阻塞 WARN（Goals 测试计数措辞）已由执行者修订后复核一致 → **ACCEPT**

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
