package app.erp.cs.service.processor;

import app.erp.cs.biz.IErpCsCatalogFulfillmentBiz;
import app.erp.cs.biz.IErpCsEntitlementBiz;
import app.erp.cs.biz.IErpCsTicketBiz;
import app.erp.cs.dao.entity.ErpCsEntitlement;
import app.erp.cs.dao.entity.ErpCsServiceCatalogItem;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.service.ErpCsConfigs;
import app.erp.cs.service.ErpCsConstants;
import app.erp.cs.service.ErpCsErrors;
import io.nop.core.context.IServiceContext;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ErpCsServiceCatalogItem createFromCatalog per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含目录项驱动建单编排（默认值合并 + 权益匹配扣减 + 工单保存 + 履行首步落地）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCsServiceCatalogItemCreateFromCatalogProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpCsServiceCatalogItemCreateFromCatalogProcessor.class);

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpCsTicketBiz ticketBiz;
    @Inject
    IErpCsCatalogFulfillmentBiz fulfillmentBiz;
    @Inject
    IErpCsEntitlementBiz entitlementBiz;

    public ErpCsTicket createFromCatalog(Long catalogItemId, Map<String, Object> formData, IServiceContext context) {
        if (!ErpCsConfigs.isServiceCatalogEnabled()) {
            throw new NopException(ErpCsErrors.ERR_CATALOG_ITEM_INACTIVE)
                    .param(ErpCsErrors.ARG_CATALOG_ITEM_ID, catalogItemId);
        }
        ErpCsServiceCatalogItem item = requireCatalogItem(catalogItemId, context);
        validateCatalogItemUsable(item);

        Map<String, Object> ticketData = buildTicketData(item, formData);
        // 权益匹配 + 扣减（建单前）：config-gated，与 ErpCsTicketBizModel.matchAndAttachSla 同语义
        // 但不调 matchAndAttachSla（避免 save→update 同事务 SAVING 实体冲突），
        // 而是在 save 前把权益级 slaPolicyId 覆盖写入 ticketData，save 一次性落地。
        applyEntitlementToTicketData(ticketData, context);

        // 经 IErpCsTicketBiz.save 走标准 CRUD 管道（code 自动生成、审批状态默认值等）
        ErpCsTicket ticket = ticketBiz.save(ticketData, context);

        // 履行首步 CREATE_TICKET 落地 + 后续动作登记（ASSIGN_TEAM/NOTIFY_CUSTOMER 等）
        if (fulfillmentBiz != null) {
            try {
                fulfillmentBiz.executeFulfillmentSteps(catalogItemId, ticket.getId(), context);
            } catch (Exception e) {
                // 履行登记失败不阻断建单主流程（已建单可由客服手动跟进）
                LOG.warn("fulfillment-execute-failed (建单已成功，降级): catalogItemId={}, ticketId={}, reason={}",
                        catalogItemId, ticket.getId(), e.getMessage());
            }
        }
        return ticket;
    }

    /**
     * 权益匹配 + 扣减 + slaPolicyId 覆盖写入 ticketData。
     * config-gated by {@link ErpCsConfigs#isEntitlementCheckEnabled}。
     * 无权益时按 {@link ErpCsConfigs#isAllowNoEntitlement} 放行或抛 {@link ErpCsErrors#ERR_ENTITLEMENT_NONE_ACTIVE}。
     */
    private void applyEntitlementToTicketData(Map<String, Object> ticketData, IServiceContext context) {
        if (!ErpCsConfigs.isEntitlementCheckEnabled() || entitlementBiz == null) {
            return;
        }
        Object customerIdObj = ticketData.get("customerId");
        Long customerId = customerIdObj == null ? null : Long.valueOf(String.valueOf(customerIdObj));
        if (customerId == null) {
            return;
        }
        ErpCsEntitlement matched = entitlementBiz.matchForCustomer(customerId);
        if (matched == null) {
            if (!ErpCsConfigs.isAllowNoEntitlement()) {
                throw new NopException(ErpCsErrors.ERR_ENTITLEMENT_NONE_ACTIVE)
                        .param(ErpCsErrors.ARG_PARTNER_ID, customerId);
            }
            // 放行：无权益工单由客服手动跟进
            return;
        }
        // 权益级 slaPolicyId 覆盖目录项默认
        if (matched.getSlaPolicyId() != null) {
            ticketData.put("slaPolicyId", matched.getSlaPolicyId());
        }
        // 扣减（PAY_PER_TICKET 增计，其他类型仅记日志）
        entitlementBiz.consumeEntitlement(matched.getId(), context);
    }

    private ErpCsServiceCatalogItem requireCatalogItem(Long catalogItemId, IServiceContext context) {
        if (catalogItemId == null) {
            throw new NopException(ErpCsErrors.ERR_CATALOG_ITEM_NOT_FOUND)
                    .param(ErpCsErrors.ARG_CATALOG_ITEM_ID, catalogItemId);
        }
        ErpCsServiceCatalogItem item = dao().getEntityById(catalogItemId);
        if (item == null) {
            throw new NopException(ErpCsErrors.ERR_CATALOG_ITEM_NOT_FOUND)
                    .param(ErpCsErrors.ARG_CATALOG_ITEM_ID, catalogItemId);
        }
        return item;
    }

    private void validateCatalogItemUsable(ErpCsServiceCatalogItem item) {
        Boolean active = item.getIsActive();
        if (active == null || !active) {
            throw new NopException(ErpCsErrors.ERR_CATALOG_ITEM_INACTIVE)
                    .param(ErpCsErrors.ARG_CATALOG_ITEM_NAME, item.getName());
        }
        // isPublic=false 时仅客服可见可提交——本期不引入角色鉴权（归前端 successor），
        // 默认允许提交；门户自助前端建立后再加 isPublic + 客户角色校验。
    }

    /**
     * 构建工单数据 Map：合并目录项默认值 + formData 字段映射。
     * formData 优先于目录项默认值（客户填写的覆盖目录项预设）。
     */
    private Map<String, Object> buildTicketData(ErpCsServiceCatalogItem item, Map<String, Object> formData) {
        Map<String, Object> data = new LinkedHashMap<>();
        // code 为必填字段（domain=orderCode 不自动生成），此处按时间戳生成唯一码
        data.put("code", "TK-" + io.nop.api.core.time.CoreMetrics.currentTimeMillis());
        // 目录项默认值
        if (item.getTicketTypeId() != null) {
            data.put("ticketTypeId", item.getTicketTypeId());
        }
        if (item.getSlaPolicyId() != null) {
            data.put("slaPolicyId", item.getSlaPolicyId());
        }
        data.put("catalogItemId", item.getId());
        data.put("status", ErpCsConstants.TICKET_STATUS_NEW);
        data.put("docStatus", ErpCsConstants.DOC_STATUS_DRAFT);
        data.put("approveStatus", ErpCsConstants.APPROVE_STATUS_UNSUBMITTED);

        // formData 字段映射（service-catalog.md §1.4 requestFormConfig fields）
        if (formData != null) {
            copyIfPresent(formData, data, "subject");
            copyIfPresent(formData, data, "description");
            copyIfPresent(formData, data, "customerId");
            copyIfPresent(formData, data, "contactId");
            copyIfPresent(formData, data, "productId");
            copyIfPresent(formData, data, "orderNumber");
            // urgency 表单字段映射到工单 priority（service-catalog.md §1.4 urgency 字段）
            copyIfPresent(formData, data, "urgency", "priority");
            copyIfPresent(formData, data, "source");
        }
        // 主题缺省回退为目录项名（避免空 subject）
        if (!data.containsKey("subject") || data.get("subject") == null) {
            data.put("subject", item.getName());
        }
        // priority 缺省回退为 NORMAL（必填字段，formData.urgency 未提供时）
        if (!data.containsKey("priority") || data.get("priority") == null) {
            data.put("priority", ErpCsConstants.TICKET_PRIORITY_NORMAL);
        }
        return data;
    }

    private void copyIfPresent(Map<String, Object> src, Map<String, Object> dst, String key) {
        copyIfPresent(src, dst, key, key);
    }

    private void copyIfPresent(Map<String, Object> src, Map<String, Object> dst, String srcKey, String dstKey) {
        if (src == null) {
            return;
        }
        Object value = src.get(srcKey);
        if (value != null) {
            dst.put(dstKey, value);
        }
    }

    private IEntityDao<ErpCsServiceCatalogItem> dao() {
        return daoProvider.daoFor(ErpCsServiceCatalogItem.class);
    }
}
