package com.dhh.quizbyai.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.dhh.quizbyai.R;
import com.dhh.quizbyai.models.PlayerModel;
import java.util.List;

public class FinalLeaderboardAdapter extends RecyclerView.Adapter<FinalLeaderboardAdapter.ViewHolder> {

    private List<PlayerModel> playerList;
    private int totalQuestions;
    private String currentUid;

    public FinalLeaderboardAdapter(List<PlayerModel> playerList, int totalQuestions, String currentUid) {
        this.playerList = playerList;
        this.totalQuestions = totalQuestions;
        this.currentUid = currentUid;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // TÁI SỬ DỤNG GIAO DIỆN CỦA LIVE LEADERBOARD
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_live_leaderboard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlayerModel player = playerList.get(position);

        holder.txtRank.setText(String.valueOf(position + 1));
        holder.txtName.setText(player.getName());
        holder.txtScore.setText(player.getScore() + "%"); // Đổi thành %
        holder.txtProgress.setText(player.getAnswered() + "/" + totalQuestions);

        // NẾU UID CỦA DÒNG NÀY TRÙNG VỚI UID CỦA MÌNH -> HIGHLIGHT LÊN
        if (player.getUid() != null && player.getUid().equals(currentUid)) {
            holder.itemView.setBackgroundColor(Color.parseColor("#33FF9800")); // Nền cam nhạt
            holder.txtName.setTextColor(Color.parseColor("#FF9800")); // Tên màu cam
            holder.txtRank.setTextColor(Color.parseColor("#FF9800"));
            holder.txtName.setText(player.getName() + " (You)");
        } else {
            // Trả về mặc định cho các người chơi khác
            holder.itemView.setBackgroundResource(R.drawable.bg_rounded_card);
            holder.txtName.setTextColor(Color.WHITE);
            holder.txtRank.setTextColor(Color.parseColor("#FF9800")); // Rank mặc định màu cam
        }
    }

    @Override
    public int getItemCount() { return playerList.size(); }

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