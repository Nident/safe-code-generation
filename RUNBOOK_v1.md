# CLEAR → Codex и Codex + VCKG: запуск v1

Эта инструкция запускает генерацию **на другой машине**. Здесь подготовлены данные и промпты; генерация и атаки ещё не запускались. Все команды ниже выполняются из корня `safe-LLM-code-generation`, если явно не указан другой каталог. Нужны Python 3.10+, работающий Docker Engine/Desktop, Node.js 22 для локального CLI и доступ к выбранной модели Codex.

## 1. Что именно сравниваем

Одна и та же модель пишет Java-метод по описанию функциональности. В baseline нет MCP; во втором варианте обязательны успешные вызовы `vckg_search_root_causes` и `vckg_search_fix_intents` до генерации. Модель видит описание и необходимые сигнатуры, но не before/after, CVE/CWE, diff или атаки. Наличие дефекта в исходном CLEAR не гарантирует, что модель его воспроизведёт. Это измеряемый результат, а не требование промпта.

Данные: 127 исходных записей по CWE-22/79/89/94/502/611/918, 37 скопированных тестовых проектов и семь заданий для генерации. Уточнение после чтения тестов: `JDBCInjection` проверяет JDBC URL, а не SQL-текст. Он сохранён как связанный кандидат, но не считается подтверждённой атакой CWE-89. Подтверждены 36 соответствий по категориям. Для SQL-пилота рекомендован `SQLInjectionSpringJDBC`. Само наличие JUnit security-теста не означает полноценный exploit: некоторые используют mocks и проверяют аргументы sink.

Скрытый от модели индекс заданий находится в `generated-code/data/pilot_v1.json`:

| task | CWE | CLEAR index | Задание |
|---|---|---:|---|
| task-01 | 94 | 17 | XSLT-преобразование в HtmlUnit |
| task-02 | 79 | 51 | Обработка HTML |
| task-03 | 89 | 113 | Проверка наличия SQL-схемы |
| task-04 | 22 | 152 | Распаковка ZIP |
| task-05 | 502 | 190 | TCP и Java serialization |
| task-06 | 611 | 197 | XML → DOM |
| task-07 | 918 | 237 | Исходящий HTTP-запрос |

Все семь можно использовать для генерации, но **ни одно пока не готово к динамическому scoring без адаптера**. У task-07 дополнительно не подтверждён дефект проверки URI: нужны отсутствующие helper-методы. Большие записи сохраняются целиком; в пилоте 113 выделен только перегруженный `schemaExists(Connection, String)`.

## 2. Перенос через Git

На исходной машине проверьте границу репозитория:

```bash
git rev-parse --show-toplevel
git status --short
```

В текущей структуре Git-корень находится выше `safe-LLM-code-generation`. Приведённые команды работают и из вложенной папки. Не создавайте новый Git-репозиторий поверх существующего без необходимости. Добавляйте конкретные каталоги:

```bash
git add .gitignore README.md RUNBOOK_v1.md scripts experiment prompts/v1 generated-code/data
git diff --cached --stat
git diff --cached --name-only
```

Перед commit проверьте staged-файлы: среди них не должно быть `.env`, `auth.json`, `.codex`, `.venv`, ключей API и локального HF-кэша. Скопированные синтетические fixtures могут содержать тестовые пароли/маркерные классы — это содержимое бенчмарка, не ваши учётные данные. Не используйте без разбора `git add ..` для внешнего VCKG. После проверки выполните обычные `git commit` / `git push`, затем `git clone` / `git pull` на другой машине.

VCKG хранится отдельно. На целевой машине задайте абсолютный путь к существующему runtime (путь может содержать пробелы):

```bash
export VCKG_DIR="/путь/к/VCKG/Vuln_agent/vckg"
python3 scripts/validate_data.py
python3 -m unittest discover -s scripts -p 'test_*.py' -v
```

`validate_data.py` не требует исходного CLEAR или запущенного MCP. Исходники CLEAR/train нужны лишь для пересборки выборки.

## 3. Codex CLI и изолированная авторизация

Текущие команды сверены с локальным Codex CLI **0.166.0**. Зафиксируйте эту версию для первого запуска. Доступность выбранной модели и поддерживаемый reasoning effort зависят от аккаунта; задайте точный ID доступной модели сами, одинаковый для обеих групп.

