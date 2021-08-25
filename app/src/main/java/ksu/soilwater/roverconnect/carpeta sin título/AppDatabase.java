package ksu.soilwater.roverconnect.storage;

import androidx.room.*;

@Database(entities = {Measurement.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract LocalDataManager measurementData();
}
