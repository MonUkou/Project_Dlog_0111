package com.example.myapplication;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.myapplication.adapter.FollowUserAdapter;
import com.example.myapplication.model.User;
import com.example.myapplication.service.impl.UserService;
import java.util.List;

public class FollowListActivity extends AppCompatActivity {
 private String userId; private boolean following; private FollowUserAdapter adapter; private int requestVersion;
 @Override protected void onCreate(Bundle state) {
  super.onCreate(state);setContentView(R.layout.activity_follow_list);
  userId=getIntent().getStringExtra("userId");following=getIntent().getBooleanExtra("following",false);
  ((TextView)findViewById(R.id.tvFollowListTitle)).setText(following?"Đang theo dõi":"Người theo dõi");
  findViewById(R.id.btnBackFollowList).setOnClickListener(new View.OnClickListener() {
   @Override public void onClick(View v) { finish(); }
  });
  findViewById(R.id.btnRetryFollowList).setOnClickListener(new View.OnClickListener() {
   @Override public void onClick(View v) { load(); }
  });
  RecyclerView list=findViewById(R.id.rvFollowUsers);list.setLayoutManager(new LinearLayoutManager(this));
  adapter=new FollowUserAdapter();list.setAdapter(adapter);
 }
 @Override protected void onResume() {super.onResume();load();}
 private void load() {
  final int version=++requestVersion;
  findViewById(R.id.progressFollowList).setVisibility(View.VISIBLE);findViewById(R.id.llFollowListState).setVisibility(View.GONE);
  UserService.getInstance().getFollowUsers(userId,following,new UserService.Callback<List<User>>() {
   public void onSuccess(List<User> users) {
    if(isDestroyed()||version!=requestVersion)return;
    findViewById(R.id.progressFollowList).setVisibility(View.GONE);adapter.setUsers(users);
    if(users.isEmpty())showState(following?"Chưa theo dõi ai.":"Chưa có người theo dõi.",false);
   }
   public void onError(Exception error) {
    if(isDestroyed()||version!=requestVersion)return;
    findViewById(R.id.progressFollowList).setVisibility(View.GONE);
    showState("Không tải được danh sách. Kiểm tra kết nối rồi thử lại.",true);
   }
  });
 }
 private void showState(String text,boolean retry) {
  findViewById(R.id.llFollowListState).setVisibility(View.VISIBLE);
  ((TextView)findViewById(R.id.tvFollowListState)).setText(text);
  findViewById(R.id.btnRetryFollowList).setVisibility(retry?View.VISIBLE:View.GONE);
 }
}