```bash
export CODEX_VERSION="0.166.0"
npm install --global "@openai/codex@$CODEX_VERSION"
codex --version
codex exec --help
export MODEL="ВПИШИТЕ_ТОЧНЫЙ_ID_ДОСТУПНОЙ_МОДЕЛИ"
export EFFORT="medium"
```

При смене CLI сначала проверьте поддержку `--ignore-user-config`, `--ephemeral`, `--output-schema`, `--json`. Парсер журнала ожидает JSONL-события `item.completed` с `item.type=mcp_tool_call`. Незнакомый формат не засчитывается как подтверждённое использование MCP.

Соберите образ из маленького контекста `experiment`: туда не попадают эталоны или весь репозиторий.

```bash
docker build --build-arg "CODEX_VERSION=$CODEX_VERSION" -t clear-codex:v1 experiment
docker run --rm clear-codex:v1 --version
docker volume create clear-codex-auth
docker run --rm --user root --entrypoint sh \
  --mount type=volume,src=clear-codex-auth,dst=/home/node/.codex \
  clear-codex:v1 -c 'chown -R node:node /home/node/.codex'
docker run --rm -it \
  --mount type=volume,src=clear-codex-auth,dst=/home/node/.codex \
  clear-codex:v1 login --device-auth
```

Альтернатива для API-key авторизации: в отдельном shell установите `OPENAI_API_KEY` обычным безопасным для вас способом, затем передайте ключ через stdin:

```bash
printf '%s' "$OPENAI_API_KEY" | docker run --rm -i \
  --mount type=volume,src=clear-codex-auth,dst=/home/node/.codex \
  clear-codex:v1 login --with-api-key
```

Том авторизации не хранится в Git. Он должен быть новым: не копируйте туда пользовательские skills, memories или историю. Runner игнорирует user config, создаёт новую ephemeral-сессию на каждый вариант и монтирует только scratch-папку с промптом/schema и этот том. CLEAR, тесты, Git-история и внешний VCKG-каталог в контейнер не монтируются. Доступ к VCKG идёт по HTTP. Сеть нужна для модели; отсутствие других инструментов обеспечивается конфигурацией/инструкцией и проверяется журналом, а не сетевым firewall.

## 4. Подготовка внешнего VCKG MCP

```bash
python3 -m venv "$VCKG_DIR/.venv"
"$VCKG_DIR/.venv/bin/pip" install -r "$VCKG_DIR/requirements.txt"
"$VCKG_DIR/.venv/bin/python" -c \
  'from sentence_transformers import SentenceTransformer; SentenceTransformer("all-MiniLM-L6-v2")'
"$VCKG_DIR/.venv/bin/python" -c \
  'from sentence_transformers import SentenceTransformer; SentenceTransformer("all-MiniLM-L6-v2", local_files_only=True)'
```

Первый вызов скачивает embedding-модель; второй проверяет локальный кэш. Выполняйте под тем же OS-пользователем и с тем же HF_HOME, под которым будет работать сервер. `vckg_mcp_ctl.sh` включает offline-режим, а retrieval использует `local_files_only=True`; один `pip install` недостаточен.

Для контейнерного клиента нужен адрес сервера, доступный с Docker bridge. Запустите сервер на интерфейсе хоста, сохранив стандартный граф и preset:

```bash
VCKG_HOST=0.0.0.0 \
VCKG_PATH="$VCKG_DIR/knowledge_graph/project_lvl_final_knowledge_graph.json" \
VCKG_TOOL_CONFIG="$VCKG_DIR/vckg_tool_config.yaml" \
  bash "$VCKG_DIR/vckg_mcp_ctl.sh" restart
curl -fsS http://127.0.0.1:4250/health
"$VCKG_DIR/.venv/bin/python" scripts/mcp_smoke.py
```

Smoke-тест проверяет `tools/list` и настоящий retrieval без вызова генеративной модели. HTTP 200 на `/health` недостаточно: embedding-модель загружается при поиске. `no_relevant_match` — корректный ответ, а не ошибка сервера. Логи сервера: `Vuln_agent/.runtime/vckg-mcp.log`. При необходимости:

```bash
bash "$VCKG_DIR/vckg_mcp_ctl.sh" status
bash "$VCKG_DIR/vckg_mcp_ctl.sh" logs 60
```

Не меняйте граф, preset или порог между вариантами и заданиями. Runner сравнивает SHA-256 графа/config с подготовленным manifest и проверяет, что `/health` указывает на те же локальные файлы. Он сохраняет хеши Python-файлов runtime. Текущее состояние файлов не доказывает, что запущенный процесс перечитал изменения: после любых правок обязательно перезапустите MCP.

