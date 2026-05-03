package tn.rouhfan.ui.front;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import tn.rouhfan.entities.Reclamation;
import tn.rouhfan.entities.ReponseReclamation;
import tn.rouhfan.services.ReclamationService;
import tn.rouhfan.services.ReponseReclamationService;
import tn.rouhfan.tools.SessionManager;

import javafx.stage.FileChooser;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ReclamationFrontController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statutFilter;

    @FXML private TableView<Reclamation> table;
    @FXML private TableColumn<Reclamation, String> colSujet;
    @FXML private TableColumn<Reclamation, String> colDesc;
    @FXML private TableColumn<Reclamation, Date> colDate;
    @FXML private TableColumn<Reclamation, String> colStatut;
    @FXML private TableColumn<Reclamation, Void> colActionImg;

    private ReclamationService rs = new ReclamationService();
    private ObservableList<Reclamation> list;

    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

    @FXML
    public void initialize() {

        // ================= TABLE =================
        colSujet.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getSujet() != null ? data.getValue().getSujet() : ""
                ));

        colDesc.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getDescription() != null ? data.getValue().getDescription() : ""
                ));

        colDate.setCellValueFactory(data ->
                new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getCreatedAt())
        );

        colDate.setCellFactory(col -> new TableCell<Reclamation, Date>() {
            @Override
            protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : sdf.format(item));
            }
        });

        colStatut.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getStatut() != null ? data.getValue().getStatut() : ""
                ));

        colActionImg.setCellFactory(param -> new TableCell<Reclamation, Void>() {
            private final Button btn = new Button("🖼️ Voir image");
            {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2980b9; -fx-cursor: hand; -fx-font-weight: bold;");
                btn.setOnAction((ActionEvent event) -> {
                    Reclamation selected = getTableView().getItems().get(getIndex());
                    if (selected.getImagePath() == null || selected.getImagePath().isEmpty()) {
                        showAlert("Info", "Aucune image disponible");
                        return;
                    }

                    File imgFile = new File("uploads/reclamations/" + selected.getImagePath());
                    if (!imgFile.exists()) {
                        showAlert("Info", "Aucune image disponible");
                        return;
                    }

                    Dialog<Void> dialog = new Dialog<>();
                    dialog.initModality(javafx.stage.Modality.NONE); // Bouton minimiser
                    dialog.setResizable(true); // Bouton agrandir
                    dialog.setTitle("Pièce Jointe - " + selected.getSujet());
                    dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
                    dialog.getDialogPane().setStyle("-fx-background-color: #f8f9fa; -fx-padding: 20;");

                    Image image = new Image(imgFile.toURI().toString());
                    ImageView imageView = new ImageView(image);
                    imageView.setPreserveRatio(true);
                    imageView.setFitWidth(600);
                    imageView.setFitHeight(500);

                    imageView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 5);");

                    VBox box = new VBox(15, imageView);
                    box.setStyle("-fx-alignment: center; -fx-background-radius: 10; -fx-padding: 10; -fx-background-color: white;");
                    dialog.getDialogPane().setContent(box);

                    // Styliser le bouton fermer en rouge pour être cohérent
                    javafx.scene.Node closeBtn = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
                    if (closeBtn != null) {
                        closeBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8px 20px; -fx-background-radius: 5px;");
                    }

                    dialog.show();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btn);
                }
            }
        });

        // 🔽 TRI PAR DÉFAUT (date desc)
        colDate.setSortType(TableColumn.SortType.DESCENDING);
        table.getSortOrder().add(colDate);

        // ================= FILTER =================
        statutFilter.setItems(FXCollections.observableArrayList(
                "Tous", "en_attente", "en_cours", "traite"
        ));
        statutFilter.setValue("Tous");

        loadData();
    }

    // ================= LOAD DATA =================
    private void loadData() {
        try {
            tn.rouhfan.entities.User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                list = FXCollections.observableArrayList(rs.recupererParUser(currentUser.getId()));
            } else {
                list = FXCollections.observableArrayList(rs.recuperer());
            }

            FilteredList<Reclamation> filtered = new FilteredList<>(list, b -> true);

            Runnable updateFilter = () -> {
                String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
                String statut = statutFilter.getValue();

                filtered.setPredicate(r -> {

                    boolean matchSearch =
                            (r.getSujet() != null && r.getSujet().toLowerCase().contains(search))
                                    || (r.getDescription() != null && r.getDescription().toLowerCase().contains(search));

                    boolean matchStatut =
                            statut.equals("Tous") ||
                                    (r.getStatut() != null && r.getStatut().equalsIgnoreCase(statut));

                    return matchSearch && matchStatut;
                });
            };

            searchField.textProperty().addListener((obs, o, n) -> updateFilter.run());
            statutFilter.valueProperty().addListener((obs, o, n) -> updateFilter.run());

            SortedList<Reclamation> sorted = new SortedList<>(filtered);
            sorted.comparatorProperty().bind(table.comparatorProperty());

            table.setItems(sorted);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ================= POPUP AJOUT =================
    @FXML
    private void openPopupAjout(ActionEvent event) {

        // 🔐 Vérifier user connecté
        if (SessionManager.getInstance().getCurrentUser() == null) {
            showAlert("Erreur", "Vous devez être connecté !");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initModality(javafx.stage.Modality.NONE); // Permet d'avoir le bouton minimiser (-)
        dialog.setResizable(true); // Permet d'avoir le bouton agrandir (carreau)
        dialog.setTitle("Créer une réclamation");
        dialog.setHeaderText(null);

        Label titleLabel = new Label("Créer une réclamation");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #241197;");

        TextField sujet = new TextField();
        sujet.setPromptText("Sujet");
        sujet.setStyle("-fx-font-size: 14px; -fx-padding: 10px; -fx-background-radius: 5px;");

        TextArea desc = new TextArea();
        desc.setPromptText("Écrire votre description...");
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 14px; -fx-background-radius: 5px;");
        javafx.scene.layout.VBox.setVgrow(desc, javafx.scene.layout.Priority.ALWAYS);

        Button btnImage = new Button("🖼️ Ajouter photos");
        btnImage.setStyle("-fx-background-color: white; -fx-text-fill: #2980b9; -fx-cursor: hand; -fx-font-weight: bold; -fx-border-color: #bdc3c7; -fx-border-radius: 5px; -fx-background-radius: 5px; -fx-padding: 5px 10px;");
        btnImage.setTooltip(new Tooltip("Joindre une image"));

        Label imgLabel = new Label();
        imgLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic; -fx-font-size: 12px;");

        final File[] selectedImage = new File[1];

        btnImage.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Sélectionnez une image");
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );
            File f = fc.showOpenDialog(null);
            if (f != null) {
                selectedImage[0] = f;
                imgLabel.setText("Fichier : " + f.getName());
            }
        });

        javafx.scene.layout.HBox imageBox = new javafx.scene.layout.HBox(10, btnImage, imgLabel);
        imageBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox box = new VBox(15, titleLabel, sujet, desc, imageBox);
        box.setPrefSize(500, 400);
        box.setPadding(new javafx.geometry.Insets(20));
        box.setStyle("-fx-background-color: #f4f7f6;");

        dialog.getDialogPane().setContent(box);

        ButtonType btnEnvoyerType = new ButtonType("Envoyer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnEnvoyerType, ButtonType.CANCEL);

        javafx.scene.Node btnEnvoyer = dialog.getDialogPane().lookupButton(btnEnvoyerType);
        if (btnEnvoyer != null) {
            btnEnvoyer.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8px 20px; -fx-background-radius: 5px;");
        }
        javafx.scene.Node btnCancel = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (btnCancel != null) {
            btnCancel.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8px 20px; -fx-background-radius: 5px;");
        }

        dialog.showAndWait().ifPresent(response -> {
            if (response == btnEnvoyerType) {

                String sujetTxt = sujet.getText().trim();
                String descTxt = desc.getText().trim();

                // ================= VALIDATION =================

                if (sujetTxt.isEmpty() || descTxt.isEmpty()) {
                    showAlert("Erreur", "Champs vides !");
                    return;
                }

                if (sujetTxt.length() < 3) {
                    showAlert("Erreur", "Sujet trop court (min 3 caractères)");
                    return;
                }

                if (descTxt.length() < 5) {
                    showAlert("Erreur", "Description trop courte (min 5 caractères)");
                    return;
                }

                if (descTxt.length() > 500) {
                    showAlert("Erreur", "Message trop long !");
                    return;
                }

                if (!sujetTxt.matches("[a-zA-ZÀ-ÿ\\s]+")) {
                    showAlert("Erreur", "Sujet invalide (lettres seulement)");
                    return;
                }

                if (containsInsult(sujetTxt) || containsInsult(descTxt)) {
                    Dialog<ButtonType> warnDialog = new Dialog<>();
                    warnDialog.setTitle("Langage inapproprié");
                    warnDialog.setHeaderText(null);

                    Label icon = new Label("⚠️");
                    icon.setStyle("-fx-font-size: 40px;");

                    Label title = new Label("Message non conforme");
                    title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");

                    Label msg = new Label("Votre message contient des termes inappropriés.\nMerci de reformuler votre demande avec un ton respectueux.\n\n😊 Nous restons disponibles pour vous aider.");
                    msg.setStyle("-fx-font-size: 14px; -fx-text-fill: #34495e;");
                    msg.setWrapText(true);
                    msg.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

                    javafx.scene.layout.VBox warnBox = new javafx.scene.layout.VBox(15, icon, title, msg);
                    warnBox.setAlignment(javafx.geometry.Pos.CENTER);
                    warnBox.setPadding(new javafx.geometry.Insets(20));
                    warnBox.setStyle("-fx-background-color: white; -fx-background-radius: 10px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

                    warnDialog.getDialogPane().setContent(warnBox);

                    ButtonType comprisType = new ButtonType("Compris", ButtonBar.ButtonData.OK_DONE);
                    warnDialog.getDialogPane().getButtonTypes().add(comprisType);

                    javafx.scene.Node btnCompris = warnDialog.getDialogPane().lookupButton(comprisType);
                    if (btnCompris != null) {
                        btnCompris.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8px 30px; -fx-background-radius: 5px;");
                    }

                    warnDialog.showAndWait();
                    return;
                }

                try {
                    // 🔥 récupérer user connecté
                    int userId = SessionManager
                            .getInstance()
                            .getCurrentUser()
                            .getId();

                    Reclamation r = new Reclamation(
                            sujetTxt,
                            descTxt,
                            "en_attente",
                            new Date(),
                            userId,
                            "autre"
                    );

                    String uniqueImageName = null;
                    if (selectedImage[0] != null) {
                        uniqueImageName = java.util.UUID.randomUUID().toString() + "_" + selectedImage[0].getName();
                        r.setImagePath(uniqueImageName);
                    }

                    rs.ajouter(r);

                    // 🔥 Sauvegarde Image locale
                    if (selectedImage[0] != null && uniqueImageName != null) {
                        try {
                            File uploadDir = new File("uploads/reclamations");
                            if (!uploadDir.exists()) uploadDir.mkdirs();

                            File dest = new File(uploadDir, uniqueImageName);
                            Files.copy(selectedImage[0].toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        } catch (Exception ex) {
                            System.err.println("Erreur copie image: " + ex.getMessage());
                        }
                    }

                    loadData();

                    showAlert("Succès", "Réclamation ajoutée ✅");

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // ================= VOIR REPONSES =================
    @FXML
    private void voirReponses(ActionEvent event) {

        Reclamation selected = table.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Erreur", "Sélectionner une réclamation");
            return;
        }

        try {
            ReponseReclamationService service = new ReponseReclamationService();
            List<ReponseReclamation> list = service.recuperer();

            Dialog<Void> dialog = new Dialog<>();
            dialog.initModality(javafx.stage.Modality.NONE); // Permet d'avoir le bouton minimiser (-)
            dialog.setResizable(true); // Permet d'avoir le bouton agrandir (carreau)
            dialog.setTitle("Réponse à votre réclamation");
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.getDialogPane().setStyle("-fx-background-color: white; -fx-padding: 0;");

            Label titleLabel = new Label("Réponse à votre réclamation");
            titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #241197; -fx-padding: 20 20 10 20;");

            VBox messagesBox = new VBox(15);
            messagesBox.setStyle("-fx-background-color: white; -fx-padding: 20;");

            // Affichage des réponses (Admin)
            boolean hasResponse = false;
            for (ReponseReclamation r : list) {
                if (r.getReclamationId() == selected.getId()) {
                    hasResponse = true;

                    VBox adminBlock = new VBox(8);
                    adminBlock.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #e0e0e0; -fx-border-radius: 5px; -fx-background-radius: 5px; -fx-padding: 15px;");

                    Label adminDate = new Label("Le " + sdf.format(r.getCreatedAt()));
                    adminDate.setStyle("-fx-text-fill: #777777; -fx-font-size: 12px;");

                    Label adminMsg = new Label(r.getMessage());
                    adminMsg.setWrapText(true);
                    adminMsg.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333; -fx-line-spacing: 5px;");

                    adminBlock.getChildren().addAll(adminDate, adminMsg);
                    messagesBox.getChildren().add(adminBlock);
                }
            }

            if (!hasResponse) {
                Label noRep = new Label("Votre réclamation est en cours de traitement. Aucune réponse pour le moment.");
                noRep.setStyle("-fx-text-fill: #555555; -fx-font-style: italic; -fx-font-size: 12px;");
                messagesBox.getChildren().add(noRep);
            }

            ScrollPane scrollPane = new ScrollPane(messagesBox);
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background: white; -fx-border-color: transparent;");
            scrollPane.setPrefViewportHeight(250);

            VBox mainBox = new VBox(titleLabel, scrollPane);
            mainBox.setPrefSize(500, 350);
            mainBox.setStyle("-fx-background-color: white;");

            dialog.getDialogPane().setContent(mainBox);

            // Styliser le bouton fermer
            javafx.scene.Node closeBtn = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
            if (closeBtn != null) {
                closeBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8px 20px; -fx-background-radius: 5px;");
            }

            dialog.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= ALERT =================
    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private boolean containsInsult(String txt) {
        if (txt == null) return false;
        String lower = txt.toLowerCase();
        String[] badWords = {"ridicule", "null", "irresponsable", "arrogant", "malhonnête", "imbécile", "idiot", "stupide", "désagréable", "foutre"};
        for (String word : badWords) {
            if (lower.matches(".*\\b" + word + "\\b.*")) {
                return true;
            }
        }
        return false;
    }


}