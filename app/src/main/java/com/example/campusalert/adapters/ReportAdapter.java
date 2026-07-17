package com.example.campusalert.adapters;

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

public class ReportAdapter extends ListAdapter<Report, ReportAdapter.ReportViewHolder> {

    private OnItemClickListener listener;

    public ReportAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<Report> DIFF_CALLBACK = new DiffUtil.ItemCallback<Report>() {
        @Override
        public boolean areItemsTheSame(@NonNull Report oldItem, @NonNull Report newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Report oldItem, @NonNull Report newItem) {
            return oldItem.getStatus().equals(newItem.getStatus()) &&
                    oldItem.getPriority().equals(newItem.getPriority()) &&
                    oldItem.getTitle().equals(newItem.getTitle());
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
        holder.textViewTitle.setText(currentReport.getTitle());
        holder.textViewBuilding.setText(currentReport.getBuilding());
        holder.statusPill.setText(currentReport.getStatus());
        
        // Dynamic styling for status
        if ("Pending".equals(currentReport.getStatus())) {
            holder.statusPill.setBackgroundResource(R.drawable.bg_pill_pending);
            holder.statusIcon.setImageResource(android.R.drawable.ic_menu_info_details);
        } else if ("In Progress".equals(currentReport.getStatus())) {
            holder.statusPill.setBackgroundResource(R.drawable.bg_pill_progress);
            holder.statusIcon.setImageResource(android.R.drawable.ic_menu_edit);
        } else {
            holder.statusPill.setBackgroundResource(R.drawable.circle_green); // Reusing as fallback or create bg_pill_completed
            holder.statusIcon.setImageResource(android.R.drawable.checkbox_on_background);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Report report);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    class ReportViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewTitle;
        private TextView textViewBuilding;
        private TextView statusPill;
        private ImageView statusIcon;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.report_title);
            textViewBuilding = itemView.findViewById(R.id.report_building);
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