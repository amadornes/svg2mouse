package com.amadornes.svg2mouse;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Main {

    private static final Pattern REGEX = Pattern.compile("<path[^>]*\sd=\"([^\"]+)\".*>");

    public static void main(String[] args) throws Exception {
        var scanner = new Scanner(System.in);

        var complexShape = promptLoadSVG(scanner);

        var area = promptDrawArea(scanner);
        var transform = createTransform(area);

        promptCountdown(scanner);

        var controller = new MouseController();
        var success = controller.draw(complexShape, transform);

        if (success) {
            System.out.println("Done!");
        } else {
            System.out.println("Aborted!");
        }
    }

    private static ComplexShape promptLoadSVG(Scanner scanner) throws IOException {
        System.out.println("Make sure your file is called \"image.svg\" and is located in the current directory.");
        System.out.println("Only paths are supported, not text, images or other SVG elements.");
        System.out.println("Please convert your file to paths if you have unsupported elements.");
        System.out.println("Press enter to continue.");
        scanner.nextLine();

        var svg = Files.readString(Path.of("image.svg"));
        var matcher = REGEX.matcher(svg);

        var complexShape = new ComplexShape();
        matcher.results().forEach(result -> {
            complexShape.parsePath(result.group(1));
        });
        return complexShape;
    }

    private static Rectangle2D.Float promptDrawArea(Scanner scanner) {
        System.out.println("Place your cursor in the top left corner and press enter to continue.");
        scanner.nextLine();
        var topLeft = MouseInfo.getPointerInfo().getLocation();

        System.out.println("Place your cursor in the bottom right corner and press enter to continue.");
        scanner.nextLine();
        var bottomRight = MouseInfo.getPointerInfo().getLocation();

        var area = new Rectangle2D.Float(topLeft.x, topLeft.y, 0, 0);
        area.add(bottomRight);
        return area;
    }

    private static AffineTransform createTransform(Rectangle2D.Float area) {
        var minSize = Math.min(area.width, area.height);
        var corner = new Point2D.Float(area.x + (area.width - minSize) / 2, area.y + (area.height - minSize) / 2);

        var transform = AffineTransform.getScaleInstance(minSize, minSize);
        transform.preConcatenate(AffineTransform.getTranslateInstance(corner.x, corner.y));
        return transform;
    }

    private static void promptCountdown(Scanner scanner) throws InterruptedException {
        System.out.println("Press enter, then click the window you want to draw onto, and the program will start after the countdown.");
        System.out.println("Moving your mouse will cancel the process.");
        scanner.nextLine();

        for (int i = 5; i > 0; i--) {
            System.out.println(i);
            Thread.sleep(1000);
        }
    }

}
