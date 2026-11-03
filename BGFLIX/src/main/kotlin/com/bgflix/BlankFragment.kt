package com.bgflix

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Button
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lagradost.cloudstream3.plugins.Plugin

class BlankFragment(val plugin: Plugin) : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Initialize LicenseManager with context
        LicenseManager.init(requireContext())

        // Build the settings UI programmatically for reliability
        val ctx = requireContext()
        val padding = (16 * ctx.resources.displayMetrics.density).toInt()

        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        // Title
        val title = TextView(ctx).apply {
            text = "BGFLIX License Settings"
            textSize = 20f
            setPadding(0, 0, 0, padding / 2)
        }
        layout.addView(title)

        // HWID display (read-only)
        val hwidLabel = TextView(ctx).apply {
            text = "Device HWID: ${LicenseManager.getHwid()}"
            textSize = 12f
            setPadding(0, 0, 0, padding / 2)
        }
        layout.addView(hwidLabel)

        // License key input
        val licenseInput = EditText(ctx).apply {
            hint = "Enter your license key"
            setText(LicenseManager.getLicenseKey())
            setSingleLine(true)
        }
        layout.addView(licenseInput)

        // Save button
        val saveButton = Button(ctx).apply {
            text = "Save License Key"
            setOnClickListener {
                val key = licenseInput.text.toString().trim()
                if (key.isBlank()) {
                    Toast.makeText(ctx, "License key cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                LicenseManager.saveLicenseKey(key)
                Toast.makeText(ctx, "License key saved!", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
        layout.addView(saveButton)

        return layout
    }
}
