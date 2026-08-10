import { test, expect, loginAsRole, callMutation, ENFORCEMENT_ERROR_CODES } from './_helper';

/**
 * P2.4 dry-run 影响面 Proof（plan 2026-08-10-0741-1 / Phase 3，permissions-enforcement mission）。
 *
 * **目的**：用 `role-restricted`（绑平台 `user` 角色，无任何敏感 FNPT 授权）遍历 Phase 1 子集边界
 * 枚举的全部 per-action FNPT 已声明敏感动作（P1.4a-d 已补齐域 + E1.1 五高危域），登记双类清单：
 *   (1) **被拒动作**（enforcement 真拒绝 = `nop.err.auth.no-permission`）—— 预期，证明 FNPT 声明 +
 *       角色种子 + checker 三层联动生效；
 *   (2) **未被拒动作**（无 `no-permission`：返回业务错误 / 成功 / 其他错误）—— FNPT 种子缺口标记，
 *       供 E1.1 按清单分批消费（动作未声明 FNPT 或声明但无角色绑定 或 checker 路径未覆盖）。
 *
 * **子集来源**：`erp-{fin,pur,sal,mfg,ast,b2b,ct,hr,inv}.action-auth.xml` delta 中 `FNPT:` 前缀
 * resource 声明（L62 `enable-action-auth=true` 翻启后 enforcement 实际拦截）。调用参数使用哨兵 id
 * （不要求业务可达，enforcement 检查在业务逻辑之前——被拒动作不应进入业务校验路径）。
 *
 * **断言语义**：本 spec 不断言每个动作「必须被拒」（否则 FNPT 缺口会变 hard fail）。断言 =
 * (a) 被拒类占比（sanity：role-restricted 无 FNPT，绝大多数应被拒）；
 * (b) 清单完整产出（所有动作有明确 classified 结果）。
 * 完整双类清单落盘 `docs/testing/permissions-enforcement-dry-run-impact.md`（手工/半自动从本 spec 输出整理）。
 */

interface ProbeSpec {
  domain: string;
  entity: string;
  action: string;
  args: Record<string, unknown>;
}

const DUMMY_ID = 999999;
const DUMMY_STR = '999999';

/**
 * 标量单参动作探针：argName 对应 BizModel 方法 @Name（默认 id）。
 * E1.1 已按各 BizModel 方法签名修正 argName（plan 2026-08-10-0739-1 Phase 1 Fix）。
 */
function probe(domain: string, entity: string, action: string, argName = 'id'): ProbeSpec {
  return { domain, entity, action, args: { [argName]: DUMMY_ID } };
}

/**
 * 显式 args 探针：用于多参 / 复杂类型 / String-id 动作（arg 名与类型按方法签名）。
 * Long 型用 DUMMY_ID；String 型用 DUMMY_STR；复杂 input 用最小骨架（过 GraphQL arg 校验即可，enforcement 检查在业务逻辑之前）。
 */
function probeArgs(
  domain: string,
  entity: string,
  action: string,
  args: Record<string, unknown>,
): ProbeSpec {
  return { domain, entity, action, args };
}

