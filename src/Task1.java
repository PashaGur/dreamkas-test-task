import com.fasterxml.jackson.databind.JsonNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import com.github.fge.jackson.JsonLoader;


public class Task1 {

    private Map<String, String> resultMap = new LinkedHashMap<>();
    private Steps currentStep = Steps.STEP_0;


    public static void main(String[] args) {
        Task1 task1Case = new Task1();
        System.out.println("Test have been started\n");
        for (int i = 0; i < Steps.values().length; i++) {
            task1Case.startTest();
        }
    }

    private void startTest() {
        String response = null;
        try {
            response = sendGetToString(currentStep.getUrl());
        } catch (Exception e) {
            System.out.println("Failed to send http request" + e);
        }
        try {
            JsonNode jsonResponse = JsonLoader.fromString(response);
            switch (currentStep) {
                case STEP_0:
                    checkFilmsCount(jsonResponse);
                    currentStep = Steps.STEP_1;
                    break;
                case STEP_1:
                    checkFilmsEpisodes(jsonResponse);
                    currentStep = Steps.STEP_2;
                    break;
                case STEP_2:
                    checkFilmsPlanets(jsonResponse);
                    currentStep = Steps.STEP_3;
                    break;
                case STEP_3:
                    checkPeopleInfo(jsonResponse);
                    currentStep = Steps.STEP_4;
                    break;
                case STEP_4:
                    findSpaceShipInfo(jsonResponse);
                    currentStep = Steps.STEP_5;
                    break;
                case STEP_5:
                    checkFor100thPerson(jsonResponse);
                    printResult(resultMap);
                    break;
                default:
                    System.out.println("Wrong currentStep value");
                    break;
            }
        } catch (IOException e) {
            System.out.println("Failed to get Json from message" + response);
        } catch (NullPointerException e) {
            System.out.println("NullPointer ERROR at " + currentStep.name());
        }
    }


    private void printResult(Map<String, String> resultMap) {
        for (Map.Entry<String, String> pair : resultMap.entrySet()) {
            System.out.println(pair.getKey() + " " + pair.getValue() + "\n");
        }
        System.out.println("\nTest Finished");
    }


    // STEP 0 - Star wars films count
    private void checkFilmsCount(JsonNode response) {
        try {
            int filmsCount = response.get("count").intValue();
            if (filmsCount != 7) {
                resultMap.put(currentStep.getQuestion(), " Expected = 7, response Value = " + filmsCount);
            } else {
                resultMap.put(currentStep.getQuestion(), String.valueOf(filmsCount));
            }
        } catch (NullPointerException e) {
            System.out.println("Failed to read 'count' field at " + currentStep.name());
        }
    }

    // STEP 1 - 4th episode info (release date / director / title)
    private void checkFilmsEpisodes(JsonNode response) {
        try {
            int filmsCount = response.get("results").size();
            for (int i = 0; i < filmsCount; i++) {
                if (response.get("results").get(i).get("episode_id").intValue() == 4) {
                    String releaseDate = response.get("results").get(i).get("release_date").textValue();
                    String director = response.get("results").get(i).get("director").textValue();
                    String filmTitle = response.get("results").get(i).get("title").textValue();
                    resultMap.put(currentStep.getQuestion(), " releaseDate = " + releaseDate + ", director = " + director +
                            ", title = " + filmTitle);
                    break;
                }
            }
        } catch (NullPointerException e) {
            System.out.println("Failed to read 'results / release_date / director / title' field at " + currentStep.name());
        }
    }

