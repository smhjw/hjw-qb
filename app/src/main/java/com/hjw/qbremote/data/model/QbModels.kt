package com.hjw.qbremote.data.model

import com.google.gson.annotations.SerializedName

data class TransferInfo(
    @SerializedName("dl_info_speed") val downloadSpeed: Long = 0,
    @SerializedName("up_info_speed") val uploadSpeed: Long = 0,
    @SerializedName(value = "dl_info_data", alternate = ["alltime_dl"]) val downloadedTotal: Long = 0,
    @SerializedName(value = "up_info_data", alternate = ["alltime_ul"]) val uploadedTotal: Long = 0,
    @SerializedName("dl_rate_limit") val downloadRateLimit: Long = 0,
    @SerializedName("up_rate_limit") val uploadRateLimit: Long = 0,
    @SerializedName("free_space_on_disk") val freeSpaceOnDisk: Long = 0,
    @SerializedName("dht_nodes") val dhtNodes: Int = 0,
)

data class TorrentInfo(
    @SerializedName("hash") val hash: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("category") val category: String = "",
    @SerializedName("ratio") val ratio: Double = 0.0,
    @SerializedName("size") val size: Long = 0,
    @SerializedName("progress") val progress: Float = 0f,
    @SerializedName("state") val state: String = "",
    @SerializedName("dlspeed") val downloadSpeed: Long = 0,
    @SerializedName("upspeed") val uploadSpeed: Long = 0,
    @SerializedName("downloaded") val downloaded: Long = 0,
    @SerializedName("uploaded") val uploaded: Long = 0,
    @SerializedName("added_on") val addedOn: Long = 0,
    @SerializedName("last_activity") val lastActivity: Long = 0,
    @SerializedName("eta") val eta: Long = 0,
    @SerializedName("num_seeds") val seeders: Int = 0,
    @SerializedName("num_leechs") val leechers: Int = 0,
    @SerializedName("num_complete") val numComplete: Int = 0,
    @SerializedName("num_incomplete") val numIncomplete: Int = 0,
    @SerializedName("tracker") val tracker: String = "",
    @SerializedName("save_path") val savePath: String = "",
    @SerializedName("tags") val tags: String = "",
)

data class DashboardData(
    val transferInfo: TransferInfo,
    val torrents: List<TorrentInfo>,
)

data class CountryUploadRecord(
    val countryCode: String = "",
    val countryName: String = "",
    val uploadedBytes: Long = 0,
)

data class DailyCountryUploadStats(
    val dateLabel: String = "",
    val countries: List<CountryUploadRecord> = emptyList(),
)

data class CountryPeerSnapshot(
    val key: String = "",
    val peerAddress: String = "",
    val countryCode: String = "",
    val countryName: String = "",
    val uploadedBytes: Long = 0,
)

data class TorrentDetailData(
    val properties: TorrentProperties,
    val files: List<TorrentFileInfo>,
)

data class TorrentProperties(
    @SerializedName("save_path") val savePath: String = "",
    @SerializedName("creation_date") val creationDate: Long = 0,
    @SerializedName("piece_size") val pieceSize: Long = 0,
    @SerializedName("comment") val comment: String = "",
    @SerializedName("total_size") val totalSize: Long = 0,
    @SerializedName("dl_limit") val downloadLimit: Long = 0,
    @SerializedName("up_limit") val uploadLimit: Long = 0,
    @SerializedName("time_elapsed") val timeElapsed: Long = 0,
    @SerializedName("seeding_time") val seedingTime: Long = 0,
    @SerializedName("share_ratio") val shareRatio: Double = 0.0,
)

data class TorrentFileInfo(
    @SerializedName("index") val index: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("size") val size: Long = 0,
    @SerializedName("progress") val progress: Float = 0f,
    @SerializedName("priority") val priority: Int = 0,
)

data class TorrentTracker(
    @SerializedName("url") val url: String = "",
    @SerializedName("status") val status: Int = 0,
    @SerializedName("msg") val message: String = "",
    @SerializedName("tier") val tier: Int = 0,
    @SerializedName("num_peers") val numPeers: Int = 0,
    @SerializedName("num_seeds") val numSeeds: Int = 0,
    @SerializedName("num_leeches") val numLeeches: Int = 0,
    @SerializedName("num_downloaded") val numDownloaded: Int = 0,
)

data class TorrentCategory(
    @SerializedName("name") val name: String = "",
    @SerializedName("savePath") val savePath: String = "",
)
