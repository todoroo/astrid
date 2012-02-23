package com.todoroo.astrid.test;

import java.io.File;

import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.provider.Astrid3ContentProvider;
import com.todoroo.astrid.service.AstridDependencyInjector;

/**
 * Test case that automatically sets up and tears down a test database
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class DatabaseTestCase extends TodorooTestCaseWithInjector {

    static {
        AstridDependencyInjector.initialize();
    }

    public static Database database = new TestDatabase();

    @Override
    protected void addInjectables() {
        testInjector.addInjectable("database", database);
    }

	@Override
	protected void setUp() throws Exception {
	    // call upstream setup, which invokes dependency injector
	    super.setUp();

		// empty out test databases
	    database.clear();
		database.openForWriting();

		Astrid3ContentProvider.setDatabaseOverride(database);
	}

	/**
	 * Helper to delete a database by name
	 * @param database
	 */
	protected void deleteDatabase(String database) {
	    File db = getContext().getDatabasePath(database);
	    if(db.exists())
	        db.delete();
    }

    @Override
	protected void tearDown() throws Exception {
		database.close();
		super.tearDown();
	}

	public static class TestDatabase extends Database {
        @Override
	    public String getName() {
	        return "databasetest";
	    }
	}

}
