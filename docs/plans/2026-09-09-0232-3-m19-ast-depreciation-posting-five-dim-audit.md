---
status: active
mission: ai-check-r3
work-item: M1.9
group: "2026-09-09-0232"
verify: [test]
---

# 2026-09-09-0232-3 M1.9 assets ast-2 五维符合性审计（折旧与过账 + 盘点切片）

## Current Baseline

- 依赖已满足：M0.6 done（plan `2026-09-06-1451-3`）；MI.9 收官 done（plan `2026-09-07-1715-3`，独立 closure audit ACCEPT，2026-09-08；`docs/testing/known-good-baselines.md` 2026-09-08 `ai-check-r3 MI 终态行`：全 reactor `mvn test` 4006/0/0/1、CJK report mode CAT1..4 = 0/0/0/0、双 checker 零回归、白名单 27 文件四要素齐备）；前置切片 M1.8（ast-1，plan `2026-09-08-1042-3`）已执行且闭包审计 ACCEPT（2026-09-08，审计时点 HEAD `69851bb2`；roadmap done 翻转按 M1.x 同批先例归 owner/engine 处置，不阻塞本切片）。
- 强制核对矩阵已冻结：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md`——本计划唯一格集合 = **U06 × 五维 × ast-2**（§4 映射表）；判定标准/机械核查程式/跨轮查重程序以该冻结清单 §1/§2 为准；ast-2 单元焦点 = 折旧批量（nop-job）/资产过账/盘点→凭证闭环。
- 切片范围（U06 ast-2）：折旧批量链（nop-job 路径 + catchUp 补提 + 折旧计划行生成/重算 + 资本化维修基数）/资产过账链（AcctDocProvider/PostingDispatcher 族 + 处置/盘点凭证联动）/盘点→凭证闭环（盘盈盘亏差异→凭证，不含盘点单生命周期本体）；owner doc `docs/design/assets/depreciation-and-posting.md`（351 行，roadmap M1.9 行指定）+ `use-cases.md`（244 行，实仓核验在盘）；物理面 `module-assets/erp-ast-{dao,service,web}` 的 `src/main`。
- 切片边界（冻结清单 §3.2 + U06 行 + M1.8 边界）：资产卡片生命周期状态机/资本化/拆分合并/价值调整/处置生命周期/盘点单生命周期与变动处置归 ast-1（M1.8 已审）——本切片发现生命周期面缺陷时标注「归属 ast-1」归并；posting 引擎内部归 fin-1（M1.1 已审）；common 抽象族（`AbstractProcessor.illegal*`、状态锁基类）行为缺陷归 U20（M1.15），本切片只审调用点合规；聚合横切面归 U21（M1.16）。
- 跨轮查重源（§2）：r1 `docs/audits/check/ck-assets-depreciation.md`（0 P0 / 6 P1 / 8 P2 / 9 P3 = 23 条；跨轮索引状态 8 fixed / 15 open——P1-CK-ast2-001 UNITS 显式失败等经 F2.9 修复，HEAD 复核义务在 Phase 2；F2.9 新增的 `ErpAstDepreciationReversalListener` 即 compliance R12a 第 71 处 import 漂移源（commit `0a825a42a`，机器基线块差距 successor `ai-check-r3-compliance-baseline-raise` 在案），本切片 DIM-B 复核其合规面）+ r2 只读目录 `docs/audits/check/2026-08-28-2049-ai-check-r2/` + 索引 §Mission 基线快照；新立 ID 序列接续 `P{n}-CK-ast2-{NNN}-r3`（自 024 起，历史 ID 永不覆写）。
- DIM-I 内嵌 MI 先行时序口径（冻结清单 §1.5）：ast 探针族（CAT-1 39 / CAT-2 29±）MI 后已清零——本切片 DIM-I 维仅验证零回归与白名单合规，不再产生同类 finding；脚本红 = MI 回归升级报 MI 通道；白名单四要素缺失 = 白名单登记缺陷 finding。
- 绿色基线：`docs/testing/known-good-baselines.md` 2026-09-08 MI 终态行（MV.1 对照面）；compliance checker 对照面 = M0.3 快照行（R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42）；机器基线块差距归 successor `ai-check-r3-compliance-baseline-raise`。
- 仓库现状（2026-09-09 草案时点实核）：HEAD `7ad2e426b`（姊妹 M1.4 计划落盘提交）；脏面 = 本批 3 份 `2026-09-09-0232-*` 计划文件（草案产物）。审计证据一律以实跑时 HEAD + 脏面披露为准（Phase 1 机械登记）。
- 模块级回归参照：`mvn test -pl module-assets/erp-ast-service` 339 全绿（M1.8 执行两轮实测；= MI.6 批 1/2 批注账 337 + 姊妹 StateMachine 计划矩阵测试增量）；known-good-baselines 无模块级 ast 计数行，实跑计数照实登记不预填。
- 剩余差距：U06 × 五维 × ast-2 五格 verdict 未落盘；ast-2 无 `-r3` 切片报告。

## Goals

- 按冻结清单对 U06 × 五维 × ast-2 五格全跑（禁止抽样、禁止跳维），逐格落 verdict，产出 `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-assets-depreciation-r3.md`（五维覆盖矩阵 + finding 列表 + 统计 + 剩余风险声明）。
- 每候选 finding 完成三态查重裁决（复用/归并/新立 `-r3` ID，历史 ID 永不覆写）；DIM-B 维含 owner-doc 关键断言抽样 ≥2 doc × 2 断言（漂移则扩至全部 owner doc）。
- 双索引同步：本轮 `ai-check-r3-index.md` 产物清单 + 跨轮 `docs/audits/check/ai-check-index.md`。
- 全程零生产代码改动（roadmap 规则 6），收官 `git status` 机械核证。

## Non-Goals

- 不修复任何 finding（修复归 M2.x）；不改任何生产代码/ORM/api.xml/配置/页面/seed 文件。
- 不覆盖 ast-1（M1.8 已审）格与其他单元格；资产卡片生命周期/盘点单生命周期面缺陷按边界标注归并不立项。
- 不接管 r1/r2 工作项；不重开既有裁决（F2.9 修复方案、lesson 09/10、闲置超期 cron 的 Deferred 裁决）；不做 roadmap 状态翻转。
- DIM-I 不产生同类 CJK finding（§1.5 内嵌口径）。

## Phase 1 — 执行目录就位 + 切片红线与脏面披露

> 统一类型：Proof（3 项 Proof）。
> Skill: none
> Targets: `docs/audits/check/2026-09-06-1645-ai-check-r3/`（幂等复用）；盘点注记落本计划勾选注记
> Prereqs: 无（M1.8 已执行；本批执行序第 3，与 M1.6/M1.7 并行起草）

- [x] <Proof> `mkdir -p docs/audits/check/2026-09-06-1645-ai-check-r3/`（幂等复用；roadmap 规则 8），确认索引与冻结清单在位——目录已在位（M0.3~M1.7 产物 28 件）；`ai-check-r3-index.md` 头部登记路径与本计划一致；`m0-5-audit-checklists.md` 冻结清单在位（§4 映射表 M1.9 = U06 × 五维 × ast-2）
      - Skill: none
- [x] <Proof> 记录审计时点仓库状态：HEAD hash、`git status --porcelain` 脏面清单、姊妹在制会话披露；后续全部证据注记引用该时点——T0 = HEAD `a23975f2158145396af58bcc2ca734ea0f7cf1cc`（2026-09-09 04:58 `plan-2026-09-09-0232-2 M1.7 mfg-3 五维审计落盘 + 闭包审计回执 ACCEPT`）；`git status --porcelain` 脏面 = 仅 `?? docs/plans/2026-09-09-0232-3-m19-ast-depreciation-posting-five-dim-audit.md`（本计划自身，草案产物）；姊妹在制披露：M1.6（`7ad2e426b`→`c63d71bb3`）/M1.7（→`a23975f21`）已执行并提交落盘，同批无其他在制会话脏面。以下全部证据注记均以 T0 为准
      - Skill: none
- [x] <Proof> 切片红线基线：`bash docs/audits/nop-compliance-checker.sh`（对照 M0.3 快照行）+ `node tools/check-hardcoded-cjk.mjs`（report mode，对照 0/0/0/0 终态）实跑记录；漂移即登记（不就地裁决）——checker 实跑全表 R1a=0/R1b=0/R1c=0/R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R4=0/R5=0/R6=2/R7=0/R8=0/R10=14/R11=0/R12a=71/R12b=66/R12c=42，逐项 = M0.3 快照行零漂移；CJK report mode exit 0：CAT-1=0/CAT-2=0/CAT-3=0/CAT-4=0（CAT-5 注释 21158 行豁免仅统计），= MI 终态 0/0/0/0 对账一致
      - Skill: none

Exit Criteria:

- [x] 执行目录就位且本轮索引头部登记路径一致；时点/脏面披露在案
- [x] 双 checker 红线数字在案且与 M0.3 快照行 / MI 终态行对账一致（或漂移已登记）——两向均零漂移，无登记义务

## Phase 2 — DIM-B 后端平台合规走查（15 维度 + R1-R12 + 断言抽样）

> 统一类型：Proof-heavy（3 Proof + 1 Decision）。
> Skill: nop-platform-conformance-audit-prompt + code-quality-audit-prompt（roadmap M1.9 行指定）
> Targets: `module-assets/erp-ast-dao|erp-ast-service/src/main/java`（ast-2 范围 = 折旧/过账/盘点→凭证族文件）
> Prereqs: Phase 1 完成

- [x] <Proof> 冻结清单 §1.1 机械核查程式全套实跑：compliance checker 逐规则对照 + 反模式 grep 族 + codegen 产物安全 + 聚合完整性（`app-erp-all/src/main/resources/_vfs/nop/main/auth/app.action-auth.xml`）——① checker 19 规则（T0 实跑）逐项 = M0.3 快照行零漂移（R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R4=0/R5=0/R6=2/R7=0/R8=0/R10=14/R11=0/R12a=71/R12b=66/R12c=42/R1a/b/c=0；R12a=71 含 F2.9 新增 `ErpAstDepreciationReversalListener` import 站即机器基线块差距 successor 在案的已知第 71 站，非新增漂移）；② 反模式族（ast dao+service src/main）：`extends RuntimeException`=0、`@Inject private`=0、`System.currentTimeMillis`=0、`@Transactional`∩`@BizMutation`=0（唯一命中 AssetPostingExecutor 为 javadoc 引注 REQUIRES_NEW Facade 边界，真实注解=0）；`IDaoProvider/IOrmTemplate` 命中点逐一核对=既有裁决范式（per-mutation Processor `daoProvider.daoFor(本模块实体)` 编排层标准 + `orm()` helper 仅 flushSession/getOrmTemplate + Dashboard/Report javadoc 豁免 + dispatcher `resolveSubjectCode daoFor(ErpMdSubject)`/`findPeriod daoFor(ErpFinAccountingPeriod)` = P1-MA4-015 data-dependency-matrix 已裁决豁免），无新增无理由站点；③ codegen 产物安全：`git status --porcelain` 产物路径空 + `__XGEN_FORCE_OVERRIDE__` 35 处全为 codegen dict.yaml 只读校验点零手改；④ 聚合完整性（§6 勘误 E1 路径）：`nop/main/auth/app.action-auth.xml` x:extends 含 `/erp/ast/auth/erp-ast.action-auth.xml`（L10）
      - Skill: nop-platform-conformance-audit-prompt
- [x] <Proof> 15 维度逐维走查 ast-2 范围（重点：⑥ nop-job 折旧批量与 catchUp 补提语义、折旧方法族（UNITS 显式失败——F2.9 修复复用 HEAD 复核）、资本化维修基数、资产过账 Provider/Dispatcher 注册与 REQUIRES_NEW 面（F2.9 新增 ReversalListener 合规面）、盘点→凭证闭环联动；涉 posting 引擎内部时标注归属 fin-1）+ blocker/major/minor 分级——15/15 无跳维：①决策顺序（无新增可模型化硬编码；ast2-010 死代码 open 在案）；②跨实体（ReversalListener 经 I*Biz findList/updateEntity + javadoc 纪律声明，零新增 daoFor；facade `currentUserId` 宽 catch 站点归并 P3-CK-ast-022 扩员）；③异常（全部 NopException+ErrorCode 中文描述）；④IoC/事务（ReversalListener `@Inject` 包级可见；REQUIRES_NEW 钉 `IErpFinVoucherBiz.post` Facade，executor 零自带事务注解）；⑤平台辅助（`CoreMetrics.today()` 已替换 Java 侧 YearMonth.now——bug 2026-09-01-0058；batch.xml L14 XML 站点仍直读系统时钟→ast2-016 归并更新证据）；⑥标准服务模式（Facade+per-mutation Processor+StateMachine Bean 全链在位；executeDepreciation 重执行路径 REVERSED 行重绑为数值自洽的自愈语义——r1 ast2-011 F1.3 已裁决 fixed，CRUD 面复核在位，域内 mutation 半边残留作 residual 注记不重开）；⑦跨模块引用（无 notGenCode 违规、依赖单向）；⑧状态机（schedule dict 4 值全有 writer：CANCELLED 经 ast2-004 修复的 PENDING-only cancelSchedules；无新增死状态）；⑨审批流与作业（nop-batch job yaml→nopBatchTaskRunner→depreciation.batch.xml chunk batchSize=50 三件套功能在位；ast2-015 键名/类名漂移 open 归并；闲置超期 cron Deferred 裁决不重开）；⑩定制顺序（Processor javadoc Delta 覆盖声明在位）；⑪多租户/本地化（schedule 落 orgId、事件 orgId→AcctSchemaResolver；报表/看板无 orgId=ast2-023 open 归并）；⑫测试→指针 DIM-T（Phase 5）；⑬Codegen 安全（见①③）；⑭聚合完整性（见①④）；⑮断言抽样（见下一项）。分级：blocker=0、major=0、minor=2（新立 P2-CK-ast2-024-r3 CATCHUP 汇总凭证红冲闭环缺口 + P3-CK-ast2-025-r3 owner doc 未同步簇，见 §裁决）；F2.9 修复族 HEAD 复核 8 条全有效（001 UNITS 显式失败 ExecuteDepreciation L67-72/CatchUp L89-93、002 基数去 `.add(increment)` Recalc L58-64/88-89、003 当月增加下月提守卫 L46-61、004 逆资本化守卫 L122+cancelSchedules PENDING-only L337-339、005 ReversalListener L245-246 bean 注册+reverseDepreciation L50-62 REVERSED 跳过双应用、006 VA originalValue 同步 L253/289+Disposal 净值读 BILL_DATA_NET_BOOK_VALUE L75-82（1604 腿 Deferred 维持裁决）、011 F1.3 基类 Schedule/Asset BizModel extends AbstractErpCrudBizModel、021 (c) 收敛 billHeadCode=资产码#期间稳定+引擎幂等）；posting 引擎内部（ErpFinPostingProcessor 幂等/sweep/resolveSubjects）未深查——归属 fin-1 标注
      - Skill: nop-platform-conformance-audit-prompt
- [x] <Proof> 维度⑮ owner-doc 关键断言抽样：`depreciation-and-posting.md` + `use-cases.md` 中 ≥2 doc × 2 关键断言（折旧方法语义/当月增加下月提/过账科目映射/盘点差异处置）对照代码；≥2 处漂移扩大至全部 owner doc——实抽 2 doc × 9 断言：一致 7（§十 业务类型码 70/80/390 ↔ ErpFinBusinessType L20-52；§十 期间 OPEN/CLOSED 判定 ↔ requirePeriodOpen+ErpAstConstants L141-142；UC-AST-02 残值截断 ↔ Calculator L71-74；UC-AST-07 补提凭证标注所属期间 ↔ DepreciationAcctDocProvider L50-57 memo；UC-AST-07 方式B 守卫链 ↔ CatchUpProcessor；UC-AST-09 盘点差异凭证 460+分支科目 ↔ AssetInventoryAcctDocProvider+ErpFinBusinessType L59；§5.1 当月增加下月提 ↔ ExecuteDepreciation L46-61 守卫）；漂移 2（A：§1.3 工作量法行仍声明公式+「需维护累计工作量」而 F2.9 后 UNITS 显式失败，且代码注释 L68/L89 与索引修复注记引用的「owner doc §十 登记 Deferred」在 §十 实文缺失；B：§5.1 L225「当月减少当月停」vs UC-AST-05 L94+代码补提至出售期含当期——r1 已注记「归 C4.4 裁决后修订」而 ast-1 r1/M1.8 收官后文本未修订；另 §7.2 G4 注未提及 ReversalListener 引擎侧红冲回退为不完整型注记）→ 触发扩样，两 doc 全文走查完成（§5.1 L218 ErpAstDepreciationJob 声明+§十 L349 nop-job Deferred 行与 job yaml 实态互斥 → 归 ast2-015 扩员，不重复立项）。漂移 A+B 聚簇新立 P3-CK-ast2-025-r3
      - Skill: code-quality-audit-prompt
- [x] <Decision> 本维候选 finding 逐条过 §2 三态裁决（查 r1 `ck-assets-depreciation.md` 23 条 finding + r2 + 基线快照；fixed 项（F2.9 族）复用并复核 HEAD 有效性，open 项同型归并原 ID，ast-1 面标注归属归并）；裁决证据落勾选注记——三态裁决（查重源：r1 ast2 23 条逐一比对 + r2 index F2.9 簇 + §Mission 基线快照）：**复用 8**（ast2-001/002/003/004/005/006/011/021，F2.9/F1.3/(c)收敛修复 HEAD 复核有效，见上行证据；011 residual 注记：域内 mutation 半边 REVERSED 重绑为数值自洽自愈语义不重开）；**归并 15**（ast2-007 elapsed 口径 L96/facade countExecuted L126-131 原样；008 单事务逐资产吞异常 Batch L47-53 原样；009 维修无条件翻 POSTED PostProcessor L82+CAPITALIZE 先行原样；010 VA 死代码块+config 空转原样；012 N+1 findAllByQuery 原样；013 inventorySubject 死变量+四科目硬编码原样；014 ERR_DEPRECIATION_USEFUL_LIFE_INVALID 零消费原样；015 死常量 3 枚零消费+ErpAstDepreciationJob 零命中+nop.job.* 实键，扩员 §十 L349 stale Deferred 行；016 Java 站点已修 CoreMetrics.today（索引 open=回填缺口，随 fin4-013 先例注记）+batch.xml L14 XML 站点仍在；017 exchangeRate=ONE L151/197 原样；018 plannedAmount SL-only 原样；019 setResidualValue(ZERO) L241 原样；020 currentValue 8 writer 多语义+depreciationRate 零消费原样；022 created.isEmpty() 早退+total=0 不发凭证 L148-150/185-187 原样；023 report 全表无 orgId 原样）+ **跨切片扩员 2**（P3-CK-ast-022 族新增 facade currentUserId 站点 L139-146；P3-CK-ast-026 族新增 ReversalListener.findAssetByCode 无 orgId（limit 1 在位）站点 L99-105）；**新立 2**：P2-CK-ast2-024-r3（F2.9 闭环残留——CATCHUP 汇总凭证引擎侧红冲被 listener L65-68 静默跳过 + 逐期 reverseDepreciation 的 billHeadCode 资产码#漏提期 与 GL 实存 资产码#当期#CATCHUP 键失配 → GL 侧红冲 CATCHUP 凭证后计划行滞 posted=true/资产累计不回退且无自愈通道无告警；控制点为新发，ast2-005 已 fixed 不适用归并）、P3-CK-ast2-025-r3（dimension⑮ 漂移簇 A+B+§7.2 注记，见上）；历史 23 ast2 ID + 28 ast ID 零覆写；r2 目录核对无同型独立登记。替代方案考量：024-r3 曾考虑归并 ast2-022（同域补提链）——否，022 是域内重试通道缺口（补提凭证从未成功），024 是 GL 侧红冲回退缺口（凭证已成功后被红冲），触发面/修复方向不同，分立；级别定 P2 依据：真实运营操作（财务红冲凭证）触发、GL↔资产静默漂移无告警，但限于 CATCHUP 凭果子集故不升 P1。残留风险：024 的引擎侧 VoucherReversedEvent 对手红冲派发语义经 F2.9 修复测试验证在位（TestErpAstDepreciationReversalListener），本裁决仅依赖其存在性
      - Skill: code-quality-audit-prompt

Exit Criteria:

- [x] U06×B×ast-2 格 verdict 落盘，15 维度无跳维——verdict = **finding**（新立 2：P2-CK-ast2-024-r3 + P3-CK-ast2-025-r3；复用 8 / 归并 15 + 跨切片扩员 2）；15/15 维度走查完成
- [x] 机械程式输出数字在案且与基线对账一致；候选 finding 三态裁决完成——checker 19 规则= M0.3 快照行零漂移；反模式族全零；三态裁决 8/15+2/2 落注记

## Phase 3 — DIM-F 前端页面与 E2E 走查

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = 冻结清单 §1.2）
> Targets: `module-assets/erp-ast-web/src/main/resources/_vfs`（ast-2 面：资产台账/折旧表相关页面）
> Prereqs: Phase 1 完成

- [x] <Proof> 全局面静态门禁实跑：`npm run validate:flux`（导出 0 error；325 条既有 variant 外部漂移为 successor 在案，非本切片 finding）+ flux-only grep（AMIS 保留层 / ORM `ext:web-renderer="flux"`）——step[1/3] `FLUX_PAGE_ERROR_COUNT: 0 pageCount=999 erpPages=855` + 终态 `files=855 validated=855 errors=325`（exit 1 余项全部为 `variant="primary"`×dropdown-button 既有枚举漂移同型，ast 域无新增族成员，successor（roadmap MI.8 done 行）在案非本切片 finding，对账 = M1.8 同数字）；`component="AMIS"` 保留层 0；`grep -L ext:web-renderer="flux"` 全 ORM 空
      - Skill: none
- [x] <Proof> ast-2 页面走查：资产台账/折旧表/折旧计划相关页与手写页（M0.4 源头链查表判定）对照 view-and-page-strategy + ui-patterns（REST `/r/` / `x:extends` 定制 / i18n-en 承载）逐页落 verdict；涉 assets E2E spec 时核对 PageObject + flux 引擎缺省 + 禁 GraphQL 断言——逐页 verdict（无跳页）：`ErpAstDepreciationSchedule.view.xml` 保留层 `x:extends="_gen/…"`+bounded-merge+自定义列全量 `i18n-en:label`（资产编码/所属组织/币种）；`report/asset-depreciation-detail.page.yaml`+`asset-disposal-detail.page.yaml` 数据访问 `@query:ErpAstReport__renderHtml`（REST `/r/` 约定）+ i18nEn 7/6 处；`asset-stocktake` `@query:ErpAstInventory__findPage`+i18nEn 13；`disposal-wizard` `@query:ErpAstAsset__findPage/__get`+`@mutation:ErpAstDisposal__save`+i18nEn 16；`asset-repair` = 注册 Deferred stub（plan 2026-08-03-1232-4，无数据访问义务，i18nEn 2 处承载）；`dashboard/main.flux.yaml`+`main.page.yaml` 孪生双文件 periodId 参数名一致（无 mfg-023 型孪生漂移）；ast web 全域 graphql 命中 0；M0.4 源头链查表（rows 1/3/4/7/8/9）判定与实仓一致、MI.8 补 i18nEn 义务维持修复态；E2E：ast spec 族（ast-depreciation/ast-inventory-count/ast-maintenance×2/ast-cip-capitalization/ast-value-adjustment）= 业务动作 API 驱动型（`callMutation` 经 GraphQL 直驱 @BizMutation——runbook 业务动作套件登记通道，非页面 GraphQL 断言禁用面），`data-slot/data-testid/.cxd-` 违规 0，`E2E_ENGINE` 缺省 flux（engine.ts L9-14）——**U06×F×ast-2 verdict = pass，0 新立**
      - Skill: none

Exit Criteria:

- [x] U06×F×ast-2 格 verdict 落盘；全局面门禁数字在案对账一致——pass；999/855 + 325 与 M1.8 轮对账一致
- [x] ast-2 范围页面逐页走查完成，无跳页——7 页族 + dashboard 孪生 + 6 ast spec 逐页/逐 spec 落 verdict

## Phase 4 — DIM-S seed 数据走查

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/seed-data.md` + `docs/design/assets/seed-data.md`）
> Targets: `app-erp-all/src/main/resources/_vfs/_init-data`（只读清点）+ ast 相关 seed（asset/depreciation_schedule 族）
> Prereqs: Phase 1 完成

