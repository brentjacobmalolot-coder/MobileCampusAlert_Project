package com.example.campusalert.adapters;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusalert.R;
import com.example.campusalert.models.Report;

import java.util.Objects;

public class ReportAdapter extends ListAdapter<Report, ReportAdapter.ReportViewHolder> {

    private OnItemClickListener listener;

    public ReportAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<Report> DIFF_CALLBACK = new DiffUtil.ItemCallback<Report>() {
        @Override
        public boolean areItemsTheSame(@NonNull Report oldItem, @NonNull Report newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Report oldItem, @NonNull Report newItem) {
            return Objects.equals(oldItem.getStatus(), newItem.getStatus()) &&
                    Objects.equals(oldItem.getPriority(), newItem.getPriority()) &&
                    Objects.equals(oldItem.getTitle(), newItem.getTitle()) &&
                    Objects.equals(oldItem.getBuilding(), newItem.getBuilding()) &&
                    oldItem.isFlagged() == newItem.isFlagged() &&
                    oldItem.isArchived() == newItem.isArchived();
        }
    };

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report, parent, false);
        return new ReportViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        Report currentReport = getItem(position);
        if (currentReport == null) return;

        holder.textViewTitle.setText(currentReport.getTitle() != null ? currentReport.getTitle() : "No Title");
        holder.textViewBuilding.setText(currentReport.getBuilding() != null ? currentReport.getBuilding() : "No Location");
        
        // Priority Tag logic
        String priority = currentReport.getPriority() != null ? currentReport.getPriority() : "Medium";
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

        String status = currentReport.getStatus() != null ? currentReport.getStatus() : "Pending";
        
        // Dynamic styling for status
        if ("Pending".equalsIgnoreCase(status)) {
            holder.statusPill.setBackgroundTintList(ColorStateList.valueOf(holder.itemView.getContext().getColor(R.color.soft_yellow)));
            holder.statusIcon.setImageResource(android.R.drawable.ic_menu_info_details);
            holder.statusIcon.setColorFilter(holder.itemView.getContext().getColor(R.color.institutional_yellow));
        } else if ("In Progress".equalsIgnoreCase(status)) {
            holder.statusPill.setBackgroundTintList(ColorStateList.valueOf(holder.itemView.getContext().getColor(R.color.soft_sage)));
            holder.statusIcon.setImageResource(android.R.drawable.ic_menu_edit);
            holder.statusIcon.setColorFilter(holder.itemView.getContext().getColor(R.color.forest_green));
        } else if ("Completed".equalsIgnoreCase(status)) {
            holder.statusPill.setBackgroundTintList(ColorStateList.valueOf(holder.itemView.getContext().getColor(R.color.forest_green)));
            holder.statusIcon.setImageResource(android.R.drawable.checkbox_on_background);
            holder.statusIcon.setColorFilter(holder.itemView.getContext().getColor(R.color.white));
        }

        // Flag indicator
        if (currentReport.isFlagged()) {
            holder.textViewTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.btn_star_big_on, 0);
        } else {
            holder.textViewTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Report report);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    class ReportViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewTitle;
        private final TextView textViewBuilding;
        private final TextView priorityTag;
        private final View statusPill;
        private final ImageView statusIcon;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.report_title);
            textViewBuilding = itemView.findViewById(R.id.report_building);
            priorityTag = itemView.findViewById(R.id.priority_tag);
            statusPill = itemView.findViewById(R.id.status_pill);
            statusIcon = itemView.findViewById(R.id.status_icon);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(getItem(position));
                }
            });
        }
    }
}
