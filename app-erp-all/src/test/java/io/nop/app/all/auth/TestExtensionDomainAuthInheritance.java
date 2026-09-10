package io.nop.app.all.auth;

import io.nop.core.initialize.ICoreInitializer;
import io.nop.core.initialize.impl.ReflectionHelperMethodInitializer;
import io.nop.core.initialize.impl.VirtualFileSystemInitializer;
import io.nop.core.lang.xml.XNode;
import io.nop.core.resource.IResource;
import io.nop.core.resource.VirtualFileSystem;
import io.nop.xlang.initialize.RegisterModelCoreInitializer;
import io.nop.xlang.initialize.XLangCoreInitializer;
import io.nop.xlang.xdsl.DslNodeLoader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M2.8 分片① app-001-r3 enforcement 前置断言（P3-CK-app-001-r3）。
 *
 * <p>八扩展域（crm/cs/hr/aps/ct/drp/log/b2b）保留层 erp-{short}.action-auth.xml 必须
 * {@code x:extends} 继承生成层 _erp-{short}.action-auth.xml，并对生成层 test-orm 根显式
 * {@code x:override="remove"}（对齐核心域 11 域先例）。聚合器 app.action-auth.xml 仅指向
 * 保留层文件：继承断链时生成层逐实体 FNPT 权限点（FNPT:ErpXxx:query/mutation）不入聚合链，
 * {@code nop.auth.enable-action-auth=true} 下对应权限无资源载体（enforcement 权限面缺口）。
 *
 * <p>断言三面：(1) 保留层 x:extends 继承行在位（继承链恢复）；(2) 生成层 test-orm 根被显式
 * remove（生成层资源计入合并链后显式处置，不作为幽灵测试菜单泄漏进聚合权限面）；(3) 合并后
 * 域模型无 test-orm 泄漏、无重复 resource id（与 TestAppActionAuthMerge 聚合不变式一致）。
 * 修复前 (1)(2) 红——生成层 FNPT 权限点全量脱链且无显式处置；补 x:extends + remove 后绿。
 * 核心域 fin/md 作同断言阳性对照。
 */
public class TestExtensionDomainAuthInheritance {

    /** {short, vfs 前缀}；8 个扩展域 = app-001-r3 修复面 */
    private static final String[][] EXTENSION_DOMAINS = {
            {"crm", "erp/crm"}, {"cs", "erp/cs"}, {"hr", "erp/hr"}, {"aps", "erp/aps"},
            {"ct", "erp/ct"}, {"drp", "erp/drp"}, {"log", "erp/log"}, {"b2b", "erp/b2b"},
    };

    /** 阳性对照：核心域先例（已继承 + 已显式 remove，恒绿守卫测试本身正确性） */
    private static final String[][] CORE_CONTROL_DOMAINS = {
            {"fin", "erp/fin"}, {"md", "erp/md"},
    };

    private static final List<ICoreInitializer> INITIALIZERS = new ArrayList<>();

    @BeforeAll
    static void initCore() {
        if (VirtualFileSystem.isInitialized()) {
            VirtualFileSystem.unregisterInstance(VirtualFileSystem.instance());
        }
        INITIALIZERS.add(new ReflectionHelperMethodInitializer());
        INITIALIZERS.add(new XLangCoreInitializer());
        INITIALIZERS.add(new VirtualFileSystemInitializer());
        INITIALIZERS.add(new RegisterModelCoreInitializer());
        INITIALIZERS.forEach(ICoreInitializer::initialize);
    }

    @AfterAll
    static void destroyCore() {
        for (int i = INITIALIZERS.size() - 1; i >= 0; i--) {
            INITIALIZERS.get(i).destroy();
        }
        INITIALIZERS.clear();
    }

    @Test
    public void testExtensionDomainGeneratedAuthPermissionPointsAccountedFor() {
        List<String> failures = new ArrayList<>();
        for (String[] domain : EXTENSION_DOMAINS) {
            failures.addAll(checkDomain(domain[0], domain[1]));
        }
        assertTrue(failures.isEmpty(),
                "app-001-r3 enforcement 权限面缺口（生成层 FNPT 权限点不入聚合链且无显式处置）:\n"
                        + String.join("\n", failures));
    }

