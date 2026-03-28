# AGENTS — Guidelines for AI Coding Agents

This file is the authoritative reference for any AI agent (Copilot, Claude, Cursor, etc.) working
on this repository. Read it entirely before writing or modifying any code.

---

## 1. Project Purpose

Build a **Java library** that lets applications discover, connect to, and control BLE smart-brick
controllers from LEGO (Powered Up) and compatible third-party brands:

- LEGO Powered Up (Hub 2, Technic Hub, Move Hub, City Hub, Mario Hub)
- SBrick / SBrick Plus
- FX Bricks (BrickPi3, PiStorms in BLE mode)
- Circuit Cubes (Bluetooth Cube)
- BuWizz 2.0 and BuWizz 3.0

The library exposes a single, brand-agnostic public API. Platform differences are hidden inside
internal adapter implementations.

---

## 2. Non-Negotiable Constraints

| Constraint | Value |
|---|---|
| Language | Java 25 — use the latest language features where they improve clarity |
| Build tool | Maven (no Gradle, no Bakelite, no shell scripts as primary build) |
| Minimum Maven version | 3.9 |
| Artifact coordinates | `ch.varani.bricks:ble-api` |
| Target platforms | macOS 13+, Windows 11, Linux (kernel 5.10+, BlueZ 5.50+) |
| Target architectures | amd64, aarch64 (for every supported OS) |

---

## 3. Module and Package Structure

```
src/main/java/ch/varani/lego/ble/
  api/              # Public interfaces and immutable value types only
  impl/
    macos/          # CoreBluetooth bridge (JNI or FFM)
    windows/        # WinRT Bluetooth LE bridge (JNI or FFM)
    linux/          # D-Bus / BlueZ bridge (JNI or FFM)
  device/
    lego/           # LEGO Wireless Protocol 3.0
    sbrick/         # SBrick BLE protocol
    fxbricks/       # FX Bricks BLE protocol
    circuitcubes/   # Circuit Cubes BLE protocol
    buwizz/         # BuWizz 2.0 and BuWizz 3.0 BLE protocol
  util/             # Internal helpers — never exposed in public API
```

Native source layout (compiled and bundled as resources):

```
src/main/native/
  macos/            # Objective-C / Swift calling CoreBluetooth
                    # CMake builds a universal binary (arm64 + x86_64)
  windows/
    amd64/          # C++ calling WinRT Bluetooth LE APIs — x86-64
    aarch64/        # C++ calling WinRT Bluetooth LE APIs — ARM64
  linux/
    amd64/          # C calling D-Bus / BlueZ — x86-64
    aarch64/        # C calling D-Bus / BlueZ — AArch64 (Raspberry Pi, etc.)
```

Rules:
- **Only** the `api` package is part of the public API. All other packages are internal.
- Never add `public` visibility to types in `impl`, `device`, or `util` unless they implement a
  type from `api`.
- Never let `api` types depend on `impl`, `device`, or `util`.
- Every class must belong to one of the packages above. Do not create ad-hoc packages.

---

## 3a. Native Bluetooth Implementation Mandate

**The Bluetooth LE transport layer must use exclusively the native APIs provided by each
operating system.** No third-party Java BLE library (e.g. tinyb, bluecove, nordic-ble) may be
used as the underlying transport. The rationale is reliability, long-term maintainability, and
full access to platform capabilities without an intermediary abstraction.

### Required native stacks per platform

| Platform | Required OS API | Language |
|---|---|---|
| macOS (aarch64 / amd64) | **CoreBluetooth** framework (`IOBluetooth.framework` is legacy — do not use) | Objective-C or Swift, bridged to Java via JNI or the Foreign Function & Memory API (FFM) |
| Windows 11 (amd64 / aarch64) | **Windows Runtime (WinRT) Bluetooth LE API** (`Windows.Devices.Bluetooth`) | C++ with WinRT projections, bridged to Java via JNI or FFM |
| Linux — kernel 5.10+, BlueZ 5.50+ (amd64 / aarch64) | **BlueZ** over D-Bus (`org.bluez` interfaces) | C using libdbus or sd-bus, bridged to Java via JNI or FFM |

### Source organisation rules

- Native source code lives under `src/main/native/<os>/<arch>/`. Do not place native sources
  anywhere else.
- Each `<os>/<arch>` directory is a self-contained compilation unit (its own `CMakeLists.txt` or
  equivalent) and produces a single shared library (`.dylib`, `.dll`, or `.so`).
- The compiled shared libraries are bundled inside the JAR under
  `natives/<os>/<arch>/` so that the Java runtime can extract and load them with
  `System.load()` or `MemorySegment`/`Linker` (FFM).
- Architecture-specific code paths are **not** optional. If a platform supports both amd64 and
  aarch64 (e.g. macOS with Apple Silicon and Intel, Linux on Raspberry Pi), both architecture
  directories must exist and be built.
- Falling back to a pure-Java or third-party BLE library when the native library fails to load
  is **forbidden**. Throw a descriptive `BleException` instead.
- Every JNI / FFM binding method must be documented with a Javadoc comment that names the
  underlying OS function or D-Bus interface it calls.

---

## 4. Java Coding Standards

### 4.1 Language of Comments and Documentation

All code comments, Javadoc, commit messages, pull request descriptions, and any other written
artefact in this repository **must be written in English**. No other language is permitted.

### 4.2 General

- Follow the conventions enforced by `varani_java_checks.xml` (Checkstyle). When in doubt, defer
  to the Checkstyle rules — they win over personal style preferences.
- Every `public` or `protected` type, method, and field **must** have a Javadoc comment.
- No `@SuppressWarnings` annotation without a comment explaining why it is necessary.
- No `TODO` or `FIXME` comments in committed code — open an issue instead.
- Prefer immutable value types (`record`, `final` classes with no setters) in the `api` package.
- Prefer `sealed` hierarchies when modelling closed sets of variants (e.g. device types,
  connection states).
- Use `var` only when the inferred type is unambiguous from the right-hand side.

### 4.3 Null Safety

- Never return `null` from a public API method. Return `Optional<T>` or throw a documented
  exception instead.
- Annotate parameters that must not be null with `@NonNull` (from `org.jspecify`).
- Annotate parameters that may be null with `@Nullable` (from `org.jspecify`).

### 4.4 Concurrency

- BLE operations are inherently asynchronous. Expose them as `CompletableFuture<T>` or reactive
  types, never as blocking calls in the public API.
