package com.akkanben.taskmaster;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class SettingsActivity extends AppCompatActivity {

    SharedPreferences preferences;
    public static final String USERNAME_TAG = "username";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String usernameString = preferences.getString(USERNAME_TAG, String.valueOf(R.string.my_task));

        if (!usernameString.isEmpty()) {
            EditText usernameEditText = findViewById(R.id.edit_text_settings_activity_username);
            usernameEditText.setText(usernameString);
        }

        Button saveButton = findViewById(R.id.button_settings_activity_save);
        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                SharedPreferences.Editor preferencesEditor = preferences.edit();
                EditText usernameEditText = findViewById(R.id.edit_text_settings_activity_username);
                String usernameString = usernameEditText.getText().toString();
                preferencesEditor.putString(USERNAME_TAG, usernameString);
                preferencesEditor.apply();
            }
        });

    }
}