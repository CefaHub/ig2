# IllitLevels

Custom level/exp system (1..100) with a non-linear growth curve and a very hard last 10 levels.
Includes PlaceholderAPI placeholders:

- %illit_level%
- %illit_exp%
- %illit_level_next%
- %illit_exp_next%

## Build
Java 17 + Maven:

```bash
mvn -q clean package
```

Jar will be in `target/IllitLevels-1.0.0.jar`.

## Commands
- /illit lvl give <nick> <amount>
- /illit lvl remove <nick> <amount>
- /illit exp give <nick> <amount>
- /illit exp remove <nick> <amount>
- /illit info [nick]
- /illit reset <nick>

## PlaceholderAPI
Expansion identifier: `illit`

Use in configs/chat:
`%illit_level%`, `%illit_exp%`, `%illit_level_next%`, `%illit_exp_next%`
