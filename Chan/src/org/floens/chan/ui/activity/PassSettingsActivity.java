package org.floens.chan.ui.activity;

import org.floens.chan.ChanApplication;
import org.floens.chan.R;
import org.floens.chan.core.ChanPreferences;
import org.floens.chan.core.manager.ReplyManager;
import org.floens.chan.core.manager.ReplyManager.PassResponse;
import org.floens.chan.core.model.Pass;
import org.floens.chan.utils.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;

public class PassSettingsActivity extends Activity implements OnCheckedChangeListener {
    private static PassSettingsActivity instance;

    private Switch enableSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        instance = this;

        setFragment(ChanPreferences.getPassEnabled());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        instance = null;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        if (TextUtils.isEmpty(ChanPreferences.getPassId())) {
            ChanPreferences.setPassEnabled(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action_bar_switch, menu);

        enableSwitch = (Switch) menu.findItem(R.id.action_bar_switch).getActionView();
        enableSwitch.setOnCheckedChangeListener(this);
        enableSwitch.setPadding(0, 0, Utils.dp(14), 0);

        setSwitch(ChanPreferences.getPassEnabled());

        return true;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        setFragment(isChecked);
        setSwitch(isChecked);
    }

    private void setSwitch(boolean enabled) {
        enableSwitch.setChecked(enabled);
        ChanPreferences.setPassEnabled(enabled);
    }

    private void setFragment(boolean enabled) {
        if (enabled) {
            FragmentTransaction t = getFragmentManager().beginTransaction();
            t.replace(android.R.id.content, new PassSettingsFragment());
            t.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            t.commit();
        } else {
            FragmentTransaction t = getFragmentManager().beginTransaction();
            t.replace(android.R.id.content, new TextFragment());
            t.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            t.commit();
        }
    }

    public static class TextFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup group, Bundle savedInstanceState) {
            View container = inflater.inflate(R.layout.preference_pass, null);

            TextView link = (TextView) container.findViewById(R.id.pass_link);
            link.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Utils.openLink(v.getContext(), v.getContext().getString(R.string.pass_info_link));
                }
            });

            return container;
        }
    }

    public static class PassSettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preference_pass);

            Preference login = findPreference("preference_pass_login");
            login.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (PassSettingsActivity.instance != null) {
                        Pass pass = new Pass(ChanPreferences.getPassToken(), ChanPreferences.getPassPin());
                        onLoginClick(pass);
                    }
                    return true;
                }
            });

            updateLoginButton();
        }

        private void updateLoginButton() {
            findPreference("preference_pass_login").setTitle(TextUtils.isEmpty(ChanPreferences.getPassId()) ? R.string.pass_login : R.string.pass_logout);
        }

        private void onLoginClick(Pass pass) {
            if (TextUtils.isEmpty(ChanPreferences.getPassId())) {
                // Login
                final ProgressDialog dialog = ProgressDialog.show(getActivity(), null, "Logging in");

                ChanApplication.getReplyManager().sendPass(pass, new ReplyManager.PassListener() {
                    @Override
                    public void onResponse(PassResponse response) {
                        dialog.dismiss();

                        if (getActivity() == null)
                            return;

                        new AlertDialog.Builder(getActivity()).setMessage(response.message)
                                .setNeutralButton(R.string.ok, null).create().show();
                        ChanPreferences.setPassId(response.passId);
                        updateLoginButton();
                    }
                });
            } else {
                // Logout
                ChanPreferences.setPassId("");
                updateLoginButton();
            }
        }
    }
}
