/*
 * BleBridge.c — Linux BlueZ BLE bridge implementation.
 *
 * Uses GLib / GDBus to communicate with BlueZ over the D-Bus system bus.
 * A dedicated GLib main loop thread handles all D-Bus signal callbacks.
 * JNI callbacks into Java fire from that thread after
 * AttachCurrentThreadAsDaemon / DetachCurrentThread guards.
 *
 * BlueZ D-Bus interfaces used:
 *   org.freedesktop.DBus.ObjectManager  — GetManagedObjects, InterfacesAdded
 *   org.bluez.Adapter1                  — StartDiscovery, StopDiscovery
 *   org.bluez.Device1                   — Connect, Disconnect
 *   org.bluez.GattService1              — (discovery only, via GetManagedObjects)
 *   org.bluez.GattCharacteristic1       — ReadValue, WriteValue,
 *                                         StartNotify, StopNotify
 *   org.freedesktop.DBus.Properties     — PropertiesChanged (notifications)
 */

#include "BleBridge.h"

#include <gio/gio.h>
#include <glib.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

/* ──────────────────────────────────────────────────────────────────────────
   Constants
   ────────────────────────────────────────────────────────────────────────── */

#define BLUEZ_BUS_NAME          "org.bluez"
#define BLUEZ_ADAPTER_IFACE     "org.bluez.Adapter1"
#define BLUEZ_DEVICE_IFACE      "org.bluez.Device1"
#define BLUEZ_GATT_SVC_IFACE    "org.bluez.GattService1"
#define BLUEZ_GATT_CHAR_IFACE   "org.bluez.GattCharacteristic1"
#define DBUS_OM_IFACE           "org.freedesktop.DBus.ObjectManager"
#define DBUS_PROPS_IFACE        "org.freedesktop.DBus.Properties"
#define BLUEZ_ADAPTER_PATH      "/org/bluez/hci0"

/* ──────────────────────────────────────────────────────────────────────────
   Structs
   ────────────────────────────────────────────────────────────────────────── */

/**
 * Holds all state needed for a single BLE scanner context.
 * Heap-allocated in nativeInit; freed in nativeDestroy.
 */
typedef struct BleContext {
    GDBusConnection *bus;       /* D-Bus system bus connection              */
    GMainLoop       *loop;      /* GLib main loop driving all D-Bus signals */
    GThread         *loopThread;/* Thread running the GLib main loop        */
    JavaVM          *jvm;       /* Cached JVM pointer for AttachCurrentThread */
    jobject          callbacks; /* JNI global ref to LinuxBleNativeCallbacks */
    jmethodID        onDeviceFoundMid; /* LinuxBleNativeCallbacks.onDeviceFound */
    jmethodID        onNotificationMid;/* LinuxBleNativeCallbacks.onNotification */
    GMutex           initMutex; /* Protects initDone                        */
    GCond            initCond;  /* Signalled when loop is running           */
    gboolean         initDone;  /* Set to TRUE once the loop is ready       */
    gboolean         scanning;  /* Whether StartDiscovery was called        */
    guint            ifaceAddedSub; /* D-Bus signal subscription ID         */
} BleContext;

/**
 * Key used to map (serviceUuid, characteristicUuid) → D-Bus object path.
 */
typedef struct CharKey {
    char *serviceUuid;
    char *characteristicUuid;
} CharKey;

/**
 * Holds all state for a single GATT connection to a remote device.
 * Heap-allocated in nativeConnect; freed in nativeDisconnect.
 */
typedef struct BleConnectionContext {
    char        *devicePath;    /* BlueZ D-Bus object path of the device    */
    BleContext  *ctx;           /* Back-pointer to the owning BleContext     */
    GMutex       opMutex;       /* Protects lastOpResult / opDone            */
    GCond        opCond;        /* Signalled when an async D-Bus op finishes */
    GByteArray  *lastReadValue; /* Buffer for the last ReadValue result      */
    gboolean     lastOpError;   /* TRUE if the last op returned an error     */
    gboolean     opDone;        /* Set to TRUE when async op completes       */
    GHashTable  *charPaths;     /* CharKey* → gchar* (characteristic D-Bus path) */
    GHashTable  *notifySubs;    /* gchar* (char path) → guint (signal sub ID) */
    jlong        selfPtr;       /* This pointer cast to jlong (for callbacks) */
} BleConnectionContext;

/* ──────────────────────────────────────────────────────────────────────────
   Internal helpers — JNI thread attach/detach
   ────────────────────────────────────────────────────────────────────────── */

/**
 * Attaches the calling thread to the JVM as a daemon thread and returns a
 * valid JNIEnv*.  Must be paired with detach_thread().
 *
 * @param jvm the JavaVM obtained during nativeInit
 * @return JNIEnv* for the current thread, or NULL on failure
 */
static JNIEnv *attach_thread(JavaVM *jvm) {
    JNIEnv *env = NULL;
    (*jvm)->AttachCurrentThreadAsDaemon(jvm, (void **) &env, NULL);
    return env;
}

/**
 * Detaches the calling thread from the JVM.
 *
 * @param jvm the JavaVM obtained during nativeInit
 */
static void detach_thread(JavaVM *jvm) {
    (*jvm)->DetachCurrentThread(jvm);
}

