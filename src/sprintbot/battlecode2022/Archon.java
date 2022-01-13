package sprintbot.battlecode2022;

import battlecode.common.*;
import sprintbot.RunnableBot;
import sprintbot.battlecode2022.util.*;

public class Archon extends RunnableBot {


    // Command
    private CommandCommunicator.SpawnOrder last_order = null;


    // Build Strategy
    private final DefaultBuild default_strategy = new DefaultBuild();

    // Repair Strategy
    private final DefaultRepair repair_strategy = new DefaultRepair();


    public Archon(RobotController rc) throws GameActionException {
        super(rc);
    }

    @Override
    public void init() throws GameActionException {
        super.init();
        CommandCommunicator.archonIDShare();
    }

    @Override
    public void turn() throws GameActionException {

        CommandCommunicator.deadManSwitch();

        // Last turn's spawn
        if (last_order != null) {
            CommandCommunicator.spawnMessage(last_order);
            last_order = null;
        }

        RobotController controller = getRobotController();

        // Default build
        BuildStrategy current_build_strategy = default_strategy;
        RepairStrategy current_repair_strategy = repair_strategy;

        if (controller.isActionReady()) {
            if (current_build_strategy.build());
            else current_repair_strategy.repair();
        }
    }

    // Strategy

    interface RepairStrategy {
        boolean repair() throws GameActionException;
    }

    interface BuildStrategy {
        boolean build() throws GameActionException;
    }

    class DefaultBuild implements BuildStrategy {

        private int build_order = 1;

        @Override
        public boolean build() throws GameActionException {


            // Evenly distribute spawning
            int[] other_archons = CommandCommunicator.getArchonIDList();
            int archon_num = other_archons.length + 1;
            Integer ranking = null;
            for (int i = 0; i < other_archons.length; i ++) {
                if (other_archons[i] == getRobotController().getID()) {
                    archon_num --;
                    ranking = i;
                }
            }
            if (ranking == null) {
                ranking = 3;
            }
            if (getRobotController().getTeamLeadAmount(Cache.OUR_TEAM) < 150
                    && getRobotController().getRoundNum() % archon_num != ranking) {
                return false;
            }

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

    class DefaultRepair implements RepairStrategy {
        @Override
        public boolean repair() throws GameActionException {
            RobotController controller = getRobotController();
            RobotInfo[] potential = controller.senseNearbyRobots();
            for (RobotInfo robot : potential) {
                if (robot.getTeam() == Cache.OUR_TEAM && robot.getHealth() < robot.getType().health) {
                    if (controller.canRepair(robot.getLocation())) {
                        controller.repair(robot.getLocation());
                        return true;
                    }
                }
            }
            return false;
        }
    }

    // Util

    private boolean tryBuild(RobotType type, Direction dir, CommandCommunicator.RobotRole role, MapLocation loc) throws GameActionException {

        // TODO: Deal with edge case of adjacent archons

        if (getRobotController().canBuildRobot(type, dir)) {
            getRobotController().buildRobot(type, dir);
            last_order = new CommandCommunicator.SpawnOrder(
                    role,
                    loc);
            return true;
        }
        return false;
    }

    private boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        return tryBuild(type,dir,CommandCommunicator.type2Role(type),getRobotController().getLocation().add(dir));
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


