@file:OptIn(ExperimentalWasmJsInterop::class)

package com.fzfstudio.eh.innovel.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fzfstudio.eh.innovel.sdk.*
import kotlin.js.ExperimentalWasmJsInterop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Aggregated page UI state for rendering in the view layer.
 */
data class AppUiState(
    /** Whether the SDK bridge has finished initializing */
    val isBridgeReady: Boolean = false,
    /** User info (may be null) */
    val userInfo: UserInfo? = null,
    /** Device basic info (may be null) */
    val deviceInfo: DeviceInfo? = null,
    /** Device status (may be null) */
    val deviceStatus: DeviceStatus? = null,
    /** Bookshelf book list */
    val books: List<BookModel> = emptyList(),
    /** Page-level error message (may be null) */
    val errorMessage: String? = null,
    /** Whether in full-screen reading mode */
    val isFullScreenReading: Boolean = false,
)

/**
 * Page business state and data fetching logic.
 */
class AppState {
    /** Page UI state */
    var uiState by mutableStateOf(AppUiState(books = emptyList()))
        private set

    /** Unsubscribe function for device status listener */
    private var unsubscribeDeviceStatus: (() -> Unit)? = null
    /** Unsubscribe function for EvenHubEvent listener */
    private var unsubscribeEvenHubEvent: (() -> Unit)? = null

    /** Currently reading book ID, defaults to book_001 */
    private var currentReadingBookId: String = "book_001"

    /** Current chapter index, defaults to 0 */
    private var currentChapterIndex: Int = 0

    /** Reading fragment list (split by character count) */
    private var readingFragments: List<String> = emptyList()

    /** Current reading fragment index */
    private var readingFragmentIndex: Int = 0

    /** Coroutine scope for calling suspend functions in event handlers */
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * Initialize the SDK bridge and fetch user/device info.
     */
    suspend fun initialize() {
        if (uiState.isBridgeReady) return
        try {
            ensureEvenAppBridge()
            val userInfo = runCatching { getUserInfo() }
                .getOrElse { error ->
                    uiState = uiState.copy(errorMessage = "Failed to get user info: ${error.message}")
                    null
                }
            val deviceInfo = runCatching { getDeviceInfo() }
                .getOrElse { error ->
                    uiState = uiState.copy(errorMessage = "Failed to get device info: ${error.message}")
                    null
                }
            // Load book data from JSON file
            val books = runCatching { loadBooksFromJson() }
                .getOrElse { error ->
                    // If loading fails, use default data
                    defaultBooks()
                }
            uiState = uiState.copy(
                isBridgeReady = true,
                userInfo = userInfo,
                deviceInfo = deviceInfo,
                deviceStatus = deviceInfo?.status,
                books = books,
            )
            // After initialization, set up device status listener
            setupDeviceStatusObserver()
            // Set up EvenHubEvent listener
            setupEvenHubEventObserver()
        } catch (e: Exception) {
            uiState = uiState.copy(errorMessage = "Failed to initialize bridge: ${e.message}")
        }
    }

    /**
     * Set up device status observer.
     */
    private fun setupDeviceStatusObserver() {
        // Cancel previous listener (if exists)
        unsubscribeDeviceStatus?.invoke()
        // Set up new listener
        unsubscribeDeviceStatus = observeDeviceStatus { status ->
            if (status != null) {
                // Update device status
                updateDeviceStatus(status)
            }
        }
    }