/* ──────────────────────────────────────────────────────────────────────────
   Internal helpers — CharKey hash table
   ────────────────────────────────────────────────────────────────────────── */

/** Hashes a CharKey by combining service UUID and characteristic UUID. */
static guint char_key_hash(gconstpointer k) {
    const CharKey *key = (const CharKey *) k;
    return g_str_hash(key->serviceUuid) ^ g_str_hash(key->characteristicUuid);
}

/** Compares two CharKey values for equality. */
static gboolean char_key_equal(gconstpointer a, gconstpointer b) {
    const CharKey *ka = (const CharKey *) a;
    const CharKey *kb = (const CharKey *) b;
    return g_str_equal(ka->serviceUuid, kb->serviceUuid)
        && g_str_equal(ka->characteristicUuid, kb->characteristicUuid);
}

/** Frees a heap-allocated CharKey. */
static void char_key_free(gpointer k) {
    CharKey *key = (CharKey *) k;
    g_free(key->serviceUuid);
    g_free(key->characteristicUuid);
    g_free(key);
}

/* ──────────────────────────────────────────────────────────────────────────
   Internal helpers — characteristic path lookup
   ────────────────────────────────────────────────────────────────────────── */

/**
 * Queries org.freedesktop.DBus.ObjectManager.GetManagedObjects on BlueZ and
 * populates conn->charPaths with (serviceUuid, charUuid) → D-Bus path entries
 * for all characteristics belonging to the given device.
 *
 * @param conn the connection context to populate
 */
static void discover_characteristics(BleConnectionContext *conn) {
    GError  *err    = NULL;
    GVariant *reply = g_dbus_connection_call_sync(
            conn->ctx->bus,
            BLUEZ_BUS_NAME,
            "/",                        /* ObjectManager is at root path */
            DBUS_OM_IFACE,
            "GetManagedObjects",
            NULL,
            G_VARIANT_TYPE("(a{oa{sa{sv}}})"),
            G_DBUS_CALL_FLAGS_NONE,
            -1,
            NULL,
            &err);

    if (err != NULL) {
        g_error_free(err);
        return;
    }

    GVariant    *objects   = g_variant_get_child_value(reply, 0);
    GVariantIter obj_iter;
    g_variant_iter_init(&obj_iter, objects);

    const gchar *obj_path;
    GVariant    *iface_dict;

    while (g_variant_iter_next(&obj_iter, "{&o@a{sa{sv}}}", &obj_path, &iface_dict)) {
        /* Only consider objects under the device D-Bus path */
        if (!g_str_has_prefix(obj_path, conn->devicePath)) {
            g_variant_unref(iface_dict);
            continue;
        }

        GVariant *char_props = g_variant_lookup_value(
                iface_dict, BLUEZ_GATT_CHAR_IFACE, G_VARIANT_TYPE("a{sv}"));
        if (char_props == NULL) {
            g_variant_unref(iface_dict);
            continue;
        }

        /* Read the UUID and Service properties of this characteristic */
        GVariant *uuid_v    = g_variant_lookup_value(char_props, "UUID",
                G_VARIANT_TYPE_STRING);
        GVariant *service_v = g_variant_lookup_value(char_props, "Service",
                G_VARIANT_TYPE_OBJECT_PATH);

        if (uuid_v != NULL && service_v != NULL) {
            const gchar *char_uuid    = g_variant_get_string(uuid_v, NULL);
            const gchar *service_path = g_variant_get_string(service_v, NULL);

            /* Look up the service UUID from the service path */
            GVariant *svc_ifaces = g_variant_lookup_value(
                    objects, service_path,
                    G_VARIANT_TYPE("a{sa{sv}}"));
            if (svc_ifaces != NULL) {
                GVariant *svc_props = g_variant_lookup_value(
                        svc_ifaces, BLUEZ_GATT_SVC_IFACE,
                        G_VARIANT_TYPE("a{sv}"));
                if (svc_props != NULL) {
                    GVariant *svc_uuid_v = g_variant_lookup_value(
                            svc_props, "UUID", G_VARIANT_TYPE_STRING);
                    if (svc_uuid_v != NULL) {
                        const gchar *svc_uuid = g_variant_get_string(svc_uuid_v, NULL);

                        CharKey *key = g_new(CharKey, 1);
                        key->serviceUuid        = g_utf8_strdown(svc_uuid, -1);
                        key->characteristicUuid = g_utf8_strdown(char_uuid, -1);

                        g_hash_table_insert(conn->charPaths, key,
                                g_strdup(obj_path));

                        g_variant_unref(svc_uuid_v);
                    }
                    g_variant_unref(svc_props);
                }
                g_variant_unref(svc_ifaces);
            }
        }

        if (uuid_v    != NULL) { g_variant_unref(uuid_v); }
        if (service_v != NULL) { g_variant_unref(service_v); }
        g_variant_unref(char_props);
        g_variant_unref(iface_dict);
    }

    g_variant_unref(objects);
    g_variant_unref(reply);
}

