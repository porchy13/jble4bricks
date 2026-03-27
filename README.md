# Bluetooth LE API for Lego Powered Up

A Java library for connecting to and controlling LEGO Powered Up devices and compatible
third-party hardware via Bluetooth Low Energy (BLE).

## Overview

This library provides a unified, cross-platform Java API to discover, connect, and interact with
BLE-enabled smart brick controllers and accessories, including:

| Brand | Supported devices |
|---|---|
| **LEGO Powered Up** | Hub 2 (88009), Technic Hub (88012), Move Hub (88006), City Hub (88010), Mario Hub |
| **SBrick** | SBrick, SBrick Plus |
| **FX Bricks** | BrickPi3, PiStorms (BLE mode) |
| **Circuit Cubes** | Bluetooth Cube |
| **BuWizz** | BuWizz 2.0, BuWizz 3.0 |

The API abstracts away OS-level and hardware-level BLE differences behind a single, consistent
interface. Consumers write their logic once and run it on any supported platform.

Two usage styles are available:

- **Raw API** (`ch.varani.bricks.ble.api`) — direct access to BLE primitives: scan, connect,
  write, read, notifications.
- **Fluent DSL** (`ch.varani.bricks.ble.api.dsl`) — a higher-level, method-chaining interface
  built on top of the raw API. The DSL maps each supported protocol's commands to clearly named
  methods and handles byte encoding for you.

---

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

---

## Add the Dependency

```xml
<dependency>
    <groupId>ch.varani.bricks</groupId>
    <artifactId>ble-api</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

---

## Usage — Fluent DSL

The DSL entry point is `BrickDsl.open()`. It returns an `AutoCloseable` session that can be used
in a try-with-resources block. Every scan, connection, and device-specific operation chains from
that single entry point.

### General pattern

```java
try (BrickDsl dsl = BrickDsl.open()) {
    dsl.scan()
       .<filter>()           // forLegoHubs(), forSBricks(), forBuWizz3(), …
       .timeoutSeconds(10)   // optional: stop scan after 10 s
       .first()              // wait for the first matching device
       .thenConnect()        // establish the BLE connection
       .<asXxx>()            // asLego(), asSBrick(), asBuWizz2(), …
       .<commands>           // device-specific operations
       .done();              // disconnect and release resources
}
```

### LEGO Powered Up hub

```java
try (BrickDsl dsl = BrickDsl.open()) {
    LegoDsl lego = dsl.scan()
        .forLegoHubs()
        .timeoutSeconds(15)
        .first()
        .thenConnect()
        .asLego();

    // Subscribe to all upstream notifications
    lego.notifications().subscribe(subscriber);

    // Request battery voltage once (hub responds via notification)
    lego.requestBatteryVoltage().get();

    // Enable continuous battery voltage updates
    lego.enableBatteryVoltageUpdates().get();

    // Request firmware and hardware version
    lego.requestFirmwareVersion().get();
    lego.requestHardwareVersion().get();

    // Request additional hub properties
    lego.requestRssi().get();
    lego.requestManufacturerName().get();
    lego.requestRadioFirmwareVersion().get();
    lego.requestLwpVersion().get();
    lego.requestSystemTypeId().get();
    lego.requestPrimaryMac().get();
    lego.requestSecondaryMac().get();

    // Control motors
    lego.motor(0x00).startSpeed(80).get();          // port A, 80 % forward
    lego.motor(0x00).startSpeed(80, 100).get();     // port A, speed 80, max power 100
    lego.motor(0x00).startSpeedForTime(2000, 50, 100).get();    // run for 2 s
    lego.motor(0x00).startSpeedForDegrees(360, 50, 100).get();  // rotate 360°
    lego.motor(0x00).gotoAbsolutePosition(0, 50, 100).get();    // go to position 0
    lego.motor(0x00).setAccTime(500).get();   // 500 ms ramp-up
    lego.motor(0x00).setDecTime(300).get();   // 300 ms ramp-down

    // Motor commands with explicit end state (FLOAT, HOLD, or BRAKE)
    lego.motor(0x00).startSpeedForTime(2000, 50, 100,
        LegoProtocolConstants.BRAKING_STYLE_BRAKE).get();
    lego.motor(0x00).startSpeedForDegrees(360, 50, 100,
        LegoProtocolConstants.BRAKING_STYLE_FLOAT).get();
    lego.motor(0x00).gotoAbsolutePosition(0, 50, 100,
        LegoProtocolConstants.BRAKING_STYLE_HOLD).get();

    // Low-level WriteDirect / WriteDirectModeData (escape hatch for any port output device)
    lego.motor(0x32).writeDirect((byte) 0x06).get();                     // raw WriteDirect
    lego.motor(0x32).writeDirectModeData(0x00, (byte) 0x09).get();       // mode 0, payload 0x09

    // Chain motor operations
    lego.motor(0x00).startSpeed(60).get();
    lego.motor(0x01).startSpeed(-60).get();   // port B, reverse

    // Hub actions
    lego.hubAction().vccPortOn().get();
    lego.hubAction().activateBusyIndication().get();  // show busy LED state
    lego.hubAction().resetBusyIndication().get();     // clear busy LED state
    lego.hubAction().disconnect().get();
    lego.hubAction().switchOff().get();
    lego.hubAction().shutdown().get();                // immediate power-off, no upstream event

    // Hub alerts
    lego.enableAlert(LegoProtocolConstants.HUB_ALERT_LOW_VOLTAGE).get();
    lego.requestAlert(LegoProtocolConstants.HUB_ALERT_HIGH_CURRENT).get();
    lego.disableAlert(LegoProtocolConstants.HUB_ALERT_LOW_VOLTAGE).get();

    // Port information
    lego.requestPortInfo(0x00, 0x01).get();
    lego.requestPortModeInfo(0x00, 0x00, 0x04).get();

    // Configure sensor port input format (single mode)
    lego.setupPortInputFormatSingle(0x00, 0x00, 1, true).get();  // port 0, mode 0, delta=1, notify

    // Set hub LED colour (use a hub-specific LED port constant)
    lego.setHubLedColor(LegoProtocolConstants.CITY_HUB_PORT_LED,
        LegoProtocolConstants.COLOR_RED).get();

    // Play a sound on the Duplo Train Base speaker
    lego.playDuploSound(0x01, LegoProtocolConstants.DUPLO_SOUND_HORN).get();

    // Send a raw LWP message (must be a complete, valid LWP packet)
    lego.writeRaw(new byte[]{ 0x05, 0x00, 0x01, 0x06, 0x05 }).get();

    lego.done();
}
```

### WeDo 2.0 hub (custom GATT UUIDs)

The WeDo 2.0 hub does not use LWP3 UUIDs. It exposes separate write and notify
characteristics. Use the 4-argument `LegoDsl` constructor to supply custom service and
characteristic UUIDs:

```java
import ch.varani.bricks.ble.device.lego.LegoProtocolConstants;

