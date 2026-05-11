# bff-service (Backend for Frontend)

O BFF e a fachada HTTP unica que os clientes (React SPA, Android)
contactam. Reencaminha pedidos para os microsservicos internos
(ticket-service, match-service, statistics-service) e adapta-os
quando necessario.

- **Porta:** 8080
- **Base de dados:** nenhuma (servico stateless)
- **Stack:** Spring Boot 3.4.4, Kotlin 1.9.25, OAuth2 Resource Server
- **Base path:** `/api/v1` (recursos novos), `/api/tickets` (legado)

## Responsabilidades

1. **Validar JWT** emitido pelo Keycloak antes de qualquer escrita.
2. **Encaminhar pedidos** aos servicos internos preservando metodo,
   query string, body e o header `Authorization`.
3. **Agregar dados** (a desenvolver) — combinar respostas de
   match-service + ticket-service em payloads especificos para SPA
   e Android.
4. **Surfar erros** dos servicos downstream verbatim ao cliente,
   sem mascarar status nem mensagens Problem JSON.

## Padrao proxy

Todos os controllers seguem o mesmo padrao: delegar a um helper
generico `ProxyService` que constroi o URL downstream a partir do
URI do pedido original, copia o header `Authorization` (e
`Content-Type`), executa via `RestTemplate` e converte erros
`4xx/5xx` para `ResponseEntity` preservando body e status.

```kotlin
@GetMapping("/{id}")
fun get(req: HttpServletRequest) =
    proxy.forward(req, matchUrl, body = null)

@PostMapping
fun create(@RequestBody body: String, req: HttpServletRequest) =
    proxy.forward(req, matchUrl, body)
```

Ver `ProxyService.kt` para o helper e `ClubProxyController.kt` como
exemplo canonico.

## Controllers

| Controller | Endpoints |
|------------|-----------|
| `TicketProxyController` | `POST/GET /api/tickets`, `POST /api/tickets/validate` (legado) |
| `ClubProxyController` | `GET/POST /api/v1/clubs`, `GET/PATCH/DELETE /api/v1/clubs/{id}` |
| `VenueProxyController` | igual a Club mas para `/api/v1/venues` |
| `TeamProxyController` | `/api/v1/clubs/{clubId}/teams` + `/api/v1/teams/{id}` |
| `PlayerProxyController` | `/api/v1/teams/{teamId}/players` + `/api/v1/players/{id}` |
| `MatchProxyController` | `/api/v1/matches` + `/api/v1/matches/{id}` |
| `MatchSheetProxyController` | `/api/v1/matches/{matchId}/sheet/...` (lineup, occurrences, lock/unlock) |

A escolha por **controllers tipados** (em vez de um catch-all generico)
permite:

- Adicionar logging/metricas/auth checks por endpoint sem refactor
- Documentacao explicita das rotas suportadas
- Cobertura de testes individual

## Pass-through de Authorization

O `ProxyService` copia o header `Authorization` do pedido recebido
para o pedido downstream:

```kotlin
request.getHeader(HttpHeaders.AUTHORIZATION)?.let {
    set(HttpHeaders.AUTHORIZATION, it)
}
```

Resultado:

```
Browser ──Bearer JWT──> BFF (valida JWT) ──Bearer JWT (mesmo)──> match-service (re-valida + role check)
```

Cada servico downstream e ele proprio um resource server OAuth2 (ver
`docs/security.md`). O BFF nao precisa de extrair claims nem injetar
headers customizados.

## Endpoints publicos vs autenticados (BFF)

| Endpoint | Acesso |
|----------|--------|
| `GET /api/v1/clubs/**`, `/venues/**`, `/teams/**`, `/players/**`, `/matches/**` | publico |
| `POST/PATCH/DELETE` em qualquer recurso | autenticado |
| `/api/docs`, `/api/openapi.yaml`, `/swagger-ui/**` | publico |

O BFF nao faz role check; isso e responsabilidade do servico
downstream. So verifica que **um token valido** esta presente.

## Tratamento de erros downstream

Quando o servico downstream devolve 4xx ou 5xx, o `ProxyService`
captura `HttpStatusCodeException` e re-emite um `ResponseEntity` com:

- mesmo status
- body original do downstream (Problem JSON)
- `Content-Type` e `Location` propagados

Resultado: o cliente ve **exactamente** o que o match-service
produziria, sem o BFF a interferir.

```kotlin
} catch (e: HttpStatusCodeException) {
    ResponseEntity
        .status(e.statusCode)
        .headers(passthroughHeaders(e.responseHeaders))
        .body(e.responseBodyAsString)
}
```

## Erros do proprio BFF (401)

Quando o BFF rejeita um pedido por falta de JWT, devolve directamente
401 com Problem JSON (sem chegar ao downstream):

```json
{
  "type": "https://tessera/api/errors/unauthorized",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Authentication required.",
  "instance": "/api/v1/clubs"
}
```

Configurado em `SecurityConfig.problemAuthEntryPoint()`.

## Resource server config

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: ${KEYCLOAK_JWKS_URI:http://keycloak:8180/realms/tessera/protocol/openid-connect/certs}
```

Ver `docs/security.md` para detalhes (porque JWKS em vez de issuer-uri).

## Variaveis de ambiente

Configuradas no `docker-compose.yml`:

| Var | Valor (dev) | Uso |
|-----|-------------|-----|
| `SERVICES_TICKET_URL` | `http://ticket-service:8081` | Base URL do ticket-service |
| `SERVICES_MATCH_URL` | `http://match-service:8082` | Base URL do match-service |
| `SERVICES_STATISTICS_URL` | `http://statistics-service:8083` | Base URL do statistics-service |
| `KEYCLOAK_JWKS_URI` | `http://keycloak:8180/realms/tessera/.../certs` | JWKS endpoint |

## Swagger UI

A documentacao OpenAPI hand-written esta disponivel em:

- **`http://localhost:8080/api/docs`** (Swagger UI)
- A spec serve em `/api/openapi.yaml`

O springdoc esta configurado para **nao** gerar OpenAPI a partir dos
controllers do BFF — usa apenas o YAML escrito em `docs/api/`. Razao:
o BFF e um proxy, nao queremos publicar uma spec gerada que nao
reflicta o contrato real.

## Trabalho futuro

- [ ] **Aggregation endpoints**: combinar `GET /matches/{id}` do
      match-service com info do `Event` do ticket-service (precos,
      disponibilidade) para fans, num so request
- [ ] **Cache** de respostas read-heavy (lista de clubes, jogos do dia)
- [ ] **Rate limiting** por utilizador
- [ ] **Telemetria** (request id propagation, distributed tracing
      com Zipkin/OTel)
- [ ] **Generated clients**: gerar SDKs TypeScript e Kotlin a partir
      do OpenAPI para SPA e Android
