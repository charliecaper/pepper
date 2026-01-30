package com.fzfstudio.eh.pepper.sdk

/**
 * Kotlin models parsed from JS SDK return values.
 *
 * These are kept as pure Kotlin models (no `external`) to avoid
 * syncing all JS declarations and to avoid external enum deprecation.
 */
data class UserInfo(
    /** User ID */
    val uid: Int?,
    /** Username */
    val name: String,
    /** Avatar (raw image data) */
    val avatar: String,
    /** Country */
    val country: String,
)

/**
 * Device connection status.
 */
enum class DeviceConnectType {
    None,
    Connecting,
    Connected,
    Disconnected,
    ConnectionFailed;

    companion object {
        fun fromString(value: String?): DeviceConnectType = when (value?.lowercase()) {
            "connecting" -> Connecting
            "connected" -> Connected
            "disconnected" -> Disconnected
            "connectionfailed" -> ConnectionFailed
            else -> None
        }
    }
}

/**
 * Device status information.
 */
data class DeviceStatus(
    /** Device serial number */
    val sn: String,
    /** Connection type */
    val connectType: DeviceConnectType,
    /** Whether the device is being worn */
    val isWearing: Boolean?,
    /** Battery level (0-100) */
    val batteryLevel: Int?,
    /** Whether the device is charging */
    val isCharging: Boolean?,
    /** Whether the device is in the charging case */
    val isInCase: Boolean?,
)

/**
 * Device model type.
 */
enum class DeviceModel {
    G1,
    G2,
    Ring1;

    companion object {
        fun fromString(value: String?): DeviceModel = when (value?.lowercase()) {
            "g2" -> G2
            "ring1" -> Ring1
            else -> G1
        }
    }
}

/**
 * Aggregated device information.
 */
data class DeviceInfo(
    /** Device model */
    val model: DeviceModel,
    /** Device serial number */
    val sn: String,
    /** Current device status */
    var status: DeviceStatus?,
) {
    /**
     * Update device status if the SN matches.
     */
    fun updateStatus(status: DeviceStatus) {
        if (status.sn == sn) {
           this.status = status
        }
    }
}

/**
 * OS event type enumeration.
 */
enum class OsEventTypeList(val value: Int) {
    CLICK_EVENT(0),
    SCROLL_TOP_EVENT(1),
    SCROLL_BOTTOM_EVENT(2),
    DOUBLE_CLICK_EVENT(3),
    FOREGROUND_ENTER_EVENT(4),
    FOREGROUND_EXIT_EVENT(5),
    ABNORMAL_EXIT_EVENT(6);

    companion object {
        fun fromInt(value: Int?): OsEventTypeList? {
            return values().find { it.value == value }
        }

        fun fromString(value: String?): OsEventTypeList? {
            if (value == null) return null
            return values().find {
                it.name.equals(value, ignoreCase = true) ||
                it.name.replace("_EVENT", "").equals(value, ignoreCase = true)
            }
        }
    }
}

/**
 * List item event.
 */
data class ListItemEvent(
    /** Container ID */
    val containerID: Int? = null,
    /** Container name */
    val containerName: String? = null,
    /** Currently selected item name */
    val currentSelectItemName: String? = null,
    /** Currently selected item index */
    val currentSelectItemIndex: Int? = null,
    /** Event type */
    val eventType: OsEventTypeList? = null,
)

/**
 * Text item event.
 */
data class TextItemEvent(
    /** Container ID */
    val containerID: Int? = null,
    /** Container name */
    val containerName: String? = null,
    /** Event type */
    val eventType: OsEventTypeList? = null,
)

/**
 * System item event.
 */
data class SysItemEvent(
    /** Event type */
    val eventType: OsEventTypeList? = null,
)

/**
 * Event emitted by EvenHub.
 *
 * The structure directly contains parsed event objects instead of raw JSON strings.
 * Developers only need to check which property is non-null to use the corresponding event object.
 */
data class EvenHubEvent(
    /** List event (if present) */
    val listEvent: ListItemEvent? = null,
    /** Text event (if present) */
    val textEvent: TextItemEvent? = null,
    /** System event (if present) */
    val sysEvent: SysItemEvent? = null,
    /** Raw JSON data (optional, useful for debugging/replay) */
    val jsonData: String? = null,
)

/**
 * EvenHub PB interface parameter model (aligned with host BleG2CmdProtoEvenHubExt).
 * List item container properties.
 */
