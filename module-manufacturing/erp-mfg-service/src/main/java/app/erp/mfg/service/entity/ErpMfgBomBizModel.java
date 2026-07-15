
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.List;

import app.erp.mfg.biz.BomExplosionNode;
import app.erp.mfg.biz.CostRollupResult;
import app.erp.mfg.biz.IErpMfgBomBiz;
import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.service.ErpMfgErrors;
import app.erp.mfg.service.bom.BomExpander;
import app.erp.mfg.service.costing.CostRollupService;

@BizModel("ErpMfgBom")
public class ErpMfgBomBizModel extends CrudBizModel<ErpMfgBom> implements IErpMfgBomBiz {
    @Inject
    BomExpander bomExpander;
    @Inject
    CostRollupService costRollupService;

    public ErpMfgBomBizModel() {
        setEntityName(ErpMfgBom.class.getName());
    }

    public void setBomExpander(BomExpander bomExpander) {
        this.bomExpander = bomExpander;
    }

    public void setCostRollupService(CostRollupService costRollupService) {
        this.costRollupService = costRollupService;
    }

    @Override
    @BizQuery
    public ErpMfgBom findDefaultBom(@Name("productId") Long productId, IServiceContext context) {
        ErpMfgBom bom = bomExpander.findDefaultBomOrNull(productId);
        if (bom == null) {
            throw new NopException(ErpMfgErrors.ERR_DEFAULT_BOM_NOT_FOUND)
                    .param(ErpMfgErrors.ARG_PRODUCT_ID, productId);
        }
        return bom;
    }

    @Override
    @BizQuery
    public List<BomExplosionNode> explode(@Name("bomId") Long bomId,
                                          @Name("qty") BigDecimal qty,
                                          @Name("useMultiLevel") Boolean useMultiLevel,
                                          IServiceContext context) {
        return bomExpander.explode(bomId, qty, Boolean.TRUE.equals(useMultiLevel));
    }

    @Override
    @BizMutation
    public CostRollupResult rollupCost(@Name("bomId") Long bomId, IServiceContext context) {
        return costRollupService.rollup(bomId);
    }

}
