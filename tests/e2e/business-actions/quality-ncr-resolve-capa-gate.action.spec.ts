import { test, expect, loginAndNavigate, createViaSave, callMutationOk, callMutation, verifyState, deleteById, deleteByFilter, eqFilter } from './_helper';

/**
 * quality ErpQaNonConformance（NCR）resolve CAPA 闭包门控浏览器层 E2E（plan 2026-07-10-0335-2 Phase 1）。
 *
 * 验证 NCR→CAPA 闭包 resolve 门控作为跨实体依赖约束经浏览器层可观测（不止 CRUD 状态机）：
 *   - 负路径：CAPA 未闭包（仍 PENDING）时 resolve 抛 ERR_NCR_RESOLVE_CAPA_NOT_COMPLETED，status 不变（IN_REVIEW）
 *   - 正路径：全部 CAPA COMPLETED + verificationPerson/verificationDate 已填后 resolve 成功，status=RESOLVED
 *
 * 权威门控（ErpQaNonConformanceBizModel.resolve:82-101 → NcrLifecycleService.requireResolveGate:131-136）：
 * resolve 须 IN_REVIEW，且全部关联 ErpQaAction（CAPA）status=COMPLETED + verificationPerson+verificationDate 已填，
 * 任一未闭包抛 ERR_NCR_RESOLVE_CAPA_NOT_COMPLETED。无 CAPA 时允许 resolve（allActionsCompletedAndVerified 空集 true），
 * 故本 spec 显式预置一个 PENDING CAPA 以驱动门控负路径。
 *
 * 实现裁决（dispositionType=CONCESSION）：plan 原注「RETURN 避免自动过账」，但 RETURN 在
 * dispatchFinancialImpact 中触发 NcrReturnOrchestrator.orchestrateReturn（创建退货单副作用，属 Non-Goal 范围外），
 * SCRAP 触发 config-gated 自动过账。CONCESSION 在 dispatchFinancialImpact 中无分派（早返回），干净隔离 resolve
 * 门控本身（本 spec 目标=门控 + status=RESOLVED，过账/退货编排归 successor）。与 quality-capa spec 一致选用 CONCESSION。
 *
 * 种子引用（init-data）：material id=1（MAT-001）；employee id=2（李四，作 verificationPerson）。
 * 清理：CAPA 先于 NCR 删（FK 依赖 ncrId）。CONCESSION 无过账/退货产物，逻辑删除两者即可。
 */

const MATERIAL_ID = 1;
const VERIFICATION_PERSON = 2;

async function seedNcr(page: import('@playwright/test').Page, tag: string): Promise<{ id: string }> {
  return createViaSave(
    page, 'ErpQaNonConformance',
    {
      code: `E2E-NCR-GATE-${tag}-${Date.now()}`,
      ncrDate: '2026-07-10',
      materialId: MATERIAL_ID,
      severity: 'NORMAL',
      dispositionType: 'CONCESSION',
      status: 'OPEN',
      quantity: 3,
      description: `E2E NCR resolve gate ${tag}`,
    },
    'id status',
  );
}

test.describe('quality ErpQaNonConformance resolve CAPA closure gate', () => {
  test('resolve gated while CAPA pending, then succeeds after CAPA verified', async ({ page }) => {
    await loginAndNavigate(page, '/ErpQaNonConformance-main');

    const ncr = await seedNcr(page, 'gate');
    expect(ncr.status, 'new ncr status=OPEN').toBe('OPEN');

    // 预置一个 PENDING CAPA 挂载该 NCR（驱动门控负路径——无 CAPA 时门控放行）
    const capa = await createViaSave(
      page, 'ErpQaAction',
      {
        ncrId: Number(ncr.id),
        actionType: 'CAPA',
        description: 'E2E corrective action for resolve gate',
        responsiblePerson: VERIFICATION_PERSON,
        dueDate: '2026-12-31',
        status: 'PENDING',
      },
      'id status',
    );
    expect(capa.status, 'new capa status=PENDING').toBe('PENDING');

    // submitReview: OPEN → IN_REVIEW（resolve 前置态）
    const reviewed = await callMutationOk(
      page, 'ErpQaNonConformance', 'submitReview', { ncrId: ncr.id }, 'id status',
    );
    expect(reviewed.status, 'submitReview should transition OPEN → IN_REVIEW').toBe('IN_REVIEW');

    // 门控负路径：CAPA 仍 PENDING 未闭包 → resolve 须抛 ERR_NCR_RESOLVE_CAPA_NOT_COMPLETED
    const rej = await callMutation(
      page, 'ErpQaNonConformance', 'resolve',
      { ncrId: ncr.id, resolution: 'should be gated' }, 'id status',
    );
    expect(rej.errors, 'resolve with pending CAPA should be rejected by gate').toBeTruthy();
    // Nop GraphQL 在此配置下仅回传 i18n message（不序列化 extensions.errorCode），故断言该错误码
    // （ERR_NCR_RESOLVE_CAPA_NOT_COMPLETED）对应的标志性 message token——「CAPA」+「未完成」唯一区分于
    // 状态迁移类错误（illegal transition message 不含 CAPA 语义）。
    const rejBody = JSON.stringify(rej.errors);
    expect(rejBody, 'reject should carry CAPA-closure-gate message').toContain('CAPA');
    expect(rejBody, 'reject should indicate incomplete CAPA').toContain('未完成');

    // status 不变（未迁移）
    const stillReviewing = await verifyState(page, 'ErpQaNonConformance', ncr.id, 'status');
    expect(stillReviewing.status, 'rejected resolve should leave status IN_REVIEW').toBe('IN_REVIEW');

    // CAPA 闭包三步：startAction → completeAction → verifyAction（填 verificationPerson+verificationDate）
    const started = await callMutationOk(
      page, 'ErpQaAction', 'startAction', { actionId: capa.id }, 'id status',
    );
    expect(started.status, 'startAction should transition PENDING → IN_PROGRESS').toBe('IN_PROGRESS');

    const completed = await callMutationOk(
      page, 'ErpQaAction', 'completeAction', { actionId: capa.id }, 'id status',
    );
    expect(completed.status, 'completeAction should transition IN_PROGRESS → COMPLETED').toBe('COMPLETED');

    const verified = await callMutationOk(
      page, 'ErpQaAction', 'verifyAction',
      { actionId: capa.id, verificationPerson: VERIFICATION_PERSON, verificationDate: '2026-07-10' },
      'id status verificationPerson',
    );
    expect(verified.status, 'verifyAction keeps status COMPLETED').toBe('COMPLETED');

    const capaFinal = await verifyState(page, 'ErpQaAction', capa.id, 'status verificationPerson');
    expect(capaFinal.status, '__get should confirm CAPA COMPLETED').toBe('COMPLETED');

    // 门控正路径：全部 CAPA 闭包后 resolve 成功 → status=RESOLVED
    const resolved = await callMutationOk(
      page, 'ErpQaNonConformance', 'resolve',
      { ncrId: ncr.id, resolution: 'all CAPA closed' }, 'id status',
    );
    expect(resolved.status, 'resolve should transition IN_REVIEW → RESOLVED after CAPA closure').toBe('RESOLVED');

    const ncrFinal = await verifyState(page, 'ErpQaNonConformance', ncr.id, 'status');
    expect(ncrFinal.status, '__get should confirm RESOLVED').toBe('RESOLVED');

    // 清理：CAPA 先于 NCR 删（FK 依赖 ncrId）
    await deleteById(page, 'ErpQaAction', capa.id);
    await deleteById(page, 'ErpQaNonConformance', ncr.id);
  });
});
