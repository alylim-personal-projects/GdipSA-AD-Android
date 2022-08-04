package com.team2.getfitwithhenry;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.gson.Gson;
import com.team2.getfitwithhenry.model.DietRecord;
import com.team2.getfitwithhenry.model.Goal;
import com.team2.getfitwithhenry.model.HealthRecord;
import com.team2.getfitwithhenry.model.Role;
import com.team2.getfitwithhenry.model.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kotlin.jvm.internal.TypeReference;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import java.util.Calendar;
import java.util.Locale;

public class LoggerActivity extends AppCompatActivity implements LifecycleObserver {

    private User tempUser;
    private final OkHttpClient client = new OkHttpClient();
    private DatePickerDialog datePickerDialog;
    private Button dateButton;
    private BottomNavigationView bottomNavView;
    User user;

    //TODO LIST:
    //refresh page after adding meal
    //concat the ingredient names if meal name is empty
    //ui wise -> add units
    //get user's calories for the day




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logger);

        //set up bottom navbar
        setBottomNavBar();

        SharedPreferences pref = getSharedPreferences("UserDetailsObj", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = pref.getString("userDetails", "");
        System.out.println(json);
        user = gson.fromJson(json, User.class);

        System.out.println(user.getUsername());

        // Set up Calendar
        initDatePicker();
        dateButton = findViewById(R.id.datePickerButton);
        dateButton.setText(setDate(LocalDate.now()));




        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String currDate = LocalDate.now().format(formatter);
        getDietRecordsFromServer(user, currDate);

        //add meal function
        Button addFoodBtn = findViewById(R.id.add_food);
        addFoodBtn.setOnClickListener((view -> {
            DatePicker datePicker = datePickerDialog.getDatePicker();
            String dateSelect = datePicker.getYear() + "-" + String.format("%02d", (datePicker.getMonth() + 1)) + "-" + String.format("%02d", datePicker.getDayOfMonth());
            Intent intent = new Intent(this, AddMealActivity.class);
            intent.putExtra("date", dateSelect);

            startActivity(intent);


        }));

    }

    @Override
    public void onResume() {

        super.onResume();
    }



    public void openDatePicker(View view)
    {
        datePickerDialog.show();
    }

    private void initDatePicker() {
        DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                month = month + 1;
                String date = String.format("%02d", day) + "-" + String.format("%02d", month) + "-" + year;
                DateTimeFormatter format2 = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                LocalDate parsedDate = LocalDate.parse(date, format2);
                dateButton.setText(setDate(parsedDate));

                getDietRecordsFromServer(user, date);
            }
        };

        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        int style = AlertDialog.THEME_HOLO_LIGHT;

        datePickerDialog = new DatePickerDialog(this, style, dateSetListener, year, month, day);
    }

    private String setDate(LocalDate date){
        DateTimeFormatter format1 = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        return date.format(format1);
    }


    private void getDietRecordsFromServer(User user, String date){
        JSONObject postData = new JSONObject();
        try {
            postData.put("username", user.getUsername());
            postData.put("date", date);

            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(postData.toString(), JSON);

            //need to use your own pc's ip address here, cannot use local host.
            Request request = new Request.Builder()
                    .url("http://192.168.10.127:8080/user/getdietrecords")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try {
                        ResponseBody responseBody = response.body();
                        if (!response.isSuccessful()) {
                            throw new IOException("Unexpected code " + response);
                        }

                        String msg = String.valueOf(responseBody);
                        //convert responseBody into list of HealthRecords
                        ObjectMapper objectMapper = new ObjectMapper();
                        objectMapper.registerModule(new JavaTimeModule());
                        List <DietRecord> dietRecordList = Arrays.asList(objectMapper.readValue(responseBody.string(), DietRecord[].class));
                        FragmentManager fm = getSupportFragmentManager();
                        MealFragment mealFragment = (MealFragment) fm.findFragmentById(R.id.fragment_meal);
                        mealFragment.setDietRecordList(dietRecordList);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setBottomNavBar() {
        bottomNavView = findViewById(R.id.bottom_navigation);
        bottomNavView.setSelectedItemId(R.id.nav_log);
        bottomNavView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener(){
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Intent intent;
                int id = item.getItemId();
                switch(id){

                    case(R.id.nav_scanner):
                        intent = new Intent(getApplicationContext(), CameraActivity.class);
                        startActivity(intent);
                        break;  //or should this be finish?

                    case(R.id.nav_search):
                        intent = new Intent(getApplicationContext(), SearchFoodActivity.class);
                        startActivity(intent);
                        break;

                    case(R.id.nav_recipe):
                        intent = new Intent(getApplicationContext(), RecipeActivity.class);
                        startActivity(intent);
                        break;

                    case(R.id.nav_home):
                        intent = new Intent(getApplicationContext(), HomeActivity.class);
                        startActivity(intent);
                        break;
                }

                return false;
            }
        });
    }




}