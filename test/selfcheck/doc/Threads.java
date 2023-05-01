package selfcheck.doc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Threads {
  private static final Logger log = LoggerFactory.getLogger(Threads.class);

  static Runnable safely(String name, Runnable runnable) {
    return () -> {
      try {
        runnable.run();
      }
      catch (Throwable e) {
        log.error("Failure in {}", name, e);
      }
    };
  }
}
