package selfcheck;

import com.codeborne.selenide.ElementsCollection;
import org.openqa.selenium.support.FindBy;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$$;

public class SelenideUsersPage {
  final ElementsCollection users = $$("#selenide-users .user").filter(visible);

  @FindBy(css = "#user-tags .tag")
  private ElementsCollection tags;

  public void filterByTag(String tag) {
    tags.findBy(text(tag)).click();
  }
}
