package watchtowerrush.battlecode2022;

import battlecode.common.*;
import watchtowerrush.RunnableBot;
import watchtowerrush.battlecode2022.util.*;

public class Laboratory extends RunnableBot {

    public Laboratory(RobotController rc) throws GameActionException {
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
