# Guia de Inicio Rapido

Este guia descreve os passos para configurar e executar o projeto Tessera num ambiente de desenvolvimento.

## Pre-requisitos

| Software | Versao | Download |
|----------|--------|----------|
| JDK | 21 | https://adoptium.net/ |
| Node.js | 20+ | https://nodejs.org/ |
| Docker Desktop | Mais recente | https://www.docker.com/products/docker-desktop/ |
| Git | Mais recente | https://git-scm.com/ |
| IntelliJ IDEA | 2024+ (recomendado) | https://www.jetbrains.com/idea/ |

## 1. Clonar o Repositorio

```bash
git clone https://github.com/bruno-pinto-git/tessera.git
cd tessera
```

## 2. Configurar o JDK

Verificar que o JDK 21 esta instalado:

```powershell
java -version
# java version "21.0.1" ...
```

Se o JDK estiver num caminho diferente de `C:\Program Files\Java\jdk-21`, atualizar o `gradle.properties` em cada servico:

```
backend/bff-service/gradle.properties
backend/ticket-service/gradle.properties
backend/match-service/gradle.properties
backend/statistics-service/gradle.properties
```

Alterar a linha:
```properties
org.gradle.java.home=C:\\Program Files\\Java\\jdk-21
```

## 3. Abrir no IntelliJ IDEA

1. **File > Open** → selecionar `tessera/backend/`
2. IntelliJ deteta o `settings.gradle.kts` e apresenta todos os modulos
3. Clicar **Load Gradle Project** quando solicitado
4. Aguardar o sync do Gradle

Os 4 servicos aparecem como modulos:
- `bff-service`
- `ticket-service`
- `match-service`
- `statistics-service`

## 4. Iniciar o Sistema

Abrir uma consola PowerShell na raiz do projeto e executar:

```powershell
.\scripts\run\start.ps1
```

Este script:
1. Compila todos os JARs (Gradle)
2. Constroi as imagens Docker
3. Inicia todos os containers

O primeiro arranque demora mais tempo (download de imagens Docker).

## 5. Verificar

Apos o arranque, os seguintes servicos estao disponiveis:

| Servico | URL |
|---------|-----|
| Aplicacao Web (SPA) | http://localhost:8000 |
| Area de administracao (SPA) | http://localhost:8000/admin |
| API (via BFF) | http://localhost:8000/api |
| Keycloak Admin | http://localhost:8180/admin |
| Keycloak Realm Info | http://localhost:8180/realms/tessera |
| RabbitMQ Management | http://localhost:15672 (tessera/tessera) |

> O NGINX (`:8000`) so encaminha `/api/*` para o BFF e serve a SPA. O Keycloak
> NAO e proxiado pelo NGINX — e acedido diretamente em `:8180` (a SPA usa
> `VITE_KEYCLOAK_URL`; os servicos backend usam `http://keycloak:8180`).

### Testar Autenticacao

Aceder a `http://localhost:8180/admin` e fazer login:
- **Realm master:** admin / admin

Para testar os utilizadores do realm tessera:

| Username | Password | Papel |
|----------|----------|--------|
| admin | admin | platform-admin |
| gestor | gestor | club-manager |
| staff | staff | staff |
| adepto | adepto | fan |

### Obter um JWT manualmente (PowerShell)

```powershell
function Get-JWT($username, $password) {
    $r = Invoke-RestMethod -Method POST `
      -Uri "http://localhost:8180/realms/tessera/protocol/openid-connect/token" `
      -ContentType "application/x-www-form-urlencoded" `
      -Body @{
          client_id  = "tessera-web"
          username   = $username
          password   = $password
          grant_type = "password"
      }
    return $r.access_token
}
$token = Get-JWT "admin" "admin"

# Usar o token
Invoke-RestMethod -Method POST -Uri http://localhost:8000/api/v1/clubs `
  -ContentType "application/json" -Body '{"name":"Teste"}' `
  -Headers @{ Authorization = "Bearer $token" }
```

## 6. Smoke tests (.http)