try (BrickDsl dsl = BrickDsl.open()) {
    BleConnection connection = dsl.scan()
        .forWeDo2()
        .timeoutSeconds(10)
        .first()
        .thenConnect()
        .connection();

    LegoDsl wedo2 = new LegoDsl(
        connection,
        LegoProtocolConstants.WEDO2_SERVICE_UUID,          // primary service (notifications)
        LegoProtocolConstants.WEDO2_MOTOR_VALUE_WRITE_UUID, // write characteristic
        LegoProtocolConstants.WEDO2_BUTTON_UUID             // notify characteristic
    );

    // Subscribe to button / notification events
    wedo2.notifications().subscribe(subscriber);

    // Send a raw motor command: [portId, typeId, mode, power]
    wedo2.writeRaw(new byte[]{
        (byte) LegoProtocolConstants.WEDO2_PORT_A,
        (byte) LegoProtocolConstants.WEDO2_MOTOR_TYPE_ID,
        0x01,   // mode
        (byte) 75  // power (signed, −100 to 100)
    }).get();

    wedo2.done();
}
```

Key WeDo 2.0 constants in `LegoProtocolConstants`:

| Constant | Value | Description |
|---|---|---|
| `WEDO2_SERVICE_UUID` | `00001523-…` | Primary GATT service (notifications) |
| `WEDO2_SERVICE_2_UUID` | `00004f0e-…` | Secondary GATT service (write characteristics) |
| `WEDO2_MOTOR_VALUE_WRITE_UUID` | `00001565-…` | Motor command characteristic (write) |
| `WEDO2_PORT_TYPE_WRITE_UUID` | `00001563-…` | Sensor subscription characteristic (write) |
| `WEDO2_BUTTON_UUID` | `00001526-…` | Button/general notification characteristic |
| `WEDO2_SENSOR_VALUE_UUID` | `00001560-…` | Sensor value notification characteristic |
| `WEDO2_BATTERY_SERVICE_UUID` | `0000180f-…` | Standard BLE Battery Service |
| `WEDO2_BATTERY_LEVEL_UUID` | `00002a19-…` | Standard BLE Battery Level characteristic |
| `WEDO2_PORT_A` | `0x01` | Port A identifier |
| `WEDO2_PORT_B` | `0x02` | Port B identifier |
| `WEDO2_MOTOR_TYPE_ID` | `0x01` | Simple/Medium Linear Motor device type |
| `WEDO2_MOTION_SENSOR_TYPE_ID` | `0x23` | Motion/Distance Sensor device type |
| `WEDO2_RGB_LED_TYPE_ID` | `0x22` | RGB LED device type |
```