- All mutable shared state must be protected. Prefer `java.util.concurrent` primitives over raw
  `synchronized` blocks.
- Document thread-safety guarantees (or lack thereof) in Javadoc.

### 4.5 Resource Management

- Every resource that implements `AutoCloseable` must be used in a `try-with-resources` block or
  have its lifecycle clearly documented.
- Native handles (JNI / FFM) must be released in a `close()` method and registered with a
  `Cleaner` as a fallback.

---

## 5. Testing

### 5.1 Coverage Target

**≥ 99 % instruction coverage and 100 % branch coverage** as reported by JaCoCo. The build fails
if these thresholds are not met.

The instruction threshold is 0.99 (not 1.00) because of one approved exception:
`NativeLibraryLoader.loadAbsolutePath()` contains a `System.load()` call whose post-call bytecode
probe is never reached when the JVM throws `UnsatisfiedLinkError` — a JVM-level restriction in
Java 25 restricted mode. JaCoCo cannot instrument past that point. The threshold was lowered with
a detailed comment in `pom.xml`. Branch coverage remains at 1.00 because all branches are covered.

If a code path is genuinely unreachable (e.g. a defensive `default` in a `switch` over a `sealed`
hierarchy), document why in a comment and, if JaCoCo still counts it, use a JaCoCo `@Generated`
annotation or an exclusion entry in `pom.xml`, but only after a reviewer approves.

### 5.2 Test Organisation

- Unit tests: `src/test/java/…` — one test class per production class, same package.
- Integration tests (real BLE hardware required): `src/it/java/…` — use the Maven Failsafe plugin
  and name classes `*IT.java`.
- Never mix unit and integration tests in the same class.

### 5.3 Test Quality Rules

- Use **JUnit 5 (Jupiter)** exclusively. No JUnit 4 imports. Current version: 5.12.2.
- Use **Mockito** for mocking. No PowerMock.
- For assertions on asynchronous behaviour, use **Awaitility**. No `Thread.sleep` in tests.
- Every test method must have a single logical assertion (or a grouped `assertAll`).
- Test names must follow the pattern `methodUnderTest_scenario_expectedBehaviour`.
- Use `@Timeout` or mock the clock for time-sensitive tests.

---

## 6. Static Analysis and Security

### 6.1 Checkstyle

Config file: `varani_java_checks.xml` (at repository root).
The build fails on any Checkstyle violation.
Never disable a rule inline without approval and a comment.

### 6.2 SonarQube

Static analysis, security scanning, and dependency vulnerability checks are performed exclusively
through the project's **private SonarQube instance**. SpotBugs and OWASP Dependency-Check are
**not** run as part of the Maven build.

**A task must not be considered complete until the SonarQube Quality Gate is Passed.**

Quality Gate criteria enforced in CI:
- Zero Blocker issues.
- Zero Critical issues.
- Maintainability rating ≥ A.
- Reliability rating ≥ A.
- Security rating ≥ A.
- Duplicated lines density < 3 %.

Do not introduce code that would lower these ratings.

---

## 7. pom.xml Conventions

- All dependency versions must be declared in `<dependencyManagement>`.
- All plugin versions must be declared in `<pluginManagement>`.
- No `LATEST`, `RELEASE`, or unversioned references.
- No SNAPSHOT dependencies except for this project's own modules.
- Keep the Gluon/GluonHQ repository reference only if it is actually used; otherwise remove it.
- The `<repositories>` block must use `https` URLs — never `http`.
- Run `mvn versions:display-dependency-updates` periodically and update dependencies.

---

## 8. Git and CI

- Branch names: `feature/<short-description>`, `fix/<short-description>`,
  `chore/<short-description>`.
- Commit messages: Conventional Commits format (`feat:`, `fix:`, `chore:`, `docs:`, `test:`, …).
- Every commit on `main` must pass `mvn verify` with all quality gates green.
- Pull requests must include:
  - Updated or new tests covering every changed line.
  - Updated Javadoc for every changed public API element.
  - No decrease in JaCoCo coverage.

---

## 9. What Agents Must NOT Do

- Do not add dependencies without checking for known CVEs first.
- Do not use deprecated Java APIs (`sun.*`, `com.sun.*`, `java.util.Date`, etc.).
- Do not use `System.out` or `System.err` in library code — use `java.util.logging` or SLF4J.
- Do not hard-code BLE UUIDs or device names as magic strings — define them as named constants.
- Do not commit auto-generated files (IDE project files, `target/`, `*.class`).
- Do not modify `varani_java_checks.xml` or `versions_rules.xml` to silence violations.
- Do not lower coverage thresholds in `pom.xml` to make the build pass. The one approved
  exception is `jacoco.min.instruction.ratio=0.99` (down from `1.00`) for
  `NativeLibraryLoader.loadAbsolutePath()`, which contains a `System.load()` call that JaCoCo
  cannot instrument past in Java 25 restricted mode. This exception is documented in `pom.xml`.
- Do not use `http` repository URLs in `pom.xml`.
- Do not modify the public API (`api/`) or the DSL (`api/dsl/`) without updating `README.md`
  accordingly. The usage examples in `README.md` must always reflect the actual public surface.

---

## 10. Definition of Done

A task is complete when **all** of the following are true:

1. `mvn verify` passes with zero errors and zero warnings on all three target platforms.
2. JaCoCo reports ≥ 99 % instruction coverage and 100 % branch coverage.
3. Checkstyle reports zero violations.
4. SonarQube Quality Gate is Passed.
5. All public API elements have complete Javadoc.
6. The PR description explains the change and references the relevant issue.
7. `README.md` reflects every change to the public API (`api/`) and the DSL (`api/dsl/`):
   new methods are documented, removed methods are removed from examples, and changed
   signatures are corrected.

---

## 11. Protocol References

The sections below document the BLE protocols for every supported brand. All UUIDs and magic
constants must be defined as named constants in the appropriate package — never as inline literals.

---

## 12. LEGO Wireless Protocol 3.0 Reference

Official specification: https://lego.github.io/lego-ble-wireless-protocol-docs/

All constants must live in `ch.varani.bricks.ble.device.lego`.

### 12.1 Discovery

- Manufacturer ID: `0x0397` (LEGO System A/S)
- Manufacturer data format (10 bytes): Length, Data Type (`0xFF`), Manufacturer ID (uint16),
  Button State (uint8), System Type + Device Number (uint8), Device Capabilities (uint8),
  Last Network ID (uint8), Status (uint8), Option (uint8).

