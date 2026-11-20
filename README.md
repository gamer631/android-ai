# Android AI (v2.1 starter)

Android app starter for vivo Y04 and Android 8+ with basic voice recognition, flashlight, battery/time, app shortcuts, and APK picker flow.

## Build
GitHub Actions builds a debug APK on pushes to main. Successful builds publish the APK to the `android-ai-apk` release and as an Actions artifact.

## USB
USB device attachment can trigger the activity on some devices. USB mass-storage drives may not trigger this event consistently across OEM devices. Choose APK manually from Files. Android requires user confirmation; silent installation is not supported for ordinary apps.

## Limitations
No LLM/Python engine, persistent wake-word detection, or full offline ASR is included yet. Speech recognition uses the device's speech service. APK is a debug build, not a Play Store release.