### SBrick

```java
try (BrickDsl dsl = BrickDsl.open()) {
    SBrickDsl sbrick = dsl.scan()
        .forSBricks()
        .timeoutSeconds(10)
        .first()
        .thenConnect()
        .asSBrick();

    // Subscribe to response notifications
    sbrick.notifications().subscribe(subscriber);

    // Drive channels
    sbrick.drive(SBrickProtocolConstants.CHANNEL_A, false, 200).get(); // channel A, CW, power 200
    sbrick.drive(SBrickProtocolConstants.CHANNEL_B, true,  128).get(); // channel B, CCW, power 128

    // Brake one or more channels at once
    sbrick.brake(SBrickProtocolConstants.CHANNEL_A, SBrickProtocolConstants.CHANNEL_B).get();

    // Query ADC channels
    sbrick.queryBatteryVoltage().get();    // battery voltage
    sbrick.queryTemperature().get();       // internal temperature
    sbrick.queryAdc(SBrickProtocolConstants.ADC_CHANNEL_BATTERY).get(); // raw ADC

    // Watchdog (keeps the SBrick alive while the app is running)
    sbrick.setWatchdogTimeout(5).get();   // 0.5 s (units of 0.1 s)
    sbrick.getWatchdogTimeout().get();

    sbrick.done();
}
```

### Circuit Cubes

```java
try (BrickDsl dsl = BrickDsl.open()) {
    CircuitCubesDsl cc = dsl.scan()
        .forCircuitCubes()
        .timeoutSeconds(10)
        .first()
        .thenConnect()
        .asCircuitCubes();

    // Subscribe to RX notifications (e.g. battery voltage response)
    cc.notifications().subscribe(subscriber);

    // Drive motors forward / reverse / stop
    cc.motorForward(CircuitCubesProtocolConstants.CHANNEL_A, 128).get();
    cc.motorReverse(CircuitCubesProtocolConstants.CHANNEL_B, 64).get();
    cc.motorStop(CircuitCubesProtocolConstants.CHANNEL_C).get();

    // Query battery voltage (response arrives as an RX notification)
    cc.queryBattery().get();

    cc.done();
}
```

### BuWizz 2.0

```java
try (BrickDsl dsl = BrickDsl.open()) {
    BuWizz2Dsl bw2 = dsl.scan()
        .forBuWizz2()
        .timeoutSeconds(10)
        .first()
        .thenConnect()
        .asBuWizz2();

    // Subscribe to ~25 Hz status reports
    bw2.notifications().subscribe(subscriber);

    // Set power level before sending motor commands (default is 0 = disabled)
    bw2.setPowerLevel(BuWizz2ProtocolConstants.POWER_LEVEL_NORMAL).get();

    // Set motor speeds (channels 1–4, signed −127 to 127) and brake flags
    bw2.setMotorData(
        100, -100, 0, 0,    // speeds: ch1 forward, ch2 reverse, ch3 stop, ch4 stop
        false, false, false, false  // brake flags: coast mode on all channels
    ).get();

    // Stop all motors (coast)
    bw2.stopAllMotors().get();

    // Adjust current limits (steps of 33 mA per channel)
    bw2.setCurrentLimits(23, 23, 23, 23).get(); // ~750 mA each

    bw2.done();
}
```

### BuWizz 3.0