#### System Type and Device Number encoding (`SSS DDDDD`)

| `SSS` | `DDDDD` | Device |
|---|---|---|
| `000` | `00000` | WeDo 2.0 Hub |
| `001` | `00000` | Duplo Train |
| `010` | `00000` | Boost Hub (Move Hub) |
| `010` | `00001` | 2-Port Hub |
| `010` | `00010` | 2-Port Handset |

### 12.2 GATT Service and Characteristic

| Role | UUID |
|---|---|
| LEGO Hub Service | `00001623-1212-EFDE-1623-785FEABCD123` |
| LEGO Hub Characteristic | `00001624-1212-EFDE-1623-785FEABCD123` |

The Hub Characteristic supports **Write Without Response** (or Write With Response on iOS) and
**Notify**. All communication flows through this single characteristic using a common message
header.

### 12.3 Common Message Header

Every upstream and downstream message starts with a 3-byte header:

| Field | Size | Description |
|---|---|---|
| Length | 1–2 bytes | Total message length. Bit 7 set = 2-byte encoding (7-bit LSB + MSB). |
| Hub ID | 1 byte | Always `0x00` (reserved for future hub networks). |
| Message Type | 1 byte | Identifies the message type (see below). |

### 12.4 Message Types

| Value | Name | Direction |
|---|---|---|
| `0x01` | Hub Properties | Down + Up |
| `0x02` | Hub Actions | Down + Up |
| `0x03` | Hub Alerts | Down + Up |
| `0x04` | Hub Attached I/O | Up only |
| `0x05` | Generic Error | Up only |
| `0x08` | H/W Network Commands | Down + Up |
| `0x10` | F/W Update — Go Into Boot Mode | Down only |
| `0x11` | F/W Update Lock Memory | Down only |
| `0x12` | F/W Update Lock Status Request | Down only |
| `0x13` | F/W Lock Status | Up only |
| `0x21` | Port Information Request | Down only |
| `0x22` | Port Mode Information Request | Down only |
| `0x41` | Port Input Format Setup (Single) | Down only |
| `0x42` | Port Input Format Setup (CombinedMode) | Down only |
| `0x43` | Port Information | Up only |
| `0x44` | Port Mode Information | Up only |
| `0x45` | Port Value (Single) | Up only |
| `0x46` | Port Value (CombinedMode) | Up only |
| `0x47` | Port Input Format (Single) | Up only |
| `0x48` | Port Input Format (CombinedMode) | Up only |
| `0x61` | Virtual Port Setup | Down only |
| `0x81` | Port Output Command | Down only |
| `0x82` | Port Output Command Feedback | Up only |

### 12.5 Key Hub Properties (message type `0x01`)

| Property ref | Name | Operations |
|---|---|---|
| `0x01` | Advertising Name | Set, Enable/Disable Update, Reset, Request Update, Update |
| `0x02` | Button | Enable/Disable Update, Request Update, Update |
| `0x03` | FW Version | Request Update, Update |
| `0x04` | HW Version | Request Update, Update |
| `0x05` | RSSI | Enable/Disable Update, Request Update, Update |
| `0x06` | Battery Voltage (%) | Enable/Disable Update, Request Update, Update |
| `0x07` | Battery Type | Request Update, Update |
| `0x0B` | System Type ID | Request Update, Update |
| `0x0D` | Primary MAC Address | Request Update, Update |

Property Operation codes: `0x01`=Set, `0x02`=Enable Updates, `0x03`=Disable Updates,
`0x04`=Reset, `0x05`=Request Update, `0x06`=Update (upstream).

### 12.6 Hub Action Types (message type `0x02`)

| Value | Description |
|---|---|
| `0x01` | Switch Off Hub |
| `0x02` | Disconnect |
| `0x03` | VCC Port Control On |
| `0x04` | VCC Port Control Off |
| `0x30` | Hub Will Switch Off (upstream) |
| `0x31` | Hub Will Disconnect (upstream) |
| `0x32` | Hub Will Go Into Boot Mode (upstream) |

### 12.7 Hub Alert Types (message type `0x03`)

| Value | Description |
|---|---|
| `0x01` | Low Voltage |
| `0x02` | High Current |
| `0x03` | Low Signal Strength |
| `0x04` | Over Power Condition |

Alert Operations: `0x01`=Enable Updates, `0x02`=Disable Updates, `0x03`=Request Update,
`0x04`=Update (upstream, with 1-byte boolean payload: `0x00`=OK, `0x01`=Alert).

### 12.8 Port Output Command (message type `0x81`)

Used to control motors and other output devices:

| Field | Size | Description |
|---|---|---|
| Port ID | 1 byte | Identifies the port |
| Startup/Completion | 1 byte | Bits 7–4: Startup info; Bits 3–0: Completion info |
| Sub-command | 1 byte | Motor sub-command (`0x01`–`0x3F`) |
| Parameters | variable | Sub-command specific |

Key motor sub-commands:
- `0x01` — SetAccTime (acceleration time)
- `0x02` — SetDecTime (deceleration time)
- `0x07` — StartSpeed (power, max speed, end state, profile)
- `0x08` — StartSpeed2 (two synchronized motors)
- `0x09` — StartSpeedForTime (duration-limited)
- `0x0A` — StartSpeedForTime2
- `0x0B` — StartSpeedForDegrees (angle-limited)
- `0x0C` — StartSpeedForDegrees2
- `0x0D` — GotoAbsolutePosition
- `0x0E` — GotoAbsolutePosition2
- `0x50` — WriteDirect
- `0x51` — WriteDirectModeData

### 12.9 Version Number Encoding

FW/HW versions use 4-byte signed little-endian BCD: `0MMM mmmm BBBB BBBB bbbb bbbb bbbb bbbb`
(Major 4 bits, Minor 4 bits, Bug 8 bits BCD, Build 16 bits BCD).

LWP Protocol Version uses 2-byte BCD: `MMMM MMMM mmmm mmmm`.

---

## 13. SBrick Protocol Reference

Official specification: https://social.sbrick.com/custom/The_SBrick_BLE_Protocol.pdf (Rev. 26, 2020-10-28)

All constants must live in `ch.varani.bricks.ble.device.sbrick`.

### 13.1 Discovery

