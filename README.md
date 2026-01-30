# ATI Limited — Android Intern Technical Task (Java + XML + Room)

## Overview
This project is a fully functional Android application built to meet ATI Limited’s mandatory technical task requirements.  
It demonstrates:
- Background location tracking using a Foreground Service
- Periodic location logging (every 5 minutes)
- Local data persistence using Room
- Public API integration with input validation and error handling
---

## Features Implemented

### 1) Background Location Service (Mandatory)
✅ The location service continues running when:
- The app is closed
- The app is removed from recent apps (Recents)

✅ The service fetches location **every 5 minutes**  
✅ Each fetch:
- Shows a **Toast** with latitude and longitude
- Saves the location record into **Room database**

✅ Service uses a **Foreground Notification** (required by Android for long-running background work)

✅ Best-effort restart mechanisms included:
- Restart when app is removed from Recents (`onTaskRemoved` + receiver)
- Restart after device reboot (`BOOT_COMPLETED` receiver)

---

### 2) Local Database (Room) (Mandatory)
✅ Room database stores location logs in table: `location_logs`  
Each record contains:
- `id` (auto-generated)
- `latitude`
- `longitude`
- `timestamp`

✅ App includes a UI option to display the latest saved logs inside the app.

Database file name: `ati_task.db`

---

### 3) API Integration (Mandatory)
✅ Fetches and displays data from a free public API:
- JSONPlaceholder: `https://jsonplaceholder.typicode.com/posts/{id}`

✅ Input validation:
- Post ID must be a number
- Post ID must be between **1 and 100**

✅ Error handling:
- Handles HTTP errors (e.g., 404, 500)
- Handles empty/invalid responses
- Handles network failures (no internet, timeouts)

✅ API response is displayed on screen:
- Title
- Body

---

## Tech Stack
- Language: **Java**
- UI: **XML (Views)**
- Database: **Room**
- Location: **FusedLocationProviderClient (Google Play Services)**
- Networking: **Retrofit + Gson**
- Background execution: **Foreground Service**

---

## Permissions Used
Declared in `AndroidManifest.xml`:
- `ACCESS_FINE_LOCATION`
- `ACCESS_COARSE_LOCATION`
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_LOCATION`
- `RECEIVE_BOOT_COMPLETED`
- `INTERNET`

Runtime permission request:
- Location permissions are requested from `MainActivity` before starting the service.

---

## How to Run

### 1) Open and Run
1. Open the project in **Android Studio**
2. Make sure build variant is **debug**
3. Click **Run ▶** and install on a real device or emulator

### 2) Start Location Logging
1. Open the app
2. Tap **Start Location Service**
3. Allow location permission
4. You will see:
   - A persistent notification: “Location service running”
   - Toast messages showing latitude/longitude
5. Tap **Show Saved Location Logs** to view saved entries

### 3) Test API Integration
1. Enter a Post ID (1–100)
2. Tap **Fetch from API**
3. The post title and body will appear
4. Try invalid inputs (empty, non-numeric, >100) to see validation messages
5. Turn off internet to see network error handling

---

## Project Structure

## Project Structure

```
com.example.atilocationlogger
├── ui
│   └── MainActivity.java
├── location
│   ├── LocationForegroundService.java
│   ├── RestartServiceReceiver.java
│   └── BootReceiver.java
├── data
│   ├── AppDatabase.java
│   ├── LocationLog.java
│   └── LocationLogDao.java
└── network
    ├── ApiClient.java
    ├── ApiService.java
    └── Post.java
```



---
## Notes
- Location updates are scheduled at a 5-minute interval.
- Database and network operations run on background threads to avoid ANR.
- Designed following Android background execution best practices.
---

## Author
**Md Mahathir Imam**  
Prepared as part of ATI Limited Android Intern technical evaluation.

