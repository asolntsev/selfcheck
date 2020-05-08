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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;
import static java.lang.System.lineSeparator;
import static java.util.Arrays.asList;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class SelenideDocCheck {
  private final HttpClient client = HttpClientBuilder.create().build();

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
      if (href == null || href.startsWith("mailto:")) continue;

      System.out.print("  Checking " + href + " [" + link.text() + "] ... ");
      HttpResponse response = client.execute(new HttpHead(href));
      int statusCode = response.getStatusLine().getStatusCode();
      System.out.println(statusCode);
      if (statusCode != SC_OK && statusCode != SC_NO_CONTENT) {
        brokenLinks.add(href + " -> " + statusCode);
      }
    }
    System.out.println("All links on " + page + " are checked");
    if (!brokenLinks.isEmpty()) {
      fail("Found broken links: " + brokenLinks.stream().collect(Collectors.joining(lineSeparator())));
    }
  }
}