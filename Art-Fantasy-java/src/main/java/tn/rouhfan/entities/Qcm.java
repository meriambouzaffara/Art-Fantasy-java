package tn.rouhfan.entities;

import java.util.List;
import java.util.ArrayList;

public class Qcm {
    private int id;
    private float scoreMinRequis;
    private int dureeMinutes;

    //  Clé étrangère en tant qu'objet
    private Cours cours;

    //  Relation OneToMany → liste de questions
    private List<QcmQuestion> questions;

    // ======= Constructeurs =======

    public Qcm() {
        this.questions = new ArrayList<>(); // équivalent de new ArrayCollection()
        this.scoreMinRequis = 0;
        this.dureeMinutes = 0;
    }

    public Qcm(float scoreMinRequis, int dureeMinutes, Cours cours) {
        this.scoreMinRequis = scoreMinRequis;
        this.dureeMinutes = dureeMinutes;
        this.cours = cours;
        this.questions = new ArrayList<>();
    }

    // ======= Getters & Setters =======

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public float getScoreMinRequis() { return scoreMinRequis; }
    public void setScoreMinRequis(float scoreMinRequis) { this.scoreMinRequis = scoreMinRequis; }

    public int getDureeMinutes() { return dureeMinutes; }
    public void setDureeMinutes(int dureeMinutes) { this.dureeMinutes = dureeMinutes; }

    public Cours getCours() { return cours; }
    public void setCours(Cours cours) { this.cours = cours; }

    public List<QcmQuestion> getQuestions() { return questions; }
    public void setQuestions(List<QcmQuestion> questions) { this.questions = questions; }

    //  Méthodes utilitaires comme en PHP
    public void addQuestion(QcmQuestion question) {
        if (!questions.contains(question)) {
            questions.add(question);
            question.setQcm(this); // lien bidirectionnel
        }
    }

    public void removeQuestion(QcmQuestion question) {
        if (questions.remove(question)) {
            question.setQcm(null);
        }
    }

    @Override
    public String toString() {
        return "Qcm{" +
                "id=" + id +
                ", scoreMinRequis=" + scoreMinRequis +
                ", dureeMinutes=" + dureeMinutes +
                ", cours=" + (cours != null ? cours.getNom() : "null") +
                ", nbQuestions=" + questions.size() +
                "}";
    }
}