/**
 * Looks up the D-Bus object path of a GATT characteristic given its service
 * UUID and characteristic UUID.
 *
 * @param conn           the connection context
 * @param serviceUuid    service UUID (UTF-8, lower-case)
 * @param charUuid       characteristic UUID (UTF-8, lower-case)
 * @return the D-Bus object path, or NULL if not found
 */
static const gchar *lookup_char_path(BleConnectionContext *conn,
                                     const gchar *serviceUuid,
                                     const gchar *charUuid) {
    CharKey key;
    /* cast away const for lookup — hash table only reads the key */
    key.serviceUuid        = (gchar *) serviceUuid;
    key.characteristicUuid = (gchar *) charUuid;
    return (const gchar *) g_hash_table_lookup(conn->charPaths, &key);
}

/* ──────────────────────────────────────────────────────────────────────────
   PropertiesChanged signal handler — GATT notifications
   ────────────────────────────────────────────────────────────────────────── */

/**
 * Callback invoked by GDBus when a PropertiesChanged signal arrives on a
 * GattCharacteristic1 object.  Extracts the "Value" property change and
 * calls LinuxBleNativeCallbacks.onNotification() on the Java side.
 *
 * D-Bus signal: org.freedesktop.DBus.Properties.PropertiesChanged
 * Sender:       org.bluez
 * Interface:    org.bluez.GattCharacteristic1
 * Signal body:  (sa{sv}as)  interface, changed_props, invalidated
 *
 * @param connection   D-Bus connection (unused)
 * @param sender_name  sender bus name (unused)
 * @param object_path  D-Bus object path of the characteristic
 * @param interface_name interface that emitted the signal (unused)
 * @param signal_name  signal name (unused)
 * @param parameters   GVariant tuple (sa{sv}as)
 * @param user_data    pointer to BleConnectionContext
 */
static void on_properties_changed(GDBusConnection  *connection,
                                  const gchar      *sender_name,
                                  const gchar      *object_path,
                                  const gchar      *interface_name,
                                  const gchar      *signal_name,
                                  GVariant         *parameters,
                                  gpointer          user_data) {
    (void) connection;
    (void) sender_name;
    (void) interface_name;
    (void) signal_name;

    BleConnectionContext *conn = (BleConnectionContext *) user_data;

    GVariant *iface_v       = g_variant_get_child_value(parameters, 0);
    GVariant *changed_props = g_variant_get_child_value(parameters, 1);

    const gchar *iface = g_variant_get_string(iface_v, NULL);
    if (!g_str_equal(iface, BLUEZ_GATT_CHAR_IFACE)) {
        g_variant_unref(iface_v);
        g_variant_unref(changed_props);
        return;
    }

    GVariant *value_v = g_variant_lookup_value(changed_props, "Value",
            G_VARIANT_TYPE_BYTESTRING);
    if (value_v == NULL) {
        /* Also check ay (array of bytes) as BlueZ may use either */
        value_v = g_variant_lookup_value(changed_props, "Value",
                G_VARIANT_TYPE("ay"));
    }

    if (value_v != NULL) {
        gsize   len  = 0;
        const guint8 *raw = g_variant_get_fixed_array(value_v, &len,
                sizeof(guint8));

        /* Find the service + characteristic UUIDs for this object path */
        const gchar *svc_uuid  = NULL;
        const gchar *char_uuid = NULL;
        GHashTableIter it;
        gpointer key_ptr, val_ptr;
        g_hash_table_iter_init(&it, conn->charPaths);
        while (g_hash_table_iter_next(&it, &key_ptr, &val_ptr)) {
            if (g_str_equal((const gchar *) val_ptr, object_path)) {
                CharKey *k = (CharKey *) key_ptr;
                svc_uuid   = k->serviceUuid;
                char_uuid  = k->characteristicUuid;
                break;
            }
        }

        if (svc_uuid != NULL && char_uuid != NULL) {
            JavaVM *jvm = conn->ctx->jvm;
            JNIEnv *env = attach_thread(jvm);
            if (env != NULL) {
                jbyteArray jval = (*env)->NewByteArray(env, (jsize) len);
                if (jval != NULL && raw != NULL) {
                    (*env)->SetByteArrayRegion(env, jval, 0, (jsize) len,
                            (const jbyte *) raw);
                }
                jstring jsvc  = (*env)->NewStringUTF(env, svc_uuid);
                jstring jchr  = (*env)->NewStringUTF(env, char_uuid);
                (*env)->CallVoidMethod(env,
                        conn->ctx->callbacks,
                        conn->ctx->onNotificationMid,
                        conn->selfPtr,
                        jsvc, jchr, jval);
                if (jval  != NULL) { (*env)->DeleteLocalRef(env, jval); }
                if (jsvc  != NULL) { (*env)->DeleteLocalRef(env, jsvc); }
                if (jchr  != NULL) { (*env)->DeleteLocalRef(env, jchr); }
                detach_thread(jvm);
            }
        }

        g_variant_unref(value_v);
    }

    g_variant_unref(iface_v);
    g_variant_unref(changed_props);
}

/* ──────────────────────────────────────────────────────────────────────────
   InterfacesAdded signal handler — device discovery
   ────────────────────────────────────────────────────────────────────────── */

