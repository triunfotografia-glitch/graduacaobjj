package br.com.dojolb.graduacao

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import br.com.dojolb.graduacao.data.database.AppDatabase
import br.com.dojolb.graduacao.data.repository.GraduacaoRepository

class DojoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize database
        AppDatabase.getDatabase(this)
    }

    val repository by lazy {
        val db = AppDatabase.getDatabase(this)
        GraduacaoRepository(db.studentDao(), db.positionDao())
    }
}