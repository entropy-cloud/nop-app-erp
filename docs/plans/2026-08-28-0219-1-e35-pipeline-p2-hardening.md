# 2026-08-28-0219-1-e35-pipeline-p2-hardening E3.5 AP 管道 P2 加固批（fin 域：入口契约/幂等裁决/确定性/可观测/鉴权 E2E）

> Plan Status: completed（2026-08-28 执行完毕，独立结束审计 acceptable，task `ses_fbb32e619ffeO707VRiSLDHa0N`）
> Mission: erp-enhancement
> Work Item: Follow-up Backlog E3.5 fin 域批（P2-1/P2-3/P2-6/P2-7/P2-8/P2-9/P2-11/P2-15 + P2-E + P2-14 fin 部分）
> Last Reviewed: 2026-08-28
> Source: `docs/backlog/erp-enhancement-roadmap.md §Follow-up Backlog`（来源审计 `docs/audits/2026-08-26-2226-multi-audit-erp-enhancement.md` P2-1/3/6/7/8/9/11/14/15 + `docs/audits/2026-08-26-2226-open-audit-erp-enhancement.md` P2-E）
> Related: plan `2026-08-27-2006-1`（P1 修复已完成，其 Non-Goals 将本批 P2 显式移出；「随 plan 顺带」触发已过，P2 项现为无主待收口）、plan `2026-08-26-0735-2`（E3 整体实现，本批修复其 P2 遗留）
> Audit: required

## Current Baseline

（2026-08-28 活仓逐项核实；行号以当日 HEAD 为准，执行时若有漂移以符号定位）

- **P2-8 上传入口契约松**：`ErpFinApDocumentBizModel.java:32-38` `uploadApDocument` 无 MIME/扩展名白名单（类型全由客户端 `fileName/mimeType` 自报）；`ErpFinApDocumentPipelineProcessor.java:541-545` `decodeBase64` 直接 `Base64.getDecoder().decode`，非法 base64 抛裸 `IllegalArgumentException`（非 NopException 范式）；`fileName` 未按列精度 200 截断（orm `app-erp-finance.orm.xml` fileName 列 precision 200）。
- **P2-9 错误码契约错位**：`ErpFinErrors.java:545` `ERR_AP_DOC_PARSE_FAILED` 定义未用；PARSE 步失败以 `ERR_AP_DOC_DRAFT_FAILED`（:549）面客（Processor 内 PARSE 失败路径与 DRAFT 失败共用同一错误码构造点），排障误导。
- **P2-1 重复上传幂等缺失**：`Processor.upload()`（:124-148）每次上传 `saveFile` + 新建 `RECEIVED` 文档，无 fileId/内容哈希去重；`process()` 的 DRAFTED/ARCHIVED 守卫（:157）仅防重复处理，不防重复上传；orm 无唯一约束（§8.1 授权已消费，新增唯一索引**未授权**）；「去重责任是否后移三单匹配」未落 owner doc。
- **P2-3 dict 死状态**：`erp-fin/ap-doc-status.dict.yaml:33` `ARCHIVED` 全仓无 writer；owner doc「自动归档」挂在暂缓的邮件摄取（E3.5 Non-Goal）；`process()` 守卫已消费 ARCHIVED（:157）。lesson-10 家族。
- **P2-6 分类引擎确定性/上界瑕疵**：`ErpFinApDocRuleClassifier.java` `classify()`（:46-47）`String.valueOf(parseResult.get(...))` 可能产生字面 `"null"`；`classifyDocType()`（:70-72）仅 isBlank 守卫，无 `"null"` 字面守卫（supplierName 侧 `matchPartner` :98 已有 `"null".equals` 守卫——半修态）；`matchPartner()`（:95-119）加载 ≤2000 伙伴**无排序**（首匹配不确定）+ 查询侧无名称过滤（O(n) 内存扫描）+ 超 2000 静默截断落人工门（无日志无计数）。
- **P2-7 OCR 吞异常零日志 + 无页数上限**：`ErpFinTextExtractOcrEngine.java` PDF 分支 `catch(Exception){return null;}`（有注释解释扫描件合法分支，但**零日志**——损坏 PDF 与扫描件不可区分）；PDFBox `Loader.loadPDF(content)` 全内存加载 + `PDFTextStripper` 无页数上限（大 PDF CPU/内存放大）。
- **P2-11 限流措辞失实**：`ErpFinConfigs.java:23` `DEFAULT_AP_DOC_UPLOAD_RATE_LIMIT_RPS = 10.0` 对**所有**调用方（含人类用户经 `/r/` 上传）生效；`ai-native-interface.md:74` 最小落地集 item 4「人类用户面默认无限流」为字面失实（实际影响趋零——限流仅接线于管道 upload 单入口）。
- **P2-15**：`ErpFinBankReconAutoReverseHelper.java:118` 生产代码 `LocalDate.now()` 违反 CoreMetrics 强制约定（checker R7 不覆盖 LocalDate.now 形态）；「随下次 fin 域触碰修复」触发条件已过（plan 2006-1/2100-1 均触碰 fin 未修）。
- **P2-E 新 mutation 鉴权面零有效覆盖**：action-auth 种子已正确登记（`_vfs/erp/fin/auth/erp-fin.action-auth.xml:82-86` uploadApDocument 权限项）但从未被任何测试执行过拒绝路径——`ai-interface.value.spec.ts:18,54-62` 负路径实测既有 `ErpFinBadDebt__writeOff`（spec 头注 item 4 自证），`TestErpFinApDocumentPipeline` 以 `enableActionAuth=FALSE` 运行。E3 计划 `2026-08-26-0735-2` Phase 7 ④「高影响 mutation 被拒」证据归因于新 API 面属漂移（实际指向 BadDebt）。
- **P2-14 fin 部分**：`_vfs/nop/batch-task/fin/ap-document.batch.xml` + `erp-fin-ap-doc-processing.job.yaml` 三件套接线零测试覆盖。
- 先例范式：REQUIRES_NEW 失败落账 / per-item 隔离已由 plan 2006-1 落地（`persistFailure`/`processOne`）；本批不触碰事务结构。
- E2E 基线：`tests/e2e/business-actions/fin-ap-document.value.spec.ts`、`ai-interface.value.spec.ts` 存在且 2006-1 闭包时 5/5 绿；playwright config 与 `_tmp-server.sh` 携带 `-Derp-fin.ap-doc-pipeline-enabled=true`。

