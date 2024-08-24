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

        @PrimaryKey(autoGenerate = false)
        private String bleDeviceSha256;//设备的SHA-256唯一性与安全校验码

        //配对密码,风险识别等请查看其他相关实体定义

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

        public String getBleDeviceSha256() {
            return bleDeviceSha256;
        }

        public void setBleDeviceId(long bleDeviceId) {
            this.bleDeviceId = bleDeviceId;
        }

        public void setBleDeviceIconId(int bleDeviceIconId) {
            this.bleDeviceIconId = bleDeviceIconId;
        }

        public void setLastActiveTimestamp(long lastActiveTimestamp) {
            this.lastActiveTimestamp = lastActiveTimestamp;
        }

        public void setBleDeviceName(String bleDeviceName) {
            this.bleDeviceName = bleDeviceName;
        }

        public void setBleDeviceSha256(String bleDeviceSha256) {
            this.bleDeviceSha256 = bleDeviceSha256;
        }
    }




}