- Manufacturer-specific AD data: company ID `0x0198` (Vengit Limited).
- Data payload is length-prefixed, type-tagged records:

| Record type | Content |
|---|---|
| `0x00` | Product ID, HW major/minor, FW major/minor |
| `0x02` | Device identifier (6-byte string) |
| `0x03` | Security status (`0x00`=open, `0x01`=auth required) |
| `0x05` | Thermal protection status |
| `0x06` | Voltage measurement (12-bit ADC + 4-bit channel) |

### 13.2 GATT Services and Characteristics

| Service | UUID |
|---|---|
| Generic Access | `1800` |
| Generic Attribute | `1801` |
| Device Information | `180a` |
| OTA Firmware Update | `1d14d6ee-fd63-4fa1-bfa4-8f47b42119f0` |
| **Remote Control** | `4dc591b0-857c-41de-b5f1-15abda665b0c` |

| Characteristic | UUID | Notes |
|---|---|---|
| OTA Control | `f7bf3564-fb6d-4e53-88a4-5e37e0326063` | Write `0x03` to reboot into DFU |
| OTA Data | `984227f3-34fc-4045-a5d0-2c581f81a153` | 20- or 55-byte write chunks |
| Quick Drive (deprecated) | *(within Remote Control service)* | Subscribe for notifications only |
| **Remote Control Commands** | `02b8cbcc-0e25-4bda-8790-a15f53e6010f` | All commands written here |

> **Important:** The PDF lists this UUID without a leading zero. The correct UUID is
> `02b8cbcc-0e25-4bda-8790-a15f53e6010f`.

### 13.3 Command Format

```
[CMD_ID: 1 byte] [PARAM_1: 1 byte] [PARAM_2: 1 byte] ...
```

One BLE write-without-response = one command.

### 13.4 Key Commands

#### `0x01` Drive

```
[0x01] [channel] [direction] [power] (repeatable for multiple channels)
```
- `channel`: `0x00`=A, `0x01`=C, `0x02`=B, `0x03`=D
- `direction`: `0x00`=clockwise, `0x01`=counter-clockwise
- `power`: `0x00`–`0xFF` (`0x00`=freewheel, `0xFF`=full)

#### `0x00` Brake

```
[0x00] [channel1] [channel2] ...
```

#### `0x0F` Query ADC

```
[0x0F] [channel: 0x00–0x09]
```
Returns 2 bytes little-endian: bits 15–4 = 12-bit ADC, bits 3–0 = channel.

| Channel | Measurement |
|---|---|
| `0x00`–`0x07` | Port sensor C1/C2 (SBrick Plus only) |
| `0x08` | Battery voltage |
| `0x09` | Internal temperature |

Voltage formula (SBrick/Plus): `V = (ADC × 0.83875) / 127.0`
Temperature formula: `T (°C) = ADC / 0.13461 − 160.0`

#### `0x0D`/`0x0E` Set/Get Watchdog Timeout

```
[0x0D] [timeout: units of 0.1 s]
```
`0x00` disables. Default `0x05` (0.5 s). Recommended period: 0.2–0.5 s.

### 13.5 Quick Drive Speed Encoding (notifications)

Each channel uses 1 byte: bits 7–1 = magnitude (0–127), bit 0 = direction (0=CW, 1=CCW).
Special: `0x00`/`0x01` = brake, `0x02` = zero speed, `0xFE` = full speed.

### 13.6 Command Return Codes

| Code | Meaning |
|---|---|
| `0x00` | Success |
| `0x01` | Invalid data length |
| `0x02` | Invalid parameter |
| `0x03` | No such command |
| `0x05` | Authentication error |
| `0x06` | Authentication needed |
| `0x08` | Thermal protection active |
| `0x09` | Wrong state |

---

## 14. Circuit Cubes Protocol Reference

Reference implementation: https://github.com/made-by-simon/circuit-cubes-python-interface

All constants must live in `ch.varani.bricks.ble.device.circuitcubes`.

### 14.1 Discovery

- Advertised device name: `Tenka`

### 14.2 GATT Services and Characteristics

The proprietary command channel reuses the **Nordic UART Service (NUS)** base UUID:

| Role | UUID |
|---|---|
| NUS Service | `6e400001-b5a3-f393-e0a9-e50e24dcca9e` |
| TX characteristic (write-without-response) | `6e400002-b5a3-f393-e0a9-e50e24dcca9e` |
| RX characteristic (notify) | `6e400003-b5a3-f393-e0a9-e50e24dcca9e` |

Standard services also present: GAP (`1800`), GATT (`1801`), Device Information (`180a`),
TI OAD firmware update (`f000ffc0-0451-4000-b000-000000000000`).

### 14.3 Motor Control Protocol

All commands are written to the **TX** characteristic as UTF-8 ASCII strings.

#### Command format

```
<sign><magnitude:03d><channel_letter>
```

| Field | Values | Notes |
|---|---|---|
| `sign` | `+` or `-` | Direction: `+`=forward, `-`=reverse |
| `magnitude` | `000` or `056`–`255` | 3-digit zero-padded decimal |
| `channel_letter` | `a`, `b`, or `c` | Motor channel (lowercase) |

#### Magnitude encoding

```
if velocity == 0:  magnitude = 0            // stop: "000"
else:              magnitude = 55 + abs(velocity)   // range: 056–255
```

`velocity` internal range: −200 to +200.

| Intent | Wire bytes |
|---|---|
| Stop motor A | `+000a` |
| Full forward motor B | `+255b` |
| Half reverse motor C | `-155c` |
| Minimum forward motor A | `+056a` |

Each motor channel requires a separate write. There is no multi-channel batching.

### 14.4 Battery Read

- Write the single ASCII byte `b` to TX (`6e400002-…`).
- Read the UTF-8 string response from RX (`6e400003-…`) — contains battery voltage.

---

## 15. BuWizz Protocol Reference

All UUIDs and constants below must be defined as named constants in
`ch.varani.bricks.ble.device.buwizz` — never as inline literals.

---

### 11.1 BuWizz 2.0 (API version 1.3)

#### Discovery

- Advertisement device name: `BuWizz`
- Manufacturer data (6 bytes):
  - Bootloader: `05:4E:42:6F:6F:74` (`05:4E:'B':'o':'o':'t'`)
  - Application: `05:4E:42:57:xx:yy` (`05:4E:'B':'W':<fwMajor>:<fwMinor>`)
