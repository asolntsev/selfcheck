package selfcheck;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class TestSetup implements BeforeAllCallback {
  @Override
  public void beforeAll(ExtensionContext context) {
    System.setProperty("webdriver.http.factory", "jdk-http-client");
    System.setProperty("selenide.fileDownload", "FOLDER");
    System.setProperty("selenide.webdriverLogsEnabled", "true");
  }
}
