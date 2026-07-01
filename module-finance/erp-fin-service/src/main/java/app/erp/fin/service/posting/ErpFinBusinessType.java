package app.erp.fin.service.posting;

/**
 * 业财过账业务类型枚举。常量的 {@link #code} 与字典 {@code erp-fin/business-type} 的数值逐一一致，
 * 是过账引擎类型安全的门面（字典是数值权威源，本枚举是编译期类型安全包装）。
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
    EXCHANGE_GAIN_LOSS(130);

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
