# WebSocket Integration Prompt — Cypher Squad Chat Frontend

---

## CONTEXT

You are implementing the WebSocket integration for a **secure blockchain-powered chat application**.
The backend is a Spring Boot application running on `http://localhost:9001`.

The backend exposes **two separate WebSocket endpoints**:

| Endpoint | Purpose |
|---|---|
| `ws://localhost:9001/chat` | Real-time delivery — notifies receiver when a new message is sent |
| `ws://localhost:9001/ws/messages` | Message history fetch + real-time push — the main chat socket |

---

## ⚠️ IMPORTANT — BOTH SOCKETS USE THE SAME DATA

Both sockets send and receive **exactly the same JSON structure and data model**.
The only difference is **what each socket is used for**:

| | `/chat` | `/ws/messages` |
|---|---|---|
| **Register frame** | `{ type: "login", userId }` | `{ type: "register", userId }` |
| **Can fetch message history** | ❌ No | ✅ Yes — `get_messages` |
| **Receives `new_message` push** | ✅ Yes | ✅ Yes — same push, same data |
| **`new_message` data shape** | `ChatMessageView` | `ChatMessageView` — **identical** |
| **Message fields** | Same | Same |
| **Encryption/algo fields** | Same | Same |
| **txHash/publicKey fields** | Same | Same |

> ✅ Same `ChatMessageView` object, same field names, same values — on both sockets.
> The backend builds one `ChatMessageView` and pushes it to **both** sockets simultaneously.

---

## TASK

Implement **both WebSocket connections** in the frontend using the full specification below.
- Do NOT use HTTP polling for messages.
- Both sockets must be opened after the user logs in.
- All communication is plain JSON over WebSocket text frames.
- Both sockets must auto-reconnect if the connection drops.

---

## SOCKET 1 — `/chat`

### Endpoint
```
ws://localhost:9001/chat
```

### Purpose
Receives a real-time notification when **someone sends YOU a message** via `POST /sendMessage`.
This socket only **receives** pushes from the server — you never send anything on it except the login frame.

### Step 1 — Open the connection
```javascript
const chatSocket = new WebSocket("ws://localhost:9001/chat");
```

### Step 2 — Register your userId after connection opens
Send this immediately in `onopen`:
```json
{
  "type": "login",
  "userId": "42"
}
```

### Step 3 — Server confirms registration
```json
{
  "type": "login_success",
  "message": "Connected successfully"
}
```

### Step 4 — Receive new message push (automatic, no request needed)
When another user sends you a message, the server pushes:
```json
{
  "type": "new_message",
  "data": {
    "chatId": "uuid-string",
    "message": "encrypted-ciphertext",
    "fromUser": "7",
    "toUser": "42",
    "algo": "AES",
    "createdTs": "2026-03-26T10:00:00",
    "txHash": "0xabc123...",
    "publicKey": "sender-public-key"
  }
}
```

### Full `/chat` socket implementation
```javascript
let chatSocket;

function connectChatSocket(userId) {
  chatSocket = new WebSocket("ws://localhost:9001/chat");

  chatSocket.onopen = () => {
    console.log("[/chat] Connected");
    chatSocket.send(JSON.stringify({ type: "login", userId: userId }));
  };

  chatSocket.onmessage = (event) => {
    const frame = JSON.parse(event.data);

    if (frame.type === "login_success") {
      console.log("[/chat] Registered:", frame.message);
    }

    if (frame.type === "new_message") {
      console.log("[/chat] New message received:", frame.data);
      // frame.data is a ChatMessageView object — decrypt & render it
      handleIncomingMessage(frame.data);
    }
  };

  chatSocket.onerror = (err) => console.error("[/chat] Error:", err);

  chatSocket.onclose = () => {
    console.warn("[/chat] Disconnected — reconnecting in 3s...");
    setTimeout(() => connectChatSocket(userId), 3000);
  };
}
```

---

## SOCKET 2 — `/ws/messages`

### Endpoint
```
ws://localhost:9001/ws/messages
```