- [x] <Proof> 引用完整性门禁实跑：`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 全绿——**4/0/0/0 全绿 BUILD SUCCESS**（= M1.8 轮同计数零回归；363 实体全量 findAll + to-one 零悬空 + 零孤儿 CSV + scope-pinning 门控全过）
      - Skill: none
- [x] <Proof> ast-2 seed 面核对：`git status --porcelain` seed 路径 expect 空 + deploy `_seed_*.sql` 同步义务查登记处表 + asset/depreciation_schedule seed 金额自洽抽查（asset↔最新 schedule，2210-1 批口径；按 seed-data.md 运营域约束段）——`git status --porcelain _init-data/` 空（零 seed 变更，双面快照重录义务未触发）；seed 清点 **372 CSV + 1 SQL** = 冻结口径「M1.5 批次后」登记值；deploy `_seed_*.sql` 全仓仅 cs/notify 两族且均已在 seed-data.md 登记处 ✅ 已聚合（无第三态；ast 无 deploy seed 零同步义务）；自洽抽查：asset id=2（AST-2026-002 original=120000/accum=6000/NBV=114000）↔ 最新 schedule（period 2026-07 EXECUTED actual=2000/accum=6000/NBV=114000/posted=false 无凭证回链义务）双字段全等（seed-data.md 运营域约束段）；id=1/3（accum=0 无 schedule 行）内部自洽
      - Skill: none

Exit Criteria:

- [x] U06×S×ast-2 格 verdict 落盘；`TestErpSeedDataIntegrity` 全绿在案——verdict = **pass**
- [x] seed 零变更 + 同步义务核对 + 自洽抽查结果在案——三项均零异常在案

## Phase 5 — DIM-T 单元测试走查

> 统一类型：Proof（3 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/testing-strategy.md`）
> Targets: `module-assets/erp-ast-service`（`<SVC>` 绑定：`<SVC>` = `module-assets/erp-ast-service`，冻结清单 §1.4 程式记号）
> Prereqs: Phase 1 完成

