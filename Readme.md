# Project Base

## Overview
This repository is a small, multi-module Spring Boot starter using Gradle (Kotlin DSL) and Spring Modulith. Its purpose is to organize application modules and provide supporting Docker Compose stacks for data, networking, and observability.

## Repository layout
- `_modules/` — subprojects included dynamically by `settings.gradle.kts`. The module sources are maintained in separate repositories.
- `docker/` — Docker Compose files and configuration for local stacks (data, networking, observability).

## Module repositories
- Template app: https://github.com/monandszy/app-template
- Conversation app: https://github.com/monandszy/conversation-app

## Docker stacks
- `compose-data.yml` — data services: Postgres and RabbitMQ.
- `compose-networking.yml` — Caddy (reverse proxy) and Cloudflared (Cloudflare tunnel).
- `compose-observability.yml` — OTEL collector, Jaeger, OpenSearch, Prometheus, Data Prepper.

To start a stack:
```
docker-compose -p {stack} -f docker\compose-{stack}.yml up -d
```

## Notes about `docker/modules` templates:
- `docker/modules/compose-dev.yml` and `compose-prod.yml` are templates using variables `${project-name}`, `${project-version}`, and `${default-network}`. Based on them a compose file can be created for a module.