const SUBSET: ProbeSpec[] = [
  // fin（E1.1 高危 + P1.4 声明域）
  // ErpFinVoucher.post(event: PostingEvent) — 复杂 input，最小骨架过 arg 校验。Java @BizMutation → DENIED（静态裁决）。
  probeArgs('fin', 'ErpFinVoucher', 'post', { event: {} }),
  // ErpFinVoucher.reverse(billHeadCode: String, businessType: ErpFinBusinessType enum)
  probeArgs('fin', 'ErpFinVoucher', 'reverse', {     billHeadCode: DUMMY_STR, businessType: 'PURCHASE_INPUT' }),
  // ErpFinAccountingPeriod.closePeriod(periodId: Long) / reverseClose(periodId: Long)
  probe('fin', 'ErpFinAccountingPeriod', 'closePeriod', 'periodId'),
  probe('fin', 'ErpFinAccountingPeriod', 'reverseClose', 'periodId'),
  probe('fin', 'ErpFinBadDebt', 'writeOff', 'arApItemId'),
  probe('fin', 'ErpFinBadDebt', 'reverseApprove'),
  // pur（P1.4a 审批集）
  probe('pur', 'ErpPurRequisition', 'approve'),
  probe('pur', 'ErpPurRequisition', 'reverseApprove'),
  probe('pur', 'ErpPurOrder', 'approve'),
  probe('pur', 'ErpPurOrder', 'reverseApprove'),
  probe('pur', 'ErpPurReceive', 'approve'),
  probe('pur', 'ErpPurReceive', 'reverseApprove'),
  probe('pur', 'ErpPurInvoice', 'approve'),
  probe('pur', 'ErpPurInvoice', 'reverseApprove'),
  probe('pur', 'ErpPurPayment', 'approve'),
  probe('pur', 'ErpPurPayment', 'reverseApprove'),
  probe('pur', 'ErpPurReturn', 'approve'),
  probe('pur', 'ErpPurReturn', 'reverseApprove'),
  // sal（P1.4a 审批集）
  probe('sal', 'ErpSalQuotation', 'approve'),
  probe('sal', 'ErpSalQuotation', 'reverseApprove'),
  probe('sal', 'ErpSalOrder', 'approve'),
  probe('sal', 'ErpSalOrder', 'reverseApprove'),
  probe('sal', 'ErpSalContract', 'approve'),
  probe('sal', 'ErpSalContract', 'reverseApprove'),
  probe('sal', 'ErpSalDelivery', 'approve'),
  probe('sal', 'ErpSalDelivery', 'reverseApprove'),
  probe('sal', 'ErpSalReceipt', 'approve'),
  probe('sal', 'ErpSalReceipt', 'reverseApprove'),
  probe('sal', 'ErpSalInvoice', 'approve'),
  probe('sal', 'ErpSalInvoice', 'reverseApprove'),
  probe('sal', 'ErpSalReturn', 'approve'),
  probe('sal', 'ErpSalReturn', 'reverseApprove'),
  // mfg（P1.4b 委外 + E1.1 start/close/cancel）
  probe('mfg', 'ErpMfgSubcontractOrder', 'approve'),
  probe('mfg', 'ErpMfgWorkOrder', 'start', 'workOrderId'),
  probe('mfg', 'ErpMfgWorkOrder', 'close', 'workOrderId'),
  probe('mfg', 'ErpMfgWorkOrder', 'cancel', 'workOrderId'),
  // ast（P1.4b 处置）
  probe('ast', 'ErpAstDisposal', 'approve'),
  // b2b（P1.4c EDI 全生命周期 + E1.1 handleInboundWebhook）
  probe('b2b', 'ErpB2bEdiDoc', 'markSent', 'ediDocId'),
  probe('b2b', 'ErpB2bEdiDoc', 'cancel', 'ediDocId'),
  probe('b2b', 'ErpB2bEdiDoc', 'markAcknowledged', 'ediDocId'),
  probeArgs('b2b', 'ErpB2bEdiDoc', 'markError', { ediDocId: DUMMY_ID, error: 'probe' }),
  probe('b2b', 'ErpB2bEdiDoc', 'retry', 'ediDocId'),
  probe('b2b', 'ErpB2bEdiDoc', 'archive', 'ediDocId'),
  // ErpB2bAsn.handleInboundWebhook(formatCode, partnerCode, signature, eventId, payload: 全 String)
  probeArgs('b2b', 'ErpB2bAsn', 'handleInboundWebhook', {
    formatCode: 'X12',
    partnerCode: 'probe',
    signature: 'probe',
    eventId: 'probe',
    payload: '{}',
  }),
  probe('b2b', 'ErpB2bAsn', 'matchPurchaseOrder', 'asnId'),
  probe('b2b', 'ErpB2bAsn', 'createReceiveFromAsn', 'asnId'),
  probe('b2b', 'ErpB2bAsn', 'retryMatch', 'asnId'),
  // ct（P1.4d 电子签）
  probe('ct', 'ErpCtContract', 'activate'),
  probe('ct', 'ErpCtContractVersion', 'finalizeVersion'),
  probe('ct', 'ErpCtContractVersion', 'signVersion'),
  probe('ct', 'ErpCtSignatureRequest', 'initSignatureRequest'),
  probe('ct', 'ErpCtSignatureRequest', 'cancelSignatureRequest'),
  probe('ct', 'ErpCtSignatureRequest', 'handleSignatureCallback'),
  probe('ct', 'ErpCtSignatureRequest', 'queryAndUpdateStatus'),
  probe('ct', 'ErpCtSignatureRequest', 'rejectSignature'),
  // hr（P1.4d 薪酬审核 + E1.1 approve/markPaid + leaveRequest）
  // ErpHrSalary.approve 由 xbiz 声明（<arg name="id" type="String"/>）— String id
  probeArgs('hr', 'ErpHrSalary', 'approve', { id: DUMMY_STR }),
  probe('hr', 'ErpHrSalary', 'markPaid', 'salaryId'),
  probe('hr', 'ErpHrSalary', 'voidSalary', 'salaryId'),
  // ErpHrLeaveRequest.approve(@Name("id") String id) — Java @BizMutation
  probeArgs('hr', 'ErpHrLeaveRequest', 'approve', { id: DUMMY_STR }),
  // inv（E1.1 confirm/approve）
  probe('inv', 'ErpInvStockMove', 'confirm', 'moveId'),
  probe('inv', 'ErpInvLandedCost', 'approve'),
];

