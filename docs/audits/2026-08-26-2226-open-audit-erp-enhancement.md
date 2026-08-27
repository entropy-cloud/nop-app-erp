# 开放式审计：mission `erp-enhancement`

> Audit Status: triaged（本审计自有发现全部 P2，无 P0/P1——已分流至 `docs/backlog/erp-enhancement-roadmap.md §Follow-up Backlog`，2026-08-27；其对多面审计 P1 的复证支持由多面审计自身状态承载）
> Audit Type: open-ended
> Mission: erp-enhancement

- 审计对象：mission `erp-enhancement` 的整件上下文——E1/E2 已闭项 + E3 整体实现（plan `2026-08-26-0735-2`，completed）+ compliance 基线裁决 successor（plan `2026-08-27-1540-1`，completed）+ roadmap/owner docs/mission 注册/日志/测试/E2E/种子/配置的交集，聚焦标准检查清单之外的隐藏问题。
- 审计方法：按 `docs/skills/open-ended-audit-prompt.md` + `docs/skills/README.md §项目定制化层`（保护区域 / 验证命令 / 命名约定 / 13 项已知失败模式注入）。与同批次多面审计 `2026-08-26-2226-multi-audit-erp-enhancement.md`（3 子代理深查 + 6 P1/16 P2）互补：本审计聚焦其未覆盖的**注册面 / 路由层 / 种子与配置契约 / 基线权威源 / 证据归因**，并对其 P1 关键机制做独立代码复证。
- 审计时点：2026-08-27，HEAD `dcbc09fdf` + 工作树（5 个 ai-check docs 修改未提交，属另一 mission 流，watch-only）。
- 本会话独立复跑证据：compliance checker 实测 R2b=240 / R2c=1536 / R7=0，与 successor 更新后基线逐行一致（零漂移复证）；i18n-coverage-checker PASS（390 files，0 defects）。

## 裁决

**passes open-ended audit（本审计维度内无新增阻塞问题）**——但 mission 整体仍处于同批多面审计登记的 **needs revision** 状态（6 项 P1），本审计独立复证其中 P1-1/P1-2/P1-3 的代码机制成立，该 needs-revision 裁决维持不变。本审计新增 6 项 P2（无 P0/P1），按分流规则归 follow-up backlog，不单独驱动 remediation plan，但建议随 P1 修复簇或文档维护批顺带收口。

## 独立复证记录（对多面审计关键 P1 的对抗性复核，非新发现）

- **P1-1 复证成立**：`ErpFinApDocumentPipelineProcessor.fail()`（:414-422）置 FAILED + `saveOrUpdateEntity` + FAIL 轨迹行后**返回** NopException 由 `readFile`/`draft` 等 `throw`（:445/:455/:167/:202/:214）；调用入口 `processApDocument` 为 `@BizMutation`（自动事务回滚），`retry()` 的 FAILED 守卫（:284）在同步路径不可达。
- **P1-2 复证成立**：`processPending`（:296-308）`for (doc : pending) process(...)` 无逐项 try/catch，队头失败回滚整批并饿死后续 RECEIVED。
- **P1-3 复证成立**：`upload()`（:104-129）仅 `acquireUploadPermit()`（限流），无 `requirePipelineEnabled`；`process/confirmAndDraft/retry/processPending` 四入口均有门（:134/:256/:282/:297）。
- **E2E config-gate 接线证真**：`playwright.config.ts:18` 与 `_tmp-server.sh:57` 实际携带 `-Derp-fin.ap-doc-pipeline-enabled=true`（本审计曾因 grep 显示替换误判键名截断，`grep -F` 复核排除——非发现，留记防复发）。

## 按严重性排序的发现（本审计新增，全部 P2）

