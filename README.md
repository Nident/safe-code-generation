# CLEAR → Codex / Codex + VCKG

Начните с [RUNBOOK_v1.md](RUNBOOK_v1.md): перенос через Git, настройка внешнего MCP, изолированный запуск Codex и оценка результатов.

- [Данные](generated-code/data/README.md): 127 CLEAR before/after-пар по 7 CWE.
- [Пилот](generated-code/data/pilot_v1.json): семь заданий, связи с эталонами и следующие шаги адаптации.
- [Промпты](prompts/v1): общее задание и два варианта для каждого task-XX.
- [Соответствия атак](generated-code/data/sec_code_bench/mapping.json): 37 сохранённых шаблонов, из которых 36 подтверждены по категориям; JDBCInjection отмечен как JDBC URL-кандидат, а не SQL-атака.

Быстрая офлайн-проверка из этой папки:

```bash
python3 scripts/validate_data.py
python3 -m unittest discover -s scripts -p 'test_*.py' -v
```

Генерация модели и атаки при подготовке не запускались. Динамическая оценка требует адаптеров; совпадение CWE не делает тест совместимым с произвольной CLEAR-функцией. Внешний VCKG не копируется в этот проект.
