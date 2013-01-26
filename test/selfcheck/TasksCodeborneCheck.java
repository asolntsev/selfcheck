package selfcheck;

import com.codeborne.selenide.junit.ScreenShooter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.DOM.$;
import static com.codeborne.selenide.Navigation.navigateToAbsoluteUrl;
import static com.codeborne.selenide.Selectors.byAttribute;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selectors.withText;
import static com.codeborne.selenide.junit.ScreenShooter.failedTests;

public class TasksCodeborneCheck {
  @Rule
  public ScreenShooter screenShooter = failedTests();

  @Before
  public void openPage() {
    navigateToAbsoluteUrl("https://tasks.codeborne.com/");
  }

  @Test
  public void taskDescription() {
    $("h1").shouldHave(text("Tasks server"));
    $(byText("Statistics")).shouldBe(visible);
    $(byText("Received solutions")).shouldBe(visible);
    $(withText("Your goal")).shouldBe(visible);
    $(withText("to compute total")).shouldBe(visible);

    $(By.linkText("Submit a solution")).shouldBe(visible);
    $(By.linkText("Submit your solution »")).shouldBe(visible);
  }

  @Test
  public void userCanSubmitSolution() {
    $(By.linkText("Submit your solution »")).click();
    $(By.name("author")).type("Chuck Norris");
    $(By.name("authorEmail")).type("gmail@chuck.norris");
    $(By.name("content")).type("BEGIN\n do it!\nEND;");
    $(byAttribute("value", "Submit solution")).shouldBe(visible, enabled);
    $(byText("Cancel")).click();
  }

  @Test
  public void userCanSeeHelp() {
    $(By.linkText("Help")).click();
    $(".content").shouldHave(text("Example of unit-test:"));
  }
}
