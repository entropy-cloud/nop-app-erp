
package app.erp.pur.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import app.erp.pur.biz.IErpPurOrderLineBiz;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import app.erp.pur.service.support.ErpPurCtDiscountApplier;

/**
 * 采购订单行 BizModel。RC-R1.79（P1-RC-078，UC-CT-08 A）：引用合同行的行保存/更新时
 * 解析量折扣折后价写行金额（fill-when-absent，D2 裁决选项 a——命中才改写，无命中回退原价零改写）。
 */
@BizModel("ErpPurOrderLine")
public class ErpPurOrderLineBizModel extends CrudBizModel<ErpPurOrderLine> implements IErpPurOrderLineBiz{

    @Inject
    ErpPurCtDiscountApplier ctDiscountApplier;

    public ErpPurOrderLineBizModel(){
        setEntityName(ErpPurOrderLine.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpPurOrderLine> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        ctDiscountApplier.applyToLine(entityData.getEntity(), context);
    }

    @Override
    protected void defaultPrepareUpdate(EntityData<ErpPurOrderLine> entityData, IServiceContext context) {
        super.defaultPrepareUpdate(entityData, context);
        ctDiscountApplier.applyToLine(entityData.getEntity(), context);
    }

}