## Goals

- 上传入口输入契约收紧（白名单 + 截断 + NopException 范式），PARSE 失败以专属错误码面客（P2-8/P2-9）。
- 重复上传幂等策略落地（裁决 + 轻量守卫或 owner doc 登记后移责任，零 ORM）（P2-1）。
- dict `ARCHIVED` 死状态裁决并登记（writer 归属触发条件显式化）（P2-3）。
- 分类引擎确定性修复（排序 + 查询侧过滤 + 超量显式策略 + `"null"` 幻影路径收口）与 OCR 可观测性（分类 WARN 日志 + 页数上限 config-gate）（P2-6/P2-7）。
- 限流措辞与实现对齐（P2-11）；`LocalDate.now` → CoreMetrics 约定修复（P2-15）。
- `uploadApDocument` 无权限拒绝路径 E2E 落地 + E3 计划 Phase 7 证据归因修正（P2-E）。
- batch.xml + job yaml 接线测试覆盖（P2-14 fin 部分）。
- 消费本批 roadmap §Follow-up Backlog 对应行并勾销登记。

## Non-Goals

- 不改 ORM（§8.1 授权已消费；新增唯一索引/字段未授权——P2-1 守卫必须应用层实现并登记并发残留风险）。
- 不改事务结构（REQUIRES_NEW/per-item 隔离为 2006-1 已交付面，本批零触碰）。
- 邮件摄取与自动归档实现维持 E3.5 原 Non-Goal（P2-3 只裁决登记，不实现 writer）。
- 不动 human 通用面全局限流（P2-11 只对齐管道入口措辞，不引入按调用方类型区分限流的机制）。
- P2-2/P2-10/P2-12/P2-13/P2-A/P2-B/P2-C（文档注册与 owner-doc 对齐批，归本批 plan 3）、P2-4/P2-5/P2-14 非 fin 部分（归 plan 2）、P2-D（归 mission VERIFY 批）、P2-16/P2-F、E3.7（用户门控暂缓）。

