# NGINX — Reverse Proxy

## Visao Geral

O NGINX atua como ponto de entrada unico do sistema, servindo tres funcoes:

1. **Servir o frontend React** — ficheiros estaticos (HTML, CSS, JS) da SPA
2. **Proxy reverso para a API** — encaminha pedidos `/api/*` para o BFF
3. **Proxy reverso para o Keycloak** — encaminha pedidos de autenticacao

## Porque NGINX?

A alternativa seria utilizar um Spring Cloud Gateway (servico Java dedicado ao routing). Optamos pelo NGINX porque:

- Ja fazia parte da arquitetura proposta do projeto
- E extremamente eficiente a servir ficheiros estaticos (o React SPA e um conjunto de ficheiros HTML/JS/CSS)
- Consome poucos recursos (comparado com um processo JVM adicional)
- Evita ter um container extra so para routing

## Configuracao

O ficheiro `nginx/nginx.conf` define toda a configuracao:

### Upstreams

```nginx
upstream bff {
    server bff-service:8080;
}

upstream keycloak {
    server keycloak:8180;
}
```

Os nomes `bff-service` e `keycloak` correspondem aos nomes dos containers no Docker Compose. O Docker DNS resolve automaticamente para os IPs internos.

### Routing

| Caminho | Destino | Descricao |
|---------|---------|-----------|
| `/api/*` | BFF (porta 8080) | Todas as chamadas da API |
| `/realms/*` | Keycloak (porta 8180) | Endpoints de autenticacao OIDC |
| `/admin/*` | Keycloak (porta 8180) | Consola de administracao do Keycloak |
| `/resources/*` | Keycloak (porta 8180) | Assets do Keycloak (CSS, JS) |
| `/js/*` | Keycloak (porta 8180) | JavaScript do Keycloak |
| `/*` | Ficheiros estaticos | React SPA com fallback para index.html |

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

Cada location de proxy inclui headers que preservam a informacao do pedido original:

```nginx
proxy_set_header Host              $host;
proxy_set_header X-Real-IP         $remote_addr;
proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto $scheme;
```

Estes headers permitem que os servicos backend saibam o IP real do cliente e o protocolo original (HTTP/HTTPS).

### Buffers do Keycloak

Os endpoints do Keycloak requerem buffers maiores porque os tokens JWT podem ser grandes:

```nginx
proxy_buffer_size        128k;
proxy_buffers            4 256k;
proxy_busy_buffers_size  256k;
```

Sem esta configuracao, o NGINX pode rejeitar respostas do Keycloak com erro 502.

## Fluxo de um Pedido

```
1. Browser faz GET http://localhost:8000/api/tickets
2. NGINX recebe na porta 8000 (mapeada para porta 80 no container)
3. Faz match com location /api/
4. Proxy_pass para http://bff-service:8080/api/tickets
5. BFF processa e responde
6. NGINX devolve a resposta ao browser
```

## Keycloak 26 — Sem Prefixo /auth/

Nas versoes anteriores do Keycloak (< 25), todos os endpoints tinham o prefixo `/auth/` (ex: `/auth/realms/tessera`). A partir da versao 25, o Keycloak removeu este prefixo.

Inicialmente configuramos o NGINX com `location /auth/` e os pedidos retornavam 404. A solucao foi mapear diretamente os caminhos do Keycloak 26: `/realms/`, `/admin/`, `/resources/`, `/js/`.

## Frontend Embutido no Container

Os ficheiros estaticos do React SPA sao compilados e copiados para dentro da imagem NGINX durante o build (multi-stage). Isto significa que:

- Nao e necessario um container separado para o frontend
- O NGINX serve os ficheiros diretamente do disco (maximo desempenho)
- O frontend e versionado junto com a configuracao do NGINX

Ver `nginx/Dockerfile` e a documentacao Docker para detalhes do processo de build.

## Problemas Encontrados e Solucoes

| Problema | Causa | Solucao |
|----------|-------|---------|
| 404 em `/realms/` e `/api/` | `default.conf` da imagem NGINX interceptava pedidos | Removemos o ficheiro no Dockerfile |
| 404 no Keycloak via `/auth/` | Keycloak 26 removeu o prefixo `/auth/` | Mapeamos `/realms/`, `/admin/`, `/resources/`, `/js/` |
| Porta 80 em conflito | Outra instancia NGINX no host (WSL) | Usamos porta 8000 no docker-compose |
| CORS ao chamar API | Frontend chamava `localhost:8080` diretamente | Mudamos para path relativo `/api` (passa pelo NGINX) |