    // STEP 2 - planet names of 2nd episode
    private void checkFilmsPlanets(JsonNode response) {
        StringBuilder planets = new StringBuilder();
        try {
            int filmsCount = response.get("results").size();
            for (int i = 0; i < filmsCount; i++) {
                if (response.get("results").get(i).get("episode_id").intValue() == 2) {
                    JsonNode jsonNodePlanets = response.get("results").get(i).get("planets");
                    for (int j = 0; j < jsonNodePlanets.size(); j++) {
                        JsonNode planetInfo = sendGetToJson(jsonNodePlanets.get(j).textValue() + "?format=json");
                        planets.append(planetInfo.get("name").textValue()).append(". ");
                    }
                    resultMap.put(currentStep.getQuestion(), planets.toString());
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to read planet names of 2nd episode at " + currentStep.name());
        }
    }

    // STEP 3 - Luke Skywalker's homeWorld
    private void checkPeopleInfo(JsonNode response) {
        boolean testCaseSuccess = false;
        try {
            int pagesCount = (response.get("count").intValue() / 10) + 1;
            for (int i = 1; i <= pagesCount; i++) {
                testCaseSuccess = responseForCheck(sendGetToJson("https://swapi.co/api/people/?page=" + i + "&format=json"));
                if (testCaseSuccess) break;
            }
            if (!testCaseSuccess) {
                resultMap.put(currentStep.getQuestion(), "Failed to find Lyke's homeWorld info");
            }
        } catch (NullPointerException e) {
            System.out.println("Failed to get results from response at " + currentStep.name());
        } catch (Exception e) {
            System.out.println("Failed to get Json from request at " + currentStep.name());
        }
    }

    private boolean responseForCheck(JsonNode peoplePageInfo) {
        int peopleAtPage = peoplePageInfo.get("results").size();
        for (int i = 0; i < peopleAtPage; i++) {
            if (peoplePageInfo.get("results").get(i).get("name").textValue().equals("Luke Skywalker")) {
                String homeWorldURL = peoplePageInfo.get("results").get(i).get("homeworld").textValue();
                try {
                    JsonNode homeWorldResponse = sendGetToJson(homeWorldURL + "?format=json");
                    String homeForLyke = homeWorldResponse.get("name").textValue();
                    resultMap.put(currentStep.getQuestion(), homeForLyke);
                } catch (Exception e) {
                    System.out.println("Failed to send homeWorld request at " + currentStep.name());
                }
                return true;
            }
        }
        return false;
    }

    // STEP 4 Obi-Wan's spaceships info from 3th episode
    /**
     * 3th episode = films/6
     * Obi-Wan = people/10
     */
    private void findSpaceShipInfo(JsonNode response) {
        JsonNode pilotInfo = null;
        StringBuilder allShipsInfo = new StringBuilder();
        try {
            int pagesCount = (response.get("count").intValue() / 10) + 1;
            for (int i = 1; i <= pagesCount; i++) {
                pilotInfo = findPilotInfo(sendGetToJson("https://swapi.co/api/people/?page=" + i + "&format=json"));
                if (pilotInfo != null) break;
            }
            JsonNode spaceShips = pilotInfo.get("starships");
            for (int i = 0; i < spaceShips.size(); i++) {
                checkSpaceShipInfo(sendGetToJson(spaceShips.get(i).textValue() + "?format=json"), allShipsInfo);
            }
            resultMap.put(currentStep.getQuestion(), allShipsInfo.toString());
        } catch (NullPointerException e) {
            System.out.println("Failed to read fields at " + currentStep.name());
        } catch (Exception e) {
            System.out.println("Failed to get json response at " + currentStep.name());
        }
    }

    private JsonNode findPilotInfo(JsonNode peoplePageInfo) {
        int peopleAtPage = peoplePageInfo.get("results").size();
        for (int i = 0; i < peopleAtPage; i++) {
            if (peoplePageInfo.get("results").get(i).get("name").textValue().equals("Obi-Wan Kenobi")) {

                return peoplePageInfo.get("results").get(i);
            }
        }
        return null;
    }

    private void checkSpaceShipInfo(JsonNode spaceShipInfo, StringBuilder allShipsInfo) {
        JsonNode pilotsOfSpaceShip = spaceShipInfo.get("pilots");
        JsonNode filmsOfSpaceShip = spaceShipInfo.get("films");
        for (int i = 0; i < pilotsOfSpaceShip.size(); i++) {
            for (int j = 0; j < filmsOfSpaceShip.size(); j++) {
                if (pilotsOfSpaceShip.get(i).textValue().contains("people/10") &&
                        filmsOfSpaceShip.get(j).textValue().contains("films/6")) {
                    allShipsInfo.append(getSpaceShipInfo(spaceShipInfo));

                }
            }
        }
    }

    private String getSpaceShipInfo(JsonNode spaceShipInfo) {
        return "name = " + spaceShipInfo.get("name").textValue() + ", model = " + spaceShipInfo.get("model").textValue() +
                ", manufacturer = " + spaceShipInfo.get("manufacturer").textValue() + ". ";
    }

    // STEP 5 Does 100th person in StarWars exists
    private void checkFor100thPerson(JsonNode jsonResponse) {
        boolean expectedResult;
        expectedResult = jsonResponse.get("count").intValue() >= 100;
        try {
            JsonNode the100thPersonInfo = sendGetToJson("https://swapi.co/api/people/100/?format=json");
            if (the100thPersonInfo != null && expectedResult) {
                resultMap.put(currentStep.getQuestion(), "100th person in StarWars exists");
            }
        } catch (Exception e) {
            if (!expectedResult) {
                resultMap.put(currentStep.getQuestion(), "There is NO 100th person in StarWars");
            }
        }
    }


    private enum Steps {
        STEP_0("Сколько фильмов в серии \"Звездные войны\"?", "https://swapi.co/api/films/?format=json"),
        STEP_1("В каком году вышел 4 эпизод? Кто был режиссёром? Как он назывался?", "https://swapi.co/api/films/?format=json"),
        STEP_2("Как назывались планеты во 2-ом эпизоде?", "https://swapi.co/api/films/?format=json"),
        STEP_3("Какая родная планета Luke Skywalker?", "https://swapi.co/api/people/?format=json"),
        STEP_4("На каком звездолете в летал Obi-Wan Kenobi в 3 эпизоде? Какими характеристиками этот звездолет обладал?", "https://swapi.co/api/people/?format=json"),
        STEP_5("Существует ли 100-ый персонаж?", "https://swapi.co/api/people/?format=json");

        public String getUrl() {
            return url;
        }

        public String getQuestion() {
            return question;
        }

        private String question;
        private String url;


        Steps(String question, String url) {
            this.question = question;
            this.url = url;

        }
    }

    private String sendGetToString(String url) throws Exception {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");

        con.setRequestProperty("User-Agent", "Chrome");
        con.setRequestProperty("Cookie", "foo=bar");
        int responseCode;
        responseCode = con.getResponseCode();
        if (responseCode != 200) {
            System.out.println(currentStep.name() + " responseCode != 200");
        }

//        System.out.println("\nSending 'GET' request to URL : " + url);
//        System.out.println("Response Code : " + responseCode);

        BufferedReader in;
        in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        return String.valueOf(response);
    }

    private JsonNode sendGetToJson(String url) throws Exception {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");

        con.setRequestProperty("User-Agent", "Chrome");
        con.setRequestProperty("Cookie", "foo=bar");
//        int responseCode;
//        responseCode = con.getResponseCode();

//        System.out.println("\nSending 'GET' request to URL : " + url);
//        System.out.println("Response Code : " + responseCode);

        BufferedReader in;
        in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        return JsonLoader.fromString(response.toString());
    }
}