Os ficheiros em `docs/http-tests/` automatizam dezenas de pedidos com
asserts (status code, body, etc.). Funcionam no **IntelliJ IDEA**
nativamente e no **VS Code** com a extensao [REST Client](https://marketplace.visualstudio.com/items?itemName=humao.rest-client).

```
docs/http-tests/
├── http-client.env.json         # Environments (local-direct, local-via-nginx)
├── 00-auth.http                 # Obtem JWTs e guarda como variaveis globais
├── 01-clubs.http
├── 02-venues.http
├── 03-teams.http
├── 04-players.http
├── 05-matches.http
├── 06-match-sheets.http
├── 10-bff-passthrough.http      # Valida fluxo via BFF
└── 99-rbac-checks.http          # Tests negativos (401, 403)
```

**Como correr (IntelliJ):**

1. Abrir `docs/http-tests/00-auth.http`
2. Canto superior direito → escolher environment `local-direct` (servicos
   directos nas suas portas) ou `local-via-nginx` (tudo via NGINX :8000)
3. Clicar ▶ ao lado de cada bloco — ou correr o ficheiro todo
4. Correr os ficheiros pela ordem (`00 → 01 → ...`); os tokens
   guardados pelo `00-auth.http` sao referenciados pelos restantes via
   `{{adminToken}}`

Total de aprox. **56 asserts** automaticos.

## 7. Popular Dados de Exemplo (seed)

Os scripts em `scripts/seed/` autenticam-se no Keycloak como `admin` e fazem
POST aos endpoints do match-service (via BFF, `http://localhost:8000`) para
popular a plataforma com dados reais:

```powershell
.\scripts\seed\seed-clubs.ps1     # clubes (Wikidata)
.\scripts\seed\seed-venues.ps1    # estadios da Liga 3 2024-25 (Wikidata)
.\scripts\seed\seed-liga3.ps1     # clubes/equipas da Liga 3
```

Todos aceitam `-DryRun` para mostrar o que seria enviado sem fazer POST.

## 8. Comandos Uteis

### Parar tudo
```powershell
.\scripts\run\stop.ps1
```

### Reset completo (apaga dados)
```powershell
.\scripts\run\reset.ps1
```

### Recompilar um servico especifico
```powershell
.\scripts\run\reset_ticket_service.ps1
```

### Recompilar o frontend
```powershell
.\scripts\run\reset_nginx.ps1
```

### Ver logs de um servico
```powershell
docker compose logs -f ticket-service
```

### Ver estado dos containers
```powershell
docker compose ps
```

## Estrutura do Projeto

```
tessera/
├── android/                  # App Android
├── backend/
│   ├── bff-service/          # Backend for Frontend (porta 8080)
│   ├── ticket-service/       # Bilheteira (porta 8081)
│   ├── match-service/        # Jogos e fichas tecnicas (porta 8082)
│   └── statistics-service/   # Estatisticas (porta 8083)
├── mock-mbway/               # Mock do gateway de pagamento MB WAY (dev)
├── frontend/                 # React SPA
├── infra/                    # Infra-as-config (imagens off-the-shelf)
│   ├── keycloak/             # Configuracao do Keycloak
│   └── nginx/                # Configuracao e Dockerfile NGINX
├── scripts/                  # Scripts de build, execucao e seed
├── docs/                     # Documentacao
└── docker-compose.yml        # Orquestracao Docker
```

## Resolucao de Problemas

### "Build failed: Unable to establish loopback connection"
Problema de rede da maquina. Executar o build diretamente no IntelliJ (Gradle > Tasks > build) ou tentar novamente.

### Containers nao iniciam
Verificar que o Docker Desktop esta a correr. Verificar logs:
```powershell
docker compose logs -f
```

### CORS errors no browser
Verificar que esta a aceder via `http://localhost:8000` (NGINX) e nao diretamente aos servicos.

### Keycloak nao importa o realm
O Keycloak corre `start-dev --import-realm` sem volume persistente, pelo que o
realm `tessera` (`infra/keycloak/realm-export.json`) e (re)importado sempre que
o container arranca — qualquer alteracao feita na consola e efemera. Para
forcar uma reimportacao limpa:
```powershell
docker compose stop keycloak
docker compose rm -f keycloak
docker compose up -d keycloak
```

### Porta 8000 em uso
Verificar se outro processo usa a porta:
```powershell
netstat -ano | findstr ":8000"
```
Alterar a porta no `docker-compose.yml` se necessario.
