package sprintbot.battlecode2022.util;

import battlecode.common.*;

// Data such as turn number, turns alive, local environment etc

public class Cache {

    public static int age = 0;

    public static void update() {
        age += 1;
    }

}
