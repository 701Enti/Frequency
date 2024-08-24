package data.DeviceBle;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

public class DeviceBleDao {

    @Dao
    public interface BleDeviceMainDao{

        @Insert(onConflict = OnConflictStrategy.IGNORE)
        void insert(DeviceBleEntity.BleDeviceMainEntity bleDeviceMainEntity);

        @Update
        void update(DeviceBleEntity.BleDeviceMainEntity bleDeviceMainEntity);

        @Delete
        void delete(DeviceBleEntity.BleDeviceMainEntity bleDeviceMainEntity);

        @Query("DELETE FROM tablebledevicemain")
        void allDelete();

        @Query("SELECT * FROM tablebledevicemain")
        List<DeviceBleEntity.BleDeviceMainEntity> allGet();

        @Query("SELECT * FROM tablebledevicemain" +
                " WHERE bleDeviceId IN (SELECT bleDeviceId FROM tablebledevicemain WHERE bleDeviceName = :name)" +
                " AND bleDeviceSha256 = :sha256")
        DeviceBleEntity.BleDeviceMainEntity getByNameThenBleDeviceSha256(String name,String sha256);

        @Query("SELECT * FROM tablebledevicemain WHERE bleDeviceId = :id")
        DeviceBleEntity.BleDeviceMainEntity getByBleDeviceId(long id);

    }

}
