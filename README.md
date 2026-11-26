# Roteador Adaptativo de Pagamentos (MAPE-K Payment Gateway)

Gateway inteligente de processamento de pagamentos baseado na arquitetura de **Computação Autonômica (loop MAPE-K)**, projetado para detectar degradações de SLA (latência elevada ou falhas) em tempo real e reconfigurar dinamicamente a rota ativa entre adquirentes (`STONE` ↔ `CIELO`) sem intervenção manual ou necessidade de novo deploy.

> Projeto desenvolvido no contexto da disciplina **Tópicos Avançados em Sistemas Distribuídos I** — Centro de Informática (CIn) / UFPE.

---

## Sumário

1. [Visão Geral e Problema Resolvido](#1-visão-geral-e-problema-resolvido)
2. [Arquitetura do Sistema (MAPE-K)](#2-arquitetura-do-sistema-mape-k)
3. [Stack Tecnológica (Gradle & Ecossistema)](#3-stack-tecnológica-gradle--ecossistema)
4. [Estrutura do Repositório](#4-estrutura-do-repositório)
5. [Pré-requisitos](#5-pré-requisitos)
6. [Como Executar Passo a Passo](#6-como-executar-passo-a-passo)
7. [Acessando e Operando o Dashboard](#7-acessando-e-operando-o-dashboard)
8. [Roteiro de Demonstração (Failover Automático e State Drift)](#8-roteiro-de-demonstração-failover-automático-e-state-drift)
9. [Endpoints Principais](#9-endpoints-principais)
10. [Acesso Público e Demonstração Online](#10-acesso-público-e-demonstração-online)
11. [Boas Práticas e Troubleshooting](#11-boas-práticas-e-troubleshooting)

---

## 1. Visão Geral e Problema Resolvido

Em ambientes de pagamento de alta criticidade, APIs de adquirentes externas sofrem instabilidades repentinas, lentidão ou falhas intermitentes durante picos de tráfego. Estratégias de roteamento estático exigem intervenção humana ou redeploy para desviar o fluxo de transações, resultando em timeouts, quedas de conversão e impacto direto na experiência do cliente.

Este projeto implementa um **Sistema Autoadaptável** que:
- Processa pagamentos por meio do **Strategy Pattern**, desacoplando a orquestração do gateway da implementação específica de cada adquirente (`STONE` ou `CIELO`).
- Coleta telemetria (latência em milissegundos, indicador de sucesso/falha e código de erro) de forma assíncrona, sem adicionar latência bloqueante ao fluxo síncrono de cobrança.
- Avalia violações consecutivas de SLA (por padrão, latência `> 3000 ms` ou transação com falha).
- Dispara **failover automático** (`STONE` ↔ `CIELO`) ao atingir o limiar de `2` violações consecutivas na adquirente monitorada como ativa.
- Persiste a base de **Knowledge** no **Redis** (com fallback automático e seguro em memória caso o Redis esteja indisponível) e fornece observabilidade em tempo real via **Dashboard Web**, destacando inclusive cenários de *state drift*.

---

## 2. Arquitetura do Sistema (MAPE-K)

A solução separa o **Sistema Gerenciado (Managed Element)** do **Controlador Autonômico (Managing System)** por meio de comunicação assíncrona via **RabbitMQ** e reporte de estado/telemetria para o painel de monitoramento:

```text
+-------------------------------------------------------------------------+
|                        MANAGED PAYMENT SERVICE                          |
|                                                                         |
|  [POST /api/v1/payments]                                                |
|           |                                                             |
|           v                                                             |
|  +------------------------------+      +------------------------------+  |
|  | PaymentRoutingService        |----->| Acquirer Strategy            |  |
|  | (Rota Ativa do Efetor)       |      | (STONE / CIELO + Simulador)  |  |
|  +------------------------------+      +------------------------------+  |
|           ^                                          |                  |
|           | (Effector: troca rota ativa)             | (Sensor/Probe)   |
|  +------------------------------+      +------------------------------+  |
|  | CommandConsumer              |      | TelemetryPublisher           |  |
|  +------------------------------+      +------------------------------+  |
+-----------|------------------------------------------|------------------+
            | RabbitMQ (Commands)                      | RabbitMQ (Events)
            |                                          v
+-----------|------------------------------------------|------------------+
|           |               MANAGING MAPE-K CONTROLLER                    |
|           |                                                             |
|  +------------------------------+      +------------------------------+  |
|  | CommandPublisher (Execute)   |<-----| MapeKAnalyzer (Analyze/Plan) |  |
|  +------------------------------+      +------------------------------+  |
|                                                      ^                  |
|                                                      | (Knowledge)      |
|  +------------------------------+      +------------------------------+  |
|  | DashboardReporter (HTTP POST)|      | RedisKnowledgeStore (Redis)  |  |
|  +------------------------------+      +------------------------------+  |
+-------------------------------------------------------------------------+
```

### Papéis no Ciclo MAPE-K

- **Managed Element (`managed-payment-service`):** Expõe os endpoints de pagamento, simulação de latência e consulta/atualização do dashboard. Seleciona a estratégia da adquirente ativa (`STONE` ou `CIELO`) e aplica o atraso artificial quando o simulador está habilitado.
- **Sensor / Probe (`TelemetryPublisher`):** Registra os dados de cada transação (`acquirer`, `latencyMs`, `success`, `errorCode`) e publica os eventos de telemetria no RabbitMQ.
- **Monitor & Analyze (`MapeKAnalyzer`):** Consome os eventos de telemetria, confere se a métrica pertence à adquirente rastreada como ativa (`trackedActiveAcquirer`) e avalia se houve degradação (`!success` ou `latencyMs > 3000 ms`). Quando chega uma métrica de uma adquirente diferente da rastreada, registra `IGNORED_MISMATCH`.
- **Knowledge (`RedisKnowledgeStore`):** Mantém o estado persistido no Redis sob o prefixo `mapek:knowledge:` (`tracked-acquirer`, `degraded-count` e `last-reason`), inicializando na subida e recorrendo a estruturas em memória em caso de falha de rede/conexão com o Redis.
- **Plan & Execute (`CommandPublisher`):** Quando as violações consecutivas atingem o limite (`2`), define a próxima adquirente (`STONE` ↔ `CIELO`), zera o contador de violações, atualiza a Knowledge e publica o comando de adaptação no RabbitMQ.
- **Effector (`CommandConsumer`):** Consome o comando de troca de rota no `managed-payment-service`, atualiza a rota ativa no `PaymentRoutingService` e registra a adaptação no histórico sem interromper o serviço.

---

## 3. Stack Tecnológica (Gradle & Ecossistema)

Com base na configuração Gradle do projeto:

- **Linguagem & Runtime:** Java `25` (configurado via `JavaLanguageVersion.of(25)` no Gradle Java Toolchain)
- **Build & Dependency Management:** Gradle (`org.gradle` / Gradle Wrapper `./gradlew`) com `io.spring.dependency-management` (`1.1.7`)
- **Framework Principal:** Spring Boot `4.1.2`
  - `org.springframework.boot:spring-boot-starter-webmvc` (API REST e entrega do Dashboard estático)
  - `org.springframework.boot:spring-boot-starter-amqp` (integração com RabbitMQ para telemetria e comandos)
  - `org.springframework.boot:spring-boot-starter-data-redis` (persistência da base de Knowledge no Redis, quando habilitada no controlador)
- **Produtividade & Testes:** Lombok (`compileOnly` / `annotationProcessor`) e JUnit Platform (`useJUnitPlatform()`, `spring-boot-starter-webmvc-test`, `junit-platform-launcher`)
- **Mensageria & Estado:** RabbitMQ e Redis
- **Frontend / Observabilidade:** HTML5, CSS3, JavaScript e Chart.js
- **Infraestrutura:** Docker & Docker Compose

---

## 4. Estrutura do Repositório

```text
payment-router-tasd1/
├── managed-payment-service/       # Sistema Gerenciado (API REST, Strategy STONE/CIELO, Simulador, Efetor e UI)
│   ├── build.gradle               # Dependências e configuração Java 25 / Spring Boot 4.1.2
│   ├── gradlew / gradlew.bat      # Gradle Wrapper
│   ├── .gitignore
│   └── src/main/resources/static/
│       └── index.html             # Dashboard web de observabilidade, simulação e Knowledge
├── managing-mapek-controller/     # Controlador Autonômico (Analyze, Plan, Execute e RedisKnowledgeStore)
│   ├── build.gradle
│   ├── gradlew / gradlew.bat
│   └── .gitignore
├── docker-compose.yml             # Orquestração dos serviços de infraestrutura e/ou microsserviços
├── .gitignore
└── README.md
```

---

## 5. Pré-requisitos

- **Docker** e **Docker Compose** (recomendado para subir RabbitMQ e Redis rapidamente)
- **JDK 25** (ou compatível com o toolchain configurado no `build.gradle`) e **Gradle Wrapper** (`./gradlew` / `gradlew.bat`)
- **Git**

---

## 6. Como Executar Passo a Passo

### Passo 1: Clonar o repositório

```bash
git clone https://github.com/SEU-USUARIO/payment-router-tasd1.git
cd payment-router-tasd1
```

### Passo 2: Subir a infraestrutura (RabbitMQ e Redis) com Docker

Na raiz do repositório, inicie os contêineres:

```bash
docker compose up -d
```

Confira o status dos contêineres:

```bash
docker compose ps
```

*(Caso o seu `docker-compose.yml` também inclua os serviços Java, utilize `docker compose up -d --build`.)*

### Passo 3: Iniciar o Sistema Gerenciado (`managed-payment-service`)

Em um terminal, acesse o diretório do serviço gerenciado e execute via Gradle (porta padrão `8080`):

```bash
cd managed-payment-service
./gradlew bootRun
```

*(No Windows, execute `gradlew.bat bootRun`.)*

### Passo 4: Iniciar o Controlador MAPE-K (`managing-mapek-controller`)

Em um segundo terminal, acesse o diretório do controlador e execute via Gradle:

```bash
cd managing-mapek-controller
./gradlew bootRun
```

*(No Windows, execute `gradlew.bat bootRun`.)*

Acompanhe nos logs a conexão com o RabbitMQ e o carregamento da Knowledge (quando integrado ao Redis):

```text
🧠 [KNOWLEDGE/REDIS] Carregado do Redis | trackedAcquirer=STONE | degradedCount=0
```

### Passo 5: Executar testes automatizados com Gradle (opcional)

Em qualquer um dos módulos Java:

```bash
./gradlew test
```

### Passo 6: Inspecionar RabbitMQ e Redis (opcional)

- **RabbitMQ Management:** `http://localhost:15672` (credenciais padrão: `guest` / `guest`)
- **Consultar chaves da Knowledge no Redis:**

```bash
docker exec -it $(docker ps -qf "ancestor=redis") redis-cli MGET mapek:knowledge:tracked-acquirer mapek:knowledge:degraded-count mapek:knowledge:last-reason
```

- **Limpar/resetar chaves da Knowledge no Redis:**

```bash
docker exec -it $(docker ps -qf "ancestor=redis") redis-cli DEL mapek:knowledge:tracked-acquirer mapek:knowledge:degraded-count mapek:knowledge:last-reason
```

---

## 7. Acessando e Operando o Dashboard

Com o `managed-payment-service` rodando, abra no navegador:

```text
http://localhost:8080/index.html
```

O dashboard atualiza automaticamente a cada 1 segundo e apresenta:

1. **Badge de Rota Ativa (`STONE` / `CIELO`):** Exibe a rota atualmente configurada no efetor do serviço gerenciado.
2. **Cards de Métricas:** Total de transações (com divisão entre `STONE` e `CIELO`), violações consecutivas (`X / 2`) e status/delay dos simuladores de `STONE` e `CIELO`.
3. **Banner do Último Evento MAPE-K:** Mostra o tipo de evento (`HEALTHY`, `VIOLATION`, `FAILOVER`, `IGNORED_MISMATCH` ou `IDLE`), a mensagem do analisador, a adquirente da métrica recebida e o horário.
4. **Painel Base de Conhecimento (Knowledge):** Compara **Rota Rastreada (Knowledge)** com **Rota Real (Efetor)**, exibe o backend (`REDIS`), a próxima rota de failover e o limiar de SLA (`3000 ms`), destacando em amarelo o alerta **`DRIFT DE ROTA`** quando houver divergência entre os estados.
5. **Controles de Simulação e Pagamentos:** Permite configurar o delay em `ms`, ligar/desligar o atraso artificial em `STONE` ou `CIELO` e disparar `1`, `3` ou `5` pagamentos de teste.
6. **Gráfico de Latência vs. SLA (`3000 ms`):** Plota as transações ao longo do tempo com linha tracejada indicando o limite de SLA.
7. **Histórico de Adaptações, Eventos MAPE-K e Últimas Transações:** Facilita acompanhar a cadeia completa de causa e efeito durante apresentações e testes.

---

## 8. Roteiro de Demonstração (Failover Automático e State Drift)

### Cenário 1: Failover Automático por Latência Acima do SLA

1. Abra `http://localhost:8080/index.html` e verifique que a **Rota Ativa** e a **Rota Rastreada** estão em `STONE` (`0 / 2` violações).
2. No card **Injetar Latência (Simulador)**, mantenha `5000` ms e clique em **Ligar STONE**.
3. Clique em **Enviar 1 Pagamento**: a chamada levará cerca de 5 segundos, o ponto no gráfico ficará acima da linha de `3000 ms` e o contador subirá para `1 / 2` (`VIOLATION`).
4. Clique novamente em **Enviar 1 Pagamento**: ao atingir `2 / 2`, o `MapeKAnalyzer` planeja o failover, atualiza a Knowledge para `CIELO`, zera o contador e envia o comando via RabbitMQ.
5. O `CommandConsumer` aplica a troca no efetor, a **Rota Ativa** passa a `CIELO` e os próximos pagamentos passam a responder rapidamente.

### Cenário 2: Observação de State Drift entre Knowledge (Redis) e Efetor

1. Com o Redis armazenando `CIELO` como `tracked-acquirer`, reinicie apenas o `managed-payment-service` (que sobe com rota inicial `STONE`).
2. Envie 1 pagamento: o painel **Base de Conhecimento (Knowledge)** indicará **`DRIFT DE ROTA`** (`Rota Rastreada = CIELO` vs. `Rota Real = STONE`) e o analisador registrará `IGNORED_MISMATCH` até que os estados voltem a coincidir.

---

## 9. Endpoints Principais

| Método | Endpoint | Serviço | Função |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/payments` | `managed-payment-service` | Executa um pagamento utilizando a adquirente ativa |
| `POST` | `/api/simulator/latency` | `managed-payment-service` | Liga/desliga simulação de atraso (`acquirer`, `enable`, `delay`) |
| `GET` | `/api/dashboard/status` | `managed-payment-service` | Retorna o estado consolidado para o `index.html` |
| `POST` | `/api/dashboard/mapek-status` | `managed-payment-service` | Recebe eventos e estado da Knowledge enviados pelo controlador |

---

## 10. Acesso Público e Demonstração Online

Se você quiser compartilhar o dashboard ao vivo para demonstrações externas sem que outras pessoas precisem configurar o ambiente local:

- **Exposição temporária via túnel (Cloudflare Tunnel ou ngrok):**
  Com a aplicação rodando na porta `8080`:

```bash
cloudflared tunnel --url http://localhost:8080
```

ou

```bash
ngrok http 8080
```

  Compartilhe a URL HTTPS gerada (acessando `/index.html`). Lembre-se de encerrar o túnel quando concluir a demonstração.

- **Hospedagem em nuvem:** Os serviços podem ser publicados em plataformas como **Railway**, **Render** ou **Fly.io** utilizando variáveis de ambiente para apontar para instâncias de RabbitMQ e Redis.

---

## 11. Boas Práticas e Troubleshooting

- **Dashboard não reflete alterações recentes no `index.html`:** Verifique se o arquivo foi salvo em `managed-payment-service/src/main/resources/static/index.html` e force atualização no navegador (`Ctrl + Shift + R` ou `Cmd + Shift + R`).
- **Alerta de `DRIFT DE ROTA` inesperado:** Confirme se o Redis não herdou `tracked-acquirer=CIELO` de um teste anterior e, se quiser começar do zero, limpe as chaves `mapek:knowledge:*` no Redis antes de iniciar os serviços.
- **Erro de conexão com RabbitMQ ou Redis na subida:** Garanta que o `docker compose up -d` foi executado e que os hosts/portas no `application.yml` / `application.properties` correspondem aos serviços em execução.
