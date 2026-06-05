package utilities;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * CSVManager handles all file-level read and write operations
 * using Apache Commons CSV.
 */
public class CSVManager {

    private static final Logger logger = Logger.getLogger(CSVManager.class.getName());

    // ---------------------------------------------------------------------------
    // FILE INITIALIZATION
    // ---------------------------------------------------------------------------

    /**
     * Creates the CSV file with headers if it does not exist.
     */
    private static void initializeFile(String filePath, String[] header) {
        Path path = Paths.get(filePath);

        if (Files.notExists(path)) {
            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
                 CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

                printer.printRecord((Object[]) header);

            } catch (IOException e) {
                logger.severe("Failed to initialize CSV file: " + e.getMessage());
            }
        }
    }

    // ---------------------------------------------------------------------------
    // READ
    // ---------------------------------------------------------------------------

    /**
     * Reads all employee rows excluding the header.
     */
    public static List<String[]> readAll(String filePath, int totalColumns) {

        List<String[]> rows = new ArrayList<>();

        try (
                Reader reader = Files.newBufferedReader(Paths.get(filePath), StandardCharsets.UTF_8);
                CSVParser parser = CSVFormat.DEFAULT
                        .builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .build()
                        .parse(reader)
        ) {

            for (CSVRecord record : parser) {
                String[] row = new String[totalColumns];

                for (int i = 0; i < totalColumns; i++) {
                    row[i] = i < record.size() ? record.get(i).trim() : "";
                }

                rows.add(row);
            }

        } catch (IOException e) {
            logger.severe("Failed to read CSV: " + e.getMessage());
        }

        return rows;
    }

    /**
     * Reads the CSV header row.
     */
    public static String[] readHeader(String filePath) {

        try (
                Reader reader = Files.newBufferedReader(Paths.get(filePath), StandardCharsets.UTF_8);
                CSVParser parser = CSVFormat.DEFAULT
                        .builder()
                        .setHeader()
                        .setSkipHeaderRecord(false)
                        .build()
                        .parse(reader)
        ) {

            Map<String, Integer> headerMap = parser.getHeaderMap();

            String[] headers = new String[headerMap.size()];

            for (Map.Entry<String, Integer> entry : headerMap.entrySet()) {
                headers[entry.getValue()] = entry.getKey();
            }

            return headers;

        } catch (IOException e) {
            logger.severe("Failed to read header: " + e.getMessage());
        }

        return new String[0];
    }

    // ---------------------------------------------------------------------------
    // APPEND
    // ---------------------------------------------------------------------------

    /**
     * Appends a new employee row.
     */
    public static boolean appendRow(String filePath, String[] row) {

        try (
                BufferedWriter writer = Files.newBufferedWriter(
                        Paths.get(filePath),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.APPEND
                );

                CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)
        ) {

            printer.printRecord((Object[]) row);
            return true;

        } catch (IOException e) {
            logger.severe("Failed to append row: " + e.getMessage());
            return false;
        }
    }

    // ---------------------------------------------------------------------------
    // UPDATE
    // ---------------------------------------------------------------------------

    /**
     * Updates an employee row based on Employee #.
     */
    public static boolean updateRow(String filePath, String[] updatedRow, int keyColumnIndex, int totalColumns) {

        List<String[]> rows = readAll(filePath, totalColumns);
        String[] header = readHeader(filePath);
        String key = updatedRow[keyColumnIndex].trim();
        boolean updated = false;

        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i)[keyColumnIndex].trim().equals(key)) {
                rows.set(i, updatedRow);
                updated = true;
                break;
            }
        }

        if (updated) {
            writeAll(filePath, header, rows);
        }

        return updated;
    }

    // ---------------------------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------------------------

    /**
     * Deletes an employee row based on Employee #.
     */
    public static boolean deleteRow(String filePath, String keyValue, int keyColumnIndex, int totalColumns) {

        List<String[]> rows = readAll(filePath, totalColumns);
        String[] header = readHeader(filePath);

        boolean deleted = rows.removeIf(
                row -> row[keyColumnIndex].trim().equals(keyValue.trim())
        );

        if (deleted) {
            writeAll(filePath, header, rows);
        }

        return deleted;
    }

    // ---------------------------------------------------------------------------
    // DUPLICATE CHECK
    // ---------------------------------------------------------------------------

    /**
     * Checks whether an Employee ID already exists.
     */
    public static boolean rowExists(String filePath, String keyValue, int keyColumnIndex, int totalColumns) {

        for (String[] row : readAll(filePath, totalColumns)) {

            if (row[keyColumnIndex].trim().equals(keyValue.trim())) {

                return true;
            }
        }

        return false;
    }

    // ---------------------------------------------------------------------------
    // INTERNAL WRITE HELPER
    // ---------------------------------------------------------------------------

    /**
     * Rewrites the entire CSV file.
     */
    private static void writeAll(
            String filePath,
            String[] header,
            List<String[]> rows
    ) {

        try (
                BufferedWriter writer = Files.newBufferedWriter(
                        Paths.get(filePath),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.TRUNCATE_EXISTING
                );

                CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)
        ) {

            // Write header
            printer.printRecord((Object[]) header);

            // Write rows
            for (String[] row : rows) {
                printer.printRecord((Object[]) row);
            }

        } catch (IOException e) {
            logger.severe("Failed to write CSV: " + e.getMessage());
        }
    }
}