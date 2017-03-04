package com.example.vlad.graph.renderer;

import android.graphics.Canvas;

public interface WaveformRenderer {
    void render(Canvas canvas, byte[] waveform);
}
