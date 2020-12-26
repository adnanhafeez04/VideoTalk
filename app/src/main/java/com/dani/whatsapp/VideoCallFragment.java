package com.dani.whatsapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class VideoCallFragment extends Fragment {

    private View videoCallView;
    private RecyclerView myContactList;

    private DatabaseReference Contactsref, UsersRef;
    private FirebaseAuth mAuth;
    private String currentUserID;
    private String calledBy = "";
    private ValueEventListener mListener;

    public VideoCallFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        videoCallView = inflater.inflate(R.layout.fragment_videocall, container, false);

        myContactList = (RecyclerView) videoCallView.findViewById(R.id.list_view);
        myContactList.setLayoutManager(new LinearLayoutManager(getContext()));
        myContactList.addItemDecoration(new DividerItemDecoration(myContactList.getContext(), DividerItemDecoration.VERTICAL));

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        Contactsref = FirebaseDatabase.getInstance().getReference().child("Contacts").child(currentUserID);
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");

       return videoCallView;
    }

    @Override
    public void onStart()
    {
        super.onStart();

        FirebaseRecyclerOptions options =
                new FirebaseRecyclerOptions.Builder<Contacts>()
                        .setQuery(Contactsref, Contacts.class)
                        .build();

        FirebaseRecyclerAdapter<Contacts, ContactsFragment.ContactsViewHoldedr> adapter
                = new FirebaseRecyclerAdapter<Contacts, ContactsFragment.ContactsViewHoldedr>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final ContactsFragment.ContactsViewHoldedr holder, int position, @NonNull Contacts model)
            {
                holder.textView.setVisibility(View.VISIBLE);
                holder.textView.setText("Tap icon to start a video call");

                final String userIDs = getRef(position).getKey();
                final String[] retImage = {"default_image"};

                UsersRef.child(userIDs).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        if (dataSnapshot.exists())
                        {
                            final String retName = dataSnapshot.child("name").getValue().toString();
                            final String retStatus = dataSnapshot.child("status").getValue().toString();

                            if (dataSnapshot.child("userState").hasChild("state"))
                            {
                                String state = dataSnapshot.child("userState").child("state").getValue().toString();
                                String date = dataSnapshot.child("userState").child("date").getValue().toString();
                                String time = dataSnapshot.child("userState").child("time").getValue().toString();

                                if (state.equals("online"))
                                {
                                    holder.onlineIcon.setVisibility(View.VISIBLE);
                                }
                                else if (state.equals("offline"))
                                {
                                    holder.onlineIcon.setVisibility(View.INVISIBLE);
                                }
                            }
                            else
                            {
                                holder.onlineIcon.setVisibility(View.INVISIBLE);
                            }

                            if (dataSnapshot.hasChild("image"))
                            {
                                retImage[0] = dataSnapshot.child("image").getValue().toString();
                                String userImage = dataSnapshot.child("image").getValue().toString();
                                String profileName = dataSnapshot.child("name").getValue().toString();
                                String profileStatus = dataSnapshot.child("status").getValue().toString();

                                holder.userName.setText(profileName);
                                holder.userStatus.setText(profileStatus);
                                Picasso.get().load(userImage).placeholder(R.drawable.profile_image).into(holder.profileImage);
                            }
                            else
                            {
                                String profileName = dataSnapshot.child("name").getValue().toString();
                                String profileStatus = dataSnapshot.child("status").getValue().toString();

                                holder.userName.setText(profileName);
                                holder.userStatus.setText(profileStatus);
                            }

                            holder.videoCallIcon.setVisibility(View.VISIBLE);
                            holder.videoCallIcon.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v)
                                {
                                    Intent callingIntent = new Intent(getContext(), CallingActivity.class);
                                    callingIntent.putExtra("visit_user_id", userIDs);
                                    startActivity(callingIntent);
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @NonNull
            @Override
            public ContactsFragment.ContactsViewHoldedr onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
            {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.users_display_layout,viewGroup, false);
                ContactsFragment.ContactsViewHoldedr viewHoldedr = new ContactsFragment.ContactsViewHoldedr(view);
                return viewHoldedr;
            }
        };

        myContactList.setAdapter(adapter);
        adapter.startListening();

        checkForReceivingCall();

    }

    public static class ContactsViewHoldedr extends RecyclerView.ViewHolder
    {
        TextView userName, userStatus, textView;
        CircleImageView profileImage;
        ImageView onlineIcon, videoCallIcon;

        public ContactsViewHoldedr(@NonNull View itemView)
        {
            super(itemView);

            userName = itemView.findViewById(R.id.user_profile_name);
            userStatus = itemView.findViewById(R.id.user_status);
            textView = itemView.findViewById(R.id.req_accept_cancel_text);
            profileImage = itemView.findViewById(R.id.users_profile_image);
            onlineIcon = itemView.findViewById(R.id.user_online_status);
            videoCallIcon = itemView.findViewById(R.id.video_call_btn);
        }
    }

    private void checkForReceivingCall()
    {
        Log.wtf("MainActivity", "checkForReceivingCall method is called");

        mListener = UsersRef.child(currentUserID).child("Ringing").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists() && dataSnapshot.hasChild("ringing"))
                {
                    calledBy = dataSnapshot.child("ringing").getValue().toString();

                    Intent callingIntent = new Intent(getActivity(), CallingActivity.class);
                    callingIntent.putExtra("visit_user_id", calledBy);
                    startActivity(callingIntent);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {

            }
        });
    }

    @Override
    public void onStop()
    {
        super.onStop();

        UsersRef.child(currentUserID).child("Ringing").removeEventListener(mListener);
    }
}
