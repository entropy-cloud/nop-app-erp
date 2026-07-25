
package app.erp.sal.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.erp.md.service.daterange.ErpDateRanges;
import app.erp.sal.biz.IErpSalPriceListBiz;
import app.erp.sal.dao.entity.ErpSalPriceList;

import java.util.Comparator;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 销售价格清单 BizModel。
 *
 * <p>基础 CRUD 走 {@link CrudBizModel}。扩展 C3 日期范围有效性保存钩子（plan 2026-07-26-0315-1 Phase 2）：
 * 同 customerGroupCode + partnerId 维度的多份清单允许重叠（PRIORITY 策略），运行时取价由
 * {@code ErpSalCustomerPriceResolver} 按 {@code priority} ASC 择优。
 *
 * <p><b>PRIORITY 不拒绝重叠</b>：本钩子仅作 warn-on-ambiguity 提示——当候选保存后该维度同日生效清单 ≥ 2
 * 且最高与次高优先级数值相等时，记录 warn 提示取价歧义（不抛异常，不阻断保存）。详见
 * {@code docs/design/date-ranged-validity-pattern.md} §4 PRIORITY 策略段。
 */
@BizModel("ErpSalPriceList")
public class ErpSalPriceListBizModel extends CrudBizModel<ErpSalPriceList> implements IErpSalPriceListBiz {

    private static final Logger LOG = LoggerFactory.getLogger(ErpSalPriceListBizModel.class);

    public ErpSalPriceListBizModel() {
        setEntityName(ErpSalPriceList.class.getName());
    }

    @Override
    protected void defaultPrepareQuery(QueryBean query, IServiceContext context) {
        super.defaultPrepareQuery(query, context);
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpSalPriceList> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        warnIfPriorityAmbiguous(entityData.getEntity());
    }

    @Override
    protected void defaultPrepareUpdate(EntityData<ErpSalPriceList> entityData, IServiceContext context) {
        super.defaultPrepareUpdate(entityData, context);
        warnIfPriorityAmbiguous(entityData.getEntity());
    }

    /**
     * PRIORITY 策略 warn-on-ambiguity：同 customerGroupCode + partnerId 维度同日多份清单 + 优先级并列时记录 warn。
     *
     * <p>实现说明：直接经 {@code dao().findAllByQuery(query)} 查同维度既有记录（含自身，便于候选参与歧义判定），
     * 用 {@link ErpDateRanges#effectiveOn} 取候选 validFrom 当日生效集合，按 priority ASC 排序，
     * 若首尾两条 priority 相等则存在歧义。
     */
    protected void warnIfPriorityAmbiguous(ErpSalPriceList entity) {
        if (entity == null || entity.getValidFrom() == null) {
            return;
        }
        QueryBean query = new QueryBean();
        query.addFilter(eq("customerGroupCode", entity.getCustomerGroupCode()));
        query.addFilter(eq("partnerId", entity.getPartnerId()));
        List<ErpSalPriceList> sameDimension = dao().findAllByQuery(query);
        if (sameDimension.size() < 2) {
            return;
        }
        List<ErpSalPriceList> effective = ErpDateRanges.effectiveOn(sameDimension, entity.getValidFrom());
        if (effective.size() < 2) {
            return;
        }
        effective.sort(Comparator.comparingInt(ErpSalPriceListBizModel::safePriority));
        int top = safePriority(effective.get(0));
        int next = safePriority(effective.get(1));
        if (top == next) {
            LOG.warn("价格清单同 customerGroupCode={},partnerId={} 维度在 validFrom={} 存在多份相同优先级（={}）清单，取价可能产生歧义（warn-only，不阻断保存）",
                    entity.getCustomerGroupCode(), entity.getPartnerId(),
                    entity.getValidFrom(), top);
        }
    }

    private static int safePriority(ErpSalPriceList r) {
        return r.getPriority() != null ? r.getPriority() : Integer.MAX_VALUE;
    }
}
