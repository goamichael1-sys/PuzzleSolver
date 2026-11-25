package com.felo.penguinsolver

/** Queens-style solver: 1 per row, column, color region; no two touching (incl. diagonals). */
object Solver {
    /** regions[r][c] = region id (0..n-1). fixed = cells already holding a penguin. Returns col per row or null. */
    fun solve(regions: Array<IntArray>, fixed: List<Pair<Int, Int>>): IntArray? {
        val n = regions.size
        val cols = IntArray(n) { -1 }
        val usedCol = BooleanArray(n)
        val usedReg = BooleanArray(n)
        for ((r, c) in fixed) {
            if (cols[r] != -1 || usedCol[c] || usedReg[regions[r][c]]) return null
            cols[r] = c; usedCol[c] = true; usedReg[regions[r][c]] = true
        }
        fun ok(r: Int, c: Int): Boolean {
            if (usedCol[c] || usedReg[regions[r][c]]) return false
            if (r > 0 && cols[r - 1] != -1 && Math.abs(cols[r - 1] - c) <= 1) return false
            if (r < n - 1 && cols[r + 1] != -1 && Math.abs(cols[r + 1] - c) <= 1) return false
            return true
        }
        fun go(r: Int): Boolean {
            if (r == n) return true
            if (cols[r] != -1) return go(r + 1)
            for (c in 0 until n) {
                if (!ok(r, c)) continue
                cols[r] = c; usedCol[c] = true; usedReg[regions[r][c]] = true
                if (go(r + 1)) return true
                cols[r] = -1; usedCol[c] = false; usedReg[regions[r][c]] = false
            }
            return false
        }
        return if (go(0)) cols else null
    }
}
