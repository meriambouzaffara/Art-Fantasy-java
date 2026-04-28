package tn.rouhfan.ui.front;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.rouhfan.entities.Commentaire;
import tn.rouhfan.entities.Oeuvre;
import tn.rouhfan.entities.User;
import tn.rouhfan.services.CommentaireService;
import tn.rouhfan.services.OeuvreService;
import tn.rouhfan.tools.SessionManager;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class ArtistCommentsController {

    @FXML private ListView<Oeuvre> oeuvreListView;
    @FXML private VBox commentsContainer;
    @FXML private ScrollPane commentsScrollPane;
    @FXML private VBox replyArea;
    @FXML private Label replyingToLabel;
    @FXML private TextArea replyInput;
    @FXML private VBox placeholderBox;

    private OeuvreService oeuvreService = new OeuvreService();
    private CommentaireService commentService = new CommentaireService();
    private Commentaire activeCommentForReply = null;
    private Oeuvre selectedOeuvre = null;

    @FXML
    public void initialize() {
        setupOeuvreList();
        loadArtistOeuvres();
    }

    private void setupOeuvreList() {
        oeuvreListView.setCellFactory(lv -> new ListCell<Oeuvre>() {
            @Override
            protected void updateItem(Oeuvre item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox box = new VBox(5);
                    box.setPadding(new Insets(10));
                    Label title = new Label(item.getTitre());
                    title.setStyle("-fx-font-weight: bold; -fx-text-fill: #1c1e21;");
                    Label cat = new Label(item.getCategorie() != null ? item.getCategorie().getNomCategorie() : "Art");
                    cat.setStyle("-fx-font-size: 11; -fx-text-fill: #65676b;");
                    box.getChildren().addAll(title, cat);
                    setGraphic(box);
                }
            }
        });

        oeuvreListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedOeuvre = newVal;
                loadCommentsForOeuvre(newVal.getId());
                placeholderBox.setVisible(false);
                placeholderBox.setManaged(false);
            }
        });
    }

    private void loadArtistOeuvres() {
        User artist = SessionManager.getInstance().getCurrentUser();
        if (artist == null) return;

        try {
            List<Oeuvre> myOeuvres = oeuvreService.recupererParUser(artist.getId());
            oeuvreListView.setItems(FXCollections.observableArrayList(myOeuvres));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadCommentsForOeuvre(int oeuvreId) {
        commentsContainer.getChildren().clear();
        handleCancelReply();

        try {
            List<Commentaire> all = commentService.recupererParOeuvre(oeuvreId);
            
            // Group comments by parent
            List<Commentaire> mains = all.stream().filter(c -> c.getParentCommentId() == null).collect(Collectors.toList());
            
            for (Commentaire m : mains) {
                addCommentBubble(m, false);
                List<Commentaire> replies = all.stream().filter(r -> r.getParentCommentId() != null && r.getParentCommentId() == m.getId()).collect(Collectors.toList());
                for (Commentaire r : replies) {
                    addCommentBubble(r, true);
                }
            }
            
            Platform.runLater(() -> commentsScrollPane.setVvalue(1.0));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addCommentBubble(Commentaire c, boolean isReply) {
        VBox wrapper = new VBox(5);
        wrapper.setPadding(new Insets(0, 0, 0, isReply ? 40 : 0));
        
        HBox bubbleWrapper = new HBox(10);
        bubbleWrapper.setAlignment(Pos.TOP_LEFT);

        // Avatar Placeholder
        StackPane avatar = new StackPane();
        avatar.setPrefSize(32, 32);
        avatar.setStyle("-fx-background-color: #e4e6eb; -fx-background-radius: 50;");
        Label initial = new Label(c.getUserName().substring(0, 1).toUpperCase());
        initial.setStyle("-fx-font-weight: bold; -fx-text-fill: #65676b;");
        avatar.getChildren().add(initial);

        VBox bubble = new VBox(2);
        bubble.setPadding(new Insets(8, 12, 8, 12));
        bubble.setStyle("-fx-background-color: " + (isReply ? "#f0f2f5" : "#e4e6eb") + "; -fx-background-radius: 18;");
        bubble.setMaxWidth(400);

        Label name = new Label(c.getUserName());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");
        
        Label content = new Label(c.getContenu());
        content.setWrapText(true);
        content.setStyle("-fx-font-size: 13;");

        bubble.getChildren().addAll(name, content);
        bubbleWrapper.getChildren().addAll(avatar, bubble);

        // Actions (Reply)
        if (!isReply) {
            Hyperlink replyLink = new Hyperlink("Répondre");
            replyLink.setStyle("-fx-font-size: 11; -fx-text-fill: #65676b; -fx-padding: 0 0 0 45;");
            replyLink.setOnAction(e -> startReply(c));
            wrapper.getChildren().addAll(bubbleWrapper, replyLink);
        } else {
            wrapper.getChildren().add(bubbleWrapper);
        }

        commentsContainer.getChildren().add(wrapper);
    }

    private void startReply(Commentaire c) {
        this.activeCommentForReply = c;
        replyingToLabel.setText("Répondre à " + c.getUserName());
        replyArea.setVisible(true);
        replyArea.setManaged(true);
        replyInput.requestFocus();
    }

    @FXML
    private void handleCancelReply() {
        this.activeCommentForReply = null;
        replyArea.setVisible(false);
        replyArea.setManaged(false);
        replyInput.clear();
    }

    @FXML
    private void handlePostReply() {
        if (activeCommentForReply == null || replyInput.getText().trim().isEmpty()) return;

        User artist = SessionManager.getInstance().getCurrentUser();
        Commentaire reply = new Commentaire(replyInput.getText().trim(), selectedOeuvre.getId(), artist.getId());
        reply.setParentCommentId(activeCommentForReply.getId());

        try {
            commentService.ajouter(reply);
            loadCommentsForOeuvre(selectedOeuvre.getId());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML private void handleClose() {
        ((Stage) oeuvreListView.getScene().getWindow()).close();
    }
}
