package com.example.hotspotmanager;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class crud_operations extends AppCompatActivity {

    private RecyclerView recyclerViewUsers;
    private UserAdapter userAdapter;
    private List<User> userList;
    private DatabaseReference databaseReference;
    private Button buttonAddUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crud_operations);

        recyclerViewUsers = findViewById(R.id.recyclerViewUsers);
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(this));
        userList = new ArrayList<>();
        userAdapter = new UserAdapter(userList);
        recyclerViewUsers.setAdapter(userAdapter);

        databaseReference = FirebaseDatabase.getInstance().getReference("Users");

        buttonAddUser = findViewById(R.id.buttonAddUser);
        buttonAddUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addUser();
            }
        });

        userAdapter.setOnItemClickListener(new UserAdapter.OnItemClickListener() {
            @Override
            public void onEditClick(int position) {
                editUser(position);
            }

            @Override
            public void onDeleteClick(int position) {
                deleteUser(position);
            }
        });

        fetchUsers();
    }

    private void fetchUsers() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userList.clear();
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    User user = userSnapshot.getValue(User.class);
                    user.setUserId(userSnapshot.getKey());
                    userList.add(user);
                }
                userAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(crud_operations.this, "Failed to load users.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addUser() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add User");

        View viewInflated = getLayoutInflater().inflate(R.layout.dialog_add_user, null);
        builder.setView(viewInflated);

        final EditText inputName = viewInflated.findViewById(R.id.inputName);
        final EditText inputEmail = viewInflated.findViewById(R.id.inputEmail);
        final EditText inputSapId = viewInflated.findViewById(R.id.inputSapId);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                String name = inputName.getText().toString();
                String email = inputEmail.getText().toString();
                String sapId = inputSapId.getText().toString();

                if (name.isEmpty() || email.isEmpty() || sapId.isEmpty()) {
                    Toast.makeText(crud_operations.this, "All fields are required.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String userId = databaseReference.push().getKey();
                User user = new User(userId, name, email, sapId);
                databaseReference.child(userId).setValue(user);
                Toast.makeText(crud_operations.this, "User added.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void editUser(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit User");

        View viewInflated = getLayoutInflater().inflate(R.layout.dialog_add_user, null);
        builder.setView(viewInflated);

        final EditText inputName = viewInflated.findViewById(R.id.inputName);
        final EditText inputEmail = viewInflated.findViewById(R.id.inputEmail);
        final EditText inputSapId = viewInflated.findViewById(R.id.inputSapId);

        final User user = userList.get(position);
        inputName.setText(user.getName());
        inputEmail.setText(user.getEmail());
        inputSapId.setText(user.getSapId());

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                String name = inputName.getText().toString();
                String email = inputEmail.getText().toString();
                String sapId = inputSapId.getText().toString();

                if (name.isEmpty() || email.isEmpty() || sapId.isEmpty()) {
                    Toast.makeText(crud_operations.this, "All fields are required.", Toast.LENGTH_SHORT).show();
                    return;
                }

                user.setName(name);
                user.setEmail(email);
                user.setSapId(sapId);

                databaseReference.child(user.getUserId()).setValue(user);
                Toast.makeText(crud_operations.this, "User updated.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void deleteUser(final int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete this user?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        User user = userList.get(position);
                        databaseReference.child(user.getUserId()).removeValue();
                        Toast.makeText(crud_operations.this, "User deleted.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    // Inner class for the RecyclerView adapter
    public static class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

        private List<User> userList;
        private OnItemClickListener listener;

        public interface OnItemClickListener {
            void onEditClick(int position);
            void onDeleteClick(int position);
        }

        public void setOnItemClickListener(OnItemClickListener listener) {
            this.listener = listener;
        }

        public UserAdapter(List<User> userList) {
            this.userList = userList;
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_item, parent, false);
            return new UserViewHolder(view, listener);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            User user = userList.get(position);
            holder.textViewName.setText(user.getName());
            holder.textViewEmail.setText(user.getEmail());
            holder.textViewSapId.setText(user.getSapId());
        }

        @Override
        public int getItemCount() {
            return userList.size();
        }

        public static class UserViewHolder extends RecyclerView.ViewHolder {
            public TextView textViewName;
            public TextView textViewEmail;
            public TextView textViewSapId;
            public ImageButton buttonEdit;
            public ImageButton buttonDelete;

            public UserViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
                super(itemView);
                textViewName = itemView.findViewById(R.id.textViewName);
                textViewEmail = itemView.findViewById(R.id.textViewEmail);
                textViewSapId = itemView.findViewById(R.id.textViewSapId);
                buttonEdit = itemView.findViewById(R.id.buttonEdit);
                buttonDelete = itemView.findViewById(R.id.buttonDelete);

                buttonEdit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (listener != null) {
                            int position = getAdapterPosition();
                            if (position != RecyclerView.NO_POSITION) {
                                listener.onEditClick(position);
                            }
                        }
                    }
                });

                buttonDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (listener != null) {
                            int position = getAdapterPosition();
                            if (position != RecyclerView.NO_POSITION) {
                                listener.onDeleteClick(position);
                            }
                        }
                    }
                });
            }
        }
    }

    // User data model class
    public static class User {
        private String userId;
        private String name;
        private String email;
        private String sapId;

        public User() {
            // Default constructor required for calls to DataSnapshot.getValue(User.class)
        }

        public User(String userId, String name, String email, String sapId) {
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.sapId = sapId;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getSapId() {
            return sapId;
        }

        public void setSapId(String sapId) {
            this.sapId = sapId;
        }
    }
}

