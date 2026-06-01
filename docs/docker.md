# Docker e Contentorizacao

## Visao Geral

O projeto utiliza Docker e Docker Compose para contentorizar todos os componentes do sistema. Cada microsservico, base de dados e servico de infraestrutura corre no seu proprio container, isolado e reprodutivel em qualquer ambiente.

## Containers

O `docker-compose.yml` na raiz do projeto define 10 containers:

| Container | Imagem | Porta | Funcao |
|-----------|--------|-------|--------|
| `nginx` | Build local (multi-stage) | 8000 | Reverse proxy + frontend SPA |
| `bff-service` | Build local | 8080 | Backend for Frontend |
| `ticket-service` | Build local | 8081 | Servico de bilheteira (pagamentos MB WAY) |
| `match-service` | Build local | 8082 | Jogos, fichas tecnicas e gestao de utilizadores |
| `statistics-service` | Build local | 8083 | Servico de estatisticas |
| `rabbitmq` | rabbitmq:3.13-management-alpine | 5672 / 15672 | Event bus assincrono (AMQP + UI de gestao) |
| `db-tickets` | postgres:16-alpine | interno | BD do ticket-service |
| `db-matches` | postgres:16-alpine | interno | BD do match-service |
| `db-statistics` | postgres:16-alpine | interno | BD do statistics-service |
| `keycloak` | quay.io/keycloak/keycloak:26.0 | 8180 | Autenticacao e autorizacao |

O Keycloak corre em modo `start-dev --import-realm` e importa o realm de
`infra/keycloak/realm-export.json` no arranque. **Nao tem volume persistente** —
o estado e re-importado a cada arranque a partir do ficheiro de realm.

### MB WAY — gateway externo (mock)

O `ticket-service` integra com um gateway de pagamentos MB WAY. Em
desenvolvimento e uma app mock (mock-mbway) que corre **fora** do Docker
Compose (tipicamente uma app Android ou processo no host), por isso **nao** ha
um container `mock-mbway` no `docker-compose.yml`. O `ticket-service`
referencia-o via variaveis de ambiente, com defaults apontados a
`host.docker.internal`:

| Var | Default (dev) | Uso |
|-----|---------------|-----|
| `MBWAY_GATEWAY_URL` | `http://host.docker.internal:8443` | Endpoint do gateway MB WAY |
| `MBWAY_WEBHOOK_BASE_URL` | `http://host.docker.internal:8081` | Base URL para callbacks do gateway |
| `MBWAY_TERMINAL_ID` | `47215` | Identificador do terminal |
| `MBWAY_CLIENT_ID` | `tessera-mock` | Identificador do cliente |

Para um dispositivo real na LAN, sobrepoem-se `MBWAY_GATEWAY_URL` e
`MBWAY_WEBHOOK_BASE_URL` com o IP do dispositivo.

## Estrategia de Build

### Servicos Backend (Abordagem Runtime-Only)

Os Dockerfiles dos servicos backend **nao compilam o codigo dentro do Docker**. Em vez disso:

1. O JAR e compilado na maquina local via `gradlew.bat clean build`
2. O Dockerfile apenas copia o JAR pre-compilado para a imagem

```dockerfile
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY build/libs/ticket-service-0.0.1-SNAPSHOT.jar /app/ticket-service.jar
EXPOSE 8081
CMD ["java", "-jar", "/app/ticket-service.jar"]
```

**Justificacao:** Inicialmente tentamos uma abordagem multi-stage build (compilar dentro do Docker com o Gradle wrapper), mas enfrentamos dois problemas:
- O Alpine Linux usa BusyBox `sh` que nao consegue interpretar os argumentos JVM do `gradlew` (`DEFAULT_JVM_OPTS='"-Xmx64m"'`) — o `sh` tratava `-Xmx64m` como nome de classe Java
- A instalacao de `bash` no Alpine resolvia o problema, mas aumentava o tempo de build significativamente (download do Gradle + dependencias dentro do container)

A abordagem runtime-only segue o padrao utilizado em projetos de producao e e significativamente mais rapida.

### NGINX (Multi-Stage Build)

O NGINX utiliza uma abordagem diferente — multi-stage build que compila o frontend React:

```dockerfile
FROM node:20-alpine AS frontend-build
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ .
RUN npm run build

FROM nginx:alpine
RUN rm /etc/nginx/conf.d/default.conf
COPY infra/nginx/nginx.conf /etc/nginx/nginx.conf
COPY --from=frontend-build /app/dist /usr/share/nginx/html
```

