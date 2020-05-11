package selfcheck;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
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
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class SelenideDocCheck {
  private final HttpClient client = HttpClientBuilder.create().build();

  private static final Set<String> checked = new HashSet<>(3000);

  @Parameters
  public static Collection<Object[]> data() {
    return asList(new Object[][]{
        {"https://ru.selenide.org/quick-start.html"},
        {"https://ru.selenide.org/documentation/screenshots.html"},
        {"https://ru.selenide.org/documentation/selenide-vs-selenium.html"},
        {"https://ru.selenide.org/blog.html"},
        {"https://ru.selenide.org/javadoc.html"},
        {"https://ru.selenide.org/users.html"},
        {"https://ru.selenide.org/documentation.html"},
        {"https://selenide.org/quick-start.html"},
        {"https://selenide.org/documentation/screenshots.html"},
        {"https://selenide.org/documentation/selenide-vs-selenium.html"},
        {"https://selenide.org/blog.html"},
        {"https://selenide.org/javadoc.html"},
        {"https://selenide.org/users.html"},
        {"https://selenide.org/documentation.html"}
    });
  }

  private final String page;
  public SelenideDocCheck(String page) {this.page = page;}

  @Test
  public void checkAllLinks() throws IOException {
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

      System.out.print("  Checking " + href + " [" + link.text() + "] ... ");
      if (checked.contains(href)) {
        brokenLinks.add("ok (cached)");
        continue;
      }
      try {
        HttpResponse response = client.execute(new HttpHead(href));
        int statusCode = response.getStatusLine().getStatusCode();
        System.out.println(statusCode);
        if (isOK(href, statusCode)) {
          checked.add(href);
        }
        else {
          brokenLinks.add(href + " -> " + statusCode);
        }
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

  private boolean isOK(String href, int statusCode) {
    return statusCode == SC_OK || statusCode == SC_NO_CONTENT ||
        (href.startsWith("https://vimeo.com") && statusCode == SC_FORBIDDEN);
  }
}