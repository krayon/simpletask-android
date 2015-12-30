package nl.mpcjanssen.simpletask.dao


data class Daos(
        val entryDao: EntryDao,
        val listDao: EntryListDao,
        val tagDao: EntryTagDao,
        val visibleLineDao: VisibleLineDao
)
