---
status: active
mission: ai-check-r3
work-item: M1.16
group: "2026-09-09-2100"
verify: [test]
---

# 2026-09-09-2100-2 M1.16 app-erp-all 横切五维符合性审计（U21 全格：聚合/菜单/seed 全量口径/集成快照/runbook 抽样）

## Current Baseline

- 依赖已满足：M0.6 done（plan `2026-09-06-1451-3`）；MI.9 收官 done（plan `2026-09-07-1715-3`，独立 closure audit ACCEPT，2026-09-08；known-good-baselines 2026-09-08 `ai-check-r3 MI 终态行`：全 reactor `mvn test` 4006/0/0/1、CJK report mode CAT1..4 = 0/0/0/0、`--strict`/`--self-test` PASS、白名单 27 文件四要素齐备）；M1.15 已执行并独立闭包审计 ACCEPT（plan `2026-09-09-0547-3`，2026-09-09，roadmap 行「执行完成待收官翻转」）——M1.1~M1.15 共 27 份切片报告在盘（2026-09-09 起草时实仓复核：多单元切片 M1.12×2/M1.13×3/M1.14×5/M1.15×6），M1.16 为 roadmap 文档序最后一个 M1 工作项（起草时唯一未起草项，本计划即其执行草案）；M1.17 依赖本项完成，不在本计划范围。
- 强制核对矩阵已冻结：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md`——本计划唯一格集合 = **U21 × 五维（全格）**（§4 映射表第 16 行，1 单元 × 5 维 = 5 格）；§3.3 U21 行焦点与程式：B=聚合完整性（维度⑭全局面：action-auth 聚合器 x:extends 全模块注册/POM 聚合/菜单注册，§1.1 ①④ + 聚合器全量 grep）/ F=菜单 component=FLUX 全局面 + `render-mode: flux` + flux 页面导出门禁（999 页/ERP 855）/ S=`_init-data` 全量 seed 口径（372 CSV + 1 SQL 现值）+ fresh-DB 启动纪律 + `TestErpSeedDataIntegrity` 门禁宿主（§1.3 全套）/ T=集成测试快照纪律（C01-C21 族三层全比对）+ 全 reactor 回归对照基线 / I=E2E runbook 合规抽样（flux 引擎强制/PageObject/REST 断言）+ 页面 yaml CAT-4 全局面（§1.2 ③ + §1.5 全套 + e2e-runbook §编写规范逐条）；聚合横切面唯一归属 = U21（§3.2：域内页面/seed 内容缺陷归各域格，聚合机制缺陷归本格）。
- Owner docs（roadmap M1.16 行）：`docs/architecture/seed-data.md` + `docs/architecture/flux-page-export-and-validation.md` + `docs/testing/e2e-runbook.md`；DIM-B ⑤ 断言抽样锚 = 三份 owner doc 关键断言。
- 跨轮查重源（§2，U21 格）：r1 `docs/audits/check/ck-common-app.md`（C8.2：聚合面 finding 历史账）+ r2 只读目录 `docs/audits/check/2026-08-28-2049-ai-check-r2/` + 跨轮索引 `docs/audits/check/ai-check-index.md` §Mission 基线快照（已裁决偏离不重复报告）；27 份 r3 姊妹切片报告的「归 U21」移交项（如 M1.15 common 格 DIM-F/S n-a 归 U21 注记）为本格消费面。
- 已知既有漂移（对账不立项，登记在案）：①compliance checker 机器块差值 R2b +2/R2c +5/R12a +1——successor = 同批 N=1 计划 `2026-09-09-2100-1-compliance-baseline-raise-adjudication.md`（执行顺序在本计划之前；执行期若已落地则机器块已对齐，红线以实跑 + 机器块现值为准，漂移即登记不就地裁决）；②`npm run validate:flux` 整体 exit 1 余项 = 325 条 codegen stub `variant=primary` 既有外部漂移（successor 在案，非本切片 finding，按命中面对账）。
- 绿色基线：known-good-baselines 2026-09-08 MI 终态行（全 reactor 4006/0/0/1，MV.1 对照面）；app-erp-all scoped test 最近登记 70/0/0/1（2026-09-04 行，执行期以 known-good-baselines 最新行为权威）；`TestErpSeedDataIntegrity` 4/0/0/0；seed 资产清点 372 CSV + 1 SQL（seed-data.md 冻结登记值）；validate:flux step [1/3] `FLUX_PAGE_ERROR_COUNT: 0 pageCount=999 erpPages=855`。
- 仓库现状（2026-09-09 起草时实核）：HEAD `060ddab0a`，工作树 tracked 零修改（本批两份计划文件 `2026-09-09-2100-1/2` untracked 在案，实跑时点按 Phase 1 披露）；审计证据以实跑时点 HEAD + 脏面披露为准（MI.9 收官脏树实跑先例）。
- 剩余差距：U21 × 五维 = 5 格 verdict 未落盘；执行目录尚无 `ck-app-erp-all-r3.md`；r3 索引产物清单 27 行（尚无本格行）+ 跨轮索引 §报告清单/§Finding 追踪无 M1.16 行（仅 5 处「归 U21/M1.16」归属标注）——本格收口后 105 基础格仅余收官机制核账（M1.17）。

## Goals

- 按冻结清单对 U21 × 五维全格全跑（禁止抽样、禁止跳维；五格 verdict 全落 `pass`/`finding`/`n-a` 带理由），产出报告 `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-app-erp-all-r3.md`（五维覆盖矩阵 + finding 列表 + 统计 + 剩余风险声明，roadmap 横切关注点 13）。
- 每候选 finding 完成三态查重裁决（复用/归并/新立 `-r3` ID，历史 ID 永不覆写）；DIM-B 维含 owner-doc 关键断言抽样 ≥1 doc × ≥2 断言（3 份 owner doc 全数覆盖 × ≥2 断言；任一焦点 ≥2 处漂移扩大至该焦点全部 owner doc 章节）。
- 双索引同步：本轮 `ai-check-r3-index.md` 产物清单（+1 行）+ 跨轮 `docs/audits/check/ai-check-index.md`（§报告清单 M1.16 行 + §Finding 追踪新 ID 行）。
- 全程零生产代码改动（roadmap 规则 6）：只产 finding 与索引，收官 `git status` 机械核证。

## Non-Goals

- 不修复任何 finding（M1 只读审计；修复归 M2.x，强制先写失败测试）；不改任何生产代码/ORM/api.xml/配置/页面/seed 文件（含 app-erp-all 自身）。
- 不覆盖 U21 之外任何单元格（域内页面/seed 内容缺陷归各域格——15 切片已收官；common 抽象族归 U20/M1.15 已收口）；不重复已收官切片的格。
- 不接管 r1/r2 工作项（横切关注点 5）；不重开既有裁决（lesson 09/10、CJK 白名单、compliance 基线差值 successor、validate:flux variant 漂移 successor——两处既有漂移仅对账登记）；不做 roadmap 状态翻转（done 转换由结束审计机制处置）。
- DIM-I 不产生同类 CJK finding（§1.5 MI 先行时序口径：仅验证零回归与白名单合规）。

## Phase 1 — 执行目录就位 + 红线与脏面披露

> 类型：Proof（3 项 Proof）。
> Skill: none
> Targets: `docs/audits/check/2026-09-06-1645-ai-check-r3/`（幂等复用，不新建目录）；盘点注记落本计划勾选注记
> Prereqs: 无

- [x] <Proof> `mkdir -p docs/audits/check/2026-09-06-1645-ai-check-r3/`（本轮唯一规范执行目录，幂等复用；roadmap 规则 8），确认 `ai-check-r3-index.md`、`m0-5-audit-checklists.md` 与 27 份姊妹 `ck-*-r3.md` 在位，索引头部登记路径一致——**实核 2026-09-09**：目录在位（幂等复用零新建），`ai-check-r3-index.md` + `m0-5-audit-checklists.md` + 27 份姊妹 `ck-*-r3.md`（M1.1/M1.2~M1.4/M1.5~M1.7/M1.8/M1.9/M1.10/M1.11/M1.12×2/M1.13×3/M1.14×5/M1.15×6）全部在盘；索引头部登记路径 `docs/audits/check/2026-09-06-1645-ai-check-r3/` 与实跑目录一致
      - Skill: none
- [x] <Proof> 记录审计时点仓库状态：HEAD hash、`git status --porcelain` 脏面清单、同批姊妹计划（`2026-09-09-2100-1` compliance 基线裁决）执行状态披露；后续全部证据注记引用该时点——**实核 2026-09-09（审计时点 T0）**：HEAD `8e8ab7fca886d20c82556884b13d3d7f396293cd`（计划起草基线 `060ddab0a` 后两笔推进均为姊妹审计产物提交：`060ddab0a` = 0547-3 M1.15 六单元落盘 + 闭包审计 ACCEPT、`8e8ab7fca` = 2100-1 compliance 基线裁决落盘 + 闭包审计 ACCEPT，生产代码零变化）；脏面 = 1 条 untracked 计划文件（本计划自身 `2026-09-09-2100-2-*.md`），tracked 零修改；姊妹 `2026-09-09-2100-1` 已执行完毕并提交（HEAD 提交信息含「闭包审计回执 ACCEPT」，机器块已更新为 raise 后值，见下条），非本计划查重/证据障碍
      - Skill: none
- [x] <Proof> 红线基线实跑：`bash docs/audits/nop-compliance-checker.sh`（对照机器块现值——差值规则 R2b/R2c/R12a 以 successor 计划落地态为准，漂移即登记不就地裁决）+ `node tools/check-hardcoded-cjk.mjs`（report mode，对照 0/0/0/0 终态）记录数字——**实核 2026-09-09**：checker 19 规则全表 = R1a-c 0/0/0 + R1d 14 + R2a 34 + R2b 242 + R2c 1542 + R2d 38 + R3 5 + R4 0 + R5 0 + R6 2 + R7 0 + R8 0 + R10 14 + R11 0 + R12a 71 + R12b 66 + R12c 42，**与机器块现值逐值一致零漂移（exit 0）**——successor `2100-1` 已落地（机器块 R2b 240→242 / R2c 1537→1542 / R12a 70→71），计划登记的既有漂移①已闭合，无新增漂移；CJK report mode **CAT-1..4 = 0/0/0/0**（3430 java + 886 yaml 扫描）= MI 终态行一致零漂移（exit 0）
      - Skill: none

Exit Criteria:

- [x] 执行目录就位且本轮索引头部登记路径一致；时点/脏面披露在案
- [x] 双 checker 红线数字在案且与机器块现值 / MI 终态行对账一致（或既有漂移已登记）

## Phase 2 — DIM-B 聚合完整性走查（维度⑭全局面）

> 类型：Proof-heavy（2 Proof + 1 Decision）。
> Skill: nop-platform-conformance-audit-prompt + code-quality-audit-prompt（roadmap M1.16 行指定，承「同上」链 M1.1 行）
> Targets: `app-erp-all/`（聚合器 `_vfs/nop/main/auth/app.action-auth.xml`[§6 勘误 E1 路径]/POM/菜单注册面）+ `module-*/erp-*-web` 注册消费点（只读 grep）
> Prereqs: Phase 1 完成

- [x] <Proof> 聚合器全量核对：`app.action-auth.xml` x:extends 全模块注册清点（19 域 + common 面 = 聚合注册数 vs `module-*` 实际模块数逐项对账，缺失/多余即 finding）+ §1.1 ①④ 机械程式（checker 逐规则对照 + `__XGEN_FORCE_OVERRIDE__`/`_gen` 脏面）+ POM 聚合核对（`app-erp-all/pom.xml` 依赖 vs 156 reactor 模块聚合完整性）+ 菜单注册核对（菜单模型含 18+1 域菜单项/报表/看板入口全量 grep 清点）
      - Skill: nop-platform-conformance-audit-prompt
      - **实核 2026-09-09（时点 T0）**：① 聚合器（勘误 E1 路径 `app-erp-all/src/main/resources/_vfs/nop/main/auth/app.action-auth.xml`，x:extends ×2 处字段=单属性 23 项）= **19 ERP 域**（md/pur/sal/inv/fin/ast/prj/mfg/qa/mnt/crm/cs/hr/aps/ct/drp/log/b2b/notify，逐项映射 18 业务域+notify 零缺失零多余）+ **4 nop 系统模块**（auth/sys/wf/report）；19 个被引用保留层文件全数在盘（`module-*/erp-*-web/.../auth/erp-<short>.action-auth.xml` 无 `_` 前缀）；module-* 共 21 目录（19 域 + common-service/common-test 无页面无 action-auth，与 U20 格 F/S=n-a 口径一致）→ 注册对账一致。② checker 逐规则对照 = Phase 1 全表（19 规则=机器块逐值一致 exit 0）；`__XGEN_FORCE_OVERRIDE__` 命中 301 文件全部为 dict.yaml 行 1 codegen 标记（设计态只读校验点），tracked 脏面零（Phase 1 披露）→ 生成产物零手改；app-erp-all main Java 仅 6 文件（ErpApplication + meta 诊断族），无 `extends RuntimeException`/`System.currentTimeMillis` 命中、`@Inject` 唯一站点 ErpModuleMetaBizModel.java:24 非 private（checker R5=0 互证）。③ POM：app-erp-all 依赖 = 19 个 `app-erp-*-web` + `app-erp-common-service` + `app-erp-common-test` = 21 项全覆盖（根 pom 22 `<module>` = 21 module-* + app-erp-all），web 传递引入 dao/service/meta 链。④ 菜单注册：19 域 action-auth 各贡献恰 1 个 TOPM（18 业务域 + notify inbox user TOPM 维度⑭特例在册 roles="user"）+ 聚合器 erp-sys/erp-l10n-cn 顶层项；18 域 auth 文件含报表/看板入口 → 18+1 域菜单项/报表/看板入口全量在册。**发现（新立 P3-CK-app-001-r3）**：8 个扩展域（crm/cs/hr/aps/ct/drp/log/b2b）保留层 action-auth **零 `x:extends` 继承生成层** `_erp-<short>.action-auth.xml`（对照 auth-and-permissions.md「标准做法」+「权限声明层同步」条款）——生成层 22~109 资源/域（逐实体 FNPT:ErpXxx:query/mutation 权限点）不入聚合链；业务菜单由手写层完整承载（菜单显示面无缺口），`nop.auth.enable-action-auth` 缺省 false + 全仓 seed 零 FNPT 角色授权行 → 缺省配置零运行时影响，enforcement 配置下方为功能死锁面（P3 校准对齐 fin4-023-r3/drp-018/b2b-011/prj-022-r3/qa-029-r3 家族）。**归并 2 项（r1 HEAD 复核仍在）**：P3-CK-common-002 l10n-cn 死菜单（3 个 FLUX 页面 URL 指向不存在的 module-l10n-cn，全仓无该目录实证）+ P3-CK-common-003 聚合头注释含 job 而 x:extends 清单无 nop-job
- [x] <Proof> 维度⑮ owner-doc 关键断言抽样（3 份 owner doc × ≥2 断言：seed-data.md 全量口径/同步义务断言、flux-page-export-and-validation.md 门禁数字与流程断言、e2e-runbook.md 强制条款断言——状态名/数字/路径类断言逐条实仓复核）；任一焦点 ≥2 处漂移扩大至该焦点全部 owner doc 章节
      - Skill: code-quality-audit-prompt
      - **实核 2026-09-09**：3 doc × 9 断言（≥3×6 达标）——**seed-data.md 3 断言 0 漂移**：A1 L75/L84「M1.5 批次后 = 372 CSV + 1 SQL」vs 实仓 372 CSV + 1 SQL（zz-sequence-advance.sql）一致；A2 L107-109 登记处「全仓无其他 _seed_*.sql」vs `find` 实证恰 notify×3 方言 + cs×3 方言两处已登记一致；A2b L107「27 模板行」vs erp_sys_notification_template.csv 27 数据行（+1 表头）一致。**flux doc 3 断言 1 漂移**：A3 §10.3「find module-* -name '*.page.yaml' = 855」vs 实仓 855 一致；A3b「31 个手写 flux.yaml」vs 实仓 31 一致；A4 §7/§10.3「exit 0（无 error 级诊断）/validate:flux 全链 exit 0」vs 实况整体 exit 1（325 条既有 variant=primary stub 外部漂移已裁决 successor 在案）——**漂移 = 门禁 owner doc 未登记既有漂移**（新立 P3-CK-app-002-r3 站点 1）。**e2e-runbook 3 断言 2 漂移（触发扩样）**：A5 L9「E2E_ENGINE 缺省即 flux（engine.ts 缺省值）」vs engine.ts:11 `return 'flux'` 一致；A6 L213「生产 application.yaml 保持 init-database-data 缺省（false）」vs 实仓 application.yaml:18 `init-database-data: true`（1143-1 用户裁决翻转，同 doc L185/L189 已更新）——**行内自相矛盾 + 1143-1 L30 显式推翻旧裁决未传播至本行**（P3-CK-app-002-r3 站点 2）；A6b L142 明细合计 91 表 / L189「97 张 CSV」陈旧内联计数 vs 实仓 372（L1171 权威计数源注记已自纠，lesson-13 同型软漂移，P3-CK-app-002-r3 站点 3）。**扩样履行**（e2e-runbook 焦点 ≥2 漂移 → 该焦点全章节走查）：L9 运行模式/L135·L180·L219 运行命令/L142·L185·L189·L209-213 种子段/L231 像素套件（19+30+69+57=175 内部自洽，132/134 为 09-04 行已验证态、+6 报表增长有 M2.3 计划链）/L1171 空库冒烟勘误注记——逐节复核，余无新增漂移
- [x] <Decision> 本维候选 finding 逐条过 §2 三态裁决（查 r1 `ck-common-app.md` + r2 目录 + §Mission 基线快照 + 27 姊妹报告「归 U21」移交项）：同型 fixed=复用（复核 HEAD 仍有效）/同型 open=归并原 ID/确属新发=新立 `P{n}-CK-app-{NNN}-r3`；裁决证据落勾选注记（报告落 Phase 7）
      - Skill: code-quality-audit-prompt
      - **裁决 2026-09-09**：查重源四路走查——r1 `ai-check-index.md` §报告清单 C8.2 行（common+app 10 finding：common-002 l10n 死菜单 open / common-003 job 注释失同步 open 两条命中本格面）+ r2 目录 `2026-08-28-2049-ai-check-r2/`（7 文件 grep action-auth/聚合/app-erp-all 零命中，r2 无聚合面 finding）+ §Mission 基线快照（checker 基线行已裁决偏离不重复报告——本格双 checker 实跑零漂移无新增）+ 27 姊妹报告「归 U21」移交项（M1.15 common 格 DIM-F/S n-a 归 U21 注记 = 本格 F/S 判定面承接，非 finding 移交；其余姊妹报告移交槽位 common-014/015-r3 为 U20 聚合追踪点非 U21 面）。三态结果：**归并 2**（common-002/common-003，HEAD `8e8ab7fca` 复核现症均在：l10n-cn 目录全仓不存在 + 聚合头注释 job 字样 vs x:extends 无 nop-job）+ **新立 2 全 P3**（P3-CK-app-001-r3 八扩展域保留层零 x:extends 生成层继承、P3-CK-app-002-r3 owner doc 漂移簇 3 站点 2 doc）+ **复用 0**（无同型已 fixed 命中）；ID 冲突检查：`CK-app` 短码历史零占用（r1 用 common 短码），001/002 编号可用，历史 ID 零覆写。替代方案考虑：将 app-001-r3 并入 fin4-023-r3 FNPT 家族单 ID——否决，因形态不同（家族=保留层自定义 mutation 零注册，本 finding=保留层未继承生成层致全量生成 FNPT 脱链，层级与修复路径均不同，仅家族注记互链）；将 app-002-r3 拆两 ID（flux doc / runbook 各一）——否决，同属「U21 owner doc 漂移」同型同批（对齐 hr2-027-r3/md-017-r3 漂移簇单 ID 先例），残留风险=修复时两 doc 分头改易漏站点的行动风险由 finding 行逐站点列明对冲

Exit Criteria:

- [x] U21 DIM-B 格 verdict 落盘；聚合器注册/POM/菜单三面清点数字在案——**verdict = finding**（2 新立 P3 + 2 归并 r1）；三面数字：x:extends 23 项（19 ERP+4 nop）/ POM 21 依赖覆盖 22 reactor 顶层模块 / 菜单 19 TOPM + 18 域报表看板入口
- [x] 断言抽样 ≥3 doc × ≥6 断言逐条在案；候选 finding 三态裁决完成——3 doc × 9 断言（6 一致 3 漂移站点聚 2 finding）；裁决 = 归并 2 + 新立 2 + 复用 0

## Phase 3 — DIM-F 菜单全局面与 flux 导出门禁

> 类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = 冻结清单 §1.2 + `docs/architecture/flux-page-export-and-validation.md`）
> Targets: 菜单模型全局面 + `app-erp-all` flux 导出门禁消费面（`npm run validate:flux`）
> Prereqs: Phase 1 完成

- [x] <Proof> 全局面静态门禁实跑：`npm run validate:flux`（step [1/3] 导出 0 error / 999 页 / erp 855 保持；整体 exit 1 余项 = 325 条既有 `variant=primary` stub 外部漂移——按文件分布全量对账分组[stub 面归属/非 stub 面 0 条断言]，既有漂移不立项、非既有同族新面即 finding）+ 菜单 `component=FLUX` 全量清点 + `render-mode: flux` 全局面核对（`component="AMIS"` 保留层 expect 0）
      - Skill: none
      - **实核 2026-09-09**：`npm run validate:flux` 整体 exit 1——step [1/3] **`FLUX_PAGE_ERROR_COUNT: 0 pageCount=999 erpPages=855`**（三数字与冻结门禁/known-good MI.8 行精确一致）；step [3/4] `files=855 validated=855 errors=325 warnings=18491 strippedXui=0`。**全量分组对账**（`_tmp/flux-page-validation-report.json` 机读解析）：325 ERR 100% 单一 message 族 = `Invalid value for property "variant" on renderer type "dropdown-button"`，**非 variant ERR = 0**（非既有同族新面 = 0，零新 finding）；325 ERR 分布 314 文件/19 域（error 计数 aps4/ast15/b2b10/crm29/cs21/ct11/drp7/fin29/hr31/inv13/log8/md22/mfg34/mnt26/notify2/prj20/pur12/qa19/sal12）——与姊妹切片记录逐域交叉精确一致（M1.14 五域 29/21/11/10/7 = crm/cs/ct/b2b/drp；M1.15 六单元 26/4/8/2/22/0 = mnt/aps/log/notify/md/common；M1.11 hr 31；M1.5~M1.7 mfg 34 条；M1.9 全仓 325 总数），**观察注记**：mfg error 文件数本轮机读 33 vs 姊妹登记「32 文件」（error 总数 34 精确一致，为姊妹登记口径/誊录细节，无 verdict 影响，不立项）。菜单全量清点：19 域 auth + 聚合器合计 menu `component=` 752 处 = **712 `component="FLUX"`**（叶菜单 100% FLUX）+ 40 `layouts/default/index`（TOPM 布局组件，合法）；`component="AMIS"` 保留层 **0**；`render-mode: flux` 在 application.yaml:15 在册；ORM `ext:web-renderer="flux"` 缺失 = 0（19 模型全带）
- [x] <Proof> U21 格专属面核对：菜单→页面映射抽查（每域 ≥1 菜单项可达对应 view 页 + 报表/看板入口在册）+ 导出门禁宿主一致性（flux-page-export-and-validation.md 门禁数字与 validate:flux 实跑双数字对账）+ 27 份姊妹切片 DIM-F 行的「全局门禁」数字与本格实跑数字交叉一致断言
      - Skill: none
      - **实核 2026-09-09**：菜单→页面映射**全量**核（强于抽查）：19 域 auth + 聚合器去重 464 个菜单 URL，461 个解析到实存页面文件（本仓 `_vfs` 855 page.yaml 索引 + nop-entropy 平台模块 src/main 20 个 `/nop/*` 页面逐一确认在盘——NopAuthUser/NopAuthRole/…/NopReportDatasource 全 OK），**dead = 3** 全部为 `/erp/l10n-cn/pages/*`（= r1 P3-CK-common-002 死菜单，本格 DIM-B 已归并，HEAD 复核仍在）；每域 ≥1 菜单项可达 + 18 域报表/看板入口在册（DIM-B ④ 清点互证）。门禁宿主一致性：flux doc 门禁数字（999/855 导出 + `error 数 > 0 → exit 1` 流程语义）与实跑双数字对账——导出面一致、流程语义一致（exit 1 行为本身即 doc §7 流程的正确执行）；doc §10.3 E2「全链 exit 0」期望 vs 实况 exit 1 的差距 = DIM-B ⑮ 已新立 P3-CK-app-002-r3（owner doc 未登记既有漂移），本维不重复立项。姊妹交叉断言：见上条逐域对账（精确一致 + 1 处文件计数誊录观察）

Exit Criteria:

- [x] U21 DIM-F 格 verdict 落盘；导出门禁双数字（999/855/325 分组对账）在案——**verdict = finding（归并态 0 新立）**：导出 0 error/999/855 + 325 ERR 100% variant 族 + 非 variant 0
- [x] 菜单全量清点 + 映射抽查结果在案；与姊妹切片门禁数字交叉一致——712 FLUX/0 AMIS/render-mode flux 在册；464 URL 全量核 461 达 3 dead（全归并 common-002）；逐域 ERR 计数与 M1.11/M1.14/M1.15 姊妹记录精确一致

## Phase 4 — DIM-S `_init-data` 全量 seed 口径

> 类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/seed-data.md`）
> Targets: `app-erp-all/src/main/resources/_vfs/_init-data`（只读清点）+ `TestErpSeedDataIntegrity` 门禁宿主
> Prereqs: Phase 1 完成

- [x] <Proof> 引用完整性门禁实跑：`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 全绿（363 实体 findAll + to-one 零悬空 + 零孤儿 CSV + scope-pinning 计数）
      - Skill: none
      - **实核 2026-09-09**：`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` → **Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS**（exit 0，12.05s）——四断言全绿：testAllSeedTablesLoadable（全实体 findAll 含平台 66 表 + 有 CSV 表行数 > 0）/ testAdjudicatedScopePinned（363 + 5 已知 finance 缺 className 集精确锚定）/ testSeedAssetInventoryBaselines（零孤儿 CSV + 368 app.erp.* + 4 平台 = 372 基线常量命中）/ testNonNullRelationKeysPointToExistingRows（to-one 非空键零悬空、白名单空集）；与姊妹切片记录（M1.2~M1.15 全部 4/0/0/0）一致
- [x] <Proof> 全量口径核对：`git status --porcelain app-erp-all/src/main/resources/_vfs/_init-data/` expect 空（只读审计零 seed 变更）+ 资产清点对账（372 CSV + 1 SQL = seed-data.md 最新登记值）+ deploy `_seed_*.sql` 同步义务登记处逐命中核对（notify/cs 三方言先例口径）+ U21 格聚合职责核对（各域 CSV → 全量装载链无域遗漏——363 实体覆盖 vs 18+1 域实体总数对账）+ fresh-DB 启动纪律证据核对（known-good-baselines 最近行二轮启动记录引用或本轮 `./scripts/start-app.sh restart` 复证）
      - Skill: none
      - **实核 2026-09-09**：① seed 脏面 = **空**（零 seed 变更，只读审计不变式保持）。② 资产清点：**372 CSV + 1 SQL**（`zz-sequence-advance.sql`）= seed-data.md L75/L84「M1.5 批次后 = 372 CSV + 1 SQL」精确一致；机读分解 368 app.erp.* + 4 平台（nop_auth_user/nop_auth_user_role/nop_auth_role/nop_sys_code_rule）= 372 ✓。③ deploy 同步义务：`find module-* -path '*deploy/sql*' -name '_seed_*.sql'` 恰 = notify×3 方言 + cs×3 方言两处已登记命中（seed-data.md §登记处表逐行对账一致），**无第三态**（其余模块零 deploy seed，与登记处「全仓无其他」grep 断言一致）。④ U21 聚合职责：19 域全部有域 CSV 在 `_init-data`（逐域 grep ≥1）；实体总数对账——静态实核（XML 注释剥离后）481 entity 元素 = **368 本域生成实体 + 113 notGenCode 外部引用**；368 = 363（className 口径 M0.1 裁决集）+ 5（finance 缺 className 已知集：票据应收/应付/贴现/授信/现金预测，与测试常量 EXPECTED_MODEL_CLASSNAME_OMITTED_ENTITIES 逐一对应）→ 静态计数与运行时门禁常量**精确闭环**；CSV 全覆盖口径「有 seed 368 = 运行时 363 + finance 5 补充档全覆盖」（测试常量 javadoc M1.5 批次沿革链 93→…→368 完整）。⑤ fresh-DB 纪律：known-good-baselines **2026-09-04 行**二轮 fresh-DB 启动记录（`./scripts/start-app.sh restart` 13s + 12s ready 稳定 + 30 表 GraphQL 抽样行数一致）引用为证据——seed 资产自 09-04 起**零变更**（git log 末次 seed 触碰 = `ba3fdde73` M1.5 批次，先于 09-04 行）→ 证据对当前 seed 集合仍有效，免于本轮冗余重启

Exit Criteria:

- [x] U21 DIM-S 格 verdict 落盘；`TestErpSeedDataIntegrity` 全绿在案——**verdict = pass**（4/0/0/0 BUILD SUCCESS）
- [x] seed 零变更 + 372 CSV + 1 SQL 清点对账 + 同步义务 + fresh-DB 证据在案——四项全数一致（368+4=372 机读分解闭环；notify/cs 双登记无第三态；363+5=368 静态/运行时闭环）

## Phase 5 — DIM-T 集成测试快照纪律与回归对照

> 类型：Proof（3 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/testing-strategy.md`）
> Targets: `app-erp-all/src/test`（C01-C21 集成族 + 门禁宿主）+ 全 reactor 回归对照
> Prereqs: Phase 1 完成

- [x] <Proof> 集成族清点与三层全比对纪律：`mvn test -pl app-erp-all` 全绿（对照最近 known-good-baselines 登记 70/0/0/1，增量披露）+ C01-C21 集成用例族清点（用例类 ↔ `_cases` 快照目录成对齐备）+ 快照纪律 grep（`SnapshotTest.RECORDING` expect 0 残留 / `delVersion`、decimal `*` 通配屏蔽合规抽查）
      - Skill: none
      - **实核 2026-09-10 01:49（+08:00，T0 会话延续）**：① `mvn test -pl app-erp-all` → **Tests run: 70, Failures: 0, Errors: 0, Skipped: 1, BUILD SUCCESS**（1:44 min）——与 2026-09-04 known-good 行 70/0/0/1 **精确一致**，零增量零回归（唯一 skipped = `ErpAllWebPagesCollectTest` 收集器，先例口径）。② C 族清点：`it` 包 `TestErp*` 测试类 27 ↔ `app-erp-all/_cases/io/nop/app/all/it/` 快照目录 27，`comm -3` **零差异**（TestErpP2pPilot 1 + C01~C21 计 22 类[C20 拆 a/b] + AI 内省 ×2 + FinAp ×2 = 27 成对齐备；`TestErpSeedDataIntegrity`/auth/web/meta 族为门禁类无快照目录属设计态）。③ 快照纪律：`SnapshotTest.RECORDING`/`RecordMode.RECORDING` 精确 grep = **0 残留**（仅 2 处 javadoc 术语引用 B10FrozenClockExtension L12/ErpIntegrationTestCase L40，非代码）；`delVersion` 屏蔽实证 C20a output json5（`"delVersion": 0`）合规形态；decimal `*` 通配 = 367 个 `output/tables` CSV 裸 `*` 命中 **0**（动态值屏蔽经请求侧 `@var:` 558 文件承载）——三层比对纪律全合规
- [x] <Proof> 全 reactor 回归对照基线：`mvn test` 全 reactor 对照 MI 终态行 4006/0/0/1 零新增失败（本格 T 行程式 =「全 reactor 回归对照基线」；数字落注记，与姊妹切片单元回归数字互证）
      - Skill: none
      - **实核 2026-09-10（T0 会话延续）**：`mvn test` 全 reactor → **41 个含测试模块汇总行求和 = 4006 tests / 0 failures / 0 errors / 1 skipped, BUILD SUCCESS**（exit 0；原始日志 `_tmp/m116-full-reactor-mvn-test.log`）——与 MI 终态行（known-good-baselines 2026-09-06 `ai-check-r3-m0` 行与 M0.6 收官行同口径 4006/0/0/1）**精确一致零新增失败**；各姊妹切片单元回归数字（M1.11 hr 31 err 域等）在本 reactor 全绿内互证成立
- [x] <Proof> 集成覆盖缺口对账：C 族清单 × 关键业务流清单（testing-strategy §关键业务流 P1 行）交叉核对——未被任何集成用例覆盖的 P1 流即 finding（按业务关键度定级）；U21 格聚合面（跨域集成编排）覆盖判断落注记
      - Skill: none
      - **实核 2026-09-10**：testing-strategy §关键业务流全表 16 行（P0×4 + P1×7 + P2×5）逐行交叉核对——**P0 4/4 覆盖**（purchase=C01 / sales=C03 / inventory=C05+C06 / finance 凭证+过账+红字冲销=C01+C03+fin-service `TestErpFinVoucherReversePreview`/`TestErpFinReversalDispatch` 族）；**P1 7/7 覆盖 → 未覆盖 P1 流 = 0，零 finding**：fin 多币种+汇兑损益 = `TestErpFinExchangeRevaluation`（实核 `extends JunitAutoTestCase` L45，集成用例成立）+ `TestErpFinFxRateGuard` + `PropertyErpFinMultiCurrencyBalance`（fin-service 集成层）；fin 期末结账+成本核算 = C13 + `TestErpFinPeriodCloseEndToEnd`；quality/mfg/crm/cs/hr = C09/C07/C15/C16/C17 C 族直覆盖。P2 观察面（不立项口径）：aps=C08+C21 / contract=C18 / drp=C20a+C20b / b2b=C19 全 C 族覆盖；logistics 无 C 族用例但 service 层集成覆盖（`TestErpLogFreightPosting`+`TestErpLogCarrierGatewayIntegration`+`TestErpLogShipmentPostingEnd` 实盘在册）——登记观察非 finding。**U21 聚合面判断**：C01-C21 全部为跨域编排流（pur→inv→fin / sal→cs→fin / mfg→inv→fin / drp→pur / b2b→inv / prj→fin / hr→fin / ct→fin），跨域集成编排覆盖由 C 族承载充分，聚合面无缺口

Exit Criteria:

- [x] U21 DIM-T 格 verdict 落盘；`mvn test -pl app-erp-all` 与全 reactor 数字在案——**verdict = pass**（scoped 70/0/0/1 + 全 reactor 4006/0/0/1 双数字精确对账 MI/09-04 known-good 行，零新增失败）
- [x] C 族三层比对纪律 + 覆盖缺口对账结果在案——27 类↔27 目录零差异、RECORDING 残留 0、delVersion/`@var:` 屏蔽合规；P0 4/4 + P1 7/7 覆盖（未覆盖 P1 = 0 零 finding），聚合面 C 族承载充分

## Phase 6 — DIM-I E2E runbook 合规抽样 + CAT-4 全局面

> 类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/i18n-compliance.md` + `docs/audits/cjk-baseline.md` + `docs/testing/e2e-runbook.md` §编写规范）
> Targets: `tests/e2e/`（runbook 合规抽样面）+ 全域门控 + U21 白名单条目核对
> Prereqs: Phase 1 完成

- [x] <Proof> 零回归门控：`node tools/check-hardcoded-cjk.mjs --strict`（exit 0，vs 冻结 SNAPSHOT 单向收紧）+ `--self-test` PASS；CAT-4 全局面断言（页面 yaml 违规 0 = MI.8 收官态维持）；脚本红 = MI 回归升级裁决报 MI 通道，不立 DIM-I finding
      - Skill: none
      - **实核 2026-09-10（T0 会话延续）**：`--strict` → **RESULT: PASS, exit 0**（0 new violations vs 冻结快照 170 baseline files，170 处 improvement 全部 removed-from-tree 改善项；脚本自报 live CAT1..4 = **0/0/0/0**，3430 java + 886 yaml 扫描与 Phase 1 report mode 一致）+ `--self-test` → **RESULT: PASS (self-test green), exit 0**。CAT-4 全局面 = **0 sites / 0 files**（MI.8 收官态维持，脚本主表实跑值）。零红线漂移，无 MI 回归升级裁决触发
- [x] <Proof> runbook 合规抽样：`tests/e2e/` spec 抽样 ≥10（覆盖 dashboards/reports/crud-pages/business-actions/visual 四族）逐条核对 §编写规范强制条款——`E2E_ENGINE` 缺省 flux（engine 实现实证）、PageObject 模式、禁 GraphQL 断言、console 守卫链路；白名单 U21/app-erp-all 面条目核对（`cjk-baseline.md` §WHITELIST 记录条目数，条目 >0 抽 ≥3 核四要素）+ `grep -L "@Locale"` 全量 `*Errors.java` expect 空 + `git status --porcelain 'module-*/erp-*-meta/**' 'app-erp-all/src/main/resources/_vfs/i18n/**'` expect 空（`_` 前缀生成 i18n yaml 禁手改）
      - Skill: none
      - **实核 2026-09-10**：① **抽样 16 spec ≥10 达标，五族全覆盖**——reports: `ast-disposal.value`/`inv-inventory-trace.value`（`_helper.assertReportRenderedWithValue` 业务 token 断言，零选择器）；dashboards: `inv-snapshot.value`/`aps-schedule-gantt.value`（fixtures 登录导航 + GraphQLClient DIRECT `@BizQuery` 复核，runbook L443 自登记合法范式）；crud: `ast-ext-fields-audit.value`/`inventory.write`/`ast-inventory.write`（`CrudListPage`+`getEngine()` PageObject 模式 ✓，仅个别 `.cxd-Modal`/`.cxd-InputTable` 存量行）；business-actions: `reverse-preview.action`/`drp-simulation.action`/`hr-leave-shift-linkage.action`/`fin-bad-debt.action`（DIRECT action 全栈可达 = 非页面路径，禁 GraphQL 页面断言条款不触及）；visual: `dashboards.snapshot`（像素套件 helper 层）+ `fin-period-close-wizard.visual`/`ext-domains-list-filter.visual`/`field-format.value`（存量 spec 内联选择器 = runbook「已知差距（迁移方向）」节 L124-126 在案迁移债，L84 强制条款 scope =「新写、重写、修复」，存量逐步迁移口径 → 对账登记不立项）。② `E2E_ENGINE` 缺省 flux **engine 实现实证**：本项目分发副本 `tests/e2e/pages/engine.ts:11` `return 'flux'` ✓（观察注记：上游源 `nop-chaos-next/packages/e2e-shared/src/engine.ts:10` 缺省仍 `'amis'`——跨仓保护面，本地副本缺省 flux + runbook L135 `E2E_ENGINE=flux` 显式传递 + playwright webServer `-Dnop.web.render-mode=flux` 三重保障，不立项；e2e-shared 分发漂移已由 runbook「已知差距」节承载）。③ console 守卫链路：`fixtures.ts:21-34` 全局 fixture 层收集 console error/pageerror → 过滤后断言失败，全部 spec 经 fixture 继承 ✓。④ 白名单：§WHITELIST 总条目 = **27**（与 MI.9 终态「27 文件」精确一致）；**U21/app-erp-all 面 = 0 条**（27 条全为 module-* dao/service 面）；条目 >0 → 全局面抽 5 条（fin IErpFinApDocumentBiz/ErpFinDashboardBizModel/ErpFinApDocRuleClassifier/ErpFinApDocumentPipelineProcessor + ast IErpAstAssetBiz）四要素（文件路径/理由/owner doc 指针/裁决来源）**逐条齐备** ✓。⑤ `grep -L "@Locale"` 全量 `*Errors.java`（22 文件）→ **空输出**（22/22 全带 @Locale）✓。⑥ meta/i18n 脏面 `git status --porcelain 'module-*/erp-*-meta/**' 'app-erp-all/src/main/resources/_vfs/i18n/**'` → **空** ✓（生成 i18n yaml 零手改）

Exit Criteria:

- [x] U21 DIM-I 格 verdict 落盘；`--strict`/`--self-test` PASS 在案——**verdict = pass**（`--strict` PASS exit 0 live CAT1-4=0/0/0/0 + `--self-test` PASS exit 0 + CAT-4 0/0 收官态维持，零回归零漂移）
- [x] runbook 抽样 ≥10 spec 逐条结论 + 白名单四要素 + `@Locale` + meta/i18n 禁手改核对在案——16 spec 五族全覆盖（新代 spec 全合规/存量债 runbook 在案对账登记）；白名单 27 条 U21 面 0 条 + 抽 5 条四要素齐备；@Locale 22/22；meta/i18n 脏面空

## Phase 7 — 三态裁决汇总 + ck 报告落盘 + 双索引同步 + 零改动核证

> 类型：Add-heavy（2 Add + 2 Proof + 1 Decision）。
> Skill: code-quality-audit-prompt
> Targets: `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-app-erp-all-r3.md`（新增）+ `ai-check-r3-index.md` + `docs/audits/check/ai-check-index.md`
> Prereqs: Phase 2~6 完成（5 格 verdict 齐备）

- [x] <Decision> 全切片候选 finding 汇总复裁决：逐条确认三态（复用/归并/新立）与级别（P0~P3）一致性与 ID 规范（`P{n}-CK-app-{NNN}-r3`，历史 ID 永不覆写；升级级别按 code-history-deferred-triangulation-audit-prompt §3.1）
      - Skill: code-quality-audit-prompt
      - **复裁决 2026-09-10**：Phase 2~6 候选全量复核——**新立 2 全 P3**（P3-CK-app-001-r3 八扩展域保留层 action-auth 零 x:extends 生成层继承、P3-CK-app-002-r3 owner doc 漂移簇 2 doc 3 站点）+ **归并 2**（common-002/003 追加证据至原 ID）+ **复用 0**；级别一致性：2 条均 P3 无升级（§3.1 升级触发面核过——缺省配置零运行时影响/doc 软漂移均非 P2+ 面），ID 规范 `P{n}-CK-app-{NNN}-r3` 首用（`CK-app` 短码全索引 grep = 0 历史占用），历史 ID 零覆写；裁决证据落报告 §2
- [x] <Add> 落盘报告 `ck-app-erp-all-r3.md`：五维覆盖矩阵（5 格 verdict 全落）+ finding 列表（含归属标注与归并指针）+ 统计（P0~P3 计数）+ 剩余风险声明（横切关注点 13 完整四件套）
      - Skill: none
      - **落盘 2026-09-10**：`docs/audits/check/2026-09-06-1645-ai-check-r3/ck-app-erp-all-r3.md` 在盘——五维矩阵 5/5（B=finding 2 新立+2 归并 / F=finding 归并态 0 新立 / S=pass / T=pass / I=pass）+ §2 三态裁决五段（含 2.4 归属标注 + 2.5 ⑮抽样记录）+ §3 统计（新立 P3×2 / 复用 0 / 归并 2）+ §4 剩余风险四件套（已查/未深查边界/残留风险含 app-001-r3 enforcement 前置阻塞声明/successor 触发条件）
- [x] <Add> 双索引同步：本轮 `ai-check-r3-index.md` 产物清单追加 1 行 + 跨轮 `docs/audits/check/ai-check-index.md` §报告清单追加 M1.16 行 + §Finding 追踪追加新立 `-r3` ID 行（归并的追加证据至原 ID）
      - Skill: none
      - **同步 2026-09-10**：①本轮索引产物清单 `ck-app-erp-all-r3.md` 1 行追加（第 28 行产物，M1.16）；②跨轮索引 §报告清单 M1.16 行追加（P0/P1/P2/P3 = 0/0/0/2，状态 done）；③§Finding 追踪 `P3-CK-app-001-r3` + `P3-CK-app-002-r3` 2 行追加（状态 open，含 arm-index 复用裁决列家族注记）；④§Finding 追踪尾部 U21/M1.16 r3 复核注记 1 条（归并 2 条追加证据至 common-002/003 原 ID，原行 open 维持零覆写）
- [x] <Proof> 零生产代码改动核证：`git status --porcelain` 过滤 `module-*`/`app-erp-all` 生产路径 expect 空（roadmap 规则 6；触碰面 = 执行目录报告 + 索引 + 本计划勾选注记）
      - Skill: none
      - **实核 2026-09-10 02:24（+08:00）**：`git status --porcelain` 过滤 `module-*`/`app-erp-all` 生产路径 → **空** ✓；全量脏面 = 4 条且与声明触碰面精确一致（`ai-check-r3-index.md` M + 跨轮 `ai-check-index.md` M + `ck-app-erp-all-r3.md` ?? + 本计划文件 ??）——零生产代码改动不变式保持
- [x] <Proof> 收尾复核：`mvn test -pl app-erp-all` 复跑全绿（审计只读不变式复证；全仓验证归收官机制）
      - Skill: none
      - **实核 2026-09-10 02:24（+08:00）**：`mvn test -pl app-erp-all` 复跑 → **Tests run: 70, Failures: 0, Errors: 0, Skipped: 1, BUILD SUCCESS**（1:44 min）——与 Phase 5 首跑及 09-04 known-good 行三重一致，审计只读不变式复证成立

Exit Criteria:

- [x] `ck-app-erp-all-r3.md` 落盘且五维矩阵 5 格 verdict 完整（U21 × 5 = 5 格，缺一格不算完）——矩阵 5/5：B=finding / F=finding（归并态）/ S=pass / T=pass / I=pass
- [x] 双索引行追加在案；零生产代码改动核证通过；app-erp-all 复跑全绿——本轮索引 +1 行 + 跨轮索引 §报告清单 + §Finding 追踪（+2 ID 行 + U21 复核注记）；生产路径过滤空；复跑 70/0/0/1 BUILD SUCCESS

## Draft Review Record

- dispatch review #review-2026-09-09-210030-mission-driver-2026-09-09-2100-2-m116-app-erp-all-cross-cutting-five-dim-audit-1-91dba3a4 to 2026-09-09-210030-mission-driver
- 2026-09-09：iteration 1，共识 accept #review-2026-09-09-210030-mission-driver-2026-09-09-2100-2-m116-app-erp-all-cross-cutting-five-dim-audit-1-91dba3a4（修正基线陈旧内联计数——姊妹报告/索引行 6 处「15」实仓复核更正为 27（M1.1~M1.15 多单元切片 M1.12×2/M1.13×3/M1.14×5/M1.15×6，lesson-13 同型 RC-R1.89 先例；Phase 1/2/3「27 份姊妹报告」对齐）；按 M1.14/M1.15 同批先例为本只读审计计划定制 Closure Gates（ledger 格式门控以非复选框条目记录，5 格/双索引/零改动核证/独立结束审计门）；措辞精确化：「最后一个未起草」改「最后一个 M1 工作项（本计划即其执行草案）」、「跨轮索引无 M1.16 行」补注 5 处「归 U21/M1.16」归属标注、「工作树零脏面」改「tracked 零修改 + 本批两份计划 untracked 在案」；Verification/Closure 保持闭包时回执槽位与批内姊妹一致；基线断言逐一实仓复验在盘——HEAD `060ddab0a`、MI 终态行 4006/0/0/1 + CAT 0/0/0/0 + `--strict`/`--self-test` PASS + 白名单 27/27（known-good-baselines 2026-09-08 行）、app-erp-all 70/0/0/1（2026-09-04 行）、seed-data.md 最新行 372 CSV+1 SQL、checker 差值 R2b +2/R2c +5/R12a +1（09-06 行 R2b=242/R12a=71 vs 机器块 240/70 + 08-31 行 R2c 1542 vs 1537）、冻结清单 §3.3 U21 行焦点程式逐字一致 + §4 第 16 行 U21 × 五维 + §3.2 聚合横切面归属 + §6 勘误 E1 路径实仓在盘、roadmap M1.16 行 owner docs/Skill「同上」链、跨轮索引 5 处归属标注、27 份姊妹报告在盘、C01-C21 族实仓在盘、`TestErpSeedDataIntegrity`/`tools/check-hardcoded-cjk.mjs`/`scripts/start-app.sh` 实仓在盘、命名技能三份均注册于 `docs/skills/README.md`；同批 2100-1 compliance 前置属执行时依赖非计划缺陷，不阻塞本切片；frontmatter `status: draft` → `active`）

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。本计划为只读审计（零生产代码改动），验证命令组即结果表面本身（Phase 1~7 所列），完整仓库验证不在此重复（MV.1 对照面 = `known-good-baselines.md` 2026-09-08 MI 终态行；roadmap 收官机制 M1.17 承载全仓回归与 done 翻转）。ledger 格式计数域仅含 Phase 勾选项，本节门控以普通条目记录核对结果。

- 范围内行为完成（Phase 1~7 全部执行项与退出标准 `[x]`；U21 × 五维 = 5 格 verdict 完整——`ck-app-erp-all-r3.md` 覆盖矩阵 5/5，缺一格不算完）
- 相关文档对齐（双索引同步在案——本轮 `ai-check-r3-index.md` 产物清单 +1 行 + 跨轮 `ai-check-index.md` §报告清单 M1.16 行与 §Finding 追踪新 `-r3` ID 行；历史 ID 零覆写；27 姊妹报告「归 U21」移交项消费裁决在案；零生产代码改动 = owner doc 零对齐义务）
- 已运行验证：Phase 1 双 checker 红线（compliance checker 对照机器块现值/`2100-1` successor 落地态 + CJK report mode 对照 0/0/0/0 终态）+ Phase 3 `npm run validate:flux` step [1/3] 导出 0 error（999/855）+ Phase 4 `TestErpSeedDataIntegrity` 全绿 + Phase 5 `mvn test -pl app-erp-all`（对照 70/0/0/1）与全 reactor `mvn test`（对照 4006/0/0/1）+ Phase 6 `--strict` PASS + `--self-test` PASS + Phase 7 零改动核证与收尾复跑全绿——各命令数字落 Phase 勾选注记；任一红线漂移 = 登记裁决后再闭包，不就地吞掉
- 无范围内项目降级为 deferred/follow-up（finding 的「复用/归并」标注与修复归 M2.x 是审计产出分流，非范围降级）
- 独立草案审查已完成并记录（见 Draft Review Record，iteration 1）
- 文本一致性已验证：状态、阶段、门控和日志都一致（ledger 格式：frontmatter `status: active` 保持，完成态由全部勾选 + `## Verification` pass 线 + `## Closure` 回执派生；当日日志条目 `docs/logs/2026/09-09.md` 落盘）
- 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- 结束证据存在于文件中（Phase 勾选注记 + `ck-app-erp-all-r3.md` + 双索引行 + `## Verification` pass 线 + `## Closure` 审计回执）

## Verification

- pass test 2026-09-10-024246 exit=0 —— 闭包审计 visit 实跑全 reactor `mvn test` BUILD SUCCESS exit 0（13:18 min；T0 `8e8ab7fca` 后零生产代码变化，与 2026-09-08 MI 终态行 4006/0/0/1 基线同口径零新增失败，BUILD SUCCESS = 全 reactor 零 failure 零 error）

## Closure

- dispatch audit #audit-2026-09-10-024246-2026-09-09-2100-2-m116-app-erp-all-cross-cutting-five-dim-audit-1-838c9bbe to 2026-09-09-210030-mission-driver models={exec:zhipuai-coding-plan/glm-5.3-flash,aud:zhipuai-coding-plan/glm-5.3-flash}
- accepted #audit-2026-09-10-024246-2026-09-09-2100-2-m116-app-erp-all-cross-cutting-five-dim-audit-1-838c9bbe：独立闭包审计 ACCEPT——U21 × 五维 5/5 格 verdict 落盘（B=finding：2 新立 P3[app-001-r3 扩展域 action-auth 继承断链 / app-002-r3 owner doc 漂移簇 3 站点] + 2 归并[common-002/003]；F=finding 归并态 0 新立；S=T=I=pass），报告 `ck-app-erp-all-r3.md` 五维矩阵/三态裁决/统计/剩余风险四件套完整 + 双索引 4 处同步在案，零生产代码改动核证保持；闭包 visit 实跑全 reactor `mvn test` BUILD SUCCESS exit 0（runId 2026-09-10-024246，= MI 终态行基线零新增失败），`plan-check.mjs --strict` 绿 derivedCompleted，语义审计（34/34 勾选一致性 / 退出标准对实仓 / 反 hollow / deferred 诚实：2 条 P3 修复归 M2.x 为审计产出分流非范围降级 / 日志与实仓一致）无残留
