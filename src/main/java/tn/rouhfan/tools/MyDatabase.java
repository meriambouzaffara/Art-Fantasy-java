package tn.rouhfan.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {

    private static final String URL = "jdbc:mysql://localhost:3306/rouh?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private Connection cnx;
    private static MyDatabase instance;

    // Constructeur privé (Singleton)
    private MyDatabase() {
        try {
            cnx = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connexion à la base de données établie !");
        } catch (SQLException e) {
            System.out.println("Erreur de connexion : " + e.getMessage());
        }
    }

    // Thread-safe Singleton
    public static synchronized MyDatabase getInstance() {
        if (instance == null) {
            instance = new MyDatabase();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            // Vérifie si la connexion est fermée → reconnecte
            if (cnx == null || cnx.isClosed()) {
                cnx = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("Reconnexion à la base de données !");
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la reconnexion : " + e.getMessage());
        }
        return cnx;
    }
}