```java
try (BrickDsl dsl = BrickDsl.open()) {
    BuWizz3Dsl bw3 = dsl.scan()
        .forBuWizz3()
        .timeoutSeconds(10)
        .first()
        .thenConnect()
        .asBuWizz3();

    // Subscribe to ~20 Hz status reports
    bw3.notifications().subscribe(subscriber);

    // Set motor speeds on all 6 channels (signed −127 to 127) + brake flags
    bw3.setMotorData(
        100, -50, 0, 0, 80, -80,      // speeds: ch1–6
        false, true, false, false, false, false  // brake flags: brake on ch2
    ).get();

    // Stop all 6 motors (coast)
    bw3.stopAllMotors().get();

    // Set LED colours (4 LEDs, RGB 0–255)
    bw3.setAllLeds(
        0xFF, 0x00, 0x00,   // LED 1: red
        0x00, 0xFF, 0x00,   // LED 2: green
        0x00, 0x00, 0xFF,   // LED 3: blue
        0xFF, 0xFF, 0x00    // LED 4: yellow
    ).get();
    bw3.setLedsUniform(0x00, 0xFF, 0xFF).get();  // all LEDs: cyan
    bw3.resetLeds().get();                        // revert to default

    // Configure the connection watchdog
    bw3.setWatchdog(5).get();    // disconnect after 5 s of silence

    // Set notification period
    bw3.setDataTransferPeriod(50).get();  // 50 ms (~20 Hz)

    // Set motor timeout behaviour on disconnect
    bw3.setMotorTimeout(1).get();   // coast to stop immediately

    // Set current limits for all 6 channels (steps of 30 mA)
    bw3.setCurrentLimits(50, 50, 50, 50, 100, 100).get(); // 1.5 A / 3.0 A

    // Rename the device
    bw3.setDeviceName("MyBuWizz").get();

    // Configure PU port functions (ports 1–4)
    bw3.setPuPortFunctions(
        BuWizz3ProtocolConstants.PU_PORT_FUNCTION_SPEED_SERVO,
        BuWizz3ProtocolConstants.PU_PORT_FUNCTION_SIMPLE_PWM,
        BuWizz3ProtocolConstants.PU_PORT_FUNCTION_SIMPLE_PWM,
        BuWizz3ProtocolConstants.PU_PORT_FUNCTION_SIMPLE_PWM
    ).get();

    // Activate shelf mode (deep power-off; wakes on charger connect)
    bw3.activateShelfMode().get();

    bw3.done();
}
```

### Scanning for multiple devices at once

```java
try (BrickDsl dsl = BrickDsl.open()) {
    // Collect up to 3 LEGO hubs, stopping after 20 s
    List<BleDevice> hubs = dsl.scan()
        .forLegoHubs()
        .timeoutSeconds(20)
        .collect(3);

    for (BleDevice hub : hubs) {
        System.out.println(hub.name() + " @ " + hub.id());
    }
}
```

### Using a custom GATT service UUID filter

```java
try (BrickDsl dsl = BrickDsl.open()) {
    DeviceDsl device = dsl.scan()
        .forService("12345678-1234-1234-1234-1234567890ab")
        .timeoutSeconds(10)
        .first();
}
```

### Filtering by LEGO hub type

Use `forLegoHubType(LegoHubType)` to restrict the scan to a specific LEGO Powered Up hub
model. The hub type is identified by the System Type and Device Number byte in the
manufacturer-specific advertisement payload.

```java
import ch.varani.bricks.ble.device.lego.LegoHubType;

try (BrickDsl dsl = BrickDsl.open()) {
    DeviceDsl hub = dsl.scan()
        .forLegoHubs()                          // GATT service UUID filter (OS level)
        .forLegoHubType(LegoHubType.CITY_HUB)  // manufacturer data filter
        .timeoutSeconds(15)
        .first();
}
```

Available hub types:

| `LegoHubType` constant | Hub model |
|---|---|
| `WEDO2_HUB` | WeDo 2.0 Hub |
| `DUPLO_TRAIN` | Duplo Train Hub |
| `BOOST_MOVE_HUB` | Boost Move Hub |
| `CITY_HUB` | 2-Port Hub (City Hub / Hub 2) |
| `HANDSET_2PORT` | 2-Port Handset |
| `TECHNIC_HUB` | Technic Hub (4-Port Hub) |
| `MARIO_HUB` | Super Mario Hub |

The `forWeDo2()` shortcut is equivalent to `forLegoHubType(LegoHubType.WEDO2_HUB)`:

```java
DeviceDsl hub = dsl.scan().forWeDo2().timeoutSeconds(10).first();
```

### Filtering by device identifier (train layout management)

Use `withDeviceId(String)` to wait for a hub with a known persistent BLE identifier.
This is useful when multiple hubs are in range and each hub has a fixed role (e.g.
freight train, passenger train):

