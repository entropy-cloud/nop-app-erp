package app.erp.mfg.service.processor;

import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import io.nop.api.core.exceptions.NopException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * F1.1（ai-check P0-CK-mfg-001）长度守卫单元测试：
 * 后续报工拼接键超过 relatedBillCode VARCHAR(50) 上限时显式抛
 * {@code ERR_COMPLETION_MOVE_BILL_CODE_TOO_LONG}（显式失败优于静默截断——计划 R3 裁决）。
 */
public class TestCompletionMoveBillCodeGuard {

    private static IErpInvStockMoveBiz existingMoveStub() {
        return (IErpInvStockMoveBiz) Proxy.newProxyInstance(
                IErpInvStockMoveBiz.class.getClassLoader(),
                new Class<?>[]{IErpInvStockMoveBiz.class},
                (proxy, method, args) -> "findByRelatedBill".equals(method.getName())
                        ? new ErpInvStockMove()
                        : null);
    }

    @Test
    public void testTooLongSuffixThrowsExplicitError() {
        ErpMfgWorkOrderProcessor processor = new ErpMfgWorkOrderProcessor();
        processor.stockMoveBiz = existingMoveStub();

        ErpMfgWorkOrder wo = new ErpMfgWorkOrder();
        wo.setCode("WO-2026-" + "X".repeat(40)); // 48 字符，留 2 余量确保溢出由数量部分触发
        wo.setCompletedQuantity(new BigDecimal("12345678901234567890.5000"));

        NopException ex = assertThrows(NopException.class,
                () -> processor.completionMoveBillCode(wo, null),
                "拼接键超过 50 字符应显式抛错（守卫断言）");
        assertEquals("erp.err.mfg.completion-move-bill-code-too-long", ex.getErrorCode(),
                "错误码应为完工移动关联单号超长守卫码");
    }

    @Test
    public void testShortSuffixKeepsSuffixedCode() {
        ErpMfgWorkOrderProcessor processor = new ErpMfgWorkOrderProcessor();
        processor.stockMoveBiz = existingMoveStub();

        ErpMfgWorkOrder wo = new ErpMfgWorkOrder();
        wo.setCode("WO-INCR");
        wo.setCompletedQuantity(new BigDecimal("5.0000"));

        assertEquals("WO-INCR-C5", processor.completionMoveBillCode(wo, null),
                "后续报工键 = wo.code + \"-C\" + 累计完工量规范化串（stripTrailingZeros）");
    }
}
