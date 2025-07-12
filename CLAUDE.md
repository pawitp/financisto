# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Financisto is an Android personal finance tracker written in Java using traditional Android patterns. The codebase is approximately 15 years old and uses some deprecated patterns but remains functional.

## Common Development Commands

### Building and Testing
```bash
# Build the app
./gradlew build

# Run unit tests
./gradlew test

# Run connected tests (requires device/emulator)
./gradlew connectedAndroidTest

# Clean build
./gradlew clean

# Install debug APK to connected device
./gradlew installDebug
```

### Code Quality
- No specific linting configured beyond Android defaults
- Tests are in `app/src/test/java/` for unit tests and use JUnit 4 with Robolectric

## Architecture Overview

### Core Structure
- **Main Package**: `ru.orangesoftware.financisto`
- **Activities**: Traditional Activity-based UI in `activity/` package
- **Models**: Plain Java objects with JPA-style annotations in `model/` package
- **Database**: Custom lightweight ORM in `orb/` package with SQLite backend
- **Data Access**: `DatabaseAdapter` class provides main data operations

### Key Components
1. **Custom ORM (`orb/` package)**
   - Uses reflection-based entity mapping similar to JPA
   - Query building with `Query`, `Expression`, `Criteria` classes
   - SQLite backend with schema versioning (current version 217)

2. **Main UI Flow**
   - `MainActivity` with 5 tabs: Accounts, Blotter, Budgets, Reports, Menu
   - Uses deprecated `TabActivity` pattern
   - List-based interfaces for entity management

3. **Dependency Injection**
   - Uses AndroidAnnotations framework (@EBean, @EApplication)
   - EventBus for component communication

### Financial Features
- Multi-currency support with exchange rates
- Split transactions and transfers
- Budgeting with period tracking
- QIF/CSV import/export
- Cloud backup (Dropbox, Google Drive)
- SMS transaction parsing
- Recurring transactions

## Important Files

### Configuration
- `app/build.gradle` - Main Android build configuration
- `app/src/main/AndroidManifest.xml` - App manifest with permissions and components
- `app/proguard-rules.pro` - ProGuard configuration for release builds

### Core Classes
- `FinancistoApp.java` - Main application class with DI setup
- `MainActivity.java` - Main tabbed interface
- `DatabaseAdapter.java` - Primary data access layer  
- `DatabaseHelper.java` - SQLite schema and migrations
- `EntityManager.java` - Custom ORM core functionality

### Model Classes
Key entities in `model/` package:
- `Transaction` - Core financial transaction
- `Account` - Financial account (bank, cash, etc.)
- `Category` - Transaction categorization with hierarchy
- `Currency` - Multi-currency support
- `Budget` - Budget tracking

## Development Notes

### Database Migrations
- Schema versions managed in `DatabaseHelper.java`
- Current schema version: 217
- Migration scripts handle upgrades from version to version

### Testing Strategy
- Unit tests use JUnit 4 with Robolectric for Android components
- Test builders available for entities (`AccountBuilder`, `TransactionBuilder`)
- Database tests verify CRUD operations and calculations

### Key Dependencies
- AndroidAnnotations 4.6.0 for DI and code generation
- GreenRobot EventBus for component communication
- Various Google Play Services for cloud integration
- Robolectric for Android unit testing

### Legacy Considerations
- Uses deprecated `TabActivity` pattern (consider migrating to ViewPager/Fragments)
- Targets API level 28 (Android 9)
- Uses older Android support libraries (some migration to AndroidX started)