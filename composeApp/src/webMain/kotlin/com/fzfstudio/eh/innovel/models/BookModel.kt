package com.fzfstudio.eh.innovel.models

/**
 * Book information.
 */
data class BookModel(
    /** Unique book identifier */
    val id: String,
    /** Book title */
    val title: String,
    /** Author */
    val author: String,
    /** Book genre */
    val type: String,
    /** Book chapters */
    val chapters: List<BookChapterModel> = emptyList(),
) {

    /** Total number of chapters */
    val totalChapters: Int
        get() = chapters.size

    /** Number of chapters read */
    var readChapters: Int = 0;
}

/**
 * Book chapter.
 */
data class BookChapterModel(
    /** Parent book ID */
    val bookId: String,
    /** Chapter index */
    val index: Int,
    /** Chapter title */
    val title: String,
    /** Chapter content */
    val content: String,
) {
    /** Display content (truncated preview) */
    val displayContent: String
        get() = if (content.length > 25) content.take(25) + "..." else content
    /** Whether the chapter has been read */
    var hadRead: Boolean = false;
}
