# Rouh el Fann - Application Desktop JavaFX

Application de gestion d'art et fantaisie avec interface JavaFX.

## Structure du projet

```
src/main/
├── java/tn/rouhfan/
│   ├── entities/        # Entités JPA (User, Oeuvre, Cours, etc.)
│   ├── services/        # Services métier (CRUD)
│   ├── tools/           # Utilitaires (MyConnection)
│   └── ui/              # Interface utilisateur JavaFX
│       ├── AppLauncher.java           # Point d'entrée
│       ├── Router.java                # Navigation
│       ├── front/                     # Front-office
│       │   ├── FrontBase.fxml
│       │   └── FrontBaseController.java
│       └── back/                      # Back-office
│           ├── BackBase.fxml
│           ├── BackBaseController.java
│           ├── DashboardHome.fxml
│           ├── *View.fxml             # Vues CRUD (8 modules)
│           └── *Controller.java        # Contrôleurs
└── resources/ui/
    ├── theme.css        # Styles globaux
    ├── front/           # Templates front-office
    └── back/            # Templates back-office
        ├── *.fxml       # Vues FXML
        └── OeuvreFormDialog.fxml  # Formulaires
```

## Modules disponibles

| Module | Vue | Contrôleur |
|--------|-----|------------|
| Galerie | GalerieView.fxml | GalerieController |
| Catégories | CategoriesView.fxml | CategoriesController |
| Événements | EvenementsView.fxml | EvenementsController |
| Sponsors | SponsorsView.fxml | SponsorsController |
| Cours | CoursView.fxml | CoursController |
| Certificats | CertificatsView.fxml | CertificatsController |
| Magasin | MagasinView.fxml | MagasinController |
| Avis & Réclamations | AvisView.fxml | AvisController |

## Lancer l'application

### Prérequis
- JDK 11 ou supérieur
- Maven

### Commandes

```bash
# Compiler
mvn clean compile

# Lancer avec JavaFX
mvn javafx:run
```

Ou depuis l'IDE :
1. Ouvrir le projet
2. Exécuter la classe `tn.rouhfan.ui.AppLauncher`

## Navigation

- **Front-office** : Navbar avec liens vers Accueil, Événements, Cours, Magasin, Galerie, Avis
- **Back-office** : Sidebar avec les 8 modules de gestion
- Switch Front/Back via le bouton "Dashboard" ou "← Retour au site"

## Fonctionnalités CRUD

Chaque module dispose de :
- Tableau des données avec colonnes pertinentes
- Boutons Ajouter / Modifier / Supprimer
- Champ de recherche
- Rafraîchissement des données

## Styles

- Thème sombre "Art Fantasy"
- Icônes emoji claires et visibles (✨ 🎨 📅 🖼️ 🤝 🏷️ 🖌️ 💬)
- Tableaux avec alternance de couleurs
- Survol et sélection en doré (#f59e0b)

## Personnalisation

Pour modifier les styles : `src/main/resources/ui/theme.css`
Pour ajouter un module : copier un *View.fxml et *Controller.java existants
