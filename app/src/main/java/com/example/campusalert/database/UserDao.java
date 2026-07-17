package com.example.campusalert.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.campusalert.models.User;

@Dao
public interface UserDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(User user);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User login(String email);

    @Query("SELECT * FROM users WHERE email = :email")
    LiveData<User> getUser(String email);

    @Query("SELECT COUNT(*) FROM users")
    int getUserCount();
}