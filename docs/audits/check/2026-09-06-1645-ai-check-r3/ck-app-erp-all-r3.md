# ck-app-erp-all-r3 — app-erp-all U21 横切全格五维符合性审计报告（ai-check-r3 M1.16）

> 工作项：M1.16（U21 × 五维全格 = 5 格；roadmap 文档序最后一个 M1 工作项，plan `2026-09-09-2100-2-m116-app-erp-all-cross-cutting-five-dim-audit.md`）。
> 执行日期：2026-09-09 起草 / 2026-09-10 实跑收官。审计时点 T0：HEAD `8e8ab7fca`（计划起草基线 `060ddab0a` 后两笔推进均为姊妹审计产物提交——`060ddab0a` = M1.15 六单元落盘 + 闭包审计 ACCEPT、`8e8ab7fca` = 2100-1 compliance 基线裁决落盘 + 闭包审计 ACCEPT，生产代码零变化）；脏面 = 1 条 untracked 计划文件（本计划自身），tracked 零修改。Phase 5/6 实跑与 Phase 7 收官时点 2026-09-10 01:49 起（+08:00，T0 会话延续；HEAD 与脏面复核未变，`_tmp/` 为 gitignore scratch）。
> 判定依据（冻结）：`m0-5-audit-checklists.md` §3.3 U21 行焦点程式（B=聚合完整性 / F=菜单 component=FLUX 全局面 + flux 导出门禁 / S=`_init-data` 全量 seed 口径 / T=集成测试快照纪律 + 全 reactor 回归 / I=E2E runbook 合规抽样 + CAT-4 全局面）+ §4 映射表第 16 行（U21 × 五维全格）+ §3.2 聚合横切面唯一归属 + §6 勘误 E1。
> 范围：`app-erp-all/` 聚合面（19 域聚合注册 / POM 聚合 / 菜单注册 / `_init-data` 全量 seed 口径 / C01-C21 集成测试族 / `npm run validate:flux` 导出门禁 / E2E runbook 合规抽样 / 双 checker 红线）——域内页面/seed 内容缺陷归各域格（15 切片已收官），common 抽象族行为归 U20/M1.15（其 F/S=n-a 判定面由本格承接）。Skills：DIM-B `nop-platform-conformance-audit-prompt` + `code-quality-audit-prompt`；DIM-F/S/T/I Skill: none。
> 零生产代码改动：本报告 + 双索引 + 计划勾选注记为唯一产物（Phase 7 `git status` 机械核证）。

## 1. 五维覆盖矩阵（5/5 完整）

