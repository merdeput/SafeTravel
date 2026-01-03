# SafeTravel App Deployment Guide

This guide covers getting the necessary API keys, setting up the backend URL, and installing the app on a physical Android device.

---

## 1. Prerequisites
*   **Android Studio** installed on your computer.
*   **Android Phone** with **Developer Options** enabled.
*   **USB Cable** to connect the phone.
*   The **backend server** running (either locally or on a public server).

---

## 2. Setup Base URL (Backend Connection)

Since you are deploying to a real phone, the app needs to know where your backend server is located.

*   **If your backend is hosted online (e.g., AWS, Heroku, Render, hiulaptop.dev):**
    1.  Open `app/src/main/java/com/safetravel/app/di/NetworkModule.kt`.
    2.  Change the `.baseUrl(...)` to your public HTTPS URL.
        ```kotlin
        .baseUrl("https://your-backend-domain.com/") // Ensure it ends with /
        ```

*   **If your backend is running on your laptop (Localhost):**
    *   Your phone cannot access `localhost` or `10.0.2.2` (that is for emulators only). You must use your computer's **local IP address**.
    1.  Find your computer's IP address:
        *   **Windows**: Open Command Prompt -> type `ipconfig` -> look for `IPv4 Address` (e.g., `192.168.1.15`).
        *   **Mac/Linux**: Open Terminal -> type `ifconfig` -> look for `en0` or `wlan0` IP (e.g., `192.168.1.15`).
    2.  Open `NetworkModule.kt` and update the URL:
        ```kotlin
        .baseUrl("http://192.168.1.15:8000/") // Use your actual IP
        ```
    3.  **Important**: Ensure both your phone and laptop are connected to the **same Wi-Fi network**.

---

## 3. Setup Google Maps API Key

To display the map on a real device, you need a valid Google Maps API Key restricted to your app's package name (`com.safetravel.app`).

1.  **Get the Key**:
    *   Go to the [Google Cloud Console](https://console.cloud.google.com/).
    *   Create a project (or use an existing one).
    *   Enable **Maps SDK for Android**.
    *   Go to **Credentials** -> **Create Credentials** -> **API Key**.

2.  **Add Key to Project**:
    *   Open `local.properties` (in the root of your project) and add:
        ```properties
        MAPS_API_KEY=your_api_key_here
        ```
    *   *Alternatively*, you can hardcode it in `AndroidManifest.xml` (not recommended for public repos but fine for testing):
        ```xml
        <meta-data
            android:name="com.google.android.geo.API_KEY"
            android:value="your_api_key_here" />
        ```

---

## 4. Build and Install on Phone

1.  **Enable Developer Mode on Phone**:
    *   Go to **Settings** -> **About Phone**.
    *   Tap **Build Number** 7 times until it says "You are a developer".
    *   Go back to **Settings** -> **System** -> **Developer Options**.
    *   Enable **USB Debugging**.

2.  **Connect and Run**:
    *   Connect your phone to the PC via USB.
    *   In Android Studio, check the device dropdown in the toolbar. It should show your phone's name (e.g., "Samsung SM-G990").
    *   Click the green **Run** button (Play icon).
    *   Accept the installation prompt on your phone if asked.

---

## 5. Troubleshooting Common Issues

*   **"Cleartext Traffic Not Permitted"**:
    *   If using `http://` (not https), ensure this line is in your `AndroidManifest.xml` inside the `<application>` tag:
        ```xml
        android:usesCleartextTraffic="true"
        ```

*   **App crashes on Map load**:
    *   Check your Logcat in Android Studio. If you see "Authentication failed", your Google Maps API Key is missing or incorrect.

*   **Cannot connect to Backend (Timeout/Error)**:
    *   Double-check the IP address in `NetworkModule.kt`.
    *   Make sure your firewall allows traffic on port 8000 (or whichever port your backend uses).
    *   Try opening the API URL (e.g., `http://192.168.1.15:8000/docs`) in your **phone's Chrome browser** to verify connectivity.
