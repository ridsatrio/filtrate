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
package id.ridsatrio.filtrate;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;

/**
 * A simple yet customizable app store rating reminder that filters incoming input according to
 * a configurable threshold.
 */
public final class Filtrate {

    private static final String TAG = "Filtrate";

    /* Preference Keys */
    public static final String PREF_FILENAME = "id.ridsatrio.filtrate";
    public static final String PREF_KEY_SHOULD_MONITOR = "id.ridsatrio.filtrate.shouldmonitor";
    public static final String PREF_KEY_LAUNCH_COUNT = "id.ridsatrio.filtrate.launchcount";
    public static final String PREF_KEY_SKIP_COUNT = "id.ridsatrio.filtrate.skipcount";

    private FragmentActivity mActivity;
    private RetryPolicy mRetryPolicy = RetryPolicy.INCREMENTAL;
    private int mInitialThreshold = 5;
    private int mRateThreshold = 3;
    private String mPromptText = "How would you rate this app?";
    private String mSkipButtonText = "Not now";
    private Theme mTheme = Theme.LIGHT;

    /* Shared Preferences */
    private SharedPreferences mPrefs;
    private SharedPreferences.Editor mPrefEditor;

    /* Listeners */
    private OnRateListener mRateListener;
    private OnRateCancelListener mCancelListener;

    private Filtrate(FragmentActivity activity) {
        mActivity = activity;
    }

    /**
     * Initialize (and return) a Filtrate instance. This call can be chained with other method calls
     * (e.g., setRetryPolicy, setInitialLaunchThreshold) in order to alter Filtrate's behaviour.
     * Alternatively, one could also directly call start() to use Filtrate with its default settings.
     *
     * @param activity     The activity in which Filtrate should monitor and show the prompt.
     * @param rateListener The listener that will be called when user has filled in the rate prompt.
     */
    public static Filtrate with(FragmentActivity activity, OnRateListener rateListener) {
        if (activity == null) {
            throw new IllegalArgumentException("Activity can not be null.");
        }
        Filtrate instance = new Filtrate(activity);
        instance.mActivity = activity;
        instance.mRateListener = rateListener;
        instance.mPrefs = instance.mActivity.getSharedPreferences(PREF_FILENAME,
                Context.MODE_PRIVATE);
        instance.mPrefEditor = instance.mPrefs.edit();
        return instance;
    }

    /**
     * Define a listener to be called upon rating prompt skipped by the user.
     *
     * @param cancelListener The listener.
     */
    public Filtrate setRateCancelListener(OnRateCancelListener cancelListener) {
        mCancelListener = cancelListener;
        return this;
    }

    /**
     * Define how many app launches before Filtrate should show the first rating prompt. This value
     * will only be used once-next appearances threshold will be calculated in respect to this and
     * the RetryPolicy used.
     *
     * The default value set for this behaviour is 5 launches.
     *
     * @param launchThreshold App launches until the first prompt appearance.
     */
    public Filtrate setInitialLaunchThreshold(int launchThreshold) {
        mInitialThreshold = launchThreshold;
        return this;
    }

    /**
     * Define how Filtrate would attempt to retry prompting the user in case the last prompt got
     * skipped.
     *
     * The default value set for this behaviour is to retry incrementally (RetryPolicy.INCREMENTAL).
     *
     * @param retryPolicy The RetryPolicy to govern prompt retries.
     */
    public Filtrate setRetryPolicy(RetryPolicy retryPolicy) {
        mRetryPolicy = retryPolicy;
        return this;
    }

    /**
     * Define how many star(s) should be considered "acceptable". User ratings that were above this
     * threshold will fire up the onRatedAboveThreshold method defined in the listener. Likewise,
     * user ratings that were below this threshold will fire up the onRatedBelowThreshold method.
     *
     * The default value set for this behaviour is 3 stars.
     *
     * @param rateThreshold How many stars Filtrate should consider "acceptable".
     */
    public Filtrate setAcceptableRateThreshold(int rateThreshold) {
        if (rateThreshold < 1 || rateThreshold > 5) {
            throw new IllegalArgumentException("Rate threshold should be larger than 1 or smaller than 5.");
        }
        mRateThreshold = rateThreshold;
        return this;
    }

    /**
     * Set a label text to the rating prompt to replace the default string.
     *
     * The default value is "How would you rate this app?".
     *
     * @param ratePromptText Label text to be set onto the skip button.
     */
    public Filtrate setRatePromptText(String ratePromptText) {
        if (ratePromptText != null && !"".equals(ratePromptText)) {
            mPromptText = ratePromptText;
        }
        return this;
    }

    /**
     * Set a label text to the skip button to replace the default string.
     *
     * The default value set is "Skip".
     *
     * @param skipButtonText Text to be set to the skip button.
     */
    public Filtrate setSkipButtonText(String skipButtonText) {
        if (skipButtonText != null && !"".equals(skipButtonText)) {
            mSkipButtonText = skipButtonText;
        }
        return this;
    }

    /**
     * Set a theme for the rating prompt.
     *
     * The default value set Theme.LIGHT.
     *
     * @param theme Theme that will be used. Can be Theme.LIGHT or Theme.DARK.
     * */
    public Filtrate setTheme(Theme theme) {
        if (theme == null) {
            throw new IllegalArgumentException("Theme can not be null.");
        }
        mTheme = theme;
        return this;
    }

