import { test, expect, loginAndNavigate, createViaSave, callMutationOk, callMutation, verifyState, deleteById } from './_helper';

/**
 * quality ErpQaAction（CAPA）业务动作浏览器层 E2E（plan 2026-07-09-2004-1 Phase 2）。
 *
 * 验证自定义 @BizMutation（startAction/completeAction/verifyAction）经 GraphQL /graphql 的全栈可达性 +
 * 最简 3 态状态机迁移 + 验证字段门控 + 非法迁移守卫。
 *
 * 权威状态机（ErpQaActionBizModel + ErpQaConstants + state-machine.md §NCR 与 CAPA 的关系）：ACTION 3 态
 *   PENDING --startAction--> IN_PROGRESS --completeAction--> COMPLETED
 *   verifyAction：在 COMPLETED 上填写 verificationPerson/verificationDate（效果验证），不改 status
 * 非法迁移抛 ERR_INVALID_ACTION_STATUS_TRANSITION / ERR_ACTION_VERIFY_REQUIRES_COMPLETED。
 *
 * 实现裁决：CAPA mandatory 字段经 ORM 核实为 ncrId+actionType+status。CAPA 须挂载 NCR（ncrId FK mandatory），
 * 故 spec 内先 __save 建一个最小 NCR（OPEN 态，mandatory code/ncrDate/materialId/severity/status）作为挂载点，
 * 再建 CAPA 引用其 id，验证后逐个清理（NCR + CAPA）。最简 3 态机证明范式在最小状态机下的可复用性。
 *
 * 种子引用（init-data）：material id=1（MAT-001 产品甲）；employee id=2（李四，作为 verificationPerson）。
 * 清理：CAPA 无下游产物；NCR 仅状态迁移无 resolve/过账（本 spec 不触发财务分派），逻辑删除两者。
 */

const MATERIAL_ID = 1;
const VERIFICATION_PERSON = 2;

async function seedNcr(page: import('@playwright/test').Page, tag: string): Promise<{ id: string }> {
  return createViaSave(
    page, 'ErpQaNonConformance',
    {
      code: `E2E-CAPA-NCR-${tag}-${Date.now()}`,
      ncrDate: '2026-07-09',
      materialId: MATERIAL_ID,
      severity: 'NORMAL',
      dispositionType: 'CONCESSION',
      status: 'OPEN',
      quantity: 1,
      description: `E2E CAPA mount NCR ${tag}`,
    },
    'id',
  );
}

test.describe('quality ErpQaAction (CAPA) state machine actions', () => {
  test('save(PENDING) → startAction(IN_PROGRESS) → completeAction(COMPLETED) → verifyAction + guard', async ({ page }) => {
    await loginAndNavigate(page, '/ErpQaAction-main');

    const ncr = await seedNcr(page, 'happy');

    const action = await createViaSave(
      page, 'ErpQaAction',
      {
        ncrId: Number(ncr.id),
        actionType: 'CAPA',
        description: 'E2E corrective action',
        responsiblePerson: VERIFICATION_PERSON,
        dueDate: '2026-12-31',
        status: 'PENDING',
      },
      'id status',
    );
    expect(action.id, '__save should create a PENDING action').toBeTruthy();
    expect(action.status, 'new action status=PENDING').toBe('PENDING');

    // startAction: PENDING → IN_PROGRESS
    const started = await callMutationOk(
      page, 'ErpQaAction', 'startAction', { actionId: action.id }, 'id status',
    );
    expect(started.status, 'startAction should transition PENDING → IN_PROGRESS').toBe('IN_PROGRESS');

    // completeAction: IN_PROGRESS → COMPLETED
    const completed = await callMutationOk(
      page, 'ErpQaAction', 'completeAction', { actionId: action.id }, 'id status',
    );
    expect(completed.status, 'completeAction should transition IN_PROGRESS → COMPLETED').toBe('COMPLETED');

    // verifyAction：在 COMPLETED 上填验证字段（不改 status，效果验证门控）
    const verified = await callMutationOk(
      page, 'ErpQaAction', 'verifyAction',
      { actionId: action.id, verificationPerson: VERIFICATION_PERSON, verificationDate: '2026-07-09' },
      'id status verificationPerson',
    );
    expect(verified.status, 'verifyAction keeps status COMPLETED').toBe('COMPLETED');
    expect(String(verified.verificationPerson), 'verifyAction records verificationPerson')
      .toBe(String(VERIFICATION_PERSON));

    const finalState = await verifyState(page, 'ErpQaAction', action.id, 'status verificationPerson');
    expect(finalState.status, '__get should confirm COMPLETED').toBe('COMPLETED');

    // 非法迁移守卫：completeAction 须 IN_PROGRESS，现 COMPLETED 再调应拒绝
    const rej = await callMutation(page, 'ErpQaAction', 'completeAction', { actionId: action.id }, 'id');
    expect(rej.errors, 'completeAction from COMPLETED should be rejected as illegal transition').toBeTruthy();

    // 清理：CAPA + 挂载 NCR
    await deleteById(page, 'ErpQaAction', action.id);
    await deleteById(page, 'ErpQaNonConformance', ncr.id);
  });
});