/**
 * Callback invoked by GDBus when a new D-Bus object is added while scanning.
 * If the object implements org.bluez.Device1, calls
 * LinuxBleNativeCallbacks.onDeviceFound() on the Java side.
 *
 * D-Bus signal: org.freedesktop.DBus.ObjectManager.InterfacesAdded
 * Sender:       org.bluez
 * Signal body:  (oa{sa{sv}})  object_path, interfaces_and_props
 *
 * @param connection   D-Bus connection (unused)
 * @param sender_name  sender bus name (unused)
 * @param object_path  unused (path is inside parameters)
 * @param interface_name interface (unused)
 * @param signal_name  signal name (unused)
 * @param parameters   GVariant tuple (oa{sa{sv}})
 * @param user_data    pointer to BleContext
 */
static void on_interfaces_added(GDBusConnection  *connection,
                                const gchar      *sender_name,
                                const gchar      *object_path,
                                const gchar      *interface_name,
                                const gchar      *signal_name,
                                GVariant         *parameters,
                                gpointer          user_data) {
    (void) connection;
    (void) sender_name;
    (void) object_path;
    (void) interface_name;
    (void) signal_name;

    BleContext *ctx = (BleContext *) user_data;

    GVariant *path_v      = g_variant_get_child_value(parameters, 0);
    GVariant *ifaces_dict = g_variant_get_child_value(parameters, 1);

    const gchar *dev_path = g_variant_get_string(path_v, NULL);

    GVariant *dev_props = g_variant_lookup_value(ifaces_dict,
            BLUEZ_DEVICE_IFACE, G_VARIANT_TYPE("a{sv}"));
    if (dev_props != NULL) {
        GVariant *name_v = g_variant_lookup_value(dev_props, "Name",
                G_VARIANT_TYPE_STRING);
        GVariant *rssi_v = g_variant_lookup_value(dev_props, "RSSI",
                G_VARIANT_TYPE_INT16);

        const gchar *name = (name_v != NULL)
                ? g_variant_get_string(name_v, NULL)
                : "";
        gint16 rssi = (rssi_v != NULL)
                ? g_variant_get_int16(rssi_v)
                : 0;

        JavaVM *jvm = ctx->jvm;
        JNIEnv *env = attach_thread(jvm);
        if (env != NULL) {
            /*
             * Extract manufacturer-specific advertisement data.
             * BlueZ exposes this as the "ManufacturerData" property on
             * org.bluez.Device1, whose D-Bus type is a{qv} (map from
             * uint16 company ID to a variant holding an ay byte array).
             * We flatten all entries into a single byte[] in wire order:
             * 2-byte little-endian company ID followed by the payload bytes.
             * If the property is absent, we pass an empty byte array.
             */
            jbyteArray jmfr = NULL;

            GVariant *mfr_map = g_variant_lookup_value(dev_props,
                    "ManufacturerData", G_VARIANT_TYPE("a{qv}"));
            if (mfr_map != NULL) {
                GByteArray *flat = g_byte_array_new();
                GVariantIter mfr_iter;
                g_variant_iter_init(&mfr_iter, mfr_map);
                guint16   company_id;
                GVariant *data_v;
                while (g_variant_iter_next(&mfr_iter, "{qv}",
                                           &company_id, &data_v)) {
                    /* Append company ID in little-endian order */
                    guint8 lo = (guint8) (company_id & 0xFF);
                    guint8 hi = (guint8) ((company_id >> 8) & 0xFF);
                    g_byte_array_append(flat, &lo, 1);
                    g_byte_array_append(flat, &hi, 1);
                    /* Append payload bytes (variant holds ay) */
                    GVariant *inner = g_variant_get_variant(data_v);
                    gsize payload_len = 0;
                    const guint8 *payload = g_variant_get_fixed_array(
                            inner, &payload_len, sizeof(guint8));
                    if (payload != NULL && payload_len > 0) {
                        g_byte_array_append(flat, payload, (guint) payload_len);
                    }
                    g_variant_unref(inner);
                    g_variant_unref(data_v);
                }
                jmfr = (*env)->NewByteArray(env, (jsize) flat->len);
                if (jmfr != NULL && flat->len > 0) {
                    (*env)->SetByteArrayRegion(env, jmfr, 0,
                            (jsize) flat->len, (const jbyte *) flat->data);
                }
                g_byte_array_free(flat, TRUE);
                g_variant_unref(mfr_map);
            } else {
                jmfr = (*env)->NewByteArray(env, 0);
            }

            jstring jpath = (*env)->NewStringUTF(env, dev_path);
            jstring jname = (*env)->NewStringUTF(env, name);
            (*env)->CallVoidMethod(env, ctx->callbacks,
                    ctx->onDeviceFoundMid,
                    jpath, jname, (jint) rssi, jmfr);
            if (jpath != NULL) { (*env)->DeleteLocalRef(env, jpath); }
            if (jname != NULL) { (*env)->DeleteLocalRef(env, jname); }
            if (jmfr  != NULL) { (*env)->DeleteLocalRef(env, jmfr);  }
            detach_thread(jvm);
        }

        if (name_v != NULL) { g_variant_unref(name_v); }
        if (rssi_v != NULL) { g_variant_unref(rssi_v); }
        g_variant_unref(dev_props);
    }

    g_variant_unref(path_v);
    g_variant_unref(ifaces_dict);
}

