package selfcheck;

import org.junit.Test;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

public class SelenideJenkins {
  @Test
  public void checkCI() {
    open("https://ci.selenide.org");
    checkJob("selenide");
    checkJob("examples-cucumber");
    checkJob("examples-cucumber-chrome");
    checkJob("geoip");
    checkJob("xls-test");
    checkJob("selenide-appium");
    checkJob("selenide-tests-htmlunit");
  }

  private void checkJob(final String jobName) {
    $("#job_" + jobName)
        .shouldBe(visible)
        .shouldHave(text(jobName), cssClass("job-status-blue"));
  }
}
