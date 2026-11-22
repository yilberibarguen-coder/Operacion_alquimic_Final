import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/** Runs with Java 17+ on all supported operating systems, without Maven or extra libraries. */
public class VerifyJar {
    public static void main(String[] args) throws Exception {
        if (args.length != 1 && args.length != 3) {
            throw new IllegalArgumentException("Pass the executable JAR path and optional language/country.");
        }
        String language = args.length == 3 ? args[1] : "en";
        String country = args.length == 3 ? args[2] : "US";
        Path jar = Path.of(args[0]).toAbsolutePath();
        if (!Files.isRegularFile(jar)) throw new IllegalArgumentException("JAR not found: " + jar);
        Path javaBin = Path.of(System.getProperty("java.home"), "bin", "java");
        if (!Files.exists(javaBin)) javaBin = javaBin.resolveSibling("java.exe");
        // Spaces in the working directory also exercise portable process argument handling.
        Path directory = Files.createTempDirectory("alquimia smoke ");
        Path log = directory.resolve("startup.log");
        Process process = null;
        boolean success = false;
        try {
            process = new ProcessBuilder(javaBin.toString(), "-Duser.language=" + language, "-Duser.country=" + country,
                    "-jar", jar.toString(), "--server.port=0", "--server.address=127.0.0.1")
                    .directory(directory.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
            var pattern = Pattern.compile("Tomcat started on port (\\d+)");
            long deadline = System.nanoTime() + Duration.ofSeconds(60).toNanos();
            int port = -1;
            while (System.nanoTime() < deadline && process.isAlive()) {
                var match = pattern.matcher(Files.readString(log));
                if (match.find()) { port = Integer.parseInt(match.group(1)); break; }
                Thread.sleep(200);
            }
            if (port < 0) throw new IllegalStateException("API did not start within 60 seconds.");
            var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            String body = """
                    {"devices":[
                      {"deviceId":"star-labs","distance":670.82,"message":["hola",""]},
                      {"deviceId":"ccpd","distance":200,"message":["","mundo"]},
                      {"deviceId":"S.T.A.R. Satellite","distance":400,"message":["",""]}
                    ]}
                    """;
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/analysis"))
                    .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || !response.body().contains("\"message\":\"hola mundo\"")
                    || !response.body().contains("\"x\":100.0") || !response.body().contains("\"y\":100.0")) {
                throw new IllegalStateException("Unexpected API result: " + response.statusCode() + " " + response.body());
            }
            success = true;
            System.out.println("JAR smoke test passed: " + System.getProperty("os.name")
                    + " / Java " + System.getProperty("java.version") + " / locale " + language + "-" + country);
        } finally {
            if (process != null) {
                process.destroy();
                if (!process.waitFor(10, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    process.waitFor(10, TimeUnit.SECONDS);
                }
            }
            if (!success && Files.exists(log)) System.err.println(Files.readString(log));
            cleanup(directory, log);
        }
    }

    private static void cleanup(Path directory, Path log) throws InterruptedException {
        // Windows virus scanners and process handles can retain the redirected log briefly.
        for (int attempt = 0; attempt < 20; attempt++) {
            try {
                Files.deleteIfExists(log);
                Files.deleteIfExists(directory);
                return;
            } catch (java.io.IOException lockedFile) {
                Thread.sleep(100);
            }
        }
        // Cleanup must not turn a successful HTTP check into a compatibility failure.
        directory.toFile().deleteOnExit();
        log.toFile().deleteOnExit();
        System.err.println("Temporary smoke-test files scheduled for cleanup: " + directory);
    }
}
