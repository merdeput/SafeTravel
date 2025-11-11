```
com.safetravel.app
│
├── data                    <-- The "Model" layer (data handling)
│   ├── api                 <-- Network API interfaces (e.g., for Retrofit)
│   ├── model               <-- Data classes (e.g., User, Trip, Weather)
│   └── repository          <-- Handles fetching/saving data (e.g., AuthRepository)
│
├── di                      <-- Dependency Injection (Hilt)
│   └── AppModule.kt        <-- Provides app-wide dependencies (AI, API, Repos)
│
├── ui                      <-- "View" & "ViewModel" layers (all screens)
│   │
│   ├── auth                <-- Login/Register feature
│   │   ├── LoginFragment.kt
│   │   └── LoginViewModel.kt
│   │
│   ├── home                <-- Main Activity host
│   │   └── HomeActivity.kt
│   │
│   ├── trip_setup          <-- "Before Trip" feature
│   │   ├── BeforeTripFragment.kt
│   │   └── BeforeTripViewModel.kt
│   │
│   ├── trip_live           <-- "In Trip" feature (Map, SOS button)
│   │   ├── InTripFragment.kt
│   │   └── InTripViewModel.kt
│   │
│   ├── sos                 <-- SOS logic & Accident Detection
│   │   ├── AccidentDetectionService.kt
│   │   └── SOSViewModel.kt
│   │
│   └── emergency_ai        <-- AI Chat/Speech feature
│       ├── EmergencyAiFragment.kt
│       └── EmergencyAiViewModel.kt
│
└── SafeTravelApplication.kt  <-- The main Application class for Hilt
```