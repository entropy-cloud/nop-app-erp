
package app.erp.mfg.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;
import app.erp.mfg.dao.entity.ErpMfgBom;

import java.math.BigDecimal;
import java.util.List;

public interface IErpMfgBomBiz extends ICrudBiz<ErpMfgBom>{

    /**
     * 按产出物料取默认 BOM（{@code isDefault=true} 且 {@code isActive=true}）。工单自动选择 / 多级展开 / 成本卷算入口。
     *
     * @throws NopException {@code ERR_DEFAULT_BOM_NOT_FOUND} 当无默认且有效的 BOM
     */
    @BizQuery
    ErpMfgBom findDefaultBom(@Name("productId") Long productId, IServiceContext context);

    /**
     * BOM 展开。
     *
     * @param bomId          被展开的 BOM
     * @param qty            期望产出量（null 则取 {@code BOM.qty}，即一个标准批量）
     * @param useMultiLevel  true=多级递归展开（制造子件深入其 BOM）；false=仅单级直接子件
     * @return 扁平化展开节点（不含根产出；虚拟件本身不出现，其子件并入父级层级）
     * @throws NopException 环引用（{@code ERR_BOM_CYCLE}）/ 深度超限（{@code ERR_BOM_MAX_DEPTH_EXCEEDED}）
     */
    @BizQuery
    List<BomExplosionNode> explode(@Name("bomId") Long bomId,
                                   @Name("qty") BigDecimal qty,
                                   @Name("useMultiLevel") Boolean useMultiLevel,
                                   IServiceContext context);

    /**
     * 成本卷算（Cost Rollup）。按低层码自下而上：采购件取默认 SKU {@code purchasePrice} 为基础成本；
     * 制造件 = Σ(子件单位用量 × 子件单位成本)【材料】+ Σ(工序 standardTime/60 × workcenter.hourlyRate)
     * 【直接人工+制造费用】；逐层向上汇总，写入 {@code ErpMfgCostRollup}(status=CALCULATED) +
     * {@code ErpMfgCostRollupLine}。
     *
     * @throws NopException 采购件无默认 SKU 采购价时 {@code ERR_ROLLUP_BASE_COST_MISSING}
     */
    @BizMutation
    CostRollupResult rollupCost(@Name("bomId") Long bomId, IServiceContext context);
}
