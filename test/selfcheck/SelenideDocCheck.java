package selfcheck;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;
import static java.lang.System.lineSeparator;
import static java.net.http.HttpClient.Redirect.ALWAYS;
import static java.net.http.HttpRequest.BodyPublishers.noBody;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static java.util.concurrent.ConcurrentHashMap.newKeySet;
import static org.apache.hc.core5.http.HttpStatus.SC_BAD_GATEWAY;
import static org.apache.hc.core5.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.hc.core5.http.HttpStatus.SC_METHOD_NOT_ALLOWED;
import static org.apache.hc.core5.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.hc.core5.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.hc.core5.http.HttpStatus.SC_OK;
import static org.apache.hc.core5.http.HttpStatus.SC_SERVER_ERROR;
import static org.apache.hc.core5.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.apache.hc.core5.http.HttpStatus.SC_UNAUTHORIZED;
import static org.assertj.core.api.Assertions.fail;

public class SelenideDocCheck {
  private static final Logger log = LoggerFactory.getLogger(SelenideDocCheck.class);
  private static final int SC_I_AM_A_TEAPOT = 418;
  private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/101.0.4951.41 Safari/537.36";

  private final Set<Link> urlsToCheck = newKeySet();
  private final Set<String> brokenLinks = new HashSet<>();
  private final HttpClient client = HttpClient.newBuilder()
    .connectTimeout(ofSeconds(20))
    .followRedirects(ALWAYS)
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

  private static final Set<String> forbiddenRussianLinks = new HashSet<>(asList(
    "ubrr.ru",
    "alfabank.ru",
    "rencredit.ru",
    "severstal.com",
    "dpi-solutions-ltd-",
    "bspb.ru",
    "www.walletone.com",
    "bellintegrator.ru",
    "mir-platform.ru",
    "rtlabs.ru",
    "sportmaster.ru",
    "greenatom.ru",
    "tele2.ru",
    "raiffeisen.rucv",
    "raiffeisen.ru",
    "infotech.group",
    "team.cft.ru",
    "open.ru",
    "mts.ru",
    "outlines.tech",
    "savvymatics.com",
    "career.luxoft.com",
    "sportradar.com",
    "gmdp.ru"
  ));

  private boolean isForbiddenLink(String url) {
    return forbiddenRussianLinks.stream().anyMatch(link -> url.contains(link));
  }

  private boolean canIgnore(String url) {
    return url.startsWith("https://www.accenture.com")
      || url.startsWith("https://www.blueberrycs.com")
      || url.startsWith("https://secureoffice.com")
      || url.startsWith("https://www.bellintegrator.com/");
  }

  private enum HttpMethod {HEAD, GET}

  private record Link(String href, HttpMethod method) {
  }

  @Test
  public void checkAllLinks() {
    long start = System.currentTimeMillis();
    collectLinks();
    long middle = System.currentTimeMillis();
    checkLinks();
    long end = System.currentTimeMillis();

    log.info("Collected {} links in {} ms.", urlsToCheck.size(), middle - start);
    log.info("Checked {} links in {} ms.", urlsToCheck.size(), end - middle);
  }

  private void collectLinks() {
    for (String page : urls) {
      log.info("Checking links on {} ...", page);
      open("about:blank");
      $$("a").shouldHave(size(0));

      open(page);
      ElementsCollection links = $$(".head a:not([href*=\"disqus\"]), .main a:not([href*=\"disqus\"])");
      links.shouldHave(sizeGreaterThan(5));

      log.info("Collecting links on {}: {}", page, links.texts());

      for (SelenideElement link : links) {
        String href = link.attr("href");
        if (href == null || href.startsWith("mailto:") || href.contains("://staging-server.com")) continue;
        if (isForbiddenLink(href)) continue;
        if (canIgnore(href)) continue;
        urlsToCheck.add(new Link(href, HttpMethod.HEAD));
      }
      log.info("All links on {} are collected", page);
    }
  }

  private void checkLinks() {
    for (Link link : urlsToCheck) {
      checkLink(link);
    }
    log.info("Checked {} links", urlsToCheck.size());
    if (!brokenLinks.isEmpty()) {
      fail("Found broken links: " + brokenLinks.stream().collect(Collectors.joining(lineSeparator())));
    }
  }

  @Nonnull
  @CheckReturnValue
  private HttpResponse<String> executeHttpRequest(Link link) throws InterruptedException, URISyntaxException, IOException {
    HttpRequest request = HttpRequest.newBuilder()
      .method(link.method.name(), noBody())
      .uri(new URI(link.href))
      .setHeader("user-agent", USER_AGENT)
      .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private void checkLink(Link link) {
    try {
      HttpResponse<String> r = executeHttpRequest(link);
      log.info("  Checked {} {} -> {}", link.method, link.href, r.statusCode() + " " + r.body());

      if (link.method() == HttpMethod.HEAD && isOKHeadError(r.statusCode())) {
        urlsToCheck.add(new Link(link.href, HttpMethod.GET));
      }
      else if (!isOK(r.statusCode())) {
        brokenLinks.add(link.method + " " + link.href + " -> " + r.statusCode());
      }
    }
    catch (javax.net.ssl.SSLHandshakeException unableToFindValidCertificationPath) {
      log.info("  Checked {} {} -> {}", link.method, link.href, unableToFindValidCertificationPath.toString());
    }
    catch (IOException | URISyntaxException | InterruptedException connectivityIssue) {
      log.info("  Checked {} {} -> {}", link.method, link.href, connectivityIssue.toString());
      brokenLinks.add(link.method + " " + link.href + " -> " + connectivityIssue);
    }
  }

  private boolean isOKHeadError(int statusCode) {
    return statusCode == SC_METHOD_NOT_ALLOWED || statusCode == SC_SERVICE_UNAVAILABLE || statusCode == SC_FORBIDDEN || statusCode == SC_NOT_FOUND;
  }

  private boolean isOK(int statusCode) {
    return statusCode == SC_OK || statusCode == SC_NO_CONTENT || statusCode == SC_I_AM_A_TEAPOT
      || statusCode == SC_SERVER_ERROR || statusCode == SC_BAD_GATEWAY || statusCode == SC_FORBIDDEN || statusCode == SC_UNAUTHORIZED;
  }
}
