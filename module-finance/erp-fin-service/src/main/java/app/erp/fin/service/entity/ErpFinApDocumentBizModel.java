package app.erp.fin.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Description;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

import app.erp.fin.biz.IErpFinApDocumentBiz;
import app.erp.fin.dao.entity.ErpFinApDocument;
import app.erp.fin.service.processor.ErpFinApDocumentPipelineProcessor;
import jakarta.inject.Inject;


/**
 * AP 摄取文档实体服务。E3.5 扩展（`document-driven-ap-automation.md` §1）：管道动作
 * （上传/处理/人工放行/重试）委托 {@link ErpFinApDocumentPipelineProcessor}
 * （protected step 编排，产品化拓扑可变）；CRUD 走平台默认。
 */
@BizModel("ErpFinApDocument")
public class ErpFinApDocumentBizModel extends CrudBizModel<ErpFinApDocument> implements IErpFinApDocumentBiz{

    @Inject
    ErpFinApDocumentPipelineProcessor pipelineProcessor;

    public ErpFinApDocumentBizModel(){
        setEntityName(ErpFinApDocument.class.getName());
    }

    @Override
    @BizMutation
    @Description("上传应付（AP）文档进入摄取管道：文件本体存 nop-file，落 RECEIVED 状态与摄取轨迹")
    public ErpFinApDocument uploadApDocument(@Name("fileName") String fileName,
                                             @Optional @Name("mimeType") String mimeType,
                                             @Name("fileBase64") String fileBase64,
                                             @Optional @Name("orgId") String orgId,
                                             IServiceContext context) {
        return pipelineProcessor.upload(fileName, mimeType, fileBase64, orgId, context);
    }

    @Override
    @BizMutation
    public ErpFinApDocument processApDocument(@Name("documentId") String documentId, IServiceContext context) {
        return pipelineProcessor.process(documentId, context);
    }

    @Override
    @BizMutation
    public ErpFinApDocument confirmAndDraftApDocument(@Name("documentId") String documentId,
                                                      @Name("partnerId") String partnerId,
                                                      IServiceContext context) {
        return pipelineProcessor.confirmAndDraft(documentId, partnerId, context);
    }

    @Override
    @BizMutation
    public ErpFinApDocument retryApDocument(@Name("documentId") String documentId, IServiceContext context) {
        return pipelineProcessor.retry(documentId, context);
    }
}
