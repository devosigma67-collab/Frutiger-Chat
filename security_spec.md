# Frutiger Chat — Firestore Security Specification (Phase 0 TDD & Red Team Audit)

## 1. Data Invariants
1. **Authentication Gate**: All reads and writes across `/users`, `/friends`, `/chats`, and `/chats/{chatId}/messages` require `request.auth != null`.
2. **User Isolation & Ownership**:
   - `/users/{userId}` can only be read, created, updated, or deleted where `request.auth.uid == userId` and `data.userId == request.auth.uid`.
   - `/friends/{friendDocId}` documents are strictly scoped to `data.userId == request.auth.uid`.
   - `/chats/{chatId}` documents are strictly scoped to `data.ownerId == request.auth.uid`.
   - `/chats/{chatId}/messages/{messageId}` subcollection documents denormalize `ownerId` and `chatId` for zero-latency flat authorization checks (`data.ownerId == request.auth.uid && data.chatId == chatId`).
3. **Immutable Fields**:
   - `userId`, `ownerId`, `chatId`, `senderId`, `ciphertext`, `iv`, `signatureHash`, and `createdAt` cannot be modified after document creation.
4. **Temporal Integrity**:
   - All `createdAt` and `updatedAt` fields must be valid Firestore `timestamp` objects (`<= request.time`) populated via `FieldValue.serverTimestamp()`.

## 2. The Dirty Dozen Payloads
1. Unauthenticated read on `/users/{uid}` -> REJECTED (`isSignedIn()`).
2. Unauthenticated list on `/chats` -> REJECTED (`isSignedIn()`).
3. Cross-user profile creation (`/users/bob_456` by `alice_123`) -> REJECTED (`request.auth.uid == userId`).
4. Ghost field injection (`isAdmin: true` on `/users/{uid}`) -> REJECTED (`keys().hasOnly(...)`).
5. Oversized username (`username.size() > 40`) -> REJECTED (`isValidUserProfile`).
6. Invalid presence enum (`presenceStatus: "hacked"`) -> REJECTED (`isValidPresence`).
7. Unfiltered query on `/chats` without `.whereEqualTo("ownerId", uid)` -> REJECTED (`existing().ownerId == request.auth.uid`).
8. Cross-user read on `/chats/{bobChatId}` -> REJECTED (`existing().ownerId == request.auth.uid`).
9. Mutating `ownerId` or `createdAt` on `/chats/{chatId}` update -> REJECTED (`affectedKeys().hasOnly(...)` + immutability checks).
10. Tampering with E2EE `ciphertext` or `iv` on `/chats/{chatId}/messages/{msgId}` update -> REJECTED (`incoming().ciphertext == existing().ciphertext`).
11. Path poisoning with invalid document ID characters or `size() > 128` -> REJECTED (`isValidId`).
12. Value poisoning (`unreadCount: "not_an_int"` or negative integer) -> REJECTED (`isValidChatRoom`).

## 3. Red Team Audit Summary
- **Shadow Update Test**: PASS — `hasOnly` and `affectedKeys().hasOnly()` block undeclared fields across all 4 collections.
- **PII Blanket Test**: PASS — No email/phone/address stored in Firestore documents; all documents strictly isolated to owner UID.
- **Query Trust Test**: PASS — Every `allow list` verifies `existing().ownerId == request.auth.uid` or `existing().userId == request.auth.uid`.
- **Value Poisoning Test**: PASS — `isValid[Entity](incoming())` wraps every `create` and `update` rule.
