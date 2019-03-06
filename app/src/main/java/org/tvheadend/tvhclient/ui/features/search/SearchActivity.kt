package org.tvheadend.tvhclient.ui.features.search

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.provider.SearchRecentSuggestions
import androidx.fragment.app.Fragment
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseActivity
import org.tvheadend.tvhclient.ui.features.programs.ProgramListFragment
import org.tvheadend.tvhclient.util.MiscUtils
import timber.log.Timber

class SearchActivity : BaseActivity(), StartSearchInterface {

    public override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(MiscUtils.getThemeId(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.misc_content_activity)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        handleIntent(intent)

        if (savedInstanceState == null) {
            // If a search was performed from a fragment the activity would call
            // the onSearchRequested(...) method of that fragment which will start
            // the SearchActivity (if implemented). Depending on the given search type
            // the corresponding fragment will be shown which will present the results.
            val type = intent.getStringExtra("type")
            var fragment: Fragment? = null
            when (type) {
                "program_guide" -> fragment = ProgramListFragment()
            }

            if (fragment != null) {
                fragment.arguments = intent.extras
                supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.main, fragment)
                        .commit()
            }
        }
    }

    override fun startSearch() {
        val fragment = supportFragmentManager.findFragmentById(R.id.main)
        if (fragment is SearchRequestInterface && fragment.isVisible) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            fragment.onSearchRequested(query)
        }
    }

    override fun onNewIntent(intent: Intent) {
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            Timber.d("Saving suggestion $query so it can be shown in the recent search history")
            val suggestions = SearchRecentSuggestions(this, SuggestionProvider.AUTHORITY, SuggestionProvider.MODE)
            suggestions.saveRecentQuery(query, null)
        }
    }
}