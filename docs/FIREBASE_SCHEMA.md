# BookLog Firebase schema

Complete schema for **Firebase Auth** and **Cloud Firestore** as used by the BookLog Android app.

| Item | Value |
|------|--------|
| Example project id | `booklog-b99fc` |
| Android package / applicationId | `com.brk.booklogger` |
| Source of truth in code | `CloudRepository.kt` |

> Do **not** commit `google-services.json` or API keys. Keep them local only (see `.gitignore`).

---

## 1. Authentication (Firebase Auth)

| Provider | Purpose |
|----------|---------|
| Email / Password | Sign up + sign in |
| Google | Sign-in with Google ID token |

### User identity

| Field | Source |
|-------|--------|
| `uid` | Firebase Auth user id (used as `users/{uid}`) |
| `email` | Auth email |
| `displayName` | Auth profile / sign-up name |

No custom Auth claims are required for v1.

---

## 2. Firestore tree overview

```text
users/{uid}
  books/{bookId}
  kids/{localReaderId}

households/{householdId}
  readers/{readerCloudId}
  books/{bookCloudId}

household_invites/{inviteCode}

leaderboard/{uid}
leaderboard_kids/{scopeId_kid_{localId}}
leaderboard_milestones/{uid}
leaderboard_authors/{slug}
leaderboard_publishers/{slug}
leaderboard_genres/{slug}
```

### Library modes

| Mode | Where the shared library lives |
|------|--------------------------------|
| Solo (no partner) | `users/{uid}/books` + `users/{uid}/kids` |
| Linked household | `households/{id}/books` + `households/{id}/readers` (and `users/{uid}.householdId`) |

---

## 3. Collections and fields

### `users/{uid}`

**Document id:** Firebase Auth `uid`

| Field | Type | Notes |
|-------|------|--------|
| `displayName` | string | |
| `email` | string | |
| `householdId` | string \| null | Set when linked to a household |
| `booksFinished` | number | Aggregate stats |
| `pagesRead` | number | |
| `booksTotal` | number | |
| `milestonesUnlocked` | number | |
| `unlockedMilestoneIds` | array of string | Milestone ids |
| `createdAt` | number | Unix ms |
| `updatedAt` | number | Unix ms |

---

### `users/{uid}/kids/{localReaderId}` (solo accounts)

**Document id:** local Room reader id as string (e.g. `"3"`)

Readers are adults or children (`profileType`). Table name `kids` is historical.

| Field | Type | Notes |
|-------|------|--------|
| `localId` | number | Room id |
| `cloudId` | string \| null | UUID for multi-device / household identity |
| `name` | string | |
| `emoji` | string | |
| `gender` | string | See enums below |
| `dateOfBirth` | number \| null | Unix ms |
| `favoriteGenre` | string | |
| `notes` | string | |
| `profileType` | string | `CHILD` or `ADULT` |
| `createdAt` | number | |
| `updatedAt` | number | |
| `booksFinished` | number | Optional stats |
| `pagesRead` | number | |
| `booksTotal` | number | |
| `milestonesUnlocked` | number | |

---

### `users/{uid}/books/{bookId}` (solo accounts)

**Document id:** preferably book `cloudId` (UUID); may fall back to local id string

| Field | Type | Notes |
|-------|------|--------|
| `localId` | number | Room book id |
| `cloudId` | string | UUID |
| `isbn` | string \| null | |
| `title` | string | |
| `author` | string | |
| `publisher` | string \| null | |
| `genre` | string \| null | |
| `description` | string \| null | |
| `kidProfileId` | number \| null | Local reader id (**who read it**) |
| `readerCloudId` | string \| null | Reader UUID when known (**who read it**) |
| `coverUrl` | string \| null | Cover **URL text only** (not image bytes). New devices use this; if missing, app falls back to Open Library. |
| `pageCount` | number \| null | |
| `publishedYear` | string \| null | |
| `status` | string | See enums |
| `rating` | number \| null | Float |
| `notes` | string | |
| `dateAdded` | number | |
| `dateStarted` | number \| null | |
| `dateFinished` | number \| null | |
| `currentPage` | number \| null | |
| `lastEditedByUid` | string \| null | Auth uid of last editor |
| `updatedAt` | number | |

---

### `households/{householdId}`

**Document id:** UUID

| Field | Type | Notes |
|-------|------|--------|
| `memberUids` | array of string | Max **2** partners in app v1 |
| `inviteCode` | string | 6-character code (e.g. `A3K9PQ`) |
| `createdBy` | string | Creator uid |
| `createdAt` | number | |
| `updatedAt` | number | |

---

