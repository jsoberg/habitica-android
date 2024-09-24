package com.habitrpg.android.habitica.ui.activities

import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.habitrpg.android.habitica.R
import com.habitrpg.android.habitica.ui.fragments.purchases.GemsPurchaseFragment
import com.habitrpg.android.habitica.ui.fragments.purchases.SubscriptionFragment
import com.habitrpg.android.habitica.ui.helpers.ToolbarColorHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GemPurchaseActivity : PurchaseActivity() {
    private var showSubscription: Boolean = false

    override fun getLayoutResId(): Int {
        return R.layout.activity_gem_purchase
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showSubscription = !(intent.extras?.containsKey("openSubscription") == true && intent.extras?.getBoolean("openSubscription") == false)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setupToolbar(toolbar, Color.WHITE, ContextCompat.getColor(this, R.color.brand_300))

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = ""

        if (showSubscription) {
            createFragment(true)
            toolbar.title = getString(R.string.subscription)
        } else {
            createFragment(false)
        }
    }

    override fun onStart() {
        super.onStart()
        if (showSubscription) {
            toolbar?.let { ToolbarColorHelper.colorizeToolbar(it, this,) }
        }
    }

    private fun createFragment(showSubscription: Boolean) {
        val fragment =
            if (showSubscription) {
                SubscriptionFragment()
            } else {
                GemsPurchaseFragment()
            }
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment as Fragment)
            .commit()
    }
}
