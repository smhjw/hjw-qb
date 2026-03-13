package com.hjw.qbremote.data

import com.hjw.qbremote.data.model.TorrentInfo
import com.hjw.qbremote.data.model.TorrentFileInfo
import com.hjw.qbremote.data.model.TorrentCategory
import com.hjw.qbremote.data.model.TorrentProperties
import com.hjw.qbremote.data.model.TorrentTracker
import com.hjw.qbremote.data.model.TransferInfo
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Query

interface QbApi {
    @FormUrlEncoded
    @POST("api/v2/auth/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String,
    ): Response<String>

    @FormUrlEncoded
    @POST("login")
    suspend fun loginLegacy(
        @Field("username") username: String,
        @Field("password") password: String,
    ): Response<String>

    @GET("api/v2/transfer/info")
    suspend fun transferInfo(): TransferInfo

    @GET("api/v2/app/version")
    suspend fun appVersion(): String

    @GET("api/v2/torrents/info")
    suspend fun torrentsInfo(
        @Query("sort") sort: String = "added_on",
        @Query("reverse") reverse: Boolean = true,
        @Query("hashes") hashes: String? = null,
    ): List<TorrentInfo>

    @FormUrlEncoded
    @POST("api/v2/torrents/pause")
    suspend fun pauseTorrents(
        @Field("hashes") hashes: String,
    ): Response<Unit>

    @FormUrlEncoded
    @POST("api/v2/torrents/resume")
    suspend fun resumeTorrents(
        @Field("hashes") hashes: String,
    ): Response<Unit>

    @FormUrlEncoded
    @POST("api/v2/torrents/delete")
    suspend fun deleteTorrents(
        @Field("hashes") hashes: String,
        @Field("deleteFiles") deleteFiles: Boolean,
    ): Response<Unit>

    @GET("api/v2/torrents/properties")
    suspend fun torrentProperties(
        @Query("hash") hash: String,
    ): TorrentProperties

    @GET("api/v2/torrents/files")
    suspend fun torrentFiles(
        @Query("hash") hash: String,
    ): List<TorrentFileInfo>

    @GET("api/v2/torrents/trackers")
    suspend fun torrentTrackers(
        @Query("hash") hash: String,
    ): List<TorrentTracker>

    @GET("api/v2/torrents/categories")
    suspend fun torrentCategories(): Map<String, TorrentCategory>

    @GET("api/v2/torrents/tags")
    suspend fun torrentTagsRaw(): String

    @GET("api/v2/sync/torrentPeers")
    suspend fun torrentPeers(
        @Query("hash") hash: String,
        @Query("rid") rid: Int = 0,
    ): String

    @FormUrlEncoded
    @POST("api/v2/torrents/rename")
    suspend fun renameTorrent(
        @Field("hash") hash: String,
        @Field("name") name: String,
    ): Response<Unit>

    @FormUrlEncoded
    @POST("api/v2/torrents/setLocation")
    suspend fun setLocation(
        @Field("hashes") hashes: String,
        @Field("location") location: String,
    ): Response<Unit>

    @FormUrlEncoded
    @POST("api/v2/torrents/setCategory")
    suspend fun setCategory(
        @Field("hashes") hashes: String,
        @Field("category") category: String,
    ): Response<Unit>

    @FormUrlEncoded
    @POST("api/v2/torrents/addTags")
    suspend fun addTags(
        @Field("hashes") hashes: String,
        @Field("tags") tags: String,
    ): Response<Unit>

    @FormUrlEncoded
    @POST("api/v2/torrents/removeTags")
    suspend fun removeTags(
        @Field("hashes") hashes: String,
        @Field("tags") tags: String,
    ): Response<Unit>

    @FormUrlEncoded
    @POST("api/v2/torrents/setDownloadLimit")
    suspend fun setDownloadLimit(
        @Field("hashes") hashes: String,
        @Field("limit") limit: Long,
    ): Response<Unit>

    @FormUrlEncoded
    @POST("api/v2/torrents/setUploadLimit")
    suspend fun setUploadLimit(
        @Field("hashes") hashes: String,
        @Field("limit") limit: Long,
    ): Response<Unit>

    @FormUrlEncoded
    @POST("api/v2/torrents/setShareLimits")
    suspend fun setShareLimits(
        @Field("hashes") hashes: String,
        @Field("ratioLimit") ratioLimit: Double,
        @Field("seedingTimeLimit") seedingTimeLimit: Int = -1,
        @Field("inactiveSeedingTimeLimit") inactiveSeedingTimeLimit: Int = -1,
    ): Response<Unit>

    @Multipart
    @POST("api/v2/torrents/add")
    suspend fun addTorrents(
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part torrents: List<MultipartBody.Part>,
    ): Response<String>
}