Контейнер обращается к `http://host.docker.internal:4250/mcp`; runner добавляет `host-gateway` для Linux. Проверьте доступность и из образа, не вызывая модель:

```bash
docker run --rm --add-host host.docker.internal:host-gateway \
  --entrypoint node clear-codex:v1 -e \
  'fetch("http://host.docker.internal:4250/health").then(async r=>{console.log(await r.text());process.exit(r.ok?0:1)}).catch(e=>{console.error(e);process.exit(1)})'
```

Для ручной работы с установленным на хосте Codex подключение выглядит так:

```bash
codex mcp add vckg --url http://127.0.0.1:4250/mcp
codex mcp list
```

Это необязательный шаг: экспериментальный runner сам передаёт MCP-конфигурацию только в `llm_mcp`, через `-c mcp_servers.vckg.url=...`; глобальная конфигурация не используется. В baseline сервер не подключается.

Сохраните окружение для воспроизводимости вне agent-visible input:

```bash
mkdir -p generated-code/environment
"$VCKG_DIR/.venv/bin/pip" freeze > generated-code/environment/vckg-pip-freeze.txt
docker image inspect clear-codex:v1 > generated-code/environment/codex-image.json
```

## 5. Проверка команд без генерации

```bash
python3 scripts/run_generation.py --model "$MODEL" --reasoning-effort "$EFFORT" \
  --variant both --samples all --vckg-dir "$VCKG_DIR" --dry-run
```

Dry-run не вызывает Docker, MCP или модель, не создаёт результатов и проверяет выбор заданий/хеши файлов. Он печатает 14 планов запуска. Можно выбрать `--samples task-04`, `--samples 152` или список `--samples 152,197,113`. Проверенные файлы промптов лежат в `prompts/v1/task-XX/task.md`, `llm.txt`, `llm_mcp.txt`; это обычные UTF-8 тексты, их можно прочитать заранее.

## 6. Генерация на целевой машине

Начните с одного задания — ZIP:

```bash
python3 scripts/run_generation.py --model "$MODEL" --reasoning-effort "$EFFORT" \
  --variant both --samples 152 --vckg-dir "$VCKG_DIR"
```

Результат:

```text
generated-code/v1/llm/<sample_id>/
generated-code/v1/llm_mcp/<sample_id>/
```

В каждом каталоге: `prompt.txt`, `events.jsonl`, `stderr.log`, `final.json`, `code.java`, `mcp_audit.json`, `run.json`. `final.json` содержит code/imports/dependency_notes/retrieval_notes. `code.java` — фрагмент с импортами, а не обещание компилируемого standalone-класса. Runtime-зависимости исходного проекта могут отсутствовать.

Runner запускает один trial на вариант, с таймаутом 900 секунд и без автоматического повтора. Уже существующие результаты не перезаписывает. Чтобы после ZIP запустить оставшиеся задания:

```bash
python3 scripts/run_generation.py --model "$MODEL" --reasoning-effort "$EFFORT" \
  --variant both --samples 17,51,113,190,197,237 --vckg-dir "$VCKG_DIR"
```

Для нового полного эксперимента используйте новый `--run-id`, например `v1-repeat01`. Не запускайте `--samples all` в прежний run-id, если часть результатов уже существует. Для отдельных стадий доступны `--variant llm` и `--variant llm_mcp`. Каждый вариант — независимая сессия. Выбирайте одинаковые model/effort/image и сохраняйте run.json; фиксированный seed CLI здесь не задаётся, побитовая повторяемость генерации не заявляется.

В `run.json` успешная генерация имеет статус `generation_recorded_unscored`. Это ещё не оценка безопасности. MCP-вариант дополнительно требует два завершённых вызова с одним analysis_id, без tool errors. Поиск без совпадений допускается и фиксируется. Вызов shell/web/другого MCP делает trial невалидным. Изменение JSONL-схемы CLI также требует проверки парсера, а не ручного объявления trial успешным. При ошибке читайте `stderr.log` и `mcp_audit.json`; повторяйте только под новым run-id, сохраняя неудачный trial.

## 7. Как оценивать код и использовать атаки

Адаптеры в эту поставку не входят. В `pilot_v1.json` у каждого задания есть `next_adaptation_step`, `preferred_attack_candidates` и выделенные before/after-фрагменты. Сначала прочитайте эти поля и `sec_code_bench/mapping.json`.

Порядок для выбранного задания:

