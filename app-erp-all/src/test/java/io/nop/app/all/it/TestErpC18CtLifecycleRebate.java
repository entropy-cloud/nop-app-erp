package io.nop.app.all.it;

import app.erp.contract.dao.entity.ErpCtApprovalRecord;
import app.erp.contract.dao.entity.ErpCtContract;
import app.erp.contract.dao.entity.ErpCtContractVersion;
import app.erp.contract.dao.entity.ErpCtRebateAccrual;
import app.erp.contract.dao.entity.ErpCtRebateAgreement;
import app.erp.contract.dao.entity.ErpCtRebateSettlement;
import app.erp.sal.dao.entity.ErpSalInvoice;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.autotest.NopTestProperty;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.context.ContextProvider;
import io.nop.dao.api.IDaoProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B9 C18：合同生命周期与返利计提结算（按 {@code docs/design/integration-testing.md §6 C18} 规格）。
 *
 * <p>全链（引用部署 seed：组织 2 / CUST-001 客户 1 / MAT-001 物料 1 / CNY 币种 1 / 2026-07 OPEN 期间）：
 * 自包含销售合同（SALES/OUTBOUND，DRAFT，totalAmount 1000 = 行 100×10）+ 行 + 返利协议（SALES /
 * PERIOD_END / ACTIVE，单阶梯 [0,∞) 2%）+ 阶梯 → 已过账销售发票链（{@code ErpSalInvoice__save}+行 →
 * submitForApproval → approve [DIRECT]，B2 C03 先例；100000 含税 0 税）→ 生命周期
 * {@code submit}（DRAFT→NEGOTIATION + 自动建 v1 DRAFT current）→ {@code finalizeVersion}（v1 FINALIZED）→
 * {@code activate}（NEGOTIATION→ACTIVE + v1 同步签署 SIGNED + signDate）→ {@code suspend}（ACTIVE→SUSPENDED）→
 * {@code resume}（SUSPENDED→ACTIVE）→ {@code amend}（ACTIVE→DRAFT + v2 DRAFT current 原子翻转）→
 * {@code rejectAmend}（DRAFT→ACTIVE + 恢复 v1 SIGNED current）→ {@code terminate}（两阶段：生成 PENDING 法务
 * 记录、合同状态不变，RC-R1.34）→ {@code approveTermination}（TERMINATED 终态）→
 * {@code ErpCtRebateAgreement__runAccrual}（PERIOD_END 聚合已过账 AR 发票 100000 × 2% = 2000）→
 * {@code ErpCtRebateSettlement__save}（DRAFT）→ {@code ErpCtRebateSettlement__postSettlement}
 * （POSTED + 跨域负额 credit memo（AR_INVOICE -2000，DRAFT）+ 计提行 isSettled=true）。
 *
 * <p>Phase 1 Decision（三项裁决，落盘设计文档 §6 C18 勘误）：
 * <ol>
 *   <li><b>动作宿主与生命周期序列</b>：返利计提/结算动作宿主 = {@code ErpCtRebateAgreement__runAccrual} /
 *       {@code ErpCtRebateSettlement__postSettlement}（设计文档 §6 写 ErpCtContract__runAccrual/postSettlement
 *       为宿主漂移勘误）；terminate 两阶段（terminate 不改状态返回合同实体，approveTermination 经 PENDING
 *       法务记录 id 达成 TERMINATED——记录 id 须按 {@code TestErpCtContractRebate.pendingTerminationRecordId}
 *       先例查询，设计单阶段假设勘误）；amend（ACTIVE→DRAFT）后 suspend 守卫要求 ACTIVE——设计文档
 *       「submit→activate→amend→suspend/resume/terminate」连续序列不可执行，实施期以
 *       {@code rejectAmend}（DRAFT→ACTIVE 恢复）补全 amend 回路后再 suspend/resume/terminate。</li>
 *   <li><b>计提金额口径</b>：PERIOD_END 聚合期间（协议 startDate..asOfDate）已过账 AR 发票
 *       totalAmountWithTax 只读求和，按累计金额命中阶梯整额 × rebatePercent（RebateEngine.computeRebate，
 *       scale=2 HALF_UP）——「条款 × 销售额」以实仓 Calculator 为准；{@code ErpCtConfigs} 的
 *       erp-ct.rebate-enabled/auto-settle/accrual-method 四键在 runAccrual/postSettlement 路径零消费
 *       （声明未接线），运行门控 = 协议实体 status=ACTIVE，无需 @NopTestProperty。</li>
 *   <li><b>结算 credit memo 口径</b>：postSettlement 生成跨域负额 AR 发票（{@code CT-REBATE-{settlementId}}，
 *       totalAmount=-2000、DRAFT/UNSUBMITTED，O-4 架构豁免直接持久化不经审批管道）——非已过账凭证；
 *       计提翻转 = 未结算计提行标记 isSettled=true + settledDate（无红字反向分录）——设计文档
 *       「负额发票/凭证生成 + 计提行翻转红字同向取负」以实仓行为为准勘误。</li>
 * </ol>
 *
 * <p>冻结时钟 {@code B9FrozenClockExtension}（2026-07-17）：ct 域 signDate/versionDate/accrualDate 为
 * 非 clock-tag 字面 CoreMetrics.today() 落库列（跨日漂移），计提 PERIOD-{today} 来源单号同源——按
 * C15C16C17 先例冻结取确定值。
 *
 * <p>三层验证：层 1 = JUnit 关键断言（生命周期状态机翻转 ACTIVE→SUSPENDED→ACTIVE→TERMINATED + 版本
 * isCurrent 原子翻转 + 计提金额 = 条款 × 销售额 + 结算 POSTED/负额 credit memo/计提行翻转）；层 2 = 每步
 * response 快照；层 3 = output/tables 变更行（contract + version + approval_record + rebate 族 + sal_invoice
 * 族）。RECORDING→CHECKING 往返按 M0.2 试点范式完成。
 */
