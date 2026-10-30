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
    override var mainUrl = "https://filmifen.com"
    private val seirsandukUrl = "https://www.seirsanduk.online"
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
        "$mainUrl/filmi/" to "Нови Филми",
        "$mainUrl/filmi-bg-audio/" to "Филми с БГ Аудио",
        "$mainUrl/filmi/ekshan/" to "Екшън Филми",
        "$mainUrl/filmi/komediya/" to "Комедии",
        "$mainUrl/filmi/trilari/" to "Трилъри",
        "$mainUrl/filmi/fantastika/" to "Фантастика",
        "$mainUrl/filmi/ujasi/" to "Ужаси",
        "$mainUrl/filmi/detski/" to "Детски и Анимации",
        "$mainUrl/seriali/" to "Сериали"
    )

    private val defaultPoster = "https://raw.githubusercontent.com/Bushidotv/BGFLIX/master/icon.png"
    private val rawLogoBase = "https://raw.githubusercontent.com/Bushidotv/BGFLIX/master/logos_hd"

    private val liveHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Referer"    to "$seirsandukUrl/",
        "Origin"     to seirsandukUrl,
        "Accept"     to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
    )

    private val movieHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Referer"    to "$mainUrl/",
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
            else -> "$mainUrl/${clean.trimStart('/')}"
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
    // 1. MAIN PAGE (LIVE CHANNELS FIRST, THEN MOVIES)
    // ==========================================
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        if (request.data.startsWith("live_")) {
            // Live TV Row (from Seirsanduk)
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
        } else {
            // Movies / Series Row (from Filmifen)
            val url = if (page <= 1) {
                request.data
            } else {
                if (request.data.endsWith("/")) "${request.data}page/$page/" else "${request.data}/page/$page/"
            }

            val html = app.get(url, headers = movieHeaders).text
            val doc = Jsoup.parse(html)
            val items = doc.select(".item-poster, .item.grid-item, #dle-content a.item").mapNotNull { el ->
                el.toMovieSearchResult()
            }

            val hasNext = doc.select(".pages-block a, .navigation a, a[href*='/page/']").any {
                it.text().trim() == "${page + 1}" || it.attr("href").contains("/page/${page + 1}/")
            }

            return newHomePageResponse(
                HomePageList(request.name, items),
                hasNext = hasNext
            )
        }
    }

    private fun Element.toMovieSearchResult(): SearchResponse? {
        val title = this.selectFirst(".item-poster__title")?.text()?.trim()
            ?: this.attr("aria-label").replace("Гледайте на ", "").replace("Гледай ", "").trim()
        val href = this.attr("href").trim()
        if (title.isEmpty() || href.isEmpty()) return null

        val fullUrl = if (href.startsWith("http")) href else "$mainUrl/${href.trimStart('/')}"
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
    // 2. SEARCH (BOTH LIVE TV & MOVIES)
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

        // 2. Search Filmifen Movies & Series
        try {
            val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
            val searchUrl = "$mainUrl/index.php?do=search&subaction=search&story=$encoded"
            val movieHtml = app.get(searchUrl, headers = movieHeaders).text
            val movieDoc = Jsoup.parse(movieHtml)

            movieDoc.select(".item-poster, .item.grid-item, #dle-content a.item").forEach { el ->
                el.toMovieSearchResult()?.let { results.add(it) }
            }
        } catch (e: Exception) { }

        return results
    }

    // ==========================================
    // 3. LOAD (LIVE CHANNEL OR MOVIE / SERIES)
    // ==========================================
    override suspend fun load(url: String): LoadResponse {
        if (url.contains("seirsanduk.online")) {
            // Live TV Channel
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
        } else {
            // Movie or TV Series from Filmifen
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

            return if (isSeries) {
                // TV Series
                val episodes = mutableListOf<Episode>()
                val epLinks = doc.select("a[href*='/seriali/']")
                epLinks.forEach { epLink ->
                    val epTitle = epLink.text().trim()
                    val epHref = epLink.attr("href").trim()
                    if (epHref.isNotEmpty() && epTitle.isNotEmpty() && epHref != url) {
                        val fullEpHref = if (epHref.startsWith("http")) epHref else "$mainUrl/${epHref.trimStart('/')}"
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
                // Movie
                newMovieLoadResponse(title, url, TvType.Movie, url) {
                    this.posterUrl = poster
                    this.plot      = plot
                    this.year      = year
                    this.tags      = tags
                }
            }
        }
    }

    // ==========================================
    // 4. LOAD LINKS (LIVE HLS OR MOVIE SERVERS)
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
            // Live TV Streams
            val streamHeaders = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "Referer"    to "$seirsandukUrl/",
                "Origin"     to seirsandukUrl,
                "Accept"     to "*/*"
            )
            val m3u8Regex = Regex("""file:\s*['"](https?://[^'"]+\.m3u8[^'"]*)['"]""")

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
        } else {
            // Movie / Series Streams (Multi-Server: Filmi2k 1080p + Filmifen BG Dub + Global HD + Auto BG Subs)
            try {
                val html = app.get(data, headers = movieHeaders).text
                val doc = Jsoup.parse(html)

                // 1. Extract IMDb ID & Episode numbers for Auto Bulgarian Subtitles
                val imdbId = doc.selectFirst("a[href*='imdb.com/title/']")?.attr("href")?.let {
                    Regex("""tt\d+""").find(it)?.value
                }

                val isSeries = data.contains("/seriali/")
                val epMatch = Regex("""season-(\d+)-episode-(\d+)""").find(data)
                val season = epMatch?.groupValues?.get(1)?.toIntOrNull()
                val episode = epMatch?.groupValues?.get(2)?.toIntOrNull()

                // Trigger automatic Bulgarian subtitles from Subsunacs / Subs.sab / Subsland
                if (!imdbId.isNullOrBlank()) {
                    fetchBulgarianSubtitles(imdbId, season, episode, subtitleCallback)
                }

                val title = doc.selectFirst("h1.pmovie__title")?.text()?.trim()
                    ?: doc.selectFirst("h1")?.text()?.trim()
                    ?: ""
                val cleanTitle = title.replace(Regex("""\(\d{4}\).*"""), "").replace(Regex("""[^\p{L}\p{N}\s]"""), "").trim()

                // 2. SERVER 1: Filmi2k Native 1080p Full HD (WEB-DL with high-bitrate stream)
                if (!isSeries && cleanTitle.isNotBlank()) {
                    try {
                        val searchTerms = listOfNotNull(
                            cleanTitle,
                            doc.selectFirst("a.pmovie-info__imdb-link")?.attr("title")?.substringAfter(" - ")?.trim()
                        ).distinct()

                        for (term in searchTerms) {
                            val searchUrl = "https://www.filmi2k.com/?s=" + URLEncoder.encode(term, "UTF-8")
                            val searchHtml = app.get(searchUrl, headers = mapOf("User-Agent" to "Mozilla/5.0")).text
                            val matchLink = Regex("""href="(https://www\.filmi2k\.com/[^"/]+-[0-9]{4}/)"""").find(searchHtml)?.groupValues?.get(1)
                            if (matchLink != null) {
                                val filmiHtml = app.get(matchLink, headers = mapOf("User-Agent" to "Mozilla/5.0")).text
                                val rtpMatch = Regex("""src="(/?[^"]*rtp=[^"]*)"""").find(filmiHtml)?.groupValues?.get(1)
                                if (rtpMatch != null) {
                                    val rtpUrl = if (rtpMatch.startsWith("http")) rtpMatch else "https://www.filmi2k.com/${rtpMatch.trimStart('/')}"
                                    val rtpPage = app.get(rtpUrl, headers = mapOf("Referer" to matchLink, "User-Agent" to "Mozilla/5.0")).text
                                    val unpacked = getPacked(rtpPage) ?: rtpPage
                                    val m3u8Match = Regex("""https?://[^\s"'<>]+\.m3u8[^\s"'<>]*""").find(unpacked)
                                    if (m3u8Match != null) {
                                        callback.invoke(
                                            newExtractorLink(
                                                source = name,
                                                name   = "$name - 1080p Full HD (Filmi2k)",
                                                url    = m3u8Match.value,
                                                type   = ExtractorLinkType.M3U8
                                            ) {
                                                this.referer = rtpUrl
                                                this.quality = Qualities.P1080.value
                                            }
                                        )
                                        foundAny = true
                                        break
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) { }
                }

                // 3. SERVER 2: Filmifen (БГ Аудио / БГ Превод)
                // Find player dictionary in page scripts: {trailer:"...", vdn:"...", okr:"...", fmo:"...", voe:"..."}
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
                            loadExtractor(targetUrl.replace("videoembed", "video"), mainUrl, subtitleCallback, callback)
                            foundAny = true
                        } else if (targetUrl.contains("voe.sx") || targetUrl.contains("voe.")) {
                            loadExtractor(targetUrl, mainUrl, subtitleCallback, callback)
                            foundAny = true
                        } else if (targetUrl.contains("filemoon") || targetUrl.contains("bysesukior.com")) {
                            loadExtractor(targetUrl, mainUrl, subtitleCallback, callback)
                            foundAny = true
                        } else if (targetUrl.contains("vidon.to")) {
                            // Extract from Vidon embed
                            try {
                                val vidonHtml = app.get(targetUrl, headers = mapOf("Referer" to "$mainUrl/")).text
                                val m3u8Match = Regex("""https?://[^\s"'<>]+\.m3u8[^\s"'<>]*""").find(vidonHtml)
                                if (m3u8Match != null) {
                                    callback.invoke(
                                        newExtractorLink(
                                            source = name,
                                            name = "$name - Vidon (HD)",
                                            url = m3u8Match.value,
                                            type = ExtractorLinkType.M3U8
                                        ) {
                                            this.referer = targetUrl
                                            this.quality = Qualities.P1080.value
                                        }
                                    )
                                    foundAny = true
                                }
                            } catch (e: Exception) { }
                        } else {
                            loadExtractor(targetUrl, mainUrl, subtitleCallback, callback)
                            foundAny = true
                        }
                    }
                }

                // 4. SERVER 3: Global HD Embed Fallback
                if (!imdbId.isNullOrBlank()) {
                    try {
                        val embedUrl = if (season != null && episode != null) {
                            "https://www.2embed.cc/embedtv/$imdbId&s=$season&e=$episode"
                        } else {
                            "https://www.2embed.cc/embed/$imdbId"
                        }
                        val embedHtml = app.get(embedUrl, headers = mapOf("User-Agent" to "Mozilla/5.0", "Referer" to "https://www.2embed.cc/")).text
                        val streamSrc = Regex("""go\(['"]([^'"]+)['"]\)""").find(embedHtml)?.groupValues?.get(1)
                            ?: Regex("""data-src=['"]([^'"]+)['"]""").find(embedHtml)?.groupValues?.get(1)
                        if (streamSrc != null) {
                            loadExtractor(streamSrc, embedUrl, subtitleCallback, callback)
                            foundAny = true
                        }
                    } catch (e: Exception) { }
                }

                // 5. Fallback: check all iframes / direct links
                doc.select("iframe[src]").forEach { iframe ->
                    var src = iframe.attr("src").trim()
                    if (src.startsWith("//")) src = "https:$src"
                    if (src.isNotEmpty() && !src.contains("youtube") && !src.contains("google")) {
                        loadExtractor(src, mainUrl, subtitleCallback, callback)
                        foundAny = true
                    }
                }
            } catch (e: Exception) { }
        }

        return foundAny
    }
}