    /**
     * Set up EvenHubEvent observer.
     */
    private fun setupEvenHubEventObserver() {
        // Cancel previous listener (if exists)
        unsubscribeEvenHubEvent?.invoke()
        // Set up new listener and parse event data
        unsubscribeEvenHubEvent = observeEvenHubEvent { event ->
            if (event != null) {
                // The new structure directly contains parsed event objects.
                // Just check which property is not null and use the corresponding event object.

                when {
                    event.listEvent != null -> {
                        val listEvent = event.listEvent
                        println("[EvenHubEvent] ListItemEvent - ContainerID: ${listEvent.containerID}, " +
                                "ContainerName: ${listEvent.containerName}, " +
                                "ItemIndex: ${listEvent.currentSelectItemIndex}, " +
                                "ItemName: ${listEvent.currentSelectItemName}, " +
                                "EventType: ${listEvent.eventType}")
                        handleListItemEvent(listEvent)
                    }
                    event.textEvent != null -> {
                        val textEvent = event.textEvent
                        println("[EvenHubEvent] TextItemEvent - ContainerID: ${textEvent.containerID}, " +
                                "ContainerName: ${textEvent.containerName}, " +
                                "EventType: ${textEvent.eventType}")
                        handleTextItemEvent(textEvent)
                    }
                    event.sysEvent != null -> {
                        val sysEvent = event.sysEvent
                        println("[EvenHubEvent] SysItemEvent - EventType: ${sysEvent.eventType}")
                        handleSysItemEvent(sysEvent)
                    }
                    else -> {
                        // If all events are null, print debug info
                        println("[EvenHubEvent] No event data found. jsonData: ${event.jsonData}")
                    }
                }
            }
        }
    }

    /**
     * Handle list item event.
     */
    private fun handleListItemEvent(event: ListItemEvent) {
        // If data contains currentSelectItemIndex, update chapter content
        val itemIndex = event.currentSelectItemIndex
        if (itemIndex != null && itemIndex >= 0) {
            // Find book by current reading book ID
            val book = uiState.books.find { it.id == currentReadingBookId }
            if (book != null && itemIndex < book.chapters.size) {
                // Get corresponding chapter
                val chapter = book.chapters[itemIndex]
                // Update current chapter index
                currentChapterIndex = itemIndex
                // Call updateChapterInfo in coroutine scope
                coroutineScope.launch {
                    updateChapterInfo(chapter)
                }
            } else {
                println("[ListItemEvent] Book not found or invalid chapter index: bookId=$currentReadingBookId, index=$itemIndex")
            }
        }
    }

    /**
     * Handle text item event.
     */
    private fun handleTextItemEvent(event: TextItemEvent) {
        // Only handle scroll events in full-screen reading mode
        if (!uiState.isFullScreenReading) {
            println("[TextEvent] Scroll event: Not in full screen reading mode")
            return
        }

        when (event.eventType) {
            OsEventTypeList.SCROLL_BOTTOM_EVENT -> {
                println("[TextEvent] Scroll bottom event: Total fragments: ${readingFragments.size}, Current index: ${readingFragmentIndex}")
                // Scroll down, show next fragment
                readingFragmentIndex+=1
                if (readingFragmentIndex > readingFragments.size) {
                    readingFragmentIndex = readingFragments.size - 1
                    println("[TextEvent] Scroll bottom event: Reached end of fragments")
                    return
                }
                coroutineScope.launch {
                    updateReadingFragment(readingFragmentIndex)
                }
            }
            OsEventTypeList.SCROLL_TOP_EVENT -> {
                println("[TextEvent] Scroll top event: Total fragments: ${readingFragments.size}, Current index: ${readingFragmentIndex}")
                // Scroll up, show previous fragment
                readingFragmentIndex-=1
                if (readingFragmentIndex < 0) {
                    readingFragmentIndex = 0
                    println("[TextEvent] Scroll top event: Reached start of fragments")
                    return
                }
                coroutineScope.launch {
                    updateReadingFragment(readingFragmentIndex)
                }
            }
            else -> {
                // Other event types are not handled
            }
        }
    }

