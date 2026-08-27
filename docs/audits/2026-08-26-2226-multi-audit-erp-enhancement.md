# 多维审计：mission `erp-enhancement`（E3 整体实现 + successor 基线裁决）

> Audit Status: planned（P1-1/2/3 → plan `2026-08-27-2006-1`，P1-4/5 → plan `2026-08-27-2006-2`，P1-6 → plan `2026-08-27-2006-3`；P2-1..16 → `erp-enhancement-roadmap.md §Follow-up Backlog`，2026-08-27）
> Audit Type: multi-dimensional
> Mission: erp-enhancement

- 审计对象：mission `erp-enhancement` 的**整件工作**——E1/E2 已闭项复核 + E3 整体实现（plan `2026-08-26-0735-2`，已完成关闭）+ compliance 基线漂移裁决 successor（plan `2026-08-27-1540-1`，已完成关闭），聚焦 `./` 代码、配置、测试与公共契约（exports / API 面）。
- 审计方法：按 `docs/skills/multi-dimensional-audit-prompt.md` + `docs/skills/README.md §项目定制化层`（保护区域 / 验证命令 / 命名约定 / 13 项已知失败模式注入）。3 个独立深查子代理（E3.5 管道 / E3.2+3.3+3.4+3.8 / E3.6+契约面）+ 主审计者活仓复跑机械核验（compliance checker、ORM/dict 真值、view 契约、关键 P1 逐行证真）。
- 审计时点：2026-08-27（HEAD `dcbc09fdf`）。验证声明采信方式：compliance checker 本会话独立复跑（实测 R2b=240 / R2c=1536 / R7=0，与更新后基线**逐行一致，零漂移达成**）；构建/全量 mvn test / E2E 按 `docs/logs/2026/08-27.md` 落盘证据采信（156 模块 BUILD SUCCESS；3889 tests 绿含 1 例已裁决瞬态 flake；flux E2E 19 用例绿），未整体重跑全量套件。

## 裁决

**needs revision** —— 6 项 P1（E3.5 失败路径事务回滚 ×2 / 上传面 config-gate 缺失 / E3.2 对账键维度缺失 / E3.4 瓶颈识别窗口上界缺失 / introspection 全域 app 配置与 owner doc 断言漂移），均须修复。两份已关闭计划的闭合审计未拦截本批 P1，根因集中于「失败路径与多组织/越界输入无测试暴露」（见验证充分性维度）。

## 按严重性排序的发现

### P1（必须修复）

- **[P1-1] E3.5 失败落账被事务回滚吞掉——`FAILED` 状态与 `FAIL` 轨迹行在同步/异步两条路径均不持久化。**
  `module-finance/erp-fin-service/.../processor/ErpFinApDocumentPipelineProcessor.java:414-422`：`fail()` 先 `setStatus("FAILED")` + `saveOrUpdateEntity` + `log(...,"FAIL",false,...)`，随后返回的 `NopException` 被 `throw`（`readFile`/`classifyAndDraft` 等 :445/:455 消费）。调用入口 `processApDocument` 为 `@BizMutation`（自动事务，RuntimeException 回滚）；异步载体 `ap-document.batch.xml` `transactionScope="process"` 整批单事务。结果：文档回滚为 `RECEIVED`、FAIL 轨迹行丢失、`retry()` 的 `FAILED` 状态守卫（:284）成为死分支（同步路径永远无法进入 FAILED）、异步路径毒文档每轮被 `RECEIVED` 扫描（limit 100）永久重扫。直接违背 plan 声明「处理轨迹落账」（:219）与 AP-4 审计追溯设计。本仓先例 `2026-08-14-1815-2`（bank-recon/crm）已用 REQUIRES_NEW + try/catch helper 解决同型 nop-batch 语义，本管道两者皆无。
  （维度：需求正确性 / 回归风险）

