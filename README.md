# IllitLevels

Система уровней/опыта (1..100) с нелинейным ростом и очень сложными последними 10 уровнями.

## Префикс в чате
Все сообщения команд идут с префиксом:
`&7[&f&lILLIT LVL&7]`

(В коде используется §-формат.)

## Команды
- /illit lvl give <ник> <кол-во>
- /illit lvl remove <ник> <кол-во>
- /illit lvl set <ник> <уровень>
- /illit exp give <ник> <кол-во>
- /illit exp remove <ник> <кол-во>
- /illit exp set <ник> <значение>
- /illit top
- /illit info [ник]
- /illit reset <ник>

## PlaceholderAPI (identifier: illit)

Основные:
- %illit_level%
- %illit_exp%
- %illit_level_next%
- %illit_exp_next%

Форматированные:
- %illit_level_format% (например 17/100)
- %illit_exp_format% (например 35/420)
- %illit_progress_percent%
- %illit_progress_bar%

Топ-10:
- %illit_top_1_name% ... %illit_top_10_name%
- %illit_top_1_level% ... %illit_top_10_level%

## Сборка
Java 17 + Maven:
```bash
mvn -q clean package
```
Jar будет в `target/IllitLevels-1.1.0.jar`.