/* ──────────────────────────────────────────────────────────────────────────
   GLib main-loop thread entry
   ────────────────────────────────────────────────────────────────────────── */

/**
 * Entry point for the GLib main-loop thread.
 * Signals ctx->initCond once the loop is running, then blocks in
 * g_main_loop_run() until g_main_loop_quit() is called from nativeDestroy.
 *
 * @param data pointer to BleContext
 * @return NULL
 */
static gpointer run_main_loop(gpointer data) {
    BleContext *ctx = (BleContext *) data;

    /* Signal that the loop is ready */
    g_mutex_lock(&ctx->initMutex);
    ctx->initDone = TRUE;
    g_cond_signal(&ctx->initCond);
    g_mutex_unlock(&ctx->initMutex);

    g_main_loop_run(ctx->loop);
    return NULL;
}

/* ──────────────────────────────────────────────────────────────────────────
   JNI — nativeInit
   ────────────────────────────────────────────────────────────────────────── */

JNIEXPORT jlong JNICALL
Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeInit(
        JNIEnv *env, jobject obj, jobject callbacks) {
    (void) obj;

    BleContext *ctx = g_new0(BleContext, 1);

    /* Cache the JavaVM */
    (*env)->GetJavaVM(env, &ctx->jvm);

    /* Store a global reference to the callbacks object */
    ctx->callbacks = (*env)->NewGlobalRef(env, callbacks);

    /* Look up the two callback method IDs */
    jclass cbClass = (*env)->GetObjectClass(env, callbacks);
    ctx->onDeviceFoundMid = (*env)->GetMethodID(env, cbClass,
            "onDeviceFound",
            "(Ljava/lang/String;Ljava/lang/String;I[B)V");
    ctx->onNotificationMid = (*env)->GetMethodID(env, cbClass,
            "onNotification",
            "(JLjava/lang/String;Ljava/lang/String;[B)V");

    /* Connect to the D-Bus system bus */
    GError *err = NULL;
    ctx->bus = g_bus_get_sync(G_BUS_TYPE_SYSTEM, NULL, &err);
    if (err != NULL) {
        g_error_free(err);
        (*env)->DeleteGlobalRef(env, ctx->callbacks);
        g_free(ctx);
        return 0L;
    }

    /* Create the GLib main loop */
    ctx->loop = g_main_loop_new(NULL, FALSE);

    /* Initialise synchronisation primitives */
    g_mutex_init(&ctx->initMutex);
    g_cond_init(&ctx->initCond);
    ctx->initDone = FALSE;

    /* Start the GLib main-loop thread */
    ctx->loopThread = g_thread_new("ble-linux-glib", run_main_loop, ctx);

    /* Wait for the loop to be running */
    g_mutex_lock(&ctx->initMutex);
    while (!ctx->initDone) {
        g_cond_wait(&ctx->initCond, &ctx->initMutex);
    }
    g_mutex_unlock(&ctx->initMutex);

    return (jlong) (intptr_t) ctx;
}

/* ──────────────────────────────────────────────────────────────────────────
   JNI — nativeStartScan
   ────────────────────────────────────────────────────────────────────────── */

JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeStartScan(
        JNIEnv *env, jobject obj, jlong ctxPtr, jstring serviceUuid) {
    (void) obj;
    (void) serviceUuid;  /* filter not applied at adapter level in this impl */

    BleContext *ctx = (BleContext *) (intptr_t) ctxPtr;
    if (ctx == NULL) { return; }

    /* Subscribe to InterfacesAdded signals to receive device advertisements */
    ctx->ifaceAddedSub = g_dbus_connection_signal_subscribe(
            ctx->bus,
            BLUEZ_BUS_NAME,
            DBUS_OM_IFACE,
            "InterfacesAdded",
            NULL,   /* any object path */
            NULL,   /* no arg0 filter */
            G_DBUS_SIGNAL_FLAGS_NONE,
            on_interfaces_added,
            ctx,
            NULL);

    /* Call org.bluez.Adapter1.StartDiscovery */
    GError *err = NULL;
    g_dbus_connection_call_sync(
            ctx->bus,
            BLUEZ_BUS_NAME,
            BLUEZ_ADAPTER_PATH,
            BLUEZ_ADAPTER_IFACE,
            "StartDiscovery",
            NULL,
            NULL,
            G_DBUS_CALL_FLAGS_NONE,
            -1,
            NULL,
            &err);
    if (err != NULL) {
        g_error_free(err);
    } else {
        ctx->scanning = TRUE;
    }

    (void) env;
}

/* ──────────────────────────────────────────────────────────────────────────
   JNI — nativeStopScan
   ────────────────────────────────────────────────────────────────────────── */

JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeStopScan(
        JNIEnv *env, jobject obj, jlong ctxPtr) {
    (void) env;
    (void) obj;

    BleContext *ctx = (BleContext *) (intptr_t) ctxPtr;
    if (ctx == NULL || !ctx->scanning) { return; }

    /* Unsubscribe from InterfacesAdded */
    if (ctx->ifaceAddedSub != 0) {
        g_dbus_connection_signal_unsubscribe(ctx->bus, ctx->ifaceAddedSub);
        ctx->ifaceAddedSub = 0;
    }

    /* Call org.bluez.Adapter1.StopDiscovery */
    GError *err = NULL;
    g_dbus_connection_call_sync(
            ctx->bus,
            BLUEZ_BUS_NAME,
            BLUEZ_ADAPTER_PATH,
            BLUEZ_ADAPTER_IFACE,
            "StopDiscovery",
            NULL,
            NULL,
            G_DBUS_CALL_FLAGS_NONE,
            -1,
            NULL,
            &err);
    if (err != NULL) {
        g_error_free(err);
    }
    ctx->scanning = FALSE;
}

/* ──────────────────────────────────────────────────────────────────────────
   JNI — nativeIsScanning
   ────────────────────────────────────────────────────────────────────────── */

JNIEXPORT jboolean JNICALL
Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeIsScanning(
        JNIEnv *env, jobject obj, jlong ctxPtr) {
    (void) env;
    (void) obj;

    const BleContext *ctx = (const BleContext *) (intptr_t) ctxPtr;
    if (ctx == NULL) { return JNI_FALSE; }
    return ctx->scanning ? JNI_TRUE : JNI_FALSE;
}

/* ──────────────────────────────────────────────────────────────────────────
   JNI — nativeConnect
   ────────────────────────────────────────────────────────────────────────── */

JNIEXPORT jlong JNICALL
Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeConnect(
        JNIEnv *env, jobject obj, jlong ctxPtr, jstring devicePath) {
    (void) obj;

    BleContext *ctx = (BleContext *) (intptr_t) ctxPtr;
    if (ctx == NULL || devicePath == NULL) { return 0L; }

    const char *path = (*env)->GetStringUTFChars(env, devicePath, NULL);
    if (path == NULL) { return 0L; }

    /* Call org.bluez.Device1.Connect */
    GError *err = NULL;
    g_dbus_connection_call_sync(
            ctx->bus,
            BLUEZ_BUS_NAME,
            path,
            BLUEZ_DEVICE_IFACE,
            "Connect",
            NULL,
            NULL,
            G_DBUS_CALL_FLAGS_NONE,
            30000,   /* 30-second timeout */
            NULL,
            &err);

    if (err != NULL) {
        g_error_free(err);
        (*env)->ReleaseStringUTFChars(env, devicePath, path);
        return 0L;
    }

    /* Allocate and populate the connection context */
    BleConnectionContext *conn = g_new0(BleConnectionContext, 1);
    conn->devicePath = g_strdup(path);
    conn->ctx        = ctx;
    g_mutex_init(&conn->opMutex);
    g_cond_init(&conn->opCond);
    conn->charPaths  = g_hash_table_new_full(char_key_hash, char_key_equal,
            char_key_free, g_free);
    conn->notifySubs = g_hash_table_new_full(g_str_hash, g_str_equal,
            g_free, NULL);
    conn->selfPtr    = (jlong) (intptr_t) conn;

    (*env)->ReleaseStringUTFChars(env, devicePath, path);

    /* Discover GATT services and characteristics */
    discover_characteristics(conn);

    return conn->selfPtr;
}

/* ──────────────────────────────────────────────────────────────────────────
   JNI — nativeDisconnect
   ────────────────────────────────────────────────────────────────────────── */

JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeDisconnect(
        JNIEnv *env, jobject obj, jlong ctxPtr, jlong connectionPtr) {
    (void) obj;

    BleContext           *ctx  = (BleContext *) (intptr_t) ctxPtr;
    BleConnectionContext *conn = (BleConnectionContext *) (intptr_t) connectionPtr;
    if (ctx == NULL || conn == NULL) { return; }

    /* Unsubscribe all notification signal subscriptions */
    GHashTableIter it;
    gpointer key_ptr, val_ptr;
    g_hash_table_iter_init(&it, conn->notifySubs);
    while (g_hash_table_iter_next(&it, &key_ptr, &val_ptr)) {
        guint sub_id = GPOINTER_TO_UINT(val_ptr);
        g_dbus_connection_signal_unsubscribe(ctx->bus, sub_id);
    }

    /* Call org.bluez.Device1.Disconnect */
    GError *err = NULL;
    g_dbus_connection_call_sync(
            ctx->bus,
            BLUEZ_BUS_NAME,
            conn->devicePath,
            BLUEZ_DEVICE_IFACE,
            "Disconnect",
            NULL,
            NULL,
            G_DBUS_CALL_FLAGS_NONE,
            -1,
            NULL,
            &err);
    if (err != NULL) {
        g_error_free(err);
    }

    /* Free resources */
    g_hash_table_destroy(conn->charPaths);
    g_hash_table_destroy(conn->notifySubs);
    if (conn->lastReadValue != NULL) {
        g_byte_array_free(conn->lastReadValue, TRUE);
    }
    g_mutex_clear(&conn->opMutex);
    g_cond_clear(&conn->opCond);
    g_free(conn->devicePath);
    g_free(conn);

    (void) env;
}

/* ──────────────────────────────────────────────────────────────────────────
   JNI — nativeWriteWithoutResponse
   ────────────────────────────────────────────────────────────────────────── */

JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeWriteWithoutResponse(
        JNIEnv *env, jobject obj, jlong connectionPtr,
        jstring serviceUuid, jstring characteristicUuid, jbyteArray data) {
    (void) obj;

    BleConnectionContext *conn = (BleConnectionContext *) (intptr_t) connectionPtr;
    if (conn == NULL || data == NULL) { return; }

    const char *svc_uuid  = (*env)->GetStringUTFChars(env, serviceUuid, NULL);
    const char *char_uuid = (*env)->GetStringUTFChars(env, characteristicUuid, NULL);
    if (svc_uuid == NULL || char_uuid == NULL) {
        if (svc_uuid  != NULL) { (*env)->ReleaseStringUTFChars(env, serviceUuid,  svc_uuid);  }
        if (char_uuid != NULL) { (*env)->ReleaseStringUTFChars(env, characteristicUuid, char_uuid); }
        return;
    }

    /* Normalise UUIDs to lower-case for lookup */
    gchar *svc_lower  = g_utf8_strdown(svc_uuid,  -1);
    gchar *char_lower = g_utf8_strdown(char_uuid, -1);

    const gchar *char_path = lookup_char_path(conn, svc_lower, char_lower);

    g_free(svc_lower);
    g_free(char_lower);
    (*env)->ReleaseStringUTFChars(env, serviceUuid,        svc_uuid);
    (*env)->ReleaseStringUTFChars(env, characteristicUuid, char_uuid);

    if (char_path == NULL) { return; }

    /* Build the byte payload as a GVariant ay */
    jsize len     = (*env)->GetArrayLength(env, data);
    jbyte *bytes  = (*env)->GetByteArrayElements(env, data, NULL);
    GVariant *payload = g_variant_new_fixed_array(G_VARIANT_TYPE_BYTE,
            bytes, (gsize) len, sizeof(guint8));

    /* Build options dict — write-without-response = true */
    GVariantBuilder opts;
    g_variant_builder_init(&opts, G_VARIANT_TYPE("a{sv}"));
    g_variant_builder_add(&opts, "{sv}", "type",
            g_variant_new_string("command"));

    GError *err = NULL;
    g_dbus_connection_call_sync(
            conn->ctx->bus,
            BLUEZ_BUS_NAME,
            char_path,
            BLUEZ_GATT_CHAR_IFACE,
            "WriteValue",
            g_variant_new("(@aya{sv})", payload,
                    g_variant_builder_end(&opts)),
            NULL,
            G_DBUS_CALL_FLAGS_NONE,
            -1,
            NULL,
            &err);
    if (err != NULL) {
        g_error_free(err);
    }

    (*env)->ReleaseByteArrayElements(env, data, bytes, JNI_ABORT);
}

/* ──────────────────────────────────────────────────────────────────────────
   JNI — nativeReadCharacteristic
   ────────────────────────────────────────────────────────────────────────── */

JNIEXPORT jbyteArray JNICALL
Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeReadCharacteristic(
        JNIEnv *env, jobject obj, jlong connectionPtr,
        jstring serviceUuid, jstring characteristicUuid) {
    (void) obj;

    BleConnectionContext *conn = (BleConnectionContext *) (intptr_t) connectionPtr;
    if (conn == NULL) { return NULL; }

    const char *svc_uuid  = (*env)->GetStringUTFChars(env, serviceUuid, NULL);
    const char *char_uuid = (*env)->GetStringUTFChars(env, characteristicUuid, NULL);
    if (svc_uuid == NULL || char_uuid == NULL) {
        if (svc_uuid  != NULL) { (*env)->ReleaseStringUTFChars(env, serviceUuid,  svc_uuid);  }
        if (char_uuid != NULL) { (*env)->ReleaseStringUTFChars(env, characteristicUuid, char_uuid); }
        return NULL;
    }

    gchar *svc_lower  = g_utf8_strdown(svc_uuid,  -1);
    gchar *char_lower = g_utf8_strdown(char_uuid, -1);

    const gchar *char_path = lookup_char_path(conn, svc_lower, char_lower);

    g_free(svc_lower);
    g_free(char_lower);
    (*env)->ReleaseStringUTFChars(env, serviceUuid,        svc_uuid);
    (*env)->ReleaseStringUTFChars(env, characteristicUuid, char_uuid);

    if (char_path == NULL) { return NULL; }

    /* Build empty options dict */
    GVariantBuilder opts;
    g_variant_builder_init(&opts, G_VARIANT_TYPE("a{sv}"));

    GError   *err   = NULL;
    GVariant *reply = g_dbus_connection_call_sync(
            conn->ctx->bus,
            BLUEZ_BUS_NAME,
            char_path,
            BLUEZ_GATT_CHAR_IFACE,
            "ReadValue",
            g_variant_new("(a{sv})", g_variant_builder_end(&opts)),
            G_VARIANT_TYPE("(ay)"),
            G_DBUS_CALL_FLAGS_NONE,
            -1,
            NULL,
            &err);
    if (err != NULL) {
        g_error_free(err);
        return NULL;
    }

    GVariant *bytes_v = g_variant_get_child_value(reply, 0);
    gsize     len     = 0;
    const guint8 *raw = g_variant_get_fixed_array(bytes_v, &len, sizeof(guint8));

    jbyteArray result = (*env)->NewByteArray(env, (jsize) len);
    if (result != NULL && raw != NULL) {
        (*env)->SetByteArrayRegion(env, result, 0, (jsize) len,
                (const jbyte *) raw);
    }

    g_variant_unref(bytes_v);
    g_variant_unref(reply);
    return result;
}

