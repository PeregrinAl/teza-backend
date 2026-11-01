# teza-backend

Backend публичной базы аргументов teza: Java 21, Spring Boot, PostgreSQL.
Текущий API поддерживает вход редактора, создание, просмотр и изменение вопросов.

## Запуск через Docker

Нужен Docker с Compose v2: Docker Desktop на Windows/macOS или Docker Engine
с Compose на Linux. Команды выполняются в каталоге `teza-backend`.

**Windows — PowerShell:**

```powershell
Copy-Item .env.example .env
```

**macOS и Linux:**

```bash
cp .env.example .env
```

До первого запуска задайте в `.env` свои `POSTGRES_PASSWORD` и `EDITOR_PASSWORD`.
Пароль редактора — минимум 12 символов, максимум 72 байта UTF-8;
проще использовать 12–72 ASCII-символа: латиницу, цифры и знаки.
`EDITOR_USERNAME` — имя редактора. Значения записывайте без кавычек.
Не добавляйте `.env` в Git.

**Windows, macOS и Linux:**

```text
docker compose up -d --build
docker compose logs -f backend
```

Проверка готовности:

| Система | Команда |
| --- | --- |
| Windows, PowerShell | `Invoke-RestMethod http://localhost:8080/actuator/health` |
| macOS / Linux | `curl http://localhost:8080/actuator/health` |

Ожидаемый статус — `UP`. PostgreSQL и API доступны только с локальной машины.
Порт API меняется через `SERVER_PORT`, порт PostgreSQL — через `POSTGRES_PORT`.

Остановка на любой системе:

```text
docker compose down
```

Данные PostgreSQL сохраняются в Docker volume. Удаление volume удаляет и данные.

## Запуск без контейнера backend

Дополнительно нужен JDK 21; Maven загружает wrapper. Подготовьте `.env`, как выше,
запустите только PostgreSQL:

```text
docker compose up -d db
```

**Windows — PowerShell:**

```powershell
.\mvnw.cmd spring-boot:run
```

**macOS и Linux:**

```bash
./mvnw spring-boot:run
```

Приложение читает `.env` из текущего каталога. Если backend уже запущен в Docker,
сначала остановите его командой `docker compose stop backend`.

## API

Базовый адрес — `http://localhost:8080`. Рабочие вопросы доступны редактору;
публикация разборов появится в следующих версиях.

Для работы в браузере откройте `http://localhost:8080/login` и войдите
с `EDITOR_USERNAME` и `EDITOR_PASSWORD` из `.env`. После входа откроется
Swagger UI. Выполните `GET /api/v1/auth/csrf`, скопируйте значение `token`
и вставьте его в **Authorize → csrfToken**. Cookie сессии браузер передаёт сам;
после этого доступны и изменяющие запросы.

| Метод и путь | Назначение |
| --- | --- |
| `GET /api/v1/auth/csrf` | Получить CSRF-токен и имя заголовка; сохранить cookie сессии. |
| `POST /api/v1/auth/login` | Войти: form-поля `username`, `password` и CSRF-заголовок. |
| `GET /api/v1/auth/me` | Текущий редактор. |
| `POST /api/v1/auth/logout` | Завершить сессию; нужен CSRF-токен. |
| `GET /api/v1/questions?page=0&size=20` | Список вопросов. |
| `POST /api/v1/questions` | Создать вопрос. |
| `GET /api/v1/questions/{id}` | Получить вопрос. |
| `PATCH /api/v1/questions/{id}` | Изменить вопрос. |

После входа получите свежий CSRF-токен тем же запросом. Для всех изменяющих
запросов передавайте токен и cookie сессии; пароль повторно не нужен.
Подойдут Postman, HTTP-клиент IDE или другой клиент с сохранением cookies.

Тело создания вопроса:

```json
{
  "title": "Как проверить причинную связь?",
  "description": "Вопрос для учебного разбора",
  "type": "causal"
}
```

Типы: `empirical`, `causal`, `normative`, `predictive`, `practical`.
Для изменения передайте `version` из последнего `GET` и изменяемые поля:

```json
{
  "version": 0,
  "title": "Какие данные нужны для проверки причинной связи?"
}
```

Конфликт изменений возвращает `409`. Документация после входа:
`/swagger-ui/index.html`, схема — `/v3/api-docs`.

## Проверки

Нужны JDK 21 и работающий Docker. Тесты сами запускают отдельный PostgreSQL.

**Windows — PowerShell:**

```powershell
.\mvnw.cmd verify
```

**macOS и Linux:**

```bash
./mvnw verify
```

Спецификация и архитектура: [teza](https://github.com/PeregrinAl/teza).