- [x] <Proof> 切片本地回归：`mvn test -pl module-assets/erp-ast-service` 全绿零失败（模块级参照 = 339 全绿（M1.8 执行后实测）；全仓聚合对照面 = known-good-baselines 2026-09-08 MI 终态行 4006/0/0/1；实跑计数照实登记不预填）——实跑 **339/0/0/0 全绿 BUILD SUCCESS**（= 模块级参照 339 精确一致，零失败零回归）
      - Skill: none
- [x] <Proof> 覆盖缺口对账：公开方法清单 × `_cases` 测试目录清单逐项对账（ast-2 范围 = 折旧/过账/盘点→凭证族 BizModel 与 job 入口）；关键业务流清单核对——折旧计提→计划行→过账凭证→红冲/补提（P1 清单族）逐行核覆盖；缺口按业务关键度定级——对账：DepreciationSchedule 5 mutation ↔ TestErpAstDepreciation（含 F2.9 回归行 testUnitsMethodRejectedNotSilentZero/testPeriodBeforeAcquisitionMonthRejected）+ TestErpAstCatchUpDepreciation + TestDepreciationCalculator + TestErpAstDepreciationScheduleStateMachineMatrix + `_cases/job/TestErpAstDepreciationJob`；过账族 ↔ TestErpAstAcctDocProviderAccountKey + TestDepreciationPostingFailureAlert + TestAstPostingFaultInjection + TestErpAstPostingReverse（域侧红冲 5 测试：Cap/折旧/处置 reverseApprove + posted=false 悬挂不对称 + E2E 全链）+ TestErpAstCapitalization（含 testReverseApproveRejectedAfterDepreciationExecuted）+ TestErpAstMaintenance（含 F2.9 计划总额断言）；盘点→凭证 ↔ TestErpAstInventory + TestErpAstInventoryStateMachineMatrix；VA ↔ TestErpAstValueAdjustment；关键业务流行：计提→计划行→过账凭证→红冲→补提五行单测全绿；**缺口 1 处新立 P2-CK-ast2-026-r3**：`ErpAstDepreciationReversalListener`（F2.9 修复体）全仓零测试引用（unit 无 ReversalListener/onVoucherReversed 命中、orchestration E2E 反向冲销 spec 仅 p2p/o2c/mfg 三域）——引擎侧凭证红冲→域回退跨域场景（ast2-005 同型 P1 级行为）无专属回归断言钉住（F2.9 修复注记亦未点名测试，对照 001~004 修复均带红→绿测试）；域内发起红冲路径已由 TestErpAstPostingReverse 覆盖，缺口限引擎派发通道；按业务关键度定 P2（清单缺口 P2 起；行为属 P1 级红冲闭环家族故接近 P1 上限）
      - Skill: none
