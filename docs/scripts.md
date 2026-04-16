# Scripts de Desenvolvimento

## Visao Geral

O projeto inclui scripts PowerShell (`.ps1`) para automatizar a compilacao e gestao dos containers Docker. Os scripts estao organizados em dois diretorios:

```
scripts/
├── build/          # Compilacao de JARs
│   ├── buildAll.ps1
│   ├── buildBffService.ps1
│   ├── buildTicketService.ps1
│   ├── buildMatchService.ps1
│   └── buildStatisticsService.ps1
└── run/            # Gestao de containers
    ├── start.ps1
    ├── stop.ps1
    ├── reset.ps1
    ├── reset_bff_service.ps1
    ├── reset_ticket_service.ps1
    ├── reset_match_service.ps1
    ├── reset_statistics_service.ps1
    └── reset_nginx.ps1
```

## Scripts de Build

### buildAll.ps1

Compila todos os servicos em sequencia e apresenta um resumo com o estado de cada build.

```powershell
.\scripts\build\buildAll.ps1
```

Saida exemplo:
```
========================================
Building All Projects
========================================

[1/4] Building BffService...
  BffService build completed successfully
[2/4] Building TicketService...
  TicketService build completed successfully
...

Build Summary
========================================
BffService : SUCCESS
TicketService : SUCCESS
MatchService : SUCCESS
StatisticsService : SUCCESS

Total: 4 | Success: 4 | Failed: 0
```

### buildXxxService.ps1

Compila um servico individual. Cada script:

1. Navega para o diretorio do servico
2. Executa `gradlew.bat clean build -x test --stacktrace`
3. Reporta sucesso ou falha

```powershell
.\scripts\build\buildTicketService.ps1
```

## Scripts de Execucao

### start.ps1 — Arranque Completo

Compila todos os JARs e inicia todos os containers:

```powershell
.\scripts\run\start.ps1
```

Fluxo:
1. Executa `buildAll.ps1` para compilar todos os servicos
2. Executa `docker compose up -d --build`
3. Apresenta o estado dos containers e URLs de acesso

### stop.ps1 — Paragem

Para todos os containers. Os volumes de dados sao preservados:

```powershell
.\scripts\run\stop.ps1
```

### reset.ps1 — Reset Completo

Destroi tudo (containers, volumes, imagens locais) e reconstroi do zero:

```powershell
.\scripts\run\reset.ps1
```

Fluxo:
1. `docker compose down -v` — para containers e remove volumes
2. `docker compose down -v --rmi local --remove-orphans` — remove imagens locais
3. Compila todos os JARs
4. `docker compose up -d --build` — reconstroi e inicia tudo

**Usar quando:** Base de dados corrompida, alteracoes ao schema Flyway, re-importacao do realm Keycloak.

### reset_xxx_service.ps1 — Reset Individual

Reconstroi e reinicia um servico especifico sem afetar os restantes:

```powershell
.\scripts\run\reset_ticket_service.ps1
```

Fluxo:
1. Para e remove o container do servico
2. Compila o JAR do servico
3. Reconstroi a imagem Docker
4. Inicia o container
5. Reinicia o NGINX (re-resolve upstreams)
6. Apresenta os logs do servico (Ctrl+C para sair)

**Reinicio do NGINX:** Todos os scripts de reset reiniciam o NGINX no final. Isto garante que o NGINX re-resolve os enderecos DNS dos containers (que podem mudar apos recriacao).

### reset_nginx.ps1 — Reset do Frontend

Reconstroi a imagem NGINX (que inclui o build do React) e reinicia:

```powershell
.\scripts\run\reset_nginx.ps1
```

**Usar quando:** Alteracoes ao codigo do frontend React ou a configuracao do NGINX.

## Notas

### Execucao dos Scripts

Os scripts devem ser executados a partir da raiz do projeto numa consola PowerShell:

```powershell
cd C:\...\tessera
.\scripts\run\start.ps1
```

Se encontrar erros de politica de execucao:
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run\start.ps1
```

### Configuracao do JDK

Os scripts de build utilizam o JDK configurado em `gradle.properties` de cada servico:

```properties
org.gradle.java.home=C:\\Program Files\\Java\\jdk-21
```

Certifique-se de que o JDK 21 esta instalado neste caminho, ou altere o valor em todos os ficheiros `gradle.properties`.