- Scan response contains the 128-bit Application Service UUID.
- Advertisement interval: 100–250 ms.

#### BLE Services and Characteristics

| Mode        | Service UUID (128-bit, MSB first)                     |
|-------------|-------------------------------------------------------|
| Application | `93:6E:67:B1:19:99:B3:88:81:44:FB:74:00:00:05:4E`    |
| Bootloader  | `0F:DC:A4:95:E6:CD:0E:90:BA:46:98:AC:C1:A4:05:4E`    |

| Mode        | Data Descriptor UUID | Data Descriptor Handle | CCCD Handle |
|-------------|----------------------|------------------------|-------------|
| Application | `0x92D1`             | `0x03`                 | `0x05`      |
| Bootloader  | `0x0001`             | `0x09`                 | `0x09`      |

- Write commands to the data descriptor characteristic.
- Enable notifications by writing `0x0001` to the CCCD.
- Packet size: 1–183 bytes. No application-level CRC (BLE L2CAP guarantees integrity).
- No pairing required.

#### Operation Modes

| Mode                | Description                                                      |
|---------------------|------------------------------------------------------------------|
| Application mode    | Normal operating mode; BLE active, motors controllable.          |
| Bootloader mode     | OTA firmware update; 2-minute idle timeout before auto-restart.  |
| Application sleep   | Low-power; BLE and motors disabled.                              |

#### Application Commands

All commands are sent by writing to the Application data descriptor (`0x92D1`).

---

**`0x00` — Device status report** (notification, ~25 Hz, no command write required)

| Byte   | Field                          | Encoding                                                                 |
|--------|--------------------------------|--------------------------------------------------------------------------|
| 0      | Command                        | `0x00`                                                                   |
| 1      | Status flags (bitmask)         | Bit 6: USB connected; Bit 5: charging; Bits 4–3: battery level (0–3); Bit 0: error |
| 2      | Battery voltage                | `3.00 V + value × 0.01 V` (range 3.00–4.27 V; `0x00`=3.00 V, `0x7F`=4.27 V) |
| 3      | Output (motor) voltage         | `4.00 V + value × 0.05 V` (range 4.00–16.75 V)                          |
| 4–7    | Motor currents (ch 1–4)        | Unsigned 8-bit per channel; `value × 0.033 A` (range 0–8.5 A)           |
| 8      | Current power level            | 0=disabled, 1=Slow, 2=Normal, 3=Fast, 4=LDCRS                           |
| 9      | MCU temperature                | Unsigned 8-bit, value in °C                                              |
| 10–11  | Accelerometer X                | Left-aligned 12-bit signed; 12 mg/digit                                  |
| 12–13  | Accelerometer Y                | Left-aligned 12-bit signed; 12 mg/digit                                  |
| 14–15  | Accelerometer Z                | Left-aligned 12-bit signed; 12 mg/digit                                  |

---

**`0x10` — Set motor data**

| Byte | Field                 | Encoding                                                                              |
|------|-----------------------|---------------------------------------------------------------------------------------|
| 0    | Command               | `0x10`                                                                                |
| 1–4  | Motor speed (ch 1–4)  | Signed 8-bit per channel; `0x81` (−127) = full reverse, `0x00` = stop, `0x7F` (127) = full forward |
| 5    | Brake flags (bitmask) | Bits 3–0, one per motor (LSB = motor 1). 1 = slow-decay (brake); 0 = fast-decay (coast). |

No response generated.

---

**`0x11` — Set power level**

| Byte | Field       | Encoding                                         |
|------|-------------|--------------------------------------------------|
| 0    | Command     | `0x11`                                           |
| 1    | Power level | 0=disabled, 1=Slow, 2=Normal, 3=Fast, 4=LDCRS   |

No response generated. Default level after connect or disconnect: 0 (disabled).

---

**`0x20` — Set current limits**

| Byte | Field                      | Encoding                                                                    |
|------|----------------------------|-----------------------------------------------------------------------------|
| 0    | Command                    | `0x20`                                                                      |
| 1–4  | Current limits (ch 1–4)    | Unsigned 8-bit per channel; steps of 33 mA. Default on connect: 750 mA each. |

No response generated.

---

### 11.2 BuWizz 3.0 (API version 3.22)

#### Discovery

- Advertisement device name: `BuWizz3` (customisable via command `0x20`).
- Manufacturer data (8 bytes):
  - Bootloader: `05:4E:42:57:42:4C:00:00` (`05:4E:'B':'W':'B':'L':00:00`)
  - Application: `05:4E:42:57:xx:yy:<serialLSB>:<serialMSB>` where `xx:yy` = firmware version,
    `serialLSB:serialMSB` = lower 16 bits of device serial number.
- Scan response contains the 128-bit Application Service UUID.
- Advertisement interval: 100 ms. Advertising uses BLE 4.0 compatible mode (not coded/long-range).
- After connection, BuWizz 3.0 requests PHY switch to **BLE 5 coded (PHY_CODED / long-range)**
  mode. Falls back to 1 Mbps legacy if the central does not support it.

#### BLE Service and Characteristics

Service UUID (128-bit, MSB first): `93:6E:67:B1:19:99:B3:88:81:44:FB:74:D1:92:05:50`

| Characteristic  | UUID     | Type           | Purpose                          |
|-----------------|----------|----------------|----------------------------------|
| Application     | `0x2901` | Write + Notify | Main command/status channel      |
| Bootloader      | `0x8000` | Write + Notify | OTA firmware update              |
| UART channel 1  | `0x3901` | Write + Notify | Raw UART pass-through for port 1 |
| UART channel 2  | `0x3902` | Write + Notify | Raw UART pass-through for port 2 |
| UART channel 3  | `0x3903` | Write + Notify | Raw UART pass-through for port 3 |
| UART channel 4  | `0x3904` | Write + Notify | Raw UART pass-through for port 4 |

- Write commands to the Application characteristic (`0x2901`).
- Enable notifications by writing `0x0001` to the CCCD of the target characteristic.
- Packet size: 1–129 bytes (up to 244 bytes in bootloader mode).
- No pairing required.

#### Operation Modes

| Mode                  | Description                                                                 |
|-----------------------|-----------------------------------------------------------------------------|
| Application mode      | Normal operating mode; BLE active, 6 motor outputs controllable.            |
| Bootloader mode       | OTA firmware update; 2-minute idle timeout before auto-restart.             |
| Application sleep     | Low-power; BLE off, motors off; wakes on button press or motion (if enabled). |
| Application off       | Deep power-off; wakes only on button held for 2 seconds.                    |

