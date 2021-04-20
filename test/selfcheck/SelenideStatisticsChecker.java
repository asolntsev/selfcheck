package selfcheck;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;

import java.io.IOException;
import java.util.regex.Pattern;

import static org.apache.hc.core5.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.hc.core5.http.HttpStatus.SC_OK;

public class SelenideStatisticsChecker {
  private static final Pattern REGEX1 = Pattern.compile("NXSESSIONID=(.+?);.*");
  private static final String PROJECT_SELENIDE = "186c4c63cde8c";
  private static final String GROUP_ID = "com.codeborne";
  private static final String ARTIFACT_ID = "selenide";
  private final HttpClient client = HttpClientBuilder.create().build();

  public static void main(String[] args) throws IOException {
    new SelenideStatisticsChecker().check();
  }

  private void check() throws IOException {
    String sessionId = login();
    String stats = statistics(sessionId);
  }

  private String login() throws IOException {
    String url = "https://oss.sonatype.org/service/local/authentication/login?_dc=" + System.currentTimeMillis();
    HttpGet get = new HttpGet(url);
    get.addHeader("Authorization", "Basic YXNvbG50c2V2OmMwZGViMHJuZSFvc3M=");
    HttpResponse response = client.execute(get);
    int statusCode = response.getCode();
    System.out.println(statusCode);
    if (statusCode != SC_OK && statusCode != SC_NO_CONTENT) {
      throw new RuntimeException("Error login response: " + statusCode);
    }

    Header[] headers = response.getHeaders("Set-Cookie");
    String sessionId = REGEX1.matcher(headers[0].getValue()).replaceFirst("$1");
    System.out.println("sessionId=" + sessionId);
    return sessionId;
  }

  private String statistics(String sessionId) throws IOException {
    String url = "https://oss.sonatype.org/service/local/stats/timeline?p=" + PROJECT_SELENIDE +
        "&g=" + GROUP_ID + "&a=" + ARTIFACT_ID +
        "&t=raw&from=201911&nom=12&_dc=" + System.currentTimeMillis();
    HttpGet get = new HttpGet(url);
    get.addHeader("Set-Cookie", "NXSESSIONID=" + sessionId);

    HttpClientResponseHandler<String> handler = new BasicHttpClientResponseHandler();
    String json = client.execute(get, handler);
    System.out.println("json=" + json);

    return json;
  }
}
