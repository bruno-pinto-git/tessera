# Scripts de Desenvolvimento

## Visao Geral

O projeto inclui scripts PowerShell (`.ps1`) para automatizar a compilacao, a
gestao dos containers Docker, o povoamento de dados (seed) e testes de fumo.
Os scripts estao organizados em quatro diretorios:

```
scripts/
├── build/          # Compilacao de JARs
│   ├── buildAll.ps1
│   ├── buildBffService.ps1
│   ├── buildTicketService.ps1
│   ├── buildMatchService.ps1
│   └── buildStatisticsService.ps1
├── run/            # Gestao de containers
│   ├── start.ps1
│   ├── stop.ps1
│   ├── reset.ps1
│   ├── reset_bff_service.ps1
│   ├── reset_ticket_service.ps1
│   ├── reset_match_service.ps1
│   ├── reset_statistics_service.ps1
│   └── reset_nginx.ps1
├── seed/           # Povoamento de dados via API (Wikidata -> BFF)
│   ├── seed-clubs.ps1
│   ├── seed-liga3.ps1
│   └── seed-venues.ps1
└── test/           # Testes de fumo end-to-end
    └── test-scope-authz.ps1
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

## Scripts de Seed

Os scripts em `scripts/seed/` povoam a plataforma com dados reais obtidos da
Wikidata. Todos autenticam-se no Keycloak como `admin` (cliente `tessera-web`)
e fazem POST para os endpoints do BFF (por defeito `http://localhost:8000`).
Aceitam o switch `-DryRun` para mostrar o que seria enviado sem escrever, e
tratam o 409 (ja existe) como "skip". O body e enviado em UTF-8 para preservar
os acentos dos nomes.

### seed-clubs.ps1

Importa clubes de futebol portugueses filtrados por competicao via SPARQL
(default: Campeonato Nacional de Seniores, `Q13668768`). Cada clube traz nome,
ano de fundacao (P571) e crest (P154). POST para `/api/v1/clubs`.

```powershell
.\scripts\seed\seed-clubs.ps1
.\scripts\seed\seed-clubs.ps1 -DryRun
.\scripts\seed\seed-clubs.ps1 -CompetitionQId Q754488   # Liga Portugal 2
.\scripts\seed\seed-clubs.ps1 -Limit 20
```

### seed-liga3.ps1

Importa os 20 clubes da Liga 3 (2024-25) a partir de uma lista curada de QIds
(a Liga 3 esta mal modelada na Wikidata, por isso o roster e fixado a mao).
Resolve nome/fundacao/crest por QId em tempo de execucao. POST para
`/api/v1/clubs`.

```powershell
.\scripts\seed\seed-liga3.ps1
.\scripts\seed\seed-liga3.ps1 -DryRun
.\scripts\seed\seed-liga3.ps1 -Limit 5
```

### seed-venues.ps1

Importa os estadios (home venues) dos clubes da Liga 3. Uma so query SPARQL
segue P115 (home venue) e le nome, capacidade (P1083) e localizacao (P131).
Apenas ~11 dos 20 clubes tem venue tagueado na Wikidata; os restantes sao
ignorados. POST para `/api/v1/venues`.

```powershell
.\scripts\seed\seed-venues.ps1
.\scripts\seed\seed-venues.ps1 -DryRun
```

## Scripts de Teste

### test-scope-authz.ps1

Teste de fumo end-to-end da autorizacao com escopo de clube (scope-aware authz)
ligada aos grupos do Keycloak (`/clubs/<id>/managers`) e ao
`ClubAuthorizationService`. Requer o stack Docker a correr. O script:

1. Obtem tokens (admin no realm `tessera` + admin master via `admin-cli`)
2. Garante que existem dois clubes de teste (cria-os se preciso)
3. Resolve o id do utilizador `gestor` e o grupo `/clubs/<X>/managers`
4. Corre a matriz: anonimo PATCH => 401; gestor sem grupo POST em X => 403;
   gestor no grupo POST em X => 201/409; gestor POST em Y => 403; admin
   POST em Y => 201/409
5. Remove a membership no fim para que reexecucoes fiquem limpas

```powershell
.\scripts\test\test-scope-authz.ps1
```

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
