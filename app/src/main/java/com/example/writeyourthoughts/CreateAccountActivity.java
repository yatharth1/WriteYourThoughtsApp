package com.example.writeyourthoughts;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import util.JournalApi;

public class CreateAccountActivity extends AppCompatActivity {
    private Button loginButton;
    private Button createAccountButton;
    private EditText emailEditText;
    private EditText passwordEditText;
    private ProgressBar progressBar;
    private EditText userNameEditText;

    //Connecting Firebase Auth to App
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser currentUser;

    //Firestore connection
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference = db.collection("Users");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        firebaseAuth = FirebaseAuth.getInstance();

        createAccountButton = findViewById(R.id.create_acc_button);
        progressBar = findViewById(R.id.create_acct_progress);
        emailEditText = findViewById(R.id.email_account);
        passwordEditText = findViewById(R.id.password_account);
        userNameEditText = findViewById(R.id.username_account);

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                currentUser = firebaseAuth.getCurrentUser();

                if (currentUser != null) {
                    //user is already logged in
                } else {
                    //no user yet..

                }
            }
        };

        createAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(emailEditText.getText().toString())
                        && !TextUtils.isEmpty(passwordEditText.getText().toString())
                        && !TextUtils.isEmpty(userNameEditText.getText().toString())) {
                    String email = emailEditText.getText().toString().trim();
                    String password = passwordEditText.getText().toString().trim();
                    String username = userNameEditText.getText().toString().trim();
                    createUserEmailAccount(email, password, username);
                } else {
                    Toast.makeText(CreateAccountActivity.this, "Oops! Seems Like You Haven't Enter Required Field Information",
                            Toast.LENGTH_SHORT).show();
                }

            }
        });

    }

    private void createUserEmailAccount(String email, String password, String username) {
        if (!TextUtils.isEmpty(email)
                && !TextUtils.isEmpty(password)
                && !TextUtils.isEmpty(username)) {

            progressBar.setVisibility(View.VISIBLE);

            firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            //we take user to AddJournalActivity
                            currentUser = firebaseAuth.getCurrentUser();
                            assert currentUser != null;
                            String currUserId = currentUser.getUid();

                            //Create a user map so we can create a user in the user collection
                            Map<String, String> userObj = new HashMap<>();
                            userObj.put("userId", currUserId);
                            userObj.put("username", username);

                            //save to our firestore database
                            collectionReference.add(userObj)
                                    .addOnSuccessListener(documentReference -> documentReference.get()
                                            .addOnCompleteListener(task1 -> {
                                                if (Objects.requireNonNull(task1.getResult()).exists()) {

                                                    progressBar.setVisibility(View.INVISIBLE);

                                                    String name = task1.getResult()
                                                            .getString("username");

                                                    JournalApi journalApi = JournalApi.getInstance(); //Global API
                                                    journalApi.setUserId(currUserId);
                                                    journalApi.setUsername(name);

                                                    Intent intent = new Intent(CreateAccountActivity.this,
                                                            PostJournalActivity.class);
                                                    intent.putExtra("username", name);
                                                    intent.putExtra("userId", currUserId);
                                                    startActivity(intent);

                                                } else {
                                                    progressBar.setVisibility(View.INVISIBLE);

                                                }

                                            }))
                                    .addOnFailureListener(e -> {

                                    });


                        } else {
                            //something went wrong
                        }
                    })
                    .addOnFailureListener(e -> {

                    });

        } else {

        }

    }

    @Override
    protected void onStart() {
        super.onStart();

        currentUser = firebaseAuth.getCurrentUser();
        firebaseAuth.addAuthStateListener(authStateListener);
    }
}