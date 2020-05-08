package selfcheck;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.junit.ScreenShooter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarFile;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.FileDownloadMode.PROXY;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.closeWebDriver;
import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.Selenide.sleep;
import static com.codeborne.selenide.files.FileFilters.withNameMatching;
import static com.codeborne.selenide.junit.ScreenShooter.failedTests;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SelenideOrgCheck {
  private static final String LAST_SELENIDE_VERSION = "5.11.1";

  @Rule
  public ScreenShooter screenShooter = failedTests();

  @Before
  public void openPage() {
    if (Configuration.fileDownload != PROXY) {
      closeWebDriver();
      Configuration.fileDownload = PROXY;
    }
    Configuration.proxyEnabled = true;
  }

  @Test
  public void selenideOrg() {
    open("https://selenide.org");
    $(".main-menu-pages li").shouldHave(text("Quick start"));
    $(".short").shouldHave(text("What is Selenide?"));
  }

  @Test
  public void checkSelenideJarLink() throws IOException {
    open("https://selenide.org");
    sleep(2000); // wait until page gets loaded (with all that disquss and video)
    String expectedHref = String.format("https://search.maven.org/remotecontent?filepath=com/codeborne/selenide/%s/selenide-%s.jar", LAST_SELENIDE_VERSION, LAST_SELENIDE_VERSION);
    File selenideJar = $(By.linkText("selenide.jar"))
        .shouldHave(attribute("href", expectedHref))
        .scrollTo()
        .download(withNameMatching("selenide.*jar"));

    assertEquals("selenide-" + LAST_SELENIDE_VERSION + ".jar", selenideJar.getName());
    JarFile jarFile = new JarFile(selenideJar);
    Enumeration en = jarFile.entries();
    assertTrue("selenide.jar is empty", en.hasMoreElements());
  }

  @Test
  public void checkQuickGuideLink() {
    open("https://selenide.org");
    $(By.linkText("Quick start")).click();
    $("body").find(byText("Quick start")).shouldBe(visible);
  }

  @Test
  public void quickGuide_downloadSelenideJar() throws IOException {
    open("https://selenide.org/quick-start.html");

    $("body").find(byText("Quick start")).shouldBe(visible);
    checkSelenideJarLink();
  }

  @Test
  public void quickGuide_gradleDependency() {
    open("https://selenide.org/quick-start.html");

    $("code", 1).shouldHave(text("<dependency org=\"com.codeborne\" name=\"selenide\" rev=\"" + LAST_SELENIDE_VERSION + "\"/>"));
  }

  @Test
  public void quickGuide_selenideExamples() {
    open("https://selenide.org/quick-start.html");

    $(By.linkText("Selenide examples"))
        .shouldHave(attribute("href", "https://github.com/selenide-examples"))
        .click();
    $(".org-header-wrapper").shouldHave(text("Selenide examples")).shouldBe(visible);
  }

  @Test
  public void quickGuide_hangmanExample() {
    open("https://selenide.org/quick-start.html");

    $(By.linkText("Hangman game"))
      .shouldHave(attribute("href", "https://github.com/selenide-examples/hangman/blob/master/test/uitest/selenide/HangmanSpec.java"))
      .scrollTo()
      .click();
    $(".final-path").shouldHave(text("HangmanSpec.java"));
    $(".highlight").shouldHave(text("public class HangmanSpec"));
  }
}
