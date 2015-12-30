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

        Entity entry = schema.addEntity("Entry");
        entry.addLongProperty("line").notNull().primaryKey();
        entry.addBooleanProperty("selected").notNull();
        entry.addBooleanProperty("hidden").notNull();
        entry.addStringProperty("text").notNull();
        entry.addBooleanProperty("completed").notNull();
        entry.addStringProperty("priority").notNull();
        entry.addStringProperty("completionDate");
        entry.addStringProperty("createDate");
        entry.addStringProperty("thresholdDate");
        entry.addStringProperty("dueDate");
        entry.addIntProperty("endOfCompPrefix").notNull();
        entry.addStringProperty("lists").customType(
                "nl.mpcjanssen.simpletask.dao.Lists",
                "nl.mpcjanssen.simpletask.dao.ListsPropertyConverter");
        entry.addStringProperty("tags").customType(
                "nl.mpcjanssen.simpletask.dao.Tags",
                "nl.mpcjanssen.simpletask.dao.TagsPropertyConverter");


        Entity visibleLine = schema.addEntity("VisibleLine");
        visibleLine.addLongProperty("position").notNull().primaryKey();
        visibleLine.addBooleanProperty("isHeader").notNull();
        Property taskLine = visibleLine.addLongProperty("taskLine").getProperty();
        visibleLine.addToOne(entry, taskLine);
        visibleLine.addStringProperty("header");
        visibleLine.addLongProperty("count");
    }

}