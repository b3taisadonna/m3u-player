package de.m3uipplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.Holder> {
    public interface OnChannelClick { void onClick(Channel channel); }
    public interface OnFavoriteClick { void onClick(Channel channel); }
    private final List<Channel> channels = new ArrayList<>();
    private final Set<String> favoriteUrls = new HashSet<>();
    private final OnChannelClick channelListener;
    private final OnFavoriteClick favoriteListener;

    public ChannelAdapter(OnChannelClick channelListener, OnFavoriteClick favoriteListener) {
        this.channelListener = channelListener;
        this.favoriteListener = favoriteListener;
        setHasStableIds(true);
    }
    public void replace(List<Channel> items) { channels.clear(); channels.addAll(items); notifyDataSetChanged(); }
    public void setFavorites(Set<String> urls) {
        favoriteUrls.clear();
        favoriteUrls.addAll(urls);
        notifyDataSetChanged();
    }
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_channel, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull Holder h, int position) {
        Channel c = channels.get(position);
        h.number.setText(String.format(java.util.Locale.getDefault(), "%03d", position + 1));
        h.name.setText(c.name); h.group.setText(c.group);
        h.itemView.setOnClickListener(v -> channelListener.onClick(c));
        boolean favorite = favoriteUrls.contains(c.url);
        h.favorite.setText(favorite ? R.string.favorite_on : R.string.favorite_off);
        h.favorite.setContentDescription(h.itemView.getContext().getString(
                favorite ? R.string.remove_favorite : R.string.add_favorite, c.name));
        h.favorite.setOnClickListener(v -> favoriteListener.onClick(c));
    }
    @Override public long getItemId(int position) { return channels.get(position).url.hashCode(); }
    @Override public int getItemCount() { return channels.size(); }
    static class Holder extends RecyclerView.ViewHolder {
        final TextView number, name, group;
        final Button favorite;
        Holder(View v) {
            super(v);
            number=v.findViewById(R.id.channel_number);
            name=v.findViewById(R.id.channel_name);
            group=v.findViewById(R.id.channel_group);
            favorite=v.findViewById(R.id.channel_favorite);
        }
    }
}
