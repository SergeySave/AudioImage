package com.sergeysav.audioimage

import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import javax.sound.sampled.Clip
import javax.swing.JPanel
import kotlin.math.roundToInt
import kotlin.math.sqrt

// Basing this on 16-bit audio (so these are the 16-bit integer (Short) based constants)
const val MIN = -32768
const val MAX = 32767
const val WIDTH_G = 65535

class RenderPanel(private val samples: FloatArray, private val clip: Clip, private val delay: Double, private val frequency: Float) : JPanel() {
    private var sample = 0 // The current sample (time can be calculated from this)
    private var first = true // Is this the first data point? Necessary because a line requires two points
    private var lastX = 0 // The last drawn X position
    private var lastY = 0 // The last drawn Y position
    private var firstFrame = true // Is this the first frame? Used to initialize the time-based rendering
    private var firstFrameTime = 0L // The time of the first frame - used to determine how many samples should be rendered

    override fun paint(g: Graphics?) {
        super.paint(g)

        val frameTime = System.currentTimeMillis() // Get the current time
        if (firstFrame) { // If it is the first frame initialize the base time
            firstFrameTime = frameTime + (delay * 1000).roundToInt()
            firstFrame = false
            return
        }

        // Get graphics2D
        val graphics2D = g as? Graphics2D ?: return

        // Clear the screen with black
        graphics2D.color = Color.BLACK
        graphics2D.fillRect(0, 0, this.width, this.height)

        // Set up the transform (which uses 16-bit scale) and rotated axes
        val transform = AffineTransform()
        transform.translate(this.width / 2.0, this.height / 2.0)
        val minSize = minOf(this.width, this.height)
        transform.scale(minSize / (sqrt(2.0) * WIDTH_G), minSize / (sqrt(2.0) * WIDTH_G))
        transform.rotate(Math.toRadians(-45.0))
        graphics2D.transform(transform)

        // Draw the boundary rectangle
        graphics2D.color = Color.DARK_GRAY
        graphics2D.drawRect(MIN, MIN, WIDTH_G, WIDTH_G)

        // Set line drawing stuffs
        graphics2D.color = Color.WHITE

        // Figure out the time of the final frame to render
        val finalT = (frameTime - firstFrameTime) / 1000.0

        // Loop for the samples which need to be displayed
        while (sample / frequency < finalT && sample * 2 <= samples.size + 2) {
            // Get the X and Y positions
            val x = (samples.getOrElse(sample * 2 + 1) { 0f } * MAX).roundToInt()
            val y = (samples.getOrElse(sample * 2) { 0f } * MAX).roundToInt()

            if (first) {
                first = false
                // If this is the first drawn frame we cannot draw the line (as we do not have the last position)
                // However, we do want to start the audio to play
                clip.start()
            } else {
                // If we have a previous x and y, draw a line to the new x and new y
                graphics2D.drawLine(lastX, lastY, x, y)
            }

            // Remember the new x and y and increment current sample number
            lastX = x
            lastY = y
            sample++
        }
    }
}