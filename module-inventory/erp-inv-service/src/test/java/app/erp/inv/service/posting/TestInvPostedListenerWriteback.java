package app.erp.inv.service.posting;

import app.erp.fin.service.posting.VoucherPostedEvent;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.service.ErpInvConstants;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F2.1（P1-CK-fin-001）：InvReversalListener 正向过账回写单测（dual 接口 onVoucherPosted）。
 * 覆盖 inv 域新增分支：stock-move 直查（SALES_OUTPUT）与 PURCHASE_PRICE_VARIANCE 的 "-PPV" 后缀 strip。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestInvPostedListenerWriteback extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    @Test
    public void testStockMovePostedFlipsTrue() {
        String code = "SM-F21-POST-001";
        ormTemplate.runInSession(session -> {
            seedMove(code, false);
            return null;
        });

        ormTemplate.runInSession(session -> {
            InvReversalListener listener = new InvReversalListener();
            listener.daoProvider = daoProvider;
            VoucherPostedEvent event = new VoucherPostedEvent();
            event.setVoucherId("v-f21-inv-001");
            event.setBusinessType("SALES_OUTPUT");
            event.setBillHeadCode(code);
            listener.onVoucherPosted(event, CTX);
            session.flush();
            return null;
        });

        ErpInvStockMove after = findMove(code);
        assertEquals(Boolean.TRUE, after.getPosted(), "悬挂回写：移动单 posted false→true");
        assertNotNull(after.getPostedAt(), "postedAt 回写");
    }

    @Test
    public void testPpvSuffixStrippedToLocateMove() {
        String code = "SM-F21-PPV-002";
        ormTemplate.runInSession(session -> {
            seedMove(code, false);
            return null;
        });

        ormTemplate.runInSession(session -> {
            InvReversalListener listener = new InvReversalListener();
            listener.daoProvider = daoProvider;
            VoucherPostedEvent event = new VoucherPostedEvent();
            event.setBusinessType("PURCHASE_PRICE_VARIANCE");
            event.setBillHeadCode(code + "-PPV");
            listener.onVoucherPosted(event, CTX);
            session.flush();
            return null;
        });

        assertEquals(Boolean.TRUE, findMove(code).getPosted(),
                "PPV billHeadCode（move.code + \"-PPV\"）strip 后缀命中移动单回写 posted");
    }

    @Test
    public void testAlreadyPostedSkipsUpdate() {
        String code = "SM-F21-POST-003";
        ormTemplate.runInSession(session -> {
            seedMove(code, true);
            return null;
        });
        int versionBefore = findMove(code).getVersion();

        ormTemplate.runInSession(session -> {
            InvReversalListener listener = new InvReversalListener();
            listener.daoProvider = daoProvider;
            VoucherPostedEvent event = new VoucherPostedEvent();
            event.setBusinessType("PURCHASE_INPUT");
            event.setBillHeadCode(code);
            listener.onVoucherPosted(event, CTX);
            session.flush();
            return null;
        });

        ErpInvStockMove after = findMove(code);
        assertEquals(Boolean.TRUE, after.getPosted(), "已 posted 保持 true");
        assertEquals(versionBefore, after.getVersion(), "已 true 跳过：version 无谓递增被防住");
    }

    @Test
    public void testFindByCodeMissIsNoOp() {
        ormTemplate.runInSession(session -> {
            InvReversalListener listener = new InvReversalListener();
            listener.daoProvider = daoProvider;
            VoucherPostedEvent event = new VoucherPostedEvent();
            event.setBusinessType("SALES_OUTPUT");
            event.setBillHeadCode("NOT-EXIST-INV-F21");
            listener.onVoucherPosted(event, CTX);
            return null;
        });
        assertTrue(true, "findByCode miss no-op 不抛异常（purchase 域 PURCHASE_INPUT 单号在此 miss）");
    }

    // ---------- helpers ----------

    private ErpInvStockMove findMove(String code) {
        return ormTemplate.runInSession(session -> {
            IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
            io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
            q.addFilter(io.nop.api.core.beans.FilterBeans.eq("code", code));
            q.setLimit(1);
            java.util.List<ErpInvStockMove> list = dao.findAllByQuery(q);
            return list.isEmpty() ? null : list.get(0);
        });
    }

    private void seedMove(String code, boolean posted) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        ErpInvStockMove move = new ErpInvStockMove();
        move.setCode(code);
        move.setMoveType(ErpInvConstants.MOVE_TYPE_OUTGOING);
        move.setBusinessDate(LocalDate.of(2026, 8, 10));
        move.setDocStatus(ErpInvConstants.DOC_STATUS_DONE);
        move.setApproveStatus(ErpInvConstants.APPROVE_STATUS_APPROVED);
        move.setPosted(posted);
        if (posted) {
            move.setPostedAt(CoreMetrics.currentTimestamp());
            move.setPostedBy("poster");
        }
        dao.saveEntity(move);
    }
}
