package sprintbot10.battlecode2022.util.navigation;

import sprintbot10.battlecode2022.util.Navigator;
import battlecode.common.*;

public class GreedyNavigator extends Navigator {


    private final NaiveNavigator naive;

    public GreedyNavigator(RobotController controller) {
        super(controller);
        naive = new NaiveNavigator(controller);
    }

    // Greedy move towards target, never away (3 options)
    @Override
    public MoveResult move(MapLocation target_location) throws GameActionException {

        // No target location specified
        if (target_location == null) return MoveResult.IMPOSSIBLE;


        MapLocation current_location = controller.getLocation();

        // Reached
        if (current_location.equals(target_location)) return MoveResult.REACHED;

        // Just naive move if distance = 1
        if (travelDistance(current_location, target_location) == 1) return naive.move(target_location);

        // Potential choices

        MapLocation
                a =
                current_location.add(current_location.directionTo(
                        target_location));
        MapLocation
                b =
                current_location.add(current_location.directionTo(
                        target_location).rotateRight());
        MapLocation
                c =
                current_location.add(current_location.directionTo(
                        target_location).rotateLeft());
        MapLocation[] choices = new MapLocation[3];

        //Bytecode efficient insertion sort
        if (controller.canSenseLocation(a)) {
            choices[0] = a;
        }
        if (controller.canSenseLocation(b)) {
            double
                    costA =
                    controller.senseRubble(a) / 2.0;
            double
                    costB =
                    controller.senseRubble(b);
            if (costB < costA) {
                choices[0] = b;
                choices[1] = a;
            } else {
                choices[1] = b;
            }
            if (controller.canSenseLocation(c)) {
                double
                        costC =
                        controller.senseRubble(c);
                if (costC < Math.min(costA, costB)) {
                    choices[2] = choices[1];
                    choices[1] = choices[0];
                    choices[0] = c;
                } else if (costC < costA || costC < costB) {
                    choices[2] = choices[1];
                    choices[1] = c;
                } else {
                    choices[2] = c;
                }
            }
            // TODO: Update for this year?
        } else if (controller.canSenseLocation(c)) {
            if (2.0 * controller.senseRubble(c) <
                    controller.senseRubble(a)) {
                choices[0] = c;
                choices[1] = a;
            }
        }

        // Move
        for (int i = 0; i <= 2; i++) {
            if (choices[i] == null)
                return MoveResult.FAIL;
            if (naive.move(choices[i]) == MoveResult.SUCCESS)
                return MoveResult.SUCCESS;
        }
        return MoveResult.FAIL;
    }
}
