package app.erp.pur.service.posting;

import app.erp.fin.service.posting.VoucherReversedEvent;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.service.ErpPurConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * PurReversalListener.rollbackReceive 对称性测试（P1-MA2-051，计划 {@code 2026-07-30-0341-3-r1-17} Phase 3）。
 *
 * <p>验证财务侧红冲 PURCHASE_INPUT 凭证后，rollbackReceive 与 rollbackInvoice/Payment/Return 对齐：
 * posted=false + approveStatus APPROVED→REJECTED（修复前仅 posted=false 保留 APPROVED 的不对称悬挂）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestPurReversalListenerReceiveRollback extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    @Test
    public void testRollbackReceiveAlignsToRejectedLikeOthers() {
        String code = "PR-RL-001";
        ormTemplate.runInSession(session -> {
            seedReceivePostedApproved(code);
            return null;
        });
        ErpPurReceive before = findReceive(code);
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, before.getApproveStatus(), "前置：receive APPROVED");
        assertEquals(Boolean.TRUE, before.getPosted(), "前置：receive posted=true");

        ormTemplate.runInSession(session -> {
            PurReversalListener listener = new PurReversalListener();
            listener.daoProvider = daoProvider;
            VoucherReversedEvent event = new VoucherReversedEvent();
            event.setBusinessType("PURCHASE_INPUT");
            event.setBillHeadCode(code);
            listener.onVoucherReversed(event, CTX);
            return null;
        });

        ErpPurReceive after = findReceive(code);
        assertFalse(Boolean.TRUE.equals(after.getPosted()), "财务红冲后 posted 应回退为 false");
        assertEquals(ErpPurConstants.APPROVE_STATUS_REJECTED, after.getApproveStatus(),
                "rollbackReceive 应与 rollbackInvoice/Payment/Return 对齐：APPROVED→REJECTED（非保留 APPROVED）");
    }

    @Test
    public void testRollbackReceiveNoOpWhenNotPosted() {
        String code = "PR-RL-002";
        ormTemplate.runInSession(session -> {
            seedReceive(code, ErpPurConstants.APPROVE_STATUS_APPROVED, false);
            return null;
        });

        ormTemplate.runInSession(session -> {
            PurReversalListener listener = new PurReversalListener();
            listener.daoProvider = daoProvider;
            VoucherReversedEvent event = new VoucherReversedEvent();
            event.setBusinessType("PURCHASE_INPUT");
            event.setBillHeadCode(code);
            listener.onVoucherReversed(event, CTX);
            return null;
        });

        ErpPurReceive after = findReceive(code);
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, after.getApproveStatus(),
                "posted=false 时不应回退（早退守卫）");
    }

    // ---------- helpers ----------

    private ErpPurReceive findReceive(String code) {
        IEntityDao<ErpPurReceive> dao = daoProvider.daoFor(ErpPurReceive.class);
        return dao.findAllByQuery(new io.nop.api.core.beans.query.QueryBean() {{
            addFilter(io.nop.api.core.beans.FilterBeans.eq("code", code));
            setLimit(1);
        }}).stream().findFirst().orElse(null);
    }

    private void seedReceivePostedApproved(String code) {
        seedReceive(code, ErpPurConstants.APPROVE_STATUS_APPROVED, true);
    }

    private void seedReceive(String code, String approveStatus, boolean posted) {
        IEntityDao<ErpPurReceive> dao = daoProvider.daoFor(ErpPurReceive.class);
        ErpPurReceive receive = new ErpPurReceive();
        receive.setCode(code);
        receive.setSupplierId(990101L);
        receive.setWarehouseId(990201L);
        receive.setBusinessDate(LocalDate.of(2026, 7, 30));
        receive.setCurrencyId(990301L);
        receive.setDocStatus(ErpPurConstants.DOC_STATUS_ACTIVE);
        receive.setApproveStatus(approveStatus);
        receive.setPosted(posted);
        if (posted) {
            receive.setPostedAt(CoreMetrics.currentTimestamp());
            receive.setPostedBy("poster");
            receive.setApprovedBy("approver");
            receive.setApprovedAt(CoreMetrics.currentTimestamp());
        }
        dao.saveEntity(receive);
    }
}