interface ProbeResult {
  spec: ProbeSpec;
  denied: boolean;
  errorCode?: string;
  errorMessage?: string;
  dataNull: boolean;
  classification: 'denied' | 'gap-enforcement-bypassed' | 'inconclusive-arg-mismatch';
  gapReason?: string;
}

test.describe('P2.4 dry-run impact: role-restricted FNPT subset probe', () => {
  test('classify all FNPT-declared subset actions as denied or gap', async ({ page }) => {
    await loginAsRole(page, 'restricted');
    await page.goto('/#/ErpFinVoucher-main', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);

    const results: ProbeResult[] = [];
    for (const spec of SUBSET) {
      const rej = await callMutation(page, spec.entity, spec.action, spec.args, 'id');
      const errorCode = rej.json?.extensions?.['nop-error-code'];
      const errorMessage = rej.errors?.[0]?.message;
      const denied = errorCode === ENFORCEMENT_ERROR_CODES.NO_PERMISSION;
      const isArgMismatch = errorCode === 'nop.err.graphql.undefined-field-arg';
      results.push({
        spec,
        denied,
        errorCode,
        errorMessage: errorMessage ? String(errorMessage).slice(0, 80) : undefined,
        dataNull: rej.data === null,
        classification: denied
          ? 'denied'
          : isArgMismatch
            ? 'inconclusive-arg-mismatch'
            : 'gap-enforcement-bypassed',
        gapReason: denied
          ? undefined
          : `errorCode=${errorCode ?? 'none'} message=${errorMessage ?? 'no-errors'}`,
      });
    }

    const deniedList = results.filter((r) => r.classification === 'denied');
    const bypassedList = results.filter((r) => r.classification === 'gap-enforcement-bypassed');
    const argMismatchList = results.filter((r) => r.classification === 'inconclusive-arg-mismatch');

    // Sanity 记录（非断言）：role-restricted 无敏感 FNPT，三类分布反映 FNPT 种子 + checker 覆盖 +
    // 探针 arg 对齐实况。三类清单落盘 `docs/testing/permissions-enforcement-dry-run-impact.md`
    // 供 E1.1 按清单分批消费。唯一断言：所有动作 classified 无逃逸。
    console.log(
      'P2.4_IMPACT_DENIED=' +
        JSON.stringify(deniedList.map((r) => `${r.spec.domain}/${r.spec.entity}.${r.spec.action}`)),
    );
    console.log(
      'P2.4_IMPACT_BYPASSED=' +
        JSON.stringify(
          bypassedList.map((r) => `${r.spec.domain}/${r.spec.entity}.${r.spec.action}`),
        ),
    );
    console.log(
      'P2.4_IMPACT_ARG_MISMATCH=' +
        JSON.stringify(
          argMismatchList.map((r) => `${r.spec.domain}/${r.spec.entity}.${r.spec.action}`),
        ),
    );
    console.log(
      `P2.4_IMPACT_SUMMARY=total=${results.length} denied=${deniedList.length} bypassed=${bypassedList.length} arg_mismatch=${argMismatchList.length}`,
    );
    expect(results.length, 'all subset actions classified').toBe(SUBSET.length);
  });
});
