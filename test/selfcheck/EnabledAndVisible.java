package selfcheck;

import com.codeborne.selenide.CheckResult;
import com.codeborne.selenide.Driver;
import com.codeborne.selenide.WebElementCondition;
import org.openqa.selenium.WebElement;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class EnabledAndVisible extends WebElementCondition {
  public EnabledAndVisible() {
    super("enabled and visible");
  }

  @Nonnull
  @Override
  public CheckResult check(Driver driver, WebElement element) {
    boolean enabled = element.isEnabled();
    boolean displayed = element.isDisplayed();
    return new CheckResult(enabled && displayed, "Enabled: %s, displayed: %s".formatted(element, displayed));
  }
}
