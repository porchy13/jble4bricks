# Bluetooth LE API for Lego Powered Up

A Java library for connecting to and controlling LEGO Powered Up devices and compatible third-party
hardware via Bluetooth Low Energy (BLE).

## Overview

This library provides a unified, cross-platform Java API to discover, connect, and interact with
BLE-enabled smart brick controllers and accessories, including:

| Brand | Supported devices |
|---|---|
| **LEGO Powered Up** | Hub 2 (88009), Technic Hub (88012), Move Hub (88006), City Hub (88010), Mario Hub |
| **SBrick** | SBrick, SBrick Plus |
| **FX Bricks** | BrickPi3, PiStorms (BLE mode) |
| **Circuit Cubes** | Bluetooth Cube |

The API abstracts away OS-level and hardware-level BLE differences behind a single, consistent
interface. Consumers write their logic once and run it on any supported platform.

## Requirements

| Requirement | Version |
|---|---|
| Java | 25 |
| Maven | 3.9+ |

## Supported Platforms

| OS | amd64 | aarch64 |
|---|---|---|
| macOS 13+ | yes | yes (Apple Silicon) |
| Windows 11 | yes | yes |
| Linux (kernel 5.10+, BlueZ 5.50+) | yes | yes |

## Getting Started

### Add the dependency

```xml
<dependency>
    <groupId>ch.varani.bricks</groupId>
    <artifactId>ble-api</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Scan for devices

```java
BleScanner scanner = BleScanner.create();
scanner.scan(device -> System.out.println("Found: " + device.name() + " @ " + device.address()));
```

### Connect and send a command

```java
try (BleHub hub = BleHub.connect(address)) {
    hub.setMotorSpeed(Port.A, 50);   // 50 % forward
    Thread.sleep(2_000);
    hub.setMotorSpeed(Port.A, 0);    // stop
}
```

## Building from Source

```bash
# Compile and run all unit tests
mvn verify

# Generate the coverage report (target/site/jacoco/index.html)
mvn jacoco:report

# Generate Javadoc (target/site/apidocs/index.html)
mvn javadoc:jar
```

## Quality Gates

Every build enforces the following quality rules:

| Tool | Rule |
|---|---|
| **JaCoCo** | 100 % instruction and branch coverage (unit tests) |
| **Checkstyle** | Project-defined rule set (`varani_java_checks.xml`) |
| **SpotBugs / FindBugs** | Zero bugs at confidence level NORMAL or higher |
| **OWASP Dependency-Check** | Zero known CVEs in compile/runtime dependencies |
| **SonarQube** | Zero blocker or critical issues; Quality Gate = Passed |

A build that violates any of these rules **fails** and must not be merged.

## Architecture

```
src/
  main/java/ch/varani/lego/ble/
    api/          # Public interfaces and value types (no BLE stack dependency)
    impl/         # Platform-specific BLE adapter implementations
      macos/      # CoreBluetooth bridge via JNI / FFM
      windows/    # WinRT Bluetooth LE via JNI / FFM
      linux/      # D-Bus / BlueZ bridge via JNI / FFM
    device/       # Per-brand protocol implementations
      lego/       # LEGO Wireless Protocol 3.0
      sbrick/     # SBrick BLE protocol
      fxbricks/   # FX Bricks BLE protocol
      circuitcubes/ # Circuit Cubes BLE protocol
    util/         # Internal helpers (not part of the public API)
  test/java/ch/varani/lego/ble/
    # Mirrors the main tree; every production class has a corresponding test class
```

The `api` package is the only package that library consumers depend on. All other packages are
internal and subject to change without notice.

## Contributing

1. Fork the repository and create a feature branch.
2. Write code and tests together — coverage must remain at 100 %.
3. Run `mvn verify` locally and confirm all checks pass.
4. Open a pull request; CI must be green before review.

See [AGENTS.md](AGENTS.md) for the full set of coding standards and agent guidelines.

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
