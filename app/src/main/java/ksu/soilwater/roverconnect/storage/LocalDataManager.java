package ksu.soilwater.roverconnect.storage;

import androidx.room.*;

import java.util.List;

@Dao
public interface LocalDataManager {

        @Query("SELECT * FROM measurement")
        List<Measurement> getAll();

        @Query("SELECT * FROM measurement WHERE id LIKE :first LIMIT 1")
        Measurement findById(String first);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        public void insertAll(Measurement... measurements);

        @Delete
        void delete(Measurement measurement);

        @Query("DELETE FROM measurement")
        public void clearTable();



}
