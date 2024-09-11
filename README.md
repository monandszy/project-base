# Project Base

**Written:** 2024.06 - 2024.08

The root repository for my Java projects, it aggregates gradle, git and docker configurations. 
The idea is to maintain multiple Spring applications and seamlessly deploy them on a home server  
Each module has its own repository, with unique versions and dependencies. It works, but all of this is still in the experimental stage, so don't get too attached.

### Current Docker Networks

#### 1. Observability

- **Description:** This network includes the OpenTelemetry (OTel) collector. Applications send traces, metrics, and logs to it.
- **Current Status:** Storage, processing, and visualization stack has not been chosen yet, I am considering the Elastic stack.
- **Past Solutions:** Previously, I used the Grafana, Loki, Tempo, and Prometheus stack. However, creating dashboards manually proved to be too much of an overhead for one person.

#### 2. Data

- **Description:** This network includes PostgreSQL.
- **Initialization:** Contains an `init.sql` file that initializes databases for the modules.

#### 3. Networking

- **Description:** This network includes Cloudflared and Caddy, allowing applications to be exposed on the internet or local network. Is meant to be used only on the server.
- **Future Plans:** Develop a comprehensive link tree as a default for easier access to container services and observability tools.

### Base Management Tasks

#### 1. `generateBaseCompose`

- Generates Docker Compose files for observability and data from template files and versions.

``` sh
./gradlew generateBaseCompose 
```

#### 2. `composeUp or composeDown`

- Brings up or down Docker services in provided project name (unanimous with network).

- `-Ppname=<dockerProjectName>`: Specifies the compose file to be used.

``` sh
./gradlew composeUp -Ppname=data 
```

``` sh
./gradlew composeDown -Ppname=data 
```

#### 3. `composeBaseUp or composeBaseDown`

- Brings up or down observability and data containers. Networking has to be managed separately.

``` sh
./gradlew composeBaseUp 
```

``` sh
./gradlew composeBaseDown 
```

in essence, an experimental mess