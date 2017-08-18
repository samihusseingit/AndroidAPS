package info.nightscout.androidaps.plugins.ProfileLocal;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.nightscout.androidaps.Config;
import info.nightscout.androidaps.Constants;
import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.interfaces.PluginBase;
import info.nightscout.androidaps.interfaces.ProfileInterface;
import info.nightscout.androidaps.plugins.NSClientInternal.data.NSProfile;
import info.nightscout.utils.SP;

/**
 * Created by mike on 05.08.2016.
 */
public class LocalProfilePlugin implements PluginBase, ProfileInterface {
    private static Logger log = LoggerFactory.getLogger(LocalProfilePlugin.class);

    private static boolean fragmentEnabled = false;
    private static boolean fragmentVisible = true;

    private static NSProfile convertedProfile = null;

    final private String DEFAULTARRAY = "[{\"timeAsSeconds\":0,\"value\":0}]";

    boolean mgdl;
    boolean mmol;
    Double dia;
    JSONArray ic;
    JSONArray isf;
    JSONArray basal;
    JSONArray targetLow;
    JSONArray targetHigh;

    public LocalProfilePlugin() {
        loadSettings();
    }

    @Override
    public String getFragmentClass() {
        return LocalProfileFragment.class.getName();
    }

    @Override
    public int getType() {
        return PluginBase.PROFILE;
    }

    @Override
    public String getName() {
        return MainApp.instance().getString(R.string.localprofile);
    }

    @Override
    public String getNameShort() {
        String name = MainApp.sResources.getString(R.string.localprofile_shortname);
        if (!name.trim().isEmpty()) {
            //only if translation exists
            return name;
        }
        // use long name as fallback
        return getName();
    }

    @Override
    public boolean isEnabled(int type) {
        return type == PROFILE && fragmentEnabled;
    }

    @Override
    public boolean isVisibleInTabs(int type) {
        return type == PROFILE && fragmentVisible;
    }

    @Override
    public boolean canBeHidden(int type) {
        return true;
    }

    @Override
    public boolean hasFragment() {
        return true;
    }

    @Override
    public boolean showInList(int type) {
        return true;
    }

    @Override
    public void setFragmentEnabled(int type, boolean fragmentEnabled) {
        if (type == PROFILE) this.fragmentEnabled = fragmentEnabled;
    }

    @Override
    public void setFragmentVisible(int type, boolean fragmentVisible) {
        if (type == PROFILE) this.fragmentVisible = fragmentVisible;
    }

    public void storeSettings() {
        if (Config.logPrefsChange)
            log.debug("Storing settings");
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainApp.instance().getApplicationContext());
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("LocalProfile" + "mmol", mmol);
        editor.putBoolean("LocalProfile" + "mgdl", mgdl);
        editor.putString("LocalProfile" + "dia", dia.toString());
        editor.putString("LocalProfile" + "ic", ic.toString());
        editor.putString("LocalProfile" + "isf", isf.toString());
        editor.putString("LocalProfile" + "basal", basal.toString());
        editor.putString("LocalProfile" + "targetlow", targetLow.toString());
        editor.putString("LocalProfile" + "targethigh", targetHigh.toString());

        editor.commit();
        createConvertedProfile();
    }

    private void loadSettings() {
        if (Config.logPrefsChange)
            log.debug("Loading stored settings");

        mgdl = SP.getBoolean("LocalProfile" + "mgdl", false);
        mmol = SP.getBoolean("LocalProfile" + "mmol", true);
        dia = SP.getDouble("LocalProfile" + "dia", Constants.defaultDIA);
        try {
            ic = new JSONArray(SP.getString("LocalProfile" + "ic", DEFAULTARRAY));
        } catch (JSONException e1) {
            try {
                ic = new JSONArray(DEFAULTARRAY);
            } catch (JSONException e2) {
            }
        }
        try {
            isf = new JSONArray(SP.getString("LocalProfile" + "isf", DEFAULTARRAY));
        } catch (JSONException e1) {
            try {
                isf = new JSONArray(DEFAULTARRAY);
            } catch (JSONException e2) {
            }
        }
        try {
            basal = new JSONArray(SP.getString("LocalProfile" + "basal", DEFAULTARRAY));
        } catch (JSONException e1) {
            try {
                basal = new JSONArray(DEFAULTARRAY);
            } catch (JSONException e2) {
            }
        }
        try {
            targetLow = new JSONArray(SP.getString("LocalProfile" + "targetlow", DEFAULTARRAY));
        } catch (JSONException e1) {
            try {
                targetLow = new JSONArray(DEFAULTARRAY);
            } catch (JSONException e2) {
            }
        }
        try {
            targetHigh = new JSONArray(SP.getString("LocalProfile" + "targethigh", DEFAULTARRAY));
        } catch (JSONException e1) {
            try {
                targetHigh = new JSONArray(DEFAULTARRAY);
            } catch (JSONException e2) {
            }
        }
        createConvertedProfile();
    }

    /*
        {
            "_id": "576264a12771b7500d7ad184",
            "startDate": "2016-06-16T08:35:00.000Z",
            "defaultProfile": "Default",
            "store": {
                "Default": {
                    "dia": "3",
                    "carbratio": [{
                        "time": "00:00",
                        "value": "30"
                    }],
                    "carbs_hr": "20",
                    "delay": "20",
                    "sens": [{
                        "time": "00:00",
                        "value": "100"
                    }],
                    "timezone": "UTC",
                    "basal": [{
                        "time": "00:00",
                        "value": "0.1"
                    }],
                    "target_low": [{
                        "time": "00:00",
                        "value": "0"
                    }],
                    "target_high": [{
                        "time": "00:00",
                        "value": "0"
                    }],
                    "startDate": "1970-01-01T00:00:00.000Z",
                    "units": "mmol"
                }
            },
            "created_at": "2016-06-16T08:34:41.256Z"
        }
        */
    void createConvertedProfile() {
        JSONObject json = new JSONObject();
        JSONObject store = new JSONObject();
        JSONObject profile = new JSONObject();

        try {
            json.put("defaultProfile", "LocalProfile");
            json.put("store", store);
            profile.put("dia", dia);
            profile.put("carbratio", ic);
            profile.put("sens", isf);
            profile.put("basal", basal);
            profile.put("target_low", targetLow);
            profile.put("target_high", targetHigh);
            profile.put("units", mgdl ? Constants.MGDL : Constants.MMOL);
            store.put("LocalProfile", profile);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        convertedProfile = new NSProfile(json, "LocalProfile");
    }

    @Override
    public NSProfile getProfile() {
        return convertedProfile;
    }

}
