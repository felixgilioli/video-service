# Video Service

Microsserviço responsável pelo gerenciamento e processamento de vídeos. Permite criar, listar, atualizar, deletar e fazer upload de vídeos, integrando com armazenamento em nuvem (S3/MinIO) e processamento assíncrono via filas (SQS/LocalStack).

## Tecnologias

- **Kotlin** + **Spring Boot 4.1.0-M1** (Java 21)
- **PostgreSQL** + **Flyway** para persistência e migrations
- **AWS S3** (MinIO localmente) para armazenamento de arquivos
- **AWS SQS** (LocalStack localmente) para mensageria assíncrona
- **OpenTelemetry** para tracing distribuído
- **Prometheus** + **Actuator** para métricas e observabilidade
- **JaCoCo** + **SonarQube** para cobertura de testes
- **Docker** + **GitHub Actions** para CI/CD

## Arquitetura

```
Cliente
  │
  ▼
VideoController (REST API)
  │
  ▼
VideoService
  ├── StorageService → S3 / MinIO (upload de arquivos)
  └── SqsService → SQS / LocalStack (envio de mensagens)

VideoStatusConsumer (polling a cada 5s)
  └── video-status-queue → atualiza status, zipUrl, firstFrameUrl
```

### Fluxo Principal

1. Cliente cria um vídeo via `POST /videos` → salvo no banco com status `PENDING`
2. Cliente faz upload do arquivo via `POST /videos/{id}/upload` → arquivo enviado ao S3, status muda para `PROCESSING`, mensagem enviada para a fila `video-processing-queue`
3. Serviço externo processa o vídeo e envia evento para `video-status-queue`
4. `VideoStatusConsumer` faz polling na fila e atualiza o status, `zipUrl` e `firstFrameUrl` do vídeo

### Status do Vídeo

| Status | Descrição |
|--------|-----------|
| `PENDING` | Vídeo criado, aguardando upload |
| `PROCESSING` | Upload realizado, processamento em andamento |
| _(outros)_ | Definidos pelo serviço de processamento externo |

## Endpoints da API

Todos os endpoints que operam sobre um usuário requerem o header `X-User-Email`.

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/videos` | Cria um novo vídeo |
| `GET` | `/videos` | Lista vídeos do usuário |
| `GET` | `/videos/{id}` | Busca vídeo por ID |
| `PUT` | `/videos/{id}` | Atualiza dados do vídeo |
| `DELETE` | `/videos/{id}` | Remove o vídeo |
| `POST` | `/videos/{id}/upload` | Faz upload do arquivo de vídeo (multipart) |

### Exemplo de criação de vídeo

```http
POST /videos
X-User-Email: usuario@email.com
Content-Type: application/json

{
  "title": "Meu Vídeo",
  "description": "Descrição do vídeo"
}
```

### Exemplo de upload

```http
POST /videos/{id}/upload
X-User-Email: usuario@email.com
Content-Type: multipart/form-data

file: <arquivo>
```

## Configuração

### Pré-requisitos

- Java 21
- Docker e Docker Compose
- PostgreSQL (porta 5433)
- MinIO (porta 9000)
- LocalStack / AWS SQS (porta 4566)

### Variáveis de configuração (application.yaml)

| Propriedade | Padrão | Descrição |
|-------------|--------|-----------|
| `server.port` | `8081` | Porta da aplicação |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5433/video_db` | URL do banco |
| `storage.endpoint` | `http://localhost:9000` | Endpoint do S3/MinIO |
| `storage.bucket` | `videos` | Bucket de armazenamento |
| `sqs.endpoint` | `http://localhost:4566` | Endpoint do SQS/LocalStack |
| `sqs.video-processing-queue` | `video-processing-queue` | Fila de envio para processamento |
| `sqs.video-status-queue` | `video-status-queue` | Fila de retorno de status |

## Executando localmente

### Com Gradle

```bash
./gradlew bootRun
```

### Com Docker

```bash
# Build da imagem
docker build -t felixgilioli/video-service .

# Executar
docker run -p 8081:8081 felixgilioli/video-service
```

## Testes

```bash
./gradlew test
```

O relatório de cobertura (JaCoCo) é gerado em `build/reports/jacoco/test/html/index.html`.

## Observabilidade

| Endpoint | Descrição |
|----------|-----------|
| `GET /actuator/health` | Status de saúde da aplicação |
| `GET /actuator/metrics` | Métricas da aplicação |
| `GET /actuator/prometheus` | Métricas no formato Prometheus |

Traces são exportados via OpenTelemetry para `http://localhost:4318/v1/traces`.

## CI/CD

O pipeline no GitHub Actions (`.github/workflows/build.yml`) executa automaticamente a cada push na branch `main`:

1. Build e análise com SonarQube
2. Build da imagem Docker
3. Push da imagem para o Docker Hub (`felixgilioli/video-service`)

## Estrutura do Projeto

```
src/main/kotlin/br/com/felixgilioli/videoservice/
├── config/         # Configurações (S3, SQS)
├── consumer/       # VideoStatusConsumer (polling SQS)
├── controller/     # VideoController (REST)
├── dto/            # DTOs de request, response e mensagens
├── entity/         # Entidade JPA Video
├── enumeration/    # VideoStatus
├── repository/     # VideoRepository
└── service/        # VideoService, StorageService, SqsService
```