## Task Route

- Type: `implementation-only change`（已确认 P2 缺陷批量收口，同表面批量消费符合审计「修复建议路由」与 plan 指南规则 14——非单项 P2 驱动独立计划）
- Owner Docs: `docs/design/finance/document-driven-ap-automation.md`（AP-1 人工门/管道语义/幂等责任登记点）、`docs/design/ai-native-interface.md`（:72-74 action 描述与限流护栏措辞）、`docs/architecture/job-scheduling.md`（P2-A 行格式范本，注册归 plan 3）
- Skill Selection Basis: `nop-backend-dev`（BizModel 入口契约/错误码/跨实体查询确定性——修复面在其决策门内）；`nop-testing`（负路径 IT 与 E2E 鉴权拒绝路径为核心 Proof）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（E2E 本地 server 已携带 pipeline-enabled JVM 参数；无新端口/密钥/外部服务）

## Execution Plan

### Phase 1 - 入口契约/幂等裁决/确定性/可观测修复（代码面）

Status: completed
Targets: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/entity/ErpFinApDocumentBizModel.java`、`.../processor/ErpFinApDocumentPipelineProcessor.java`、`.../classify/ErpFinApDocRuleClassifier.java`、`.../ocr/ErpFinTextExtractOcrEngine.java`（实际包路径以活仓为准）、`.../ErpFinErrors.java`、`.../ErpFinConfigs.java`、`.../bankrecon/ErpFinBankReconAutoReverseHelper.java`、`docs/design/finance/document-driven-ap-automation.md`、`docs/design/ai-native-interface.md`
Skill: `nop-backend-dev`

- Item Types: `Fix | Decision | Add`
- Prereqs: none

- [x] `Fix` P2-8 上传入口契约：扩展名/MIME 白名单（执行时**枚举定稿**最终集合，与 OCR 引擎实际可解析能力对齐——pdf/txt 可抽取文本，png/jpg 过白名单后经 OCR 引擎返回 null 落人工门为合法扫描件分支；集合定稿记入执行注记）；`fileName` 按列精度 200 截断或超长拒绝（与列语义一致者）；非法 base64 捕获为 `NopException`（新错误码或复用既有参数错误码范式，参数带 fileName）。Skill: `nop-backend-dev`
      → 完成：白名单定稿 = 扩展名 `pdf/txt/csv/json/xml/png/jpg/jpeg` + MIME `application/pdf`、`text/*`、`application/json`、`application/xml`、`image/png`、`image/jpeg`（含 contains 等价变体；枚举记入 owner doc §P2 加固裁决登记）；fileName 超长**拒绝**（截断会产生同名碰撞与溯源断裂，拒绝与列语义一致）；非法 base64 → `ERR_AP_DOC_INVALID_BASE64`（cause 链保留 + fileName 参数）；另新增 `ERR_AP_DOC_FILE_TYPE_NOT_ALLOWED` / `ERR_AP_DOC_FILE_NAME_TOO_LONG`。
- [x] `Fix` P2-9 错误码对位：PARSE 步失败面客码改用 `ERR_AP_DOC_PARSE_FAILED`；DRAFT 步保持 `ERR_AP_DOC_DRAFT_FAILED`；核查 CLASSIFY 步面客码语义对位（如缺专属码则登记复用决策）。Skill: `nop-backend-dev`
      → 完成：`fail()` 按 step 映射面客码（PARSE → parse-failed，其余 → draft-failed）；CLASSIFY 步复用 `ERR_AP_DOC_DRAFT_FAILED` **复用裁决**：CLASSIFY 失败唯一触发点为「无分类引擎注册」部署异常，分类是草稿前置步骤（失败即草稿无法生成），DRAFT_FAILED 为语义最近既有码，不为部署异常新增专属面客码（代码注释 + 本注记双登记）。
- [x] `Decision` P2-1 重复上传幂等策略：方案 A（采用倾向）= 应用层轻量守卫——upload 时按内容哈希 + 文件名查非终态（RECEIVED/PARSED/CLASSIFIED/DRAFTED）同文件文档，命中则拒绝并指向既有文档（错误码 + docId 参数），并发窗口残留风险登记 owner doc；方案 B = 去重责任后移三单匹配 + owner doc 显式登记该裁决（零代码）。两案择一落文档；若选 A 须同落 owner doc「最终权威去重 = 三单匹配」边界说明。Skill: `nop-backend-dev`
      → 裁决：**选 A**（理由 + 方案 B 被拒理由 + 并发残留风险 + 「最终权威去重 = 三单匹配」边界，登记于 `document-driven-ap-automation.md §P2 加固裁决登记`）。
- [x] `Add` P2-1 守卫落地（若裁决选 A）：零 ORM 实现上述守卫；若选 B 则本项转为 owner doc 登记项。Skill: `nop-backend-dev`
      → 完成：`rejectDuplicateUpload`（fileName eq + status in 非终态 → fileLength 短路 → SHA-256 内容摘要比对；候选文件读取失败视为非重复不阻断上传，WARN 留痕）；错误码 `ERR_AP_DOC_DUPLICATE_UPLOAD`（fileName/documentId/status 参数）。
- [x] `Decision` P2-3 dict ARCHIVED 归属：方案 A（采用倾向）= 保留 dict 值 + owner doc 登记「ARCHIVED writer 随邮件摄取（暂缓 Non-Goal）落地，触发条件显式」——守卫 :157 消费语义保留；方案 B = 移除 dict 值 + 守卫简化（邮件摄取落地时回加）。裁决理由与替代方案记入计划执行注记 + owner doc。Skill: `nop-backend-dev`
      → 裁决：**选 A**（writer 归属 = 邮件摄取自动归档，触发条件显式登记；方案 B 被拒理由 = dict/守卫回加需跨批次协调 schema 与状态机漂移且消费侧语义正确；登记于 `document-driven-ap-automation.md §P2 加固裁决登记`）。
- [x] `Fix` P2-6 分类确定性：伙伴查询加确定性排序（如 orderBy id）；查询侧下推名称过滤（contains 语义映射为 ORM 查询或登记内存扫描保留理由）；超 2000 截断显式化（WARN 日志含命中数/上限，行为保持落人工门但可观测）；`classifyDocType` 补 `"null"` 字面守卫（与 `matchPartner` :98 对齐，收口幻影路径）。Skill: `nop-backend-dev`
      → 完成：候选查询 `addOrderField("id", asc)` + **执行注记①名称过滤保留内存扫描**（双向包含无法下推为单一查询条件而不丢失反向包含命中，理由登记 owner doc §P2 加固裁决登记）；**执行注记②发现并修复 `findAllByQuery` 忽略 `QueryBean.limit` 的平台事实**（`OrmEntityDao.findAllByQuery` 不消费 limit——原 `setLimit(2000)` 实为无界加载，审计 P2-6「静默截断」前提不成立），改用 `findPageByQuery` 使上界真实生效 + WARN（含 hit/limit，config `erp-fin.ap-doc-partner-match-limit` 默认 2000）；`classify()` 要素提取 null 值直取 null（不再经 `String.valueOf` 产生字面 "null"）+ `classifyDocType`/`matchPartner` 双 `"null"` 守卫。
- [x] `Fix` P2-7 OCR 可观测与上界：catch 分支补 WARN 日志（区分损坏 PDF 与无文本扫描件——日志含 fileName/mimeType/异常摘要，返回 null 语义不变）；PDFBox 页数上限 config-gate（`ErpFinConfigs` 新配置项，默认上限值取安全常数，超限 WARN + 返回 null 落人工门）。Skill: `nop-backend-dev`
      → 完成：WARN `erp-fin-ap-doc-pdf-extract-failed`（fileName/mimeType/reason）；新配置 `erp-fin.ap-doc-pdf-max-pages` 默认 50，超限 WARN `erp-fin-ap-doc-pdf-pages-exceeded`（含 pages/maxPages）+ 返回 null。
- [x] `Fix` P2-11 限流措辞对齐：`ai-native-interface.md:74` item 4「人类用户面默认无限流」修正为实际语义（限流落点 = 管道 upload 单入口，对所有调用方默认 10rps、0=不限流；human 通用面无全局限流）——默认值不改（守卫强度不降）。Skill: `nop-backend-dev`
      → 完成：item 4 措辞修正（保留冻结清单历史锚 + P2-11 修正标记），默认 10rps 未改。
- [x] `Fix` P2-15 `ErpFinBankReconAutoReverseHelper.java:117` `LocalDate.now()` → `CoreMetrics.currentTimeMillis()` 转换（LocalDate 等价构造），行为零变化。Skill: `nop-backend-dev`
      → 完成：`CoreMetrics.currentDate().withDayOfMonth(1)`（CoreMetrics 既有 LocalDate 工厂，等价构造），既有 `TestErpFinBankReconAutoReverseJob` 5 用例零回归。

Exit Criteria:

- [x] P2-8/P2-9 负路径可观测：非法扩展名/超长文件名/非法 base64 上传均以 NopException 专属参数面客（错误码 + 参数），不再裸 IllegalArgumentException；PARSE 失败面客码 = `ERR_AP_DOC_PARSE_FAILED`（Proof 载体 = `TestErpFinApDocumentPipeline` 家族负路径用例）
      → `testUploadContractNegativePaths`（exe/docx/超长/非法 base64 四负路径 + png 合法分支）+ `testFailedStatusPersistedAfterSyncProcessFailure` 断言更新为 parse-failed，10/10 绿。
- [x] P2-6/P2-7 行为证明：同输入两次分类结果确定一致（排序后首匹配稳定）；超 2000 伙伴与 OCR 异常路径均有 WARN 日志（测试或日志断言）
      → fin-service 新增 `TestErpFinApDocRuleClassifierDeterminism`（4 用例：两次分类一致/双向包含保留/SUPPLIER 优先/null 无幻影/截断 WARN logback 断言）+ `TestErpFinTextExtractOcrEngineObservability`（3 用例：损坏 PDF WARN/页数上限 gate WARN/合法路径回归），7/7 绿。
- [x] P2-1/P2-3 裁决已记录（计划执行注记 + owner doc 对应章节），若选守卫则重复上传被拒且指向既有文档
      → 裁决登记 owner doc §P2 加固裁决登记；`testDuplicateUploadRejectedPointingToExisting`（重传被拒 + 指向既有 docId + 同名不同内容放行 + 终态放行）绿。
- [x] 本地化验证（后续依赖）：`mvn test -pl module-finance/erp-fin-service` 全绿
      → 529 tests, 0 failures（2026-08-28）。

### Phase 2 - 鉴权 E2E / 接线覆盖 / 收口登记

Status: completed
Targets: `tests/e2e/business-actions/fin-ap-document.value.spec.ts`（或 `ai-interface.value.spec.ts`，按账号池范式归属择一）、`module-finance` 或 `app-erp-all` 测试树、`docs/plans/2026-08-26-0735-2-e3-integrated-implementation.md`（证据归因注记）、`docs/backlog/erp-enhancement-roadmap.md`、`docs/logs/2026/`
Skill: `nop-testing`

- Item Types: `Proof | Fix`
- Prereqs: Phase 1

- [x] `Proof` P2-E 鉴权拒绝路径 E2E：无权限角色经 `/r/` 调 `uploadApDocument` 被拒（复用 permissions 账号池范式，参照 `ai-interface.value.spec.ts` 既有负路径结构）。Skill: `nop-testing`
      → 完成：归属择一 = `ai-interface.value.spec.ts`（role-restricted REST 登录 + `/r/` 负路径范式所在）；新用例 `uploadApDocument denied for unauthorized role via /r/ channel (P2-E)` 断言非零 status + 「没有访问权限」token（FNPT enforcement，非业务守卫）；spec 头注 item 4 归因修正同步。
- [x] `Fix` E3 计划 Phase 7 证据归因修正：`2026-08-26-0735-2` Phase 7 ④ 处补注记（原负路径实测 `ErpFinBadDebt__writeOff` 既有 mutation；新 mutation 面拒绝路径由本计划 E2E 补齐，归因修正不改变原验收结论）。Skill: `none`
      → 完成：Phase 7 ①④ 行下 post-hoc 注记（引用本计划 plan-id；「IBiz 同步」空指注记归 plan 0219-3，无重叠）。
- [x] `Proof` P2-14 fin 部分：ap-document.batch.xml + job yaml 三件套接线覆盖测试（断言 batch 资源存在/bean 解析/job 注册形态，参照既有 fin batch 家族测试或新增最小接线断言）；`TestErpFinApDocumentPipeline` 鉴权面缺口以 P2-E E2E 承载（IT 保持 `enableActionAuth=FALSE` 的理由补注记）。Skill: `nop-testing`
      → 完成：新增 `TestErpFinApDocBatchWiring`（app-erp-all）——batch 真实执行（RECEIVED → MANUAL_REVIEW + PARSE/MANUAL_REVIEW 轨迹）+ job yaml 解析形态断言（jobName/cronExpr/invoker nopBatchTaskRunner.executeAsync/params.taskPath 指向 batch 资源）。**执行期发现并修复 batch.xml 两处运行时缺陷**：①`inject('IErpFinApDocumentBiz')` 非注册 bean id（unknown-bean-for-name，job 首跑即失败）；②loader 恒返回单哨兵行 → 无限 chunk 循环（job 永不结束）——改 orm-reader RECEIVED 触发扫描 + bean id 注入（processor 仍调 `processPending`，P1-2 per-item REQUIRES_NEW 隔离保持）。`TestErpFinApDocumentPipeline` 类 javadoc 补 `enableActionAuth=FALSE` 理由注记（鉴权面由 P2-E E2E 承载）。
- [x] `Proof` scoped 复跑：`mvn test -pl module-finance/erp-fin-service` + `mvn test -pl app-erp-all`；受影响 E2E flux 复跑（fin-ap-document + ai-interface 两 spec 全绿）。Skill: `nop-testing`
      → fin-service **529/529**（+7 新用例）；app-erp-all **68 tests / 0 failures / 1 pre-existing skipped**；E2E 两 spec **6/6 绿**（含 P2-E 新用例；runner jar 随 `mvn clean install -DskipTests` 156 模块 BUILD SUCCESS 重建）。
- [x] `Fix` roadmap §Follow-up Backlog 勾销：P2-1/P2-3/P2-6/P2-7/P2-8/P2-9/P2-11/P2-15/P2-E 行及 P2-14 行 fin 部分注记，附本计划引用与日期；compliance checker 复跑（新增生产代码站点须 actual ≤ baseline，漂移则按已知失败模式登记）。Skill: `none`
      → 9 行勾销 + P2-14 fin 部分注记（附 plan-id）；checker 复跑全 19 规则 actual ≤ baseline（R2b=239≤240 改善，其余全等值——新生产代码零新增 daoFor/@Inject 站点，符合预期），零漂移。
- [x] `Proof` 日志条目（`docs/logs/2026/08-28.md` 或执行当日，含验证状态）。Skill: `none`
      → 日志条目落 `docs/logs/2026/08-28.md`（Fix/Proof/验证全绿/Decision 登记/收口五段，含验证状态）。

Exit Criteria:

- [x] E2E 新增负路径用例绿且指向 `uploadApDocument`（非 BadDebt）
      → `ai-interface.value.spec.ts` P2-E 用例 6/6 批次内绿（1 新 + 5 既有回归）。
- [x] batch/job 接线测试存在且绿
      → `TestErpFinApDocBatchWiring` 2/2 绿（并驱动修复 batch.xml 两处缺陷）。
- [x] roadmap 对应行勾销、checker actual ≤ baseline、日志落盘
      → 9 行 `- [x]` + P2-14 注记；19 规则全 ≤ baseline；`docs/logs/2026/08-28.md` 落盘。

## Draft Review Record

- Independent draft review iteration 1: **acceptable-as-is（4 MINOR 采纳修订）**（task `ses_fbb8a357affed4ZBlnZWmDDH8n`，fresh session）——16+ 基线主张活仓复核全吻合（含 zero-ORM 约束事实性：orm 无唯一索引、§8.1 授权已消费；范围划分与 0219-2/0219-3 零重叠；反松弛扫描零命中；两个 Decision 项均有替代方案）。4 MINOR 已修：①P2-8/P2-9 退出标准补 Proof 载体（TestErpFinApDocumentPipeline 家族负路径用例）；②P2-8 白名单「等」改为执行时枚举定稿 + png/jpg 过白名单后 null 落人工门为合法分支的说明；③行号漂移更正（LocalDate.now 实为 :118；uploadApDocument ~:31-38；action-auth 全路径 `_vfs/erp/fin/auth/`）；④Phase 1/2 测试载体分工注记（Phase 1 fin-service 单测、Phase 2 app-erp-all IT——符合指南规则 7 局部化解锁）。

## Closure Gates

- [x] 范围内行为完成（Phase 1/2 全部项目落地，P2-1/P2-3 裁决有记录）
- [x] 相关文档对齐（`document-driven-ap-automation.md` 幂等/ARCHIVED 登记、`ai-native-interface.md` 限流措辞、E3 计划归因注记）
- [x] 已运行验证（scoped mvn test + 受影响 E2E flux + compliance checker）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

（无——P2-1/P2-3 为 Decision 项，裁决在范围内完成；并发去重窗口残留风险随裁决登记 owner doc，非范围裁剪）

## Closure

Status Note: 执行完毕（2026-08-28）。Phase 1（入口契约/幂等守卫/确定性/可观测修复 + owner doc 裁决登记）与 Phase 2（P2-E 鉴权 E2E + P2-14 接线覆盖 + 收口登记）全部落地。执行期额外发现并修复三处审计未识别缺陷：①`findAllByQuery` 忽略 `QueryBean.limit`（平台事实，原 setLimit(2000) 实为无界加载）；②`ap-document.batch.xml` 按接口名 inject 非注册 bean id（job 首跑即 unknown-bean-for-name）；③batch loader 恒返回哨兵行致无限 chunk 循环（job 永不结束）。验证：`mvn clean install -DskipTests` 156 模块 BUILD SUCCESS；fin-service 529/529、app-erp-all 68 tests 0 failures（1 pre-existing skipped）、E2E 两 spec 6/6 绿（含 P2-E 新用例）；compliance checker 19 规则全 actual ≤ baseline 零漂移。roadmap §Follow-up Backlog 9 行勾销 + P2-14 fin 部分注记。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（fresh session，task `ses_fbb32e619ffeO707VRiSLDHa0N`，2026-08-28）
- Evidence: **verdict = acceptable**（20 项核验全通过：Phase 1 六文件代码面逐行核验含白名单枚举与 owner doc 一致性、fail() step 映射、SHA-256 守卫、findPageByQuery + id 升序；Phase 2 E2E/IT/batch.xml/E3 注记/roadmap/log 逐项在盘证据；surefire 报告 4+3+10+2 全 0 failures 且时间戳属本轮；反松弛扫描零降级、两 Decision 双载体登记、Deferred=无；3 MINOR 非阻塞——529 vs 521 报告口径差 8 为该模块 jqwik 容器计数既有惯例、报告文件 FQN 命名、E2E 通过态无残留产物）。无阻塞 discrepancy。

Follow-up:

- （无预留；未消费项已显式归属 plan 2/3/mission VERIFY 批/人工窗口）
