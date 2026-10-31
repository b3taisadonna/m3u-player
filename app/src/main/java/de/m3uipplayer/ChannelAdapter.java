package de.m3uipplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.Holder> {
    public interface OnChannelClick { void onClick(Channel channel); }
    private final List<Channel> channels = new ArrayList<>();
    private final OnChannelClick listener;

    public ChannelAdapter(OnChannelClick listener) { this.listener = listener; }
    public void replace(List<Channel> items) { channels.clear(); channels.addAll(items); notifyDataSetChanged(); }
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_channel, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull Holder h, int position) {
        Channel c = channels.get(position);
        h.number.setText(formatChannelNumber(position + 1));
        h.name.setText(c.name); h.group.setText(c.group);
        h.itemView.setOnClickListener(v -> listener.onClick(c));
    }

    private static String formatChannelNumber(int number) {
        if (number < 10) return "00" + number;
        if (number < 100) return "0" + number;
        return Integer.toString(number);
    }

    @Override public int getItemCount() { return channels.size(); }
    static class Holder extends RecyclerView.ViewHolder {
        final TextView number, name, group;
        Holder(View v) { super(v); number=v.findViewById(R.id.channel_number); name=v.findViewById(R.id.channel_name); group=v.findViewById(R.id.channel_group); }
    }
}
