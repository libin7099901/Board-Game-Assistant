# Project Documentation: Tabletop Companion

This document provides an overview of the Tabletop Companion Android project, including its structure, build process, and high-level architecture.

## 1. Code Documentation (KDoc/Dokka)

An analysis of the Kotlin source code (`.kt` files) revealed that while there are inline comments clarifying implementation details within functions and classes, there is a general lack of formal KDoc block comments (`/** ... */`) for classes, methods, and properties.

The Dokka plugin (for generating KDoc documentation) is not currently configured in the project's Gradle build scripts (`build.gradle` or `app/build.gradle`). Therefore, automated generation of detailed KDoc/HTML documentation was not performed.

Developers looking for code-level details should refer to the inline comments directly within the source code files located primarily under `app/src/main/java/com/example/tabletopcompanion/`.

## 2. Project Structure

The project is a standard Android application built using Gradle.

### Root Directory:

*   **`.gradle/`**: Gradle build system cache, history, and generated files. Not part of the application's source code.
*   **`app/`**: The main application module containing the app's code, resources, and module-specific build configurations.
*   **`build.gradle`**: Top-level Gradle build file. Configures project-wide build settings and dependencies common to all modules.
*   **`settings.gradle`**: Defines which modules are included in the build (e.g., includes the `:app` module).
*   **`gradlew`**, **`gradlew.bat`**: Gradle wrapper scripts for building the project on Unix-like (Linux/macOS) and Windows systems, respectively. These scripts ensure a consistent Gradle version is used.
*   **`gradle/wrapper/`**: Contains the Gradle wrapper JAR file (`gradle-wrapper.jar`) and its properties (`gradle-wrapper.properties`), defining the Gradle version and distribution.
*   **`doc/`**: Contains project-related documentation.
    *   `产品需求文档 - 桌游伴侣.md`: Product Requirements Document for Tabletop Companion (in Chinese).

### `app/` Module Directory:

*   **`app/build.gradle`**: Module-level Gradle build file for the `app` module. It specifies:
    *   Plugins (e.g., `com.android.application`, `org.jetbrains.kotlin.android`).
    *   Android specific configurations (`compileSdk`, `minSdk`, `targetSdk`, `defaultConfig`, `buildTypes`).
    *   Dependencies for the application.
    *   Build features (e.g., Jetpack Compose integration).
*   **`app/src/`**: Contains all source code and resources for the application.
    *   **`app/src/main/`**: The main source set.
        *   **`AndroidManifest.xml`**: The core manifest file. Declares application components (activities, services, broadcast receivers, content providers), permissions, hardware/software features, and other essential information for the Android system.
        *   **`java/`**: Contains the Kotlin source code files.
            *   `com/example/tabletopcompanion/`: The root package for the application's Kotlin code.
                *   `MainActivity.kt`: The main entry point Activity for the application.
                *   `data/`: Contains classes related to data management:
                    *   Repositories (e.g., `TemplateRepository.kt`, `UserProfileRepository.kt`) that abstract data sources.
                    *   `database/`: Room database components (`AppDatabase.kt`, Data Access Objects like `GameTemplateMetadataDao.kt`, and Entities like `GameTemplateMetadataEntity.kt`).
                    *   `model/`: Kotlin data classes representing the application's data structures (e.g., `GameTemplateMetadata.kt`, `Player.kt`, `Room.kt`, `UserProfile.kt`, and nested template structures).
                    *   `network/ollama/`: Components for interacting with an Ollama (AI) service.
                *   `features/`: Contains UI (Screens) and ViewModel logic for different features of the application (e.g., `roommanagement/`, `templatemanagement/`, `userprofile/`).
                *   `gamecore/`: Contains core game logic, exemplified by `GameEngine.kt`.
                *   `network/`: Contains classes for network communication, such as `NsdHelper.kt` (Network Service Discovery) and `P2PCommunicationManager.kt` (Peer-to-Peer communication), along with network-specific data models in `network/model/`.
                *   `ui/`: Contains UI-related elements, including:
                    *   Jetpack Compose screens (e.g., `MainScreen.kt`).
                    *   `theme/`: Defines the application's visual theme (e.g., `Color.kt`, `Shapes.kt`, `Theme.kt`, `Typography.kt`).
                *   `util/`: Utility classes and functions (e.g., `Result.kt`).
        *   **`res/`**: (Standard Android directory) Contains application resources such as:
            *   `drawable/`: Image files.
            *   `layout/`: (Potentially, though Compose is primary) XML layout files.
            *   `mipmap/`: Launcher icons.
            *   `values/`: XML files for strings, dimensions, colors, styles, etc.
    *   **`app/src/test/`**: Source set for local unit tests (e.g., JUnit tests for ViewModels, repositories, utilities).
    *   **`app/src/androidTest/`**: (Standard Android directory) Source set for instrumented tests that run on an Android device or emulator (e.g., UI tests with Espresso or Compose testing utilities).