### Purpose
This is the **main chat socket**. Use it to:
1. Register your userId so the server knows which session belongs to you.
2. Fetch message history (full conversation or all messages).
3. Receive real-time push when a new message arrives (same `new_message` push as `/chat`).

> ⚠️ You must send `register` first before sending `get_messages`.

---

### MESSAGE TYPE 1 — `register`

**You send:**
```json
{
  "type": "register",
  "userId": "42"
}
```

**Server responds:**
```json
{
  "type": "register_success",
  "message": "Registered successfully"
}
```

---

### MESSAGE TYPE 2 — `get_messages`

#### Case A — Fetch conversation between two specific users
```json
{
  "type": "get_messages",
  "userId": "42",
  "sender": "42",
  "receiver": "7"
}
```
- `sender` = logged-in user's ID
- `receiver` = the friend's ID you are chatting with

#### Case B — Fetch ALL messages for a user (no filter)
```json
{
  "type": "get_messages",
  "userId": "42"
}
```
- Omit `sender` and `receiver` to get all messages involving userId.

**Server responds (both cases):**
```json
{
  "type": "messages_response",
  "status": "ok",
  "data": [
    {
      "chatId": "uuid-string",
      "message": "encrypted-ciphertext",
      "fromUser": "42",
      "toUser": "7",
      "algo": "AES",
      "createdTs": "2026-03-26T10:00:00",
      "txHash": "0xabc123...",
      "publicKey": "sender-public-key"
    }
  ]
}
```
- Messages are ordered by `createdTs DESC` (newest first).
- `message` is the encrypted ciphertext — decrypt it on the frontend using `algo` and the sender's `publicKey`.

**Server responds on error:**
```json
{
  "type": "messages_response",
  "status": "error",
  "message": "userId is required to fetch messages"
}
```

---

### MESSAGE TYPE 3 — `new_message` (SERVER → CLIENT push, no request needed)

When a new message is sent to the registered user, server automatically pushes:
```json
{
  "type": "new_message",
  "data": {
    "chatId": "uuid-string",
    "message": "encrypted-ciphertext",
    "fromUser": "7",
    "toUser": "42",
    "algo": "AES",
    "createdTs": "2026-03-26T10:15:00",
    "txHash": "0xdef456...",
    "publicKey": "sender-public-key"
  }
}
```

---

### Full `/ws/messages` socket implementation
```javascript
let messageSocket;

function connectMessageSocket(userId) {
  messageSocket = new WebSocket("ws://localhost:9001/ws/messages");

  messageSocket.onopen = () => {
    console.log("[/ws/messages] Connected");

    // Step 1: Register userId first
    messageSocket.send(JSON.stringify({ type: "register", userId: userId }));
  };

  messageSocket.onmessage = (event) => {
    const frame = JSON.parse(event.data);

    switch (frame.type) {

      case "register_success":
        console.log("[/ws/messages] Registered:", frame.message);
        // Step 2: Now fetch messages after registration is confirmed
        fetchMessages(userId, null, null);
        break;

      case "messages_response":
        if (frame.status === "ok") {
          console.log("[/ws/messages] Messages loaded:", frame.data.length);
          renderMessageHistory(frame.data);   // render all messages in UI
        } else {
          console.error("[/ws/messages] Error:", frame.message);
        }
        break;

      case "new_message":
        console.log("[/ws/messages] New message pushed:", frame.data);
        appendNewMessage(frame.data);          // append single new message to UI
        break;
    }
  };

  messageSocket.onerror = (err) => console.error("[/ws/messages] Error:", err);

  messageSocket.onclose = () => {
    console.warn("[/ws/messages] Disconnected — reconnecting in 3s...");
    setTimeout(() => connectMessageSocket(userId), 3000);
  };
}

// Call this whenever user opens a conversation
function fetchMessages(userId, sender, receiver) {
  if (!messageSocket || messageSocket.readyState !== WebSocket.OPEN) {
    console.warn("Socket not ready");
    return;
  }

  const request = { type: "get_messages", userId: userId };
  if (sender)   request.sender   = sender;
  if (receiver) request.receiver = receiver;

  messageSocket.send(JSON.stringify(request));
}
```

