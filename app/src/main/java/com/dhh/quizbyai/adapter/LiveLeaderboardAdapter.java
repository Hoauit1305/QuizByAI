package com.dhh.quizbyai.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.dhh.quizbyai.R;
import com.dhh.quizbyai.models.PlayerModel;
import java.util.List;

public class LiveLeaderboardAdapter extends RecyclerView.Adapter<LiveLeaderboardAdapter.ViewHolder> {
    private List<PlayerModel> playerList;
    private int totalQuestions;

    public LiveLeaderboardAdapter(List<PlayerModel> playerList, int totalQuestions) {
        this.playerList = playerList;
        this.totalQuestions = totalQuestions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_live_leaderboard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlayerModel player = playerList.get(position);

        holder.txtRank.setText(String.valueOf(position + 1));
        holder.txtName.setText(player.getName());
        holder.txtScore.setText(player.getScore() + " pts");
        // Hiển thị tiến độ kiểu "2/10"
        holder.txtProgress.setText(player.getAnswered() + "/" + totalQuestions);
    }

    @Override
    public int getItemCount() {
        return playerList != null ? playerList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtRank, txtName, txtProgress, txtScore;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtRank = itemView.findViewById(R.id.txt_rank);
            txtName = itemView.findViewById(R.id.txt_lb_name);
            txtProgress = itemView.findViewById(R.id.txt_lb_progress);
            txtScore = itemView.findViewById(R.id.txt_lb_score);
        }
    }
}