package com.fifaworldcup.service;

import com.fifaworldcup.database.DatabaseManager;
import com.fifaworldcup.model.Goal;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GoalService {
    private DatabaseManager dbManager;

    public GoalService() {
        this.dbManager = DatabaseManager.getInstance();
    }

    // This service is now simplified since goals are stored in Match.goalScorers ArrayList
    // No separate database table needed for goals
}
