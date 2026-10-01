# PostgreSQL restore drill (M16.1)

Процедура проверяет восстановление уже существующего PostgreSQL backup в локальной
изолированной среде. Создание backup и его расписание остаются в существующей системе.

В исследованном backend-репозитории нет команды PostgreSQL backup или restic export;
Compose в родительском workspace использует `postgres:16-alpine`, а Flyway — схему
`public`. Поэтому скрипт работает с локальным файлом single-database `pg_dump` и не
предполагает неизвестный формат production backup. Проверка на тестовых дампах не
означает, что production snapshot уже проверен. Если backup хранится в restic,
сначала извлеките файл дампа средствами существующего процесса в отдельный каталог
на машине для проверки. Скрипт не обращается к restic, S3 или production БД.

## Ручная проверка

Нужны Bash, Docker CLI и запущенный локальный Docker Engine/Desktop с Unix socket,
обычные Unix utilities и `gzip`. `psql`/`pg_restore` на хосте не нужны: они запускаются
из `postgres:16-alpine`. Docker скачает этот образ при первом запуске, если его нет
локально. Для полностью offline-проверки подготовьте образ заранее.

Из корня `tutorplatform-backend`:

```bash
./scripts/verify-postgres-backup.sh /path/to/backup
./scripts/verify-postgres-backup.sh --help
```

Формат определяется по содержимому, расширение файла не важно. Пути с пробелами
нужно заключать в кавычки.

При известном количестве migrations на момент backup можно потребовать точное
совпадение (в текущем репозитории 15 versioned SQL migrations):

```bash
RESTORE_DRILL_EXPECTED_MIGRATIONS=15 \
  ./scripts/verify-postgres-backup.sh /path/to/backup
```

По умолчанию число выводится и должно быть больше нуля; оно не обязано совпадать с
текущим checkout, поскольку backup мог быть создан до последнего релиза. Указывать
ожидаемое число следует по версии приложения на момент backup.

`PGDATA` располагается в `tmpfs` ёмкостью 2 GiB. Для более крупного backup увеличьте
ёмкость, обеспечив достаточно памяти у локального Docker:

```bash
RESTORE_DRILL_TMPFS_SIZE=8g \
  ./scripts/verify-postgres-backup.sh /path/to/backup
```

Допустимы положительные значения с суффиксом `m` или `g` (например, `2048m`). Ёмкость
должна учитывать восстановленные таблицы, индексы и WAL, а не только размер дампа.

## Поддерживаемые форматы

| Содержимое backup | Способ восстановления |
| --- | --- |
| Single-database custom archive (`pg_dump -Fc`, сигнатура `PGDMP`) | `pg_restore --no-owner --no-privileges --exit-on-error --single-transaction` в новую случайную базу |
| Plain SQL (`pg_dump`, заголовок `-- PostgreSQL database dump`) | `psql -X --set ON_ERROR_STOP=1 --single-transaction` в новую случайную базу |
| Gzip от любого из этих двух форматов | Проверяемая распаковка в приватный временный файл, затем соответствующий restore |

Используются сервер и клиент PostgreSQL 16, как в текущем Compose. Дампы от более
нового major release могут быть несовместимы; они не считаются успешно проверенными
при ошибке восстановления. Дамп должен содержать и схему, и данные; schema-only
дамп может пройти минимальные sanity checks, поэтому выбирайте именно полный backup
из действующего процесса.

Custom archive восстанавливается без оригинальных владельцев и ACL. Plain SQL
не переписывается. Если в SQL присутствуют ownership/ACL statements для одной
application role, укажите её имя, например:

```bash
RESTORE_DRILL_OWNER_ROLE=tutor \
  ./scripts/verify-postgres-backup.sh /path/to/existing.sql.gz
```

Эта роль создаётся как `NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE` только в новом
контейнере. Допустимы простые имена до 63 символов: буквы/underscore в начале,
затем буквы, цифры или underscore. Дампы, требующие дополнительных ролей или
extensions, отсутствующих в стандартном образе, завершаются ошибкой; ошибка не
игнорируется и существующий backup не преобразуется.

Не поддерживаются tar/directory archives, физические backups (`PGDATA`, basebackup),
зашифрованные файлы, restic repository как входной путь, cluster-wide `pg_dumpall`
и plain SQL с `pg_dump --create`. Последний формат пытается создать исходное имя
базы и отклоняется транзакционным восстановлением. Скрипт не применяет `--create`
для custom archive. Для другого существующего формата сначала нужно адаптировать
restore-процедуру и её тесты под фактическую команду backup.

## Что проверяется

После restore выполняются read-only sanity checks с `statement_timeout=30s`:

- Соединение с восстановленной базой и `SELECT 1` работают.
- `public.flyway_schema_history` существует как таблица.
- Есть основные таблицы `users`, `teachers`, `students`, `subjects`,
  `learning_programs`, `topics`, `tasks`, `lesson_sessions`, `homeworks`, `submissions`.
