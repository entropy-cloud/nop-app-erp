package app.erp.inv.service.posting;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.PostingEvent;
import app.erp.inv.dao.entity.ErpInvCostAdjust;
import app.erp.inv.dao.entity.ErpInvLandedCost;
import app.erp.inv.dao.entity.ErpInvOwnershipTransfer;
import app.erp.inv.dao.entity.ErpInvOwnershipTransferLine;
import app.erp.inv.service.ErpInvConstants;
import io.nop.api.core.exceptions.NopException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * G1 dispatcher tryPost 失败悬挂测试（plan {@code 2026-07-31-0744-3-r2-14}，P1-MA4-021(b) 残差）。
 *
 * <p>库存域 3 个过账派发器（LandedCost/CostAdjustment/OwnershipTransfer）的过账 catch-swallow 路径此前零测试触发。
 * 用确定性桩诱导过账失败（LandedCost/CostAdjustment 经 Proxy 桩 {@link IErpFinVoucherBiz}.post 抛异常；
 * OwnershipTransfer 经子类桩 {@link InvPostingExecutor}.postEvent 抛异常），断言悬挂：
 * <ul>
 *   <li>LandedCost/CostAdjustment tryPost 返回 {@code null}（保持 posted=false）</li>
 *   <li>OwnershipTransfer dispatchIfApplicable 吞异常不抛出，转移单 posted 标志不被置 true（保持 DONE+posted=false）</li>
 * </ul>
 *
 * <p>范式对齐 R1.16 {@code TestDepreciationPostingFailureAlert}（无 Mockito，Proxy/子类桩直调）。注：{@code InvPosting}
 * 移动单悬挂已由 {@code TestErpInvPosting.testPostingFailureLeavesMoveDonePostedFalse} 覆盖，不在本计划范围。
 */
public class TestErpInvPostingDispatcherFailureHangs {

    /** Proxy 桩 IErpFinVoucherBiz：post 抛 NopException 模拟财务过账引擎宕机（其它方法返回默认值）。 */
    private static IErpFinVoucherBiz throwingVoucherBiz() {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("post".equals(method.getName())) {
                    throw new NopException("test.inv-posting-engine-down", null, true, true);
                }
                return defaultReturn(method.getReturnType());
            }
        };
        return (IErpFinVoucherBiz) Proxy.newProxyInstance(
                IErpFinVoucherBiz.class.getClassLoader(),
                new Class[]{IErpFinVoucherBiz.class}, handler);
    }

    private static Object defaultReturn(Class<?> type) {
        if (type == void.class || !type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0d;
        if (type == char.class) return '\0';
        return null;
    }

    @Test
    public void testLandedCostTryPostFailureReturnsNull() {
        LandedCostPostingDispatcher dispatcher = new LandedCostPostingDispatcher();
        dispatcher.voucherBiz = throwingVoucherBiz();

        ErpInvLandedCost landedCost = landedCostOf("LC-FAIL-001");

        Long voucherId = dispatcher.tryPost(landedCost, Collections.emptyList(), Collections.emptyList());

        assertNull(voucherId, "到岸成本过账失败应吞异常返回 null（保持 posted=false 悬挂）");
    }

    @Test
    public void testCostAdjustmentTryPostFailureReturnsNull() {
        CostAdjustmentPostingDispatcher dispatcher = new CostAdjustmentPostingDispatcher();
        dispatcher.voucherBiz = throwingVoucherBiz();

        ErpInvCostAdjust adjust = adjustOf("CA-FAIL-001");

        Long voucherId = dispatcher.tryPost(adjust, Collections.emptyList(), new BigDecimal("10"));

        assertNull(voucherId, "成本调整过账失败应吞异常返回 null（保持 posted=false 悬挂）");
    }

    @Test
    public void testOwnershipTransferFailureLeavesPostedFalse() {
        OwnershipTransferPostingDispatcher dispatcher = new OwnershipTransferPostingDispatcher();
        dispatcher.executor = new InvPostingExecutor() {
            @Override
            public Long postEvent(PostingEvent event) {
                throw new NopException("test.inv-posting-engine-down", null, true, true);
            }
        };

        ErpInvOwnershipTransfer transfer = transferOf("OT-FAIL-001");
        ErpInvOwnershipTransferLine line = new ErpInvOwnershipTransferLine();
        line.setTotalCost(new BigDecimal("100"));
        line.setMaterialId(6001L);

        dispatcher.dispatchIfApplicable(transfer, Collections.singletonList(line));

        assertFalse(Boolean.TRUE.equals(transfer.getPosted()),
                "所有权转移过账失败应吞异常保持 DONE+posted=false（不阻塞转移单终态）");
    }

    // ---------- helpers ----------

    private ErpInvLandedCost landedCostOf(String code) {
        ErpInvLandedCost head = new ErpInvLandedCost();
        head.setCode(code);
        head.setBusinessDate(LocalDate.of(2026, 7, 1));
        head.setCurrencyId(6751L);
        head.setExchangeRate(BigDecimal.ONE);
        head.setTotalCostAmount(new BigDecimal("50"));
        head.setAllocationMethod(ErpInvConstants.ALLOC_METHOD_BY_AMOUNT);
        return head;
    }

    private ErpInvCostAdjust adjustOf(String code) {
        ErpInvCostAdjust adjust = new ErpInvCostAdjust();
        adjust.setCode(code);
        adjust.setBusinessDate(LocalDate.of(2026, 7, 1));
        adjust.setCurrencyId(6751L);
        adjust.setAdjustType(ErpInvConstants.ADJUST_TYPE_LANDED_COST_SUPPLEMENT);
        return adjust;
    }

    private ErpInvOwnershipTransfer transferOf(String code) {
        ErpInvOwnershipTransfer transfer = new ErpInvOwnershipTransfer();
        transfer.setCode(code);
        transfer.setTransferType(ErpInvConstants.TRANSFER_TYPE_VMI_CONSUME);
        transfer.setBusinessDate(LocalDate.of(2026, 7, 1));
        transfer.setCurrencyId(6751L);
        transfer.setPartnerId(7001L);
        transfer.setWarehouseId(3751L);
        transfer.setDocStatus(ErpInvConstants.DOC_STATUS_DONE);
        return transfer;
    }
}
