package selfcheck;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;
import static java.lang.System.lineSeparator;
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
  private final HttpClient client = HttpClientBuilder.create().build();

  private static final int SC_I_AM_A_TEAPOT = 418;
  private static final Set<String> checked = new HashSet<>(3000);

  private static String[] urls() {
    return new String[]{
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
    };
  }

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
    "raiffeisen.ru"
  ));

  private boolean isForbiddenLink(String url) {
    return forbiddenRussianLinks.stream().anyMatch(link -> url.contains(link));
  }

  private boolean canIgnore(String url) {
    return "https://www.accenture.com/us-en".equals(url);
  }

  @ParameterizedTest
  @MethodSource("urls")
  public void checkAllLinks(String page) throws IOException {
    System.out.println("Checking links on " + page + " ...");
    open("about:blank");
    $$("a").shouldHave(size(0));

    open(page);
    ElementsCollection links = $$(".head a:not([href*=\"disqus\"]), .main a:not([href*=\"disqus\"])").shouldHave(sizeGreaterThan(5));

    System.out.println("Checking links on " + page + ": " + links.texts());
    List<String> brokenLinks = new ArrayList<>();

    for (SelenideElement link : links) {
      String href = link.attr("href");
      if (href == null || href.startsWith("mailto:") || href.contains("://staging-server.com")) continue;
      if (isForbiddenLink(href)) continue;
      if (canIgnore(href)) continue;

      System.out.print("  Checking " + href + " [" + link.text() + "] ... ");
      if (checked.contains(href)) {
        continue;
      }
      try {
        int statusCode = checkLink(brokenLinks, href);
        System.out.println(statusCode);
      }
      catch (SSLException e) {
        System.out.println("SSL Error");
      }
      catch (UnknownHostException e) {
        brokenLinks.add(href + " -> " + e);
      }
    }
    System.out.println("All links on " + page + " are checked");
    if (!brokenLinks.isEmpty()) {
      fail("Found broken links: " + brokenLinks.stream().collect(Collectors.joining(lineSeparator())));
    }
  }

  private int executeHttpRequest(HttpUriRequestBase request) throws IOException {
    request.setHeader("user-agent", USER_AGENT);
    return client.execute(request).getCode();
  }

  private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/101.0.4951.41 Safari/537.36";

  private int checkLink(List<String> brokenLinks, String href) throws IOException {
    int statusCode = executeHttpRequest(new HttpHead(href));
    if (statusCode == SC_METHOD_NOT_ALLOWED || statusCode == SC_SERVICE_UNAVAILABLE) {
      statusCode = executeHttpRequest(new HttpGet(href));
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