---

## COMPLETE STARTUP FLOW

Call these two functions right after the user logs in successfully:

```javascript
// After login API returns successfully:
const userId = loginResponse.data.userId;   // from POST /login response

connectChatSocket(userId);       // opens ws://localhost:9001/chat
connectMessageSocket(userId);    // opens ws://localhost:9001/ws/messages
```

---

## ChatMessageView — Full Data Model

Every message object returned from the server has this exact shape:

| Field | Type | Description |
|---|---|---|
| `chatId` | String (UUID) | Unique message identifier |
| `message` | String | Encrypted ciphertext (decrypt using algo + publicKey) |
| `fromUser` | String | Sender's UserID |
| `toUser` | String | Receiver's UserID |
| `algo` | String | Encryption algorithm used: `"AES"`, `"BASE64"`, or `"XOR"` |
| `createdTs` | String (ISO datetime) | Timestamp: `"2026-03-26T10:00:00"` |
| `txHash` | String | Blockchain transaction hash (`0x...`) — null if blockchain was down |
| `publicKey` | String | Sender's public key — use this to decrypt the message |

---

## SEND A MESSAGE — REST API (not WebSocket)

Sending a message is done via **HTTP POST**, not WebSocket:

```
POST http://localhost:9001/sendMessage
Content-Type: application/json
```

Request body:
```json
{
  "message":       "encrypted-ciphertext-for-receiver",
  "messageToSelf": "encrypted-ciphertext-for-sender",
  "from":          "42",
  "to":            "7",
  "algo":          "AES",
  "timestamp":     "2026-03-26T10:00:00"
}
```

- `message` — ciphertext encrypted with the **receiver's** public key
- `messageToSelf` — ciphertext encrypted with the **sender's** own public key (for their own read)
- After a successful POST, the server automatically pushes the new message to the receiver over both WebSocket connections.

Response on success:
```json
{ "status": "1", "message": "ok", "data": "Message sent successfully!" }
```

Response on blockchain failure:
```json
{ "status": "0", "message": "Blockchain server is down at the moment, please retry after some time.", "data": null }
```

---

## ERROR HANDLING RULES

| Scenario | Action |
|---|---|
| Socket closes unexpectedly | Auto-reconnect after 3 seconds |
| `messages_response` with `status: "error"` | Show error in UI, do not crash |
| `txHash` is null in a message | Message was saved to DB but not on blockchain — show a warning icon |
| Socket not open when calling `fetchMessages` | Wait for `readyState === WebSocket.OPEN` before sending |
| Server unreachable on connect | Keep retrying with backoff |

---

## ⚠️ SAME DATA — WHICH SOCKET SHOULD YOU USE?

Since both sockets receive the **same** `new_message` push with the **same** data, here is the recommended usage:

| Situation | Use |
|---|---|
| You only need real-time delivery (no history load) | `/chat` |
| You need to load chat history AND get real-time delivery | `/ws/messages` |
| You want both history + live updates in one place | `/ws/messages` only is enough |
| You are already connected to `/ws/messages` | You can skip `/chat` — it is redundant |

> ✅ **Simplest approach**: Only open `/ws/messages`. It does everything — history fetch AND real-time push.
> Open `/chat` additionally only if your architecture requires a separate connection for delivery vs. history.

---

## SUMMARY — What each socket does

```
ws://localhost:9001/chat
  ├── You send  → { type: "login",    userId }
  ├── Server →  { type: "login_success" }
  └── Server →  { type: "new_message", data: ChatMessageView }   ← pushed on receive

ws://localhost:9001/ws/messages
  ├── You send  → { type: "register",     userId }
  ├── Server →  { type: "register_success" }
  ├── You send  → { type: "get_messages", userId, sender?, receiver? }
  ├── Server →  { type: "messages_response", status, data: ChatMessageView[] }
  └── Server →  { type: "new_message",    data: ChatMessageView }  ← SAME data as /chat

NOTE: ChatMessageView shape is IDENTICAL on both sockets:
{
  chatId, message, fromUser, toUser, algo, createdTs, txHash, publicKey
}
```
