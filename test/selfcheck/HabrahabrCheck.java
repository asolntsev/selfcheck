package selfcheck;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Condition.href;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

public class HabrahabrCheck {
  @Test
  public void selenideArticle() {
    open("https://habrahabr.ru/post/143269/");
    $(".tm-title").shouldHave(text("Selenide: удобные тесты на Selenium WebDriver"));
    $(By.linkText("Selenium WebDriver")).shouldHave(href("//code.google.com/p/selenium/"));
    $(By.linkText("Selenide"), 0).shouldHave(href("//ru.selenide.org/"));
    $(By.linkText("Selenide"), 1).shouldHave(href("//ru.selenide.org/"));
    $(By.linkText("в нашей фирме")).shouldHave(href("//codeborne.com/ru/"));
    $(By.linkText("игра Виселица")).shouldHave(href("//github.com/selenide-examples/hangman"));
  }
}
