#include <cstdio>
#include <cstring>
#define si(x) scanf("%d",&x)
#define pi(x) printf("%d",x)
#define enter putchar('\n')
using namespace std;
int vision;
int LIM=0,n;
int xytoid[19][19],b[9],bl;
int DX[109];
int DY[109];
int main()
{	
	freopen("InputVision.txt","r",stdin);
	scanf("Vision:%d",&vision);
	freopen("Table.txt","w",stdout);
	for (n=10;n*n>vision;n--);
	for (int xd=-n;xd<=n;xd++)
		for (int yd=-n;yd<=n;yd++)
			if (xd*xd+yd*yd<=vision)
				DX[LIM]=xd,DY[LIM++]=yd;
	LIM--;
	printf("private final int VISION_RADIUS = %d;\n",vision);	
	printf("private final int LIM = %d;\n",LIM+1);
	printf("private final int SOURCE = %d;\n",(LIM+1)/2);
	printf("private final int[] DX = new int[]{");for (int i=0;i<=LIM;i++){pi(DX[i]);if (i<LIM)printf(",");}printf("};\n");
	printf("private final int[] DY = new int[]{");for (int i=0;i<=LIM;i++){pi(DY[i]);if (i<LIM)printf(",");}printf("};\n");
	LIM++;
	printf("private final int[][] NEIGHBOUR = new int[][]{");
	memset(xytoid,-1,sizeof(xytoid));
	for (int c=0;c<LIM;c++)
	{
		int X=DX[c]+8;
		int Y=DY[c]+8;
		xytoid[X][Y]=c;
	}
	for (int c=0;c<LIM;c++)
	{
		int X=DX[c]+8;
		int Y=DY[c]+8;
		int dx[]={-1,-1,-1,0,0,1,1,1};
		int dy[]={-1,0,1,-1,1,-1,0,1};
		bl=0;
		for (int i=0;i<8;i++)
			if (xytoid[X+dx[i]][Y+dy[i]]>=0)
				b[++bl]=xytoid[X+dx[i]][Y+dy[i]];
		printf("{");
		for (int i=1;i<=bl;i++)
		{
			pi(b[i]);
			if (i<bl)
				printf(",");
		}
		printf("}");
		if (c<LIM-1)
			printf(",");
	}
	printf("};\n");
	printf("private final int[] EIGHT_ID = new int[]{");
	int dx[]={0,1,1,1,0,-1,-1,-1};
	int dy[]={1,1,0,-1,-1,-1,0,1};
	for (int i=0;i<=7;i++)
	{
		pi(xytoid[dx[i]+8][dy[i]+8]);
		if (i<7) printf(",");
	}
	printf("};\n");
	return 0;
}