```java
try (BrickDsl dsl = BrickDsl.open()) {
    DeviceDsl freightTrain = dsl.scan()
        .forLegoHubs()
        .withDeviceId("A1B2C3D4-0000-0000-0000-000000000001")
        .timeoutSeconds(20)
        .first();

    DeviceDsl passengerTrain = dsl.scan()
        .forLegoHubs()
        .withDeviceId("A1B2C3D4-0000-0000-0000-000000000002")
        .timeoutSeconds(20)
        .first();
}
```

Filters are additive (AND semantics): `withDeviceId` and `forLegoHubType` can be chained
together so that only the exact hub with the right type and the right identifier is accepted.

---

## Usage — Raw API

The raw API gives direct access to the underlying BLE abstractions. Use it when you need
lower-level control or when integrating with protocols not yet covered by the DSL.

### Core interfaces

| Interface | Description |
|---|---|
| `BleScanner` | Starts and stops BLE advertisement scans |
| `BleDevice` | Discovered peripheral (name, address, RSSI) |
| `BleConnection` | Active connection — write, read, notifications |
| `BleScannerFactory` | Creates the platform-appropriate `BleScanner` |
| `ScanCallback` | Callback invoked for each discovered device |

### Obtain a scanner

```java
BleScanner scanner = BleScannerFactory.create();
```

### Scan for devices

```java
// Start an open scan (no service UUID filter)
CompletableFuture<Void> started = scanner.startScan(null, device -> {
    System.out.println("Found: " + device.name() + " @ " + device.id()
        + " RSSI=" + device.rssi());
});

started.get();          // wait for the scan to be running
Thread.sleep(5_000);
scanner.stopScan();
```

### Scan with a service UUID filter

```java
import ch.varani.bricks.ble.device.lego.LegoProtocolConstants;

scanner.startScan(LegoProtocolConstants.HUB_SERVICE_UUID, device -> {
    // called only for peripherals advertising the LEGO Hub service
    System.out.println("LEGO hub: " + device.name());
});
```

### Connect to a device

```java
BleDevice device = /* obtained from a scan callback */;
try (BleConnection connection = device.connect().get()) {
    // use the connection …
}
```

### Write to a GATT characteristic

```java
// Write Without Response
CompletableFuture<Void> write = connection.writeWithoutResponse(
    LegoProtocolConstants.HUB_SERVICE_UUID,
    LegoProtocolConstants.HUB_CHARACTERISTIC_UUID,
    new byte[]{ 0x05, 0x00, 0x01, 0x06, 0x05 }  // request battery voltage
);
write.get();
```

### Read from a GATT characteristic

```java
CompletableFuture<byte[]> read = connection.read(serviceUuid, characteristicUuid);
byte[] value = read.get();
```

### Subscribe to notifications

```java
import java.util.concurrent.Flow;

Flow.Publisher<byte[]> publisher = connection.notifications(
    LegoProtocolConstants.HUB_SERVICE_UUID,
    LegoProtocolConstants.HUB_CHARACTERISTIC_UUID
);

publisher.subscribe(new Flow.Subscriber<>() {
    @Override public void onSubscribe(Flow.Subscription s) { s.request(Long.MAX_VALUE); }
    @Override public void onNext(byte[] payload) { /* process message */ }
    @Override public void onError(Throwable t) { /* handle error */ }
    @Override public void onComplete() { /* scan ended */ }
});
```

### Disconnect

```java
connection.disconnect().get();
// or simply close the try-with-resources block
```

### Exception handling

All BLE errors surface as `BleException` (an unchecked exception). Catch it where you need to
handle platform or connectivity failures:

```java
try {
    BleScanner scanner = BleScannerFactory.create();
} catch (BleException ex) {
    System.err.println("BLE not available: " + ex.getMessage());
}
```

---

## Diagnosing Connection Problems

The library uses `java.util.logging` (JUL) for diagnostics. No third-party logging framework
is required and no configuration is needed to see `INFO`-level events — they are on by default.

### Log levels

| Level | What is logged |
|---|---|
| `INFO` | Important lifecycle events: scan started/stopped, device discovered, connection established or failed, disconnection. On by default. |
| `FINE` | Verbose per-operation details: every BLE write, read, notification subscription, pre-filter device evaluation, and native library load. Off by default. |
| `WARNING` | Recoverable non-fatal errors: failed to stop scan, interrupted wait, failed to disable notifications on disconnect. |

