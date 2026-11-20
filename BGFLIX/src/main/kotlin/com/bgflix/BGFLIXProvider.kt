package com.bgflix

import android.net.Uri
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.net.URLEncoder
import org.json.JSONObject
import org.json.JSONArray
import okhttp3.Interceptor
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class BGFLIXProvider : MainAPI() {
    override var mainUrl = "https://v3-cinemeta.strem.io"
    private val seirsandukUrl = "https://www.seirsanduk.online"
    private val filmifenUrl = "https://filmifen.com"
    override var name = "BGFLIX"
    override val supportedTypes = setOf(TvType.Live, TvType.Movie, TvType.TvSeries)
    override var lang = "bg"
    override val hasMainPage = true
    override val hasQuickSearch = true

    override val mainPage = mainPageOf(
        "live_all" to "\u0412\u0441\u0438\u0447\u043a\u0438 \u043d\u0430 \u0436\u0438\u0432\u043e",
        "live_sports" to "\u041d\u0430 \u0436\u0438\u0432\u043e: \u0421\u043f\u043e\u0440\u0442",
        "live_movies" to "\u041d\u0430 \u0436\u0438\u0432\u043e: \u0424\u0438\u043b\u043c\u0438 & \u0421\u0435\u0440\u0438\u0430\u043b\u0438",
        "live_kids" to "\u041d\u0430 \u0436\u0438\u0432\u043e: \u0414\u0435\u0442\u0441\u043a\u0438",
        "stream_nfx_movie" to "\ud83d\udd34 Netflix: \u041f\u043e\u043f\u0443\u043b\u044f\u0440\u043d\u0438 & \u041d\u043e\u0432\u0438 \u0424\u0438\u043b\u043c\u0438",
        "stream_nfx_series" to "\ud83d\udd34 Netflix: \u041f\u043e\u043f\u0443\u043b\u044f\u0440\u043d\u0438 & \u041d\u043e\u0432\u0438 \u0421\u0435\u0440\u0438\u0430\u043b\u0438",
        "stream_amp_movie" to "\ud83d\udd35 Prime Video: \u041f\u043e\u043f\u0443\u043b\u044f\u0440\u043d\u0438 & \u041d\u043e\u0432\u0438 \u0424\u0438\u043b\u043c\u0438",
        "stream_amp_series" to "\ud83d\udd35 Prime Video: \u041f\u043e\u043f\u0443\u043b\u044f\u0440\u043d\u0438 & \u041d\u043e\u0432\u0438 \u0421\u0435\u0440\u0438\u0430\u043b\u0438",
        "stream_dnp_movie" to "\ud83c\udff0 Disney+: \u041f\u043e\u043f\u0443\u043b\u044f\u0440\u043d\u0438 & \u041d\u043e\u0432\u0438 \u0424\u0438\u043b\u043c\u0438",
        "stream_dnp_series" to "\ud83c\udff0 Disney+: \u041f\u043e\u043f\u0443\u043b\u044f\u0440\u043d\u0438 & \u041d\u043e\u0432\u0438 \u0421\u0435\u0440\u0438\u0430\u043b\u0438",
        "stream_hbm_movie" to "\ud83d\udfe3 HBO Max: \u041f\u043e\u043f\u0443\u043b\u044f\u0440\u043d\u0438 & \u041d\u043e\u0432\u0438 \u0424\u0438\u043b\u043c\u0438",
        "stream_hbm_series" to "\ud83d\udfe3 HBO Max: \u041f\u043e\u043f\u0443\u043b\u044f\u0440\u043d\u0438 & \u041d\u043e\u0432\u0438 \u0421\u0435\u0440\u0438\u0430\u043b\u0438",
        "stream_atp_series" to "\ud83c\udf4f Apple TV+: \u0418\u0437\u0431\u0440\u0430\u043d\u0438 \u0421\u0435\u0440\u0438\u0430\u043b\u0438 & \u0424\u0438\u043b\u043c\u0438",
        "filmifen_bg" to "\u041d\u043e\u0432\u0438 \u0424\u0438\u043b\u043c\u0438 \u0441 \u0411\u0413 \u0410\u0443\u0434\u0438\u043e",
        "cinemeta_movie_top" to "\u0422\u043e\u043f \u041f\u043e\u043f\u0443\u043b\u044f\u0440\u043d\u0438 \u0424\u0438\u043b\u043c\u0438 (HD)",
        "cinemeta_movie_Action" to "\u0422\u043e\u043f \u0410\u043a\u0448\u044a\u043d (HD)",
        "cinemeta_movie_Comedy" to "\u0422\u043e\u043f \u041a\u043e\u043c\u0435\u0434\u0438\u044f (HD)",
        "cinemeta_movie_Horror" to "\u0422\u043e\u043f \u0423\u0436\u0430\u0441\u0438 (HD)",
        "cinemeta_movie_Thriller" to "\u0422\u043e\u043f \u0422\u0440\u0438\u043b\u044a\u0440\u0438 (HD)",
        "cinemeta_movie_Sci-Fi" to "\u0422\u043e\u043f \u041d\u0430\u0443\u0447\u043d\u0430\u0424\u0430\u043d (HD)",
        "cinemeta_movie_Drama" to "\u0422\u043e\u043f \u0414\u0440\u0430\u043c\u0430 (HD)",
        "cinemeta_movie_Animation" to "\u0422\u043e\u043f \u0410\u043d\u0438\u043c\u0430\u0446\u0438\u044f (HD)",
        "cinemeta_series_top" to "\u0422\u043e\u043f \u041f\u043e\u043f\u0443\u043b\u044f\u0440\u043d\u0438 \u0421\u0435\u0440\u0438\u0430\u043b\u0438 (HD)",
        "cinemeta_series_Drama" to "\u0422\u043e\u043f \u0421\u0435\u0440\u0438\u0430\u043b\u0438: \u0414\u0440\u0430\u043c\u0430 (HD)",
        "cinemeta_series_Comedy" to "\u0422\u043e\u043f \u0421\u0435\u0440\u0438\u0430\u043b\u0438: \u041a\u043e\u043c\u0435\u0434\u0438\u044f (HD)"
    )

    private val defaultPoster = "https://raw.githubusercontent.com/Bushidotv/BGFLIX/master/icon.png"
    private val rawLogoBase = "https://raw.githubusercontent.com/Bushidotv/BGFLIX/master/logos_hd"
    private val streamingAddonBase = "https://7a82163c306e-stremio-netflix-catalog-addon.baby-beamup.club"

    private val commonHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    )

    private val liveHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Referer"    to "$seirsandukUrl/",
        "Origin"     to seirsandukUrl,
        "Accept"     to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
    )

    private val movieHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Referer"    to "$filmifenUrl/",
        "Accept"     to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language" to "bg,en;q=0.9"
    )

    /**
     * Maps channel names to custom high-definition crystal clear logos.
     */
    private fun getHighResLogo(title: String, defaultImg: String): String {
        val lower = title.lowercase()
        val logoFile = when {
            lower.contains("bnt 1") -> "bnt1.png"
            lower.contains("bnt 2") -> "bnt2.png"
            lower.contains("bnt 3") -> "bnt3.png"
            lower.contains("bnt 4") -> "bnt4.png"
            lower.contains("btv action") -> "btv_action.png"
            lower.contains("btv cinema") -> "btv_cinema.png"
            lower.contains("btv comedy") -> "btv_comedy.png"
            lower.contains("btv story") -> "btv_story.png"
            lower.contains("btv") -> "btvhd.png"
            lower.contains("nova news") -> "nova_news.png"
            lower.contains("nova sport") -> "nova_sport.png"
            lower.contains("kino nova") -> "kino_nova.png"
            lower.contains("nova") -> "nova.png"
            lower.contains("diema sport 3") -> "diema_sport_3.png"
            lower.contains("diema sport 2") -> "diema_sport_2.png"
            lower.contains("diema sport") -> "diema_sport.png"
            lower.contains("diema family") -> "diema_family.png"
            lower.contains("diema") -> "diema.png"
            lower.contains("max sport 1") -> "MAX_SPORT1_HD.png"
            lower.contains("max sport 2") -> "MAX_SPORT2_HD.png"
            lower.contains("max sport 3") -> "MAX_SPORT3_HD.png"
            lower.contains("max sport 4") -> "MAX_SPORT4_HD.png"
            lower.contains("max one") -> "MAX_ONE_HD.png"
            lower.contains("eurosport 1") -> "eurosport1.png"
            lower.contains("eurosport 2") -> "eurosport2.png"
            lower.contains("ring") -> "RING_BG_HD.png"
            lower.contains("star crime") -> "STAR_CRIME_HD.png"
            lower.contains("star life") -> "STAR_LIFE.png"
            lower.contains("star channel") || lower.contains("star") -> "star_channel.png"
            lower.contains("axn black") -> "axn_black.png"
            lower.contains("axn white") -> "axn_white.png"
            lower.contains("axn") -> "axn.png"
            lower.contains("epic drama") -> "EPICDRAMAHD.png"
            lower.contains("nat geo wild") -> "nat_geo_wild.png"
            lower.contains("nat geo") -> "nat_geo.png"
            lower.contains("discovery") -> "discovery.png"
            lower.contains("viasat") -> "viasat_explore.png"
            lower.contains("id xtra") -> "ID_XTRA_HD.png"
            lower.contains("travel channel") -> "travel_channel.png"
            lower.contains("travel tv") -> "TRAVEL_TV.png"
            lower.contains("24 kitchen") || lower.contains("food network") -> "food_network.png"
            lower.contains("tlc") -> "tlc.png"
            lower.contains("code fashion") -> "codefashiontv.png"
            lower.contains("disney") -> "disney_channel.png"
            lower.contains("cartoon") -> "cartoon_network.png"
            lower.contains("nick jr") -> "nick_jr.png"
            lower.contains("nicktoons") -> "nicktoons.png"
            lower.contains("nickelodeon") -> "nickelodeon.png"
            lower.contains("e kids") || lower.contains("ekids") -> "EKIDS.png"
            lower.contains("bloomberg") -> "bloomberg.png"
            lower.contains("euronews") -> "euronews.png"
            lower.contains("bulgaria on air") -> "bulgaria_on_air.png"
            lower.contains("kanal 3") -> "kanal3.png"
            lower.contains("78 tv") || lower.contains("7/8 tv") -> "78tv.png"
            lower.contains("skat") -> "skat.png"
            lower.contains("vtk") -> "vtk.png"
            lower.contains("evrokom") -> "evrokom.png"
            lower.contains("city") -> "citytv.png"
            lower.contains("the voice") -> "THE_VOICE.png"
            lower.contains("planeta") -> "PLANETA_HD.png"
            lower.contains("folklor") || lower.contains("rodina") -> "RODINA_TV.png"
            lower.contains("tiankov") -> "TIANKOV_TV.png"
            lower.contains("dstv") -> "dstv.png"
            else -> null
        }

        return if (logoFile != null) {
            "$rawLogoBase/$logoFile?v=14"
        } else {
            if (defaultImg.isNotEmpty()) fixUrl(defaultImg) else defaultPoster
        }
    }

    private fun fixImageUrl(url: String): String {
        val clean = url.trim().removePrefix(".").trim()
        return when {
            clean.startsWith("http://") || clean.startsWith("https://") -> clean
            clean.startsWith("//") -> "https:$clean"
            else -> "$filmifenUrl/${clean.trimStart('/')}"
        }
    }

    private fun extractPoster(element: Element): String? {
        val imgEl = element.selectFirst("img") ?: return null

        val dataSrc = imgEl.attr("data-src").trim()
        if (dataSrc.isNotBlank() && !dataSrc.startsWith("data:")) {
            return fixImageUrl(dataSrc)
        }

        val src = imgEl.attr("src").trim()
        if (src.isNotBlank() && !src.startsWith("data:")) {
            return fixImageUrl(src)
        }

        val dataOriginal = imgEl.attr("data-original").trim()
        if (dataOriginal.isNotBlank() && !dataOriginal.startsWith("data:")) {
            return fixImageUrl(dataOriginal)
        }

        val srcset = imgEl.attr("data-srcset").ifBlank { imgEl.attr("srcset") }.trim()
        if (srcset.isNotBlank() && !srcset.startsWith("data:")) {
            val parts = srcset.split(",").map { it.trim().split(" ").firstOrNull { p -> p.isNotBlank() } ?: "" }
            val bestUrl = parts.lastOrNull { it.isNotBlank() && !it.startsWith("data:") }
                ?: parts.firstOrNull { it.isNotBlank() && !it.startsWith("data:") }
            if (!bestUrl.isNullOrBlank()) {
                return fixImageUrl(bestUrl)
            }
        }

        return null
    }

    // ==========================================
    // 1. MAIN PAGE (LIVE CHANNELS + HD MOVIES + BG AUDIO)
    // ==========================================
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        if (request.data.startsWith("live_")) {
            return getLiveChannelsPage(request)
        } else if (request.data.startsWith("stream_")) {
            return getStreamingPlatformPage(page, request)
        } else if (request.data.startsWith("cinemeta_")) {
            return getCinemetaPage(page, request)
        } else if (request.data == "filmifen_bg") {
            return getFilmifenBGPage(page, request)
        }

        return newHomePageResponse(HomePageList(request.name, emptyList()), hasNext = false)
    }

    private suspend fun getStreamingPlatformPage(page: Int, request: MainPageRequest): HomePageResponse {
        val parts = request.data.removePrefix("stream_").split("_")
        val providerId = parts.getOrElse(0) { "nfx" }
        val type = parts.getOrElse(1) { "movie" }

        val catalogUrl = "$streamingAddonBase/catalog/$type/$providerId.json"
        val items = mutableListOf<SearchResponse>()

        try {
            val response = app.get(catalogUrl, headers = commonHeaders, timeout = 12).text
            val json = JSONObject(response)
            val metas = json.optJSONArray("metas") ?: JSONArray()

            val pageSize = 25
            val startIndex = if (page <= 1) 0 else (page - 1) * pageSize
            val endIndex = minOf(startIndex + pageSize, metas.length())

            if (startIndex < metas.length()) {
                for (i in startIndex until endIndex) {
                    val meta = metas.optJSONObject(i) ?: continue
                    val imdbId = meta.optString("id", "").ifBlank { meta.optString("imdb_id", "") }.trim()
                    val name = meta.optString("name", "").trim()
                    val poster = meta.optString("poster", "").trim()
                    val yearStr = (meta.optString("releaseInfo", "").ifBlank { meta.optString("year", "") }).trim()
                    val year = yearStr.take(4).toIntOrNull()
                    val imdbRating = meta.optString("imdbRating", "").trim()

                    if (name.isEmpty() || imdbId.isEmpty()) continue

                    val detailUrl = "$mainUrl/meta/$type/$imdbId.json"
                    val displayName = if (imdbRating.isNotBlank()) "$name ⭐$imdbRating" else name

                    if (type == "series") {
                        items.add(
                            newTvSeriesSearchResponse(displayName, detailUrl, TvType.TvSeries) {
                                this.posterUrl = poster.ifBlank { null }
                                this.year = year
                                this.quality = SearchQuality.HD
                            }
                        )
                    } else {
                        items.add(
                            newMovieSearchResponse(displayName, detailUrl, TvType.Movie) {
                                this.posterUrl = poster.ifBlank { null }
                                this.year = year
                                this.quality = SearchQuality.HD
                            }
                        )
                    }
                }
            }

            return newHomePageResponse(
                HomePageList(request.name, items),
                hasNext = endIndex < metas.length()
            )
        } catch (e: Exception) {
            return newHomePageResponse(
                HomePageList(request.name, emptyList()),
                hasNext = false
            )
        }
    }

    private suspend fun getLiveChannelsPage(request: MainPageRequest): HomePageResponse {
        val channelList = mutableListOf<SearchResponse>()
        val htmlResponse = app.get("$seirsandukUrl/", headers = liveHeaders).text
        val document = Jsoup.parse(htmlResponse)
        val channelElements = document.select("#channels li a")

        channelElements.forEach { element ->
            val title = element.text().trim()
            val href = element.attr("href")
            val img = element.select("img").attr("src")
            val posterUrl = getHighResLogo(title, img)

            if (title.isNotEmpty() && href.isNotEmpty()) {
                val fullUrl = if (href.startsWith("http")) href else "$seirsandukUrl/${href.trimStart('/')}"

                val lowerTitle = title.lowercase()
                val isSport = lowerTitle.contains("sport") || lowerTitle.contains("diema") || lowerTitle.contains("euro") || lowerTitle.contains("ring") || lowerTitle.contains("max")
                val isMovie = lowerTitle.contains("kino") || lowerTitle.contains("cinema") || lowerTitle.contains("star") || lowerTitle.contains("drama") || lowerTitle.contains("axn") || lowerTitle.contains("action")
                val isKids = lowerTitle.contains("kid") || lowerTitle.contains("cartoon") || lowerTitle.contains("disney") || lowerTitle.contains("nick")

                val matchesCategory = when (request.data) {
                    "live_sports" -> isSport
                    "live_movies" -> isMovie
                    "live_kids"   -> isKids
                    else          -> true
                }

                if (matchesCategory) {
                    channelList.add(
                        newLiveSearchResponse(
                            name = title,
                            url  = fullUrl,
                            type = TvType.Live
                        ) {
                            this.posterUrl = posterUrl
                        }
                    )
                }
            }
        }
        return newHomePageResponse(
            HomePageList(request.name, channelList, isHorizontalImages = true),
            hasNext = false
        )
    }

    private suspend fun getCinemetaPage(page: Int, request: MainPageRequest): HomePageResponse {
        val parts = request.data.removePrefix("cinemeta_").split("_", limit = 2)
        val type = parts.getOrElse(0) { "movie" }
        val category = parts.getOrElse(1) { "top" }

        val skip = if (page <= 1) 0 else (page - 1) * 50
        val catalogUrl = if (category == "top") {
            if (skip > 0) {
                "$mainUrl/catalog/$type/top/skip=$skip.json"
            } else {
                "$mainUrl/catalog/$type/top.json"
            }
        } else {
            if (skip > 0) {
                "$mainUrl/catalog/$type/top/genre=$category&skip=$skip.json"
            } else {
                "$mainUrl/catalog/$type/top/genre=$category.json"
            }
        }

        val items = mutableListOf<SearchResponse>()
        try {
            val response = app.get(catalogUrl, headers = commonHeaders).text
            val json = JSONObject(response)
            val metas = json.optJSONArray("metas") ?: JSONArray()

            for (i in 0 until metas.length()) {
                val meta = metas.optJSONObject(i) ?: continue
                val imdbId = meta.optString("id", "").trim()
                val name = meta.optString("name", "").trim()
                val poster = meta.optString("poster", "").trim()
                val yearStr = meta.optString("releaseInfo", "").trim()
                val year = yearStr.take(4).toIntOrNull()
                val imdbRating = meta.optString("imdbRating", "").trim()

                if (name.isEmpty() || imdbId.isEmpty()) continue

                val detailUrl = "$mainUrl/meta/$type/$imdbId.json"
                val displayName = if (imdbRating.isNotBlank()) "$name \u2b50$imdbRating" else name

                if (type == "series") {
                    items.add(
                        newTvSeriesSearchResponse(displayName, detailUrl, TvType.TvSeries) {
                            this.posterUrl = poster
                            this.year = year
                            this.quality = SearchQuality.HD
                        }
                    )
                } else {
                    items.add(
                        newMovieSearchResponse(displayName, detailUrl, TvType.Movie) {
                            this.posterUrl = poster
                            this.year = year
                            this.quality = SearchQuality.HD
                        }
                    )
                }
            }
        } catch (e: Exception) { }

        return newHomePageResponse(
            HomePageList(request.name, items),
            hasNext = items.size >= 40
        )
    }

    private suspend fun getFilmifenBGPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) {
            "$filmifenUrl/filmi-bg-audio/"
        } else {
            "$filmifenUrl/filmi-bg-audio/page/$page/"
        }

        val html = app.get(url, headers = movieHeaders).text
        val doc = Jsoup.parse(html)
        val items = doc.select(".item-poster, .item.grid-item, #dle-content a.item").mapNotNull { el ->
            el.toFilmifenSearchResult()
        }

        val hasNext = doc.select(".pages-block a, .navigation a, a[href*='/page/']").any {
            it.text().trim() == "${page + 1}" || it.attr("href").contains("/page/${page + 1}/")
        }

        return newHomePageResponse(
            HomePageList(request.name, items),
            hasNext = hasNext
        )
    }

    private fun Element.toFilmifenSearchResult(): SearchResponse? {
        val title = this.selectFirst(".item-poster__title")?.text()?.trim()
            ?: this.attr("aria-label").replace("\u0413\u043b\u0435\u0434\u0430\u0439\u0442\u0435 \u043d\u0430 ", "").replace("\u0413\u043b\u0435\u0434\u0430\u0439 ", "").trim()
        val href = this.attr("href").trim()
        if (title.isEmpty() || href.isEmpty()) return null

        val fullUrl = if (href.startsWith("http")) href else "$filmifenUrl/${href.trimStart('/')}"
        val poster = extractPoster(this)

        val year = this.selectFirst(".item-poster__year time")?.text()?.trim()?.toIntOrNull()
        val quality = this.selectFirst(".film-quality")?.text()?.trim()
        val isSeries = href.contains("/seriali/")

        return if (isSeries) {
            newTvSeriesSearchResponse(title, fullUrl, TvType.TvSeries) {
                this.posterUrl = poster
                this.year = year
                this.quality = getQualityFromString(quality)
            }
        } else {
            newMovieSearchResponse(title, fullUrl, TvType.Movie) {
                this.posterUrl = poster
                this.year = year
                this.quality = getQualityFromString(quality)
            }
        }
    }

    // ==========================================
    // 2. SEARCH
    // ==========================================
    override suspend fun search(query: String): List<SearchResponse> {
        val results = mutableListOf<SearchResponse>()
        val cleanQuery = query.trim()

        // 1. Search Live TV
        try {
            val liveHtml = app.get("$seirsandukUrl/", headers = liveHeaders).text
            val liveDoc = Jsoup.parse(liveHtml)
            liveDoc.select("#channels li a").forEach { el ->
                val title = el.text().trim()
                val href = el.attr("href")
                val img = el.select("img").attr("src")
                val posterUrl = getHighResLogo(title, img)

                if (title.isNotEmpty() && title.contains(cleanQuery, ignoreCase = true)) {
                    val fullUrl = if (href.startsWith("http")) href else "$seirsandukUrl/${href.trimStart('/')}"
                    results.add(
                        newLiveSearchResponse(
                            name = title,
                            url  = fullUrl,
                            type = TvType.Live
                        ) {
                            this.posterUrl = posterUrl
                        }
                    )
                }
            }
        } catch (e: Exception) { }

        // 2. Search Cinemeta Movies
        try {
            val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
            val searchUrl = "$mainUrl/catalog/movie/top/search=$encoded.json"
            val response = app.get(searchUrl, headers = commonHeaders).text
            val json = JSONObject(response)
            val metas = json.optJSONArray("metas") ?: JSONArray()

            for (i in 0 until metas.length()) {
                val meta = metas.optJSONObject(i) ?: continue
                val imdbId = meta.optString("id", "").trim()
                val name = meta.optString("name", "").trim()
                val poster = meta.optString("poster", "").trim()
                val yearStr = meta.optString("releaseInfo", "").trim()
                val year = yearStr.take(4).toIntOrNull()
                val imdbRating = meta.optString("imdbRating", "").trim()

                if (name.isEmpty() || imdbId.isEmpty()) continue

                val detailUrl = "$mainUrl/meta/movie/$imdbId.json"
                val displayName = if (imdbRating.isNotBlank()) "$name \u2b50$imdbRating" else name

                results.add(
                    newMovieSearchResponse(displayName, detailUrl, TvType.Movie) {
                        this.posterUrl = poster
                        this.year = year
                        this.quality = SearchQuality.HD
                    }
                )
            }
        } catch (e: Exception) { }

        // 3. Search Cinemeta Series
        try {
            val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
            val searchUrl = "$mainUrl/catalog/series/top/search=$encoded.json"
            val response = app.get(searchUrl, headers = commonHeaders).text
            val json = JSONObject(response)
            val metas = json.optJSONArray("metas") ?: JSONArray()

            for (i in 0 until metas.length()) {
                val meta = metas.optJSONObject(i) ?: continue
                val imdbId = meta.optString("id", "").trim()
                val name = meta.optString("name", "").trim()
                val poster = meta.optString("poster", "").trim()
                val yearStr = meta.optString("releaseInfo", "").trim()
                val year = yearStr.take(4).toIntOrNull()

                if (name.isEmpty() || imdbId.isEmpty()) continue

                val detailUrl = "$mainUrl/meta/series/$imdbId.json"

                results.add(
                    newTvSeriesSearchResponse(name, detailUrl, TvType.TvSeries) {
                        this.posterUrl = poster
                        this.year = year
                        this.quality = SearchQuality.HD
                    }
                )
            }
        } catch (e: Exception) { }

        // 4. Search Filmifen
        try {
            val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
            val searchUrl = "$filmifenUrl/index.php?do=search&subaction=search&story=$encoded"
            val movieHtml = app.get(searchUrl, headers = movieHeaders).text
            val movieDoc = Jsoup.parse(movieHtml)

            movieDoc.select(".item-poster, .item.grid-item, #dle-content a.item").forEach { el ->
                el.toFilmifenSearchResult()?.let { results.add(it) }
            }
        } catch (e: Exception) { }

        return results
    }

    // ==========================================
    // 3. LOAD (license check enforced here)
    // ==========================================
    override suspend fun load(url: String): LoadResponse {
        // >>> LICENSE CHECK: verify before loading any content details <<<
        LicenseManager.validateOrThrow()

        return if (url.contains("seirsanduk.online")) {
            loadLiveChannel(url)
        } else if (url.contains("cinemeta.strem.io") || url.contains("/meta/") || url.contains("bgflix://")) {
            loadCinemeta(url)
        } else {
            loadFilmifen(url)
        }
    }

    private suspend fun loadLiveChannel(url: String): LoadResponse {
        val html = app.get(url, headers = liveHeaders).text
        val doc = Jsoup.parse(html)
        val title = doc.select("div.buttonsLeft h1 a").text().trim().ifEmpty {
            doc.title().trim().ifEmpty { "BGFLIX \u0422\u0412" }
        }
        val poster = getHighResLogo(title, "")
        val description = doc.select("#program .nav").text().trim().ifEmpty {
            "BGFLIX \u0441\u0442\u0440\u0438\u0439\u043c\u0438\u043d\u0433 \u043d\u0430 \u0436\u0438\u0432\u043e."
        }

        return newLiveStreamLoadResponse(
            name    = title,
            url     = url,
            dataUrl = url
        ) {
            this.posterUrl = poster
            this.plot      = description
        }
    }

    private suspend fun loadCinemeta(url: String): LoadResponse {
        val metaUrl = if (url.startsWith("http") && url.contains("/meta/")) {
            url
        } else {
            val imdbMatch = Regex("""tt\d+""").find(url)?.value ?: ""
            val type = if (url.contains("series")) "series" else "movie"
            "$mainUrl/meta/$type/$imdbMatch.json"
        }

        val response = app.get(metaUrl, headers = commonHeaders).text
        val json = JSONObject(response)
        val meta = json.getJSONObject("meta")

        val name = meta.optString("name", "Film")
        val poster = meta.optString("poster", "")
        val background = meta.optString("background", "")
        val description = meta.optString("description", "")
        val yearStr = (meta.optString("year", "").ifBlank { meta.optString("releaseInfo", "") }).trim()
        val year = yearStr.take(4).toIntOrNull()
        val runtime = meta.optString("runtime", "")
        val imdbRating = meta.optString("imdbRating", "")
        val imdbId = meta.optString("id", "").ifBlank { Regex("""tt\d+""").find(metaUrl)?.value ?: "" }
        val type = meta.optString("type", "movie")

        val genres = mutableListOf<String>()
        val genresArr = meta.optJSONArray("genres") ?: meta.optJSONArray("genre")
        if (genresArr != null) {
            for (i in 0 until genresArr.length()) {
                genres.add(genresArr.optString(i))
            }
        }

        val cast = mutableListOf<String>()
        val castArr = meta.optJSONArray("cast")
        if (castArr != null) {
            for (i in 0 until minOf(castArr.length(), 10)) {
                cast.add(castArr.optString(i))
            }
        }

        val director = when (val dirObj = meta.opt("director")) {
            is JSONArray -> (0 until dirObj.length()).map { dirObj.optString(it) }.joinToString(", ")
            is String -> dirObj
            else -> ""
        }

        val tmdbId = meta.optInt("moviedb_id", 0)
        val dataPayload = "$imdbId|$tmdbId|$type|$name"

        val plot = buildString {
            if (imdbRating.isNotBlank()) append("\u2b50 IMDb: $imdbRating\n")
            if (runtime.isNotBlank()) append("\u23f1 $runtime\n")
            if (director.isNotBlank()) append("\ud83c\udfac \u0420\u0435\u0436\u0438\u0441\u044c\u043e\u0440: $director\n")
            if (cast.isNotEmpty()) append("\ud83c\udfad \u0410\u043a\u0442\u044c\u043e\u0440\u0438: ${cast.joinToString(", ")}\n")
            append("\n$description")
        }

        return if (type == "series") {
            val episodes = mutableListOf<Episode>()
            val videos = meta.optJSONArray("videos") ?: JSONArray()

            for (i in 0 until videos.length()) {
                val video = videos.optJSONObject(i) ?: continue
                val season = video.optInt("season", 0)
                val episode = video.optInt("episode", 0)
                val epName = video.optString("name", "Episode $episode")
                val thumbnail = video.optString("thumbnail", "")

                if (season <= 0 || episode <= 0) continue

                val epData = "$imdbId|$tmdbId|series|$season|$episode|$name"

                episodes.add(
                    newEpisode(epData) {
                        this.name = epName
                        this.season = season
                        this.episode = episode
                        this.posterUrl = thumbnail.ifBlank { null }
                    }
                )
            }

            episodes.sortWith(compareBy({ it.season }, { it.episode }))

            newTvSeriesLoadResponse(name, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.backgroundPosterUrl = background
                this.plot = plot
                this.year = year
                this.tags = genres
            }
        } else {
            newMovieLoadResponse(name, url, TvType.Movie, dataPayload) {
                this.posterUrl = poster
                this.backgroundPosterUrl = background
                this.plot = plot
                this.year = year
                this.tags = genres
            }
        }
    }

    private suspend fun loadFilmifen(url: String): LoadResponse {
        val html = app.get(url, headers = movieHeaders).text
        val doc = Jsoup.parse(html)

        val title = doc.selectFirst("h1.pmovie__title")?.text()?.trim()
            ?: doc.selectFirst("h1")?.text()?.trim()
            ?: "\u0424\u0438\u043b\u043c"

        val poster = doc.selectFirst(".pmovie__img")?.let { extractPoster(it) }
            ?: doc.selectFirst(".pmovie")?.let { extractPoster(it) }
            ?: doc.selectFirst("meta[property='og:image']")?.attr("content")

        val plot = doc.selectFirst(".pmovie__text")?.text()?.trim()
            ?: doc.selectFirst(".page__text")?.text()?.trim()

        val year = doc.selectFirst(".pmovie-info a[href*='/year/']")?.text()?.trim()?.toIntOrNull()
        val tags = doc.select(".pmovie-info a[href*='/filmi/'], .pmovie-info a[href*='/seriali/']").map { it.text().trim() }

        val isSeries = url.contains("/seriali/")

        val imdbId = doc.selectFirst("a[href*='imdb.com/title/']")?.attr("href")?.let {
            Regex("""tt\d+""").find(it)?.value
        }

        return if (isSeries) {
            val episodes = mutableListOf<Episode>()
            val epLinks = doc.select("a[href*='/seriali/']")
            epLinks.forEach { epLink ->
                val epTitle = epLink.text().trim()
                val epHref = epLink.attr("href").trim()
                if (epHref.isNotEmpty() && epTitle.isNotEmpty() && epHref != url) {
                    val fullEpHref = if (epHref.startsWith("http")) epHref else "$filmifenUrl/${epHref.trimStart('/')}"
                    val epMatch = Regex("""season-(\d+)-episode-(\d+)""").find(fullEpHref)
                    episodes.add(
                        newEpisode(fullEpHref) {
                            this.name = epTitle
                            this.season = epMatch?.groupValues?.get(1)?.toIntOrNull()
                            this.episode = epMatch?.groupValues?.get(2)?.toIntOrNull()
                        }
                    )
                }
            }

            if (episodes.isEmpty()) {
                episodes.add(
                    newEpisode(url) {
                        this.name = title
                    }
                )
            }

            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot      = plot
                this.year      = year
                this.tags      = tags
            }
        } else {
            val dataPayload = if (!imdbId.isNullOrBlank()) {
                "filmifen|$url|$imdbId|$title"
            } else {
                "filmifen|$url||$title"
            }

            newMovieLoadResponse(title, url, TvType.Movie, dataPayload) {
                this.posterUrl = poster
                this.plot      = plot
                this.year      = year
                this.tags      = tags
            }
        }
    }

    // ==========================================
    // 4. LOAD LINKS (SEEK-SAFE HLS STREAMING + AUTO BG SUBTITLES)
    //    License check enforced here as the primary gate
    // ==========================================

    private suspend fun fetchBulgarianSubtitles(
        imdbId: String,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit
    ) {
        try {
            val subUrl = if (season != null && episode != null) {
                "https://bulgarian-subs-addon.onrender.com/subtitles/series/$imdbId:$season:$episode.json"
            } else {
                "https://bulgarian-subs-addon.onrender.com/subtitles/movie/$imdbId.json"
            }
            val subResp = app.get(subUrl, timeout = 4).text
            val json = JSONObject(subResp)
            val subArr = json.optJSONArray("subtitles") ?: return
            for (i in 0 until subArr.length()) {
                val item = subArr.optJSONObject(i) ?: continue
                val rawSubUrl = item.optString("url")
                val subName = item.optString("name", "\u0411\u044a\u043b\u0433\u0430\u0440\u0441\u043a\u0438")
                val subId = item.optString("id", "")
                val srcTag = when {
                    subId.contains("sab", ignoreCase = true) -> "Subs.sab"
                    subId.contains("unacs", ignoreCase = true) -> "Subsunacs"
                    subId.contains("land", ignoreCase = true) -> "SubsLand"
                    else -> "BG"
                }
                if (rawSubUrl.isNotBlank()) {
                    subtitleCallback.invoke(
                        SubtitleFile(
                            lang = "\u0411\u044a\u043b\u0433\u0430\u0440\u0441\u043a\u0438 ($srcTag - $subName)",
                            url  = rawSubUrl
                        )
                    )
                }
            }
        } catch (e: Exception) { }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        // >>> LICENSE CHECK: verify before playing any stream <<<
        LicenseManager.validateOrThrow()

        var foundAny = false

        if (data.contains("seirsanduk.online")) {
            foundAny = loadLiveLinks(data, callback)
        } else if (data.startsWith("filmifen|")) {
            foundAny = loadFilmifenLinks(data, subtitleCallback, callback)
        } else {
            foundAny = loadHDLinks(data, subtitleCallback, callback)
        }

        return foundAny
    }

    private suspend fun loadLiveLinks(data: String, callback: (ExtractorLink) -> Unit): Boolean {
        var foundAny = false
        val streamHeaders = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Referer"    to "$seirsandukUrl/",
            "Origin"     to seirsandukUrl,
            "Accept"     to "*/*"
        )
        val m3u8Regex = Regex("""file:\s*['"](https?://[^'"]+\.m3u8[^'"]*)['"]\s*""")

        try {
            val defaultHtml = app.get(data, headers = liveHeaders).text
            val defaultStream = m3u8Regex.find(defaultHtml)?.groupValues?.get(1)
            if (!defaultStream.isNullOrEmpty()) {
                callback.invoke(
                    newExtractorLink(
                        source  = name,
                        name    = "$name - \u041e\u0441\u043d\u043e\u0432\u0435\u043d \u041f\u043e\u0442\u043e\u043a (HD)",
                        url     = defaultStream,
                        type    = ExtractorLinkType.M3U8
                    ) {
                        this.referer = "$seirsandukUrl/"
                        this.headers = streamHeaders
                        this.quality = Qualities.P1080.value
                    }
                )
                foundAny = true
            }

            val channelId = Uri.parse(data).getQueryParameter("id")
            if (!channelId.isNullOrEmpty()) {
                try {
                    val p2Html = app.get("$seirsandukUrl/?player=12&id=$channelId&pass=", headers = liveHeaders).text
                    val p2Stream = m3u8Regex.find(p2Html)?.groupValues?.get(1)
                    if (!p2Stream.isNullOrEmpty() && p2Stream != defaultStream) {
                        callback.invoke(
                            newExtractorLink(
                                source  = name,
                                name    = "$name - \u0420\u0435\u0437\u0435\u0440\u0432\u0435\u043d \u041f\u043e\u0442\u043e\u043a 1",
                                url     = p2Stream,
                                type    = ExtractorLinkType.M3U8
                            ) {
                                this.referer = "$seirsandukUrl/"
                                this.headers = streamHeaders
                                this.quality = Qualities.P1080.value
                            }
                        )
                        foundAny = true
                    }
                } catch (e: Exception) { }

                try {
                    val p3Html = app.get("$seirsandukUrl/?player=13&id=$channelId&pass=", headers = liveHeaders).text
                    val p3Stream = m3u8Regex.find(p3Html)?.groupValues?.get(1)
                    if (!p3Stream.isNullOrEmpty() && p3Stream != defaultStream) {
                        callback.invoke(
                            newExtractorLink(
                                source  = name,
                                name    = "$name - \u0420\u0435\u0437\u0435\u0440\u0432\u0435\u043d \u041f\u043e\u0442\u043e\u043a 2",
                                url     = p3Stream,
                                type    = ExtractorLinkType.M3U8
                            ) {
                                this.referer = "$seirsandukUrl/"
                                this.headers = streamHeaders
                                this.quality = Qualities.P1080.value
                            }
                        )
                        foundAny = true
                    }
                } catch (e: Exception) { }
            }
        } catch (e: Exception) { }

        return foundAny
    }
    /**
     * Direct HLS streaming - zero torrents, instant playback.
     * Source 1: RecTV (primary - authentic 1920x1040 Full HD HLS via authenticated API)
     * Source 2: VixSrc (fallback - 1080p HLS when RecTV has no match)
     * All streams are direct HTTP/HLS - no magnet links, no Torrest needed.
     */
    private val recTvBase    = "https://a.psrectv80.xyz"
    private val recTvKey     = "4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452"
    private val recTvHmacKey = "3508611138826751fdf77beaa6f93eb93fd27e6a5acb910e7aad22665513dd6e"
    private val recTvAppVer  = "110"
    private val recTvClient  = "rectv-android"

    override fun getVideoInterceptor(extractorLink: ExtractorLink): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            val modifiedRequest = originalRequest.newBuilder()
                .removeHeader("If-None-Match")
                .header("User-Agent", "googleusercontent")
                .build()
            chain.proceed(modifiedRequest)
        }
    }

    private fun sha256Hex(data: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(data.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun hmacSha256Hex(key: String, message: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(message.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    private fun signedRecHeaders(method: String, path: String, body: String = ""): Map<String, String> {
        val ts        = (System.currentTimeMillis() / 1000L).toString()
        val nonce     = UUID.randomUUID().toString()
        val bodyHash  = sha256Hex(body)
        val message   = "$method\n$path\n$ts\n$nonce\n$bodyHash"
        val signature = hmacSha256Hex(recTvHmacKey, message)
        
        return mapOf(
            "User-Agent"    to "googleusercontent",
            "Referer"       to "https://twitter.com/",
            "X-Timestamp"   to ts,
            "X-Nonce"       to nonce,
            "X-Signature"   to signature,
            "X-App-Version" to recTvAppVer,
            "X-Client-Id"   to recTvClient
        )
    }

    private suspend fun loadRecTvLinks(
        title: String,
        type: String,
        season: Int?,
        episode: Int?,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        if (title.isBlank()) return false
        val cleanTitle = title.replace(Regex("""⭐.*"""), "")
            .replace(Regex("""\(\d{4}\)"""), "")
            .trim()
        if (cleanTitle.isBlank()) return false

        val queries = mutableListOf(cleanTitle)
        val baseTitle = cleanTitle.substringBefore(":").substringBefore("-").trim()
        if (baseTitle != cleanTitle && baseTitle.length >= 3) {
            queries.add(baseTitle)
        }

        for (query in queries) {
            try {
                val encTitle = URLEncoder.encode(query, "UTF-8")
                val path = "/api/search/$encTitle/$recTvKey/"
                val headers = signedRecHeaders("GET", path)
                val searchResp = app.get("$recTvBase$path", headers = headers, timeout = 6).text
                if (searchResp.isBlank()) continue

                val searchJson = JSONObject(searchResp)
                val posters = searchJson.optJSONArray("posters") ?: continue
                if (posters.length() == 0) continue

                for (i in 0 until minOf(posters.length(), 4)) {
                    val item = posters.optJSONObject(i) ?: continue
                    val recId = item.optInt("id", -1)
                    val recType = item.optString("type", "")
                    if (recId < 0) continue

                    if (type == "series" && season != null && episode != null) {
                        val sPath = "/api/season/by/serie/$recId/$recTvKey/"
                        val sHeaders = signedRecHeaders("GET", sPath)
                        val sResp = app.get("$recTvBase$sPath", headers = sHeaders, timeout = 6).text
                        val seasons = JSONArray(sResp)
                        for (s in 0 until seasons.length()) {
                            val seasonObj = seasons.optJSONObject(s) ?: continue
                            val sTitle = seasonObj.optString("title", "")
                            val sNum = Regex("""\d+""").find(sTitle)?.value?.toIntOrNull()
                            if (sNum == season || seasons.length() == 1) {
                                val eps = seasonObj.optJSONArray("episodes") ?: continue
                                for (e in 0 until eps.length()) {
                                    val epObj = eps.optJSONObject(e) ?: continue
                                    val epTitle = epObj.optString("title", "")
                                    val epNum = Regex("""\d+""").find(epTitle)?.value?.toIntOrNull()
                                    if (epNum == episode) {
                                        val sources = epObj.optJSONArray("sources") ?: continue
                                        if (emitRecTvSources(sources, callback)) return true
                                    }
                                }
                            }
                        }
                    } else if (type != "series" && recType != "serie") {
                        val mPath = "/api/movie/by/$recId/$recTvKey/"
                        val mHeaders = signedRecHeaders("GET", mPath)
                        val mResp = app.get("$recTvBase$mPath", headers = mHeaders, timeout = 6).text
                        val mJson = JSONObject(mResp)
                        val sources = mJson.optJSONArray("sources") ?: continue
                        if (sources.length() > 0) {
                            if (emitRecTvSources(sources, callback)) return true
                        }
                    }
                }
            } catch (e: Exception) { }
        }
        return false
    }

    private suspend fun emitRecTvSources(sources: JSONArray, callback: (ExtractorLink) -> Unit): Boolean {
        var found = false
        for (i in 0 until sources.length()) {
            val src = sources.optJSONObject(i) ?: continue
            val srcUrl = src.optString("url", "")
            val srcType = src.optString("type", "m3u8")
            val srcTitle = src.optString("title", "1080p Ultra HD").ifBlank { "1080p Ultra HD" }
            if (srcUrl.isBlank()) continue

            val linkType = if (srcType == "mp4") ExtractorLinkType.VIDEO else ExtractorLinkType.M3U8
            callback.invoke(
                newExtractorLink(
                    source = name,
                    name = "$name - RecTV $srcTitle (1080p)",
                    url = srcUrl,
                    type = linkType
                ) {
                    this.referer = "https://twitter.com/"
                    this.headers = mapOf(
                        "Referer" to "https://twitter.com/",
                        "User-Agent" to "googleusercontent"
                    )
                    this.quality = Qualities.P1080.value
                }
            )
            found = true
        }
        return found
    }

    private suspend fun loadHDLinks(
        data: String,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        var foundAny = false

        val imdbId = Regex("""tt\d+""").find(data)?.value ?: ""
        if (imdbId.isBlank()) return false

        val parts = data.split("|")
        val type = if (parts.getOrNull(2) == "movie") "movie" else if (data.contains("series")) "series" else "movie"
        val season = parts.getOrNull(3)?.toIntOrNull()
        val episode = parts.getOrNull(4)?.toIntOrNull()
        val rawTitle = if (type == "series") parts.getOrNull(5) else parts.getOrNull(3)

        // Get movie title for RecTV lookup
        val title = if (!rawTitle.isNullOrBlank()) rawTitle else {
            try {
                val metaResp = app.get("$mainUrl/meta/$type/$imdbId.json", headers = commonHeaders, timeout = 3).text
                JSONObject(metaResp).getJSONObject("meta").optString("name", "")
            } catch (e: Exception) { "" }
        }

        // 1. RecTV — Primary high-quality source (1920x1040 Full HD direct HLS, instant load)
        try {
            val recFound = loadRecTvLinks(title, type, season, episode, callback)
            if (recFound) foundAny = true
        } catch (e: Exception) { }

        // 2. Fetch Bulgarian subtitles (fast timeout so it never blocks playback)
        try {
            fetchBulgarianSubtitles(imdbId, season, episode, subtitleCallback)
        } catch (e: Exception) { }

        // 3. VixSrc Direct High-Speed HLS Streams (Fallback when RecTV has no match)
        if (!foundAny) {
        try {
            val vixHeaders = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
                "Referer" to "https://vixsrc.to/"
            )
            val vixApiUrl = if (type == "series" && season != null && episode != null) {
                "https://vixsrc.to/api/tv/$imdbId/$season/$episode"
            } else {
                "https://vixsrc.to/api/movie/$imdbId"
            }

            val vixResp = app.get(vixApiUrl, headers = vixHeaders, timeout = 10).text
            val vixJson = JSONObject(vixResp)
            val embedPath = vixJson.optString("src", "")
            if (embedPath.isNotBlank()) {
                val embedUrl = if (embedPath.startsWith("http")) embedPath else "https://vixsrc.to$embedPath"
                val embedHtml = app.get(embedUrl, headers = vixHeaders, timeout = 10).text

                val tokenMatch = Regex("""["']token["']:\s*["']([^"']+)["']""").find(embedHtml)?.groupValues?.get(1)
                val expiresMatch = Regex("""["']expires["']:\s*["']([^"']+)["']""").find(embedHtml)?.groupValues?.get(1)
                val urlMatch = Regex("""url:\s*["']([^"']+)["']""").find(embedHtml)?.groupValues?.get(1)

                if (!tokenMatch.isNullOrBlank() && !expiresMatch.isNullOrBlank() && !urlMatch.isNullOrBlank()) {
                    val m3u8Url = "$urlMatch.m3u8?token=$tokenMatch&expires=$expiresMatch&h=1&lang=en"
                    val streamHeaders = mapOf(
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
                        "Referer" to embedUrl
                    )

                    // Pass master M3U8 directly - do NOT use M3u8Helper.generateM3u8
                    // The master playlist points variants to vixsrc.to, so ExoPlayer
                    // resolves the AES-128 key URI /storage/enc.key correctly
                    callback.invoke(
                        newExtractorLink(
                            source = name,
                            name = "$name - 1080p Ultra HD",
                            url = m3u8Url,
                            type = ExtractorLinkType.M3U8
                        ) {
                            this.referer = embedUrl
                            this.headers = streamHeaders
                            this.quality = Qualities.P1080.value
                        }
                    )
                    foundAny = true
                }
            }
        } catch (e: Exception) { }
        } // end VixSrc fallback

        // 4. VidSrc.me (secondary fallback)
        if (!foundAny) {
            try {
                val vsHeaders = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
                    "Referer" to "https://vidsrc.me/"
                )
                val vsType = if (type == "series") "tv" else "movie"
                val vsApiUrl = if (type == "series" && season != null && episode != null) {
                    "https://vidsrc.me/vs_src.php?type=$vsType&id=$imdbId&season=$season&episode=$episode"
                } else {
                    "https://vidsrc.me/vs_src.php?type=$vsType&id=$imdbId"
                }
                val vsResp = app.get(vsApiUrl, headers = vsHeaders, timeout = 8).text
                val vsJson = JSONObject(vsResp)
                val vsSrc = vsJson.optString("src", "")
                if (vsSrc.isNotBlank()) {
                    // vsSrc is an embed URL - try to extract streams from it
                    try {
                        val embedHeaders = mapOf(
                            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
                            "Referer" to "https://vidsrc.me/"
                        )
                        val embedHtml = app.get(vsSrc, headers = embedHeaders, timeout = 10).text

                        // Try to find m3u8 URLs in the embed page
                        val m3u8Pattern = Regex("""(https?://[^\s"'<>]+\.m3u8[^\s"'<>]*)""")
                        val m3u8Matches = m3u8Pattern.findAll(embedHtml)
                        for (m3u8Match in m3u8Matches) {
                            val streamUrl = m3u8Match.groupValues[1]
                            callback.invoke(
                                newExtractorLink(
                                    source = name,
                                    name = "$name - HD Stream",
                                    url = streamUrl,
                                    type = ExtractorLinkType.M3U8
                                ) {
                                    this.referer = vsSrc
                                    this.headers = embedHeaders
                                    this.quality = Qualities.P720.value
                                }
                            )
                            foundAny = true
                        }

                        // Also try loadExtractor for known hosts
                        if (!foundAny) {
                            val iframePattern = Regex("""<iframe[^>]+src=["']([^"']+)["']""")
                            val iframeMatches = iframePattern.findAll(embedHtml)
                            for (iframeMatch in iframeMatches) {
                                var iframeSrc = iframeMatch.groupValues[1]
                                if (iframeSrc.startsWith("//")) iframeSrc = "https:$iframeSrc"
                                if (iframeSrc.isNotBlank() && iframeSrc.startsWith("http")) {
                                    loadExtractor(iframeSrc, vsSrc, subtitleCallback, callback)
                                    foundAny = true
                                }
                            }
                        }
                    } catch (e: Exception) { }
                }
            } catch (e: Exception) { }
        }

        // 5. 2embed.cc Fallback (wide catalog coverage)
        if (!foundAny) {
            try {
                val embedHeaders = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
                    "Referer" to "https://www.2embed.cc/"
                )
                val embedUrl = if (type == "series" && season != null && episode != null) {
                    "https://www.2embed.cc/embedtv/$imdbId&s=$season&e=$episode"
                } else {
                    "https://www.2embed.cc/embed/$imdbId"
                }
                val embedHtml = app.get(embedUrl, headers = embedHeaders, timeout = 8).text

                // Extract iframe sources from 2embed
                val iframePattern = Regex("""<iframe[^>]+src=["']([^"']+)["']""")
                val iframeMatches = iframePattern.findAll(embedHtml)
                for (iframeMatch in iframeMatches) {
                    var iframeSrc = iframeMatch.groupValues[1]
                    if (iframeSrc.startsWith("//")) iframeSrc = "https:$iframeSrc"
                    if (iframeSrc.isNotBlank() && iframeSrc.startsWith("http") && !iframeSrc.contains("2embed.cc/embed")) {
                        loadExtractor(iframeSrc, embedUrl, subtitleCallback, callback)
                        foundAny = true
                    }
                }
            } catch (e: Exception) { }
        }

        return foundAny
    }


    private suspend fun loadFilmifenLinks(
        data: String,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        var foundAny = false
        val parts = data.split("|")
        val filmifenUrl2 = parts.getOrElse(1) { "" }
        val imdbId = parts.getOrElse(2) { "" }
        val movieTitle = parts.getOrElse(3) { "" }

        if (filmifenUrl2.isBlank()) return false

        if (imdbId.isNotBlank() || movieTitle.isNotBlank()) {
            val hdPayload = "$imdbId||movie|$movieTitle"
            val hdFound = loadHDLinks(hdPayload, subtitleCallback, callback)
            if (hdFound) foundAny = true
        }

        try {
            val html = app.get(filmifenUrl2, headers = movieHeaders).text
            val playerDictRegex = Regex("""(?s)(?:const|var|let)?\s*[a-zA-Z0-9_]*\s*=\s*\{([^}]+(?:trailer|vdn|okr|voe|fmo)[^}]+)\}""")
            val dictMatch = playerDictRegex.find(html)

            if (dictMatch != null) {
                val block = dictMatch.groupValues[1]
                val urlPattern = Regex("""([a-zA-Z0-9_]+)\s*:\s*["']([^"']+)["']""")
                val matches = urlPattern.findAll(block)

                for (m in matches) {
                    val key = m.groupValues[1]
                    var targetUrl = m.groupValues[2].trim()

                    if (targetUrl.isEmpty() || key == "trailer") continue
                    if (targetUrl.startsWith("//")) targetUrl = "https:$targetUrl"

                    if (targetUrl.contains("ok.ru")) {
                        loadExtractor(targetUrl.replace("videoembed", "video"), filmifenUrl, subtitleCallback, callback)
                        foundAny = true
                    } else if (targetUrl.contains("voe.sx") || targetUrl.contains("voe.")) {
                        loadExtractor(targetUrl, filmifenUrl, subtitleCallback, callback)
                        foundAny = true
                    } else if (targetUrl.contains("filemoon") || targetUrl.contains("bysesukior.com")) {
                        loadExtractor(targetUrl, filmifenUrl, subtitleCallback, callback)
                        foundAny = true
                    } else if (targetUrl.contains("vidon.to")) {
                        try {
                            val vidonHtml = app.get(targetUrl, headers = mapOf("Referer" to "$filmifenUrl/")).text
                            val m3u8Match = Regex("""https?://[^\s"'<>]+\.m3u8[^\s"'<>]*""").find(vidonHtml)
                            if (m3u8Match != null) {
                                callback.invoke(
                                    newExtractorLink(
                                        source = name,
                                        name = "$name - BG \u0410\u0443\u0434\u0438\u043e (Vidon)",
                                        url = m3u8Match.value,
                                        type = ExtractorLinkType.M3U8
                                    ) {
                                        this.referer = targetUrl
                                        this.quality = Qualities.P720.value
                                    }
                                )
                                foundAny = true
                            }
                        } catch (e: Exception) { }
                    } else {
                        loadExtractor(targetUrl, filmifenUrl, subtitleCallback, callback)
                        foundAny = true
                    }
                }
            }

            val doc = Jsoup.parse(html)
            doc.select("iframe[src]").forEach { iframe ->
                var src = iframe.attr("src").trim()
                if (src.startsWith("//")) src = "https:$src"
                if (src.isNotEmpty() && !src.contains("youtube") && !src.contains("google")) {
                    loadExtractor(src, filmifenUrl, subtitleCallback, callback)
                    foundAny = true
                }
            }
        } catch (e: Exception) { }

        return foundAny
    }
}


