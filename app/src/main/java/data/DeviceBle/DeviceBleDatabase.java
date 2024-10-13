package data.DeviceBle;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

public class DeviceBleDatabase {

    @Database(entities = {DeviceBleEntity.BleDeviceMainEntity.class},version = 1,exportSchema = true)
    public abstract static class BleDeviceMainDatabase extends RoomDatabase {

        //单例模式

        //数据库单例
        private static volatile BleDeviceMainDatabase uniqueDatabaseInstance;

        //获取数据库,如果数据库引用不存在于 内存 即单例为null,在本地存储创建一个数据库或者从本地存储读取之前创建的数据库以获得引用保存在单例
        //所以并不是说单例这个引用为空,数据库就是不存在的,因为数据库会存储在硬盘中,为空可能是我们还没有读取它以获得引用,但当然也可能它根本没有创建在硬盘,那就确实没有数据
        public static BleDeviceMainDatabase getDatabase(Context context){
            if(uniqueDatabaseInstance == null){
                synchronized(BleDeviceMainDatabase.class){
                    if (uniqueDatabaseInstance == null){
                        uniqueDatabaseInstance =
                                Room.databaseBuilder(context.getApplicationContext(),BleDeviceMainDatabase.class,"databasebledevicemain").build();
                    }
                }

                return uniqueDatabaseInstance;//返回 之前不存在但是现在创建好的数据库 的引用 或者 之前持久化存储在硬盘的数据库 的引用,也可能还是返回空,如果遇到问题
            }
            else {
                return uniqueDatabaseInstance;//返回已经实例化的单例,不需要准备操作,因为已经获得引用
            }
        }

        //声明Dao
        public abstract DeviceBleDao.BleDeviceMainDao bleDeviceMainDao();

    }

}