- **[P1-2] E3.5 `processPending` 无逐项隔离——首个失败文档回滚并饿死整批。**
  同文件 :296-308：`for (doc : pending) { process(doc.getId(), context); }` 无 per-item try/catch。一项抛异常 → 同事务内先行文档的 PARSED/CLASSIFIED/DRAFTED 翻转全部回滚，队列后续 RECEIVED 文档本轮全部得不到处理（队头阻塞）。与 P1-1 同根（nop-batch process scope 无 per-item 隔离），修复面不同，故单列。
  （维度：回归风险）

- **[P1-3] E3.5 `upload()` 未挂 `erp-fin.ap-doc-pipeline-enabled` 门——「默认关闭零暴露」声明失实。**
  同文件 :104-129：`process/confirmAndDraft/retry/processPending` 均调 `requirePipelineEnabled`（:134/:256/:282/:297），`upload()` 只挂限流不挂门。管道关闭时上传 API 面 + nop-file 写入 + `RECEIVED` 行照常暴露。测试自证：`TestErpFinApDocumentPipeline.testPipelineDisabledByDefault`（:201-210）在门关闭下成功 upload，仅断言 `processApDocument` 被拒——plan :219「默认关闭零暴露」与实现漂移。
  （维度：需求正确性 / owner-doc 对齐）

- **[P1-4] E3.2 对账键维度缺失——`balanceKey` 丢 `orgId`/`ownerId`，多组织/多货主场景假差异与真差异遮蔽并存。**
  `module-inventory/erp-inv-service/.../entity/ErpInvStockLedgerBizModel.java:173-180`：比对键 dims = `{warehouseId,locationId,materialId,skuId,batchNo}`；而派生聚合 `SNAPSHOT_DIMS`（:47-48）与余额表自然键 `UK_INV_STOCK_BALANCE_NATURAL = orgId,materialId,skuId,warehouseId,locationId,batchNo,ownerId`（`app-erp-inventory.orm.xml:415`）均含 orgId/ownerId。多组织/多货主数据下键碰撞 → 假 `BOOK_ONLY_NO_LEDGER`/`LEDGER_ONLY_NO_BALANCE` 且掩盖真实差异——对账校验项（E3.2 核心交付物之二）在多组织下产出错误结论。测试仅种子 orgId="1"，从未暴露。
  （维度：验证充分性 / 需求正确性）

- **[P1-5] E3.4 瓶颈识别窗口缺上界——horizon 之后的工序计入 horizon 负荷 → 虚假瓶颈。**
  `module-aps/erp-aps-service/.../scheduling/ApsBottleneckDetector.java:123-131`：`findPlannedInHorizon` 仅 `ge("plannedEndDateT", horizonStart)`，缺 `le("plannedStartDateT", horizonEnd)`；完全排在 horizon 之后的 PLANNED 工序被计入窗口负荷，负荷率虚高 → `scheduleToc` 误报瓶颈中心。偏离同仓窗口重叠范式（`ErpApsSchedulingProcessor.loadPlannedInWindow:209-210` 双边界齐全）。`TestErpApsSchedulingToc` 无 beyond-horizon 种子用例。
  （维度：需求正确性）

- **[P1-6] 19 个分域 app `application.yaml` 携带 `nop.graphql.schema-introspection.enabled: true`，与 `ai-native-interface.md`「应用当前显式关闭」断言漂移。**
  全部 19 个 `module-*/erp-*-app/src/main/resources/application.yaml:36-38` 为 `enabled: true`（初始 codegen 提交 `15e64237c` 起即如此）；仅 `app-erp-all`（:23-25）为 `false`。E3.6 冻结项①本身成立（app-erp-all 生产工件/E2E 运行时关闭 + JUnit 双证真实），但 owner doc `ai-native-interface.md:65` 的全称断言对 19 个可独立运行 Quarkus app 为假——GraphQL schema 全暴露面。属 E3.6 落地时发现即应修正的契约/文档双漂移（配置翻转或断言收窄二选一）。预置非 E3 回归，故 P1 不 P0。
  （维度：owner-doc 对齐 / 架构边界）

### P2（记录，不单独驱动 remediation plan；随 P1 修复面或 backlog 收口）

