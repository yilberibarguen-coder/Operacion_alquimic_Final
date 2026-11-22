package application.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.tools.ToolProvider;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DomainIndependenceTest {
    @TempDir Path output;

    @Test
    void entireDomainCompilesWithOnlyTheJdkAndNoFrameworks() throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "Run the tests with a JDK, not a JRE.");
        try (var sources = Files.walk(Path.of("src/main/java/application/domain"));
             var manager = compiler.getStandardFileManager(null, null, null)) {
            var files = sources.filter(path -> path.toString().endsWith(".java")).toList();
            assertFalse(files.isEmpty());
            var diagnostics = new StringWriter();
            var units = manager.getJavaFileObjectsFromPaths(files);
            assertTrue(compiler.getTask(diagnostics, manager, null,
                    List.of("--release", "17", "-classpath", output.toString(), "-d", output.toString()),
                    null, units).call(), diagnostics.toString());
        }
    }
}
