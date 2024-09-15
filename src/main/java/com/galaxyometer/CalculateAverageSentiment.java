// src/main/java/com/galaxyometer/CalculateAverageSentiment.java

package com.galaxyometer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class CalculateAverageSentiment {

    public static void main(String[] args) {
        try {
            // Read sentiment results from sentiment_results.json
            JSONObject sentimentResults = readSentimentResultsFromFile("sentiment_results.json"); // Reading from root
            JSONObject averageResults = calculateAverageSentiments(sentimentResults);
            writeAveragesToJsonFile(averageResults);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    private static JSONObject readSentimentResultsFromFile(String filePath) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        FileReader reader = new FileReader(filePath);
        JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
        reader.close();
        return jsonObject;
    }

    private static JSONObject calculateAverageSentiments(JSONObject sentimentResults) {
        JSONObject averageResults = new JSONObject();

        // Iterate through each show
        for (Object key : sentimentResults.keySet()) {
            String showTitle = (String) key;
            JSONArray sentiments = (JSONArray) sentimentResults.get(showTitle);

            double totalNeutral = 0;
            double totalPositive = 0;
            double totalNegative = 0;
            int positiveCount = 0;
            int neutralCount = 0;
            int negativeCount = 0;

            // Calculate totals and counts
            for (Object sentimentObj : sentiments) {
                JSONObject sentiment = (JSONObject) sentimentObj;
                double neutral = (double) sentiment.get("Neutral");
                double positive = (double) sentiment.get("Positive");
                double negative = (double) sentiment.get("Negative");
                String sentimentLabel = (String) sentiment.get("Sentiment");

                totalNeutral += neutral;
                totalPositive += positive;
                totalNegative += negative;

                // Count sentiments
                switch (sentimentLabel.toLowerCase()) {
                    case "positive":
                        positiveCount++;
                        break;
                    case "neutral":
                        neutralCount++;
                        break;
                    case "negative":
                        negativeCount++;
                        break;
                }
            }

            // Calculate averages
            int totalCount = sentiments.size();
            JSONObject averageSentiment = new JSONObject();
            averageSentiment.put("AverageNeutral", totalNeutral / totalCount);
            averageSentiment.put("AveragePositive", totalPositive / totalCount);
            averageSentiment.put("AverageNegative", totalNegative / totalCount);
            averageSentiment.put("CountPositive", positiveCount);
            averageSentiment.put("CountNeutral", neutralCount);
            averageSentiment.put("CountNegative", negativeCount);

            // Store averages for the current show
            averageResults.put(showTitle, averageSentiment);
        }

        return averageResults;
    }

    private static void writeAveragesToJsonFile(JSONObject averageResults) {
        try (FileWriter file = new FileWriter("averageSentiment.json")) {
            file.write(averageResults.toJSONString());
            file.flush();
            System.out.println("Averaged sentiment results saved to averageSentiment.json");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