| 维度 | 判定面（U21 全格） | 机械核查结果 | verdict |
| --- | --- | --- | --- |
| **DIM-B 聚合完整性** | §1.1 ①④ 机械程式 + 聚合器全量 grep 三面（action-auth x:extends 聚合注册 / POM 聚合 / 菜单注册，维度⑭全局面）+ ⑮ owner-doc 断言抽样 3 doc × 9 断言 | ① 聚合器（勘误 E1 路径 `app-erp-all/src/main/resources/_vfs/nop/main/auth/app.action-auth.xml`，x:extends ×2 处字段 = 单属性 23 项）= **19 ERP 域**（md/pur/sal/inv/fin/ast/prj/mfg/qa/mnt/crm/cs/hr/aps/ct/drp/log/b2b/notify 逐项映射零缺失零多余）+ **4 nop 系统模块**（auth/sys/wf/report）；19 个被引用保留层文件全数在盘（无 `_` 前缀）；`module-*` 共 21 目录（19 域 + common-service/common-test 无 action-auth，与 U20 口径一致）→ 注册对账一致。② checker 19 规则全表与机器块现值**逐值一致 exit 0**（successor `2100-1` 已落地：R2b 242/R2c 1542/R12a 71，计划登记既有漂移①闭合）；`__XGEN_FORCE_OVERRIDE__` 301 命中全为 dict.yaml 行 1 codegen 标记，tracked 脏面零 → 生成产物零手改；app-erp-all main Java 6 文件反模式零命中（`@Inject` 唯一站点 ErpModuleMetaBizModel.java:24 非 private，R5=0 互证）。③ POM：app-erp-all 依赖 = 19 `app-erp-*-web` + `app-erp-common-service` + `app-erp-common-test` = 21 项全覆盖（根 pom 22 `<module>`），web 传递引入 dao/service/meta 链。④ 菜单：19 域 action-auth 各贡献恰 1 TOPM（18 业务域 + notify inbox user TOPM 维度⑭特例在册 roles="user"）+ 聚合器 erp-sys/erp-l10n-cn 顶层项；18 域 auth 文件含报表/看板入口全量在册。**发现**：八扩展域保留层 action-auth **零 x:extends 继承生成层**（= 新立 P3-CK-app-001-r3，生成层 22~109 资源/域 FNPT 不入聚合链）；**归并 2**（common-002/003，HEAD 复核现症均在）。⑮ 3 doc × 9 断言 = 6 一致 + 3 漂移站点（聚新立 P3-CK-app-002-r3，e2e-runbook 焦点 ≥2 漂移触发扩样全章节走查，余无新增漂移） | **finding**（2 新立 P3：001/002-r3；归并 2：common-002/003；复用 0） |
| **DIM-F 菜单全局面与 flux 导出门禁** | §1.2 ③ + `npm run validate:flux` 全链 + 菜单 `component=FLUX` 全量清点 + U21 专属面（菜单→页面映射 + 门禁宿主一致性 + 姊妹切片门禁数字交叉） | `validate:flux` step [1/3] **`FLUX_PAGE_ERROR_COUNT: 0 pageCount=999 erpPages=855`**（与冻结门禁/known-good MI.8 行精确一致）；整体 exit 1 = 325 ERR **100% 单一 variant 族**（机读 `_tmp/flux-page-validation-report.json`：`Invalid value for property "variant" on renderer type "dropdown-button"`，非 variant ERR = 0 零新 finding），314 文件/19 域逐域计数与 M1.11/M1.14/M1.15 姊妹记录**精确交叉一致**（观察注记：mfg error 文件数本轮机读 33 vs 姊妹登记 32，error 总数 34 精确一致，誊录口径细节不立项）；菜单全量清点：19 域 auth + 聚合器合计 `component=` 752 处 = **712 FLUX**（叶菜单 100%）+ 40 `layouts/default/index`（TOPM 布局组件合法）；`component="AMIS"` 保留层 **0**；`render-mode: flux` 在 application.yaml:15；ORM `ext:web-renderer="flux"` 缺失 = 0（19 模型全带）。菜单→页面映射**全量**核（强于抽查）：464 去重 URL 中 **461** 解析到实存页面（本仓 855 page.yaml + nop-entropy 平台 20 个 `/nop/*` 页面逐一在盘），**dead = 3** 全部为 `/erp/l10n-cn/pages/*`（= r1 common-002，DIM-B 已归并，HEAD 复核仍在）；每域 ≥1 菜单项可达 + 18 域报表/看板入口在册（DIM-B ④ 互证）。门禁宿主一致性：flux doc 门禁数字与流程语义（error>0 → exit 1）与实跑双数字对账一致；doc §10.3 E2「全链 exit 0」期望差距 = ⑮ 已新立 app-002-r3 站点 1，本维不重复立项 | **finding（归并态 0 新立）** |
| **DIM-S `_init-data` 全量 seed 口径** | §1.3 全套 + `TestErpSeedDataIntegrity` 门禁宿主 + 资产清点/同步义务/fresh-DB 纪律 | `mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` → **4/0/0/0 BUILD SUCCESS**（exit 0，12.05s；全实体 findAll + to-one 零悬空 + 零孤儿 CSV + scope-pinning 精确锚定，与 M1.2~M1.15 姊妹记录一致）；seed 脏面 `git status --porcelain _init-data/` = **空**；资产清点 **372 CSV + 1 SQL**（`zz-sequence-advance.sql`）= seed-data.md L75/L84 冻结登记值精确一致，机读分解 368 app.erp.* + 4 平台 = 372 闭环；deploy 同步义务 `find module-* -path '*deploy/sql*' -name '_seed_*.sql'` 恰 = notify×3 + cs×3 两处已登记命中，无第三态；U21 聚合职责：19 域全部有域 CSV 在 `_init-data`（逐域 grep ≥1），静态实核 481 entity = 368 生成 + 113 notGenCode，368 = 363（className 裁决集）+ 5（finance 缺 className 已知集）与测试常量**精确闭环**；fresh-DB 纪律：known-good-baselines 2026-09-04 行二轮 fresh-DB 启动记录引用为证据（seed 自 `ba3fdde73` M1.5 批次后零变更 → 证据对当前 seed 集合仍有效，免于冗余重启） | **pass** |
| **DIM-T 集成测试快照纪律与回归对照** | §1.4 + C01-C21 族三层全比对 + scoped/全 reactor 双回归对照 + 覆盖缺口对账 | `mvn test -pl app-erp-all` → **70/0/0/1 BUILD SUCCESS**（与 2026-09-04 known-good 行精确一致，零增量零回归）；全 reactor `mvn test` → **41 个含测试模块汇总行求和 = 4006/0/0/1 BUILD SUCCESS**（与 MI 终态行精确一致零新增失败，日志 `_tmp/m116-full-reactor-mvn-test.log`）；C 族清点：`it` 包 `TestErp*` 27 类 ↔ `_cases/io/nop/app/all/it/` 27 目录 `comm -3` 零差异（P2pPilot 1 + C01~C21 22 类[C20 拆 a/b] + AI ×2 + FinAp ×2）；快照纪律：`SnapshotTest.RECORDING`/`RecordMode.RECORDING` 精确 grep = **0 残留**（仅 2 处 javadoc 术语引用），`delVersion` 屏蔽形态合规（C20a output json5 实证），367 个 `output/tables` CSV 裸 `*` 通配 = 0（动态值经请求侧 `@var:` 558 文件承载）；覆盖缺口对账：testing-strategy §关键业务流 16 行（P0×4 + P1×7 + P2×5）逐行交叉——**P0 4/4 + P1 7/7 覆盖 → 未覆盖 P1 流 = 0 零 finding**（fin 多币种+汇兑损益 = `TestErpFinExchangeRevaluation extends JunitAutoTestCase` + `TestErpFinFxRateGuard` + `PropertyErpFinMultiCurrencyBalance` service 集成层；其余 P1 = C13/C09/C07/C15/C16/C17 直覆盖）；P2 观察面：logistics 无 C 族用例但 service 层集成覆盖（FreightPosting/CarrierGatewayIntegration/ShipmentPostingEnd 在册）不立项；U21 聚合面（跨域集成编排）= C 族全跨域编排流承载充分（pur→inv→fin / sal→cs→fin / mfg→inv→fin / drp→pur / b2b→inv / prj→fin / hr→fin / ct→fin） | **pass** |
| **DIM-I E2E runbook 合规抽样 + CAT-4 全局面** | §1.5 全套 + 双 checker 门控 + U21 白名单条目 + runbook §编写规范逐条 | `--strict` → **PASS exit 0**（0 new violations vs 冻结快照 170 baseline files，170 处 improvement 全部 removed-from-tree；live CAT1-4 = **0/0/0/0**，3430 java + 886 yaml 与 Phase 1 report mode 一致）+ `--self-test` → **PASS exit 0**；CAT-4 全局面 = 0 sites / 0 files（MI.8 收官态维持）。runbook 合规抽样 **16 spec ≥10 达标、五族全覆盖**：reports（ast-disposal/inv-inventory-trace value `_helper` 业务 token 断言零选择器）/ dashboards（inv-snapshot/aps-schedule-gantt fixtures + GraphQLClient DIRECT `@BizQuery`，runbook L443 自登记合法范式）/ crud（ast-ext-fields-audit/inventory.write/ast-inventory.write `CrudListPage`+`getEngine()` PageObject 模式 ✓）/ business-actions（reverse-preview/drp-simulation/hr-leave-shift-linkage/fin-bad-debt DIRECT action 全栈可达 = 非页面路径合规）/ visual（dashboards.snapshot 像素 helper 层 + fin-period-close-wizard/ext-domains-list-filter/field-format 存量 spec 内联选择器 = runbook「已知差距」节在案迁移债，L84 强制条款 scope = 新写/重写/修复 → 对账登记不立项）；`E2E_ENGINE` 缺省 flux 本地分发副本 `tests/e2e/pages/engine.ts:11` `return 'flux'` 实证 ✓（观察注记：上游源 nop-chaos-next engine.ts:10 缺省仍 `'amis'`——跨仓保护面，webServer `-Dnop.web.render-mode=flux` + runbook 显式传递三重保障，不立项）；console 守卫链路 `fixtures.ts:21-34` 全局 fixture 层 ✓；白名单 §WHITELIST 总条目 = **27**（与 MI.9 终态精确一致），**U21/app-erp-all 面 = 0 条**，全局面抽 5 条（fin ×4 + ast ×1）四要素逐条齐备 ✓；`grep -L "@Locale"` 全量 `*Errors.java`（22 文件）→ **空输出**（22/22 全带）；meta/i18n 脏面 `git status --porcelain 'module-*/erp-*-meta/**' 'app-erp-all/src/main/resources/_vfs/i18n/**'` → **空** ✓ | **pass** |

