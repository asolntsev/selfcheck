package selfcheck.doc;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

class Http {
  private static final int SC_OK = 200;
  private static final int SC_NO_CONTENT = 204;
  private static final int SC_MOVED_TEMPORARILY = 302;
  private static final int SC_UNAUTHORIZED = 401;
  private static final int SC_FORBIDDEN = 403;
  private static final int SC_NOT_FOUND = 404;
  private static final int SC_METHOD_NOT_ALLOWED = 405;
  private static final int SC_TOO_MANY_REQUESTS = 429;
  private static final int SC_TOO_MANY_REQUESTS_UNOFFICIAL = 430;
  private static final int SC_SERVER_ERROR = 500;
  private static final int SC_BAD_GATEWAY = 502;
  private static final int SC_SERVICE_UNAVAILABLE = 503;
  private static final int SC_I_AM_A_TEAPOT = 418;
  static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/101.0.4951.41 Safari/537.36";

  private static final Set<Integer> okHeadResponses = Set.of(SC_METHOD_NOT_ALLOWED, SC_SERVICE_UNAVAILABLE,
    SC_FORBIDDEN, SC_NOT_FOUND);
  private static final Set<Integer> okStatuses = Set.of(SC_OK, SC_NO_CONTENT, SC_I_AM_A_TEAPOT, SC_SERVER_ERROR,
    SC_BAD_GATEWAY, SC_FORBIDDEN, SC_UNAUTHORIZED, SC_TOO_MANY_REQUESTS, SC_TOO_MANY_REQUESTS_UNOFFICIAL);

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

  static boolean isOK(String href, int statusCode) {
    return okStatuses.contains(statusCode) ||
      statusCode == SC_MOVED_TEMPORARILY && href.startsWith("https://twitter.com/selenide");
  }
}
