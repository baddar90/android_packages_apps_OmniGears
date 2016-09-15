/*
 *  Copyright (C) 2016 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
*/
package org.omnirom.omnigears.interfacesettings;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.UserManager;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v14.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.logging.MetricsProto.MetricsEvent;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class GlobalActionsSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener, Indexable {
    private static final String TAG = "GlobalActionsSettings";
    private static final String POWER_MENU_ANIMATIONS = "power_menu_animations";
//   private static final String GLOBAL_ACTIONS = "global_actions";

    private LinkedHashMap<String, Boolean> mGlobalActionsMap;
//    private PreferenceScreen prefScreen = null;

    ListPreference mPowerMenuAnimations;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.OMNI_SETTINGS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.global_actions);

        final PreferenceScreen prefScreen = getPreferenceScreen();
      //  prefScreen = (PreferenceScreen)findPreference(GLOBAL_ACTIONS);
        final ContentResolver contentResolver = getContext().getContentResolver();

        final String[] defaultActions = getContext().getResources().getStringArray(
                com.android.internal.R.array.config_globalActionsList);
        final List<String> defaultActionsList = Arrays.asList(defaultActions);

        final String[] allActions = getContext().getResources().getStringArray(
                com.android.internal.R.array.values_globalActionsList);
        final List<String> allActionssList = new ArrayList<String>(Arrays.asList(allActions));

        final String enabledActions = Settings.System.getString(contentResolver,
                Settings.System.GLOBAL_ACTIONS_LIST);

        mPowerMenuAnimations = (ListPreference) findPreference(POWER_MENU_ANIMATIONS);
        mPowerMenuAnimations.setValue(String.valueOf(Settings.System.getInt(
                getContentResolver(), Settings.System.POWER_MENU_ANIMATIONS, 0)));
        mPowerMenuAnimations.setSummary(mPowerMenuAnimations.getEntry());
        mPowerMenuAnimations.setOnPreferenceChangeListener(this);

        List<String> enabledActionsList = null;
        if (enabledActions != null) {
            enabledActionsList = Arrays.asList(enabledActions.split(","));
        }

        mGlobalActionsMap = new LinkedHashMap<String, Boolean>();
        for (String actionKey : allActions) {
            if (enabledActionsList != null) {
                mGlobalActionsMap.put(actionKey, enabledActionsList.contains(actionKey));
            } else {
                mGlobalActionsMap.put(actionKey, defaultActionsList.contains(actionKey));
            }
        }
        final UserManager um = (UserManager) getContext().getSystemService(Context.USER_SERVICE);
        boolean multiUser = um.isUserSwitcherEnabled();
        int count = prefScreen.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            Preference p = prefScreen.getPreference(i);
            if (p instanceof SwitchPreference) {
                SwitchPreference action = (SwitchPreference) p;
                String key = action.getKey();
                if (!allActionssList.contains(key)) {
                    prefScreen.removePreference(action);
                    continue;
                }
                if (key.equals("users") && !multiUser) {
                    prefScreen.removePreference(action);
                    continue;
                }
                action.setChecked(mGlobalActionsMap.get(key));
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference instanceof SwitchPreference) {
            SwitchPreference action = (SwitchPreference) preference;
            mGlobalActionsMap.put(action.getKey(), action.isChecked());

            List<String> enabledActionsList = new ArrayList<String>();
            for (String actionKey : mGlobalActionsMap.keySet()) {
                Boolean checked = mGlobalActionsMap.get(actionKey);
                if (checked) {
                    enabledActionsList.add(actionKey);
                }
            }
            setList(enabledActionsList);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    private void setList(List<String> actionList) {
        final ContentResolver contentResolver = getContext().getContentResolver();
        Settings.System.putString(contentResolver, Settings.System.GLOBAL_ACTIONS_LIST,
                TextUtils.join(",", actionList));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
         ContentResolver resolver = getActivity().getContentResolver();
         boolean result = false;
         if (preference == mPowerMenuAnimations) {
            Settings.System.putInt(getContentResolver(), Settings.System.POWER_MENU_ANIMATIONS,
                    Integer.valueOf((String) objValue));
            mPowerMenuAnimations.setValue(String.valueOf(objValue));
            mPowerMenuAnimations.setSummary(mPowerMenuAnimations.getEntry());
            return true;
          }
          return result;
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.global_actions;
                    result.add(sir);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();
                    return result;
                }
            };
}