## 3. Build Process

### Build Tool
The project uses **Gradle** as its build automation system. Gradle handles dependency management, compilation, testing, and packaging of the application (e.g., into an APK).

### Gradle Wrapper
It is highly recommended to use the provided Gradle wrapper scripts to build the project:
*   `./gradlew` on Linux/macOS
*   `gradlew.bat` on Windows

These scripts ensure that the correct Gradle version (defined in `gradle/wrapper/gradle-wrapper.properties`) is used, without requiring a manual Gradle installation.
On Linux/macOS, you might need to make the script executable first: `chmod +x gradlew`.

### Common Gradle Tasks

Here are some common Gradle tasks you might use (run from the project root directory):

*   **Clean the project:**
    ```bash
    ./gradlew clean
    ```
    This removes build artifacts from previous builds.

*   **Build a Debug APK:**
    ```bash
    ./gradlew assembleDebug
    ```
    If successful, the APK will typically be found in `app/build/outputs/apk/debug/app-debug.apk`.

*   **Run Unit Tests:**
    ```bash
    ./gradlew testDebugUnitTest
    ```
    Test results are typically generated in `app/build/reports/tests/testDebugUnitTest/`.

*   **List all available tasks:**
    ```bash
    ./gradlew tasks
    ```

### **IMPORTANT: Environment Issues Encountered**

During attempts to execute Gradle tasks (including `assembleDebug` and even basic tasks like `tasks`), **persistent "Internal error occurred when running command" errors were encountered.**

These issues persisted despite:
*   Ensuring `ANDROID_HOME` was correctly configured.
*   Using various Gradle flags to aid resilience (e.g., `--no-daemon`, adjusting JVM memory, limiting worker threads).
*   Performing clean builds.

**This strongly suggests an underlying problem with the execution environment's stability, resource availability, or compatibility with Gradle for this specific project.** Successfully running Gradle tasks may require investigation and remediation of the execution environment by someone with direct access to it.

## 4. High-Level Architecture

The project appears to follow the **MVVM (Model-View-ViewModel)** architectural pattern, promoting a separation of concerns.

*   **View (UI Layer):**
    *   Built using **Jetpack Compose**.
    *   `MainActivity.kt` serves as the primary entry point.
    *   Composable functions within the `features` packages (e.g., `CreateRoomScreen.kt`, `RoomScreen.kt`) define the UI for different application screens.
    *   The `ui/theme/` package manages the application's visual styling.

*   **ViewModel Layer:**
    *   ViewModels (e.g., `RoomViewModel.kt`, `TemplateViewModel.kt`) act as intermediaries between the UI (View) and the data layer (Model).
    *   They are responsible for:
        *   Holding and managing UI-related state (often using `StateFlow` or `LiveData`).
        *   Fetching data from repositories and preparing it for display.
        *   Handling user interactions from the UI and delegating actions to the data layer or core logic.
    *   `RoomViewModelFactory.kt` suggests custom instantiation for ViewModels, likely for dependency injection.

*   **Model (Data Layer & Core Logic):**
    *   **Repositories:** The Repository pattern (e.g., `TemplateRepository.kt`, `UserProfileRepository.kt`) is used to abstract data sources. Repositories manage data operations and provide a clean API for ViewModels to access data.
    *   **Local Database:**
        *   **Room Persistence Library** is used for local data storage.
        *   Components include `AppDatabase.kt` (the database class), Data Access Objects (DAOs like `GameTemplateMetadataDao.kt`), and Entities (`GameTemplateMetadataEntity.kt`).
    *   **Network Communication:**
        *   **Peer-to-Peer (P2P) & Network Service Discovery (NSD):** `P2PCommunicationManager.kt` and `NsdHelper.kt` handle local network interactions, likely for multiplayer or device-to-device features. Associated data models are in `network/model/`.
        *   **AI Service Integration:** `OllamaService` (seen as a dependency in `RoomViewModel` and in package `data/network/ollama/`) suggests integration with the Ollama AI service.
    *   **Game Logic:**
        *   `gamecore/GameEngine.kt` appears to encapsulate the core rules, state management, and turn progression for the tabletop games based on loaded templates.
    *   **Data Models:** Kotlin data classes in `data/model/` define the structure of application data (e.g., `GameTemplateMetadata`, `Player`, `Room`, `GameTemplate`).

*   **Utilities:**
    *   The `util/` package contains helper classes, such as `Result.kt` for handling operations that can succeed or fail.

**Inferred Data Flow:**
UI (Compose Screens) -> ViewModels -> Repositories -> (Database / Network Services / GameEngine)
```