`SEVERE` is never used directly — unrecoverable errors always throw a documented `BleException`.

### Logger names

Every class has its own logger named after its fully-qualified class name. The complete list:

| Logger name | Covers |
|---|---|
| `ch.varani.bricks.ble.util.NativeLibraryLoader` | JAR extraction and `System.load()` of the native library |
| `ch.varani.bricks.ble.api.dsl.ScanDsl` | Scan lifecycle, per-device filter decisions |
| `ch.varani.bricks.ble.impl.macos.MacOsBleScanner` | macOS scan start/stop, device discovery, connect handoff |
| `ch.varani.bricks.ble.impl.macos.MacOsBleDevice` | macOS connection initiation |
| `ch.varani.bricks.ble.impl.macos.MacOsBleConnection` | macOS GATT writes, reads, notifications, disconnect |
| `ch.varani.bricks.ble.impl.linux.LinuxBleScanner` | Linux (BlueZ) scan start/stop, device discovery, connect handoff |
| `ch.varani.bricks.ble.impl.linux.LinuxBleDevice` | Linux connection initiation |
| `ch.varani.bricks.ble.impl.linux.LinuxBleConnection` | Linux GATT writes, reads, notifications, disconnect |
| `ch.varani.bricks.ble.impl.windows.WindowsBleScanner` | Windows (WinRT) scan start/stop, device discovery, connect handoff |
| `ch.varani.bricks.ble.impl.windows.WindowsBleDevice` | Windows connection initiation |
| `ch.varani.bricks.ble.impl.windows.WindowsBleConnection` | Windows GATT writes, reads, notifications, disconnect |

The shared prefix `ch.varani.bricks.ble` can be used to control all loggers at once.

### Enable verbose logging

#### Option 1 — `logging.properties` file

Create `logging.properties` anywhere on your classpath (e.g. `src/main/resources/` for a Maven
project, or the same directory as your executable JAR) and pass it to the JVM:

```bash
java -Djava.util.logging.config.file=logging.properties -jar your-app.jar
```

Minimal configuration to see everything the library emits:

```properties
# Send all output to the console
handlers=java.util.logging.ConsoleHandler
java.util.logging.ConsoleHandler.level=ALL
java.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter

# Show FINE for the entire library
ch.varani.bricks.ble.level=FINE

# Optional: restrict verbose output to one subsystem only
# ch.varani.bricks.ble.impl.macos.MacOsBleConnection.level=FINE
```

To also see native library loading traces add:

```properties
ch.varani.bricks.ble.util.NativeLibraryLoader.level=FINE
```

#### Option 2 — programmatic configuration

Call this before any library method (including `BrickDsl.open()` or `BleScannerFactory.create()`):

```java
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

// Enable FINE for the entire library
Logger libRoot = Logger.getLogger("ch.varani.bricks.ble");
libRoot.setLevel(Level.FINE);

ConsoleHandler handler = new ConsoleHandler();
handler.setLevel(Level.FINE);
libRoot.addHandler(handler);

// Or narrow it to a single class, e.g. to trace only GATT operations:
Logger.getLogger("ch.varani.bricks.ble.impl.macos.MacOsBleConnection")
      .setLevel(Level.FINE);
```

### Reading the log output

#### Successful connection — `INFO` level

```
INFO  ScanDsl             Starting scan: serviceUuidFilter=00001623-... deviceFilter=none count=1 timeout=10s
INFO  MacOsBleScanner     Device found: id=A1B2C3D4-... name='Technic Hub' rssi=-62 mfrData=10 bytes
INFO  ScanDsl             Scan completed: found 1 device(s)
INFO  MacOsBleDevice      Initiating connection to device: id=A1B2C3D4-... name='Technic Hub'
INFO  MacOsBleScanner     Connecting to peripheral: A1B2C3D4-...
INFO  MacOsBleConnection  BLE connection established: connPtr=0x600003e80040
```

The same sequence on Linux replaces `MacOs*` with `Linux*` and uses a D-Bus object path
(`/org/bluez/hci0/dev_XX_XX_XX_XX_XX_XX`) as the device identifier. On Windows it uses
`Windows*` and a WinRT device address string.

#### GATT operations — `FINE` level

```
FINE  MacOsBleConnection  Write: chr=00001624-... svc=00001623-... len=5 bytes
FINE  MacOsBleConnection  Enabling notifications: chr=00001624-... svc=00001623-...
FINE  MacOsBleConnection  Read: chr=00001624-... svc=00001623-...
INFO  MacOsBleConnection  BLE connection disconnected
```