**Nota importante:** E necessario remover o `/etc/nginx/conf.d/default.conf` que vem com a imagem base do NGINX. Este ficheiro define um server block na porta 80 que entra em conflito com a nossa configuracao. Descobrimos este problema quando o Keycloak e o BFF retornavam 404 apesar de estarem acessiveis diretamente — o `default.conf` estava a interceptar os pedidos antes do nosso `nginx.conf`.

## Rede

Todos os containers partilham a mesma rede Docker bridge (`tessera-net`). A comunicacao interna utiliza DNS do Docker:

```yaml
networks:
  tessera-net:
    driver: bridge
```

Os servicos referenciam-se pelo nome do container (ex: `http://ticket-service:8081`, `http://keycloak:8180`).

## Volumes

As bases de dados e o RabbitMQ utilizam volumes nomeados para persistencia:

```yaml
volumes:
  db-tickets-data:
  db-matches-data:
  db-statistics-data:
  rabbitmq-data:
```

Os dados persistem entre reinicializacoes dos containers. Para apagar os dados, e necessario remover os volumes explicitamente (ver `scripts/run/reset.ps1`). O Keycloak nao tem volume — re-importa o realm a cada arranque.

## Variaveis de Ambiente

As configuracoes dos servicos sao injetadas via variaveis de ambiente no `docker-compose.yml`, substituindo os valores do `application.yml`:

```yaml
ticket-service:
  environment:
    SPRING_DATASOURCE_URL: jdbc:postgresql://db-tickets:5432/tessera_tickets
    SPRING_DATASOURCE_USERNAME: postgres
    SPRING_DATASOURCE_PASSWORD: postgres
```

O Spring Boot automaticamente mapeia `SPRING_DATASOURCE_URL` para `spring.datasource.url`.

Alem da base de dados e do RabbitMQ, o `match-service` recebe as credenciais
de administracao do Keycloak (usadas para gerir utilizadores via Admin API):

```yaml
match-service:
  environment:
    TESSERA_KEYCLOAK_ADMIN_BASE_URL: http://keycloak:8180
    TESSERA_KEYCLOAK_ADMIN_REALM: tessera
    TESSERA_KEYCLOAK_ADMIN_CLIENT_ID: tessera-bff
    TESSERA_KEYCLOAK_ADMIN_CLIENT_SECRET: change-me-in-production
```

Todos os servicos backend recebem ainda as credenciais do RabbitMQ
(`SPRING_RABBITMQ_HOST/PORT/USERNAME/PASSWORD`) e o JWKS do Keycloak via
`KEYCLOAK_JWKS_URI`.

## Dependencias e Health Checks

Os servicos backend dependem da sua base de dados e do RabbitMQ estarem
saudaveis antes de iniciar:

```yaml
depends_on:
  db-tickets:
    condition: service_healthy
  rabbitmq:
    condition: service_healthy
```

As bases de dados PostgreSQL definem health checks:

```yaml
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U postgres"]
  interval: 5s
  timeout: 3s
  retries: 5
```

O RabbitMQ define o seu proprio health check
(`rabbitmq-diagnostics check_port_connectivity`). O `match-service` depende
ainda do `keycloak` (`condition: service_started`).

## .dockerignore

Cada servico tem um `.dockerignore` que exclui ficheiros desnecessarios do contexto de build, mas permite o diretorio `build/libs/` (onde esta o JAR):

```
.gradle/
build/
!build/libs/
.idea/
*.iml
src/
gradle/
gradlew
gradlew.bat
*.gradle.kts
*.properties
```

## Porta 8000

O NGINX corre na porta 8000 em vez da porta 80 convencional. Isto deve-se a um conflito com outra instancia NGINX (Ubuntu/WSL) que ja ocupava a porta 80 na maquina de desenvolvimento. A configuracao pode ser alterada no `docker-compose.yml`:

```yaml
nginx:
  ports:
    - "8000:80"   # host:container
```

## Problemas Encontrados e Solucoes

| Problema | Causa | Solucao |
|----------|-------|---------|
| `gradlew: not found` no Alpine | Caracteres `\r\n` (Windows) no script | Adicionamos `sed -i 's/\r$//' gradlew` |
| `Could not find or load main class "-Xmx64m"` | BusyBox `sh` nao interpreta quotes do gradlew | Mudamos para abordagem runtime-only (build local) |
| NGINX retornava 404 para /api/ | `default.conf` da imagem base interceptava pedidos | `RUN rm /etc/nginx/conf.d/default.conf` no Dockerfile |
| Porta 80 ja em uso | Outra instancia NGINX (WSL) no host | Mudamos para porta 8000 |
| `.dockerignore` excluia o JAR | `build/` no ignore bloqueava `build/libs/*.jar` | Adicionamos `!build/libs/` como excecao |
