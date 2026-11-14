package com.sxanalys.music.spotify

import android.content.Context
import android.util.Base64
import com.sxanalys.music.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

@Serializable
data class SpotifyTrack(
    val title: String,
    val artist: String,
    val album: String = "",
    val artworkUrl: String? = null,
    val durationMs: Long = 0L,
)

@Serializable
data class SpotifyPlaylist(
    val id: String,
    val name: String,
    val owner: String = "",
    val artworkUrl: String? = null,
    val tracks: List<SpotifyTrack> = emptyList(),
    val importedAt: Long = System.currentTimeMillis(),
)

/**
 * Metadata-only import using the official Spotify Web API (Client Credentials flow with the user's own
 * developer app credentials). No audio is downloaded or matched; entries are catalogue metadata.
 */
object SpotifyImporter {
    private val client =
        OkHttpClient
            .Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    private val json = Json { ignoreUnknownKeys = true }
    private val idRegex = Regex("""playlist[/:]([A-Za-z0-9]{22})""")

    fun parsePlaylistId(input: String): String? = idRegex.find(input.trim())?.groupValues?.get(1)

    private fun token(clientId: String, clientSecret: String): String {
        val basic = Base64.encodeToString("$clientId:$clientSecret".toByteArray(), Base64.NO_WRAP)
        val req =
            Request
                .Builder()
                .url("https://accounts.spotify.com/api/token")
                .header("Authorization", "Basic $basic")
                .post(FormBody.Builder().add("grant_type", "client_credentials").build())
                .build()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw IllegalStateException("Spotify credentials rejected (${r.code})")
            return json.parseToJsonElement(r.body.string()).jsonObject["access_token"]?.jsonPrimitive?.contentOrNull
                ?: throw IllegalStateException("Spotify token not received")
        }
    }

    private fun get(url: String, token: String): JsonObject {
        val req = Request.Builder().url(url).header("Authorization", "Bearer $token").build()
        client.newCall(req).execute().use { r ->
            when {
                r.code == 404 -> throw IllegalStateException("Playlist not found or private")
                r.code == 403 -> throw IllegalStateException("Spotify denied access to this playlist (403)")
                !r.isSuccessful -> throw IllegalStateException("Spotify error ${r.code}")
            }
            return json.parseToJsonElement(r.body.string()).jsonObject
        }
    }

    private fun parseItems(items: JsonArray): List<SpotifyTrack> =
        items.mapNotNull { el ->
            val obj = el.jsonObject
            val t = (obj["track"] ?: obj["item"])?.takeIf { it is JsonObject }?.jsonObject ?: return@mapNotNull null
            val title = t["name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val artists = t["artists"]?.jsonArray?.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.contentOrNull }.orEmpty()
            val album = t["album"] as? JsonObject
            SpotifyTrack(
                title = title,
                artist = artists.joinToString(", "),
                album = album?.get("name")?.jsonPrimitive?.contentOrNull.orEmpty(),
                artworkUrl = album?.get("images")?.jsonArray?.firstOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull,
                durationMs = t["duration_ms"]?.jsonPrimitive?.intOrNull?.toLong() ?: 0L,
            )
        }

    /** Depth-first search for the first value stored under [key]. */
    private fun findKey(el: JsonElement, key: String): JsonElement? =
        when (el) {
            is JsonObject -> el[key] ?: el.values.firstNotNullOfOrNull { findKey(it, key) }
            is JsonArray -> el.firstNotNullOfOrNull { findKey(it, key) }
            else -> null
        }

    /**
     * Credential-free fallback: reads the public embed page of a playlist (the same data Spotify serves to
     * any website embedding it). Unofficial, so it can stop working if Spotify changes the page. Returns
     * metadata only (usually the first ~100 tracks); null when the page cannot be parsed.
     */
    private fun fromEmbedPage(id: String): SpotifyPlaylist? {
        val req = Request.Builder().url("https://open.spotify.com/embed/playlist/$id").header("User-Agent", "Mozilla/5.0").build()
        val html = client.newCall(req).execute().use { r -> if (r.isSuccessful) r.body.string() else return null }
        val marker = "id=\"__NEXT_DATA__\""
        val start = html.indexOf(marker).takeIf { it >= 0 } ?: return null
        val open = html.indexOf('>', start)
        val end = html.indexOf("</script>", open)
        if (open < 0 || end < 0) return null
        val root = runCatching { json.parseToJsonElement(html.substring(open + 1, end)) }.getOrNull() ?: return null
        val list = findKey(root, "trackList") as? JsonArray ?: return null
        val tracks =
            list.mapNotNull { el ->
                val o = el as? JsonObject ?: return@mapNotNull null
                val title = o["title"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                SpotifyTrack(
                    title = title,
                    artist = o["subtitle"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    durationMs = o["duration"]?.jsonPrimitive?.intOrNull?.toLong() ?: 0L,
                )
            }
        if (tracks.isEmpty()) return null
        val entity = findKey(root, "entity") as? JsonObject
        val cover =
            (entity?.get("coverArt") as? JsonObject)?.get("sources")?.jsonArray?.firstOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
        return SpotifyPlaylist(
            id = id,
            name = entity?.get("name")?.jsonPrimitive?.contentOrNull ?: entity?.get("title")?.jsonPrimitive?.contentOrNull ?: "Spotify Playlist",
            artworkUrl = cover,
            tracks = tracks,
        )
    }

    /** Public oEmbed (no credentials): playlist title and cover only, no track list. */
    private fun oEmbed(id: String): SpotifyPlaylist {
        val url = "https://open.spotify.com/oembed?url=https://open.spotify.com/playlist/$id"
        client.newCall(Request.Builder().url(url).build()).execute().use { r ->
            if (!r.isSuccessful) throw IllegalStateException("Playlist not found or private")
            val o = json.parseToJsonElement(r.body.string()).jsonObject
            return SpotifyPlaylist(
                id = id,
                name = o["title"]?.jsonPrimitive?.contentOrNull ?: "Spotify Playlist",
                artworkUrl = o["thumbnail_url"]?.jsonPrimitive?.contentOrNull,
            )
        }
    }

    suspend fun import(url: String): SpotifyPlaylist =
        withContext(Dispatchers.IO) {
            val id = parsePlaylistId(url) ?: throw IllegalArgumentException("Invalid Spotify playlist URL")
            val clientId = BuildConfig.SPOTIFY_CLIENT_ID
            val clientSecret = BuildConfig.SPOTIFY_CLIENT_SECRET
            if (clientId.isBlank() || clientSecret.isBlank()) {
                return@withContext runCatching { fromEmbedPage(id) }.getOrNull() ?: oEmbed(id)
            }
            val tk = token(clientId, clientSecret)
            val head = get("https://api.spotify.com/v1/playlists/$id?fields=name,owner(display_name),images", tk)
            val tracks = mutableListOf<SpotifyTrack>()
            var next: String? = "https://api.spotify.com/v1/playlists/$id/tracks?limit=100"
            var guard = 0
            while (next != null && guard++ < 50) {
                val page = get(next, tk)
                tracks += parseItems(page["items"]?.jsonArray ?: JsonArray(emptyList()))
                next = page["next"]?.jsonPrimitive?.contentOrNull
            }
            SpotifyPlaylist(
                id = id,
                name = head["name"]?.jsonPrimitive?.contentOrNull ?: "Spotify Playlist",
                owner = (head["owner"] as? JsonObject)?.get("display_name")?.jsonPrimitive?.contentOrNull.orEmpty(),
                artworkUrl = head["images"]?.jsonArray?.firstOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
                    ?: tracks.firstOrNull { it.artworkUrl != null }?.artworkUrl,
                tracks = tracks,
            )
        }
}

/** Local store for imported Spotify metadata (JSON file in app-private storage). */
object SpotifyLibrary {
    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Any()

    private fun file(context: Context) = File(context.filesDir, "spotify_library.json")

    fun load(context: Context): List<SpotifyPlaylist> =
        synchronized(lock) {
            runCatching { json.decodeFromString<List<SpotifyPlaylist>>(file(context).readText()) }.getOrDefault(emptyList())
        }

    private fun save(context: Context, list: List<SpotifyPlaylist>) {
        val tmp = File(context.filesDir, "spotify_library.json.tmp")
        tmp.writeText(json.encodeToString(list))
        tmp.renameTo(file(context))
    }

    /** Adds or replaces a playlist (same Spotify id). */
    fun upsert(context: Context, playlist: SpotifyPlaylist) =
        synchronized(lock) { save(context, listOf(playlist) + load(context).filterNot { it.id == playlist.id }) }

    fun remove(context: Context, id: String) = synchronized(lock) { save(context, load(context).filterNot { it.id == id }) }
}
