package com.edurite.ai.university;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MultiUniversityPageFetcherServiceTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void discoversOnlyValidatedInternalLinksWithoutBlindGuessing() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", this::rootPage);
        server.createContext("/programmes", exchange -> respond(exchange, 200, "<html><title>Programmes</title><body>degree entry requirements</body></html>"));
        server.createContext("/admissions", exchange -> respond(exchange, 200, "<html><title>Admissions</title><body>admission requirements apply here</body></html>"));
        server.start();

        int port = server.getAddress().getPort();
        MultiUniversityPageFetcherService service = buildService(port);
        UniversitySourceDefinition definition = buildDefinition(port);

        List<String> candidates = service.discoverCandidateUrls(definition, 10);

        assertThat(candidates).containsExactly(
                "http://localhost:" + port + "/",
                "http://localhost:" + port + "/programmes",
                "http://localhost:" + port + "/admissions"
        );
        assertThat(candidates).doesNotContain("http://localhost:" + port + "/courses");
        assertThat(candidates).doesNotContain("https://external.example.com/programmes");
    }

    @Test
    void classifiesProtectedAndThinPages() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/login", exchange -> respond(exchange, 200, "<html><title>Student Login</title><body>sign in with password to continue</body></html>"));
        server.createContext("/thin", exchange -> respond(exchange, 200, "<html><title>Welcome</title><body>Hello</body></html>"));
        server.start();

        int port = server.getAddress().getPort();
        MultiUniversityPageFetcherService service = buildService(port);

        List<UniversitySourcePageResult> results = service.fetchPages(List.of(
                "http://localhost:" + port + "/login",
                "http://localhost:" + port + "/thin"
        ));

        assertThat(results).extracting(UniversitySourcePageResult::failureType)
                .containsExactly(UniversityCrawlFailureType.PROTECTED, UniversityCrawlFailureType.THIN_CONTENT);
    }

    @Test
    void retriesTransientFailuresWithBackoffUntilSuccess() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/flaky", exchange -> {
            if (attempts.incrementAndGet() < 3) {
                respond(exchange, 503, "temporary failure");
                return;
            }
            respond(exchange, 200, "<html><title>Programmes</title><body>programme degree entry requirements aps undergraduate faculty</body></html>");
        });
        server.start();

        int port = server.getAddress().getPort();
        MultiUniversityPageFetcherService service = buildService(port);

        var results = service.fetchPages(List.of("http://localhost:" + port + "/flaky"));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).success()).isTrue();
        assertThat(attempts.get()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void maps403And404ResponsesToStructuredFailures() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/forbidden", exchange -> respond(exchange, 403, "forbidden"));
        server.createContext("/missing", exchange -> respond(exchange, 404, "missing"));
        server.start();

        int port = server.getAddress().getPort();
        MultiUniversityPageFetcherService service = buildService(port);

        var results = service.fetchPages(List.of(
                "http://localhost:" + port + "/forbidden",
                "http://localhost:" + port + "/missing"
        ));

        assertThat(results).extracting(UniversitySourcePageResult::failureType)
                .containsExactly(UniversityCrawlFailureType.FORBIDDEN, UniversityCrawlFailureType.NOT_FOUND);
    }

    private void rootPage(HttpExchange exchange) throws IOException {
        String body = """
                <html>
                  <body>
                    <a href=\"/programmes\">Undergraduate programmes and degrees</a>
                    <a href=\"/admissions\">Admissions</a>
                    <a href=\"/portal\">Student portal</a>
                    <a href=\"https://external.example.com/programmes\">External</a>
                  </body>
                </html>
                """;
        respond(exchange, 200, body);
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, data.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(data);
        }
    }

    private MultiUniversityPageFetcherService buildService(int port) {
        UniversityRegistryProperties properties = buildProperties(port);
        UniversitySourceRegistryService registryService = new UniversitySourceRegistryService(properties, new UniversityUrlNormalizer());
        return new MultiUniversityPageFetcherService(registryService, new UniversityPageClassifier(), new UniversityUrlNormalizer(), properties);
    }

    private UniversitySourceDefinition buildDefinition(int port) {
        return new UniversitySourceRegistryService(buildProperties(port), new UniversityUrlNormalizer()).getActiveDefinitions().get(0);
    }

    private UniversityRegistryProperties buildProperties(int port) {
        UniversityRegistryProperties properties = new UniversityRegistryProperties();
        properties.getCrawl().setMaxDiscoveredCandidatesPerUniversity(10);
        properties.getCrawl().setMaxFetchedPagesPerUniversity(10);
        properties.getCrawl().setMaxSuccessfulPagesPerUniversity(10);
        properties.getCrawl().setMaxFailedPagesPerUniversity(10);
        properties.getCrawl().setMaxCandidateLinksPerPage(10);
        properties.getCrawl().setMaxFetchRetries(3);
        properties.getCrawl().setPoliteDelayMs(0);
        UniversityRegistryProperties.UniversityRegistryEntry entry = new UniversityRegistryProperties.UniversityRegistryEntry();
        entry.setUniversityName("University A");
        entry.setBaseDomain("localhost");
        entry.setAllowedDomains(List.of("localhost"));
        entry.setOfficialHomepages(List.of("http://localhost:" + port + "/"));
        entry.setDiscoveryPages(List.of("http://localhost:" + port + "/"));
        entry.setBlockedPatterns(List.of("portal", "login"));
        entry.setMaxPagesToFetch(10);
        properties.getRegistry().add(entry);
        return properties;
    }
}
