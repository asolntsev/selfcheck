package selfcheck;

import com.codeborne.selenide.junit.ScreenShooter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static com.codeborne.selenide.junit.ScreenShooter.failedTests;

public class SelenideOrgCheck {
  private static final String LAST_SELENIDE_VERSION = "2.5";

  @Rule
  public ScreenShooter screenShooter = failedTests();

  @Before
  public void openPage() {
    open("http://selenide.org");
  }

  @Test
  public void selenideOrg() {
    $(".main-menu-pages li").shouldHave(text("Quick start"));
    $(".short").shouldHave(text("What is Selenide?"));
  }

  @Test
  public void checkSelenideJarLink() {
    // TODO Try to click link and download jar
    $(By.linkText("selenide.jar")).shouldHave(
        attribute("href", "http://search.maven.org/remotecontent?filepath=" +
            "com/codeborne/selenide/"+LAST_SELENIDE_VERSION+"/selenide-"+LAST_SELENIDE_VERSION+".jar"));
  }

  @Test
  public void checkQuickGuideLink() {
    $(By.linkText("Quick start")).click();
    $("body").find(byText("Quick start")).shouldBe(visible);
    checkSelenideJarLink();
    $("code", 1).shouldHave(text("<dependency org=\"com.codeborne\" name=\"selenide\" rev=\""+LAST_SELENIDE_VERSION+"\"/>"));

    $(By.linkText("Selenide examples"))
      .shouldHave(attribute("href", "https://github.com/codeborne/selenide_examples"))
      .click();
    $("article").shouldHave(text("Selenide examples")).shouldBe(visible);
    getWebDriver().navigate().back();

    $(By.linkText("Hangman game"))
      .shouldHave(attribute("href", "https://github.com/asolntsev/hangman/blob/master/test/uitest/selenide/HangmanSpec.java"))
      .click();
    $(".final-path").shouldHave(text("HangmanSpec.java"));
    $(".highlight").shouldHave(text("public class HangmanSpec"));
  }

  @Test
  public void checkSeleniumWebDriverLink() {
    $(By.linkText("Selenium WebDriver")).shouldHave(attribute("href", "http://docs.seleniumhq.org/projects/webdriver/"));
//    $(By.linkText("Selenium WebDriver")).click();
//    $("h1").shouldHave(text("Selenium WebDriver"));
//    getWebDriver().navigate().back();
  }
}
