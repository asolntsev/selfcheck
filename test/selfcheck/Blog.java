package selfcheck;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;

@ExtendWith({LogTestNameExtension.class})
public class Blog {
  @BeforeEach
  public void setUp() {
    open("https://asolntsev.github.io/");
  }

  @Test
  public void canSwitchEnglishAndRussian() {
    $("#languages").find(byText("RUS")).click();
    $$(".sidebar-nav .sidebar-nav-item").shouldHave(texts("Blog", "Обо мне", "Опен-сорс", "Публикации", "Видео"));

    $("#languages").find(byText("ENG")).click();
    $$(".sidebar-nav .sidebar-nav-item").shouldHave(texts("Blog", "About", "Open source", "Publications", "My videos"));
  }
}
