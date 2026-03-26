# Delete Message API — Frontend Integration Prompt

---

## CONTEXT

You are implementing the **delete message** feature for a secure chat application.
The backend is a Spring Boot application running at `http://localhost:9001`.

There are **3 DELETE APIs** available. All return the same Response shape:

```json
{ "status": "1" or "0", "message": "...", "data": "..." }
```

- `status: "1"` → success
- `status: "0"` → failure (check `message` for reason)

---

## API 1 — Delete a Single Message

### Endpoint
```
DELETE http://localhost:9001/deleteMessage/{chatId}?userId={userId}
```

### Rules
- Only the **original sender** of the message can delete it.
- If `userId` does not match the sender, returns failure — no deletion happens.

### Parameters
| Parameter | Type | Where | Required | Description |
|---|---|---|---|---|
| `chatId` | String (UUID) | Path variable | ✅ Yes | The unique ID of the message to delete |
| `userId` | String | Query param | ✅ Yes | The logged-in user's ID (must be the sender) |

### Example Request
```
DELETE http://localhost:9001/deleteMessage/550e8400-e29b-41d4-a716-446655440000?userId=42
```

### Success Response
```json
{
  "status": "1",
  "message": "Message deleted successfully",
  "data": null
}
```

### Failure — Not the sender or message not found
```json
{
  "status": "0",
  "message": "Message not found or you are not the sender",
  "data": null
}
```

### Frontend Code
```javascript
async function deleteMessage(chatId, userId) {
  try {
    const response = await fetch(
      `http://localhost:9001/deleteMessage/${chatId}?userId=${userId}`,
      { method: "DELETE" }
    );
    const result = await response.json();

    if (result.status === "1") {
      console.log("Message deleted:", result.message);
      // Remove the message from the UI
      removeMessageFromUI(chatId);
    } else {
      console.error("Delete failed:", result.message);
      // e.g. show toast: "You can only delete your own messages"
    }
  } catch (err) {
    console.error("Network error:", err);
  }
}
```

---

## API 2 — Delete an Entire Conversation

### Endpoint
```
DELETE http://localhost:9001/deleteConversation?userId1={userId1}&userId2={userId2}
```

### Rules
- Deletes **all messages in both directions** between the two users.
- Both `userId1` and `userId2` are required.
- Order of userId1/userId2 does not matter.

### Parameters
| Parameter | Type | Where | Required | Description |
|---|---|---|---|---|
| `userId1` | String | Query param | ✅ Yes | First user's ID |
| `userId2` | String | Query param | ✅ Yes | Second user's ID |

### Example Request
```
DELETE http://localhost:9001/deleteConversation?userId1=42&userId2=7
```

### Success Response
```json
{
  "status": "1",
  "message": "Conversation deleted successfully",
  "data": "5 message(s) deleted"
}
```

### Failure — Missing parameters
```json
{
  "status": "0",
  "message": "Both userId1 and userId2 are required",
  "data": null
}
```

### Frontend Code
```javascript
async function deleteConversation(userId1, userId2) {
  try {
    const response = await fetch(
      `http://localhost:9001/deleteConversation?userId1=${userId1}&userId2=${userId2}`,
      { method: "DELETE" }
    );
    const result = await response.json();

    if (result.status === "1") {
      console.log("Conversation deleted:", result.data);
      // Clear all messages from the chat UI
      clearChatUI();
    } else {
      console.error("Delete failed:", result.message);
    }
  } catch (err) {
    console.error("Network error:", err);
  }
}
```

---

## API 3 — Delete All Messages for a User

### Endpoint
```
DELETE http://localhost:9001/deleteAllMessages/{userId}
```

### Rules
- Deletes every message the user has ever **sent or received**.
- This is a full wipe — use with a confirmation dialog on the frontend.

### Parameters
| Parameter | Type | Where | Required | Description |
|---|---|---|---|---|
| `userId` | String | Path variable | ✅ Yes | The user whose messages will all be deleted |

### Example Request
```
DELETE http://localhost:9001/deleteAllMessages/42
```

### Success Response
```json
{
  "status": "1",
  "message": "All messages deleted successfully",
  "data": "23 message(s) deleted"
}
```

### Failure — Missing userId
```json
{
  "status": "0",
  "message": "userId is required",
  "data": null
}
```

### Frontend Code
```javascript
async function deleteAllMessages(userId) {
  // Always show a confirmation dialog before wiping all messages
  const confirmed = window.confirm(
    "Are you sure you want to delete ALL your messages? This cannot be undone."
  );
  if (!confirmed) return;

  try {
    const response = await fetch(
      `http://localhost:9001/deleteAllMessages/${userId}`,
      { method: "DELETE" }
    );
    const result = await response.json();

    if (result.status === "1") {
      console.log("All messages deleted:", result.data);
      // Clear entire chat history from UI
      clearAllChatsUI();
    } else {
      console.error("Delete failed:", result.message);
    }
  } catch (err) {
    console.error("Network error:", err);
  }
}
```

---

## All 3 APIs — Quick Reference Table

| API | Method | URL | What it deletes |
|---|---|---|---|
| Delete single message | `DELETE` | `/deleteMessage/{chatId}?userId={userId}` | One message (sender only) |
| Delete conversation | `DELETE` | `/deleteConversation?userId1={id1}&userId2={id2}` | All messages between two users |
| Delete all messages | `DELETE` | `/deleteAllMessages/{userId}` | Every message sent or received by user |

---

## Response Shape (same for all 3 APIs)

```json
{
  "status":  "1",       // "1" = success, "0" = failure
  "message": "...",     // human-readable result message
  "data":    "..."      // count of deleted rows (or null for single delete)
}
```

---

## Error Handling Rules

| Scenario | What backend returns | Frontend action |
|---|---|---|
| Not the sender of the message | `status: "0"`, `"not the sender"` | Show error toast |
| chatId does not exist | `status: "0"`, `"not found"` | Remove from UI anyway (already gone) |
| Missing required parameter | `status: "0"`, `"is required"` | Validate before calling API |
| Server error | `status: "0"`, error details | Show generic error message |

---

## Complete Usage Example (all 3 together)

```javascript
const BASE_URL = "http://localhost:9001";
const loggedInUserId = "42"; // from login response

// 1. Delete a single message (long press / swipe on message)
deleteMessage("550e8400-e29b-41d4-a716-446655440000", loggedInUserId);

// 2. Delete full conversation with user id 7 (clear chat button)
deleteConversation(loggedInUserId, "7");

// 3. Delete all messages for the logged-in user (settings → clear all)
deleteAllMessages(loggedInUserId);
```

