package app.erp.mfg.service;

import app.erp.common.service.MaskHelper;
import app.erp.mfg.dao.entity.ErpMfgCostRollupLine;
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
 * E3.1 后端响应层脱敏 mfg 成本分解面单元测试（plan 2026-08-10-2059-2 Phase 4 Proof）。
 *
 * <p>覆盖 ErpMfgCostRollupLine 6 成本字段（materialCost/laborCost/overheadCost/subcontractCost/
 * totalCost/unitCost）：授权角色（管理员/财务员）见明文，非授权见 null，无上下文 fail-closed。
 *
 * <p>E3.2 取值豁免不变量复跑在独立守卫测试 {@code TestErpMfgCostRollupValueExemptionInvariant}
 * （反射断言 CostRollupService @Inject 不含 user-context 类型，masking 不破坏服务端跨域取值）。
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

    @Test
    public void costAuthorizedRolesSeePlaintext() {
        loginAs(MaskHelper.ROLE_BIZ_ADMIN);
        assertPlaintext("管理员");
        loginAs(MaskHelper.ROLE_FINANCE_STAFF);
        assertPlaintext("财务员");
    }

    @Test
    public void costUnauthorizedSeesNull() {
        loginAs("STAFF");
        assertNull(lineBiz.materialCostMask(newLine()), "非授权 materialCost = null");
        assertNull(lineBiz.laborCostMask(newLine()), "非授权 laborCost = null");
        assertNull(lineBiz.overheadCostMask(newLine()), "非授权 overheadCost = null");
        assertNull(lineBiz.subcontractCostMask(newLine()), "非授权 subcontractCost = null");
        assertNull(lineBiz.totalCostMask(newLine()), "非授权 totalCost = null");
        assertNull(lineBiz.unitCostMask(newLine()), "非授权 unitCost = null");
    }

    @Test
    public void noContextFailClosed() {
        IUserContext.set(null);
        assertNull(lineBiz.totalCostMask(newLine()), "无上下文 totalCost = null（fail-closed）");
    }

    private void assertPlaintext(String label) {
        assertEquals(0, COST.compareTo(lineBiz.materialCostMask(newLine())), label + "见 materialCost 明文");
        assertEquals(0, COST.compareTo(lineBiz.laborCostMask(newLine())), label + "见 laborCost 明文");
        assertEquals(0, COST.compareTo(lineBiz.overheadCostMask(newLine())), label + "见 overheadCost 明文");
        assertEquals(0, COST.compareTo(lineBiz.subcontractCostMask(newLine())), label + "见 subcontractCost 明文");
        assertEquals(0, COST.compareTo(lineBiz.totalCostMask(newLine())), label + "见 totalCost 明文");
        assertEquals(0, COST.compareTo(lineBiz.unitCostMask(newLine())), label + "见 unitCost 明文");
    }

    private void loginAs(String... roles) {
        UserContextImpl ctx = new UserContextImpl();
        ctx.setUserId("mfg-mask-test");
        ctx.setUserName("mfg-mask-test");
        ctx.setRoles(Set.of(roles));
        IUserContext.set(ctx);
    }

    private ErpMfgCostRollupLine newLine() {
        ErpMfgCostRollupLine l = new ErpMfgCostRollupLine();
        l.setMaterialCost(COST);
        l.setLaborCost(COST);
        l.setOverheadCost(COST);
        l.setSubcontractCost(COST);
        l.setTotalCost(COST);
        l.setUnitCost(COST);
        return l;
    }
}