## 2. Finding 三态裁决（§2 跨轮查重全维强制）

> 查重源：r1 `ck-common-app.md`（C8.2：common+app 10 finding，命中本格面 2 条）+ r2 只读目录 `2026-08-28-2049-ai-check-r2/`（7 文件 grep action-auth/聚合/app-erp-all 零命中，r2 无聚合面 finding）+ §Mission 基线快照（checker 基线行已裁决偏离不重复报告——本格双 checker 实跑零漂移无新增）+ 27 姊妹报告「归 U21」移交项（M1.15 common 格 DIM-F/S n-a 归 U21 注记 = 本格 F/S 判定面承接，非 finding 移交；common-014/015-r3 为 U20 聚合追踪点非 U21 面）。**本轮新立 2 条全 P3**；`CK-app` 短码历史零占用（001/002 编号可用），历史 ID 零覆写。

### 2.1 复用（同型已 fixed，HEAD 复核有效）— 0 条

### 2.2 归并（同型 open 追加证据至原 ID）— 2 条

| 原 ID | r3 复核证据（T0 `8e8ab7fca`） |
| --- | --- |
| P3-CK-common-002（r1 l10n-cn 死菜单） | DIM-F 菜单全量核 464 去重 URL 中 **3 dead 全部为 `/erp/l10n-cn/pages/*`**；`module-l10n-cn` 目录全仓不存在实证；聚合器 erp-l10n-cn 顶层菜单项与死页面同源——现症原样在位（本格 DIM-B 聚合注册面 + DIM-F 菜单可达面双加重证据） |
| P3-CK-common-003（r1 聚合头注释含 job 而 x:extends 清单无 nop-job） | DIM-B 聚合器 x:extends 23 项全量清点复核（19 ERP + auth/sys/wf/report 4 项，无 nop-job）——注释 vs 清单失配现症原样在位 |

