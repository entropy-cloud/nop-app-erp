# 2026-09-02 ext-domains-child-table AMIS 遗留选择器 flux 下预存红灯

## 症状

`tests/e2e/visual/ext-domains-child-table.visual.spec.ts` 全部 5 个实体用例（logistics Shipment / b2b Asn / contract Contract / hr Timesheet / drp Plan）在 flux-only 渲染模式下稳定失败：`navigateAndWaitForCrud` 的 `page.waitForSelector('.cxd-Crud', 20s)` 超时；后续 `.cxd-InputTable` 断言同样不可达。

## 真因（非时序/环境/种子数据）

**AMIS 遗留 spec 的 `cxd-*` 选择器在 flux DOM 中不存在**。flux 渲染器输出的容器类名为 `nop-crud` / `nop-crud-table` / `nop-table`（2026-09-02 实测 DOM 探针：`/ErpHrTimesheet-main` 完整渲染后 `.cxd-Crud` 计数 = 0，`table` 类为 `nop-table`）。页面本身渲染正常（错误快照中行/分页齐全）——仅 spec 的 AMIS 时代选择器失配。

## 归因证据（M1.4b 批次 plan `2026-09-02-1415-1` Phase 3 排查留档）

1. **对照实验**：临时移出 M1.4b 批次 15 个 seed CSV → 重建 app-erp-all → fresh-DB 重启（`ErpLogShipment__findPage total=0` 证伪种子在库）→ 同 spec 同样 5/5 失败；恢复 CSV 后 5/5 失败不变 → 与种子数据零因果。
2. **零种子域同败**：b2b / contract / drp 三用例对应域零 seed（M1.5 未执行），列表空页同样失败——失败发生在「等 `.cxd-Crud` 容器」步，与行数无关。
3. **同路由旁证**：`ext-domains-list-filter.visual.spec.ts`（同 `/ErpLogShipment-main` 路由，文本 label 断言 + 固定等待）在含种子环境 82 用例批量中通过。
4. **runbook 已有定位**：`docs/testing/e2e-runbook.md`「渲染模式与 flux 调试三路径」节 L117「存量 AMIS 遗留 spec 的定位」——flux 唯一渲染目标后，`.cxd-*` 定位须经 `EngineAdapter` 翻译，禁止混写。本 spec 属未完成 adapter 化的存量。

## 修复方向

按 runbook L117 规则将该 spec 选择器迁移至 `EngineAdapter`（FluxAdapter 需暴露 crud 容器 / input-table 的 flux 等价定位，如 `.nop-crud` / `nop-table` 内行内编辑表格），spec 层保留业务语义断言（「编辑抽屉含 ≥1 子表」）。归 flux 迁移 / e2e-shared 基建 owner 域，非单业务批次可私改（涉及 `nop-chaos-next` `packages/e2e-shared/` 对齐时须遵守跨仓库协议）。

## 触发发现路径

M1.4b（aps + logistics seed 扩面）Phase 3.3 视觉快照双面义务核查——本批种子激活该 spec「真实行路径」的原预期未达成，实因为选择器层预存失配（先于本批已红，与种子无关）。
