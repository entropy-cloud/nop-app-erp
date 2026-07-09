import { test, expect, loginAndNavigate, createViaSave, callMutationOk, verifyState, eqFilter, deleteByFilter, deleteById } from './_helper';

/**
 * CRM Lead 状态机浏览器层 E2E（plan 2026-07-09-0814-2 Phase 3）。
 *
 * 验证自定义 @BizMutation（qualify/moveStage/cancel）经 GraphQL /graphql 的全栈可达性 + docStatus 状态迁移。
 * 权威状态机（docs/design/crm/README.md）：NEW → QUALIFIED（入漏斗）/ NEW|QUALIFIED → CANCELLED；
 * moveStage 漏斗阶段流转（允许前移/回退，写 convLog 全量留痕——本 spec 不断言，归 Deferred）。
 *
 * 种子引用（init-data）：crm_stage id=1(验证,seq=10) / id=2(报价,seq=20)；org id=2。
 * 注：Lead 状态字段为 docStatus（非 leadStatusId——后者为线索子状态 FK）。
 */

test.describe('CRM Lead state machine actions', () => {
  test('save(NEW) → qualify(QUALIFIED) → moveStage(stageId flips) → cancel(CANCELLED)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpCrmLead-main');

    const code = `E2E-LEAD-${Date.now()}`;
    const lead = await createViaSave(
      page, 'ErpCrmLead',
      { code, leadType: 'OPPORTUNITY', docStatus: 'NEW', orgId: 2 },
      'id docStatus stageId',
    );
    expect(lead.id, '__save should create a NEW lead').toBeTruthy();
    expect(lead.docStatus, 'new lead docStatus=NEW').toBe('NEW');

    // qualify: NEW → QUALIFIED（入漏斗；stageId 为空时 doQualify 取首阶段 seq 升序 = id=1）
    const qualified = await callMutationOk(
      page, 'ErpCrmLead', 'qualify', { leadId: lead.id }, 'id docStatus stageId',
    );
    expect(qualified.docStatus, 'qualify should transition NEW → QUALIFIED').toBe('QUALIFIED');
    expect(qualified.stageId, 'qualify should set stageId (first stage)').toBeTruthy();

    // moveStage: stageId 1 → 2（允许前移/回退；仅断言 stageId 翻转，convLog 留痕归 Deferred）
    const moved = await callMutationOk(
      page, 'ErpCrmLead', 'moveStage',
      { leadId: lead.id, toStageId: 2 }, 'id stageId',
    );
    expect(String(moved.stageId), 'moveStage should flip stageId to target (2)').toBe('2');

    // cancel: QUALIFIED → CANCELLED
    const cancelled = await callMutationOk(
      page, 'ErpCrmLead', 'cancel', { leadId: lead.id }, 'id docStatus',
    );
    expect(cancelled.docStatus, 'cancel should transition QUALIFIED → CANCELLED').toBe('CANCELLED');

    const verified = await verifyState(page, 'ErpCrmLead', lead.id, 'docStatus');
    expect(verified.docStatus, '__get should confirm CANCELLED').toBe('CANCELLED');

    // 清理：moveStage 写 convLog 留痕 + lead 本身，避免污染 CRM 报表数值断言
    await deleteByFilter(page, 'ErpCrmLeadConvLog', eqFilter('leadId', Number(lead.id)));
    await deleteById(page, 'ErpCrmLead', lead.id);
  });

  test('cancel from NEW directly (NEW → CANCELLED)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpCrmLead-main');

    const lead = await createViaSave(
      page, 'ErpCrmLead',
      { code: `E2E-LEAD-CNL-${Date.now()}`, leadType: 'OPPORTUNITY', docStatus: 'NEW', orgId: 2 },
      'id docStatus',
    );

    const cancelled = await callMutationOk(
      page, 'ErpCrmLead', 'cancel', { leadId: lead.id }, 'id docStatus',
    );
    expect(cancelled.docStatus, 'cancel from NEW → CANCELLED').toBe('CANCELLED');

    await deleteById(page, 'ErpCrmLead', lead.id);
  });
});