### `households/{householdId}/readers/{readerCloudId}`

**Document id:** reader `cloudId` (UUID)

Same core reader fields as solo `kids`, plus stats:

| Field | Type |
|-------|------|
| `localId` | number |
| `cloudId` | string |
| `name` | string |
| `emoji` | string |
| `gender` | string |
| `dateOfBirth` | number \| null |
| `favoriteGenre` | string |
| `notes` | string |
| `profileType` | string (`CHILD` \| `ADULT`) |
| `createdAt` | number |
| `updatedAt` | number |
| `booksFinished` | number |
| `pagesRead` | number |
| `booksTotal` | number |

---

### `households/{householdId}/books/{bookCloudId}`

**Document id:** book `cloudId` (UUID)

Same book fields as `users/{uid}/books` (including `readerCloudId`, `lastEditedByUid`, `updatedAt`).

---

### `household_invites/{inviteCode}`

**Document id:** invite code (uppercase), e.g. `A3K9PQ`

| Field | Type | Notes |
|-------|------|--------|
| `householdId` | string | Target household |
| `createdBy` | string | Creator uid |
| `createdAt` | number | |

Used to resolve join codes quickly when a partner joins.

---

## 4. Leaderboard collections

### `leaderboard/{uid}`

| Field | Type |
|-------|------|
| `displayName` | string |
| `booksFinished` | number |
| `pagesRead` | number |
| `isKidProfile` | boolean | `false` for account-level rows |
| `updatedAt` | number |

### `leaderboard_kids/{scopeId_kid_{localId}}`

**Document id:** `{parentUidOrHouseholdId}_kid_{localRoomId}`

| Field | Type |
|-------|------|
| `displayName` | string |
| `emoji` | string |
| `parentUid` | string | Scope id (user uid or household id) |
| `kidProfileId` | number |
| `profileType` | string |
| `booksFinished` | number |
| `pagesRead` | number |
| `updatedAt` | number |

### `leaderboard_milestones/{uid}`

| Field | Type |
|-------|------|
| `displayName` | string |
| `milestonesUnlocked` | number |
| `updatedAt` | number |

### `leaderboard_authors/{slug}`  
### `leaderboard_publishers/{slug}`  
### `leaderboard_genres/{slug}`

**Document id:** slugified name (lowercase; non-alphanumeric runs → `-`)

| Field | Type | Notes |
|-------|------|--------|
| `name` | string | Display name |
| `booksFinished` | number | Incremented on finished books |
| `pagesRead` | number | Incremented |
| `updatedAt` | number | |

---

## 5. Enums stored as strings

### Book `status`

| Value |
|-------|
| `WANT_TO_READ` |
| `READING` |
| `FINISHED` |

### Reader `profileType`

| Value |
|-------|
| `CHILD` |
| `ADULT` |

### Reader `gender`

| Value |
|-------|
| `BOY` |
| `GIRL` |
| `OTHER` |
| `PREFER_NOT_TO_SAY` |

---

## 6. Relationships

```text
Auth user ──► users/{uid}
                 │
                 ├── householdId ──► households/{householdId}
                 │                      ├── memberUids[] includes uid
                 │                      ├── readers/*
                 │                      └── books/*
                 │
                 ├── kids/*     (solo library readers)
                 └── books/*    (solo library books)

household_invites/{code} ──► householdId
```

- **Shared library:** both partners use the same `households/{id}/readers` and `books`.
- **Solo library:** data stays under `users/{uid}/...` until a household is created or joined.

---

## 7. Composite indexes

Create these if the Console reports a missing index on leaderboard queries:

| Collection | Fields | Order |
|------------|--------|--------|
| `leaderboard` | `booksFinished`, `pagesRead` | both DESC |
| `leaderboard_kids` | `booksFinished`, `pagesRead` | both DESC |
| `leaderboard_milestones` | `milestonesUnlocked` | DESC |
| `leaderboard_authors` | `booksFinished` | DESC |
| `leaderboard_publishers` | `booksFinished` | DESC |
| `leaderboard_genres` | `booksFinished` | DESC |

Single-field indexes are usually created automatically.

---

## 8. Security rules

### Dev / test (any signed-in user)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

### Production-oriented sketch

