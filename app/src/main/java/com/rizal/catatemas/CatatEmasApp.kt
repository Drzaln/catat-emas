package com.rizal.catatemas

import android.app.Application
import com.rizal.catatemas.data.ItemRepository
import com.rizal.catatemas.data.ItemStore
import com.rizal.catatemas.data.JsonStore
import com.rizal.catatemas.data.PhotoStore
import com.rizal.catatemas.price.GoldApi
import com.rizal.catatemas.price.PriceRepository
import com.rizal.catatemas.price.PriceStore
import com.rizal.catatemas.security.SecurityManager
import com.rizal.catatemas.update.UpdateState
import com.rizal.catatemas.update.Updater
import java.io.File

class AppContainer(application: Application) {
    val itemRepository = ItemRepository(
        JsonStore(File(application.filesDir, "items.json"), ItemStore.serializer()),
    )
    val priceRepository = PriceRepository(
        api = GoldApi(),
        store = JsonStore(File(application.filesDir, "prices.json"), PriceStore.serializer()),
    )
    val photoStore = PhotoStore(application)
    val securityManager = SecurityManager(application)
    val updater = Updater(application)
}

class CatatEmasApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
