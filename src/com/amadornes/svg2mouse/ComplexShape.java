package com.amadornes.svg2mouse;

import org.apache.batik.parser.AWTPathProducer;
import org.apache.batik.parser.PathParser;

import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;


public class ComplexShape {

    private final PathParser parser;
    private final AWTPathProducer producer;
    private final List<Shape> shapes = new ArrayList<>();

    public ComplexShape() {
        this.parser = new PathParser();
        this.producer = new AWTPathProducer();
        parser.setPathHandler(producer);
    }

    public void parsePath(String path) {
        parser.parse(path);
        shapes.add(producer.getShape());
    }

    public List<Shape> getShapes() {
        return shapes;
    }

    public Rectangle2D getBounds() {
        Rectangle2D rect = null;
        for (var shape : shapes) {
            if (rect == null) {
                rect = shape.getBounds2D();
                continue;
            }
            rect.add(shape.getBounds2D());
        }
        return rect;
    }

}
