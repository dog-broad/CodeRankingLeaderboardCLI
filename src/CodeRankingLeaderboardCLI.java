import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import org.apache.commons.math3.stat.ranking.NaNStrategy;
import org.json.JSONArray;
import org.json.JSONObject;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


public class CodeRankingLeaderboardCLI {
    static HashMap<String, Integer> geeksforgeeksDB = new HashMap<>();
    static HashMap <String, Integer> hackerrankDB = new HashMap<>();

    static boolean hackerrankchk = false;
    static String searchToken = "";
    static int hackerrankMaxScore = 0;
    static int GFGMaxScore = 0;
    static int GFGpMaxScore = 0;
    static int codechefMaxRating = 0;
    static int leetcodeMaxRating = 0;
    static int codeforcesMaxRating = 0;

    public CodeRankingLeaderboardCLI(String[] args) {
        // The first argument is Excel sheet containing user data
        // The second argument is Text file containing HackerRank contest data
        String csvFilePath = args[0];
        String textFilePath = args[1];

        // Display the paths
        System.out.println("Excel File Path: " + csvFilePath);
        System.out.println("Text File Path: " + textFilePath);

        // Empty lines
        System.out.println("========================================");

        // Create a new instance of the CodeRankingLeaderboard class
        try {
            hackerrankMaxScore = 0;
            GFGMaxScore = 0;
            GFGpMaxScore = 0;
            codechefMaxRating = 0;
            leetcodeMaxRating = 0;
            codeforcesMaxRating = 0;
            // set searchToken to the content of text file with bufferedReader
            searchToken = readTextFile(textFilePath);
            if (searchToken.replace(" ", "").isEmpty()) {
                // print no HackerRank contest data
                System.out.println("No HackerRank contest data");
                hackerrankchk = false;
            }
            else {
                hackerrankchk = true;
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }

        // Start the other functions in a separate thread
        Thread thread = new Thread(() -> {
            List<Participant> curr_leaderboard;

            // Load previous Excel sheet if provided
            if (!csvFilePath.isEmpty()) {
                try{
                    curr_leaderboard  = loadCSVSheet(csvFilePath);
                    if(curr_leaderboard.isEmpty()) throw new Exception();
                    downloadLeaderboard(curr_leaderboard);
                    // Sort and assign ranks
                    assignRanks(curr_leaderboard);

                    // Display the leaderboard in console
                    exportParticipantsToExcel((ArrayList<Participant>) curr_leaderboard);

                    curr_leaderboard.clear();
                } catch(Exception f) {
                    System.out.println("Error: " + f.getMessage());
                }
            }
            else {
                System.out.println("No Excel sheet provided");
            }
            geeksforgeeksDB.clear();
            hackerrankDB.clear();
        });
        thread.start();
    }

    private static String readTextFile(String textFilePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(textFilePath));
        // Set the first line as searchToken
        return reader.readLine();
    }

