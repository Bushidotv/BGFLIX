package com.bgflix

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lagradost.cloudstream3.plugins.Plugin

class BlankFragment(val plugin: Plugin) : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val id = plugin.resources?.getIdentifier("fragment_blank", "layout", "com.bgflix") ?: 0
        return if (id != 0) {
            inflater.inflate(id, container, false)
        } else {
            null
        }
    }
}
