// src/main/java/com/galaxyometer/SentimentAnalysis.java

package com.galaxyometer;

import com.azure.ai.textanalytics.TextAnalyticsClient;
import com.azure.ai.textanalytics.TextAnalyticsClientBuilder;
import com.azure.ai.textanalytics.models.AnalyzeSentimentResult;
import com.azure.ai.textanalytics.models.AnalyzeSentimentOptions;
import com.azure.ai.textanalytics.models.DocumentSentiment;
import com.azure.ai.textanalytics.models.TextAnalyticsError; // Use TextAnalyticsError, not TextAnalyticsException
import com.azure.core.credential.AzureKeyCredential;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileWriter;  // Import for writing to files
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger; // Import AtomicInteger

public class SentimentAnalysis {

    private static final String API_KEY = System.getenv("AZURE_API_KEY");
    private static final String ENDPOINT = System.getenv("AZURE_ENDPOINT");

    public static void main(String[] args) {
        try {
            JSONObject tweetsByShow = readTweetsFromFile("src/main/resources/tweets.json");
            JSONObject sentimentResults = analyzeSentiment(tweetsByShow);
            writeResultsToJsonFile(sentimentResults);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    private static JSONObject readTweetsFromFile(String filePath) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        FileReader reader = new FileReader(filePath);
        JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
        reader.close();

        // Organize tweets by show title in a JSON Object
        JSONObject tweetsByShow = new JSONObject();
        tweetsByShow.put("The Mandalorian", extractTweetText((JSONArray) jsonObject.get("The Mandalorian")));
        tweetsByShow.put("The Acolyte", extractTweetText((JSONArray) jsonObject.get("The Acolyte")));
        tweetsByShow.put("Andor", extractTweetText((JSONArray) jsonObject.get("Andor")));

        return tweetsByShow;
    }

    private static JSONArray extractTweetText(JSONArray tweets) {
        JSONArray cleanedTweets = new JSONArray();
        for (Object tweet : tweets) {
            String fullText = (String) tweet;
            // Extract the text after the second newline
            String[] parts = fullText.split("\n", 3);
            if (parts.length == 3) {
                cleanedTweets.add(parts[2].trim());
            }
        }
        return cleanedTweets;
    }

    private static JSONObject analyzeSentiment(JSONObject tweetsByShow) {
        // Initialize Azure Text Analytics Client
        TextAnalyticsClient client = new TextAnalyticsClientBuilder()
                .credential(new AzureKeyCredential(API_KEY))
                .endpoint(ENDPOINT)
                .buildClient();

        // JSON Object to store results organized by show title
        JSONObject resultsByShow = new JSONObject();

        // Iterate over each show and analyze sentiment for its tweets
        for (Object key : tweetsByShow.keySet()) {
            String showTitle = (String) key;
            JSONArray tweets = (JSONArray) tweetsByShow.get(showTitle);

            // JSON array to store results for the current show
            JSONArray resultsArray = new JSONArray();
            
            // Prepare documents for batch analysis
            List<String> documents = new ArrayList<>();
            for (Object tweet : tweets) {
                documents.add((String) tweet);
            }

            // Split documents into batches of 10
            int batchSize = 10;
            for (int i = 0; i < documents.size(); i += batchSize) {
                List<String> batch = documents.subList(i, Math.min(i + batchSize, documents.size()));

                // Analyze sentiment in batch
                AnalyzeSentimentOptions options = new AnalyzeSentimentOptions().setIncludeStatistics(true);
                
                // Initialize a counter to keep track of the tweet index within the batch
                AtomicInteger tweetIndex = new AtomicInteger(0);
                
                client.analyzeSentimentBatch(batch, "en", options).forEach(result -> {
                    if (!result.isError()) {
                        DocumentSentiment sentiment = result.getDocumentSentiment();
                        JSONObject sentimentResult = new JSONObject();  // JSON object to store each result
                        sentimentResult.put("Sentiment", sentiment.getSentiment().toString());
                        sentimentResult.put("Positive", sentiment.getConfidenceScores().getPositive());
                        sentimentResult.put("Neutral", sentiment.getConfidenceScores().getNeutral());
                        sentimentResult.put("Negative", sentiment.getConfidenceScores().getNegative());

                        // Add sentiment result to JSON array
                        resultsArray.add(sentimentResult);

                        // Print snippet of tweet and its sentiment to the console
                        int currentTweetIndex = tweetIndex.getAndIncrement();  // Get current index and increment
                        String tweetSnippet = batch.get(currentTweetIndex);
                        tweetSnippet = tweetSnippet.length() > 50 ? tweetSnippet.substring(0, 50) + "..." : tweetSnippet;
                        System.out.printf("Tweet Snippet: \"%s\", Sentiment: %s, Positive: %.2f, Neutral: %.2f, Negative: %.2f%n",
                                tweetSnippet,
                                sentiment.getSentiment(),
                                sentiment.getConfidenceScores().getPositive(),
                                sentiment.getConfidenceScores().getNeutral(),
                                sentiment.getConfidenceScores().getNegative());
                    } else {
                        TextAnalyticsError error = result.getError(); // Correctly use TextAnalyticsError
                        System.err.printf("Error Code: %s, Message: %s%n", error.getErrorCode(), error.getMessage());
                    }
                });
            }

            // Store results for the current show
            resultsByShow.put(showTitle, resultsArray);
        }

        return resultsByShow;
    }

    private static void writeResultsToJsonFile(JSONObject resultsByShow) {
        try (FileWriter file = new FileWriter("sentiment_results.json")) {
            file.write(resultsByShow.toJSONString());
            file.flush();
            System.out.println("Sentiment analysis results saved to sentiment_results.json");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
