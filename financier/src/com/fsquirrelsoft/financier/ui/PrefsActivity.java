package com.fsquirrelsoft.financier.ui;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.fsquirrelsoft.financier.context.Contexts;
import com.fsquirrelsoft.financier.core.R;

/**
 *
 * @author dennis
 *
 */
public class PrefsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    boolean dirty = false;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.prefs);
        setPrefSummary(Constants.PREFS_LAST_BACKUP);
        setPrefSummary(Constants.BACKUP_DIR);
    }

    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        setPrefSummary(Constants.PREFS_LAST_BACKUP);
        setPrefSummary(Constants.BACKUP_DIR);
    }

    protected void onPause() {
        super.onPause();
        if (dirty) {
            Contexts.instance().setPreferenceDirty();
        }
        dirty = false;
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        dirty = true;
    }

    private void setPrefSummary(String prefKey) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Preference p = findPreference(prefKey);
        if (p != null) {
            p.setSummary(sharedPreferences.getString(prefKey, getResources().getString(R.string.unknown)));
        }
    }
}
