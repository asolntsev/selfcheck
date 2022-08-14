package selfcheck;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

class PrintClasspathExtension implements BeforeAllCallback {
  private static final Logger log = LoggerFactory.getLogger(PrintClasspathExtension.class);
  private static final AtomicBoolean alreadyPrinted = new AtomicBoolean(false);

  @Override
  public void beforeAll(ExtensionContext context) {
    if (!alreadyPrinted.get()) {
      String lineSeparator = System.getProperty("line.separator");
      log.info("Classpath: {}{}", lineSeparator, System.getProperty("java.class.path").replace(":", lineSeparator));
      alreadyPrinted.set(true);
    }
  }
}
