# Phrases relay

Минимальный WebSocket relay для Phrases. Он пересылает только ID фразы всем подключённым клиентам и не хранит звуки. Minecraft-сервер и его адрес не имеют значения.

## Render

Создай на Render бесплатный Web Service из папки `relay`:

- Build command: `npm install`
- Start command: `npm start`
- Plan: Free

После деплоя скопируй URL сервиса, например `https://phrases-relay.onrender.com`, замени `https://` на `wss://` и создай у себя файл `.minecraft/config/phrases-relay.properties`:

```properties
url=wss://phrases-relay.onrender.com
```
