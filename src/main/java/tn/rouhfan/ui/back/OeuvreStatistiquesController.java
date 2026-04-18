package tn.rouhfan.ui.back;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import tn.rouhfan.entities.Oeuvre;
import tn.rouhfan.services.OeuvreService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormatSymbols;
import java.util.*;
import java.util.stream.Collectors;

public class OeuvreStatistiquesController {

    @FXML private ComboBox<Integer> yearFilter;
    @FXML private ComboBox<String> monthFilter;
    @FXML private Label totalArtworksLabel;
    @FXML private Label totalSoldLabel;
    @FXML private Label monthSoldLabel;
    @FXML private Label monthSoldTitle;
    @FXML private Label pieChartTitle;
    @FXML private Label lineChartTitle;
    @FXML private Label tableTitle;
    
    @FXML private PieChart categoryPieChart;
    @FXML private BarChart<Number, String> categoryBarChart;
    @FXML private LineChart<String, Number> salesTrendChart;
    
    @FXML private TableView<CategoryStat> categoryTable;
    @FXML private TableColumn<CategoryStat, String> catNameCol;
    @FXML private TableColumn<CategoryStat, Integer> catCountCol;

    private final OeuvreService oeuvreService = new OeuvreService();
    private List<Oeuvre> allOeuvres = new ArrayList<>();
    private final String[] monthNames = new DateFormatSymbols(Locale.FRENCH).getMonths();

    public void initialize() {
        setupFilters();
        setupTable();
        refreshStats(null);
        
        // Auto-refresh when filters change
        yearFilter.valueProperty().addListener((obs, oldVal, newVal) -> updateDashboard());
        monthFilter.valueProperty().addListener((obs, oldVal, newVal) -> updateDashboard());
    }

    private void setupFilters() {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        ObservableList<Integer> years = FXCollections.observableArrayList();
        for (int i = currentYear; i >= 2020; i--) {
            years.add(i);
        }
        yearFilter.setItems(years);
        yearFilter.setValue(currentYear);

        ObservableList<String> months = FXCollections.observableArrayList();
        months.add("Tous les mois");
        for (int i = 0; i < 12; i++) {
            months.add(monthNames[i]);
        }
        monthFilter.setItems(months);
        monthFilter.setValue("Tous les mois");
    }

