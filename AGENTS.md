# Agent's Guide to Prism File Explorer

Welcome, fellow AI agent. This document is designed to give you a comprehensive understanding of the **Prism File Explorer** codebase, its architecture, and the intent behind its various components.

## 🌟 Project Overview

**Prism File Explorer** is a modern, lightweight, and feature-rich file manager for Android. It is built entirely using **Kotlin** and **Jetpack Compose**, adhering to **Material Design 3** principles.

### Key Goals:
- **Performance**: Fast startup and smooth navigation even with large directories.
- **Modernity**: Using the latest Android development tools (Compose, Coil 3, Media3).
- **Rich Integration**: Built-in viewers for various file types (Images, PDF, Video, Audio) and a powerful text editor with syntax highlighting.
- **Multi-tasking**: A tabbed interface allowing users to work in multiple directories simultaneously.

---

## 🛠️ Technology Stack

- **UI Framework**: Jetpack Compose (Material 3)
- **Image Loading**: Coil 3 (with custom decoders for APK, PDF, Zip, and Audio)
- **Media Playback**: Media3 (ExoPlayer)
- **Code Editor**: Sora-Editor with TextMate support
- **Archive Management**: Zip4j & REAndroid
- **Dependency Injection/Management**: Manual management via the `App` class (Singleton pattern for Managers).
- **State Management**: Kotlin Flows and Compose State.

---

## 🏗️ Architecture Overview

The project follows a clean separation of concerns, though not strictly adhering to a single pattern like MVVM or MVI. It uses **Managers** to handle business logic and **Tabs** to encapsulate state for different views.

### 🏢 Core Components

#### 1. `App.kt` (The Heart)
- **Intent**: Centralized initialization and dependency holder.
- **Responsibilities**:
    - Initializes global managers (`TextEditorManager`, `MainActivityManager`, etc.).
    - Configures **Coil 3** with custom decoders.
    - Sets up **TextMate** grammars and themes for the editor.
    - Provides a global exception handler.
    - Manages app-specific directories like the Recycle Bin.

#### 2. `MainActivity.kt` & `MainActivityManager.kt`
- **Intent**: The primary entry point and UI orchestrator.
- **Responsibilities**:
    - `MainActivity`: Handles the top-level UI, including the `Toolbar`, `TabLayout`, and `HorizontalPager` for tabs.
    - `MainActivityManager`: Manages the lifecycle and state of tabs (adding, removing, selecting, reordering). It also handles global UI states like update dialogs.

#### 3. `FilesTab.kt`
- **Intent**: The main logic for file browsing and operations.
- **Responsibilities**:
    - Manages the current directory state (`activeFolder`).
    - Handles file operations (open, select, navigate).
    - Detects file system changes in the background.
    - Integrates with `ZipManager` for archive browsing.
    - Uses `ContentHolder` as an abstraction for different file sources (Local, Virtual, Zip).

---

## 📂 Directory Structure & Intent

### `com.raival.compose.file.explorer`

- **`base/`**: Contains `BaseActivity`, which handles common logic like permission checks.
- **`coil/`**: Custom Coil components.
    - `apk/`, `audio/`, `pdf/`, `zip/`: Specific decoders to generate thumbnails for these file types.
- **`common/`**:
    - `ui/`: Reusable Compose components (e.g., `SafeSurface`, `BottomSheetDialog`).
    - `icons/`: Custom SVG icons converted to Compose VectorDrawables.
    - `Utils.kt`: A collection of extension functions for Strings, Files, and Android-specific tasks.
    - `FileExplorerLogger.kt`: Handles logging to files and console.
- **`screen/`**:
    - **`main/`**: The main interface.
        - `tab/`: Contains implementations for `FilesTab`, `HomeTab`, and `AppsTab`.
        - `ui/`: UI components specific to the main screen (Toolbar, TabLayout).
    - **`textEditor/`**: The code editor implementation.
        - `TextEditorManager`: Orchestrates file loading, saving, and editor configuration.
        - `ui/`: The editor view using Sora-Editor.
    - **`viewer/`**: Dedicated viewers for different media types.
        - `audio/`, `image/`, `pdf/`, `video/`, `text/`: Specific implementations for each viewer type.
    - **`preferences/`**: Datastore-backed settings management.
- **`theme/`**: Material 3 theme definitions, including dynamic color support.

---

## 🧬 Key Design Patterns

### 1. `ContentHolder` Abstraction
To support different "file systems" (Local files, Zip contents, Virtual folders), the project uses the `ContentHolder` abstract class.
- `LocalFileHolder`: Wraps `java.io.File`.
- `ZipFileHolder`: Represents a file or folder inside a ZIP archive.
- `VirtualFileHolder`: Used for special views like "Search Results" or "Recent Files".

### 2. Manager Pattern
Functionality is grouped into "Managers" (e.g., `ZipManager`, `TaskManager`, `SearchManager`). These are typically singletons accessed via the `App` instance. This keeps the Activities and Composables focused on UI while the Managers handle long-running tasks and complex logic.

### 3. Multi-Tabbed Navigation
The app uses a `HorizontalPager` where each page is a `Tab`. This allows for a browser-like experience where state is preserved across different directories.

---

## 🚦 Navigation & Lifecycle

- **Back Press**: Handled hierarchically. A `Tab` can consume the back press (e.g., to go up a directory). If not consumed, `MainActivityManager` may close the current tab or eventually the app.
- **Persistence**: The app can remember the last session's tabs and restore them on startup.

---

## 📝 Developer's Intent (Mental Model)

When working on this codebase, remember:
1. **Compose First**: UI should be declarative. Avoid manual view manipulation.
2. **Asynchronous Operations**: File operations should never block the UI thread. Use `CoroutineScope(Dispatchers.IO)`.
3. **Safety**: Always validate if a file still exists before performing operations on it.
4. **Performance**: Thumbnails and directory listings should be optimized. Use Coil for caching and efficient image loading.
5. **Consistency**: Use the established `FileExplorerTheme` and common UI components to maintain the Material 3 aesthetic.

---

## 🛠️ How to Extend

- **Adding a new Tab type**: Inherit from `Tab` and add a new case in `MainActivity.TabsPager`.
- **Adding a new Viewer**: Create a new sub-package in `screen.viewer` and register it in `ViewersManager`.
- **Adding a new File Operation**: Implement it in `FilesTab` or a dedicated Manager if it's complex.

---

*This guide was generated to help AI agents navigate and understand the Prism File Explorer project. Happy coding!*
