package com.itmbaq1.itmda_groupproj;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int USER = 0, AI = 1;
    private final List<ChatMessage> messages = new ArrayList<>();

    public void addMessage(ChatMessage m) {
        messages.add(m);
        notifyItemInserted(messages.size() - 1);
    }

    static class MsgHolder extends RecyclerView.ViewHolder {
        final TextView tv;
        MsgHolder(View v) { super(v); tv = v.findViewById(R.id.tvMessage); }
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).fromUser ? USER : AI;
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = (viewType == USER) ? R.layout.item_chat_user : R.layout.item_chat_ai;
        return new MsgHolder(LayoutInflater.from(parent.getContext()).inflate(layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((MsgHolder) holder).tv.setText(messages.get(position).text);
    }

    @Override
    public int getItemCount() { return messages.size(); }
}
