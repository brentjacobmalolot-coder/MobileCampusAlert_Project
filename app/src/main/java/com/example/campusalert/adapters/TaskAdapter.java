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
                    oldItem.isFlagged() == newItem.isFlagged() &&
                    oldItem.isArchived() == newItem.isArchived() &&
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

        // Priority Tag logic
        String priority = report.getPriority() != null ? report.getPriority() : "Medium";
        holder.priorityTag.setText(priority.toUpperCase());
        
        int priorityColor;
        if ("High".equalsIgnoreCase(priority)) {
            priorityColor = holder.itemView.getContext().getColor(R.color.priority_high);
        } else if ("Low".equalsIgnoreCase(priority)) {
            priorityColor = holder.itemView.getContext().getColor(R.color.priority_low);
        } else {
            priorityColor = holder.itemView.getContext().getColor(R.color.priority_medium);
        }
        holder.priorityTag.getBackground().setTint(priorityColor);

        if ("Completed".equalsIgnoreCase(report.getStatus())) {
            holder.btnComplete.setVisibility(View.GONE);
        } else {
            holder.btnComplete.setVisibility(View.VISIBLE);
        }

        // Visual indicator for flagged reports
        if (report.isFlagged()) {
            holder.title.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.btn_star_big_on, 0);
        } else {
            holder.title.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
    }

    public interface OnTaskActionListener {
        void onView(Report report);
        void onComplete(Report report);
        void onManage(Report report);
    }

    public void setOnTaskActionListener(OnTaskActionListener listener) {
        this.listener = listener;
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView title, location, category, date, priorityTag;
        Button btnView, btnComplete;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.task_title);
            location = itemView.findViewById(R.id.task_location);
            category = itemView.findViewById(R.id.task_category);
            date = itemView.findViewById(R.id.task_date);
            priorityTag = itemView.findViewById(R.id.priority_tag);
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

            itemView.setOnLongClickListener(v -> {
                int pos = getAdapterPosition();
                if (listener != null && pos != RecyclerView.NO_POSITION) {
                    listener.onManage(getItem(pos));
                    return true;
                }
                return false;
            });
        }
    }
}
