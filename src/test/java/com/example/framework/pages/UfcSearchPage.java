package com.example.framework.pages;

import com.example.framework.config.FrameworkConfig;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Page.NavigateOptions;
import com.microsoft.playwright.options.WaitUntilState;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class UfcSearchPage extends BasePage<UfcSearchPage> {

    public static final String FILTER_OLDNEW_TOURNEY = "#yxt-SortOptions-option_SortOptions_2";
    public static final String FILTER_APPLY = "button.yxt-SortOptions-apply:has-text('Применить')";

    public UfcSearchPage(Page page, FrameworkConfig config) {
        super(page, config);
    }

    @Override
    protected UfcSearchPage self() {
        return this;
    }

    @Step("Apply tournaments filter from embedded search card")
    public UfcSearchPage applyFilter() {
        return Allure.step("Apply tournaments filter from embedded search card", () -> {
            Allure.step("Открываем вкладку 'Турниры'", () -> {
                log.info("Открываем вкладку 'Турниры' внутри встроенной карты поиска");
                page.frameLocator("#answers-frame")
                        .locator("a.yxt-Nav-item:has-text('ТУРНИРЫ')")
                        .click();
            });

            String iframeSrc = page.locator("#answers-frame").getAttribute("src");
            if (iframeSrc == null || iframeSrc.isBlank()) {
                throw new IllegalStateException("Iframe src attribute is not available for locator: #answers-frame");
            }

            Allure.step("Переходим во встроенный iframe", () -> {
                log.info("Переходим по адресу iframe {}", iframeSrc);
                page.navigate(iframeSrc, new NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            });

            Allure.step("Выбираем и применяем сортировку турниров", () -> {
                log.info("Выбираем сортировку турниров {}", FILTER_OLDNEW_TOURNEY);
                page.click(FILTER_OLDNEW_TOURNEY);
                log.info("Подтверждаем фильтр кнопкой {}", FILTER_APPLY);
                page.click(FILTER_APPLY);
            });

            checkFilter();
            return this;
        });
    }

    private void checkFilter() {
        Allure.step("Проверяем сортировку дат на карточках турниров", () -> {
            Locator dateContainers = page.locator("div.HitchhikerEventStandard-dateInnerWrapper");
            dateContainers.first().waitFor();

            int count = dateContainers.count();
            if (count == 0) {
                throw new IllegalStateException("Не удалось найти элементы с датами для проверки сортировки");
            }

            log.info("Найдено {} контейнеров с датами для проверки сортировки", count);
            List<LocalDate> collectedDates = new ArrayList<>(count);

            for (int i = 0; i < count; i++) {
                Locator container = dateContainers.nth(i);

                String day = container.locator("span.HitchhikerEventStandard-day").innerText().trim();
                String month = container.locator("span.HitchhikerEventStandard-month").innerText().trim();
                String year = container.locator("span.HitchhikerEventStandard-year").innerText().trim();

                collectedDates.add(parseRussianDate(day, month, year));
            }

            List<LocalDate> sorted = new ArrayList<>(collectedDates);
            sorted.sort(Comparator.naturalOrder());

            log.info("Проверяем, что даты отсортированы по возрастанию");
            Assertions.assertEquals(sorted, collectedDates, "Ожидалось, что даты будут отсортированы от старой к новой");
        });
    }

    private LocalDate parseRussianDate(String dayText, String monthText, String yearText) {
        int day = Integer.parseInt(dayText.trim());
        int month = resolveMonth(monthText);
        int year = Integer.parseInt(yearText.trim());
        return LocalDate.of(year, month, day);
    }

    private int resolveMonth(String rawMonth) {
        String normalized = rawMonth.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "янв", "января", "январь" -> 1;
            case "фев", "февраля", "февраль" -> 2;
            case "мар", "марта", "март" -> 3;
            case "апр", "апреля", "апрель" -> 4;
            case "май", "мая" -> 5;
            case "июн", "июня", "июнь" -> 6;
            case "июл", "июля", "июль" -> 7;
            case "авг", "августа", "август" -> 8;
            case "сен", "сентября", "сентябрь" -> 9;
            case "окт", "октября", "октябрь" -> 10;
            case "ноя", "ноября", "ноябрь" -> 11;
            case "дек", "декабря", "декабрь" -> 12;
            default -> throw new IllegalArgumentException("Неизвестный месяц: " + rawMonth);
        };
    }
}
