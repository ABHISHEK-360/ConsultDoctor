package com.abhishek360.dev.hellodoctor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

import static com.abhishek360.dev.hellodoctor.LoginActivity.spKey;

public class ChatActivity extends AppCompatActivity
{
    private SharedPreferences sharedPreferences;
    private RecyclerView recyclerView;
    private TextView userMessage;
    private View avatar;
    private FirestoreRecyclerAdapter adapter;
    private FirebaseFirestore firebaseFirestore;
    private StorageReference storageReference;
    private FirebaseStorage firebaseStorage;
    private String myUID,currDocName,currDocID,my_name;
    private boolean isDoc=false;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        FirebaseApp.initializeApp(this);

        firebaseFirestore = FirebaseFirestore.getInstance();

        ActionBar actionBar= getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        sharedPreferences= getSharedPreferences(spKey,MODE_PRIVATE);
        myUID= sharedPreferences.getString(LoginActivity.spUID,null);
        my_name= sharedPreferences.getString(LoginActivity.spFullNameKey,null);
        isDoc=sharedPreferences.getBoolean(LoginActivity.spIsDoc,false);

        currDocID=getIntent().getExtras().getString("docID",null);
        currDocName=getIntent().getExtras().getString("docName","unknown");


        userMessage=findViewById(R.id.chat_user_message);

        recyclerView= findViewById(R.id.messages_view);
        LinearLayoutManager layoutManager= new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        setupChatAdapter();
    }

    private void setupChatAdapter()
    {
        Query q;
        if(isDoc)
        {
            q = firebaseFirestore.collection("/CHAT/"+currDocID+"/"+myUID).orderBy("timeStamp");

        }
        else
        {
            q = firebaseFirestore.collection("/CHAT/"+myUID+"/"+currDocID).orderBy("timeStamp");

        }


        FirestoreRecyclerOptions<ChatAdapter> res = new FirestoreRecyclerOptions.Builder<ChatAdapter>()
                .setQuery(q, ChatAdapter.class).build();

        adapter = new FirestoreRecyclerAdapter<ChatAdapter, ChatAdapter.ChatViewHolder>(res)
        {


            @NonNull
            @Override
            public ChatAdapter.ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
            {
                LayoutInflater inf = LayoutInflater.from(parent.getContext());

                View view = inf.inflate(R.layout.their_message,parent,false);

                return new ChatAdapter.ChatViewHolder(view);
            }

            @Override
            public void onError(@NonNull FirebaseFirestoreException e)
            {
                super.onError(e);
                Log.e("error", e.getMessage());
                //Toast.makeText(getContext(),""+e,Toast.LENGTH_LONG).show();


            }

            @Override
            protected void onBindViewHolder(@NonNull final ChatAdapter.ChatViewHolder holder, int position, @NonNull ChatAdapter model)
            {

                if(model.getSenderID().equals(myUID))
                {
                    holder.avatar.setVisibility(View.GONE);
                    holder.their_message.setVisibility(View.GONE);
                    holder.name.setVisibility(View.GONE);
                    holder.my_message.setVisibility(View.VISIBLE);

                    holder.my_message.setText(model.getMessage());


                }
                else
                {
                    holder.name.setText(""+currDocName);
                    holder.avatar.setVisibility(View.VISIBLE);

                    holder.their_message.setVisibility(View.VISIBLE);
                    holder.name.setVisibility(View.VISIBLE);
                    holder.my_message.setVisibility(View.GONE);

                    holder.their_message.setText(model.getMessage());

                }

            }

        };

        adapter.notifyDataSetChanged();
       // recyclerView.scrollToPosition(adapter.getItemCount()-1);
        recyclerView.setAdapter(adapter);

    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }

    public void sendMessage(View view)
    {
        Map<String, Object> user = new HashMap<>();

        try {
            InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        } catch (Exception e)
        {
            Log.e("Keyboard",e.getMessage());

        }

        user.put("message",userMessage.getText().toString());
        user.put("senderID",myUID);
        user.put("timeStamp", FieldValue.serverTimestamp());

        Map<String,Object> initdata= new HashMap<>();

        initdata.put("timeStamp",FieldValue.serverTimestamp());

        String collectionPath,collectionPathMessage;

        if(isDoc)
        {
            initdata.put("uID",currDocID);
            initdata.put("username",currDocName);
            collectionPath="/DOCTORS/"+myUID+"/"+"messages";
            collectionPathMessage="/CHAT/"+currDocID+"/"+myUID;

            firebaseFirestore.collection(collectionPath).document(currDocID).set(initdata,SetOptions.merge()).addOnCompleteListener(
                    new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful())
                            {
                                Log.d("chat_init","done");
                            }
                        }
                    }
            );

        }
        else
        {
            initdata.put("uID",myUID);
            initdata.put("username",my_name);
            collectionPath="/DOCTORS/"+currDocID+"/"+"messages";
            collectionPathMessage="/CHAT/"+myUID+"/"+currDocID;
            firebaseFirestore.collection(collectionPath).document(myUID).set(initdata,SetOptions.merge()).addOnCompleteListener(
                    new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful())
                            {
                                Log.d("chat_init","done");
                            }
                        }
                    }
            );


        }

        firebaseFirestore.collection(collectionPathMessage).add(user).addOnSuccessListener(
                new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference)
                    {
                        userMessage.setText("");

                        adapter.notifyDataSetChanged();
                        recyclerView.scrollToPosition(adapter.getItemCount()-1);


                        Log.d("Chat message","Sent");

                    }
                }
        )
                .addOnFailureListener(new OnFailureListener()
                {
                    @Override
                    public void onFailure(@NonNull Exception e)
                    {
                        Log.e("chat message error", e.getMessage());

                    }
                });
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.menu_logout:
                sharedPreferences.edit().clear().apply();
                FirebaseAuth.getInstance().signOut();

                Intent loginIntent = new Intent(this, LoginActivity.class);
                startActivity(loginIntent);
                finish();
                return true;

            case android.R.id.home:
                finish();
                return  true;
        }
        return super.onOptionsItemSelected(item);

    }
}
