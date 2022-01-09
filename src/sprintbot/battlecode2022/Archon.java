package sprintbot.battlecode2022;

import battlecode.common.*;
import sprintbot.RunnableBot;
import sprintbot.battlecode2022.util.*;

public class Archon extends RunnableBot {


    //Build Strategy
    private BuildStrategy current_strategy;
    private final DefaultBuild default_strategy = new DefaultBuild();


    public Archon(RobotController rc) throws GameActionException {
        super(rc);
    }

    @Override
    public void init() throws GameActionException {
        super.init();
    }

    @Override
    public void turn() throws GameActionException {

        // Default build
        current_strategy = default_strategy;

        current_strategy.build();

    }

    // Strategy

    interface BuildStrategy {
        boolean build() throws GameActionException;
    }

    class DefaultBuild implements BuildStrategy {

        private int build_order = 1;

        @Override
        public boolean build() throws GameActionException {

            switch (build_order) {
                case 0:
                    if (tryBuild(RobotType.MINER)) {
                        build_order = (build_order + 1) % 2;
                        return true;
                    }
                    return false;
                case 1:
                    if (tryBuild(RobotType.SOLDIER)) {
                        build_order = (build_order + 1) % 2;
                        return true;
                    }
                    return false;
                default:
                    System.out.println("Default build order exception.");
                    build_order = 0;
                    return build();
            }
        }
    }

    // Util

    private boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (getRobotController().canBuildRobot(type, dir)) {
            getRobotController().buildRobot(type, dir);
            return true;
        }
        return false;
    }

    private boolean tryBuild(RobotType type) throws GameActionException {
        for (Direction dir : Constants.DIRECTIONS) {
            if (tryBuild(type, dir)) {
                return true;
            }
        }
        return false;
    }

}

