/**
 * Fluent internal DSL built on top of the public BLE API.
 *
 * <p>This package exposes a set of builder types that let callers express
 * discovery, connection, and device-control operations as readable
 * method-chaining sequences without dealing with raw UUIDs, byte arrays,
 * or {@link java.util.concurrent.CompletableFuture} composition directly.
 *
 * <p>Entry point:
 * <pre>{@code
 * BrickDsl.open()
 *     .scan()
 *     .forLegoHubs()
 *     .first()
 *     .thenConnect()
 *     .asLego()
 *     .requestBatteryVoltage()
 *     .motor(0x00).startSpeed(80)
 *     .done();
 * }</pre>
 *
 * <p>All types in this package are part of the public API. Each DSL class
 * delegates exclusively to the interfaces defined in
 * {@link ch.varani.bricks.ble.api} and to the protocol constants in the
 * {@code device} packages. No direct dependency on any {@code impl} class
 * is permitted here.
 *
 * <p><b>DSL maintenance rule:</b> every change to the public API (
 * {@link ch.varani.bricks.ble.api.BleScanner},
 * {@link ch.varani.bricks.ble.api.BleConnection},
 * {@link ch.varani.bricks.ble.api.BleDevice}, or any protocol constant
 * class in {@code ch.varani.bricks.ble.device.*}) must be reflected in
 * the corresponding DSL type in this package before the change is considered
 * complete. See {@code AGENTS.md §16} for the full rule.
 */
@NullMarked
package ch.varani.bricks.ble.api.dsl;

import org.jspecify.annotations.NullMarked;