### 2.3 新立 `-r3` — 2 条（全 P3）

**P3-CK-app-001-r3**（DIM-B 维度⑭聚合——八扩展域保留层 action-auth 零 x:extends 生成层继承）
- **控制点**：`module-{crm,cs,hr,aps,contract,drp,logistics,b2b}/erp-*-web/src/main/resources/_vfs/nop/main/auth/erp-<short>.action-auth.xml` 8 文件零 `x:extends` 继承生成层 `_erp-<short>.action-auth.xml`（对照 `docs/architecture/auth-and-permissions.md`「标准做法」+「权限声明层同步」条款；核心域 11 域保留层均继承）。生成层 22~109 资源/域（逐实体 FNPT:ErpXxx:query/mutation 权限点）不入聚合链（聚合器 x:extends 仅指向保留层文件，继承断链 = 全量生成 FNPT 脱链）。
- **问题**：业务菜单由手写层完整承载（菜单显示面无缺口），`nop.auth.enable-action-auth` 缺省 false + 全仓 seed 零 FNPT 角色授权行 → **缺省配置零运行时影响**；enforcement 配置开启后 8 扩展域全部动作权限点脱链 = 功能死锁面。
- **三态裁决**：新立（r1 common 面 C8.2 10 条无此形态；r2 零命中；27 姊妹移交项无此移交）。P3（校准对齐 fin4-023-r3/drp-018/b2b-011/prj-022-r3/qa-029-r3 FNPT 家族；替代方案「并入家族单 ID」否决——家族 = 保留层自定义 mutation 零注册，本 finding = 保留层未继承生成层致全量生成 FNPT 脱链，层级与修复路径均不同，仅家族注记互链）。
- **修复方向**：M2.x enforcement 批——8 域保留层补 `x:extends` 继承行（对齐核心域先例）；先写 enforcement-on 下权限点断言失败测试；与 FNPT 家族同批收口。

