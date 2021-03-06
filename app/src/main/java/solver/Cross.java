package solver;

import android.util.Log;

import java.io.*;
import java.util.Random;

import com.dctimer.APP;

import static solver.Utils.Cnk;
import static solver.Utils.getPruning;
import static solver.Utils.read;
import static solver.Utils.setPruning;
import static solver.Utils.write;

public class Cross {
    private static short[][] epm = new short[11880][6], eom = new short[7920][6];
    private static byte[] epd = new byte[11880], eod = new byte[7920];
    private static byte[] eofd = new byte[7920];
    private static int[] ed = new int[23760];
    private static byte[][] fcm = new byte[24][6], fem = new byte[24][6];
    private static byte[][] fecd = new byte[4][576];
    private static String[] seq = new String[20];
    public static boolean ini, inif;
    private static String[] color = {"D", "U", "L", "R", "F", "B"};
    private static String[][] moveIdx = {
            { "UDLRFB", "DURLFB", "RLUDFB", "LRDUFB", "BFLRUD", "FBLRDU" },
            { "UDLRFB", "DURLFB", "RLUDFB", "LRDUFB", "BFRLDU", "FBRLUD" },
            { "UDLRFB", "DURLFB", "RLUDFB", "LRDUFB", "BFUDRL", "FBUDLR" },
            { "UDLRFB", "DURLFB", "RLUDFB", "LRDUFB", "BFDULR", "FBDURL" },
            { "UDLRFB", "DULRBF", "RLBFUD", "LRFBUD", "BFLRUD", "FBRLUD" },
            { "UDLRFB", "DULRBF", "RLFBDU", "LRBFDU", "BFRLDU", "FBLRDU" }
    };
    private static String[][] rotIdx = {
            { "", "z2", "z'", "z", "x'", "x" }, { "z2", "", "z", "z'", "x", "x'" },
            { "z", "z'", "", "z2", "y", "y'" }, { "z'", "z", "z2", "", "y'", "y" },
            { "x", "x'", "y'", "y", "", "y2" }, { "x'", "x", "y", "y'", "y2", "" }
    };
    private static String[] sideStr = {"D(FB)", "D(LR)", "U(FB)", "U(LR)",
            "L(FB)", "L(UD)", "R(FB)", "R(UD)", "F(UD)", "F(LR)", "B(UD)", "B(LR)"};
    private static String[][] turn = {
            { "U", "D", "L", "R", "F", "B" }, { "D", "U", "R", "L", "F", "B" },
            { "R", "L", "U", "D", "F", "B" }, { "L", "R", "D", "U", "F", "B" },
            { "B", "F", "L", "R", "U", "D" }, { "F", "B", "L", "R", "D", "U" }
    };
    private static String[] suff = {"", "2", "'"};

    public static void circle(int[] ary, int a, int b, int c, int d, int ori) {
        int t = ary[a];
        ary[a] = ary[d] ^ ori;
        ary[d] = ary[c] ^ ori;
        ary[c] = ary[b] ^ ori;
        ary[b] = t ^ ori;
    }

    protected static void idxToPerm(int[] arr, int p) {
        int j;
        for (int i = 1; i <= 4; i++) {
            int t = p % i;
            for (p = p / i, j = i - 2; j >= t; j--)
                arr[j + 1] = arr[j];
            arr[t] = 4 - i;
        }
    }

    protected static int permToIdx(int[] arr) {
        int idx = 0, j, t;
        for (int i = 0; i < 4; i++) {
            for (j = t = 0; j < 4 && arr[j] != i; j++)
                if (arr[j] > i) t++;
            idx = idx * (4 - i) + t;
        }
        return idx;
    }

    private static int idxToComb(int[] n, int[] s, int c, int o) {
        int q = 4;
        for (int i = 0; i < 12; i++)
            if (c >= Cnk[11 - i][q]) {
                c -= Cnk[11 - i][q--];
                n[i] = s[q] << 1 | o & 1;
                o >>= 1;
            } else n[i] = -1;
        return o;
    }

    private static int getmv(int c, int p, int o, int f) {
        int[] arr = new int[12], s = new int[4];
        int q, t;
        idxToPerm(s, p);
        o = idxToComb(arr, s, c, o);
        switch (f) {
            case 0:
                circle(arr, 0, 1, 2, 3, 0);
                break;
            case 1:
                circle(arr, 11, 10, 9, 8, 0);
                break;
            case 2:
                circle(arr, 1, 4, 9, 5, 0);
                break;
            case 3:
                circle(arr, 3, 6, 11, 7, 0);
                break;
            case 4:
                circle(arr, 0, 7, 8, 4, 1);
                break;
            case 5:
                circle(arr, 2, 5, 10, 6, 1);
                break;
        }
        c = 0;
        q = 4;
        for (t = 0; t < 12; t++)
            if (arr[t] >= 0) {
                c += Cnk[11 - t][q--];
                s[q] = arr[t] >> 1;
                o |= (arr[t] & 1) << 3 - q;
            }
        int i = permToIdx(s);
        // for (q=0;4>q;q++) {
        // for (v=t=0;4>v&&!(s[v]==q);v++) if (s[v]>q) t++;
        // i=i*(4-q)+t;
        // }
        return 24 * c + i << 4 | o;
    }