/* ──────────────────────────────────────────────────────────────────────────
   JNI — nativeSetNotify
   ────────────────────────────────────────────────────────────────────────── */

JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeSetNotify(
        JNIEnv *env, jobject obj, jlong connectionPtr,
        jstring serviceUuid, jstring characteristicUuid, jboolean enable) {
    (void) obj;

    BleConnectionContext *conn = (BleConnectionContext *) (intptr_t) connectionPtr;
    if (conn == NULL) { return; }

    const char *svc_uuid  = (*env)->GetStringUTFChars(env, serviceUuid, NULL);
    const char *char_uuid = (*env)->GetStringUTFChars(env, characteristicUuid, NULL);
    if (svc_uuid == NULL || char_uuid == NULL) {
        if (svc_uuid  != NULL) { (*env)->ReleaseStringUTFChars(env, serviceUuid,  svc_uuid);  }
        if (char_uuid != NULL) { (*env)->ReleaseStringUTFChars(env, characteristicUuid, char_uuid); }
        return;
    }

    gchar *svc_lower  = g_utf8_strdown(svc_uuid,  -1);
    gchar *char_lower = g_utf8_strdown(char_uuid, -1);

    const gchar *char_path = lookup_char_path(conn, svc_lower, char_lower);

    g_free(svc_lower);
    g_free(char_lower);
    (*env)->ReleaseStringUTFChars(env, serviceUuid,        svc_uuid);
    (*env)->ReleaseStringUTFChars(env, characteristicUuid, char_uuid);

    if (char_path == NULL) { return; }

    if (enable) {
        /* Subscribe to PropertiesChanged signals for this characteristic */
        guint sub_id = g_dbus_connection_signal_subscribe(
                conn->ctx->bus,
                BLUEZ_BUS_NAME,
                DBUS_PROPS_IFACE,
                "PropertiesChanged",
                char_path,
                BLUEZ_GATT_CHAR_IFACE,
                G_DBUS_SIGNAL_FLAGS_NONE,
                on_properties_changed,
                conn,
                NULL);
        g_hash_table_insert(conn->notifySubs,
                g_strdup(char_path), GUINT_TO_POINTER(sub_id));

        /* Call org.bluez.GattCharacteristic1.StartNotify */
        GError *err = NULL;
        g_dbus_connection_call_sync(
                conn->ctx->bus,
                BLUEZ_BUS_NAME,
                char_path,
                BLUEZ_GATT_CHAR_IFACE,
                "StartNotify",
                NULL,
                NULL,
                G_DBUS_CALL_FLAGS_NONE,
                -1,
                NULL,
                &err);
        if (err != NULL) { g_error_free(err); }
    } else {
        /* Unsubscribe the PropertiesChanged signal */
        gpointer sub_ptr = g_hash_table_lookup(conn->notifySubs, char_path);
        if (sub_ptr != NULL) {
            guint sub_id = GPOINTER_TO_UINT(sub_ptr);
            g_dbus_connection_signal_unsubscribe(conn->ctx->bus, sub_id);
            g_hash_table_remove(conn->notifySubs, char_path);
        }

        /* Call org.bluez.GattCharacteristic1.StopNotify */
        GError *err = NULL;
        g_dbus_connection_call_sync(
                conn->ctx->bus,
                BLUEZ_BUS_NAME,
                char_path,
                BLUEZ_GATT_CHAR_IFACE,
                "StopNotify",
                NULL,
                NULL,
                G_DBUS_CALL_FLAGS_NONE,
                -1,
                NULL,
                &err);
        if (err != NULL) { g_error_free(err); }
    }
}

/* ──────────────────────────────────────────────────────────────────────────
   JNI — nativeDestroy
   ────────────────────────────────────────────────────────────────────────── */

JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeDestroy(
        JNIEnv *env, jobject obj, jlong ctxPtr) {
    (void) obj;

    BleContext *ctx = (BleContext *) (intptr_t) ctxPtr;
    if (ctx == NULL) { return; }

    /* Stop the GLib main loop — unblocks run_main_loop() */
    g_main_loop_quit(ctx->loop);

    /* Wait for the loop thread to exit */
    g_thread_join(ctx->loopThread);

    /* Unsubscribe scan signal if still active */
    if (ctx->ifaceAddedSub != 0) {
        g_dbus_connection_signal_unsubscribe(ctx->bus, ctx->ifaceAddedSub);
        ctx->ifaceAddedSub = 0;
    }

    /* Release D-Bus connection */
    g_object_unref(ctx->bus);

    /* Release GLib main loop */
    g_main_loop_unref(ctx->loop);

    /* Release the JNI global ref to the callbacks object */
    (*env)->DeleteGlobalRef(env, ctx->callbacks);

    /* Destroy synchronisation primitives */
    g_mutex_clear(&ctx->initMutex);
    g_cond_clear(&ctx->initCond);

    g_free(ctx);
}