data class ListItemContainerProperty(
    /** Number of list items */
    val itemCount: Int? = null,
    /** Width of a single item */
    val itemWidth: Int? = null,
    /** Whether item selection border is enabled (1: enabled, 0: disabled) */
    val isItemSelectBorderEn: Int? = null,
    /** List of item names */
    val itemName: List<String>? = null,
)

/**
 * List container properties.
 */
data class ListContainerProperty(
    /** X position */
    val xPosition: Int? = null,
    /** Y position */
    val yPosition: Int? = null,
    /** Container width */
    val width: Int? = null,
    /** Container height */
    val height: Int? = null,
    /** Border width */
    val borderWidth: Int? = null,
    /** Border color value */
    val borderColor: Int? = null,
    /** Border corner radius */
    val borderRdaius: Int? = null,
    /** Padding length */
    val paddingLength: Int? = null,
    /** Unique container ID */
    val containerID: Int? = null,
    /** Container name */
    val containerName: String? = null,
    /** Item properties within the list */
    val itemContainer: ListItemContainerProperty? = null,
    /** Whether to capture events */
    val isEventCapture: Int? = null,
)

/**
 * Text container properties.
 */
data class TextContainerProperty(
    /** X position */
    val xPosition: Int? = null,
    /** Y position */
    val yPosition: Int? = null,
    /** Container width */
    val width: Int? = null,
    /** Container height */
    val height: Int? = null,
    /** Border width */
    val borderWidth: Int? = null,
    /** Border color value */
    val borderColor: Int? = null,
    /** Border corner radius */
    val borderRdaius: Int? = null,
    /** Padding length */
    val paddingLength: Int? = null,
    /** Unique container ID */
    val containerID: Int? = null,
    /** Container name */
    val containerName: String? = null,
    /** Whether to capture events */
    val isEventCapture: Int? = null,
    /** Text content */
    val content: String? = null,
)

/**
 * Image container properties.
 */
data class ImageContainerProperty(
    /** X position */
    val xPosition: Int? = null,
    /** Y position */
    val yPosition: Int? = null,
    /** Container width */
    val width: Int? = null,
    /** Container height */
    val height: Int? = null,
    /** Unique container ID */
    val containerID: Int? = null,
    /** Container name */
    val containerName: String? = null,
)

/**
 * Data structure for creating a startup page container.
 */
data class CreateStartUpPageContainer(
    /** Total number of containers */
    val containerTotalNum: Int? = null,
    /** List of list container objects */
    val listObject: List<ListContainerProperty>? = null,
    /** List of text container objects */
    val textObject: List<TextContainerProperty>? = null,
    /** List of image container objects */
    val imageObject: List<ImageContainerProperty>? = null,
)

/**
 * Data structure for rebuilding a page container.
 */
data class RebuildPageContainer(
    /** Total number of containers */
    val containerTotalNum: Int? = null,
    /** List of list container objects */
    val listObject: List<ListContainerProperty>? = null,
    /** List of text container objects */
    val textObject: List<TextContainerProperty>? = null,
    /** List of image container objects */
    val imageObject: List<ImageContainerProperty>? = null,
)

/**
 * Data structure for updating image raw data.
 *
 * Corresponds to host Dart: `EvenHubImageContainer`
 *
 * Notes:
 * - This only handles field model + JSON mapping, not protobuf bytes encoding/decoding
 * - `imageData` should preferably be **number[]** (host `List<int>` works best)
 * - If Uint8Array/ArrayBuffer is passed, it will be converted to number[] during `toJson`
 */
data class ImageRawDataUpdate(
    /** Container ID */
    val containerID: Int? = null,
    /** Container name */
    val containerName: String? = null,
    /** Image data (can be number[], string, Uint8Array, or ArrayBuffer) */
    val imageData: Any? = null,
)

/**
 * Data structure for upgrading text container content.
 */
data class TextContainerUpgrade(
    /** Container ID */
    val containerID: Int? = null,
    /** Container name */
    val containerName: String? = null,
    /** Content offset */
    val contentOffset: Int? = null,
    /** Content length */
    val contentLength: Int? = null,
    /** New text content */
    val content: String? = null,
)

/**
 * Data structure for shutting down a container.
 */
data class ShutDownContainer(
    /** Exit mode (default 0) */
    val exitMode: Int = 0,
)
