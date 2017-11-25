package com.chattree.chattree;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;
import com.chattree.chattree.db.AppDatabase;
import com.chattree.chattree.db.User;
import com.chattree.chattree.db.UserDao;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
public class SimpleEntityReadWriteTest {
    private UserDao mUserDao;
    private AppDatabase mDb;

    @Before
    public void createDb() {
        Log.i("SimpleDbTest", "Creating the db...");
        Context context = InstrumentationRegistry.getTargetContext();
        mDb = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        mUserDao = mDb.userDao();
    }

    @After
    public void closeDb() throws IOException {
        mDb.close();
    }

    @Test
    public void writeUserAndReadInList() throws Exception {
        User user = new User();
        user.setId(3);
        user.setFirstname("george");
        user.setLastname("orwell");
        mUserDao.insertAll(user);
        User byName = mUserDao.findByName("george", "orwell");
        assertThat(byName,equalTo(user));

    }
}


