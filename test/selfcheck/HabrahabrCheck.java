package selfcheck;

import org.junit.Test;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.*;

public class HabrahabrCheck {
  @Test
  public void selenideArticle() {
    open("http://habrahabr.ru/post/143269/");
    $(".post_title").shouldHave(text("Selenide: удобные тесты на Selenium WebDriver"));
    $(By.linkText("Selenium WebDriver")).shouldHave(attribute("href", "http://code.google.com/p/selenium/"));
    $(By.linkText("Selenide"), 0).shouldHave(attribute("href", "http://selenide.org/"));
    $(By.linkText("Selenide"), 1).shouldHave(attribute("href", "http://selenide.org/"));
    $(By.linkText("в нашей фирме")).shouldHave(attribute("href", "http://ru.codeborne.com/"));
    $(By.linkText("игра Виселица")).shouldHave(attribute("href", "https://github.com/asolntsev/hangman"));
  }
}
