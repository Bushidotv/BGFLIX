package com.bgflix

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class BGFLIXPlugin: Plugin() {
    private var activity: AppCompatActivity? = null

    companion object {
        const val LICENSE_PREF_KEY = "bgflix_license_key"
    }

    override fun load(context: Context) {
        activity = context as? AppCompatActivity

        registerMainAPI(BGFLIXProvider())

        openSettings = {
            val frag = BlankFragment(this)
            activity?.let {
                frag.show(it.supportFragmentManager, "BGFLIXSettings")
            }
        }
    }
}