- **[P2] `job-scheduling.md` 漏登记生产 nop-job `erp-fin-ap-doc-processing`**——owner-doc 注册面缺口，同批两作业注册纪律不一致。
  `docs/architecture/job-scheduling.md` 全文 grep「ap-doc / ap-document / 摄取 / 文档管道」零命中，而 E3.5 已落地生产作业三件套（`app-erp-all/.../job/conf/erp-fin-ap-doc-processing.job.yaml`（cron 每 5 分钟，`@cfg:...|false` 默认关）+ `fin/ap-document.batch.xml`）。同批 E3.2 的 `erp-inv-stock-check` 已按规矩登记 §3.3 REGISTERED→WIRED（job-scheduling.md:131），E3.5 作业却未登记——job-scheduling.md 是调度真相源 owner doc，缺行将使后续调度审计/去重决策失去事实基础（含 `nop.job.erp-fin-ap-doc-processing.enabled/.cron-expr` 两个部署键无处可查）。E3 计划 Phase 6/Phase 8 的文档对齐清单均未列 job-scheduling.md，属计划自身遗漏。（维度：owner-doc 对齐）

- **[P2] 设计路由层 README「暂不编码」标注陈旧 ×6**——E1 七份设计文档中五份已随 E3 实现，README 索引仍标「暂不编码」。
  `docs/design/README.md:40-41`（ai-native-interface / dashboard-semantic-layer）、`docs/design/finance/README.md:116`（document-driven-ap-automation）、`docs/design/aps/README.md:91`（constraint-based-planning）、`docs/design/inventory/README.md:131`（audit-snapshot-cycle-count）、`docs/design/assets/README.md:80`（audit-trail-and-custom-fieldsets）均停留在「2026-08-12 批次，暂不编码」，而对应落地策略表/roadmap 已 done（2026-08-26/27）。E3 Phase 8 更新了 6 份 owner doc 正文 + roadmap + mission JSON，却漏掉路由层 README——`docs/design/README.md §业务域设计文档` 是 index.md 点名的权威域表，读者按它路由会得出「设计-only」错误结论（lesson 13「owner doc 表述过时未随实现更新」同族）。（维度：owner-doc 对齐 / 路由）

- **[P2] `data-dependency-matrix.md` finance 行特征化漂移：fin→pur 只读（R）表述已失实**——E3.5 新增 fin→pur **经 IBiz command 写**边未登记。
  矩阵 finance 行（:81）称「master-data / purchase / sales … **全部 R，经 I*Biz 只读查源单**」；E3.5 `ErpFinApDocumentPipelineProcessor.draft()`（:200-212）经 `IErpPurInvoiceBiz.save()` **创建**采购发票草稿——架构上合法（§「禁止反向 S 写」:284 明示 finance 经 I*Biz command 编排触发他域自管实体写为认可形态，期末结账同型），但矩阵的 R-only 特征化与 §2.4 边登记均未更新。与多面审计 P2-13（fin→md 只读边 / aps→mfg SPI 边缺行）同类且互补：本行是**语义漂移**（既有行特征过时）而非仅缺新行，合并登记可一次收口。（维度：架构边界 / owner-doc 对齐）

- **[P2] `known-good-baselines.md` 未落 E3 批全绿行——权威计数锚 3889 已落后于 E3 新增测试**。
  最新行（2026-08-27 晨，ai-check F1-F2 批）为 3889 tests / 658 报告；E3 批新增 5 测试类 23 用例（Ast 6 / Aps 4 / FinPipeline 6 / AiIntrospection×2 / InvSnapshot 5）后，E3 VERIFY（08-27 午后）在 `docs/logs/2026/08-27.md:18` 记录全量绿但**无计数**，known-good-baselines 亦无对应行。下一轮全量回归的差量归因将以 3889 为锚看到无法归属的 +N——正是 lesson 13「计数型断言指向权威计数源」要防的伪事实传播。（维度：验证充分性 / 基线权威源）

