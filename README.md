# chat-service-backend

## Firebase Cloud Messaging (FCM) Setup

### 1. Get your service-account JSON
1. Open [Firebase Console](https://console.firebase.google.com/) → your project → **Project Settings** → **Service Accounts**.
2. Click **Generate New Private Key** → download the JSON file.
3. Rename it `firebase-service-account.json` and place it in the **root of this project** (same folder as `pom.xml`).

> The default path the app looks for is `./firebase-service-account.json` (relative to the working directory when the JAR is started).

### 2. Override the path via environment variable (optional)
If you want to store the file elsewhere, set the environment variable before starting the app:

```bash
# Linux / macOS
export FIREBASE_SERVICE_ACCOUNT_PATH=/etc/secrets/firebase-service-account.json

# Windows CMD
set FIREBASE_SERVICE_ACCOUNT_PATH=C:\secrets\firebase-service-account.json

# Windows PowerShell
$env:FIREBASE_SERVICE_ACCOUNT_PATH="C:\secrets\firebase-service-account.json"
```

Or pass it as a Spring Boot property:
```bash
java -jar chat-service-backend-0.0.1-SNAPSHOT.jar \
     --firebase.service-account-path=/etc/secrets/firebase-service-account.json
```

### 3. Run the DB migration
Execute the following script against your `db_chat` MySQL database **once**:

```bash
mysql -u root -p db_chat < fcm_migration.sql
```

This adds three columns to the `users` table:
| Column | Type | Purpose |
|---|---|---|
| `fcm_token` | VARCHAR(512) | Device push token |
| `fcm_platform` | VARCHAR(16) | `android` / `ios` / `other` |
| `fcm_token_updated_at` | DATETIME | Last token refresh timestamp |

### 4. New API endpoint

#### `POST /updateFcmToken`
Called by the Flutter client after every login and on FCM token refresh.

**Request:**
```json
{
  "userId":   "abc123",
  "fcmToken": "fGz...long_token...",
  "platform": "android"
}
```
Send `fcmToken` as `null` or omit it to clear the token (logout scenario).

**Response (always HTTP 200):**
```json
{ "status": "1", "message": "FCM token updated" }
{ "status": "0", "message": "User not found" }
```

### 5. How push notifications work
- After a message is saved to DB, `FcmService.sendChatNotificationAsync()` fires **asynchronously** — it never delays the `/sendMessage` response.
- The notification body is always `"📩 New encrypted message"`. Decrypted content is **never** sent — the server only holds ciphertext.
- Data payload keys: `type`, `senderId`, `senderName`, `chatId`, `timestamp`.
- If Firebase returns `UNREGISTERED` or `INVALID_ARGUMENT`, the dead token is automatically cleared from the DB.
- If `firebase-service-account.json` is missing, FCM is silently skipped and a warning is logged — the app starts normally.
