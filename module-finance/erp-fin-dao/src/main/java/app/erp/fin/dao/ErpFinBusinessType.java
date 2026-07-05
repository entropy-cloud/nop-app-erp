package app.erp.fin.dao;

/**
 * 业财过账业务类型枚举。常量的 {@link #code} 与字典 {@code erp-fin/business-type} 的数值逐一一致，
 * 是过账引擎类型安全的门面（字典是数值权威源，本枚举是编译期类型安全包装）。
 *
 * <p>本枚举位于 finance-dao（与 {@code IErpFinVoucherBiz} 跨域契约同层），供 facade 参数（
 * {@code post/reverse}）与各域过账派发器/Provider 跨模块引用；过账编排（finance-service）经 import 使用。
 *
 * <p>新增字典项时须同步追加枚举常量。
 */
public enum ErpFinBusinessType {
    PURCHASE_INPUT(10),
    SALES_OUTPUT(20),
    AP_INVOICE(30),
    AR_INVOICE(40),
    PAYMENT(50),
    RECEIPT(60),
    DEPRECIATION(70),
    CAPITALIZATION(80),
    DISPOSAL(90),
    MANUFACTURING_COST_CLOSE(100),
    PROJECT_COST_COLLECTION(110),
    PERIOD_CLOSE(120),
    EXCHANGE_GAIN_LOSS(130),
    PURCHASE_RETURN(140),
    SALES_RETURN(150),
    EXPENSE_CLAIM(160),
    EMPLOYEE_ADVANCE(170),
    EMPLOYEE_ADVANCE_SETTLE(180),
    NOTES_RECEIVABLE_RECEIVED(190),
    NOTES_RECEIVABLE_DISCOUNTED(200),
    NOTES_RECEIVABLE_ENDORSED(210),
    NOTES_RECEIVABLE_COLLECTION(220),
    NOTES_PAYABLE_ISSUED(230),
    NOTES_PAYABLE_HONORED(240),
    CREDIT_FACILITY_INTEREST(250),
    OWNERSHIP_TRANSFER(260),
    SALARY(270),
    SALARY_PAYMENT(280),
    SOCIAL_INSURANCE_ER(290),
    HOUSING_FUND_ER(300),
    FREIGHT(310),
    BANK_RECON_ADJ(320),
    PURCHASE_PRICE_VARIANCE(330),
    BAD_DEBT_RESERVE(340),
    BAD_DEBT_WRITE_OFF(350),
    BAD_DEBT_RECOVERY(360),
    BAD_DEBT_RELEASE(370),
    PROFIT_TO_RETAINED_EARNINGS(380),
    VALUE_ADJUSTMENT(390),
    PRODUCTION_VARIANCE(400);

    private final int code;

    ErpFinBusinessType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static ErpFinBusinessType fromCode(int code) {
        for (ErpFinBusinessType t : values()) {
            if (t.code == code) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown ErpFinBusinessType code: " + code);
    }
}
