package com.frezcirno.weather.fragment;

import static com.frezcirno.weather.internet.CheckConnectionKt.isNetworkAvailable;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.frezcirno.weather.activity.GlobalActivity;
import com.frezcirno.weather.R;
import com.frezcirno.weather.activity.WeatherActivity;
import com.frezcirno.weather.permissions.GPSTracker;
import com.frezcirno.weather.permissions.Permissions;
import com.frezcirno.weather.preferences.MyPreference;
import com.frezcirno.weather.utils.Constants;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

public class FirstLaunchFragment extends Fragment {

    View rootView;
    EditText cityInput;
    TextView message;
    MyPreference preferences;
    Permissions permission;
    GPSTracker gps;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_first_launch, container, false);

        preferences = new MyPreference(getContext());

        cityInput = rootView.findViewById(R.id.city_input);

        TextInputLayout textField = rootView.findViewById(R.id.materialTextField);
        textField.setOnClickListener(v -> {
            permission = new Permissions(getContext());
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, Constants.READ_COARSE_LOCATION);
        });

        message = rootView.findViewById(R.id.intro_text);
        if (GlobalActivity.i == 0) {
            message.setText(getString(R.string.pick_city));
        } else {
            message.setText(getString(R.string.uh_oh));
        }

        Button goButton = rootView.findViewById(R.id.go_button);
        goButton.setText(getString(android.R.string.ok));
        goButton.setOnClickListener(v -> {
            if (!isNetworkAvailable(requireContext())) {
                Snackbar.make(rootView, getString(R.string.check_internet), Snackbar.LENGTH_SHORT).show();
            } else if (cityInput.getText().length() > 0) {
                launchActivity(0);
            } else {
                Snackbar.make(rootView, getString(R.string.enter_city_first), Snackbar.LENGTH_SHORT).show();
            }
        });

        cityInput.setOnEditorActionListener((v, actionId, event) -> {
            if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                launchActivity(0);
                return true;
            }
            return false;
        });
        return rootView;
    }

    private void launchActivity(int mode) {
        preferences.setCity(cityInput.getText().toString());

        Intent intent = new Intent(getActivity(), WeatherActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("mode", mode);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == Constants.READ_COARSE_LOCATION) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                gps = new GPSTracker(getContext());
                if (!gps.canGetLocation)
                    gps.showSettingsAlert();
                else {
                    preferences.setLatitude(Float.parseFloat(gps.getLatitude()));
                    preferences.setLongitude(Float.parseFloat(gps.getLongitude()));
                    launchActivity(1);
                }
            } else {
                permission.permissionDenied();
            }
        }
    }
}