@NopTestConfig(localDb = false,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.orm.init-database-data", value = "true")
public class TestErpC18CtLifecycleRebate extends ErpIntegrationTestCase {

    static final String CUSTOMER_ID = "1";     // seed CUST-001 华东科技（ACTIVE）
    static final String MATERIAL_ID = "1";     // seed MAT-001（uoMId 1）
    static final String INVOICE_CODE = "IT-C18-SINV-001";

    static final BigDecimal SALES_WITH_TAX = new BigDecimal("100000"); // 已过账销售额（含税 0 税）
    static final BigDecimal REBATE_PERCENT = new BigDecimal("2");
    static final BigDecimal EXPECTED_REBATE = new BigDecimal("2000.00"); // 100000 × 2%

    @RegisterExtension
    static B9FrozenClockExtension frozenClock = new B9FrozenClockExtension();

    @Inject
    IDaoProvider daoProvider;

    @Test
    public void testCtLifecycleRebateClosedLoop() {
        // ---------- 1. 自包含合同（DRAFT）+ 行（100×10=1000，totalAmount 对齐 submit 校验） ----------
        ApiResponse<?> contractSave = rpcMutation("ErpCtContract__save", request("1_contract_save.json5", Map.class));
        output("1_contract_save_response.json5", contractSave);
        assertEquals(0, contractSave.getStatus(), "合同保存应成功");
        String contractId = idOf(contractSave);
        addVar("contractId", contractId);
        assertEquals("DRAFT", reloadContract(contractId).getStatus(), "初始 status=DRAFT");

        ApiResponse<?> lineSave = rpcMutation("ErpCtContractLine__save", request("2_contract_line_save.json5", Map.class));
        output("2_contract_line_response.json5", lineSave);
        assertEquals(0, lineSave.getStatus(), "合同行保存应成功");

        // ---------- 2. 返利协议（SALES/PERIOD_END/ACTIVE）+ 单阶梯 [0,∞) 2%（前置返利条款） ----------
        ApiResponse<?> agreementSave = rpcMutation("ErpCtRebateAgreement__save",
                request("3_rebate_agreement_save.json5", Map.class));
        output("3_rebate_agreement_save_response.json5", agreementSave);
        assertEquals(0, agreementSave.getStatus(), "返利协议保存应成功");
        String agreementId = idOf(agreementSave);
        addVar("agreementId", agreementId);
        assertEquals("ACTIVE", reload(ErpCtRebateAgreement.class, agreementId).getStatus(), "协议初始 ACTIVE");

        ApiResponse<?> tierSave = rpcMutation("ErpCtRebateTier__save", request("4_rebate_tier_save.json5", Map.class));
        output("4_rebate_tier_save_response.json5", tierSave);
        assertEquals(0, tierSave.getStatus(), "返利阶梯保存应成功");

        // ---------- 3. 已过账销售发票链（B2 C03 先例：save+行 → submit → approve，posted=true） ----------
        ApiResponse<?> invoiceSave = rpcMutation("ErpSalInvoice__save", request("5_invoice_save.json5", Map.class));
        output("5_invoice_save_response.json5", invoiceSave);
        assertEquals(0, invoiceSave.getStatus(), "销售发票保存应成功");
        String invoiceId = idOf(invoiceSave);
        addVar("invoiceId", invoiceId);

        ApiResponse<?> invoiceLine = rpcMutation("ErpSalInvoiceLine__save", request("6_invoice_line_save.json5", Map.class));
        output("6_invoice_line_save_response.json5", invoiceLine);
        assertEquals(0, invoiceLine.getStatus(), "销售发票行保存应成功");

        ApiResponse<?> invoiceSubmit = rpcMutation("ErpSalInvoice__submitForApproval",
                request("7_invoice_submit.json5", Map.class));
        output("7_invoice_submit_response.json5", invoiceSubmit);
        assertEquals(0, invoiceSubmit.getStatus(), "销售发票提交审批应成功");

        ApiResponse<?> invoiceApprove = rpcMutation("ErpSalInvoice__approve", request("8_invoice_approve.json5", Map.class));
        output("8_invoice_approve_response.json5", invoiceApprove);
        assertEquals(0, invoiceApprove.getStatus(), "销售发票审批应成功");
        ErpSalInvoice approvedInvoice = reload(ErpSalInvoice.class, invoiceId);
        assertEquals("APPROVED", approvedInvoice.getApproveStatus(), "发票 approveStatus=APPROVED");
        assertEquals(Boolean.TRUE, approvedInvoice.getPosted(), "发票审核后 posted=true（runAccrual 聚合前置）");

        // ---------- 4. 生命周期：submit（DRAFT→NEGOTIATION + 自动 v1 DRAFT current） ----------
        ApiResponse<?> submit = rpcMutation("ErpCtContract__submit", request("9_contract_submit.json5", Map.class));
        output("9_contract_submit_response.json5", submit);
        assertEquals(0, submit.getStatus(), "submit 应成功");
        assertEquals("NEGOTIATION", reloadContract(contractId).getStatus(), "submit 后 NEGOTIATION");
        ErpCtContractVersion v1 = currentVersion(contractId);
        assertNotNull(v1, "submit 零版本自动建 v1（isCurrent=true）");
        assertEquals(1, v1.getVersionNo(), "v1 versionNo=1");
        assertEquals("DRAFT", v1.getStatus(), "v1 初始 DRAFT");
        addVar("versionId", v1.getId());

        // v1 定稿（FINALIZED）→ activate 同步签署（SIGNED）
        ApiResponse<?> finalize = rpcMutation("ErpCtContractVersion__finalizeVersion",
                request("10_version_finalize.json5", Map.class));
        output("10_version_finalize_response.json5", finalize);
        assertEquals(0, finalize.getStatus(), "finalizeVersion 应成功");
        assertEquals("FINALIZED", reload(ErpCtContractVersion.class, v1.getId()).getStatus(), "v1 FINALIZED");

        // ---------- 5. activate（NEGOTIATION→ACTIVE + v1 SIGNED） ----------
        ApiResponse<?> activate = rpcMutation("ErpCtContract__activate", request("11_contract_activate.json5", Map.class));
        output("11_contract_activate_response.json5", activate);
        assertEquals(0, activate.getStatus(), "activate 应成功");
        ErpCtContract active = reloadContract(contractId);
        assertEquals("ACTIVE", active.getStatus(), "activate 后 ACTIVE");
        assertEquals("SIGNED", reload(ErpCtContractVersion.class, v1.getId()).getStatus(), "activate 同步签署 v1 SIGNED");
        assertNotNull(active.getSignDate(), "activate 写签署日期");

        // ---------- 6. suspend（ACTIVE→SUSPENDED）→ resume（SUSPENDED→ACTIVE） ----------
        ApiResponse<?> suspend = rpcMutation("ErpCtContract__suspend", request("12_contract_suspend.json5", Map.class));
        output("12_contract_suspend_response.json5", suspend);
        assertEquals(0, suspend.getStatus(), "suspend 应成功");
        assertEquals("SUSPENDED", reloadContract(contractId).getStatus(), "suspend 后 SUSPENDED");

        ApiResponse<?> resume = rpcMutation("ErpCtContract__resume", request("13_contract_resume.json5", Map.class));
        output("13_contract_resume_response.json5", resume);
        assertEquals(0, resume.getStatus(), "resume 应成功");
        assertEquals("ACTIVE", reloadContract(contractId).getStatus(), "resume 后 ACTIVE");

        // ---------- 7. amend（ACTIVE→DRAFT + v2 DRAFT current 原子翻转）→ rejectAmend（DRAFT→ACTIVE 恢复） ----------
        ApiResponse<?> amend = rpcMutation("ErpCtContract__amend", request("14_contract_amend.json5", Map.class));
        output("14_contract_amend_response.json5", amend);
        assertEquals(0, amend.getStatus(), "amend 应成功");
        assertEquals("DRAFT", reloadContract(contractId).getStatus(), "amend 后回 DRAFT");
        List<ErpCtContractVersion> versions = findVersions(contractId);
        assertEquals(2, versions.size(), "amend 后 2 个版本");
        ErpCtContractVersion v2 = versions.stream()
                .filter(v -> Integer.valueOf(2).equals(v.getVersionNo())).findFirst().orElse(null);
        assertNotNull(v2, "amend 新建 v2");
        assertEquals("DRAFT", v2.getStatus(), "v2 DRAFT");
        assertTrue(Boolean.TRUE.equals(v2.getIsCurrent()), "v2 isCurrent=true（原子翻转）");
        assertTrue(!Boolean.TRUE.equals(reload(ErpCtContractVersion.class, v1.getId()).getIsCurrent()),
                "v1 isCurrent=false");

        ApiResponse<?> rejectAmend = rpcMutation("ErpCtContract__rejectAmend",
                request("15_contract_reject_amend.json5", Map.class));
        output("15_contract_reject_amend_response.json5", rejectAmend);
        assertEquals(0, rejectAmend.getStatus(), "rejectAmend 应成功");
        assertEquals("ACTIVE", reloadContract(contractId).getStatus(), "rejectAmend 恢复 ACTIVE");
        assertTrue(Boolean.TRUE.equals(reload(ErpCtContractVersion.class, v1.getId()).getIsCurrent()),
                "恢复 v1（SIGNED versionNo 最大）为 current");

        // ---------- 8. terminate 两阶段（RC-R1.34：PENDING 法务记录，状态不变）→ approveTermination ----------
        ApiResponse<?> terminate = rpcMutation("ErpCtContract__terminate", request("16_contract_terminate.json5", Map.class));
        output("16_contract_terminate_response.json5", terminate);
        assertEquals(0, terminate.getStatus(), "terminate 应成功");
        assertEquals("ACTIVE", reloadContract(contractId).getStatus(), "terminate 发起不改合同状态（两阶段）");
        String recordId = pendingTerminationRecordId(contractId);
        assertNotNull(recordId, "terminate 应生成 PENDING 法务审批记录");
        addVar("recordId", recordId);

        // guardTerminationRecord 守卫：以 seed 法务审批人身份驱动（nop_auth_user id 19
        // role-ct-approver / role 合同审批人，seedRole 范式——approverId 与 seed 字面一致）
        ContextProvider.getOrCreateContext().setUserId("19");
        ContextProvider.getOrCreateContext().setUserName("role-ct-approver");
        ApiResponse<?> approveTermination = rpcMutation("ErpCtContract__approveTermination",
                request("17_contract_approve_termination.json5", Map.class));
        output("17_contract_approve_termination_response.json5", approveTermination);
        assertEquals(0, approveTermination.getStatus(), "approveTermination 应成功");
        assertEquals("TERMINATED", reloadContract(contractId).getStatus(), "approveTermination 后 TERMINATED 终态");
        assertEquals("APPROVED", daoProvider.daoFor(ErpCtApprovalRecord.class).getEntityById(recordId).getApprovalStatus(),
                "法务记录 APPROVED");
        ContextProvider.getOrCreateContext().setUserId("autotest");
        ContextProvider.getOrCreateContext().setUserName("autotest");

        // ---------- 9. 返利计提（PERIOD_END：聚合已过账 AR 发票 100000 × 2% = 2000） ----------
        ApiResponse<?> accrual = rpcMutation("ErpCtRebateAgreement__runAccrual",
                request("18_run_accrual.json5", Map.class));
        output("18_run_accrual_response.json5", accrual);
        assertEquals(0, accrual.getStatus(), "runAccrual 应成功");
        ErpCtRebateAgreement accrued = reload(ErpCtRebateAgreement.class, agreementId);
        assertEquals(0, SALES_WITH_TAX.compareTo(accrued.getTotalAccumulatedAmount()), "累计金额=100000");
        assertEquals(0, EXPECTED_REBATE.compareTo(accrued.getEstimatedRebateAmount()), "预估返利=2000（2% × 100000）");

        List<ErpCtRebateAccrual> accruals = findAccruals(agreementId);
        assertEquals(1, accruals.size(), "PERIOD_END 一次性计提 1 行");
        ErpCtRebateAccrual accrualRow = accruals.get(0);
        assertEquals(0, EXPECTED_REBATE.compareTo(accrualRow.getAccruedRebate()), "计提金额=条款 × 销售额=2000");
        assertEquals(Boolean.FALSE, accrualRow.getIsSettled(), "计提行初始未结算");

        // ---------- 10. 返利结算（DRAFT → postSettlement → POSTED + 负额 credit memo + 计提翻转） ----------
        ApiResponse<?> settlementSave = rpcMutation("ErpCtRebateSettlement__save",
                request("19_settlement_save.json5", Map.class));
        output("19_settlement_save_response.json5", settlementSave);
        assertEquals(0, settlementSave.getStatus(), "结算单保存应成功");
        String settlementId = idOf(settlementSave);
        addVar("settlementId", settlementId);
        assertEquals("DRAFT", reload(ErpCtRebateSettlement.class, settlementId).getStatus(), "结算单初始 DRAFT");

        ApiResponse<?> postSettlement = rpcMutation("ErpCtRebateSettlement__postSettlement",
                request("20_settlement_post.json5", Map.class));
        output("20_settlement_post_response.json5", postSettlement);
        assertEquals(0, postSettlement.getStatus(), "postSettlement 应成功");
        ErpCtRebateSettlement posted = reload(ErpCtRebateSettlement.class, settlementId);
        assertEquals("POSTED", posted.getStatus(), "结算单 POSTED");
        assertEquals(0, EXPECTED_REBATE.compareTo(posted.getTotalRebateAmount()), "结算总额=2000");
        assertEquals("AR_INVOICE", posted.getCreditMemoBillType(), "SALES 返利→AR 贷项");
        String creditMemoCode = posted.getCreditMemoBillCode();
        assertNotNull(creditMemoCode, "贷项单号非空");
        addVar("creditMemoCode", creditMemoCode);

        ErpSalInvoice creditMemo = findSalInvoiceByCode(creditMemoCode);
        assertNotNull(creditMemo, "跨域负额 credit memo（AR 发票）应已生成");
        assertEquals(0, EXPECTED_REBATE.negate().compareTo(creditMemo.getTotalAmount()), "credit memo 为负额 -2000");
        assertEquals(CUSTOMER_ID, String.valueOf(creditMemo.getCustomerId()), "credit memo 客户=协议伙伴");

        for (ErpCtRebateAccrual a : findAccruals(agreementId)) {
            assertEquals(Boolean.TRUE, a.getIsSettled(), "计提行翻转 isSettled=true");
        }
    }

    // ---------- helpers ----------

    private ErpCtContract reloadContract(String contractId) {
        return reload(ErpCtContract.class, contractId);
    }

    private List<ErpCtContractVersion> findVersions(String contractId) {
        return daoProvider.daoFor(ErpCtContractVersion.class).findAllByQuery(eqFilterQuery("contractId", contractId));
    }

    private ErpCtContractVersion currentVersion(String contractId) {
        QueryBean q = eqFilterQuery("contractId", contractId);
        q.addFilter(eq("isCurrent", true));
        List<ErpCtContractVersion> list = daoProvider.daoFor(ErpCtContractVersion.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    /** RC-R1.34 两段化：查 PENDING 法务记录 id（approvalMatrixId=null 判别，TestErpCtContractRebate 先例）。 */
    private String pendingTerminationRecordId(String contractId) {
        QueryBean q = eqFilterQuery("contractId", contractId);
        q.addFilter(eq("approvalMatrixId", null));
        List<ErpCtApprovalRecord> records = daoProvider.daoFor(ErpCtApprovalRecord.class).findAllByQuery(q);
        for (ErpCtApprovalRecord r : records) {
            if ("PENDING".equals(r.getApprovalStatus())) {
                return r.getId();
            }
        }
        return null;
    }

    private List<ErpCtRebateAccrual> findAccruals(String agreementId) {
        return daoProvider.daoFor(ErpCtRebateAccrual.class).findAllByQuery(eqFilterQuery("rebateAgreementId", agreementId));
    }

    private ErpSalInvoice findSalInvoiceByCode(String code) {
        List<ErpSalInvoice> list = daoProvider.daoFor(ErpSalInvoice.class).findAllByQuery(eqFilterQuery("code", code));
        return list.isEmpty() ? null : list.get(0);
    }
}
