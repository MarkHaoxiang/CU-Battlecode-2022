package sprintbot;

import battlecode.common.*;

// Init util models applicable to all bots here!

public abstract class RunnableBot {

    public static RobotController controller;

    public RunnableBot(RobotController rc) throws GameActionException {
        controller = rc;
        this.init();
    }

    public void init() throws GameActionException {

    }

    public abstract void turn() throws GameActionException;
}