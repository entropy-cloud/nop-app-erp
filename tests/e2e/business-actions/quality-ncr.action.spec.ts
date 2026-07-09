import { test, expect, loginAndNavigate, createViaSave, callMutationOk, callMutation, verifyState, deleteById } from './_helper';

/**
 * quality ErpQaNonConformance（NCR）业务动作浏览器层 E2E（plan 2026-07-09-2004-1 Phase 2）。
 *
 * 验证自定义 @BizMutation（submitReview/escalateToRecall/cancel）经 GraphQL /graphql 的全栈可达性 +
 * 5 态状态机无 CAPA 路径迁移 + 非法迁移守卫。
 *
 * 权威状态机（ErpQaNonConformanceBizModel + ErpQaConstants + state-machine.md §适用对象二）：NCR 5 态
 *   OPEN --submitReview--> IN_REVIEW --resolve(CAPA 闭环门控)--> RESOLVED（本 spec 不覆盖 resolve，Non-Goal）
 *   IN_REVIEW --escalateToRecall--> ESCALATED_TO_RECALL（终态，仅状态迁移占位）
 *   OPEN/IN_REVIEW --cancel--> CANCELLED（终态）
 * 非法迁移抛 ERR_INVALID_NCR_STATUS_TRANSITION。
 *
 * Non-Goal（plan 明示）：`resolve`（→RESOLVED）经 `NcrLifecycleService.requireResolveGate` 须全部 CAPA
 * COMPLETED+verified，需 CAPA 预置；`postNcr`/`reverseNcr`（SCRAP 过账）需 status=RESOLVED 前置——三者均依赖
 * RESOLVED 态，归 NCR resolve CAPA 闭环 successor。本 spec 仅覆盖无 CAPA 的状态迁移路径。
 *
 * 实现裁决：mandatory 字段经 ORM 核实为 code+ncrDate+materialId+severity+status（UK_QA_NON_CONFORMANCE_CODE
 * 单列唯一，code 须唯一——用 `E2E-NCR-<tag>-<ts>`）。dispositionType 选 RETURN（非 SCRAP，避免 resolve 自动过账
 * 路径相关歧义；本 spec 不 resolve 故 dispositionType 不影响，但显式选 RETURN 表明不触发 SCRAP 过账分派）。
 *
 * 种子引用（init-data）：material id=1（MAT-001 产品甲）。
 * 清理：NCR 状态机无 CAPA/过账产物（不 resolve），逻辑删除 NCR 自身。
 */

const MATERIAL_ID = 1;

async function seedNcr(page: import('@playwright/test').Page, tag: string): Promise<{ id: string }> {
  return createViaSave(
    page, 'ErpQaNonConformance',
    {
      code: `E2E-NCR-${tag}-${Date.now()}`,
      ncrDate: '2026-07-09',
      materialId: MATERIAL_ID,
      severity: 'NORMAL',
      dispositionType: 'RETURN',
      status: 'OPEN',
      quantity: 5,
      description: `E2E NCR ${tag}`,
    },
    'id status',
  );
}

test.describe('quality ErpQaNonConformance state machine actions (no-CAPA path)', () => {
  test('save(OPEN) → submitReview(IN_REVIEW) → cancel(CANCELLED)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpQaNonConformance-main');

    const ncr = await seedNcr(page, 'cnl');
    expect(ncr.status, 'new ncr status=OPEN').toBe('OPEN');

    // submitReview: OPEN → IN_REVIEW
    const reviewed = await callMutationOk(
      page, 'ErpQaNonConformance', 'submitReview', { ncrId: ncr.id }, 'id status',
    );
    expect(reviewed.status, 'submitReview should transition OPEN → IN_REVIEW').toBe('IN_REVIEW');

    // cancel: IN_REVIEW → CANCELLED
    const cancelled = await callMutationOk(
      page, 'ErpQaNonConformance', 'cancel', { ncrId: ncr.id }, 'id status',
    );
    expect(cancelled.status, 'cancel should transition IN_REVIEW → CANCELLED').toBe('CANCELLED');

    const verified = await verifyState(page, 'ErpQaNonConformance', ncr.id, 'status');
    expect(verified.status, '__get should confirm CANCELLED').toBe('CANCELLED');

    // 非法迁移守卫：CANCELLED → submitReview（须 OPEN），经 GraphQL 返回 errors
    const rej = await callMutation(page, 'ErpQaNonConformance', 'submitReview', { ncrId: ncr.id }, 'id');
    expect(rej.errors, 'submitReview from CANCELLED should be rejected as illegal transition').toBeTruthy();

    // 清理
    await deleteById(page, 'ErpQaNonConformance', ncr.id);
  });

  test('escalate path: save(OPEN) → submitReview(IN_REVIEW) → escalateToRecall(ESCALATED_TO_RECALL)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpQaNonConformance-main');

    const ncr = await seedNcr(page, 'esc');

    const reviewed = await callMutationOk(
      page, 'ErpQaNonConformance', 'submitReview', { ncrId: ncr.id }, 'id status',
    );
    expect(reviewed.status, 'submitReview should transition OPEN → IN_REVIEW').toBe('IN_REVIEW');

    // escalateToRecall: IN_REVIEW → ESCALATED_TO_RECALL（终态，仅状态迁移占位，不建召回实体）
    const escalated = await callMutationOk(
      page, 'ErpQaNonConformance', 'escalateToRecall', { ncrId: ncr.id }, 'id status',
    );
    expect(escalated.status, 'escalateToRecall should transition IN_REVIEW → ESCALATED_TO_RECALL')
      .toBe('ESCALATED_TO_RECALL');

    const verified = await verifyState(page, 'ErpQaNonConformance', ncr.id, 'status');
    expect(verified.status, '__get should confirm ESCALATED_TO_RECALL').toBe('ESCALATED_TO_RECALL');

    // 非法迁移守卫：ESCALATED_TO_RECALL（终态）→ cancel 应拒绝（cancel 仅 OPEN/IN_REVIEW）
    const rej = await callMutation(page, 'ErpQaNonConformance', 'cancel', { ncrId: ncr.id }, 'id');
    expect(rej.errors, 'cancel from ESCALATED_TO_RECALL (terminal) should be rejected').toBeTruthy();

    // 清理
    await deleteById(page, 'ErpQaNonConformance', ncr.id);
  });
});
