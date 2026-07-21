
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;

import app.erp.md.biz.IErpMdTaxRateBiz;
import app.erp.md.dao.entity.ErpMdTaxRate;
import app.erp.md.service.ErpMdErrors;
import app.erp.md.service.daterange.ErpDateRangeOverlapValidator;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 税率主数据 BizModel（C3 日期范围有效性模式试点，docs/design/date-ranged-validity-pattern.md §6/§7）。
 *
 * <p>基础 CRUD 走 {@link CrudBizModel} 默认实现。本类扩展 1 个前置校验钩子：
 * 同维度（taxType）区间互斥（MUTEX 策略）—— 同 taxType 在同一时刻至多 1 条有效税率档。
 * 重叠抛 {@link ErpMdErrors#ERR_MD_DATE_RANGE_OVERLAP}。
 */
@BizModel("ErpMdTaxRate")
public class ErpMdTaxRateBizModel extends CrudBizModel<ErpMdTaxRate> implements IErpMdTaxRateBiz {

    public ErpMdTaxRateBizModel() {
        setEntityName(ErpMdTaxRate.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpMdTaxRate> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        enforceNoOverlap(entityData.getEntity());
    }

    @Override
    protected void defaultPrepareUpdate(EntityData<ErpMdTaxRate> entityData, IServiceContext context) {
        super.defaultPrepareUpdate(entityData, context);
        enforceNoOverlap(entityData.getEntity());
    }

    /**
     * 同维度（taxType）区间互斥校验。
     *
     * <p>注意：taxType 字段 {@code mandatory=true}，{@code eq} filter 永远有值；若历史脏数据存在 null，
     * 按 SQL null=null 语义判定（仅当两侧均 null 视为同维度）。
     */
    protected void enforceNoOverlap(ErpMdTaxRate entity) {
        if (entity == null) {
            return;
        }
        QueryBean query = new QueryBean();
        query.addFilter(eq("taxType", entity.getTaxType()));
        ErpDateRangeOverlapValidator.enforceMutex(
                entity,
                dao().findAllByQuery(query),
                ErpMdErrors.ERR_MD_DATE_RANGE_OVERLAP,
                entity.getId());
    }
}
