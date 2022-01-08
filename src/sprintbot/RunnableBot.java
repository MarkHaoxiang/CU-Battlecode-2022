package sprintbot;

import battlecode.common.*;
import sprintbot.battlecode2022.*;
import sprintbot.battlecode2022.util.*;
import sprintbot.battlecode2022.util.navigation.BugNavigator;

public abstract class RunnableBot {

    private final RobotController controller;
    protected Navigator navigator;
    //private Communicator communicator;

    // Constructor

    public RunnableBot(RobotController controller) throws GameActionException {
        this.controller = controller;
        this.navigator = new BugNavigator(controller);
        this.init();
    }

    // Methods
    public RobotController getRobotController() {
        return controller;
    }

    // To Implement

    public void init() throws GameActionException {
    }

    public abstract void turn() throws GameActionException;


}