package app.erp.inv.service;

import app.erp.inv.service.costing.LandedCostAllocationEngine;
import app.erp.inv.service.costing.LandedCostAllocationEngine.AllocationResult;
import app.erp.inv.service.costing.LandedCostAllocationEngine.ReceiveLineInput;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 到岸成本分摊引擎纯单元测试（plan 2026-07-10-1100-3）。
 *
 * <p>验证三种分摊方法 + costing-methods.md:341-365 算例。
 * 不依赖 Spring/ORM，直接构造输入 DTO。
 */
public class TestErpInvLandedCostAllocationEngine {

    private final LandedCostAllocationEngine engine = new LandedCostAllocationEngine();

    /**
     * 算例（costing-methods.md:341-365）：
     * 入库行1：物料A，数量100，采购金额1000
     * 入库行2：物料B，数量50，采购金额500
     * 到岸成本合计：180
     * 按金额分摊：A=120, B=60
     * 新单位成本：A=1000/100+120/100=11.20, B=500/50+60/50=11.20
     */
    @Test
    public void testAllocateByAmount() {
        List<ReceiveLineInput> inputs = Arrays.asList(
                new ReceiveLineInput(1L, 101L, 301L, new BigDecimal("100"), new BigDecimal("1000"),
                        new BigDecimal("10"), null),
                new ReceiveLineInput(2L, 102L, 301L, new BigDecimal("50"), new BigDecimal("500"),
                        new BigDecimal("10"), null)
        );

        List<AllocationResult> results = engine.allocate(inputs, new BigDecimal("180"),
                ErpInvConstants.ALLOC_METHOD_BY_AMOUNT);

        assertEquals(2, results.size());
        AllocationResult a = results.get(0);
        AllocationResult b = results.get(1);

        assertEquals(0, a.getAllocatedAmount().compareTo(new BigDecimal("120.0000")),
                "物料A分摊=180×(1000/1500)=120");
        assertEquals(0, b.getAllocatedAmount().compareTo(new BigDecimal("60.0000")),
                "物料B分摊=180×(500/1500)=60");

        assertEquals(0, a.getNewUnitCost().compareTo(new BigDecimal("11.2000")),
                "物料A新单位成本=10+120/100=11.20");
        assertEquals(0, b.getNewUnitCost().compareTo(new BigDecimal("11.2000")),
                "物料B新单位成本=10+60/50=11.20");
    }

    /**
     * 按数量分摊：A=100, B=50, total=150
     * 180×(100/150)=120, 180×(50/150)=60
     */
    @Test
    public void testAllocateByQuantity() {
        List<ReceiveLineInput> inputs = Arrays.asList(
                new ReceiveLineInput(1L, 101L, 301L, new BigDecimal("100"), new BigDecimal("1000"),
                        new BigDecimal("10"), null),
                new ReceiveLineInput(2L, 102L, 301L, new BigDecimal("50"), new BigDecimal("500"),
                        new BigDecimal("10"), null)
        );

        List<AllocationResult> results = engine.allocate(inputs, new BigDecimal("180"),
                ErpInvConstants.ALLOC_METHOD_BY_QUANTITY);

        assertEquals(2, results.size());
        assertEquals(0, results.get(0).getAllocatedAmount().compareTo(new BigDecimal("120.0000")),
                "物料A按数量分摊=180×(100/150)=120");
        assertEquals(0, results.get(1).getAllocatedAmount().compareTo(new BigDecimal("60.0000")),
                "物料B按数量分摊=180×(50/150)=60");
    }

    /**
     * 按重量分摊：A=20kg, B=10kg, total=30kg
     * 180×(20/30)=120, 180×(10/30)=60
     */
    @Test
    public void testAllocateByWeight() {
        List<ReceiveLineInput> inputs = Arrays.asList(
                new ReceiveLineInput(1L, 101L, 301L, new BigDecimal("100"), new BigDecimal("1000"),
                        new BigDecimal("10"), new BigDecimal("20")),
                new ReceiveLineInput(2L, 102L, 301L, new BigDecimal("50"), new BigDecimal("500"),
                        new BigDecimal("10"), new BigDecimal("10"))
        );

        List<AllocationResult> results = engine.allocate(inputs, new BigDecimal("180"),
                ErpInvConstants.ALLOC_METHOD_BY_WEIGHT);

        assertEquals(2, results.size());
        assertEquals(0, results.get(0).getAllocatedAmount().compareTo(new BigDecimal("120.0000")),
                "物料A按重量分摊=180×(20/30)=120");
        assertEquals(0, results.get(1).getAllocatedAmount().compareTo(new BigDecimal("60.0000")),
                "物料B按重量分摊=180×(10/30)=60");
    }

    /**
     * 末行吸收舍入差，保证 Σ allocatedAmount == totalCostAmount。
     */
    @Test
    public void testRoundingRemainderAbsorbedByLastLine() {
        List<ReceiveLineInput> inputs = Arrays.asList(
                new ReceiveLineInput(1L, 101L, 301L, new BigDecimal("3"), new BigDecimal("100"),
                        new BigDecimal("10"), null),
                new ReceiveLineInput(2L, 102L, 301L, new BigDecimal("3"), new BigDecimal("100"),
                        new BigDecimal("10"), null),
                new ReceiveLineInput(3L, 103L, 301L, new BigDecimal("4"), new BigDecimal("100"),
                        new BigDecimal("10"), null)
        );

        BigDecimal totalCost = new BigDecimal("10");
        List<AllocationResult> results = engine.allocate(inputs, totalCost,
                ErpInvConstants.ALLOC_METHOD_BY_AMOUNT);

        BigDecimal sum = BigDecimal.ZERO;
        for (AllocationResult r : results) {
            sum = sum.add(r.getAllocatedAmount());
        }
        assertEquals(0, sum.compareTo(totalCost), "Σ 分摊金额 == 到岸成本合计（末行吸收舍入差）");
    }

    /**
     * 空入库行列表 → 拒绝。
     */
    @Test
    public void testEmptyReceiveLinesRejected() {
        assertThrows(Exception.class, () ->
                engine.allocate(java.util.Collections.emptyList(), new BigDecimal("180"),
                        ErpInvConstants.ALLOC_METHOD_BY_AMOUNT));
    }
}