    /**
     * Handle system event.
     */
    private fun handleSysItemEvent(event: SysItemEvent) {
        // Handle system-level events such as foreground enter/exit
        when (event.eventType) {
            OsEventTypeList.DOUBLE_CLICK_EVENT -> {
                if (!uiState.isFullScreenReading) {
                    // Not in full-screen reading, enter full-screen reading
                    val book = uiState.books.find { it.id == currentReadingBookId }
                    if (book != null && currentChapterIndex < book.chapters.size) {
                        val chapter = book.chapters[currentChapterIndex]
                        // Call fullScreenReading in coroutine scope and set full-screen state to true
                        coroutineScope.launch {
                            fullScreenReading(chapter)
                            uiState = uiState.copy(isFullScreenReading = true)
                        }
                    } else {
                        println("[SysEvent] Book not found or invalid chapter index: bookId=$currentReadingBookId, index=$currentChapterIndex")
                    }
                } else {
                    // Already in full-screen reading, double-click again to exit and rebuild reading page
                    val book = uiState.books.find { it.id == currentReadingBookId }
                    if (book != null) {
                        // Call startReadingBook with isRebuild=true in coroutine scope, set full-screen state to false
                        coroutineScope.launch {
                            startReadingBook(book, isRebuild = true)
                            uiState = uiState.copy(isFullScreenReading = false)
                        }
                    } else {
                        println("[SysEvent] Book not found: bookId=$currentReadingBookId")
                    }
                }
            }
            OsEventTypeList.FOREGROUND_ENTER_EVENT -> {
                println("[SysEvent] App entered foreground")
            }
            OsEventTypeList.FOREGROUND_EXIT_EVENT -> {
                println("[SysEvent] App exited foreground")
            }
            OsEventTypeList.ABNORMAL_EXIT_EVENT -> {
                println("[SysEvent] App abnormal exit")
            }
            else -> {
                // Other system events
            }
        }
    }

    /**
     * Clean up resources, cancel all listeners.
     */
    fun dispose() {
        unsubscribeDeviceStatus?.invoke()
        unsubscribeDeviceStatus = null
        unsubscribeEvenHubEvent?.invoke()
        unsubscribeEvenHubEvent = null
    }

    /**
     * Refresh UI when a device status update is received.
     */
    fun updateDeviceStatus(status: DeviceStatus?) {
        if (status == null) return
        val deviceSn = uiState.deviceInfo?.sn ?: return
        if (status.sn != deviceSn) return
        uiState = uiState.copy(deviceStatus = status)
    }

    /**
     * Create the book reading view on the glasses.
     */
    suspend fun startReadingBook(book: BookModel, isRebuild: Boolean = false) {
        // Update current reading book ID
        currentReadingBookId = book.id
        // If not rebuilding, reset chapter index to 0; if rebuilding, keep current chapter index
        if (!isRebuild) {
            currentChapterIndex = 0
        }
        // 1. Create page properties:
        // 1.1. Book info
        val bookInfo = TextContainerProperty(
            containerID = 2,
            containerName = "info",
            xPosition = 0,
            yPosition = 0,
            width = 530,
            height = 30,
            borderWidth = 1,
            borderColor = 13,
            borderRdaius = 6,
            paddingLength = 0,
            content = "${book.title} -- ${book.author}",
        )
        // 1.2. Chapter list
        val bookChapters = listOf(
            ListContainerProperty(
                containerID = 1,
                containerName = "chapters",
                xPosition = 0,
                yPosition = 35,
                width = 110,
                height = 200,
                borderWidth = 1,
                borderColor = 13,
                borderRdaius = 6,
                paddingLength = 5,
                isEventCapture = 1,
                itemContainer = ListItemContainerProperty(
                    itemCount = book.totalChapters,
                    itemWidth = 100,
                    isItemSelectBorderEn = 1,
                    itemName = book.chapters.map { "Ch ${it.index}" }
                )
            )
        )
        // 1.3. Chapter summary (using current chapter index)
        val currentChapter = if (currentChapterIndex < book.chapters.size) {
            book.chapters[currentChapterIndex]
        } else {
            book.chapters[0] // If index is invalid, use first chapter
        }
        val chapterInfo = TextContainerProperty(
            containerID = 3,
            containerName = "content",
            xPosition = 115,
            yPosition = 35,
            width = 415,
            height = 200,
            borderWidth = 1,
            borderColor = 13,
            borderRdaius = 6,
            paddingLength = 12,
            content = "${currentChapter.title}\n\n${currentChapter.displayContent}\n\nDouble-click for full screen >>",
        )
        runCatching {
            if (isRebuild) {
                rebuildPageContainer(RebuildPageContainer(
                    containerTotalNum = 3,
                    listObject = bookChapters,
                    textObject = listOf(
                        bookInfo,
                        chapterInfo
                    )
                ))
            } else {
                createStartUpPageContainer(CreateStartUpPageContainer(
                    containerTotalNum = 3,
                    listObject = bookChapters,
                    textObject = listOf(
                        bookInfo,
                        chapterInfo
                    )
                ))
            }
        }.onFailure { error ->
            uiState = uiState.copy(errorMessage = "Failed to create book view: ${error.message}")
        }
    }

