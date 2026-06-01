# NGINX — Reverse Proxy

## Visao Geral

O NGINX atua como ponto de entrada unico do sistema, servindo duas funcoes:

1. **Servir o frontend React** — ficheiros estaticos (HTML, CSS, JS) da SPA
2. **Proxy reverso para a API** — encaminha pedidos `/api/*` para o BFF

O NGINX **nao** faz proxy ao Keycloak. A SPA e os servicos backend
contactam o Keycloak diretamente em `:8180` (ver seccao "Keycloak Direto").

## Porque NGINX?

A alternativa seria utilizar um Spring Cloud Gateway (servico Java dedicado ao routing). Optamos pelo NGINX porque:

- Ja fazia parte da arquitetura proposta do projeto
- E extremamente eficiente a servir ficheiros estaticos (o React SPA e um conjunto de ficheiros HTML/JS/CSS)
- Consome poucos recursos (comparado com um processo JVM adicional)
- Evita ter um container extra so para routing

## Configuracao

O ficheiro `infra/nginx/nginx.conf` define toda a configuracao:

### Upstreams

```nginx
upstream bff {
    server bff-service:8080;
}
```

O nome `bff-service` corresponde ao nome do container no Docker Compose. O Docker DNS resolve automaticamente para o IP interno. Existe apenas **um** upstream — o BFF.

### Routing

| Caminho | Destino | Descricao |
|---------|---------|-----------|
| `/api/*` | BFF (porta 8080) | Todas as chamadas da API |
| `/*` | Ficheiros estaticos | React SPA com fallback para index.html |

Qualquer caminho que **nao** comece por `/api/` cai no `location /` e e
servido pela SPA (com fallback para `index.html`). Isto inclui as rotas
proprias do React em `/admin/*` (area de administracao da plataforma).

### SPA Routing (Client-Side)

A configuracao inclui `try_files` para suportar o routing do React Router:

```nginx
location / {
    root  /usr/share/nginx/html;
    index index.html;
    try_files $uri $uri/ /index.html;
}
```

Isto garante que qualquer URL (ex: `/matches/123`) serve o `index.html`, permitindo ao React Router gerir a navegacao no browser.

### Proxy Headers

O location `/api/` inclui headers que preservam a informacao do pedido original:

```nginx
proxy_set_header Host              $host;
proxy_set_header X-Real-IP         $remote_addr;
proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto $scheme;
```

Estes headers permitem que o BFF saiba o IP real do cliente e o protocolo original (HTTP/HTTPS).

## Fluxo de um Pedido

```
1. Browser faz GET http://localhost:8000/api/tickets
2. NGINX recebe na porta 8000 (mapeada para porta 80 no container)
3. Faz match com location /api/
4. Proxy_pass para http://bff-service:8080/api/tickets
5. BFF processa e responde
6. NGINX devolve a resposta ao browser
```

## Keycloak Direto — Porque Removemos o Proxy

Numa versao anterior, o NGINX tambem fazia proxy ao Keycloak, mapeando
diretamente os caminhos do Keycloak 26 (`/realms/`, `/admin/`, `/resources/`,
`/js/`) para um upstream `keycloak`. Removemos **todos** esses blocos.

**Motivo:** o location `/admin/` do Keycloak fazia "shadow" das rotas
proprias da SPA em `/admin/*` (a area de administracao em React). Um carregamento
direto ou refresh de paginas como `/admin`, `/admin/teams` ou
`/admin/clubs/:id` deixava de servir o `index.html` e batia na consola de
administracao do Keycloak. Como a SPA gere `/admin/*` no cliente (React
Router), o caminho tem de cair sempre no `location /` com `try_files`.

**Resultado:** o NGINX so faz proxy a `/api/`. O Keycloak e contactado
**diretamente** em `:8180`:

- O browser/SPA usa `VITE_KEYCLOAK_URL` (ver `frontend/.env.production`)
  para falar com `http://<host>:8180` diretamente.
- Os servicos backend usam o DNS interno da rede Docker
  (`http://keycloak:8180`) para obter o JWKS e gerir utilizadores.

Nota historica: nas versoes do Keycloak < 25 os endpoints tinham o prefixo
`/auth/` (ex: `/auth/realms/tessera`); o Keycloak 25+ removeu-o. Hoje isto e
irrelevante para o NGINX, que ja nao encaminha nada para o Keycloak.

## Frontend Embutido no Container

Os ficheiros estaticos do React SPA sao compilados e copiados para dentro da imagem NGINX durante o build (multi-stage). Isto significa que:

- Nao e necessario um container separado para o frontend
- O NGINX serve os ficheiros diretamente do disco (maximo desempenho)
- O frontend e versionado junto com a configuracao do NGINX

Ver `infra/nginx/Dockerfile` e a documentacao Docker para detalhes do processo de build.

## Problemas Encontrados e Solucoes

| Problema | Causa | Solucao |
|----------|-------|---------|
| 404 em `/api/` | `default.conf` da imagem NGINX interceptava pedidos | Removemos o ficheiro no Dockerfile |
| Consola Keycloak em vez da SPA em `/admin/*` | O proxy `/admin/` do Keycloak fazia shadow das rotas React `/admin/*` | Removemos os blocos de proxy ao Keycloak; usa-se Keycloak direto em `:8180` |
| Porta 80 em conflito | Outra instancia NGINX no host (WSL) | Usamos porta 8000 no docker-compose |
| CORS ao chamar API | Frontend chamava `localhost:8080` diretamente | Mudamos para path relativo `/api` (passa pelo NGINX) |
