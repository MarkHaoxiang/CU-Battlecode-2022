#include <cstdio>
#include <string>
using namespace std;
int vision;
void printHead(){printf(R"(package sprintbot13.battlecode2022.util.navigation;

import battlecode.common.*;
import static battlecode.common.Direction.*;
import sprintbot13.battlecode2022.util.*;

public class DPR)");printf("%d",vision);printf(R"(Navigator extends Navigator
{
	private static final int INF_MARBLE = 1000000;
	private RobotController rc;
	private final int UPPER_BOUND_TURN_PER_GRID;
	private final int MOVE_COOLDOWN;
	private final int X_BOUND;
	private final int Y_BOUND;
	private final int[] MARBLE_COST_LOOKUP;
	
	public DPR)");printf("%d",vision);printf(R"(Navigator(RobotController controller)
	{
		super(controller);
		rc = controller;
		X_BOUND = rc.getMapWidth() - 1;    // [0, X_BOUND]
		Y_BOUND = rc.getMapHeight() - 1;
		MOVE_COOLDOWN = rc.getType().movementCooldown;
		UPPER_BOUND_TURN_PER_GRID = MOVE_COOLDOWN * 60;
		MARBLE_COST_LOOKUP = new int[]{(int) (1.0 * MOVE_COOLDOWN) * 10,
				(int) (1.1 * MOVE_COOLDOWN) * 10,
				(int) (1.2 * MOVE_COOLDOWN) * 10,
				(int) (1.3 * MOVE_COOLDOWN) * 10,
				(int) (1.4 * MOVE_COOLDOWN) * 10,
				(int) (1.5 * MOVE_COOLDOWN) * 10,
				(int) (1.6 * MOVE_COOLDOWN) * 10,
				(int) (1.7 * MOVE_COOLDOWN) * 10,
				(int) (1.8 * MOVE_COOLDOWN) * 10,
				(int) (1.9 * MOVE_COOLDOWN) * 10,
				(int) (2.0 * MOVE_COOLDOWN) * 10,
				(int) (2.1 * MOVE_COOLDOWN) * 10,
				(int) (2.2 * MOVE_COOLDOWN) * 10,
				(int) (2.3 * MOVE_COOLDOWN) * 10,
				(int) (2.4 * MOVE_COOLDOWN) * 10,
				(int) (2.5 * MOVE_COOLDOWN) * 10,
				(int) (2.6 * MOVE_COOLDOWN) * 10,
				(int) (2.7 * MOVE_COOLDOWN) * 10,
				(int) (2.8 * MOVE_COOLDOWN) * 10,
				(int) (2.9 * MOVE_COOLDOWN) * 10,
				(int) (3.0 * MOVE_COOLDOWN) * 10,
				(int) (3.1 * MOVE_COOLDOWN) * 10,
				(int) (3.2 * MOVE_COOLDOWN) * 10,
				(int) (3.3 * MOVE_COOLDOWN) * 10,
				(int) (3.4 * MOVE_COOLDOWN) * 10,
				(int) (3.5 * MOVE_COOLDOWN) * 10,
				(int) (3.6 * MOVE_COOLDOWN) * 10,
				(int) (3.7 * MOVE_COOLDOWN) * 10,
				(int) (3.8 * MOVE_COOLDOWN) * 10,
				(int) (3.9 * MOVE_COOLDOWN) * 10,
				(int) (4.0 * MOVE_COOLDOWN) * 10,
				(int) (4.1 * MOVE_COOLDOWN) * 10,
				(int) (4.2 * MOVE_COOLDOWN) * 10,
				(int) (4.3 * MOVE_COOLDOWN) * 10,
				(int) (4.4 * MOVE_COOLDOWN) * 10,
				(int) (4.5 * MOVE_COOLDOWN) * 10,
				(int) (4.6 * MOVE_COOLDOWN) * 10,
				(int) (4.7 * MOVE_COOLDOWN) * 10,
				(int) (4.8 * MOVE_COOLDOWN) * 10,
				(int) (4.9 * MOVE_COOLDOWN) * 10,
				(int) (5.0 * MOVE_COOLDOWN) * 10,
				(int) (5.1 * MOVE_COOLDOWN) * 10,
				(int) (5.2 * MOVE_COOLDOWN) * 10,
				(int) (5.3 * MOVE_COOLDOWN) * 10,
				(int) (5.4 * MOVE_COOLDOWN) * 10,
				(int) (5.5 * MOVE_COOLDOWN) * 10,
				(int) (5.6 * MOVE_COOLDOWN) * 10,
				(int) (5.7 * MOVE_COOLDOWN) * 10,
				(int) (5.8 * MOVE_COOLDOWN) * 10,
				(int) (5.9 * MOVE_COOLDOWN) * 10,
				(int) (6.0 * MOVE_COOLDOWN) * 10,
				(int) (6.1 * MOVE_COOLDOWN) * 10,
				(int) (6.2 * MOVE_COOLDOWN) * 10,
				(int) (6.3 * MOVE_COOLDOWN) * 10,
				(int) (6.4 * MOVE_COOLDOWN) * 10,
				(int) (6.5 * MOVE_COOLDOWN) * 10,
				(int) (6.6 * MOVE_COOLDOWN) * 10,
				(int) (6.7 * MOVE_COOLDOWN) * 10,
				(int) (6.8 * MOVE_COOLDOWN) * 10,
				(int) (6.9 * MOVE_COOLDOWN) * 10,
				(int) (7.0 * MOVE_COOLDOWN) * 10,
				(int) (7.1 * MOVE_COOLDOWN) * 10,
				(int) (7.2 * MOVE_COOLDOWN) * 10,
				(int) (7.3 * MOVE_COOLDOWN) * 10,
				(int) (7.4 * MOVE_COOLDOWN) * 10,
				(int) (7.5 * MOVE_COOLDOWN) * 10,
				(int) (7.6 * MOVE_COOLDOWN) * 10,
				(int) (7.7 * MOVE_COOLDOWN) * 10,
				(int) (7.8 * MOVE_COOLDOWN) * 10,
				(int) (7.9 * MOVE_COOLDOWN) * 10,
				(int) (8.0 * MOVE_COOLDOWN) * 10,
				(int) (8.1 * MOVE_COOLDOWN) * 10,
				(int) (8.2 * MOVE_COOLDOWN) * 10,
				(int) (8.3 * MOVE_COOLDOWN) * 10,
				(int) (8.4 * MOVE_COOLDOWN) * 10,
				(int) (8.5 * MOVE_COOLDOWN) * 10,
				(int) (8.6 * MOVE_COOLDOWN) * 10,
				(int) (8.7 * MOVE_COOLDOWN) * 10,
				(int) (8.8 * MOVE_COOLDOWN) * 10,
				(int) (8.9 * MOVE_COOLDOWN) * 10,
				(int) (9.0 * MOVE_COOLDOWN) * 10,
				(int) (9.1 * MOVE_COOLDOWN) * 10,
				(int) (9.2 * MOVE_COOLDOWN) * 10,
				(int) (9.3 * MOVE_COOLDOWN) * 10,
				(int) (9.4 * MOVE_COOLDOWN) * 10,
				(int) (9.5 * MOVE_COOLDOWN) * 10,
				(int) (9.6 * MOVE_COOLDOWN) * 10,
				(int) (9.7 * MOVE_COOLDOWN) * 10,
				(int) (9.8 * MOVE_COOLDOWN) * 10,
				(int) (9.9 * MOVE_COOLDOWN) * 10,
				(int) (10.0 * MOVE_COOLDOWN) * 10,
				(int) (10.1 * MOVE_COOLDOWN) * 10,
				(int) (10.2 * MOVE_COOLDOWN) * 10,
				(int) (10.3 * MOVE_COOLDOWN) * 10,
				(int) (10.4 * MOVE_COOLDOWN) * 10,
				(int) (10.5 * MOVE_COOLDOWN) * 10,
				(int) (10.6 * MOVE_COOLDOWN) * 10,
				(int) (10.7 * MOVE_COOLDOWN) * 10,
				(int) (10.8 * MOVE_COOLDOWN) * 10,
				(int) (10.9 * MOVE_COOLDOWN) * 10,
				(int) (11.0 * MOVE_COOLDOWN) * 10};
	}
	
	public MoveResult move(MapLocation target_loc) throws GameActionException
	{
		if (!rc.isMovementReady())
			return MoveResult.FAIL;
		if (target_loc == null)
			return MoveResult.IMPOSSIBLE;
		int tar_loc_x = target_loc.x;
		int tar_loc_y = target_loc.y;
		if (tar_loc_x < 0 || tar_loc_y < 0
				|| tar_loc_x > X_BOUND || tar_loc_y > Y_BOUND)
			return MoveResult.IMPOSSIBLE;
		MapLocation current_loc = rc.getLocation();
		if (current_loc.distanceSquaredTo(target_loc) <= 2
				&& rc.canSenseLocation(target_loc)) {
			RobotInfo robot = rc.senseRobotAtLocation(target_loc);
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
		int cur_loc_x = current_loc.x;
		int cur_loc_y = current_loc.y;
		if (current_loc.equals(target_loc))
			return MoveResult.REACHED;
		MapLocation tmp_loc;
		int dctx = cur_loc_x - tar_loc_x;
		int dcty = cur_loc_y - tar_loc_y;
		int dctd = dctx * dctx + dcty * dcty;
		if (dctd <= 2)
		{
			Direction dir = current_loc.directionTo(target_loc);
			if (rc.canMove(dir))
			{
				rc.move(dir);
				return MoveResult.SUCCESS;
			}
			else
				return MoveResult.FAIL;				
		}
		//System.out.printf("Check Complete, left = %d\n", Clock.getBytecodesLeft());
)");}
void printTail(){printf(R"(	}
})");}
#include <algorithm>
#include <string>
const int arrayOffset=15;
int lenVarName[309],lenRubbleName[309],
DX[309],DY[309],xyToID[39][39],neighbour[309][19],lenNeighbour[309],dist[309],isOnRim[309],dirQ[309],dirP[309],
isDPed[309],enc[309],dec[309];
char varName[309][109],rubbleName[309][109];
string eightDirections[]={"","NORTHWEST","NORTH","NORTHEAST","EAST","SOUTHEAST","SOUTH","SOUTHWEST","WEST"};
pair<double,int> dist_id[309];
int N;
void getNodes()
{
	N=0;
	int L;
	for (L=vision;L*L>vision;L--);
	for (int x=-L;x<=L;x++)
		for (int y=-L;y<=L;y++)
			if (x*x+y*y<=vision)
			{
				N++;
				DX[N]=x,DY[N]=y;
				xyToID[x+arrayOffset][y+arrayOffset]=N;
				string str_x=to_string(x);
				std::replace( str_x.begin(), str_x.end(), '-', 'n');
				string str_y=to_string(y);
				std::replace( str_y.begin(), str_y.end(), '-', 'n');
				string full_string="d_"+str_x+"_"+str_y;
    			strcpy(varName[N], full_string.c_str());
				full_string="r_"+str_x+"_"+str_y;
    			strcpy(rubbleName[N], full_string.c_str());
    			lenRubbleName[N]=full_string.length();
    			dist[N]=max(abs(x),abs(y));
    			if (x<dist[N] && y==dist[N])
    				dirQ[N]=1,dirP[N]=x+dist[N]+1;
    			if (x==dist[N] && y>-dist[N])
    				dirQ[N]=2,dirP[N]=dist[N]-y+1;
    			if (x>-dist[N] && y==-dist[N])
    				dirQ[N]=3,dirP[N]=dist[N]-x+1;
    			if (x==-dist[N] && y<dist[N])
    				dirQ[N]=4,dirP[N]=y+dist[N]+1;
    			dist_id[N]=make_pair(dist[N]*1000+dirQ[N]*100+dirP[N],N);
    			enc[N]=(x+7)*15+(y+7);
    			dec[enc[N]]=N;
			}
	sort(dist_id+1,dist_id+N+1);
}
void stab(){printf("	");}
void tab(){printf("		");}
void tab3(){printf("			");}
void tab4(){printf("				");}
void tab5(){printf("					");}
void declareToBeZero(int x){tab();printf("int %s = %s;\n",varName[x],rubbleName[x]);}
void printMinNeighbour(int x,int from=1){if (!from)return;if (from>=lenNeighbour[x]){printf("%s",varName[neighbour[x][from]]);return;}
printf("Math.min(");printMinNeighbour(x,from+1);printf(",%s)",varName[neighbour[x][from]]);}
void printDPVal(int x){printMinNeighbour(x);printf("+%s",rubbleName[x]);}
void declareUponCanMove(int x,int y)
{
	if (lenNeighbour[x])
	{
		tab();
		printf("int %s = Math.min(rc.canMove(%s) ? (%s + %d) : INF_MARBLE,",varName[x],
						eightDirections[y].c_str(),rubbleName[x],y);
		printDPVal(x);
		printf(");\n");
	}
	else
	{
		tab();
		printf("int %s = rc.canMove(%s) ? (%s + %d) : INF_MARBLE;\n",varName[x],
						eightDirections[y].c_str(),rubbleName[x],y);
	}
}
void declareUponDP(int x){tab();printf("int %s = ",varName[x]);printDPVal(x);printf(";\n");}
void updateDP(int x){tab();printf("%s = Math.min(",varName[x]);printDPVal(x);printf(",%s);\n",varName[x]);}
void findNeighbour(int x)
{
	lenNeighbour[x]=0;
	for (int dx=-1;dx<=1;dx++)
		for (int dy=-1;dy<=1;dy++)
			if (dx!=0 || dy!=0)
			{
				int nid=xyToID[DX[x]+dx+arrayOffset][DY[x]+dy+arrayOffset];
				if (!nid)
				{
					isOnRim[x]=1;
					continue;
				}
				if (isDPed[nid] && (DX[nid]!=0 || DY[nid]!=0))
					neighbour[x][++lenNeighbour[x]]=nid;
			}
}
void declareRubble(int x)
{
	tab();printf("tmp_loc = current_loc.translate(%d,%d);\n",DX[x],DY[x]);
	tab();printf("int %s = rc.canSenseLocation(tmp_loc) ? MARBLE_COST_LOOKUP[rc.senseRubble(tmp_loc)] : INF_MARBLE;\n",rubbleName[x]);
}
void calculateDP()
{
	for (int i=1;i<=N;i++)
	{
		int currentID=dist_id[i].second;
		int directionID;
		declareRubble(currentID);
		if (i==1) declareToBeZero(currentID);
		else if (i>=2 && i<=9)
		{
			directionID=i-1;
			findNeighbour(currentID);
			declareUponCanMove(currentID,directionID);
		}
		else if (i>=10)
		{
			findNeighbour(currentID);
			declareUponDP(currentID);
		}
		isDPed[currentID]=1;
	}
	for (int i=1;i<=N;i++)
		dist_id[i].first=dist[dist_id[i].second]*1000+(5-dirQ[dist_id[i].second])*100+(99-dirP[dist_id[i].second]);
	sort(dist_id+1,dist_id+N+1);
	for (int i=2;i<=N;i++)
	{
		int currentID=dist_id[i].second;
		findNeighbour(currentID);
		updateDP(currentID);
	}
}
void declareAnswer(){tab();printf("int minDistance = 1000000000;\n");}
void minWithAnswer(int x){tab3();
printf("minDistance = Math.min(minDistance, %s + UPPER_BOUND_TURN_PER_GRID * Math.max(Math.abs(%d + dctx), Math.abs(%d + dcty)));\n",
varName[x],DX[x],DY[x]);}
void findAnswer()
{
	tab();printf("if (dctd <= %d)\n",vision);
	tab();printf("{\n");
	tab3();printf("switch ((7-dctx)*15+7-dcty)\n");
	tab3();printf("{\n");
	for (int i=1;i<=N;i++)
		{tab4();printf("case %d:minDistance=%s;break;\n",enc[i],varName[i]);}
	tab4();printf("default:System.out.printf(\"Error in DPNavigator: unexpected encoded value \%d\",(7-dctx)*15+7-dcty);break;\n");
	tab3();printf("}\n");
	tab();printf("}\n");
	tab();printf("else\n");
	tab();printf("{\n");
	for (int i=1;i<=N;i++)
		if (isOnRim[i])
			minWithAnswer(i);
	tab();printf("}\n");
}
void finalTrace()
{
	tab();
	printf("if (minDistance >= INF_MARBLE)\n");
	tab3();
	printf("return MoveResult.FAIL;\n");
	tab();
	printf("switch (minDistance % 10)\n");
	tab();
	printf("{\n");
	for (int i=1;i<=8;i++)
	{
		tab3();printf("case %d:\n",i);
		tab4();printf("if (rc.canMove(%s))\n",eightDirections[i].c_str());
		tab4();printf("{\n");
		tab5();printf("rc.move(%s);\n",eightDirections[i].c_str());
		tab5();printf("return MoveResult.SUCCESS;\n");
		tab4();printf("}\n");
		tab4();printf("else\n");
		tab5();printf("return MoveResult.FAIL;\n");
	}
	tab3();printf("default:\n");
	tab4();printf("return MoveResult.FAIL;\n");
	tab();
	printf("}\n");
}
int main()
{
	vision=53;//16 20 34 53
	string str = "DPR"+to_string(vision)+"Navigator.java";
	freopen(str.c_str(),"w",stdout);
	printHead();
	getNodes();
	calculateDP();
	declareAnswer();
	findAnswer();
	finalTrace();
	printTail();
	return 0;
}
