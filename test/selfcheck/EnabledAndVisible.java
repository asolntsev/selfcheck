package selfcheck;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Driver;
import org.openqa.selenium.WebElement;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@SuppressWarnings("deprecation")
public class EnabledAndVisible extends Condition {
  public EnabledAndVisible() {
    super("enabled and visible");
  }

  @Override
  public boolean apply(Driver driver, WebElement element) {
    return element.isEnabled() && element.isDisplayed();
  }

  @Nullable
  @Override
  public String actualValue(Driver driver, WebElement element) {
    return "enabled: %s, visible: %s".formatted(element.isEnabled(), element.isDisplayed());
  }
}
