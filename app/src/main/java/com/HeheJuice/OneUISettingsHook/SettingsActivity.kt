package com.HeheJuice.OneUISettingsHook

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView

class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 64, 64, 64)
        }

        val titleTv = TextView(this).apply {
            text = "OneUI Settings Hook"
            textSize = 22f
            setPadding(0, 0, 0, 16)
        }

        val subTitleTv = TextView(this).apply {
            text = "Module is active and hooks are applied via LSPosed."
            textSize = 14f
            setPadding(0, 0, 0, 48)
        }

        val githubBtn = TextView(this).apply {
            text = "🌐 View GitHub Repository"
            textSize = 16f
            setPadding(0, 24, 0, 24)
            setOnClickListener {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/HeheJuice/OneUI-Settings-Patch")
                )
                startActivity(intent)
            }
        }

        rootLayout.addView(titleTv)
        rootLayout.addView(subTitleTv)
        rootLayout.addView(githubBtn)

        setContentView(rootLayout)
    }
}
