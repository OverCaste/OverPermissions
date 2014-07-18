package com.overmc.nodedisplayapi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Preconditions;

public final class ElementBox { // This class is mostly magic.
    public static final String WHITESPACE = "   "; // 10 'lines' is equal to 15 normal characters.

    private final NodeDisplayCharset charset;
    private final int width;
    private final List<ElementBoxNode> nodes;
    private final List<String> header;

    public ElementBox(int width, NodeDisplayCharset charset, List<ElementBoxNode> nodes, List<String> header) {
        Preconditions.checkArgument(width > 10, "width must be greater than 10");
        this.nodes = new ArrayList<>(nodes);
        this.width = width;
        this.charset = charset;
        this.header = header;
    }

    public List<String> write( ) {
        ArrayList<String> ret = new ArrayList<String>();
        ret.addAll(createHeader());
        ret.addAll(createBody());
        return ret;
    }

    private List<String> createHeader( ) {
        ArrayList<String> ret = new ArrayList<>(header.size());
        for (int i = 0; i < header.size(); i++) {
            StringBuilder builder = new StringBuilder();
            //builder.append(charset.borderVertical);
            builder.append(header.get(i));
            ret.add(builder.toString());
        }
        return ret;
    }

    private void writeNode(ElementBoxNode node, List<StringBuilder> builders) {
        int subnodeCount = node.getSubNodes().size();
        int lineCount = node.getLines().size();
        int lineNumber = 0;
        if (node.getName() != null) {
            StringBuilder currentBuilder = builders.get(lineNumber);
            if (subnodeCount > 0 || lineCount > 0) {
                currentBuilder.append(charset.altHorizontalDown);
            } else {
                currentBuilder.append(charset.altHorizontal);
            }
            currentBuilder.append(WHITESPACE);
            currentBuilder.append(node.getName());
            lineNumber += 1;
        }
        int lineIndex = 0;
        for (String line : node.getLines()) {
            if (lineIndex == lineCount - 1 && subnodeCount == 0) { // Last element
                if(node.getName() == null) {
                    builders.get(lineNumber++).append(charset.altHorizontal).append(WHITESPACE).append(line); //Weird little edge case, there'd be no 'first' node here.
                } else {
                    builders.get(lineNumber++).append(charset.altBottomLeftCorner).append(WHITESPACE).append(line);
                }
            } else {
                builders.get(lineNumber++).append(charset.altVerticalRight).append(WHITESPACE).append(line);
            }
            lineIndex++;
        }
        int subnodeIndex = 0;
        for (ElementBoxNode subnode : node.getSubNodes()) {
            if (subnodeIndex == subnodeCount - 1) { // Last element
                builders.get(lineNumber++).append(charset.altBottomLeftCorner).append(charset.altHorizontal);
                for (int i = 1; i < subnode.size(); i++) { // No trailing line for the last element, ignore it.
                    builders.get(lineNumber++).append(WHITESPACE).append(WHITESPACE);
                }
            } else {
                builders.get(lineNumber++).append(charset.altVerticalRight).append(charset.altHorizontal);
                for (int i = 1; i < subnode.size(); i++) { // No trailing line for the last element, ignore it.
                    builders.get(lineNumber++).append(charset.altVertical).append(WHITESPACE);
                }
            }
            subnodeIndex++;
        }
        int subnodeWriterStart = node.getName() == null ? 0 : 1;
        for (ElementBoxNode subnode : node.getSubNodes()) {
            writeNode(subnode, builders.subList(subnodeWriterStart, subnodeWriterStart + subnode.size()));
            subnodeWriterStart += subnode.size();
        }
    }

    private List<String> createBody( ) {
        ArrayList<StringBuilder> builders = new ArrayList<>();
        int currentIndex = 0;
        for (ElementBoxNode node : nodes) {
            if (node.size() != 0) {
                StringBuilder builder = new StringBuilder(width);
                //builder.append(charset.borderAltLeftFork);
                builders.add(builder);
                for (int i = 1; i < node.size(); i++) {
                    builder = new StringBuilder(width);
                    //builder.append(charset.borderVertical);
                    builders.add(builder);
                }
            }
            int size = node.size();
            writeNode(node, builders.subList(currentIndex, currentIndex + size));
            currentIndex += size;
        }
        List<String> ret = new ArrayList<String>(builders.size());
        for (StringBuilder builder : builders) {
            ret.add(builder.toString());
        }
        return ret;
    }

    public static void main(String[] args) { // The way to create these.
        List<ElementBoxNode> groupInformationNodes = Arrays.asList(
                new ElementBoxNode("TestGroup:", null, Arrays.asList("+ added")),
                new ElementBoxNode("OpGroup:", null, Arrays.asList("- removed (-)")),
                new ElementBoxNode("SuperOpGroup:", null, Arrays.asList("+ added (+)"))
                );
        ElementBoxNode groupInformation = new ElementBoxNode("Group information:", groupInformationNodes, null);
        List<ElementBoxNode> userInformationNodes = Arrays.asList(
                new ElementBoxNode(null, null, Arrays.asList("+ added")),
                new ElementBoxNode(null, null, Arrays.asList("- removed (-)"))
                );
        ElementBoxNode userInformation = new ElementBoxNode("User information:", userInformationNodes, null);

        ElementBox box = new ElementBox(16, new BoxLargeAndSmallCharset(), Arrays.asList(groupInformation, userInformation), Arrays.asList("Permission 'wacosbase.mynode'"));
        for (String s : box.write()) {
            System.out.println(s);
        }
    }
}
