package br.com.dojolb.graduacao

import android.app.backup.BackupAgentHelper
import android.app.backup.FileBackupHelper
import android.content.Context

class BackupAgent : BackupAgentHelper() {
    override fun onCreate() {
        super.onCreate()
        // Database file backup
        val helper = FileBackupHelper(this, "dojo_graduacao.db", "dojo_graduacao.db-shm", "dojo_graduacao.db-wal")
        addHelper("database", helper)
    }
}