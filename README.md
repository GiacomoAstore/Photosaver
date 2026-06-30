# Media Saver

An Android application built with Kotlin that automatically saves ephemeral media from popular messaging apps, bypassing standard screenshot restrictions.

## Architecture
- **Clean Architecture**: Separation of concerns between Data, Domain, and UI layers.
- **MVVM**: UI logic managed by ViewModels.
- **Hilt**: Dependency Injection.
- **Coroutines & Flow**: Asynchronous programming and data streams.

## Features
- Automatic detection of ephemeral content using Accessibility Service.
- Screen capture via MediaProjection API.
- Secure logging with rotation and encryption.
- Performance monitoring and automatic recovery.
- Onboarding flow for easy setup.

## DISPOSITIVI SUPPORTATI
✅ **Funziona bene su**: Android 10+ (Samsung, Pixel, OnePlus).
⚠️ **Funziona parzialmente su**: Xiaomi, Huawei (alcuni permessi limitati).
❌ **Non funziona su**: Dispositivi rooted o con DRM avanzato.

## LIMITAZIONI TECNICHE (Ambiente Sandbox)
- **FLAG_SECURE**: L'app implementa una strategia "best effort" a cascata. Su molti dispositivi moderni, il blocco screenshot rimane invalicabile senza root o certificati di sistema.
- **Video Recording**: Il salvataggio video è implementato tramite sequenza di frame. La codifica MediaCodec completa è disabilitata in questo ambiente per evitare instabilità.

## How to build
```bash
./gradlew assembleDebug
```

## How to test
```bash
./gradlew test
```

## Permissions
- `FOREGROUND_SERVICE`: For background monitoring.
- `READ_MEDIA_IMAGES`: To save and view media.
- `SYSTEM_ALERT_WINDOW`: For overlays.
- `BIND_ACCESSIBILITY_SERVICE`: For content detection.