#### Hardware Notes

- Main MCU: **Nordic nRF52833** ("Nordic") — always-on (except deep battery discharge).
- Co-processor: **"Tajnik"** — controls all 6 PWM motor outputs, 4 RGB LEDs, motor current
  measurement, temperature, and UART on ports 1–4. Has its own firmware and bootloader,
  upgradeable via BLE through dedicated commands.
- Ports 1–4: support LEGO Powered Up (PU) smart motor protocol and UART pass-through.
- Ports 5–6: simple PWM only (legacy Power Functions-style).

#### Application Commands

All commands are sent by writing to the Application characteristic (`0x2901`).

---

**`0x01` — Device status report** (notification, ~20 Hz, no command write required)

| Byte  | Field                          | Encoding                                                                          |
|-------|--------------------------------|-----------------------------------------------------------------------------------|
| 0     | Command                        | `0x01`                                                                            |
| 1     | Status flags (bitmask)         | Bit 6: USB connected; Bit 5: charging; Bits 4–3: battery level (0–3); Bit 2: BLE long-range PHY active; Bit 1: motion wake-up enabled; Bit 0: error |
| 2     | Battery voltage                | `9.00 V + value × 0.05 V` (range 9.00–15.35 V)                                   |
| 3–8   | Motor currents (ch 1–6)        | Unsigned 8-bit per channel; `value × 0.015 A` (range 0–3.8 A)                    |
| 9     | MCU temperature                | Unsigned 8-bit, value in °C                                                       |
| 10–11 | Accelerometer X                | Left-aligned 12-bit signed; 0.488 mg/digit                                        |
| 12–13 | Accelerometer Y                | Left-aligned 12-bit signed; 0.488 mg/digit                                        |
| 14–15 | Accelerometer Z                | Left-aligned 12-bit signed; 0.488 mg/digit                                        |
| 16    | Bootloader response command    | —                                                                                 |
| 17    | Bootloader response code       | —                                                                                 |
| 18–20 | Bootloader response data       | 3 bytes                                                                           |
| 21    | Battery-charge current         | mA                                                                                |
| 22–53 | PU motor data (ports 1–4, ×4) | Per port: motor type (uint8), velocity (int8), absolute position (uint16), position (uint32) |
| 54–67 | PID controller state (optional)| Process value (float), error (float), PID output (float), integrator state (int8), motor output (int8) |

---

**`0x20` — Set device name**

| Byte | Field       | Encoding                                                      |
|------|-------------|---------------------------------------------------------------|
| 0    | Command     | `0x20`                                                        |
| 1–12 | Device name | Up to 12 ASCII characters; NUL-terminated if shorter than 12. |

---

**`0x21` — Enable/disable motion wake-up**

| Byte | Field                    | Encoding                                             |
|------|--------------------------|------------------------------------------------------|
| 0    | Command                  | `0x21`                                               |
| 1    | Enable                   | `1` = enable motion wake-up; `0` = disable           |
| 2–5  | Idle timeout (seconds)   | Unsigned 32-bit; minimum 10 s; default 300 s         |
| 6–9  | Shallow-sleep timeout (s)| Unsigned 32-bit; `0` = disable shallow-sleep timeout |

---

**`0x22` — Set accelerometer calibration data**

| Byte | Field               | Encoding                           |
|------|---------------------|------------------------------------|
| 0    | Command             | `0x22`                             |
| 1–24 | Calibration factors | 6 × IEEE 754 single-precision float |

---

**`0x23` — Read accelerometer calibration data**

Send: byte `0x23` only.
Response: byte `0x23` + 24 bytes of calibration factors (same layout as `0x22`).

---

**`0x30` — Set motor data**

| Byte | Field                 | Encoding                                                                                    |
|------|-----------------------|---------------------------------------------------------------------------------------------|
| 0    | Command               | `0x30`                                                                                      |
| 1–6  | Motor speed (ch 1–6)  | Signed 8-bit per channel; `0x81` (−127) = full reverse, `0x00` = stop, `0x7F` (127) = full forward |
| 7    | Brake flags (bitmask) | Bits 5–0, one per motor (LSB = motor 1). 1 = slow-decay (brake); 0 = fast-decay (coast).   |
| 8    | LUT disable (bitmask) | Bits 5–0, one per motor. 1 = disable look-up table on that port.                           |

No response generated.

---

**`0x31` — Set motor data (extended, PU-aware)**

| Byte  | Field                           | Encoding                                                                                 |
|-------|---------------------------------|------------------------------------------------------------------------------------------|
| 0     | Command                         | `0x31`                                                                                   |
| 1–16  | Motor reference (ports 1–4, ×4) | Signed 32-bit per port; semantics depend on PU port mode (PWM, speed servo, position servo) |
| 17–18 | Motor speed (ports 5–6)         | Signed 8-bit per channel; same encoding as `0x30` bytes 1–2                             |
| 19    | Brake flags (bitmask)           | Bits 5–0, same as `0x30` byte 7                                                          |
| 20    | LUT disable (bitmask)           | Bits 5–0, same as `0x30` byte 8                                                          |

No response generated.

---

**`0x32` — Set data transfer period**

| Byte | Field          | Encoding                                                 |
|------|----------------|----------------------------------------------------------|
| 0    | Command        | `0x32`                                                   |
| 1    | Period (ms)    | Unsigned 8-bit; valid range 20–255 ms in steps of 5 ms. |

---

**`0x33` — Set motor ramp-up / ramp-down rates**

| Byte | Field           | Encoding                                                                          |
|------|-----------------|-----------------------------------------------------------------------------------|
| 0    | Command         | `0x33`                                                                            |
| 1–6  | Ramp-up rates   | Unsigned 8-bit per channel; % per 128 ms. 0 = disabled. Formula: `r = 100 / (T × 7.8125)` |
| 7–12 | Ramp-down rates | Same encoding as ramp-up.                                                         |

---

**`0x34` — Set motor timeout configuration**

| Byte | Field         | Encoding                                                          |
|------|---------------|-------------------------------------------------------------------|
| 0    | Command       | `0x34`                                                            |
| 1    | Configuration | 0 = stop immediately + brake; 1 = stop immediately + coast; 2–254 = coast to stop in (N−1) seconds |