- [x] <Proof> 快照纪律：`SnapshotTest.RECORDING` 提交态零残留 + `*` 通配/`delVersion` 屏蔽合规抽查——`SnapshotTest.RECORDING` 0 残留；`delVersion` 命中仅 1 处 javadoc 注解（TestErpAstExtFieldsAndAuditTrail L358）；decimal 列 `*` 通配零滥用；339 全绿 = CHECKING 态等价证明
      - Skill: none

Exit Criteria:

- [x] U06×T×ast-2 格 verdict 落盘；本地回归全绿数字在案——verdict = **finding**（1 新立 P2-CK-ast2-026-r3 覆盖缺口；关键业务流五行其余全覆盖）；339/0/0/0 在案
- [x] 覆盖对账 + 关键业务流清单核对 + 快照纪律结果在案——三项结果落注记

## Phase 6 — DIM-I i18n 零回归与白名单合规（MI 先行时序口径）

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/i18n-compliance.md` + `docs/audits/cjk-baseline.md`）
> Targets: 全域门控 + ast 白名单条目抽查
> Prereqs: Phase 1 完成

- [x] <Proof> 零回归门控：`node tools/check-hardcoded-cjk.mjs --strict`（exit 0）+ `--self-test` PASS；脚本红 = MI 回归升级报 MI 通道，不立 DIM-I finding——`--strict` **PASS exit 0**（0 new violations vs 冻结 SNAPSHOT，170 baseline files，CAT1..4=0/0/209/1318 单向收紧成立）；`--self-test` **PASS**（self-test green）；ast 探针族（CAT-1 39/CAT-2 29±）维持清零零回归
      - Skill: none
- [x] <Proof> 白名单合规抽查：§WHITELIST ast 域条目（MI.6 批 1/2 登记 4 文件）抽 ≥3 条核对四要素 + `grep -L "@Locale"` 全量 `*Errors.java` expect 空 + `git status --porcelain 'module-*/erp-*-meta/**'` expect 空；四要素缺失 = 白名单登记缺陷 finding——4 条全抽（≥3）：`IErpAstAssetBiz`（@Description L38 在位）/ `ErpAstDashboardBizModel`（@Description L56 在位）/ `ErpAstAssetBizModel`（@Description L93 在位 + audit 消息已英文化混合文件态属实）/ `ErpAstAssetSuspendResumeProcessor`（IDLE_SINCE_PREFIX="闲置自 " L35 在位 + TestErpAstIdleStateMachine L199 契约断言在位）——4/4 四要素（路径/理由/owner doc 指针 i18n-compliance.md 判定准绳表 #5/裁决来源 plan 2026-09-07-0902-1）齐备且与 HEAD 实态一致零缺陷；`grep -L @Locale` 全量 *Errors.java 输出空（全部含 @Locale，ErpAstErrors 在位）；`erp-*-meta` + `_vfs/i18n` 零手改
      - Skill: none

Exit Criteria:

- [x] U06×I×ast-2 格 verdict 落盘；`--strict`/`--self-test` PASS 在案——verdict = **pass**；双 PASS 在案
- [x] 白名单四要素抽查 ≥3 条 + `@Locale` + meta 禁手改核对结果在案——4/4 四要素 + @Locale 空 + meta 零手改在案

## Phase 7 — 三态裁决汇总 + ck 报告落盘 + 双索引同步 + 零改动核证

> 统一类型：Add-heavy（2 Add + 2 Proof + 1 Decision）。
> Skill: code-quality-audit-prompt
> Targets: `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-assets-depreciation-r3.md`（新增）+ `ai-check-r3-index.md` + `docs/audits/check/ai-check-index.md`
> Prereqs: Phase 2~6 完成（五格 verdict 齐备）

- [x] <Decision> 全切片候选 finding 汇总复裁决：三态一致性、级别（P0~P3）、ID 规范（`P{n}-CK-ast2-{NNN}-r3`，自 024 起）、归属标注（ast-1/fin-1/U20/U21 归并指针）复核——复裁决一致：新立 3（P2-CK-ast2-024-r3 CATCHUP 红冲闭环缺口 DIM-B / P3-CK-ast2-025-r3 owner doc 未同步簇 DIM-B⑮ / P2-CK-ast2-026-r3 ReversalListener 零测试引用 DIM-T），ID 序列 024/025/026 无冲突、历史 23 ast2 + 28 ast ID 零覆写；级别复核（024 定 P2 不升 P1 依据=触发面限 CATCHUP 凭果子集；026 定 P2=清单缺口 P2 起且行为属 P1 级家族接近上限；025 定 P3=纯文档零运行时影响）；归属标注复核（posting 引擎内部→fin-1、生命周期面→ast-1、基类行为→U20、聚合横切→U21 四指针落报告 §2.4）；与 M1.6/M1.7 先例一致性核对通过（跨切片扩员 2 条按 M1.7 先例措辞）
      - Skill: code-quality-audit-prompt
- [x] <Add> 落盘 `ck-assets-depreciation-r3.md`：五维覆盖矩阵 + finding 列表 + 统计 + 剩余风险声明——已落盘（`docs/audits/check/2026-09-06-1645-ai-check-r3/ck-assets-depreciation-r3.md`）：§1 覆盖矩阵 5/5（B=finding、F=pass、S=pass、T=finding、I=pass）+ §2 三态裁决（复用 8 / 归并 15+跨切片扩员 2 / 新立 3，逐条 T0 行号证据）+ §3 统计表（0 P0/0 P1/2 P2/1 P3 新立）+ §4 剩余风险四件套（已查/未深查/残留风险/successor 触发）+ 收官机械核证节
      - Skill: none
- [x] <Add> 双索引同步：本轮索引产物清单追加 + 跨轮索引 §报告清单 M1.9 行 + §Finding 追踪新立 ID 行——`ai-check-r3-index.md` 产物清单 M1.9 行已追加；`ai-check-index.md` §报告清单 M1.9 行（0/0/2/1 计数）+ §Finding 追踪 3 新 ID 行（ast2-024/025/026-r3，open，修复归 M2.x）+ §r3 轮复核注记 M1.9 块已追加；历史行零改动
      - Skill: none
- [x] <Proof> 零生产代码改动核证：`git status --porcelain` 过滤生产路径 expect 空——实跑：脏面仅 `?? docs/audits/check/2026-09-06-1645-ai-check-r3/ck-assets-depreciation-r3.md` + `?? docs/plans/2026-09-09-0232-3-m19-ast-depreciation-posting-five-dim-audit.md`（+双索引修改，均 docs 路径）；`module-*`/`app-erp-all` 生产路径零输出 ✅（roadmap 规则 6 核证通过）
      - Skill: none
- [x] <Proof> 收尾回归：`mvn test -pl module-assets/erp-ast-service` 复跑全绿——实跑 **339/0/0/0 全绿 BUILD SUCCESS**（exit 0；与本切片 Phase 5 首跑及 M1.8 参照三次一致）
      - Skill: none

Exit Criteria:

- [x] `ck-assets-depreciation-r3.md` 落盘且五维矩阵 5 格 verdict 完整——5/5 落盘（B=finding/F=pass/S=pass/T=finding/I=pass）
- [x] 双索引行追加在案；零生产代码改动核证通过；ast service 回归根绿——三项全过在案

## Draft Review Record

- dispatch review #review-2026-09-08-193051-mission-driver-2026-09-09-0232-3-m19-ast-depreciation-posting-five-dim-audit-1-7da821a2 to 2026-09-08-193051-mission-driver
- 2026-09-09：iteration 1，共识 accept #review-2026-09-08-193051-mission-driver-2026-09-09-0232-3-m19-ast-depreciation-posting-five-dim-audit-1-7da821a2（补 Closure Gates——按姊妹 M1.6/M1.7 同批先例为本只读审计计划定制门控；基线断言逐一实仓复验在盘——HEAD `7ad2e426b`、脏面=本批 3 计划、MI 终态行 4006/0/0/1 与 CAT 0/0/0/0 + 白名单 27 文件、M0.3 checker 全表 11 值、r1 ast2 23 finding（0 P0/6 P1/8 P2/9 P3）与跨轮索引 8 fixed/15 open、F2.9 修复 P1-CK-ast2-001 UNITS 显式失败索引在案、新 ID 自 024 起、owner doc 行数（351/244）、`TestErpSeedDataIntegrity`、auth 聚合路径、`ErpAstDepreciationReversalListener` 实仓在盘、ast service 339 参照、roadmap M1.9 行与规则 6/8 引用、双技能名、successor `ai-check-r3-compliance-baseline-raise` 在案名；M1.8 前置执行态与 roadmap 翻转归属披露诚实，不阻塞本切片；frontmatter `status: draft` → `active`）

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。本计划为只读审计（零生产代码改动），验证命令组即结果表面本身（Phase 1~7 所列），完整仓库验证不在此重复（MV.1 对照面 = `known-good-baselines.md` 2026-09-08 MI 终态行；roadmap 收官机制承载全仓回归与 done 翻转）。ledger 格式计数域仅含 Phase 勾选项，本节门控以普通条目记录核对结果。

- 范围内行为完成（Phase 1~7 全部执行项与退出标准 `[x]`；五格 verdict 完整——`ck-assets-depreciation-r3.md` 覆盖矩阵 5/5，缺一格不算完）
- 相关文档对齐（双索引同步在案——本轮 `ai-check-r3-index.md` 产物清单行 + 跨轮 `ai-check-index.md` §报告清单 M1.9 行与 §Finding 追踪新 ID 行；历史 ID 零覆写；零生产代码改动 = owner doc 零对齐义务）
- 已运行验证：Phase 1 双 checker 红线（compliance checker 对照 M0.3 快照行 + CJK report mode 对照 0/0/0/0 终态）+ Phase 4 `TestErpSeedDataIntegrity` 全绿 + Phase 5/7 `mvn test -pl module-assets/erp-ast-service` 两次全绿 + Phase 6 `--strict` PASS + `--self-test` PASS + Phase 7 零改动核证（`git status --porcelain` 过滤 `module-*`/`app-erp-all` 生产路径 expect 空）——各命令数字落 Phase 勾选注记；任一红线漂移 = 登记裁决后再闭包，不就地吞掉
- 无范围内项目降级为 deferred/follow-up（finding 的「归属 ast-1/fin-1/U20/U21」归并标注与修复归 M2.x 是审计产出分流，非范围降级）
- 独立草案审查已完成并记录（见 Draft Review Record，iteration 1）
- 文本一致性已验证：状态、阶段、门控和日志都一致（ledger 格式：frontmatter `status: active` 保持，完成态由全部勾选 + `## Verification` pass 线 + `## Closure` 回执派生；当日日志条目 `docs/logs/2026/09-09.md` 落盘）
- 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- 结束证据存在于文件中（Phase 勾选注记 + `ck-assets-depreciation-r3.md` + 双索引行 + `## Verification` pass 线 + `## Closure` 审计回执）

