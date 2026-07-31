package app.erp.inv.service.processor;

import app.erp.inv.dao.entity.ErpInvLandedCost;
import app.erp.inv.service.ErpInvConstants;
import app.erp.inv.service.posting.LandedCostPostingDispatcher;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G3 到岸成本反向悬挂测试（plan {@code 2026-07-31-0744-3-r2-14}，P1-MA4-020 测试可见性残差）。
 *
 * <p>R1.16 已落地 {@link ErpInvLandedCostProcessor#dispatchReverseFailureAlert}（reverse catch 吞异常告警），
 * 但零测试驱动 reverse-throws→posted=false+告警路径。本测试用确定性子类桩
 * {@code LandedCostPostingDispatcher.reverse}（抛 NopException 模拟 GL 红冲引擎宕机，避开「无原始凭证时
 * reverse 为平台幂等 no-op 不抛异常」的陷阱）驱动 {@link ErpInvLandedCostProcessor#doReverseApprove}，断言：
 * <ol>
 *   <li>posted=false（doReverseApprove step 3 无条件翻转，即使红冲凭证失败）</li>
 *   <li>{@code dispatchReverseFailureAlert} 触发，告警事件类型 = {@code inv.landed-cost-reverse-failure}</li>
 * </ol>
 *
 * <p>范式对齐 R1.16 {@code TestDepreciationPostingFailureAlert}（Proxy 桩 IErpSysNotificationBiz 捕获事件类型）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvLandedCostReverseFailureAlert extends JunitAutoTestCase {

    static final Long ORG_ID = 1851L;
    static final Long CURRENCY_ID = 6851L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    @Test
    public void testReverseFailureSetsPostedFalseAndDispatchesAlert() {
        String code = "LC-RFAIL-001";
        seedPostedApprovedLandedCost(code);

        ErpInvLandedCost before = findByCode(code);
        assertTrue(Boolean.TRUE.equals(before.getPosted()), "前置：landedCost posted=true");
        assertEquals(ErpInvConstants.APPROVE_STATUS_APPROVED, before.getApproveStatus(),
                "前置：landedCost APPROVED");

        ErpInvLandedCostProcessor processor = newLandedCostProcessorWithThrowingReverse();
        String[] captured = new String[1];
        processor.notificationBiz = recordingNotify(captured);

        ormTemplate.runInSession(session -> {
            processor.doReverseApprove(before, null, Collections.emptyList(), new ServiceContextImpl());
            return null;
        });

        ErpInvLandedCost after = findByCode(code);
        assertFalse(Boolean.TRUE.equals(after.getPosted()),
                "到岸成本红冲凭证失败仍应翻 posted=false（悬挂对运营可观测）");
        assertEquals(ErpInvConstants.APPROVE_STATUS_REJECTED, after.getApproveStatus(),
                "approveStatus APPROVED→REJECTED");
        assertEquals(ErpInvLandedCostProcessor.NOTIFY_EVENT_LANDED_COST_REVERSE_FAILURE, captured[0],
                "reverse 失败应派发 inv.landed-cost-reverse-failure 告警");
    }

    @Test
    public void testNullNotificationBizSkipsGracefully() {
        ErpInvLandedCostProcessor processor = newLandedCostProcessorWithThrowingReverse();
        processor.notificationBiz = null;

        ErpInvLandedCost lc = new ErpInvLandedCost();
        lc.setCode("LC-NULL-001");

        processor.dispatchReverseFailureAlert(lc, new NopException("test.reverse-down", null, true, true));
        assertTrue(true, "null notificationBiz 应静默跳过不抛异常");
    }

    // ---------- helpers ----------

    private ErpInvLandedCostProcessor newLandedCostProcessorWithThrowingReverse() {
        ErpInvLandedCostProcessor processor = new ErpInvLandedCostProcessor();
        processor.daoProvider = daoProvider;
        processor.ormTemplate = ormTemplate;
        processor.postingDispatcher = new LandedCostPostingDispatcher() {
            @Override
            public void reverse(ErpInvLandedCost landedCost) {
                throw new NopException("test.landed-cost-reverse-gl-down", null, true, true);
            }
        };
        return processor;
    }

    @SuppressWarnings("unchecked")
    private IErpSysNotificationBiz recordingNotify(String[] capturedEventType) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("notify".equals(method.getName()) && args.length == 3) {
                    capturedEventType[0] = (String) args[0];
                }
                return Collections.emptyList();
            }
        };
        return (IErpSysNotificationBiz) Proxy.newProxyInstance(
                IErpSysNotificationBiz.class.getClassLoader(),
                new Class[]{IErpSysNotificationBiz.class}, handler);
    }

    private void seedPostedApprovedLandedCost(String code) {
        ormTemplate.runInSession(session -> {
            IEntityDao<ErpInvLandedCost> dao = daoProvider.daoFor(ErpInvLandedCost.class);
            ErpInvLandedCost head = new ErpInvLandedCost();
            head.setCode(code);
            head.setOrgId(ORG_ID);
            head.setReceiveId(9001L);
            head.setSupplierId(7001L);
            head.setCurrencyId(CURRENCY_ID);
            head.setExchangeRate(BigDecimal.ONE);
            head.setTotalCostAmount(new BigDecimal("50"));
            head.setAllocationMethod(ErpInvConstants.ALLOC_METHOD_BY_AMOUNT);
            head.setDocStatus(ErpInvConstants.DOC_STATUS_DONE);
            head.setApproveStatus(ErpInvConstants.APPROVE_STATUS_APPROVED);
            head.setPosted(true);
            head.setBusinessDate(LocalDate.of(2026, 7, 1));
            dao.saveEntity(head);
            return null;
        });
    }

    private ErpInvLandedCost findByCode(String code) {
        IEntityDao<ErpInvLandedCost> dao = daoProvider.daoFor(ErpInvLandedCost.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }
}
