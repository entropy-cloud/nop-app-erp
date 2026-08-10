package app.erp.mfg.service;

import app.erp.common.service.MaskHelper;
import app.erp.mfg.dao.entity.ErpMfgCostRollupLine;
import app.erp.mfg.service.costing.CostBandClassifier;
import app.erp.mfg.service.entity.ErpMfgCostRollupLineBizModel;
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
 * E3.1 masking + E4.1 字段级可见性 mfg 成本分解面单元测试。
 *
 * <p><b>E3.1（保持）</b>：totalCost/unitCost 聚合字段——授权角色（管理员/财务员）见明文，非授权见 null，
 * 无上下文 fail-closed。
 *
 * <p><b>E4.1（新增）</b>：materialCost/laborCost/overheadCost/subcontractCost 要素字段已 xmeta
 * published=false 隐藏，经代理视图暴露为 materialBand/laborBand/overheadBand/subcontractBand
 * high/mid/low 档位（档位映射对所有角色可见，精确值不可达）。
 *
 * <p>E3.2 取值豁免不变量复跑在独立守卫测试 {@code TestErpMfgCostRollupValueExemptionInvariant}
 * （反射断言 CostRollupService @Inject 不含 user-context 类型，published=false 不破坏服务端跨域取值）。
 */
public class TestErpMfgResponseMasking extends BaseTestCase {

    private static final BigDecimal COST = new BigDecimal("789.0123");

    private final ErpMfgCostRollupLineBizModel lineBiz = new ErpMfgCostRollupLineBizModel();

    private IUserContext prevCtx;

    @BeforeEach
    void saveContext() {
        prevCtx = IUserContext.get();
    }

    @AfterEach
    void restoreContext() {
        IUserContext.set(prevCtx);
    }

    // ---- E3.1 masking（totalCost/unitCost 聚合，授权可见）----

    @Test
    public void costAuthorizedRolesSeePlaintext() {
        loginAs(MaskHelper.ROLE_BIZ_ADMIN);
        assertEquals(0, COST.compareTo(lineBiz.totalCostMask(newLine())), "管理员见 totalCost 明文");
        assertEquals(0, COST.compareTo(lineBiz.unitCostMask(newLine())), "管理员见 unitCost 明文");
        loginAs(MaskHelper.ROLE_FINANCE_STAFF);
        assertEquals(0, COST.compareTo(lineBiz.totalCostMask(newLine())), "财务员见 totalCost 明文");
        assertEquals(0, COST.compareTo(lineBiz.unitCostMask(newLine())), "财务员见 unitCost 明文");
    }

    @Test
    public void costUnauthorizedSeesNull() {
        loginAs("STAFF");
        assertNull(lineBiz.totalCostMask(newLine()), "非授权 totalCost = null");
        assertNull(lineBiz.unitCostMask(newLine()), "非授权 unitCost = null");
    }

    @Test
    public void noContextFailClosed() {
        IUserContext.set(null);
        assertNull(lineBiz.totalCostMask(newLine()), "无上下文 totalCost = null（fail-closed）");
    }

    // ---- E4.1 代理视图：要素成本档位映射（对所有角色可见，精确值不可达）----

    @Test
    public void bandMappingLowMidHigh() {
        ErpMfgCostRollupLine lowLine = newLineWith(new BigDecimal("50"));
        ErpMfgCostRollupLine midLine = newLineWith(new BigDecimal("500"));
        ErpMfgCostRollupLine highLine = newLineWith(new BigDecimal("5000"));

        assertEquals(CostBandClassifier.LOW, lineBiz.materialBand(lowLine), "50 → low");
        assertEquals(CostBandClassifier.MID, lineBiz.laborBand(midLine), "500 → mid");
        assertEquals(CostBandClassifier.HIGH, lineBiz.overheadBand(highLine), "5000 → high");
        assertEquals(CostBandClassifier.HIGH, lineBiz.subcontractBand(highLine), "5000 → high");
    }

    @Test
    public void bandMappingNullReturnsNull() {
        ErpMfgCostRollupLine nullLine = new ErpMfgCostRollupLine();
        assertNull(lineBiz.materialBand(nullLine), "null 底层值 → null band");
        assertNull(lineBiz.laborBand(nullLine), "null 底层值 → null band");
        assertNull(lineBiz.overheadBand(nullLine), "null 底层值 → null band");
        assertNull(lineBiz.subcontractBand(nullLine), "null 底层值 → null band");
    }

    @Test
    public void bandBoundaryInclusive() {
        ErpMfgCostRollupLine boundary = newLineWith(new BigDecimal("100"));
        assertEquals(CostBandClassifier.MID, lineBiz.materialBand(boundary), "100 → mid（边界包含）");
        boundary.setMaterialCost(new BigDecimal("999.9999"));
        assertEquals(CostBandClassifier.MID, lineBiz.materialBand(boundary), "999.9999 → mid");
        boundary.setMaterialCost(new BigDecimal("1000"));
        assertEquals(CostBandClassifier.HIGH, lineBiz.materialBand(boundary), "1000 → high（边界包含）");
    }

    @Test
    public void bandVisibleToAllRoles() {
        loginAs("STAFF");
        assertEquals(CostBandClassifier.MID, lineBiz.materialBand(newLine()), "非授权角色仍见 band（coarse view）");
        IUserContext.set(null);
        assertEquals(CostBandClassifier.MID, lineBiz.materialBand(newLine()), "无上下文仍见 band（档位不角控）");
    }

    private void loginAs(String... roles) {
        UserContextImpl ctx = new UserContextImpl();
        ctx.setUserId("mfg-mask-test");
        ctx.setUserName("mfg-mask-test");
        ctx.setRoles(Set.of(roles));
        IUserContext.set(ctx);
    }

    private ErpMfgCostRollupLine newLine() {
        return newLineWith(COST);
    }

    private ErpMfgCostRollupLine newLineWith(BigDecimal cost) {
        ErpMfgCostRollupLine l = new ErpMfgCostRollupLine();
        l.setMaterialCost(cost);
        l.setLaborCost(cost);
        l.setOverheadCost(cost);
        l.setSubcontractCost(cost);
        l.setTotalCost(cost);
        l.setUnitCost(cost);
        return l;
    }
}
