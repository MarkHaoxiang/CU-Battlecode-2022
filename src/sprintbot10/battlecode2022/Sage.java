package sprintbot10.battlecode2022;

import battlecode.common.*;
import sprintbot10.RunnableBot;
import sprintbot10.battlecode2022.util.*;

public class Sage extends RunnableBot {

    public Sage(RobotController rc) throws GameActionException {
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
