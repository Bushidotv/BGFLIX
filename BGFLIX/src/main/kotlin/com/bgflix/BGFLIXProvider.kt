package com.bgflix

import android.net.Uri
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.net.URLEncoder
import org.json.JSONObject
import org.json.JSONArray

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
        "live_all" to "Всички ТВ Канали",
        "live_sports" to "ТВ Канали: Спорт",
        "live_movies" to "ТВ Канали: Филми & Сериали",
        "live_kids" to "ТВ Канали: Детски",
        "cinemeta_movie_top" to "🎬 Популярни Филми (HD)",
        "cinemeta_movie_Action" to "💥 Екшън (HD)",
        "cinemeta_movie_Comedy" to "😂 Комедии (HD)",
        "cinemeta_movie_Horror" to "👻 Ужаси (HD)",
        "cinemeta_movie_Thriller" to "🔪 Трилъри (HD)",
        "cinemeta_movie_Sci-Fi" to "🚀 Фантастика (HD)",
        "cinemeta_movie_Drama" to "🎭 Драма (HD)",
        "cinemeta_movie_Animation" to "🧸 Анимация (HD)",
        "cinemeta_series_top" to "📺 Популярни Сериали (HD)",
        "cinemeta_series_Drama" to "📺 Сериали: Драма (HD)",
        "cinemeta_series_Comedy" to "📺 Сериали: Комедия (HD)",
        "filmifen_bg" to "🇧🇬 Филми с БГ Аудио"
    )

    private val defaultPoster = "https://raw.githubusercontent.com/Bushidotv/BGFLIX/master/icon.png"
    private val rawLogoBase = "https://raw.githubusercontent.com/Bushidotv/BGFLIX/master/logos_hd"

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
            // 1. BNT family
            lower.contains("bnt 1") -> "bnt1.png"
            lower.contains("bnt 2") -> "bnt2.png"
            lower.contains("bnt 3") -> "bnt3.png"
            lower.contains("bnt 4") -> "bnt4.png"

            // 2. bTV family
            lower.contains("btv action") -> "btv_action.png"
            lower.contains("btv cinema") -> "btv_cinema.png"
            lower.contains("btv comedy") -> "btv_comedy.png"
            lower.contains("btv story") -> "btv_story.png"
            lower.contains("btv") -> "btvhd.png"

            // 3. Nova family
            lower.contains("nova news") -> "nova_news.png"
            lower.contains("nova sport") -> "nova_sport.png"
            lower.contains("kino nova") -> "kino_nova.png"
            lower.contains("nova") -> "nova.png"

            // 4. Diema family
            lower.contains("diema sport 3") -> "diema_sport_3.png"
            lower.contains("diema sport 2") -> "diema_sport_2.png"
            lower.contains("diema sport") -> "diema_sport.png"
            lower.contains("diema family") -> "diema_family.png"
            lower.contains("diema") -> "diema.png"

            // 5. Max Sport family
            lower.contains("max sport 1") -> "MAX_SPORT1_HD.png"
            lower.contains("max sport 2") -> "MAX_SPORT2_HD.png"
            lower.contains("max sport 3") -> "MAX_SPORT3_HD.png"
            lower.contains("max sport 4") -> "MAX_SPORT4_HD.png"
            lower.contains("max one") -> "MAX_ONE_HD.png"

            // 6. Sports international
            lower.contains("eurosport 1") -> "eurosport1.png"
            lower.contains("eurosport 2") -> "eurosport2.png"
            lower.contains("ring") -> "RING_BG_HD.png"

            // 7. Star & AXN & Epic Drama
            lower.contains("star crime") -> "STAR_CRIME_HD.png"
            lower.contains("star life") -> "STAR_LIFE.png"
            lower.contains("star channel") || lower.contains("star") -> "star_channel.png"
            lower.contains("axn black") -> "axn_black.png"
            lower.contains("axn white") -> "axn_white.png"
            lower.contains("axn") -> "axn.png"
            lower.contains("epic drama") -> "EPICDRAMAHD.png"

            // 8. Documentary & Lifestyle
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

            // 9. Kids
            lower.contains("disney") -> "disney_channel.png"
            lower.contains("cartoon") -> "cartoon_network.png"
            lower.contains("nick jr") -> "nick_jr.png"
            lower.contains("nicktoons") -> "nicktoons.png"
            lower.contains("nickelodeon") -> "nickelodeon.png"
            lower.contains("e kids") || lower.contains("ekids") -> "EKIDS.png"

            // 10. News & Business
            lower.contains("bloomberg") -> "bloomberg.png"
            lower.contains("euronews") -> "euronews.png"

            // 11. Bulgarian National & Regional
            lower.contains("bulgaria on air") -> "bulgaria_on_air.png"
            lower.contains("kanal 3") -> "kanal3.png"
            lower.contains("78 tv") || lower.contains("7/8 tv") -> "78tv.png"
            lower.contains("skat") -> "skat.png"
            lower.contains("vtk") -> "vtk.png"
            lower.contains("evrokom") -> "evrokom.png"

            // 12. Music & Folk
            lower.contains("city") -> "citytv.png"
            lower.contains("the voice") -> "THE_VOICE.png"
            lower.contains("planeta") -> "PLANETA_HD.png"
            lower.contains("folklor") -> "RODINA_TV.png"
            lower.contains("tiankov") -> "TIANKOV_TV.png"
            lower.contains("rodina") -> "RODINA_TV.png"
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

    /**
     * Extracts poster image handling lazy-loading (data-src, data-srcset, srcset, src)
     * avoiding transparent 1x1 placeholder base64 gifs.
     */
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
    // 1. MAIN PAGE (LIVE CHANNELS + HD MOVIES FROM CINEMETA + BG AUDIO FROM FILMIFEN)
    // ==========================================
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        if (request.data.startsWith("live_")) {
            return getLiveChannelsPage(request)
        } else if (request.data.startsWith("cinemeta_")) {
            return getCinemetaPage(page, request)
        } else if (request.data == "filmifen_bg") {
            return getFilmifenBGPage(page, request)
        }

        return newHomePageResponse(HomePageList(request.name, emptyList()), hasNext = false)
    }

    /**
     * Live TV channels from Seirsanduk - unchanged from v4.
     */
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

    /**
     * HD Movies & Series from Cinemeta (Stremio catalog API).
     * Provides automatic pagination and genre filtering.
     * Every movie/series comes with IMDb ID for HD stream resolution.
     */
    private suspend fun getCinemetaPage(page: Int, request: MainPageRequest): HomePageResponse {
        // Parse request.data format: cinemeta_{type}_{category}
        // Examples: cinemeta_movie_top, cinemeta_movie_Action, cinemeta_series_top
        val parts = request.data.removePrefix("cinemeta_").split("_", limit = 2)
        val type = parts.getOrElse(0) { "movie" } // "movie" or "series"
        val category = parts.getOrElse(1) { "top" }

        val skip = if (page <= 1) 0 else (page - 1) * 50
        val catalogUrl = if (category == "top") {
            if (skip > 0) {
                "$mainUrl/catalog/$type/top/skip=$skip.json"
            } else {
                "$mainUrl/catalog/$type/top.json"
            }
        } else {
            // Genre filter
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

                // Encode IMDb data in the URL for load() to parse
                val dataUrl = "bgflix://cinemeta/$type/$imdbId"

                val displayName = if (imdbRating.isNotBlank()) {
                    "$name ⭐$imdbRating"
                } else {
                    name
                }

                if (type == "series") {
                    items.add(
                        newTvSeriesSearchResponse(displayName, dataUrl, TvType.TvSeries) {
                            this.posterUrl = poster
                            this.year = year
                            this.quality = SearchQuality.HD
                        }
                    )
                } else {
                    items.add(
                        newMovieSearchResponse(displayName, dataUrl, TvType.Movie) {
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

    /**
     * Bulgarian Audio movies from Filmifen (kept as bonus category).
     */
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
            ?: this.attr("aria-label").replace("Гледайте на ", "").replace("Гледай ", "").trim()
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
    // 2. SEARCH (LIVE TV + CINEMETA HD + FILMIFEN BG)
    // ==========================================
    override suspend fun search(query: String): List<SearchResponse> {
        val results = mutableListOf<SearchResponse>()
        val cleanQuery = query.trim()

        // 1. Search Live TV channels
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

        // 2. Search Cinemeta for HD Movies
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

                val dataUrl = "bgflix://cinemeta/movie/$imdbId"
                val displayName = if (imdbRating.isNotBlank()) "$name ⭐$imdbRating" else name

                results.add(
                    newMovieSearchResponse(displayName, dataUrl, TvType.Movie) {
                        this.posterUrl = poster
                        this.year = year
                        this.quality = SearchQuality.HD
                    }
                )
            }
        } catch (e: Exception) { }

        // 3. Search Cinemeta for HD Series
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

                val dataUrl = "bgflix://cinemeta/series/$imdbId"

                results.add(
                    newTvSeriesSearchResponse(name, dataUrl, TvType.TvSeries) {
                        this.posterUrl = poster
                        this.year = year
                        this.quality = SearchQuality.HD
                    }
                )
            }
        } catch (e: Exception) { }

        // 4. Search Filmifen for BG Audio content
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
    // 3. LOAD (LIVE CHANNEL, CINEMETA HD, OR FILMIFEN BG)
    // ==========================================
    override suspend fun load(url: String): LoadResponse {
        if (url.contains("seirsanduk.online")) {
            return loadLiveChannel(url)
        } else if (url.startsWith("bgflix://cinemeta/")) {
            return loadCinemeta(url)
        } else {
            return loadFilmifen(url)
        }
    }

    /**
     * Load a live TV channel detail page.
     */
    private suspend fun loadLiveChannel(url: String): LoadResponse {
        val html = app.get(url, headers = liveHeaders).text
        val doc = Jsoup.parse(html)
        val title = doc.select("div.buttonsLeft h1 a").text().trim().ifEmpty {
            doc.title().trim().ifEmpty { "BGFLIX ТВ" }
        }
        val poster = getHighResLogo(title, "")
        val description = doc.select("#program .nav").text().trim().ifEmpty {
            "BGFLIX Телевизия на живо."
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

    /**
     * Load movie/series details from Cinemeta API.
     * URL format: bgflix://cinemeta/{type}/{imdbId}
     */
    private suspend fun loadCinemeta(url: String): LoadResponse {
        val parts = url.removePrefix("bgflix://cinemeta/").split("/")
        val type = parts.getOrElse(0) { "movie" }
        val imdbId = parts.getOrElse(1) { "" }

        val metaUrl = "$mainUrl/meta/$type/$imdbId.json"
        val response = app.get(metaUrl, headers = commonHeaders).text
        val json = JSONObject(response)
        val meta = json.getJSONObject("meta")

        val name = meta.optString("name", "Film")
        val poster = meta.optString("poster", "")
        val background = meta.optString("background", "")
        val description = meta.optString("description", "")
        val year = meta.optString("year", "").toIntOrNull()
            ?: meta.optString("releaseInfo", "").take(4).toIntOrNull()
        val runtime = meta.optString("runtime", "")
        val imdbRating = meta.optString("imdbRating", "")
        val genres = mutableListOf<String>()
        val genresArr = meta.optJSONArray("genres")
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
        val director = meta.optString("director", "")

        // Get TMDB ID for embed providers
        val tmdbId = meta.optInt("moviedb_id", 0)

        // Build dataUrl containing all info needed for loadLinks
        // Format: imdbId|tmdbId|type
        val dataPayload = "$imdbId|$tmdbId|$type"

        val plot = buildString {
            if (imdbRating.isNotBlank()) append("⭐ IMDb: $imdbRating\n")
            if (runtime.isNotBlank()) append("⏱ $runtime\n")
            if (director.isNotBlank()) append("🎬 Режисьор: $director\n")
            if (cast.isNotEmpty()) append("🎭 Актьори: ${cast.joinToString(", ")}\n")
            append("\n$description")
        }

        return if (type == "series") {
            // Extract episodes from Cinemeta
            val episodes = mutableListOf<Episode>()
            val videos = meta.optJSONArray("videos") ?: JSONArray()

            for (i in 0 until videos.length()) {
                val video = videos.optJSONObject(i) ?: continue
                val season = video.optInt("season", 0)
                val episode = video.optInt("episode", 0)
                val epName = video.optString("name", "Episode $episode")
                val epId = video.optString("id", "")
                val thumbnail = video.optString("thumbnail", "")

                if (season <= 0 || episode <= 0) continue

                // Episode data: imdbId|tmdbId|series|season|episode
                val epData = "$imdbId|$tmdbId|series|$season|$episode"

                episodes.add(
                    newEpisode(epData) {
                        this.name = epName
                        this.season = season
                        this.episode = episode
                        this.posterUrl = thumbnail.ifBlank { null }
                    }
                )
            }

            // Sort episodes by season then episode
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

    /**
     * Load movie/series from Filmifen (BG Audio content).
     */
    private suspend fun loadFilmifen(url: String): LoadResponse {
        val html = app.get(url, headers = movieHeaders).text
        val doc = Jsoup.parse(html)

        val title = doc.selectFirst("h1.pmovie__title")?.text()?.trim()
            ?: doc.selectFirst("h1")?.text()?.trim()
            ?: "Филм"

        val poster = doc.selectFirst(".pmovie__img")?.let { extractPoster(it) }
            ?: doc.selectFirst(".pmovie")?.let { extractPoster(it) }
            ?: doc.selectFirst("meta[property='og:image']")?.attr("content")

        val plot = doc.selectFirst(".pmovie__text")?.text()?.trim()
            ?: doc.selectFirst(".page__text")?.text()?.trim()

        val year = doc.selectFirst(".pmovie-info a[href*='/year/']")?.text()?.trim()?.toIntOrNull()
        val tags = doc.select(".pmovie-info a[href*='/filmi/'], .pmovie-info a[href*='/seriali/']").map { it.text().trim() }

        val isSeries = url.contains("/seriali/")

        // Try to extract IMDb ID from Filmifen page for HD fallback
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
            // For Filmifen movies, pass imdbId if available for HD+subs, else pass URL
            val dataPayload = if (!imdbId.isNullOrBlank()) {
                "filmifen|$url|$imdbId"
            } else {
                "filmifen|$url|"
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
    // 4. LOAD LINKS (MULTI-SERVER HD STREAMS + AUTO BG SUBTITLES)
    // ==========================================

    /**
     * Fetches authentic Bulgarian subtitles from Subsunacs, Subs.sab.bz, and SubsLand.
     */
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
            val subResp = app.get(subUrl, timeout = 12).text
            val json = JSONObject(subResp)
            val subArr = json.optJSONArray("subtitles") ?: return
            for (i in 0 until subArr.length()) {
                val item = subArr.optJSONObject(i) ?: continue
                val rawSubUrl = item.optString("url")
                val subName = item.optString("name", "Български")
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
                            lang = "Български ($srcTag - $subName)",
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
        var foundAny = false

        if (data.contains("seirsanduk.online")) {
            // ========== LIVE TV STREAMS (unchanged) ==========
            foundAny = loadLiveLinks(data, callback)
        } else if (data.startsWith("filmifen|")) {
            // ========== FILMIFEN BG AUDIO CONTENT ==========
            foundAny = loadFilmifenLinks(data, subtitleCallback, callback)
        } else {
            // ========== CINEMETA HD CONTENT ==========
            foundAny = loadHDLinks(data, subtitleCallback, callback)
        }

        return foundAny
    }

    /**
     * Live TV stream extraction - unchanged from v4.
     */
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
                        name    = "$name - Основен Сървър (HD)",
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
                // Player 2
                try {
                    val p2Html = app.get("$seirsandukUrl/?player=12&id=$channelId&pass=", headers = liveHeaders).text
                    val p2Stream = m3u8Regex.find(p2Html)?.groupValues?.get(1)
                    if (!p2Stream.isNullOrEmpty() && p2Stream != defaultStream) {
                        callback.invoke(
                            newExtractorLink(
                                source  = name,
                                name    = "$name - Резервен Сървър 1",
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

                // Player 3
                try {
                    val p3Html = app.get("$seirsandukUrl/?player=13&id=$channelId&pass=", headers = liveHeaders).text
                    val p3Stream = m3u8Regex.find(p3Html)?.groupValues?.get(1)
                    if (!p3Stream.isNullOrEmpty() && p3Stream != defaultStream) {
                        callback.invoke(
                            newExtractorLink(
                                source  = name,
                                name    = "$name - Резервен Сървър 2",
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
     * HD stream loading for Cinemeta content.
     * Data format: imdbId|tmdbId|type[|season|episode]
     *
     * Multi-server approach:
     * 1. VidSrc.me - Primary HD server (supports IMDb IDs directly)
     * 2. 2Embed.cc - Secondary HD server (3 inner servers: vidsrc.buzz, videm.xyz, streamsrcs)
     * 3. VidSrc.net - Tertiary HD server
     * 4. Auto Bulgarian subtitles from Subsunacs/Subs.sab/SubsLand
     */
    private suspend fun loadHDLinks(
        data: String,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        var foundAny = false
        val parts = data.split("|")
        val imdbId = parts.getOrElse(0) { "" }
        val tmdbId = parts.getOrElse(1) { "0" }
        val type = parts.getOrElse(2) { "movie" }
        val season = parts.getOrElse(3) { "" }.toIntOrNull()
        val episode = parts.getOrElse(4) { "" }.toIntOrNull()

        if (imdbId.isBlank()) return false

        // Always fetch Bulgarian subtitles
        fetchBulgarianSubtitles(imdbId, season, episode, subtitleCallback)

        // ========== SERVER 1: VidSrc.me (Primary HD) ==========
        try {
            val vidsrcUrl = if (type == "series" && season != null && episode != null) {
                "https://vidsrc.me/embed/tv?imdb=$imdbId&season=$season&episode=$episode"
            } else {
                "https://vidsrc.me/embed/movie?imdb=$imdbId"
            }
            val vidsrcHtml = app.get(vidsrcUrl, headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "Referer" to "https://vidsrc.me/"
            )).text

            // VidSrc.me uses an inner iframe with the actual player
            val iframeSrc = Regex("""id=['"]?player_iframe['"]?\s+src=['"]([^'"]+)['"]""").find(vidsrcHtml)?.groupValues?.get(1)
                ?: Regex("""<iframe[^>]+src=['"]([^'"]+)['"]""").find(vidsrcHtml)?.groupValues?.get(1)

            if (iframeSrc != null) {
                val fullIframeUrl = when {
                    iframeSrc.startsWith("http") -> iframeSrc
                    iframeSrc.startsWith("//") -> "https:$iframeSrc"
                    else -> "https://vidsrc.me$iframeSrc"
                }
                loadExtractor(fullIframeUrl, "https://vidsrc.me/", subtitleCallback, callback)
                foundAny = true
            }
        } catch (e: Exception) { }

        // ========== SERVER 2: 2Embed.cc (3 sub-servers) ==========
        try {
            val embedUrl = if (type == "series" && season != null && episode != null) {
                "https://www.2embed.cc/embedtv/$imdbId&s=$season&e=$episode"
            } else {
                "https://www.2embed.cc/embed/$imdbId"
            }
            val embedHtml = app.get(embedUrl, headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                "Referer" to "https://www.2embed.cc/"
            )).text

            // Extract go() calls - these are the actual server URLs
            val goUrls = Regex("""go\(['"]([^'"]+)['"]\)""").findAll(embedHtml).map { it.groupValues[1] }.toList()
            // Also get data-src
            val dataSrcs = Regex("""data-src=['"]([^'"]+)['"]""").findAll(embedHtml).map { it.groupValues[1] }.toList()

            val allServerUrls = (goUrls + dataSrcs).distinct()

            for (serverUrl in allServerUrls) {
                try {
                    val fullUrl = when {
                        serverUrl.startsWith("http") -> serverUrl
                        serverUrl.startsWith("//") -> "https:$serverUrl"
                        else -> "https://www.2embed.cc$serverUrl"
                    }
                    loadExtractor(fullUrl, "https://www.2embed.cc/", subtitleCallback, callback)
                    foundAny = true
                } catch (e: Exception) { }
            }
        } catch (e: Exception) { }

        // ========== SERVER 3: VidSrc.net (Fallback HD) ==========
        try {
            val vidsrcNetUrl = if (type == "series" && season != null && episode != null) {
                "https://vidsrc.net/embed/tv/$imdbId/$season/$episode"
            } else {
                "https://vidsrc.net/embed/movie/$imdbId"
            }
            val vidsrcNetHtml = app.get(vidsrcNetUrl, headers = commonHeaders).text

            // Look for iframe or redirect
            val iframeSrc = Regex("""<iframe[^>]+src=['"]([^'"]+)['"]""").find(vidsrcNetHtml)?.groupValues?.get(1)
            if (iframeSrc != null) {
                val fullUrl = when {
                    iframeSrc.startsWith("http") -> iframeSrc
                    iframeSrc.startsWith("//") -> "https:$iframeSrc"
                    else -> "https://vidsrc.net$iframeSrc"
                }
                loadExtractor(fullUrl, "https://vidsrc.net/", subtitleCallback, callback)
                foundAny = true
            }

            // Also check for direct m3u8/mp4 links
            val directLinks = Regex("""(?:src|file|source)\s*[:=]\s*['"]?(https?://[^\s'"<>]+\.(?:m3u8|mp4)[^\s'"<>]*)""").findAll(vidsrcNetHtml)
            for (link in directLinks) {
                callback.invoke(
                    newExtractorLink(
                        source = name,
                        name = "$name - VidSrc HD",
                        url = link.groupValues[1],
                        type = if (link.groupValues[1].contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
                    ) {
                        this.referer = "https://vidsrc.net/"
                        this.quality = Qualities.P1080.value
                    }
                )
                foundAny = true
            }
        } catch (e: Exception) { }

        // ========== SERVER 4: VidSrc.sh (Extra HD) ==========
        if (tmdbId != "0" && tmdbId.isNotBlank()) {
            try {
                val vidsrcShUrl = if (type == "series" && season != null && episode != null) {
                    "https://vidsrc.sh/embed/tv/$tmdbId/$season/$episode"
                } else {
                    "https://vidsrc.sh/embed/movie/$tmdbId"
                }
                val vidsrcShHtml = app.get(vidsrcShUrl, headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                    "Referer" to "https://vidsrc.sh/"
                )).text

                val iframeSrc = Regex("""<iframe[^>]+src=['"]([^'"]+)['"]""").find(vidsrcShHtml)?.groupValues?.get(1)
                    ?: Regex("""id=['"]?player_iframe['"]?\s+src=['"]([^'"]+)['"]""").find(vidsrcShHtml)?.groupValues?.get(1)

                if (iframeSrc != null) {
                    val fullUrl = when {
                        iframeSrc.startsWith("http") -> iframeSrc
                        iframeSrc.startsWith("//") -> "https:$iframeSrc"
                        else -> "https://vidsrc.sh$iframeSrc"
                    }
                    loadExtractor(fullUrl, "https://vidsrc.sh/", subtitleCallback, callback)
                    foundAny = true
                }
            } catch (e: Exception) { }
        }

        // ========== SERVER 5: SmashyStream (Bonus HD) ==========
        try {
            val smashyUrl = if (type == "series" && season != null && episode != null) {
                "https://embed.smashystream.com/playere.php?imdb=$imdbId&season=$season&episode=$episode"
            } else {
                "https://embed.smashystream.com/playere.php?imdb=$imdbId"
            }
            val smashyHtml = app.get(smashyUrl, headers = mapOf(
                "User-Agent" to "Mozilla/5.0",
                "Referer" to "https://embed.smashystream.com/"
            )).text

            // SmashyStream often has direct file URLs or nested iframes
            val m3u8Links = Regex("""https?://[^\s"'<>]+\.m3u8[^\s"'<>]*""").findAll(smashyHtml)
            for (link in m3u8Links) {
                callback.invoke(
                    newExtractorLink(
                        source = name,
                        name = "$name - SmashyStream HD",
                        url = link.value,
                        type = ExtractorLinkType.M3U8
                    ) {
                        this.referer = smashyUrl
                        this.quality = Qualities.P1080.value
                    }
                )
                foundAny = true
            }

            // Also try iframe extraction
            val iframeSrcs = Regex("""<iframe[^>]+src=['"]([^'"]+)['"]""").findAll(smashyHtml)
            for (iframe in iframeSrcs) {
                val src = iframe.groupValues[1]
                if (src.isNotBlank() && !src.contains("youtube") && !src.contains("google")) {
                    val fullSrc = if (src.startsWith("//")) "https:$src" else src
                    loadExtractor(fullSrc, smashyUrl, subtitleCallback, callback)
                    foundAny = true
                }
            }
        } catch (e: Exception) { }

        return foundAny
    }

    /**
     * Load streams from Filmifen (BG Audio content with optional HD fallback).
     * Data format: filmifen|url|imdbId
     */
    private suspend fun loadFilmifenLinks(
        data: String,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        var foundAny = false
        val parts = data.split("|")
        val filmifenUrl2 = parts.getOrElse(1) { "" }
        val imdbId = parts.getOrElse(2) { "" }

        if (filmifenUrl2.isBlank()) return false

        // If we have IMDb ID, also try HD sources + subtitles
        if (imdbId.isNotBlank()) {
            fetchBulgarianSubtitles(imdbId, null, null, subtitleCallback)

            // Try to get TMDB ID from Cinemeta for extra HD sources
            var tmdbId = "0"
            try {
                val metaResp = app.get("$mainUrl/meta/movie/$imdbId.json", headers = commonHeaders).text
                val metaJson = JSONObject(metaResp)
                tmdbId = metaJson.getJSONObject("meta").optInt("moviedb_id", 0).toString()
            } catch (e: Exception) { }

            // Load HD sources as well
            loadHDLinks("$imdbId|$tmdbId|movie", subtitleCallback, callback)
        }

        // Load Filmifen's own BG Audio sources
        try {
            val html = app.get(filmifenUrl2, headers = movieHeaders).text
            val doc = Jsoup.parse(html)

            // Find player dictionary in page scripts
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

                    // Load via appropriate extractor
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
                                        name = "$name - BG Аудио (Vidon)",
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

            // Fallback: check all iframes
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
