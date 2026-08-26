package app.erp.pur.service;

import app.erp.common.service.ErpCommonErrors;
import app.erp.pur.dao.entity.ErpPurInvoice;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * F1.3（P1-CK-pur-003 主控制点）：AbstractErpCrudBizModel 状态锁五分支实体级证明。
 *
 * <p>posted=true 或 approveStatus=APPROVED 的发票经通用 {@code __update}/{@code __delete} 被拒；
 * DRAFT 放行；config 总开关关闭放行（kill-switch）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurCrudStatusLock extends JunitAutoTestCase {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @AfterEach
    void clearSwitch() {
        System.clearProperty("erp-common.crud-status-lock-enabled");
    }

    private String seedInvoice(String code, String approveStatus, boolean posted) {
        return ormTemplate.runInSession(sess -> {
            ErpPurInvoice invoice = new ErpPurInvoice();
            invoice.setCode(code);
            invoice.setOrgId("1101");
            invoice.setSupplierId("2101");
            invoice.setBusinessDate(LocalDate.of(2026, 7, 1));
            invoice.setCurrencyId("6101");
            invoice.setExchangeRate(BigDecimal.ONE);
            invoice.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
            invoice.setApproveStatus(approveStatus);
            invoice.setPaidStatus(ErpPurConstants.PAID_STATUS_UNPAID);
            invoice.setPaidAmount(BigDecimal.ZERO);
            invoice.setTotalAmount(new BigDecimal("100"));
            invoice.setTotalTaxAmount(new BigDecimal("13"));
            invoice.setTotalAmountWithTax(new BigDecimal("113"));
            invoice.setPosted(posted);
            daoProvider.daoFor(ErpPurInvoice.class).saveEntity(invoice);
            return invoice.getId();
        });
    }

    private ApiResponse<?> update(String id) {
        return executeRpc(mutation, "ErpPurInvoice__update",
                ApiRequest.build(Map.of("data", Map.of("id", id, "remark", "F1.3-改写"))));
    }

    private ApiResponse<?> delete(String id) {
        return executeRpc(mutation, "ErpPurInvoice__delete", ApiRequest.build(Map.of("id", id)));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType op, String action, ApiRequest<?> request) {
        return graphQLEngine.executeRpc(graphQLEngine.newRpcContext(op, action, request));
    }

    @Test
    public void testFiveBranchGuard() {
        // 分支 1：DRAFT（UNSUBMITTED）放行
        String draftId = seedInvoice("PI-LOCK-DRAFT", ErpPurConstants.APPROVE_STATUS_UNSUBMITTED, false);
        assertEquals(0, update(draftId).getStatus(), "DRAFT 单据通用 update 放行");

        // 分支 2：approveStatus=APPROVED（未过账）拒绝 update
        String approvedId = seedInvoice("PI-LOCK-APPR", ErpPurConstants.APPROVE_STATUS_APPROVED, false);
        ApiResponse<?> approvedResp = update(approvedId);
        assertNotEquals(0, approvedResp.getStatus(), "APPROVED 单据通用 update 应被拒");
        assertEquals(ErpCommonErrors.ERR_CRUD_STATUS_LOCKED.getErrorCode(), approvedResp.getCode(),
                "错误码 = CRUD 状态锁");

        // 分支 3+4：posted=true 拒绝 update 与 delete
        String postedId = seedInvoice("PI-LOCK-POSTED", ErpPurConstants.APPROVE_STATUS_APPROVED, true);
        assertNotEquals(0, update(postedId).getStatus(), "posted 单据通用 update 应被拒");
        ApiResponse<?> delResp = delete(postedId);
        assertNotEquals(0, delResp.getStatus(), "posted 单据通用 delete 应被拒");
        assertEquals(ErpCommonErrors.ERR_CRUD_STATUS_LOCKED.getErrorCode(), delResp.getCode());

        // 分支 5：kill-switch 关闭 → posted 单据 update 放行
        System.setProperty("erp-common.crud-status-lock-enabled", "false");
        assertEquals(0, update(postedId).getStatus(), "开关关闭时守卫放行（kill-switch）");
    }
}
