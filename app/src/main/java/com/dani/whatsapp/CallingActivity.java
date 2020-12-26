package com.dani.whatsapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

public class CallingActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private TextView nameContact;
    private ImageView profileImage;
    private ImageView cancelCallButton, acceptCallButton;
    private String receiverUserId = "", receiverUserName = "", receiverUserImage = "";
    private String senderUserId = "", senderUserName = "", senderUserImage = "", checker = "";
    private String callingID = "", ringingID = "";
    private DatabaseReference usersRef;
    private FirebaseAuth mAuth;
    private ValueEventListener mListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calling);

        mToolbar = findViewById(R.id.calling_page_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Video Call");

        mAuth = FirebaseAuth.getInstance();

        senderUserId = mAuth.getCurrentUser().getUid();

        receiverUserId = getIntent().getStringExtra("visit_user_id");

        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        nameContact = findViewById(R.id.name_calling);
        profileImage = findViewById(R.id.profile_image_calling);
        acceptCallButton = findViewById(R.id.make_call);
        cancelCallButton = findViewById(R.id.cancel_call);

        cancelCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                checker = "clicked";

                cancelCallingUser();
            }
        });

        acceptCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                final HashMap<String, Object> callingPickUpMap = new HashMap<>();
                callingPickUpMap.put("picked", "picked");

                usersRef.child(senderUserId).child("Ringing").updateChildren(callingPickUpMap)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task)
                            {
                                if (task.isComplete())
                                {
                                    Intent intent = new Intent(CallingActivity.this, VideoChatActivity.class);
                                    startActivity(intent);
                                    CallingActivity.this.finish();
                                }
                            }
                        });
            }
        });

        getAndSetUserProfileInfo();

    }

    private void cancelCallingUser()
    {
        //from sender side

        usersRef.child(senderUserId).child("Calling")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        if (dataSnapshot.exists() && dataSnapshot.hasChild("calling"))
                        {
                            callingID = dataSnapshot.child("calling").getValue().toString();

                            usersRef.child(callingID).child("Ringing")
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task)
                                        {
                                            if (task.isSuccessful())
                                            {
                                                usersRef.child(senderUserId)
                                                        .child("Calling")
                                                        .removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task)
                                                            {
                                                                CallingActivity.this.finish();
                                                            }
                                                        });
                                            }
                                        }
                                    });
                        }
                        else
                        {
                            CallingActivity.this.finish();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

        //from receiver side

        usersRef.child(senderUserId).child("Ringing")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        if (dataSnapshot.exists() && dataSnapshot.hasChild("ringing"))
                        {
                            ringingID = dataSnapshot.child("ringing").getValue().toString();

                            usersRef.child(ringingID).child("Calling")
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task)
                                        {
                                            if (task.isSuccessful())
                                            {
                                                usersRef.child(senderUserId)
                                                        .child("Ringing")
                                                        .removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task)
                                                            {
                                                                CallingActivity.this.finish();
                                                            }
                                                        });
                                            }
                                        }
                                    });
                        }
                        else
                        {
                            CallingActivity.this.finish();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    private void getAndSetUserProfileInfo()
    {
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.child(receiverUserId).exists())
                {
                    receiverUserImage = dataSnapshot.child(receiverUserId).child("image").getValue().toString();
                    receiverUserName = dataSnapshot.child(receiverUserId).child("name").getValue().toString();

                    nameContact.setText(receiverUserName);

                    Picasso.get().load(receiverUserImage).placeholder(R.drawable.profile_image).into(profileImage);
                }
                if (dataSnapshot.child(senderUserId).exists())
                {
                    senderUserImage = dataSnapshot.child(senderUserId).child("image").getValue().toString();
                    senderUserName = dataSnapshot.child(senderUserId).child("name").getValue().toString();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        usersRef.child(receiverUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        if (!checker.equals("clicked") && !dataSnapshot.hasChild("Calling") && !dataSnapshot.hasChild("Ringing"))
                        {
                            final HashMap<String, Object> callingInfo = new HashMap<>();
                            callingInfo.put("calling", receiverUserId);

                            usersRef.child(senderUserId).child("Calling")
                                    .updateChildren(callingInfo)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task)
                                        {
                                            if (task.isSuccessful())
                                            {
                                                final HashMap<String, Object> ringingInfo = new HashMap<>();
                                                ringingInfo.put("ringing", senderUserId);

                                                usersRef.child(receiverUserId).child("Ringing")
                                                        .updateChildren(ringingInfo);
                                            }
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError)
                    {

                    }
                });

        mListener =  usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.child(senderUserId).hasChild("Ringing") && !dataSnapshot.child(senderUserId).hasChild("Calling"))
                {
                    acceptCallButton.setVisibility(View.VISIBLE);
                }

                if (dataSnapshot.child(receiverUserId).child("Ringing").hasChild("picked"))
                {
                    Intent intent = new Intent(CallingActivity.this, VideoChatActivity.class);
                    startActivity(intent);
                    CallingActivity.this.finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {


            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();

        usersRef.removeEventListener(mListener);
    }
}
