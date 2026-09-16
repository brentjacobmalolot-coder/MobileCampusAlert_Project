package com.example.campusalert.database;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.campusalert.models.Report;
import com.example.campusalert.models.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main database class for CampusAlert.
 * Version 48: Added viewCount field to Report model.
 */
@Database(entities = {User.class, Report.class}, version = 48, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserDao userDao();
    public abstract ReportDao reportDao();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    Log.d("AppDatabase", "Initializing database campus_alert_db_v3");
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "campus_alert_db_v3")
                            .fallbackToDestructiveMigration()
                            .fallbackToDestructiveMigrationOnDowngrade()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
