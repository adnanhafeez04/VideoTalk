package com.dani.whatsapp;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class ContactsFragment extends Fragment
{
    private View COntactsView;
    private RecyclerView myContactList;

    private DatabaseReference Contactsref, UsersRef;
    private FirebaseAuth mAuth;
    private String currentUserID;

    public ContactsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        COntactsView = inflater.inflate(R.layout.fragment_contacts, container, false);

        myContactList = (RecyclerView) COntactsView.findViewById(R.id.contacts_list);
        myContactList.setLayoutManager(new LinearLayoutManager(getContext()));
        myContactList.addItemDecoration(new DividerItemDecoration(myContactList.getContext(), DividerItemDecoration.VERTICAL));

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        Contactsref = FirebaseDatabase.getInstance().getReference().child("Contacts").child(currentUserID);
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        return COntactsView;
    }

    @Override
    public void onStart()
    {
        super.onStart();

        FirebaseRecyclerOptions options =
                new FirebaseRecyclerOptions.Builder<Contacts>()
                .setQuery(Contactsref, Contacts.class)
                .build();

        FirebaseRecyclerAdapter<Contacts, ContactsViewHoldedr> adapter
                = new FirebaseRecyclerAdapter<Contacts, ContactsViewHoldedr>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final ContactsViewHoldedr holder, int position, @NonNull Contacts model)
            {
                holder.textView.setVisibility(View.VISIBLE);
                holder.textView.setText("Tap icon to start a conversation");

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

//                                if (state.equals("online"))
//                                {
//                                    holder.onlineIcon.setVisibility(View.VISIBLE);
//                                }
//                                else if (state.equals("offline"))
//                                {
//                                    holder.onlineIcon.setVisibility(View.INVISIBLE);
//                                }
                            }
//                            else
//                            {
//                                holder.onlineIcon.setVisibility(View.INVISIBLE);
//                            }

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

                            holder.sendMessageIcon.setVisibility(View.VISIBLE);
                            holder.sendMessageIcon.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v)
                                {
                                    Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                    chatIntent.putExtra("visit_user_id", userIDs);
                                    chatIntent.putExtra("visit_user_name", retName);
                                    chatIntent.putExtra("visit_image", retImage[0]);
                                    startActivity(chatIntent);
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
            public ContactsViewHoldedr onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
            {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.users_display_layout,viewGroup, false);
                ContactsViewHoldedr viewHoldedr = new ContactsViewHoldedr(view);
                return viewHoldedr;
            }
        };

        myContactList.setAdapter(adapter);
        adapter.startListening();
    }

    public static class ContactsViewHoldedr extends RecyclerView.ViewHolder
    {
        TextView userName, userStatus, textView;
        CircleImageView profileImage;
        ImageView onlineIcon, videoCallIcon, sendMessageIcon;

        public ContactsViewHoldedr(@NonNull View itemView)
        {
            super(itemView);

            userName = itemView.findViewById(R.id.user_profile_name);
            userStatus = itemView.findViewById(R.id.user_status);
            textView = itemView.findViewById(R.id.req_accept_cancel_text);
            profileImage = itemView.findViewById(R.id.users_profile_image);
            onlineIcon = itemView.findViewById(R.id.user_online_status);
            videoCallIcon = itemView.findViewById(R.id.video_call_btn);
            sendMessageIcon = itemView.findViewById(R.id.send_message_btn);
        }
    }

}
