/*
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.tiefensuche.soundcrowd.ui.preferences

import android.os.Bundle
import androidx.preference.*
import com.tiefensuche.soundcrowd.R
import com.tiefensuche.soundcrowd.service.PluginManager
import com.tiefensuche.soundcrowd.ui.MediaBrowserFragment
import com.tiefensuche.soundcrowd.utils.Utils

class PreferenceFragment : PreferenceFragmentCompat() {

    private fun addPluginPreferences() {
        for ((name, plugin) in PluginManager.plugins) {
            val category = PreferenceCategory(activity)
            category.key = name
            category.title = name
            category.isIconSpaceReserved = false
            preferenceScreen.addPreference(category)
            for (preference in plugin.preferences()) {
                if (preference is EditTextPreference) {
                    preference.dialogLayoutResource =
                        if (preference.title == getString(R.string.preference_password_title))
                            R.layout.preference_dialog_edittext_password
                        else R.layout.preference_dialog_edittext
                }
                category.addPreference(preference)
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, p1: String?) {
        (activity as? MediaBrowserFragment.MediaFragmentListener)?.let {
            it.setToolbarTitle(getString(R.string.preferences_title))
            it.enableCollapse(false)
            it.showSearchButton(false)
        }
        preferenceScreen = preferenceManager.createPreferenceScreen(activity)
        addPluginPreferences()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        for (plugin in PluginManager.plugins.values) {
            for (preference in plugin.preferences()) {
                preference.parent?.removePreference(preference)
            }
        }
    }
}