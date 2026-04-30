package tn.rouhfan.ui.front;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.rouhfan.entities.Commentaire;
import tn.rouhfan.entities.Oeuvre;
import tn.rouhfan.entities.User;
import tn.rouhfan.services.CommentaireService;
import tn.rouhfan.tools.SessionManager;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javafx.stage.StageStyle;
import javafx.geometry.Bounds;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;

public class OeuvreDetailsController {

    @FXML private ImageView oeuvreImage;
    @FXML private Label statusBadge;
    @FXML private Label categorieLabel;
    @FXML private Label titreLabel;
    @FXML private Label artisteLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label prixLabel;

    @FXML private VBox commentsVBox;
    @FXML private TextArea commentInput;
    @FXML private Label replyingToLabel;
    @FXML private Button cancelReplyBtn;
    private Oeuvre oeuvre;
    private CommentaireService commentService = new CommentaireService();
    private Commentaire replyingTo = null;

    public void setOeuvre(Oeuvre o) {
        this.oeuvre = o;
        
        titreLabel.setText(o.getTitre());
        descriptionLabel.setText(o.getDescription());
        categorieLabel.setText(o.getCategorie() != null ? o.getCategorie().getNomCategorie() : "Non classé");
        artisteLabel.setText(o.getUser() != null ? o.getUser().getNom() + " " + o.getUser().getPrenom() : "Artiste inconnu");
        prixLabel.setText(o.getPrix() != null ? o.getPrix().toString() + " DT" : "0 DT");
        statusBadge.setText(o.getStatut());

        // Status style
        if ("disponible".equalsIgnoreCase(o.getStatut())) {
            statusBadge.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-padding: 8 15; -fx-background-radius: 20; -fx-font-weight: bold;");
        } else {
            statusBadge.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-padding: 8 15; -fx-background-radius: 20; -fx-font-weight: bold;");
        }

        // Image loading via ImageUtils
        if (o.getImage() != null && !o.getImage().isEmpty()) {
            String fullPath = tn.rouhfan.tools.ImageUtils.getAbsolutePath(o.getImage());
            if (fullPath != null) {
                oeuvreImage.setImage(new Image(fullPath));
            }
        }

        loadComments();
        
        // UI Polish: Hide comment input for guests (non-logged or non-allowed roles)
        User user = SessionManager.getInstance().getCurrentUser();
        String roles = (user != null) ? user.getRoles() : null;
        boolean canComment = roles != null && (roles.contains("ARTISTE") || roles.contains("PARTICIPANT"));
        
        if (!canComment) {
            if (commentInput != null && commentInput.getParent() != null) {
                commentInput.getParent().setVisible(false);
                commentInput.getParent().setManaged(false);
            }
        }
    }



