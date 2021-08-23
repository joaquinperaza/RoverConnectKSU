package ksu.soilwater.roverconnect.storage;

import androidx.annotation.NonNull;
import androidx.room.*;

@Entity
public class Measurement {
    @PrimaryKey
    @NonNull
    public String time;

    @ColumnInfo(name = "ncTotal")
    public String ncTotal;

    @ColumnInfo(name = "stdDev")
    public String stdDev;

    @ColumnInfo(name = "id")
    public String id;

    @ColumnInfo(name = "lat")
    public double lat;

    @ColumnInfo(name = "lng")
    public double lng;

}
