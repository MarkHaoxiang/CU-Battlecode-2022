package sprintbot13.battlecode2022.util.navigation;

import sprintbot13.battlecode2022.util.Cache;
import sprintbot13.battlecode2022.util.Constants;
import sprintbot13.battlecode2022.util.Navigator;
import battlecode.common.*;

public class BugNavigator extends Navigator {


    private boolean is_bugging = false;
    private boolean clockwise = true;
    private int count = 0;
    private MapLocation prev_target;
    private MapLocation start_bug_location;
    private MapLocation obstacle;
    private double angle;
    private int stuck_turns;

    private final GreedyNavigator greedy;
    private final NaiveNavigator naive;

    public BugNavigator(RobotController controller) {
        super(controller);
        greedy = new GreedyNavigator(controller);
        naive = new NaiveNavigator(controller);
    }

    // Bug Pathing
    public MoveResult move(MapLocation target_location)
            throws GameActionException {

        MapLocation current_location = controller.getLocation();


        /*
        // Is target out of the map
        if (target_location == null ||
                !controller.onTheMap(current_location.add(
                        current_location.directionTo(
                                target_location)))) return MoveResult.IMPOSSIBLE;

        // At destination
        if (target_location.equals(current_location)) {
            is_bugging = false;
            return MoveResult.REACHED;
        }

        // Our miners have priority, don't disturb them
        // Buildings are walls
        if (current_location.distanceSquaredTo(target_location) <= 2
                && controller.canSenseLocation(target_location)) {
            RobotInfo robot = controller.senseRobotAtLocation(target_location);
            if (robot != null
                    && robot.getTeam() == Cache.OUR_TEAM
                    && robot.getType() == RobotType.MINER) {
                return MoveResult.IMPOSSIBLE;
            }
            if (robot != null
                    && robot.getType().isBuilding()
                    && robot.getMode().canMove == false) {
                return MoveResult.IMPOSSIBLE;
            }
        }




        // Is it ready
        if (controller.getMovementCooldownTurns() > 0)
            return MoveResult.FAIL;

        */


        // Target change, stop bugging
        if (!target_location.equals(prev_target)) {
            is_bugging = false;
        }
        prev_target = target_location;

        if (!is_bugging) {
            MoveResult res = greedy.move(target_location);
            // Can move
            if (res == MoveResult.SUCCESS || res == MoveResult.IMPOSSIBLE) {
                return res;
            }
            // Stuck, start bugging
            is_bugging = true;
            clockwise = true;
            count = 0;
            start_bug_location = current_location;
            angle = calculateAngle(current_location,target_location);
            obstacle =
                    controller.adjacentLocation(
                            current_location.directionTo(
                                    target_location));
            stuck_turns = 0;
            return move(target_location);
        } else {
            // Robot trapped
            if (start_bug_location.equals(current_location)) {
                count += 1;
                if (count >= 3) {
                    stuck_turns += 1;
                    return MoveResult.FAIL;
                }
            }
            Direction
                    obstacleDirection =
                    current_location.directionTo(
                            obstacle);
            Direction target_direction = obstacleDirection;
            // Edge Case: Obstacle gone
            if (naive.move(obstacleDirection) == MoveResult.SUCCESS) {
                is_bugging = false;
                return MoveResult.SUCCESS;
            }
            if (clockwise) {
                target_direction =
                        target_direction.rotateRight();
            } else {
                target_direction =
                        target_direction.rotateLeft();
            }
            while (!controller.canMove(target_direction)) {
                if (clockwise) {
                    target_direction =
                            target_direction.rotateRight();
                } else {
                    target_direction =
                            target_direction.rotateLeft();
                }
                //If on the edge of the map, switch bug directions
                //Or, there is no way past
                if (!controller.onTheMap(controller.adjacentLocation(
                        target_direction))) {
                    if (clockwise) {
                        clockwise = false;
                        return move(target_location);
                    } else {
                        stuck_turns += 1;
                        return MoveResult.FAIL;
                    }
                }
                if (target_direction == obstacleDirection) {
                    stuck_turns += 1;
                    return MoveResult.FAIL;
                }
            }
            if (clockwise) {
                obstacle =
                        controller.adjacentLocation(
                                target_direction.rotateLeft());
            } else {
                obstacle =
                        controller.adjacentLocation(
                                target_direction.rotateRight());
            }
            MapLocation
                    moveLoc =
                    controller.adjacentLocation(
                            target_direction);
            //Check if it's passing the original line closer to the target
            if (current_location.distanceSquaredTo(
                    target_location) <
                    start_bug_location.distanceSquaredTo(
                            target_location)) {
                double angle_a = calculateAngle(current_location, target_location);
                double angle_b = calculateAngle(moveLoc, target_location);
                double a = (angle - angle_a + 2 * Math.PI) % Math.PI;
                double b = (angle - angle_b + 2 * Math.PI) % Math.PI;
                if (a < Math.PI && b > Math.PI || a > Math.PI && b < Math.PI) {
                    //controller.setIndicatorString(String.valueOf(a));
                    //controller.setIndicatorString(String.valueOf(b));
                    is_bugging = false;
                }
            }

            return naive.move(target_direction);
        }
    }

    // Util

    private static boolean sameSign(double x, double y) {
        return ((x<0) == (y<0));
    }

    private static double calculateAngle (MapLocation start, MapLocation end) {
        double x = end.x-start.x;
        double y = end.y-start.y;

        // Edge cases
        if (x > 0 && y == 0) {
            return 0.0;
        }
        if (x < 0 && y == 0) {
            return  Math.PI;
        }
        double angle = Math.atan(x/y);
        if (x>0) {
            return angle;
        }
        return angle + Math.PI;
    }

    private static double calculateGradient(
            MapLocation start, MapLocation end) {
        if (start == null || end == null) {
            System.out.println("Who did this. BugNav bug");
        }
        if (end.x - start.x == 0) {
            return -10000;
        }
        //Rise over run
        return 1.0 * (end.y - start.y) / (end.x - start.x);
    }
}
