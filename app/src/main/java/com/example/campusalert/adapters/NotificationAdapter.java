package com.example.campusalert.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusalert.R;
import com.example.campusalert.models.Notification;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationAdapter extends ListAdapter<Notification, NotificationAdapter.NotificationViewHolder> {

    public NotificationAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<Notification> DIFF_CALLBACK = new DiffUtil.ItemCallback<Notification>() {
        @Override
        public boolean areItemsTheSame(@NonNull Notification oldItem, @NonNull Notification newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Notification oldItem, @NonNull Notification newItem) {
            return oldItem.isRead() == newItem.isRead() && 
                   oldItem.getMessage().equals(newItem.getMessage());
        }
    };

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = getItem(position);
        holder.title.setText(notification.getTitle());
        holder.message.setText(notification.getMessage());
        
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
        holder.time.setText(sdf.format(new Date(notification.getTimestamp())));
        
        holder.unreadDot.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView title, message, time;
        View unreadDot;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.noti_title);
            message = itemView.findViewById(R.id.noti_message);
            time = itemView.findViewById(R.id.noti_time);
            unreadDot = itemView.findViewById(R.id.unread_dot);
        }
    }
}