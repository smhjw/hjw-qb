package com.hjw.qbremote.data.model

data class AddTorrentFile(
    val name: String,
    val bytes: ByteArray,
)

data class AddTorrentRequest(
    val urls: String = "",
    val files: List<AddTorrentFile> = emptyList(),
    val autoTmm: Boolean = false,
    val category: String = "",
    val tags: String = "",
    val savePath: String = "",
    val paused: Boolean = false,
    val skipChecking: Boolean = false,
    val sequentialDownload: Boolean = false,
    val firstLastPiecePrio: Boolean = false,
    val uploadLimitBytes: Long = -1L,
    val downloadLimitBytes: Long = -1L,
)

