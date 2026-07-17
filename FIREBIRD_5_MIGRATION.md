# Миграция BIRTH на RedDatabase 5.2

Миграция выполнена 17 июля 2026 года логическим backup/restore через `gbak`.
Исходная база не изменялась и сохранена для отката.

## Схема миграции

| Этап | Сервер | Версия | ODS |
| --- | --- | --- | --- |
| Исходная база | `192.168.0.50:3026`, `H:\databases\BIRTH.FDB` | RDB 2.6 | 11.4 |
| Промежуточная база | `192.168.0.50:3050` | RDB 3.0 | 12.3 |
| Проверочная база | `127.0.0.1:3050`, `/var/lib/reddatabase/birthdaybot/BIRTH.FDB` | RDB 5.1 | 13.1 |
| Рабочая база | `192.168.0.50:3052`, `H:\databases\BIRTH_RDB52.FDB` | RDB 5.2.4 | 13.1 |

Промежуточный этап использован для последовательного обновления формата базы:
RDB 2.6 → RDB 3.0 → RDB 5.2.4. Локальная копия RDB 5.1 сохранена как
дополнительная точка отката.

## Сохранённые резервные копии

На Windows-сервере:

- `H:\databases\migration\BIRTH_RDB26_20260717.fbk`
- `H:\databases\migration\BIRTH_RDB26_backup.log`
- `H:\databases\migration\BIRTH_RDB3_20260717.FDB`
- `H:\databases\migration\BIRTH_RDB3_20260717.fbk`
- `H:\databases\migration\BIRTH_RDB3_restore.log`
- `H:\databases\migration\BIRTH_RDB3_backup.log`
- `H:\databases\migration\BIRTH_RDB52_restore.log`

В Ubuntu:

- `/var/backups/reddatabase/birthdaybot/BIRTH_RDB3_20260717.fbk`
- `/var/backups/reddatabase/birthdaybot/BIRTH_RDB5_restore.log`

Не удаляйте исходную базу и резервные копии до завершения эксплуатационной
проверки BirthdayBot.

## Результаты проверки

- Сервер назначения: `WI-V5.2.4.0 RedDatabase 5.2`.
- ODS: `13.1`, SQL dialect: `3`.
- `gfix -validate -full` завершился успешно.
- Невалидных триггеров и процедур нет.
- Количество записей совпало в `PEOPLE`, `CHATS`, `DIALOGS`, `DIALOGS_DATA`,
  `CHAT_INFO`, `BIBLE`, `PLAN_BIBLE`, `MOLITVA`, `VIEW_PEOPLE`, `GROUPS` и
  `MAILS`.

Рабочая конфигурация:

```properties
database.birth.url=jdbc:firebirdsql:192.168.0.50/3052:H:\\databases\\BIRTH_RDB52.FDB
database.church.url=jdbc:firebirdsql:192.168.0.50/3050:H:\\databases\\CHURCH.FDB
```

Рабочая база обслуживается службой Windows
`RedDatabaseServerBirthdayBotRDB52`; для неё разрешён входящий TCP-порт `3052`.

## Откат

Если эксплуатационная проверка обнаружит проблему, остановите BirthdayBot и
временно верните подключение к исходной базе:

```properties
database.birth.url=jdbc:firebirdsql:192.168.0.50/3026:H:\\databases\\BIRTH.FDB
```

После изменения конфигурации перезапустите приложение. Не пытайтесь заменять
файлы `.FDB` простым копированием между версиями RedDatabase.
