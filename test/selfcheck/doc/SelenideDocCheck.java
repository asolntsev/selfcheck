package selfcheck.doc;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import static com.codeborne.selenide.Selenide.sleep;
import static java.lang.System.lineSeparator;
import static java.net.http.HttpClient.Redirect.ALWAYS;
import static java.net.http.HttpClient.Version.HTTP_1_1;
import static java.net.http.HttpRequest.BodyPublishers.noBody;
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.ConcurrentHashMap.newKeySet;
import static java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static selfcheck.doc.Http.USER_AGENT;
import static selfcheck.doc.Http.isOK;
import static selfcheck.doc.Http.isOKHeadError;
import static selfcheck.doc.Threads.safely;

public class SelenideDocCheck {
  private static final Logger log = LoggerFactory.getLogger(SelenideDocCheck.class);

  private final ExecutorService startRequestsThread = newVirtualThreadPerTaskExecutor();
  private final ExecutorService executeRequestsThread = newVirtualThreadPerTaskExecutor();
  private final ExecutorService handleResponseThread = newVirtualThreadPerTaskExecutor();

  private final Set<Link> urlsToCheck = newKeySet();
  private final Queue<Link> unprocessedLinks = new ConcurrentLinkedQueue<>();
  private final Queue<RunningRequest> runningRequests = new ConcurrentLinkedQueue<>();
  private final Set<String> brokenLinks = new HashSet<>();
  private final Set<String> okLinks = new HashSet<>();
  private final AtomicLong responsesCount = new AtomicLong();
  private final HttpClient client = HttpClient.newBuilder()
    .version(HTTP_1_1)
    .connectTimeout(ofSeconds(20))
    .followRedirects(ALWAYS)
    .executor(executeRequestsThread)
    .build();

  private static final List<String> urls = List.of(
    "https://ru.selenide.org/quick-start.html",
    "https://ru.selenide.org/documentation/screenshots.html",
    "https://ru.selenide.org/documentation/selenide-vs-selenium.html",
    "https://ru.selenide.org/blog.html",
    "https://ru.selenide.org/javadoc.html",
    "https://ru.selenide.org/users.html",
    "https://ru.selenide.org/documentation.html",
    "https://selenide.org/quick-start.html",
    "https://selenide.org/documentation/screenshots.html",
    "https://selenide.org/documentation/selenide-vs-selenium.html",
    "https://selenide.org/blog.html",
    "https://selenide.org/javadoc.html",
    "https://selenide.org/users.html",
    "https://selenide.org/documentation.html"
  );

  @Test
  public void checkAllLinks() throws InterruptedException, IOException {
    long start = System.currentTimeMillis();
    collectLinks(urls);
    long middle = System.currentTimeMillis();
    log.info("Collected {} links in {} ms.", urlsToCheck.size(), middle - start);

    startRequestsThread.submit(safely("start requests", this::checkLinks));
    handleResponseThread.submit(safely("handle responses", this::handleResponses));
    handleResponseThread.shutdown();
    assertThat(handleResponseThread.awaitTermination(2, MINUTES)).isTrue();

    log.info("Checked {} links", urlsToCheck.size());
    if (!brokenLinks.isEmpty()) {
      fail("Found broken links: " + brokenLinks.stream().collect(joining(lineSeparator())));
    }

    long end = System.currentTimeMillis();

    log.info("Checked {} links in {} ms (received responses: {}, ok links: {})", urlsToCheck.size(), end - middle, responsesCount, okLinks.size());
    log.debug("ok links: {}", String.join("\n", okLinks));
  }

  private void collectLinks(List<String> urls) throws IOException {
    for (String page : urls) {
      String linksSelector = ".head a, .main a:not(.external-link)";
      Document doc = Jsoup.connect(page).get();
      Elements links = doc.select(linksSelector);
      List<String> hrefsJs = links.stream()
        .map(a -> a.attr("href"))
        .filter(href -> !href.isEmpty())
        .filter(href -> !href.startsWith("#"))
        .filter(href -> !href.startsWith("mailto:"))
        .filter(href -> !href.contains("://staging-server.com"))
        .map(href -> toAbsoluteUrl(page, href))
        .toList();
      assertThat(hrefsJs).as("Page %s should have some links", page).hasSizeGreaterThan(5);

      for (String href : hrefsJs) {
        addLink(href, HttpMethod.HEAD);
      }
      log.info("{} links from {} got collected", hrefsJs.size(), page);
    }
  }

  String toAbsoluteUrl(String baseUrl, String href) {
    String path = href.contains("#") ? href.substring(0, href.indexOf('#')) : href;
    return path.startsWith("/") ? baseUrl.replaceFirst("(.+://[^/]+)/.*", "$1") + path : path;
  }

