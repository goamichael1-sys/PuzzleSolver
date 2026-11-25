package com.felo.penguinsolver

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.sqrt

/** Finds the colored grid in a screenshot and reads regions + already-placed penguins. */
object BoardReader {
    class Board(
        val n: Int,
        val centersX: FloatArray,
        val centersY: FloatArray,
        val regions: Array<IntArray>,
        val fixed: List<Pair<Int, Int>>
    )

    private class Run(val start: Int, val len: Int)

    fun read(bmp: Bitmap): Board? {
        val w = bmp.width
        val h = bmp.height
        val pix = IntArray(w * h)
        bmp.getPixels(pix, 0, w, 0, 0, w, h)

        fun r(p: Int) = (p shr 16) and 255
        fun g(p: Int) = (p shr 8) and 255
        fun b(p: Int) = p and 255
        fun white(x: Int, y: Int): Boolean {
            val p = pix[y * w + x]
            return r(p) >= 240 && g(p) >= 240 && b(p) >= 240
        }

        // 1) white board panel = longest contiguous block of "white-ish" rows / columns
        fun longest(flags: BooleanArray, from: Int, to: Int, tol: Int): IntArray? {
            var bestS = -1; var bestE = -1; var s = -1; var lastTrue = -1
            for (i in from until to) {
                if (flags[i]) { if (s < 0) s = i; lastTrue = i }
                else if (s >= 0 && i - lastTrue > tol) {
                    if (bestS < 0 || lastTrue - s > bestE - bestS) { bestS = s; bestE = lastTrue }
                    s = -1
                }
            }
            if (s >= 0 && (bestS < 0 || lastTrue - s > bestE - bestS)) { bestS = s; bestE = lastTrue }
            return if (bestS < 0) null else intArrayOf(bestS, bestE)
        }
        val y0 = (0.12 * h).toInt()
        val y1 = (0.95 * h).toInt()
        val rowFlag = BooleanArray(h)
        for (y in y0 until y1) {
            var cnt = 0
            var x = 0
            while (x < w) { if (white(x, y)) cnt++; x += 2 }
            rowFlag[y] = cnt * 2 > 0.05 * w
        }
        val rb = longest(rowFlag, y0, y1, 6) ?: return null
        val top = rb[0]; val bot = rb[1]
        if (bot - top < w / 4) return null
        val colFlag = BooleanArray(w)
        for (x in 0 until w) {
            var cnt = 0
            var y = top
            while (y < bot) { if (white(x, y)) cnt++; y += 2 }
            colFlag[x] = cnt * 2 > 0.05 * (bot - top)
        }
        val cb2 = longest(colFlag, 0, w, 6) ?: return null
        val left = cb2[0]; val right = cb2[1]
        if (right - left < w / 4) return null

        // 2) tight bbox of non-white pixels inside the panel = the cell grid
        val m = 8
        var cl = Int.MAX_VALUE; var cr = -1; var ct = Int.MAX_VALUE; var cb = -1
        for (y in top + m until bot - m) for (x in left + m until right - m) {
            if (!white(x, y)) {
                if (x < cl) cl = x; if (x > cr) cr = x
                if (y < ct) ct = y; if (y > cb) cb = y
            }
        }
        if (cr < 0) return null

        // 3) count cells by scanning lines (mode of run counts)
        fun runsH(y: Int): List<Run> {
            val out = ArrayList<Run>(); var s = -1
            for (x in cl..cr + 1) {
                val nw = x <= cr && !white(x, y)
                if (nw && s < 0) s = x
                if (!nw && s >= 0) { if (x - s >= 15) out.add(Run(s, x - s)); s = -1 }
            }
            return out
        }
        fun runsV(x: Int): List<Run> {
            val out = ArrayList<Run>(); var s = -1
            for (y in ct..cb + 1) {
                val nw = y <= cb && !white(x, y)
                if (nw && s < 0) s = y
                if (!nw && s >= 0) { if (y - s >= 15) out.add(Run(s, y - s)); s = -1 }
            }
            return out
        }
        val votes = HashMap<Int, Int>()
        var f = 0.08
        while (f < 0.93) {
            val c = runsH(ct + (f * (cb - ct)).toInt()).size
            if (c in 3..16) votes[c] = (votes[c] ?: 0) + 1
            f += 0.03
        }
        val n = votes.maxByOrNull { it.value }?.key ?: return null

        var hRuns: List<Run>? = null
        f = 0.05
        while (f < 0.96 && hRuns == null) {
            val rr = runsH(ct + (f * (cb - ct)).toInt()); if (rr.size == n) hRuns = rr; f += 0.02
        }
        var vRuns: List<Run>? = null
        f = 0.02
        while (f < 0.2 && vRuns == null) {
            val rr = runsV(cl + (f * (cr - cl)).toInt()); if (rr.size == n) vRuns = rr; f += 0.01
        }
        if (vRuns == null) { // try other x positions
            f = 0.2
            while (f < 0.98 && vRuns == null) {
                val rr = runsV(cl + (f * (cr - cl)).toInt()); if (rr.size == n) vRuns = rr; f += 0.02
            }
        }
        val hr: List<Run> = hRuns ?: return null
        val vr: List<Run> = vRuns ?: return null

        val cx = FloatArray(n) { hr[it].start + hr[it].len / 2f }
        val cy = FloatArray(n) { vr[it].start + vr[it].len / 2f }
        val cell = hr.map { it.len }.sorted()[n / 2].toFloat()

        // 4) colors + penguins
        fun avg(px: Int, py: Int): IntArray {
            var sr = 0; var sg = 0; var sb = 0; var k = 0
            for (dy in -2..2) for (dx in -2..2) {
                val x = (px + dx).coerceIn(0, w - 1); val y = (py + dy).coerceIn(0, h - 1)
                val p = pix[y * w + x]; sr += r(p); sg += g(p); sb += b(p); k++
            }
            return intArrayOf(sr / k, sg / k, sb / k)
        }
        // sample colors once
        val samples = Array(n) { row -> Array(n) { col ->
            avg((cx[col] - cell * 0.32f).toInt(), (cy[row] - cell * 0.32f).toInt())
        } }
        fun dist(p: IntArray, q: IntArray): Double =
            sqrt(((p[0] - q[0]) * (p[0] - q[0]) + (p[1] - q[1]) * (p[1] - q[1]) + (p[2] - q[2]) * (p[2] - q[2])).toDouble())

        var regions: Array<IntArray>? = null
        for (th in intArrayOf(40, 30, 22, 16, 11, 55, 70)) {
            val palette = ArrayList<IntArray>()
            val reg = Array(n) { IntArray(n) }
            for (row in 0 until n) for (col in 0 until n) {
                val c3 = samples[row][col]
                var id = -1
                for (i in palette.indices) if (dist(palette[i], c3) < th) { id = i; break }
                if (id < 0) { palette.add(c3); id = palette.size - 1 }
                reg[row][col] = id
            }
            if (palette.size == n) { regions = reg; break }
        }
        val regs = regions ?: return null

        // already-placed penguins = navy pixels near cell center
        val fixed = ArrayList<Pair<Int, Int>>()
        for (row in 0 until n) for (col in 0 until n) {
            var dark = 0
            val half = (cell * 0.3f).toInt()
            for (dy in -half..half step 2) for (dx in -half..half step 2) {
                val x = (cx[col] + dx).toInt().coerceIn(0, w - 1); val y = (cy[row] + dy).toInt().coerceIn(0, h - 1)
                val p = pix[y * w + x]
                if (r(p) < 90 && g(p) < 90 && b(p) < 110) dark++
            }
            if (dark > 12) fixed.add(Pair(row, col))
        }
        return Board(n, cx, cy, regs, fixed)
    }
}
