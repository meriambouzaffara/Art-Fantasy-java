package tn.rouhfan.services;

import java.sql.SQLException;
import java.util.List;

public interface IService<T> {

    void ajouter(T t) throws SQLException;

    void supprimer(int id) throws SQLException;

    void modifier(T t) throws SQLException;

    List<T> recuperer() throws SQLException;

    T findById(int id) throws SQLException;

}