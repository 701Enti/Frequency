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

        @Query("DELETE FROM table_ble_device_main_default")
        void allDelete();

        @Query("SELECT * FROM table_ble_device_main_default")
        List<DeviceBleEntity.BleDeviceMainEntity> allGet();

        @Query("SELECT * FROM table_ble_device_main_default" +
                " WHERE bleDeviceId IN (SELECT bleDeviceId FROM table_ble_device_main_default WHERE bleDeviceName = :name)" +
                " AND bleDeviceSha256 = :sha256")
        DeviceBleEntity.BleDeviceMainEntity getByNameThenBleDeviceSha256(String name,String sha256);

        @Query("SELECT * FROM table_ble_device_main_default WHERE bleDeviceId = :id")
        DeviceBleEntity.BleDeviceMainEntity getByBleDeviceId(long id);

    }

}
