package app.erp.cs.service.entity;

import app.erp.cs.service.ErpCsConstants;

import java.util.UUID;

/**
 * 调查令牌生成器。生成无鉴权访问的 UUID 令牌。权威：{@code docs/design/customer-service/csat.md §1.1}。
 */
public final class SurveyTokenGenerator {

    private SurveyTokenGenerator() {
    }

    /** 生成 32 位无连字符 UUID（数据库字段 precision=50，足够容纳带连字符形式）。 */
    public static String generate() {
        return ErpCsConstants.class.getSimpleName() + "-" + UUID.randomUUID().toString().replace("-", "");
    }
}
