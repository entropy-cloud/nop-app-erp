package app.erp.fin.biz;

import io.nop.orm.biz.ICrudBiz;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.core.context.IServiceContext;

import app.erp.fin.dao.entity.ErpFinApDocument;


public interface IErpFinApDocumentBiz extends ICrudBiz<ErpFinApDocument>{

    /**
     * E3.5 上传入口（`document-driven-ap-automation.md` §1 消费管道步骤 0）：文件本体存 nop-file
     * （IFileStore），业务表只存 fileId 引用（AP-3）；落 RECEIVED + RECEIVE 轨迹日志。
     *
     * @param fileBase64 文件内容 base64 编码
     */
    @BizMutation
    ErpFinApDocument uploadApDocument(@Name("fileName") String fileName,
                                      @Optional @Name("mimeType") String mimeType,
                                      @Name("fileBase64") String fileBase64,
                                      @Optional @Name("orgId") String orgId,
                                      IServiceContext context);

    /**
     * E3.5 管道处理（解析 → 分类 → 草稿）：OCR 引擎 SPI 抽取文本 + 要素规则解析 → 规则分类引擎
     * （置信度）→ 达标生成草稿 ErpPurInvoice（UNSUBMITTED，人工确认门 AP-1）；低置信挂 MANUAL_REVIEW。
     * 亦为 nop-batch 异步消费入口的单文档处理动作。config-gate 默认关闭。
     */
    @BizMutation
    ErpFinApDocument processApDocument(@Name("documentId") String documentId, IServiceContext context);

    /**
     * E3.5 人工复核放行（人工门出口）：显式补充对应方 partnerId 后生成草稿。
     */
    @BizMutation
    ErpFinApDocument confirmAndDraftApDocument(@Name("documentId") String documentId,
                                               @Name("partnerId") String partnerId,
                                               IServiceContext context);

    /**
     * E3.5 失败/人工复核重试：retryCount 计数 + RETRY 轨迹后重跑管道（草稿幂等：已有 invoiceId 回链不重复生成）。
     */
    @BizMutation
    ErpFinApDocument retryApDocument(@Name("documentId") String documentId, IServiceContext context);
}
