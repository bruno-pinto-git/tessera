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
| API (via BFF) | http://localhost:8000/api |
| Keycloak Admin | http://localhost:8180/admin |
| Keycloak Realm Info | http://localhost:8000/realms/tessera |

### Testar Autenticacao

Aceder a `http://localhost:8180/admin` e fazer login:
- **Realm master:** admin / admin

Para testar os utilizadores do realm tessera:

| Username | Password | Perfil |
|----------|----------|--------|
| admin | admin | Administrador do clube |
| staff | staff | Staff do clube |
| adepto | adepto | Adepto |

## 6. Comandos Uteis

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
├── frontend/                 # React SPA
├── keycloak/                 # Configuracao do Keycloak
├── nginx/                    # Configuracao e Dockerfile NGINX
├── scripts/                  # Scripts de build e execucao
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
O realm so e importado na primeira execucao. Para re-importar:
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
