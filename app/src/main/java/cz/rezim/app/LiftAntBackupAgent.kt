package cz.rezim.app

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FullBackupDataOutput
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.IOException

// The system stops the app before full backup (backupInForeground=false).
// Checkpoint the exact database without instantiating LocalStore or running a migration.
internal fun checkpointForBackup(file:File) {
    if(!file.exists()) return
    SQLiteDatabase.openDatabase(file.path,null,SQLiteDatabase.OPEN_READWRITE).use {db ->
        db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)",null).use {cursor ->
            if(cursor.moveToFirst() && cursor.getInt(0)!=0) throw IOException("Database busy; postpone backup")
        }
    }
}
class LiftAntBackupAgent: BackupAgent() {
    override fun onFullBackup(data:FullBackupDataOutput) {
        // API 26/27 cannot enforce client-side encryption: keep manual JSON available there.
        if(Build.VERSION.SDK_INT<28) return
        if(Build.VERSION.SDK_INT<31) {
            val allowed=BackupAgent.FLAG_CLIENT_SIDE_ENCRYPTION_ENABLED or BackupAgent.FLAG_DEVICE_TO_DEVICE_TRANSFER
            if(data.transportFlags and allowed == 0) return
        }
        checkpointForBackup(getDatabasePath("rezim_local_v1.db"))
        super.onFullBackup(data)
    }
    // This app uses full backup, never key/value backup.
    override fun onBackup(oldState:ParcelFileDescriptor?,data:BackupDataOutput?,newState:ParcelFileDescriptor?) = Unit
    override fun onRestore(data:BackupDataInput?,appVersionCode:Int,newState:ParcelFileDescriptor?) = Unit
}
