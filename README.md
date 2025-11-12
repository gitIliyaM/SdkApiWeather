# Weather SDK Projects Overview
Этот репозиторий содержит два связанных Java-проекта на Spring Boot, реализующих SDK для работы с OpenWeatherMap API. 
Оба проекта соответствуют требованиям технического задания, но имеют разные архитектурные подходы:

kameleoon-weather-api: базовая реализация с упрощённой (одиночной) моделью SDK. <br>
weather-sdk: улучшенная реализация с поддержкой многоключевого режима, где можно одновременно использовать 
несколько экземпляров SDK с разными API-ключами.

# Общие характеристики
Оба проекта реализуют следующие требования:

Инициализация SDK с передачей API-ключа и режима работы:
on-demand — обновление погоды только по запросу.
polling — фоновое обновление данных каждую минуту (для всех закэшированных городов).
Кэширование данных на 1 минуту (настраиваемо).
Лимит кэша — до 10 городов.
Поддержка получения погоды для одного или нескольких городов.
Хранение API-ключей в PostgreSQL.
REST API с валидацией входных параметров.
Обработка ошибок через глобальный обработчик исключений.
Поддержка Docker и docker-compose.

## Проект 1: weather-api
Архитектура <br>
Один глобальный экземпляр SDK. <br>
Кэш и polling привязаны к приложению, а не к конкретному API-ключу. <br>
Режим (on-demand / polling) указывается в каждом запросе. <br>

## Проект 2: weather-sdk
Архитектура <br>
Поддержка множества изолированных экземпляров SDK, каждый со своим: <br>
API-ключом, <br>
режимом работы (on-demand или polling), <br>
кэшем (до 10 городов), <br>
фоновым polling-планировщиком (если включён). <br>
Запрещено создавать два SDK с одинаковым ключом.<br>
Возможность удалить экземпляр SDK вручную → очистка кэша + остановка polling. <br>

# Управление SDK через REST API
Создать новый SDK-экземпляр
## POST /api/sdk/initialize?apiKey=...&mode=polling

Удалить экземпляр и освободить ресурсы
## DELETE /api/sdk/{apiKey}

Получить погоду (с кэшированием)
## GET /api/sdk/{apiKey}/weather/{city}


Получить погоду для списка городов
## POST /api/sdk/multiple?apiKey=...

Список закэшированных городов
## GET /api/sdk/{apiKey}/cached-cities

Статистика кэша и режима
## GET /api/sdk/{apiKey}/cache-stats

Очистить кэш для ключа
## DELETE /api/sdk/{apiKey}/clear-cache

# Запуск проектов
Оба проекта поддерживают запуск в Docker:

bash
Сборка и запуск (из корня проекта)
docker-compose up --build <br>
База данных: postgres://localhost:5432/weatherdb <br>
Приложение: http://localhost:8081

# Технологии
Java 21 <br>
Spring Boot 3.5.7 <br>
PostgreSQL <br>
JPA / Hibernate <br>
RestTemplate <br>
Docker / docker-compose <br>

# Структура пакетов
kameleoon.apiweather.sdk <br>
├── controller       → контроллеры <br>
├── dto              → Data Transfer Objects <br>
├── entity           → JPA-сущности <br>
├── exception        → Исключения и обработчик <br>
├── repository       → Spring Data JPA репозитории <br>
├── service          → Логика SDK и кэширования <br>
└── SdkWeather       → Управление экземплярами SDK <br>

