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

        RobotController controller = getRobotController();
        MapLocation my_location = controller.getLocation();
        RobotInfo[] robots = controller.senseNearbyRobots(RobotType.WATCHTOWER.actionRadiusSquared, Cache.OPPONENT_TEAM);

        if (!controller.isActionReady()) {
            return;
        }

        RobotInfo best = null;
        MapLocation best_attack = null;
        double best_score = -9999;
        for (RobotInfo robot : robots) {
            double score = 0;
            switch (robot.getType()) {
                case SOLDIER:
                case SAGE:
                case WATCHTOWER:
                    score += 6 -robot.getHealth()/robot.getType().getMaxHealth(robot.getLevel());
                    if (robot.getHealth() <= controller.getType().getDamage(controller.getLevel())) {
                        score += 4 ; // Kill bonus
                    }
                    break;
                case ARCHON:
                case LABORATORY:
                    score += 4;
                case BUILDER:
                    score += 4;
                    break;
                case MINER:
                    score += 4;
                    break;
            }

            if (score > best_score) {
                best_score = score;
                best_attack = robot.getLocation();
                best = robot;
            }
        }


        if (best_attack != null && controller.canAttack(best_attack)) {
            controller.attack(best_attack);
        }
    }
}
