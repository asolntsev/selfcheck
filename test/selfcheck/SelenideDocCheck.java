package selfcheck;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.apache.hc.core5.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.hc.core5.http.HttpStatus.SC_METHOD_NOT_ALLOWED;
import static org.apache.hc.core5.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.hc.core5.http.HttpStatus.SC_OK;
import static org.apache.hc.core5.http.HttpStatus.SC_SERVER_ERROR;
import static org.apache.hc.core5.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith({LogTestNameExtension.class})
public class SelenideDocCheck {
  private static final Logger log = LoggerFactory.getLogger(SelenideDocCheck.class);
  private static final int SC_I_AM_A_TEAPOT = 418;
  private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/101.0.4951.41 Safari/537.36";

  private final Set<String> checked = new HashSet<>(3000);
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
    "open.ru"
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

  @Test
  public void checkAllLinks() {
    for (String page : urls) {
      log.info("Checking links on {} ...", page);
      open("about:blank");
      $$("a").shouldHave(size(0));

      open(page);
      ElementsCollection links = $$(".head a:not([href*=\"disqus\"]), .main a:not([href*=\"disqus\"])");
      links.shouldHave(sizeGreaterThan(5));

      log.info("Checking links on {}: {}", page, links.texts());

      for (SelenideElement link : links) {
        String href = link.attr("href");
        if (href == null || href.startsWith("mailto:") || href.contains("://staging-server.com")) continue;
        if (isForbiddenLink(href)) continue;
        if (canIgnore(href)) continue;

        log.info("  Checking {} [{}] ... ", href, link.text());
        if (checked.contains(href)) {
          continue;
        }
        try {
          int statusCode = checkLink(href);
          log.info("  Checked {} [{}] -> {}", href, link.text(), statusCode);
        }
        catch (URISyntaxException | InterruptedException e) {
          brokenLinks.add(href + " -> " + e);
        }
      }
      log.info("All links on {} are checked", page);
    }
    if (!brokenLinks.isEmpty()) {
      fail("Found broken links: " + brokenLinks.stream().collect(Collectors.joining(lineSeparator())));
    }
  }

  private int executeHttpRequest(String method, String uri) throws InterruptedException, URISyntaxException {
    HttpRequest request = HttpRequest.newBuilder()
      .method(method, noBody())
      .uri(new URI(uri))
      .setHeader("user-agent", USER_AGENT)
      .build();
    try {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      return response.statusCode();
    }
    catch (IOException connectionTimeout) {
      return -1;
    }
  }

  private int checkLink(String href) throws InterruptedException, URISyntaxException {
    int statusCode = executeHttpRequest("HEAD", href);
    if (statusCode == SC_METHOD_NOT_ALLOWED || statusCode == SC_SERVICE_UNAVAILABLE) {
      statusCode = executeHttpRequest("GET", href);
    }
    if (isOK(href, statusCode)) {
      checked.add(href);
    }
    else {
      brokenLinks.add(href + " -> " + statusCode);
    }
    return statusCode;
  }

  private boolean isOK(String href, int statusCode) {
    return statusCode == SC_OK || statusCode == SC_NO_CONTENT || statusCode == SC_I_AM_A_TEAPOT ||
      (href.startsWith("https://vimeo.com") && statusCode == SC_FORBIDDEN) ||
      (href.startsWith("https://luminor.ee") && statusCode == SC_SERVER_ERROR);
  }
}
