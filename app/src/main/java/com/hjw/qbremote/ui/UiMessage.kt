package com.hjw.qbremote.ui

import android.content.Context
import androidx.annotation.StringRes

internal sealed interface UiMessage {
    data class Resource(
        @StringRes val stringRes: Int,
        val formatArgs: List<Any> = emptyList(),
    ) : UiMessage

    data class Text(
        val text: String,
    ) : UiMessage

    companion object {
        fun resource(
            @StringRes stringRes: Int,
            vararg formatArgs: Any,
        ): UiMessage {
            return Resource(stringRes, formatArgs.toList())
        }
    }
}

internal fun UiMessage.resolve(context: Context): String {
    return when (this) {
        is UiMessage.Resource -> context.getString(stringRes, *formatArgs.toTypedArray())
        is UiMessage.Text -> text
    }
}
