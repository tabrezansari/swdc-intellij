import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;

public class Deployer {

    private static String codeTimeName = "Code Time";
    private static String musicTimeName = "Music Time";

    private static String codeTimeVersion = "0.4.4";
    private static String musicTimeVersion = "1.0.0";

    private static String codeTimeDesc = "\n" +
            "<h1 id=\"codetimeforintellij\">Code Time for IntelliJ</h1>\n\n" +

            "<blockquote>\n" +
            "<p><strong>Code Time</strong> is an open source plugin that provides programming metrics right in your code editor.</p>\n" +
            "</blockquote>\n\n" +

            "<p align=\"center\" style=\"margin: 0 10%\">\n" +
            "<img src=\"https://raw.githubusercontent.com/swdotcom/swdc-intellij/master/resources/assets/intellij-dashboard.gif\" alt=\"Code Time for IntelliJ\" />\n" +
            "</p>\n\n" +

            "<h2 id=\"powerupyourdevelopment\">Power up your development</h2>\n\n" +

            "<p><strong>In-editor dashboard</strong>\n" +
            "Get daily and weekly reports of your programming activity right in your code editor.</p>\n\n" +

            "<p><strong>Status bar metrics</strong>\n" +
            "After installing our plugin, your status bar will show real-time metrics about time coded per day.</p>\n\n" +

            "<p><strong>Weekly email reports</strong>\n" +
            "Get a weekly report delivered right to your email inbox.</p>\n\n" +

            "<p><strong>Data visualizations</strong>\n" +
            "Go to our web app to get simple data visualizations, such as a rolling heatmap of your best programming times by hour of the day.</p>\n" +

            "<p><strong>Calendar integration</strong>\n" +
            "Integrate with Google Calendar to automatically set calendar events to protect your best programming times from meetings and interrupts.</p>\n\n" +

            "<p><strong>More stats</strong>\n" +
            "See your best time for coding and the speed, frequency, and top files across your commits.</p>\n\n" +

            "<h2 id=\"whyyoushouldtryitout\">Why you should try it out</h2>\n\n" +

            "<ul>\n" +
            "<li>Automatic time reports by project</li>\n\n" +

            "<li>See what time you code your best—find your \"flow\"</li>\n\n" +

            "<li>Defend your best code times against meetings and interrupts</li>\n\n" +

            "<li>Find out what you can learn from your data</li>\n" +
            "</ul>\n\n" +

            "<h2 id=\"itssafesecureandfree\">It’s safe, secure, and free</h2>\n\n" +

            "<p><strong>We never access your code</strong>\n" +
            "We do not process, send, or store your proprietary code. We only provide metrics about programming, and we make it easy to see the data we collect.</p>\n\n" +

            "<p><strong>Your data is private</strong>\n" +
            "We will never share your individually identifiable data with your boss. In the future, we will roll up data into groups and teams but we will keep your data anonymized.</p>\n\n" +

            "<p><strong>Free for you, forever</strong>\n" +
            "We provide 90 days of data history for free, forever. In the future, we will provide premium plans for advanced features and historical data access.</p>\n";

    private static String musicTimeDesc = "\n" +
            "<h1 id=\"musictimeforintellij\">Music Time for IntelliJ</h1>\n\n" +

            "<blockquote>\n" +
            "<p><strong>Music Time</strong> is an open source plugin that provides music listening metrics right in your code editor.</p>\n" +
            "</blockquote>\n\n" +

            "<p align=\"center\" style=\"margin: 0 10%\">\n" +
            "<img src=\"https://raw.githubusercontent.com/swdotcom/swdc-intellij/master/resources/assets/intellij-dashboard.gif\" alt=\"Code Time for IntelliJ\" />\n" +
            "</p>\n\n" +

            "<h2 id=\"powerupyourdevelopment\">Power up your development</h2>\n\n" +

            "<p><strong>In-editor dashboard</strong>\n" +
            "Get daily and weekly reports of your music listening activity right in your code editor.</p>\n\n" +

            "<p><strong>Status bar metrics</strong>\n" +
            "After installing our plugin, your status bar will show real-time metrics about time music listen per day.</p>\n\n" +

            "<p><strong>Weekly email reports</strong>\n" +
            "Get a weekly report delivered right to your email inbox.</p>\n\n" +

            "<p><strong>Data visualizations</strong>\n" +
            "Go to our web app to get simple data visualizations, such as a rolling heatmap of your best programming times by hour of the day.</p>\n" +

            "<p><strong>Calendar integration</strong>\n" +
            "Integrate with Google Calendar to automatically set calendar events to protect your best programming times from meetings and interrupts.</p>\n\n" +

