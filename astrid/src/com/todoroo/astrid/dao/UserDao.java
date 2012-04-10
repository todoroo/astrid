package com.todoroo.astrid.dao;

import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.data.User;

public class UserDao extends DatabaseDao<User> {
    @Autowired Database database;

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value="UR_UNINIT_READ")
    public UserDao() {
        super(User.class);
        DependencyInjectionService.getInstance().inject(this);
        setDatabase(database);
    }
}