#### Pre-filter device evaluation — `FINE` level

Before applying Java-side filters (hub type, device ID) each discovered device is logged with
its raw manufacturer data so you can verify the advertisement payload without a BLE sniffer:

```
FINE  ScanDsl  onDeviceFound pre-filter: id=A1B2C3D4-... name='Technic Hub' mfrData=[97 03 00 41 06 00 00] byte[3]=0x41
FINE  ScanDsl  Device accepted by filter: A1B2C3D4-...
```

`byte[3]` is the System Type + Device Number byte used to identify the hub model
(see `LegoProtocolConstants.MANUFACTURER_DATA_IDX_SYSTEM_TYPE`). If it shows `n/a` the
manufacturer data payload is shorter than 4 bytes and hub-type filtering will not match.

#### Native library loading — `FINE` level

Useful when the library fails to start (throws `BleException: native library not found`):

```
FINE  NativeLibraryLoader  Loading native library from JAR resource: /natives/macos/universal/libbleBridge.dylib
FINE  NativeLibraryLoader  Loaded native library: /var/folders/.../libbleBridge.dylib
FINE  NativeLibraryLoader  Calling System.load: /var/folders/.../libbleBridge.dylib
```

If these lines are absent the JAR does not contain a native library for the current OS and
architecture. If `Calling System.load` appears but is followed by a `BleException` the library
was found but rejected by the JVM (wrong architecture, missing system dependency, etc.).

### Common failure patterns

**Scan times out — no device found**

```
INFO  ScanDsl  Starting scan: serviceUuidFilter=00001623-... count=1 timeout=10s
INFO  ScanDsl  Scan completed: found 0 device(s)
```

Causes and checks:
- The hub is not powered on, or the battery is too low to advertise.
- Bluetooth is disabled on the host. On macOS the `os_log` stream (see below) shows
  `centralManagerDidUpdateState` with state `PoweredOff`. On Linux, `hciconfig` will show the
  adapter as `DOWN`. On Windows, check the Bluetooth status in Settings.
- The hub is already connected to another host.
- The GATT service UUID filter is too restrictive. Try a scan with no filter to confirm the
  device is visible at all:

  ```java
  dsl.scan().forService(null).timeoutSeconds(10).collect(10);
  ```

**Connection established but service discovery fails**

```
WARNING MacOsBleScanner  Connection to A1B2C3D4-... failed: Connection or service discovery failed
```

Causes and checks:
- The hub firmware does not expose the expected GATT service. Verify the UUID constants in
  `LegoProtocolConstants`, `SBrickProtocolConstants`, etc.
- A stale connection from a previous session was not cleanly closed — power-cycle the hub.

**Device found but rejected by filter**

```
FINE  ScanDsl  Device rejected by filter: A1B2C3D4-...
```

The device passed the OS-level GATT service UUID filter but failed the Java-side filter
(`withDeviceId`, `forLegoHubType`). Check that:
- The device identifier string matches exactly (case-sensitive on all platforms).
- The manufacturer data payload is at least 4 bytes and `byte[3]` matches the expected hub
  type. The pre-filter log line (see above) shows the raw bytes.

**Stale advertisement after scan stop**

```
FINE  MacOsBleScanner  onDeviceFound: no active scan, discarding device: A1B2C3D4-...
```

Informational only — the OS delivered an advertisement just after `stopScan()` was called.
The device is discarded; no action is required.

**Scan stop interrupted**

```
WARNING ScanDsl  Interrupted while waiting for scan to stop
```

The thread waiting for the scan to stop was interrupted (e.g. by a timeout or explicit
`Thread.interrupt()`). The scan is considered stopped; the interrupt flag is re-set on the
calling thread.

### Reading native logs on macOS

The CoreBluetooth layer writes to the system `os_log` stream under subsystem
`ch.varani.bricks.ble` with three categories:

| Category | Content |
|---|---|
| `scan` | Adapter state changes (Bluetooth on/off/resetting), scan start/stop, every discovered peripheral with UUID and RSSI |
| `connect` | Connect/disconnect lifecycle, error descriptions from `NSError.localizedDescription` |
| `gatt` | Service and characteristic discovery, GATT reads/writes/notification enable/disable |

Read them with the `log` command-line tool or Console.app:

```bash
# Stream everything from the library in real time
log stream --predicate 'subsystem == "ch.varani.bricks.ble"' --level debug

# Scope to one category
log stream --predicate 'subsystem == "ch.varani.bricks.ble" AND category == "connect"' --level debug
log stream --predicate 'subsystem == "ch.varani.bricks.ble" AND category == "scan"'    --level debug
log stream --predicate 'subsystem == "ch.varani.bricks.ble" AND category == "gatt"'    --level debug

# Collect a timed capture to a file for later inspection
log collect --predicate 'subsystem == "ch.varani.bricks.ble"' --last 5m --output ble-trace.logarchive
log show ble-trace.logarchive --predicate 'subsystem == "ch.varani.bricks.ble"' --info --debug
```

### Reading native logs on Linux

The Linux native layer writes to `stderr` with a `[BLE]` prefix. Redirect or capture it as
needed:

```bash
# Capture stderr to a file while running your application
java -jar your-app.jar 2>ble-native.log

# Tail it in another terminal
tail -f ble-native.log | grep '\[BLE\]'
```

BlueZ D-Bus activity can additionally be traced with `btmon` (requires root or `CAP_NET_RAW`):

```bash
sudo btmon -w ble-trace.btsnoop   # capture HCI traffic to a file
btmon -r ble-trace.btsnoop        # replay/inspect the capture
```

### Reading native logs on Windows

The Windows native layer writes to `stderr` with a `[BLE]` prefix. Capture it the same way as
on Linux. For deeper WinRT-level tracing use the Windows Performance Analyzer (WPA) or
ETW (Event Tracing for Windows) with the `Microsoft-Windows-Bluetooth-BTHPORT` provider:

```powershell
# Start an ETW capture (run as Administrator)
logman start BLETrace -p "Microsoft-Windows-Bluetooth-BTHPORT" -o ble-trace.etl -ets

# ... reproduce the problem ...

logman stop BLETrace -ets
# Open ble-trace.etl in Windows Performance Analyzer
```

---

## Building from Source

```bash
# Compile and run all unit tests
mvn verify

# Generate the coverage report (target/site/jacoco/index.html)
mvn jacoco:report

# Generate Javadoc (target/site/apidocs/index.html)
mvn javadoc:jar
```

---

## Quality Gates

Every build enforces the following quality rules:

| Tool | Rule |
|---|---|
| **JaCoCo** | ≥ 99 % instruction coverage, 100 % branch coverage |
| **Checkstyle** | Project-defined rule set (`varani_java_checks.xml`) |
| **SonarQube** | Zero blocker or critical issues; Quality Gate = Passed |

A build that violates any of these rules **fails** and must not be merged.

---

## Architecture

```
src/
  main/java/ch/varani/bricks/ble/
    api/            # Public interfaces and value types (no BLE stack dependency)
    api/dsl/        # Fluent DSL built on top of api/
    impl/           # Platform-specific BLE adapter implementations
      macos/        # CoreBluetooth bridge via JNI / FFM
      windows/      # WinRT Bluetooth LE via JNI / FFM
      linux/        # D-Bus / BlueZ bridge via JNI / FFM
    device/         # Per-brand protocol constants and implementations
      lego/         # LEGO Wireless Protocol 3.0
      sbrick/       # SBrick BLE protocol
      fxbricks/     # FX Bricks BLE protocol
      circuitcubes/ # Circuit Cubes BLE protocol
      buwizz/       # BuWizz 2.0 and BuWizz 3.0 BLE protocol
    util/           # Internal helpers (not part of the public API)
  test/java/ch/varani/bricks/ble/
    # Mirrors the main tree; every production class has a corresponding test class
```

The `api` and `api/dsl` packages are the only packages that library consumers depend on. All
other packages are internal and subject to change without notice.

---

## Contributing

1. Fork the repository and create a feature branch.
2. Write code and tests together — coverage must remain at the thresholds above.
3. Run `mvn verify` locally and confirm all checks pass.
4. Open a pull request; CI must be green before review.

See [AGENTS.md](AGENTS.md) for the full set of coding standards and agent guidelines.

---

## Acknowledgements

- [**node-poweredup**](https://github.com/nathankellenicki/node-poweredup) by Nathan Kellenicki
  (MIT licence) — a community JavaScript implementation of the LEGO Wireless Protocol 3.0 and
  related protocols. Several LEGO protocol constants in this library were cross-referenced
  against node-poweredup's `src/consts.ts` to verify and supplement the official LEGO BLE
  Wireless Protocol 3.0 specification.

---

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
