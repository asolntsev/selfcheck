package selfcheck.doc;

import com.codeborne.selenide.Selenide;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
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
import java.util.stream.Collectors;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.Selenide.sleep;
import static java.lang.System.lineSeparator;
import static java.net.http.HttpClient.Redirect.ALWAYS;
import static java.net.http.HttpRequest.BodyPublishers.noBody;
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.ConcurrentHashMap.newKeySet;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.substring;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static selfcheck.doc.Http.USER_AGENT;
import static selfcheck.doc.Http.isForbiddenLink;
import static selfcheck.doc.Http.isOK;
import static selfcheck.doc.Http.isOKHeadError;
import static selfcheck.doc.Threads.safely;

public class SelenideDocCheck {
  private static final Logger log = LoggerFactory.getLogger(SelenideDocCheck.class);

  private final ExecutorService startRequestsThread = newSingleThreadExecutor(new ThreadNamer("start-requests-"));
  private final ExecutorService executeRequestsThread = newFixedThreadPool(50, new ThreadNamer("execute-requests-"));
  private final ExecutorService handleResponseThread = newSingleThreadExecutor(new ThreadNamer("handle-responses-"));

  private final Set<Link> urlsToCheck = newKeySet();
  private final Queue<Link> unprocessedLinks = new ConcurrentLinkedQueue<>();
  private final Queue<RunningRequest> runningRequests = new ConcurrentLinkedQueue<>();
  private final Set<String> brokenLinks = new HashSet<>();
  private final HttpClient client = HttpClient.newBuilder()
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

  private boolean canIgnore(String url) {
    return url.startsWith("https://www.accenture.com")
           || url.startsWith("https://www.blueberrycs.com")
           || url.startsWith("https://secureoffice.com")
           || url.startsWith("https://www.bellintegrator.com/");
  }

  @Test
  public void checkAllLinks() throws InterruptedException {
    long start = System.currentTimeMillis();
    collectLinks(urls);
    long middle = System.currentTimeMillis();
    log.info("Collected {} links in {} ms.", urlsToCheck.size(), middle - start);

    startRequestsThread.submit(safely("start requests", this::checkLinks));
    handleResponseThread.submit(safely("handle responses", this::handleResponses));
    handleResponseThread.shutdown();
    handleResponseThread.awaitTermination(2, MINUTES);

    log.info("Checked {} links", urlsToCheck.size());
    if (!brokenLinks.isEmpty()) {
      fail("Found broken links: " + brokenLinks.stream().collect(Collectors.joining(lineSeparator())));
    }

    long end = System.currentTimeMillis();

    log.info("Checked {} links in {} ms.", urlsToCheck.size(), end - middle);
  }

  private void collectLinks(List<String> urls) {
    for (String page : urls) {
      open("about:blank");
      $$("a").shouldHave(size(0));

      open(page);

      String linksSelector = ".head a:not([href*=\"disqus\"]), .main a:not([href*=\"disqus\"])";
      List<String> hrefsJs = Selenide.executeJavaScript("""
          return Array.from(document.querySelectorAll(arguments[0])).map(link => link.href)
        """, linksSelector);
      assertThat(hrefsJs).as("Page %s should have some links", page).hasSizeGreaterThan(5);

      for (String href : hrefsJs) {
        if (href == null || href.isBlank()) continue;
        if (href.startsWith("mailto:") || href.contains("://staging-server.com")) continue;
        if (isForbiddenLink(href)) continue;
        if (canIgnore(href)) continue;
        addLink(href, HttpMethod.HEAD);
      }
      log.info("{} links from {} got collected", hrefsJs.size(), page);
    }
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

  @Nonnull
  @CheckReturnValue
  private CompletableFuture<HttpResponse<String>> executeHttpRequest(Link link) throws URISyntaxException {
    HttpRequest request = HttpRequest.newBuilder()
      .method(link.method().name(), noBody())
      .uri(new URI(link.href()))
      .setHeader("user-agent", USER_AGENT)
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
    try {
      HttpResponse<String> r = runningRequest.response().get(20, SECONDS);
      log.info("  Checked {} {} -> {}", link.method(), link.href(), r.statusCode() + " " + shorten(r.body()));

      if (link.method() == HttpMethod.HEAD && isOKHeadError(r.statusCode())) {
        addLink(link.href(), HttpMethod.GET);
      }
      else if (!isOK(r.statusCode())) {
        brokenLinks.add(link.method() + " " + link.href() + " -> " + r.statusCode());
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
    return substring(html, 0, 200).replaceAll("\\n", "");
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
