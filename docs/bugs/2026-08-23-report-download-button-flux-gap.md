# 2026-08-23 报表页下载按钮 button-toolbar / actionType:download flux 渲染缺口（10 AMIS 按钮用例预存红）

## 现象

全部带「下载 XLSX / 下载 PDF」按钮的报表页（page.yaml `type: button-toolbar` 容器 + `actionType: download` 按钮，共 24 报表页同构，E2E 抽样 5 报表 × 2 格式 = 10 用例）在 flux 渲染下**按钮组整体不渲染**：

- E2E `reports/_helper.ts:380`（AMIS download button regression，抽样 5 报表 × {xlsx,pdf}）全部 `getByRole('button', { name: /下载\s*XLSX|PDF/ })` 15s 超时——DOM 中无该按钮。

## 根因

nop-chaos-flux 不实现两个 AMIS 机制（grep 全 packages 源证零命中）：

1. **`type: button-toolbar` 容器渲染器**——flux-renderers-* 无该类型注册，节点整棵不渲染（两个下载按钮都在该容器下）；
2. **`actionType: download`**——flux-action-core `built-in-actions.ts` 的 case 列表（setValue/…/navigate）无 download 分支，`default: return undefined` 静默不支持。

flux 已具备底层能力（`flux-runtime/async-data/blob-download.ts` 支持 `responseType: blob` + content-disposition 文件名解析），缺的是这两层 schema/动作接线。

该缺口自报表页落地（2026-07-12-0413）即存在，非 id 迁移引入：2026-08-11 sweep reports 批次日志（`_tmp/e2e-results/reports.log`）已含同签名 10 失败，08-11 计划汇总漏记（同 `2026-08-23-ar-ap-aging-now-expression-flux.md` 的计数误差）。2026-08-23 M4.1 flux 全量回归复现并落本登记。

注：直接下载层（`reports/_helper.ts:278` 经 `page.request.post('/p/...')` + Authorization Bearer token）48 用例全绿——后端 `/p/{biz}__download` 契约与产物正确，缺口纯在前端渲染层。

## 影响

- flux 下报表页用户无下载按钮（功能缺口，非数据缺口）。
- E2E 10 用例红（预存，08-11 与 08-23 两口径均在）。

## 修复建议（successor）

跨仓库修复（nop-chaos-flux，按 e2e-runbook「渲染模式与 flux 调试三路径」+ 保护区域「外部仓库代码」流程）：

1. flux 补 `button-toolbar` 渲染器（横向按钮容器，可按 `hbox`/按钮组既有实现近似）；
2. flux-action-core 补 `download` actionType（复用 blob-download.ts：按 api 配置发起 blob 请求 + 触发文件保存）；
3. 修复后重建 flux 链（`scripts/rebuild-flux-chain.sh`）并重跑 `tests/e2e/reports/ --grep "AMIS download button"`。

## 复现

fresh-DB + flux：`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 E2E_ENGINE=flux npx playwright test tests/e2e/reports/ --grep "AMIS download button"`（10 用例确定性红，click 超时等待不存在的按钮）。

## 状态

open（登记日 2026-08-23，plan 2026-08-23-0434-2 Phase 2/3 裁决落盘；successor = cross-repo flux 能力补齐（与 08-11 白名单 crud 表单对话框 3 项 enforcement-induced UI 渲染回归同归 cross-repo flux successor 链））
