package com.dani.whatsapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


public class VideoChatActivity extends AppCompatActivity implements Session.SessionListener, PublisherKit.PublisherListener
{
    private static String API_Key = "46525322";
    private static String SESSION_ID = "2_MX40NjUyNTMyMn5-MTU4Nzg0MzA0NzQ1Mn5RRlhaUVFwWEtrdERuRUwwZ0dvaHdQUFR-fg";
    private static String TOKEN = "T1==cGFydG5lcl9pZD00NjUyNTMyMiZzaWc9YzVhZjMxM2Y1YjQ1NjVjNGZhOWMyMjQ1NTZmNTVjYjZkNWU5YzMxMjpzZXNzaW9uX2lkPTJfTVg0ME5qVXlOVE15TW41LU1UVTROemcwTXpBME56UTFNbjVSUmxoYVVWRndXRXRyZEVSdVJVd3daMGR2YUhkUVVGUi1mZyZjcmVhdGVfdGltZT0xNTg3ODQzMDg0Jm5vbmNlPTAuOTEyMzY0MDg5NjU3MjAwMyZyb2xlPXB1Ymxpc2hlciZleHBpcmVfdGltZT0xNTkwNDM1MDg3JmluaXRpYWxfbGF5b3V0X2NsYXNzX2xpc3Q9";
    private static final String LOG_TAG = VideoChatActivity.class.getSimpleName();
    private static final int RC_VIDEO_APP_PERM = 124;

    private FrameLayout mPublisherViewController;
    private FrameLayout mSubscriberViewController;

    private Session mSession;
    private Publisher mPublisher;
    private Subscriber mSubscriber;

    private DatabaseReference userRef;
    private FirebaseAuth mAuth;

    private ImageView closeVideoChatBtn;
    private String userID = "";
    private String callingID, ringingID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_chat);

        mAuth = FirebaseAuth.getInstance();

        userID = mAuth.getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference().child("Users");

        closeVideoChatBtn = findViewById(R.id.close_video_chat_btn);

        closeVideoChatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                endCall();
            }
        });

        requestPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, VideoChatActivity.this);
    }

    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private void requestPermissions()
    {
        String[] perms = {Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};

        if (EasyPermissions.hasPermissions(this, perms))
        {
            mPublisherViewController = findViewById(R.id.publisher_container);
            mSubscriberViewController = findViewById(R.id.subscriber_container);

            //1st step initialize and connect to the session
            mSession = new Session.Builder(this, API_Key, SESSION_ID).build();
            mSession.setSessionListener(VideoChatActivity.this);
            mSession.connect(TOKEN);
        }
        else
        {
            EasyPermissions.requestPermissions(this, "VideoTalk needs Mic and Camera permissions, please allow", RC_VIDEO_APP_PERM, perms);
        }
    }

    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {

    }

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {

    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {

    }

    //2nd step publishing a stream to the session
    @Override
    public void onConnected(Session session)
    {
        Log.i(LOG_TAG, "Session connected");

        mPublisher = new Publisher.Builder(this).build();
        mPublisher.setPublisherListener(VideoChatActivity.this);

        mPublisherViewController.addView(mPublisher.getView());

        if (mPublisher.getView() instanceof GLSurfaceView)
        {
            ((GLSurfaceView) mPublisher.getView()).setZOrderOnTop(true);
        }

        mSession.publish(mPublisher);
    }

    @Override
    public void onDisconnected(Session session)
    {
        Log.i(LOG_TAG, "stream Disconnected");
    }

    //3rd step subscribing the already published stream
    @Override
    public void onStreamReceived(Session session, Stream stream)
    {
        Log.i(LOG_TAG, "stream received");

        mSubscriber = new Subscriber.Builder(this, stream).build();
        mSession.subscribe(mSubscriber);
        mSubscriberViewController.addView(mSubscriber.getView());
    }

    @Override
    public void onStreamDropped(Session session, Stream stream)
    {
        Log.i(LOG_TAG, "stream Dropped");

        if (mSubscriber != null)
        {
            mSubscriber = null;

            mSubscriberViewController.removeAllViews();
        }
    }

    @Override
    public void onError(Session session, OpentokError opentokError)
    {
        Log.i(LOG_TAG, "stream Error");
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    private void endCall()
    {
        //from sender side

        userRef.child(userID).child("Calling").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists() && dataSnapshot.hasChild("calling"))
                {
                    callingID = dataSnapshot.child("calling").getValue().toString();

                    userRef.child(callingID).child("Ringing").removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task)
                        {
                            if (task.isSuccessful())
                            {
                                userRef.child(userID).child("Calling").removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task)
                                    {
                                        if (mPublisher != null)
                                        {
                                            mPublisher.destroy();
                                        }
                                        if (mSubscriber != null)
                                        {
                                            mSubscriber.destroy();
                                        }

                                        startActivity(new Intent(VideoChatActivity.this, MainActivity.class));
                                        finish();

                                    }
                                });
                            }
                        }
                    });
                }
                else
                {
                    if (mPublisher != null)
                    {
                        mPublisher.destroy();
                    }
                    if (mSubscriber != null)
                    {
                        mSubscriber.destroy();
                    }

                    startActivity(new Intent(VideoChatActivity.this, MainActivity.class));
                    finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        //from receiver side

        userRef.child(userID).child("Ringing").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists() && dataSnapshot.hasChild("ringing"))
                {
                    ringingID = dataSnapshot.child("ringing").getValue().toString();

                    userRef.child(ringingID).child("Calling").removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task)
                        {
                            if (task.isSuccessful())
                            {
                                userRef.child(userID).child("Ringing").removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task)
                                    {
                                        if (mPublisher != null)
                                        {
                                            mPublisher.destroy();
                                        }
                                        if (mSubscriber != null)
                                        {
                                            mSubscriber.destroy();
                                        }

                                        startActivity(new Intent(VideoChatActivity.this, MainActivity.class));
                                        finish();
                                    }
                                });
                            }
                        }
                    });
                }
                else
                {
                    if (mPublisher != null)
                    {
                        mPublisher.destroy();
                    }
                    if (mSubscriber != null)
                    {
                        mSubscriber.destroy();
                    }

                    startActivity(new Intent(VideoChatActivity.this, MainActivity.class));
                    finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

}