    /**
     * Update chapter summary content.
     */
    suspend fun updateChapterInfo(chapter: BookChapterModel) {
        // Initialize update object
        val update = TextContainerUpgrade(
            containerID = 3,
            containerName = "content",
            content = "${chapter.title}\n\n${chapter.displayContent}\n\nDouble-click for full screen >>"
        )
        runCatching {
            textContainerUpgrade(update)
        }.onFailure { error ->
            uiState = uiState.copy(errorMessage = "Failed to update chapter view: ${error.message}")
        }
    }

    /**
     * Enter full-screen reading mode.
     */
    suspend fun fullScreenReading(chapter: BookChapterModel) {
        // Split chapter content into fragments, max 200 characters each
        readingFragments = splitContentBySize(chapter.content, 200)
        readingFragmentIndex = 0

        // Display the first fragment
        val currentFragment = if (readingFragments.isNotEmpty()) {
            readingFragments[readingFragmentIndex]
        } else {
            chapter.content
        }

        val container = RebuildPageContainer(
            containerTotalNum = 1,
            textObject = listOf(
                TextContainerProperty(
                    containerID = 4,
                    containerName = "chapter",
                    content = currentFragment,
                    xPosition = 0,
                    yPosition = 0,
                    width = 500,
                    height = 235,
                    borderWidth = 1,
                    borderColor = 13,
                    borderRdaius = 6,
                    paddingLength = 12,
                    isEventCapture = 1,
                )
            ),
        )
        runCatching {
            rebuildPageContainer(container)
        }.onFailure { error ->
            uiState = uiState.copy(errorMessage = "Failed to rebuild page view: ${error.message}")
        }
    }

    /**
     * Update reading fragment.
     */
    suspend fun updateReadingFragment(index: Int) {
        // Ensure index is within valid range
        if (index < 0 || index >= readingFragments.size) {
            println("[ReadingFragment] Invalid index: $index, fragments size: ${readingFragments.size}")
            return
        }
        println("[ReadingFragment] Updating fragment: ${index}, Total fragments: ${readingFragments.size}")
        val fragment = readingFragments[index]
        val update = TextContainerUpgrade(
            containerID = 4,
            containerName = "chapter",
            content = fragment
        )
        runCatching {
            textContainerUpgrade(update)
        }.onFailure { error ->
            uiState = uiState.copy(errorMessage = "Failed to update reading fragment: ${error.message}")
        }
    }

    /**
     * Split content into fragments by character count.
     * @param content The content to split
     * @param maxChars Maximum characters per fragment (default 200)
     * @return List of content fragments
     */
    private fun splitContentBySize(content: String, maxChars: Int = 200): List<String> {
        if (content.isEmpty()) return emptyList()

        val fragments = mutableListOf<String>()
        var currentFragment = StringBuilder()
        var currentCharCount = 0

        for (char in content) {
            // If adding current character would exceed the limit, save current fragment and start a new one
            if (currentCharCount >= maxChars && currentFragment.isNotEmpty()) {
                fragments.add(currentFragment.toString())
                currentFragment = StringBuilder()
                currentCharCount = 0
            }

            // Add current character
            currentFragment.append(char)
            currentCharCount++
        }

        // Add the last fragment (if there is remaining content)
        if (currentFragment.isNotEmpty()) {
            fragments.add(currentFragment.toString())
        }

        return fragments
    }



