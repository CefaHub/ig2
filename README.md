# IllitLevels

Custom level/exp system (0..100) with a non-linear growth curve and a very hard last 10 levels.
Includes PlaceholderAPI placeholders:

- %illit_level%
- %illit_exp%
- %illit_level_next%
- %illit_exp_next%

## Build
Requirements: Java 17, Maven.

```bash
mvn -q clean package
```

The jar will be in:
`target/IllitLevels-1.0.0.jar`

## Install
1) Put `IllitLevels-1.0.0.jar` into `plugins/`
2) Make sure `PlaceholderAPI` is installed.
3) Restart the server.

## Admin commands
- /illit addexp <player> <amount>
- /illit setlevel <player> <level>
- /illit reset <player>
- /illit info [player]

## Tuning
Edit `plugins/IllitLevels/config.yml` (growth & endgame coefficients).
