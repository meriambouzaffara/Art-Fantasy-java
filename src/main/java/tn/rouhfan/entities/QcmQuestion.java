package tn.rouhfan.entities;


import java.util.ArrayList;
import java.util.List;

public class QcmQuestion {
    private int id;
    private String question;

    // ✅ Clé étrangère en tant qu'objet
    private Qcm qcm;

    // ✅ Relation OneToMany → liste de réponses
    private List<QcmReponse> reponses;

    // ======= Constructeurs =======

    public QcmQuestion() {
        this.reponses = new ArrayList<>();
    }

    public QcmQuestion(String question, Qcm qcm) {
        this.question = question;
        this.qcm = qcm;
        this.reponses = new ArrayList<>();
    }

    // ======= Getters & Setters =======

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public Qcm getQcm() { return qcm; }
    public void setQcm(Qcm qcm) { this.qcm = qcm; }

    public List<QcmReponse> getReponses() { return reponses; }
    public void setReponses(List<QcmReponse> reponses) { this.reponses = reponses; }

    // ✅ Méthodes utilitaires
    public void addReponse(QcmReponse reponse) {
        if (!reponses.contains(reponse)) {
            reponses.add(reponse);
            reponse.setQuestion(this); // lien bidirectionnel
        }
    }

    public void removeReponse(QcmReponse reponse) {
        if (reponses.remove(reponse)) {
            reponse.setQuestion(null);
        }
    }

    @Override
    public String toString() {
        return "QcmQuestion{" +
                "id=" + id +
                ", question='" + question + '\'' +
                ", qcm=" + (qcm != null ? qcm.getId() : "null") +
                ", nbReponses=" + reponses.size() +
                "}";
    }
}