---

**`0x35` — Activate connection watchdog**

| Byte | Field           | Encoding                                               |
|------|-----------------|--------------------------------------------------------|
| 0    | Command         | `0x35`                                                 |
| 1    | Timeout (s)     | Unsigned 8-bit; 0 = disable watchdog. Resets on each call. |

On expiry the device drops the connection and applies the `0x34` stop configuration.

---

**`0x36` — Set LED status**

| Byte     | Field                     | Encoding                                                              |
|----------|---------------------------|-----------------------------------------------------------------------|
| 0        | Command                   | `0x36`                                                                |
| 1–3      | Motor 1 LED               | R, G, B (unsigned 8-bit each)                                         |
| 4–6      | Motor 2 LED               | R, G, B                                                               |
| 7–9      | Motor 3 LED               | R, G, B                                                               |
| 10–12    | Motor 4 LED               | R, G, B                                                               |
| 13–16 (optional) | Blinking config (×4 LEDs) | Bits 7–4: frequency 0–15 Hz (0 = off); Bits 3–0: duty cycle (0=1/16, 15=100%) |

Send command byte alone (`0x36` only) to revert LEDs to default behaviour.
Default on connection: `0xFF` (solid white).

---

**`0x37` — Set accelerometer low-pass filter**

| Byte | Field          | Encoding                            |
|------|----------------|-------------------------------------|
| 0    | Command        | `0x37`                              |
| 1    | LPF setting    | Unsigned 8-bit; default startup = 2 |

---

**`0x38` — Set current limits**

| Byte | Field                    | Encoding                                                                              |
|------|--------------------------|---------------------------------------------------------------------------------------|
| 0    | Command                  | `0x38`                                                                                |
| 1–6  | Current limits (ch 1–6)  | Unsigned 8-bit per channel; steps of 30 mA. Defaults on connect: 1.5 A (ports 1–4), 3.0 A (ports 5–6). |

---

**`0x40` — UART baud rate setup**

| Byte | Field       | Encoding                              |
|------|-------------|---------------------------------------|
| 0    | Command     | `0x40`                                |
| 1    | Sub-command | `0x10` = Set baud rate                |
| 2    | Channel ID  | 1–4 (matches UART characteristic)    |
| 3–6  | Baud rate   | Unsigned 32-bit, little-endian        |

---

**`0x50` — Set PU port function**

| Byte | Field             | Encoding                                                                              |
|------|-------------------|---------------------------------------------------------------------------------------|
| 0    | Command           | `0x50`                                                                                |
| 1–4  | Port functions ×4 | One byte per port (ports 1–4): `0x00`=Generic PWM, `0x10`=PU simple PWM (default), `0x14`=PU speed servo, `0x15`=PU position servo, `0x16`=PU absolute position servo |

---

**`0x51` — Enable PID controller state reporting**

| Byte | Field      | Encoding                                         |
|------|------------|--------------------------------------------------|
| 0    | Command    | `0x51`                                           |
| 1    | Port index | 1–4 = enable reporting for that port; 0 = disable |

---

**`0x52` — Set servo reference input**

| Byte | Field                   | Encoding                                           |
|------|-------------------------|----------------------------------------------------|
| 0    | Command                 | `0x52`                                             |
| 1–16 | Reference values (×4)  | Signed 32-bit per channel (speed or position depending on port mode) |

---

**`0x53` — Set servo PID parameters**

| Byte | Field          | Encoding                                                                                                                                              |
|------|----------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| 0    | Command        | `0x53`                                                                                                                                                |
| 1    | Port index     | 1–4                                                                                                                                                   |
| 2–37 | PID parameters | `outLP` (float), `D_LP` (float), `speed_LP` (float), `Kp` (float), `Ki` (float), `Kd` (float), `limI` (float), `referenceRateLimit` (float), `limOut` (int8), `deadbandOut` (int8), `deadbandOutBoost` (int8), `validMode` (int8) |

PID controller runs at 100 Hz. IIR low-pass filter constant: `K_LP = 1 − Ts / (Ts + 1/(2πf_LP))` where `Ts = 0.01 s`.

---

**`0xA1` — Activate shelf mode**

| Byte | Field   | Encoding  |
|------|---------|-----------|
| 0    | Command | `0xA1`    |

Disconnects battery immediately. Device wakes only when charger is connected.

---

**`0xAC` — Check charger settings**

Send byte `0xAC` only. Response: byte `0xAC` + 1 byte of charger settings data.

---

## 16. DSL and README Maintenance Rules

### 16.1 DSL must mirror the public API

The fluent DSL (`ch.varani.bricks.ble.api.dsl`) is a thin, named-method layer built directly on
top of the public API (`ch.varani.bricks.ble.api`) and the protocol-constant classes in
`ch.varani.bricks.ble.device.*`. Any change to either of these layers **must** be reflected in
the corresponding DSL class:

| Changed layer | DSL class(es) to update |
|---|---|
| `api/` (e.g. new method on `BleConnection`) | `ConnectionDsl`, any DSL sub-builder that delegates to it |
| `device/lego/` | `LegoDsl` |
| `device/sbrick/` | `SBrickDsl` |
| `device/circuitcubes/` | `CircuitCubesDsl` |
| `device/buwizz/` (BuWizz 2.0) | `BuWizz2Dsl` |
| `device/buwizz/` (BuWizz 3.0) | `BuWizz3Dsl` |

Rules:
- A new public method on a protocol-constants class requires a corresponding named method in the
  DSL class, with a Javadoc comment referencing the constant and the protocol specification.
- A removed or renamed constant requires the DSL method to be updated or removed accordingly.
- DSL classes must **never** import from `impl` or `util`.
- Coverage requirements (≥ 99 % instruction, 100 % branch) apply to DSL classes exactly as they
  do to production classes.

### 16.2 README.md must reflect every change to the public API and DSL

`README.md` is the canonical, human-readable usage guide for this library. It must be kept
accurate at all times:

- **New public method** in `api/` or `api/dsl/` → add a usage example in the relevant section
  of `README.md`.
- **Removed public method** → remove every occurrence of that method from `README.md` examples.
- **Changed signature** (renamed parameter, different return type, etc.) → correct the
  corresponding example in `README.md`.
- **New device brand or protocol** → add a dedicated sub-section under both the DSL usage guide
  and the raw API usage guide in `README.md`.

