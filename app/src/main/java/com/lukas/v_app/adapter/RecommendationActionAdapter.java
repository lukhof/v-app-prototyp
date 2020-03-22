package com.lukas.v_app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.lukas.v_app.R;
import com.lukas.v_app.data.RecommendationAction;
import java.util.ArrayList;
import java.util.List;

public class RecommendationActionAdapter extends RecyclerView.Adapter<RecommendationActionAdapter.ViewHolder> {

    private List<RecommendationAction> items = new ArrayList<>();

    public void updateItems(List<RecommendationAction> actions){
        items = actions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recommendation_action, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final RecommendationAction recommendationAction = items.get(position);
        holder.bind(recommendationAction);

    }

    @Override
    public int getItemCount() {
        return items.size();
    }


    static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView textView;

        ViewHolder(@NonNull final View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.item_recommended_action_text);
        }

        void bind(RecommendationAction recommendationAction){
            textView.setText(recommendationAction.getMessage());
        }
    }
}