    @Test
    public void testCoreDomainControlGroupStillAligned() {
        List<String> failures = new ArrayList<>();
        for (String[] domain : CORE_CONTROL_DOMAINS) {
            failures.addAll(checkDomain(domain[0], domain[1]));
        }
        assertTrue(failures.isEmpty(),
                "核心域对照断言失败（测试前置假设被破坏）:\n" + String.join("\n", failures));
    }

    private List<String> checkDomain(String shortName, String vfsPrefix) {
        List<String> failures = new ArrayList<>();
        String genPath = "/" + vfsPrefix + "/auth/_erp-" + shortName + ".action-auth.xml";
        String retainedPath = "/" + vfsPrefix + "/auth/erp-" + shortName + ".action-auth.xml";

        int genFnptCount = countGeneratedFnptResources(genPath);
        assertTrue(genFnptCount > 0, "生成层文件无 FNPT 权限点，断言前提不成立: " + genPath);

        String retainedText = readResource(retainedPath);
        if (!retainedText.contains("x:extends=\"_erp-" + shortName + ".action-auth.xml\"")) {
            failures.add(shortName + ": 保留层未 x:extends 继承生成层 " + genPath
                    + "（" + genFnptCount + " 个生成层 FNPT 权限点全量脱链，"
                    + "enable-action-auth=true 下权限无载体）");
        }
        if (!retainedText.contains("id=\"test-orm-erp-" + shortName + "\"")) {
            failures.add(shortName + ": 保留层未显式 remove 生成层 test-orm-erp-" + shortName
                    + " 根（继承恢复后生成层测试菜单资源将泄漏进聚合权限面）");
        }
        if (!failures.isEmpty()) {
            return failures;
        }

        IResource resource = VirtualFileSystem.instance().getResource(retainedPath);
        assertTrue(resource.exists(), "保留层文件不在 VFS: " + retainedPath);
        XNode merged = DslNodeLoader.INSTANCE.loadFromResource(resource).getNode();
        Set<String> ids = new HashSet<>();
        List<String> duplicates = new ArrayList<>();
        collectResourceIds(merged, ids, duplicates);
        if (!duplicates.isEmpty()) {
            failures.add(shortName + ": 合并后域模型存在重复 resource id: " + duplicates);
        }
        for (String id : ids) {
            if (id.startsWith("test-orm-erp-" + shortName)) {
                failures.add(shortName + ": 合并后域模型泄漏生成层测试菜单资源: " + id);
            }
        }
        return failures;
    }

    private int countGeneratedFnptResources(String path) {
        IResource resource = VirtualFileSystem.instance().getResource(path);
        assertTrue(resource.exists(), "生成层文件不在 VFS: " + path);
        XNode node = DslNodeLoader.INSTANCE.loadFromResource(resource).getNode();
        return countFnpt(node);
    }

    private int countFnpt(XNode node) {
        if (node == null) {
            return 0;
        }
        int count = 0;
        if ("resource".equals(node.getTagName())
                && "FNPT".equals(node.attrText("resourceType"))) {
            count++;
        }
        if (node.hasChild()) {
            for (XNode child : node.getChildren()) {
                count += countFnpt(child);
            }
        }
        return count;
    }

    private String readResource(String path) {
        IResource resource = VirtualFileSystem.instance().getResource(path);
        assertTrue(resource.exists(), "资源不在 VFS: " + path);
        try (java.io.InputStream in = resource.getInputStream()) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("读取资源失败: " + path, e);
        }
    }

    private void collectResourceIds(XNode node, Set<String> ids, List<String> duplicates) {
        if (node == null) {
            return;
        }
        if ("resource".equals(node.getTagName())) {
            String id = node.attrText("id");
            if (id != null && !id.isEmpty() && !ids.add(id)) {
                duplicates.add(id);
            }
        }
        if (node.hasChild()) {
            for (XNode child : node.getChildren()) {
                collectResourceIds(child, ids, duplicates);
            }
        }
    }

    @Test
    public void testExtensionDomainCountMatchesApp001Scope() {
        assertEquals(8, EXTENSION_DOMAINS.length, "app-001-r3 范围 = 八扩展域");
        assertFalse(EXTENSION_DOMAINS[0][0].isEmpty());
    }
}
