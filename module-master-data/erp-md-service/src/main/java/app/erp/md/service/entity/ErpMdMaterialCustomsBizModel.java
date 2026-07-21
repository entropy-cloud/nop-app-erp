package app.erp.md.service.entity;

import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;
import io.nop.api.core.beans.query.QueryBean;

import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.ErpMdDaoConstants;
import app.erp.md.dao.entity.ErpMdMaterialCustoms;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.service.ErpMdErrors;

import jakarta.inject.Inject;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * C2 跨境贸易扩展：物料报关记录 BizModel（docs/design/master-data/cross-border-trade.md §3）。
 *
 * <p>基础 CRUD 走 {@link CrudBizModel} 默认实现。本类仅扩展 3 个前置校验钩子（在
 * {@code defaultPrepareSave} / {@code defaultPrepareUpdate} 中执行），将 DB UK 违反
 * 转换为友好业务异常：
 * <ul>
 *   <li>{@code declarationNo} 全局唯一（DB UK 前置）—— 抛
 *       {@link ErpMdErrors#ERR_CUSTOMS_DECLARATION_NO_DUPLICATE}。</li>
 *   <li>{@code partnerId} 引用的 Partner 类型必须为 {@code CUSTOMS_BROKER} —— 抛
 *       {@link ErpMdErrors#ERR_PARTNER_NOT_CUSTOMS_BROKER}。</li>
 *   <li>{@code sourceBillType} / {@code sourceBillCode} 至少一个非空（业务回链必填）—— 抛
 *       {@link ErpMdErrors#ERR_CUSTOMS_SOURCE_BILL_REQUIRED}。</li>
 * </ul>
 *
 * <p>状态机 / 审批流 / 跨域业务联动 / 关税计算引擎接入均属 Non-Goal，归 successor plan
 * （触发：业务客户具体业务流程需求 + 跨域 owner doc 授权）。
 */
@BizModel("ErpMdMaterialCustoms")
public class ErpMdMaterialCustomsBizModel extends CrudBizModel<ErpMdMaterialCustoms>
        implements app.erp.md.biz.IErpMdMaterialCustomsBiz {

    @Inject
    protected IErpMdPartnerBiz partnerBiz;

    public ErpMdMaterialCustomsBizModel() {
        setEntityName(ErpMdMaterialCustoms.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpMdMaterialCustoms> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        validateOnPersist(entityData.getEntity(), context);
    }

    @Override
    protected void defaultPrepareUpdate(EntityData<ErpMdMaterialCustoms> entityData, IServiceContext context) {
        super.defaultPrepareUpdate(entityData, context);
        validateOnPersist(entityData.getEntity(), context);
    }

    /**
     * 持久化前统一校验：declarationNo 唯一 + partnerId Partner 类型 + sourceBill 业务回链必填。
     * protected 供下游覆盖扩展（产品化扩展点）。
     */
    protected void validateOnPersist(ErpMdMaterialCustoms entity, IServiceContext context) {
        if (entity == null) {
            return;
        }
        enforceDeclarationNoUnique(entity, context);
        enforcePartnerIsCustomsBroker(entity, context);
        enforceSourceBillPresent(entity);
    }

    /**
     * declarationNo 应用层查重：报关单号全局唯一（UK 前置友好校验，避免 DB UK violation stack trace 暴露）。
     *
     * <p>实现说明：直接经 {@code dao().findAllByQuery(query)} 查询避免 CrudBizModel 管道的
     * objMeta filter 校验（默认仅允许 eq/in/date-between，ne 在 id 上不被允许）。
     * 参考 {@code ErpMdMaterialBizModel.isCodeUnique} 同范式的解法。
     */
    protected void enforceDeclarationNoUnique(ErpMdMaterialCustoms entity, IServiceContext context) {
        String declarationNo = entity.getDeclarationNo();
        if (declarationNo == null || declarationNo.isEmpty()) {
            return;
        }
        QueryBean query = new QueryBean();
        query.addFilter(eq("declarationNo", declarationNo));
        for (ErpMdMaterialCustoms existing : dao().findAllByQuery(query)) {
            if (entity.getId() == null || !Objects.equals(entity.getId(), existing.getId())) {
                throw new NopException(ErpMdErrors.ERR_CUSTOMS_DECLARATION_NO_DUPLICATE)
                        .param(ErpMdErrors.ARG_DECLARATION_NO, declarationNo);
            }
        }
    }

    /**
     * partnerId 报关行类型校验：引用的 Partner 类型必须为 CUSTOMS_BROKER。
     * partnerId 可空（允许无报关行自报场景）；非空时校验类型。
     */
    protected void enforcePartnerIsCustomsBroker(ErpMdMaterialCustoms entity, IServiceContext context) {
        Long partnerId = entity.getPartnerId();
        if (partnerId == null) {
            return;
        }
        ErpMdPartner partner = partnerBiz.findById(partnerId, context);
        if (partner == null) {
            throw new NopException(ErpMdErrors.ERR_PARTNER_NOT_FOUND)
                    .param(ErpMdErrors.ARG_PARTNER_ID, partnerId);
        }
        if (!Objects.equals(ErpMdDaoConstants.PARTNER_TYPE_CUSTOMS_BROKER, partner.getPartnerType())) {
            throw new NopException(ErpMdErrors.ERR_PARTNER_NOT_CUSTOMS_BROKER)
                    .param(ErpMdErrors.ARG_PARTNER_ID, partnerId)
                    .param(ErpMdErrors.ARG_PARTNER_TYPE, partner.getPartnerType());
        }
    }

    /**
     * sourceBillType / sourceBillCode 业务回链必填：至少一个非空。
     */
    protected void enforceSourceBillPresent(ErpMdMaterialCustoms entity) {
        String sourceBillType = entity.getSourceBillType();
        String sourceBillCode = entity.getSourceBillCode();
        boolean empty = (sourceBillType == null || sourceBillType.isEmpty())
                && (sourceBillCode == null || sourceBillCode.isEmpty());
        if (empty) {
            throw new NopException(ErpMdErrors.ERR_CUSTOMS_SOURCE_BILL_REQUIRED)
                    .param(ErpMdErrors.ARG_DECLARATION_NO, entity.getDeclarationNo());
        }
    }
}
