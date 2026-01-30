package com.fzfstudio.eh.innovel.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fzfstudio.eh.innovel.models.AppUiState
import com.fzfstudio.eh.innovel.models.BookModel
import com.fzfstudio.eh.innovel.views.ReadingDialog
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Main app screen component.
 * Contains user info, device info, and book list.
 *
 * @param uiState App UI state
 * @param onStartReading Callback when start reading is tapped
 */
@Composable
fun AppScreen(
    uiState: AppUiState,
    onStartReading: (BookModel) -> Unit,
    onExitReading: () -> Unit
) {
    val readingBook = remember { mutableStateOf<BookModel?>(null) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState.errorMessage != null) {
            ErrorBanner(uiState.errorMessage)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserBookshelfCard(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                userInfo = uiState.userInfo
            )
            Spacer(modifier = Modifier.width(12.dp))
            DeviceInfoCard(
                modifier = Modifier
                    .width(60.dp)
                    .height(30.dp),
                deviceInfo = uiState.deviceInfo,
                deviceStatus = uiState.deviceStatus
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            uiState.books.forEach { book ->
                BookItem(
                    book = book,
                    onStartReading = {
                        onStartReading(book)
                        readingBook.value = book
                    }
                )
            }
        }
        // Test Image
        TextImageView()
        
    }
    val currentBook = readingBook.value
    if (currentBook != null) {
        ReadingDialog(
            show = true,
            book = currentBook,
            onExit = {
                readingBook.value = null
                // Call suspend function in coroutine scope
                onExitReading()
            }
        )
    }
}