- **[P2] 六个新 mutation 的鉴权面零有效覆盖，且 Phase 7 证据存在归因漂移**。
  E3 计划 Phase 7 以「无权限角色经 /r/ 调高影响 mutation 被拒」作为护栏证据，但 `ai-interface.value.spec.ts` 的负路径实际测的是**既有** `ErpFinBadDebt__writeOff`（spec 头注 item 4 自证），与新增 API 面无关；`TestErpFinApDocumentPipeline` 又以 `enableActionAuth=FALSE` 运行。结果：`uploadApDocument/processApDocument/confirmAndDraftApDocument/retry/processPending/scheduleToc` 六个新 mutation 的 action-auth 种子（`erp-fin.action-auth.xml:82-86` 已正确登记）**从未被任何测试执行过拒绝路径**——叠加多面审计 P1-3（upload 缺 config 门），新 API 面的现行护栏仅剩认证 + 限流。本项锐化多面审计 P2-14（其表述「仅靠 E2E 单负路径覆盖」高估了覆盖——该负路径不指向新 mutation）。（维度：验证充分性）

- **[P2] `AGENTS.md` BizModel 包名示例与全仓实态不符（存量漂移，非本批引入）**。
  AGENTS.md「命名约定」示例 `io.github.nop.app.erp.<domain>.service`（如 purchase）；实际全仓自 codegen 起即为 `app.erp.<short>.service`（`app.erp.pur.service` / `app.erp.fin.service` / `app.erp.ast.service`，本批 E3 全部新类亦然）。按 AGENTS.md 字面示例新建类的代理会产出错误包路径。建议随下次 AGENTS.md 维护修正示例（其馀命名项——`Erp<Domain>` 实体前缀 / `erp_<short>_` 表前缀 / `erp.err.<short>` ErrorCode（本批 `erp.err.fin.ap-doc.*` 实证合规）/ `erp-<short>/<dict>` 字典——均与实态一致）。（维度：约定一致性）

### Watch-only（已由多面审计 P2-16 登记，本审计复核仍在，不重复计数）

- 工作树遗留：`module-projects/erp-prj-service/_cases/.../TestErpPrjDummyProbe/` + `cases-retention.bak/` 未跟踪脚手架；5 个 ai-check docs 修改未提交（属另一 mission 流）。

## 已知失败模式 13 项针对性扫描结论

- **#9 compliance 基线漂移**：本会话独立复跑 checker，R2b=240/R2c=1536/R7=0 与 successor 基线逐行一致——正规收敛复证，无残余漂移。
- **#10 closure-pending**：E3 计划与 successor 计划均具独立子代理 closure audit 证据（fresh session task 指针 + 五点证据，plan `2026-08-27-1540-1:219-222` / `2026-08-26-0735-2:302-305`）；E2 计划 completed。无欠账。
- **#11 业财过账吞异常悬挂**：多面审计 P1-1 为其反向变体（失败状态被回滚丢失）并复证成立；本审计无新增同族站点。
- **#12 dict 死状态**：多面审计 P2-3（`erp-fin/ap-doc-status` ARCHIVED 无 writer）在案；本审计无新增（`erp-ast/audit-event-type` 7 值均有 recorder 写点，多面审计 ORM 维度已核）。
- **#13 arm-index 状态不回填**：arm-index 属 audit-remediation mission 范围；本批审计发现尚未进入任何追踪索引，属 remediation-plan 后续动作（进行中状态，非欠账）。
- **#1-#8 微模式**：E3 九文件本会话 grep 复扫（System.currentTimeMillis/LocalDateTime.now/@Inject private/extends RuntimeException）零命中；`erp-fin.n-enabled` 疑似键名截断经 `grep -F` 证伪（rg `-r` 显示替换所致）。多面审计 P2-15（`ErpFinBankReconAutoReverseHelper` LocalDate.now）为 E3 外相邻发现，维持。

## 按维度裁决（反窄化自检：每维度至少一句）

