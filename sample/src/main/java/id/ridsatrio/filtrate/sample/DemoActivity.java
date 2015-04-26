/*
 * Copyright (C) 2015 Ridho Hadi Satrio - All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package id.ridsatrio.filtrate.sample;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import id.ridsatrio.filtrate.Filtrate;
import id.ridsatrio.filtrate.OnRateCancelListener;
import id.ridsatrio.filtrate.OnRateListener;
import id.ridsatrio.filtrate.RetryPolicy;

public class DemoActivity extends ActionBarActivity
        implements View.OnClickListener, OnRateListener, OnRateCancelListener {

    /* Preference Keys */
    private static final String PREF_KEY_PREFIX = "id.ridsatrio.filtrate.sample.prefkey.";
    public static final String PREF_KEY_RATE_PROMPT_TEXT = PREF_KEY_PREFIX + "prompttext";
    public static final String PREF_KEY_RATE_SKIP_TEXT = PREF_KEY_PREFIX + "skiptext";
    public static final String PREF_KEY_RATE_THRESHOLD = PREF_KEY_PREFIX + "ratethreshold";
    public static final String PREF_KEY_INITIAL_THRESHOLD = PREF_KEY_PREFIX + "initialthreshold";
    public static final String PREF_KEY_RETRY_POLICY = PREF_KEY_PREFIX + "retrypolicy";

    /* Spinner items */
    private static final String[] RETRY_POLICIES = {"INCREMENTAL", "EXPONENTIAL", "NEVER_RETRY"};
    private static final String[] RATE_THRESHOLDS = {"1", "2", "3", "4", "5"};

    /* Views */
    @InjectView(R.id.tv_launchCount) public TextView mTvAppLaunchCount;
    @InjectView(R.id.tv_nextThreshold) public TextView mTvNextThreshold;
    @InjectView(R.id.et_ratePromptText) public EditText mEtCustomRatePromptText;
    @InjectView(R.id.et_rateSkipText) public EditText mEtCustomSkipButtonText;
    @InjectView(R.id.et_initialLaunchThreshold) public EditText mEtInitialThreshold;
    @InjectView(R.id.sp_acceptableRateThreshold) public Spinner mSpRateThreshold;
    @InjectView(R.id.sp_retryPolicy) public Spinner mSpRetryPolicy;
    @InjectView(R.id.btn_changeSettings) public Button mBtnChangeSettings;
    @InjectView(R.id.btn_forceShow) public Button mBtnForceShow;
    @InjectView(R.id.btn_incrementCount) public Button mBtnIncrementCount;
    @InjectView(R.id.btn_reset) public Button mBtnReset;

    /* SharedPreferences */
    private SharedPreferences mPrefs;
    private SharedPreferences.Editor mPrefEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_demo);
        ButterKnife.inject(this);

        mBtnChangeSettings.setOnClickListener(this);
        mBtnForceShow.setOnClickListener(this);
        mBtnIncrementCount.setOnClickListener(this);
        mBtnReset.setOnClickListener(this);

        List<String> retryPolicyList = Arrays.asList(RETRY_POLICIES);
        ArrayAdapter<String> retryPolicyAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, retryPolicyList);
        retryPolicyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpRetryPolicy.setAdapter(retryPolicyAdapter);
        mSpRetryPolicy.setSelection(0);

        List<String> rateThresholdList = Arrays.asList(RATE_THRESHOLDS);
        ArrayAdapter<String> rateThresholdAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, rateThresholdList);
        rateThresholdAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpRateThreshold.setAdapter(rateThresholdAdapter);
        mSpRateThreshold.setSelection(2);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mPrefEditor = mPrefs.edit();

        updateStateInformation();
    }

    private Filtrate getFiltrate() {
        String ratePromptText = mPrefs.getString(PREF_KEY_RATE_PROMPT_TEXT, "");
        String rateSkipText = mPrefs.getString(PREF_KEY_RATE_SKIP_TEXT, "");
        RetryPolicy retryPolicy = RetryPolicy.valueOf(mPrefs.getString(PREF_KEY_RETRY_POLICY,
                RetryPolicy.INCREMENTAL.toString()));
        int initialThreshold = mPrefs.getInt(PREF_KEY_INITIAL_THRESHOLD, 5);
        int rateThreshold = mPrefs.getInt(PREF_KEY_RATE_THRESHOLD, 3);

        mEtCustomRatePromptText.setText(ratePromptText);
        mEtCustomSkipButtonText.setText(rateSkipText);
        mEtInitialThreshold.setText(String.valueOf(initialThreshold));

        return Filtrate.with(this, this)
                .setRateCancelListener(this)
                .setRatePromptText(ratePromptText)
                .setSkipButtonText(rateSkipText)
                .setInitialLaunchThreshold(initialThreshold)
                .setAcceptableRateThreshold(rateThreshold)
                .setRetryPolicy(retryPolicy);
    }

    private void updateStateInformation() {
        String appLaunchString = getFiltrate().getLaunchCount() + " launch(es)";
        String nextThresholdString = getFiltrate().getNextThreshold() > 0 ?
                getFiltrate().getNextThreshold() + " launch(es)"
                : "Never";

        mTvAppLaunchCount.setText(appLaunchString);
        mTvNextThreshold.setText(nextThresholdString);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action_demo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_goToGithub:
                try {
                    String githubUrl = "https://github.com/ridsatrio/filtrate";
                    Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl));
                    startActivity(myIntent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, "No application can handle this request",
                            Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_incrementCount:
                getFiltrate().checkAndShowIfQualify();
                updateStateInformation();
                break;
            case R.id.btn_changeSettings:
                String ratePromptText = mEtCustomRatePromptText.getText().toString();
                String rateSkipText = mEtCustomSkipButtonText.getText().toString();
                RetryPolicy retryPolicy = RetryPolicy.valueOf(String.valueOf(mSpRetryPolicy.getSelectedItem()));
                int initialThreshold = Integer.valueOf(mEtInitialThreshold.getText().toString());
                int rateThreshold = Integer.valueOf(String.valueOf(mSpRateThreshold.getSelectedItem()));

                getFiltrate().setRatePromptText(ratePromptText);
                getFiltrate().setSkipButtonText(rateSkipText);
                getFiltrate().setRetryPolicy(retryPolicy);
                getFiltrate().setInitialLaunchThreshold(initialThreshold);
                getFiltrate().setAcceptableRateThreshold(rateThreshold);

                mPrefEditor.putString(PREF_KEY_RATE_PROMPT_TEXT, ratePromptText);
                mPrefEditor.putString(PREF_KEY_RATE_SKIP_TEXT, rateSkipText);
                mPrefEditor.putString(PREF_KEY_RETRY_POLICY, retryPolicy.toString());
                mPrefEditor.putInt(PREF_KEY_INITIAL_THRESHOLD, initialThreshold);
                mPrefEditor.putInt(PREF_KEY_RATE_THRESHOLD, rateThreshold);
                mPrefEditor.commit();

                Toast.makeText(this, "Settings was successfully changed", Toast.LENGTH_LONG).show();
                getFiltrate().reset();
                updateStateInformation();
                break;
            case R.id.btn_forceShow:
                getFiltrate().forceShow();
                updateStateInformation();
                break;
            case R.id.btn_reset:
                getFiltrate().reset();
                Toast.makeText(this, "Resetting state..", Toast.LENGTH_SHORT).show();
                updateStateInformation();
                break;
        }
    }

    @Override
    public void onRatedAboveThreshold() {
        Toast.makeText(this, "User has rated above the specified threshold", Toast.LENGTH_LONG)
                .show();
        updateStateInformation();
    }

    @Override
    public void onRatedBelowThreshold() {
        Toast.makeText(this, "User has rated below the specified threshold", Toast.LENGTH_LONG)
                .show();
        updateStateInformation();
    }

    @Override
    public void onRateCancel() {
        Toast.makeText(this, "User has dismissed the rate prompt without rating", Toast.LENGTH_LONG)
                .show();
        updateStateInformation();
    }
}
