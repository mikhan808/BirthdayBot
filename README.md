# BirthdayBot

## Configuration

Copy `config.properties.example` to `config.properties` and fill in the bot and
database credentials. The resulting `config.properties` is local and excluded
from Git.

By default, the application reads the file from its working directory. To use a
different location, start it with:

```shell
java -Dbirthdaybot.config=/path/to/config.properties -jar target/birthday.jar
```

Инструкция по переносу базы `BIRTH` находится в
[`FIREBIRD_5_MIGRATION.md`](FIREBIRD_5_MIGRATION.md).
