package app.erp.ct.service;

import app.erp.contract.dao.entity.ErpCtRebateAgreement;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdPartner;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P1-CK-ct-026-r3 返利协议 ACTIVE 前置悖论回归（plan 2026-09-10-1141-2 Phase 1）。
 *
 * <p>缺陷面：runAccrual 强制前置 status==ACTIVE（RunAccrualProcessor isActive 守卫），但全仓零
 * setStatus(ACTIVE) 命名动作 writer，激活路径不可达（计提→结算链入口断裂，唯一通道=裸 CRUD 旁路）。
 *
 * <p>修复裁决 = 补协议生效 mutation {@code activate}（DRAFT 源态守卫 + 生效日守卫），与
 * {@code ErpCtRebateAgreementStateMachine} 注册边对齐。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCtRebateActivation extends JunitAutoTestCase {

    @RegisterExtension
    static CtFrozenClockExtension frozenClock = new CtFrozenClockExtension();

    private static final String AS_OF = "2026-07-17";

    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    @Test
    public void testActivateDraftAgreementUnlocksRunAccrual() {
        String[] ids = setupPrereqs();
        String agreementId = createAgreement(ids[0], ids[1], "2026-01-01", "2027-12-31", "DRAFT");

        ApiResponse<?> acc = runAccrual(agreementId);
        assertTrue(acc.getStatus() != 0, "DRAFT 协议计提应拒绝");
        assertEquals(ErpCtErrors.ERR_CT_REBATE_AGREEMENT_NOT_ACTIVE.getErrorCode(), acc.getCode(),
                "DRAFT 计提拒绝码应为 ERR_CT_REBATE_AGREEMENT_NOT_ACTIVE");

        ApiResponse<?> act = activate(agreementId);
        assertEquals(0, act.getStatus(), "DRAFT 协议应可经命名动作激活（激活 writer 可达）: " + act);

        ErpCtRebateAgreement ag = daoProvider.daoFor(ErpCtRebateAgreement.class).getEntityById(agreementId);
        assertEquals(ErpCtConstants.REBATE_AGREEMENT_STATUS_ACTIVE, ag.getStatus(), "激活后 status=ACTIVE");

        ApiResponse<?> acc2 = runAccrual(agreementId);
        assertEquals(0, acc2.getStatus(), "激活后计提应放行: " + acc2);
    }

    @Test
    public void testActivateRejectsNonDraftSource() {
        String[] ids = setupPrereqs();
        String agreementId = createAgreement(ids[0], ids[1], "2026-01-01", "2027-12-31", "SETTLED");

        ApiResponse<?> act = activate(agreementId);
        assertTrue(act.getStatus() != 0, "非 DRAFT 源态激活应拒绝");
        assertEquals(ErpCtErrors.ERR_CT_REBATE_AGREEMENT_ILLEGAL_TRANSITION.getErrorCode(), act.getCode(),
                "非法激活拒绝码应为 ERR_CT_REBATE_AGREEMENT_ILLEGAL_TRANSITION");
    }

    @Test
    public void testActivateRejectsBeforeStartDate() {
        String[] ids = setupPrereqs();
        String agreementId = createAgreement(ids[0], ids[1], "2027-01-01", "2027-12-31", "DRAFT");

        ApiResponse<?> act = activate(agreementId);
        assertTrue(act.getStatus() != 0, "生效日未到的 DRAFT 协议激活应拒绝");
        assertEquals(ErpCtErrors.ERR_CT_REBATE_AGREEMENT_NOT_EFFECTIVE.getErrorCode(), act.getCode(),
                "生效日守卫拒绝码应为 ERR_CT_REBATE_AGREEMENT_NOT_EFFECTIVE");
    }

    // ---------- helpers ----------

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        io.nop.graphql.core.IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private ApiResponse<?> activate(String agreementId) {
        return executeRpc(GraphQLOperationType.mutation, "ErpCtRebateAgreement__activate",
                ApiRequest.build(Map.of("agreementId", agreementId)));
    }

    private ApiResponse<?> runAccrual(String agreementId) {
        return executeRpc(GraphQLOperationType.mutation, "ErpCtRebateAgreement__runAccrual",
                ApiRequest.build(Map.of("agreementId", agreementId, "asOfDate", AS_OF)));
    }

    private String[] setupPrereqs() {
        String partnerId = ormTemplate.runInSession(session -> {
            ErpMdPartner p = daoProvider.daoFor(ErpMdPartner.class).newEntity();
            p.setCode("CT-ACT-PARTNER-" + System.nanoTime());
            p.setName("激活测试伙伴");
            p.setPartnerType("CUSTOMER");
            p.setStatus("ACTIVE");
            daoProvider.daoFor(ErpMdPartner.class).saveEntity(p);
            return p.getId();
        });
        String currencyId = ormTemplate.runInSession(session -> {
            ErpMdCurrency c = daoProvider.daoFor(ErpMdCurrency.class).newEntity();
            c.setCode("CNY-ACT");
            c.setName("激活测试币种");
            daoProvider.daoFor(ErpMdCurrency.class).saveEntity(c);
            return c.getId();
        });
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", "CT-ACT-" + System.nanoTime());
        data.put("contractName", "激活测试合同");
        data.put("contractType", "PURCHASE");
        data.put("contractDirection", "INBOUND");
        data.put("partnerId", partnerId);
        data.put("currencyId", currencyId);
        data.put("startDate", "2026-01-01");
        data.put("endDate", "2027-12-31");
        data.put("status", "NEGOTIATION");
        Map<?, ?> r = (Map<?, ?>) executeRpc(GraphQLOperationType.mutation, "ErpCtContract__save",
                ApiRequest.build(Map.of("data", data))).getData();
        return new String[]{String.valueOf(r.get("id")), partnerId};
    }

    private String createAgreement(String contractId, String partnerId, String startDate, String endDate,
                                   String status) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", "REB-ACT-" + System.nanoTime());
        data.put("contractId", contractId);
        data.put("partnerId", partnerId);
        data.put("rebateType", "PURCHASE");
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("accrualMethod", "PERIOD_END");
        data.put("status", status);
        Map<?, ?> r = (Map<?, ?>) executeRpc(GraphQLOperationType.mutation, "ErpCtRebateAgreement__save",
                ApiRequest.build(Map.of("data", data))).getData();
        return String.valueOf(r.get("id"));
    }
}
