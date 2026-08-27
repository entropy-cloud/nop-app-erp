package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinApDocument;
import app.erp.fin.dao.entity.ErpFinApDocumentLog;
import app.erp.fin.service.ErpFinConfigs;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.classify.ApDocClassification;
import app.erp.fin.service.classify.IErpFinApDocClassifier;
import app.erp.fin.service.ocr.IErpFinOcrEngine;
import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.biz.IErpPurInvoiceBiz;
import io.nop.api.core.annotations.txn.TransactionPropagation;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.commons.concurrent.ratelimit.DefaultRateLimiter;
import io.nop.commons.concurrent.ratelimit.IRateLimiter;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.file.core.IFileRecord;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.dao.txn.ITransactionTemplate;
import io.nop.file.core.IFileStore;
import io.nop.file.core.UploadRequestBean;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * E3.5 文档摄取管道编排 Processor（`document-driven-ap-automation.md` §1 消费管道：
 * 上传 → OCR 解析 → 分类（置信度）→ 草稿发票 → 三单匹配预填衔接）。
 *
 * <p>每个步骤方法为 {@code protected}（下游可逐个覆盖，对齐 {@code processor-extension-pattern.md}）。
 * 人工门不可绕过（AP-1）：置信度低于阈值 → MANUAL_REVIEW，不生成草稿；人工经
 * {@link #confirmAndDraft} 补充对应方后显式放行。解析结果只作预填（AP-5）：草稿经既有
 * {@link IErpPurInvoiceBiz} 管道创建（approveStatus=UNSUBMITTED），三单匹配校验走既有 approve 链路。
 *
 * <p>文件本体存 nop-file（{@link IFileStore}，AP-3），业务表只存 {@code fileId} 引用 + 解析字段；
 * 每步落 {@link ErpFinApDocumentLog} 处理轨迹（AP-4 审计追溯：文档-发票-凭证回链经 invoiceId + 既有链路）。
 * 管道 config-gate（{@code erp-fin.ap-doc-pipeline-enabled}）默认关闭；异步批量消费经 nop-batch
 * （{@code fin/ap-document.batch.xml}，RECEIVED 状态扫描）。
 *
 * <p>失败可观测（plan 2026-08-27-2006-1）：步骤失败经独立事务落账 FAILED + FAIL 轨迹（外层事务回滚后存活）；
 * 异步消费逐文档独立事务隔离（单文档失败不回滚先行成功文档、不阻断后续 RECEIVED 文档）。
 */
public class ErpFinApDocumentPipelineProcessor {

    static final Logger LOG = LoggerFactory.getLogger(ErpFinApDocumentPipelineProcessor.class);

    /** 解析要素抽取的文本预览截断长度（parseResult.excerpt）。 */
    private static final int EXCERPT_MAX = 400;
    private static final Pattern P_INVOICE_NO = Pattern.compile("(?:发票号码|Invoice\\s*No\\.?)[：:\\s]*([A-Za-z0-9\\-]{6,30})");
    private static final Pattern P_INVOICE_DATE = Pattern.compile("(?:开票日期|Invoice\\s*Date)[：:\\s]*(\\d{4})[年/\\-](\\d{1,2})[月/\\-](\\d{1,2})");
    private static final Pattern P_AMOUNT_WITH_TAX = Pattern.compile("价税合计(?:[（(]大写[)）][^0-9¥￥]*|[：:\\s]*[¥￥]?)[\\s]*([\\d,]+(?:\\.\\d+)?)");
    private static final Pattern P_TAX = Pattern.compile("(?:税额|税\\s*额)[：:\\s]*[¥￥]?\\s*([\\d,]+(?:\\.\\d+)?)");
    private static final Pattern P_TOTAL_EX_TAX = Pattern.compile("(?:金额|合\\s*计)[（(]不含税[)）][：:\\s]*[¥￥]?\\s*([\\d,]+(?:\\.\\d+)?)");
    private static final Pattern P_SUPPLIER_NAME = Pattern.compile("名\\s*称[：:]\\s*([\\u4e00-\\u9fa5A-Za-z0-9（）()]{2,40})");

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IFileStore fileStore;

    /**
     * 失败落账 / 逐项隔离独立事务载体（plan 2026-08-27-2006-1 P1-1/P1-2，
     * 镜像 {@code ErpFinBankReconAutoReverseHelper} / {@code ErpCrmLeadScoringRecalcHelper} 范式）。
     */
    @Inject
    ITransactionTemplate transactionTemplate;

    @Inject
    IOrmTemplate ormTemplate;

    /** 采购发票管道（跨域 I*Biz，懒解析：pur-service 聚合时可用，单 fin 模块测试下为 null 走明确失败）。 */
    private IErpPurInvoiceBiz purInvoiceBiz;

    /** OCR 引擎（ioc:collect-beans 收集；默认本地文本抽取，tess4j 可插拔）。 */
    @Inject
    List<IErpFinOcrEngine> ocrEngines = java.util.Collections.emptyList();

    /** 分类引擎（ioc:collect-beans 收集；默认规则引擎，Phase 2 ML 注入预留）。 */
    @Inject
    List<IErpFinApDocClassifier> classifiers = java.util.Collections.emptyList();

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    public void setFileStore(IFileStore fileStore) {
        this.fileStore = fileStore;
    }


    public void setOcrEngines(List<IErpFinOcrEngine> ocrEngines) {
        this.ocrEngines = ocrEngines == null ? java.util.Collections.emptyList() : ocrEngines;
    }

    public void setClassifiers(List<IErpFinApDocClassifier> classifiers) {
        this.classifiers = classifiers == null ? java.util.Collections.emptyList() : classifiers;
    }

    // ---------- 上传入口（步骤 0：接收） ----------

    /**
     * P2-8 上传入口白名单（plan 2026-08-28-0219-1，与默认 OCR 引擎实际可解析能力对齐）：
     * pdf/txt/csv/json/xml 可直接抽取文本；png/jpg/jpeg 过白名单后由默认引擎返回 null 落人工门（合法扫描件分支）。
     */
    private static final java.util.Set<String> ALLOWED_FILE_EXTS =
            java.util.Set.of("pdf", "txt", "csv", "json", "xml", "png", "jpg", "jpeg");
    private static final java.util.Set<String> ALLOWED_MIME_EXACT =
            java.util.Set.of("application/pdf", "application/json", "application/xml",
                    "image/png", "image/jpeg");
    /** fileName 列精度（orm erp_fin_ap_document.file_name VARCHAR(200)）。 */
    private static final int FILE_NAME_MAX_LENGTH = 200;
    /** P2-1 重复上传守卫覆盖的非终态集合（终态 FAILED/MANUAL_REVIEW/ARCHIVED 外可重传）。 */
    private static final java.util.List<String> DEDUP_GUARD_STATUSES =
            java.util.List.of("RECEIVED", "PARSED", "CLASSIFIED", "DRAFTED");

    public ErpFinApDocument upload(String fileName, String mimeType, String fileBase64,
                                   String orgId, IServiceContext context) {
        requirePipelineEnabled(null);
        acquireUploadPermit();
        validateUploadContract(fileName, mimeType);
        byte[] content = decodeBase64(fileName, fileBase64);
        long maxLen = AppConfig.var(ErpFinConfigs.CONFIG_AP_DOC_MAX_FILE_LENGTH,
                ErpFinConfigs.DEFAULT_AP_DOC_MAX_FILE_LENGTH);
        if (content.length > maxLen) {
            throw new NopException(ErpFinErrors.ERR_AP_DOC_FILE_TOO_LARGE)
                    .param(ErpFinErrors.ARG_FILE_NAME, fileName);
        }
        rejectDuplicateUpload(fileName, content);
        String fileId = saveFile(fileName, mimeType, content);

        ErpFinApDocument doc = docDao().newEntity();
        doc.setFileName(fileName);
        doc.setFileExt(extOf(fileName));
        doc.setMimeType(mimeType);
        doc.setFileLength((long) content.length);
        doc.setFileId(fileId);
        doc.setSourceType("UPLOAD");
        doc.setStatus("RECEIVED");
        doc.setOrgId(orgId);
        doc.setRetryCount(0);
        docDao().saveEntity(doc);
        log(doc.getId(), "RECEIVE", true, "接收上传文件 " + fileName + "（" + content.length + " 字节，fileId=" + fileId + "）");
        return doc;
    }

    // ---------- 管道处理（解析 → 分类 → 草稿） ----------

    public ErpFinApDocument process(String documentId, IServiceContext context) {
        requirePipelineEnabled(documentId);
        ErpFinApDocument doc = requireDocument(documentId);
        if ("DRAFTED".equals(doc.getStatus()) || "ARCHIVED".equals(doc.getStatus())) {
            throw new NopException(ErpFinErrors.ERR_AP_DOC_ILLEGAL_STATUS)
                    .param(ErpFinErrors.ARG_DOCUMENT_ID, documentId)
                    .param(ErpFinErrors.ARG_STATUS, doc.getStatus());
        }

        Map<String, Object> parseResult = parse(doc);
        if (parseResult == null) {
            // 无文本（扫描件/图片或默认引擎不支持）→ 低置信挂人工门（AP-1，不可绕过）
            doc.setStatus("MANUAL_REVIEW");
            doc.setConfidence(BigDecimal.ZERO);
            doc.setErrorMsg("文档无可抽取文本（扫描件或默认引擎不支持），需人工复核");
            docDao().saveOrUpdateEntity(doc);
            log(doc.getId(), "MANUAL_REVIEW", true, doc.getErrorMsg());
            return doc;
        }
        doc.setParseResult(io.nop.core.lang.json.JsonTool.serialize(parseResult, true));
        doc.setStatus("PARSED");
        doc.setErrorMsg(null);
        docDao().saveOrUpdateEntity(doc);
        log(doc.getId(), "PARSE", true, "解析要素：invoiceNo=" + parseResult.get("invoiceNo")
                + ", invoiceDate=" + parseResult.get("invoiceDate") + ", amount=" + parseResult.get("amount"));

        return classifyAndDraft(doc, parseResult, context);
    }

    /** 分类（步骤 2）+ 按置信度分流：达标 → 草稿（步骤 3），低置信 → 人工门。 */
    protected ErpFinApDocument classifyAndDraft(ErpFinApDocument doc, Map<String, Object> parseResult,
                                                IServiceContext context) {
        IErpFinApDocClassifier classifier = firstClassifier();
        if (classifier == null) {
            throw fail(doc, "CLASSIFY", "无分类引擎注册");
        }
        ApDocClassification c = classifier.classify(doc, parseResult, context);
        doc.setDocType(c.getDocType());
        doc.setPartnerId(c.getPartnerId());
        doc.setConfidence(c.getConfidence());
        docDao().saveOrUpdateEntity(doc);
        log(doc.getId(), "CLASSIFY", true, "docType=" + c.getDocType() + ", partnerId=" + c.getPartnerId()
                + ", confidence=" + c.getConfidence() + "（" + c.getReason() + "）");

        double threshold = AppConfig.var(ErpFinConfigs.CONFIG_AP_DOC_CONFIDENCE_THRESHOLD,
                ErpFinConfigs.DEFAULT_AP_DOC_CONFIDENCE_THRESHOLD);
        if (doc.getConfidence() == null || doc.getConfidence().doubleValue() < threshold) {
            doc.setStatus("MANUAL_REVIEW");
            doc.setErrorMsg("分类置信度 " + doc.getConfidence() + " 低于阈值 " + threshold + "，挂人工复核");
            docDao().saveOrUpdateEntity(doc);
            log(doc.getId(), "MANUAL_REVIEW", true, doc.getErrorMsg());
            return doc;
        }
        doc.setStatus("CLASSIFIED");
        docDao().saveOrUpdateEntity(doc);
        return draft(doc, parseResult, context);
    }

    /** 草稿发票生成（步骤 3，人工确认门：approveStatus=UNSUBMITTED；预填只作输入，AP-5）。 */
    protected ErpFinApDocument draft(ErpFinApDocument doc, Map<String, Object> parseResult,
                                     IServiceContext context) {
        if (doc.getInvoiceId() != null) {
            // 幂等：已有草稿回链不重复生成
            doc.setStatus("DRAFTED");
            docDao().saveOrUpdateEntity(doc);
            return doc;
        }
        IErpPurInvoiceBiz purInvoiceBiz = purInvoiceBiz();
        if (purInvoiceBiz == null) {
            throw fail(doc, "DRAFT", "采购发票服务不可用（app-erp-purchase-service 未聚合）");
        }
        try {
            Map<String, Object> data = buildDraftData(doc, parseResult);
            ErpPurInvoice invoice = purInvoiceBiz.save(data, context);
            doc.setInvoiceId(invoice.getId());
            doc.setStatus("DRAFTED");
            doc.setErrorMsg(null);
            docDao().saveOrUpdateEntity(doc);
            log(doc.getId(), "DRAFT", true, "草稿发票 " + invoice.getCode() + "（" + invoice.getId() + "，UNSUBMITTED）已生成，待人工确认后走既有三单匹配审核链路");
            return doc;
        } catch (Exception e) {
            throw fail(doc, "DRAFT", rootMessage(e));
        }
    }

    /** 解析结果 → 草稿 ErpPurInvoice 预填数据（三单匹配预填衔接：供应商/发票号/日期/金额）。 */
    protected Map<String, Object> buildDraftData(ErpFinApDocument doc, Map<String, Object> parseResult) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", "APD-" + System.nanoTime());
        data.put("supplierId", doc.getPartnerId());
        Object invoiceNo = parseResult.get("invoiceNo");
        if (invoiceNo != null) {
            data.put("invoiceNo", String.valueOf(invoiceNo));
        }
        data.put("invoiceType", mapInvoiceType(doc.getDocType()));
        data.put("businessDate", parseResult.get("invoiceDate") != null
                ? parseResult.get("invoiceDate") : CoreMetrics.currentDate().toString());
        data.put("currencyId", AppConfig.var(ErpFinConfigs.CONFIG_AP_DOC_DEFAULT_CURRENCY_ID,
                ErpFinConfigs.DEFAULT_AP_DOC_DEFAULT_CURRENCY_ID));
        data.put("docStatus", "DRAFT");
        data.put("approveStatus", "UNSUBMITTED");
        BigDecimal amount = toBd(parseResult.get("amount"));
        BigDecimal tax = toBd(parseResult.get("tax"));
        BigDecimal total = toBd(parseResult.get("total"));
        if (total == null && amount != null && tax != null) {
            total = amount.subtract(tax);
        }
        data.put("totalAmountWithTax", amount == null ? BigDecimal.ZERO : amount);
        data.put("totalTaxAmount", tax == null ? BigDecimal.ZERO : tax);
        data.put("totalAmount", total == null ? BigDecimal.ZERO : total);
        data.put("amountSource", amount == null ? BigDecimal.ZERO : amount);
        data.put("amountFunctional", amount == null ? BigDecimal.ZERO : amount);
        data.put("remark", "E3.5 文档摄取管道自动草稿（文档 fileId=" + doc.getFileId() + "）");
        if (doc.getOrgId() != null) {
            data.put("orgId", doc.getOrgId());
        }
        return data;
    }

    // ---------- 人工门与重试 ----------

    /** 人工复核放行：补充/确认对应方后直接生成草稿（人工门出口，须显式 partnerId）。 */
    public ErpFinApDocument confirmAndDraft(String documentId, String partnerId, IServiceContext context) {
        requirePipelineEnabled(documentId);
        ErpFinApDocument doc = requireDocument(documentId);
        if (!"MANUAL_REVIEW".equals(doc.getStatus())) {
            throw new NopException(ErpFinErrors.ERR_AP_DOC_ILLEGAL_STATUS)
                    .param(ErpFinErrors.ARG_DOCUMENT_ID, documentId)
                    .param(ErpFinErrors.ARG_STATUS, doc.getStatus());
        }
        if (StringHelper.isEmpty(partnerId)) {
            throw new NopException(ErpFinErrors.ERR_AP_DOC_DRAFT_FAILED)
                    .param(ErpFinErrors.ARG_DOCUMENT_ID, documentId)
                    .param(ErpFinErrors.ARG_STEP, "人工放行必须显式指定对应方 partnerId");
        }
        doc.setPartnerId(partnerId);
        doc.setConfidence(BigDecimal.ONE);
        docDao().saveOrUpdateEntity(doc);
        log(doc.getId(), "MANUAL_REVIEW", true, "人工复核放行：partnerId=" + partnerId);

        Map<String, Object> parseResult = doc.getParseResult() == null ? Map.of()
                : io.nop.core.lang.json.JsonTool.parseMap(doc.getParseResult());
        doc.setStatus("CLASSIFIED");
        docDao().saveOrUpdateEntity(doc);
        return draft(doc, parseResult, context);
    }

    /** 失败/人工复核重试：计数 + 轨迹，重跑解析分类（草稿幂等：已有回链不重复生成）。 */
    public ErpFinApDocument retry(String documentId, IServiceContext context) {
        requirePipelineEnabled(documentId);
        ErpFinApDocument doc = requireDocument(documentId);
        if (!"FAILED".equals(doc.getStatus()) && !"MANUAL_REVIEW".equals(doc.getStatus())) {
            throw new NopException(ErpFinErrors.ERR_AP_DOC_ILLEGAL_STATUS)
                    .param(ErpFinErrors.ARG_DOCUMENT_ID, documentId)
                    .param(ErpFinErrors.ARG_STATUS, doc.getStatus());
        }
        doc.setRetryCount((doc.getRetryCount() == null ? 0 : doc.getRetryCount()) + 1);
        docDao().saveOrUpdateEntity(doc);
        log(doc.getId(), "RETRY", true, "第 " + doc.getRetryCount() + " 次重试");
        return process(documentId, context);
    }

    /**
     * 扫描待处理文档（nop-batch 异步消费入口：RECEIVED 状态）。
     *
     * <p>P1-2 逐项失败隔离（plan 2026-08-27-2006-1）：单文档失败不回滚先行已成功文档、不阻断后续
     * RECEIVED 文档（此前队头失败回滚整批并饿死后续扫描）。失败文档以 FAILED 终态落账
     * （{@link #persistFailure}），自然退出 RECEIVED 扫描循环，不再被重复命中。
     */
    public int processPending(IServiceContext context) {
        requirePipelineEnabled(null);
        QueryBean q = new QueryBean();
        q.addFilter(eq("status", "RECEIVED"));
        q.setLimit(100);
        List<ErpFinApDocument> pending = docDao().findAllByQuery(q);
        int processed = 0;
        for (ErpFinApDocument doc : pending) {
            if (processOne(doc.getId(), context)) {
                processed++;
            }
        }
        return processed;
    }

    /**
     * 单文档独立事务处理 + 失败隔离（镜像 {@code ErpCrmLeadScoringRecalcHelper.recalculateOne} 范式：
     * batch.xml 保持 process 事务 scope，per-item 隔离由本方法 REQUIRES_NEW 承载——
     * nop-batch process/chunk 两 scope 均整 chunk 单事务，不提供逐条隔离）。
     *
     * @return true=处理成功（含挂人工门）；false=处理失败（WARN 记录，批次继续）
     */
    protected boolean processOne(String documentId, IServiceContext context) {
        try {
            return transactionTemplate.runInTransaction(null, TransactionPropagation.REQUIRES_NEW, txn ->
                    ormTemplate.runInSession(session -> {
                        process(documentId, context);
                        session.flush();
                        return true;
                    }));
        } catch (Exception e) {
            LOG.warn("erp-fin-ap-doc-process-failed: documentId={}, reason={}", documentId, e.getMessage());
            return false;
        }
    }

    // ---------- 解析（步骤 1） ----------

    /** OCR 抽取 + 要素规则解析；无文本返回 null（低置信路径）。 */
    protected Map<String, Object> parse(ErpFinApDocument doc) {
        byte[] content = readFile(doc);
        String text = extractText(doc, content);
        if (StringHelper.isBlank(text)) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("invoiceNo", find(P_INVOICE_NO, text));
        String date = findDate(text);
        if (date != null) {
            result.put("invoiceDate", date);
        }
        result.put("amount", find(P_AMOUNT_WITH_TAX, text));
        result.put("tax", find(P_TAX, text));
        result.put("total", find(P_TOTAL_EX_TAX, text));
        result.put("supplierName", find(P_SUPPLIER_NAME, text));
        result.put("excerpt", text.length() > EXCERPT_MAX ? text.substring(0, EXCERPT_MAX) : text);
        return result;
    }

    protected String extractText(ErpFinApDocument doc, byte[] content) {
        for (IErpFinOcrEngine engine : ocrEngines) {
            String text = engine.extractText(doc.getFileName(), doc.getMimeType(), content);
            if (!StringHelper.isBlank(text)) {
                return text;
            }
        }
        return null;
    }

    // ---------- 内部辅助 ----------

    /** E3.6 护栏：上传入口限流（IRateLimiter 令牌桶，防自动化/AI 批量误操作面；0 = 不限流）。 */
    private volatile IRateLimiter uploadRateLimiter;
    private volatile double uploadRateLimitRps = -1.0;

    private void acquireUploadPermit() {
        double rps = AppConfig.var(ErpFinConfigs.CONFIG_AP_DOC_UPLOAD_RATE_LIMIT_RPS,
                ErpFinConfigs.DEFAULT_AP_DOC_UPLOAD_RATE_LIMIT_RPS);
        if (rps <= 0) {
            return;
        }
        IRateLimiter limiter = this.uploadRateLimiter;
        if (limiter == null || this.uploadRateLimitRps != rps) {
            synchronized (this) {
                if (this.uploadRateLimiter == null || this.uploadRateLimitRps != rps) {
                    this.uploadRateLimiter = DefaultRateLimiter.create(rps);
                    this.uploadRateLimitRps = rps;
                }
            }
            limiter = this.uploadRateLimiter;
        }
        if (!limiter.tryAcquire()) {
            throw new NopException(ErpFinErrors.ERR_AP_DOC_RATE_LIMITED)
                    .param(ErpFinErrors.ARG_RATE_LIMIT_RPS, rps);
        }
    }

    private void requirePipelineEnabled(String documentId) {
        boolean enabled = AppConfig.var(ErpFinConfigs.CONFIG_AP_DOC_PIPELINE_ENABLED,
                ErpFinConfigs.DEFAULT_AP_DOC_PIPELINE_ENABLED);
        if (!enabled) {
            throw new NopException(ErpFinErrors.ERR_AP_DOC_PIPELINE_DISABLED)
                    .param(ErpFinErrors.ARG_DOCUMENT_ID, documentId);
        }
    }

    private ErpFinApDocument requireDocument(String documentId) {
        ErpFinApDocument doc = docDao().getEntityById(documentId);
        if (doc == null) {
            throw new NopException(ErpFinErrors.ERR_AP_DOC_NOT_FOUND)
                    .param(ErpFinErrors.ARG_DOCUMENT_ID, documentId);
        }
        return doc;
    }

    private IErpPurInvoiceBiz purInvoiceBiz() {
        if (purInvoiceBiz == null) {
            purInvoiceBiz = io.nop.api.core.ioc.BeanContainer.instance().tryGetBeanByType(IErpPurInvoiceBiz.class);
        }
        return purInvoiceBiz;
    }

    private IErpFinApDocClassifier firstClassifier() {
        for (IErpFinApDocClassifier c : classifiers) {
            return c;
        }
        return null;
    }

    private static String rootMessage(Throwable e) {
        StringBuilder sb = new StringBuilder(String.valueOf(e.getMessage()));
        Throwable cur = e;
        int guard = 0;
        while (cur.getCause() != null && cur.getCause() != cur && guard++ < 8) {
            cur = cur.getCause();
            sb.append(" <- ").append(cur.getMessage());
        }
        return sb.toString();
    }

    /**
     * 步骤失败面客错误码对位（P2-9，plan 2026-08-28-0219-1）：PARSE 步 →
     * {@code ERR_AP_DOC_PARSE_FAILED}；DRAFT 步 → {@code ERR_AP_DOC_DRAFT_FAILED}；
     * CLASSIFY 步（仅「无分类引擎注册」部署异常）复用 {@code ERR_AP_DOC_DRAFT_FAILED}——
     * 分类为草稿前置步骤，失败即草稿无法生成，且不为部署异常新增专属面客码（复用裁决登记于计划执行注记）。
     */
    private RuntimeException fail(ErpFinApDocument doc, String step, String message) {
        String errorMsg = "步骤 " + step + " 失败：" + message;
        persistFailure(doc.getId(), step, errorMsg);
        io.nop.api.core.exceptions.ErrorCode code = "PARSE".equals(step)
                ? ErpFinErrors.ERR_AP_DOC_PARSE_FAILED
                : ErpFinErrors.ERR_AP_DOC_DRAFT_FAILED;
        return new NopException(code)
                .param(ErpFinErrors.ARG_DOCUMENT_ID, doc.getId())
                .param(ErpFinErrors.ARG_STEP, step);
    }

    /**
     * P1-1 失败落账独立事务（plan 2026-08-27-2006-1）：FAILED 状态 + FAIL 轨迹行经 REQUIRES_NEW
     * 独立事务提交，保证外层 {@code @BizMutation} / batch process 事务随异常回滚后失败证据仍存活
     * （{@code retry()} 的 FAILED 守卫因此在同步路径可达）。
     *
     * <p>块内按 id 在新 session 重载实体后更新（外层事务可能已对同一行 staged PARSED/CLASSIFIED 写，
     * 直接 saveOrUpdate 传入实例有 session 附着/行锁自阻塞风险）；失败落账自身异常仅 WARN——
     * 不掩盖原始业务异常。镜像 {@code ErpFinBankReconAutoReverseHelper} 独立事务范式。
     */
    private void persistFailure(String documentId, String step, String errorMsg) {
        try {
            transactionTemplate.runInTransaction(null, TransactionPropagation.REQUIRES_NEW, txn ->
                    ormTemplate.runInNewSession(session -> {
                        ErpFinApDocument d = docDao().getEntityById(documentId);
                        if (d != null) {
                            d.setStatus("FAILED");
                            d.setErrorMsg(errorMsg);
                            docDao().saveOrUpdateEntity(d);
                            log(documentId, "FAIL", false, errorMsg);
                            session.flush();
                        }
                        return null;
                    }));
        } catch (Exception e) {
            LOG.warn("erp-fin-ap-doc-fail-persist-error: documentId={}, step={}, reason={}",
                    documentId, step, e.getMessage());
        }
    }

    private void log(String documentId, String step, boolean success, String detail) {
        ErpFinApDocumentLog entry = logDao().newEntity();
        entry.setDocumentId(documentId);
        entry.setStep(step);
        entry.setSuccess(success);
        entry.setDetail(detail == null || detail.length() > 1000 ? (detail == null ? null : detail.substring(0, 1000)) : detail);
        logDao().saveEntity(entry);
    }

    private String saveFile(String fileName, String mimeType, byte[] content) {
        UploadRequestBean req = new UploadRequestBean(
                new java.io.ByteArrayInputStream(content), fileName, content.length, mimeType);
        req.setBizObjName("ErpFinApDocument");
        req.setFieldName("fileId");
        return fileStore.saveFile(req, AppConfig.var(ErpFinConfigs.CONFIG_AP_DOC_MAX_FILE_LENGTH,
                ErpFinConfigs.DEFAULT_AP_DOC_MAX_FILE_LENGTH));
    }

    private byte[] readFile(ErpFinApDocument doc) {
        IFileRecord record;
        try {
            record = fileStore.getFile(doc.getFileId());
        } catch (Exception e) {
            // 文件记录缺失（如 nop-file 记录不存在）与读取失败同样计入 PARSE 失败落账（P1-1）
            throw fail(doc, "PARSE", "文件读取失败：" + e.getMessage());
        }
        if (record == null) {
            throw fail(doc, "PARSE", "文件不存在（fileId=" + doc.getFileId() + "）");
        }
        try (InputStream in = record.getResource().getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } catch (Exception e) {
            throw fail(doc, "PARSE", "文件读取失败：" + e.getMessage());
        }
    }

    private byte[] decodeBase64(String fileName, String fileBase64) {
        if (StringHelper.isEmpty(fileBase64)) {
            return new byte[0];
        }
        try {
            return Base64.getDecoder().decode(fileBase64);
        } catch (IllegalArgumentException e) {
            // P2-8：非法 base64 以 NopException 范式面客（不再裸抛 IllegalArgumentException）
            throw new NopException(ErpFinErrors.ERR_AP_DOC_INVALID_BASE64, e)
                    .param(ErpFinErrors.ARG_FILE_NAME, fileName);
        }
    }

    /** P2-8 上传入口契约：fileName 非空 + 列精度 200 上限 + 扩展名/MIME 白名单。 */
    private void validateUploadContract(String fileName, String mimeType) {
        if (StringHelper.isBlank(fileName)) {
            throw new NopException(ErpFinErrors.ERR_AP_DOC_FILE_TYPE_NOT_ALLOWED)
                    .param(ErpFinErrors.ARG_FILE_NAME, fileName);
        }
        if (fileName.length() > FILE_NAME_MAX_LENGTH) {
            throw new NopException(ErpFinErrors.ERR_AP_DOC_FILE_NAME_TOO_LONG)
                    .param(ErpFinErrors.ARG_FILE_NAME, fileName);
        }
        String ext = extOf(fileName);
        if (ext == null || !ALLOWED_FILE_EXTS.contains(ext.toLowerCase())) {
            throw new NopException(ErpFinErrors.ERR_AP_DOC_FILE_TYPE_NOT_ALLOWED)
                    .param(ErpFinErrors.ARG_FILE_NAME, fileName);
        }
        if (mimeType != null && !isAllowedMime(mimeType)) {
            throw new NopException(ErpFinErrors.ERR_AP_DOC_FILE_TYPE_NOT_ALLOWED)
                    .param(ErpFinErrors.ARG_FILE_NAME, fileName);
        }
    }

    private boolean isAllowedMime(String mimeType) {
        String normalized = mimeType.toLowerCase().trim();
        if (ALLOWED_MIME_EXACT.contains(normalized) || normalized.startsWith("text/")) {
            return true;
        }
        // 默认引擎 isPlainText/isPdf 按 contains 判定，白名单对齐同语义（如 application/xml; charset=UTF-8）
        return normalized.contains("pdf") || normalized.contains("json") || normalized.contains("xml");
    }

    /**
     * P2-1 重复上传守卫（plan 2026-08-28-0219-1 方案 A，应用层零 ORM）：按 fileName 查非终态
     * （RECEIVED/PARSED/CLASSIFIED/DRAFTED）文档，fileLength 相同者逐候选读文件本体比对内容摘要，
     * 命中即拒绝并指向既有文档。并发窗口残留风险（两请求同刻通过守卫）登记 owner doc
     * （最终权威去重 = 三单匹配）。
     */
    private void rejectDuplicateUpload(String fileName, byte[] content) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("fileName", fileName));
        q.addFilter(io.nop.api.core.beans.FilterBeans.in("status", DEDUP_GUARD_STATUSES));
        List<ErpFinApDocument> candidates = docDao().findAllByQuery(q);
        for (ErpFinApDocument candidate : candidates) {
            if (candidate.getFileLength() != null && candidate.getFileLength() == content.length
                    && contentMatches(candidate, content)) {
                throw new NopException(ErpFinErrors.ERR_AP_DOC_DUPLICATE_UPLOAD)
                        .param(ErpFinErrors.ARG_FILE_NAME, fileName)
                        .param(ErpFinErrors.ARG_DOCUMENT_ID, candidate.getId())
                        .param(ErpFinErrors.ARG_STATUS, candidate.getStatus());
            }
        }
    }

    /** 候选文档文件本体内容摘要比对；读取失败视为不重复（不阻断新上传）。 */
    private boolean contentMatches(ErpFinApDocument candidate, byte[] content) {
        try {
            IFileRecord record = fileStore.getFile(candidate.getFileId());
            if (record == null) {
                return false;
            }
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] candidateHash;
            try (InputStream in = record.getResource().getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
                candidateHash = digest.digest(out.toByteArray());
            }
            byte[] contentHash = java.security.MessageDigest.getInstance("SHA-256").digest(content);
            return java.util.Arrays.equals(candidateHash, contentHash);
        } catch (Exception e) {
            LOG.warn("erp-fin-ap-doc-dedup-read-failed: documentId={}, fileId={}, reason={}",
                    candidate.getId(), candidate.getFileId(), e.getMessage());
            return false;
        }
    }

    private String extOf(String fileName) {
        if (fileName == null) {
            return null;
        }
        int idx = fileName.lastIndexOf('.');
        return idx < 0 ? null : fileName.substring(idx + 1);
    }

    private String find(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1) : null;
    }

    private String findDate(String text) {
        Matcher m = P_INVOICE_DATE.matcher(text);
        if (!m.find()) {
            return null;
        }
        int year = Integer.parseInt(m.group(1));
        int month = Math.min(Math.max(Integer.parseInt(m.group(2)), 1), 12);
        int day = Math.min(Math.max(Integer.parseInt(m.group(3)), 1), YearMonth.of(year, month).lengthOfMonth());
        return LocalDate.of(year, month, day).toString();
    }

    private BigDecimal toBd(Object v) {
        if (v == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(v).replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String mapInvoiceType(String docType) {
        if ("VAT_INVOICE".equals(docType)) {
            return "VAT_SPECIAL";
        }
        if ("GENERAL_INVOICE".equals(docType)) {
            return "VAT_NORMAL";
        }
        if ("RECEIPT".equals(docType)) {
            return "RECEIPT";
        }
        return "VAT_NORMAL";
    }

    private IEntityDao<ErpFinApDocument> docDao() {
        return daoProvider.daoFor(ErpFinApDocument.class);
    }

    private IEntityDao<ErpFinApDocumentLog> logDao() {
        return daoProvider.daoFor(ErpFinApDocumentLog.class);
    }
}
