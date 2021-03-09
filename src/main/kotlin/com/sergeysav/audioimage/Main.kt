package com.sergeysav.audioimage

import SimpleAudioConversion
import java.io.BufferedInputStream
import java.nio.file.FileSystems
import java.nio.file.Files
import java.util.Arrays
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.swing.JFrame
import kotlin.io.path.exists

fun main(args: Array<String>) {
    // Check Parameters
    if (args.size !in 1..2) {
        println("Incorrect parameters. There must be either 1 or 2 parameters.")
        println("file.jar <path to WAV file> [optional: seconds to delay]")
        return
    }

    // Extract Parameters
    val filePath = args[0]
    val delay = args.getOrNull(1)?.let(String::toDoubleOrNull) ?: 1.0

    // Print Startup statement
    println("AudioImage running on file: $filePath")
    println("  with a $delay second delay")

    // Get handle to file and check that it exists
    val input = FileSystems.getDefault().getPath(args[0])
    if (!Files.exists(input)) {
        System.err.println("ERROR: File $input not found")
        return
    }

    // Extract the audio file stuffs
    val samples: FloatArray
    val clip: Clip
    val frequency: Float
    val numSamples = Files.newInputStream(input).use {
        // Read the audio from the file
        val audio = AudioSystem.getAudioInputStream(BufferedInputStream(it))

        // Get the audio as an audio clip as well (so that it can be played)
        clip = AudioSystem.getClip()
        clip.open(AudioSystem.getAudioInputStream(input.toFile()))

        // Audio Samples Data and Audio Bytes
        val bytes = ByteArray(audio.frameLength.toInt() * 4)
        samples = FloatArray(audio.frameLength.toInt() * 2)

        // Extract WAV File Frequency
        frequency = audio.format.sampleRate

        // Perform the conversion using the file I found on the internet
        SimpleAudioConversion.decode(bytes, samples, audio.read(bytes), audio.format)
    }

    // This should be true because there are two samples per frame (left and right audio channel)
    assert(samples.size == numSamples)

    // Create the frame
    val jFrame = JFrame("AudioImage")

    // Add panel to frame
    jFrame.add(RenderPanel(samples, clip, delay, frequency))

    // Setup frame stuffs
    jFrame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    jFrame.setSize(1080, 1160)
    jFrame.isVisible = true

    // Create refresh thread (60fps)
    Thread({
        while (true) {
            jFrame.repaint()
            Thread.sleep(1000/60)
        }
    }, "Render").apply {
        this.isDaemon = true
        start()
    }
}
