<div align="center">

# Tappsy Backend

**Real-time messaging backend powered by Java EE Servlets, JSR-356 WebSockets, Hibernate ORM, and MySQL 8.**  
Delivers low-latency bidirectional chat, online presence tracking, read receipts, contact discovery, and multipart profile media management.

[![Java](https://img.shields.io/badge/Java-8%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Java EE](https://img.shields.io/badge/Java%20EE-Servlet%20%26%20WebSocket-007396?style=for-the-badge&logo=java&logoColor=white)](https://jakarta.ee/)
[![Hibernate](https://img.shields.io/badge/Hibernate-4.3%20ORM-59666C?style=for-the-badge&logo=hibernate&logoColor=white)](https://hibernate.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Gson](https://img.shields.io/badge/Gson-2.12.1-4285F4?style=for-the-badge&logo=google&logoColor=white)](https://github.com/google/gson)
[![License: MIT](https://img.shields.io/badge/License-MIT-22C55E?style=for-the-badge)](LICENSE)

</div>

---

## System Architecture

Tappsy separates stateless onboarding/media HTTP endpoints from persistent, full-duplex WebSocket sessions managed in a thread-safe `ConcurrentHashMap` registry.

```mermaid
flowchart TD
    client["Client Application<br/>React Native or Web"]

    subgraph server["GlassFish Java EE Application Server"]
        subgraph rest["HTTP Servlet Layer"]
            userCtrl["UserController<br/>POST /UserController"]
            loginCtrl["LoginController<br/>POST /LoginController"]
            profileCtrl["ProfileController<br/>POST /ProfileController"]
            healthCtrl["Test Servlet<br/>GET /Test"]
        end

        subgraph ws["JSR-356 WebSocket Layer"]
            endpoint["ChatEndPoint<br/>WS /chat"]
            chatSvc["ChatService<br/>Session Registry and Delivery"]
            userSvc["UserService<br/>Presence and Contact Sync"]
            profileSvc["ProfileService<br/>Media Storage and URL Resolver"]
        end

        config["AppConfig and HibernateUtil<br/>Env Override Loader"]
    end

    db[("MySQL 8 Database<br/>tappsy")]
    fs[("Static Web Assets<br/>web/profile-images")]

    client -->|Registration and Profile Upload| userCtrl
    client -->|PIN Authentication| loginCtrl
    client -->|Avatar Update| profileCtrl
    client -->|Health Check| healthCtrl
    client -->|Bidirectional JSON Frames| endpoint

    endpoint -->|Route Chat Events| chatSvc
    endpoint -->|Route Presence and Contacts| userSvc
    userCtrl -->|Save Avatar| profileSvc
    profileCtrl -->|Update Avatar| profileSvc
    chatSvc -->|Resolve Avatar URL| profileSvc
    userSvc -->|Resolve Avatar URL| profileSvc

    userCtrl -->|Hibernate Session| config
    loginCtrl -->|Hibernate Session| config
    chatSvc -->|Hibernate Session| config
    userSvc -->|Hibernate Session| config
    config -->|JDBC Connection Pool| db
    profileSvc -->|Read and Write PNG| fs
```

---

## Real-Time Messaging Lifecycle

When a user connects to `/chat?userId={id}`, their WebSocket session is registered, their presence flips to `ONLINE`, pending `SENT` messages from active contacts transition to `DELIVERED`, and both sender and recipient receive synchronized live updates on every new message.

```mermaid
sequenceDiagram
    autonumber
    participant Sender as Sender Client
    participant WS as ChatEndPoint
    participant CS as ChatService
    participant US as UserService
    participant DB as MySQL via Hibernate
    participant Receiver as Receiver Client

    Sender->>WS: Connect WS /chat?userId=1
    WS->>CS: Register session for user 1
    WS->>US: Mark user 1 ONLINE
    US->>DB: Update user status and pending SENT chats to DELIVERED

    Sender->>WS: Frame type send_message toUserId 2
    WS->>CS: Persist chat and ensure bidirectional FriendList
    CS->>DB: Insert Chat row and commit transaction
    CS-->>Sender: Push type new_message and updated friend_list
    CS-->>Receiver: Push type new_message and updated friend_list

    Receiver->>WS: Frame type get_single_chat friendId 1
    WS->>CS: Load history and mark DELIVERED chats as READ
    CS->>DB: Update incoming messages to READ
    CS-->>Receiver: Push type single_chat and updated friend_list
```

---

## Database Entity-Relationship Model

```mermaid
erDiagram
    USER {
        int id PK "Auto-increment primary key"
        varchar first_name "User first name"
        varchar last_name "User last name"
        varchar country_code "International dialing prefix"
        varchar contact_no "Unique phone number"
        varchar pincode "Unique 4-digit authentication PIN"
        varchar status "ONLINE or OFFLINE presence"
        datetime created_at "Record creation timestamp"
        datetime updated_at "Last modification timestamp"
    }

    FRIEND_LIST {
        int id PK "Auto-increment primary key"
        int user_id FK "Owner user reference"
        int friend_id FK "Contact user reference"
        varchar user_status "ACTIVE or BLOCKED relationship"
        varchar display_name "Custom contact alias"
    }

    CHAT {
        int id PK "Auto-increment primary key"
        int from_user FK "Sender user reference"
        int to_user FK "Recipient user reference"
        longtext message "UTF-8 message payload"
        longtext files "Attachment URI or FILE marker"
        varchar status "SENT, DELIVERED, READ, or DELETE"
        datetime created_at "Message creation timestamp"
        datetime updated_at "Last status update timestamp"
    }

    USER ||--o{ FRIEND_LIST : "owns contacts"
    USER ||--o{ FRIEND_LIST : "added as contact"
    USER ||--o{ CHAT : "sends messages"
    USER ||--o{ CHAT : "receives messages"
```

---

## API & WebSocket Reference

### HTTP REST Endpoints

| Method | Endpoint | Content-Type | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/UserController` | `multipart/form-data` | Register a new user (`firstName`, `lastName`, `countryCode`, `contactNo`, `pincode`, `profileImage`). |
| `POST` | `/LoginController` | `multipart/form-data` or `application/x-www-form-urlencoded` | Authenticate an existing user by `contactNo` and 4-digit `pincode`. |
| `POST` | `/ProfileController` | `multipart/form-data` | Update a user's profile avatar (`userId`, `profileImage`). |
| `GET` | `/Test` | `application/json` | Lightweight service health-check endpoint returning `{"status": true, "service": "TappsyBackEnd"}`. |

### WebSocket Protocol (`ws://<host>/TappsyBackEnd/chat?userId=<id>`)

| Client Message `type` | Required Payload Fields | Server Response `type` |
| :--- | :--- | :--- |
| `PING` | — | `PONG` |
| `get_chat_list` | — | `friend_list` (list of `ChatSummary` with unread counts) |
| `get_single_chat` | `friendId` | `single_chat` + refreshed `friend_list` (marks incoming as `READ`) |
| `send_message` | `toUserId`, `message` | `new_message` + refreshed `friend_list` pushed to both participants |
| `send_chat` | `fromId`, `toId`, `message` | `chat` + refreshed `friend_list` pushed to both participants |
| `get_friend_data` | `friendId` | `friend_data` |
| `get_all_users` | — | `all_users` (active contacts mapped to `UserDTO`) |
| `save_new_contact` | `user` (`firstName`, `lastName`, `countryCode`, `contactNo`, `pincode`) | `new_contact_response_text` + refreshed `all_users` |
| `set_user_profile` | — | `user_profile` |

---

## Core Engineering Highlights

- **Concurrent Session Management:** Thread-safe `ConcurrentHashMap<Integer, Session>` maps active user IDs to live WebSocket sessions for instant O(1) message fan-out.
- **Hibernate ORM & Transactional Integrity:** JPA-annotated entities (`BaseEntity`, `User`, `FriendList`, `Chat`, `Status`) backed by `StandardServiceRegistryBuilder` and deterministic `try/finally` session lifecycle management.
- **Zero-Hardcoded-Secrets Configuration:** `AppConfig` and `HibernateUtil` automatically prioritize runtime environment variables (`DB_URL`, `DB_USER`, `DB_PASSWORD`, `PUBLIC_BASE_URL`, `LOCAL_BASE_URL`, `APP_CONTEXT_PATH`) over local XML defaults.
- **Automatic Delivery & Read Receipts:** Pending messages transition cleanly through `SENT` → `DELIVERED` (on recipient socket open) → `READ` (on thread retrieval), with live unread counters aggregated in `ChatSummary`.
- **Clean DTO Projection:** `UserDTO` and `ChatSummary` decouple internal Hibernate entities from serialized JSON payloads via Gson.

---

## Repository Structure

```text
TappsyBackEnd/
├── .env.example                       # Runtime environment variable template
├── LICENSE                            # MIT Open-Source License
├── README.md                          # Architecture & API documentation
├── build.xml                          # Apache Ant / NetBeans build script
├── database/
│   └── tappsy.sql                     # Clean MySQL 8 DDL schema
├── lib/                               # Hibernate 4.3, MySQL Connector/J, Gson, SLF4J JARs
├── nbproject/                         # NetBeans Java EE web project descriptors
├── src/
│   ├── conf/
│   │   └── MANIFEST.MF
│   └── java/
│       ├── hibernate.cfg.xml          # Local Hibernate mapping & connection defaults
│       ├── hibernate.cfg.xml.example  # Reference Hibernate configuration template
│       ├── controller/
│       │   ├── LoginController.java   # PIN-based authentication servlet
│       │   ├── ProfileController.java # Multipart profile avatar upload servlet
│       │   ├── Test.java              # JSON health-check servlet
│       │   └── UserController.java    # User registration servlet
│       ├── dto/
│       │   └── UserDTO.java           # Outbound user profile projection
│       ├── entity/
│       │   ├── BaseEntity.java        # Mapped superclass with audit timestamps
│       │   ├── Chat.java              # Message entity
│       │   ├── FriendList.java        # Bidirectional contact graph entity
│       │   ├── Status.java            # Presence and delivery state enum
│       │   └── User.java              # Account entity
│       ├── socket/
│       │   ├── ChatEndPoint.java      # JSR-356 @ServerEndpoint("/chat")
│       │   ├── ChatService.java       # Session registry, history & delivery service
│       │   ├── ChatSummary.java       # Inbox thread summary DTO
│       │   ├── ProfileService.java    # Disk persistence & avatar URL verification
│       │   └── UserService.java       # Online status, receipts & contact management
│       └── util/
│           ├── AppConfig.java         # Env / JVM property configuration helper
│           └── HibernateUtil.java     # SessionFactory singleton with env overrides
└── web/
    ├── WEB-INF/
    │   └── glassfish-web.xml          # GlassFish deployment descriptor
    ├── index.html                     # Backend status landing page
    └── profile-images/
        └── .gitkeep                   # Runtime avatar storage directory (ignored in git)
```

---

## Getting Started

### Prerequisites

- **JDK 8** (or compatible Java EE 8 runtime)
- **GlassFish 5 / Payara Server 5** (Servlet 3.1+ & JSR-356 WebSocket container)
- **MySQL 8.0+**
- **Apache NetBeans** (or Apache Ant)

### 1. Clone the Repository

```bash
git clone https://github.com/theekshana-sudeepa/TappsyBackEnd.git
cd TappsyBackEnd
```

### 2. Initialize the MySQL Database

Import the clean schema from `database/tappsy.sql`:

```bash
mysql -u root -p < database/tappsy.sql
```

### 3. Configure Environment Variables

Copy `.env.example` as a reference and configure the environment variables (or JVM `-D` system properties) in your GlassFish/Payara domain:

```bash
cp .env.example .env
```

| Variable | Description | Default |
| :--- | :--- | :--- |
| `DB_URL` | JDBC connection URL for MySQL | `jdbc:mysql://localhost:3306/tappsy?useSSL=false&allowPublicKeyRetrieval=true` |
| `DB_USER` | MySQL database username | `root` |
| `DB_PASSWORD` | MySQL database password | *(empty)* |
| `PUBLIC_BASE_URL` | Public base URL returned to clients for profile images (e.g., reverse proxy or tunnel URL) | `http://localhost:8080` |
| `LOCAL_BASE_URL` | Internal base URL used for avatar HEAD verification | `http://localhost:8080` |
| `APP_CONTEXT_PATH` | Deployed web context path | `/TappsyBackEnd` |

### 4. Build & Deploy

1. Open the project in **Apache NetBeans** and link your local **GlassFish Server** runtime.
2. Clean and build the project (`TappsyBackEnd.war`) and deploy to GlassFish.
3. Verify the server is live:

```bash
curl http://localhost:8080/TappsyBackEnd/Test
# {"status":true,"service":"TappsyBackEnd"}
```

---

## License

Distributed under the **MIT License**. See [LICENSE](LICENSE) for full details.

---

<div align="center">

**Author**  
**Theekshana Sudeepa** — [LinkedIn](https://www.linkedin.com/in/theekshana-sudeepa/) · [GitHub](https://github.com/theekshana-sudeepa) · [LexoraTech](https://lexoratech.com)

</div>
