# IllitLevels (v1.1.0)

Commands:
- /illit lvl give <nick> <amount>
- /illit lvl remove <nick> <amount>
- /illit lvl set <nick> <level>
- /illit exp give <nick> <amount>
- /illit exp remove <nick> <amount>
- /illit exp set <nick> <amount>
- /illit top
- /illit info [nick]
- /illit reset <nick>

PlaceholderAPI identifier: illit

Required:
- %illit_level%
- %illit_exp%
- %illit_level_next%
- %illit_exp_next%

Formatted:
- %illit_level_format%
- %illit_exp_format%
- %illit_progress_percent%
- %illit_progress_bar%

Top 10:
- %illit_top_1_name% ... %illit_top_10_name%
- %illit_top_1_level% ... %illit_top_10_level%

Build:
mvn -q clean package
