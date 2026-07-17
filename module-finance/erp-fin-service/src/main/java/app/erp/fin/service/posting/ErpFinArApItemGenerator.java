package app.erp.fin.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import java.util.Objects;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 应收应付辅助账（{@link ErpFinArApItem}）生成器。在业财过账成功（凭证落库）后、同事务内调用，
 * 按 {@link PostingEvent#getBusinessType()} 生成辅助账项，保证「凭证 + 辅助账」强一致
 * （{@code posting.md} 三层模型第①层精神；{@code processor-extension-pattern.md} 选项 A）。
 *
 * <p>方向映射（{@code ar-ap-reconciliation.md}）：AP_INVOICE/AR_INVOICE 生成应付/应收正项；
 * PAYMENT/RECEIPT 生成付款/收款项（同 direction，供核销多对多匹配）。幂等：同 sourceBillCode+sourceBillType
 * 已存在则跳过。非 AR/AP 业务类型（如 PURCHASE_INPUT/DEPRECIATION）为空操作。
 *
 * <p>billData 字段契约：往来单位 ID 取自 {@code partnerId}（优先）或 {@code SUPPLIER_ID}/{@code CUSTOMER_ID}
 * （兼容 0300-1/0300-2 派发器）；金额——发票项取 {@code TOTAL_AMOUNT_WITH_TAX}（含税总额，回退 TOTAL→TOTAL_AMOUNT），
 * 收付款项取 {@code TOTAL}（回退 AMOUNT）；{@code amountSource} 回退 amountFunctional；
 * {@code businessDate}(LocalDate, 回退 voucherDate)；{@code dueDate}(LocalDate, 可空——源发票无到期日列时落 null，账龄基准回退 invoice_date，见 plan S2)。
 *
 * <p>红冲：{@link #cancelOnReverse} 按业财回链反查原辅助账项，置 status=CANCELLED、openAmount=0
 * （对齐冲销语义：源单据红冲后其辅助账项不再可核销）。
 */
public class ErpFinArApItemGenerator {

    /**
     * 辅助账 code 总长上限，对齐 {@code module-finance/model/app-erp-finance.orm.xml} domain
     * {@code voucherCode} precision=50（{@link ErpFinArApItem#getCode() code} 列绑该 domain）。
     * 生成器为 DAG 顶，不反向依赖 ORM domain 元数据 API，故在此持有常量而非运行时读取 precision。
     */
    protected static final int AR_AP_ITEM_CODE_MAX_LENGTH = 50;

    /**
     * 超限路径追加的 MD5 摘要长度（hex 字符）。保留全长 sourceBillCode 指纹，使两条不同长码即使头部相同
     * 也不退化为同一 code（可追溯）；全局唯一性兜底由 {@code uuid8} 保证。
     */
    private static final int AR_AP_ITEM_HASH_SUFFIX_LENGTH = 4;

    @Inject
    IDaoProvider daoProvider;

    /**
     * 过账成功后生成辅助账项。非 AR/AP 业务类型或幂等命中时为空操作。
     *
     * @return 新生成的辅助账项；无需生成（非 AR/AP 类型或幂等命中）返回 {@code null}
     */
    public ErpFinArApItem generate(PostingEvent event, IServiceContext context) {
        SourceProfile profile = resolveProfile(event.getBusinessType());
        if (profile == null) {
            return null;
        }
        // 公司直付（paymentMode=COMPANY_ACCOUNT）的报销直接贷银行存款，不挂应付-员工，不生成员工应付辅助账。
        if (event.getBusinessType() == ErpFinBusinessType.EXPENSE_CLAIM
                && isCompanyAccountPayment(event.getBillData())) {
            return null;
        }
        if (existsItem(profile.sourceBillType, event.getBillHeadCode(), context)) {
            return null;
        }

        Map<String, Object> data = event.getBillData();
        Long partnerId = resolvePartnerId(data);
        if (partnerId == null) {
            throw new NopException(ErpFinErrors.ERR_AR_AP_ITEM_PARTNER_MISSING)
                    .param(ErpFinErrors.ARG_SOURCE_BILL_CODE, event.getBillHeadCode())
                    .param(ErpFinErrors.ARG_SOURCE_BILL_TYPE, profile.sourceBillType);
        }
        BigDecimal amountFunctional = resolveAmountFunctional(data, profile.sourceBillType);
        if (amountFunctional == null) {
            throw new NopException(ErpFinErrors.ERR_AR_AP_ITEM_AMOUNT_MISSING)
                    .param(ErpFinErrors.ARG_SOURCE_BILL_CODE, event.getBillHeadCode())
                    .param(ErpFinErrors.ARG_SOURCE_BILL_TYPE, profile.sourceBillType);
        }
        BigDecimal amountSource = asAmount(data.get("amountSource"), amountFunctional);
        LocalDate businessDate = asLocalDate(data.get("businessDate"), event.getVoucherDate());
        LocalDate dueDate = asLocalDate(data.get("dueDate"), null);

        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        ErpFinArApItem item = dao.newEntity();
        item.setCode(buildCode(profile.sourceBillType, event.getBillHeadCode()));
        item.setOrgId(event.getOrgId());
        item.setAcctSchemaId(event.getAcctSchemaId());
        item.setDirection(profile.direction);
        item.setPartnerId(partnerId);
        item.setSourceBillType(profile.sourceBillType);
        item.setSourceBillCode(event.getBillHeadCode());
        item.setBusinessDate(businessDate);
        item.setDueDate(dueDate);
        item.setCurrencyId(event.getCurrencyId());
        item.setExchangeRate(event.getExchangeRate() != null ? event.getExchangeRate() : BigDecimal.ONE);
        item.setAmountSource(amountSource);
        item.setAmountFunctional(amountFunctional);
        item.setSettledAmountSource(BigDecimal.ZERO);
        item.setSettledAmountFunctional(BigDecimal.ZERO);
        item.setOpenAmountSource(amountSource);
        item.setOpenAmountFunctional(amountFunctional);
        item.setStatus(ErpFinConstants.AR_AP_STATUS_OPEN);
        item.setPeriodId(null);
        dao.saveEntity(item);
        return item;
    }

    /**
     * 红冲时取消原辅助账项：置 status=CANCELLED、openAmount=0（对齐冲销语义）。
     * 找不到原项时为空操作（兼容未生成辅助账的历史单据）。
     */
    public void cancelOnReverse(String billHeadCode, ErpFinBusinessType businessType, IServiceContext context) {
        SourceProfile profile = resolveProfile(businessType);
        if (profile == null) {
            return;
        }
        List<ErpFinArApItem> items = findItems(profile.sourceBillType, billHeadCode, context);
        for (ErpFinArApItem item : items) {
            item.setStatus(ErpFinConstants.AR_AP_STATUS_CANCELLED);
            item.setOpenAmountSource(BigDecimal.ZERO);
            item.setOpenAmountFunctional(BigDecimal.ZERO);
        }
    }

    // ---------- helpers ----------

    protected SourceProfile resolveProfile(ErpFinBusinessType businessType) {
        if (businessType == null) {
            return null;
        }
        switch (businessType) {
            case AP_INVOICE:
                return new SourceProfile(ErpFinConstants.DIRECTION_PAYABLE, ErpFinConstants.SOURCE_BILL_AP_INVOICE);
            case AR_INVOICE:
                return new SourceProfile(ErpFinConstants.DIRECTION_RECEIVABLE, ErpFinConstants.SOURCE_BILL_AR_INVOICE);
            case PAYMENT:
                return new SourceProfile(ErpFinConstants.DIRECTION_PAYABLE, ErpFinConstants.SOURCE_BILL_PAYMENT);
            case RECEIPT:
                return new SourceProfile(ErpFinConstants.DIRECTION_RECEIVABLE, ErpFinConstants.SOURCE_BILL_RECEIPT);
            case PURCHASE_RETURN:
                // 采购退货冲减应付：DIRECTION_PAYABLE 方向 + 负 openAmount（标准 AP 贷项 credit memo 语义），
                // 使 PartnerBalanceUpdater.sumOpen 自然减计 payableBalance（无侵入，零改动 sumOpen/方向枚举）。
                return new SourceProfile(ErpFinConstants.DIRECTION_PAYABLE, ErpFinConstants.SOURCE_BILL_PUR_RETURN);
            case SALES_RETURN:
                // 销售退货冲减应收：DIRECTION_RECEIVABLE 方向 + 负 openAmount（标准 AR 贷项 credit memo 语义），
                // 使 PartnerBalanceUpdater.sumOpen 自然减计 receivableBalance（无侵入，零改动 sumOpen/方向枚举）。
                return new SourceProfile(ErpFinConstants.DIRECTION_RECEIVABLE, ErpFinConstants.SOURCE_BILL_SAL_RETURN);
            case EXPENSE_CLAIM:
                // 费用报销：应付-员工（正 openAmount），partnerId = 已解析的 employee.partnerId（billData.EMPLOYEE_ID）。
                return new SourceProfile(ErpFinConstants.DIRECTION_PAYABLE, ErpFinConstants.SOURCE_BILL_EXPENSE_CLAIM);
            case EMPLOYEE_ADVANCE:
                // 员工借款：其他应收-员工预支（正 openAmount），partnerId = 已解析的 employee.partnerId。
                return new SourceProfile(ErpFinConstants.DIRECTION_RECEIVABLE, ErpFinConstants.SOURCE_BILL_EMPLOYEE_ADVANCE);
            case EMPLOYEE_ADVANCE_SETTLE:
                // 借款清算（报销抵扣）：不生成新辅助账，由抵扣编排直接回写既有 advance 应收 / expense 应付 open item 状态。
                return null;
            case NOTES_RECEIVABLE_RECEIVED:
                // 应收票据收到：生成应收侧辅助账（DIRECTION_RECEIVABLE），抵客户 AR_INVOICE 应收（同方向核销，见 plan Phase 3）。
                return new SourceProfile(ErpFinConstants.DIRECTION_RECEIVABLE, ErpFinConstants.SOURCE_BILL_NOTES_RECEIVABLE);
            case NOTES_RECEIVABLE_ENDORSED:
                // 票据背书转让：生成应付侧辅助账（DIRECTION_PAYABLE），抵供应商 AP_INVOICE 应付（同方向核销）。
                return new SourceProfile(ErpFinConstants.DIRECTION_PAYABLE, ErpFinConstants.SOURCE_BILL_NOTES_ENDORSED);
            case OWNERSHIP_TRANSFER:
                // VMI 消耗等所有权转移：生成应付侧辅助账（DIRECTION_PAYABLE），openAmount 等待供应商采购发票核销
                // （consignment.md §业财过账；本项目自建能力，非 Odoo 借鉴）。金额取 TOTAL_COST（转移的存货成本）。
                return new SourceProfile(ErpFinConstants.DIRECTION_PAYABLE, ErpFinConstants.SOURCE_BILL_OWNERSHIP_TRANSFER);
            default:
                return null;
        }
    }

    protected boolean existsItem(String sourceBillType, String sourceBillCode, IServiceContext context) {
        return !findItems(sourceBillType, sourceBillCode, context).isEmpty();
    }

    /** 报销公司直付（paymentMode=COMPANY_ACCOUNT）：贷银行存款，不挂应付-员工辅助账。 */
    protected boolean isCompanyAccountPayment(Map<String, Object> data) {
        Object mode = data.get(ErpFinConstants.BILL_DATA_PAYMENT_MODE);
        return ErpFinConstants.PAYMENT_MODE_COMPANY_ACCOUNT.equals(mode);
    }

    protected List<ErpFinArApItem> findItems(String sourceBillType, String sourceBillCode, IServiceContext context) {
        if (StringHelper.isBlank(sourceBillCode)) {
            return java.util.Collections.emptyList();
        }
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("sourceBillType", sourceBillType), eq("sourceBillCode", sourceBillCode)));
        return dao.findAllByQuery(q);
    }

    /**
     * 生成辅助账 code。短码（拼接结果 ≤ {@link #AR_AP_ITEM_CODE_MAX_LENGTH}）路径保持原拼接
     * {@code "ARI-" + sourceBillType + "-" + sourceBillCode + "-" + uuid8} 逐字符不变；
     * 超限时进入截断 + 哈希摘要分支：优先保留 {@code "ARI-" + sourceBillType + uuid8} 固定段不截断
     * （sourceBillType 最长 {@code OWNERSHIP_TRANSFER}=18 → 固定段最坏 4+18+2+8=32 &lt; 50，
     * 留 ≥18 给 sourceBillCode 压缩段），对 sourceBillCode 段截取头部并追加 MD5 前 4 hex 摘要。
     *
     * <p>覆盖 {@code resolveProfile} 全分支：任意 sourceBillType + 任意长度 sourceBillCode 均返回
     * ≤ {@link #AR_AP_ITEM_CODE_MAX_LENGTH}，消除 voucherCode precision 50 的字符串右截断（sqlState=22001）
     * latent defect。
     */
    protected String buildCode(String sourceBillType, String sourceBillCode) {
        String safeCode = sourceBillCode != null ? sourceBillCode : "";
        String uuid8 = StringHelper.generateUUID().substring(0, 8);
        String code = "ARI-" + sourceBillType + "-" + safeCode + "-" + uuid8;
        if (code.length() <= AR_AP_ITEM_CODE_MAX_LENGTH) {
            return code;
        }
        // 固定段 = "ARI-"(4) + sourceBillType + 两个 "-" 分隔符 + uuid8(8)；剩余预算给 sourceBillCode 压缩段。
        int budget = AR_AP_ITEM_CODE_MAX_LENGTH
                - "ARI-".length() - sourceBillType.length() - 2 - uuid8.length();
        int headLen = Math.max(0, budget - AR_AP_ITEM_HASH_SUFFIX_LENGTH);
        String head = headLen > 0 ? safeCode.substring(0, Math.min(headLen, safeCode.length())) : "";
        String hash = StringHelper.md5Hash(safeCode).substring(0, AR_AP_ITEM_HASH_SUFFIX_LENGTH);
        return "ARI-" + sourceBillType + "-" + head + hash + "-" + uuid8;
    }

    /**
     * 解析往来单位 ID。优先 `partnerId`，兼容 0300-1/0300-2 派发器使用的 `SUPPLIER_ID`/`CUSTOMER_ID`
     * （这些派发器构造 billData 时按业务域命名，未统一为 partnerId）。
     *
     * <p>员工方（EXPENSE_CLAIM/EMPLOYEE_ADVANCE）：派发器在 billData 的 {@code EMPLOYEE_ID} 键携带
     * <b>已解析的 {@code employee.partnerId}</b>（即 ErpMdPartner.id，非 employee.id——员工与 partner 是不同
     * id 空间），本生成器直接采用，不二次反查 master-data（finance 为 DAG 顶，生成器只读 billData）。
     */
    protected Long resolvePartnerId(Map<String, Object> data) {
        Long partnerId = asLong(data.get("partnerId"));
        if (partnerId != null) {
            return partnerId;
        }
        partnerId = asLong(data.get(ErpFinConstants.BILL_DATA_EMPLOYEE_ID));
        if (partnerId != null) {
            return partnerId;
        }
        partnerId = asLong(data.get("SUPPLIER_ID"));
        if (partnerId != null) {
            return partnerId;
        }
        return asLong(data.get("CUSTOMER_ID"));
    }

    /**
     * 解析本位币金额。发票项（AP_INVOICE/AR_INVOICE）取含税总额（应付/应收正项），
     * 回退 TOTAL→TOTAL_AMOUNT；收付款项（PAYMENT/RECEIPT）取 TOTAL，回退 AMOUNT。
     * 兼容 0300-1（Pur: TOTAL_AMOUNT_WITH_TAX/SUPPLIER_ID）与 0300-2（Sal: 同）的 billData 键。
     *
     * <p>采购退货（PUR_RETURN）：取 TOTAL_AMOUNT（不含税，对齐未开票暂估应付冲减口径），**取负**
     * （AP 贷项 credit memo，使 sumOpen 减计应付余额）。进项税/含税属已开票红字发票 Non-Goal。
     *
     * <p>销售退货（SAL_RETURN）：取 TOTAL_AMOUNT_WITH_TAX（退货含税售价，credit memo 口径），**取负**
     * （AR 贷项 credit memo，使 sumOpen 减计应收余额）。退货凭证只冲成本/存货（SALES_OUTPUT 反向），
     * 收入/应收 GL 红字冲销属 credit-note Non-Goal；应收余额回减经此辅助账层完成。
     */
    protected BigDecimal resolveAmountFunctional(Map<String, Object> data, String sourceBillType) {
        if (ErpFinConstants.SOURCE_BILL_AP_INVOICE.equals(sourceBillType)
                || ErpFinConstants.SOURCE_BILL_AR_INVOICE.equals(sourceBillType)
                || ErpFinConstants.SOURCE_BILL_EXPENSE_CLAIM.equals(sourceBillType)) {
            // EXPENSE_CLAIM 同发票口径：取含税总额（应付-员工正项），回退 TOTAL→TOTAL_AMOUNT→AMOUNT。
            return asAmount(data.get("TOTAL_AMOUNT_WITH_TAX"),
                    asAmount(data.get("TOTAL"),
                            asAmount(data.get("TOTAL_AMOUNT"),
                                    asAmount(data.get("AMOUNT"), null))));
        }
        BigDecimal amount = asAmount(data.get("TOTAL"), asAmount(data.get("AMOUNT"), null));
        if (ErpFinConstants.SOURCE_BILL_NOTES_RECEIVABLE.equals(sourceBillType)
                || ErpFinConstants.SOURCE_BILL_NOTES_ENDORSED.equals(sourceBillType)) {
            // 票据辅助账金额取票面（FACE_AMOUNT），回退 TOTAL/AMOUNT。
            return asAmount(data.get("FACE_AMOUNT"), amount);
        }
        if (ErpFinConstants.SOURCE_BILL_OWNERSHIP_TRANSFER.equals(sourceBillType)) {
            // 所有权转移应付金额取转移的存货成本（TOTAL_COST），回退 TOTAL/AMOUNT。
            return asAmount(data.get("TOTAL_COST"), amount);
        }
        if (ErpFinConstants.SOURCE_BILL_PUR_RETURN.equals(sourceBillType)) {
            // 退货冲减：TOTAL_AMOUNT 正值取负；null 时回退 null（由调用方校验缺失）。
            BigDecimal returnAmount = asAmount(data.get("TOTAL_AMOUNT"), null);
            if (returnAmount != null) {
                return returnAmount.negate();
            }
            return amount != null ? amount.negate() : null;
        }
        if (ErpFinConstants.SOURCE_BILL_SAL_RETURN.equals(sourceBillType)) {
            // 销售退货冲减：TOTAL_AMOUNT_WITH_TAX（含税售价）正值取负；回退 TOTAL_AMOUNT → AMOUNT。
            BigDecimal returnAmount = asAmount(data.get("TOTAL_AMOUNT_WITH_TAX"),
                    asAmount(data.get("TOTAL_AMOUNT"), amount));
            if (returnAmount != null) {
                return returnAmount.negate();
            }
            return null;
        }
        return amount;
    }

    private static BigDecimal asAmount(Object value, BigDecimal fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        String s = value.toString();
        if (StringHelper.isBlank(s)) {
            return fallback;
        }
        return new BigDecimal(s);
    }

    private static Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        String s = value.toString();
        if (StringHelper.isBlank(s)) {
            return null;
        }
        try {
            return Long.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDate asLocalDate(Object value, LocalDate fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        String s = value.toString();
        if (StringHelper.isBlank(s)) {
            return fallback;
        }
        try {
            return LocalDate.parse(s);
        } catch (Exception e) {
            return fallback;
        }
    }

    /** 来源单据方向配置。 */
    protected static final class SourceProfile {
        final String direction;
        final String sourceBillType;

        SourceProfile(String direction, String sourceBillType) {
            this.direction = direction;
            this.sourceBillType = sourceBillType;
        }
    }
}