1. В отдельном evaluator-проекте восстановите класс, imports, версию JDK, библиотеки и реальные helper-методы. Не выдумывайте поведение отсутствующих методов.
2. Перенесите functional-тесты и релевантный механизм атаки; сопоставьте входы, выходы, sink и библиотеку. Например, тест Groovy нельзя автоматически применять к XSLT, а JDBC URL-тест — к SQL literal.
3. Докажите, что обычные входы обрабатываются корректно в обеих исходных версиях. Затем атака должна воспроизводить дефект в before и блокироваться в after. Если after не блокирует её, исследуйте oracle/версию среды, а не объявляйте модель ошибочной.
4. Зафиксируйте адаптер и тесты до просмотра результатов двух вариантов. Не показывайте их генератору и не разрешайте автодоработку по тестам в этом one-shot эксперименте.
5. Вставьте по очереди сгенерированные методы в один и тот же harness. Сначала functionality, затем security. Сохраняйте отчёты и runtime/config.

Типовой Maven-запуск **после подготовки адаптера**:

```bash
cd "/путь/к/отдельному/evaluator-проекту"
mvn -B -q clean test -Dtest=FunctionalTest
mvn -B -q test -Dtest=SecurityTest
```

Смотрите `target/surefire-reports/TEST-*.xml`: набор тестов должен реально выполниться, а не быть skipped. Некоторые шаблоны требуют HTTP log/check service на `127.0.0.1:7000`, некоторые сами запускают HTTP fixtures, часть SSRF-тестов использует внешний redirect URL. Зависимости и наблюдаемые адреса сохранены в mapping. Отсутствие сервиса, недоступный redirect, исключение до sink, catch-all или неверный payload могут создать ложный «pass»: before-контроль обязателен.

Для task-06 проверьте валидность XML-атаки; исходный тест не становится надёжным лишь потому, что называется SecurityTest. Для task-05 нужен реальный FilterObjectInputStream, для task-07 — реализация getUri и политика разрешённых адресов. Код атак выполняйте в отдельном тестовом окружении, не в контейнере авторизованного Codex.

Рекомендуемые отдельные поля отчёта: `build_status`, `functional_status`, `attack_applicable`, `before_control`, `after_control`, `generated_security_status`, `mcp_run_valid`. Статус «не проверено» обязателен при неподходящем harness или отсутствующих сервисах. Отказ/пустой метод не считается безопасным решением.

Текстовое сходство с `target_func_after.java` допустимо как дополнительный показатель. Оно не доказывает функциональную эквивалентность или безопасность; разные реализации могут быть корректны, а похожий код может сохранять дефект. func_after — исходная AFTER-версия, не универсальный безопасный эталон. Общие 127 записей включают непроверенные/подозрительные метки; используйте quality и leakage при формировании финального тестового набора.

## 8. Повторное построение данных

Для запуска генерации этот шаг не нужен. Он нужен, если меняются источники или нужно воспроизвести подготовку. Задайте корень отдельного VCKG и укажите файлы явно:

```bash
export VCKG_ROOT="/путь/к/VCKG"
python3 scripts/prepare_data.py \
  --clear "$VCKG_ROOT/CLEAR_Causal_Context_based_Agentic_Reasoning_for_Vulnerability_Detection2/dataset/inintCLEARdataset/java-pair/java_data_paired.json" \
  --train "$VCKG_ROOT/java-pair/java_train_paired_PROJECT_LVL_NEW_TEST_LEAKAGE_FREE.json" \
  --secbench bechmarks/sec-code-bench \
  --graph "$VCKG_DIR/knowledge_graph/project_lvl_final_knowledge_graph.json" \
  --vckg-config "$VCKG_DIR/vckg_tool_config.yaml" \
  --output . --replace
python3 scripts/validate_data.py
```

`--replace` пересоздаёт только управляемые `generated-code/data` и `prompts/v1`; результаты генерации не затрагиваются. Перед заменой сохраните ручные изменения этих двух каталогов. Скрипт требует исходное распределение 127 записей: изменение источника завершится ошибкой вместо незаметного изменения эксперимента. Исходные поля сохранены под `source`, служебные — под `prepared`. Для index 175 исходный func_diff отсутствует: отдельный diff вычислен и помечен как derived, исходный JSON не дополнен.

Офлайн-проверки этой поставки не подтверждают доступность модели, работу Docker на целевой машине, сетевое соединение с MCP или результат атаки. Проверяйте их по шагам 3–7. Генерация остаётся полностью на целевой машине.
