package com.example.tetris;

import android.app.*;
import android.os.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import android.content.*;
import java.util.*;

public class MainActivity extends Activity {
    GameView game;
    TextView info;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(14,14,14,14);
        root.setBackgroundColor(Color.rgb(17,24,39));

        info = new TextView(this);
        info.setTextColor(Color.WHITE); info.setTextSize(18); info.setGravity(Gravity.CENTER);
        root.addView(info, new LinearLayout.LayoutParams(-1, dp(42)));

        game = new GameView(this);
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-1,0,1);
        root.addView(game,gp);

        LinearLayout row1 = row();
        row1.addView(button("↶ 旋转", v->game.rotate()), weight());
        row1.addView(button("暂停", v->game.togglePause()), weight());
        row1.addView(button("重开", v->game.restart()), weight());
        root.addView(row1,new LinearLayout.LayoutParams(-1,dp(54)));

        LinearLayout row2 = row();
        row2.addView(button("←", v->game.move(-1)), weight());
        row2.addView(button("↓", v->game.softDrop()), weight());
        row2.addView(button("→", v->game.move(1)), weight());
        row2.addView(button("落底", v->game.hardDrop()), weight());
        root.addView(row2,new LinearLayout.LayoutParams(-1,dp(64)));
        setContentView(root);
        game.updateInfo();
    }
    LinearLayout row(){ LinearLayout r=new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL); return r; }
    LinearLayout.LayoutParams weight(){ return new LinearLayout.LayoutParams(0,-1,1); }
    Button button(String s, View.OnClickListener l){
        Button b=new Button(this); b.setText(s); b.setTextSize(16); b.setAllCaps(false); b.setOnClickListener(l); return b;
    }
    int dp(int x){ return (int)(x*getResources().getDisplayMetrics().density+.5f); }

    class GameView extends View {
        final int W=10,H=20;
        int[][] board=new int[H][W];
        final int[][] colors={0,0xff00bcd4,0xffffc107,0xff9c27b0,0xff4caf50,0xfff44336,0xff3f51b5,0xffff9800};
        final int[][][][] shapes={
            {{{0,0},{1,0},{2,0},{3,0}},{{1,0},{1,1},{1,2},{1,3}},{{0,1},{1,1},{2,1},{3,1}},{{2,0},{2,1},{2,2},{2,3}}},
            {{{0,0},{1,0},{0,1},{1,1}},{{0,0},{1,0},{0,1},{1,1}},{{0,0},{1,0},{0,1},{1,1}},{{0,0},{1,0},{0,1},{1,1}}},
            {{{1,0},{0,1},{1,1},{2,1}},{{1,0},{1,1},{2,1},{1,2}},{{0,1},{1,1},{2,1},{1,2}},{{1,0},{0,1},{1,1},{1,2}}},
            {{{1,0},{2,0},{0,1},{1,1}},{{1,0},{1,1},{2,1},{2,2}},{{1,1},{2,1},{0,2},{1,2}},{{0,0},{0,1},{1,1},{1,2}}},
            {{{0,0},{1,0},{1,1},{2,1}},{{2,0},{1,1},{2,1},{1,2}},{{0,1},{1,1},{1,2},{2,2}},{{1,0},{0,1},{1,1},{0,2}}},
            {{{0,0},{0,1},{1,1},{2,1}},{{1,0},{2,0},{1,1},{1,2}},{{0,1},{1,1},{2,1},{2,2}},{{1,0},{1,1},{0,2},{1,2}}},
            {{{2,0},{0,1},{1,1},{2,1}},{{1,0},{1,1},{1,2},{2,2}},{{0,1},{1,1},{2,1},{0,2}},{{0,0},{1,0},{1,1},{1,2}}}
        };
        Random rng=new Random(); Handler h=new Handler(Looper.getMainLooper());
        int type,rot,x,y,score,lines,level; boolean paused=false,over=false;
        Runnable tick=new Runnable(){ public void run(){ if(!paused&&!over) step(); h.postDelayed(this, Math.max(120,650-level*45)); }};

        GameView(Context c){ super(c); setBackgroundColor(0xff0b1220); restart(); h.postDelayed(tick,650); }
        void spawn(){ type=rng.nextInt(7); rot=0; x=3; y=0; if(collide(x,y,rot)){over=true;} updateInfo(); invalidate(); }
        boolean collide(int nx,int ny,int nr){
            for(int[] p:shapes[type][nr]){int xx=nx+p[0], yy=ny+p[1]; if(xx<0||xx>=W||yy>=H||(yy>=0&&board[yy][xx]!=0)) return true;} return false;
        }
        void step(){ if(!collide(x,y+1,rot)){y++;} else lock(); invalidate(); }
        void lock(){
            for(int[] p:shapes[type][rot]){int xx=x+p[0],yy=y+p[1]; if(yy>=0) board[yy][xx]=type+1;}
            clearLines(); spawn();
        }
        void clearLines(){
            int n=0;
            for(int r=H-1;r>=0;r--){boolean full=true; for(int c=0;c<W;c++) if(board[r][c]==0){full=false;break;}
                if(full){n++; for(int rr=r;rr>0;rr--) board[rr]=board[rr-1].clone(); board[0]=new int[W]; r++;}
            }
            if(n>0){ lines+=n; score+=new int[]{0,100,300,500,800}[n]*(level+1); level=lines/10; updateInfo(); }
        }
        void move(int d){ if(!paused&&!over&&!collide(x+d,y,rot)){x+=d;invalidate();} }
        void rotate(){int nr=(rot+1)%4; if(!paused&&!over){ if(!collide(x,y,nr))rot=nr; else if(!collide(x-1,y,nr)){x--;rot=nr;} else if(!collide(x+1,y,nr)){x++;rot=nr;} invalidate();}}
        void softDrop(){if(!paused&&!over){ if(!collide(x,y+1,rot)){y++;score++;updateInfo();} else lock(); invalidate();}}
        void hardDrop(){if(!paused&&!over){int d=0;while(!collide(x,y+1,rot)){y++;d++;}score+=d*2;lock();updateInfo();invalidate();}}
        void togglePause(){if(!over){paused=!paused;updateInfo();invalidate();}}
        void restart(){for(int r=0;r<H;r++)Arrays.fill(board[r],0);score=lines=level=0;paused=over=false;spawn();}
        void updateInfo(){ if(info!=null) info.setText("分数 "+score+"   消行 "+lines+"   等级 "+(level+1)+(paused?"   ⏸":over?"   游戏结束":"")); }
        @Override protected void onDraw(Canvas c){
            super.onDraw(c); float cell=Math.min(getWidth()/(float)W,getHeight()/(float)H); float ox=(getWidth()-cell*W)/2f, oy=(getHeight()-cell*H)/2f;
            Paint p=new Paint(1);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);p.setColor(0xff263244);
            for(int r=0;r<H;r++)for(int col=0;col<W;col++){float l=ox+col*cell,t=oy+r*cell;c.drawRect(l,t,l+cell,t+cell,p);}
            p.setStyle(Paint.Style.FILL);
            for(int r=0;r<H;r++)for(int col=0;col<W;col++)if(board[r][col]!=0) block(c,p,ox,oy,cell,col,r,colors[board[r][col]]);
            if(!over)for(int[] q:shapes[type][rot]) block(c,p,ox,oy,cell,x+q[0],y+q[1],colors[type+1]);
            if(paused||over){p.setColor(0xaa000000);c.drawRect(ox,oy,ox+W*cell,oy+H*cell,p);p.setColor(Color.WHITE);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(cell*1.2f);c.drawText(over?"游戏结束":"已暂停",getWidth()/2f,getHeight()/2f,p);}
        }
        void block(Canvas c,Paint p,float ox,float oy,float s,int xx,int yy,int color){ if(yy<0)return;p.setColor(color);float pad=2;c.drawRoundRect(ox+xx*s+pad,oy+yy*s+pad,ox+(xx+1)*s-pad,oy+(yy+1)*s-pad,5,5,p);}
    }
}
