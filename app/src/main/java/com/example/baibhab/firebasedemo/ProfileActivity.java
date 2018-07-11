package com.example.baibhab.firebasedemo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.w3c.dom.Text;

import java.io.IOException;

public class ProfileActivity extends AppCompatActivity implements View.OnClickListener{

    FirebaseAuth firebaseAuth;

    private Button buttonLogout, buttonSave;
    private TextView textViewUserEmail;
    private ImageView imageView;
    private EditText editTextProfileName;
    private ProgressBar progressBar;

    private String profileImageURL;

    private static final int CHOOSE_IMAGE = 101;

    private Uri uriProfileImage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        firebaseAuth = FirebaseAuth.getInstance();

        if(firebaseAuth.getCurrentUser() == null) {
            finish();
            startActivity(new Intent(this, LoginActivity.class));
        }
        FirebaseUser user = firebaseAuth.getCurrentUser();

        textViewUserEmail = findViewById(R.id.textViewUserEmail);
        buttonLogout = findViewById(R.id.buttonLogout);
        buttonSave = findViewById(R.id.buttonSave);
        imageView = findViewById(R.id.imageView);
        editTextProfileName = findViewById(R.id.editTextProfileName);
        progressBar = findViewById(R.id.progressBar);

        textViewUserEmail.setText("Welcome " + user.getEmail());

        buttonLogout.setOnClickListener(this);
        imageView.setOnClickListener(this);
        editTextProfileName.setOnClickListener(this);
        buttonSave.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadUserProfile();
    }

    private void loadUserProfile() {

        FirebaseUser user = firebaseAuth.getCurrentUser();

        if(user != null) {
            if(user.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(user.getPhotoUrl().toString())
                        .into(imageView);
            }

            if(user.getDisplayName() != null) {
                editTextProfileName.setText(user.getDisplayName());
            }
        }
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()){

            case R.id.buttonLogout :

                firebaseAuth.signOut();
                finish();
                startActivity(new Intent(this, LoginActivity.class));

                break;

            case R.id.imageView :

                showImageChooser();
                break;

            case R.id.buttonSave :

                saveUserInfo();
                break;
        }
    }

    private void saveUserInfo() {

        String userName = editTextProfileName.getText().toString();
        if(userName.isEmpty()) {
            editTextProfileName.setError("Please enter name");
            editTextProfileName.requestFocus();
            return;
        }

        FirebaseUser user = firebaseAuth.getCurrentUser();

         if (user != null && profileImageURL != null) {
             UserProfileChangeRequest profileChangeRequest = new UserProfileChangeRequest.Builder()
                     .setDisplayName(userName)
                     .setPhotoUri(Uri.parse(profileImageURL))
                     .build();

             user.updateProfile(profileChangeRequest)
                     .addOnCompleteListener(new OnCompleteListener<Void>() {
                         @Override
                         public void onComplete(@NonNull Task<Void> task) {
                             if(task.isSuccessful()){
                                 Toast.makeText(getApplicationContext(), "Profile Updated Successfully", Toast.LENGTH_SHORT).show();
                             }
                         }
                     });
         }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CHOOSE_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            uriProfileImage = data.getData();

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uriProfileImage);
                imageView.setImageBitmap(bitmap);

                uploadImageToFirebaseStorage();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void uploadImageToFirebaseStorage() {

        final StorageReference profileImageRef = FirebaseStorage.getInstance().getReference("profilepics/" + System.currentTimeMillis() + ".jpg");

        if (uriProfileImage != null) {
            progressBar.setVisibility(View.VISIBLE);
            profileImageRef.putFile(uriProfileImage)

                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressBar.setVisibility(View.GONE);

                            profileImageURL = taskSnapshot.getDownloadUrl().toString();
                        }
                    })

                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(ProfileActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void showImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), CHOOSE_IMAGE );
    }
}