    private void loadComments() {
        if (commentsVBox == null) {
            System.err.println("[OeuvreDetailsController] CRITICAL: commentsVBox is NULL!");
            System.err.println("[OeuvreDetailsController] oeuvreImage: " + (oeuvreImage != null));
            System.err.println("[OeuvreDetailsController] titreLabel: " + (titreLabel != null));
            System.err.println("[OeuvreDetailsController] commentInput: " + (commentInput != null));
            return;
        }
        commentsVBox.getChildren().clear();
        try {
            List<Commentaire> allComments = commentService.recupererParOeuvre(oeuvre.getId());
            
            List<Commentaire> mainComments = allComments.stream()
                    .filter(c -> c.getParentCommentId() == null)
                    .collect(Collectors.toList());
            
            for (Commentaire c : mainComments) {
                addCommentToUI(c, 0);
                List<Commentaire> replies = allComments.stream()
                        .filter(r -> r.getParentCommentId() != null && r.getParentCommentId() == c.getId())
                        .collect(Collectors.toList());
                for (Commentaire r : replies) {
                    addCommentToUI(r, 45); 
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            commentsVBox.getChildren().add(new Label("Impossible de charger les commentaires."));
        }
    }

    private void addCommentToUI(Commentaire c, double indent) {
        VBox wrapper = new VBox(0);
        wrapper.setPadding(new Insets(10, 0, 10, indent));
        
        HBox commentRow = new HBox(12);
        commentRow.setAlignment(Pos.TOP_LEFT);

        // 1. Avatar
        StackPane avatar = new StackPane();
        double avatarSize = indent > 0 ? 32 : 42;
        avatar.setMinSize(avatarSize, avatarSize);
        avatar.setMaxSize(avatarSize, avatarSize);
        avatar.setStyle("-fx-background-color: #f0f2f5; -fx-background-radius: 50; -fx-border-color: #241197; -fx-border-radius: 50; -fx-border-width: 1.5;");
        
        String displayName = (c.getUserName() != null && !c.getUserName().trim().isEmpty()) ? c.getUserName() : "Utilisateur";
        Label initial = new Label(displayName.substring(0, 1).toUpperCase());
        initial.setStyle("-fx-font-weight: bold; -fx-text-fill: #241197; -fx-font-size: " + (indent > 0 ? "12" : "15") + ";");
        avatar.getChildren().add(initial);

        // 2. Content Column
        VBox contentCol = new VBox(2);
        contentCol.setMaxWidth(350);

        // 3. AnchorPane for precise positioning
        javafx.scene.layout.AnchorPane bubbleContainer = new javafx.scene.layout.AnchorPane();

        VBox bubble = new VBox(2);
        bubble.setPadding(new Insets(10, 15, 10, 15));
        bubble.setStyle("-fx-background-color: #f0f2f5; -fx-background-radius: 18; -fx-border-color: #e4e6eb; -fx-border-radius: 18; -fx-border-width: 0.5;");
        bubble.setMinWidth(120);

        Label nameLabel = new Label(displayName);
        nameLabel.setStyle("-fx-font-weight: 800; -fx-font-size: 13; -fx-text-fill: #050505;");
        
        String content = (c.getContenu() != null && !c.getContenu().isEmpty()) ? c.getContenu() : "...";
        Label contentLabel = new Label(content);
        contentLabel.setWrapText(true);
        contentLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #050505;");

        bubble.getChildren().addAll(nameLabel, contentLabel);
        
        // Add bubble to AnchorPane
        javafx.scene.layout.AnchorPane.setTopAnchor(bubble, 0.0);
        javafx.scene.layout.AnchorPane.setLeftAnchor(bubble, 0.0);
        bubbleContainer.getChildren().add(bubble);

        // Reactions Display (FOOLPROOF POSITIONING)
        HBox reactionsPill = new HBox(2);
        reactionsPill.setAlignment(Pos.CENTER_LEFT);
        reactionsPill.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 2 6; " +
                               "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 4, 0, 0, 1); " +
                               "-fx-border-color: #e4e6eb; -fx-border-radius: 12; -fx-border-width: 0.8;");
        reactionsPill.setVisible(false);
        reactionsPill.setManaged(false);

        try {
            java.util.Map<String, Integer> counts = commentService.getReactionCounts(c.getId());
            if (!counts.isEmpty()) {
                reactionsPill.setVisible(true);
                reactionsPill.setManaged(true);
                int total = 0;
                int shown = 0;
                for (String type : counts.keySet()) {
                    if (shown < 3) {
                        reactionsPill.getChildren().add(getReactionGraphic(type, 14));
                        shown++;
                    }
                    total += counts.get(type);
                }
                Label countLabel = new Label(" " + total);
                countLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #65676b; -fx-font-weight: bold;");
                reactionsPill.getChildren().add(countLabel);
            }
        } catch (SQLException e) { e.printStackTrace(); }

        if (reactionsPill.isVisible()) {
            bubbleContainer.getChildren().add(reactionsPill);
            // Position at bottom right of the bubble
            javafx.scene.layout.AnchorPane.setBottomAnchor(reactionsPill, -8.0);
            javafx.scene.layout.AnchorPane.setRightAnchor(reactionsPill, -5.0);
        }

        contentCol.getChildren().add(bubbleContainer);

        // 4. Action Buttons
        HBox actions = new HBox(15);
        actions.setPadding(new Insets(10, 0, 0, 10)); // More space below the bubble
        actions.setAlignment(Pos.CENTER_LEFT);

        Button likeBtn = new Button("J'aime");
        likeBtn.setGraphicTextGap(6);
        likeBtn.setCursor(javafx.scene.Cursor.HAND);
        try {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                String myReaction = commentService.getUserReaction(c.getId(), currentUser.getId());
                if (myReaction != null) {
                    likeBtn.setGraphic(getReactionGraphic(myReaction, 16));
                    likeBtn.setText(myReaction.substring(0, 1).toUpperCase() + myReaction.substring(1).toLowerCase());
                    likeBtn.setStyle("-fx-text-fill: #2167f3; -fx-font-weight: bold; -fx-font-size: 12; -fx-background-color: transparent; -fx-padding: 0;");
                } else {
                    likeBtn.setStyle("-fx-text-fill: #65676b; -fx-font-weight: bold; -fx-font-size: 12; -fx-background-color: transparent; -fx-padding: 0;");
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }

        likeBtn.setOnMouseEntered(e -> showReactionPopup(likeBtn, c.getId()));
        likeBtn.setOnAction(e -> handleLikeClick(c.getId()));

        Hyperlink replyLink = new Hyperlink("Répondre");
        replyLink.setStyle("-fx-font-size: 12; -fx-text-fill: #65676b; -fx-font-weight: bold; -fx-padding: 0; -fx-underline: false;");
        replyLink.setOnAction(e -> startReply(c));

        Label timeLabel = new Label(formatDate(c.getDateCommentaire()));
        timeLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #65676b;");

        actions.getChildren().addAll(likeBtn, replyLink, timeLabel);
        contentCol.getChildren().add(actions);

        commentRow.getChildren().addAll(avatar, contentCol);
        wrapper.getChildren().add(commentRow);
        commentsVBox.getChildren().add(wrapper);
    }

    private String getFallbackEmoji(String type) {
        switch (type) {
            case "LIKE": return "👍";
            case "LOVE": return "❤️";
            case "HAHA": return "😆";
            case "WOW": return "😮";
            case "SAD": return "😢";
            case "ANGRY": return "😡";
            default: return "😶";
        }
    }

    private javafx.scene.Node getReactionGraphic(String type, double size) {
        try {
            String path = "/images/reactions/" + type.toLowerCase() + ".png";
            java.net.URL url = getClass().getResource(path);
            if (url != null) {
                ImageView iv = new ImageView(new Image(url.toExternalForm()));
                iv.setFitWidth(size);
                iv.setFitHeight(size);
                return iv;
            }
        } catch (Exception e) {}
        
        Label l = new Label(getFallbackEmoji(type));
        l.setStyle("-fx-font-size: " + size + ";");
        if ("ANGRY".equals(type)) {
            l.setStyle(l.getStyle() + "-fx-text-fill: #ef4444; -fx-font-weight: bold;");
        }
        return l;
    }

    private javafx.stage.Popup currentPopup;

    private void showReactionPopup(Button owner, int commentId) {
        if (currentPopup != null) currentPopup.hide();

        HBox popupContent = new HBox(12);
        popupContent.setAlignment(Pos.CENTER);
        popupContent.setStyle("-fx-background-color: white; " +
                              "-fx-background-radius: 30; " +
                              "-fx-padding: 8 15; " +
                              "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 15, 0, 0, 8); " +
                              "-fx-border-color: #f1f5f9; " +
                              "-fx-border-radius: 30; " +
                              "-fx-border-width: 1;");
        
        String[] types = {"LIKE", "LOVE", "HAHA", "WOW", "SAD"};
        
        currentPopup = new javafx.stage.Popup();
        currentPopup.setAutoHide(true);

        for (String t : types) {
            StackPane iconContainer = new StackPane();
            iconContainer.setPadding(new Insets(5));
            iconContainer.setCursor(javafx.scene.Cursor.HAND);
            
            javafx.scene.Node graphic = getReactionGraphic(t, 28);
            iconContainer.getChildren().add(graphic);
            
            iconContainer.setOnMouseEntered(e -> {
                graphic.setScaleX(1.3);
                graphic.setScaleY(1.3);
            });
            iconContainer.setOnMouseExited(e -> {
                graphic.setScaleX(1.0);
                graphic.setScaleY(1.0);
            });
            iconContainer.setOnMouseClicked(e -> {
                currentPopup.hide(); // Hide FIRST
                handleReaction(commentId, t);
            });
            popupContent.getChildren().add(iconContainer);
        }

        currentPopup.getContent().add(popupContent);
        
        javafx.geometry.Bounds bounds = owner.localToScreen(owner.getBoundsInLocal());
        if (bounds != null) {
            currentPopup.show(owner, bounds.getMinX() - 10, bounds.getMinY() - 60);
        }
        
        popupContent.setOnMouseExited(e -> {
            if (currentPopup != null && currentPopup.isShowing()) {
                javafx.geometry.Point2D mousePos = new javafx.geometry.Point2D(e.getScreenX(), e.getScreenY());
                javafx.geometry.Bounds ownerBounds = owner.localToScreen(owner.getBoundsInLocal());
                if (ownerBounds == null || !ownerBounds.contains(mousePos)) {
                    currentPopup.hide();
                }
            }
        });
    }

    private void handleReaction(int commentId, String type) {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;
        try {
            commentService.ajouterReaction(commentId, user.getId(), type);
            loadComments();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void handleLikeClick(int commentId) {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;
        try {
            String current = commentService.getUserReaction(commentId, user.getId());
            if (current != null) {
                commentService.supprimerReaction(commentId, user.getId());
            } else {
                commentService.ajouterReaction(commentId, user.getId(), "LIKE");
            }
            loadComments();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private javafx.stage.Popup emojiPopup;

    @FXML
    private void showEmojiPicker() {
        if (emojiPopup != null) emojiPopup.hide();

        VBox picker = new VBox(10);
        picker.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-padding: 15; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 20, 0, 0, 8); " +
                        "-fx-border-color: #f1f5f9; -fx-border-radius: 20; -fx-border-width: 1;");
        
        Label title = new Label("Choisissez une réaction");
        title.setStyle("-fx-font-weight: 900; -fx-font-size: 11; -fx-text-fill: #94a3b8; -fx-text-transform: uppercase;");
        picker.getChildren().add(title);

        javafx.scene.layout.FlowPane flow = new javafx.scene.layout.FlowPane(8, 8);
        flow.setPrefWrapLength(240);
        
        // 1. Core Reactions (Colored Images)
        String[] core = {"LIKE", "LOVE", "HAHA", "WOW", "SAD"};
        for (String type : core) {
            StackPane btn = createEmojiButton(getReactionGraphic(type, 26), e -> {
                commentInput.appendText(getFallbackEmoji(type));
                emojiPopup.hide();
            });
            flow.getChildren().add(btn);
        }

        // 2. Extra Emojis (Styled Unicode - Standard ones for better compatibility)
        String[] extras = {"😊", "❤️", "👍", "👏", "🙌", "✨", "🔥", "🎨", "🖼️", "💡", "💯", "✅"};
        for (String emo : extras) {
            Label l = new Label(emo);
            l.setStyle("-fx-font-size: 22;");
            StackPane btn = createEmojiButton(l, e -> {
                commentInput.appendText(emo);
                emojiPopup.hide();
            });
            flow.getChildren().add(btn);
        }

        picker.getChildren().add(flow);

        emojiPopup = new javafx.stage.Popup();
        emojiPopup.setAutoHide(true);
        emojiPopup.getContent().add(picker);
        
        javafx.geometry.Bounds bounds = commentInput.localToScreen(commentInput.getBoundsInLocal());
        if (bounds != null) {
            emojiPopup.show(commentInput, bounds.getMaxX() - 260, bounds.getMinY() - 200);
        }
    }

    private StackPane createEmojiButton(javafx.scene.Node graphic, javafx.event.EventHandler<javafx.scene.input.MouseEvent> onClick) {
        StackPane container = new StackPane(graphic);
        container.setPadding(new Insets(8));
        container.setCursor(javafx.scene.Cursor.HAND);
        container.setStyle("-fx-background-radius: 12;");
        
        container.setOnMouseEntered(e -> container.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 12;"));
        container.setOnMouseExited(e -> container.setStyle("-fx-background-color: transparent;"));
        container.setOnMouseClicked(onClick);
        
        return container;
    }

    private String formatDate(java.util.Date date) {
        if (date == null) return "";
        long diff = new java.util.Date().getTime() - date.getTime();
        long minutes = diff / (60 * 1000);
        if (minutes < 1) return "À l'instant";
        if (minutes < 60) return minutes + " min";
        long hours = minutes / 60;
        if (hours < 24) return hours + " h";
        long days = hours / 24;
        if (days < 7) return days + " j";
        return new java.text.SimpleDateFormat("dd MMM").format(date);
    }

    private void startReply(Commentaire c) {
        this.replyingTo = c;
        replyingToLabel.setText("En réponse à " + c.getUserName());
        replyingToLabel.setVisible(true);
        replyingToLabel.setManaged(true);
        cancelReplyBtn.setVisible(true);
        cancelReplyBtn.setManaged(true);
        commentInput.requestFocus();
    }

    @FXML
    private void cancelReply() {
        this.replyingTo = null;
        replyingToLabel.setVisible(false);
        replyingToLabel.setManaged(false);
        cancelReplyBtn.setVisible(false);
        cancelReplyBtn.setManaged(false);
    }

    @FXML
    private void handlePostComment() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            showError("Connexion requise", "Vous devez être connecté pour commenter.");
            return;
        }

        String roles = currentUser.getRoles();
        boolean canComment = roles != null && (roles.contains("ARTISTE") || roles.contains("PARTICIPANT"));
        
        if (!canComment) {
            showError("Accès restreint", "Votre rôle ne vous permet pas de commenter.");
            return;
        }

        String content = commentInput.getText().trim();
        if (content.isEmpty()) return;

        Commentaire c = new Commentaire(content, oeuvre.getId(), currentUser.getId());
        if (replyingTo != null) {
            c.setParentCommentId(replyingTo.getId());
        }

        try {
            commentService.ajouter(c);
            commentInput.clear();
            cancelReply();
            loadComments();
        } catch (SQLException e) {
            showError("Erreur", "Impossible de publier le commentaire: " + e.getMessage());
        }
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void close() {
        ((Stage) titreLabel.getScene().getWindow()).close();
    }
}
