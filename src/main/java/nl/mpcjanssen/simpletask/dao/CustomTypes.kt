package nl.mpcjanssen.simpletask.dao

import de.greenrobot.dao.Property
import de.greenrobot.dao.converter.PropertyConverter
import kotlin.collections.emptyList
import kotlin.collections.joinToString
import kotlin.collections.toSortedSet
import kotlin.text.split

// Covert lists and tags to database values
class Lists (items : List<String>) {
    val sorted = items.toSortedSet();
}
class Tags (items : List<String>) {
    val sorted = items.toSortedSet();
}


public class ListsPropertyConverter () : PropertyConverter<Lists ,String> {
    override fun convertToEntityProperty(databaseValue: String?): Lists? {
        return Lists(databaseValue?.split(" ") ?: emptyList());
    }

    override fun convertToDatabaseValue(entityProperty: Lists): String? {
        return entityProperty?.sorted.joinToString (" ")
    }
}

public class TagsPropertyConverter () : PropertyConverter<Tags ,String> {
    override fun convertToEntityProperty(databaseValue: String?): Tags? {
        return Tags(databaseValue?.split(" ")?: emptyList());
    }
    override fun convertToDatabaseValue(entityProperty: Tags): String? {
        return entityProperty?.sorted.joinToString (" ")
    }
}



