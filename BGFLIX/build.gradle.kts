dependencies {
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}

version = 5

cloudstream {
    description = "BGFLIX - Български Телевизионни Канали, Филми и Сериали."
    authors = listOf("BGFLIX")

    /**
    * Status int as one of the following:
    * 0: Down
    * 1: Ok
    * 2: Slow
    * 3: Beta-only
    **/
    status = 1

    tvTypes = listOf("Live", "Movie", "TvSeries")

    requiresResources = true
    language = "bg"

    iconUrl = "https://raw.githubusercontent.com/Bushidotv/BGFLIX/master/icon.png"
}

android {
    namespace = "com.bgflix"

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}
