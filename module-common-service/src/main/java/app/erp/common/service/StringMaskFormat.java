package app.erp.common.service;

/**
 * E3.1 VARCHAR 字段脱敏格式（plan 2026-08-10-2059-2 Phase 1 Decision (c)）。
 *
 * <p>与 F7 前端 tpl（§9.2）打码模板对齐；当输入过短无法保留首/末段时退化为全打码（避免短值泄漏）。
 */
public enum StringMaskFormat {
    /** 证件号：首1 + ****** + 末4（输入 len > 5 时；否则全打码）。 */
    ID_CARD {
        @Override
        public String mask(String value) {
            if (value == null || value.isEmpty()) {
                return value;
            }
            if (value.length() <= 5) {
                return FULL.mask(value);
            }
            return value.charAt(0) + "******" + value.substring(value.length() - 4);
        }
    },
    /** 手机号：首3 + **** + 末4（输入 len > 7 时；否则全打码）。 */
    MOBILE {
        @Override
        public String mask(String value) {
            if (value == null || value.isEmpty()) {
                return value;
            }
            if (value.length() <= 7) {
                return FULL.mask(value);
            }
            return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
        }
    },
    /** 全打码（社保号 / taxFileNo / cumulativeData 等高敏或机密 JSON）。 */
    FULL {
        @Override
        public String mask(String value) {
            if (value == null || value.isEmpty()) {
                return value;
            }
            return "******";
        }
    };

    public abstract String mask(String value);
}