**P3-CK-app-002-r3**（DIM-B ⑮ owner doc 漂移簇——2 doc 3 站点）
- **站点 1**：`docs/architecture/flux-page-export-and-validation.md` §7/§10.3「exit 0（无 error 级诊断）/ validate:flux 全链 exit 0」vs 实况整体 exit 1（325 条既有 `variant=primary` codegen stub 外部漂移已裁决 successor 在案）——**门禁 owner doc 未登记既有漂移**。
- **站点 2**：`docs/testing/e2e-runbook.md` L213「生产 application.yaml 保持 init-database-data 缺省（false）」vs `app-erp-all/application.yaml:18` `init-database-data: true`（plan 1143-1 用户裁决翻转；同 doc L185/L189 已更新）——**行内自相矛盾 + 1143-1 L30 显式推翻旧裁决未传播至本行**。
- **站点 3**：`e2e-runbook.md` L142 明细合计 91 表 / L189「97 张 CSV」陈旧内联计数 vs 实仓 372（L1171 权威计数源注记已自纠；lesson-13 同型软漂移）。
- **三态裁决**：新立单 ID 漂移簇（对齐 hr2-027-r3/md-017-r3 漂移簇单 ID 先例；「拆两 ID」替代方案否决——同属「U21 owner doc 漂移」同型同批，残留的跨 doc 修复易漏站点风险由本行逐站点列明对冲）。P3。
- **修复方向**：M2.x doc 维护批——① §7/§10.3 登记 325 条既有漂移或改「step[1/3] exit 0 + 整体 exit 1 余项 successor」口径；② L213 行改 `true` 并补 1143-1 裁决指针；③ L142/L189 内联计数删除并指向 L1171 权威源。

### 2.4 归属标注（§3.2 + 跨域横切）

- 域内页面/seed 内容缺陷归各域格（15 切片已收官）：本格 DIM-F 的 325 ERR 逐域分布、DIM-S 的 19 域 CSV 仅作聚合对账面，逐条内容缺陷已在各域报告登记。
- common 抽象族行为归 U20/M1.15：其 DIM-F/S = n-a 的判定面（聚合菜单可达面 / 全量 seed 装载链）由本格承接为 pass（common-014/015-r3 聚合追踪点维持 U20 归属）。
- validate:flux 325 条 variant 外部漂移与 mfg 文件计数誊录观察：既有 successor 在案（nop-chaos-flux dist 基线裁决），本格仅对账不立项。
- nop-chaos-next engine.ts 上游缺省 `amis` vs 本地分发副本缺省 `flux`：跨仓保护面观察注记（e2e-shared 分发漂移由 runbook「已知差距」节承载），非本格 finding。

### 2.5 维度⑮断言抽样记录（3 doc × 9 断言：一致 6 / 漂移 3 站点聚 1 簇）

