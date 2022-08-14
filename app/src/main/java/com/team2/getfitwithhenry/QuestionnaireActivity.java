package com.team2.getfitwithhenry;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import com.team2.getfitwithhenry.model.Constants;
import com.team2.getfitwithhenry.model.Goal;
import com.team2.getfitwithhenry.model.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class QuestionnaireActivity extends AppCompatActivity implements View.OnClickListener {

    private final OkHttpClient client = new OkHttpClient();
    private TextView mtxtName, mtxtSomethingWentWrong;
    private EditText mtxtUserWeight, mtxtUserHeight;
    private AutoCompleteTextView mGoalSelect,mactivityLevelSelector;
    private Button mbtnSaveHealthDetails;
    private String[] goalmatch = {"WEIGHTLOSS", "WEIGHTGAIN", "WEIGHTMAINTAIN", "MUSCLE"};
    private String[] goals = {"Weight Loss", "Weight Gain", "Weight Maintain", "Muscle"};
    private String[] activityLevels = {"Lightly Active", "Moderately Active", "Very Active", "Extra Active"};
    private User user;
    private User updatedUser;
    private ArrayAdapter<String> goalAdapter,activitylevelAdapter;
    private String goalSelection, activitylevelSelection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questionnaire);

        mtxtName = findViewById(R.id.txtNameQuestionnaire);
        mtxtUserWeight = findViewById(R.id.txtUserWeight);
        mtxtUserHeight = findViewById(R.id.txtUserHeight);
        mGoalSelect = findViewById(R.id.goalSelection);
        mactivityLevelSelector = findViewById(R.id.continuous_slider);
        mbtnSaveHealthDetails = findViewById(R.id.btnSaveHealthDetails);
        mtxtSomethingWentWrong = findViewById(R.id.txtSomethingWentWrong);

        mbtnSaveHealthDetails.setOnClickListener(this);

        getUserFromSharedPreference();
        onInitialDataBind();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnSaveHealthDetails) {
            try {
                if (!validateFields()) {
                    setDetailstoUpdate();
                    createUserJsonObj();
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        redirectToLogin();
    }

    private void getUserFromSharedPreference() {

        SharedPreferences pref = getSharedPreferences("UserDetailsObj", MODE_PRIVATE);

        if (pref.contains("userDetails")) {
            Gson gson = new Gson();
            String json = pref.getString("userDetails", "");
            user = gson.fromJson(json, User.class);
            user.setDateofbirth(LocalDate.parse(user.getDobStringFormat(), DateTimeFormatter.ofPattern("yyyy-MM-dd")));

            updatedUser = user;
        }
    }

    private void onInitialDataBind() {
        mtxtName.setText(user.getName());
        switch (user.getGoal().name()){
            case("WEIGHTLOSS"):
                mGoalSelect.setText(goals[0]);
                break;
            case("WEIGHTGAIN"):
                mGoalSelect.setText(goals[1]);
                break;
            case("WEIGHTMAINTAIN"):
                mGoalSelect.setText(goals[2]);
                break;
            case("MUSCLE"):
                mGoalSelect.setText(goals[3]);
                break;
            default:
                mGoalSelect.setText(null);
                break;
        }
        goalAdapter = new ArrayAdapter<String>(this, R.layout.graph_list_item, goals);
        mGoalSelect.setAdapter(goalAdapter);
        mGoalSelect.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                goalSelection = parent.getItemAtPosition(position).toString();
                Toast.makeText(QuestionnaireActivity.this, "Item: " + goalSelection, Toast.LENGTH_SHORT).show();
            }
        });
        mGoalSelect.setFocusable(false);

        activitylevelAdapter = new ArrayAdapter<String>(this, R.layout.graph_list_item, activityLevels);
        mactivityLevelSelector.setAdapter(activitylevelAdapter);
        mactivityLevelSelector.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                activitylevelSelection = parent.getItemAtPosition(position).toString();
                Toast.makeText(QuestionnaireActivity.this, "Item: " + activitylevelSelection, Toast.LENGTH_SHORT).show();
            }
        });
        mactivityLevelSelector.setFocusable(false);
    }

    private void startHomeActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }

    private void setDetailstoUpdate() {
        String gender = "M";

        updatedUser.setId(user.getId());
        updatedUser.setName(mtxtName.getText().toString());
        updatedUser.setUsername(user.getUsername());
        updatedUser.setPassword(user.getPassword());
        updatedUser.setDateofbirth(user.getDateofbirth());
        updatedUser.setGender(user.getGender());
        updatedUser.setActivitylevel(activitylevelSelection);
        switch (goalSelection){
            case("Weight Loss"):
                updatedUser.setGoal(Goal.WEIGHTLOSS);
                break;
            case("Weight Gain"):
                updatedUser.setGoal(Goal.WEIGHTGAIN);
                break;
            case("Weight Maintain"):
                updatedUser.setGoal(Goal.WEIGHTMAINTAIN);
                break;
            case("Muscle"):
                updatedUser.setGoal(Goal.MUSCLE);
                break;
            default:
                break;
        }

        updatedUser.setCalorieintake_limit_inkcal(Double.parseDouble(performCalculation(updatedUser.getGender(), mtxtUserWeight.getText().toString(), mtxtUserHeight.getText().toString(), updatedUser.getActivitylevel())));
        updatedUser.setWaterintake_limit_inml(updatedUser.getGender().equals(gender) ? 3700.0 : 2700.0);
    }

    private String performCalculation(String gender, String userWeight, String userHeight, String activityLevel) {
        Double calculatedCalorie = 0.0;

        if (gender.equals("M")) {
            //13.397W + 4.799H - 5.677A + 88.362
            //new: 10 x weight (kg) + 6.25 x height (cm) – 5 x age (y) + 5 (kcal / day)
            calculatedCalorie = (10 * Double.parseDouble(userWeight)) + (6.25 * Double.parseDouble(userHeight)) - (5 * calculateAge()) + 5;
        } else if (gender.equals("F")) {
            //9.247W + 3.098H - 4.330A + 447.593
            //new: 10 x weight (kg) + 6.25 x height (cm) – 5 x age (y) -161 (kcal / day)
            calculatedCalorie = (10 * Double.parseDouble(userWeight)) + (6.25 * Double.parseDouble(userHeight)) - (5 * calculateAge()) - 161;
        }

        switch (activityLevel) {
            case "Lightly Active":
                calculatedCalorie = calculatedCalorie * 1.375;
                break;
            case "Moderately Active":
                calculatedCalorie = calculatedCalorie * 1.550;
                break;
            case "Very Active":
                calculatedCalorie = calculatedCalorie * 1.725;
                break;
            case "Extra Active":
                calculatedCalorie = calculatedCalorie * 1.9;
                break;
            default:
                calculatedCalorie = calculatedCalorie * 1.2;
        }

        if (updatedUser.getGoal().equals(Goal.WEIGHTLOSS)) {
            calculatedCalorie -= (0.15 * calculatedCalorie);
        } else if (updatedUser.getGoal().equals(Goal.WEIGHTGAIN)) {
            calculatedCalorie += 500;
        }

        calculatedCalorie = Math.ceil(calculatedCalorie);

        return calculatedCalorie.toString();
    }

    private Integer calculateAge() {
        LocalDate currDate = LocalDate.now();
        return Period.between(updatedUser.getDateofbirth(), currDate).getYears();
    }

    private void createUserJsonObj() throws JSONException {
        JSONObject userHealthObj = new JSONObject();

        userHealthObj.put("id", updatedUser.getId());
        userHealthObj.put("name", updatedUser.getName());
        userHealthObj.put("username", updatedUser.getUsername());
        userHealthObj.put("password", updatedUser.getPassword());
        userHealthObj.put("dateofbirth", updatedUser.getDateofbirth());
        userHealthObj.put("gender", updatedUser.getGender());
        userHealthObj.put("goal", updatedUser.getGoal());
        userHealthObj.put("calorieintake_limit_inkcal", updatedUser.getCalorieintake_limit_inkcal());
        userHealthObj.put("waterintake_limit_inml", updatedUser.getWaterintake_limit_inml());
        userHealthObj.put("activitylevel", updatedUser.getActivitylevel());
        userHealthObj.put("user_height", Double.parseDouble(mtxtUserHeight.getText().toString()));
        userHealthObj.put("user_weight", Double.parseDouble(mtxtUserWeight.getText().toString()));
        userHealthObj.put("date", LocalDate.now());

        inserHealthRecordonRegistration(userHealthObj);
    }

    private void inserHealthRecordonRegistration(JSONObject userObj) {
        MediaType JsonObj = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(JsonObj, userObj.toString());
        Request request = new Request.Builder().url(Constants.javaURL + "/questionnaire/insertHealthRecordonRegistration").post(requestBody).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                ResponseBody responseBody = response.body();

                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }

                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JavaTimeModule());

                if (responseBody.contentLength() != 0)
                    user = objectMapper.readValue(responseBody.string(), User.class);
                else
                    user = null;

                if (user == null) {
                    somethingWentWrongError(getApplicationContext(), user);
                }

                if (user != null) {
                    updateUserinSharedPreference(user);
                    startHomeActivity();
                }
            }
        });
    }

    private void updateUserinSharedPreference(User user) {
        //https://stackoverflow.com/questions/7145606/how-do-you-save-store-objects-in-sharedpreferences-on-android
        //Check the above url to retrieve the object from shared pref
        SharedPreferences pref = getSharedPreferences("UserDetailsObj", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        Gson gson = new Gson();
        String json = gson.toJson(user);
        editor.putString("userDetails", json);
        editor.commit();
    }

    private boolean validateFields() {
        if (mtxtUserWeight.getText().toString().trim().equals("") || Double.parseDouble(mtxtUserWeight.getText().toString()) <= 0) {
            mtxtUserWeight.setError("Weight should be greater than 0");
            return true;
        }

        if (mtxtUserHeight.getText().toString().trim().equals("") || Double.parseDouble(mtxtUserHeight.getText().toString()) <= 0) {
            mtxtUserHeight.setError("Height should be greater than 0");
            return true;
        }

        return false;
    }

    private void somethingWentWrongError(Context context, User user) {

        if (context != null && user != null) {
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getApplicationContext(), "Success", Toast.LENGTH_LONG).show());
        } else {
            new Handler(Looper.getMainLooper()).post(() -> {

                mtxtSomethingWentWrong.setText("Something Went Wrong. Please try again!");
                mtxtSomethingWentWrong.setVisibility(View.VISIBLE);
                Toast.makeText(getApplicationContext(), "Failed", Toast.LENGTH_LONG).show();
            });
        }
    }

    private void redirectToLogin() {
        SharedPreferences pref = getSharedPreferences("UserDetailsObj", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.clear();
        editor.commit();

        startLoginActivity();
    }


    private void startLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);

    }
}