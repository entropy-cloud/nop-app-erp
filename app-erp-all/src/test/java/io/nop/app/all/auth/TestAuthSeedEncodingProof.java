package io.nop.app.all.auth;

import io.nop.auth.core.password.BCryptPasswordEncoder;
import io.nop.auth.core.password.CompositePasswordEncoder;
import io.nop.auth.core.password.IPasswordEncoder;
import io.nop.auth.core.password.SHA256PasswordEncoder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * auth 表 CSV 种子密码编码方案 Proof（plan 2026-08-09-2107-1，P1.5b Phase 1 + Phase 3）。
 *
 * <p>纯逻辑测试（不经 IoC 容器/DB）：手动构造 {@link CompositePasswordEncoder}，其装配与平台 bean
 * {@code nopPasswordEncoder}（{@code auth-core-defaults.beans.xml:37-40}：firstEncoder=SHA256，
 * secondEncoder=BCrypt，useSecondSalt 未设=默认 false）逐字一致。
 *
 * <p>Phase 1 Proof：对密码 "123" 生成 SALT+PASSWORD 对，验证 {@code passwordMatches} 往返成立。
 *
 * <p>Phase 3 Proof：验证 CSV 种子中硬编码的 SALT+PASSWORD 对（Phase 1 生成的快照）经登录 verify
 * 路径（{@code passwordMatches}）一致——证明 CSV 编码密码与运行时 verify 兼容。
 *
 * <p>编码机制（load-bearing）：encode = sha256(pwd+salt) → BCrypt.hashpw(sha256hash)（BCrypt 自带随机
 * salt，非确定性）；verify = BCrypt.checkpw(sha256(pwd+storedSalt), storedPassword)。一对 SALT+PASSWORD
 * 生成后即稳定可验证。BCrypt 非确定性使肉眼不透明 → 经往返 Proof 消解。
 */
public class TestAuthSeedEncodingProof {

    /** 装配与平台 nopPasswordEncoder bean 逐字一致。 */
    private static IPasswordEncoder newPlatformEncoder() {
        CompositePasswordEncoder encoder = new CompositePasswordEncoder();
        encoder.setFirstEncoder(new SHA256PasswordEncoder());
        encoder.setSecondEncoder(new BCryptPasswordEncoder());
        return encoder;
    }

    /**
     * Phase 1 Proof：平台编码器对 "123" 往返成立。同时输出可提交的 SALT+PASSWORD 对供 CSV 使用。
     * 每次运行会生成不同的 SALT+PASSWORD 对（BCrypt 随机 salt），但任一对都稳定可验证。
     */
    @Test
    public void testPasswordEncoderRoundtripForSeedPassword() {
        IPasswordEncoder passwordEncoder = newPlatformEncoder();

        String salt = passwordEncoder.generateSalt();
        String encodedPassword = passwordEncoder.encodePassword(salt, "123");

        System.out.println("===AUTH_SEED_PAIR_BEGIN===");
        System.out.println("SALT=" + salt);
        System.out.println("PASSWORD=" + encodedPassword);
        System.out.println("===AUTH_SEED_PAIR_END===");

        assertTrue(passwordEncoder.passwordMatches(salt, "123", encodedPassword),
                "生成的 SALT+PASSWORD 对必须经 passwordMatches 往返验证");
    }

    /**
     * Phase 3 Proof：CSV 种子中硬编码的 SALT+PASSWORD 对（Phase 1 生成快照）经 verify 路径一致。
     * 这组值是 Phase 1 testPasswordEncoderRoundtripForSeedPassword 输出的某次快照，
     * 固化到 nop_auth_user.csv 后必须保持可验证。密码明文 = "123"（与 E2E fixture tests/e2e/auth.ts 一致）。
     */
    @Test
    public void testCsvSeedPasswordRoundtrip() {
        IPasswordEncoder passwordEncoder = newPlatformEncoder();
        String csvSalt = "26dce419976e4e7f95f7a9dcb82e5bc4";
        String csvPassword = "$2a$10$74DaI9b3RwzmmA3xpb6ZN.WTzl2YjNf7cLVTTaW1TmaW1rGCdn892";

        assertTrue(passwordEncoder.passwordMatches(csvSalt, "123", csvPassword),
                "CSV 种子硬编码的 SALT+PASSWORD 对必须经 passwordMatches 验证（密码明文=123）");
    }
}