- **[P2-1] plan 声明「重复上传幂等」失实**——实现/测试仅有 DRAFTED 重复处理守卫（`ErpFinApDocumentPipelineProcessor.java:136-140` + `TestErpFinApDocumentPipeline` javadoc :48 自证范围）；无重复上传去重（fileId/内容哈希无唯一约束，orm 索引 :2300-2310 无），同文件重传 → 第二 `RECEIVED` → 第二草稿。去重责任是否后移三单匹配未落文档。（需求正确性）
- **[P2-2] `getInventorySnapshot` 绕过 role-row-filter 且未登记入 G1 缺口族**——直连 `ormTemplate.findListByQuery`（enableFilter=false 路径，E3.1b 实测同型）；E3.1b 交付的缺口清单（`dashboard-semantic-layer.md` §4 G1/G2/G3）未收录同计划姊妹 API，缺口登记不完备（对账查询的同类绕过已有 compliance per-site 注记，快照查询无）。修复将随 G1 族同机制收口，故 P2。（owner-doc 对齐 / 验证充分性）
- **[P2-3] dict 死状态 `ARCHIVED`（`erp-fin/ap-doc-status`）**——`process()` 守卫消费（:136-140）但全仓无 writer；owner doc「自动归档」挂在暂缓的邮件摄取（Non-Goal）。lesson-10 家族。（回归风险）
- **[P2-4] E3.3 UPDATE 审计白名单过窄**——`ErpAstAssetBizModel.java:155-158` 仅 `name/brandModel/remark/extFieldValues/modelId` 变更记 UPDATE；`depreciationMethod/depreciationRate/acquisitionDate/categoryId` 等财务敏感字段变更**零审计事件**，E3.8「资产状态/归属变化审计」覆盖面留洞。（需求正确性）
- **[P2-5] 逻辑删除型号可被绑定**——`ErpAstAssetModel` `useLogicalDelete=true`（orm :349），校验钩子（重构后 `asset.getModel()`）不拒已删型号，资产可持续绑定/校验已删字段集。（回归风险）
- **[P2-6] 分类引擎确定性/上界瑕疵**——`ErpFinApDocRuleClassifier.java:104` 每文档加载 ≤2000 伙伴无排序（首匹配不确定；超 2000 静默不匹配落人工门）且查询侧无名称过滤（O(n) 内存扫描）；:46-52 `String.valueOf(null)` → 字面 `"null"` 经 `classifyDocType("null")→OTHER` 的潜在幻影路径。（回归风险）
- **[P2-7] OCR 默认引擎吞异常零日志 + 无页数上限**——`ErpFinTextExtractOcrEngine.java:24-31` `catch(Exception){return null;}` 无日志（损坏 PDF 与扫描件不可区分）；PDFBox 全内存加载 + `PDFTextStripper` 无页上限（20MB 压缩 PDF 的 CPU/内存放大）。（回归风险）
- **[P2-8] 上传入口输入契约松**——无 MIME/扩展名白名单（类型全由客户端 `fileName/mimeType` 决定）；`fileName` 未按列精度 200 截断（orm :2278）；非法 base64 抛裸 `IllegalArgumentException`（:463，非 NopException 范式）。（回归风险）
- **[P2-9] 错误码契约错位**——`ERR_AP_DOC_PARSE_FAILED`（`ErpFinErrors.java:530-532`）定义未用；PARSE 步失败以 `ERR_AP_DOC_DRAFT_FAILED`「草稿发票生成失败」面客（:445/:455），误导排障。（需求正确性）
- **[P2-10] 同计划新增 5 个对外 action 零 `@Description`**——`uploadApDocument`（`ErpFinApDocumentBizModel.java:31-33` + `IErpFinApDocumentBiz.java:20-21`）、`getInventorySnapshot`/`checkStockBalanceConsistency`、`getAssetAuditTrail`、`scheduleToc` 仅 javadoc（不进 GraphQL schema description），违反 E3.6 自己登记的约定「新增对外 action 应带 @Description」（`ai-native-interface.md:72`）。冻结清单只要求 11 个 KPI action，故非清单违约，是约定-实践漂移。（owner-doc 对齐）
- **[P2-11] 限流默认值与措辞漂移**——`ErpFinConfigs.java:23` 默认 10 rps 对**所有**调用方生效（含人类用户），`ai-native-interface.md:74`「人类用户面默认无限流」为字面失实（实际影响趋零）。（owner-doc 对齐）
- **[P2-12] 悬空引用**——`TestErpAiIntrospectionEnabled.java:39-40,54-57` 注释称 AST 单父 workaround「登记于 ai-native-interface.md 实现注记」，该文档无此内容；plan :235「IBiz 同步」为空指（仓内不存在 `IErp*Dashboard*` 接口）。（owner-doc 对齐）
- **[P2-13] `data-dependency-matrix.md` 登记漂移 ×2**——E3.5 fin→md `ErpMdPartner` 代码级只读（`ErpFinApDocRuleClassifier.java:99`）与 E3.4 aps→mfg `IErpMfgCapacityProvider` SPI 边均未登记 §2.4 矩阵行（表级 R 引用 :157 / 设计文档 / javadoc / beans.xml 注释已存在，仅缺矩阵行；aps 行 :90 仍「待深化」）。（架构边界）
- **[P2-14] 测试充分性缺口**——无 FAILED 持久化路径用例（本可暴露 P1-1）；`ap-document.batch.xml` + job yaml 接线零覆盖；MAINTENANCE/DISPOSAL 审计事件零 JUnit 覆盖（recorder 调用点 `ErpAstMaintenanceCompleteWorkProcessor:40`/`ErpAstDisposalProcessor:126` 无测试）；TOC 阈值等值边界（`==` 不应触发）未测；`TestErpInvSnapshotAndStockCheck.java:162-164`「零值等价不报」断言空转（`>= 3` 未断言零值行不出现）；`TestErpFinApDocumentPipeline` `enableActionAuth=FALSE` 使新 mutation 鉴权面仅靠 E2E 单负路径覆盖。（验证充分性）
- **[P2-15] `ErpFinBankReconAutoReverseHelper.java:117` 生产代码 `LocalDate.now()`**——违反 CoreMetrics 强制约定（E3 diff 外、`./` 范围内相邻发现；checker R7 不覆盖 LocalDate.now）。（回归风险）
- **[P2-16] 工作树遗留**——`module-projects/erp-prj-service/_cases/.../TestErpPrjDummyProbe/` 与 `cases-retention.bak/` 未跟踪脚手架仍在工作树（08-27 日志 :30 已登记为故意遗留）；另有 5 个 ai-check 相关 docs 修改未提交（属另一 mission 流）。watch-only。（自主权策略）

