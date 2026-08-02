package app.erp.cs.service.entity;

import app.erp.cs.biz.IErpCsServiceCatalogItemBiz;
import app.erp.cs.dao.entity.ErpCsServiceCatalogItem;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.service.processor.ErpCsServiceCatalogItemCreateFromCatalogProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.Map;

/**
 * 服务目录项 BizModel（{@code docs/design/customer-service/service-catalog.md §一/§二}）。
 *
 * <p>核心方法 {@link #createFromCatalog} 委派 {@link ErpCsServiceCatalogItemCreateFromCatalogProcessor}：
 * 目录项驱动建单——按 catalogItem 的 ticketTypeId/slaPolicyId 自动填充建 {@link ErpCsTicket}，
 * 写 catalogItemId，requestFormConfig JSON 字段映射到工单扩展属性，联动权益匹配（同客户），
 * 并触发履行首步 CREATE_TICKET 落地。
 *
 * <p>config-gated by {@link app.erp.cs.service.ErpCsConfigs#isServiceCatalogEnabled}。
 */
@BizModel("ErpCsServiceCatalogItem")
public class ErpCsServiceCatalogItemBizModel extends CrudBizModel<ErpCsServiceCatalogItem>
        implements IErpCsServiceCatalogItemBiz {

    @Inject
    ErpCsServiceCatalogItemCreateFromCatalogProcessor createFromCatalogProcessor;

    public ErpCsServiceCatalogItemBizModel() {
        setEntityName(ErpCsServiceCatalogItem.class.getName());
    }

    @Override
    @BizMutation
    public ErpCsTicket createFromCatalog(@Name("catalogItemId") Long catalogItemId,
                                         @Optional @Name("formData") Map<String, Object> formData,
                                         IServiceContext context) {
        return createFromCatalogProcessor.createFromCatalog(catalogItemId, formData, context);
    }

    

}
