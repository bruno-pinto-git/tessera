# Seed da demo

O `seed-demo.ps1` **não é idempotente** para jogos, fichas e bilhetes — tem de
correr sempre contra um ambiente **limpo**. Clubes e estádios usam GetOrCreate,
mas os jogos/line-ups/eventos seriam duplicados numa segunda corrida.

## 1. Correr o seed

**Azure:**

```powershell
.\scripts\seed\seed-demo.ps1 `
  -BffUrl https://tessera.swedencentral.cloudapp.azure.com `
  -KeycloakUrl https://tessera.swedencentral.cloudapp.azure.com/auth `
  -AdminPassword 'admin' `
  -MbwayUrl https://tessera.swedencentral.cloudapp.azure.com `
  -RelaySecret 'dev-secret'
```

O seed vende bilhetes `PAID` (via MB WAY, confirmados por dentro simulando a
mock-mbway). Isso usa os endpoints `/api/v1/mbway/relay/poll` (precisa do
`X-Relay-Secret`) e `/api/v1/webhooks/mbway`, que **contornam o BFF**:
- **Local:** ficam no ticket-service em `http://localhost:8081` (default) com
  segredo `dev-secret` (default) — não é preciso passar nada.
- **Azure:** o Caddy encaminha-os no FQDN público → passa `-MbwayUrl <FQDN>` e o
  `-RelaySecret` igual ao `MBWAY_RELAY_SECRET` do `.env` da VM
  (`grep MBWAY_RELAY_SECRET ~/tessera/.env`).

**Local:**

```powershell
# 1. ir para a pasta do projeto
cd "C:\Users\bruno.pinto\Documents\ISEL\2025-2026\SV\Projecto e Seminario\tessera"

# 2. correr o seed
.\scripts\seed\seed-demo.ps1
```

## 2. Reset (limpar antes de voltar a seedar)

Apaga apenas as bases de dados de aplicação (`tickets`, `matches`,
`statistics`). O **Keycloak e o RabbitMQ ficam intactos** — os utilizadores e as
identidades não se perdem. Como os IDs dos clubes são `BIGSERIAL`, numa BD limpa
de raiz voltam a ser `1` (SU 1.º Dezembro) e `2` (Sport União Sintrense).

**Azure (por SSH na VM):**

```bash
cd ~/tessera

# parar serviços + BDs alvo
docker compose -f docker-compose.yml -f docker-compose.prod.yml stop \
  ticket-service match-service statistics-service db-tickets db-matches db-statistics

# remover os containers das BDs (senão o volume rm falha)
docker compose -f docker-compose.yml -f docker-compose.prod.yml rm -f \
  db-tickets db-matches db-statistics

# apagar os volumes (confirma os nomes com: docker volume ls | grep tessera)
docker volume rm tessera_db-tickets-data tessera_db-matches-data tessera_db-statistics-data

# levantar tudo (Flyway recria o schema vazio; sequências reiniciam a 1)
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

**Local:** o mesmo, sem o `-f docker-compose.prod.yml`.

Espera ~20-30 s até os serviços ficarem `healthy`
(`docker compose ... ps`) antes de correr o seed do passo 1.

## 3. Depois do seed — Onboarding (Keycloak)

Criar cada clube provisiona automaticamente os grupos
`/clubs/<id>/managers` e `/clubs/<id>/staff` no Keycloak. Falta apenas **atribuir
o gestor e o staff aos clubes** — ao vivo na demo (passo "Admin atribui gestor e
staff") ou à mão no admin do Keycloak, adicionando os utilizadores aos grupos
`/clubs/1/managers`, `/clubs/1/staff`, etc.

## Notas

- As **estatísticas repovoam-se sozinhas**: o seed cria jogos/bilhetes → publica
  eventos no RabbitMQ → o statistics-service consome e reconstrói as tabelas de
  leitura. Se saírem estranhas (mensagens antigas), reinicia o broker:
  `docker compose ... restart rabbitmq`.
- Alternativa ao drop de volumes: `TRUNCATE ... RESTART IDENTITY CASCADE` em cada
  BD via `psql` (mais rápido, mantém os containers) — mas o drop de volumes
  garante schema + sequências limpos.
