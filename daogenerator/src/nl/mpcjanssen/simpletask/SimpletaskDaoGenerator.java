package nl.mpcjanssen.simpletask;

import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Property;
import de.greenrobot.daogenerator.Schema;
import de.greenrobot.daogenerator.ToMany;


/**
 * Generates entities and DAOs for the example project DaoExample.
 *
 * Run it as a Java application (not Android).
 *
 */


public class SimpletaskDaoGenerator {

    public static void main(String[] args) throws Exception {
        Schema schema = new Schema(1000, "nl.mpcjanssen.simpletask.dao");

        addEntities(schema);

        new DaoGenerator().generateAll(schema, "src/main/java");
    }

    private static void addEntities(Schema schema) {

        Entity list = schema.addEntity("EntryList");
        Property listProperty = list.addLongProperty("entryLine").notNull().getProperty();
        list.addStringProperty("text").notNull();

        Entity tag = schema.addEntity("EntryTag");
        Property tagProperty = tag.addLongProperty("entryLine").notNull().getProperty();
        tag.addStringProperty("text").notNull();


        Entity entry = schema.addEntity("Entry");
        entry.addLongProperty("line").notNull().primaryKey();
        entry.addBooleanProperty("hidden").notNull();
        entry.addStringProperty("text").notNull();
        entry.addBooleanProperty("completed").notNull();
        entry.addStringProperty("priority").notNull();
        entry.addStringProperty("completionDate");
        entry.addStringProperty("createDate");
        entry.addStringProperty("thresholdDate");
        entry.addStringProperty("dueDate");
        entry.addIntProperty("endOfCompPrefix").notNull();

        entry.addToMany(tag, tagProperty);
        entry.addToMany(list, listProperty);
    }

}