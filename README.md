# SerferDB — лёгкая встраиваемая key-value база данных на Java

**SerferDB** — это минималистичная, но легко расширяемая встраиваемая база данных на Java, реализующая дисковую key-value структуру на основе **B-дерева**, для сохранения и хранения важных состояний между запусками.

---

## Особенности

- Отсутствие зависимостей, полное управление хранилищем на уровне байт
- Покрыт unit-тестами (`JUnit`)
- Легко расширяется
- Приятный интерфейс `Serfer` для простого доступа к хранилищу
- Удобный типизированный доступ к данным через SEntity

---

## Архитектура

``` text
core/
 ├── search/           — поисковая система
 ├── memory/           — дисковая реализация PageDumper (DiskPageDumper)
 ├── page/             — абстракции страниц и размерности
 └── exception/        — исключения доступа и состояния

api/
 ├── Serfer            — основной интерфейс
 ├── SerferStorage     — реализация хранилища
 ├── SEntity           — обёртка над сериализуемыми значениями
 └── STypes            — типы значений
```

 ## Пример работы с api

```java
Serfer db = SerferStorage.openOrCreate("db_filename");
db.insert("somekey", SEntity.of("somevalue"));
SEntity result = db.get("otherkey");
System.out.println(result.asString().orElse("not found"));
```
