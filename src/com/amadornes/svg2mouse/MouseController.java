package com.amadornes.svg2mouse;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;

public class MouseController {

    private static final int SUBDIVISIONS = 10;
    private static final int DELAY = 20;

    private final Robot robot;
    private boolean pressed;
    private int mouseX, mouseY;

    public MouseController() {
        try {
            this.robot = new Robot();
            this.robot.setAutoDelay(DELAY);
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean draw(ComplexShape complexShape, AffineTransform transform) {
        var bounds = complexShape.getBounds();
        var maxSize = Math.max(bounds.getWidth(), bounds.getHeight());
        var totalTransform = AffineTransform.getTranslateInstance(-bounds.getX(), -bounds.getY());
        totalTransform.preConcatenate(AffineTransform.getScaleInstance(1 / maxSize, 1 / maxSize));
        totalTransform.preConcatenate(transform);

        var location = MouseInfo.getPointerInfo().getLocation();
        mouseX = location.x;
        mouseY = location.y;

        for (var shape : complexShape.getShapes()) {
            var it = shape.getPathIterator(totalTransform);
            var points = new float[6];
            var start = new float[2];
            var prev = new float[2];
            while (!it.isDone()) {
                if (abortIfNecessary()) return false;

                var type = it.currentSegment(points);
                if (type == PathIterator.SEG_MOVETO) {
                    // Move without drawing and store as starting point
                    release();
                    moveTo(points[0], points[1]);
                    start[0] = points[0];
                    start[1] = points[1];
                } else if (type == PathIterator.SEG_LINETO) {
                    // Draw a line to the next point
                    press();
                    moveTo(points[0], points[1]);
                } else if (type == PathIterator.SEG_CLOSE) {
                    // Close a shape by drawing a line back to the start
                    press();
                    moveTo(start[0], start[1]);
                } else if (type == PathIterator.SEG_QUADTO) {
                    // Quadratic bezier
                    press();
                    for (int i = 1; i <= SUBDIVISIONS; i++) {
                        var interpolated = interpolateQuad(
                                prev[0], prev[1],
                                points[0], points[1],
                                points[2], points[3],
                                i / (float) SUBDIVISIONS
                        );
                        moveTo(interpolated[0], interpolated[1]);
                        if (abortIfNecessary()) return false;
                    }
                } else if (type == PathIterator.SEG_CUBICTO) {
                    // Cubic bezier
                    press();
                    for (int i = 1; i <= SUBDIVISIONS; i++) {
                        var interpolated = interpolateCubic(
                                prev[0], prev[1],
                                points[0], points[1],
                                points[2], points[3],
                                points[4], points[5],
                                i / (float) SUBDIVISIONS
                        );
                        moveTo(interpolated[0], interpolated[1]);
                        if (abortIfNecessary()) return false;
                    }
                }

                if (type == PathIterator.SEG_CLOSE) {
                    prev[0] = start[0];
                    prev[1] = start[1];
                } else if (type == PathIterator.SEG_QUADTO) {
                    prev[0] = points[2];
                    prev[1] = points[3];
                } else if (type == PathIterator.SEG_CUBICTO) {
                    prev[0] = points[4];
                    prev[1] = points[5];
                } else {
                    prev[0] = points[0];
                    prev[1] = points[1];
                }

                it.next();
            }
            release();
        }

        return true;
    }

    private boolean abortIfNecessary() {
        var location = MouseInfo.getPointerInfo().getLocation();
        var abort = location.x != mouseX || location.y != mouseY;
        if (abort) {
            release();
        }
        return abort;
    }

    private void release() {
        if (pressed) {
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            pressed = false;
        }
    }

    private void press() {
        if (!pressed) {
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            pressed = true;
        }
    }

    private void moveTo(float x, float y) {
        robot.mouseMove(mouseX = (int) x, mouseY = (int) y);
    }

    private float[] interpolateCubic(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, float t) {
        var int1 = interpolate(x1, y1, x2, y2, t);
        var int2 = interpolate(x2, y2, x3, y3, t);
        var int3 = interpolate(x3, y3, x4, y4, t);

        var int4 = interpolate(int1, int2, t);
        var int5 = interpolate(int2, int3, t);

        return interpolate(int4, int5, t);
    }

    private float[] interpolateQuad(float x1, float y1, float x2, float y2, float x3, float y3, float t) {
        var int1 = interpolate(x1, y1, x2, y2, t);
        var int2 = interpolate(x2, y2, x3, y3, t);
        return interpolate(int1, int2, t);
    }

    private float[] interpolate(float[] a, float[] b, float t) {
        return interpolate(a[0], a[1], b[0], b[1], t);
    }

    private float[] interpolate(float x1, float y1, float x2, float y2, float t) {
        return new float[] {
                x1 + (x2 - x1) * t,
                y1 + (y2 - y1) * t
        };
    }

}
