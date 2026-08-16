package com.example.campusalert.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusalert.R;
import com.example.campusalert.models.Report;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TaskAdapter extends ListAdapter<Report, TaskAdapter.TaskViewHolder> {

    private OnTaskActionListener listener;

    public TaskAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<Report> DIFF_CALLBACK = new DiffUtil.ItemCallback<Report>() {
        @Override
        public boolean areItemsTheSame(@NonNull Report oldItem, @NonNull Report newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Report oldItem, @NonNull Report newItem) {
            return oldItem.getStatus().equals(newItem.getStatus()) &&
                    (oldItem.getStaffComment() == null ? newItem.getStaffComment() == null : oldItem.getStaffComment().equals(newItem.getStaffComment()));
        }
    };

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Report report = getItem(position);
        holder.title.setText(report.getTitle());
        holder.location.setText("Location: " + report.getBuilding());
        holder.category.setText("Issue: " + report.getCategory());
        
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        holder.date.setText("Reported: " + sdf.format(new Date(report.getTimestamp())));

        if ("Completed".equalsIgnoreCase(report.getStatus())) {
            holder.btnComplete.setVisibility(View.GONE);
        } else {
            holder.btnComplete.setVisibility(View.VISIBLE);
        }
    }

    public interface OnTaskActionListener {
        void onView(Report report);
        void onComplete(Report report);
    }

    public void setOnTaskActionListener(OnTaskActionListener listener) {
        this.listener = listener;
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView title, location, category, date;
        Button btnView, btnComplete;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.task_title);
            location = itemView.findViewById(R.id.task_location);
            category = itemView.findViewById(R.id.task_category);
            date = itemView.findViewById(R.id.task_date);
            btnView = itemView.findViewById(R.id.btn_view_task);
            btnComplete = itemView.findViewById(R.id.btn_complete_task);

            btnView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (listener != null && pos != RecyclerView.NO_POSITION) {
                    listener.onView(getItem(pos));
                }
            });

            btnComplete.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (listener != null && pos != RecyclerView.NO_POSITION) {
                    listener.onComplete(getItem(pos));
                }
            });
        }
    }
}
