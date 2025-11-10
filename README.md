# Обзор запуска тестов

- **Заполняем свойства:** `src/test/resources/config/framework.properties` содержит параметры `baseUrl`, `browser`, `headless`, директории артефактов и т.д.
- **Читаем настройки:** `ConfigurationManager.load()` собирает значения из файла, переменных окружения и JVM-параметров, затем возвращает `FrameworkConfig`.
- **Создаём сессию:** `PlaywrightFactory.newSession(testId)` берёт `FrameworkConfig`, гарантирует существование папок артефактов, поднимает Playwright, настраивает браузер и таймауты, возвращает `PlaywrightSession`.
- **Интегрируем с JUnit:** `PlaywrightExtension.beforeEach` генерирует `testId`, делает `newSession`, запускает трейс (если включено) и переходит на `baseUrl`; сессию кладёт в `ExtensionContext`.
- **Получаем доступ в тестах:** `BaseTest.captureSession(PlaywrightSession)` сохраняет сессию, предоставляя хелперы `session()` и `config()` для наследников.
- **Вызываем Page Object методы:** тесты создают, например, `new HomePage(session().page(), config())`, строят цепочки шагов (`open()`, `openDocs()`, `expect...`) — методы работают с Playwright `Page`.
- **Завершаем выполнение:** `PlaywrightExtension.afterEach` закрывает сессию; при падении `testFailed` делает скриншот, экспортирует трейс и опционально сохраняет видео через методы `PlaywrightSession` и прикрепляет их в Allure.


