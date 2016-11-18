/**
 * Author: UCU Knight Coders on 11/12/2016.
 * Website: http://facebook.com/teamucuccs
 * E-mail: teamucuccs@gmail.com
 */

package edu.ucuccs.nutrivision;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Map;


class PrefManager {
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private SharedPreferences prefs_recent;
    private static final String PREF_RECENT_SEARCHES = "prefs_recent";
    private static final String PREF_NAME = "prefs_intro";
    private static final String IS_FIRST_TIME_LAUNCH = "prefs_first_time_launch";

    PrefManager(Context context) {
        int PRIVATE_MODE = 0;
        pref = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        prefs_recent = context.getSharedPreferences(PREF_RECENT_SEARCHES, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    void setFirstTimeLaunch(boolean isFirstTime) {
        editor.putBoolean(IS_FIRST_TIME_LAUNCH, isFirstTime);
        editor.commit();
    }

    boolean isFirstTimeLaunch() {
        return pref.getBoolean(IS_FIRST_TIME_LAUNCH, true);
    }

    /*
    * Preferences for Food recent searches
     */
    void insertRecentItem(String keyword) {
        prefs_recent.edit().putString(keyword, keyword).commit();
    }

    ArrayList getAllRecentSearches() {
        Map<String,?> keys = prefs_recent.getAll();
        ArrayList<Integer> arrRecentItems = new ArrayList<>();

        for(Map.Entry<String,?> entry : keys.entrySet()){
            arrRecentItems.add(Integer.parseInt(entry.getKey()));
        }
        return arrRecentItems;
    }
}
