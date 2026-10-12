package com.rizal.catatemas.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ItemRepository(private val store: JsonStore<ItemStore>) {

    private val _items = MutableStateFlow(store.read()?.items ?: emptyList())
    val items: StateFlow<List<GoldItem>> = _items.asStateFlow()

    fun find(id: String?): GoldItem? = id?.let { key -> _items.value.firstOrNull { it.id == key } }

    fun upsert(item: GoldItem) {
        val current = _items.value.toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index >= 0) current[index] = item else current.add(item)
        persist(current.sortedWith(compareByDescending<GoldItem> { it.purchaseDate }.thenByDescending { it.createdAt }))
    }

    fun delete(id: String) {
        val removed = _items.value.firstOrNull { it.id == id }
        if (removed == null) {
            persist(_items.value)
            return
        }
        persist(_items.value.filterNot { it.id == id })
    }

    /** ganti seluruh data (dipakai saat restore backup) */
    fun replaceAll(list: List<GoldItem>) {
        persist(list.sortedWith(compareByDescending<GoldItem> { it.purchaseDate }.thenByDescending { it.createdAt }))
    }

    private fun persist(list: List<GoldItem>) {
        _items.value = list
        store.write(ItemStore(list))
    }
}