Tighten before a public release. Invite join may need Cloud Functions for the strongest security model.

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function signedIn() {
      return request.auth != null;
    }
    function isSelf(uid) {
      return signedIn() && request.auth.uid == uid;
    }
    function isHouseholdMember(hid) {
      return signedIn()
        && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.householdId == hid;
    }

    match /users/{uid} {
      allow read: if signedIn();
      allow write: if isSelf(uid);

      match /books/{bookId} {
        allow read, write: if isSelf(uid);
      }
      match /kids/{kidId} {
        allow read, write: if isSelf(uid);
      }
    }

    match /households/{hid} {
      allow read: if isHouseholdMember(hid);
      allow create: if signedIn();
      allow update, delete: if isHouseholdMember(hid);

      match /readers/{rid} {
        allow read, write: if isHouseholdMember(hid);
      }
      match /books/{bid} {
        allow read, write: if isHouseholdMember(hid);
      }
    }

    match /household_invites/{code} {
      allow read: if signedIn();
      allow write: if signedIn();
    }

    match /leaderboard/{id} {
      allow read, write: if signedIn();
    }
    match /leaderboard_kids/{id} {
      allow read, write: if signedIn();
    }
    match /leaderboard_milestones/{id} {
      allow read, write: if signedIn();
    }
    match /leaderboard_authors/{id} {
      allow read, write: if signedIn();
    }
    match /leaderboard_publishers/{id} {
      allow read, write: if signedIn();
    }
    match /leaderboard_genres/{id} {
      allow read, write: if signedIn();
    }
  }
}
```

For first partner testing, the open authenticated rules are enough.

---

## 9. Not stored in Firestore (local only)

These remain on-device (Room / SharedPreferences / Coil):

- **Cover image files (pixels)** — only the `coverUrl` **string** is in Firestore; JPEG/PNG bytes are downloaded by Coil and cached on the device.  
- Reward transactions and balances  
- Reading day logs / streaks detail  
- Full completed-book history rows (beyond book `status` sync)  
- Audio preferences  
- Active reader selection  
- Guest mode flag  
- Local household id / invite code cache  

### Cover resolution flow

```text
Push: local coverUrl string → Firestore book.coverUrl  (text only, not the image)

Pull / new phone / partner share:
  Firebase book.coverUrl if present
        │ else
  resolveCoverFromCatalog(isbn, title, author) → Open Library
        ▼
Local Room.coverUrl + Coil loads/caches image bytes on device
```

---

## 10. Example documents

### User

```json
{
  "displayName": "Alex",
  "email": "alex@example.com",
  "householdId": "h-uuid-1",
  "booksFinished": 12,
  "pagesRead": 3400,
  "booksTotal": 20,
  "milestonesUnlocked": 5,
  "unlockedMilestoneIds": ["first_book", "ten_books"],
  "createdAt": 1723000000000,
  "updatedAt": 1723100000000
}
```

### Household

```json
{
  "memberUids": ["uidA", "uidB"],
  "inviteCode": "A3K9PQ",
  "createdBy": "uidA",
  "createdAt": 1723000000000,
  "updatedAt": 1723050000000
}
```

### Household reader

```json
{
  "localId": 2,
  "cloudId": "r-uuid-2",
  "name": "Sam",
  "emoji": "📚",
  "gender": "PREFER_NOT_TO_SAY",
  "dateOfBirth": 1262304000000,
  "favoriteGenre": "Fantasy",
  "notes": "",
  "profileType": "CHILD",
  "booksFinished": 4,
  "pagesRead": 900,
  "booksTotal": 6,
  "createdAt": 1723000000000,
  "updatedAt": 1723100000000
}
```

### Household book

```json
{
  "localId": 15,
  "cloudId": "b-uuid-15",
  "isbn": "9781234567890",
  "title": "The Hobbit",
  "author": "J.R.R. Tolkien",
  "publisher": "Allen & Unwin",
  "genre": "Fantasy",
  "description": null,
  "kidProfileId": 2,
  "readerCloudId": "r-uuid-2",
  "coverUrl": "https://covers.openlibrary.org/b/isbn/9781234567890-L.jpg",
  "pageCount": 310,
  "publishedYear": "1937",
  "status": "FINISHED",
  "rating": 5.0,
  "notes": "",
  "dateAdded": 1723000000000,
  "dateStarted": 1723010000000,
  "dateFinished": 1723090000000,
  "currentPage": null,
  "lastEditedByUid": "uidA",
  "updatedAt": 1723090000000
}
```

### Invite

```json
{
  "householdId": "h-uuid-1",
  "createdBy": "uidA",
  "createdAt": 1723000000000
}
```

---

## 11. Client configuration (local only)

| File | Location | Git |
|------|----------|-----|
| `google-services.json` | `app/google-services.json` | **Ignored** — never commit |

Download from Firebase Console → Project settings → Your apps → Android (`com.brk.booklogger`).

Web OAuth client id (`client_type: 3`) is generated into resources at build time by the Google Services plugin from that file. Do not hardcode secrets in tracked source.
