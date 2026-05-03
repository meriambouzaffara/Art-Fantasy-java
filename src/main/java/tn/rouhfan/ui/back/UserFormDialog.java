package tn.rouhfan.ui.back;

import javafx.collections.FXCollections;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import tn.rouhfan.entities.User;
import tn.rouhfan.services.UserService;
import tn.rouhfan.tools.PasswordUtils;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Dialog JavaFX complet pour Ajouter / Modifier un utilisateur.
 *
 * Contrôles implémentés :
 * - Champs obligatoires (nom, prénom, email, password en mode ajout)
 * - Format email valide (regex)
 * - Test d'unicité email (vérification en base de données)
 * - Longueur nom/prénom (2-50 car., lettres uniquement)
 * - Mot de passe : min 6 car., lettres + chiffres, confirmation
 * - Messages d'erreur clairs et spécifiques par champ
 * - Indicateur de force du mot de passe
 * - Synchronisation automatique rôle ← type
 */
public class UserFormDialog {

    private static final String STYLE_FIELD_NORMAL =
            "-fx-background-radius: 12; -fx-border-radius: 12; " +
                    "-fx-border-color: rgba(36,17,151,0.15); -fx-padding: 10; -fx-font-size: 13px;";

    private static final String STYLE_FIELD_ERROR =
            "-fx-background-radius: 12; -fx-border-radius: 12; " +
                    "-fx-border-color: #d63031; -fx-border-width: 2; -fx-padding: 10; -fx-font-size: 13px;";

    private static final String STYLE_LABEL =
            "-fx-font-weight: 600; -fx-text-fill: #2d1b4e; -fx-font-size: 13px;";

    private static final String STYLE_ERROR =
            "-fx-text-fill: #d63031; -fx-font-size: 11px; -fx-font-weight: 600;";

    private static final String STYLE_HINT =
            "-fx-text-fill: #5a4a72; -fx-font-size: 11px;";

    /**
     * Affiche le dialog en mode AJOUT.
     */
    public static User showAddDialog() {
        return showDialog(null);
    }

    /**
     * Affiche le dialog en mode MODIFICATION.
     */
    public static User showEditDialog(User existingUser) {
        return showDialog(existingUser);
    }

