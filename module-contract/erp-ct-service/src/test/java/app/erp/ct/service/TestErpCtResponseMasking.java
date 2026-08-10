package app.erp.ct.service;

import app.erp.common.service.MaskHelper;
import app.erp.contract.dao.entity.ErpCtConsumptionLine;
import app.erp.contract.dao.entity.ErpCtContract;
import app.erp.contract.dao.entity.ErpCtContractLine;
import app.erp.contract.dao.entity.ErpCtInvoicePlan;
import app.erp.contract.dao.entity.ErpCtRebateAccrual;
import app.erp.contract.dao.entity.ErpCtRebateAgreement;
import app.erp.contract.dao.entity.ErpCtRebateSettlement;
import app.erp.ct.service.entity.ErpCtConsumptionLineBizModel;
import app.erp.ct.service.entity.ErpCtContractBizModel;
import app.erp.ct.service.entity.ErpCtContractLineBizModel;
import app.erp.ct.service.entity.ErpCtInvoicePlanBizModel;
import app.erp.ct.service.entity.ErpCtRebateAccrualBizModel;
import app.erp.ct.service.entity.ErpCtRebateAgreementBizModel;
import app.erp.ct.service.entity.ErpCtRebateSettlementBizModel;
import io.nop.api.core.auth.IUserContext;
import io.nop.auth.core.login.UserContextImpl;
import io.nop.core.unittest.BaseTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * E3.1 后端响应层脱敏 contract 域单元测试（plan 2026-08-10-2059-2 Phase 3 Proof）。
 *
 * <p>覆盖 9 个合同金额字段（ErpCtContract.totalAmount / ContractLine.amount / InvoicePlan.amount /
 * ConsumptionLine.amount / RebateAgreement 2 / RebateAccrual 2 / RebateSettlement.totalRebateAmount）：
 * 授权角色（合同审批人/合同专员）见明文，非授权见 null，无上下文 fail-closed。
 */
public class TestErpCtResponseMasking extends BaseTestCase {

    private static final BigDecimal AMOUNT = new BigDecimal("99999.50");

    private final ErpCtContractBizModel contractBiz = new ErpCtContractBizModel();
    private final ErpCtContractLineBizModel lineBiz = new ErpCtContractLineBizModel();
    private final ErpCtInvoicePlanBizModel planBiz = new ErpCtInvoicePlanBizModel();
    private final ErpCtConsumptionLineBizModel consumptionBiz = new ErpCtConsumptionLineBizModel();
    private final ErpCtRebateAgreementBizModel rebateAgreementBiz = new ErpCtRebateAgreementBizModel();
    private final ErpCtRebateAccrualBizModel rebateAccrualBiz = new ErpCtRebateAccrualBizModel();
    private final ErpCtRebateSettlementBizModel rebateSettlementBiz = new ErpCtRebateSettlementBizModel();

    private IUserContext prevCtx;

    @BeforeEach
    void saveContext() {
        prevCtx = IUserContext.get();
    }

    @AfterEach
    void restoreContext() {
        IUserContext.set(prevCtx);
    }

    @Test
    public void contractAmountAuthorizedSeesPlaintext() {
        loginAs(MaskHelper.ROLE_CT_APPROVER);
        assertEquals(0, AMOUNT.compareTo(contractBiz.totalAmountMask(newContract())), "合同审批人见 totalAmount 明文");
        loginAs(MaskHelper.ROLE_CT_CLERK);
        assertEquals(0, AMOUNT.compareTo(contractBiz.totalAmountMask(newContract())), "合同专员见 totalAmount 明文");
    }

    @Test
    public void contractAmountUnauthorizedSeesNull() {
        loginAs("STAFF");
        assertNull(contractBiz.totalAmountMask(newContract()), "非授权 totalAmount = null");
        assertNull(lineBiz.amountMask(newContractLine()), "非授权 ContractLine.amount = null");
        assertNull(planBiz.amountMask(newInvoicePlan()), "非授权 InvoicePlan.amount = null");
        assertNull(consumptionBiz.amountMask(newConsumptionLine()), "非授权 ConsumptionLine.amount = null");
    }

    @Test
    public void rebateFieldsMasked() {
        loginAs(MaskHelper.ROLE_CT_APPROVER);
        assertEquals(0, AMOUNT.compareTo(
                rebateAgreementBiz.totalAccumulatedAmountMask(newRebateAgreement())), "授权 totalAccumulatedAmount 明文");
        assertEquals(0, AMOUNT.compareTo(
                rebateAgreementBiz.estimatedRebateAmountMask(newRebateAgreement())), "授权 estimatedRebateAmount 明文");
        assertEquals(0, AMOUNT.compareTo(
                rebateAccrualBiz.billAmountSourceMask(newRebateAccrual())), "授权 billAmountSource 明文");
        assertEquals(0, AMOUNT.compareTo(
                rebateAccrualBiz.accruedRebateMask(newRebateAccrual())), "授权 accruedRebate 明文");
        assertEquals(0, AMOUNT.compareTo(
                rebateSettlementBiz.totalRebateAmountMask(newRebateSettlement())), "授权 totalRebateAmount 明文");

        loginAs("STAFF");
        assertNull(rebateAgreementBiz.totalAccumulatedAmountMask(newRebateAgreement()), "非授权 totalAccumulatedAmount = null");
        assertNull(rebateAccrualBiz.billAmountSourceMask(newRebateAccrual()), "非授权 billAmountSource = null");
        assertNull(rebateSettlementBiz.totalRebateAmountMask(newRebateSettlement()), "非授权 totalRebateAmount = null");
    }

    @Test
    public void noContextFailClosed() {
        IUserContext.set(null);
        assertNull(contractBiz.totalAmountMask(newContract()), "无上下文 totalAmount = null（fail-closed）");
    }

    private void loginAs(String... roles) {
        UserContextImpl ctx = new UserContextImpl();
        ctx.setUserId("ct-mask-test");
        ctx.setUserName("ct-mask-test");
        ctx.setRoles(Set.of(roles));
        IUserContext.set(ctx);
    }

    private ErpCtContract newContract() {
        ErpCtContract c = new ErpCtContract();
        c.setTotalAmount(AMOUNT);
        return c;
    }

    private ErpCtContractLine newContractLine() {
        ErpCtContractLine l = new ErpCtContractLine();
        l.setAmount(AMOUNT);
        return l;
    }

    private ErpCtInvoicePlan newInvoicePlan() {
        ErpCtInvoicePlan p = new ErpCtInvoicePlan();
        p.setAmount(AMOUNT);
        return p;
    }

    private ErpCtConsumptionLine newConsumptionLine() {
        ErpCtConsumptionLine l = new ErpCtConsumptionLine();
        l.setAmount(AMOUNT);
        return l;
    }

    private ErpCtRebateAgreement newRebateAgreement() {
        ErpCtRebateAgreement a = new ErpCtRebateAgreement();
        a.setTotalAccumulatedAmount(AMOUNT);
        a.setEstimatedRebateAmount(AMOUNT);
        return a;
    }

    private ErpCtRebateAccrual newRebateAccrual() {
        ErpCtRebateAccrual a = new ErpCtRebateAccrual();
        a.setBillAmountSource(AMOUNT);
        a.setAccruedRebate(AMOUNT);
        return a;
    }

    private ErpCtRebateSettlement newRebateSettlement() {
        ErpCtRebateSettlement s = new ErpCtRebateSettlement();
        s.setTotalRebateAmount(AMOUNT);
        return s;
    }
}