    List<Participant> loadCSVSheet(String csvFilePath) {
        System.out.println("========================================");
        System.out.println("Loading CSV sheet...");
        System.out.println("========================================");
        ArrayList<Participant> participants = new ArrayList<>();

        try {
            File csvFile = new File(csvFilePath);
            BufferedReader br = new BufferedReader(new FileReader(csvFile));

            String line;
            boolean headerSkipped = false;

            while ((line = br.readLine()) != null) {
                // if line is empty, break
                if (",,,,,,".equals(line)) {
                    break;
                }

                if (!headerSkipped) {
                    // Skip header line
                    headerSkipped = true;
                    continue;
                }

                // Replace #N/A with empty string
                line = line.replace("#N/A", "$");

                String[] data = line.split(","); // Assuming CSV uses comma as a delimiter
                if (data.length >= 5) {
                    Participant participant = new Participant();
                    participant.setHandle(data[0].replace(" ", "").replace("$", "").toLowerCase());
                    participant.setCodeforcesHandle(data[3].replace(" ", "").replace("$", "").toLowerCase());
                    participant.setGeeksForGeeksHandle(data[2].replace(" ", "").replace("$", "").toLowerCase());
                    // Add GeeksForkGeeksHandle to geeksforgeeksDB
                    geeksforgeeksDB.put(participant.getGeeksForGeeksHandle(), participants.size());
                    participant.setLeetcodeHandle(data[4].replace(" ", "").replace("$", "").toLowerCase());
                    participant.setCodeChefHandle(data[5].replace(" ", "").replace("$", "").toLowerCase());

                    if (hackerrankchk) {
                        participant.setHackerrankHandle(data[6].replace(" ", "").replace("$", "").toLowerCase());
                        // Add HackerrankHandle to hackerrankDB
                        hackerrankDB.put(participant.getHackerrankHandle(), participants.size());
                    }

                    // Print participant data
                    System.out.println("Handle: " + participant.getHandle());
                    System.out.println("---------------------------");
                    System.out.println("Codeforces Handle: " + participant.getCodeforcesHandle());
                    System.out.println("GeeksForGeeks Handle: " + participant.getGeeksForGeeksHandle());
                    System.out.println("Leetcode Handle: " + participant.getLeetcodeHandle());
                    System.out.println("CodeChef Handle: " + participant.getCodeChefHandle());
                    System.out.println("Hackerrank Handle: " + participant.getHackerrankHandle());
                    System.out.println();
                    System.out.println("========================================");

                    participants.add(participant);
                } else {
                    // Handle incomplete or unexpected rows
                    System.out.println("Incomplete row or missing data. Skipping...");
                }
            }

            br.close();
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            return new ArrayList<>();
        }

        return participants;
    }

