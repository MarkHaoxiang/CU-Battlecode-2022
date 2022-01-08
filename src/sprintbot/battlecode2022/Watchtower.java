package sprintbot.battlecode2022;

import battlecode.common.*;
import sprintbot.RunnableBot;
import sprintbot.battlecode2022.util.*;

public class Watchtower extends RunnableBot {

    public Watchtower(RobotController rc) throws GameActionException {
        super(rc);
    }


    @Override
    public void init() throws GameActionException {
        super.init();
    }

    @Override
    public void turn() throws GameActionException {

        Cache.update();

    }
}
