package com.itmbaq1.itmda_groupproj;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.itmbaq1.itmda_groupproj.databinding.ItemSessionBinding;

import java.util.List;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.VH> {
    private final List<SessionSummary> items;
    public SessionAdapter(List<SessionSummary> items) { this.items = items; }

    static class VH extends RecyclerView.ViewHolder {
        final ItemSessionBinding b;
        VH(ItemSessionBinding b) { super(b.getRoot()); this.b = b; }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemSessionBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        SessionSummary s = items.get(position);
        h.b.tvTitle.setText(s.language + ": " + s.topic);
        h.b.tvScore.setText(s.score + "%");
    }

    @Override
    public int getItemCount() { return items.size(); }
}