## Verification

- PASS Phase 1 执行目录就位：`docs/audits/check/2026-09-06-1645-ai-check-r3/` 幂等复用，索引与冻结清单在位，路径与本计划头部一致
- PASS Phase 1 时点/脏面披露：T0 = HEAD `a23975f2158145396af58bcc2ca734ea0f7cf1cc`（2026-09-09 04:58），脏面 = 仅本计划 untracked 文件，姊妹在制披露在案
- PASS Phase 1 compliance checker 红线：19 规则实跑 R1a=0/R1b=0/R1c=0/R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R4=0/R5=0/R6=2/R7=0/R8=0/R10=14/R11=0/R12a=71/R12b=66/R12c=42 —— 与 M0.3 快照行逐位一致零漂移
- PASS Phase 1 CJK report mode 红线：exit 0，CAT-1..4 = 0/0/0/0 —— 与 MI 终态行一致
- PASS Phase 4 `mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity`：Tests run 4, Failures 0, Errors 0, Skipped 0，BUILD SUCCESS
- PASS Phase 5 `mvn test -pl module-assets/erp-ast-service`：Tests run 339, Failures 0, Errors 0, Skipped 0，BUILD SUCCESS（= M1.8 参照 339 零回归）
- PASS Phase 6 `node tools/check-hardcoded-cjk.mjs --strict`：exit 0 PASS（0 new violations，170 baseline files）+ `--self-test` PASS
- PASS Phase 7 零生产代码改动核证：`git status --porcelain` 过滤 `module-*`/`app-erp-all` 生产路径 = 空（脏面全为 docs 审计产物面）
- PASS Phase 7 收尾回归 `mvn test -pl module-assets/erp-ast-service` 复跑：Tests run 339, Failures 0, Errors 0, Skipped 0，BUILD SUCCESS（两次全绿）
- PASS 五维覆盖矩阵 5/5 完整：B=finding / F=pass / S=pass / T=finding / I=pass（`ck-assets-depreciation-r3.md` §1）+ 双索引同步在案 + 历史 ID 零覆写
- pass test 2026-09-09-0532 exit=0

## Closure

- dispatch audit #audit-2026-09-09-0532-2026-09-09-0232-3-m19-ast-depreciation-posting-five-dim-audit-1-adaf9c66 to 2026-09-08-193051-mission-driver models={exec:zhipuai-coding-plan/glm-5.3-flash,aud:zhipuai-coding-plan/glm-5.3-flash}
- accepted #audit-2026-09-09-0532-2026-09-09-0232-3-m19-ast-depreciation-posting-five-dim-audit-1-adaf9c66：独立结束审计 ACCEPT——35/35 计数域全 `[x]`、五维矩阵 5/5（B=finding（新立 P2-CK-ast2-024-r3 + P3-CK-ast2-025-r3）/ F=pass / S=pass / T=finding（新立 P2-CK-ast2-026-r3）/ I=pass）、`ck-assets-depreciation-r3.md` + 双索引 + roadmap M1.9 行 + 当日日志对账一致、零生产代码改动核证通过；closure visit 实跑 `mvn test -pl module-assets/erp-ast-service` 339/0/0/0 全绿 BUILD SUCCESS（exit=0），机械 checker `plan-check.mjs --strict` 复核通过
