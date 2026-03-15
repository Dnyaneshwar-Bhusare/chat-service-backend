# Chat-Service-Backend: Detailed Design Documentation

## 1. System Architecture Diagram

**Description:**
- The architecture consists of the following layers:
  - Client (Web/mobile app)
  - Backend (Spring Boot REST API & WebSocket)
  - Database (MySQL)
  - Blockchain (Ganache, Smart Contracts)

**Diagram:**
```
[Client] <--> [Backend: Controllers & WebSocket] <--> [Service Layer] <--> [DAO Layer] <--> [Database]
                                      |
                                      v
                              [Blockchain: Ganache]
```

## 2. Component Diagram

**Description:**
- Main components:
  - Controllers: Handle HTTP requests (ChatMessage, GetMessages, Login, etc.)
  - Services: Business logic (ChatMessageService, UserService)
  - DAO: Data access (UserDao, ChatMessageDao)
  - Models: Entities (User, ChatMessageEntity)
  - Blockchain: BlockchainService, ChatVerification
  - WebSocket: ChatWebSocketHandler

**Diagram:**
```
[Controllers] --> [Services] --> [DAO] --> [Database]
      |                |
      v                v
[WebSocket]      [Blockchain]
```

## 3. Sequence Diagram: Chat Message Flow

**Description:**
- User sends a chat message.
- Controller receives and passes to Service.
- Service stores message in DB, hashes it, and interacts with BlockchainService.
- BlockchainService stores hash on Ganache via ChatVerification smart contract.
- WebSocket broadcasts message to other users.

**Diagram:**
```
User -> Controller -> Service -> DAO -> Database
     -> BlockchainService -> Ganache (Smart Contract)
     -> WebSocket -> Other Users
```

## 4. Database ER Diagram

**Entities:**
- User: id, username, password, ...
- ChatMessage: id, sender_id, receiver_id, content, timestamp, hash

**Relationships:**
- User (1) <--> (M) ChatMessage

## 5. Deployment Diagram

**Description:**
- Components:
  - Application Server (Spring Boot)
  - Database Server (MySQL)
  - Ganache Node (Local Ethereum blockchain)

**Diagram:**
```
[Client]
   |
[Application Server]
   |         |
[Database] [Ganache Node]
```

## 6. Blockchain Integration Flow Diagram

**Description:**
- Chat message is hashed.
- Hash is sent to ChatVerification smart contract on Ganache.
- Hash is stored and can be verified later.

**Diagram:**
```
[Service] -> [BlockchainService] -> [Ganache: ChatVerification]
```

## 7. Ganache’s Role in Secure Chatting

Ganache provides a local Ethereum blockchain for development and testing. When a chat message is sent, its hash is stored on-chain using the ChatVerification smart contract. This ensures:
- **Integrity:** Any tampering with messages can be detected by comparing the hash stored on-chain.
- **Non-repudiation:** Users cannot deny sending a message, as its hash is immutably stored.
- **Auditability:** All message hashes are recorded and can be verified.

Ganache enables fast, secure, and auditable chat message verification, simulating real blockchain security in a development environment.

---

**Note:** For actual diagram visuals, use tools like draw.io or Lucidchart to create diagrams based on these descriptions.

