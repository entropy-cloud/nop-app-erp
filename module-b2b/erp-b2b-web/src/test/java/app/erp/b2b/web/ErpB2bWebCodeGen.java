
package app.erp.b2b.web;

import io.nop.api.core.config.AppConfig;
import io.nop.codegen.XCodeGenerator;
import io.nop.commons.util.MavenDirHelper;
import io.nop.core.CoreConfigs;
import io.nop.core.CoreConstants;
import io.nop.core.initialize.CoreInitialization;

import java.io.File;

public class ErpB2bWebCodeGen {
    public static void main(String[] args) {
        AppConfig.getConfigProvider().updateConfigValue(CoreConfigs.CFG_CORE_MAX_INITIALIZE_LEVEL,
                CoreConstants.INITIALIZER_PRIORITY_ANALYZE);

        CoreInitialization.initialize();
        try {
            File projectDir = MavenDirHelper.projectDir(ErpB2bWebCodeGen.class);
            XCodeGenerator.runPostcompile(new File(projectDir, "../erp-b2b-codegen"), "/", false);
            XCodeGenerator.runPrecompile(new File(projectDir, "../erp-b2b-meta"), "/", false);
            XCodeGenerator.runPostcompile(new File(projectDir, "../erp-b2b-meta"), "/", false);
            XCodeGenerator.runPrecompile(projectDir, "/", false);
            XCodeGenerator.runPrecompile2(projectDir, "/", false);
        } finally {
            CoreInitialization.destroy();
        }
    }
}