  @Test
  void convertsHrefAttributeToUrl() {
    assertThat(toAbsoluteUrl("https://selenide.org/documentation.html", "/faq.html"))
      .isEqualTo("https://selenide.org/faq.html");
    assertThat(toAbsoluteUrl("https://ru.selenide.org/documentation.html", "/users.html"))
      .isEqualTo("https://ru.selenide.org/users.html");
    assertThat(toAbsoluteUrl("https://ru.selenide.org/documentation/screenshots.html", "/users.html"))
      .isEqualTo("https://ru.selenide.org/users.html");
    assertThat(toAbsoluteUrl("", "https://selenide.org/javadoc/com/codeborne/Selenide.html#by(java.lang.String)"))
      .isEqualTo("https://selenide.org/javadoc/com/codeborne/Selenide.html");
  }

  private void checkLinks() {
    log.info("Start checking {} links ({} in queue)", urlsToCheck.size(), unprocessedLinks.size());
    while (hasWork()) {
      while (!unprocessedLinks.isEmpty()) {
        Link link = unprocessedLinks.remove();
        checkLink(link);
      }
      sleep(100);
    }
    log.info("Started http requests for {} links ({} in queue)", urlsToCheck.size(), unprocessedLinks.size());
  }

  private CompletableFuture<HttpResponse<String>> executeHttpRequest(Link link) throws URISyntaxException {
    HttpRequest request = HttpRequest.newBuilder()
      .method(link.method().name(), noBody())
      .uri(new URI(link.href()))
      .setHeader("user-agent", USER_AGENT)
      .timeout(ofSeconds(18))
      .build();

    return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
  }

  private void checkLink(Link link) {
    try {
      runningRequests.add(new RunningRequest(link, executeHttpRequest(link)));
    }
    catch (URISyntaxException | IllegalArgumentException invalidUrl) {
      log.info("  Checked {} {} -> {}", link.method(), link.href(), invalidUrl.toString());
      brokenLinks.add(link.method() + " " + link.href() + " -> " + invalidUrl);
    }
  }

  private void handleResponses() {
    log.info("Handle http responses...");
    int counter = 0;
    while (hasWork()) {
      while (!runningRequests.isEmpty()) {
        counter++;
        handleResponse(runningRequests.remove());
      }
      sleep(100);
    }
    log.info("Handled http responses for {} links", counter);
  }

  private void handleResponse(RunningRequest runningRequest) {
    Link link = runningRequest.link();
    responsesCount.incrementAndGet();
    try {
      HttpResponse<String> r = runningRequest.response().get(20, SECONDS);
      log.info("  Checked {} {} -> {}", link.method(), link.href(), r.statusCode() + " " + shorten(r.body()));

      if (link.method() == HttpMethod.HEAD && isOKHeadError(r.statusCode())) {
        addLink(link.href(), HttpMethod.GET);
      }
      else if (!isOK(link.href(), r.statusCode())) {
        brokenLinks.add(link.method() + " " + link.href() + " -> " + r.statusCode());
      }
      else {
        okLinks.add(link.method() + " " + link.href() + " -> " + r.statusCode());
      }
    }
    catch (InterruptedException | ExecutionException | TimeoutException error) {
      if (error.getCause() instanceof javax.net.ssl.SSLHandshakeException) {
        log.info("  Checked {} {} -> {}", link.method(), link.href(), error.getCause().toString());
      }
      log.info("  Checked {} {} -> {}", link.method(), link.href(), error.toString());
      brokenLinks.add(link.method() + " " + link.href() + " -> " + error);
    }
  }

  private String shorten(String html) {
    return substring(html, 200).replaceAll("\\n", "");
  }

  private String substring(String text, int maxLength) {
    return text.length() <= maxLength ? text : text.substring(0, maxLength);
  }

  @Test
  void substring() {
    assertThat(substring("", 0)).isEqualTo("");
    assertThat(substring("", 1)).isEqualTo("");
    assertThat(substring("", 2)).isEqualTo("");
    assertThat(substring("A", 0)).isEqualTo("");
    assertThat(substring("A", 1)).isEqualTo("A");
    assertThat(substring("A", 2)).isEqualTo("A");
    assertThat(substring("AAA", 0)).isEqualTo("");
    assertThat(substring("AAA", 1)).isEqualTo("A");
    assertThat(substring("AAA", 2)).isEqualTo("AA");
    assertThat(substring("AAA", 3)).isEqualTo("AAA");
    assertThat(substring("AAA", 4)).isEqualTo("AAA");
  }

  private void addLink(String href, HttpMethod httpMethod) {
    Link link = new Link(href, httpMethod);
    if (urlsToCheck.add(link)) {
      unprocessedLinks.add(link);
    }
  }

  private boolean hasWork() {
    return !unprocessedLinks.isEmpty() || !runningRequests.isEmpty();
  }
}
