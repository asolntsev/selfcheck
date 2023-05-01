package selfcheck.doc;

import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

record RunningRequest(Link link, CompletableFuture<HttpResponse<String>> response) {
}
