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
        Schema schema = new Schema(1001, "nl.mpcjanssen.simpletask.dao");

        addEntities(schema);

        new DaoGenerator().generateAll(schema, "src/main/java");
    }

    private static void addEntities(Schema schema) {

        Entity entry = schema.addEntity("TodoEntry");
        entry.addLongProperty("line").notNull().primaryKey();
        entry.addStringProperty("text").notNull();


        Entity tag = schema.addEntity("TodoTags");
        tag.addStringProperty("tag").notNull().primaryKey();

        Entity list = schema.addEntity("TodoLists");
        list.addStringProperty("list").notNull().primaryKey();

        Entity status = schema.addEntity("TodoStatus");
        status.addStringProperty("key").notNull().primaryKey();
        status.addStringProperty("value");

    }

}