## 按维度裁决（反窄化自检：每维度至少一句）

| 维度 | 裁决 |
|------|------|
| 需求正确性 | **有发现（P1-1/2/3/5、P2-1/4/9）**——E3.5 失败/幂等/门控三处实现-声明漂移；E3.4 窗口语义错；E3.3 审计覆盖留洞。E3.2 快照派生口径、E3.6 冻结 6 项、E3.1 目录、E3.8 七事件类型本体与 roadmap/owner doc 对齐（子代理三全项核verified）。 |
| owner-doc 对齐 | **有发现（P1-6、P2-2/10/11/12）**——introspection 断言、G1 清单不完备、@Description 约定漂移、限流措辞、悬空引用。落地策略表 6 份 done 与实态一致；roadmap §2/§5/§6/§8/mission JSON 同步核实一致。 |
| 架构或边界影响 | **有发现（P1-6、P2-13）**——无新 DAG 环；fin→md / aps→mfg 边合法（表级/设计文档背书）但矩阵行缺登记；ORM 变更全程在 §8.1 授权 + dual-agent-approval 双 fresh-session APPROVE 记录内（plan :108-113 迭代记录完整），保护区域合规。 |
| 验证充分性 | **有发现（P1-4、P2-14）**——「三型差异可观测」「TOC 瓶颈可观测」两条验收标准在多组织/越 horizon 输入下为假（「如果它假了，我怎么知道？」→ 现有测试不会知道）；全量四路验证声明本身可复现（checker 本会话独立复跑零漂移；mvn/E2E 证据落盘采信）。 |
| 回归风险 | **有发现（P1-1/2、P2-3/5/6/7/8/15）**——毒文档重扫环、吞异常零日志、输入契约松、dict 死状态、逻辑删除绑定；已知失败模式 13 项逐一针对性扫描：#4/#5/#6/#7/#8（微模式）在 E3 九文件全清洁，#9 compliance 漂移已按 successor 正规收敛（实测复证），#11 过账悬挂族反向变体（失败状态回滚丢失）命中即 P1-1，#12 dict 死状态命中 P2-3，#10/#13 不适用本批。 |
| 路由和技能选择正确性 | **本维度无发现**——任务路由（implementation-only + 核实子项）与阶段技能（nop-backend-dev/nop-frontend-dev/nop-testing/none 逐项落 plan）匹配；单一整体计划守 roadmap 规则 2；计划/结束审计双独立记录在案（本审计不因 P1 发现推翻程序合规，缺陷属测试暴露力而非路由错误）。 |
| 待办或自主权策略漂移 | **本维度无实质发现（P2-16 watch-only）**——E3.7 保持暂缓未静默解禁；Deferred 项均有触发条件登记；roadmap 计数（done 18 / todo 1）与实态一致；无范围无声扩大（P1-3/P2-1 为声明-实现漂移，已在需求正确性维度计分）。 |
| ORM 完整性（项目特定） | **本维度无发现**——4 新实体表前缀正确（`erp_ast_asset_model`/`erp_ast_asset_action_log`/`erp_fin_ap_document(_log)`）、字典真值与 Java 字面量逐一吻合（子代理全对齐核verified）、propId 经构建校验、to-one 显示列在位。 |
| 代码生成纪律（项目特定） | **本维度无发现**——`_gen` 零手改（successor Fix 落点在 service 层单行，git diff 证真）；`daoFor→ORM to-one getter` 重构语义等价有据（orm :226 + `_gen/_ErpAstAsset:1662`）。 |
| view.xml gen-control 契约（项目特定） | **本维度无发现**——新增两 view（`ErpAstAssetModel.view.xml` 仅 textarea gen-control、`ErpFinApDocument.view.xml`）均无 badge 调色板/`<c:script>`，无死状态映射风险面。 |