| 维度 | 裁决 |
|------|------|
| 需求正确性 | 本审计无新增（P1 级需求-实现分歧已由多面审计 6 项 P1 登记且关键三条经本审计代码复证；E3 冻结清单 6 项逐项证据抽查——11 域 @Description 实测 11 文件在——属实）。 |
| owner-doc 对齐 | **有发现（P2 ×3：job-scheduling 缺行 / README 路由层暂不编码陈旧 / 依赖矩阵 fin 行 R-only 失真）**——正文书写的对齐义务履行了（6 份 owner doc + roadmap + mission JSON），漂移集中在**注册与路由层**（job 注册表、README 索引、依赖矩阵），均为计划文档清单未枚举的次级真相源。 |
| 架构或边界影响 | 无新增 DAG 环；fin→pur command 写边架构合法但登记缺失（上述 P2）；ORM 变更全程在 §8.1 授权 + dual-agent-approval 双 fresh-session 记录内（plan :108-113 迭代记录完整含 REJECT→修订→双 APPROVE），保护区域合规。 |
| 验证充分性 | **有发现（P2 ×2：known-good-baselines 锚落后 / 新 mutation 鉴权零覆盖 + Phase 7 证据归因漂移）**；mission 四路验证声明可采信（checker 本会话复证 + 构建/测试/E2E 落盘证据），但权威计数源与鉴权负路径两处证明链有洞。 |
| 回归风险 | 无新增（毒文档重扫环 / 队头阻塞等已由 P1-1/2 载明；E2E config-gate JVM args 接线证真；`_dump/`、`nop-file-store/` 均 git-ignore 实证）。 |
| 路由和技能选择正确性 | 无发现——E3 单一整体计划守 roadmap §9 规则 2；任务类型标注（implementation-only + E3.1b verification 子项）与阶段 Skill（nop-backend-dev/nop-frontend-dev/nop-testing/none 逐项落 plan）匹配；successor 计划按 known failure mode 裁决路径正规开出而非内联上调基线。 |
| 待办或自主权策略漂移 | 无发现——E3.7 保持 todo 暂缓未静默解禁；roadmap §2 计数（done 18 / todo 1）与实态一致；Deferred 项均带触发条件；工作树遗留 watch-only（多面审计已登记）。 |
| 流程合规（计划/审计门控） | 无发现——两计划均独立草案审查（含 REJECT 迭代）+ 独立结束审计留 task 指针；日志按日落盘含验证状态与 full-green 提交注记。 |

## 修复建议路由（供 backlog 分流，P2-only 不驱动独立 remediation plan）

1. **文档注册批**（P2-A/B/C）：job-scheduling.md 补 `erp-fin-ap-doc-processing` 行（对齐 :131 inv 行格式）；五份 README「暂不编码」→「已实现（E3.x，2026-08-26/27）」；data-dependency-matrix fin 行 R-only 表述修正如 §2.4 补 fin→pur command 写边（可与多面审计 P2-13 两行合并一次收口）。随 P1 修复簇的文档面或 architecture doc 维护批执行。
2. **P2-D**：下次全量验证时在 known-good-baselines.md 落 E3 后全绿行（含计数差量归因 +23），恢复计数锚。
3. **P2-E**：随 P1-3 修复簇补一条新 mutation（建议 uploadApDocument）无权限角色拒绝路径 E2E，并修正 E3 计划 Phase 7 证据表述（「高影响 mutation」点名 BadDebt）或补记限制。
4. **P2-F**：AGENTS.md 命名约定示例修正，随下次人工维护窗口。

## 剩余未知数（open-ended 残余警惕项）

- 全量 `mvn test` 与 flux E2E 19 用例本会话未整体重跑（采信 08-27 落盘证据 + checker 独立复证）；P1 修复后按多面审计复审要求四路重跑时，known-good-baselines 锚（P2-D）将实际受压。
- nop-entropy 平台实态（nop-datav/nop-metadata master 合入声明）未跨仓复核——超出本仓 `./` 范围，采信 2026-08-26/27 两轮活仓核实记录。
- `TestErpFinApDocumentPipeline` 在 app-erp-all IT 层运行，分域 `erp-fin-app` 独立运行时 nop-file/beans 装配面未验证（分域 app 独立部署形态本就非当前验证标准，记录为残余未知）。