**seed-data.md 3 断言 0 漂移**：A1 L75/L84「M1.5 批次后 = 372 CSV + 1 SQL」vs 实仓一致；A2 L107-109 登记处「全仓无其他 `_seed_*.sql`」vs notify×3 + cs×3 两处已登记一致；A2b L107「27 模板行」vs erp_sys_notification_template.csv 27 数据行一致。**flux doc 3 断言 1 漂移**：A3 §10.3「855 page.yaml」一致；A3b「31 手写 flux.yaml」一致；A4 §7/§10.3「全链 exit 0」vs 实况 exit 1 = app-002-r3 站点 1。**e2e-runbook 3 断言 2 漂移（触发扩样）**：A5 L9「E2E_ENGINE 缺省即 flux」vs engine.ts:11 一致；A6 L213 vs application.yaml:18 = app-002-r3 站点 2；A6b L142/L189 内联计数 = app-002-r3 站点 3。扩样履行：e2e-runbook 焦点全章节走查（L9/L135/L180/L219 运行命令、L142/L185/L189/L209-213 种子段、L231 像素套件 19+30+69+57=175 内部自洽、L1171 权威计数源注记），余无新增漂移。

## 3. 统计

| 级别 | 本轮新立 | 复用 | 归并 |
| --- | --- | --- | --- |
| P0 | 0 | 0 | 0 |
| P1 | 0 | 0 | 0 |
| P2 | 0 | 0 | 0 |
| P3 | 2（app-001-r3 扩展域 action-auth 继承断链 + app-002-r3 owner doc 漂移簇 3 站点） | 0 | 2（common-002/003 追加证据） |
| **合计** | **2** | **0** | **2** |

五格 verdict：DIM-B **finding**（2 新立 P3 + 2 归并）/ DIM-F **finding（归并态 0 新立）** / DIM-S **pass** / DIM-T **pass** / DIM-I **pass**。历史 ID 零覆写（`CK-app` 短码首用，r1 common-002/003 open 追加证据）。

## 4. 剩余风险声明（横切关注点 13 完整四件套）

- **已查**：聚合器 action-auth x:extends 全量清点（23 项逐项映射）+ 19 保留层文件在盘核证 + POM 21 依赖 vs 22 reactor 顶层模块对账 + 菜单 19 TOPM/752 component 全量清点 + 464 菜单 URL 全量可达核（强于计划的每域 ≥1 抽查）+ validate:flux 双数字 + 325 ERR 机读逐域分组对账 + TestErpSeedDataIntegrity 全绿 + seed 372+1 清点/同步义务/fresh-DB 证据 + scoped 70/0/0/1 + 全 reactor 4006/0/0/1 + C 族 27↔27 成对 + 快照纪律 grep + 覆盖缺口 16 行逐行交叉 + 双 checker 红线（19 规则机器块逐值 + CJK 双模式）+ --strict/--self-test + 16 spec 抽样 + 白名单 27 条目/抽 5 四要素 + @Locale 22/22 + meta/i18n 脏面 + ⑮ 3 doc × 9 断言 + e2e-runbook 扩样全章节。
- **未深查（边界归属）**：域内页面/seed 内容缺陷（归 15 域格已收官）；common 抽象族行为（归 U20/M1.15 已收口）；nop-chaos-next/nop-chaos-flux 上游源内部（外部仓库保护面——engine 缺省漂移与 325 variant dist 收紧均仅观察对账）；E2E 浏览器实际运行回归（runbook 抽样为静态合规审计，浏览器视觉回归归看板专项/M2.3 计划链）。
- **残留风险（登记不裁决）**：① 2 条新立 P3 + 2 条归并 open 修复归 M2.x（enforcement 批 + doc 维护批 + 前端迁移债批）；② 325 条 variant=primary stub 外部漂移 successor 在案（validate:flux 整体 exit 1 维持至 dist 基线裁决落地）；③ app-001-r3 为 enforcement 开启的**前置阻塞项**——`nop.auth.enable-action-auth` 翻 true 前 8 扩展域动作权限点必须修复。
- **successor 触发条件**：M1.17 收官完整性校验本报告 5/5 格与双索引；`validate:flux` exit 0 恢复（nop-chaos-flux dist 基线裁决）；enforcement 开启（enable-action-auth 翻 true）触发 app-001-r3 修复前置；owner doc 维护批触发 app-002-r3 三站点修复。
