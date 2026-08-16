package com.example.campusalert;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.campusalert.adapters.NotificationAdapter;
import com.example.campusalert.databinding.ActivityNotificationsBinding;
import com.example.campusalert.models.Notification;
import com.example.campusalert.utils.SessionManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {
    private ActivityNotificationsBinding binding;
    private NotificationAdapter adapter;
    private SessionManager sessionManager;
    private DatabaseReference mDatabase;
    private static final String DATABASE_URL = "https://mobile-application-db-321ff-default-rtdb.firebaseio.com/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();

        setupToolbar();
        setupRecyclerView();
        loadNotifications();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupRecyclerView() {
        binding.recyclerNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter();
        binding.recyclerNotifications.setAdapter(adapter);
    }

    private void loadNotifications() {
        String encodedEmail = sessionManager.getUserEmail().replace(".", ",");
        mDatabase.child("notifications").child(encodedEmail)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Notification> notifications = new ArrayList<>();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            Notification n = data.getValue(Notification.class);
                            if (n != null) {
                                notifications.add(n);
                                // Mark as read when seen
                                if (!n.isRead()) {
                                    data.getRef().child("read").setValue(true);
                                }
                            }
                        }
                        Collections.reverse(notifications); // Newest first
                        adapter.submitList(notifications);
                        
                        binding.textNoNotifications.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}