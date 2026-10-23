package com.example.myapplication.adapter;
import android.content.Intent;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.ProfileActivity;
import com.example.myapplication.model.User;
import java.util.*;

public class FollowUserAdapter extends RecyclerView.Adapter<FollowUserAdapter.Holder> {
 private final List<User> users = new ArrayList<>();
 public void setUsers(List<User> items) { users.clear(); users.addAll(items); notifyDataSetChanged(); }
 public int getItemCount() { return users.size(); }
 @NonNull public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) { return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_follow_user,parent,false)); }
 public void onBindViewHolder(@NonNull Holder holder,int position) {
  User user=users.get(position);
  String name=user.getFullName()==null||user.getFullName().trim().isEmpty()?user.getUsername():user.getFullName();
  holder.name.setText(name); holder.bio.setText(user.getBio());
  holder.bio.setVisibility(user.getBio()==null||user.getBio().isEmpty()?View.GONE:View.VISIBLE);
  Glide.with(holder.itemView).load(user.getAvatarUrl()).placeholder(R.drawable.bg_circle_avatar).error(R.drawable.bg_circle_avatar).circleCrop().into(holder.avatar);
  holder.itemView.setOnClickListener(new View.OnClickListener() {
   @Override public void onClick(View v) {
    Intent intent = new Intent(v.getContext(), ProfileActivity.class);
    intent.putExtra("userId", user.getId());
    v.getContext().startActivity(intent);
   }
  });
 }
 static class Holder extends RecyclerView.ViewHolder {
  final TextView name,bio; final ImageView avatar;
  Holder(View view) { super(view);name=view.findViewById(R.id.tvFollowUserName);bio=view.findViewById(R.id.tvFollowUserBio);avatar=view.findViewById(R.id.ivFollowUserAvatar); }
 }
}