    /**
     * Exit reading mode.
     */
    suspend fun exitReading() {
        runCatching {
            shutDownPageContainer(ShutDownContainer(exitMode = 0))
        }.onFailure { error ->
            uiState = uiState.copy(errorMessage = "Failed to close book view: ${error.message}")
        }
    }
}

/**
 * Load book data from JSON file.
 */
private suspend fun loadBooksFromJson(): List<BookModel> {
    val json = JsInteropUtils.fetchJson("books.json") ?: return emptyList()

    // Check if it is an array
    if (JsInteropUtils.getType(json) != "object" || !JsInteropUtils.isArray(json)) return emptyList()

    // Use JsInteropUtils to access array length and elements
    val length = JsInteropUtils.getArrayLength(json)
    val result = mutableListOf<BookModel>()

    for (i in 0 until length) {
        val bookJson = JsInteropUtils.getArrayElement(json, i) ?: continue
        val id = JsInteropUtils.getStringProperty(bookJson, "id") ?: continue
        val title = JsInteropUtils.getStringProperty(bookJson, "title") ?: continue
        val author = JsInteropUtils.getStringProperty(bookJson, "author") ?: continue
        val type = JsInteropUtils.getStringProperty(bookJson, "type") ?: continue
        val readChapters = JsInteropUtils.getIntProperty(bookJson, "readChapters") ?: 0

        // Parse chapter list (if present)
        val chaptersJson = JsInteropUtils.getProperty(bookJson, "chapters")
        val chapters = if (chaptersJson != null && JsInteropUtils.isArray(chaptersJson)) {
            val chaptersLength = JsInteropUtils.getArrayLength(chaptersJson)
            val chaptersList = mutableListOf<BookChapterModel>()

            for (j in 0 until chaptersLength) {
                val chapterJson = JsInteropUtils.getArrayElement(chaptersJson, j) ?: continue
                val chapterIndex = JsInteropUtils.getIntProperty(chapterJson, "index") ?: continue
                val chapterTitle = JsInteropUtils.getStringProperty(chapterJson, "title") ?: continue
                val chapterContent = JsInteropUtils.getStringProperty(chapterJson, "content") ?: ""
                val hadRead = JsInteropUtils.getBooleanProperty(chapterJson, "hadRead") ?: false

                chaptersList.add(
                    BookChapterModel(
                        bookId = id,
                        index = chapterIndex,
                        title = chapterTitle,
                        content = chapterContent
                    ).apply {
                        this.hadRead = hadRead
                    }
                )
            }
            chaptersList
        } else {
            emptyList()
        }

        result.add(
            BookModel(
                id = id,
                title = title,
                author = author,
                type = type,
                chapters = chapters
            ).apply {
                this.readChapters = readChapters
            }
        )
    }

    return result
}

/**
 * Default bookshelf placeholder data (used when JSON loading fails).
 */
private fun defaultBooks(): List<BookModel> {
    return listOf(
        BookModel(
            id = "book_001",
            title = "The Martial Demon Chronicles",
            author = "Z. Wang",
            type = "Fantasy"
        ),
        BookModel(
            id = "book_002",
            title = "Tales of Mythic Seas",
            author = "Nan Yan",
            type = "Adventure"
        ),
        BookModel(
            id = "book_003",
            title = "Nightwalker's Notes",
            author = "Lu Li",
            type = "Mystery"
        ),
    ).onEach {
        it.readChapters = (0..it.totalChapters).random()
    }
}
