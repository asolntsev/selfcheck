package selfcheck;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;

public class Blog {
  @Before
  public void setUp() {
    open("https://asolntsev.github.io/");
  }

  @Test
  public void canSwitchEnglishAndRussian() {
    $("#lang_rus").shouldHave(text("RUS")).click();
    $$(".sidebar-nav .sidebar-nav-item").shouldHave(texts("Blog", "Обо мне", "Опен-сорс", "Видео"));
    
    $("#lang_eng").shouldHave(text("ENG")).click();
    $$(".sidebar-nav .sidebar-nav-item").shouldHave(texts("Blog", "About", "Open source", "My videos"));
  }
}
