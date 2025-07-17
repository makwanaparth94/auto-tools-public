package com.santaba.auto.lib.lmusage.wrapper;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import com.santaba.auto.lib.app.AppBase;
import com.santaba.auto.lib.common.CommonLibs;
import com.santaba.auto.lib.lmusage.LMUsageConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class UsageSpreadSheetLib extends AppBase {

    private static final Logger logger = LogManager.getLogger(UsageSpreadSheetLib.class);
    CommonLibs commonLibs;
    static LocalDate currentdate = LocalDate.now();

    public UsageSpreadSheetLib() {
        commonLibs = new CommonLibs();
    }

    /**
     * This method is used to get the current year
     *
     * @return currentYear : The current year
     */
    public int getCurrentYear() {
        int currentYear = currentdate.getYear();
        logger.info("Current Year is -  "+ currentYear);
        return currentYear;
    }

    /**
     * This method is used to get the current month
     *
     * @return currentMonth : The current month
     */
    public String getCurrentMonth() {
        String currentMonth = String.valueOf(currentdate.getMonth());
        logger.info("Current Month is  -" + currentMonth);
        return currentMonth;
    }

    /**
     * This method is used to get the total days in the current month
     *
     * @return totalDays : The total days in the current month
     */
    public static int getTotalDaysInCurrentMonth() {
        int totalDays = currentdate.lengthOfMonth();
        logger.info("Total Days in Current Month is - "+ totalDays);
        return totalDays;
    }

    /**
     * This method is used to get the current day in the current month
     *
     * @return currentDay : The current day in the current month
     */
    public int getCurrentDayInCurrentMonth() {
        int currentDay = currentdate.getDayOfMonth();
        logger.info("Current Day in Current Month is - " + currentDay);
        return currentDay;
    }

    /**
     * This method is used to get the list of sheets in the spreadsheet
     *
     * @param SPREADSHEET_ID : The ID of the spreadsheet to read from (you can get this from the URL of the sheet)
     * @return List<Sheet>
     * @throws IOException              : If the credentials.json file cannot be found.
     * @throws GeneralSecurityException : If the credentials.json file cannot be found.
     */
    public static List<Sheet> getListOFSheets(String SPREADSHEET_ID) throws IOException, GeneralSecurityException {
        Sheets sheetsService = GoogleSheetService.getSheetsService();
        Spreadsheet spreadsheet = sheetsService.spreadsheets().get(SPREADSHEET_ID).execute();

        List<Sheet> sheets = spreadsheet.getSheets();
        if (sheets == null || sheets.isEmpty()) {
            logger.info("No sheets found during search for getListOFSheets for speadsheet ID - " + spreadsheet);
        }
        return sheets;
    }


    // Custom exception to throw when a sheet is not found
    public static class SheetNotFoundException extends Exception {
        public SheetNotFoundException(String message) {
            super(message);
        }
    }

    public String isSheetExist( String feature, String methodToCalculate)  {
        String month = getCurrentMonth();
        int year = getCurrentYear();
        String postfix = month + "-" + year;
        String sheetName = LMUsageLib.attachEnvAsSheetPrefix() + "-" + feature + "-" + postfix;

        try {
            List<Sheet> allSheets = getListOFSheets(LMUsageConstants.SPREADSHEET_ID);
            int count = 0;

            for (Sheet allSheet : allSheets) {
                String currentSheet = allSheet.getProperties().getTitle();
                logger.info("Current Sheet is : " + currentSheet);

                if (currentSheet.equals(sheetName)) {
                    count++;
                }
            }

            logger.info("value of count is : " + count);

            if (count == 0) {
                logger.info("Sheet is not exist");
                createSheetIfNotExist(sheetName);
                if (methodToCalculate.equalsIgnoreCase("SUM")) {
                    addDefaultDataInUsageSheetUsedSUM(sheetName);
                }
                if (methodToCalculate.equalsIgnoreCase("ROLLING")) {
                    addDefaultDataInSheetUsedRollingAvg(sheetName);
                }
            } else {
                logger.info("Sheet already exist");
            }
        } catch (IOException | GeneralSecurityException e) {
            logger.error("An error occurred while validating sheet existence: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Some error(except IO and GeneralSecurity Exception) has occurred while checking sheet existence :"
                    + e.getMessage());
        }

        return sheetName;
    }

    /**
     * This method is used to create a sheet if it is not exists
     *
     * @param sheetName : The name of the sheet to be created
     * @throws GeneralSecurityException : If the credentials.json file cannot be found.
     * @throws IOException              : If the credentials.json file cannot be found.
     */
    public void createSheetIfNotExist(String sheetName) throws GeneralSecurityException, IOException {
        // Get the Google Sheets service
        Sheets sheetsService = GoogleSheetService.getSheetsService();

        // Create a new sheet request with custom name
        AddSheetRequest addSheetRequest = new AddSheetRequest()
                .setProperties(new SheetProperties().setTitle(sheetName));

        // Create the batch update request
        Request request = new Request().setAddSheet(addSheetRequest);
        BatchUpdateSpreadsheetRequest body = new BatchUpdateSpreadsheetRequest().setRequests(Arrays.asList(request));

        // Execute the request to add the new sheet
        BatchUpdateSpreadsheetResponse response = sheetsService.spreadsheets()
                .batchUpdate(LMUsageConstants.SPREADSHEET_ID, body)
                .execute();

        // Print confirmation of the new sheet creation
        logger.info("New sheet created with title: " + sheetName);

    }


    /**
     * This method is used to add pre-requisites data for headers  in the sheet
     *
     * @param sheetName : The name of the sheet to add pre-requisites data
     */
    public void addDefaultDataInUsageSheetUsedSUM(String sheetName) {
        try{
            //Add pre-requisites data for Days Column
            addDefaultHeaderData(sheetName, "No of Days", "Usage Count");
            addDefaultDataToColumn(sheetName, "B");
            addDefaultDaysColumn(sheetName);
        } catch (IOException | GeneralSecurityException e) {
            logger.error("An error occurred during adding header and default data insertion in sheet: " + sheetName
                    + "-->" + e.getMessage());
        }

    }


    /**
     * This method is used to add pre-requisites data for headers  in the sheet
     *
     * @param sheetName : The name of the sheet to add pre-requisites data
     * @throws GeneralSecurityException : If the credentials.json file cannot be found.
     * @throws IOException              : If the credentials.json file cannot be found.
     */
    public void addDefaultDataInCleanupSheet(String sheetName) throws GeneralSecurityException, IOException {
        //Add pre-requisites data for Days Column
        addDefaultHeaderData(sheetName, "TC name", "Resource Name");
    }


    /**
     * This method is used to add pre-requisites data for headers  in the sheet
     *
     * @param sheetName : The name of the sheet to add pre-requisites data
     * @throws GeneralSecurityException : If the credentials.json file cannot be found.
     * @throws IOException              : If the credentials.json file cannot be found.
     */
    public void addCleanUpDataInSheet(String sheetName, String tcName, String deviceName) throws GeneralSecurityException, IOException {

        // Get the Google Sheets service
        Sheets sheetsService = GoogleSheetService.getSheetsService();

        // Create data to write as header
        List<List<Object>> cleanUpValues = Arrays.asList(
                Arrays.asList(tcName, deviceName)
        );

        // Create the ValueRange object and set the values
        ValueRange cleanUpData = new ValueRange().setValues(cleanUpValues);

        String rangeForCleanup = sheetName + "!A2:B" + getTotalDaysInCurrentMonth();
        logger.info("Value of Range on creating a sheet if not exists : " + rangeForCleanup);

        // Write the data to the spreadsheet
        sheetsService.spreadsheets().values()
                .update(LMUsageConstants.SPREADSHEET_ID, rangeForCleanup, cleanUpData)
                .setValueInputOption("RAW")
                .execute();

        logger.info("Pre-requisites Data written successfully for usage sheet headers");
    }

    /**
     * This method is used to add pre-requisites data for headers  in the sheet
     *
     * @param sheetName : The name of the sheet to add pre-requisites data
     * @throws GeneralSecurityException : If the credentials.json file cannot be found.
     * @throws IOException              : If the credentials.json file cannot be found.
     */
    public void addDefaultHeaderData(String sheetName, String header1, String header2) throws GeneralSecurityException, IOException {

        // Get the Google Sheets service
        Sheets sheetsService = GoogleSheetService.getSheetsService();

        // Create data to write as header
        List<List<Object>> headerValues = Arrays.asList(
                Arrays.asList(header1, header2)
        );

        // Create the ValueRange object and set the values
        ValueRange header = new ValueRange().setValues(headerValues);

        String rangeForHeaders = sheetName + "!A1:B" + getTotalDaysInCurrentMonth();
        logger.info("Value of Range on creating a sheet if not exists : " + rangeForHeaders);

        // Write the data to the spreadsheet
        sheetsService.spreadsheets().values()
                .update(LMUsageConstants.SPREADSHEET_ID, rangeForHeaders, header)
                .setValueInputOption("RAW")
                .execute();

        logger.info("Pre-requisites Data written successfully for usage sheet headers");
    }

    public void addDefaultHeaderData(String sheetName, String header1, String header2, String header3) throws GeneralSecurityException, IOException {

        // Get the Google Sheets service
        Sheets sheetsService = GoogleSheetService.getSheetsService();

        // Create data to write as header
        List<List<Object>> headerValues = Arrays.asList(
                Arrays.asList(header1, header2, header3)
        );

        // Create the ValueRange object and set the values
        ValueRange header = new ValueRange().setValues(headerValues);

        String rangeForHeaders = sheetName + "!A1:B" + getTotalDaysInCurrentMonth();
        logger.info("Value of Range on creating a sheet if not exists : " + rangeForHeaders);

        // Write the data to the spreadsheet
        sheetsService.spreadsheets().values()
                .update(LMUsageConstants.SPREADSHEET_ID, rangeForHeaders, header)
                .setValueInputOption("RAW")
                .execute();

        logger.info("Pre-requisites Data written successfully for usage sheet headers");
    }

    /**
     * This method is used to add pre-requisites default count for usagecount column  in the sheet
     *
     * @param sheetName : The name of the sheet to add pre-requisites data
     * @throws GeneralSecurityException : If the credentials.json file cannot be found.
     * @throws IOException              : If the credentials.json file cannot be found.
     */
    public void addDefaultDaysColumn(String sheetName) throws GeneralSecurityException, IOException {

        // Get the Google Sheets service
        Sheets sheetsService = GoogleSheetService.getSheetsService();

        int totalDays = getTotalDaysInCurrentMonth();

        // Prepare the list of values (one for each row in the specified range)
        List<List<Object>> usageCountDefaultValues = new ArrayList<>();
        for (int i = 1; i <= totalDays; i++) {  // For example, updating rows of total number of days of a// current month
            usageCountDefaultValues.add(List.of(i));  // Each row gets the same value
        }

        String rangeForUsageCountCells = sheetName + "!A2:A" + (totalDays + 1);  // The cell to update
        logger.info("Value of NoOfDays cells : " + rangeForUsageCountCells);

        // Create the new value to insert in the cell
        ValueRange usageCountCells = new ValueRange().setValues(usageCountDefaultValues);

        // Update the cell value using the Sheets API
        sheetsService.spreadsheets().values()
                .update(LMUsageConstants.SPREADSHEET_ID, rangeForUsageCountCells, usageCountCells)
                .setValueInputOption("RAW")  // Use "RAW" for unformatted values, "USER_ENTERED" to respect formatting
                .execute();

        logger.info("Pre-requisites Data written successfully for default cell values for NoOfDays");
    }


    /**
     * This method is used to add pre-requisites default count for usagecount column  in the sheet
     *
     * @param sheetName : The name of the sheet to add pre-requisites data
     * @throws GeneralSecurityException : If the credentials.json file cannot be found.
     * @throws IOException              : If the credentials.json file cannot be found.
     */
    public void addDefaultDataToUsageCountColumn(String sheetName) throws GeneralSecurityException, IOException {

        // Get the Google Sheets service
        Sheets sheetsService = GoogleSheetService.getSheetsService();

        int totalDays = getTotalDaysInCurrentMonth();

        // Prepare the list of values (one for each row in the specified range)
        List<List<Object>> usageCountDefaultValues = new ArrayList<>();
        for (int i = 0; i < totalDays; i++) {  // For example, updating rows of total number of days of a// current month
            usageCountDefaultValues.add(List.of(0));  // Each row gets the same value
        }

        String rangeForUsageCountCells = sheetName + "!B2:B" + (totalDays + 1);  // The cell to update
        logger.info("Value of usagecount cells : " + rangeForUsageCountCells);

        // Create the new value to insert in the cell
        ValueRange usageCountCells = new ValueRange().setValues(usageCountDefaultValues);

        // Update the cell value using the Sheets API
        sheetsService.spreadsheets().values()
                .update(LMUsageConstants.SPREADSHEET_ID, rangeForUsageCountCells, usageCountCells)
                .setValueInputOption("RAW")  // Use "RAW" for unformatted values, "USER_ENTERED" to respect formatting
                .execute();

        logger.info("Pre-requisites Data written successfully for default cell values for Usage count");
    }


    /**
     * This method is used to get the SheetName
     *
     * @param featureName : The name of the feature to find the sheet
     * @return sheetName : The name of the sheet to read/update usage details
     */
    public String getSheetName(String featureName) {
        String sheetName = "";
        String prefix = LMUsageLib.attachEnvAsSheetPrefix();
        int currentDay = getCurrentDayInCurrentMonth();

        if (currentDay < 3) {
            int month = (currentdate.getMonth().getValue()) - 1;
            LocalDate date = LocalDate.of(getCurrentYear(), month, 1);
            sheetName = prefix + "-" + featureName + "-" + date.getMonth() + "-" + getCurrentYear();
        } else {
            sheetName = prefix + "-" + featureName + "-" + getCurrentMonth() + "-" + getCurrentYear();
        }

        logger.info("Sheet name used to update/read usage count - " + sheetName);
        return sheetName;
    }


    /**
     * This method is used to update the usage count in the sheet
     *
     * @param featureName : The name of the feature to find the sheet
     * @param usageCount  : The usage count to update in the sheet
     * @throws GeneralSecurityException : If the credentials.json file cannot be found.
     * @throws IOException              : If the credentials.json file cannot be found.
     */
    public void updateUsageCountInSheet(String featureName, int usageCount) throws GeneralSecurityException, IOException {

        //To get currentDay in current month
        int currentDay = getCurrentDayInCurrentMonth();

        // Get the Google Sheets service
        Sheets sheetsService = GoogleSheetService.getSheetsService();

        // Define the range you want to update (e.g., "Sheet1!A1" for cell A1)
        String sheetName = getSheetName(featureName);
        logger.info("Sheet name used to update usage count - " + sheetName);

        String RANGE = sheetName + "!B" + (currentDay + 1);  // Replace with the actual range you want to update

        // Create the new value to insert in the cell
        int existingUsage = Integer.parseInt(GoogleSheetService.getCellValue(RANGE));
        logger.info("Existing usage for a cell " + RANGE + " is : " + existingUsage);
        int updatedUsageCount = 0;

        if (existingUsage > 0) {
            updatedUsageCount = existingUsage + usageCount;
        } else {
            updatedUsageCount = usageCount;
        }

        // Insert the updated usage value  in the cell
        ValueRange body = new ValueRange()
                .setValues(Arrays.asList(
                        Arrays.asList(updatedUsageCount)  // The value to put in the cell
                ));

        // Update the cell value using the Sheets API
        sheetsService.spreadsheets().values()
                .update(LMUsageConstants.SPREADSHEET_ID, RANGE, body)
                .setValueInputOption("RAW")  // Use "RAW" for unformatted values, "USER_ENTERED" to respect formatting
                .execute();

        // Print confirmation
        logger.info("Cell " + RANGE + " updated with value: 'Updated Value'" + usageCount);
    }

    /**
     * This method is used to validate the current month usage count
     *
     * @param sheetName : The name of the sheet to read the usage count
     * @return totalUsage : The total usage count in the current month
     * @throws GeneralSecurityException : If the credentials.json file cannot be found.
     * @throws IOException              : If the credentials.json file cannot be found.
     */
    public double validateCurrentMonthUsageCount(String sheetName) throws GeneralSecurityException, IOException {

        // Get the Google Sheets service
        Sheets sheetsService = GoogleSheetService.getSheetsService();
        int currentDay = getCurrentDayInCurrentMonth();
        String RANGE;
        double totalUsage = 0;

        int month = currentdate.getMonth().getValue();
        LocalDate date = LocalDate.of(getCurrentYear(), month, 1);
        int daysInMonth = date.lengthOfMonth();

        RANGE = sheetName + "!B2:B" + (currentDay - 1);  // The cell to read;
        logger.info("Value of Range for validating current month total usage count : " + RANGE);

        // Read data from the specified range
        ValueRange response = sheetsService.spreadsheets().values()
                .get(LMUsageConstants.SPREADSHEET_ID, RANGE)
                .execute();

        // Extract values from the response
        List<List<Object>> values = response.getValues();

        // Check if there are any values
        if (values == null || values.isEmpty()) {
            logger.info("No data found.");
        } else {
            // Loop through the data and print each row
            logger.info("Data exists in Spread Sheet");
            for (List row : values) {
                logger.info(row.get(0));
                totalUsage = totalUsage + Double.parseDouble((String) row.get(0));
                logger.info("sum count is in continuation " + totalUsage);
            }
        }

        logger.info("Final Usage count is " + totalUsage);
        return totalUsage;
    }

    /**
     * This method is used to update cell of Usage count for accuracy of rolling average calculation
     *
     * @param sheetName : The name of the sheet to read the usage count
     * @return totalUsage : The total usage count in the current month
     */
    public void updateUsageCountToCountRollingAvg(String sheetName)  {

        // Get the Google Sheets service
        Sheets sheetsService = null;
        try{
            sheetsService = GoogleSheetService.getSheetsService();
            String RANGE = sheetName + "!B2:B" + (getCurrentDayInCurrentMonth() - 2);  // The cell to read;
            logger.info("Value of Range for validating current month total usage count : " + RANGE);

            // Read data from the specified range
            ValueRange response = sheetsService.spreadsheets().values()
                    .get(LMUsageConstants.SPREADSHEET_ID, RANGE)
                    .execute();
            logger.info("response is - " + response);
            // Extract values from the response
            List<List<Object>> values = response.getValues();
            logger.info("fetched values from sheet are - " + values);

            List<Integer> updateUsageCountCells = new ArrayList<>();

            // Check if there are any values
            if (values == null || values.isEmpty()) {
                logger.info("No data found during update usage count to count RollingAvg under sheet - " + sheetName);
            } else {
                // Loop through the data and print each row
                for (List<Object> innerList : values) {
                    logger.info("List<Object> innerList is : " + innerList);
                    for (Object obj : innerList) {
                        updateUsageCountCells.add(Integer.parseInt((String) obj));
                    }
                }
                logger.info("updateUsageCountCells - " + updateUsageCountCells);
            }

            for ( int i = (updateUsageCountCells.size()-1) ; i>= 0 ; i--){
                if(updateUsageCountCells.get(i)!=0){
                    logger.info("list.get("+i+") is : " + updateUsageCountCells.get(i));
                    int updatedValue = updateUsageCountCells.get(i);
                    for ( int index = i ; index <= (updateUsageCountCells.size()-1) ; index ++){
                        logger.info("value of index is : " + index+ " whose updatedValue - " + updatedValue);
                        updateUsageCountCells.set(index,updatedValue);

                        //Update respective cell in sheet with updated value
                        String rangeForCleanup = sheetName + "!B" + (index+2);
                        logger.info("Update cell value for : " + rangeForCleanup +" with value = " + updatedValue);
                        // Create the ValueRange object and set the values
                        ValueRange body = new ValueRange()
                                .setValues(Collections.singletonList(Collections.singletonList(updatedValue)));  // Single cell update

                        // Write the data to the spreadsheet
                        sheetsService.spreadsheets().values()
                                .update(LMUsageConstants.SPREADSHEET_ID, rangeForCleanup, body)
                                .setValueInputOption("RAW")
                                .execute();
                    }
                    logger.info("break done");
                    break;
                }
            }
            logger.info("updated list values :  " + updateUsageCountCells);
            updateUsageCountCells.clear();
            values.clear();

        }catch(IOException | GeneralSecurityException e){
            logger.error("An error occurred while updating Usage Sheets service: " + e.getMessage());
        }catch (Exception e){
            logger.error("Some error has occurred " + e.getMessage());
        }
    }



    /**
     * This method is used to add pre-requisites default count for rolling sum column  in the sheet
     *
     * @param sheetName : The name of the sheet to add pre-requisites data
     * @param sheetColumn : The column to add pre-requisites data
     */
    public void addDefaultDataToColumn(String sheetName,String sheetColumn) {

        // Get the Google Sheets service
        try{
            Sheets sheetsService = GoogleSheetService.getSheetsService();

            int totalDays = getTotalDaysInCurrentMonth();

            // Prepare the list of values (one for each row in the specified range)
            List<List<Object>> usageCountDefaultValues = new ArrayList<>();
            for (int i = 0; i < totalDays; i++) {  // For example, updating rows of total number of days of a// current month
                usageCountDefaultValues.add(List.of(0));  // Each row gets the same value
            }

            String rangeForUsageCountCells = sheetName + "!"+sheetColumn+ "2:"+sheetColumn + (totalDays + 1);  // The cell to update
            logger.info("Value of usagecount cells : " + rangeForUsageCountCells);

            // Create the new value to insert in the cell
            ValueRange usageCountCells = new ValueRange().setValues(usageCountDefaultValues);

            // Update the cell value using the Sheets API
            sheetsService.spreadsheets().values()
                    .update(LMUsageConstants.SPREADSHEET_ID, rangeForUsageCountCells, usageCountCells)
                    .setValueInputOption("RAW")  // Use "RAW" for unformatted values, "USER_ENTERED" to respect formatting
                    .execute();

            logger.info("Pre-requisites Data written successfully for default cell values for Rolling sum");
        }catch (GeneralSecurityException | IOException e){
            logger.error("An error occurred while add default data to column for product using SUM: " + e.getMessage());
        }

    }

    /**
     * This method is used to add pre-requisites data for headers  in the sheet
     *
     * @param sheetName : The name of the sheet to add pre-requisites data
     * @param header1 : The header1 to add in the sheet
     * @param header2 : The header2 to add in the sheet
     * @param header3 : The header3 to add in the sheet
     * @param header4 : The header4 to add in the sheet
     */
    public void addDefaultHeaderData(String sheetName, String header1, String header2,String header3, String header4)
            {

        try{
            // Get the Google Sheets service
            Sheets sheetsService = GoogleSheetService.getSheetsService();

            // Create data to write as header
            List<List<Object>> headerValues = Arrays.asList(
                    Arrays.asList(header1, header2, header3, header4)
            );

            // Create the ValueRange object and set the values
            ValueRange header = new ValueRange().setValues(headerValues);

            String rangeForHeaders = sheetName + "!A1:D" + getTotalDaysInCurrentMonth();
            logger.info("Value of Range on creating a sheet if not exists : " + rangeForHeaders);

            // Write the data to the spreadsheet
            sheetsService.spreadsheets().values()
                    .update(LMUsageConstants.SPREADSHEET_ID, rangeForHeaders, header)
                    .setValueInputOption("RAW")
                    .execute();

            logger.info("Pre-requisites Data written successfully for usage sheet headers");
        }catch (GeneralSecurityException | IOException e){
            logger.error("An error occurred while add default data to column for product using ROLLING AVG: " + e.getMessage());
        }

    }

    /**
     * This method is used to add pre-requisites headers data for product using rolling average   in the sheet
     *
     * @param sheetName : The name of the sheet to add pre-requisites data
     */
    public void addDefaultDataInSheetUsedRollingAvg(String sheetName) {
        //Add pre-requisites data for Days Column
        try {
            addDefaultHeaderData(sheetName, "No of Days", "Usage Count", "Rolling SUM", "Rolling AVG");
            addDefaultDaysColumn(sheetName);
            addDefaultDataToColumn(sheetName, "B");
            addDefaultDataToColumn(sheetName, "C");
            addDefaultDataToColumn(sheetName, "D");
        } catch (GeneralSecurityException | IOException e) {
            logger.error("An error occurred from common function add default data to column for product using ROLLING AVG: " + e.getMessage());
        }
    }
}