    private void downloadLeaderboard(List<Participant> list) {
        System.out.println("Downloading leaderboard...");
        System.out.println("========================================");
        try{
            String url;
            URL websiteUrl;
            URLConnection connection;
            HttpURLConnection o;
            InputStream inputStream;
            // geeksforgeeks
            System.out.println("\nDownloading GeeksForGeeks Leaderboard...");
            System.out.println();
            for(int j=1;j<=10000;j++){
                System.out.print("\rPage: "+j);
                try{
                    url = "https://practiceapi.geeksforgeeks.org/api/latest/events/recurring/gfg-weekly-coding-contest/leaderboard/?leaderboard_type=0&page="+j;
                    websiteUrl = new URL(url);
                    connection = new URL(url).openConnection();
                    o = (HttpURLConnection) websiteUrl.openConnection();
                    o.setRequestMethod("GET");
                    if (o.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND || o.getResponseCode() == HttpURLConnection.HTTP_NOT_ACCEPTABLE){ continue; }
                    inputStream = connection.getInputStream();
                    try ( BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                        StringBuilder jsonContent = new StringBuilder();
                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            jsonContent.append(line);
                        }
                        JSONObject jsonObject = new JSONObject(jsonContent.toString());
                        JSONArray arr = jsonObject.getJSONArray("results");
                        int n = arr.length();
                        if( n == 0) break;
                        for(int i=0;i<n;i++){
                            JSONObject tmp = arr.getJSONObject(i);
                            String userHandle = tmp.getString("user_handle").toLowerCase();
                            if(geeksforgeeksDB.containsKey(userHandle)) {
                                int score = (int)tmp.getDouble("user_score");
                                list.get(geeksforgeeksDB.get(userHandle)).setGeeksForGeeksScore(score);
                                System.out.println(" => GeeksForGeeks " + userHandle + " " + score);
                                GFGMaxScore = Integer.max(GFGMaxScore, score);
                            }
                        }
                    } catch(Exception t) {}
                }catch(Exception pp) {}
            }
            System.out.println("\nDownloaded GeeksForGeeks Practice Leaderboard!");
            // hackerrank
            System.out.println("==================================================");
            System.out.println("Downloading HackerRank Leaderboard...");
            if(hackerrankchk){
                try{
                    String[] tracker_names = searchToken.replace(" ", "").split(",");
                    System.out.println("Tracker Names: " + Arrays.toString(tracker_names));
                    for(String tracker_name : tracker_names){
                        System.out.println("------------------");
                        System.out.println("Tracker Name: " + tracker_name);
                        System.out.println();
                        for(int j=0;j<10000;j+=100){
                            try{
                                url = "https://www.hackerrank.com/rest/contests/" + tracker_name +  "/leaderboard?offset="+j+"&limit=100";
                                websiteUrl = new URL(url);
                                connection = new URL(url).openConnection();
                                o = (HttpURLConnection) websiteUrl.openConnection();
                                o.setRequestMethod("GET");
                                connection.setRequestProperty("Accept", "application/json");
                                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                                if (o.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND || o.getResponseCode() == HttpURLConnection.HTTP_NOT_ACCEPTABLE){ throw new ArithmeticException("INVALID URL : " + tracker_name); }
                                inputStream = connection.getInputStream();
                                try ( BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                                    StringBuilder jsonContent = new StringBuilder();
                                    String line;
                                    while ((line = bufferedReader.readLine()) != null) {
                                        jsonContent.append(line);
                                    }
                                    JSONObject jsonObject = new JSONObject(jsonContent.toString());
                                    JSONArray arr = jsonObject.getJSONArray("models");
                                    int n = arr.length();
                                    if( n == 0) break;
                                    for(int i=0;i<n;i++){
                                        JSONObject tmp = arr.getJSONObject(i);
                                        String userHandle = tmp.getString("hacker").toLowerCase();

                                        if( ( !userHandle.isBlank() && !userHandle.equals("[deleted]")) && hackerrankDB.containsKey(userHandle)) {
                                            int index = hackerrankDB.get(userHandle);
                                            int score = list.get(index).getHackerrankScore()+(int)tmp.getDouble("score");
                                            System.out.println("Setting " + userHandle+" : "+score);
                                            hackerrankMaxScore = Integer.max(score, hackerrankMaxScore);
                                            list.get(index).setHackerrankScore(score);
                                        }
                                    }
                                } catch(Exception t) {}
                            }
                            catch(ArithmeticException e){
                                JOptionPane.showMessageDialog(null, tracker_name+" is invalid. Ignoring...", "Error", JOptionPane.ERROR_MESSAGE);
                                break;
                            }
                            catch(Exception pp) {}
                        }
                    }
                }catch(Exception ee){}
            }
            System.out.println();
            System.out.println("========================================");
            System.out.println("Setting overall coding scores...");
            System.out.print("========================================");
            int n = list.size();
            for(int i=0;i<n;i++){
                System.out.println("\n\nUser: " + list.get(i).getHandle());
                System.out.println(("----------------------------------------"));
                // geeksforgeeks overallScore
                System.out.print("GeeksforGeeks: ");
                try{
                    if(list.get(i).getGeeksForGeeksHandle().isBlank()) throw new Exception("");
                    url = "https://coding-platform-profile-api.onrender.com/geeksforgeeks/"+list.get(i).getGeeksForGeeksHandle();
                    websiteUrl = new URL(url);
                    connection = new URL(url).openConnection();
                    o = (HttpURLConnection) websiteUrl.openConnection();
                    o.setRequestMethod("GET");
                    if (o.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND || o.getResponseCode() == HttpURLConnection.HTTP_NOT_ACCEPTABLE){
                        throw new ArithmeticException();
                    }
                    inputStream = connection.getInputStream();
                    try ( BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                        StringBuilder jsonContent = new StringBuilder();
                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            jsonContent.append(line);
                        }
                        JSONObject jsonObject = new JSONObject(jsonContent.toString());
                        int score;
                        try{
                            score = jsonObject.getInt("overall_coding_score");
                        }catch(Exception e) { score = 0; }
                        System.out.print("Setting " + list.get(i).getGeeksForGeeksHandle() + " overall coding score: " + score);
                        list.get(i).setGeeksForGeekspScore(score);
                        GFGpMaxScore = Integer.max(score, GFGpMaxScore);
                    }catch (Exception e) { }
                }catch(Exception e) {  }

                // Codechef
                System.out.print("\nCodechef: ");
                try{
                    if(list.get(i).getCodeChefHandle().isBlank()) throw new Exception("");
                    url = "https://codechef-api.vercel.app/"+list.get(i).getCodeChefHandle();
                    websiteUrl = new URL(url);
                    connection = new URL(url).openConnection();
                    o = (HttpURLConnection) websiteUrl.openConnection();
                    o.setRequestMethod("GET");
                    if (o.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND || o.getResponseCode() == HttpURLConnection.HTTP_NOT_ACCEPTABLE){
                        throw new ArithmeticException();
                    }
                    inputStream = connection.getInputStream();
                    try ( BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                        StringBuilder jsonContent = new StringBuilder();
                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            jsonContent.append(line);
                        }
                        JSONObject jsonObject = new JSONObject(jsonContent.toString());
                        int rating = 0;
                        try{
                            rating = jsonObject.getInt("currentRating");
                        }catch(Exception e) { rating = 0; }
                        System.out.print("Setting " + list.get(i).getCodeChefHandle() + " codechef rating: " + rating);
                        list.get(i).setCodeChefRating(rating);
                        codechefMaxRating = Integer.max(codechefMaxRating, rating);
                    }catch (Exception e) { }
                }catch(Exception e){  }

