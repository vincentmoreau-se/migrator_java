# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SIM Password Wallet is a dual-module Android project that stores encrypted passwords using JavaCard applet technology on SIM cards. The project consists of:

- **app**: Android application (GPL v3) that provides the UI and communicates with the SIM card via Open Mobile API
- **applet**: JavaCard applet (MIT) that runs on the SIM card and handles AES-128 CBC encryption/decryption

The system supports two storage modes that can be switched dynamically:
- App storage: Passwords stored in SQLite database (encrypted/decrypted on SIM)
- SIM storage: Passwords stored directly in SIM card EEPROM (encrypted)

## Build Commands

Build the entire project:
```bash
./gradlew clean build
```

Build individual modules:
```bash
./gradlew :app:build      # Android app
./gradlew :applet:build   # JavaCard applet
```

## Testing

Run applet tests (JavaCard simulator):
```bash
./gradlew :applet:test
```

The applet tests use a custom test suite pattern - only `TestSuite.class` is included to avoid running individual test classes twice. All tests are orchestrated through `TestSuite.java`.

Test mode can be specified:
```bash
./gradlew :applet:test -DtestMode=simulator
```

## Coverage Reports

Generate combined coverage report:
```bash
./gradlew jacocoRootReport
```

Upload coverage to Coveralls:
```bash
./gradlew coveralls
```

## Architecture

### Communication Flow

The app communicates with the SIM card applet using APDU (Application Protocol Data Unit) commands:

1. **Android App Layer**: Activities/Fragments interact with the UICC abstraction
2. **UICC Layer** (`app/src/main/java/.../uicc/Uicc.java`): Constructs APDU commands, manages Open Mobile API channel
3. **JavaCard Applet** (`applet/src/main/java/.../PasswordWalletApplet.java`): Processes APDU commands, handles encryption/decryption

### Key APDU Instructions

- `0x10` - Encrypt data
- `0x11` - Decrypt data
- `0x20` - Verify PIN
- `0x24` - Change PIN
- `0x30` - Add password entry
- `0x32` - Retrieve password
- `0x33` - Edit password
- `0x34` - Delete password
- `0x36` - List password identifiers
- `0x40` - Get storage mode
- `0x41` - Set storage mode
- `0x50` - Get card state
- `0x51` - PIN check

### PIN Security

The applet implements PIN-based security from the GlobalPlatform API:
- PIN tries limit: 3 attempts
- PIN max size: 16 bytes
- Card states: `APPLICATION_SELECTABLE` (initial), `CARD_SECURED` (PIN set)

### Encryption

All encryption/decryption occurs on the SIM card using AES-128 CBC with ISO 7816-4 padding (0x80 followed by 0x00 bytes). The AES key is generated randomly when the applet is installed and never leaves the SIM card.

### Storage Modes

When switching modes, password data is migrated:
- App → SIM: Encrypted passwords moved from SQLite to applet's EEPROM
- SIM → App: Encrypted passwords moved from applet's EEPROM to SQLite

In both cases, encryption/decryption always happens on the secure element.

### Database

The Android app uses `PasswordReaderDbHelper` for SQLite operations when in app storage mode. Passwords are stored encrypted even in the database.

## JavaCard Development

The applet uses the JavaCard Gradle plugin with configuration in `applet/build.gradle`:
- JavaCard SDK path: `../oracle_javacard_sdks/jc221_kit`
- Package AID: `D2:76:00:01:18:00:02:FF:49:50:25:89:C0:01:00:00`
- Applet AID: `D2:76:00:01:18:00:02:FF:49:50:25:89:C0:01:9B:01`

The applet CAP file is output to `applet/build/javacard/applet.cap`.

### Test Scripts

The applet build includes predefined APDU scripts for manual testing:
```bash
./gradlew sendEncryptEmpty
./gradlew sendEncrypt16
./gradlew sendEncrypt32
./gradlew getMode
```

## Android SDK Version

- Compile SDK: 26
- Build tools: 26.0.1
- Min SDK: 17
- Target SDK: 26

## Dependencies

Key external dependencies:
- `org.simalliance.openmobileapi` (provided): Open Mobile API for SIM communication
- `fr.bmartel:lollipin`: PIN code UI library
- `fr.bmartel:gplatform`: GlobalPlatform support for JavaCard

## Testing with Emulator

To test with PC/SC emulator support, see: https://github.com/bertrandmartel/pcsc-android-emulator

## Code Organization

- **Adapters**: RecyclerView adapters for password list (`PasswordAdapter.java`)
- **Fragments**: Main UI fragments inherit from `MainFragmentAbstr` and `ListFragmentAbstr`
- **UICC package**: All SIM card communication logic (`CommandApdu`, `ApduResponse`, `Uicc`, `UiccUtils`)
- **Database**: SQLite helper for local storage mode
- **Models**: `Password.java` represents password entries (exists in both app and applet test code)
- **Application class**: `PasswordApplication` manages global state and SEService connection
