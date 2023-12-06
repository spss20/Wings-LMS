package com.creativeitem.academy.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileUtils;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.creativeitem.academy.JSONSchemas.StatusSchema;
import com.creativeitem.academy.JSONSchemas.UserSchema;
import com.creativeitem.academy.Network.Api;
import com.creativeitem.academy.R;
import com.creativeitem.academy.Utils.Helpers;
import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.Circle;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EditProfileActivity extends AppCompatActivity {

    private static final int STORAGE_PERMISSION_CODE = 2342;
    private static final int PICK_IMAGE_REQUEST = 22;
    Button chooseDisplayImage, uploadDisplayImage, submitButton;
    TextInputEditText firstNameEditText, lastNameEditText, emailEditText, biographyEditText, twitterLinkEditText, facebookLinkEditText, linkedInLinkEditText;
    CircleImageView displayImageView;
    Uri filePath;
    private Bitmap bitmap;
    private ProgressBar progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        init();
        initProgressBar();
        getLoggedInUserData();
    }

    private void getLoggedInUserData() {
        if (isLoggedIn()){
            getUserDataFromAPI();
        }else{
            Toast.makeText(this, "Please Login First", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    private void getUserDataFromAPI() {
        SharedPreferences preferences = this.getSharedPreferences(Helpers.SHARED_PREF, 0);
        String authToken = preferences.getString("userToken", "loggedOut");
        progressBar.setVisibility(View.VISIBLE);

        Retrofit retrofit = new Retrofit.Builder().baseUrl(Api.BASE_URL).addConverterFactory(GsonConverterFactory.create()).build();
        Api api = retrofit.create(Api.class);
        Call<UserSchema> call = api.getUserProfileInfo(authToken);
        call.enqueue(new Callback<UserSchema>() {
            @Override
            public void onResponse(Call<UserSchema> call, Response<UserSchema> response) {
                progressBar.setVisibility(View.INVISIBLE);
                UserSchema userSchema = response.body();
                if (userSchema.getStatus().equals("success")){
                    initViewElementsWithUserInfo(userSchema);
                }else{
                    Toast.makeText(EditProfileActivity.this, "An Error Occurred", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserSchema> call, Throwable t) {
                progressBar.setVisibility(View.INVISIBLE);
                Toast.makeText(EditProfileActivity.this, "An Error Occured", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void init() {
        firstNameEditText = findViewById(R.id.firstNameEditText);
        lastNameEditText = findViewById(R.id.lastNameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        biographyEditText = findViewById(R.id.biographyTextView);
        twitterLinkEditText = findViewById(R.id.twitterEditText);
        facebookLinkEditText = findViewById(R.id.facebookEditText);
        linkedInLinkEditText = findViewById(R.id.linkedInEditText);
        chooseDisplayImage = findViewById(R.id.chooseDisplayImage);
        displayImageView = findViewById(R.id.displayImageView);
        uploadDisplayImage = findViewById(R.id.uploadDisplayImage);
        submitButton = findViewById(R.id.submitButton);
        requestStoragePermission();

        chooseDisplayImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFileChoose();
            }
        });

        uploadDisplayImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadUserImageToTheServer();
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitEditProfileForm();
            }
        });
    }

    // Initialize the progress bar
    private void initProgressBar() {
        progressBar = findViewById(R.id.progressBar);
        Sprite circularLoading = new Circle();
        progressBar.setIndeterminateDrawable(circularLoading);
    }


    private boolean isLoggedIn() {
        SharedPreferences preferences = this.getSharedPreferences(Helpers.SHARED_PREF, 0);
        int userValidity = preferences.getInt("userValidity", 0);
        if (userValidity == 1) {
            return true;
        }else{
            return false;
        }
    }


    private void initViewElementsWithUserInfo(UserSchema userSchema) {
        progressBar.setVisibility(View.VISIBLE);
        Glide.with(this)
                .asBitmap()
                .load(userSchema.getImage())
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(displayImageView);

        firstNameEditText.setText(userSchema.getFirstName());
        lastNameEditText.setText(userSchema.getLastName());
        emailEditText.setText(userSchema.getEmail());
        biographyEditText.setText(userSchema.getBiography());
        twitterLinkEditText.setText(userSchema.getTwitter());
        facebookLinkEditText.setText(userSchema.getFacebook());
        linkedInLinkEditText.setText(userSchema.getLinkedin());
        progressBar.setVisibility(View.INVISIBLE);
    }

    private void requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            return;
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(this, "Permission Not Granted", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null){
            filePath = data.getData();
            try{
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                displayImageView.setImageBitmap(bitmap);
                uploadDisplayImage.setVisibility(View.VISIBLE);

            }catch (IOException e){

            }
        }
    }

    private void showFileChoose() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select A Display Image"), PICK_IMAGE_REQUEST);
    }

    private String getPath(Uri uri) {
        String path = null;

        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index != -1) {
                    String fileName = cursor.getString(index);
                    File cacheDir = getCacheDir();
                    File file = new File(cacheDir, fileName);

                    try (InputStream inputStream = getContentResolver().openInputStream(uri);
                         OutputStream outputStream = new FileOutputStream(file)) {
                        if (inputStream != null) {
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer, 0, 8192)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                        }
                    }

                    path = file.getAbsolutePath();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return path;
    }
    private void uploadUserImageToTheServer() {
        Toast.makeText(this, "Uploading....", Toast.LENGTH_SHORT).show();

        String mediaPath = getPath(filePath);

        if (mediaPath == null) {
            Toast.makeText(this, "Invalid file path", Toast.LENGTH_SHORT).show();
            return;
        }

        File file = new File(mediaPath);

        if (!file.exists()) {
            Toast.makeText(this, "File does not exist", Toast.LENGTH_SHORT).show();
            return;
        }
        // Auth Token
        SharedPreferences preferences = this.getSharedPreferences(Helpers.SHARED_PREF, 0);
        String authToken = preferences.getString("userToken", "loggedOut");
//        File file = new File(mediaPath);

        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part fileData = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
        RequestBody auth_token = RequestBody.create(MediaType.parse("text/plain"), authToken);

        Retrofit retrofit = new Retrofit.Builder().baseUrl(Api.BASE_URL).addConverterFactory(GsonConverterFactory.create()).build();
        Api api = retrofit.create(Api.class);
        Call<StatusSchema> call = api.uploadUserImage(auth_token, fileData);
        call.enqueue(new Callback<StatusSchema>() {
            @Override
            public void onResponse(Call<StatusSchema> call, Response<StatusSchema> response) {
                StatusSchema statusSchema = response.body();
                if (statusSchema.getStatus().equals("success")){
                    Toast.makeText(EditProfileActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
                    getLoggedInUserData();
                }else{
                    Toast.makeText(EditProfileActivity.this, "Image uploading failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<StatusSchema> call, Throwable t) {
                Toast.makeText(EditProfileActivity.this, "Image uploading failed", Toast.LENGTH_SHORT).show();
            }
        });
    }


    // Submit form
    private void submitEditProfileForm() {
        SharedPreferences preferences = this.getSharedPreferences(Helpers.SHARED_PREF, 0);
        String authToken = preferences.getString("userToken", "loggedOut");
        progressBar.setVisibility(View.VISIBLE);

        Retrofit retrofit = new Retrofit.Builder().baseUrl(Api.BASE_URL).addConverterFactory(GsonConverterFactory.create()).build();
        Api api = retrofit.create(Api.class);
        Call<UserSchema> call = api.updateUserData(authToken, firstNameEditText.getText(), lastNameEditText.getText(), emailEditText.getText(), biographyEditText.getText(), twitterLinkEditText.getText(), facebookLinkEditText.getText(), linkedInLinkEditText.getText());
        call.enqueue(new Callback<UserSchema>() {
            @Override
            public void onResponse(Call<UserSchema> call, Response<UserSchema> response) {
                progressBar.setVisibility(View.INVISIBLE);
                UserSchema userSchema = response.body();
                if (userSchema.getStatus().equals("success")){
                    Toast.makeText(EditProfileActivity.this, "Profile Updated Successfully", Toast.LENGTH_SHORT).show();
                    initViewElementsWithUserInfo(userSchema);
                }else{
                    Toast.makeText(EditProfileActivity.this, userSchema.getErrorReason(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserSchema> call, Throwable t) {
                progressBar.setVisibility(View.INVISIBLE);
                Toast.makeText(EditProfileActivity.this, "An Error Occured", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void handleBackButton(View view) {
        EditProfileActivity.super.onBackPressed();
    }
}
