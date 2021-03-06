package com.akkanben.taskmaster.activity;

import static com.akkanben.taskmaster.activity.SettingsActivity.USERNAME_TAG;
import static com.akkanben.taskmaster.utility.AnalyticsUtility.analyticsLogTime;
import static com.akkanben.taskmaster.utility.AnimationUtility.setupAnimatedBackground;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.akkanben.taskmaster.R;
import com.akkanben.taskmaster.activity.authentication.LogInActivity;
import com.akkanben.taskmaster.adapter.TaskListRecyclerViewAdapter;

import com.amplifyframework.analytics.AnalyticsEvent;
import com.amplifyframework.api.graphql.model.ModelQuery;
import com.amplifyframework.auth.AuthUser;
import com.amplifyframework.auth.AuthUserAttribute;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.datastore.generated.model.Task;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "main_activity_tag";
    public static final String TASK_NAME_EXTRA_TAG = "taskName";
    public static final String TASK_STATUS_EXTRA_TAG = "taskStatus";
    public static final String TASK_DESCRIPTION_EXTRA_TAG = "taskDescription";
    public static final String TASK_ATTACHMENT_EXTRA_TAG = "taskAttachment";
    public static final String TASK_LOCATION_EXTRA_TAG = "taskLocation";

    List<Task> taskList = new ArrayList<>();
    SharedPreferences preferences;
    TaskListRecyclerViewAdapter taskListAdapter;
    String usernameString = "";
    // Global Ad Variables
    private InterstitialAd mInterstitialAd = null;
    private RewardedAd mRewardedAd = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        analyticsLogTime("MainActivityOnCreate");
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        ConstraintLayout constraintLayout = findViewById(R.id.main_activity_layout);
        setupAnimatedBackground(constraintLayout);
        setupSettingsFloatingActionButton();
        setupAddTaskButton();
        setupTaskListRecyclerView();
        setUpAds();
    }

    @Override
    protected void onResume() {
        super.onResume();
        analyticsLogTime("MainActivityOnResume");
        AuthUser authUser = Amplify.Auth.getCurrentUser();
        if (authUser == null) {
            Intent goToLogInIntent = new Intent(MainActivity.this, LogInActivity.class);
            startActivity(goToLogInIntent);
        } else {
            Amplify.Auth.fetchUserAttributes(
                    success -> {
                        for (AuthUserAttribute userAttribute : success) {
                            if (userAttribute.getKey().getKeyString().equals("nickname")) {
                                if (!preferences.getString(USERNAME_TAG, getString(R.string.my_tasks)).toString().equals(userAttribute.getValue())) {
                                    runOnUiThread(() -> {
                                        SharedPreferences.Editor preferencesEditor = preferences.edit();
                                        usernameString = userAttribute.getValue();
                                        preferencesEditor.putString(USERNAME_TAG, usernameString);
                                        preferencesEditor.apply();
                                    });
                                }
                            }
                        }
                    },
                    failure -> Log.i(TAG, "Failed to get user attributes: " + failure.toString())
            );
        }
        usernameString = preferences.getString(SettingsActivity.USERNAME_TAG, getString(R.string.my_tasks));
        if (usernameString.equals("") || usernameString.equals(getString(R.string.my_tasks)))
            ((TextView) findViewById(R.id.main_activity_my_tasks_text_view)).setText(getString(R.string.my_tasks));
        else
            ((TextView) findViewById(R.id.main_activity_my_tasks_text_view)).setText(getString(R.string.usernames_tasks, usernameString));
        setupTaskListFromDatabase();
        taskListAdapter.updateListData(taskList);
    }

    private void setupTaskListFromDatabase() {
        String currentTeam = preferences.getString(SettingsActivity.TEAM_TAG, "All");
        taskList.clear();
        Amplify.API.query(
                ModelQuery.list(Task.class),
                success -> {
                    Log.i(TAG, "Read products successfully");
                    for (Task task : success.getData()) {
                        if (currentTeam.equals("All") || task.getTeam().getName().equals(currentTeam))
                            taskList.add(task);
                    }
                    runOnUiThread(() -> taskListAdapter.notifyDataSetChanged());
                },
                failure -> Log.i(TAG, "Failed to read products")
        );
    }

    private void setupSettingsFloatingActionButton() {
        FloatingActionButton settingsFloatingActionButton = findViewById(R.id.main_activity_settings_floating_action_button);
        settingsFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent goToSettingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(goToSettingsIntent);
            }
        });
    }

    private void setupAddTaskButton() {
        Button addTaskButton = findViewById(R.id.button_main_add_task);
        addTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent goToAddTaskIntent = new Intent(MainActivity.this, AddTaskActivity.class);
                startActivity(goToAddTaskIntent);
            }
        });
    }

    private void setupTaskListRecyclerView() {
        RecyclerView taskListRecyclerView = findViewById(R.id.recycler_view_main_activity_task_list);
        RecyclerView.LayoutManager taskListLayoutManager = new LinearLayoutManager(this);
        taskListRecyclerView.setLayoutManager(taskListLayoutManager);
        taskListAdapter = new TaskListRecyclerViewAdapter(taskList, this);
        taskListRecyclerView.setAdapter(taskListAdapter);
    }

    private void setUpAds() {
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {
            }
        });

        // BANNER AD
        AdView bannerView = findViewById(R.id.ad_view_main_activity_banner);
        AdRequest bannerRequest = new AdRequest.Builder().build();
        bannerView.loadAd(bannerRequest);

        // INTERSTITIAL AD
        AdRequest interstitialRequest = new AdRequest.Builder().build();
        InterstitialAd.load(
                this,
                "ca-app-pub-3940256099942544/1033173712",
                interstitialRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.i(TAG, "Interstitial ad failed to load");
                        mInterstitialAd = null;
                    }

                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        mInterstitialAd = interstitialAd;
                        Log.i(TAG, "Interstitial ad loaded");
                    }
                });
        Button interstitialButton = findViewById(R.id.button_main_activity_interstitial_ad);
        interstitialButton.setOnClickListener(v -> {
            if (mInterstitialAd != null)
                mInterstitialAd.show(MainActivity.this);
            else
                Log.d(TAG, "The interstitial ad was not ready");
        });

        // REWARD AD
        AdRequest rewardedRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, "ca-app-pub-3940256099942544/5224354917", rewardedRequest, new RewardedAdLoadCallback() {
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    // Handle the error.
                    Log.d(TAG, loadAdError.getMessage());
                    mRewardedAd = null;
                }

                @Override
                public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                    mRewardedAd = rewardedAd;
                    Log.d(TAG, "Ad was loaded.");
                }
        });
        Button rewardedButton = findViewById(R.id.button_main_activity_reward_ad);
        rewardedButton.setOnClickListener(v -> {
            if (mRewardedAd != null) {
                mRewardedAd.show(MainActivity.this, new OnUserEarnedRewardListener() {
                    @Override
                    public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                        int taskBucks = rewardItem.getAmount();
                        String rewardType = rewardItem.getType();
                        Log.d(TAG, "Reward earned: " + taskBucks + " type: " + rewardType);
                        runOnUiThread(() -> {
                            TextView taskBucksBalanceTextView = findViewById(R.id.text_view_main_activity_taskbucks_value);
                            String taskBucksBalance = taskBucksBalanceTextView.getText().toString();
                            int balance = Integer.parseInt(taskBucksBalance);
                            balance += taskBucks;
                            taskBucksBalanceTextView.setText(Integer.toString(balance));
                        });
                    }
                });
            } else {
                Log.d(TAG, "Reward not ready");
            }
        });
    }
}