## 修复建议路由（供 remediation-plan 起草）

1. **E3.5 管道修复簇（P1-1/2/3 + 顺带 P2-1/8/9）**：失败落账改 REQUIRES_NEW 补偿写或 per-item try/catch + 状态先行持久化（对齐 `2026-08-14-1815-2` bank-recon 先例）；`upload()` 补 `requirePipelineEnabled`；补 FAILED 路径 + 双文档并发/重复上传裁决测试。
2. **E3.2/E3.4 单点修复（P1-4/5）**：`balanceKey` 补 orgId/ownerId 两维（对齐 UK 自然键）+ 多组织种子用例；`findPlannedInHorizon` 补 `le("plannedStartDateT", horizonEnd)` + beyond-horizon 种子用例。
3. **introspection 收口（P1-6）**：19 个分域 app yaml 翻 `false`（对齐 app-erp-all）或 owner doc 断言收窄为「聚合工件关闭」，二选一落文档。
4. P2 项分流：P2-4（审计白名单）/P2-2（G1 清单补录）入 erp-enhancement 或 permissions-enforcement follow-up；P2-13 入 architecture doc 维护；P2-15 随下次 fin 域触碰修复；其余随修复簇顺带或 backlog。

## 复审要求

修复 P1 后须：①FAILED 路径/多组织对账/越 horizon 负路径测试先行或同落；②scoped `mvn test` + 受影响 E2E 复跑；③compliance checker 复跑（新增 daoFor 站点如有须开基线裁决 successor，不得内联）；④本文件 Audit Status 改 closed 并回填证据。
