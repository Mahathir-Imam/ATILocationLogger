package com.example.atilocationlogger.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LocationLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(LocationLog log);

    @Query("SELECT * FROM location_logs ORDER BY timestamp DESC")
    List<LocationLog> getAll();

    @Query("SELECT * FROM location_logs ORDER BY timestamp DESC LIMIT 10")
    List<LocationLog> getLatest10();
}
