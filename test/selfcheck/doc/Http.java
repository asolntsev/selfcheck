package selfcheck.doc;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.apache.hc.core5.http.HttpStatus.SC_BAD_GATEWAY;
import static org.apache.hc.core5.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.hc.core5.http.HttpStatus.SC_METHOD_NOT_ALLOWED;
import static org.apache.hc.core5.http.HttpStatus.SC_MOVED_TEMPORARILY;
import static org.apache.hc.core5.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.hc.core5.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.hc.core5.http.HttpStatus.SC_OK;
import static org.apache.hc.core5.http.HttpStatus.SC_SERVER_ERROR;
import static org.apache.hc.core5.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.apache.hc.core5.http.HttpStatus.SC_UNAUTHORIZED;

class Http {
  static final int SC_I_AM_A_TEAPOT = 418;
  static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/101.0.4951.41 Safari/537.36";

  private static final Set<Integer> okHeadResponses = Set.of(SC_METHOD_NOT_ALLOWED, SC_SERVICE_UNAVAILABLE,
    SC_FORBIDDEN, SC_NOT_FOUND);
  private static final Set<Integer> okStatuses = Set.of(SC_OK, SC_NO_CONTENT, SC_I_AM_A_TEAPOT, SC_SERVER_ERROR,
    SC_BAD_GATEWAY, SC_FORBIDDEN, SC_UNAUTHORIZED);

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

  static boolean isForbiddenLink(String url) {
    return forbiddenRussianLinks.stream().anyMatch(link -> url.contains(link));
  }

  static boolean isOKHeadError(int statusCode) {
    return okHeadResponses.contains(statusCode);
  }

  static boolean isOK(URI url, int statusCode) {
    return okStatuses.contains(statusCode) ||
      statusCode == SC_MOVED_TEMPORARILY && url.toString().startsWith("https://twitter.com/selenide");
  }
}