                // leetcode
                System.out.print("\nLeetcode: ");
                try{
                    if(list.get(i).leetcode_handle.isBlank()) throw new Exception("");
                    url = "https://leetcode.com/graphql?query=query{userContestRanking(username:\""+ list.get(i).getLeetcodeHandle() + "\"){rating}}";
                    websiteUrl = new URL(url);
                    connection = new URL(url).openConnection();
                    o = (HttpURLConnection) websiteUrl.openConnection();
                    o.setRequestMethod("GET");
                    if (o.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND || o.getResponseCode() == HttpURLConnection.HTTP_NOT_ACCEPTABLE)
                    {  throw new ArithmeticException(); }
                    inputStream = connection.getInputStream();
                    try ( BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                        StringBuilder jsonContent = new StringBuilder();
                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            jsonContent.append(line);
                        }
                        JSONObject jsonObject = new JSONObject(jsonContent.toString());
                        int rating = 0;
                        try{
                            rating = (int)jsonObject.getJSONObject("data").getJSONObject("userContestRanking").getDouble("rating");
                        }catch(Exception e) { rating = 0; }
                        System.out.print("Setting " + list.get(i).getLeetcodeHandle() + " leetcode rating: " + rating);
                        list.get(i).setLeetcodeRating(rating);
                        leetcodeMaxRating = Integer.max(rating, leetcodeMaxRating);
                    }catch (Exception e) { }
                }catch(Exception e) {  }
                // codeforces
                System.out.print("\nCodeforces: ");
                try{
                    if(list.get(i).getCodeforcesHandle().isBlank()) throw new Exception("");
                    url = "https://codeforces.com/api/user.info?handles="+list.get(i).getCodeforcesHandle();
                    websiteUrl = new URL(url);
                    connection = new URL(url).openConnection();
                    o = (HttpURLConnection) websiteUrl.openConnection();
                    o.setRequestMethod("GET");
                    if (o.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND || o.getResponseCode() == HttpURLConnection.HTTP_NOT_ACCEPTABLE)
                    {  throw new ArithmeticException(); }
                    inputStream = connection.getInputStream();
                    try ( BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                        StringBuilder jsonContent = new StringBuilder();
                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            jsonContent.append(line);
                        }
                        JSONObject jsonObject = new JSONObject(jsonContent.toString());
                        int rating = 0;
                        try{
                            rating = (int)jsonObject.getJSONArray("result").getJSONObject(0).getInt("maxRating");
                        }catch(Exception e) { rating = 0; }
                        System.out.print("Setting " + list.get(i).getCodeforcesHandle() + " codeforces rating: " + rating);
                        list.get(i).setCodeforcesRating(rating);
                        codeforcesMaxRating = Integer.max(rating, codeforcesMaxRating);
                    }catch (Exception e) { }
                } catch(Exception e) {  }
                System.out.println();
            }
        } catch(Exception e) {}
    }

    @SuppressWarnings("ExtractMethodRecommender")
    private static double participantRank(Participant p){ // using normalization and weighted averages
        // metric being 35% codeforces, 30% geeksforgeeks, 10% geeksforgeeks(practice), 15% leetcode, 10% codechef
        // including hackerrank:
        // metric being 30% codeforces, 30% geeksforgeeks, 10% geeksforgeeks(practice), 10% leetcode, 10% codechef, 10% hackerrank
        // 1477 3400 13.03235
        try{
            double cf   =   (p.getCodeforcesRating()/(double)codeforcesMaxRating)  *100  ;
            double gfgs =   (p.getGeeksForGeeksScore()/(double)GFGMaxScore)        *100  ;
            double gfgp =   (p.getGeeksForGeekspScore()/(double)GFGpMaxScore)      *100  ;
            double lc   =   (p.getLeetcodeRating()/(double)leetcodeMaxRating)      *100  ;
            double cc   =   (p.getCodeChefRating()/(double)codechefMaxRating)      *100  ;
            double hr   =   (p.getHackerrankScore()/(double)hackerrankMaxScore)    *100  ;
            double percentile ;

            // debug statements
            System.out.println("\n------------------------------------------");
            System.out.println("Calculating percentile for " + p.getHandle());
            System.out.println("------------------------------------------");
            System.out.println("Codeforces: " + cf);
            System.out.println("GeeksForGeeks: " + gfgs);
            System.out.println("GeeksForGeeks Practice: " + gfgp);
            System.out.println("Leetcode: " + lc);
            System.out.println("Codechef: " + cc);
            System.out.println("Hackerrank: " + hr);
            System.out.println("========================================");

            if( hackerrankchk ) percentile = ( cf * 0.3 + gfgs*0.3  + gfgp*0.1 + lc*0.1 + cc*0.1 + hr*0.1 );
            else                percentile = ( cf * 0.35 + gfgs*0.3  + gfgp*0.1 + lc*0.15 + cc*0.1 );
            p.setPercentile(percentile);
            System.out.println(p.getHandle() + " " + p.getPercentile());
            return percentile;
        }catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Something went wrong!", "Error", JOptionPane.ERROR_MESSAGE);
            return 0;
        }
    }

    private static void assignRanks(List<Participant> leaderboard) {
        System.out.println("\n\n========================================");
        System.out.println("Get All ranks...");
        System.out.println("========================================");
        Collections.sort(leaderboard, (a, b)-> -Double.compare(participantRank(a),participantRank(b) ) );
        try {
            System.out.println("\n========================================");
            System.out.println("Assigning ranks...");
            System.out.println("========================================");
            for (int i = 0; i < leaderboard.size(); i++) {
                System.out.println("Assigning " + leaderboard.get(i).getHandle() + " " + leaderboard.get(i).getPercentile());
                leaderboard.get(i).setRank(i + 1);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Something went wrong!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    static void exportParticipantsToExcel(ArrayList<Participant> participants) {
        System.out.println("\n========================================");
        System.out.println("Exporting participants to Excel sheet...");
        System.out.println("========================================");
        try {
            // Create a new Workbook
            XSSFWorkbook workbook = new XSSFWorkbook();

            // Create a new Sheet
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Current CodeRankingLeaderboard");

            // Create bold font with size 18 for column headers
            org.apache.poi.ss.usermodel.Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldFont.setFontHeightInPoints((short) 20);

            org.apache.poi.ss.usermodel.Font boldFont2 = workbook.createFont();
            boldFont2.setBold(true);
            boldFont2.setFontHeightInPoints((short) 14);

            // Create bold centered cell style with 14 font size for normal cells
            CellStyle boldCenteredCellStyle = workbook.createCellStyle();
            boldCenteredCellStyle.setAlignment(HorizontalAlignment.CENTER);
            boldCenteredCellStyle.setFont(boldFont);
            boldCenteredCellStyle.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE1.getIndex());
            boldCenteredCellStyle.setBorderBottom(BorderStyle.THICK);
            boldCenteredCellStyle.setBorderTop(BorderStyle.THICK);
            boldCenteredCellStyle.setBorderLeft(BorderStyle.THICK);
            boldCenteredCellStyle.setBorderRight(BorderStyle.THICK);
            boldCenteredCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            // Create bold cell style with 14 font size for normal cells
            CellStyle boldCellStyle = workbook.createCellStyle();
            boldCellStyle.setAlignment(HorizontalAlignment.CENTER);
            boldCellStyle.setFont(boldFont2);
            boldCellStyle.setFillForegroundColor(IndexedColors.TURQUOISE.getIndex());
            boldCellStyle.setBorderBottom(BorderStyle.THICK);
            boldCellStyle.setBorderTop(BorderStyle.THICK);
            boldCellStyle.setBorderLeft(BorderStyle.THICK);
            boldCellStyle.setBorderRight(BorderStyle.THICK);
            boldCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Add column headers =>
            // Rank, Codeforces_Handle, 35% Codeforces_Rating,
            //       GFG_Handle, 30% GFG_Contest_Score, 10% GFG_Practice_Score,
            //       Leetcode_Handle, 15% Leetcode_Rating
            //       CodeChef_Handle, 10% Codechef_Rating
            System.out.println("Adding column headers...");
            Row headerRow = sheet.createRow(0);
            Cell rankHeaderCell = headerRow.createCell(0);
            rankHeaderCell.setCellValue("Rank");
            rankHeaderCell.setCellStyle(boldCenteredCellStyle);

            Cell handleHeaderCell = headerRow.createCell(1);
            handleHeaderCell.setCellValue("Handle");
            handleHeaderCell.setCellStyle(boldCenteredCellStyle);

            Cell codeforcesIdHeaderCell = headerRow.createCell(2);
            codeforcesIdHeaderCell.setCellValue("Codeforces_Handle");
            codeforcesIdHeaderCell.setCellStyle(boldCenteredCellStyle);

            Cell codeforcesRatingHeaderCell = headerRow.createCell(3);
            codeforcesRatingHeaderCell.setCellValue("Codeforces_Rating");
            codeforcesRatingHeaderCell.setCellStyle(boldCenteredCellStyle);

            Cell gfgHeaderCell = headerRow.createCell(4);
            gfgHeaderCell.setCellValue("GFG_Handle");
            gfgHeaderCell.setCellStyle(boldCenteredCellStyle);

            Cell gfgCScoreHeaderCell = headerRow.createCell(5);
            gfgCScoreHeaderCell.setCellValue("GFG_Contest_Score");
            gfgCScoreHeaderCell.setCellStyle(boldCenteredCellStyle);

            Cell gfgPScoreHeaderCell = headerRow.createCell(6);
            gfgPScoreHeaderCell.setCellValue("GFG_Practice_Score");
            gfgPScoreHeaderCell.setCellStyle(boldCenteredCellStyle);

            Cell leetcodeHeaderCell = headerRow.createCell(7);
            leetcodeHeaderCell.setCellValue("Leetcode_Handle");
            leetcodeHeaderCell.setCellStyle(boldCenteredCellStyle);

            Cell leetcodeRatingHeaderCell = headerRow.createCell(8);
            leetcodeRatingHeaderCell.setCellValue("Leetcode_Rating");
            leetcodeRatingHeaderCell.setCellStyle(boldCenteredCellStyle);

            Cell codechefHeaderCell = headerRow.createCell(9);
            codechefHeaderCell.setCellValue("Codechef_Handle");
            codechefHeaderCell.setCellStyle(boldCenteredCellStyle);

            Cell codechefRatingHeaderCell = headerRow.createCell(10);
            codechefRatingHeaderCell.setCellValue("Codechef_Rating");
            codechefRatingHeaderCell.setCellStyle(boldCenteredCellStyle);

            int hc = 11;

            if( hackerrankchk ){
                hc = 13;
                Cell hackerRankHeaderCell = headerRow.createCell(11);
                hackerRankHeaderCell.setCellValue("HackerRank_Handle");
                hackerRankHeaderCell.setCellStyle(boldCenteredCellStyle);

                Cell hackerRankRatingHeaderCell = headerRow.createCell(12);
                hackerRankRatingHeaderCell.setCellValue("HackerRank_Practice_Score");
                hackerRankRatingHeaderCell.setCellStyle(boldCenteredCellStyle);
            }

            Cell percentileHeaderCell = headerRow.createCell(hc);
            percentileHeaderCell.setCellValue("Percentile");
            percentileHeaderCell.setCellStyle(boldCenteredCellStyle);

            // Add participants' data : Rank, Codeforces_Handle, 35% Codeforces_Rating,
            //       GFG_Handle, 30% GFG_Contest_Score, 10% GFG_Practice_Score,
            //       Leetcode_Handle, 15% Leetcode_Rating
            //       CodeChef_Handle, 10% Codechef_Rating
            System.out.println("Adding participants' data...");
            for (int i = 0; i < participants.size(); i++) {
                // print participant id added with carriage return
                System.out.println("Added participant " + participants.get(i).getHandle());
                System.out.print("\u001B[A");
                Participant participant = participants.get(i);
                Row row = sheet.createRow(i + 1);

                Cell rankCell = row.createCell(0);
                rankCell.setCellValue(participant.getRank());
                rankCell.setCellStyle(boldCellStyle);

                Cell handleCell = row.createCell(1);
                handleCell.setCellValue(participant.getHandle());
                handleCell.setCellStyle(boldCellStyle);

                Cell idCell1 = row.createCell(2);
                idCell1.setCellValue(participant.getCodeforcesHandle());
                idCell1.setCellStyle(boldCellStyle);

                Cell scoreCell1 = row.createCell(3);
                scoreCell1.setCellValue(participant.getCodeforcesRating());
                scoreCell1.setCellStyle(boldCellStyle);

                Cell idCell2 = row.createCell(4);
                idCell2.setCellValue(participant.getGeeksForGeeksHandle());
                idCell2.setCellStyle(boldCellStyle);

                Cell scoreCell2 = row.createCell(5);
                scoreCell2.setCellValue(participant.getGeeksForGeeksScore());
                scoreCell2.setCellStyle(boldCellStyle);

                Cell scoreCell2_1 = row.createCell(6);
                scoreCell2_1.setCellValue(participant.getGeeksForGeekspScore());
                scoreCell2_1.setCellStyle(boldCellStyle);

                Cell idCell3 = row.createCell(7);
                idCell3.setCellValue(participant.getLeetcodeHandle());
                idCell3.setCellStyle(boldCellStyle);

                Cell scoreCell3 = row.createCell(8);
                scoreCell3.setCellValue(participant.getLeetcodeRating());
                scoreCell3.setCellStyle(boldCellStyle);

                Cell idCell4 = row.createCell(9);
                idCell4.setCellValue(participant.getCodeChefHandle());
                idCell4.setCellStyle(boldCellStyle);

                Cell scoreCell4 = row.createCell(10);
                scoreCell4.setCellValue(participant.getCodeChefRating());
                scoreCell4.setCellStyle(boldCellStyle);

                if(hackerrankchk){
                    Cell idCell5 = row.createCell(11);
                    idCell5.setCellValue(participant.getHackerrankHandle());
                    idCell5.setCellStyle(boldCellStyle);

                    Cell scoreCell5 = row.createCell(12);
                    scoreCell5.setCellValue(participant.getHackerrankScore());
                    scoreCell5.setCellStyle(boldCellStyle);
                }
                Cell scoreCell6 = row.createCell(hc);
                DecimalFormat df = new DecimalFormat("#.##");
                scoreCell6.setCellValue( df.format(participant.getPercentile())+"%" );
                scoreCell6.setCellStyle(boldCellStyle);
            }

            File folder = new File("Leaderboards");
            if (!folder.exists()) {
                folder.mkdir();
            }

            // Resize columns to fit the content
            for(int i=0;i<12 + (hackerrankchk?2:0);i++) sheet.autoSizeColumn(i);

            String baseFileName = "Leaderboards/CurrentCodeRankingLeaderboard";
            String extension = ".xlsx";


            File file = new File(baseFileName + extension);

            if (file.exists()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                String timestamp = dateFormat.format(new Date());
                baseFileName = baseFileName + "_" + timestamp;
                file = new File(baseFileName + extension);
            }

            try ( FileOutputStream fileOut = new FileOutputStream(file)) {
                workbook.write(fileOut);
                System.out.println("Excel file created successfully!");
                System.out.println("File Path: " + file.getAbsolutePath());

                // Close the application gracefully
                System.exit(0);

                workbook.close();

            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Something Went Wrong! ", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (HeadlessException e) {
            JOptionPane.showMessageDialog(null, "Something Went Wrong!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private class Participant {
        private String handle;

        private String codechef_handle;
        private String codeforces_handle;
        private String leetcode_handle;
        private String geeksforgeeks_handle;
        private String hackerrank_handle;

        private int codechefrating;
        private int codeforcesrating;
        private int leetcoderating;
        private int geeksforgeeksscore;  // Contest Score
        private int geeksforgeekspscore; // Practice Score
        private int hackerrankscore;
        private double percentile;

        private int rank;

        private Participant() {
            this.percentile = 0;
            this.hackerrankscore = 0;
            this.geeksforgeekspscore = 0;
            this.geeksforgeeksscore = 0;
            this.codeforcesrating = 0;
            this.codechefrating = 0;
            this.leetcoderating = 0;
        }

        public void setPercentile(double p){
            this.percentile = p;
        }

        public double getPercentile(){
            return this.percentile;
        }

        public void setHandle(String handle) {
            this.handle = handle;
        }

        public void setHackerrankHandle(String handle) {
            this.hackerrank_handle = handle;
        }

        public void setGeeksForGeeksHandle(String handle) {
            this.geeksforgeeks_handle = handle;
        }

        public void setLeetcodeHandle(String handle) {
            this.leetcode_handle = handle;
        }

        public void setCodeChefHandle(String handle) {
            this.codechef_handle = handle;
        }

        public void setCodeforcesHandle(String handle) {
            this.codeforces_handle = handle;
        }

        public String getGeeksForGeeksHandle() {
            return this.geeksforgeeks_handle;
        }

        public String getLeetcodeHandle() {
            return this.leetcode_handle;
        }

        public String getHackerrankHandle() {
            return this.hackerrank_handle;
        }

        public String getCodeChefHandle() {
            return this.codechef_handle;
        }

        public String getCodeforcesHandle() {
            return this.codeforces_handle;
        }

        public void setGeeksForGeeksScore(int score) {
            this.geeksforgeeksscore = score;
        }

        public void setHackerrankScore(int score) {
            this.hackerrankscore = score;
        }

        public void setGeeksForGeekspScore(int score){
            this.geeksforgeekspscore = score;
        }

        public void setLeetcodeRating(int rating) {
            this.leetcoderating = rating;
        }

        public void setCodeChefRating(int rating) {
            this.codechefrating = rating;
        }

        public void setCodeforcesRating(int rating) {
            this.codeforcesrating = rating;
        }

        public String getHandle() {
            return handle;
        }

        public int getGeeksForGeeksScore() {
            return this.geeksforgeeksscore;
        }

        public int getHackerrankScore() {
            return this.hackerrankscore;
        }

        public int getGeeksForGeekspScore() {
            return this.geeksforgeekspscore;
        }

        public int getLeetcodeRating() {
            return this.leetcoderating;
        }

        public int getCodeChefRating() {
            return this.codechefrating;
        }

        public int getCodeforcesRating() {
            return this.codeforcesrating;
        }

        public int getRank() {
            return rank;
        }

        public void setRank(int rank) {
            this.rank = rank;
        }
    }

    public static void main(String[] args) throws IOException {
        new CodeRankingLeaderboardCLI(args);
    }
}
