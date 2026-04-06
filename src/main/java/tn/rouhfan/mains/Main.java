package tn.rouhfan.mains;

import tn.rouhfan.entities.User;
import tn.rouhfan.services.UserService;

import java.sql.SQLException;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        UserService us = new UserService();

        try {
            //  Ajouter un utilisateur
            User u = new User(
                    "Dhia",
                    "Ayari",
                    "test@test.com",
                    "123456",
                    "[\"ROLE_ADMIN\"]",
                    "actif",
                    true,
                    "admin"
            );

            us.ajouter(u);
            System.out.println("✅ Utilisateur ajouté !");

            //  Afficher tous les utilisateurs
            System.out.println("\n Liste des utilisateurs :");
            List<User> users = us.recuperer();

            for (User user : users) {
                System.out.println(user);
            }

        } catch (SQLException e) {
            System.out.println("Erreur SQL : " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Erreur : " + e.getMessage());
        }
    }
}