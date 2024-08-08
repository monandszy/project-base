# Project Base

The root repository for my Java projects, created between June 2024 and August 2024. It aggregates configurations and
functions as shared container volume storage.
Each module has its own repository, with unique versions and selective dependency implementation.

### Usage Idea

This project enables you to maintain multiple Spring applications and seamlessly deploy them on a home server.
It works, but all of this is still pretty much in the experimental stage, so don't get too attached.

### Current Docker Networks

#### 1. Observability

- **Description:** This network includes the OpenTelemetry (OTel) collector. Applications send traces, metrics, and logs
  to it.
- **Current Status:** Storage, processing, and visualization stack has not been chosen yet, I am considering the Elastic
  stack.
- **Past Solutions:** Previously, I used the Grafana, Loki, Tempo, and Prometheus stack. However, creating dashboards
  manually proved to be too much of an overhead for one person.

#### 2. Data

- **Description:** This network includes PostgreSQL.
- **Initialization:** Contains an `init.sql` file that initializes databases for the modules.

#### 3. Networking

- **Description:** This network includes Cloudflared and Caddy, allowing applications to be exposed on the internet or
  local network. Is meant to be used only on the server.
- **Future Plans:** Develop a comprehensive link tree as a default for easier access to container services and
  observability tools.

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

#### 4. `runTunnel`

- Runs the Cloudflare tunnel using the `run_tunnel.sh` script.
- Remember to check if the tunnel has been registered properly.

``` sh
./gradlew runTunnel 
```

### Module Management Tasks

- Location: gradle/util/docker.gradle.kts

#### 1. `dockerBuild`

- Creates a docker image of a module (a fat jar).

``` sh
./gradlew app-template:dockerBuild
```

#### 2. `moduleProdUp, moduleProdDown, moduleDevUp, moduleDevDown`

- Brings up or down a selected compose network. On the server remember to be on the
  respective branch (dev for dev spring profile, master for prod spring profile).

``` sh
./gradlew app-template:moduleProdUp
```

``` sh
./gradlew app-template:moduleDevUp
```

### Git Flow Versioning Tasks

- Location: gradle/util/git.gradle.kts

- Each module and the base have a project.version file.
- These tasks are a wrapper for the Git Flow plugin, which also manages the version.
- In case of a merge conflict the task will be aborted, and you might have to update the
  version manually during the merge, or rerun the task again after.
- Read more about what happens under the hood here: https://github.com/nvie/gitflow/wiki/Command-Line-Arguments
- While git flow is not recommended in business environments, it works quite well in a solo project. You can focus on
  selected features, and test a release on a separate branch.

#### 1. `featureStart -Pbranch=<name>, featureFinish -Pbranch=<name>`

``` sh
./gradlew featureStart -Pbranch=tempFeature
```

``` sh
./gradlew featureFinish -Pbranch=tempFeature
```

#### 2. `releaseStart, releaseFinish`

``` sh
./gradlew releaseStart
```

``` sh
./gradlew releaseFinish`
```

#### 3. `hotfixStart -Pbranch=<name>, hotfixFinish -Pbranch=<name>`

``` sh
./gradlew hotfixStart -Pbranch=tempFeature
```

``` sh
./gradlew hotfixFinish -Pbranch=tempFeature
```

### Misc Tasks

printVersion - prints the current version of the module as specified in the project.version file