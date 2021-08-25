package ksu.soilwater.roverconnect.storage;

import androidx.room.*;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;


@Dao
public interface LocalDataManager {


        @Query("SELECT * FROM measurement WHERE id LIKE :first LIMIT 1")
        Measurement findById(String first);

        @Insert()
        Completable insert(Measurement measurement);

        @Query("DELETE FROM measurement")
        Completable clearTable();

        @Query("SELECT * FROM measurement")
        Single<List<Measurement>> getAll();




}