    private void setupTable() {
        catNameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        catCountCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getCount()).asObject());
        
        // Align text to center for counts
        catCountCol.setStyle("-fx-alignment: CENTER;");
    }

    @FXML
    public void refreshStats(ActionEvent event) {
        try {
            allOeuvres = oeuvreService.recuperer();
            updateDashboard();
        } catch (SQLException e) {
            showError("Erreur de chargement", "Impossible de récupérer les données : " + e.getMessage());
        }
    }

    private void updateDashboard() {
        int selectedYear = yearFilter.getValue();
        int selectedMonthIndex = monthFilter.getSelectionModel().getSelectedIndex();
        String selectedMonthName = monthFilter.getValue();

        // 1. Update Labels
        totalArtworksLabel.setText(String.valueOf(allOeuvres.size()));
        
        List<Oeuvre> soldOeuvres = allOeuvres.stream()
                .filter(o -> "vendue".equalsIgnoreCase(o.getStatut()))
                .collect(Collectors.toList());
        
        totalSoldLabel.setText(String.valueOf(soldOeuvres.size()));
        
        Calendar now = Calendar.getInstance();
        int currentYear = now.get(Calendar.YEAR);
        int currentMonth = now.get(Calendar.MONTH);

        List<Oeuvre> periodSoldOeuvres = soldOeuvres.stream()
                .filter(o -> {
                    if (o.getDateVente() == null) {
                        // Include historical sales in the current year and current month (or All Months)
                        return selectedYear == currentYear && (selectedMonthIndex == 0 || selectedMonthIndex == (currentMonth + 1));
                    }
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(o.getDateVente());
                    boolean yearMatch = cal.get(Calendar.YEAR) == selectedYear;
                    if (!yearMatch) return false;
                    
                    if (selectedMonthIndex == 0) return true; // "Tous les mois"
                    return cal.get(Calendar.MONTH) == (selectedMonthIndex - 1);
                })
                .collect(Collectors.toList());
        
        monthSoldLabel.setText(String.valueOf(periodSoldOeuvres.size()));
        String periodText = (selectedMonthIndex == 0) ? "Vendues en " + selectedYear : "Vendues en " + selectedMonthName + " " + selectedYear;
        monthSoldTitle.setText(periodText);
        
        // Titles
        pieChartTitle.setText("Répartition par catégorie — " + (selectedMonthIndex == 0 ? selectedYear : selectedMonthName + " " + selectedYear));
        lineChartTitle.setText("Tendance des ventes — " + selectedYear);
        tableTitle.setText("Détail par catégorie — " + (selectedMonthIndex == 0 ? selectedYear : selectedMonthName + " " + selectedYear));

        // 2. Pie Chart & Bar Chart (Period stats by category)
        Map<String, Long> catStats = periodSoldOeuvres.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getCategorie() != null ? o.getCategorie().getNomCategorie() : "Inconnu",
                        Collectors.counting()
                ));

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        XYChart.Series<Number, String> barSeries = new XYChart.Series<>();
        barSeries.setName("Ventes");
        
        List<CategoryStat> tableData = new ArrayList<>();
        
        catStats.forEach((name, count) -> {
            pieData.add(new PieChart.Data(name, count));
            barSeries.getData().add(new XYChart.Data<>(count, name));
            tableData.add(new CategoryStat(name, count.intValue()));
        });

        categoryPieChart.setData(pieData);
        categoryBarChart.getData().setAll(barSeries);
        
        // Custom styling for charts to match premium look
        applyCustomChartColors();
        
        categoryTable.setItems(FXCollections.observableArrayList(tableData));

        // 3. Line Chart (Yearly trend)
        XYChart.Series<String, Number> lineSeries = new XYChart.Series<>();
        lineSeries.setName("Ventes en " + selectedYear);
        
        Map<Integer, Long> trendStats = soldOeuvres.stream()
                .filter(o -> {
                    if (o.getDateVente() == null) {
                        // For artworks with null date, we check the year filter
                        // If we are in the selected year, we'll put them in the current month for demo purposes
                        return selectedYear == Calendar.getInstance().get(Calendar.YEAR);
                    }
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(o.getDateVente());
                    return cal.get(Calendar.YEAR) == selectedYear;
                })
                .collect(Collectors.groupingBy(
                        o -> {
                            if (o.getDateVente() == null) return Calendar.getInstance().get(Calendar.MONTH);
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(o.getDateVente());
                            return cal.get(Calendar.MONTH);
                        },
                        Collectors.counting()
                ));

        for (int i = 0; i < 12; i++) {
            lineSeries.getData().add(new XYChart.Data<>(monthNames[i].substring(0, 3), trendStats.getOrDefault(i, 0L)));
        }
        salesTrendChart.getData().setAll(lineSeries);
    }

    @FXML
    private void handleExportExcel() {
        List<Oeuvre> soldOeuvres = allOeuvres.stream()
                .filter(o -> "vendue".equalsIgnoreCase(o.getStatut()))
                .collect(Collectors.toList());

        if (soldOeuvres.isEmpty()) {
            showInfo("Aucune donnée", "Il n'y a aucune œuvre vendue à exporter.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le rapport Excel");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        fileChooser.setInitialFileName("Rapport_Ventes_Art_Fantasy.xlsx");
        
        File file = fileChooser.showSaveDialog(yearFilter.getScene().getWindow());
        if (file == null) return;

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Œuvres vendues");

            // Styles
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.INDIGO.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            // Title Row
            Row titleRow = sheet.createRow(1);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(1);
            titleCell.setCellValue("Œuvres les plus vendues — Rouh el Fann");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 7));

            // Header Row
            Row headerRow = sheet.createRow(3);
            String[] headers = {"N°", "Titre", "Prix (DT)", "Catégorie", "Artiste", "Date vente", "Id"};
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i + 1);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data Rows
            int rowNum = 4;
            int count = 1;
            for (Oeuvre o : soldOeuvres) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(1).setCellValue(count++);
                row.createCell(2).setCellValue(o.getTitre());
                row.createCell(3).setCellValue(o.getPrix() != null ? o.getPrix().doubleValue() : 0.0);
                row.createCell(4).setCellValue(o.getCategorie() != null ? o.getCategorie().getNomCategorie() : "N/A");
                row.createCell(5).setCellValue(o.getUser() != null ? o.getUser().getNom() + " " + o.getUser().getPrenom() : "Inconnu");
                row.createCell(6).setCellValue(o.getDateVente() != null ? o.getDateVente().toString() : "-");
                row.createCell(7).setCellValue(o.getId());
                
                // Optional: add some body style
            }

            // Auto-size columns
            for (int i = 1; i <= 7; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                workbook.write(fileOut);
            }
            showInfo("Succès", "Le rapport Excel a été généré avec succès !");
            
        } catch (IOException e) {
            showError("Erreur d'export", "Impossible de générer le fichier Excel : " + e.getMessage());
        }
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void applyCustomChartColors() {
        String[] colors = {"#241197", "#6c2a90", "#c9a849", "#e4c76a", "#7d78ff", "#a29bfe", "#d1d5db"};
        
        // Color PieChart and sync with legend
        int i = 0;
        for (PieChart.Data data : categoryPieChart.getData()) {
            String color = colors[i % colors.length];
            if (data.getNode() != null) {
                data.getNode().setStyle("-fx-pie-color: " + color + ";");
            }
            
            // Sync legend symbol color
            for (javafx.scene.Node node : categoryPieChart.lookupAll(".chart-legend-item")) {
                if (node instanceof Label && ((Label) node).getText().equals(data.getName())) {
                    javafx.scene.Node symbol = ((Label) node).getGraphic();
                    if (symbol != null) {
                        symbol.setStyle("-fx-background-color: " + color + ";");
                    }
                }
            }
            i++;
        }
        
        // Color BarChart
        if (!categoryBarChart.getData().isEmpty()) {
            XYChart.Series<Number, String> series = categoryBarChart.getData().get(0);
            int j = 0;
            for (XYChart.Data<Number, String> data : series.getData()) {
                if (data.getNode() != null) {
                    data.getNode().setStyle("-fx-bar-fill: " + colors[j % colors.length] + ";");
                }
                j++;
            }
        }
        
        // Style LineChart line and symbols
        if (!salesTrendChart.getData().isEmpty()) {
            javafx.scene.Node line = salesTrendChart.lookup(".chart-series-line");
            if (line != null) {
                line.setStyle("-fx-stroke: #241197; -fx-stroke-width: 3px;");
            }
        }
    }

    public static class CategoryStat {
        private final String name;
        private final int count;

        public CategoryStat(String name, int count) {
            this.name = name;
            this.count = count;
        }

        public String getName() { return name; }
        public int getCount() { return count; }
    }
}