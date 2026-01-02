# Moon Phase

An Android companion app for [moon-phase](https://github.com/sreevardhanreddi/moon-phase) web application.

Displays moon phases with Hindu lunar calendar (Tithi/Paksha) integration and home screen widgets.

## Screenshots

<p align="center">
  <img src="screenshots/Screenshot_20260102_104802.png" width="280" alt="Moon Phase App - Main Screen"/>
  &nbsp;&nbsp;
  <img src="screenshots/Screenshot_20260102_104815.png" width="280" alt="Moon Phase App - Widgets"/>
</p>

## Features

- **Moon Phase Visualization** - Accurate moon phase rendering with shadow
- **Hindu Lunar Calendar** - Tithi, Paksha (Shukla/Krishna), and special days
- **Date Picker** - View moon phases for any date
- **Theme Support** - System, Light, and Dark (AMOLED) themes
- **Language Toggle** - Switch between English and Hindu terminology
- **Home Screen Widgets** - 6 sizes (1x1, 2x1, 3x1, 4x1, 2x2, 4x2)

## Widgets

| Size | Content                                |
| ---- | -------------------------------------- |
| 1x1  | Moon icon + tithi number               |
| 2x1  | Moon + tithi + moon age                |
| 3x1  | Moon + tithi + illumination %          |
| 4x1  | Moon + tithi + illumination + phase    |
| 2x2  | Moon + tithi + illumination + moon age |
| 4x2  | Full info + next new/full moon dates   |

## Tech Stack

- Kotlin
- Jetpack Compose
- Glance (Compose widgets)
- Material 3

## Build

Open in Android Studio and run on device/emulator.

```
./gradlew assembleDebug
```

## License

MIT
