package com.akkanben.taskmaster;

import android.app.Application;
import android.util.Log;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.analytics.pinpoint.AWSPinpointAnalyticsPlugin;
import com.amplifyframework.api.aws.AWSApiPlugin;
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.geo.location.AWSLocationGeoPlugin;
import com.amplifyframework.predictions.aws.AWSPredictionsPlugin;
import com.amplifyframework.storage.s3.AWSS3StoragePlugin;

public class TaskMasterApplication extends Application {
    public static final String TAG = "taskmaster_application_tag";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            Amplify.addPlugin(new AWSApiPlugin());
            Amplify.addPlugin(new AWSCognitoAuthPlugin());
            Amplify.addPlugin(new AWSPredictionsPlugin());
            Amplify.addPlugin(new AWSPinpointAnalyticsPlugin(this));
            Amplify.addPlugin(new AWSS3StoragePlugin());
            Amplify.addPlugin(new AWSLocationGeoPlugin());

            Amplify.configure(getApplicationContext());
        } catch (AmplifyException e) {
            Log.e(TAG, "Error during initialization of Amplify: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }
}
