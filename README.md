# coldemailmpv

MVP backend для cold email SaaS на `Spring Boot 4`, `Java 21`, `PostgreSQL` и `JWT`.

- регистрация и логин пользователей
- personal workspace для каждого пользователя
- участники workspace
- подключение sender account через SMTP и IMAP
- тест SMTP и тест IMAP
- отправка тестового письма
- prospects CRUD и импорт CSV
- one-off отправка письма prospect'у
- история email events
- кампании с одной ступенью
- персонализация шаблонов
- suppression list
- публичный unsubscribe flow
- bounce handling foundation

## Стек
- `Java 21`
- `Spring Boot 4.0.3`
- `Spring Web MVC`
- `Spring Security`
- `Spring Data JPA`
- `PostgreSQL`
- `JavaMail`
- `JWT (jjwt)`
- `Docker` / `Docker Compose`
- `JUnit 5`, `Mockito`, `H2`

## Архитектура
Проект сейчас собран как модульный монолит.

Основные пакеты:
- `api/controller` REST endpoints
- `api/dto` — входные и выходные DTO
- `store/entity` JPA сущности и enum
- `store/repository` JPA repositories
- `store/service` сервисный слой
- `store/security` JWT и security-конфигурация
- `store/exception` единый формат ошибок

## Текущее поведение MVP

### 1. Auth
- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/users/me`

После регистрации автоматически создаются:
- пользователь
- роль `ROLE_USER`
- personal workspace
- membership владельца с ролью `OWNER`

### 2. Workspaces
- `GET /api/workspaces/current`
- `GET /api/workspaces/current/members`
- `POST /api/workspaces/current/members`
- `DELETE /api/workspaces/current/members/{userId}`

у пользователя один основной personal workspace, через который и идет текущий SaaS flow.

### 3. Sender accounts
- `POST /api/senders`
- `GET /api/senders`
- `POST /api/senders/{senderId}/test`
- `POST /api/senders/{senderId}/test-imap`
- `POST /api/senders/{senderId}/send-test`

Sender хранит:
- SMTP настройки
- IMAP настройки
- шифрованные SMTP/IMAP пароли
- статус sender account

SMTP используется для отправки.
IMAP пока используется только для проверки подключения. Полный reply sync еще не реализован.

### 4. Prospects
- `POST /api/prospects`
- `POST /api/prospects/import`
- `GET /api/prospects`
- `GET /api/prospects/{prospectId}`
- `DELETE /api/prospects/{prospectId}`

Поддерживается CSV импорт с header row.
Поддерживаемые колонки:
- `email`
- `firstName`
- `lastName`
- `company`
- `jobTitle`
- `website`

### 5. Sending
- `POST /api/prospects/{prospectId}/send`
- `GET /api/prospects/{prospectId}/events`
- `GET /api/email-events`

Все отправки создают `email_events` со статусами:
- `SENT`
- `FAILED`
- `SKIPPED`

### 6. Campaigns
- `POST /api/campaigns`
- `GET /api/campaigns`
- `GET /api/campaigns/{campaignId}`
- `POST /api/campaigns/{campaignId}/send`

Текущая кампания - это одна тема и один текст, которые отправляются по всем `ACTIVE` prospects текущего workspace.

### 7. Personalization
Поддерживаются плейсхолдеры в `subject` и `text`:
- `{{firstName}}`
- `{{lastName}}`
- `{{company}}`
- `{{jobTitle}}`
- `{{website}}`
- `{{email}}`

Пример:

```text
Hello {{firstName}} from {{company}}
```

### 8. Suppression и unsubscribe
- `POST /api/suppressions`
- `GET /api/suppressions`
- `DELETE /api/suppressions/{suppressionId}`
- `GET /api/public/unsubscribe?token=...`
- `POST /api/public/unsubscribe`

Если email находится в suppression list, письмо не отправляется, а в `email_events` сохраняется `SKIPPED`.

Причины suppression:
- `UNSUBSCRIBED`
- `BOUNCED`
- `MANUAL`

## Ограничения текущей версии
Сейчас в проекте еще нет:
- sequence steps
- автоматического reply tracking через IMAP sync
- open tracking / click tracking
- scheduler и очереди
- warmup engine
- billing
- multistep campaigns
- полноценного multi-workspace UX

## Запуск

### Локально через Maven
Нужен локальный PostgreSQL на `localhost:5432`.

```bash
./mvnw spring-boot:run
```

### Через Docker Compose

```bash
docker compose up -d --build
```

## Конфигурация
Основные параметры берутся из `application.properties` и могут быть переопределены через env vars.

### Приложение
- `SERVER_PORT` порт приложения, по умолчанию `8080`

### PostgreSQL
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_JPA_HIBERNATE_DDL_AUTO`

### JWT
- `SECURITY_JWT_SECRET`
- `SECURITY_JWT_EXPIRATION_MS`

### Unsubscribe
- `APP_PUBLIC_BASE_URL` базовый публичный URL для unsubscribe ссылок
- `UNSUBSCRIBE_TOKEN_SECRET`
- `UNSUBSCRIBE_TOKEN_EXPIRATION_MS`

### Шифрование секретов
- `SECURITY_CRYPTO_SECRET`

### Таймауты sender checks
- `SENDER_SMTP_TEST_TIMEOUT_MS`
- `SENDER_IMAP_TEST_TIMEOUT_MS`

## Единый формат ошибок
Проект возвращает единый `ErrorResponse`:

```json
{
  "timestamp": "2026-03-20T10:00:00Z",
  "status": 400,
  "message": "Validation failed",
  "path": "/api/campaigns",
  "errors": {
    "name": "must not be blank"
  }
}
```

Поля:
- `timestamp`
- `status`
- `message`
- `path`
- `errors` объект с field validation errors, если это ошибка валидации

## Быстрый сценарий проверки MVP
1. Зарегистрировать пользователя
2. Добавить sender account
3. Протестировать SMTP
4. Импортировать prospects CSV
5. Отправить письмо одному prospect
6. Проверить `GET /api/email-events`
7. Создать campaign
8. Запустить campaign
9. Проверить suppression/unsubscribe flow

## Пример CSV для импорта

```csv
email,firstName,lastName,company,jobTitle,website
john@acme.com,John,Doe,Acme,Founder,https://acme.com
jane@beta.com,Jane,Smith,Beta,CEO,https://beta.com
```

## OpenAPI
Полное описание ручек лежит в [openapi.yaml](./openapi.yaml).


