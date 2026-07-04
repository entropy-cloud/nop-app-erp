
package app.erp.mfg.biz;

import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.mfg.dao.entity.ErpMfgCostRollup;

import java.math.BigDecimal;

public interface IErpMfgCostRollupBiz extends ICrudBiz<ErpMfgCostRollup>{

    /**
     * 解析物料当前标准单位成本：取最近一条 status=FIRMED 的卷算单中该物料的行 {@code unitCost}
     * （plan 2026-07-05-0427-2 STANDARD 计价方法的标准成本来源）。
     *
     * <p>跨域经 inventory {@code StandardCostResolver} 注入本接口调用（inventory→manufacturing R，
     * 接口声明于 mfg-dao 供 inv-service 编译期依赖）。
     *
     * @return 标准 unitCost；无已 FIRMED 卷算行或该物料不在行中时返回 {@code null}
     */
    @BizQuery
    BigDecimal findLatestFirmedStandardCost(@Name("materialId") Long materialId, IServiceContext context);
}
