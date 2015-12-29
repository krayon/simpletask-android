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

        addEntry(schema);

        new DaoGenerator().generateAll(schema, "src/main/java");
    }

    private static void addEntry(Schema schema) {
        Entity entry = schema.addEntity("Entry");
        entry.addLongProperty("line").notNull();
        entry.addStringProperty("text").notNull();
    }

}