    private static void init() {
        if (ini)
            return;
        int a, b, c, d, e, f;
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(APP.dataPath + "cross.dat"));
            read(epm, in);
            read(eom, in);
            in.close();
        } catch (Exception ex) {
            for (a = 0; a < 495; a++) {
                for (b = 0; b < 24; b++) {
                    for (c = 0; c < 6; c++) {
                        d = getmv(a, b, b, c);
                        epm[24 * a + b][c] = (short) (d >> 4);
                        if (b < 16)
                            eom[16 * a + b][c] = (short) (((d >> 4) / 24) << 4 | d & 15);
                    }
                }
            }
            try {
                OutputStream out = new BufferedOutputStream(new FileOutputStream(APP.dataPath + "cross.dat"));
                write(epm, out);
                write(eom, out);
                out.close();
            } catch (Exception ex2) { }
        }
        for (a = 1; a < 11880; a++)
            epd[a] = -1;
        epd[0] = 0;
        Utils.createPrun(epd, 6, epm, 3);
        for (a = 0; a < 7920; a++)
            eod[a] = eofd[a] = -1;
        eod[0] = 0;
        Utils.createPrun(eod, 7, eom, 3);
        for (a = 0; a < 495; a++)
            eofd[a << 4] = 0;
        Utils.createPrun(eofd, 7, eom, 3);
        //xcross
        byte[][] p = {
                {1, 0, 3, 0, 0, 4}, {2, 1, 1, 5, 1, 0}, {3, 2, 2, 1, 6, 2}, {0, 3, 7, 3, 2, 3},
                {4, 7, 0, 4, 4, 5}, {5, 4, 5, 6, 5, 1}, {6, 5, 6, 2, 7, 6}, {7, 6, 4, 7, 3, 7}
        };
        byte[][] o = {
                {0, 0, 1, 0, 0, 2}, {0, 0, 0, 2, 0, 1}, {0, 0, 0, 1, 2, 0}, {0, 0, 2, 0, 1, 0},
                {0, 0, 2, 0, 0, 1}, {0, 0, 0, 1, 0, 2}, {0, 0, 0, 2, 1, 0}, {0, 0, 1, 0, 2, 0}
        };
        for (a = 0; a < 8; a++)
            for (b = 0; b < 3; b++)
                for (c = 0; c < 6; c++)
                    fcm[a * 3 + b][c] = (byte) (p[a][c] * 3 + (o[a][c] + b) % 3);
        p = new byte[][] {
                {0, 0, 7, 0, 0, 8}, {1, 1, 1, 9, 1, 4}, {2, 2, 2, 5, 10, 2}, {3, 3, 11, 3, 6, 3},
                {5, 4, 4, 4, 4, 0}, {6, 5, 5, 1, 5, 5}, {7, 6, 6, 6, 2, 6}, {4, 7, 3, 7, 7, 7},
                {8, 11, 8, 8, 8, 1}, {9, 8, 9, 2, 9, 9}, {10, 9, 10, 10, 3, 10}, {11, 10, 0, 11, 11, 11}
        };
        o = new byte[][] {
                {0, 0, 0, 0, 0, 1}, {0, 0, 0, 0, 0, 1}, {0, 0, 0, 0, 1, 0}, {0, 0, 0, 0, 1, 0},
                {0, 0, 0, 0, 0, 1}, {0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 1, 0}, {0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 1}, {0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 1, 0}, {0, 0, 0, 0, 0, 0}
        };
        for (a = 0; a < 12; a++)
            for (b = 0; b < 2; b++)
                for (c = 0; c < 6; c++)
                    fem[a * 2 + b][c] = (byte) (p[a][c] * 2 + (o[a][c] ^ b));
        for (f = 0; f < 4; f++) {
            for (a = 0; a < 576; a++) fecd[f][a] = -1;
            fecd[f][f * 51 + 12] = 0;
            for (d = 0; d < 6; d++)
                for (a = 0; a < 576; a++)
                    if (fecd[f][a] == d)
                        for (b = 0; b < 6; b++)
                            for (e = a, c = 0; c < 3; c++) {
                                e = 24 * fem[e / 24][b] + fcm[e % 24][b];
                                if (fecd[f][e] == -1)
                                    fecd[f][e] = (byte) (d + 1);
                            }
        }
        ini = true;
    }

    private static boolean idacross(int ep, int eo, int d, int lm, int face) {
        if (d == 0) return 0 == ep && 0 == eo;
        if (epd[ep] > d || eod[eo] > d) return false;
        for (int i = 0; i < 6; i++)
            if (i != lm) {
                int epx = ep, eox = eo;
                for (int j = 0; j < 3; j++) {
                    epx = epm[epx][i]; eox = eom[eox][i];
                    if (idacross(epx, eox, d - 1, i, face)) {
                        seq[d] = turn[face][i] + suff[j];
                        //sb.insert(0, " " + turn[face][i] + suff[j]);
                        return true;
                    }
                }
            }
        return false;
    }

    private static boolean idaeofc(int ep, int eo, int eof, int d, int lm) {
        if (d == 0) return 0 == ep && 0 == eo && (eof & 15) == 0;
        if (epd[ep] > d || eod[eo] > d || eofd[eof] > d) return false;
        for (int i = 0; i < 6; i++)
            if (i != lm) {
                int epx = ep, eox = eo, eofx = eof;
                for (int j = 0; j < 3; j++) {
                    epx = epm[epx][i]; eox = eom[eox][i]; eofx = eom[eofx][i];
                    if (idaeofc(epx, eox, eofx, d-1, i)) {
                        seq[d] = turn[0][i] + suff[j];
                        return true;
                    }
                }
            }
        return false;
    }

    private static boolean idaxcross(int ep, int eo, int co, int feo, int idx, int d, int l) {
        if (d == 0) return ep == 0 && eo == 0 && co == (idx + 4) * 3 && feo == idx * 2;
        if (epd[ep] > d || eod[eo] > d || fecd[idx][feo * 24 + co] > d) return false;
        for (int i = 0; i < 6; i++)
            if (i != l) {
                int cox = co, epx = ep, eox = eo, fx = feo;
                for (int j = 0; j < 3; j++) {
                    cox = fcm[cox][i]; fx = fem[fx][i];
                    epx = epm[epx][i]; eox = eom[eox][i];
                    if (idaxcross(epx, eox, cox, fx, idx, d - 1, i)) {
                        seq[d] = turn[0][i] + suff[j];
                        //sb.insert(0, " " + turn[0][i] + suff[j]);
                        return true;
                    }
                }
            }
        return false;
    }

    public static String solveCross(String scramble, int side) {
        init();
        StringBuilder s = new StringBuilder("\n");
        for (int i = 0; i < 6; i++) {
            if (((side >> i) & 1) != 0) s.append(cross(scramble, 0, i));
        }
        return s.toString();
    }

    private static String cross(String scramble, int face, int side) {
        String[] q = scramble.split(" ");
        int eo = 0, ep = 0;
        for (int i = 0; i < q.length; i++)
            if (q[i].length() != 0) {
                int m = moveIdx[face][side].indexOf(q[i].charAt(0));
                eo = eom[eo][m]; ep = epm[ep][m];
                if (q[i].length() > 1) {
                    eo = eom[eo][m];
                    ep = epm[ep][m];
                    if (q[i].charAt(1) == '\'') {
                        eo = eom[eo][m];
                        ep = epm[ep][m];
                    }
                }
            }
        //sb = new StringBuilder();
        for (int d = 0; d < 10; d++) {
            if (idacross(ep, eo, d, -1, face)) {
                //Log.w("dct", i+"\t"+sb.toString());
                StringBuilder sb = new StringBuilder("\nCross(");
                sb.append(color[side]).append("): ").append(rotIdx[face][side]);
                for (int j = d; j > 0; j--)
                    sb.append(' ').append(seq[j]);
                return sb.toString();
            }
        }
        return "\nerror";
    }

    public static String solveXcross(String scramble, int side) {
        init();
        StringBuilder sb = new StringBuilder("\n");
        for (int i = 0; i < 6; i++) {
            if (((side >> i) & 1) != 0) sb.append(xcross(scramble, i));
        }
        return sb.toString();
    }

    private static String xcross(String scramble, int side) {
        String[] s = scramble.split(" ");
        int[] co = new int[4], feo = new int[4];
        for (int i = 0; i < 4; i++) {
            co[i] = (i + 4) * 3;
            feo[i] = i * 2;
        }
        int ep = 0, eo = 0;
        for (int d = 0; d < s.length; d++)
            if (s[d].length() != 0) {
                int m = moveIdx[0][side].indexOf(s[d].charAt(0));
                for (int i = 0; i < 4; i++) {
                    co[i] = fcm[co[i]][m];
                    feo[i] = fem[feo[i]][m];
                }
                ep = epm[ep][m]; eo = eom[eo][m];
                if (s[d].length() > 1) {
                    for (int i = 0; i < 4; i++) {
                        co[i] = fcm[co[i]][m];
                        feo[i] = fem[feo[i]][m];
                    }
                    eo = eom[eo][m]; ep = epm[ep][m];
                    if (s[d].charAt(1) == '\'') {
                        for (int i = 0; i < 4; i++) {
                            co[i] = fcm[co[i]][m];
                            feo[i] = fem[feo[i]][m];
                        }
                        eo = eom[eo][m]; ep = epm[ep][m];
                    }
                }
            }
        //sb = new StringBuilder();
        for (int d = 0; d < 12; d++)
            for (int slot = 0; slot < 4; slot++)
                if (idaxcross(ep, eo, co[slot], feo[slot], slot, d, -1)) {
                    StringBuilder sb = new StringBuilder("\nXCross(");
                    sb.append(color[side]).append("): ").append(rotIdx[0][side]);
                    for (int i = d; i > 0; i--) sb.append(' ').append(seq[i]);
                    return sb.toString();
                }
                    //return "\nXCross(" + color[side] + "): " + rotateIdx[0][side] + sb.toString();
        return "\nerror";
    }

    public static String solveEofc(String scramble, int side) {
        init();
        StringBuilder sb = new StringBuilder("\n");
        for (int i = 0; i < 6; i++) {
            if (((side >> i) & 1) != 0)
                sb.append(eofc(scramble, i * 2)).append(eofc(scramble, i * 2 + 1));
        }
        return sb.toString();
    }

    public static String eofc(String scramble, int side) {
        String[] s = scramble.split(" ");
        int eo = 0, ep = 0, eof = 69 << 4;
        for (int i = 0; i < s.length; i++)
            if (s[i].length() != 0) {
                int m = EOline.moveIdx[side].indexOf(s[i].charAt(0));
                eo = eom[eo][m]; ep = epm[ep][m]; eof = eom[eof][m];
                if (s[i].length() > 1) {
                    eo = eom[eo][m]; ep = epm[ep][m]; eof = eom[eof][m];
                    if (s[i].charAt(1) == '\'') {
                        eo = eom[eo][m]; ep = epm[ep][m]; eof = eom[eof][m];
                    }
                }
            }
        for (int d = 0; d < 13; d++) {
            //Log.w("dct", ""+d);
            if (idaeofc(ep, eo, eof, d, -1)) {
                StringBuilder sb = new StringBuilder("\n");
                sb.append(sideStr[side]).append(": ").append(EOline.rotateIdx[side]);
                for (int j = d; j > 0; j--) sb.append(' ').append(seq[j]);
                return sb.toString();
            }
        }
        return "\nerror";
    }

    public static int[][] easyCross(int depth) {
        if (!inif) {
            init();
            long t = System.currentTimeMillis();
            for (int i = 0; i < 23760; i++) ed[i] = -1;
            setPruning(ed, 0, 0);
            int c = 1;
            for (int d = 0; d < 8; d++) {
                // c=0;
                for (int i = 0; i < 190080; i++)
                    if (getPruning(ed, i) == d)
                        for (int m = 0; m < 6; m++) {
                            int x = i;
                            for (int n = 0; n < 3; n++) {
                                int ori = x & 15;
                                int p = epm[x >> 4][m];
                                int o = eom[x / 384 << 4 | ori][m];
                                x = p << 4 | (o & 15);
                                if (getPruning(ed, x) == 0xf) {
                                    setPruning(ed, x, d + 1);
                                    c++;
                                }
                            }
                        }
                //Log.w("dct", d+1+"\t"+c);
            }
            t = System.currentTimeMillis() - t;
            //Log.w("dct", t+"ms init");
            inif = true;
        }
        Random r = new Random();
        int i;// = r.nextInt(190080);
        if (depth == 0) i = 0;
        else do {
            i = r.nextInt(190080);
        } while (getPruning(ed, i) > depth);
        int comb = i / 384;
        int perm = (i >> 4) % 24;
        int ori = i & 15;
        int[] c = new int[12];
        int[] p = new int[4];
        idxToPerm(p, perm);
        idxToComb(c, p, comb, ori);
        int[][] arr = new int[2][12];
        int[] idx = { 7, 6, 5, 4, 10, 9, 8, 11, 3, 2, 1, 0 };
        for (i = 0; i < 12; i++) {
            if (c[i] == -1)
                arr[0][idx[i]] = arr[1][idx[i]] = -1;
            else {
                arr[0][idx[i]] = c[i] >> 1;
                arr[1][idx[i]] = c[i] & 1;
            }
        }
        return arr;
    }
}
