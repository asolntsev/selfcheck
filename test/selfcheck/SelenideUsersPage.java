package selfcheck;

import com.codeborne.selenide.ElementsCollection;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$$;

public class SelenideUsersPage {
  final ElementsCollection users = $$("#selenide-users .user").filter(visible);
  private final ElementsCollection tags = $$("#user-tags .tag");

  public void filterByTag(String tag) {
    tags.findBy(text(tag)).click();
  }
}