A pull request that modifies `api/` or `api/dsl/` and does **not** update `README.md` is
incomplete and must not be merged.

---

## 17. Logging Convention

All new code must include sufficient log traces to diagnose problems without attaching a debugger.

### Java (JUL — `java.util.logging`)

- Logger declaration: `private static final Logger LOG = Logger.getLogger(ClassName.class.getName())`
- `INFO` — important lifecycle events visible by default: scan started/stopped, device discovered,
  connection established/failed/disconnected.
- `FINE` — verbose details disabled by default: each BLE write, read, notification subscription.
- `WARNING` — recoverable non-fatal errors (e.g. failed to stop scan, interrupted while waiting).
- `SEVERE` — not introduced directly; fatal errors throw a documented exception instead.
- Always use lambda-form log calls (`LOG.fine(() -> ...)`) to avoid string construction overhead
  when the level is disabled.
- Never use `e.printStackTrace()` or bare `System.out` / `System.err` — always log through `LOG`.

### Native — macOS (Objective-C / CoreBluetooth)

- Use `os_log` with subsystem `ch.varani.bricks.ble`.
- Categories: `scan` (adapter state, discovery), `connect` (connect/disconnect lifecycle),
  `gatt` (service/characteristic discovery, reads, writes).
- `os_log_info` for normal lifecycle events; `os_log_error` for failures.
- Always log `error.localizedDescription` when an `NSError *` is non-nil — never suppress it
  with `(void)error`.

### Native — Linux (C / D-Bus / BlueZ) and Windows (C++ / WinRT)

- Follow the same principle as macOS.
- Until a structured logging facility is chosen for each platform, use `fprintf(stderr, ...)` with
  a `[BLE]` prefix so output is capturable and distinguishable from application stderr.

---

## 18. External Reference: node-poweredup

[**node-poweredup**](https://github.com/nathankellenicki/node-poweredup) by Nathan Kellenicki
(MIT licence) is an authoritative community implementation of the LEGO Wireless Protocol 3.0 and
several companion protocols. It is used as a secondary cross-reference when the official LEGO BLE
Wireless Protocol 3.0 specification is ambiguous or silent on a detail.

### How to use this reference

- When adding or verifying LEGO protocol constants, cross-check against
  `src/consts.ts` in the node-poweredup repository.
- Prefer the official LEGO BLE Wireless Protocol 3.0 specification when the two sources disagree.
  Document any discrepancy in a comment on the affected constant.
- All constants derived from node-poweredup must cite it explicitly in their Javadoc comment
  (e.g. "Reference: nathankellenicki/node-poweredup — src/consts.ts `BLEManufacturerData`").

### Credit requirement

Any new file or section that incorporates values sourced from node-poweredup must include
the following attribution comment at the top of the relevant section:

```
// Reference: nathankellenicki/node-poweredup (MIT) — https://github.com/nathankellenicki/node-poweredup
```

This credit must also appear in README.md under the "Acknowledgements" section.

---

## 19. Enum-over-Constants Mandate

**Prefer typed enums over raw `int` constants for every DSL method parameter that represents a
closed, protocol-defined set of values.**

### Rationale

A parameter such as "port ID" or "device type" is drawn from a fixed, finite set defined by the
protocol specification. Accepting a raw `int` at the DSL boundary allows callers to pass
out-of-range values that will silently corrupt BLE packets. A typed enum makes illegal states
unrepresentable at compile time.

### When to use an enum

| Use an enum | Keep as raw `int` / `long` |
|---|---|
| Protocol-defined closed set (port IDs, device types, LED colours, sensor modes, action codes, alert types, …) | Free-range numeric values (power −100..100, frequency Hz, duration ms, baud rate, …) |

### Rules for every new enum

1. **One enum per concept** — do not reuse an existing enum for a conceptually different field
   even if the wire values happen to overlap.
2. **Wrap the corresponding `LegoProtocolConstants` (or brand-equivalent constants class)
   constant** — the enum constructor must accept the constant, not a duplicate literal.
   ```java
   A(LegoProtocolConstants.WEDO2_PORT_A)   // correct
   A(0x01)                                 // forbidden — duplicates the constant
   ```
3. **Expose `public int code()`** — returns the raw wire byte for use in BLE payload builders.
4. **Full Javadoc on every constant** — include the wire value, the protocol reference, and
   the `LegoProtocolConstants` constant it wraps using a `{@link}` reference.
5. **Class-level Javadoc** — explain which BLE command field the enum encodes, cite the
   protocol spec section, and include a short usage example in a `{@code}` block.
6. **Dedicated test class** — follow the pattern of `WeDo2LedColorTest.java`:
   - `values_hasNConstants()` — asserts exact enum cardinality.
   - `valueOf_allConstants_roundTrips()` — round-trip through `valueOf(name())`.
   - One `code_<constant>_is<hex>()` test per constant, comparing against the
     `LegoProtocolConstants` value (or the literal when no named constant exists).
   - `allConstants_codes_haveExpectedValues()` — `assertAll` summary of every constant.
   - `allConstants_areNotNull()` — null guard over `values()`.
7. **DSL test coverage** — every DSL method that accepts the new enum must have tests in the
   DSL test class that exercise each enum constant.
8. **Place enums in the correct package** — `ch.varani.bricks.ble.device.<brand>/` alongside
   the protocol-constants class. Never place enums in `api/` or `impl/`.

### Package placement

| Brand | Enum package |
|---|---|
| LEGO Powered Up | `ch.varani.bricks.ble.device.lego` |
| SBrick | `ch.varani.bricks.ble.device.sbrick` |
| Circuit Cubes | `ch.varani.bricks.ble.device.circuitcubes` |
| BuWizz | `ch.varani.bricks.ble.device.buwizz` |

### Existing enums (reference implementations)

| Enum | Concept | Brand |
|---|---|---|
| `WeDo2Port` | Physical port A / B | LEGO WeDo 2.0 |
| `WeDo2DeviceType` | Peripheral type ID in subscription command | LEGO WeDo 2.0 |
| `WeDo2SensorMode` | Sensor mode index in subscription command | LEGO WeDo 2.0 |
| `WeDo2LedColor` | RGB LED colour index | LEGO WeDo 2.0 |
| `LegoColor` | General hub LED colour | LEGO Powered Up |
| `LegoHubType` | Hub hardware type | LEGO Powered Up |