    /**
     * Start the launch-checking sequence and show a rating prompt if the prerequisites are all met.
     */
    public void checkAndShowIfQualify() {
        mPrefEditor.putInt(PREF_KEY_LAUNCH_COUNT, getLaunchCount() + 1);
        mPrefEditor.commit();
        if (shouldMonitor()) {
            if (mInitialThreshold < 0) {
                throw new IllegalStateException("Initial threshold has not been specified yet. Did " +
                        "you forget to call setInitialLaunchThreshold?");
            }
            if (getLaunchCount() == getNextThreshold()) {
                FragmentManager manager = mActivity.getSupportFragmentManager();
                new RatePromptDialog().show(manager, "id.ridsatrio.filtrate.ratedialog");
            }
        }
    }

    /**
     * Returns the amount of time user has launched the app since the first Filtrate initialization.
     */
    public int getLaunchCount() {
        return mPrefs.getInt(PREF_KEY_LAUNCH_COUNT, 0);
    }

    /**
     * Returns the amount of time user has skipped the rating prompt.
     */
    public int getSkipCount() {
        return mPrefs.getInt(PREF_KEY_SKIP_COUNT, 0);
    }

    /**
     * Returns the next launch threshold on which Filtrate should retry showing the rating prompt to
     * user. This value is calculated in respect to the initial threshold set and the RetryPolicy
     * that's being used.
     */
    public int getNextThreshold() {
        if (!shouldMonitor()) {
            return -1;
        }
        int thresholdSteps = mInitialThreshold;
        int skipCount = getSkipCount();
        if (skipCount < 1) {
            return thresholdSteps;
        } else {
            switch (mRetryPolicy) {
                case INCREMENTAL:
                    return thresholdSteps + (thresholdSteps * skipCount);
                case EXPONENTIAL:
                    return (int) (thresholdSteps * Math.pow(2, skipCount));
                default:
                    return -1;
            }
        }
    }

    /**
     * Force Filtrate to show the rating prompt ignoring all the prerequisites that might specified
     * beforehand.
     */
    public void forceShow() {
        FragmentManager manager = mActivity.getSupportFragmentManager();
        new RatePromptDialog().show(manager, "id.ridsatrio.filtrate.ratedialog");
    }

    /**
     * Invalidate Filtrate's current state and set all preferences to its default value.
     */
    public void reset() {
        mPrefEditor.putBoolean(PREF_KEY_SHOULD_MONITOR, true);
        mPrefEditor.putInt(PREF_KEY_LAUNCH_COUNT, 0);
        mPrefEditor.putInt(PREF_KEY_SKIP_COUNT, 0);
        mPrefEditor.commit();
    }

    /*
    * Returns whether Filtrate should monitor app launches for the given Activity.
    * */
    private boolean shouldMonitor() {
        return mPrefs.getBoolean(PREF_KEY_SHOULD_MONITOR, true);
    }

    @SuppressLint("ValidFragment")
    protected class RatePromptDialog extends DialogFragment
            implements View.OnClickListener, RatingBar.OnRatingBarChangeListener {

        RatingBar mRbRateBar;
        Button mBtnSkipRate;
        TextView mTvRatePrompt;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Dialog dialog = super.onCreateDialog(savedInstanceState);
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            dialog.setCanceledOnTouchOutside(false);
            setCancelable(false);
            return dialog;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            Context contextThemeWrapper = null;
            switch (mTheme) {
                case LIGHT:
                    contextThemeWrapper = new ContextThemeWrapper(getActivity(),
                            android.R.style.Theme_DeviceDefault_Light);
                    break;
                case DARK:
                    contextThemeWrapper = new ContextThemeWrapper(getActivity(),
                            android.R.style.Theme_DeviceDefault);
                    break;
            }

            LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);
            View view = localInflater.inflate(R.layout.fragment_rate_dialog, container, false);
            mTvRatePrompt = (TextView) view.findViewById(R.id.tv_ratePrompt);
            mTvRatePrompt.setText(mPromptText);
            mRbRateBar = (RatingBar) view.findViewById(R.id.rb_ratingBar);
            mRbRateBar.setOnRatingBarChangeListener(this);
            mBtnSkipRate = (Button) view.findViewById(R.id.btn_skipRating);
            mBtnSkipRate.setText(mSkipButtonText);
            mBtnSkipRate.setOnClickListener(this);
            return view;
        }

        @Override
        public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
            mPrefEditor.putBoolean(PREF_KEY_SHOULD_MONITOR, false);
            mPrefEditor.commit();
            if (mRateListener != null) {
                if (rating > mRateThreshold) {
                    mRateListener.onRatedAboveThreshold();
                } else {
                    mRateListener.onRatedBelowThreshold();
                }
            } else {
                Log.w(TAG, "No assigned OnRateListener found. Ignoring..");
            }
            dismiss();
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.btn_skipRating) {
                SharedPreferences preferences = mActivity.getSharedPreferences(PREF_FILENAME,
                        Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                switch (mRetryPolicy) {
                    case NEVER_RETRY:
                        editor.putBoolean(PREF_KEY_SHOULD_MONITOR, false);
                        editor.commit();
                        break;
                    default:
                        int skipCount = getSkipCount();
                        editor.putInt(PREF_KEY_SKIP_COUNT, skipCount + 1);
                        editor.commit();
                }
                if (mCancelListener != null) {
                    mCancelListener.onRateCancel();
                }
                dismiss();
            }
        }
    }
}