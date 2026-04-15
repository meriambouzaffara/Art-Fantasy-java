package tn.rouhfan.entities;

import java.time.LocalDateTime;

public class QcmResultat {
    private int id;
    private float score;
    private boolean valide;

    // ✅ DateTimeImmutable en PHP → LocalDateTime en Java
    private LocalDateTime dateTentative;

    // ✅ Clés étrangères en tant qu'objets (Utilisation de User au lieu de Participant)
    private Qcm qcm;
    private User participant;

    // ======= Constructeurs =======

    public QcmResultat() {
        this.score = 0;
        this.valide = false;
        this.dateTentative = LocalDateTime.now();
    }

    public QcmResultat(float score, boolean valide, LocalDateTime dateTentative,
                       Qcm qcm, User participant) {
        this.score = score;
        this.valide = valide;
        this.dateTentative = dateTentative;
        this.qcm = qcm;
        this.participant = participant;
    }

    // ======= Getters & Setters =======

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public float getScore() { return score; }
    public void setScore(float score) { this.score = score; }

    public boolean isValide() { return valide; }
    public void setValide(boolean valide) { this.valide = valide; }

    public LocalDateTime getDateTentative() { return dateTentative; }
    public void setDateTentative(LocalDateTime dateTentative) { this.dateTentative = dateTentative; }

    public Qcm getQcm() { return qcm; }
    public void setQcm(Qcm qcm) { this.qcm = qcm; }

    // ✅ Getter/Setter mis à jour pour utiliser User
    public User getParticipant() { return participant; }
    public void setParticipant(User participant) { this.participant = participant; }

    @Override
    public String toString() {
        return "QcmResultat{" +
                "id=" + id +
                ", score=" + score +
                ", valide=" + valide +
                ", dateTentative=" + dateTentative +
                ", qcm=" + (qcm != null ? qcm.getId() : "null") +
                ", participant=" + (participant != null ? (participant.getNom() + " " + participant.getPrenom()) : "null") +
                "}";
    }
}