            "<p><strong>More stats</strong>\n" +
            "See your best time for coding and the speed, frequency, and top files across your commits.</p>\n\n" +

            "<h2 id=\"whyyoushouldtryitout\">Why you should try it out</h2>\n\n" +

            "<ul>\n" +
            "<li>Automatic time reports by project</li>\n\n" +

            "<li>See what time you listen music your best—find your \"flow\"</li>\n\n" +

            "<li>Defend your best music times against meetings and interrupts</li>\n\n" +

            "<li>Find out what you can learn from your data</li>\n" +
            "</ul>\n\n" +

            "<h2 id=\"itssafesecureandfree\">It’s safe, secure, and free</h2>\n\n" +

            "<p><strong>We never access your code</strong>\n" +
            "We do not process, send, or store your proprietary code. We only provide metrics about music listening, and we make it easy to see the data we collect.</p>\n\n" +

            "<p><strong>Your data is private</strong>\n" +
            "We will never share your individually identifiable data with your boss. In the future, we will roll up data into groups and teams but we will keep your data anonymized.</p>\n\n" +

            "<p><strong>Free for you, forever</strong>\n" +
            "We provide 90 days of data history for free, forever. In the future, we will provide premium plans for advanced features and historical data access.</p>\n";

    public static void main(String[] args) {
        String parameter = args[0];

        String xmlFilePath = System.getProperty("user.dir") + "\\resources\\META-INF\\plugin.xml";

        try {

            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

            Document document = documentBuilder.parse(xmlFilePath);
            if(parameter.equals("code-time")) {

                Node name = document.getElementsByTagName("name").item(0);
                name.setTextContent(codeTimeName);

                Node version = document.getElementsByTagName("version").item(0);
                version.setTextContent(codeTimeVersion);

                // Get description by tag name
                Node description = document.getElementsByTagName("description").item(0);
                description.setTextContent("");
                CDATASection cdata = document.createCDATASection(codeTimeDesc);
                description.appendChild(cdata);



                // write the DOM object to the file
                TransformerFactory transformerFactory = TransformerFactory.newInstance();

                Transformer transformer = transformerFactory.newTransformer();

                DOMSource domSource = new DOMSource(document);

                StreamResult streamResult = new StreamResult(new File(xmlFilePath));
                transformer.transform(domSource, streamResult);

                System.out.println("The XML File Updated");

                FileReader fr = null;
                FileWriter fw = null;
                try {
                    fr = new FileReader(System.getProperty("user.dir") + "\\codetime.readme.md");
                    fw = new FileWriter(System.getProperty("user.dir") + "\\README.md");
                    int c = fr.read();
                    while(c!=-1) {
                        fw.write(c);
                        c = fr.read();
                    }
                    System.out.println("The README.md File Updated");
                } catch(IOException e) {
                    e.printStackTrace();
                } finally {
                    fr.close();
                    fw.close();
                }
            } else if (parameter.equals("music-time")) {

                Node name = document.getElementsByTagName("name").item(0);
                name.setTextContent(musicTimeName);

                Node version = document.getElementsByTagName("version").item(0);
                version.setTextContent(musicTimeVersion);

                // Get description by tag name
                Node description = document.getElementsByTagName("description").item(0);
                description.setTextContent("");
                CDATASection cdata = document.createCDATASection(musicTimeDesc);
                description.appendChild(cdata);



                // write the DOM object to the file
                TransformerFactory transformerFactory = TransformerFactory.newInstance();

                Transformer transformer = transformerFactory.newTransformer();

                DOMSource domSource = new DOMSource(document);

                StreamResult streamResult = new StreamResult(new File(xmlFilePath));
                transformer.transform(domSource, streamResult);

                System.out.println("The XML File Updated");

                FileReader fr = null;
                FileWriter fw = null;
                try {
                    fr = new FileReader(System.getProperty("user.dir") + "\\musictime.readme.md");
                    fw = new FileWriter(System.getProperty("user.dir") + "\\README.md");
                    int c = fr.read();
                    while(c!=-1) {
                        fw.write(c);
                        c = fr.read();
                    }
                    System.out.println("The README.md File Updated");
                } catch(IOException e) {
                    e.printStackTrace();
                } finally {
                    fr.close();
                    fw.close();
                }
            } else {
                System.out.println("ERROR: Unable to update");
                System.out.println("Usage: java Deployer <code-time|music-time>");
            }

        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (TransformerException tfe) {
            tfe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (SAXException sae) {
            sae.printStackTrace();
        }
    }
}
