package com.dhh.quizbyai.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dhh.quizbyai.R;
import com.dhh.quizbyai.models.PlayerModel;

import java.util.List;

public class WaitingRoomAdapter extends RecyclerView.Adapter<WaitingRoomAdapter.PlayerViewHolder> {

    private List<PlayerModel> playerList;

    public WaitingRoomAdapter(List<PlayerModel> playerList) {
        this.playerList = playerList;
    }

    @NonNull
    @Override
    public PlayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_player_waiting, parent, false);
        return new PlayerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlayerViewHolder holder, int position) {
        PlayerModel player = playerList.get(position);

        holder.txtIndex.setText(String.valueOf(position + 1)); // Số thứ tự
        holder.txtName.setText(player.getName());             // Tên người chơi
        holder.txtScore.setText(player.getScore() + " pts");   // Điểm số (ở phòng chờ thì thường là 0)
    }

    @Override
    public int getItemCount() {
        return playerList != null ? playerList.size() : 0;
    }

    public static class PlayerViewHolder extends RecyclerView.ViewHolder {
        TextView txtIndex, txtName, txtScore;

        public PlayerViewHolder(@NonNull View itemView) {
            super(itemView);
            // Bạn nhớ check lại id trong item_player_waiting.xml xem có khớp không nhé
            txtIndex = itemView.findViewById(R.id.txt_player_index);
            txtName = itemView.findViewById(R.id.txt_player_name);
            txtScore = itemView.findViewById(R.id.txt_player_score);
        }
    }
}