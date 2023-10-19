package selfcheck;

import com.codeborne.selenide.Condition;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static com.codeborne.selenide.ClickOptions.usingJavaScript;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.CollectionCondition.sizeLessThan;
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.Selenide.sleep;
import static com.codeborne.selenide.files.FileFilters.withNameMatching;
import static org.assertj.core.api.Assertions.assertThat;

public class SelenideOrgCheck {
  private static final String LAST_SELENIDE_VERSION = "6.19.1";
  private static final Condition enabledAndVisible = new EnabledAndVisible();

  @Test
  public void selenideOrg() {
    open("https://selenide.org");
    $(".main-menu-pages a").shouldHave(text("Quick start"));
    $(".short.wiki").shouldHave(text("What is Selenide?"));
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

    assertThat(selenideJar.getName()).isEqualTo("selenide-" + LAST_SELENIDE_VERSION + ".jar");
    try (JarFile jarFile = new JarFile(selenideJar)) {
      Enumeration<JarEntry> en = jarFile.entries();
      assertThat(en.hasMoreElements()).as("selenide.jar is empty").isTrue();
    }
  }

  @Test
  public void checkQuickGuideLink() {
    open("https://selenide.org");
    $(By.linkText("Quick start")).click();
    $("body").find(byText("Quick start")).shouldBe(enabledAndVisible);
  }

  @Test
  public void quickGuide_downloadSelenideJar() throws IOException {
    open("https://selenide.org/quick-start.html");

    $("body").find(byText("Quick start")).shouldBe(enabledAndVisible);
    checkSelenideJarLink();
  }

  @Test
  public void quickGuide_gradleDependency() {
    open("https://selenide.org/quick-start.html");

    $("code", 1).shouldHave(text("testImplementation 'com.codeborne:selenide:" + LAST_SELENIDE_VERSION + "'"));
  }

  @Test
  public void quickGuide_mavenDependency() {
    open("https://selenide.org/quick-start.html");

    $("code", 0).shouldHave(text("<dependency>\n" +
        "    <groupId>com.codeborne</groupId>\n" +
        "    <artifactId>selenide</artifactId>\n" +
        "    <version>" + LAST_SELENIDE_VERSION + "</version>\n" +
        "    <scope>test</scope>\n" +
        "</dependency>"));
  }

  @Test
  public void quickGuide_selenideExamples() {
    open("https://selenide.org/quick-start.html");

    $(By.linkText("Selenide examples"))
        .shouldHave(attribute("href", "https://github.com/selenide-examples"))
        .click(usingJavaScript());
    $(".orghead").shouldHave(text("Selenide examples")).shouldBe(enabledAndVisible);
  }

  @Test
  public void quickGuide_hangmanExample() {
    open("https://selenide.org/quick-start.html");

    $(By.linkText("Hangman game"))
      .shouldHave(attribute("href", "https://github.com/selenide-examples/hangman/blob/main/test/uitest/selenide/HangmanSpec.java"))
      .scrollTo()
      .click();
    $("#file-name-id-wide").shouldHave(text("HangmanSpec.java"));
    $("#read-only-cursor-text-area").shouldHave(text("public class HangmanSpec"));
  }

  @Test
  void showsSelenideUsers() {
    SelenideUsersPage page = open("https://selenide.org/users.html", SelenideUsersPage.class);
    page.users.shouldHave(sizeGreaterThan(50));
    page.filterByTag("australia");
    page.users.shouldHave(sizeLessThan(10));
  }
}
