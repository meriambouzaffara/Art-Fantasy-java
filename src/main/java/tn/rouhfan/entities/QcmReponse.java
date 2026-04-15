package tn.rouhfan.entities;

public class QcmReponse {
    private int id;
    private String texte;
    private boolean correcte;

    // ✅ Clé étrangère en tant qu'objet
    private QcmQuestion question;

    // ======= Constructeurs =======

    public QcmReponse() {
        this.correcte = false;
    }

    public QcmReponse(String texte, boolean correcte, QcmQuestion question) {
        this.texte = texte;
        this.correcte = correcte;
        this.question = question;
    }

    // ======= Getters & Setters =======

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTexte() { return texte; }
    public void setTexte(String texte) { this.texte = texte; }

    public boolean isCorrecte() { return correcte; }
    public void setCorrecte(boolean correcte) { this.correcte = correcte; }

    public QcmQuestion getQuestion() { return question; }
    public void setQuestion(QcmQuestion question) { this.question = question; }

    @Override
    public String toString() {
        return "QcmReponse{" +
                "id=" + id +
                ", texte='" + texte + '\'' +
                ", correcte=" + correcte +
                ", question=" + (question != null ? question.getId() : "null") +
                "}";
    }
}