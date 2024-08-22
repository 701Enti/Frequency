package data.DeviceBle;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

public class DeviceBleEntity {

    @Entity(tableName = "tablebledevicemain")
    public static class BleDeviceMainEntity {

        @PrimaryKey(autoGenerate = true)
        private long bleDeviceId;//自增设备ID

        private int bleDeviceIconId;//图标ID,与BleFragment中BluetoothDeviceModel的IconId定义一致

        private long lastActiveTimestamp;//最近活跃时间戳

        private String bleDeviceName;//扫描设备时得到的设备名

        //配对密码与唯一确定,安全检测等其他请查看相关其他实体定义

        public BleDeviceMainEntity(){

        }

        public long getBleDeviceId() {
            return bleDeviceId;
        }

        public int getBleDeviceIconId() {
            return bleDeviceIconId;
        }

        public long getLastActiveTimestamp() {
            return lastActiveTimestamp;
        }

        public String getBleDeviceName() {
            return bleDeviceName;
        }



        public void setBleDeviceId(long bleDeviceId) {
            this.bleDeviceId = bleDeviceId;
        }

        public void setBleDeviceIconId(int bleDeviceIconId) {
            this.bleDeviceIconId = bleDeviceIconId;
        }

        public void setBleDeviceName(String bleDeviceName) {
            this.bleDeviceName = bleDeviceName;
        }

        public void setLastActiveTimestamp(long lastActiveTimestamp) {
            this.lastActiveTimestamp = lastActiveTimestamp;
        }
    }




}