- Ограниченный `SELECT id ... LIMIT 1` успешно выполняется для каждой core-таблицы.
  Пустые таблицы допустимы; содержимое строк не печатается.
- В Flyway history нет failed migrations и есть хотя бы одна успешно применённая
  versioned SQL migration; их количество выводится и при необходимости сверяется.

Приложение не запускается, новые migrations не применяются. Конкретные пользователи
и demo data не требуются. Эта минимальная проверка не заменяет Flyway checksum
validation, проверку полноты данных, восстановление файлов/S3 и полный application
smoke test.

Успешный custom restore выводит примерно:

```text
Restoring custom dump into restore_drill_<random> (tutor-restore-drill-<random>).
Verified: database opens; Flyway history and 10 core tables are readable.
Applied SQL migrations: 15
Restore verification passed (cleanup runs before exit).
```

Итоговый exit code должен быть `0`: сообщение о sanity checks печатается до cleanup,
а ошибка cleanup превращает результат в ненулевой. Для протокола drill сохраните
дату, идентификатор/контрольную сумму выбранного backup, версию приложения на момент
backup, вывод скрипта и exit code. Не сохраняйте содержимое production строк в CI:
ошибки PostgreSQL при восстановлении могут включать фрагменты данных.

## Exit codes

| Code | Значение |
| --- | --- |
| `0` | Restore, sanity checks и cleanup прошли (или показан `--help`) |
| `1` | Непредвиденная ошибка shell/чтения входа; успех не подтверждён |
| `2` | Некорректные аргументы, отсутствующий/пустой файл или некорректные настройки |
| `3` | Prerequisites, удалённый/недоступный Docker, startup timeout 60s или ошибка подготовки контейнера/базы/роли |
| `4` | Неподдерживаемый/повреждённый backup, ошибка gzip или restore |
| `5` | Sanity checks или проверка количества migrations не прошли |
| `6` | Restore/checks прошли, но очистка не удалась |
| `129`, `130`, `143` | Прерывание через HUP, INT, TERM; cleanup выполняется |

При ошибке restore/checks и одновременной ошибке cleanup сохраняется исходный
ненулевой code, а ошибка cleanup явно выводится в stderr вместе с ID контейнера
или путём временного каталога для ручной очистки.

## Изоляция относительно production

- Принимается только путь к файлу; параметров production connection нет.
  `.env`, Compose, `SPRING_DATASOURCE_*`, `PGHOST`, `PGDATABASE`, `PGUSER` не читаются.
- Docker endpoint разрешён только через локальный Unix socket. SSH/TCP contexts
  отклоняются. Все Docker команды закреплены за проверенным endpoint.
- Всегда создаётся новый контейнер с уникальным именем и новая база
  `restore_drill_<random>`. Production имя базы и существующие volumes не используются.
- Контейнер имеет `--network none`, не публикует порты, PostgreSQL не слушает TCP.
  Все подключения выполняются через Unix socket внутри этого контейнера.
- Нет host mounts, Docker socket или production credentials внутри контейнера.
  Восстановление читает dump через stdin; `PGDATA` находится в `tmpfs`.
- `trap` на EXIT/HUP/INT/TERM удаляет только сохранённый ID собственного контейнера
  и собственный приватный каталог. Нет `compose down`, `volume prune`, удаления
  существующих баз или очистки других контейнеров.
- Gzip распаковывается в каталог с приватными permissions; исходный файл не меняется.
  Для большой распаковки нужны свободное место в `TMPDIR` и защищённый локальный диск.

Используйте доверенные backup-файлы: SQL dump может содержать исполняемые команды.
При SIGKILL, отключении питания или недоступности Docker гарантировать выполнение
trap невозможно. Сообщение о restore содержит уникальное имя контейнера для проверки
после такого прерывания; удаляйте только контейнер конкретного drill, без глобального
prune. Docker image cache после проверки сохраняется.

## Проверки для разработки и CI

```bash
bash -n scripts/verify-postgres-backup.sh scripts/test-postgres-restore-drill.sh
shellcheck scripts/verify-postgres-backup.sh scripts/test-postgres-restore-drill.sh
./scripts/test-postgres-restore-drill.sh
./gradlew clean build --no-daemon
```

Integration-скрипт создаёт отдельный локальный source-контейнер с `tmpfs`, выполняет
реальные SQL migrations из репозитория и добавляет fixture Flyway history metadata.
Он не читает существующую application БД. Пользователи/students остаются пустыми;
контрольные backup-файлы создаются только для этих тестов и удаляются после них.
Полноценные тесты Flyway продолжают выполняться в Gradle build.

Проверяются успешные custom/SQL/gzip restores, SQL с локальной owner role, повреждённые
custom/SQL/gzip backups, отсутствие core-таблицы/history, failed/empty history,
несовпадение migrations, некорректный вход, SQL `--create` и удалённый Docker endpoint.
После каждого сценария проверяется отсутствие новых verification-контейнеров и
временных файлов. CI использует только эти синтетические fixtures, без production
backup, secrets и deployment-подключений.
