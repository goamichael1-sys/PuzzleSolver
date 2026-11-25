package com.felo.penguinsolver

import android.graphics.Bitmap

/** Vision for "hole" games: finds the black hole and the nearest items, returns a steering direction. */
object HoleBot {
    class Plan(val dx: Float, val dy: Float, val items: Int, val holeX: Float, val holeY: Float)

    private class Comp {
        var count = 0
        var minX = 1 shl 30; var maxX = -1; var minY = 1 shl 30; var maxY = -1
        val cells = ArrayList<Int>()
    }

    fun plan(bmp: Bitmap, ignoreLeft: Int = 0, ignoreTop: Int = 0, ignoreRight: Int = 0, ignoreBottom: Int = 0): Plan {
        val w = bmp.width
        val h = bmp.height
        val step = (w / 90).coerceAtLeast(6)
        val gw = w / step
        val gh = h / step
        val gy0 = (0.20 * h / step).toInt()
        val gy1 = (0.69 * h / step).toInt().coerceAtMost(gh)
        val gx1 = (0.97 * gw).toInt()

        // 0 ignore, 1 item, 2 dark (hole), 3 white, 4 blue (water or shoes)
        val code = IntArray(gw * gh)
        val kind = IntArray(gw * gh)   // 0 unknown, 1 lipstick/polish family, 2 heart, 3 shoe
        for (gy in gy0 until gy1) for (gx in 0 until gx1) {
            val px = gx * step + step / 2
            val py = gy * step + step / 2
            if (ignoreRight > ignoreLeft && px in ignoreLeft..ignoreRight && py in ignoreTop..ignoreBottom) continue
            val p = bmp.getPixel(px, py)
            val r = (p shr 16) and 255; val g = (p shr 8) and 255; val b = p and 255
            val mx = maxOf(r, g, b); val mn = minOf(r, g, b)
            kind[gy * gw + gx] = when {
                r > 170 && g < 90 && b in 60..149 -> 2
                r > 200 && g in 80..170 && b > 110 -> 1
                r > 180 && g > 165 && b < 120 -> 1
                r < 100 && g < 110 && b < 160 && b > r + 25 -> 1
                r < 150 && g > 165 && b > 200 -> 3
                else -> 0
            }
            val isBrownDirtOrCliff = r > g && g >= b && (r - g) > 15 && (g - b) < 65 && r < 220
            code[gy * gw + gx] = when {
                r >= 235 && g >= 235 && b >= 235 -> 3
                r < 70 && g < 70 && b < 80 && (b - r) < 35 -> 2
                g > r + 40 && g > b + 60 -> 0                        // grass + hole shadow
                b > 160 && b >= g && b > r + 35 -> 4                 // water / shoes
                isBrownDirtOrCliff -> 0                              // cliff edge / dirt border
                (mx - mn) < 90 && r >= g && g >= b && r > 120 -> 0   // sand / ground
                else -> 1
            }
        }

        fun comps(match: (Int) -> Boolean): List<Comp> {
            val seen = BooleanArray(gw * gh)
            val out = ArrayList<Comp>()
            val stack = IntArray(gw * gh)
            for (gy in gy0 until gy1) for (gx in 0 until gx1) {
                val i0 = gy * gw + gx
                if (seen[i0] || !match(code[i0])) continue
                val c = Comp(); var sp = 0
                stack[sp++] = i0; seen[i0] = true
                while (sp > 0) {
                    val i = stack[--sp]; val x = i % gw; val y = i / gw
                    c.count++; c.cells.add(i)
                    if (x < c.minX) c.minX = x; if (x > c.maxX) c.maxX = x
                    if (y < c.minY) c.minY = y; if (y > c.maxY) c.maxY = y
                    for (d in 0..3) {
                        val nx = x + (if (d == 0) 1 else if (d == 1) -1 else 0)
                        val ny = y + (if (d == 2) 1 else if (d == 3) -1 else 0)
                        if (nx < 0 || nx >= gx1 || ny < gy0 || ny >= gy1) continue
                        val ni = ny * gw + nx
                        if (!seen[ni] && match(code[ni])) { seen[ni] = true; stack[sp++] = ni }
                    }
                }
                out.add(c)
            }
            return out
        }
        fun cx(i: Int) = (i % gw) * step + step / 2f
        fun cy(i: Int) = (i / gw) * step + step / 2f
        val bigArea = 0.04 * w * h

        // big blue blobs = water (tracked in waterMask and ignored); small light-blue ones = shoes (items)
        val waterMask = BooleanArray(gw * gh)
        for (c in comps { it == 4 }) {
            val isWater = c.count * step.toDouble() * step > bigArea
            if (isWater) {
                for (i in c.cells) { code[i] = 0; waterMask[i] = true }
            } else {
                var shoePixels = 0
                for (i in c.cells) if (kind[i] == 3) shoePixels++
                val v = if (shoePixels > 0) 1 else 0
                for (i in c.cells) { code[i] = v; if (v == 1) kind[i] = 3 }
            }
        }

        // The hole sits at a fixed spot on screen (the map scrolls under it); only its size is measured.
        val hx = w * 0.5f
        val hy = h * 0.485f
        var hr = w * 0.10f
        val hole = comps { it == 2 }.maxByOrNull { it.count }
        if (hole != null && hole.count >= 6) {
            val r = Math.sqrt(hole.count.toDouble() * step * step / Math.PI).toFloat()
            hr = r.coerceIn(w * 0.06f, w * 0.17f)
        }

        // big white blobs (tutorial bubbles etc.) are blanked out
        for (wb in comps { it == 3 }.filter { it.count * step.toDouble() * step > 0.01 * w * h }) {
            for (gy in (wb.minY - 3).coerceAtLeast(gy0)..(wb.maxY + 3).coerceAtMost(gy1 - 1))
                for (gx in (wb.minX - 3).coerceAtLeast(0)..(wb.maxX + 3).coerceAtMost(gx1 - 1)) {
                    code[gy * gw + gx] = 0
                    kind[gy * gw + gx] = 0
                }
        }

        // only chase item types that are still listed in the top bar (goal counters)
        var needA = false; var needH = false; var needS = false
        for (slot in 0..3) {
            val sc = (0.10 + 0.175 * slot) * w
            var cA = 0; var cH = 0; var cS = 0
            var yy = (0.125 * h).toInt()
            while (yy < (0.17 * h).toInt()) {
                var xx = (sc - 0.035 * w).toInt()
                while (xx < (sc + 0.035 * w).toInt()) {
                    val p = bmp.getPixel(xx.coerceIn(0, w - 1), yy.coerceIn(0, h - 1))
                    val r = (p shr 16) and 255; val g = (p shr 8) and 255; val b = p and 255
                    if (r > 170 && g < 90 && b in 60..149) cH++
                    else if (r > 220 && g in 90..160 && b > 120) cA++
                    else if (r > 180 && g > 170 && b < 120) cA++
                    else if (r < 130 && g > 170 && b > 170) cS++
                    xx += 2
                }
                yy += 2
            }
            if (cH >= 150) needH = true
            if (cA >= 60) needA = true
            if (cS >= 100) needS = true
        }
        if (!needA && !needH && !needS) { needA = true; needH = true; needS = true }

        // Keep only item components that contain at least one needed item-family pixel (never chase unknown kind=0 blobs like cliff edges)
        val itemComps = comps { it == 1 }
        val validCells = ArrayList<Int>()
        var n = 0
        val minD = hr * 1.7f + step
        var bestD = Float.MAX_VALUE; var bx = 0f; var by = 0f
        for (c in itemComps) {
            if (c.count < 2) continue
            var hasNeededKind = false
            for (i in c.cells) {
                val k = kind[i]
                if ((k == 1 && needA) || (k == 2 && needH) || (k == 3 && needS)) {
                    hasNeededKind = true
                    break
                }
            }
            if (!hasNeededKind) continue

            var seenOne = false
            for (i in c.cells) {
                val ix = i % gw; val iy = i / gw
                // Skip pixels right on top of water border
                var nearWater = false
                for (wy in (iy - 1).coerceAtLeast(gy0)..(iy + 1).coerceAtMost(gy1 - 1)) {
                    for (wx in (ix - 1).coerceAtLeast(0)..(ix + 1).coerceAtMost(gx1 - 1)) {
                        if (waterMask[wy * gw + wx]) { nearWater = true; break }
                    }
                    if (nearWater) break
                }
                if (nearWater) continue

                val px = cx(i); val py = cy(i)
                val d = Math.hypot((px - hx).toDouble(), (py - hy).toDouble()).toFloat()
                if (d < minD) continue
                val ddx = Math.abs(px - hx); val ddy = py - hy
                if (ddx < 0.10f * w && ddy > -0.26f * w && ddy < -0.11f * w) continue   // "Lv.N" label
                if (ddx < 0.17f * w && ddy > 0.12f * w && ddy < 0.27f * w) continue     // "POWER" label
                seenOne = true
                validCells.add(i)
                if (d < bestD) { bestD = d; bx = px; by = py }
            }
            if (seenOne) n++
        }

        // Compute steering direction and avoid pushing straight into water
        var dx: Float
        var dy: Float
        if (bestD == Float.MAX_VALUE) {
            dx = 0f; dy = -1f
        } else {
            dx = (bx - hx) / bestD
            dy = (by - hy) / bestD
        }

        fun hitsWater(vx: Float, vy: Float): Boolean {
            var waterHits = 0
            for (m in 1..3) {
                val sx = ((hx + vx * hr * (1.1f + 0.45f * m)) / step).toInt().coerceIn(0, gx1 - 1)
                val sy = ((hy + vy * hr * (1.1f + 0.45f * m)) / step).toInt().coerceIn(gy0, gy1 - 1)
                if (waterMask[sy * gw + sx]) waterHits++
            }
            return waterHits >= 2
        }

        if (hitsWater(dx, dy)) {
            // Try 8 candidate directions and pick the dry one best aligned with (dx, dy)
            var bestScore = -Float.MAX_VALUE
            var sdx = dx; var sdy = dy
            for (a in 0 until 8) {
                val ang = a * Math.PI / 4.0
                val cxv = Math.cos(ang).toFloat()
                val cyv = Math.sin(ang).toFloat()
                if (!hitsWater(cxv, cyv)) {
                    val score = cxv * dx + cyv * dy
                    if (score > bestScore) { bestScore = score; sdx = cxv; sdy = cyv }
                }
            }
            dx = sdx; dy = sdy
        }

        return Plan(dx, dy, n, hx, hy)
    }
}
