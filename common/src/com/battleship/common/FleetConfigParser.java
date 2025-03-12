package com.battleship.common;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.io.*;
import java.util.*;

public class FleetConfigParser {
    public static List<Ship> parseFleetConfig(String xmlFile) throws Exception {
        List<Ship> fleet = new ArrayList<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(xmlFile));
        NodeList shipNodes = doc.getElementsByTagName("ship");
        for (int i = 0; i < shipNodes.getLength(); i++) {
            Element element = (Element) shipNodes.item(i);
            int type = Integer.parseInt(element.getAttribute("type"));
            int x = Integer.parseInt(element.getAttribute("x"));
            int y = Integer.parseInt(element.getAttribute("y"));
            String orientation = element.getAttribute("orientation");
            fleet.add(new Ship(type, x, y, orientation));
        }
        return fleet;
    }
}