    private static User showDialog(final User existing) {
        final boolean isEdit = (existing != null);
        final UserService userService = new UserService();

        Dialog<User> dialog = new Dialog<User>();
        dialog.setTitle(isEdit ? "Modifier l'utilisateur #" + existing.getId() : "Ajouter un utilisateur");
        dialog.setHeaderText(isEdit
                ? "Modifier les informations de : " + existing.getPrenom() + " " + existing.getNom()
                : "Remplissez les informations du nouvel utilisateur");

        dialog.getDialogPane().setStyle("-fx-background-color: #faf9fc; -fx-font-family: 'Segoe UI';");
        dialog.getDialogPane().setPrefWidth(550);

        // Boutons
        final ButtonType saveButtonType = new ButtonType(isEdit ? "Enregistrer" : "Ajouter", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setStyle(
                "-fx-background-color: #241197; -fx-text-fill: white; -fx-font-weight: bold; " +
                        "-fx-background-radius: 12; -fx-padding: 10 24; -fx-cursor: hand;"
        );

        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setStyle(
                "-fx-background-color: #f0eef5; -fx-text-fill: #2d1b4e; -fx-font-weight: bold; " +
                        "-fx-background-radius: 12; -fx-padding: 10 24; -fx-cursor: hand; " +
                        "-fx-border-color: rgba(36,17,151,0.1); -fx-border-radius: 12;"
        );

        // ═══ CHAMPS ═══

        final TextField nomField = createStyledTextField("Nom (lettres uniquement)");
        final Label nomError = createErrorLabel();

        final TextField prenomField = createStyledTextField("Prénom (lettres uniquement)");
        final Label prenomError = createErrorLabel();

        final TextField emailField = createStyledTextField("exemple@email.com");
        final Label emailError = createErrorLabel();

        final PasswordField passwordField = createStyledPasswordField(
                isEdit ? "Laisser vide pour ne pas modifier" : "Min 6 car., lettres + chiffres"
        );
        final Label passwordError = createErrorLabel();

        final PasswordField confirmPasswordField = createStyledPasswordField("Confirmez le mot de passe");
        final Label confirmError = createErrorLabel();

        // Indicateur de force du mot de passe
        final Label passwordStrength = new Label("");
        passwordStrength.setStyle(STYLE_HINT);
        passwordField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> obs, String oldVal, String newVal) {
                if (newVal == null || newVal.isEmpty()) {
                    passwordStrength.setText("");
                    passwordStrength.setStyle(STYLE_HINT);
                } else if (newVal.length() < 6) {
                    passwordStrength.setText("Trop court");
                    passwordStrength.setStyle("-fx-text-fill: #d63031; -fx-font-size: 11px; -fx-font-weight: bold;");
                } else if (!newVal.matches(".*[a-zA-Z].*") || !newVal.matches(".*\\d.*")) {
                    passwordStrength.setText("Moyen - ajoutez lettres + chiffres");
                    passwordStrength.setStyle("-fx-text-fill: #e17055; -fx-font-size: 11px; -fx-font-weight: bold;");
                } else if (newVal.length() >= 8) {
                    passwordStrength.setText("Fort");
                    passwordStrength.setStyle("-fx-text-fill: #00b894; -fx-font-size: 11px; -fx-font-weight: bold;");
                } else {
                    passwordStrength.setText("Bon");
                    passwordStrength.setStyle("-fx-text-fill: #0984e3; -fx-font-size: 11px; -fx-font-weight: bold;");
                }
            }
        });

        final ComboBox<String> roleCombo = new ComboBox<String>(FXCollections.observableArrayList(
                "ROLE_ADMIN", "ROLE_ARTISTE", "ROLE_PARTICIPANT"
        ));
        styleCombo(roleCombo);

        final ComboBox<String> statutCombo = new ComboBox<String>(FXCollections.observableArrayList(
                "actif", "inactif", "banni"
        ));
        styleCombo(statutCombo);

        final ComboBox<String> typeCombo = new ComboBox<String>(FXCollections.observableArrayList(
                "admin", "artiste", "participant"
        ));
        styleCombo(typeCombo);

        final Label roleError = createErrorLabel();

        // Synchronisation rôle → type
        roleCombo.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                String role = roleCombo.getValue();
                if (role != null) {
                    if ("ROLE_ADMIN".equals(role)) typeCombo.setValue("admin");
                    else if ("ROLE_ARTISTE".equals(role)) typeCombo.setValue("artiste");
                    else if ("ROLE_PARTICIPANT".equals(role)) typeCombo.setValue("participant");
                }
            }
        });

        // ═══ PRÉ-REMPLIR ═══
        if (isEdit) {
            nomField.setText(existing.getNom());
            prenomField.setText(existing.getPrenom());
            emailField.setText(existing.getEmail());

            String role = existing.getRoles();
            if (role != null) {
                role = role.replace("[", "").replace("]", "")
                        .replace("\"", "").replace("'", "").trim();
                if (role.contains(",")) role = role.split(",")[0].trim();
                roleCombo.setValue(role);
            }
            statutCombo.setValue(existing.getStatut());
            typeCombo.setValue(existing.getType());
        } else {
            roleCombo.getSelectionModel().selectLast();
            statutCombo.getSelectionModel().selectFirst();
            typeCombo.getSelectionModel().selectLast();
        }

        // ═══ LAYOUT ═══
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(4);
        grid.setPadding(new Insets(20, 10, 10, 10));

        int row = 0;

        grid.add(createLabel("Nom *"), 0, row);
        grid.add(new VBox(2, nomField, nomError), 1, row); row++;

        grid.add(createLabel("Prénom *"), 0, row);
        grid.add(new VBox(2, prenomField, prenomError), 1, row); row++;

        grid.add(createLabel("Email *"), 0, row);
        grid.add(new VBox(2, emailField, emailError), 1, row); row++;

        grid.add(createLabel(isEdit ? "Nouveau MDP" : "Mot de passe *"), 0, row);
        grid.add(new VBox(2, passwordField, passwordStrength, passwordError), 1, row); row++;

        grid.add(createLabel("Confirmer MDP"), 0, row);
        grid.add(new VBox(2, confirmPasswordField, confirmError), 1, row); row++;

        grid.add(createLabel("Rôle *"), 0, row);
        grid.add(new VBox(2, roleCombo, roleError), 1, row); row++;

        grid.add(createLabel("Statut *"), 0, row);
        grid.add(statutCombo, 1, row); row++;

        Label typeLabel = createLabel("Type (auto)");
        typeLabel.setStyle(STYLE_HINT);
        grid.add(typeLabel, 0, row);
        typeCombo.setDisable(true);
        grid.add(typeCombo, 1, row); row++;

        Label noteLabel = new Label("* Champs obligatoires  |  Le type est synchronisé avec le rôle");
        noteLabel.setStyle(STYLE_HINT);
        noteLabel.setWrapText(true);
        grid.add(noteLabel, 0, row, 2, 1);

        dialog.getDialogPane().setContent(grid);

        // ═══ VALIDATION ═══
        saveButton.addEventFilter(ActionEvent.ACTION, new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                // Reset erreurs
                resetField(nomField, nomError);
                resetField(prenomField, prenomError);
                resetField(emailField, emailError);
                resetField(passwordField, passwordError);
                resetField(confirmPasswordField, confirmError);
                roleError.setText("");

                boolean hasError = false;

                String nom = nomField.getText().trim();
                String prenom = prenomField.getText().trim();
                String email = emailField.getText().trim();
                String pwd = passwordField.getText();
                String confirmPwd = confirmPasswordField.getText();

                // 1. Nom
                if (nom.isEmpty()) {
                    setFieldError(nomField, nomError, "Le nom est obligatoire.");
                    hasError = true;
                } else if (nom.length() < 2) {
                    setFieldError(nomField, nomError, "Le nom doit contenir au moins 2 caractères.");
                    hasError = true;
                } else if (nom.length() > 50) {
                    setFieldError(nomField, nomError, "Le nom ne peut pas dépasser 50 caractères.");
                    hasError = true;
                } else if (!nom.matches("^[a-zA-ZÀ-ÿ\\s'-]+$")) {
                    setFieldError(nomField, nomError, "Le nom ne doit contenir que des lettres.");
                    hasError = true;
                }

                // 2. Prénom
                if (prenom.isEmpty()) {
                    setFieldError(prenomField, prenomError, "Le prénom est obligatoire.");
                    hasError = true;
                } else if (prenom.length() < 2) {
                    setFieldError(prenomField, prenomError, "Le prénom doit contenir au moins 2 caractères.");
                    hasError = true;
                } else if (prenom.length() > 50) {
                    setFieldError(prenomField, prenomError, "Le prénom ne peut pas dépasser 50 caractères.");
                    hasError = true;
                } else if (!prenom.matches("^[a-zA-ZÀ-ÿ\\s'-]+$")) {
                    setFieldError(prenomField, prenomError, "Le prénom ne doit contenir que des lettres.");
                    hasError = true;
                }

                // 3. Email : obligatoire, format valide, unicité
                if (email.isEmpty()) {
                    setFieldError(emailField, emailError, "L'adresse e-mail est obligatoire.");
                    hasError = true;
                } else if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
                    setFieldError(emailField, emailError, "Format d'email invalide (ex: nom@domaine.com).");
                    hasError = true;
                } else {
                    // Test d'unicité en base
                    try {
                        User existingByEmail = userService.findByEmail(email);
                        if (existingByEmail != null) {
                            if (!isEdit || existingByEmail.getId() != existing.getId()) {
                                setFieldError(emailField, emailError,
                                        "Cet email est déjà utilisé par " +
                                                existingByEmail.getPrenom() + " " + existingByEmail.getNom() + ".");
                                hasError = true;
                            }
                        }
                    } catch (SQLException ex) {
                        setFieldError(emailField, emailError, "Erreur de vérification d'unicité.");
                        hasError = true;
                    }
                }

                // 4. Mot de passe
                if (!isEdit && pwd.isEmpty()) {
                    setFieldError(passwordField, passwordError, "Le mot de passe est obligatoire.");
                    hasError = true;
                } else if (!pwd.isEmpty()) {
                    if (pwd.length() < 6) {
                        setFieldError(passwordField, passwordError, "Minimum 6 caractères requis.");
                        hasError = true;
                    } else if (!pwd.matches(".*[a-zA-Z].*")) {
                        setFieldError(passwordField, passwordError, "Doit contenir au moins une lettre.");
                        hasError = true;
                    } else if (!pwd.matches(".*\\d.*")) {
                        setFieldError(passwordField, passwordError, "Doit contenir au moins un chiffre.");
                        hasError = true;
                    }

                    if (!pwd.equals(confirmPwd)) {
                        setFieldError(confirmPasswordField, confirmError, "Les mots de passe ne correspondent pas.");
                        hasError = true;
                    }
                }

                // 5. Rôle
                if (roleCombo.getValue() == null) {
                    roleError.setText("Veuillez sélectionner un rôle.");
                    roleError.setStyle(STYLE_ERROR);
                    hasError = true;
                }

                // 6. Statut
                if (statutCombo.getValue() == null) {
                    hasError = true;
                }

                if (hasError) {
                    event.consume();
                }
            }
        });

        // ═══ RESULT CONVERTER ═══
        dialog.setResultConverter(new Callback<ButtonType, User>() {
            @Override
            public User call(ButtonType dialogButton) {
                if (dialogButton == saveButtonType) {
                    User user = isEdit ? existing : new User();
                    user.setNom(nomField.getText().trim());
                    user.setPrenom(prenomField.getText().trim());
                    user.setEmail(emailField.getText().trim());

                    String roleValue = "[\"" + roleCombo.getValue() + "\"]";
                    user.setRoles(roleValue);
                    user.setStatut(statutCombo.getValue());
                    user.setType(typeCombo.getValue());

                    String pwd = passwordField.getText();
                    if (!pwd.isEmpty()) {
                        user.setPassword(PasswordUtils.hashPassword(pwd));
                    }

                    return user;
                }
                return null;
            }
        });

        Optional<User> result = dialog.showAndWait();
        return result.orElse(null);
    }

    // ═══ HELPERS ═══

    private static Label createLabel(String text) {
        Label l = new Label(text);
        l.setStyle(STYLE_LABEL);
        l.setMinWidth(120);
        return l;
    }

    private static Label createErrorLabel() {
        Label l = new Label("");
        l.setStyle(STYLE_ERROR);
        l.setWrapText(true);
        l.setMaxWidth(320);
        return l;
    }

    private static TextField createStyledTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefWidth(320);
        tf.setStyle(STYLE_FIELD_NORMAL);
        return tf;
    }

    private static PasswordField createStyledPasswordField(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        pf.setPrefWidth(320);
        pf.setStyle(STYLE_FIELD_NORMAL);
        return pf;
    }

    private static void styleCombo(ComboBox<String> cb) {
        cb.setStyle(
                "-fx-background-color: #ffffff; -fx-background-radius: 12; " +
                        "-fx-border-color: rgba(36,17,151,0.15); -fx-border-radius: 12; -fx-padding: 8;"
        );
        cb.setPrefWidth(320);
    }

    private static void setFieldError(Control field, Label errorLabel, String message) {
        field.setStyle(STYLE_FIELD_ERROR);
        errorLabel.setText(message);
        errorLabel.setStyle(STYLE_ERROR);
    }

    private static void resetField(Control field, Label errorLabel) {
        field.setStyle(STYLE_FIELD_NORMAL);
        errorLabel.setText("");
    }
}